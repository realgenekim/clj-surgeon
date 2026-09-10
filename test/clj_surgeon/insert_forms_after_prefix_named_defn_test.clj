(ns clj-surgeon.insert-forms-after-prefix-named-defn-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-001
;; INTENT-TEST: INSERT-FORMS-001
(deftest insert-forms-after-prefix-named-defn

  (let [s "^:private\n    (defn a [] 1)\n"]
    (h/accepted s (assoc-in (h/request s) [:anchor :position] "before")
                "(defn b [] 3)\n^:private\n    (defn a [] 1)\n"))
  (h/accepted h/source (h/request h/source)
              "(defn a [] 1)\n(defn b [] 3)\n\n(defn ab [] 2)\n")
  (h/accepted h/source (assoc-in (h/request h/source) [:anchor :position] "before")
              "(defn b [] 3)\n(defn a [] 1)\n(defn ab [] 2)\n"))
