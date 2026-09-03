(ns clj-surgeon.mcp-alias-migration
  "Request boundary for the alias_migration MCP verb.

  This namespace owns the filesystem half — scope expansion, project-root
  confinement, the frozen read, the transaction spec, the durable per-file
  detail file, and the O(1) receipt. It knows nothing about the rewrite itself,
  which lives in the pure `clj-surgeon.alias-migration` planner, and nothing
  about tool registration, which lives in `clj-surgeon.mcp-tool`."
  (:require
   [clj-surgeon.alias-migration :as planner]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.nio.file FileSystems FileVisitOption FileVisitResult Files
                  LinkOption Path PathMatcher SimpleFileVisitor)
   (java.nio.file.attribute FileAttribute)
   (java.util EnumSet UUID)))

(declare unknown-profile?)

(def request-fields
  #{:op :workspace_root :from :to :scope :expect :verify})

(def skipped-directories
  #{".git" ".hg" "target" "node_modules" ".cpcache" ".clj-kondo" ".lsp"
    ".clj-surgeon" ".shadow-cljs" "out"})

;; ---------------------------------------------------------------------------
;; refusals

(defn- refusal
  [error-type message extra]
  (merge {:ok false
          :operation "alias_migration"
          :error_type (name error-type)
          :error message
          :source_unchanged true
          :mutation_attempted false
          :write_authority false
          :next_action "correct_request"}
         extra))

(defn- invalid-request
  [message path]
  (refusal :invalid-mcp-request message
           {:path path
            :remedy (str "Send exactly op, workspace_root, from {lib, var}, "
                         "to {lib, var, alias_policy}, scope {paths}, and "
                         "expect {files}.")}))

;; ---------------------------------------------------------------------------
;; request validation

(defn- nonblank-string?
  [value]
  (and (string? value) (not (str/blank? value))))

