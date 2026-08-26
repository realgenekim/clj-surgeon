(ns clj-surgeon.mcp-operation-async-test
  (:require
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn- invoke-verification-view
  [status-fn]
  (let [calls (atom [])]
    (try
      (inspect-tool/init! {:project-root "."})
      (with-redefs [cold-verify/status status-fn]
        (inspect-tool/handle-inspect
          nil
          {"verification_job" "verify/fixture"
           "view" "verification"}
          (fn [content error? structured]
            (swap! calls conj {:content (first content)
                               :error? error?
                               :structured structured}))))
      (first @calls)
      (finally
        (inspect-tool/init! nil)))))

;; @spec MCP-OP-ASYNC-001
;; @spec MCP-OP-ASYNC-005
(deftest pending-verification-publishes-one-observed-state-without-job-time
  (let [state (atom {:ok true
                     :status :running
                     :verification_complete false
                     :job_elapsed_ms 17.0
                     :next_call {:tool "inspect_clojure"}})
        reads (atom 0)
        call (invoke-verification-view
               (fn [_ _]
                 (swap! reads inc)
                 (let [observed @state]
                   (reset! state {:ok true
                                  :status :passed
                                  :verification_complete true
                                  :job_elapsed_ms 25.0})
                   observed)))]
    (is (= 1 @reads))
    (is (false? (:error? call)))
    (is (= :running (get-in call [:structured :status])))
    (is (false? (get-in call [:structured :verification_complete])))
    (is (number? (get-in call [:structured :elapsed_ms])))
    (is (not (contains? (:structured call) :job_elapsed_ms)))))

;; @spec MCP-OP-ASYNC-002
;; @spec MCP-OP-ASYNC-003
(deftest completed-verification-labels-request-and-job-clocks
  (doseq [observed [{:ok true
                     :status :passed
                     :verification_complete true
                     :job_elapsed_ms 12.25
                     :next_action "none"}
                    {:ok false
                     :status :failed
                     :verification_complete true
                     :job_elapsed_ms 12.25
                     :next_action "review_verification_failure"}]]
    (testing (name (:status observed))
      (let [{:keys [content error? structured]}
            (invoke-verification-view (fn [_ _] observed))]
        (is (= (not (:ok observed)) error?))
        (is (number? (:elapsed_ms structured)))
        (is (= 12.25 (:job_elapsed_ms structured)))
        (is (str/includes?
              content (format "request %.2f ms" (:elapsed_ms structured))))
        (is (str/includes? content "job 12.25 ms"))))))

;; @spec MCP-OP-ASYNC-004
(deftest refusal-before-owned-job-execution-omits-job-time
  (doseq [refusal
          [{:ok false
            :error-type :unknown-or-expired-verification-job
            :verification_complete false}
           {:ok false
            :error-type :verification-job-workspace-mismatch
            :verification_complete false}]]
    (testing (name (:error-type refusal))
      (let [{:keys [error? structured]}
            (invoke-verification-view (fn [_ _] refusal))]
        (is (true? error?))
        (is (number? (:elapsed_ms structured)))
        (is (not (contains? structured :job_elapsed_ms)))))))
