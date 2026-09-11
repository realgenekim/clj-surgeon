(ns clj-surgeon.insert-forms-parity-test
  {:lane :battery}
  (:require [cheshire.core :as json]
            [clj-surgeon.splice-entrance-support :as entrance]
            [clj-surgeon.insert-forms-support :as h]
            [clj-surgeon.mcp-insert-forms :as mcp]
            [clojure.test :refer [deftest]]))

(defn callback [req]
  (let [out (atom nil)]
    (mcp/handle nil (json/parse-string (json/generate-string req))
                (fn [content error? result] (reset! out [content error? result])))
    @out))

;; @spec INSERT-FORMS-018
;; INTENT-TEST: INSERT-FORMS-018
(deftest insert-forms-cli-mcp-parity
  (let [source h/source]
    (entrance/exercise "insert-forms" source "(defn a [] 1)\n(defn b [] 3)\n(defn ab [] 2)\n" (h/request h/source) callback)))
