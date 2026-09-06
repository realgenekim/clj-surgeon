(ns clj-surgeon.mission-provider-fallback-events-test
  {:lane :battery}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-process :as process]
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-candidate-race :as race]
   [clj-surgeon.mission-events :as observer]
   [clj-surgeon.mission-typist-executor :as executor]
   [clj-surgeon.telemetry-events :as events]
   [clojure.test :refer [deftest is]]))

(def receipt
  {:usable true :content "private candidate source"
   :attempts [{:route "openrouter-cerebras" :request_started true
               :http_status 429 :error_type "provider-rate-limited"}
              {:route "groq" :request_started true :usable true
               :model "openai/gpt-oss-120b" :upstream "Groq"
               :request_wall_s 0.25 :completion_tokens 7 :reasoning_tokens 3
               :cost_usd 0.002 :cost_source "provider-reported"}]})

(def prior {:mission_id "M-123" :mission_state "applied" :mission_verb "owner_forms"
            :executor "typist" :candidate_count 3 :provider "openrouter"
            :model "openai/gpt-oss-120b" :upstream "Cerebras"})

(def authority
  {:root "/var/tmp/forge" :route {:k 1 :provider {:id :openrouter}}
   :dossier {:prompt "private prompt"}
   :transport {:interpreter "/usr/bin/python3" :source "# fake client, never executed"
               :sha256 (mission/sha256 "# fake client, never executed")}})

(defn fake-process [_]
  {:finished? true :exit 0 :termination-confirmed true :elapsed_ms 123
   :out (json/generate-string {:candidates [receipt]})})

(deftest actual-receipt-emits-provider-event-before-return
  (let [seen (atom [])]
    (binding [observer/*context* (atom prior)]
      (with-redefs [process/run-bounded! fake-process
                    events/record! #(swap! seen conj %)]
        (is (= (assoc receipt :transport-wall-ms 123)
               (executor/request-one! authority 0 (atom {}))))))
    (is (= 1 (count @seen)))
    (is (= "mission-provider-fallback" (:kind (first @seen))))
    (is (= "M-123" (:mission_id (first @seen))))
    (is (not (contains? (first @seen) :mission_state))))
  (let [ledger (java.io.File/createTempFile "provider-fallback-events-" ".jsonl")]
    (try
      (binding [observer/*context* (atom prior)]
        (with-redefs [events/events-file (fn [] (str ledger))
                      process/run-bounded! fake-process]
          (executor/request-one! authority 0 (atom {}))))
      (let [event (json/parse-string (slurp ledger) true)]
        (is (= "mission-provider-fallback" (:kind event)))
        (is (= "M-123" (:mission_id event)))
        (is (= "groq" (:provider event))))
      (finally (.delete ledger)))))

(deftest receipt-projection-keeps-only-observed-provider-facts
  (let [r (observer/provider-fallback-event prior receipt)]
    (is (= "mission-provider-fallback" (:kind r)))
    (is (= "groq" (:provider r)))
    (is (= "Groq" (:upstream r)))
    (is (= "openai/gpt-oss-120b" (:model r)))
    (is (true? (:ok r)))
    (is (= 250.0 (:wall_ms r)))
    (is (= 7 (:completion_tokens r)))
    (is (= 3 (:reasoning_tokens r)))
    (is (= 0.002 (:cost_usd r)))
    (is (= "provider-reported" (:cost_source r)))
    (is (nil? (:prompt_tokens r)))
    (is (not (contains? r :mission_state)))
    (is (not (.contains (pr-str r) "private")))))

(deftest planned-or-unstarted-fallback-is-not-an-event
  (doseq [candidate [{:fallback true :route {:generation {:fallback {:provider :groq}}}}
                     (dissoc receipt :attempts)
                     (update receipt :attempts subvec 0 1)
                     (update receipt :attempts conj {})
                     (assoc-in receipt [:attempts 0 :request_started] false)
                     (assoc-in receipt [:attempts 0 :http_status] 500)
                     (assoc-in receipt [:attempts 0 :error_type] "timeout")
                     (assoc-in receipt [:attempts 0 :route] "groq")
                     (assoc-in receipt [:attempts 1 :request_started] false)
                     (assoc-in receipt [:attempts 1 :route] "untrusted")]]
    (is (nil? (observer/provider-fallback-event prior candidate)))))

(deftest failed-fallback-keeps-unknown-identity-and-usage-unknown
  (let [failed (assoc-in receipt [:attempts 1]
                 {:route "groq" :request_started true :usable false :error_type "timeout"})
        r (observer/provider-fallback-event prior failed)]
    (is (false? (:ok r)))
    (is (= "timeout" (:error_type r)))
    (is (= "groq" (:provider r)))
    (doseq [k [:model :upstream :wall_ms :prompt_tokens :completion_tokens :reasoning_tokens :cost_usd :cost_source]]
      (is (nil? (get r k)))))
  (let [r (observer/provider-fallback-event prior (assoc-in receipt [:attempts 1 :model] "wrong"))]
    (is (false? (:ok r)))
    (is (= "provider-identity-unverified" (:error_type r)))
    (is (nil? (:model r)))))

(deftest receipt-fields-cannot-smuggle-private-data
  (let [candidate (update-in receipt [:attempts 1] merge
                    {:usable false :error_type "secret-body" :model "secret-model"
                     :upstream "secret-upstream" :content "secret-source"
                     :cost_usd "secret-cost" :request_wall_s Double/POSITIVE_INFINITY
                     :completion_tokens true :reasoning_tokens -1})
        r (observer/provider-fallback-event (assoc prior :secret "secret-context") candidate)]
    (is (= "provider-fallback-refused" (:error_type r)))
    (is (not (.contains (pr-str r) "secret")))
    (doseq [k [:wall_ms :completion_tokens :reasoning_tokens :cost_usd :cost_source :model :upstream]]
      (is (nil? (get r k)))))
  (let [r (observer/provider-fallback-event prior (assoc-in receipt [:attempts 1 :cost_usd] 0))]
    (is (= 0.0 (:cost_usd r)))
    (is (= "provider-reported" (:cost_source r)))))

(deftest logger-failure-cannot-change-transport-result
  (with-redefs [process/run-bounded! fake-process
                events/record! (fn [_] (throw (ex-info "private logger failure" {})))]
    (is (= (assoc receipt :transport-wall-ms 123)
           (executor/request-one! authority 0 (atom {}))))))

(deftest worker-threads-inherit-only-the-explicit-mission-context
  (let [seen (atom [])]
    (binding [observer/*context* (atom prior)]
      (with-redefs [process/run-bounded! fake-process
                    events/record! #(swap! seen conj %)]
        (let [handle (executor/request-candidates! (assoc-in authority [:route :k] 3))]
          (try
            (dotimes [_ 3] (is (:usable (race/next! handle))))
            (is (nil? (race/next! handle)))
            (finally (is (:terminated? (race/close! handle))))))))
    (is (= 3 (count @seen)))
    (is (every? #(= "M-123" (:mission_id %)) @seen))
    (is (every? #(not (contains? % :mission_state)) @seen))))

(deftest direct-executor-has-no-invented-mission-id
  (let [seen (atom [])]
    (binding [observer/*context* nil]
      (with-redefs [process/run-bounded! fake-process
                    events/record! #(swap! seen conj %)]
        (executor/request-one! authority 0 (atom {}))))
    (is (= 1 (count @seen)))
    (is (nil? (:mission_id (first @seen))))))
