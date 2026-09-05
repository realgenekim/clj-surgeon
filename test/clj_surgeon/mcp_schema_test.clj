(ns ^{:lane :fast} clj-surgeon.mcp-schema-test
  (:require
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clj-surgeon.mcp-schema :as schema]
   [clojure.test :refer [deftest is testing]]))

(deftest direct-change-contract-is-projected-from-the-published-schema
  ;; @spec MCP-OP-MATCHED-003
  (is (= {:request
          {:allowed #{"changes" "expect" "verify" "expect_matched"}
           :required #{"changes"}}
          :change
          {:allowed #{"id" "files" "forms" "owner" "find" "inside"
                      "replace" "delete" "insert_before" "insert_after"
                      "rename_binding" "assoc_entry" "expect"}
           :required #{"id" "files" "expect"}}
          :owner
          {:allowed #{"kind" "name"}
           :required #{"kind"}}
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
           :required #{"key" "value"}}
          :expect-matched
          {:allowed #{"file" "file_hash" "match" "count"}
           :required #{"file" "file_hash" "match" "count"}}}
         schema/direct-change-contract))
  (is (re-find #"Never combine edits and changes"
               (get-in schema/explicit-change-schema
                       [:properties "changes" :description]))))

(deftest contract-projection-cannot-ignore-a-published-field
  (testing "a schema edit changes the validator-facing projection immediately"
    (let [changed (assoc-in schema/explicit-change-schema
                            [:properties "changes" :items :properties "trace"]
                            {:type "string"})]
      (is (contains? (get-in (schema/direct-contract-shape changed)
                             [:change :allowed])
                     "trace")))))

(deftest public-schema-exposes-one-mutually-exclusive-extraction-transaction
  (is (= #{"file" "to" "forms" "require_policy"}
         (set (get-in schema/clj-change-schema
                      [:properties "extraction" :required]))))
  (is (= ["minimal" "copy-all"]
         (get-in schema/clj-change-schema
                 [:properties "extraction" :properties "require_policy" :enum])))
  (is (= "^[0-9a-f]{64}$"
         (get-in schema/clj-change-schema
                 [:properties "extraction" :properties "source_hash" :pattern])))
  (is (= true
         (get-in schema/clj-change-schema
                 [:properties "extraction" :properties "public_forms"
                  :uniqueItems])))
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
                   {:required ["delete_owners"]}
                   {:required ["symbol_migration" "require_change"]}]
                  (:anyOf %))
              routes))
    (is (= #{"file" "files" "within" "from" "to"
             "old" "new" "before" "after" "matches"}
           (set (keys (:properties gesture)))))
    (is (nil? (:required gesture)))
    (is (= [["from" "to"] ["old" "new"] ["before" "after"]]
           (->> (:allOf gesture)
                (mapcat :oneOf)
                (keep :required)
                (filter #(= 2 (count %)))
                vec)))
    (is (false? (:additionalProperties gesture)))
    (is (= #{"form" "namespace" "root"}
           (set (keys (get-in gesture [:properties "within" :properties])))))
    (is (= [{:required ["form"]}
            {:required ["namespace"]}
            {:required ["root"]}]
           (get-in gesture [:properties "within" :oneOf])))
    (is (= [{:type "string" :minLength 1}
            {:type "boolean" :enum [true]}]
           (get-in gesture
                   [:properties "within" :properties "namespace" :oneOf])))
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

;; ---------------------------------------------------------------------------
;; `expect_matched` is a property of the direct changes transaction only. The
;; published schema merged every branch's properties, so it advertised the field
;; on the edits and extraction branches that refuse it at validation.
;; ---------------------------------------------------------------------------

(def ^:private matched-basis
  {"file" "src/a.clj"
   "file_hash" (apply str (repeat 64 "a"))
   "match" "(f _)"
   "count" 2})

(def ^:private one-change
  {"id" "c1"
   "files" ["src/a.clj"]
   "forms" ["f"]
   "find" ":old"
   "replace" ":new"
   "expect" {"matches" 1}})

(def ^:private one-edit
  {"file" "src/a.clj"
   "within" {"form" "f"}
   "from" ":old"
   "to" ":new"})

(def ^:private one-extraction
  {"file" "src/a.clj"
   "to" "src/b.clj"
   "forms" ["f"]
   "require_policy" "minimal"})

(deftest published-schema-advertises-expect-matched-only-where-it-is-accepted
  ;; @spec MCP-OP-MATCHED-005
  (testing "the direct changes branch accepts the prior-match basis"
    (is (:ok (admission/authorize
               schema/clj-change-schema
               {"changes" [one-change]
                "expect" {"changes" 1 "edits" 1 "files" 1}
                "expect_matched" matched-basis}))))
  (testing "the editor gesture branch does not"
    (is (:ok (admission/authorize schema/clj-change-schema
                                  {"edits" [one-edit]})))
    (is (false? (:ok (admission/authorize
                       schema/clj-change-schema
                       {"edits" [one-edit]
                        "expect_matched" matched-basis})))))
  (testing "the extraction branch does not"
    (is (:ok (admission/authorize schema/clj-change-schema
                                  {"extraction" one-extraction})))
    (is (false? (:ok (admission/authorize
                       schema/clj-change-schema
                       {"extraction" one-extraction
                        "expect_matched" matched-basis}))))))

(deftest edit-clojure-schema-omits-the-direct-transaction-fields
  ;; @spec MCP-OP-MATCHED-005
  (testing "edit_clojure advertises neither changes nor the prior-match basis"
    (is (not (contains? (:properties schema/editor-tool-schema) "changes")))
    (is (not (contains? (:properties schema/editor-tool-schema)
                        "expect_matched"))))
  (testing "and its published schema refuses them"
    (is (false? (:ok (admission/authorize
                       schema/editor-tool-schema
                       {"edits" [one-edit] "changes" [one-change]}))))
    (is (false? (:ok (admission/authorize
                       schema/editor-tool-schema
                       {"edits" [one-edit]
                        "expect_matched" matched-basis}))))))
