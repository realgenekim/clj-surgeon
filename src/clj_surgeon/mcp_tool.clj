(ns clj-surgeon.mcp-tool
  (:require
   [cheshire.core :as json]
   [clj-surgeon.extract :as extract]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-admit-tool :as admit-tool]
   [clj-surgeon.mcp-alias-migration :as alias-migration]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-combinable-transaction :as combinable]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-compact-relations :as compact-relations]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-extraction :as extraction]
   [clj-surgeon.mcp-feature-thread :as feature-thread]
   [clj-surgeon.mcp-formatter :as formatter]
   [clj-surgeon.mcp-helper-extraction :as helper-extraction]
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
    "When a prior inspect_clojure match on the same snapshot found the sites, copy its file, "
    "file_hash, pattern, and match_count into the optional expect_matched object; the receipt then "
    "reports matched_count, addressed_matches, and every matched site this transaction did not "
    "address as unaddressed_matches [{line, hash}]. A file, hash, or count disagreement refuses "
    "expect-matched-stale before any write. "
    "Omit verify unless the user or repository explicitly requests a configured transaction profile. "
    ;; @spec MCP-OP-VERIFY-013
    "A test profile is NAMED BY THE REPOSITORY in .clj-surgeon.edn under "
    ":verification-profiles; a verify naming a profile this workspace does not "
    "configure is refused before any write. lint is the only built-in profile "
    "and runs a lint and format gate — it is NOT a test profile, and it fails "
    "on pre-existing lint debt in source this transaction never touched. "
    "When requested, verify is fast, full, lint, or the project-owned exact profile. Staged formatting, "
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
    (census-tool/init! configured)
    (admit-tool/init! configured)
    (feature-thread/init! configured)))

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

;; @spec MCP-OP-MATCHED-001
;; @spec MCP-OP-MATCHED-002
(defn- resolve-expect-matched
  "Bind the optional prior-match basis to the same confined absolute path the
   transaction reads, keeping the caller's project-relative names for the
   receipt and every refusal."
  [project-root basis resolved]
  (when basis
    (let [resolution (resolve-source-path project-root (:file basis))]
      (if-not (:ok resolution)
        (assoc resolution :raw-path (:file basis))
        {:ok true
         :expect-matched
         (assoc basis
                :file (:path resolution)
                :public {:file (:file basis)
                         :files (->> (:path-facts resolved)
                                     (map :raw)
                                     distinct
                                     sort
                                     vec)})}))))

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

(def ^:private verification-profiles-unconfigured-remedy
  (str "This workspace configures no verification profiles. Add "
       ":verification-profiles to .clj-surgeon.edn at the workspace root, "
       "binding each profile name to this repository's OWN test command, "
       "then retry with that name. clj-surgeon runs no built-in gate as "
       "verification: a lint or format check is not a test profile, and "
       "refusing a correct edit on pre-existing lint debt is not "
       "verification."))

;; @spec MCP-OP-VERIFY-013
(defn- unconfigured-verification-refusal
  "Refuse, before any write, a `verify` this workspace never configured.

  Reached only when the profile map is the built-in one — a workspace with its
  own `:verification-profiles` keeps every prior behaviour."
  [config verify]
  (when (and verify
             (= :built-in (:verification-profile-source config))
             (not (contains? (:verification-profiles config) verify)))
    {:error (str "No verification profile named " (pr-str verify)
                 " is configured for this workspace")
     :error-type :verification-profiles-unconfigured
     :field "verify"
     :actual verify
     :accepted (vec (sort (keys (:verification-profiles config))))
     :source-unchanged true
     :remedy verification-profiles-unconfigured-remedy}))

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
                       ;; @spec MCP-OP-MATCHED-001
                       (:expect-matched resolved)
                       (assoc :expect-matched (:expect-matched resolved))

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

;; @spec MCP-OP-ALIAS-027
(defn resolve-verification-config
  "Resolve a routed workspace's lazy profile accessors into concrete profiles.

  A workspace context published by the HTTP server carries
  :verification-profile-selection-fn / :verification-profiles-fn rather than
  :verification-profiles, so every public entrance must resolve them or it
  silently reads the SERVER's profiles instead of the requested workspace's.
  One function, both callers, so the two cannot drift apart again."
  [config]
  (cond
    (:verification-profile-selection-fn config)
    (let [{:keys [profiles source]} ((:verification-profile-selection-fn config))]
      (assoc config
             :verification-profiles profiles
             :verification-profile-source source))

    (:verification-profiles-fn config)
    (assoc config
           :verification-profiles ((:verification-profiles-fn config)))

    :else config))

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
        config (resolve-verification-config config)
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
                         ;; @spec MCP-OP-MATCHED-001
                         matched-basis
                         (when (:ok resolved)
                           (resolve-expect-matched
                             root
                             (get-in validated [:params :expect-matched])
                             resolved))
                         resolved (cond
                                    (nil? matched-basis) resolved
                                    (not (:ok matched-basis)) matched-basis
                                    :else (assoc resolved :expect-matched
                                                 (:expect-matched
                                                   matched-basis)))
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
                    ;; @spec MCP-OP-VERIFY-013
                    ;; one gate for BOTH write routes, ahead of every write:
                    ;; a `verify` this workspace never configured is refused,
                    ;; never satisfied by a built-in lint run.
                    (timed #(or (unconfigured-verification-refusal
                                  config (get-in validated [:params :verify]))
                                (if extraction?
                                  (execute-extraction!
                                    config root (:extraction resolved) receipt
                                    (get-in validated [:params :verify]))
                                  (execute-explicit-change!
                                    config root resolved receipt
                                    (get-in validated [:params :verify])
                                    (:compact-location-normalization validated)
                                    (:compact-relation-plan validated)
                                    compact-effect-identity?
                                    public-operation))))
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

(def verification-line-characters
  "Stated bound on the visible verification line of a success receipt."
  600)

(def verification-failure-detail-characters
  "Stated bound on the WHOLE verbatim failure detail a refusal text carries,
  header and omission markers included — not a fresh budget per check."
  2000)

