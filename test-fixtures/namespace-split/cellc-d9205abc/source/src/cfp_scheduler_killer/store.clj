(ns cfp-scheduler-killer.store
  "Append-only, event-sourced store. THE LOG IS THE DATABASE.

   Every mutation appends one JSON line to data/store/events.jsonl and folds
   that same event into an in-memory atom. Nothing is ever updated in place and
   nothing is ever deleted: 'remove' is an event too. Current state is DERIVED
   by replaying the log from the beginning, so the full history — what changed,
   when, by whom — is always recoverable.

   Lineage: hl7_recode/store.py (arkana-cfo) — append event JSONL, derive state
   by folding, history always recoverable — plus the typed {at, type, actor,
   payload} event shape we were already writing to events_log.

   WHY JSONL BY DEFAULT: zero setup. `make server-dev` runs with no database
   installed, no container, no connection string. A judge clones the repo and it
   works.

   POSTGRES LIVES BEHIND THIS SAME BOUNDARY (bd 3s1). Set STORE_BACKEND=postgres
   and exactly two functions change — `append-line!` and `read-events` — because
   the abstraction is the same either way: an ORDERED SEQUENCE OF LINES. A file
   of lines, or a table of lines. Everything above the seam (fold, replay,
   time-travel, the security tests that assert the log only grows) is untouched
   and cannot tell the difference. See cfp-scheduler-killer.store-pg.

   Two rules that keep this honest:
     1. The projection (`state`) is derived and disposable. Never write to the
        atom except through `fold-event`. Delete the atom, reload the file, get
        the identical state.
     2. Payloads are COMPLETE. The event carries every field needed to rebuild
        the row, because there is no other table to join against later."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [taoensso.timbre :as log])
  (:import (java.time Instant LocalDate)
           (java.time.format DateTimeFormatter)))

;; --- Paths ------------------------------------------------------------------

(def default-store-path "data/store/events.jsonl")

(defonce ^{:doc "Where the log lives. Rebound by tests to a temp file."}
  store-path (atom default-store-path))

;; --- The backend seam -------------------------------------------------------
;;
;; :jsonl (default) or :postgres. This is the ENTIRE switch: two functions read
;; it (`append-line!`, `read-events`) and one derives the change mark from it.
;;
;; store-pg is loaded LAZILY via requiring-resolve, so the default path never
;; touches JDBC — a judge with no database still boots, and the test suite never
;; opens a pool.

