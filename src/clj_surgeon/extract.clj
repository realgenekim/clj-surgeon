(ns clj-surgeon.extract
  "Extract forms from one namespace to a new namespace file.

   Algorithm:
   1. Find named forms with exact boundaries
   2. Compile a dependency-minimal target ns form
   3. Write new file with forms in topological order
   4. Remove forms from source file
   5. Add a source require only when remaining forms call moved Vars
   6. Report callers that may need updating

   Does NOT: fix circular deps or update callers in other namespaces.

   ALL PLANNING IS PURE. Only execute! writes files."
  (:require
   [clj-surgeon.analyze :as analyze]
   [clj-surgeon.cljc.require-ops :as require-ops]
   [clj-surgeon.extract-header :as extract-header]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.quoted-var-refs :as quoted-var-refs]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.parser :as parser]
   [rewrite-clj.zip :as z]))

;; @spec MCP-OP-PLAN-007
(defn publicize-defn-source
  "Losslessly change one exact defn- source form to defn. Refuse all other
  declaration shapes so visibility changes cannot silently alter metadata or
  custom macro semantics."
  [source]
  (let [form (z/of-string source)
        head (z/down form)]
    (when-not (= "defn-" (z/string head))
      (throw (ex-info "Only exact defn- forms can be made public"
                      {:error-type :unsupported-public-form
                       :form-head (z/string head)})))
    (-> head
        (z/replace (parser/parse-string "defn"))
        z/root-string)))

;; ============================================================
;; Pure helpers
;; ============================================================

(defn- source-paths-from-deps-edn
  "Read :paths and alias :extra-paths from deps.edn. Returns nil if no deps.edn."
  []
  (let [f (io/file "deps.edn")]
    (when (.exists f)
      (let [deps (edn/read-string (slurp f))]
        (distinct
          (concat (:paths deps)
                  (mapcat :extra-paths (vals (:aliases deps)))))))))