(defn- test-tally
  "The `Ran N tests containing M assertions.` line a check's output carried."
  [output]
  (when (string? output)
    (first (re-seq #"(?m)^Ran \d+ tests? containing \d+ assertions\.$" output))))

(defn- hot-tally
  "The hot verifier reports a summary map, never a clojure.test tally line."
  [hot]
  (when-let [summary (:summary hot)]
    (str (or (:test summary) 0) " tests · " (or (:pass summary) 0) " passes"
         " · " (or (:fail summary) 0) " failures"
         " · " (or (:error summary) 0) " errors")))

;; @spec MCP-OP-VERIFY-012
(defn- whole-lines-within
  "Whole lines of `text` inside `limit` characters, cut only at a line
  boundary. A single line longer than the bound is SHORTENED and marked, never
  dropped: a bound must degrade, never delete. Returns [text omitted-lines]."
  [text limit]
  (let [lines (str/split-lines (or text ""))
        [kept _]
        (reduce (fn [[kept used] line]
                  (let [cost (inc (count line))]
                    (if (<= (+ used cost) limit)
                      [(conj kept line) (+ used cost)]
                      (reduced [kept used]))))
                [[] 0]
                lines)
        shortened? (empty? kept)
        kept (if shortened?
               [(str (subs (first lines) 0 (max 0 (min limit (count (first lines)))))
                     " … [line truncated to " limit " characters]")]
               kept)]
    [(str/join "\n" kept)
     (if shortened? 0 (- (count lines) (count kept)))
     shortened?]))

(defn- check-label
  [check]
  (str "`" (or (:command check) "check") "`"
       (when (some? (:exit check)) (str " exit " (:exit check)))))

;; @spec MCP-OP-VERIFY-011
(defn- cold-state-phrase
  "What a cold job's own status licenses the receipt to say. A running job has
  proved nothing, and this line never lets it read as a pass."
  [cold]
  (case (:status cold)
    :running (str "cold: RUNNING — not yet a pass; copy next_call to "
                  "inspect_clojure for its verdict")
    :passed (str "cold: passed"
                 (when (some? (:exit cold)) (str " · exit " (:exit cold))))
    (str "cold: " (name (or (:status cold) :unknown))
         (when (some? (:exit cold)) (str " · exit " (:exit cold))))))

;; @spec MCP-OP-VERIFY-011
(defn verification-success-line
  "State the verification this call actually performed, or that none ran.

  `written bytes read back` is a claim about BYTES. It has never been a claim
  about tests, and a receipt that renders the same text either way teaches an
  actor to re-verify every call. The real profile result carries THREE places
  a verdict can live — command `:checks`, `:hot-verification` and
  `:cold-verification` — and every one of them that ran is named here."
  [verification]
  (let [line
        (if-not (map? verification)
          "✓ verification: none requested — bytes read back only"
          (let [checks (vec (:checks verification))
                hot (:hot-verification verification)
                cold (:cold-verification verification)
                passed (count (filter :ok checks))
                tally (some #(test-tally (:output %)) checks)
                segments
                (remove
                  nil?
                  [(when (seq checks)
                     (str passed " of " (count checks) " check"
                          (if (= 1 (count checks)) "" "s") " passed · "
                          (str/join " · " (map check-label checks))))
                   (when tally tally)
                   (when (map? hot)
                     (str "hot: " (name (or (:status hot)
                                            (if (:ok hot) :complete :failed)))
                          (when-let [t (hot-tally hot)] (str " · " t))))
                   (when (map? cold) (cold-state-phrase cold))
                   (when (seq (:argv verification))
                     (str "`" (str/join " " (:argv verification)) "`"
                          (when (some? (:exit verification))
                            (str " exit " (:exit verification)))))])]
            (str "✓ verification: " (or (:profile verification) "unknown")
                 (when-let [source (:profile-source verification)]
                   (str " (" (name source) ")"))
                 " · "
                 (if (seq segments)
                   (str/join " · " segments)
                   "profile ran; it reported no per-check detail"))))]
    (if (<= (count line) verification-line-characters)
      line
      (str (subs line 0 (- verification-line-characters 40))
           " … [line truncated at " verification-line-characters "]"))))

;; @spec MCP-OP-VERIFY-012
(defn verification-failure-block
  "The failing evidence's own bytes, verbatim, inside ONE stated budget.

  Only what actually FAILED is rendered: a successful command check is never
  shown under a `✗`, and a hot or cold failure is rendered even when every
  command check passed. The whole block — header, per-entry lines and the
  omission markers themselves — is held inside
  `verification-failure-detail-characters`, so N failures share one budget
  rather than minting a fresh one each."
  [verification]
  (when (map? verification)
    (let [checks (vec (:checks verification))
          hot (:hot-verification verification)
          cold (:cold-verification verification)
          failed-checks (vec (remove :ok checks))
          hot-failed? (and (map? hot) (not (:ok hot)))
          cold-failed? (and (map? cold)
                            (not= :running (:status cold))
                            (not (:passed cold))
                            (not= :passed (:status cold)))
          entries
          (concat
            (map (fn [check]
                   [(str "  ✗ " (check-label check)
                         (when-let [elapsed (:elapsed_ms check)]
                           (str " · " (mcp-operation/format-elapsed-ms elapsed))))
                    (:output check)])
                 failed-checks)
            (when hot-failed?
              [[(str "  ✗ hot: "
                     (name (or (:status hot) :failed))
                     (when-let [t (hot-tally hot)] (str " · " t)))
                (:output hot)]])
            (when cold-failed?
              [[(str "  ✗ " (cold-state-phrase cold)) (:output cold)]])
            (when (and (:diagnostics verification)
                       (not (seq failed-checks))
                       (not hot-failed?)
                       (not cold-failed?)
                       (false? (:ok verification)))
              [["  ✗ exact verifier" (:diagnostics verification)]]))]
      (when (seq entries)
        (let [;; @spec MCP-OP-VERIFY-012
              ;; the HEADER is inside the budget too. A 2,200-character profile
              ;; name published a 2,385-character block: a bound the header sat
              ;; beside is not a bound.
              header-limit (quot verification-failure-detail-characters 4)
              raw-header (str "verification: "
                              (or (:profile verification) "unknown")
                              (when-let [source (:profile-source verification)]
                                (str " (" (name source) ")"))
                              " · failed")
              header (if (<= (count raw-header) header-limit)
                       raw-header
                       (str (subs raw-header 0 header-limit)
                            " … [header truncated to " header-limit
                            " characters; the complete verification block is "
                            "in structuredContent]"))
              ;; room for the omission marker, whose own length depends on
              ;; what it must report; measured longest form is ~200 chars
              reserve 280
              budget (- verification-failure-detail-characters
                        (count header) reserve)
              [rendered omitted-lines omitted-entries]
              (reduce
                (fn [[acc omitted-lines omitted-entries] [label output]]
                  (let [remaining (- budget (count acc))]
                    (if (< remaining (+ (count label) 2))
                      [acc omitted-lines (inc omitted-entries)]
                      (let [[body dropped shortened?]
                            (if (string? output)
                              (whole-lines-within
                                output (- remaining (count label) 2))
                              ["" 0 false])]
                        [(str acc "\n" label
                              (when (seq body) (str "\n" body)))
                         (+ omitted-lines dropped (if shortened? 1 0))
                         omitted-entries]))))
                ["" 0 0]
                entries)
              marker (when (or (pos? omitted-lines) (pos? omitted-entries))
                       (str "\n… [bounded excerpt: " omitted-lines
                            " output line"
                            (if (= 1 omitted-lines) "" "s") " and "
                            omitted-entries " failed check"
                            (if (= 1 omitted-entries) "" "s")
                            " omitted to hold this block inside "
                            verification-failure-detail-characters
                            " characters; the complete verification block is "
                            "in structuredContent]"))
              assembled (str header rendered marker)]
          ;; the bound is on the WHOLE block, marker included. If the reserve
          ;; under-estimated the marker, cut the body again at a line boundary
          ;; rather than publish one character over a stated number.
          (if (<= (count assembled) verification-failure-detail-characters)
            assembled
            ;; last resort only, and it SHORTENS rather than drops: losing the
            ;; failing line entirely would be a bound that deletes.
            (let [room (max 0 (- verification-failure-detail-characters
                                 (count header) (count (or marker ""))))]
              (str header
                   (subs rendered 0 (min room (count rendered)))
                   marker))))))))

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
          ;; @spec MCP-OP-MATCHED-001
          matched-line
          (when-let [total (:matched_count result)]
            (let [unaddressed (:unaddressed_matches result)
                  lines (str/join ", " (map :line unaddressed))]
              (cond
                (zero? (long total))
                (str "\n✓ prior match basis · the pattern matched no site "
                     "in this snapshot")

                (zero? (long (or (:unaddressed_match_count result) 0)))
                (format "\n✓ prior match basis · %s addressed"
                        (if (= 1 (long total))
                          "the 1 matched site"
                          (str "all " (long total) " matched sites")))

                :else
                (format (str "\n⚠ prior match basis · %d of %d matched site%s "
                             "not addressed by this transaction (pre-image "
                             "line%s %s%s)")
                        (long (:unaddressed_match_count result))
                        (long total)
                        (if (= 1 (long total)) "" "s")
                        (if (= 1 (count unaddressed)) "" "s")
                        lines
                        (if (:unaddressed_matches_truncated result)
                          "; truncated"
                          "")))))
          terminal-response-line
          (when (string? (:terminal_response result))
            (str "\n→ If this mutation completes all remaining work, return exactly: "
                 (:terminal_response result)
                 "\n  If work remains, continue."))]
      (if (= "edit_clojure-preview" operation)
        (str operation "\n"
             (format
               (str "  %s changed files · %s changed characters · %s\n\n"
                    "✓ complete bounded diff compiled\n"
                    "✓ source unchanged · no write authority\n"
                    "✓ lifecycle preview · next action none")
               (:changed_files result)
               (:changed_characters result)
               (mcp-operation/format-elapsed-ms (:elapsed_ms result))))
        ;; @spec MCP-OP-VERIFY-011
        ;; Caller-derived text — a check command, a profile name, a
        ;; terminal_response — is CONCATENATED after formatting, never spliced
        ;; into the format TEMPLATE. A command spelled `printf %s ok` or a
        ;; profile named `coverage 100%` threw from `format` and destroyed the
        ;; public receipt of a mutation that had already committed.
        (let [head (str operation "\n"
                        (format
                          (str "  %s edits · %s files · %s\n\n"
                               "✓ atomic commit complete\n")
                          (or (:edits result) (:match-count result) 0)
                          (or (:files result) (:changed-file-count result) 0)
                          (mcp-operation/format-elapsed-ms (:elapsed_ms result))))]
          (if (:verification_complete result)
            (str head
                 "✓ written bytes read back\n"
                 (verification-success-line (:verification result))
                 caller-proof-line matched-line "\n"
                 "✓ terminal evidence · verification_complete=true · next action none"
                 terminal-response-line)
            (str head
                 "✓ written bytes read back and hot proof complete\n"
                 (verification-success-line (:verification result))
                 caller-proof-line matched-line "\n"
                 "… cold verification running · edit remains committed\n"
                 "→ copy next_call to inspect_clojure after doing other useful work")))))
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
          ;; @spec MCP-OP-FIELD-002
          named-field-line (when (and field (seq (:accepted result)))
                             (format "  field %s accepts: %s%s\n"
                                     field
                                     (str/join ", " (:accepted result))
                                     (if (contains? result :actual)
                                       (str " · received "
                                            (pr-str (:actual result)))
                                       "")))
          source-safe? (or (:source-unchanged result)
                           (:source_unchanged result)
                           (:rolled-back result))]
      (str operation "\n"
       (format (str "  refused · %s%s · %s\n"
                   "%s%s\n"
                   "%s\n"
                   ;; @spec MCP-OP-VERIFY-012
                   "%s"
                   "→ %s")
              reason
              (if path (str " at " (pr-str path)) "")
              (mcp-operation/format-elapsed-ms (:elapsed_ms result))
              (or change-line "")
              (or named-field-line "")
              (if source-safe?
                "✓ source unchanged"
                "⚠ source state requires structured receipt review")
              (if-let [block (verification-failure-block (:verification result))]
                (str block "\n")
                "")
              (or (:remedy result) (:next_action result)
                  "Correct the request and retry once."))))))

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

