(ns request-shape-compression-screen-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]]
   [owner-aware-symbol-migration :as migration]
   [request-shape-compression-screen :as screen]))

(defn- keywordized-oracle []
  (json/parse-string (json/generate-string migration/oracle-request) true))

(deftest replacement-groups-round-trip-the-frozen-oracle
  (let [request (keywordized-oracle)
        shape (#'screen/replacement-group-shape request)
        expanded (#'screen/expand-replacement-groups shape)
        grouped-sites
        (reduce +
                (for [group (:replacement_groups shape)
                      site (:sites group)]
                  (count (:forms site))))]
    (testing "the pure shape carries every exact decision"
      (is (= (#'screen/edit-multiset (:edits request))
             (#'screen/edit-multiset expanded))))
    (testing "the frozen 51-edit request earns a bounded reduction"
      (is (= 4 (count (:replacement_groups shape))))
      (is (= 12 grouped-sites))
      (is (= 21 (count (:edits shape))))
      (is (= 5220 (#'screen/json-bytes shape)))
      (is (= 1189 (- (#'screen/json-bytes request)
                     (#'screen/json-bytes shape)))))))

(deftest replacement-groups-do-not-hide-unique-decisions
  (let [request {:workspace_root "/tmp/workspace"
                 :edits [{:file "src/a.clj"
                          :within {:form "alpha"}
                          :from ":old-a"
                          :to ":new-a"
                          :matches 1}
                         {:file "src/b.clj"
                          :within {:form "beta"}
                          :from ":old-b"
                          :to ":new-b"
                          :matches 1}]}
        shape (#'screen/replacement-group-shape request)]
    (is (empty? (:replacement_groups shape)))
    (is (= 2 (count (:edits shape))))
    (is (= (#'screen/edit-multiset (:edits request))
           (#'screen/edit-multiset
             (#'screen/expand-replacement-groups shape))))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (clojure.test/run-tests 'request-shape-compression-screen-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
