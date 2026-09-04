(ns clj-surgeon.recovery
  "One bounded reset button for the shared structural tool stack."
  (:require
   [clj-surgeon.mcp-recovery :as mcp-recovery]
   [clj-surgeon.measured :as measured]
   [clj-surgeon.mcp-workspace :as mcp-workspace]
   [clj-surgeon.workspace-onboarding :as onboarding]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]))

(defn- elapsed-ms
  [started]
  (measured/elapsed-ms started))

(defn- failure-receipt-path
  [workspace]
  (io/file (mcp-workspace/receipt-dir workspace) "last-failure.edn"))

(defn- write-failure-receipt!
  [target receipt]
  (.mkdirs (.getParentFile target))
  (spit target (with-out-str (pp/pprint receipt)))
  (.getCanonicalPath target))

(defn recover!
  "Run one repair attempt and prove the first structural transaction."
  [{:keys [workspace up-fn probe-fn failure-receipt] :as options}]
  (let [started (measured/start)
        phase (atom :onboarding)
        workspace (.getCanonicalPath
                    (io/file (or workspace (System/getProperty "user.dir"))))
        up-fn (or up-fn onboarding/up!)
        probe-fn (or probe-fn mcp-recovery/probe!)]
    (try
      (let [up-started (measured/start)
            up-result (up-fn (assoc (select-keys options
                                                 [:tool-root :state-dir :lsp-command
                                                  :runner :readiness-probe
                                                  :surgeon-url :cclsp-url])
                                    :workspace workspace))
            up-elapsed (elapsed-ms up-started)
            _ (reset! phase :proof)
            probe-started (measured/start)
            probe-result (probe-fn {:workspace workspace
                                    :surgeon-url (get-in up-result
                                                         [:servers :clj-surgeon])
                                    :cclsp-url (get-in up-result [:servers :cclsp])})
            probe-elapsed (elapsed-ms probe-started)]
        {:ok true
         :operation :clj-surgeon-recover
         :terminal-state :recovered
         :workspace workspace
         :agent-session-restart-required false
         :changed {:cclsp-config (:cclsp-config-changed up-result)
                   :codex-config (:codex-config-changed up-result)
                   :server-restarted (:cclsp-server-restarted up-result)}
         :proof probe-result
         ;; @spec MCP-OP-TIME-005
         ;; A recovery receipt is written to disk and fingerprinted; its three
         ;; clock readings ride the partition like every other measured field.
         measured/measured-key {:elapsed-ms {:up up-elapsed
                                 :probe probe-elapsed
                                 :total (elapsed-ms started)}}
         :next-action :none})
      (catch Exception error
        (let [error-data (ex-data error)
              restart-required? (true? (:agent-session-restart-required error-data))
              target (io/file (or failure-receipt (failure-receipt-path workspace)))
              receipt (merge
                        {:ok false
                         :operation :clj-surgeon-recover
                         :phase (or (:phase error-data) @phase)
                         :terminal-state (if restart-required?
                                           :restart-required
                                           :fallback-safe)
                         :workspace workspace
                         :error-type (or (:error-type error-data)
                                         :recovery-failed)
                         :error (.getMessage error)
                         :agent-session-restart-required restart-required?
                         measured/measured-key {:elapsed-ms {:total (elapsed-ms started)}}
                         :next-action (if restart-required?
                                        :restart-agent-session-once
                                        :report-failure-and-use-cli-fallback)}
                        (select-keys error-data
                                     [:capabilities :safe-route
                                      :retained-source-anchor
                                      :fallback-command]))
              receipt-path (write-failure-receipt! target receipt)]
          (assoc receipt
                 :failure-receipt receipt-path
                 :report-command ["clj-surgeon" "report-failure"
                                  "--receipt" receipt-path]))))))
