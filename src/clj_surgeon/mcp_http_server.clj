(ns clj-surgeon.mcp-http-server
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-formatter :as mcp-formatter]
   [clj-surgeon.mcp-hot-verify :as hot-verify]
   [clj-surgeon.mcp-prepared-confirmation :as prepared-confirmation]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure-mcp.logging :as mcp-logging]
   [clojure.edn :as edn]
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

(def default-verification-profiles
  {"fast"
   {:commands
    [["clj-kondo" "--lint" "{files}"]
     ["npx" "@chrisoakman/standard-clojure-style" "check" "{files}"]]}
   "full" {:cold {:command ["make" "test"]
                  :timeout-ms 1200000}}})

(def default-formatter mcp-formatter/default-command)

(defn formatter-from-config
  "Return one closed formatter command from project data."
  [config]
  (let [formatter (or (:formatter config) default-formatter)]
    (when-not (and (vector? formatter)
                   (seq formatter)
                   (every? #(and (string? %) (seq %)) formatter)
                   (some #{"{files}"} formatter))
      (throw (ex-info "Invalid project formatter"
                      {:error-type :invalid-project-formatter})))
    formatter))

(defn- verification-command?
  [command]
  (and (vector? command)
       (seq command)
       (every? string? command)))

(defn- verification-profile?
  [profile]
  (or (verification-command? profile)
      (and (map? profile)
           (seq profile)
           (every? #{:commands :hot :cold} (keys profile))
           (or (nil? (:commands profile))
               (and (vector? (:commands profile))
                    (seq (:commands profile))
                    (every? verification-command? (:commands profile))))
           (or (nil? (:hot profile))
               (hot-verify/valid-profile? (:hot profile)))
           (or (nil? (:cold profile))
               (cold-verify/valid-profile? (:cold profile))))))

(defn- exact-verification-profile?
  [profile]
  (and (map? profile)
       (= #{:acceptance :timeout-ms :commands} (set (keys profile)))
       (= :exact-exit (:acceptance profile))
       (integer? (:timeout-ms profile))
       (<= 1 (:timeout-ms profile) 120000)
       (vector? (:commands profile))
       (= 1 (count (:commands profile)))
       (let [command (first (:commands profile))]
         (and (verification-command? command)
              (not-any? #{"{files}"} command)))))

(defn verification-profiles-from-config
  "Return a validated closed verification-profile map from project data."
  [config]
  (when-let [profiles (:verification-profiles config)]
    (when-not (and (map? profiles)
                   (seq profiles)
                   (every? string? (keys profiles))
                   (every? (fn [[profile-name profile]]
                             (if (= "exact" profile-name)
                               (exact-verification-profile? profile)
                               (verification-profile? profile)))
                           profiles))
      (throw
        (ex-info "Invalid project verification profiles"
                 {:error-type :invalid-project-verification-profiles})))
    profiles))

(defn resolve-verification-profiles
  "Select explicit, project-owned, or built-in closed verification profiles."
  [explicit project-config]
  (cond
    explicit
    {:profiles (or (verification-profiles-from-config
                     {:verification-profiles explicit})
                   default-verification-profiles)
     :source :process}

    (:verification-profiles project-config)
    {:profiles (verification-profiles-from-config project-config)
     :source :project}

    :else
    {:profiles default-verification-profiles
     :source :built-in}))

(defn- read-project-config
  [project-dir]
  (let [config-file (io/file project-dir ".clj-surgeon.edn")]
    (if-not (.isFile config-file)
      {}
      (try
        (edn/read-string (slurp config-file))
        (catch Exception error
          (throw
            (ex-info "Invalid .clj-surgeon.edn"
                     {:error-type :invalid-project-verification-profiles
                      :file (.getPath config-file)}
                     error)))))))

(defn workspace-context
  "Build one root-specific context extension. Verification profiles are read
   lazily on every basis application so one workspace cannot inherit another
   root's configuration and an intentional config change does not require a
   server restart."
  [verification-profiles workspace-root]
  {:verification-profile-selection-fn
   (fn []
     (resolve-verification-profiles
       verification-profiles
       (read-project-config workspace-root)))
   :verification-profiles-fn
   (fn []
     (:profiles
       (resolve-verification-profiles
         verification-profiles
         (read-project-config workspace-root))))
   :formatter-fn
   (fn []
     (formatter-from-config (read-project-config workspace-root)))})

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
          (try
            (.doFilter chain request response)
            (finally
              (when (= "DELETE" (.getMethod ^HttpServletRequest request))
                ;; @spec MCP-OP-PREP-ACT-002
                ;; @spec MCP-OP-PREP-ACT-004
                (when-let [session-id
                           (.getHeader ^HttpServletRequest request
                                       "Mcp-Session-Id")]
                  (prepared-confirmation/end-session!
                    prepared-confirmation/process-registry session-id)))))
          (.sendError ^HttpServletResponse response 403
                      "Cross-origin MCP requests are forbidden"))))))

(defn- health-servlet
  []
  (proxy [HttpServlet] []
    (doGet [^HttpServletRequest _request ^HttpServletResponse response]
      (let [readiness (runtime/readiness)]
        (.setStatus response (if (:ok readiness) 200 503))
        (.setContentType response "application/json")
        (.setCharacterEncoding response "UTF-8")
        (doto (.getWriter response)
          (.write (json/generate-string readiness))
          (.flush))))))

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
           focused-test
           semantic-resolver verify! read-source write-source!]
    telemetry-mode :telemetry}]
  (let [project-dir (str (or project-dir (System/getProperty "user.dir")))
        project-config (read-project-config project-dir)
        verification-selection
        (resolve-verification-profiles verification-profiles project-config)
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
                           ;; @spec MCP-OP-ADMIT-081
                           ;; The admission gate's focused-test profile, from
                           ;; the -X start map. Absent here, each workspace's
                           ;; own .clj-surgeon/focused-test.edn is the fallback.
                           :focused-test focused-test
                           :cclsp-url cclsp-url
                           :semantic-resolver semantic-resolver
                           :verify! verify!
                           :read-source read-source
                           :write-source! write-source!
                           :verification-profiles (:profiles verification-selection)
                           :verification-profile-source
                           (:source verification-selection)
                           :workspace-context-factory
                           (fn [workspace-root]
                             (workspace-context verification-profiles
                                                workspace-root))})
        transport (-> (HttpServletStreamableServerTransportProvider/builder)
                      (.jsonMapper (McpJsonMapper/getDefault))
                      (.mcpEndpoint endpoint)
                      (.build))
        mcp (-> (McpServer/async transport)
                (mcp-server/configure-specification)
                (.build))
        _registered (mcp-server/register-live-server! mcp)
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
                       :project-root project-dir
                       :verification-profile-source
                       (:source verification-selection)}]
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
      (when mcp (mcp-server/unregister-live-server! mcp))
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
