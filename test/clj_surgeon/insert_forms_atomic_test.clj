(ns clj-surgeon.insert-forms-atomic-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-014
;; INTENT-TEST: INSERT-FORMS-014
(deftest insert-forms-atomic-multiform-and-race

  (doseq [[stage state kind unchanged]
          [[:stage "failed" :io-error true]
           [:before-recheck "refused" :source-changed-before-commit false]
           [:read-back "rolled-back" :io-error true]
           [:external-after-write "recovery-required" :commit-outcome-unknown false]]]
    (h/with-file h/source
      (fn [_ file req]
        (let [req (assoc req :payload {:text "(def b 2)\n(def c 3)" :forms 2})
              external (str h/source "; external\n")
              hook (fn [& _]
                     (when (#{:before-recheck :external-after-write} stage) (spit file external))
                     (when-not (= :before-recheck stage) (throw (java.io.IOException. "injected"))))
              result (insert/execute! req {stage hook})]
          (is (= state (:state result)) (pr-str result))
          (is (= kind (:error-type result)))
          (is (= (if unchanged h/source external) (slurp file))))))))
