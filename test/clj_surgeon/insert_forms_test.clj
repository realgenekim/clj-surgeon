(ns clj-surgeon.insert-forms-test
  {:lane :fast}
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.insert-forms]
            [clj-surgeon.insert-forms-oracle]
            [clj-surgeon.insert-forms-plan]
            [clj-surgeon.insert-forms-support]
            [clojure.string]
))



;; @spec INSERT-FORMS-022
;; INTENT-TEST: INSERT-FORMS-022
;; @spec INSERT-FORMS-001
;; INTENT-TEST: INSERT-FORMS-001


;; @spec INSERT-FORMS-005
;; INTENT-TEST: INSERT-FORMS-005


;; @spec INSERT-FORMS-006
;; INTENT-TEST: INSERT-FORMS-006


;; @spec INSERT-FORMS-023
;; INTENT-TEST: INSERT-FORMS-023

(deftest insert-forms-exact-root-anchor
  (testing "E4 exact reconstruction and four refusal stages"
    (let [expected (slurp "test-fixtures/clj-splice/rename-f3-B.clj")
          a (.indexOf expected "(defn- normalized-speaker-name")
          b (.indexOf expected ";; INTENT: AGENDA-PUBLIC-003")
          payload (clojure.string/trimr (subs expected a b))
          source (str (subs expected 0 a) (subs expected b))
          request {:version 1 :workspace_root "/fixture" :file "src/schedule.clj"
                   :guard {:sha256 (clj-surgeon.insert-forms-support/sha source)}
                   :anchor {:scope "top-level" :owner {:kind "ns" :name "cfp-scheduler-killer.views.schedule"}
                            :expect 1 :position "after"}
                   :payload {:text payload :forms 1}}
          result (clj-surgeon.insert-forms/plan source request)]
      (is (= [true expected "02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c"]
             [(:ok result) (:candidate result) (some-> (:candidate result) clj-surgeon.insert-forms-support/sha)]))
      (doseq [[req error] [[(assoc-in request [:guard :sha256] (apply str (repeat 64 "0"))) :source-hash-mismatch]
                           [(assoc-in request [:anchor :owner :name] "absent") :anchor-not-found]
                           [(assoc-in request [:payload :forms] 2) :payload-form-count-mismatch]
                           [(assoc-in request [:payload :text] "(") :payload-parse-error]]]
        (clj-surgeon.insert-forms-support/refused source req error))))
  (testing "insert-forms-after-prefix-named-defn"

  (let [f (clj-surgeon.insert-forms-support/fixtures) s (str (:defn-text f) "\n(def untouched :keep)\n")
        req (-> (clj-surgeon.insert-forms-support/request s) (assoc-in [:anchor :owner :name] (:defn-name f))
                (assoc :payload {:text "(defn witnessed [] :ok)" :forms 1}))]
    (clj-surgeon.insert-forms-support/accepted s req (str (:defn-text f) "\n(defn witnessed [] :ok)\n(def untouched :keep)\n")))
  (let [s "^:private\n    (defn a [] 1)\n"]
    (clj-surgeon.insert-forms-support/accepted s (assoc-in (clj-surgeon.insert-forms-support/request s) [:anchor :position] "before")
                "(defn b [] 3)\n^:private\n    (defn a [] 1)\n"))
  (clj-surgeon.insert-forms-support/accepted clj-surgeon.insert-forms-support/source (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source)
              "(defn a [] 1)\n(defn b [] 3)\n(defn ab [] 2)\n")
  (clj-surgeon.insert-forms-support/accepted clj-surgeon.insert-forms-support/source (assoc-in (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) [:anchor :position] "before")
              "(defn b [] 3)\n(defn a [] 1)\n(defn ab [] 2)\n"))
  (testing "insert-forms-comment-string-anchor-decoys"

  (doseq [decoy ["; (defn a [] 1)\n" "\"(defn a [] 1)\"\n"
                 "(comment (defn a [] 1))\n" "'(defn a [] 1)\n" "#_(defn a [] 1)\n"]]
    (clj-surgeon.insert-forms-support/refused decoy (clj-surgeon.insert-forms-support/request decoy) :anchor-not-found)
    (let [s (str decoy clj-surgeon.insert-forms-support/source)]
      (clj-surgeon.insert-forms-support/accepted s (clj-surgeon.insert-forms-support/request s)
                  (str decoy "(defn a [] 1)\n(defn b [] 3)\n(defn ab [] 2)\n")))))
  (testing "insert-forms-anchor-cardinality"

  (doseq [[s error n] [["(defn ab [] 2)" :anchor-not-found 0]
                       ["(defn a [] 1)\n(defn a [] 2)" :anchor-multiple-matches 2]]]
    (let [r (clj-surgeon.insert-forms-support/refused s (clj-surgeon.insert-forms-support/request s) error)]
      (is (= 1 (:expected r))) (is (= n (:actual r)))))
  (let [s "(deftest t (testing \"x\" 1) (testing \"x\" 2))"]
    (clj-surgeon.insert-forms-support/refused s (assoc (clj-surgeon.insert-forms-support/request s) :anchor
                   (assoc (clj-surgeon.insert-forms-support/body "deftest" "t" {:position "first"})
                          :testing_path [{:label "x" :expect 1}]))
               :anchor-multiple-matches)))
  (testing "insert-forms-candidate-envelope"
  (doseq [[source anchor error expected]
          [["(defn a [] 1)\n(defn a [] 2)\n" nil :anchor-multiple-matches
            [{:kind "defn" :name "a" :line 1} {:kind "defn" :name "a" :line 2}]]
           ["(defn a\n  ([] 1)\n  ([x] x))" (clj-surgeon.insert-forms-support/body "defn" "a" {:position "last"})
            :anchor-ambiguous [{:kind "arity" :name "[]" :line 2}
                               {:kind "arity" :name "[x]" :line 3}]]]]
    (clj-surgeon.insert-forms-support/with-file source
      (fn [_ file req]
        (let [result (clj-surgeon.insert-forms/execute! (cond-> req anchor (assoc :anchor anchor)))]
          (is (= error (:error-type result)))
          (is (= source (slurp file)))
          (is (= expected (mapv #(select-keys % [:kind :name :line]) (:candidates result))))
          (is (and (false? (:candidates_truncated result))
                   (= "revise-request" (:next_action result))
                   (seq (:remedy result)))))))))
)



;; @spec INSERT-FORMS-002
;; INTENT-TEST: INSERT-FORMS-002


;; @spec INSERT-FORMS-007
;; INTENT-TEST: INSERT-FORMS-007


;; @spec INSERT-FORMS-008
;; INTENT-TEST: INSERT-FORMS-008

(deftest insert-forms-body-anchor
  (testing "insert-forms-deftest-nested-testing"

  (let [f (clj-surgeon.insert-forms-support/fixtures) s (:test-text f)
        req (assoc (clj-surgeon.insert-forms-support/request s)
                   :anchor (assoc (clj-surgeon.insert-forms-support/body "deftest" "admitted" {:position "last"})
                                  :testing_path [{:label (:outer-label f) :expect 1}
                                                 {:label (:inner-label f) :expect 1}])
                   :payload {:text "(is (= 1 1))" :forms 1})]
    (clj-surgeon.insert-forms-support/accepted s req (str (subs s 0 (- (count s) 3)) "\n      (is (= 1 1)))))")))
  (let [s "(deftest t\n  (testing \"outer\"\n    (testing \"inner\"\n      (is true)))\n  (testing \"other\" (testing \"inner\" (is false))))"
        a (assoc (clj-surgeon.insert-forms-support/body "deftest" "t" {:position "last"})
                 :testing_path [{:label "outer" :expect 1} {:label "inner" :expect 1}])
        r (assoc (clj-surgeon.insert-forms-support/request s) :anchor a :payload {:text "(is (= 1 1))" :forms 1})]
    (clj-surgeon.insert-forms-support/accepted s r "(deftest t\n  (testing \"outer\"\n    (testing \"inner\"\n      (is true)\n      (is (= 1 1))))\n  (testing \"other\" (testing \"inner\" (is false))))")))
  (testing "insert-forms-body-boundaries"

  (doseq [[s boundary expected]
          [["(deftest t\n  1\n  2)" {:position "first"} "(deftest t\n  3\n  1\n  2)"]
           ["(deftest t\n  1\n  2)" {:position "last"} "(deftest t\n  1\n  2\n  3)"]
           ["(deftest t\n  1\n  2)" {:position "after-child" :child 1} "(deftest t\n  1\n  3\n  2)"]
           ["(deftest t)" {:position "first"} "(deftest t\n  3)"]
           ["(deftest t)" {:position "last"} "(deftest t\n  3)"]]]
    (clj-surgeon.insert-forms-support/accepted s (assoc (clj-surgeon.insert-forms-support/request s) :anchor (clj-surgeon.insert-forms-support/body "deftest" "t" boundary)
                    :payload {:text "3" :forms 1}) expected))
  (doseq [[boundary error] [[{:position "after-child" :child 0} :invalid-request]
                            [{:position "after-child" :child 3} :anchor-index-out-of-range]
                            [{:position "first" :child 1} :invalid-request]]]
    (let [s "(deftest t 1 (testing \"x\" 2 3))"]
      (clj-surgeon.insert-forms-support/refused s (assoc (clj-surgeon.insert-forms-support/request s) :anchor (clj-surgeon.insert-forms-support/body "deftest" "t" boundary)) error))))
  (testing "insert-forms-defn-headers-and-arities"

  (let [s "(defn \"a\" [] 1)"]
    (clj-surgeon.insert-forms-support/refused s (assoc-in (clj-surgeon.insert-forms-support/request s) [:anchor :owner :name] "\"a\"") :invalid-request))
  (doseq [s ["(defn ^:private a \"doc\" {:a 1} [x] {:pre [x]}\n  x)"
             "(clojure.core/defn a [x]\n  x)"]]
    (let [r (assoc (clj-surgeon.insert-forms-support/request s) :anchor (clj-surgeon.insert-forms-support/body "defn" "a" {:position "last"})
                   :payload {:text "42" :forms 1})]
      (clj-surgeon.insert-forms-support/accepted s r (str (subs s 0 (dec (count s))) "\n  42)"))))
  (let [s "(defn a ([] 0) ([x]\n  x) {:attr true})"
        r (assoc (clj-surgeon.insert-forms-support/request s) :anchor (clj-surgeon.insert-forms-support/body "defn" "a" {:position "last"})
                 :payload {:text "42" :forms 1})]
    (clj-surgeon.insert-forms-support/refused s r :anchor-ambiguous)
    (clj-surgeon.insert-forms-support/accepted s (assoc-in r [:anchor :arity] 2)
                "(defn a ([] 0) ([x]\n  x\n  42) {:attr true})")
    (clj-surgeon.insert-forms-support/refused s (assoc-in r [:anchor :arity] 3) :anchor-index-out-of-range))
  (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :anchor
                        (assoc (clj-surgeon.insert-forms-support/body "defn" "a" {:position "last"}) :arity 1))
             :invalid-request)
  (let [s "(defn a :bad)"]
    (clj-surgeon.insert-forms-support/refused s (assoc (clj-surgeon.insert-forms-support/request s) :anchor (clj-surgeon.insert-forms-support/body "defn" "a" {:position "last"}))
               :unsupported-owner-shape)))
)



;; @spec INSERT-FORMS-009
;; INTENT-TEST: INSERT-FORMS-009

(deftest insert-forms-payload-count-and-order
  (testing "insert-forms-payload-count-and-order"

  (doseq [text ["" "; only comment\n" "1 2"]]
    (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text text :forms 1})
               :payload-form-count-mismatch))
  (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text "#_1 2" :forms 1})
             :unsupported-payload-syntax)
  (clj-surgeon.insert-forms-support/accepted clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text "'x #foo/bar 2" :forms 2})
              "(defn a [] 1)\n'x #foo/bar 2\n(defn ab [] 2)\n"))
)



