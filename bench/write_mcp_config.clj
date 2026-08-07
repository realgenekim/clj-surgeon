(ns write-mcp-config
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn toml-string
  [value]
  (str "\""
       (str/escape (str value)
                   {\\ "\\\\"
                    \" "\\\""
                    \newline "\\n"
                    \return "\\r"
                    \tab "\\t"})
       "\""))

(defn config-source
  [{:keys [url server-root project-root telemetry-dir run-id]}]
  (str
    "[mcp_servers.clj-surgeon]\n"
    (if url
      (str "url = " (toml-string url) "\n")
      (str
        "command = \"clojure\"\n"
        "cwd = " (toml-string server-root) "\n"
        "args = ["
        (str/join ", "
                  (map toml-string
                       ["-X:clj-surgeon/mcp-stdio"
                        ":project-dir" (pr-str project-root)
                        ":telemetry" ":full"
                        ":telemetry-dir" (pr-str telemetry-dir)
                        ":run-id" (pr-str run-id)
                        ":nrepl-port" ":none"]))
        "]\n"))
    "required = true\n"
    "enabled_tools = [\"apply_clojure_changes\"]\n"
    "default_tools_approval_mode = \"approve\"\n"
    "startup_timeout_sec = 5\n"
    "tool_timeout_sec = 45\n"))

(defn write-config!
  [path opts]
  (let [target (io/file path)]
    (.mkdirs (.getParentFile (.getAbsoluteFile target)))
    (spit target (config-source opts))
    {:ok true :operation :write-mcp-config :file (.getCanonicalPath target)}))

(defn self-test!
  []
  (let [stdio-source
        (config-source
          {:server-root "/tmp/server with space"
           :project-root "/tmp/project\"quoted"
           :telemetry-dir "/tmp/telemetry"
           :run-id "r01"})
        http-source (config-source {:url "http://127.0.0.1:41234/mcp"})]
    (assert (str/includes? stdio-source "[mcp_servers.clj-surgeon]"))
    (assert (str/includes? stdio-source "cwd = \"/tmp/server with space\""))
    (assert (str/includes? stdio-source "\\\"quoted\\\""))
    (assert (str/includes? stdio-source "\":nrepl-port\", \":none\""))
    (assert (str/includes? http-source
                           "url = \"http://127.0.0.1:41234/mcp\""))
    (assert (not (str/includes? http-source "command =")))
    (assert (str/includes? http-source "required = true"))
    (assert (str/includes? http-source
                           "enabled_tools = [\"apply_clojure_changes\"]"))
    (assert (str/includes? http-source
                           "default_tools_approval_mode = \"approve\""))
    (assert (str/includes? http-source "startup_timeout_sec = 5"))
    (println {:ok true :operation :write-mcp-config-self-test})))

(defn -main
  [& args]
  (if (= ["--self-test"] args)
    (self-test!)
    (let [[path server-root project-root telemetry-dir run-id] args
          http? (= "--url" server-root)]
      (when-not (or (= 5 (count args))
                    (and http? (= 3 (count args))))
        (throw
          (ex-info
            (str "Usage: bb bench/write_mcp_config.clj CONFIG "
                 "SERVER_ROOT PROJECT_ROOT TELEMETRY_DIR RUN_ID\n"
                 "   or: bb bench/write_mcp_config.clj CONFIG --url HTTP_URL")
            {:error-type :invalid-arguments :actual (count args)})))
      (println
        (pr-str
          (write-config! path
                         (if http?
                           {:url project-root}
                           {:server-root server-root
                            :project-root project-root
                            :telemetry-dir telemetry-dir
                            :run-id run-id})))))))

(apply -main *command-line-args*)
