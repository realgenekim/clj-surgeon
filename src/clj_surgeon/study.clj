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
   [clj-surgeon.argv-depth :as argv-depth]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.parse-admission :as admission]
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

;; @spec MCP-OP-STUDY-030
(def ^:private skip-dirs
  "Directories to skip during project discovery.

   Matched by NAME at any depth, which is a known limitation, not an accident:
   `target/` is pruned wherever it appears, so a real source namespace living
   under `src/app/target/` is invisible to `:ls-tree` exactly as compiled
   output under a project's own `target/` is. `find`'s `-prune` has no
   path-anchored form here, and anchoring would mean walking every candidate
   directory to decide. `test-fixtures/study/prune-target` and its frozen
   golden hold both cases, so the day this becomes path-anchored the golden
   changes and someone reads this note."
  #{".git" ".cpcache" ".gitlibs" "target" "node_modules"
    ".clj-kondo" ".lsp" ".shadow-cljs" ".nrepl" ".idea" ".vscode"})

(defn- in-skip-dir?
  "True if the path (relative to root) passes through any skip directory."
  [path root]
  (let [rel (str (fs/relativize root path))]
    (boolean (some skip-dirs (str/split rel #"/")))))

;; @spec MCP-OP-SHELL-ARGV-001
(defn existing-directory?
  "True only when `path` names an existing directory. Never throws: a string
   that is not a legal path (an embedded NUL, say) is simply not a directory."
  [path]
  (try (boolean (fs/directory? (str path)))
       (catch Exception _e false)))

;; @spec MCP-OP-SHELL-ARGV-001
(defn- find-start-token
  "Render a directory as a `find` start-point token. A RELATIVE path beginning
   with `-` would be parsed by find as an OPTION rather than a path, so prefix
   it with `./`. Absolute paths are already unambiguous."
  [path]
  (let [s (str path)]
    (if (str/starts-with? s "-") (str "./" s) s)))

;; @spec MCP-OP-SHELL-ARGV-003
(defn- nul-separated-paths
  "Split NUL-delimited command output into paths, dropping the empty trailing
   token. Never trims: leading or trailing whitespace, and NEWLINES, are legal
   inside a path and are data, not framing. `str/split-lines` turned one real
   path into two fictional ones and silently dropped the project (Andon pull
   inb-d27b79, 2026-09-03)."
  [out]
  (->> (str/split (str out) #"\u0000")
       (remove #(= "" %))
       vec))

;; @spec MCP-OP-STUDY-014
;; @spec MCP-OP-SHELL-ARGV-001
;; @spec MCP-OP-SHELL-ARGV-003
(defn- find-build-files
  "Find deps.edn, project.clj, bb.edn under dir, skipping hidden/cache dirs.
   Uses system find with -prune for speed (~10x faster than fs/glob on large trees).

   Every hit is re-resolved against the canonical scan root: `find` reports a
   symlink by the LINK's own name, so a `deps.edn` symlinked out of the root
   would otherwise be slurped and read as a build file.

   `-H` follows a symlink given as the START POINT only. `existing-directory?`
   uses Files.isDirectory, which follows links, so a symlinked root PASSES the
   entrance gate; find's `-P` default would then refuse to descend it and
   discovery would return nothing for a root the gate accepted. `-H` makes the
   gate and the executor answer the same question, and it does NOT follow links
   found inside the tree, so MCP-OP-STUDY-014's confinement and
   MCP-OP-STUDY-035's named `:paths` skip are both unchanged.

   Output is `-print0`: a directory name may contain a newline, and that is
   data, not a separator."
  [root dir]
  (if-not (existing-directory? dir)
    []
    (try
      (let [;; argv, never `sh -c`: a workspace-confined directory name must not
            ;; be able to reach a shell from the MCP read entrance.
            ;; @spec MCP-OP-STUDY-014
            ;; `-mindepth 1` for the same reason `walk-clj-files` carries it:
            ;; the prune list must never eat the START POINT. A resolved scan
            ;; root can be named `target`, and pruning it at depth 0 reports an
            ;; ordinary workspace as empty.
            prune-args (concat ["-mindepth" "1" "("]
                               (drop 1 (mapcat #(vector "-o" "-name" %) skip-dirs))
                               [")" "-prune" "-o"
                                "(" "-name" "deps.edn"
                                "-o" "-name" "project.clj"
                                "-o" "-name" "bb.edn" ")" "-print0"])
            result (apply babashka.process/shell
                          {:out :string :err :string :continue true}
                          "find" "-H" (find-start-token dir) prune-args)]
        (if (zero? (:exit result))
          (->> (nul-separated-paths (:out result))
               (filter #(some? (mcp-paths/real-path-within root %)))
               sort
               vec)
          []))
      (catch Exception _e []))))

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
  "Pure: read one build file's content AS DATA, with a reader that does not
   evaluate.

   @spec MCP-OP-SHELL-ARGV-004 — `clojure.edn/read-string` for EVERY build
   file, `deps.edn`, `bb.edn` and `project.clj` alike. This used to read
   `project.clj` with `clojure.core/read-string` inside
   `(binding [*read-eval* false] …)`, on the ground that a Leiningen project
   file is Clojure source rather than EDN. MCP-OP-SHELL-ARGV-004 rules that
   out in as many words: \"as data\" means a reader for which `*read-eval*`
   is NOT CONSULTED, not a reader called with `*read-eval*` bound false,
   because a binding is a property of one CALL SITE and the requirement is a
   property of the READER. The binding was correct today and one refactor away
   from being somewhere else.

   `edn/read-string` reads every shape `source-paths-from-config` was written
   for — the `deps.edn` and `bb.edn` maps, and the `project.clj` list whose
   `:source-paths` is looked up positionally. A `project.clj` that genuinely
   needs code reading now throws and falls back to the default source paths,
   which is the same answer this already gave for an unreadable build file:
   a refusal to guess, not a regression.

   @spec MCP-OP-SHELL-ARGV-005 — and it removes this namespace's last call to
   an evaluating reader, which is what the src-scanning oracle for that spec
   exists to keep at zero."
  [_filename source]
  (edn/read-string source))

;; @spec MCP-OP-STUDY-013
;; @spec MCP-OP-SHELL-ARGV-007
(defn- extract-source-paths
  "I/O wrapper: read a build file and return its source paths.

   @spec MCP-OP-SHELL-ARGV-007 — the nesting ceiling is applied to the file's
   BYTES, before any reader touches them, and OUTSIDE the `try` below. That
   placement is the requirement rather than a detail: the default-paths
   fallback is the right answer for a build file this tool cannot PARSE and
   the wrong answer for one it is REFUSING, because a caller told nothing
   cannot tell a build file that was ignored from one that was never read."
  [build-file]
  (let [filename (str (fs/file-name build-file))
        text (try (slurp (str build-file)) (catch Exception _e nil))]
    (when text (argv-depth/refuse-over-nested-build-file! build-file text))
    (try
      (source-paths-from-config filename (read-build-file filename text))
      (catch Exception _e ["src"]))))


;; @spec MCP-OP-STUDY-014
;; @spec MCP-OP-STUDY-021
;; @spec MCP-OP-STUDY-033
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
   same bytes, and the same bytes must be counted and printed once.

   `limit` bounds the entries this returns AND the work spent producing them.
   The pipeline is a transducer over `into`, not a lazy `keep`: a lazy seq
   over the vector `split-lines` returns is CHUNKED, so it would canonicalise
   32 paths to yield 11. `keep?` is a cheap string test applied before any
   `toRealPath` syscall, so a caller that filters afterwards cannot make the
   limit under-count."
  [root dir limit keep?]
  (when (fs/directory? dir)
    (try
      (let [;; -prune the cache/vendor directories exactly as find-build-files
            ;; does. Without it a scan walked every file under target/,
            ;; node_modules/, and .gitlibs/ and then filtered by name.
            ;; @spec MCP-OP-STUDY-014
            ;; `-mindepth 1`, so the prune list can never eat the START POINT.
            ;; `skip-dirs` names `target`, `node_modules` and `.git` because a
            ;; scan should not DESCEND into them; it does not mean a caller may
            ;; not NAME one. Field evidence (the trunk's
            ;; `the-fence-does-not-refuse-an-ordinary-path-under-a-symlinked-root`,
            ;; arriving with the round-twelve merge): the fixture's real tree is
            ;; a directory literally named `target` behind a symlink named
            ;; `link`, and `ls-tree` resolves its scan root before walking — so
            ;; the start point became `…/target`, `find` pruned it at depth 0,
            ;; and an entirely ordinary workspace reported `No Clojure files
            ;; found`. Canonicalising a root can RENAME it into the skip list,
            ;; which makes this a property of the resolved path rather than of
            ;; anything the caller did. The start point is never a build file or
            ;; a source file itself, so excluding depth 0 from the results costs
            ;; nothing.
            prune-args (concat ["-mindepth" "1" "("]
                               (drop 1 (mapcat #(vector "-o" "-name" %) skip-dirs))
                               [")" "-prune" "-o"
                                "(" "-name" "*.clj"
                                "-o" "-name" "*.cljs"
                                "-o" "-name" "*.cljc" ")" "-print0"])
            ;; -P (find's default) on purpose: MCP-OP-STUDY-035 names a
            ;; `:paths` entry that IS a symlink as a typed skip rather than
            ;; descending it, and that skip is decided before this walk runs.
            result (apply babashka.process/shell
                          {:out :string :err :string :continue true}
                          "find" (find-start-token dir) prune-args)]
        (when (zero? (:exit result))
          (into []
                (comp (remove str/blank?)
                      (filter keep?)
                      (keep (fn [path]
                              (when-let [canonical (mcp-paths/real-path-within
                                                     root path)]
                                [path (str canonical)])))
                      (take limit))
                (nul-separated-paths (:out result)))))
      (catch Exception _e nil))))

;; @spec MCP-OP-STUDY-021
(defn- source-dir-files
  "One walk per source DIRECTORY per scan, memoized in `cache`.

   500 sibling `deps.edn` files each declaring `:paths [\"..\"]` all resolve
   to the same directory. Walking it once per project cost 500 walks of the
   same 500 files — 8.4 s — and reported `file_count` 250,500.

   `limit` is constant for one scan (`cap + 1`), so the cache key does not
   carry it."
  ([cache root dir limit]
   (source-dir-files cache root dir limit (constantly true)))
  ([cache root dir limit keep?]
   (let [key (str dir)]
     (if (contains? @cache key)
       (get @cache key)
       (let [found (vec (walk-clj-files root dir limit keep?))]
         (swap! cache assoc key found)
         found)))))

;; @spec MCP-OP-STUDY-014
(defn- lexical-root-of
  "The scan root as the CALLER spelled it, absolutized and lexically
   normalized but NOT link-resolved — the frame a `:paths` entry discovered
   under a symlinked root must be measured in."
  ^java.nio.file.Path [dir]
  (try (.normalize (.toAbsolutePath ^java.nio.file.Path (fs/path (str dir))))
       (catch Throwable _ nil)))

(defn- within?
  [^java.nio.file.Path candidate ^java.nio.file.Path base]
  (boolean (and candidate base (.startsWith candidate base))))

;; @spec MCP-OP-STUDY-014
;; @spec MCP-OP-SHELL-ARGV-006
(defn- confined-source-dir
  "One declared source path, resolved under its project root and fenced — or
   nil when it escapes.

   TWO FRAMES OF REFERENCE, each compared like with like, and that is the whole
   correctness argument. The scan root can be a SYMLINK: `find` is started with
   `-H` so the link itself is descended, and the project roots discovery hands
   back are therefore spelled UNDER THE LINK, while `root` here is the
   link-RESOLVED canonical root. A single lexical `startsWith` against the
   resolved root therefore measured `link/src` against `target/` and refused
   every ordinary `{:paths [\"src\"]}` under a symlinked root — the whole scan
   came back `No Clojure files found`, which is a completeness claim about a
   tree that was never read. Field evidence: the trunk's
   `the-fence-does-not-refuse-an-ordinary-path-under-a-symlinked-root`, driven
   at both real launchers, arrived with the round-twelve merge and went red
   against this branch's fence.

   So the LEXICAL half compares the normalised entry against the UNRESOLVED
   scan root, and the LINK half re-resolves the entry and compares its real
   path against the CANONICAL root. An entry is confined when either frame
   places it inside, and when its real path — where it has one — is inside the
   canonical root. `..` and an absolute entry fail both frames; a symlinked
   `:paths` entry pointing out of the tree fails the second."
  [^java.nio.file.Path root ^java.nio.file.Path lexical-root
   ^java.nio.file.Path project-root src-path]
  (try
    (let [resolved (.normalize (.resolve project-root (str src-path)))
          real (try (.toRealPath resolved
                                 (make-array java.nio.file.LinkOption 0))
                    (catch Throwable _ nil))]
      (when (and (or (within? resolved root)
                     (within? resolved lexical-root))
                 (or (nil? real) (within? real root)))
        resolved))
    (catch Exception _ nil)))

(defn- confined-source-dirs
  "Resolve a build file's declared source paths under its project root,
   keeping only those that stay inside the canonical scan root — and RECORDING
   each one it drops.

   `(fs/path project-root \"../../..\")` is NOT normalized by `fs/path`, so an
   unnormalized escape used to be handed straight to `find` and moved the
   whole scan outside the root.

   @spec MCP-OP-SHELL-ARGV-006 — every entry is UNTRUSTED CALLER DATA, and a
   dropped entry is now a TYPED, COUNTED refusal instead of a silent `keep`.
   Two checks, in this order:

   - a STRING check, because nothing performed one. A non-string entry —
     `{:paths [[\"src\"] 42]}` — is refused here and never reaches a path
     constructor at all.

   - the RESOLVE-then-FENCE check that was already here, applied to the REAL
     path and never to the spelling, because `..`, an absolute entry and a
     symlink are three spellings of one escape.

   The refusal names the entry AS THE CALLER SPELLED IT and never the tree it
   resolved to: the target is a fact about the box, the spelling is the fact
   about the request, and a refusal that publishes the target hands over the
   very path it just declined to read. It is recorded rather than swallowed
   because a build file naming one escaping path and one legitimate one must
   not be reported as complete — a completeness claim over a walk that was
   fenced is the defect this fence exists to stop, wearing a green receipt.

   It travels on the SAME channel MCP-OP-STUDY-035 already opened for a
   `:paths` entry that is itself a symlink (`:paths-unresolved`), with its own
   `:reason`, rather than on a second one: two refusal channels for one
   question is how two channels come to disagree."
  [refused! project-name root lexical-root project-root src-paths]
  (keep (fn [src-path]
          (if-not (string? src-path)
            (do (swap! refused! conj {:path (pr-str src-path)
                                      :reason :not-a-string
                                      :project project-name})
                nil)
            (if-let [resolved (confined-source-dir root lexical-root
                                                   project-root src-path)]
              [(str src-path) resolved]
              (do (swap! refused! conj {:path (str src-path)
                                        :reason :outside-project-root
                                        :project project-name})
                  nil))))
        src-paths))

;; @spec MCP-OP-STUDY-035
(defn- unresolved-source-dir
  "The typed reason a declared source directory cannot be walked, or nil.

   `find` runs with the default `-P`, which is what keeps a symlinked project
   root from being descended (MCP-OP-STUDY-014) — and what makes a `:paths`
   entry that IS a symlink yield nothing: `find` prints the link by its own
   name and stops. The scan then reported `no-clojure-files` and offered
   \"scan a directory that contains Clojure sources\", which is exactly wrong
   for a directory that does contain them. A confinement decision the caller
   cannot see is a silent false negative, so it becomes a named skip."
  [declared resolved]
  (when (fs/sym-link? resolved)
    {:path declared :reason :symlink}))

;; @spec MCP-OP-STUDY-035
(defn- walkable-source-dirs
  "Split a project's confined source directories into the ones a walk can
   reach and the ones it must skip by name. `skipped!` records each skip
   against the project that declared it."
  [skipped! project-name confined]
  (vec (for [[declared resolved] confined
             :let [unresolved (unresolved-source-dir declared resolved)]]
         (do (when unresolved
               (swap! skipped! conj (assoc unresolved :project project-name)))
             (when-not unresolved resolved)))))

(def ^:private empty-accumulation
  {:seen #{} :projects []})

;; @spec MCP-OP-STUDY-021
;; @spec MCP-OP-STUDY-033
(defn- accumulate-projects
  "Fold project candidates into a DEDUPLICATED, capped file set.

   `candidates` is a seq of THUNKS, each returning
   `{:name :root :files [[walk-path canonical-path] ...] :truncated? bool}`.
   They are thunks so that `cap` stops the WALK and not merely the count: once
   the distinct file count passes the cap, no later candidate is ever listed.
   The cap used to be compared against a count that `discover-projects` had
   already materialised in full, which is the opposite of a bound on work.

   `:truncated?` says the candidate's own walk stopped at `cap + 1` rather
   than running out of files, which is what the cap stopping INSIDE a
   candidate looks like from here. A walk cut short always pushes the distinct
   count past the cap — at most `|seen|` of its `cap + 1` entries can be
   duplicates, and `|seen| <= cap` on entry — so it can only ever produce a
   refusal, never a silently short listing. The count that refusal carries is
   a FLOOR, and `:halted-early` is what makes the receipt say so.

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
      (let [{:keys [name root files truncated?]} ((first remaining))
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
           :halted-early (boolean (or truncated? (next remaining)))}
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
  [cache root lexical-root by-root limit skipped!]
  (map (fn [[project-root files]]
         (fn []
           (let [build-file (first files)
                 src-paths (extract-source-paths build-file)
                 root-path (fs/path project-root)
                 project-name (str (fs/file-name root-path))
                 walks (mapv #(source-dir-files cache root % limit)
                             (remove nil?
                                     (walkable-source-dirs
                                       skipped! project-name
                                       (confined-source-dirs
                                         skipped! project-name
                                         root lexical-root
                                         root-path src-paths))))]
             {:name project-name
              :root (str root-path)
              :files (vec (apply concat walks))
              :truncated? (boolean (some #(>= (count %) limit) walks))})))
       (sort-by (fn [[project-root _]] [(- (count project-root)) project-root])
                by-root)))

;; @spec MCP-OP-STUDY-021
;; @spec MCP-OP-STUDY-033
(defn- discover-projects
  "Find projects under dir via build files, deduplicated and capped as it goes.

   Returns `{:ok true :projects [{:name :root :files}] :file-count n}` or
   `{:ok false :file-count n}` when discovery passed `cap`.
   Falls back to a recursive scan if no build files are found.

   `root` is the canonical scan root: nothing outside it is discovered."
  [root dir cap]
  (let [dir (fs/path dir)
        lexical-root (lexical-root-of dir)
        cache (atom {})
        skipped! (atom [])
        ;; One file past the cap is all any walk needs to produce: it is
        ;; already a refusal, and stopping there is what keeps the
        ;; toRealPath canonicalisation syscalls proportional to the cap
        ;; instead of to the tree. That bound does NOT extend to the walk
        ;; itself: find still enumerates the whole tree and `:out :string`
        ;; materialises its entire stdout into one JVM string before this
        ;; transducer ever runs (measured: 1,130,000 bytes at max_files 10
        ;; over a 10,000-file corpus, wall time still tracking tree size).
        ;; Fine at today's ceilings; the real bound on a huge tree.
        limit (inc cap)
        build-files (find-build-files root dir)
        ;; Group by project root, keep first build file per root
        by-root (group-by #(str (fs/parent %)) build-files)]
    (assoc
      (by-name
        (if (seq by-root)
          (accumulate-projects cap
                               (build-project-candidates
                                 cache root lexical-root by-root limit skipped!)
                               empty-accumulation)
        ;; No build files — fallback to recursive scan. The skip-directory
        ;; test goes INTO the walk, so it can never shrink a listing below the
        ;; limit the walk stopped at.
          (accumulate-projects
            cap
            [(fn []
               (let [walk (source-dir-files cache root dir limit
                                            #(not (in-skip-dir? % dir)))]
                 {:name (str (fs/file-name dir))
                  :root (str dir)
                  :files walk
                  :truncated? (>= (count walk) limit)}))]
            empty-accumulation)))
      :paths-unresolved @skipped!)))

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
                   ["rg" "-li" "--null"
                    "-g" "*.clj" "-g" "*.cljs" "-g" "*.cljc"
                    "-g" "deps.edn" "-g" "project.clj" "-g" "bb.edn"
                    "--" pattern (str dir)]
                   ;; fallback: system grep
                   (let [exclude-args (mapcat #(vector "--exclude-dir" %)
                                              [".git" ".cpcache" ".gitlibs" "target"
                                               "node_modules" ".clj-kondo" ".lsp" ".shadow-cljs"])]
                     (concat ["grep" "-rliEZ"
                              "--include=*.clj" "--include=*.cljs" "--include=*.cljc"
                              "--include=deps.edn" "--include=project.clj" "--include=bb.edn"]
                             exclude-args
                             ["--" pattern (str dir)])))
            result (apply babashka.process/shell
                          {:out :string :err :string :continue true}
                          args)]
        (if (zero? (:exit result))
          ;; @spec MCP-OP-SHELL-ARGV-003 — NUL-delimited: a matching path may
          ;; contain a newline, and line-splitting invented two paths from one.
          (set (nul-separated-paths (:out result)))
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

;; @spec MCP-OP-STUDY-031
(def ns-grep-match-steps-floor
  "The MINIMUM per-file character-read allowance for one ns-grep pass,
   whatever the paths involved. `java.util.regex` backtracks, so a
   caller-supplied pattern is caller-supplied CPU; this floor is what a
   short-path tree (this repository's own `src/`, ordinary 36-character
   paths) gets. `(.*.*.*.*.*.*)*x` against paths that short still costs
   335,730,084 character reads for one match — orders of magnitude over this
   floor — while honest patterns read tens to low thousands of characters.
   See `ns-grep-match-steps-per-file` for how a longer tree is charged."
  20000)

;; @spec MCP-OP-STUDY-031
(def ^:private ns-grep-match-steps-length-factor
  "Quadratic coefficient applied to the longest relative path length in a
   pass, in `ns-grep-match-steps-per-file`. See that function's docstring
   for the measured basis."
  64)

;; @spec MCP-OP-STUDY-031
(defn ns-grep-match-steps-per-file
  "How many characters the regular-expression engine may READ, per file,
   while `ns-grep` filters one scan whose longest relative path is
   `longest-len` characters.

   A flat per-file allowance miscalibrates: `.*a.*b.*`-shaped patterns cost
   quadratically in subject length, so a constant sized for this
   repository's own ~36-character paths refused a completely honest
   `.*handler.*internal.*` over an ordinary monorepo path. Measured basis, not
   a guess: at length 106 (an ordinary monorepo-shaped relative path, no
   adversarial repetition), that honest pattern reads 33,566 characters
   testing one non-matching file; `64 x 106^2` = 719,104, a margin of about
   21.4x. The floor keeps short trees exactly as generous as before; the
   quadratic term keeps long ones honest without opening the door any wider
   than the length actually in front of it."
  ^long [longest-len]
  (max ns-grep-match-steps-floor
       (* ns-grep-match-steps-length-factor
          (long longest-len) (long longest-len))))

;; @spec MCP-OP-STUDY-031
(defn ns-grep-scan-budget
  "Pure: the total step allowance for one ns-grep pass over `projects`,
   relative to `dir`. `ns-grep-match-steps-per-file`, evaluated at the
   longest relative path the pass will test, times the number of files
   discovery found — so the allowance and the work both still scale
   linearly with the tree (adding files can never turn a passing call into
   a failing one; a rendering path-length outlier can only ever raise the
   allowance, never lower it)."
  [projects dir]
  (let [dir-path (fs/path dir)
        rel (fn [f] (str (fs/relativize dir-path (fs/path f))))
        files (mapcat :files projects)
        file-count (count files)
        longest (reduce max 0 (map (comp count rel) files))]
    (* (ns-grep-match-steps-per-file longest) (max 1 file-count))))

;; @spec MCP-OP-STUDY-031
(defn ns-grep-pool
  "A mutable step allowance shared by every match in one ns-grep pass:
   `[characters-left budget]`. Pooling it across the pass rather than per
   match is what bounds the SCAN — a per-match budget still lets a pattern
   that costs just under it be paid once per file."
  ^longs [budget]
  (long-array [(long budget) (long budget)]))

;; @spec MCP-OP-STUDY-031
(defn- budgeted-subject
  "The match subject, wrapped so the regex engine's own character reads are
   counted against `pool` and can run out.

   A wall-clock deadline cannot do this job: `future-cancel` does not
   interrupt a running matcher and `Matcher` polls no interrupt flag, so the
   only place a backtracking match can be stopped from is inside the
   `CharSequence` it reads. Exactly `budget` reads succeed; the next one
   throws — TOTALLY: `subSequence` returns another counted view sharing the
   same pool rather than the raw, uncounted `String`, and `toString` charges
   the whole length it hands back. `java.util.regex` reads a subject only
   through `charAt` (verified across 15 constructs, lookbehind and
   backreferences included), so neither is on today's match path — but a
   future engine path, or `Matcher.group()`'s own `subSequence` call during
   capture extraction, must not be a silent bypass of the counter."
  ^CharSequence [^String s ^longs pool]
  (letfn [(charge! [^long n]
            (let [left (unchecked-subtract (aget pool 0) n)]
              (aset pool 0 left)
              (when (neg? left)
                (throw (ex-info "ns-grep match budget exhausted"
                                {:error-type :ns-grep-match-budget-exceeded
                                 :budget (aget pool 1)})))))]
    (reify CharSequence
      (length [_] (.length s))
      (charAt [_ index]
        (charge! 1)
        (.charAt s index))
      (subSequence [_ start end]
        (budgeted-subject (.substring s start end) pool))
      (toString [_]
        (charge! (.length s))
        s))))

;; @spec MCP-OP-STUDY-012
;; @spec MCP-OP-STUDY-031
(defn ns-grep-hit?
  "Pure: true if a source file's SCAN-RELATIVE path plausibly names a
   namespace matching the ALREADY COMPILED pattern. Tests the relative path as
   given and again with '_' turned into '-', because the Clojure require
   convention keeps a file's path in lockstep with its declared namespace
   (path segment <-> ns segment, '_' <-> '-'). Takes the path already
   relativized to the scanned dir — never the absolute filesystem path, whose
   ancestor directories (e.g. a checkout named `...-store` or `...-study`) could
   spuriously match. Never opens or parses the file — this is a path/namespace
   filter, never a file-contents filter (that is `grep`, via
   `filter-projects-by-hits`).

   `pool` is the shared step allowance from `ns-grep-pool`. Every character
   the engine reads is charged to it, and an exhausted pool THROWS a typed
   `:ns-grep-match-budget-exceeded` `ex-info` that `ls-tree` turns into a
   typed refusal. The pattern was compiled under a guard and then matched
   without one, which left the read entrance one call away from hours of
   uninterruptible CPU."
  [re rel-path pool]
  (boolean (or (re-find re (budgeted-subject rel-path pool))
               (re-find re (budgeted-subject (str/replace rel-path "_" "-")
                                             pool)))))

;; @spec MCP-OP-STUDY-012
;; @spec MCP-OP-STUDY-022
;; @spec MCP-OP-STUDY-031
(defn filter-projects-by-ns-grep
  "Pure: keep only files whose path/namespace — relative to dir, never the
   absolute filesystem path — matches pattern. Narrower than
   `filter-projects-by-hits`/`grep`, which searches file bodies and matches
   any line containing the pattern — comments, strings, and unrelated
   substrings included. Drops projects left with no files.

   The pattern is compiled ONCE here; an uncompilable one is refused by
   `ls-tree` long before this. The whole pass shares ONE step budget, sized by
   `ns-grep-scan-budget` from the number of discovered files and the longest
   relative path among them, so the work this filter can do is bounded by the
   tree it was handed rather than by the pattern it was given."
  [projects dir pattern]
  (let [dir-path (fs/path dir)
        re (compile-pattern pattern)
        rel (fn [f] (str (fs/relativize dir-path (fs/path f))))
        pool (ns-grep-pool (ns-grep-scan-budget projects dir))]
    (if-not re
      []
      (->> projects
           (map (fn [project]
                  (update project :files
                          (fn [files]
                            (filterv #(ns-grep-hit? re (rel %) pool) files)))))
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

;; @spec MCP-OP-MEM-005
(defn- safe-outline
  "Run outline on a file, returning error map on parse errors.

   A parser-admission refusal (MCP-OP-MEM-005) is kept TYPED rather than
   flattened to a message: the entry carries `:refusal`, `:reason`, `:limit`
   and `:observed` so the scan's receipt can name and count it. It stays a
   per-file skip — before this, a file deep enough to exhaust the reader's
   stack threw a StackOverflowError, which is an `Error` and not an
   `Exception`, and killed the whole scan."
  [file]
  (try
    (outline/outline file)
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
        (if (= :parser_admission_refused (:refusal data))
          (assoc (select-keys data [:refusal :reason :limit :observed :remedy])
                 :file file
                 :error (ex-message e))
          {:file file :error (str (ex-message e))})))
    (catch StackOverflowError _
      ;; @spec MCP-OP-MEM-005
      ;; The estimator is an ESTIMATE, and this catch is what makes the
      ;; scan-kill class closed WITHOUT depending on it being complete. An
      ;; Error is not an Exception, so before this one overflowing file killed
      ;; the whole scan and no file's outline came back at all.
      (let [r (admission/stack-overflow-refusal file)]
        (assoc (select-keys r [:refusal :reason :limit :observed :remedy])
               :file file
               :error (str "parser admission refused "
                           (admission/public-ceiling-name (:reason r))
                           ": " file))))
    (catch Exception e
      {:file file :error (str (.getMessage e))})))

;; @spec MCP-OP-MEM-005
(defn- admission-refusals
  "Every parser-admission refusal in a scan, as receipt rows in path order."
  [projects dir]
  (vec
    (for [{:keys [outlines]} projects
          [f result] outlines
          :when (= :parser_admission_refused (:refusal result))]
      {:file (str (fs/relativize (fs/path dir) (fs/path f)))
       :reason (:reason result)
       :limit (:limit result)
       :observed (:observed result)
       :remedy (:remedy result)})))

;; @spec MCP-OP-MEM-005
(defn- scan-resources
  "The scan's own cost, carried as metadata on the projects vector
   `outline-take` returned. Zeroed when a caller assembled the projects some
   other way."
  [projects]
  (or (::scan-resources (meta projects))
      (admission/meter-resources nil)))

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
;; @spec MCP-OP-STUDY-028
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
   (let [meter (admission/new-meter)
         wanted (vec (take n (files-in-scan-order projects)))
         missing (->> (map second wanted)
                      distinct
                      (remove #(contains? @cache %))
                      vec)]
     ;; @spec MCP-OP-MEM-005 — charge the admission scan against THIS scan
     ;; only. The meter is made here and closed over lexically, then rebound
     ;; inside each worker: two concurrent scans each charge their own, and
     ;; the count does not depend on binding conveyance surviving a change of
     ;; executor (`map-fn` may be a claypoole pool).
     (when (seq missing)
       (swap! cache into
              (map-fn (fn [file]
                        (binding [admission/*scan-meter* meter]
                          [file (safe-outline file)]))
                      missing)))
     (let [outlines @cache
           by-project (group-by first wanted)]
       ;; Projects with no outlines are KEPT, carrying their `:files`. A
       ;; project the receipt's byte budget did not reach is still a project
       ;; the scan found: dropping it here made it vanish from the body while
       ;; `project_count` still counted it.
       (with-meta
         (->> (map-indexed vector projects)
              (mapv (fn [[index project]]
                      (assoc project :outlines
                             (mapv (fn [[_ file]] [file (get outlines file)])
                                   (get by-project index []))))))
         ;; @spec MCP-OP-MEM-005
         {::scan-resources (admission/meter-resources meter)})))))

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
  ([projects dir {:keys [file-count paths-unresolved]}]
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
     ;; @spec MCP-OP-MEM-005
     ;; A refused file is a named, counted skip — never a silent one and never
     ;; a dead scan. Nothing is appended when nothing was refused, so an
     ;; ordinary scan's output is byte-identical to before this control
     ;; existed, and the scan's own cost is charged INSIDE the refusal block
     ;; for the same reason; the EDN receipt carries it unconditionally.
     (let [refused (admission-refusals projects dir)]
       (when (seq refused)
         (.append sb (format "── parser_admission_refused: %d file(s)\n"
                             (count refused)))
         (doseq [{:keys [file reason limit observed]} refused]
           ;; A stack-overflow skip measured nothing, so it names no limit.
           (.append sb (if (and limit observed)
                         (format "   %s  %s limit %d, observed %d\n"
                                 file
                                 (admission/public-ceiling-name reason)
                                 limit observed)
                         (format "   %s  %s\n"
                                 file
                                 (admission/public-ceiling-name reason)))))
         (let [{:keys [scan_ms bytes_scanned]} (scan-resources projects)]
           (.append sb (format "── resources: scan_ms %s, bytes_scanned %s\n"
                               scan_ms bytes_scanned)))))
     ;; @spec MCP-OP-SHELL-ARGV-006
     ;; @spec MCP-OP-STUDY-035
     ;; Named and counted, on the same terms as the admission block above and
     ;; for the same reason. The ENTRY is printed as the caller SPELLED it and
     ;; the tree it resolved to is not printed at all: publishing the target
     ;; would hand over the very path this fence just declined to read. Nothing
     ;; is appended when nothing was refused, so an ordinary scan's output is
     ;; byte-identical to before this control existed.
     (when (seq paths-unresolved)
       (.append sb (format "── source_paths_unresolved: %d entr%s\n"
                           (count paths-unresolved)
                           (if (= 1 (count paths-unresolved)) "y" "ies")))
       (doseq [{:keys [project path reason]} paths-unresolved]
         (.append sb (format "   %s  %s  refused: %s\n"
                             project path (name reason)))))
     (str sb))))

(defn format-ls-tree-edn-entries
  "Pure: one entry per outlined file and nothing else.

   The MCP `ls-tree` payload is a FILE LIST bounded by a byte budget, so it
   takes the entries alone: a trailing receipt map inside the payload would be
   read as another file, and `scan_ms` is a measured wall-clock reading whose
   width varies run to run, which the entrance's determinism witnesses forbid
   in a bounded payload. The MCP receipt carries its own resource accounting."
  [projects dir]
  (vec
    (for [{:keys [outlines]} projects
          [f result] outlines
          :let [rel-path (str (fs/relativize (fs/path dir) (fs/path f)))]]
      (-> result
          (assoc :file rel-path)
          (dissoc :forward-refs)))))

;; @spec MCP-OP-MEM-005
(defn format-ls-tree-edn
  "Pure: format ls-tree results as the CLI's EDN vector.

   ONE trailing receipt map is always appended. It carries `:resources`
   unconditionally — the scan's own cost with its `bytes_scanned` denominator,
   because a scan regression appears on ORDINARY scans and a meter wired to
   the rare refusal branch is one nobody ever sees move — and it names and
   counts `:parser_admission_refused` only when something actually was
   refused. The human TEXT rendering keeps the older, quieter contract."
  ([projects dir] (format-ls-tree-edn projects dir nil))
  ([projects dir {:keys [paths-unresolved]}]
   (let [entries (format-ls-tree-edn-entries projects dir)
         refused (admission-refusals projects dir)
         receipt (cond-> {:resources (scan-resources projects)}
                   (seq refused)
                   (assoc :parser_admission_refused
                          {:count (count refused) :files refused})
                   ;; @spec MCP-OP-SHELL-ARGV-006
                   ;; Named and counted here too, and carrying only the
                   ;; SPELLING. Conditional for the same reason the admission
                   ;; key is: an ordinary scan's receipt is unchanged.
                   (seq paths-unresolved)
                   (assoc :source_paths_unresolved
                          {:count (count paths-unresolved)
                           :entries (vec paths-unresolved)}))]
     (conj entries {:receipt receipt}))))

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
        skipped! (atom [])
        limit (inc cap)
        build-files #{"deps.edn" "project.clj" "bb.edn"}
        {build-hits true src-hits false}
        (group-by #(contains? build-files (str (fs/file-name %))) grep-hits)

        ;; Projects with matching build files → find all their source files
        built (accumulate-projects
                cap
                (map (fn [build-file]
                       (fn []
                         (let [project-root (str (fs/parent (fs/path build-file)))
                               project-name (str (fs/file-name
                                                   (fs/path project-root)))
                               walks (mapv
                                       #(source-dir-files cache root % limit)
                                       (remove
                                         nil?
                                         (walkable-source-dirs
                                           skipped! project-name
                                           ;; @spec MCP-OP-SHELL-ARGV-006
                                           ;; The grep fast path reads the same
                                           ;; build files through the same
                                           ;; extractor, so it inherits the same
                                           ;; defect and needs the same fence
                                           ;; AND the same refusal channel. A
                                           ;; fence reported at one of two call
                                           ;; sites is not reported, it is that
                                           ;; call site's habit — and `:grep` is
                                           ;; one argument away for the caller
                                           ;; who just planted the file.
                                           (confined-source-dirs
                                             skipped! project-name
                                             root (lexical-root-of dir)
                                             (fs/path project-root)
                                             (extract-source-paths build-file)))))]
                           {:name project-name
                            :root project-root
                            :files (vec (apply concat walks))
                            :truncated? (boolean
                                          (some #(>= (count %) limit) walks))})))
                     (sort-by #(vector (- (count (str %))) (str %))
                              (or build-hits [])))
                empty-accumulation)]
    (if-not (:ok built)
      (assoc built :paths-unresolved @skipped!)
      (let [build-roots (set (map :root (:projects built)))
            ;; Source file hits not in a build-matched project → group by
            ;; nearest project root
            orphan-src-hits (remove #(some (fn [r]
                                             (str/starts-with? (str %) (str r "/")))
                                           build-roots)
                                    (or src-hits []))]
        (assoc
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
                          ;; Grep hits are canonicalised here, so the same
                          ;; cap-plus-one bound applies: a pattern matching a
                          ;; whole tree must not cost one toRealPath per hit.
                          (let [files (into []
                                            (comp
                                              (keep
                                                (fn [{:keys [file]}]
                                                  (when-let
                                                    [canonical
                                                     (mcp-paths/real-path-within
                                                       root file)]
                                                    [file (str canonical)])))
                                              (take limit))
                                            entries)]
                            {:name (str (fs/file-name (fs/path project-root)))
                             :root project-root
                             :files files
                             :truncated? (>= (count files) limit)})))))
              (select-keys built [:seen :projects])))
          :paths-unresolved @skipped!)))))


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
;; @spec MCP-OP-STUDY-026
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

   `dir-label` is what the CALLER named — the scan-relative directory, or the
   project-relative `dir` an MCP request supplied. Refusal messages use it:
   `dir` is rebound below to the canonical realpath, which is the confinement
   boundary and the basis for relativizing file paths, but it is also a
   host-absolute path that means nothing to the caller and leaks where the
   workspace lives (MCP-OP-STUDY-006 forbids publishing one).

   Printing, exit codes, and receipts belong to the entrances, not here."
  [{:keys [dir dir-label grep ns-grep max-files] :as _opts}]
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
          named (str (or dir-label dir))
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
          ;; The ns-grep pass runs under a shared step budget, so an
          ;; exhausted pool arrives here as a typed value rather than as a
          ;; regex engine spinning for hours where nothing can cancel it.
          filtered (when (and (:ok discovery) ns-grep)
                     (try
                       {:ok true
                        :projects (filter-projects-by-ns-grep
                                    (:projects discovery) dir ns-grep)}
                       (catch clojure.lang.ExceptionInfo error
                         (if (= :ns-grep-match-budget-exceeded
                                (:error-type (ex-data error)))
                           {:ok false :budget (:budget (ex-data error))}
                           (throw error)))))
          projects (if filtered (:projects filtered) (:projects discovery))
          file-count (total-file-count projects)
          ;; Declared source directories a `-P` walk cannot reach. Reported on
          ;; success AND on refusal: a scan that found nothing because every
          ;; declared path was a symlink must not be told to find a directory
          ;; with Clojure sources in it.
          unresolved (vec (:paths-unresolved discovery))]
      (cond
        (and filtered (not (:ok filtered)))
        {:ok false
         :error-type :ns-grep-match-budget-exceeded
         :dir dir
         :grep grep
         :ns-grep ns-grep
         :match-budget (:budget filtered)
         :error (format (str "Matching :ns-grep %s under %s exhausted the "
                             "%d-character match budget for this scan")
                        (pr-str ns-grep) named (:budget filtered))
         :remedy (str "This ns_grep pattern's cost grows with path length: "
                      "several unbounded .* in sequence, or nested "
                      "quantifiers, are the usual cause. Anchor the "
                      "pattern, or use a literal path segment or a simple "
                      "alternation instead.")}

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
         ;; A count taken where the walk stopped is a FLOOR, and the receipt
         ;; carries that as data, not only as two words inside a sentence.
         :observed-at-least (boolean (:halted-early discovery))
         :error (format "Discovery found %s%d Clojure files under %s, above the %d-file scan cap"
                        (if (:halted-early discovery) "at least " "")
                        (:file-count discovery) named cap)
         :remedy (format (str "Scan a subdirectory, add a grep or ns_grep "
                              "pattern, or raise max_files (ceiling %d).")
                         max-scan-files-ceiling)}

        (empty? projects)
        (cond->
          {:ok false
           :error-type :no-clojure-files
           :dir dir
           :grep grep
           :ns-grep ns-grep
         ;; `(when grep ...)` here would render the literal string "null" into
         ;; the message; this branch was unreachable before the format shadow
         ;; was removed, so no caller depended on that spelling.
           :error (format "No Clojure files found under %s%s%s"
                          named
                          (if grep (str " matching '" grep "'") "")
                          (if ns-grep (str " with ns/path matching '" ns-grep "'") ""))}
          (seq unresolved)
          (assoc :paths-unresolved unresolved
                 :remedy (format (str "Declared source %s %s could not be "
                                      "walked (%s): scan the directory the "
                                      "link resolves to, or declare its real "
                                      "path in :paths.")
                                 (if (= 1 (count unresolved)) "path" "paths")
                                 (str/join ", " (map (comp pr-str :path)
                                                     unresolved))
                                 (str/join ", " (distinct
                                                  (map (comp name :reason)
                                                       unresolved))))))

        :else
        (cond-> {:ok true
                 :dir dir
                 :grep grep
                 :ns-grep ns-grep
                 :file-count file-count
                 :projects projects}
          (seq unresolved) (assoc :paths-unresolved unresolved))))))
