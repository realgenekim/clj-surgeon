(ns clj-surgeon.verification-process
  "Shared bounded command execution, independent of MCP and nREPL."
  (:require [clj-surgeon.mcp-process :as process-env]
            [clj-surgeon.structural-lens :as lens]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def exact-verification-visible-bytes 12000)
(defn- sha256-text [text] (lens/source-hash text))

(defn expand-command
  [command files]
  (let [expanded (vec (mapcat #(if (= "{files}" %) files [%]) command))
        executable (first expanded)
        search-paths ["/opt/homebrew/opt/node@20/bin"
                      "/opt/homebrew/bin"
                      "/usr/local/bin"
                      "/usr/bin"
                      "/bin"]
        resolved (when-not (str/includes? executable "/")
                   (some (fn [directory]
                           (let [candidate (io/file directory executable)]
                             (when (and (.isFile candidate) (.canExecute candidate))
                               (.getPath candidate))))
                         search-paths))]
    (assoc expanded 0 (or resolved executable))))

(defn run-process!
  "Run one bounded command and return its evidence.

  `visible-byte-limit` bounds how much of the child's output this JVM READS
  back. It defaults to the receipt's publication budget because most callers
  publish what they read; a caller that PARSES the output rather than
  publishing it must pass its own ceiling, because a cap sized for a receipt
  is a cap on the truth a detector gets to see. See MCP-OP-ADMIT-122."
  ([project-root command]
   (run-process! project-root command 120000))
  ([project-root command timeout-ms]
   (run-process! project-root command timeout-ms
                 exact-verification-visible-bytes))
  ([project-root command timeout-ms visible-byte-limit]
   (let [started (System/nanoTime)]
     (try
       (process-env/run-bounded!
         {:command command
          :cwd project-root
          :timeout-ms timeout-ms
          :merge-error? true
          :visible-byte-limit visible-byte-limit})
       (catch Exception error
         {:finished? false
          :launch-error true
          :exit nil
          :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0)
          :output (or (.getMessage error) (.getName (class error)))
          :output-bytes 0
          :output-sha256 (sha256-text "")
          :output-truncated false
          :admission-error (ex-data error)})))))
