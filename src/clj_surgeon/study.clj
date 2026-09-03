(ns clj-surgeon.study
  "One kernel for the read-only study operations.

   Every study operation is a pure-in/data-out function here. The CLI
   (`clj-surgeon.core`) is kernel plus print; the MCP read entrance
   (`inspect_clojure`) is kernel plus receipt. Neither entrance owns a second
   implementation, so `:ls-tree`, `:ls-deps`, `:deps`, `:topo`, and
   `:ls-extract` cannot drift between them.

   The write operations (`:mv`, `:rename-ns!`, `:fix-declares!`) are NOT part
   of this kernel and are deliberately absent from the MCP read entrance."
  (:require
   [babashka.fs :as fs]
   [babashka.process]
   [clj-surgeon.analyze :as analyze]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.outline :as outline]
   [clojure.edn :as edn]
   [clojure.string :as str]))

;; ============================================================
;; File-scoped study operations — source in, data out
;; ============================================================

;; @spec MCP-OP-STUDY-002
;; @spec MCP-OP-STUDY-008
;; @spec MCP-OP-STUDY-009
(defn deps
  "Intra-namespace call graph for one source string.

   With `:form`, return that single adjacency row, or nil when absent."
  [source {:keys [form]}]
  (let [rows (analyze/intra-ns-deps (analyze/string->zloc source))]
    (if form
      (first (filter #(= form (:name %)) rows))
      rows)))

;; @spec MCP-OP-STUDY-003
(defn topo
  "Topological form ordering for one source string."
  [source]
  (analyze/topological-sort (analyze/string->zloc source)))

;; @spec MCP-OP-STUDY-004
(defn ls-deps
  "Transitive dependency tree for one named form, or nil when absent."
  [source {:keys [form]}]
  (analyze/dep-tree
    (analyze/intra-ns-deps (analyze/string->zloc source))
    form))

;; @spec MCP-OP-STUDY-005
(defn ls-extract
  "Minimal extractable unit for one named form."
  [source {:keys [form]}]
  (analyze/extraction-closure (analyze/string->zloc source) form))

(defn owner-names
  "Bounded top-level owner vocabulary for one source string.

   Used only to make a missing-form refusal factual; never selection authority."
  [source]
  (vec (sort (map :name (analyze/intra-ns-deps (analyze/string->zloc source))))))

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

;; @spec MCP-OP-STUDY-014
(defn- find-build-files
  "Find deps.edn, project.clj, bb.edn under dir, skipping hidden/cache dirs.
   Uses system find with -prune for speed (~10x faster than fs/glob on large trees).

   Every hit is re-resolved against the canonical scan root: `find` reports a
   symlink by the LINK's own name, so a `deps.edn` symlinked out of the root
   would otherwise be slurped and read as a build file."
  [root dir]
  (try
    (let [;; argv, never `sh -c`: a workspace-confined directory name must not
          ;; be able to reach a shell from the MCP read entrance.
          prune-args (concat ["("]
                             (drop 1 (mapcat #(vector "-o" "-name" %) skip-dirs))
                             [")" "-prune" "-o"
                              "(" "-name" "deps.edn"
                              "-o" "-name" "project.clj"
                              "-o" "-name" "bb.edn" ")" "-print"])
          result (apply babashka.process/shell
                        {:out :string :err :string :continue true}
                        "find" (str dir) prune-args)]
      (if (zero? (:exit result))
        (->> (str/split-lines (str/trim (:out result)))
             (remove str/blank?)
             (filter #(some? (mcp-paths/real-path-within root %)))
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

(defn- read-build-file
  "Pure: read one build file's content WITHOUT evaluating anything in it.

   deps.edn and bb.edn are EDN, so `clojure.edn/read-string` reads them and
   has no eval reader at all. project.clj is Clojure source and needs the
   Clojure reader, so it is read with `*read-eval*` bound false, which makes
   `#=(...)` throw instead of run.

   Before this, every build file under a scanned tree reached
   `clojure.core/read-string` with `*read-eval*` at its ambient true: a
   `#=(clojure.core/spit ...)` inside ANY deps.edn/bb.edn/project.clj in the
   tree executed as the scanning process during discovery, and the silent
   catch below hid it."
  [filename source]
  (if (= "project.clj" filename)
    (binding [*read-eval* false]
      (read-string source))
    (edn/read-string source)))

(defn- extract-source-paths
  "I/O wrapper: read a build file and return its source paths."
  [build-file]
  (let [filename (str (fs/file-name build-file))]
    (try
      (source-paths-from-config filename
                                (read-build-file filename
                                                 (slurp (str build-file))))
      (catch Exception _e ["src"]))))

;; @spec MCP-OP-STUDY-014
(defn- find-clj-files
  "Find all .clj/.cljs/.cljc files under a directory using system find.

   Every hit is re-resolved against the canonical scan root and dropped when
   its realpath lands outside: `find` reports a symlink by the LINK's own
   name, so `src/leak.clj -> /etc/passwd` matches `-name '*.clj'` and would
   otherwise be outlined — that is, slurped — even though its bytes are
   outside the root. The path RETURNED is the one the walk produced, so the
   scan-relative rendering is unchanged; only escapes are removed."
  [root dir]
  (when (fs/directory? dir)
    (try
      (let [result (babashka.process/shell
                     {:out :string :err :string :continue true}
                     "find" (str dir)
                     "-name" "*.clj" "-o" "-name" "*.cljs" "-o" "-name" "*.cljc")]
        (when (zero? (:exit result))
          (->> (str/split-lines (str/trim (:out result)))
               (remove str/blank?)
               (filter #(some? (mcp-paths/real-path-within root %))))))
      (catch Exception _e nil))))

;; @spec MCP-OP-STUDY-014
(defn- confined-source-dirs
  "Resolve a build file's declared source paths under its project root,
   keeping only those that stay inside the canonical scan root.

   `(fs/path project-root \"../../..\")` is NOT normalized by `fs/path`, so an
   unnormalized escape used to be handed straight to `find` and moved the
   whole scan outside the root."
  [root project-root src-paths]
  (keep #(mcp-paths/normalized-path-within root project-root %) src-paths))

(defn- discover-projects
  "Find projects under dir via build files. Returns [{:name :root :files}].
   Falls back to recursive scan if no build files found.

   `root` is the canonical scan root: nothing outside it is discovered."
  [root dir]
  (let [dir (fs/path dir)
        build-files (find-build-files root dir)
        ;; Group by project root, keep first build file per root
        by-root (group-by #(str (fs/parent %)) build-files)]
    (if (seq by-root)
      (->> by-root
           (map (fn [[project-root files]]
                  (let [build-file (first files)
                        src-paths (extract-source-paths build-file)
                        root-path (fs/path project-root)
                        clj-files (->> (confined-source-dirs root root-path src-paths)
                                       (mapcat #(find-clj-files root %))
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
      (let [clj-files (->> (find-clj-files root dir)
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
    (let [;; "--" ends option parsing for both rg and grep, so a caller-
          ;; supplied pattern (e.g. "--pre=/bin/sh", which rg would run as a
          ;; per-file preprocessor command) is always taken as a plain
          ;; positional pattern, never as a flag.
          args (if (rg-available?)
                 ;; ripgrep: fast, respects .gitignore automatically
                 ;; Note: rg uses -i for case-insensitive (not -E which means encoding)
                 ["rg" "-li"
                  "-g" "*.clj" "-g" "*.cljs" "-g" "*.cljc"
                  "-g" "deps.edn" "-g" "project.clj" "-g" "bb.edn"
                  "--" pattern (str dir)]
                 ;; fallback: system grep
                 (let [exclude-args (mapcat #(vector "--exclude-dir" %)
                                            [".git" ".cpcache" ".gitlibs" "target"
                                             "node_modules" ".clj-kondo" ".lsp" ".shadow-cljs"])]
                   (concat ["grep" "-rliE"
                            "--include=*.clj" "--include=*.cljs" "--include=*.cljc"
                            "--include=deps.edn" "--include=project.clj" "--include=bb.edn"]
                           exclude-args
                           ["--" pattern (str dir)])))
          result (apply babashka.process/shell
                        {:out :string :err :string :continue true}
                        args)]
      (if (zero? (:exit result))
        (set (str/split-lines (str/trim (:out result))))
        #{}))
    (catch Exception _e #{})))

;; @spec MCP-OP-STUDY-012
(defn- ns-grep-hit?
  "Pure: true if a source file's SCAN-RELATIVE path plausibly names a
   namespace matching pattern. Tests the relative path as given and again
   with '_' turned into '-', because the Clojure require convention keeps a
   file's path in lockstep with its declared namespace (path segment <-> ns
   segment, '_' <-> '-'). Takes the path already relativized to the scanned
   dir — never the absolute filesystem path, whose ancestor directories
   (e.g. a checkout named `…-store` or `…-study`) could spuriously match.
   Never opens or parses the file — this is a path/namespace filter, never a
   file-contents filter (that is `grep`, via `filter-projects-by-hits`)."
  [pattern rel-path]
  (let [re (re-pattern pattern)]
    (boolean (or (re-find re rel-path)
                 (re-find re (str/replace rel-path "_" "-"))))))

;; @spec MCP-OP-STUDY-012
(defn filter-projects-by-ns-grep
  "Pure: keep only files whose path/namespace — relative to dir, never the
   absolute filesystem path — matches pattern. Narrower than
   `filter-projects-by-hits`/`grep`, which searches file bodies and matches
   any line containing the pattern — comments, strings, and unrelated
   substrings included. Drops projects left with no files."
  [projects dir pattern]
  (let [dir-path (fs/path dir)
        rel (fn [f] (str (fs/relativize dir-path (fs/path f))))]
    (->> projects
         (map (fn [project]
                (update project :files
                        (fn [files]
                          (filterv #(ns-grep-hit? pattern (rel %)) files)))))
         (remove #(empty? (:files %)))
         vec)))

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

;; @spec MCP-OP-STUDY-011
(defn format-ls-tree-names
  "Pure: format ls-tree results as a compact table of contents — exactly
   {:file :ns :form-count :line-count} per file and nothing else, so a whole
   tree's names fit well inside a bounded receipt where the fuller `text`/
   `edn` per-form rows would truncate. Expects projects with :outlines
   already computed."
  [projects dir]
  (vec
    (for [{:keys [outlines]} projects
          [f result] outlines
          :let [rel-path (str (fs/relativize (fs/path dir) (fs/path f)))]]
      {:file rel-path
       :ns (:ns result)
       :form-count (or (:form-count result) 0)
       :line-count (or (:lines result) 0)})))

(defn- find-nearest-build-file
  "Walk up from a file to find the nearest deps.edn/project.clj/bb.edn."
  [file-path stop-at]
  (loop [dir (fs/parent (fs/path file-path))]
    (when (and dir (str/starts-with? (str dir) (str stop-at)))
      (let [candidates [(str dir "/deps.edn") (str dir "/project.clj") (str dir "/bb.edn")]]
        (if-let [found (first (filter #(fs/exists? %) candidates))]
          {:build-file found :root (str dir)}
          (recur (fs/parent dir)))))))

;; @spec MCP-OP-STUDY-014
(defn- discover-projects-grep
  "Fast path: use rg/grep results to build project list without globbing.
   For projects with matching deps.edn: find all their source files.
   For individual matching source files: group by nearest project root.

   `root` is the canonical scan root; `grep-hits` are already confined to it."
  [root grep-hits dir]
  (let [build-files #{"deps.edn" "project.clj" "bb.edn"}
        {build-hits true src-hits false}
        (group-by #(contains? build-files (str (fs/file-name %))) grep-hits)

        ;; Projects with matching build files → find all their source files
        build-projects
        (->> (or build-hits [])
             (map (fn [bf]
                    (let [project-root (str (fs/parent (fs/path bf)))
                          src-paths (extract-source-paths bf)
                          clj-files (->> (confined-source-dirs
                                           root (fs/path project-root) src-paths)
                                         (mapcat #(find-clj-files root %))
                                         (remove nil?)
                                         sort
                                         vec)]
                      {:name (str (fs/file-name (fs/path project-root)))
                       :root project-root
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


;; ============================================================
;; :ls-tree kernel — directory in, data out
;; ============================================================

;; @spec MCP-OP-STUDY-014
(defn- canonical-scan-root
  "The canonical realpath of the directory to scan, or nil when it is not an
   existing directory. Everything discovery reports must resolve inside it."
  [dir]
  (try
    (let [root (mcp-paths/real-root dir)]
      (when (fs/directory? (str root)) root))
    (catch Exception _e nil)))

;; @spec MCP-OP-STUDY-001
;; @spec MCP-OP-STUDY-012
(defn ls-tree
  "Scan one absolute-or-relative directory and return outlined projects.

   `grep` filters by file CONTENTS (ripgrep over file bodies — matches
   comments, strings, and unrelated substrings). `ns-grep` filters by each
   file's PATH/namespace instead (see `filter-projects-by-ns-grep`) and is
   the narrower, structural answer to 'table of contents filtered to
   namespaces matching X'. Both may be given; ns-grep narrows whatever grep
   (or the full scan) already found.

   Returns {:ok true :dir <absolutized> :projects [...]} or a typed refusal.
   Printing, exit codes, and receipts belong to the entrances, not here."
  [{:keys [dir grep ns-grep] :as _opts}]
  (cond
    (not dir)
    {:ok false
     :error-type :missing-dir
     :error "Error: :dir is required for :ls-tree"}

    ;; Defense in depth alongside grep-tree's "--" argv separator: a pattern
    ;; that starts with '-' would otherwise look like a flag to rg/grep (or
    ;; to a future caller of the pattern in another context). Refused before
    ;; any subprocess or scan runs.
    (and grep (str/starts-with? grep "-"))
    {:ok false
     :error-type :invalid-grep-pattern
     :dir dir
     :grep grep
     :error "Error: :grep must not start with '-'"}

    (and ns-grep (str/starts-with? ns-grep "-"))
    {:ok false
     :error-type :invalid-ns-grep-pattern
     :dir dir
     :ns-grep ns-grep
     :error "Error: :ns-grep must not start with '-'"}

    (nil? (canonical-scan-root dir))
    {:ok false
     :error-type :dir-not-found
     :dir (str (fs/absolutize dir))
     :error (format "Error: :dir %s is not an existing directory" dir)}

    :else
    ;; The canonical realpath of the scanned directory IS the confinement
    ;; boundary for everything discovery reports. `fs/absolutize` alone left
    ;; symlinked components unresolved, so a realpath comparison against it
    ;; could not be made at all.
    (let [root (canonical-scan-root dir)
          dir (str root)
          discovered (if grep
                       ;; Fast path: rg first, skip expensive directory globbing
                       (let [hits (->> (grep-tree grep dir)
                                       (filter #(some? (mcp-paths/real-path-within
                                                         root %))))]
                         (discover-projects-grep root hits dir))
                       ;; Full scan: discover all projects
                       (discover-projects root dir))
          projects (if ns-grep
                     (filter-projects-by-ns-grep discovered dir ns-grep)
                     discovered)]
      (if (empty? projects)
        {:ok false
         :error-type :no-clojure-files
         :dir dir
         :grep grep
         :ns-grep ns-grep
         ;; `(when grep ...)` here would render the literal string "null" into
         ;; the message; this branch was unreachable before the format shadow
         ;; was removed, so no caller depended on that spelling.
         :error (format "No Clojure files found under %s%s%s"
                        dir
                        (if grep (str " matching '" grep "'") "")
                        (if ns-grep (str " with ns/path matching '" ns-grep "'") ""))}
        {:ok true
         :dir dir
         :grep grep
         :ns-grep ns-grep
         :projects (outline-all-files projects)}))))
