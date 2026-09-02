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
   [clj-surgeon.extract-rewire :as extract-rewire]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.mcp-process :as process-env]
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

;; @spec MCP-OP-EXTRACT-014
(defn source-paths-in-root
  "Read :paths and alias :extra-paths from one project root's deps.edn.

  The root is explicit because a server answering for a workspace must read
  THAT workspace's layout: reading `deps.edn` from the process working
  directory derives namespace names from whichever project the server itself
  happens to live in. Returns nil when the root has no readable deps.edn."
  [root]
  (let [f (if root (io/file (str root) "deps.edn") (io/file "deps.edn"))]
    (when (.exists f)
      (try
        (let [deps (edn/read-string (slurp f))]
          (seq (distinct
                 (concat (:paths deps)
                         (mapcat :extra-paths (vals (:aliases deps)))))))
        (catch Exception _ nil)))))

(defn- source-paths-from-deps-edn
  "Read :paths and alias :extra-paths from deps.edn in the working directory."
  []
  (source-paths-in-root nil))

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

;; @spec MCP-OP-EXTRACT-014
(defn workspace-relative-path
  "Pure: one path expressed relative to a workspace root, when it is inside it.

  Deriving a namespace from an ABSOLUTE path lets a directory named `src`
  ABOVE the workspace decide the name."
  [root path]
  (let [root (some-> root str (str/replace #"/+$" ""))
        path (str path)]
    (if (and (seq root) (str/starts-with? path (str root "/")))
      (subs path (inc (count root)))
      path)))

;; @spec MCP-OP-EXTRACT-014
(defn workspace-target-ns
  "Derive a target namespace from a path, read against ONE workspace root."
  [root path]
  (file-path->ns-name (workspace-relative-path root path)
                      (or (source-paths-in-root root) ["src" "test" "dev"])))

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

;; @spec MCP-OP-EXTRACT-004
(defn- add-require-to-ns
  "Add one libspec for the target while preserving source trivia.

  The :refer list is COUPLED to rewiring and is never a free choice. When the
  extraction qualified the remaining call sites itself, it emits a bare
  [ns :as alias] -- a refer list would be a second place to maintain, and
  undoing it by hand was in 4 of 4 rf1 runs' repair patch. When rewiring is
  disabled the caller owns those sites, so the refer list must stay or the
  source would not compile."
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

(defn namespace-form-text
  "Pure: the exact text of one source's single top-level ns form, or nil."
  [source]
  (loop [location (z/of-string source)]
    (when location
      (if (and (z/list? location)
               (= "ns" (some-> location z/down z/string)))
        (z/string location)
        (recur (z/right location))))))

(defn replace-ns-form
  "Pure: swap one source's ns form for new text, preserving every other byte."
  [source new-ns-form]
  (loop [location (z/of-string source)]
    (cond
      (nil? location) source
      (and (z/list? location)
           (= "ns" (some-> location z/down z/string)))
      (-> location (z/replace (parser/parse-string new-ns-form)) z/root-string)
      :else (recur (z/right location)))))

(defn- source-body
  "Pure: one source with its ns form elided, so header analysis never counts a
  libspec's own name as a reference to it."
  [source ns-text]
  (if ns-text (str/replace-first source ns-text "") source))

;; @spec MCP-OP-EXTRACT-005
;; @spec MCP-OP-EXTRACT-006
(defn ns-name-of
  "Pure: the namespace symbol one source declares, as a string, or nil."
  [source]
  (some-> (namespace-form-text source)
          z/of-string z/down z/right z/string))

;; @spec MCP-OP-EXTRACT-015
(defn header-guarantees
  "Pure: the properties the extraction's header rewrite GUARANTEES.

  A receipt that reports only counts cannot be acted on by a reader who was not
  driving the run: in cohort rf1 the receipt named forms-extracted,
  new-file-lines, callers-to-review and target-requires, and a cold reader still
  could not tell whether the docstring was copied, the imports pruned, or the
  visibility derived, because no field said so. Every claim here is a property,
  not a tally."
  [{:keys [require-policy doc target-alias refer-emitted
           target-imports omitted-target-imports promoted-forms
           target-requires omitted-target-requires]}]
  {:docstring (cond
                (= :copy-all require-policy) :copied-from-source
                (str/blank? (str doc)) :none
                :else :caller-supplied)
   :requires-kept (if (= :copy-all require-policy)
                    :copied-exactly
                    (vec target-requires))
   :requires-pruned (if (= :copy-all require-policy)
                      []
                      (vec omitted-target-requires))
   :imports-kept (if (= :copy-all require-policy)
                   :copied-exactly
                   (vec target-imports))
   :imports-pruned (if (= :copy-all require-policy)
                     []
                     (vec omitted-target-imports))
   :visibility-derived (vec promoted-forms)
   :alias target-alias
   :refer (if (seq refer-emitted) (vec refer-emitted) :none)})

;; @spec MCP-OP-EXTRACT-016
(defn classify-callers
  "Pure: split the files that mention a moved name into three STATES.

  `rewired` were repointed by this extraction. `unresolved` require the source
  namespace and still need a human or a second call -- including any whose
  header could not be read. `mentions-only` provably do not require the source
  namespace, so they need nothing; they are reported separately rather than as
  outstanding work, because rf1's `callers-to-review` count was read as a list
  of work the tool had in fact already done."
  [{:keys [files sources source-ns moved-vars rewired-files]}]
  (let [moved (set moved-vars)]
    (reduce
      (fn [acc file]
        (cond
          (contains? rewired-files file) acc
          :else
          (let [source (get sources file)
                facts (when source
                        (some-> (namespace-form-text source)
                                (extract-header/required-namespace-facts source-ns)))]
            (cond
              (nil? facts)
              (update acc :unresolved conj
                      {:file file :reason :no-readable-namespace-form})

              (false? (:provable? facts))
              (update acc :unresolved conj
                      {:file file :reason (:reason facts)})

              (not (:required? facts))
              (update acc :mentions-only conj file)

              (= :all (:referred facts))
              (update acc :unresolved conj
                      {:file file :reason :refer-all-cannot-be-proved})

              (seq (filter moved (:referred facts)))
              (update acc :unresolved conj
                      {:file file
                       :reason :moved-vars-are-referred-not-alias-qualified
                       :vars (vec (sort (filter moved (:referred facts))))})

              :else
              (update acc :mentions-only conj file)))))
      {:unresolved [] :mentions-only []}
      files)))

;; @spec MCP-OP-EXTRACT-017
(defn private-plan-field?
  "Pure: a compiled-plan key that is the executor's working state, never output.

  Minted here, so every surface that publishes a plan filters by the same rule."
  [field]
  (str/starts-with? (name field) "_"))

;; @spec MCP-OP-EXTRACT-017
(def receipt-tail-fields
  "The ONLY compiled-plan keys a reader-facing receipt may carry beyond its
  ordered head. An allowlist, not a denylist: the 347 KB receipt happened
  because a new internal key had only to be forgotten to leak, and the leak was
  three whole caller files plus the whole source file. A key absent from this
  set cannot reach a reader, whatever it is named."
  ;; :omitted-target-requires is deliberately absent: requires were already
  ;; pruned correctly before this change, so the list of ones NOT copied is
  ;; diagnostic, not a guarantee. Imports ARE listed both ways in :header,
  ;; because copying four unrelated imports is the defect rf1 measured.
  [:file :to :source-ns :require-policy :source-referred-forms
   :form-count :lines-extracted
   :preview-path :missing-required-public-forms
   ;; :log is deliberately absent: an action queue -- create-file, nine
   ;; remove-form entries, add-require -- restating what :new-file-preview and
   ;; :header already state as the resulting STATE.
   :source-require-added :rewire-callers :verified :receipt-file :undo])

(defn- compile-expr-for
  "Pure: the expression that requires every touched namespace."
  [namespaces]
  (str "(require " (str/join " " (map #(str "'" %) namespaces))
       ") (println :compile-ok)"))

(defn- classpath-args-for
  "Pure: the argv that resolves the project classpath.

  Two steps, not `clojure -M<alias> -e`: an alias may carry :main-opts, and on
  this very repository `-M:clj-surgeon/mcp-test -e ...` runs the whole test
  suite instead of compiling. Resolve the classpath, then run clojure.main
  against it."
  [alias]
  (cond-> ["clojure" "-Spath"]
    (not (str/blank? (str alias))) (conj (str "-A" alias))))

;; @spec MCP-OP-EXTRACT-020
(defn attribute-compile-failure
  "Pure: decide whether a failed compile is attributable to THIS change.

  A receipt whose evidence source cannot see its own subject must say
  `:unverified`, never `false`. A missing dependency, or an error raised inside
  a namespace this extraction never touched, says the classpath could not load
  the project at all -- reporting that as `:ok false` would tell a reader to
  undo work that is in fact correct."
  [output touched-files]
  (let [named (set (map #(second %) (re-seq #"\(([A-Za-z0-9_.\-]+\.clj[cs]?):" output)))
        ours (set (map #(last (str/split (str %) #"/")) touched-files))
        foreign (seq (remove ours named))]
    (cond
      (re-find #"Could not locate .* on classpath" output)
      {:ok :unverified :reason :classpath-incomplete}

      foreign
      {:ok :unverified :reason :failure-outside-the-changed-files
       :raised-in (vec (sort foreign))}

      :else {:ok false})))

(defn- compile-command-for
  "Pure: the exact command a reader can run, matching what the apply runs."
  ([namespaces] (compile-command-for namespaces nil))
  ([namespaces alias]
   (str "CP=$(" (str/join " " (classpath-args-for alias))
        ") && java -cp \"$CP\" clojure.main -e \""
        (compile-expr-for namespaces) "\"")))

;; @spec MCP-OP-EXTRACT-019
(defn- project-build-file
  "The nearest build file at a project root, or nil."
  [root]
  (some #(let [f (io/file root %)] (when (.exists f) (.getPath f)))
        ["deps.edn" "project.clj" "bb.edn"]))

;; @spec MCP-OP-EXTRACT-019
(defn compile-check!
  "Run the SAME command the receipt prints, as one bounded subprocess, and
  report what it proved.

  Compiling is the only way this transaction's correctness gets checked, so the
  apply performs it rather than leaving a reader to discover that nothing was
  verified. It is a subprocess on purpose: loading the rewritten namespaces into
  the running process would mutate the very namespaces a server is serving."
  [{:keys [namespaces root timeout-ms alias touched-files]
    :or {timeout-ms 30000}}]
  (let [printed (compile-command-for namespaces alias)
        base {:namespaces (vec namespaces) :command printed}
        run! (fn [argv ms]
               (try
                 (process-env/run-bounded!
                   {:command argv :cwd (str root) :timeout-ms ms
                    :visible-byte-limit (* 256 1024)})
                 (catch Exception error {:launch-error (.getMessage error)})))]
    (if-not (project-build-file root)
      (assoc base :checked false :status :skipped
             :reason :no-project-build-file)
      (let [cp (run! (classpath-args-for alias) timeout-ms)
            classpath (some-> (:out cp) str/trim not-empty)
            result (if (and (:finished? cp) (zero? (long (or (:exit cp) 1)))
                            classpath)
                     (run! ["java" "-cp" classpath "clojure.main" "-e"
                            (compile-expr-for namespaces)]
                           timeout-ms)
                     {:launch-error
                      (str "could not resolve the project classpath: "
                           (str/trim (str (:err cp) (:launch-error cp))))})
            output (str/trim (str (:err result) (:out result)))
            tail (if (> (count output) 400)
                   (str "..." (subs output (- (count output) 400)))
                   output)]
        (cond
          (:launch-error result)
          (assoc base :checked false :status :unverified
                 :reason :compiler-unavailable :output-tail (:launch-error result))

          (not (:finished? result))
          (assoc base :checked false :status :unverified
                 :reason :compile-did-not-finish :output-tail tail)

          (zero? (long (or (:exit result) 1)))
          (assoc base :checked true :status :run :ok true :exit 0)

          :else
          ;; @spec MCP-OP-EXTRACT-020
          (merge base {:checked true :status :run :exit (:exit result)
                       :output-tail tail}
                 (attribute-compile-failure output touched-files)))))))

;; @spec MCP-OP-EXTRACT-018
(defn target-outline
  "Pure: a BOUNDED description of the new file -- its ns form and the name,
  kind and line range of each form. Never the file's text: on the rf1 fixture
  the whole new file is 6.6 KB and the whole source 78 KB, and an agent that
  gets a receipt too long to read ignores the receipt."
  [ns-form form-texts]
  (let [ns-lines (count (str/split-lines ns-form))]
    {:ns-form ns-form
     :forms (first
              (reduce
                (fn [[acc line] form]
                  (let [n (count (str/split-lines (:text form)))]
                    [(conj acc {:name (:name form)
                                ;; the FINAL kind, after any promotion: a
                                ;; receipt that still said defn- would deny the
                                ;; visibility change it just made
                                :type (or (some-> (:text form) z/of-string
                                                  z/down z/string)
                                          (:type form))
                                :lines [line (+ line (dec n))]})
                     (+ line n 1)]))
                [[] (+ ns-lines 2)]
                form-texts))}))

;; @spec MCP-OP-EXTRACT-015
;; @spec MCP-OP-EXTRACT-016
(defn receipt-map
  "Pure: one receipt a reader who did not drive the run can act on.

  Ordered, and ordered deliberately: what happened (:applied), to what
  (:target-ns/:target-file), what is now GUARANTEED about the headers
  (:header/:source-header), what state the callers are in
  (:*-rewired/:callers-unresolved/:complete), what has NOT been checked
  (:compile), and only then the tallies. Every caller key names a STATE, never
  a queue of history: `callers-to-review` was read by a cold model as work it
  had to do by hand, when the tool had already done all of it.

  Built with array-map so the printed order is the order above; do not assoc
  onto the result, which would convert it to an unordered map."
  [{:keys [applied plan candidates would extra]}]
  (let [root (:_project-root plan)
        rel #(workspace-relative-path root %)
        rewired (mapv (fn [{:keys [file old-alias rewrites require-action]}]
                        {:file (rel file) :old-alias old-alias :sites rewrites
                         :require-action require-action})
                      (:_caller-plans plan))
        source-rewired (->> (:source-rewrites candidates)
                            (group-by :owner)
                            (map (fn [[owner entries]]
                                   {:owner owner
                                    :vars (vec (sort (map :var entries)))
                                    :sites (reduce + (map :count entries))}))
                            (sort-by :owner)
                            vec)
        classification (:_caller-classification plan)
        unresolved (vec (:unresolved classification))
        namespaces (:_touched-namespaces plan)]
    (apply array-map
      (concat
        [:applied (boolean applied)]
        (when-not applied [:would would])
        [:target-ns (:target-ns plan)
         :target-file (:to plan)
         :header (:_header plan)
         :source-header {:requires-removed (vec (:removed-requires candidates))
                         :imports-removed (vec (:removed-imports candidates))
                         :narrowing-note (:narrowing-note candidates)}
         :source-callers-rewired source-rewired
         :external-callers-rewired rewired
         :callers-unresolved unresolved
         :complete (empty? unresolved)
         :compile (or (:compile plan)
                      {:checked false
                       :status :not-run
                       :will-check (boolean (:_will-check plan))
                       :namespaces namespaces
                       :command (compile-command-for namespaces)})
         :new-file-preview (:_target-outline plan)
         :quoted-var-references-unrewired (:quoted-var-references plan)
         :note "quoted-var-references-unrewired: Vars reached by quoting, never rewritten here."
         :callers-mentions-only (mapv rel (:mentions-only classification))
         ;; no :summary: every tally it held is a count of a vector printed
         ;; above it, and a receipt that restates its own contents is longer
         ;; without being clearer.
         :history {:note (str "callers-to-review is now the three caller "
                              "fields above; remaining-source-callers is "
                              "source-callers-rewired.")}]
        ;; appended, never assoc'ed: assoc past eight entries would convert the
        ;; array-map to an unordered one and lose the reading order above
        (mapcat identity extra)))))

(defn compile-candidates
  "Purely compile and parse the complete source and target after extraction.

  The future source is the extraction's complete result: the moved forms
  removed, the header narrowed to what the remaining body still uses, the
  remaining call sites of moved Vars alias-qualified, and the target require
  added."
  [{:keys [source source-file target-file form-ranges target-source target-ns
           target-alias source-referred-forms moved-sources remaining-callers
           rewire-callers]
    :or {rewire-callers true}}]
  (let [source-without-forms (remove-form-ranges source form-ranges)
        ns-text (namespace-form-text source-without-forms)
        narrowed (when ns-text
                   (extract-header/narrow-source-ns-header
                     ns-text
                     (or (seq moved-sources) [""])
                     [(source-body source-without-forms ns-text)]))
        ;; @spec MCP-OP-EXTRACT-005
        ;; @spec MCP-OP-EXTRACT-012
        ;; Narrowing only REMOVES entries the extraction itself made dead, so
        ;; being unable to prove a header is not a reason to fail an extraction
        ;; that is otherwise correct: it degrades to the unnarrowed header,
        ;; exactly the behaviour before this ratchet, and says so. (Target
        ;; header minimization still fails closed: those requires decide
        ;; whether the new namespace compiles at all.)
        narrowed (when (:ok narrowed) narrowed)
        narrowing-note
          (when-not narrowed
            "The source namespace header could not be proved and was left unnarrowed.")
        narrowed-source (if narrowed
                          (replace-ns-form source-without-forms
                                           (:ns-form narrowed))
                          source-without-forms)
        qualified (if (and rewire-callers (seq remaining-callers))
                    (extract-rewire/qualify-owner-call-sites
                      narrowed-source remaining-callers target-alias)
                    {:ok true :source narrowed-source})]
        (if-not (:ok qualified)
          qualified
          (let [qualified? (boolean (and rewire-callers (seq remaining-callers)))
                future-source (if (seq source-referred-forms)
                                (add-require-to-ns (:source qualified)
                                                   target-ns
                                                   target-alias
                                                   (if qualified?
                                                     []
                                                     source-referred-forms))
                                (:source qualified))]
            {:ok true
             :source (validate-complete-source! source-file future-source)
             :target (validate-complete-source! target-file target-source)
             :removed-requires (:removed-requires narrowed)
             :removed-imports (:removed-imports narrowed)
             :narrowing-note narrowing-note
             :source-rewrites (:rewrites qualified)
             :unmatched-rewrites (:unmatched qualified)}))))

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

;; @spec MCP-OP-EXTRACT-008
(defn- extraction-receipt
  [{:keys [source-file target-file original-source future-source target-source
           caller-plans]}]
  (cond->
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
     :inverse {:operation :undo-extract!}}
    (seq caller-plans)
    (assoc :callers
           (mapv (fn [{:keys [file original source]}]
                   {:file (canonical-path file)
                    :source-hash (structural-lens/source-hash original)
                    :result-hash (structural-lens/source-hash source)
                    :original-source original
                    :result-source source})
                 caller-plans))))

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
           public-forms derive-required-public-forms doc alias rewire-callers
           project-root]
    :or {workspace-sources {} require-policy :minimal public-forms []
         derive-required-public-forms false rewire-callers true}}]
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
            extracted-names (set (map #(str (:name %)) matched))
            ;; @spec MCP-OP-EXTRACT-001
            ;; The caller states the intended reading order; an internal
            ;; topological re-sort is a diff the caller did not ask for, and in
            ;; cohort rf1 it was the trigger an agent spent 15 returns chasing.
            caller-order (mapv str forms)
            ;; @spec MCP-OP-EXTRACT-013
            ;; Honouring the caller's order means the caller can state one that
            ;; needs a `declare`. Emitting that file would ship source that does
            ;; not compile, so it refuses and names the order that works.
            moved-deps (into {}
                             (map (juxt :name :depends-on))
                             (analyze/intra-ns-deps
                               (analyze/string->zloc source)))
            position (into {} (map-indexed (fn [i n] [n i])) caller-order)
            forward-references
            (vec (for [name caller-order
                       dependency (sort (get moved-deps name))
                       :when (and (contains? position dependency)
                                  (> (get position dependency)
                                     (get position name)))]
                   {:form name :depends-on dependency}))
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
            ordered-texts (mapv #(get texts-by-name %) caller-order)
            header-result
            (extract-header/compile-target-header
              {:source-ns-form ns-form-text
               :target-ns target-ns
               :form-sources (mapv :text ordered-texts)
               :require-policy require-policy
               :doc doc})
            alias-result (if (= :copy-all require-policy)
                           {:ok true :aliases {}}
                           (extract-header/source-aliases ns-form-text))
            ;; @spec MCP-OP-EXTRACT-004
            target-alias (cond
                           (not (str/blank? (str alias))) (str alias)
                           (and (:ok alias-result)
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
;; @spec MCP-OP-EXTRACT-011
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
                           captured-sources subjects)
            ;; @spec MCP-OP-EXTRACT-007
            ;; Every file the planner has already PROVED references a moved Var
            ;; is repointed here, in the same pure pass that proved it. A file
            ;; whose only mention of a moved name is a comment rewrites nothing
            ;; and is dropped, so no caller gains an unused require.
            caller-plans
            (when (and rewire-callers target-alias (seq other-files))
              (vec
                (keep
                  (fn [caller-file]
                    (let [caller-source (get captured-sources caller-file)
                          caller-ns-form (when caller-source
                                           (namespace-form-text caller-source))
                          old-alias (when caller-ns-form
                                      (extract-header/alias-for-namespace
                                        caller-ns-form source-ns))]
                      (when old-alias
                        (let [result (extract-rewire/requalify-caller
                                       {:source caller-source
                                        :old-alias old-alias
                                        :old-ns source-ns
                                        :target-ns target-ns
                                        :alias target-alias
                                        :moved-vars (vec (sort extracted-names))})]
                          (when (or (not (:ok result))
                                    (pos? (long (or (:rewrites result) 0))))
                            (assoc result :file caller-file
                                   :old-alias old-alias
                                   :original caller-source))))))
                  other-files)))
            caller-refusal (some #(when-not (:ok %) %) caller-plans)]
        (cond
          (seq forward-references)
          {:error (str "The declared form order would need a declare: "
                       (str/join ", "
                                 (map #(str (:form %) " before "
                                            (:depends-on %))
                                      forward-references)))
           :error-type :forward-reference-in-declared-order
           :forward-references forward-references
           :dependency-order
           (vec (->> (:sorted (analyze/topological-sort
                                (analyze/string->zloc source)))
                     (filter extracted-names)))
           :source-unchanged true
           :target-unchanged true}

          caller-refusal
          (assoc caller-refusal
                 :error (str "Caller rewiring could not be proved for "
                             (:file caller-refusal) ": "
                             (:error caller-refusal))
                 :source-unchanged true
                 :target-unchanged true)

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
           ;; @spec MCP-OP-EXTRACT-018
           :new-file-preview (target-outline (:ns-form header-result)
                                             publicized-texts)
           :callers-to-review other-files
           :rewire-callers (boolean rewire-callers)
           ;; @spec MCP-OP-EXTRACT-009
           ;; The dry run previews the SAME compiled plan the executor applies,
           ;; per file, so a caller can review the complete effect before any
           ;; byte moves.
           :_preview
           (let [candidates
                 (try
                   (compile-candidates
                     {:source source
                      :source-file file
                      :target-file to
                      :form-ranges form-texts
                      :target-source new-file-content
                      :target-ns target-ns
                      :target-alias target-alias
                      :source-referred-forms source-referred
                      :moved-sources (mapv :text publicized-texts)
                      :remaining-callers remaining-callers
                      :rewire-callers rewire-callers})
                   (catch Exception error
                     {:ok false
                      :error-type (or (:error-type (ex-data error))
                                      :invalid-extraction-candidate)
                      :error (.getMessage error)}))]
             {:target {:file to
                       :ns-form (:ns-form header-result)
                       :form-order (mapv :name publicized-texts)
                       :requires (:target-requires header-result)
                       :omitted-requires (:omitted-target-requires header-result)
                       :imports (:target-imports header-result)
                       :omitted-imports (:omitted-target-imports header-result)
                       :lines (count (str/split-lines new-file-content))}
              :source (if (:ok candidates)
                        {:file file
                         :ns-form (namespace-form-text (:source candidates))
                         :removed-requires (:removed-requires candidates)
                         :removed-imports (:removed-imports candidates)
                         :call-sites-qualified (:source-rewrites candidates)
                         :unmatched-call-sites (:unmatched-rewrites candidates)}
                        candidates)
              :callers (mapv #(select-keys % [:file :old-alias :rewrites
                                              :require-action])
                             caller-plans)})
           :caller-rewrites
           (mapv #(select-keys % [:file :old-alias :rewrites :require-action])
                 caller-plans)
           :quoted-var-references
           (mapv #(select-keys % [:subject :file :line :character
                                  :reference-authority])
                 (:locations quoted-proof))
           :_source source
           :_source-hash (structural-lens/source-hash source)
           :_new-file-content new-file-content
           :_form-texts form-texts
           :_source-referred-forms source-referred
           :_moved-sources (mapv :text publicized-texts)
           :_caller-plans (vec caller-plans)
           :_new-file-lines (count (str/split-lines new-file-content))
           :_will-check true
           :_project-root project-root
           :_target-outline (assoc (target-outline (:ns-form header-result)
                                                   publicized-texts)
                                   :lines (count (str/split-lines new-file-content)))
           ;; @spec MCP-OP-EXTRACT-015
           :_header
           (header-guarantees
             {:require-policy (:require-policy header-result)
              :doc doc
              :target-alias target-alias
              :refer-emitted (if (and rewire-callers (seq remaining-callers))
                               []
                               source-referred)
              :target-requires (:target-requires header-result)
              :omitted-target-requires (:omitted-target-requires header-result)
              :target-imports (:target-imports header-result)
              :omitted-target-imports (:omitted-target-imports header-result)
              :promoted-forms (vec (sort requested-public-forms))})
           ;; @spec MCP-OP-EXTRACT-016
           :_caller-classification
           (classify-callers
             {:files other-files
              :sources captured-sources
              :source-ns source-ns
              :moved-vars extracted-names
              :rewired-files (set (map :file caller-plans))})
           :_touched-namespaces
           (vec (concat [target-ns source-ns]
                        (keep #(ns-name-of (:original %)) caller-plans)))})))))

;; @spec MCP-OP-EXTRACT-016
(defn- apply-command-for
  "Pure: the exact :extract! invocation that applies one previewed plan."
  [{:keys [file forms to public doc alias rewire-callers]}]
  (str "clj-surgeon :op :extract! :file " file
       " :forms '" (pr-str (vec forms)) "'"
       " :to " to
       (when (seq public) (str " :public '" (pr-str (vec public)) "'"))
       (when alias (str " :alias " alias))
       (when doc " :doc \"<your docstring>\"")
       (when (false? rewire-callers) " :rewire-callers false")))

;; @spec MCP-OP-EXTRACT-009
;; @spec MCP-OP-EXTRACT-015
;; @spec MCP-OP-EXTRACT-016
(defn- dry-run-receipt
  "Reshape one compiled plan into the SAME receipt the executor emits, with
  :applied false and the command that would apply it. A preview a reader has to
  translate into the apply receipt is a second thing to learn."
  [compiled opts]
  (if (or (:error compiled) (not (map? compiled)))
    compiled
    (let [candidates (get-in compiled [:_preview :source])
          candidates (if (map? candidates) candidates {})]
      (receipt-map {:applied false
                    :plan compiled
                    :candidates
                    {:source-rewrites (:call-sites-qualified candidates)
                     :removed-requires (:removed-requires candidates)
                     :removed-imports (:removed-imports candidates)
                     :narrowing-note (:narrowing-note candidates)}
                    :would (apply-command-for opts)
                    ;; @spec MCP-OP-EXTRACT-017
                    :extra (select-keys compiled receipt-tail-fields)}))))

(defn plan-raw
  "Capture one workspace snapshot and delegate extraction decisions to
  compile-plan. This is the filesystem shell, not the pure planner.

  Returns the compiled plan unreshaped, for the executor; `plan` is the
  reader-facing dry run built on top of it."
  [{:keys [file forms to source-paths require-policy public public-forms doc
           alias rewire-callers]
    :as opts
    :or {require-policy :minimal rewire-callers true}}]
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
         :require-policy require-policy
         :project-root (str project-root)
         :public-forms (or public public-forms [])
         ;; @spec MCP-OP-EXTRACT-011
         ;; When the caller does not name the promotions, DERIVE them. The
         ;; planner already computes exactly which moved private forms a
         ;; remaining owner must call; shipping one of them still private, as
         ;; rf1 did twice, is a success receipt for a namespace that will not
         ;; compile. Deriving is only a default: a supplied :public stays
         ;; authoritative, and a wrong one still refuses.
         :derive-required-public-forms
         (not (or (contains? opts :public) (contains? opts :public-forms)))
         :doc doc
         :alias alias
         :rewire-callers rewire-callers}))
    (catch Exception error
      {:ok false
       :error-type :extraction-snapshot-failed
       :error (.getMessage error)
       :source-unchanged true
       :target-unchanged true})))

;; @spec MCP-OP-EXTRACT-009
;; @spec MCP-OP-EXTRACT-016
(defn plan
  "Preview one extraction as the SAME receipt the executor emits, with
  :applied false and the command that would apply it."
  [opts]
  (dry-run-receipt (plan-raw opts) opts))

;; ============================================================
;; Effects: Execute the extraction
;; ============================================================

(defn execute!
  "Execute an extraction plan.
   Both future files are compiled and parsed before either file is written.
   The source write is hash-fenced; a failed commit restores the original
   source and removes the newly-created target."
  [{:keys [file to receipt-out] :as opts}]
  (let [p (plan-raw opts)]
    (if (:error p)
      p
      (let [original-source (:_source p)
            original-source-hash (:_source-hash p)
            new-content (:_new-file-content p)
            form-texts (:_form-texts p)
            source-referred-forms (:_source-referred-forms p)
            target-alias (:target-alias p)
            target-ns (:target-ns p)
            caller-plans (:_caller-plans p)
            candidates (compile-candidates
                         {:source original-source
                          :source-file file
                          :target-file to
                          :form-ranges form-texts
                          :target-source new-content
                          :target-ns target-ns
                          :target-alias target-alias
                          :source-referred-forms source-referred-forms
                          :moved-sources (:_moved-sources p)
                          :remaining-callers (:remaining-source-callers p)
                          :rewire-callers (:rewire-callers p)})
            updated-source (:source candidates)
            target-file (io/file to)
            source-file (io/file file)
            receipt-error (receipt-refusal receipt-out source-file target-file)
            receipt (extraction-receipt
                      {:source-file source-file
                       :target-file target-file
                       :original-source original-source
                       :future-source updated-source
                       :target-source new-content
                       :caller-plans caller-plans})]
        (cond
          (false? (:ok candidates))
          candidates

          receipt-error
          receipt-error

          ;; @spec MCP-OP-EXTRACT-008
          ;; Every caller file is hash-fenced against the snapshot the plan was
          ;; compiled from, exactly as the source already is.
          (some (fn [{:keys [file original]}]
                  (not= original (slurp (io/file file))))
                caller-plans)
          {:error "Extraction caller changed after planning"
           :error-type :stale-extraction-caller
           :files (mapv :file caller-plans)
           :source-unchanged true
           :target-unchanged true}

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
              (doseq [{:keys [file source]} caller-plans]
                (file-ops/atomic-write! (io/file file) source))
              (when-not (and (= new-content (slurp target-file))
                             (= updated-source (slurp source-file))
                             (every? (fn [{:keys [file source]}]
                                       (= source (slurp (io/file file))))
                                     caller-plans))
                (throw (ex-info "Extraction read-back verification failed"
                                {:error-type :extraction-read-back-failed})))
              (let [receipt-file (publish-receipt! receipt-out receipt)
                    ;; @spec MCP-OP-EXTRACT-019
                    ;; The last step of the transaction: compile what was
                    ;; written. A failure is reported honestly and NOT
                    ;; auto-reverted -- the receipt names how to revert.
                    compile-result
                    (if (false? (:compile-check opts))
                      {:checked false :status :not-run
                       :namespaces (:_touched-namespaces p)
                       :reason :disabled-by-caller}
                      (compile-check!
                        {:namespaces (:_touched-namespaces p)
                         :alias (:compile-alias opts)
                         :touched-files (concat [file to]
                                                (map :file caller-plans))
                         :root (project-root-for-source file
                                                        (:source-paths opts))}))]
                (receipt-map
                  {:applied true
                   :plan (assoc p :compile compile-result)
                   :candidates candidates
                   ;; @spec MCP-OP-EXTRACT-017
                   ;; The apply obeys the SAME allowlist as the dry run; two
                   ;; surfaces with two rules is how one of them leaks.
                   :extra
                   (select-keys
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
                   :source-require-added (boolean (seq source-referred-forms))}
                  receipt-file (assoc :receipt-file receipt-file)

                  (false? (:ok compile-result))
                  (assoc :undo
                         (if receipt-file
                           {:receipt receipt-file
                            :command (str "clj-surgeon :op :undo-extract! "
                                          ":receipt " receipt-file)
                            :note (str "the compile FAILED after the write; "
                                       "these bytes are on disk and were not "
                                       "reverted for you")}
                           {:receipt nil
                            :note (str "the compile FAILED after the write and "
                                       "no receipt was requested, so there is "
                                       "no guarded revert; re-run with "
                                       ":receipt-out <path>.edn to get one")})))
                   receipt-tail-fields)}))
              (catch Exception commit-error
                (let [source-restored?
                      (try
                        (file-ops/atomic-write! source-file original-source)
                        (= original-source (slurp source-file))
                        (catch Exception _ false))
                      callers-restored?
                      (every? (fn [{:keys [file original]}]
                                (try
                                  (file-ops/atomic-write! (io/file file) original)
                                  (= original (slurp (io/file file)))
                                  (catch Exception _ false)))
                              caller-plans)
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
                                   :callers-restored callers-restored?
                                   :target-removed target-removed?
                                   :receipt-removed receipt-removed?
                                   :source-unchanged (and source-restored?
                                                          callers-restored?)}
                                  commit-error)))))))))))

