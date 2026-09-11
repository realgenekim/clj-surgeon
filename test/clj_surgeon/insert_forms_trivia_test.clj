(ns clj-surgeon.insert-forms-trivia-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clojure.test :refer [deftest]]))

;; @spec INSERT-FORMS-010
;; INTENT-TEST: INSERT-FORMS-010
(deftest insert-forms-trivia-and-indentation

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
    (h/accepted s (assoc (h/request s) :payload {:text text :forms 1}) expected))
  (h/refused h/source (assoc (h/request h/source) :payload {:text "\t42" :forms 1})
             :unsupported-indentation))
