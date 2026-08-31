(ns clj-surgeon.mcp-embedded-elaborator-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clj-surgeon.mcp-elaborator-intent :as intent]
   [clj-surgeon.mcp-elaborator-policy :as policy]
   [clj-surgeon.mcp-elaborator-supervisor :as supervisor]
   [clj-surgeon.mcp-schema :as schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private missing-api {})

(defn- call-api
  [symbol & args]
  (try
    (if-let [f (requiring-resolve symbol)]
      (apply f args)
      missing-api)
    (catch java.io.FileNotFoundException _ missing-api)
    (catch IllegalArgumentException _ missing-api)))

(def ^:private old-1023 (apply str (repeat 1023 "x")))
(def ^:private old-1024 (apply str (repeat 1024 "x")))
(def ^:private old-2048 (apply str (repeat 2048 "x")))

(defn- request
  ([old decision]
   (request old decision nil))
  ([old decision extra]
   (cond->
     {:workspace_root "/private/tmp/elaborator-fixture"
      :edits [{:file "src/demo.clj"
               :within {:form "large-owner"}
               :from old
               :to nil
               :matches 1}]
      :elaborate {:decision decision}}
     extra (merge extra))))

(def ^:private capture
  {:workspace_root "/private/tmp/elaborator-fixture"
   :file "src/demo.clj"
   :owner "large-owner"
   :source_sha256 (apply str (repeat 64 "a"))
   :owner_source old-2048})

(def ^:private exact-runtime
  {:model "gpt-5.3-codex-spark"
   :reported_model "gpt-5.3-codex-spark"
   :provider "openai"
   :account_type "chatgpt"
   :plan_type "pro"
   :cli_version "codex-cli 0.149.1"
   :cli_sha256 (apply str (repeat 64 "b"))
   :schema_sha256 (apply str (repeat 64 "c"))
   :auth_identity_sha256 (apply str (repeat 64 "d"))
   :allow_provider_model_fallback false
   :reroutes []})

