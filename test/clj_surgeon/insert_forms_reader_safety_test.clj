(ns clj-surgeon.insert-forms-reader-safety-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-011
;; INTENT-TEST: INSERT-FORMS-011
(deftest insert-forms-reader-safety-and-limits

  (let [s (str (apply str (repeat 32768 " ")) "(defn a [] 1)")
        req (assoc (h/request s) :payload {:text (apply str (repeat 1000 "1\n")) :forms 1000})
        result (insert/plan s req)]
    (is (= :limit-exceeded (:error-type result)))
    (is (= true (:source_unchanged result))))
  (h/with-file h/source
    (fn [_ file req]
      (let [huge (assoc-in req [:anchor :owner :name] (apply str (repeat 2097153 "a")))
            result (insert/execute! huge)]
        (is (= :limit-exceeded (:error-type result)))
        (is (= h/source (slurp file))))))
  (is (= :invalid-request (:error-type (insert/read-request "{:value #inst \"2026-01-01\"}"))))
  (doseq [s ["#=(throw (Exception.)) (defn a [] 1)" "#?(:clj (defn a [] 1))"
             "\ufeff(defn a [] 1)"]]
    (h/refused s (h/request s) :unsupported-source))
  (doseq [text ["#=(throw (Exception.))" "#?(:clj 1)"]]
    (h/refused h/source (assoc (h/request h/source) :payload {:text text :forms 1})
               :unsupported-payload-syntax))
  (doseq [text [(apply str (repeat 1048577 "x"))
                (str (apply str (repeat 513 "(")) "0" (apply str (repeat 513 ")")))]]
    (h/refused h/source (assoc (h/request h/source) :payload {:text text :forms 1})
               :limit-exceeded))
  (h/refused h/source (assoc-in (h/request h/source) [:payload :forms] 1001) :limit-exceeded)
  (let [s (apply str (repeat 8388609 " "))]
    (h/refused s (h/request s) :limit-exceeded))
  (doseq [text ["{} {}" "{:a 1 :a 2}" "#foo {}" "#=(+ 1 2)"]]
    (is (= :invalid-request (:error-type (insert/read-request text))))))
