(ns clj-surgeon.recovery-test
  (:require
   [clj-surgeon.mcp-workspace :as mcp-workspace]
   [clj-surgeon.recovery :as recovery]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is use-fixtures]]))

(def ^:private temp-roots (atom []))
(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots))

(deftest recover-publishes-one-terminal-success-receipt
  (let [root (.toFile
               (tmp-leak/track!
                 temp-roots
                 (java.nio.file.Files/createTempDirectory
                   "clj-surgeon-recover-success"
                   (make-array java.nio.file.attribute.FileAttribute 0))))
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
               (tmp-leak/track!
                 temp-roots
                 (java.nio.file.Files/createTempDirectory
                   "clj-surgeon-recover-failure"
                   (make-array java.nio.file.attribute.FileAttribute 0))))
        receipt-file (io/file (mcp-workspace/receipt-dir (.getPath root))
                              "last-failure.edn")
        result (recovery/recover!
                 {:workspace (.getPath root)
                  :up-fn (fn [_]
                           {:ok true :servers {:clj-surgeon "surgeon"
                                               :cclsp "cclsp"}})
                  :probe-fn (fn [_]
                              (throw (ex-info "expired"
                                              {:error-type :invalid-mcp-session
                                               :capabilities
                                               {:structural-read :ready
                                                :structural-write :ready
                                                :semantic-surface :unavailable}
                                               :safe-route :structural-cli
                                               :fallback-command
                                               ["clj-surgeon" ":op" ":cat"
                                                ":file" "src/sample/core.clj"
                                                ":form" "target"]})))})
        persisted (edn/read-string (slurp receipt-file))]
    (is (false? (:ok result)))
    (is (= :fallback-safe (:terminal-state result)))
    (is (= :proof (:phase result)))
    (is (= :invalid-mcp-session (:error-type result)))
    (is (= ["clj-surgeon" "report-failure" "--receipt"
            (.getCanonicalPath receipt-file)]
           (:report-command result)))
    (is (= :report-failure-and-use-cli-fallback (:next-action persisted)))
    (is (= :structural-cli (:safe-route persisted)))
    (is (= {:structural-read :ready
            :structural-write :ready
            :semantic-surface :unavailable}
           (:capabilities persisted)))
    (is (= ["clj-surgeon" ":op" ":cat"
            ":file" "src/sample/core.clj" ":form" "target"]
           (:fallback-command result)))))

(deftest recover-creates-the-failure-receipt-directory
  (let [root (.toFile
               (tmp-leak/track!
                 temp-roots
                 (java.nio.file.Files/createTempDirectory
                   "clj-surgeon-recover-missing-receipts"
                   (make-array java.nio.file.attribute.FileAttribute 0))))
        receipt-dir (io/file (mcp-workspace/receipt-dir (.getPath root)))
        receipt-file (io/file receipt-dir "last-failure.edn")]
    (is (false? (.exists receipt-dir)))
    (let [result (recovery/recover!
                   {:workspace (.getPath root)
                    :up-fn (fn [_]
                             (throw (ex-info "startup failed"
                                             {:error-type :startup-failed})))})]
      (is (.isFile receipt-file))
      (is (= (.getCanonicalPath receipt-file)
             (:failure-receipt result)))
      (is (= ["clj-surgeon" "report-failure" "--receipt"
              (.getCanonicalPath receipt-file)]
             (:report-command result))))))
