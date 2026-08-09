(ns clj-surgeon.recovery-test
  (:require
   [clj-surgeon.recovery :as recovery]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]))

(deftest recover-publishes-one-terminal-success-receipt
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "clj-surgeon-recover-success"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        result (recovery/recover!
                 {:workspace (.getPath root)
                  :up-fn (fn [_]
                           {:ok true
                            :servers {:clj-surgeon "surgeon"
                                      :cclsp "cclsp"}
                            :cclsp-config-changed false
                            :codex-config-changed false
                            :cclsp-server-restarted false})
                  :probe-fn (fn [_]
                              {:ok true
                               :catalog {:inspect-clojure true}
                               :semantic {:lsp-session "lsp-1"}
                               :mutation {:verification-complete true}})})]
    (is (:ok result))
    (is (= :recovered (:terminal-state result)))
    (is (= :none (:next-action result)))
    (is (false? (:agent-session-restart-required result)))))

(deftest recover-fails-once-and-publishes-an-executable-report-command
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "clj-surgeon-recover-failure"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        receipt-file (io/file root "last-failure.edn")
        result (recovery/recover!
                 {:workspace (.getPath root)
                  :failure-receipt (.getPath receipt-file)
                  :up-fn (fn [_]
                           {:ok true :servers {:clj-surgeon "surgeon"
                                               :cclsp "cclsp"}})
                  :probe-fn (fn [_]
                              (throw (ex-info "expired"
                                              {:error-type
                                               :invalid-mcp-session})))})
        persisted (edn/read-string (slurp receipt-file))]
    (is (false? (:ok result)))
    (is (= :fallback-safe (:terminal-state result)))
    (is (= :proof (:phase result)))
    (is (= :invalid-mcp-session (:error-type result)))
    (is (= ["clj-surgeon" "report-failure" "--receipt"
            (.getCanonicalPath receipt-file)]
           (:report-command result)))
    (is (= :report-failure-and-use-cli-fallback (:next-action persisted)))))
