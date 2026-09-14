(ns clj-surgeon.test-registration-battery-test
  "REAL-GATE-001: cold, copied-repository registration masks."
  {:lane :battery}
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is]]))

;; INTENT-TEST: REGNS-006
;; @spec REGNS-006
;; INTENT-TEST: REGNS-012
;; @spec REGNS-012
(deftest registration-first-contact-gate-matrix
  (let [root (.getCanonicalPath (io/file "."))
        scratch (System/getProperty "java.io.tmpdir")
        unit (shell/sh "python3" "-B" (str (io/file root "test/regns_gate_matrix_test.py"))
               :dir scratch)]
    (is (zero? (:exit unit)) (str (:out unit) (:err unit)))
    (when (zero? (:exit unit))
      ;; Deliberately start outside the source checkout: root is request data.
      (let [result (shell/sh "python3" "-B" (str (io/file root "test/regns_gate_matrix.py"))
                     "--root" root "--scratch" scratch :dir scratch)]
        (println (:out result))
        (is (zero? (:exit result)) (str (:out result) (:err result)))))))
