(ns clj-surgeon.mcp-server
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-prepared-confirmation :as prepared-confirmation]
   [clj-surgeon.mcp-runtime :as runtime]
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
    "Batch known Clojure reads with inspect_clojure. Use prepare-change when one "
    "fully qualified Var names the goal but exact sites are unknown. Compile "
    "two or more exact edits or any cross-file decision into one "
    "apply_clojure_changes call. Do not repeat reads after read_complete=true or "
    "inspect writes after verification_complete=true."))

(defn- warn
  [& messages]
  (binding [*out* *err*]
    (apply println messages)))

(def ^:private outcome-classes-by-tool
  {"inspect_clojure"
   #{:read-success :prepared-basis :verification-pending
     :verification-complete :verification-failed :typed-refusal}
   "apply_clojure_changes"
   #{:committed :verification-pending :typed-refusal}
   "edit_clojure"
   #{:committed :typed-refusal}
   "transform_clojure"
   #{:preview :committed :typed-refusal}
   "relation_census"
   #{:read-success :typed-refusal}
   ;; @spec MCP-OP-ALIAS-001
   "alias_migration"
   #{:committed :typed-refusal}
   "admit_clojure_patch"
   #{:preview :committed :typed-refusal}})

;; @spec MCP-OP-COVERAGE-001
;; @spec MCP-OP-COVERAGE-002
(defn public-tool-registry
  "Return the canonical public registration entries for the active profile."
  []
  (mapv
    (fn [tool]
      (let [outcome-classes (or (:outcome-classes tool)
                                (get outcome-classes-by-tool (:name tool)))]
        (when-not outcome-classes
          (throw (ex-info "Public MCP tool lacks declared outcome classes"
                          {:tool (:name tool)})))
        (assoc tool :outcome-classes outcome-classes)))
    (mcp-tool/all-tools)))

(defn- registered-tools
  []
  (mapv #(dissoc % :outcome-classes) (public-tool-registry)))

(defn make-tools
  "clojure-mcp factory. Tool handlers are Vars so nREPL redefinition is live."
  [_nrepl-client-atom _working-dir]
  (registered-tools))

(def ^:private live-tool-state runtime/live-tool-state)

(defn tool-contract
  "Return only fields whose change requires a tools/list refresh."
  [tool]
  (select-keys tool
               [:name :description :schema :output-schema
                :annotations :structured?]))

(defn tool-contracts
  "Index stable, handler-free tool contracts by name."
  [tools]
  (into (sorted-map)
        (map (fn [tool] [(:name tool) (tool-contract tool)]))
        tools))

(defn tool-sync-plan
  "Pure add/replace/remove plan from registered contracts to desired tools."
  [registered desired-tools]
  (let [desired (tool-contracts desired-tools)
        desired-names (set (keys desired))]
    {:remove (->> (keys registered)
                  (remove desired-names)
                  sort
                  vec)
     :upsert (->> (keys desired)
                  (filter #(not= (get registered %) (get desired %)))
                  sort
                  vec)}))

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
                      exchange (contract/json-containers->clj arguments)
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

(defn- add-tool!
  [server tool]
  (.block (.addTool server (create-async-tool tool))))

(defn- remove-tool!
  [server tool-name]
  (.block (.removeTool server tool-name)))

(defn register-live-server!
  "Record one live SDK server and the contracts installed at construction."
  [server]
  (let [registered (tool-contracts (registered-tools))]
    (swap! live-tool-state
           (fn [state]
             (if (identical? server (:server state))
               (assoc state :registered registered)
               {:server server
                :registered registered
                :previous state})))
    {:ok true :status :registered :tool-count (count registered)}))

(defn unregister-live-server!
  "Forget server only when it is still the registered live instance."
  [server]
  (swap! live-tool-state
         (fn [state]
           (if (identical? server (:server state))
             (:previous state)
             state)))
  ;; @spec MCP-OP-PREP-ACT-002
  (prepared-confirmation/reset-registry!)
  {:ok true :status :unregistered})

(defn sync-tools!
  "Synchronize current tool contracts into the connected SDK server.

  Handler-only Var changes do not churn tools/list. SDK add/remove operations
  emit notifications/tools/list_changed because the server advertises that
  capability. A failed operation leaves the recorded registry at the last
  successfully installed state."
  []
  (if-let [{:keys [server registered]} @live-tool-state]
    (let [desired-tools (registered-tools)
          desired-by-name (into {} (map (juxt :name identity)) desired-tools)
          desired-contracts (tool-contracts desired-tools)
          {:keys [remove upsert]}
          (tool-sync-plan registered desired-tools)]
      (try
        (doseq [tool-name remove]
          (remove-tool! server tool-name)
          (swap! live-tool-state update :registered dissoc tool-name))
        (doseq [tool-name upsert]
          (add-tool! server (get desired-by-name tool-name))
          (swap! live-tool-state assoc-in
                 [:registered tool-name]
                 (get desired-contracts tool-name)))
        {:ok true
         :status :synchronized
         :removed remove
         :upserted upsert
         :tool-count (count desired-contracts)
         :before-contract-hash (format "%08x" (hash registered))
         :after-contract-hash (format "%08x" (hash desired-contracts))
         :server-restart-required false
         :agent-session-restart :client-dependent}
        (catch Exception error
          {:ok false
           :status :sync-failed
           :removed remove
           :upserted upsert
           :registered (vec (keys (:registered @live-tool-state)))
           :error (.getMessage error)
           :remedy "Fix the reload error and run sync-tools! again."})))
    {:ok false
     :status :server-unavailable
     :error "No live clj-surgeon MCP server is registered"
     :remedy "Start the MCP server before synchronizing tool contracts."}))

(defn configure-specification
  "Attach the complete minimal clj-surgeon contract to an MCP server builder."
  [specification]
  (-> specification
      (.serverInfo "clj-surgeon" "experimental")
      (.instructions server-instructions)
      (.capabilities
        (-> (McpSchema$ServerCapabilities/builder)
            (.tools true)
            (.build)))
      (.tools (mapv create-async-tool (registered-tools)))))

(defn build-stdio-server
  "Build the minimal protocol surface: one tool capability and no others."
  []
  (let [transport (StdioServerTransportProvider. (McpJsonMapper/getDefault))
        specification (configure-specification (McpServer/async transport))]
    (doto (.build specification)
      (register-live-server!))))

(defn start-embedded-nrepl!
  "Start an nREPL inside the live MCP JVM. Failure never blocks the MCP server."
  [port port-file]
  (try
    (let [server (nrepl-server/start-server :port (or port 0))]
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
  :tool-profile   :full (default) or :edit
  :nrepl-port     0 for ephemeral development nREPL; :none disables it
  :port-file      embedded nREPL discovery file (default .nrepl-port)
  :log-file       clojure-mcp diagnostic log"
  [{:keys [project-dir receipt-dir telemetry-dir run-id tool-profile nrepl-port
           port-file log-file]
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
                     :tool-profile (normalize-option tool-profile :full)
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
