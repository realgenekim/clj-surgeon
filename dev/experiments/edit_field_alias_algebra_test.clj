(ns edit-field-alias-algebra-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is run-tests testing]]
   [edit-field-alias-algebra :as algebra]))

(def base-edit
  {"file" "src/sample/app.clj"
   "within" {"namespace" true}
   "matches" 1})

(def all-fields
  ["from" "to" "old" "new" "before" "after"])

(def accepted-field-sets
  #{#{"from" "to"}
    #{"old" "new"}
    #{"before" "after"}})

(defn subsets
  [values]
  (reduce (fn [result value]
            (into result (map #(conj % value) result)))
          [#{}]
          values))

(defn edit-with-fields
  [fields]
  (into base-edit (map (fn [field] [field (str "value-for-" field)]) fields)))

(deftest exactly-three-complete-pairs-are-accepted
  (doseq [[fields expected-from expected-to]
          [[#{"from" "to"} "value-for-from" "value-for-to"]
           [#{"old" "new"} "value-for-old" "value-for-new"]
           [#{"before" "after"} "value-for-before" "value-for-after"]]]
    (let [result (algebra/lower-edit (edit-with-fields fields))]
      (is (:ok result) (str "expected accepted pair " fields))
      (is (= expected-from (get-in result [:edit "from"])))
      (is (= expected-to (get-in result [:edit "to"])))
      (is (= (select-keys base-edit ["file" "within" "matches"])
             (select-keys (:edit result) ["file" "within" "matches"])))
      (is (= ["from" "to"]
             (get-in result [:normalization :emitted_pair]))))))

(deftest every-other-subset-refuses-before-write
  (doseq [fields (subsets all-fields)]
    (testing (pr-str fields)
      (let [result (algebra/lower-edit (edit-with-fields fields))]
        (is (= (contains? accepted-field-sets fields) (:ok result)))
        (when-not (:ok result)
          (is (:source_unchanged result))
          (is (false? (:write_authority result)))
          (is (= (vec (sort fields)) (:present_fields result))))))))

(deftest refusal-categories-distinguish-repair-shapes
  (doseq [[fields reason]
          [[#{} :missing-edit-field-pair]
           [#{"from"} :partial-edit-field-pair]
           [#{"old" "to"} :mixed-edit-field-pairs]
           [#{"from" "to" "old"} :mixed-edit-field-pairs]
           [#{"from" "to" "old" "new"} :multiple-edit-field-pairs]
           [#{"old" "new" "before" "after"} :multiple-edit-field-pairs]
           [(set all-fields) :multiple-edit-field-pairs]]]
    (let [result (algebra/lower-edit (edit-with-fields fields))]
      (is (= reason (:reason result)) (pr-str fields))
      (is (re-find #"call edit_clojure once" (:remedy result)))
      (is (not (re-find #"apply_clojure_changes" (:remedy result)))))))

(deftest duplicate-authority-refuses-even-when-values-agree
  (let [result (algebra/lower-edit
                 (merge base-edit
                        {"from" "same-old"
                         "to" "same-new"
                         "old" "same-old"
                         "new" "same-new"}))]
    (is (false? (:ok result)))
    (is (= :multiple-edit-field-pairs (:reason result)))
    (is (:source_unchanged result))))

(deftest a-single-bad-edit-refuses-the-whole-batch-with-its-path
  (let [good (merge base-edit {"old" "x" "new" "y"})
        bad (merge base-edit {"before" "x" "to" "y"})
        result (algebra/lower-edits [good bad good])]
    (is (false? (:ok result)))
    (is (= ["edits" 1] (:path result)))
    (is (nil? (:edits result)))
    (is (:source_unchanged result))))

(deftest retained-capture-manifest-binds-the-motivating-evidence
  (let [manifest (-> "dev/experiments/edit_field_alias_capture_manifest.edn"
                     io/file
                     slurp
                     edn/read-string)
        calls (:calls manifest)]
    (is (= "d109fa0bef5c40a9cdb9313bfa5ff9e361258d338e9fd80c5ba92c8d81b5eded"
           (get-in manifest [:archive :sha256])))
    (is (= 8 (count calls)))
    (is (= {[:refused ["old" "new"]] 3
            [:refused ["before" "after"]] 2
            [:committed ["from" "to"]] 3}
           (frequencies (map (juxt :status :pair) calls))))
    (is (= 7 (count (set (map :arguments-sha256 calls)))))
    (is (= 2 (get (frequencies (map :arguments-sha256 calls))
                  "9071930bde7b0299945f518a0631d41829ad0fecca53628dfff9c52a15fccf92")))
    (is (= 51 (get-in manifest [:archive :workload :total-edits])))
    (is (= "a38392ed0b1791fdebb94440ed4edd4fb226a7480bee74c2dcdc2a233ec3144d"
           (get-in manifest
                   [:canonicalization :all-eight-semantic-sha256])))
    (is (= "unknown-fields" (get-in manifest [:refusal :reason])))
    (is (re-find #"apply_clojure_changes"
                 (get-in manifest [:refusal :current-remedy])))))

(let [{:keys [fail error]} (run-tests)]
  (when (pos? (+ fail error))
    (System/exit 1)))
