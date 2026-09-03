(ns clj-surgeon.result-budget
  "The bounded output budget for tree-scale READ results.

   A tree-scale read (`ls-tree` today) walks a repository and encodes one
   record per file. Before this namespace existed the encoder had no ceiling:
   the result grew with the repository, so the caller's retained heap did too.
   Measured on the memory battery's synthetic corpus, `cli-ls-tree` retained
   9.4 KB per file — 0.9 MB at 1,000 files and 94.0 MB at 10,000, which is the
   `held-scales-with-n` failure MCP-OP-MEM-003 exists to close.

   The control is a RESULT CEILING `R`, counted in RECORDS, applied to the
   encoder rather than to the walk. The walk still visits every candidate; what
   is bounded is what the encoder KEEPS. A scan whose candidate count is at or
   under `R` is encoded whole and is byte-identical to the unbounded path — the
   ceiling is invisible until it binds.

   When it binds the caller gets one of two TYPED answers, never a silent
   truncation:

   - a CONTINUATION — the first `R` records plus `:next_call`, whose cursor
     carries the offset and the manifest digest; or
   - a REFUSAL — when the caller asked for a complete result, naming `R`, the
     observed count, and how many fit.

   The cursor is bound to the MANIFEST DIGEST, not to the offset alone. A page
   taken after the tree changed would silently interleave records from two
   different repositories; binding the digest turns that into a refusal.

   PURE. No I/O, no heavy requires — it loads under babashka so its witnesses
   run in the millisecond fast suite.

   See docs/intent/read-path-memory/read-path-memory-design.md."
  (:require
   [clojure.string :as str])
  (:import
   (java.security MessageDigest)))

;; ============================================================
;; The ceiling
;; ============================================================

;; @spec MCP-OP-MEM-003
(def max-result-records
  "Server hard cap on the number of RECORDS one tree-scale read result may
   encode. A request may lower it; nothing may raise it.

   1,000 is derived from three independent bounds, all measured:

   1. RETENTION. One `ls-tree` record retains 9.4-9.5 KB (memory battery,
      2026-09-03: cli-ls-tree held 0.9 MB at N=1,000 and 94.0 MB at N=10,000).
      A 1,000-record ceiling therefore pins retained result heap at about
      9.5 MB whatever the repository's size, and makes the battery's
      `held(10,000) <= held(1,000) + 2.0 MiB` line hold BY CONSTRUCTION rather
      than by luck: both scales encode the same number of records.

   2. REAL CORPORA. This repository holds 163 `.clj`/`.cljc`/`.cljs` files
      under `src/` and `test/`. 1,000 is 6.1x that, so no ordinary
      single-project scan is truncated and no existing caller changes
      behaviour. The ceiling binds on repository-of-repositories scans, which
      is exactly where the unbounded result was unusable anyway.

   3. OUTPUT SIZE. The text encoding runs about 1.5 KB per file, so 1,000
      records is roughly 1.5 MB of output — already past what one CLI result
      or one model context can absorb. A ceiling above this bounds nothing a
      caller could use.

   Lowering this constant is always safe. Raising it re-opens the retention
   failure and must be re-measured on the battery first."
  1000)

