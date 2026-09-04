(ns marvin-voice-remote.channel
  "Channel relay for the marvin-channel connector (a custom claude/channel MCP server).
   This is the REAL transport edge of the channel-connector PoC — it replaces the
   file mailbox so the skiff poller and the phone can rendezvous through Cloud Run
   (no inbound to the laptop; both sides connect OUTBOUND).

   ADDITIVE: nothing here touches the existing /voice -> STT -> Slack -> Marvin -> TTS
   path. Two per-seat queues, both DRAINED on GET (claim-once):

     POST /api/channel/messages   phone  -> Claude   {seat, from, text, id?}
     GET  /api/channel/messages?seat=X   -> drain     {:messages [...]}
     POST /api/channel/replies    Claude -> phone     {seat, text, reply_to?, files?}
     GET  /api/channel/replies?seat=X    -> drain      {:replies [...]}

   Auth: bearer token CHANNEL_TOKEN. If CHANNEL_TOKEN is unset, the endpoints are
   open ONLY when ENV=dev (local). See channel-connector/SPEC.md."
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [datastar-kit.ds :as ds]
   [marvin-voice-remote.auth :as auth]
   [marvin-voice-remote.blob :as blob]
   ;; DURABLE CAPTURE ARCHIVE (bd marvin-voice-remote-0ar). Every call below is
   ;; total, never-throwing and off the hot path — a broken archive must never
   ;; break dictation. It observes and persists; it never decides.
   [marvin-voice-remote.capture-archive :as capture-archive]
   [marvin-voice-remote.friction-ui :as friction-ui]
   ;; S5c DELTA-6 REDUCER INGRESS. One call, from one additive watch at the reply
   ;; seam (`::reducer-reply-ingest` below): an arriving connector reply becomes a
   ;; server-authored `:fact/reply-arrived` in the seat's live reducer session.
   ;; The dependency runs surface -> session loop, never the other way: nothing in
   ;; `reducer-session` knows this file exists.
   [marvin-voice-remote.reducer-session :as reducer-session]
   ;; S4 SHADOW TAPS (ARCHITECTURE.md §7 S4). `shadow/record!` is total,
   ;; never-throwing and returns nil, ALWAYS — so every call below is additive by
   ;; construction: no response, status, or decision in this file can depend on
   ;; it. It observes; it never decides. Failures are counted, not logged blind
   ;; (S13), and shown on /reducer-lab/shadow.
   [marvin-voice-remote.reducer.shadow :as shadow]
   ;; THE SSE CENSUS (bd marvin-voice-remote-gyb). Cloud Run holds this service at
   ;; maxScale=1 / concurrency=300, so 300 held requests is the ceiling for the
   ;; WHOLE service and an SSE stream is a request that never ends. The registry
   ;; supplies the two disciplines an abandoned stream cannot escape —
   ;; per-page-load supersession and a bounded rotation deadline — plus the
   ;; off-thread push agent that keeps a slow socket off an http-kit worker.
   [marvin-voice-remote.sse-registry :as sse]
   [org.httpkit.server :as http]
   [taoensso.timbre :as log]))

;; --- Build info (baked at build time, git fallback for dev) — mirror views.clj ---
(def ^:private git-sha
  (try (clojure.string/trim (slurp (clojure.java.io/resource "build-sha.txt")))
       (catch Exception _
         (try (.trim (slurp (.getInputStream (.exec (Runtime/getRuntime) "git rev-parse --short HEAD"))))
              (catch Exception _ "dev")))))

(def ^:private build-time
  (try (java.time.Instant/parse (clojure.string/trim (slurp (clojure.java.io/resource "build-time.txt"))))
       (catch Exception _ (java.time.Instant/now))))

(defn- time-ago [^java.time.Instant inst]
  (let [mins (.toMinutes (java.time.Duration/between inst (java.time.Instant/now)))]
    (cond
      (< mins 1)    "just now"
      (< mins 60)   (str mins "m ago")
      (< mins 1440) (str (quot mins 60) "h ago")
      :else         (str (quot mins 1440) "d ago"))))

;; seat (string) -> vector of pending items. Two kinds: :messages, :replies.
(defonce channel-state (atom {:messages {} :replies {}}))

;; Bounded, append-only-within-the-window client playback telemetry.
;; reply-id -> vector of immutable ACK events. This is intentionally independent of
;; the claim-once reply queues: an ACK observes playback and can never deliver, drain,
;; delete, or otherwise mutate a reply. Every accepted ACK is also emitted as a
;; structured server log, so container replacement does not erase the durable receipt.
(defonce reply-acks (atom {}))

(def ^:private reply-ack-replies-cap 2000)
(def ^:private reply-ack-events-cap 20)

(defn append-reply-ack
  "Append one ACK while bounding live memory. Keeps the newest events per reply and
   the most recently observed replies globally; structured logs remain the durable log."
  [acks reply-id ack]
  (let [updated (assoc acks reply-id
                       (->> (conj (vec (get acks reply-id)) ack)
                            (take-last reply-ack-events-cap)
                            vec))]
    (if (<= (count updated) reply-ack-replies-cap)
      updated
      (->> updated
           (sort-by (fn [[_ events]] (:received-ts (last events))))
           (take-last reply-ack-replies-cap)
           (into {})))))

;; --- Server-owned single-seat conversation state (the /bridge v1 write path) ---
;; Born as /bridge2's Datastar SSE proving ground (that page was deleted 2026-07-26 —
;; zero traffic in 7 days). The atom stays: it is the seatless conversation mirror
;; that handle-bridge-dictate / handle-bridge-say / handle-post-reply still write,
;; and it is what bridge3's per-seat convos were modeled on.
(def ^:private convo-cap 50)

(defonce bridge-convo (atom []))  ; vec of {:role "you"|"bridge" :text String :ts iso}

(defn convo-append
  "Pure: append a turn, keeping only the last `convo-cap`."
  [convo turn]
  (let [v (conj (vec convo) turn)]
    (if (> (count v) convo-cap) (subvec v (- (count v) convo-cap)) v)))

