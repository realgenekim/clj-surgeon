(ns clj-surgeon.ls-tree-snapshot
  "The immutable manifest snapshot a tree-scale continuation cursor is PINNED
   to.

   A paged read has to answer one question the first page cannot defer: WHICH
   repository is page 2 a page of? The branch this namespace repairs answered
   it with a digest folded from `<path>\\t<size>\\t<mtime>` per candidate. Sol's
   executed review (2026-09-03) showed that is not repository identity:

   - a file whose bytes change while path, size and mtime are preserved pages
     as UNCHANGED, so page 2 serves content page 1's tree never held;
   - a cursor minted against one root is accepted against a DIFFERENT root
     whose files happen to carry the same stats.

   Both are silent wrong results — the caller cannot tell — which is why they
   were blockers rather than fixes.

   The remedy is PINNING rather than re-deriving. The page that first needs a
   cursor writes an IMMUTABLE SNAPSHOT: the ordered candidate list, and for
   every candidate its path, size, mtime and the SHA-256 of its CONTENT. Later
   pages are served FROM that snapshot. Three properties follow:

   1. IDENTITY IS CONTENT. A served file whose current content digest differs
      from its pinned one refuses `:stale-result-cursor` and NAMES the path.
      A byte swap under a preserved stat is caught because nothing about the
      stat is load-bearing any more.

   2. THE SNAPSHOT IS ADDRESSED BY ROOT. Snapshots live under the workspace
      state root keyed by the SHA-256 of the canonical root path, so a cursor
      from another root does not resolve at all: `:unknown-result-cursor`.

   3. A PAGE READS ONLY ITS SLICE. Page 2 does no discovery, no glob and no
      tree walk; it seeks `offset` lines into the pinned row file and takes
      `limit`. That closes the `O(pages x N)` re-walk the same review found.

   MEMORY. Nothing here retains a row. The snapshot is WRITTEN streaming — one
   row rendered, digested, written and dropped — and READ streaming, a
   transducer over `line-seq` that keeps only the slice a page will encode.
   The heap cost is one 64 KB block buffer plus the page, whatever N is.

   SEMANTICS THIS BUYS, AND ITS PRICE. A continuation is a SNAPSHOT read, not
   a live one: files created after the snapshot are not in it and will not
   appear on later pages, and a file deleted or rewritten refuses when its own
   page is served. That is the honest trade for not re-walking the tree per
   page, and it is strictly safer than the alternative it replaces, which
   interleaved two repositories without saying so. Callers who need the new
   file rescan; the receipt names the snapshot so they can tell.

   See docs/intent/read-path-memory/read-path-memory-design.md."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io File)
   (java.security MessageDigest)))

;; ============================================================
;; Where snapshots live
;; ============================================================

(def ^:dynamic *state-root*
  "Overrides the workspace state root. `nil` means the default. Bound by
   witnesses so a test never writes into the operator's real state directory,
   and honoured ahead of the environment so a binding is always authoritative."
  nil)

(defn state-root
  "The local-state root snapshots live under: the binding, else
   `CLJ_SURGEON_STATE_ROOT`, else `~/.local/state/clj-surgeon` — the same
   directory shape `clj-surgeon.mcp-workspace/receipt-dir` already uses."
  []
  (or *state-root*
      (System/getenv "CLJ_SURGEON_STATE_ROOT")
      (str (io/file (System/getProperty "user.home")
                    ".local" "state" "clj-surgeon"))))

(defn- hex
  [^bytes bs]
  (str/join (map #(format "%02x" (bit-and 0xff (long %))) bs)))

(defn sha256-hex
  "SHA-256 of a string, as 64 lowercase hex characters."
  ^String [^String s]
  (hex (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))))

(defn- canonical
  [root]
  (let [f (io/file (str root))]
    (try (.getCanonicalPath f) (catch Exception _ (.getAbsolutePath f)))))

(def ^:private id-pattern #"[0-9a-f]{64}")

(defn cursor-dir
  "The snapshot directory for one root. Keyed by the SHA-256 of the CANONICAL
   root path, so two roots never share a snapshot namespace and a cursor is
   not portable between them."
  ^File [root]
  (io/file (state-root) "workspaces" (sha256-hex (canonical root))
           "ls-tree-cursors"))

(defn- meta-file ^File [root cursor-id] (io/file (cursor-dir root) (str cursor-id ".edn")))
(defn- rows-file ^File [root cursor-id] (io/file (cursor-dir root) (str cursor-id ".rows")))

(defn- new-id
  "128 bits of `UUID/randomUUID` entropy twice over, rendered as 64 hex
   characters — the same shape as the digests beside it, so one `id-pattern`
   guards every identifier this namespace accepts from a caller."
  []
  (str (str/replace (str (java.util.UUID/randomUUID)) "-" "")
       (str/replace (str (java.util.UUID/randomUUID)) "-" "")))

;; ============================================================
;; Content identity
;; ============================================================

(defn content-digest
  "SHA-256 of a file's BYTES, streamed in 64 KB blocks: constant heap whatever
   the file's size.

   `nil` when the file cannot be read. A candidate that has vanished since the
   snapshot is exactly as stale as one whose bytes moved, and the caller turns
   the nil into that refusal rather than guessing."
  [path]
  (try
    (let [md (MessageDigest/getInstance "SHA-256")
          buf (byte-array 65536)]
      (with-open [in (io/input-stream (io/file (str path)))]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update md buf 0 n)
              (recur)))))
      (hex (.digest md)))
    (catch Exception _ nil)))

