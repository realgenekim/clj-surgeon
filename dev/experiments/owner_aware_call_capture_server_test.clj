(ns owner-aware-call-capture-server-test
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [owner-aware-call-capture-server :as server]
   [owner-aware-call-construction-screen :as screen]))

(deftest projected-server-surface-is-arm-exact
  (let [control (server/capture-tool :control "/tmp/control.json")
        candidate (server/capture-tool :candidate "/tmp/candidate.json")]
    (is (= "edit_clojure" (:name control) (:name candidate)))
    (is (= (:schema (screen/tool-surface :control)) (:schema control)))
    (is (= (:schema (screen/tool-surface :candidate)) (:schema candidate)))
    (is (= (:description (screen/tool-surface :control))
           (:description control)))
    (is (= (:description (screen/tool-surface :candidate))
           (:description candidate)))))

(deftest capture-handler-records-every-call-without-invoking-product-writes
  (let [path (str (java.nio.file.Files/createTempFile
                    "owner-aware-call-capture-"
                    ".json"
                    (make-array java.nio.file.attribute.FileAttribute 0)))
        callbacks (atom [])
        handler (server/capture-handler path)]
    (try
      (handler nil {"edits" [{"file" "src/a.clj"}]}
               #(swap! callbacks conj [%1 %2 %3]))
      (handler nil {"symbol_migration" {"target_alias" "new"}}
               #(swap! callbacks conj [%1 %2 %3]))
      (let [receipt (json/parse-string (slurp path))]
        (is (= 2 (count (get receipt "calls"))))
        (is (= 2 (get-in @callbacks [1 2 :call_count])))
        (is (false? (get-in @callbacks [0 1])))
        (is (true? (get-in @callbacks [0 2 :source_unchanged]))))
      (finally
        (.delete (io/file path))))))

(apply clojure.test/run-tests ['owner-aware-call-capture-server-test])
