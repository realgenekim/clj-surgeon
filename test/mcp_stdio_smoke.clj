(ns mcp-stdio-smoke
  (:require
   [babashka.process :as process]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def requests
  [{:jsonrpc "2.0"
    :id 1
    :method "initialize"
    :params {:protocolVersion "2024-11-05"
             :capabilities {}
             :clientInfo {:name "clj-surgeon-smoke" :version "0"}}}
   {:jsonrpc "2.0" :method "notifications/initialized"}
   {:jsonrpc "2.0" :id 2 :method "tools/list" :params {}}
   {:jsonrpc "2.0" :id 3 :method "tools/call"
    :params {:name "apply_clojure_changes" :arguments {}}}])

(def smoke-deadline-ms
  120000)

(defn- assert!
  [truth message data]
  (when-not truth
    (throw (ex-info message data))))

(defn- response-by-id
  [responses id]
  (first (filter #(= id (:id %)) responses)))

(defn -main
  [& _]
  (let [started (System/nanoTime)
        command ["clojure" "-X:clj-surgeon/mcp-stdio"
                 ":nrepl-port" ":none" ":telemetry" ":off"]
        server (process/process command {:in :pipe :out :pipe :err :pipe})
        responses (atom [])
        done (promise)
        stderr-future (future (slurp (:err server)))
        stdout-future
        (future
          (try
            (with-open [reader (io/reader (:out server))]
              (loop []
                (when-let [line (.readLine reader)]
                  (let [message
                        (try
                          (json/parse-string line true)
                          (catch Exception error
                            (deliver done {:error :non-json-stdout
                                           :line line
                                           :message (.getMessage error)})))]
                    (when message
                      (swap! responses conj message)
                      (when (= #{1 2 3}
                               (set (keep :id @responses)))
                        (deliver done {:ok true})))
                    (when-not (realized? done) (recur))))))
            (catch Exception error
              (deliver done {:error :stdout-reader-failed
                             :message (.getMessage error)}))))]
    (try
      (let [writer (io/writer (:in server))]
        (doseq [request requests]
          (.write writer (json/generate-string request))
          (.write writer "\n"))
        (.flush writer))
      (let [terminal (deref done smoke-deadline-ms
                            {:error :timeout
                             :deadline-ms smoke-deadline-ms})]
        (assert! (:ok terminal) "MCP smoke did not receive three responses"
                 (assoc terminal :responses @responses))
        (let [initialize (response-by-id @responses 1)
              tools-list (response-by-id @responses 2)
              invalid-call (response-by-id @responses 3)
              tools (get-in tools-list [:result :tools])
              refusal-text (get-in invalid-call
                                   [:result :content 0 :text])
              refusal (get-in invalid-call [:result :structuredContent])
              capabilities (get-in initialize [:result :capabilities])]
          (assert! (= "clj-surgeon"
                      (get-in initialize [:result :serverInfo :name]))
                   "Wrong MCP server identity" {:initialize initialize})
          (assert! (str/includes?
                     (get-in initialize [:result :instructions])
                     "verification_complete=true")
                   "Server instructions do not teach terminal verification"
                   {:initialize initialize})
          (assert! (not (contains? capabilities :prompts))
                   "MCP must not advertise prompts" {:capabilities capabilities})
          (assert! (not (contains? capabilities :resources))
                   "MCP must not advertise resources" {:capabilities capabilities})
          (assert! (= ["inspect_clojure" "apply_clojure_changes" "edit_clojure"]
                      (mapv :name tools))
                   "MCP must expose exactly three structural tools" {:tools tools})
          (assert! (= false (get-in tools [0 :inputSchema :additionalProperties]))
                   "inspect_clojure schema must refuse unknown fields" {:tools tools})
          (assert! (= true (get-in tools [0 :annotations :readOnlyHint]))
                   "inspect_clojure must be annotated read-only" {:tools tools})
          (assert! (= false (get-in tools [1 :inputSchema :additionalProperties]))
                   "apply_clojure_changes schema must refuse unknown fields" {:tools tools})
          (assert! (= true (get-in invalid-call [:result :isError]))
                   "Invalid call must use the MCP error channel"
                   {:invalid-call invalid-call})
          (assert! (= "invalid-mcp-request" (:error_type refusal))
                   "Invalid call must preserve its stable refusal type"
                   {:refusal refusal})
          (assert! (= true (:source_unchanged refusal))
                   "Invalid call must prove that source was unchanged"
                   {:refusal refusal})
          (assert! (str/includes? refusal-text "refused · missing-fields")
                   "Invalid call must include a concise human-readable refusal"
                   {:refusal-text refusal-text})
          (println
            (pr-str
              {:ok true
               :operation :mcp-stdio-smoke
               :server "clj-surgeon"
               :tools ["inspect_clojure" "apply_clojure_changes" "edit_clojure"]
               :response-count 3
               :wall-ms (/ (double (- (System/nanoTime) started)) 1000000.0)}))))
      (finally
        (process/destroy-tree server)
        (deref stdout-future 5000 nil)
        (let [stderr (deref stderr-future 5000 "")]
          (when (and (string? stderr)
                     (not (str/blank? stderr)))
            (binding [*out* *err*]
              (println (str/trim stderr)))))))))

(apply -main *command-line-args*)
