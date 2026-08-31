(ns clj-surgeon.mcp-elaborator-receipt
  "Source-free ledgers, receipts, and promotion decisions."
  (:require
   [clj-surgeon.mcp-elaborator-intent :as intent]
   [clj-surgeon.mcp-elaborator-policy :as policy]))

(defn- rate-window
  [window]
  (when (map? window)
    (select-keys window [:usedPercent :used_percent
                         :windowDurationMins :window_duration_mins
                         :resetsAt :resets_at])))

(defn rate-limit-receipt
  "Allowlist only non-secret Spark meter fields for durable persistence."
  [raw]
  (when-let [window (policy/meter-window raw)]
    {:limit_id "codex_bengalfox"
     :limit_name (:limitName window)
     :primary (rate-window (:primary window))
     :secondary (rate-window (:secondary window))}))

(defn verification-receipt
  "Project ordinary verifier evidence without paths, commands, or output bytes."
  [verification]
  (when (map? verification)
    (select-keys verification
                 [:ok :profile :profile-source :profile-sha256
                  :acceptance :process-outcome :exit :elapsed_ms
                  :output-bytes :output-sha256 :output-truncated
                  :status :passed :verification_complete])))

;; @spec MCP-OP-ELAB-010
(defn ledger-row
  [evidence]
  (let [runtime (:runtime evidence)]
    {:schema "clj-surgeon.embedded-elaborator-ledger.v1"
     :timestamp (:timestamp evidence)
     :intent_sha256 (:intent_sha256 evidence)
     :model (:model runtime)
     :cli_sha256 (:cli_sha256 runtime)
     :schema_sha256 (:schema_sha256 runtime)
     :auth_identity_sha256 (:auth_identity_sha256 runtime)
     :turn_id (:turn_id evidence)
     :input_tokens (:input_tokens evidence)
     :output_tokens (:output_tokens evidence)
     :latency_ms (:latency_ms evidence)
     :result_class (:result_class evidence)
     :alarm (:alarm evidence)
     :circuit_open (:circuit_open evidence)
     :quota_reason (:quota_reason evidence)
     :elaboration_sha256 (:elaboration_sha256 evidence)
     :rate_limits_before (rate-limit-receipt (:rate_limits_before evidence))
     :rate_limits_after (rate-limit-receipt (:rate_limits_after evidence))}))

;; @spec MCP-OP-ELAB-011
(defn quota-decision
  [{:keys [rolling_24h_used_percent meter_present meter_consistent]}]
  (if-not (and meter_present meter_consistent (number? rolling_24h_used_percent))
    {:alarm false
     :circuit_open true
     :reason "meter-unavailable-or-inconsistent"
     :caller_override_allowed false
     :alarm_threshold_percent policy/alarm-threshold-percent
     :circuit_threshold_percent policy/circuit-threshold-percent}
    {:alarm (>= rolling_24h_used_percent policy/alarm-threshold-percent)
     :circuit_open (>= rolling_24h_used_percent policy/circuit-threshold-percent)
     :reason (cond
               (>= rolling_24h_used_percent policy/circuit-threshold-percent)
               "rolling-budget-circuit"
               (>= rolling_24h_used_percent policy/alarm-threshold-percent)
               "rolling-budget-alarm"
               :else "within-budget")
     :caller_override_allowed false
     :alarm_threshold_percent policy/alarm-threshold-percent
     :circuit_threshold_percent policy/circuit-threshold-percent}))