;; @spec MCP-OP-MEM-003
(defn parse-ceiling
  "The caller's requested record ceiling, or `nil` when it asked for none.

   Returns `:invalid` for anything that is not a positive integer, so the
   caller can refuse loudly. A malformed ceiling must never silently become
   the server cap: that is the shape of an accidental unbounded read."
  [requested]
  (cond
    (nil? requested) nil
    (integer? requested) (if (pos? requested) (long requested) :invalid)
    (string? requested) (let [t (str/trim requested)]
                          (if (re-matches #"\d+" t)
                            (let [n (Long/parseLong t)]
                              (if (pos? n) n :invalid))
                            :invalid))
    :else :invalid))

;; @spec MCP-OP-MEM-003
(defn resolve-ceiling
  "The effective ceiling `R` for a request. A request may LOWER the server cap
   and may never raise it."
  [requested]
  (let [parsed (parse-ceiling requested)]
    (if (or (nil? parsed) (= :invalid parsed))
      max-result-records
      (min parsed max-result-records))))

;; ============================================================
;; The manifest digest a cursor is bound to
;; ============================================================

;; @spec MCP-OP-MEM-003
(defn digest-start
  "A fresh SHA-256 accumulator for a candidate manifest."
  ^MessageDigest []
  (MessageDigest/getInstance "SHA-256"))

(defn digest-candidate!
  "Fold one candidate into the manifest digest and return the accumulator.

   The line is `<relative-path>\\t<size>\\t<mtime-millis>\\n`. It is folded and
   dropped: the manifest is never materialised, so the digest costs one stat
   per file and no retained heap."
  ^MessageDigest [^MessageDigest md path size mtime]
  (.update md (.getBytes (str path "\t" size "\t" mtime "\n") "UTF-8"))
  md)

(defn digest-hex
  "The finished manifest digest as 64 lowercase hex characters."
  [^MessageDigest md]
  (str/join (map #(format "%02x" %) (.digest md))))

(defn cursor-token
  "The opaque continuation cursor: an offset bound to a manifest digest."
  [offset digest]
  (str offset ":" digest))

(defn parse-cursor
  "Parse a continuation cursor, or return `nil` when it is not one."
  [token]
  (when (string? token)
    (let [[offset digest] (str/split (str/trim token) #":" 2)]
      (when (and offset digest
                 (re-matches #"\d+" offset)
                 (re-matches #"[0-9a-f]{64}" digest))
        {:offset (Long/parseLong offset)
         :manifest-digest digest}))))

;; ============================================================
;; Typed receipts — continuation, refusal, stale cursor
;; ============================================================

(defn- next-call
  [{:keys [dir ceiling output-format]} cursor]
  (cond-> {:op :ls-tree :dir dir}
    (not= ceiling max-result-records) (assoc :max-results ceiling)
    output-format (assoc :format output-format)
    cursor (assoc :cursor cursor)))

;; @spec MCP-OP-MEM-003
(defn continuation
  "The receipt for a result that stopped at the ceiling with more to come.

   `:offset` is where this page started, `:returned` how many records it
   encoded, `:total` the candidate count, `:remaining` what a later page would
   still hold."
  [{:keys [ceiling offset returned total digest] :as request}]
  {:result_ceiling {:limit ceiling
                    :server_max max-result-records
                    :offset offset
                    :returned returned
                    :total total
                    :remaining (max 0 (- total (+ offset returned)))
                    :manifest_digest digest}
   :next_call (next-call request (cursor-token (+ offset returned) digest))})

;; @spec MCP-OP-MEM-003
(defn ceiling-refusal
  "The receipt for a caller that asked for a COMPLETE result the ceiling
   cannot hold. It names `R` and the count that fits, and it narrows: the
   remedy is a smaller scope or an explicit page, never `raise the ceiling`."
  [{:keys [ceiling total digest] :as request}]
  {:error-type :result-ceiling-exceeded
   :error (format (str "ls-tree found %d file(s); a complete result may hold "
                       "at most %d record(s)")
                  total ceiling)
   :limit {:kind :result-records
           :requested ceiling
           :server-max max-result-records
           :observed total
           :fits ceiling}
   :complete false
   :source-unchanged true
   :remedy (str "narrow :dir, add :grep, or drop :complete and page through "
                "the result with the :cursor in :next_call")
   :next_call (next-call request (cursor-token 0 digest))})

;; @spec MCP-OP-MEM-003
(defn stale-cursor-refusal
  "The receipt for a continuation whose manifest digest no longer matches the
   tree. Paging on regardless would interleave records from two different
   repositories, so the page refuses and the remedy is to rescan."
  [{:keys [cursor-digest digest] :as request}]
  {:error-type :stale-result-cursor
   :error "the tree changed since this cursor was issued"
   :limit {:kind :manifest-digest
           :requested cursor-digest
           :observed digest}
   :complete false
   :source-unchanged true
   :remedy "rescan from the start; the cursor's manifest no longer exists"
   :next_call (next-call request nil)})

;; ============================================================
;; Text rendering — the same receipts, in the text encoding
;; ============================================================

(defn- render-next-call
  [{:keys [op dir max-results format cursor]}]
  (str "clj-surgeon :op " op " :dir " dir
       (when max-results (str " :max-results " max-results))
       (when format (str " :format " format))
       (when cursor (str " :cursor " cursor))))

(defn continuation-text
  "The continuation receipt as the trailing block of a text result."
  [receipt]
  (let [{:keys [limit offset returned total remaining]} (:result_ceiling receipt)]
    (str (format "── result_ceiling: %d record(s), %d of %d file(s) shown from offset %d, %d remaining\n"
                 limit returned total offset remaining)
         (format "   next_call: %s\n" (render-next-call (:next_call receipt))))))

(defn refusal-text
  "A refusal receipt as text, for the text encoding of a read."
  [receipt]
  (str (format "── %s: %s\n" (name (:error-type receipt)) (:error receipt))
       (format "   remedy: %s\n" (:remedy receipt))
       (format "   next_call: %s\n" (render-next-call (:next_call receipt)))))

;; @spec MCP-OP-MEM-003
(defn invalid-ceiling-refusal
  "The receipt for a `:max-results` that is not a positive integer. A malformed
   ceiling must be named, never silently promoted to the server cap: that is
   how a caller ends up with an unbounded read it believed it had bounded."
  [{:keys [requested] :as request}]
  {:error-type :invalid-result-ceiling
   :error (str ":max-results must be a positive integer; got " (pr-str requested))
   :limit {:kind :result-records
           :requested requested
           :server-max max-result-records}
   :complete false
   :source-unchanged true
   :remedy (str "pass :max-results as a positive integer at or below "
                max-result-records ", or omit it")
   :next_call (next-call (assoc request :ceiling max-result-records) nil)})

;; @spec MCP-OP-MEM-003
(defn invalid-cursor-refusal
  "The receipt for a `:cursor` this server did not issue. Ignoring it and
   starting from the top would hand the caller page 1 while it believed it had
   page 2 — a silently wrong result, which is worse than a refusal."
  [{:keys [token] :as request}]
  {:error-type :invalid-result-cursor
   :error (str ":cursor is not a continuation cursor: " (pr-str token))
   :limit {:kind :result-cursor :requested token}
   :complete false
   :source-unchanged true
   :remedy "drop :cursor and rescan; a cursor is only ever copied from :next_call"
   :next_call (next-call request nil)})
