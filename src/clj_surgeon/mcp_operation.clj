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
  expose a partial public result."
  [{:keys [clock-nanos execute fit summarize serialize callback]
    :or {clock-nanos #(System/nanoTime)
         fit identity
         serialize json/generate-string}}]
  (let [started-ns (clock-nanos)
        domain-result (execute)
        finished-ns (clock-nanos)
        ;; @spec MCP-OP-STUDY-040
        ;; The FIT sees the FINALIZED result — the envelope included — and
        ;; nothing is added to what it returns. A fit that runs inside
        ;; `execute` measures a result the publisher has not finished
        ;; building, and the difference has to be covered by a reserve; a
        ;; reserve is a constant taken from one observation, and Sol's O2
        ;; round-3 review section 5 escaped it with an accepted clock value.
        ;; Placing the fit here removes the difference instead of estimating
        ;; it, and it holds for whatever shape the envelope takes.
        result (fit (finalize-result domain-result started-ns finished-ns))
        summary (summarize result)
        body (serialize result)]
    (callback [summary] (not (:ok result)) result)
    body))
