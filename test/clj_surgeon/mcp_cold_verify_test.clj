(ns clj-surgeon.mcp-cold-verify-test
  (:require
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-process :as process-env]
   [clojure.test :refer [deftest is]]))

(defn- temp-dir
  []
  (.toFile (java.nio.file.Files/createTempDirectory
             "clj-surgeon-cold-verify-"
             (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

(defn- await-terminal
  [root job]
  (loop [attempt 0]
    (let [result (cold-verify/status (.getPath root) job)]
      (if (or (:verification_complete result) (>= attempt 100))
        result
        (do (Thread/sleep 10) (recur (inc attempt)))))))

(deftest launches-bounded-cold-proof-and-returns-a-queryable-receipt
  (let [root (temp-dir)]
    (try
      (cold-verify/clear-jobs!)
      (let [launched (cold-verify/launch!
                       (.getPath root) "full"
                       {:command ["/bin/sh" "-c" "printf cold-ok"]
                        :timeout-ms 1000})
            result (await-terminal root (:verification_job launched))]
        (is (:ok (cold-verify/attach-undo!
                   (.getPath root) (:verification_job launched)
                   "/tmp/cold-undo.edn" "receipt-hash")))
        (let [result (cold-verify/status (.getPath root)
                                         (:verification_job launched))]
          (is (= "/tmp/cold-undo.edn" (:undo_receipt result)))
          (is (= "receipt-hash" (:receipt_hash result))))
        (is (:ok launched))
        (is (= :running (:status launched)))
        (is (false? (:verification_complete launched)))
        (is (= "inspect_clojure" (get-in launched [:next_call :tool])))
        (is (:ok result))
        (is (:passed result))
        (is (= :passed (:status result)))
        (is (true? (:verification_complete result)))
        (is (= "cold-ok" (:output result)))
        (is (= "none" (:next_action result))))
      (finally
        (cold-verify/clear-jobs!)
        (delete-tree! root)))))

(deftest terminal-failure-and-timeout-are-evidence-not-tool-call-failures
  (let [root (temp-dir)]
    (try
      (cold-verify/clear-jobs!)
      (doseq [[command expected-status]
              [[["/bin/sh" "-c" "printf nope; exit 7"] :failed]
               [["/bin/sh" "-c" "sleep 5 & wait"] :timed-out]]]
        (let [launched (cold-verify/launch!
                         (.getPath root) "full"
                         {:command command :timeout-ms 100})
              result (await-terminal root (:verification_job launched))]
          (is (:ok result))
          (is (false? (:passed result)))
          (is (= expected-status (:status result)))
          (when (= :timed-out expected-status)
            (is (true? (:termination_confirmed result))))
          (is (true? (:verification_complete result)))
          (is (= "review_failure_and_use_undo_receipt" (:next_action result)))))
      (finally
        (cold-verify/clear-jobs!)
        (delete-tree! root)))))

(deftest cold-clj-kondo-admission-timeout-is-unverified
  ;; @spec MCP-OP-ANALYZER-002
  ;; @spec MCP-OP-ANALYZER-004
  ;; @spec MCP-OP-ANALYZER-005
  (let [root (temp-dir)
        bounded-call (atom nil)]
    (try
      (cold-verify/clear-jobs!)
      (with-redefs [process-env/run-bounded!
                    (fn [request]
                      (reset! bounded-call request)
                      {:finished? true
                       :exit 75
                       :elapsed_ms 150.0
                       :output "clj-kondo-admission-timeout"
                       :output-bytes 27
                       :output-sha256 (apply str (repeat 64 "a"))
                       :output-truncated false
                       :admission {:status :admission-timeout
                                   :error-type :clj-kondo-admission-timeout}})]
        (let [launched (cold-verify/launch!
                         (.getPath root) "full"
                         {:command ["clj-kondo" "--lint" "must-not-launch"]
                          :timeout-ms 250})
              result (await-terminal root (:verification_job launched))]
          (is (= ["clj-kondo" "--lint" "must-not-launch"]
                 (:command @bounded-call)))
          (is (= 250 (:timeout-ms @bounded-call)))
          (is (:ok result))
          (is (false? (:passed result)))
          (is (= :unverified (:status result)))
          (is (= :clj-kondo-admission-timeout (:error-type result)))
          (is (= :admission-timeout
                 (get-in result [:admission :status])))
          (is (true? (:verification_complete result)))
          (is (= "restore_analyzer_authority_before_retry"
                 (:next_action result)))))
      (finally
        (cold-verify/clear-jobs!)
        (delete-tree! root)))))

(deftest missing-cold-clj-kondo-admission-is-unverified
  ;; @spec MCP-OP-ANALYZER-004
  ;; @spec MCP-OP-ANALYZER-005
  (let [root (temp-dir)]
    (try
      (cold-verify/clear-jobs!)
      (binding [process-env/*clj-kondo-admission-path*
                "/definitely/missing/clj-kondo-admission"]
        (let [launched (cold-verify/launch!
                         (.getPath root) "full"
                         {:command ["clj-kondo" "--lint" "must-not-launch"]
                          :timeout-ms 250})
              result (await-terminal root (:verification_job launched))]
          (is (:ok result))
          (is (false? (:passed result)))
          (is (= :unverified (:status result)))
          (is (= :clj-kondo-admission-unavailable (:error-type result)))
          (is (= "restore_analyzer_authority_before_retry"
                 (:next_action result)))))
      (finally
        (cold-verify/clear-jobs!)
        (delete-tree! root)))))

(deftest status-is-confined-to-the-launching-workspace
  (let [root (temp-dir)
        other (temp-dir)]
    (try
      (cold-verify/clear-jobs!)
      (let [launched (cold-verify/launch!
                       (.getPath root) "full"
                       {:command ["/bin/sh" "-c" "printf ok"]
                        :timeout-ms 1000})
            refused (cold-verify/status (.getPath other)
                                        (:verification_job launched))]
        (is (false? (:ok refused)))
        (is (= :verification-job-workspace-mismatch (:error-type refused))))
      (finally
        (cold-verify/clear-jobs!)
        (delete-tree! root)
        (delete-tree! other)))))

(deftest reserves-worker-capacity-before-launching-the-future
  (let [root (temp-dir)
        max-running-var (ns-resolve 'clj-surgeon.mcp-cold-verify
                                    'max-running-jobs)]
    (try
      (cold-verify/clear-jobs!)
      (with-redefs-fn {max-running-var 1}
        (fn []
          (let [first-job (cold-verify/launch!
                            (.getPath root) "full"
                            {:command ["/bin/sh" "-c" "sleep 0.2"]
                             :timeout-ms 1000})
                refused (cold-verify/launch!
                          (.getPath root) "full"
                          {:command ["/bin/sh" "-c" "printf should-not-run"]
                           :timeout-ms 1000})]
            (is (:ok first-job))
            (is (false? (:ok refused)))
            (is (= :cold-verification-capacity-exceeded
                   (:error-type refused)))
            (is (= 1 (:running-jobs refused)))
            (is (:passed (await-terminal root (:verification_job first-job)))))))
      (finally
        (cold-verify/clear-jobs!)
        (delete-tree! root)))))

(deftest cold-profile-is-closed-and-bounded
  (is (cold-verify/valid-profile?
        {:command ["make" "test"] :timeout-ms 600000}))
  (doseq [invalid [{:command ["make" "test"]}
                   {:command "make test" :timeout-ms 1000}
                   {:command ["make" ""] :timeout-ms 1000}
                   {:command ["make"] :timeout-ms 99}
                   {:command ["make"] :timeout-ms 1800001}
                   {:command ["make"] :timeout-ms 1000 :extra true}]]
    (is (false? (cold-verify/valid-profile? invalid)) (pr-str invalid))))