;; @spec MCP-OP-ALIAS-002
(defn validate-request
  "Validate one closed alias_migration request without reading source."
  [params]
  (let [unknown (vec (sort (map name (remove request-fields (keys params)))))
        from (:from params)
        to (:to params)
        scope (:scope params)
        expect (:expect params)
        policy (:alias_policy to)]
    (cond
      (seq unknown)
      (invalid-request (str "Unknown alias_migration fields: "
                            (str/join ", " unknown))
                       (vec unknown))

      (not (and (map? from) (nonblank-string? (:lib from))
                (or (nil? (:var from)) (nonblank-string? (:var from)))))
      (invalid-request "from must be {lib, var}; var is a non-blank string or null"
                       ["from"])

      (not (and (map? to) (nonblank-string? (:lib to))
                (or (nil? (:var to)) (nonblank-string? (:var to)))))
      (invalid-request "to must be {lib, var, alias_policy}; var is a non-blank string or null"
                       ["to"])

      (not (contains? #{nil "preserve-refer" "alias-qualify"} (:refer_policy to)))
      (invalid-request
        "to.refer_policy must be preserve-refer (default) or alias-qualify"
        ["to" "refer_policy"])

      (not (and (sequential? policy) (seq policy) (every? nonblank-string? policy)))
      (invalid-request "to.alias_policy must be one non-empty array of names"
                       ["to" "alias_policy"])

      (not (and (map? scope) (sequential? (:paths scope)) (seq (:paths scope))
                (every? nonblank-string? (:paths scope))))
      (invalid-request "scope.paths must be one non-empty array of glob patterns"
                       ["scope" "paths"])

      (and (contains? scope :exclude)
           (not (and (sequential? (:exclude scope))
                     (every? nonblank-string? (:exclude scope)))))
      (invalid-request "scope.exclude must be an array of project-relative paths"
                       ["scope" "exclude"])

      (not (and (map? expect) (integer? (:files expect)) (nat-int? (:files expect))))
      (invalid-request "expect.files must be one non-negative integer"
                       ["expect" "files"])

      (not (or (nil? (:verify params)) (nonblank-string? (:verify params))))
      (invalid-request
        "verify names one configured transaction profile, or is omitted"
        ["verify"])

      :else
      {:ok true
       :request {:workspace-root (:workspace_root params)
                 :verify (:verify params)
                 :from {:lib (:lib from) :var (:var from)}
                 :to {:lib (:lib to) :var (:var to)
                      :alias-policy (vec policy)
                      :refer-policy (or (:refer_policy to) "preserve-refer")}
                 :scope {:paths (vec (:paths scope))
                         :exclude (vec (:exclude scope))}
                 :expect {:files (:files expect)}}})))

;; ---------------------------------------------------------------------------
;; scope expansion

(defn- glob-matcher
  [pattern]
  (.getPathMatcher (FileSystems/getDefault) (str "glob:" pattern)))

(defn- relative-path
  [^Path root ^Path candidate]
  (str/replace (.toString (.relativize root candidate)) "\\" "/"))

(def max-walk-entries
  "Ceiling on the RAW entries one scope walk visits, filtered or not.

  The file ceiling bounds the filtered set, which is what discovery keeps; it
  says nothing about what the walk had to touch to build it. Sixty thousand
  non-source files under scope.paths select nothing and still cost sixty
  thousand visits. This bound is on the work, so it is counted on every visited
  entry — directory, file, and failure alike — and it stops the walk on the
  first entry past it."
  50000)

(def source-file-suffixes
  "The file-name suffixes the scope walk retains a path string for.

  `relative-source-path?` also admits `.edn`, which discovery then drops; the
  walk therefore filters on the file NAME before it materialises any relative
  path, so a tree of non-source files costs no strings at all."
  [".clj" ".cljs" ".cljc"])

(defn- source-file-name?
  [^Path candidate]
  (let [file-name (str (.getFileName candidate))]
    (boolean (some #(str/ends-with? file-name ^String %) source-file-suffixes))))

(def max-unreadable-paths
  "How many unreadable scope paths one refusal names.

  The count is exact; the list is bounded, because a refusal whose length grows
  with the tree is a refusal that cannot be published."
  20)

(def max-scope-depth
  "Segment bound on one scope walk, counted from the project root.

  The walk never follows a symlinked directory, so no link cycle can form; this
  bound is a second, independent stop for a pathologically deep real tree. It is
  a REFUSAL and never a truncation: a file dropped for being deep is a file
  missing from the found count, and the verb's over-declare idiom would then
  launder that omission into a commit that leaves the deep namespace requiring
  the retired lib."
  64)

;; @spec MCP-OP-ALIAS-004
;; @spec MCP-OP-ALIAS-037
;; @spec MCP-OP-ALIAS-048
;; @spec MCP-OP-ALIAS-049
;; @spec MCP-OP-ALIAS-050
(defn scan-scope
  "Every confined project-relative Clojure source under scope.paths, or a scan
  refusal.

  `Files/walkFileTree` is called with an empty option set, so `FOLLOW_LINKS` is
  off: a symlinked directory is reported once and never descended. A link cycle
  inside the root therefore terminates, and a directory link pointing out of the
  root is never entered — the realpath gate downstream then has nothing to
  refuse rather than a whole foreign tree. Build output and version-control
  directories are pruned as whole subtrees rather than filtered afterwards.

  Depth is checked per entry rather than handed to `walkFileTree` as its
  `maxDepth`, because that parameter truncates silently and this bound must be
  observable to the caller.

  Every visitor callback returns TERMINATE once the entry ceiling is crossed,
  read failures included. A ceiling that counts an entry class it will not stop
  on is not a ceiling for that class."
  [^Path root {:keys [paths exclude]}]
  (let [matchers (mapv glob-matcher paths)
        excluded (set exclude)
        default-fs (FileSystems/getDefault)
        found (java.util.ArrayList.)
        too-deep (volatile! nil)
        unreadable (java.util.ArrayList.)
        unreadable-count (volatile! 0)
        visited (volatile! 0)
        over-walk (volatile! nil)
        count-entry!
        (fn []
          (let [seen (vswap! visited inc)]
            (when (> seen max-walk-entries)
              (vreset! over-walk seen))
            seen))
        depth-of (fn [^Path candidate]
                   (.getNameCount (.relativize root ^Path candidate)))
        record-too-deep!
        (fn [^Path candidate depth]
          (vreset! too-deep {:path (relative-path root candidate) :depth depth})
          FileVisitResult/TERMINATE)
        visitor
        (proxy [SimpleFileVisitor] []
          (preVisitDirectory [dir _attrs]
            (let [^Path directory dir
                  depth (depth-of directory)]
              (count-entry!)
              (cond
                @over-walk FileVisitResult/TERMINATE

                (> depth max-scope-depth)
                (record-too-deep! directory depth)

                (and (not (.equals root directory))
                     (contains? skipped-directories
                                (str (.getFileName directory))))
                FileVisitResult/SKIP_SUBTREE

                :else FileVisitResult/CONTINUE)))
          (visitFile [file _attrs]
            (let [^Path candidate file
                  depth (depth-of candidate)]
              (count-entry!)
              (cond
                ;; @spec MCP-OP-ALIAS-050
                @over-walk FileVisitResult/TERMINATE

                (> depth max-scope-depth)
                (record-too-deep! candidate depth)

                :else
                (do
                  ;; the name is tested before any relative path is built, so a
                  ;; tree of non-source files materialises no strings
                  (when (and (source-file-name? candidate)
                             (Files/isRegularFile candidate
                                                  (make-array LinkOption 0)))
                    (let [relative (relative-path root candidate)]
                      (when (and (mcp-paths/relative-source-path? relative)
                                 (not (contains? excluded relative))
                                 (some #(.matches ^PathMatcher %
                                                  (.getPath default-fs relative
                                                            (make-array String 0)))
                                       matchers))
                        (.add found relative))))
                  FileVisitResult/CONTINUE))))
          ;; @spec MCP-OP-ALIAS-049
          (visitFileFailed [file _error]
            (count-entry!)
            (let [^Path candidate file]
              ;; a subtree the walk would have pruned anyway is not in scope,
              ;; so its unreadability says nothing about the scope
              (when-not (contains? skipped-directories
                                   (str (.getFileName candidate)))
                (vswap! unreadable-count inc)
                (when (< (.size unreadable) max-unreadable-paths)
                  (.add unreadable (relative-path root candidate)))))
            ;; @spec MCP-OP-ALIAS-050
            ;; the ceiling counts read failures, so it has to STOP on them too;
            ;; a tree whose entries are unreadable is exactly the tree that
            ;; walks furthest past a bound that only the other callbacks honour
            (if @over-walk FileVisitResult/TERMINATE FileVisitResult/CONTINUE))
          ;; @spec MCP-OP-ALIAS-050
          (postVisitDirectory [_dir _error]
            (if @over-walk FileVisitResult/TERMINATE FileVisitResult/CONTINUE)))]
    (Files/walkFileTree root
                        (EnumSet/noneOf FileVisitOption)
                        Integer/MAX_VALUE
                        visitor)
    (cond
      ;; @spec MCP-OP-ALIAS-050
      @over-walk
      {:ok false
       :error-type :alias-migration-walk-too-large
       :visited-entries @over-walk
       :max-entries max-walk-entries}

      @too-deep
      (let [deep @too-deep]
        {:ok false
         :error-type :alias-migration-scope-too-deep
         :path (:path deep)
         :depth (:depth deep)
         :max-depth max-scope-depth})

      ;; @spec MCP-OP-ALIAS-049
      (pos? @unreadable-count)
      {:ok false
       :error-type :alias-migration-scope-unreadable
       :unreadable-paths (vec unreadable)
       :unreadable-count @unreadable-count}

      :else {:ok true :files (vec (sort found))})))

;; @spec MCP-OP-ALIAS-004
(defn expand-scope
  "The sources one scope selects, when the bounded scan admits it.

  `scan-scope` is the entrance that carries the scan's typed refusals; this is
  the projection callers that only need the file list use."
  [^Path root scope]
  (vec (:files (scan-scope root scope))))

;; ---------------------------------------------------------------------------
;; planning

(def max-scope-files
  "Ceiling on the number of source files one alias_migration call reads.

  The frozen read holds every scoped source in memory at once, so the scope is
  bounded before the first slurp rather than after the plan. A migration whose
  scope selects more than this many files is a scope the caller has not yet
  narrowed; the refusal names both the count found and the expect.files the
  request declared, which is what a caller needs to narrow it."
  2000)

(def max-source-bytes
  "Ceiling on the size of one source file alias_migration reads.

  Two mebibytes is far above any hand-written Clojure namespace; a file past it
  is a generated or vendored artifact that the frozen read must not hold."
  (* 2 1024 1024))

(def max-scope-bytes
  "Ceiling on the TOTAL bytes of source one alias_migration call holds at once.

  The file ceiling and the per-file byte ceiling do not bound their product: a
  scope of four hundred and fifty files of one and nine tenths mebibytes each
  passes both and is still eight hundred and fifty megabytes the frozen read
  would try to hold on a five-hundred-and-twelve-mebibyte heap. This third bound
  is the one that names the resource actually spent, and it is accumulated from
  the filesystem's recorded sizes BEFORE the first slurp, so a scope that cannot
  fit is a typed refusal rather than an OutOfMemoryError."
  (* 256 1024 1024))

(defn- sized-sources
  "Every resolution carrying its size in bytes, measured without reading it.

  `Files/size` reads the directory entry, so the whole scope is weighed for far
  less than the cost of holding one file of it."
  [resolutions]
  (mapv (fn [{:keys [path] :as resolution}]
          (assoc resolution
                 :bytes (Files/size (.toPath (io/file ^String path)))))
        resolutions))

;; @spec MCP-OP-ALIAS-055
(defn- subtree-aggregates
  "What every ancestor directory of the selected sources holds.

  One pass over the walk's own output rather than a second traversal. Only
  directory prefixes become keys: a file is never a narrowing target."
  [entries]
  (persistent!
    (reduce
      (fn [acc [^String relative bytes]]
        (let [segments (str/split relative #"/")
              last-index (dec (count segments))]
          (loop [acc acc index 0 prefix nil]
            (if (>= index last-index)
              acc
              (let [prefix (if prefix
                             (str prefix "/" (nth segments index))
                             (nth segments index))
                    seen (get acc prefix)]
                (recur (assoc! acc prefix
                               {:files (inc (long (:files seen 0)))
                                :bytes (+ (long (:bytes seen 0)) (long bytes))})
                       (inc index)
                       prefix))))))
      (transient {})
      entries)))

;; @spec MCP-OP-ALIAS-055
(defn- narrowing-prefix
  "The LARGEST subtree that fits under one aggregate ceiling, or nil.

  Largest, because the narrowing exists to cover as much of the caller's scope
  as one legal call can; the smallest fitting subtree is executable and
  useless. Ties go to the deepest prefix — the most specific name for the same
  set — and then to the lexicographically first, so the refusal is a function
  of the tree and not of the order the walk happened to visit it in.

  Returns `[prefix {:files n :bytes b}]`."
  [aggregates measure ceiling]
  (->> aggregates
       (filter (fn [[_ totals]]
                 (and (pos? (long (:files totals 0)))
                      (<= (long (get totals measure 0)) ceiling))))
       (sort-by (fn [[prefix totals]]
                  [(- (long (get totals measure 0)))
                   (- (count (str/split prefix #"/")))
                   prefix]))
       first))

;; @spec MCP-OP-ALIAS-006
;; @spec MCP-OP-ALIAS-012
;; @spec MCP-OP-ALIAS-038
;; @spec MCP-OP-ALIAS-039
;; @spec MCP-OP-ALIAS-046
;; @spec MCP-OP-ALIAS-048
;; @spec MCP-OP-ALIAS-049
;; @spec MCP-OP-ALIAS-050
;; @spec MCP-OP-ALIAS-051
(defn plan!
  "Expand scope, confine every path, bound the read, freeze the sources, and plan.

  Returns {:ok true :plan p :root r :paths {relative absolute}} or one typed
  refusal. No bytes are written, and both read bounds are enforced before the
  first file is slurped. `expect.files` is deliberately NOT one of them: an
  over-declared expectation is the verb's self-correcting field idiom, and it
  must reach discovery to earn a found count and an executable next_call."
  [workspace-root request]
  (let [root (mcp-paths/real-root workspace-root)
        scan (scan-scope root (:scope request))
        relatives (:files scan)
        scanned (count relatives)
        expected (get-in request [:expect :files])]
    (cond
      ;; @spec MCP-OP-ALIAS-048
      (= :alias-migration-scope-too-deep (:error-type scan))
      (refusal :alias-migration-scope-too-deep
               (str (:path scan) " is " (:depth scan)
                    " path segments below the project root, past the "
                    max-scope-depth "-segment bound one alias_migration walks")
               {:path (:path scan)
                :depth (:depth scan)
                :max_depth (:max-depth scan)
                :next_call nil
                :remedy (str "Narrow scope.paths so the walk does not reach "
                             (:path scan) ", or flatten that tree; the bound is "
                             "refused rather than truncated so no file can "
                             "silently leave the found count.")})

      ;; @spec MCP-OP-ALIAS-050
      (= :alias-migration-walk-too-large (:error-type scan))
      (refusal :alias-migration-walk-too-large
               (str "scope.paths walks more than " max-walk-entries
                    " filesystem entries; the walk stopped at "
                    (:visited-entries scan))
               {:visited_entries (:visited-entries scan)
                :max_entries (:max-entries scan)
                :next_call nil
                :remedy (str "Narrow scope.paths to the source directories "
                             "that can require from.lib; this bound is on the "
                             "entries walked, not on the sources selected, so "
                             "a directory of non-source files still costs it.")})

      ;; @spec MCP-OP-ALIAS-049
      (= :alias-migration-scope-unreadable (:error-type scan))
      (refusal :alias-migration-scope-unreadable
               (str (:unreadable-count scan)
                    " path(s) under scope.paths could not be read, so what the"
                    " scope contains is not knowable: "
                    (str/join ", " (:unreadable-paths scan)))
               {:unreadable_paths (:unreadable-paths scan)
                :unreadable_count (:unreadable-count scan)
                :next_call nil
                :remedy (str "Make those paths readable, or exclude them "
                             "through scope.paths, and resend. Continuing past "
                             "them would silently shrink the scope and hand "
                             "back a found count that omits whatever they hold.")})

      (> scanned max-scope-files)
      ;; @spec MCP-OP-ALIAS-055
      (let [[prefix totals] (narrowing-prefix
                              (subtree-aggregates
                                (map (fn [relative] [relative 0]) relatives))
                              :files max-scope-files)]
        (refusal :alias-migration-scope-too-large
                 (str "scope.paths selects " scanned " files, above the "
                      max-scope-files "-file ceiling one alias_migration reads")
                 (cond-> {:scanned_files scanned
                          :max_files max-scope-files
                          :expected_files expected
                          :next_call (when prefix
                                       (planner/narrowing-call request prefix))
                          :expect_files_unchanged_reason
                          planner/expect-files-unchanged-reason
                          :remedy
                          (if prefix
                            (str "Resend the next_call: it narrows scope.paths "
                                 "to " prefix "/**, the largest subtree of this "
                                 "scope that fits the ceiling. expect.files "
                                 "declared " expected " and is left as declared.")
                            (str "Narrow scope.paths to the namespaces that can "
                                 "require from.lib; expect.files declared "
                                 expected ". No single subtree of this scope "
                                 "fits the ceiling, so only the caller can "
                                 "choose the narrowing."))}
                   prefix (assoc :would_select_files (long (:files totals))))))

      :else
      (let [resolutions (mapv #(mcp-paths/resolve-source-path root %) relatives)
            bad (first (remove :ok resolutions))
            sized (when-not bad (sized-sources resolutions))
            oversized (first (filter #(> (:bytes %) max-source-bytes) sized))
            scope-bytes (reduce + 0 (map :bytes sized))
            ;; @spec MCP-OP-ALIAS-055
            [byte-prefix byte-totals] (when (and (not bad)
                                                 (> scope-bytes max-scope-bytes))
                                        (narrowing-prefix
                                          (subtree-aggregates
                                            (map (juxt :relative :bytes) sized))
                                          :bytes max-scope-bytes))]
        (cond
          ;; @spec MCP-OP-ALIAS-051
          bad
          (refusal :alias-migration-scope-path-refused
                   (or (:error bad) "A scope path is outside the configured project root")
                   {:path (:path bad)
                    ;; the file was refused before it was opened, so whether it
                    ;; requires from.lib is not known and expect.files stands
                    :next_call (planner/excluding-call request (:path bad) :unknown)
                    :expect_files_unchanged_reason
                    planner/expect-files-unchanged-reason
                    :remedy (str "Exclude " (:path bad) " through scope.exclude"
                                 " and resend; the next_call already does.")})

          oversized
          (refusal :alias-migration-source-too-large
                   (str (:relative oversized) " is larger than the "
                        max-source-bytes "-byte ceiling one alias_migration reads")
                   {:path (:relative oversized)
                    :bytes (:bytes oversized)
                    :max_bytes max-source-bytes
                    ;; @spec MCP-OP-ALIAS-051
                    :next_call (planner/excluding-call request
                                                       (:relative oversized)
                                                       :unknown)
                    :expect_files_unchanged_reason
                    planner/expect-files-unchanged-reason
                    :remedy (str "Exclude " (:relative oversized)
                                 " through scope.exclude, or narrow scope.paths;"
                                 " the next_call already excludes it.")})

          ;; @spec MCP-OP-ALIAS-046
          (> scope-bytes max-scope-bytes)
          (refusal :alias-migration-scope-too-large-bytes
                   (str "scope.paths selects " scope-bytes
                        " bytes of source across " (count sized)
                        " files, above the " max-scope-bytes
                        "-byte ceiling one alias_migration holds at once")
                   (cond-> {:scope_bytes scope-bytes
                            :max_bytes max-scope-bytes
                            :scanned_files (count sized)
                            :expected_files expected
                            ;; no bounded EXCLUSION exists: every file is under
                            ;; the per-file ceiling, so bringing an over-large
                            ;; scope under this one takes at least a hundred and
                            ;; twenty-nine exclusions. A bounded NARROWING does:
                            ;; one prefix replaces scope.paths outright and is
                            ;; constant in the size of the tree.
                            ;; @spec MCP-OP-ALIAS-055
                            :next_call (when byte-prefix
                                         (planner/narrowing-call request
                                                                 byte-prefix))
                            :expect_files_unchanged_reason
                            planner/expect-files-unchanged-reason
                            :remedy
                            (if byte-prefix
                              (str "Resend the next_call: it narrows scope.paths"
                                   " to " byte-prefix "/**, the largest subtree "
                                   "of this scope that fits the aggregate byte "
                                   "ceiling.")
                              (str "Narrow scope.paths; no bounded set of "
                                   "scope.exclude entries can bring this scope "
                                   "under the aggregate ceiling, and no single "
                                   "subtree of it fits either."))}
                     byte-prefix
                     (assoc :would_select_files (long (:files byte-totals))
                            :would_select_bytes (long (:bytes byte-totals)))))

          :else
          (let [sources (mapv (fn [{:keys [relative path]}]
                                {:file relative :source (slurp path)})
                              sized)
                plan (planner/plan (assoc request :workspace-root
                                          (.toString root))
                                   sources)]
            (cond
              (not (:ok plan))
              (assoc plan :scanned_files (count sources))

              ;; the lib-only migration also moves the defining namespace, so
              ;; its destination is confined and proved absent before anything
              ;; is written
              (:lib-rename plan)
              (let [{:keys [new-file file]} (:lib-rename plan)
                    destination (mcp-paths/resolve-new-source-path root new-file)]
                (if-not (:ok destination)
                  (if (= "target-already-exists" (:error_type destination))
                    (refusal :alias-migration-target-lib-exists
                             (str new-file " already exists, so "
                                  (:to (:lib-rename plan))
                                  " cannot take over the definition in " file)
                             {:file new-file
                              :from_lib (:from (:lib-rename plan))
                              :to_lib (:to (:lib-rename plan))
                              :defining_file file
                              :next_call nil})
                    (refusal :alias-migration-scope-path-refused
                             (or (:error destination)
                                 "The renamed namespace path is refused")
                             {:path new-file :next_call nil}))
                  {:ok true
                   :plan plan
                   :root root
                   :scanned-files (count sources)
                   :destination destination
                   :paths (into {} (map (juxt :relative :path)) resolutions)}))

              :else
              {:ok true
               :plan plan
               :root root
               :scanned-files (count sources)
               :paths (into {} (map (juxt :relative :path)) resolutions)})))))))

;; ---------------------------------------------------------------------------
;; transaction spec

;; @spec MCP-OP-ALIAS-016
;; @spec MCP-OP-ALIAS-018
(defn plan->spec
  "Lower one plan into a single failure-atomic transaction spec.

  Every change's `find` is the exact original bytes of one complete top-level
  form. A file that drifted between the frozen read and commit no longer
  matches, so the whole transaction refuses before any write. A lib-only
  migration also carries the renamed defining namespace as one created file, so
  the new namespace and every rewritten caller land or refuse together."
  ([plan paths] (plan->spec plan paths nil))
  ([plan paths destination]
  (let [changes
        (vec (mapcat
               (fn [file-index {:keys [file edits]}]
                 (map-indexed
                   (fn [edit-index {:keys [kind original replacement]}]
                     {:id (keyword "alias-migration"
                                   (str "f" file-index "-e" edit-index
                                        "-" (name kind)))
                      :in [(get paths file)]
                      :find original
                      :do [:replace replacement]
                      :expect {:matches 1}})
                   edits))
               (range) (:files plan)))]
    (cond-> {:changes changes
             :expect {:changes (count changes)
                      :edits (count changes)
                      :files (count (:files plan))}}
      destination
      (assoc :create-files
             [{:file (:path destination)
               :content (get-in plan [:lib-rename :content])
               :directories (mapv str (:missing-parent-directories destination))}])))))

;; ---------------------------------------------------------------------------
;; durable per-file detail

(def max-detail-files
  "How many alias_migration detail documents one workspace keeps.

  Every call writes one per-file detail document under
  `.clj-surgeon/alias-migration/`. They are diagnostic, not transactional — the
  undo receipt is the durable artefact — so the writer retains the most recent
  twenty OF ITS OWN, the run's own document always among them, and deletes the
  rest. The bound counts only documents this writer can prove it wrote; a
  directory a caller has filled with files of its own can hold any number of
  them and this writer will not touch one."
  20)

;; @spec MCP-OP-ALIAS-052
(def details-retention
  "How durable a published `details_path` is, in one word the receipt carries.

  BEST-EFFORT, and deliberately so. The bound above is per writer, not per
  caller: twenty concurrent migrations in one workspace publish twenty-one
  documents between them, all of them recorded in the writer's shared manifest,
  and the twenty-first prunes a path a peer's receipt named seconds earlier.
  Each run protects its OWN document and nothing else. The bound is best-effort
  in the same breath: two runs that read the manifest at once and write it back
  in turn lose one another's entries, and a document dropped from the manifest
  is never deleted again, so the directory can hold more than the bound.

  Protecting peers was considered and rejected. An index of recently published
  paths is read-modify-written by every call, so it carries the identical race
  it would be fixing; making it correct needs a lock file on the hot path of
  every migration. That is a durable, contended, failure-prone mechanism spent
  on a diagnostic document the caller is told to read from the receipt it was
  just handed, while the artefact that actually has to survive — the undo
  receipt — is already transactional and untouched by this pruning.

  So the receipt says the true thing instead: retention is best-effort, the
  bound is published alongside it, and a caller who needs the detail should
  read it now rather than assume the path keeps."
  "best-effort")

;; @spec MCP-OP-ALIAS-045
;; @spec MCP-OP-ALIAS-054
(def detail-document-prefix
  "The name prefix every `alias_migration` detail document carries.

  Retention has to delete files, and a deleter that cannot name what it owns
  will eventually delete something it does not. The prefix is that name: it is
  the only thing `prune-details!` will remove, so any other document sharing
  the directory — an undo receipt written there because the caller configured
  `receipt-dir` to this path, a peer tool's notes, a human's scratch file — is
  outside retention's reach by construction rather than by luck."
  "detail-")

;; @spec MCP-OP-ALIAS-054
(defn detail-document-name?
  "Whether one file name is inside the detail writer's own name namespace.

  A NAME test, and only that. It answers the question the receipt/detail
  collision guard asks — could this name be one the detail writer generates —
  and it is deliberately NOT the ownership test: a caller may create a file
  called `detail-anything.edn` for its own reasons, and a name it happens to
  share with us is not consent to delete it. See `detail-document-owned?`."
  [^String file-name]
  (boolean (and file-name
                (str/starts-with? file-name detail-document-prefix)
                (str/ends-with? file-name ".edn"))))

;; @spec MCP-OP-ALIAS-054
(def detail-writer-marker
  "The value every detail document carries under a top-level `:writer` key.

  Prefix matching is not proof of ownership. The prefix says a name COULD be
  ours; this marker, written inside the document by the only code that writes
  one, says it IS. Retention deletes a file only when the marker is in it and
  the name is in the manifest below — two independent facts, both produced by
  this writer, neither of them a guess about a stranger's file."
  "clj-surgeon.alias-migration")

;; @spec MCP-OP-ALIAS-054
(def detail-manifest-name
  "The manifest naming the detail documents this writer has written.

  It lives in the detail directory, inside the writer's own name namespace so
  that the collision guard covers it too, and it is the writer's record of what
  it owns. A document absent from it is never a pruning candidate however its
  name reads, which is what makes a caller's own `detail-*.edn` files safe."
  "detail-manifest.edn")

(def ^:private max-detail-document-bytes
  "How large a document retention will read to check its ownership marker.

  Reading is bounded because an unbounded read on the pruning path is a heap
  risk in the one place that must not fail. A document past the bound is not
  proved ours, so it is not deleted — the bound fails toward keeping files."
  (* 8 1024 1024))

;; @spec MCP-OP-ALIAS-054
(defn- detail-manifest-entry?
  "Whether one manifest entry names a file this writer could have written.

  The manifest is a file on disk and therefore editable; an entry is only ever
  a bare document name in this directory, never a path, never the manifest."
  [entry]
  (boolean (and (string? entry)
                (detail-document-name? entry)
                (not= detail-manifest-name entry)
                (= entry (.getName (io/file ^String entry))))))

;; @spec MCP-OP-ALIAS-054
(defn- document-run-id
  "The run id a detail document's own name encodes, or nil.

  One place computes it, so the id a document is WRITTEN with and the id
  retention reads it BACK by can never drift apart."
  [^String file-name]
  (when (detail-document-name? file-name)
    (subs file-name (count detail-document-prefix) (- (count file-name) 4))))

;; @spec MCP-OP-ALIAS-054
(defn- detail-document-owned?
  "Whether one file on disk carries this writer's ownership marker.

  The marker says the bytes were written by this writer; the `:run-id` says
  they are the bytes written for THIS file. A document copied over another —
  a backup restored on top of it, a caller duplicating a file it read — keeps
  the marker and gains a run id that no longer names the file it sits in, and
  that is enough to stop deleting it. It is defence against ACCIDENTAL
  replacement only: a forger who writes the marker writes the run id too, and
  nothing on a cooperative local filesystem stops that."
  [^java.io.File file]
  (boolean
    (and (.isFile file)
         (<= (.length file) max-detail-document-bytes)
         (try
           (let [document (edn/read-string (slurp file))]
             (and (= detail-writer-marker (:writer document))
                  (= (document-run-id (.getName file)) (:run-id document))))
           (catch Exception _ false)))))

;; @spec MCP-OP-ALIAS-054
(defn- read-detail-manifest
  "The document names this writer has recorded, or none."
  [^java.io.File directory]
  (let [file (io/file directory detail-manifest-name)]
    (if-not (and (.isFile file) (<= (.length file) max-detail-document-bytes))
      []
      (try
        (let [document (edn/read-string (slurp file))]
          (if (= detail-writer-marker (:writer document))
            (into [] (comp (filter detail-manifest-entry?) (distinct))
                  (:documents document))
            []))
        (catch Exception _ [])))))

;; @spec MCP-OP-ALIAS-054
(defn- write-detail-manifest!
  "Record exactly the documents this writer still owns in this directory."
  [^java.io.File directory names]
  (file-ops/atomic-write!
    (.getPath (io/file directory detail-manifest-name))
    (pr-str {:version 1
             :writer detail-writer-marker
             :documents (vec (sort names))})))

;; @spec MCP-OP-ALIAS-054
(defn detail-directory
  "The directory `alias_migration` publishes its per-run detail documents in."
  ^java.io.File [project-root]
  (io/file (io/file (str project-root)) ".clj-surgeon" "alias-migration"))

;; @spec MCP-OP-ALIAS-054
(defn- resolved-path
  "One path's canonical identity, resolving symlinks, creating nothing.

  Textual absolute-path equality is not directory identity: a symlink and the
  directory it points at are one directory and compare unequal as strings, so a
  guard written on strings misses exactly the aliasing it exists to catch.
  `toRealPath` needs the path to exist, and a receipt directory the caller has
  configured but nothing has created yet does not — so the nearest EXISTING
  ancestor is resolved and the remainder appended to it. Nothing here calls
  mkdirs: the guard has to be decidable before the first write, not after it.

  The remainder is CALLER TEXT and is normalised before it is appended: the
  ancestor is a real path, but `missing/../.clj-surgeon/alias-migration` IS the
  detail directory and appended raw it compares unequal to it, which is a
  bypass of the guard rather than a curiosity. Normalisation is lexical and so
  is only sound below a canonical ancestor: a remainder that still climbs after
  it names a directory above the one whose identity was proved, and this
  returns nil for that rather than guess. nil is `receipt-dir-escapes` at the
  boundary — an undecidable identity is a refusal, never a pass."
  ^Path [path]
  (let [absolute (.toPath (.getAbsoluteFile (io/file (str path))))]
    (loop [candidate absolute
           remainder []]
      (if (nil? candidate)
        (.normalize absolute)
        (let [real (try
                     (when (Files/exists candidate (make-array LinkOption 0))
                       (.toRealPath candidate (make-array LinkOption 0)))
                     (catch java.io.IOException _ nil)
                     (catch SecurityException _ nil))]
          (if real
            (if (empty? remainder)
              real
              (let [tail (.normalize
                           ^Path (reduce (fn [^Path acc ^Path segment]
                                           (.resolve acc segment))
                                         (first remainder) (rest remainder)))]
                (when-not (some (fn [^Path segment] (= ".." (str segment))) tail)
                  (.normalize (.resolve real tail)))))
            (recur (.getParent candidate)
                   (into [(.getFileName candidate)] remainder))))))))

;; @spec MCP-OP-ALIAS-054
(defn receipt-dir-escapes?
  "Whether the configured receipt directory has no decidable identity.

  It has none when the part of it that does not exist yet still climbs above
  the nearest existing ancestor after normalisation: lexical `..` past a
  canonical path is not the same directory the kernel would open, and the
  guard below cannot answer honestly about a path it cannot name. The boundary
  refuses instead, before anything is created."
  [receipt-dir]
  (nil? (resolved-path receipt-dir)))

;; @spec MCP-OP-ALIAS-054
(defn receipt-detail-collision?
  "Whether a receipt written under `receipt-name` would land in the detail
  writer's own name namespace.

  Co-locating the two directories is legal and stays legal: the prefix above
  keeps the namespaces disjoint, so this predicate is false for every name the
  two actually generate. It exists so that a future change to either naming
  scheme fails as a typed refusal at the boundary rather than as a silently
  deleted undo receipt in the field.

  Directory sameness is CANONICAL, not textual, so a receipt directory that is
  a symlink to the detail directory is the same directory here — and the whole
  predicate is answerable without creating either one, which is what lets the
  refusal fire before any mkdirs. It is asked TWICE: once on the configured
  path before anything exists, and once on the directory that was actually
  created, because a path's identity is not settled until it exists.

  Sameness is containment, not equality: a receipt directory that resolves
  INSIDE the detail directory is in the detail writer's own subtree under a
  name its retention owns, and the guard is not made honest by a directory
  level. An unresolvable path is not a match here — `receipt-dir-escapes?`
  refuses it first, so this never has to guess."
  [project-root receipt-dir ^String receipt-name]
  (let [receipt (resolved-path receipt-dir)
        detail (resolved-path (detail-directory project-root))]
    (boolean (and (detail-document-name? receipt-name)
                  receipt
                  detail
                  (.startsWith receipt detail)))))

;; @spec MCP-OP-ALIAS-054
(defn- create-receipt-directory!
  "Create the receipt directory, returning what THIS call brought into being.

  The return is the directories that did not exist a moment ago, deepest
  first, so a refusal that follows can remove exactly what this run made and
  nothing a caller or a peer already had. `File/delete` never removes a
  non-empty directory, so a peer that filled one in between keeps it.

  Creation goes through `Files/createDirectories`, which fails loudly, but only
  when the path is not already a directory: a receipt directory that is a
  symlink to a real directory is a legal configuration `createDirectories`
  would reject outright."
  [receipt-dir]
  (let [file (io/file (str receipt-dir))
        path (.toPath (.getAbsoluteFile file))
        created (loop [candidate path
                       acc []]
                  (if (or (nil? candidate)
                          (Files/exists candidate (make-array LinkOption 0)))
                    acc
                    (recur (.getParent candidate) (conj acc candidate))))]
    (when-not (.isDirectory file)
      (try
        (Files/createDirectories path (make-array FileAttribute 0))
        (catch java.io.IOException _ nil)
        (catch SecurityException _ nil)))
    created))

;; @spec MCP-OP-ALIAS-045
;; @spec MCP-OP-ALIAS-054
(defn- prune-details!
  "Delete all but the most recent `max-detail-files` documents THIS WRITER WROTE.

  `keep` is the document this run just wrote; it is retained regardless of how
  the filesystem timestamps compare, so a run can never discard its own
  receipt's `details_path`.

  A candidate has to be PROVED ours, twice over: its name is in the manifest
  this writer maintains in the directory, AND the file on disk carries
  `detail-writer-marker` under a top-level `:writer` key. A name prefix alone is
  not proof of ownership — a caller is free to keep its own `detail-*.edn`
  files here, and a name we happen to share with it is not consent to delete
  it. So a manifest entry whose file lacks the marker is left alone (the file
  is no longer the one we wrote), and a marked file we never recorded is left
  alone too (we cannot show we wrote it). The directory is never listed for
  candidates; the manifest is the only source of them.

  Never the `retired/` subtree, which is transactional, and never the manifest
  itself. A caller may point `receipt-dir` at this same directory; an undo
  receipt sitting here is not retention's to delete, and deleting one would
  publish a committed receipt naming an inverse that no longer exists.

  It protects no PEER's detail document. Peers share this manifest, so a
  concurrent peer's freshly published path is an ordinary candidate here and
  can be deleted; `details-retention` says why that is the chosen behaviour and
  the receipt publishes the word."
  [^java.io.File directory ^String keep]
  (let [owned (->> (conj (vec (remove #{keep} (read-detail-manifest directory)))
                         keep)
                   (map (fn [^String name] (io/file directory name)))
                   (filterv detail-document-owned?))
        candidates (->> owned
                        (remove (fn [^java.io.File file]
                                  (= keep (.getName file))))
                        (sort-by (juxt (fn [^java.io.File file]
                                         (- (.lastModified file)))
                                       (fn [^java.io.File file]
                                         (.getName file)))))
        stale (vec (drop (dec max-detail-files) candidates))
        deleted (into #{} (map (fn [^java.io.File file] (.getName file))) stale)]
    (doseq [^java.io.File file stale]
      (.delete file))
    (write-detail-manifest!
      directory
      (into [] (comp (map (fn [^java.io.File file] (.getName file)))
                     (remove deleted))
            owned))))

;; @spec MCP-OP-ALIAS-020
;; @spec MCP-OP-ALIAS-045
(defn write-details!
  "Write per-file detail outside the receipt and return its relative path."
  [^Path root plan]
  (let [directory (io/file (.toFile root) ".clj-surgeon" "alias-migration")
        file-name (str detail-document-prefix (UUID/randomUUID) ".edn")
        target (io/file directory file-name)]
    (.mkdirs directory)
    (file-ops/atomic-write!
      (.getPath target)
      (pr-str (cond-> {:version 1
                       ;; @spec MCP-OP-ALIAS-054
                       ;; the ownership marker retention reads back: written by
                       ;; the only code that writes one of these documents
                       :writer detail-writer-marker
                       ;; @spec MCP-OP-ALIAS-054
                       ;; the id retention reads back to prove these bytes are
                       ;; the ones written for THIS file
                       :run-id (document-run-id file-name)
                       :files (mapv (fn [entry]
                                      (-> entry
                                          (select-keys [:file :alias :collided :sites
                                                        :refer-sites :require-mode])
                                          (update :collided vec)))
                                    (:files plan))}
                (:lib-rename plan)
                (assoc :lib-rename (dissoc (:lib-rename plan) :content)))))
    (prune-details! directory file-name)
    (str ".clj-surgeon/alias-migration/" file-name)))

;; ---------------------------------------------------------------------------
;; the receipt

(defn- verification-summary
  [verification requested]
  (if-not verification
    (let [status (if requested "not-configured" "not-requested")]
      {:kondo_delta {:status status}
       :focused_test {:status status}})
    (let [checks (:checks verification)
          deltas (keep :diagnostic-delta checks)]
      {:kondo_delta
       (if (seq deltas)
         {:status "compared"
          :introduced (reduce + 0 (map :introduced-count deltas))
          :removed (reduce + 0 (map :removed-count deltas))
          :blocking_introduced (reduce + 0 (map :blocking-introduced-count deltas))}
         {:status "not-configured"})
       :focused_test
       {:status (if (:ok verification) "pass" "fail")
        :profile (str (:profile verification))
        :ok (boolean (:ok verification))
        :checks (count checks)}})))

;; @spec MCP-OP-ALIAS-019
;; @spec MCP-OP-ALIAS-020
;; @spec MCP-OP-ALIAS-026
(defn- lib-renamed-summary
  "One constant-size record of the defining namespace's move, or nil."
  [plan commit]
  (when-let [{:keys [from to file new-file]} (:lib-rename plan)]
    {:from from
     :to to
     :file file
     :new_file new-file
     :retired_to (:retired-file commit)}))

;; @spec MCP-OP-ALIAS-042
(defn receipt
  "Render one receipt whose length is constant in the number of namespaces.

  `:ok` and `:committed` are the kernel's own computed `:committed`, never a
  literal: the tool tells agents its receipt is terminal evidence and not to
  re-read the files, so a claim of commit that the kernel did not make would be
  unfalsifiable at the caller."
  [plan commit details-path]
  (let [totals (:totals plan)
        committed? (true? (:committed commit))]
    (merge
      {:ok committed?
       :operation "alias_migration"
       :committed committed?
       :files (:files totals)
       :sites (:sites totals)
       :refer_sites (:refer-sites totals)
       :alias_histogram (into {} (:alias-histogram totals))
       :collisions_resolved (:collisions-resolved totals)
       :string_mentions (count (:string-mentions totals))
       :lib_renamed (lib-renamed-summary plan commit)
       :details_path details-path
       ;; @spec MCP-OP-ALIAS-052
       :details_retention details-retention
       :details_retained max-detail-files
       :next_action (if committed? "none" "review_receipt")}
      (verification-summary (:verification commit) (:verify-requested commit))
      (select-keys commit [:undo_receipt :receipt_hash]))))

(defn commit-refusal
  "Translate one kernel refusal into the verb's typed public refusal."
  [plan commit]
  (refusal (or (some-> (or (:error-type commit) (:error_type commit)) name)
               "alias-migration-transaction-refused")
           (or (:error commit) "The alias migration transaction refused")
           (cond-> {:files (get-in plan [:totals :files])
                    :sites (get-in plan [:totals :sites])
                    :source_unchanged (boolean
                                        (or (:source-unchanged commit)
                                            (:source_unchanged commit)
                                            (not (:committed commit))))
                    :remedy (or (:remedy commit)
                                (str "Re-send the same alias_migration request;"
                                     " the frozen snapshot is recomputed from"
                                     " current source."))}
             (:change-id commit) (assoc :change_id (str (:change-id commit)))
             ;; @spec MCP-OP-ALIAS-054
             ;; a guard asked before and after the directory exists has two
             ;; answers, and the caller is told which one refused it
             (:phase commit) (assoc :phase (:phase commit)))))

(defn declared-file-set
  "The project-relative files one plan will change."
  [plan]
  (into (sorted-set) (map :file) (:files plan)))

(defn unexpected-files
  "Files a plan would change that are not in the declared expectation."
  [plan expected]
  (set/difference (declared-file-set plan) (set expected)))

;; ---------------------------------------------------------------------------
;; the write, through the shared transaction kernel

;; @spec MCP-OP-ALIAS-054
(defn new-receipt-name
  "The file name one alias migration publishes its undo receipt under."
  []
  (str (UUID/randomUUID) ".edn"))

;; @spec MCP-OP-ALIAS-028
(defn selected-profile
  "The profile this request asked for, or nil.

  Verification is OPT-IN, exactly as it is for the other public write tools,
  whose contract reads: omit verify unless the user or repository explicitly
  requests a configured transaction profile. Auto-selecting the workspace's default profile would
  make every migration depend on whatever that profile shells out to — the
  built-in `fast` profile runs `npx @chrisoakman/standard-clojure-style`, so a
  correct migration would roll back on a machine with no npx or no network, and
  every call would pay that wall time."
  [verification-profiles verify]
  (when (and verify (contains? verification-profiles verify))
    verify))

(defn unknown-profile?
  [verification-profiles verify]
  (and verify (not (contains? verification-profiles verify))))

;; @spec MCP-OP-ALIAS-016
;; @spec MCP-OP-ALIAS-017
;; @spec MCP-OP-ALIAS-022
;; @spec MCP-OP-ALIAS-044
(defn retire-relative-path
  "Where the superseded defining file is kept, as a project-relative path."
  [relative]
  (str ".clj-surgeon/alias-migration/retired/" relative))

(defn retire-path
  "Where the superseded defining file is kept so the move stays reversible."
  [project-root relative]
  (str (io/file project-root (retire-relative-path relative))))

;; @spec MCP-OP-ALIAS-041
(defn resolve-retire-source
  "The real, root-confined path of the defining file a lib migration supersedes.

  The transaction's edits addressed the canonical path `resolve-source-path`
  returned, so the retire must move that same path. A symbolic link at the
  defining path is a typed refusal rather than a guess between two wrong moves:
  retiring the link leaves the real definition in the tree under a name nothing
  requires, and retiring its target retires a file the request never named."
  [project-root relative]
  (let [root (mcp-paths/real-root project-root)
        lexical (.normalize (.resolve root ^String relative))]
    (if (Files/isSymbolicLink lexical)
      {:ok false
       :error-type :alias-migration-retire-symlink-refused
       :error (str relative " is a symbolic link, so the superseded defining"
                   " file cannot be retired reversibly")}
      (let [resolved (mcp-paths/resolve-source-path root relative)]
        (if (:ok resolved)
          {:ok true :path (:path resolved)}
          {:ok false
           :error-type :alias-migration-retire-path-refused
           :error (or (:error resolved)
                      "The superseded defining file is outside the project root")})))))

;; @spec MCP-OP-ALIAS-041
(defn- retire-file!
  "Move the superseded defining file out of the source tree, reversibly.

  `real-source` is the same canonical path the transaction's edits addressed."
  [project-root relative ^String real-source]
  (let [target (io/file (retire-path project-root relative))]
    (.mkdirs (.getParentFile target))
    (Files/move (.toPath (io/file real-source)) (.toPath target)
                (into-array java.nio.file.CopyOption
                            [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
    ;; the receipt publishes this, so it is project-relative: an absolute server
    ;; path is not something the caller can act on and leaks the host layout
    (retire-relative-path relative)))

;; @spec MCP-OP-ALIAS-041
(defn- restore-retired!
  "Put the retired defining file back at the same canonical path it came from."
  [project-root relative ^String real-source]
  (let [target (io/file real-source)
        retired (io/file (retire-path project-root relative))]
    (when (.exists retired)
      (.mkdirs (.getParentFile target))
      (Files/move (.toPath retired) (.toPath target)
                  (into-array java.nio.file.CopyOption
                              [java.nio.file.StandardCopyOption/REPLACE_EXISTING])))))

(defn commit!
  "Commit one alias migration through `execute-mcp-change!`.

  Verification, when the workspace configures a profile, shares the
  transaction's rollback authority exactly as the other write tools do. A
  lib-only migration additionally retires the superseded defining file after
  the kernel commits; any later failure restores it before rolling back, so the
  tree is never left with two definitions or none."
  ([config project-root spec files] (commit! config project-root spec files nil))
  ([{:keys [verification-profiles receipt-dir verify attempted]}
    project-root spec files retire]
  ;; the receipt directory is NOT created here. Every refusal below decides on
  ;; paths alone, and a refusal that first mkdirs the very directory it is
  ;; refusing to write in has already mutated the tree it reports untouched.
  (let [retire-source (when retire (resolve-retire-source project-root retire))
        receipt-name (new-receipt-name)]
  (cond
    ;; @spec MCP-OP-ALIAS-054
    (receipt-dir-escapes? receipt-dir)
    {:error (str "The configured receipt directory cannot be canonicalized: "
                 "the part of it that does not exist yet climbs above the "
                 "nearest directory that does")
     :error-type :alias-migration-receipt-dir-escapes
     :source-unchanged true
     :remedy (str "Configure receipt-dir as a path whose missing components "
                  "descend from an existing directory; a receipt directory "
                  "whose identity cannot be proved cannot be checked against "
                  (.getPath (detail-directory project-root)) ".")}

    ;; @spec MCP-OP-ALIAS-054
    (receipt-detail-collision? project-root receipt-dir receipt-name)
    {:error (str "The configured receipt directory would publish this undo "
                 "receipt inside the detail writer's own name namespace")
     :error-type :alias-migration-receipt-detail-collision
     :source-unchanged true
     :remedy (str "Configure receipt-dir outside "
                  (.getPath (detail-directory project-root))
                  ", or rename the detail documents; a receipt this verb may "
                  "prune is a receipt that cannot be trusted.")}

    (unknown-profile? verification-profiles verify)
    {:error (str "Unknown verification profile: " verify)
     :error-type :unknown-verification-profile
     :source-unchanged true}

    ;; the defining file is resolved before the transaction writes, so a path
    ;; the retire could not honour refuses with nothing yet mutated
    (and retire-source (not (:ok retire-source)))
    {:error (:error retire-source)
     :error-type (:error-type retire-source)
     :source-unchanged true}

    :else
    ;; @spec MCP-OP-ALIAS-054
    ;; the identity proved above was proved on a path that did not exist yet,
    ;; and a path's identity is not settled until it does: between that answer
    ;; and this line a missing component can become a symlink to the detail
    ;; directory. So the directory that was actually CREATED is re-proved
    ;; before a byte is written into it, and a refusal removes only what this
    ;; call made.
    (let [created (create-receipt-directory! receipt-dir)]
     (if (receipt-detail-collision? project-root receipt-dir receipt-name)
      (do
        (doseq [^Path path created]
          (.delete (.toFile path)))
        {:error (str "The receipt directory that now exists is the detail "
                     "writer's own directory; the identity checked before it "
                     "existed is not the identity it has")
         :error-type :alias-migration-receipt-detail-collision
         :phase "post-create"
         :source-unchanged true
         :remedy (str "Configure receipt-dir outside "
                      (.getPath (detail-directory project-root))
                      ", and check what is creating symlinks there; a receipt "
                      "this verb may prune is a receipt that cannot be "
                      "trusted.")})
      (let [profile (selected-profile verification-profiles verify)
        baseline (when profile
                   (change-buffer/capture-verification-baseline!
                     project-root profile verification-profiles files))]
    (if (and baseline (not (:ok baseline)))
      {:error "Verification baseline capture failed before the alias migration"
       :error-type (or (:error-type baseline) :verification-baseline-failed)
       :verification baseline
       :source-unchanged true}
      (let [;; @spec MCP-OP-ALIAS-047
            ;; the marker is set by the transaction's OWN write boundary, not
            ;; by this call site. Entering `execute-mcp-change!` is not a
            ;; mutation: spec validation, the frozen read, compilation, receipt
            ;; staging and the whole-file hash preflight all run inside it
            ;; before a source byte is written, and heap exhaustion in any of
            ;; them leaves the tree exactly as the caller left it — as do the
            ;; retire resolution, the profile check and the baseline capture
            ;; above. The kernel calls this back immediately before its first
            ;; write, which is the only moment at which the answer changes.
            result (transaction/execute-mcp-change!
                     {:spec spec
                      :receipt-out (str (io/file receipt-dir receipt-name))
                      :on-write-boundary (when attempted
                                           #(vreset! attempted true))
                      :write-refusal-context {:operation "alias_migration"
                                              :project-root project-root}})]
        (if (:error result)
          result
          (let [retired (try
                          (when retire
                            (retire-file! project-root retire
                                          (:path retire-source)))
                          (catch Exception error
                            {:retire-error (.getMessage error)}))]
            (cond
              ;; @spec MCP-OP-ALIAS-043
              (:retire-error retired)
              (let [rollback (transaction/execute-undo!
                               {:receipt (:receipt-file result)})
                    rolled-back? (boolean (:ok rollback))]
                ;; the same discipline as the verification-failure branch: an
                ;; undo receipt for a transaction that has already been undone
                ;; would invite a second, destructive undo
                (when rolled-back?
                  (.delete (io/file (:receipt-file result))))
                {:error (str "The superseded defining file could not be retired; "
                             "the alias migration was rolled back")
                 :error-type :alias-migration-retire-failed
                 :cause-error (:retire-error retired)
                 :rolled-back rolled-back?
                 :source-unchanged rolled-back?})

              (nil? profile)
              (cond-> result retired (assoc :retired-file retired))

              :else
              (let [verification (change-buffer/run-verification!
                                   project-root profile verification-profiles
                                   files baseline)]
                (if (:ok verification)
                  (cond-> (assoc result :verification verification)
                    retired (assoc :retired-file retired))
                  (let [_ (when retire
                            (restore-retired! project-root retire
                                              (:path retire-source)))
                        rollback (transaction/execute-undo!
                                   {:receipt (:receipt-file result)})
                        rolled-back? (boolean (:ok rollback))]
                    (when rolled-back?
                      (.delete (io/file (:receipt-file result))))
                    {:error "Verification failed; the alias migration was rolled back"
                     :error-type (or (:error-type verification) :verification-failed)
                     :verification verification
                     :rolled-back rolled-back?
                     :source-unchanged rolled-back?})))))))))))))))

;; @spec MCP-OP-ALIAS-001
;; @spec MCP-OP-ALIAS-005
;; @spec MCP-OP-ALIAS-007
;; @spec MCP-OP-ALIAS-008
;; @spec MCP-OP-ALIAS-009
;; @spec MCP-OP-ALIAS-010
;; @spec MCP-OP-ALIAS-011
;; @spec MCP-OP-ALIAS-013
;; @spec MCP-OP-ALIAS-014
;; @spec MCP-OP-ALIAS-015
;; @spec MCP-OP-ALIAS-018
;; @spec MCP-OP-ALIAS-019
(defn- execute-migration!
  "Plan, commit, and publish one O(1) alias_migration receipt.

  `attempted` is handed to `commit!`, which sets it at the transaction kernel's
  own entrance — the first write — so the heap-exhaustion guard around this
  function can say truthfully whether any write was ever begun. Setting it here,
  at the CALL to `commit!`, would claim a mutation for a heap exhausted while
  resolving the retire source or capturing a verification baseline, none of
  which write a byte."
  [config params attempted]
  (let [validated (validate-request params)]
    (if-not (:ok validated)
      validated
      (let [request (:request validated)
            project-root (:project-root config)]
        (if (unknown-profile? (:verification-profiles config) (:verify request))
          ;; refuse before any discovery: an unusable verification authority is
          ;; known from the request alone, and doing the work first would make
          ;; the refusal depend on the tree's state
          (refusal :unknown-verification-profile
                   (str "Unknown verification profile: " (:verify request))
                   {:verify (:verify request)
                    :configured_profiles (vec (sort (keys (:verification-profiles config))))
                    :remedy "Name a profile this workspace configures, or omit verify."})
          (let [planned (plan! project-root request)]
        (if-not (:ok planned)
          planned
          (let [{:keys [plan root paths destination]} planned
                spec (plan->spec plan paths destination)
                files (mapv #(get paths (:file %)) (:files plan))
                verify (:verify request)
                commit (commit! (assoc config :verify verify
                                        :attempted attempted)
                                (.toString root) spec files
                                (get-in plan [:lib-rename :file]))]
            ;; @spec MCP-OP-ALIAS-042
            (if (or (:error commit) (not (:committed commit)))
              (commit-refusal plan commit)
              (receipt plan
                       (-> commit
                           (assoc :undo_receipt (:receipt-file commit)
                                  :receipt_hash (:receipt-hash commit)
                                  :verify-requested (boolean verify)))
                       (write-details! root plan)))))))))))

;; @spec MCP-OP-ALIAS-047
(defn execute!
  "One alias_migration, with heap exhaustion published as a typed refusal.

  The ceilings above make an OutOfMemoryError unreachable for any scope the verb
  accepts, but a ceiling is an argument and this is a guard: the MCP tool
  entrance has no `try` of its own, so without this an `Error` escapes as an
  untyped throw and the caller learns nothing about the state of its tree. The
  refusal reports `source_unchanged` from whether the transaction kernel was
  ever entered rather than from a hopeful literal."
  [config params]
  (let [attempted (volatile! false)]
    (try
      (execute-migration! config params attempted)
      (catch OutOfMemoryError error
        (let [mutated? @attempted]
          {:ok false
           :operation "alias_migration"
           :error_type "alias-migration-resource-exhausted"
           :error (str "alias_migration exhausted the server's heap"
                       (if mutated?
                         " after entering the transaction kernel"
                         " before entering the transaction kernel"))
           :cause (str (.getMessage error))
           :source_unchanged (not mutated?)
           :mutation_attempted mutated?
           :write_authority false
           :max_files max-scope-files
           :max_bytes max-scope-bytes
           :next_action (if mutated? "review_receipt" "correct_request")
           :next_call nil
           :remedy (if mutated?
                     (str "Inspect the workspace's undo receipts before "
                          "resending; the transaction's own state is the "
                          "authority, not this refusal.")
                     (str "Narrow scope.paths; the whole scope is held in "
                          "memory at once."))})))))