;; ============================================================
;; The cursor MAC
;; ============================================================

(defn mac
  "The authenticator for one `(cursor-id, offset)` pair.

   Keyed on the snapshot's per-snapshot SECRET, which is written into the
   snapshot and NEVER returned to a caller. Keying it on the published
   manifest digest instead would let any holder of a receipt mint any offset —
   which is the second blocker, not a fix for it."
  [cursor-id offset secret]
  (sha256-hex (str cursor-id ":" offset ":" secret)))

;; ============================================================
;; Writing a snapshot
;; ============================================================

(def snapshot-ttl-ms
  "How long a snapshot is kept. A cursor is a within-session artifact; a day
   is far past any paging run and short enough that the state directory does
   not grow without bound."
  (* 24 60 60 1000))

(defn- prune!
  "Best-effort removal of snapshots past their TTL. Failure is silent on
   purpose: housekeeping must never turn a good read into a refusal."
  [^File dir]
  (try
    (let [now (System/currentTimeMillis)]
      (doseq [^File f (or (.listFiles dir) [])
              :let [nm (.getName f)]
              :when (and (.isFile f)
                         (str/ends-with? nm ".edn")
                         (> (- now (.lastModified f)) snapshot-ttl-ms))]
        (let [id (subs nm 0 (- (count nm) 4))]
          (.delete f)
          (.delete (io/file dir (str id ".rows"))))))
    (catch Exception _ nil)))

(defn write-snapshot!
  "Pin `rows` — a LAZY seq of `{:pidx :path :abs}` in result order — and return
   `{:cursor-id :digest :total :secret}`.

   Every row is stat'd, content-digested, rendered, folded into the snapshot
   digest, written and DROPPED. Nothing accumulates, so this costs one block
   buffer of heap at N = 10 and at N = 10,000 alike.

   The meta file is written LAST and renamed into place, so a snapshot is
   either complete or absent: a crash mid-write leaves rows nobody can address,
   and the cursor that would have named them resolves to
   `:unknown-result-cursor` rather than to a truncated manifest."
  [{:keys [root projects rows]}]
  (let [dir (cursor-dir root)]
    (.mkdirs dir)
    (prune! dir)
    (let [cursor-id (new-id)
          md (MessageDigest/getInstance "SHA-256")
          total (with-open [w (io/writer (rows-file root cursor-id))]
                  (reduce
                    (fn [n {:keys [pidx path abs]}]
                      (let [f (io/file (str abs))
                            line (pr-str {:i n :x pidx :p path
                                          :s (.length f)
                                          :m (.lastModified f)
                                          :h (content-digest abs)})]
                        (.update md (.getBytes (str line "\n") "UTF-8"))
                        (.write w line)
                        (.write w "\n")
                        (inc n)))
                    0
                    rows))
          digest (hex (.digest md))
          secret (new-id)
          tmp (io/file dir (str cursor-id ".edn.tmp"))]
      (spit tmp (pr-str {:v 1
                         :cursor-id cursor-id
                         :root (canonical root)
                         :created (System/currentTimeMillis)
                         :digest digest
                         :total total
                         :secret secret
                         :projects (mapv #(select-keys % [:name :root]) projects)}))
      (.renameTo tmp (meta-file root cursor-id))
      {:cursor-id cursor-id :digest digest :total total :secret secret})))

;; ============================================================
;; Reading a snapshot
;; ============================================================

(defn read-meta
  "The snapshot's meta map, or `nil` when this root holds no such snapshot.

   `cursor-id` is re-checked against the id pattern here as well as at parse
   time: it becomes a FILENAME, and a component that builds a path from
   caller-supplied text validates it itself rather than trusting its callers."
  [root cursor-id]
  (when (and (string? cursor-id) (re-matches id-pattern cursor-id))
    (let [f (meta-file root cursor-id)]
      (when (.isFile f)
        (try (edn/read-string (slurp f)) (catch Exception _ nil))))))

(defn read-rows
  "Rows `[offset, offset+limit)` of a pinned manifest, in result order.

   A transducer over `line-seq`: the lines before the slice are read and
   dropped one at a time and the lines after it are never read, so a page's
   retained cost is the page and not the manifest."
  [root cursor-id offset limit]
  (let [f (rows-file root cursor-id)]
    (when (.isFile f)
      (with-open [r (io/reader f)]
        (into [] (comp (drop offset) (take limit) (map edn/read-string))
              (line-seq r))))))

(defn stale-row
  "The first row of `rows` whose pinned content no longer matches the file on
   disk, as `{:path :pinned :observed}` — or `nil` when every row still holds.

   `:observed` is `nil` for a file that has been deleted, which reads as
   exactly what it is in the refusal."
  [root rows]
  (some (fn [{:keys [p h]}]
          (let [now (content-digest (io/file (str root) p))]
            (when (not= h now)
              {:path p :pinned h :observed now})))
        rows))
