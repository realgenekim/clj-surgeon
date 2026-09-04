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

(deftest description-distinguishes-owner-and-file-wide-roots
  (is (str/includes? program/transform-tool-description
                     "Start with (form 'owner)"))
  (is (str/includes? program/transform-tool-description
                     "Start with []"))
  (is (str/includes? program/transform-tool-description
                     "expect.matches is the authoritative exact cardinality guard"))
  (is (not (str/includes? program/transform-tool-description
                          "(expect-count 1)"))))

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
        (is (= false (:verification_complete result)))
        (is (= "commit" (:next_action result)))
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
        (is (= true (:verification_complete result)))
        (is (= "none" (:next_action result)))
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

(deftest compiles-several-programs-against-one-frozen-snapshot
  (let [sources {"src/a.clj" "(ns a)\n(def cfg {:timeout 100})\n"
                 "src/b.clj" "(ns b)\n(def label \"old\")\n"}
        programs [{:file "src/a.clj"
                   :expression "(-> (form 'cfg) initializer (match :timeout) right (transform #(+ % 50)))"
                   :expect {:matches 1 :max_changed_characters 3}}
                  {:file "src/b.clj"
                   :expression "(-> (form 'label) initializer (transform (constantly \"new\")))"
                   :expect {:matches 1 :max_changed_characters 5}}]
        result (program/compile-programs sources programs)]
    (is (:ok result) (pr-str result))
    (is (= 2 (:program-count result)))
    (is (= 2 (:edit-count result)))
    (is (= 2 (get-in result [:compiled :changed-file-count])))
    (is (= "(ns a)\n(def cfg {:timeout 150})\n"
           (get-in result [:compiled :future-sources "src/a.clj"])))
    (is (= "(ns b)\n(def label \"new\")\n"
           (get-in result [:compiled :future-sources "src/b.clj"])))
    (let [refused (program/compile-programs
                    sources (assoc-in programs [1 :expect :matches] 2))]
      (is (false? (:ok refused)))
      (is (= :expected-count-mismatch (:error-type refused)))
      (is (= 1 (:program-index refused)))
      (is (= "src/b.clj" (:program-file refused)))
      (is (:source-unchanged refused)))))

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

(deftest callback-reports-elapsed-time-on-success-and-refusal
  (let [root (temp-dir)
        source-file (io/file root "src/dogfood/fidelity.clj")
        calls (atom [])
        callback (fn [content error? structured]
                   (swap! calls conj {:content (first content)
                                      :error? error?
                                      :structured structured}))]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file fidelity-source)
      (program/init! {:project-root (.getPath root)})
      (program/handle-transform-clojure nil transform-request callback)
      (let [{:keys [content error? structured]} (first @calls)
            elapsed (get-in structured [:measured :elapsed_ms])]
        (is (false? error?))
        (is (number? elapsed))
        (when (number? elapsed)
          (is (<= 0 elapsed))
          (is (str/includes?
                content (format "%.2f ms" elapsed)))))
      (program/init! nil)
      (program/handle-transform-clojure nil transform-request callback)
      (let [{:keys [content error? structured]} (second @calls)
            elapsed (get-in structured [:measured :elapsed_ms])]
        (is (true? error?))
        (is (number? elapsed))
        (when (number? elapsed)
          (is (str/includes?
                content (format "%.2f ms" elapsed)))))
      (finally
        (program/init! nil)
        (delete-tree! root)))))
