(ns clj-surgeon.insert-forms-portable-receipt-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clj-surgeon.mcp-inspect-tool :as inspect]
   [clojure.test :refer [deftest is]]))

;; @spec INSERT-FORMS-012
;; INTENT-TEST: INSERT-FORMS-012
(deftest insert-forms-portable-read-receipt
  (h/with-file h/source
    (fn [dir _ req]
      (let [read-result (inspect/execute-inspect! {:project-root (str dir)}
                          {:requests [{:id "r" :operation "outline" :file (:file req)}]
                           :expect {:requests 1 :files 1}})
            projection (get-in read-result [:read_receipts (:file req)])]
        (is (= {:version 1 :read_complete true :workspace_root (:workspace_root req)
                :file (:file req) :sha256 (h/sha h/source)} projection))
        (is (= "committed" (:state (insert/execute! (assoc req :guard {:read_receipt projection}))))))))

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
