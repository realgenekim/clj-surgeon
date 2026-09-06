(ns clj-surgeon.mission-usage-test
  {:lane :fast}
  (:require
   [clj-surgeon.mission-display :as display]
   [clj-surgeon.mission-usage :as usage]
   [clojure.test :refer [deftest is]]))

(def known {:request_started true :prompt_tokens 1530 :completion_tokens 1930
            :reasoning_tokens 1089 :cost_usd 0.001983 :cost_source "provider-reported"})
(defn closed [completed cancelled] {:terminated? true :completed completed :cancelled cancelled})

(deftest actual-t1-legacy-receipt-counts-once
  ;; T1/receipts/mission-4039462254614884675/transport-close.edn scalar subset.
  (let [r (usage/summarize (closed [(assoc (dissoc known :request_started) :index 0 :request_wall_s 0.947)] []))]
    (is (= :complete (:status r)))
    (is (= 1 (:dispatched-attempts r)))
    (is (= 1530 (get-in r [:prompt-tokens :known-total])))
    (is (= 1930 (get-in r [:completion-tokens :known-total])))
    (is (= 1089 (get-in r [:reasoning-tokens :known-total])))
    (is (= 0.001983M (get-in r [:cost-usd :known-total])))))

(deftest fallback-attempts-supersede-wrapper-totals
  (let [r (usage/summarize (closed [(assoc known :index 0 :attempts [{:request_started true} known])] []))]
    (is (= 2 (:dispatched-attempts r)))
    (is (= 1930 (get-in r [:completion-tokens :known-total])))
    (is (= 1 (get-in r [:cost-usd :unknown-attempts])))
    (is (= 0.001983M (get-in r [:cost-usd :known-total])))
    (is (= :partial (:status r)))))

(deftest cancellation-and-completion-never-double-count
  (let [r (usage/summarize (closed [(assoc known :index 0 :attempts [known])] [0 1]))]
    (is (= 1 (:completed-candidates r)))
    (is (= 1 (:cancelled-candidates r)))
    (is (= 1 (:unknown-usage-candidates r)))
    (is (= 1 (:dispatched-attempts r)))
    (is (= :partial (:status r)))))

(deftest unknown-and-not-started-are-distinct
  (let [r (usage/summarize (closed [{:index 0 :attempts [{:request_started false}]}
                                    {:index 1 :error_type "candidate-request-interrupted"}] []))]
    (is (= 0 (:dispatched-attempts r)))
    (is (= 1 (:nonstarted-attempts r)))
    (is (= 1 (:unknown-usage-candidates r)))
    (is (nil? (get-in r [:cost-usd :known-total])))))

(deftest invalid-values-remain-unknown-and-zero-is-real
  (let [r (usage/summarize (closed [{:index 0 :attempts [(assoc known :cost_usd 0 :reasoning_tokens 0)
                                                         (assoc known :cost_usd Double/NaN :prompt_tokens -1
                                                                :completion_tokens false :reasoning_tokens "1")]}] []))]
    (is (= 0M (get-in r [:cost-usd :known-total])))
    (is (= 1 (get-in r [:cost-usd :unknown-attempts])))
    (is (= 1930 (get-in r [:completion-tokens :known-total])))
    (is (= 0 (get-in r [:reasoning-tokens :known-total])))))

(deftest bounds-and-duplicate-identities-refuse-the-summary
  (doseq [r [nil (closed (vec (repeat 6 known)) [])
             (closed [(assoc known :index 0) (assoc known :index 0)] [])
             (closed [{:index 0 :attempts [known known known]}] [])]]
    (is (= :unavailable (:status (usage/summarize r))))))

(deftest compact-show-preserves-saved-usage-without-artifact-parsing
  (let [summary (usage/summarize (closed [(assoc known :index 0 :attempts [known])] []))
        view {:ok true :id "M-1" :state :verified :receipt {:committed true :usage summary}}
        result (display/show-result view {})]
    (is (= summary (get-in result [:receipt :usage])))
    (is (not (.contains (pr-str result) "private-source")))))
