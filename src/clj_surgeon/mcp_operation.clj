(ns clj-surgeon.mcp-operation
  (:require
   [cheshire.core :as json])
  (:import
   (java.util Locale)))

(defn- finite-non-negative?
  [value]
  (and (number? value)
       (Double/isFinite (double value))
       (not (neg? (double value)))))

;; @spec MCP-OP-TIME-003
(defn format-elapsed-ms
  "Render one public MCP duration with stable locale-independent spelling."
  [elapsed-ms]
  (when-not (finite-non-negative? elapsed-ms)
    (throw (ex-info "MCP elapsed time must be finite and non-negative"
                    {:error-type :invalid-mcp-elapsed-time
                     :elapsed-ms elapsed-ms})))
  (String/format Locale/ROOT
                 "%.2f ms"
                 (object-array [(double elapsed-ms)])))

(defn- finalize-result
  [domain-result started-ns finished-ns]
  (when-not (map? domain-result)
    (throw (ex-info "MCP domain execution must return a map"
                    {:error-type :invalid-mcp-operation-result
                     :result-type (some-> domain-result class .getName)})))
  (let [elapsed-ms (/ (- (double finished-ns) (double started-ns))
                      1000000.0)]
    (when-not (finite-non-negative? elapsed-ms)
      (throw (ex-info "MCP request clock produced an invalid interval"
                      {:error-type :invalid-mcp-elapsed-time
                       :started-ns started-ns
                       :finished-ns finished-ns
                       :elapsed-ms elapsed-ms})))
    (assoc domain-result :elapsed_ms elapsed-ms)))

;; @spec MCP-OP-RESULT-001
;; @spec MCP-OP-RESULT-002
;; @spec MCP-OP-RESULT-003
;; @spec MCP-OP-RESULT-004
;; @spec MCP-OP-RESULT-005
;; @spec MCP-OP-RESULT-006
;; @spec MCP-OP-TIME-001
;; @spec MCP-OP-TIME-002
(defn invoke!
  "Execute and publish one public MCP operation through a single finalizer.

  The request clock surrounds domain execution only. Summary rendering and
  serialization both complete before callback publication, so failures cannot
  expose a partial public result.

  The optional `guard` runs at the FINALIZATION POINT, on the exact result and
  summary publication would hand to the callback -- timing fields already
  finalized, summary already rendered. It returns nil to publish that
  candidate, or a replacement result to publish instead; the replacement is
  re-summarized and nothing after the guard can grow the envelope. A guard
  that will not accept its own replacement is a defect in the guard, so the
  replacement is published once and not re-guarded."
  [{:keys [clock-nanos execute summarize serialize callback guard]
    :or {clock-nanos #(System/nanoTime)
         serialize json/generate-string}}]
  (let [started-ns (clock-nanos)
        domain-result (execute)
        finished-ns (clock-nanos)
        finalized (finalize-result domain-result started-ns finished-ns)
        finalized-summary (summarize finalized)
        replacement (when guard (guard finalized finalized-summary))
        result (or replacement finalized)
        summary (if replacement (summarize result) finalized-summary)
        body (serialize result)]
    (callback [summary] (not (:ok result)) result)
    body))
