(ns clj-surgeon.core
  "ns-surgeon: structural operations on Clojure namespaces.

   A babashka CLI tool. Returns EDN.

   Usage:
     bb -m ns-surgeon.core :op :outline :file src/my/ns.clj
     bb -m ns-surgeon.core :op :mv :file src/my/ns.clj :form my-fn :before other-fn
     bb -m ns-surgeon.core :op :mv :file src/my/ns.clj :form my-fn :before other-fn :dry-run true"
  (:require
   [babashka.fs :as fs]
   [babashka.process]
   [clj-surgeon.analyze :as analyze]
   [clj-surgeon.census-discovery :as census-discovery]
   [clj-surgeon.cljc.analyze :as cljc-analyze]
   [clj-surgeon.cljc.merge :as cljc-merge]
   [clj-surgeon.cljc.require-ops :as cljc-req]
   [clj-surgeon.cljc.split :as cljc-split]
   [clj-surgeon.edit-dsl :as edit-dsl]
   [clj-surgeon.extract :as extract]
   [clj-surgeon.fix-declares :as fix-declares]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.forward-refs :as fwd]
   [clj-surgeon.intent-transaction :as intent-transaction]
   [clj-surgeon.move :as move]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.relation-census :as relation-census]
   [clj-surgeon.rename :as rename]
   [clj-surgeon.show-form :as show-form]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.string :as str]))

(defn run-outline [{:keys [file]}]
  (let [result (outline/outline file)
        ns-name (:ns result)
        forward-refs (when ns-name
                       (fwd/detect-forward-refs file ns-name))]
    (assoc result :forward-refs (or forward-refs []))))

(defn run-mv [{:as opts}]
  (move/move-form (cond-> opts (#{:mv-with-deps "mv-with-deps" ":mv-with-deps"} (:op opts)) (assoc :with-deps true))))

(defn run-edit [opts]
  (let [prepared (edit-dsl/prepare-edit-options opts)]
    (if (:error prepared)
      prepared
      (structural-lens/edit-file-with-evaluator prepared edit-dsl/evaluate-edit))))

(defn run-xray [opts]
  (let [prepared (edit-dsl/prepare-xray-options opts)]
    (if (:error prepared)
      prepared
      (edit-dsl/evaluate-xray (slurp (:file prepared)) prepared))))

(defn run-declares [{:keys [file]}]
  (let [;; Get declares from the OUTLINE (not deps — deps excludes declares)
        ol (outline/outline file)
        declares (->> (:forms ol)
                      (filter #(= 'declare (:type %))))
        ;; Use topo sort to find genuine cycles
        zloc (analyze/file->zloc file)
        topo (analyze/topological-sort zloc)
        truly-cyclic (set (:cycles topo))
        ;; Also check forward-refs to see which declares are still needed
        fwd (when (:ns ol)
              (set (map #(str (:name %))
                        (fwd/detect-forward-refs file (:ns ol)))))]
    {:file file
     :declares
     (mapv (fn [d]
             (let [name-str (str (:name d))
                   has-forward-ref? (contains? fwd name-str)
                   in-cycle? (contains? truly-cyclic name-str)]
               {:name name-str
                :line (:line d)
                :needed? (or in-cycle? has-forward-ref?)}))
           declares)
     :summary {:total (count declares)
               :removable (count (remove #(or (contains? truly-cyclic (str (:name %)))
                                              (contains? fwd (str (:name %))))
                                         declares))
               :needed (count (filter #(or (contains? truly-cyclic (str (:name %)))
                                           (contains? fwd (str (:name %))))
                                      declares))}}))

(defn run-deps [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)
        deps (analyze/intra-ns-deps zloc)]
    (if form
      (first (filter #(= form (:name %)) deps))
      deps)))

(defn run-topo [{:keys [file]}]
  (let [zloc (analyze/file->zloc file)]
    (analyze/topological-sort zloc)))

(defn run-closure [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)]
    (analyze/extraction-closure zloc form)))

(defn run-ls-deps [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)
        deps (analyze/intra-ns-deps zloc)]
    (analyze/dep-tree deps form)))

;; ============================================================
;; CLJC operations: merge, split, add-require
;; ============================================================

(defn run-cljc-merge
  "Merge parallel CLJ + CLJS files (same ns) into a single CLJC source.
   :clj  / :cljs — input file paths (required)
   :out — optional output path; omitted prints to stdout."
  [{:keys [clj cljs out] :as _opts}]
  (let [cljc-src (cljc-merge/merge-files (slurp clj) (slurp cljs))]
    (if out
      (do (spit out cljc-src)
          {:wrote out :bytes (count cljc-src)})
      cljc-src)))

(defn run-cljc-split
  "Split a CLJC file into parallel CLJ + CLJS sources.
   :file     — input CLJC path (required)
   :clj-out  — optional output CLJ path
   :cljs-out — optional output CLJS path
   When out paths are omitted, returns both contents in a map."
  [{:keys [file clj-out cljs-out] :as _opts}]
  (let [{:keys [clj cljs] :as result} (cljc-split/split-file (slurp file))]
    (cond-> (do (when clj-out (spit clj-out clj)) (when cljs-out (spit cljs-out cljs)) result) clj-out (assoc :wrote-clj clj-out) cljs-out (assoc :wrote-cljs cljs-out))))

(defn run-cljc-add-require
  "Add a require to a CLJC file at the given platform.
   :file     — input CLJC path (required)
   :platform — :clj | :cljs | :cljc (required)
   :ns       — namespace symbol to require (required)
   :as       — optional alias
   :out      — optional output path; omitted prints to stdout."
  [{:keys [file platform ns as out] :as _opts}]
  (let [updated (cljc-req/add-require (slurp file)
                                      {:platform platform
                                       :ns ns
                                       :as as})]
    (if out
      (do (spit out updated)
          {:wrote out :bytes (count updated)})
      updated)))

;; ============================================================
;; :ls-tree — directory-wide namespace map
;; ============================================================

(def ^:private skip-dirs
  "Directories to skip during project discovery."
  #{".git" ".cpcache" ".gitlibs" "target" "node_modules"
    ".clj-kondo" ".lsp" ".shadow-cljs" ".nrepl" ".idea" ".vscode"})

