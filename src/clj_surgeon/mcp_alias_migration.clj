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
(declare elide)

(def request-fields
  #{:op :workspace_root :from :to :scope :expect :verify})

(def skipped-directories
  #{".git" ".hg" "target" "node_modules" ".cpcache" ".clj-kondo" ".lsp"
    ".clj-surgeon" ".shadow-cljs" "out"})

;; @spec MCP-OP-ALIAS-056
(def control-directories
  "Directory names inside a workspace whose contents belong to another tool.

  A receipt published into one of these is not a stray file: it is a file the
  owning tool READS. `receipt-dir=.git/refs/heads` publishes `<uuid>.edn` into
  git's ref namespace and `git show-ref` exits 128 on it — the workspace's own
  tooling is broken by a verb that reported success. The version-control
  metadata directories are here because their contents are parsed, and the
  machine-local build caches because their contents are deleted without notice,
  which is not where a durable undo receipt may live.

  `.clj-surgeon` is deliberately ABSENT. Co-locating the receipts with the
  detail documents is a legal configuration and stays legal; the collision
  guard above, not this set, is what keeps the two name namespaces disjoint."
  #{".git" ".hg" ".svn" ".jj" "target" "node_modules" ".cpcache"})

;; @spec MCP-OP-ALIAS-056
(def control-directories-anywhere
  "Control directory names refused at ANY depth, inside the workspace or out.

  Containment against the workspace root is the wrong instrument for these.
  A LINKED git worktree's `.git` is a file, and its real control directory is
  `<main>/.git/worktrees/<name>/` — outside the root, so containment never
  sees it, and `receipt-dir=<that>/refs/heads` published a receipt into git's
  per-worktree ref storage with ok=true. A monorepo subproject whose
  `project-root` sits below the repository root has the same shape.

  Only the version-control metadata names are here. `target`, `node_modules`
  and `.cpcache` stay containment-only: they are ordinary words that a
  caller's own absolute receipt directory may legitimately contain, and an
  absolute receipt directory outside the workspace is the ORDINARY
  configuration. A `.git` anywhere on the path is never ordinary."
  #{".git" ".hg" ".svn" ".jj"})

;; ---------------------------------------------------------------------------
;; refusals

(defn- refusal
  [error-type message extra]
  (merge {:ok false
          :operation "alias_migration"
          ;; @spec MCP-OP-ALIAS-059
          ;; forwarded-refusal-kind: every caller spells its kind as a
          ;; keyword literal at its own call site, scanned there by the
          ;; `:error_type "…"` scan; this constructor only forwards that
          ;; argument verbatim and mints nothing of its own
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

;; @spec MCP-OP-ALIAS-051
(defn glob-parse-error
  "The glob parser's own message for one unparseable pattern, or nil.

  `getPathMatcher` is handed CALLER TEXT, and it throws
  `PatternSyntaxException` on an unclosed group, class or escape — `src/{**` is
  one keystroke from `src/{clj,cljs}/**`. The throw had no catch anywhere on
  the path: `execute!` catches `OutOfMemoryError` only and
  `mcp-operation/invoke!` catches nothing, so it surfaced as
  `mcp-adapter-failure`, a receipt carrying no `source_unchanged`, no
  `mutation_attempted`, no remedy and no next_call, whose text is raw JSON that
  never passes through the refusal summary. Compiling the pattern here, before
  the walk, turns that class into the typed refusal the rest of this verb
  publishes — and the parser's own message is the only thing that tells the
  caller WHERE the spelling broke, so it travels in the refusal."
  [pattern]
  (try
    (glob-matcher pattern)
    nil
    (catch Exception error
      (or (.getMessage error) (.getName (class error))))))

;; @spec MCP-OP-ALIAS-060
(defn- relative-path
  "One project-relative path string, byte-faithful to the filesystem it came
  from.

  The separator is the one THAT filesystem uses, read from the filesystem
  itself, and never a hardcoded backslash. On POSIX a backslash is an ordinary
  path character: `\\` is a legal top-level directory name and `c\\d.clj` is a
  legal file name, so replacing every backslash with a slash turned one
  segment into two and dropped the owner under it from `scope.paths [\"**\"]` —
  after which the verb committed the owners it could still see under a receipt
  claiming complete discovery."
  [^Path root ^Path candidate]
  (let [relative (.relativize root candidate)
        separator (.getSeparator (.getFileSystem relative))
        text (.toString relative)]
    (if (= "/" separator)
      text
      (str/replace text separator "/"))))


;; @spec MCP-OP-ALIAS-061
(def refused-code-point-types
  "Unicode general categories no `scope.paths` entry may carry.

  C0 and C1 controls and DEL (CONTROL), the format and bidirectional-override
  characters (FORMAT — U+200B, U+FEFF, U+202E), unpaired surrogates
  (SURROGATE), private-use and unassigned code points. SPACE_SEPARATOR is NOT
  here: a directory named `root with spaces` is ordinary, and this gate exists
  for the spellings a caller cannot SEE, not for the ones they can."
  #{(int Character/CONTROL) (int Character/FORMAT) (int Character/SURROGATE)
    (int Character/PRIVATE_USE) (int Character/UNASSIGNED)
    (int Character/LINE_SEPARATOR) (int Character/PARAGRAPH_SEPARATOR)})

;; @spec MCP-OP-ALIAS-061
(def replacement-character
  "U+FFFD, the only trace a malformed byte sequence can leave in a JVM string.

  Overlong UTF-8 cannot survive decoding: the `C0 AF` encoding of `/` is
  normalised to `/` by the JSON layer before this verb sees it, so there is
  nothing left to type. A decoder that does NOT normalise emits U+FFFD, and
  that is the observable form of the same malformation."
  \ufffd)

;; @spec MCP-OP-ALIAS-061
(defn refused-code-point
  "The first non-printable or malformed code point in one scope entry.

  Returns `{:code-point n :index i}` or nil. Only U+0000 was ever typed, and
  NUL is not special: it is one member of a class whose whole point is that
  the caller cannot see it. Every other member compiled as a glob, matched
  nothing, and was published as `scope-matches-nothing` — an assertion about
  the TREE that the walk never made.

  A surrogate is refused only when it is UNPAIRED: a valid pair is one
  ordinary supplementary character and names a path like any other."
  [entry]
  (let [text (str entry)
        length (count text)]
    (loop [index 0]
      (when (< index length)
        (let [ch (.charAt text index)
              paired? (and (Character/isHighSurrogate ch)
                           (< (inc index) length)
                           (Character/isLowSurrogate (.charAt text (inc index))))
              code-point (if paired?
                           (Character/toCodePoint ch (.charAt text (inc index)))
                           (int ch))]
          (cond
            (and (not paired?)
                 (or (= replacement-character ch)
                     (contains? refused-code-point-types
                                (int (Character/getType (int ch))))))
            {:code-point code-point :index index}

            paired?
            (if (contains? refused-code-point-types
                           (int (Character/getType code-point)))
              {:code-point code-point :index index}
              (recur (+ index 2)))

            :else (recur (inc index))))))))

;; @spec MCP-OP-ALIAS-061
(defn code-point-label
  "One code point, spelled the way a caller can search for it."
  [code-point]
  (format "U+%04X" code-point))

;; @spec MCP-OP-ALIAS-057
(defn scope-glob-patterns
  "The glob patterns one `scope.paths` entry selects under `root`.

  `scope.paths` are globs, so the entry `src` selects only a path spelled
  exactly `src`, and a caller who means the directory selects nothing at all.
  That is not a hypothetical: every tool arm of the E3-P cohort spelled it that
  way on its first call and paid a refusal for it, four refusals in four of four
  arms, on a rung whose pass line is one call.

  An entry that names an existing DIRECTORY under the root is therefore read as
  that directory's subtree as well — the obvious reading of a directory name.
  The literal pattern is kept alongside it, so an entry that is both a legal
  glob and a directory name loses nothing it selected before; this widens what a
  scope admits and can never narrow it.

  Resolution is lexical-then-checked: an entry that escapes the root, is
  absolute, or is the root itself contributes no subtree pattern, so the
  confinement the walk performs afterwards is never handed a wider tree than the
  caller named.

  The subtree pattern is built from the entry NORMALISED AGAINST THE ROOT and
  never from the caller's raw text. The check and the pattern must agree about
  which directory the entry names, or the entry is detected as a directory and
  then handed a glob that can never match it: `./src` was read as a directory
  and published as the patterns `[\"./src\" \"./src/**\"]`, neither of which
  matches a project-relative path, so it selected zero files and earned the
  `scope-matches-nothing` refusal this requirement exists to end — as did
  `src/.`, `src//`, and every other spelling of the same directory."
  [^Path root pattern]
  (let [trimmed (str/replace pattern #"/+$" "")
        subtree
        (when (and (not (str/blank? trimmed))
                   (not (str/starts-with? trimmed "/"))
                   (not (str/includes? trimmed "\u0000")))
          (try
            (let [candidate (.normalize (.resolve root trimmed))]
              (when (and (.startsWith candidate root)
                         (not (.equals candidate root))
                         (Files/isDirectory candidate (make-array LinkOption 0)))
                (str (relative-path root candidate) "/**")))
            (catch Exception _ nil)))]
    (cond-> [pattern]
      subtree (conj subtree))))

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

;; @spec MCP-OP-ALIAS-060
(defn independent-scope-count
  "A SECOND count of the sources one scope selects, over Path objects alone.

  The scope walk derives one relative path STRING per entry and matches on
  that string; this counts the same scope by matching the compiled patterns
  against the relative PATH the filesystem hands back, so it builds no path
  string and cannot inherit the walk's path arithmetic. Two enumerations that
  share no arithmetic are two witnesses; one enumeration is an assertion.

  It exists because a discovery defect is SILENT by construction: round twelve
  found a legal POSIX directory dropped from `scope.paths [\"**\"]`, and the
  verb committed the remainder under a receipt claiming complete discovery.
  Nothing in the receipt could have contradicted it, because nothing had
  counted the tree a second time.

  Runs only where the walk already returned `:ok`, so every bound the walk
  enforces — depth, entry ceiling, readability — has already been cleared by
  the same tree."
  [^Path root patterns exclude]
  (let [matchers (mapv glob-matcher patterns)
        excluded (into #{}
                       (map (fn [^String entry]
                              (.getPath (.getFileSystem root) entry
                                        (make-array String 0))))
                       exclude)
        counted (volatile! 0)
        visitor
        (proxy [SimpleFileVisitor] []
          (preVisitDirectory [dir _attrs]
            (let [^Path directory dir]
              (if (and (not (.equals root directory))
                       (contains? skipped-directories
                                  (str (.getFileName directory))))
                FileVisitResult/SKIP_SUBTREE
                FileVisitResult/CONTINUE)))
          (visitFile [file _attrs]
            (let [^Path candidate file
                  relative (.relativize root candidate)]
              (when (and (source-file-name? candidate)
                         (Files/isRegularFile candidate
                                              (make-array LinkOption 0))
                         (not (contains? excluded relative))
                         (some #(.matches ^PathMatcher % relative) matchers))
                (vswap! counted inc)))
            FileVisitResult/CONTINUE)
          (visitFileFailed [_file _error] FileVisitResult/CONTINUE)
          (postVisitDirectory [_dir _error] FileVisitResult/CONTINUE))]
    (Files/walkFileTree root
                        (EnumSet/noneOf FileVisitOption)
                        Integer/MAX_VALUE
                        visitor)
    @counted))

;; @spec MCP-OP-ALIAS-004
;; @spec MCP-OP-ALIAS-037
;; @spec MCP-OP-ALIAS-048
;; @spec MCP-OP-ALIAS-049
;; @spec MCP-OP-ALIAS-050
;; @spec MCP-OP-ALIAS-060
(defn- scan-parsed-scope
  "The bounded walk itself, over globs already proved parseable.

  Split from `scan-scope` so pattern COMPILATION happens before the first
  filesystem entry is visited: a `PatternSyntaxException` raised half-way
  through a walk is a throw with a partial answer behind it, and this verb owes
  its caller a typed refusal it can read the tree's state from."
  [^Path root patterns exclude]
  (let [matchers (mapv glob-matcher patterns)
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

      :else
      ;; @spec MCP-OP-ALIAS-060
      ;; the completeness claim is WITNESSED and never asserted: an
      ;; independent enumeration of the same scope must agree with what the
      ;; walk considered, or the scope's contents are not knowable and no
      ;; migration downstream may claim to have closed the fan-out
      (let [files (vec (sort found))
            enumerated (independent-scope-count root patterns exclude)]
        (if (= (count files) enumerated)
          {:ok true :files files}
          {:ok false
           :error-type :alias-migration-discovery-incomplete
           :files-considered (count files)
           :files-enumerated enumerated})))))

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
  (let [;; @spec MCP-OP-ALIAS-061
        refused (first (keep (fn [entry]
                               (when-let [found (refused-code-point entry)]
                                 (assoc found :entry entry)))
                             paths))
        nul (when (and refused (zero? (:code-point refused))) refused)
        expanded (when-not refused
                   (mapv (fn [entry]
                           {:entry entry
                            :patterns (scope-glob-patterns root entry)})
                         paths))
        unparseable (first
                      (keep (fn [{:keys [entry patterns]}]
                              (when-let [pattern (first (filter glob-parse-error
                                                                patterns))]
                                {:entry entry
                                 :pattern pattern
                                 :cause (glob-parse-error pattern)}))
                            expanded))]
    (cond
      ;; @spec MCP-OP-ALIAS-051
      ;; a NUL cannot appear in any filesystem path, but `getPathMatcher`
      ;; accepts one, so the entry compiled, matched nothing, and earned
      ;; `scope-matches-nothing` — a refusal about the TREE for a spelling
      ;; cause that prints as nothing at all. It is refused here, before the
      ;; first visited entry, and the byte is named in words because the
      ;; caller cannot see it in their own request.
      nul
      {:ok false
       :error-type :alias-migration-scope-path-refused
       :refusal-reason :nul-byte
       :path (:entry nul)
       :pattern (:entry nul)
       :cause (str "the entry carries a NUL byte (U+0000) at index "
                   (:index nul)
                   ", which no filesystem path can hold, so the entry names no"
                   " path this walk could visit")}

      ;; @spec MCP-OP-ALIAS-061
      ;; every OTHER invisible or malformed spelling, refused for the same
      ;; reason NUL is and named the same way: the caller cannot see it in
      ;; their own request, so the refusal has to say which code point it is
      ;; and where
      refused
      {:ok false
       :error-type :alias-migration-scope-path-refused
       :refusal-reason :refused-code-point
       :path (:entry refused)
       :pattern (:entry refused)
       :cause (str "the entry carries the non-printable or malformed code "
                   "point " (code-point-label (:code-point refused))
                   " at index " (:index refused)
                   ", which renders as nothing a caller can see, so the entry"
                   " names no path this walk could visit")}

      ;; @spec MCP-OP-ALIAS-051
      unparseable
      {:ok false
       :error-type :alias-migration-scope-path-refused
       :refusal-reason :unparseable-glob
       :path (:entry unparseable)
       :pattern (:pattern unparseable)
       :cause (:cause unparseable)}

      :else
      (scan-parsed-scope root (mapcat :patterns expanded) exclude))))

;; @spec MCP-OP-ALIAS-051
;; @spec MCP-OP-ALIAS-058
(def glob-metacharacter-escapes
  "Every character `java.nio.file.PathMatcher` reads as glob syntax.

  A directory name is DATA and a glob is SYNTAX; deriving one from the other
  without escaping publishes the caller's own filesystem as a pattern
  language. `a{b` is a legal POSIX directory name and `a{b/**` is not a legal
  glob; `[x]`, `{a,b}` and `*` are legal names AND legal globs, which is
  worse, because the pattern parses and then selects something else."
  {\\ "\\\\" \* "\\*" \? "\\?" \[ "\\[" \] "\\]" \{ "\\{" \} "\\}"})

;; @spec MCP-OP-ALIAS-051
;; @spec MCP-OP-ALIAS-058
(defn glob-escape
  "One literal path segment, spelled so a glob matches it and nothing else."
  [segment]
  (str/escape segment glob-metacharacter-escapes))

;; @spec MCP-OP-ALIAS-004
;; @spec MCP-OP-ALIAS-058
(def max-suggested-scope-paths
  "How many source roots one `scope-matches-nothing` remedy NAMES.

  The remedy is a next_call and a next_call is constant-size or it is not
  publishable; a tree with many top-level source directories has only a sample
  of them named. The sample is not the selection: when roots are dropped the
  remedy also carries `**`, so the bound costs the caller detail in the listing
  and never a file in the file set (`suggested-scope-paths`)."
  6)

;; @spec MCP-OP-ALIAS-058
(def completing-scope-path
  "The pattern appended when the root listing is truncated.

  It is the pattern the walk itself used, so a truncated remedy selects exactly
  the file set the walk saw. A bounded LISTING is honest; a bounded SELECTION
  presented as \"the source roots this tree actually holds\" is not — on a
  nine-root tree the alphabetical first six selected 14 of 118 sources and
  dropped `src/**`, the root holding a hundred namespaces, with no field saying
  a root had been dropped."
  "**")

;; @spec MCP-OP-ALIAS-058
(defn suggested-scope-paths
  "Glob patterns that select the Clojure sources this tree actually holds.

  Derived from the walk's own answers rather than hardcoded, so the remedy
  follows the tree it was refused over: a top-level directory holding sources
  becomes `<dir>/**`, and a source file sitting directly at the root becomes
  `*`. Both spellings were measured against `java.nio.file.PathMatcher` on this
  corpus before being published as a remedy — `src/**/*.clj` is NOT used,
  because it matches neither `src/a.clj` nor any `.cljc` or `.cljs` file, and a
  remedy that silently drops a file class is the defect this refusal exists to
  end.

  A root name is DATA: its glob metacharacters are escaped before the pattern
  is built, so the directory `a{b` becomes `a\\{b/**` — a pattern that parses
  and matches that directory alone — and `[x]`, `{a,b}` and `*` stop matching
  something else in silence. Every derived pattern is then validated through
  `glob-parse-error`, the same gate the scan applies to a caller's own entry,
  so a remedy this verb would refuse is never published as a correction.

  Roots are ranked by the number of sources they hold, ties broken
  lexicographically, so the sample a bound keeps is the part of the tree the
  caller most likely meant rather than the part whose name sorts first. When
  the tree holds more roots than the bound names, `completing-scope-path` is
  appended and `:roots` / `:roots-listed` differ: the caller is told the
  listing is a sample, and the selection stays complete either way. The listed
  roots keep their sorted order so the published call is a function of the tree
  and not of the ranking's tie order.

  Returns `{:source-files n :roots m :roots-listed k :truncated? bool :paths v}`;
  `:paths` is empty when the tree holds no Clojure source at all, which is
  itself the honest answer and is reported as such."
  [^Path root]
  (let [relatives (:files (scan-scope root {:paths ["**"] :exclude []}))
        counts (reduce (fn [acc ^String relative]
                         (let [cut (.indexOf relative "/")]
                           (update acc
                                   (if (neg? cut)
                                     "*"
                                     (str (glob-escape (subs relative 0 cut))
                                          "/**"))
                                   (fnil inc 0))))
                       {}
                       relatives)
        ;; @spec MCP-OP-ALIAS-051
        ;; the derived remedy is validated through the SAME parser gate the
        ;; scan uses before it is published: a pattern this verb would refuse
        ;; is never handed to the caller as a correction, and dropping one
        ;; leaves `roots` above the listed count, so the completing `**` is
        ;; appended and the selection stays whole
        ranked (->> counts
                    (sort-by (fn [[pattern n]] [(- n) pattern]))
                    (map key)
                    (remove glob-parse-error))
        listed (vec (sort (take max-suggested-scope-paths ranked)))
        truncated? (> (count counts) (count listed))]
    {:source-files (count relatives)
     :roots (count counts)
     :roots-listed (count listed)
     :truncated? truncated?
     :ranked (vec ranked)
     :counts counts
     :paths (cond-> listed
              truncated? (conj completing-scope-path))}))

;; @spec MCP-OP-ALIAS-058
(defn root-listing
  "One candidate listing of `n` roots, completed when it does not name them all.

  Separated from the bound so the listing can be SHRUNK: the completing
  pattern keeps the selection whole at any listing size, so dropping a root
  from the listing costs the caller a name and never a file."
  [{:keys [ranked roots]} n]
  (let [listed (vec (sort (take n ranked)))
        truncated? (> roots (count listed))]
    {:roots-listed (count listed)
     :truncated? truncated?
     :paths (cond-> listed
              truncated? (conj completing-scope-path))}))

;; @spec MCP-OP-ALIAS-058
(defn fitting-suggestion
  "The largest root listing whose rescoping call fits the next_call ceiling.

  A remedy is executable or it is prose. `rescoping-call` answers nil past its
  512-character bound, and six top-level directories with ordinary
  fifty-two-character names compose a 539-character call — so the round-11
  receipt published `next_call nil` beside a remedy that opened \"Resend the
  next_call\". The bound belongs on the LISTING, which the completing `**`
  makes free to shrink, and never on the selection: this drops one root name
  at a time until the call fits, and the file set the call selects is the same
  file set at every size.

  When no listing fits — a request already carrying enough exclusions that the
  one-pattern call `[\"**\"]` is past the ceiling — the widest listing is
  returned with `:next-call nil` and `:next-call-characters`, the measured size
  of that smallest possible call, so the refusal can name the ceiling, the size
  that missed it, and the roots the caller must choose from rather than point
  at a call nobody composed."
  [request {:keys [ranked] :as suggestion}]
  (let [ceiling (min max-suggested-scope-paths (count ranked))
        fitted (first (keep (fn [n]
                              (let [candidate (root-listing suggestion n)]
                                (when-let [call (planner/rescoping-call
                                                  request (:paths candidate))]
                                  (assoc candidate :next-call call))))
                            (range ceiling -1 -1)))]
    (or fitted
        (assoc (root-listing suggestion ceiling)
               :next-call nil
               :next-call-characters
               (planner/next-call-characters
                 (planner/rescoping-call-shape
                   request [completing-scope-path]))))))

;; @spec MCP-OP-ALIAS-058
(def max-refusal-root-list-characters
  "Ceiling on the rendered root listing one refusal embeds, in CHARACTERS.

  An item COUNT is not a size. Seven legal top-level directories, each named a
  digit followed by 246 quotation marks, are six items — inside every count
  bound this verb states — and 3,019 JSON characters, which carried the
  visible refusal 794 characters past the 4,096 it publishes as its ceiling.
  A bound that counts entries bounds the caller's patience and not the
  receipt."
  512)

;; @spec MCP-OP-ALIAS-059
(defn root-sizes
  "The largest roots as `<pattern> (<sources>)`, bounded in CHARACTERS.

  What a caller needs when no call can be composed: not the sample the remedy
  would have sent, but the roots themselves with the weight of each, so the
  scope they spell by hand is the part of the tree they meant.

  Each entry is elided at the per-field ceiling, because a root NAME is
  caller-supplied data, and the listing then stops at the first entry that
  would carry it past `max-refusal-root-list-characters`. The ceiling is read
  on the RENDERED form and not on the raw string: the remedy embeds
  `(pr-str listing)`, and a root name of 246 quotation marks renders at twice
  its own length, which is exactly how a listing inside every count bound
  reached 3,019 characters. Where entries are dropped the listing says so and
  says how many, so a caller reading six of seven roots knows the seventh
  exists — silent truncation inside a remedy is the same defect as silent
  truncation inside a fact line, and worse, because the caller acts on a
  remedy.

  That drop marker is PART OF THE LISTING and is charged against the same
  ceiling: a budget that stopped one item short of the end retained four
  ordinary 116-character roots and rendered 528 characters against a stated
  512, the overflow carried by the very item announcing that the bound had
  fired. So the bound is read on the RENDERED listing with the marker inside
  it, and the listing shrinks until that measurement fits."
  [{:keys [ranked counts]}]
  (let [candidates (mapv (fn [pattern]
                           (elide (str pattern " (" (get counts pattern) ")")))
                         (take max-suggested-scope-paths ranked))
        total (count candidates)
        listing (fn [kept-count]
                  (let [dropped (- total kept-count)]
                    (cond-> (subvec candidates 0 kept-count)
                      (pos? dropped)
                      (conj (str "… [+" dropped
                                 " more roots, complete in structuredContent]")))))
        ;; the ceiling is read on the listing this returns, marker INCLUDED,
        ;; so the answer is measured rather than estimated: an incremental
        ;; charge of `(count (pr-str item)) + 1` over a starting 2 is one
        ;; character more than the vector renders, and cut a listing that
        ;; rendered at exactly the ceiling
        fitted (first (filter #(<= (count (pr-str (listing %)))
                                   max-refusal-root-list-characters)
                              (range total -1 -1)))]
    (listing (or fitted 0))))

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

;; @spec MCP-OP-ALIAS-059
(def max-refusal-field-characters
  "Ceiling on ONE caller-supplied string a refusal carries.

  `max-refusal-fact-characters` bounds the rendered `facts ·` line, and only
  that line: `:error` and `:remedy` are envelope keys, rendered whole, and
  both quote the caller's own text. A 10,001-character `scope.paths` entry
  produced a 20,031-character parser message — the parser echoes the pattern
  twice — inside a 30,141-character error sentence and a 51,191-character text
  block. Every other ceiling this verb carries reads on the size of the TREE;
  this one reads on the size of the caller's own input, which is the one thing
  in a refusal that nothing upstream of it constrains."
  200)

;; @spec MCP-OP-ALIAS-059
(def max-refusal-field-items
  "How many caller-supplied entries one refusal field names."
  8)

;; @spec MCP-OP-ALIAS-059
(def max-refusal-text-characters
  "The ceiling the whole rendered refusal text is held to.

  Stated as a number the witness can assert the rendered block against: a
  receipt is constant-size or it is not a receipt, and per-field bounds are
  only a claim about the whole until the whole is measured."
  4096)

;; @spec MCP-OP-ALIAS-059
(defn elide
  "One caller-supplied string, bounded, naming the length it replaced.

  Truncation with a typed marker rather than in silence: the caller sent the
  bytes and is the only one who can recognise them from a prefix, and the
  original length is what says the prefix is a prefix."
  [value]
  (let [text (str value)]
    (if (<= (count text) max-refusal-field-characters)
      text
      (str (subs text 0 max-refusal-field-characters)
           "… [elided, " (count text) " characters]"))))

;; @spec MCP-OP-ALIAS-059
(defn elide-items
  "One caller-supplied list, bounded in entry length AND in entry count."
  [values]
  (let [values (vec values)
        bounded (mapv elide (take max-refusal-field-items values))]
    (cond-> bounded
      (> (count values) max-refusal-field-items)
      (conj (str "… [elided, " (count values) " entries]")))))

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
      ;; @spec MCP-OP-ALIAS-051
      ;; the glob never compiled, so no file was visited and the tree's state
      ;; is known exactly: untouched. The parser's own message is the only
      ;; thing that says WHERE the spelling broke, so it is quoted rather than
      ;; summarised, and no next_call is computable — a malformed pattern has
      ;; no mechanical correction, only the one the caller meant.
      (= :alias-migration-scope-path-refused (:error-type scan))
      (let [nul? (= :nul-byte (:refusal-reason scan))
            invisible? (or nul?
                           (= :refused-code-point (:refusal-reason scan)))]
        (refusal :alias-migration-scope-path-refused
                 (str "scope.paths entry " (pr-str (elide (:path scan)))
                      (if invisible?
                        " cannot name a path"
                        (str " is not a parseable glob"
                             (when-not (= (:path scan) (:pattern scan))
                               (str " — it names a directory, and the subtree"
                                    " pattern "
                                    (pr-str (elide (:pattern scan)))
                                    " derived from it"))))
                      ": " (elide (:cause scan))
                      ". No file was visited, so what the scope contains is not"
                      " known.")
                 {:path (elide (:path scan))
                  :pattern (elide (:pattern scan))
                  :cause (elide (:cause scan))
                  :next_call nil
                  :remedy
                  (if invisible?
                    (str "Remove the code point the cause names and resend. "
                         "It renders as "
                         "nothing in a console, a log and a diff, so an entry "
                         "that looks exactly right can still carry one — the "
                         "cause above names its index. No next_call is "
                         "composed because only the caller knows which path "
                         "the entry was meant to spell.")
                    (str "Correct the glob and resend. The parser reported "
                         (pr-str (elide (:cause scan)))
                         "; an unclosed {group}, [class] or trailing \\ is "
                         "the usual cause — src/{clj,cljs}/** is one "
                         "keystroke from src/{**. No next_call is composed "
                         "because only the caller knows which paths the "
                         "pattern was meant to select."))}))

      ;; @spec MCP-OP-ALIAS-060
      (= :alias-migration-discovery-incomplete (:error-type scan))
      (refusal :alias-migration-discovery-incomplete
               (str "Discovery considered " (:files-considered scan)
                    " source file(s) under scope.paths while an independent "
                    "enumeration of the same scope found "
                    (:files-enumerated scan)
                    ", so what the scope contains is not knowable and no "
                    "migration over it could claim to have closed the fan-out")
               {:files_considered (:files-considered scan)
                :files_enumerated (:files-enumerated scan)
                :next_call nil
                :remedy (str "Nothing was written. Two enumerations of the "
                             "same scope disagree, which means one of them "
                             "cannot see a file the other can — a tree "
                             "changing under the walk, or a path this build "
                             "derives incorrectly. Re-run against a quiescent "
                             "tree; if the counts still disagree, this is a "
                             "clj-surgeon defect and the migration must not "
                             "be attempted, because a partial migration "
                             "published under a complete-discovery receipt is "
                             "worse than a refusal: the caller stops looking.")})

      ;; @spec MCP-OP-ALIAS-048
      (= :alias-migration-scope-too-deep (:error-type scan))
      (refusal :alias-migration-scope-too-deep
               (str (elide (:path scan)) " is " (:depth scan)
                    " path segments below the project root, past the "
                    max-scope-depth "-segment bound one alias_migration walks")
               {:path (elide (:path scan))
                :depth (:depth scan)
                :max_depth (:max-depth scan)
                :next_call nil
                :remedy (str "Narrow scope.paths so the walk does not reach "
                             (elide (:path scan))
                             ", or flatten that tree; the bound is "
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
                    (str/join ", " (elide-items (:unreadable-paths scan))))
               {:unreadable_paths (elide-items (:unreadable-paths scan))
                :unreadable_count (:unreadable-count scan)
                :next_call nil
                :remedy (str "Make those paths readable, or exclude them "
                             "through scope.paths, and resend. Continuing past "
                             "them would silently shrink the scope and hand "
                             "back a found count that omits whatever they hold.")})

      ;; @spec MCP-OP-ALIAS-058
      ;; The scope selected NO file. That is a fact about the caller's spelling,
      ;; not about the tree, and the domain refusal below — "no namespace under
      ;; scope requires from.lib" — is a claim discovery never got to make.
      ;; Four of four E3-P tool arms were refused here and told the wrong cause.
      (zero? scanned)
      (let [;; @spec MCP-OP-ALIAS-059
            ;; the caller's own paths ride the refusal bounded in entry length
            ;; and in entry count: they are named AS GIVEN, and a name that
            ;; does not fit is elided with the length it replaced
            given (elide-items (get-in request [:scope :paths]))
            suggestion (suggested-scope-paths root)
            {:keys [source-files roots]} suggestion
            ;; @spec MCP-OP-ALIAS-058
            ;; the LISTING is shrunk until the call fits; the completing `**`
            ;; keeps the selection whole at every listing size, so a remedy
            ;; that would have published `next_call nil` publishes a shorter
            ;; listing and a call the caller can actually resend
            {:keys [roots-listed truncated? paths next-call
                    next-call-characters]}
            (fitting-suggestion request suggestion)]
        (refusal :alias-migration-scope-matches-nothing
                 (str "scope.paths " (pr-str given) " matched 0 files. "
                      "scope.paths are globs: an entry selects a file only when "
                      "the glob matches that file's whole project-relative path, "
                      "and a directory name selects its subtree only when that "
                      "directory exists under the project root. Whether any "
                      "namespace here requires "
                      (get-in request [:from :lib])
                      " is not known, because no file was read.")
                 (cond->
                  {:paths given
                   :files_matched 0
                   :source_files_under_root source-files
                   :source_roots roots
                   :roots_listed roots-listed
                   :suggested_paths paths
                   :expected_files expected
                   :next_call next-call
                   :expect_files_unchanged_reason
                   planner/expect-files-unchanged-reason
                   :remedy
                   (cond
                    ;; @spec MCP-OP-ALIAS-058
                    ;; no source anywhere under the root: there is nothing to
                    ;; derive a spelling from, and saying so is the honest
                    ;; answer rather than a fabricated remedy
                    (zero? roots)
                    (str "This project root holds no .clj, .cljs or .cljc file "
                         "at all, so no spelling of scope.paths can select one. "
                         "Check workspace_root before correcting scope.paths.")

                    ;; @spec MCP-OP-ALIAS-058
                    ;; no listing fits the ceiling — a request already carrying
                    ;; enough exclusions that even ["**"] is past it. A remedy
                    ;; may name a next_call only when the receipt carries one:
                    ;; the round-11 receipt said "Resend the next_call" two
                    ;; lines above "next_call · none"
                    (nil? next-call)
                    (str "No next_call is composed: the shortest call this "
                         "remedy can compose is " next-call-characters
                         " characters, past the "
                         planner/max-next-call-characters
                         "-character next_call ceiling, so there is no call to "
                         "resend. Spell scope.paths yourself. This tree's "
                         (count (root-sizes suggestion)) " largest of " roots
                         " top-level source roots, each with the number of "
                         "sources it holds, are "
                         (pr-str (root-sizes suggestion)) "; "
                         (pr-str completing-scope-path)
                         " on its own selects every one of the " source-files
                         " sources the walk saw. expect.files declared "
                         expected
                         " and is left as declared, because no file was read.")

                    ;; @spec MCP-OP-ALIAS-058
                    ;; the listing is a bounded SAMPLE and the selection is
                    ;; complete; both facts are stated, because a remedy that
                    ;; names six of nine roots and calls them "the source roots
                    ;; this tree actually holds" selected a sixth of the tree
                    truncated?
                    (str "Resend the next_call: it replaces scope.paths with "
                         (pr-str paths) " — the " roots-listed " largest of "
                         "this tree's " roots " top-level source roots, "
                         "completed by " (pr-str completing-scope-path)
                         " so it still selects every one of the " source-files
                         " sources the walk saw. expect.files declared "
                         expected
                         " and is left as declared, because no file was read.")

                    :else
                    (str "Resend the next_call: it replaces scope.paths with "
                         (pr-str paths) ", every one of the " roots
                         " source roots this tree holds, selecting all "
                         source-files " of its sources. expect.files declared "
                         expected
                         " and is left as declared, because no file was read."))}

                   ;; @spec MCP-OP-ALIAS-058
                   ;; the roots and their weights ride the receipt only where
                   ;; the caller must spell the scope by hand
                   (and (pos? roots) (nil? next-call))
                   (assoc :next_call_characters next-call-characters
                          :max_next_call_characters
                          planner/max-next-call-characters
                          :source_root_sizes (root-sizes suggestion)))))

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
(defn- configured-receipt-file
  "The receipt directory the caller named, read the way the caller wrote it.

  An ABSOLUTE receipt-dir is a place of the caller's own choosing and is taken
  exactly as written: the receipt directory this server ships with lives under
  the user's state root, outside every workspace, so an external receipt
  directory is the ORDINARY configuration and stays legal wherever it points.

  ;; @spec MCP-OP-ALIAS-056
  A RELATIVE one is read against the WORKSPACE it names, never against whatever
  directory this process happens to have been started in. An MCP server's
  working directory is not the caller's, and resolving a caller's relative path
  against it names a directory nobody asked for — in a workspace at all only by
  accident."
  ^java.io.File [project-root receipt-dir]
  (let [file (io/file (str receipt-dir))]
    (if (.isAbsolute file)
      file
      (io/file (str project-root) (str receipt-dir)))))

;; @spec MCP-OP-ALIAS-054
;; @spec MCP-OP-ALIAS-056
(defn receipt-dir-escapes?
  "Whether the configured receipt directory has no identity this verb may use.

  It has none when the part of it that does not exist yet still climbs above
  the nearest existing ancestor after normalisation: lexical `..` past a
  canonical path is not the same directory the kernel would open, and the
  guard below cannot answer honestly about a path it cannot name.

  It has none, either, when a RELATIVE receipt directory resolves OUTSIDE the
  workspace root. A relative path is written against the workspace and reads as
  a place inside it, and `toRealPath` resolves a symlink COMPONENT before the
  normalisation above ever runs — so a link pointing out of the tree carries
  the receipts somewhere the caller never named, and does it silently, with a
  canonical answer to show for it. An absolute receipt directory outside the
  workspace is an explicit choice and is not this refusal's business. The
  boundary refuses instead, before anything is created."
  [project-root receipt-dir]
  (let [configured (io/file (str receipt-dir))
        receipt (resolved-path (configured-receipt-file project-root receipt-dir))
        root (resolved-path project-root)]
    (boolean (or (nil? receipt)
                 (and (not (.isAbsolute configured))
                      root
                      (not (.startsWith receipt root)))))))

;; @spec MCP-OP-ALIAS-056
(defn- control-directory-of
  "The workspace control directory this resolved path lies inside, or nil.

  Containment is canonical against the canonical workspace root, and what
  remains is read segment by segment: a receipt directory OUTSIDE the workspace
  is not this verb's business — it belongs to whoever configured it — and one
  inside it is named by the first control segment on the way down, so a
  directory nested any depth below `.git` answers as truthfully as `.git`
  itself. An existing symlink component is already resolved by `resolved-path`,
  so a link INTO `.git` is caught by the same containment as a plain path.

  Containment is not the whole guard. It answers only about the workspace
  ROOT, and a linked git worktree keeps its real control directory outside
  that root; so a version-control metadata segment is refused wherever it
  appears on the resolved path, and the containment scan is what adds the
  build-cache names on top of it inside the workspace."
  [project-root ^Path receipt]
  (let [root (resolved-path project-root)]
    (or (when (and root receipt (.startsWith receipt root))
          (first (filter control-directories (map str (.relativize root receipt)))))
        ;; @spec MCP-OP-ALIAS-056
        ;; and a version-control metadata segment ANYWHERE, root or not
        (when receipt
          (first (filter control-directories-anywhere (map str receipt)))))))

;; @spec MCP-OP-ALIAS-056
(defn receipt-dir-in-control-directory
  "The workspace control directory the configured receipt directory lies in, or nil."
  [project-root receipt-dir]
  (control-directory-of
    project-root
    (resolved-path (configured-receipt-file project-root receipt-dir))))

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
  (let [receipt (resolved-path (configured-receipt-file project-root receipt-dir))
        detail (resolved-path (detail-directory project-root))]
    (boolean (and (detail-document-name? receipt-name)
                  receipt
                  detail
                  (.startsWith receipt detail)))))

;; @spec MCP-OP-ALIAS-054
(defn- create-receipt-directory!
  "Create the receipt directory, returning the components THIS CALL created.

  Component by component, with `Files/createDirectory`, and a component is
  recorded only when this call's own create RETURNED. `createDirectories`
  answers for a whole chain and cannot say which links of it were its own, so a
  set computed by listing what did not exist a moment ago is not a set of
  things this call made: two calls racing the same missing chain both recorded
  all of it, and once the directories were empty one caller's cleanup deleted
  directories the peer was still counting as its own. `FileAlreadyExists` is
  the peer's answer — not mine, not recorded, and so never deleted by me.

  A component that already exists is left alone whatever it is: a receipt
  directory that is a symlink to a real directory is a legal configuration, and
  `createDirectory` would reject it outright.

  The return is deepest first, so a refusal that follows removes exactly what
  this call brought into being. `File/delete` never removes a non-empty
  directory, so a peer that filled one in between keeps it."
  [^java.io.File receipt-file]
  (let [path (.toPath (.getAbsoluteFile receipt-file))
        chain (loop [candidate path
                     acc ()]
                (if (nil? candidate)
                  acc
                  (recur (.getParent candidate) (conj acc candidate))))]
    (reduce (fn [created ^Path component]
              (if (Files/exists component (make-array LinkOption 0))
                created
                (try
                  (Files/createDirectory component (make-array FileAttribute 0))
                  (conj created component)
                  (catch java.nio.file.FileAlreadyExistsException _ created)
                  (catch java.io.IOException _ (reduced created))
                  (catch SecurityException _ (reduced created)))))
            ()
            chain)))

;; @spec MCP-OP-ALIAS-056
(defn- real-directory
  "The directory that now exists at this path, resolved ONCE, or nil.

  Every write after this goes through the path this returns and never through
  the configured one again, so a link swapped in at a configured ANCESTOR
  after the guards have answered cannot redirect a byte.

  It narrows the window; it does not close it. The value is a path, not an open
  directory — the JVM offers no `openat`, so the kernel resolves this string
  afresh on every open, and an attacker who can replace a component of the REAL
  path can still redirect the write. That attacker already owns the workspace.
  MCP-OP-ALIAS-056 states the boundary, and `receipt-published-elsewhere?`
  proves after the fact where the receipt actually landed, so the verb never
  reports success over a redirected one."
  ^Path [^java.io.File receipt-file]
  (let [path (.toPath (.getAbsoluteFile receipt-file))]
    (try
      (when (Files/isDirectory path (make-array LinkOption 0))
        (.toRealPath path (make-array LinkOption 0)))
      (catch java.io.IOException _ nil)
      (catch SecurityException _ nil))))

;; @spec MCP-OP-ALIAS-056
(defn- receipt-publication-fault
  "Why the receipt that now exists cannot be trusted, or nil.

  The last window the OS leaves open is the receipt directory's own name. It
  cannot be closed before the write, because a name is resolved on every open;
  it can be DETECTED after it, and a detected one is rolled back rather than
  reported as a success. `toRealPath` on the file that now exists names the
  file it actually landed in, which is the only authority on the question.

  The extension is proved on that real name too. The receipt path is
  CANONICALISED before staging (`canonical-receipt-path`), so a link already
  sitting on the destination name is followed rather than replaced by the
  atomic rename, and the write lands under the link's target name. When that
  target sits INSIDE the proved directory, the parent comparison agrees — and
  a canonical name without `.edn` is one `execute-undo!` refuses to read, so
  the transaction reported ok is one nothing can undo. Proving the extension
  is what turns that into a typed refusal with a rollback attempt."
  [^Path proved receipt-file]
  (let [actual (try
                 (.toRealPath (.toPath (io/file (str receipt-file)))
                              (make-array LinkOption 0))
                 (catch java.io.IOException _ nil)
                 (catch SecurityException _ nil))]
    (cond
      (or (nil? actual) (nil? proved)) :receipt-unlocatable
      (not= (.getParent actual) proved) :receipt-published-elsewhere
      (not (str/ends-with? (str (.getFileName actual)) ".edn")) :receipt-not-undoable
      :else nil)))

;; @spec MCP-OP-ALIAS-056
(defn- real-path-string
  "The path this name resolves to right now, or the name as written."
  [path]
  (or (try
        (str (.toRealPath (.toPath (io/file (str path))) (make-array LinkOption 0)))
        (catch java.io.IOException _ nil)
        (catch SecurityException _ nil))
      (str path)))

;; @spec MCP-OP-ALIAS-056
(defn- undo-restored-the-migration?
  "Did this `execute-undo!` result put the pre-migration bytes back?

  It is the undo transaction's `:ok`, and it is NEVER its `:rolled-back`.
  `commit-compiled!` mints `{:error \"Transaction write failed; all files
  restored\" :rolled-back true}` — with no `:ok` at all — when the transaction
  IT was running got put back, and the transaction running here is the UNDO.
  Recovery restores each file to the state that transaction READ, which is the
  MIGRATED content, so `:rolled-back true` on this result means the undo was
  reversed and the alias migration is STILL IN PLACE.

  That shape reads exactly like a false RED — `rolled_back false` and
  \"rollback FAILED\" over a result whose own prose says every file was
  restored — and the one-line reading that would fix it,
  `(or (:ok rollback) (:rolled-back rollback))`, publishes `source_unchanged
  true` over twelve migrated files instead. The name exists so the question is
  asked once, in words, rather than re-derived at three call sites."
  [rollback]
  (boolean (:ok rollback)))

;; @spec MCP-OP-ALIAS-056
(def ^:private undo-recovery-unmigrated-statuses
  "Per-file recovery statuses that PROVE a file no longer carries the migration.

  `commit-compiled!`'s `:recovery` map answers about the transaction it was
  running, and the transaction running here is the UNDO. Recovery restores each
  file to the state that transaction READ — which for an undo is the MIGRATED
  content — so `:original` (never rewritten) and `:restored` (rewritten and put
  back) both mean the file is STILL MIGRATED. The one status that leaves
  pre-migration bytes on disk is `:restore-failed`: the undo's write landed and
  recovery could not reverse it.

  Every other status names a state this process cannot pin — a hash that
  matches neither side, a read that failed — and an unnamed file is counted as
  still migrated. That over-states the work a human has left to do, which costs
  a wasted look; under-stating it loses a file."
  #{:restore-failed})

;; @spec MCP-OP-ALIAS-056
(defn- undo-recovery-counts
  "How many of `file-count` files one FAILED undo left migrated, measured.

  `(count files)` is the plan's number and is never a measurement: an undo that
  restored six of twelve published twelve, in the field a human reads to decide
  what to undo by hand. A rollback that offers no `:recovery` map has told this
  verb nothing per file, and the whole plan is counted as still migrated."
  [rollback file-count]
  (let [recovery (:recovery rollback)
        unmigrated (if (sequential? recovery)
                     (count (filter (comp undo-recovery-unmigrated-statuses
                                          :status)
                                    recovery))
                     0)
        restored (min unmigrated file-count)]
    {:still-migrated (- file-count restored) :restored restored}))

;; @spec MCP-OP-ALIAS-056
(defn- rollback-report
  "The fields a post-write refusal owes its caller about the tree it left.

  `source-unchanged` is the ROLLBACK's own answer and nothing else. A refusal
  that restored the tree and one whose restoration FAILED are otherwise the
  same shape, and the difference is the only thing the caller needs: whether
  its working tree is mid-migration. The orphan receipt is named by its REAL
  path — the receipt name is canonicalised before staging, so the path the
  caller configured is not necessarily where the bytes are.

  `files-still-migrated` and `files-restored` are MEASURED from the rollback's
  own per-file answers rather than counted off the plan, and they sum to the
  plan's file count."
  [rolled-back? rollback receipt-file file-count]
  (let [{:keys [still-migrated restored]}
        (if rolled-back?
          {:still-migrated 0 :restored file-count}
          (undo-recovery-counts rollback file-count))]
    {:rolled-back rolled-back?
     :source-unchanged rolled-back?
     :receipt-file (real-path-string receipt-file)
     :files-still-migrated still-migrated
     :files-restored restored}))

;; @spec MCP-OP-ALIAS-056
(defn- rollback-sentence
  "The clause a post-write refusal ends with, true of what actually happened."
  [rolled-back? receipt-file file-count]
  (if rolled-back?
    "the alias migration was rolled back"
    (str "rollback FAILED; " file-count
         (if (= 1 file-count) " file remains" " files remain")
         " migrated; receipt at " (real-path-string receipt-file))))

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

;; @spec MCP-OP-ALIAS-034
(def max-string-mention-sites
  "How many `file:line` string-mention sites one receipt names.

  The count is exact and the list is bounded: the receipt is constant in N or
  it is not a receipt, and a caller with twenty sites already has a day's work
  they can find the rest of by searching for the name the count reports."
  20)

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
       ;; @spec MCP-OP-ALIAS-034
       :string_mentions (count (:string-mentions totals))
       :string_mention_sites (vec (take max-string-mention-sites
                                        (:string-mentions totals)))
       ;; @spec MCP-OP-ALIAS-034
       ;; the bound says that it fired: a caller had to compare
       ;; `string_mentions` against the length of the list to learn that the
       ;; list was a sample, which is the silent-truncation class this verb
       ;; has already paid for twice
       :string_mention_sites_shown (min max-string-mention-sites
                                        (count (:string-mentions totals)))
       :lib_renamed (lib-renamed-summary plan commit)
       :details_path details-path
       ;; @spec MCP-OP-ALIAS-052
       :details_retention details-retention
       :details_retained max-detail-files
       :next_action (if committed? "none" "review_receipt")}
      (verification-summary (:verification commit) (:verify-requested commit))
      (select-keys commit [:undo_receipt :receipt_hash]))))

(defn commit-refusal
  "Translate one kernel refusal into the verb's typed public refusal.

  `source_unchanged` is READ from the refusal when the refusal states it, and
  synthesised from `:committed` only when it does not. The two are not the
  same question. A post-write refusal rolls the transaction back and reports
  the rollback's answer; it carries no `:committed` key at all, so
  `(not (:committed commit))` answered `true` for a FAILED rollback as
  readily as for a successful one — a constant no unrestored tree could move,
  published beside prose that claimed the rollback happened. Absent still
  falls back; an explicit `false` is now the answer.

  `mutated?` is the transaction kernel's OWN write-boundary answer, handed in
  by the caller that holds it, and it is a THIRD question again. `refusal`
  hardcoded `mutation_attempted false` for every refusal this verb publishes,
  so a post-write refusal over twelve written files told a caller that gates
  its retry on that field to retry over a mid-migration workspace. It is not a
  restatement of `source_unchanged` either: a rollback that SUCCEEDS restores
  the tree, and the kernel still crossed its write boundary to get there. The
  argument is required rather than defaulted, because a default is how the
  constant came back.

  `next_action` answers a fourth question — is this tree safe to send another
  request over — so it follows `source_unchanged` and not `mutated?`. The
  hardcoded `correct_request` told an automated caller to re-send an alias
  migration across twelve already-migrated files, beside a `remedy` in the
  same map that says the tree is MID-MIGRATION and to undo it by hand. A rollback
  that SUCCEEDED is the other side of it: that branch deletes the orphan
  receipt, so `review_receipt` there would name a file that no longer exists.

  @spec MCP-OP-ALIAS-047
  @spec MCP-OP-ALIAS-056"
  [plan commit mutated?]
  (let [source-unchanged (cond
                           (contains? commit :source-unchanged)
                           (:source-unchanged commit)

                           (contains? commit :source_unchanged)
                           (:source_unchanged commit)

                           :else (not (:committed commit)))]
    ;; @spec MCP-OP-ALIAS-059
    ;; forwarded-refusal-kind: the kernel's own error-type travels VERBATIM
    ;; rather than being renamed to a constant, so every kind the transaction
    ;; kernel can mint is a kind this verb's text block must render. It is the
    ;; one site in the entrance's reachable set where the kind is not a
    ;; literal, and it invents nothing: the kinds it forwards are minted, and
    ;; scanned, in the kernel's own sources.
    (refusal (or (some-> (or (:error-type commit) (:error_type commit)) name)
                 "alias-migration-transaction-refused")
             (or (:error commit) "The alias migration transaction refused")
             (cond-> {:files (get-in plan [:totals :files])
                      :sites (get-in plan [:totals :sites])
                      :source_unchanged (boolean source-unchanged)
                      ;; @spec MCP-OP-ALIAS-056
                      :mutation_attempted (boolean mutated?)
                      :next_action (if source-unchanged
                                     "correct_request"
                                     "review_receipt")
                      :remedy (or (:remedy commit)
                                  (str "Re-send the same alias_migration request;"
                                       " the frozen snapshot is recomputed from"
                                       " current source."))}
               (:change-id commit) (assoc :change_id (str (:change-id commit)))
               ;; @spec MCP-OP-ALIAS-054
               ;; a guard asked before and after the directory exists has two
               ;; answers, and the caller is told which one refused it
               (:phase commit) (assoc :phase (:phase commit))
               ;; @spec MCP-OP-ALIAS-056
               ;; a refusal that attempted a rollback owes the caller the
               ;; facts about the tree it left, and an allowlist that computes
               ;; them and drops them tells the caller nothing
               (contains? commit :rolled-back)
               (assoc :rolled_back (boolean (:rolled-back commit)))

               (contains? commit :files-still-migrated)
               (assoc :files_still_migrated (:files-still-migrated commit))

               ;; @spec MCP-OP-ALIAS-056
               ;; the other half of the same measurement: a caller told six
               ;; files are still migrated cannot tell whether the rollback
               ;; reached the other six or never got to them
               (contains? commit :files-restored)
               (assoc :files_restored (:files-restored commit))

               (:receipt-file commit)
               (assoc :receipt_file (str (:receipt-file commit)))

               ;; @spec MCP-OP-ALIAS-056
               ;; the control directory the guard found, named on the wire
               (:control_directory commit)
               (assoc :control_directory (:control_directory commit))))))

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
        receipt-name (new-receipt-name)
        ;; @spec MCP-OP-ALIAS-056
        receipt-file (configured-receipt-file project-root receipt-dir)
        control (receipt-dir-in-control-directory project-root receipt-dir)]
  (cond
    ;; @spec MCP-OP-ALIAS-054
    ;; @spec MCP-OP-ALIAS-056
    (receipt-dir-escapes? project-root receipt-dir)
    {:error (str "The configured receipt directory has no identity this verb "
                 "may use: the part of it that does not exist yet climbs above "
                 "the nearest directory that does, or a relative receipt "
                 "directory resolves outside the workspace root")
     :error-type :alias-migration-receipt-dir-escapes
     :source-unchanged true
     :remedy (str "Configure receipt-dir as a path whose missing components "
                  "descend from an existing directory, and whose relative form "
                  "stays inside " (str project-root) "; a receipt directory "
                  "outside the workspace is legal when it is named absolutely, "
                  "and a relative path that leaves through a symlink is not "
                  "the directory the caller asked for.")}

    ;; @spec MCP-OP-ALIAS-056
    control
    ;; @spec MCP-OP-ALIAS-056
    ;; not "the workspace's": the segment scan that makes this refusal
    ;; reachable is the one for a control directory OUTSIDE the root — a
    ;; linked worktree's real .git lives under the MAIN repository
    {:error (str "The configured receipt directory lies inside a "
                 control " directory, which belongs to another tool")
     :error-type :alias-migration-receipt-dir-in-control-directory
     :source-unchanged true
     :control_directory control
     :remedy (str "Configure receipt-dir outside "
                  (str/join ", " (sort control-directories))
                  "; a receipt published into one of these is a file the "
                  "owning tool reads — an undo receipt in .git/refs/heads is a "
                  "ref git cannot parse — or a file its owner deletes without "
                  "notice.")}

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
     ;; @spec MCP-OP-ALIAS-059
     ;; forwarded-refusal-kind: the retire resolution's own kind, minted and
     ;; scanned in this namespace, travels verbatim rather than being renamed
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
    (let [created (create-receipt-directory! receipt-file)
          ;; @spec MCP-OP-ALIAS-056
          ;; resolved ONCE: every write below goes through this path and never
          ;; through the configured one again
          real-dir (real-directory receipt-file)
          undo-creation! (fn [] (doseq [^Path path created]
                                  (.delete (.toFile path))))]
     (cond
      (nil? real-dir)
      (do
        (undo-creation!)
        {:error (str "No directory exists at the configured receipt directory "
                     "after creation, so this verb cannot prove where a receipt "
                     "would be published")
         :error-type :alias-migration-receipt-dir-escapes
         :phase "post-create"
         :source-unchanged true
         :remedy (str "Configure receipt-dir as a path this server may create "
                      "a directory at; nothing occupying that name may be a "
                      "regular file.")})

      (receipt-detail-collision? project-root real-dir receipt-name)
      (do
        (undo-creation!)
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

      ;; @spec MCP-OP-ALIAS-056
      (control-directory-of project-root real-dir)
      (do
        (undo-creation!)
        {:error (str "The receipt directory that now exists lies inside a "
                     (control-directory-of project-root real-dir)
                     " directory, which belongs to another tool")
         :error-type :alias-migration-receipt-dir-in-control-directory
         :phase "post-create"
         :source-unchanged true
         :control_directory (control-directory-of project-root real-dir)
         :remedy (str "Configure receipt-dir outside "
                      (str/join ", " (sort control-directories))
                      ", and check what is creating symlinks there.")})

      :else
      (let [profile (selected-profile verification-profiles verify)
        baseline (when profile
                   (change-buffer/capture-verification-baseline!
                     project-root profile verification-profiles files))]
    (if (and baseline (not (:ok baseline)))
      ;; @spec MCP-OP-ALIAS-028
      ;; the cause, and the one correction the caller can execute. A baseline
      ;; that cannot read its analyzer's answer is not transient, so the
      ;; generic "re-send the same request" remedy is exactly wrong here: the
      ;; E-CALLER arm re-sent it, reproduced the refusal, and succeeded only
      ;; when it dropped `verify` — which nothing in the receipt suggested.
      {:error "Verification baseline capture failed before the alias migration"
       :error-type (or (:error-type baseline) :verification-baseline-failed)
       :verification baseline
       :verification_profile verify
       :verification_command (some :command (remove :ok (:checks baseline)))
       :source-unchanged true
       :remedy (str "The migration itself is unaffected — nothing was written "
                    "— and `verify` is opt-in. Send the next_call, which is "
                    "this same request with the \"" verify "\" profile "
                    "dropped, or configure a profile whose diagnostic command "
                    "answers EDN this server can read. Re-sending this request "
                    "unchanged reproduces this refusal.")}
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
                      ;; @spec MCP-OP-ALIAS-056
                      ;; the PROVED path, never the configured one
                      :receipt-out (str (.resolve real-dir ^String receipt-name))
                      :on-write-boundary (when attempted
                                           #(vreset! attempted true))
                      :write-refusal-context {:operation "alias_migration"
                                              :project-root project-root}})
            ;; @spec MCP-OP-ALIAS-056
            ;; resolved ONCE, before anything else touches the tree: asking
            ;; twice would let the answer change between the test and the
            ;; branch that reports it
            ;; only a COMMITTED result published a receipt; a kernel result
            ;; that did not commit has nothing to prove and nothing to undo
            fault (when (and (not (:error result)) (:committed result))
                    (receipt-publication-fault real-dir (:receipt-file result)))]
        (cond
          (:error result)
          result

          ;; @spec MCP-OP-ALIAS-056
          ;; A name is resolved on every open, so the identity proved a moment
          ;; ago is not a guarantee about where the bytes went. Where they went
          ;; is a fact, and it is read here from the file that now exists. A
          ;; receipt somewhere else is rolled back, not reported as a success:
          ;; the OS leaves the window open, and what must never happen is a
          ;; verb reporting ok over it.
          fault
          (let [rollback (transaction/execute-undo!
                           {:receipt (:receipt-file result)})
                rolled-back? (undo-restored-the-migration? rollback)
                ;; @spec MCP-OP-ALIAS-056
                ;; the count the prose repeats is the MEASURED one, read back
                ;; out of the report rather than counted off the plan twice
                report (rollback-report rolled-back? rollback
                                        (:receipt-file result) (count files))
                count-migrated (:files-still-migrated report)]
            (when rolled-back?
              (.delete (io/file (:receipt-file result))))
            (merge
              {:error (str (if (= :receipt-not-undoable fault)
                             (str "The undo receipt resolved onto a name no "
                                  "undo can read — a published receipt must "
                                  "still be an .edn file after resolution")
                             (str "The undo receipt was published outside the "
                                  "receipt directory whose identity this verb "
                                  "proved"))
                           "; "
                           (rollback-sentence rolled-back?
                                              (:receipt-file result)
                                              count-migrated))
               :error-type :alias-migration-receipt-published-elsewhere
               :phase "post-write"
               :remedy (str "Check what is replacing " (str real-dir)
                            " while this verb runs; a receipt this verb cannot "
                            "locate is a receipt no undo can be trusted to find."
                            (when-not rolled-back?
                              (str " The tree is MID-MIGRATION: undo it by hand "
                                   "from the receipt named in receipt_file.")))}
              report))

          :else
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
                    rolled-back? (undo-restored-the-migration? rollback)
                    ;; @spec MCP-OP-ALIAS-056
                    report (rollback-report rolled-back? rollback
                                            (:receipt-file result)
                                            (count files))
                    count-migrated (:files-still-migrated report)]
                ;; the same discipline as the verification-failure branch: an
                ;; undo receipt for a transaction that has already been undone
                ;; would invite a second, destructive undo
                (when rolled-back?
                  (.delete (io/file (:receipt-file result))))
                (merge
                  {:error (str "The superseded defining file could not be "
                               "retired; "
                               (rollback-sentence rolled-back?
                                                  (:receipt-file result)
                                                  count-migrated))
                   :error-type :alias-migration-retire-failed
                   :cause-error (:retire-error retired)}
                  report))

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
                        rolled-back? (undo-restored-the-migration? rollback)
                        ;; @spec MCP-OP-ALIAS-056
                        report (rollback-report rolled-back? rollback
                                                (:receipt-file result)
                                                (count files))
                        count-migrated (:files-still-migrated report)]
                    (when rolled-back?
                      (.delete (io/file (:receipt-file result))))
                    (merge
                      ;; @spec MCP-OP-ALIAS-028
                      ;; the same rule as the baseline branch: a profile that
                      ;; reported a failure reports it again on an identical
                      ;; re-send, so the generic "re-send the same request"
                      ;; remedy would be a retry loop. The rollback restored
                      ;; the tree, so the executable correction is the same
                      ;; request without the profile — which is what this
                      ;; refusal's next_call carries.
                      {:error (str "Verification failed; "
                                   (rollback-sentence rolled-back?
                                                      (:receipt-file result)
                                                      count-migrated))
                       :error-type (or (:error-type verification)
                                       :verification-failed)
                       :verification verification
                       :verification_profile verify
                       :remedy (str "The \"" verify "\" profile reported a "
                                    "failure and the migration was rolled "
                                    "back, so re-sending this request "
                                    "unchanged reports it again. Send the "
                                    "next_call — this same request with the "
                                    "profile dropped — or correct what the "
                                    "profile reported and send this request "
                                    "again after that.")}
                      report))))))))))))))))

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
              ;; @spec MCP-OP-ALIAS-056
              ;; the kernel's own write boundary, not a literal: the same
              ;; volatile ALIAS-047's heap guard reads
              (cond-> (commit-refusal plan commit @attempted)
                ;; @spec MCP-OP-ALIAS-028
                ;; a baseline failure has exactly one executable correction,
                ;; and it is composed here because this is where the REQUEST
                ;; is: the same call without the profile that could not be read
                (and (:verification commit) verify)
                (assoc :next_call (planner/unverified-call request)))
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
