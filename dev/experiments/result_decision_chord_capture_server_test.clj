(ns result-decision-chord-capture-server-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [result-decision-chord-capture-server :as server]))

(deftest treatment-changes-only-visible-success-content
  (let [calls (atom [])
        control (promise)
        treatment (promise)
        tool {:id :inspect-clojure :name "inspect_clojure"}
        capture (str (System/getProperty "java.io.tmpdir")
                     "/result-decision-chord-handler-test.json")]
    ((server/inspect-handler :control calls capture tool)
     nil {:request "same"} #(deliver control [%1 %2 %3]))
    (reset! calls [])
    ((server/inspect-handler :treatment calls capture tool)
     nil {:request "same"} #(deliver treatment [%1 %2 %3]))
    (let [[control-content control-error control-structured] @control
          [treatment-content treatment-error treatment-structured] @treatment]
      (is (= server/frozen-inspect-content control-content))
      (is (= (str (first server/frozen-inspect-content)
                  "\n\n" server/decision-chord)
             (first treatment-content)))
      (is (= control-error treatment-error false))
      (is (= control-structured treatment-structured
             server/frozen-inspect-result)))))

(deftest frozen-result-is-one-real-product-inspect-projection
  (is (true? (:ok server/frozen-inspect-result)))
  (is (true? (:read_complete server/frozen-inspect-result)))
  (is (= 1 (:request_count server/frozen-inspect-result)))
  (is (= 1 (:file_count server/frozen-inspect-result)))
  (is (= (str "(defn greet [name]\n"
              "  (str \"Hello, \" name))")
         (get-in server/frozen-inspect-result [:results 0 :forms 0 :source]))))

(deftest treatment-language-exists-only-in-the-success-result
  (is (not (clojure.string/includes?
             clj-surgeon.mcp-server/server-instructions
             server/decision-chord)))
  (doseq [tool (clj-surgeon.mcp-tool/tools-for-profile :full)]
    (is (not (clojure.string/includes?
               (str (:description tool))
               server/decision-chord))))
  (is (= server/source-before
         (slurp "dev/experiments/fixtures/result_decision_chord/before/src/sample/core.clj"))))

(deftest decision-chord-is-idempotent
  (is (= [(str "summary\n\n" server/decision-chord)]
         (server/append-decision-chord
           [(str "summary\n\n" server/decision-chord)]))))

(deftest repeated-inspection-never-receives-source-or-the-chord
  (let [calls (atom [])
        first-result (promise)
        repeated-result (promise)
        tool {:id :inspect-clojure :name "inspect_clojure"}
        capture (str (System/getProperty "java.io.tmpdir")
                     "/result-decision-chord-refusal-test.json")]
    ((server/inspect-handler :treatment calls capture tool)
     nil server/inspect-request #(deliver first-result [%1 %2 %3]))
    ((server/inspect-handler :treatment calls capture tool)
     nil server/inspect-request #(deliver repeated-result [%1 %2 %3]))
    (is (= 2 (count @calls)))
    (is (false? (second @repeated-result)))
    (is (not-any? #(and (string? %)
                        (clojure.string/includes? % server/decision-chord))
                  (first @repeated-result)))
    (is (nil? (get-in @repeated-result [2 :results])))))
