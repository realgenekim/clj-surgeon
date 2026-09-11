(ns clj-surgeon.rename-alias-parity-test
  {:lane :battery}
  (:require [cheshire.core :as json]
            [clj-surgeon.splice-entrance-support :as entrance]
            [clj-surgeon.rename-alias-test :as r]
            [clj-surgeon.mcp-rename-alias :as mcp]
            [clojure.test :refer [deftest]]))

(defn callback [req]
  (let [out (atom nil)]
    (mcp/handle nil (json/parse-string (json/generate-string req))
                (fn [content error? result] (reset! out [content error? result])))
    @out))

;; @spec RENAME-ALIAS-011
;; INTENT-TEST: RENAME-ALIAS-011
(deftest rename-alias-cli-mcp-parity
  (let [source (str r/header "events/x")]
    (entrance/exercise "rename-alias" source "(ns demo (:require [example.events :as ev]))\nev/x" (r/request {r/file source} 1) callback)))
