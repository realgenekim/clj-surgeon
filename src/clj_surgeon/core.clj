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
   [clj-surgeon.parse-admission :as admission]
   [clj-surgeon.relation-census :as relation-census]
   [clj-surgeon.rename :as rename]
   [clj-surgeon.show-form :as show-form]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.string :as str]))

(defn- named-plan-refusal
  "Run `f`; turn a parser-admission refusal into a NAMED refusal the caller can
   read, instead of a stack trace.

   Gating `clj-surgeon.analyze` (MCP-OP-MEM-005) swaps an uncatchable
   StackOverflowError for a typed ExceptionInfo. This is the minimal surface
   that makes that typed refusal usable at the planning ops, the way
   `safe-outline` does for the scan. Anything that is not an admission refusal
   is re-thrown untouched."
  ;; @spec MCP-OP-MEM-005
  [f]
  (try
    (f)
    (catch clojure.lang.ExceptionInfo e
      (let [data (ex-data e)]
        (if (= :parser_admission_refused (:refusal data))
          (assoc (select-keys data [:refusal :reason :limit :observed :remedy :file])
                 :error (ex-message e))
          (throw e))))))

(defn run-outline [{:keys [file]}]
  (named-plan-refusal
    (fn []
      (let [result (outline/outline file)
            ns-name (:ns result)
            forward-refs (when ns-name
                           (fwd/detect-forward-refs file ns-name))]
        (assoc result :forward-refs (or forward-refs []))))))

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
 (named-plan-refusal
  (fn []
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
                                      declares))}}))))

