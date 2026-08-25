(ns clj-surgeon.mcp-program-tool-test
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-program-tool :as program]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-program-test"
             (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

(def fidelity-source
  (str "(ns dogfood.fidelity)\n\n"
       "(def configs\n"
       "  [^{:owner \"a\"} {:name :a, :timeout 100} ; keep-a\n"
       "   {:name :b\n"
       "    :timeout 100 ; keep-b\n"
       "    :enabled? true}])\n"))

(def transform-request
  {:file "src/dogfood/fidelity.clj"
   :expression (str "(-> (form 'configs) (match :timeout) right "
                    "(transform #(+ % 50)))")
   :expect {:matches 2
            :max_changed_characters 6}})

(deftest previews-one-program-as-several-lossless-addressed-edits
  (let [root (temp-dir)
        source-file (io/file root "src/dogfood/fidelity.clj")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file fidelity-source)
      (let [result (program/execute-request!
                     {:project-root (.getPath root)}
                     transform-request)]
        (is (:ok result))
        (is (= :transform-preview (:operation result)))
        (is (= 2 (:match-count result)))
        (is (= 2 (:edit-count result)))
        (is (= fidelity-source (slurp source-file)))
        (is (= 2 (count (re-seq #"-100\n\+150" (:diff result)))))
        (is (not (contains? result :future-source))))
      (finally
        (delete-tree! root)))))

(deftest commits-the-compiled-addressed-edits-with-read-back-proof
  (let [root (temp-dir)
        source-file (io/file root "src/dogfood/fidelity.clj")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file fidelity-source)
      (let [result (program/execute-request!
                     {:project-root (.getPath root)}
                     (assoc transform-request :commit true))]
        (is (:ok result))
        (is (= :transform! (:operation result)))
        (is (:committed result))
        (is (= 2 (:match-count result)))
        (is (= (str/replace fidelity-source "100" "150")
               (slurp source-file)))
        (is (= 2 (get-in result [:receipt :inverse-edit-count])))
        (is (= 1 (get-in result [:verified :file-count]))))
      (finally
        (delete-tree! root)))))

(deftest exact-count-and-churn-budgets-refuse-before-write
  (let [root (temp-dir)
        source-file (io/file root "src/dogfood/fidelity.clj")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file fidelity-source)
      (testing "cardinality"
        (let [result (program/execute-request!
                       {:project-root (.getPath root)}
                       (assoc-in transform-request [:expect :matches] 3))]
          (is (false? (:ok result)))
          (is (= :expected-count-mismatch (:error-type result)))
          (is (:source-unchanged result))))
      (testing "changed characters"
        (let [result (program/execute-request!
                       {:project-root (.getPath root)}
                       (assoc-in transform-request
                                 [:expect :max_changed_characters]
                                 1))]
          (is (false? (:ok result)))
          (is (= :change-budget-exceeded (:error-type result)))
          (is (:source-unchanged result))))
      (is (= fidelity-source (slurp source-file)))
      (finally
        (delete-tree! root)))))

(deftest one-shot-commit-refuses-comment-bearing-selected-subtrees
  (let [root (temp-dir)
        source "(ns dogfood.map)\n(def settings {:timeout 100 ; keep\n :ready? true})\n"
        source-file (io/file root "src/dogfood/map.clj")
        request {:file "src/dogfood/map.clj"
                 :expression (str "(-> (form 'settings) initializer "
                                  "(transform #(assoc % :retries 3)))")
                 :expect {:matches 1
                          :max_changed_characters 200}
                 :commit true}]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file source)
      (let [result (program/execute-request!
                     {:project-root (.getPath root)}
                     request)]
        (is (false? (:ok result)))
        (is (= :lossless-commit-refused (:error-type result)))
        (is (:source-unchanged result))
        (is (= source (slurp source-file))))
      (finally
        (delete-tree! root)))))

(deftest unrecovered-write-failure-never-claims-source-unchanged
  (let [root (temp-dir)
        source-file (io/file root "src/dogfood/fidelity.clj")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file fidelity-source)
      (let [result
            (with-redefs [transaction/commit-compiled!
                          (constantly
                            {:ok false
                             :error-type :transaction-recovery-required
                             :rolled-back false
                             :error "manual recovery required"})]
              (program/execute-request!
                {:project-root (.getPath root)}
                (assoc transform-request :commit true)))]
        (is (false? (:ok result)))
        (is (= :transaction-recovery-required (:error-type result)))
        (is (false? (:source-unchanged result))))
      (finally
        (delete-tree! root)))))
