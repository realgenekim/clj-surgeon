(ns clj-surgeon.test-registration-battery-test
  "REAL-GATE-001: cold, copied-repository registration masks."
  {:lane :battery}
  (:require
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is]]))

;; INTENT-TEST: REGNS-006
;; @spec REGNS-006
(deftest registration-first-contact-gate-matrix
  (let [result (shell/sh "python3" "test/regns_gate_matrix.py"
                 "--scratch" (System/getProperty "java.io.tmpdir"))]
    (println (:out result))
    (is (zero? (:exit result)) (str (:out result) (:err result)))))
