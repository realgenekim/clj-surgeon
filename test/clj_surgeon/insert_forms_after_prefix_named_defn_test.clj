(ns clj-surgeon.insert-forms-after-prefix-named-defn-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clojure.test :refer [deftest]]))

;; @spec INSERT-FORMS-001
;; INTENT-TEST: INSERT-FORMS-001
(deftest insert-forms-after-prefix-named-defn

  (let [f (h/fixtures) s (str (:defn-text f) "\n(def untouched :keep)\n")
        req (-> (h/request s) (assoc-in [:anchor :owner :name] (:defn-name f))
                (assoc :payload {:text "(defn witnessed [] :ok)" :forms 1}))]
    (h/accepted s req (str (:defn-text f) "\n(defn witnessed [] :ok)\n(def untouched :keep)\n")))
  (let [s "^:private\n    (defn a [] 1)\n"]
    (h/accepted s (assoc-in (h/request s) [:anchor :position] "before")
                "(defn b [] 3)\n^:private\n    (defn a [] 1)\n"))
  (h/accepted h/source (h/request h/source)
              "(defn a [] 1)\n(defn b [] 3)\n(defn ab [] 2)\n")
  (h/accepted h/source (assoc-in (h/request h/source) [:anchor :position] "before")
              "(defn b [] 3)\n(defn a [] 1)\n(defn ab [] 2)\n"))
