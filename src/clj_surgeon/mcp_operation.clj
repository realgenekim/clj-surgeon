(ns clj-surgeon.mcp-operation
  (:require
   [cheshire.core :as json]
   [clj-surgeon.measured :as measured])
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
    ;; @spec MCP-OP-TIME-005
    ;; THE publication boundary for the measured partition. Every public MCP
    ;; result passes through here, so this is the one place that can make "a
    ;; measured field enters a receipt only through the partition" true by
    ;; CONSTRUCTION rather than by every producer remembering. Code inside an
    ;; operation may carry a clock reading in whatever shape suits it; what it
    ;; may not do is publish one beside the partition instead of inside it.
    (-> domain-result
        measured/partition-measured
        (measured/attach {:elapsed_ms elapsed-ms}))))

(def measured-output-schema
  "The public JSON shape of the measured partition.

  Every clock-derived field a public MCP result publishes lives here, so a
  caller reading the wire sees the same partition the receipts do: what is
  under `measured` was read from a clock, and nothing under it belongs in a
  hash, a byte-identity comparison, or a parity reference."
  {:type "object"
   :properties {"elapsed_ms" {:type "number" :minimum 0}
                "job_elapsed_ms" {:type "number" :minimum 0}
                "inspection_elapsed_ms" {:type "number" :minimum 0}
                "scan_ms" {:type "number" :minimum 0}}
   :required ["elapsed_ms"]})

(defn measured-field
  "One measured field of a public MCP result.

  Reads the partition. Tolerates a bare top-level value because several budget
  and byte-count checks build a throwaway result with a zeroed clock BEFORE
  publication, and those maps never reach a caller. Producing an unpartitioned
  measured field is what `clj-surgeon.measured-invariant-test` forbids; reading
  one tolerantly costs nothing and keeps the pre-publication checks honest."
  [result k]
  (or (measured/field result k) (get result k)))

(defn elapsed-ms
  "The request clock of one public MCP result."
  [result]
  (measured-field result :elapsed_ms))

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
  [{:keys [clock-nanos execute summarize serialize callback]
    :or {clock-nanos #(System/nanoTime)
         serialize json/generate-string}}]
  (let [started-ns (clock-nanos)
        domain-result (execute)
        finished-ns (clock-nanos)
        result (finalize-result domain-result started-ns finished-ns)
        summary (summarize result)
        body (serialize result)]
    (callback [summary] (not (:ok result)) result)
    body))
