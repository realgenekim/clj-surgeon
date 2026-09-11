(ns clj-surgeon.rename-alias-parity-test
  {:lane :battery}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.rename-alias :as insert]
   [clj-surgeon.rename-alias-test :as rh]
   [clj-surgeon.insert-forms-support :as h]
   [clj-surgeon.mcp-rename-alias :as mcp]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(defn invoke-cli [prefix path]
  (apply shell/sh (concat ["bb" "-m" "clj-surgeon.core"] prefix [":request-file" (str path)])))
(defn stdout [result]
  (let [r (insert/read-request (:out result))]
    (is (map? r))
    (is (str/starts-with? (:out result) "{:state "))
    (is (str/ends-with? (:out result) "}\n"))
    r))
(defn callback [req]
  (let [out (atom nil)]
    (mcp/handle nil (json/parse-string (json/generate-string req)) (fn [content error? result] (reset! out [content error? result])))
    @out))

;; @spec RENAME-ALIAS-011
;; INTENT-TEST: RENAME-ALIAS-011
(deftest rename-alias-cli-mcp-parity
  (doseq [prefix [[":rename-alias!"] [":op" ":rename-alias!"]]]
    (h/with-file (str rh/header "events/x")
      (fn [dir file _]
        (let [req (assoc (rh/request {rh/file (slurp file)} 1) :workspace_root (.getCanonicalPath dir))
              request-file (io/file dir "request.edn")
              _ (spit request-file (pr-str req))
              result (invoke-cli prefix request-file)
              receipt (stdout result)]
          (is (= 0 (:exit result)) (pr-str result))
          (is (= "committed" (:state receipt)))
          (is (= true (:write_verified receipt)))
          (is (= "(ns demo (:require [example.events :as ev]))\nev/x" (slurp file)))
          (is (number? (:elapsed_ms receipt)))
          (is (< (count (:out result)) 4097))
          (spit file (str rh/header "events/x"))
          (let [[content error? mcp-receipt] (callback req)]
            (is (= false error?))
            (is (= (dissoc receipt :elapsed_ms :receipt_details_path :receipt_hash)
                   (dissoc mcp-receipt :elapsed_ms :receipt_details_path :receipt_hash)))
            (is (= mcp-receipt (edn/read-string (first content))))
            (let [detail (slurp (:receipt_details_path mcp-receipt))]
              (is (= (:receipt_hash mcp-receipt) (h/sha detail)))
              (is (= req (:request (edn/read-string detail))))))
          (doseq [[request kind] [[(assoc req :preview true) :invalid-request]
                                  [(assoc-in req [:guards rh/file :sha256] (apply str (repeat 64 "0"))) :source-hash-mismatch]
                                  [(assoc-in req [:expect :references :total] 31) :expect-count-mismatch]]]
            (spit file (str rh/header "events/x"))
            (spit request-file (pr-str request))
            (let [result (invoke-cli prefix request-file) receipt (stdout result)
                  [_ error? mcp-receipt] (callback request)]
              (is (= 2 (:exit result)))
              (is (= kind (:error-type receipt) (:error-type mcp-receipt)))
              (is (= false error?))
              (is (= "refused" (:state receipt)))
              (is (= (str rh/header "events/x") (slurp file)))))
          (doseq [bad ["{} {}" "{:version 1 :version 1}" "#unsafe/tag {}" "#=(+ 1 2)" "{"]]
            (spit request-file bad)
            (let [result (invoke-cli prefix request-file)]
              (is (= 2 (:exit result)))
              (is (= :invalid-request (:error-type (stdout result))))))
          (let [result (invoke-cli prefix (io/file dir "missing.edn"))]
            (is (= 1 (:exit result)))
            (is (= "failed" (:state (stdout result)))))
          (let [result (apply shell/sh (concat ["bb" "-m" "clj-surgeon.core"] prefix ["--help"]))]
            (is (= 0 (:exit result)))
            (is (str/includes? (:out result) ":request-file"))
            (is (str/includes? (:out result) "verification_complete=false")))))))
  (doseq [[stage state] [[:read-back "rolled-back"] [:external-after-write "recovery-required"]]]
    (h/with-file (str rh/header "events/x")
      (fn [dir file _]
        (let [req (assoc (rh/request {rh/file (slurp file)} 1) :workspace_root (.getCanonicalPath dir))
              request-file (io/file dir "failure.edn")
              _ (spit request-file (pr-str req))
              driver (str "(require '[clj-surgeon.core :as core] '[clj-surgeon.rename-alias :as insert] '[clojure.java.io :as io]) "
                          "(let [execute insert/execute!] (with-redefs [insert/execute! (fn [r] (execute r {"
                          (pr-str stage) " (fn [] "
                          (when (= stage :external-after-write)
                            "(spit (io/file (:workspace_root r) (get-in r [:scope :paths 0])) \"foreign\\n\") ")
                          "(throw (java.io.IOException. \"injected\")))}))] (apply core/-main *command-line-args*)))")
              result (shell/sh "bb" "-e" driver "--" ":rename-alias!" ":request-file" (str request-file))
              receipt (stdout result)]
          (is (= 1 (:exit result)) (pr-str result))
          (is (= state (:state receipt)))
          (is (= true (:mutation_attempted receipt)))
          (is (= (if (= stage :read-back) (str rh/header "events/x") "foreign\n") (slurp file)))))))
  (let [result (shell/sh "bb" "-m" "clj-surgeon.core" "--help")]
    (is (= 0 (:exit result)))
    (is (str/includes? (:out result) "rename-alias!"))))
