(ns owner-aware-symbol-migration-test
  (:require
   [clojure.test :refer [deftest is run-tests testing]]
   [owner-aware-symbol-migration :as migration]))

(deftest grouped-table-expands-without-losing-owner-or-count
  (let [manifest
        {"workspace_root" "/workspace"
         "edits" []
         "delete_owners" [{"file" "src/app.clj" "forms" ["old-owner"]}]
         "symbol_migration"
         {"target_alias" "new-provider"
          "target_rule" "preserve-name"
          "columns" ["owner" "from" "matches"]
          "files" [["src/app.clj"
                    [["render-one" "old/row" 1]
                     ["render-many" "row" 3]]]]}}
        result (migration/compile-manifest manifest)]
    (is (:ok result))
    (is (= [{"file" "src/app.clj"
             "within" {"form" "render-one"}
             "from" "old/row"
             "to" "new-provider/row"
             "matches" 1}
            {"file" "src/app.clj"
             "within" {"form" "render-many"}
             "from" "row"
             "to" "new-provider/row"
             "matches" 3}]
           (get-in result [:request "edits"])))
    (is (= [{"file" "src/app.clj" "forms" ["old-owner"]}]
           (get-in result [:request "delete_owners"])))
    (is (nil? (get-in result [:request "symbol_migration"])))))

(deftest malformed-table-refuses-before-current-contract
  (doseq [[label path value expected]
          [[:columns ["symbol_migration" "columns"]
            ["from" "owner" "matches"] :unsupported-columns]
           [:target-rule ["symbol_migration" "target_rule"]
            "guess-target" :unsupported-target-rule]
           [:count ["symbol_migration" "files" 0 1 0 2]
            0 :invalid-site]]]
    (testing (name label)
      (let [result (migration/compile-manifest
                     (assoc-in migration/candidate-manifest path value))]
        (is (false? (:ok result)))
        (is (= expected (:error-type result)))))))

(deftest frozen-cleanup-has-exact-compiler-parity
  (let [result (migration/report)]
    (is (:all-correct result))
    (is (= migration/oracle-payload-bytes
           (get-in result [:payload :oracle-bytes])))
    (is (<= (get-in result [:payload :candidate-bytes])
            migration/candidate-payload-budget))
    (is (= 51 (get-in result [:parity :match-count])))
    (is (= 9 (get-in result [:parity :changed-file-count])))
    (is (= 23 (get-in result [:decision :owner-row-count])))
    (is (= 27 (get-in result [:decision :declared-match-count])))
    (is (get-in result [:parity :normalized-transaction-equal]))
    (is (get-in result [:parity :addressed-replacements-equal]))
    (is (get-in result [:parity :future-hashes-equal]))
    (is (get-in result [:parity :expected-after-hashes-equal]))
    (is (get-in result [:refusals :wrong-owner :refused]))
    (is (get-in result [:refusals :wrong-count :refused]))
    (is (zero? (:model-calls result)))
    (is (zero? (:mutation-actions result)))))

(let [{:keys [fail error]} (run-tests)]
  (when (pos? (+ fail error))
    (System/exit 1)))