(defonce ^{:doc "Which log we are talking to. Read from STORE_BACKEND at load
                 time; rebindable at the REPL with `set-backend!`."}
  backend
  (atom (if (= "postgres" (some-> (System/getenv "STORE_BACKEND")
                                  str/trim str/lower-case))
          :postgres
          :jsonl)))

(defn postgres? [] (= :postgres @backend))

(defn set-backend!
  "Switch backends. Does NOT reload state — call `load!` afterwards."
  [k]
  {:pre [(#{:jsonl :postgres} k)]}
  (reset! backend k))

(defn- pg
  "Resolve a store-pg fn lazily. Keeps JDBC off the default path entirely."
  [sym]
  (requiring-resolve (symbol "cfp-scheduler-killer.store-pg" (name sym))))

;; --- The derived projection -------------------------------------------------

(def empty-state
  "Every collection is a map keyed by its natural id. Events are keyed by SLUG
   (the natural key everything user-facing uses); the rest by uuid string."
  {:events {}        ; slug   -> event map
   :committees {}    ; id     -> committee map
   :forms {}         ; id     -> form map
   :people {}        ; id     -> person map
   :memberships {}   ; id     -> membership map
   :submissions {}   ; id     -> submission map
   :ratings {}       ; [submission-id person-id] -> rating map (UPSERT: latest wins)
   :tasks {}         ; [submission-id task-key] -> speaker task (UPSERT)
   :rooms {}         ; id -> room
   :slots {}         ; submission-id -> placement (UPSERT: one placement per talk)
   :blocks {}        ; id -> non-session block (Lunch, Keynote TBD…)
   :comments {}      ; id     -> comment map (accumulate)
   :log []})         ; every event, in order — the story, kept for the UI

(defonce state (atom empty-state))

(def ^{:dynamic true
       :doc "When bound, EVERY read sees this projection instead of the live
             one. This is how time-travel works: bind a state folded from a
             prefix of the log and the entire app — board, exports, schedule —
             renders that moment with no code changes anywhere else."}
  *as-of-state* nil)

(def ^{:dynamic true
       :doc "The MOMENT *as-of-state* represents (ISO instant string), bound
             alongside it. Views that render 'now' — days left, the you-are-
             here dot — use this so the whole frame rewinds, not just the
             data (Gene, 2026-08-10: scrubbing didn't change days-left)."}
  *as-of-cutoff* nil)

(defn now-inst
  "The rendering 'now': the as-of cutoff during time travel, else the clock."
  ^java.time.Instant []
  (if *as-of-cutoff*
    (java.time.Instant/parse (str *as-of-cutoff*))
    (java.time.Instant/now)))

;; --- Wire format helpers ----------------------------------------------------
;;
;; JSONL is the wire; the atom holds domain values. Dates cross that boundary as
;; ISO-8601 strings, converted OUT when building a payload and back IN when
;; folding, so a reloaded state is indistinguishable from a live one.

(def ^:dynamic *clock*
  "Operator seam (2026-08-10, demo-time staging): when bound to an Instant or
   ISO string, now-iso returns it — so fabricated demo facts can carry the
   moment they pretend to have happened while still walking through the
   ordinary domain verbs. Production code never binds this."
  nil)

(defn now-iso [] (if *clock* (str *clock*) (.toString (Instant/now))))

(defn ->iso-date
  "LocalDate (or an ISO string, or nil) -> \"yyyy-MM-dd\"."
  [d]
  (cond
    (nil? d) nil
    (instance? LocalDate d) (.format ^LocalDate d DateTimeFormatter/ISO_LOCAL_DATE)
    (string? d) (not-empty d)
    :else (str d)))

(defn <-iso-date
  "\"yyyy-MM-dd\" -> LocalDate. Tolerates an already-parsed LocalDate."
  [s]
  (cond
    (nil? s) nil
    (instance? LocalDate s) s
    (str/blank? (str s)) nil
    :else (try (LocalDate/parse (str s))
               (catch Exception _
                 (log/warn :bad-date-in-log :value s) nil))))

(defn ->iso-instant
  "Instant (or an ISO string, or nil) -> ISO-8601 string."
  [t]
  (cond
    (nil? t) nil
    (instance? Instant t) (.toString ^Instant t)
    (string? t) (not-empty t)
    :else (str t)))

(defn <-iso-instant
  [s]
  (cond
    (nil? s) nil
    (instance? Instant s) s
    (str/blank? (str s)) nil
    :else (try (Instant/parse (str s))
               (catch Exception _
                 (log/warn :bad-instant-in-log :value s) nil))))

(defn new-id
  "A fresh id as a string. Ids live in the log as strings — JSON has no uuid."
  []
  (str (random-uuid)))

;; --- Fold: events -> state --------------------------------------------------
;;
;; One pure function per event type. These are the heart of the system and take
;; no I/O, so they unit-test as plain data in -> data out.

(defn- event-by-id
  "Find an event map by its id (the log references events by id, humans by slug)."
  [state event-id]
  (some (fn [e] (when (= event-id (:id e)) e)) (vals (:events state))))

(defn- update-present
  "Like `update`, but ONLY when the key is already there.

   This is load-bearing. `event.updated` carries a PARTIAL map (just the fields
   that changed), and plain `update` on a missing key inserts nil — so merging a
   parsed partial silently blanked every date the edit didn't mention. Changing
   the CFP open date erased the event's start date, end date and close date.
   The projection was wrong; the LOG still had the truth, so fixing this
   function and re-folding restored the data with nothing to migrate."
  [m k f]
  (if (contains? m k) (update m k f) m))

(defn- parse-event-row
  "ISO strings -> domain time values, for a full record OR a partial change map."
  [p]
  (-> p
      (update-present :starts-on <-iso-date)
      (update-present :ends-on <-iso-date)
      (update-present :cfp-opens-at <-iso-instant)
      (update-present :cfp-closes-at <-iso-instant)
      (update-present :created-at <-iso-instant)))

(defmulti fold-event
  "Apply one event to the state. Unknown types are ignored (forward
   compatibility: an older build must survive a log written by a newer one)."
  (fn [_state event] (:type event)))

(defmethod fold-event :default [state event]
  (log/debug :unknown-event-type :type (:type event))
  state)

(defmethod fold-event "event.created" [state {:keys [payload]}]
  (assoc-in state [:events (:slug payload)] (parse-event-row payload)))

(defmethod fold-event "event.updated" [state {:keys [payload]}]
  (let [{:keys [slug changes]} payload]
    (if (get-in state [:events slug])
      (update-in state [:events slug] merge (parse-event-row changes))
      state)))

;; Archiving is a FACT, never a deletion (Gene, 2026-08-10): the event and its
;; whole history stay in the log; listings just stop leading with it.
(defmethod fold-event "event.archived" [state {:keys [payload]}]
  (let [{:keys [slug archived-at]} payload]
    (if (get-in state [:events slug])
      (assoc-in state [:events slug :archived-at] (<-iso-instant archived-at))
      state)))

(defmethod fold-event "event.unarchived" [state {:keys [payload]}]
  (let [{:keys [slug]} payload]
    (if (get-in state [:events slug])
      (assoc-in state [:events slug :archived-at] nil)
      state)))

(defmethod fold-event "committee.created" [state {:keys [payload]}]
  (assoc-in state [:committees (:id payload)]
            (update-present payload :created-at <-iso-instant)))

(defmethod fold-event "form.installed" [state {:keys [payload]}]
  (assoc-in state [:forms (:id payload)]
            (update-present payload :created-at <-iso-instant)))

;; The form builder's ONE write. The payload carries the COMPLETE new field
;; vector rather than a per-key delta, for two reasons: payloads must stand
;; alone (there is no other table to join against later), and REORDER is a
;; statement about the whole vector — there is no honest delta for "these
;; twelve questions are now in this order". Re-folding the log therefore
;; reproduces the edited form exactly, which is the property the whole append-
;; only design exists to buy.
(defmethod fold-event "form.updated" [state {:keys [payload]}]
  (if (get-in state [:forms (:id payload)])
    (-> state
        (assoc-in [:forms (:id payload) :fields] (:fields payload))
        (assoc-in [:forms (:id payload) :edited-at] (<-iso-instant (:at payload))))
    state))

;; "The organizer has read this form" — a deliberate second act, like informing
;; a speaker. The dashboard checklist ticks on this fact, never on a page view.
(defmethod fold-event "form.reviewed" [state {:keys [payload]}]
  (if (get-in state [:forms (:id payload)])
    (assoc-in state [:forms (:id payload) :reviewed-at] (<-iso-instant (:at payload)))
    state))

(defmethod fold-event "person.created" [state {:keys [payload]}]
  (assoc-in state [:people (:id payload)]
            (update-present payload :created-at <-iso-instant)))

(defmethod fold-event "member.added" [state {:keys [payload]}]
  ;; The role vocabulary changed on 2026-08-09 ("member" -> "reviewer") and the
  ;; log is append-only, so every event written before then still says "member".
  ;; It is translated HERE, on the way into the projection, which is the whole
  ;; trick: the historical events stay untouched and correct, and everything
  ;; downstream sees one vocabulary. (committees/normalize-role is the same rule;
  ;; it can't be called from here — committees requires store, not the reverse.)
  (assoc-in state [:memberships (:id payload)]
            (-> payload
                (update-present :created-at <-iso-instant)
                (update :role #(if (= "member" %) "reviewer" %)))))

(defmethod fold-event "member.removed" [state {:keys [payload]}]
  (update state :memberships dissoc (:id payload)))

(defmethod fold-event "submission.created" [state {:keys [payload]}]
  (assoc-in state [:submissions (:id payload)]
            (update-present payload :created-at <-iso-instant)))

;; A rating is UPSERT-by-(submission × person): the projection keeps only the
;; latest opinion, because a reviewer has exactly one current view of a talk.
;; The LOG keeps every version — changing your mind from 2 to 4 after reading
;; the comments is exactly the history worth having.
(defmethod fold-event "rating.set" [state {:keys [payload]}]
  (assoc-in state [:ratings [(:submission-id payload) (:person-id payload)]]
            (update-present payload :at <-iso-instant)))

;; Comments accumulate — each one is its own utterance, keyed by its own id.
(defmethod fold-event "comment.added" [state {:keys [payload]}]
  (assoc-in state [:comments (:id payload)]
            (update-present payload :at <-iso-instant)))

(defmethod fold-event "submission.status-changed" [state {:keys [payload]}]
  (if (get-in state [:submissions (:submission-id payload)])
    (assoc-in state [:submissions (:submission-id payload) :status] (:to payload))
    state))

(defmethod fold-event "room.added" [state {:keys [payload]}]
  (assoc-in state [:rooms (:id payload)] (update-present payload :at <-iso-instant)))

(defmethod fold-event "room.renamed" [state {:keys [payload]}]
  (if (get-in state [:rooms (:id payload)])
    (assoc-in state [:rooms (:id payload) :name] (:name payload))
    state))

(defmethod fold-event "room.removed" [state {:keys [payload]}]
  ;; Removing a room does NOT unplace its sessions — they fall back to
  ;; "unroomed", which is a first-class state here. Silently dropping a
  ;; placement because a room was renamed out of existence would lose work.
  (update state :rooms dissoc (:id payload)))

;; A placement is UPSERT-by-submission: a talk is in exactly one place at a
;; time, and moving it is an edit of that one fact. The log keeps every move,
;; which is what makes the .ics SEQUENCE bump correct.
(defmethod fold-event "slot.assigned" [state {:keys [payload]}]
  (assoc-in state [:slots (:submission-id payload)]
            (update-present payload :at <-iso-instant)))

(defmethod fold-event "slot.cleared" [state {:keys [payload]}]
  (update state :slots dissoc (:submission-id payload)))

(defmethod fold-event "block.added" [state {:keys [payload]}]
  (assoc-in state [:blocks (:id payload)] (update-present payload :at <-iso-instant)))

(defmethod fold-event "block.removed" [state {:keys [payload]}]
  (update state :blocks dissoc (:id payload)))

;; The lock lives on the event's settings so it travels with the event and
;; shows up in every projection that reads settings.
(defmethod fold-event "schedule.locked" [state {:keys [payload]}]
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :schedule-lock]
              {:locked? true
               :version-label (:version-label payload)
               :at (:at payload)})
    state))

(defmethod fold-event "schedule.unlocked" [state {:keys [payload]}]
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :schedule-lock]
              {:locked? false
               :version-label (:version-label payload)
               :at (:at payload)})
    state))

;; Comms events are a LOG, not a projection — nothing folds into state. The
;; send history is read straight from the log, which is the honest shape: "we
;; attempted this, at this time, with this result" is a sequence of facts, not
;; a current value.
(defmethod fold-event "comms.rendered" [state _] state)
(defmethod fold-event "comms.sent" [state _] state)
(defmethod fold-event "comms.failed" [state _] state)

(defmethod fold-event "replay.marked" [state {:keys [payload]}]
  ;; Marks an event as a REPLAY TARGET. The simulator refuses to play into
  ;; anything without this, so a demo can never pollute a real conference.
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :replay?] true)
    state))

(defmethod fold-event "sink.registered" [state {:keys [payload]}]
  ;; Webhooks live in the event's own settings, so they travel with the event
  ;; and are visible in the log like everything else.
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :webhooks (:id payload)]
              (select-keys payload [:id :url :types :created-at]))
    state))

