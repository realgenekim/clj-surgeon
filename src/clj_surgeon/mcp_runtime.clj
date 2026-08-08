(ns clj-surgeon.mcp-runtime)

;; Keep process-lifetime state outside hot-reloaded handler namespaces. A
;; handler reload may replace Vars, but it must never disarm the live server.
(defonce tool-config (atom nil))
(defonce live-tool-state (atom nil))

(defn readiness
  []
  (let [configured? (some? @tool-config)
        registered? (some? (:server @live-tool-state))]
    {:ok (and configured? registered?)
     :server "clj-surgeon"
     :tool_runtime (if configured? "ready" "not-initialized")
     :tool_registry (if registered? "ready" "not-registered")}))
