(ns clj-surgeon.mcp-helper-extraction
  "Request boundary for the `helper_extraction` MCP verb.

  This namespace owns the I/O half — the closed-field validation, the admitted
  discovery roots, the confined scan and frozen read, the verification-profile
  preflight, one guarded transaction through the extraction kernel, the
  fresh-process proof, and the terminal receipt. It knows nothing about the
  rewrite itself, which lives in the pure `clj-surgeon.helper-extraction`
  planner, exactly as `mcp-alias-migration` stands to `alias-migration`.

  Contract of record: docs/plans/helper-closure-extraction.md revision 3, the
  EARS registry in docs/intent/helper-extraction/helper-extraction-specs.md
  (prefix MCP-OP-HELPER), and the `Planner and boundary surfaces` section of
  docs/intent/helper-extraction/helper-extraction-design.md.

  WHY `tool` IS A FUNCTION AND RESOLVES TWO VARS LAZILY. `mcp-tool` requires
  THIS namespace to register the verb, so this namespace may not require
  `mcp-tool` at load time. The handler and the refusal summarizer belong to the
  registration layer — they need `resolve-verification-config`, the workspace
  router's receipt directory and the refusal-envelope renderers, and every one
  of those has exactly one home in `mcp-tool`. Duplicating them here is the
  drift `resolve-verification-config`'s own docstring forbids (\"one function,
  both callers, so the two cannot drift apart again\"), so `tool` resolves them
  by name at CALL time instead, when `mcp-tool` is loaded. The same trick reads
  the server's built-in verification profiles out of `mcp-http-server`, which
  requires `mcp-tool`; both are DEBT, and the fix is a leaf namespace owning the
  profile map and the refusal renderers rather than a copy of either here.

  ROLLBACK AUTHORITY. This verb's write is one `extraction` change, and the
  kernel that owns it is `clj-surgeon.mcp-extraction` — `compile-extraction`,
  `commit!`, and the hash-fenced inverse `undo!` — the same guarded path
  `apply_clojure_changes` takes for an extraction. `intent-transaction`'s
  `changes` array cannot carry an extraction change today, so this is not a
  second rollback mechanism but the repository's own. Every post-staging failure
  or THROW exits through `finish-failure!`, which undoes through that inverse
  and reports `rollback-failed` when the inverse does not verify; this namespace
  never restores a byte itself."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.helper-extraction :as planner]
   [clj-surgeon.mcp-alias-migration :as alias-migration]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-extraction :as extraction]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk])
  (:import
   (java.nio.file Files LinkOption Path)
   (java.security MessageDigest)
   (java.util UUID)))

(def operation "helper_extraction")

;; ---------------------------------------------------------------------------
;; refusals
;;
;; Every v1 refusal carries `next_call nil` (MCP-OP-HELPER-010) and offers no
;; scope narrowing, caller exclusion, invented alias or weaker profile
;; (MCP-OP-HELPER-016).

(defn refusal
  "One typed helper_extraction refusal, in the repository's receipt shape."
  ([suffix message] (refusal suffix message {}))
  ([suffix message evidence]
   (merge {:ok false
           :operation operation
           :error_type (str "helper-extraction-" suffix)
           :error message
           :next_call nil
           :source_unchanged true
           :mutation_attempted false
           :write_authority false}
          evidence)))

;; ---------------------------------------------------------------------------
;; request validation
;;
;; @spec MCP-OP-HELPER-002
;; @spec MCP-OP-HELPER-025

