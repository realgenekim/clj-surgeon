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

   The cursor is bound to a PINNED MANIFEST SNAPSHOT, not to the offset alone
   and not to a digest folded from stats. The page that first needs a cursor
   writes the ordered candidate list with each file's CONTENT digest into the
   workspace state root (`clj-surgeon.ls-tree-snapshot`); later pages are served
   from it, and a served file whose content moved refuses by name. This
   namespace owns the cursor GRAMMAR and the typed receipts; the snapshot store
   owns the bytes.

   PURE. No I/O, no heavy requires — it loads under babashka so its witnesses
   run in the millisecond fast suite.

   See docs/intent/read-path-memory/read-path-memory-design.md."
  (:require
   [clojure.string :as str]))

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
    (integer? requested) (or (try
                               (when (pos? requested)
                                 (let [n (long requested)]
                                   (when (pos? n) n)))
                               ;; `long` throws on an integer past Long range.
                               ;; A ceiling that cannot be a record count is
                               ;; invalid, and invalid is a receipt.
                               (catch Exception _ nil))
                             :invalid)
    (string? requested) (let [t (str/trim requested)]
                          (if (re-matches #"\d+" t)
                            ;; A well-FORMED integer can still be one this
                            ;; server could not have meant. Forty digits parse
                            ;; as nothing and used to throw
                            ;; NumberFormatException out of the operation; a
                            ;; number too large to be a record count is
                            ;; invalid, not an exception.
                            (let [n (try (Long/parseLong t) (catch Exception _ nil))]
                              (if (and n (pos? n)) n :invalid))
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
(def ^:private hex64 #"[0-9a-f]{64}")

(defn cursor-token
  "The opaque continuation cursor: `<cursor-id>:<offset>:<mac>`.

   The cursor-id NAMES a pinned manifest snapshot; the mac AUTHENTICATES this
   exact offset against that snapshot's secret. Before this shape the token was
   `<offset>:<manifest-digest>`, and its offset half was neither authenticated
   nor range-checked: an edited offset past the end returned an empty vector
   with no receipt, which a caller reads as a complete result."
  [cursor-id offset mac]
  (str cursor-id ":" offset ":" mac))

(defn parse-cursor
  "Parse a continuation cursor, or return `nil` when it is not one.

   `nil` covers BOTH a token of the wrong shape and a well-shaped token whose
   offset cannot be a record index — forty digits parse as nothing and used to
   throw `NumberFormatException` out of the operation. Both are the same fact
   about the caller's token, so both earn the same typed refusal."
  [token]
  (when (string? token)
    (let [parts (str/split (str/trim token) #":")]
      (when (= 3 (count parts))
        (let [[cursor-id offset mac] parts]
          (when (and (re-matches hex64 cursor-id)
                     (re-matches hex64 mac)
                     (re-matches #"\d+" offset))
            (when-let [n (try (Long/parseLong offset) (catch Exception _ nil))]
              {:cursor-id cursor-id :offset n :mac mac})))))))

;; ============================================================
;; Typed receipts — continuation, refusal, stale cursor
;; ============================================================

(defn- next-call
  [{:keys [dir ceiling output-format next-cursor]}]
  (cond-> {:op :ls-tree :dir dir}
    (not= ceiling max-result-records) (assoc :max-results ceiling)
    output-format (assoc :format output-format)
    next-cursor (assoc :cursor next-cursor)))

;; @spec MCP-OP-MEM-003
(defn continuation
  "The receipt for a result that stopped at the ceiling with more to come.

   `:offset` is where this page started, `:returned` how many records it
   encoded, `:total` the candidate count, `:remaining` what a later page would
   still hold.

   `:returned` is MEASURED by the encoder that produced the page, never
   computed from the manifest: the caller of this fn is handed the count
   rather than deriving one. A number derived separately can drift from the
   records beside it, and it did — `(min ceiling remaining)` printed
   `:returned 5` over a page of two, with a next cursor, which reads as a
   complete-looking page that holds nothing."
  [{:keys [ceiling offset returned total digest] :as request}]
  {:result_ceiling {:limit ceiling
                    :server_max max-result-records
                    :offset offset
                    :returned returned
                    :total total
                    :remaining (max 0 (- total (+ offset returned)))
                    :manifest_digest digest}
   :next_call (next-call request)})

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
   :next_call (next-call request)})

;; @spec MCP-OP-MEM-003
(defn stale-cursor-refusal
  "The receipt for a page one of whose PINNED files no longer holds the content
   the snapshot recorded — its bytes moved, or it is gone.

   It NAMES the file. A refusal that only says `the tree changed` leaves the
   caller to find which of ten thousand files moved; naming the path is the
   difference between a receipt and an apology. `:observed nil` is a deletion.

   Identity here is CONTENT, not stat: this refusal is what a byte swap under a
   preserved path, size and mtime produces, and the stat-derived digest it
   replaces served that swap as unchanged."
  [{:keys [path pinned observed] :as request}]
  {:error-type :stale-result-cursor
   :error (format "%s changed since this cursor was issued" path)
   :limit {:kind :pinned-content
           :file path
           :requested pinned
           :observed observed}
   :complete false
   :source-unchanged true
   :remedy "rescan from the start; this page's pinned manifest no longer holds"
   :next_call (next-call request)})

;; @spec MCP-OP-MEM-003
(defn unknown-cursor-refusal
  "The receipt for a cursor whose pinned snapshot this root does not hold —
   expired, pruned, never written, minted against a DIFFERENT root, or filed
   under bytes that no longer PROVE the manifest they are filed as.

   Snapshots are addressed by the canonical root path TWICE — the directory
   they are filed under, and the seed of the manifest digest itself — so a
   cursor is not portable between roots however alike two trees look, twins
   included. Serving it from whatever the current root happens to contain is
   the first blocker; refusing it as a FORGERY, which is what a content-only
   address did once the twin had been scanned, is a true refusal with a false
   receipt.

   The last cause is the round-three finding: a snapshot is verified before it
   is SERVED, not only before it is reused, so rows that no longer re-fold to
   their own address are `unknown` rather than authoritative. `unknown` is the
   honest word for it — the manifest that address names is not on disk any
   more, whatever is."
  [{:keys [token] :as request}]
  {:error-type :unknown-result-cursor
   :error (str "no pinned manifest for this cursor under this root: "
               (pr-str token))
   :limit {:kind :result-cursor :requested token}
   :complete false
   :source-unchanged true
   :remedy (str "rescan from the start; a cursor is only valid for the root "
                "that issued it, and only until its snapshot expires")
   :next_call (next-call request)})

;; @spec MCP-OP-MEM-003
(defn out-of-range-refusal
  "The receipt for a GENUINE cursor — this server minted it, the mac verifies —
   whose offset is past the end of its pinned manifest.

   Distinct from `:invalid-result-cursor` on purpose: that one says the token
   was not ours, this one says the token was ours and the position is not
   there. Before the range check, this returned an empty vector with no
   receipt, which is a complete result as far as any caller can tell."
  [{:keys [offset total] :as request}]
  {:error-type :result-cursor-out-of-range
   :error (format "cursor offset %d is past the end of a %d-record manifest"
                  offset total)
   :limit {:kind :result-offset :requested offset :observed total}
   :complete false
   :source-unchanged true
   :remedy "rescan from the start; the last page carries no :next_call"
   :next_call (next-call request)})

;; @spec MCP-OP-MEM-003
(defn unconfined-row-refusal
  "The receipt for a pinned manifest one of whose rows names a path OUTSIDE
   the root it was taken of — absolute, or escaping through `..`.

   It NAMES the path, as `:stale-result-cursor` does, because the caller's
   next question is always WHICH row. It is a fifth typed answer rather than
   one of the four cursor refusals because it states a different fact: the
   token was well formed, this server minted it, its offset is in range and
   its snapshot verified. What failed is the MANIFEST's claim to be a manifest
   of this root.

   No legitimate scan can produce such a row — `rel-path` relativizes files
   discovered under the root, and discovery lists only regular files and
   symlinks that resolve to them — so this is reachable through a manifest
   rewritten under the state root, or through the TREE changing under a
   pinned manifest (a directory that becomes a symlink out of the root
   between the pin and the page). It refuses rather than reads: before this
   receipt existed a `..` row ENCODED a namespace from outside the scan root
   with no signal at all, and an absolute row threw
   `IllegalArgumentException` out of an operation whose whole promise is a
   typed receipt and never a throw.

   THE MESSAGE SAYS `does not resolve to a source file inside`, not `is not
   inside`, and the difference is a round-six finding rather than a
   preference: two different facts reach this receipt. One is an ESCAPE — a
   `..` row, an absolute row, a parent that resolves outside — and `is not
   inside the scanned root` is true of it. The other is a leaf whose entry
   exists and is not a regular file — a directory, a symlink to one, a
   dangling symlink, a FIFO — which IS inside the root, and about which the
   old wording was simply FALSE, sending a reader hunting an attack that did
   not happen. One sentence has to be true of both."
  [{:keys [path] :as request}]
  {:error-type :unconfined-manifest-row
   :error (format "pinned manifest row %s does not resolve to a source file inside the scanned root"
                  (pr-str path))
   :limit {:kind :manifest-row :requested path}
   :complete false
   :source-unchanged true
   :remedy (str "rescan from the start; this cursor's pinned manifest is not "
                "a manifest of this root")
   :next_call (next-call request)})

;; @spec MCP-OP-MEM-003
(defn empty-page-refusal
  "The receipt for a page that encoded ZERO records while its pinned manifest
   still held rows to serve.

   A page ADVANCES by the number of records it encoded, so a page that encodes
   nothing advances by nothing: the continuation it would otherwise mint
   carries a cursor at its OWN offset, and a caller following `:next_call`
   follows it forever. The receipt would look healthy the whole way round —
   `:returned 0` beside a `:remaining` that never falls.

   Unreachable through the shipped path, and named anyway. It is unreachable
   because two separate facts hold at once — the slice guard refuses a manifest
   that cannot supply the rows the page promised, and both encoders `emit!`
   exactly once per candidate — and neither is stated where a refactor of the
   other would read it. This refusal is the statement: a page never advances by
   zero, whatever else changes.

   It carries NO cursor. A token that cannot make progress is worse than no
   token, because it looks like progress."
  [{:keys [offset slice] :as request}]
  {:error-type :empty-result-page
   :error (format (str "the page at offset %d encoded 0 of %d pinned row(s); "
                       "a page that advances by zero would repeat forever")
                  offset slice)
   :limit {:kind :result-records :requested slice :observed 0 :offset offset}
   :complete false
   :source-unchanged true
   :remedy (str "rescan from the start; this page cannot advance and its "
                "cursor would name its own offset")
   :next_call (next-call (dissoc request :next-cursor))})

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
   :next_call (next-call (assoc request :ceiling max-result-records))})

;; @spec MCP-OP-MEM-003
(defn invalid-cursor-refusal
  "The receipt for a `:cursor` this server did not issue. Ignoring it and
   starting from the top would hand the caller page 1 while it believed it had
   page 2 — a silently wrong result, which is worse than a refusal.

   It means ONLY that. A cursor minted here against a different root — even an
   identical twin checkout — is `:unknown-result-cursor`, because the root is
   bound into the manifest address; this receipt is never printed about a
   token this server minted."
  [{:keys [token] :as request}]
  {:error-type :invalid-result-cursor
   :error (str ":cursor is not a continuation cursor: " (pr-str token))
   :limit {:kind :result-cursor :requested token}
   :complete false
   :source-unchanged true
   :remedy "drop :cursor and rescan; a cursor is only ever copied from :next_call"
   :next_call (next-call request)})