(defmethod fold-event "sink.removed" [state {:keys [payload]}]
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (update-in state [:events slug :settings :webhooks] dissoc (:id payload))
    state))

;; API keys live in the event's settings next to the webhooks, for the same
;; reason: they travel with the conference and every grant shows up in the log.
;; The key MATERIAL is in the payload — this is a single-tenant organizer tool,
;; and a key you cannot read back is a key the organizer will re-issue forever.
(defmethod fold-event "api-key.created" [state {:keys [payload]}]
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :api-keys (:id payload)]
              (select-keys payload [:id :label :key :created-at :created-by]))
    state))

;; Revoking DROPS the key from the projection: the question a request asks is
;; "is this key live", and a live/dead flag is a second thing to get wrong. The
;; log still says it existed and when it was revoked, which is the record that
;; matters after a leak.
(defmethod fold-event "api-key.revoked" [state {:keys [payload]}]
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (update-in state [:events slug :settings :api-keys] dissoc (:id payload))
    state))

(defmethod fold-event "person.profile-updated" [state {:keys [payload]}]
  ;; A profile is the person's OWN description of themselves, so it merges the
  ;; keys they sent and leaves the rest alone — never a wholesale replace.
  (if (get-in state [:people (:person-id payload)])
    (update-in state [:people (:person-id payload) :profile] merge (:changes payload))
    state))

