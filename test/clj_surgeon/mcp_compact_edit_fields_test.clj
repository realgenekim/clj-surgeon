(ns clj-surgeon.mcp-compact-edit-fields-test
  (:require
   [clj-surgeon.mcp-compact-edit-fields :as compact-edit-fields]
   [clojure.test :refer [deftest is testing]]))

(def ^:private all-value-fields
  ["from" "to" "old" "new" "before" "after"])

(def ^:private accepted-field-sets
  #{#{"from" "to"}
    #{"old" "new"}
    #{"before" "after"}})

(defn- selected-fields
  [mask]
  (->> all-value-fields
       (keep-indexed (fn [index field]
                       (when (bit-test mask index) field)))
       vec))

(defn- edit-with-fields
  [fields]
  (reduce (fn [edit field]
            (assoc edit field (str ":" field)))
          {"file" "src/sample/app.clj"
           "within" {"form" "f"}}
          fields))

(deftest exact-value-pair-algebra-is-injective
  ;; @spec MCP-OP-EDIT-017
  ;; @spec MCP-OP-EDIT-018
  (doseq [mask (range 64)]
    (let [fields (selected-fields mask)
          field-set (set fields)
          result (compact-edit-fields/normalize-edit
                   (edit-with-fields fields) 0)
          accepted? (contains? accepted-field-sets field-set)]
      (testing (str "field subset " fields)
        (is (= accepted? (:ok result)))
        (if accepted?
          (do
            (is (= (str ":" (first fields))
                   (get (:edit result) "from")))
            (is (= (str ":" (second fields))
                   (get (:edit result) "to"))))
          (do
            (is (= :invalid-editor-field-pair (:reason result)))
            (is (= ["edits" 0] (:path result)))
            (is (= (vec (sort fields)) (:supplied-fields result)))
            (is (:source-unchanged result))
            (is (false? (:mutation-attempted result)))
            (is (false? (:write-authority result)))))))))

(deftest one-invalid-sibling-refuses-the-whole-normalization
  ;; @spec MCP-OP-EDIT-018
  (let [result
        (compact-edit-fields/normalize-edits
          [(edit-with-fields ["from" "to"])
           (edit-with-fields ["old" "after"])
           (edit-with-fields ["before" "after"])])]
    (is (false? (:ok result)))
    (is (= :invalid-editor-field-pair (:reason result)))
    (is (= ["edits" 1] (:path result)))
    (is (= ["after" "old"] (:supplied-fields result)))
    (is (not (contains? result :edits)))
    (is (not (contains? result :evidence)))))
