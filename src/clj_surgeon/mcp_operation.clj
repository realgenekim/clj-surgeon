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

(defn finalize-result
  "THE publication boundary for the measured partition.

  Every public MCP result passes through here, so this is the one place that
  can make \"a measured field enters a receipt only through the partition\" true
  by CONSTRUCTION rather than by every producer remembering. Code inside an
  operation may carry a clock reading in whatever shape suits it; what it may
  not do is publish one beside the partition instead of inside it.

  `elapsed` is the authoritative request clock: a `measured` reading or a bare
  number. PUBLIC, because it is the boundary — a failure raised outside
  `invoke!` (the SDK adapter's own catch, Sol review 2026-09-04 §2) publishes
  through this function rather than building a result of its own."
  [domain-result elapsed]
  (when-not (map? domain-result)
    (throw (ex-info "MCP domain execution must return a map"
                    {:error-type :invalid-mcp-operation-result
                     :result-type (some-> domain-result class .getName)})))
  (let [elapsed-ms (measured/value elapsed)]
    (when-not (finite-non-negative? elapsed-ms)
      (throw (ex-info "MCP request clock produced an invalid interval"
                      {:error-type :invalid-mcp-elapsed-time
                       :elapsed-ms elapsed-ms})))
    ;; @spec MCP-OP-TIME-005
    (let [result (-> domain-result
                     measured/partition-measured
                     (measured/attach {:elapsed_ms elapsed-ms}))]
      ;; A TYPED REFUSAL, not a diagnostic. A reading with no key to relocate
      ;; (one sitting directly in a vector, say) would reach the wire as a
      ;; nested JSON object and reach a parity hash as measured data. There is
      ;; no honest way to publish it, so the boundary refuses instead of
      ;; guessing — the bad state is unrepresentable downstream.
      (when-let [path (measured/first-unpartitioned-measured-path result)]
        (throw (ex-info "A measured value cannot be published outside the partition"
                        {:error-type :unpartitioned-measured-field
                         :path path})))
      result)))

(defn finalize-failure
  "Publish a failure raised OUTSIDE `invoke!` through `invoke!`'s own finalizer.

  The SDK adapter catches anything the handler, the finalizer, the summary or
  the serializer throws. Before this existed it built its own result map, and
  that map had no `measured` block — invalid against every canonical output
  schema, which requires one (Sol review 2026-09-04 §2). One boundary means
  one boundary."
  [failure started]
  (finalize-result failure (measured/elapsed-ms started)))

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
                ;; @spec MCP-OP-TIME-004
                ;; A narrower internal phase keeps its own figure, under a
                ;; phase-specific field of THIS partition. The census's map of
                ;; per-phase durations is one object rather than four fields
                ;; because the set of phases that ran is a fact about the
                ;; request, not a fixed shape.
                "phases_elapsed_ms" {:type "object"}
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
  ;; @spec MCP-OP-TIME-006
  ;; `get-in` on the well-known key rather than a `measured/field` verb: the
  ;; round-four review's §1b took that verb as a laundering route out of an
  ;; already-published block, and the namespace no longer offers one.
  (or (get-in result [measured/measured-key k]) (get result k)))

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
    :or {clock-nanos measured/raw-nanos
         serialize json/generate-string}}]
  (let [started-ns (clock-nanos)
        domain-result (execute)
        finished-ns (clock-nanos)
        result (finalize-result domain-result
                                (/ (- (double finished-ns) (double started-ns))
                                   1000000.0))
        summary (summarize result)
        body (serialize result)]
    (callback [summary] (not (:ok result)) result)
    body))