(def ^:private editor-tool-fields
  (set (keys (:properties mcp-schema/editor-tool-schema))))

;; @spec MCP-OP-MATCHED-005
(defn- undeclared-editor-fields
  "Request fields `edit_clojure`'s published schema does not declare.

   Both public entrances share one handler, so without this the handler accepts
   `changes` and `expect_matched` on a tool whose schema denies them."
  [params]
  (->> (keys (or params {}))
       (map #(if (keyword? %) (name %) (str %)))
       (remove editor-tool-fields)
       sort
       vec))

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

                   ;; @spec MCP-OP-MATCHED-005
                   (and (= "edit_clojure" operation)
                        (seq (undeclared-editor-fields params)))
                   {:ok false
                    :error_type "invalid-mcp-request"
                    :error "edit_clojure does not authorize fields its published schema omits"
                    :unexpected_fields (undeclared-editor-fields params)
                    :source_unchanged true
                    :mutation_attempted false
                    :write_authority false
                    :remedy (str "edit_clojure accepts only the fields its schema "
                                 "declares. Send changes, expect, and expect_matched "
                                 "to apply_clojure_changes instead.")}

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

;; @spec MCP-OP-ALIAS-059
(def alias-migration-refusal-envelope-keys
  "Receipt keys the refusal text renders structurally rather than as facts.

  ONLY the keys this renderer actually renders. `mutation_attempted`,
  `write_authority`, `source_unchanged`, `next_action` and
  `expect_files_unchanged_reason` were listed here and rendered nowhere, so
  they were removed from the fact line and then dropped from the text
  altogether: the E-PREWRITE cohort read an `alias-policy-exhausted` refusal
  whose structuredContent carried `mutation_attempted false` and
  `write_authority false` — the two fields that separate \"refused before
  touching anything\" from \"tried and rolled back\" — and whose text carried
  neither. A key is envelope because the renderer HAS a place for it, never
  because it looks structural."
  #{:ok :operation :error_type :error :next_call :remedy :elapsed_ms
    :workspace_root :receipt_hash :undo_receipt :details_path
    :details_retained :details_retention})

