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

;; @spec MCP-OP-STUDY-013
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

;; @spec MCP-OP-STUDY-013
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
;; @spec MCP-OP-STUDY-021
(defn- walk-clj-files
  "Find all .clj/.cljs/.cljc files under a directory using system find.
   Returns `[[walk-path canonical-path] ...]`.

   Every hit is re-resolved against the canonical scan root and dropped when
   its realpath lands outside: `find` reports a symlink by the LINK's own
   name, so `src/leak.clj -> /etc/passwd` matches `-name '*.clj'` and would
   otherwise be outlined — that is, slurped — even though its bytes are
   outside the root. The path RETURNED first is the one the walk produced, so
   the scan-relative rendering is unchanged; only escapes are removed.

   The canonical path travels alongside it because it is the identity a file
   is deduplicated by: two projects whose declared `:paths` overlap reach the
   same bytes, and the same bytes must be counted and printed once."
  [root dir]
  (when (fs/directory? dir)
    (try
      (let [;; -prune the cache/vendor directories exactly as find-build-files
            ;; does. Without it a scan walked every file under target/,
            ;; node_modules/, and .gitlibs/ and then filtered by name.
            prune-args (concat ["("]
                               (drop 1 (mapcat #(vector "-o" "-name" %) skip-dirs))
                               [")" "-prune" "-o"
                                "(" "-name" "*.clj"
                                "-o" "-name" "*.cljs"
                                "-o" "-name" "*.cljc" ")" "-print"])
            result (apply babashka.process/shell
                          {:out :string :err :string :continue true}
                          "find" (str dir) prune-args)]
        (when (zero? (:exit result))
          (->> (str/split-lines (str/trim (:out result)))
               (remove str/blank?)
               (keep (fn [path]
                       (when-let [canonical (mcp-paths/real-path-within root path)]
                         [path (str canonical)])))
               vec)))
      (catch Exception _e nil))))

;; @spec MCP-OP-STUDY-021
(defn- source-dir-files
  "One walk per source DIRECTORY per scan, memoized in `cache`.

   500 sibling `deps.edn` files each declaring `:paths [\"..\"]` all resolve
   to the same directory. Walking it once per project cost 500 walks of the
   same 500 files — 8.4 s — and reported `file_count` 250,500."
  [cache root dir]
  (let [key (str dir)]
    (if (contains? @cache key)
      (get @cache key)
      (let [found (vec (walk-clj-files root dir))]
        (swap! cache assoc key found)
        found))))

;; @spec MCP-OP-STUDY-014
(defn- confined-source-dirs
  "Resolve a build file's declared source paths under its project root,
   keeping only those that stay inside the canonical scan root.

   `(fs/path project-root \"../../..\")` is NOT normalized by `fs/path`, so an
   unnormalized escape used to be handed straight to `find` and moved the
   whole scan outside the root."
  [root project-root src-paths]
  (keep #(mcp-paths/normalized-path-within root project-root %) src-paths))

(def ^:private empty-accumulation
  {:seen #{} :projects []})

;; @spec MCP-OP-STUDY-021
(defn- accumulate-projects
  "Fold project candidates into a DEDUPLICATED, capped file set.

   `candidates` is a seq of THUNKS, each returning
   `{:name :root :files [[walk-path canonical-path] ...]}`. They are thunks so
   that `cap` stops the WALK and not merely the count: once the distinct file
   count passes the cap, no later candidate is ever listed. The cap used to be
   compared against a count that `discover-projects` had already materialised
   in full, which is the opposite of a bound on work.

   A file belongs to exactly one project — the first candidate to reach it —
   keyed by its CANONICAL path. Overlapping declarations (a root project
   declaring `:paths [\".\"]` beside a nested project, or 500 siblings each
   declaring `:paths [\"..\"]`) therefore count and print each file once,
   where before a two-file tree reported five files and printed them twice
   over.

   Candidates sharing a project root are merged rather than listed twice."
  [cap candidates state]
  (loop [remaining (seq candidates)
         seen (:seen state)
         projects (:projects state)]
    (if-not remaining
      {:ok true :seen seen :projects projects :file-count (count seen)}
      (let [{:keys [name root files]} ((first remaining))
            [seen-after kept]
            (reduce (fn [[seen kept] [walk-path canonical]]
                      (if (contains? seen canonical)
                        [seen kept]
                        [(conj seen canonical) (conj kept walk-path)]))
                    [seen []]
                    files)
            index (when (seq kept)
                    (first (keep-indexed (fn [i p] (when (= root (:root p)) i))
                                         projects)))
            projects-after (cond
                             (empty? kept) projects
                             index (update-in projects [index :files]
                                              #(vec (sort (concat % kept))))
                             :else (conj projects {:name name
                                                   :root root
                                                   :files (vec (sort kept))}))]
        (if (> (count seen-after) cap)
          {:ok false
           :seen seen-after
           :projects projects-after
           :file-count (count seen-after)
           :halted-early (boolean (next remaining))}
          (recur (next remaining) seen-after projects-after))))))

(defn- by-name
  "Discovery's display order: projects sorted by name, files already sorted."
  [accumulation]
  (update accumulation :projects #(vec (sort-by :name %))))

;; @spec MCP-OP-STUDY-021
(defn- build-project-candidates
  "One thunk per project root, DEEPEST root first, so the most specific
   project owns its own files and an outer project that declares `:paths
   [\".\"]` takes only what is left."
  [cache root by-root]
  (map (fn [[project-root files]]
         (fn []
           (let [build-file (first files)
                 src-paths (extract-source-paths build-file)
                 root-path (fs/path project-root)]
             {:name (str (fs/file-name root-path))
              :root (str root-path)
              :files (mapcat #(source-dir-files cache root %)
                             (confined-source-dirs root root-path src-paths))})))
       (sort-by (fn [[project-root _]] [(- (count project-root)) project-root])
                by-root)))

;; @spec MCP-OP-STUDY-021
(defn- discover-projects
  "Find projects under dir via build files, deduplicated and capped as it goes.

   Returns `{:ok true :projects [{:name :root :files}] :file-count n}` or
   `{:ok false :file-count n}` when discovery passed `cap`.
   Falls back to a recursive scan if no build files are found.

   `root` is the canonical scan root: nothing outside it is discovered."
  [root dir cap]
  (let [dir (fs/path dir)
        cache (atom {})
        build-files (find-build-files root dir)
        ;; Group by project root, keep first build file per root
        by-root (group-by #(str (fs/parent %)) build-files)]
    (by-name
      (if (seq by-root)
        (accumulate-projects cap
                             (build-project-candidates cache root by-root)
                             empty-accumulation)
        ;; No build files — fallback to recursive scan
        (accumulate-projects
          cap
          [(fn []
             {:name (str (fs/file-name dir))
              :root (str dir)
              :files (remove (fn [[walk-path _]] (in-skip-dir? walk-path dir))
                             (source-dir-files cache root dir))})]
          empty-accumulation)))))

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
  ;; One probe per scan. `rg --version` was spawned twice — once to decide
  ;; whether to warn, once to build the argv — so every grep scan paid for two
  ;; subprocesses before doing any work.
  (let [rg? (rg-available?)]
    (when-not rg?
      (binding [*out* *err*]
        (println "WARNING: ripgrep (rg) not found. Falling back to grep (much slower).")
        (println "Install: brew install ripgrep  OR  apt install ripgrep")))
    (try
      (let [;; "--" ends option parsing for both rg and grep, so a caller-
            ;; supplied pattern (e.g. "--pre=/bin/sh", which rg would run as a
            ;; per-file preprocessor command) is always taken as a plain
            ;; positional pattern, never as a flag.
            args (if rg?
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
      (catch Exception _e #{}))))

;; @spec MCP-OP-STUDY-022
(defn compile-pattern
  "Compile one caller-supplied regular expression, or nil when the engine
   rejects it or it is not even a string.

   `re-pattern` used to be called PER FILE inside `ns-grep-hit?`: an
   uncompilable pattern such as \"[\" threw a raw `PatternSyntaxException`
   out of the read entrance — no typed error, no `read_complete`, no
   continuation — and a compilable one paid a fresh compile for every file in
   the tree."
  [pattern]
  (try
    (when (string? pattern)
      (re-pattern pattern))
    (catch Exception _e nil)))

;; @spec MCP-OP-STUDY-012
(defn- ns-grep-hit?
  "Pure: true if a source file's SCAN-RELATIVE path plausibly names a
   namespace matching the ALREADY COMPILED pattern. Tests the relative path as
   given and again with '_' turned into '-', because the Clojure require
   convention keeps a file's path in lockstep with its declared namespace
   (path segment <-> ns segment, '_' <-> '-'). Takes the path already
   relativized to the scanned dir — never the absolute filesystem path, whose
   ancestor directories (e.g. a checkout named `…-store` or `…-study`) could
   spuriously match. Never opens or parses the file — this is a path/namespace
   filter, never a file-contents filter (that is `grep`, via
   `filter-projects-by-hits`)."
  [re rel-path]
  (boolean (or (re-find re rel-path)
               (re-find re (str/replace rel-path "_" "-")))))

;; @spec MCP-OP-STUDY-012
;; @spec MCP-OP-STUDY-022
(defn filter-projects-by-ns-grep
  "Pure: keep only files whose path/namespace — relative to dir, never the
   absolute filesystem path — matches pattern. Narrower than
   `filter-projects-by-hits`/`grep`, which searches file bodies and matches
   any line containing the pattern — comments, strings, and unrelated
   substrings included. Drops projects left with no files.

   The pattern is compiled ONCE here; an uncompilable one is refused by
   `ls-tree` long before this."
  [projects dir pattern]
  (let [dir-path (fs/path dir)
        re (compile-pattern pattern)
        rel (fn [f] (str (fs/relativize dir-path (fs/path f))))]
    (if-not re
      []
      (->> projects
           (map (fn [project]
                  (update project :files
                          (fn [files]
                            (filterv #(ns-grep-hit? re (rel %)) files)))))
           (remove #(empty? (:files %)))
           vec))))

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

;; @spec MCP-OP-STUDY-021
(defn total-file-count
  "How many DISTINCT source files discovery found across projects.

   Summing the per-project counts double-counted every file two overlapping
   projects both claimed; discovery now hands each file to exactly one project,
   and this counts distinctly so that it stays true even if a caller assembles
   the project list some other way."
  [projects]
  (count (into #{} (mapcat :files) projects)))

(defn- files-in-scan-order
  [projects]
  (for [[index project] (map-indexed vector projects)
        file (:files project)]
    [index file]))

;; @spec MCP-OP-STUDY-015
;; @spec MCP-OP-STUDY-024
(defn outline-take
  "Outline the FIRST n files across projects in scan order, and return the
   projects rebuilt with `:outlines`. Every project is kept, `:files` intact,
   even when the bound reached none of its files: a project the receipt could
   not carry is still one the scan found, and the renderer names it.

   Outlining is the expensive half of `:ls-tree`: it opens and PARSES every
   file it is given. It is therefore deliberately not part of discovery. A
   caller with a byte budget grows n until the rendered receipt overflows and
   never pays to parse a tree it cannot return. Before this, the whole tree
   was outlined up front — 1072 files, 618 MB of heap, to return three.

   `cache` is an atom of file -> outline, so a growing bound never re-parses
   a file.

   `map-fn` maps `(fn [file] [file outline])` across the files still missing.
   It may run in any order and on any threads — results are re-keyed by file,
   so a strategy changes only the order work is done in, never the answer. It
   defaults to serial `map` because this kernel must keep loading under
   babashka, where the CLI runs; the JVM MCP entrance passes a bounded
   claypoole pool (`clj-surgeon.parallel/bounded-map`)."
  ([projects n cache] (outline-take projects n cache map))
  ([projects n cache map-fn]
   (let [wanted (vec (take n (files-in-scan-order projects)))
         missing (->> (map second wanted)
                      distinct
                      (remove #(contains? @cache %))
                      vec)]
     (when (seq missing)
       (swap! cache into (map-fn (fn [file] [file (safe-outline file)]) missing)))
     (let [outlines @cache
           by-project (group-by first wanted)]
       ;; Projects with no outlines are KEPT, carrying their `:files`. A
       ;; project the receipt's byte budget did not reach is still a project
       ;; the scan found: dropping it here made it vanish from the body while
       ;; `project_count` still counted it.
       (->> (map-indexed vector projects)
            (mapv (fn [[index project]]
                    (assoc project :outlines
                           (mapv (fn [[_ file]] [file (get outlines file)])
                                 (get by-project index []))))))))))

;; @spec MCP-OP-STUDY-015
(defn outline-all
  "Outline every discovered file — the whole-tree rendering the CLI prints."
  ([projects] (outline-all projects map))
  ([projects map-fn]
   (outline-take projects (total-file-count projects) (atom {}) map-fn)))

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

;; @spec MCP-OP-STUDY-017
;; @spec MCP-OP-STUDY-024
(defn format-ls-tree-text
  "Pure: format ls-tree results as compact text for LLM/human scanning.
   Expects projects with :outlines already computed.

   `:file-count`, when supplied, is the TRUE number of files discovered. The
   trailing total line counted only the outlines it was handed, so a bounded
   receipt that returned 3 of 1072 files ended with `total: 3 files` while its
   own envelope said `file_count 1072` — the body contradicted the receipt it
   travelled in. When the true count differs from what is shown, the line
   reports the true total plus what was shown and omitted, and omits the form
   total, which is unknowable for files that were deliberately never parsed.
   With no `:file-count`, or when everything is shown, the complete line is
   unchanged.

   A per-project header obeys the same rule against its project's own `:files`:
   `── beta (50 files, 71 forms)` when the receipt carries them all, and
   `── beta (50 files; 9 shown)` when it does not — including `0 shown`, so a
   project the bound never reached is named rather than silently absent while
   `project_count` still counts it."
  ([projects dir] (format-ls-tree-text projects dir nil))
  ([projects dir {:keys [file-count]}]
   (let [sb (StringBuilder.)
         multi-project? (> (count projects) 1)
         shown-files (reduce + (map #(count (:outlines %)) projects))
         total-files (or file-count shown-files)
         total-forms (reduce + (map (fn [p]
                                      (reduce + (map #(or (:form-count (second %)) 0)
                                                     (:outlines p))))
                                    projects))]
     (doseq [{:keys [name files outlines]} projects
             :let [project-forms (reduce + (map #(or (:form-count (second %)) 0) outlines))
                   shown (count outlines)
                   ;; The files DISCOVERY found for this project, not the
                   ;; subset the receipt could carry. The header counted the
                   ;; outlines it was handed, so a project showing 9 of 50
                   ;; announced itself as 9 files.
                   project-files (if files (count files) shown)]]
       (when multi-project?
         (.append sb (if (= shown project-files)
                       (format "── %s (%d files, %d forms)\n\n"
                               name project-files project-forms)
                       ;; No form total: it is unknowable for files this
                       ;; receipt deliberately never parsed.
                       (format "── %s (%d files; %d shown)\n\n"
                               name project-files shown))))
       (doseq [[f result] outlines
               :let [rel-path (str (fs/relativize (fs/path dir) (fs/path f)))]]
         (.append sb (format-file-text result rel-path))
         (.append sb "\n")))
     (if (= total-files shown-files)
       (.append sb (format "── total: %d files, %d forms\n" total-files total-forms))
       (.append sb (format "── total: %d files; %d shown, %d omitted\n"
                           total-files shown-files (- total-files shown-files))))
     (str sb))))

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
;; @spec MCP-OP-STUDY-021
(defn- discover-projects-grep
  "Fast path: use rg/grep results to build project list without globbing.
   For projects with matching deps.edn: find all their source files.
   For individual matching source files: group by nearest project root.

   `root` is the canonical scan root; `grep-hits` are already confined to it.

   Two accumulation phases, sharing one deduplicated file set and one cap.
   The build phase runs first and lazily, so the cap stops the walk; only then
   are the source hits it did NOT claim grouped into their own projects, which
   is what keeps a build project that yielded no files from swallowing a hit
   underneath it."
  [root grep-hits dir cap]
  (let [cache (atom {})
        build-files #{"deps.edn" "project.clj" "bb.edn"}
        {build-hits true src-hits false}
        (group-by #(contains? build-files (str (fs/file-name %))) grep-hits)

        ;; Projects with matching build files → find all their source files
        built (accumulate-projects
                cap
                (map (fn [build-file]
                       (fn []
                         (let [project-root (str (fs/parent (fs/path build-file)))]
                           {:name (str (fs/file-name (fs/path project-root)))
                            :root project-root
                            :files (mapcat
                                     #(source-dir-files cache root %)
                                     (confined-source-dirs
                                       root (fs/path project-root)
                                       (extract-source-paths build-file)))})))
                     (sort-by #(vector (- (count (str %))) (str %))
                              (or build-hits [])))
                empty-accumulation)]
    (if-not (:ok built)
      built
      (let [build-roots (set (map :root (:projects built)))
            ;; Source file hits not in a build-matched project → group by
            ;; nearest project root
            orphan-src-hits (remove #(some (fn [r]
                                             (str/starts-with? (str %) (str r "/")))
                                           build-roots)
                                    (or src-hits []))]
        (by-name
          (accumulate-projects
            cap
            (->> orphan-src-hits
                 (map (fn [file]
                        (let [info (find-nearest-build-file file dir)]
                          (if info
                            (assoc info :file (str file))
                            ;; Loose file — no build file found; use parent dir
                            ;; as root
                            {:root (str (fs/parent (fs/path file)))
                             :file (str file)}))))
                 (group-by :root)
                 (map (fn [[project-root entries]]
                        (fn []
                          {:name (str (fs/file-name (fs/path project-root)))
                           :root project-root
                           :files (keep (fn [{:keys [file]}]
                                          (when-let [canonical
                                                     (mcp-paths/real-path-within
                                                       root file)]
                                            [file (str canonical)]))
                                        entries)}))))
            (select-keys built [:seen :projects])))))))


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

(def default-max-scan-files
  "How many source files one `:ls-tree` scan may DISCOVER before refusing.

   A cap on discovery, not on the receipt: the receipt's own byte budget
   already decides how many files come back. This exists because the work
   between 'directory named' and 'first byte budgeted' used to be unbounded —
   the whole tree was parsed before any bound applied."
  2000)

(def max-scan-files-ceiling
  "Highest `:max-files` a request may ask for."
  20000)

;; @spec MCP-OP-STUDY-001
;; @spec MCP-OP-STUDY-012
;; @spec MCP-OP-STUDY-015
(defn ls-tree
  "Discover the projects and source files under one directory. Parses nothing.

   `grep` filters by file CONTENTS (ripgrep over file bodies — matches
   comments, strings, and unrelated substrings). `ns-grep` filters by each
   file's PATH/namespace instead (see `filter-projects-by-ns-grep`) and is
   the narrower, structural answer to 'table of contents filtered to
   namespaces matching X'. Both may be given; ns-grep narrows whatever grep
   (or the full scan) already found.

   Returns {:ok true :dir <canonical> :file-count n :projects [{:name :root
   :files}]} or a typed refusal. Outlining is the separate, bounded step
   (`outline-take`/`outline-all`), so a caller with a byte budget never pays
   to parse a tree it cannot return.

   Printing, exit codes, and receipts belong to the entrances, not here."
  [{:keys [dir grep ns-grep max-files] :as _opts}]
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

    ;; Compiled once, here, under a guard. `re-pattern` ran per file, so
    ;; `:ns-grep "["` left the read entrance as a raw PatternSyntaxException.
    (and ns-grep (nil? (compile-pattern ns-grep)))
    {:ok false
     :error-type :invalid-ns-grep-pattern
     :dir dir
     :ns-grep ns-grep
     :error (format "Error: :ns-grep %s is not a valid regular expression"
                    (pr-str ns-grep))}

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
          cap (min (or max-files default-max-scan-files) max-scan-files-ceiling)
          discovery (if grep
                      ;; Fast path: rg first, skip expensive directory globbing
                      (let [hits (->> (grep-tree grep dir)
                                      (filter #(some? (mcp-paths/real-path-within
                                                        root %))))]
                        (discover-projects-grep root hits dir cap))
                      ;; Full scan: discover all projects
                      (discover-projects root dir cap))
          projects (if (and (:ok discovery) ns-grep)
                     (filter-projects-by-ns-grep (:projects discovery) dir ns-grep)
                     (:projects discovery))
          file-count (total-file-count projects)]
      (cond
        ;; Refused DURING discovery — which only lists names — and before any
        ;; file is opened. The count and the cap are both named so the caller
        ;; can choose a remedy instead of guessing one.
        (not (:ok discovery))
        {:ok false
         :error-type :study-tree-too-large
         :dir dir
         :grep grep
         :ns-grep ns-grep
         :file-count (:file-count discovery)
         :max-files cap
         :error (format "Discovery found %s%d Clojure files under %s, above the %d-file scan cap"
                        (if (:halted-early discovery) "at least " "")
                        (:file-count discovery) dir cap)
         :remedy (format (str "Scan a subdirectory, add a grep or ns_grep "
                              "pattern, or raise max_files (ceiling %d).")
                         max-scan-files-ceiling)}

        (empty? projects)
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

        :else
        {:ok true
         :dir dir
         :grep grep
         :ns-grep ns-grep
         :file-count file-count
         :projects projects}))))
