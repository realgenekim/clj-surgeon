(ns clj-surgeon.census-pool-test
  "Unit witnesses for the census plan-phase pool.

   The pool is the one place claypoole is used. What must hold is narrow and
   testable without a census: every input is mapped exactly once, the pool is
   bounded by the size it was given, no thread outlives the call, and the result
   is the same collection `map` would produce."
  (:require
   [clj-surgeon.census-pool :as census-pool]
   [clj-surgeon.relation-census :as census]
   [clojure.test :refer [deftest is testing]]))

(defn- eventually-dead?
  [threads]
  (loop [attempts 0]
    (cond
      (every? #(not (.isAlive ^Thread %)) threads) true
      (> attempts 100) false
      :else (do (Thread/sleep 20) (recur (inc attempts))))))

;; @spec MCP-OP-CENSUS-020
(deftest pooled-map-maps-every-input-exactly-once
  (let [inputs (vec (range 200))]
    (testing "the pool computes what map computes"
      (is (= (mapv inc inputs)
             (sort ((census-pool/pooled-map 8) inc inputs)))))
    (testing "a pool of one is still a complete map"
      (is (= (mapv inc inputs)
             (sort ((census-pool/pooled-map 1) inc inputs)))))
    (testing "each input is visited exactly once"
      (let [seen (atom [])]
        ((census-pool/pooled-map 4) #(do (swap! seen conj %) %) inputs)
        (is (= (frequencies inputs) (frequencies @seen)))))))

;; @spec MCP-OP-CENSUS-020
(deftest the-pool-is-bounded-and-outlives-nothing
  (let [threads (atom #{})
        work (fn [x] (swap! threads conj (Thread/currentThread)) (Thread/sleep 5) x)]
    ((census-pool/pooled-map 4) work (range 80))
    (testing "the pool never exceeds the size it was given"
      (is (<= (count @threads) 4) (str "threads used: " (count @threads))))
    (testing "work really was spread when there was work to spread"
      (is (> (count @threads) 1)))
    (testing "no worker outlives the call"
      (is (eventually-dead? @threads)))))

;; @spec MCP-OP-CENSUS-020
(deftest the-default-pool-is-the-box
  (is (= (census/effective-pool-size census/max-pool-size)
         (census-pool/default-pool-size)))
  (is (<= (census-pool/default-pool-size)
          (.availableProcessors (Runtime/getRuntime)))))