;; @spec INSERT-FORMS-010
;; INTENT-TEST: INSERT-FORMS-010


;; @spec INSERT-FORMS-021
;; INTENT-TEST: INSERT-FORMS-021

(deftest insert-forms-layout-literal-preservation
  (testing "insert-forms-trivia-and-indentation"

  (doseq [[s text expected]
          [["(defn a [] 1) ; trailing\n; standalone\n(def b 2)" "; payload\n(def c 3)"
            "(defn a [] 1) ; trailing\n; payload\n(def c 3)\n; standalone\n(def b 2)"]
           ["(defn a [] 1)" "(def b 2)" "(defn a [] 1)\n(def b 2)\n"]
           ["(defn a [] 1)\r\n" "(def b\n  2)" "(defn a [] 1)\r\n(def b\r\n  2)\r\n"]
           ["(defn a [] 1)" "  \"é\n  literal\"" "(defn a [] 1)\n\"é\n  literal\"\n"]
           ["(defn a [] 1)\r\n(def z 2)\n" "(def b 2)"
            "(defn a [] 1)\r\n(def b 2)\r\n(def z 2)\n"]
           ["; first\r\n(defn a [] 1) ; tail\n(def z 2)\r\n" "(def b 2)"
            "; first\r\n(defn a [] 1) ; tail\n(def b 2)\r\n(def z 2)\r\n"]
           ["(defn a [] 1)\n(def z \"a\r\nb\")\r\n" "\"c\r\nd\""
            "(defn a [] 1)\n\"c\r\nd\"\n(def z \"a\r\nb\")\r\n"]]]
    (clj-surgeon.insert-forms-support/accepted s (assoc (clj-surgeon.insert-forms-support/request s) :payload {:text text :forms 1}) expected))
  (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text "\t42" :forms 1})
             :unsupported-indentation))
  (testing "insert-forms-existing-separators"
  (doseq [[source expected]
          [["(defn a [] 1)\n\n(def z 2)\n"
            "(defn a [] 1)\n\n(defn b [] 3)\n\n(def z 2)\n"]
           ["(defn a [] 1) ; tail\n\n(def z 2)\n"
            "(defn a [] 1) ; tail\n\n(defn b [] 3)\n\n(def z 2)\n"]]]
    (clj-surgeon.insert-forms-support/accepted source (clj-surgeon.insert-forms-support/request source) expected))
  (doseq [[source boundary payload expected]
          [["(deftest t\n  1\n\n  2)" {:position "after-child" :child 1} "3"
            "(deftest t\n  1\n\n  3\n\n  2)"]
           ["(deftest t\n  1\n\n  2)" {:position "last"} "3"
            "(deftest t\n  1\n\n  2\n\n  3)"]
           ["(defn a [])" {:position "first"} "1" "(defn a []\n  1)"]
           ["(deftest t\n)" {:position "last"} "3" "(deftest t\n  3)"]
           ["(deftest t)" {:position "last"} "3\n" "(deftest t\n  3\n  )"]
           ["(deftest t)" {:position "last"} "3 ; tail" "(deftest t\n  3 ; tail\n  )"]
           ["(deftest t 1 2)" {:position "first"} "3"
            "(deftest t\n           3\n           1 2)"]
           ["(deftest t\n  1\n  2)" {:position "first"} "3\n"
            "(deftest t\n  3\n  1\n  2)"]]]
    (clj-surgeon.insert-forms-support/accepted source
                (assoc (clj-surgeon.insert-forms-support/request source)
                       :anchor (if (= source "(defn a [])")
                                 (clj-surgeon.insert-forms-support/body "defn" "a" boundary)
                                 (clj-surgeon.insert-forms-support/body "deftest" "t" boundary))
                       :payload {:text payload :forms 1}) expected)))
)