;; @spec MCP-OP-ALIAS-059
(def max-refusal-fact-characters
  "Ceiling on ONE rendered discriminating fact.

  The text block is constant-size or it is not a receipt, and the one thing in
  a fact that grows without limit is a caller-supplied path."
  160)

;; @spec MCP-OP-ALIAS-059
(def max-refusal-facts
  "How many discriminating facts one refusal text renders.

  Sixteen, not twelve: five keys that were listed as envelope and rendered
  nowhere are facts now, and the widest live refusal —
  `scope-matches-nothing` where no next_call can be composed — carries
  fifteen. A bound that would drop one of them turns the fix for text ⊇
  structured back into the defect it closed."
  16)

;; @spec MCP-OP-ALIAS-059
(def max-rendered-next-call-characters
  "Ceiling on the next_call JSON one refusal text inlines.

  Twice the planner's own 512-character next_call bound, so every call the verb
  composes is inlined and only a pathological one is replaced by a POINTER that
  names its length — never dropped in silence."
  1024)


;; @spec MCP-OP-ALIAS-059
(defn- ceiling-writer
  "A `Writer` that collects into `builder` and refuses past `ceiling`.

  Every concrete `Writer.write` overload is intercepted rather than only the
  abstract one, because `print-method` reaches the writer three different
  ways: `append` on a char for an ordinary character, `write` on a String for
  an escape sequence, and `write` on a char array for a copied region."
  [^StringBuilder builder ceiling]
  (let [refuse! (fn []
                  (when (> (.length builder) ceiling)
                    (throw (ex-info "print ceiling reached"
                                    {::print-ceiling true}))))]
    (proxy [java.io.Writer] []
      (write
        ([data]
         (cond
           (integer? data) (.append builder (char (int data)))
           (string? data) (.append builder ^String data)
           :else (.append builder (String. ^chars data)))
         (refuse!))
        ([data off len]
         (if (string? data)
           (.append builder ^String (subs ^String data off (+ (int off) (int len))))
           (.append builder ^String (String. ^chars data (int off) (int len))))
         (refuse!)))
      (flush [])
      (close []))))

;; @spec MCP-OP-ALIAS-059
(def ^:private print-safe-scalar-classes
  "The EXACT classes `print-method` may be handed, enumerated by class.

  Round-fifteen review finding 1: an allowlist BY TYPE admits every SUBTYPE.
  The predicate this replaces admitted everything satisfying `number?` — that
  is `(instance? java.lang.Number …)` — and `print-method`'s own `Number`
  implementation is `print-simple`, which writes `(str o)`. `java.lang.Number`
  is not final, so a `proxy` or an anonymous subclass carries an ARBITRARY
  `toString` straight through the branch the renderer called safe: a throwing
  one escaped `bounded-pr-str` and a looping one hung it — exactly the two
  failures the round-fourteen fix closed for every other class.

  Measured on this JVM (`Modifier/isFinal`): `String` and `Boolean` are final
  and cannot be subclassed at all, so their `instance?` tests were already
  exact. `clojure.lang.Keyword`, `clojure.lang.Symbol`, `java.math.BigInteger`,
  `java.math.BigDecimal`, `clojure.lang.Ratio` and `java.lang.Number` are NOT
  final, and `print-method` reaches `print-simple` — `(str o)` — for Keyword,
  Symbol and Number alike. So membership is decided by `(class value)` and not
  by `instance?`, uniformly, for every scalar: a subclass of any of them is
  not this class and renders as an identity marker.

  The set is the numeric representations Clojure and the JVM really produce
  plus the four other scalars the renderer has always printed. `Character` is
  deliberately absent: it was not admitted before this change either, and
  admitting it now would alter a rendering."
  #{java.lang.String
    java.lang.Boolean
    clojure.lang.Keyword
    clojure.lang.Symbol
    java.lang.Long
    java.lang.Integer
    java.lang.Short
    java.lang.Byte
    java.lang.Double
    java.lang.Float
    java.math.BigInteger
    java.math.BigDecimal
    clojure.lang.BigInt
    clojure.lang.Ratio})

;; @spec MCP-OP-ALIAS-059
(defn- print-safe-leaf?
  "True when `value` is a scalar `print-method` can render without ever
  reaching an arbitrary object's `toString`.

  Decided on the value's EXACT class, never on `instance?`: see
  `print-safe-scalar-classes`."
  [value]
  (or (nil? value)
      (contains? print-safe-scalar-classes (class value))))

;; @spec MCP-OP-ALIAS-059
(defn- opaque-object-marker
  "Identity-only rendering for a value that is not Clojure data — the class's
  simple name and identity hash, WITHOUT ever calling `.toString`.

  `print-method`'s own default for an object it does not recognise calls
  `.toString` before a single character reaches `ceiling-writer`, so a
  `deftype` whose `toString` never returns hangs the renderer no matter how
  tight the ceiling is, and one whose `toString` throws escapes the
  ceiling's own catch entirely. A raw Java collection is exactly this shape
  too: its own `toString` walks and stringifies every element, which is the
  same unbounded work one level removed."
  [value]
  (str "#object[" (.getSimpleName (class value)) " "
       (Integer/toHexString (System/identityHashCode value)) "]"))

;; @spec MCP-OP-ALIAS-059
(declare write-bounded)

;; @spec MCP-OP-ALIAS-059
(defn- write-safe-leaf
  "Prints one admitted scalar, with the CALL ITSELF guarded.

  The allowlist decides which classes may reach `print-method`; this decides
  what happens if one of them misbehaves anyway. Defence in depth, and cheap:
  for every class in `print-safe-scalar-classes` the guard can never fire,
  because none of them can be subclassed into hostility while remaining that
  exact class. The ceiling's own refusal is re-thrown rather than swallowed —
  it is the renderer stopping on purpose, not a leaf failing."
  [^java.io.Writer writer value]
  (try
    (print-method value writer)
    (catch clojure.lang.ExceptionInfo e
      (if (::print-ceiling (ex-data e))
        (throw e)
        (.write writer (opaque-object-marker value))))
    (catch Throwable _
      (.write writer (opaque-object-marker value)))))

