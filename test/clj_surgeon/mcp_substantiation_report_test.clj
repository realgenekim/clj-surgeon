(ns clj-surgeon.mcp-substantiation-report-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-operation :as operation]
   [clj-surgeon.mcp-substantiation :as substantiation]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [substantiation-report :as report]
   [substantiation-report-io :as report-io])
  (:import
   (java.nio.file Files)))

(defn- temp-directory []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-substantiation-report-test-"
             (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- recorded-events []
  (let [state (substantiation/start! {:directory (temp-directory)
                                      :session-id "ledger"
                                      :clock (constantly "2026-08-30T00:00:00Z")})
        context (substantiation/begin-call!
                  state
                  {:session-id "session" :client-name "test" :client-version "1"}
                  "inspect_clojure"
                  {:requests [{:file "src/demo.clj" :forms ["alpha"]}]})]
    (substantiation/complete-call!
      state context
      {:ok true
       :operation "inspect_clojure"
       :read_complete true
       :results []
       :elapsed_ms 1.0})
    {:state state
     :events (report/parse-lines (slurp (:file state)))}))

(defn- rechain [events]
  (loop [remaining events
         sequence 1
         previous nil
         result []]
    (if-let [event (first remaining)]
      (let [call-id (:call_id event)
            open (dissoc event :schema :sequence :previous_event_sha256
                         :event_sha256 :call_id)
            closed (-> (substantiation/close-event
                         {:sequence sequence
                          :previous-event-sha256 previous
                          :call-id call-id
                          :event open})
                       substantiation/canonical-json
                       (json/parse-string true))]
        (recur (next remaining) (inc sequence)
               (:event_sha256 closed) (conj result closed)))
      result)))

(deftest validates-one-complete-hash-chain
  (let [{:keys [events]} (recorded-events)]
    (is (= events (report/validate-chain events)))
    (is (= [1 2] (mapv :sequence events)))
    (is (nil? (:previous_event_sha256 (first events))))
    (is (= (:event_sha256 (first events))
           (:previous_event_sha256 (second events))))))

(deftest refuses-tamper-gaps-and-unmatched-starts
  (let [{:keys [events]} (recorded-events)]
    (testing "digest tamper"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"digest mismatch"
                            (report/validate-chain
                              (assoc-in events [1 :tool] "tampered")))))
    (testing "sequence gap"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"sequence gap"
                            (report/validate-chain
                              [(assoc (first events) :sequence 2)]))))
    (testing "unmatched start"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"unmatched starts"
                            (report/validate-chain [(first events)]))))))

(deftest refuses-rehashed-nested-schema-and-unregistered-stage
  (let [{:keys [events]} (recorded-events)
        unknown-transport
        (rechain (assoc-in events [0 :transport :untrusted_field]
                           "untrusted-value"))
        registry (edn/read-string
                   (slurp "bench/fixtures/substantiation_telemetry/feature_registry.edn"))
        baseline (edn/read-string
                   (slurp "bench/fixtures/substantiation_telemetry/baseline.edn"))
        bad-stage
        (rechain (assoc-in events [0 :features]
                           [{:feature_id "prepared-request"
                             :stage "unregistered"
                             :counts {}
                             :dimensions {}}]))]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"transport is not closed"
                          (report/validate-chain unknown-transport)))
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"stage is not registered"
          (report/compile-report
            {:events bad-stage
             :registry registry
             :baseline baseline
             :marker {:start-sequence 1 :end-sequence 2 :sha256 "marker"}
             :ledger-bytes 100})))))