(defn file-path->ns-name
  "Derive namespace name from a file path.
   src/writer/state/distillery.clj → writer.state.distillery
   /tmp/foo/src/my/app.clj → my.app
   src/clj/myapp/core.clj → myapp.core (with source-paths [\"src/clj\"])
   src/cljs/myapp/ui.cljs → myapp.ui (with source-paths [\"src/cljs\"])"
  ([path] (file-path->ns-name path nil))
  ([path source-paths]
   (let [source-paths (or (seq source-paths)
                          (source-paths-from-deps-edn)
                          ["src"])
         ;; Normalize: strip leading ./
         norm (str/replace path #"^\.\/" "")
         ;; For absolute paths, extract everything after the source root
         ;; For relative paths, match against source-paths directly
         match-root (fn [root]
                      (cond
                        ;; Relative path starting with root/
                        (str/starts-with? norm (str root "/"))
                        (subs norm (+ (count root) 1))
                        ;; Absolute path containing /root/
                        (str/includes? norm (str "/" root "/"))
                        (let [i (str/index-of norm (str "/" root "/"))]
                          (subs norm (+ i (count root) 2)))
                        :else nil))
         ;; Try all source paths, pick longest match (most specific root)
         matched (->> source-paths
                      (keep (fn [root] (when-let [rel (match-root root)]
                                         {:root root :relative rel})))
                      (sort-by #(count (:root %)) >)
                      first)
         relative (if matched
                    (:relative matched)
                    ;; Last-resort fallback: old /src/ splitting behavior
                    (let [src-idx (str/index-of norm "/src/")]
                      (if src-idx
                        (subs norm (+ src-idx 5))
                        (if (str/starts-with? norm "src/")
                          (subs norm 4)
                          norm))))]
     (-> relative
         (str/replace #"\.clj[sc]?$" "")
         (str/replace "/" ".")
         (str/replace "_" "-")))))

(defn- project-root-for-source
  [file source-paths]
  (let [path (-> file io/file .getCanonicalFile .toPath)
        roots (or (seq source-paths) ["src" "test" "dev"])]
    (or
      (some (fn [ancestor]
              (when (some #(.startsWith path (.resolve ancestor (str %))) roots)
                (.toFile ancestor)))
            (take-while some? (iterate #(.getParent %) (.getParent path))))
      (some (fn [ancestor]
              (when (.exists (io/file (.toFile ancestor) "deps.edn"))
                (.toFile ancestor)))
            (take-while some? (iterate #(.getParent %) (.getParent path))))
      (some-> file io/file .getParentFile .getParentFile))))

(defn- add-require-to-ns
  "Add one optional alias and refer entry while preserving source trivia."
  [file-source new-ns-name alias referred]
  (require-ops/insert-into-require-sorted file-source
                                          (symbol new-ns-name)
                                          (some-> alias symbol)
                                          (mapv symbol referred)))

(defn- source-line-chunks
  "Split source into line chunks while retaining every line terminator."
  [source]
  (let [chunks (vec (str/split source #"(?<=\n)" -1))]
    (if (and (seq chunks) (= "" (peek chunks)))
      (pop chunks)
      chunks)))

(defn- removal-range
  [line-chunks {:keys [comment-start end-line]}]
  (let [trailing-line end-line
        end (if (and (< trailing-line (count line-chunks))
                     (str/blank? (nth line-chunks trailing-line)))
              (inc trailing-line)
              trailing-line)]
    {:start comment-start :end end}))

(defn- remove-form-ranges
  "Remove every planned form against one immutable source snapshot.
   A following line is removed only when it is actually blank."
  [source form-texts]
  (let [line-chunks (source-line-chunks source)
        ranges (sort-by :start > (map #(removal-range line-chunks %) form-texts))]
    (->> ranges
         (reduce (fn [chunks {:keys [start end]}]
                   (when-not (<= 0 start end (count chunks))
                     (throw (ex-info "Extraction range is outside the source snapshot"
                                     {:error-type :invalid-extraction-range
                                      :start start
                                      :end end
                                      :line-count (count chunks)})))
                   (into (subvec chunks 0 start)
                         (subvec chunks end)))
                 line-chunks)
         (apply str))))

(defn- validate-complete-source!
  [file source]
  (try
    (parser/parse-string-all source)
    source
    (catch Exception e
      (throw (ex-info "Extraction candidate is not complete Clojure source"
                      {:error-type :invalid-extraction-candidate
                       :file file
                       :source-unchanged true}
                      e)))))

(defn compile-candidates
  "Purely compile and parse the complete source and target after extraction."
  [{:keys [source source-file target-file form-ranges target-source target-ns
           target-alias source-referred-forms]}]
  (let [source-without-forms (remove-form-ranges source form-ranges)
        future-source (if (seq source-referred-forms)
                        (add-require-to-ns source-without-forms
                                           target-ns
                                           target-alias
                                           source-referred-forms)
                        source-without-forms)]
    {:source (validate-complete-source! source-file future-source)
     :target (validate-complete-source! target-file target-source)}))

(def ^:private receipt-version 1)

(defn- canonical-path
  [file]
  (.getCanonicalPath (io/file file)))

(defn- receipt-refusal
  [receipt-out source-file target-file]
  (when receipt-out
    (let [receipt-path (canonical-path receipt-out)
          source-path (canonical-path source-file)
          target-path (canonical-path target-file)]
      (cond
        (not (str/ends-with? receipt-path ".edn"))
        {:error "Extraction receipt path must end in .edn"
         :error-type :invalid-extraction-receipt-path
         :source-unchanged true
         :target-unchanged true}

        (#{source-path target-path} receipt-path)
        {:error "Extraction receipt must not alias a source or target file"
         :error-type :extraction-receipt-alias
         :source-unchanged true
         :target-unchanged true}

        (.exists (io/file receipt-path))
        {:error "Extraction receipt already exists"
         :error-type :extraction-receipt-exists
         :source-unchanged true
         :target-unchanged true}))))

(defn- extraction-receipt
  [{:keys [source-file target-file original-source future-source target-source]}]
  {:receipt-version receipt-version
   :operation :extract!
   :source {:file (canonical-path source-file)
            :source-hash (structural-lens/source-hash original-source)
            :result-hash (structural-lens/source-hash future-source)
            :original-source original-source
            :result-source future-source}
   :target {:file (canonical-path target-file)
            :absent-before true
            :result-hash (structural-lens/source-hash target-source)
            :result-source target-source}
   :inverse {:operation :undo-extract!}})

(defn- publish-receipt!
  [receipt-out receipt]
  (when receipt-out
    (let [receipt-file (io/file receipt-out)]
      (.mkdirs (.getParentFile (.getAbsoluteFile receipt-file)))
      (file-ops/atomic-write! receipt-file (pr-str receipt))
      (when-not (= receipt (edn/read-string (slurp receipt-file)))
        (throw (ex-info "Extraction receipt read-back verification failed"
                        {:error-type :extraction-receipt-read-back-failed})))
      (canonical-path receipt-file))))

;; ============================================================
;; Pure: Build extraction plan
;; ============================================================

;; @spec MCP-OP-PLAN-006
(defn compile-plan
  "Purely compile an extraction plan from one source snapshot and a captured
  workspace source map. No file, process, clock, or registry access occurs."
  [{:keys [file source forms to target-ns workspace-sources require-policy
           public-forms derive-required-public-forms]
    :or {workspace-sources {} require-policy :minimal public-forms []
         derive-required-public-forms false}}]
  (let [lines (vec (str/split-lines source))
        ol (outline/outline-source file source)
        all-forms (:forms ol)
        source-ns (some-> (:ns ol) str)
        form-names (set (map str forms))
        matched (->> all-forms
                     (filter #(and (contains? form-names (str (:name %)))
                                   (not= 'declare (:type %))))
                     vec)
        missing (set/difference
                  form-names
                  (set (map #(str (:name %)) matched)))]
    (cond
      (seq missing)
      {:error (str "Forms not found: " (str/join ", " (sort missing)))}

      (nil? source-ns)
      {:error "Could not determine source namespace"}

      (nil? target-ns)
      {:error "Could not determine target namespace"}

      :else
      (let [src-zloc (z/of-string source {:track-position? true})
            ns-zloc (loop [location src-zloc]
                      (when location
                        (if (and (z/list? location)
                                 (= "ns" (some-> location z/down z/string)))
                          location
                          (recur (z/right location)))))
            ns-form-text (when ns-zloc (z/string ns-zloc))
            zloc (analyze/string->zloc source)
            extracted-names (set (map #(str (:name %)) matched))
            topo-order (let [topology (analyze/topological-sort zloc)]
                         (->> (:sorted topology)
                              (filter extracted-names)))
            form-texts
            (->> (sort-by :line matched)
                 (mapv
                   (fn [form]
                     (let [form-start
                           (let [index (dec (dec (:line form)))]
                             (loop [line-index index]
                               (if (neg? line-index)
                                 0
                                 (if (str/starts-with?
                                       (str/trim (nth lines line-index "")) ";")
                                   (recur (dec line-index))
                                   (inc line-index)))))
                           form-end (:end-line form)]
                       {:name (str (:name form))
                        :type (str (:type form))
                        :line (:line form)
                        :end-line form-end
                        :comment-start form-start
                        :text (str/join "\n"
                                        (subvec lines form-start form-end))}))))
            texts-by-name (into {} (map (juxt :name identity) form-texts))
            ordered-texts (mapv #(get texts-by-name %) topo-order)
            header-result
            (extract-header/compile-target-header
              {:source-ns-form ns-form-text
               :target-ns target-ns
               :form-sources (mapv :text ordered-texts)
               :require-policy require-policy})
            alias-result (if (= :copy-all require-policy)
                           {:ok true :aliases {}}
                           (extract-header/source-aliases ns-form-text))
            target-alias (when (and (:ok alias-result)
                                    (not= :copy-all require-policy))
                           (extract-header/allocate-alias
                             target-ns (:aliases alias-result)))
            remaining-callers
            (extract-header/remaining-source-callers source extracted-names)
            source-referred
            (extract-header/source-referred-forms remaining-callers)
            private-form-names
            (->> matched
                 (filter #(forms/private-form? (str (:type %))))
                 (map #(str (:name %)))
                 set)
            supported-public-form-names
            (->> matched
                 (filter #(= "defn-" (str (:type %))))
                 (map #(str (:name %)))
                 set)
            required-public-forms
            (set/intersection private-form-names (set source-referred))
            requested-public-forms
            (if derive-required-public-forms
              required-public-forms
              (set (map str public-forms)))
            invalid-public-forms
            (set/difference requested-public-forms private-form-names)
            unsupported-public-forms
            (set/difference requested-public-forms
                            supported-public-form-names)
            missing-required-public-forms
            (set/difference required-public-forms requested-public-forms)
            publicized-texts
            (if (seq unsupported-public-forms)
              ordered-texts
              (mapv (fn [form]
                      (if (contains? requested-public-forms (:name form))
                        (update form :text publicize-defn-source)
                        form))
                    ordered-texts))
            new-file-content
            (when (:ok header-result)
              (str
                (str/join "\n\n"
                          (concat [(:ns-form header-result)]
                                  (map :text publicized-texts)))
                "\n"))
            captured-sources (assoc workspace-sources (str file) source)
            other-files
            (->> captured-sources
                 (remove #(= (str file) (str (key %))))
                 (filter (fn [[_ content]]
                           (some #(str/includes? content (str %))
                                 extracted-names)))
                 (map (comp str key))
                 sort
                 vec)
            subjects (mapv #(str source-ns "/" %) (sort extracted-names))
            quoted-proof (quoted-var-refs/scan-sources
                           captured-sources subjects)]
        (cond
          (seq invalid-public-forms)
          {:error "public-forms must name selected private forms"
           :error-type :invalid-public-forms
           :invalid-public-forms (vec (sort invalid-public-forms))}

          (seq unsupported-public-forms)
          {:error "One or more selected private forms cannot be publicized losslessly"
           :error-type :unsupported-public-forms
           :unsupported-public-forms (vec (sort unsupported-public-forms))}

          (not (:ok header-result))
          header-result

          (not (:ok alias-result))
          alias-result

          (not (:ok quoted-proof))
          (assoc quoted-proof
                 :error "Quoted Var caller proof failed; extraction was not planned")

          :else
          {:file file
           :to to
           :source-ns source-ns
           :target-ns target-ns
           :target-alias target-alias
           :require-policy (:require-policy header-result)
           :copied-require-count (:copied-require-count header-result)
           :target-requires (:target-requires header-result)
           :omitted-target-requires (:omitted-target-requires header-result)
           :remaining-source-callers remaining-callers
           :source-referred-forms source-referred
           :required-public-forms (vec (sort required-public-forms))
           :missing-required-public-forms
           (vec (sort missing-required-public-forms))
           :public-forms (vec (sort requested-public-forms))
           :source-require-added (boolean (seq source-referred))
           :forms-to-extract (mapv :name form-texts)
           :form-count (count matched)
           :lines-extracted
           (reduce + (map #(- (:end-line %) (dec (:comment-start %)))
                          form-texts))
           :new-file-preview
           (let [preview-lines (str/split-lines new-file-content)]
             (if (> (count preview-lines) 20)
               (str (str/join "\n" (take 20 preview-lines)) "\n... ("
                    (count preview-lines) " lines total)")
               new-file-content))
           :callers-to-review other-files
           :quoted-var-references
           (mapv #(select-keys % [:subject :file :line :character
                                  :reference-authority])
                 (:locations quoted-proof))
           :_source source
           :_source-hash (structural-lens/source-hash source)
           :_new-file-content new-file-content
           :_form-texts form-texts
           :_source-referred-forms source-referred})))))

(defn plan
  "Capture one workspace snapshot and delegate extraction decisions to
  compile-plan. This is the filesystem shell, not the pure planner."
  [{:keys [file forms to source-paths require-policy]
    :or {require-policy :minimal}}]
  (try
    (let [source (slurp file)
          target-ns (file-path->ns-name to source-paths)
          project-root (project-root-for-source file source-paths)
          source-canonical-path (.getCanonicalPath (io/file file))
          workspace-sources
          (->> (file-seq (io/file project-root))
               (filter #(.isFile %))
               (filter #(re-matches #".*\.clj[sc]?$" (.getName %)))
               (remove #(= source-canonical-path (.getCanonicalPath %)))
               (remove #(str/includes? (.getPath %) "/.git/"))
               (map (fn [workspace-file]
                      [(.getPath workspace-file) (slurp workspace-file)]))
               (into (sorted-map)))]
      (compile-plan
        {:file file
         :source source
         :forms forms
         :to to
         :target-ns target-ns
         :workspace-sources workspace-sources
         :require-policy require-policy}))
    (catch Exception error
      {:ok false
       :error-type :extraction-snapshot-failed
       :error (.getMessage error)
       :source-unchanged true
       :target-unchanged true})))

;; ============================================================
;; Effects: Execute the extraction
;; ============================================================

(defn execute!
  "Execute an extraction plan.
   Both future files are compiled and parsed before either file is written.
   The source write is hash-fenced; a failed commit restores the original
   source and removes the newly-created target."
  [{:keys [file to receipt-out] :as opts}]
  (let [p (plan opts)]
    (if (:error p)
      p
      (let [original-source (:_source p)
            original-source-hash (:_source-hash p)
            new-content (:_new-file-content p)
            form-texts (:_form-texts p)
            source-referred-forms (:_source-referred-forms p)
            target-alias (:target-alias p)
            target-ns (:target-ns p)
            candidates (compile-candidates
                         {:source original-source
                          :source-file file
                          :target-file to
                          :form-ranges form-texts
                          :target-source new-content
                          :target-ns target-ns
                          :target-alias target-alias
                          :source-referred-forms source-referred-forms})
            updated-source (:source candidates)
            target-file (io/file to)
            source-file (io/file file)
            receipt-error (receipt-refusal receipt-out source-file target-file)
            receipt (extraction-receipt
                      {:source-file source-file
                       :target-file target-file
                       :original-source original-source
                       :future-source updated-source
                       :target-source new-content})]
        (cond
          receipt-error
          receipt-error

          (.exists target-file)
          {:error "Extraction target already exists"
           :error-type :extraction-target-exists
           :file to
           :source-unchanged true
           :target-unchanged true}

          (not= original-source-hash
                (structural-lens/source-hash (slurp source-file)))
          {:error "Extraction source changed after planning"
           :error-type :stale-extraction-source
           :file file
           :source-unchanged true
           :target-unchanged true}

          :else
          (do
            (.mkdirs (.getParentFile (.getAbsoluteFile target-file)))
            (try
              (file-ops/atomic-write! target-file new-content)
              (file-ops/atomic-write! source-file updated-source)
              (when-not (and (= new-content (slurp target-file))
                             (= updated-source (slurp source-file)))
                (throw (ex-info "Extraction read-back verification failed"
                                {:error-type :extraction-read-back-failed})))
              (let [receipt-file (publish-receipt! receipt-out receipt)]
                (cond->
                  {:file file
                   :to to
                   :target-requires (:target-requires p)
                   :omitted-target-requires (:omitted-target-requires p)
                   :remaining-source-callers (:remaining-source-callers p)
                   :source-referred-forms source-referred-forms
                   :log (vec (concat
                               [{:action :create-file
                                 :file to
                                 :forms (count form-texts)
                                 :lines (count (str/split-lines new-content))}]
                               (map (fn [form]
                                      {:action :remove-form
                                       :form (:name form)
                                       :from-line (:line form)})
                                    (sort-by :line > form-texts))
                               (when (seq source-referred-forms)
                                 [{:action :add-require
                                   :ns target-ns
                                   :alias target-alias
                                   :refer source-referred-forms}])))
                   :verified {:source-hash
                              (structural-lens/source-hash original-source)
                              :source-result-hash
                              (structural-lens/source-hash updated-source)
                              :target-result-hash
                              (structural-lens/source-hash new-content)
                              :parsed true
                              :atomic-write true
                              :read-back true}
                   :callers-to-review (:callers-to-review p)
                   :quoted-var-references (:quoted-var-references p)
                   :summary {:forms-extracted (count form-texts)
                             :new-file-lines (count (str/split-lines new-content))
                             :source-require-added
                             (boolean (seq source-referred-forms))
                             :callers-to-review (count (:callers-to-review p))
                             :quoted-var-references
                             (count (:quoted-var-references p))}}
                  receipt-file (assoc :receipt-file receipt-file)))
              (catch Exception commit-error
                (let [source-restored?
                      (try
                        (file-ops/atomic-write! source-file original-source)
                        (= original-source (slurp source-file))
                        (catch Exception _ false))
                      target-removed?
                      (or (not (.exists target-file))
                          (and (= new-content (slurp target-file))
                               (.delete target-file)))
                      receipt-file (some-> receipt-out io/file)
                      receipt-removed?
                      (or (nil? receipt-file)
                          (not (.exists receipt-file))
                          (and (= (pr-str receipt) (slurp receipt-file))
                               (.delete receipt-file)))]
                  (throw (ex-info "Extraction commit failed and was rolled back"
                                  {:error-type :extraction-commit-failed
                                   :source-restored source-restored?
                                   :target-removed target-removed?
                                   :receipt-removed receipt-removed?
                                   :source-unchanged source-restored?}
                                  commit-error)))))))))))

(defn undo!
  "Undo one successful extraction while both result files still match its receipt."
  [{:keys [receipt]}]
  (try
    (let [receipt-data (edn/read-string (slurp receipt))
          source (:source receipt-data)
          target (:target receipt-data)
          source-file (io/file (:file source))
          target-file (io/file (:file target))]
      (cond
        (= :compiled-extraction (:operation receipt-data))
        ((requiring-resolve 'clj-surgeon.mcp-extraction/undo!) receipt-data)

        (not= receipt-version (:receipt-version receipt-data))
        {:error "Unsupported extraction receipt version"
         :error-type :invalid-extraction-receipt
         :source-unchanged true
         :target-unchanged true}

        (not= :extract! (:operation receipt-data))
        {:error "Receipt is not an extraction receipt"
         :error-type :invalid-extraction-receipt
         :source-unchanged true
         :target-unchanged true}

        (or (not (.exists source-file)) (not (.exists target-file)))
        {:error "Extraction result files are missing"
         :error-type :stale-extraction-result
         :source-unchanged true
         :target-unchanged true}

        (not= (:result-hash source)
              (structural-lens/source-hash (slurp source-file)))
        {:error "Extraction source no longer matches the receipt"
         :error-type :stale-extraction-result
         :file (:file source)
         :source-unchanged true
         :target-unchanged true}

        (not= (:result-hash target)
              (structural-lens/source-hash (slurp target-file)))
        {:error "Extraction target no longer matches the receipt"
         :error-type :stale-extraction-result
         :file (:file target)
         :source-unchanged true
         :target-unchanged true}

        :else
        (let [original-source (validate-complete-source!
                                (:file source)
                                (:original-source source))
              result-source (:result-source source)
              result-target (:result-source target)]
          (if-not (.delete target-file)
            {:error "Could not remove the extraction target"
             :error-type :extraction-undo-delete-failed
             :source-unchanged true
             :target-unchanged true}
            (try
              (file-ops/atomic-write! source-file original-source)
              (when-not (= (:source-hash source)
                           (structural-lens/source-hash (slurp source-file)))
                (throw (ex-info "Extraction undo read-back verification failed"
                                {:error-type :extraction-undo-read-back-failed})))
              {:ok true
               :operation :undo-extract!
               :receipt (canonical-path receipt)
               :verified {:source-restored true
                          :source-hash (:source-hash source)
                          :target-absent (not (.exists target-file))
                          :read-back true}}
              (catch Exception undo-error
                (let [source-restored-to-result?
                      (try
                        (file-ops/atomic-write! source-file result-source)
                        (= (:result-hash source)
                           (structural-lens/source-hash (slurp source-file)))
                        (catch Exception _ false))
                      target-restored?
                      (try
                        (file-ops/atomic-write! target-file result-target)
                        (= (:result-hash target)
                           (structural-lens/source-hash (slurp target-file)))
                        (catch Exception _ false))]
                  (throw (ex-info "Extraction undo failed and its result was restored"
                                  {:error-type :extraction-undo-failed
                                   :source-result-restored source-restored-to-result?
                                   :target-result-restored target-restored?}
                                  undo-error)))))))))
    (catch java.io.FileNotFoundException _
      {:error "Extraction receipt does not exist"
       :error-type :extraction-receipt-not-found
       :source-unchanged true
       :target-unchanged true})
    (catch RuntimeException error
      (if (:error-type (ex-data error))
        (throw error)
        {:error "Extraction receipt is invalid"
         :error-type :invalid-extraction-receipt
         :source-unchanged true
         :target-unchanged true}))))
