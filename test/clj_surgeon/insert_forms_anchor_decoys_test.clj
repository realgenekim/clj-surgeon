(ns clj-surgeon.insert-forms-anchor-decoys-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clojure.test :refer [deftest]]))

;; @spec INSERT-FORMS-005
;; INTENT-TEST: INSERT-FORMS-005
(deftest insert-forms-comment-string-anchor-decoys

  (doseq [decoy ["; (defn a [] 1)\n" "\"(defn a [] 1)\"\n"
                 "(comment (defn a [] 1))\n" "'(defn a [] 1)\n" "#_(defn a [] 1)\n"]]
    (h/refused decoy (h/request decoy) :anchor-not-found)
    (let [s (str decoy h/source)]
      (h/accepted s (h/request s)
                  (str decoy "(defn a [] 1)\n(defn b [] 3)\n\n(defn ab [] 2)\n")))))
