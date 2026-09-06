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
  "Characters kept of any free-text field. Beyond this the value is truncated
   and the line says so."
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

(defn truncate
  "[value truncated?] -- free text bounded at `free-text-limit`."
  [value]
  (cond
    (nil? value) [nil false]
    (> (count (str value)) free-text-limit)
    [(subs (str value) 0 free-text-limit) true]
    :else [(str value) false]))

(def mission-enums
  {:mission_state #{"proposed" "ready" "blocked" "applied" "verified" "failed" "undone"}
   :mission_verb #{"owner_forms" "helper_extraction"}
   :executor #{"native" "typist"}
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
   because \"not measured\" and \"instant\" are different facts."
  [{:keys [ts seat pid kind tool ok error_type wall_ms mission_id dropped] :as event}]
  (let [[error truncated?] (truncate error_type)]
    (cond-> (merge {:ts ts
                    :seat seat
                    :pid pid
                    :kind kind
                    :tool tool
                    :ok (boolean ok)
                    :error_type error
                    :wall_ms (when (number? wall_ms) (long (Math/round (double wall_ms))))
                    :mission_id mission_id}
              (mission-fields event))
      truncated? (assoc :error_type_truncated true)
      (pos? (or dropped 0)) (assoc :telemetry_dropped dropped))))

(defn render-line
  "Serialize one line map to bytes, bounded by `line-limit`. If the rendered
   line still exceeds the ceiling -- only reachable through an absurd tool or
   mission id -- the free-text fields are dropped entirely rather than the
   line being split, and the line says `:over_limit true`."
  ^String [line]
  (let [rendered (str (json/generate-string line) "\n")]
    (if (<= (count (.getBytes rendered "UTF-8")) line-limit)
      rendered
      (str (json/generate-string
             (assoc line :error_type nil :tool (first (truncate (:tool line)))
                    :mission_id nil :over_limit true))
           "\n"))))

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
    (let [target (io/file (str file))]
      (when-let [parent (.getParentFile (.getAbsoluteFile target))]
        (when (.mkdirs parent)
          (try (Files/setPosixFilePermissions
                 (.toPath parent) (PosixFilePermissions/fromString "rwx------"))
               (catch Exception _ nil))))
      (with-open [channel (FileChannel/open (.toPath target) (open-options))]
        (.write channel (ByteBuffer/wrap (.getBytes ^String line "UTF-8"))))
      (try (Files/setPosixFilePermissions
             (.toPath target) (PosixFilePermissions/fromString "rw-------"))
           (catch Exception _ nil))
      true)
    (catch Throwable _ false)))

(defn record!
  "Append one ledger line for a completed public tool or mission boundary. Never throws, never
   fails the call. A failed append increments `dropped`; the count rides out
   on the next line that lands, then resets -- so the drop is reported exactly
   once and by the process that suffered it.

   `event` keys: :kind :tool :ok :error_type :wall_ms :mission_id plus the
   optional fixed enums/count accepted by mission-fields."
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
