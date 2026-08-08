(ns clj-surgeon.mcp-semantic-client
  "Persistent, restart-safe client for the local cclsp semantic provider."
  (:require
   [cheshire.core :as json])
  (:import
   (io.modelcontextprotocol.client McpClient McpSyncClient)
   (io.modelcontextprotocol.client.transport HttpClientStreamableHttpTransport)
   (io.modelcontextprotocol.spec McpSchema$CallToolRequest)
   (java.net URI)
   (java.time Duration)))

(def default-url "http://127.0.0.1:7890/mcp")
(defonce runtime (atom {:url default-url :client nil}))

(defn- endpoint-parts
  [url]
  (let [uri (URI. url)
        port (.getPort uri)
        authority (str (.getScheme uri) "://" (.getHost uri)
                       (when (pos? port) (str ":" port)))]
    {:base-url authority
     :endpoint (let [path (.getPath uri)]
                 (if (seq path) path "/mcp"))}))

(defn- connect-client
  [url]
  (let [{:keys [base-url endpoint]} (endpoint-parts url)
        transport (-> (HttpClientStreamableHttpTransport/builder base-url)
                      (.endpoint endpoint)
                      (.connectTimeout (Duration/ofSeconds 3))
                      (.build))
        client (-> (McpClient/sync transport)
                   (.initializationTimeout (Duration/ofSeconds 5))
                   (.requestTimeout (Duration/ofSeconds 40))
                   (.build))]
    (.initialize client)
    client))

(defn close!
  []
  (when-let [^McpSyncClient client (:client @runtime)]
    (try
      (.closeGracefully client)
      (catch Exception _)))
  (swap! runtime assoc :client nil))

(defn init!
  "Set the provider URL. Preserve an already-hot client when the URL is unchanged."
  [{:keys [url]}]
  (let [url (or url default-url)]
    (when (not= url (:url @runtime))
      (close!))
    (swap! runtime assoc :url url)
    {:url url :connected (boolean (:client @runtime))}))

(defn- client!
  []
  (or (:client @runtime)
      (locking runtime
        (or (:client @runtime)
            (let [client (connect-client (:url @runtime))]
              (swap! runtime assoc :client client)
              client)))))

(defn normalize-result
  "Turn an SDK CallToolResult into the keyword-keyed provider contract."
  [result]
  (let [structured (.structuredContent result)]
    (cond
      (true? (.isError result))
      {:ok false
       :error-type :semantic-provider-refusal
       :error (some-> result .content first str)}

      structured
      (assoc (json/parse-string (json/generate-string structured) true) :ok true)

      :else
      {:ok false
       :error-type :semantic-provider-contract-missing
       :error "cclsp returned no structuredContent for resolve_var_surface"})))

(defn- call-resolve-var!
  [^McpSyncClient client workspace-root qualified-var source-anchor]
  (normalize-result
    (.callTool client
               (McpSchema$CallToolRequest.
                 "resolve_var_surface"
                 (cond-> {"var" qualified-var
                          "include_declaration" true}
                   workspace-root (assoc "workspace_root" workspace-root)
                   source-anchor (assoc "source_anchor"
                                        (json/parse-string
                                          (json/generate-string source-anchor))))))))

(defn resolve-var!
  "Resolve one fully qualified Clojure Var. Reconnect once after a provider restart."
  ([qualified-var]
   (resolve-var! nil qualified-var nil))
  ([workspace-root qualified-var]
   (resolve-var! workspace-root qualified-var nil))
  ([workspace-root qualified-var source-anchor]
   (try
     (call-resolve-var! (client!) workspace-root qualified-var source-anchor)
     (catch Exception first-error
       (close!)
       (try
         (call-resolve-var! (client!) workspace-root qualified-var source-anchor)
         (catch Exception retry-error
           {:ok false
            :error-type :semantic-provider-unavailable
            :error (.getMessage retry-error)
            :first-error (.getMessage first-error)
            :remedy (str "Start or repair cclsp at " (:url @runtime)
                         ", then retry the same inspect_clojure call.")}))))))