(defn- in-skip-dir?
  "True if the path (relative to root) passes through any skip directory."
  [path root]
  (let [rel (str (fs/relativize root path))]
    (boolean (some skip-dirs (str/split rel #"/")))))

(defn- find-build-files
  "Find deps.edn, project.clj, bb.edn under dir, skipping hidden/cache dirs.
   Uses system find with -prune for speed (~10x faster than fs/glob on large trees)."
  [dir]
  (try
    (let [prune-expr (str/join " -o "
                               (map #(str "-name " %) skip-dirs))
          cmd (format "find %s \\( %s \\) -prune -o \\( -name deps.edn -o -name project.clj -o -name bb.edn \\) -print"
                      (str dir) prune-expr)
          result (babashka.process/shell {:out :string :err :string :continue true}
                                         "sh" "-c" cmd)]
      (if (zero? (:exit result))
        (->> (str/split-lines (str/trim (:out result)))
             (remove str/blank?)
             sort
             vec)
        []))
    (catch Exception _e [])))

(defn source-paths-from-config
  "Pure: given a build filename and its parsed content, return source paths.
   Defaults to [\"src\"] when paths not specified."
  [filename content]
  (case filename
    "deps.edn"    (or (:paths content) ["src"])
    "bb.edn"      (or (:paths content) ["src"])
    "project.clj" (let [kvs (drop 3 content)
                        m (apply hash-map kvs)]
                    (or (:source-paths m) ["src"]))
    ["src"]))

(defn- extract-source-paths
  "I/O wrapper: read a build file and return its source paths."
  [build-file]
  (try
    (source-paths-from-config (str (fs/file-name build-file))
                              (read-string (slurp (str build-file))))
    (catch Exception _e ["src"])))

(defn- find-clj-files
  "Find all .clj/.cljs/.cljc files under a directory using system find."
  [dir]
  (when (fs/directory? dir)
    (try
      (let [result (babashka.process/shell
                     {:out :string :err :string :continue true}
                     "find" (str dir)
                     "-name" "*.clj" "-o" "-name" "*.cljs" "-o" "-name" "*.cljc")]
        (when (zero? (:exit result))
          (->> (str/split-lines (str/trim (:out result)))
               (remove str/blank?))))
      (catch Exception _e nil))))

(defn- discover-projects
  "Find projects under dir via build files. Returns [{:name :root :files}].
   Falls back to recursive scan if no build files found."
  [dir]
  (let [dir (fs/path dir)
        build-files (find-build-files dir)
        ;; Group by project root, keep first build file per root
        by-root (group-by #(str (fs/parent %)) build-files)]
    (if (seq by-root)
      (->> by-root
           (map (fn [[root files]]
                  (let [build-file (first files)
                        src-paths (extract-source-paths build-file)
                        root-path (fs/path root)
                        clj-files (->> src-paths
                                       (mapcat #(find-clj-files (fs/path root %)))
                                       (map str)
                                       sort
                                       vec)]
                    {:name (str (fs/file-name root-path))
                     :root (str root-path)
                     :files clj-files})))
           (remove #(empty? (:files %)))
           (sort-by :name)
           vec)
      ;; No build files — fallback to recursive scan
      (let [clj-files (->> (find-clj-files dir)
                           (remove #(in-skip-dir? % dir))
                           (map str)
                           sort
                           vec)]
        (when (seq clj-files)
          [{:name (str (fs/file-name dir))
            :root (str dir)
            :files clj-files}])))))

(defn- rg-available?
  "Check if ripgrep (rg) is on the PATH."
  []
  (try
    (let [r (babashka.process/shell {:out :string :err :string :continue true}
                                    "rg" "--version")]
      (zero? (:exit r)))
    (catch Exception _e false)))

(defn- grep-tree
  "Single recursive grep on a directory tree. Returns set of matching absolute paths.
   Uses ripgrep (rg) if available — faster and respects .gitignore.
   Falls back to system grep (MUCH slower on large trees)."
  [pattern dir]
  (when-not (rg-available?)
    (binding [*out* *err*]
      (println "WARNING: ripgrep (rg) not found. Falling back to grep (much slower).")
      (println "Install: brew install ripgrep  OR  apt install ripgrep")))
  (try
    (let [args (if (rg-available?)
                 ;; ripgrep: fast, respects .gitignore automatically
                 ;; Note: rg uses -i for case-insensitive (not -E which means encoding)
                 ["rg" "-li"
                  "-g" "*.clj" "-g" "*.cljs" "-g" "*.cljc"
                  "-g" "deps.edn" "-g" "project.clj" "-g" "bb.edn"
                  pattern (str dir)]
                 ;; fallback: system grep
                 (let [exclude-args (mapcat #(vector "--exclude-dir" %)
                                            [".git" ".cpcache" ".gitlibs" "target"
                                             "node_modules" ".clj-kondo" ".lsp" ".shadow-cljs"])]
                   (concat ["grep" "-rliE"
                            "--include=*.clj" "--include=*.cljs" "--include=*.cljc"
                            "--include=deps.edn" "--include=project.clj" "--include=bb.edn"]
                           exclude-args
                           [pattern (str dir)])))
          result (apply babashka.process/shell
                        {:out :string :err :string :continue true}
                        args)]
      (if (zero? (:exit result))
        (set (str/split-lines (str/trim (:out result))))
        #{}))
    (catch Exception _e #{})))

(defn filter-projects-by-hits
  "Pure: given a set of matching file paths and a list of projects, filter
   to relevant ones. If a project's build file matched, all its source files
   are included. Otherwise, only individually matching source files."
  [projects hits]
  (let [build-match? (fn [root]
                       (some hits
                             [(str root "/deps.edn")
                              (str root "/project.clj")
                              (str root "/bb.edn")]))]
    (->> projects
         (map (fn [{:keys [root files] :as project}]
                (if (build-match? root)
                  project
                  (assoc project :files (filterv #(hits (str %)) files)))))
         (remove #(empty? (:files %)))
         vec)))

(defn- safe-outline
  "Run outline on a file, returning error map on parse errors."
  [file]
  (try
    (outline/outline file)
    (catch Exception e
      {:file file :error (str (.getMessage e))})))

(defn- outline-all-files
  "Compute outlines for all files across projects, in parallel.
   Returns projects with :outlines — a vec of [file outline] pairs."
  [projects]
  (let [;; Collect all [project-idx file] pairs
        all-files (for [[pidx project] (map-indexed vector projects)
                        f (:files project)]
                    [pidx f])
        ;; Parse all files in parallel
        results (pmap (fn [[pidx f]]
                        [pidx f (safe-outline f)])
                      all-files)
        ;; Group back by project index
        by-project (group-by first results)]
    (mapv (fn [[pidx project]]
            (let [file-results (mapv (fn [[_ f outline]] [f outline])
                                     (get by-project pidx []))]
              (assoc project :outlines file-results)))
          (map-indexed vector projects))))

(defn format-file-text
  "Pure: format a single file's outline map as compact text lines."
  [result rel-path]
  (let [lines (StringBuilder.)]
    (.append lines (format "%s  %d lines, %d forms\n"
                           rel-path
                           (or (:lines result) 0)
                           (or (:form-count result) 0)))
    (when (:ns result)
      (.append lines (format "  ns: %s\n" (:ns result))))
    (when (seq (:requires result))
      (.append lines (format "  requires: %s\n"
                             (str/join " " (:requires result)))))
    (when (:error result)
      (.append lines (format "  ⚠ %s\n" (:error result))))
    (doseq [form (:forms result)
            :when (:name form)]
      (let [line-range (if (and (:line form) (:end-line form)
                                (not= (:line form) (:end-line form)))
                         (format "%d-%d" (:line form) (:end-line form))
                         (str (or (:line form) "?")))
            type-str (str (:type form))
            args-str (when (:args form) (str " " (:args form)))]
        (.append lines (format "  %s: %s %s%s\n"
                               line-range type-str (:name form)
                               (or args-str "")))))
    (str lines)))

(defn format-ls-tree-text
  "Pure: format ls-tree results as compact text for LLM/human scanning.
   Expects projects with :outlines already computed."
  [projects dir]
  (let [sb (StringBuilder.)
        multi-project? (> (count projects) 1)
        total-files (reduce + (map #(count (:outlines %)) projects))
        total-forms (reduce + (map (fn [p]
                                     (reduce + (map #(or (:form-count (second %)) 0)
                                                    (:outlines p))))
                                   projects))]
    (doseq [{:keys [name outlines]} projects
            :let [project-forms (reduce + (map #(or (:form-count (second %)) 0) outlines))]]
      (when multi-project?
        (.append sb (format "── %s (%d files, %d forms)\n\n"
                            name (count outlines) project-forms)))
      (doseq [[f result] outlines
              :let [rel-path (str (fs/relativize (fs/path dir) (fs/path f)))]]
        (.append sb (format-file-text result rel-path))
        (.append sb "\n")))
    (.append sb (format "── total: %d files, %d forms\n" total-files total-forms))
    (str sb)))

(defn format-ls-tree-edn
  "Pure: format ls-tree results as EDN vector.
   Expects projects with :outlines already computed."
  [projects dir]
  (vec
    (for [{:keys [outlines]} projects
          [f result] outlines
          :let [rel-path (str (fs/relativize (fs/path dir) (fs/path f)))]]
      (-> result
          (assoc :file rel-path)
          (dissoc :forward-refs)))))

(defn- find-nearest-build-file
  "Walk up from a file to find the nearest deps.edn/project.clj/bb.edn."
  [file-path stop-at]
  (loop [dir (fs/parent (fs/path file-path))]
    (when (and dir (str/starts-with? (str dir) (str stop-at)))
      (let [candidates [(str dir "/deps.edn") (str dir "/project.clj") (str dir "/bb.edn")]]
        (if-let [found (first (filter #(fs/exists? %) candidates))]
          {:build-file found :root (str dir)}
          (recur (fs/parent dir)))))))

(defn- discover-projects-grep
  "Fast path: use rg/grep results to build project list without globbing.
   For projects with matching deps.edn: find all their source files.
   For individual matching source files: group by nearest project root."
  [grep-hits dir]
  (let [build-files #{"deps.edn" "project.clj" "bb.edn"}
        {build-hits true src-hits false}
        (group-by #(contains? build-files (str (fs/file-name %))) grep-hits)

        ;; Projects with matching build files → find all their source files
        build-projects
        (->> (or build-hits [])
             (map (fn [bf]
                    (let [root (str (fs/parent (fs/path bf)))
                          src-paths (extract-source-paths bf)
                          clj-files (->> src-paths
                                         (mapcat #(find-clj-files (str root "/" %)))
                                         (remove nil?)
                                         sort
                                         vec)]
                      {:name (str (fs/file-name (fs/path root)))
                       :root root
                       :files clj-files})))
             (remove #(empty? (:files %))))

        build-roots (set (map :root build-projects))

        ;; Source file hits not in a build-matched project → group by nearest project root
        orphan-src-hits (remove #(some (fn [r] (str/starts-with? (str %) (str r "/")))
                                       build-roots)
                                (or src-hits []))
        src-projects
        (->> orphan-src-hits
             (map (fn [f]
                    (let [info (find-nearest-build-file f dir)]
                      (if info
                        (assoc info :file (str f))
                        ;; Loose file — no build file found; use parent dir as root
                        {:root (str (fs/parent (fs/path f)))
                         :file (str f)}))))
             (group-by :root)
             (map (fn [[root entries]]
                    {:name (str (fs/file-name (fs/path root)))
                     :root root
                     :files (vec (sort (map :file entries)))})))]
    (->> (concat build-projects src-projects)
         (sort-by :name)
         vec)))

;; @spec MCP-OP-CENSUS-032
(defn census-root
  "The CANONICAL workspace root the CLI census walks.

   The tool resolves its project root through `mcp-paths/real-root` before it
   walks anything. The CLI only absolutized, so `:dir` naming a symlink to a
   workspace handed the walk a link; the walk correctly does not follow links,
   visited nothing, and reported `no-fold-arms-found` on a tree the tool
   censused. Canonicalising here is what makes the two entrances answer the
   same question."
  [dir]
  (.getCanonicalPath (java.io.File. (str (fs/absolutize (or dir "."))))))

;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-024
;; @spec MCP-OP-CENSUS-027
;; @spec MCP-OP-CENSUS-028
;; @spec MCP-OP-CENSUS-032
(defn census-sources
  "Project-relative {:file :source} inputs that define fold arms.

   Discovery is the shared `census-discovery` kernel — the same walk the MCP
   tool runs, with the same canonical root, the same root confinement, the
   same skip-directory pruning, the same byte cap and the same ceiling. The
   CLI keeps no walk of its own: a rule that lives in one entrance is a rule
   the other entrance breaks.

   `declared?` also collects the top-level names of the files that define no
   arms — a door commonly lives in a helper namespace that defines none — and
   drops their text."
  ([dir file] (census-sources dir file {}))
  ([dir file {:keys [declared?]}]
   (let [discovered (when-not file (census-discovery/discover (census-root dir)))
         root (or (:root discovered) (census-root dir))
         relative #(str (fs/relativize root %))
         paths (if file
                 [(str (fs/absolutize file))]
                 (mapv #(str root "/" %) (:files discovered)))]
     (cond
       (and file (> (fs/size (first paths)) relation-census/max-source-bytes))
       {:oversized (relative (first paths))}

       ;; The ceiling is answered before a single source is read.
       (:exceeded? discovered)
       {:exceeded? true
        :observed (:observed discovered)
        :discovered discovered}

       :else
       (reduce
         (fn [acc p]
           (let [source (slurp p)]
             (if (relation-census/defines-arms? source)
               (update acc :inputs conj {:file (relative p)
                                         :source source})
               (cond-> acc
                 declared?
                 (update :declared into
                         (relation-census/source-declared-names source))))))
         {:scanned (count paths)
          :inputs []
          :declared #{}
          :oversized-skipped (vec (:oversized discovered))
          :skipped-outside-root (:skipped-outside-root discovered 0)}
         paths)))))

;; @spec MCP-OP-CENSUS-021
;; @spec MCP-OP-CENSUS-031
(defn census-plan-pool
  "The plan-phase map-fn and the pool that will actually run it.

   The CLI is a babashka tool and claypoole is a JVM dependency, so the bounded
   pool is resolved at run time: on the JVM the plan phase runs on census_pool's
   shutdown-bound pool, and under babashka, where that namespace cannot load,
   it runs serially. Either way the receipt reports the pool that ran, never the
   pool that was asked for."
  [requested]
  (let [size (if requested (relation-census/effective-pool-size requested) 1)
        pooled (when (> size 1)
                 (try
                   (when-let [pooled-map (requiring-resolve
                                           'clj-surgeon.census-pool/pooled-map)]
                     (pooled-map size))
                   (catch Throwable _ nil)))]
    (if pooled
      {:map-fn pooled :pool-size size}
      {:map-fn map :pool-size 1})))

;; @spec MCP-OP-CENSUS-015
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-021
(defn run-relation-census
  "Census collection writes inside fold arms. Reads only; writes nothing."
  [{:keys [dir file doors threads]}]
  (let [doors-arg doors
        pool (when (some? threads) (relation-census/coerce-pool-size threads))
        parsed-doors (if doors
                       (relation-census/parse-doors
                         (str/split (str doors) #",") nil)
                       relation-census/default-doors)
        want-declared? (boolean doors)
        scan (delay (census-sources dir file {:declared? want-declared?}))]
    (cond
      (and pool (not (:ok pool)))
      {:ok false
       :error-type :invalid-pool-size
       :error (str ":threads must be an integer between 1 and "
                   relation-census/max-pool-size
                   " (got " (pr-str threads) ")")
       :next-command "clj-surgeon :op :relation-census :dir . :threads 8"}

      (map? parsed-doors)
      {:ok false
       :error-type :unknown-door-symbol
       :error (str "Unknown identity door " (:invalid parsed-doors) ": "
                   (:why parsed-doors))
       :door (:invalid parsed-doors)
       :known-doors (vec (sort (map str relation-census/default-doors)))
       :next-command (str "clj-surgeon :op :relation-census :dir . :doors "
                          (str/join "," (sort (map str relation-census/default-doors))))}

      (:oversized @scan)
      {:ok false
       :error-type :source-too-large
       :error (str (:oversized @scan) " is larger than "
                   relation-census/max-source-bytes " bytes")
       :file (:oversized @scan)
       :maximum relation-census/max-source-bytes
       :next-command "clj-surgeon :op :relation-census :file <a source under the byte cap>"}

      ;; The same ceiling semantics as the MCP entrance: a tree the census may
      ;; not finish is refused with nothing read, never truncated into success.
      (:exceeded? @scan)
      {:ok false
       :error-type :too-many-candidate-files
       :error (str "This directory holds more than "
                   relation-census/max-scanned-files
                   " candidate Clojure sources (" (:observed @scan)
                   " seen before the walk stopped). The census reads at most "
                   relation-census/max-scanned-files
                   " and will not report a truncated tree as a complete census")
       :maximum relation-census/max-scanned-files
       :fits relation-census/max-scanned-files
       :observed (:observed @scan)
       :observed-at-least true
       :files-read 0
       :read-complete false
       :next-command "clj-surgeon :op :relation-census :dir <a narrower subtree>"}

      :else
      (let [inputs (:inputs @scan)
            doors parsed-doors]
        (if (empty? inputs)
          {:ok false
           :error-type :no-fold-arms-found
           :error "No file defines defmethod fold-event arms"
           :dir (census-root dir)
           :next-command "clj-surgeon :op :relation-census :dir <a directory with fold arms>"}
          (let [threads (when pool (:size pool))
                {:keys [map-fn pool-size]} (census-plan-pool threads)
                result (relation-census/plan
                         {:inputs inputs
                          :doors doors
                          :map-fn map-fn})
                confirmed (when (and want-declared? (:ok result))
                            (relation-census/parse-doors
                              (str/split (str doors-arg) #",")
                              (into (:declared @scan #{}) (:declared result))))]
            (cond
              (not (:ok result)) result

              ;; Whether a door is DEFINED can only be answered once the scan
              ;; has been parsed: confirm it against the plan's own :declared
              ;; plus the names read from the files that define no arms.
              (map? confirmed)
              {:ok false
               :error-type :unknown-door-symbol
               :error (str "Unknown identity door " (:invalid confirmed) ": "
                           (:why confirmed))
               :door (:invalid confirmed)
               :known-doors (vec (sort (map str relation-census/default-doors)))
               :next-command (str "clj-surgeon :op :relation-census :dir . :doors "
                                  (str/join "," (sort (map str relation-census/default-doors))))}

              :else
              (let [unrecognised (relation-census/unrecognised-summary
                                   (:unrecognised result) 5)
                    skipped (:oversized-skipped @scan)]
                (-> result
                    (dissoc :all-sites :declared :unrecognised)
                    (assoc :read-complete (empty? skipped)
                           :oversized-skipped
                           (when (seq skipped)
                             {:count (count skipped)
                              :files (vec (take 12 skipped))
                              :maximum relation-census/max-source-bytes})
                           :skipped-outside-root
                           (when (pos? (:skipped-outside-root @scan 0))
                             (:skipped-outside-root @scan))
                           :pool-size pool-size
                           :pool-size-requested (when (and threads
                                                          (> threads pool-size))
                                                  threads)
                           :unrecognised-calls unrecognised
                           :raw (filterv #(= :raw (:class %)) (:all-sites result))
                           :guarded (filterv #(= :guarded (:class %)) (:all-sites result))
                           :unknown (filterv #(= :unknown (:class %)) (:all-sites result))
                           :next-action
                           (cond
                             (pos? (get-in result [:counts :raw] 0))
                             "review the raw sites: each is a collection write in a fold arm with no dominating recognised guard"
                             (pos? (get-in result [:counts :unknown] 0))
                             "review the unknown sites: this census version declines to decide them"
                             (pos? (:count unrecognised 0))
                             (str "no site is unguarded, but " (:count unrecognised)
                                  " call(s) inside arms are not modelled by this census version ("
                                  (str/join ", " (take 3 (map :call (:examples unrecognised))))
                                  "): a write behind one of them is not a site here")
                             :else "none")))))))))))

(defn run-ls-tree [{:keys [dir format grep] :as _opts}]
  (when-not dir
    (println "Error: :dir is required for :ls-tree")
    (System/exit 1))
  (let [dir (str (fs/absolutize dir))
        projects (if grep
                   ;; Fast path: rg first, skip expensive directory globbing
                   (let [hits (grep-tree grep dir)]
                     (discover-projects-grep hits dir))
                   ;; Full scan: discover all projects
                   (discover-projects dir))]
    (if (empty? projects)
      (do (println (format "No Clojure files found under %s%s"
                           dir (when grep (str " matching '" grep "'"))))
          (System/exit 1))
      (let [projects (outline-all-files projects)]
        (if (= format :edn)
          (format-ls-tree-edn projects dir)
          (format-ls-tree-text projects dir))))))

;; ============================================================
;; Ops registry — single source of truth for dispatch + help
;; ============================================================

(def ops-registry
  ;; @spec OP-ALG-CLI-001, OP-ALG-DECODE-001, OP-ALG-IDENTITY-001
  "Single source of truth for all operations.
   Each key is the canonical op name. Drives dispatch, help, and error messages."
  ;; hash-map, NOT sorted-map: sorted-map COMPARES keys on contains?/get, so any
  ;; non-keyword lookup throws ClassCastException (the ed6ad99 bug class). Ordering
  ;; for display is done at render time (format-global-help sort-by, error-msg sort).
  (hash-map
    :cljc-add-require {:handler   run-cljc-add-require
                       :desc      "Add a platform-aware require to a CLJC file"
                       :args      {:file     {:required true :desc "Input CLJC file"}
                                   :platform {:required true :desc ":clj, :cljs, or :cljc"}
                                   :ns       {:required true :desc "Namespace to require"}
                                   :as       {:desc "Optional alias"}
                                   :out      {:desc "Output path (default: stdout)"}}
                       :examples  ["clj-surgeon :op :cljc-add-require :file src/foo.cljc :platform :cljs :ns goog.string :as gstr"]
                       :category  :cljc}

    :cljc-analyze     {:handler   (fn [{:keys [file clj cljs]}]
                                    (cond
                                      file           (cljc-analyze/analyze-cljc (slurp file))
                                      (and clj cljs) (cljc-analyze/analyze-pair (slurp clj) (slurp cljs))
                                      :else          {:error "supply :file or :clj + :cljs"}))
                       :desc      "Classify forms by platform (shared/clj-only/cljs-only/divergent)"
                       :args      {:file {:desc "A single CLJC file to analyze"}
                                   :clj  {:desc "CLJ file (use with :cljs)"}
                                   :cljs {:desc "CLJS file (use with :clj)"}}
                       :examples  ["clj-surgeon :op :cljc-analyze :file src/foo.cljc"
                                   "clj-surgeon :op :cljc-analyze :clj src/foo.clj :cljs src/foo.cljs"]
                       :category  :cljc}

    :cljc-merge       {:handler   run-cljc-merge
                       :desc      "Combine CLJ + CLJS into a single CLJC"
                       :args      {:clj  {:required true :desc "Input CLJ file"}
                                   :cljs {:required true :desc "Input CLJS file"}
                                   :out  {:desc "Output CLJC path (default: stdout)"}}
                       :examples  ["clj-surgeon :op :cljc-merge :clj src/foo.clj :cljs src/foo.cljs :out src/foo.cljc"]
                       :category  :cljc}

    :cljc-split       {:handler   run-cljc-split
                       :desc      "Split a CLJC into parallel CLJ + CLJS files"
                       :args      {:file     {:required true :desc "Input CLJC file"}
                                   :clj-out  {:desc "Output CLJ path"}
                                   :cljs-out {:desc "Output CLJS path"}}
                       :examples  ["clj-surgeon :op :cljc-split :file src/foo.cljc"]
                       :category  :cljc}

    :declares         {:handler   run-declares
                       :desc      "Audit which forward declares are needed vs removable"
                       :args      {:file {:required true :desc "Clojure source file"}}
                       :examples  ["clj-surgeon :op :declares :file src/my/ns.clj"]
                       :category  :read}

    :deps             {:handler   run-deps
                       :desc      "Intra-namespace call graph"
                       :args      {:file {:required true :desc "Clojure source file"}
                                   :form {:desc "Single form to show deps for"}}
                       :examples  ["clj-surgeon :op :deps :file src/my/ns.clj"
                                   "clj-surgeon :op :deps :file src/my/ns.clj :form sync-draft!"]
                       :category  :read}

    :extract          {:handler   extract/plan
                       :desc      "Plan dependency-minimal form extraction with caller and quoted-Var proof (dry run)"
                       :args      {:file           {:required true :desc "Source file"}
                                   :forms          {:required true :desc "EDN vector of form names, e.g. '[foo bar]'"}
                                   :to             {:required true :desc "Target file path for new namespace"}
                                   :require-policy {:desc ":minimal (default) proves exact requires; :copy-all preserves the complete source ns header as a conservative starting point"}}
                       :examples  ["clj-surgeon :op :extract :file src/state.clj :forms '[distill refine]' :to src/distillery.clj"]
                       :category  :write
                       :pair      :extract!}

    :extract!         {:handler   extract/execute!
                       :desc      "Execute one failure-atomic form extraction to a new namespace"
                       :args      {:file           {:required true :desc "Source file"}
                                   :forms          {:required true :desc "EDN vector of form names"}
                                   :to             {:required true :desc "New target file; existing files refuse"}
                                   :require-policy {:desc ":minimal (default) proves exact requires; :copy-all preserves the complete source ns header as a conservative starting point"}
                                   :receipt-out    {:desc "Optional new .edn path for a guarded inverse receipt"}}
                       :workflow  ["Run :extract first. Review target-requires, omitted-target-requires, remaining-source-callers, callers-to-review, and authority-labeled quoted-var-references. Unsupported require shapes refuse instead of copying or dropping unproved dependencies."
                                   "Application compiles both complete files from one source snapshot, parses them, hash-fences the source, writes atomically, and verifies read-back."
                                   "Existing targets, stale source, invalid candidates, receipt aliases, and handled write failures refuse or roll back without leaving a partial extraction."
                                   "Use :receipt-out when the extraction must be reversible. Pass that path to :undo-extract!; do not edit the receipt."]
                       :examples  ["clj-surgeon :op :extract! :file src/state.clj :forms '[distill refine]' :to src/distillery.clj :receipt-out /tmp/distillery-extract.edn"]
                       :category  :write
                       :pair      :extract}

    :undo-extract!    {:handler   extract/undo!
                       :desc      "Undo an extraction while both result files still match its receipt"
                       :args      {:receipt {:required true :desc "Guarded .edn receipt emitted by :extract!"}}
                       :workflow  ["Supply the unchanged receipt emitted by :extract!."
                                   "The command refuses before writing when either extraction result has changed or disappeared."
                                   "Success restores the original source and removes only the exact target created by that extraction."]
                       :examples  ["clj-surgeon :op :undo-extract! :receipt /tmp/distillery-extract.edn"]
                       :category  :write}

    ;; @spec MCP-OP-POS-AUTH-005
    ;; @spec MCP-OP-POS-AUTH-006
    ;; @spec MCP-OP-POS-AUTH-007
    ;; @spec MCP-OP-POS-AUTH-008
    ;; @spec MCP-OP-POS-AUTH-009
    ;; @spec MCP-OP-POS-AUTH-010
    :edit             {:handler   run-edit
                       :desc      "Plan one hash-fenced structural edit; :expect can verify and apply a literal replacement in one call"
                       :args      {:file     {:required true :desc "Clojure source file; modified only by a successful :expect-guarded edit"}
                                   :query    {:desc "EDN lens pipeline ending in [:replace FORM] or [:replace-span FORM ...]; supply exactly one of :query and :expr"}
                                   :expr     {:desc "Sandboxed pure Clojure edit program; supply exactly one of :query and :expr"}
                                   :expect   {:desc "Optional declared before-state for a literal replacement. Whitespace is ignored; comments, metadata, and reader syntax must match. Equality applies and verifies the edit; any difference refuses"}
                                   :plan-out {:desc "Optional .edn audit artifact with :expect; required for a plan-only edit; must not alias :file"}}
                       :workflow  ["Supply exactly one of :query and :expr. Use :expr for pure Clojure collection composition through sandboxed SCI."
                                   "Use (transform path pure-function) when the replacement must be derived from the selected form. The plan stores its concrete replacement. Transform remains plan-only because its generated after-state requires review; :expect refuses it."
                                   "SCI exposes pure clojure.core collection functions and clj-surgeon builders. It does not expose I/O, processes, namespaces, mutable references, or host interop."
                                   "Use :xray to read or compute from a structural path. Use :edit when the complete selection and either the replacement or its pure transformation rule are known."
                                   "Start with (form 'NAME). A direct :expect-guarded edit requires this named-owner root. A line-rooted or otherwise unnamed query is plan-only: remove :expect, supply :plan-out, review the plan, then apply it with :replace-subform!."
                                   "When an owner plus an exact key, guard, map key, binding, or subtree identifies the target, the :edit plan can be the first source-bearing call; do not pre-read merely to reconstruct that relationship."
                                   "Without :expect, this command is PLAN ONLY: :plan-out is required, the command saves a hash-fenced review artifact, and source never changes."
                                   "Do not preflight whether :plan-out exists. A successful plan atomically replaces that artifact; any refusal preserves it."
                                   "Review the returned selector, one edit, diff, source hash, and result hash. The command already returns the review evidence; do not reread the saved plan file."
                                   "When the diff is exact, apply that saved plan with :replace-subform!; never reproduce it with apply_patch, a text edit, or a second equivalent plan."
                                   "Apply only after review, as a separate command: clj-surgeon :op :replace-subform! :plan PLAN.edn."
                                   ":expect is optional; without it the default flow is unchanged: plan first, review, then apply separately."
                                   "With :expect FORM, a named-owner root, and a literal replacement, the command applies and verifies in one guarded call. It ignores whitespace, but comments, metadata, reader macros, and token spelling must match. Omit :plan-out unless the audit artifact must be retained."
                                   "A literal replace or replace-span written inline in :expr preserves its exact replacement spelling, including #(), comments, commas, metadata, and multiline layout. A computed replacement or :query has no lexical source and uses canonical printing."
                                   ":selector :query is semantic data and may display #() as fn*. The edit :after and :diff fields report the exact source that the plan writes."
                                   "A difference refuses with :expect-mismatch, returns :expected, :actual, and :actual-source, and leaves the source bytes and any existing plan artifact unchanged. If undeclared comments or metadata caused the refusal, narrow the selector or declare the exact before-source."
                                   "Unknown flags, getter-only queries, computed transforms, ambiguous targets, non-.edn plan paths, and source/plan path aliasing refuse without changing source or an existing plan."]
                       :examples  ["clj-surgeon :op :edit :file src/policy.clj :expr \"(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))\" :plan-out plan.edn"
                                   "clj-surgeon :op :edit :file src/cache.clj :expr \"(-> (line 412) (match '(old-reader account-id)) (replace '(new-reader account-id)))\" :plan-out plan.edn"
                                   "clj-surgeon :op :edit :file src/state.clj :expr \"(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))\" :plan-out plan.edn"
                                   "clj-surgeon :op :edit :file src/state.clj :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' :plan-out plan.edn"
                                   "clj-surgeon :op :edit :file src/state.clj :query '[[:form transition] [:find :finish] [:span 2] [:replace-span :finish (assoc state :status :complete)]]' :plan-out plan.edn"
                                   "clj-surgeon :op :edit :file src/state.clj :expr \"(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))\" :expect '(assoc state :status :done)'"
                                   "clj-surgeon :op :replace-subform! :plan plan.edn"]
                       :category  :write}

    :change           {:handler   intent-transaction/plan-change
                       :canonical-operation :change
                       :lifecycle :preview
                       :desc      "Compile one scoped structural change transaction without writing source"
                       :args      {:spec      {:desc "Inline EDN map; compatibility entrance for small specs"}
                                   :spec-file {:desc "EDN spec path, or - to read one document from stdin (preferred)"}}
                       :workflow  ["Provide exactly one of :spec or :spec-file. Prefer :spec-file - for a nontrivial plan, like kubectl apply -f -."
                                   "Express the complete mechanical model plan as one :changes document: :in, optional :forms, :find, :do, and :expect."
                                   "Each change declares explicit :in, optional unique :forms, one supported :do, and positive :expect {:matches N}. Replacement uses exact source :find with [:replace SOURCE]; whole-owner [:delete true] requires named :forms and omits :find."
                                   "Declare aggregate :expect values for :changes, :edits, and :files. Use :each-form or :each-file when distribution matters."
                                   "This command reads each scoped file once, compiles every change against the original snapshots, and writes nothing."
                                   "Whitespace may differ. Comments, metadata, reader syntax, token spelling, and collection type must match exactly. Legacy exact :intents with :intent-count and :changed-file-count remain accepted."
                                   "Different changes may touch disjoint syntax in the same file. Any identical, ancestor/descendant, or otherwise overlapping targets refuse the whole plan."
                                   "Review the per-change, per-form, and per-file counts, hashes, concrete edits, combined diff, and whole-file parse proof."
                                   "Use one change for one repeated structural rule; use several changes to materialize one heterogeneous model plan without repeated edit turns."]
                       :examples  ["clj-surgeon :op :change :spec-file - <<'EDN'\n{:changes [{:id :body-class :in [\"src/ui.clj\"] :forms [shell reader] :find \":body\" :do [:replace \":body.page\"] :expect {:matches 2 :each-form 1}}] :expect {:changes 1 :edits 2 :files 1}}\nEDN"]
                       :category  :write
                       :pair      :change!}

    :change!          {:handler   intent-transaction/execute-change!
                       :canonical-operation :change
                       :lifecycle :commit
                       :desc      "Apply one guarded structural change transaction and save its inverse receipt"
                       :args      {:spec        {:desc "Inline EDN map; compatibility entrance for small specs"}
                                   :spec-file   {:desc "EDN spec path, or - to read one document from stdin (preferred)"}
                                   :receipt-out {:required true :desc "Durable .edn inverse receipt; must not alias a source file"}}
                       :workflow  ["Provide exactly one of :spec or :spec-file. Prefer :spec-file - so a large plan travels as data instead of shell-escaped text, like kubectl apply -f -."
                                   "Express the complete mechanical model plan once as the same guarded :changes document accepted by :change."
                                   "Every action, exact selector, per-change count or distribution guard, and aggregate :expect value is consent to the exact materialized transaction. If the task already supplies complete files and owners, declare them without probing source only to confirm them."
                                   "The command compiles from one snapshot, parses every complete future file, rechecks hashes, commits every file, verifies read-back hashes, and publishes the receipt last."
                                   "If a handled write or receipt-publication failure occurs, the command restores transaction-owned bytes and reports whether rollback was complete. It never overwrites unknown concurrent bytes."
                                   "The console result is compact. Do not open :receipt-out; pass its path as :receipt PATH to :undo-change!."
                                   "Use :change when review is required before mutation. Use :change! when the exact guarded intent set is already the model's approved plan."]
                       :examples  ["clj-surgeon :op :change! :spec-file - :receipt-out /tmp/ui-change.edn <<'EDN'\n{:changes [{:id :body-class :in [\"src/ui.clj\"] :forms [shell reader] :find \":body\" :do [:replace \":body.page\"] :expect {:matches 2 :each-form 1}}] :expect {:changes 1 :edits 2 :files 1}}\nEDN\n\nclj-surgeon :op :change! :spec-file - :receipt-out /tmp/delete.edn <<'EDN'\n{:changes [{:id :obsolete :in [\"src/app.clj\"] :forms [old-handler old-test] :do [:delete true] :expect {:matches 2 :each-form 1}}] :expect {:changes 1 :edits 2 :files 1}}\nEDN"]
                       :category  :write
                       :pair      :change}

    :undo-change!     {:handler   intent-transaction/execute-undo!
                       :desc      "Undo a completed structural intent transaction when every result hash still matches"
                       :args      {:receipt {:required true :desc "Durable .edn receipt emitted by :change!"}}
                       :workflow  ["Supply the unchanged receipt emitted by :change!."
                                   "The command refuses the entire inverse before writing when any current file differs from the recorded forward result hash."
                                   "Every reconstructed original file must parse and match its recorded original hash before commit."
                                   "A successful receipt verifies every restored file's read-back hash. A second undo refuses because the forward result hashes no longer match."]
                       :examples  ["clj-surgeon :op :undo-change! :receipt /tmp/api-change.edn"]
                       :category  :write}

    :fix-declares     {:handler   (fn [opts] (fix-declares/plan (:file opts)))
                       :desc      "Plan declare elimination (dry run)"
                       :args      {:file {:required true :desc "Clojure source file"}}
                       :examples  ["clj-surgeon :op :fix-declares :file src/my/ns.clj"]
                       :category  :write
                       :pair      :fix-declares!}

    :fix-declares!    {:handler   (fn [opts] (fix-declares/execute! (:file opts)))
                       :desc      "Execute declare elimination"
                       :args      {:file {:required true :desc "Clojure source file"}}
                       :examples  ["clj-surgeon :op :fix-declares! :file src/my/ns.clj"]
                       :category  :write
                       :pair      :fix-declares}

    :find-subform     {:handler   structural-lens/find-file
                       :aliases   [:match-form :grep-form]
                       :desc      "Find nested syntax structurally across a file or within a named form"
                       :args      {:file   {:required true :desc "Clojure source file"}
                                   :inside {:desc "Restrict search to this top-level form"}
                                   :match  {:required true :desc "Clojure form pattern; _ matches exactly one subtree and pattern arity is exact"}}
                       :workflow  ["Omit :inside for file-wide structural search; add it only to narrow the search."
                                   "Use :match-form for structural search; :match accepts one Clojure form pattern, not a regular expression."
                                   "The _ wildcard matches exactly one subtree. There is no variadic wildcard; use (loop _ _) for a two-argument loop form."
                                   "Each match names its enclosing form in :inside when available; reuse that value to narrow a plan without a line-number lookup."
                                   "Zero and multiple matches are useful read evidence; mutation still requires exactly one match."]
                       :examples  ["clj-surgeon :op :match-form :file src/views.clj :match '(post! \"/api/items\" _)'"
                                   "clj-surgeon :op :match-form :file src/runtime.clj :match '(loop _ _)'"
                                   "clj-surgeon :op :match-form :file src/views.clj :inside render :match '(post! \"/api/items\" _)'"]
                       :category  :read}

    :lens             {:handler  structural-lens/lens-file
                       :aliases  [:q]
                       :desc     "Query Clojure syntax with an EDN pipeline or emit one guarded replacement plan"
                       :args     {:file     {:required true :desc "Clojure source file"}
                                  :query    {:required true :desc "EDN structural pipeline; use [:partition-all N] for sibling inventories and optionally end in [:replace FORM] or [:replace-span FORM ...]"}
                                  :plan-out {:desc "Write the replayable EDN plan for a terminal replacement"}}
                       :workflow ["Pipe located syntax through [:form NAME], [:find PATTERN], [:where {:tag TAG}] or [:where {:parent-tag TAG}], and :right/:left/:up/:down."
                                  "Navigation-only queries are read-only and report zero, one, or many matches plus a per-step count trace."
                                  "Use semantic sibling navigation for case clauses, cond branches, map entries, and bindings; do not reconstruct textual context."
                                  "Use [:span 2] to select a node and its next semantic peer; [:replace-span FORM FORM] preserves comments and whitespace between peers and requires equal arity."
                                  "Use [:partition-all 2] at the first sibling to return every consecutive pair in one read. A shorter final span is explicit and is never dropped or interpreted."
                                  "When repeated nested heads make the first outer sibling unknown, promote heads to owners with :up, then use :outermost before navigating to their children."
                                  "When the first outer sibling is already known, anchor there directly; that query is shorter than :up :outermost."
                                  "A final [:replace FORM] reuses the same structural path as an updater, emits a plan and never writes source."
                                  "Review the one edit, diff, selector, trace, and hashes. Apply the reviewed plan separately with :replace-subform!."
                                  "Writes refuse zero or multiple selected nodes. Arbitrary evaluation, fuzzy choice, and implicit bulk updates are unsupported."]
                       :examples ["clj-surgeon :op :q :file src/state.clj :query '[[:form transition] [:find :finish] :right]'"
                                  "clj-surgeon :op :q :file src/state.clj :query '[[:form transition] [:find case] :up :down :right :right [:partition-all 2]]'"
                                  "clj-surgeon :op :q :file src/policy.clj :query '[[:form classify-request] [:find cond] :up :outermost :down :right [:partition-all 2]]'"
                                  "clj-surgeon :op :q :file src/state.clj :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' :plan-out plan.edn"
                                  "clj-surgeon :op :q :file src/state.clj :query '[[:form transition] [:find :finish] [:span 2] [:replace-span :finish (assoc state :status :complete)]]' :plan-out plan.edn"]
                       :category :read}

    :xray             {:handler run-xray
                       :desc "Compute one read-only EDN value from structurally selected Clojure data"
                       :args {:file {:required true :desc "Clojure source file; never modified"}
                              :expr {:required true :desc "One sandboxed pure Clojure path, optionally count-refined and analyzed"}
                              :evidence {:desc ":compact (default) or :full for computed reads; literal paths always return exact source"}}
                       :workflow ["Use one Clojure path for every structural read. A path without a terminal returns literal source evidence."
                                  "Start with (form 'NAME) for a known named form. Start with (line N) when a physical line identifies one otherwise unnamed top-level owner; blank gaps and overlapping owners refuse."
                                  "End with (analyze pure-function). The function always receives one vector of ordinary Clojure data in match order, including for zero or one match. Write one terminating pure function over this contract instead of a separate shape-discovery query."
                                  "End a literal path with (expect-count n) to return exact source only at that cardinality. Put the same guard before analyze to refuse before calling the function without changing its vector input type."
                                  "After selecting a def, use initializer to select its right-hand side without evaluating it. An unbound def or non-def produces zero matches."
                                  "Literal reads return exact selected source. Computed reads return compact :value, addresses, ranges, trace, cardinality, and hashes without repeating source bodies."
                                  "Use :evidence :full when a computed read also needs exact selected source; :compact remains the default."
                                  "Selected values are never evaluated. Computed X-ray shallowly normalizes a selected map literal or top-level hash-map/array-map syntax; nested constructor syntax and exact evidence remain source-shaped."
                                  "Identify nested descendants inside that function with (filter predicate (tree-seq coll? seq value))."
                                  "Return concrete EDN, not a lazy sequence. Malformed map constructor syntax refuses."
                                  "SCI is capability-limited, not termination-proof. It does not expose I/O, processes, namespaces, mutable references, classes, or host interop. Analyzers must perform bounded work."
                                  "The command is READ ONLY. It never writes source or creates an edit plan."
                                  "Truncated selection, analyzer failure, lazy or non-EDN output, and output over 65,536 characters refuse with structured EDN."]
                       :examples ["clj-surgeon :op :xray :file src/state.clj :expr \"(-> (form 'transition) (match :finish) right)\""
                                  "clj-surgeon :op :xray :file src/cache.clj :expr \"(-> (line 412) (match '(old-reader account-id)))\""
                                  "clj-surgeon :op :xray :file src/policy.clj :expr \"(-> (form 'audit-report) initializer (expect-count 1) (analyze (fn [[report]] (frequencies (map :category (:events report))))))\""
                                  "clj-surgeon :op :xray :file src/policy.clj :expr \"(-> (form 'classify-request) (match 'cond) up outermost down right (partition-all 2) (analyze #(mapv first %)))\""]
                       :category :read}

    :ls               {:handler   run-outline
                       :aliases   [:outline]
                       :desc      "List forms in a namespace (line ranges, arglists, forward refs)"
                       :args      {:file {:required true :desc "Clojure source file"}}
                       :examples  ["clj-surgeon :op :ls :file src/my/ns.clj"]
                       :category  :read}

    :show-form        {:handler  show-form/show
                       :desc     "Show exact top-level forms from one file snapshot or one guarded cross-file manifest"
                       :aliases  [:cat]
                       :args     {:file     {:required true :desc "Clojure source file"}
                                  :form     {:desc "Unqualified top-level name; supply exactly one selector"}
                                  :forms    {:desc "Nonempty EDN vector of up to 50 unique top-level names; supply exactly one selector"}
                                  :line     {:desc "Positive one-based line; supply exactly one selector"}
                                  :contains {:desc "Nonblank case-sensitive literal text; supply exactly one selector"}
                                  :platform {:desc "Keyword platform to disambiguate CLJC forms, such as :clj or :cljs"}
                                  :spec     {:desc "Inline cross-file EDN read manifest; compatibility entrance for small specs"}
                                  :spec-file {:desc "Cross-file EDN read manifest path, or - for stdin (preferred)"}
                                  :format   {:desc "Cross-file output: :edn (default exact source) or :semantic (canonical compact data without comments/layout)"}}
                       :workflow ["Supply exactly one selector: :form, :forms, :line, or :contains."
                                  "When several owner names in one file are known, use :forms once; it preserves requested order and reads one source snapshot."
                                  "When owners span files, use :spec-file - with :reads plus exact :expect file/form counts. Each physical file is read once."
                                  "Attach stdin in the same shell action: printf '%s\n' 'MANIFEST' | clj-surgeon :op :cat :spec-file -. Never invoke :spec-file - and wait to type the document later."
                                  "For a large behavior or architecture read, add :format :semantic. It prints compact canonical Clojure data with file hashes; comments and layout are omitted and reader shorthand may expand."
                                  "Keep the default :edn format when exact lexical source, comments, layout, or reader spelling matters."
                                  "Do not combine :spec or :spec-file with direct read arguments."
                                  "Batch reads are all-or-nothing: a missing, ambiguous, invalid, or duplicate name returns no partial source."
                                  "Cross-file manifests reject duplicate physical paths and unknown keys. Combined source over the declared limit, or the hard 65,536-character cap, refuses without partial source."
                                  "Use :cat instead of reconstructing a sed range when a top-level name or containing line is known."
                                  "Make :cat the first source inspection; do not run :ls solely as a preflight."
                                  "With distinctive text but no form name, use literal :contains to return its one enclosing form in the same command; keyword-shaped values such as :finish remain literal text."
                                  "Literal search includes attached comments, strings, and docstrings; it never interprets a regular expression."
                                  "Platform-qualified form selection follows the .clj, .cljs, or .cljc file extension; unknown extensions refuse."
                                  "Read :source as the exact parsed form and :source-hash as the complete file snapshot."
                                  "On ambiguity, stop and refine the selector; the command never chooses the first match."]
                       :examples ["clj-surgeon :op :cat :file src/my/ns.clj :form transition!"
                                  "clj-surgeon :op :cat :file src/my/ns.clj :forms '[transition! validate-state]'"
                                  "printf '%s\n' '{:reads [{:file \"src/a.clj\" :forms [start stop]} {:file \"src/b.clj\" :forms [route]}] :expect {:file-count 2 :form-count 3}}' | clj-surgeon :op :cat :spec-file - :format :semantic"
                                  "clj-surgeon :op :cat :file src/my/ns.clj :line 1134"
                                  "clj-surgeon :op :cat :file src/my/ns.clj :contains :finish"
                                  "clj-surgeon :op :cat :file src/my/ns.cljc :form transition! :platform :cljs"]
                       :category :read}

    :ls-deps          {:handler   run-ls-deps
                       :desc      "Transitive dependency tree for a form"
                       :args      {:file {:required true :desc "Clojure source file"}
                                   :form {:required true :desc "Name of target form"}}
                       :examples  ["clj-surgeon :op :ls-deps :file src/my/ns.clj :form transition!"]
                       :category  :read}

    :ls-extract       {:handler   run-closure
                       :desc      "Minimal extractable unit (form + exclusive deps)"
                       :args      {:file {:required true :desc "Clojure source file"}
                                   :form {:required true :desc "Name of target form"}}
                       :examples  ["clj-surgeon :op :ls-extract :file src/my/ns.clj :form rebuild!"]
                       :category  :read}

    :relation-census  {:handler   run-relation-census
                       :aliases   [:census]
                       :desc      "Classify every collection write inside defmethod fold-event arms"
                       :args      {:dir     {:desc "Root directory to scan (default: .)"}
                                   :file    {:desc "Census exactly one file instead of scanning"}
                                   :doors   {:desc "Comma-separated identity doors (default: conj-once,cons-once,upsert-by,conj-distinct-by,cons-distinct-by)"}
                                   :threads {:desc "Plan-phase parallelism; changes elapsed time, never the answer"}}
                       :workflow  ["A :raw site is the vulnerability: a write with no dominating recognised guard."
                                   "An :unknown site is review work this version declines to decide; :reason names why."
                                   "The census locates review work. It never proves idempotency and is not an enforcement gate."]
                       :examples  ["clj-surgeon :op :relation-census :dir ."
                                   "clj-surgeon :op :relation-census :file src/app/folds.clj"
                                   "clj-surgeon :op :relation-census :dir . :threads 8"]
                       :category  :read}

    :ls-tree          {:handler   run-ls-tree
                       :aliases   [:tree :map :outline-tree]
                       :desc      "Map namespaces across a directory tree"
                       :args      {:dir    {:required true :desc "Root directory to scan"}
                                   :grep   {:desc "Filter pattern (regex) — uses ripgrep"}
                                   :format {:desc ":edn for machine-readable (default: text)"}}
                       :examples  ["clj-surgeon :op :ls-tree :dir ."
                                   "clj-surgeon :op :ls-tree :dir ~/src.local/ :grep \"postgres|jdbc\""]
                       :category  :read}

    :mv               {:handler run-mv
                       :aliases [:mv-with-deps]
                       :desc "Reorder a form with dependency guards; writes unless :dry-run true; :mv-with-deps presets :with-deps true"
                       :args {:file {:required true :desc "Clojure source file rewritten in place unless dry-run"}
                              :form {:required true :desc "Unqualified name of the top-level form to move"}
                              :before {:required true :desc "Unqualified top-level form name to place it before"}
                              :with-deps {:desc "true to move the minimum required dependency closure"}
                              :dry-run {:desc "true to return EDN plan/diff without writing (always start here)"}}
                       :workflow ["Always preview plain :mv with :dry-run true; stop on a nonzero exit or any :error-type."
                                  "If preview returns :ok true, inspect :plan/:diff, then rerun the same command without :dry-run."
                                  "Only for :would-strand-dependencies, run the safe :recommended-command; it previews :mv-with-deps."
                                  "Review :plan/:added-forms, :move-order, and :diff; execute :apply-command only after consenting to every added form."
                                  "For :would-strand-users or any other refusal, stop. :mv-with-deps never moves callers or adds declarations."
                                  "A dry run is a preview, not a saved, hash-bound plan. Preview again after any source change."
                                  "After writing, rerun :ls plus the repository formatter, linter, compiler, and tests."]
                       :examples ["clj-surgeon :op :mv :file src/my/ns.clj :form foo :before bar :dry-run true"
                                  "clj-surgeon :op :mv :file src/my/ns.clj :form foo :before bar"
                                  "clj-surgeon :op :mv-with-deps :file src/my/ns.clj :form foo :before bar :dry-run true"
                                  "clj-surgeon :op :mv-with-deps :file src/my/ns.clj :form foo :before bar"]
                       :category :write}

    :replace-subform  {:handler   structural-lens/plan-file-replacement
                       :desc      "Plan one hash-guarded nested structural replacement"
                       :args      {:file   {:required true :desc "Clojure source file"}
                                   :inside {:desc "Restrict search to this top-level form"}
                                   :match  {:required true :desc "Clojure form pattern; _ matches one subtree"}
                                   :with   {:required true :desc "Replacement Clojure form"}
                                   :plan-out {:desc "Write the replayable EDN plan to this path"}}
                       :workflow  ["Inspect or find the exact parsed subtree before planning."
                                   "When a case key, cond guard, map key, or binding name identifies the target, use :cat :contains on that sibling text to recover its owner and context in one read."
                                   "A case clause, cond branch, map entry, or binding pair is adjacent syntax, not a synthetic wrapper list; match its contained value or expression."
                                   "Run plan generation as its own command; never chain planning and application in one shell invocation."
                                   "Review the returned match, diff, address, source hash, and result hash before applying the saved plan."]
                       :examples  ["clj-surgeon :op :replace-subform :file src/views.clj :inside render :match '(post! \"/api/items\" _)' :with '(items/actions surface)' :plan-out plan.edn"]
                       :category  :write
                       :pair      :replace-subform!}

    :replace-subform! {:handler   structural-lens/execute-plan!
                       :desc      "Apply a previously emitted structural replacement plan"
                       :args      {:plan {:required true :desc "EDN plan file from :replace-subform"}}
                       :workflow  ["Run plan generation as a separate command; never chain it with application."
                                   "Before this command, review the evidence returned by plan generation; do not reopen the saved plan only to repeat that review."
                                   "Apply the reviewed plan directly with :replace-subform!."
                                   "A successful receipt includes :verified read-back hash and whole-file parse evidence; the reviewed plan is the edit-level diff, so do not repeat those checks with rg, cat, git diff, or shasum."
                                   "When the task asks only to verify this exact edit, the reviewed plan plus successful receipt completes that request; do not probe for a Git worktree merely to repeat it."
                                   "Do not edit the plan with apply_patch or another text tool."
                                   "If the intended edit changes, generate a new plan."
                                   "Stop on nonzero status, then run the repository formatter, linter, and tests after success."]
                       :examples  ["clj-surgeon :op :replace-subform! :plan plan.edn"]
                       :category  :write
                       :pair      :replace-subform}

    :rename-ns        {:handler   rename/plan
                       :desc      "Plan a namespace prefix rename (dry run)"
                       :args      {:from {:required true :desc "Old namespace prefix"}
                                   :to   {:required true :desc "New namespace prefix"}
                                   :root {:desc "Project root (default: .)"}}
                       :examples  ["clj-surgeon :op :rename-ns :from old.prefix :to new.prefix :root ."]
                       :category  :write
                       :pair      :rename-ns!}

    :rename-ns!       {:handler   rename/execute!
                       :desc      "Execute a namespace prefix rename"
                       :args      {:from {:required true :desc "Old namespace prefix"}
                                   :to   {:required true :desc "New namespace prefix"}
                                   :root {:desc "Project root (default: .)"}}
                       :examples  ["clj-surgeon :op :rename-ns! :from old.prefix :to new.prefix :root ."]
                       :category  :write
                       :pair      :rename-ns}

    :topo             {:handler   run-topo
                       :desc      "Topological sort (optimal form ordering)"
                       :args      {:file {:required true :desc "Clojure source file"}}
                       :examples  ["clj-surgeon :op :topo :file src/my/ns.clj"]
                       :category  :read}))

;; Alias resolution — derived from registry at load time

(def ^:private alias->canonical
  "Alias -> canonical op keyword."
  (reduce-kv (fn [m canonical {:keys [aliases]}]
               (reduce #(assoc %1 %2 canonical) m (or aliases [])))
             {}
             ops-registry))

(def preferred-op-names
  "Public caller spellings for operations whose implementation keys remain
   stable for compatibility."
  {:find-subform :match-form
   :show-form :cat})

(def hidden-from-primary-help
  "Compatibility-only operations superseded by the Clojure-native read and
   edit surfaces. They remain dispatchable."
  #{:lens})

(defn public-op-name
  "Return the preferred caller spelling for one registry operation."
  [op-key]
  (get preferred-op-names op-key op-key))

(defn public-op-keys
  "Return the exact operations advertised to new callers."
  [registry]
  (->> (keys registry)
       (remove hidden-from-primary-help)
       (map public-op-name)
       (concat [:help :mv-with-deps])
       set
       sort
       vec))

(defn resolve-op
  "Resolve an op (keyword or bare string, e.g. `:op ls-tree`) to its
   canonical name, following aliases. Returns nil for unknown ops.
   Strings are coerced to keywords; a stray leading colon in a string
   (\":ls-tree\") is forgiven. Non-keyword/string input resolves to nil."
  [op]
  (let [op (if (string? op)
             (keyword (cond-> op (str/starts-with? op ":") (subs 1)))
             op)]
    (when (keyword? op)
      (if (contains? ops-registry op)
        op
        (get alias->canonical op)))))

;; ============================================================
;; Help formatting — pure functions, registry in, string out
;; ============================================================

(def ^:private category-order [:read :write :cljc])
(def ^:private category-labels
  {:read  "Read-only (analysis, no side effects)"
   :write "Write operations (read each operation safety workflow)"
   :cljc  "CLJC"})

(defn format-global-help
  "Categorized command list with 1-line descriptions.
   Pure: registry in, string out."
  [registry]
  (let [sb (StringBuilder.)
        by-cat (->> registry
                    (remove (fn [[op-key]]
                              (contains? hidden-from-primary-help op-key)))
                    (group-by (fn [[_ v]] (:category v))))]
    (.append sb "clj-surgeon — structural operations on Clojure namespaces\n\n")
    (.append sb "Usage: clj-surgeon :op <command> [args...]\n")
    (.append sb "       clj-surgeon up [WORKSPACE]      join the shared hot MCP stack\n")
    (.append sb "       clj-surgeon recover [WORKSPACE] repair once; receipt names fallback\n")
    (.append sb "       clj-surgeon report-failure --receipt PATH\n")
    (.append sb "       clj-surgeon --help              show this message\n")
    (.append sb "       clj-surgeon :op :help           show this message\n")
    (.append sb "       clj-surgeon --version           show machine-readable version\n")
    (.append sb "       clj-surgeon :op <cmd> --help    show command details\n\n  Agent entrance:\n      Prefer persistent MCP inspect_clojure and apply_clojure_changes.\n      Use this process-starting CLI when MCP is unavailable or lacks the operation.\n\n")
    (doseq [cat category-order
            :let [label (get category-labels cat)
                  ops   (get by-cat cat)]]
      (when (seq ops)
        (.append sb (str "  " label ":\n"))
        (doseq [[op-key {:keys [desc pair]}]
                (sort-by (comp public-op-name first) ops)]
          (.append sb (format "    %-20s %s" (name (public-op-name op-key)) desc))
          (when pair
            (.append sb (format "  -> %s" (name pair))))
          (.append sb "\n"))
        (.append sb "\n")))
    (.append sb "  Quick start:\n")
    (.append sb "    clj-surgeon :op :ls :file src/my/ns.clj\n")
    (.append sb "    clj-surgeon :op :cat :file src/my/ns.clj :contains 'distinctive text'\n")
    (.append sb "    clj-surgeon :op :xray :file src/my/ns.clj :expr \"(-> (form 'transition) (match :finish) right)\"\n")
    (.append sb "    clj-surgeon :op :xray :file src/my/ns.clj :expr \"(-> (form 'audit-report) initializer (expect-count 1) (analyze (fn [[report]] (frequencies (map :category (:events report))))))\"\n")
    (.append sb "    clj-surgeon :op :edit :file src/my/ns.clj :expr \"(-> (form 'transition) (match :done) (replace :complete))\" :expect :done\n")
    (.append sb "    clj-surgeon :op :edit :file src/my/ns.clj :expr \"(-> (form 'retry-policy) (match :delays) right (transform #(mapv inc %)))\" :plan-out plan.edn\n")
    (.append sb "    clj-surgeon :op :ls-tree :dir . :grep \"postgres\"\n")
    (.append sb "    clj-surgeon :op :deps :file src/my/ns.clj :form my-fn\n    clj-surgeon :op :mv :file src/my/ns.clj :form foo :before bar :dry-run true\n\n")
    (.append sb "  Compatibility aliases: :outline, :show-form, :find-subform, :grep-form, :lens, :q, :tree, :map, :outline-tree.\n")
    (.append sb "  Convenience alias: :mv-with-deps presets :with-deps true.\n\n")
    (.append sb "  All ops return EDN. Read-only operations never write.\n  Write operations differ: :mv writes unless :dry-run true; paired operations use their documented ! executor.\n")
    (str sb)))

(defn format-op-help
  "Per-command help: description, args, examples.
   Pure: op-key + op-def in, string out."
  [op-key {:keys [desc args examples pair aliases workflow]}]
  (let [sb (StringBuilder.)
        public-name (public-op-name op-key)
        compatibility-aliases (->> (cons op-key aliases)
                                   (remove #{public-name})
                                   distinct)]
    (.append sb (format "clj-surgeon :op %s\n\n" (name public-name)))
    (.append sb (format "  %s\n" desc))
    (when (seq compatibility-aliases)
      (.append sb (format "  Compatibility aliases: %s\n"
                          (str/join ", " (map name compatibility-aliases)))))
    (.append sb "\n")
    (when (seq args)
      (.append sb "  Arguments:\n")
      (let [sorted-args (sort-by (fn [[_ v]] (if (:required v) 0 1)) args)]
        (doseq [[arg-key {:keys [required desc]}] sorted-args]
          (.append sb (format "    %-16s %s%s\n"
                              (str ":" (name arg-key))
                              (if required "(required) " "")
                              (or desc "")))))
      (.append sb "\n"))
    (when pair
      (.append sb (format "  See also: :op %s\n\n" (name pair))))
    (when (seq workflow)
      (.append sb "  Safe workflow:\n")
      (doseq [[index step] (map-indexed vector workflow)]
        (.append sb (format "    %d. %s\n" (inc index) step)))
      (.append sb "\n"))
    (when (seq examples)
      (.append sb "  Examples:\n")
      (doseq [ex examples]
        (.append sb (format "    %s\n" ex)))
      (.append sb "\n"))
    (str sb)))

;; ============================================================
;; Dispatch + CLI
;; ============================================================

(defn- with-cat-remedy
  [result opts]
  (if (or (not= :show-form (resolve-op (:op opts)))
          (contains? opts :name))
    (if-let [remedy (show-form/invocation-remedy opts)]
      (assoc-in result [:remedies :cat] remedy)
      result)
    result))

(defn- with-match-form-pattern-remedy
  [result {:keys [file inside pattern] :as opts}]
  (if (and file
           (contains? opts :pattern)
           (string? pattern)
           (not (str/blank? pattern))
           (not (contains? opts :match)))
    (if (str/includes? pattern "|")
      (let [args ["rg" "-n" "--max-count" "20" pattern (str file)]]
        (assoc-in result [:remedies :text-search]
                  {:operation :text-search
                   :reason (str ":match-form :match accepts one EDN form pattern, not a regular expression. "
                                "Inspect at most 20 matching lines, then :cat the containing form or refine the pattern")
                   :command (show-form/render-command args)
                   :command-args args}))
      (let [args (cond-> ["clj-surgeon" ":op" ":match-form"
                          ":file" (str file)]
                   (contains? opts :inside) (into [":inside" (str inside)])
                   true (into [":match" pattern]))]
        (assoc-in result [:remedies :match-form]
                  {:operation :match-form
                   :reason "Use :match for one structural EDN pattern, not a regular expression"
                   :command (show-form/render-command args)
                   :command-args args})))
    result))

(defn parse-spec-document
  "Parse exactly one EDN document from a spec source."
  [source source-label]
  (try
    (let [reader (java.io.PushbackReader. (java.io.StringReader. source))
          eof (Object.)
          value (edn/read {:eof eof} reader)
          trailing (edn/read {:eof eof} reader)]
      (when (identical? eof value)
        (throw (ex-info "Spec is empty" {})))
      (when-not (identical? eof trailing)
        (throw (ex-info "Spec must contain exactly one EDN form" {})))
      value)
    (catch Exception exception
      (throw (ex-info (str "Invalid spec from " source-label
                           ": " (.getMessage exception))
                      {:error-type :invalid-spec-document
                       :spec-source source-label}
                      exception)))))

(defn- read-stdin-spec
  []
  (if (.ready ^java.io.Reader *in*)
    (slurp *in*)
    (throw
      (ex-info
        "No spec document is attached to stdin"
        {:error-type :missing-spec-stdin
         :remedy "Pipe the manifest in the same shell action: printf '%s\\n' 'MANIFEST' | clj-surgeon :op OP :spec-file -"}))))

(defn- load-spec-input
  [{:keys [spec-file] :as opts}]
  (let [inline? (contains? opts :spec)
        file? (contains? opts :spec-file)]
    (cond
      (and inline? file?)
      (throw (ex-info "Provide exactly one of :spec or :spec-file"
                      {:error-type :conflicting-spec-inputs}))

      inline?
      opts

      file?
      (let [source-label (if (= "-" spec-file) "stdin" spec-file)
            source (try
                     (if (= "-" spec-file) (read-stdin-spec) (slurp spec-file))
                     (catch Exception exception
                       (if (:error-type (ex-data exception))
                         (throw exception)
                         (throw (ex-info (str "Cannot read spec from " source-label
                                              ": " (.getMessage exception))
                                         {:error-type :invalid-spec-source
                                          :spec-source source-label}
                                         exception)))))]
        (-> opts
            (dissoc :spec-file)
            (assoc :spec (parse-spec-document source source-label))))

      :else
      (throw (ex-info "Provide exactly one of :spec or :spec-file"
                      {:error-type :missing-spec-input})))))

(defn run [{:keys [op] :as opts}]
  ;; Load .clj-surgeon.edn project aliases from nearest config file
  (when-let [anchor (or (:file opts) (:clj opts) (:cljs opts) (:dir opts))]
    (forms/init-from-file! anchor))
  (let [canonical (resolve-op op)
        opts (if (or (#{:change :change!} canonical)
                     (and (= :show-form canonical)
                          (or (contains? opts :spec)
                              (contains? opts :spec-file))))
               (load-spec-input opts)
               opts)
        op-def (get ops-registry canonical)
        result (if op-def
                 (let [missing (->> (:args op-def)
                                    (keep (fn [[arg {:keys [required]}]]
                                            (when (and required
                                                       (not (contains? opts arg))
                                                       (not (and (= canonical :show-form)
                                                                 (= arg :file)
                                                                 (contains? opts :spec))))
                                              arg)))
                                    vec)]
                   (if (seq missing)
                     (cond-> {:error (str "Missing required arguments: "
                                          (str/join ", " (map #(str ":" (name %)) missing)))
                              :error-type :missing-arguments
                              :missing missing}
                       (= canonical :xray)
                       (assoc :usage "clj-surgeon :op :xray :file FILE :expr \"(-> (form 'NAME) (expect-count 1) (analyze pure-function))\"")

                       (= canonical :show-form)
                       (merge (show-form/refusal-context opts))

                       (and (= canonical :find-subform) (contains? opts :line))
                       (with-cat-remedy opts)

                       (and (= canonical :find-subform) (contains? opts :pattern))
                       (with-match-form-pattern-remedy opts))
                     (let [handler-result ((:handler op-def) opts)]
                       (if (and (= canonical :show-form) (:error handler-result))
                         (with-cat-remedy handler-result opts)
                         handler-result))))
                 (with-cat-remedy
                   {:error (str "Unknown op: " op
                                ". Valid ops: "
                                (str/join ", " (public-op-keys ops-registry)))
                    :error-type :unknown-operation
                    :usage "clj-surgeon :op :help"}
                   opts))]
    (if (string? result) (println result) (pp/pprint result))
    result))

(defn parse-val
  "Parse a single CLI value string into its Clojure equivalent.
   Pure: string in, value out."
  [s]
  (cond
    (= s "true") true
    (= s "false") false
    (.startsWith s ":") (keyword (subs s 1))
    (.startsWith s "[") (read-string s)  ;; parse EDN vectors after shell unquoting
    (.startsWith s "{") (read-string s)  ;; parse EDN maps
    :else s))

(defn parse-args
  "Parse CLI arg strings into an opts map.
   Pure: string sequence in, map out."
  [args]
  (let [help-flags #{"--help" "-h"}
        has-help?  (some help-flags args)
        kv-args    (remove help-flags args)]
    (when (odd? (count kv-args))
      (throw (ex-info "Arguments must be key-value pairs"
                      {:error-type :invalid-arguments})))
    (cond-> (->> kv-args
                 (partition 2)
                 (map (fn [[k v]]
                        (let [key (keyword (subs k 1))]
                          [key (if (#{:match :with :contains :query :expr :expect} key) v (parse-val v))])))
                 (into {}))
      has-help? (assoc :help true))))

(defn -main [& args]
  (try
    (let [result
          (cond
            (empty? args)
            (println (format-global-help ops-registry))

            (= ["--version"] (vec args))
            (pp/pprint {:tool "clj-surgeon"
                        :version structural-lens/tool-version})

            (= "up" (first args))
            (let [[_ workspace & extra] args]
              (cond
                (= "--help" workspace)
                (println (str "Usage: clj-surgeon up [WORKSPACE]\n\n"
                              "Idempotently joins an existing workspace to one shared "
                              "clj-surgeon and cclsp MCP stack. WORKSPACE defaults to cwd."))

                (seq extra)
                (throw (ex-info "Usage: clj-surgeon up [WORKSPACE]"
                                {:error-type :invalid-arguments}))

                :else
                (pp/pprint
                  ((requiring-resolve 'clj-surgeon.workspace-onboarding/up!)
                   {:workspace workspace}))))

            (= "recover" (first args))
            (let [[_ workspace & extra] args]
              (cond
                (= "--help" workspace)
                (println
                  (str "Usage: clj-surgeon recover [WORKSPACE]\n\n"
                       "Make one bounded repair attempt, then prove tools/list, "
                       "one exact semantic surface, and one guarded write. "
                       "WORKSPACE defaults to cwd. A typed semantic-provider-warming "
                       "result is not a recovery condition: wait and retry its "
                       "next_call once. Fallback-safe receipts contain executable "
                       "report-command and fallback-command vectors."))

                (seq extra)
                (throw (ex-info "Usage: clj-surgeon recover [WORKSPACE]"
                                {:error-type :invalid-arguments}))

                :else
                (pp/pprint
                  ((requiring-resolve 'clj-surgeon.recovery/recover!)
                   {:workspace workspace}))))

            (= "report-failure" (first args))
            (let [[_ flag receipt-file & extra] args]
              (cond
                (= "--help" flag)
                (println
                  (str "Usage: clj-surgeon report-failure --receipt PATH\n\n"
                       "Redact and deduplicate one local recovery failure. "
                       "Never uploads source, prompts, URLs, or workspace paths."))

                (or (not= "--receipt" flag)
                    (str/blank? receipt-file)
                    (seq extra))
                (throw
                  (ex-info
                    "Usage: clj-surgeon report-failure --receipt PATH"
                    {:error-type :invalid-arguments}))

                :else
                (pp/pprint
                  ((requiring-resolve
                     'clj-surgeon.failure-report/report-failure!)
                   {:receipt-file receipt-file}))))

            :else
            (let [opts (parse-args args)]
              (cond
                (contains? #{:help "help"} (:op opts))
                (println (format-global-help ops-registry))

                (and (:help opts) (nil? (:op opts)))
                (println (format-global-help ops-registry))

                (and (:help opts) (:op opts))
                (let [canonical (resolve-op (:op opts))
                      op-def (get ops-registry canonical)]
                  (if op-def
                    (println (format-op-help canonical op-def))
                    (let [error {:error (str "Unknown op: " (:op opts))
                                 :error-type :unknown-operation}]
                      (pp/pprint error)
                      error)))

                :else (run opts))))]
      (when (and (map? result) (:error result))
        (System/exit 1)))
    (catch Exception e
      (pp/pprint (merge (or (ex-data e) {})
                        {:error (.getMessage e)
                         :error-type (or (:error-type (ex-data e))
                                         :invalid-arguments)}))
      (System/exit 1))))