(defmethod fold-event "submission.answers-updated" [state {:keys [payload]}]
  ;; Speakers may edit after acceptance (swyx: no lock). The form SNAPSHOT is
  ;; deliberately NOT touched — the questions they answered are history; only
  ;; the answers move.
  (if (get-in state [:submissions (:submission-id payload)])
    (update-in state [:submissions (:submission-id payload) :answers]
               merge (:changes payload))
    state))

(defmethod fold-event "submission.notified" [state {:keys [payload]}]
  ;; The Inform gate: until this exists, a speaker sees "Under review" no matter
  ;; what the committee decided (Sessionize's own pattern, adopted with pride).
  (if (get-in state [:submissions (:submission-id payload)])
    (-> state
        (assoc-in [:submissions (:submission-id payload) :notified-at]
                  (<-iso-instant (:at payload)))
        (assoc-in [:submissions (:submission-id payload) :notified-status]
                  (:status-at-notify payload)))
    state))

(defmethod fold-event "task.installed" [state {:keys [payload]}]
  (assoc-in state [:tasks [(:submission-id payload) (:key payload)]]
            (update-present payload :at <-iso-instant)))

(defmethod fold-event "task.completed" [state {:keys [payload]}]
  ;; UPSERT onto the installed task — a speaker who pastes a new slides URL is
  ;; correcting the same task, not creating a second one.
  (let [k [(:submission-id payload) (:key payload)]]
    (if (get-in state [:tasks k])
      (update-in state [:tasks k] merge
                 {:value (:value payload)
                  :completed-at (<-iso-instant (:at payload))})
      state)))

(defmethod fold-event "submission.priority-toggled" [state {:keys [payload]}]
  (if (get-in state [:submissions (:submission-id payload)])
    (assoc-in state [:submissions (:submission-id payload) :priority] (boolean (:priority payload)))
    state))

(defn fold-one
  "Fold a single event, and keep it in the :log so the UI can show the story."
  [state event]
  (-> (fold-event state event)
      (update :log conj event)))

(defn fold
  "Replay a sequence of events into state. Pure — this is the whole definition
   of what the data 'is'."
  ([events] (fold empty-state events))
  ([state events] (reduce fold-one state events)))

;; --- Sinks ------------------------------------------------------------------
;;
;; Anything that wants to hear about events registers here: webhooks (Zapier),
;; Slack, and later Airtable / email push (bd 4u2, 8ch). Sinks fire AFTER the
;; state swap, asynchronously, and a failing sink is logged and dropped — a
;; broken webhook must never fail a speaker's submission.

(defonce ^{:doc "Extra sinks registered at runtime (tests, REPL)."}
  runtime-sinks (atom []))

(defn sink-config
  "Configured sinks: resources/sinks.edn if present, plus runtime registrations.
   Shape: [{:type :webhook :url \"...\" :events [\"submission.created\"]}
           {:type :slack :webhook-url \"...\" :events [...]}]"
  []
  (concat
   (when-let [res (io/resource "sinks.edn")]
     (try (read-string (slurp res))
          (catch Exception e
            (log/error :sinks-edn-unreadable :msg (.getMessage e))
            nil)))
   @runtime-sinks))

(defn- wants-event?
  "A sink with no :events hears everything; otherwise only its listed types."
  [sink event]
  (let [types (:events sink)]
    (or (empty? types) (contains? (set types) (:type event)))))

(defmulti deliver-sink!
  "Deliver one event to one sink. Add a method to add an integration."
  (fn [sink _event] (:type sink)))

(defmethod deliver-sink! :default [sink event]
  (log/warn :unknown-sink-type :sink-type (:type sink) :event-type (:type event)))

(defmethod deliver-sink! :webhook [{:keys [url]} event]
  ;; Generic JSON POST of the whole event — Zapier/Make/n8n ready.
  (let [post (requiring-resolve 'hato.client/post)]
    (post url {:headers {"content-type" "application/json"}
               :body (json/write-str event)
               :timeout 5000})))

(defn slack-text
  "Human sentence for a Slack incoming webhook. Kept pure + public so it can be
   tested without a network."
  [{:keys [type payload]}]
  (case type
    "submission.created"
    (let [sp (first (:speakers payload))]
      (str "New submission: " (or (get-in payload [:answers :talk-title]) "(untitled)")
           " from " (or (:name sp) "unknown speaker")
           (when-let [org (:org sp)] (str " (" org ")"))))
    "event.created" (str "New event: " (:name payload))
    "member.added"  (str "Committee member added: " (:email payload))
    (str "Event: " type)))

;; The :slack METHOD deliberately lives in `sinks`, not here — a Slack message
;; worth reading names the talk, the speaker, the new average and links to the
;; row, and every one of those needs submissions/ratings/people, which `store`
;; must never depend on. `slack-text` above stays as the plain one-line
;; fallback (Slack's notification preview, and the message for event types the
;; rich builder has no opinion about). There is exactly ONE defmethod for
;; :slack in the codebase; two would make delivery depend on load order.

(defmethod deliver-sink! :test [{:keys [captured]} event]
  ;; For tests: an atom that records what it was handed.
  (swap! captured conj event))

(defonce ^{:doc "Recent delivery attempts, newest first. IN MEMORY ONLY — a
                 restart forgets them. The Settings page says so out loud rather
                 than implying a durable delivery record we do not keep."}
  deliveries (atom []))

(def max-deliveries 50)

(defn- record-delivery! [entry]
  (swap! deliveries #(vec (take max-deliveries (cons entry %)))))

(def ^{:dynamic true
       :doc "Extra sinks every event gets by default, as (fn [event] [sinks]).
             Set by cfp-scheduler-killer.sinks at load time so `store` never has
             to know what a PC push email is."}
  *default-sinks-fn* nil)

(defn event-sinks
  "Sinks attached to ONE event: its registered webhooks, its Airtable base if
   configured, plus the defaults (PC push, durability snapshot).

   Reads the atom DIRECTLY rather than through `snapshot`, because this runs
   inside the write path where the state is already current — re-entering the
   self-healing reload from there would be a recursion waiting to happen."
  [event-id]
  (when event-id
    (let [event (event-by-id @state event-id)
          settings (:settings event)]
      (vec
       (concat
        (map (fn [w] {:type :webhook :url (:url w)
                      :events (:types w) :sink-id (:id w)})
             (vals (:webhooks settings)))
        (when-let [a (:airtable settings)]
          [(assoc a :type :airtable)])
        ;; Slack carries its OWN :events list, chosen on the Settings page, so
        ;; `wants-event?` filters it exactly like a webhook's types.
        (when-let [s (:slack settings)]
          [(assoc s :type :slack)])
        (when *default-sinks-fn* (*default-sinks-fn* event)))))))

;; Kept as an alias: the Settings page and its tests speak of webhooks.
(defn event-webhooks [event-id]
  (filterv #(= :webhook (:type %)) (event-sinks event-id)))

(defonce ^{:doc "In-flight sink deliveries. Sinks are fire-and-forget in
                 production, but a delivery that OUTLIVES its context is a
                 hazard: some sinks (the PC push) append to the store, and a
                 straggler from one test would land in the next test's log.
                 Tracking them lets `await-sinks!` drain deterministically."}
  in-flight (atom #{}))

(defn await-sinks!
  "Block until every in-flight sink delivery has finished. Tests only —
   production never waits on the fan-out, that is the whole point."
  []
  (doseq [f (vec @in-flight)]
    (try (deref f 5000 nil) (catch Exception _ nil)))
  (reset! in-flight #{})
  nil)

(defn- track-future
  [f]
  (swap! in-flight conj f)
  f)

(defn fire-sinks!
  "Notify every interested sink, asynchronously. Never throws, never blocks.

   Sinks come from three places: resources/sinks.edn (deployment), runtime
   registration (tests/REPL) and the EVENT's own registered sinks."
  [event]
  (doseq [sink (concat (sink-config) (event-sinks (:event-id event)))]
    (when (wants-event? sink event)
      (track-future
       (future
         (let [started (System/currentTimeMillis)]
           (try
             (deliver-sink! sink event)
             (record-delivery! {:at (now-iso) :sink-type (:type sink) :url (:url sink)
                                :event-type (:type event) :ok true
                                :ms (- (System/currentTimeMillis) started)})
             (catch Exception e
               (record-delivery! {:at (now-iso) :sink-type (:type sink) :url (:url sink)
                                  :event-type (:type event) :ok false
                                  :error (.getMessage e)
                                  :ms (- (System/currentTimeMillis) started)})
               (log/warn :sink-delivery-failed :sink-type (:type sink)
                         :event-type (:type event) :msg (.getMessage e))))))))))

;; --- Append -----------------------------------------------------------------

(def ^:private write-lock (Object.))

(defonce ^{:doc "[length last-modified] of the log as this process last saw it.
                 Lets us notice writes made by ANOTHER process (`make seed-demo`
                 while the dev server is up) and re-fold instead of serving
                 stale state."}
  file-mark (atom nil))

(defonce ^{:private true
           :doc "Postgres mode's local change mark: the highest seq this
                 process has written or loaded."}
  pg-mark (atom 0))

(defonce ^{:private true
           :doc "Throttle state for the external-writer probe: {:at ms :seq n}."}
  pg-external-check (atom {:at 0 :seq 0}))

(def ^:private pg-check-interval-ms
  "How stale one writer may be about the OTHER writer's appends."
  2000)

(defn- pg-observed-seq
  "The honest Postgres mark (2026-08-10, the one-database ruling: laptop dev
   and Cloud Run share the production database). The local high-water alone
   goes silently stale against a SECOND writer, so at most once per interval
   this asks the database for max(seq) — one indexed SELECT per ~2s per
   process, not per read. A foreign append moves the mark, refresh-if-changed!
   re-folds, and both writers converge within the interval."
  []
  (let [now (System/currentTimeMillis)
        {at :at s :seq} @pg-external-check]
    (if (< (- now at) pg-check-interval-ms)
      (max @pg-mark s)
      (let [db-seq (or ((pg 'max-seq)) 0)]
        (reset! pg-external-check {:at now :seq db-seq})
        (max @pg-mark db-seq)))))

(defn- current-mark [path]
  (if (postgres?)
    [(pg-observed-seq) :pg]
    (let [f (io/file path)]
      (when (.exists f) [(.length f) (.lastModified f)]))))

(defn- append-line!
  "Append one already-encoded JSON line to whichever log is configured.

   JSONL: opening in append mode and closing flushes to the OS, which is the
   durability we need here (a lost trailing line on a hard crash is recoverable
   from the UI; a corrupted file is not, so we never rewrite).

   Postgres: one INSERT. Durability is the database's problem, which is the
   entire reason the backend exists — Cloud Run's disk is ephemeral."
  [path line]
  (if (postgres?)
    (do (reset! pg-mark ((pg 'append-line!) line))
        (reset! file-mark (current-mark path)))
    (do
      (io/make-parents (io/file path))
      (with-open [w (io/writer path :append true)]
        (.write w ^String line)
        (.write w "\n")
        (.flush w))
      (reset! file-mark (current-mark path)))))

(defn canonicalize
  "Round-trip an event through JSON and back.

   This is the invariant that makes 'the log IS the state' literally true: we
   fold EXACTLY what we wrote, so the live projection is byte-for-byte the one a
   restart would rebuild. Without it the two drift — a form snapshot written
   in-process kept Clojure keywords (:talk-title) while the reloaded one held
   strings (\"talk-title\"), so code comparing them worked until the first
   restart. Paying one encode/decode per append buys that whole class of bug
   away, and we were encoding anyway to write the line."
  [line]
  (json/read-str line :key-fn keyword))

(defn append!
  "THE write path. Appends `event` to the log, folds it into `state`, then fires
   sinks. Returns the event (with :at/:actor defaulted).

   Serialized on a lock so the file order and the atom order can never diverge."
  ([event] (append! state event))
  ([state-atom event]
   ;; A write from inside a time-travel render would be a write into the past,
   ;; which is meaningless — and would silently land in the present. Refuse.
   (when *as-of-state*
     (throw (ex-info "Cannot write while viewing the past."
                     {:type :read-only-as-of})))
   (let [event (merge {:at (now-iso) :actor "system"} event)
         line (json/write-str event)
         canonical (canonicalize line)]
     (locking write-lock
       (append-line! @store-path line)
       ;; Fold the CANONICAL form, never the in-process one — see `canonicalize`.
       (swap! state-atom fold-one canonical))
     (fire-sinks! canonical)
     canonical)))

(defn append-all!
  "Append several events as one unit — used when a single user action produces
   more than one fact (creating an event also creates its committee and form).

   NOTE: this is atomic with respect to OTHER writers (they wait on the lock),
   but it is not a transaction: a crash mid-batch leaves a prefix on disk. That
   prefix is still valid history, which is why every event must stand alone."
  ([events] (append-all! state events))
  ([state-atom events]
   (when *as-of-state*
     (throw (ex-info "Cannot write while viewing the past."
                     {:type :read-only-as-of})))
   (let [events (mapv #(merge {:at (now-iso) :actor "system"} %) events)
         lines (mapv json/write-str events)
         canonical (mapv canonicalize lines)]
     (locking write-lock
       (doseq [l lines] (append-line! @store-path l))
       (swap! state-atom #(reduce fold-one % canonical)))
     (doseq [e canonical] (fire-sinks! e))
     canonical)))

;; --- Load -------------------------------------------------------------------

(defn read-events-from-file
  "Read a JSONL log FILE. Always the file, whatever the configured backend —
   the GCS snapshot restore reads a specific file on purpose."
  [path]
  (let [f (io/file path)]
    (if-not (.exists f)
      []
      (with-open [r (io/reader f)]
        (into []
              (keep (fn [line]
                      (when-not (str/blank? line)
                        (try (json/read-str line :key-fn keyword)
                             (catch Exception e
                               (log/error :unparseable-log-line
                                          :msg (.getMessage e)
                                          :line (subs line 0 (min 120 (count line))))
                               nil)))))
              (line-seq r))))))

(defn read-events
  "Read the log. A malformed line is reported and skipped rather than taking the
   whole app down — one bad line must not cost you the other 10,000.

   In Postgres mode the `path` argument is IGNORED: there is one table, and the
   callers that pass a path (`load!`, `state-as-of`) mean 'the log', not 'that
   file'. Callers that genuinely mean a FILE call `read-events-from-file`."
  ([] (read-events @store-path))
  ([path]
   (if (postgres?)
     ((pg 'read-events))
     (read-events-from-file path))))

(defn load!
  "Boot: replay the whole log into `state`. Idempotent — call it as often as
   you like, you always land on the same state."
  ([] (load! state @store-path))
  ([state-atom path]
   ;; Takes the WRITE LOCK. A reload reads the file, folds it, and resets the
   ;; atom; an append that lands between the read and the reset would be
   ;; silently discarded — a lost update. Nothing appended concurrently until
   ;; sinks started writing to the store, which is exactly when this began
   ;; failing intermittently.
   (locking write-lock
     (let [events (read-events path)
           folded (fold events)]
       (reset! state-atom folded)
       ;; Postgres: the mark is the highest seq in the TABLE, read once here.
       ;; Everywhere else it is derived from this process's own appends.
       (when (postgres?) (reset! pg-mark ((pg 'max-seq))))
       (reset! file-mark (current-mark path))
       (log/info :store-loaded :backend @backend :path path :events (count events)
                 :events-count (count (:events folded))
                 :people (count (:people folded)))
       folded))))

(defn refresh-if-changed!
  "Catch up with appends made by another writer (the second process, Cloud Run).

   JSONL: any change → full reload (a file can be rewritten by hand, so the
   only safe response is replay-from-genesis).

   Postgres: the log is append-only and the projection is a left fold, so
   catching up = fetch ONLY the rows past our high-water mark and fold them
   onto the state we already hold — never re-read the whole table. (The
   15-second page loads of 2026-08-10: at ~7,000 events, every refresh was
   refetching the entire table over the WAN while a review batch moved the
   mark every few seconds.) Correct by construction: fold is a left fold and
   local appends serialize on the same write-lock, so the tail can only
   contain FOREIGN events. Full reload remains the fallback whenever the mark
   is missing or max(seq) moved backwards — someone changed history, replay it."
  []
  (let [m (current-mark @store-path)]
    (when (not= m @file-mark)
      (if (postgres?)
        (let [db-seq (first m)
              mark (or @pg-mark 0)]
          (if (and (pos? mark) (>= db-seq mark))
            (locking write-lock
              ;; re-read the mark under the lock: a local append may have
              ;; moved it between the check above and here
              (let [mark @pg-mark
                    {:keys [events max-seq]} ((pg 'read-events-since) mark)]
                (when (seq events)
                  (swap! state fold events)
                  (log/info :store-tail-folded :from mark :events (count events)))
                (swap! pg-mark max max-seq)
                (reset! file-mark (current-mark @store-path))))
            (do (log/info :store-changed-on-disk :path @store-path)
                (load!))))
        (do (log/info :store-changed-on-disk :path @store-path)
            (load!))))
    nil))

(defn reset-for-test!
  "Point the store at a scratch file and clear the atom. Tests only.

   FORCES the JSONL backend: the whole suite is temp-file based, and a stray
   STORE_BACKEND=postgres in the environment must never silently point 1,900
   assertions at the real cloud database. The Postgres tests set the backend
   themselves, deliberately."
  [path]
  (set-backend! :jsonl)
  (reset! store-path path)
  (reset! runtime-sinks [])
  (reset! state empty-state)
  (reset! file-mark nil)
  nil)

;; --- Read helpers -----------------------------------------------------------

(defn snapshot
  "The current projection. EVERY read goes through here, and it first checks
   whether the log changed underneath us — which means reads are self-healing:
   the state is correct even if nobody remembered to call `load!` at boot, and
   correct again after another process appends. Costs one File.length()."
  []
  (or *as-of-state*
      (do (refresh-if-changed!)
          @state)))

(defonce ^:private as-of-cache
  ;; {[file-mark cutoff] -> state}. Live scrubbing re-folds on every tick, and
  ;; a drag across a 300-event log is a few hundred folds. Keyed on the file
  ;; mark so ANY append invalidates the whole cache — a stale past would be
  ;; worse than a slow one.
  (atom {}))

(def ^:private as-of-cache-limit 64)

(defn state-as-of
  "A THROWAWAY projection of the world as of `cutoff` (an ISO instant string).

   Pure: a fold over the prefix of the log at or before that moment. It does not
   touch the live atom, so time-travelling costs nothing and risks nothing —
   which is the whole dividend of having stored events instead of rows."
  ([cutoff]
   (let [k [@file-mark (str cutoff)]]
     (if-let [hit (get @as-of-cache k)]
       hit
       ;; Fold from the IN-MEMORY log — the state atom already holds every
       ;; event in :log, so a scrub tick must never touch the database. (At
       ;; 7k events, every cache miss was a full-table WAN read; a few scrub
       ;; ticks in flight exhausted the 5-connection pool — 2026-08-10.)
       (let [mem-log (:log @state)
             folded (state-as-of cutoff (if (seq mem-log) mem-log (read-events)))]
         (swap! as-of-cache
                (fn [c]
                  ;; Drop everything on a new file mark, and cap the rest.
                  (let [c (into {} (filter #(= @file-mark (first (key %)))) c)]
                    (assoc (if (> (count c) as-of-cache-limit) {} c) k folded))))
         folded))))
  ([cutoff events]
   (fold (filterv #(<= (compare (str (:at %)) (str cutoff)) 0) events))))

(defn log-bounds
  "[first-at last-at] over the whole log — the ends of the time-travel slider."
  []
  (let [ats (keep :at (:log (snapshot)))]
    (when (seq ats) [(first ats) (last ats)])))

(defn get-event-by-slug [slug] (get-in (snapshot) [:events slug]))
(defn get-event-by-id [event-id] (event-by-id (snapshot) event-id))
(defn all-events [] (vals (:events (snapshot))))

(defn committees-for-event [event-id]
  (->> (vals (:committees (snapshot)))
       (filter #(= event-id (:event-id %)))
       (sort-by :created-at)))

(defn forms-for-event [event-id]
  (->> (vals (:forms (snapshot)))
       (filter #(= event-id (:event-id %)))
       (sort-by :created-at)))

(defn memberships-for-committee [committee-id]
  (->> (vals (:memberships (snapshot)))
       (filter #(= committee-id (:committee-id %)))
       (sort-by :created-at)))

(defn person-by-email [email]
  (some (fn [p] (when (= email (:email p)) p)) (vals (:people (snapshot)))))

(defn person-by-id [person-id] (get-in (snapshot) [:people person-id]))

(defn submissions-for-event [event-id]
  (->> (vals (:submissions (snapshot)))
       (filter #(= event-id (:event-id %)))
       (sort-by :created-at)))

(defn submission-by-id [submission-id]
  (get-in (snapshot) [:submissions submission-id]))

(defn ratings-for-submission [submission-id]
  (->> (vals (:ratings (snapshot)))
       (filter #(= submission-id (:submission-id %)))
       (sort-by :at)))

(defn ratings-by-person [person-id]
  (->> (vals (:ratings (snapshot)))
       (filter #(= person-id (:person-id %)))
       (sort-by :at)))

(defn rating-by [submission-id person-id]
  (get-in (snapshot) [:ratings [submission-id person-id]]))

(defn rooms-for-event [event-id]
  (->> (vals (:rooms (snapshot)))
       (filter #(= event-id (:event-id %)))
       (sort-by (juxt :order :name))
       vec))

(defn slot-for [submission-id]
  (get-in (snapshot) [:slots submission-id]))

(defn slots-for-event [event-id]
  (->> (vals (:slots (snapshot)))
       (filter #(= event-id (:event-id %)))
       vec))

(defn blocks-for-event [event-id]
  (->> (vals (:blocks (snapshot)))
       (filter #(= event-id (:event-id %)))
       (sort-by (juxt :day :start))
       vec))

(defn tasks-for-submission
  "Installed speaker tasks, in the order the event declared them."
  [submission-id]
  (->> (vals (:tasks (snapshot)))
       (filter #(= submission-id (:submission-id %)))
       (sort-by :position)))

(defn submissions-for-person
  "Every submission where this person is the owning speaker."
  [person-id]
  (->> (vals (:submissions (snapshot)))
       (filter (fn [s] (some #(= person-id (:person-id %)) (:speakers s))))
       (sort-by :created-at)))

(defn comments-for-submission [submission-id]
  (->> (vals (:comments (snapshot)))
       (filter #(= submission-id (:submission-id %)))
       (sort-by :at)))

(defn comments-by-person [person-id]
  (->> (vals (:comments (snapshot)))
       (filter #(= person-id (:person-id %)))
       (sort-by :at)))

(defn log-for-event
  "The story of one conference, in order.

   An event belongs to a conference if it says so on the ENVELOPE (:event-id) or
   in its payload. The envelope matters for facts that are not themselves scoped
   to a conference but happened because of one — `person.created` is the case
   that taught us this: a person is event-independent (that is the whole point of
   email identity), yet 'Ann was added here, and that is when she first appeared'
   is part of this event's history."
  [event-id]
  (filterv (fn [e]
             (let [p (:payload e)]
               (or (= event-id (:event-id e))
                   (= event-id (:id p))
                   (= event-id (:event-id p)))))
           (:log (snapshot))))

(comment
  (load!)
  (snapshot)
  (all-events)
  (count (:log @state)))
