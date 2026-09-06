(ns clj-surgeon.outline-corpus-integration-test
  "Full repository serialization parity against the frozen legacy outline."
  {:lane :integration}
  (:require
   [clj-surgeon.outline :as outline]
   [clj-surgeon.outline-differential-test :refer [legacy-outline-source]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn- source-files
  [roots]
  (->> roots
       (mapcat (fn [root]
                 (let [base (io/file root)]
                   (when (.exists base) (file-seq base)))))
       (filter #(.isFile ^java.io.File %))
       (filter (fn [^java.io.File f]
                 (let [n (.getName f)]
                   (some #(str/ends-with? n %) [".clj" ".cljc" ".cljs"]))))
       (map #(.getPath ^java.io.File %))
       sort))

;; @spec MCP-OP-MEM-015
;; @spec TEST-ISO-001
(deftest single-parse-outline-is-byte-identical-over-the-repository
  (testing "every source file under src/ and test/ outlines identically"
    (let [paths (source-files ["src" "test"])
          mismatches (reduce
                       (fn [acc path]
                         (let [source (slurp path)
                               expected (pr-str
                                          (legacy-outline-source path source {}))
                               actual (pr-str
                                        (outline/outline-source path source {}))]
                           (if (= expected actual)
                             acc
                             (conj acc path))))
                       []
                       paths)]
      (is (<= 100 (count paths))
          "the differential must cover the whole tree, not a stub")
      (is (= [] mismatches)
          (str (count mismatches) " of " (count paths)
               " files outlined differently: "
               (str/join ", " (take 5 mismatches)))))))
