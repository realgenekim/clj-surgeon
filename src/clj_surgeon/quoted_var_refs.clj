(ns clj-surgeon.quoted-var-refs
  "Lossless supplemental proof for Var-quoted references omitted by semantic
  providers. Structural evidence remains explicitly distinct from LSP evidence."
  (:require
   [clj-surgeon.analyze :as analyze]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-source-anchor :as source-anchor]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str]
   [rewrite-clj.zip :as z]))

(def ^:private source-extensions #{"clj" "cljs" "cljc"})
(def ^:private excluded-directory-names
  #{".cpcache" ".git" ".clj-kondo" ".lsp" ".shadow-cljs" "node_modules"
    "out" "target"})
(def ^:private max-candidate-files 256)

(defn- relative-to-root
  [^java.nio.file.Path root file]
  (let [path (.toRealPath (.toPath file)
                          (make-array java.nio.file.LinkOption 0))]
    (when (.startsWith path root)
      (-> (.relativize root path)
          str
          (str/replace "\\" "/")))))

(defn- clojure-source-file?
  [file]
  (and (.isFile file)
       (contains? source-extensions
                  (some-> (.getName file)
                          (str/split #"\.")
                          last))))

(defn- workspace-files
  [project-root]
  (let [root (mcp-paths/real-root project-root)
        roots (->> (source-anchor/workspace-source-roots project-root)
                   (map #(-> root (.resolve (str %)) .toFile))
                   (filter #(.exists %))
                   (map #(.getCanonicalFile %))
                   distinct
                   (sort-by #(-> % .toPath .getNameCount))
                   (reduce (fn [outermost candidate]
                             (if (some #(.startsWith (.toPath candidate)
                                                     (.toPath %))
                                       outermost)
                               outermost
                               (conj outermost candidate)))
                           []))]
    (->> roots
         (mapcat
           (fn [source-root]
             (tree-seq
               (fn [file]
                 (and (.isDirectory file)
                      (not (contains? excluded-directory-names
                                      (.getName file)))))
               (fn [file] (or (seq (.listFiles file)) []))
               source-root)))
         (filter clojure-source-file?)
         (filter #(relative-to-root root (.getCanonicalFile %)))
         (map #(.getCanonicalFile %))
         distinct
         (sort-by #(.getPath %))
         vec)))

(defn- top-level-locations
  [root]
  (->> (iterate z/right root)
       (take-while some?)))

(defn- namespace-context
  [relative-file source]
  (let [root (z/of-string source {:track-position? true})
        ns-zloc (->> (top-level-locations root)
                     (filter #(and (z/list? %)
                                   (= 'ns (some-> % z/sexpr first))))
                     first)
        ns-name (some-> (outline/outline-source relative-file source) :ns str)]
    {:root root
     :namespace ns-name
     :aliases (or (some-> ns-zloc analyze/parse-ns-aliases) {})}))

(defn- inert-ancestor?
  [location]
  (->> (iterate z/up (z/up location))
       (take-while some?)
       (some (fn [ancestor]
               (let [form (try (z/sexpr ancestor)
                               (catch Exception _ nil))]
                 (or (= :uneval (z/tag ancestor))
                     (and (seq? form)
                          (contains? #{'quote 'clojure.core/quote}
                                     (first form)))))))))

(defn- target-symbols
  [target-namespace target-name current-namespace aliases]
  (into #{(symbol target-namespace target-name)}
        (concat
          (when (= target-namespace current-namespace)
            [(symbol target-name)])
          (for [[alias namespace] aliases
                :when (= target-namespace (str namespace))]
            (symbol (str alias) target-name)))))

(defn- var-reference-symbol
  [location]
  (let [form (try (z/sexpr location)
                  (catch Exception _ nil))]
    (when (and (seq? form)
               (= 2 (count form))
               (= 'var (first form))
               (symbol? (second form))
               (not (inert-ancestor? location)))
      (second form))))

(defn references-in-source-for-subjects
  "Return exact Var-reference locations for several fully qualified subjects.
  Parse and traverse the captured source once. Results remain grouped in the
  supplied subject order. Strings, comments, and quoted data are excluded."
  [relative-file source subjects]
  (let [{:keys [root namespace aliases]} (namespace-context relative-file source)
        distinct-subjects (vec (distinct subjects))
        subjects-by-symbol
        (reduce
          (fn [result subject]
            (let [[target-namespace target-name] (str/split subject #"/" 2)]
              (reduce
                (fn [index accepted]
                  (update index accepted (fnil conj []) subject))
                result
                (target-symbols target-namespace target-name namespace aliases))))
          {}
          distinct-subjects)
        references-by-subject
        (loop [location root
               references (zipmap distinct-subjects (repeat []))]
          (if (z/end? location)
            references
            (let [reference (var-reference-symbol location)
                  matching-subjects (get subjects-by-symbol reference)
                  node-meta (meta (z/node location))
                  evidence
                  (when (seq matching-subjects)
                    {:file relative-file
                     :line (:row node-meta)
                     :character (:col node-meta)
                     :range {:start {:line (dec (:row node-meta))
                                     :character (dec (:col node-meta))}
                             :end {:line (dec (:end-row node-meta))
                                   :character (dec (:end-col node-meta))}}
                     :source (z/string location)
                     :reference-authority :structural-var-quote})]
              (recur
                (z/next location)
                (reduce
                  (fn [result subject]
                    (update result subject conj evidence))
                  references
                  matching-subjects)))))]
    (mapv
      (fn [[subject reference]]
        (assoc reference :subject subject))
      (for [subject subjects
            reference (get references-by-subject subject)]
        [subject reference]))))

(defn references-in-source
  "Return exact Var-reference locations for one fully qualified subject.
  Strings, comments, and quoted data are excluded."
  [relative-file source subject]
  (mapv #(dissoc % :subject)
        (references-in-source-for-subjects
          relative-file source [subject])))

(defn scan-sources
  "Purely scan one captured map of relative file names to source strings for
  quoted Var references. The result is deterministic by file and subject.
  file-paths can map each source key to the authoritative path reported by an
  effectful adapter."
  ([sources subjects]
   (scan-sources
     sources subjects
     {:max-candidate-files max-candidate-files}))
  ([sources subjects {:keys [file-paths max-candidate-files]
                      :or {file-paths {}
                           max-candidate-files max-candidate-files}}]
   (let [names (set (map #(second (str/split % #"/" 2)) subjects))]
     (try
       (let [candidates
             (->> sources
                  (sort-by key)
                  (keep (fn [[file source]]
                          (when (some #(str/includes? source %) names)
                            {:file (str file)
                             :file-path (str (get file-paths file file))
                             :source source})))
                  vec)]
         (if (> (count candidates) max-candidate-files)
           {:ok false
            :error-type :quoted-var-scan-budget-exceeded
            :error "Quoted Var proof exceeded its candidate-file budget"
            :candidate-file-count (count candidates)
            :limit max-candidate-files
            :source-unchanged true}
           (let [locations
                 (mapcat
                   (fn [{:keys [file file-path source]}]
                     (for [reference
                           (references-in-source-for-subjects
                             file source subjects)]
                       (merge reference
                              {:file_path file-path
                               :source_sha256
                               (structural-lens/source-hash source)
                               :role :reference})))
                   candidates)]
             {:ok true
              :locations (vec locations)
              :sources (into {}
                             (map (juxt :file-path :source) candidates))
              :scanned-file-count (count sources)
              :candidate-file-count (count candidates)
              :reference-count (count locations)})))
       (catch Exception error
         {:ok false
          :error-type :quoted-var-scan-failed
          :error (.getMessage error)
          :source-unchanged true})))))

(defn scan-workspace
  "Scan one confined workspace for quoted Var references to subjects.
  Returns exact locations and a source cache so downstream capture does not
  reread matching files."
  [project-root subjects read-source]
  (let [root (mcp-paths/real-root project-root)
        files (workspace-files project-root)
        relative-paths
        (into {}
              (map (fn [file]
                     [(relative-to-root root file) (.getPath file)]))
              files)]
    (try
      (scan-sources
        (into (sorted-map)
              (map (fn [file]
                     [(relative-to-root root file) (read-source file)]))
              files)
        subjects
        {:file-paths relative-paths
         :max-candidate-files max-candidate-files})
      (catch Exception error
        {:ok false
         :error-type :quoted-var-scan-failed
         :error (.getMessage error)
         :source-unchanged true}))))
