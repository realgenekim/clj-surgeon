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

(def admit-tool-description
  (str
    "Admit one natively authored unified diff and return a single receipt. "
    "Pass the exact patch text you would give apply_patch. The gate applies it "
    "to a frozen in-memory snapshot, then reports the change as form identity: "
    "which top-level owners changed, added, or vanished; which bytes moved with "
    "no structural reason; which comments, metadata, reader conditionals, or "
    "#_ discards were disturbed; and typed hazards a line patcher cannot see "
    "(unreadable post image, duplicate top-level definition however it is "
    "wrapped, a lost ns require, an edit inside an opaque code-shaped string). "
    "mode=preview is the default and never writes. verify=focused runs the "
    "clj-kondo finding delta and the focused tests against that snapshot "
    "BEFORE any write; blocking findings or failing tests write nothing. "
    "mode=commit then writes every changed file in one atomic compare-and-swap "
    "transaction. Copy expect_pre_sha256 from a preview's next_call to bind the "
    "commit to the bytes the preview inspected; a commit whose files moved "
    "since refuses. verification_complete is true only when the analyzer ran "
    "clean and the focused runner produced attributable test evidence. "
    "One call replaces the re-read, the git diff, and the focused test run."))

(def admit-tool-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string"}
    ;; A soft note for callers; the enforced limit is UTF-8 bytes.
    "patch" {:type "string" :maxLength max-patch-bytes}
    "mode" {:type "string" :enum ["preview" "commit"]}
    "verify" {:type "string" :enum ["focused" "none"]}
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

;; @spec MCP-OP-ADMIT-069
(defn- next-call
  "One executable follow-up that never carries the patch back.

  Echoing the payload would let a refusal grow without bound with the very
  input that caused it, and the caller already holds the text. The digest
  binds the follow-up to the same patch without restating it."
  [{:keys [workspace-root patch verify expect-pre]} mode blocked-by]
  (cond-> {:tool "admit_clojure_patch"
           :arguments (cond-> {:mode mode :verify (or verify "focused")}
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
   :source-unchanged true
   :next_call nil})

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
                  :skipped 0}))))))

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
   {:detector "focused-tests"
    :reading? (or (true? (:ok evidence)) (= :tests-failed (:reason evidence)))
    :clean? (true? (:ok evidence))
    :reason (or (:reason evidence) :no-test-evidence)}])

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
  (if (not= "focused" verify)
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
  (if (not= "focused" verify)
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
  [config images verify]
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
        snapshot (temp-tree! "clj-surgeon-admit-snapshot")]
    (try
      (materialize! snapshot
                    (remove #(= :delete (:operation %)) images)
                    :post)
      (let [tests (if (seq (:missing plan))
                    ;; @spec MCP-OP-ADMIT-111
                    ;; The tree named a suite that is not in the tree. Running
                    ;; the runner here buys an opaque exit code; refusing here
                    ;; names the file, the namespace and the paths tried.
                    {:ran false
                     :reason :focused-namespace-missing
                     :missing_focused_namespaces (:missing plan)
                     :namespaces namespaces}
                    (test-runner config {:namespaces namespaces
                                         :snapshot-root (.getPath snapshot)}))
            tests (merge {:ran false :passed 0 :failed 0 :skipped 0
                          :tests-run 0 :namespaces namespaces}
                         tests
                         profile-provenance)
            evidence (test-evidence tests namespaces)
            ;; @spec MCP-OP-ADMIT-125
            verdicts (detector-verdicts lint evidence)
            {:keys [status reasons]} (verification-status verify lint evidence)
            lint-blocking (and (:ran lint) (false? (:ok lint)))
            tests-blocking (and (:ran tests)
                                (pos? (long (or (:failed tests) 0))))]
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
                      tests-blocking :focused-tests-failed
                      :else nil)})
      (finally
        (delete-tree! snapshot)))))

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
      (when-not (and allow-partial?
                     (= "focused" verify)
                     (= :partial (:verification_status verification))
                     profile-absent?
                     analyzer-read?)
        reason))))

