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

(defn- java-json-value [value]
  (cond
    (map? value)
    (let [result (java.util.LinkedHashMap.)]
      (doseq [[key child] value]
        (.put result (name key) (java-json-value child)))
      result)

    (sequential? value)
    (java.util.ArrayList. (map java-json-value value))

    :else value))

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

(deftest property-values-are-authority-not-documentation
  (let [commit-schema {:type "object"
                       :additionalProperties false
                       :properties {"commit" {:type "boolean" :const true}}
                       :required ["commit"]}
        preview-schema {:type "object"
                        :additionalProperties false
                        :properties {"expression" {:type "string"
                                                   :minLength 1}}
                        :required ["expression"]}]
    (is (= {:ok true} (admission/authorize commit-schema {"commit" true})))
    (doseq [invalid [false nil "false" 1]]
      (is (= :public-schema-denied
             (:error-type (admission/authorize commit-schema
                                               {"commit" invalid})))))
    (is (= :public-schema-denied
           (:error-type (admission/authorize preview-schema
                                             {"expression" "identity"
                                              "commit" true}))))
    (is (= :public-schema-denied
           (:error-type (admission/authorize preview-schema
                                             {"expression" ""}))))))

(deftest sdk-java-json-values-obey-the-same-authority
  (let [request (java-json-value
                  {:workspace_root "/tmp/work"
                   :extraction {:file "src/a.clj"
                                :forms ["alpha" "beta"]}})]
    (is (= {:ok true} (admission/authorize extraction-schema request)))
    (.put ^java.util.Map request "surprise" true)
    (is (= ["surprise"]
           (:unexpected-fields
             (admission/authorize extraction-schema request))))))

(deftest map-cardinality-and-typed-additional-properties-are-authority
  (let [schema {:type "object"
                :default {}
                :minProperties 1
                :maxProperties 2
                :additionalProperties {:type "string"
                                       :pattern "^[0-9a-f]{4}$"}}]
    (is (= {:ok true} (admission/authorize schema {"a" "1a2b"})))
    (doseq [invalid [{}
                     {"a" "not-a-hash"}
                     {"a" "1a2b" "b" "2b3c" "c" "3c4d"}]]
      (is (= :public-schema-denied
             (:error-type (admission/authorize schema invalid)))))))

(deftest positional-array-items-are-authority
  (let [schema {:type "array"
                :minItems 2
                :maxItems 2
                :prefixItems [{:const "owner"}
                              {:type "integer" :minimum 1}]}]
    (is (admission/valid? schema ["owner" 2]))
    (doseq [invalid [["wrong" 2] ["owner" 0] ["owner"]]]
      (is (false? (admission/valid? schema invalid))))))

(deftest unknown-schema-authority-fails-closed
  (let [result (admission/authorize {:type "object"
                                     :if {:required ["commit"]}}
                                    {})]
    (is (= :public-schema-denied (:error-type result)))
    (is (= [:if] (:unsupported-schema-keywords result)))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (run-tests 'clj-surgeon.experiments.mcp-candidate-admission-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
