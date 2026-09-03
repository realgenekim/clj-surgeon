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

   2. THE SNAPSHOT IS ADDRESSED BY ROOT, TWICE. Snapshots live under the
      workspace state root keyed by the SHA-256 of the canonical root path,
      AND the canonical root seeds the manifest digest itself, so a cursor
      from another root does not resolve at all: `:unknown-result-cursor`.
      The second binding is not redundant. Two identical checkouts fold to one
      CONTENT digest, so with a content-only address a foreign cursor resolved
      to the twin's meta and refused `:invalid-result-cursor` — a forgery
      receipt about a token this server had minted. A refusal is not enough;
      the receipt has to be true.

   3. A PAGE READS ONLY ITS SLICE. Page 2 does no discovery, no glob and no
      tree walk; it seeks `offset` lines into the pinned row file and takes
      `limit`. That closes the `O(pages x N)` re-walk the same review found.

   4. A SNAPSHOT IS ADDRESSED BY WHAT IT CONTAINS, AND WHERE. `cursor-id` IS
      the manifest digest — folded over every row's position, project, path
      and CONTENT digest, seeded with `manifest-version` and the canonical
      root. The first shape of this namespace minted
      the id from `UUID/randomUUID`, which named the SCAN rather than the tree,
      and the memory battery caught it: five reps of one operation over one
      corpus produced four distinct output hashes (`nondeterministic:4`),
      differing in exactly one line of 98,361 characters — the cursor. The same
      randomness pinned a new 1.4 MB snapshot on every scan of a tree that had
      not moved. Content-addressing makes an unchanged tree scan IDENTICALLY
      WITHIN ONE WARM SNAPSHOT STORE and REUSE its snapshot, and a changed
      tree get a new id by construction.

      THE QUALIFICATION IS LOAD-BEARING, and it was missing. The determinism
      is split across the two halves of the cursor token, which have opposite
      requirements. The manifest DIGEST is a pure function of the tree and its
      root, so it is identical across cold stores, across machines, and after
      a prune. The MAC is keyed on a per-snapshot random SECRET, which must
      NOT be derivable from published material — that is the forgery blocker —
      so a store that has never seen this tree mints a fresh secret and the
      cursor line differs. Measured: two cold stores, identical digest,
      different macs, one line of 36,853 bytes apart. So a battery run against
      a CLEANED state root, or any scan after the 24 h TTL prune
      (`snapshot-ttl-ms`), is legitimately not byte-identical to the one
      before it, and a reader who took the unqualified claim as a contract was
      reading a promise nothing could keep.

   A REUSED SNAPSHOT IS VERIFIED, NEVER ASSUMED. A file sitting under a
   content address is a CLAIM about its content; reuse re-folds the rows on
   disk and takes the snapshot only when they still prove the id they are
   filed under. Corrupt, truncated, or tampered is a MISS, and a miss rebuilds.

   THE MAC IS NOT KEYED ON THE ID. Content-addressing PUBLISHES the id: it is
   the receipt's `:manifest_digest`. Keying the authenticator on it — which an
   earlier brief specified as `sha256(cursor-id || offset || snapshot-digest)`
   — would let any holder of a receipt mint a cursor for any offset. The key
   stays a per-snapshot random secret written only inside the snapshot file.

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
   (java.nio.file Paths)
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

