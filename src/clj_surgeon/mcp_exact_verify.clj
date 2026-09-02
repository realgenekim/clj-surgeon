(ns clj-surgeon.mcp-exact-verify
  (:require
   [clj-surgeon.mcp-process :as process-env]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.charset StandardCharsets)
   (java.security MessageDigest)))

(defn admission-unverified?
  [{:keys [admission admission-error]}]
  (let [status (or (:status admission)
                   (get-in admission-error [:admission :status]))
        error-type (or (:error-type admission)
                       (:error-type admission-error))]
    (or (#{:delegated :admission-timeout :pressure-deferred} status)
        (#{:clj-kondo-admission-unavailable
           :clj-kondo-executable-unavailable
           :clj-kondo-admission-timeout
           :clj-kondo-pressure-deferred
           :process-interrupted}
         error-type))))

(defn- bytes->hex
  [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))

(def exact-verification-visible-bytes 12000)

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

(defn classify-exact-process-outcome
  [{:keys [finished? launch-error exit admission] :as process}]
  (cond
    (admission-unverified? process)
    {:process-outcome (if (= :admission-timeout (:status admission))
                        :admission-timeout
                        :admission-unavailable)}
    launch-error {:process-outcome :launch-failure}
    (not finished?) {:process-outcome :timeout}
    (zero? exit) {:process-outcome :pass}
    (>= exit 128) {:process-outcome :crash-or-signal-style-exit}
    :else {:process-outcome :ordinary-nonzero}))

(defn- sha256-text
  [text]
  (-> (doto (MessageDigest/getInstance "SHA-256")
        (.update (.getBytes ^String text StandardCharsets/UTF_8)))
      .digest
      bytes->hex))

(defn compile-exact-profile
  "Compile one project-owned exact profile into an immutable execution value."
  [profile profiles profile-source]
  ;; @spec MCP-OP-VERIFY-001
  ;; @spec MCP-OP-VERIFY-002
  ;; @spec MCP-OP-VERIFY-003
  ;; @spec MCP-OP-VERIFY-004
  ;; @spec MCP-OP-VERIFY-005
  (let [definition (get profiles profile)
        command (first (:commands definition))
        valid-command? (and (vector? command)
                            (seq command)
                            (every? #(and (string? %) (not (str/blank? %))) command))]
    (cond
      (not= "exact" profile)
      {:ok false :error-type :unknown-verification-profile
       :source-unchanged true}

      (not= :project profile-source)
      {:ok false :error-type :exact-profile-not-project-owned
       :source-unchanged true}

      (not (and (map? definition)
                (= #{:acceptance :timeout-ms :commands}
                   (set (keys definition)))
                (= :exact-exit (:acceptance definition))
                (integer? (:timeout-ms definition))
                (<= 1 (:timeout-ms definition) 120000)
                (= 1 (count (:commands definition)))
                valid-command?
                (not-any? #{"{files}"} command)))
      {:ok false :error-type :invalid-exact-verification-profile
       :source-unchanged true}

      :else
      {:ok true
       :profile profile
       :profile-source profile-source
       :profile-sha256 (sha256-text (pr-str (into (sorted-map) definition)))
       :acceptance :exact-exit
       :timeout-ms (:timeout-ms definition)
       :argv (expand-command command [])})))

(defn run-process!
  ([project-root command]
   (run-process! project-root command 120000))
  ([project-root command timeout-ms]
   (let [started (System/nanoTime)]
     (try
       (process-env/run-bounded!
         {:command command
          :cwd project-root
          :timeout-ms timeout-ms
          :merge-error? true
          :visible-byte-limit exact-verification-visible-bytes})
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

(defn run-exact-verification!
  "Execute one compiled exact profile and return terminal bounded evidence."
  [project-root compiled-profile]
  ;; @spec MCP-OP-VERIFY-003
  ;; @spec MCP-OP-VERIFY-005
  ;; @spec MCP-OP-VERIFY-006
  ;; @spec MCP-OP-VERIFY-007
  ;; @spec MCP-OP-VERIFY-009
  ;; @spec MCP-OP-VERIFY-010
  ;; @spec MCP-OP-ANALYZER-004
  (let [cwd (.getCanonicalPath (io/file project-root))
        process (run-process! cwd (:argv compiled-profile)
                              (:timeout-ms compiled-profile))
        outcome (:process-outcome (classify-exact-process-outcome process))
        evidence (merge
                   (select-keys compiled-profile
                                [:profile :profile-source :profile-sha256
                                 :acceptance :timeout-ms :argv])
                   {:cwd cwd
                    :process-outcome outcome
                    :exit (:exit process)
                    :elapsed_ms (:elapsed_ms process)
                    :output-bytes (:output-bytes process)
                    :output-sha256 (:output-sha256 process)
                    :output-truncated (:output-truncated process)}
                   (select-keys process [:admission :admission-error]))]
    (case outcome
      :pass (assoc evidence :ok true)
      :ordinary-nonzero
      (assoc evidence :ok false
             :error-type :verification-failed
             :diagnostics (:output process))
      (assoc evidence :ok false
             :error-type :verification-unverified
             :diagnostics (:output process)))))