;; @spec MCP-OP-ALIAS-059
(defn- write-bounded-elements
  "Writes `coll`'s elements to `writer`, `sep`-separated, one at a time.

  Walked with `seq`/`next` rather than `count`/`nth`, so an endless lazy
  sequence is walked lazily — the ceiling writer's own refusal, thrown from
  inside one of these `.write` calls, is what stops it, exactly as it always
  stopped `print-method`'s recursion. A `(first coll)` truthiness check would
  stop early on a real `nil` element, so emptiness is read from the seq
  itself."
  [^java.io.Writer writer coll level sep]
  (loop [items (seq coll) first? true]
    (when items
      (when-not first? (.write writer ^String sep))
      (write-bounded writer (first items) (dec level))
      (recur (next items) false))))

;; @spec MCP-OP-ALIAS-059
(defn- write-bounded-map-entries
  [^java.io.Writer writer m level]
  (loop [entries (seq m) first? true]
    (when entries
      (when-not first? (.write writer ", "))
      (let [entry (first entries)]
        (write-bounded writer (key entry) (dec level))
        (.write writer " ")
        (write-bounded writer (val entry) (dec level)))
      (recur (next entries) false))))

;; @spec MCP-OP-ALIAS-059
(defn- write-bounded-meta
  "Writes `value`'s metadata, when the caller asked to see it.

  Round-fifteen review finding 3: replacing `print-method`'s recursion
  dropped two of its renderings — this one, and the record tag below. Core's
  `print-meta` writes the map (or, for a lone `:tag`, the tag alone) under
  exactly this condition, and the metadata itself recurses through the same
  bounded writer as any other value, so nothing here is unbounded: a
  poisonous object inside a metadata map renders as the same identity marker
  it renders as anywhere else."
  [^java.io.Writer writer value level]
  (when (or *print-dup* (and *print-meta* *print-readably*))
    (let [m (meta value)]
      (when (and m (pos? (count m)))
        (.write writer "^")
        (if (and (= 1 (count m)) (:tag m))
          (write-bounded writer (:tag m) level)
          (write-bounded writer m level))
        (.write writer " ")))))

;; @spec MCP-OP-ALIAS-059
(defn- write-bounded
  "Writes `value` to `writer`, recursing into Clojure data and refusing to
  invoke `toString` on anything else.

  The renderer prints only Clojure data — maps, vectors, sets, lists, seqs,
  strings, numbers, keywords, symbols, booleans and nil — checked by type
  BEFORE any printer is chosen; every other object, recursively inside
  collections, renders as `opaque-object-marker` instead of being handed to
  `print-method`'s object-toString fallback.

  `level` mirrors the old `*print-level*` second floor: decremented on every
  recursive descent, rendering `#` once it reaches zero. It is unreachable in
  ordinary use for the same reason `*print-level*` was — `ceiling-writer`
  throws on character count first, and every level of nesting costs at least
  the two characters of its own delimiters."
  [^java.io.Writer writer value level]
  (cond
    (print-safe-leaf? value)
    (write-safe-leaf writer value)

    (zero? level)
    (.write writer "#")

    ;; @spec MCP-OP-ALIAS-059
    ;; a record is a map AND carries its own tag; `.getName` reads the class
    ;; name without ever calling the value's `toString`, so the tag costs
    ;; nothing the ceiling does not already bound
    (record? value)
    (do (write-bounded-meta writer value level)
        (.write writer "#")
        (.write writer (.getName (class value)))
        (.write writer "{")
        (write-bounded-map-entries writer value level)
        (.write writer "}"))

    (map? value)
    (do (write-bounded-meta writer value level)
        (.write writer "{")
        (write-bounded-map-entries writer value level)
        (.write writer "}"))

    (set? value)
    (do (write-bounded-meta writer value level)
        (.write writer "#{")
        (write-bounded-elements writer value level " ")
        (.write writer "}"))

    (vector? value)
    (do (write-bounded-meta writer value level)
        (.write writer "[")
        (write-bounded-elements writer value level " ")
        (.write writer "]"))

    (or (list? value) (seq? value))
    (do (write-bounded-meta writer value level)
        (.write writer "(")
        (write-bounded-elements writer value level " ")
        (.write writer ")"))

    :else
    (.write writer (opaque-object-marker value))))

;; @spec MCP-OP-ALIAS-059
(def ^:private print-time-budget-ms
  "The wall-clock floor under one bounded rendering.

  Round-fifteen review finding 1: the ceiling bounds CHARACTERS, and work that
  emits no character is unbounded by it — a `lazy-seq` whose body never
  returns never yields its first element, so nothing is ever written and the
  renderer waits forever. Two seconds is three orders of magnitude above the
  slowest ordinary rendering measured here (a ten-megabyte string publishes
  160 characters in a fraction of a millisecond) and far below any timeout a
  caller waits on."
  2000)

;; @spec MCP-OP-ALIAS-059
(defn- bounded-text
  [^StringBuilder builder ceiling]
  (let [text (.toString builder)]
    (if (> (count text) ceiling)
      (str (subs text 0 ceiling) "…")
      text)))

