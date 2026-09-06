(ns clj-surgeon.telemetry-events
  "TELEMETRY-EVENTS-001 -- ONE box-wide JSONL ledger, appended as a side
   effect of the public MCP functions that do the work.

   THE BLIND SPOT THIS CLOSES. `mcp-telemetry/start!` writes into a directory
   the LAUNCHER chose (`:telemetry-dir`, e.g. a fixture-scoped server under
   /var/tmp/forge/<lane>-fx), and the usage collector reads a fixed list of
   directories. On 2026-09-05 the hourly watch reported the same four figures
   all night while a dozen public calls happened elsewhere: every call against
   a directory nobody told the collector about was invisible, and invisible
   was indistinguishable from calm.

   WATCHING BELONGS WHERE THE WORK HAPPENS (Gene, 2026-09-06: \"I think
   telemetry / watching is in wrong place; I think it should be in the MCP
   fns, and written as JSONL file someplace? Reduces need for watches -- it's
   a side effect of the fns that are doing the work.\"). So the ledger is not
   a watcher to be pointed at roots; it is an append the call itself performs,
   to ONE path that does not depend on how the process was launched.

   THE APPEND IS ATOMIC PER LINE. Every write opens the file CREATE+APPEND and
   issues exactly ONE `write` of one complete line, so concurrent processes'
   lines never interleave -- POSIX guarantees an O_APPEND write under
   PIPE_BUF-scale size is not split, and the 4 KB line ceiling keeps every
   line inside that regime. A free-text field longer than 1 KB is TRUNCATED
   and says so in the line, because a ledger that can be made to exceed its
   own atomicity budget by a long error string is a ledger that corrupts
   itself under exactly the conditions you most want it.

   A FAILED APPEND NEVER FAILS THE CALL. Telemetry is a side effect; a tool
   that refuses work because its ledger is unwritable has inverted its
   priorities. Drops are COUNTED in-process and reported in the next line that
   does land (`telemetry_dropped`), so a silently blind ledger still tells the
   reader how much it lost -- a non-zero drop count is an alarm, not an
   archive."
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.lang ProcessHandle)
   (java.nio ByteBuffer)
   (java.nio.channels FileChannel)
   (java.nio.file Files OpenOption StandardOpenOption)
   (java.nio.file.attribute PosixFilePermissions)
   (java.time Instant)))

(def events-file-env "CLJ_SURGEON_EVENTS_FILE")

(def free-text-limit
  "UTF-8 BYTES kept of any free-text field. Beyond this the value is truncated
   and the line says so.

   BYTES, not characters (Sol fence r1, 2026-09-06). The ceiling this feeds is
   the atomic-append budget, and that budget is measured in bytes: a 1024-
   character field of three-byte codepoints is 3 KB, which is how a `count`-
   based bound let a 5185-byte line out of a 4096-byte ledger. Truncation
   never splits a codepoint -- a half-written character is invalid UTF-8, and
   an invalid line is a line the collector cannot parse at all."
  1024)

(def line-limit
  "Hard ceiling on one serialized line, newline included. Keeps every append
   inside the size regime where one O_APPEND write is not split."
  4096)

(defonce ^{:doc "Appends this process failed to write, not yet reported."}
  dropped
  (atom 0))

(defn default-events-file
  "The box-wide ledger path, `~/.clj-surgeon/events.jsonl` -- a dotdir in
   $HOME beside ~/.codex and ~/.claude, deliberately NOT under ~/.local/state,
   which is where the per-launcher directories the collector kept missing
   live. `CLJ_SURGEON_EVENTS_FILE` overrides it. Reads the `user.home`
   PROPERTY, not $HOME, so an isolated test JVM stays isolated."
  []
  (let [override (System/getenv events-file-env)]
    (if (and override (not (str/blank? override)))
      override
      (str (io/file (System/getProperty "user.home")
                    ".clj-surgeon" "events.jsonl")))))

(defn current-pid [] (.pid (ProcessHandle/current)))

(defn current-seat
  "$SURGEON_SEAT, else $USER, else \"unknown\". Never blank."
  []
  (let [candidate (or (System/getenv "SURGEON_SEAT") (System/getenv "USER"))]
    (if (str/blank? (str candidate)) "unknown" (str candidate))))

;; ---------------------------------------------------------------------------
;; SCRUBBING, HASHING, AND BYTE-BOUNDING
;;
;; Sol's fence review (r1, 2026-09-06) put a key-shaped canary and a file-
;; content canary through the caller's `mission_id` and found both verbatim in
;; the "content-free" ledger. The ledger's whole claim is that it carries
;; counts, not content; a field copied straight from a caller is content.

