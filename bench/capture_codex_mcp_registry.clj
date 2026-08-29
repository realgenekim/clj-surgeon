(ns capture-codex-mcp-registry
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io])
  (:import
   (java.nio.file Files StandardCopyOption)
   (java.util.concurrent TimeUnit)))

(def ^:private total-timeout-ms 45000)

(def mcp-registry-observation-source
  {:kind "codex-app-server-json-rpc"
   :method "mcpServerStatus/list"
   :detail "toolsAndAuthOnly"
   :response-path ["result" "data"]})

(defn- parse-args [args]
  (when (odd? (count args))
    (throw (ex-info "Expected --key value pairs" {:args args})))
  (into {} (map (fn [[key value]] [(keyword (subs key 2)) value]))
        (partition 2 args)))

(defn- send-json! [writer value]
  (.write writer (json/generate-string value))
  (.newLine writer)
  (.flush writer))

(defn- remaining-ms [deadline-nanos]
  (max 0 (long (/ (- deadline-nanos (System/nanoTime)) 1000000))))

(defn- read-line! [reader deadline-nanos]
  (let [timeout-ms (remaining-ms deadline-nanos)
        _ (when (zero? timeout-ms)
            (throw (ex-info "Timed out waiting for Codex app-server"
                            {:timeout-ms total-timeout-ms})))
        line (future (.readLine reader))
        value (deref line timeout-ms ::timeout)]
    (when (= ::timeout value)
      (future-cancel line)
      (throw (ex-info "Timed out waiting for Codex app-server"
                      {:timeout-ms timeout-ms})))
    (when (nil? value)
      (throw (ex-info "Codex app-server closed stdout" {})))
    value))

(defn- response-error [message]
  (or (:error message)
      (when-not (contains? message :result)
        {:message "Response omitted both result and error"})))

(defn- read-response! [reader id transcript deadline-nanos]
  (loop []
    (let [raw (read-line! reader deadline-nanos)
          message (json/parse-string raw true)]
      (swap! transcript conj {:direction "from-app-server"
                              :raw raw})
      (if (= id (:id message)) message (recur)))))

(defn- request! [writer reader transcript deadline-nanos id method params]
  (let [request {:id id :method method :params params}]
    (swap! transcript conj {:direction "to-app-server"
                            :raw (json/generate-string request)})
    (send-json! writer request)
    (let [response (read-response! reader id transcript deadline-nanos)]
      (when-let [error (response-error response)]
        (throw (ex-info "Codex app-server request failed"
                        {:id id :method method :error error})))
      response)))

(defn- notify! [writer transcript method]
  (let [notification {:method method}]
    (swap! transcript conj {:direction "to-app-server"
                            :raw (json/generate-string notification)})
    (send-json! writer notification)))

