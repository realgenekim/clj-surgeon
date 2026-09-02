(ns clj-surgeon.mcp-tool
  (:require
   [cheshire.core :as json]
   [clj-surgeon.extract :as extract]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-combinable-transaction :as combinable]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-compact-relations :as compact-relations]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-extraction :as extraction]
   [clj-surgeon.mcp-formatter :as formatter]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-prepared-confirmation :as prepared-confirmation]
   [clj-surgeon.mcp-program-tool :as program-tool]
   [clj-surgeon.mcp-relation-census :as census-tool]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.mcp-workspace-sources :as workspace-sources]
   [clj-surgeon.mcp-write-refusal :as write-refusal]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.file Files Path)
   (java.util UUID)))

;; @spec MCP-OP-EDIT-008
;; @spec MCP-OP-EDIT-009
(def tool-description
  (str
    "Apply one failure-atomic Clojure transaction. For exact nested replacements, "
    "send only workspace_root, edits, and optional verify. Each edits item contains "
    "file, within {form}, from, to, and optional positive matches (default 1). "
    "Do not send changes, expect, basis, or decisions with edits; Surgeon derives "
    "IDs and counts. A redundant top-level expect is ignored and reported, while "
    "every exact per-edit guard remains authoritative. If inspect_clojure returned "
    "basis and next_call, preserve workspace_root, basis, site IDs, and verify; "
    "fill every decision and submit once. To move named owners into one new "
    "namespace, use extraction once. Supply exact caller_changes or explicitly "
    "ignored_caller_files when the decision is known. Omit public_forms to derive only "
    "mechanically required visibility from the same frozen snapshot; omission never "
    "accounts for a discovered caller. When the task already supplies the exact file, "
    "destination, forms, and any caller decisions, submit extraction directly without "
    "plan-extraction. A genuine caller decision refuses pre-write with a completed plan. "
    "When the task supplies an exact external verification command, omit verify and run "
    "that command once after the transaction; do not substitute the full profile. "
    "apply refuses missing, unmoved, already-public, or unsupported declarations. "
    "Exact forms, caller edits, and files counts may be derived. "
    "Direct extraction reports structural caller candidates, not semantic completeness. "
    "Otherwise, use changes for different actions or owner-level edits. Never combine edits and changes. If any insertion, deletion, rename, assoc_entry, or heterogeneous action needs changes, encode every action in one changes array. Each changes item contains id, files, "
    "expect, exactly one of forms or owner, and exactly one action: replace, delete, "
    "insert_before, insert_after, rename_binding, or assoc_entry. Exact replacement, "
    "insertion, and assoc_entry "
    "items contain find, except guarded top-level insertion: omit find and name exactly one forms owner. To delete two or more known named owners, use forms "
    "with delete: true once; do not create marker forms or wait for semantic preparation. "
    "Insertion actions use an array of nonblank strings; one array item may contain several complete forms, "
    "which Surgeon splits in order. Insertion strings refuse malformed forms and refuse comment-bearing gaps. For named top-level "
    "def or defn owners, use forms: [name]. owner is only for the namespace form "
    "and must be {kind: namespace, name: ns-name}; never pass owner as a string. "
    "For one multimethod implementation, use forms: [{kind: defmethod, name: render, dispatch: :card}]. "
    "find and replace must each contain one complete Clojure form. Example item: "
    "{id: status, files: [src/app.clj], forms: [render], find: :old, "
    "replace: :new, expect: {matches: 1, each_form: 1}}. "
    "For a local rename that must preserve a destructured data key, use forms plus "
    "rename_binding: {from: sort-by, to: sort-field, preserve_external_key: true}; "
    "matches counts the binding and its resolved local usages. "
    "To add one key/value to logically equal maps while preserving comments, use "
    "find with assoc_entry: {key: :status, value: :ready}. "
    "Top-level aggregate expect is optional redundant bookkeeping: Surgeon derives changes, edits, and files "
    "from the exact per-change guards and reports a supplied mismatch as ignored normalization. "
    "Omit verify unless the user or repository explicitly requests a configured transaction profile. "
    "When requested, verify is fast, full, or the project-owned exact profile. Staged formatting, "
    "commands, and hot laws roll back on failure. A configured cold job returns "
    "verification_complete=false plus one inspect next_call; continue useful "
    "work and copy it once instead of replaying the edit. Success parses and "
    "reads back every file and publishes an inverse receipt. "
    "verification_complete=true is terminal. When terminal_response is present, "
    "check whether this mutation completes all remaining user-requested work. "
    "If it does, return terminal_response exactly. Do not add text. If work "
    "remains, do not return terminal_response; continue from the terminal evidence. Use native "
    "patching for prose or one arbitrary text edit. For a repeated exact symbol "
    "migration across named owners, use the paired symbol_migration and "
    "require_change fields. symbol_migration groups [owner, from, matches] rows "
    "by file and preserve-name changes only the qualifier. require_change names "
    "the exact target lib/alias and per-file old lib/alias removals. Both tables "
    "are complete authority: Surgeon discovers or chooses none of their values."))

;; @spec MCP-OP-SCHEMA-001
;; @spec MCP-OP-RELAY-003
;; @spec MCP-OP-RELAY-005
(def clj-change-output-schema
  {:type "object"
   :properties {"ok" {:type "boolean"}
                "elapsed_ms" {:type "number" :minimum 0}
                "terminal_response" {:type "string"}}
   :required ["ok" "elapsed_ms"]})

(def exact-terminal-response-text
  "Done — changes committed and exact verification completed.")

