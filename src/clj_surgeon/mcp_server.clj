(ns clj-surgeon.mcp-server
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure-mcp.core :as mcp-core]
   [clojure-mcp.logging :as mcp-logging]
   [clojure.java.io :as io]
   [nrepl.server :as nrepl-server])
  (:import
   (io.modelcontextprotocol.json McpJsonMapper)
   (io.modelcontextprotocol.server McpServer McpServerFeatures$AsyncToolSpecification)
   (io.modelcontextprotocol.server.transport StdioServerTransportProvider)
   (io.modelcontextprotocol.spec McpSchema$CallToolResult McpSchema$ServerCapabilities McpSchema$Tool McpSchema$ToolAnnotations)
   (java.io FileDescriptor FileOutputStream PrintStream PrintWriter)
   (java.lang.management ManagementFactory)
   (reactor.core.publisher Mono)))

(def default-log-file
  (str (io/file (System/getProperty "user.home")
                ".local" "state" "clj-surgeon" "mcp-server.log")))

(def server-instructions
  (str
    "PREFER inspect_clojure when several known Clojure structural questions can "
    "be batched. It reads each distinct file once and returns ordered hash-bound "
    "evidence; after read_complete=true, do not repeat or split the read. PREFER "
    "apply_clojure_changes over apply_patch for 2+ exact replacements or edits "
    "spanning files. If every file, owner, before form, replacement, and count "
    "is known, do not read first. After verification_complete=true, do not "
    "reread or diff unless explicitly requested."))

(defn- warn
  [& messages]
  (binding [*out* *err*]
    (apply println messages)))

(defn make-tools
  "clojure-mcp factory. Tool handlers are Vars so nREPL redefinition is live."
  [_nrepl-client-atom _working-dir]
  (mcp-tool/all-tools))

(defn- structured-call-result
  [json-mapper content error? structured]
  (-> (McpSchema$CallToolResult/builder)
      (.textContent content)
      (.isError (boolean error?))
      (.structuredContent json-mapper (json/generate-string structured))
      (.build)))

(defn create-structured-async-tool
  "Create one SDK-native tool with annotations and structuredContent support."
  [{:keys [name description schema output-schema annotations tool-fn]}]
  (let [json-mapper (McpJsonMapper/getDefault)
        annotation-record
        (McpSchema$ToolAnnotations.
          (:title annotations)
          (:read-only annotations)
          (:destructive annotations)
          (:idempotent annotations)
          (:open-world annotations)
          (:return-direct annotations))
        builder (-> (McpSchema$Tool/builder)
                    (.name name)
                    (.description description)
                    (.inputSchema json-mapper (json/generate-string schema))
                    (.annotations annotation-record))
        _ (when output-schema
            (.outputSchema builder json-mapper
                           (json/generate-string output-schema)))
        mcp-tool (.build builder)
        handler
        (reify java.util.function.BiFunction
          (apply [_ exchange arguments]
            (Mono/create
              (reify java.util.function.Consumer
                (accept [_ sink]
                  (try
                    (tool-fn
                      exchange arguments
                      (fn [content error? structured]
                        (.success sink
                                  (structured-call-result
                                    json-mapper content error? structured))))
                    (catch Exception error
                      (let [failure {:ok false
                                     :operation name
                                     :error_type "mcp-adapter-failure"
                                     :error (.getMessage error)}]
                        (.success sink
                                  (structured-call-result
                                    json-mapper
                                    [(json/generate-string failure)]
                                    true failure))))))))))]
    (McpServerFeatures$AsyncToolSpecification. mcp-tool handler)))

(defn- create-async-tool
  [tool]
  (if (:structured? tool)
    (create-structured-async-tool tool)
    (mcp-core/create-async-tool tool)))

(defn configure-specification
  "Attach the complete minimal clj-surgeon contract to an MCP server builder."
  [specification]
  (-> specification
      (.serverInfo "clj-surgeon" "experimental")
      (.instructions server-instructions)
      (.capabilities
        (-> (McpSchema$ServerCapabilities/builder)
            (.tools false)
            (.build)))
      (.tools (mapv create-async-tool (mcp-tool/all-tools)))))

(defn build-stdio-server
  "Build the minimal protocol surface: one tool capability and no others."
  []
  (let [transport (StdioServerTransportProvider. (McpJsonMapper/getDefault))
        specification (configure-specification (McpServer/async transport))]
    (.build specification)))

(defn start-embedded-nrepl!
  "Start an nREPL inside the live MCP JVM. Failure never blocks the MCP server."
  [port port-file]
  (try
    (let [handler @(requiring-resolve 'cider.nrepl/cider-nrepl-handler)
          server (nrepl-server/start-server :port (or port 0)
                                            :handler handler)]
      (spit port-file (:port server))
      (warn "clj-surgeon MCP: embedded nREPL on" (:port server)
            "(" port-file ")")
      server)
    (catch Exception error
      (warn "clj-surgeon MCP: embedded nREPL failed —" (.getMessage error))
      nil)))

(defn armor-stdout!
  "Redirect later JVM and Clojure stdout to stderr after stdio transport capture."
  []
  (let [err (PrintStream. (FileOutputStream. FileDescriptor/err) true)]
    (System/setOut err)
    (alter-var-root #'*out* (constantly (PrintWriter. err true)))
    err))

(defn- normalize-option
  [value default]
  (if (nil? value) default value))

(defn start
  "Start the persistent apply_clojure_changes stdio MCP server and block.

  :project-dir    source project root (default user.dir)
  :receipt-dir    durable inverse-receipt directory
  :telemetry      off, metrics, or full (default metrics)
  :telemetry-dir  local JSONL directory outside repositories
  :run-id         optional benchmark correlation ID
  :nrepl-port     0 for ephemeral development nREPL; :none disables it
  :port-file      embedded nREPL discovery file (default .nrepl-port)
  :log-file       clojure-mcp diagnostic log"
  [{:keys [project-dir receipt-dir telemetry-dir run-id nrepl-port port-file
           log-file]
    telemetry-mode :telemetry}]
  (let [project-dir (str (normalize-option project-dir
                                           (System/getProperty "user.dir")))
        telemetry-state
        (telemetry/start! {:mode (normalize-option telemetry-mode :metrics)
                           :directory telemetry-dir
                           :run-id run-id})
        started-ms (.getUptime (ManagementFactory/getRuntimeMXBean))
        port-file (str (normalize-option port-file
                                         (io/file project-dir ".nrepl-port")))
        nrepl (when-not (= nrepl-port :none)
                (start-embedded-nrepl! nrepl-port port-file))]
    (mcp-logging/configure-logging!
      {:log-file (str (normalize-option log-file default-log-file))
       :enable-logging? true
       :log-level :info})
    (mcp-tool/init! {:project-root project-dir
                     :receipt-dir receipt-dir
                     :telemetry telemetry-state})
    (build-stdio-server)
    (armor-stdout!)
    (telemetry/emit!
      telemetry-state :server.start
      (cond->
        {:version "experimental"
         :jvm_uptime_ms started-ms
         :mcp_ready_ms (.getUptime (ManagementFactory/getRuntimeMXBean))
         :nrepl_port (:port nrepl)}
        (= :full (:mode telemetry-state))
        (assoc :project_root project-dir)))
    (warn "clj-surgeon MCP: ready — telemetry" (name (:mode telemetry-state)))
    @(promise)))