(defn- matching-servers [response server-name]
  (filterv #(= server-name (:name %)) (get-in response [:result :data])))

(defn- canonical-path [path]
  (some-> path io/file .getCanonicalPath))

(defn- tool-projection [selected]
  (->> (:tools selected)
       (map (fn [[tool-name tool]]
              {:name (name tool-name)
               :description (:description tool)
               :input-schema (:inputSchema tool)
               :output-schema (:outputSchema tool)
               :annotations (:annotations tool)}))
       (sort-by :name)
       vec))

(defn- write-json-atomically! [output value]
  (let [output-file (io/file output)
        _ (some-> output-file .getParentFile .mkdirs)
        temporary-file
        (io/file (str output ".tmp." (.pid (java.lang.ProcessHandle/current))))]
    (spit temporary-file
          (str (json/generate-string value {:pretty true}) "\n"))
    (Files/move (.toPath temporary-file)
                (.toPath output-file)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))))

(defn- cleanup-process! [process]
  (.destroy process)
  (let [terminated? (.waitFor process 2 TimeUnit/SECONDS)]
    (when-not terminated?
      (.destroyForcibly process)
      (.waitFor process 2 TimeUnit/SECONDS))
    {:alive (.isAlive process)
     :exit-code (when-not (.isAlive process) (.exitValue process))}))

(defn- capture! [{:keys [codex output server]
                  :or {codex "codex" server "clj-surgeon"}}]
  (when-not output
    (throw (ex-info "--output is required" {})))
  (let [expected-codex-home (canonical-path (System/getenv "CODEX_HOME"))
        codex-executable (canonical-path codex)
        stderr-file (io/file (str output ".app-server.stderr"))
        process-builder (doto (ProcessBuilder. ^java.util.List
                                [codex "app-server" "--stdio"])
                          (.redirectError stderr-file))
        process (.start process-builder)
        transcript (atom [])
        deadline-nanos (+ (System/nanoTime) (* total-timeout-ms 1000000))
        outcome
        (try
          {:receipt
           (with-open [writer (io/writer (.getOutputStream process))
                       reader (io/reader (.getInputStream process))]
             (let [initialize
                   (request! writer reader transcript deadline-nanos 1
                             "initialize"
                             {:clientInfo {:name "clj-surgeon-benchmark"
                                           :title "clj-surgeon benchmark"
                                           :version "1"}
                              :capabilities {:experimentalApi true
                                             :requestAttestation false}})
                   actual-codex-home
                   (canonical-path (get-in initialize [:result :codexHome]))]
               (when (and expected-codex-home
                          (not= expected-codex-home actual-codex-home))
                 (throw
                   (ex-info "Codex app-server ignored the requested CODEX_HOME"
                            {:expected expected-codex-home
                             :actual actual-codex-home})))
               (notify! writer transcript "initialized")
               (loop [attempt 1]
                 (let [response
                       (request! writer reader transcript deadline-nanos
                                 (+ 1 attempt) "mcpServerStatus/list"
                                 {:detail "toolsAndAuthOnly"})
                       matches (matching-servers response server)]
                   (when (< 1 (count matches))
                     (throw (ex-info "Codex reported duplicate MCP servers"
                                     {:server server
                                      :matching-server-count (count matches)})))
                   (if-let [selected (when (seq (:tools (first matches)))
                                       (first matches))]
                     {:schema "clj-surgeon.codex-mcp-registry.v1"
                      :ok true
                      :server server
                      :observation-source
                      (assoc mcp-registry-observation-source
                             :server-selector {:field "name"
                                               :value server})
                      :codex-executable codex-executable
                      :expected-codex-home expected-codex-home
                      :actual-codex-home actual-codex-home
                      :initialize initialize
                      :attempts attempt
                      :selected-server-metadata (dissoc selected :tools)
                      :tool-names (vec (sort (keys (:tools selected))))
                      :tool-projection (tool-projection selected)
                      :transcript @transcript}
                     (if (or (= attempt 60)
                             (zero? (remaining-ms deadline-nanos)))
                       (throw
                         (ex-info "Codex did not ingest the requested MCP tools"
                                  {:server server :attempts attempt}))
                       (do (Thread/sleep 250) (recur (inc attempt)))))))))}
          (catch Throwable throwable
            {:failure throwable}))
        cleanup (cleanup-process! process)
        stderr (when (.isFile stderr-file) (slurp stderr-file))]
    (if-let [failure (:failure outcome)]
      (let [receipt {:schema "clj-surgeon.codex-mcp-registry.v1"
                     :ok false
                     :server server
                     :codex-executable codex-executable
                     :expected-codex-home expected-codex-home
                     :error {:class (str (class failure))
                             :message (.getMessage failure)
                             :data (ex-data failure)}
                     :process cleanup
                     :stderr stderr
                     :transcript @transcript}]
        (write-json-atomically! output receipt)
        (throw (ex-info "Codex MCP registry capture failed"
                        {:output output :receipt receipt}
                        failure)))
      (let [receipt (assoc (:receipt outcome)
                           :process cleanup
                           :stderr stderr)]
        (when (:alive cleanup)
          (throw (ex-info "Codex app-server survived forced cleanup"
                          {:output output :process cleanup})))
        (write-json-atomically! output receipt)
        receipt))))

(defn -main [& args]
  (try
    (let [receipt (capture! (parse-args args))]
      (println (json/generate-string
                 (select-keys receipt [:ok :server :attempts :tool-names]))))
    (finally
      (shutdown-agents))))