;; @spec MCP-OP-ALIAS-059
(defn bounded-pr-str
  "`pr-str` bounded in WORK as well as in output.

  A ceiling applied to a finished string is a bound on the receipt and not on
  the request. `pr-str` realises the complete value before anything measures
  it, so an endless lazy sequence never reaches the gate, a value nested
  twenty thousand deep takes the renderer down with a StackOverflowError, and
  a ten-megabyte string is rendered whole in order to publish 160 characters
  of it — 362 ms of work to produce a fact line nobody could tell apart from
  the cheap one.

  So printing STOPS at the ceiling instead: the writer refuses the moment the
  buffer passes it. That alone is not enough — `print-method`'s own default
  for an object it does not recognise calls that object's `toString` before a
  single character reaches the writer, so a `deftype` whose `toString` never
  returns hangs the renderer regardless of the ceiling. `write-bounded`
  replaces `print-method`'s recursion for compound values with its own,
  admitting only Clojure data before a value is ever printed and rendering
  everything else as an identity marker — `level` standing in for the old
  `*print-level*` second floor, since `*print-length*` is now redundant: the
  writer's own character ceiling is what actually stops an endless or huge
  value, exactly as before.

  The three-argument arity takes the budget as a DEADLINE ALLOWANCE, so a
  caller rendering several values into one receipt can spend one budget across
  all of them rather than one each: `refusal-fact-line` renders up to sixteen
  facts, and sixteen unrenderable values cost a measured 32,011 ms when each
  gets its own two seconds. The receipt is the unit a caller waits on."
  ([value ceiling] (bounded-pr-str value ceiling print-time-budget-ms))
  ([value ceiling budget-ms]
  (let [builder (StringBuilder.)
        outcome-promise (promise)
        ;; @spec MCP-OP-ALIAS-059
        ;; a DAEMON thread and not `future`: the send-off pool's threads are
        ;; not daemons, so one abandoned inside a `toString` that never
        ;; returns keeps its JVM — an MCP server — from ever exiting. Measured
        ;; on this branch: a probe that rendered a looping `Number` correctly
        ;; then hung at JVM exit until it was killed. A rendering the budget
        ;; abandons must cost a leaked thread and nothing else.
        worker (doto (Thread.
                       ^Runnable
                       (bound-fn []
                         (deliver outcome-promise
                                  (try
                                    (binding [*print-readably* true]
                                      (write-bounded
                                        (ceiling-writer builder ceiling)
                                        value ceiling))
                                    ::completed
                                    (catch Throwable t t))))
                       "clj-surgeon-bounded-print")
                 (.setDaemon true)
                 (.start))
        outcome (deref outcome-promise (max 1 (long budget-ms)) ::timed-out)]
    (cond
      ;; @spec MCP-OP-ALIAS-059
      ;; A character ceiling cannot stop work that produces no character. A
      ;; `lazy-seq` whose body never returns yields no first element, so the
      ;; ceiling writer is never called and the renderer waits forever; the
      ;; same is true of any admitted leaf that could be made to loop. The
      ;; TIME bound is the outer floor under both, and it returns the same
      ;; identity marker every other unrenderable value gets. The builder is
      ;; deliberately NOT read here: the abandoned thread may still be writing
      ;; to it, and a partially-written buffer read across a race is worse
      ;; than an honest marker.
      (= ::timed-out outcome)
      (do (.interrupt ^Thread worker)
          (if (nil? value) "nil" (opaque-object-marker value)))

      (instance? Throwable outcome)
      (if (and (instance? clojure.lang.ExceptionInfo outcome)
               (::print-ceiling (ex-data ^clojure.lang.ExceptionInfo outcome)))
        (bounded-text builder ceiling)
        (throw ^Throwable outcome))

      :else (bounded-text builder ceiling)))))

;; @spec MCP-OP-ALIAS-059
(defn refusal-fact-line
  "The refusal's own discriminating fields, rendered for a text-reading client.

  A refusal has two faces — `structuredContent` and `content[0].text` — and a
  client that reads only the text must not be told less than one that reads the
  structure. In the E3-P cohort (2026-09-03) the structured refusal carried
  `found_files 0` and `scanned_files 0`, the two numbers that separate `your
  glob matched nothing` from `nothing here requires that lib`; the text carried
  neither, and the arm that read the text sent the same wrong scope twice.

  Sorted by field name so the line is a function of the refusal and not of map
  order, and bounded in both count and per-fact length — and the COUNT bound
  says when it fired. No alias_migration refusal carries twelve discriminating
  facts today, so this bound has never dropped one; a bound that truncates in
  silence breaks the text ⊇ structured contract on the day it first fires, and
  a reader of the text has no way to know it did.

  The print deadline is ONE budget for the whole line rather than one per fact,
  which has a consequence worth stating: a fact late in a slow receipt gets
  less of that budget, so an ordinary value can render as an identity marker
  where on its own it would have rendered as itself. The degradation is typed
  — a marker naming the value's class and identity hash — never wrong data and
  never a hang, and it is the correct trade for a receipt bounded as the one
  unit the caller actually waits on."
  [result]
  (let [;; @spec MCP-OP-ALIAS-059
        ;; EVERY non-envelope key, whatever the shape of its value. The old
        ;; predicate admitted strings, numbers, booleans and flat sequentials
        ;; and dropped everything else IN SILENCE, so a nested map added to a
        ;; live refusal was carried by structuredContent and absent from the
        ;; text — and the source-derived key witness could not see it, because
        ;; it probed every key with the string "probe-value". A value too
        ;; large to render whole is ELIDED at the per-fact bound, which is the
        ;; only honest way to bound a fact: named, cut, and pointed at the
        ;; structure.
        renderable (->> result
                        (remove (fn [[field _]]
                                  (contains?
                                    alias-migration-refusal-envelope-keys
                                    field)))
                        (sort-by key))
        dropped (max 0 (- (count renderable) max-refusal-facts))
        ;; @spec MCP-OP-ALIAS-059
        ;; ONE print budget for the whole receipt, not one per fact: a refusal
        ;; carrying sixteen unrenderable values cost a measured 32,011 ms while
        ;; each fact got its own. `mapv` and not `map`, because a deadline
        ;; means nothing to a sequence nobody has realised yet.
        deadline (+ (System/currentTimeMillis) print-time-budget-ms)
        facts (->> renderable
                   (take max-refusal-facts)
                   (mapv (fn [[field value]]
                           ;; bounded in WORK, not merely cut afterwards
                           (str (name field) "="
                                (bounded-pr-str
                                  value max-refusal-fact-characters
                                  (- deadline
                                     (System/currentTimeMillis)))))))]
    (when (seq facts)
      (str "facts · " (str/join " · " facts)
           (when (pos? dropped)
             (str " · +" dropped " more in structuredContent"))))))

;; @spec MCP-OP-ALIAS-059
(defn rendered-next-call
  "The next_call line: sendable JSON, a bounded pointer, or a stated absence.

  A refusal that carries an executable remedy the caller never sees costs a
  model return at random — whichever face of the receipt that caller happens to
  read. An absent next_call is STATED rather than omitted, because a missing
  line and an uncomputable remedy are indistinguishable in silence.

  The stated absence does not point at a remedy unless one is there. The live
  `invalid-workspace-root` receipt carried no `:remedy` and this line still
  said \"the remedy above names what only the caller can decide\", sending a
  caller to a sentence that does not exist — a receipt that describes its own
  contents wrongly is worse than one that is merely thin, because the caller
  stops looking."
  [result]
  (if-let [call (:next_call result)]
    (let [encoded (json/generate-string call)]
      (if (<= (count encoded) max-rendered-next-call-characters)
        (str "next_call · " encoded)
        (str "next_call · " (count encoded)
             " characters, in structuredContent.next_call — send it verbatim")))
    (if (:remedy result)
      (str "next_call · none — this refusal has no mechanically composable "
           "correction; the remedy above names what only the caller can decide")
      (str "next_call · none — this refusal has no mechanically composable "
           "correction and carries no remedy; the cause above is the whole of "
           "what is known"))))

