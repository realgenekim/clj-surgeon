(ns clj-surgeon.insert-forms-portable-receipt-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)))

;; @spec INSERT-FORMS-012
;; INTENT-TEST: INSERT-FORMS-012
(deftest insert-forms-portable-read-receipt

  (let [projection {:version 1 :read_complete true :workspace_root "/fixture"
                    :file "src/example.clj" :sha256 (h/sha h/source)}
        req (assoc (h/request h/source) :guard {:read_receipt projection})]
    (h/accepted h/source req "(defn a [] 1)\n(defn b [] 3)\n\n(defn ab [] 2)\n")
    (doseq [p [(assoc projection :workspace_root "/wrong")
               (assoc projection :file "other.clj")
               (assoc projection :read_complete false) (dissoc projection :sha256)]]
      (h/refused h/source (assoc req :guard {:read_receipt p}) :invalid-guard))
    (h/refused h/source (assoc req :guard {:read_receipt projection :sha256 (h/sha h/source)})
               :invalid-guard)))
