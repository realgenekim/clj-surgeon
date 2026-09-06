(ns clj-surgeon.mission-candidate-test
  (:require
   [clj-surgeon.mission-candidate :as candidate]
   [clojure.test :refer [deftest is testing]]))

(def basis
  {:sources {"src/a.clj" "(ns a)\n(defn run [] [1 2])\n;; protected\n"}
   :owners [{:file "src/a.clj" :owner "run" :start 7 :end 26}]
   :budget {:max-files 1 :max-changed-chars 100}})

(def first-edit {:file "src/a.clj" :before "[1 2]" :after "[3 2]"})

(deftest pure-candidate-preserves-every-unselected-byte
  (let [result (candidate/compile-candidate basis [first-edit])]
    (is (:ok result))
    (is (= "(ns a)\n(defn run [] [3 2])\n;; protected\n"
           (get-in result [:future-sources "src/a.clj"])))
    (is (= 1 (:changed-files result)))
    (is (= (:sources basis) (:original-sources result)))))

(deftest original-snapshot-not-sequential-rewrite
  ;; Regression from the prototype duplicate-file-block bug: both edits must
  ;; survive, or the candidate must refuse. No last-block-wins success.
  (let [changes [{:file "src/a.clj" :before "1" :after "3"}
                 {:file "src/a.clj" :before "2" :after "4"}]
        a (candidate/compile-candidate basis changes)
        b (candidate/compile-candidate basis (vec (reverse changes)))]
    (is (:ok a))
    (is (= (:future-sources a) (:future-sources b)))
    (is (= "(ns a)\n(defn run [] [3 4])\n;; protected\n"
           (get-in a [:future-sources "src/a.clj"])))))

(deftest candidate-refusals-are-data-and-never-partial-writes
  (doseq [[label b changes]
          [[:empty basis []]
           [:unknown-file basis [(assoc first-edit :file "src/b.clj")]]
           [:absolute basis [(assoc first-edit :file "/src/a.clj")]]
           [:alias-path basis [(assoc first-edit :file "src/./a.clj")]]
           [:parent-path basis [(assoc first-edit :file "src/../a.clj")]]
           [:empty-anchor basis [(assoc first-edit :before "")]]
           [:missing-anchor basis [(assoc first-edit :before "99")]]
           [:outside-owner basis [{:file "src/a.clj" :before "protected" :after "bad"}]]
           [:overlap basis [first-edit {:file "src/a.clj" :before "1" :after "9"}]]
           [:duplicate basis [first-edit first-edit]]
           [:syntax-error basis [(assoc first-edit :after "[")]]
           [:no-op basis [(assoc first-edit :after "[1 2]")]]
           [:budget (assoc-in basis [:budget :max-changed-chars] 1) [first-edit]]
           [:file-budget (assoc-in basis [:budget :max-files] 0) [first-edit]]
           [:malformed-span (assoc-in basis [:owners 0 :end] 999) [first-edit]]
           [:unknown-key basis [(assoc first-edit :shell "bad")]]]]
    (testing (name label)
      (let [result (candidate/compile-candidate b changes)]
        (is (false? (:ok result)))
        (is (keyword? (:error-type result)))
        (is (not (contains? result :future-sources)))))))

(deftest ambiguity-is-within-the-authorized-span
  (let [b {:sources {"src/a.clj" "(def x [1 1])"}
           :owners [{:file "src/a.clj" :owner "x" :start 0 :end 13}]
           :budget {:max-files 1 :max-changed-chars 100}}]
    (is (= :candidate-ambiguous-anchor
           (:error-type (candidate/compile-candidate b
                          [{:file "src/a.clj" :before "1" :after "2"}]))))))

(deftest deep-candidate-is-a-refusal-not-a-crash
  (let [result (try
                 (candidate/compile-candidate
                   {:sources {"src/a.clj" "(def a 1)"}
                    :owners [{:file "src/a.clj" :owner "a" :start 0 :end 9}]
                    :budget {:max-files 1 :max-changed-chars 20000}}
                   [{:file "src/a.clj" :before "1"
                     :after (str (apply str (repeat 6000 "(")) "1"
                                 (apply str (repeat 6000 ")")))}])
                 (catch StackOverflowError _ :stack-overflow))]
    (is (map? result))
    (is (false? (:ok result)))))