(def request-fields
  "The closed field set. Anything else refuses `unknown-field`."
  #{:op :workspace_root :from :helpers :to :scope :verification :expect})

(defn- nonblank-string?
  [value]
  (and (string? value) (not (str/blank? value))))

(defn- string-array?
  [value]
  (and (sequential? value) (seq value) (every? nonblank-string? value)))

(defn- invalid-request
  [message path]
  (refusal "invalid-request" message {:path (vec path)}))

;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-002
;; @spec MCP-OP-HELPER-017
;; @spec MCP-OP-HELPER-025
(defn validate-request
  "Validate one closed helper_extraction request without reading source.

  `expect` is OPTIONAL and absent in normal use; when supplied it is a strict
  guard the planner enforces (MCP-OP-HELPER-017)."
  [params]
  (let [params (walk/keywordize-keys params)
        unknown (vec (sort (map name (remove request-fields (keys params)))))
        {:keys [from helpers to scope verification expect]} params]
    (cond
      ;; @spec MCP-OP-HELPER-002
      ;; @spec MCP-OP-HELPER-025
      ;; the size guarantee is enforced here, not documented: a per-file,
      ;; per-owner or per-site table has no field to arrive in
      (seq unknown)
      (refusal "unknown-field"
               (str "The request carries a field outside the closed set: "
                    (str/join ", " unknown))
               {:unknown_fields unknown
                :closed_fields (vec (sort (map name request-fields)))
                :decision "which of the closed request fields carries this information"})

      (not (and (map? from) (nonblank-string? (:file from))
                (= #{:file} (set (keys from)))))
      (invalid-request "from must be {file}, a project-relative path"
                       ["from"])

      (not (mcp-paths/relative-source-path? (:file from)))
      (invalid-request "from.file must be a project-relative Clojure source path"
                       ["from" "file"])

      (not (and (string-array? helpers)
                (= (count helpers) (count (distinct helpers)))))
      (invalid-request "helpers must be one non-empty array of distinct names"
                       ["helpers"])

      (not (and (map? to) (nonblank-string? (:lib to))
                (string-array? (:alias_policy to))
                (= #{:lib :alias_policy} (set (keys to)))))
      (invalid-request "to must be {lib, alias_policy}; alias_policy is non-empty"
                       ["to"])

      (not (and (map? scope) (string-array? (:paths scope))
                (= #{:paths} (set (keys scope)))))
      (invalid-request "scope.paths must be one non-empty array of glob patterns"
                       ["scope" "paths"])

      (not (and (map? verification) (nonblank-string? (:profile verification))
                (= #{:profile} (set (keys verification)))))
      (invalid-request "verification must be {profile}, naming one admitted profile"
                       ["verification" "profile"])

      ;; @spec MCP-OP-HELPER-017
      (and (some? expect)
           (not (and (map? expect)
                     (= #{:caller_files} (set (keys expect)))
                     (nat-int? (:caller_files expect)))))
      (invalid-request "expect, when supplied, is {caller_files}, a non-negative integer"
                       ["expect"])

      :else
      {:ok true
       :request (cond-> {:op (or (:op params) operation)
                         :workspace_root (:workspace_root params)
                         :from {:file (:file from)}
                         :helpers (vec helpers)
                         :to {:lib (:lib to)
                              :alias_policy (vec (:alias_policy to))}
                         :scope {:paths (vec (:paths scope))}
                         :verification {:profile (:profile verification)}}
                  (some? expect) (assoc :expect {:caller_files (:caller_files expect)}))})))

;; ---------------------------------------------------------------------------
;; the admitted discovery roots
;;
;; @spec MCP-OP-HELPER-005
;; Revision 3 rule 4: v1 roots are explicit and config-bound — `src`, `test`,
;; plus `.clj-surgeon.edn :source-roots` when present. There is no universal
;; project discovery, and `scope.paths` is a WRITE-AUTHORIZATION subset of
;; these roots rather than a substitute for them.

(defn- configured-source-roots
  "`:source-roots` from the workspace's own `.clj-surgeon.edn`, or nil.

  A malformed config is not silently ignored and not thrown either: it answers
  nil, and the caller falls back to the planner's declared roots. Reading a
  root list out of a file this verb cannot parse would make discovery depend on
  a guess."
  [project-root]
  (let [config-file (io/file (str project-root) ".clj-surgeon.edn")]
    (when (.isFile config-file)
      (try
        (let [roots (:source-roots (edn/read-string (slurp config-file)))]
          (when (string-array? roots) (vec roots)))
        (catch Exception _ nil)))))

(defn- admissible-root?
  "Whether one configured root names a directory INSIDE this workspace.

  A root is admissible only as a normalized project-relative path whose real
  location stays under the real workspace root. `../sibling`, an absolute path
  and a symlinked relocation are all rejected here rather than admitted and
  then quietly contributing nothing: a root the receipt calls admitted and the
  walk never enters makes the closure evidence disagree with the admission."
  [^Path root candidate]
  (try
    (and (string? candidate)
         (not (str/blank? candidate))
         (not (str/starts-with? candidate "/"))
         (let [lexical (.normalize (.resolve root ^String candidate))]
           (and (.startsWith lexical root)
                (or (not (Files/exists lexical (into-array LinkOption [])))
                    (let [real (.toRealPath lexical (into-array LinkOption []))]
                      (and (.startsWith real (.toRealPath root (into-array LinkOption [])))
                           (Files/isDirectory real (into-array LinkOption []))))))))
    (catch Exception _ false)))

;; @spec MCP-OP-HELPER-005
;; @spec MCP-OP-HELPER-012
(defn admitted-roots
  "Every discovery root this workspace admits, in a stable order, or a refusal.

  The answer is the one the closure receipt publishes, so an inadmissible
  configured root is a typed refusal and never a silent drop."
  [^Path root]
  (let [configured (configured-source-roots (str root))
        rejected (vec (remove #(admissible-root? root %) configured))]
    (if (seq rejected)
      (refusal "invalid-source-root"
               (str "`.clj-surgeon.edn :source-roots` names a root this verb"
                    " cannot admit: " (str/join ", " (map pr-str rejected)))
               {:rejected_source_roots rejected
                :admitted_source_roots (vec planner/admitted-roots)
                :decision (str "which project-relative directories inside this"
                               " workspace are discovery roots")})
      {:ok true
       :roots (vec (distinct (concat planner/admitted-roots configured)))})))

(defn- root-globs
  [roots]
  (mapv #(str % "/**") roots))

;; ---------------------------------------------------------------------------
;; the confined scan and the frozen read

(defn- scan
  "Every confined project-relative Clojure source one glob set selects.

  `alias_migration`'s own bounded walk, unchanged: the entry ceiling, the depth
  bound, the unreadable-path refusal, the pruned control directories and the
  independent enumeration that WITNESSES the completeness claim are all
  properties of the scope this verb needs and none of them are worth a second
  implementation."
  [root paths]
  (alias-migration/scan-scope root {:paths paths :exclude []}))

(defn- scan-refusal
  [scan-result phase]
  (refusal "scope-unscannable"
           (str "The " phase " scan could not enumerate this workspace: "
                (name (or (:error-type scan-result) :unknown)))
           {:phase phase
            :scan (dissoc scan-result :ok)
            :decision "which paths this workspace's scan can enumerate"}))

(defn- symlink-entry?
  "Whether the walk's entry is itself a symbolic link, at any segment.

  Both directions matter and neither is read: a link pointing OUT of the
  workspace is an escape, and a link pointing back IN is an alias that would be
  enumerated twice under two names."
  [^Path root relative]
  (try
    (let [resolved (.normalize (.resolve root ^String relative))]
      (loop [candidate resolved]
        (cond
          (or (nil? candidate) (.equals candidate root)) false
          (Files/isSymbolicLink candidate) true
          :else (recur (.getParent candidate)))))
    (catch Exception _ true)))

(defn- prune-symlinks
  "The enumeration with every symlinked entry DROPPED, plus what was dropped.

  Repository fence rule: a symlink a walk produces is pruned, never read and
  never counted. Refusing the whole operation on one unrelated link under an
  admitted root would let a single stray link deny every extraction, and
  following it would read bytes outside the workspace; pruning does neither.
  The dropped set is carried so the receipt can say the walk saw them."
  [^Path root files]
  (let [grouped (group-by #(symlink-entry? root %) files)]
    {:files (vec (get grouped false []))
     :pruned (vec (get grouped true []))}))

(defn- read-sources
  "The frozen read: `{:file relative :source text :authorized bool}` for every
  source under the admitted roots, in scan order.

  Every path is resolved through the root-confinement gate before it is read.
  Symlinked entries never reach this function; a path that still fails
  confinement here is a genuine fault rather than a link, so it refuses."
  [root files authorized]
  (reduce
    (fn [acc relative]
      (let [resolved (mcp-paths/resolve-source-path root relative)]
        (if-not (:ok resolved)
          (reduced (refusal "unreadable-source"
                            (str "A source under an admitted root could not be "
                                 "confined to the workspace: " relative)
                            {:file relative
                             :cause (:error resolved)
                             :decision "which paths this workspace's roots may contain"}))
          (conj acc {:file relative
                     :source (slurp (:path resolved))
                     :path (:path resolved)
                     ;; @spec MCP-OP-HELPER-021
                     ;; discovery covers every admitted root; AUTHORIZATION is
                     ;; the subset scope.paths names, and the planner refuses a
                     ;; supported reference found outside it
                     :authorized (contains? authorized relative)}))))
    []
    files))

;; ---------------------------------------------------------------------------
;; verification profiles
;;
;; @spec MCP-OP-HELPER-011
;; @spec MCP-OP-HELPER-022

(defn- built-in-verification-profiles
  "The server's own configured profile map — the SAME configuration path
  `alias_migration`'s `verify` reads.

  Resolved by name because `mcp-http-server` requires `mcp-tool`, which
  requires this namespace; a literal copy of the profile map here would be a
  second source of truth for what this server can run."
  []
  (try
    (some-> (requiring-resolve 'clj-surgeon.mcp-http-server/default-verification-profiles)
            deref)
    (catch Exception _ nil)))

(def max-profile-timeout-ms
  "Ceiling on a configured profile's declared timeout.

  A timeout is the only bound between this verb and a proof that never
  returns, so an unbounded or absurd one is a profile this verb refuses rather
  than a number it honours."
  3600000)

(defn- argv?
  "One command: a non-empty vector of non-empty strings."
  [command]
  (and (vector? command)
       (seq command)
       (every? #(and (string? %) (seq %)) command)))

(defn- argv-list?
  "A non-empty vector of commands, each one an argv vector."
  [commands]
  (and (vector? commands) (seq commands) (every? argv? commands)))

(defn profile-capability
  "What one configured verification profile can do, or nil when v1 cannot admit it.

  v1 admits ONLY profiles whose every check is an external command this process
  runs and waits on, spelled as argv:

  * a `:cold` job is asynchronous — `launch!` returns `:running` and the receipt
    would say `verification_complete false`, which cannot gate a commit;
  * a `:hot` law runs inside a warm application JVM, which is precisely the
    stale-Var false proof MCP-OP-HELPER-022 exists to forbid;
  * a `:commands` entry that is not an argv VECTOR of non-empty strings is not a
    command this verb can run. A profile whose `:commands` is `[\"/bin/true\"]`
    is a vector of one STRING, not a vector of one command, and admitting it let
    a malformed profile stage a whole extraction before ending as a timeout.

  Shape is decided here, before anything is staged, and this function never
  throws: an unrecognised spec is nil, which the preflight turns into a typed
  refusal."
  [spec]
  (try
    (cond
      (argv? spec)
      {:synchronous? true :rollback-capable? true :fresh-process? true
       :commands [spec] :shape :command}

      (not (map? spec)) nil

      ;; asynchronous or warm-JVM authority: never admitted, whatever else the
      ;; profile carries
      (or (contains? spec :hot) (contains? spec :cold)) nil

      (= #{:acceptance :timeout-ms :commands} (set (keys spec)))
      (when (and (argv-list? (:commands spec))
                 (integer? (:timeout-ms spec))
                 (pos? (:timeout-ms spec))
                 (<= (:timeout-ms spec) max-profile-timeout-ms))
        {:synchronous? true :rollback-capable? true :fresh-process? true
         :commands (vec (:commands spec)) :timeout-ms (:timeout-ms spec)
         :shape :exact})

      (argv-list? (:commands spec))
      (let [timeout (:timeout-ms spec)]
        (when (or (nil? timeout)
                  (and (integer? timeout) (pos? timeout)
                       (<= timeout max-profile-timeout-ms)))
          (cond-> {:synchronous? true :rollback-capable? true :fresh-process? true
                   :commands (vec (:commands spec)) :shape :commands}
            timeout (assoc :timeout-ms timeout))))

      :else nil)
    (catch Throwable _ nil)))

(defn- runnable-command?
  "Whether the profile's executable IS an executable file, right now.

  A resolved spelling that merely CONTAINS a slash proves nothing: an absolute
  path to a file that does not exist passed that test and staged a whole
  extraction that could only ever end in a launch failure. The question this
  answers is the one MCP-OP-HELPER-011 asks — can this proof run before I write
  — so it is answered against the filesystem, and it never throws."
  [command]
  (try
    (let [resolved (str (first (change-buffer/expand-command command [])))
          candidate (io/file resolved)]
      (and (str/includes? resolved "/")
           (.isFile candidate)
           (.canExecute candidate)))
    (catch Throwable _ false)))

;; @spec MCP-OP-HELPER-011
(defn admitted-profiles
  "`{profile-name capability}` for every profile v1 may prove a write with.

  Capability is a property of the CONFIGURED profile, derived from the same
  verification-profiles configuration every other write tool reads — never a
  fixture-only flag this verb sets for itself."
  ([]
   ;; the SERVER's own built-in registry, and nothing this verb declares for
   ;; itself. There is no source-declared `helper-proof`: the acceptance-owned
   ;; proof is repository data and reaches this verb the way every other
   ;; profile does, through the workspace's configured verification profiles.
   (admitted-profiles (built-in-verification-profiles)))
  ([profiles]
   ;; a PURE FILTER over what was handed in: exactly those profiles this verb
   ;; may prove a write with, and nothing this function knows from elsewhere. A
   ;; caller asking "what does THIS configuration admit" must never be answered
   ;; with the server's defaults. Accepts either a bare `{name spec}` map or a
   ;; whole config map carrying `:verification-profiles`, because both are
   ;; spellings the callers of this boundary already hold.
   (let [profiles (if (contains? profiles :verification-profiles)
                    (:verification-profiles profiles)
                    profiles)]
     (into (sorted-map)
           (keep (fn [[profile-name spec]]
                   ;; a profile with no command of its own can prove nothing,
                   ;; and is therefore never admitted
                   (when-let [capability (profile-capability spec)]
                     ;; @spec MCP-OP-HELPER-011
                     ;; a profile with no command, or one whose executable is
                     ;; not an executable file right now, can prove nothing and
                     ;; is therefore not admissible. Admission is the gate: a
                     ;; malformed or unlaunchable profile that only fails at the
                     ;; preflight has already been called admitted once, and a
                     ;; caller reading `admitted-profiles` would believe it.
                     (when (and (seq (:commands capability))
                                (every? runnable-command? (:commands capability)))
                       [profile-name capability]))))
           (or profiles {})))))

(defn refusal-types
  "The closed set of `error_type` strings this BOUNDARY can emit.

  It is the planner's set plus the refusals only an I/O boundary can raise:
  profile admissibility, request shape, workspace and scan faults, and the
  destination-derivation limitation. The planner treats `verification.profile`
  as an opaque string, so `verification-preflight-unavailable` is raised HERE."
  []
  (vec (distinct (concat (planner/refusal-types)
                         (map #(str "helper-extraction-" %)
                              ["verification-preflight-unavailable"
                               "invalid-request"
                               "unknown-field"
                               "invalid-source-root"
                               "workspace-unreadable"
                               "scope-unscannable"
                               "unreadable-source"
                               "destination-not-derivable"
                               "receipt-dir-inside-workspace"
                               "transaction-refused"])))))

;; @spec MCP-OP-HELPER-011
(defn verification-preflight
  "nil when `profile-name` may prove this write now, or the typed refusal.

  Nothing is staged by this function, and nothing may be staged before it
  answers: an unusable verification authority is knowable from the request and
  the configuration alone.

  Two arities because the two callers ask different questions. `plan` is a READ
  and asks only whether the named profile is an admissible one; `execute!` asks
  the whole question a moment before staging, runnability included, because a
  profile that cannot launch is a profile that cannot roll a write back."
  ([profiles profile-name] (verification-preflight profiles profile-name false))
  ([profiles profile-name check-runnable?]
  (let [;; @spec MCP-OP-HELPER-011
        ;; ONLY the routed workspace's configured profiles. The server's
        ;; built-in registry is NOT a source of authority here: an empty or
        ;; absent configuration admits NOTHING, and a request naming the
        ;; built-in `fast` profile against a workspace that configures no
        ;; profiles is refused rather than proved by a command that workspace
        ;; never declared.
        admitted (admitted-profiles profiles)
        capability (get admitted profile-name)
        commandless? (and capability (empty? (:commands capability)))
        unrunnable (when (and capability check-runnable? (not commandless?))
                     (first (remove runnable-command? (:commands capability))))]
    (cond
      (nil? capability)
      (refusal "verification-preflight-unavailable"
               (str "The verification profile " (pr-str profile-name)
                    " is not a synchronous, rollback-capable profile this"
                    " workspace configures.")
               {:profile profile-name
                :needed {:synchronous true :rollback_capable true
                         :fresh_process true}
                :configured_profiles (vec (sort (keys (or profiles {}))))
                :admitted_profiles (vec (keys admitted))
                :staged false
                :decision "which admitted profile proves this write"})

      ;; a name the contract fixes that this workspace never gave a command:
      ;; admissible in principle, unable to prove anything here
      commandless?
      (refusal "verification-preflight-unavailable"
               (str "The verification profile " (pr-str profile-name)
                    " is named by the contract but this workspace configures no"
                    " command for it.")
               {:profile profile-name
                :configured_profiles (vec (sort (keys (or profiles {}))))
                :staged false
                :decision "which admitted profile proves this write"})

      unrunnable
      (refusal "verification-preflight-unavailable"
               (str "The verification profile " (pr-str profile-name)
                    " names an executable that cannot be found now: "
                    (pr-str (first unrunnable)))
               {:profile profile-name
                :unrunnable_command (vec unrunnable)
                :staged false
                :decision "which admitted profile proves this write"})))))

;; ---------------------------------------------------------------------------
;; planning
;;
;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-012

(defn- lib-path
  "The project-relative path a namespace name occupies under its source root."
  [lib]
  (str (str/replace (str/replace (str lib) "-" "_") "." "/") ".clj"))

(defn- root-of
  "The admitted source root `relative` sits under, or nil."
  [roots relative]
  (first (filter #(str/starts-with? (str relative) (str % "/")) roots)))

;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-012
(defn- destination-limitation
  "A boundary refusal when the destination is not an EXACT decomposition of
  `from.file` into one admitted source root plus the namespace's own path.

  The destination namespace must equal `to.lib` exactly and its path must be
  that namespace's path under the SAME admitted root the source occupies. Two
  ways to get this wrong, and both are refused here rather than guessed:

  * a path walk that looks for the nearest ancestor directory called `src`
    infers `ancestor.project.src.acid.web.response` for a project that happens
    to live under one;
  * a `from.file` whose path does NOT end in its own declared namespace path
    decomposes into no root, and falling back to the empty prefix invents a
    destination at the project root that no admitted root contains.

  `next_call nil`: nothing the caller can resend fixes a tree whose file layout
  and namespace declarations disagree."
  [roots from-file dest-lib dest-file]
  (let [dest-file (str dest-file)
        source-root (root-of roots from-file)
        expected (when source-root (str source-root "/" (lib-path dest-lib)))]
    (cond
      (str/blank? dest-file)
      (refusal "destination-not-derivable"
               (str "The destination path for " dest-lib " could not be derived"
                    " project-relatively from from.file.")
               {:limitation "destination-path-not-project-relative"
                :lib dest-lib
                :decision "which project-relative path the destination namespace occupies"})

      (or (str/starts-with? dest-file "/") (str/starts-with? dest-file "../"))
      (refusal "destination-not-derivable"
               (str "The destination path for " dest-lib " is not"
                    " project-relative: " dest-file)
               {:limitation "destination-path-not-project-relative"
                :lib dest-lib :file dest-file
                :decision "which project-relative path the destination namespace occupies"})

      (nil? source-root)
      (refusal "destination-not-derivable"
               (str "from.file " (pr-str from-file) " does not sit under any"
                    " admitted source root, so the destination cannot be"
                    " derived from the source's own root.")
               {:limitation "source-file-outside-admitted-roots"
                :lib dest-lib :from_file from-file :admitted_roots (vec roots)
                :decision "which admitted source root holds this namespace"})

      (not= expected dest-file)
      (refusal "destination-not-derivable"
               (str "The destination this seam would write, " (pr-str dest-file)
                    ", is not " (pr-str dest-lib) "'s own path under the source's"
                    " admitted root " (pr-str source-root) ".")
               {:limitation "destination-not-an-exact-source-root-decomposition"
                :lib dest-lib :file dest-file :expected_file expected
                :source_root source-root :from_file from-file
                :decision "which project-relative path the destination namespace occupies"}))))

;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-005
;; @spec MCP-OP-HELPER-012
;; @spec MCP-OP-HELPER-021
(defn plan
  "Read the tree under `workspace_root`, confine it, and plan one extraction.

  Discovery runs over every ADMITTED root; `scope.paths` supplies the
  write-authorization subset. The planner — not this namespace — decides
  caller-outside-scope, because it is the half that knows which files carry a
  supported reference.

  THE PROFILE IS CHECKED FIRST, before the workspace root is even resolved. An
  unusable verification authority is knowable from the request and the profile
  registry alone, and answering it after the walk would make the refusal depend
  on the state of a tree this call was never going to write."
  ([params] (plan params nil))
  ([params profiles]
  (let [validated (validate-request params)]
    (if-not (:ok validated)
      validated
      (let [request (:request validated)
            ;; @spec MCP-OP-HELPER-011
            ;; @spec MCP-OP-HELPER-016
            ;; the boundary owns this refusal: the pure planner takes
            ;; verification.profile as an opaque string, and whether a name is
            ;; a synchronous, rollback-capable profile is a fact about the
            ;; registry, not about the request's grammar. No weaker profile is
            ;; ever offered as a continuation.
            preflight (verification-preflight
                        profiles (get-in request [:verification :profile]))
            root-result (when-not preflight
                          (try {:ok true :root (mcp-paths/real-root (:workspace_root request))}
                               (catch Exception error
                                 {:ok false :error (.getMessage error)})))]
        (if preflight
          preflight
        (if-not (:ok root-result)
          (refusal "workspace-unreadable"
                   (str "The workspace root could not be resolved: "
                        (:error root-result))
                   {:workspace_root (:workspace_root request)
                    :decision "which directory this workspace is rooted at"})
          (let [root (:root root-result)
                admitted (admitted-roots root)]
            (if-not (:ok admitted)
              admitted
            (let [roots (:roots admitted)
                discovery (scan root (root-globs roots))]
            (if-not (:ok discovery)
              (scan-refusal discovery "discovery")
              (let [authorized (scan root (get-in request [:scope :paths]))]
                (if-not (:ok authorized)
                  (scan-refusal authorized "authorization")
                  ;; @spec MCP-OP-HELPER-005
                  ;; symlinked entries are PRUNED before the completeness and
                  ;; read sets form, in both directions
                  (let [walked (prune-symlinks root (:files discovery))
                        authorized-set (set (:files (prune-symlinks
                                                      root (:files authorized))))
                        sources (read-sources root (:files walked)
                                              authorized-set)]
                    (if (map? sources)
                      sources
                      (let [planned (planner/plan request (mapv #(dissoc % :path) sources))]
                        (if-not (:ok planned)
                          planned
                          (or (destination-limitation
                                roots
                                (get-in request [:from :file])
                                (get-in planned [:plan :destination :lib])
                                (get-in planned [:plan :destination :file]))
                              (-> planned
                                  ;; @spec MCP-OP-HELPER-012
                                  ;; the closure receipt states the roots the
                                  ;; walk ACTUALLY admitted, not a fixed pair
                                  (assoc-in [:receipt :closure :roots] roots)
                                  (assoc-in [:receipt :closure :pruned_symlinks]
                                            (count (:pruned walked)))
                                  ;; the planner's own O(1) receipt travels WITH
                                  ;; the plan: as a SIBLING key it never reached
                                  ;; the terminal mapper at all, which is why the
                                  ;; wire receipt printed null helpers, null
                                  ;; caller files and null sites
                                  (assoc-in [:plan :receipt] (:receipt planned))
                                  (assoc
                                     :roots roots
                                     :pruned_symlinks (vec (:pruned walked))
                                     :paths (into {} (map (juxt :file :path)) sources)
                                     ;; the FROZEN read, carried forward by the
                                     ;; plan rather than re-slurped at write
                                     ;; time: the kernel's stale-source gate has
                                     ;; to see the bytes the plan was derived
                                     ;; from, or drift between the two commits
                                     ;; silently over a stale plan
                                     :sources (into {} (map (juxt :path :source)) sources)))))))))))))))))))))

;; ---------------------------------------------------------------------------
;; the terminal states and the terminal receipt
;;
;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022

(defn terminal-states
  "The four distinct states one staged helper_extraction can end in."
  []
  [:committed :verification-failed :verification-timeout :rollback-failed])

(def proof-stdout-fields
  "The fields the acceptance-owned proof prints as JSON on stdout.

  Copied when the proof actually printed them and omitted when it did not. A
  constant substituted for a field the proof did not emit is the manufactured
  evidence MCP-OP-HELPER-022 exists to forbid."
  [:profile :behavior_cases :caller_files :selected_sites :retained_sites
   :changed_files :application_compile_claim :evidence_id])

(def typed-check-fields
  "The three TYPED checks the receipt reports, and never a bare coverage count."
  [:structural_callers :helper_behaviors :compiled_callers])

(defn- compiled-claim-backed?
  "Whether a compiled-caller claim is backed by evidence of compiles that happened."
  [verification]
  (let [claimed (:compiled_callers verification)]
    (or (not (number? claimed))
        (zero? claimed)
        (= claimed (count (:compiled_evidence verification))))))

(defn- verification-face
  "The receipt's typed verification map, copied from evidence that exists.

  With no profile result — nil, or an empty map — this claims NOTHING: status
  `unknown`, no counts, no `ok`, no `fresh_process`. Manufacturing a zero here
  would report an unexecuted check as a completed one."
  [verification]
  (if-not (and (map? verification) (seq verification))
    {:status "unknown"}
    (let [backed? (compiled-claim-backed? verification)
          ok? (and (true? (:ok verification)) backed?)]
      (cond-> {:status (cond (not backed?) "unbacked-claim"
                             ok? "checks-completed"
                             (contains? verification :ok) "checks-failed"
                             :else "unknown")}
        ;; the executed profile names itself
        (:profile verification) (assoc :profile (:profile verification))
        (contains? verification :ok) (assoc :ok ok?)
        ;; a fact about an execution that happened, never an assumption
        (true? (:fresh_process verification)) (assoc :fresh_process true)
        (false? (:fresh_process verification)) (assoc :fresh_process false)
        :always (merge (into {}
                             (keep (fn [field]
                                     (when (number? (get verification field))
                                       [field (get verification field)])))
                             typed-check-fields))
        ;; a claim of N compiled callers with evidence of fewer is reduced to
        ;; what the evidence supports, and the profile result is not ok
        (not backed?) (assoc :compiled_callers
                             (count (:compiled_evidence verification))
                             :unbacked_claim :compiled_callers)
        (contains? verification :compiled_evidence)
        (assoc :compiled_evidence (vec (:compiled_evidence verification)))
        :always (merge (into {}
                             (keep (fn [field]
                                     (when (some? (get verification field))
                                       [field (get verification field)])))
                             proof-stdout-fields))
        ;; the proof's OWN status word, carried under its own name so it can
        ;; never be mistaken for this receipt's typed check status
        (:status verification) (assoc :proof_status (:status verification))))))

(defn- sha256
  [text]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" (bit-and 0xff %))
                    (.digest digest (.getBytes ^String text "UTF-8"))))))

(defn- aggregate-hash
  "One digest over a whole `{file hash}` read-back.

  Order-independent by sorting first, so the same restoration always answers
  the same digest and two receipts can be compared without the manifest either
  of them omits."
  [read-back]
  (sha256 (pr-str (into (sorted-map) (or read-back {})))))

(def mutation-claim-counts
  "The plan-derived counts that ASSERT A COMPLETED MUTATION.

  Each one is a sentence about the tree as it now stands: how many owners the
  source no longer defines, how many callers were rewritten, how many sites
  changed, which aliases the write installed. After a proven rollback every one
  of them is false, and a receipt that carries `source_retired 6` beside
  `restored true` and `source_unchanged true` contradicts itself in the same
  object — measured on a real negative run that restored all 27 staged files
  and still reported a retirement.

  NOT here, deliberately: `helpers` (how many the REQUEST selected),
  `source_file` (the extraction subject: 1), `closure` (what the walk covered)
  and `destination_lib`. Those are facts about the request and the discovery,
  true whatever the write did, and prefixing them would say the request itself
  was hypothetical."
  [:source_retired :caller_files :changed_files :sites :retained_sites
   :alias_histogram :partition])

(defn- planned-key
  [field]
  (keyword (str "planned_" (name field))))

;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-020
(defn- plan-counts
  "The O(1) counts the plan derived, projected onto the state the tree is IN.

  `outcome` is the terminal fact the kernel reported, and it decides whether a
  count is an assertion or a description:

    :committed   the write stands, so the plan's counts describe the tree.
    :restored    the inverse verified, so nothing was retired, rewritten or
                 aliased: the plan's counts move to `planned_*` and
                 `source_retired` is the ACTUAL zero.
    :unrestored  the inverse did not verify, so how much of the source is
                 retired is genuinely NOT KNOWN — it is stated as unknown and
                 never as a number, in either direction.
    :unknown     no terminal evidence at all: the plan's counts are published
                 as planned and nothing is claimed about the tree."
  [plan outcome]
  (let [receipt (:receipt plan)
        facts (cond-> {}
                (map? receipt) (merge (select-keys receipt
                                                   [:helpers :source_file :closure
                                                    :source_retired :caller_files
                                                    :changed_files :sites
                                                    :retained_sites :alias_histogram
                                                    :partition]))
                (map? (:counts plan)) (assoc :counts (:counts plan))
                (some? (:partition plan)) (assoc :partition (:partition plan))
                (coll? (:helpers plan)) (assoc :helpers (count (:helpers plan)))
                (map? (:destination plan)) (assoc :destination_lib
                                                  (get-in plan [:destination :lib])))]
    (if (= :committed outcome)
      facts
      (let [claims (select-keys facts mutation-claim-counts)
            described (into {}
                            (map (fn [[field value]] [(planned-key field) value]))
                            claims)]
        (merge (apply dissoc facts mutation-claim-counts)
               described
               (case outcome
                 ;; the one terminal retirement number a proven restoration
                 ;; entitles this receipt to state
                 :restored {:source_retired 0}
                 :unrestored {:source_retired_unknown
                              (str "the rollback did not verify, so how many"
                                   " owners the source still defines is not"
                                   " known from this receipt; read"
                                   " recovery_required")}
                 {}))))))

;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022
(defn terminal-receipt
  "A PURE MAPPING from the facts the kernel and the profile produced onto the
  receipt.

  It executes nothing, recomputes nothing, and invents nothing. `restored`,
  `source_unchanged`, `destination_created` and every verification number are
  present only because the injected evidence carried them; with no evidence the
  honest receipt claims none of them. `restored` and `source_unchanged` are two
  faces of ONE kernel fact — a failed restoration is never reported as
  unchanged."
  [{:keys [kernel verification plan]}]
  (let [kernel (or kernel {})
        status (:status kernel)
        committed? (= :committed status)
        restored (when (contains? kernel :restored) (boolean (:restored kernel)))
        ;; @spec MCP-OP-HELPER-020
        ;; which sentences this receipt is entitled to say about the tree
        outcome (cond committed? :committed
                      (true? restored) :restored
                      (false? restored) :unrestored
                      :else :unknown)]
    (cond-> (merge {:operation operation
                    :status (if status (name status) "unknown")
                    :verification (verification-face verification)}
                   (plan-counts plan outcome))
      (some? status) (assoc :kernel_status (name status)
                            :committed committed?)
      (some? status) (assoc :ok (boolean committed?))
      (some? restored) (assoc :restored restored
                              ;; @spec MCP-OP-HELPER-020
                              ;; unchanged is claimed ONLY because the kernel
                              ;; restored it, and never after a failed rollback
                              :source_unchanged restored)
      committed? (assoc :source_unchanged false)
      (true? (:destination_removed kernel)) (assoc :destination_created false)
      (and (not (contains? kernel :destination_removed))
           (contains? kernel :destination_created))
      (assoc :destination_created (boolean (:destination_created kernel)))
      ;; @spec MCP-OP-HELPER-009
      ;; A VERIFIED rollback publishes constant-size evidence: how many files
      ;; came back and one aggregate digest over the read-back, never the
      ;; manifest. Measured: the per-file map made an otherwise identical
      ;; verification-failed receipt grow from 282 bytes at one file to 37,029
      ;; at a thousand. The manifest and the per-file hashes are written to
      ;; `details_path` under local state, where a caller who needs them can
      ;; read them.
      (contains? kernel :restoration_read_back)
      (assoc :restoration_read_back
             (let [read-back (:restoration_read_back kernel)]
               {:files (count read-back)
                :aggregate_sha256 (aggregate-hash read-back)
                :manifest_in "details_path"}))
      (contains? kernel :restored_files)
      (assoc :restored_file_count (count (:restored_files kernel)))
      ;; @spec MCP-OP-HELPER-020
      ;; a rollback that did not complete NAMES the files it could not restore
      ;; and carries the kernel's own recovery-required evidence
      (contains? kernel :unrestored_files)
      (assoc :files (vec (:unrestored_files kernel)))
      (contains? kernel :recovery_required)
      (assoc :recovery_required (:recovery_required kernel))
      (contains? kernel :details_path) (assoc :details_path (:details_path kernel))
      ;; @spec MCP-OP-HELPER-009
      ;; an absent detail artifact is STATED. A receipt whose bounded evidence
      ;; points at an external document must say when that document is not
      ;; there, or the caller reads the absence as "nothing more to see".
      (contains? kernel :details_unavailable)
      (assoc :details_unavailable (:details_unavailable kernel))
      (contains? kernel :undo_receipt) (assoc :undo_receipt (:undo_receipt kernel))
      (contains? kernel :receipt_hash) (assoc :receipt_hash (:receipt_hash kernel))
      (contains? kernel :elapsed_ms) (assoc :elapsed_ms (:elapsed_ms kernel)))))

;; ---------------------------------------------------------------------------
;; the fresh-process proof
;;
;; @spec MCP-OP-HELPER-022

(def max-proof-output-bytes
  "How much of the proof's own stdout this process reads back.

  The proof PRINTS its evidence as JSON, so the cap that matters is the one on
  what the parser gets to see, not the one sized for a receipt."
  (* 512 1024))

(def default-proof-timeout-ms 600000)

(defn- parse-proof-output
  "The proof's stdout JSON, or nil when it printed something else.

  A proof that printed no JSON is not a proof that failed: the exit code says
  that. This only decides whether there are FIELDS to copy."
  [output]
  (try
    (let [trimmed (str/trim (str output))
          start (str/last-index-of trimmed "{")]
      (when start
        (let [parsed (json/parse-string (subs trimmed start) true)]
          (when (map? parsed) parsed))))
    (catch Exception _ nil)))

;; @spec MCP-OP-HELPER-022
(defn run-proof!
  "Run one admitted profile's own command in a FRESH process at the candidate cwd.

  Fresh, so a warm namespace holding stale Vars for retired helpers cannot
  manufacture a proof of a tree they no longer belong to. `fresh_process` is
  reported from the fact that a child process ran and answered, never asserted."
  [project-root profile-name capability]
  ;; the cwd may arrive as a Path or a String; the process runner coerces with
  ;; `io/file`, which has no implementation for a Path and turned a real launch
  ;; into a launch-error that then read as "no fresh process ran"
  (let [project-root (str project-root)
        timeout (or (:timeout-ms capability) default-proof-timeout-ms)
        outcomes
        (reduce (fn [acc command]
                  (let [argv (change-buffer/expand-command command [])
                        process (change-buffer/run-process!
                                  project-root argv timeout max-proof-output-bytes)
                        outcome (assoc (select-keys process
                                                    [:exit :elapsed_ms :finished?
                                                     :output :output-bytes
                                                     :output-sha256 :output-truncated
                                                     :launch-error])
                                       ;; @spec MCP-OP-HELPER-022
                                       ;; a child that STARTED, independent of
                                       ;; whether it finished: a timed-out proof
                                       ;; has no exit code and did run, and
                                       ;; deriving freshness from the exit code
                                       ;; reported the opposite
                                       :started? (not (true? (:launch-error process)))
                                       :command (vec argv))
                        acc (conj acc outcome)]
                    (if (and (:finished? process) (zero? (or (:exit process) 1)))
                      acc
                      (reduced acc))))
                []
                (:commands capability))
        last-outcome (last outcomes)
        finished? (boolean (:finished? last-outcome))
        ok? (and finished? (= (count outcomes) (count (:commands capability)))
                 (every? #(zero? (or (:exit %) 1)) outcomes))
        printed (parse-proof-output (:output last-outcome))]
    (merge
      ;; the proof's own JSON fields, copied when present and never replaced
      (select-keys printed (into [:status] (concat proof-stdout-fields
                                                   typed-check-fields
                                                   [:compiled_evidence])))
      {:profile profile-name
       ;; @spec MCP-OP-HELPER-022
       ;; a fresh child process RAN. Not "answered": a proof killed at its
       ;; timeout started, executed, and was cut off, and reporting
       ;; `fresh_process false` for it says the opposite of what happened.
       ;; It is still a FACT, never a courtesy: a child that could not be
       ;; launched at all reports false, and `cwd_exists` separates the two
       ;; causes that otherwise print the same `Exec failed, error: 2` — a
       ;; missing executable, and a working directory that is not there.
       :fresh_process (boolean (some :started? outcomes))
       :cwd_exists (.isDirectory (io/file project-root))
       :timed_out (not finished?)
       :ok ok?
       ;; the RAW evidence, retained rather than summarized into a constant
       :process_evidence outcomes
       :cwd (str project-root)})))

;; ---------------------------------------------------------------------------
;; the write
;;
;; @spec MCP-OP-HELPER-008
;; ONE transaction carrying ONE typed `extraction` change — source retirement,
;; destination creation, the source-local lowering and every caller whole-form
;; change land or refuse together, through the extraction kernel entrance.

(defn- extraction-request
  "The planner's typed `extraction` change, with every path root-confined.

  The planner speaks project-relative paths; the kernel writes canonical ones,
  and the resolution gate between them is the same one every other write tool
  uses."
  [root change paths]
  (let [source (mcp-paths/resolve-source-path root (:file change))
        target (mcp-paths/resolve-new-source-path root (:to change))
        ;; the planner speaks project-relative paths and STRING ids; the
        ;; transaction kernel addresses canonical paths and refuses a change
        ;; whose `:id` is not a keyword ("Change :id must be a keyword"). The
        ;; id is minted here, positionally and deterministically, rather than
        ;; coerced from a path — a project-relative path carries `/`, which no
        ;; keyword name may hold, so coercion would either fail or silently
        ;; rename the change.
        callers (into []
                      (map-indexed
                        (fn [index caller-change]
                          (-> caller-change
                              (assoc :id (keyword "helper-extraction"
                                                  (str "c" index)))
                              (update :in (fn [files]
                                            (mapv #(get paths % %) files))))))
                      (:caller_changes change))
        ignored (mapv #(mcp-paths/resolve-source-path root %)
                      (:ignored_caller_files change))
        refused (first (remove :ok (concat [source target] ignored)))]
    (if refused
      (refusal "unreadable-source"
               (str "A path in the planned transaction could not be confined to "
                    "the workspace: " (or (:error refused) "unknown"))
               {:cause (:error refused)
                :decision "which paths this transaction may address"})
      {:ok true
       :extraction
       {:file (:path source)
        :to (:path target)
        :forms (vec (:forms change))
        :require-policy (keyword (:require_policy change))
        :source-hash (:source_hash change)
        :created-directories (mapv str (:missing-parent-directories target))
        :caller-changes callers
        :ignored-caller-files (mapv :path ignored)
        :expect {:forms (count (:forms change))
                 :caller-edits (count callers)
                 ;; the source, the destination and every caller file the
                 ;; transaction writes
                 :files (+ 2 (count (distinct (mapcat :in callers))))}}})))

;; @spec MCP-OP-HELPER-009
(defn- resolve-receipt-dir
  "Where this call publishes its undo receipt and its detail document.

  The kernel's LOCAL-STATE receipt directory for this workspace is the default
  and the only place this verb may publish. A configured directory is honoured
  only when it stays OUT of the tree being mutated: a receipt published inside
  the workspace is a file the extraction can retire, an undo can restore over,
  and a caller can mistake for source. Containment is decided on REAL paths, so
  a symlink alias pointing back into the workspace is caught with the direct
  spelling."
  [config project-root]
  (let [configured (:receipt-dir config)
        default (workspace/receipt-dir project-root)
        real (fn [path]
               (try (.toRealPath (.toPath (io/file (str path)))
                                 (into-array LinkOption []))
                    (catch Exception _
                      ;; a directory that does not exist yet has no real path;
                      ;; its nearest existing ancestor decides containment
                      (loop [candidate (.getAbsoluteFile (io/file (str path)))]
                        (cond
                          (nil? candidate) nil
                          (.exists candidate)
                          (try (.toRealPath (.toPath candidate)
                                            (into-array LinkOption []))
                               (catch Exception _ nil))
                          :else (recur (.getParentFile candidate)))))))]
    (if-not configured
      {:ok true :dir (str default)}
      (let [workspace-real (real project-root)
            configured-real (real configured)]
        (if (and workspace-real configured-real
                 (.startsWith ^Path configured-real ^Path workspace-real))
          (refusal "receipt-dir-inside-workspace"
                   (str "The configured receipt directory resolves inside the"
                        " workspace this call mutates: " (str configured))
                   {:receipt_dir (str configured)
                    :resolved (str configured-real)
                    :workspace_root (str project-root)
                    :local_state_receipt_dir (str default)
                    :staged false
                    :decision (str "where this workspace's undo receipts and"
                                   " per-caller detail are published")})
          {:ok true :dir (str configured)})))))

(defn- restoration-read-back
  "What is on disk after a rollback, read back rather than assumed.

  The kernel's undo verifies its own writes; this is the INDEPENDENT read the
  receipt publishes, because `restored: true` is a claim about the filesystem
  and a claim about the filesystem is worth exactly what re-reading it costs."
  [files]
  (into (sorted-map)
        (keep (fn [file]
                (let [candidate (io/file (str file))]
                  (when (.isFile candidate)
                    [(str file) (sha256 (slurp candidate))]))))
        files))

;; @spec MCP-OP-HELPER-008
;; @spec MCP-OP-HELPER-011
;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022
(defn execute!
  "Plan, preflight, stage one transaction, prove it in a fresh process, and
  publish one O(1) terminal receipt.

  Order is the contract: the profile's capability is validated BEFORE anything
  is staged (MCP-OP-HELPER-011), the whole write is one transaction
  (MCP-OP-HELPER-008), the proof runs in a fresh process inside that
  transaction's rollback authority (MCP-OP-HELPER-022), and the four terminal
  states stay distinct (MCP-OP-HELPER-020)."
  [config params]
  (let [profiles (:verification-profiles config)
        planned (plan params profiles)]
    (if-not (:ok planned)
      planned
      (let [profile-name (get-in (walk/keywordize-keys params) [:verification :profile])
            ;; asked AGAIN, with runnability, immediately before staging: the
            ;; plan-time answer was about the registry, and this one is about
            ;; whether the proof can actually launch here and now
            preflight (verification-preflight profiles profile-name true)]
        (if preflight
          ;; nothing staged, and the receipt says so
          preflight
          (let [capability (get (admitted-profiles profiles) profile-name)
                ;; the injectable proof step. One key, one default, no behavior
                ;; change: the production path is `run-proof!` itself, and a
                ;; caller that supplies its own is exercising the same seam the
                ;; boundary uses.
                proof! (or (:run-proof! config) run-proof!)
                ;; the kernel handoff seam. Same shape as `:run-proof!`: one
                ;; key, one default, no behaviour change. A throw from INSIDE
                ;; the commit — after it has written and read back every byte —
                ;; is the one failure a boundary cannot witness without a seam
                ;; here, and it is the failure that leaves an extraction
                ;; standing with nobody holding the inverse.
                commit! (or (:commit! config) extraction/commit!)
                root (mcp-paths/real-root (get-in (walk/keywordize-keys params)
                                                  [:workspace_root]))
                project-root (str root)
                change (first (get-in planned [:plan :transactions 0 :changes]))
                resolved (extraction-request root change (:paths planned))]
            (if-not (:ok resolved)
              resolved
              (let [;; @spec MCP-OP-HELPER-008
                    ;; the plan's OWN frozen bytes, never a second read: a
                    ;; re-slurp here would compile the transaction against a
                    ;; tree that may have moved under the plan, and the
                    ;; pre-image gate would then be asserting the drift rather
                    ;; than catching it
                    sources (:sources planned)
                    request (assoc (:extraction resolved)
                                   :source (get sources (get-in resolved [:extraction :file]))
                                   :target-ns (get-in planned [:plan :destination :lib])
                                   :workspace-sources sources)
                    compiled (extraction/compile-extraction request)]
                (if-not (:ok compiled)
                  (assoc compiled :ok false :operation operation)
                  (let [receipt-decision (resolve-receipt-dir config project-root)]
                    (if-not (:ok receipt-decision)
                      ;; nothing staged: where a receipt may be published is
                      ;; decided before the kernel is entered
                      receipt-decision
                      (let [receipt-dir (:dir receipt-decision)
                            receipt-file (str (io/file receipt-dir
                                                       (str (UUID/randomUUID) ".edn")))
                            details-file (str (io/file receipt-dir
                                                       (str "helper-extraction-"
                                                            (UUID/randomUUID) ".edn")))
                            started (System/nanoTime)
                            elapsed #(/ (double (- (System/nanoTime) started)) 1000000.0)
                            ;; @spec MCP-OP-HELPER-008
                            ;; @spec MCP-OP-HELPER-020
                            ;; THE INVERSE IS OWNED BEFORE THE FIRST BYTE IS
                            ;; WRITTEN. `commit!` can throw AFTER it has written
                            ;; and read back every file — a wrapper, an
                            ;; interrupt, an OOM on the way out — and a boundary
                            ;; that only learns the receipt from `commit!`'s
                            ;; RETURN VALUE has no inverse in exactly that case.
                            ;; `build-receipt` derives the same hash-fenced
                            ;; receipt `commit!` publishes, from the same
                            ;; compiled snapshot, so the authority to undo
                            ;; exists before there is anything to undo.
                            inverse-receipt (extraction/build-receipt compiled)
                            committed (volatile! nil)
                            originals (:original-sources compiled)
                            created (vec (:created-files compiled))
                            ;; the tree may already be back: `commit!`'s own
                            ;; handler restores what it wrote before it fails,
                            ;; and running the inverse over a restored tree
                            ;; would refuse and report a rollback failure that
                            ;; did not happen. So this is READ, not assumed.
                            tree-restored?
                            (fn []
                              (try
                                (and (every? (fn [[file original]]
                                               (let [candidate (io/file (str file))]
                                                 (and (.isFile candidate)
                                                      (= original (slurp candidate)))))
                                             originals)
                                     (not-any? #(.exists (io/file (str %))) created))
                                (catch Throwable _ false)))
                            ;; @spec MCP-OP-HELPER-009
                            ;; the external detail artifact carries everything
                            ;; the bounded receipt does not: the restored-file
                            ;; manifest, the per-file read-back, and the proof's
                            ;; own failure evidence. When it cannot be written
                            ;; the receipt SAYS SO — a details_path naming a
                            ;; file that does not exist is worse than no path,
                            ;; because the caller stops looking.
                            publish-details!
                            (fn [document]
                              (try
                                (.mkdirs (io/file receipt-dir))
                                (file-ops/atomic-write! details-file (pr-str document))
                                {:details_path details-file}
                                (catch Throwable error
                                  {:details_unavailable
                                   (str "the detail document could not be"
                                        " published: "
                                        (or (.getMessage error)
                                            (.getName (class error))))})))
                            finish-failure!
                            (fn [failed-state proof cause]
                              (let [receipt (or (:receipt @committed) inverse-receipt)
                                    touched (mapv :file (:files receipt))
                                    restored-already? (tree-restored?)
                                    rollback (when-not restored-already?
                                               (try (extraction/undo! receipt)
                                                    (catch Throwable undo-error
                                                      {:ok false
                                                       :error (or (.getMessage undo-error)
                                                                  (.getName (class undo-error)))
                                                       :threw true})))
                                    rolled-back? (or restored-already?
                                                     (boolean (:ok rollback)))
                                    read-back (when rolled-back?
                                                (try (restoration-read-back touched)
                                                     (catch Throwable _ {})))]
                                (when rolled-back?
                                  (try (.delete (io/file receipt-file))
                                       (catch Exception _ nil)))
                                ;; @spec MCP-OP-HELPER-009
                                ;; the MANIFEST lives in the detail document;
                                ;; the receipt carries counts and one digest
                                (let [detail
                                      (publish-details!
                                        {:operation operation
                                         :status failed-state
                                         :restored rolled-back?
                                         :restored_files touched
                                         :restoration_read_back read-back
                                         :rollback rollback
                                         ;; the EXACT proof failure evidence,
                                         ;; whole, out here rather than in the
                                         ;; bounded receipt
                                         :verification proof
                                         :cause_error cause
                                         :plan (select-keys (:plan planned)
                                                            [:destination :files :moved])})]
                                (cond->
                                  (terminal-receipt
                                    {:kernel (if rolled-back?
                                               (merge detail
                                                {:status failed-state
                                                :restored true
                                                :restored_files touched
                                                :restoration_read_back read-back
                                                :destination_removed true
                                                :elapsed_ms (elapsed)})
                                               ;; @spec MCP-OP-HELPER-020
                                               ;; the one state that keeps the
                                               ;; linear evidence, because a
                                               ;; human has to act on it
                                               ;; @spec MCP-OP-HELPER-020
                                               ;; the recovery authority stands
                                               ;; whether or not the external
                                               ;; artifact could be written
                                               (merge detail
                                                {:status :rollback-failed
                                                :restored false
                                                :unrestored_files touched
                                                :recovery_required
                                                {:receipt receipt-file
                                                 :reason (or (:error rollback)
                                                             "the extraction undo did not verify")
                                                 :recovery rollback}
                                                :elapsed_ms (elapsed)}))
                                     :verification proof
                                     :plan (:plan planned)})
                                  cause (assoc :cause_error cause)))))]
                        (try
                          ;; the kernel handoff is INSIDE the guard
                          (let [result (commit! compiled)]
                            (vreset! committed result)
                            (if-not (:ok result)
                              ;; the kernel refused or restored its own partial
                              ;; write: nothing of this transaction stands, so
                              ;; this is a typed refusal and not a terminal
                              ;; state — no terminal state was reached
                              (refusal "transaction-refused"
                                       (str "The extraction kernel did not commit: "
                                            (or (:error result)
                                                (some-> (:error-type result) name)
                                                "unknown"))
                                       {:kernel_error_type (some-> (:error-type result) name)
                                        :kernel_error (:error result)
                                        :decision "what the kernel refused about this transaction"})
                              (do
                                (.mkdirs (io/file receipt-dir))
                                (file-ops/atomic-write! receipt-file
                                                        (pr-str (:receipt result)))
                                (let [detail (publish-details!
                                               (select-keys (:plan planned)
                                                            [:destination :files :moved]))
                                      proof (proof! project-root profile-name capability)]
                                  (if (:ok proof)
                                    (terminal-receipt
                                      {:kernel (merge detail
                                                {:status :committed
                                                :destination_created true
                                                :undo_receipt receipt-file
                                                :receipt_hash (:receipt-hash result)
                                                :elapsed_ms (elapsed)})
                                       :verification proof
                                       :plan (:plan planned)})
                                    (finish-failure! (if (:timed_out proof)
                                                       :verification-timeout
                                                       :verification-failed)
                                                     proof nil))))))
                          ;; a throw is a proof that did not complete, and an
                          ;; incomplete proof may never leave a commit standing
                          (catch Throwable error
                            (finish-failure! :verification-failed nil
                                             (or (.getMessage error)
                                                 (.getName (class error))))))))))))))))))

;; ---------------------------------------------------------------------------
;; registration
;;
;; @spec MCP-OP-HELPER-001

(def tool-description
  (str
    "Move a named set of public helpers out of one namespace into a new one, "
    "and close every reference to them across the project, in a single call "
    "whose payload does not grow with the number of callers. Send from {file}, "
    "helpers, to {lib, alias_policy}, scope {paths}, and verification "
    "{profile}. Surgeon resolves each helper to exactly one owner, scans the "
    "selected bodies for their own dependencies, discovers every supported "
    "reference under the admitted roots (src, test, and .clj-surgeon.edn "
    ":source-roots) under every spelling a file binds -- each :as alias, the "
    "fully qualified symbol with or without a require, and the referred bare "
    "name -- partitions the callers into moved_only, mixed, qualified_only and "
    "untouched, chooses each file's alias as the first alias_policy entry bound "
    "to nothing in that file, and commits ONE failure-atomic transaction whose "
    "proof runs in a fresh process before the commit stands. Never send a "
    "per-file, per-owner, or per-site table; Surgeon derives them, and the "
    "request refuses any field outside the closed set. expect is optional and "
    "is a strict guard when supplied. The receipt is one constant-size object: "
    "counts, the partition, the alias histogram, the executed profile's typed "
    "checks, and a details_path -- never a file list. A refusal is fail-closed, "
    "names the one unresolved decision, and carries next_call null: v1 offers no "
    "scope narrowing, caller exclusion, invented alias or weaker profile."))

;; @spec MCP-OP-HELPER-001
(defn tool
  "The `helper_extraction` registration map.

  A function rather than a def because `:tool-fn` and `:summarize` live in the
  registration layer that requires THIS namespace; see the namespace docstring."
  []
  {:id :helper-extraction
   :name operation
   :description tool-description
   :schema mcp-schema/helper-extraction-schema
   :inputSchema mcp-schema/helper-extraction-schema
   :output-schema mcp-schema/helper-extraction-output-schema
   :structured? true
   ;; @spec MCP-OP-COVERAGE-001
   ;; @spec MCP-OP-COVERAGE-002
   ;; Declared on the tool rather than in `mcp-server`'s fallback table, so the
   ;; verb's outcome vocabulary travels with the verb. The four terminal states
   ;; collapse onto three PUBLIC outcome classes: `committed` is the one success,
   ;; `verification-failed` covers the proof outcomes the kernel rolled back
   ;; (failure and timeout alike), and `typed-refusal` covers every pre-write
   ;; refusal AND `rollback-failed`, which is a receipt the caller must read
   ;; rather than a success.
   :outcome-classes #{:committed :verification-failed :typed-refusal}
   :summarize (some-> (requiring-resolve
                        'clj-surgeon.mcp-tool/helper-extraction-summary)
                      deref)
   :tool-fn (requiring-resolve 'clj-surgeon.mcp-tool/handle-helper-extraction)})