;; @spec MCP-OP-ELAB-012
(defn project-receipt
  [evidence]
  (let [runtime (:runtime evidence)]
    {:schema "clj-surgeon.embedded-elaborator-receipt.v1"
     :intent_sha256 (:intent_sha256 evidence)
     :elaboration_sha256 (when-let [replacement (:replacement evidence)]
                           (intent/sha256 replacement))
     :elaborated_by {:model (:model runtime)
                     :cli_sha256 (:cli_sha256 runtime)
                     :schema_sha256 (:schema_sha256 runtime)
                     :auth_identity_sha256 (:auth_identity_sha256 runtime)
                     :isolation_policy_version policy/isolation-policy-version}
     :turn_count (:turn_count evidence)
     :latency_ms (:latency_ms evidence)
     :input_tokens (:input_tokens evidence)
     :output_tokens (:output_tokens evidence)
     :result_class (:result_class evidence)
     :tool_item_observed (:tool_item_observed evidence)
     :reroute_observed (:reroute_observed evidence)
     :ordinary (:ordinary evidence)}))

;; @spec MCP-OP-ELAB-012
;; @spec MCP-OP-ELAB-018
(defn receipt-consistency
  [evidence]
  (let [pairs [[:intent_sha256 :bound_intent_sha256]
               [:elaboration_sha256 :bound_elaboration_sha256]
               [:transaction_hash :bound_transaction_hash]
               [:read_back_hash :bound_read_back_hash]
               [:verifier_hash :bound_verifier_hash]]
        consistent? (every? (fn [[left right]]
                              (let [left-value (get evidence left)
                                    right-value (get evidence right)]
                                (and (string? left-value)
                                     (re-matches #"[0-9a-f]{64}" left-value)
                                     (= left-value right-value))))
                            pairs)]
    (if consistent?
      {:verified true}
      {:verified false :error_type "receipt-hash-mismatch"})))

;; @spec MCP-OP-ELAB-013
(defn d1-record
  [cases]
  {:eligible (count cases)
   :invoked (count cases)
   :synthetic_case false
   :authority_hashes_retained (boolean (seq cases))
   :phase_clocks_retained (boolean (seq cases))
   :emitted_byte_counts_retained (boolean (seq cases))})

(defn- d1-case-pass?
  [case]
  (and (= 1 (:candidate_count case))
       (zero? (:authority_fields case))
       (zero? (:tool_items case))
       (zero? (:reroutes case))
       (:guard_equal case)
       (:ordinary_writer case)
       (:verified case)
       (:effect_equal case)
       (<= (:elaborated_wall_ms case) (:hand_typed_wall_ms case))
       (zero? (:automatic_retries case))
       (false? (:secret_exposure case))
       (:cleanup_ok case)))

;; @spec MCP-OP-ELAB-014
(defn d1-decision
  [cases]
  (let [pass? (and (seq cases) (every? d1-case-pass? cases))]
    {:pass (boolean pass?)
     :promotion_blocked (not (boolean pass?))}))

;; @spec MCP-OP-ELAB-015
(defn d2-decision
  [evidence]
  (let [tripwire? (some true?
                        ((juxt :wrong_subject :weakened_guard :tool_effect
                               :secret_leak :reroute :automatic_replay
                               :verification_bypass :receipt_mismatch
                               :rollback_failure :surviving_child)
                         evidence))
        slow? (let [windows (:p95_windows_ms evidence)]
                (and (= 2 (count windows))
                     (every? #(> % 10000) windows)))
        disabled? (or tripwire? slow? (:verified_below_control evidence)
                      (:fallback_slower evidence)
                      (:quota_alarm_unacknowledged evidence))]
    {:circuit_open (boolean disabled?)
     :offering_disabled (boolean disabled?)}))

;; @spec MCP-OP-ELAB-016
(defn performance-claim
  [evidence]
  {:claim_allowed
   (boolean
     (and (:same_wall_class evidence)
          (:exact_correctness evidence)
          (:subject_identity evidence)
          (:effect_identity evidence)
          (:ordinary_verification evidence)
          (:losses_assigned evidence)
          (< (:elaborated_complete_wall_ms evidence)
             (:ordinary_complete_wall_ms evidence))
          (:ordinary_schema_bytes_recorded evidence)
          (:ordinary_pre_first_call_wall_recorded evidence)))})
