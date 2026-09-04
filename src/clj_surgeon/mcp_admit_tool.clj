(ns clj-surgeon.mcp-admit-tool
  "The admit_clojure_patch gate: one receipt for a natively authored patch.

  The caller keeps its own route. It composes the unified diff it would have
  handed to apply_patch and hands it here instead, at the exact moment it would
  otherwise pay three separate returns — re-read the file, run git diff, run a
  focused test. The gate applies the patch to a frozen in-memory snapshot,
  reports the change in form identity rather than in lines, refuses the closed
  set of hazards a text patcher cannot see, verifies the snapshot, and only
  then commits atomically. Everything it writes goes through the existing
  transaction commit path; everything it confines goes through the existing
  path guards."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.diagnostic-delta :as diagnostic-delta]
   [clj-surgeon.form-identity :as form-identity]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.mcp-workspace-sources :as workspace-sources]
   [clj-surgeon.mcp-write-refusal :as write-refusal]
   [clj-surgeon.patch-apply :as patch-apply]
   [clj-surgeon.workspace-lock :as workspace-lock]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def max-patch-bytes
  "Admission limit, counted in UTF-8 bytes.

  A character count is the wrong meter for a payload whose cost is bytes: a
  patch of multibyte source can be twice its character count on the wire and
  in every buffer it passes through."
  262144)
(def analyzed-extensions #{"clj" "cljc" "cljs"})
(def data-extensions #{"edn"})

(def report-file-name ".clj-surgeon-focused-test-report")

(def ^:private trimmable-receipt-keys
  "Receipt collections that grow with the patch and may be trimmed to fit."
  [:hazards :files])

;; @spec MCP-OP-ADMIT-153
;; @spec MCP-OP-ADMIT-155
;; @spec MCP-OP-ADMIT-157
(def admit-tool-description
  (str
    "Admit one natively authored patch and return a single receipt. "
    "PATCH: paste the exact text you would give apply_patch -- either the "
    "*** Begin Patch/*** End Patch grammar or a plain unified diff (diff "
    "--git and git's extended headers are accepted). Nothing else is "
    "required: the gate freezes every file the patch touches, derives the "
    "pre-image itself, and re-reads all of them under an exclusive write lock "
    "immediately before writing, so a tree that moves refuses with "
    "source-hash-mismatch naming the file (pre_image_binding: derived). "
    "Send expect_pre_sha256 only to ALSO assert the bytes an earlier preview "
    "showed you (pre_image_binding: bound) -- never compute digests just to "
    "call this tool. "
    "MODE: propose runs every check in the snapshot, writes nothing, and does "
    "NOT refuse on a failing check -- it returns verify_ok:false plus the "
    "failing command's exit code and last 40 lines, so your loop is propose "
    "-> read the failing lines -> fix -> commit, inside the gate. preview is "
    "the default and is the same reading, except a failed check IS a refusal. "
    "commit writes every changed file in one atomic compare-and-swap "
    "transaction, and only on a complete verification. "
    "VERIFY: \"focused\" uses this repository's .clj-surgeon/focused-test.edn "
    "profile; \"none\" checks nothing and can never commit; or pass your OWN "
    "commands and no profile is needed at all -- verify: {\"commands\": "
    "[\"make test\", \"npx jest\"]} (optional \"cwd\", \"timeout_ms\"). Each "
    "command runs, in order, inside a copy of this workspace with your patch "
    "already applied, and the receipt names it, its exit code and its last "
    "lines; a non-zero exit stops the sequence and blocks the write. "
    "REPORT: the change as form identity -- which top-level owners changed, "
    "added or vanished; which bytes moved with no structural reason; which "
    "comments, metadata, reader conditionals or #_ discards were disturbed; "
    "and typed hazards a line patcher cannot see (unreadable post image, "
    "duplicate top-level definition however it is wrapped, a lost ns require, "
    "an edit inside an opaque code-shaped string). verification_complete is "
    "true only when the analyzer ran clean and the checks you asked for "
    "actually produced a result. "
    "One call replaces the re-read, the git diff, and the test run."))

(def admit-tool-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string"}
    ;; A soft note for callers; the enforced limit is UTF-8 bytes.
    "patch" {:type "string" :maxLength max-patch-bytes}
    ;; @spec MCP-OP-ADMIT-157
    "mode" {:type "string" :enum ["preview" "propose" "commit"]}
    ;; @spec MCP-OP-ADMIT-153
    "verify" {:oneOf
              [{:type "string" :enum ["focused" "none"]}
               {:type "object"
                :properties
                {"commands" {:type "array"
                             :items {:type ["string" "array"]}
                             :minItems 1
                             :maxItems 8}
                 "profile_from_receipt" {:type "array"}
                 "cwd" {:type "string"}
                 "timeout_ms" {:type "number"}}}]}
    ;; @spec MCP-OP-ADMIT-106
    "allow_partial" {:type "boolean"}
    "expect_pre_sha256" {:type "object"
                         :additionalProperties {:type "string"}}}
   :required ["patch"]})

;; @spec MCP-OP-SCHEMA-001
(def admit-output-schema
  {:type "object"
   :properties {"ok" {:type "boolean"}
                "elapsed_ms" {:type "number" :minimum 0}}
   :required ["ok" "elapsed_ms"]})

(def ^:private runtime-config runtime/tool-config)

(defn init!
  "Set the live admit tool configuration. Passing nil disarms it."
  [config]
  (reset! runtime-config
          (when config
            (assoc config :workspace-router (workspace/router config)))))

;; ---------------------------------------------------------------------------
;; Receipt construction
;; ---------------------------------------------------------------------------

(defn patch-bytes
  ;; @spec MCP-OP-ADMIT-086
  [patch]
  (count (.getBytes (str patch) "UTF-8")))

(defn patch-digest
  [patch]
  (when (string? patch) (structural-lens/source-hash patch)))

;; @spec MCP-OP-ADMIT-140
(def ^:private caller-field-spelling-characters
  "How much of a caller-supplied value a refusal may quote back.

  Enough for the caller to recognise the value that was refused; never enough
  for the value to be the reason the receipt does not fit."
  120)

;; @spec MCP-OP-ADMIT-140
(defn- bounded-caller-spelling
  "One caller-supplied value as a quoted, BOUNDED spelling with the cut stated.

  A refusal that names the field a caller got wrong has to show enough of the
  value to be recognisable. A refusal that echoes the value VERBATIM lets the
  caller choose the size of clj-surgeon's receipt: a 60,000-character `mode`
  published a 61,214-byte receipt whose own sentence called 32,640 the budget
  (MCP-OP-ADMIT-140)."
  [value]
  (let [text (if (string? value) value (pr-str value))
        n (count text)]
    (if (<= n caller-field-spelling-characters)
      (str "\"" text "\"")
      (str "\"" (subs text 0 caller-field-spelling-characters)
           "\" […" (- n caller-field-spelling-characters)
           " more character(s), cut here so a caller's value cannot be the"
           " size of this receipt]"))))

;; @spec MCP-OP-ADMIT-069
(defn- next-call
  "One executable follow-up that never carries the patch back.

  Echoing the payload would let a refusal grow without bound with the very
  input that caused it, and the caller already holds the text. The digest
  binds the follow-up to the same patch without restating it."
  [{:keys [workspace-root patch verify expect-pre]} mode blocked-by]
  ;; @spec MCP-OP-ADMIT-140
  ;; A follow-up call is a call the caller SHOULD send, so it never echoes a
  ;; caller's out-of-enum `mode` or `verify` back: those two are the fields
  ;; whose legal values this gate declares, and a next_call proposing the
  ;; unusable value that just refused is neither sendable nor an affordance.
  (cond-> {:tool "admit_clojure_patch"
           ;; @spec MCP-OP-ADMIT-153
           ;; @spec MCP-OP-ADMIT-157
           :arguments (cond-> {:mode (if (contains? #{"preview" "propose"
                                                      "commit"} mode)
                                       mode
                                       "preview")
                               :verify (cond
                                         (contains? #{"focused" "none"} verify)
                                         verify
                                         ;; An inline plan IS a sendable
                                         ;; verify value, so it travels whole.
                                         (map? verify) verify
                                         :else "focused")}
                        workspace-root (assoc :workspace_root workspace-root)
                        (seq expect-pre) (assoc :expect_pre_sha256 expect-pre))
           :patch_field "patch"
           :patch_sha256 (patch-digest patch)
           :note (str "resend the same patch text in the patch field; "
                      "it is deliberately not echoed here")}
    blocked-by (assoc :blocked_by blocked-by)))

;; @spec MCP-OP-ADMIT-116
(defn- hazard-lift
  "What would lift this refusal, named on the refusal itself.

  A refusal whose only follow-up is `preview` tells the caller to run the call
  that just refused. That is what a correct dead-require sewing patch met in
  the field: no override, no sites, and no way to tell whether the gate had
  found something real. Every refusal now carries either the sites to repair
  or an explicit statement that nothing lifts it."
  [hazard]
  (or (:lift hazard)
      {:description (str "no lift is recorded for a "
                         (name (or (:type hazard) :unknown))
                         " refusal; treat it as blocking and repair the "
                         "condition the message names")
       :liftable false
       :sites []}))

;; @spec MCP-OP-ADMIT-050
(defn- empty-receipt
  "The closed receipt key set, so no path can publish a partial payload."
  [mode]
  {:ok true
   :operation :admit-patch-preview
   :mode mode
   :committed false
   ;; @spec MCP-OP-ADMIT-105
   :mutation_attempted false
   :files []
   :owners {:added [] :removed [] :changed []}
   :protected_node_drift {}
   :byte_drift_outside_hunks 0
   :hazards []
   :lint_delta {:ran false}
   :tests {:ran false :passed 0 :failed 0 :skipped 0 :namespaces []}
   :hashes {}
   :pre_image_binding "unbound"
   :lock_scope :none
   :verification_status :unverified
   :verification_reasons []
   ;; @spec MCP-OP-ADMIT-123
   ;; @spec MCP-OP-ADMIT-125
   ;; Absent, not empty. `[]` is the affirmative claim that every requested
   ;; detector answered, and the seed is merged onto receipts -- refusals
   ;; parsed, no-op and hazard -- where no detector was ever consulted.
   :detectors_not_run nil
   :verification_complete false
   ;; @spec MCP-OP-ADMIT-157
   ;; The verification's own verdict as a FIELD, so `propose` can publish a
   ;; failing check on a receipt that is not a refusal. `nil` is the third
   ;; state and the seed: no verification was run at all.
   :verify_ok nil
   :source-unchanged true
   :next_call nil})

;; @spec MCP-OP-ADMIT-133
;; @spec MCP-OP-ADMIT-138
;; One member of this set -- `:transaction-recovery-required` -- is proved by
;; `make admit-transaction-recovery-battery` rather than by the fast suite,
;; because the only fixture that can produce it is a widened race and a timing
;; bound is a battery target. The exemption is declared in
;; `clj-surgeon.admit-patch-test/battery-only-refusal-kinds` with the target
;; that proves it.
(def admit-refusal-kinds
  "THE enumeration of `:error-type` values the admit gate may publish.

  Round three DERIVED this set by scanning five source files for literal
  shapes. That derivation was already wrong -- it missed
  `:workspace-lock-unavailable`, which the suite drives live -- and it was
  wrong in a way no witness could see, because a kind built dynamically
  (`(keyword (str ...))`, or forwarded out of another namespace's `ex-data`
  at line 1901 below) has no literal to scan for. A reviewer planted exactly
  such a kind and every enumeration witness stayed green.

  So the enumeration is no longer derived from TEXT. It is declared here,
  enforced by `checked-refusal-kind!` at the choke point every published
  receipt passes through, and proved complete by EXECUTION: the suite records
  every kind the entrance actually publishes and asserts set equality with
  this def in both directions. A kind that reaches the surface without a
  member here throws. A member here that nothing drives is a claim about the
  gate that no fixture supports, and fails the same witness. The source scan
  survives only as a complement, checking that no kind constructed in the
  files the gate calls is missing from this set or from the justified
  not-reachable list beside it."
  #{:admit-tool-error
    :admit-tool-failure
    :analyzer-memory-exhausted
    :binary-patch-unsupported
    :duplicate-definition
    :duplicate-patch-target
    :hunk-truncated
    :invalid-admit-request
    :invalid-patch
    :invalid-relative-source-path
    ;; @spec MCP-OP-ADMIT-133
    ;; `:invalid-source-path` was a member here until round fifteen's merge
    ;; with the census landing. It is constructed at `mcp-paths` :283, in the
    ;; `:else` arm of `resolve-source-path`'s catch -- "not the filesystem
    ;; answering" -- and the one fixture that used to drive it, a source path
    ;; under a REGULAR FILE, now publishes `:source-file-not-found`: the catch
    ;; asks the whole `FileSystemException` hierarchy and ENOTDIR is a member
    ;; of it. Every remaining shape that makes `.toRealPath` throw was driven
    ;; (`no-filesystem-shape-reaches-invalid-source-path`) and each is claimed
    ;; by a TYPED arm above the `:else`; the only class that could reach it,
    ;; `InvalidPathException` for a NUL, is rejected lexically by
    ;; `relative-source-path?` before any I/O. A member here that nothing
    ;; drives is a claim about the gate no fixture supports -- this
    ;; docstring's own rule -- so the kind moved to the justified
    ;; not-reachable list in `clj-surgeon.admit-patch-test`, where the source
    ;; scan still watches it.
    :invalid-workspace-root
    :namespace-form-removed
    :next-call-exceeds-public-budget
    :no-op-patch
    :overlapping-hunks
    :patch-does-not-apply
    :patch-too-large
    :path-outside-project
    ;; @spec MCP-OP-ADMIT-148
    :request-decode-constraint-exceeded
    :require-removed
    :server-not-initialized
    :source-file-not-found
    :source-hash-mismatch
    ;; @spec MCP-OP-ADMIT-133
    ;; Added by round fifteen's merge with the census landing, which
    ;; introduced it at `mcp-paths` :220 (the file's own bits deny read) and
    ;; :262 (a directory above it does). Enumerated because it is DRIVEN --
    ;; `an-unreadable-source-refuses-as-source-not-readable` drives both
    ;; sites through the real entrance with `chmod 000` -- and not merely
    ;; because the trunk constructs it.
    :source-not-readable
    :source-not-regular-file
    :target-already-exists
    :target-parent-not-directory
    :transaction-recovery-required
    :transaction-write-failed
    :unreadable-post-image
    :unsupported-patch-target
    :verification-failed
    :verification-incomplete
    :workspace-lock-unavailable})

;; @spec MCP-OP-ADMIT-133
;; @spec MCP-OP-ADMIT-137
(defn checked-refusal-kind!
  "Return `receipt` unchanged, or throw if it refuses under an unenumerated
  kind.

  A plain `IllegalArgumentException`, deliberately. An `ex-info` carrying an
  `:error-type` is exactly the shape this namespace's own catch clauses know
  how to turn back into a receipt, so the violation would launder itself into
  the surface the guard exists to protect.

  Called from `bound-receipt` and from the MCP handler's edge, and NOT from
  the inner `refusal` helper. `refusal` runs inside the gate's own
  `(catch Exception ...)`, which would swallow this throw and relabel it
  `:admit-tool-failure` -- an enumerated kind. A guard whose violation is
  caught and renamed to something legal is not a guard, so the enforcement
  point is the one that sits outside every catch on the path.

  The predicate is `(not (true? ...))` and not `(false? ...)` because the
  RENDERER's predicate is truthiness: `summary` shows anything falsey as a
  refusal. A guard testing `false?` therefore let `:ok nil` reach the caller
  as a refusal under a kind nothing enumerated, and `refusal` merges its
  caller's `data` map last, so that override is one keyword away
  (MCP-OP-ADMIT-137). The two faces of a receipt must not disagree about
  which one it is."
  [receipt]
  (when (and (map? receipt)
             (not (true? (:ok receipt)))
             (not (contains? admit-refusal-kinds (:error-type receipt))))
    (throw (IllegalArgumentException.
             (str "admit gate refusal kind is not enumerated: "
                  (pr-str (:error-type receipt))
                  " -- add it to clj-surgeon.mcp-admit-tool/admit-refusal-kinds"
                  " with a fixture that drives it through the entrance, or"
                  " stop constructing it"))))
  receipt)

;; @spec MCP-OP-ADMIT-055
(defn- refusal
  [context error-type message & [data]]
  (merge (empty-receipt (or (:mode context) "preview"))
         {:ok false
          :operation :admit-patch-refused
          :error-type error-type
          :error message
          :next_call (next-call context "preview" error-type)}
         data))

;; ---------------------------------------------------------------------------
;; Request shape
;; ---------------------------------------------------------------------------

(defn- extension
  [path]
  (let [dot (str/last-index-of path ".")]
    (when dot (str/lower-case (subs path (inc dot))))))

;; @spec MCP-OP-ADMIT-005
;; @spec MCP-OP-ADMIT-070
(defn- file-kind
  [path]
  (let [ext (extension path)]
    (cond
      (contains? analyzed-extensions ext) "clojure"
      (contains? data-extensions ext) "data"
      :else "passthrough")))

;; ---------------------------------------------------------------------------
;; Verification
;; ---------------------------------------------------------------------------

(defn- temp-tree!
  [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (and file (.exists ^java.io.File file))
    (doseq [child (reverse (file-seq file))]
      (.delete ^java.io.File child))))

(defn- materialize!
  [root images image-key]
  (mapv (fn [{:keys [file] :as image}]
          (let [target (io/file root file)]
            (.mkdirs (.getParentFile target))
            (spit target (get image image-key))
            (.getPath target)))
        images))

(def default-analyzer-command
  "The analyzer invocation, before the file list is expanded into it."
  ["clj-kondo" "--lint" "{files}"])

