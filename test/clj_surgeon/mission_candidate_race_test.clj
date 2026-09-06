(ns clj-surgeon.mission-candidate-race-test
  {:lane :fast}
  (:require
   [clj-surgeon.mission-candidate-race :as race]
   [clojure.test :refer [deftest is]])
  (:import
   (java.util.concurrent CountDownLatch TimeUnit)))

(deftest validates-bounds-before-creating-workers
  (doseq [k [0 6 -1 1.5 nil]]
    (is (thrown? clojure.lang.ExceptionInfo (race/start! k identity))))
  (is (thrown? clojure.lang.ExceptionInfo (race/start! 1 nil))))

(deftest returns-completion-order-and-original-index
  (let [release (CountDownLatch. 1)
        h (race/start! 2 (fn [index]
                           (when (zero? index) (.await release))
                           {:index 99 :content (str index)}))]
    (try
      (is (= {:index 1 :content "1"} (race/next! h)))
      (.countDown release)
      (is (= {:index 0 :content "0"} (race/next! h)))
      (is (nil? (race/next! h)))
      (let [closed (race/close! h)]
        (is (:terminated? closed))
        (is (= #{0 1} (set (map :index (:completed closed)))))
        (is (empty? (:cancelled closed))))
      (finally (.countDown release) (race/close! h)))))

(deftest redacts-exceptions-and-rejects-nonmap-responses
  (let [h (race/start! 2 (fn [i] (if (zero? i)
                                   (throw (ex-info "secret-key" {:key "secret-key"}))
                                   "not a candidate")))]
    (try
      (let [responses [(race/next! h) (race/next! h)]]
        (is (= #{"candidate-request-failed" "candidate-response-invalid"}
               (set (map :error_type responses))))
        (is (every? #(false? (:usable %)) responses))
        (is (not (.contains (pr-str responses) "secret-key"))))
      (finally (is (:terminated? (race/close! h)))))))

(deftest close-cancels-owned-waiters-and-retains-completed-result
  (let [started (CountDownLatch. 1) release (CountDownLatch. 1)
        h (race/start! 2 (fn [i]
                           (if (zero? i) {:content "ready"}
                             (do (.countDown started) (.await release) {:content "late"}))))]
    (try
      (is (.await started 5 TimeUnit/SECONDS))
      (is (= 0 (:index (race/next! h))))
      (let [closed (race/close! h)]
        (is (:terminated? closed))
        (is (= [1] (:cancelled closed)))
        (is (some #(= "ready" (:content %)) (:completed closed)))
        (is (nil? (race/next! h)))
        (is (= closed (race/close! h))))
      (finally (.countDown release) (race/close! h)))))

(deftest single-and-maximum-size-races-exhaust-and-retain-results
  (doseq [k [1 5]]
    (let [h (race/start! k (fn [index] {:content (str index)}))]
      (try
        (let [results (vec (repeatedly k #(race/next! h)))]
          (is (= (set (range k)) (set (map :index results))))
          (is (nil? (race/next! h)))
          (is (= k (count (:completed (race/close! h))))))
        (finally (is (:terminated? (race/close! h))))))))
