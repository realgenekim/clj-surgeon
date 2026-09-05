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

(defn admitted-roots
  "Every discovery root this workspace admits, in a stable order."
  [project-root]
  (vec (distinct (concat planner/admitted-roots
                         (configured-source-roots project-root)))))

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

(defn- read-sources
  "The frozen read: `{:file relative :source text :authorized bool}` for every
  source under the admitted roots, in scan order.

  Every path is resolved through the root-confinement gate before it is read,
  so a symlink out of the workspace refuses rather than being slurped."
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

(defn profile-capability
  "What one configured verification profile can do, or nil when v1 cannot admit it.

  v1 admits ONLY profiles whose every check is an external command this process
  runs and waits on:

  * a `:cold` job is asynchronous — `launch!` returns `:running` and the receipt
    would say `verification_complete false`, which cannot gate a commit;
  * a `:hot` law runs inside a warm application JVM, which is precisely the
    stale-Var false proof MCP-OP-HELPER-022 exists to forbid.

  Everything admitted here is synchronous, runs in a fresh child process, and
  fails before the kernel's rollback authority is released."
  [spec]
  (cond
    (and (vector? spec) (seq spec) (every? string? spec))
    {:synchronous? true :rollback-capable? true :fresh-process? true
     :commands [spec] :shape :command}

    (and (map? spec) (= #{:acceptance :timeout-ms :commands} (set (keys spec))))
    {:synchronous? true :rollback-capable? true :fresh-process? true
     :commands (vec (:commands spec)) :timeout-ms (:timeout-ms spec)
     :shape :exact}

    (and (map? spec) (seq (:commands spec))
         (nil? (:hot spec)) (nil? (:cold spec)))
    {:synchronous? true :rollback-capable? true :fresh-process? true
     :commands (vec (:commands spec)) :shape :commands}

    :else nil))

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
                     (when (seq (:commands capability))
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
                               "workspace-unreadable"
                               "scope-unscannable"
                               "unreadable-source"
                               "destination-not-derivable"])))))

(defn- runnable-command?
  "Whether the profile's executable can be found now, before anything is staged.

  `expand-command` resolves a bare executable against the same search path the
  child would use; a command it cannot resolve is a profile that is not
  runnable, and MCP-OP-HELPER-011 wants that answered BEFORE the write, not as
  a launch failure after it."
  [command]
  (let [resolved (first (change-buffer/expand-command command []))]
    (or (str/includes? (str resolved) "/")
        (.isFile (io/file (str resolved))))))

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
  (let [;; the workspace's own configuration wins over the server registry for
        ;; a name both carry: a contract name the workspace gave a real command
        ;; is that command
        admitted (merge (admitted-profiles) (admitted-profiles profiles))
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

(defn- destination-limitation
  "A boundary refusal when the destination this seam would write is not the one
  `to.lib` names.

  The destination namespace must equal `to.lib` EXACTLY and its path must be
  project-relative. A seam that infers a namespace by walking up to the nearest
  ancestor directory called `src` writes `ancestor.project.src.acid.web.response`
  for a project that happens to live under one — so if that ever becomes the
  path taken, this names the KERNEL LIMITATION rather than passing a guessed
  namespace through (`next_call nil`: nothing the caller can send fixes it)."
  [dest-lib dest-file]
  (cond
    (str/blank? (str dest-file))
    (refusal "destination-not-derivable"
             (str "The destination path for " dest-lib " could not be derived"
                  " project-relatively from from.file.")
             {:limitation "destination-path-not-project-relative"
              :lib dest-lib
              :decision "which project-relative path the destination namespace occupies"})

    (or (str/starts-with? (str dest-file) "/")
        (str/starts-with? (str dest-file) "../"))
    (refusal "destination-not-derivable"
             (str "The destination path for " dest-lib " is not"
                  " project-relative: " dest-file)
             {:limitation "destination-path-not-project-relative"
              :lib dest-lib :file dest-file
              :decision "which project-relative path the destination namespace occupies"})))

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
                roots (admitted-roots (str root))
                discovery (scan root (root-globs roots))]
            (if-not (:ok discovery)
              (scan-refusal discovery "discovery")
              (let [authorized (scan root (get-in request [:scope :paths]))]
                (if-not (:ok authorized)
                  (scan-refusal authorized "authorization")
                  (let [sources (read-sources root (:files discovery)
                                              (set (:files authorized)))]
                    (if (map? sources)
                      sources
                      (let [planned (planner/plan request (mapv #(dissoc % :path) sources))]
                        (if-not (:ok planned)
                          planned
                          (or (destination-limitation
                                (get-in planned [:plan :destination :lib])
                                (get-in planned [:plan :destination :file]))
                              (assoc planned
                                     :roots roots
                                     :paths (into {} (map (juxt :file :path)) sources)
                                     ;; the FROZEN read, carried forward by the
                                     ;; plan rather than re-slurped at write
                                     ;; time: the kernel's stale-source gate has
                                     ;; to see the bytes the plan was derived
                                     ;; from, or drift between the two commits
                                     ;; silently over a stale plan
                                     :sources (into {} (map (juxt :path :source)) sources))))))))))))))))))

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