;; @spec INSERT-FORMS-003
;; INTENT-TEST: INSERT-FORMS-003


;; @spec INSERT-FORMS-020
;; INTENT-TEST: INSERT-FORMS-020

(deftest insert-forms-parse-stages
  (testing "insert-forms-unbalanced-payload-refuses"

  (doseq [text [")" "(defn b []" "(def b 1) ]"]]
    (clj-surgeon.insert-forms-support/refused clj-surgeon.insert-forms-support/source (assoc (clj-surgeon.insert-forms-support/request clj-surgeon.insert-forms-support/source) :payload {:text text :forms 1})
               :payload-parse-error)))
  (testing "insert-forms-parse-errors-refuse"
  (doseq [[source perturb error]
          [["(defn a [] 1" identity :source-parse-error]
           [clj-surgeon.insert-forms-support/source (constantly "(") :candidate-parse-error]]]
    (clj-surgeon.insert-forms-support/with-file source
      (fn [_ file req]
        (binding [clj-surgeon.insert-forms-plan/*candidate-text* perturb]
          (let [result (clj-surgeon.insert-forms/execute! req)]
            (is (= error (:error-type result)) (pr-str result))
            (is (= "refused" (:state result)))
            (is (= source (slurp file)))))))))
)



;; @spec INSERT-FORMS-019
;; INTENT-TEST: INSERT-FORMS-019

(deftest insert-forms-candidate-structure-refuses
  (testing "insert-forms-candidate-structure-refuses"
  ;; Opus F1: splice one byte inside the anchor; syntax still parses.
  (clj-surgeon.insert-forms-support/with-file clj-surgeon.insert-forms-support/source
    (fn [_ file req]
      (binding [clj-surgeon.insert-forms-plan/*splice-offset* dec]
        (let [result (clj-surgeon.insert-forms/execute! req)]
          (is (= :candidate-structure-mismatch (:error-type result)))
          (is (= "refused" (:state result)))
          (is (= clj-surgeon.insert-forms-support/source (slurp file))))))))
)



;; @spec INSERT-FORMS-013
;; INTENT-TEST: INSERT-FORMS-013

(deftest insert-forms-other-top-level-hashes
  (testing "insert-forms-other-top-level-hashes"

  (let [s "(defn a [] 1)\n#_(def a :discard)\n42\n42\n; semicolon survives\n(comment :keep)\n"
        r (clj-surgeon.insert-forms/plan s (clj-surgeon.insert-forms-support/request s))]
    (is (= true (:ok r)))
    (is (= 5 (get-in r [:receipt :preservation :other_forms_checked])))
    (is (= true (get-in r [:receipt :preservation :other_forms_unchanged])))
    (when (:ok r)
      (let [b (:candidate r) receipt (:receipt r)]
        (doseq [bad [(clojure.string/replace b "(comment :keep)" "(comment  :keep)")
                     (clojure.string/replace b "#_(def a :discard)\n" "")
                     (clojure.string/replace b "; semicolon survives\n" "")
                     (str "(defn b [] 3)\n" s)]]
          (is (= false (:ok (clj-surgeon.insert-forms-oracle/verify s bad (clj-surgeon.insert-forms-support/request s) (assoc receipt :result_hash (clj-surgeon.insert-forms-support/sha bad)))))))
        (is (= false (:ok (clj-surgeon.insert-forms-oracle/verify s b (clj-surgeon.insert-forms-support/request s) (assoc receipt :result_hash (apply str (repeat 64 "0")))))))))
    (is (= [1 2 3 4 5] (mapv :before_index (get-in r [:detail :preservation_entries])))))
  (let [s "(defn a [] 1)\n(defn z [] 2)\n"
        req (assoc (clj-surgeon.insert-forms-support/request s) :anchor (clj-surgeon.insert-forms-support/body "defn" "a" {:position "last"})
              :payload {:text "42" :forms 1})
        result (clj-surgeon.insert-forms/plan s req)
        wrong "(defn a [] 1)\n(defn z [] 2\n42\n)\n"]
    (is (:ok result))
    (is (false? (:ok (clj-surgeon.insert-forms-oracle/verify s wrong req
                       (assoc (:receipt result) :result_hash (clj-surgeon.insert-forms-support/sha wrong))))))))
)
