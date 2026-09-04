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

;; @spec MCP-OP-STUDY-040
(def envelope-keys
  "The top-level keys the FINALIZER adds to a domain result — the request
  envelope, in whatever shape the wire gives it.

  Declared HERE, in the namespace that owns the envelope, because two other
  places need to know it without knowing its shape: the public budget gate
  measures the FINAL published envelope, and every substitute the gate builds
  from scratch must carry the envelope of the result it replaces.

  `finalize-result` adds `:elapsed_ms` today. The MEM-003 landing nests the
  request clock and its siblings under `measured`. Both are named, and a
  witness fails if the finalizer ever adds a third key that is not.

  Field evidence (Opus O2 round-4 review, 2026-09-04, section 7): the budget
  gate copied `:elapsed_ms` by name and guarded on `(contains? result
  :elapsed_ms)`, so the moment the wire nests the clock every substitute would
  silently lose it and the fit would throw on the first request instead of
  measuring the result. Neither is a wire change — both are one namespace
  assuming a shape another namespace owns."
  #{:elapsed_ms :measured})

;; @spec MCP-OP-STUDY-040
(defn envelope
  "The envelope of a finalized result, as a map to merge into a substitute."
  [result]
  (select-keys result envelope-keys))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-TIME-003
(defn request-elapsed-ms
  "The request clock, wherever the envelope carries it.

  A renderer needs the number, not its address. Reading `:elapsed_ms` at the
  top level is one shape of the envelope, and the moment the wire nests the
  clock under `measured` every text block would render `nil` and throw — the
  same class as the budget gate naming one shape, one layer up."
  [result]
  (if (contains? result :elapsed_ms)
    (:elapsed_ms result)
    (get-in result [:measured :elapsed_ms])))

;; @spec MCP-OP-STUDY-040
(defn finalized?
  "Does this result carry the envelope the publisher publishes?

  A result with no envelope is not one the publisher could publish, so
  measuring it would reintroduce the gap the 64-byte publish reserve used to
  paper over. The question is asked about the envelope, never about one of
  its shapes."
  [result]
  (boolean (some #(contains? result %) envelope-keys)))

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
