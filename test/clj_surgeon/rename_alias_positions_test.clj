(ns clj-surgeon.rename-alias-positions-test
  {:lane :fast}
  (:require
   [clj-surgeon.rename-alias-plan :as p]
   [clj-surgeon.rename-alias-test :as r]
   [clojure.test :refer [deftest is]]))

;; @spec RENAME-ALIAS-014
;; INTENT-TEST: RENAME-ALIAS-014
(deftest column-one-reference-addresses
  ;; Line 1 exercises the traversal directly: an executable file must start with ns.
  (doseq [[source line] [["events/x" 1] [(str r/header "events/x\n") 2]]]
    (let [root (p/source! source r/file)
          site (first (p/references root nil "events" r/file))]
      (is (= line (:line site)))
      (is (= line (:end_line site)))
      (is (integer? (get-in site [:address :preorder])))))
  (let [source (str r/header "events/x\n")
        result (p/plan {r/file source} (r/request {r/file source} 0))
        site (first (get-in result [:write_refusal_evidence :items]))]
    (is (= :expect-count-mismatch (:error-type result)))
    (is (= 2 (:line site)))
    (is (integer? (get-in site [:address :preorder])))))
