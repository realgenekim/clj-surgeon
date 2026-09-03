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
   [clj-surgeon.mcp-paths :as mcp-paths]
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


;; @spec MCP-OP-EXTRACT-032
(def ^:private skipped-workspace-directories
  "Directory names a source walk never descends -- AT THE WORKSPACE ROOT ONLY.

  Build output and vendored trees are not the workspace's own source. `target/`
  in particular holds COPIES of the very files being rewired, and rewiring a
  copy is rewriting build output under a receipt that claims a caller was
  repaired; `.cpcache/` and `node_modules/` are cost with no callers in them.

  Matched at the root and nowhere else, because these are the names of TOP-LEVEL
  build artifacts, while the same words are ordinary namespace segments further
  down: `src/app/out/writer.clj` is `app.out.writer`, a real caller. Matching
  the bare basename anywhere under the tree dropped that namespace silently --
  discovery reported a smaller workspace, no refusal was raised, and the
  receipt still said `complete`."
  #{"target" ".cpcache" "node_modules" "out" ".git"})

(def ^:private default-max-workspace-files
  "How many Clojure sources a workspace may hold before discovery refuses.

  The walk slurps every source it keeps, so an unbounded workspace is an
  unbounded read. Refusing above a stated cap is a plan; discovering how large
  a repository is by running out of heap is not."
  2000)

(def ^:private max-workspace-file-bytes
  "The largest source discovery will slurp. Larger files are skipped and named
  in the receipt rather than read: a caller nobody can hand-edit is not the
  caller this verb is for, and reading it costs the whole file."
  (* 512 1024))

;; @spec MCP-OP-EXTRACT-029
(defn- walk-workspace-sources
  "Every Clojure source under `root`, WITHOUT following directory symlinks.

  `file-seq` follows them: one `app/loop -> app` made the walk rediscover every
  source once per level until the kernel's symlink limit stopped it, and each
  rediscovery was a separate caller plan writing the same file again. This walk
  classifies each entry with NOFOLLOW and descends only into real directories.

  A symbolic link is never descended. One whose real target is INSIDE the root
  is already reachable by its real path, so it is skipped rather than
  duplicated; one that resolves OUTSIDE the root is surfaced as `:escape`, so
  the caller refuses rather than dropping it quietly.

  A skip-list name is a build tree only at the workspace ROOT; deeper down it
  is a namespace segment. Every directory the walk declines to enter is NAMED
  in `:skipped-directories`, so an incomplete scan is visible rather than
  inferred from a smaller file count.

  Returns one of
  `{:files [...] :skipped-large [...] :skipped-directories [...]}`,
  `{:escape <path>}`, or `{:over-cap <count>}`."
  [^java.io.File root real-root cap]
  (let [no-follow (make-array java.nio.file.LinkOption 0)]
    (loop [stack (mapv (fn [entry] [entry true]) (or (.listFiles root) []))
           files (transient [])
           skipped (transient [])
           skipped-dirs (transient [])
           seen 0]
      (if (empty? stack)
        {:files (persistent! files)
         :skipped-large (persistent! skipped)
         :skipped-directories (persistent! skipped-dirs)}
        (let [[^java.io.File entry root-level?] (peek stack)
              stack (pop stack)
              path (.toPath entry)
              link? (java.nio.file.Files/isSymbolicLink path)
              real (when link?
                     (try (.toRealPath path no-follow)
                          (catch Exception _ nil)))]
          (cond
            ;; a link to a directory is never descended
            (and link? real
                 (java.nio.file.Files/isDirectory real no-follow))
            (if (.startsWith real real-root)
              ;; already reachable by its real path; descending would
              ;; rediscover every source under it a second time
              (recur stack files skipped skipped-dirs seen)
              ;; refused by the caller, never dropped quietly
              {:escape (.getPath entry)})

            ;; a broken link points at nothing to read
            (and link? (nil? real))
            (recur stack files skipped skipped-dirs seen)

            (and (not link?) (.isDirectory entry))
            ;; @spec MCP-OP-EXTRACT-032
            (if (and root-level?
                     (contains? skipped-workspace-directories (.getName entry)))
              (recur stack files skipped
                     (conj! skipped-dirs {:dir (.getPath entry)
                                          :reason :build-tree})
                     seen)
              (recur (into stack
                           (map (fn [child] [child false]))
                           (or (.listFiles entry) []))
                     files skipped skipped-dirs seen))

            (not (re-matches #".*\.clj[sc]?$" (.getName entry)))
            (recur stack files skipped skipped-dirs seen)

            (> (inc seen) (long cap))
            {:over-cap (inc seen)}

            (> (.length entry) (long max-workspace-file-bytes))
            (recur stack files
                   (conj! skipped {:file (.getPath entry)
                                   :bytes (.length entry)})
                   skipped-dirs
                   (inc seen))

            :else
            (recur stack (conj! files (.getPath entry)) skipped skipped-dirs
                   (inc seen))))))))

;; @spec MCP-OP-EXTRACT-024
(defn- confine-workspace-paths
  "Refuse the FIRST workspace path that resolves outside the project root.

  Returns nil when every path is confined, or one typed refusal naming the
  offending path. Called twice on purpose -- once where the walk turns the
  workspace into a read set, and once in the instant before the first
  `atomic-write!` -- because rewiring turns that read set into a WRITE set and
  the filesystem can change between proving a plan and committing it. A path
  that escapes is never dropped quietly: silently skipping a caller would ship a
  half-rewired workspace under a success receipt."
  [root paths]
  (let [real (try {:root (mcp-paths/real-root root)}
                  (catch Exception error {:error (.getMessage error)}))]
    (if-let [root-error (:error real)]
      {:ok false
       :error (str "The extraction project root could not be resolved: "
                   root-error)
       :error-type :project-root-unresolvable
       :root (str root)
       :source-unchanged true
       :target-unchanged true}
      (let [real-root (:root real)]
        (some (fn [path]
                (let [resolved (mcp-paths/resolve-discovered-source-path
                                 real-root path)]
                  (when-not (:ok resolved)
                    {:ok false
                     :error (str "A workspace path resolves outside the "
                                 "extraction project root and was refused "
                                 "before any write: " path)
                     :error-type :caller-path-outside-root
                     :path (str path)
                     :root (.toString real-root)
                     :refusal (select-keys resolved [:error_type :error])
                     :source-unchanged true
                     :target-unchanged true})))
              paths)))))

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
;; @spec MCP-OP-EXTRACT-030
(defn read-back-verified?
  "Did every file this transaction wrote read back byte-identical?

  `expected` is [[path content] ...]; `reader` is the only I/O and is passed in
  so the answer is a computed comparison rather than a constant. The receipt's
  `:read-back` used to be the literal `true`, which is a claim no evidence
  supports: a flag that cannot be false is not a verification, it is a label."
  [expected reader]
  (every? (fn [[path content]] (= content (reader path))) expected))

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
   :source-require-added :rewire-callers :verified :receipt-file :undo
   ;; @spec MCP-OP-EXTRACT-029
   :discovery])

(defn- compile-expr-for
  "Pure: the expression that requires every touched namespace."
  [namespaces]
  (str "(require " (str/join " " (map #(str "'" %) namespaces))
       ") (println :compile-ok)"))

;; @spec MCP-OP-EXTRACT-021
(defn normalize-aliases
  "Pure: a clean vector of alias names, with any leading colon removed.

  Accepts a single alias or a collection, so a caller may pass `:test`,
  \"test\" or [\"a\" \"b\"] and get the same answer."
  [aliases]
  (->> (cond (nil? aliases) []
             (or (string? aliases) (keyword? aliases)) [aliases]
             :else aliases)
       (map #(str/replace (str (if (keyword? %) (name %) %)) #"^:" ""))
       (remove str/blank?)
       vec))

;; @spec MCP-OP-EXTRACT-021
(defn- alias-flag
  "Pure: the -A flag for a set of aliases, or nil when there are none."
  [aliases]
  (when-let [names (seq (normalize-aliases aliases))]
    (str "-A:" (str/join ":" names))))

(defn- classpath-args-for
  "Pure: the argv that resolves the project classpath.

  Two steps, not `clojure -M<alias> -e`: an alias may carry :main-opts, and on
  this very repository `-M:clj-surgeon/mcp-test -e ...` runs the whole test
  suite instead of compiling. Resolve the classpath, then run clojure.main
  against it."
  [aliases]
  (cond-> ["clojure" "-Spath"]
    (alias-flag aliases) (conj (alias-flag aliases))))

;; @spec MCP-OP-EXTRACT-021
;; @spec MCP-OP-EXTRACT-027
(defn declared-compile-config
  "The compile declaration governing one workspace, and the file it came from.

  The search is BOUNDED at `root`: a `.clj-surgeon.edn` above the workspace
  governs somebody else's tree, and letting it choose this compile's classpath
  aliases would let a directory the operator never named put its own
  `:extra-paths` first on a classpath that then runs workspace code."
  [root]
  (let [config (forms/project-config root root)]
    {:aliases (normalize-aliases (some-> config :compile :aliases))
     :config-file (:config-file config)}))

;; @spec MCP-OP-EXTRACT-021
(defn declared-compile-aliases
  "The aliases a workspace declares for compiling itself, from its own
  `.clj-surgeon.edn` under `{:compile {:aliases [\"...\"]}}`.

  A workspace knows which alias puts its test-only dependencies on the
  classpath; the tool cannot know it. Declaring it is how a correct extraction
  stops being handed a command that is red for a reason it did not cause."
  [root]
  (:aliases (declared-compile-config root)))

;; @spec MCP-OP-EXTRACT-022
(defn candidate-compile-aliases
  "Pure-ish: deps.edn aliases that add a `test` path, as a fallback guess.

  A guess is only useful if it is determinate, so these are returned sorted and
  the caller applies the FIRST one; the receipt says it guessed."
  [root]
  (let [f (io/file (str root) "deps.edn")]
    (when (.exists f)
      (try
        (->> (:aliases (edn/read-string (slurp f)))
             (keep (fn [[alias-name spec]]
                     (when (some #(re-find #"(^|/)test(/|$)" (str %))
                                 (concat (:extra-paths spec) (:paths spec)))
                       (str/replace (str alias-name) #"^:" ""))))
             sort
             vec
             seq)
        (catch Exception _ nil)))))

;; @spec MCP-OP-EXTRACT-020
(defn attribute-compile-failure
  "Pure: decide whether a failed compile is attributable to THIS change.

  A receipt whose evidence source cannot see its own subject must say
  `:unverified`, never `false`. A missing dependency, or an error raised inside
  a namespace this extraction never touched, says the classpath could not load
  the project at all -- reporting that as `:ok false` would tell a reader to
  undo work that is in fact correct."
  [output touched-files]
  (let [named (set (map second
                        (re-seq #"\(([A-Za-z0-9_.\-/]+\.clj[cs]?):" output)))
        ours (set (map #(str/replace (str %) "\\" "/") touched-files))
        ;; @spec MCP-OP-EXTRACT-028
        ;; Path suffix, never basename. `vendor/core.clj` and `src/app/core.clj`
        ;; share a basename and nothing else; treating them as the same file is
        ;; how a failure raised in a namespace this extraction never touched
        ;; became `:ok false` and told a reader to revert correct work. A name
        ;; carrying no directory at all cannot be attributed to anything, so it
        ;; is not attributed: unattributable evidence reports `:unverified`.
        ours? (fn [candidate]
                (and (str/includes? candidate "/")
                     (boolean
                       (some #(or (= % candidate)
                                  (str/ends-with? % (str "/" candidate)))
                             ours))))
        foreign (seq (remove ours? named))]
    (cond
      (re-find #"Could not locate .* on classpath" output)
      {:ok :unverified :reason :classpath-incomplete}

      foreign
      {:ok :unverified :reason :failure-outside-the-changed-files
       :raised-in (vec (sort foreign))}

      :else {:ok false})))

(def ^:private classpath-placeholder
  "The token standing for the resolved classpath in the published argv.

  The compile check is two processes: the first PRINTS a classpath and the
  second consumes it, so a receipt written before either has run has to name
  the second's third argument somehow. Nothing ever substitutes a shell
  variable here -- no element of either vector reaches a shell."
  "$CLASSPATH")

;; @spec MCP-OP-EXTRACT-025
(defn compile-argv-for
  "Pure: the two argv VECTORS the compile check executes, in order.

  Vectors, never a shell string. The alias and the namespace names are
  workspace-controlled text: a `;` inside an alias turns a printed shell string
  into a command that executes when a reader pastes it, and the rf2 red team
  created a file that way. As argv each is exactly one token and `ProcessBuilder`
  consults no shell, so the record a receipt publishes and the record the apply
  runs are the same object."
  ([namespaces] (compile-argv-for namespaces nil))
  ([namespaces aliases]
   [(vec (classpath-args-for aliases))
    ["java" "-cp" classpath-placeholder "clojure.main" "-e"
     (compile-expr-for namespaces)]]))

;; @spec MCP-OP-EXTRACT-025
(defn shell-quote-token
  "Pure: one POSIX-shell word that reproduces `s` byte for byte.

  Single quotes, with an embedded single quote spliced as backslash-escaped:
  inside single quotes a POSIX shell interprets nothing at all, so there is no
  escape sequence left to get wrong."
  [s]
  (str "'" (str/replace (str s) "'" "'\\''") "'"))

;; @spec MCP-OP-EXTRACT-025
(defn- compile-command-shell
  "Pure: a pasteable rendering of the SAME two argv vectors, every token
  shell-quoted. A convenience for a reader at a prompt; `:command` stays the
  executable record."
  ([namespaces] (compile-command-shell namespaces nil))
  ([namespaces aliases]
   (let [[classpath-argv run-argv] (compile-argv-for namespaces aliases)]
     (str "CP=$(" (str/join " " (map shell-quote-token classpath-argv)) ")"
          " && "
          (str/join " " (map #(if (= classpath-placeholder %)
                                "\"$CP\""
                                (shell-quote-token %))
                             run-argv))))))

;; @spec MCP-OP-EXTRACT-025
(defn- compile-command-fields
  "Pure: the two command keys a receipt publishes for one compile check."
  [namespaces aliases]
  {:command (compile-argv-for namespaces aliases)
   :command_shell (compile-command-shell namespaces aliases)})

;; @spec MCP-OP-EXTRACT-026
(def ^:private compile-trust-fields
  "What the compile check costs a reader who did not choose it.

  A receipt that reports a compile without saying whose code ran lets an agent
  treat `:ok true` as free. It is not free: requiring a namespace runs the
  workspace's top-level forms. The flag is published on every surface -- dry
  run, apply, and the not-run branch -- so the boundary is visible before the
  subprocess starts, not only in a docstring nobody reads at a receipt."
  {:runs-workspace-code true
   :opt-out (str "pass :compile-check false for a repository you did not "
                 "author; the extraction still applies and the receipt reports "
                 ":status :not-run instead of claiming a verification")})

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
  the running process would mutate the very namespaces a server is serving.

  TRUST BOUNDARY. `(require 'the.ns)` EXECUTES this workspace's code -- every
  top-level form in every touched namespace and everything they require, with
  the chosen alias's `:extra-paths` first on the classpath, in a subprocess with
  this process's user and filesystem. That is a deliberate default, kept because
  an unchecked apply is a receipt that proves nothing, and it is the right
  default for a repository the operator wrote. For a repository the operator did
  NOT write, pass `:compile-check false`: the extraction completes and the
  receipt reports `:status :not-run` with `:runs-workspace-code false` rather
  than claiming a verification it did not perform. The subprocess is bounded by
  `timeout-ms` and an output limit; it is not a sandbox."
  [{:keys [namespaces root timeout-ms aliases touched-files config-file]
    :or {timeout-ms 30000}}]
  (let [aliases (normalize-aliases aliases)
        base (cond-> (merge {:namespaces (vec namespaces)}
                            ;; @spec MCP-OP-EXTRACT-026
                            compile-trust-fields
                            (compile-command-fields namespaces aliases))
               (seq aliases) (assoc :aliases aliases)
               ;; @spec MCP-OP-EXTRACT-027
               config-file (assoc :config-file config-file))
        run! (fn [argv ms]
               (try
                 (process-env/run-bounded!
                   {:command argv :cwd (str root) :timeout-ms ms
                    :visible-byte-limit (* 256 1024)})
                 (catch Exception error {:launch-error (.getMessage error)})))]
    (if-not (project-build-file root)
      (assoc base :checked false :status :skipped
             :reason :no-project-build-file)
      (let [cp (run! (classpath-args-for aliases) timeout-ms)
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
          (let [attributed (attribute-compile-failure output touched-files)]
            (merge base
                   {:checked true :status :run :exit (:exit result)
                    :output-tail tail}
                   attributed
                   ;; @spec MCP-OP-EXTRACT-022
                   ;; The classpath could not load the project and this
                   ;; workspace declared no aliases. Hand back a command that
                   ;; is determinate rather than one that is known-red: the
                   ;; agent's next call must not be a guess it has to invent.
                   (when (and (= :classpath-incomplete (:reason attributed))
                              (empty? aliases))
                     (let [candidates (candidate-compile-aliases root)]
                       (cond-> {:candidate-aliases (vec candidates)}
                         (seq candidates)
                         (-> (merge (compile-command-fields
                                      namespaces [(first candidates)]))
                             (assoc :guessed true
                                    :declare-instead
                                    (str "declare the right one in "
                                         ".clj-surgeon.edn as "
                                         "{:compile {:aliases [\"<alias>\"]}}")))))))))))))

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

;; @spec MCP-OP-EXTRACT-033
(defn scan-gap
  "Pure: one `callers-unresolved` entry for a workspace scan that did not read
  every Clojure source, or nil when it did.

  `:complete` is the field an agent reads. Leaving a skipped source to be
  inferred from `:discovery :skipped-large` publishes a receipt that says the
  rewire is complete while a caller nobody read still requires the source
  namespace, so the gap is stated where the completeness verdict is made.

  A DECLARED build-tree exclusion is not a gap: `target/`, `out/`, `.cpcache/`,
  `node_modules/` and `.git/` at the root are named in `:discovery`, and the
  same reasoning that refuses to rewire a build copy says an unread build copy
  is not an outstanding caller. Every other skipped directory is a gap, because
  nothing proved what was inside it. Making `:complete` false on every git
  repository would retire the flag as surely as leaving it true here."
  [discovery]
  (let [large (mapv :file (:skipped-large discovery))
        dirs (->> (:skipped-directories discovery)
                  (remove #(= :build-tree (:reason %)))
                  (mapv :dir))]
    (when (or (seq large) (seq dirs))
      (cond-> {:file :workspace-scan
               :reason :workspace-scan-incomplete
               :remedy (str "discovery did not read every Clojure source under "
                            "the root, so a caller in one of these may still "
                            "require the source namespace; read them by hand, "
                            "or raise :max-workspace-file-bytes and re-run")}
        (seq large) (assoc :sources-too-large large)
        (seq dirs) (assoc :directories-not-read dirs)))))

;; @spec MCP-OP-EXTRACT-015
;; @spec MCP-OP-EXTRACT-016
;; @spec MCP-OP-EXTRACT-033
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
        ;; @spec MCP-OP-EXTRACT-033
        ;; an unread source is an unresolved caller, stated where the
        ;; completeness verdict is made rather than left in :discovery
        unresolved (cond-> (vec (:unresolved classification))
                     (scan-gap (:discovery plan))
                     (conj (scan-gap (:discovery plan))))
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
                      ;; @spec MCP-OP-EXTRACT-021
                      ;; the dry run prints the command the apply will RUN,
                      ;; aliases included; a preview whose command differs from
                      ;; the one that executes is a second thing to learn
                      (cond-> (merge
                                {:checked false
                                 :status :not-run
                                 :will-check (boolean (:_will-check plan))
                                 :namespaces namespaces}
                                ;; @spec MCP-OP-EXTRACT-026
                                compile-trust-fields
                                (compile-command-fields
                                  namespaces (:_compile-aliases plan)))
                        (seq (:_compile-aliases plan))
                        (assoc :aliases (:_compile-aliases plan))

                        ;; @spec MCP-OP-EXTRACT-027
                        (:_compile-config-file plan)
                        (assoc :config-file (:_compile-config-file plan))))
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
           project-root compile-aliases compile-config-file discovery]
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
      ;; @spec MCP-OP-EXTRACT-031
      ;; Checked here, not only in the rewriter: with `rewire-callers false`
      ;; the alias never reaches the rewriter and goes straight into the source
      ;; header as `[target :as <alias> :refer [...]]`.
      (and (some? alias) (extract-rewire/invalid-alias? alias))
      {:error (str "alias must be one simple Clojure symbol -- no whitespace, "
                   "no `/`, no reader delimiter, no leading digit -- and it is "
                   "written into a namespace require")
       :error-type :invalid-rewire-alias
       :alias alias
       :source-unchanged true
       :target-unchanged true}

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
           ;; @spec MCP-OP-EXTRACT-021
           :_compile-aliases (normalize-aliases compile-aliases)
           ;; @spec MCP-OP-EXTRACT-027
           :_compile-config-file compile-config-file
           ;; @spec MCP-OP-EXTRACT-029
           :discovery discovery
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
           alias rewire-callers compile-alias max-workspace-files]
    :as opts
    :or {require-policy :minimal rewire-callers true}}]
  (try
    (let [source (slurp file)
          target-ns (file-path->ns-name to source-paths)
          project-root (project-root-for-source file source-paths)
          source-canonical-path (.getCanonicalPath (io/file file))
          cap (or max-workspace-files default-max-workspace-files)
          ;; @spec MCP-OP-EXTRACT-029
          walked (walk-workspace-sources (io/file project-root)
                                         (mcp-paths/real-root project-root)
                                         cap)
          discovered
          (->> (:files walked)
               (remove #(= source-canonical-path
                           (.getCanonicalPath (io/file %))))
               vec)
          ;; @spec MCP-OP-EXTRACT-024
          ;; Confine the read set at the moment the walk produces it: this is
          ;; where a directory symlink turns a path that LOOKS like it is under
          ;; the root into one that is not.
          escape (or (when-let [escaped (:escape walked)]
                       (confine-workspace-paths project-root [escaped]))
                     (confine-workspace-paths project-root discovered))
          over-cap (:over-cap walked)
          explicit-compile-aliases (not-empty (normalize-aliases compile-alias))
          declared-compile (declared-compile-config project-root)
          workspace-sources
          (when-not (or escape over-cap)
            (into (sorted-map)
                  (map (fn [path] [path (slurp path)]) discovered)))]
      (cond
        ;; @spec MCP-OP-EXTRACT-029
        over-cap
        {:ok false
         :error (str "This workspace holds more than " cap
                     " Clojure sources; discovery reads every one of them.")
         :error-type :workspace-file-cap-exceeded
         :cap cap
         :seen over-cap
         :remedy (str "extract from a smaller root, or raise the cap with "
                      ":max-workspace-files <n>")
         :source-unchanged true
         :target-unchanged true}

        escape escape

        :else
        (compile-plan
          {:file file
           :source source
           :forms forms
           :to to
           :target-ns target-ns
           :workspace-sources workspace-sources
           :require-policy require-policy
           :project-root (str project-root)
           :compile-aliases (or explicit-compile-aliases
                                (:aliases declared-compile))
           ;; @spec MCP-OP-EXTRACT-027
           :compile-config-file (when-not explicit-compile-aliases
                                  (:config-file declared-compile))
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
           :rewire-callers rewire-callers
           ;; @spec MCP-OP-EXTRACT-029
           ;; @spec MCP-OP-EXTRACT-032
           :discovery (let [large (:skipped-large walked)
                            dirs (:skipped-directories walked)]
                        (cond-> {:files (count (:files walked))}
                          (seq large) (assoc :skipped-large (vec large))
                          (seq dirs) (assoc :skipped-directories (vec dirs))))})))
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
            ;; @spec MCP-OP-EXTRACT-024
            ;; A delay, so the confinement check runs exactly where the cond
            ;; forces it -- after every staleness fence and before the first
            ;; write -- and runs once.
            ;; @spec MCP-OP-EXTRACT-030
            verified (volatile! {:parsed false :atomic-write false
                                 :read-back false})
            caller-escape (delay (confine-workspace-paths
                                   (:_project-root p)
                                   (map :file caller-plans)))
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

          ;; @spec MCP-OP-EXTRACT-024
          ;; The last thing checked before the first byte is written: every
          ;; caller path is re-confined against the project root here, not only
          ;; where the walk found it, because the plan was proved against a
          ;; filesystem that has had every preceding check's worth of time to
          ;; change.
          @caller-escape @caller-escape

          :else
          (do
            (.mkdirs (.getParentFile (.getAbsoluteFile target-file)))
            (try
              ;; @spec MCP-OP-EXTRACT-030
              ;; Every flag the receipt publishes under :verified is computed
              ;; here, in the order the transaction earns it. `verified` starts
              ;; with all three false and is raised one step at a time, so a
              ;; failure carries exactly what HAD been proved when it happened.
              (let [expected-bytes
                    (into [[(.getPath target-file) new-content]
                           [(.getPath source-file) updated-source]]
                          (map (fn [{:keys [file source]}] [file source])
                               caller-plans))]
                (vreset! verified (assoc @verified
                                         :parsed (true? (:ok candidates))))
                (file-ops/atomic-write! target-file new-content)
                (file-ops/atomic-write! source-file updated-source)
                (doseq [{:keys [file source]} caller-plans]
                  (file-ops/atomic-write! (io/file file) source))
                (vreset! verified (assoc @verified :atomic-write true))
                (vreset! verified
                         (assoc @verified
                                :read-back
                                (read-back-verified?
                                  expected-bytes
                                  (fn [path] (slurp (io/file path))))))
                (when-not (:read-back @verified)
                  (throw (ex-info "Extraction read-back verification failed"
                                  {:error-type :extraction-read-back-failed
                                   :verified @verified}))))
              (let [receipt-file (publish-receipt! receipt-out receipt)
                    ;; @spec MCP-OP-EXTRACT-019
                    ;; The last step of the transaction: compile what was
                    ;; written. A failure is reported honestly and NOT
                    ;; auto-reverted -- the receipt names how to revert.
                    compile-result
                    (if (false? (:compile-check opts))
                      ;; @spec MCP-OP-EXTRACT-026
                      {:checked false :status :not-run
                       :namespaces (:_touched-namespaces p)
                       :runs-workspace-code false
                       :reason :disabled-by-caller}
                      (compile-check!
                        {:namespaces (:_touched-namespaces p)
                         :aliases (or (not-empty
                                        (normalize-aliases
                                          (:compile-alias opts)))
                                      (:_compile-aliases p))
                         :config-file (:_compile-config-file p)
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
                              :parsed (:parsed @verified)
                              :atomic-write (:atomic-write @verified)
                              :read-back (:read-back @verified)}
                   :source-require-added (boolean (seq source-referred-forms))}
                  (:discovery p) (assoc :discovery (:discovery p))

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
                                   ;; @spec MCP-OP-EXTRACT-030
                                   :verified @verified
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