(defn- plan-counts
  "The O(1) counts the plan already derived, folded in without recomputation."
  [plan]
  (let [receipt (:receipt plan)]
    (cond-> {}
      (map? receipt) (merge (select-keys receipt
                                         [:helpers :source_retired :caller_files
                                          :sites :retained_sites :alias_histogram
                                          :partition :closure]))
      (map? (:counts plan)) (assoc :counts (:counts plan))
      (some? (:partition plan)) (assoc :partition (:partition plan))
      (coll? (:helpers plan)) (assoc :helpers (count (:helpers plan)))
      (map? (:destination plan)) (assoc :destination_lib
                                        (get-in plan [:destination :lib])))))

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
        restored (when (contains? kernel :restored) (boolean (:restored kernel)))]
    (cond-> (merge {:operation operation
                    :status (if status (name status) "unknown")
                    :verification (verification-face verification)}
                   (plan-counts plan))
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
      (contains? kernel :restoration_read_back)
      (assoc :restoration_read_back (:restoration_read_back kernel))
      (contains? kernel :restored_files)
      (assoc :restored_files (vec (:restored_files kernel)))
      ;; @spec MCP-OP-HELPER-020
      ;; a rollback that did not complete NAMES the files it could not restore
      ;; and carries the kernel's own recovery-required evidence
      (contains? kernel :unrestored_files)
      (assoc :files (vec (:unrestored_files kernel)))
      (contains? kernel :recovery_required)
      (assoc :recovery_required (:recovery_required kernel))
      (contains? kernel :details_path) (assoc :details_path (:details_path kernel))
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
  (let [timeout (or (:timeout-ms capability) default-proof-timeout-ms)
        outcomes
        (reduce (fn [acc command]
                  (let [argv (change-buffer/expand-command command [])
                        process (change-buffer/run-process!
                                  project-root argv timeout max-proof-output-bytes)
                        outcome (assoc (select-keys process
                                                    [:exit :elapsed_ms :finished?
                                                     :output :output-bytes
                                                     :output-sha256 :output-truncated])
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
       ;; a child process ran to completion and answered: that is the fact
       :fresh_process (boolean (some :exit outcomes))
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

(defn- sha256
  [text]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" (bit-and 0xff %))
                    (.digest digest (.getBytes ^String text "UTF-8"))))))

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
          (let [capability (get (merge (admitted-profiles)
                                       (admitted-profiles profiles))
                                profile-name)
                ;; the injectable proof step. One key, one default, no behavior
                ;; change: the production path is `run-proof!` itself, and a
                ;; caller that supplies its own is exercising the same seam the
                ;; boundary uses.
                proof! (or (:run-proof! config) run-proof!)
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
                  (let [receipt-dir (or (:receipt-dir config)
                                        (workspace/receipt-dir project-root))
                        receipt-file (str (io/file receipt-dir (str (UUID/randomUUID) ".edn")))
                        details-file (str (io/file receipt-dir
                                                   (str "helper-extraction-"
                                                        (UUID/randomUUID) ".edn")))
                        started (System/nanoTime)
                        result (extraction/commit! compiled)]
                    (if-not (:ok result)
                      ;; the extraction kernel refused or restored its own
                      ;; partial write: nothing of this transaction stands, and
                      ;; the receipt says so as a typed refusal rather than as a
                      ;; terminal state, because no terminal state was reached
                      (refusal "transaction-refused"
                               (str "The extraction kernel did not commit: "
                                    (or (:error result)
                                        (some-> (:error-type result) name)
                                        "unknown"))
                               {:kernel_error_type (some-> (:error-type result) name)
                                :kernel_error (:error result)
                                :decision "what the kernel refused about this transaction"})
                      ;; ONE encompassing guard from the first written byte to
                      ;; the terminal receipt. Publishing a receipt, running the
                      ;; proof and mapping the result can all THROW, and a throw
                      ;; after the commit would otherwise leave the tree
                      ;; modified with nobody rolling it back. Every exit below
                      ;; goes through `finish-failure!`, which undoes through
                      ;; the extraction kernel's own hash-fenced `undo!` and
                      ;; reports `rollback-failed` when that undo does not
                      ;; verify — this function never restores a byte itself.
                      (let [touched (mapv :file (get-in result [:receipt :files]))
                            elapsed #(/ (double (- (System/nanoTime) started)) 1000000.0)
                            finish-failure!
                            (fn [failed-state proof cause]
                              (let [rollback (try (extraction/undo! (:receipt result))
                                                   (catch Throwable undo-error
                                                     {:ok false
                                                      :error (or (.getMessage undo-error)
                                                                 (.getName (class undo-error)))
                                                      :threw true}))
                                    rolled-back? (boolean (:ok rollback))]
                                (when rolled-back?
                                  (try (.delete (io/file receipt-file))
                                       (catch Exception _ nil)))
                                (cond->
                                  (terminal-receipt
                                    {:kernel (if rolled-back?
                                               {:status failed-state
                                                :restored true
                                                :restored_files touched
                                                :restoration_read_back
                                                (try (restoration-read-back touched)
                                                     (catch Throwable _ {}))
                                                :destination_removed true
                                                :details_path details-file
                                                :elapsed_ms (elapsed)}
                                               {:status :rollback-failed
                                                :restored false
                                                :unrestored_files touched
                                                :recovery_required
                                                {:receipt receipt-file
                                                 :reason (or (:error rollback)
                                                             "the extraction undo did not verify")
                                                 :recovery rollback}
                                                :details_path details-file
                                                :elapsed_ms (elapsed)})
                                     :verification proof
                                     :plan (:plan planned)})
                                  cause (assoc :cause_error cause))))]
                        (try
                          (.mkdirs (io/file receipt-dir))
                          (file-ops/atomic-write! receipt-file (pr-str (:receipt result)))
                          ;; @spec MCP-OP-HELPER-009
                          ;; per-caller detail lives beside the undo receipt in
                          ;; the kernel's own LOCAL-STATE receipt directory:
                          ;; this verb publishes nothing into the workspace it
                          ;; mutated
                          (file-ops/atomic-write!
                            details-file (pr-str (select-keys (:plan planned)
                                                              [:destination :files :moved])))
                          (let [proof (proof! project-root profile-name capability)]
                            (if (:ok proof)
                              (terminal-receipt
                                {:kernel {:status :committed
                                          :destination_created true
                                          :undo_receipt receipt-file
                                          :receipt_hash (:receipt-hash result)
                                          :details_path details-file
                                          :elapsed_ms (elapsed)}
                                 :verification proof
                                 :plan (:plan planned)})
                              (finish-failure! (if (:timed_out proof)
                                                 :verification-timeout
                                                 :verification-failed)
                                               proof nil)))
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