;; @spec MCP-OP-RELAY-001
;; @spec MCP-OP-RELAY-002
(defn exact-terminal-response
  "Return one constant relay only for a complete project-owned exact pass."
  [result]
  (let [verification (when (map? result) (:verification result))
        read-back-hashes (when (map? result) (:read_back_hashes result))
        nonblank-string? #(and (string? %) (not (str/blank? %)))
        sha256? #(and (string? %)
                      (boolean (re-matches #"[0-9a-f]{64}" %)))
        contradiction-keys [:error :error_type :error-type :reason :remedy
                            :recovery_required :recovery-required
                            :next_call :next-call :next-action
                            :verification_job :verification-job
                            :source_unchanged :source-unchanged
                            :rolled_back :rolled-back]
        no-contradictory-outcome?
        (and (map? result)
             (not-any? #(get result %) contradiction-keys))
        complete-read-back?
        (and (map? read-back-hashes)
             (seq read-back-hashes)
             (every? (fn [[path sha]]
                       (and (nonblank-string? path)
                            (sha256? sha)))
                     read-back-hashes))
        complete-verification-evidence?
        (and (map? verification)
             (true? (:ok verification))
             (= "exact" (:profile verification))
             (= :project (:profile-source verification))
             (sha256? (:profile-sha256 verification))
             (= :exact-exit (:acceptance verification))
             (= :pass (:process-outcome verification))
             (number? (:exit verification))
             (zero? (:exit verification))
             (nonblank-string? (:cwd verification))
             (vector? (:argv verification))
             (seq (:argv verification))
             (every? nonblank-string? (:argv verification))
             (number? (:elapsed_ms verification))
             (<= 0 (:elapsed_ms verification))
             (integer? (:output-bytes verification))
             (<= 0 (:output-bytes verification))
             (sha256? (:output-sha256 verification))
             (contains? #{true false} (:output-truncated verification)))]
    (when (and (map? result)
               (= "apply_clojure_changes" (:operation result))
               no-contradictory-outcome?
               (true? (:ok result))
               (true? (:committed result))
               (true? (:verification_complete result))
               (= "none" (:next_action result))
               complete-read-back?
               (nonblank-string? (:undo_receipt result))
               (sha256? (:receipt_hash result))
               complete-verification-evidence?)
      exact-terminal-response-text)))

(defn- with-exact-terminal-response
  [result]
  (if-let [response (exact-terminal-response result)]
    (assoc result :terminal_response response)
    result))

(def ^:private runtime-config runtime/tool-config)

(defn init!
  "Set the live tool configuration. Passing nil disarms the handler."
  [config]
  (prepared-confirmation/reset-registry!)
  (combinable/reset-registry!)
  (let [configured (when config
                     (assoc config :workspace-router
                            (workspace/router config)))]
    (reset! runtime-config configured)
    (inspect-tool/init! configured)
    (program-tool/init! configured)
    (census-tool/init! configured)))

(defn- real-root
  ^Path [root]
  (mcp-paths/real-root root))

(defn- resolve-source-path
  [^Path root relative]
  (mcp-paths/resolve-source-path root relative))

;; @spec MCP-OP-EDIT-031
(defn- resolve-created-paths
  "Confine every create_files target to one absent path inside the real root.

  This is where creation's lexical, escape, and non-existence guards live, so
  each refusal names the offending project-relative path itself."
  [^Path root create-files]
  (loop [remaining create-files
         resolved []]
    (if-let [{:keys [file content]} (first remaining)]
      (let [target (mcp-paths/resolve-new-source-path root file)]
        (if-not (:ok target)
          (assoc target :ok false :source-unchanged true :raw-path file)
          (recur (next remaining)
                 (conj resolved
                       {:file (:path target)
                        :content content
                        :relative-file file
                        :workspace-root (.toString root)
                        :directories (mapv str
                                           (:missing-parent-directories target))}))))
      {:ok true :create-files resolved})))

(defn- resolve-transaction-paths
  [project-root spec]
  (loop [changes (:changes spec)
         resolved []
         path-facts []]
    (if-let [change (first changes)]
      (let [raw-paths (:in change)
            paths (mapv #(resolve-source-path project-root %) raw-paths)
            refusal (first (remove :ok paths))]
        (if refusal
          (let [index (.indexOf paths refusal)]
            (assoc refusal :raw-path (nth raw-paths index)))
          (recur (next changes)
                 (conj resolved
                       (assoc change :in (mapv :path paths)))
                 (into path-facts
                       (map (fn [raw path]
                              {:raw raw :path (:path path)})
                            raw-paths paths)))))
      (let [creations (when (seq (:create-files spec))
                        (resolve-created-paths project-root
                                               (:create-files spec)))]
        (if (and creations (not (:ok creations)))
          creations
          {:ok true
           :spec (cond-> (assoc spec :changes resolved)
                   creations
                   (assoc :create-files
                          (mapv #(dissoc % :relative-file)
                                (:create-files creations))))
           :path-facts path-facts})))))

(defn- resolve-extraction-paths
  [root {:keys [file to caller-changes ignored-caller-files expect] :as request}]
  (let [source (mcp-paths/resolve-source-path root file)
        target (mcp-paths/resolve-new-source-path root to)
        caller-spec (contract/tool-params->transaction
                      {:changes caller-changes
                       :expect {:changes (count caller-changes)
                                :edits (:caller-edits expect)
                                :files (count (distinct (mapcat :files caller-changes)))}})
        callers (resolve-transaction-paths root caller-spec)
        ignored (mapv #(mcp-paths/resolve-source-path root %)
                      ignored-caller-files)
        refusal (first (remove :ok (concat [source target callers] ignored)))]
    (if refusal
      refusal
      {:ok true
       :extraction
       (assoc request
              :file (:path source)
              :to (:path target)
              :request-file file
              :request-to to
              :created-directories
              (mapv str (:missing-parent-directories target))
              :caller-changes (get-in callers [:spec :changes])
              :ignored-caller-files (mapv :path ignored))})))

(defn- publicize-extraction-decision-refusal
  [root sources request result]
  (if-not (= :extraction-decisions-required (:error-type result))
    result
    (let [relative-paths (workspace-sources/relative-paths root sources)
          public-path #(get relative-paths % %)
          public-reference (fn [reference]
                             (cond-> reference
                               (:file reference) (update :file public-path)))
          completed-plan
          (-> (:completed-plan result)
              (assoc :file (:request-file request)
                     :to (:request-to request))
              (update :callers-to-review #(mapv public-path %))
              (update :quoted-var-references
                      #(mapv public-reference %)))
          genuine-unknowns
          (mapv #(update % :file public-path) (:genuine-unknowns result))]
      (assoc result
             :completed-plan completed-plan
             :genuine-unknowns genuine-unknowns
             :next-call
             {:workspace-root (.toString root)
              :extraction
              {:file (:request-file request)
               :to (:request-to request)
               :forms (:forms request)
               :public-forms (:required-public-forms completed-plan)
               :require-policy (name (:require-policy request))
               :source-hash (:source-hash completed-plan)
               :caller-changes []
               :ignored-caller-files []}}
             :next-action "fill_caller_decisions_then_apply_once"))))

(defn- execute-extraction!
  [config root request receipt verify]
  ;; @spec MCP-OP-VERIFY-001
  ;; @spec MCP-OP-VERIFY-005
  ;; @spec MCP-OP-VERIFY-006
  ;; @spec MCP-OP-VERIFY-007
  ;; @spec MCP-OP-VERIFY-008
  ;; @spec MCP-OP-VERIFY-009
  ;; @spec MCP-OP-VERIFY-010
  (let [exact-profile (when (= "exact" verify)
                        (change-buffer/compile-exact-profile
                          verify (:verification-profiles config)
                          (:verification-profile-source config)))
        sources (workspace-sources/read-all root)
        request (assoc request
                       :source (get sources (:file request))
                       :target-ns (extract/file-path->ns-name
                                    (:to request) ["src" "test" "dev"])
                       :workspace-sources sources)
        compiled (->> (extraction/compile-extraction request)
                      (publicize-extraction-decision-refusal
                        root sources request))
        compiled
        (if (and (:ok compiled) (:formatter config))
          (let [format! (or (:format-candidates! config)
                            formatter/format-candidates!)
                format-sources (select-keys (:future-sources compiled)
                                            (:created-files compiled))
                formatted (format! (.toString root) (:formatter config)
                                   format-sources)]
            (if (:ok formatted)
              (let [future-sources (merge (:future-sources compiled)
                                          (:future-sources formatted))
                    prepared (extraction/with-future-sources
                               compiled future-sources)]
                (if (:ok prepared)
                  (assoc prepared :format
                         (dissoc formatted :future-sources :ok))
                  prepared))
              formatted))
          compiled)]
    (if-not (:ok compiled)
      compiled
      (let [project-root (.toString root)
            original-files (vec (keys (:original-sources compiled)))
            future-files (vec (keys (:future-sources compiled)))
            baseline (when (and verify (nil? exact-profile))
                       (cond
                         (:capture-verification-baseline! config)
                         ((:capture-verification-baseline! config)
                          project-root verify (:verification-profiles config)
                          original-files)

                         (nil? (:verify! config))
                         (change-buffer/capture-verification-baseline!
                           project-root verify (:verification-profiles config)
                           original-files)))]
        (if (or (and exact-profile (not (:ok exact-profile)))
                (and baseline (not (:ok baseline))))
          {:error (if exact-profile
                    "Exact project verification profile is unavailable"
                    "Verification baseline capture failed before extraction")
           :error-type (or (:error-type exact-profile)
                           (:error-type baseline)
                           :verification-baseline-failed)
           :verification (or exact-profile baseline)
           :source-unchanged true}
          (let [result (extraction/commit! compiled)]
            (if-not (:ok result)
              result
              (try
                (file-ops/atomic-write! receipt (pr-str (:receipt result)))
                (let [result (assoc result :receipt-file receipt)
                      verification
                      (when verify
                        (cond
                          exact-profile
                          (change-buffer/run-exact-verification!
                            project-root exact-profile)

                          (:verify! config)
                          ((:verify! config) project-root verify
                                             (:verification-profiles config) future-files)

                          :else
                          (change-buffer/run-verification!
                            project-root verify (:verification-profiles config)
                            future-files baseline)))]
                  (if (or (nil? verification) (:ok verification))
                    (do
                      (cold-verify/attach-undo-from-verification!
                        project-root verification (:receipt-file result) (:receipt-hash result))
                      (cond-> result verification (assoc :verification verification)))
                    (let [rollback (extraction/undo! (:receipt result))
                          rolled-back (boolean (:ok rollback))]
                      (when rolled-back (.delete (io/file receipt)))
                      {:error (if (= :verification-unverified
                                     (:error-type verification))
                                "Verification authority was unverified; extraction was rolled back"
                                "Verification failed; extraction was rolled back")
                       :error-type (or (:error-type verification)
                                       :verification-failed)
                       :verification verification
                       :rolled-back rolled-back
                       :recovery rollback
                       :source-unchanged rolled-back})))
                (catch Exception error
                  (let [rollback (extraction/undo! (:receipt result))]
                    {:error "Could not publish extraction receipt"
                     :error-type :receipt-publish-failed
                     :cause-error (.getMessage error)
                     :rolled-back (boolean (:ok rollback))
                     :recovery rollback
                     :source-unchanged (boolean (:ok rollback))}))))))))))

(defn- default-receipt-dir
  [project-root]
  (workspace/receipt-dir project-root))

(defn- delete-empty-dir!
  [directory created?]
  (when created?
    (try
      (Files/deleteIfExists (.toPath (io/file directory)))
      (catch Exception _ nil))))

(defn- elapsed-ms
  [started-ns]
  (/ (double (- (System/nanoTime) started-ns)) 1000000.0))

(defn- timed
  [f]
  (let [started (System/nanoTime)
        result (f)]
    [result (elapsed-ms started)]))

(defn- record-result!
  [telemetry-state request response total-start timings]
  (when telemetry-state
    (telemetry/record-call!
      telemetry-state request response
      (assoc timings :total_ms (elapsed-ms total-start))))
  response)

(defn- resolve-program-paths
  [root programs]
  (loop [remaining programs
         resolved-programs []]
    (if-let [program (first remaining)]
      (let [resolved (mcp-paths/resolve-source-path root (:file program))]
        (if-not (:ok resolved)
          (assoc resolved :ok false :source-unchanged true)
          (recur (next remaining)
                 (conj resolved-programs
                       (assoc program
                              :relative-file (:file program)
                              :file (.toString (:path resolved)))))))
      {:ok true :programs resolved-programs})))

(defn- compiled-addressed-edits
  [compiled]
  (mapcat :edits (:files compiled)))

(defn- merge-programs-into-compiled
  [compiled programs]
  (let [sources
        (reduce
          (fn [current {:keys [file]}]
            (if (contains? current file)
              current
              (assoc current file (slurp file))))
          (:original-sources compiled)
          programs)
        program-result (program-tool/compile-programs sources programs)]
    (if-not (:ok program-result)
      program-result
      (let [raw-edits (concat (compiled-addressed-edits compiled)
                              (compiled-addressed-edits
                                (:compiled program-result)))
            edits (mapv (fn [index edit]
                          (-> edit
                              (assoc :id (str "hybrid/" (inc index)))
                              (dissoc :intent-index)))
                        (range) raw-edits)
            combined (transaction/compile-addressed-transaction
                       sources edits)]
        (if-not (:ok combined)
          (assoc combined :ok false :source-unchanged true)
          (assoc combined
                 :program-count (:program-count program-result)
                 :program-edit-count (:edit-count program-result)
                 :program-changed-characters
                 (:changed-characters program-result)))))))

(defn- resolve-spec-from-path-map [spec path-map]
  (try
    {:ok true
     :spec
     (update spec :changes
             (fn [changes]
               (mapv (fn [change]
                       (update change :in
                               (fn [paths]
                                 (mapv (fn [path]
                                         (or (get path-map path)
                                             (throw
                                               (ex-info
                                                 "Final relation path widened beyond the captured universe"
                                                 {:path path}))))
                                       paths))))
                     changes)))}
    (catch clojure.lang.ExceptionInfo error
      {:error (.getMessage error)
       :error-type :compact-relation-path-conflict
       :failed-stage :path-resolution
       :path (:path (ex-data error))
       :mutation-attempted false
       :write-authority false
       :source-unchanged true
       :next-action "correct_request"})))

(defn- prepare-relation-spec
  [root sources relation-plan path-map]
  (let [raw-sources
        (into {}
              (map (fn [[raw canonical]] [raw (get sources canonical)]))
              path-map)
        frozen (compact-relations/compile-frozen raw-sources relation-plan)]
    (if-not (:ok frozen)
      frozen
      (let [validated (contract/validate-tool-params (:request frozen))]
        (if-not (:ok validated)
          validated
          (let [resolved
                (resolve-spec-from-path-map
                  (contract/tool-params->transaction (:params validated))
                  path-map)]
            (if-not (:ok resolved)
              resolved
              (let [canonical-files
                    (mapv (fn [raw]
                            (str (.relativize
                                   root
                                   (.toPath (io/file (get path-map raw))))))
                          (:relation-files relation-plan))
                    prepared
                    (compact-location/normalize-spec
                      sources (:spec resolved)
                      (:compact-location-normalization validated))]
                (cond-> prepared
                  (not (:error prepared))
                  (assoc :compact-relation-normalization
                         (assoc (:relation-normalization frozen)
                                :files canonical-files)))))))))))

(defn- execute-explicit-change!
  ;; @spec OP-ALG-MCP-001
  ;; @spec MCP-OP-EDIT-030
  [config root resolved receipt verify compact-location-plan relation-plan
   compact-effect-identity? public-operation]
  (let [exact-profile (when (= "exact" verify)
                        (change-buffer/compile-exact-profile
                          verify (:verification-profiles config)
                          (:verification-profile-source config)))
        files (->> (get-in resolved [:spec :changes])
                   (mapcat :in)
                   distinct
                   vec)
        project-root (.toString root)
        baseline (when (and verify (nil? exact-profile))
                   (cond
                     (:capture-verification-baseline! config)
                     ((:capture-verification-baseline! config)
                      project-root verify (:verification-profiles config) files)

                     (nil? (:verify! config))
                     (change-buffer/capture-verification-baseline!
                       project-root verify (:verification-profiles config) files)))
        baseline-refusal? (or (and exact-profile (not (:ok exact-profile)))
                              (and baseline (not (:ok baseline))))]
    (if baseline-refusal?
      {:error (if exact-profile
                "Exact project verification profile is unavailable"
                "Verification baseline capture failed before the direct transaction")
       :error-type (or (:error-type exact-profile)
                       (:error-type baseline)
                       :verification-baseline-failed)
       :verification (or exact-profile baseline)
       :source-unchanged true}
      (let [base-prepare! (:prepare-compiled! config)
            programs (:programs resolved)
            relation-evidence (atom nil)
            base-prepare-compiled!
            (cond
              (seq programs)
              (fn [project-root compiled]
                (let [with-programs
                      (merge-programs-into-compiled compiled programs)]
                  (if (and (:ok with-programs) base-prepare!)
                    (base-prepare! project-root with-programs)
                    with-programs)))

              :else base-prepare!)
            prepare-compiled!
            (if compact-effect-identity?
              (fn [project-root compiled]
                (let [prepared (if base-prepare-compiled!
                                 (base-prepare-compiled! project-root compiled)
                                 compiled)]
                  (if (:error prepared)
                    prepared
                    (assoc prepared
                           :canonical-effect-identity
                           (transaction/canonical-effect-identity
                             project-root prepared)))))
              base-prepare-compiled!)
            relation-prepare
            (when relation-plan
              (fn [sources _spec]
                (let [prepared
                      (prepare-relation-spec
                        root sources relation-plan
                        (:relation-path-map resolved))]
                  (when-let [evidence (:compact-relation-normalization prepared)]
                    (reset! relation-evidence evidence))
                  prepared)))
            result (transaction/execute-mcp-change!
                     (cond-> {:spec (:spec resolved)
                              :receipt-out receipt
                              :write-refusal-context
                              {:operation public-operation
                               :project-root project-root}}
                       prepare-compiled!
                       (assoc :prepare-compiled!
                              #(prepare-compiled! project-root %))

                       relation-prepare
                       (assoc :prepare-spec relation-prepare)

                       (and compact-location-plan (nil? relation-prepare))
                       (assoc :prepare-spec
                              #(compact-location/normalize-spec %1 %2 compact-location-plan))))
            result (cond-> result
                     @relation-evidence
                     (assoc :compact-relation-normalization @relation-evidence))]
        (if (or (:error result) (nil? verify))
          result
          (let [verification (cond
                               exact-profile
                               (change-buffer/run-exact-verification!
                                 project-root exact-profile)

                               (:verify! config)
                               ((:verify! config) project-root verify
                                                  (:verification-profiles config) files)

                               :else
                               (change-buffer/run-verification!
                                 project-root verify
                                 (:verification-profiles config) files baseline))]
            (if (:ok verification)
              (do
                (cold-verify/attach-undo-from-verification!
                  project-root verification (:receipt-file result) (:receipt-hash result))
                (assoc result :verification verification))
              (let [rollback (transaction/execute-undo!
                               {:receipt (:receipt-file result)})
                    rolled-back? (boolean (:ok rollback))
                    hot-rollback (when rolled-back?
                                   (change-buffer/reload-after-rollback!
                                     project-root verify
                                     (:verification-profiles config)))]
                (when rolled-back?
                  (.delete (io/file (:receipt-file result))))
                {:error (if (= :verification-unverified
                               (:error-type verification))
                          "Verification authority was unverified; the direct transaction was rolled back"
                          "Verification failed; the direct transaction was rolled back")
                 :error-type (or (:error-type verification)
                                 :verification-failed)
                 :verification verification
                 :rolled-back rolled-back?
                 :hot-rollback hot-rollback
                 :recovery rollback
                 :source-unchanged rolled-back?}))))))))

(defn- execute-request-in-context!
  "Validate, confine, and execute one typed request through the loaded kernel."
  [{:keys [project-root receipt-dir telemetry] :as config} params
   public-operation]
  (let [normalized-params (json/parse-string (json/generate-string params) true)
        editor-gesture? (some #(contains? normalized-params %)
                              [:edits :programs :delete_owners :create_files
                               :symbol_migration :require_change])
        ;; @spec MCP-OP-EDIT-033
        compact-effect-identity?
        (and (not (contains? normalized-params :programs))
             (not (contains? normalized-params :create_files))
             (some #(contains? normalized-params %)
                   [:edits :delete_owners :symbol_migration :require_change]))
        config (cond
                 (:verification-profile-selection-fn config)
                 (let [{:keys [profiles source]}
                       ((:verification-profile-selection-fn config))]
                   (assoc config
                          :verification-profiles profiles
                          :verification-profile-source source))

                 (:verification-profiles-fn config)
                 (assoc config
                        :verification-profiles
                        ((:verification-profiles-fn config)))

                 :else config)
        config (cond
                 (:formatter-fn config)
                 (assoc config :formatter ((:formatter-fn config)))

                 ;; Managed workspace contexts that predate formatter-fn must
                 ;; adopt the hot-loaded default without a server restart.
                 (or (:verification-profile-selection-fn config)
                     (:verification-profiles-fn config))
                 (assoc config :formatter formatter/default-command)

                 :else config)
        config (if (and (:formatter config) (not editor-gesture?))
                 (let [command (:formatter config)]
                   (assoc config
                          :verification-profiles
                          (formatter/verification-profiles-after-format
                            (:verification-profiles config) command)
                          :prepare-compiled!
                          (fn [project-root compiled]
                            (let [format! (or (:format-candidates! config)
                                              formatter/format-candidates!)
                                  formatted (format! project-root command
                                                     (:future-sources compiled))]
                              (if (:ok formatted)
                                (let [prepared (transaction/with-future-sources
                                                 compiled
                                                 (:future-sources formatted))]
                                  (if (:ok prepared)
                                    (assoc prepared :format
                                           (dissoc formatted
                                                   :future-sources :ok))
                                    prepared))
                                formatted)))))
                 config)
        basis? (string? (:basis normalized-params))
        extraction? (map? (:extraction normalized-params))
        total-start (System/nanoTime)
        [validated validation-ms]
        (timed #(if basis?
                  (change-buffer/validate-basis-request normalized-params)
                  (contract/validate-tool-params params)))]
    (if basis?
      (record-result!
        telemetry params
        (if (:ok validated)
          (change-buffer/apply-basis! config normalized-params)
          validated)
        total-start {:validation_ms validation-ms})
      (if-not (:ok validated)
        (record-result! telemetry params (contract/normalize-refusal validated)
                        total-start {:validation_ms validation-ms})
        (try
          (let [[prepared confinement-ms]
                (timed
                  #(let [root (real-root project-root)
                         resolved
                         (if extraction?
                           (resolve-extraction-paths
                             root (get-in validated [:params :extraction]))
                           (resolve-transaction-paths
                             root
                             (contract/tool-params->transaction
                               (:params validated))))
                         resolved
                         (if-let [relation-plan (:compact-relation-plan validated)]
                           (compact-relations/validate-path-resolution
                             relation-plan resolved)
                           resolved)
                         programs (get-in validated [:params :programs])]
                     {:root root
                      :resolved
                      (if (and (:ok resolved) (seq programs))
                        (let [program-paths
                              (resolve-program-paths root programs)]
                          (if (:ok program-paths)
                            (assoc resolved :programs
                                   (:programs program-paths))
                            program-paths))
                        resolved)}))
                {:keys [root resolved]} prepared]
            (if-not (:ok resolved)
              (record-result! telemetry params
                              (contract/normalize-refusal resolved)
                              total-start
                              {:validation_ms validation-ms
                               :confinement_ms confinement-ms})
              (let [directory (str (or receipt-dir (default-receipt-dir project-root)))
                    directory-file (io/file directory)
                    existed? (.exists directory-file)
                    _ (.mkdirs directory-file)
                    receipt (str (io/file directory
                                          (str (UUID/randomUUID) ".edn")))
                    [result kernel-ms]
                    (timed #(if extraction?
                              (execute-extraction!
                                config root (:extraction resolved) receipt
                                (get-in validated [:params :verify]))
                              (execute-explicit-change!
                                config root resolved receipt
                                (get-in validated [:params :verify])
                                (:compact-location-normalization validated)
                                (:compact-relation-plan validated)
                                compact-effect-identity?
                                public-operation)))
                    classified (cond->
                                 (contract/classify-kernel-result
                                   (.toString root) result)
                                 (:compact-field-normalization validated)
                                 (assoc :compact_field_normalization
                                        (:compact-field-normalization validated))
                                 (:input-normalization validated)
                                 (assoc :input_normalization
                                        (:input-normalization validated))
                                 (contains? normalized-params :create_files)
                                 (assoc :canonical_effect_identity_suppressed_reason
                                        "create-files-present"))]
                (when-not (:ok classified)
                  (delete-empty-dir! directory (not existed?)))
                (record-result! telemetry params classified total-start
                                {:validation_ms validation-ms
                                 :confinement_ms confinement-ms
                                 :kernel_ms kernel-ms}))))
          (catch Exception error
            (record-result!
              telemetry params
              {:ok false
               :error_type "mcp-adapter-failure"
               :error (.getMessage error)
               :source_unchanged true
               :remedy "Correct the project root or request and call apply_clojure_changes once."}
              total-start {:validation_ms validation-ms})))))))

(defn execute-request!
  "Route one request to a canonical workspace context, then execute it."
  [config params]
  (let [public-operation (or (:public-operation config)
                             "apply_clojure_changes")
        normalized (json/parse-string (json/generate-string params) true)
        explicit-root? (contains? normalized :workspace_root)]
    (if-not explicit-root?
      (let [result (execute-request-in-context!
                     config normalized public-operation)
            resolved (workspace/canonical-root (:project-root config))]
        (cond-> result
          (:ok resolved) (assoc :workspace_root (:workspace-root resolved))))
      (let [workspace-router (or (:workspace-router config)
                                 (workspace/router config))
            routed (workspace/resolve-request workspace-router normalized)]
        (if-not (:ok routed)
          routed
          (assoc (execute-request-in-context!
                   (:config routed) (:params routed) public-operation)
                 :workspace_root (:workspace-root routed)))))))

(defn concise-summary
  "Render compact visible content; the full receipt remains structuredContent."
  [result]
  (if (:ok result)
    (let [operation (or (:operation result) "apply_clojure_changes")
          caller-proof (get-in result [:caller_proof :level])
          caller-proof-line
          (case caller-proof
            "semantic-complete"
            "\n✓ caller proof · semantic complete for one hash-bound session"

            "structural-candidates-only"
            "\n⚠ caller proof · structural candidates only; not semantic completeness"

            "caller-proof-unavailable"
            "\n⚠ caller proof unavailable · absence cannot authorize deletion"

            "")
          terminal-response-line
          (when (string? (:terminal_response result))
            (str "\n→ If this mutation completes all remaining work, return exactly: "
                 (:terminal_response result)
                 "\n  If work remains, continue."))]
      (if (= "edit_clojure-preview" operation)
        (format (str operation "\n"
                     "  %s changed files · %s changed characters · %s\n\n"
                     "✓ complete bounded diff compiled\n"
                     "✓ source unchanged · no write authority\n"
                     "✓ lifecycle preview · next action none")
                (:changed_files result)
                (:changed_characters result)
                (mcp-operation/format-elapsed-ms (:elapsed_ms result)))
        (if (:verification_complete result)
          (format (str operation "\n"
                       "  %s edits · %s files · %s\n\n"
                       "✓ atomic commit complete\n"
                       "✓ written bytes read back and verified"
                       caller-proof-line "\n"
                       "✓ terminal evidence · verification_complete=true · next action none"
                       terminal-response-line)
                  (or (:edits result) (:match-count result) 0)
                  (or (:files result) (:changed-file-count result) 0)
                  (mcp-operation/format-elapsed-ms (:elapsed_ms result)))
          (format (str operation "\n"
                       "  %s edits · %s files · %s\n\n"
                       "✓ atomic commit complete\n"
                       "✓ written bytes read back and hot proof complete"
                       caller-proof-line "\n"
                       "… cold verification running · edit remains committed\n"
                       "→ copy next_call to inspect_clojure after doing other useful work")
                  (or (:edits result) (:match-count result) 0)
                  (or (:files result) (:changed-file-count result) 0)
                  (mcp-operation/format-elapsed-ms (:elapsed_ms result))))))
    (let [operation (or (:operation result) "apply_clojure_changes")
          reason (or (:reason result) (:error-type result)
                     (:error_type result) "unknown-error")
          reason (if (keyword? reason) (name reason) reason)
          path (or (:path result) (:error-path result) (:error_path result))
          change-index (or (:change-index result) (:change_index result))
          change-id (or (:change-id result) (:change_id result))
          field (:field result)
          change-line (when (or (some? change-index) change-id field)
                        (format "  change %s%s%s\n"
                                (if (some? change-index) change-index "unknown")
                                (if change-id (str " · " change-id) "")
                                (if field (str " · field " field) "")))
          source-safe? (or (:source-unchanged result)
                           (:source_unchanged result)
                           (:rolled-back result))]
      (format (str operation "\n"
                   "  refused · %s%s · %s\n"
                   "%s\n"
                   "%s\n"
                   "→ %s")
              reason
              (if path (str " at " (pr-str path)) "")
              (mcp-operation/format-elapsed-ms (:elapsed_ms result))
              (or change-line "")
              (if source-safe?
                "✓ source unchanged"
                "⚠ source state requires structured receipt review")
              (or (:remedy result) (:next_action result)
                  "Correct the request and retry once.")))))

(defn- prepared-confirmation-request?
  [params]
  (some #(or (contains? params %) (contains? params (name %)))
        [:confirm :fill :preview]))

(defn- read-confirmation-snapshot
  [root config expected-hashes]
  (let [read-source (or (:read-source config) slurp)]
    (loop [files (sort (keys expected-hashes))
           relative-sources (sorted-map)
           resolved-sources {}
           actual-hashes (sorted-map)
           path-facts []]
      (if-let [file (first files)]
        (let [resolved (resolve-source-path root file)]
          (if-not (:ok resolved)
            resolved
            (let [path (:path resolved)
                  source (try
                           (read-source path)
                           (catch Exception _ nil))]
              (if-not (string? source)
                {:ok true
                 :relative-sources relative-sources
                 :resolved-sources resolved-sources
                 :actual-hashes actual-hashes
                 :path-facts path-facts}
                (recur (next files)
                       (assoc relative-sources file source)
                       (assoc resolved-sources path source)
                       (assoc actual-hashes file
                              (structural-lens/source-hash source))
                       (conj path-facts {:raw file :path path}))))))
        {:ok true
         :relative-sources relative-sources
         :resolved-sources resolved-sources
         :actual-hashes actual-hashes
         :path-facts path-facts}))))

(defn- preview-compiled-confirmation
  [root validated snapshot descriptor-sha fill]
  ;; @spec MCP-OP-PREP-ACT-009
  ;; @spec MCP-OP-PREP-ACT-010
  ;; @spec MCP-OP-PREP-ACT-011
  ;; @spec MCP-OP-PREP-ACT-012
  (let [spec (contract/tool-params->transaction (:params validated))
        resolved (resolve-transaction-paths root spec)]
    (if-not (:ok resolved)
      (contract/normalize-refusal resolved)
      (let [compiled (transaction/compile-transaction
                       (:resolved-sources snapshot) (:spec resolved))]
        (if (:error compiled)
          (contract/normalize-refusal compiled)
          (let [future-sources
                (into (sorted-map)
                      (map (fn [{:keys [raw path]}]
                             [raw (get (:future-sources compiled) path)]))
                      (:path-facts snapshot))
                result
                (prepared-confirmation/preview-result
                  {:descriptor-sha256 descriptor-sha
                   :fill fill
                   :snapshot-guards (:actual-hashes snapshot)
                   :sources (:relative-sources snapshot)
                   :future-sources future-sources})]
            (prepared-confirmation/enforce-preview-bounds
              result
              (fn [candidate]
                (let [normalized (assoc candidate :elapsed_ms 0.0)]
                  (inspect-tool/mcp-result-byte-count
                    (concise-summary normalized) normalized))))))))))

(defn- execute-prepared-confirmation!
  [config exchange params]
  ;; @spec MCP-OP-PREP-ACT-005
  ;; @spec MCP-OP-PREP-ACT-006
  ;; @spec MCP-OP-PREP-ACT-007
  ;; @spec MCP-OP-PREP-ACT-008
  ;; @spec MCP-OP-PREP-ACT-013
  (let [shape (prepared-confirmation/validate-confirm-request params)]
    (if-not (:ok shape)
      shape
      (let [session-key (prepared-confirmation/exchange-session-key exchange)
            digest (:confirm shape)
            entry (prepared-confirmation/lookup!
                    prepared-confirmation/process-registry session-key digest)]
        (if-not (:ok entry)
          entry
          (let [holes (prepared-confirmation/validate-holes
                        (:caller-holes entry) (:fill shape))]
            (if-not (:ok holes)
              holes
              (let [arguments
                    (prepared-confirmation/reconstruct-arguments
                      (:descriptor entry) (:fill shape))
                    workspace-router (or (:workspace-router config)
                                         (workspace/router config))
                    routed (workspace/resolve-request workspace-router arguments)]
                (if-not (:ok routed)
                  routed
                  (let [root (real-root (:workspace-root entry))
                        snapshot (read-confirmation-snapshot
                                   root (:config routed) (:file-hashes entry))]
                    (if-not (:ok snapshot)
                      snapshot
                      (let [snapshot-check
                            (prepared-confirmation/validate-snapshot
                              (:file-hashes entry) (:actual-hashes snapshot))]
                        (if-not (:ok snapshot-check)
                          (do
                            (prepared-confirmation/expire!
                              prepared-confirmation/process-registry
                              session-key digest)
                            snapshot-check)
                          (let [validated (contract/validate-tool-params
                                            (:params routed))]
                            (if-not (:ok validated)
                              (contract/normalize-refusal validated)
                              (if (:preview shape)
                                (let [preview-use
                                      (prepared-confirmation/use-preview!
                                        prepared-confirmation/process-registry
                                        session-key digest)]
                                  (if-not (:ok preview-use)
                                    preview-use
                                    (preview-compiled-confirmation
                                      root validated snapshot digest
                                      (:fill shape))))
                                (let [consumed
                                      (prepared-confirmation/consume!
                                        prepared-confirmation/process-registry
                                        session-key digest)]
                                  (if-not (:ok consumed)
                                    consumed
                                    (execute-request!
                                      (-> config
                                          (dissoc :telemetry-state)
                                          (assoc :public-operation
                                                 "edit_clojure"))
                                      arguments)))))))))))))))))))

(defn request-operation
  "Name the public operation from one JSON- or Clojure-shaped request."
  [params]
  (if (some (fn [field]
              (or (contains? params field)
                  (contains? params (name field))))
            [:edits :programs :delete_owners :create_files])
    "edit_clojure"
    "apply_clojure_changes"))

(defn- handle-operation
  [operation exchange params callback]
  (mcp-operation/invoke!
    {:execute
     (fn []
       (write-refusal/bound-public-refusal
         (with-exact-terminal-response
           (let [result
                 (cond
                   (and (= "edit_clojure" operation)
                        @runtime-config
                        (prepared-confirmation-request? params)
                        (prepared-confirmation/exchange-session-key exchange))
                   (execute-prepared-confirmation! @runtime-config exchange params)

                   (and (= "edit_clojure" operation)
                        (or (contains? params :verify)
                            (contains? params "verify")))
                   {:ok false
                    :error_type "invalid-mcp-request"
                    :error "edit_clojure does not authorize transaction verification"
                    :source_unchanged true
                    :mutation_attempted false
                    :write_authority false
                    :remedy "Use apply_clojure_changes when verification must share rollback authority."}

                   @runtime-config
                   (execute-request!
                     (assoc @runtime-config :public-operation operation)
                     params)

                   :else
                   {:ok false
                    :error_type "server-not-initialized"
                    :error (str operation " server is not initialized")
                    :source_unchanged true
                    :remedy "Restart the configured clj-surgeon MCP server."})]
             (cond-> (if (= "edit_clojure-preview" (:operation result))
                       result
                       (assoc result :operation operation))
               ;; @spec MCP-OP-EDIT-032
               (= "edit_clojure" operation)
               (->> (combinable/attach-note!
                      combinable/process-registry
                      (prepared-confirmation/exchange-session-key exchange)
                      params)))))
         concise-summary))
     :summarize concise-summary
     :callback callback}))

(defn handle-clj-change
  "Legacy inferred callback retained for compatibility with installed callers."
  [exchange params callback]
  (handle-operation (request-operation params) exchange params callback))

;; @spec MCP-OP-PREP-REQ-007
(defn handle-edit-clojure
  "Stable callback that preserves edit_clojure entrance authority and identity."
  [exchange params callback]
  (handle-operation "edit_clojure" exchange params callback))

(defn handle-apply-clojure-changes
  "Stable callback that preserves apply_clojure_changes entrance authority and identity."
  [exchange params callback]
  (handle-operation "apply_clojure_changes" exchange params callback))

(def edit-tool-description
  (str
    "Commit one atomic Clojure edit transaction with no preflight read when the "
    "decision is complete. edits are exact literal replacements guarded by the "
    "exact value pair from/to. The exact aliases old/new and before/after are "
    "also accepted and lowered to from/to; supply exactly one complete pair. The "
    "exact old subtree uses file with within {form}, {namespace:true} for the "
    "file's unique ns form, or {namespace:name} for an explicitly named ns. Use "
    "explicit files with within {root:true} for one grouped "
    "Clojure/EDN edit. matches defaults to one and is enforced in every file. Optional programs are "
    "independent computed relations: file, an expression ending in transform, "
    "and expect {matches, max_changed_characters}. delete_owners groups exact "
    "named top-level forms by file and removes them without source bodies. "
    "create_files creates absent .clj, .cljs, .cljc, or .edn files in the same "
    "transaction from exact {file, content} pairs. The target must not exist, "
    "the content must parse, it is written verbatim, and undo deletes it. A "
    "create-only transaction is legal. Start a program with "
    "(form 'owner) for one owner or [] for the whole file. All edits, programs, and deletions "
    "compile against the same original snapshot; none observes another's output. "
    "Any stale count, overlap, budget, comment-bearing computed selection, parse, "
    "or write failure refuses or rolls back the whole batch. Exact spelling and "
    "comments belong in edits; computed values belong in programs. Success returns "
    "terminal read-back and undo evidence. For a repeated exact symbol migration "
    "across named owners, use paired symbol_migration and require_change. Group "
    "[owner, from, matches] rows by file; preserve-name changes only the qualifier. "
    "Declare the exact target lib/alias and per-file old lib/alias removals. Surgeon "
    "discovers or chooses none of these authoritative values. A served "
    "prepared_confirmation may instead be submitted as {confirm, fill} on the "
    "same MCP session; fill must contain every and only the served caller_holes. "
    "Add literal preview=true to compile one complete bounded inert diff without "
    "consuming commit authority. Preview hashes, diffs, future hashes, and result "
    "objects are never commit inputs; commit repeats {confirm, fill} without "
    "preview and consumes the confirmation before the ordinary transaction."))

(def edit-clojure-tool
  {:id :edit-clojure
   :name "edit_clojure"
   :description edit-tool-description
   :schema mcp-schema/editor-tool-schema
   :output-schema clj-change-output-schema
   :structured? true
   :tool-fn #'handle-edit-clojure})

(def clj-change-tool
  {:id :clj-change
   :name "apply_clojure_changes"
   :description tool-description
   :schema mcp-schema/clj-change-schema
   :output-schema clj-change-output-schema
   :structured? true
   :tool-fn #'handle-apply-clojure-changes})

(defn tools-for-profile
  "Return the exact public tool catalog for one startup profile."
  [profile]
  (case (or profile :full)
    :full [inspect-tool/inspect-tool
           clj-change-tool
           edit-clojure-tool
           program-tool/transform-clojure-tool
           census-tool/relation-census-tool]
    :edit [edit-clojure-tool]
    (throw (ex-info "Unsupported MCP tool profile"
                    {:profile profile
                     :supported [:full :edit]}))))

(defn all-tools
  []
  (tools-for-profile (:tool-profile @runtime-config)))