;; @spec MCP-OP-ALIAS-042
;; @spec MCP-OP-ALIAS-059
(defn- bounded-refusal-text
  "One rendered refusal, held to the ceiling the verb publishes.

  The last gate rather than the only one: every list this renderer embeds is
  bounded in characters upstream, and this is what makes the whole a receipt
  even when a field nobody bounded grows. It is a typed cut — the marker names
  the length it replaced and where the whole refusal is — because a text block
  silently shorter than the receipt it renders breaks the text ⊇ structured
  contract with nothing in the text to show it."
  [text]
  (let [ceiling alias-migration/max-refusal-text-characters]
    (if (<= (count text) ceiling)
      text
      ;; @spec MCP-OP-VERIFY-012
      ;; Cut at a line boundary when one lies within a short lookback of the
      ;; ceiling, so a bounded verification excerpt does not lose its last line
      ;; to a mid-line cut. A single line longer than the window still cuts
      ;; hard: the ceiling is the ceiling.
      (let [marker (str "\n… [refusal text truncated at " ceiling
                        " characters; it rendered " (count text)
                        " — every field is complete in structuredContent]")
            hard (max 0 (- ceiling (count marker)))
            boundary (str/last-index-of text "\n" hard)
            cut (if (and boundary (<= (- hard boundary) 200)) boundary hard)]
        (str (subs text 0 cut) marker)))))

(defn alias-migration-summary
  "Render one compact visible summary whose length is constant in N.

  The committed block is gated on the receipt's own `:committed`, so the visible
  check marks and the structured receipt can never disagree."
  [result]
  (if (and (:ok result) (true? (:committed result)))
    (format (str "alias_migration\n"
                 "  %s files · %s sites · aliases %s · %s collisions resolved · %s\n\n"
                 "\u2713 atomic commit complete\n"
                 ;; @spec MCP-OP-VERIFY-011
                 "\u2713 written bytes read back\n"
                 "%s\n"
                 "\u2713 terminal evidence · per-file detail at %s (%s retention)")
            (:files result) (:sites result)
            (pr-str (:alias_histogram result))
            (:collisions_resolved result)
            (mcp-operation/format-elapsed-ms (:elapsed_ms result))
            (verification-success-line (:verification result))
            (:details_path result)
            (or (:details_retention result) "best-effort"))
    (bounded-refusal-text
      (str/join
       "\n"
       (remove
        nil?
        [(format (str "alias_migration\n"
                      "  refused · %s · %s\n\n"
                      "%s")
                 (or (:error_type result) (:reason result) "unknown-error")
                 (mcp-operation/format-elapsed-ms (:elapsed_ms result))
                 (if (or (:source_unchanged result) (:source-unchanged result))
                   "\u2713 source unchanged"
                   "\u26a0 source state requires structured receipt review"))
         ;; @spec MCP-OP-VERIFY-012
         (verification-failure-block (:verification result))
         (str "\u2192 " (or (:error result)
                            (:remedy result)
                            "Correct the request and retry once."))
         (refusal-fact-line result)
         (when-let [remedy (:remedy result)]
           (str "remedy · " remedy))
         (rendered-next-call result)])))))

(def alias-migration-tool-description
  (str
    "Migrate one Var to a new namespace and name across every namespace that "
    "requires the old one, in a single call whose payload does not grow with "
    "the number of affected files. Send from {lib, var}, to {lib, var, "
    "alias_policy}, scope {paths}, and expect {files}. Surgeon discovers every "
    "requiring namespace and every call site itself under every spelling that "
    "file makes legal — each :as alias, the fully qualified name, and the bare "
    "referred name — chooses each file's alias as the first alias_policy entry "
    "bound to nothing in that file, rewrites the require and every site, and "
    "commits one failure-atomic transaction. Locals of the same name, strings, "
    "docstrings, comments, metadata, #_ discards, and every reader-conditional "
    "branch other than the file's own platform branch stay byte-identical. "
    "Never send a per-file, per-owner, or per-site table; Surgeon discovers "
    "them. The receipt is one constant-size object: files, sites, the alias "
    "histogram, collisions resolved, the kondo delta, the focused-test result, "
    "and a details_path holding per-file detail, retained best-effort: read it "
    "from the receipt rather than assume the path keeps. Its receipt is terminal "
    "evidence of the rewrite; do not re-read the files it changed. A refusal is "
    "fail-closed and carries an executable next_call: send that once."))

;; @spec MCP-OP-ALIAS-001
(defn handle-alias-migration
  "Stable callback that plans, commits, and publishes one O(1) receipt."
  [_exchange params callback]
  (mcp-operation/invoke!
    {:execute
     (fn []
       (let [normalized (json/parse-string (json/generate-string params) true)]
         (if-not @runtime-config
           {:ok false
            :operation "alias_migration"
            :error_type "server-not-initialized"
            :error "alias_migration server is not initialized"
            :source_unchanged true
            :remedy "Restart the configured clj-surgeon MCP server."}
           (let [workspace-router (or (:workspace-router @runtime-config)
                                      (workspace/router @runtime-config))
                 routed (workspace/resolve-request workspace-router normalized)]
             (if-not (:ok routed)
               (assoc routed :operation "alias_migration")
               ;; the same receipt-directory derivation the direct dispatch
               ;; uses: the routed project root names the workspace's own
               ;; durable receipt directory
               (let [routed-config (resolve-verification-config (:config routed))
                     receipt-dir (str (or (:receipt-dir routed-config)
                                          (default-receipt-dir
                                            (:project-root routed-config))))]
                 (assoc (alias-migration/execute!
                          (assoc routed-config :receipt-dir receipt-dir)
                          (:params routed))
                        :workspace_root (:workspace-root routed))))))))
     :summarize alias-migration-summary
     :callback callback}))