(defn run-deps [{:keys [file form]}]
  (named-plan-refusal
    (fn []
      (let [zloc (analyze/file->zloc file)
            deps (analyze/intra-ns-deps zloc)]
        (if form
          (first (filter #(= form (:name %)) deps))
          deps)))))

(defn run-topo [{:keys [file]}]
  (named-plan-refusal
    (fn []
      (let [zloc (analyze/file->zloc file)]
        (analyze/topological-sort zloc)))))

(defn run-closure [{:keys [file form]}]
  (named-plan-refusal
    (fn []
      (let [zloc (analyze/file->zloc file)]
        (analyze/extraction-closure zloc form)))))

(defn run-ls-deps [{:keys [file form]}]
  (named-plan-refusal
    (fn []
      (let [zloc (analyze/file->zloc file)
            deps (analyze/intra-ns-deps zloc)]
        (analyze/dep-tree deps form)))))

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

(defn- existing-directory?
  "True only when `path` names an existing directory. Never throws: a string
   that is not a legal path (an embedded NUL, say) is simply not a directory."
  [path]
  (try (boolean (fs/directory? (str path)))
       (catch Exception _e false)))

(defn- find-start-token
  "Render a directory as a `find` start-point token. A RELATIVE path beginning
   with `-` would be parsed by find as an OPTION rather than a path, so prefix
   it with `./`. Absolute paths are already unambiguous."
  [path]
  (let [s (str path)]
    (if (str/starts-with? s "-") (str "./" s) s)))

(defn- nul-separated-paths
  "Split NUL-delimited command output into paths, dropping the trailing empty
   token. Never trims: leading or trailing whitespace, and newlines, are legal
   inside a path and are data, not framing."
  [out]
  (->> (str/split (str out) #"\u0000")
       (remove #(= "" %))
       sort
       vec))

;; @spec MCP-OP-SHELL-ARGV-001
;; @spec MCP-OP-SHELL-ARGV-003
(defn- find-build-files
  "Find deps.edn, project.clj, bb.edn under dir, skipping hidden/cache dirs.
   Uses system find with -prune for speed (~10x faster than fs/glob on large trees).

   The command is an explicit ARGUMENT VECTOR — `dir` is exactly one token and
   never reaches a shell interpreter. It must stay that way: this function used
   to `format` dir into a string run through `sh -c`, so any :dir carrying `;`
   or `$(...)` executed arbitrary commands (Andon pull inb-d27b79, 2026-09-03).
   Output is NUL-delimited so a path containing a newline survives intact.

   A dir that is not an existing directory yields [] and logs the reason; the
   typed refusal for that case belongs to the entrance (ls-tree-root-refusal),
   because this helper's contract is a vector of paths."
  [dir]
  (if-not (existing-directory? dir)
    (do (binding [*out* *err*]
          (println (str "clj-surgeon: skipping project discovery; "
                        "not an existing directory: " (pr-str (str dir)))))
        [])
    (try
      (let [prune-tokens (concat ["("]
                                 (->> (sort skip-dirs)
                                      (map (fn [d] ["-name" d]))
                                      (interpose ["-o"])
                                      (apply concat))
                                 [")" "-prune"])
            ;; -H: follow a symlink given as the START POINT only.
            ;; `existing-directory?` uses Files.isDirectory, which follows
            ;; links, so a symlinked root PASSES the entrance gate; find's -P
            ;; default would then refuse to descend it and discovery would
            ;; return nothing for a root the gate accepted. -H makes the gate
            ;; and the executor answer the same question. It does NOT follow
            ;; links found inside the tree, so the walk stays acyclic.
            args (concat ["find" "-H" (find-start-token dir)]
                         prune-tokens
                         ["-o" "("
                          "-name" "deps.edn"
                          "-o" "-name" "project.clj"
                          "-o" "-name" "bb.edn"
                          ")" "-print0"])
            result (apply babashka.process/shell
                          {:out :string :err :string :continue true}
                          args)]
        (if (zero? (:exit result))
          (nul-separated-paths (:out result))
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

(defn- extract-source-paths
  "I/O wrapper: read a build file AS DATA and return its source paths.

   `edn/read-string`, NOT `clojure.core/read-string`, and the difference is a
   class rather than a nicety — the same class round twenty closed one frame
   over at `core/parse-val`, and Opus's round-twenty-one BLOCKING finding
   is that it survived HERE, in the entrance the round-twenty enumeration did
   not walk.

   `clojure.core/read-string` honours `*read-eval*`, which defaults to true,
   so the reader EVALUATES `#=(…)` in the file it is reading. The file is a
   `deps.edn` / `bb.edn` / `project.clj` DISCOVERED UNDER THE DIRECTORY THE
   CALLER NAMED, so the caller does not even need to control argv text:
   controlling a directory is enough. Demonstrated at both real launchers at
   0a91e720 under the ordinary invocation:

     $ cat $FX/evil-tree/deps.edn
     {:paths #=(clojure.core/spit \"$FX/PWNED-LSTREE.txt\" \"…\")}
     $ clj-surgeon :op :ls-tree :dir $FX/evil-tree
     EXIT=0
     src/a.clj  1 lines, 0 forms
     $ cat $FX/PWNED-LSTREE.txt
     READER EVAL EXECUTED via :op :ls-tree :dir

   Exit 0, a green receipt, nothing printed. The `catch` below does not help
   and never did: the evaluation happens INSIDE the reader, before any value
   is returned, so the catch swallows the evidence rather than the effect.

   A build file is data this op looks ONE key up in. `edn/read-string` reads
   every shape `source-paths-from-config` was written for — the `deps.edn` and
   `bb.edn` maps, and the `project.clj` list whose `:source-paths` is looked up
   positionally — and refuses `#=`, arbitrary tagged literals and every other
   reader escape. A `project.clj` that genuinely needs code reading (an
   unquote, say) now throws and falls back to [\"src\"], which is the same
   answer this fn already gave for an unreadable build file; that is a refusal
   to guess, not a regression, and it is the argument for refusing such a file
   rather than for `*read-eval*`."
  [build-file]
  (try
    (source-paths-from-config (str (fs/file-name build-file))
                              (edn/read-string (slurp (str build-file))))
    (catch Exception _e ["src"])))

;; @spec MCP-OP-SHELL-ARGV-003
(defn- find-clj-files
  "Find all .clj/.cljs/.cljc files under a directory using system find.

   NUL-delimited: a source path may contain a newline, and str/split-lines
   turned one real path into two fictional ones that then failed to parse and
   were silently dropped (Andon pull inb-d27b79, 2026-09-03). The -name
   alternation is parenthesised because -print0 would otherwise bind to the
   last -name only."
  [dir]
  (when (existing-directory? dir)
    (try
      (let [result (babashka.process/shell
                     {:out :string :err :string :continue true}
                     ;; -H for the same reason as find-build-files: a source
                     ;; path may itself be reached through a symlinked root.
                     "find" "-H" (find-start-token dir)
                     "(" "-name" "*.clj"
                     "-o" "-name" "*.cljs"
                     "-o" "-name" "*.cljc" ")" "-print0")]
        (when (zero? (:exit result))
          (seq (nul-separated-paths (:out result)))))
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

;; @spec MCP-OP-SHELL-ARGV-003
(defn- grep-tree
  "Single recursive grep on a directory tree. Returns set of matching absolute paths.
   Uses ripgrep (rg) if available — faster and respects .gitignore.
   Falls back to system grep (MUCH slower on large trees).

   NUL-delimited (rg --null / grep -Z) for the same reason as find-clj-files: a
   matching path may contain a newline. The caller's pattern is passed after
   `-e` and the directory after `--`, so neither can be read as an option."
  [pattern dir]
  (when-not (rg-available?)
    (binding [*out* *err*]
      (println "WARNING: ripgrep (rg) not found. Falling back to grep (much slower).")
      (println "Install: brew install ripgrep  OR  apt install ripgrep")))
  (try
    (let [args (if (rg-available?)
                 ;; ripgrep: fast, respects .gitignore automatically
                 ;; Note: rg uses -i for case-insensitive (not -E which means encoding)
                 ["rg" "-li" "--null"
                  "-g" "*.clj" "-g" "*.cljs" "-g" "*.cljc"
                  "-g" "deps.edn" "-g" "project.clj" "-g" "bb.edn"
                  "-e" pattern "--" (str dir)]
                 ;; fallback: system grep
                 (let [exclude-args (mapcat #(vector "--exclude-dir" %)
                                            [".git" ".cpcache" ".gitlibs" "target"
                                             "node_modules" ".clj-kondo" ".lsp" ".shadow-cljs"])]
                   (concat ["grep" "-rliZE"
                            "--include=*.clj" "--include=*.cljs" "--include=*.cljc"
                            "--include=deps.edn" "--include=project.clj" "--include=bb.edn"]
                           exclude-args
                           ["-e" pattern "--" (str dir)])))
          result (apply babashka.process/shell
                        {:out :string :err :string :continue true}
                        args)]
      (if (zero? (:exit result))
        (set (nul-separated-paths (:out result)))
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
  "Run outline on a file, returning error map on parse errors.

   A parser-admission refusal (MCP-OP-MEM-005) is kept TYPED rather than
   flattened to a message: the entry carries `:refusal`, `:reason`, `:limit`
   and `:observed` so the scan's receipt can name and count it. It stays a
   per-file skip — before this, a file deep enough to exhaust the reader's
   stack threw a StackOverflowError, which is an `Error` and not an
   `Exception`, and killed the whole scan."
  ;; @spec MCP-OP-MEM-005
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
      ;; the whole pmap scan and no file's outline came back at all.
      (let [r (admission/stack-overflow-refusal file)]
        (assoc (select-keys r [:refusal :reason :limit :observed :remedy])
               :file file
               :error (str "parser admission refused "
                           (admission/public-ceiling-name (:reason r))
                           ": " file))))
    (catch Exception e
      {:file file :error (str (.getMessage e))})))

(defn- admission-refusals
  "Every parser-admission refusal in a scan, as receipt rows in path order."
  ;; @spec MCP-OP-MEM-005
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

(defn- outline-all-files
  "Compute outlines for all files across projects, in parallel.
   Returns projects with :outlines — a vec of [file outline] pairs."
  [projects]
  ;; @spec MCP-OP-MEM-005 — charge the admission scan against THIS scan only.
  ;; The meter is made here and closed over lexically, then rebound inside each
  ;; worker: two concurrent ls-tree calls each charge their own, and the count
  ;; does not depend on binding conveyance surviving a change of executor.
  (let [meter (admission/new-meter)
        ;; Collect all [project-idx file] pairs
        all-files (for [[pidx project] (map-indexed vector projects)
                        f (:files project)]
                    [pidx f])
        ;; Parse all files in parallel
        results (pmap (fn [[pidx f]]
                        (binding [admission/*scan-meter* meter]
                          [pidx f (safe-outline f)]))
                      all-files)
        ;; Group back by project index
        by-project (group-by first results)
        outlined (mapv (fn [[pidx project]]
                         (let [file-results (mapv (fn [[_ f outline]] [f outline])
                                                  (get by-project pidx []))]
                           (assoc project :outlines file-results)))
                       (map-indexed vector projects))]
    (with-meta outlined {::scan-resources (admission/meter-resources meter)})))

(defn- scan-resources
  "The scan's own cost, carried on the projects vector `outline-all-files`
   returned. Zeroed when a caller assembled the projects some other way."
  ;; @spec MCP-OP-MEM-005
  [projects]
  (or (::scan-resources (meta projects))
      (admission/meter-resources nil)))

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
    ;; @spec MCP-OP-MEM-005
    ;; A refused file is a named, counted skip — never a silent one and never a
    ;; dead scan. Nothing is appended when nothing was refused, so an ordinary
    ;; scan's output is byte-identical to before this control existed.
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
        ;; @spec MCP-OP-MEM-005 — charge the control's own cost with its
        ;; denominator. The TEXT rendering stays inside the refusal block, so an
        ;; ordinary scan's human output is byte-identical to before this control
        ;; existed; the EDN receipt carries it unconditionally, because that is
        ;; the surface a regression check reads.
        (let [{:keys [scan_ms bytes_scanned]} (scan-resources projects)]
          (.append sb (format "── resources: scan_ms %s, bytes_scanned %s\n"
                              scan_ms bytes_scanned)))))
    (str sb)))

(defn format-ls-tree-edn
  "Pure: format ls-tree results as EDN vector.
   Expects projects with :outlines already computed.

   ONE trailing receipt map is always appended. It carries `:resources`
   unconditionally — the scan's own cost with its `bytes_scanned` denominator,
   because a scan regression appears on ORDINARY scans and a meter wired to the
   rare refusal branch is one nobody ever sees move — and it names and counts
   `:parser_admission_refused` only when something actually was refused. The
   human TEXT rendering keeps the older, quieter contract: an ordinary scan's
   text is byte-identical to before this control existed."
  ;; @spec MCP-OP-MEM-005
  [projects dir]
  (let [entries (vec
                  (for [{:keys [outlines]} projects
                        [f result] outlines
                        :let [rel-path (str (fs/relativize (fs/path dir)
                                                           (fs/path f)))]]
                    (-> result
                        (assoc :file rel-path)
                        (dissoc :forward-refs))))
        refused (admission-refusals projects dir)
        ;; @spec MCP-OP-MEM-005
        ;; `:resources` is UNCONDITIONAL. The meter exists to catch a scan
        ;; regression — the first draft was 638x slower and every test passed —
        ;; and a regression shows up on ORDINARY scans, which are ~100% of
        ;; production runs and were 0% of the runs that printed the number. A
        ;; gauge wired to the rare branch is a gauge nobody will see move.
        receipt (cond-> {:resources (scan-resources projects)}
                  (seq refused)
                  (assoc :parser_admission_refused
                         {:count (count refused) :files refused}))]
    (conj entries {:receipt receipt})))

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
;; @spec MCP-OP-CENSUS-014
(defn- denied-ancestor
  "The nearest EXISTING ancestor directory of `path` this process may neither
   read nor traverse, or nil.

   Sol's round-fifteen item 9. `fs/exists?` is false of a readable file under a
   `chmod 000` parent, because the stat cannot get through the parent — so the
   entrance answered `:file-not-found`, \"name a source that exists\", about a
   file that is right there. That is the `file-not-found`/`file-not-readable`
   confusion commit 1038893 was written to end, reproduced by moving the
   permission bit one directory up. The two remedies differ, so the two answers
   must, and the refusal has to name the DIRECTORY whose bit must change rather
   than the file whose bits are already fine."
  [path]
  (loop [dir (fs/parent (fs/absolutize path))]
    (when dir
      (cond
        (not (fs/exists? dir))
        (recur (fs/parent dir))

        ;; Opus's round-seventeen item 3. The docstring said "ancestor
        ;; DIRECTORY" and the code never asked. A mode-644 regular FILE is
        ;; readable and not executable, so it passed the permission test by
        ;; accident, and `src/app/afile.clj/x.clj` — an ordinary source file in
        ;; a path prefix, which is ENOTDIR and not a permission problem at all
        ;; — was published as a denied directory: a refusal stating a falsehood
        ;; and carrying a remedy that cannot be followed, because the file is
        ;; already readable and making it more readable changes nothing. The
        ;; tool answered `not-found` for the same observation, which is the
        ;; only cross-entrance cause disagreement the ten-shape parity
        ;; enumeration found.
        (not (fs/directory? dir))
        nil

        (not (and (fs/readable? dir) (fs/executable? dir)))
        (str dir)

        :else nil))))

;; @spec MCP-OP-CENSUS-018
(defn census-workspace
  "The tree a CLI census is over, canonical, or the TYPED REFUSAL it earns.

   NEVER nil. Opus's round-nineteen item 2, blocking: this fn returned nil for
   every exception, `escaping-source` opened with `(when workspace …)`, and a
   nil workspace therefore answered \"not escaping\" for every path — not
   merely absent, but affirmatively reporting a containment it never tested.
   An unresolvable `:dir` is an ordinary operator typo and was enough to reach
   it: `:dir <typo> :file <link leaving the tree>` READ the target and
   published `{:ok true, :files-scanned 1, :read-complete true}` — a
   completeness claim over a tree the request never named, with a GREEN
   receipt, while the MCP entrance refused the identical request
   `invalid-workspace-root`.

   So a workspace that does not resolve is a REFUSAL, never a licence to read,
   and the nil state is made unrepresentable rather than handled. The name is
   the one the other entrance already publishes, because this is one
   observation and the two entrances disagreeing about it is the defect class
   this whole fence exists to close.

   Sol's round-eighteen item 2, blocking. Every read this op performs is now
   confined to this one answer, and the answer has to exist before the fence
   can ask its containment question at all.

   The workspace is the tree THE REQUEST NAMED:

   - `:dir` when the request gives one — the same `census-root` the walk is
     confined to, so a named `:file` and a walked member are measured against
     one fence and cannot disagree;
   - the `:file` ITSELF when the request names only a file, because a request
     that names one source is a census over exactly that source. Its workspace
     is the location the caller typed with every link ABOVE the final component
     resolved: the parent chain is the box's business, the final component is
     the request's. A link at that final component therefore leaves the
     one-file workspace, and is refused — which is the honest answer, since
     there is no tree in the request for the target to be inside of.

   Returns a `java.nio.file.Path`, never a string, because containment is a
   path-prefix question and `startsWith` on strings answers a different one
   (`/a/bc` starts with `/a/b`)."
  [dir file]
  (or (try
        (if (and (nil? dir) (string? file) (not (str/blank? file)))
          (let [named (.toPath (java.io.File. (str (fs/absolutize file))))
                parent (.getParent named)]
            (when parent
              (.resolve (.toRealPath parent
                                     (make-array java.nio.file.LinkOption 0))
                        (.getFileName named))))
          (let [^java.nio.file.Path real
                (.toRealPath (.toPath (java.io.File. (census-root dir)))
                             (make-array java.nio.file.LinkOption 0))]
            ;; Opus's round-twenty-one item 3. `toRealPath` succeeds on a
            ;; regular FILE, so "is there a tree here at all" was never
            ;; asked, and `:dir <a file>` produced
            ;; `{:error-type :no-fold-arms-found, :files-scanned 0}` — the
            ;; shape of a COMPLETENESS CLAIM over a tree that was never a
            ;; tree, while the MCP entrance refused the identical request
            ;; `invalid-workspace-root` and this same launcher refused it
            ;; `workspace-root-not-a-directory` one op over, under
            ;; `:ls-tree`. One entrance disagreeing with the other is the
            ;; class this fence exists to close; one entrance disagreeing
            ;; with ITSELF is that class with the excuse removed.
            ;;
            ;; A workspace is a TREE. `existing-directory?` is not reused
            ;; here deliberately: that predicate asks about the caller's
            ;; string, and the question at this point is about the path the
            ;; caller's string RESOLVED to, which is what every later fence
            ;; measures against.
            (when (.isDirectory (.toFile real)) real)))
        (catch Exception _ nil))
      {:error-type :invalid-workspace-root
       :error (str relation-census/workspace-root-token " is not an existing "
                   "directory, so there is no tree for this census to be over "
                   "and no fence a source could be inside of")}))

(defn resolved-workspace
  "The `java.nio.file.Path` in a `census-workspace` answer, or nil for a refusal.

   One predicate, so \"did the workspace resolve\" is asked the same way at
   every site rather than by each site's idea of what a workspace looks like."
  [workspace]
  (when (instance? java.nio.file.Path workspace) workspace))

;; @spec MCP-OP-CENSUS-018
(defn escaping-source
  "The REAL path of `path`, when it resolves outside `workspace`; else nil.

   `toRealPath` resolves EVERY link in the path, so a chain, an absolute
   target and a link into a sibling workspace are one question with one answer
   — a containment test that stops after one hop passes all three, which is
   why the witness drives all three.

   Returns the real path so the caller can decide what to say about it; what
   the caller must NOT do is publish it. The target is a fact about the box,
   the link is the fact about the request, and MCP-OP-CENSUS-014 has said since
   round sixteen which of those a refusal may name."
  [workspace path]
  ;; Opus's round-nineteen item 2, blocking. This opened `(when workspace …)`,
  ;; so a workspace that did not resolve was answered \"contained\" — a fence
  ;; reporting a test it never ran. The nil state is not handled here, it is
  ;; REFUSED: a caller that has not resolved its workspace has no containment
  ;; question to ask, and a guard returning nil in this position would
  ;; reproduce the defect exactly.
  (let [^java.nio.file.Path resolved (resolved-workspace workspace)]
    (when-not resolved
      (throw (ex-info (str "a containment question was asked with no resolved "
                           "workspace; the workspace must be refused before "
                           "any path is measured against it")
                      {:error-type :census-fence-misuse})))
    ;; `IOException` and not `Exception`, and NOT to nil: between the fence's
    ;; existence check and this call the filesystem may change, and a path
    ;; that no longer resolves is MISSING — which the caller publishes as
    ;; `:not-found`, the cause the other entrance publishes for it. Silence
    ;; here would be the fail-open again, one frame down.
    (let [real (try (.toRealPath (.toPath (java.io.File. (str path)))
                                 (make-array java.nio.file.LinkOption 0))
                    (catch java.io.IOException _ ::unresolvable))]
      (cond
        (= ::unresolvable real) ::unresolvable
        (.startsWith ^java.nio.file.Path real resolved) nil
        :else real))))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-018
;; @spec MCP-OP-CENSUS-019
(defn census-source-refusal
  "THE ONE FENCE every path this op reads passes through, before any open.

   nil when the path may be opened; otherwise the typed refusal it earns, the
   cause, and — when a directory in the path is what may not be read — that
   directory.

   Sol's round-fifteen items 1, 2, 5 and 7, blocking. Round fourteen asked the
   readability question in the `:file` branch of the entrance and nowhere else,
   so the `:dir` WALK — the ordinary invocation — still handed every path the
   discovery kernel found straight to `slurp`:

     :dir <tree with a chmod-000 source>
       {:error \"…/denied.clj (Permission denied)\", :error-type :invalid-arguments}

   untyped, no anchor, no remedy, and `:invalid-arguments` is in neither
   declared refusal set, so both enumeration witnesses were blind to it. A rule
   that lives in one branch is a rule the other branches break.

   It asks the SAME questions, in the same order, that `mcp-paths/
   resolve-source-path` asks at the other entrance, because the two entrances
   answering one tree differently is the defect class this closes:

   - existence FIRST, and when the answer is \"not there\", whether an ancestor
     directory is what this process may not read;
   - REGULARITY next, FOLLOWING links, so a FIFO, a socket, or a directory
     named `*.clj` is refused BEFORE any open. `fs/readable?` is true of a
     named pipe and `slurp` blocks on one forever with no writer: one FIFO
     anywhere under `:dir` wedged the census for thirty seconds with zero bytes
     on stdout and no diagnostic. Asked AFTER existence so a path that is not
     there is still reported as missing;
   - READABILITY last, so a directory is reported as a directory rather than as
     a permission problem, and before anything opens it.

   Sol's round-eighteen item 2, blocking: CONTAINMENT, asked between existence
   and regularity, exactly where `mcp-paths/resolve-source-path` asks it. This
   fence had no containment question at all, so a `:file` naming a link under
   the workspace published the bytes of the file it pointed at outside the
   workspace — while the walk, one branch over, counted the identical link
   `skipped-outside-root` and read nothing. After existence, because a link
   that resolves to nothing is missing and not an escape; before regularity,
   because what a path outside the workspace IS is not this census's business
   to report."
  [workspace path]
  (let [given (str path)
        absolute (str (fs/absolutize given))
        ;; DELAYED, so the ordering below is the ordering that runs: the
        ;; containment question is asked after existence and never before it.
        escape (delay (escaping-source workspace absolute))]
    (cond
      ;; Sol's round-eighteen item 3, and the only LEXICAL question this fence
      ;; asks: a path that is not a Clojure source is not a source this census
      ;; can read, whatever the filesystem says about it. First, because the
      ;; tool asks it first — `relative-source-path?` refuses before it stats
      ;; anything — and the two entrances answering one path differently is
      ;; the defect class this fence exists to close.
      (not (relation-census/named-source-extension? given))
      {:error-type :file-not-a-source-path
       :cause :not-a-relative-source-path
       :error (str given " is not a Clojure source path: a censused source "
                   "carries one of "
                   (str/join ", " (sort relation-census/named-source-extensions))
                   " as its extension")}

      (not (fs/exists? absolute))
      (if-let [parent (denied-ancestor absolute)]
        {:error-type :file-not-readable
         :cause :parent-denied
         :parent parent
         :error (str given " cannot be read: the directory " parent
                     " may not be read by this process")}
        ;; `:not-found` is published here for the reason every other branch of
        ;; this fence publishes a cause: the two entrances name their refusals
        ;; from different sets by design, so the CAUSE is the only field a
        ;; witness can compare across them — and Opus's round-sixteen item 2
        ;; found them disagreeing about a symlink loop and a name too long,
        ;; which `fs/exists?` reports here as "not there" and the tool reported
        ;; as unreadable with the exception text attached.
        {:error-type :file-not-found
         :cause :not-found
         :error (str given " does not exist")})

      ;; The link is named as the request spelled it; the TARGET is never
      ;; named, here or in the remedy. MCP-OP-CENSUS-014: a refusal that
      ;; publishes where a link points has told the caller a fact about the
      ;; box in the course of refusing to tell them one.
      ;; Opus's round-nineteen item 2, blocking, asked HERE — after existence,
      ;; before containment and before any open. A workspace that did not
      ;; resolve is not a workspace this path can be inside of, and the fence
      ;; fails CLOSED rather than answering a question it cannot ask.
      (nil? (resolved-workspace workspace))
      workspace

      ;; The path resolved a moment ago and does not now: it is MISSING, and
      ;; missing is the cause both entrances publish for it.
      (= ::unresolvable @escape)
      {:error-type :file-not-found
       :cause :not-found
       :error (str given " does not exist")}

      @escape
      {:error-type :file-outside-workspace
       :cause :outside-project
       :error (str given " resolves outside the workspace this census is "
                   "over, so reading it would answer about a tree the "
                   "request did not name")}

      (not (fs/regular-file? absolute))
      {:error-type :file-not-a-regular-file
       :cause :not-a-regular-file
       :error (str given " is not a regular file")}

      (not (fs/readable? absolute))
      {:error-type :file-not-readable
       :cause :permission-denied
       :error (str given " exists but cannot be read")})))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-017
(defn census-read-refusal
  "A read that failed AFTER the fence admitted the path, as a fence refusal.

   Opus's round-sixteen NO-GO items 1 and 3, blocking. Round sixteen gave the
   MCP reader this catch (`mcp-relation-census/read-failure-refusal`) and left
   this entrance's reader a bare `(slurp p)`, so the identical mode-flip storm
   answered at the two entrances differently:

     MCP  {:OK 14502, \"unreadable-source-path\" 5498}
     CLI  {:file-not-readable 16523, :census-adapter-failure 1623, :OK 1854}

   and each of those 1,623 carried round fourteen's REJECTED receipt —
   `census-adapter-failure`, `exhausted` false, and a resource-exhaustion
   remedy telling a request that named ONE file to point `:dir` at a directory
   it knows is smaller. That is the sentence the round-fifteen fix was written
   under, recurring with the entrances swapped: a rule that lives in one branch
   is a rule the other branches break.

   The fence answering does not end the question. Between the check and the
   read the filesystem may change, and it does — a mode flipped by another
   process, an ordinary editor's atomic save. The two are the SAME fact to a
   continuation, a name the next call must not carry, so they answer alike:
   the type the fence gives a path it may not read, and a cause that says the
   read is what failed rather than the check.

   The exception's own MESSAGE is not published, for the reason
   MCP-OP-CENSUS-014 states globally: `FileNotFoundException` renders as
   \"<absolute path> (Permission denied)\", and a refusal that leaks the
   server's absolute root tells the caller a fact about the box instead of a
   fact about their request."
  [given ^Throwable error]
  {:error-type :file-not-readable
   :cause :read-failed-after-fence
   :error (str given " passed the fence and then could not be read; its mode "
               "or its existence changed under the census ("
               (.getName (class error)) ")")})

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
         ;; The tree every read below is confined to. For a walk it is the
         ;; canonical root the kernel already walked; for a `:file` request it
         ;; is what the request named. ONE answer, computed once, handed to
         ;; the fence on every member — a containment rule that lives in one
         ;; branch is a rule the other branches break, which is precisely how
         ;; the walk came to be confined and the `:file` branch not.
         workspace (census-workspace dir file)
         relative #(str (fs/relativize root %))
         paths (if file
                 [(str (fs/absolutize file))]
                 (mapv #(str root "/" %) (:files discovered)))]
     (cond
       (and file (> (fs/size (first paths)) relation-census/max-source-bytes))
       {:oversized (relative (first paths))}

       ;; Both bounds are answered before a single source is read, and the
       ;; walk's own figures travel with the refusal: what it skipped and what
       ;; it collapsed are facts about the tree, not decorations on a success.
       (:walk-exceeded? discovered)
       {:walk-exceeded? true
        :entries-observed (:entries-observed discovered)
        :entries-yielded (:entries-yielded discovered)
        :discovered discovered
        :scanned 0
        :oversized-skipped (vec (:oversized discovered))
        :skipped-outside-root (:skipped-outside-root discovered 0)
        :duplicates (:duplicates discovered 0)}

       (:exceeded? discovered)
       {:exceeded? true
        :observed (:observed discovered)
        :discovered discovered
        :scanned 0
        :oversized-skipped (vec (:oversized discovered))
        :skipped-outside-root (:skipped-outside-root discovered 0)
        :duplicates (:duplicates discovered 0)}

       ;; A subtree the walk could not ENTER, decided after both bounds and
       ;; before anything is read — the same position, and for the same
       ;; reason, as a member the fence refuses. Opus's round-sixteen item 4.
       (seq (:unreadable-directories discovered))
       {:unreadable-directory (first (:unreadable-directories discovered))
        :unreadable-directories (vec (:unreadable-directories discovered))
        :discovered discovered
        :scanned 0
        :oversized-skipped (vec (:oversized discovered))
        :skipped-outside-root (:skipped-outside-root discovered 0)
        :duplicates (:duplicates discovered 0)}

       :else
       (let [;; WHERE the path came from, decided once. A `:file` request IS
             ;; the request, so the refusal names it exactly as the caller
             ;; spelled it and no narrowing exists; a walk member is named
             ;; project-relative and there is no request to narrow. The MCP
             ;; entrance decides the same two provenances from `requested`.
             from-request? (boolean file)
             shown (fn [p] (if from-request? (str file) (relative p)))
             provenance (if from-request? :request :walk)]
         (reduce
           (fn [acc p]
             ;; The fence, on EVERY member, before the open. Stopping at the
             ;; first refusable path is what the MCP entrance's `collect-inputs`
             ;; does, for the same reason: nothing after it can be trusted
             ;; either, and the refusal names the one the walk tripped on.
             (if-let [refused (census-source-refusal workspace p)]
               (reduced (assoc acc :unreadable
                               (assoc refused
                                      :file (shown p)
                                      :provenance provenance)))
               ;; The typed catch at the READ. Opus's round-sixteen items 1
               ;; and 3: the fence has answered, and between that answer and
               ;; this open the filesystem may change. `IOException` and not
               ;; `Throwable`, exactly as `collect-inputs` catches it: an
               ;; exhaustion is a different fact and keeps its own answer at
               ;; the op's catch-all.
               (let [read (try {:source (slurp p)}
                               (catch java.io.IOException error
                                 {:unreadable error}))]
                 (if-let [error (:unreadable read)]
                   (reduced (assoc acc :unreadable
                                   (assoc (census-read-refusal (shown p) error)
                                          :file (shown p)
                                          :provenance provenance)))
                   (let [source (:source read)
                         acc (update acc :scanned inc)]
                     (if (relation-census/defines-arms? source)
                       (update acc :inputs conj {:file (relative p)
                                                 :source source})
                       (cond-> acc
                         declared?
                         (update :declared into
                                 (relation-census/source-declared-names
                                   source)))))))))
           {:scanned 0
            :inputs []
            :declared #{}
            :oversized-skipped (vec (:oversized discovered))
            :skipped-outside-root (:skipped-outside-root discovered 0)
            :duplicates (:duplicates discovered 0)}
           paths))))))

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

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-017
(defn- census-crash-refusal
  "A Throwable that escaped the census, as a DECLARED typed refusal.

   Sol's round-fifteen review, NO-GO item 3. `:invalid-arguments` — the type
   the launcher's catch-all stamps on anything that reaches it — is in neither
   `cli-refusal-types` nor `mcp-refusal-types`, so every witness pinned to
   those sets was green over an entire class of answers this op can give. That
   is the general form of the `:dir` defect rather than the defect itself: the
   declared enumeration described a SUBSET of what the op emits, and the next
   escape would have had the same signature.

   The two names are the ones the MCP entrance already publishes for these two
   cases, because a throw is not a different KIND of event at the two
   entrances. It gets NO continuation, for the reason `exhaustion-refusal`
   gives at the tool: every continuation this op hands back is computed from
   the walk's own aggregates, and a walk that threw is exactly the case in
   which those were lost with it.

   `VirtualMachineError` is tested by NAME rather than with `instance?`,
   because a class literal is resolved when this namespace is analysed and
   babashka's runtime does not carry that class."
  [opts ^Throwable error]
  (let [exhausted? (boolean
                     (some #(= "java.lang.VirtualMachineError" (.getName ^Class %))
                           (supers (class error))))
        anchor (try (relation-census/cli-anchor opts) (catch Throwable _ nil))]
    (cond-> {:ok false
             :error-type (if exhausted?
                           :census-resource-exhausted
                           :census-adapter-failure)
             :error (str (if exhausted?
                           "The census exhausted a runtime resource: "
                           "The census failed: ")
                         (.getName (class error))
                         (when-let [message (.getMessage error)]
                           (str " " message)))
             :exhausted exhausted?
             :files-read 0
             :read-complete false
             :remedy (str "The census stopped part-way through, so the walk's "
                          "own aggregates were lost with it and this refusal "
                          "can compute no narrower command: point :dir at a "
                          "directory you know is smaller, or census one :file "
                          "at a time, and retry.")}
      anchor (assoc :anchor anchor))))

;; @spec MCP-OP-CENSUS-015
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-021
(defn- run-relation-census*
  "The census op body. Every exit from it is a receipt or a typed refusal;
   `run-relation-census` is what guarantees that for the exits it does not
   plan."
  [{:keys [dir file doors threads] :as opts}]
  (let [doors-arg doors
        ;; The SAME pure pass the entrance (`run`) runs ahead of its config
        ;; load, called again here so an in-process caller of this op
        ;; function gets the identical refusal without going through the CLI
        ;; dispatch. Pure, so running it twice costs nothing and cannot
        ;; differ. It is handed the WHOLE request, not one field of it: Sol's
        ;; round-ten item 4 was that this call passed `{:threads threads}`
        ;; and the validator destructured `{:keys [threads]}`, so every other
        ;; malformed shape reached the filesystem first.
        shape-refusal (relation-census/validate-cli-request-shape opts)
        ;; The workspace the caller named, computed once, by the SAME pure
        ;; function the shape pass uses. Sol's round-eleven item 2, blocking:
        ;; round ten anchored the shape refusals and left the post-scan ones
        ;; spelling their own command, so an undefined door — a question only
        ;; the scan can answer — still handed back the literal `:dir .` and
        ;; replaying it censused the replay's cwd. A rule that lives in one
        ;; branch is a rule the other branches break, so every refusal below
        ;; carries this anchor, and every continuation below is built by
        ;; `relation-census/cli-continuation`, which renders it shell-safe.
        anchor (relation-census/cli-anchor opts)
        continue-with (fn [fix] (relation-census/cli-continuation anchor fix))
        ;; The remedy that REPLACES a continuation too large to send, from
        ;; the one place that knows what it measured. Sol's round-twelve
        ;; item 3: the wording was spelled out at three sites and named the
        ;; bound without naming the value it compared against.
        overflow-remedy (fn [fix]
                          (relation-census/cli-continuation-overflow-remedy
                            anchor fix))
        ;; A continuation that NARROWS to a subtree still goes through the one
        ;; builder: the subtree is just a different anchor, made absolute the
        ;; same way, so a narrowing can no more be relative — or unquoted —
        ;; than a retry can.
        narrow-to (fn [path]
                    (relation-census/cli-continuation
                      (relation-census/cli-anchor {:dir path}) :none))
        ;; A refusal offers exactly one of a continuation and a remedy
        ;; (MCP-OP-CENSUS-014): a null continuation is not a smaller promise
        ;; than a real one, it is a field the caller must interpret.
        or-remedy (fn [continuation remedy]
                    (or continuation {:remedy remedy}))
        pool (when (some? threads) (relation-census/coerce-pool-size threads))
        parsed-doors (if doors
                       (relation-census/parse-doors
                         (str/split (str doors) #",") nil)
                       relation-census/default-doors)
        want-declared? (boolean doors)
        ;; The entrance's own fence call, DELAYED: MCP-OP-CENSUS-016 requires
        ;; the shape pass to touch nothing, and a `let` binding is forced
        ;; before the first `cond` branch is tested.
        ;; The workspace, resolved ONCE per request and DELAYED, because
        ;; MCP-OP-CENSUS-016 requires the shape pass above to touch nothing
        ;; and resolving a root is a filesystem question.
        workspace (delay (census-workspace dir file))
        named-refusal (delay (when (string? file)
                               (census-source-refusal @workspace file)))
        scan (delay (census-sources dir file {:declared? want-declared?}))
        ;; ONE fact bundle, published by EVERY receipt shape below that got as
        ;; far as a scan — success, no-fold-arms-found and every refusal. The
        ;; MCP tool builds its own from the same kernel, so the two entrances
        ;; cannot publish different evidence for one tree. Forced only inside
        ;; a branch that has already forced the scan; the bounds refusals
        ;; above it never walk anything.
        facts #(relation-census/discovery-facts
                 {:files-scanned (:scanned @scan)
                  :skipped-outside-root (:skipped-outside-root @scan 0)
                  :duplicates (:duplicates @scan 0)
                  :oversized (:oversized-skipped @scan)}
                 :kebab)]
    (cond
      (some? shape-refusal)
      shape-refusal

      (map? parsed-doors)
      (merge
        {:ok false
         :error-type :unknown-door-symbol
         :error (str "Unknown identity door " (:invalid parsed-doors) ": "
                     (:why parsed-doors))
         :door (:invalid parsed-doors)
         :known-doors (vec (sort (map str relation-census/default-doors)))
         :anchor anchor}
        (or-remedy (continue-with :doors) (overflow-remedy :doors)))

      ;; @spec MCP-OP-CENSUS-018
      ;; Opus's round-nineteen item 2, blocking. A `:dir` THE CALLER GAVE that
      ;; does not resolve is refused HERE, at the entrance, before any path is
      ;; measured and before anything is read — parity with the MCP entrance,
      ;; which has refused this as `invalid-workspace-root` since it shipped.
      ;;
      ;; Guarded on `(some? dir)` deliberately. When the request names only a
      ;; `:file`, the workspace IS that file, so a workspace that does not
      ;; resolve means the FILE does not resolve, and the honest answer is the
      ;; fence's `:file-not-found` — which is what the ten-shape parity
      ;; enumeration asserts for a missing file, a symlink loop and a name too
      ;; long. Refusing those as a bad workspace would be a second name for
      ;; one observation, which is the defect this branch exists to close.
      (and (some? dir) (nil? (resolved-workspace @workspace)))
      (merge
        {:ok false
         :anchor anchor}
        @workspace
        {:remedy (str relation-census/workspace-root-token
                      " is not an existing directory, so nothing about it can "
                      "be narrowed and no source can be inside it: name a "
                      "directory that exists with :dir, or name one source to "
                      "census with :file.")})

      ;; @spec MCP-OP-CENSUS-014
      ;; The FIRST filesystem question this op asks about a NAMED source, and
      ;; it is asked HERE, at the entrance, before `@scan` is forced. Sol's round-thirteen item 7:
      ;; a `:file` that does not exist reached `census-sources`, which stats
      ;; the named path with `fs/size` before anything had asked whether it
      ;; was there, and the `java.nio.file.NoSuchFileException` surfaced
      ;; through the launcher as a bare `:invalid-arguments` whose entire
      ;; payload was the path — no type to branch on, no anchor, no remedy.
      ;; Round fourteen added the readability question beside it, and Sol's
      ;; round-fifteen items 1/2/5/7 found the ANSWER living in this branch
      ;; alone while the `:dir` walk three frames down still read whatever it
      ;; was handed. So the questions moved into `census-source-refusal`, the
      ;; one fence BOTH the named `:file` and every walk-discovered member now
      ;; pass through, and this branch is that fence applied to the one path
      ;; the caller named.
      ;;
      ;; It cannot live in the pure shape pass: existence is a filesystem
      ;; question and MCP-OP-CENSUS-016 requires that pass to touch nothing.
      ;;
      ;; No continuation, and that is the honest answer: the file this op was
      ;; given IS the request, so the request minus it is not a request.
      ;;
      ;; The refusals are separately NAMED rather than one name carrying a
      ;; cause, because their remedies differ — "name a source that exists"
      ;; against "the source is there, fix what may read it" against "that
      ;; path is not a file at all" — and a caller who must read a second
      ;; field to learn which remedy applies has been handed a branch dressed
      ;; as a type. The enumeration witness drives on the type NAME, so a
      ;; distinct name is also what makes each impossible to ship unexercised.
      ;; `:cause` is carried BESIDE the name, never instead of it: it
      ;; distinguishes the file's own bit from a parent directory's, which is
      ;; a fact about what to fix and not a second remedy.
      (some? @named-refusal)
      (let [{:keys [error-type error cause parent]} @named-refusal]
        (cond-> {:ok false
                 :error-type error-type
                 :anchor anchor
                 :error error
                 :file file
                 :remedy
                 (case error-type
                   :file-not-found
                   (str file " does not exist, and the one source this op was "
                        "given IS the request, so the request minus it is not "
                        "a request and no narrower command can be computed: "
                        "name a source that exists with :file, or point :dir "
                        "at a directory to census its tree.")

                   ;; Sol's round-eighteen item 2. The remedy names the LINK
                   ;; and the two things the caller can do about it, and never
                   ;; where the link points: a refusal that publishes the
                   ;; target has told the caller a fact about the box in the
                   ;; course of refusing to tell them one.
                   :file-not-a-source-path
                   (str file " is not a Clojure source path, and the one "
                        "source this op was given IS the request, so the "
                        "request minus it is not a request and no narrower "
                        "command can be computed: name a "
                        (str/join ", " (sort relation-census/named-source-extensions))
                        " source with :file, or point :dir at a directory to "
                        "census its tree.")

                   :file-outside-workspace
                   (str file " resolves outside the workspace this census is "
                        "over, and a census is a completeness claim about a "
                        "named tree, so reading it would answer about a tree "
                        "the request did not name and no narrower command can "
                        "be computed: name the tree that source really lives "
                        "in with :dir, or name a source whose real path stays "
                        "inside the tree you are censusing.")

                   :file-not-a-regular-file
                   (str file " is not a regular file — a directory, a named "
                        "pipe or a socket carrying a source name is refused "
                        "before it is opened, because reading one blocks or "
                        "fails rather than yielding a source — and the one "
                        "source this op was given IS the request, so no "
                        "narrower command can be computed: name a regular "
                        "file with :file, or point :dir at a directory to "
                        "census its tree.")

                   (if (= :parent-denied cause)
                     (str file " is there, and the directory " parent
                          " is what this process may not read, so the file "
                          "cannot be reached; the one source this op was "
                          "given IS the request, so the request minus it is "
                          "not a request and no narrower command can be "
                          "computed: make " parent " readable, name a "
                          "reachable source with :file, or point :dir at a "
                          "directory to census its tree.")
                     (str file " exists but this process may not read it, and "
                          "the one source this op was given IS the request, "
                          "so the request minus it is not a request and no "
                          "narrower command can be computed: make the file "
                          "readable, name a readable source with :file, or "
                          "point :dir at a directory to census its tree.")))}
          cause (assoc :cause cause)
          parent (assoc :parent parent)))

      (:oversized @scan)
      {:ok false
       :error-type :source-too-large
       :anchor anchor
       :error (str (:oversized @scan) " is larger than "
                   relation-census/max-source-bytes " bytes")
       :file (:oversized @scan)
       :maximum relation-census/max-source-bytes
       ;; The op was given ONE named :file. The request minus it is not a
       ;; request, so no narrower command can be computed and the refusal says
       ;; so rather than captioning the argument the caller must supply.
       :remedy (str (:oversized @scan) " is the one source this op was given "
                    "and it is larger than " relation-census/max-source-bytes
                    " bytes, so no narrower command can be computed: name a "
                    "source under the byte cap with :file, or point :dir at a "
                    "directory, where an oversized source is skipped and "
                    "counted instead of refused.")}

      ;; The entry bound: the ceiling bounds what the census READS, this one
      ;; bounds what the walk COSTS, and a tree of non-sources trips only this.
      (:walk-exceeded? @scan)
      (let [discovered (:discovered @scan)
            narrower (census-discovery/entry-narrowing-subtree discovered)
            continuation (when narrower
                           (narrow-to (str (:root discovered) "/" narrower)))]
        (merge
          {:ok false
           :error-type :too-many-walk-entries
           :error (str "This directory holds more than "
                       relation-census/max-walk-entries
                       " filesystem entries ("
                       (:entries-observed @scan)
                       " visited before the walk stopped). The census visits "
                       "at most " relation-census/max-walk-entries
                       " entries and will not report a truncated tree as a "
                       "complete census")
           :maximum relation-census/max-walk-entries
           :fits relation-census/max-walk-entries
           :observed (:entries-observed @scan)
           :observed-at-least true
           :entries-yielded (:entries-yielded @scan)
           :files-read 0
           :read-complete false
           :anchor anchor}
          ;; A null continuation is not a smaller promise than a real one; the
          ;; refusal offers exactly one of a next-command and a remedy.
          (or-remedy
            continuation
            (str "The walk stopped at the entry bound, so every count it "
                 "observed is a lower bound and no subtree it finished "
                 "walking is known to fit; point :dir at a directory you know "
                 "is smaller, or census one :file at a time."))
          (facts)))

      ;; The same ceiling semantics as the MCP entrance: a tree the census may
      ;; not finish is refused with nothing read, never truncated into success,
      ;; and the continuation is COMPUTED from the walk's own per-directory
      ;; aggregates rather than described in prose the caller cannot run.
      (:exceeded? @scan)
      (let [discovered (:discovered @scan)
            narrower (census-discovery/narrowing-subtree discovered)
            continuation (when narrower
                           (narrow-to (str (:root discovered) "/" narrower)))]
        (merge
          {:ok false
           :error-type :too-many-candidate-files
           :error (str "This directory holds more than "
                       relation-census/max-scanned-files
                       " candidate Clojure sources (" (:observed @scan)
                       " seen before the walk stopped). The census reads at "
                       "most " relation-census/max-scanned-files
                       " and will not report a truncated tree as a complete "
                       "census")
           :maximum relation-census/max-scanned-files
           :fits relation-census/max-scanned-files
           :observed (:observed @scan)
           :observed-at-least true
           :files-read 0
           :read-complete false
           :anchor anchor}
          (or-remedy
            continuation
            (str "The walk stopped at the ceiling, so every count it observed "
                 "is a lower bound and no subtree it finished walking is "
                 "known to fit; point :dir at a directory you know is "
                 "smaller, or census one :file at a time."))
          (facts)))

      ;; @spec MCP-OP-CENSUS-014
      ;; A member the WALK discovered that the fence refuses. It is decided
      ;; after both bounds, exactly as the tool decides it — a tree the census
      ;; may not finish is refused before anything is read — and it answers
      ;; the way the tool answers the same tree: typed, naming the member by
      ;; its project-relative path, with the walk's own figures, and with a
      ;; remedy rather than a continuation. There is no continuation to
      ;; compute: the path came from the WALK and not from the request, so
      ;; there is no request to narrow.
      ;; @spec MCP-OP-CENSUS-014
      ;; @spec MCP-OP-CENSUS-018
      ;; A subtree the walk could not ENTER. Opus's round-sixteen item 4: this
      ;; was swallowed with `:continue` and no counter, so a `chmod 000`
      ;; directory holding a thousand arms was invisible and the receipt still
      ;; said `read-complete true` — while ONE unreadable FILE refused the
      ;; whole census. A census is a completeness claim; a subtree the walk
      ;; could not enter falsifies it exactly as an unreadable member does, so
      ;; it earns the same type, and its own cause because what must change is
      ;; a bit on a DIRECTORY. No continuation: the path came from the walk.
      (:unreadable-directory @scan)
      ;; Opus's round-seventeen item 5, the CLI half of the identical defect:
      ;; a root the walk cannot enter is recorded walk-relative as `""` and
      ;; interpolated into three sentences. Same function as the tool, so the
      ;; two entrances cannot drift apart on what they call the root.
      ;; Sol's round-eighteen item 4: this remedy named the same subject twice
      ;; in one sentence, once by the token and once by the server's absolute
      ;; path. The root has ONE name; the absolute path is in `:anchor`, which
      ;; is where a reader checks their request and where a replay reads it.
      (let [directory (relation-census/shown-directory
                        (:unreadable-directory @scan))]
        (merge
          {:ok false
           :error-type :file-not-readable
           :cause :directory-denied
           :anchor anchor
           :directory directory
           :error (str "the directory " directory
                       " may not be read or traversed by this process, so "
                       "this census cannot claim to have read the tree")
           :remedy (str directory " came from the workspace walk, not from "
                        "the request, so there is no request to narrow and no "
                        "narrower command can be computed: "
                        (relation-census/directory-repair-phrase directory)
                        ", remove it, or name the sources to census with "
                        ":file. A census is a completeness claim, and a "
                        "subtree this process may not enter cannot be counted "
                        "as read.")}
          (facts)))

      ;; The remedy is chosen by PROVENANCE, not by the type. Opus's
      ;; round-sixteen item 1: a read that failed after the fence on the ONE
      ;; source a `:file` request named reached this branch, and the walk's
      ;; wording told a one-file request that the path "came from the
      ;; workspace walk" and to remove or repair it — a remedy about a tree the
      ;; caller never asked for. A `:file` request IS the request, so it earns
      ;; the same wording every other named-source refusal above earns.
      (:unreadable @scan)
      ;; Sol's round-eighteen item 4, the same class one branch over: the
      ;; walk-provenance wording named the root absolutely. The subject here is
      ;; a MEMBER, named project-relative, so the tree it is under is named by
      ;; the root's one name.
      (let [{:keys [error-type error cause parent file provenance]}
            (:unreadable @scan)]
        (cond-> (merge
                  {:ok false
                   :error-type error-type
                   :anchor anchor
                   :error error
                   :file file
                   :remedy
                   (if (= :request provenance)
                     (str file " passed the fence and then could not be read "
                          "— its mode or its existence changed under the "
                          "census — and the one source this op was given IS "
                          "the request, so the request minus it is not a "
                          "request and no narrower command can be computed: "
                          "make " file " readable and stable, name another "
                          "readable regular file with :file, or point :dir at "
                          "a directory to census its tree.")
                     (str file " came from the workspace walk, not from the "
                          "request, so there is no request to narrow and no "
                          "narrower command can be computed: remove or repair "
                          "it under " relation-census/workspace-root-token
                          (when parent
                            (str " (the directory " parent
                                 " is what this process may not read)"))
                          (when (= :read-failed-after-fence cause)
                            (str " (it passed the fence and then could not be "
                                 "read; its mode or its existence changed "
                                 "under the census)"))
                          ", or name a readable regular file with :file."))}
                  (facts))
          cause (assoc :cause cause)
          parent (assoc :parent parent)))

      :else
      (let [inputs (:inputs @scan)
            doors parsed-doors]
        (if (empty? inputs)
          (merge
            {:ok false
             :error-type :no-fold-arms-found
             :error "No file defines defmethod fold-event arms"
             :dir (census-root dir)
             :anchor anchor
             ;; Nothing was found, so there is no subtree to narrow to and no
             ;; command to compute: the refusal names what it scanned instead
             ;; of captioning the directory the caller was supposed to pick.
             ;; Sol's round-eighteen item 4. `:dir` above carries the
             ;; absolute root, which is the field a reader checks and a replay
             ;; reads; the SENTENCE uses the root's one name.
             :remedy (str "Nothing under "
                          relation-census/workspace-root-token
                          " defines defmethod fold-event arms ("
                          (:scanned @scan) " file(s) scanned), so no narrower "
                          "command can be computed: point :dir at a directory "
                          "whose sources define fold arms, or name one with "
                          ":file.")}
            (facts))
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
              ;; A per-file refusal from the plan phase — an unparseable
              ;; source, or a worker that threw. It names the file it failed
              ;; on, which is not a narrower REQUEST: the tree minus one
              ;; source is not expressible in this grammar. So it carries the
              ;; anchor and a remedy, and no continuation at all.
              (not (:ok result))
              (merge result
                     {:anchor anchor
                      :remedy (str (:file result)
                                   " could not be censused, and a request "
                                   "cannot name a tree minus one source, so "
                                   "no narrower command can be computed: fix "
                                   "that source, or census a directory that "
                                   "excludes it with :dir.")})

              ;; Whether a door is DEFINED can only be answered once the scan
              ;; has been parsed: confirm it against the plan's own :declared
              ;; plus the names read from the files that define no arms.
              (map? confirmed)
              (merge
                {:ok false
                 :error-type :unknown-door-symbol
                 :error (str "Unknown identity door " (:invalid confirmed) ": "
                             (:why confirmed))
                 :door (:invalid confirmed)
                 :known-doors (vec (sort (map str relation-census/default-doors)))
                 :anchor anchor}
                ;; Sol's round-eleven item 2, exactly here: this branch spelled
                ;; `:dir .` and the replay censused the replay's cwd.
                (or-remedy (continue-with :doors) (overflow-remedy :doors))
                (facts))

              :else
              (let [unrecognised (relation-census/unrecognised-summary
                                   (:unrecognised result) 5)
                    skipped (:oversized-skipped @scan)]
                (-> result
                    (dissoc :all-sites :declared :unrecognised)
                    ;; The discovery facts are the SAME facts every refusal
                    ;; above publishes, through the same kernel the tool uses.
                    (merge (facts))
                    (assoc :read-complete (empty? skipped)
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

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-017
(defn run-relation-census
  "Census collection writes inside fold arms. Reads only; writes nothing.

   The one entrance, and the one place a Throwable from the census path is
   turned into a DECLARED refusal. Nothing below may reach the launcher's
   catch-all: a refusal typed `:invalid-arguments` is a refusal no enumeration
   witness can see."
  [opts]
  (let [result (try
                 (run-relation-census* opts)
                 (catch Throwable error
                   (census-crash-refusal opts error)))]
    ;; Opus's round-sixteen item 7. Applied HERE, at the op's last step,
    ;; rather than at the sites that build the strings: a bound enforced at
    ;; some of a namespace's construction sites is not a bound, it is those
    ;; sites' habit. A receipt is not touched — it carries its own 4,096-byte
    ;; cap and its own trimming rules — and neither is a continuation, which
    ;; is short by construction and whose truncation would name a DIFFERENT
    ;; file rather than fail.
    (if (false? (:ok result))
      (relation-census/bound-refusal result)
      result)))

;; @spec MCP-OP-SHELL-ARGV-002
(defn ls-tree-root-refusal
  "Typed refusal when an :ls-tree root is not an existing directory; nil when
   the root is usable. A root that fails this check must never reach project
   discovery: an empty result is indistinguishable from an empty tree, and the
   caller needs to know its root was wrong (Andon pull inb-d27b79)."
  [dir]
  (when-not (existing-directory? dir)
    {:error (str ":ls-tree :dir must be an existing directory: "
                 (pr-str (str dir)))
     :error-type :workspace-root-not-a-directory
     :dir (str dir)
     :next-action "pass_an_existing_directory_path"}))

(defn run-ls-tree
  "Outline every Clojure project under :dir.

   The `:format` value is bound as `output-format`, NOT destructured as
   `format`: a binding named `format` shadows clojure.core/format for the whole
   body, so the empty-result branch below called the caller's :format VALUE as
   a function (`ArityException: Wrong number of args (3) passed to: :edn`).
   Never name a local after a core fn this body calls."
  [{:keys [dir grep] output-format :format :as _opts}]
  (when-not dir
    (println "Error: :dir is required for :ls-tree")
    (System/exit 1))
  (let [dir (try (str (fs/absolutize dir))
                 (catch Exception _e (str dir)))]
    (if-let [refusal (ls-tree-root-refusal dir)]
      ;; The CLI top level turns a result map carrying :error into exit 1,
      ;; so the shell contract is unchanged while the refusal stays testable.
      refusal
      (let [projects (if grep
                       ;; Fast path: rg first, skip expensive directory globbing
                       (let [hits (grep-tree grep dir)]
                         (discover-projects-grep hits dir))
                       ;; Full scan: discover all projects
                       (discover-projects dir))]
        (if (empty? projects)
          ;; NOTE (inb-eca3b1): calling System/exit from inside a library
          ;; operation is owed a separate fix; the exit-1 contract is unchanged
          ;; here, only the message that precedes it.
          (do (println (format "No Clojure files found under %s%s"
                               dir (if grep (str " matching '" grep "'") "")))
              (System/exit 1))
          (let [projects (outline-all-files projects)]
            (if (= output-format :edn)
              (format-ls-tree-edn projects dir)
              (format-ls-tree-text projects dir))))))))

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
                       ;; Pure request-shape validation the ENTRANCE runs
                       ;; before it loads project aliases: MCP-OP-CENSUS-016
                       ;; says a malformed request refuses before any
                       ;; filesystem work, and config discovery is filesystem
                       ;; work.
                       :shape     #'relation-census/validate-cli-request-shape
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

;; @spec MCP-OP-CENSUS-014
(defn- print-launcher-refusal!
  "THE ONE PLACE the LAUNCHER's own refusals are printed, and bounded.

   Sol's round-eighteen item 1, blocking. `run-relation-census` bounds the op's
   exits and `mcp-relation-census/entrance-bounded` bounds the tool's, and the
   launcher — which is the public CLI entrance, the thing an operator actually
   types — bounded nothing: `parse-args` throws BEFORE dispatch, so a repeated
   10,001-character `:doors` reached the catch-all below and was printed
   verbatim, twice in `:values` and once in the message, 20,228 bytes with no
   truncation marker.

   The bound is a property of the EXIT, not of the op. `census/bound-refusal`
   is the same function the op's last step uses, for the reason its own
   docstring gives: a bound enforced at some of the sites is not a bound, it is
   those sites' habit, and the habit does not travel to the site added next
   round. So there is ONE printing site for a launcher refusal, and it is
   bounded; the names it can print are declared in
   `census/launcher-refusal-types` and enumerated by a witness that drives both
   real launchers as subprocesses.

   Returns the bounded map, because `-main`'s exit code is decided from it."
  [refusal]
  (let [bounded (relation-census/bound-refusal refusal)]
    (pp/pprint bounded)
    bounded))

(defn- run-op
  "Dispatch one shape-validated request. Loads project aliases first."
  [canonical {:keys [op] :as opts}]
  ;; Load .clj-surgeon.edn project aliases from nearest config file
  (when-let [anchor (or (:file opts) (:clj opts) (:cljs opts) (:dir opts))]
    (forms/init-from-file! anchor))
  (let [opts (if (or (#{:change :change!} canonical)
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
    (cond
      (string? result) (println result)
      ;; Round nineteen. THIS branch is a LAUNCHER refusal, not an op's: it is
      ;; about the `:op` in the request, and no op ran. So it leaves through
      ;; the launcher's one bounded exit, exactly as its `--help` twin does.
      ;; Every other result here is an op's own — a receipt or a typed op
      ;; refusal — and is printed untouched, because a bound belongs at the
      ;; exit that OWNS the answer and this function does not own theirs.
      (= :unknown-operation (:error-type result)) (print-launcher-refusal! result)
      :else (pp/pprint result))
    result))

(defn run [{:keys [op] :as opts}]
  (let [canonical (resolve-op op)
        ;; The op's PURE request-shape pass, ahead of every filesystem call
        ;; this entrance makes — `run-op`'s config load included. Sol's
        ;; round-nine finding was that ordering: `bb … :threads
        ;; not-a-number` refused only after this entrance had stat'ed the
        ;; workspace, read its `.clj-surgeon.edn`, and walked the ancestor
        ;; chain. "Before any filesystem work" (MCP-OP-CENSUS-016) has to
        ;; mean the entrance, not just the op body; the round-eight witness
        ;; instrumented the op body and was blind to exactly this frame.
        shape-refusal (when-let [shape (:shape (get ops-registry canonical))]
                        (shape opts))]
    (if shape-refusal
      ;; Opus's round-seventeen item 1, blocking, the CLI half. This branch
      ;; RETURNS the op's pure shape refusal without ever entering the op, so
      ;; the bound `run-relation-census` applies at its own last step never saw
      ;; it: `:threads <10,001 a's>` printed a 10,514-byte refusal with a
      ;; 10,054-character field and no truncation marker, and so did an
      ;; unknown argument whose NAME was that long, and a `:dir` that did not
      ;; decode.
      ;;
      ;; The same one function the op's exit uses, at THIS exit, because this
      ;; is an exit — that is the whole content of the rule. `:relation-census`
      ;; is the only op that declares a `:shape` pass, so nothing else changes
      ;; shape here; an op that declares one later inherits the bound rather
      ;; than rediscovering this defect.
      (let [bounded (relation-census/bound-refusal shape-refusal)]
        (pp/pprint bounded)
        bounded)
      (run-op canonical opts))))

(def max-argument-nesting-depth
  "How deeply one CLI argument may nest before it is refused unread.

   Opus's round-twenty-one item 4. `edn/read-string` is recursive, so a
   10,001-deep nested argument overflowed the reader's stack and left both
   real launchers as an untyped `StackOverflowError` — an `Error`, which
   `-main`'s `catch Exception` never saw. Nothing was evaluated and no caller
   value was published unbounded, which is why the reviewer ruled it
   non-blocking; what it broke is the claim that every refusal the launcher
   prints leaves through ONE bounded exit, because a raw stack trace is a
   refusal no enumeration can drive.

   256, and the number is a CEILING rather than a measurement of what the
   reader survives: every argument this CLI accepts is a path, a keyword, a
   flat list of door symbols or a one-level map, so a legitimate request is
   two or three deep and the ceiling is three orders of magnitude of slack.
   A bound set where the stack happens to give out would move with the JVM,
   the platform and the thread; a bound set where the REQUESTS are is a
   property of the tool."
  256)

(def ^:private opening-delimiters
  "The three characters that open a nesting level in EDN."
  #{\[ \{ \(})

(def ^:private closing-delimiters
  "The three characters that close one."
  #{\] \} \)})

(defn- scanned-nesting-depth
  "The deepest run of open delimiters in `s`, measured WITHOUT a reader.

   Character scanning, deliberately: the whole point is to answer \"is this
   too deep to read\" before anything recursive touches it, and a reader that
   throws on depth has already used the stack it was supposed to protect.

   EDN strings and character literals are skipped, so a path or a door name
   containing a bracket is not counted as nesting. It stops as soon as the
   ceiling is exceeded, so the scan is bounded by the answer rather than by
   the argument."
  [^String s]
  (let [n (.length s)]
    (loop [i 0 depth 0 deepest 0 in-string? false escaped? false]
      (if (or (>= i n) (> deepest max-argument-nesting-depth))
        deepest
        (let [c (.charAt s i)]
          (cond
            in-string?
            (recur (inc i) depth deepest
                   (not (and (not escaped?) (= c \")))
                   (and (not escaped?) (= c \\)))

            (= c \") (recur (inc i) depth deepest true false)

            ;; A character literal: `\\[` is a bracket the caller wrote, not a
            ;; delimiter, so the next character is consumed whatever it is.
            (= c \\) (recur (+ i 2) depth deepest false false)

            (opening-delimiters c)
            (let [d (inc depth)] (recur (inc i) d (max deepest d) false false))

            (closing-delimiters c)
            (recur (inc i) (dec depth) deepest false false)

            :else (recur (inc i) depth deepest false false)))))))

(defn- refuse-over-nested!
  "Throw the DECLARED launcher refusal when one argument nests past the
   ceiling. Named separately from `parse-val` so the two branches that read
   cannot drift into checking different things."
  [^String s]
  (let [measured (scanned-nesting-depth s)]
    (when (> measured max-argument-nesting-depth)
      (throw (ex-info
               (str "an argument nests at least " measured
                    " deep, past the " max-argument-nesting-depth
                    "-level ceiling; it is refused unread, because a reader "
                    "deep enough to measure it is a reader deep enough to "
                    "overflow")
               {:error-type :argument-nesting-too-deep
                :ceiling max-argument-nesting-depth
                :measured measured
                :value s})))))

(defn parse-val
  "Parse a single CLI value string into its Clojure equivalent.
   Pure: string in, value out.

   `edn/read-string`, NOT `clojure.core/read-string`, and the difference is a
   class rather than a nicety. `clojure.core/read-string` honours `*read-eval*`,
   which defaults to true, so the reader EVALUATES `#=(…)` in caller text —
   demonstrated at this branch's tip through the real JVM launcher:

     $ clj-surgeon :op :relation-census :dir .
         :doors '[#=(clojure.core/println \"PWNED-ARBITRARY-EVAL\")]'
     PWNED-ARBITRARY-EVAL
     {:ok false, :error-type :doors-not-a-string, …}

   The evaluation happened while the op was REFUSING the argument, which is
   the tell: the reader ran before any validation could. argv is usually the
   operator's own shell, so the blast radius is small in the ordinary case —
   but it is not always the operator's: a wrapper, a config-driven runner or
   an agent composing argv from a request turns a value into code, and \"the
   caller could have run it anyway\" is an argument about the ordinary case
   made about the one that is not.

   `edn/read-string` reads the data these two branches were written for —
   vectors, maps, sets, keywords, symbols, strings and numbers — and refuses
   `#=`, arbitrary tagged literals and every other reader escape. A value it
   cannot read throws, and the launcher publishes the same bounded
   `:invalid-arguments` it already publishes for a token that does not parse."
  [s]
  (cond
    (= s "true") true
    (= s "false") false
    (.startsWith s ":") (keyword (subs s 1))
    (.startsWith s "[") (do (refuse-over-nested! s) (edn/read-string s))
    (.startsWith s "{") (do (refuse-over-nested! s) (edn/read-string s))
    :else s))

(defn parse-args
  "Parse CLI arg strings into an opts map.
   Pure: string sequence in, map out.

   A REPEATED flag is refused, naming it. Sol's round-eleven item 7: this fn
   built its map with `(into {})`, so `:file one.clj :file two.clj` collapsed
   last-one-wins and the census reported `:ok true` over `two.clj` alone —
   the caller asked about two sources and got a receipt claiming completeness
   about one. That is the failure MCP-OP-CENSUS-019 already names for an
   argument the op does not accept, in a different disguise: an argument
   silently DROPPED tells the caller a bound was applied that never existed,
   and here the drop is invisible in the receipt as well. No op in this CLI
   has a repeatable flag, so a repeat is a malformed request rather than a
   list, and the refusal is generic for the same reason the collapse was: this
   fn builds the map before anything knows which op it is for."
  [args]
  (let [help-flags #{"--help" "-h"}
        has-help?  (some help-flags args)
        kv-args    (remove help-flags args)]
    (when (odd? (count kv-args))
      (throw (ex-info "Arguments must be key-value pairs"
                      {:error-type :invalid-arguments})))
    (let [pairs (->> kv-args
                     (partition 2)
                     (mapv (fn [[k v]]
                             (let [key (keyword (subs k 1))]
                               [key (if (#{:match :with :contains :query :expr :expect} key) v (parse-val v))]))))
          repeated (->> (map first pairs)
                        frequencies
                        (filter (fn [[_ n]] (> n 1)))
                        (map first)
                        (sort-by name)
                        first)]
      (when repeated
        (let [flag (str ":" (name repeated))
              times (count (filter #(= repeated (first %)) pairs))]
          (throw (ex-info
                   (str flag " was given " times
                        " times; every clj-surgeon argument is given at most "
                        "once, and a repeated one would be silently dropped")
                   {:error-type :duplicate-argument
                    :argument flag
                    :occurrences times
                    :values (mapv second
                                  (filter #(= repeated (first %)) pairs))}))))
      (cond-> (into {} pairs)
        has-help? (assoc :help true)))))

(defn launcher-throwable-refusal
  "The LAST-RESORT refusal for anything that reaches `-main`'s outermost catch.

   `Throwable`, not `Exception`, and that one word is Opus's round-twenty-one
   item 4. A 10,001-deep nested EDN argument overflowed the reader's stack, and
   a `StackOverflowError` is an `Error`: it walked past `catch Exception` and
   both real launchers published a raw stack trace instead of a typed refusal.
   A caller-controlled argument reaching an untyped stack trace is a refusal no
   enumeration can drive, which is the round-nineteen argument about undeclared
   names one class over.

   `core/max-argument-nesting-depth` now refuses that particular input unread,
   so no argv is expected to arrive here at all — which is exactly why this
   exists. The depth bound is a guess about which `Error` a caller can reach;
   this is the promise that the guess being wrong still leaves a typed,
   bounded exit rather than a stack trace.

   The name is `:invalid-arguments`, the launcher's already-declared generic,
   whenever the `Throwable` carries none of its own: this catch sits at the end
   of a body whose only work is turning argv into a request, so an unnamed
   failure here is a failure about the arguments. An `Error` carries no
   `ex-data` and often no message, so the class name is published when there is
   nothing else to say — the caller needs to know WHAT failed, and the class is
   the tool's own fact, not the caller's value.

   Bounded by `print-launcher-refusal!` like every other launcher refusal; a
   returned map rather than a printed one so a witness can drive it with a real
   `StackOverflowError`, which no argv can produce once the ceiling is in
   place."
  [^Throwable t]
  ;; `ex-data`, not `(instance? clojure.lang.IExceptionInfo t)`: a class
  ;; literal is resolved when this namespace is ANALYSED and babashka's
  ;; runtime does not carry that interface, so the literal took the bb
  ;; launcher out at analysis time — the same lesson `census-adapter-failure`
  ;; already records about `VirtualMachineError`. `ex-data` answers nil for
  ;; anything that carries none, which is the question being asked.
  (let [data (or (ex-data t) {})
        message (.getMessage t)]
    (merge data
           {:error (if (and message (not (str/blank? message)))
                     message
                     (str "the launcher failed with "
                          (.getName (class t))
                          " and no message"))
            :error-type (or (:error-type data) :invalid-arguments)})))

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
                    ;; A launcher refusal like any other: the op name is the
                    ;; caller's own string, so it is bounded at the same one
                    ;; exit rather than printed raw.
                    (print-launcher-refusal!
                      {:error (str "Unknown op: " (:op opts))
                       :error-type :unknown-operation})))

                :else (run opts))))]
      (when (and (map? result) (:error result))
        (System/exit 1)))
    (catch Throwable t
      (print-launcher-refusal! (launcher-throwable-refusal t))
      (System/exit 1))))