(deftest refuses-rehashed-nested-value-forgeries
  (let [{:keys [events]} (recorded-events)
        cases
        [[:unknown-operation
          #(assoc-in % [0 :operation] "src/private.clj")]
         [:unknown-request-field-presence
          #(update-in % [0 :request_shape :field_presence]
                      conj "src/private.clj")]
         [:request-count-source-string
          #(assoc-in % [0 :request_shape :request_count] "src/private.clj")]
         [:unknown-result-field-presence
          #(update-in % [1 :result_shape :field_presence]
                      conj "src/private.clj")]
         [:unknown-result-semantic-kind
          #(assoc-in % [1 :result_shape :semantic_kinds]
                     ["src/private.clj"])]
         [:unknown-result-error-type
          #(assoc-in % [1 :result_shape :error_type] "src/private.clj")]
         [:result-boolean-source-string
          #(assoc-in % [1 :result_shape :ok] "src/private.clj")]
         [:raw-client-name
          #(assoc-in % [0 :transport :client_name] "src/private.clj")]
         [:raw-client-version
          #(assoc-in % [0 :transport :client_version] "private prose")]]]
    (doseq [[case-id mutate] cases]
      (testing (name case-id)
        (is (thrown? clojure.lang.ExceptionInfo
                     (report/validate-chain (rechain (mutate events)))))))))

(deftest accepts-future-features-and-refuses-reused-or-drifting-identities
  (let [{:keys [events]} (recorded-events)
        future-feature
        (rechain
          (assoc-in events [0 :features]
                    [{:feature_id "elaborator.wall-fill"
                      :stage "future-stage"
                      :counts {:requests 1}
                      :dimensions {:eligible true}}]))
        duplicate-event-id
        (rechain (assoc-in events [1 :event_id] (:event_id (first events))))
        drifts
        [[:session-token
          #(assoc-in % [1 :transport :session_token] (apply str (repeat 64 "a")))]
         [:key-id
          #(assoc-in % [1 :transport :key_id] (apply str (repeat 64 "b")))]
         [:tool
          #(assoc-in % [1 :tool] "edit_clojure")]]]
    (is (= future-feature (report/validate-chain future-feature)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (report/validate-chain duplicate-event-id)))
    (doseq [[case-id mutate] drifts]
      (testing (name case-id)
        (is (thrown? clojure.lang.ExceptionInfo
                     (report/validate-chain (rechain (mutate events)))))))))

(deftest report-retains-zero-features-and-refuses-promotion-authority
  (let [{:keys [events]} (recorded-events)
        registry (edn/read-string
                   (slurp "bench/fixtures/substantiation_telemetry/feature_registry.edn"))
        baseline (edn/read-string
                   (slurp "bench/fixtures/substantiation_telemetry/baseline.edn"))
        compiled (report/compile-report
                   {:events events
                    :registry registry
                    :baseline baseline
                    :marker {:start-sequence 1 :end-sequence 2 :sha256 "marker"}
                    :ledger-bytes 100})]
    (is (= 0 (get-in compiled [:features ["prepared-request" "emitted"]])))
    (is (= :measured (get-in compiled [:claims :counts])))
    (is (= :observed-before-after
           (get-in compiled [:claims :historical_comparison])))
    (is (= :projected (get-in compiled [:claims :decode_seconds])))
    (is (false? (:promotion_authority compiled)))
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"no promotion authority"
          (substantiation/substantiation-report
            {:marker {:sha256 "marker"}
             :events []
             :projection-rate-ms-per-byte 3.5237
             :promotion-request true})))))

(deftest report-output-is-new-confined-and-write-once
  (let [{:keys [state]} (recorded-events)
        parent (temp-directory)
        output (io/file parent "weekly")
        arguments {:ledger (:file state)
                   :registry "bench/fixtures/substantiation_telemetry/feature_registry.edn"
                   :baseline "bench/fixtures/substantiation_telemetry/baseline.edn"
                   :output-root output
                   :installed-commit (apply str (repeat 40 "a"))
                   :installed-tag "stable-test"
                   :compiler-commit (apply str (repeat 40 "b"))
                   :compiler-tree (apply str (repeat 40 "c"))}
        result (report-io/compile-report! arguments)]
    (is (:ok result))
    (is (= #{"episodes.json" "episodes.sha256" "report.json"
             "report.md" "receipt.edn"}
           (set (map #(.getName %) (.listFiles output)))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"already exists"
                          (report-io/compile-report! arguments)))))

(deftest segment-refuses-existing-path-and-preserves-public-result
  (let [directory (temp-directory)
        state (substantiation/start! {:directory directory :session-id "one"})
        result {:ok true :committed true}
        context (substantiation/begin-call!
                  state nil "edit_clojure"
                  {:workspace_root "/private/repo"
                   :edits [{:file "src/secret.clj"
                            :within {:form "secret-owner"}
                            :from "secret-old"
                            :to "secret-new"}]})]
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (substantiation/start! {:directory directory :session-id "one"})))
    (is (identical? result (substantiation/complete-call! state context result)))
    (let [text (slurp (:file state))]
      (is (not (.contains text "/private/repo")))
      (is (not (.contains text "src/secret.clj")))
      (is (not (.contains text "secret-owner")))
      (is (not (.contains text "secret-old")))
      (is (not (.contains text "secret-new"))))))

(deftest operation-hooks-block-before-execution-and-preserve-finish-result
  (let [executions (atom 0)
        callbacks (atom [])
        blocked-body
        (operation/invoke!
          {:clock-nanos (let [clock (atom 0)] #(swap! clock + 1000000))
           :before-execute #(identity {:blocked-result {:ok false
                                                        :error_type "ledger-unhealthy"}})
           :execute #(do (swap! executions inc) {:ok true})
           :summarize (constantly "summary")
           :callback #(swap! callbacks conj [%1 %2 %3])})
        result {:ok true :value 42}
        returned (atom nil)
        healthy-body
        (operation/invoke!
          {:clock-nanos (let [clock (atom 0)] #(swap! clock + 1000000))
           :before-execute #(identity {:call-id "call"})
           :after-result (fn [_ finalized]
                           (reset! returned finalized)
                           finalized)
           :execute (constantly result)
           :summarize (constantly "summary")
           :callback (fn [_ _ _])})]
    (is (zero? @executions))
    (is (= "ledger-unhealthy" (get-in @callbacks [0 2 :error_type])))
    (is (= false (get-in @callbacks [0 2 :ok])))
    (is (= 42 (:value @returned)))
    (is (= 42 (:value (json/parse-string healthy-body true))))
    (is (= false (:ok (json/parse-string blocked-body true))))))

(deftest feature-lifecycle-is-derived-from-public-shapes
  (let [state (substantiation/start! {:directory (temp-directory)
                                      :session-id "features"
                                      :clock (constantly "2026-08-30T00:00:00Z")})
        inspect-params {:requests [{:file "src/demo.clj" :forms ["alpha"]}]}
        inspect-context (substantiation/begin-call!
                          state nil "inspect_clojure" inspect-params)
        descriptor {:arguments {:workspace_root "/repo"
                                :edits [{:file "src/demo.clj"
                                         :within {:form "alpha"}
                                         :from "old"
                                         :to nil
                                         :matches 1}]}
                    :caller_holes ["arguments.edits[0].to"]}
        _ (substantiation/complete-call!
            state inspect-context
            {:ok true
             :operation "inspect_clojure"
             :read_complete true
             :results []
             :prepared_request descriptor})
        edit-context (substantiation/begin-call!
                       state nil "edit_clojure"
                       {:workspace_root "/repo"
                        :edits [{:file "src/demo.clj"
                                 :within {:form "alpha"}
                                 :from "old"
                                 :to "new"
                                 :matches 1}]})
        _ (substantiation/complete-call!
            state edit-context
            {:ok true :operation "edit_clojure" :committed true})
        events (report/parse-lines (slurp (:file state)))
        features (mapcat :features events)]
    (is (some #(and (= "read-normalization" (:feature_id %))
                    (= "operation-omitted" (:stage %)))
              features))
    (is (some #(and (= "read-normalization" (:feature_id %))
                    (= "ids-omitted" (:stage %)))
              features))
    (is (some #(and (= "prepared-request" (:feature_id %))
                    (= "emitted" (:stage %)))
              features))
    (is (some #(and (= "prepared-request" (:feature_id %))
                    (= "consumed" (:stage %)))
              features))
    (is (some #(and (= "prepared-request" (:feature_id %))
                    (= "committed" (:stage %)))
              features))
    (is (= events (report/validate-chain events)))))

(deftest caller-controlled-vocabulary-is-closed-before-append
  (let [state (substantiation/start! {:directory (temp-directory)
                                      :session-id "closed-vocabulary"
                                      :clock (constantly "2026-08-30T00:00:00Z")})
        context (substantiation/begin-call!
                  state nil "inspect_clojure"
                  {:operation "secret-operation"
                   :secret-field-name "secret-value"
                   :secret-file-name "secret-file-value"
                   :requests []})
        _ (substantiation/complete-call!
            state context
            {:ok false
             :operation "secret-result-operation"
             :error_type "secret-refusal-type"
             :secret-result-field "secret-result-value"})
        events (report/parse-lines (slurp (:file state)))
        start (first events)
        finish (second events)
        ledger (slurp (:file state))]
    (is (= "other" (:operation start)))
    (is (= "other" (:operation finish)))
    (is (= "other" (get-in finish [:result_shape :error_type])))
    (is (= ["operation" "requests"]
           (get-in start [:request_shape :field_presence])))
    (is (= ["file"]
           (mapv :kind (get-in start [:request_shape :subject_tokens]))))
    (is (not (.contains ledger "secret-field-name")))
    (is (not (.contains ledger "secret-value")))
    (is (not (.contains ledger "secret-file-name")))
    (is (not (.contains ledger "secret-file-value")))
    (is (not (.contains ledger "secret-refusal-type")))
    (is (not (.contains ledger "secret-result-value")))))

(deftest strict-overhead-verdict-keeps-every-threshold-edge
  (let [passing {:event-max-bytes 32768
                 :projection-samples 10000
                 :projection-p95-ms 0.499
                 :append-samples 1000
                 :append-p50-ms 0.999
                 :append-p95-ms 4.999
                 :append-max-ms 24.999
                 :live-samples-per-arm 100
                 :live-p50-delta-ms 2.0
                 :live-p95-delta-ms 5.0
                 :semantic-parity true
                 :model-calls 0
                 :network-calls 0}]
    (is (:ok (substantiation/measurement-verdict passing)))
    (is (false? (:ok (substantiation/measurement-verdict
                       (assoc passing :projection-p95-ms 0.5)))))
    (is (false? (:ok (substantiation/measurement-verdict
                       (assoc passing :append-p50-ms 1.0)))))
    (is (false? (:ok (substantiation/measurement-verdict
                       (assoc passing :append-p95-ms 5.0)))))
    (is (false? (:ok (substantiation/measurement-verdict
                       (assoc passing :append-max-ms 25.0)))))
    (is (false? (:ok (substantiation/measurement-verdict
                       (dissoc passing :semantic-parity)))))))