(defn- execute-in-context!
  [config {:keys [patch mode verify expect_pre_sha256 allow_partial]}
   workspace-root]
  (let [mode (or mode "preview")
        verify (or verify "focused")
        context {:mode mode :workspace-root workspace-root
                 :patch patch :verify verify}]
    (cond
      (not (and (string? patch) (not (str/blank? patch))))
      (refusal context :invalid-patch
               "patch must be non-blank unified diff text")

      (not (contains? #{"preview" "commit"} mode))
      (refusal context :invalid-admit-request
               "mode must be preview or commit")

      (not (contains? #{"focused" "none"} verify))
      (refusal context :invalid-admit-request
               "verify must be focused or none")

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
                              base-binding (cond
                                             ;; Nothing existed to bind to.
                                             (every? #(= :add (:operation %))
                                                     images) "created"
                                             (seq expect_pre_sha256) "bound"
                                             :else "unbound")
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
                                  (if (= "focused" verify)
                                    (verify-snapshot! config images verify)
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
                                (and blocked (= "commit" mode))
                                (merge base verification
                                       {:ok false
                                        :operation :admit-patch-refused
                                        :committed false
                                        :mutation_attempted false
                                        :source-unchanged true
                                        :error-type :verification-failed
                                        :error (str "Snapshot verification failed ("
                                                    (name blocked)
                                                    "); nothing was written")
                                        :next_call (next-call context "preview"
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
                                          :error-type :verification-incomplete
                                          :error
                                          (str "Verification did not complete ("
                                               (name (:verification_status
                                                       verification))
                                               ": "
                                               (str/join ", "
                                                         (map name
                                                              (:verification_reasons
                                                                verification)))
                                               "); nothing was written. Run "
                                               "mode preview to see the same "
                                               "receipt without a write, and "
                                               "repair "
                                               (name reason)
                                               " before committing.")
                                          ;; @spec MCP-OP-ADMIT-120
                                          ;; Propose the verify that can lift
                                          ;; this, not the one that just
                                          ;; failed to.
                                          :next_call
                                          (next-call (assoc context
                                                            :verify "focused")
                                                     "preview"
                                                     :verification-incomplete)}))

                                (= "preview" mode)
                                (merge base verification
                                       {:operation :admit-patch-preview
                                        :next_call (next-call context "commit" nil)}
                                       (when blocked
                                         {:ok false
                                          :operation :admit-patch-refused
                                          :error-type :verification-failed
                                          :error (str "Snapshot verification failed ("
                                                      (name blocked) ")")
                                          :next_call (next-call context "preview"
                                                                blocked)}))

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

;; @spec MCP-OP-ADMIT-069
(defn- bound-receipt
  "Fit one public receipt inside the shared MCP payload budget."
  [receipt]
  (-> receipt
      (write-refusal/bound-public-refusal pr-str)
      (write-refusal/bound-public-payload trimmable-receipt-keys)))

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
                {:ok false
                 :error-type (if (str/includes? (.getName (class error))
                                                "StreamConstraints")
                               :patch-too-large
                               :invalid-admit-request)
                 :error (str "request could not be decoded: "
                             (.getMessage error))}))]
        (if-not (:ok normalized)
          (bound-receipt
            (merge (empty-receipt "preview")
                   {:ok false
                    :operation :admit-patch-refused
                    :error-type (:error-type normalized)
                    :error (:error normalized)
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
;; The refusal text is a superset of structuredContent
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-ADMIT-131
(def ^:private admit-refusal-envelope-keys
  "Receipt keys the text renders on their own dedicated line, so the generic
  fact walk below does not duplicate them, or that the fact walk would
  otherwise bury the diagnostic that matters under low-value structural
  detail.

  `:files` and `:hashes` are excluded for the second reason: a multi-file
  patch's per-file hunk spans and pre/post digests are already the largest
  branch of the receipt, and a refusal's job is to say why, not to re-dump
  diff metadata the caller already sent -- the pre-image digests a caller
  actually needs back (to bind a commit) are `next_call`'s own
  `expect_pre_sha256`, rendered verbatim by `admit-rendered-next-call`."
  #{:ok :operation :error-type :error :next_call :remedy :elapsed_ms
    :workspace-root :detectors_not_run :source-unchanged :mode
    :files :hashes})

;; @spec MCP-OP-ADMIT-131
(def ^:private max-admit-refusal-fact-characters
  "Ceiling on one rendered leaf. A fact is elided past this length, never
  dropped: the text still names the field and how much was cut."
  200)

;; @spec MCP-OP-ADMIT-131
(def ^:private max-admit-refusal-facts
  "How many leaves one refusal text renders before it states how many more
  there were, rather than silently stopping."
  40)

;; @spec MCP-OP-ADMIT-131
(def ^:private max-rendered-admit-next-call-characters
  "Ceiling on the next_call JSON a receipt's text inlines verbatim.

  Above this, the text names the field, its length, and where the caller can
  read it in full (structuredContent.next_call) -- never silence."
  1024)

;; @spec MCP-OP-ADMIT-131
(defn- admit-leaf-entries
  "Every scalar leaf reachable in `v`, as [dotted/bracketed-path value]
  pairs, depth-first.

  An empty map or vector contributes no leaves -- an absent field is not a
  fact -- and a `nil` leaf is likewise silent, because `nil` is the closed
  receipt's own default for a field nothing populated."
  [path v]
  (cond
    (map? v)
    (mapcat (fn [[k cv]]
              (admit-leaf-entries (str path (when (seq path) ".") (name k)) cv))
            (sort-by (comp str key) v))

    (sequential? v)
    (apply concat
           (map-indexed
             (fn [i cv] (admit-leaf-entries (str path "[" i "]") cv))
             v))

    (nil? v)
    []

    :else
    [[path v]]))

;; @spec MCP-OP-ADMIT-131
(defn- admit-refusal-facts
  "Every leaf of a refusal `result` a text-reading client would otherwise
  never see.

  A refusal has two faces -- structuredContent and content[0].text -- and a
  client that reads only the text must not be told less than one that reads
  the structure. A field renders only when it differs from the closed empty
  receipt's own default for that key, so a refusal that never reached a
  check does not bury its cause under a page of zeros and empty vectors --
  and a field the gate actually populated, however deep, is never
  suppressed by that filter."
  [result]
  (let [baseline (empty-receipt (or (:mode result) "preview"))
        leaves (->> (apply dissoc result admit-refusal-envelope-keys)
                    (remove (fn [[k v]] (= v (get baseline k))))
                    (mapcat (fn [[k v]] (admit-leaf-entries (name k) v)))
                    (filter (fn [[_ v]]
                              (or (string? v) (number? v) (boolean? v)
                                  (keyword? v) (symbol? v))))
                    (sort-by first))
        rendered (take max-admit-refusal-facts leaves)
        overflow (- (count leaves) (count rendered))]
    (when (seq rendered)
      (str "facts · "
           (str/join
             " · "
             (map (fn [[path v]]
                    (let [text (if (or (keyword? v) (symbol? v)) (name v) (str v))]
                      (str path "="
                           (if (> (count text) max-admit-refusal-fact-characters)
                             (str (subs text 0 max-admit-refusal-fact-characters) "…")
                             text))))
                  rendered))
           (when (pos? overflow)
             (str " · [+" overflow " more facts in structuredContent]"))))))

;; @spec MCP-OP-ADMIT-131
;; @spec MCP-OP-ADMIT-132
(defn- admit-rendered-next-call
  "The next_call line: sendable JSON, a bounded pointer, or a stated absence.

  A receipt whose next_call the caller never sees costs a return at random --
  whichever face of the receipt that caller happens to read. This is the
  affordance the tool description tells a caller to copy expect_pre_sha256
  from, so it must be readable from the text alone. Mirrors
  MCP-OP-ALIAS-059's `rendered-next-call`."
  [result]
  (if-let [call (:next_call result)]
    (let [encoded (json/generate-string call)]
      (if (<= (count encoded) max-rendered-admit-next-call-characters)
        (str "next_call · " encoded)
        (str "next_call · " (count encoded)
             " characters, in structuredContent.next_call — send it verbatim")))
    (str "next_call · none — this receipt has no follow-up call")))

(defn- summary
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
         (detector-note result)
         ;; @spec MCP-OP-ADMIT-132
         "\n" (admit-rendered-next-call result))
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
           (str "\nremedy · " remedy))
         (when-let [facts (admit-refusal-facts result)]
           (str "\n" facts))
         "\n" (admit-rendered-next-call result))))

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
    {:execute #(try
                 (if-let [config @runtime-config]
                   (execute-request! config params)
                   (merge (empty-receipt "preview")
                          {:ok false
                           :operation :admit-patch-refused
                           :error-type :server-not-initialized
                           :error "admit_clojure_patch server is not initialized"}))
                 (catch Exception error
                   (merge (empty-receipt "preview")
                          {:ok false
                           :operation :admit-patch-refused
                           :error-type (if (str/includes? (.getName (class error))
                                                          "StreamConstraints")
                                         :patch-too-large
                                         :admit-tool-failure)
                           :error (or (.getMessage error)
                                      (.getName (class error)))}))
                 ;; @spec MCP-OP-ADMIT-129
                 (catch Throwable error
                   (edge-throwable-refusal error)))
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
