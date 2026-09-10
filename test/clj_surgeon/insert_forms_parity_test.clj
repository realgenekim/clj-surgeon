(ns clj-surgeon.insert-forms-parity-test
  {:lane :battery}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-support :as h]
   [clj-surgeon.mcp-insert-forms :as mcp]
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

;; @spec INSERT-FORMS-018
;; INTENT-TEST: INSERT-FORMS-018
(deftest insert-forms-cli-mcp-parity
  (doseq [prefix [[":insert-forms!"] [":op" ":insert-forms!"]]]
    (h/with-file h/source
      (fn [dir file req]
        (let [request-file (io/file dir "request.edn")
              _ (spit request-file (pr-str req))
              result (invoke-cli prefix request-file)
              receipt (stdout result)]
          (is (= 0 (:exit result)) (pr-str result))
          (is (= "committed" (:state receipt)))
          (is (= true (:write_verified receipt)))
          (is (= "(defn a [] 1)\n(defn b [] 3)\n\n(defn ab [] 2)\n" (slurp file)))
          (is (number? (:elapsed_ms receipt)))
          (is (< (count (:out result)) 4097))
          (spit file h/source)
          (let [[content error? mcp-receipt] (callback req)]
            (is (= false error?))
            (is (= (dissoc receipt :elapsed_ms :receipt_details_path :receipt_hash)
                   (dissoc mcp-receipt :elapsed_ms :receipt_details_path :receipt_hash)))
            (is (= mcp-receipt (edn/read-string (first content))))
            (let [detail (slurp (:receipt_details_path mcp-receipt))]
              (is (= (:receipt_hash mcp-receipt) (h/sha detail)))
              (is (= req (:request (edn/read-string detail))))))
          (doseq [[request kind] [[(assoc req :preview true) :invalid-request]
                                  [(assoc-in req [:guard :sha256] (apply str (repeat 64 "0"))) :source-hash-mismatch]
                                  [(assoc req :payload {:text ")" :forms 1}) :payload-parse-error]]]
            (spit file h/source)
            (spit request-file (pr-str request))
            (let [result (invoke-cli prefix request-file) receipt (stdout result)
                  [_ error? mcp-receipt] (callback request)]
              (is (= 2 (:exit result)))
              (is (= kind (:error-type receipt) (:error-type mcp-receipt)))
              (is (= false error?))
              (is (= "refused" (:state receipt)))
              (is (= h/source (slurp file)))))
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
    (h/with-file h/source
      (fn [dir file req]
        (let [request-file (io/file dir "failure.edn")
              _ (spit request-file (pr-str req))
              driver (str "(require '[clj-surgeon.core :as core] '[clj-surgeon.insert-forms :as insert] '[clojure.java.io :as io]) "
                          "(let [execute insert/execute!] (with-redefs [insert/execute! (fn [r] (execute r {"
                          (pr-str stage) " (fn [] "
                          (when (= stage :external-after-write)
                            "(spit (io/file (:workspace_root r) (:file r)) \"foreign\\n\") ")
                          "(throw (java.io.IOException. \"injected\")))}))] (apply core/-main *command-line-args*)))")
              result (shell/sh "bb" "-e" driver "--" ":insert-forms!" ":request-file" (str request-file))
              receipt (stdout result)]
          (is (= 1 (:exit result)) (pr-str result))
          (is (= state (:state receipt)))
          (is (= true (:mutation_attempted receipt)))
          (is (= (if (= stage :read-back) h/source "foreign\n") (slurp file)))))))
  (let [result (shell/sh "bb" "-m" "clj-surgeon.core" "--help")]
    (is (= 0 (:exit result)))
    (is (str/includes? (:out result) "insert-forms!"))))
