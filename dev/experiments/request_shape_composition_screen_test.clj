(ns request-shape-composition-screen-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]]
   [owner-aware-symbol-migration :as migration]
   [request-shape-composition-screen :as composition]))

(defn- keywordized-oracle []
  (json/parse-string (json/generate-string migration/oracle-request) true))

(defn- pair [report left right]
  (some #(when (= [left right] (:order %)) %)
        (:ordered-pairs report)))

(deftest every-ordered-pair-round-trips-the-same-frozen-decision
  (let [report (composition/report (keywordized-oracle))]
    (is (= 42 (count (:ordered-pairs report))))
    (is (every? :round-trip (:ordered-pairs report)))
    (is (= 34 (count (:clears-20-percent report))))
    (is (= 8 (count (:misses-20-percent report))))
    (doseq [{[left right] :order :as forward} (:ordered-pairs report)]
      (testing (str left " then " right)
        (is (= (dissoc forward :order)
               (dissoc (pair report right left) :order)))))))

(deftest overlap-and-interference-stay-visible
  (let [report (composition/report (keywordized-oracle))]
    (testing "default omission is already contained in replacement groups"
      (is (= {:saved-bytes 1189
              :increment-over-best 0
              :overlap-bytes 360
              :composition :sub-additive}
             (select-keys
               (pair report :omit-default-matches :replacement-groups)
               [:saved-bytes
                :increment-over-best
                :overlap-bytes
                :composition]))))
    (testing "file indexing makes file groups worse"
      (is (= {:saved-bytes 1082
              :increment-over-best -82
              :overlap-bytes 918
              :composition :interfering}
             (select-keys
               (pair report :file-index :file-groups)
               [:saved-bytes
                :increment-over-best
                :overlap-bytes
                :composition]))))))

(deftest smallest-new-pair-and-best-triples-are-frozen
  (let [report (composition/report (keywordized-oracle))
        candidate (pair report :file-index :replacement-groups)]
    (testing "the smallest new pair clears the byte gate"
      (is (= 4892 (:bytes candidate)))
      (is (= 1517 (:saved-bytes candidate)))
      (is (= 328 (:increment-over-best candidate)))
      (is (> (:reduction candidate) 0.20)))
    (testing "the absolute best triple includes the existing relation facade"
      (is (= [:file-index
              :closed-relations-with-require-delta
              :positional-tuples]
             (get-in report [:best-triple :members])))
      (is (= 3477 (get-in report [:best-triple :bytes]))))
    (testing "the best triple that does not reuse relations is explicit"
      (is (= [:file-index :replacement-groups :positional-tuples]
             (get-in report [:best-new-triple :members])))
      (is (= 4200 (get-in report [:best-new-triple :bytes])))
      (is (= 2209 (get-in report [:best-new-triple :saved-bytes]))))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (clojure.test/run-tests 'request-shape-composition-screen-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
