(ns request-shape-composition-screen-test
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [clojure.walk :as walk]
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

(deftest in-range-wrong-index-can-silently-mutate-the-wrong-file
  (let [workspace
        (.toFile
          (java.nio.file.Files/createTempDirectory
            "clj-surgeon-wrong-index-"
            (make-array java.nio.file.attribute.FileAttribute 0)))
        intended (io/file workspace "src/intended.clj")
        wrong (io/file workspace "src/wrong.clj")
        intended-before "(ns intended)\n\n(defn shared [] :old)\n"
        wrong-before "(ns wrong)\n\n(defn shared [] :old)\n"
        wrong-after "(ns wrong)\n\n(defn shared [] :new)\n"
        shape
        {:files ["src/intended.clj" "src/wrong.clj"]
         :replacement_groups
         [{:from ":old"
           :to ":new"
           :sites [{:file_index 1 :forms ["shared"]}]}]
         :edits []}
        selected #{:file-index :replacement-groups}]
    (try
      (.mkdirs (.getParentFile intended))
      (spit intended intended-before)
      (spit wrong wrong-before)
      (let [edits (composition/expanded-edits shape selected {})
            request {"edits" (walk/stringify-keys edits)}
            result
            (mcp-tool/execute-request!
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath (io/file workspace "receipts"))}
              request)]
        (is (= "src/wrong.clj" (:file (first edits))))
        (is (:ok result) (pr-str result))
        (is (:committed result))
        (is (:verification_complete result))
        (is (= intended-before (slurp intended)))
        (is (= wrong-after (slurp wrong))))
      (finally
        (fs/delete-tree workspace)))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (clojure.test/run-tests 'request-shape-composition-screen-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
