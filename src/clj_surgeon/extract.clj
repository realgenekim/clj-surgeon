(ns clj-surgeon.extract
  "Extract forms from one namespace to a new namespace file.

   Algorithm:
   1. Find named forms with exact boundaries
   2. Copy source ns form as template (over-include requires, don't under-include)
   3. Write new file with forms in topological order
   4. Remove forms from source file
   5. Add require for new namespace to source file
   6. Report callers that may need updating

   Does NOT: detect bare references, fix circular deps, update callers.
   The compiler catches bare refs instantly. The AI fixes what the compiler reports.

   ALL PLANNING IS PURE. Only execute! writes files."
  (:require [clj-surgeon.outline :as outline]
            [clj-surgeon.analyze :as analyze]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

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

(defn- file-path->ns-name
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

(defn- ns-name->alias
  "Derive a short alias from a namespace name.
   writer.state.distillery → distillery"
  [ns-name]
  (last (str/split ns-name #"\.")))

(defn- rewrite-ns-name
  "Replace the namespace name in a (ns ...) form string.
   Returns the new ns form string."
  [ns-form-str old-ns-name new-ns-name]
  (let [zloc (z/of-string ns-form-str)
        ;; Find the ns name token (second child of the ns form)
        name-zloc (some-> zloc z/down z/right)]
    (if (and name-zloc (= (z/string name-zloc) old-ns-name))
      (-> name-zloc
          (z/replace (n/token-node (symbol new-ns-name)))
          z/root-string)
      ;; Fallback: text replace
      (str/replace ns-form-str old-ns-name new-ns-name))))

(defn- add-require-to-ns
  "Add a require entry to a file's source string.
   Finds (:require ...) in the ns form and appends the new entry."
  [file-source new-ns-name alias]
  (let [require-entry (str "[" new-ns-name " :as " alias "]")
        ;; Simple approach: find last "])" pattern in the :require block
        ;; and insert before the closing paren
        lines (str/split-lines file-source)
        ;; Find the line with (:require
        req-idx (first (keep-indexed
                        (fn [i line]
                          (when (str/includes? line "(:require")
                            i))
                        lines))
        ;; Find the closing bracket of :require (line ending with ]))
        close-idx (when req-idx
                    (first (keep-indexed
                            (fn [i line]
                              (when (and (>= i req-idx)
                                         (re-find #"\]\s*\)\s*\)?\s*$" (str/trim line)))
                                i))
                            lines)))]
    (if close-idx
      ;; Insert new require entry before the closing line
      (let [indent "            "
            new-lines (concat (take close-idx lines)
                              [(str indent require-entry)]
                              (drop close-idx lines))]
        (str/join "\n" new-lines))
      ;; Fallback: just append after ns form
      file-source)))

;; ============================================================
;; Pure: Build extraction plan
;; ============================================================

(defn plan
  "Build a plan to extract forms from source to a new namespace.
   Returns EDN with the plan or {:error ...}.
   PURE — no side effects."
  [{:keys [file forms to source-paths]}]
  (let [source (slurp file)
        lines (vec (str/split-lines source))
        total-lines (count lines)
        ol (outline/outline file)
        all-forms (:forms ol)
        source-ns (str (:ns ol))
        target-ns (file-path->ns-name to source-paths)
        target-alias (ns-name->alias target-ns)
        form-names (set (map str forms))
        ;; Find the requested forms (skip declares)
        matched (->> all-forms
                     (filter #(and (contains? form-names (str (:name %)))
                                   (not= 'declare (:type %))))
                     vec)
        missing (clojure.set/difference
                 form-names
                 (set (map #(str (:name %)) matched)))]
    (cond
      (seq missing)
      {:error (str "Forms not found: " (str/join ", " (sort missing)))}

      (nil? source-ns)
      {:error "Could not determine source namespace"}

      :else
      (let [;; Get the source ns form text from the zipper (outline strips it)
            src-zloc (z/of-string source {:track-position? true})
            ns-zloc (loop [z src-zloc]
                      (when z
                        (if (and (z/list? z)
                                 (= "ns" (some-> z z/down z/string)))
                          z
                          (recur (z/right z)))))
            ns-form-text (when ns-zloc (z/string ns-zloc))
            ;; Topologically sort the extracted forms
            zloc (analyze/file->zloc file)
            deps (analyze/intra-ns-deps zloc)
            ;; Filter deps to just extracted forms
            extracted-names (set (map #(str (:name %)) matched))
            topo-order (let [t (analyze/topological-sort zloc)]
                         (->> (:sorted t)
                              (filter extracted-names)))
            ;; Get form text for each extracted form (with comment headers)
            form-texts (->> (sort-by :line matched)
                            (mapv (fn [f]
                                    (let [form-start
                                          (let [idx (dec (dec (:line f)))]
                                            (loop [i idx]
                                              (if (neg? i) 0
                                                  (if (str/starts-with?
                                                       (str/trim (nth lines i "")) ";")
                                                    (recur (dec i))
                                                    (inc i)))))
                                          form-end (:end-line f)]
                                      {:name (str (:name f))
                                       :line (:line f)
                                       :end-line form-end
                                       :comment-start form-start
                                       :text (str/join "\n"
                                                       (subvec lines
                                                               form-start
                                                               form-end))}))))
            texts-by-name (into {} (map (juxt :name identity) form-texts))
            ;; Order texts by topo sort
            ordered-texts (mapv #(get texts-by-name %) topo-order)
            ;; Build new file content
            new-ns-form (when ns-form-text
                          (rewrite-ns-name ns-form-text source-ns target-ns))
            new-file-content (str/join "\n\n"
                                       (concat [new-ns-form]
                                               (map :text ordered-texts)
                                               [""]))
            ;; Find other .clj files that might need require updates
            project-root (or (some-> file io/file .getParentFile .getParent) ".")
            other-files (->> (file-seq (io/file project-root))
                             (filter #(.isFile %))
                             (filter #(re-matches #".*\.clj$" (.getName %)))
                             (remove #(= (.getPath %) (.getPath (io/file file))))
                             (remove #(str/includes? (.getPath %) "/.git/"))
                             (filter (fn [f]
                                       (try
                                         (let [content (slurp f)]
                                           (some #(str/includes? content (str %))
                                                 extracted-names))
                                         (catch Exception _ false))))
                             (mapv #(.getPath %)))]
        {:file file
         :to to
         :source-ns source-ns
         :target-ns target-ns
         :target-alias target-alias
         :forms-to-extract (mapv :name form-texts)
         :form-count (count matched)
         :lines-extracted (reduce + (map #(- (:end-line %) (dec (:comment-start %)))
                                         form-texts))
         :new-file-preview (let [lines (str/split-lines new-file-content)]
                             (if (> (count lines) 20)
                               (str (str/join "\n" (take 20 lines)) "\n... ("
                                    (count lines) " lines total)")
                               new-file-content))
         :callers-to-review other-files
         :_new-file-content new-file-content
         :_form-texts form-texts}))))

;; ============================================================
;; Effects: Execute the extraction
;; ============================================================

(defn execute!
  "Execute an extraction plan.
   1. Write new namespace file
   2. Remove forms from source
   3. Add require for new namespace to source
   Returns a log of actions."
  [{:keys [file forms to] :as opts}]
  (let [p (plan opts)]
    (if (:error p)
      p
      (let [new-content (:_new-file-content p)
            form-texts (:_form-texts p)
            target-alias (:target-alias p)
            target-ns (:target-ns p)
            log (atom [])]
        ;; 1. Create target directory and write new file
        (let [target-file (io/file to)]
          (.mkdirs (.getParentFile target-file))
          (spit target-file new-content)
          (swap! log conj {:action :create-file :file to
                           :forms (count form-texts)
                           :lines (count (str/split-lines new-content))}))
        ;; 2. Remove forms from source (bottom to top)
        (let [sorted-forms (sort-by :line > form-texts)]
          (doseq [f sorted-forms]
            (let [current-lines (vec (str/split-lines (slurp file)))
                  form-start (:comment-start f)
                  form-end (:end-line f)
                  ;; Remove the form + any trailing blank line
                  end-idx (min (inc form-end) (count current-lines))
                  remaining (into (subvec current-lines 0 form-start)
                                  (subvec current-lines end-idx))]
              (spit file (str/join "\n" remaining))
              (swap! log conj {:action :remove-form :form (:name f)
                               :from-line (:line f)}))))
        ;; 3. Add require for new namespace to source ns form
        (let [current-source (slurp file)
              updated (add-require-to-ns current-source target-ns target-alias)]
          (spit file updated)
          (swap! log conj {:action :add-require
                           :ns target-ns :alias target-alias}))
        ;; Return result
        {:file file
         :to to
         :log @log
         :callers-to-review (:callers-to-review p)
         :summary {:forms-extracted (count form-texts)
                   :new-file-lines (count (str/split-lines new-content))
                   :callers-to-review (count (:callers-to-review p))}}))))