;; @spec MCP-OP-ELAB-001
(deftest immutable-isolation-receipt-is-the-admission-root
  (let [receipt (call-api
                  'clj-surgeon.mcp-elaborator-policy/admission-receipt)]
    (is (= true (:ok receipt)))
    (is (= "PASS" (:verdict receipt)))
    (is (= "H-S" (:strongest_off_configuration receipt)))
    (is (= "3c2cc192d56cb781a4db0c1a9f80eef86da0ba28"
           (:commit receipt)))
    (is (= "289b0b4eb25fec7eb770d2c8b1dc777811ccc9f347a23fff6b60455c3edff0ad"
           (:receipt_sha256 receipt)))
    (is (= "432cf51484e3c8bde2dad6874fa824a5c3429ace9072bd27cec40c179140cddf"
           (:hardening_config_sha256 receipt)))
    (is (= 60 (:hostile_turns receipt)))
    (is (= 0 (:tool_attempts receipt)))
    (is (= 0 (:side_effects receipt)))
    (is (= 0 (:orphans receipt)))))

;; @spec MCP-OP-ELAB-002
(deftest prepared-hole-admission-is-closed-and-byte-exact
  (let [valid (call-api 'clj-surgeon.mcp-elaborator-intent/validate-request
                        (request old-1024 (apply str (repeat 256 "d"))))
        cases [[(request old-1023 "d") "old-body-too-small"]
               [(request old-1024 (apply str (repeat 257 "d")))
                "decision-ratio-exceeded"]
               [(request old-2048 (apply str (repeat 513 "d")))
                "decision-too-large"]
               [(assoc-in (request old-2048 "d") [:edits 0 :to] ":literal")
                "mixed-edit-authority"]
               [(assoc (request old-2048 "d") :edits
                       [(:edits (request old-2048 "d"))
                        (:edits (request old-2048 "d"))])
                "exactly-one-edit-required"]
               [(assoc-in (request old-2048 "d") [:edits 0 :matches] 2)
                "exactly-one-match-required"]
               [(assoc-in (request old-2048 "d") [:edits 0 :within]
                          {:namespace true})
                "named-owner-required"]
               [(assoc-in (request old-2048 "d") [:elaborate :model] "other")
                "unknown-elaboration-field"]
               [(assoc (request old-2048 "d") :verify "fast")
                "caller-control-forbidden"]]]
    (is (= true (:ok valid)))
    (is (= 1024 (:old_body_bytes valid)))
    (is (= 256 (:decision_bytes valid)))
    (doseq [[input error-type] cases]
      (let [result (call-api
                     'clj-surgeon.mcp-elaborator-intent/validate-request input)]
        (is (= false (:ok result)))
        (is (= error-type (:error_type result)))
        (is (= true (:source_unchanged result)))))))

;; @spec MCP-OP-ELAB-003
(deftest identity-is-caller-owned-and-never-enters-model-input
  (let [result (call-api 'clj-surgeon.mcp-elaborator-intent/capture-intent
                         (request old-2048 "Batch the selected body.") capture)
        model-input (:model_input result)
        authority (:authority_intent result)]
    (is (= true (:ok result)))
    (is (= #{:old_body :decision} (set (keys model-input))))
    (is (= old-2048 (:old_body model-input)))
    (is (= "Batch the selected body." (:decision model-input)))
    (is (= "/private/tmp/elaborator-fixture" (:workspace_root authority)))
    (is (= "src/demo.clj" (:file authority)))
    (is (= "large-owner" (:owner authority)))
    (is (= 1 (:matches authority)))
    (is (= (:source_sha256 capture) (:source_sha256 authority)))
    (is (re-matches #"[0-9a-f]{64}" (or (:intent_sha256 result) "")))
    (doseq [forbidden [:workspace_root :file :owner :within :selector :matches
                       :operation :verify :receipt]]
      (is (= false (contains? model-input forbidden))))))

;; @spec MCP-OP-ELAB-004
(deftest hostile-output-is-consumed-once-or-rejected-whole
  (let [accepted (call-api
                   'clj-surgeon.mcp-elaborator-adapter/consume-turn
                   [{:type "userMessage"}
                    {:type "reasoning"}
                    {:type "agentMessage" :phase "final_answer"
                     :text "{\"replacement\":\":complete\"}"}])
        rejected [[[{:type "commandExecution"}] "non-text-item"]
                  [[{:type "agentMessage" :phase "final_answer"
                     :text "{\"replacement\":\":a\"}"}
                    {:type "agentMessage" :phase "final_answer"
                     :text "{\"replacement\":\":b\"}"}]
                   "multiple-candidates"]
                  [[{:type "agentMessage" :phase "final_answer"
                     :text "{\"replacement\":\":a\",\"file\":\"x\"}"}]
                   "unknown-output-field"]
                  [[{:type "agentMessage" :phase "final_answer"
                     :text "{\"replacement\":\"\"}"}]
                   "blank-replacement"]
                  [[{:type "agentMessage" :phase "final_answer"
                     :text "not-json"}]
                   "malformed-output"]]]
    (is (= true (:ok accepted)))
    (is (= ":complete" (:replacement accepted)))
    (is (= 1 (:candidate_count accepted)))
    (is (= 1 (:turn_count accepted)))
    (doseq [[events error-type] rejected]
      (let [result (call-api
                     'clj-surgeon.mcp-elaborator-adapter/consume-turn events)]
        (is (= false (:ok result)))
        (is (= error-type (:error_type result)))
        (is (= nil (:replacement result)))
        (is (= true (:source_unchanged result)))))))

;; @spec MCP-OP-ELAB-005
(deftest accepted-body-reenters-only-the-ordinary-writer
  (let [completed (call-api
                    'clj-surgeon.mcp-elaborator-intent/complete-request
                    (request old-2048 "Replace the body.") ":replacement")]
    (is (= true (:ok completed)))
    (is (= ":replacement" (get-in completed [:request :edits 0 :to])))
    (is (= old-2048 (get-in completed [:request :edits 0 :from])))
    (is (= "src/demo.clj" (get-in completed [:request :edits 0 :file])))
    (is (= "large-owner" (get-in completed [:request :edits 0 :within :form])))
    (is (= 1 (get-in completed [:request :edits 0 :matches])))
    (is (= false (contains? (:request completed) :elaborate)))
    (is (= "edit_clojure" (:ordinary_operation completed)))
    (is (= true (:fresh_capture_required completed)))
    (is (= false (:write_authority completed)))))

;; @spec MCP-OP-ELAB-006
(deftest boot-supervisor-never-owns-server-readiness
  (let [plan (call-api 'clj-surgeon.mcp-elaborator-adapter/boot-plan)]
    (is (= :background (:start_mode plan)))
    (is (= 1000 (:initialize_timeout_ms plan)))
    (is (= 10000 (:warmup_timeout_ms plan)))
    (is (= true (:fixed_no_effect_warmup plan)))
    (is (= true (:fresh_thread_per_intent plan)))
    (is (= 1 (:max_in_flight plan)))
    (is (= false (:readiness_depends_on_child plan)))
    (is (= false (:caller_may_start plan)))
    (is (= false (:caller_may_wait plan)))
    (is (= :ordinary-edits-healthy (:boot_failure_server_state plan)))))

;; @spec MCP-OP-ELAB-007
(deftest exact-model-pin-has-no-fallback
  (let [accepted (call-api
                   'clj-surgeon.mcp-elaborator-policy/admission-decision
                   exact-runtime)
        rejected (for [runtime [(assoc exact-runtime :reported_model "gpt-5.6-sol")
                                (assoc exact-runtime
                                       :allow_provider_model_fallback true)
                                (assoc exact-runtime :reroutes ["fallback"])
                                (assoc exact-runtime :account_type "api")
                                (dissoc exact-runtime :schema_sha256)]]
                   (call-api
                     'clj-surgeon.mcp-elaborator-policy/admission-decision
                     runtime))]
    (is (= true (:ok accepted)))
    (is (= "gpt-5.3-codex-spark" (:model accepted)))
    (is (= false (:fallback_enabled accepted)))
    (doseq [result rejected]
      (is (= false (:ok result)))
      (is (= "elaborator-unavailable" (:error_type result)))
      (is (= false (:fallback_used result))))))

;; @spec MCP-OP-ELAB-008
(deftest one-turn-failures-never-replay-or-retain-partials
  (doseq [failure [:timeout :cancelled :crash :eof :protocol-desync :http-401
                   :http-429 :malformed :tool-item :quota-stop
                   :cleanup-failure]]
    (let [result (call-api
                   'clj-surgeon.mcp-elaborator-adapter/failure-decision failure)]
      (is (= false (:ok result)))
      (is (= 1 (:turn_count result)))
      (is (= 0 (:candidate_count result)))
      (is (= false (:replay result)))
      (is (= nil (:replacement result)))
      (is (= true (:source_unchanged result)))
      (is (= true (:ordinary_path_available result)))))
  (let [limits (call-api 'clj-surgeon.mcp-elaborator-adapter/turn-limits)]
    (is (= 10000 (:deadline_ms limits)))
    (is (= 32768 (:output_byte_ceiling limits)))))

;; @spec MCP-OP-ELAB-009
(deftest cleanup-targets-only-the-owned-process-group
  (let [plan (call-api 'clj-surgeon.mcp-elaborator-adapter/shutdown-plan
                       {:pid 4201 :pgid 4201 :cwd "/private/tmp/spark"})]
    (is (= [[:close-stdin 4201]
            [:wait-ms 1000]
            [:signal-pgid 4201 :sigterm]
            [:wait-ms 1000]
            [:signal-pgid 4201 :sigkill]]
           (:actions plan)))
    (is (= 4201 (:owned_pid plan)))
    (is (= 4201 (:owned_pgid plan)))
    (is (= "/private/tmp/spark" (:owned_cwd plan)))
    (is (= false (:may_signal_unrelated plan)))
    (is (= true (:record_ancestry plan)))
    (is (= true (:require_empty_group plan)))))

;; @spec MCP-OP-ELAB-010
(deftest ledger-is-append-only-attributable-and-source-free
  (let [row (call-api
              'clj-surgeon.mcp-elaborator-receipt/ledger-row
              {:timestamp "2026-08-30T00:00:00Z"
               :intent_sha256 (apply str (repeat 64 "1"))
               :turn_id "turn-1"
               :input_tokens 10 :output_tokens 20 :latency_ms 30
               :result_class "accepted"
               :elaboration_sha256 (apply str (repeat 64 "2"))
               :runtime exact-runtime
               :rate_limits_before {:used_percent 51}
               :rate_limits_after {:used_percent 53}
               :source old-2048 :decision "secret decision"
               :file "src/demo.clj" :owner "large-owner"
               :replacement ":replacement" :email "hidden@example.com"})]
    (is (= "clj-surgeon.embedded-elaborator-ledger.v1" (:schema row)))
    (is (= "gpt-5.3-codex-spark" (:model row)))
    (is (= "turn-1" (:turn_id row)))
    (is (= 10 (:input_tokens row)))
    (is (= 20 (:output_tokens row)))
    (is (= 30 (:latency_ms row)))
    (doseq [forbidden [:source :decision :file :owner :workspace_root
                       :replacement :auth :email :token]]
      (is (= false (contains? row forbidden))))))

;; @spec MCP-OP-ELAB-011
(deftest quota-alarm-and-circuit-thresholds-are-product-owned
  (doseq [[percent alarm? open?]
          [[0 false false] [79 false false] [80 true false]
           [89 true false] [90 true true] [100 true true]]]
    (let [result (call-api
                   'clj-surgeon.mcp-elaborator-receipt/quota-decision
                   {:rolling_24h_used_percent percent
                    :meter_present true :meter_consistent true})]
      (is (= alarm? (:alarm result)))
      (is (= open? (:circuit_open result)))
      (is (= 80 (:alarm_threshold_percent result)))
      (is (= 90 (:circuit_threshold_percent result)))))
  (doseq [meter [{:meter_present false :meter_consistent true}
                 {:meter_present true :meter_consistent false}]]
    (let [result (call-api
                   'clj-surgeon.mcp-elaborator-receipt/quota-decision meter)]
      (is (= true (:circuit_open result)))
      (is (= "meter-unavailable-or-inconsistent" (:reason result)))
      (is (= false (:caller_override_allowed result))))))

;; @spec MCP-OP-ELAB-012
(deftest receipts-bind-generation-to-ordinary-effect-without-authority
  (let [receipt (call-api
                  'clj-surgeon.mcp-elaborator-receipt/project-receipt
                  {:intent_sha256 (apply str (repeat 64 "1"))
                   :replacement ":replacement"
                   :runtime exact-runtime
                   :turn_count 1 :latency_ms 25
                   :input_tokens 10 :output_tokens 20
                   :result_class "accepted"
                   :tool_item_observed false :reroute_observed false
                   :ordinary {:operation "edit_clojure"
                              :receipt_hash (apply str (repeat 64 "3"))
                              :verification_complete true}
                   :source old-2048 :decision "hidden"
                   :file "src/demo.clj"})]
    (is (re-matches #"[0-9a-f]{64}" (or (:intent_sha256 receipt) "")))
    (is (re-matches #"[0-9a-f]{64}" (or (:elaboration_sha256 receipt) "")))
    (is (= "gpt-5.3-codex-spark" (get-in receipt [:elaborated_by :model])))
    (is (= 1 (:turn_count receipt)))
    (is (= "edit_clojure" (get-in receipt [:ordinary :operation])))
    (is (= true (get-in receipt [:ordinary :verification_complete])))
    (doseq [forbidden [:source :decision :replacement :file :owner
                       :prepared_request :next_call :retry]]
      (is (= false (contains? receipt forbidden))))))

;; @spec MCP-OP-ELAB-013
(deftest d1-records-real-eligibility-and-never-manufactures-a-case
  (let [zero (call-api 'clj-surgeon.mcp-elaborator-receipt/d1-record [])
        one (call-api
              'clj-surgeon.mcp-elaborator-receipt/d1-record
              [{:old_body_bytes 2048 :decision_bytes 64
                :candidate_count 1 :tool_items 0 :reroutes 0
                :guard_equal true :effect_equal true :verified true
                :elaborated_wall_ms 900 :hand_typed_wall_ms 1000}])]
    (is (= 0 (:eligible zero)))
    (is (= 0 (:invoked zero)))
    (is (= false (:synthetic_case zero)))
    (is (= 1 (:eligible one)))
    (is (= 1 (:invoked one)))
    (is (= true (:authority_hashes_retained one)))
    (is (= true (:phase_clocks_retained one)))
    (is (= true (:emitted_byte_counts_retained one)))))

;; @spec MCP-OP-ELAB-014
(deftest d1-passes-only-exact-one-shot-parity-and-wall-win
  (let [good {:candidate_count 1 :authority_fields 0 :tool_items 0 :reroutes 0
              :guard_equal true :ordinary_writer true :verified true
              :effect_equal true :elaborated_wall_ms 900
              :hand_typed_wall_ms 1000 :automatic_retries 0
              :secret_exposure false :cleanup_ok true}
        accepted (call-api
                   'clj-surgeon.mcp-elaborator-receipt/d1-decision [good])]
    (is (= true (:pass accepted)))
    (doseq [bad [(assoc good :candidate_count 2)
                 (assoc good :authority_fields 1)
                 (assoc good :tool_items 1)
                 (assoc good :guard_equal false)
                 (assoc good :verified false)
                 (assoc good :effect_equal false)
                 (assoc good :elaborated_wall_ms 1001)
                 (assoc good :automatic_retries 1)
                 (assoc good :secret_exposure true)
                 (assoc good :cleanup_ok false)]]
      (let [result (call-api
                     'clj-surgeon.mcp-elaborator-receipt/d1-decision [bad])]
        (is (= false (:pass result)))
        (is (= true (:promotion_blocked result)))))))

;; @spec MCP-OP-ELAB-015
(deftest d2-tripwires-open-the-circuit
  (let [good {:wrong_subject false :weakened_guard false :tool_effect false
              :secret_leak false :reroute false :automatic_replay false
              :verification_bypass false :receipt_mismatch false
              :rollback_failure false :surviving_child false
              :p95_windows_ms [9000 9000] :verified_below_control false
              :fallback_slower false :quota_alarm_unacknowledged false}
        accepted (call-api
                   'clj-surgeon.mcp-elaborator-receipt/d2-decision good)]
    (is (= false (:circuit_open accepted)))
    (doseq [bad [(assoc good :wrong_subject true)
                 (assoc good :weakened_guard true)
                 (assoc good :tool_effect true)
                 (assoc good :secret_leak true)
                 (assoc good :reroute true)
                 (assoc good :automatic_replay true)
                 (assoc good :verification_bypass true)
                 (assoc good :receipt_mismatch true)
                 (assoc good :rollback_failure true)
                 (assoc good :surviving_child true)
                 (assoc good :p95_windows_ms [10001 10002])
                 (assoc good :verified_below_control true)
                 (assoc good :fallback_slower true)
                 (assoc good :quota_alarm_unacknowledged true)]]
      (let [result (call-api
                     'clj-surgeon.mcp-elaborator-receipt/d2-decision bad)]
        (is (= true (:circuit_open result)))
        (is (= true (:offering_disabled result)))))))

;; @spec MCP-OP-ELAB-016
(deftest performance-claim-requires-same-stratum-complete-wall-win
  (let [base {:same_wall_class true :exact_correctness true
              :subject_identity true :effect_identity true
              :ordinary_verification true :losses_assigned true
              :elaborated_complete_wall_ms 900
              :ordinary_complete_wall_ms 1000
              :ordinary_schema_bytes_recorded true
              :ordinary_pre_first_call_wall_recorded true}
        accepted (call-api
                   'clj-surgeon.mcp-elaborator-receipt/performance-claim base)]
    (is (= true (:claim_allowed accepted)))
    (doseq [bad [(assoc base :same_wall_class false)
                 (assoc base :exact_correctness false)
                 (assoc base :subject_identity false)
                 (assoc base :effect_identity false)
                 (assoc base :ordinary_verification false)
                 (assoc base :losses_assigned false)
                 (assoc base :elaborated_complete_wall_ms 1000)
                 (assoc base :ordinary_schema_bytes_recorded false)
                 (assoc base :ordinary_pre_first_call_wall_recorded false)]]
      (is (= false
             (:claim_allowed
               (call-api
                 'clj-surgeon.mcp-elaborator-receipt/performance-claim bad)))))))

;; @spec MCP-OP-ELAB-017
(deftest b0432c25-is-a-permanent-identity-prohibition
  (let [falsifier (call-api
                    'clj-surgeon.mcp-elaborator-intent/identity-falsifier)]
    (is (= "b0432c25" (:receipt falsifier)))
    (is (= "wrong-explicit-file-with-correct-reference"
           (:observed_failure falsifier)))
    (is (= false (:may_assert_reference falsifier)))
    (is (= false (:may_assert_file falsifier)))
    (is (= false (:may_assert_owner falsifier)))
    (is (= false (:may_assert_subject_identity falsifier)))
    (is (= true (:identity_received_from_caller falsifier)))
    (is (= true (:later_hardening_and_ratification_required falsifier)))))

;; @spec MCP-OP-ELAB-018
(deftest permanent-falsifier-boundaries-fail-closed
  (testing "the wall classifier has exactly one admitted boundary hole"
    (doseq [[old decision admitted?]
            [[old-1023 "d" false]
             [old-1024 (apply str (repeat 256 "d")) true]
             [old-1024 (apply str (repeat 257 "d")) false]]]
      (is (= admitted?
             (:ok (call-api
                    'clj-surgeon.mcp-elaborator-intent/validate-request
                    (request old decision)))))))
  (testing "a null hole without elaborate remains the ordinary refusal"
    (let [result (call-api
                   'clj-surgeon.mcp-elaborator-intent/classify-request
                   (dissoc (request old-2048 "d") :elaborate))]
      (is (= :ordinary (:path result)))
      (is (= "ordinary-null-refusal" (:expected_result result)))
      (is (= false (:contact_child result)))))
  (testing "replacement text is data even when it resembles identity"
    (let [result (call-api
                   'clj-surgeon.mcp-elaborator-intent/complete-request
                   (request old-2048 "d")
                   "{:file \"other.clj\" :owner wrong}")]
      (is (= "src/demo.clj" (get-in result [:request :edits 0 :file])))
      (is (= "large-owner" (get-in result [:request :edits 0 :within :form])))
      (is (= "{:file \"other.clj\" :owner wrong}"
             (get-in result [:request :edits 0 :to])))))
  (testing "receipt mismatches cannot claim verification"
    (let [result (call-api
                   'clj-surgeon.mcp-elaborator-receipt/receipt-consistency
                   {:intent_sha256 "a" :bound_intent_sha256 "b"
                    :elaboration_sha256 "c" :bound_elaboration_sha256 "c"
                    :transaction_hash "d" :bound_transaction_hash "d"
                    :read_back_hash "e" :bound_read_back_hash "e"
                    :verifier_hash "f" :bound_verifier_hash "f"})]
      (is (= false (:verified result)))
      (is (= "receipt-hash-mismatch" (:error_type result))))
    (is (= false
           (:verified
             (call-api
               'clj-surgeon.mcp-elaborator-receipt/receipt-consistency
               {})))))
  (testing "only the owned process group can be selected"
    (let [result (call-api
                   'clj-surgeon.mcp-elaborator-adapter/validate-owned-process
                   {:pid 42 :pgid 42 :actual_pgid 99})]
      (is (= false (:ok result)))
      (is (= false (:signal_allowed result))))))

;; @spec MCP-OP-ELAB-019
(deftest keepalive-is-disabled-and-idle-cells-are-exact
  (let [plan (call-api 'clj-surgeon.mcp-elaborator-adapter/keepalive-plan)]
    (is (= false (:enabled plan)))
    (is (= [0 60 240] (:idle_minutes plan)))
    (is (= [:first_bang :immediate_midstream] (:measurements_per_cell plan)))
    (is (= true (:fixed_source_free_no_effect_tick plan)))
    (is (= true (:fresh_thread plan)))
    (is (= true (:one_turn plan)))
    (is (= true (:isolation_gated plan)))
    (is (= true (:pin_verified plan)))
    (is (= true (:metered plan)))
    (is (= true (:receipted plan)))
    (is (= true (:suppressed_while_circuit_open plan)))
    (is (= :no-decay-means-no-spend (:default_decision plan)))))

(defn- delete-tree!
  [root]
  (when (.exists (io/file root))
    (doseq [file (reverse (file-seq (io/file root)))]
      (io/delete-file file true))))

(defn- wait-for-state
  [spark-supervisor expected timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [current (supervisor/state spark-supervisor)]
        (cond
          (= expected (:status current)) current
          (>= (System/currentTimeMillis) deadline) current
          :else (do (Thread/sleep 10) (recur)))))))

(defn- fake-config
  ([temporary mode]
   (fake-config temporary mode 100))
  ([temporary mode budget]
   (let [source (io/file "test/fixtures/embedded_elaborator/fake_app_server.py")
         fake (io/file temporary (str "fake-" mode ".py"))
         schema-file (.getCanonicalPath (io/file "deps.edn"))
         auth-file (io/file temporary "auth.json")]
     (io/copy source fake)
     (.setExecutable fake true true)
     (spit auth-file "{\"tokens\":{\"account_id\":\"fake-account\"}}")
     {:enabled? true
      :codex-path (.getCanonicalPath fake)
      :auth-file (.getCanonicalPath auth-file)
      :schema-file schema-file
      :expected-cli-sha256 (intent/sha256 (slurp fake))
      :expected-schema-sha256 (intent/sha256 (slurp schema-file))
      :auth-identity-sha256 (intent/sha256 "fake-account")
      :ledger-file (.getCanonicalPath (io/file temporary "ledger.jsonl"))
      :rolling-24h-call-budget budget})))

(defn- recursive-map-keys
  [value]
  (->> (tree-seq coll? seq value)
       (filter map?)
       (mapcat keys)
       set))

;; Green-only boundary witnesses added after frozen red 2145b753.
;; @spec MCP-OP-ELAB-001
;; @spec MCP-OP-ELAB-002
;; @spec MCP-OP-ELAB-006
(deftest public-schema-and-hardening-config-admit-only-the-one-hole-branch
  (let [valid {"workspace_root" "/private/tmp/elaborator-fixture"
               "edits" [{"file" "src/demo.clj"
                         "within" {"form" "large-owner"}
                         "from" old-1024
                         "to" nil
                         "matches" 1}]
               "elaborate" {"decision" "d"}}]
    (is (= (slurp "docs/observations/2026-08-30-spark-isolation-screen/hardening.config.toml")
           policy/hardening-config))
    (is (= "432cf51484e3c8bde2dad6874fa824a5c3429ace9072bd27cec40c179140cddf"
           (intent/sha256 policy/hardening-config)))
    (is (:ok (admission/authorize schema/editor-tool-schema valid)))
    (let [unicode-old (apply str (repeat 256 "💥"))
          unicode-valid (assoc-in valid ["edits" 0 "from"] unicode-old)]
      (is (= 1024 (intent/utf8-bytes unicode-old)))
      (is (:ok (admission/authorize schema/editor-tool-schema unicode-valid)))
      (is (:ok (intent/validate-request
                 (assoc-in (request old-1024 "d") [:edits 0 :from]
                           unicode-old)))))
    (is (false? (:ok (admission/authorize
                       schema/editor-tool-schema
                       (assoc-in valid ["elaborate" "model"] "other")))))
    (is (false? (:ok (admission/authorize
                       schema/editor-tool-schema
                       (assoc-in valid ["edits" 0 "to"] ":literal")))))
    (is (false? (:ok (admission/authorize
                       schema/editor-tool-schema
                       (dissoc valid "elaborate")))))
    (is (= "null" (get-in schema/elaborated-edit-schema
                          [:properties "to" :type])))
    (is (= 1 (get-in schema/elaborated-editor-schema
                     [:properties "edits" :maxItems])))))

;; @spec MCP-OP-ELAB-003
(deftest capture-admits-one-exact-nested-guard-without-sending-owner-identity
  (let [nested (str "(defn large-owner []\n" old-1024 "\n)")
        result (intent/capture-intent
                 (request old-1024 "d")
                 (assoc capture :owner_source nested))]
    (is (:ok result) (pr-str result))
    (is (= {:old_body old-1024 :decision "d"} (:model_input result)))
    (is (not (str/includes? (intent/canonical-json (:model_input result))
                            "large-owner")))))

;; @spec MCP-OP-ELAB-006
(deftest boot-failure-does-not-own-health-and-callers-never-start-it
  (let [started (System/nanoTime)
        spark-supervisor (supervisor/start-background! {:enabled? false})
        elapsed-ms (/ (- (System/nanoTime) started) 1000000.0)
        state (wait-for-state spark-supervisor :unavailable 1000)]
    (is (< elapsed-ms 100.0))
    (is (= :unavailable (:status state)))
    (is (= :operator-config-absent (:reason state)))
    (is (= false (:readiness_depends_on_child
                   (call-api 'clj-surgeon.mcp-elaborator-adapter/boot-plan))))
    (is (nil? (supervisor/stop! spark-supervisor)))))

;; @spec MCP-OP-ELAB-006
;; @spec MCP-OP-ELAB-009
(deftest shutdown-wins-the-background-boot-race-and-cleans-an-unpublished-child
  (let [temporary (str (java.nio.file.Files/createTempDirectory
                         "embedded-elaborator-stop-race-"
                         (make-array java.nio.file.attribute.FileAttribute 0)))
        spark-supervisor (supervisor/start-background!
                           (fake-config temporary "slow-boot"))]
    (try
      (Thread/sleep 75)
      (let [cleanup (supervisor/stop! spark-supervisor)]
        (is (= :stopped (:status (supervisor/state spark-supervisor))))
        (is (:cleanup_ok cleanup) (pr-str cleanup))
        (is (empty? (:remaining cleanup)))
        (is (not (.exists (io/file (:cwd cleanup))))))
      (Thread/sleep 300)
      (is (= :stopped (:status (supervisor/state spark-supervisor))))
      (finally
        (supervisor/stop! spark-supervisor)
        (delete-tree! temporary)))))

;; @spec MCP-OP-ELAB-006
;; @spec MCP-OP-ELAB-008
(deftest ordinary-and-unavailable-edit-paths-do-not-contact-or-wait-for-spark
  (let [ordinary (mcp-tool/execute-request!
                   {:project-root "." :public-operation "edit_clojure"}
                   {:edits []})]
    (is (false? (:ok ordinary)))
    (is (not= "elaborator-unavailable" (:error_type ordinary))))
  (let [root (.getCanonicalPath (io/file "."))
        unavailable (mcp-tool/execute-request!
                      {:project-root root :public-operation "edit_clojure"}
                      (assoc (request old-1024 "d") :workspace_root root))]
    (is (false? (:ok unavailable)))
    (is (= "elaborator-unavailable" (:error_type unavailable)))
    (is (= true (:source_unchanged unavailable)))
    (is (= true (:ordinary_path_available unavailable)))))

;; @spec MCP-OP-ELAB-004
;; @spec MCP-OP-ELAB-005
;; @spec MCP-OP-ELAB-006
;; @spec MCP-OP-ELAB-007
;; @spec MCP-OP-ELAB-008
;; @spec MCP-OP-ELAB-009
;; @spec MCP-OP-ELAB-010
;; @spec MCP-OP-ELAB-011
;; @spec MCP-OP-ELAB-012
(deftest supervised-jsonl-fake-proves-boot-turn-ledger-reentry-and-cleanup
  (let [temporary (str (java.nio.file.Files/createTempDirectory
                         "embedded-elaborator-test-"
                         (make-array java.nio.file.attribute.FileAttribute 0)))
        fake (.getCanonicalPath
               (io/file "test/fixtures/embedded_elaborator/fake_app_server.py"))
        schema-file (.getCanonicalPath (io/file "deps.edn"))
        auth-file (io/file temporary "auth.json")
        ledger-file (str (io/file temporary "ledger.jsonl"))
        workspace (io/file temporary "workspace")
        source-dir (io/file workspace "src")
        source-file (io/file source-dir "demo.clj")
        owner-source (str "(def large-owner \"" old-2048 "\")")
        _auth (spit auth-file "{\"tokens\":{\"account_id\":\"fake-account\"}}")
        config {:enabled? true
                :codex-path fake
                :auth-file (.getCanonicalPath auth-file)
                :schema-file schema-file
                :expected-cli-sha256 (intent/sha256 (slurp fake))
                :expected-schema-sha256 (intent/sha256 (slurp schema-file))
                :auth-identity-sha256 (intent/sha256 "fake-account")
                :ledger-file ledger-file
                :rolling-24h-call-budget 100}
        spark-supervisor (supervisor/start-background! config)]
    (try
      (.mkdirs source-dir)
      (spit source-file (str "(ns demo)\n" owner-source "\n"))
      (let [ready (wait-for-state spark-supervisor :available 5000)]
        (is (= :available (:status ready)) (pr-str ready))
        (is (= policy/model-slug (:model ready)))
        (let [turn (supervisor/elaborate!
                     spark-supervisor
                     {:model_input {:old_body owner-source
                                    :decision "Return the completed body."}
                      :intent_sha256 (apply str (repeat 64 "1"))})]
          (is (:ok turn) (pr-str turn))
          (is (= ":complete" (:replacement turn)))
          (is (= 1 (:turn_count turn)))
          (is (= 1 (:candidate_count turn)))
          (is (= 12 (:input_tokens turn)))
          (is (= 3 (:output_tokens turn))))
        (let [ordinary-request (atom nil)
              edit-request {:workspace_root (.getCanonicalPath workspace)
                            :edits [{:file "src/demo.clj"
                                     :within {:form "large-owner"}
                                     :from owner-source
                                     :to nil
                                     :matches 1}]
                            :elaborate {:decision "Return the completed body."}}
              result (supervisor/execute-edit!
                       spark-supervisor
                       {:project-root (.getCanonicalPath workspace)}
                       edit-request
                       (fn [completed]
                         (reset! ordinary-request completed)
                         {:ok true
                          :receipt_hash (apply str (repeat 64 "e"))
                          :verification_complete true
                          :verification {:ok true
                                         :profile "exact"
                                         :profile-sha256
                                         (apply str (repeat 64 "f"))
                                         :acceptance :exact-exit
                                         :process-outcome :pass
                                         :exit 0
                                         :cwd "/must/not/project"
                                         :argv ["must-not-persist"]}}))]
          (is (:ok result) (pr-str result))
          (is (= ":complete" (get-in @ordinary-request [:edits 0 :to])))
          (is (= owner-source (get-in @ordinary-request [:edits 0 :from])))
          (is (= "src/demo.clj" (get-in @ordinary-request [:edits 0 :file])))
          (is (not (contains? @ordinary-request :elaborate)))
          (is (= policy/model-slug (get-in result [:elaboration :elaborated_by :model])))
          (is (not (contains? (:elaboration result) :replacement)))
          (is (= "exact" (get-in result [:elaboration :ordinary
                                         :verification_receipt :profile])))
          (is (nil? (get-in result [:elaboration :ordinary
                                    :verification_receipt :cwd])))
          (is (nil? (get-in result [:elaboration :ordinary
                                    :verification_receipt :argv]))))
        (let [rows (mapv #(json/parse-string % true)
                         (remove empty? (str/split-lines (slurp ledger-file))))]
          (is (= 3 (count rows)))
          (is (every? #(= "clj-surgeon.embedded-elaborator-ledger.v1"
                          (:schema %)) rows))
          (is (every? #(not-any? (set (keys %))
                                 [:source :decision :file :owner
                                  :workspace_root :replacement :auth :email])
                      rows))
          (is (every? #(empty? (set/intersection
                                 #{:source :decision :file :owner :workspace_root
                                   :replacement :auth :email :token :accessToken}
                                 (recursive-map-keys %)))
                      rows)))
        (let [cleanup (supervisor/stop! spark-supervisor)]
          (is (:cleanup_ok cleanup) (pr-str cleanup))
          (is (empty? (:remaining cleanup)))
          (is (:signal_allowed cleanup))))
      (finally
        (supervisor/stop! spark-supervisor)
        (delete-tree! temporary)))))

;; @spec MCP-OP-ELAB-004
;; @spec MCP-OP-ELAB-007
;; @spec MCP-OP-ELAB-008
;; @spec MCP-OP-ELAB-009
(deftest hostile-jsonl-is-rejected-and-boot-failure-cleans-owned-groups
  (doseq [[mode expected-error] [["action" "non-text-item"]
                                 ["reroute" "reroute"]]]
    (let [temporary (str (java.nio.file.Files/createTempDirectory
                           (str "embedded-elaborator-" mode "-")
                           (make-array java.nio.file.attribute.FileAttribute 0)))
          spark-supervisor (supervisor/start-background!
                             (fake-config temporary mode))]
      (try
        (is (= :available (:status (wait-for-state spark-supervisor
                                                   :available 5000))))
        (let [result (supervisor/elaborate!
                       spark-supervisor
                       {:model_input {:old_body old-1024 :decision "d"}
                        :intent_sha256 (apply str (repeat 64 "4"))})]
          (is (false? (:ok result)) (pr-str result))
          (is (= expected-error (:error_type result)) (pr-str result))
          (is (nil? (:replacement result)))
          (is (= 1 (:turn_count result)))
          (is (= false (:replay result))))
        (let [cleanup (supervisor/stop! spark-supervisor)]
          (is (:cleanup_ok cleanup) (pr-str cleanup))
          (is (empty? (:remaining cleanup))))
        (finally
          (supervisor/stop! spark-supervisor)
          (delete-tree! temporary)))))
  (doseq [mode ["missing-model" "metadata-model" "other-provider"]]
    (let [temporary (str (java.nio.file.Files/createTempDirectory
                           "embedded-elaborator-boot-failure-"
                           (make-array java.nio.file.attribute.FileAttribute 0)))
          spark-supervisor (supervisor/start-background!
                             (fake-config temporary mode))]
      (try
        (let [failed (wait-for-state spark-supervisor :unavailable 5000)]
          (is (= :boot-admission-failed (:reason failed)) (pr-str failed))
          (is (true? (get-in failed [:cleanup :cleanup_ok])) (pr-str failed))
          (is (empty? (get-in failed [:cleanup :remaining]))))
        (finally
          (supervisor/stop! spark-supervisor)
          (delete-tree! temporary))))))

;; @spec MCP-OP-ELAB-008
;; @spec MCP-OP-ELAB-009
;; @spec MCP-OP-ELAB-010
(deftest oversize-stall-and-crashed-leader-kill-the-retained-owned-process-group
  (doseq [mode ["oversize-stall" "crash-descendant" "meter-backward"]]
    (let [temporary (str (java.nio.file.Files/createTempDirectory
                           (str "embedded-elaborator-" mode "-")
                           (make-array java.nio.file.attribute.FileAttribute 0)))
          config (fake-config temporary mode)
          spark-supervisor (supervisor/start-background! config)]
      (try
        (is (= :available (:status (wait-for-state spark-supervisor
                                                   :available 5000))))
        (let [started (System/nanoTime)
              result (supervisor/elaborate!
                       spark-supervisor
                       {:model_input {:old_body old-1024 :decision "d"}
                        :intent_sha256 (apply str (repeat 64 "8"))})
              elapsed-ms (/ (- (System/nanoTime) started) 1000000.0)
              stopped (supervisor/state spark-supervisor)
              rows (mapv #(json/parse-string % true)
                         (remove empty? (str/split-lines
                                          (slurp (:ledger-file config)))))]
          (is (false? (:ok result)) (pr-str result))
          (is (= 1 (:turn_count result)))
          (is (= false (:replay result)))
          (is (< elapsed-ms 4000.0) (str mode " took " elapsed-ms "ms"))
          (is (= :unavailable (:status stopped)))
          (is (true? (get-in stopped [:cleanup :cleanup_ok])) (pr-str stopped))
          (is (empty? (get-in stopped [:cleanup :remaining])))
          (is (= 2 (count (filter #(= "clj-surgeon.embedded-elaborator-ledger.v1"
                                      (:schema %)) rows)))))
        (finally
          (supervisor/stop! spark-supervisor)
          (delete-tree! temporary))))))

;; @spec MCP-OP-ELAB-010
;; @spec MCP-OP-ELAB-011
(deftest ledger-restart-recovery-makes-the-80-90-alarm-and-circuit-durable
  (let [temporary (str (java.nio.file.Files/createTempDirectory
                         "embedded-elaborator-quota-"
                         (make-array java.nio.file.attribute.FileAttribute 0)))
        config (fake-config temporary "quota" 2)
        first-supervisor (supervisor/start-background! config)]
    (try
      (is (= :available (:status (wait-for-state first-supervisor
                                                 :available 5000))))
      (is (:cleanup_ok (supervisor/stop! first-supervisor)))
      (let [second-supervisor (supervisor/start-background! config)]
        (try
          (let [failed (wait-for-state second-supervisor :unavailable 5000)
                rows (mapv #(json/parse-string % true)
                           (remove empty? (str/split-lines
                                            (slurp (:ledger-file config)))))]
            (is (= :boot-admission-failed (:reason failed)) (pr-str failed))
            (is (true? (get-in failed [:cleanup :cleanup_ok])))
            (is (= 2 (count (filter #(= "clj-surgeon.embedded-elaborator-ledger.v1"
                                        (:schema %)) rows))))
            (is (= 1 (count (filter #(= "quota-alarm" (:event %)) rows))))
            (is (= 100.0 (:rolling_24h_used_percent
                           (first (filter #(= "quota-alarm" (:event %)) rows))))))
          (finally
            (supervisor/stop! second-supervisor))))
      (finally
        (supervisor/stop! first-supervisor)
        (delete-tree! temporary)))))
