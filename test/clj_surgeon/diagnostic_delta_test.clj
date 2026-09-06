(ns clj-surgeon.diagnostic-delta-test
  (:require
   [clj-surgeon.diagnostic-delta :as delta]
   [clojure.test :refer [deftest is testing]]))

(def existing-warning
  {:filename "src/sample/core.clj"
   :type :unresolved-symbol
   :level :warning
   :message "Unresolved symbol: legacy"
   :row 4
   :col 7})

(def new-error
  {:filename "src/sample/caller.clj"
   :type :unresolved-var
   :level :error
   :message "Unresolved var: sample.core/added"
   :row 9
   :col 3})

(deftest diagnostic-delta-exhausts-the-pure-multiset-contract
  (testing "clean baseline and future"
    (is (= {:ok true
            :baseline-count 0
            :future-count 0
            :introduced-count 0
            :removed-count 0
            :unchanged-count 0
            :blocking-introduced-count 0
            :introduced []
            :removed []
            :blocking-introduced []}
           (delta/diagnostic-delta {:findings []} {:findings []}))))

  (testing "line drift does not turn a retained finding into a regression"
    (let [moved (assoc existing-warning :row 400 :col 2)
          result (delta/diagnostic-delta {:findings [existing-warning]}
                                         {:findings [moved]})]
      (is (:ok result))
      (is (= 1 (:unchanged-count result)))
      (is (zero? (:introduced-count result)))))

  (testing "multiplicity detects one additional identical finding"
    (let [duplicate (assoc existing-warning :row 99)
          result (delta/diagnostic-delta {:findings [existing-warning]}
                                         {:findings [existing-warning duplicate]})]
      (is (false? (:ok result)))
      (is (= 1 (:introduced-count result)))
      (is (= [duplicate] (:blocking-introduced result)))))

  (testing "removed findings and input order do not affect acceptance"
    (let [info {:filename "./src/sample/core.clj"
                :type "redundant-expression"
                :level "info"
                :message "Redundant expression"}
          result (delta/diagnostic-delta {:findings [existing-warning info]}
                                         {:findings [info]})]
      (is (:ok result))
      (is (= 1 (:removed-count result)))
      (is (= existing-warning (first (:removed result))))))

  (testing "new warnings and errors block while new info remains evidence"
    (let [info {:filename "src/sample/core.clj"
                :type :redundant-expression
                :level :info
                :message "Redundant expression"}
          result (delta/diagnostic-delta {:findings []}
                                         {:findings [info new-error]})]
      (is (false? (:ok result)))
      (is (= 2 (:introduced-count result)))
      (is (= [new-error] (:blocking-introduced result)))))

  (testing "Windows and dot-relative paths normalize to the same identity"
    (let [windows (assoc existing-warning :filename "src\\sample\\core.clj")
          relative (assoc existing-warning :filename "./src/sample/core.clj")]
      (is (:ok (delta/diagnostic-delta [windows] [relative])))))

  (testing "malformed snapshots refuse as stable data"
    (doseq [invalid [nil {} {:findings nil} {:findings [{}]}]]
      (is (= :invalid-diagnostic-snapshot
             (:error-type (delta/diagnostic-delta invalid {:findings []})))))))

(deftest diagnostic-delta-is-order-independent
  (let [info {:filename "src/sample/core.clj"
              :type :redundant-expression
              :level :info
              :message "Redundant expression"}
        left (delta/diagnostic-delta {:findings [existing-warning]}
                                     {:findings [info new-error existing-warning]})
        right (delta/diagnostic-delta {:findings [existing-warning]}
                                      {:findings [existing-warning new-error info]})]
    (is (= (select-keys left [:ok :baseline-count :future-count
                              :introduced-count :removed-count
                              :unchanged-count :blocking-introduced-count])
           (select-keys right [:ok :baseline-count :future-count
                               :introduced-count :removed-count
                               :unchanged-count :blocking-introduced-count])))
    (is (= (frequencies (map delta/finding-fingerprint (:introduced left)))
           (frequencies (map delta/finding-fingerprint (:introduced right)))))))
