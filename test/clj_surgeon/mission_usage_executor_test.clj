(ns clj-surgeon.mission-usage-executor-test
  {:lane :battery}
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mission-events :as events]
   [clj-surgeon.mission-typist-executor :as executor]
   [clojure.test :refer [deftest is]]))

(deftest closed-usage-survives-success-rejection-and-post-close-commit-error
  (doseq [mode [:success :rejection :commit-error]]
    (let [order (atom [])
          candidate {:index 0 :request_started true :prompt_tokens 2 :completion_tokens 3
                     :reasoning_tokens 1 :cost_usd 0 :cost_source "provider-reported"}]
      (with-redefs [executor/unchanged? (constantly true)
                    executor/make-artifacts! (constantly "/var/tmp/forge/unused-mock-artifacts")
                    file-ops/atomic-write! (fn [& _] nil)
                    events/observe-phase! (fn [_ f] (f))
                    executor/request-candidates! (constantly [candidate])
                    executor/compile-candidate! (fn [& _] {:ok (not= mode :rejection)})
                    executor/verify-candidate! (fn [& _] {:ok true})
                    executor/close-candidates! (fn [& _] (swap! order conj :close)
                                                 {:terminated? true :completed [candidate] :cancelled []})
                    executor/commit-candidate! (fn [& _]
                                                 (swap! order conj :commit)
                                                 (if (= mode :commit-error)
                                                   (throw (ex-info "private error" {:error-type :test-commit-refusal}))
                                                   {:ok true :committed true}))]
        (let [result (executor/execute! {} {:plan {:typist {:route {}}}})]
          (is (= 3 (get-in result [:usage :completion-tokens :known-total])))
          (is (= 0M (get-in result [:usage :cost-usd :known-total])))
          (is (= :complete (get-in result [:usage :status])))
          (is (= (if (= mode :rejection) [:close] [:close :commit]) @order))
          (when (= mode :commit-error)
            (is (= :test-commit-refusal (:error-type result)))
            (is (not (.contains (pr-str result) "private error")))))))))

(deftest pre-transport-refusal-does-not-invent-zero-cost
  (let [result (executor/execute! {} {:plan {}})]
    (is (= :typist-stale-plan (:error-type result)))
    (is (= :unavailable (get-in result [:usage :status])))
    (is (nil? (get-in result [:usage :cost-usd :known-total])))))