(defn undo!
  "Undo one successful extraction while both result files still match its receipt."
  [{:keys [receipt]}]
  (try
    (let [receipt-data (edn/read-string (slurp receipt))
          source (:source receipt-data)
          target (:target receipt-data)
          source-file (io/file (:file source))
          target-file (io/file (:file target))
          callers (vec (:callers receipt-data))
          stale-caller
          (some (fn [{:keys [file result-hash] :as caller}]
                  (let [f (io/file file)]
                    (when (or (not (.exists f))
                              (not= result-hash
                                    (structural-lens/source-hash (slurp f))))
                      caller)))
                callers)]
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

        ;; @spec MCP-OP-EXTRACT-008
        ;; A rewired caller is part of the extraction's result; undoing without
        ;; it would leave the workspace pointing at a namespace that no longer
        ;; exists.
        stale-caller
        {:error "Extraction caller no longer matches the receipt"
         :error-type :stale-extraction-result
         :file (:file stale-caller)
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
              (doseq [{:keys [file original-source]} callers]
                (file-ops/atomic-write! (io/file file) original-source))
              (when-not (and (= (:source-hash source)
                                (structural-lens/source-hash (slurp source-file)))
                             (every? (fn [{:keys [file source-hash]}]
                                       (= source-hash
                                          (structural-lens/source-hash
                                            (slurp (io/file file)))))
                                     callers))
                (throw (ex-info "Extraction undo read-back verification failed"
                                {:error-type :extraction-undo-read-back-failed})))
              {:ok true
               :operation :undo-extract!
               :receipt (canonical-path receipt)
               :verified {:source-restored true
                          :source-hash (:source-hash source)
                          :callers-restored (mapv :file callers)
                          :target-absent (not (.exists target-file))
                          :read-back true}}
              (catch Exception undo-error
                (let [source-restored-to-result?
                      (try
                        (file-ops/atomic-write! source-file result-source)
                        (= (:result-hash source)
                           (structural-lens/source-hash (slurp source-file)))
                        (catch Exception _ false))
                      _callers-restored-to-result?
                      (every? (fn [{:keys [file result-source]}]
                                (try
                                  (file-ops/atomic-write! (io/file file)
                                                          result-source)
                                  true
                                  (catch Exception _ false)))
                              callers)
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
