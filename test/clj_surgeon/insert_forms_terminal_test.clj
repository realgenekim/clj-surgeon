(ns clj-surgeon.insert-forms-terminal-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-017
;; INTENT-TEST: INSERT-FORMS-017
(deftest insert-forms-terminal-response-eligibility

  (let [r (insert/plan h/source (h/request h/source))]
    (is (= true (:ok r)))
    (is (= false (get-in r [:receipt :verification_complete])))
    (is (= {:tier "parse+byte-preservation" :behavior "not-run"} (get-in r [:receipt :verification])))
    (is (not (contains? (:receipt r) :terminal_response))))
  (h/with-file h/source
    (fn [_ _ req]
      (let [r (insert/execute! req)]
        (is (= "committed" (:state r)))
        (is (not (contains? r :terminal_response)))
        (is (= false (:verification_complete r)))))))
