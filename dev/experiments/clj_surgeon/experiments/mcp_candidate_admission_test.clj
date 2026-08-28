(ns clj-surgeon.experiments.mcp-candidate-admission-test
  (:require
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clojure.test :refer [deftest is run-tests testing]]))

(def edit-schema
  {:type "object"
   :additionalProperties false
   :properties {"workspace_root" {}
                "edits" {}
                "programs" {}
                "delete_owners" {}}
   :oneOf [{:anyOf [{:required ["edits"]}
                    {:required ["programs"]}
                    {:required ["delete_owners"]}]
            :not {:anyOf [{:required ["extraction"]}]}}]})

(def extraction-schema
  {:type "object"
   :additionalProperties false
   :properties {"workspace_root" {}
                "extraction" {}
                "verify" {}}
   :oneOf [{:required ["extraction"]
            :not {:anyOf [{:required ["edits"]}
                          {:required ["programs"]}
                          {:required ["delete_owners"]}]}}]})

(deftest public-schema-is-executable-admission-authority
  (testing "the edit entrance cannot smuggle an extraction into the shared kernel"
    (let [result (admission/authorize edit-schema
                                      {"workspace_root" "/tmp/work"
                                       "extraction" {"file" "src/a.clj"}})]
      (is (false? (:ok result)))
      (is (= :public-schema-denied (:error-type result)))
      (is (= ["extraction"] (:unexpected-fields result)))))
  (testing "the extraction entrance authorizes the same extraction"
    (is (= {:ok true}
           (admission/authorize extraction-schema
                                {"workspace_root" "/tmp/work"
                                 "extraction" {"file" "src/a.clj"}}))))
  (testing "unknown top-level fields fail closed"
    (let [result (admission/authorize edit-schema
                                      {"edits" [] "surprise" true})]
      (is (false? (:ok result)))
      (is (= ["surprise"] (:unexpected-fields result)))))
  (testing "one valid public branch must match"
    (is (= {:ok true} (admission/authorize edit-schema {"programs" []})))
    (is (= 0 (:matching-branches (admission/authorize edit-schema {}))))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (run-tests 'clj-surgeon.experiments.mcp-candidate-admission-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
