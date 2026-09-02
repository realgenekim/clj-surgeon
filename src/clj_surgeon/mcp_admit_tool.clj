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

;; @spec MCP-OP-ADMIT-050
(defn- empty-receipt
  "The closed receipt key set, so no path can publish a partial payload."
  [mode]
  {:ok true
   :operation :admit-patch-preview
   :mode mode
   :committed false
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

(defn- kondo-findings
  "Run clj-kondo over one materialized image set and return its findings."
  [project-root paths]
  (let [command (-> (change-buffer/expand-command
                      ["clj-kondo" "--lint" "{files}"] (vec paths))
                    (into ["--cache" "false"
                           "--config" "{:output {:format :edn}}"]))
        {:keys [finished? exit output]} (change-buffer/run-process!
                                          project-root command)
        parsed (when finished?
                 (try (edn/read-string output) (catch Exception _ nil)))]
    (if (and (map? parsed) (vector? (:findings parsed)))
      {:ok true :findings (:findings parsed)}
      {:ok false
       :error-type :clj-kondo-unavailable
       :exit exit
       :output (when output (subs output 0 (min 400 (count output))))})))

;; @spec MCP-OP-ADMIT-040
(defn default-lint-runner
  "Compare analyzer findings between the pre and post images.

  Both images are materialized outside the workspace, so the delta is computed
  against the snapshot and is identical in preview and in commit. Findings are
  compared location-independently, so an unrelated edit that merely moves an
  existing finding is not a regression."
  [{:keys [project-root]} images]
  (if (empty? images)
    {:ran false :reason :no-clojure-files}
    (let [before (temp-tree! "clj-surgeon-admit-pre")
          after (temp-tree! "clj-surgeon-admit-post")]
      (try
        (let [pre (kondo-findings project-root (materialize! before images :pre))
              post (kondo-findings project-root (materialize! after images :post))]
          (if-not (and (:ok pre) (:ok post))
            {:ran false
             :ok false
             :status :unverified
             :error-type (or (:error-type pre) (:error-type post))
             :error "clj-kondo did not produce readable findings"}
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
(defn resolve-focused-test
  "Server start configuration wins; the repository file is the fallback.

  Precedence is explicit because the two sources answer different questions.
  The start config is what this server was launched to do; the repository file
  is what this tree says about itself, and it travels with the tree."
  [{:keys [focused-test project-root]}]
  (or (when (map? focused-test)
        (assoc focused-test :profile-source :server-config))
      (read-focused-test-file project-root)))

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
      (not (and (map? profile) (vector? command) (seq command)))
      {:ran false :reason :no-focused-test-profile :namespaces (vec namespaces)}

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
            {:keys [finished? exit]}
            (change-buffer/run-process! (:project-root config) expanded
                                        (or timeout-ms 300000))
            written? (.isFile report)
            rows (when written?
                   (try (parse-test-report (slurp report)) (catch Exception _ nil)))]
        (cond-> {:ran (boolean finished?)
                 :exit exit
                 :exit-ok (and (boolean finished?) (zero? (long (or exit 0))))
                 :profile-source profile-source
                 :report_written written?
                 :report_written_at (when written? (.lastModified report))
                 :report_started_at started
                 :namespaces (vec namespaces)}
          (not written?) (assoc :reason :no-test-evidence)
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
    (let [lint-ok (and (:ran lint) (not (false? (:ok lint))))
          tests-ok (:ok evidence)
          reasons (cond-> []
                    (not lint-ok) (conj (or (:error-type lint)
                                            (:reason lint)
                                            :analyzer-unverified))
                    (not tests-ok) (conj (or (:reason evidence)
                                             :no-test-evidence)))]
      {:status (cond
                 (and lint-ok tests-ok) :complete
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
        namespaces (->> clojure-images
                        (map :file)
                        (filter #(.exists (io/file (:project-root config)
                                                   (test-namespace-file %))))
                        (map #(str (source-namespace %) "-test"))
                        distinct
                        vec)
        lint (lint-runner config clojure-images)
        snapshot (temp-tree! "clj-surgeon-admit-snapshot")]
    (try
      (materialize! snapshot
                    (remove #(= :delete (:operation %)) images)
                    :post)
      (let [tests (test-runner config {:namespaces namespaces
                                       :snapshot-root (.getPath snapshot)})
            tests (merge {:ran false :passed 0 :failed 0 :skipped 0
                          :tests-run 0 :namespaces namespaces}
                         tests)
            evidence (test-evidence tests namespaces)
            {:keys [status reasons]} (verification-status verify lint evidence)
            lint-blocking (and (:ran lint) (false? (:ok lint)))
            tests-blocking (and (:ran tests)
                                (pos? (long (or (:failed tests) 0))))]
        {:lint_delta lint
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
        (not= admitted declared)
        (refusal context :invalid-admit-request
                 (str "expect_pre_sha256 must name exactly the files the patch "
                      "touches")
                 {:declared (vec (sort declared))
                  :admitted (vec (sort admitted))})

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
(defn- execute-in-context!
  [config {:keys [patch mode verify expect_pre_sha256]} workspace-root]
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
                                    :source-unchanged true
                                    :error-type (:type (first blocking))
                                    :error (:message (first blocking))
                                    :next_call (next-call context "preview"
                                                          (:type (first blocking)))})

                            :else
                            ;; Verification runs against the snapshot, before
                            ;; any write, in both modes. A check that failed
                            ;; blocks the commit; a check that could not run
                            ;; does not block it, but never reads as complete.
                            (let [verification
                                  (if (= "focused" verify)
                                    (verify-snapshot! config images verify)
                                    {:verification_status :unverified
                                     :verification_reasons
                                     [:verification-not-requested]
                                     :verification_complete false})
                                  blocked (::blocking verification)
                                  verification (dissoc verification ::blocking)]
                              (cond
                                (and blocked (= "commit" mode))
                                (merge base verification
                                       {:ok false
                                        :operation :admit-patch-refused
                                        :committed false
                                        :source-unchanged true
                                        :error-type :verification-failed
                                        :error (str "Snapshot verification failed ("
                                                    (name blocked)
                                                    "); nothing was written")
                                        :next_call (next-call context "preview"
                                                              blocked)})

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
                                                              :source-unchanged false
                                                              :lock_scope scope
                                                              :next_call nil})
                                                lock-path
                                                (assoc :lock_path lock-path))]
                                          ;; The caller asked for verification
                                          ;; and did not get any. The write
                                          ;; happened; ok reports the truth
                                          ;; about the proof, not about the
                                          ;; bytes.
                                          (if (and (= "focused" verify)
                                                   (= :unverified
                                                      (:verification_status
                                                        verification)))
                                            (assoc receipt
                                                   :ok false
                                                   :error-type :verification-unverified
                                                   :error (str "Committed, but no "
                                                               "requested check "
                                                               "produced a usable "
                                                               "result: "
                                                               (str/join
                                                                 ", "
                                                                 (map name
                                                                      (:verification_reasons
                                                                        verification))))
                                                   :next_call
                                                   (next-call context "preview"
                                                              :verification-unverified))
                                            receipt))))))))))))))))))))))))

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
         "\nverification_complete=" (:verification_complete result))
    (str "admit_clojure_patch refused · " (name (or (:error-type result)
                                                    :unknown))
         " · " (mcp-operation/format-elapsed-ms (:elapsed_ms result))
         "\n" (:error result)
         "\nsource unchanged")))

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
                                      (.getName (class error)))})))
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
