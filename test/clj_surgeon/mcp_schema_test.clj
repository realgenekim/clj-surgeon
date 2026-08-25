(ns clj-surgeon.mcp-schema-test
  (:require
   [clj-surgeon.mcp-schema :as schema]
   [clojure.test :refer [deftest is testing]]))

(deftest direct-change-contract-is-projected-from-the-published-schema
  (is (= {:request
          {:allowed #{"changes" "expect" "verify"}
           :required #{"changes" "expect"}}
          :change
          {:allowed #{"id" "files" "forms" "owner" "find" "inside"
                      "replace" "delete" "insert_before" "insert_after"
                      "rename_binding" "assoc_entry" "expect"}
           :required #{"id" "files" "expect"}}
          :owner
          {:allowed #{"kind" "name"}
           :required #{"kind" "name"}}
          :form-owner
          {:allowed #{"kind" "name" "dispatch"}
           :required #{"kind" "name" "dispatch"}}
          :expect
          {:allowed #{"matches" "each_form" "each_file"}
           :required #{"matches"}}
          :aggregate-expect
          {:allowed #{"changes" "edits" "files"}
           :required #{"changes" "edits" "files"}}
          :rename-binding
          {:allowed #{"from" "to" "preserve_external_key"}
           :required #{"from" "to" "preserve_external_key"}}
          :assoc-entry
          {:allowed #{"key" "value"}
           :required #{"key" "value"}}}
         schema/direct-change-contract)))

(deftest contract-projection-cannot-ignore-a-published-field
  (testing "a schema edit changes the validator-facing projection immediately"
    (let [changed (assoc-in schema/explicit-change-schema
                            [:properties "changes" :items :properties "trace"]
                            {:type "string"})]
      (is (contains? (get-in (schema/direct-contract-shape changed)
                             [:change :allowed])
                     "trace")))))

(deftest public-schema-exposes-one-mutually-exclusive-extraction-transaction
  (is (= #{"file" "to" "forms" "require_policy" "caller_changes"
           "ignored_caller_files" "expect"}
         (set (get-in schema/clj-change-schema
                      [:properties "extraction" :required]))))
  (is (= ["minimal" "copy-all"]
         (get-in schema/clj-change-schema
                 [:properties "extraction" :properties "require_policy" :enum])))
  (is (= 0
         (get-in schema/clj-change-schema
                 [:properties "extraction" :properties "expect" :properties
                  "caller_edits" :minimum]))))

(deftest public-schema-exposes-one-mutually-exclusive-editor-gesture
  (let [routes (:oneOf schema/clj-change-schema)
        gesture (get-in schema/clj-change-schema
                        [:properties "edits" :items])
        deletion (get-in schema/editor-tool-schema
                         [:properties "delete_owners" :items])
        program (get-in schema/editor-tool-schema
                        [:properties "programs" :items])]
    (is (some #(= [{:required ["edits"]}
                   {:required ["programs"]}
                   {:required ["delete_owners"]}]
                  (:anyOf %))
              routes))
    (is (= #{"file" "within" "from" "to" "matches"}
           (set (keys (:properties gesture)))))
    (is (= ["file" "within" "from" "to"]
           (:required gesture)))
    (is (false? (:additionalProperties gesture)))
    (is (= ["form"]
           (get-in gesture [:properties "within" :required])))
    (is (= #{"file" "expression" "expect"}
           (set (keys (:properties program)))))
    (is (= ["file" "expression" "expect"] (:required program)))
    (is (= #{"file" "forms"}
           (set (keys (:properties deletion)))))
    (is (= ["file" "forms"] (:required deletion)))
    (is (= 128 (get-in deletion [:properties "forms" :maxItems])))
    (is (= ["matches" "max_changed_characters"]
           (get-in program [:properties "expect" :required])))
    (is (= 16 (get-in schema/editor-tool-schema
                      [:properties "programs" :maxItems])))))