(def secret-re
  "The runner's own regexes (bin/typist-run SECRET_RE), spelled identically so
   one string cannot be redacted by the Python side and survive the Clojure
   side. Anything key-shaped becomes `<redacted>` on its way into a line."
  #"gsk_[A-Za-z0-9_-]+|sk-or-[A-Za-z0-9_-]+|Bearer\s+[A-Za-z0-9_.\-]+")

(defn scrub
  "Pure. Replace anything key-shaped with `<redacted>`. Non-strings pass
   through untouched, so a number or nil is never stringified by accident."
  [value]
  (if (string? value)
    (str/replace value secret-re "<redacted>")
    value))

(defn utf8-bytes ^long [value] (alength (.getBytes (str value) "UTF-8")))

(defn truncate-utf8
  "[string truncated?] -- `s` cut to at most `limit` UTF-8 BYTES, never
   splitting a codepoint. Walks codepoints and stops before the one that would
   cross the ceiling, so the result is always valid UTF-8 and always <= limit."
  [s ^long limit]
  (let [s (str s)]
    (if (<= (utf8-bytes s) limit)
      [s false]
      (loop [idx 0 used 0]
        (if (>= idx (.length s))
          [(subs s 0 idx) true]
          (let [cp (.codePointAt s idx)
                width (Character/charCount cp)
                cost (utf8-bytes (subs s idx (+ idx width)))]
            (if (> (+ used cost) limit)
              [(subs s 0 idx) true]
              (recur (+ idx width) (+ used cost)))))))))

(defn truncate
  "[value truncated?] -- free text SCRUBBED and then bounded at
   `free-text-limit` UTF-8 bytes. Scrub before truncate: a key cut in half is
   still most of a key."
  [value]
  (if (nil? value)
    [nil false]
    (truncate-utf8 (scrub (str value)) free-text-limit)))

(def mission-id-shape
  "The ONLY shape a raw mission id may keep: `M-<digits>`, the mission
   ledger's own minted form. Everything else is hashed.

   THE NARROW RULE WAS CHOSEN DELIBERATELY over the wider
   `^[A-Za-z0-9._-]{1,64}$` identifier shape Sol's fix note offered as an
   alternative, and the reason is that the wide shape is not a shape at all:
   `gsk_ABCdef123` matches it perfectly and is a live credential. Defending
   the wide shape means bolting the scrubber on as a second, BLOCKLIST gate --
   and a blocklist protects against the key formats we happen to have written
   regexes for. `M-<digits>` is an ALLOWLIST that admits no payload by
   construction: a digit string cannot carry a key, a path, or a file body.
   The cost is that free-form mission names (`real-2j`) become digests rather
   than words; the digest is stable, so a mission's lines still group, and the
   run receipts -- which are not a fleet-wide box-shared file -- keep the
   readable name. Sol's canary was `gsk_LEDGER_CANARY|FILE-CONTENT-CANARY`,
   which BOTH rules reject; the point of the narrow one is the canary nobody
   has thought of yet."
  #"^M-[0-9]{1,12}$")

(defn- sha256-prefix
  "First 16 hex characters of the SHA-256 of `s`. Enough to correlate one
   caller's lines with each other and useless for recovering the input."
  [s]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes (str s) "UTF-8"))]
    (subs (apply str (map #(format "%02x" %) digest)) 0 16)))

(defn safe-mission-id
  "The mission id as it is allowed to be PERSISTED.

   Kept RAW only when it matches `mission-id-shape`. Everything else is
   persisted as `sha256:<16 hex>` of the raw id: stable enough to group a
   mission's lines, one-way, and bounded by construction. nil stays nil
   (absent is a fact, not a value to hash)."
  [raw]
  (when (some? raw)
    (let [s (str raw)]
      (if (re-matches mission-id-shape s)
        s
        (str "sha256:" (sha256-prefix s))))))

(def mission-enums
  {:mission_state #{"proposed" "ready" "blocked" "applied" "verified" "failed" "undone"}
   :mission_verb #{"owner_forms" "helper_extraction" "fallback"}
   :fallback_kind #{"native-tool"}
   :report_basis #{"user-reported"}
   :fallback_reason #{"refusal" "unsupported" "slower-than-native" "user-choice"}
   :executor #{"native" "typist"}
   :cost_source #{"provider-reported"}
   :provider #{"openrouter" "groq" "spark"}
   :model #{"openai/gpt-oss-120b" "gpt-5.3-codex-spark"}
   :upstream #{"Cerebras" "Groq" "OpenAI"}
   :refused_rung #{"mechanical-class" "complete-dossier" "source-policy"
                   "cheap-gate" "independent-acceptance" "guarded-commit"
                   "bounded-scope" "pinned-provider" "verified-rate"}})

(defn mission-fields
  "Only fixed mission enums and bounded candidate counts enter shared JSONL."
  [event]
  (cond-> (reduce-kv (fn [out key admitted]
                       (let [value (get event key)]
                         (cond-> out (contains? admitted value) (assoc key value))))
            {} mission-enums)
    (and (integer? (:candidate_count event)) (<= 1 (:candidate_count event) 5))
    (assoc :candidate_count (:candidate_count event))
    (and (string? (:mission_id event))
         (re-matches #"M-[0-9]{1,12}" (:mission_id event)))
    (assoc :mission_id (:mission_id event))))

(defn line-map
  "Build one ledger line. PURE -- no clock, no I/O, no filesystem. Every field
   is a scalar the reader needs and nothing else: no key, no path, no source.
   `wall_ms` is rounded to a long; a nil stays nil rather than becoming 0,
   because \"not measured\" and \"instant\" are different facts.

   Only the admitted mission-field enums/count pass through. Unknown scalar
   and nested fields are dropped; retained values still pass the shared
   scrubber and byte bound. Named fields own their validation."
  [{:keys [ts seat pid kind tool ok error_type wall_ms mission_id dropped
           prompt_tokens completion_tokens reasoning_tokens cost_usd
           provider upstream]
    :as event}]
  (let [[error truncated?] (truncate error_type)
        tok (fn [v] (when (and (integer? v) (<= 0 v Long/MAX_VALUE)) (long v)))
        bounded (fn [v] (first (truncate v)))]
    (cond-> (merge (into {} (map (fn [[k v]] [k (if (string? v) (bounded v) v)]))
                          (dissoc (mission-fields event) :mission_id))
                   {:ts ts
                    ;; EVERY string field goes through scrub+byte-truncate, including
                    ;; the ones that come from the environment. $SURGEON_SEAT is
                    ;; attacker-adjacent in exactly the way a tool argument is: Sol's
                    ;; 5185-byte line was a long seat, copied without a bound.
                    :seat (bounded seat)
                    :pid pid
                    :kind (bounded kind)
                    :tool (bounded tool)
                    :ok (boolean ok)
                    :error_type error
                    :wall_ms (when (number? wall_ms) (long (Math/round (double wall_ms))))
                    ;; NEVER the caller's raw id: validated-or-hashed (see
                    ;; `safe-mission-id`), so a ledger line cannot be used as a
                    ;; smuggling channel for key material or file content.
                    :mission_id (safe-mission-id mission_id)
                    ;; COST FIELDS -- optional, and ALWAYS PRESENT AS KEYS so a reader
                    ;; never has to distinguish "absent" from "the writer forgot".
                    ;; null is the honest value for a caller that has no such number:
                    ;; the MCP tools have none, the typist runner and the ledger do.
                    ;; A token count that is not a number stays nil rather than
                    ;; becoming 0 -- "not reported" and "zero tokens" are different
                    ;; facts, the same rule `wall_ms` already follows.
                    :prompt_tokens (tok prompt_tokens)
                    :completion_tokens (tok completion_tokens)
                    :reasoning_tokens (tok reasoning_tokens)
                    :cost_usd (when (and (number? cost_usd)
                                         (Double/isFinite (double cost_usd))
                                         (<= 0 (double cost_usd)))
                                (double cost_usd))
                    ;; free text, bounded like every other free-text field
                    :provider (some-> (:provider (mission-fields event)) bounded)
                    :upstream (some-> (:upstream (mission-fields event)) bounded)})
      truncated? (assoc :error_type_truncated true)
      (pos? (or dropped 0)) (assoc :telemetry_dropped dropped))))

(def free-text-drop-order
  "The order free-text fields are surrendered in when a line will not fit.
   Fixed, so the reader always knows what a shrunken line kept: the DIAGNOSIS
   goes last (error_type), the decoration first."
  [:provider :upstream :mission_id :error_type :tool :seat :kind])

(defn render-line
  "Serialize one line map, GUARANTEED under `line-limit` UTF-8 bytes.

   Three stages, each strictly smaller than the last, so there is no input for
   which this returns an over-budget line -- the property Sol's `huge-line-
   bytes=5185 limit=4096` disproved of the previous version, whose fallback
   left `seat` (and therefore the ceiling) unbounded:

     1. the line as built;
     2. free-text fields dropped one at a time in `free-text-drop-order`,
        stopping the moment it fits, with `:over_limit true` saying so;
     3. the FLOOR -- a line of nothing but the scalars a counter needs (ts,
        pid, ok, wall_ms, kind, over_limit), every string field nil. Those
        are bounded by construction, so stage 3 cannot fail.

   A line that still would not fit at stage 3 is impossible; if it somehow
   were, `append-line!` refuses it rather than splitting an append."
  ^String [line]
  (let [render (fn [m] (str (json/generate-string m) "\n"))
        fits? (fn [s] (<= (utf8-bytes s) line-limit))
        first-cut (render line)]
    (if (fits? first-cut)
      first-cut
      (or (some (fn [n]
                  (let [candidate (render (-> (apply dissoc line
                                                     (take n free-text-drop-order))
                                              (assoc :over_limit true)))]
                    (when (fits? candidate) candidate)))
                (range 1 (inc (count free-text-drop-order))))
          (render {:ts (:ts line) :pid (:pid line) :ok (:ok line)
                   :wall_ms (:wall_ms line) :over_limit true})))))

(defn- open-options
  ^"[Ljava.nio.file.OpenOption;" []
  (into-array OpenOption [StandardOpenOption/CREATE
                          StandardOpenOption/WRITE
                          StandardOpenOption/APPEND]))

(defn append-line!
  "Append one already-rendered line with a single O_APPEND write. Returns true
   on success, false on any failure -- it NEVER throws, because a caller of
   this is in the middle of returning a tool result."
  [file line]
  (try
    (when (> (utf8-bytes line) line-limit)
      (throw (ex-info "ledger line exceeds the atomic-append budget"
                      {:bytes (utf8-bytes line) :limit line-limit})))
    (let [target (io/file (str file))]
      (when-let [parent (.getParentFile (.getAbsoluteFile target))]
        (.mkdirs parent)
        ;; ENFORCED ON EVERY WRITE, not only on the one that created the
        ;; directory (Sol fence r1: `existing-dir-mode=755`). Whether this
        ;; process made the parent is an accident of ordering; whether the
        ;; ledger's parent is private is a property the ledger owes its
        ;; reader on every append.
        ;;
        ;; TIGHTENING ONLY -- the group and other bits are STRIPPED, and no
        ;; owner bit is ever added. Privacy is what 0700 buys, and privacy is
        ;; entirely a statement about group and other; adding owner bits would
        ;; instead mean the ledger silently unlocks a directory an operator
        ;; deliberately made read-only, which is a write this code has no
        ;; business winning. A normal owner-writable parent lands on exactly
        ;; 0700.
        (try (let [path (.toPath parent)
                   opts (into-array java.nio.file.LinkOption [])]
               (when (Files/isDirectory path opts)
                 (let [current (Files/getPosixFilePermissions path opts)
                       owner-only (set (filter #{java.nio.file.attribute.PosixFilePermission/OWNER_READ
                                                 java.nio.file.attribute.PosixFilePermission/OWNER_WRITE
                                                 java.nio.file.attribute.PosixFilePermission/OWNER_EXECUTE}
                                         current))]
                   (when (not= current owner-only)
                     (Files/setPosixFilePermissions path owner-only)))))
             (catch Exception _ nil)))
      (with-open [channel (FileChannel/open (.toPath target) (open-options))]
        (.write channel (ByteBuffer/wrap (.getBytes ^String line "UTF-8"))))
      (try (Files/setPosixFilePermissions
             (.toPath target) (PosixFilePermissions/fromString "rw-------"))
           (catch Exception _ nil))
      true)
    (catch Throwable _ false)))

(defn record!
  "Append one ledger line for a completed public MCP call. Never throws, never
   fails the call. A failed append increments `dropped`; the count rides out
   on the next line that lands, then resets -- so the drop is reported exactly
   once and by the process that suffered it.

   `event` keys: :kind :tool :ok :error_type :wall_ms :mission_id, plus the
   optional cost fields :prompt_tokens :completion_tokens :reasoning_tokens
   :cost_usd :provider :upstream, which pass straight through and are null
   for any caller that does not have them."
  ([event] (record! (default-events-file) event))
  ([file event]
   (let [carried @dropped
         line (line-map (assoc event
                               :ts (str (Instant/now))
                               :seat (current-seat)
                               :pid (current-pid)
                               :dropped carried))]
     (if (append-line! file (render-line line))
       (do (swap! dropped - carried) line)
       (do (swap! dropped inc) nil)))))