;; @spec MCP-OP-ADMIT-122
(def analyzer-findings-visible-bytes
  "How many bytes of analyzer findings this gate will read back, at most.

  This is NOT the receipt budget and must never be set from it. The receipt
  budget answers `how much may we publish to the caller?` and its job is to
  bound noise. This answers `how much of the analyzer's answer may the
  detector see before it decides?` and a cap there is a cap on truth: the
  parse is all-or-nothing, so one byte over the line does not degrade the
  delta, it deletes it.

  The number exists only to bound this process's heap against a runaway
  analyzer. It is deliberately three orders of magnitude above anything a
  real patch provokes -- the largest findings payload measured over the 14
  frozen field patches was 21,883 bytes, and `max-patch-bytes` caps a patch
  at 262,144 bytes of diff -- so that reaching it is a genuine anomaly, and
  by MCP-OP-ADMIT-121 a named one rather than a silent one."
  (* 16 1024 1024))

;; @spec MCP-OP-ADMIT-121
(defn analyzer-read-ceiling
  "How many bytes of analyzer output this gate will read back.

  Named on the config so a workspace can raise it, and named on the refusal so
  a caller who hits it can see what to raise."
  [config]
  (let [declared (:admit-analyzer-visible-bytes config)]
    (if (and (number? declared) (pos? declared))
      (long declared)
      (long analyzer-findings-visible-bytes))))

;; @spec MCP-OP-ADMIT-127
(def analyzer-admission-remedies
  "What would lift each analyzer admission failure, in the caller's terms."
  {:clj-kondo-admission-unavailable
   (str "this server cannot find its own analyzer admission wrapper; install"
        " it (make install-clj-kondo-admission) or name it on"
        " CLJ_SURGEON_CLJ_KONDO_ADMISSION")
   :clj-kondo-executable-unavailable
   (str "clj-kondo is not resolvable from this server's PATH; install it, or"
        " name the analyzer on :admit-analyzer-command")
   :clj-kondo-pressure-deferred
   (str "the host was above the analyzer admission load ceiling and the run"
        " was deferred rather than queued; retry when the box is quieter, or"
        " raise CLJ_SURGEON_CLJ_KONDO_MAX_NORMALIZED_LOAD")
   :clj-kondo-admission-timeout
   (str "the analyzer waited for the machine-wide admission lock past this"
        " call's deadline; retry, or raise the analyzer timeout")
   :process-interrupted
   "the analyzer run was interrupted before it answered; retry"})

;; @spec MCP-OP-ADMIT-127
(defn analyzer-admission-failure
  "The typed admission failure a bounded analyzer run reported, or nil.

  `run-process!` already preserves both halves of this fact -- the wrapper's
  terminal evidence on `:admission` when the child exited, and the launch
  exception's ex-data on `:admission-error` when it did not -- and
  `kondo-findings` read neither, so a transient pressure deferral, an
  admission timeout, a missing wrapper and a missing executable all published
  the one type reserved for an analyzer that answered something unreadable.
  A gate that reports a missing tool it is in fact holding cannot be acted
  on: the four states have four different remedies."
  [{:keys [admission admission-error]}]
  (let [nested (:admission admission-error)
        error-type (or (:error-type admission-error)
                       (:error-type nested)
                       (:error-type admission))]
    (when error-type
      {:error-type error-type
       :status (or (:status nested) (:status admission))
       :gate (or (:gate admission-error) (:gate nested) (:gate admission))
       :remedy (get analyzer-admission-remedies error-type)})))

;; @spec MCP-OP-ADMIT-121
;; @spec MCP-OP-ADMIT-127
(defn- kondo-findings
  "Run clj-kondo over one materialized image set and return its findings.

  Two failures live here and they are not the same fact. An analyzer that did
  not answer -- absent, unlaunchable, killed, or answering something that is
  not a findings map -- is `clj-kondo-unavailable`. An analyzer that answered
  more than this gate read back is `analyzer-output-truncated`: the detector
  ran, the truth existed, and the ceiling cut it. Collapsing the second into
  the first is how a gate reports a missing tool it is in fact holding."
  [config paths]
  (let [project-root (:project-root config)
        ceiling (analyzer-read-ceiling config)
        command (-> (change-buffer/expand-command
                      (or (:admit-analyzer-command config)
                          default-analyzer-command)
                      (vec paths))
                    (into ["--cache" "false"
                           "--config" "{:output {:format :edn}}"]))
        raw (change-buffer/run-process! project-root command 120000 ceiling)
        {:keys [finished? exit output output-bytes output-truncated]} raw
        ;; @spec MCP-OP-ADMIT-127
        admission (analyzer-admission-failure raw)
        parsed (when (and finished? (not output-truncated))
                 (try (edn/read-string output) (catch Exception _ nil)))]
    (cond
      output-truncated
      (let [observed (long (or output-bytes 0))]
        {:ok false
         :error-type :analyzer-output-truncated
         :detector "clj-kondo"
         :cap ceiling
         :observed-bytes observed
         :exit exit
         :remedy (str "clj-kondo answered with " observed
                      " bytes of findings and this gate reads at most "
                      ceiling
                      "; raise the analyzer read ceiling"
                      " (:admit-analyzer-visible-bytes) or narrow the patch"
                      " to fewer files")
         :error (str "clj-kondo findings were cut at " ceiling " bytes of "
                     observed "; the analyzer ran and the gate could not read"
                     " its answer")})

      ;; @spec MCP-OP-ADMIT-127
      ;; Before the readability test, because an analyzer that was never
      ;; admitted did not answer unreadably -- it did not answer.
      admission
      (cond-> {:ok false
               :error-type (:error-type admission)
               :detector "clj-kondo"
               :admission_failure true
               :error (str "the analyzer did not run: "
                           (name (:error-type admission)))
               :exit exit}
        (:gate admission) (assoc :gate (:gate admission))
        (:status admission) (assoc :admission_status (:status admission))
        (:remedy admission) (assoc :remedy (:remedy admission)))

      (and (map? parsed) (vector? (:findings parsed)))
      {:ok true :findings (:findings parsed)}

      :else
      {:ok false
       :error-type :clj-kondo-unavailable
       :detector "clj-kondo"
       :error "clj-kondo did not produce readable findings"
       :exit exit
       :output (when output (subs output 0 (min 400 (count output))))})))