(defn- random-hex64
  "128 bits of `UUID/randomUUID` entropy twice over, rendered as 64 hex
   characters — the same shape as the digests beside it, so one `id-pattern`
   guards every identifier this namespace accepts from a caller.

   This mints SECRETS and build temporaries. It no longer mints cursor ids: an
   id that names the scan rather than the tree makes an unchanged tree scan
   differently every time, and pins a new snapshot for a manifest it already
   holds."
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

(def manifest-version
  "The PROJECTION version of a pinned manifest: which fields a row carries and
   what identity is folded from them.

   It seeds the snapshot digest, so changing the row projection changes every
   id and no snapshot written under an older shape is ever reused under a
   newer one. A projection version that is not IN the address is a migration
   nobody can detect.

   v2 binds the CANONICAL ROOT into the address (see `digest-header`), which
   changes every id, so every v1 snapshot becomes unaddressable at once and
   ages out under the TTL rather than being read under a projection that no
   longer describes it."
  2)

(defn- digest-header
  "What seeds the manifest digest: the projection version AND the canonical
   root the manifest was taken of.

   The root is in the ADDRESS, not merely in the directory the snapshot is
   filed under, because a receipt made a promise the address could not keep.
   `:unknown-result-cursor` says a cursor minted against another root does not
   resolve at all — but two identical checkouts fold to one content digest, so
   once the twin had been scanned the cursor resolved to the TWIN's meta and
   fell through to the mac check, refusing `:invalid-result-cursor`: the
   remedy text for a forgery, printed about a token this server minted.

   Binding the root makes the promise true by construction rather than by
   luck: twins now have different addresses, so a foreign cursor finds no
   meta and is `unknown`, and `:invalid-result-cursor` goes back to meaning
   only what it says — this server did not mint that token.

   The price is that `:manifest_digest` no longer identifies tree CONTENT
   across roots: two identical checkouts report different digests. That is the
   right trade for a cursor, whose whole job is to answer WHICH repository
   page 2 is a page of."
  [root]
  (str "clj-surgeon/ls-tree-manifest/v" manifest-version "\n" (canonical root) "\n"))

(defn- row-identity
  "The canonical identity of one manifest row, as folded into the snapshot
   digest: position, project index, path, CONTENT digest.

   Size and mtime are deliberately ABSENT. Stat is not identity here — that
   was Sol's first blocker — and a digest that folded mtime would hand a
   touched-but-unchanged tree a new id, a new snapshot, and a different
   cursor, which is the nondeterminism this addressing exists to remove."
  [{:keys [i x p h]}]
  (str i "\t" x "\t" p "\t" h "\n"))

;; ============================================================
;; The cursor MAC
;; ============================================================

(defn mac
  "The authenticator for one `(cursor-id, offset)` pair.

   Keyed on the snapshot's per-snapshot SECRET, which is written into the
   snapshot and NEVER returned to a caller. Keying it on the published
   manifest digest instead would let any holder of a receipt mint any offset —
   which is the second blocker, not a fix for it.

   That deviation from the original brief became MORE load-bearing, not less,
   once the id was content-addressed: `cursor-id` now IS the manifest digest,
   so a mac keyed on either of them is a mac keyed on material printed in the
   receipt. The witness is `a-receipt-holder-cannot-mint-a-cursor-for-another-
   offset`, which builds every mac derivable from a receipt and requires each
   one to refuse."
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
  "Best-effort removal of snapshots past their TTL, and of build temporaries a
   crashed write left behind. Failure is silent on purpose: housekeeping must
   never turn a good read into a refusal."
  [^File dir]
  (try
    (let [now (System/currentTimeMillis)]
      (doseq [^File f (or (.listFiles dir) [])
              :let [nm (.getName f)]
              :when (and (.isFile f)
                         (> (- now (.lastModified f)) snapshot-ttl-ms))]
        (cond
          ;; A `.tmp` is never addressable, so age is the only thing that can
          ;; be said about it. Sweeping it here is why an interrupted write
          ;; leaks nothing permanent.
          (str/ends-with? nm ".tmp") (.delete f)
          (str/ends-with? nm ".edn") (let [id (subs nm 0 (- (count nm) 4))]
                                        (.delete f)
                                        (.delete (io/file dir (str id ".rows")))))))
    (catch Exception _ nil)))

(defn- touch!
  "Best-effort TTL refresh, so a snapshot that is being REUSED does not expire
   on the clock of the scan that first wrote it. Silent on failure, like
   `prune!`: housekeeping must never turn a good read into a refusal."
  [^File f]
  (try (.setLastModified f (System/currentTimeMillis)) (catch Exception _ nil)))

(declare read-meta)

(defn rows-digest
  "Re-fold `[digest row-count]` from a pinned rows file on disk, or `nil` when
   it cannot be read, a line is not a row, or the rows are out of order.

   This is what makes REUSE safe. A snapshot is addressed by its content, so a
   file sitting under that address is a CLAIM about its content; re-folding it
   before serving is the whole difference between content-addressed and
   name-addressed. Streaming, one line at a time: verifying a manifest must
   not cost what building it would.

   PUBLIC as the one place the manifest address is computed from bytes on
   disk. A witness that needs a snapshot which PASSES verification must fold
   its address the way the implementation does, not the way a test author
   remembers it doing."
  [root ^File f]
  (when (.isFile f)
    (try
      (let [md (MessageDigest/getInstance "SHA-256")]
        (.update md (.getBytes ^String (digest-header root) "UTF-8"))
        (with-open [r (io/reader f)]
          (let [n (reduce
                    (fn [n line]
                      (let [row (edn/read-string line)]
                        (when-not (and (map? row) (= n (:i row)) (string? (:p row)))
                          (throw (ex-info "not a manifest row" {:at n})))
                        (.update md (.getBytes (row-identity row) "UTF-8"))
                        (inc n)))
                    0
                    (line-seq r))]
            [(hex (.digest md)) n])))
      (catch Exception _ nil))))

(defn verified-snapshot
  "The snapshot filed under `cursor-id` for `root` — but ONLY when its own
   bytes still prove it: this projection version, this root, this id, a secret
   present, and rows on disk that re-fold to exactly the id they are filed
   under with the row count the meta claims.

   Anything else is a MISS. On the WRITE path a miss rebuilds from the tree;
   on the SERVE path a miss is `:unknown-result-cursor`, because a manifest
   that no longer proves its own address is not a manifest this root holds.

   PUBLIC because SERVE needs it as much as REUSE does. The round-three review
   found `run-pinned-page` resolving the snapshot with `read-meta`, so a rows
   file tampered to substitute one candidate for another was served under an
   unchanged cursor with no signal: `[m06 m01 m08 m09 m10]`, m01 standing in
   for m07. Verifying on reuse and trusting on serve makes the address a
   filename again on exactly the path a caller reads."
  [root cursor-id]
  (when-let [m (read-meta root cursor-id)]
    (let [[d n] (rows-digest root (rows-file root cursor-id))]
      (when (and (= manifest-version (:v m))
                 (= cursor-id (:cursor-id m))
                 (= cursor-id (:digest m))
                 (= (canonical root) (:root m))
                 (string? (:secret m))
                 (= cursor-id d)
                 (= (:total m) n))
        m))))

(defn write-snapshot!
  "Pin `rows` — a LAZY seq of `{:pidx :path :abs}` in result order — and return
   `{:cursor-id :digest :total :secret}`.

   The snapshot is CONTENT-ADDRESSED: `cursor-id` IS the manifest digest,
   folded over every row's position, project, path and content digest under
   `manifest-version`. Two consequences, and both are the point:

   - An unchanged tree scans to the SAME cursor, so the result is
     deterministic. A random id made two scans of one corpus differ in exactly
     one line, which the memory battery reported as `nondeterministic:4`.
   - An unchanged tree REUSES its snapshot — after verifying it — so the state
     directory holds one snapshot per distinct TREE STATE rather than one per
     scan. Four identical scans used to leave four 1.4 MB snapshots.

   Every row is content-digested, rendered, folded, written and DROPPED.
   Nothing accumulates: this costs one block buffer of heap at N = 10 and at
   N = 10,000 alike, and the digest cannot be known before the last row, which
   is why the rows are built under a temporary name and renamed into place.

   The meta file is written LAST and renamed into place, so a snapshot is
   either complete or absent: a crash mid-write leaves rows nobody can address,
   and the cursor that would have named them resolves to
   `:unknown-result-cursor` rather than to a truncated manifest. A REBUILD over
   a snapshot that failed verification removes the meta first, so the id is
   unaddressable while its bytes are being replaced rather than briefly
   addressable with the wrong ones.

   The MAC secret stays per-snapshot, random, and written ONLY into the
   snapshot file. Content-addressing publishes the id — it is the receipt's
   `:manifest_digest` — which is exactly why the mac may not be keyed on it.
   A rebuild mints a FRESH secret: the discarded snapshot's authenticator is
   discarded with it, so cursors minted against bytes that failed verification
   refuse rather than being honoured against bytes nobody verified."
  [{:keys [root projects rows]}]
  (let [dir (cursor-dir root)]
    (.mkdirs dir)
    (prune! dir)
    (let [tmp-rows (io/file dir (str "build-" (random-hex64) ".rows.tmp"))
          md (doto (MessageDigest/getInstance "SHA-256")
               (.update (.getBytes ^String (digest-header root) "UTF-8")))
          total (with-open [w (io/writer tmp-rows)]
                  (reduce
                    (fn [n {:keys [pidx path abs]}]
                      (let [row {:i n :x pidx :p path :h (content-digest abs)}]
                        (.update md (.getBytes (row-identity row) "UTF-8"))
                        (.write w (pr-str row))
                        (.write w "\n")
                        (inc n)))
                    0
                    rows))
          cursor-id (hex (.digest md))]
      (if-let [existing (verified-snapshot root cursor-id)]
        (do (.delete tmp-rows)
            (touch! (meta-file root cursor-id))
            {:cursor-id cursor-id
             :digest cursor-id
             :total (:total existing)
             :secret (:secret existing)})
        (let [secret (random-hex64)
              tmp (io/file dir (str cursor-id ".edn.tmp"))]
          (.delete (meta-file root cursor-id))
          (.delete (rows-file root cursor-id))
          (.renameTo tmp-rows (rows-file root cursor-id))
          (spit tmp (pr-str {:v manifest-version
                             :cursor-id cursor-id
                             :root (canonical root)
                             :created (System/currentTimeMillis)
                             :digest cursor-id
                             :total total
                             :secret secret
                             :projects (mapv #(select-keys % [:name :root]) projects)}))
          (.renameTo tmp (meta-file root cursor-id))
          {:cursor-id cursor-id :digest cursor-id :total total :secret secret})))))

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

(defn row-file
  "The file a manifest row's path names, or `nil` when that row is not
   CONFINED to `root`.

   ONE resolver, used by everything that turns a row into a file: the
   staleness check and the encoder's candidate list. They used to disagree —
   `io/file` at the check, `fs/path` at the read — so the file that was
   VERIFIED and the file that was READ could be different files, and an
   absolute row passed the check and then threw
   `IllegalArgumentException: ... is not a relative path` out of the operation
   from the read side. A boundary enforced by two resolvers is not a boundary.

   Confinement is LEXICAL: the row path must be relative, and `root/p`
   normalized must still lie under `root` normalized. It deliberately does NOT
   resolve symlinks. Measured on this branch: `discover-projects` FOLLOWS a
   symlinked `.clj` whose target is outside the root and encodes it on a fresh
   scan, so a realpath check here would refuse a page for a tree the fresh
   scan encodes whole — a page-1/page-2 divergence introduced by the guard
   itself. The guard therefore refuses what discovery can NEVER produce (an
   absolute path, a `..` escape) and defers to discovery on what it can.
   `a-symlinked-file-inside-the-root-pages-exactly-as-it-is-discovered` pins
   that choice so it cannot be changed by accident."
  ^File [root p]
  (when (string? p)
    (try
      (let [base (.normalize (.toAbsolutePath (.toPath (io/file (str root)))))
            child (Paths/get p (into-array String []))]
        (when-not (.isAbsolute child)
          (let [resolved (.normalize (.resolve base child))]
            (when (and (.startsWith resolved base) (not= resolved base))
              (.toFile resolved)))))
      (catch Exception _ nil))))

(defn unconfined-row
  "The first row of `rows` whose path is not confined to `root`, as
   `{:path p}` — or `nil` when every row resolves inside it.

   Checked BEFORE staleness and before any candidate is built, so an
   unconfined row is never opened: the refusal costs no read at all."
  [root rows]
  (some (fn [{:keys [p]}] (when-not (row-file root p) {:path p})) rows))

(defn stale-row
  "The first row of `rows` whose pinned content no longer matches the file on
   disk, as `{:path :pinned :observed}` — or `nil` when every row still holds.

   `:observed` is `nil` for a file that has been deleted, which reads as
   exactly what it is in the refusal — and for an unconfined row, which is
   never opened here; `unconfined-row` names that case properly and runs
   first."
  [root rows]
  (some (fn [{:keys [p h]}]
          (let [f (row-file root p)
                now (when f (content-digest f))]
            (when (not= h now)
              {:path p :pinned h :observed now})))
        rows))
