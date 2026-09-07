(ns clj-surgeon.synchronous-verification
  "Existing extraction proof and profile admission, shared by MCP and CLI."
  (:require [cheshire.core :as json]
            [clj-surgeon.verification-process :as change-buffer]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- refusal [suffix message data]
  (merge {:ok false :error_type (str "helper-extraction-" suffix) :error message
          :operation "helper_extraction" :next_call nil :source_unchanged true
          :committed false :mutation_attempted false :write_authority false} data))

(def max-profile-timeout-ms
  "Ceiling on a configured profile's declared timeout.

  A timeout is the only bound between this verb and a proof that never
  returns, so an unbounded or absurd one is a profile this verb refuses rather
  than a number it honours."
  3600000)

(defn- argv?
  "One command: a non-empty vector of non-empty strings."
  [command]
  (and (vector? command)
       (seq command)
       (every? #(and (string? %) (seq %)) command)))

(defn- argv-list?
  "A non-empty vector of commands, each one an argv vector."
  [commands]
  (and (vector? commands) (seq commands) (every? argv? commands)))

(defn profile-capability
  "What one configured verification profile can do, or nil when v1 cannot admit it.

  v1 admits ONLY profiles whose every check is an external command this process
  runs and waits on, spelled as argv:

  * a `:cold` job is asynchronous — `launch!` returns `:running` and the receipt
    would say `verification_complete false`, which cannot gate a commit;
  * a `:hot` law runs inside a warm application JVM, which is precisely the
    stale-Var false proof MCP-OP-HELPER-022 exists to forbid;
  * a `:commands` entry that is not an argv VECTOR of non-empty strings is not a
    command this verb can run. A profile whose `:commands` is `[\"/bin/true\"]`
    is a vector of one STRING, not a vector of one command, and admitting it let
    a malformed profile stage a whole extraction before ending as a timeout.

  Shape is decided here, before anything is staged, and this function never
  throws: an unrecognised spec is nil, which the preflight turns into a typed
  refusal."
  [spec]
  (try
    (cond
      (argv? spec)
      {:synchronous? true :rollback-capable? true :fresh-process? true
       :commands [spec] :shape :command}

      (not (map? spec)) nil

      ;; asynchronous or warm-JVM authority: never admitted, whatever else the
      ;; profile carries
      (or (contains? spec :hot) (contains? spec :cold)) nil

      (= #{:acceptance :timeout-ms :commands} (set (keys spec)))
      (when (and (argv-list? (:commands spec))
                 (integer? (:timeout-ms spec))
                 (pos? (:timeout-ms spec))
                 (<= (:timeout-ms spec) max-profile-timeout-ms))
        {:synchronous? true :rollback-capable? true :fresh-process? true
         :commands (vec (:commands spec)) :timeout-ms (:timeout-ms spec)
         :shape :exact})

      (argv-list? (:commands spec))
      (let [timeout (:timeout-ms spec)]
        (when (or (nil? timeout)
                  (and (integer? timeout) (pos? timeout)
                       (<= timeout max-profile-timeout-ms)))
          (cond-> {:synchronous? true :rollback-capable? true :fresh-process? true
                   :commands (vec (:commands spec)) :shape :commands}
            timeout (assoc :timeout-ms timeout))))

      :else nil)
    (catch Throwable _ nil)))

(defn- runnable-command?
  "Whether the profile's executable IS an executable file, right now.

  A resolved spelling that merely CONTAINS a slash proves nothing: an absolute
  path to a file that does not exist passed that test and staged a whole
  extraction that could only ever end in a launch failure. The question this
  answers is the one MCP-OP-HELPER-011 asks — can this proof run before I write
  — so it is answered against the filesystem, and it never throws."
  [command]
  (try
    (let [resolved (str (first (change-buffer/expand-command command [])))
          candidate (io/file resolved)]
      (and (str/includes? resolved "/")
           (.isFile candidate)
           (.canExecute candidate)))
    (catch Throwable _ false)))

;; @spec MCP-OP-HELPER-011
(defn admitted-profiles
  "`{profile-name capability}` for every profile v1 may prove a write with.

  Capability is a property of the CONFIGURED profile, derived from the same
  verification-profiles configuration every other write tool reads — never a
  fixture-only flag this verb sets for itself."
  ([]
   ;; the SERVER's own built-in registry, and nothing this verb declares for
   ;; itself. There is no source-declared `helper-proof`: the acceptance-owned
   ;; proof is repository data and reaches this verb the way every other
   ;; profile does, through the workspace's configured verification profiles.
   (admitted-profiles {}))
  ([profiles]
   ;; a PURE FILTER over what was handed in: exactly those profiles this verb
   ;; may prove a write with, and nothing this function knows from elsewhere. A
   ;; caller asking "what does THIS configuration admit" must never be answered
   ;; with the server's defaults. Accepts either a bare `{name spec}` map or a
   ;; whole config map carrying `:verification-profiles`, because both are
   ;; spellings the callers of this boundary already hold.
   (let [profiles (if (contains? profiles :verification-profiles)
                    (:verification-profiles profiles)
                    profiles)]
     (into (sorted-map)
           (keep (fn [[profile-name spec]]
                   ;; a profile with no command of its own can prove nothing,
                   ;; and is therefore never admitted
                   (when-let [capability (profile-capability spec)]
                     ;; @spec MCP-OP-HELPER-011
                     ;; a profile with no command, or one whose executable is
                     ;; not an executable file right now, can prove nothing and
                     ;; is therefore not admissible. Admission is the gate: a
                     ;; malformed or unlaunchable profile that only fails at the
                     ;; preflight has already been called admitted once, and a
                     ;; caller reading `admitted-profiles` would believe it.
                     (when (and (seq (:commands capability))
                                (every? runnable-command? (:commands capability)))
                       [profile-name capability]))))
           (or profiles {})))))

(defn verification-preflight
  "nil when `profile-name` may prove this write now, or the typed refusal.

  Nothing is staged by this function, and nothing may be staged before it
  answers: an unusable verification authority is knowable from the request and
  the configuration alone.

  Two arities because the two callers ask different questions. `plan` is a READ
  and asks only whether the named profile is an admissible one; `execute!` asks
  the whole question a moment before staging, runnability included, because a
  profile that cannot launch is a profile that cannot roll a write back."
  ([profiles profile-name] (verification-preflight profiles profile-name false))
  ([profiles profile-name check-runnable?]
  (let [;; @spec MCP-OP-HELPER-011
        ;; ONLY the routed workspace's configured profiles. The server's
        ;; built-in registry is NOT a source of authority here: an empty or
        ;; absent configuration admits NOTHING, and a request naming the
        ;; built-in `fast` profile against a workspace that configures no
        ;; profiles is refused rather than proved by a command that workspace
        ;; never declared.
        admitted (admitted-profiles profiles)
        capability (get admitted profile-name)
        commandless? (and capability (empty? (:commands capability)))
        unrunnable (when (and capability check-runnable? (not commandless?))
                     (first (remove runnable-command? (:commands capability))))]
    (cond
      (nil? capability)
      (refusal "verification-preflight-unavailable"
               (str "The verification profile " (pr-str profile-name)
                    " is not a synchronous, rollback-capable profile this"
                    " workspace configures.")
               {:profile profile-name
                :needed {:synchronous true :rollback_capable true
                         :fresh_process true}
                :configured_profiles (vec (sort (keys (or profiles {}))))
                :admitted_profiles (vec (keys admitted))
                :staged false
                :decision "which admitted profile proves this write"})

      ;; a name the contract fixes that this workspace never gave a command:
      ;; admissible in principle, unable to prove anything here
      commandless?
      (refusal "verification-preflight-unavailable"
               (str "The verification profile " (pr-str profile-name)
                    " is named by the contract but this workspace configures no"
                    " command for it.")
               {:profile profile-name
                :configured_profiles (vec (sort (keys (or profiles {}))))
                :staged false
                :decision "which admitted profile proves this write"})

      unrunnable
      (refusal "verification-preflight-unavailable"
               (str "The verification profile " (pr-str profile-name)
                    " names an executable that cannot be found now: "
                    (pr-str (first unrunnable)))
               {:profile profile-name
                :unrunnable_command (vec unrunnable)
                :staged false
                :decision "which admitted profile proves this write"})))))

(def proof-stdout-fields
  "The fields the acceptance-owned proof prints as JSON on stdout.

  Copied when the proof actually printed them and omitted when it did not. A
  constant substituted for a field the proof did not emit is the manufactured
  evidence MCP-OP-HELPER-022 exists to forbid."
  [:profile :behavior_cases :caller_files :selected_sites :retained_sites
   :changed_files :application_compile_claim :evidence_id])

(def typed-check-fields
  "The three TYPED checks the receipt reports, and never a bare coverage count."
  [:structural_callers :helper_behaviors :compiled_callers])

(def max-proof-output-bytes
  "How much of the proof's own stdout this process reads back.

  The proof PRINTS its evidence as JSON, so the cap that matters is the one on
  what the parser gets to see, not the one sized for a receipt."
  (* 512 1024))

(def default-proof-timeout-ms 600000)

(defn- parse-proof-output
  "The proof's stdout JSON, or nil when it printed something else.

  A proof that printed no JSON is not a proof that failed: the exit code says
  that. This only decides whether there are FIELDS to copy."
  [output]
  (try
    (let [trimmed (str/trim (str output))
          start (str/last-index-of trimmed "{")]
      (when start
        (let [parsed (json/parse-string (subs trimmed start) true)]
          (when (map? parsed) parsed))))
    (catch Exception _ nil)))

;; @spec MCP-OP-HELPER-022
(defn run-proof!
  "Run one admitted profile's own command in a FRESH process at the candidate cwd.

  Fresh, so a warm namespace holding stale Vars for retired helpers cannot
  manufacture a proof of a tree they no longer belong to. `fresh_process` is
  reported from the fact that a child process ran and answered, never asserted."
  [project-root profile-name capability]
  ;; the cwd may arrive as a Path or a String; the process runner coerces with
  ;; `io/file`, which has no implementation for a Path and turned a real launch
  ;; into a launch-error that then read as "no fresh process ran"
  (let [project-root (str project-root)
        timeout (or (:timeout-ms capability) default-proof-timeout-ms)
        outcomes
        (reduce (fn [acc command]
                  (let [argv (change-buffer/expand-command command [])
                        process (change-buffer/run-process!
                                  project-root argv timeout max-proof-output-bytes)
                        outcome (assoc (select-keys process
                                                    [:exit :elapsed_ms :finished?
                                                     :output :output-bytes
                                                     :output-sha256 :output-truncated
                                                     :launch-error])
                                       ;; @spec MCP-OP-HELPER-022
                                       ;; a child that STARTED, independent of
                                       ;; whether it finished: a timed-out proof
                                       ;; has no exit code and did run, and
                                       ;; deriving freshness from the exit code
                                       ;; reported the opposite
                                       :started? (not (true? (:launch-error process)))
                                       :command (vec argv))
                        acc (conj acc outcome)]
                    (if (and (:finished? process) (zero? (or (:exit process) 1)))
                      acc
                      (reduced acc))))
                []
                (:commands capability))
        last-outcome (last outcomes)
        finished? (boolean (:finished? last-outcome))
        ok? (and finished? (= (count outcomes) (count (:commands capability)))
                 (every? #(zero? (or (:exit %) 1)) outcomes))
        printed (parse-proof-output (:output last-outcome))]
    (merge
      ;; the proof's own JSON fields, copied when present and never replaced
      (select-keys printed (into [:status] (concat proof-stdout-fields
                                                   typed-check-fields
                                                   [:compiled_evidence])))
      {:profile profile-name
       ;; @spec MCP-OP-HELPER-022
       ;; a fresh child process RAN. Not "answered": a proof killed at its
       ;; timeout started, executed, and was cut off, and reporting
       ;; `fresh_process false` for it says the opposite of what happened.
       ;; It is still a FACT, never a courtesy: a child that could not be
       ;; launched at all reports false, and `cwd_exists` separates the two
       ;; causes that otherwise print the same `Exec failed, error: 2` — a
       ;; missing executable, and a working directory that is not there.
       :fresh_process (boolean (some :started? outcomes))
       :cwd_exists (.isDirectory (io/file project-root))
       :timed_out (not finished?)
       :ok ok?
       ;; the RAW evidence, retained rather than summarized into a constant
       :process_evidence outcomes
       :cwd (str project-root)})))
