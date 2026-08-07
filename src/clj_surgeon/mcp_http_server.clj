(ns clj-surgeon.mcp-http-server
  (:require
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure-mcp.logging :as mcp-logging]
   [clojure.java.io :as io]
   [nrepl.server :as nrepl-server])
  (:import
   (io.modelcontextprotocol.json McpJsonMapper)
   (io.modelcontextprotocol.server McpServer)
   (io.modelcontextprotocol.server.transport HttpServletStreamableServerTransportProvider)
   (jakarta.servlet DispatcherType Filter FilterChain ServletRequest ServletResponse)
   (jakarta.servlet.http HttpServlet HttpServletRequest HttpServletResponse)
   (java.lang.management ManagementFactory)
   (java.net InetSocketAddress)
   (java.util EnumSet)
   (org.eclipse.jetty.ee10.servlet ServletContextHandler)
   (org.eclipse.jetty.server Server ServerConnector)))

(def default-host "127.0.0.1")
(def default-port 7888)
(def default-endpoint "/mcp")

(defn allowed-origin?
  "Allow non-browser clients and loopback browser origins only."
  [origin]
  (or (nil? origin)
      (boolean
        (re-matches
          #"https?://(?:localhost|127\.0\.0\.1|\[::1\])(?::[0-9]+)?"
          origin))))

(defn- origin-filter
  []
  (proxy [Filter] []
    (init [_filter-config])
    (destroy [])
    (doFilter [^ServletRequest request
               ^ServletResponse response
               ^FilterChain chain]
      (let [origin (.getHeader ^HttpServletRequest request "Origin")]
        (if (allowed-origin? origin)
          (.doFilter chain request response)
          (.sendError ^HttpServletResponse response 403
                      "Cross-origin MCP requests are forbidden"))))))

(defn- health-servlet
  []
  (proxy [HttpServlet] []
    (doGet [^HttpServletRequest _request ^HttpServletResponse response]
      (.setStatus response 200)
      (.setContentType response "application/json")
      (.setCharacterEncoding response "UTF-8")
      (doto (.getWriter response)
        (.write "{\"ok\":true,\"server\":\"clj-surgeon\"}")
        (.flush)))))

(defn- configure-logging!
  [log-file]
  (mcp-logging/configure-logging!
    {:log-file (str (or log-file mcp-server/default-log-file))
     :enable-logging? true
     :log-level :info}))

(defn- write-ready-file!
  [ready-file readiness]
  (when ready-file
    (let [target (io/file ready-file)
          parent (.getParentFile (.getAbsoluteFile target))]
      (.mkdirs parent)
      (spit target (str (pr-str readiness) "\n")))))

(defn start-http-server!
  "Start one nonblocking, loopback-only, repository-scoped MCP server."
  [{:keys [project-dir receipt-dir telemetry-dir run-id port ready-file
           nrepl-port port-file log-file cclsp-url verification-profiles
           semantic-resolver verify! read-source write-source!]
    telemetry-mode :telemetry}]
  (let [project-dir (str (or project-dir (System/getProperty "user.dir")))
        host default-host
        port (int (or port default-port))
        endpoint default-endpoint
        telemetry-state
        (telemetry/start! {:mode (or telemetry-mode :metrics)
                           :directory telemetry-dir
                           :run-id run-id})
        port-file (str (or port-file (io/file project-dir ".nrepl-port")))
        nrepl (when-not (= nrepl-port :none)
                (mcp-server/start-embedded-nrepl! nrepl-port port-file))
        _ (configure-logging! log-file)
        _ (mcp-tool/init! {:project-root project-dir
                           :receipt-dir receipt-dir
                           :telemetry telemetry-state
                           :cclsp-url cclsp-url
                           :semantic-resolver semantic-resolver
                           :verify! verify!
                           :read-source read-source
                           :write-source! write-source!
                           :verification-profiles
                           (or verification-profiles
                               {"fast"
                                {:commands
                                 [["clj-kondo" "--lint" "{files}"]
                                  ["npx" "@chrisoakman/standard-clojure-style"
                                   "check" "{files}"]]}
                                "full" ["make" "test"]})})
        transport (-> (HttpServletStreamableServerTransportProvider/builder)
                      (.jsonMapper (McpJsonMapper/getDefault))
                      (.mcpEndpoint endpoint)
                      (.build))
        mcp (-> (McpServer/async transport)
                (mcp-server/configure-specification)
                (.build))
        jetty (Server. (InetSocketAddress. host port))
        context (ServletContextHandler. "/")]
    (.addServlet context transport endpoint)
    (.addServlet context (health-servlet) "/healthz")
    (.addFilter context (origin-filter) "/*"
                (EnumSet/of DispatcherType/REQUEST))
    (.setHandler jetty context)
    (.setStopAtShutdown jetty true)
    (try
      (.start jetty)
      (let [connector ^ServerConnector (first (.getConnectors jetty))
            actual-port (.getLocalPort connector)
            url (str "http://" host ":" actual-port endpoint)
            readiness {:ok true
                       :server :clj-surgeon
                       :transport :streamable-http
                       :host host
                       :port actual-port
                       :url url
                       :pid (.pid (java.lang.ProcessHandle/current))
                       :project-root project-dir}]
        (write-ready-file! ready-file readiness)
        (telemetry/emit!
          telemetry-state :server.start
          (cond->
            {:version "experimental"
             :transport "streamable-http"
             :host host
             :port actual-port
             :mcp_ready_ms (.getUptime (ManagementFactory/getRuntimeMXBean))
             :nrepl_port (:port nrepl)}
            (= :full (:mode telemetry-state))
            (assoc :project_root project-dir)))
        (assoc readiness
               :jetty jetty
               :mcp mcp
               :transport-provider transport
               :nrepl nrepl
               :telemetry telemetry-state
               :ready-file ready-file))
      (catch Exception error
        (when nrepl (nrepl-server/stop-server nrepl))
        (.close mcp)
        (throw error)))))

(defn stop-http-server!
  [{:keys [^Server jetty mcp nrepl telemetry ready-file]}]
  (try
    (telemetry/emit! telemetry :server.stop {})
    (finally
      (when mcp (.close mcp))
      (when jetty (.stop jetty))
      (when nrepl (nrepl-server/stop-server nrepl))
      (when ready-file
        (java.nio.file.Files/deleteIfExists (.toPath (io/file ready-file)))))))

(defn start
  "Start the persistent Streamable HTTP MCP server and block until stopped."
  [opts]
  (let [running (start-http-server! opts)]
    (binding [*out* *err*]
      (println "clj-surgeon MCP: persistent server ready on" (:url running)))
    (.join ^Server (:jetty running))))