;; @spec MCP-OP-ADMIT-040
(defn default-lint-runner
  "Compare analyzer findings between the pre and post images.

  Both images are materialized outside the workspace, so the delta is computed
  against the snapshot and is identical in preview and in commit. Findings are
  compared location-independently, so an unrelated edit that merely moves an
  existing finding is not a regression."
  [config images]
  (if (empty? images)
    {:ran false :reason :no-clojure-files}
    (let [before (temp-tree! "clj-surgeon-admit-pre")
          after (temp-tree! "clj-surgeon-admit-post")]
      (try
        (let [pre (kondo-findings config (materialize! before images :pre))
              post (kondo-findings config (materialize! after images :post))]
          (if-not (and (:ok pre) (:ok post))
            ;; @spec MCP-OP-ADMIT-121
            ;; The failing half speaks for itself: its type, the ceiling it
            ;; hit, what it observed, and what would lift it. A single
            ;; hard-coded sentence here is what turned a read ceiling into a
            ;; report of a missing analyzer.
            (let [failed (if (:ok pre) post pre)]
              (cond-> {:ran false
                       :ok false
                       :status :unverified
                       :detector (or (:detector failed) "clj-kondo")
                       :error-type (:error-type failed)
                       :error (or (:error failed)
                                  "clj-kondo did not produce readable findings")}
                (:cap failed) (assoc :cap (:cap failed))
                (:observed-bytes failed)
                (assoc :observed-bytes (:observed-bytes failed))
                ;; @spec MCP-OP-ADMIT-127
                (:gate failed) (assoc :gate (:gate failed))
                (:admission_status failed)
                (assoc :admission_status (:admission_status failed))
                (:admission_failure failed)
                (assoc :admission_failure true)
                (:remedy failed) (assoc :remedy (:remedy failed))))
            (let [strip (fn [root findings]
                          (mapv #(update % :filename
                                         (fn [f]
                                           (str/replace (str f)
                                                        (str (.getPath ^java.io.File root) "/")
                                                        "")))
                                findings))
                  delta (diagnostic-delta/diagnostic-delta
                          {:findings (strip before (:findings pre))}
                          {:findings (strip after (:findings post))})]
              (-> delta
                  (assoc :ran true)
                  (update :introduced #(vec (take 20 %)))
                  (update :removed #(vec (take 20 %)))
                  (update :blocking-introduced #(vec (take 20 %)))))))
        (finally
          (delete-tree! before)
          (delete-tree! after))))))

(defn- report-row
  "Normalize one namespace's counts from any supported report dialect."
  [value]
  (let [get* (fn [& keys] (some #(or (get value %) (get value (name %))) keys))
        number (fn [v] (cond (number? v) (long v)
                             (string? v) (or (parse-long v) 0)
                             :else 0))]
    {:tests (number (get* :tests :test :count))
     :failures (number (get* :failures :fail :failed))
     :errors (number (get* :errors :error))}))

(defn parse-test-report
  "Read a focused-test report as namespace -> {:tests :failures :errors}.

  EDN, JSON, and JUnit XML are accepted, because the report is written by
  whatever runner the repository already owns and the gate has no business
  dictating its serializer. What the gate does dictate is that the numbers
  arrive in a file the runner wrote, not in text the runner printed."
  [text]
  (let [trimmed (str/trim (str text))]
    (cond
      (str/blank? trimmed) nil

      (str/starts-with? trimmed "<")
      (let [rows (re-seq #"<testsuite\s+([^>]*)>" trimmed)
            attribute (fn [attrs name]
                        (second (re-find (re-pattern (str name "=\"([^\"]*)\""))
                                         attrs)))]
        (when (seq rows)
          (into {}
                (keep (fn [[_ attrs]]
                        (when-let [name (attribute attrs "name")]
                          [name (report-row {:tests (attribute attrs "tests")
                                             :failures (attribute attrs "failures")
                                             :errors (attribute attrs "errors")})])))
                rows)))

      :else
      (let [value (or (try (edn/read-string trimmed) (catch Exception _ nil))
                      (try (json/parse-string trimmed true) (catch Exception _ nil)))]
        (when (map? value)
          (into {} (map (fn [[key row]]
                          [(if (keyword? key) (name key) (str key))
                           (report-row row)]))
                value))))))

;; @spec MCP-OP-ADMIT-081
(defn read-focused-test-file
  "The repository-declared focused-test profile, if the workspace ships one."
  [project-root]
  (let [file (io/file (str project-root) ".clj-surgeon" "focused-test.edn")]
    (when (.isFile file)
      (try
        (let [value (edn/read-string (slurp file))]
          (when (map? value)
            (assoc value :profile-source :repository-file)))
        (catch Exception _ nil)))))

;; @spec MCP-OP-ADMIT-081
;; @spec MCP-OP-ADMIT-110
(defn resolve-focused-test
  "The tree's own statement first, the server's start configuration second,
  merged one key at a time.

  The two sources answer different halves of the question, which is why whole
  map precedence is wrong in both directions. A tree says *which suites cover
  which sources* -- that is a fact about the code, it travels with the code,
  and no server can know it. A server says *how to run a suite on this box* --
  the classpath, the deps, the timeout -- which no tree can know. The gate ran
  for three cohorts with the server outranking the tree, so a workspace that
  shipped `{:namespaces {...}}` and nothing else had its coverage statement
  read and discarded, and the gate fell back to deriving `<ns>-test` by path
  convention. Flipping whole maps instead would be no better: the profile
  those trees ship declares no `:command`, so the repository file would have
  swallowed the only command there was and verification would have stopped
  entirely."
  [{:keys [focused-test project-root]}]
  (let [server (when (map? focused-test) focused-test)
        repo (read-focused-test-file project-root)]
    (when (or server repo)
      (let [command (or (:command repo) (:command server))
            timeout (or (:timeout-ms repo) (:timeout-ms server))
            mapping (or (:namespaces repo) (:namespaces server))]
        (cond-> {:command command
                 :profile-source (cond (:command repo) :repository-file
                                       (:command server) :server-config
                                       :else :none)
                 :namespaces-source (cond (:namespaces repo) :repository-file
                                          (:namespaces server) :server-config
                                          :else :path-convention)}
          timeout (assoc :timeout-ms timeout)
          mapping (assoc :namespaces mapping))))))

(defn source-namespace
  "Derive the namespace a project-relative source path declares."
  [path]
  (let [without-extension (str/replace path #"\.[^./]+$" "")
        segments (str/split without-extension #"/")
        below-root (if (< 1 (count segments)) (rest segments) segments)]
    (-> (str/join "." below-root)
        (str/replace "_" "-"))))

(defn test-namespace-file
  "Project-relative file a `<ns>-test` namespace would occupy."
  [path]
  (let [without-extension (str/replace path #"\.[^./]+$" "")
        ext (or (extension path) "clj")
        segments (str/split without-extension #"/")
        below-root (if (< 1 (count segments)) (rest segments) segments)]
    (str "test/" (str/join "/" below-root) "_test." ext)))

;; @spec MCP-OP-ADMIT-111
(defn namespace-source-paths
  "Project-relative files a test namespace could occupy, in search order."
  [namespace]
  (let [stem (-> (str namespace)
                 (str/replace "-" "_")
                 (str/replace "." "/"))]
    (mapv #(str "test/" stem "." %) ["clj" "cljc" "cljs"])))

;; @spec MCP-OP-ADMIT-109
(defn- profile-test-namespaces
  "The test namespaces the tree's own profile assigns to one source file.

  A key may name the source path or the source namespace, and a value may be
  one namespace or many, because a profile is written by a person and both
  spellings are the obvious one."
  [mapping file]
  (when (map? mapping)
    (let [declared (source-namespace file)
          raw (some (fn [key] (when (contains? mapping key) (get mapping key)))
                    [file (keyword file)
                     declared (keyword declared) (symbol declared)])]
      (cond
        (nil? raw) nil
        (or (string? raw) (symbol? raw)) [(str raw)]
        (sequential? raw) (vec (distinct (map str raw)))
        :else nil))))

;; @spec MCP-OP-ADMIT-109
;; @spec MCP-OP-ADMIT-110
;; @spec MCP-OP-ADMIT-111
;; @spec MCP-OP-ADMIT-112
(defn focused-namespace-plan
  "Which test namespaces cover each touched source, and which cannot be found.

  Three sources answer for a file, in this order, and the receipt says which
  one spoke: the tree's own `:namespaces` mapping; the file being a suite
  itself, which covers itself; and the `<ns>-test` path convention.

  The distinction that carries the refusal is ASSERTION versus DISCOVERY. A
  mapping entry and a touched suite are assertions -- the tree said this suite
  exists and covers this source -- so a namespace that resolves to no file is
  a broken profile, and a broken profile must be a typed refusal rather than a
  runner that exits one with nothing to show. The path convention is
  discovery: a source with no sibling suite has no focused coverage, which is
  the pre-existing `no-mapped-test-namespace` case and not an error."
  [project-root profile files present]
  (let [mapping (:namespaces profile)
        exists? (fn [path]
                  (or (contains? present path)
                      (.isFile (io/file (str project-root) path))))
        resolve-asserted
        (fn [file namespace]
          (if (some exists? (namespace-source-paths namespace))
            {:namespace namespace}
            {:missing {:file file
                       :namespace namespace
                       :paths_tried (namespace-source-paths namespace)}}))]
    (reduce
      (fn [plan file]
        (let [declared (source-namespace file)
              asserted (or (profile-test-namespaces mapping file)
                           ;; @spec MCP-OP-ADMIT-112
                           (when (str/ends-with? (str declared) "-test")
                             [declared]))]
          (if asserted
            (let [resolved (mapv #(resolve-asserted file %) asserted)
                  found (vec (keep :namespace resolved))
                  missing (vec (keep :missing resolved))]
              (cond-> plan
                (seq found) (update :per-file assoc file found)
                (seq missing) (update :missing into missing)))
            (if (exists? (test-namespace-file file))
              (update plan :per-file assoc file [(str declared "-test")])
              plan))))
      {:per-file {} :missing []}
      files)))

(def ^:private runner-tail-lines
  "How much of a failed runner's own words the receipt carries."
  40)

(def ^:private runner-tail-chars 4000)

;; @spec MCP-OP-ADMIT-107
(defn- runner-output-tail
  "The last lines the focused runner printed, merged stderr and stdout.

  A receipt that says only `exit 1` hands the reader an exit code and a dead
  end. Four rung-L commits and a replay were diagnosed months later by reading
  the runner's argv off a shell script, because the one place the answer was
  cheap -- the payload -- discarded it."
  [output]
  (when (string? output)
    (let [tail (str/join "\n" (take-last runner-tail-lines
                                         (str/split-lines output)))]
      (if (< runner-tail-chars (count tail))
        (subs tail (- (count tail) runner-tail-chars))
        tail))))

;; ---------------------------------------------------------------------------
;; Inline verification -- the caller's own commands, run in the gate's snapshot
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-153
(def inline-tail-lines
  "How many of a PASSING inline command's last lines the receipt carries."
  20)

;; @spec MCP-OP-ADMIT-153
;; @spec MCP-OP-ADMIT-156
(def inline-failure-tail-lines
  "How many of a FAILING inline command's last lines the receipt carries.

  A failure is the only output a reader has to act on, so it gets the wider
  window. A pass gets enough to prove which command ran."
  40)

(def ^:private inline-tail-chars 6000)

;; @spec MCP-OP-ADMIT-153
(def inline-default-timeout-ms 300000)

;; @spec MCP-OP-ADMIT-153
(def inline-max-commands 8)

;; @spec MCP-OP-ADMIT-153
(def inline-overlay-max-files
  "The overlay ceiling, in files. A tree larger than this is refused by name
  rather than copied: an unbounded copy is a wall cost the caller cannot see."
  20000)

;; @spec MCP-OP-ADMIT-153
(def inline-overlay-max-bytes 268435456)

;; @spec MCP-OP-ADMIT-153
(def inline-overlay-skipped-directories
  "Directory names the overlay never copies.

  `.git` is the whole of it, and deliberately so: excluding `target`,
  `node_modules` or a build cache would break the very commands a caller
  sends. A command whose verification needs git history is out of scope and
  the receipt says the overlay is not a clone."
  #{".git"})

;; @spec MCP-OP-ADMIT-153
;; @spec MCP-OP-ADMIT-156
(defn inline-output-tail
  "The last `lines` lines of one command's merged output, verbatim."
  [output lines]
  (when (string? output)
    (let [tail (str/join "\n" (take-last lines (str/split-lines output)))]
      (if (< inline-tail-chars (count tail))
        (subs tail (- (count tail) inline-tail-chars))
        tail))))

;; @spec MCP-OP-ADMIT-153
(defn inline-command-argv
  "One caller command as argv.

  A string is a shell line, because that is the spelling a receipt's verify
  rows and a project's README both use (`TMPDIR=... make runtests-unit`), and
  refusing it would send the caller back to hand-splitting argv. A vector is
  exec'd as given, with no shell between the caller and the process."
  [command]
  (cond
    (string? command) ["sh" "-c" command]
    (and (sequential? command)
         (seq command)
         (every? string? command)) (vec command)
    :else nil))

;; @spec MCP-OP-ADMIT-153
(defn- receipt-rows->commands
  "The commands carried by a `feature_thread` receipt's verify rows.

  A row is a string, or a map naming its command under `command`/`:command`
  (`argv` is accepted too, because that is the other spelling the receipts
  use). Anything else is passed through untouched so the shape validator --
  not this reader -- publishes the refusal."
  [rows]
  (when (sequential? rows)
    (mapv (fn [row]
            (if (map? row)
              (or (:command row) (get row "command")
                  (:argv row) (get row "argv")
                  row)
              row))
          rows)))

;; @spec MCP-OP-ADMIT-153
(defn normalize-verify
  "One `verify` argument as a mode word plus, for `inline`, its command plan.

  Returns `{:mode \"focused\"|\"none\"|\"inline\" ...}` or `{:error <sentence>}`.
  The caller's spelling never reaches a receipt from here; the error sentence
  quotes it through `bounded-caller-spelling` at the call site."
  [verify]
  (cond
    (nil? verify) {:mode "focused"}
    (string? verify) (if (contains? #{"focused" "none"} verify)
                       {:mode verify}
                       {:error :not-a-vocabulary})
    (map? verify)
    (let [raw (or (:commands verify) (get verify "commands")
                  (receipt-rows->commands
                    (or (:profile_from_receipt verify)
                        (get verify "profile_from_receipt")
                        (:profile-from-receipt verify))))
          cwd (or (:cwd verify) (get verify "cwd"))
          timeout (or (:timeout_ms verify) (get verify "timeout_ms"))
          argvs (when (sequential? raw) (mapv inline-command-argv raw))]
      (cond
        (not (sequential? raw))
        {:error (str "verify object must carry a non-empty \"commands\" array"
                     " (or \"profile_from_receipt\" rows that name commands)")}

        (empty? raw)
        {:error "verify \"commands\" must name at least one command"}

        (< inline-max-commands (count raw))
        {:error (str "verify \"commands\" may name at most "
                     inline-max-commands " commands; this call sent "
                     (count raw))}

        (some nil? argvs)
        {:error (str "every verify command must be a shell line or a"
                     " non-empty array of strings; command "
                     (inc (long (count (take-while some? argvs))))
                     " is neither")}

        (and (some? cwd)
             (or (not (string? cwd))
                 (str/starts-with? cwd "/")
                 (str/includes? cwd "..")))
        {:error (str "verify \"cwd\" must be a relative path inside the"
                     " snapshot, with no parent segments")}

        (and (some? timeout) (not (number? timeout)))
        {:error "verify \"timeout_ms\" must be a number"}

        :else
        {:mode "inline"
         :commands (vec raw)
         :argvs argvs
         :cwd cwd
         :timeout-ms (long (or timeout inline-default-timeout-ms))}))

    :else {:error :not-a-vocabulary}))

;; @spec MCP-OP-ADMIT-153
(defn- overlay-source-files
  "Every file the overlay would copy, with a running byte count.

  Stops the moment either ceiling is passed, so an oversized tree costs a walk
  rather than a copy."
  [^java.io.File root]
  (loop [pending [root] files [] bytes 0]
    (cond
      (or (< inline-overlay-max-files (count files))
          (< inline-overlay-max-bytes bytes))
      {:over-ceiling true :files (count files) :bytes bytes}

      (empty? pending) {:files files :bytes bytes}

      :else
      (let [^java.io.File current (peek pending)
            rest-pending (pop pending)]
        (cond
          (not (.exists current)) (recur rest-pending files bytes)

          (.isDirectory current)
          (if (and (not= current root)
                   (contains? inline-overlay-skipped-directories
                              (.getName current)))
            (recur rest-pending files bytes)
            (recur (into rest-pending (or (seq (.listFiles current)) []))
                   files bytes))

          :else (recur rest-pending (conj files current)
                       (+ bytes (.length current))))))))

;; @spec MCP-OP-ADMIT-153
(defn overlay-snapshot!
  "The workspace as this patch would leave it, in a directory of its own.

  The profile path can run in the workspace root because its command is told
  where the snapshot is and puts it first on a classpath. An inline command is
  the caller's own sentence -- `make test` -- and has no such seam, so the only
  honest venue for it is a tree that already contains the post-images. A green
  from a command run in the unpatched workspace would be a green about bytes
  the gate is not about to write, which is the failure mode this whole gate
  exists to prevent.

  Returns `{:ok true :root <File>}` or a typed refusal map."
  [project-root images]
  (let [source-root (io/file (str project-root))
        walk (overlay-source-files source-root)]
    (if (:over-ceiling walk)
      {:ok false
       :error-type :inline-verify-workspace-too-large
       :error (str "inline verification copies the workspace into a snapshot"
                   " and this tree passes the ceiling of "
                   inline-overlay-max-files " files / "
                   inline-overlay-max-bytes " bytes; run the commands"
                   " yourself, or declare a focused-test profile whose"
                   " command is pointed at {snapshot}")}
      (let [root (temp-tree! "clj-surgeon-admit-overlay")
            source-path (.toPath (.getCanonicalFile source-root))]
        (doseq [^java.io.File file (:files walk)]
          (let [relative (str (.relativize source-path
                                           (.toPath (.getCanonicalFile file))))
                target (io/file root relative)]
            (.mkdirs (.getParentFile target))
            (io/copy file target)))
        (doseq [{:keys [file operation post]} images]
          (let [target (io/file root file)]
            (if (= :delete operation)
              (.delete target)
              (do (.mkdirs (.getParentFile target))
                  (spit target post)))))
        {:ok true :root root :files (count (:files walk))
         :bytes (:bytes walk)}))))

;; @spec MCP-OP-ADMIT-153
;; @spec MCP-OP-ADMIT-156
(defn run-inline-commands!
  "Run the caller's commands in the overlay, in order, and report every one.

  A non-zero exit stops the sequence: the commands are a conjunction, and
  running the rest buys noise rather than evidence. Every command that did not
  run is still NAMED on the receipt, because a row that is missing and a row
  that was skipped read identically to a caller counting rows."
  [{:keys [commands argvs cwd timeout-ms]} overlay-root]
  (let [venue (if cwd (io/file overlay-root cwd) (io/file overlay-root))]
    (loop [index 0 rows [] failed nil]
      (cond
        (or failed (= index (count argvs)))
        (let [remaining (mapv (fn [i]
                                {:command (nth commands i)
                                 :argv (nth argvs i)
                                 :status "not-run-after-failure"})
                              (range index (count argvs)))]
          {:rows (into rows remaining)
           :failed failed
           :cwd (.getPath venue)})

        :else
        (let [argv (nth argvs index)
              {:keys [finished? exit output elapsed_ms]}
              (change-buffer/run-process! (.getPath venue) argv
                                          (long (or timeout-ms
                                                    inline-default-timeout-ms)))
              ok? (and (boolean finished?) (zero? (long (or exit 0))))
              row {:command (nth commands index)
                   :argv argv
                   :exit exit
                   :finished (boolean finished?)
                   :status (cond ok? "passed"
                                 (not finished?) "did-not-finish"
                                 :else "failed")
                   :elapsed_ms elapsed_ms
                   :output_tail (inline-output-tail
                                  output
                                  (if ok?
                                    inline-tail-lines
                                    inline-failure-tail-lines))}]
          (recur (inc index) (conj rows row)
                 (when-not ok?
                   {:index index
                    :command (nth commands index)
                    :argv argv
                    :exit exit
                    :finished (boolean finished?)})))))))

;; @spec MCP-OP-ADMIT-153
(defn inline-test-runner
  "The inline verification, shaped like the focused runner's return value."
  [spec overlay]
  (if-not (:ok overlay)
    ;; @spec MCP-OP-ADMIT-153
    ;; The overlay's own kind travels as the REASON, the way an analyzer's
    ;; failure does: the top-level kind stays `verification-incomplete`,
    ;; because what happened is that a requested check could not run.
    {:ran false
     :reason (or (:error-type overlay) :inline-verify-overlay-unavailable)
     :verify_mode "inline"
     :overlay_error (:error overlay)
     :namespaces []}
    (let [{:keys [rows failed cwd]} (run-inline-commands! spec (:root overlay))]
      (cond-> {:ran true
               :verify_mode "inline"
               :commands rows
               :command_cwd cwd
               :overlay_files (:files overlay)
               :exit (:exit failed)
               :exit-ok (nil? failed)
               :namespaces []}
        failed (merge {:reason (if (:finished failed)
                                 :inline-verify-command-failed
                                 :inline-verify-command-did-not-finish)
                       :failed_command (:command failed)
                       :failed_command_argv (:argv failed)
                       :runner_exit (:exit failed)
                       ;; @spec MCP-OP-ADMIT-156
                       :runner_output_tail (:output_tail
                                             (nth rows (:index failed)))})))))

;; @spec MCP-OP-ADMIT-153
(defn inline-evidence
  "Whether an inline verification may be counted as verification.

  The evidence class is the caller's own exit codes, named as such. It is a
  weaker class than `namespace-report` -- nothing here proves a suite ran --
  and the receipt says so rather than borrowing the stronger word: the caller
  chose the commands, the receipt quotes them, and a reader can see what was
  believed."
  [tests]
  (cond
    (not (:ran tests)) {:ok false :reason (or (:reason tests)
                                              :inline-verify-not-run)}
    (some? (:reason tests)) {:ok false :reason (:reason tests)
                             :exit (:exit tests)}
    :else {:ok true :evidence :inline-command-exit}))

;; @spec MCP-OP-ADMIT-041
;; @spec MCP-OP-ADMIT-068
;; @spec MCP-OP-ADMIT-080
(defn default-test-runner
  "Run the repository-declared focused test command and read its report file.

  The command is repository-declared configuration, not agent input, so the
  threat model here is misconfiguration rather than an adversarial command:
  the question is not whether a hostile runner can lie, but whether an
  ordinary one can be *believed by accident*. It can. A command that prints
  `Ran 7 tests containing 21 assertions` and runs nothing at all is
  indistinguishable from a real suite if stdout is the evidence, and naming
  `{snapshot}` in argv proves only that the word appeared on a command line.

  So the gate names a `{report}` path inside the freshly created snapshot
  directory and requires the runner to write machine-readable results there.
  The file cannot pre-exist, because the directory was made for this call, so
  its presence is proof that this command produced it, and its contents are
  numbers the runner computed rather than a sentence it printed."
  [config {:keys [namespaces snapshot-root]}]
  (let [{:keys [command timeout-ms profile-source] :as profile}
        (resolve-focused-test config)]
    (cond
      ;; @spec MCP-OP-ADMIT-118
      ;; These two are NOT the same state and must never share a reason. One
      ;; is a tree that declares no focused suite; the other is a tree that
      ;; declares one and misconfigures it. The commit waiver exists only for
      ;; the first, and while they shared a name the second inherited it.
      (nil? profile)
      {:ran false :reason :no-focused-test-profile :namespaces (vec namespaces)}

      (not (and (map? profile) (vector? command) (seq command)))
      {:ran false :reason :focused-test-profile-has-no-command
       :profile-source profile-source :namespaces (vec namespaces)}

      (not-any? #{"{snapshot}"} command)
      {:ran false :reason :test-command-not-snapshot-bound
       :profile-source profile-source :namespaces (vec namespaces)}

      (not-any? #{"{report}"} command)
      {:ran false :reason :test-command-not-report-bound
       :profile-source profile-source :namespaces (vec namespaces)}

      (empty? namespaces)
      {:ran false :reason :no-mapped-test-namespace
       :profile-source profile-source :namespaces []}

      (nil? snapshot-root)
      {:ran false :reason :no-snapshot-venue
       :profile-source profile-source :namespaces (vec namespaces)}

      :else
      (let [report (io/file snapshot-root report-file-name)
            _ (.delete report)
            started (System/currentTimeMillis)
            expanded (vec (mapcat (fn [item]
                                    (cond
                                      (= "{namespaces}" item) namespaces
                                      (= "{snapshot}" item) [(str snapshot-root)]
                                      (= "{report}" item) [(.getPath report)]
                                      :else [item]))
                                  command))
            {:keys [finished? exit output]}
            (change-buffer/run-process! (:project-root config) expanded
                                        (or timeout-ms 300000))
            written? (.isFile report)
            failed? (or (not finished?) (not (zero? (long (or exit 0)))))
            rows (when written?
                   (try (parse-test-report (slurp report)) (catch Exception _ nil)))]
        (cond-> {:ran (boolean finished?)
                 :exit exit
                 :exit-ok (and (boolean finished?) (zero? (long (or exit 0))))
                 :profile-source profile-source
                 :report_written written?
                 :report_written_at (when written? (.lastModified report))
                 :report_started_at started
                 ;; @spec MCP-OP-ADMIT-107
                 ;; The three facts a reader needs to diagnose a runner that
                 ;; produced nothing, none of which the field payloads carried:
                 ;; which file was supposed to appear, what was run, and where.
                 :report_file (.getPath report)
                 :command_argv expanded
                 :command_cwd (str (:project-root config))
                 :namespaces (vec namespaces)}
          ;; @spec MCP-OP-ADMIT-107
          (and (not written?) failed?)
          (merge {:reason :verification-runner-failed
                  :runner_exit exit
                  :runner_output_tail (runner-output-tail output)})

          (and (not written?) (not failed?))
          (merge {:reason :report-file-absent
                  :runner_exit exit
                  :runner_output_tail (runner-output-tail output)})

          (and written? (nil? rows)) (assoc :reason :unreadable-test-report)
          rows (merge
                 {:namespace-results rows
                  :tests-run (reduce + 0 (map :tests (vals rows)))
                  :failed (reduce + 0 (map #(+ (:failures %) (:errors %))
                                           (vals rows)))
                  :passed (- (reduce + 0 (map :tests (vals rows)))
                             (reduce + 0 (map #(+ (:failures %) (:errors %))
                                              (vals rows))))
                  :skipped 0})
          ;; @spec MCP-OP-ADMIT-156
          ;; A suite that RAN and failed used to publish its counts and throw
          ;; its own words away: the tail was attached only where the report
          ;; was missing. The failing assertion's line is the fact the caller
          ;; needs, and it is already in hand here.
          (and rows (pos? (long (reduce + 0 (map #(+ (:failures %) (:errors %))
                                                 (vals rows))))))
          (merge {:runner_exit exit
                  :runner_output_tail (runner-output-tail output)}))))))

;; @spec MCP-OP-ADMIT-044
;; @spec MCP-OP-ADMIT-080
;; @spec MCP-OP-ADMIT-089
(defn test-evidence
  "Decide whether a test run may be counted as verification.

  Only a report the runner wrote counts. A parsed stdout summary is not
  evidence, because it is text the command chose to print and it names
  whatever it likes; the gate's earlier namespace check compared the runner's
  answer against the runner's own input, which any command passes by standing
  still."
  [tests namespaces]
  (let [wanted (set namespaces)
        rows (:namespace-results tests)]
    (cond
      (not (:ran tests))
      {:ok false :reason (or (:reason tests) :tests-not-run)}

      (empty? wanted)
      {:ok false :reason :no-mapped-test-namespace}

      (not (map? rows))
      {:ok false :reason (or (:reason tests) :no-test-evidence)}

      (not= wanted (set (map #(if (keyword? %) (name %) (str %)) (keys rows))))
      {:ok false :reason :report-namespaces-do-not-match}

      (not (every? #(pos? (long (or (:tests %) 0))) (vals rows)))
      {:ok false :reason :no-test-evidence}

      (pos? (long (or (:failed tests) 0)))
      {:ok false :reason :tests-failed}

      ;; A report that says everything passed, from a command that exited
      ;; three, describes a run that did not finish the way it meant to. The
      ;; report is not evidence of a clean suite; it is evidence of whatever
      ;; happened before the runner gave up.
      (false? (:exit-ok tests))
      {:ok false :reason :runner-exit-nonzero :exit (:exit tests)}

      :else {:ok true :evidence :namespace-report})))

;; @spec MCP-OP-ADMIT-107
(def unverifiable-test-reasons
  "Test reasons that describe a check which could not run, not one that ran.

  `partial` says one of the two requested checks produced a usable result. A
  runner that was launched, exited non-zero and wrote nothing produced no
  result at all, and calling that half a verification is how a clean analyzer
  delta carried a commit past a suite that never ran."
  #{:verification-runner-failed :report-file-absent
    ;; @spec MCP-OP-ADMIT-111
    :focused-namespace-missing
    ;; @spec MCP-OP-ADMIT-153
    ;; The overlay could not be built, so no command ever ran. That is a
    ;; check that did not happen, never half a verification.
    :inline-verify-overlay-unavailable
    :inline-verify-workspace-too-large
    :inline-verify-not-run
    ;; @spec MCP-OP-ADMIT-118
    :focused-test-profile-has-no-command})

;; @spec MCP-OP-ADMIT-124
(def unverifiable-lint-error-types
  "Analyzer failures that describe a check which could not run.

  MCP-OP-ADMIT-107 already says a focused runner that produced no result is
  not half a verification. The analyzer had no such rule, so a dead analyzer
  beside a live suite published `partial` -- one of the two requested checks
  produced a usable result -- when the check that carries this gate's
  substance had produced nothing at all. `no-clojure-files` is not here: a
  patch with nothing to analyze is a legitimate empty reading, not a failure."
  #{:clj-kondo-unavailable
    :clj-kondo-executable-unavailable
    :clj-kondo-admission-unavailable
    :clj-kondo-admission-timeout
    :clj-kondo-pressure-deferred
    ;; @spec MCP-OP-ADMIT-121
    :analyzer-output-truncated
    :process-interrupted})

;; @spec MCP-OP-ADMIT-125
(defn analyzer-clean-reading?
  "Did the analyzer produce a reading with nothing blocking in it?

  The one predicate for the analyzer half. `verification-status` decided the
  word, `detectors-not-run` decided the list and the commit waiver decided
  permission, each from its own arithmetic over the same map; they are folded
  here so they cannot answer differently."
  [lint]
  (and (true? (:ran lint)) (not (false? (:ok lint)))))

;; @spec MCP-OP-ADMIT-123
;; @spec MCP-OP-ADMIT-125
(defn detector-verdicts
  "Normalize both detectors to one shape: did it answer, was the answer clean.

  `verification_status` and `detectors_not_run` are two readings of the same
  fact and were computed from two different predicates -- the status from the
  evidence verdict, the detector list from whether a child process exited. A
  runner that exited and wrote an unusable report satisfies the second and
  not the first, so a receipt could publish `partial` beside
  `detectors_not_run []`: the affirmative claim that every requested detector
  answered, on a receipt where the substantive half had answered nothing.
  `:ran` is a fact about the operating system. `reading?` is the fact the
  receipt is about.

  A check that ran and came back bad news -- a suite that failed, an analyzer
  that introduced a blocking finding -- produced exactly the reading this
  gate asked for. It is already blocking on its own terms and must not also
  be reported silent."
  [lint evidence]
  [{:detector (or (:detector lint) "clj-kondo")
    :reading? (true? (:ran lint))
    :clean? (analyzer-clean-reading? lint)
    :reason (or (:error-type lint) (:reason lint) :analyzer-unverified)}
   ;; @spec MCP-OP-ADMIT-153
   ;; An inline run's detector is named for what it actually is: the caller's
   ;; own commands and their exit codes. Publishing it as `focused-tests`
   ;; would let a reader believe a suite was attributed when none was.
   (let [inline? (= "inline" (:verify_mode evidence))
         failed-kinds #{:tests-failed :inline-verify-command-failed
                        :inline-verify-command-did-not-finish}]
     {:detector (if inline? "inline-commands" "focused-tests")
      :reading? (or (true? (:ok evidence))
                    (contains? failed-kinds (:reason evidence)))
      :clean? (true? (:ok evidence))
      :reason (or (:reason evidence) :no-test-evidence)})])

;; @spec MCP-OP-ADMIT-123
;; @spec MCP-OP-ADMIT-125
(defn detectors-not-run
  "Name every requested detector that produced no reading at all.

  `verification_status` says that something did not run. It does not say
  what, and a reader scoring `ok`, `hazards` and one summary line can finish
  without ever learning that the substantive half was silent. That is the
  shape the field replay had: `ok` true, `hazards` all class note, an empty
  blocking set, and an analyzer that never executed."
  [verify verdicts]
  ;; @spec MCP-OP-ADMIT-153
  (if-not (contains? #{"focused" "inline"} verify)
    (mapv (fn [{:keys [detector]}]
            {:detector detector :reason :verification-not-requested})
          verdicts)
    (into []
          (comp (remove :reading?)
                (map #(select-keys % [:detector :reason])))
          verdicts)))

;; @spec MCP-OP-ADMIT-082
(defn verification-status
  "Fold the two checks into one word a caller can act on.

  `verification_complete` answers only `did everything pass?`, and answering
  `false` cannot distinguish a clean run with no test profile from a run where
  nothing at all could be checked. Those deserve different reactions, so the
  receipt says which of the requested checks produced a usable result."
  [verify lint evidence]
  ;; @spec MCP-OP-ADMIT-153
  (if-not (contains? #{"focused" "inline"} verify)
    {:status :unverified :reasons [:verification-not-requested]}
    (let [;; @spec MCP-OP-ADMIT-125
          lint-ok (analyzer-clean-reading? lint)
          tests-ok (true? (:ok evidence))
          ;; @spec MCP-OP-ADMIT-124
          ;; @spec MCP-OP-ADMIT-127
          ;; The set names the types this gate knows. An admission failure is
          ;; by construction a check that could not run, whatever the wrapper
          ;; called it, so a type the set has not heard of still reads as
          ;; unverified rather than falling through to partial.
          unverifiable (or (contains? unverifiable-test-reasons (:reason evidence))
                           (contains? unverifiable-lint-error-types
                                      (:error-type lint))
                           (true? (:admission_failure lint)))
          reasons (cond-> []
                    (not lint-ok) (conj (or (:error-type lint)
                                            (:reason lint)
                                            :analyzer-unverified))
                    (not tests-ok) (conj (or (:reason evidence)
                                             :no-test-evidence)))]
      {:status (cond
                 (and lint-ok tests-ok) :complete
                 ;; @spec MCP-OP-ADMIT-107
                 unverifiable :unverified
                 (or lint-ok tests-ok) :partial
                 :else :unverified)
       :reasons reasons})))

;; @spec MCP-OP-ADMIT-042
;; @spec MCP-OP-ADMIT-043
;; @spec MCP-OP-ADMIT-068
;; @spec MCP-OP-ADMIT-082
(defn- verify-snapshot!
  "Run every requested check against the snapshot, before anything is written."
  [config images verify verify-spec]
  (let [clojure-images (filterv #(and (= "clojure" (file-kind (:file %)))
                                      (not= :delete (:operation %)))
                                images)
        lint-runner (or (:admit-lint-runner config) default-lint-runner)
        test-runner (or (:admit-test-runner config) default-test-runner)
        ;; @spec MCP-OP-ADMIT-109
        ;; @spec MCP-OP-ADMIT-110
        profile (resolve-focused-test config)
        touched (mapv :file clojure-images)
        present (set (map :file (remove #(= :delete (:operation %)) images)))
        plan (focused-namespace-plan (:project-root config) profile
                                     touched present)
        per-file (:per-file plan)
        namespaces (vec (distinct (mapcat #(get per-file %) touched)))
        ;; @spec MCP-OP-ADMIT-113
        ;; Which source spoke for the command, which spoke for the coverage,
        ;; and what that coverage actually resolved to per touched file.
        profile-provenance {:profile_source (or (:profile-source profile) :none)
                            :profile_source_namespaces
                            (or (:namespaces-source profile) :path-convention)
                            :focused_namespaces per-file
                            ;; @spec MCP-OP-ADMIT-119
                            ;; The commit waiver's precondition, observed
                            ;; directly at the only place that can see it.
                            :profile_absent (nil? profile)}
        lint (lint-runner config clojure-images)
        inline? (= "inline" verify)
        ;; @spec MCP-OP-ADMIT-153
        ;; An inline command is the caller's own sentence and has no seam to
        ;; be told where a partial snapshot is, so its venue is a full overlay
        ;; of the workspace with the post-images written over it.
        overlay (when inline?
                  (try
                    (overlay-snapshot! (:project-root config) images)
                    (catch Exception error
                      {:ok false
                       :error-type :inline-verify-overlay-unavailable
                       :error (or (.getMessage error)
                                  (.getName (class error)))})))
        snapshot (temp-tree! "clj-surgeon-admit-snapshot")]
    (try
      (materialize! snapshot
                    (remove #(= :delete (:operation %)) images)
                    :post)
      (let [tests (if inline?
                    (inline-test-runner verify-spec overlay)
                    (if (seq (:missing plan))
                    ;; @spec MCP-OP-ADMIT-111
                    ;; The tree named a suite that is not in the tree. Running
                    ;; the runner here buys an opaque exit code; refusing here
                    ;; names the file, the namespace and the paths tried.
                    {:ran false
                     :reason :focused-namespace-missing
                     :missing_focused_namespaces (:missing plan)
                     :namespaces namespaces}
                    (test-runner config {:namespaces namespaces
                                         :snapshot-root (.getPath snapshot)})))
            tests (merge {:ran false :passed 0 :failed 0 :skipped 0
                          :tests-run 0 :namespaces namespaces}
                         (if inline? {} profile-provenance)
                         tests
                         (when inline? {:profile_absent (nil? profile)}))
            evidence (if inline?
                       ;; @spec MCP-OP-ADMIT-153
                       (assoc (inline-evidence tests) :verify_mode "inline")
                       (test-evidence tests namespaces))
            ;; @spec MCP-OP-ADMIT-125
            verdicts (detector-verdicts lint evidence)
            {:keys [status reasons]} (verification-status verify lint evidence)
            lint-blocking (and (:ran lint) (false? (:ok lint)))
            tests-blocking (if inline?
                             ;; @spec MCP-OP-ADMIT-153
                             (and (:ran tests) (false? (:exit-ok tests)))
                             (and (:ran tests)
                                  (pos? (long (or (:failed tests) 0)))))]
        {:lint_delta lint
         ;; @spec MCP-OP-ADMIT-123
         :detectors_not_run (detectors-not-run verify verdicts)
         :tests (cond-> tests
                  (not (:ok evidence)) (assoc :reason (:reason evidence))
                  (some? (:exit evidence)) (assoc :runner_exit (:exit evidence))
                  (:ok evidence) (assoc :evidence (:evidence evidence)))
         :verification_status status
         :verification_reasons (vec reasons)
         :verification_complete (= :complete status)
         ::blocking (cond
                      lint-blocking :blocking-lint-findings
                      tests-blocking (if inline?
                                       :inline-verify-command-failed
                                       :focused-tests-failed)
                      :else nil)})
      (finally
        (delete-tree! snapshot)
        ;; @spec MCP-OP-ADMIT-153
        (when (:root overlay) (delete-tree! (:root overlay)))))))

;; ---------------------------------------------------------------------------
;; Execution
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-098
(defn relativize-under-root
  "Rewrite an absolute path that lies inside the workspace as a relative one.

  Ten field payloads wrote absolute headers -- the agent had the workspace
  path in hand and used it -- and were refused as invalid relative paths. The
  path they named was the right file, inside the root, and unambiguous.

  This is normalisation, not confinement. Anything that does not lie under the
  resolved root is handed on unchanged and refused by the same guard as
  before; all this does is stop the gate rejecting an exact synonym for a path
  it would have accepted."
  [root file]
  (let [root-path (str root)
        prefix (if (str/ends-with? root-path "/") root-path (str root-path "/"))]
    (if (and (str/starts-with? (str file) "/")
             (str/starts-with? (str file) prefix))
      (subs (str file) (count prefix))
      file)))

;; @spec MCP-OP-ADMIT-096
(defn- namespace-of
  "The namespace a source declares, or nil."
  [source]
  (try
    (->> (:units (form-identity/decompose source))
         (filter #(= :ns (:form-kind %)))
         first
         :name)
    (catch Exception _ nil)))

;; @spec MCP-OP-ADMIT-096
(defn deletion-hazards
  "Refuse to delete a namespace the rest of the workspace still requires.

  Deleting a file is the one edit whose damage is entirely outside the file.
  Nothing in the deleted image can show it, so the gate reads the workspace's
  own requires -- the same structural read it uses everywhere else -- and names
  the callers that would stop loading."
  [root deleted-images]
  (let [deleted (into {} (keep (fn [{:keys [file pre]}]
                                 (when-let [name (namespace-of pre)]
                                   [name file])))
                      deleted-images)]
    (when (seq deleted)
      (let [gone (set (map :file deleted-images))
            sources (workspace-sources/read-all root)
            relative (workspace-sources/relative-paths root sources)
            dependents
            (reduce
              (fn [acc [path source]]
                (let [project-path (get relative path)]
                  (if (contains? gone project-path)
                    acc
                    (let [required (try
                                     (->> (:units (form-identity/decompose source))
                                          (filter #(= :ns (:form-kind %)))
                                          first
                                          :node
                                          form-identity/ns-requires
                                          keys
                                          set)
                                     (catch Exception _ #{}))]
                      (reduce (fn [acc [name _]]
                                (cond-> acc
                                  (contains? required name)
                                  (update name (fnil conj []) project-path)))
                              acc
                              deleted)))))
              {}
              sources)]
        (vec (keep (fn [[name file]]
                     (when-let [callers (seq (sort (get dependents name)))]
                       {:type :namespace-form-removed
                        :file file
                        :owner name
                        :span [1 1]
                        :class :refusal
                        :scope :workspace
                        :message (str "Deleting " file " removes namespace "
                                      name ", which is still required by "
                                      (str/join ", " callers))
                        :dependents (vec callers)}))
                   deleted))))))

;; @spec MCP-OP-ADMIT-095
(defn- freeze-sources
  "Read every admitted target once. Returns the frozen snapshot or a refusal.

  A creation has no bytes to read, so its pre-image is defined to be empty and
  its target is resolved as an absent path; that confinement guard is also the
  creation's staleness fence, because a file that appeared since the preview
  fails it. A move resolves both ends."
  [root parsed-files]
  (reduce
    (fn [acc {:keys [file operation move-to]}]
      (let [operation (or operation :update)
            resolved (if (= :add operation)
                       (mcp-paths/resolve-new-source-path root file)
                       (mcp-paths/resolve-source-path root file))
            destination (when (= :move operation)
                          (mcp-paths/resolve-new-source-path root move-to))]
        (cond
          (not (:ok resolved))
          (reduced {:error-type (keyword (:error_type resolved))
                    :error (:error resolved)
                    :file file})

          (and destination (not (:ok destination)))
          (reduced {:error-type (keyword (:error_type destination))
                    :error (:error destination)
                    :file move-to})

          :else
          (assoc acc file
                 (cond-> {:absolute (:path resolved)
                          :operation operation
                          :source (if (= :add operation)
                                    ""
                                    (slurp (:path resolved)))
                          :missing-parent-directories
                          (vec (map str (:missing-parent-directories resolved)))}
                   destination
                   (assoc :destination (:path destination)
                          :destination-missing-parents
                          (vec (map str (:missing-parent-directories
                                          destination)))))))))
    {}
    parsed-files))

;; @spec MCP-OP-ADMIT-052
;; @spec MCP-OP-ADMIT-053
;; @spec MCP-OP-ADMIT-087
;; @spec MCP-OP-ADMIT-095
;; @spec MCP-OP-ADMIT-096
;; @spec MCP-OP-ADMIT-097
(defn- compiled-transaction
  "Project the admitted images onto the kernel's transaction shape.

  The kernel already knows how to create, replace and delete under one
  rollback; the gate's job is to say which is which. A move is a creation of
  the destination and a deletion of the source in the same transaction, so a
  half-finished rename cannot survive a failure."
  [snapshot images]
  (let [by-operation (group-by #(or (:operation %) :update) images)
        updates (:update by-operation)
        moves (:move by-operation)
        creations (:add by-operation)
        deletions (:delete by-operation)
        absolute (fn [file] (get-in snapshot [file :absolute]))
        plan (fn [{:keys [file pre post]}]
               {:file (absolute file)
                :source-hash (structural-lens/source-hash pre)
                :result-hash (structural-lens/source-hash post)
                :match-count 1
                :edits []})
        created (fn [target content parents]
                  {:file target
                   :content content
                   :result-hash (structural-lens/source-hash content)
                   :workspace-root nil
                   :directories parents})]
    {:ok true
     :files (mapv plan updates)
     :original-sources (into {} (map (fn [{:keys [file pre]}] [(absolute file) pre]))
                             updates)
     :future-sources (into {} (map (fn [{:keys [file post]}] [(absolute file) post]))
                           updates)
     :created-files (vec (concat
                           (map (fn [{:keys [file post]}]
                                  (created (absolute file) post
                                           (get-in snapshot [file :missing-parent-directories])))
                                creations)
                           (map (fn [{:keys [file post]}]
                                  (created (get-in snapshot [file :destination]) post
                                           (get-in snapshot [file :destination-missing-parents])))
                                moves)))
     :created-directories (vec (distinct
                                 (concat
                                   (mapcat #(get-in snapshot [(:file %) :missing-parent-directories])
                                           creations)
                                   (mapcat #(get-in snapshot [(:file %) :destination-missing-parents])
                                           moves))))
     :deleted-files (vec (concat
                           (map (fn [{:keys [file pre]}]
                                  {:file (absolute file)
                                   :result-hash (structural-lens/source-hash pre)})
                                deletions)
                           (map (fn [{:keys [file pre]}]
                                  {:file (absolute file)
                                   :result-hash (structural-lens/source-hash pre)})
                                moves)))}))

(defn- passthrough-file
  [file]
  {:file file :kind "passthrough" :hunks 0
   :hunk_line_spans {:pre [] :post []}
   :owners {:added [] :removed [] :changed []}
   :protected_node_drift {}
   :byte_drift_outside_hunks 0
   :pre_sha256 nil :post_sha256 nil})

(defn- aggregate
  [images deltas]
  (let [by-file (into {} (map (juxt :file identity)) deltas)]
    {:files (mapv (fn [{:keys [file hunk-count hunk-spans pre post operation
                              move-to]}]
                    (let [delta (get by-file file)]
                      (cond-> {:file file
                       :kind (file-kind file)
                       :operation (or operation :update)
                       :hunks hunk-count
                       :hunk_line_spans hunk-spans
                       :owners (or (:owners delta)
                                   {:added [] :removed [] :changed []})
                       :protected_node_drift (or (:protected-node-drift delta) {})
                       :byte_drift_outside_hunks
                       (or (:byte-drift-outside-hunks delta) 0)
                       :pre_sha256 (structural-lens/source-hash pre)
                       :post_sha256 (structural-lens/source-hash post)}
                        move-to (assoc :move_to move-to))))
                  images)
     :owners {:added (vec (mapcat (fn [{:keys [file owners]}]
                                    (map #(str file "::" %) (:added owners)))
                                  deltas))
              :removed (vec (mapcat (fn [{:keys [file owners]}]
                                      (map #(str file "::" %) (:removed owners)))
                                    deltas))
              :changed (vec (mapcat (fn [{:keys [file owners]}]
                                      (map #(str file "::" %) (:changed owners)))
                                    deltas))}
     :protected_node_drift (into {} (mapcat (fn [{:keys [file protected-node-drift]}]
                                              (map (fn [[owner classes]]
                                                     [(str file "::" owner) classes])
                                                   protected-node-drift))
                                            deltas))
     :byte_drift_outside_hunks (reduce + 0 (map :byte-drift-outside-hunks deltas))
     :hazards (vec (mapcat :hazards deltas))
     :hashes (into {} (map (fn [{:keys [file pre post]}]
                             [file {:pre (structural-lens/source-hash pre)
                                    :post (structural-lens/source-hash post)}]))
                   images)}))

;; @spec MCP-OP-ADMIT-063
(defn- declared-path
  "Recover a project-relative path from a JSON object key.

  A JSON round-trip keywordizes these keys, and a path contains slashes, so
  `name` would silently return `app/core.clj` for `src/app/core.clj` and make
  every binding look mismatched. The printed keyword is the whole path."
  [key]
  (if (keyword? key) (subs (str key) 1) (str key)))

;; @spec MCP-OP-ADMIT-060
;; @spec MCP-OP-ADMIT-084
(defn- call-guarding-writes
  "Hold exclusive write authority for a root, or run unguarded when nil.

  Preview writes nothing, so it takes no lock: a preview that ran clj-kondo
  and a focused suite while holding the workspace's write lock would block
  every commit on that tree for the length of a test run, which buys no safety
  because it has nothing to protect."
  [root thunk]
  (if root
    (workspace-lock/call-with-workspace-write-lock root thunk)
    (thunk)))

(defmacro ^:private guarding-writes
  [root & body]
  `(call-guarding-writes ~root (fn [] ~@body)))

;; @spec MCP-OP-ADMIT-053
;; @spec MCP-OP-ADMIT-062
(defn- stale-snapshot-refusal
  "Re-read every frozen file immediately before the write, under the lock.

  The kernel's own compare-and-swap runs inside its transaction, which is the
  right place for it, but the gate has held this snapshot across hazard
  analysis and a full verification run. Confirming the snapshot here, while
  exclusive write authority is held, is what makes the interval between the
  proof and the write empty."
  [context snapshot]
  (let [drifted (->> snapshot
                     (keep (fn [[file {:keys [absolute source operation]}]]
                             (if (= :add operation)
                               ;; A creation's fence is the absence of its
                               ;; target, not the content of a file that by
                               ;; definition has none.
                               (when (.exists (io/file absolute))
                                 {:file file :expected-hash nil
                                  :actual-hash (structural-lens/source-hash
                                                 (slurp absolute))})
                               (let [current (try (slurp absolute)
                                                  (catch Exception _ nil))]
                                 (when-not (= current source)
                                   {:file file
                                    :expected-hash (structural-lens/source-hash
                                                     source)
                                    :actual-hash (some-> current
                                                         structural-lens/source-hash)})))))
                     vec)]
    (when (seq drifted)
      (select-keys
        (refusal context :source-hash-mismatch
                 (str "The workspace changed while this admission was being "
                      "verified: " (str/join ", " (map :file drifted)))
                 {:drifted drifted})
        [:ok :operation :committed :source-unchanged :error-type :error
         :next_call :drifted]))))

(defn- pre-image-binding-refusal
  "Refuse a commit whose files moved since the preview that authorized it."
  [context expected snapshot]
  (when (seq expected)
    (let [expected (into {} (map (fn [[key value]] [(declared-path key) value]))
                         expected)
          admitted (set (keys snapshot))
          declared (set (keys expected))]
      (cond
        ;; @spec MCP-OP-ADMIT-108
        ;; The old message stated the rule and withheld every fact needed to
        ;; obey it, so a caller could only repair it by guessing which side
        ;; was wrong. Both sets and their difference are named here, because
        ;; the gate already holds them and the caller does not.
        (not= admitted declared)
        (let [touched (vec (sort admitted))
              named (vec (sort declared))
              missing (vec (remove declared touched))
              unexpected (vec (remove admitted named))]
          (refusal context :invalid-admit-request
                   (str "expect_pre_sha256 must name exactly the files the "
                        "patch touches. This patch touches "
                        (str/join ", " touched)
                        "; expect_pre_sha256 named "
                        (if (seq named) (str/join ", " named) "nothing")
                        (when (seq missing)
                          (str "; missing from expect_pre_sha256: "
                               (str/join ", " missing)))
                        (when (seq unexpected)
                          (str "; named but not touched by this patch: "
                               (str/join ", " unexpected))))
                   {:declared named
                    :admitted touched
                    :files_touched touched
                    :files_named named
                    :missing missing
                    :unexpected unexpected}))

        :else
        (let [drifted (->> snapshot
                           (keep (fn [[file {:keys [source]}]]
                                   (let [actual (structural-lens/source-hash source)
                                         wanted (get expected file)]
                                     (when-not (= actual wanted)
                                       {:file file :expected-hash wanted
                                        :actual-hash actual}))))
                           vec)]
          (when (seq drifted)
            (refusal context :source-hash-mismatch
                     (str "The workspace moved since the preview that "
                          "authorized this commit: "
                          (str/join ", " (map :file drifted)))
                     {:drifted drifted})))))))

;; @spec MCP-OP-ADMIT-003
;; @spec MCP-OP-ADMIT-004
;; @spec MCP-OP-ADMIT-012
;; @spec MCP-OP-ADMIT-051
;; @spec MCP-OP-ADMIT-060
;; @spec MCP-OP-ADMIT-061
;; @spec MCP-OP-ADMIT-062
;; @spec MCP-OP-ADMIT-105
;; @spec MCP-OP-ADMIT-106
(defn incomplete-commit-refusal-reason
  "Why this commit may not proceed on the verification it obtained, or nil.

  The gate used to treat `partial` as permission: a clean analyzer delta plus
  a focused runner that produced nothing scored one usable check out of two,
  and one out of two wrote the files. Four rung-L commits and a replay landed
  that way, each carrying `verification_complete: false` on a receipt whose
  `committed` was already `true` -- an honest field about a write that had
  happened, which is not the same thing as a gate.

  `allow_partial` is the escape hatch for the one honest case: a repository
  that ships no focused-test profile at all cannot produce test evidence and
  is not hiding a failure. A profile that exists and did not deliver is
  exactly the case the caller must not be able to wave through, so the waiver
  is denied there -- and it is denied on the OBSERVED absence of a profile,
  never on a runner reason that happens to say `no-focused-test-profile`,
  because a tree that ships a profile declaring no `:command` reported that
  same reason and would have inherited a waiver written for a different state.

  What `allow_partial` may NOT do is authorise a write on a verification that
  never happened. The waiver read `profile_absent` and never the status word,
  so a dead analyzer beside an absent profile -- ZERO detectors, not one of
  two -- waived the block all the same, and MCP-OP-ADMIT-124's whole purpose
  bought nothing at the commit gate. It is denied unless the analyzer
  produced a reading, the status is `partial`, and `verify` is `focused`;
  `verify: \"none\"` is never waivable in commit mode, because that is rung L
  one rung over, on any repository shipping no profile -- including this one.

  The requirement does not depend on `verify`, and that is the whole lesson of
  rung L. Leaving `verify: \"none\"` as an explicit waiver looked principled --
  the caller declined the check, the receipt said `unverified`, nothing was
  hidden. What it actually bought was a third rung on a ladder: cohort z8's
  agents were told `mode commit, verify focused`, met a refusal, tried
  `allow_partial: true`, met a refusal, and then sent `verify: \"none\"` and
  got their write. Three of the six commits on that rung landed that way, and
  every one of the three was the only `verify: \"none\"` call in its run. A
  gate a caller can turn off is a caller's gate. Verification may still be
  declined -- in `preview`, which is where an unverified answer belongs."
  [verification verify allow-partial?]
  (when (not= :complete (:verification_status verification))
    (let [reason (or (get-in verification [:tests :reason])
                     (first (:verification_reasons verification))
                     :verification-incomplete)
          profile-absent? (true? (get-in verification [:tests :profile_absent]))
          ;; @spec MCP-OP-ADMIT-125
          ;; @spec MCP-OP-ADMIT-126
          ;; The same predicate `verification_status` and `detectors_not_run`
          ;; use, so the three cannot disagree about what ran.
          analyzer-read? (analyzer-clean-reading? (:lint_delta verification))]
      ;; @spec MCP-OP-ADMIT-153
      ;; The waiver exists for a tree that CANNOT produce test evidence. An
      ;; inline call is a caller that supplied the check itself, so there is
      ;; always a check that could run and the honest-absence case never
      ;; applies -- a failing inline command must never be waivable.
      (when-not (and allow-partial?
                     (= "focused" verify)
                     (= :partial (:verification_status verification))
                     profile-absent?
                     analyzer-read?)
        reason))))

;; @spec MCP-OP-ADMIT-154
(def inline-verify-affordance
  "The ONE call that supplies a missing profile, spelled as a caller sends it."
  (str "admit_clojure_patch {\"mode\": \"commit\", \"verify\": {\"commands\":"
       " [\"make test\"]}, \"patch\": \"…\"}"))

;; @spec MCP-OP-ADMIT-154
(defn verification-incomplete-error
  "Why this commit stopped, in words the caller can act on.

  A remedy that names an internal state -- `repair no-focused-test-profile` --
  is not a remedy: it names something an agent cannot reach from inside a
  task. When the state IS the absent profile, the sentence leads with that
  fact and hands over the exact call that fixes it in one hop."
  [verification reason]
  (let [status (name (:verification_status verification))
        reasons (str/join ", " (map name (:verification_reasons verification)))]
    (if (= :no-focused-test-profile reason)
      (str "this workspace has no verification profile, so the gate could run"
           " no check of its own and wrote nothing. Supply your own commands"
           " in the same call -- they run inside the gate's snapshot and their"
           " exit codes and last lines come back on this receipt: "
           inline-verify-affordance
           ". Use mode propose first to read the result without a write.")
      (str "Verification did not complete (" status ": " reasons
           "); nothing was written. Run mode preview to see the same receipt"
           " without a write, and repair " (name reason)
           " before committing."))))

;; @spec MCP-OP-ADMIT-156
(defn verification-failure-detail
  "The failing command's own exit code and last lines, for the refusal.

  A refusal that says only `focused-tests-failed` hands a reader a category
  and a dead end. The failing assertion's line is the one fact that makes the
  next edit possible; the gate already holds it, and discarding it is what
  sends the caller back outside the gate to run the suite again."
  [verification]
  (let [tests (:tests verification)
        exit (or (:runner_exit tests) (:exit tests))
        tail (:runner_output_tail tests)
        command (or (:failed_command tests) (:command_argv tests))]
    (cond-> {}
      (some? exit) (assoc :failing_command_exit exit)
      (some? command) (assoc :failing_command command)
      (not (str/blank? (str tail))) (assoc :failing_command_output_tail tail))))

;; @spec MCP-OP-ADMIT-156
(defn- verification-failed-error
  [blocked verification suffix]
  (let [{:keys [failing_command_exit failing_command_output_tail]}
        (verification-failure-detail verification)]
    (str "Snapshot verification failed (" (name blocked) ")" suffix
         (when (some? failing_command_exit)
           (str "; the command exited " failing_command_exit))
         (when failing_command_output_tail
           (str "; its last lines were:\n" failing_command_output_tail)))))

(defn- execute-in-context!
  [config {:keys [patch mode verify expect_pre_sha256 allow_partial]}
   workspace-root]
  (let [requested-mode (or mode "preview")
        requested-verify (or verify "focused")
        ;; @spec MCP-OP-ADMIT-140
        ;; The caller's strings are NORMALISED before either can reach a
        ;; receipt. `context` is what `refusal` merges into
        ;; `(empty-receipt (:mode context))`, so an unvalidated mode here is
        ;; a caller writing an identity key of every refusal below.
        ;; @spec MCP-OP-ADMIT-157
        mode (if (contains? #{"preview" "propose" "commit"} requested-mode)
               requested-mode
               "preview")
        ;; @spec MCP-OP-ADMIT-153
        verify-spec (normalize-verify (when (some? verify) verify))
        verify (or (:mode verify-spec) "focused")
        context {:mode mode :workspace-root workspace-root
                 :patch patch
                 ;; @spec MCP-OP-ADMIT-153
                 ;; A follow-up call must be SENDABLE, so an inline plan
                 ;; travels to `next-call` as the object the caller sent
                 ;; rather than as the word `inline`, which is a receipt
                 ;; vocabulary and not a request one.
                 :verify (if (= "inline" verify) requested-verify verify)}]
    (cond
      ;; @spec MCP-OP-ADMIT-140
      ;; FIRST, before the patch is even looked at: a mode outside the enum is
      ;; a typed refusal naming the field, quoting the value cut to a bounded
      ;; spelling, and carrying the DEFAULT mode in the receipt it publishes.
      (not (contains? #{"preview" "propose" "commit"} requested-mode))
      (refusal context :invalid-admit-request
               (str "mode must be preview, propose or commit; this call sent "
                    (bounded-caller-spelling requested-mode)))

      ;; @spec MCP-OP-ADMIT-153
      (= :not-a-vocabulary (:error verify-spec))
      (refusal context :invalid-admit-request
               (str "verify must be focused, none, or an object naming your"
                    " own commands -- {\"commands\": [\"make test\"]};"
                    " this call sent "
                    (bounded-caller-spelling requested-verify)))

      (:error verify-spec)
      (refusal context :invalid-admit-request (:error verify-spec))

      (not (and (string? patch) (not (str/blank? patch))))
      (refusal context :invalid-patch
               "patch must be non-blank unified diff text")

      (not (or (nil? expect_pre_sha256) (map? expect_pre_sha256)))
      (refusal context :invalid-admit-request
               "expect_pre_sha256 must be an object of file to sha256")

      :else
      (let [parsed (patch-apply/parse-patch patch)]
        (if-not (:ok parsed)
          ;; A refusal on an unparseable payload must say which grammars were
          ;; tried and show the line that stopped it. The field failure this
          ;; replaces was one identical message repeated across every run,
          ;; naming a grammar the caller was never going to write.
          (merge
            (refusal context (:error-type parsed) (:error parsed)
                     (select-keys parsed [:file :hunk-index :grammar
                                          :grammars-tried :expected-headers
                                          :offending-line :header :patch-line]))
            {:next_call (assoc (next-call context "preview"
                                          (:error-type parsed))
                               :expected_headers
                               (or (:expected-headers parsed)
                                   patch-apply/expected-headers))})
          (let [targets (mapv :file (:files parsed))
                repeated (->> targets frequencies
                              (keep (fn [[file n]] (when (< 1 n) file)))
                              sort vec)
                passthrough (filterv #(= "passthrough" (file-kind %)) targets)]
            (cond
              (seq repeated)
              ;; Two sections for one file describe two different post images
              ;; of the same bytes. Refusing here keeps that ambiguity out of
              ;; the transaction, where it can only surface as a failed write.
              (refusal context :duplicate-patch-target
                       (str "patch names the same file in more than one "
                            "section: " (str/join ", " repeated))
                       {:grammar (:grammar parsed)
                        :files repeated :file (first repeated)})

              :else
              (if (seq passthrough)
              ;; A preview that returned ok here would advertise a commit that
              ;; is guaranteed to refuse. Both modes say the same thing.
              (refusal context :unsupported-patch-target
                       (str "The gate admits Clojure and EDN sources only; "
                            "apply these natively: "
                            (str/join ", " passthrough))
                       {:files (mapv passthrough-file passthrough)})
              ;; Exclusive write authority spans the whole admission in
              ;; commit mode: the snapshot, the hazard analysis, the
              ;; verification run, and the write. A compare-and-swap answers
              ;; "did this change?"; only a lock answers "may I write now?",
              ;; and eight concurrent commits proved the difference by losing
              ;; edits their own receipts called committed.
              (guarding-writes
                (when (= "commit" mode) workspace-root)
                (let [root (mcp-paths/real-root (:project-root config))
                      parsed (update parsed :files
                                     (fn [files]
                                       (mapv #(-> %
                                                  (update :file
                                                          (partial
                                                            relativize-under-root
                                                            root))
                                                  (cond->
                                                    (:move-to %)
                                                    (update :move-to
                                                            (partial
                                                              relativize-under-root
                                                              root))))
                                             files)))
                      targets (mapv :file (:files parsed))
                      snapshot (freeze-sources root (:files parsed))]
                (if (:error-type snapshot)
                  (refusal context (:error-type snapshot) (:error snapshot)
                           (select-keys snapshot [:file]))
                  (or
                    (pre-image-binding-refusal context expect_pre_sha256 snapshot)
                    (let [sources (into {} (map (fn [[file {:keys [source]}]]
                                                  [file source]))
                                        snapshot)
                          applied (patch-apply/apply-parsed sources (:files parsed))]
                      (if-not (:ok applied)
                        (refusal context (:error-type applied) (:error applied)
                                 (select-keys applied [:file :hunk-index :line
                                                       :patch-line
                                                       :offending-line]))
                        (let [images (:files applied)
                              deltas (mapv
                                       (fn [{:keys [file pre post hunk-spans
                                                    operation]}]
                                         (if (= "clojure" (file-kind file))
                                           (form-identity/form-identity-delta
                                             {:file file :pre pre :post post
                                              :hunk-spans hunk-spans
                                              :operation operation})
                                           {:file file
                                            :owners {:added [] :removed []
                                                     :changed []}
                                            :protected-node-drift {}
                                            :byte-drift-outside-hunks 0
                                            :hazards []}))
                                       images)
                              deletion (deletion-hazards
                                         root
                                         (filter #(= :delete (:operation %))
                                                 images))
                              report (update (aggregate images deltas)
                                             :hazards into deletion)
                              created (set (map :file
                                                (filter #(= :add (:operation %))
                                                        images)))
                              expect-pre (into {}
                                               (comp
                                                 (remove #(contains? created
                                                                     (key %)))
                                                 (map (fn [[file {:keys [pre]}]]
                                                        [file pre])))
                                               (:hashes report))
                              context (assoc context :expect-pre expect-pre)
                              ;; @spec MCP-OP-ADMIT-155
                              ;; `unbound` was a lie of omission. The gate
                              ;; freezes every touched file itself and
                              ;; re-reads all of them under exclusive write
                              ;; authority immediately before the write, so a
                              ;; commit with no caller digests is already
                              ;; fenced against a tree that moved -- and the
                              ;; word `unbound` read as NO PROTECTION, which
                              ;; is what sent a field agent to spend four to
                              ;; five minutes computing whole-file digests for
                              ;; eight files before its first call. `bound`
                              ;; keeps its stronger meaning: it also covers
                              ;; the interval BEFORE this call, between the
                              ;; preview the caller read and this commit,
                              ;; which the gate cannot observe for itself.
                              base-binding (cond
                                             ;; Nothing existed to bind to.
                                             (every? #(= :add (:operation %))
                                                     images) "created"
                                             (seq expect_pre_sha256) "bound"
                                             :else "derived")
                              no-op? (every? #(= (:pre %) (:post %)) images)
                              blocking (form-identity/refusal-hazards
                                         (:hazards report))
                              ;; @spec MCP-OP-ADMIT-125
                              ;; These two branches refuse before any
                              ;; detector is consulted. Saying so is not the
                              ;; same claim as `[]`.
                              not-attempted
                              (detectors-not-run
                                verify
                                (detector-verdicts
                                  {:ran false
                                   :reason :verification-not-attempted}
                                  {:ok false
                                   :reason :verification-not-attempted}))
                              base (assoc (merge (empty-receipt mode) report)
                                          :pre_image_binding base-binding)]
                          (cond
                            ;; @spec MCP-OP-ADMIT-102
                            ;; A patch that changes nothing is not a small
                            ;; success, it is a request the gate failed to
                            ;; understand. Reporting ok for it is how a
                            ;; truncated hunk came back looking like work.
                            no-op?
                            (merge base
                                   {:ok false
                                    :operation :admit-patch-refused
                                    :committed false
                                    :source-unchanged true
                                    ;; @spec MCP-OP-ADMIT-125
                                    :detectors_not_run not-attempted
                                    :error-type :no-op-patch
                                    :error (str "The patch produces a post-image "
                                                "identical to the pre-image for "
                                                "every file it names; nothing "
                                                "was requested that the reader "
                                                "could act on")
                                    :next_call (next-call context "preview"
                                                          :no-op-patch)})

                            (seq blocking)
                            (merge base
                                   {:ok false
                                    :operation :admit-patch-refused
                                    :committed false
                                    :mutation_attempted false
                                    :source-unchanged true
                                    ;; @spec MCP-OP-ADMIT-125
                                    :detectors_not_run not-attempted
                                    :error-type (:type (first blocking))
                                    :error (:message (first blocking))
                                    ;; @spec MCP-OP-ADMIT-116
                                    :next_call
                                    (assoc (next-call context "preview"
                                                      (:type (first blocking)))
                                           :lifted_by
                                           (hazard-lift (first blocking)))})

                            :else
                            ;; Verification runs against the snapshot, before
                            ;; any write, in both modes. A check that failed
                            ;; blocks the commit; a check that could not run
                            ;; does not block it, but never reads as complete.
                            (let [verification
                                  (if (contains? #{"focused" "inline"} verify)
                                    (verify-snapshot! config images verify verify-spec)
                                    ;; @spec MCP-OP-ADMIT-119
                                    {:verification_status :unverified
                                     :verification_reasons
                                     [:verification-not-requested]
                                     :verification_complete false
                                     ;; @spec MCP-OP-ADMIT-123
                                     :detectors_not_run
                                     (detectors-not-run
                                       verify
                                       (detector-verdicts {:ran false}
                                                          {:ok false}))
                                     :tests {:ran false :passed 0 :failed 0
                                             :skipped 0 :namespaces []
                                             :profile_absent
                                             (nil? (resolve-focused-test
                                                     config))}})
                                  blocked (::blocking verification)
                                  verification (dissoc verification ::blocking)]
                              (cond
                                ;; @spec MCP-OP-ADMIT-157
                                ;; `propose` is the loop: run the same checks
                                ;; in the same snapshot, publish the same
                                ;; receipt, write nothing, and do NOT convert
                                ;; a failing check into a refusal -- the
                                ;; verdict is a field, so the caller can read
                                ;; the failing lines and send the fix without
                                ;; having to distinguish a refused call from a
                                ;; broken one.
                                (= "propose" mode)
                                (merge base verification
                                       (verification-failure-detail
                                         verification)
                                       {:ok true
                                        :operation :admit-patch-proposed
                                        :committed false
                                        :mutation_attempted false
                                        :source-unchanged true
                                        :verify_ok (nil? blocked)
                                        :next_call (next-call context
                                                              "commit" nil)})

                                (and blocked (= "commit" mode))
                                (merge base verification
                                       ;; @spec MCP-OP-ADMIT-156
                                       (verification-failure-detail
                                         verification)
                                       {:ok false
                                        :operation :admit-patch-refused
                                        :committed false
                                        :mutation_attempted false
                                        :source-unchanged true
                                        :verify_ok false
                                        :error-type :verification-failed
                                        :error (verification-failed-error
                                                 blocked verification
                                                 "; nothing was written")
                                        :next_call (next-call context "propose"
                                                              blocked)})

                                ;; @spec MCP-OP-ADMIT-105
                                ;; @spec MCP-OP-ADMIT-106
                                (and (= "commit" mode)
                                     (incomplete-commit-refusal-reason
                                       verification verify
                                       (true? allow_partial)))
                                (let [reason
                                      (incomplete-commit-refusal-reason
                                        verification verify
                                        (true? allow_partial))]
                                  (merge base verification
                                         {:ok false
                                          :operation :admit-patch-refused
                                          :committed false
                                          :mutation_attempted false
                                          :source-unchanged true
                                          :verify_ok false
                                          :error-type :verification-incomplete
                                          ;; @spec MCP-OP-ADMIT-154
                                          :error
                                          (verification-incomplete-error
                                            verification reason)
                                          ;; @spec MCP-OP-ADMIT-120
                                          ;; @spec MCP-OP-ADMIT-154
                                          ;; Propose the verify that can lift
                                          ;; this, not the one that just
                                          ;; failed to. Where the profile is
                                          ;; the missing thing, the only
                                          ;; verify that can lift it is the
                                          ;; caller's own commands.
                                          :next_call
                                          (next-call
                                            (assoc context :verify
                                                   (if (= :no-focused-test-profile
                                                          reason)
                                                     {:commands
                                                      ["<your project's test command, e.g. make test>"]}
                                                     "focused"))
                                            "propose"
                                            :verification-incomplete)}))

                                (= "preview" mode)
                                (merge base verification
                                       {:operation :admit-patch-preview
                                        :verify_ok (nil? blocked)
                                        :next_call (next-call context "commit" nil)}
                                       (when blocked
                                         (merge
                                           ;; @spec MCP-OP-ADMIT-156
                                           (verification-failure-detail
                                             verification)
                                           {:ok false
                                            :operation :admit-patch-refused
                                            :error-type :verification-failed
                                            :error (verification-failed-error
                                                     blocked verification "")
                                            :next_call (next-call
                                                         context "propose"
                                                         blocked)})))

                                :else
                                (do
                                  (when-let [hook (:admit-before-commit! config)]
                                    (hook))
                                  (if-let [stale (stale-snapshot-refusal
                                                   context snapshot)]
                                    (merge base verification stale)
                                    (let [committed (transaction/commit-compiled!
                                                      (compiled-transaction
                                                        snapshot images))]
                                      (if-not (:ok committed)
                                        (merge base verification
                                               {:ok false
                                                :operation :admit-patch-refused
                                                :committed false
                                                :mutation_attempted true
                                                :source-unchanged
                                                (or (= :source-hash-mismatch
                                                       (:error-type committed))
                                                    (true? (:rolled-back committed)))
                                                :error-type (:error-type committed)
                                                :error (:error committed)
                                                :next_call (next-call
                                                             context "preview"
                                                             (:error-type committed))})
                                        (let [{:keys [scope lock-path]}
                                              (workspace-lock/lock-scope
                                                workspace-root)
                                              receipt
                                              (cond-> (merge base verification
                                                             {:operation :admit-patch!
                                                              :committed true
                                                              :mutation_attempted true
                                                              :source-unchanged false
                                                              ;; @spec MCP-OP-ADMIT-157
                                                              :verify_ok (nil? blocked)
                                                              :lock_scope scope
                                                              :next_call nil})
                                                lock-path
                                                (assoc :lock_path lock-path))]
                                          ;; @spec MCP-OP-ADMIT-105
                                          ;; Nothing reaches here on an
                                          ;; incomplete verification any more:
                                          ;; the refusal above stands between
                                          ;; the check and the write, rather
                                          ;; than downgrading a receipt for a
                                          ;; write that already happened.
                                          receipt)))))))))))))))))))))))

;; ---------------------------------------------------------------------------
;; Telemetry and payload bounding
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-054
(defn- telemetry-event
  [params receipt]
  {:tool "admit_clojure_patch"
   :request_shape {:patch_bytes (patch-bytes (:patch params))
                   :patch_sha256 (patch-digest (:patch params))
                   :mode (:mode params)
                   :verify (:verify params)
                   :files (count (:files receipt))}
   :outcome {:ok (boolean (:ok receipt))
             :committed (boolean (:committed receipt))
             :verification_complete (boolean (:verification_complete receipt))
             :hazard_types (vec (distinct (map :type (:hazards receipt))))
             :byte_drift (:byte_drift_outside_hunks receipt)
             :error_type (:error-type receipt)}})

(defn- record-telemetry!
  [config params receipt]
  (when-let [state (:telemetry config)]
    (let [event (telemetry-event params receipt)]
      (if-let [emit (:emit! state)]
        (emit event)
        (telemetry/emit! state :tool.call event))))
  receipt)

;; @spec MCP-OP-ADMIT-136
;; `summary-characters` renders the text face; it lives with the renderer,
;; below, and is declared here because the bound is applied at the one point
;; every receipt passes.
(declare summary-characters)

;; @spec MCP-OP-ADMIT-136
;; @spec MCP-OP-ADMIT-139
(defn- public-faces-fit?
  "Both faces of one candidate receipt inside the ONE budget.

  The structured face is measured as the JSON the caller receives -- envelope
  included, which is the 271 bytes round four charged to nobody -- and the
  text face as the block that spells every one of its leaves. A candidate
  fits only when both do."
  [candidate]
  (and (<= (write-refusal/json-bytes candidate)
           write-refusal/public-byte-budget)
       (<= (summary-characters candidate)
           write-refusal/public-byte-budget)))

;; @spec MCP-OP-ADMIT-143
(defn- binding-face
  "Which of a receipt's two faces was over the one budget.

  `structured`, `text` or `both`. A reader who sees `payload_omitted` asks
  exactly one question -- how much am I not being shown -- and the byte
  figure beside it answers a different one when the text face is what forced
  the trim."
  [receipt]
  (let [json-over? (> (write-refusal/json-bytes receipt)
                      write-refusal/public-byte-budget)
        text-over? (> (summary-characters receipt)
                      write-refusal/public-byte-budget)]
    (cond
      (and json-over? text-over?) "both"
      text-over? "text"
      json-over? "structured"
      :else "neither")))

;; @spec MCP-OP-ADMIT-135
;; @spec MCP-OP-ADMIT-139
(defn- oversize-next-call-refusal
  "A next_call the public budget cannot carry is a typed refusal naming its
  size -- never a pointer, and never a silently truncated call.

  There is no honest degraded rendering of a next_call: it is the one field a
  caller must be able to send back byte for byte, so a shortened one is not a
  smaller version of the affordance, it is the absence of it wearing its name.
  If it will not fit, the receipt says so, states the exact character count
  and the budget it exceeded, and refuses -- so a caller knows the call
  existed, knows why it cannot have it, and knows the number that would
  change the answer.

  Round three's answer, a pointer at structuredContent, is the same failure
  one level down: the reader this text block exists for is the one who cannot
  read structuredContent.

  The decision is made on the RECEIPT that would carry the call, envelope
  included, and after the payload has been trimmed -- not on the call's
  characters alone. Round four compared the call's length to the budget, so a
  next_call of exactly 32,640 characters was published inside a receipt of
  32,911 bytes: 271 bytes of keys, quotes and braces charged to nobody, over
  the very number the refusal text calls a budget (MCP-OP-ADMIT-139). And it
  fires only when the next_call is the REASON the receipt cannot fit: a
  receipt too large for other reasons states its elision and publishes, as it
  always has."
  [receipt]
  (when-let [call (:next_call receipt)]
    (let [characters (count (json/generate-string call))]
      (when (not (public-faces-fit? receipt))
        (let [receipt-bytes (write-refusal/json-bytes receipt)
              sentence (str "this receipt's next_call is " characters
                            " characters and the receipt that would carry it"
                            " is " receipt-bytes " bytes; the public payload"
                            " budget is " write-refusal/public-byte-budget
                            " bytes, envelope included, so this call cannot be"
                            " published verbatim, and a next_call a caller"
                            " cannot send back byte for byte is not a"
                            " next_call")
              measurements {:next_call_characters characters
                            :receipt_bytes receipt-bytes
                            :public_byte_budget write-refusal/public-byte-budget
                            :blocked_next_call_for (:error-type receipt)}]
          (if (true? (:ok receipt))
            ;; A receipt that was not otherwise a refusal BECOMES one, under
            ;; the oversize kind, carrying its own safety claims forward.
            (merge (empty-receipt (or (:mode receipt) "preview"))
                   measurements
                   {:ok false
                    :operation :admit-patch-refused
                    :error-type :next-call-exceeds-public-budget
                    :error sentence
                    :source-unchanged (:source-unchanged receipt)
                    :mutation_attempted (:mutation_attempted receipt)
                    :remedy (str "narrow the request so its follow-up call and"
                                 " the receipt carrying it fit "
                                 write-refusal/public-byte-budget
                                 " bytes; fewer files in one patch is the"
                                 " lever, because expect_pre_sha256 carries one"
                                 " digest per file")})
            ;; @spec MCP-OP-ADMIT-142
            ;; A receipt that ALREADY refuses keeps its kind, its remedy and
            ;; its safety claims; the next_call is the only thing that goes.
            ;; Round five measured this arm turning a
            ;; `transaction-recovery-required` refusal -- a third party changed
            ;; the caller's files and the gate could not put them back -- into
            ;; a size complaint whose `mutation_attempted` read `false` and
            ;; whose remedy was `fewer files in one patch is the lever`. That
            ;; is the exact swallowing MCP-OP-ADMIT-139's reduction arm was
            ;; fixed for, one field over, on the arm that fires.
            (merge receipt
                   measurements
                   {:next_call nil
                    :next_call_omitted true
                    :next_call_omission sentence})))))))

;; @spec MCP-OP-ADMIT-139
(def ^:private receipt-identity-keys
  "The keys a receipt is not allowed to lose, whatever its size.

  A receipt that will not fit is REDUCED, never replaced. The first draft of
  this bound replaced an oversize receipt with a typed size complaint, and the
  battery caught what that costs: a 64-file rolled-back transaction, whose
  `transaction-recovery-required` means A THIRD PARTY CHANGED YOUR FILES AND
  THE GATE COULD NOT PUT THEM BACK, came back to the caller as
  `receipt-exceeds-public-budget` with a remedy of `use fewer files`. The most
  safety-critical receipt this gate produces was swallowed by a size rule.
  What may be dropped is bulk; what may never be dropped is the receipt's
  answer to what happened, whether the workspace changed, and what to do."
  [:ok :operation :mode :error-type :error :remedy :source-unchanged
   :mutation_attempted :pre_image_binding :lock_scope :verification_complete
   :verification_status :elapsed_ms :next_call])

;; @spec MCP-OP-ADMIT-140
(def ^:private identity-value-byte-bound
  "The most JSON one receipt-identity value may spend.

  Twelve bounded identity values cannot add to the public budget, which is
  the property that makes `no identity key can be the reason a receipt is
  oversize` true by construction rather than by inspection."
  1024)

;; @spec MCP-OP-ADMIT-140
(defn- bound-identity-values
  "Bound every identity value except the ones that have their own answer.

  `reduce-receipt-to-budget` may never DROP an identity key and `cut`
  shortens only the two sentences, so an identity value carrying bulk was
  reachable by no arm of the bound at all: a caller's 60,000-character `mode`
  published a 61,214-byte receipt, 28,574 over, with no annotation of any kind
  and a 389-character `next_call` blamed for it.

  `:error` and `:remedy` are exempt because `cut` is their answer and it
  states its cut; `:next_call` is exempt because a shortened next_call is not
  a smaller affordance but the absence of one wearing its name, and
  `oversize-next-call-refusal` is its answer; `:error-type` is exempt because
  `checked-refusal-kind!` already bounds it to an enumerated keyword. Every
  value this DOES bound is named in `receipt_identity_bounded`, so a reader is
  never told a value verbatim that was not."
  [receipt]
  (if-not (map? receipt)
    receipt
    ;; NOTE the order: the error-type keyword is written LAST because the
    ;; enumeration tripwire scans this file for a literal error-type key
    ;; followed by a keyword, and a set literal that happened to put one
    ;; after it would read as a refusal kind constructed here.
    ;; @spec MCP-OP-ADMIT-149
    ;; The error-type exemption holds only where its own REASON holds.
    ;; `checked-refusal-kind!` bounds the kind to an enumerated keyword, but
    ;; it fires only on `(not (true? (:ok …)))`, so on an `:ok true` receipt
    ;; the justification is false and the value went out unbounded: 60,443
    ;; bytes through this function, echoed verbatim, nothing named as bounded.
    ;; The gate does not build such a receipt today; a bound that holds by
    ;; construction rather than by its own rule is one refactor from not
    ;; holding.
    (let [exempt (cond-> #{:ok :error :remedy :next_call}
                   (not (true? (:ok receipt))) (conj :error-type))
          candidates (remove exempt receipt-identity-keys)]
      (reduce
        (fn [current key]
          (if-not (contains? current key)
            current
            (let [value (get current key)
                  text (if (string? value) value (pr-str value))]
              (if (<= (write-refusal/json-bytes {key value})
                      identity-value-byte-bound)
                current
                (-> current
                    (assoc key
                           (str (subs text 0 (min (count text)
                                                  caller-field-spelling-characters))
                                "…[cut to " caller-field-spelling-characters
                                " characters; this value was "
                                (count text)
                                " and an identity key may not be the size of"
                                " a receipt]"))
                    (update :receipt_identity_bounded
                            (fnil conj []) (name key)))))))
        receipt
        candidates))))

;; @spec MCP-OP-ADMIT-139
(def ^:private receipt-reduction-keys
  [:receipt_reduced :receipt_omitted_fields :receipt_bytes_before
   :receipt_text_characters_before :public_byte_budget :error_truncated
   ;; @spec MCP-OP-ADMIT-140
   ;; a receipt that dropped the note saying a value was cut would be telling
   ;; the reader a bounded value verbatim
   :receipt_identity_bounded
   ;; @spec MCP-OP-ADMIT-141
   ;; and neither is the payload's own truncation notice bulk: reduction was
   ;; measured dropping all four of these to make room, which is a receipt
   ;; jettisoning the only annotation that makes its cut honest
   :payload_truncated :payload_truncation :payload_omitted
   :payload_omitted_bytes :payload_binding_face
   ;; the terminal answer itself
   :receipt_over_budget :receipt_residual_bytes
   :receipt_residual_text_characters :receipt_unreducible_fields])

;; @spec MCP-OP-ADMIT-139
(defn- reduce-receipt-to-budget
  "Shrink one receipt until BOTH its faces fit the one public budget, keeping
  its identity and its safety claims.

  Round four bounded only the trimmable VECTORS and only as JSON. A sixty-file
  preview under 200-character directory names published a 125,104-byte receipt
  with `:ok true` and a 185,060-character text -- four times the budget, with
  no annotation at all -- because `hashes` is a MAP with one entry per path and
  nothing shortened it.

  Bulk fields go first, largest first, each one named in
  `receipt_omitted_fields`; the identity keys stay. If even those will not fit,
  the `error` and `remedy` sentences are cut with the cut stated rather than
  the receipt being lost. What this function never does is change the receipt's
  `error-type`: a caller must not learn that its patch was too wordy when what
  actually happened is that its workspace needs manual recovery."
  [receipt]
  (if (public-faces-fit? receipt)
    receipt
    (let [bytes-before (write-refusal/json-bytes receipt)
          characters-before (summary-characters receipt)
          annotate (fn [candidate omitted]
                     (cond-> (assoc candidate
                                    :receipt_reduced true
                                    :receipt_omitted_fields omitted
                                    :receipt_bytes_before bytes-before
                                    :receipt_text_characters_before
                                    characters-before
                                    :public_byte_budget
                                    write-refusal/public-byte-budget)
                       (:error_truncated candidate) identity))
          cut (fn [candidate omitted]
                ;; last resort: the sentences, with the cut stated
                (let [shrink (fn [c k]
                               (if-let [text (get c k)]
                                 (assoc c k
                                        (str (subs (str text) 0
                                                   (min 200 (count (str text))))
                                             "…[cut to fit the public payload"
                                             " budget; full text in the"
                                             " server log]"))
                                 c))]
                  (assoc (-> candidate (shrink :error) (shrink :remedy))
                         :error_truncated true)))]
      (loop [current (annotate receipt [])
             omitted []]
        (if (public-faces-fit? current)
          current
          (let [droppable (->> (apply dissoc current
                                      (concat receipt-identity-keys
                                              receipt-reduction-keys))
                               (map (fn [[k v]]
                                      [k (write-refusal/json-bytes {k v})]))
                               (sort-by second >))]
            (if-let [[key _] (first droppable)]
              (let [omitted (conj omitted (name key))]
                (recur (annotate (dissoc current key) omitted) omitted))
              ;; nothing left but identity: cut the sentences once, then stop.
              ;; If a next_call is what still will not fit, the oversize
              ;; next_call refusal below is the honest answer.
              ;; @spec MCP-OP-ADMIT-144
              ;; -- and it is the answer BEFORE the cut, not after it. Round
              ;; six cut both sentences here to make room for a next_call
              ;; `oversize-next-call-refusal` was about to drop anyway, so a
              ;; 52-character error sentence and a 49-character
              ;; manual-recovery remedy reached the caller whole and labelled
              ;; `error_truncated`, pointing an MCP client at a server log it
              ;; cannot read. A sentence is cut only when cutting it is what
              ;; the budget actually needs.
              (if (and (:next_call current)
                       (public-faces-fit? (dissoc current :next_call)))
                current
                (let [final (annotate (cut current omitted) omitted)]
                  (if (public-faces-fit? final)
                    final
                    ;; @spec MCP-OP-ADMIT-141
                    ;; Nothing droppable remains and the sentences are already
                    ;; cut. Round five's tail here was `(if (or ...) final
                    ;; final)` -- both arms identical, the predicate computed
                    ;; and thrown away -- so a 60,563-byte receipt was published
                    ;; carrying `receipt_reduced true`, which a reader can only
                    ;; read as `the reduction succeeded`. A budget this receipt
                    ;; could not be brought inside is a FACT about it, and it is
                    ;; stated in a typed way on both faces rather than left
                    ;; wearing the shape of success.
                    (let [residual (write-refusal/json-bytes final)
                          residual-text (summary-characters final)]
                      (assoc final
                             :receipt_over_budget true
                             :receipt_residual_bytes residual
                             :receipt_residual_text_characters residual-text
                             :receipt_unreducible_fields
                             (->> receipt-identity-keys
                                  (filter #(contains? final %))
                                  (map (fn [k]
                                         [(name k)
                                          (write-refusal/json-bytes
                                            {k (get final k)})]))
                                  (sort-by second >)
                                  (mapv first))))))))))))))

(def ^:private sentence-cut-marker
  "The words `reduce-receipt-to-budget`'s `cut` appends to a sentence."
  "[cut to fit the public payload")

;; @spec MCP-OP-ADMIT-141
;; @spec MCP-OP-ADMIT-144
(defn- redescribe-published-receipt
  "Re-derive one receipt's account of ITSELF from the bytes it will publish.

  The terminal over-budget annotations and `error_truncated` are claims
  about a candidate, and the candidate is not always what is published: when
  a receipt is over budget because of its `next_call`, dropping the call is
  what makes it fit, and every annotation reduction stamped on the way there
  describes a receipt that no longer exists. Round six carried all of them
  forward unexamined and published a 1,670-byte receipt claiming a 30,179-byte
  residual, with `receipt_unreducible_fields` naming a `next_call` it had in
  fact dropped, and a 49-character manual-recovery remedy -- the one
  instruction a human needs after a failed rollback -- labelled as cut to fit
  a budget it is 0.15% of.

  A receipt that fits does not say it is over budget. A sentence that reaches
  the caller whole is not labelled as truncated. Both are read off the
  published value, not remembered."
  [receipt]
  (if-not (map? receipt)
    receipt
    (cond-> receipt
      (public-faces-fit? receipt)
      (dissoc :receipt_over_budget :receipt_residual_bytes
              :receipt_residual_text_characters :receipt_unreducible_fields)

      (not-any? #(str/includes? (str (get receipt %)) sentence-cut-marker)
                [:error :remedy])
      (dissoc :error_truncated))))

;; @spec MCP-OP-ADMIT-069
;; @spec MCP-OP-ADMIT-133
;; @spec MCP-OP-ADMIT-135
;; @spec MCP-OP-ADMIT-136
;; @spec MCP-OP-ADMIT-139
(defn- bound-receipt
  "Fit one public receipt inside the shared MCP payload budget, refusing
  outright when its next_call alone cannot fit, and refusing to publish a
  refusal whose kind is not enumerated.

  This is the one place every receipt `execute-request!` returns passes
  through, and it sits OUTSIDE every `catch` on that path -- so a kind built
  dynamically, or forwarded out of another namespace's ex-data, cannot be
  laundered into the public surface by the enumeration never having heard of
  it.   The oversize check runs LAST, on the receipt that would actually be
  published, so that the envelope and the trimming are both counted before it
  decides (MCP-OP-ADMIT-139); the guard then runs over its refusal too.

  The payload bound is ONE pass against ONE budget with TWO faces: a
  candidate fits only when its JSON fits `public-byte-budget` AND the text
  block that spells every one of its leaves fits the same number. When both
  cannot fit, the STRUCTURE is what gives ground -- it is the face that can
  say `payload_omitted` and be believed -- so the text stays a superset of
  whatever survives (MCP-OP-ADMIT-136). Two passes with two predicates would
  reset the cumulative omission record; a second budget would be a second
  budget."
  [receipt]
  (let [checked (bound-identity-values (checked-refusal-kind! receipt))
        pre-payload (write-refusal/bound-public-refusal checked pr-str)
        ;; @spec MCP-OP-ADMIT-136
        trimmed (write-refusal/bound-public-payload
                  pre-payload trimmable-receipt-keys public-faces-fit?)
        ;; @spec MCP-OP-ADMIT-143
        ;; `payload_omitted_bytes` counts JSON, while the loop's exit test is
        ;; `public-faces-fit?` -- which for this gate can be the TEXT face. A
        ;; payload trimmed because its text face did not fit reported a byte
        ;; figure that was never the binding constraint, so the record names
        ;; which face forced it.
        faced (if (:payload_truncated trimmed)
                (assoc trimmed :payload_binding_face
                       (binding-face pre-payload))
                trimmed)
        ;; @spec MCP-OP-ADMIT-139
        ;; @spec MCP-OP-ADMIT-144
        ;; @spec MCP-OP-ADMIT-151
        ;; The CHEAP CORRECT MOVE FIRST. When the receipt is over budget only
        ;; because of its `next_call`, reduction cannot reach the budget --
        ;; `next_call` is an identity key, `bound-identity-values` exempts it,
        ;; and `cut` shortens only the two sentences -- so the ladder walks
        ;; all the way down, drops every droppable field, cuts both sentences
        ;; and stamps the terminal over-budget annotations, to make room for a
        ;; call that `oversize-next-call-refusal` is about to drop anyway.
        ;; Round six published the result: a 1,670-byte receipt, 5% of the
        ;; budget, saying `receipt_over_budget true` with a residual of 30,179
        ;; bytes, its 52-character error sentence marked as cut. Testing the
        ;; receipt WITHOUT its next_call first takes the move that actually
        ;; fits, and costs the caller nothing else.
        ;;
        ;; @spec MCP-OP-ADMIT-151
        ;; The ORDER is the behaviour, and it is observable in exactly one
        ;; place: what else the receipt lost. Round seven's reviewer replaced
        ;; this whole branch with `(reduce-receipt-to-budget faced)` and the
        ;; focused suite stayed green at 164/4220/0 while the sabotaged
        ;; receipt dropped `hashes` and the honest `payload_trim_unavailable`
        ;; notice -- every assertion was about the published value's
        ;; self-description, and a receipt that pays for a call it is about
        ;; to drop describes itself perfectly honestly. The witness therefore
        ;; asserts that every key other than the call survives VERBATIM.
        bounded (if (and (:next_call faced)
                         (not (public-faces-fit? faced))
                         (public-faces-fit? (dissoc faced :next_call)))
                  faced
                  (reduce-receipt-to-budget faced))]
    ;; @spec MCP-OP-ADMIT-139
    ;; The oversize decision is taken AFTER reduction, on the receipt that
    ;; would actually be published: if something STILL will not fit once every
    ;; droppable field is gone, the next_call is what is left, and blaming it
    ;; is then a fact rather than a guess. The guard runs last so the
    ;; refusal's own kind is checked too.
    ;; @spec MCP-OP-ADMIT-140
    ;; The oversize refusal is bounded by the SAME pass as everything else.
    ;; Round five published it around the bound: it was built by
    ;; `(merge (empty-receipt (or (:mode receipt) "preview")) ...)`, which
    ;; re-echoed the caller's 60,000-character mode into the receipt that is
    ;; supposed to be the answer to `this did not fit`.
    (checked-refusal-kind!
      (if-let [replacement (oversize-next-call-refusal bounded)]
        ;; @spec MCP-OP-ADMIT-144
        ;; RE-DERIVE, never inherit. Dropping the next_call is what makes the
        ;; receipt fit, and it fits with room to spare -- so every claim
        ;; reduction made about a candidate that no longer exists has to be
        ;; re-tested against the bytes actually published.
        (redescribe-published-receipt
          (reduce-receipt-to-budget (bound-identity-values replacement)))
        bounded))))

;; ---------------------------------------------------------------------------
;; Entry point
;; ---------------------------------------------------------------------------

(defn- request-patch
  [params]
  (or (get params :patch) (get params "patch")))

(defn- oversize-refusal
  [bytes]
  (merge (empty-receipt "preview")
         {:ok false
          :operation :admit-patch-refused
          :error-type :patch-too-large
          :error (str "patch is " bytes " UTF-8 bytes; the admission limit is "
                      max-patch-bytes)
          :patch_bytes bytes
          :next_call {:tool "admit_clojure_patch"
                      :patch_field "patch"
                      :note (str "split the change into patches of at most "
                                 max-patch-bytes " UTF-8 bytes")
                      :blocked_by :patch-too-large}}))

;; @spec MCP-OP-ADMIT-002
;; @spec MCP-OP-ADMIT-066
;; @spec MCP-OP-ADMIT-071
(defn execute-request!
  "Route and execute one admit_clojure_patch request.

  The size cap is enforced on the raw request, before any JSON round-trip:
  a payload large enough to trip the decoder's own string limits must become a
  typed refusal, never an exception escaping the handler."
  [config params]
  (let [raw-patch (request-patch params)
        bytes (patch-bytes raw-patch)]
    (if (> bytes max-patch-bytes)
      (bound-receipt (oversize-refusal bytes))
      (let [normalized
            (try
              {:ok true
               :params (json/parse-string (json/generate-string params) true)}
              (catch Exception error
                ;; @spec MCP-OP-ADMIT-148
                ;; A DECODER limit is not the patch being too large. The
                ;; patch's own byte size was already checked above, so the
                ;; only decoder constraint a patch's bytes can trip here is
                ;; the string-value length; a name length, a nesting depth or
                ;; a number length is a fact about the request's STRUCTURE.
                ;; Round six measured a 230-byte patch published as
                ;; `:patch-too-large` because a 60,000-character KEY NAME
                ;; exceeded `StreamReadConstraints.getMaxNameLength()`, with
                ;; `next_call.blocked_by` saying the same and no remedy at
                ;; all -- so an agent that branches on the field splits a
                ;; patch that was never too large, and splits again.
                (let [message (str (.getMessage error))
                      constraint? (str/includes? (.getName (class error))
                                                 "StreamConstraints")
                      patch-sized? (str/includes? message
                                                  "getMaxStringLength")]
                  (cond-> {:ok false
                           :error-type (cond
                                         (and constraint? patch-sized?)
                                         :patch-too-large
                                         constraint?
                                         :request-decode-constraint-exceeded
                                         :else :invalid-admit-request)
                           :error (str "request could not be decoded: "
                                       message)}
                    (and constraint? (not patch-sized?))
                    (assoc :remedy
                           (str "this is a JSON decoder limit on the"
                                " request's own structure, not the patch's"
                                " size: the patch is " bytes " UTF-8 bytes,"
                                " inside the " max-patch-bytes "-byte"
                                " admission limit. Shorten the request field"
                                " the decoder names above -- splitting the"
                                " patch will not change this answer"))))))]
        (if-not (:ok normalized)
          (bound-receipt
            (merge (empty-receipt "preview")
                   {:ok false
                    :operation :admit-patch-refused
                    :error-type (:error-type normalized)
                    :error (:error normalized)
                    ;; @spec MCP-OP-ADMIT-148
                    :remedy (:remedy normalized)
                    :next_call {:tool "admit_clojure_patch"
                                :patch_field "patch"
                                :blocked_by (:error-type normalized)}}))
          (let [router (or (:workspace-router config) (workspace/router config))
                routed (workspace/resolve-request router (:params normalized))]
            (if-not (:ok routed)
              (bound-receipt
                (merge (empty-receipt "preview")
                       {:ok false
                        :operation :admit-patch-refused
                        :error-type :invalid-workspace-root
                        :error (:error routed)}))
              (let [context (:config routed)
                    result (try
                             (execute-in-context! context (:params routed)
                                                  (:workspace-root routed))
                             (catch Exception error
                               (let [data (ex-data error)
                                     error-type (or (:error-type data)
                                                    :admit-tool-failure)]
                                 (merge (empty-receipt (or (:mode params)
                                                           "preview"))
                                        (select-keys data [:lock-path])
                                        {:ok false
                                         :operation :admit-patch-refused
                                         :error-type error-type
                                         :error (or (.getMessage error)
                                                    (.getName (class error)))
                                         :next_call
                                         (next-call
                                           {:workspace-root (:workspace-root routed)
                                            :patch (request-patch params)
                                            :verify (:verify params)}
                                           "preview" error-type)}))))]
                (record-telemetry! context (:params routed) result)
                (bound-receipt
                  (assoc result :workspace-root (:workspace-root routed)))))))))))

;; ---------------------------------------------------------------------------
;; MCP surface
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-123
(defn- detector-note
  "The text block's copy of `detectors_not_run`, verbatim.

  A text block that is a strict subset of structuredContent is a receipt that
  reads clean to every consumer who sees only the text -- which is most of
  them. Whatever structure names here, the text names too."
  [result]
  (let [absent (:detectors_not_run result)]
    (if (seq absent)
      (str "\ndetectors that did not run: "
           (str/join ", "
                     (map (fn [{:keys [detector reason]}]
                            (str detector " (" (name reason) ")"))
                          absent))
           "\nthis receipt reports what was inspected; with a detector silent"
           " it is not a clean bill of health")
      "")))

;; ---------------------------------------------------------------------------
;; The text block is a superset of structuredContent -- on BOTH branches
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-134
(def admit-receipt-fact-exclusions
  "The receipt keys the fact walk below does NOT render. It is EMPTY, and
  that is the point.

  Round three excluded eleven keys and, separately, three SHAPES -- an empty
  map, an empty vector, a `nil` -- and its witness re-declared the same
  eleven keys on its own side before checking that they were absent. A test
  that copies the policy it is auditing agrees with the implementation by
  construction; the reviewer's word for it was `tautological`, and it was
  right. Two of those eleven, `:files` and `:hashes`, were excluded on the
  reasoning that they are `diff metadata the caller already sent` -- a
  judgement about VALUE, not about whether the text is a superset, and the
  caller who reads only the text is exactly the caller who cannot go look
  them up.

  So there is no exclusion list to get wrong. Every leaf of the receipt
  renders in the fact walk, including the four the text also renders on
  their own dedicated lines -- `:error-type` in the header, `:error`,
  `:remedy`, and the verbatim `:next_call`. Those four are duplicated on
  purpose: an exclusion set of size zero is a claim a witness can check with
  no shared policy at all, and cheapness is not worth a hole. If a key ever
  has to leave the walk, it is added here, with its reason, and the witness
  fails until the EARS text names it too."
  #{})

;; @spec MCP-OP-ADMIT-134
(def max-admit-receipt-fact-characters
  "Ceiling on ONE rendered leaf's value. Past this the value is cut, never
  the fact: the text still names the field, the first 200 characters, and
  exactly how many characters it did not print."
  200)

;; @spec MCP-OP-ADMIT-136
(def admit-receipt-fact-head
  "The receipt keys whose leaves elision never reaches, in render order.

  Round four sorted every fact by path and dropped from the tail, so the
  four fields a caller most needs -- `source-unchanged`, `mutation_attempted`,
  `pre_image_binding`, `lock_scope`: did you touch my files, did you try to
  write, is this bound to the bytes I read, what did you lock -- were the
  first to go, because their names sort late. A field's position in the
  alphabet is not a statement about how much it matters.

  So the walk renders these first and never elides them. `error`, `remedy`
  and `next_call` are here for the same reason they have their own lines:
  they are the receipt's answer to `what now`."
  [:ok :operation :source-unchanged :mutation_attempted :pre_image_binding
   :lock_scope :error :remedy :next_call
   ;; @spec MCP-OP-ADMIT-141
   ;; a receipt reduction could not bring inside the budget says so on the
   ;; text face too, and elision is exactly what must not reach that
   :receipt_over_budget :receipt_residual_bytes
   ;; @spec MCP-OP-ADMIT-142
   ;; and a receipt that had to drop its follow-up call says that where
   ;; elision cannot reach either
   :next_call_omitted :next_call_omission])

;; @spec MCP-OP-ADMIT-136
(defn- fact-root
  "The receipt key one leaf path belongs to -- `files[3].path` -> `files`."
  [path]
  (let [n (count path)
        dot (or (str/index-of path ".") n)
        bracket (or (str/index-of path "[") n)]
    (subs path 0 (min dot bracket))))

;; @spec MCP-OP-ADMIT-134
(defn admit-leaf-entries
  "Every leaf reachable in `v`, as [dotted/bracketed-path display-string]
  pairs, depth-first.

  A leaf here is what a JSON reader sees as a leaf, which includes the
  value-less shapes: `{}`, `[]`, `null` and `\"\"` each contribute one
  entry carrying the characters JSON spells for them. Round three returned
  nothing for an empty collection or a `nil`, on the reasoning that `an
  absent field is not a fact` -- but structuredContent still shows the key,
  so a text that omits it is a strict subset of the structure, which is the
  defect."
  [path v]
  (cond
    (and (map? v) (seq v))
    (mapcat (fn [[k cv]]
              (admit-leaf-entries (str path (when (seq path) ".") (name k)) cv))
            (sort-by (comp str key) v))

    (map? v)
    [[path "{}"]]

    (and (coll? v) (seq v))
    (apply concat
           (map-indexed
             (fn [i cv] (admit-leaf-entries (str path "[" i "]") cv))
             (if (set? v) (sort-by pr-str v) v)))

    (coll? v)
    [[path "[]"]]

    (nil? v)
    [[path "null"]]

    (= v "")
    [[path "\"\""]]

    (keyword? v)
    ;; the characters cheshire spells for it, namespace included -- `name`
    ;; would drop the namespace and disagree with structuredContent
    [[path (subs (str v) 1)]]

    :else
    [[path (str v)]]))

;; @spec MCP-OP-ADMIT-134
(defn- rendered-fact
  "One `path=value` fact, its value cut at the per-leaf ceiling and the cut
  stated in characters."
  [[path text]]
  (str path "="
       (if (> (count text) max-admit-receipt-fact-characters)
         (str (subs text 0 max-admit-receipt-fact-characters)
              "…[+" (- (count text) max-admit-receipt-fact-characters)
              " characters in structuredContent]")
         text)))

;; @spec MCP-OP-ADMIT-134
;; @spec MCP-OP-ADMIT-135
;; @spec MCP-OP-ADMIT-136
(defn- admit-receipt-facts
  "Every leaf of `result` a text-reading client would otherwise never see,
  rendered inside `budget` characters -- what is actually LEFT of the one
  public byte budget after the rest of this text block has been counted.

  Round four charged this section a fixed half of `public-byte-budget`. That
  was a second, invented budget -- the same defect round three blocked on for
  `next_call`, one field over -- and it bit on an ordinary receipt: a
  twenty-file preview whose structured face was 14,918 bytes, under half the
  budget and untruncated, published a text missing 71 of its own leaves. The
  headroom the half-budget reserved was never contested; the leaves were lost
  anyway.

  There is one budget and this section is charged the remainder of it, so a
  receipt whose whole text fits publishes every leaf. When it does not fit,
  the STRUCTURED face gives ground first -- `bound-receipt` trims the
  receipt's own bounded collections until the text that spells it fits, so
  supersetness is preserved by shrinking the structure rather than by
  quietly shortening the text.

  What is left after that cannot be dropped silently. The order is: (1) each
  leaf's VALUE is cut at `max-admit-receipt-fact-characters`, naming the cut;
  (2) `admit-receipt-fact-head` renders first and is never elided; (3) the
  remaining leaves elide from the tail of the path-sorted order, and the text
  NAMES the elided paths and states their exact count; (4) if the naming
  itself will not fit, the names shrink and the count of unnamed paths is
  stated -- the count is exact at every step; (5) the `next_call` is rendered
  after this section, verbatim, and is never elided at any size
  (MCP-OP-ADMIT-135)."
  [result budget]
  (let [entries (->> (apply dissoc result admit-receipt-fact-exclusions)
                     (mapcat (fn [[k v]] (admit-leaf-entries (name k) v)))
                     (sort-by first)
                     vec)
        order (into {} (map-indexed (fn [i k] [(name k) i]))
                    admit-receipt-fact-head)
        head (->> entries
                  (filter (fn [[path _]] (contains? order (fact-root path))))
                  (sort-by (fn [[path _]] [(order (fact-root path)) path]))
                  vec)
        tail (filterv (fn [[path _]] (not (contains? order (fact-root path))))
                      entries)
        head-facts (mapv rendered-fact head)
        tail-facts (mapv rendered-fact tail)
        tail-paths (mapv first tail)
        total (count tail-facts)
        ;; Render the WHOLE section, elision note included, and measure that.
        ;; Budgeting the join of the parts and then appending the marker is
        ;; how a bound gets quietly exceeded by the thing that announces it.
        line (fn [kept named]
               (let [dropped (- total kept)]
                 (str "facts · "
                      (str/join " · " (into head-facts (subvec tail-facts 0 kept)))
                      (when (pos? dropped)
                        (str "\nfacts_elided · " dropped
                             " leaf(s) are in structuredContent and not above: "
                             (str/join " · " (subvec tail-paths kept
                                                     (+ kept (min named dropped))))
                             (when (< named dropped)
                               (str " · [+" (- dropped named)
                                    " path(s) not named here]")))))))]
    (when (seq entries)
      ;; keeping one more fact always costs more than naming it costs, so the
      ;; rendered length rises with `kept` and a bisection is exact
      (let [kept (loop [low 0 high total]
                   (if (< low high)
                     (let [mid (quot (+ low high 1) 2)]
                       (if (<= (count (line mid (- total mid))) budget)
                         (recur mid high)
                         (recur low (dec mid))))
                     low))]
        (loop [named (- total kept)]
          (let [rendered (line kept named)]
            (cond
              (<= (count rendered) budget) rendered
              (pos? named) (recur (dec named))
              ;; @spec MCP-OP-ADMIT-141
              ;; Below roughly 191 characters of remainder the note that
              ;; ANNOUNCES the elision is itself longer than the remainder,
              ;; so the section exceeded the budget it was handed in order to
              ;; say that it had to. A shorter note is tried, and if even
              ;; that will not fit the section is absent rather than over --
              ;; a bound announced by a thing not charged against it is the
              ;; same defect this round's blocker was.
              :else
              (let [short-note (str "facts_elided · " total
                                    " leaf(s) are in structuredContent"
                                    " and not above")]
                (when (<= (count short-note) budget) short-note)))))))))

;; @spec MCP-OP-ADMIT-132
;; @spec MCP-OP-ADMIT-135
(defn- admit-rendered-next-call
  "The next_call line: the JSON verbatim at any size, or a stated absence.

  Round three replaced a next_call above 1,024 characters with a pointer at
  structuredContent -- an invented second budget one thirtieth the size of
  the real one, and the tool description tells callers to copy
  `expect_pre_sha256` out of this very field. A routine 14-file preview
  produced 1,550 characters here and 1,554 in the review, so the instructed
  copy was already impossible for an ordinary change -- and impossible
  precisely for the text-only caller this ratchet exists for, because a
  pointer at structuredContent is no use to someone who cannot read
  structuredContent. The next_call is the one thing in a receipt
  a caller must be able to send back byte for byte; it is rendered last so
  that everything else gives ground before it, and it never gives ground.
  A next_call that alone cannot fit the public payload budget is a typed
  refusal (`oversize-next-call-refusal`), never a pointer."
  [result]
  (if-let [call (:next_call result)]
    (str "next_call · " (json/generate-string call))
    (str "next_call · none — this receipt has no follow-up call")))

;; @spec MCP-OP-ADMIT-134
(defn- summary-head
  "Everything the text block says above the fact section: the header line,
  and on a refusal the error sentence, the source-unchanged claim, the
  detector note and the remedy line."
  [result]
  (if (:ok result)
    (str "admit_clojure_patch\n  " (name (:operation result))
         " · " (count (:files result)) " file(s)"
         " · owners +" (count (get-in result [:owners :added]))
         " ~" (count (get-in result [:owners :changed]))
         " -" (count (get-in result [:owners :removed]))
         " · drift " (:byte_drift_outside_hunks result) " bytes"
         " · hazards " (count (:hazards result))
         " · " (mcp-operation/format-elapsed-ms (:elapsed_ms result))
         "\nverification_complete=" (:verification_complete result)
         " verification_status="
         (name (or (:verification_status result) :unverified))
         (detector-note result))
    (str "admit_clojure_patch refused · " (name (or (:error-type result)
                                                    :unknown))
         " · " (mcp-operation/format-elapsed-ms (:elapsed_ms result))
         "\n" (:error result)
         ;; @spec MCP-OP-ADMIT-129
         (if (true? (:source-unchanged result))
           "\nsource unchanged"
           "\nwhether the workspace was changed is unverified")
         (detector-note result)
         ;; @spec MCP-OP-ADMIT-131
         (when-let [remedy (:remedy result)]
           (str "\nremedy · " remedy)))))

;; @spec MCP-OP-ADMIT-134
;; @spec MCP-OP-ADMIT-135
;; @spec MCP-OP-ADMIT-136
(defn- summary
  "One receipt's text face, inside the ONE public byte budget.

  The header and the verbatim `next_call` are rendered first and measured;
  the fact walk is then charged EXACTLY what is left, so a receipt whose
  whole text fits publishes every leaf its structuredContent spells. Round
  four gave the fact walk a fixed half of the budget instead and dropped 71
  leaves from a twenty-file preview whose text was 18,761 characters -- less
  than three fifths of the budget it was nowhere near."
  ([result] (summary result write-refusal/public-byte-budget))
  ([result budget]
   (let [head (summary-head result)
         ;; @spec MCP-OP-ADMIT-132
         ;; @spec MCP-OP-ADMIT-135
         next-call (admit-rendered-next-call result)
         fixed (str head "\n" next-call)
         ;; the newline that would join the fact section to the head
         remaining (- budget (count fixed) 1)]
     (if-let [facts (admit-receipt-facts result remaining)]
       (str head "\n" facts "\n" next-call)
       fixed))))

;; @spec MCP-OP-ADMIT-136
(defn- summary-characters
  "An upper bound on the length of the text face `receipt` will publish.

  It is the length of the text that renders EVERY leaf, not the length of
  the text that would be published: `summary` always fits its budget by
  eliding, so asking it whether it fits would always be answered yes and
  the structure would never give ground. The question this bound asks is
  `would anything have to be elided`.

  `:elapsed_ms` is stamped by `mcp-operation/finalize-result` AFTER the
  receipt is bounded, so the widest rendering `format-elapsed-ms` can produce
  stands in for it here. Over-reserving costs a few leaves of the structured
  face; under-reserving would cost supersetness, and the two are not
  symmetric. Any residual overshoot is caught exactly by `summary`, which
  charges the fact walk the real remainder and names whatever it elides."
  [receipt]
  (count (summary (assoc receipt :elapsed_ms Double/MAX_VALUE)
                  Long/MAX_VALUE)))

;; @spec MCP-OP-ADMIT-129
(defn- edge-throwable-refusal
  "Turn anything that reached the handler's edge into a typed refusal.

  Every catch below this one is `(catch Exception ...)`, and an
  `OutOfMemoryError` raised while both analyzer images are live -- below the
  read ceiling, on findings diversity rather than findings size -- is an
  `Error`. It escaped with no receipt at all, and a caller that receives
  nothing cannot tell a refusal from a write.

  The heap is named because it is the number that would lift it. What is NOT
  named is `source unchanged`: this is the one edge where the gate does not
  know how far the failure got, and a false claim of safety here would
  terminate the investigation that should start."
  [^Throwable error]
  (let [oom? (instance? OutOfMemoryError error)
        max-heap-mib (quot (.maxMemory (Runtime/getRuntime)) (* 1024 1024))
        message (or (.getMessage error) (.getName (class error)))]
    (merge (empty-receipt "preview")
           {:ok false
            :operation :admit-patch-refused
            :mutation_attempted nil
            :source-unchanged nil
            :error-type (cond
                          oom? :analyzer-memory-exhausted
                          (str/includes? (.getName (class error))
                                         "StreamConstraints") :patch-too-large
                          (instance? Error error) :admit-tool-error
                          :else :admit-tool-failure)
            :error (if oom?
                     (str "the admit gate exhausted its heap (" max-heap-mib
                          " MiB maximum) before it could publish a reading: "
                          message)
                     message)}
           (when oom?
             {:max_heap_mib max-heap-mib
              :remedy (str "raise the server heap (MCP_JAVA_OPTS -J-Xmx; "
                           max-heap-mib " MiB at this call) or narrow the"
                           " patch to fewer files")}))))

(defn handle-admit-clojure-patch
  "clojure-mcp callback handler retained as a Var for hot reload."
  [_exchange params callback]
  (mcp-operation/invoke!
    ;; @spec MCP-OP-ADMIT-133
    {:execute #(checked-refusal-kind!
                 (try
                 (if-let [config @runtime-config]
                   (execute-request! config params)
                   (merge (empty-receipt "preview")
                          {:ok false
                           :operation :admit-patch-refused
                           :error-type :server-not-initialized
                           :error "admit_clojure_patch server is not initialized"}))
                 ;; @spec MCP-OP-ADMIT-146
                 ;; Both catch arms publish THROUGH the bound. Round six
                 ;; wrapped them in `checked-refusal-kind!` alone, so a
                 ;; throwable message reached the caller verbatim: 60,617
                 ;; bytes of structuredContent, 27,977 past the budget, with
                 ;; no annotation of any kind. A receipt this handler can
                 ;; publish and `bound-receipt` never sees is a hole in
                 ;; MCP-OP-ADMIT-139's universal sentence, whether or not a
                 ;; caller input is known that reaches it.
                 (catch Exception error
                   (bound-receipt
                     (merge (empty-receipt "preview")
                            {:ok false
                             :operation :admit-patch-refused
                             :error-type (if (str/includes?
                                               (.getName (class error))
                                               "StreamConstraints")
                                           :patch-too-large
                                           :admit-tool-failure)
                             :error (or (.getMessage error)
                                        (.getName (class error)))})))
                 ;; @spec MCP-OP-ADMIT-129
                 ;; @spec MCP-OP-ADMIT-146
                 (catch Throwable error
                   (bound-receipt (edge-throwable-refusal error)))))
     :summarize summary
     :callback callback}))

;; @spec MCP-OP-ADMIT-001
(def admit-clojure-patch-tool
  {:id :admit-clojure-patch
   :name "admit_clojure_patch"
   :description admit-tool-description
   :schema admit-tool-schema
   :output-schema admit-output-schema
   :structured? true
   :tool-fn #'handle-admit-clojure-patch})
