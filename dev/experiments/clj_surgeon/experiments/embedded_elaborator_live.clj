(ns clj-surgeon.experiments.embedded-elaborator-live
  "Live source-free idle-cell measurement for the embedded Spark supervisor."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-elaborator-intent :as intent]
   [clj-surgeon.mcp-elaborator-receipt :as receipt]
   [clj-surgeon.mcp-elaborator-supervisor :as supervisor]
   [clojure.string :as str])
  (:import
   (java.time Instant)))

(defn- wait-for-boot
  [spark-supervisor]
  (let [deadline (+ (System/currentTimeMillis) 15000)]
    (loop []
      (let [state (supervisor/state spark-supervisor)]
        (if (or (not= :starting (:status state))
                (>= (System/currentTimeMillis) deadline))
          state
          (do (Thread/sleep 25) (recur)))))))

(defn- public-turn
  [turn expected]
  {:ok (:ok turn)
   :error_type (:error_type turn)
   :turn_id (:turn_id turn)
   :turn_count (:turn_count turn)
   :candidate_count (:candidate_count turn)
   :latency_ms (:latency_ms turn)
   :input_tokens (:input_tokens turn)
   :output_tokens (:output_tokens turn)
   :result_class (:result_class turn)
   :tool_item_observed (:tool_item_observed turn)
   :reroute_observed (:reroute_observed turn)
   :runtime (:runtime turn)
   :exact_replacement (= expected (:replacement turn))
   :elaboration_sha256 (when (:replacement turn)
                         (intent/sha256 (:replacement turn)))
   :rate_limits_before (receipt/rate-limit-receipt
                         (:rate_limits_before turn))
   :rate_limits_after (receipt/rate-limit-receipt
                        (:rate_limits_after turn))})

(defn- bang
  [spark-supervisor body label]
  (let [model-input {:old_body body
                     :decision "Return the old body exactly unchanged."}
        turn (supervisor/elaborate!
               spark-supervisor
               {:model_input model-input
                :intent_sha256
                (intent/sha256
                  (intent/canonical-json {:measurement "idle-first-bang-v1"
                                          :cell label
                                          :model_input model-input}))})]
    (public-turn turn body)))

(defn- emit!
  [value]
  (println (json/generate-string value))
  (flush))

(defn- idle-cells
  []
  (let [configured (or (System/getenv "CLJ_SURGEON_SPARK_IDLE_MINUTES") "0")]
    (mapv parse-long (str/split configured #","))))

(defn -main
  [& _]
  (let [spark-supervisor (supervisor/start-background!)
        boot (wait-for-boot spark-supervisor)
        body (str "(defn source-free-real-shaped-measurement []\n  \""
                  (apply str (repeat 1200 "m"))
                  "\")")]
    (try
      (emit! {:event "boot"
              :timestamp (str (Instant/now))
              :state boot})
      (when (= :available (:status boot))
        (loop [previous 0
               [cell & remaining] (idle-cells)]
          (when cell
            (let [wait-minutes (- cell previous)]
              (when (pos? wait-minutes)
                (Thread/sleep (* wait-minutes 60 1000)))
              (emit! {:event "idle-cell"
                      :timestamp (str (Instant/now))
                      :idle_minutes cell
                      :first_bang (bang spark-supervisor body
                                        (str cell "-first"))
                      :immediate_midstream (bang spark-supervisor body
                                                 (str cell "-midstream"))})
              (recur cell remaining)))))
      (finally
        (emit! {:event "shutdown"
                :timestamp (str (Instant/now))
                :cleanup (supervisor/stop! spark-supervisor)})))))