(defn convo-working?
  "Derived, SERVER-SIDE: are we awaiting a bridge reply? True iff the last turn is a
   'you' turn (i.e. Gene spoke/typed and bridge hasn't answered after it yet). This
   is the server-driven version of the spinner — it survives a page reload."
  [convo]
  (= "you" (:role (last convo))))

;; --- dictation log (the durable diagnosability surface) ---
;; An APPEND-ONLY record of every dictation/typed turn the phone produced AND its outcome,
;; showing "every recording + its state" — crucially including the
;; FAILURES, which previously left NO server record at all (the observability gap). Mirrors
;; bridge-convo: a capped vector, persisted to disk so it survives a Cloud Run cold start
;; (mirror of the inbox's persist/load). Each record:
;;   {:id <dedup_id|server-uuid> :ts <iso> :transcribed? bool :transcript String|nil
;;    :error String|nil :enqueued? bool :answered-ts iso|nil}
;; :answered-ts is the ONE allowed projection upsert (stamped once, guarded by nil?); every
;; other field of a record is immutable once appended.
(def ^:private dictations-cap 200)

(defonce dictations (atom []))

(defn dictation-append
  "Pure: append a dictation record, keeping only the last `dictations-cap`."
  [ds rec]
  (let [v (conj (vec ds) rec)]
    (if (> (count v) dictations-cap) (subvec v (- (count v) dictations-cap)) v)))

(defn dictation-append-failure
  "Pure: append a FAILURE record UNLESS the latest record already logged for this id is an
   unanswered failure. A retryable 5xx makes the client retry the SAME dedup_id every ~18s
   forever; without this guard each attempt would append an identical failure and the 200-cap
   would evict all real history. So a server-seen failure is logged ONCE per id (until success
   supersedes it). Still append-only — we only ever ADD, never mutate a prior record."
  [ds {:keys [id] :as rec}]
  (let [latest (last (filter #(= id (:id %)) ds))]
    (if (and latest (false? (:transcribed? latest)) (nil? (:answered-ts latest)))
      (vec ds)
      (dictation-append ds rec))))

(defn dictation-mark-answered
  "Pure: stamp :answered-ts on the MOST-RECENT record that has none yet (the lone allowed
   projection upsert — guarded by nil? so an existing stamp is NEVER overwritten). With no
   unanswered record, returns the log unchanged."
  [ds ts]
  (let [idx (->> (map-indexed vector ds)
                 (filter (fn [[_ r]] (nil? (:answered-ts r))))
                 (map first)
                 last)]
    (if idx (assoc (vec ds) idx (assoc (nth ds idx) :answered-ts ts)) (vec ds))))

(defn dictations-latest-by-id
  "Read-time projection: newest-first, ONE row per :id (the latest append for that id wins,
   so a success-after-failure / retry-duplicate collapses to a single row). The append-only
   log underneath is untouched; this only shapes the display."
  [ds]
  (:out (reduce (fn [{:keys [seen out]} d]
                  (if (contains? seen (:id d))
                    {:seen seen :out out}
                    {:seen (conj seen (:id d)) :out (conj out d)}))
                {:seen #{} :out []}
                (reverse ds))))

;; Persistence — mirror server.clj's persist-inbox!/load-persisted-inbox! so the log survives
;; a Cloud Run cold start. Load on namespace require; persist on every change via a watch.
(def ^:private dictations-state-file "bridge-dictations.edn")

(defn- persist-dictations! []
  (try
    (blob/save-edn! dictations-state-file @dictations)
    (catch Exception e (log/warn :dictations-persist-failed :error (.getMessage e)))))

(defn- load-persisted-dictations! []
  (try
    (when-let [ds (blob/load-edn dictations-state-file)]
      (when (vector? ds) (reset! dictations ds))
      (log/info :dictations-restored :count (count ds)))
    (catch Exception e (log/warn :dictations-restore-failed :error (.getMessage e)))))

;; Load FIRST (defonce, once per JVM), THEN install the persist watch — so the restoring
;; reset! doesn't fire a redundant persist of what we just read.
(defonce ^:private dictations-loaded (do (load-persisted-dictations!) true))
(defonce ^:private dictations-persist-installed
  (do (add-watch dictations ::persist
                 (fn [_ _ old new] (when (not= old new) (persist-dictations!))))
      true))

;; --- pure queue ops (unit-tested) ---
(defn enqueue
  "Pure: append item to state[kind][seat]."
  [state kind seat item]
  (update-in state [kind seat] (fnil conj []) item))

(defn drain
  "Pure: return [new-state drained-items] for state[kind][seat], leaving [] behind."
  [state kind seat]
  [(assoc-in state [kind seat] []) (get-in state [kind seat] [])])

;; --- atomic drain against the live atom (claim-once even under concurrency) ---
(defn drain!
  [kind seat]
  (let [[old _] (swap-vals! channel-state assoc-in [kind seat] [])]
    (get-in old [kind seat] [])))

;; --- auth ---
(defn bearer-matches?
  "Pure: does the request's Authorization header carry exactly `Bearer <expected>`?"
  [req expected]
  (and (some? expected)
       (= (get-in req [:headers "authorization"]) (str "Bearer " expected))))

(defn token-ok?
  "Bearer-token check. Configured token must match; if no token is configured the
   endpoint is open only in dev (so local `make run` works without secrets)."
  [req]
  (let [expected (System/getenv "CHANNEL_TOKEN")]
    (if (nil? expected)
      (= "dev" (System/getenv "ENV"))
      (bearer-matches? req expected))))

;; --- helpers ---
(defn- json-response [m & [status]]
  {:status (or status 200) :headers {"Content-Type" "application/json"} :body (json/write-str m)})

(defn dictate-client-status
  "Pure: the HTTP status a FAILED transcription answers with — 400 when the
   REQUEST was the problem, 500 when this server was.

   THE RULE IS A RETRY INSTRUCTION, not a taxonomy (Gene, 2026-06-29). Every
   uploader on this app — the legacy IndexedDB queue, the reducer's bounded
   ladder — reads it that way: a 4xx marks the record permanently failed and
   stops, a 5xx keeps it pending and tries again. So a status here is a promise
   about whether trying again could ever work.

   TWO WAYS TO BE A CLIENT ERROR, and the second one is Gene's 2026-07-28 fix.
   A Groq 4xx (bad, unsupported, or too-short audio) is one. The other is a
   refusal this server made BEFORE Groq was ever called — `groq/transcribe-audio`
   rejecting a sub-kilobyte file as \"No audio captured (recording too short)\" —
   which carries `:client-error? true` and no `:status`, because Groq never saw
   the request and inventing a Groq status for it would put a lie in the log.
   That refusal used to fall through to 500, so an empty capture was reported to
   every uploader as \"the server broke, try again\" — and they did, three times,
   with the same permanently empty bytes."
  [status client-error?]
  (if (or client-error? (and (integer? status) (<= 400 status 499))) 400 500))

(defn- body-json [req]
  (try (json/read-str (slurp (:body req)) :key-fn keyword) (catch Exception _ nil)))

(defn- seat-param [req]
  (or (get-in req [:params :seat]) (get-in req [:query-params "seat"])))

(def ^:private max-audio-duration-ms (* 24 60 60 1000))

(defn- audio-duration-ms-param
  "Parse the optional capture duration without letting a malformed client poison
   an otherwise valid dictation. The 24-hour ceiling makes logs safely summable."
  [req]
  (try
    (let [value (get-in req [:params :audio_duration_ms])
          duration-ms (when (some? value) (Long/parseLong (str value)))]
      (when (and duration-ms (<= 0 duration-ms max-audio-duration-ms))
        duration-ms))
    (catch Exception _ nil)))

(defn- now-iso [] (str (java.time.Instant/now)))

(defn- new-reply-id [] (str (java.util.UUID/randomUUID)))

(defn- valid-reply-id? [reply-id]
  (and (string? reply-id) (not (clojure.string/blank? reply-id)) (<= (count reply-id) 256)))

(defn reply-acks-snapshot
  "Read-time per-reply projection over the append-only ACK event log."
  [acks]
  (into {}
        (for [[reply-id events] acks]
          [reply-id
           (reduce (fn [state {:keys [event mode ts received-ts]}]
                     (cond-> (assoc state
                                    :last-event event
                                    :last-event-ts ts
                                    :last-mode mode
                                    :last-received-ts received-ts)
                       (= "started" event)
                       (assoc :started? true
                              :started-ts (or (:started-ts state) ts)
                              :last-started-ts ts)

                       (= "finished" event)
                       (assoc :finished? true
                              :finished-ts ts)))
                   {:started? false :finished? false :events events}
                   events)])))

;; --- observability (kickass #30): no message content ---
;; Per-seat throughput, last-seen timestamps, global auth failures, and bounded reply-ID
;; playback receipts stop the relay being a black box. Read via GET /api/channel/stats.
;; Current backlog is derived live from channel-state at read time (not a counter).
(defonce metrics (atom {:auth-fails 0 :seats {}}))

(defn- bump!
  ([path] (bump! path 1))
  ([path n] (swap! metrics update-in path (fnil + 0) n)))

(defn- mark-seen! [seat k] (swap! metrics assoc-in [:seats seat k] (now-iso)))

(defn stats-snapshot
  "Pure-ish: merge the throughput counters with the live backlog from channel-state.
   Message content is NEVER included — only counters, reply IDs, events, and timestamps."
  ([metrics-val chan-state] (stats-snapshot metrics-val chan-state {}))
  ([metrics-val chan-state acks]
   (let [seats (into #{} (concat (keys (:seats metrics-val))
                                 (keys (:messages chan-state))
                                 (keys (:replies chan-state))))]
     {:auth-fails (:auth-fails metrics-val 0)
      :reply-acks (reply-acks-snapshot acks)
      :seats (into {}
                   (for [s seats]
                     [s (merge {:msg-in 0 :msg-drained 0 :reply-in 0 :reply-drained 0}
                               (get-in metrics-val [:seats s])
                               {:backlog-messages (count (get-in chan-state [:messages s] []))
                                :backlog-replies  (count (get-in chan-state [:replies s] []))})]))})))

;; --- handlers (referenced as #'channel/... in the route table) ---
(defn handle-post-message [req]
  (if-not (token-ok? req)
    (do (bump! [:auth-fails]) (log/warn :channel-auth-fail :endpoint "post-message")
        (json-response {:error "unauthorized"} 401))
    (let [{:keys [seat from text id]} (body-json req)]
      (if (and seat text)
        (do (swap! channel-state enqueue :messages seat
                   {:from from :text text :id id :ts (now-iso)})
            (bump! [:seats seat :msg-in]) (mark-seen! seat :last-msg-ts)
            (json-response {:ok true}))
        (json-response {:error "seat and text required"} 400)))))

(defn handle-get-messages [req]
  (if-not (token-ok? req)
    (do (bump! [:auth-fails]) (json-response {:error "unauthorized"} 401))
    (if-let [seat (seat-param req)]
      (let [msgs (drain! :messages seat)]
        (when (seq msgs) (bump! [:seats seat :msg-drained] (count msgs)))
        (json-response {:messages msgs}))
      (json-response {:error "seat required"} 400))))

(defn handle-post-reply [req]
  (if-not (token-ok? req)
    (do (bump! [:auth-fails]) (log/warn :channel-auth-fail :endpoint "post-reply")
        (json-response {:error "unauthorized"} 401))
    (let [{:keys [seat text reply_to files reply_id id]} (body-json req)]
      (if (and seat text)
        (let [reply-ts (now-iso)
              stable-id (str (or reply_id id (new-reply-id)))
              reply (cond-> {:seat seat :text text :ts reply-ts :reply_id stable-id}
                      reply_to (assoc :reply_to reply_to)
                      (seq files) (assoc :files files))]
          (swap! channel-state enqueue :replies seat reply)
          ;; ADDITIVE: a reply for seat=bridge is a "bridge" turn in the seatless
          ;; server-owned conversation (clears the working spinner). Does not change
          ;; the reply queue or the return — purely additive mirror.
          (when (= seat "bridge")
            (swap! bridge-convo convo-append {:role "bridge" :text text :ts reply-ts
                                              :reply_id stable-id})
            ;; DICTATION LOG: a bridge reply ANSWERS the most-recent unanswered dictation —
            ;; the lone allowed projection upsert (stamps :answered-ts only when nil, never
            ;; overwriting). Stamps the ✓ answered marker on the dictation log.
            (swap! dictations dictation-mark-answered (now-iso)))
          (bump! [:seats seat :reply-in]) (mark-seen! seat :last-reply-ts)
          (json-response {:ok true :reply_id stable-id}))
        (json-response {:error "seat and text required"} 400)))))

(defn handle-reply-ack [req]
  (if-not (or (token-ok? req) (auth/authenticated? req))
    (do (bump! [:auth-fails])
        (log/warn :channel-auth-fail :endpoint "reply-ack")
        (json-response {:error "unauthorized"} 401))
    (let [{:keys [reply_id event mode ts]} (body-json req)]
      (if (and (valid-reply-id? reply_id)
               (#{"started" "finished"} event)
               (#{"tts-audio" "speech-synthesis"} mode))
        (let [received-ts (now-iso)
              ack {:event event
                   :mode mode
                   :ts (if (and (string? ts) (<= (count ts) 64)) ts received-ts)
                   :received-ts received-ts}]
          (swap! reply-acks append-reply-ack reply_id ack)
          ;; S4 TAP — the RICHEST semantic-trace source the server has: a real
          ;; :playback-started / :playback-finished WITH reply identity (P10: a
          ;; reply counts as played only on an acknowledged complete artifact).
          (shadow/record! (or (seat-param req) "bridge")
                          (if (= "started" event) :playback-started :playback-finished)
                          {:source :reply-ack
                           :ids    {:reply reply_id}
                           :live   {:mode mode :client-ts (:ts ack)}})
          (log/info :reply-playback-ack :reply-id reply_id :event event
                    :mode mode :client-ts (:ts ack) :received-ts received-ts)
          (json-response {:ok true}))
        (json-response {:error "reply_id, event (started|finished), and playback mode required"} 400)))))

(defn handle-get-replies [req]
  (if-not (token-ok? req)
    (do (bump! [:auth-fails]) (json-response {:error "unauthorized"} 401))
    (if-let [seat (seat-param req)]
      (let [reps (drain! :replies seat)]
        (when (seq reps) (bump! [:seats seat :reply-drained] (count reps)))
        (json-response {:replies reps}))
      (json-response {:error "seat required"} 400))))

(defn handle-channel-stats
  "GET -> scrubbed relay metrics (per-seat throughput + live backlog + auth failures).
   Counts/timestamps only, never message content. Bearer-gated like the other endpoints."
  [req]
  (if-not (token-ok? req)
    (json-response {:error "unauthorized"} 401)
    (json-response (stats-snapshot @metrics @channel-state @reply-acks))))

;; --- Browser-facing direct-to-bridge dictation (cookie-authed, NOT bearer) ---
;; A one-tap page pinned to seat=bridge so Gene can dictate from the car with no
;; seat dropdown. record -> STT -> enqueue messages[bridge]; poll replies[bridge] -> browser TTS.

;; IDEMPOTENCY for the offline persist-and-retry queue (Gene 2026-06-29): the client
;; saves each recording to IndexedDB and retries indefinitely until a 2xx with a
;; transcript comes back. If the server committed the turn but the RESPONSE was lost in
;; a dead zone, the client retries the SAME dedup_id — without dedup that double-creates
;; the 'you' turn. We remember dedup_id -> transcript (bounded) so a retry of an
;; already-processed capture returns the SAME transcript and short-circuits (no second
;; enqueue, no re-transcribe). DO NOTHING is correct here: a request_id is a true
;; immutable idempotency ledger entry (not a re-derivable fact being dropped).
(defonce ^:private bridge-dictate-seen (atom {:order [] :m {}})) ; dedup_id -> transcript
(defn- dictate-remember
  "Bounded remember of dedup_id -> transcript (most-recent ~64). Pure fn over the atom value."
  [{:keys [order m]} id transcript]
  (let [order' (conj order id)
        m'     (assoc m id transcript)]
    (if (> (count order') 64)
      {:order (vec (rest order')) :m (dissoc m' (first order'))}
      {:order order' :m m'})))

(defn handle-bridge-dictate
  "POST multipart audio -> Groq STT -> enqueue to messages[\"bridge\"].
   Optional `dedup_id` param (additive): a client request-id so a retry of a capture
   the server already processed returns the cached transcript instead of double-creating."
  [req]
  (let [dedup-id (get-in req [:params :dedup_id])
        cached   (when (seq dedup-id) (get-in @bridge-dictate-seen [:m dedup-id]))]
    (if cached
      ;; Retry of an already-processed capture — return the SAME transcript, no re-enqueue.
      (do (log/info :bridge-dictate-dup :id dedup-id)
          (json-response {:ok true :transcript cached :dedup true}))
      (let [audio (get-in req [:params :audio])
            tmp   (:tempfile audio)
            ;; Stable record id for the dictation log: the client's dedup_id when present,
            ;; else a server-minted uuid. Same id across this request's success/failure
            ;; appends so the log de-dups correctly by id.
            rec-id (or (when (seq dedup-id) dedup-id) (str (java.util.UUID/randomUUID)))]
        (if-not tmp
          (json-response {:error "audio required"} 400)
          ;; Groq rejects the multipart `.tmp` filename by extension — copy the upload
          ;; to a properly-named temp file (extension from the client filename, default .webm).
          (let [ext   (or (some->> (:filename audio) (re-find #"\.[A-Za-z0-9]+$")) ".webm")
                named (java.io.File/createTempFile "bridge-dictate-" ext)]
            (try
              (io/copy tmp named)
              (let [{:keys [transcript]} ((requiring-resolve 'marvin-voice-remote.groq/transcribe-audio) (.getPath named))]
                (if (clojure.string/blank? transcript)
                  ;; PERMANENT (Gene 2026-06-29): a silent / too-short capture transcribes to
                  ;; nothing. Return 400 so the client marks it FAILED (not pending-forever) and
                  ;; do NOT enqueue an empty 'you' turn. Distinguishable from a retryable 5xx.
                  (do (log/warn :bridge-dictate-empty :id dedup-id)
                      ;; DICTATION LOG (failure): record the rejected capture so it shows up on
                      ;; the dictation log as 'not transcribed' instead of vanishing silently.
                      (swap! dictations dictation-append-failure
                             {:id rec-id :ts (now-iso) :transcribed? false :enqueued? false
                              :error "no speech detected (audio too short or silent)" :answered-ts nil})
                      (json-response {:error "no speech detected (audio too short or silent)"} 400))
                  (do
                    (swap! channel-state enqueue :messages "bridge"
                           {:from "gene" :text transcript :ts (now-iso)})
                    ;; ADDITIVE: mirror this turn into the server-owned seatless conversation
                    ;; (watch on bridge-convo fires the SSE push). Does not change the return.
                    (swap! bridge-convo convo-append {:role "you" :text (str transcript) :ts (now-iso)})
                    ;; Record the dedup_id->transcript so a lost-response retry is idempotent.
                    (when (seq dedup-id)
                      (swap! bridge-dictate-seen dictate-remember dedup-id (str transcript)))
                    ;; DICTATION LOG (success): append the transcribed+enqueued turn keyed by
                    ;; rec-id (a later success with the same id supersedes any prior failure row).
                    (swap! dictations dictation-append
                           {:id rec-id :ts (now-iso) :transcribed? true :transcript (str transcript)
                            :enqueued? true :error nil :answered-ts nil})
                    (log/info :bridge-dictate :chars (count (str transcript)))
                    (json-response {:ok true :transcript transcript}))))
              (catch Exception e
                ;; Surface WHY: ex-info from groq carries {:status :body} on a Groq non-2xx.
                ;; CATEGORIZE for the client retry queue (Gene 2026-06-29): a Groq 4xx (bad/short/
                ;; unsupported audio) is PERMANENT — return 4xx so the client stops retrying it;
                ;; a 5xx / timeout / network error is RETRYABLE — return 500 so the client retries.
                (let [{:keys [status body client-error?]} (ex-data e)
                      client-status (dictate-client-status status client-error?)]
                  (log/error :bridge-dictate-error :err (.getMessage e) :status status :body body :client-status client-status)
                  ;; DICTATION LOG (failure): the server SAW this capture and rejected it (Groq 4xx
                  ;; permanent / 5xx retryable). Logged ONCE per id (dictation-append-failure guards
                  ;; the 5xx retry-loop) so it surfaces in the dictation log as 'not transcribed'.
                  (swap! dictations dictation-append-failure
                         {:id rec-id :ts (now-iso) :transcribed? false :enqueued? false
                          :error (.getMessage e) :answered-ts nil})
                  (json-response {:error (.getMessage e) :groq-status status :groq-body body} client-status)))
              (finally (.delete named)))))))))

(defn handle-bridge-replies
  "GET -> drain replies[\"bridge\"] for the phone UI."
  [_req]
  (json-response {:replies (drain! :replies "bridge")}))

(defn handle-bridge-say
  "POST {text} -> enqueue TYPED text to messages[\"bridge\"] (type instead of tap-to-talk)."
  [req]
  (let [{:keys [text]} (body-json req)]
    (if-not (seq text)
      (json-response {:error "text required"} 400)
      (do (swap! channel-state enqueue :messages "bridge" {:from "gene" :text text :ts (now-iso)})
          ;; ADDITIVE: mirror typed turn into the server-owned seatless conversation.
          (swap! bridge-convo convo-append {:role "you" :text text :ts (now-iso)})
          ;; DICTATION LOG: a typed turn is transcribed (trivially) + enqueued. Server-minted id.
          (swap! dictations dictation-append
                 {:id (str (java.util.UUID/randomUUID)) :ts (now-iso) :transcribed? true
                  :transcript text :enqueued? true :error nil :answered-ts nil})
          (log/info :bridge-say :chars (count text))
          (json-response {:ok true})))))

;; ElevenLabs voice for /bridge readback. SERVER-OWNED selection (Datastar
;; doctrine: the "Other Voices" dropdown is just an input; the chosen voice lives HERE,
;; survives reload, and drives every subsequent reply readback). Defaults to Tom Clark —
;; the long-standing bridge readback voice — so nothing changes until Gene picks another.
;; Voice catalog is tts/voices: Alexander, Hayes, Matt, Jude, Hugh, Tom Clark.
(defonce ^:private bridge-voice-id (atom "mZ2wXweMuluxNdZUBBh7")) ; default: Tom Clark

;; PER-SEAT voice overrides (eer): one global atom meant any tab changing 🔊 voice
;; changed EVERY seat's readback mid-conversation. Seat pages (bridge3/4) read+write
;; their seat's entry here; the seatless legacy /bridge page keeps the global.
(defonce ^:private bridge-seat-voices (atom {}))

(defn- voice-for-seat
  "The effective readback voice for a seat: per-seat override, else the global default."
  [seat]
  (or (when (seq (str seat)) (get @bridge-seat-voices seat))
      @bridge-voice-id))

;; SERVER-OWNED conversation text size (Datastar doctrine: the "Text Size"
;; dropdown is just an input; the chosen size lives HERE, survives reload). Applied via
;; the --msg-size CSS var on <body> (body is never morphed, so SSE #convo updates keep it).
;; Values are the allowed CSS pixel sizes; default 19px = "Large" so it's readable without
;; glasses (Gene 2026-06-26). 16px = the old default ("Normal").
(def ^:private bridge-text-sizes
  [["16px" "Normal"] ["19px" "Large"] ["23px" "Larger"]])
(defonce ^:private bridge-text-size (atom "19px")) ; default: Large

;; Cache synthesized /bridge TTS by text. Two reasons: (1) seeking (±10s) makes the
;; browser issue HTTP Range requests for the SAME clip — without a cache each Range
;; would re-hit ElevenLabs (slow, costly, and the bytes could differ, breaking the
;; seek); (2) replays reuse the clip for free. Bounded to the most recent clips.
(defonce ^:private bridge-tts-cache (atom {:order [] :m {}}))

(defn- tts-sanitize
  "Neutralize markup before TTS so a reply containing '<' (e.g. '</dev/null') doesn't
   TRUNCATE the spoken narration (bug 3zg — '<' is read as the start of a tag and swallows
   the rest). Drop whole tag-looking spans, replace stray angle brackets with spoken words so
   meaning survives ('x < 5' -> 'x less than 5'), and collapse whitespace. Applied at the
   single chokepoint every page's TTS goes through (/api/bridge/tts)."
  [s]
  (-> (str s)
      (clojure.string/replace "<" " less than ")
      (clojure.string/replace ">" " greater than ")
      (clojure.string/replace #"\s+" " ")
      clojure.string/trim))

;; INTENT: TTS-VERIFIED-COMPLETE-i37
(defn- bridge-tts-bytes
  "The promoted artifact for one reply, cached by [voice text].

   P10 / THE i37 TRUNCATION SCAR. A 200 from ElevenLabs can still carry a
   TRUNCATED mp3 — the sentence stops halfway, the media element fires `ended`
   with no error, and the session records `playback-finished` for a reply Gene
   only half heard. So the artifact is verified against the length the upstream
   DECLARED (`tts/verify-complete`) BEFORE it may enter this cache, and a
   truncated one is thrown as a typed failure rather than promoted: caching it
   would make one bad synthesis permanent for every later replay of the same
   text, which is the difference between a bad minute and a bad artifact."
  [text' seat]
  (let [text (tts-sanitize text')]
  ;; Cache key is [voice-id text]: the SAME text in a NEWLY-picked voice must NOT return
  ;; the previous voice's bytes, so the cache key carries the effective voice (per-seat
  ;; override when ?seat= is present — eer — else the global default).
    (let [vid (voice-for-seat seat)
          k   [vid text]]
      (or (get-in @bridge-tts-cache [:m k])
          (let [{:keys [audio-bytes declared-length]}
                ((requiring-resolve 'marvin-voice-remote.tts/synthesize-speech)
                 text :voice-id vid :speed 1.1)
                actual (when audio-bytes (alength ^bytes audio-bytes))]
            (when-let [bad ((requiring-resolve 'marvin-voice-remote.tts/verify-complete)
                            declared-length actual)]
              ;; NOT CACHED, and that ordering is the point: the throw happens
              ;; before the swap, so a truncated artifact never becomes the
              ;; answer to the next request for the same words.
              (throw (ex-info "playback-truncated" bad)))
            (swap! bridge-tts-cache
                   (fn [{:keys [order m]}]
                     (let [order' (conj order k)
                           m'     (assoc m k audio-bytes)]
                       (if (> (count order') 12)
                         {:order (vec (rest order')) :m (dissoc m' (first order'))}
                         {:order order' :m m'}))))
            audio-bytes)))))

(defn- parse-byte-range
  "Parse a `Range: bytes=START-END` header into [start end] (inclusive), clamped to
   the resource length. Returns nil if absent/unsatisfiable so the caller sends 200."
  [hdr len]
  (when hdr
    (when-let [[_ s e] (re-matches #"bytes=(\d*)-(\d*)" hdr)]
      (let [start (if (seq s) (parse-long s) 0)
            end   (if (seq e) (min (parse-long e) (dec len)) (dec len))]
        (when (and start end (<= 0 start end) (< start len)) [start end])))))

(defn handle-bridge-tts
  "GET ?text=... -> ElevenLabs mp3 in the chosen Marvin voice (for the /bridge page).
   Sends Content-Length + Accept-Ranges and honors Range requests so the page's
   ±10s seek (and iOS's media-element seeking) actually work — a streamed body with
   no length is not seekable, which is why the buttons did nothing.

   AND IT REFUSES A TRUNCATED ARTIFACT (P10, the i37 scar). If the bytes disagree
   with the length the upstream declared, this answers HTTP 502 with a TYPED
   `playback-truncated` body and never 200 — because a 200 carrying half a
   sentence is indistinguishable, to the media element and therefore to the
   reducer, from a reply that played fine. A 502 makes the element `error`, which
   is the P11 path: a typed failure the session can show. `playback-truncated`
   must never be able to become `playback-finished`."
  [req]
  (let [text (get-in req [:params :text])
        seat (get-in req [:params :seat])
        reply-id (get-in req [:params :reply_id])]
    (if-not (seq text)
      (json-response {:error "text required"} 400)
      (try
        (let [bytes (bridge-tts-bytes text seat)
              len   (alength ^bytes bytes)
              rng   (parse-byte-range (get-in req [:headers "range"]) len)
              ;; S4 TAP — evidence a client INTENDED to play this reply. NOT
              ;; evidence that audio played: only /api/channel/reply-ack proves
              ;; that (P10/S13, and the i37 truncation scar is exactly the gap
              ;; between the two). Recorded as a :server/ observation so it can
              ;; never be mistaken for a semantic :playback-started.
              _     (shadow/record! seat :server/tts-bytes-served
                                    {:source :bridge-tts
                                     :ids    {:reply reply-id}
                                     :live   {:bytes len :range (boolean rng)}})]
          (if rng
            (let [[start end] rng]
              {:status 206
               :headers {"Content-Type" "audio/mpeg"
                         "Accept-Ranges" "bytes"
                         "Content-Range" (str "bytes " start "-" end "/" len)
                         "Content-Length" (str (inc (- end start)))}
               :body (java.util.Arrays/copyOfRange ^bytes bytes (int start) (int (inc end)))})
            {:status 200
             :headers {"Content-Type" "audio/mpeg"
                       "Accept-Ranges" "bytes"
                       "Content-Length" (str len)}
             :body bytes}))
        (catch Exception e
          (let [{:error/keys [kind] :keys [declared actual]} (ex-data e)]
            (if (= :playback-truncated kind)
              (do
                (log/error :bridge-tts-truncated :seat seat :reply-id reply-id
                           :declared declared :actual actual :chars (count text))
                ;; The shadow records the REFUSAL, so the artifact that was never
                ;; served still leaves a receipt (S13: a failure that is only
                ;; absent is a failure nobody can count).
                (shadow/record! seat :server/tts-artifact-refused
                                {:source :bridge-tts
                                 :ids    {:reply reply-id}
                                 :live   {:reason :playback-truncated
                                          :declared declared :actual actual}})
                (json-response {:error "playback-truncated"
                                :declared declared :actual actual}
                               502))
              (do
                (log/error :bridge-tts-error :err (.getMessage e) :seat seat
                           :reply-id reply-id :chars (count text) :cause (ex-data e))
                (json-response {:error (.getMessage e)} 500)))))))))

(defn handle-bridge-voice
  "POST voice_id=<elevenlabs id> -> set the SERVER-owned /bridge readback voice. The
   The 'Other Voices' dropdown POSTs here; the chosen voice lives in `bridge-voice-id`
   (server state, Datastar doctrine — not a client variable) and drives every subsequent
   reply readback. Unknown ids are rejected so a typo can't silently break TTS. Returns
   204 (POST-and-forget); the page already reflects the choice and a reload re-renders it
   from server state via the <option selected> built into the page."
  [req]
  (let [vid   (get-in req [:params :voice_id])
        seat  (some-> (get-in req [:params :seat]) clojure.string/trim)
        valid (set (map :id (var-get (requiring-resolve 'marvin-voice-remote.tts/voices))))]
    (if (and vid (valid vid))
      (do (if (and (seq seat) (re-matches #"[A-Za-z0-9_-]{1,40}" seat))
            ;; seat pages (bridge3/4) scope the choice to THEIR seat (eer)
            (swap! bridge-seat-voices assoc seat vid)
            ;; the seatless legacy /bridge page sets the global default
            (reset! bridge-voice-id vid))
          (log/info :bridge-voice-selected :id vid :seat (or seat "global"))
          {:status 204 :body ""})
      (json-response {:error "unknown voice_id"} 400))))

(defn handle-bridge-text-size
  "POST size=<px> -> set the SERVER-owned conversation text size. The 'Text Size'
   dropdown POSTs here; the chosen size lives in `bridge-text-size` (server state) and a
   reload re-renders it. Validated against `bridge-text-sizes` so a junk value can't break
   the layout. 204 (POST-and-forget); the page already applied it via the --msg-size var."
  [req]
  (let [sz    (get-in req [:params :size])
        valid (set (map first bridge-text-sizes))]
    (if (and sz (valid sz))
      (do (reset! bridge-text-size sz)
          (log/info :bridge-text-size-selected :size sz)
          {:status 204 :body ""})
      (json-response {:error "unknown size"} 400))))

;; ============================================================================
;; Shared HTML/SSE-fragment escaping helpers + the version badge.
;; These were born with /bridge2 (deleted 2026-07-26 — zero traffic in 7 days) and
;; are now used by /bridge (v1), /bridge3, /bridge4 and /voice-lab.
;; ============================================================================

(defn- html-escape [s]
  (-> (str s)
      (clojure.string/replace "&" "&amp;")
      (clojure.string/replace "<" "&lt;")
      (clojure.string/replace ">" "&gt;")))

(defn- attr-escape
  "Escape text for a double-quoted HTML attribute (data-text). &#10; for newlines is
   CRITICAL: these fragments travel in SSE data: fields where a raw newline truncates
   the frame. Client getAttribute() decodes entities back to the original text."
  [s]
  (-> (html-escape s)
      (clojure.string/replace "\"" "&quot;")
      (clojure.string/replace "\r" "")
      (clojure.string/replace "\n" "&#10;")))

(defn- convo-text
  "Escape a turn's text AND convert newlines to <br>. CRITICAL for SSE: a `data:` field
   cannot contain a raw newline — the newline ends the field and truncates the fragment on
   the wire (page-load HTML looks fine since HTML ignores newlines; only the live push cut)."
  [s]
  (-> (html-escape s)
      (clojure.string/replace "\r\n" "<br>")
      (clojure.string/replace "\n" "<br>")
      (clojure.string/replace "\r" "<br>")))

(defn- bridge2-version-fragment []
  (str "<div id=\"ver\">deployed " (time-ago build-time)
       "<br><span class=sha>v" git-sha "</span></div>"))

(defn- bridge-page-html
  "Build the /bridge page HTML, injecting build sha + relative deploy time
   (version badge) at render time. Mirrors the /voice version indicator."
  []
  (str "<!doctype html><html><head><meta charset=utf-8>"
       "<meta name=viewport content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
       "<title>Talk to Bridge</title><style>"
       "*{box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;margin:0;padding:16px;"
       "background:#f7f7f8;color:#1a1a1a}h1{font-size:20px;margin:8px 0 16px}"
       "#rec{width:100%;height:140px;font-size:26px;font-weight:600;border:none;border-radius:18px;"
       "color:#fff;background:#2563eb;-webkit-tap-highlight-color:transparent}#rec.on{background:#dc2626}"
       "#status{margin:14px 2px;font-size:15px;color:#555;min-height:20px}"
       ".msg{margin:10px 0;padding:12px 14px;border-radius:12px;font-size:16px;line-height:1.35}"
       ".me{background:#e7eefe}.bridge{background:#e9f7ee}.lbl{font-size:11px;color:#888;text-transform:uppercase;letter-spacing:.04em}"
       "#ctl{display:flex;gap:8px;margin:12px 0;flex-wrap:wrap}.c{flex:1;min-width:64px;height:50px;font-size:15px;font-weight:600;border:none;border-radius:10px;background:#e5e7eb;color:#111;-webkit-tap-highlight-color:transparent}"
       ;; version badge (top-right, subtle, non-interactive) + status/heartbeat line
       "#ver{position:fixed;top:6px;right:8px;font-size:18px;font-weight:700;color:#111;text-align:right;pointer-events:none;z-index:10;line-height:1.2}#ver .sha{font-size:11px;font-weight:400;color:#888}"
       "#meta{margin:6px 2px 2px;font-size:12px;color:#888;display:flex;align-items:center;gap:8px}"
       "#beat{display:inline-block;width:8px;height:8px;border-radius:50%;background:#cbd5e1;transition:background .15s}"
       "#beat.on{background:#22c55e}"
       "#type{display:flex;gap:8px;margin:10px 0}#txt{flex:1;font-size:16px;padding:10px;border:1px solid #ccc;border-radius:10px;resize:vertical}#send{padding:0 18px;font-size:15px;font-weight:600;border:none;border-radius:10px;background:#2563eb;color:#fff;-webkit-tap-highlight-color:transparent}"
       "</style></head><body>"
       ;; --- version indicator (server-injected sha + relative deploy time) ---
       "<div id=ver>deployed " (time-ago build-time) "<br><span class=sha>v" git-sha "</span></div>"
       "<h1>🌉 Talk to Bridge</h1>"
       "<button id=rec>● Tap to talk</button>"
       "<div id=status>Idle — tap, speak, tap again to send.</div>"
       "<div id=meta><span id=beat></span><span id=heard>Last heard from bridge: —</span></div>"
       "<div id=ctl><button class=c id=back>« 10s</button><button class=c id=fwd>10s »</button><button class=c id=replay>↺ replay</button><button class=c id=skip>⏭ skip</button></div>"
       "<div id=type><textarea id=txt rows=2 placeholder=\"…or type to bridge\"></textarea><button id=send>Send</button></div>"
       "<div id=log></div>"
       "<script>"
       "let mr,chunks=[],on=false,seen=new Set(),audio=new Audio(),queue=[],playing=false,recMime='',primed=false,lastReply='',lastHeardAt=0,lastSentAt=0,msgWaiting=0,cueAudio=new Audio();"
       ;; Recording is active if the latch is on OR the MediaRecorder is mid-capture.
       ;; While active, reply audio must NOT play (it would contaminate the mic).
       "function recordingActive(){return on===true||(mr&&mr.state==='recording');}"
       ;; iOS Safari's MediaRecorder records audio/mp4 (AAC), NOT webm. We let it use its
       ;; native default (forcing a mimeType broke capture) and map the real mr.mimeType to
       ;; the correct file extension — mislabeling an mp4 blob as webm makes Groq reject it.
       "function extFor(m){if(!m)return '.webm';if(m.indexOf('mp4')>=0)return '.mp4';if(m.indexOf('ogg')>=0)return '.ogg';return '.webm';}"
       "function unlock(){try{audio.src='data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=';audio.play().catch(()=>{});}catch(e){}}"
       ;; TWO CLASSES OF AUDIO ON iOS (instructional — kept on purpose, per Gene 2026-06-25):
       ;; (A) Web Speech `speechSynthesis` = `sayFallback` below. iOS treats this like a SYSTEM
       ;;     notification and routes it to the iOS *system speaker* — it does NOT follow the
       ;;     media route (it won't go to AirPods like music). Great demo of the distinction;
       ;;     retained as the fallback AND as a reference example.
       ;; (B) An <audio> element playing a real media file (our /api/bridge/tts ElevenLabs clip)
       ;;     rides the *media* channel — same path as the replies (AirPods/Bluetooth). This is
       ;;     what `say` uses now, so the cues match the reply voice and output.
       "function sayFallback(t){try{speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(t);u.rate=1.1;speechSynthesis.speak(u);}catch(e){}}"
       ;; Cues in the SAME ElevenLabs voice as replies (via /api/bridge/tts, server-cached so
       ;; instant after first use), on a SEPARATE cueAudio element so they don't disturb the
       ;; reply queue. Falls back to (A) the browser/system-speaker voice if play() fails.
       "function say(t){try{cueAudio.src='/api/bridge/tts?text='+encodeURIComponent(t);cueAudio.play().catch(()=>sayFallback(t));}catch(e){sayFallback(t);}}"
       ;; Prime speechSynthesis on the first user gesture — iOS needs a speak() from a
       ;; direct tap handler before any later speak() is allowed.
       "function prime(){if(primed)return;primed=true;try{speechSynthesis.resume();const u=new SpeechSynthesisUtterance(' ');u.volume=0;speechSynthesis.speak(u);}catch(e){}}"
       "const rec=document.getElementById('rec'),st=document.getElementById('status'),log=document.getElementById('log');"
       "const heardEl=document.getElementById('heard'),beat=document.getElementById('beat');"
       ;; Single source of truth for the always-visible status line.
       "function setStatus(t){st.textContent=t;}"
       "function add(cls,lbl,txt){const d=document.createElement('div');d.className='msg '+cls;"
       "d.innerHTML='<div class=lbl>'+lbl+'</div>'+txt.replace(/</g,'&lt;');log.prepend(d);}"
       "async function start(){const s=await navigator.mediaDevices.getUserMedia({audio:true});"
       ;; Let MediaRecorder pick its NATIVE default (iOS→mp4, Chrome→webm) — forcing an
       ;; explicit mimeType broke capture on iOS (recorded 0 bytes). Read mr.mimeType
       ;; AFTER construction to label the blob/extension with the real container.
       "chunks=[];mr=new MediaRecorder(s);recMime=mr.mimeType||'';"
       "mr.ondataavailable=e=>{if(e.data&&e.data.size>0)chunks.push(e.data);};"
       "mr.onstop=send;mr.start();on=true;rec.classList.add('on');rec.textContent='■ Tap to send';setStatus('Listening…');}"
       "async function send(){rec.classList.remove('on');rec.textContent='● Tap to talk';"
       ;; Guard: mediaRecorder may be missing (start() rejected) or no chunks captured.
       "if(!mr){setStatus('Mic not ready — tap to talk again.');return;}"
       "if(!chunks||chunks.length===0){setStatus('No audio captured — hold a moment and speak.');return;}"
       ;; Real container type from MediaRecorder (recMime may carry a `;codecs=` suffix — strip it).
       "const mime=(recMime||'audio/webm').split(';')[0];const blob=new Blob(chunks,{type:mime});"
       ;; Guard: a tap talk->send with no speech yields a near-empty blob → Groq 400. Bail early.
       "if(blob.size<1024){setStatus('No audio captured — hold a moment and speak.');return;}"
       ;; NOTE: spoken 'Got it, transcribing' cue fires in rec.onclick (the tap gesture),
       ;; not here — iOS only allows speechSynthesis from a direct user-gesture handler.
       "setStatus('Transcribing…');"
       "const fd=new FormData();fd.append('audio',blob,'d'+extFor(mime));"
       "try{const r=await fetch('/api/bridge/dictate',{method:'POST',body:fd});const j=await r.json();"
       ;; Record the successful-send time so the timer counts up from here (Task 2),
       ;; and resume any replies that queued silently during recording (Task 1).
       "if(r.ok&&j.transcript){add('me','you',j.transcript);lastSentAt=Date.now();say('Waiting for response');"
       "if(!playing){playNext();}tickHeard();}"
       "else{setStatus('Error '+(r.status||'')+': '+(j.error||'?'));say('Sorry, transcription error');}}"
       "catch(e){setStatus('Send failed: '+(e&&e.message?e.message:e));}}"
       ;; Tap handler: prime TTS + unlock reply audio on first gesture. On the STOP tap,
       ;; fire the spoken cue HERE (direct gesture) so iOS doesn't suppress it.
       "rec.onclick=()=>{prime();unlock();if(!on){start();}else{on=false;say('Got it, transcribing');try{mr&&mr.stop();}catch(e){send();}}};"
       ;; Sequential playback queue: a new reply NEVER interrupts the one playing —
       ;; it queues and plays when the current finishes. Fixes long-message truncation.
       ;; While recording, do NOT start playback — leave replies queued so they play
       ;; AFTER the dictation is sent. Show a silent visual indicator only.
       "function playNext(){if(recordingActive()){playing=false;return;}"
       "if(queue.length===0){playing=false;setStatus('Idle');return;}"
       "playing=true;msgWaiting=0;const t=queue.shift();setStatus(queue.length?('▶ reading · '+queue.length+' queued'):'▶ reading');"
       "try{audio.src='/api/bridge/tts?text='+encodeURIComponent(t);audio.playbackRate=1.25;audio.play().catch(function(err){playing=false;if(err&&err.name==='NotAllowedError'){queue.unshift(t);}else{playNext();}});}catch(e){playing=false;playNext();}}"
       "audio.onended=()=>{playing=false;playNext();};"
       ;; Self-heal: an error (e.g. iOS froze the clip) must never leave the latch stuck.
       "audio.onerror=()=>{playing=false;playNext();};"
       ;; While recording: queue silently and surface a VISUAL-only badge (no audio cue,
       ;; the mic would capture it). Otherwise behave as before.
       "function enqueuePlay(t){queue.push(t);if(recordingActive()){msgWaiting++;setStatus('📨 '+msgWaiting+' message'+(msgWaiting>1?'s':'')+' waiting');return;}if(!playing){playNext();}}"
       ;; Replay the most recent reply text on demand.
       "document.getElementById('replay').onclick=()=>{if(lastReply){enqueuePlay(lastReply);}else{setStatus('Nothing to replay yet.');}};"
       "async function poll(){"
       ;; heartbeat blink — silence never looks like death
       "beat.classList.add('on');setTimeout(()=>beat.classList.remove('on'),250);"
       ;; watchdog: latch says playing but the clip is actually done/stalled — recover.
       "if(!recordingActive()&&playing&&(audio.ended||(audio.paused&&audio.currentTime>0))){playing=false;playNext();}"
       "try{const r=await fetch('/api/bridge/replies');const j=await r.json();"
       "for(const rep of (j.replies||[])){const k=rep.ts+rep.text;if(seen.has(k))continue;seen.add(k);"
       "lastReply=rep.text;lastHeardAt=Date.now();add('bridge','bridge',rep.text);enqueuePlay(rep.text);}}catch(e){}}"
       ;; Format seconds as M:SS for the live send timer.
       "function fmtMMSS(s){const m=Math.floor(s/60),r=s%60;return m+':'+(r<10?'0':'')+r;}"
       ;; Heard/sent counter ticks every second (independent of poll cadence). After a
       ;; successful send with no reply yet, show a live 'Sent M:SS ago · waiting…' count
       ;; so Gene always sees a moving number. Once a reply arrives (lastHeardAt advances
       ;; past lastSentAt) revert to the 'Last heard from bridge: Ns ago' display.
       ;; "Claude engine" status: a spinner + rotating one-word verb (à la Claude Code's
       ;; thinking spinner) WHILE the bridge is working (we sent, no reply yet), and a calm
       ;; "idle" line otherwise. Ticks at 250ms so the spinner animates smoothly; the verb
       ;; rotates every ~3s; the timer still reads whole seconds from the timestamp.
       "const VERBS=['Riffing','Mulling','Wiring','Cooking','Pondering','Noodling','Conjuring','Percolating','Vibing','Scheming','Brewing','Tinkering','Finagling','Channeling','Computing'];"
       "const SPIN=['\\u280b','\\u2819','\\u2839','\\u2838','\\u283c','\\u2834','\\u2826','\\u2827','\\u2807','\\u280f'];let tk=0;"
       "function tickHeard(){tk++;"
       "if(lastSentAt&&(!lastHeardAt||lastHeardAt<lastSentAt)){"
       "const s=Math.floor((Date.now()-lastSentAt)/1000);"
       "heardEl.textContent=SPIN[tk%SPIN.length]+' '+VERBS[Math.floor(tk/12)%VERBS.length]+'\\u2026 '+fmtMMSS(s);return;}"
       "if(!lastHeardAt){heardEl.textContent='\\u25cb idle \\u00b7 last heard: \\u2014';return;}"
       "const s=Math.floor((Date.now()-lastHeardAt)/1000);heardEl.textContent='\\u25cb idle \\u00b7 last heard '+s+'s ago';}"
       "setInterval(tickHeard,250);tickHeard();"
       "document.getElementById('skip').onclick=()=>{audio.pause();playing=false;playNext();};"
       "document.getElementById('back').onclick=()=>{audio.currentTime=Math.max(0,audio.currentTime-10);};"
       "document.getElementById('fwd').onclick=()=>{audio.currentTime+=10;};"
       ;; Text input: type a message to inject into bridge context (easier than tap-to-talk
       ;; on a call / when you can't speak). Reuses the same send-status + reply pipeline.
       "document.getElementById('send').onclick=async()=>{const ta=document.getElementById('txt');const t=ta.value.trim();if(!t)return;prime();unlock();add('me','you',t);ta.value='';setStatus('Sent — waiting…');lastSentAt=Date.now();tickHeard();try{const r=await fetch('/api/bridge/say',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({text:t})});if(!r.ok)setStatus('Send error '+r.status);}catch(e){setStatus('Send failed: '+e);}};"
       ;; iOS backgrounding can freeze a clip so 'ended'/'pause' never fire, wedging the
       ;; latch true and blocking ALL future playback. On return to foreground, if the
       ;; element isn't actually playing, clear the latch and resume.
       "document.addEventListener('visibilitychange',()=>{if(document.visibilityState!=='visible')return;"
       "if(!recordingActive()&&playing&&(audio.paused||audio.ended)){playing=false;playNext();}});"
       "setInterval(poll,2500);poll();"
       ;; Pre-warm the cue clips so the ElevenLabs voice plays instantly (server caches by text).
       "try{fetch('/api/bridge/tts?text='+encodeURIComponent('Got it, transcribing'));fetch('/api/bridge/tts?text='+encodeURIComponent('Waiting for response'));}catch(e){}"
       "</script></body></html>"))

(defn handle-bridge-page [_req]
  {:status 200 :headers {"Content-Type" "text/html; charset=utf-8"} :body (bridge-page-html)})

;; ============================================================================
;; /bridge3 — SEAT-AWARE + MULTIPLAYER dictation tool (PARALLEL, PURELY ADDITIVE).
;; ----------------------------------------------------------------------------
;; THE GOVERNING RULE: this is additive. It NEVER touches the legacy /bridge page or
;; its /api/bridge/* endpoints and claim-once reply path. bridge3 carries its OWN
;; state, OWN /api/bridge3/* endpoints, OWN per-seat SSE stream, and OWN reply path.
;; (It was written against /bridge2, deleted 2026-07-26 after 7 days of zero traffic;
;; the same rule now reads against /bridge v1, which shares that write path.)
;;
;; THREE new powers over the legacy page:
;;   1. SEAT-AWARE: the page reads its seat from ?seat=X (default "bridge"). Every
;;      API call carries the seat, so one page can drive ANY Claude-session seat.
;;   2. MULTIPLAYER: replies are BROADCAST per-seat (an SSE fan-out), NOT claim-once
;;      drained, so every open tab on a seat sees the same conversation AND plays the
;;      same replies. The conversation #convo IS the reply feed: the client scans it
;;      for new bridge turns (dedup by turn :ts) and speaks them — so a tab never
;;      double-plays and two tabs never steal audio from each other. (The legacy
;;      claim-once GET /api/bridge/replies path is left completely untouched.)
;;   3. SESSIONS SCREEN: create/rename/delete named sessions (each = a seat = a
;;      channel into its own Claude session), with per-seat health and the one-line
;;      launch command Gene runs to start that session's connector + Claude session.
;;
;; REPLY INGEST — the multiplayer seam, done with ZERO edits to existing code: a
;; defonce watch on `channel-state` catches every reply ENQUEUED to :replies[seat]
;; (the connector's POST /api/channel/replies). It appends a bridge turn to the
;; per-seat bridge3 conversation and fans it out to that seat's subscribers. The
;; watch only ADDS to NEW atoms + NEW subscriber channels — it changes nothing about
;; the seatless bridge-convo mirror or the claim-once queue. For seat=bridge, /bridge
;; v1 still drains replies[bridge] for its own audio; bridge3 ingested the same reply
;; independently via the enqueue, so the two never contend.
;; ============================================================================

(def ^:private bridge3-convo-cap 80)

;; seat -> vec of {:role "you"|"bridge" :text String :ts iso}. Per-seat so one relay
;; serves many independent Claude sessions. SEPARATE from the seatless bridge-convo.
(defonce bridge3-convos (atom {}))

;; seat -> #{http-kit channels}. Per-seat SSE subscriber sets (the fan-out).
(defonce ^:private bridge3-subs (atom {}))

;; Named-session registry (create/rename/delete). v1: a small server-side atom + edn,
;; acknowledging Cloud Run's ephemeral FS (a cold start reloads from the edn if the
;; volume persisted, else re-seeds the default). Each entry: {:seat :name :created-ts}.
(defonce bridge3-sessions (atom []))

(defn- b3-convo-append
  "Pure: append a turn to convos[seat], keeping only the last bridge3-convo-cap."
  [convos seat turn]
  (update convos seat
          (fn [v] (let [v2 (conj (vec v) turn)]
                    (if (> (count v2) bridge3-convo-cap)
                      (subvec v2 (- (count v2) bridge3-convo-cap)) v2)))))

;; --- sessions persistence (mirror dictations: load on require, persist on change) ---
(def ^:private bridge3-sessions-file "bridge3-sessions.edn")

(defn- persist-bridge3-sessions! []
  (try
    (blob/save-edn! bridge3-sessions-file @bridge3-sessions)
    (catch Exception e (log/warn :bridge3-sessions-persist-failed :error (.getMessage e)))))

(defn- load-bridge3-sessions! []
  (try
    (if-let [s (blob/load-edn bridge3-sessions-file)]
      (do (when (vector? s) (reset! bridge3-sessions s))
          (log/info :bridge3-sessions-restored :count (count s)))
      ;; Seed a sensible default so the list is never empty (the seat /bridge3
      ;; defaults to). FIXED timestamp, deliberately: a (now-iso) here made every
      ;; clean boot render a different created-ts — the bridge3-sessions golden
      ;; flake that dogged check-pages since the registry landed.
      (reset! bridge3-sessions [{:seat "bridge" :name "Bridge" :created-ts "2026-06-29T00:00:00Z"}]))
    (catch Exception e (log/warn :bridge3-sessions-restore-failed :error (.getMessage e)))))

(defonce ^:private bridge3-sessions-loaded (do (load-bridge3-sessions!) true))
(defonce ^:private bridge3-sessions-persist-installed
  (do (add-watch bridge3-sessions ::persist
                 (fn [_ _ old new] (when (not= old new) (persist-bridge3-sessions!))))
      true))

;; --- bridge3 CONVERSATION persistence (the keystone: this is the state Gene
;; loses on every deploy — "we broke the magic"/"do we have persistent storage").
;; Same pattern: load once on require, persist via watch, through the blob layer
;; (GCS when GCS_BUCKET is set). Capped per seat, so the blob stays small.
(def ^:private bridge3-convos-file "bridge3-convos.edn")

(defn- persist-bridge3-convos! []
  (try
    (blob/save-edn! bridge3-convos-file @bridge3-convos)
    (catch Exception e (log/warn :bridge3-convos-persist-failed :error (.getMessage e)))))

(defn- load-bridge3-convos! []
  (try
    (when-let [c (blob/load-edn bridge3-convos-file)]
      (when (map? c) (reset! bridge3-convos c))
      (log/info :bridge3-convos-restored :seats (count c) :turns (reduce + 0 (map count (vals c)))))
    (catch Exception e (log/warn :bridge3-convos-restore-failed :error (.getMessage e)))))

(defonce ^:private bridge3-convos-loaded (do (load-bridge3-convos!) true))
(defonce ^:private bridge3-convos-persist-installed
  (do (add-watch bridge3-convos ::persist
                 (fn [_ _ old new] (when (not= old new) (persist-bridge3-convos!))))
      true))

(defn b3-sessions-create
  "Pure: add a session unless its seat already exists (idempotent by seat)."
  [sessions seat name]
  (if (some #(= seat (:seat %)) sessions)
    sessions
    (conj (vec sessions) {:seat seat :name name :created-ts (now-iso)})))

(defn b3-sessions-rename
  "Pure: rename the session for `seat` (no-op if absent)."
  [sessions seat name]
  (mapv (fn [s] (if (= seat (:seat s)) (assoc s :name name) s)) sessions))

(defn b3-sessions-delete
  "Pure: drop the session for `seat`."
  [sessions seat]
  (vec (remove #(= seat (:seat %)) sessions)))

(defn- valid-seat?
  "A seat is a short slug: letters, digits, dash, underscore (so it's safe in a URL,
   an env var, and the launch command). Rejects junk that could break those."
  [seat]
  (boolean (and (string? seat) (re-matches #"[A-Za-z0-9_-]{1,40}" seat))))

;; --- per-seat fragments (own renderers: bridge turns carry data-role + data-ts on
;; the .msg so the client can scan #convo for NEW bridge turns and speak them) ---
(defn- bridge3-convo-fragment
  "Full <div id=convo> for one seat's conversation (newest first). Each turn's .msg
   carries data-role + data-ts so the multiplayer client can dedup-by-ts which bridge
   turns it has already spoken. Morphed by id over SSE + repainted by poll."
  [convo]
  (str "<div id=\"convo\">"
       (apply str
              (for [turn (reverse convo)
                    :let [you? (= "you" (:role turn))]]
                (str "<div class=\"msg " (if you? "me" "bridge") "\""
                     " data-role=\"" (:role turn) "\" data-ts=\"" (:ts turn) "\""
                     (when-let [reply-id (:reply_id turn)]
                       (str " data-reply-id=\"" (attr-escape reply-id) "\""))
                     ;; data-text = authoritative TTS/copy text (az7) — no innerHTML scraping.
                     " data-text=\"" (attr-escape (:text turn)) "\">"
                     "<div class=lbl>" (if you? "you" "bridge")
                     " <span class=ago data-ts=\"" (:ts turn) "\"></span></div>"
                     (convo-text (:text turn))
                     "<button class=playbtn onclick=\"replayMsg(this)\">▶ play</button>"
                     "<button class=copybtn onclick=\"connCopyMsg(this)\">⧉ copy</button>"
                     "</div>")))
       "</div>"))

(defn- bridge3-status-fragment [convo]
  (let [working? (convo-working? convo)]
    (str "<span id=\"convo-status\" class=\"" (if working? "working" "idle") "\""
         " data-working=\"" (if working? "1" "0") "\">"
         "<span id=convo-spinner></span>"
         "<span id=convo-verb>" (if working? "working" "idle") "</span>"
         "</span>")))

(defn- bridge3-lastheard-fragment [convo]
  (let [ts (:ts (last (filter #(= "bridge" (:role %)) convo)))]
    (str "<span id=\"last-heard\" class=lastheard data-bridge-ts=\"" (or ts "") "\">"
         (if ts "last reply: …" "last reply: —")
         "</span>")))

(defn render-bridge3-frames
  "One seat's current state as Datastar patch-elements events (morph-by-id), built
   with the SPEC-VALIDATED ds/sse-raw constructor."
  [seat]
  (let [convo (get @bridge3-convos seat [])]
    (str (ds/sse-raw (bridge3-convo-fragment convo))
         (ds/sse-raw (bridge3-status-fragment convo))
         (ds/sse-raw (bridge3-lastheard-fragment convo))
         (ds/sse-raw (bridge2-version-fragment)))))

;; Per-seat SSE write mutex (one writer at a time per process).
(defonce ^:private bridge3-write-lock (Object.))

(defn- bridge3-send! [ch payload]
  (locking bridge3-write-lock
    (try (http/send! ch payload false) (catch Exception _ false))))

(defn- bridge3-broadcast!
  "Fan a seat's current frames out to every subscriber on that seat; reap dead channels.

   The RENDER happens on the caller's thread and the WRITES do not. This is called
   from `bridge3-append-turn!`, which runs inside the connector's reply-ingest POST
   handler and inside the dictate handlers — i.e. on an http-kit worker thread, and
   this server runs `:thread 8`. A socket write to ONE slow or half-dead client
   therefore used to block a request handler outright; eight of them starve the
   entire worker pool and unrelated requests start timing out. Moving the writes to
   `sse-registry`'s single push agent removes that head-of-line blocking and, as a
   bonus, SERIALIZES the writes so two state changes in quick succession paint in
   the order they happened. Rendering first, on the caller's thread, keeps the
   frames a snapshot of the state as it was at the moment of the change.

   A reaped channel is dropped from the per-seat subscriber set AND deregistered
   from the process-wide census, so the two views of 'how many streams are open'
   cannot drift apart."
  [seat]
  (let [frames (render-bridge3-frames seat)]
    (sse/push-off-thread!
      (fn []
        (let [subs (get @bridge3-subs seat #{})
              dead (reduce (fn [d ch] (if (bridge3-send! ch frames) d (conj d ch))) #{} subs)]
          (when (seq dead)
            (swap! bridge3-subs update seat #(reduce disj (or % #{}) dead))
            (run! #(sse/deregister! %) dead)))))))

(defn bridge3-append-turn!
  "Append a turn to a seat's conversation AND fan it out to that seat's subscribers."
  [seat turn]
  (swap! bridge3-convos b3-convo-append seat turn)
  (bridge3-broadcast! seat))

(defn reply-additions
  "Pure: per-seat, the reply items present in new state's :replies[seat] whose :ts is
   not in the old state's :replies[seat]. Because enqueue only APPENDS and drain sets
   to [], the only swap that yields additions is a reply enqueue — so each reply is
   reported EXACTLY once across the atom's lifetime, even though it may linger in the
   queue across later (unrelated) swaps."
  [old new]
  (for [[seat items] (:replies new)
        :let [olds (set (map :ts (get-in old [:replies seat])))]
        item items
        :when (not (contains? olds (:ts item)))]
    [seat item]))

;; THE MULTIPLAYER SEAM: a defonce watch on channel-state. Every reply the connector
;; POSTs (enqueued to :replies[seat]) becomes a bridge turn in the per-seat bridge3
;; conversation + a broadcast to that seat's tabs. ZERO edits to any existing handler.
(defonce ^:private bridge3-replies-watch-installed
  (do (add-watch channel-state ::bridge3-replies
                 (fn [_ _ old new]
                   (doseq [[seat item] (reply-additions old new)]
                     (bridge3-append-turn! seat {:role "bridge" :text (:text item) :ts (:ts item)
                                                 :reply_id (:reply_id item)}))))
      true))

;; S4 SHADOW TAP — a SECOND, independent watch on the same seam. Deliberately not
;; a line inside the watch above: a separate `defonce` cannot alter the existing
;; watch's behavior even by accident, which is the whole promise the taps make.
;; This is the reducer's :fact/reply-arrived, server-authoritative and carrying a
;; real reply identity — one of the two seams that yields a REAL reducer event.
(defonce ^:private shadow-replies-watch-installed
  (do (add-watch channel-state ::shadow-replies
                 (fn [_ _ old new]
                   ;; observe! and not a bare record!: a throwing watch fn breaks
                   ;; every subsequent swap! on channel-state, which would turn an
                   ;; observer into an outage.
                   (shadow/observe!
                     "watch"
                     (fn []
                       (doseq [[seat item] (reply-additions old new)]
                         ;; IDENTITY MUST MATCH THE ACK PATH (sol pre-deploy
                         ;; review, 2026-07-26, F2). `handle-post-reply` stamps a
                         ;; stable :reply_id and /api/channel/reply-ack reports
                         ;; playback against THAT id. This tap keyed on :ts, so S4
                         ;; joined an enqueue to an ack by two different
                         ;; identities and every real pair fell apart into a
                         ;; manufactured coverage gap. `reply-additions` still
                         ;; dedups by :ts — that is its own change detection and
                         ;; is unrelated to the identity we RECORD.
                         ;;
                         ;; An entry without a :reply_id is recorded as its own
                         ;; NAMED coverage condition (:outcome :unidentified),
                         ;; never silently joined on :ts.
                         (if-let [rid (:reply_id item)]
                           (shadow/record! seat :server/reply-enqueued
                                           {:source :bridge3-replies-watch
                                            :ids    {:reply rid}
                                            :event  {:event/type :fact/reply-arrived
                                                     :reply/id   rid
                                                     :reply/text (:text item)}})
                           (shadow/record! seat :server/reply-enqueued
                                           {:source  :bridge3-replies-watch
                                            :outcome :unidentified
                                            :live    {:ts (:ts item)}})))))))
      true))

;; S5c DELTA-6 — THE REDUCER INGRESS. A THIRD, independent watch on the same
;; seam, and for the same reason the shadow tap is not a line inside the
;; multiplayer watch: a separate `defonce` cannot alter either existing watch's
;; behavior even by accident.
;;
;; The shadow tap above only OBSERVES (it records the event it would have
;; submitted). This one SUBMITS it. Until it existed, nothing translated an
;; arriving connector reply into a `:fact/reply-arrived` for a seat's reducer
;; session: `/voice-lab`'s page polls `/api/bridge3/replies` and posts the fact
;; from the CLIENT, and `/bridge3-new` — the Datastar game-engine page — has no
;; polling loop by design, so on that surface Gene's first field turn captured,
;; transcribed, reached Claude, got an answer, and then showed nothing at all.
;;
;; Delta 6: every reply DISCOVERY TRANSPORT submits the same fact through the
;; reducer, and only the reducer's `:speak` effect reaches playback. Dedup is by
;; REPLY ID (`reducer-session/reply-envelope` derives `:event/id` from it), so a
;; double fire, a connector re-POST and a redelivery all collapse to one
;; ingestion. `ingest-reply!` is total and no-ops when the seat has no live
;; session — a throwing watch fn would break every subsequent swap! on
;; channel-state, which would turn an observer into an outage.
(defonce ^:private reducer-reply-ingest-watch-installed
  (do (add-watch channel-state ::reducer-reply-ingest
                 (fn [_ _ old new]
                   (try
                     (doseq [[seat item] (reply-additions old new)]
                       (when-let [rid (:reply_id item)]
                         (reducer-session/ingest-reply! seat rid (:text item))))
                     (catch Throwable t
                       (log/warn :reducer-reply-ingest-watch-error :err (.getMessage t))))))
      true))

;; --- THE POST SEAM, INSTALLED (scar B59) -----------------------------------
;;
;; The reducer decides that a turn is finished (`handle-transcript` -> `:posted?`
;; on the utterance); this is what makes that decision reach Marvin. Installed
;; HERE, at load time, and not required the other way round: the dependency runs
;; surface -> session loop (`channel` already requires `reducer-session` for
;; `ingest-reply!`), and reversing it would be a cycle AND would teach the
;; session loop what a seat's outbound queue is.
;;
;; The text is the reducer's OWN `(:text utt)` — joined across every partial
;; range this turn absorbed, sign-off stripped, blank-guarded. The capture port
;; (`handle-bridge3-capture`) deliberately produces none of that.
;;
;; AND IT IS IDEMPOTENT BY `utt/id` (sol, 2026-07-27). The session loop's own
;; ledger now advances on the RECEIPT — a delivery that threw leaves the turn
;; eligible, which is the whole point — so this lane is at-least-once by
;; construction and must be able to absorb the second arrival of one turn. The
;; claim is taken BEFORE the two writes and RELEASED if either throws, so the
;; retry the session loop just made possible is not immediately suppressed by
;; the ledger that exists to make it safe.
(defonce ^:private reducer-post-seen (atom {:order [] :m #{}})) ; utt/id -> claimed

(def ^:private max-reducer-post-seen 200)

(defn- claim-post
  "Pure: claim `id`, bounded FIFO. Returns the next ledger value — IDENTICAL to
   the one passed in when the id was already claimed (or is nil), which is how
   the caller's `swap-vals!` tells a fresh claim from a duplicate without a
   second read."
  [{:keys [order m] :as l} id]
  (if (or (nil? id) (contains? m id))
    l
    (let [order' (vec (take-last max-reducer-post-seen (conj (vec order) id)))]
      {:order order' :m (set order')})))

(defn- release-post
  "Pure: undo a claim (the delivery threw, so nothing landed to dedup against)."
  [{:keys [order m]} id]
  (let [order' (vec (remove #(= % id) order))]
    {:order order' :m (disj (set m) id)}))

(reducer-session/set-post-fn!
  (fn [seat {:keys [text] :as post}]
    (let [id (:utt/id post)
          [before after] (swap-vals! reducer-post-seen claim-post id)]
      (if (and id (= before after))
        ;; already delivered once — the reducer's retry found its way here twice.
        (log/info :reducer-post-duplicate :seat seat :utt id)
        (try
          (let [ts (now-iso)]
            (swap! channel-state enqueue :messages seat {:from "gene" :text text :ts ts})
            (bridge3-append-turn! seat {:role "you" :text text :ts ts})
            (log/info :reducer-post :seat seat :utt id :chars (count (str text))))
          (catch Throwable t
            (swap! reducer-post-seen release-post id)
            (throw t)))))))

;; --- bridge3 dictation idempotency (own ledger — separate from /bridge v1's) ---
(defonce ^:private bridge3-dictate-seen (atom {:order [] :m {}})) ; dedup_id -> transcript

;; --- the OVER protocol (bd: loud-room send trigger; Gene, movie-night) --------
;; In a noisy room silence never comes, so the hands-free client cuts long
;; utterances into CHUNKS (partial=1). The word "over" — Gene's radio habit —
;; becomes the real end-of-message trigger AT THE TRANSCRIPTION LAYER:
;;   chunk ends with the token  -> post accumulated partials + chunk (token stripped)
;;   chunk doesn't              -> stash as pending, post nothing yet
;;   a FINAL send (silence/pause) always posts pending + itself.
;; The token only counts when it follows sentence punctuation ("Deploy it. Over.")
;; or is the whole transcript — "I think the game is over" is a sentence about
;; the game, not a radio sign-off, and stays intact.

(def ^:private over-token-re #"(?i)([.!?,;:…])\s*over[.!?…]*\s*$")
(def ^:private over-only-re #"(?i)^\s*over[.!?…]*\s*$")

(defn noise-transcript?
  "Whisper's canonical hallucinations on silent/near-silent audio — hands-free
   mic-open moments transcribe to '.', 'you', 'Thank you.' etc. and arrive as
   messages (Gene: 'Annoying and confusing!'). A dictation matching these is
   noise, not speech. TYPED text never passes through this."
  [t]
  (let [s (-> (str t) clojure.string/trim clojure.string/lower-case)]
    (boolean
      (or (re-matches #"[\s.,!?…\-]*" s) ; punctuation-only / empty
          (contains? #{"you" "you." "thank you." "thank you" "thanks for watching"
                       "thanks for watching." "bye." "bye" "hmm." "hmm" "uh" "um"}
                     s)))))

(defn over-signoff?
  "Does this transcript end with a radio 'over' sign-off (punctuation-gated)?"
  [t]
  (let [s (str t)]
    (boolean (or (re-find over-token-re s) (re-matches over-only-re s)))))

(defn strip-over
  "Remove a trailing sign-off token (keeping the sentence punctuation before it).
   A transcript that is ONLY the token strips to \"\"."
  [t]
  (let [s (str t)]
    (cond
      (re-matches over-only-re s) ""
      (re-find over-token-re s) (clojure.string/trim (clojure.string/replace s over-token-re "$1"))
      :else (clojure.string/trim s))))

;; seat -> {:parts [String] :updated-ms long}. Partials expire after 5 minutes —
;; an abandoned half-thought shouldn't prepend itself to tomorrow's first message.
(defonce bridge3-pending-partials (atom {}))
(def ^:private partial-ttl-ms (* 5 60 1000))

(defn partials-add
  "Pure: append text to seat's pending parts, dropping an expired buffer first."
  [m seat text now-ms]
  (let [{:keys [parts updated-ms]} (get m seat)
        live? (and parts (< (- now-ms updated-ms) partial-ttl-ms))]
    (assoc m seat {:parts (conj (if live? parts []) text) :updated-ms now-ms})))

(defn partials-flush
  "Pure: [joined-prefix m-without-seat]. Expired or absent -> [\"\" m]."
  [m seat now-ms]
  (let [{:keys [parts updated-ms]} (get m seat)
        live? (and parts (< (- now-ms updated-ms) partial-ttl-ms))]
    [(if live? (clojure.string/join " " parts) "") (dissoc m seat)]))

(defn- shadow-post-decision!
  "S4 TAP HELPER — journal one POST-OR-WITHHOLD decision for the shadow diff.

   This seam is the ONE place the live keyword/OVER engine and the reducer's
   `read-transcript` are directly comparable with zero fabrication: both are pure
   functions of the RAW transcript, and the server has it. `raw` must therefore be
   the raw STT text, never the assembled/stripped text the handler posts — the
   reducer must derive its own verdict from the same input the live engine saw.

   IDENTITY (ARCHITECTURE §7 S4a). `dedup-id` is the legacy `dedup_id` param and is
   the ONLY stable identifier these handlers receive. It is recorded on EVERY
   decision — including the duplicate, blank-STT and error paths, which is where a
   retry story is actually told — and it is recorded IN ITS REAL DOMAIN:
   `shadow/dedup-identity` resolves the minting site from the registry's issuer
   table and yields `{:request id}` or `{:request id :utterance id}` accordingly.
   The server does NOT decide the domain here; conflating the two would let a
   browser keyword check's per-check id masquerade as a capture id.

   Returns nil, always (`shadow/observe!`), so no caller can branch on it and no
   exception in the marshalling below can reach the handler's response."
  [seat source outcome dedup-id raw final? live]
  (shadow/observe!
    seat
    #(let [ident (shadow/dedup-identity dedup-id)]
       (shadow/record! seat :server/utterance-post-decision
                       (cond-> {:source  source
                                :outcome outcome
                                :final?  (boolean final?)
                                :live    live}
                         (some? raw) (assoc :text (str raw))
                         ident       (assoc :ids   (:ids ident)
                                            :dedup (select-keys ident [:id :issuer :domains])))))))

(defn handle-bridge3-dictate
  "POST multipart audio (+ ?seat=X &dedup_id=) -> Groq STT -> enqueue messages[seat] +
   append a 'you' turn to the seat's bridge3 conversation (broadcast to all its tabs).
   Mirrors the /bridge dictate CONTRACT (so the reused IndexedDB persist-and-retry
   client works unchanged): {:ok true :transcript} on success, 400 on no-speech
   (permanent), 500 on a retryable error. Seat-parameterized; the legacy /api/bridge/
   dictate is untouched."
  [req]
  (let [seat     (or (seat-param req) "bridge")
        dedup-id (get-in req [:params :dedup_id])
        audio-duration-ms (audio-duration-ms-param req)
        cached   (when (seq dedup-id) (get-in @bridge3-dictate-seen [:m dedup-id]))]
    (cond
      (not (valid-seat? seat)) (json-response {:error "invalid seat"} 400)
      cached (do (log/info :bridge3-dictate-dup :id dedup-id :seat seat)
                 (shadow-post-decision! seat :bridge3-dictate :duplicate dedup-id cached true nil)
                 (json-response {:ok true :transcript cached :dedup true}))
      :else
      (let [audio (get-in req [:params :audio])
            tmp   (:tempfile audio)]
        (if-not tmp
          (json-response {:error "audio required"} 400)
          (let [ext   (or (some->> (:filename audio) (re-find #"\.[A-Za-z0-9]+$")) ".webm")
                named (java.io.File/createTempFile "bridge3-dictate-" ext)]
            (try
              (io/copy tmp named)
              ;; bd 0ar: persist the VERBATIM bytes BEFORE transcription, so a
              ;; capture survives even when STT (or this whole handler) throws.
              (let [arc (capture-archive/archive-audio!
                          {:seat seat :source :bridge3-dictate
                           :dedup-id dedup-id :filename (:filename audio)}
                          named)]
                (try
                  (let [{:keys [transcript]} ((requiring-resolve 'marvin-voice-remote.groq/transcribe-audio) (.getPath named))
                        partial? (= "1" (get-in req [:params :partial]))
                        now-ms (System/currentTimeMillis)]
                    (cond
                      (clojure.string/blank? transcript)
                      (do (log/warn :bridge3-dictate-empty :id dedup-id :seat seat)
                          (capture-archive/archive-meta! arc {:verdict :terminal :transcript ""
                                                              :reason :blank-stt :partial? partial?})
                          (shadow-post-decision! seat :bridge3-dictate :terminal dedup-id ""
                                                 (not partial?) {:posted? false :reason :blank-stt})
                          (json-response {:error "no speech detected (audio too short or silent)"} 400))

                      ;; Whisper noise hallucination ('.', 'you', …) — drop QUIETLY:
                      ;; ok+empty, no error toast, nothing posted. Hands-free rooms
                      ;; breathe; that must not become messages.
                      (noise-transcript? transcript)
                      (do (log/info :bridge3-dictate-noise :seat seat :transcript (str transcript))
                          (capture-archive/archive-meta! arc {:verdict :terminal :transcript transcript
                                                              :noise? true :partial? partial?})
                          ;; The reducer has NO noise rule, so this is the most
                          ;; frequent post-divergence class in a field journal —
                          ;; waived slice-pending, bead marvin-voice-remote-q0v.
                          (shadow-post-decision! seat :bridge3-dictate :terminal dedup-id transcript
                                                 (not partial?) {:posted? false :noise? true})
                          (json-response {:ok true :transcript "" :empty true :noise true}))

                      ;; OVER protocol: a timeout-cut chunk WITHOUT a sign-off is
                      ;; stashed, not posted — the message isn't finished yet.
                      (and partial? (not (over-signoff? transcript)))
                      (do (swap! bridge3-pending-partials partials-add seat (str transcript) now-ms)
                          (when (seq dedup-id)
                            (swap! bridge3-dictate-seen dictate-remember dedup-id (str transcript)))
                          (log/info :bridge3-dictate-partial :seat seat :chars (count (str transcript)))
                          (capture-archive/archive-meta! arc {:verdict :continued :transcript transcript
                                                              :posted? false :stashed? true :partial? true})
                          (shadow-post-decision! seat :bridge3-dictate :continued dedup-id transcript
                                                 false {:posted? false :stashed? true})
                          (json-response {:ok true :partial true :transcript transcript}))

                      :else
                      ;; final send (silence/pause) or a chunk ending in the sign-off:
                      ;; post pending partials + this transcript (token stripped) as ONE message
                      (let [[prefix m'] (partials-flush @bridge3-pending-partials seat now-ms)
                            _ (reset! bridge3-pending-partials m')
                            full (clojure.string/trim (str prefix (when (seq prefix) " ") (strip-over transcript)))]
                        (if (clojure.string/blank? full)
                          ;; bare "Over." with nothing pending: confirm, nothing to post
                          (do (capture-archive/archive-meta! arc {:verdict :terminal :transcript transcript
                                                                  :reason :blank-after-strip :partial? partial?})
                              (shadow-post-decision! seat :bridge3-dictate :terminal dedup-id transcript
                                                     (not partial?)
                                                     {:posted? false :reason :blank-after-strip})
                              (json-response {:ok true :transcript "" :empty true}))
                          (do
                            (swap! channel-state enqueue :messages seat
                                   {:from "gene" :text full :ts (now-iso)
                                    :audio-duration-ms audio-duration-ms})
                            (bridge3-append-turn! seat {:role "you" :text full :ts (now-iso)})
                            (when (seq dedup-id)
                              (swap! bridge3-dictate-seen dictate-remember dedup-id full))
                            (log/info :bridge3-dictate :seat seat :chars (count full)
                                      :joined-partials (boolean (seq prefix))
                                      :audio-duration-ms audio-duration-ms)
                            (capture-archive/archive-meta! arc {:verdict :accepted :transcript transcript
                                                                :posted? true :partial? partial?
                                                                :audio-duration-ms audio-duration-ms})
                            (shadow-post-decision! seat :bridge3-dictate :accepted dedup-id transcript
                                                   (not partial?)
                                                   {:posted? true :joined-partials? (boolean (seq prefix))})
                            (json-response {:ok true :transcript full}))))))
                  (catch Exception e
                    (let [{:keys [status body client-error?]} (ex-data e)
                          client-status (dictate-client-status status client-error?)]
                      (log/error :bridge3-dictate-error :seat seat :err (.getMessage e) :status status)
                      (capture-archive/archive-meta! arc {:verdict :error :error (.getMessage e) :status status})
                      ;; :error carries no :live :posted?, so the diff scores it a
                      ;; COVERAGE GAP rather than inventing a verdict for a capture
                      ;; that never reached one.
                      (shadow-post-decision! seat :bridge3-dictate :error dedup-id nil nil
                                             {:error (.getMessage e) :status status})
                      (json-response {:error (.getMessage e) :groq-status status :groq-body body} client-status)))))
              (finally (.delete named)))))))))

;; --- THE THIN CAPTURE / STT PORT (scar B59) ---------------------------------
;;
;; ITS OWN dedup ledger, deliberately separate from `bridge3-dictate-seen`. The
;; legacy handler remembers the POSTED text (partials stashed, sign-off stripped,
;; earlier chunks joined); this one remembers the RAW transcript. One map holding
;; both would answer a retry with whichever meaning happened to be written last,
;; and the whole point of this port is that it has no opinion about meaning.
(defonce ^:private bridge3-capture-seen (atom {:order [] :m {}})) ; dedup_id -> RAW transcript

;; INTENT: THIN-CAPTURE-PORT-B59
(defn handle-bridge3-capture
  "POST multipart audio (+ ?seat=X &dedup_id=) -> Groq STT -> `{:ok true
   :transcript <RAW>}`. A CAPTURE PORT, and nothing else.

   WHAT THIS IS NOT, and why it exists (scar B59). `/api/bridge3/dictate` does
   STT *and* makes every product decision on the way past: the noise filter, the
   OVER sign-off check, the per-seat partials stash, the strip, and the post to
   `channel-state` + the seat's conversation. Since `reducer/core`'s
   `handle-transcript` took ownership of exactly those rulings — cancel/over
   derived from the raw text, Whisper's silence hallucinations dropped, partial
   ranges JOINED out of `:pending-parts`, `:posted?` recorded on the utterance —
   the two decide the same question INDEPENDENTLY and can disagree. Two
   authorities for one decision is D2's whole prohibition, and the disagreement
   is not theoretical: the endpoint mode is now policy (`:endpoint-mode
   :keyword`) and the adapter has never heard of it.

   So this handler reaches NO verdict. No noise filter, no `over-signoff?`, no
   `strip-over`, no partials stash, no `enqueue`, no `bridge3-append-turn!`. The
   raw text goes back EXACTLY as STT produced it, INCLUDING any trailing
   \"over\" / \"cancel\" — the reducer strips it, rules on it, and posts through
   `reducer-session`'s post seam.

   WHAT IT STILL OWES (and these are transport promises, not product ones):

   - the VERBATIM bytes are archived BEFORE transcription (bd 0ar), so a capture
     survives even when STT — or this whole handler — throws;
   - `dedup_id` is answered from a bounded ledger with the SAME raw transcript
     and `:dedup true`, without re-transcribing and without re-archiving. That
     is what makes a retry of any upload/STT step keep the same LOGICAL
     UTTERANCE instead of minting a second one downstream;
   - a blank transcript is 400 — the retry ladder reads a 4xx as terminal
     (`dictate-client-status`), and no number of retries makes silence into
     speech;
   - an STT failure carries the same `{:error :groq-status :groq-body}` shape and
     the same status mapping the legacy handler uses, because the JS port
     forwards those two keys as `:upstream/status` / `:upstream/detail`.

   `archive-meta!` records `{:verdict :captured}`: a port records what it
   OBSERVED. It reaches no verdict about posting because posting is not its
   decision to describe."
  [req]
  (let [seat     (or (seat-param req) "bridge")
        dedup-id (get-in req [:params :dedup_id])
        cached   (when (seq dedup-id) (get-in @bridge3-capture-seen [:m dedup-id]))]
    (cond
      (not (valid-seat? seat)) (json-response {:error "invalid seat"} 400)
      cached (do (log/info :bridge3-capture-dup :id dedup-id :seat seat)
                 (shadow-post-decision! seat :bridge3-new-capture :duplicate dedup-id cached nil nil)
                 (json-response {:ok true :transcript cached :dedup true}))
      :else
      (let [audio (get-in req [:params :audio])
            tmp   (:tempfile audio)]
        (if-not tmp
          (json-response {:error "audio required"} 400)
          (let [ext   (or (some->> (:filename audio) (re-find #"\.[A-Za-z0-9]+$")) ".webm")
                named (java.io.File/createTempFile "bridge3-capture-" ext)]
            (try
              (io/copy tmp named)
              (let [arc (capture-archive/archive-audio!
                          {:seat seat :source :bridge3-new-capture
                           :dedup-id dedup-id :filename (:filename audio)}
                          named)]
                (try
                  (let [{:keys [transcript]} ((requiring-resolve 'marvin-voice-remote.groq/transcribe-audio) (.getPath named))]
                    (if (clojure.string/blank? transcript)
                      (do (log/warn :bridge3-capture-empty :id dedup-id :seat seat)
                          (capture-archive/archive-meta! arc {:verdict :captured :transcript ""
                                                              :reason :blank-stt})
                          (shadow-post-decision! seat :bridge3-new-capture :terminal dedup-id ""
                                                 nil {:reason :blank-stt})
                          (json-response {:error "no speech detected (audio too short or silent)"} 400))
                      (do
                        (when (seq dedup-id)
                          (swap! bridge3-capture-seen dictate-remember dedup-id (str transcript)))
                        (log/info :bridge3-capture :seat seat :chars (count (str transcript)))
                        (capture-archive/archive-meta! arc {:verdict :captured :transcript transcript})
                        (shadow-post-decision! seat :bridge3-new-capture :captured dedup-id transcript
                                               nil nil)
                        (json-response {:ok true :transcript transcript}))))
                  (catch Exception e
                    (let [{:keys [status body client-error?]} (ex-data e)
                          client-status (dictate-client-status status client-error?)]
                      (log/error :bridge3-capture-error :seat seat :err (.getMessage e) :status status)
                      (capture-archive/archive-meta! arc {:verdict :error :error (.getMessage e) :status status})
                      (shadow-post-decision! seat :bridge3-new-capture :error dedup-id nil nil
                                             {:error (.getMessage e) :status status})
                      (json-response {:error (.getMessage e) :groq-status status :groq-body body} client-status)))))
              (finally (.delete named)))))))))

(defn handle-bridge3-say
  "POST {text} (+ ?seat=X) -> enqueue TYPED text to messages[seat] + append a 'you'
   turn to the seat's bridge3 conversation (broadcast to all its tabs)."
  [req]
  (let [seat (or (seat-param req) "bridge")
        {:keys [text]} (body-json req)]
    (cond
      (not (valid-seat? seat)) (json-response {:error "invalid seat"} 400)
      (not (seq text))         (json-response {:error "text required"} 400)
      :else
      (do (swap! channel-state enqueue :messages seat {:from "gene" :text text :ts (now-iso)})
          (bridge3-append-turn! seat {:role "you" :text text :ts (now-iso)})
          (log/info :bridge3-say :seat seat :chars (count text))
          (json-response {:ok true})))))

(defn handle-bridge3-convo
  "GET ?seat=X -> the SERVER-rendered conversation + status fragments for one seat, so
   a tab can repaint its DISPLAY by POLLING (resilient to Cloud Run severing the long-
   lived SSE stream). Same full fragment as the SSE push -> idempotent repaint."
  [req]
  (let [seat  (or (seat-param req) "bridge")
        convo (get @bridge3-convos seat [])]
    (json-response {:convo  (bridge3-convo-fragment convo)
                    :status (bridge3-status-fragment convo)})))

(defn latest-transcription
  "Return the newest completed Gene-authored turn for `seat` as a stable receipt."
  [convos seat]
  (if-let [{:keys [text ts]} (->> (get convos seat [])
                                  reverse
                                  (some #(when (and (= "you" (:role %))
                                                    (not (clojure.string/blank? (:text %))))
                                           %)))]
    {:schema "marvin.latest-transcription.v1"
     :available true
     :id (str "transcription:" seat ":" ts)
     :seat seat
     :text text
     :observed-at ts
     :source "bridge3-conversation"}
    {:schema "marvin.latest-transcription.v1"
     :available false
     :seat seat
     :source "bridge3-conversation"}))

(defn handle-code-director-latest-transcription
  "Return the latest completed transcription from the fixed Code Director seat."
  [request]
  (if-not (token-ok? request)
    (json-response {:schema "marvin.latest-transcription.v1"
                    :available false
                    :error "unauthorized"}
                   401)
    (json-response (latest-transcription @bridge3-convos "code-director-sse"))))

(defn handle-bridge3-state
  "Debug: the bridge3 SERVER truth for one seat — turns, derived working state, the
   live subscriber count, and the rendered fragments."
  [req]
  (let [seat  (or (seat-param req) "bridge")
        convo (get @bridge3-convos seat [])]
    (json-response {:seat            seat
                    :turns           (count convo)
                    :working?        (convo-working? convo)
                    :sse-subscribers (count (get @bridge3-subs seat #{}))
                    :convo           convo})))

;; --- sessions: health + list + mutate ---
(defn session-health
  "Derive a seat's health from the scrubbed relay metrics + live backlog. `live?` is a
   heuristic: any message or reply activity within the last 10 minutes."
  [metrics-val chan-state seat]
  (let [m       (get-in metrics-val [:seats seat])
        recent? (fn [ts] (and ts (try (< (.toMinutes (java.time.Duration/between
                                                       (java.time.Instant/parse ts)
                                                       (java.time.Instant/now)))
                                         10)
                                      (catch Exception _ false))))]
    {:last-msg-ts      (:last-msg-ts m)
     :last-reply-ts    (:last-reply-ts m)
     :reply-in         (:reply-in m 0)
     :msg-in           (:msg-in m 0)
     :backlog-messages (count (get-in chan-state [:messages seat] []))
     :backlog-replies  (count (get-in chan-state [:replies seat] []))
     :live?            (boolean (or (recent? (:last-msg-ts m)) (recent? (:last-reply-ts m))))}))

(defn handle-bridge3-sessions-list
  "GET -> the session registry, each entry enriched with derived health and the
   last conversation turn (ts/role/preview) so a client session picker can show
   'which conversation is this' without fetching every convo."
  [_req]
  (let [convos @bridge3-convos]
    (json-response
      {:sessions (mapv (fn [s]
                         (let [lt (peek (get convos (:seat s)))]
                           (merge s
                                  {:health (session-health @metrics @channel-state (:seat s))}
                                  (when lt
                                    {:last {:ts (:ts lt)
                                            :role (:role lt)
                                            :preview (let [t (str (:text lt))]
                                                       (subs t 0 (min 80 (count t))))}}))))
                       @bridge3-sessions)})))

(defn- form-param [req k]
  (or (get-in req [:params k]) (get-in req [:params (name k)])
      (get-in req [:form-params (name k)])))

;; Seat pending delete-confirmation (6gg): the doctrine's server-rendered two-step
;; replaces confirm() — a browser modal would block the SSE stream (NEVER rule 2).
;; Server state → the page re-renders the row as Confirm/Cancel after the redirect.
(defonce ^:private b3-confirming-delete (atom nil))

(defn handle-bridge3-sessions-mutate
  "POST (form) action=create|rename|delete|confirm-delete|cancel-delete + seat + name.
   Mutates the registry then 303-redirects back to /bridge3/sessions so a reload shows
   the truth (server owns the list; classic POST-redirect-GET, no client JSON rendering).
   delete is TWO-STEP: 'delete' only marks the seat as confirming; 'confirm-delete'
   executes; 'cancel-delete' clears — no client-side confirm() modal (NEVER rule 2)."
  [req]
  (let [action (form-param req :action)
        seat   (some-> (form-param req :seat) clojure.string/trim)
        name   (some-> (form-param req :name) clojure.string/trim)]
    (cond
      (and (= action "create") (valid-seat? seat))
      (swap! bridge3-sessions b3-sessions-create seat (if (seq name) name seat))
      (and (= action "rename") (valid-seat? seat) (seq name))
      (swap! bridge3-sessions b3-sessions-rename seat name)
      (and (= action "delete") (valid-seat? seat))
      (reset! b3-confirming-delete seat)
      (and (= action "confirm-delete") (valid-seat? seat) (= seat @b3-confirming-delete))
      (do (swap! bridge3-sessions b3-sessions-delete seat)
          (reset! b3-confirming-delete nil))
      (= action "cancel-delete")
      (reset! b3-confirming-delete nil))
    {:status 303 :headers {"Location" "/bridge3/sessions"} :body ""}))

(defn handle-sse-bridge3
  "SSE endpoint for /bridge3?seat=X (both /bridge3 and /bridge4 subscribe here).
   Registers the channel under its seat AND in the process-wide SSE census, sends
   that seat's current full state on connect, heartbeats every 15s, and deregisters
   on close.

   TWO BOUNDS, not one (bd marvin-voice-remote-gyb):

   SUPERSESSION — the page carries a per-page-load `client` id, so when a tab's
   Datastar stream reconnects, the stream it is replacing is closed before this one
   is served. Without that id the server cannot tell a reconnect from a second tab
   and must let both live.

   BOUNDED RECYCLE — the heartbeat loop below also enforces a rotation deadline and
   deliberately closes the stream at 240–270 s (under Cloud Run's 300 s `--timeout`,
   so streams recycle on OUR terms). This exists because heartbeat reaping ALONE
   never returns the concurrency slot of a phone asleep in a pocket: a write to a
   sleeping peer's socket succeeds into the kernel send buffer and returns success
   long after the peer is gone, so the heartbeat never errors and the ghost holds
   one of the instance's 300 slots until the platform times it out. Recycling is
   free here precisely because this endpoint sends the seat's FULL current frames
   on every connect — a recycled client re-opens and re-paints the whole truth, so
   nothing is lost and the user sees nothing. (The client side of that bargain is
   `retry:'always'` on the pages' `data-star-init`; a clean server-side close reads
   as a COMPLETED response to Datastar's default `retry:'auto'` and would never be
   re-opened.)"
  [req]
  (let [seat (or (seat-param req) "bridge")]
    (http/as-channel
      req
      {:on-open  (fn [ch]
                   (http/send! ch {:status 200
                                   :headers {"Content-Type"  "text/event-stream"
                                             "Cache-Control" "no-cache"
                                             "Connection"    "keep-alive"
                                             "X-Accel-Buffering" "no"}}
                               false)
                   ;; Close-before-serve: register (and thereby supersede this
                   ;; client's previous stream) BEFORE this one starts painting.
                   (sse/register! {:ch          ch
                                   :endpoint    "/sse/bridge3"
                                   :seat        seat
                                   :client-id   (sse/client-id-of req)
                                   :remote-addr (:remote-addr req)
                                   :close!      (fn [] (http/close ch))})
                   (bridge3-send! ch (render-bridge3-frames seat))
                   (swap! bridge3-subs update seat (fnil conj #{}) ch)
                   (future
                     (loop []
                       (Thread/sleep 15000)
                       (when (contains? (get @bridge3-subs seat #{}) ch)
                         (if (sse/rotate-due? ch)
                           ;; Bounded recycle. The comment frame is best-effort — the
                           ;; close is what returns the slot.
                           (do (bridge3-send! ch ": recycle\n\n")
                               (swap! bridge3-subs update seat #(disj (or % #{}) ch))
                               (sse/close! ch :rotation))
                           (if (bridge3-send! ch ": heartbeat\n\n")
                             (recur)
                             ;; Detectably dead: reap from both views.
                             (do (swap! bridge3-subs update seat #(disj (or % #{}) ch))
                                 (sse/deregister! ch))))))))
       :on-close (fn [ch _status]
                   (swap! bridge3-subs update seat #(disj (or % #{}) ch))
                   (sse/deregister! ch))})))

(defn- launch-command
  "The one-line command Gene runs (on Buster/laptop) to start ONE connector + Claude
   session for a seat. Sources BASE_URL + CHANNEL_TOKEN from the env file (secrets are
   NEVER embedded in the page) and overrides SEAT. EXACTLY ONE connector per seat — the
   mailbox drain is destructive, so a second connector on the same seat splits the drain."
  [seat]
  (str "cd ~/src/marvin-voice-remote/channel-connector && "
       "set -a; . ~/secrets/marvin-channel.env; set +a; SEAT=" seat " "
       "claude --dangerously-load-development-channels server:marvin-channel"))

(def ^:private bridge-surface-nav-css
  ".surface-context{margin:-4px 2px 10px;font-size:13px;color:#6b7280}.surface-context b{color:#2563eb}.surface-nav{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin:0 0 16px}.surface-nav a,.surface-nav b{display:flex;align-items:center;justify-content:center;min-height:48px;border:1px solid #cbd5e1;border-radius:12px;background:#fff;color:#2563eb;font-size:17px;font-weight:700;text-decoration:none;-webkit-tap-highlight-color:transparent}.surface-nav b{border-color:#2563eb;background:#2563eb;color:#fff}")

(defn- bridge-surface-nav
  "Phone-sized navigation shared by the push-to-talk and hands-free surfaces.
   Keeps the active seat while switching modes; /bridge5 is the config picker."
  [seat active]
  (let [seat-html (html-escape seat)
        seat-attr (attr-escape seat)]
    (str "<div class=surface-context>session: <b>" seat-html "</b> · multiplayer</div>"
         "<nav id=surface-nav class=surface-nav aria-label=\"Voice mode\">"
         (if (= active :ptt)
           "<b aria-current=page>PTT</b>"
           ;; PTT now routes to the REDUCER page (Gene, 2026-07-27): the C1
           ;; adoption ratchet — tap-to-talk users land on bridge3-new.
           (str "<a href=\"/bridge3-new?seat=" seat-attr "\">PTT</a>"))
         (if (= active :hf)
           "<b aria-current=page>HF</b>"
           (str "<a href=\"/bridge4?seat=" seat-attr "\">HF</a>"))
         "<a href=\"/bridge5\">Config</a></nav>")))

(defn- bridge3-page-html
  "Build the /bridge3 page for one SEAT. SERVER owns the conversation (per-seat
   bridge3-convos); the page subscribes to /sse/bridge3?seat=X and morphs #convo /
   #convo-status / #last-heard / #ver by id. MULTIPLAYER: replies are NOT drained —
   the client speaks NEW bridge turns it finds in the server-owned #convo (dedup by
   :ts), so every tab on the seat plays the same replies without stealing them. The
   dictation + transport machinery is the proven /bridge shape (browser-native ops
   only: MediaRecorder, audio, IndexedDB), re-implemented seat-aware."
  [seat]
  (let [convo  (get @bridge3-convos seat [])
        ;; One id per PAGE LOAD. It is what lets the server recognise this tab's
        ;; Datastar reconnect and close the stream it replaces, instead of letting
        ;; the abandoned one sit on a concurrency slot. Minted here (not in the
        ;; arity) so callers and page goldens are unaffected.
        client (sse/new-client-id)
        cur    (voice-for-seat seat)
        voices (var-get (requiring-resolve 'marvin-voice-remote.tts/voices))
        voice-opts (apply str
                          (for [v voices]
                            (str "<option value=\"" (:id v) "\""
                                 (when (= (:id v) cur) " selected")
                                 ">" (html-escape (:name v)) "</option>")))
        tsize  @bridge-text-size
        size-opts (apply str
                         (for [[px label] bridge-text-sizes]
                           (str "<option value=\"" px "\""
                                (when (= px tsize) " selected")
                                ">" label "</option>")))]
    (str "<!doctype html><html><head><meta charset=utf-8>"
         "<meta name=viewport content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
         "<title>Talk to Bridge (v3) · " (html-escape seat) "</title>"
         "<script type=module src=\"/vendor/datastar-aliased.js?v=" (System/currentTimeMillis) "\"></script>"
         "<style>"
         "*{box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;margin:0;padding:16px;"
         "background:#f7f7f8;color:#1a1a1a}h1{font-size:20px;margin:8px 0 12px}"
         "#rec{width:100%;height:140px;font-size:26px;font-weight:600;border:none;border-radius:18px;"
         "color:#fff;background:#2563eb;-webkit-tap-highlight-color:transparent}#rec.on{background:#dc2626}"
         "#statusline{margin:14px 2px;font-size:15px;color:#555;min-height:20px}#status{color:#555}"
         bridge-surface-nav-css
         "#qstatus{margin:8px 2px;font-size:14px;font-weight:600;padding:7px 11px;border-radius:9px;cursor:pointer;-webkit-tap-highlight-color:transparent}#qstatus.qok{color:#1a7f37;background:#e9f7ee}#qstatus.qpending{color:#92400e;background:#fef3c7}#qstatus.qfail{color:#b42318;background:#fde8e6}"
         ".msg{margin:10px 0;padding:12px 14px;border-radius:12px;font-size:var(--msg-size,19px);line-height:1.35}"
         ".msg{position:relative}"
         ".copybtn{margin-top:8px;background:#eef1f4;color:#555;border:1px solid #d8dee4;border-radius:7px;padding:3px 10px;font-size:12px;font-weight:600;cursor:pointer;-webkit-tap-highlight-color:transparent}.copybtn:hover{background:#e2e7ec}"
         ".playbtn{margin-top:8px;margin-right:6px;background:#e9f7ee;color:#1a7f37;border:1px solid #bfe3cc;border-radius:7px;padding:3px 10px;font-size:12px;font-weight:600;cursor:pointer;-webkit-tap-highlight-color:transparent}.playbtn:hover{background:#d8f0e0}"
         "#toast{position:fixed;top:14px;right:14px;background:#1a7f37;color:#fff;padding:9px 16px;border-radius:9px;font-size:14px;font-weight:600;box-shadow:0 8px 26px rgba(31,35,40,.28);opacity:0;transform:translateY(-8px);pointer-events:none;z-index:1000;transition:opacity .18s,transform .18s}#toast.show{opacity:1;transform:translateY(0)}"
         ".me{background:#e7eefe}.bridge{background:#e9f7ee}.lbl{font-size:11px;color:#888;text-transform:uppercase;letter-spacing:.04em}"
         "#ctl{display:flex;gap:8px;margin:12px 0 6px;flex-wrap:wrap}#ctl2{display:flex;gap:8px;margin:0 0 12px;flex-wrap:wrap}.c{flex:1;min-width:64px;height:50px;font-size:15px;font-weight:600;border:none;border-radius:10px;background:#e5e7eb;color:#111;-webkit-tap-highlight-color:transparent}"
         "#ver{position:fixed;top:6px;right:8px;font-size:22px;font-weight:700;color:#111;text-align:right;pointer-events:none;z-index:10;line-height:1.2}#ver .sha{font-size:15px;font-weight:600;color:#555}"
         "#convo-status{display:inline-flex;align-items:center;gap:6px;font-size:15px;color:#555;vertical-align:middle}"
         "#convo-status.idle{color:#888}"
         "#convo-spinner{display:inline-block;width:14px;text-align:center;font-family:monospace}"
         ".lastheard{color:#888;font-variant-numeric:tabular-nums}"
         "#perm{display:none;gap:8px;margin:10px 0}#perm.show{display:flex}#perm button{flex:1;height:48px;font-size:16px;font-weight:600;border:none;border-radius:10px;-webkit-tap-highlight-color:transparent}#perm-yes{background:#16a34a;color:#fff}#perm-no{background:#dc2626;color:#fff}"
         "#type{display:flex;gap:8px;margin:10px 0}#txt{flex:1;font-size:16px;padding:10px;border:1px solid #ccc;border-radius:10px;resize:vertical}#send{padding:0 18px;font-size:15px;font-weight:600;border:none;border-radius:10px;background:#2563eb;color:#fff;-webkit-tap-highlight-color:transparent}"
         "</style></head>"
         "<body style=\"--msg-size:" tsize "\">"
         ;; SSE stream for THIS seat (inner element, matching the proven /voice pattern).
         ;; `client` = this page load, so the server can supersede this tab's own
         ;; stale stream on reconnect without evicting Gene's OTHER tabs on the seat.
         ;; `retry:'always'` is LOAD-BEARING, not decoration: Datastar's fetch-based
         ;; SSE defaults to retry:'auto', which the vendored datastar-aliased.js only
         ;; honours when the request ERRORS. The server now recycles streams at their
         ;; 240–270 s rotation deadline, and a deliberate server-side close reads as a
         ;; cleanly COMPLETED response — with 'auto' this page would go permanently
         ;; deaf about four minutes after load. retryInterval:3000 is the delay after
         ;; a clean recycle and also caps how fast a client re-attempts while the
         ;; service is 429ing, which Datastar does not back off on its own.
         ;; The `&` is entity-escaped because this is a raw HTML attribute value; the
         ;; browser decodes it back to `&` before Datastar ever sees the URL.
         "<div data-star-init=\"@get('/sse/bridge3?seat=" seat "&amp;client=" client "', {retry:'always',retryInterval:3000})\" style=\"display:none\"></div>"
         (bridge2-version-fragment)
         "<h1>🌉 Talk to Bridge (v3)</h1>"
         ;; Large mode navigation sits below the title, outside the iPhone safe-area/build overlay.
         (bridge-surface-nav seat :ptt)
         "<button id=rec disabled style=\"opacity:.55\">⏳ Loading microphone…</button>"
         "<div id=rectimer style=\"display:none;margin:8px 2px;font-size:17px;font-weight:700;color:#dc2626\">● Recording 0:00</div>"
         "<div id=toast></div>"
         "<div id=qstatus class=qok onclick=\"qstatusTap();\">\\u2713 all dictations sent</div>"
         "<div id=statusline>Client: <span id=status>idle</span> &nbsp;&middot;&nbsp; Bridge: "
         (bridge3-status-fragment convo)
         " &nbsp;&middot;&nbsp; "
         (bridge3-lastheard-fragment convo)
         "</div>"
         "<div id=ctl><button class=c id=back>« 10s</button><button class=c id=playpause>⏸ pause</button><button class=c id=fwd>10s »</button></div>"
         "<div id=ctl2><button class=c id=prev>⏮ prev</button><button class=c id=skip>⏭ skip (s)</button><button class=c id=latest>⏭⏭ latest</button><button class=c id=replay>↺ replay</button></div>"
         "<div id=perm><button id=perm-yes>Yes</button><button id=perm-no>No</button></div>"
         "<div id=type><textarea id=txt rows=2 placeholder=\"…or type to bridge\"></textarea><button id=send>Send</button></div>"
         "<div id=voicebar style=\"display:flex;align-items:center;gap:8px;margin:10px 0;font-size:14px;color:#555\">"
         "<label for=bridge3-voice>🔊 Other Voices</label>"
         "<select id=bridge3-voice style=\"flex:1;padding:8px;border:1px solid #ccc;border-radius:10px;background:#fff;color:#111;font-size:15px;-webkit-tap-highlight-color:transparent\" "
         "onchange=\"fetch('/api/bridge/voice',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'voice_id='+encodeURIComponent(this.value)+'&seat='+encodeURIComponent(SEAT)}).then(()=>showToast('\\u2713 Voice set'));\">"
         voice-opts
         "</select></div>"
         "<div id=textsizebar style=\"display:flex;align-items:center;gap:8px;margin:10px 0;font-size:14px;color:#555\">"
         "<label for=bridge3-textsize>🔠 Text Size</label>"
         "<select id=bridge3-textsize style=\"flex:1;padding:8px;border:1px solid #ccc;border-radius:10px;background:#fff;color:#111;font-size:15px;-webkit-tap-highlight-color:transparent\" "
         "onchange=\"document.body.style.setProperty('--msg-size',this.value);fetch('/api/bridge/text-size',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'size='+encodeURIComponent(this.value)}).then(()=>showToast('\\u2713 Text size'));\">"
         size-opts
         "</select></div>"
         (bridge3-convo-fragment convo)
         ;; --- THE SHARED PLAYER (bd 6iu, post-zz3): js/bridge-player.js is the ONE copy of
         ;; the reply-audio + transport machine (queue, cues, scanBridge, poll, tickers,
         ;; beacons). BP config MUST precede it; this inline script keeps ONLY the
         ;; tap-to-talk ENGINE + offline dictation queue, then calls BridgePlayerBoot()
         ;; (contract: recordingActive() exists before boot; nothing in the player runs
         ;; until boot, so ordering is safe).
         "<script>window.__MARVIN_BRIDGE__={protocol:3,surface:'bridge3',submitPath:'/api/bridge3/dictate',dedupParam:'dedup_id',nativePtt:true};var BP={seat:" (json/write-str seat) ",page:'bridge3',build:" (json/write-str git-sha) ",keyToggleId:'rec'};var SEAT=BP.seat;</script>"
         "<script src=\"/js/bridge-player.js?v=" (System/currentTimeMillis) "\"></script>"
         "<script>"
         "let mr,chunks=[],on=false,recMime='',micStream=null,micPromise=null,recStart=0;"
         "function recordingActive(){return on===true||(mr&&mr.state==='recording');}"
         "function extFor(m){if(!m)return '.webm';if(m.indexOf('mp4')>=0)return '.mp4';if(m.indexOf('ogg')>=0)return '.ogg';return '.webm';}"
         ;; Engine flags ride the player's beacons; the mic warms on first gesture; healFlags
         ;; (zz3 self-repair for a stuck `on`) runs before each poll consults the guards.
         "BP.snapExtra=function(){return {on:on,mrs:(mr?mr.state:null)};};"
         "BP.onUnlock=function(){if(nativeHost())return;try{getMic().catch(function(){});}catch(e){}};"
         "BP.prePoll=function(){healFlags();};"
         "function healFlags(){if(nativeHost())return;if(on&&(!mr||mr.state!=='recording')&&recStart&&(Date.now()-recStart)>4000){clog(Object.assign({k:'heal',why:'on-stuck'},snap()));on=false;rec.classList.remove('on');rec.textContent='● Tap to talk (t)';tickRec();tickStatus();}else if(!on&&mr&&mr.state==='recording'&&recStart&&(Date.now()-recStart)>8000){clog(Object.assign({k:'mr-stuck'},snap()));}}"
         "setInterval(function(){if(queue.length||playing||on||userPaused||(mr&&mr.state==='recording'))clog(Object.assign({k:'hb3'},snap()));},5000);"
         "function tickRec(){var e=document.getElementById('rectimer');if(!e)return;if(recordingActive()){e.style.display='block';e.textContent='\\u25cf Recording '+fmtMMSS(Math.floor((Date.now()-recStart)/1000));}else if(e.style.display!=='none'){e.style.display='none';e.textContent='';}}"
         "setInterval(tickRec,1000);tickRec();"
         "const rec=document.getElementById('rec');"
         "var nativePttStatus=window.__MARVIN_NATIVE_HF_STATUS__||null,nativePttPending=null,nativePttSeq=0;"
         "function nativeHost(){return !!window.ReactNativeWebView;}"
         "function postNativePtt(type){var req='ptt-'+Date.now()+'-'+(++nativePttSeq);window.ReactNativeWebView.postMessage(JSON.stringify({__marvinCommand:{protocol:3,type:type,requestId:req}}));clog({k:'ptt-native-command',event:type,requestId:req});}"
         "function applyNativePttStatus(s){nativePttStatus=s||{};var active=!!(nativePttStatus.running&&nativePttStatus.captureMode==='ptt');if(nativePttPending==='start'&&(active||nativePttStatus.lastError))nativePttPending=null;if(nativePttPending==='stop'&&(!nativePttStatus.running||nativePttStatus.lastError))nativePttPending=null;on=active;if(active){rec.classList.add('on');rec.textContent='\u25a0 Tap to send (t)';if(!recStart)recStart=Date.now();}else{rec.classList.remove('on');rec.textContent=nativePttPending?'\u23f3 Working\u2026':'\u25cf Tap to talk (t)';if(!nativePttPending)recStart=0;}if(nativePttStatus.lastError)setStatus('\u26a0 '+nativePttStatus.lastError);else if(nativePttStatus.uploadingCount>0)setStatus('Uploading native recording\u2026');else if(active)setStatus('Recording with native microphone\u2026');tickRec();clog({k:'ptt-native-status',phase:nativePttStatus.phase,mode:nativePttStatus.captureMode,running:!!nativePttStatus.running,pending:nativePttStatus.pending||0,uploading:nativePttStatus.uploadingCount||0,error:nativePttStatus.lastError||null});}"
         "window.addEventListener('marvin-native-hf-status',function(ev){applyNativePttStatus(ev.detail);});if(nativePttStatus)applyNativePttStatus(nativePttStatus);"
         "window.addEventListener('marvin-native-utterance',function(ev){var u=ev.detail||{};clog({k:'ptt-native-utterance',id:u.id,outcome:u.outcome,attempt:u.attempt||0,bytes:u.bytes||null,error:u.error||null});if(u.outcome==='queued')setStatus('Recording with native microphone\u2026');else if(u.outcome==='sent'){cueSend();resumePlayback();showToast('\u2713 Dictation sent');setStatus('Sent \u2014 waiting for reply\u2026');}else if(u.outcome==='retry')setStatus('Saved \u2014 retrying upload\u2026');else if(u.outcome==='failed')setStatus('\u26a0 '+(u.error||'Dictation failed'));});"
         "rec.addEventListener('click',function(ev){if(!nativeHost())return;ev.preventDefault();ev.stopImmediatePropagation();prime();unlock();if(nativePttPending)return;var active=!!(nativePttStatus&&nativePttStatus.running&&nativePttStatus.captureMode==='ptt');if(!active){if(playing&&!userPaused){resumeAfterSend=true;}try{audio.pause();}catch(e){}playing=false;nativePttPending='start';recStart=Date.now();rec.classList.add('on');rec.textContent='\u23f3 Starting\u2026';setStatus('Starting native microphone\u2026');postNativePtt('ptt.start');}else{nativePttPending='stop';rec.classList.remove('on');rec.textContent='\u23f3 Sending\u2026';setStatus('Stopping and saving native recording\u2026');postNativePtt('ptt.stop');}},true);"
         ;; (showToast/copy/replay/scanBridge/playNext/transport/poll/tickers: bridge-player.js)
         "function getMic(){if(micStream){var ts=micStream.getTracks?micStream.getTracks():[];var live=ts.length&&ts.every(function(t){return t.readyState==='live';});if(live)return Promise.resolve(micStream);try{ts.forEach(function(t){t.stop();});}catch(e){}micStream=null;}if(micPromise)return micPromise;micPromise=navigator.mediaDevices.getUserMedia({audio:true}).then(function(s){micStream=s;micPromise=null;return s;}).catch(function(e){micPromise=null;throw e;});return micPromise;}"
         "function releaseMic(){try{micStream&&micStream.getTracks().forEach(function(t){t.stop();});}catch(e){}micStream=null;try{window.ReactNativeWebView&&window.ReactNativeWebView.postMessage(JSON.stringify({__marvinWebMicReleased:true}));}catch(e){}}"
         "async function start(){let s;try{s=await getMic();window.__MARVIN_WEB_MIC_STARTING__=false;}catch(e){window.__MARVIN_WEB_MIC_STARTING__=false;on=false;rec.classList.remove('on');rec.textContent='● Tap to talk (t)';clog({k:'mic-blocked',err:String(e)});setStatus('Mic blocked: '+(e&&e.message?e.message:e));return;}"
         ;; RACE GUARD (zz3): if Gene tap-stopped while getMic was awaiting, `on` is already
         ;; false — do NOT start the recorder or re-assert on=true (that stuck recordingActive()
         ;; true forever and silently muted all playback).
         "if(!on){return;}"
         "chunks=[];mr=new MediaRecorder(s);recMime=mr.mimeType||'';"
         "mr.ondataavailable=e=>{if(e.data&&e.data.size>0)chunks.push(e.data);};"
         "mr.onstop=send;mr.start();on=true;rec.classList.add('on');rec.textContent='■ Tap to send (t)';tickStatus();}"
         ;; --- OFFLINE-FIRST PERSIST-AND-RETRY QUEUE (seat-aware: each record carries its seat
         ;; and uploads to /api/bridge3/dictate?seat=<rec.seat>, so a record captured on one seat
         ;; always lands on THAT seat even if the page later switches seats). DB is namespaced
         ;; 'bridge3-dictations' so it never collides with /bridge's IndexedDB store.
         "var DICT_DB='bridge3-dictations',DICT_STORE='pending',_db=null,_draining=false;"
         "function idbOpen(){return new Promise(function(res,rej){if(_db)return res(_db);var rq=indexedDB.open(DICT_DB,1);rq.onupgradeneeded=function(){var d=rq.result;if(!d.objectStoreNames.contains(DICT_STORE))d.createObjectStore(DICT_STORE,{keyPath:'id'});};rq.onsuccess=function(){_db=rq.result;res(_db);};rq.onerror=function(){rej(rq.error);};});}"
         "function idbStore(mode){return idbOpen().then(function(d){return d.transaction(DICT_STORE,mode).objectStore(DICT_STORE);});}"
         "function idbPut(rec){return idbStore('readwrite').then(function(s){return new Promise(function(res,rej){var rq=s.put(rec);rq.onsuccess=function(){res();};rq.onerror=function(){rej(rq.error);};});});}"
         "function idbDel(id){return idbStore('readwrite').then(function(s){return new Promise(function(res,rej){var rq=s.delete(id);rq.onsuccess=function(){res();};rq.onerror=function(){rej(rq.error);};});});}"
         "function idbAll(){return idbStore('readonly').then(function(s){return new Promise(function(res,rej){var rq=s.getAll();rq.onsuccess=function(){res(rq.result||[]);};rq.onerror=function(){rej(rq.error);};});});}"
         "function uuid(){try{if(window.crypto&&crypto.randomUUID)return crypto.randomUUID();}catch(e){}return 'd-'+Date.now()+'-'+Math.random().toString(16).slice(2);}"
         "function updateQ(){idbAll().then(function(recs){var e=document.getElementById('qstatus');if(!e)return;var pend=recs.filter(function(r){return r.status!=='failed';}),fail=recs.filter(function(r){return r.status==='failed';});if(!pend.length&&!fail.length){e.className='qok';e.textContent='\\u2713 all dictations sent';return;}var parts=[];if(pend.length)parts.push('\\u23f3 '+pend.length+' pending \\u2014 retrying');if(fail.length)parts.push('\\u26a0 '+fail.length+' failed (tap for why)');e.className=fail.length?'qfail':'qpending';e.textContent=parts.join(' \\u00b7 ');}).catch(function(){});}"
         "function qstatusTap(){idbAll().then(function(recs){var fail=recs.filter(function(r){return r.status==='failed';});if(fail.length){showToast('\\u26a0 '+(fail[0].reason||'failed')+(fail.length>1?(' (+'+(fail.length-1)+' more)'):''));}else{drainQueue();showToast('\\u21bb Retrying\\u2026');}}).catch(function(){});}"
         "function uploadRecord(rec){var fd=new FormData();fd.append('audio',rec.blob,'d'+extFor(rec.mime));fd.append('dedup_id',rec.id);return fetch('/api/bridge3/dictate?seat='+encodeURIComponent(rec.seat||SEAT),{method:'POST',body:fd}).then(function(r){return r.json().catch(function(){return {};}).then(function(j){if(r.ok&&j.transcript){var n=rec.attempts||0;return idbDel(rec.id).then(function(){beep(1,1175);showToast(n>0?('\\u2713 Dictation sent (after '+n+' retr'+(n>1?'ies':'y')+')'):'\\u2713 Dictation sent');});}if(r.status>=400&&r.status<500&&r.status!==429){rec.status='failed';rec.attempts=(rec.attempts||0)+1;rec.reason=(j&&j.error)?j.error:('HTTP '+r.status);return idbPut(rec).then(function(){beep(2,300);showToast('\\u26a0 Dictation failed: '+rec.reason);});}rec.attempts=(rec.attempts||0)+1;rec.status='pending';return idbPut(rec);});}).catch(function(){rec.attempts=(rec.attempts||0)+1;rec.status='pending';return idbPut(rec);});}"
         "function drainQueue(){if(_draining)return Promise.resolve();_draining=true;return idbAll().then(function(recs){recs.sort(function(a,b){return a.createdAt-b.createdAt;});recs=recs.filter(function(rec){return rec.status!=='failed';});var chain=Promise.resolve();recs.forEach(function(rec){chain=chain.then(function(){return uploadRecord(rec);});});return chain;}).catch(function(){}).then(function(){_draining=false;updateQ();if(!playing&&!recordingActive()&&!userPaused&&queue.length>0)playNext();});}"
         "async function send(){rec.classList.remove('on');rec.textContent='● Tap to talk (t)';if(window.ReactNativeWebView){releaseMic();try{if(navigator.audioSession)navigator.audioSession.type='playback';}catch(e){}}"
         "try{if(actx&&actx.state!=='running')actx.resume();}catch(e){}cueSend();"
         "resumePlayback();"
         "if(!mr){setStatus('Mic not ready — tap to talk again.');return;}"
         "if(!chunks||chunks.length===0){setStatus('No audio captured — hold a moment and speak.');return;}"
         "const mime=(recMime||'audio/webm').split(';')[0];const blob=new Blob(chunks,{type:mime});"
         "if(blob.size<1024){setStatus('No audio captured — hold a moment and speak.');return;}"
         "setStatus('Saving…');const reqId=uuid();"
         "try{await idbPut({id:reqId,blob:blob,mime:mime,createdAt:Date.now(),status:'pending',attempts:0,seat:SEAT});}catch(e){setStatus('Save failed: '+(e&&e.message?e.message:e));}"
         "updateQ();setStatus('Transcribing…');"
         "await drainQueue();"
         "try{var left=await idbAll();if(left.length){setStatus('Saved \\u2014 will send when back online ('+left.length+' pending)');beep(2,300);}else{tickStatus();}}catch(e){}}"
         "rec.onclick=()=>{if(!on)window.__MARVIN_WEB_MIC_STARTING__=true;prime();unlock();if(!on){on=true;recStart=Date.now();rec.classList.add('on');rec.textContent='■ Tap to send (t)';tickStatus();cueStart();buzz(10);tickRec();if(playing&&!userPaused){resumeAfterSend=true;}try{audio.pause();}catch(e){}playing=false;start();}else{window.__MARVIN_WEB_MIC_STARTING__=false;on=false;rec.classList.remove('on');rec.textContent='● Tap to talk (t)';buzz(15);tickRec();tickStatus();try{mr&&mr.stop();}catch(e){send();}}};"
         ;; playback/transport/poll/tickers all live in bridge-player.js; only the
         ;; engine-side lifecycle handlers remain here.
         "document.addEventListener('visibilitychange',()=>{if(document.visibilityState!=='visible')return;"
         "warmCtx();drainQueue();"
         "if(!recordingActive()&&playing&&(audio.paused||audio.ended)){playing=false;playNext();}});"
         "window.addEventListener('online',function(){drainQueue();});"
         "window.addEventListener('pagehide',releaseMic);window.addEventListener('beforeunload',releaseMic);"
         "setInterval(function(){drainQueue();},18000);updateQ();drainQueue();"
         "BridgePlayerBoot();rec.disabled=false;rec.style.opacity='1';rec.textContent='● Tap to talk (t)';clog({k:'ptt-ui-ready'});"
         "</script></body></html>")))

(defn handle-bridge3-page
  "GET /bridge3?seat=X -> the seat-aware multiplayer dictation page (default seat=bridge)."
  [req]
  (let [seat (let [s (seat-param req)] (if (valid-seat? s) s "bridge"))]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"
               "Cache-Control" "no-cache, no-store, must-revalidate"}
     :body (bridge3-page-html seat)}))

(defn- session-row-html [{:keys [seat name] :as _s} health confirming?]
  (str "<div class=sess>"
       "<div class=sesshd>"
       "<span class=\"dot " (if (:live? health) "live" "idle") "\"></span>"
       "<b>" (html-escape name) "</b> <span class=seat>" (html-escape seat) "</span>"
       ;; V5 (Gene 2026-06-30): the sessions screen IS the channel picker. Each session opens
       ;; in HANDS-FREE (/bridge4) or push-to-talk (/bridge3) — pick your channel + your mode.
       "<span class=openlinks><a href=\"/bridge4?seat=" seat "\">🎙 hands-free ↗</a>"
       "<a href=\"/bridge3?seat=" seat "\">⌨ push-to-talk ↗</a></span>"
       "</div>"
       "<div class=meta>"
       (if (:live? health) "● active (msg/reply within 10m)" "○ no recent activity")
       " · backlog msgs " (:backlog-messages health)
       " · backlog replies " (:backlog-replies health)
       (when-let [lr (:last-reply-ts health)] (str " · last reply " (html-escape lr)))
       "</div>"
       ;; one-line launch command + browser-native copy
       "<div class=launchrow><code class=launch id=\"lc-" seat "\">" (html-escape (launch-command seat)) "</code>"
       "<button class=cp onclick=\"cpLaunch('lc-" seat "')\">⧉ copy</button></div>"
       "<div class=ops>"
       "<form method=post action=\"/api/bridge3/sessions\" class=opf>"
       "<input type=hidden name=action value=rename><input type=hidden name=seat value=\"" seat "\">"
       "<input name=name placeholder=\"rename…\" value=\"" (html-escape name) "\">"
       "<button type=submit>rename</button></form>"
       ;; TWO-STEP delete (6gg): first tap marks the seat confirming server-side; the
       ;; redirected page re-renders THIS row with Confirm/Cancel. No confirm() modal —
       ;; a browser modal blocks the SSE stream (NEVER rule 2).
       (if confirming?
         (str "<form method=post action=\"/api/bridge3/sessions\" class=opf>"
              "<input type=hidden name=action value=confirm-delete><input type=hidden name=seat value=\"" seat "\">"
              "<button type=submit class=del>⚠ really delete " (html-escape seat) "?</button></form>"
              "<form method=post action=\"/api/bridge3/sessions\" class=opf>"
              "<input type=hidden name=action value=cancel-delete>"
              "<button type=submit>cancel</button></form>")
         (str "<form method=post action=\"/api/bridge3/sessions\" class=opf>"
              "<input type=hidden name=action value=delete><input type=hidden name=seat value=\"" seat "\">"
              "<button type=submit class=del>delete</button></form>"))
       "</div>"
       "</div>"))

(defn- bridge3-sessions-page-html []
  (let [m  @metrics
        cs @channel-state]
    (str "<!doctype html><html><head><meta charset=utf-8>"
         "<meta name=viewport content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
         "<title>Sessions — Bridge v3</title>"
         "<style>"
         "*{box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;margin:0;padding:16px;background:#f7f7f8;color:#1a1a1a}"
         "h1{font-size:20px;margin:8px 0 4px}h2{font-size:13px;color:#888;margin:18px 0 8px;text-transform:uppercase;letter-spacing:.04em}"
         "a.back{font-size:13px;color:#2563eb;text-decoration:none}"
         ".sess{margin:10px 0;padding:12px 14px;border-radius:12px;background:#fff;border:1px solid #e5e7eb}"
         ".sesshd{display:flex;align-items:center;gap:8px;font-size:16px;flex-wrap:wrap}.sesshd .seat{font-size:12px;color:#9aa0a6}"
         ".sesshd .openlinks{margin-left:auto;display:flex;gap:12px}.sesshd .openlinks a{font-size:13px;color:#2563eb;text-decoration:none;white-space:nowrap;font-weight:600}"
         ".dot{width:9px;height:9px;border-radius:50%;display:inline-block}.dot.live{background:#16a34a}.dot.idle{background:#cbd5e1}"
         ".meta{font-size:12px;color:#888;margin:6px 0 8px}"
         ".launchrow{display:flex;gap:6px;align-items:flex-start;margin:6px 0}"
         "code.launch{display:block;flex:1;background:#0f172a;color:#e2e8f0;font-size:11px;line-height:1.4;padding:8px 10px;border-radius:8px;white-space:pre-wrap;word-break:break-all;font-family:ui-monospace,Menlo,monospace}"
         ".cp{align-self:stretch;font-size:12px;font-weight:600;border:1px solid #cbd5e1;border-radius:8px;background:#f1f5f9;color:#334155;padding:0 10px;cursor:pointer}"
         ".ops{display:flex;gap:8px;flex-wrap:wrap;margin-top:8px}.opf{display:flex;gap:4px}"
         ".opf input[name=name]{font-size:13px;padding:6px 8px;border:1px solid #ccc;border-radius:8px}"
         ".opf button{font-size:13px;font-weight:600;border:none;border-radius:8px;background:#e5e7eb;color:#111;padding:0 12px;cursor:pointer}.opf button.del{background:#fde8e6;color:#b42318}"
         ".create{margin:10px 0;padding:12px 14px;border-radius:12px;background:#fff;border:1px dashed #cbd5e1;display:flex;gap:6px;flex-wrap:wrap;align-items:center}"
         ".create input{font-size:14px;padding:7px 9px;border:1px solid #ccc;border-radius:8px}.create button{font-size:14px;font-weight:600;border:none;border-radius:8px;background:#2563eb;color:#fff;padding:0 14px;cursor:pointer}"
         ".note{font-size:12px;color:#9aa0a6;margin:14px 2px;line-height:1.5}"
         "#toast{position:fixed;top:14px;right:14px;background:#1a7f37;color:#fff;padding:9px 16px;border-radius:9px;font-size:14px;font-weight:600;opacity:0;transition:opacity .18s;pointer-events:none}#toast.show{opacity:1}"
         "</style></head><body>"
         "<div id=toast></div>"
         "<h1>🛰️ Sessions</h1>"
         "<a class=back href=\"/bridge3\">← back to Talk to Bridge</a>"
         "<h2>Create a session</h2>"
         "<form method=post action=\"/api/bridge3/sessions\" class=create>"
         "<input type=hidden name=action value=create>"
         "<input name=seat placeholder=\"seat (slug, e.g. marvin-dev)\" pattern=\"[A-Za-z0-9_-]{1,40}\" required>"
         "<input name=name placeholder=\"display name\">"
         "<button type=submit>+ create</button>"
         "</form>"
         "<h2>Sessions</h2>"
         ;; ALWAYS surface "bridge" (this channel) as the default, first (Gene 2026-06-30):
         ;; the bridge seat is the live channel into THIS Claude session even if it was never
         ;; explicitly created, so the picker must always offer it as the default to open.
         (let [ss     @bridge3-sessions
               has-b? (some #(= "bridge" (:seat %)) ss)
               ss*    (if has-b? ss (cons {:seat "bridge" :name "Bridge (this channel · default)"} ss))]
           (apply str (for [s ss*] (session-row-html s (session-health m cs (:seat s))
                                                     (= (:seat s) @b3-confirming-delete)))))
         "<div class=note>Each session is a SEAT = a channel into its own Claude Code session. Run the launch "
         "command on Buster/laptop to start that session's connector + Claude session. <b>Exactly ONE connector "
         "per seat</b> — the mailbox drain is destructive, so a second connector on the same seat splits the "
         "drain. Health is derived from the relay's per-seat stats (activity within 10 min).</div>"
         "<script>"
         "function showToast(m){var t=document.getElementById('toast');if(!t)return;t.textContent=m;t.classList.add('show');clearTimeout(window.__tT);window.__tT=setTimeout(function(){t.classList.remove('show');},1800);}"
         ;; copy is a browser-native op (user gesture) — allowed client JS.
         "function cpLaunch(id){var el=document.getElementById(id);if(!el)return;var txt=el.textContent;function done(){showToast('\\u2713 Launch command copied');}if(navigator.clipboard&&window.isSecureContext){navigator.clipboard.writeText(txt).then(done).catch(done);}else{var ta=document.createElement('textarea');ta.value=txt;ta.style.position='fixed';ta.style.top='-1000px';document.body.appendChild(ta);ta.focus();ta.select();try{document.execCommand('copy');}catch(e){}document.body.removeChild(ta);done();}}"
         "</script></body></html>")))

(defn handle-bridge3-sessions-page [_req]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"
             "Cache-Control" "no-cache, no-store, must-revalidate"}
   :body (bridge3-sessions-page-html)})

;; ====================================================================================
;; /voice-lab — HANDS-FREE (no-push-to-talk) voice mode PROTOTYPE.  PURELY ADDITIVE.
;;
;; THE GOVERNING RULE (Rich Hickey): this NEVER touches /bridge or /bridge3 — not their
;; page fns, routes, or /api/bridge/* /api/bridge3/* endpoints. It carries its OWN page
;; (voice-lab-page-html), OWN route (/voice-lab), OWN seat-parameterized dictate + replies
;; endpoints. It REUSES (read-only) the shared groq/transcribe path, the channel-state
;; mailbox (enqueue/drain!), and /api/bridge/tts for readback.
;;
;; The idea: phone in pocket, one unlock tap, then VOICE-ACTIVITY DETECTION drives the loop.
;; Two selectable MODES (URL ?mode=, default KEYWORD), sharing the SAME warm-mic energy-VAD
;; start, meter, barge-in, dictate + replies endpoints:
;;
;;  • mode=silence (a.k.a. vad) — the original: energy above threshold sustained `minspeech`
;;    ms => start a segment; energy below threshold for `silence` ms => stop + AUTO-SEND.
;;    Silence alone ends the utterance. Good in a quiet room, brittle against road noise.
;;
;;  • mode=keyword (DEFAULT, what Gene wants in the car) — start-on-speech, END-ON-KEYWORD:
;;    energy-VAD starts capturing; the recorder runs THROUGH pauses (mid-thought silence and
;;    road noise do NOT end the message). On each trailing pause (silence >= `endpause` ms)
;;    the accumulated audio is transcribed and we check whether the end-word (default "over")
;;    is the LAST word. YES => finalize: send the transcript with the end-word stripped, then
;;    resume. NO => he was just thinking; keep recording. 'Check only after a pause + only the
;;    last word' is what makes "Overton" / "stand over there" safe.
;;
;; All thresholds + the mode + the end-word are URL-tunable so Gene tunes live in the car (no
;; redeploy). The mic-level meter + state readout let him SEE the VAD react to his voice vs
;; road noise and tune the `energy` threshold against the bar.
;; ====================================================================================

(defn handle-voice-lab-dictate
  "POST multipart audio (+ ?seat=X &dedup_id=) -> Groq STT -> enqueue messages[seat].
   Seat-aware sibling of /api/bridge3/dictate; SAME contract: {:ok true :transcript} on
   success, 400 on no-speech (permanent / too-short blip), 500 on a retryable error.
   Purely additive — does not touch the /bridge or bridge3 dictate handlers or their state."
  [req]
  (let [seat     (or (seat-param req) "bridge")
        dedup-id (get-in req [:params :dedup_id])]
    (if-not (valid-seat? seat)
      (json-response {:error "invalid seat"} 400)
      (let [audio (get-in req [:params :audio])
            tmp   (:tempfile audio)]
        (if-not tmp
          (json-response {:error "audio required"} 400)
          (let [ext   (or (some->> (:filename audio) (re-find #"\.[A-Za-z0-9]+$")) ".webm")
                named (java.io.File/createTempFile "voice-lab-dictate-" ext)]
            (try
              (io/copy tmp named)
              ;; bd 0ar: verbatim bytes persisted BEFORE transcription.
              (let [arc (capture-archive/archive-audio!
                          {:seat seat :source :voice-lab-dictate
                           :dedup-id dedup-id :filename (:filename audio)}
                          named)]
                (try
                  (let [{:keys [transcript]} ((requiring-resolve 'marvin-voice-remote.groq/transcribe-audio) (.getPath named))]
                    (if (clojure.string/blank? transcript)
                      (do (log/warn :voice-lab-dictate-empty :id dedup-id :seat seat)
                          (capture-archive/archive-meta! arc {:verdict :terminal :transcript ""
                                                              :reason :blank-stt})
                          (json-response {:error "no speech detected (audio too short or silent)"} 400))
                      (do
                        (swap! channel-state enqueue :messages seat
                               {:from "gene" :text transcript :ts (now-iso)})
                        (log/info :voice-lab-dictate :seat seat :chars (count (str transcript)))
                        (capture-archive/archive-meta! arc {:verdict :accepted :transcript transcript
                                                            :posted? true})
                        (json-response {:ok true :transcript transcript}))))
                  (catch Exception e
                    (let [{:keys [status body client-error?]} (ex-data e)
                          client-status (dictate-client-status status client-error?)]
                      (log/error :voice-lab-dictate-error :seat seat :err (.getMessage e) :status status)
                      (capture-archive/archive-meta! arc {:verdict :error :error (.getMessage e) :status status})
                      (json-response {:error (.getMessage e) :groq-status status :groq-body body} client-status)))))
              (finally (.delete named)))))))))

(defn handle-voice-lab-replies
  "GET ?seat=X -> claim-once drain of replies[seat] for the hands-free page to read aloud.
   Same claim-once shape as /api/bridge/replies but SEAT-AWARE (defaults to seat=bridge)."
  [req]
  (let [seat (or (seat-param req) "bridge")]
    (if-not (valid-seat? seat)
      (json-response {:error "invalid seat"} 400)
      (json-response {:replies (drain! :replies seat)}))))

;; Ring buffer of recent client beacons so the bridge can READ them back without Cloud Run
;; log access (the scoped deployer SA can't read logs) — see handle-voice-lab-diag.
(defonce ^:private voice-lab-diag (atom []))
(def ^:private voice-lab-diag-cap 2000)
(def ^:private upload-probe-max-bytes (* 1024 1024))

(defn- read-upload-probe
  "Consume a bounded raw request body while computing a receipt. The probe deliberately
   does not retain, transcribe, enqueue, or otherwise interpret the payload."
  [input-stream]
  (let [buffer (byte-array 8192)
        digest (java.security.MessageDigest/getInstance "SHA-256")]
    (loop [total 0]
      (let [read-count (.read ^java.io.InputStream input-stream buffer)]
        (cond
          (neg? read-count)
          {:bytes total
           :sha256 (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))}

          (> (+ total read-count) upload-probe-max-bytes)
          nil

          :else
          (do
            (.update digest buffer 0 read-count)
            (recur (+ total read-count))))))))

(defn handle-voice-lab-upload-probe
  "POST a raw payload and return a timing/byte receipt without invoking STT, an LLM,
   conversation state, or dictation deduplication. Used by the native Audio Test pane."
  [req]
  (if-not (token-ok? req)
    (json-response {:error "unauthorized"} 401)
    (let [started (System/nanoTime)
          receipt (read-upload-probe (:body req))
          server-read-ms (/ (- (System/nanoTime) started) 1000000.0)]
      (if receipt
        (json-response (assoc receipt
                              :ok true
                              :server-read-ms server-read-ms
                              :received-at (now-iso)))
        (json-response {:error "payload exceeds 1 MiB probe limit"} 413)))))

;; --- the beacon envelope, and the two shapes that arrive on it -------------
;;
;; THE `:k nil` BUG (Gene, 2026-07-27, field logs): every /bridge3-new beacon
;; landed here with no name. The legacy pages post FLAT reports keyed `:k`
;; ({k:"native-hf", event:"...", ...}); the R-STATIC page's entry wraps its
;; ports' rows instead ({page:"bridge3-new", build, seat, client, t,
;; row:{event:"session-degraded", …}}), so the extractor's `(:k parsed)` was nil,
;; `nm` was nil, and the whole beacon was dropped BEFORE the tap — no journal
;; row, no diagnostic census, and a log line reading `:k nil` that told a human
;; debugging the field nothing at all. Two named shapes, one extractor, and the
;; name is READ rather than invented: no beacon is renamed, prefixed or
;; namespaced here, so the registry keeps classifying exactly the names the
;; client actually emits.

(defn beacon-name
  "Pure: the causal NAME of a client beacon, across both envelope shapes.

   `native-hf` is unwrapped to its `:event` because it carries the causal name
   over a BOUNDED set; `native-shell-failure` is deliberately NOT unwrapped (its
   `:event` is an open set of failure codes minted at the raise site — the
   envelope IS the classification). The wrapped `:row` shape is unwrapped for the
   same reason `native-hf` is: the name is inside it and the wrapper has none."
  [parsed]
  (let [k (:k parsed)]
    (cond
      (= "native-hf" (str k))  (:event parsed)
      (some? k)                k
      (map? (:row parsed))     (:event (:row parsed))
      :else                    nil)))

(defn beacon-payload
  "Pure: the flat field map to read identities and live values from. For a
   wrapped beacon the row's fields are lifted next to the envelope's (the row
   wins on a collision — it is the more specific record); a flat beacon is
   already this shape and is returned unchanged."
  [parsed]
  (if (map? (:row parsed))
    (merge (dissoc parsed :row) (:row parsed))
    parsed))

(defn handle-voice-lab-clientlog
  "POST a client crash/step report (navigator.sendBeacon from /voice-lab). Body is a small
   JSON blob; logged server-side AND kept in a ring buffer so a tab that DIES still leaves a
   fingerprint (the last step beacon before a crash names the failing step). Diagnostic-only,
   never touches app state. Always 204 so the beacon never blocks. Purely additive."
  [req]
  (let [seat   (or (seat-param req) "bridge")
        body   (try (some-> (:body req) slurp) (catch Exception _ nil))
        parsed (when (seq body)
                 (try (json/read-str body :key-fn keyword)
                      (catch Exception _ {:raw (subs body 0 (min 2000 (count body)))})))
        ;; SEGREGATE, DON'T DROP (bead fuz, 2026-07-26): ratchet-sim gate runs
        ;; beacon here too (isSimulator:true) and were polluting field-data
        ;; greps — 49/146 of one evening's "field" beacons were CI traffic.
        ;; Sim reports stay in the ring (append-only doctrine: the ledger keeps
        ;; everything) but log under a DISTINCT key so `textPayload:"native-hf"`
        ;; style field sweeps never match CI noise again.
        sim?   (true? (:isSimulator parsed))
        entry  (cond-> (assoc (if (map? parsed) parsed {:raw parsed}) :seat seat :rx (now-iso))
                 sim? (assoc :sim? true))]
    (swap! voice-lab-diag (fn [v] (vec (take-last voice-lab-diag-cap (conj v entry)))))
    ;; S4 TAP — mine the EXISTING beacon stream (sol, 2026-07-26: it already
    ;; carries the richest client observations). The beacon NAME is translated
    ;; through resources/protocol/semantic-trace-1.edn rather than interpreted
    ;; here, so this file holds no trace vocabulary of its own. THREE outcomes and
    ;; no fourth (ARCHITECTURE §7 S4a): a known semantic name is journalled; a name
    ;; the registry EXPLICITLY CLASSIFIED as diagnostic-only (`:beacon-diagnostic`
    ;; — heartbeats, command acks, <audio> chatter, health probes, failure reports)
    ;; is dropped without touching the ring but COUNTED by name, so dropped never
    ;; means silent; anything else is journalled :unmapped, counted, and alarms on
    ;; first sighting, because an unrecognized causal beacon is a FINDING.
    ;; Sim traffic (bead `fuz`) is segregated by shadow into a `~sim` seat.
    (shadow/observe!
      seat
      (fn []
        (when (map? parsed)
          ;; `native-hf` carries the causal name in :event over a BOUNDED set of
          ;; command/utterance/playback names. `native-shell-failure` does NOT get
          ;; unwrapped: its :event is an OPEN set of failure codes minted at the
          ;; raise site (`bridge-message-${code}`, `navigation-refused-${code}`, …),
          ;; so unwrapping it turned every new failure code into a fresh :unmapped
          ;; name AND made the registry's `native-shell-failure` classification dead
          ;; code. The envelope IS the classification; the code rides in :live.
          ;; …and the wrapped /bridge3-new shape ({page, build, seat, row:{event}})
          ;; is unwrapped by `beacon-name`/`beacon-payload` for the same reason —
          ;; the name and the identities live in the row, and reading them off the
          ;; wrapper produced the `:k nil` blackout.
          (let [nm (beacon-name parsed)
                p  (beacon-payload parsed)]
            (when nm
              (if-let [dclass (shadow/beacon-diagnostic-class nm)]
                (shadow/note-diagnostic! nm dclass)
                (shadow/record! seat (or (shadow/beacon->semantic nm) (str nm))
                                {:source  :voice-lab-clientlog
                                 :outcome (when (string? (:outcome p)) (keyword (:outcome p)))
                                 :sim?    sim?
                                 :build   (select-keys p [:build :appBuild :nativeGitSha :hostedBuild :sequence])
                                 ;; EVERY identity present, in its own domain (S4a).
                                 ;; :detectionId is a registry identity (barge-in
                                 ;; offers carry one) and used to be dropped here.
                                 :ids     {:utterance (or (:utteranceId p) (:utterance_id p) (:id p))
                                           :reply     (or (:replyId p) (:reply_id p))
                                           :request   (or (:requestId p) (:request_id p))
                                           :detection (or (:detectionId p) (:detection_id p))}
                                 :live    (select-keys p [:outcome :attempt :error :dur :event
                                                          :boundary :detail])})))))))
    ;; SEGREGATION MUST REACH THE BODY, NOT JUST THE KEY (sol pre-deploy review,
    ;; 2026-07-26, F5). Logging sim reports under a distinct key fixed
    ;; `jsonPayload`-shaped queries but not the broad ones: the raw :report still
    ;; carried "native-hf", so a `textPayload:"native-hf"` field sweep kept
    ;; matching CI lines — the exact miscount bead `fuz` was filed for. The body
    ;; stays in the ring (append-only: the ledger keeps everything); it is the
    ;; LOG LINE that elides it, leaving the length so nothing looks lost.
    ;; EVERY field on a sim line, not just :report — `:k` is "native-hf" too, so
    ;; eliding the body alone still left the beacon NAME on the line for a broad
    ;; grep to match. A sim line is identified by its distinct log key and its
    ;; seat; anything that could collide with a field-data sweep is elided, and
    ;; the ring above still holds the whole entry for anyone who wants it.
    (log/warn (if sim? :voice-lab-clientlog-sim :voice-lab-clientlog)
              :seat seat
              ;; the NAME, not the raw `:k` field — a wrapped /bridge3-new beacon
              ;; has no `:k` and used to log `:k nil`, which is the field-debugging
              ;; blackout `beacon-name` exists to end.
              :k (if sim? "[sim-elided]" (beacon-name entry))
              :report (cond
                        (not (seq body)) nil
                        sim?             (str "[sim-elided len=" (count body) "]")
                        :else            (subs body 0 (min 1000 (count body)))))
    {:status 204 :headers {} :body ""}))

(defn handle-voice-lab-diag
  "GET (bearer CHANNEL_TOKEN) -> the recent /voice-lab client beacons. Token-gated like the
   channel relay endpoints so the bridge can read crash fingerprints WITHOUT a session cookie
   (the deployer SA can't read Cloud Run logs). ?clear=1 empties the ring. Diagnostic-only."
  [req]
  (if-not (token-ok? req)
    (json-response {:error "unauthorized"} 401)
    (let [clear? (= "1" (get-in req [:params :clear]))
          snap   @voice-lab-diag]
      (when clear? (reset! voice-lab-diag []))
      (json-response {:count (count snap) :beacons snap}))))

(defn voice-lab-keyword-finalize
  "Pure end-word endpointing for KEYWORD mode. Given a raw STT `transcript` and the
   `end-word` sentinel (default \"over\"), decide whether the utterance is COMPLETE — true
   ONLY when the end-word is the LAST word (normalized: lowercased, leading/trailing
   punctuation stripped). The 'last word only' rule — combined with the client checking
   ONLY after a trailing pause — is what makes 'Overton' / 'stand over there' safe: those
   aren't the final word before a real gap, so they never finalize.
   Returns {:ended bool :text cleaned}. When ended, :text is the transcript with the trailing
   sentinel removed (and trailing punctuation/space trimmed). Returns ended=false when there
   is no match, when the transcript is blank, or when stripping the sentinel would leave
   nothing to send (e.g. a lone \"over\" from road noise)."
  [transcript end-word]
  (let [norm  (fn [w] (-> (str w)
                          clojure.string/lower-case
                          (clojure.string/replace #"^[^\p{Alnum}]+" "")
                          (clojure.string/replace #"[^\p{Alnum}]+$" "")))
        end   (norm end-word)
        words (->> (clojure.string/split (str transcript) #"\s+")
                   (remove clojure.string/blank?)
                   vec)]
    (if (or (clojure.string/blank? end)
            (empty? words)
            (not= (norm (peek words)) end))
      {:ended false :text (str transcript)}
      (let [cleaned (-> (clojure.string/join " " (pop words))
                        (clojure.string/replace #"[\s\p{Punct}]+$" "")
                        clojure.string/trim)]
        (if (clojure.string/blank? cleaned)
          {:ended false :text (str transcript)}
          {:ended true :text cleaned})))))

(defn handle-voice-lab-keyword-check
  "POST multipart audio (+ ?seat=X &end=over) -> Groq STT -> end-word endpoint check.
   KEYWORD mode's pause-check: transcribe the accumulated utterance and decide whether the
   end-word is the LAST word. If YES -> strip the sentinel and ENQUEUE the cleaned text to
   the seat's mailbox (the reply returns via /api/voice-lab/replies), answering
   {:ok true :ended true :transcript cleaned}. If NO (he was just thinking mid-thought) ->
   {:ok true :ended false :transcript raw} and NOTHING is enqueued, so the client keeps
   recording through the pause. A blank/too-short STT result is treated as 'not done'
   (ended=false), NOT an error, so a road-noise pause never breaks the loop. Purely
   additive — never enqueues unless the utterance is truly complete; reuses the SAME shared
   groq/transcribe path + seat mailbox as the silence-VAD dictate handler."
  [req]
  (let [seat     (or (seat-param req) "bridge")
        end-word (or (get-in req [:params :end]) "over")
        ;; S4a: the page HAS been sending `dedup_id` on every keyword check
        ;; ('vlk-'+Date.now(), voice-lab-page-html kwCheck) and this handler threw
        ;; it away, so the shadow journal recorded post decisions with NO identity
        ;; at all. Bound here for the tap ONLY — the live decision path does not
        ;; read it, so this handler's behavior is unchanged.
        dedup-id (get-in req [:params :dedup_id])]
    (if-not (valid-seat? seat)
      (json-response {:error "invalid seat"} 400)
      (let [audio (get-in req [:params :audio])
            tmp   (:tempfile audio)]
        (if-not tmp
          (json-response {:error "audio required"} 400)
          (let [ext   (or (some->> (:filename audio) (re-find #"\.[A-Za-z0-9]+$")) ".webm")
                named (java.io.File/createTempFile "voice-lab-kw-" ext)]
            (try
              (io/copy tmp named)
              ;; bd 0ar: verbatim bytes persisted BEFORE transcription.
              (let [arc (capture-archive/archive-audio!
                          {:seat seat :source :voice-lab-keyword-check
                           :dedup-id dedup-id :filename (:filename audio)}
                          named)]
                (try
                  (let [{:keys [transcript]} ((requiring-resolve 'marvin-voice-remote.groq/transcribe-audio) (.getPath named))]
                    (if (clojure.string/blank? transcript)
                      (do (capture-archive/archive-meta! arc {:verdict :terminal :transcript ""
                                                              :reason :blank-stt :end-word end-word})
                          (shadow-post-decision! seat :voice-lab-keyword-check :terminal dedup-id ""
                                                 false {:posted? false :reason :blank-stt})
                          (json-response {:ok true :ended false :transcript ""}))
                      (let [{:keys [ended text]} (voice-lab-keyword-finalize transcript end-word)]
                        (if ended
                          (do
                            (swap! channel-state enqueue :messages seat
                                   {:from "gene" :text text :ts (now-iso)})
                            (log/info :voice-lab-keyword-send :seat seat :end end-word :chars (count text))
                            (capture-archive/archive-meta! arc {:verdict :accepted :transcript transcript
                                                                :posted? true :end-word end-word})
                            ;; every keyword check is a ROLLING pause check, never a
                            ;; terminal capture — final? false, which is exactly the
                            ;; input the reducer's OVER derivation must decide from.
                            (shadow-post-decision! seat :voice-lab-keyword-check :accepted dedup-id transcript
                                                   false {:posted? true :end-word end-word})
                            (json-response {:ok true :ended true :transcript text}))
                          (do (capture-archive/archive-meta! arc {:verdict :continued :transcript transcript
                                                                  :posted? false :end-word end-word})
                              (shadow-post-decision! seat :voice-lab-keyword-check :continued dedup-id transcript
                                                     false {:posted? false :end-word end-word})
                              (json-response {:ok true :ended false :transcript transcript}))))))
                  (catch Exception e
                    (let [{:keys [status]} (ex-data e)]
                      ;; soft error: keep the hands-free loop alive — the client keeps recording
                      ;; through a transient STT hiccup rather than dropping out of keyword mode.
                      (log/warn :voice-lab-keyword-check-soft-error :seat seat :err (.getMessage e) :status status)
                      (capture-archive/archive-meta! arc {:verdict :error :error (.getMessage e)
                                                          :status status :end-word end-word})
                      (shadow-post-decision! seat :voice-lab-keyword-check :error dedup-id nil nil
                                             {:error (.getMessage e) :status status})
                      (json-response {:ok true :ended false :transcript "" :soft_error (.getMessage e)})))))
              (finally (.delete named)))))))))

(defn- voice-lab-page-html
  "Build the /voice-lab page — a hands-free VAD prototype. Mostly browser-native ops
   (Web Audio AnalyserNode for the energy meter, MediaRecorder on a warm mic stream,
   <audio> playback) — explicitly a SANDBOX, not a Datastar server-owned surface. The
   thresholds are read from the URL so Gene tunes live in the car without a redeploy."
  []
  (str "<!doctype html><html><head><meta charset=utf-8>"
       "<meta name=viewport content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
       "<title>Voice Lab (hands-free)</title>"
       "<style>"
       "*{box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;margin:0;padding:16px;background:#0b1020;color:#e8eaf0}"
       "h1{font-size:20px;margin:6px 0 10px}"
       "#start{width:100%;height:120px;font-size:26px;font-weight:700;border:none;border-radius:18px;color:#fff;background:#2563eb;-webkit-tap-highlight-color:transparent}"
       "#start.live{background:#16a34a}"
       "#modeline{margin:10px 2px 4px;font-size:16px;font-weight:700;color:#fbbf24}"
       "#state{margin:16px 2px;font-size:22px;font-weight:700;min-height:30px}"
       "#meterwrap{height:34px;background:#1c2540;border-radius:10px;overflow:hidden;margin:10px 0}"
       "#meterfill{height:100%;width:0%;background:#16a34a;transition:width .05s linear}"
       "#thresh{position:relative;height:0}#threshline{position:absolute;top:-44px;width:2px;height:34px;background:#fbbf24}"
       "#params{margin:12px 2px;font-size:13px;color:#9aa6c8;font-family:ui-monospace,monospace;line-height:1.6}"
       "#log{margin-top:14px}.row{margin:8px 0;padding:10px 12px;border-radius:10px;font-size:16px;line-height:1.35}"
       ".you{background:#1e2a4a}.bridge{background:#173a2a}.lbl{font-size:11px;color:#8a93b0;text-transform:uppercase;letter-spacing:.04em}"
       "#ver{position:fixed;top:6px;right:8px;font-size:12px;color:#6b75a0;text-align:right}"
       ".hint{font-size:13px;color:#9aa6c8;margin:8px 2px;line-height:1.5}"
       "</style></head>"
       "<body>"
       "<div id=ver>" git-sha " · " (time-ago build-time) "</div>"
       "<h1>🎙 Voice Lab — hands-free (VAD)</h1>"
       "<div id=modeline></div>"
       "<button id=start>Start hands-free</button>"
       "<div id=state>Tap once to grant mic + unlock audio, then talk.</div>"
       "<div id=meterwrap><div id=meterfill></div></div>"
       "<div id=thresh><div id=threshline></div></div>"
       "<div id=params></div>"
       "<div class=hint>Tune live in the car by editing the URL and reloading:<br>"
       "<b>?mode=</b>keyword (default — say the end-word + pause to send) or silence (quiet ends it) · "
       "<b>?end=</b>end-word, e.g. over/out/done/send · <b>?cancel=</b>cancel-word (default cancel) · <b>?endpause=</b>ms of pause that triggers a keyword check · "
       "<b>?quietclose=</b>ms of dead silence that auto-flushes (default 20000) · <b>?maxutter=</b>hard cap ms per recording (default 120000).<br>"
       "<b>?energy=</b>RMS speech threshold (0..1) · <b>?silence=</b>ms of quiet to end an utterance (silence mode) · "
       "<b>?minspeech=</b>ms of voice to start · <b>?floor=</b>ms min utterance (blip guard) · <b>?seat=</b>connector seat. "
       "Watch the yellow line vs the meter: set <b>energy</b> just above where road noise peaks.</div>"
       "<div id=log></div>"
       "<script>"
       ;; --- URL-tunable params (read live; defaults documented on the page) -------------
       "var qp=new URLSearchParams(location.search);"
       ;; --- crash telemetry: a tab that DIES still leaves a fingerprint server-side. clog()
       ;; beacons to /api/voice-lab/clientlog; global handlers catch JS errors/rejections; the
       ;; last STEP beacon before a crash names the failing step. Diagnostic-only (DIAG-LOG). --
       (str "function clog(o){try{o.build='" git-sha "';o.t=Date.now();var b=new Blob([JSON.stringify(o)],{type:'application/json'});navigator.sendBeacon&&navigator.sendBeacon('/api/voice-lab/clientlog?seat='+encodeURIComponent(qp.get('seat')||'bridge'),b);}catch(e){}}")
       "window.addEventListener('error',function(ev){clog({k:'js-error',msg:(ev&&ev.message)||String(ev),src:ev&&ev.filename,line:ev&&ev.lineno,stack:(ev&&ev.error&&String(ev.error.stack||'')||'').slice(0,800)});});"
       "window.addEventListener('unhandledrejection',function(ev){clog({k:'reject',reason:String((ev&&ev.reason)||''),stack:(ev&&ev.reason&&String(ev.reason.stack||'')||'').slice(0,800)});});"
       "clog({k:'boot',ua:navigator.userAgent,mr:(typeof MediaRecorder),wl:('wakeLock' in navigator),gum:!!(navigator.mediaDevices&&navigator.mediaDevices.getUserMedia),mem:(window.performance&&performance.memory&&performance.memory.jsHeapSizeLimit)||0});"
       "function num(k,d){var v=parseFloat(qp.get(k));return (isFinite(v)&&v>0)?v:d;}"
       "var P={energy:num('energy',0.015),silence:num('silence',900),minspeech:num('minspeech',300),floor:num('floor',400),seat:(qp.get('seat')||'bridge'),end:((qp.get('end')||'over').toLowerCase()),cancel:((qp.get('cancel')||'cancel').toLowerCase()),endpause:num('endpause',1500),quietclose:num('quietclose',20000),maxutter:num('maxutter',120000)};"
       ;; mode select: keyword is the DEFAULT (what Gene wants); silence|vad reach the old behavior.
       "var mreq=(qp.get('mode')||'keyword').toLowerCase();var mode=(mreq==='silence'||mreq==='vad')?'silence':'keyword';var KW=(mode==='keyword');"
       "document.getElementById('modeline').textContent='MODE: '+mode.toUpperCase()+(KW?(' \\u2014 say \"'+P.end+'\" to send, \"'+P.cancel+'\" to cancel + pause'):' \\u2014 a quiet pause ends the message');"
       "document.getElementById('params').textContent='ACTIVE: energy='+P.energy+'  silence='+P.silence+'ms  minspeech='+P.minspeech+'ms  floor='+P.floor+'ms  seat='+P.seat+(KW?('  | end=\"'+P.end+'\"  cancel=\"'+P.cancel+'\"  endpause='+P.endpause+'ms'):'');"
       ;; place the yellow threshold marker on the meter (meter maps rms 0..0.1 -> 0..100%)
       "(function(){var pct=Math.min(100,P.energy/0.1*100);document.getElementById('threshline').style.left=pct+'%';})();"
       ;; --- state ----------------------------------------------------------------------
       "var actx,analyser,buf,micStream,mr,chunks=[],recMime='',rafId=0,wl=null,frames=0;"
       "var started=false,recording=false,stopping=false,sending=false,playing=false,checking=false;"
       "var speechStartAt=0,lastVoiceAt=0,recStartAt=0,recDur=0;"
       "var audio=new Audio();audio.playsInline=true;var playQ=[];"
       "var startBtn=document.getElementById('start'),stateEl=document.getElementById('state'),meter=document.getElementById('meterfill');"
       "function setState(t){stateEl.textContent=t;}"
       "function pushLog(who,txt){var d=document.createElement('div');d.className='row '+(who==='you'?'you':'bridge');d.innerHTML='<div class=lbl>'+(who==='you'?'you':'bridge')+'</div>'+txt.replace(/</g,'&lt;');var l=document.getElementById('log');l.insertBefore(d,l.firstChild);}"
       "function buzz(ms){try{navigator.vibrate&&navigator.vibrate(ms);}catch(e){}}"
       ;; --- audio unlock (iOS: must run inside the start gesture) -----------------------
       "function unlock(){try{audio.src='data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=';audio.play().catch(function(){});}catch(e){}}"
       "function extFor(m){if(!m)return '.webm';if(m.indexOf('mp4')>=0)return '.mp4';if(m.indexOf('ogg')>=0)return '.ogg';return '.webm';}"
       ;; --- RMS over the analyser's time-domain buffer ---------------------------------
       "function rms(){analyser.getFloatTimeDomainData(buf);var s=0;for(var i=0;i<buf.length;i++){s+=buf[i]*buf[i];}return Math.sqrt(s/buf.length);}"
       ;; --- the VAD loop: one rAF that always runs once started ------------------------
       ;; The loop runs on a TIMER, not requestAnimationFrame. Diag proved the bug: rAF gets
       ;; suspended the instant the tab backgrounds or the screen sleeps (frame counter froze at
       ;; 1090 while the setInterval heartbeat kept ticking) — the meter looked dead but the page
       ;; was alive. setTimeout survives backgrounding and resumes on foreground, so it can't
       ;; permanently freeze. schedule() is the ONLY place the timer is armed; loop() always
       ;; re-arms via finally so a single thrown frame can't kill the engine. ~50ms = 20Hz, plenty
       ;; for a VAD meter. rafId holds the timeout handle (kept the name to minimize churn).
       "function schedule(){if(started)rafId=setTimeout(loop,50);}"
       "function loop(){rafId=0;if(!started)return;try{if(actx&&actx.state!=='running'){try{actx.resume();}catch(_){}}frames++;var e=rms();var pct=Math.min(100,Math.round(e/0.1*100));meter.style.width=pct+'%';meter.style.background=(e>P.energy)?'#dc2626':'#16a34a';var now=performance.now();if(KW){loopKw(e,now);}else{loopSilence(e,now);}}catch(err){clog({k:'loop-err',msg:String((err&&err.message)||err)});setState('\\u26a0 loop hiccup: '+((err&&err.message)||err)+' \\u2014 recovering');}finally{schedule();}}"
       ;; Wake Lock (iOS 16.4+): keep the screen awake during hands-free so iOS never pauses rAF.
       "function lockScreen(){try{if('wakeLock' in navigator){navigator.wakeLock.request('screen').then(function(s){wl=s;}).catch(function(){});}}catch(e){}}"
       ;; --- SILENCE mode: start-on-speech, END-ON-SILENCE (the original behavior) ---------
       ;; speech-start detection (not while recording / sending / mid-stop). While a reply
       ;; is PLAYING use a higher threshold so the reply's own audio doesn't self-trigger
       ;; (barge-in still works when Gene actually talks over it).
       "function loopSilence(e,now){"
       "if(!recording&&!sending&&!stopping){var th=playing?P.energy*2:P.energy;if(e>th){if(speechStartAt===0)speechStartAt=now;if(now-speechStartAt>=P.minspeech){if(playing){try{audio.pause();}catch(_){}playing=false;playQ=[];}startRec(now);}}else{speechStartAt=0;}}"
       ;; speech-end detection while recording
       "else if(recording){if(e>P.energy)lastVoiceAt=now;if(now-lastVoiceAt>=P.silence)stopRec(now);}}"
       ;; --- KEYWORD mode: start-on-speech, RECORD THROUGH PAUSES, END-ON-KEYWORD ----------
       ;; Same energy-VAD start + barge-in as silence mode, but the recorder runs through
       ;; mid-thought pauses. Every trailing pause (>= endpause ms) triggers a transcribe +
       ;; last-word==end-word check; only the end-word as the LAST word finalizes the message.
       "function loopKw(e,now){if(checking||sending)return;"
       "if(!recording){var th=playing?P.energy*2:P.energy;if(e>th){if(speechStartAt===0)speechStartAt=now;if(now-speechStartAt>=P.minspeech){if(playing){try{audio.pause();}catch(_){}playing=false;playQ=[];}startRecKw(now);}}else{speechStartAt=0;}}"
       "else{if(e>P.energy)lastVoiceAt=now;"
       ;; RUNAWAY GUARD (bd 2mj, two field incidents 2026-07-05/06): a recording that
       ;; never hears the end-word must END ITSELF — quietclose ms of dead silence, or
       ;; maxutter ms total (road noise keeps lastVoiceAt fresh forever). Flush, never
       ;; swallow: the accumulated audio goes through the normal dictate path.
       "if((now-lastVoiceAt>=P.quietclose)||(now-recStartAt>=P.maxutter)){kwRunaway(now);return;}"
       "if(now-lastVoiceAt>=P.endpause)kwCheck(now);}}"
       "function kwRunaway(now){if(checking||sending)return;clog({k:'runaway-cut',dur:Math.round(now-recStartAt),quiet:Math.round(now-lastVoiceAt)});try{mr&&mr.stop();}catch(e){}recording=false;var had=chunks;chunks=[];"
       "if(had.length===0||((now-lastVoiceAt)>=P.quietclose&&(now-recStartAt)<P.floor)){setState('\\u25cf Listening\\u2026 (auto-reset)');return;}"
       "var blob=new Blob(had,{type:recMime||'audio/webm'});setState('\\u23f3 Auto-sending (no \"'+P.end+'\" heard)\\u2026');send(blob);}"
       "function kwRec(){return '\\ud83c\\udf99 Recording (say \"'+P.end+'\" to send, \"'+P.cancel+'\" to cancel)\\u2026';}"
       ;; start a CONTINUOUS recorder (timeslice => chunks accumulate into a growing valid blob)
       "function startRecKw(now){recording=true;recStartAt=now;lastVoiceAt=now;speechStartAt=0;chunks=[];try{mr=new MediaRecorder(micStream);recMime=mr.mimeType||'';mr.ondataavailable=function(ev){if(ev.data&&ev.data.size>0)chunks.push(ev.data);};mr.start(250);setState(kwRec());buzz(12);}catch(e){recording=false;setState('\\u25cf Listening\\u2026 (rec err: '+e+')');}}"
       ;; on a pause: transcribe the accumulation; server checks last-word==end-word. ended =>
       ;; it already enqueued the stripped text (reply arrives via pollReplies); else keep going.
       ;; last word of an STT transcript, normalized the same way the server strips the end-word.
       "function kwLastWord(t){if(!t)return '';var w=String(t).trim().toLowerCase().replace(/[^a-z0-9\\s']/g,' ').trim().split(/\\s+/);return w.length?w[w.length-1]:'';}"
       ;; cancel cue: a DESCENDING two-tone 'bwoop' (520->300Hz), distinct from the send path.
       "function cueCancel(){try{actx=actx||new (window.AudioContext||window.webkitAudioContext)();if(actx.state!=='running')actx.resume();var t=actx.currentTime;var notes=[520,300];for(var i=0;i<notes.length;i++){var o=actx.createOscillator(),g=actx.createGain();o.type='sine';o.frequency.value=notes[i];o.connect(g);g.connect(actx.destination);g.gain.setValueAtTime(0.0001,t);g.gain.exponentialRampToValueAtTime(0.18,t+0.01);g.gain.exponentialRampToValueAtTime(0.0001,t+0.16);o.start(t);o.stop(t+0.17);t+=0.14;}}catch(e){}}"
       "function kwCheck(now){if(chunks.length===0){lastVoiceAt=now;return;}checking=true;setState('\\u23f3 Checking\\u2026');var blob=new Blob(chunks,{type:recMime||'audio/webm'});var fd=new FormData();fd.append('audio',blob,'k'+extFor(recMime));fd.append('dedup_id','vlk-'+Date.now());"
       "fetch('/api/voice-lab/keyword-check?seat='+encodeURIComponent(P.seat)+'&end='+encodeURIComponent(P.end),{method:'POST',body:fd}).then(function(r){return r.json().catch(function(){return {};}).then(function(j){"
       "if(j&&j.ended){try{mr&&mr.stop();}catch(e){}recording=false;chunks=[];checking=false;if(j.transcript)pushLog('you',j.transcript);setState('\\u23f3 Sending\\u2026');setTimeout(function(){if(!recording&&!playing&&!sending&&!checking)setState('\\u25cf Listening\\u2026');},500);}"
       ;; CANCEL word last (server returns ended:false since cancel!=end, so nothing enqueues):
       ;; DISCARD locally — stop the recorder, drop chunks, reset to Listening, never dictate.
       "else if(P.cancel&&kwLastWord(j&&j.transcript)===P.cancel){try{mr&&mr.stop();}catch(e){}recording=false;chunks=[];checking=false;try{cueCancel();}catch(e){}buzz(30);setState('\\u2715 Canceled \\u2014 \\u25cf Listening\\u2026');setTimeout(function(){if(!recording&&!playing&&!sending&&!checking)setState('\\u25cf Listening\\u2026');},700);}"
       "else{checking=false;lastVoiceAt=performance.now();setState(kwRec());}"
       "});}).catch(function(e){checking=false;lastVoiceAt=performance.now();setState(kwRec());});}"
       ;; --- start a fresh MediaRecorder segment on the WARM stream ----------------------
       "function startRec(now){recording=true;recStartAt=now;lastVoiceAt=now;speechStartAt=0;chunks=[];try{mr=new MediaRecorder(micStream);recMime=mr.mimeType||'';mr.ondataavailable=function(ev){if(ev.data&&ev.data.size>0)chunks.push(ev.data);};mr.onstop=onRecStop;mr.start();setState('\\ud83c\\udf99 Heard you, recording\\u2026');buzz(12);}catch(e){recording=false;setState('\\u25cf Listening\\u2026 (rec err: '+e+')');}}"
       "function stopRec(now){recDur=now-recStartAt;recording=false;stopping=true;try{mr&&mr.stop();}catch(e){onRecStop();}}"
       ;; blip guard: utterances shorter than `floor` ms are discarded (road bumps, clicks)
       "function onRecStop(){stopping=false;if(recDur<P.floor){setState('\\u25cf Listening\\u2026 (ignored '+Math.round(recDur)+'ms blip)');return;}var blob=new Blob(chunks,{type:recMime||'audio/webm'});send(blob);}"
       ;; --- AUTO-SEND the utterance to the seat's dictation path ------------------------
       "function send(blob){sending=true;setState('\\u23f3 Sending\\u2026');var fd=new FormData();fd.append('audio',blob,'d'+extFor(recMime));fd.append('dedup_id','vl-'+Date.now()+'-'+Math.random().toString(36).slice(2));"
       "fetch('/api/voice-lab/dictate?seat='+encodeURIComponent(P.seat),{method:'POST',body:fd}).then(function(r){return r.json().catch(function(){return {};}).then(function(j){"
       "if(r.ok&&j.transcript){pushLog('you',j.transcript);setState('\\u23f3 Sent \\u2014 waiting for reply\\u2026');}"
       "else{setState('\\u26a0 '+((j&&j.error)||('HTTP '+r.status))+' \\u2014 \\u25cf listening');}"
       "sending=false;});}).catch(function(e){sending=false;setState('\\u26a0 send failed \\u2014 \\u25cf listening');});}"
       ;; --- steady reply poll (claim-once drain of replies[seat]) -> read aloud ---------
       "function pollReplies(){if(!started)return;fetch('/api/voice-lab/replies?seat='+encodeURIComponent(P.seat)).then(function(r){return r.ok?r.json():null;}).then(function(j){if(j&&j.replies&&j.replies.length){j.replies.forEach(function(rp){if(rp&&rp.text){pushLog('bridge',rp.text);enqueuePlay(rp.text);}});}}).catch(function(){}).then(function(){setTimeout(pollReplies,1500);});}"
       ;; --- play queue (reuses the read-only /api/bridge/tts) ---------------------------
       "function enqueuePlay(t){if(!t)return;playQ.push(t);if(!playing&&!recording)playNext();}"
       "function playNext(){if(playQ.length===0){playing=false;if(!recording&&!sending)setState('\\u25cf Listening\\u2026');return;}playing=true;var t=playQ.shift();clog({k:'play',n:playQ.length});setState('\\u25b6 Reading reply\\u2026');try{audio.src='/api/bridge/tts?text='+encodeURIComponent(t);audio.onended=function(){playNext();};audio.play().catch(function(){playing=false;playNext();});}catch(e){playing=false;playNext();}}"
       ;; --- ONE unlock tap: mic + audio + AudioContext + the loop ----------------------
       "startBtn.onclick=function(){if(started)return;clog({k:'tap'});unlock();navigator.mediaDevices.getUserMedia({audio:{echoCancellation:true,noiseSuppression:true,autoGainControl:true}}).then(function(s){clog({k:'mic-ok'});micStream=s;actx=new (window.AudioContext||window.webkitAudioContext)();clog({k:'actx',state:actx.state});if(actx.state!=='running')actx.resume();var src=actx.createMediaStreamSource(s);analyser=actx.createAnalyser();analyser.fftSize=1024;buf=new Float32Array(analyser.fftSize);src.connect(analyser);clog({k:'graph-ok'});started=true;startBtn.classList.add('live');startBtn.textContent='\\u25cf Hands-free ON';setState('\\u25cf Listening\\u2026');buzz(20);lockScreen();clog({k:'lock-done'});schedule();pollReplies();clog({k:'running'});setInterval(function(){clog({k:'hb',f:frames,rec:recording,pl:playing,ck:checking,sn:sending});},2000);}).catch(function(e){clog({k:'gum-fail',err:String(e)});setState('\\u26a0 mic denied: '+e);});};"
       ;; iOS: release the mic + audio session on navigate-away so the record-mode session ends.
       ;; iOS pauses (or drops) rAF + releases the wake lock when the tab hides/screen locks.
       ;; On return: cancel any stale frame, restart the loop, re-acquire the lock. Belt + braces.
       "document.addEventListener('visibilitychange',function(){clog({k:'vis',s:document.visibilityState,f:frames});if(started&&document.visibilityState==='visible'){if(rafId)clearTimeout(rafId);rafId=0;schedule();lockScreen();}});"
       "window.addEventListener('pagehide',function(){started=false;try{micStream&&micStream.getTracks().forEach(function(t){t.stop();});}catch(e){}try{actx&&actx.close();}catch(e){}});"
       "</script></body></html>"))

;; ====================================================================================
;; /voice-lab — S5a: THE FIRST TRUE BrowserMediaPort (ARCHITECTURE.md §7 S5a, D4, D11)
;; ====================================================================================
;;
;; D11 orders the strangler: the reducer becomes authoritative for `/voice-lab` FIRST
;; (a lab surface, freeze-exempt), then iOS, then `/bridge4` LAST. This is that first
;; flip. The page above (`voice-lab-page-html`) is preserved verbatim at
;; /voice-lab-legacy as the receipt window — it is the engine this one replaces, kept
;; runnable side by side so a behavior question can be ANSWERED rather than remembered.
;;
;; THE PAGE'S NEGATIVE SPACE — what a MediaPort must NOT contain (D4), all of which
;; the legacy page above DOES contain and this one does not:
;;
;;   · no energy/speech THRESHOLD              (P5 :speech-db lives in policy.clj)
;;   · no sustain / gap-tolerance rule         (P5)
;;   · no silence endpoint, no chunk cut       (P6 :silence-ms / :chunk-ms)
;;   · no idle recycle, no runaway guard       (P6 :idle-recycle-ms; the legacy
;;                                              quietclose/maxutter pair is now the
;;                                              reducer's :watchdog-stop reflex, whose
;;                                              bound ARRIVES in the arming payload)
;;   · no blip guard / minimum utterance       (the reducer commits ranges, D5)
;;   · no end-word, no cancel-word, no keyword check   (P6 :end-words/:cancel-words)
;;   · no barge-in rule, no tap semantics      (P1/P2/P3 — the page sends :cmd/tap and
;;                                              is TOLD what happened)
;;   · no reply queue, no dedup, no seeding    (P8/P9)
;;   · no mute rule, no replay rule            (P9/P11)
;;   · no playback rate                        (P12 — :rate rides the :speak effect)
;;   · no retry policy, no state machine, no `if` on a measurement
;;   · no URL-tunable policy: there is nothing on this page to tune.
;;
;; What it DOES contain is the whole of D4's allowed list and nothing else:
;; getUserMedia + permission, an AnalyserNode measuring RMS normalized to dBFS,
;; MediaRecorder, an idempotent range upload, <audio> playback, two cue tones, and
;; the browser lifecycle. Its numbers are transport cadences (sample/flush/poll
;; intervals) and audio-cue frequencies — measurement and presentation, never policy.
;;
;; KNOWN OVERLAP, stated rather than hidden: the committed range is uploaded to
;; /api/bridge3/dictate, which is still the STT + mailbox path and still applies its
;; OWN legacy noise/OVER rules when it decides whether to enqueue a message. The
;; reducer's verdict on the same transcript is computed and journaled in parallel
;; (shadow's post-decision comparison), so the two are watched — but until S5c the
;; POST decision is not yet the reducer's. That is the slice boundary, not an oversight.
;;
;; PROTOCOL: every message in both directions is a marvin.voice/1 envelope
;; (resources/protocol/marvin-voice-1.edn). The page never invents a kind and never
;; interprets one it does not execute.

(defn- voice-lab-reducer-page-html
  "Build the S5a /voice-lab page: a pure BrowserMediaPort against the server-side
   reducer session (`marvin-voice-remote.reducer-session`).

   The loop is: measure -> POST facts -> the reducer decides -> effects arrive on
   an SSE stream -> execute them -> report what happened as more facts. Display is
   whatever the server pushed in the batch's `view`; the page composes no status
   text of its own (P4: green never lies, and it can only never lie if the sentence
   is a function of the same state the decision was)."
  []
  (str "<!doctype html><html><head><meta charset=utf-8>"
       "<meta name=viewport content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
       "<title>Voice Lab (reducer)</title>"
       "<style>"
       "*{box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;margin:0;padding:16px;background:#0b1020;color:#e8eaf0}"
       "h1{font-size:19px;margin:6px 0 4px}"
       "#sub{font-size:12px;color:#8a93b0;margin:0 0 12px}"
       ".friction-note{font-size:11px;color:#8a93b0;margin:-4px 0 12px;line-height:1.35}"
       "#start{width:100%;height:112px;font-size:25px;font-weight:700;border:none;border-radius:18px;color:#fff;background:#2563eb;-webkit-tap-highlight-color:transparent}"
       "#start.live{background:#16a34a}"
       "#state{margin:14px 2px 6px;font-size:22px;font-weight:700;min-height:30px}"
       "#badges{margin:0 2px 10px;font-size:12px;color:#9aa6c8;font-family:ui-monospace,monospace}"
       "#meterwrap{height:30px;background:#1c2540;border-radius:10px;overflow:hidden;margin:8px 0 2px}"
       "#meterfill{height:100%;width:0%;background:#16a34a;transition:width .06s linear}"
       "#db{font-size:12px;color:#8a93b0;font-family:ui-monospace,monospace;margin:0 2px 10px}"
       "#ctl{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin:10px 0}"
       "#ctl button{min-height:52px;border-radius:12px;border:1px solid #2b3766;background:#182040;color:#dbe3ff;font-size:15px;font-weight:700;-webkit-tap-highlight-color:transparent}"
       "#banner{display:none;margin:8px 0;padding:9px 11px;border-radius:9px;background:#5b1d1d;color:#ffdede;font-size:13px}"
       ".failrow{margin:8px 0;padding:9px 11px;border-radius:9px;background:#5b1d1d;color:#ffdede;font-size:13px}"
       ".pane{margin-top:12px}.pane h2{font-size:12px;color:#8a93b0;text-transform:uppercase;letter-spacing:.05em;margin:0 2px 6px}"
       ".row{margin:6px 0;padding:9px 11px;border-radius:10px;font-size:16px;line-height:1.35}"
       ".you{background:#1e2a4a}.bridge{background:#173a2a}.lbl{font-size:11px;color:#8a93b0;text-transform:uppercase;letter-spacing:.04em}"
       "#ver{position:fixed;top:6px;right:8px;font-size:11px;color:#6b75a0;text-align:right}"
       "#ver a{color:#6b75a0}"
       "</style></head>"
       "<body>"
       "<div id=ver>" git-sha " · " (time-ago build-time) "<br><a href=/voice-lab-legacy>legacy engine</a></div>"
       "<h1>🎙 Voice Lab — reducer</h1>"
       "<div id=sub>Every decision is made on the server by the pure reducer. This page only measures, executes, and displays.</div>"
       (friction-ui/note-html (friction-ui/effective-state :voice-lab))
       "<button id=start>Start hands-free</button>"
       "<div id=banner></div>"
       ;; TRANSPORT failures speak in #banner (the page saw them itself);
       ;; CAPTURES THE SERVER NEVER GOT are listed here, from the reducer's view.
       ;; Two different truths with two different owners, never merged.
       "<div id=failed></div>"
       "<div id=state>Tap once to grant the mic.</div>"
       "<div id=badges></div>"
       "<div id=meterwrap><div id=meterfill></div></div>"
       "<div id=db>— dBFS</div>"
       "<div id=ctl>"
       "<button id=btap>Tap</button>"
       "<button id=bhold>Hold</button>"
       "<button id=bmute>Mute</button>"
       "<button id=bstop>Stop</button>"
       "</div>"
       "<div class=pane><h2>Transcripts (reducer state)</h2><div id=log></div></div>"
       "<div class=pane><h2>Replies spoken (effects executed)</h2><div id=replies></div></div>"
       "<script>"
       ;; --- TRANSPORT CADENCES. Not policy: these are how often the pipe is
       ;; sampled/flushed/polled. No product question is answered by any of them.
       ;; FINALIZE_MS is the same kind of thing: how long we wait for the RECORDER
       ;; to hand back the tail it already holds. It is a bound on a browser API,
       ;; not on the user — no product question is answered by it either.
       ;; HOLD_MAX is a MEMORY bound, not a retry rule: how many finished
       ;; capture Blobs the page keeps around so it can obey a :retry-upload
       ;; effect. How many times to retry, and whether to retry at all, are
       ;; decided by the reducer (policy :max-upload-attempts / :upload-retry-
       ;; kinds) and arrive as effects — the page counts nothing.
       "var SAMPLE_MS=50,BATCH_MS=150,POLL_MS=1500,RECONNECT_MS=1000,FINALIZE_MS=1000,HOLD_MAX=3;"
       "var qp=new URLSearchParams(location.search);"
       "var SEAT=qp.get('seat')||'bridge';"
       "var CID='web:'+Math.random().toString(36).slice(2,10);"
       "var SID=null,CURSOR=0,SEQ=0,ASEQ=0,started=false;"
       "var stream=null,actx=null,analyser=null,buf=null,es=null,wd=0;"
       "var cap={epoch:null,rec:null,chunks:[],mime:''};"
       "var pending=[];var holds={},held=[];"
       "var audio=new Audio();audio.playsInline=true;"
       "function el(i){return document.getElementById(i);}"
       ;; visible loss (D5): a failure the user must SEE, never a silent resume.
       "function note(m){var b=el('banner');b.textContent=m;b.style.display='block';try{console.error('[voice-lab]',m);}catch(e){}}"
       "function clear_note(){el('banner').style.display='none';}"
       ;; --- ENVELOPES (marvin.voice/1). The page mints identity + wall clock; it
       ;; interprets neither. ------------------------------------------------------
       "function env(kind,p){SEQ++;var e={protocol:'marvin.voice/1',kind:kind,'session/id':SID,'client/id':CID,'event/id':CID+'-'+SEQ,'causation/id':CID+'-boot','occurred-at-ms':Date.now(),sequence:SEQ};if(p){for(var k in p){e[k]=p[k];}}return e;}"
       "function post(envs){if(!SID||!envs||!envs.length)return Promise.resolve(null);"
       "return fetch('/api/reducer-session/events?seat='+encodeURIComponent(SEAT),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(envs)})"
       ".then(function(r){return r.json().catch(function(){return {};}).then(function(j){"
       "if(r.status===409&&j['session/id']){SID=j['session/id'];note('the server session restarted — resynced');}"
       "else if(!r.ok){note('rejected '+(j.code||r.status)+': '+(j.detail||j.error||''));}"
       "return j;});}).catch(function(e){note('offline: '+e);return null;});}"
       "function send(kind,p){return post([env(kind,p)]);}"
       ;; --- THE EFFECT STREAM. A batch is executed at most once: the cursor is the
       ;; dedup, and a reconnect resumes FROM it rather than replaying from zero. ---
       "function connect(){try{if(es)es.close();}catch(e){}"
       ;; No cursor on the FIRST connect: a fresh tab holds no lease and owes the
       ;; session no history, and replaying the outbox at it would re-execute a
       ;; :speak — the page would read old replies aloud on every reload. A
       ;; RECONNECT sends the last batch it executed and is made whole from there.
       ;; CID rides the stream URL so the server can recognise THIS page's reconnect
       ;; and close the stream it replaces, instead of leaving the abandoned one
       ;; holding one of the instance's 300 concurrency slots. Plain `&` — this is a
       ;; JS string literal in a <script> block, not an HTML attribute value.
       "es=new EventSource('/api/reducer-session/effects?seat='+encodeURIComponent(SEAT)+'&client='+encodeURIComponent(CID)+(CURSOR>0?('&cursor='+CURSOR):''));"
       "es.addEventListener('hello',function(m){var j=JSON.parse(m.data);SID=j['session/id'];"
       "if(j['lost?'])note('reconnected past the effect buffer — some effects were lost');else clear_note();"
       "render(j.view);});"
       "es.addEventListener('batch',function(m){var b=JSON.parse(m.data);if(b['batch/seq']<=CURSOR)return;CURSOR=b['batch/seq'];render(b.view);(b.effects||[]).forEach(exec);});"
       "es.onerror=function(){try{es.close();}catch(e){}setTimeout(connect,RECONNECT_MS);};}"
       ;; --- EXECUTE. One dispatch, no branching on measurements, no state kept for
       ;; a later decision — every branch below is 'do the named thing'. -----------
       "function exec(e){var t=e['effect/type'];"
       "if(t==='open-mic')openMic(e);"
       "else if(t==='close-mic')closeMic(e);"
       "else if(t==='speak')speak(e);"
       "else if(t==='stop-speech'){try{audio.pause();}catch(x){}}"
       "else if(t==='cue')beep(e.cue);"
       "else if(t==='arm-reflex')armReflex(e);"
       "else if(t==='disarm-reflex')disarmReflex(e);"
       "else if(t==='retry-upload')retryUpload(e);}"
       "function ext(m){if(!m)return '.webm';if(m.indexOf('mp4')>=0)return '.mp4';if(m.indexOf('ogg')>=0)return '.ogg';return '.webm';}"
       "function openMic(e){if(!stream){note('open-mic arrived with no mic stream');return;}"
       "cap.epoch=e['lease/epoch'];cap.chunks=[];"
       ;; The handler appends to THE ARRAY THIS CAPTURE OWNS, captured in the
       ;; closure — never to `cap.chunks`, which closeMic replaces. Reading the
       ;; live field is how the recorder's FINAL dataavailable (the tail since the
       ;; last timeslice) used to land in the next capture's empty array and be
       ;; dropped: real, silent speech loss.
       "try{cap.rec=new MediaRecorder(stream);cap.mime=cap.rec.mimeType||'';"
       "var mine=cap.chunks;"
       "cap.rec.ondataavailable=function(ev){if(ev.data&&ev.data.size>0)mine.push(ev.data);};cap.rec.start(250);}"
       "catch(err){cap.epoch=null;note('recorder failed: '+err);return;}"
       "send('fact/mic-opened',{'lease/epoch':cap.epoch});}"
       ;; close + (maybe) commit. `commit` null is the reducer saying DISCARD; the
       ;; page does not decide that and does not second-guess it.
       ;; MediaRecorder.stop() is ASYNCHRONOUS. Everything recorded since the last
       ;; 250 ms timeslice — and, if stop precedes the first one, the WHOLE
       ;; recording — is handed back in a final `dataavailable` that fires AFTER
       ;; stop() returns. Building the upload Blob inline therefore uploaded a
       ;; capture with its tail missing, silently. So finalize() waits for the
       ;; recorder to say it is done before the Blob exists. This is browser
       ;; capture MECHANICS, not policy: the page still decides nothing, and the
       ;; wait is bounded — a recorder that never reports proceeds with what was
       ;; captured, loudly, because a truncated upload beats a lost one.
       "function finalize(rec,chunks,cb){var done=false,t=0;"
       "function settle(late){if(done)return;done=true;clearTimeout(t);"
       "if(late)note('the recorder did not finish in '+FINALIZE_MS+'ms — uploading what it had');cb(chunks);}"
       "if(!rec||rec.state==='inactive'){settle(false);return;}"
       "rec.onstop=function(){settle(false);};"
       "t=setTimeout(function(){settle(true);},FINALIZE_MS);"
       "try{rec.stop();}catch(x){settle(false);}}"
       ;; --- UPLOAD: REPORT THE OUTCOME, DECIDE NOTHING. --------------------------
       ;; THE FABRICATION THIS REPLACES (sol re-score, 2026-07-26; bead mck):
       ;; both failure paths used to post `fact/upload-acked` plus an EMPTY
       ;; transcript. The reducer was told the bytes were safe when they were
       ;; gone, the session moved on, and a capture the user had spoken vanished
       ;; with no state, no diagnostic and nothing on screen. There was an excuse
       ;; — marvin.voice/1 had no word for a failed upload — and there is not one
       ;; now: `fact/upload-failed` exists, and the reducer answers it with
       ;; `retry-upload` (bounded by policy) or a terminal, visible failure.
       ;;
       ;; The page's whole part: say WHICH failure it was (status, kind, attempt)
       ;; and keep the bytes so it can obey a retry order. It counts no retries,
       ;; picks no backoff, and never decides that a failure is final.
       "function retain(utt,blob,fin){holds[utt]={blob:blob,fin:fin};held.push(utt);"
       "while(held.length>HOLD_MAX){var old=held.shift();delete holds[old];}}"
       "function sendBlob(utt,blob,fin,attempt){"
       "var fd=new FormData();fd.append('audio',blob,'u'+ext(blob.type));fd.append('dedup_id',utt);"
       "fetch('/api/bridge3/dictate?seat='+encodeURIComponent(SEAT),{method:'POST',body:fd})"
       ".then(function(r){return r.json().catch(function(){return {};}).then(function(j){"
       ;; NOT ok = the bytes did not land. One fact, no transcript, no ack.
       "if(!r.ok){note('dictate '+r.status+': '+(j.error||'no transcript'));"
       "send('fact/upload-failed',{'utt/id':utt,'http/status':r.status,'error/kind':'http',attempt:attempt});return;}"
       ;; ok = the server HAS the bytes, so the ack is true. An empty transcript
       ;; here is honest (STT heard nothing), unlike the empty one that used to
       ;; be manufactured out of a network error.
       "delete holds[utt];"
       "post([env('fact/upload-acked',{'utt/id':utt}),env('fact/transcript',{'utt/id':utt,text:String(j.transcript||''),'final?':fin})]);});})"
       ".catch(function(err){note('upload failed — this capture never reached the server: '+err);"
       "send('fact/upload-failed',{'utt/id':utt,'http/status':null,'error/kind':'network',attempt:attempt});});}"
       "function upload(utt,fin,chunks,mime){"
       "var blob=new Blob(chunks,{type:mime||'audio/webm'});retain(utt,blob,fin);sendBlob(utt,blob,fin,1);}"
       ;; The reducer ordered another attempt. If the bytes are gone (the hold
       ;; ring recycled them), say so as :aborted rather than retrying nothing —
       ;; policy does not retry an abort, so the reducer ends it visibly.
       "function retryUpload(e){var utt=e['utt/id'],h=holds[utt];"
       "if(!h){send('fact/upload-failed',{'utt/id':utt,'http/status':null,'error/kind':'aborted',attempt:e.attempt});return;}"
       "sendBlob(utt,h.blob,h.fin,e.attempt);}"
       "function closeMic(e){var c=e.commit,chunks=cap.chunks,rec=cap.rec,mime=cap.mime;"
       "cap.epoch=null;cap.chunks=[];cap.rec=null;"
       ;; A discarded range still has to stop the recorder — it just throws the
       ;; bytes away instead of uploading them.
       "if(!c){finalize(rec,chunks,function(){});return;}"
       "var utt=c['utt/id'],fin=!c['partial?'];"
       "send('fact/range-committed',{'utt/id':utt});"
       ;; closeMic RETURNS here: the rest of the batch (a :speak in particular)
       ;; executes immediately. Only the Blob waits.
       "finalize(rec,chunks,function(all){upload(utt,fin,all,mime);});}"
       ;; playback: the RATE arrives in the effect (P12 is policy, not a page constant)
       "function speak(e){var rid=e['reply/id'],eid=e['effect/id'],ep=e['lease/epoch'];"
       "pushReply(e.text);"
       "function fail(msg){send('fact/playback-failed',{'reply/id':rid,'effect/id':eid,'lease/epoch':ep,error:String(msg)});}"
       "try{audio.src='/api/bridge/tts?text='+encodeURIComponent(e.text);"
       "if(e.rate)audio.playbackRate=e.rate;"
       "audio.onplaying=function(){send('fact/playback-started',{'reply/id':rid,'effect/id':eid,'lease/epoch':ep});};"
       "audio.onended=function(){send('fact/playback-finished',{'reply/id':rid,'effect/id':eid,'lease/epoch':ep});};"
       "audio.onerror=function(){fail('audio element error');};"
       "audio.play().catch(function(err){fail(err);});}catch(err){fail(err);}}"
       ;; armed reflexes (D3): the threshold ARRIVES in the payload. The page holds a
       ;; timer, never a number.
       "function armReflex(e){if(e.reflex!=='watchdog-stop')return;var ms=e.params&&e.params['max-ms'];if(!ms)return;"
       "var ep=e['lease/epoch'];clearTimeout(wd);wd=setTimeout(function(){send('fact/reflex-fired',{reflex:'watchdog-stop','lease/epoch':ep,action:'stop'});},ms);}"
       "function disarmReflex(e){if(e.reflex==='watchdog-stop'){clearTimeout(wd);wd=0;}}"
       ;; cues: two fixed tones. Frequencies are presentation, not policy.
       "function beep(kind){try{if(!actx)return;if(actx.state!=='running')actx.resume();"
       "var notes=(kind==='transmit')?[660,990]:[880];var t=actx.currentTime;"
       "for(var i=0;i<notes.length;i++){var o=actx.createOscillator(),g=actx.createGain();o.type='sine';o.frequency.value=notes[i];"
       "o.connect(g);g.connect(actx.destination);g.gain.setValueAtTime(0.0001,t);g.gain.exponentialRampToValueAtTime(0.16,t+0.01);"
       "g.gain.exponentialRampToValueAtTime(0.0001,t+0.14);o.start(t);o.stop(t+0.15);t+=0.12;}"
       "try{navigator.vibrate&&navigator.vibrate(12);}catch(x){}}catch(e){}}"
       ;; --- MEASURE. RMS -> dBFS, the normalized unit the policy is written in
       ;; (D4 allows normalized energy measurement and nothing more). Facts are
       ;; queued at sample time so their timestamps stay honest, and flushed in
       ;; batches so the radio is not woken 20 times a second.
       "function measure(){if(!started)return;"
       "try{if(actx&&actx.state!=='running'){try{actx.resume();}catch(x){}}"
       "analyser.getFloatTimeDomainData(buf);var s=0;for(var i=0;i<buf.length;i++){s+=buf[i]*buf[i];}"
       "var rms=Math.sqrt(s/buf.length);var db=20*Math.log10(Math.max(rms,0.00001));"
       "el('meterfill').style.width=Math.max(0,Math.min(100,db+100))+'%';"
       "el('db').textContent=db.toFixed(1)+' dBFS';"
       "if(cap.epoch!==null){pending.push(env('fact/audio-energy',{'lease/epoch':cap.epoch,'audio/seq':ASEQ++,'rms-dbfs':Math.round(db*10)/10}));}"
       "}catch(err){note('meter: '+err);}finally{setTimeout(measure,SAMPLE_MS);}}"
       "function flush(){if(pending.length){var b=pending;pending=[];post(b);}setTimeout(flush,BATCH_MS);}"
       ;; --- DISPLAY. Every string below came from the server's view. -------------
       "function esc(t){return String(t==null?'':t).replace(/</g,'&lt;');}"
       "function render(v){if(!v)return;"
       "el('state').textContent=v.status;"
       "el('badges').textContent='mode='+v.mode+'  phase='+v.phase+'  mic='+(v['mic-open?']?'HOT':'off')+'  capture-epoch='+(v['capture-epoch']||'-')+'  queue='+v.queue+(v['muted?']?'  MUTED':'');"
       ;; The lost-capture list is the SERVER's, verbatim: the page renders the
       ;; rows the reducer says exist and composes no verdict of its own.
       "var f=el('failed');f.innerHTML='';"
       "(v['upload-failures']||[]).forEach(function(u){var d=document.createElement('div');d.className='failrow';"
       "d.textContent='⚠ this capture never reached the server ('+esc(u['error/kind'])+(u['http/status']?' '+u['http/status']:'')+', tries: '+u.attempt+')';"
       "f.appendChild(d);});"
       "var l=el('log');l.innerHTML='';"
       "(v.transcripts||[]).slice().reverse().forEach(function(t){var d=document.createElement('div');d.className='row you';"
       "d.innerHTML='<div class=lbl>you · '+esc(t.status)+'</div>'+esc(t.text);l.appendChild(d);});}"
       "function pushReply(t){var d=document.createElement('div');d.className='row bridge';"
       "d.innerHTML='<div class=lbl>bridge</div>'+esc(t);var p=el('replies');p.insertBefore(d,p.firstChild);}"
       ;; --- REPLY INTAKE. Mechanical forwarding: drain the seat's mailbox and hand
       ;; each reply to the reducer VERBATIM. Dedup (P9), seeding (P8), ordering and
       ;; muting are all decided there, on the server's own reply identity.
       "function pollReplies(){fetch('/api/voice-lab/replies?seat='+encodeURIComponent(SEAT))"
       ".then(function(r){return r.ok?r.json():null;}).then(function(j){"
       "if(j&&j.replies&&j.replies.length){post(j.replies.filter(function(x){return x&&x.text;})"
       ".map(function(x){return env('fact/reply-arrived',{'reply/id':String(x.reply_id||x.ts),'reply/text':String(x.text)});}));}"
       "}).catch(function(){}).then(function(){setTimeout(pollReplies,POLL_MS);});}"
       ;; --- iOS audio unlock has to happen inside the start gesture.
       "function unlock(){try{audio.src='data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=';audio.play().catch(function(){});}catch(e){}}"
       ;; --- COMMANDS. Every button is one envelope; not one of them decides anything.
       "el('start').onclick=function(){if(started){send('command/hf-start',null);return;}"
       "navigator.mediaDevices.getUserMedia({audio:{echoCancellation:true,noiseSuppression:true,autoGainControl:true}}).then(function(s){"
       "stream=s;actx=new (window.AudioContext||window.webkitAudioContext)();if(actx.state!=='running')actx.resume();"
       "var src=actx.createMediaStreamSource(s);analyser=actx.createAnalyser();analyser.fftSize=1024;buf=new Float32Array(analyser.fftSize);src.connect(analyser);"
       "started=true;el('start').classList.add('live');el('start').textContent='● Hands-free ON';"
       "unlock();measure();flush();pollReplies();send('command/hf-start',null);"
       "}).catch(function(e){note('mic denied: '+e);});};"
       "el('btap').onclick=function(){send('command/tap',null);};"
       "el('bstop').onclick=function(){send('command/hf-stop',null);};"
       "var muted=false;el('bmute').onclick=function(){muted=!muted;el('bmute').textContent=muted?'Unmute':'Mute';send(muted?'command/mute':'command/unmute',null);};"
       "el('bhold').addEventListener('pointerdown',function(ev){ev.preventDefault();send('command/ptt-start',null);});"
       "el('bhold').addEventListener('pointerup',function(ev){ev.preventDefault();send('command/ptt-stop',null);});"
       ;; --- LIFECYCLE. Releasing the mic is a platform fact, not a product decision.
       "document.addEventListener('visibilitychange',function(){if(document.visibilityState==='visible'&&started)connect();});"
       "window.addEventListener('pagehide',function(){started=false;try{if(es)es.close();}catch(e){}"
       "try{stream&&stream.getTracks().forEach(function(t){t.stop();});}catch(e){}try{actx&&actx.close();}catch(e){}});"
       "connect();"
       "</script></body></html>"))

(defn handle-voice-lab-page
  "GET /voice-lab -> the S5a reducer surface (see the block comment above)."
  [_req]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"
             "Cache-Control" "no-cache, no-store, must-revalidate"}
   :body (voice-lab-reducer-page-html)})

(defn handle-voice-lab-legacy-page
  "GET /voice-lab-legacy -> the PRE-S5a hands-free engine, byte-for-byte.

   The receipt window (D11/S0): the engine the reducer surface replaces, kept
   runnable beside it so 'did the old one do that?' is a question with an answer
   instead of a memory. It keeps its own endpoints (/api/voice-lab/*), so the two
   surfaces share nothing but the seat."
  [_req]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"
             "Cache-Control" "no-cache, no-store, must-revalidate"}
   :body (voice-lab-page-html)})

;; ====================================================================================
;; /bridge4 — bridge3's SEAT-AWARE + MULTIPLAYER backend/UI (server-owned conversation,
;; per-seat SSE fan-out, broadcast reply path) with the /voice-lab HANDS-FREE input
;; engine (VAD + keyword "over"+pause) REPLACING push-to-talk. PURELY ADDITIVE: it
;; REUSES bridge3's endpoints (/sse/bridge3, /api/bridge3/dictate, /api/bridge3/say,
;; /api/bridge3/convo, /api/bridge3/state, /api/bridge3/sessions) and bridge3's state
;; (bridge3-convos / bridge3-subs / the replies watch), and adds exactly ONE new
;; endpoint — /api/bridge4/keyword-check — that REUSES the pure voice-lab-keyword-finalize
;; and feeds the bridge3 broadcast path. NOTHING above bridge3/voice-lab is modified.
;; ====================================================================================

(defonce bridge4-keyword-seen (atom {:order [] :m {}})) ; logical utterance dedup_id -> final transcript

(defn handle-bridge4-keyword-check
  "POST multipart audio (+ ?seat=X &end=over) -> Groq STT -> end-word endpoint check,
   SEAT-AWARE + MULTIPLAYER. Reuses the PURE voice-lab-keyword-finalize. On a finalized
   utterance, enqueues the stripped text to the seat mailbox AND appends a 'you' turn to
   the bridge3 conversation (broadcast to every bridge4/bridge3 tab on the seat), so the
   reply returns through bridge4's server-owned #convo via the existing bridge3 replies
   watch. If NOT ended (mid-thought pause) -> {:ok true :ended false :transcript raw} and
   nothing is enqueued. A blank/too-short STT result is 'not done', NOT an error, so a
   road-noise pause never breaks the loop. Purely additive — touches no existing handler."
  [req]
  (let [seat     (or (seat-param req) "bridge")
        end-word (or (get-in req [:params :end]) "over")
        dedup-id (get-in req [:params :dedup_id])
        audio-duration-ms (audio-duration-ms-param req)
        cached   (when (seq dedup-id) (get-in @bridge4-keyword-seen [:m dedup-id]))]
    (cond
      (not (valid-seat? seat))
      (json-response {:error "invalid seat"} 400)

      cached
      (do (log/info :bridge4-keyword-dup :id dedup-id :seat seat)
          (shadow-post-decision! seat :bridge4-keyword-check :duplicate dedup-id cached false nil)
          (json-response {:ok true :ended true :transcript cached :dedup true}))

      :else
      (let [audio (get-in req [:params :audio])
            tmp   (:tempfile audio)]
        (if-not tmp
          (json-response {:error "audio required"} 400)
          (let [ext   (or (some->> (:filename audio) (re-find #"\.[A-Za-z0-9]+$")) ".webm")
                named (java.io.File/createTempFile "bridge4-kw-" ext)]
            (try
              (io/copy tmp named)
              ;; bd 0ar: persist the VERBATIM bytes BEFORE transcription, so a
              ;; capture survives even when STT (or this whole handler) throws.
              ;; This is the exact upload that was lost on 2026-07-26.
              (let [arc (capture-archive/archive-audio!
                          {:seat seat :source :bridge4-keyword-check
                           :dedup-id dedup-id :filename (:filename audio)}
                          named)]
                (try
                  (let [{:keys [transcript]} ((requiring-resolve 'marvin-voice-remote.groq/transcribe-audio) (.getPath named))]
                    (if (clojure.string/blank? transcript)
                      (do (capture-archive/archive-meta! arc {:verdict :terminal :transcript ""
                                                              :reason :blank-stt :end-word end-word})
                          (shadow-post-decision! seat :bridge4-keyword-check :terminal dedup-id ""
                                                 false {:posted? false :reason :blank-stt})
                          (json-response {:ok true :ended false :transcript ""}))
                      (let [{:keys [ended text]} (voice-lab-keyword-finalize transcript end-word)]
                        (if ended
                          (do
                            ;; Remember before returning so a lost response followed by another
                            ;; native rolling check cannot enqueue the same logical utterance twice.
                            (when (seq dedup-id)
                              (swap! bridge4-keyword-seen dictate-remember dedup-id text))
                            (swap! channel-state enqueue :messages seat
                                   {:from "gene" :text text :ts (now-iso)
                                    :audio-duration-ms audio-duration-ms})
                            (bridge3-append-turn! seat {:role "you" :text (str text) :ts (now-iso)})
                            (log/info :bridge4-keyword-send :seat seat :end end-word
                                      :id dedup-id :chars (count text)
                                      :audio-duration-ms audio-duration-ms)
                            (capture-archive/archive-meta! arc {:verdict :accepted :transcript transcript
                                                                :posted? true :end-word end-word
                                                                :audio-duration-ms audio-duration-ms})
                            (shadow-post-decision! seat :bridge4-keyword-check :accepted dedup-id transcript
                                                   false {:posted? true :end-word end-word})
                            (json-response {:ok true :ended true :transcript text}))
                          (do (capture-archive/archive-meta! arc {:verdict :continued :transcript transcript
                                                                  :posted? false :end-word end-word})
                              (shadow-post-decision! seat :bridge4-keyword-check :continued dedup-id transcript
                                                     false {:posted? false :end-word end-word})
                              (json-response {:ok true :ended false :transcript transcript}))))))
                  (catch Exception e
                    (let [{:keys [status]} (ex-data e)]
                      ;; soft error: keep the hands-free loop alive across a transient STT hiccup.
                      (log/warn :bridge4-keyword-check-soft-error :seat seat :err (.getMessage e) :status status)
                      (capture-archive/archive-meta! arc {:verdict :error :error (.getMessage e)
                                                          :status status :end-word end-word})
                      (shadow-post-decision! seat :bridge4-keyword-check :error dedup-id nil nil
                                             {:error (.getMessage e) :status status})
                      (json-response {:ok true :ended false :transcript "" :soft_error (.getMessage e)})))))
              (finally (.delete named)))))))))

(defn- bridge4-page-html
  "Build the /bridge4 page for one SEAT. SAME server-owned multiplayer model as /bridge3
   (subscribes to /sse/bridge3?seat=X; morphs #convo / #convo-status / #last-heard / #ver
   by id; speaks NEW bridge turns it finds in the broadcast #convo, dedup by :ts) — but
   the INPUT is the /voice-lab HANDS-FREE engine (VAD + keyword 'over'+pause) instead of
   push-to-talk. KEYWORD is the default (?mode=keyword&end=over); ?mode=silence reaches
   end-on-quiet. Tunables (energy/minspeech/floor/endpause/silence/seat) read from the URL.
   The ONLY client JS is browser-native input (mic, Web Audio AnalyserNode, MediaRecorder,
   timers) + posting audio to bridge3's seat dictate (silence mode) or /api/bridge4/
   keyword-check (keyword mode). The reply audio + transport reuse bridge3's proven path."
  ([seat]
   (bridge4-page-html seat (friction-ui/effective-state :bridge4)))
  ([seat friction-state]
   (let [convo  (get @bridge3-convos seat [])
        ;; One id per PAGE LOAD — see bridge3-page-html. bridge4 shares bridge3's
        ;; stream, so it needs the same reconnect identity to be superseded by.
         client (sse/new-client-id)
         cur    (voice-for-seat seat)
         voices (var-get (requiring-resolve 'marvin-voice-remote.tts/voices))
         voice-opts (apply str
                           (for [v voices]
                             (str "<option value=\"" (:id v) "\""
                                  (when (= (:id v) cur) " selected")
                                  ">" (html-escape (:name v)) "</option>")))
         tsize  @bridge-text-size
         size-opts (apply str
                          (for [[px label] bridge-text-sizes]
                            (str "<option value=\"" px "\""
                                 (when (= px tsize) " selected")
                                 ">" label "</option>")))]
     (str "<!doctype html><html><head><meta charset=utf-8>"
          "<meta name=viewport content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
          "<title>Talk to Bridge (v4) · " (html-escape seat) "</title>"
          "<script type=module src=\"/vendor/datastar-aliased.js?v=" (System/currentTimeMillis) "\"></script>"
          "<style>"
          "*{box-sizing:border-box}body{font-family:-apple-system,system-ui,sans-serif;margin:0;padding:16px;"
          "background:#f7f7f8;color:#1a1a1a}h1{font-size:20px;margin:8px 0 12px}"
          "#start{width:100%;height:120px;font-size:24px;font-weight:700;border:none;border-radius:18px;"
          "color:#fff;background:#2563eb;-webkit-tap-highlight-color:transparent}#start.live{background:#16a34a}"
          "#modeline{margin:10px 2px 4px;font-size:15px;font-weight:700;color:#b45309}"
          "#hfstate{margin:10px 2px;font-size:18px;font-weight:700;min-height:26px;color:#1a1a1a}"
          "#rectimer{display:none;margin:8px 2px;font-size:18px;font-weight:800;color:#b91c1c;font-variant-numeric:tabular-nums}"
          "#meterwrap{height:14px;background:#e5e7eb;border-radius:8px;overflow:hidden;margin:8px 0}"
          "#meterfill{height:100%;width:0%;background:#16a34a;transition:width .05s linear}"
          "#thresh{position:relative;height:0}#threshline{position:absolute;top:-38px;width:2px;height:28px;background:#b45309}"
          "#params{margin:10px 2px;font-size:12px;color:#6b7280;font-family:ui-monospace,monospace;line-height:1.6}"
          ".hint{font-size:12px;color:#6b7280;margin:8px 2px;line-height:1.5}"
          ".friction-note{font-size:11px;color:#6b7280;margin:-8px 2px 12px;line-height:1.35}"
          "#statusline{margin:14px 2px;font-size:15px;color:#555;min-height:20px}#status{color:#555}"
          bridge-surface-nav-css
          ".msg{margin:10px 0;padding:12px 14px;border-radius:12px;font-size:var(--msg-size,19px);line-height:1.35}"
          ".msg{position:relative}"
          ".copybtn{margin-top:8px;background:#eef1f4;color:#555;border:1px solid #d8dee4;border-radius:7px;padding:3px 10px;font-size:12px;font-weight:600;cursor:pointer;-webkit-tap-highlight-color:transparent}.copybtn:hover{background:#e2e7ec}"
          ".playbtn{margin-top:8px;margin-right:6px;background:#e9f7ee;color:#1a7f37;border:1px solid #bfe3cc;border-radius:7px;padding:3px 10px;font-size:12px;font-weight:600;cursor:pointer;-webkit-tap-highlight-color:transparent}.playbtn:hover{background:#d8f0e0}"
          "#toast{position:fixed;top:14px;right:14px;background:#1a7f37;color:#fff;padding:9px 16px;border-radius:9px;font-size:14px;font-weight:600;box-shadow:0 8px 26px rgba(31,35,40,.28);opacity:0;transform:translateY(-8px);pointer-events:none;z-index:1000;transition:opacity .18s,transform .18s}#toast.show{opacity:1;transform:translateY(0)}"
          ".me{background:#e7eefe}.bridge{background:#e9f7ee}.lbl{font-size:11px;color:#888;text-transform:uppercase;letter-spacing:.04em}"
          "#ctl{display:flex;gap:8px;margin:12px 0 6px;flex-wrap:wrap}#ctl2{display:flex;gap:8px;margin:0 0 12px;flex-wrap:wrap}.c{flex:1;min-width:64px;height:50px;font-size:15px;font-weight:600;border:none;border-radius:10px;background:#e5e7eb;color:#111;-webkit-tap-highlight-color:transparent}"
          "#ver{position:fixed;top:6px;right:8px;font-size:22px;font-weight:700;color:#111;text-align:right;pointer-events:none;z-index:10;line-height:1.2}#ver .sha{font-size:15px;font-weight:600;color:#555}"
          "#convo-status{display:inline-flex;align-items:center;gap:6px;font-size:15px;color:#555;vertical-align:middle}"
          "#convo-status.idle{color:#888}"
          "#convo-spinner{display:inline-block;width:14px;text-align:center;font-family:monospace}"
          ".lastheard{color:#888;font-variant-numeric:tabular-nums}"
          "#type{display:flex;gap:8px;margin:10px 0}#txt{flex:1;font-size:16px;padding:10px;border:1px solid #ccc;border-radius:10px;resize:vertical}#send{padding:0 18px;font-size:15px;font-weight:600;border:none;border-radius:10px;background:#2563eb;color:#fff;-webkit-tap-highlight-color:transparent}"
          "</style></head>"
          "<body style=\"--msg-size:" tsize "\">"
          ;; SSE stream for THIS seat — REUSES bridge3's per-seat fan-out (inner element).
          ;; `client` = this page load (supersedes only this tab's own stale stream).
          ;; `retry:'always'` is LOAD-BEARING: Datastar's default retry:'auto' only
          ;; re-opens an ERRORED request, and the server's 240–270 s rotation closes
          ;; the stream CLEANLY — with 'auto' this hands-free surface would stop
          ;; receiving replies about four minutes after load and say nothing about it.
          ;; retryInterval:3000 sets the post-recycle re-open delay and caps re-attempt
          ;; rate while the service is shedding load. `&amp;` because this is a raw
          ;; HTML attribute value; the browser decodes it before Datastar parses it.
          "<div data-star-init=\"@get('/sse/bridge3?seat=" seat "&amp;client=" client "', {retry:'always',retryInterval:3000})\" style=\"display:none\"></div>"
          (bridge2-version-fragment)
          "<h1>🌉 Talk to Bridge (v4) — hands-free</h1>"
          (bridge-surface-nav seat :hf)
          (friction-ui/note-html friction-state)
          "<button id=start>Start hands-free</button>"
          "<div id=hfstate>Tap once to grant mic + unlock audio, then talk.</div>"
          "<div id=rectimer role=status aria-live=polite></div>"
          "<div id=meterwrap><div id=meterfill></div></div>"
          "<div id=thresh><div id=threshline></div></div>"
          ;; one-tap recalibrate (Gene 2026-06-30): re-sample the ambient noise floor in place when
          ;; surroundings change (highway <-> quiet room), no stop/reload needed.
          "<button id=recal style=\"font-size:13px;font-weight:600;border:1px solid #cbd5e1;border-radius:9px;background:#f1f5f9;color:#334155;padding:7px 12px;margin:2px 0 6px;cursor:pointer;-webkit-tap-highlight-color:transparent\">🎚 recalibrate noise floor</button>"
          ;; calibration health readout (Gene 2026-07-03): bulb + calibrated-vs-normal thresholds.
          ;; 🟢 sane trigger · 🟡 high (loud env or hit the 0.05 cap — deaf-risk, recalibrate somewhere quieter)
          "<span id=calinfo style=\"font-size:12px;color:#6b7280;margin-left:8px\"></span>"
          ;; one-tap TTS boost (Gene 2026-07-03): apply ?ttsgain=2.5 without typing URL params
          ;; on the phone — merge into the CURRENT URL (preserving seat/mode/etc) and reload.
          ;; ?ttsgain=1 resets; documented in the #diag hint below.
          "<a id=boostvoice href=\"#\" style=\"font-size:13px;font-weight:600;color:#2563eb;margin-left:8px;text-decoration:none;-webkit-tap-highlight-color:transparent\" "
          "onclick=\"var q=new URLSearchParams(location.search);q.set('ttsgain','2.5');location.search=q.toString();return false;\">🔊 boost my voice</a>"
          "<div id=toast></div>"
          "<div id=statusline>Client: <span id=status>idle</span> &nbsp;&middot;&nbsp; Bridge: "
          (bridge3-status-fragment convo)
          " &nbsp;&middot;&nbsp; "
          (bridge3-lastheard-fragment convo)
          "</div>"
          "<div id=ctl><button class=c id=back>« 10s</button><button class=c id=playpause>⏸ pause</button><button class=c id=fwd>10s »</button></div>"
          "<div id=ctl2><button class=c id=prev>⏮ prev</button><button class=c id=skip>⏭ skip (s)</button><button class=c id=latest>⏭⏭ latest</button><button class=c id=replay>↺ replay</button></div>"
          "<div id=type><textarea id=txt rows=2 placeholder=\"…or type to bridge\"></textarea><button id=send>Send</button></div>"
          "<div id=voicebar style=\"display:flex;align-items:center;gap:8px;margin:10px 0;font-size:14px;color:#555\">"
          "<label for=bridge4-voice>🔊 Other Voices</label>"
          "<select id=bridge4-voice style=\"flex:1;padding:8px;border:1px solid #ccc;border-radius:10px;background:#fff;color:#111;font-size:15px;-webkit-tap-highlight-color:transparent\" "
          "onchange=\"fetch('/api/bridge/voice',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'voice_id='+encodeURIComponent(this.value)+'&seat='+encodeURIComponent(SEAT)}).then(()=>showToast('\\u2713 Voice set'));\">"
          voice-opts
          "</select></div>"
          "<div id=textsizebar style=\"display:flex;align-items:center;gap:8px;margin:10px 0;font-size:14px;color:#555\">"
          "<label for=bridge4-textsize>🔠 Text Size</label>"
          "<select id=bridge4-textsize style=\"flex:1;padding:8px;border:1px solid #ccc;border-radius:10px;background:#fff;color:#111;font-size:15px;-webkit-tap-highlight-color:transparent\" "
          "onchange=\"document.body.style.setProperty('--msg-size',this.value);fetch('/api/bridge/text-size',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'size='+encodeURIComponent(this.value)}).then(()=>showToast('\\u2713 Text size'));\">"
          size-opts
          "</select></div>"
          (bridge3-convo-fragment convo)
          ;; Diagnostics moved to the bottom (Gene 2026-06-30): mode + live params + URL-tuning
          ;; hint were too distracting above the buttons. Meter stays up top; readouts live here, muted.
          "<div id=diag style=\"margin-top:20px;padding-top:12px;border-top:1px solid #e5e7eb;font-size:12px;color:#9ca3af\">"
          "<div id=modeline></div>"
          "<div id=params></div>"
          "<div class=hint>Tune live by editing the URL and reloading: "
          "<b>?mode=</b>keyword (default — say the end-word + pause) or silence · "
          "<b>?end=</b>end-word (over/out/done) · <b>?cancel=</b>cancel-word (default cancel) · <b>?endpause=</b>ms pause that triggers a check · "
          "<b>?kwcycle=</b>ms rolling keyword check even in continuous noise (default 4000) · "
          "<b>?energy=</b>RMS threshold · <b>?minspeech=</b>ms to start · <b>?floor=</b>ms blip guard · "
          "<b>?barge=1|0</b> enables/disables the per-surface barge-in field candidate · "
          "<b>?bargehold=</b>ms voice must stay above the barge threshold (default 450) · "
          "<b>?silence=</b>ms quiet to end (silence mode) · <b>?longttsbuffer=0|1</b> full-buffer automatic replies · <b>?ttsgain=</b>2.5 boost / 1 reset.</div>"
          "</div>"
          ;; --- THE SHARED PLAYER (bd 6iu): js/bridge-player.js carries the whole reply-audio
          ;; + transport machine; this inline script keeps ONLY the hands-free VAD engine,
          ;; then calls BridgePlayerBoot(). keyToggleId:'start' = the t-key toggles hands-free.
          "<script>window.__MARVIN_BRIDGE__={protocol:2,surface:'bridge4',build:" (json/write-str git-sha) ",submitPath:'/api/bridge3/dictate',dedupParam:'dedup_id',capturePolicyVersion:2,nativeHf:true,nativeReplyOfferVersion:1,nativeBargeInVersion:1};var BP={seat:" (json/write-str seat) ",page:'bridge4',build:" (json/write-str git-sha) ",keyToggleId:'start',longTtsBuffer:" (if (:long-tts-buffer? friction-state) "true" "false") "};var SEAT=BP.seat;</script>"
          "<script src=\"/js/bridge-player.js?v=" (System/currentTimeMillis) "\"></script>"
          "<script>"
          ;; --- URL-tunable hands-free params (read live; defaults documented on the page) ---
          "var qp=new URLSearchParams(location.search);"
          "function num(k,d){var v=parseFloat(qp.get(k));return (isFinite(v)&&v>0)?v:d;}"
          "var P={energy:num('energy',0.015),silence:num('silence',900),nativesilence:num('nativesilence',2200),minspeech:num('minspeech',300),floor:num('floor',400),seat:SEAT,end:((qp.get('end')||'over').toLowerCase()),cancel:((qp.get('cancel')||'cancel').toLowerCase()),endpause:num('endpause',1500),kwcycle:num('kwcycle',4000),barge:" (if (:barge-in? friction-state) "true" "false") ",bargehold:num('bargehold',450),quietclose:num('quietclose',20000),maxutter:num('maxutter',120000),cycle:num('cycle',3000)};"
          ;; AMBIENT NOISE CALIBRATION (Gene 2026-06-30, driving): on Start, sample the room/car
          ;; noise for ~1.5s and set P.energy to 1.5x the loudest ambient (floored at the URL/base
          ;; value), so road noise can't trip the speech trigger but a real voice still clears it.
          ;; Calibrate off the NOISE-FLOOR MEAN, not the peak (2026-07-07): the old peak*1.5
          ;; capped at 0.05 kept going DEAF — one stray word/cough during the 'stay quiet' second
          ;; spiked the peak, so P.energy pinned at the 0.05 cap and normal speech never crossed it
          ;; (confirmed in prod: th:0.05, zero recordings). Mean barely moves for a single loud
          ;; sample, and the cap drops to 0.035 (below the deaf zone). ?cal=0 skips calibration
          ;; entirely and uses the fixed ?energy= value — a field escape hatch.
          "var energyBase=P.energy,calibrating=false,calStart=0,ambientPeak=0,calSum=0,calCount=0,calEnabled=(qp.get('cal')!=='0');"
          ;; barge-in threshold, CAPPED (ir5-A, Gene field-confirmed 2x: P.energy*2 post-calibration
          ;; = 0.10 = the meter's max — an interrupting voice can never reach it). Named fn = unit-testable.
          "function bargeTh(){return Math.min(P.energy*2,0.06);}"
          "var mreq=(qp.get('mode')||'keyword').toLowerCase();var mode=(mreq==='silence'||mreq==='vad')?'silence':'keyword';var KW=(mode==='keyword');"
          "document.getElementById('modeline').textContent='MODE: '+(nativeHost()?'NATIVE ':'')+mode.toUpperCase()+(KW?(' \\u2014 say \"'+P.end+'\" to send, \"'+P.cancel+'\" to cancel + pause'):' \\u2014 a quiet pause ends the message');"
          "document.getElementById('params').textContent='ACTIVE: energy='+P.energy+'  minspeech='+P.minspeech+'ms  nativebarge='+(P.barge?'ON':'OFF')+'  bargehold='+P.bargehold+'ms  floor='+P.floor+'ms  seat='+P.seat+(KW?('  | end=\"'+P.end+'\"  cancel=\"'+P.cancel+'\"  endpause='+P.endpause+'ms  kwcycle='+P.kwcycle+'ms'):('  silence='+P.silence+'ms'));"
          "(function(){var pct=Math.min(100,P.energy/0.1*100);document.getElementById('threshline').style.left=pct+'%';})();"
          ;; Engine flags ride the shared player's beacons (BP.snapExtra); the engine's own
          ;; started-gated heartbeat keeps the freeze signature (frozen `f` while hb ticks).
          "BP.snapExtra=function(){return {rec:recording,mrs:(mr?mr.state:null)};};"
          "setInterval(function(){if(started)clog({k:'hb',f:frames,rec:recording,mrs:(mr?mr.state:null),ck:checking,sn:sending,pl:playing,th:Math.round(P.energy*1e4)/1e4,cal:calibrating});},2000);"
          ;; --- engine state (from voice-lab; actx/audio/queue live in bridge-player.js) ----
          ;; EVENT-PUMP detection (2026-07-07): the old setTimeout(50) VAD loop went ~1Hz deaf
          ;; whenever iOS throttled JS timers (screen off/backgrounded — proven in prod: frames +3/3s).
          ;; Energy detection now runs in a ScriptProcessorNode.onaudioprocess callback. NB this
          ;; still executes on the MAIN thread — the win is that its events are pumped by the AUDIO
          ;; CLOCK (2048-sample buffers ≈ 23/s at 48kHz, matching prod frames exactly), and iOS keeps
          ;; delivering them while the mic session is live, unlike setTimeout which gets throttled.
          ;; (A fully-blocked main thread would still starve it — that's what the hb/frames signature
          ;; is for.) A slow keep-alive resumes a suspended context even when itself throttled.
          "var micStream,mr,chunks=[],recMime='',wl=null,frames=0,spNode=null,muteNode=null,curEnergy=0,keepAlive=0,cycleStartAt=0;"
          "var started=false,recording=false,stopping=false,sending=false,checking=false,remoteMuted=false,remoteMutedAt=0;"
          "var speechStartAt=0,bargeStartAt=0,lastVoiceAt=0,lastKwCheckAt=0,recStartAt=0,recDur=0;"
          ;; --- helpers (playback/copy/replay/scanBridge/transport: bridge-player.js) -------
          ;; CONTINUOUS CAPTURE (2026-07-07, Gene: "still 5s to actually start recording... I
          ;; started counting at 1"): the recorder now runs ALWAYS while hands-free is on (idle
          ;; cycling below), so recordingActive() must be the recording FLAG only — the old
          ;; `mr.state==='recording'` clause would make the playback mutex permanently true.
          "function recordingActive(){return nativeHost()?!!(nativeHfStatus&&nativeHfStatus.phase==='recording'):recording===true;}"
          "function extFor(m){if(!m)return '.webm';if(m.indexOf('mp4')>=0)return '.mp4';if(m.indexOf('ogg')>=0)return '.ogg';return '.webm';}"
          "const stateEl=document.getElementById('hfstate'),meter=document.getElementById('meterfill'),startBtn=document.getElementById('start'),recTimerEl=document.getElementById('rectimer');"
          "function setState(t){if(stateEl)stateEl.textContent=t;}"
          ;; One semantic CaptureEngine interface, two executors. In a normal browser these
          ;; functions fall through to Web Audio below. In the TestFlight shell, bridge v2 sends
          ;; the hosted policy to Swift and WebView never calls getUserMedia for HF.
          "var nativeHfStatus=window.__MARVIN_NATIVE_HF_STATUS__||null,nativeRequestSeq=0,nativeStatusReceivedAt=0,nativePendingType=null;"
          "function nativeHost(){return !!window.ReactNativeWebView;}"
          "function postNativeHf(type){var req='hf-'+Date.now()+'-'+(++nativeRequestSeq);var cmd={protocol:2,type:type,requestId:req};if(type==='hf.start'||type==='hf.update-policy')cmd.policy={version:2,mode:mode,energyThreshold:P.energy,silenceMs:P.nativesilence,minSpeechMs:P.minspeech,endWord:P.end,cancelWord:P.cancel,endPauseMs:P.endpause,keywordCycleMs:P.kwcycle,maxUtteranceMs:P.maxutter,duplex:{version:1,enabled:P.barge,bargeHoldMs:P.bargehold}};window.ReactNativeWebView.postMessage(JSON.stringify({__marvinCommand:cmd}));clog({k:'native-hf-command',event:type,requestId:req,policy:cmd.policy||null});}"
          "function shortBuild(v){v=String(v||'unknown');return v.length>10?v.slice(0,7):v;}"
          "function applyNativeHfStatus(s){nativeHfStatus=s||{};nativeStatusReceivedAt=Date.now();window.__MARVIN_NATIVE_HF_STATUS__=nativeHfStatus;window.__MARVIN_NATIVE_CAPTURE__=!!nativeHfStatus.running;started=!!nativeHfStatus.running;startBtn.classList.toggle('live',started);startBtn.textContent=started?'\\u25cf Hands-free ON (tap to stop)':'Start hands-free';if(meter&&!started)meter.style.width='0%';var ids='TF '+shortBuild(nativeHfStatus.appBuild)+' \\u00b7 native '+shortBuild(nativeHfStatus.nativeGitSha)+' \\u00b7 web '+shortBuild(nativeHfStatus.hostedBuild);var uploads=Number(nativeHfStatus.uploadingCount||0);var delivery=uploads>0?' \\u00b7 '+uploads+' awaiting transcript':'';if(nativeHfStatus.lastError){setState('\\u26a0 '+nativeHfStatus.lastError+' \\u00b7 '+ids);}else if(started){setState('\\u25cf HF ON \\u00b7 mic: NATIVE \\u00b7 '+(nativeHfStatus.phase||'listening')+delivery+' \\u00b7 '+ids+' \\u00b7 safe to lock');}else{setState('Stopped \\u2014 '+ids+' \\u2014 tap to resume');}tickNativeRecording();}"
          "function applyNativeHfLevel(s){if(!meter||!started)return;var e=Number((s&&s.level)||0),th=Number((s&&s.threshold)||P.energy);meter.style.width=Math.min(100,Math.round(e/0.1*100))+'%';meter.style.background=(e>th)?'#dc2626':'#16a34a';if(calibrating)calTick(e,performance.now());}"
          "function tickNativeRecording(){if(!recTimerEl||!nativeHost())return;var active=!!(nativeHfStatus&&nativeHfStatus.phase==='recording');if(!active){recTimerEl.style.display='none';recTimerEl.textContent='';return;}var elapsed=Number(nativeHfStatus.recordingElapsedMs||0)+Math.max(0,Date.now()-nativeStatusReceivedAt);var sec=Math.floor(elapsed/1000);recTimerEl.style.display='block';recTimerEl.textContent='\\u25cf Recording '+Math.floor(sec/60)+':'+String(sec%60).padStart(2,'0')+(mode==='keyword'?(' \\u00b7 say \"'+P.end+'\" to send, \"'+P.cancel+'\" to cancel'):' \\u00b7 quiet pause sends');}"
          "function nativeCommandSettled(s){return !!(s&&(s.lastError||s.phase==='error'||(nativePendingType==='start'&&s.running)||(nativePendingType==='stop'&&!s.running)||(nativePendingType==='update-policy'&&Math.abs(Number(s.energyThreshold||0)-P.energy)<0.0005)));}window.addEventListener('marvin-native-hf-status',function(ev){if(nativeCommandSettled(ev.detail))nativePendingType=null;applyNativeHfStatus(ev.detail);});if(nativeHfStatus)applyNativeHfStatus(nativeHfStatus);"
          "window.addEventListener('marvin-native-hf-level',function(ev){applyNativeHfLevel(ev.detail);});"
          ;; --- HANDS-FREE ENGINE (audio-thread detection; see startEngine/computeAndTick) ----
          ;; The VAD state machine, driven by the audio thread (computeAndTick) instead of setTimeout.
          "function computeAndTick(inbuf){var s=0;for(var i=0;i<inbuf.length;i++){s+=inbuf[i]*inbuf[i];}var e=Math.sqrt(s/inbuf.length);curEnergy=e;frames++;if(remoteMuted){if(meter)meter.style.width='0%';return;}var now=performance.now();var pct=Math.min(100,Math.round(e/0.1*100));if(meter){meter.style.width=pct+'%';meter.style.background=(e>P.energy)?'#dc2626':'#16a34a';}pumpRecorder(now);if(calibrating){calTick(e,now);}else if(KW){loopKw(e,now);}else{loopSilence(e,now);}}"
          ;; CONTINUOUS CAPTURE: the recorder runs from the moment hands-free turns on. While idle
          ;; it restarts every P.cycle ms (chunk #1 of a MediaRecorder holds the container header,
          ;; so a rolling buffer can't drop old chunks — instead we cycle whole short segments; on
          ;; speech onset the current segment simply KEEPS recording, giving 0-3s of pre-roll that
          ;; contains the words that used to be clipped, and ZERO recorder-spinup lag at onset —
          ;; the fix for both the ~5s start lag and the first-word clipping, bd ghu). Echo bleed of
          ;; a reply into the pre-roll is attenuated by echoCancellation:true on the stream. The
          ;; pump is the SINGLE owner of recorder liveness: after any send/stop path leaves mr
          ;; inactive, the next audio event revives it (~46ms) — no per-path restart bookkeeping.
          ;; Races: guarded by recording/checking/sending/stopping so it can never reset chunks
          ;; while an utterance is being closed out (onRecStop runs under `stopping`).
          "function startIdleCycle(){if(!started||!micStream)return;try{mr=new MediaRecorder(micStream);recMime=mr.mimeType||'';mr.ondataavailable=function(ev){if(ev.data&&ev.data.size>0)chunks.push(ev.data);};chunks=[];cycleStartAt=performance.now();mr.start(250);}catch(e){clog({k:'cycle-err',err:String(e)});}}"
          ;; NB every INTENTIONAL stop below neuters ondataavailable first: MediaRecorder fires a
          ;; final dataavailable asynchronously after stop(), and without the neuter that tail
          ;; chunk (which has no container header) would land in the NEXT cycle's chunks array —
          ;; a malformed blob. The two paths that WANT the final chunk (silence-mode stopRec and
          ;; the stop-hands-free flush) build their blob in onstop, which fires after it.
          "function pumpRecorder(now){if(recording||checking||sending||stopping)return;if(!mr||mr.state!=='recording'){startIdleCycle();}else if(now-cycleStartAt>=P.cycle){try{mr.ondataavailable=null;mr.stop();}catch(e){}startIdleCycle();}}"
          ;; runaway guard (ported from voice-lab, was MISSING here): keyword mode records through
          ;; pauses, so in a noisy car lastVoiceAt stays fresh forever — cut after quietclose ms of
          ;; continuous quiet or maxutter ms total, FLUSH (never drop) anything substantive. With
          ;; the lower calibration cap raising false-trigger odds, this is the Groq-bill fuse.
          "function kwRunaway(now){if(checking||sending)return;clog({k:'runaway-cut',dur:Math.round(now-recStartAt),quiet:Math.round(now-lastVoiceAt)});recording=false;var had=chunks;chunks=[];try{if(mr){mr.ondataavailable=null;if(mr.state==='recording')mr.stop();}}catch(e){}if(had.length===0){setState('\\u25cf Listening\\u2026 (auto-reset)');return;}sendUtterance(new Blob(had,{type:recMime||'audio/webm'}));}"
          "function applyCapturePolicy(old,mean){clog({k:'noise-floor-calibrated',oldThreshold:old,newThreshold:P.energy,mean:mean,samples:calCount});if(nativeHost()){nativePendingType='update-policy';setState('\\u23f3 Applying native noise floor '+P.energy.toFixed(4)+'\\u2026');postNativeHf('hf.update-policy');}else{setState('\\u25cf Listening\\u2026');}}"
          "function calTick(e,now){if(!playing){calSum+=e;calCount++;if(e>ambientPeak)ambientPeak=e;}if(now-calStart>=1500){calibrating=false;var old=P.energy,mean=calCount?calSum/calCount:energyBase;P.energy=Math.min(Math.max(energyBase,mean*3),0.035);var tl=document.getElementById('threshline');if(tl)tl.style.left=Math.min(100,P.energy/0.1*100)+'%';var ci=document.getElementById('calinfo');if(ci){var ok=P.energy<0.03;ci.textContent=(ok?'\\ud83d\\udfe2':'\\ud83d\\udfe1')+' trigger '+P.energy.toFixed(4)+' \\u00b7 normal '+energyBase.toFixed(4)+(ok?'':' \\u00b7 HIGH \\u2014 recalibrate somewhere quieter');}var pe=document.getElementById('params');if(pe)pe.textContent='ACTIVE: energy='+P.energy.toFixed(4)+' (calibrated)  minspeech='+P.minspeech+'ms  nativebarge='+(P.barge?'ON':'OFF')+'  bargehold='+P.bargehold+'ms  floor='+P.floor+'ms  seat='+P.seat+(KW?('  | end=\"'+P.end+'\"  cancel=\"'+P.cancel+'\"  endpause='+P.endpause+'ms  kwcycle='+P.kwcycle+'ms'):'');applyCapturePolicy(old,mean);}else{setState('\\u2699 Calibrating to your surroundings\\u2026 stay quiet');}}"
          ;; mic -> ScriptProcessor (energy on the audio thread) -> gain(0) -> destination.
          ;; The gain(0) sink is required for the node to fire onaudioprocess, and mutes it so the
          ;; mic never reaches the speakers (no echo/feedback).
          "function startEngine(stream){micStream=stream;actx=actx||new (window.AudioContext||window.webkitAudioContext)();if(actx.state!=='running')actx.resume();var src=actx.createMediaStreamSource(stream);spNode=actx.createScriptProcessor(2048,1,1);spNode.onaudioprocess=function(ev){if(!started)return;try{if(actx.state!=='running')actx.resume();}catch(_){}try{computeAndTick(ev.inputBuffer.getChannelData(0));}catch(err){setState('\\u26a0 loop hiccup: '+((err&&err.message)||err)+' \\u2014 recovering');}};muteNode=actx.createGain();muteNode.gain.value=0;src.connect(spNode);spNode.connect(muteNode);muteNode.connect(actx.destination);}"
          "function teardownEngine(){try{if(spNode){spNode.onaudioprocess=null;spNode.disconnect();}}catch(e){}try{muteNode&&muteNode.disconnect();}catch(e){}spNode=null;muteNode=null;stopKeepAlive();}"
          ;; keep-alive: even throttled to ~1Hz in the background, this resumes a suspended context so
          ;; onaudioprocess (and thus detection) restarts — the belt to the audio-thread suspenders.
          "function startKeepAlive(){if(keepAlive)return;keepAlive=setInterval(function(){if(!started)return;try{if(actx&&actx.state!=='running')actx.resume();}catch(e){}},800);}"
          "function stopKeepAlive(){if(keepAlive){clearInterval(keepAlive);keepAlive=0;}}"
          "function lockScreen(){try{if('wakeLock' in navigator){navigator.wakeLock.request('screen').then(function(s){wl=s;}).catch(function(){});}}catch(e){}}"
          "function onsetReady(e,now){if(playing){speechStartAt=0;if(e>bargeTh()){if(bargeStartAt===0)bargeStartAt=now;return now-bargeStartAt>=P.bargehold;}bargeStartAt=0;return false;}bargeStartAt=0;if(e>P.energy){if(speechStartAt===0)speechStartAt=now;return now-speechStartAt>=P.minspeech;}speechStartAt=0;return false;}"
          "function loopSilence(e,now){"
          "if(!recording&&!sending&&!stopping){if(onsetReady(e,now)){if(playing){try{audio.pause();}catch(_){}playing=false;queue.length=0;}startRec(now);}}"
          "else if(recording){if(e>P.energy)lastVoiceAt=now;if(now-lastVoiceAt>=P.silence)stopRec(now);}}"
          "function loopKw(e,now){if(checking||sending)return;"
          "if(!recording){if(onsetReady(e,now)){if(playing){try{audio.pause();}catch(_){}playing=false;queue.length=0;}startRecKw(now);}}"
          "else{if(e>P.energy)lastVoiceAt=now;if((now-lastVoiceAt>=P.quietclose)||(now-recStartAt>=P.maxutter)){kwRunaway(now);return;}if((now-lastVoiceAt>=P.endpause)||(now-lastKwCheckAt>=P.kwcycle))kwCheck(now);}}"
          ;; live recording label WITH an elapsed timer (Gene 2026-06-30): 'Recording 0:07 · say
          ;; "over" to send'. Mode-aware (no end-word hint in silence mode). Refreshed ~4x/s below.
          "function kwRec(){var s=recStartAt?Math.max(0,Math.round((performance.now()-recStartAt)/1000)):0;return '\\ud83c\\udf99 Recording '+fmtMMSS(s)+(KW?(' \\u00b7 say \"'+P.end+'\" to send, \"'+P.cancel+'\" to cancel'):'')+'\\u2026';}"
          ;; onset = FLIP THE FLAG, nothing else: the pump's recorder is already rolling (with
          ;; 0-3s of pre-roll containing the words that used to be clipped), so there is zero
          ;; spin-up lag. The pump revives a dead recorder if one somehow slipped through.
          "function startRecKw(now){recording=true;recStartAt=now;lastVoiceAt=now;lastKwCheckAt=now;speechStartAt=0;bargeStartAt=0;if(!mr||mr.state!=='recording')startIdleCycle();setState(kwRec());buzz(12);}"
          ;; last word of an STT transcript, normalized (lowercased, strip surrounding punctuation)
          ;; the same way the SERVER strips the end-word — so the client-side cancel check matches.
          "function kwLastWord(t){if(!t)return '';var w=String(t).trim().toLowerCase().replace(/[^a-z0-9\\s']/g,' ').trim().split(/\\s+/);return w.length?w[w.length-1]:'';}"
          ;; cancel cue: a DESCENDING two-tone 'bwoop' (520->300Hz), deliberately unlike the
          ;; ASCENDING/flat send beep-beep so Gene hears 'discarded' vs 'sent' eyes-closed.
          "function cueCancel(){try{actx=actx||new (window.AudioContext||window.webkitAudioContext)();if(actx.state!=='running')actx.resume();var t=actx.currentTime;var notes=[520,300];for(var i=0;i<notes.length;i++){var o=actx.createOscillator(),g=actx.createGain();o.type='sine';o.frequency.value=notes[i];o.connect(g);g.connect(actx.destination);g.gain.setValueAtTime(0.0001,t);g.gain.exponentialRampToValueAtTime(CUE_GAIN,t+0.01);g.gain.exponentialRampToValueAtTime(0.0001,t+0.16);o.start(t);o.stop(t+0.17);t+=0.14;}}catch(e){}}"
          "function kwCheck(now){if(window.__MARVIN_NATIVE_CAPTURE__||checking||sending)return;if(chunks.length===0){lastVoiceAt=now;lastKwCheckAt=now;return;}checking=true;lastKwCheckAt=now;setState('\\u23f3 Checking\\u2026');var blob=new Blob(chunks,{type:recMime||'audio/webm'});var fd=new FormData();fd.append('audio',blob,'k'+extFor(recMime));fd.append('dedup_id','b4k-'+Date.now());fd.append('audio_duration_ms',String(Math.max(0,Math.round(now-recStartAt))));"
          "fetch('/api/bridge4/keyword-check?seat='+encodeURIComponent(P.seat)+'&end='+encodeURIComponent(P.end),{method:'POST',body:fd}).then(function(r){return r.json().catch(function(){return {};}).then(function(j){"
          ;; send-confirmation cue (Gene 2026-07-03, mid-run): when 'over' fires, beep-beep like
          ;; push-to-talk so he HEARS the send without looking at the screen.
          "if(remoteMuted){checking=false;setState('\\u23f8 MIC MUTED from Director');}else if(j&&j.ended){try{if(mr){mr.ondataavailable=null;mr.stop();}}catch(e){}recording=false;chunks=[];checking=false;try{beep(2,880);}catch(e){}buzz(20);setState('\\u23f3 Sending\\u2026');setTimeout(function(){if(!recording&&!playing&&!sending&&!checking)setState('\\u25cf Listening\\u2026');},500);}"
          ;; CANCEL word as the last word (server returns ended:false + raw transcript, since
          ;; cancel!=end so it never enqueues to the LLM): DISCARD locally — stop the recorder,
          ;; drop the chunks, reset to Listening, and never touch the dictate path.
          "else if(P.cancel&&kwLastWord(j&&j.transcript)===P.cancel){try{if(mr){mr.ondataavailable=null;mr.stop();}}catch(e){}recording=false;chunks=[];checking=false;try{cueCancel();}catch(e){}buzz(30);setState('\\u2715 Canceled \\u2014 \\u25cf Listening\\u2026');setTimeout(function(){if(!recording&&!playing&&!sending&&!checking)setState('\\u25cf Listening\\u2026');},700);}"
          "else{checking=false;lastVoiceAt=lastKwCheckAt=performance.now();setState(kwRec());}"
          "});}).catch(function(e){checking=false;lastVoiceAt=lastKwCheckAt=performance.now();setState(kwRec());});}"
          "function startRec(now){if(window.__MARVIN_NATIVE_CAPTURE__)return;recording=true;recStartAt=now;lastVoiceAt=now;speechStartAt=0;if(!mr||mr.state!=='recording')startIdleCycle();setState('\\ud83c\\udf99 Heard you, recording\\u2026');buzz(12);}"
          ;; onstop is attached at STOP time (the pump's idle recorders must never have one, or a
          ;; cycle restart would fire a spurious send). recDur stays onset-based, so the blip
          ;; floor still measures SPEECH duration even though the blob now carries pre-roll.
          "function stopRec(now){recDur=now-recStartAt;recording=false;stopping=true;try{mr.onstop=onRecStop;mr.stop();}catch(e){onRecStop();}}"
          "function onRecStop(){stopping=false;if(window.__MARVIN_NATIVE_CAPTURE__){chunks=[];return;}if(recDur<P.floor){setState('\\u25cf Listening\\u2026 (ignored '+Math.round(recDur)+'ms blip)');return;}var blob=new Blob(chunks,{type:recMime||'audio/webm'});sendUtterance(blob);}"
          "function sendUtterance(blob){if(window.__MARVIN_NATIVE_CAPTURE__){sending=false;chunks=[];return Promise.resolve({ok:false,message:'Native capture owns audio delivery'});}sending=true;setState('\\u23f3 Sending\\u2026');var fd=new FormData();fd.append('audio',blob,'d'+extFor(recMime));fd.append('dedup_id','b4-'+Date.now()+'-'+Math.random().toString(36).slice(2));fd.append('audio_duration_ms',String(Math.max(0,Math.round(recDur))));"
          "return fetch('/api/bridge3/dictate?seat='+encodeURIComponent(P.seat),{method:'POST',body:fd}).then(function(r){return r.json().catch(function(){return {};}).then(function(j){"
          "var ok=!!(r.ok&&j.transcript);if(ok){setState('\\u23f3 Sent \\u2014 waiting for reply\\u2026');}"
          "else{setState('\\u26a0 '+((j&&j.error)||('HTTP '+r.status))+' \\u2014 \\u25cf listening');}"
          "sending=false;return {ok:ok,message:ok?'Current utterance entered the delivery pipeline':((j&&j.error)||('HTTP '+r.status))};});}).catch(function(e){sending=false;setState('\\u26a0 send failed \\u2014 \\u25cf listening');return {ok:false,message:'Send failed: '+String(e)};});}"
          ;; --- start/stop hands-free (one tap; toggles) ------------------------------------
          "function beginCalibration(force){P.energy=energyBase;ambientPeak=0;calSum=0;calCount=0;calibrating=!!force||calEnabled;calStart=performance.now();}"
          "function startHandsFree(){if(started||nativePendingType)return;prime();unlock();if(nativeHost()){var canCal=!!(nativeHfStatus&&nativeHfStatus.policyUpdateSupported);beginCalibration(false);calibrating=calibrating&&canCal;setState(calibrating?'\\u2699 Starting native HF + calibrating\\u2026 stay quiet a sec':'\\u23f3 Starting native HF audio\\u2026');nativePendingType='start';postNativeHf('hf.start');return;}window.__MARVIN_WEB_MIC_STARTING__=true;navigator.mediaDevices.getUserMedia({audio:{echoCancellation:true,noiseSuppression:true,autoGainControl:true}}).then(function(s){window.__MARVIN_WEB_MIC_STARTING__=false;startEngine(s);started=true;beginCalibration(false);startBtn.classList.add('live');startBtn.textContent='\\u25cf Hands-free ON (tap to stop)';setState(calEnabled?'\\u2699 Calibrating to your surroundings\\u2026 stay quiet a sec':'\\u25cf Listening\\u2026');buzz(20);lockScreen();startKeepAlive();}).catch(function(e){window.__MARVIN_WEB_MIC_STARTING__=false;setState('\\u26a0 mic denied: '+e);});}"
          "function releaseMic(){try{micStream&&micStream.getTracks().forEach(function(t){t.stop();});}catch(e){}micStream=null;try{window.ReactNativeWebView&&window.ReactNativeWebView.postMessage(JSON.stringify({__marvinWebMicReleased:true}));}catch(e){}}"
          ;; tap-to-stop FLUSHES a mid-recording utterance instead of dropping it (Gene bug
          ;; 2026-06-30: lost transcriptions by tapping stop mid-sentence). If recording with
          ;; buffered audio, stop the recorder and on its onstop send what we have via the normal
          ;; dictate path (transcribe + post), THEN release the mic; otherwise tear down cleanly.
          "function stopHandsFree(){if(nativeHost()){if(nativePendingType)return;nativePendingType='stop';setState('\\u23f3 Stopping native HF audio\\u2026');postNativeHf('hf.stop');return;}started=false;teardownEngine();try{wl&&wl.release&&wl.release();}catch(e){}wl=null;startBtn.classList.remove('live');startBtn.textContent='Start hands-free';if(meter)meter.style.width='0%';if(recording&&mr&&mr.state==='recording'&&chunks.length>0){recDur=performance.now()-recStartAt;recording=false;checking=false;stopping=false;setState('\\u23f3 Sending your last words\\u2026');mr.onstop=function(){try{sendUtterance(new Blob(chunks,{type:recMime||'audio/webm'}));}catch(e){}releaseMic();};try{mr.stop();}catch(e){try{sendUtterance(new Blob(chunks,{type:recMime||'audio/webm'}));}catch(_){}releaseMic();}}else{recording=false;sending=false;checking=false;stopping=false;try{mr&&mr.state==='recording'&&mr.stop();}catch(e){releaseMic();}if(!mr||mr.state!=='recording')releaseMic();chunks=[];setState('Stopped \\u2014 tap to resume');}}function discardDirectorCapture(){try{if(mr){mr.ondataavailable=null;mr.onstop=null;if(mr.state==='recording'||mr.state==='paused')mr.stop();}}catch(e){}mr=null;recording=false;checking=false;stopping=false;chunks=[];speechStartAt=0;bargeStartAt=0;}function finishDirectorUtterance(command){if(remoteMuted)return Promise.resolve({state:'failed',message:'Microphone is muted; unmute before sending'});if(sending||checking||stopping)return Promise.resolve({state:'failed',message:'Capture is already finalizing an utterance'});if(!recording||!mr||mr.state!=='recording')return Promise.resolve({state:'failed',message:'No active utterance to send'});recording=false;checking=false;stopping=true;recDur=performance.now()-recStartAt;setState('\\u23f3 OVER from Director \\u2014 sending\\u2026');return new Promise(function(resolve){var recorder=mr;recorder.onstop=function(){stopping=false;var blob=new Blob(chunks,{type:recMime||'audio/webm'});chunks=[];sendUtterance(blob).then(function(result){if(started&&!remoteMuted){startIdleCycle();startKeepAlive();}if(result.ok){clog({k:'director-mic-control',id:command.id,state:'over'});resolve({state:'over',message:'Current utterance finalized and sent'});}else{resolve({state:'failed',message:result.message||'Current utterance failed to send'});}});};try{recorder.requestData();recorder.stop();}catch(e){stopping=false;resolve({state:'failed',message:'Could not finalize current utterance: '+String(e)});}});}function applyDirectorMicCommand(command){var action=String((command&&command.action)||'');var wantMuted=action==='toggle-mute'?!remoteMuted:action==='mute';if(nativeHost())return {state:'failed',message:'Remote microphone control is not available in this native build'};if(!started)return {state:'failed',message:'Hands-free is stopped'};if(action==='over')return finishDirectorUtterance(command);if(action==='clear'){discardDirectorCapture();if(!remoteMuted){startIdleCycle();startKeepAlive();setState('\\u2715 Cleared \\u2014 \\u25cf Listening\\u2026');}else{setState('\\u23f8 MIC MUTED from Director');}if(meter)meter.style.width='0%';clog({k:'director-mic-control',id:command.id,state:'cleared'});return {state:'cleared',message:'Current utterance discarded; capture state preserved'};}if(wantMuted){if(!remoteMuted){remoteMuted=true;remoteMutedAt=performance.now();try{if(mr&&mr.state==='recording'){mr.requestData();mr.pause();}}catch(e){}try{micStream&&micStream.getAudioTracks().forEach(function(t){t.enabled=false;});}catch(e){}}if(meter)meter.style.width='0%';if(recTimerEl){recTimerEl.style.display='none';recTimerEl.textContent='';}setState('\\u23f8 MIC MUTED from Director');clog({k:'director-mic-control',id:command.id,state:'muted'});return {state:'muted',message:'Current utterance paused; microphone track disabled'};}if(remoteMuted){var pauseMs=remoteMutedAt?Math.max(0,performance.now()-remoteMutedAt):0;remoteMuted=false;remoteMutedAt=0;try{micStream&&micStream.getAudioTracks().forEach(function(t){t.enabled=true;});}catch(e){}if(recStartAt)recStartAt+=pauseMs;if(lastVoiceAt)lastVoiceAt+=pauseMs;if(lastKwCheckAt)lastKwCheckAt+=pauseMs;if(speechStartAt)speechStartAt+=pauseMs;if(cycleStartAt)cycleStartAt+=pauseMs;try{if(mr&&mr.state==='paused')mr.resume();else if(!mr||mr.state!=='recording')startIdleCycle();}catch(e){startIdleCycle();}}startKeepAlive();setState(recording?kwRec():'\\u25cf Listening\\u2026');clog({k:'director-mic-control',id:command.id,state:'unmuted'});return {state:'unmuted',message:'Microphone track enabled; paused utterance resumed'};}window.MarvinCaptureControl={apply:function(command){return Promise.resolve(applyDirectorMicCommand(command));},state:function(){return remoteMuted?'muted':'unmuted';}};"
          ;; TRIGGER PROVENANCE (sol, 2026-07-27): the 01:14Z field stop was indistinguishable
          ;; from a tap vs the shared 't' shortcut (bridge-player.js synthetic click()) vs a
          ;; ghost click. Every toggle now logs isTrusted + pointerType + which way it toggled
          ;; BEFORE acting, so the next field stop names its own finger.
          "startBtn.onclick=function(ev){ev=ev||{};clog({k:'hf-toggle',dir:started?'stop':'start',trusted:ev.isTrusted===true,ptr:(ev.pointerType||null),detail:(ev.detail!=null?ev.detail:null)});if(!started)startHandsFree();else stopHandsFree();};"
          ;; recalibrate in place: restart the 1.5s ambient sample (only meaningful while live).
          "document.getElementById('recal').onclick=function(){if(!started){setState('Start HF first, then tap recalibrate while the room is quiet');return;}if(nativeHost()&&!(nativeHfStatus&&nativeHfStatus.policyUpdateSupported)){setState('\\u26a0 This TestFlight build cannot apply native recalibration \\u2014 update the app');return;}beginCalibration(true);try{beep(1,520);}catch(e){}buzz(15);setState('\\u2699 Recalibrating\\u2026 stay quiet');};"
          ;; (poll/tickers/keyboard/transport: bridge-player.js — keyToggleId:'start' wires
          ;; the t-key to the hands-free toggle.)
          ;; iOS: on return to foreground, cancel any stale frame, restart the loop, re-lock,
          ;; and nudge stalled playback. On navigate-away release mic + audio session.
          "document.addEventListener('visibilitychange',function(){if(document.visibilityState!=='visible')return;warmCtx();if(started){try{if(actx&&actx.state!=='running')actx.resume();}catch(e){}startKeepAlive();lockScreen();}if(!recordingActive()&&playing&&(audio.paused||audio.ended)){playing=false;playNext();}});"
          "window.addEventListener('pagehide',function(){started=false;releaseMic();try{actx&&actx.close();}catch(e){}});"
          "window.addEventListener('beforeunload',releaseMic);"
          ;; live recording-timer refresh (Gene 2026-06-30): tick the 'Recording 0:07 · say over'
          ;; label ~4x/s while actively recording, but never stomp the Checking/Sending states.
          "setInterval(function(){if(recording&&!checking&&!sending&&!stopping)setState(kwRec());},250);"
          "setInterval(tickNativeRecording,250);tickNativeRecording();"
          "BridgePlayerBoot();"
          (str "</script><script src=\"/js/director-capture-control.js?v=" git-sha "\"></script></body></html>")))))

(defn- code-director-page-html
  ([seat mode]
   (code-director-page-html seat mode (friction-ui/effective-state :bridge4)))
  ([seat mode friction-state]
   (let [desk? (not= mode "running")
         mode-nav (str "<nav id=deskmodes aria-label=\"Code Director audio mode\">"
                       "<a " (when desk? "aria-current=page ") "href=\"/code-director?mode=desk\">⌨ Desk hands-free</a>"
                       "<a " (when-not desk? "aria-current=page ") "href=\"/code-director?mode=running\">🏃 Running hands-free</a>"
                       "</nav>")
         intro (if desk?
                 (str mode-nav
                      "<strong>Desk mode</strong> · start once, then speak hands-free. "
                      "Recent keyboard and mouse activity is treated as input noise and cannot begin a turn.")
                 (str mode-nav
                      "<strong>Running mode</strong> · the standard Bridge 4 audio treatment for movement and open air."))
         common (-> (bridge4-page-html seat friction-state)
                    (clojure.string/replace
                      (str "<title>Talk to Bridge (v4) · " (html-escape seat) "</title>")
                      "<title>Code Director Interface</title>")
                    (clojure.string/replace
                      "<h1>🌉 Talk to Bridge (v4) — hands-free</h1>"
                      (str "<h1>🎙 Code Director Interface</h1>"
                           "<section id=deskintro>" intro "</section>"))
                    (clojure.string/replace
                      "</head>"
                      (str "<style>"
                           "#deskintro{max-width:48rem;margin:0 auto 1rem;padding:.75rem 1rem;border:1px solid #6b7280;border-radius:.75rem;background:#111827;color:#f9fafb}"
                           "#deskmodes{display:flex;gap:.5rem;margin-bottom:.75rem}"
                           "#deskmodes a{padding:.55rem .8rem;border:1px solid #6b7280;border-radius:999px;color:#bfdbfe;text-decoration:none}"
                           "#deskmodes a[aria-current=page]{background:#dbeafe;color:#111827;border-color:#93c5fd;font-weight:700}"
                           "#deskhint{max-width:42rem;margin:.5rem auto 1.5rem;text-align:center;color:#6b7280}"
                           "#deskutterance{display:flex;justify-content:center;gap:.65rem;margin:.75rem auto 0}"
                           "#deskutterance button{min-height:44px;padding:.65rem 1rem;border:1px solid #6b7280;border-radius:.75rem;background:#111827;color:#f9fafb;font-weight:800}"
                           "#deskutterance #cancelutterance{background:#7f1d1d;border-color:#b91c1c}"
                           "#desknoise[data-active=true]{color:#b45309;font-weight:700}"
                           "</style></head>")))]
     (if-not desk?
       common
       (-> common
           (clojure.string/replace
             "<button id=start>Start hands-free</button>"
             (str "<button id=start>Start desk hands-free</button>"
                  "<div id=deskhint>Start once. Then speak naturally and say OVER; keyboard strikes and clicks are rejected automatically. "
                  "<span id=desknoise>Input-noise guard ready.</span>"
                  "<div id=deskutterance aria-label=\"Current utterance controls\">"
                  "<button id=overutterance type=button>OVER · send</button>"
                  "<button id=cancelutterance type=button>Cancel utterance</button>"
                  "</div></div>"))
           (clojure.string/replace
             "<script>window.__MARVIN_BRIDGE__="
             "<script>window.__MARVIN_CODE_DIRECTOR__=true;window.__MARVIN_DESK_INPUT_UNTIL__=0;</script><script>window.__MARVIN_BRIDGE__=")
           (clojure.string/replace
             "var qp=new URLSearchParams(location.search);"
             "var qp=new URLSearchParams(location.search);var deskMode=window.__MARVIN_CODE_DIRECTOR__===true;")
           (clojure.string/replace
             "function loopKw(e,now){if(checking||sending)return;"
             "function loopKw(e,now){if(checking||sending)return;if(deskMode&&!recording&&now<window.__MARVIN_DESK_INPUT_UNTIL__){speechStartAt=0;bargeStartAt=0;return;}")
           (clojure.string/replace
             "</body></html>"
             (str "<script defer src=\"/js/code-director-desk.js?v="
                  (System/currentTimeMillis)
                  "\"></script></body></html>")))))))

(defn handle-code-director-page
  [req]
  (let [seat (let [s (seat-param req)]
               (if (valid-seat? s) s "code-director-sse"))
        requested-mode (or (get-in req [:query-params "mode"])
                           (get-in req [:params :mode])
                           (get-in req [:params "mode"]))
        mode (if (= requested-mode "running") "running" "desk")]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"
               "Cache-Control" "no-cache, no-store, must-revalidate"}
     :body (code-director-page-html seat mode
                                    (friction-ui/effective-state :bridge4 req))}))

(defn handle-bridge4-page
  "GET /bridge4?seat=X -> the seat-aware multiplayer HANDS-FREE dictation page
   (default seat=bridge, default mode=keyword end=over)."
  [req]
  (let [seat (let [s (seat-param req)] (if (valid-seat? s) s "bridge"))]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"
               "Cache-Control" "no-cache, no-store, must-revalidate"}
     :body (bridge4-page-html seat (friction-ui/effective-state :bridge4 req))}))

;; ====================================================================================
;; /api/capture-archive/* — RECOVERY over the append-only durable capture archive
;; (bd marvin-voice-remote-0ar). Gene lost a multi-minute recording the server had
;; already received AND already transcribed. capture-archive now persists every
;; upload; these two endpoints are how a lost recording comes back:
;;
;;   GET  /api/capture-archive/recent?seat=X&minutes=N  -> what survived, newest first
;;   POST /api/capture-archive/recover {id, seat}       -> re-transcribe + inject
;;
;; PURELY ADDITIVE: no page is changed (the recovery UI is a later slice), no
;; existing handler, state atom, or contract is touched. Injection reuses the
;; ORDINARY dictation path — the seat mailbox + a bridge3 'you' turn — so a
;; recovered utterance is indistinguishable downstream from a live one, and it is
;; keyed by a STABLE dedup id ("recovered-<capture-id>") so recovering the same
;; capture twice can only ever produce ONE conversation turn.
;; ====================================================================================

(defn handle-capture-archive-recent
  "GET ?seat=X&minutes=N -> the capture groups still in durable storage.

   A GROUP is one logical utterance: every rolling keyword check of the same
   growing recording shares a dedup id, and the group's LONGEST capture is the
   full recording (the others are prefixes of it) — that is `best_id`, the one to
   recover. Read-only; never mutates the archive or any conversation."
  [req]
  (let [seat    (or (seat-param req) "bridge")
        minutes (or (some-> (get-in req [:params :minutes]) str parse-long) 60)]
    (cond
      (not (valid-seat? seat)) (json-response {:error "invalid seat"} 400)
      (not (capture-archive/enabled?))
      (json-response {:ok true :seat seat :minutes minutes :enabled false
                      :captures [] :groups []})
      :else
      (let [{:keys [captures groups]} (capture-archive/recent seat (min minutes 1440))]
        (json-response
          {:ok true :seat seat :minutes minutes :enabled true
           :count (count captures)
           :captures (mapv (fn [c]
                             {:id (:id c) :ts (:ts c) :bytes (:bytes c)
                              :chars (or (:chars c) 0)
                              :verdict (some-> (:verdict c) name)
                              :source (some-> (:source c) name)
                              :group (:dedup-id c)
                              ;; DURABLE marker first (survives ledger eviction and
                              ;; cold starts), in-memory ledger as the fast second.
                              :recovered (boolean
                                           (or (:recovered c)
                                               (get-in @bridge3-dictate-seen
                                                       [:m (capture-archive/recover-dedup-id (:id c))])))})
                           captures)
           :groups (mapv (fn [g]
                           {:group (:group g) :captures (:captures g)
                            :best_id (:best-id g) :best_bytes (:best-bytes g)
                            :chars (or (:chars g) 0)
                            :verdict (some-> (:verdict g) name)
                            :source (some-> (:source g) name)
                            :first_ts (:first-ts g) :last_ts (:last-ts g)})
                         groups)})))))

;; --- recover: the exactly-once gate (sol finding 2) ---------------------------
;;
;; The old shape was CHECK-THEN-ACT: read `bridge3-dictate-seen`, then transcribe,
;; then write the ledger. Two taps arriving inside the ~2s Groq call both read an
;; empty ledger and both injected — two turns, two Groq bills, from one capture.
;; And because the ledger is a bounded 64-entry in-memory map, a cold start or an
;; eviction reopened the same hole minutes later.
;;
;; Both holes are closed here:
;;
;;   1. ATOMIC CLAIM. The recovery id is claimed with a single `swap-vals!` BEFORE
;;      any work. The first claimer proceeds; a concurrent caller gets the cached
;;      answer if one has landed, otherwise a 409 "recovery in progress" — never a
;;      second Groq call, never a second injection. The claim is released in a
;;      `finally`, so a FAILED recovery stays retryable.
;;
;;      This is INSTANCE-LOCAL atomicity, and that is sufficient here ONLY because
;;      the service is pinned to --max-instances 1 (Makefile cloudrundeploy guard,
;;      bd fyg — the same assumption blob.clj's last-writer-wins model rests on).
;;      If that pin is ever lifted, this claim must become a durable lease.
;;
;;   2. DURABLE MARKER. A successful injection writes a `.recovered.edn` object
;;      next to the capture, SYNCHRONOUSLY — before the 200 and before the claim
;;      is released — so the claim is never handed back while the fact it guards
;;      is still in flight. Recovery consults it BEFORE claiming (the cheap case)
;;      and AGAIN AFTER the claim is held (the narrow case where the previous
;;      recovery committed in between), so a cold start, an evicted ledger entry,
;;      or a check-then-claim gap can no longer reinject a delivered utterance.
(defonce ^:private capture-recover-claims (atom #{}))

(defn- claim-recovery!
  "Atomically claim `dedup-id`. True only for the caller that took it."
  [dedup-id]
  (let [[old _] (swap-vals! capture-recover-claims conj dedup-id)]
    (not (contains? old dedup-id))))

(defn- release-recovery! [dedup-id]
  (swap! capture-recover-claims disj dedup-id)
  nil)

(defn handle-capture-archive-recover
  "POST {id, seat} -> fetch the stored webm, re-transcribe it through the SAME
   groq path the live dictate handlers use, and inject the result into the seat's
   conversation as an ordinary dictation.

   EXACTLY-ONCE by construction: the injection is keyed by the stable dedup id
   \"recovered-<id>\", which is checked against the durable `.recovered.edn`
   marker and the in-memory ledger and then ATOMICALLY CLAIMED before any work.
   A double tap costs one cached answer or one 409, never a second turn and never
   a second Groq call."
  [req]
  (let [body (body-json req)
        seat (or (:seat body) (seat-param req) "bridge")
        id   (str (:id body))
        dedup-id (capture-archive/recover-dedup-id id)]
    (cond
      (not (valid-seat? seat)) (json-response {:error "invalid seat"} 400)
      (not (capture-archive/valid-capture-id? id)) (json-response {:error "invalid capture id"} 400)
      :else
      (let [cached (get-in @bridge3-dictate-seen [:m dedup-id])
            ;; PRE-CLAIM durable check: the marker outlives the ledger.
            marker (when-not cached (capture-archive/recovered-record seat id))]
        (cond
          (or cached marker)
          (do (log/info :capture-recover-dup :id id :seat seat
                        :via (if cached :ledger :marker))
              (json-response {:ok true :id id :dedup_id dedup-id :dedup true
                              :transcript (or cached (:transcript marker))}))

          (not (claim-recovery! dedup-id))
          ;; Another thread is mid-recovery of this exact capture. If it has
          ;; already committed, hand back its answer; otherwise say so plainly —
          ;; a silent second transcription is the bug we are here to kill.
          (if-let [now-cached (get-in @bridge3-dictate-seen [:m dedup-id])]
            (json-response {:ok true :id id :dedup_id dedup-id :dedup true
                            :transcript now-cached})
            (do (log/info :capture-recover-in-progress :id id :seat seat)
                (json-response {:error "recovery already in progress for this capture"
                                :id id :dedup_id dedup-id :in_progress true} 409)))

          :else
          (try
            ;; A MAP, not the string: a marker whose transcript is nil is still an
            ;; answer ("already delivered"), and `if-let` on the bare string would
            ;; read that as "never recovered" and inject it a second time.
            (if-let [settled (let [c (get-in @bridge3-dictate-seen [:m dedup-id])
                                   mk (when-not c (capture-archive/recovered-record seat id))]
                               (when (or c mk) {:transcript (or c (:transcript mk))}))]
              ;; POST-CLAIM RECHECK (sol round-2 finding 2). The pre-claim check
              ;; above and the claim are two steps, and a recovery that COMMITTED
              ;; between them — the previous caller had already written the ledger
              ;; and the marker and was on its way to releasing the claim — would
              ;; otherwise be re-run here in full: a second Groq call and a second
              ;; injection of an utterance the user has already received. Re-read
              ;; both sources once the claim is held, when nothing else can be
              ;; mid-flight, and answer from what is already there.
              (do (log/info :capture-recover-dup :id id :seat seat :via :post-claim)
                  (json-response {:ok true :id id :dedup_id dedup-id :dedup true
                                  :transcript (:transcript settled)}))
              (if-let [^bytes bytes (capture-archive/fetch-audio seat id)]
                (let [named (java.io.File/createTempFile "capture-recover-" ".webm")]
                  (try
                    (with-open [out (io/output-stream named)] (.write out bytes))
                    (let [{:keys [transcript]} ((requiring-resolve 'marvin-voice-remote.groq/transcribe-audio)
                                                (.getPath named))
                          full (clojure.string/trim (strip-over (str transcript)))]
                      (if (clojure.string/blank? full)
                        (do (log/warn :capture-recover-empty :id id :seat seat)
                            (json-response {:error "no speech in the recovered capture"
                                            :id id :bytes (alength bytes)} 400))
                        (do
                          ;; Remember BEFORE enqueueing: a lost response must not be
                          ;; able to inject the same recovered utterance twice.
                          (swap! bridge3-dictate-seen dictate-remember dedup-id full)
                          (swap! channel-state enqueue :messages seat
                                 {:from "gene" :text full :ts (now-iso)})
                          (bridge3-append-turn! seat {:role "you" :text full :ts (now-iso)})
                          ;; ...and durably, BEFORE the 200 and before the claim is
                          ;; released (sol round-2 finding 2). A marker still in
                          ;; flight is a marker that is not yet an answer: the next
                          ;; tap would find an empty ledger on a cold instance, no
                          ;; marker, and no claim, and inject this utterance twice.
                          (capture-archive/mark-recovered!
                            seat id {:dedup-id dedup-id :transcript full :chars (count full)} true)
                          (log/info :capture-recovered :id id :seat seat :chars (count full)
                                    :bytes (alength bytes))
                          (json-response {:ok true :id id :dedup_id dedup-id :dedup false
                                          :transcript full :chars (count full)
                                          :bytes (alength bytes)}))))
                    (catch Exception e
                      (let [{:keys [status body client-error?]} (ex-data e)
                            client-status (dictate-client-status status client-error?)]
                        (log/error :capture-recover-error :id id :seat seat :err (.getMessage e) :status status)
                        (json-response {:error (.getMessage e) :groq-status status :groq-body body}
                                       client-status)))
                    (finally (.delete named))))
                (json-response {:error "capture not found" :id id :seat seat} 404)))
            ;; Released whether we injected or failed: a failed recovery must stay
            ;; retryable, and a successful one is now guarded by ledger + marker.
            (finally (release-recovery! dedup-id))))))))
