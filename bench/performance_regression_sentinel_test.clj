(ns performance-regression-sentinel-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [performance-regression-sentinel :as sentinel]))

(def sha-a (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))
(def sha-c (apply str (repeat 64 "c")))
(def sha-d (apply str (repeat 64 "d")))
(def sha-e (apply str (repeat 64 "e")))
(def commit-a (apply str (repeat 40 "a")))
(def commit-b (apply str (repeat 40 "b")))
(def commit-c (apply str (repeat 40 "c")))

(def invocation-id
  #uuid "10000000-0000-0000-0000-000000000001")
(def red-event-id
  #uuid "20000000-0000-0000-0000-000000000002")
(def turn-id
  #uuid "30000000-0000-0000-0000-000000000003")
(def call-id
  #uuid "40000000-0000-0000-0000-000000000004")
(def other-call-id
  #uuid "40000000-0000-0000-0000-000000000099")
(def run-ids
  {:C1 #uuid "50000000-0000-0000-0000-000000000001"
   :S1 #uuid "50000000-0000-0000-0000-000000000002"
   :S2 #uuid "50000000-0000-0000-0000-000000000003"
   :C2 #uuid "50000000-0000-0000-0000-000000000004"})

(def scope
  {:route-id :sessionize-format-extraction/apply-v1
   :task-sha256 sha-a
   :policy-version 1})

(def stable-identity
  {:kind :release-tag
   :tag "stable/perf-sentinel"
   :tag-object commit-a
   :commit commit-b
   :tree commit-c
   :baseline-receipt-sha256 sha-b})

(def candidate-identity
  {:commit commit-c
   :tree commit-a})

(def controller-identity
  {:commit commit-a
   :tree commit-b
   :artifact-manifest-sha256 sha-c})

(def valid-request
  {:schema :clj-surgeon.performance-regression-sentinel-request/v1
   :invocation-id invocation-id
   :mode :prepublish
   :candidate candidate-identity
   :stable stable-identity
   :controller controller-identity
   :recovery nil})

(def recovery-arm
  {:red-event-id red-event-id
   :scope scope
   :stable (select-keys stable-identity [:commit :tree])
   :candidate {:kind :same
               :commit (:commit candidate-identity)
               :tree (:tree candidate-identity)
               :supersedes nil}
   :owner {:id :release-owner
           :authorization-receipt-sha256 sha-d}})

(def valid-recovery-request
  (assoc valid-request :mode :recovery :recovery recovery-arm))

(def valid-backfill-request
  (-> valid-request
      (assoc :mode :backfill)
      (assoc :stable {:kind :historical-commit
                      :commit commit-b
                      :tree commit-c})))

(defn record
  ([kind]
   (record kind {}))
  ([kind fields]
   (merge {:kind kind :turn-id turn-id :call-id call-id} fields)))

(def valid-records
  [(record :turn-start {:at-ns 100})
   (record :apply-start
           {:at-ns 200
            :action :mcp
            :tool "apply_clojure_changes"
            :arguments-sha256 sha-a})
   (record :apply-capture {:arguments-sha256 sha-a})
   (record :apply-result
           {:result-sha256 sha-b
            :read-back-sha256 sha-c})
   (record :apply-receipt
           {:receipt-sha256 sha-d
            :verifier-receipt-sha256 sha-e})
   (record :apply-complete
           {:at-ns 900
            :verification-complete true
            :result-sha256 sha-b
            :read-back-sha256 sha-c
            :receipt-sha256 sha-d
            :verifier-receipt-sha256 sha-e})
   (record :turn-complete {:at-ns 1100})])

(def semantic-receipt
  {:schema :clj-surgeon.performance-regression-semantic-score/v1
   :scorer-sha256 sha-a
   :scorer-inputs-sha256 sha-b
   :expected-source-manifest-sha256 sha-c
   :final-source-manifest-sha256 sha-c
   :source-set-exact true
   :passed true})

(def valid-attempt-input
  {:run-id #uuid "50000000-0000-0000-0000-000000000005"
   :position :C1
   :arm :candidate
   :schedule-index 1
   :records valid-records
   :semantic-receipt semantic-receipt
   :pressure {:status :admitted
              :complete true
              :sampled-at-ns 50
              :policy-sha256 sha-a
              :receipt-sha256 sha-b}
   :resources {:workspace "/private/tmp/sentinel/workspace-c1"
               :home "/private/tmp/sentinel/home-c1"
               :port 41001
               :result-root "/private/tmp/sentinel/result-c1"}
   :identity {:expected-sha256 sha-c :actual-sha256 sha-c}
   :inventory {:pre-sha256 sha-d
               :post-sha256 sha-d
               :exact true}
   :runtime {:formatter {:binary-sha256 sha-a
                         :argv-sha256 sha-b}
             :verifier {:binary-sha256 sha-c
                        :argv-sha256 sha-d
                        :profile-sha256 sha-e}}})

(defn compiled-attempt [position wall]
  {:status :valid
   :run-id (get run-ids position)
   :position position
   :arm (if (#{:C1 :C2} position) :candidate :stable)
   :t-verified-ns wall
   :route-adherent true
   :correct true
   :verification-complete true})

(defn verdict-input
  ([request attempts]
   (verdict-input request attempts {:state :clear}))
  ([request attempts ledger]
   {:request request
    :scope scope
    :attempts attempts
    :ledger ledger}))

(defn invalid-result? [result reason]
  (and (= false (:ok result))
       (= :invalid (:status result))
       (= reason (:reason result))))

;; @spec PERF-SENT-CONFIG-001 PERF-SENT-PROMOTION-002
(deftest closed-request-decoder-refuses-before-authority
  (testing "the exact request is accepted without caller policy injection"
    (doseq [request [valid-request
                     (assoc valid-request :mode :nightly)
                     valid-backfill-request]]
      (is (= {:ok true :request request}
             (sentinel/decode-request request)))))
  (testing "missing, unknown, mutable, and caller-owned fields fail closed"
    (doseq [request [(dissoc valid-request :candidate)
                     (assoc valid-request :unknown true)
                     (assoc-in valid-request [:candidate :commit] "HEAD")
                     (assoc valid-request :mode :benchmark)
                     (assoc valid-request :result-root "/tmp/caller-root")
                     (assoc valid-request :model :gpt-anything)]]
      (is (= :invalid-configuration
             (:error-type (sentinel/decode-request request))))))
  (testing "promotion-shaped requests receive the dedicated refusal"
    (doseq [request [(assoc valid-request :promotion true)
                     (assoc valid-request :satisfy-promotion-gate true)]]
      (is (= :sentinel-promotion-authority-refused
             (:error-type (sentinel/decode-request request)))))))

;; @spec PERF-SENT-CONFIG-001 PERF-SENT-RECOVERY-001 PERF-SENT-RECOVERY-003
(deftest request-mode-and-recovery-arm-are-a-closed-pair
  (is (= true (:ok (sentinel/decode-request valid-recovery-request))))
  (is (= :invalid-configuration
         (:error-type
           (sentinel/decode-request
             (assoc valid-request :mode :recovery :recovery nil)))))
  (is (= :invalid-configuration
         (:error-type
           (sentinel/decode-request
             (assoc valid-request :recovery recovery-arm)))))
  (is (= :identity-drift
         (:error-type
           (sentinel/decode-request
             (assoc-in valid-recovery-request
                       [:recovery :stable :commit]
                       commit-a)))))
  (is (= :identity-drift
         (:error-type
           (sentinel/decode-request
             (assoc-in valid-recovery-request
                       [:recovery :candidate :supersedes]
                       candidate-identity))))))

;; @spec PERF-SENT-ATTEMPT-001 PERF-SENT-ATTEMPT-002
;; @spec PERF-SENT-ATTEMPT-003 PERF-SENT-TIME-001
(deftest compile-attempt-joins-one-complete-first-action-lifecycle
  (let [result (sentinel/compile-attempt valid-attempt-input)]
    (is (:ok result))
    (is (= :valid (get-in result [:attempt :status])))
    (is (= :C1 (get-in result [:attempt :position])))
    (is (= 1000 (get-in result [:attempt :t-verified-ns])))
    (is (= call-id (get-in result [:attempt :apply-call-id])))
    (is (true? (get-in result [:attempt :verification-complete])))))

;; @spec PERF-SENT-ATTEMPT-001 PERF-SENT-ATTEMPT-003
(deftest compile-attempt-refuses-preambles-duplicates-or-divergent-evidence
  (let [preamble (record :assistant-message {:at-ns 150 :text "Working..."})
        duplicate (nth valid-records 1)
        mismatched (assoc-in valid-attempt-input
                             [:records 2 :call-id]
                             other-call-id)
        tampered (assoc-in valid-attempt-input
                           [:records 5 :receipt-sha256]
                           sha-a)]
    (doseq [attempt [(update valid-attempt-input :records
                             #(vec (concat [(first %)] [preamble] (rest %))))
                     (update valid-attempt-input :records
                             #(vec (concat % [duplicate])))
                     mismatched
                     tampered]]
      (is (invalid-result? (sentinel/compile-attempt attempt)
                           :invalid-evidence)))))

;; @spec PERF-SENT-ADMIT-001 PERF-SENT-IDENT-001 PERF-SENT-CLIENT-001
(deftest compile-attempt-types-environment-and-identity-failures
  (is (invalid-result?
        (sentinel/compile-attempt
          (assoc-in valid-attempt-input [:pressure :status] :deferred))
        :invalid-environment))
  (is (invalid-result?
        (sentinel/compile-attempt
          (assoc-in valid-attempt-input [:identity :actual-sha256] sha-d))
        :identity-drift))
  (is (invalid-result?
        (sentinel/compile-attempt
          (assoc-in valid-attempt-input [:inventory :exact] false))
        :invalid-evidence)))

;; @spec PERF-SENT-INVALID-001 PERF-SENT-ATTEMPT-003
(deftest correctness-failure-keeps-candidate-and-stable-blame-distinct
  (let [failed (assoc-in valid-attempt-input
                         [:semantic-receipt :passed]
                         false)]
    (is (invalid-result?
          (sentinel/compile-attempt failed)
          :candidate-correctness-failure))
    (is (invalid-result?
          (sentinel/compile-attempt
            (assoc-in valid-attempt-input
                      [:semantic-receipt :source-set-exact]
                      false))
          :candidate-correctness-failure))
    (is (invalid-result?
          (sentinel/compile-attempt (assoc failed :arm :stable :position :S1))
          :stable-baseline-failure))))

;; @spec PERF-SENT-TIME-001
(deftest t-verified-is-observed-turn-wall-and-cannot-be-substituted
  (doseq [records [(assoc-in valid-records [6 :at-ns] 100)
                   (assoc-in valid-records [5 :at-ns] 1100)
                   (assoc-in valid-records [0 :at-ns] 1100)
                   (assoc-in valid-records [6 :at-ns]
                             Double/POSITIVE_INFINITY)
                   (update valid-records 6 dissoc :at-ns)]]
    (is (invalid-result?
          (sentinel/compile-attempt
            (assoc valid-attempt-input
                   :records records
                   :process-wall-ns 1000
                   :server-elapsed-ns 1000))
          :invalid-evidence))))

;; @spec PERF-SENT-SCHEDULE-001 PERF-SENT-THRESHOLD-001
(deftest adaptive-first-pair-boundary-is-exact
  (is (= {:decision :green-stop
          :required-positions [:C1 :S1]}
         (sentinel/compile-screen-decision {:C1 1079 :S1 1000})))
  (is (= {:decision :reverse-required
          :required-positions [:C1 :S1 :S2 :C2]}
         (sentinel/compile-screen-decision {:C1 1080 :S1 1000})))
  (is (= :invalid-evidence
         (:error-type
           (sentinel/compile-screen-decision
             {:C1 Double/NaN :S1 1000})))))

;; @spec PERF-SENT-SCHEDULE-002 PERF-SENT-SCHEDULE-003
;; @spec PERF-SENT-SCHEDULE-004
(deftest verdict-refuses-missing-extra-or-reordered-attempts
  (let [green [(compiled-attempt :C1 1079)
               (compiled-attempt :S1 1000)]
        triggered [(compiled-attempt :C1 1080)
                   (compiled-attempt :S1 1000)]]
    (doseq [attempts [(conj green (compiled-attempt :S2 1000))
                      triggered
                      (conj triggered
                            (compiled-attempt :C2 1080)
                            (compiled-attempt :S2 1000))
                      (conj triggered
                            (compiled-attempt :S2 1000)
                            (compiled-attempt :S2 1000))]]
      (let [result (sentinel/compile-verdict
                     (verdict-input valid-request attempts))]
        (is (= :invalid (:status result)))
        (is (= :schedule-violation (:reason result)))
        (is (nil? (:slowdown result)))
        (is (pos? (:required-process-exit result)))))))

;; @spec PERF-SENT-VERDICT-001 PERF-SENT-VERDICT-002
;; @spec PERF-SENT-VERDICT-003 PERF-SENT-PUBLISH-001
(deftest ordinary-verdicts-form-a-closed-green-yellow-red-union
  (let [green (sentinel/compile-verdict
                (verdict-input
                  valid-request
                  [(compiled-attempt :C1 1079)
                   (compiled-attempt :S1 1000)]))
        yellow (sentinel/compile-verdict
                 (verdict-input
                   valid-request
                   [(compiled-attempt :C1 1080)
                    (compiled-attempt :S1 1000)
                    (compiled-attempt :S2 1000)
                    (compiled-attempt :C2 1079)]))
        red (sentinel/compile-verdict
              (verdict-input
                valid-request
                [(compiled-attempt :C1 1110)
                 (compiled-attempt :S1 1000)
                 (compiled-attempt :S2 1000)
                 (compiled-attempt :C2 1090)]))]
    (is (= [:green :below-trigger :allowed 0 false]
           ((juxt :status :reason :publication-state
                  :required-process-exit :promotion-authority)
            green)))
    (is (= [:yellow :triggered-not-confirmed :allowed 0 false]
           ((juxt :status :reason :publication-state
                  :required-process-exit :promotion-authority)
            yellow)))
    (is (= :red (:status red)))
    (is (= :confirmed-regression (:reason red)))
    (is (= :blocked (:publication-state red)))
    (is (pos? (:required-process-exit red)))))

;; @spec PERF-SENT-THRESHOLD-001 PERF-SENT-VERDICT-002
(deftest red-requires-both-position-losses-and-ten-percent-pooled
  (doseq [attempts [[(compiled-attempt :C1 1100)
                     (compiled-attempt :S1 1000)
                     (compiled-attempt :S2 1000)
                     (compiled-attempt :C2 1100)]
                    [(compiled-attempt :C1 1200)
                     (compiled-attempt :S1 1000)
                     (compiled-attempt :S2 1000)
                     (compiled-attempt :C2 1000)]]]
    (let [result (sentinel/compile-verdict
                   (verdict-input valid-request attempts))]
      (if (= 1100 (:t-verified-ns (last attempts)))
        (is (= :red (:status result)))
        (is (= :yellow (:status result)))))))

;; @spec PERF-SENT-INVALID-001 PERF-SENT-VERDICT-001
(deftest invalid-verdict-reasons-are-closed-and-never-carry-slowdown
  (let [reasons [:candidate-correctness-failure
                 :stable-baseline-failure
                 :invalid-environment
                 :invalid-evidence
                 :identity-drift
                 :schedule-violation
                 :not-comparable
                 :incomplete-archive-input]]
    (doseq [reason reasons]
      (let [result (sentinel/compile-verdict
                     (assoc (verdict-input valid-request [])
                            :invalid-reason reason))]
        (is (= :invalid (:status result)))
        (is (= reason (:reason result)))
        (is (nil? (:metrics result)))
        (is (nil? (:slowdown result)))
        (is (pos? (:required-process-exit result)))))
    (is (= :closed-verdict-refused
           (:error-type
             (sentinel/compile-verdict
               (assoc (verdict-input valid-request [])
                      :invalid-reason :product-is-slow)))))))

;; @spec PERF-SENT-PUBLISH-001 PERF-SENT-PUBLISH-002
;; @spec PERF-SENT-LEDGER-003
(deftest publication-is-derived-from-resulting-ledger-state
  (let [attempts [(compiled-attempt :C1 1079)
                  (compiled-attempt :S1 1000)]
        clear (sentinel/compile-verdict
                (verdict-input valid-request attempts {:state :clear}))
        blocked (sentinel/compile-verdict
                  (verdict-input valid-request attempts
                                 {:state :blocked
                                  :red-event-id red-event-id}))
        nightly (sentinel/compile-verdict
                  (verdict-input (assoc valid-request :mode :nightly)
                                 attempts
                                 {:state :clear}))]
    (is (= [:clear :allowed 0]
           [(:resulting-ledger-state clear)
            (:publication-state clear)
            (:required-process-exit clear)]))
    (is (= [:blocked :blocked red-event-id]
           [(:resulting-ledger-state blocked)
            (:publication-state blocked)
            (:blocking-red-event-id blocked)]))
    (is (pos? (:required-process-exit blocked)))
    (is (= :not-applicable (:publication-state nightly)))
    (is (zero? (:required-process-exit nightly)))))

;; @spec PERF-SENT-LEDGER-003 PERF-SENT-VERDICT-002
(deftest another-red-never-replaces-the-original-block
  (let [result (sentinel/compile-verdict
                 (verdict-input
                   valid-request
                   [(compiled-attempt :C1 1110)
                    (compiled-attempt :S1 1000)
                    (compiled-attempt :S2 1000)
                    (compiled-attempt :C2 1090)]
                   {:state :blocked :red-event-id red-event-id}))]
    (is (= :red (:status result)))
    (is (= :blocked (:resulting-ledger-state result)))
    (is (= red-event-id (:blocking-red-event-id result)))))

;; @spec PERF-SENT-RECOVERY-002 PERF-SENT-RECOVERY-004
;; @spec PERF-SENT-PUBLISH-002 PERF-SENT-LEDGER-003
(deftest recovery-is-forced-four-run-and-strictly-below-eight-percent
  (let [ledger {:state :blocked :red-event-id red-event-id}
        recovered (sentinel/compile-verdict
                    (verdict-input
                      valid-recovery-request
                      [(compiled-attempt :C1 1079)
                       (compiled-attempt :S1 1000)
                       (compiled-attempt :S2 1000)
                       (compiled-attempt :C2 1079)]
                      ledger))
        equality (sentinel/compile-verdict
                   (verdict-input
                     valid-recovery-request
                     [(compiled-attempt :C1 1080)
                      (compiled-attempt :S1 1000)
                      (compiled-attempt :S2 1000)
                      (compiled-attempt :C2 1080)]
                     ledger))
        incomplete (sentinel/compile-verdict
                     (verdict-input
                       valid-recovery-request
                       [(compiled-attempt :C1 1000)
                        (compiled-attempt :S1 1000)]
                       ledger))]
    (is (= [:recovered :recovery-proved :clear :allowed 0]
           ((juxt :status :reason :resulting-ledger-state
                  :publication-state :required-process-exit)
            recovered)))
    (is (= :yellow (:status equality)))
    (is (= :recovery-not-cleared (:reason equality)))
    (is (= :blocked (:resulting-ledger-state equality)))
    (is (pos? (:required-process-exit equality)))
    (is (= :invalid (:status incomplete)))
    (is (= :schedule-violation (:reason incomplete)))))

;; @spec PERF-SENT-RECOVERY-004 PERF-SENT-LEDGER-003
(deftest recovery-red-retains-the-original-block
  (let [result (sentinel/compile-verdict
                 (verdict-input
                   valid-recovery-request
                   [(compiled-attempt :C1 1100)
                    (compiled-attempt :S1 1000)
                    (compiled-attempt :S2 1000)
                    (compiled-attempt :C2 1100)]
                   {:state :blocked :red-event-id red-event-id}))]
    (is (= :red (:status result)))
    (is (= red-event-id (:blocking-red-event-id result)))
    (is (= :blocked (:resulting-ledger-state result)))
    (is (pos? (:required-process-exit result)))))

;; @spec PERF-SENT-PROMOTION-001 PERF-SENT-PROMOTION-002
(deftest promotion-verifier-rejects-sentinel-evidence-without-mutation
  (let [evidence {:schema :clj-surgeon.performance-regression-sentinel-verdict/v1
                  :invocation-id invocation-id
                  :promotion-authority false
                  :status :green}
        result (sentinel/verify-promotion-evidence evidence)]
    (is (= false (:ok result)))
    (is (= :sentinel-promotion-authority-refused (:error-type result)))
    (is (= evidence (:preserved-evidence result)))
    (is (false? (:promotion-authority result)))
    (is (false? (:retry-authority result)))
    (is (false? (:baseline-mutation-attempted result)))))

;; @spec PERF-SENT-BACKFILL-001 PERF-SENT-BASELINE-001
(deftest historical-backfill-is-an-explicit-two-pair-sequence
  (let [plan
        (sentinel/compile-backfill-plan
          {:fixture-tree "b0a3578f0cfc310afe17a4201a8a6095a057f070"
           :task-sha256
           "58dee06dfef9bf41b7ca26845d07380436354d9a"
           :capsule-sha256
           "a91227ae77a1618eae94f68cd7b7c039064fd619"
           :profile-sha256 sha-a
           :prompt-sha256 sha-b
           :overlay-sha256 sha-c
           :pairs
           [{:stable {:commit "b8e52cb603c35471cab6d4f562161a1a588c3b20"
                      :tree "fe42cf35db2c743bd64351fab65f03f63686034e"}
             :candidate {:commit "75585beeda63a4dcc9bb1e219d5721d89b93baa2"
                         :tree "b61fe3610643ddd23f1e7061879ac871b55e623a"}}
            {:stable {:commit "75585beeda63a4dcc9bb1e219d5721d89b93baa2"
                      :tree "b61fe3610643ddd23f1e7061879ac871b55e623a"}
             :candidate {:commit "19ab864889799b0028a5f7cb66c63b957ff7b973"
                         :tree "72e1ef5fc09587013e7b8d60f2ed027385280973"}}]})]
    (is (:ok plan))
    (is (= 2 (count (:pairs plan))))
    (is (= [:C1 :S1] (get-in plan [:pairs 0 :initial-schedule])))
    (is (= [:C1 :S1] (get-in plan [:pairs 1 :initial-schedule])))
    (is (every? false? (map :promotion-authority (:pairs plan))))
    (is (false? (:ancestry-inferred plan)))
    (is (false? (:evidence-reused plan)))))
