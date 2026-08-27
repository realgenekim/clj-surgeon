(ns analyzer-contract-test-runner
  (:require
   [clj-surgeon.analyzer-contract-test]
   [clj-surgeon.mcp-process :as process]
   [clojure.java.io :as io]
   [clojure.test :refer [run-tests]]))

(def mission-scope-files
  ["test/analyzer_contract_test_runner.clj"
   "test/clj_surgeon/analyzer_contract_test.clj"
   "src/clj_surgeon/forward_refs.clj"
   "src/clj_surgeon/binding_rename.clj"
   "src/clj_surgeon/mcp_change_buffer.clj"])

(defn sha256 [text]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes text java.nio.charset.StandardCharsets/UTF_8))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest)))))

(defn mission-scope-sha256 []
  (sha256
    (apply str
           (map (fn [path]
                  (str path "\u0000" (slurp (io/file path)) "\u0000"))
                mission-scope-files))))

(let [result (process/call-with-analyzer-contract-mission
               (System/getProperty "user.dir")
               (mission-scope-sha256)
               #(run-tests 'clj-surgeon.analyzer-contract-test))]
  (System/exit (+ (:fail result) (:error result))))
