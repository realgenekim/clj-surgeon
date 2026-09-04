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
;; @spec MCP-OP-STUDY-049
(def envelope-keys
  "The top-level keys the FINALIZER may add to a domain result — the request
  envelope, in whatever shape the wire gives it.

  This is a statement about the FINALIZER's vocabulary, checked by a witness.
  It is NOT how the envelope is identified: `finalize-result` records exactly
  what it added, and `envelope` reads that record. A domain result may
  legitimately carry a top-level key spelled `:measured`, and it is domain
  data.

  `finalize-result` adds `:elapsed_ms` today. The MEM-003 landing nests the
  request clock and its siblings under `measured`. Both are named, and a
  witness fails if the finalizer ever adds a third key that is not."
  #{:elapsed_ms :measured})

;; @spec MCP-OP-STUDY-049
(def ^:private envelope-meta-key
  "Where a finalized result records the envelope its finalizer added.

  METADATA, not a key: a key would be forgeable by the domain in exactly the
  way key NAMES were, and it would travel into `structuredContent`. Metadata
  survives `assoc`, `dissoc` and `cond->` — every mutation the budget gate
  performs on a candidate — and disappears at serialization, which is where it
  stops being anyone's business."
  ::envelope)

;; @spec MCP-OP-STUDY-049
(defn stamp-envelope
  "Merge `added` into `result` AND record that the publisher is what added it.

  The only way to become `finalized?`. Field evidence (Sol O2 round-5 review,
  2026-09-04, section 4): while the envelope was a set of NAMES, a domain
  result carrying a 40,000-character `measured.user_blob` was read as
  publisher metadata and merged into an `inspect-output-limit` substitute
  that was never measured — 81,861 bytes against a 32,768-byte cap. The
  collision is not a bad name; it is asking the wrong question. Who added a
  key is a fact about construction, and only the constructor knows it."
  [result added]
  (when-not (map? added)
    (throw (IllegalArgumentException.
             (str "the envelope a finalizer records must be a map, not "
                  (pr-str added)))))
  (vary-meta (merge result added) assoc envelope-meta-key added))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-049
(defn envelope
  "The envelope of a finalized result, as a map to merge into a substitute.

  Read from the CONSTRUCTION record, never by matching key names."
  [result]
  (or (get (meta result) envelope-meta-key) {}))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-049
;; @spec MCP-OP-TIME-003
(defn request-elapsed-ms
  "The request clock, wherever the envelope carries it.

  A renderer needs the number, not its address. It reads the number the
  RECEIPT PUBLISHES — the top-level `:elapsed_ms`, else the nested
  `measured.elapsed_ms` — because the text block must agree with
  `structuredContent`, and a stage that rewrites the published clock (a
  fixed-clock witness, a job that re-times itself) has changed what the
  receipt says. Rendering the finalizer's original number over the published
  one would make the text disagree with the receipt it summarizes.

  The CONSTRUCTION record is the FALLBACK, for a result whose envelope the
  publisher has not yet merged into the map.

  Reading a NUMBER off a spelling is not the hazard MCP-OP-STUDY-049 exists
  for: the hazard is deciding WHOSE key a key is and copying it into a
  substitute. `envelope` and `finalized?` make that decision, and they read
  the construction record only."
  [result]
  (let [carried (envelope result)]
    (cond
      (contains? result :elapsed_ms) (:elapsed_ms result)
      (some? (get-in result [:measured :elapsed_ms]))
      (get-in result [:measured :elapsed_ms])
      (contains? carried :elapsed_ms) (:elapsed_ms carried)
      :else (get-in carried [:measured :elapsed_ms]))))

;; @spec MCP-OP-STUDY-040
;; @spec MCP-OP-STUDY-049
(defn finalized?
  "Does this result carry the envelope the publisher publishes?

  A result with no envelope is not one the publisher could publish, so
  measuring it would reintroduce the gap the 64-byte publish reserve used to
  paper over. The question is asked of the CONSTRUCTION record: a map that
  merely spells `:elapsed_ms` was not finalized, it was written."
  [result]
  (contains? (meta result) envelope-meta-key))

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
    (stamp-envelope domain-result {:elapsed_ms elapsed-ms})))

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