(def alias-migration-tool
  {:id :alias-migration
   :name "alias_migration"
   :description alias-migration-tool-description
   :schema mcp-schema/alias-migration-schema
   :output-schema mcp-schema/alias-migration-output-schema
   :structured? true
   ;; @spec MCP-OP-ALIAS-059
   ;; the SDK wrapper publishes `mcp-adapter-failure` from outside
   ;; `mcp-operation/invoke!`, so it never sees the summarizer the operation
   ;; passes in; naming it on the tool is what lets that one refusal class
   ;; render its two faces the same way every other one does
   :summarize alias-migration-summary
   :tool-fn #'handle-alias-migration})

  ;; -------------------------------------------------------------------------
  ;; helper_extraction
  ;;
  ;; The registration half of `clj-surgeon.mcp-helper-extraction`. The handler
  ;; and the summarizer live HERE, with `handle-alias-migration` and
  ;; `alias-migration-summary`, because they need the three things this layer
  ;; owns: `resolve-verification-config` (one function, both callers), the
  ;; workspace router's own receipt directory, and the refusal-envelope
  ;; renderers. The verb's own boundary namespace supplies everything else.

;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-010
;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022
;; @spec MCP-OP-HELPER-010
;; @spec MCP-OP-HELPER-016
(defn helper-extraction-refusal
  "One helper_extraction refusal in the closed envelope this verb publishes.

  `next_call nil` is EXPLICIT rather than omitted: v1 has no mechanically
  composable continuation, and a refusal that simply lacks the key cannot be
  told apart from one that lost it."
  [error-type message evidence]
  (merge {:ok false
          :operation "helper_extraction"
          :error_type error-type
          :error message
          :next_call nil
          :source_unchanged true
          :committed false
          :mutation_attempted false
          :write_authority false}
         evidence))

(defn helper-extraction-summary
  "One compact visible summary whose length is constant in the caller count.

  The committed block is gated on the receipt's own `:committed`, so the visible
  check marks and the structured receipt can never disagree; the refusal face
  uses the same envelope rules alias_migration does, so every discriminating
  fact the structured receipt carries is also in the text."
  [result]
  (if (and (:ok result) (true? (:committed result)))
    (let [verification (:verification result)]
      (format (str "helper_extraction\n"
                   "  %s helpers \u00b7 %s caller files \u00b7 %s sites \u00b7 aliases %s \u00b7 %s\n\n"
                   "\u2713 one atomic transaction committed\n"
                   "\u2713 proof %s in a fresh process \u00b7 %s\n"
                   "\u2713 terminal evidence \u00b7 per-caller detail at %s")
              (:helpers result) (:caller_files result) (:sites result)
              (pr-str (:alias_histogram result))
              (mcp-operation/format-elapsed-ms (:elapsed_ms result))
              (pr-str (:profile verification))
              (pr-str (select-keys verification
                                   [:status :structural_callers :helper_behaviors
                                    :compiled_callers]))
              (:details_path result)))
    (bounded-refusal-text
      (str/join
       "\n"
       (remove
        nil?
        [(format (str "helper_extraction\n"
                      "  refused \u00b7 %s \u00b7 %s\n\n"
                      "%s")
                 (or (:error_type result) (:status result) "unknown-error")
                 (mcp-operation/format-elapsed-ms (:elapsed_ms result))
                 (if (or (:source_unchanged result) (:source-unchanged result))
                   "\u2713 source unchanged"
                   "\u26a0 source state requires structured receipt review"))
         ;; @spec MCP-OP-HELPER-010
         ;; @spec MCP-OP-HELPER-016
         ;; NO generic retry prescription. When `next_call` is nil there is no
         ;; mechanically known correction, and "correct the request and retry
         ;; once" tells a caller to do the one thing that reproduces the
         ;; outcome — measured on a real verification-failed receipt whose
         ;; next_call was null and whose text said exactly that. The cause is
         ;; what is known; the absence of a continuation is stated by
         ;; `rendered-next-call` on the line below.
         (when-let [cause (or (:error result) (:remedy result))]
           (str "\u2192 " cause))
         (refusal-fact-line result)
         (when-let [remedy (:remedy result)]
           (str "remedy \u00b7 " remedy))
         (rendered-next-call result)])))))

;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-008
;; @spec MCP-OP-HELPER-011
(defn handle-helper-extraction
  "Stable callback that plans, stages, proves, and publishes one O(1) receipt."
  [_exchange params callback]
  (mcp-operation/invoke!
    {:execute
     (fn []
       (let [started (System/nanoTime)
             normalized (json/parse-string (json/generate-string params) true)
             record! (fn [state result]
                       (when state
                         (telemetry/record-helper-call!
                           state params result
                           {:total_ms (/ (double (- (System/nanoTime) started))
                                         1000000.0)}))
                       result)]
         (if-not @runtime-config
           ;; @spec MCP-OP-HELPER-010
           ;; every refusal this handler emits carries the closed envelope,
           ;; `next_call nil` included. A refusal that merely omits next_call
           ;; is indistinguishable, to a caller reading either face, from one
           ;; whose continuation was dropped on the way out.
             (helper-extraction-refusal
             "server-not-initialized"
             "helper_extraction server is not initialized"
             {:remedy "Restart the configured clj-surgeon MCP server."})
           (let [workspace-router (or (:workspace-router @runtime-config)
                                      (workspace/router @runtime-config))
                 routed (workspace/resolve-request workspace-router normalized)]
             (if-not (:ok routed)
               ;; @spec MCP-OP-HELPER-010
               ;; a workspace-routing refusal is a helper_extraction refusal
               ;; and wears the same envelope; the router's own cause and
               ;; remedy travel verbatim inside it
               ;; @spec MCP-OP-HELPER-010
               ;; through the boundary's own normalizer as well, so a routing
               ;; refusal wears exactly the envelope every other pre-write
               ;; refusal wears
               (let [result (helper-extraction/normalize-refusal
                              (merge (helper-extraction-refusal
                                      (or (some-> (:error_type routed) name)
                                          (some-> (:error-type routed) name)
                                          "invalid-workspace-root")
                                      (or (:error routed) "The workspace root could not be resolved")
                                      {})
                                    (dissoc routed :ok :next_call)))]
                 (record! (or (:telemetry (:config routed))
                              (:telemetry @runtime-config)) result))
               ;; the same receipt-directory derivation alias_migration uses:
               ;; the routed workspace's own LOCAL-STATE directory, outside the
               ;; tree this verb mutates
               (let [routed-config (resolve-verification-config (:config routed))
                     receipt-dir (str (or (:receipt-dir routed-config)
                                          (default-receipt-dir
                                            (:project-root routed-config))))]
                 (record! (:telemetry routed-config)
                           (assoc (helper-extraction/execute!
                                    (assoc routed-config :receipt-dir receipt-dir)
                                    (assoc (:params routed)
                                           :workspace_root (:workspace-root routed)))
                                  :workspace_root (:workspace-root routed)))))))))
     :summarize helper-extraction-summary
     :callback callback}))

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
           census-tool/relation-census-tool
           alias-migration-tool
           (helper-extraction/tool)
           admit-tool/admit-clojure-patch-tool
           feature-thread/feature-thread-tool]
    :edit [edit-clojure-tool]
    (throw (ex-info "Unsupported MCP tool profile"
                    {:profile profile
                     :supported [:full :edit]}))))

(defn all-tools
  []
  (tools-for-profile (:tool-profile @runtime-config)))
