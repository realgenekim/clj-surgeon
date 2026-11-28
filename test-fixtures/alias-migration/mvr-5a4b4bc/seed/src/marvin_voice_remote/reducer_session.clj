(ns marvin-voice-remote.reducer-session
  "S5a — THE SESSION LOOP. The reducer becomes AUTHORITATIVE for one live
   surface (ARCHITECTURE.md §7 S5a, D11: `/voice-lab` first, iOS second,
   `/bridge4` last).

   S4 ran the reducer in SHADOW: real events went in, decisions came out, and
   nothing was ever executed. This namespace is the same reducer with the wire
   attached at both ends:

     browser --(marvin.voice/1 envelopes)--> POST /api/reducer-session/events
                                              |
                                       protocol/explain  (fail closed)
                                       protocol/fact->event
                                       core/step         (THE decision)
                                              |
     browser <--(effect batches, SSE)------ GET /api/reducer-session/effects

   Three properties are the whole design, and each one is a rule the old
   engines broke:

   1. NO DECISION LIVES HERE. This namespace validates, routes, journals and
      transports. Every product question — is that speech, has the utterance
      ended, does this reply speak now, may the mic reopen — is answered by
      `core/step` under `policy/current`. Grep this file for a threshold and
      you will not find one; the only numbers are transport cadences, named
      as such.

   2. EVERY EFFECT IS EXECUTED BY THE CLIENT. v1 executes nothing server-side
      (D8's durable outbox + runners is Gate G1 work, and G1 ruled 'no new
      database now' on 2026-07-25). The outbox here is an in-memory ring with
      a monotonic cursor, which is exactly enough to survive a reconnect and
      honest about not surviving a restart — a reconnecting client that has
      fallen off the back of the ring is TOLD it lost effects (D5: never
      silently resume).

   3. THE JOURNAL KEEPS RUNNING. Every accepted event is also recorded into
      `reducer.shadow`'s journal, so the /reducer-lab/shadow console gains,
      for the first time, a trace that carries REAL reducer-catalog events and
      REAL `:cmd/*` commands. That closes two S4 coverage gaps by supply
      rather than by argument: `speak-comparison`'s 'the replayed session
      never seeded' gap (no server seam produces a command — this one does)
      and the identity gaps (`:requires-identity`), because the ids come off
      the reducer's own state instead of being inferred.

   WHAT IS DELIBERATELY NOT HERE: no state in the DOM, no policy on the wire,
   no server-side audio. The page is a MediaPort (D4) — its negative space is
   documented at `channel/voice-lab-reducer-page-html`."
  (:require [marvin-voice-remote.json :as mjson]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [marvin-voice-remote.reducer.core :as core]
            [marvin-voice-remote.reducer.policy :as policy]
            [marvin-voice-remote.reducer.protocol :as protocol]
            [marvin-voice-remote.reducer.shadow :as shadow]
            [marvin-voice-remote.sse-registry :as sse]
            [org.httpkit.server :as http]
            [taoensso.timbre :as log])
  (:import [java.util.concurrent.atomic AtomicLong]))

;; --- Transport cadences ---------------------------------------------------
;;
;; THESE ARE NOT POLICY. Every number below is a property of the pipe (how
;; often a dead socket is noticed, how deep a reconnect buffer is), not of the
;; product (what counts as speech, when an utterance ends). D3 rule 1 forbids
;; the SECOND kind in an adapter; the first kind has to live somewhere and
;; this is where. Each one says which failure it bounds.

(def heartbeat-ms
  "SSE keep-alive. Proxies and phone radios drop an idle stream; a comment
   frame every 15 s keeps it open and, more usefully, is how a dead peer is
   discovered (the send fails). Matches the bridge2/bridge3 streams so all
   three behave identically behind the same proxy."
  15000)

(def tick-ms
  "How often the server injects `:fact/clock` into a live session.

   The reducer has no clock — time arrives in events (D2). Two decisions
   therefore CANNOT happen without one: the tail-grace reopen (P7 stores a
   `:cooldown-until` and waits for an event past it) and lease expiry (D7:
   'expiry rides explicit clock events'). Between a closed mic and the next
   energy fact there is no client traffic at all, so a session with no tick
   would simply stop.

   250 ms is chosen against the SMALLEST deadline a tick has to resolve —
   `:tail-grace-ms` (450) — so the mic reopens inside one grace of when it
   became eligible. It is a sampling interval for a deadline the POLICY owns;
   moving the policy number does not require moving this one."
  250)

(def max-batches
  "Effect-outbox depth per session. A ring, not a log (S7: bounded resources).
   200 batches is minutes of real dictation; a client that reconnects further
   behind that is told it lost effects rather than being handed a silent gap."
  200)

(def max-sessions
  "Seat names arrive from a query parameter. Cap how many sessions are held and
   evict the least-recently-touched, so a scanner cannot grow this atom."
  32)

(def idle-session-ms
  "A session with no traffic for this long stops being ticked (and is eligible
   for eviction). Ten minutes: long enough to survive a phone-call interruption,
   short enough that abandoned tabs do not keep a thread busy forever."
  600000)

(def max-seen-events
  "Depth of the per-session `event/id` idempotency ledger (S5c delta-4, server
   half). Matches `core/max-seen-events` (512) on purpose: the reducer already
   keeps a 512-deep FIFO of applied event ids, and a SHALLOWER ledger here would
   admit a redelivery the reducer then no-ops — which is safe for STATE but
   would double-journal it and report a fresh acceptance. Two windows of the
   same depth cannot disagree about what counts as recent."
  512)

(def max-convo
  "Depth of the DISPLAY conversation ledger. A remote's convo log is a glance
   surface, not an archive; 24 turns is more than a phone shows and it is a
   bound rather than a leak (S7)."
  24)

(def max-transitions
  "Depth of the DISPLAY transition tape. Storyboard B shows the last 3; keeping
   8 leaves headroom for a longer tape without making this a log."
  8)

(def max-command-epochs
  "Depth of the per-session RENDER-EPOCH ledger (sol finding 4.2). One entry per
   command accepted at a given rendered surface; 64 is many more taps than can be
   in flight across one morph, and it is a BOUND, not a policy — how long a stale
   tap stays recognisable is a memory question, not a product one."
  64)

(def max-posted-utterances
  "Depth of the per-session POSTED-UTTERANCE ledger (scar B59). One entry per
   utterance the reducer ruled `:posted?`, so an at-least-once redelivery, an
   upload retry, or a re-render cannot post the same turn twice. 128 turns is
   far more than any retry burst can span, and it is a BOUND rather than a
   policy — 'have I already sent this one' is a memory question."
  128)

;; --- THE POST SEAM (scar B59) ---------------------------------------------
;;
;; THE HOLE THIS FILLS. `handle-transcript` now owns the whole post decision —
;; it derives cancel/over from the raw text, drops Whisper's silence
;; hallucinations, joins the partial ranges held in `:pending-parts`, and records
;; `:posted?` plus the joined `:text` on the utterance. Nothing then TURNED that
;; ruling into a message for Marvin: the dictate handler used to post, and the
;; capture port that replaced it deliberately reaches no verdict. A reducer whose
;; decision nothing acts on is not an authority; it is a log.
;;
;; WHY AN INSTALLED FN AND NOT A REQUIRE. The dependency runs surface -> session
;; loop and must keep running that way: `channel` already requires this
;; namespace (for `ingest-reply!`), so requiring `channel` back would be a cycle
;; AND would make the session loop know what a Slack seat is. Instead `channel`
;; installs the effect at load time and this namespace holds a slot it calls
;; without knowing what is on the other end — the same shape as an effect
;; handler, which is what a post IS.
;;
;; NIL IS A VALID VALUE. A test, the reducer lab, or a process that never loaded
;; `channel` has no post fn, and that must be a quiet no-op rather than a crash:
;; the reducer's ruling is still recorded on the utterance either way.

(defonce post-fn
  (atom nil))

(defn set-post-fn!
  "Install (or clear) the fn that delivers a posted turn: `(f seat {:utt/id id
   :text joined})`. Returns nil. Called once, at load time, by the surface that
   owns the outbound queue."
  [f]
  (reset! post-fn f)
  nil)

;; --- THE CLOCK DOMAIN -----------------------------------------------------
;;
;; THE INVARIANT (sol finding 5, 2026-07-26):
;;
;;   EVERY `:event/now` THE REDUCER EVER SEES IS STAMPED BY THIS SERVER, AT
;;   INGEST, IN SERVER ARRIVAL ORDER. NO CLIENT TIMESTAMP ORDERS ANYTHING.
;;
;; The reducer compares times across event KINDS and decides product questions
;; with the answer: `empty-window-blocks-a-reply?` ranks a reply's `:arrived-at`
;; against a window's `:opened-at` to tell "the reply is late to an idle window"
;; apart from "the user tapped to seize the mic and outranked the reply"
;; (tap-to-interrupt, golden-8). Those two timestamps used to come from two
;; DIFFERENT CLOCKS — `:opened-at` off a browser envelope's `Date.now()`,
;; `:arrived-at` off a reply the server itself stamped — and nothing anywhere
;; reconciled them. `protocol/explain` type-checks `:occurred-at-ms` and
;; requires it non-negative; it cannot check that a phone's clock agrees with
;; Cloud Run's. A device minutes fast made every reply look older than every
;; window; a device minutes slow, the reverse; a clock ROLLED BACK mid-session
;; (NTP correction, timezone/manual change, a suspended tab resuming) flipped
;; the ordering underneath a live conversation. The symptom is not a crash: it
;; is replies that mysteriously stop draining, or a tap-to-interrupt that
;; speaks over the user — behavior nobody can reproduce because the bug lives
;; on the reporter's wrist.
;;
;; WHY AT INGEST rather than at validation: this is the one place every event
;; passes through in the order the server accepted it — client POSTs and the
;; server's own `:fact/clock` alike — so it is the only place the stamps can be
;; made both single-domain AND strictly increasing.
;;
;; MOST-ORIGINAL-FORM: the client's own timestamp is NOT deleted. It is moved
;; to `:client/occurred-at-ms` and carried through the reducer and into the
;; journal as ordinary payload, where it is available for skew diagnostics and
;; for reading a trace against the device that produced it. It simply no longer
;; ORDERS anything.
;;
;; THE COST, stated rather than hidden: a client batches its energy facts
;; (`BATCH_MS` = 150 ms in the voice-lab page), and every event in one POST is
;; stamped at that POST's arrival. So intra-batch resolution collapses and the
;; reducer's timing quantizes to the batch cadence instead of the 50 ms sample
;; cadence. Every threshold the P-ledger owns is far larger than one batch
;; (`:sustain-ms` 450, `:silence-ms` 2200, `:tail-grace-ms` 450, `:chunk-ms`
;; 12000, `:idle-recycle-ms` 60000), the error is bounded by one batch, and it
;; does not accumulate — the next batch's stamp is real wall time again. A
;; bounded 150 ms quantization is the price of an ordering that cannot be
;; inverted by a stranger's clock, and it is worth paying.

(def ^:dynamic *now-ms*
  "THE server clock, as a 0-arg fn of epoch milliseconds.

   A var rather than a direct `System/currentTimeMillis` call for exactly one
   reason: a test that must drive time deterministically has to be able to,
   and the alternative — letting tests feed time through client envelopes — is
   the very coupling this section exists to remove. Production never rebinds
   it. A test that does is asserting about the SERVER's clock, which is now the
   only clock there is."
  #(System/currentTimeMillis))

(defn- now-ms ^long [] (long (*now-ms*)))

(defn- stamp
  "Pure: one accepted event, moved into the server's clock domain.

   The client's timestamp is retained verbatim under `:client/occurred-at-ms`
   (never dropped, never trusted)."
  [ev now]
  (-> ev
      (assoc :client/occurred-at-ms (:event/now ev))
      (assoc :event/now now)))

(defn stamp-batch
  "Pure: `events` in the server's clock domain, plus the session clock value to
   carry forward. Returns `[stamped-events clock']`.

   STRICTLY INCREASING, and that is load-bearing rather than tidy. Two events
   that arrive in the same millisecond — a reply POSTed by the relay and a
   `:fact/mic-opened` POSTed by the phone, which is precisely the race
   `empty-window-blocks-a-reply?` adjudicates — would otherwise carry EQUAL
   stamps, and the reducer's `>` comparison would silently rule for the window
   on a tie. `max(prev + 1, wall)` guarantees that the event the server
   accepted second is stamped later, whatever the wall clock's resolution, and
   it re-anchors on wall time the moment wall time overtakes it, so the stamps
   never drift away from real seconds."
  [events clock now]
  (reduce (fn [[acc c] ev]
            (let [c' (max (inc c) now)]
              [(conj acc (stamp ev c')) c']))
          [[] (or clock 0)]
          events))

;; --- Display ledgers (server-owned, pure) ---------------------------------
;;
;; NEITHER OF THESE IS A DECISION, and neither is reducer state. They are the
;; two things a DISPLAY needs that the reducer deliberately does not keep:
;;
;;   :convo        what was actually said, in the order it was said. The reducer
;;                 keeps no transcript archive on purpose (`handle-replay-reply`
;;                 says so in as many words: a replay of a long-gone reply must
;;                 CARRY its text, because the reducer has none). A remote with
;;                 no conversation log is not a remote, so the log is kept here.
;;
;;   :transitions  the status line's own history — the tape storyboard B renders
;;                 ("the reducer's thinking made visible", Gene 2026-07-27) and
;;                 the origin timestamp every elapsed timer counts from.
;;
;; Both are bounded, both are pure functions of what the reducer already
;; decided, and neither may ever be READ BACK by a decision. The direction of
;; travel is one way: reducer -> ledger -> screen.

(defn- upsert-by-id
  "Pure: replace the entry with this `:convo/id`, or append it. Bounded, oldest
   first. Upsert rather than append because the same logical turn can be
   reported twice — a partial transcript then a final one — and a conversation
   log that showed both would be reporting one utterance as two."
  [rows row]
  (let [i (first (keep-indexed #(when (= (:convo/id %2) (:convo/id row)) %1) rows))]
    (vec (take-last max-convo
                    (if i
                      (assoc (vec rows) i row)
                      (conj (vec rows) row))))))

(defn note-convo
  "Pure: extend the display conversation with whatever this step said out loud.

   Reads the REDUCER'S OWN record, never the raw event, for the user's side:
   `:fact/transcript` carries text 'exactly as STT produced it, INCLUDING any
   trailing protocol word', and the reducer strips and rules on it (P6). Showing
   the raw text would put '… over' on screen and would make the log disagree
   with the decision that was made from it. Marvin's side has no reducer record
   to read — the reply text exists only on the event — so it is taken from there
   and kept here, which is the whole reason this ledger exists.

   A TYPED TURN IS THE SAME ROW (bead 3he). `:cmd/say` mints an utterance exactly
   as a capture does, so its branch below differs from the spoken one in ONE
   expression — which utterance id to look up — and the row it produces is
   identical in shape: same `utt:` convo id, same `:who :you`, same `:status` and
   `:text`, read off the reducer's own record. That is the whole point. Nothing
   downstream of this ledger can tell Gene's keyboard from his voice, because
   there is nothing left in the row to tell them apart WITH."
  [convo {:keys [event state]}]
  (let [utt-row (fn [id]
                  (let [utt (get-in state [:utterances id])]
                    (if (or (nil? utt) (str/blank? (:text utt)))
                      convo
                      (upsert-by-id convo {:convo/id (str "utt:" id)
                                           :who      :you
                                           :utt/id   id
                                           :status   (:status utt)
                                           :text     (:text utt)}))))]
    (case (:event/type event)
      :fact/reply-arrived
      (if (str/blank? (:reply/text event))
        convo
        (upsert-by-id convo {:convo/id (str "reply:" (:reply/id event))
                             :who      :marvin
                             :reply/id (:reply/id event)
                             :text     (:reply/text event)}))

      :fact/transcript (utt-row (:utt/id event))
      :cmd/say         (utt-row (core/say-utt-id event))
      ;; A TAPPED OVER with no live capture posts its stash DIRECTLY (Scenario B
      ;; in `core/handle-over`), minting an utterance under `over-utt-id` exactly
      ;; as `:cmd/say` mints one — so it rides this same row, read off the
      ;; reducer's own record. (The capture-flush shape of OVER posts through
      ;; `:fact/transcript` above and needs nothing here.)
      :cmd/over        (utt-row (core/over-utt-id event))

      convo)))

(defn post-of-step
  "Pure: the ONE message this step commands, or nil.

   READS THE REDUCER'S OWN RECORD, never the raw event — the same rule
   `note-convo` follows and for a stronger reason. `:fact/transcript` carries
   text exactly as STT produced it, INCLUDING the trailing protocol word, and
   under `:endpoint-mode :keyword` a turn may be assembled from several such
   transcripts. `(get-in state [:utterances id])` is where the reducer put the
   JOINED, STRIPPED turn it actually ruled on; posting `(:text event)` would send
   Marvin the last fragment with '… over' on the end.

   `:posted?` is the whole gate. It is false for a cancel, false for a silence
   hallucination, false for a partial that is still being stashed, and false for
   a lone sign-off that stripped to nothing (bead 0r9) — every one of which is a
   ruling the surface would otherwise have to re-derive.

   A TYPED TURN RIDES THIS EXACT SEAM (bead 3he), and that is the point of having
   `:cmd/say` mint an utterance rather than carry its message inline. The only
   difference below is which id to look up; everything that makes this seam
   trustworthy — reading the reducer's `:posted?` ruling rather than the raw
   event, the blank guard, the dedup, the delivered-not-decided ledger in
   `commit-posted` — applies to a typed message with no second implementation to
   keep in step. The say-specific id is `say-<event-id>`, so a redelivered say
   envelope resolves to the same utterance and posts once.

   Idempotent by `utt/id` against `posted`, so a redelivered envelope, an upload
   retry, or a re-applied batch can never send the same turn twice.

   WHAT `posted` HOLDS, AND WHEN (sol, 2026-07-27). It is the DELIVERED ledger,
   not the decided one: `:posted-utts` advances only after `deliver-posts!`
   returns, so a turn the reducer ruled on but whose delivery THREW is still
   eligible here the next time the same utterance arrives. Within one batch the
   caller seeds this from the session ledger and advances it per step anyway, so
   two transcript steps for one utterance in one batch still post once."
  [posted {:keys [event state]}]
  (when-let [id (case (:event/type event)
                  :fact/transcript (:utt/id event)
                  :cmd/say         (core/say-utt-id event)
                  ;; a tapped OVER that posted its stash directly (Scenario B in
                  ;; `core/handle-over`) rides this seam exactly as `:cmd/say`
                  ;; does — same `:posted?` gate, same blank guard, same
                  ;; idempotency. The capture-flush shape posts via the
                  ;; `:fact/transcript` that returns for the flushed range.
                  :cmd/over        (core/over-utt-id event)
                  nil)]
    (let [utt (get-in state [:utterances id])]
      (when (and (:posted? utt)
                 (not (str/blank? (:text utt)))
                 (not (contains? (or posted #{}) id)))
        {:utt/id id :text (:text utt)}))))

(defn note-posted
  "Pure: record `id` as posted. Bounded FIFO — returns `[posted' order']`."
  [posted order id]
  (let [order' (vec (take-last max-posted-utterances (conj (vec order) id)))]
    [(set order') order']))

(defn commit-posted
  "Pure: record `ids` as DELIVERED on a session value.

   THE LEDGER ADVANCES ON THE RECEIPT, NOT ON THE DECISION (sol, 2026-07-27).
   `ingest` used to write `:posted-utts` inside the swap, i.e. BEFORE
   `deliver-posts!` had run — so an outbound queue that threw (a Slack outage, a
   full queue, a connector restart) marked the turn delivered on the way past and
   the dedup ledger then suppressed every later attempt at the same utterance
   FOREVER. The reducer's ruling was intact and the words never left the building.

   The decision and the receipt are two different facts, so they are two different
   writes: the swap records the RULING (`:posted?` on the utterance, `:last-posts`
   for the seam), and this records the DELIVERY, after the fn returned without
   throwing. A turn whose delivery failed stays eligible, which is what makes the
   client's own upload retry — a fresh `event/id` carrying the same `utt/id` —
   able to land it. The redelivery it opens is at-least-once by construction, so
   the receiving lane is keyed on `utt/id` too (see channel.clj's post fn)."
  [session ids]
  (reduce (fn [s id]
            (if (contains? (:posted-utts s #{}) id)
              s                                   ; a receipt recorded twice is one receipt
              (let [[posted' order'] (note-posted (:posted-utts s #{}) (:posted-order s) id)]
                (assoc s :posted-utts posted' :posted-order order'))))
          session
          ids))

(defn note-transition
  "Pure: append a row when what the SURFACE SAYS changes — or when the server
   ACCEPTED a command, which is a row of its own.

   Keyed on the rendered `status-view`/`turn-view` pair rather than on the
   reducer's internal phase, and that is the point: the tape is a record of what
   the user was TOLD, so it can be read against what they remember seeing. Two
   different internal states that say the same sentence are ONE row here; the
   sentence is the observable.

   THE RECEIPT ROW (Gene, 2026-07-27, live field test: 'no visual indication of
   any receipt from server'). Half the transport is commands that change no
   sentence — mute, replay, a boundary press — so under the dedup rule above the
   screen stayed perfectly still and the user could not tell an accepted command
   from a dropped one. That is D6's visible-failure rule read from the other
   side: an ACCEPTED command must also be visible. So a step carrying `heard`
   (the command's event type) ALWAYS appends, and the row is tagged with it. The
   tape moving IS the receipt.

   `heard` is passed only for commands the reducer ACCEPTED — the caller decides
   that by comparing diagnostics across the step, so a refused command (the
   reducer's `:diag/ignored-command`) never renders as a receipt. A receipt for
   a command that was thrown away would be the same lie in a new place.

   `:at` is a SERVER stamp (the same `at` the batch was ingested with), because
   it is also the origin every elapsed timer counts from — and a display clock
   anchored on a client timestamp is the drift THE CLOCK DOMAIN section exists
   to prevent."
  ([transitions state at] (note-transition transitions state at nil))
  ([transitions state at heard] (note-transition transitions state at heard false))
  ([transitions state at heard stale?]
   (let [row  (cond-> {:at     at
                       :status (core/status-view policy/current state)
                       :turn   (core/turn-view state)}
                heard  (assoc :heard heard)
                stale? (assoc :stale? true))
         prev (peek (vec transitions))]
     (if (and (nil? heard) prev (= (dissoc prev :at) (dissoc row :at)))
       (vec transitions)
       (vec (take-last max-transitions (conj (vec transitions) row)))))))

;; --- Stale intent (sol finding 4.2) ---------------------------------------
;;
;; THE DEFECT, in one sentence: two taps on the same rendered button both author
;; the same command, because the second one leaves the phone BEFORE the morph
;; that would have re-labelled the button arrives. On 2026-07-28 that is a mic
;; that opens and immediately re-opens; in general it is the user's second
;; press being answered as if it were a new intent.
;;
;; THE FIX IS NOT A CLIENT DECISION, and that is the whole design constraint: a
;; page that disabled its own button would be holding product state in the DOM
;; (v2.5 forbids it) and would still be guessing. So the SERVER stamps the
;; render it produced — `data-mv-epoch`, the session's own `:seq` — onto the
;; surface, every control echoes it back in `:ui/epoch`, and the session refuses
;; a command whose (epoch, kind) pair it has ALREADY accepted this session.
;;
;; WHY THE PAIR AND NOT THE EPOCH ALONE. Every accepted command changes the tape,
;; which changes the view, which bumps `:seq` — so one render epoch can only ever
;; legitimately produce one command OF A GIVEN KIND. Refusing on the bare epoch
;; would also refuse the honest case of two DIFFERENT controls tapped inside one
;; morph window (talk, then Mute), which is a real thing a user does. The pair
;; refuses exactly the double-tap and nothing else.
;;
;; AN ENVELOPE WITH NO `:ui/epoch` IS NEVER REFUSED. The native shell and any
;; N−1 page do not stamp one; absence means "this sender does not participate",
;; not "epoch 0". Fail closed applies to authorization, not to a display hint.

(defn stale-command?
  "Pure: is this a SECOND command of the same kind from a render whose epoch has
   already been spent? Non-commands, and commands with no `:ui/epoch`, are never
   stale."
  [seen ev]
  (let [t (:event/type ev)
        e (:ui/epoch ev)]
    (boolean (and (keyword? t)
                  (= "cmd" (namespace t))
                  (integer? e)
                  (contains? (or seen #{}) [e t])))))

(defn note-command-epoch
  "Pure: spend this render epoch for this command kind. Bounded FIFO — returns
   `[seen' order']`. A non-command, or a command that carried no epoch, spends
   nothing and returns the pair unchanged."
  [seen order ev]
  (let [t (:event/type ev)
        e (:ui/epoch ev)]
    (if-not (and (keyword? t) (= "cmd" (namespace t)) (integer? e))
      [(or seen #{}) (vec order)]
      (let [order' (vec (take-last max-command-epochs (conj (vec order) [e t])))]
        [(set order') order']))))

(defn accepted-command
  "Pure: the command event type when this step was an ACCEPTED command, else nil.

   Accepted means the reducer recorded no new diagnostic for it. `core/step`
   refuses a command it cannot honor by appending to `:diagnostics`
   (`:diag/ignored-command`, `:diag/unknown-reply`, …) and otherwise leaves the
   vector alone, so comparing the vectors across the step is the reducer's own
   verdict rather than a second opinion about what a command should have done.
   Compared by VALUE and not by count, because `:diagnostics` is bounded — a
   full ring appends without growing."
  [ev before-state after-state]
  (let [t (:event/type ev)]
    (when (and (keyword? t)
               (= "cmd" (namespace t))
               (= (:diagnostics before-state) (:diagnostics after-state)))
      t)))

;; --- Session store --------------------------------------------------------
;;
;; ONE atom holding seat -> session value. Every mutation is a pure function of
;; the previous value applied inside `swap-vals!`, so the swap can retry safely;
;; the side effects (journal, broadcast, log) all run AFTER the swap, reading
;; what the swap decided. That ordering is the whole concurrency story.

(defonce sessions
  (atom {}))

(defonce ^:private subscribers
  ;; seat -> #{http-kit channel}
  (atom {}))

(defonce ^:private clock-counter (AtomicLong. 0))

(defn new-session
  "Pure: a fresh session value for `seat`.

   `:seq` is the effect-outbox CURSOR — monotonic, never reused, and the only
   thing a reconnecting client needs to send to be caught up. `:utt-order`
   exists because the reducer's `:utterances` is a MAP: the reducer has no
   reason to care what order captures happened in, and a display does, so the
   ordering is kept here rather than pushed into the reducer's state shape."
  [seat now]
  (let [sid (str "vs-" seat "-" now)]
    {:session/id sid
     :seat       seat
     :state      (core/initial-state sid)
     :outbox     []
     :seq        0
     :utt-order  []
     :last-view  nil
     :last-steps []
     ;; --- the idempotency ledger (S5c delta-4, server half) ---------------
     ;;
     ;; `event/id` -> the `:seq` this session had reached when that event was
     ;; FIRST applied. The client's fact outbox retries on failure with the
     ;; SAME `event/id` (reducer-session-client.md §3.1 rule 4), so a redelivery
     ;; is normal traffic, not an error — and it must produce the SAME OUTCOME,
     ;; not a second application. Held IN THE SESSION rather than in a
     ;; seat-keyed store on the side, because that is what makes a 409 correct
     ;; for free: a new session mints an empty ledger, which is exactly right,
     ;; since the old ids belong to a session that no longer exists.
     :seen-events {}
     :seen-order  []
     ;; --- display ledgers (server-owned; NOT reducer state) ----------------
     ;;
     ;; Same rationale as `:utt-order` above: the reducer has no reason to keep
     ;; a conversation archive (it keeps none — see `handle-replay-reply`) or a
     ;; history of its own status line, and a DISPLAY has every reason to. So
     ;; they are kept here, next to the ordering, rather than pushed into the
     ;; reducer's state shape.
     :convo       []
     :transitions (note-transition [] (core/initial-state sid) now)
     ;; --- the render-epoch ledger (sol 4.2) --------------------------------
     ;;
     ;; `#{[epoch command-kind]}` plus its insertion order, bounded. Held in the
     ;; SESSION for the same reason `:seen-events` is: a 409 mints a new session
     ;; and therefore an empty ledger, which is exactly right — the epochs it
     ;; held were render cursors of a session that no longer exists.
     :cmd-seen    #{}
     :cmd-order   []
     ;; --- the POSTED-UTTERANCE ledger (scar B59) ---------------------------
     ;;
     ;; `#{utt/id}` plus its insertion order, bounded. Held in the SESSION for
     ;; the same reason `:seen-events` is: a 409 mints a new session, and the
     ;; utterance ids it held belonged to a session that no longer exists.
     ;; `:last-posts` is the SCRATCH slot the post-swap side effect reads —
     ;; a sibling of `:last-steps`, never state.
     :posted-utts #{}
     :posted-order []
     :last-posts  []
     ;; the per-POST accounting the handler reads back (applied vs duplicate) —
     ;; a scratch slot like :last-steps, never state.
     :last-ingest nil
     ;; the last `:event/now` this session issued — see `stamp-batch`. Zero, not
     ;; `now`: `max(prev + 1, wall)` re-anchors on wall time immediately, so a
     ;; fresh session's first event is stamped with the real epoch millisecond
     ;; it arrived at rather than one past the millisecond the session was made.
     ;;
     ;; ACCEPTED RESIDUAL (sol round-2 finding 5, documented rather than fixed):
     ;; because the clock seeds from ZERO, the first stamp after a restart comes
     ;; from wall time. If the server's wall clock were rolled BACKWARD across a
     ;; restart, journal entries written after it could carry stamps that order
     ;; before entries written before it — the journal is the only place this can
     ;; be observed. LIVE ordering is untouched: sessions do not survive a
     ;; restart, and within one session the clock is monotonic by construction
     ;; (`max(prev + 1, wall)`), so no reducer decision, lease epoch, or effect
     ;; sequence can be inverted by it. Making this residual impossible would mean
     ;; persisting a clock floor across restarts — durable state bought to defend
     ;; against an operator rolling back the host clock, which is not a trade this
     ;; system should make. If journal ordering ever becomes load-bearing (an
     ;; audit, a replay across restarts), that is when it gets paid for.
     :clock      0
     :created-at now
     :touched-at now}))

;; --- The display view (server-owned, pushed down) -------------------------

(defn status-line
  "Pure: `core/status-view` rendered as the one line the surface shows.

   NO DECISION LIVES HERE — that is rule 1 of this namespace, and until
   2026-07-26 this function broke it. It authored the sentence itself, from its
   own `cond` over `:hf :phase` and `:speaking`: user-visible status is PRODUCT
   POLICY (ARCHITECTURE.md:80-82, D2), 'green never lies' (P4) is an invariant
   about what the user is TOLD, and a second reader of phase semantics is a
   second thing that has to stay right about them. sol's re-score named it; the
   cond now lives in `core/status-view` with the rest of the product, tested in
   core's own suite.

   What is left is the only thing transport is allowed to do with a view:
   JOIN it. Icon and text are separate in the reducer so a surface can lay them
   out its own way; a surface that just wants the line gets the icon, a space,
   and the text — and the one row with no icon (the idle invitation) gets no
   leading space."
  [state]
  (let [{:keys [icon text]} (core/status-view state)]
    (if (str/blank? icon) text (str icon " " text))))

(defn view
  "Pure: everything the surface displays, as data. The page renders it; it never
   computes it (D4: an adapter holds no product state)."
  [session]
  (let [state (:state session)
        utts  (:utterances state)]
    {:session/id     (:session/id session)
     :mode           (:mode state)
     :phase          (get-in state [:hf :phase])
     :mic-open?      (boolean (get-in state [:hf :mic-open?]))
     :paused?        (boolean (get-in state [:hf :paused?]))
     :muted?         (boolean (:muted? state))
     :speech?        (pos? (or (get-in state [:hf :speech-at]) 0))
     :speaking       (get-in state [:speaking :reply/id])
     :queue          (count (:reply-queue state))
     :capture-epoch  (get-in state [:leases :capture :epoch])
     :playback-epoch (get-in state [:leases :playback :epoch])
     :status         (status-line state)
     ;; S5c C1 (bead 9zu): the two DERIVED views the surface renders, served
     ;; verbatim. `:status-icon`/`:status-text` are the same pair `:status`
     ;; joins — carried apart as well as joined so a layout can lay them out its
     ;; own way (storyboard A puts the icon in a card; storyboard B does not use
     ;; it at all) without a page re-deriving either half.
     :status-icon    (:icon (core/status-view state))
     :status-text    (:text (core/status-view state))
     :turn           (core/turn-view state)
     ;; S5c C2: the transport's own derived view — which of the player-dock
     ;; controls are LIVE right now (prev/next/latest/replay/skip) and whether
     ;; playback is paused. Derived in core with the status and turn lines and
     ;; for the same reason (D2): whether a button may be pressed is a product
     ;; ruling, and a surface that computed it would be a second authority on
     ;; the reply queue.
     :transport      (core/transport-view state)
     ;; the manual TURN controls (director-control port): whether OVER and CANCEL
     ;; are LIVE right now. Derived in core with the transport for the same D2
     ;; reason — whether a turn can be posted or discarded is a product ruling,
     ;; and a surface that computed it would be a second authority on the stash.
     :capture        (core/capture-view state)
     ;; the display ledgers — see `note-convo` / `note-transition`. `:since` is
     ;; the server stamp the elapsed timer counts from.
     :convo          (vec (:convo session))
     :transitions    (vec (:transitions session))
     ;; …and it counts from the last STATE row, never from a receipt row: a
     ;; receipt says "the server heard you", not "you are now in a new state",
     ;; and letting one restart the elapsed clock would make the timer lie about
     ;; how long the session has been listening every time the user hit Mute.
     :since          (or (:at (peek (filterv #(nil? (:heard %)) (:transitions session))))
                         (:at (peek (vec (:transitions session)))))
     ;; S6: a capture whose bytes never reached the server is SHOWN, and keeps
     ;; being shown after the session has recovered and moved on. Derived in
     ;; core (`upload-failures`) like the status line and for the same reason —
     ;; what counts as a lost capture is the reducer's ruling, not a display's.
     :upload-failures (core/upload-failures state)
     :transcripts    (vec (for [id (take-last 8 (:utt-order session))
                                :let [u (get utts id)]
                                :when (and u (not (str/blank? (:text u))))]
                            {:utt/id id :status (:status u) :text (:text u)}))
     :diagnostics    (vec (for [d (take-last 5 (:diagnostics state))]
                            (select-keys d [:diag/kind :reason :event/type])))}))

;; --- The pure ingest ------------------------------------------------------

(defn- note-utterances
  "Pure: extend the display ordering with any capture ids the reducer has minted
   since the last event. Insertion order, bounded."
  [order state]
  (let [known (set order)
        new'  (remove known (keys (:utterances state)))]
    (vec (take-last 24 (into (vec order) new')))))

(defn dedup-events
  "Pure: split `events` into the ones this session has NEVER applied and the ones
   it already has, by `event/id`.

   Two sources of duplication, and both are ordinary rather than exceptional:
   the client's fact outbox resends the same bytes after a network failure or a
   timeout (reducer-session-client.md §3.1 rule 4, 'with its ORIGINAL event/id
   intact'), and a batch can carry the same id twice if a retry raced its own
   success. `:ids` catches the second case INSIDE the batch, so one POST can
   never apply one id twice either.

   An event with no `event/id` is treated as fresh — the schema requires the
   field (`protocol/explain` rejects a batch without it before this is ever
   called), so a missing id here means a caller bypassed validation, and
   dropping it silently would be the worse failure."
  [session events]
  (let [seen (:seen-events session {})]
    (reduce (fn [acc ev]
              (let [id (:event/id ev)]
                (if (and (some? id)
                         (or (contains? (:ids acc) id) (contains? seen id)))
                  (update acc :duplicates conj ev)
                  (-> acc
                      (update :fresh conj ev)
                      (cond-> (some? id) (update :ids conj id))))))
            {:fresh [] :duplicates [] :ids #{}}
            events)))

(defn remember-events
  "Pure: record each applied event id against the cursor the session reached, so
   a redelivery of the same envelope can be ANSWERED rather than re-applied.

   Bounded FIFO at `max-seen-events`; eviction drops the oldest ids, which is
   correct because an id old enough to fall out of a 512-deep window is older
   than any realistic retry burst — and, in the one case it is not, the reducer's
   own `:seen-event-ids` (the same depth) still makes the re-application a
   no-op. Two independent windows, so a defect in either one degrades to the
   other rather than to a double-applied command."
  [session events cursor]
  (if (empty? events)
    session
    (let [ids   (keep :event/id events)
          seen' (reduce #(assoc %1 %2 cursor) (:seen-events session {}) ids)
          order (vec (concat (:seen-order session []) ids))
          keep' (vec (take-last max-seen-events order))
          drop' (set (remove (set keep') order))]
      (assoc session
             :seen-events (apply dissoc seen' drop')
             :seen-order  keep'))))

(defn ingest
  "PURE: apply `events` (already validated, already reducer-shaped) to a session
   value.

   IDEMPOTENT BY EVENT ID (S5c delta-4, server half). Every envelope is checked
   against the session's ledger FIRST: an id this session has already applied is
   not stepped, not journaled, and not broadcast a second time — it is counted,
   and the cursor it produced the first time is reported back, so a client that
   resends a batch it never got an answer for receives the SAME outcome instead
   of a second application. Before this existed the retry was safe only because
   the client never retried (reducer-session-client.md §4.11 called it the
   blocker for the outbox); with the outbox landing, `core/step`'s own
   `:seen-event-ids` would have absorbed the state change but the session loop
   would still have journaled the event twice and told the client it had
   accepted new work.

   THE CLOCK DOMAIN IS ESTABLISHED HERE, first thing, for every event without
   exception: `stamp-batch` replaces each `:event/now` with a server stamp taken
   at `at` and made strictly increasing, and moves the client's own timestamp to
   `:client/occurred-at-ms` as payload. Nothing downstream — not `core/step`,
   not the journal, not the outbox — ever sees a client clock in a position to
   order anything. The invariant and the reasons are stated in full at the top
   of this namespace under THE CLOCK DOMAIN; this is where it is enforced.

   Returns the next session value, whose `:last-steps` holds the per-event
   breakdown `[{:event e :effects [..] :state after}]` — the scratch slot the
   post-swap side effects (journal, SSE broadcast) read so they never have to
   re-derive what the swap decided.

   A batch is appended when the step produced effects OR the display view
   changed. Both halves matter: effects are work the client must do, and a view
   change is state the client must show — an energy tick that does neither
   produces no batch at all, which is what keeps a 150 ms metering stream from
   becoming a 150 ms SSE stream."
  [session events at]
  (let [{:keys [fresh duplicates]} (dedup-events session events)
        [events clock'] (stamp-batch fresh (:clock session) at)
        {:keys [state effects steps order convo transitions cmd-seen cmd-order
                posts]}
        (reduce (fn [acc ev]
                  (if (stale-command? (:cmd-seen acc) ev)
                    ;; A SECOND TAP ON A SUPERSEDED RENDER. It is not stepped —
                    ;; the reducer never sees it, so it can neither open a second
                    ;; capture nor record a diagnostic about one — and it is not
                    ;; silent either: the tape gets a row saying the surface HEARD
                    ;; it and REFUSED it, which is the same D6 visibility rule the
                    ;; receipt row satisfies from the other side.
                    (assoc acc :transitions (note-transition (:transitions acc) (:state acc)
                                                             at (:event/type ev) true))
                    (let [before (:state acc)
                          {:keys [state effects]} (core/step (policy/effective) before ev)
                          step {:event ev :effects (vec effects) :state state}
                          [seen' order'] (note-command-epoch (:cmd-seen acc) (:cmd-order acc) ev)
                          ;; THE POST DECISION, READ (never re-made): the
                          ;; reducer's own `:posted?` on the utterance this step
                          ;; completed. Recorded here, delivered after the swap.
                          post  (post-of-step (:posted acc) step)
                          [posted' porder'] (if post
                                              (note-posted (:posted acc) (:posted-order acc)
                                                           (:utt/id post))
                                              [(:posted acc) (:posted-order acc)])]
                      (-> acc
                          (assoc :state state)
                          (assoc :cmd-seen seen' :cmd-order order')
                          (assoc :posted posted' :posted-order porder')
                          (cond-> post (update :posts conj post))
                          (assoc :order (note-utterances (:order acc) state))
                          ;; the ledgers advance PER STEP, not per batch: the tape
                          ;; is a record of the reducer's thinking, and folding a
                          ;; whole batch into one row would hide the middle of it.
                          (assoc :convo (note-convo (:convo acc) step))
                          (assoc :transitions (note-transition (:transitions acc) state at
                                                               (accepted-command ev before state)))
                          (update :effects into effects)
                          (update :steps conj step)))))
                {:state (:state session) :effects [] :steps []
                 :order (:utt-order session)
                 :convo (vec (:convo session))
                 :transitions (vec (:transitions session))
                 :cmd-seen (:cmd-seen session #{})
                 :cmd-order (vec (:cmd-order session))
                 :posted (:posted-utts session #{})
                 :posted-order (vec (:posted-order session))
                 :posts []}
                events)
        session'  (assoc session
                         :state state :utt-order order
                         :convo convo :transitions transitions
                         :cmd-seen cmd-seen :cmd-order cmd-order
                         ;; `:posted-utts` / `:posted-order` are DELIBERATELY NOT
                         ;; advanced here (sol, 2026-07-27): they are the DELIVERED
                         ;; ledger and `deliver-posts!` writes them, after the
                         ;; outbound fn returned. `posted`/`posted-order` above are
                         ;; the batch-local accumulators that keep two transcript
                         ;; steps for one utterance inside THIS batch to one post.
                         :last-posts (vec posts)
                         :touched-at at :clock clock')
        v         (view session')
        batch?    (or (seq effects) (not= v (:last-view session)))
        n         (if batch? (inc (:seq session)) (:seq session))
        ;; A PURE REPLAY reports the cursor the batch produced the FIRST time —
        ;; the same outcome, not today's. A MIXED batch (some new, some already
        ;; applied) reports the new cursor, because new work really was done.
        replayed  (when (seq duplicates)
                    (let [cs (keep #(get (:seen-events session) (:event/id %)) duplicates)]
                      (when (seq cs) (apply max cs))))]
    (-> (assoc session' :last-steps steps :seq n)
        (cond->
         batch?
          (-> (assoc :last-view v)
              (update :outbox (fn [ob]
                                (vec (take-last max-batches
                                                (conj (vec ob)
                                                      {:batch/seq    n
                                                       :batch/at     at
                                                       :batch/events (mapv :event/id events)
                                                       :effects      (vec effects)
                                                       :view         v})))))))
        (remember-events events n)
        (assoc :last-ingest {:applied    (count events)
                             :duplicates (count duplicates)
                             :cursor     n
                             :replay-cursor replayed}))))

(defn batches-after
  "Pure: the retained batches a client at `cursor` has not seen, plus whether the
   ring dropped any of them.

   D5: 'if the buffer can't hold the gap, record and DISPLAY explicit loss.
   Never silently resume.' A client whose cursor is older than the oldest
   retained batch gets `:lost? true` and shows it."
  [session cursor]
  (let [ob     (:outbox session)
        oldest (:batch/seq (first ob))
        cur    (or cursor 0)]
    {:batches (vec (filter #(> (:batch/seq %) cur) ob))
     :lost?   (boolean (and (pos? cur) oldest (< cur (dec oldest))))}))

;; --- Journal taps (S4 continues to run under S5a) -------------------------
;;
;; The registry (`resources/protocol/semantic-trace-1.edn`) is a CLOSED set
;; shared with semanticTrace.ts, so these tables map onto it and never extend
;; it. An event or effect with no row is not journaled — inventing a name here
;; would be exactly the drift the registry's own editing rules forbid, and a
;; name outside the set lands in the :unmapped quarantine and alarms.
;;
;; `:fact/audio-energy` and `:fact/clock` are deliberately absent: they are
;; sampling, not lifecycle, and journaling them at 4-7 Hz would evict the whole
;; ring (S7) with rows carrying no causal fact.
;;
;; `:fact/upload-failed` is absent for the opposite reason and it is a NAMED
;; GAP, not an oversight: the semantic vocabulary is a CLOSED set of 15 names
;; shared with semanticTrace.ts, and it has no name for a failed send —
;; `:utterance-sent` would be a lie and `:utterance-send-attempt` already
;; denotes `:fact/range-committed`. Inventing one here is exactly the drift the
;; registry's editing rules forbid. The failure is not lost: it is reducer
;; STATE (`core/upload-failures`) and a `:diag/upload-failed` record. If S4
;; comparison ever needs it, the registry gains the name first, in its own
;; edit, on both sides of the wire.

(def event->semantic
  "Reducer event type -> the semantic-trace name it denotes. The reviewable
   claim: each row asserts that this event IS that lifecycle moment."
  {:cmd/hf-start           :mode-selected
   :cmd/hf-stop            :mode-selected
   :cmd/ptt-start          :mode-selected
   :fact/mic-opened        :capture-started
   :fact/range-committed   :utterance-send-attempt
   :fact/upload-acked      :utterance-sent
   :fact/transcript        :endpoint-check
   :fact/reply-arrived     :reply-offered
   :fact/playback-started  :playback-started
   :fact/playback-finished :playback-finished
   :fact/playback-failed   :playback-failed})

(def event->server-observation
  "Reducer COMMAND -> the `:server/` observation it denotes (S5c A2).

   P9's mute and replay are reply-QUEUE policy, and the closed 15-name semantic
   vocabulary — which is one list with semanticTrace.ts — has no word for
   either. Inventing `:reply-muted` here is exactly the drift the registry's
   editing rules forbid. But these commands are applied SERVER-SIDE by this
   session loop, which makes them server observations in the namespace that
   exists for precisely that, and they are declared in
   `semantic-trace-1.edn` :server-observations.

   Journaling them is not cosmetic. `shadow/journal->events` replays the
   journal's `:event`s, so an unjournalled `:cmd/mute` means the replayed
   session's `:muted?` is ALWAYS false — the shadow would then score every
   correctly-withheld reply as a divergence, and `:corpus-complete?` could never
   witness a mute or a replay at all."
  {:cmd/mute         :server/reducer-mute
   :cmd/unmute       :server/reducer-mute
   :cmd/replay-reply :server/reducer-replay})

(defn effect->semantic
  "Pure: the semantic-trace name an EMITTED effect denotes, or nil.

   Four lifecycle moments are visible only in the reducer's output, never in
   its input: a discarded window (`:close-mic` with no commit) is the cancel, a
   `:stop-speech` is the interruption, the speech-registered cue is the moment
   P5's sustained-loudness rule fired, and every OTHER cue is the `:cue` event
   itself. Reading them off the effects is what keeps them from being re-derived
   (and re-decided) here.

   ONE EFFECT YIELDS ONE NAME (S5c A2). The `:speech-registered` cue stays
   `:speech-started` — the stronger, more specific claim — and does NOT also
   emit `:cue`. The golden traces (perfectWebGoldenTraces.ts) carry both names
   around one onset because two different producers emit them; deriving both
   from ONE effect here would report a single causal fact twice, which is the
   same double-count the registry refuses for barge-in outcomes."
  [effect]
  (case (:effect/type effect)
    :close-mic    (when (nil? (:commit effect)) :utterance-canceled)
    :stop-speech  :playback-interrupted
    :cue          (if (= :speech-registered (:cue effect)) :speech-started :cue)
    :open-mic     :capture-rearmed
    nil))

(defn- step-ids
  "Pure: every stable identifier available at this step, IN ITS ACTUAL DOMAIN
   (S4a). The utterance id comes off the reducer's own `:capturing` record or
   the event's `:utt/id`; the reply id off the event or the current playback.
   Nothing is inferred from time, sequence, or transcript match — an id that is
   not there is left out, because a manufactured join is worse than a named gap."
  [ev state]
  (let [utt (or (:utt/id ev)
                (get-in ev [:commit :utt/id])   ; a :close-mic effect's committed range
                (get-in state [:capturing :utt/id]))
        rep (or (:reply/id ev)
                (get-in state [:speaking :reply/id]))]
    (cond-> {}
      (some? utt) (assoc :utterance utt)
      (some? rep) (assoc :reply rep))))

(defn journal-step!
  "Record ONE applied event (and any lifecycle-bearing effects it produced) into
   the shadow journal. Total and fire-and-forget by construction — `record!`
   returns nil and swallows, `observe!` covers the marshalling around it — so a
   journal problem can never reach the session loop.

   WHICH STATE AN EFFECT'S IDS COME FROM is not cosmetic. `:close-mic`'s cancel
   and `:stop-speech`'s interruption refer to something the event just ENDED, so
   their ids exist only in the state that PRECEDED them; `:open-mic` refers to a
   capture the event just MINTED, which exists only in the state that follows.
   Reading either from the wrong side records a null identity, and S4a calls a
   dropped identifier an instrumentation defect."
  [seat {:keys [event effects state]} prior]
  (shadow/observe!
   seat
   (fn []
     (when-let [name' (get event->semantic (:event/type event))]
       (shadow/record! seat name'
                       (cond-> {:source :reducer-session
                                :outcome :accepted
                                :event   event
                                :ids     (step-ids event state)}
                         (contains? event :text)   (assoc :text (:text event))
                         (contains? event :final?) (assoc :final? (boolean (:final? event))))))
     ;; S5c A2: the reply-queue COMMANDS, in the :server/ namespace (see
     ;; `event->server-observation`). `:event` is preserved so the replay
     ;; reconstructs the real mute state rather than a session that was never
     ;; muted; `:live` states which way the mute went, since one observation
     ;; name covers both commands.
     (when-let [obs (get event->server-observation (:event/type event))]
       (shadow/record! seat obs
                       (cond-> {:source  :reducer-session
                                :outcome :accepted
                                :event   event
                                :ids     (step-ids event state)}
                         (= :server/reducer-mute obs)
                         (assoc :live {:muted? (= :cmd/mute (:event/type event))}))))
     (doseq [e effects]
       (when-let [name' (effect->semantic e)]
         (shadow/record! seat name'
                         {:source  :reducer-session
                          :outcome :accepted
                          :ids     (step-ids e (if (= :open-mic (:effect/type e))
                                                 state
                                                 prior))}))))))

;; --- Wire decoding (JSON has no keywords) ---------------------------------

(def ^:private keyword-fields
  "Fields the schema types `:keyword`. JSON cannot carry a keyword, so exactly
   these arrive as strings and are converted back. DERIVED FROM THE SCHEMA, so
   adding a keyword-typed field to marvin-voice-1.edn cannot leave the decoder
   behind (D6: the contract and its codecs are one thing)."
  (delay (set (keep (fn [[k tag]] (when (= :keyword tag) k)) protocol/field-types))))

(defn coerce-envelope
  "Pure: one JSON-decoded map into a marvin.voice/1 envelope.

   Keys are already keywordized by the reader (`session/id` -> `:session/id`).
   This restores keyword VALUES for the schema's keyword-typed fields and
   nothing else — a decoder that guessed at other values would be interpreting
   the payload, which is validation's job, not decoding's."
  [m]
  (if-not (map? m)
    m
    (reduce (fn [acc k]
              (let [v (get acc k)]
                (if (string? v) (assoc acc k (keyword v)) acc)))
            m
            @keyword-fields)))

(defn- json-str
  "Pure: EDN -> JSON with NAMESPACES PRESERVED, in keys and values alike.

   data.json's default writer calls `name` on keywords, which would render
   `:effect/type`, `:client/id`, `:utt/id` and `:lease/epoch` as `type`, `id`,
   `id` and `epoch` — three collisions and a silent contract change. The wire
   uses the full `namespace/name` string in both directions, so the JSON a
   client sends back round-trips through `coerce-envelope` unchanged."
  [x]
  (let [full (fn [k] (if (keyword? k) (subs (str k) 1) k))]
    (json/write-str (walk/postwalk (fn [v] (if (keyword? v) (full v) v)) x)
                    :escape-slash false)))

(defn- json-response
  [body status]
  {:status  status
   :headers {"Content-Type" "application/json; charset=utf-8"
             "Cache-Control" "no-store"}
   :body    (json-str body)})

(defn valid-seat?
  "A seat is a short slug — same rule as the rest of the app (channel.clj)."
  [seat]
  (boolean (and (string? seat) (re-matches #"[A-Za-z0-9_-]{1,40}" seat))))

(defn- seat-of [req]
  (or (get-in req [:params :seat])
      (get-in req [:query-params "seat"])
      "bridge"))

(defn- body-string [req]
  (let [b (:body req)]
    (cond
      (nil? b)    ""
      (string? b) b
      :else       (slurp b))))

(defn parse-envelopes
  "Pure-ish: request body -> [envelope ...], or `{:error ...}`.

   Accepts a bare JSON array or `{\"events\": [...]}`, because a batching client
   and a one-shot client should not need two endpoints. Anything else is a
   parse failure, reported rather than coerced."
  [body]
  (if (str/blank? body)
    {:error "empty body"}
    (let [parsed (try (mjson/parse body)
                      (catch Exception e {::bad (.getMessage e)}))]
      (cond
        (::bad parsed)      {:error (str "unparseable JSON: " (::bad parsed))}
        (sequential? parsed) (mapv coerce-envelope parsed)
        (and (map? parsed)
             (sequential? (:events parsed))) (mapv coerce-envelope (:events parsed))
        :else               {:error "body must be a JSON array of envelopes, or {\"events\": [...]}"}))))

(defn validate-batch
  "Pure: nil when every envelope conforms AND converts to a reducer event; else
   the FIRST failure as `{:index i :code kw :detail s}`.

   FAIL CLOSED, ALL-OR-NOTHING (D6: 'schema violations ... fail closed and
   visibly'). One bad envelope rejects the whole POST and applies none of it.
   Partial application would be worse than either extreme: the client could not
   tell which half of its batch the session had seen, and at-least-once delivery
   would then replay a mixture."
  [envelopes]
  (first
   (keep-indexed
    (fn [i env]
      (if-let [bad (protocol/explain env)]
        (assoc bad :index i)
        (when-not (protocol/fact->event env)
          {:index i :code :not-an-event
           :detail (str "kind " (pr-str (:kind env))
                        " is not an event kind (effects are reducer OUTPUTS, never inputs)")})))
    envelopes)))

;; --- Broadcast ------------------------------------------------------------

(defn- sse-frame [event data]
  (str "event: " event "\ndata: " (json-str data) "\n\n"))

(defn- send-frame!
  "Send one frame; false when the peer is gone."
  [ch payload]
  (boolean (try (http/send! ch payload false) (catch Exception _ false))))

(defn- broadcast!
  "Push a batch to every live subscriber of `seat`, dropping the dead ones.

   THE FRAME IS BUILT HERE, THE SOCKETS ARE WRITTEN SOMEWHERE ELSE, and the
   split is the point. This runs on the thread that just applied a client's
   events — an http-kit worker, and this server runs `:thread 8` — so a write
   that blocks blocks a POST handler, and eight of them starve the entire worker
   pool while unrelated requests time out. A half-dead phone socket blocks for
   exactly that long. So the caller's thread does only the pure work (render the
   batch to a frame) and hands the writes to `sse-registry`'s single push agent,
   which is `datastar-kit.sse`'s discipline, transplanted: off-thread, and
   serialized, so two batches decided a millisecond apart still paint in the
   order the reducer decided them.

   A channel whose write fails is dropped from BOTH ledgers — the per-seat
   subscriber set that decides who gets frames, and the process-wide census that
   decides whether this instance is near its concurrency ceiling. A reap that
   forgot the census would leave a phantom in the health number, which is the one
   number an operator uses to tell a wedged instance from a busy one."
  [seat batch]
  (let [frame (sse-frame "batch" batch)]
    (sse/push-off-thread!
     (fn []
       (let [subs (get @subscribers seat #{})
             dead (reduce (fn [d ch] (if (send-frame! ch frame) d (conj d ch))) #{} subs)]
         (when (seq dead)
           (swap! subscribers update seat #(apply disj (or % #{}) dead))
           (run! #(sse/deregister! %) dead)))))))

;; --- Session lifecycle ----------------------------------------------------

(defn- evict-cold
  "Pure: keep the store bounded by dropping the least-recently-touched sessions."
  [m]
  (if (<= (count m) max-sessions)
    m
    (let [keep' (->> (sort-by (comp - :touched-at val) m)
                     (take max-sessions)
                     (map key)
                     set)]
      (select-keys m keep'))))

(defn- ensure-session
  "Pure: the session map with `seat` present."
  [m seat now]
  (if (get m seat)
    m
    (evict-cold (assoc m seat (new-session seat now)))))

(defn session-for
  "The session value for `seat`, creating it if needed. Returns the value."
  [seat]
  (let [now (now-ms)]
    (get (swap! sessions ensure-session seat now) seat)))

(defn reset-session!
  "Drop a seat's session (tests, and the surface's own hard reset). The next
   event mints a fresh `:session/id`, which is what makes a stale client's
   POSTs 409 rather than land in a session it does not know about."
  [seat]
  (swap! sessions dissoc seat)
  nil)

(defn- deliver-posts!
  "Deliver the turns this batch's steps ruled `:posted?` to the installed post fn.
   Returns the `utt/id`s that were DELIVERED — the ones the fn returned from
   without throwing — so the caller can advance the dedup ledger on the receipt.

   A SIDE EFFECT OF APPLYING STEPS, and therefore TOTAL. It runs on the thread
   that just applied a client's events — the same position `broadcast!` occupies
   — so a Slack outage, a full queue, or a missing fn must never be able to throw
   into the event handler. It logs and moves on.

   BUT A FAILURE IS NO LONGER A SILENT WRITE-OFF (sol, 2026-07-27). The predecessor
   logged the exception and left `:posted-utts` — already advanced inside the swap
   — claiming the turn had been delivered, so the utterance could never be posted
   again by any path: the reducer's ruling was on the record and Gene's words were
   gone. The ledger now advances HERE, from the return value, so a throwing
   delivery leaves the turn eligible for the client's own upload retry.

   No fn installed is a quiet no-op: a test harness or the reducer lab has no
   outbound queue and must not need one. It reports NOTHING delivered, which is
   the truth — and harmless, because with no queue there is nothing to duplicate."
  [seat posts]
  (if-let [f @post-fn]
    (reduce (fn [acc p]
              (try
                (f seat p)
                (conj acc (:utt/id p))
                (catch Throwable t
                  (log/warn :reducer-session-post-failed
                            :seat seat :utt (:utt/id p) :err (.getMessage t))
                  acc)))
            []
            posts)
    []))

(defn apply-events!
  "Apply reducer events to `seat`'s session: swap, then journal, then POST, then
   broadcast.

   The swap is the ONLY mutation and it is pure; everything after it reads the
   value the swap produced. Returns the new session value."
  [seat events]
  (let [now (now-ms)
        [before after] (swap-vals! sessions
                                   (fn [m]
                                     ;; ensure INSIDE the swap: a concurrent
                                     ;; eviction between a separate ensure and
                                     ;; this update would apply events to nil.
                                     (-> m
                                         (ensure-session seat now)
                                         (update seat ingest events now))))
        session (get after seat)]
    ;; Journal each step against the state that PRECEDED it (see `journal-step!`).
    (reduce (fn [prev-state step]
              (journal-step! seat step prev-state)
              (:state step))
            (get-in before [seat :state])
            (:last-steps session))
    ;; THE POST (scar B59). Keyed on `utt/id` — the swap consults the DELIVERED
    ;; ledger and produces no entry for a turn already on it, so nothing is sent
    ;; twice; and the ledger advances below, from what actually left, so a
    ;; delivery that threw is not written off as done (sol, 2026-07-27).
    (let [delivered (deliver-posts! seat (:last-posts session))]
      (when (seq delivered)
        (swap! sessions (fn [m]
                          ;; total: the seat may have been evicted under us, and a
                          ;; receipt for a session that no longer exists is not a
                          ;; reason to resurrect it.
                          (if (get m seat) (update m seat commit-posted delivered) m)))))
    (when (> (:seq session) (or (get-in before [seat :seq]) 0))
      (broadcast! seat (last (:outbox session))))
    session))

;; --- The clock (D7: expiry rides explicit clock events) -------------------

(defn clock-envelope
  "Pure-ish: a server-origin `:fact/clock` envelope. Built as a real envelope and
   pushed through the same validate-then-convert path as client traffic, so the
   one message the server itself produces is held to the same contract."
  [session now]
  {:protocol       protocol/protocol-version
   :kind           :fact/clock
   :session/id     (:session/id session)
   :event/id       (str "clk-" (:seat session) "-" (.incrementAndGet ^AtomicLong clock-counter))
   :causation/id   (str "tick-" (:seat session))
   :occurred-at-ms now
   :sequence       0})

(defn tick!
  "Advance one session's clock. No-op when the seat has no session."
  [seat]
  (when-let [session (get @sessions seat)]
    (let [now (now-ms)
          env (clock-envelope session now)]
      (when-let [ev (protocol/fact->event env)]
        (apply-events! seat [ev])))))

;; --- Server-side reply ingestion (S5c delta-6) ----------------------------
;;
;; THE BUG THIS EXISTS FOR (Gene, 2026-07-27, bridge3-new's first field turn):
;; the capture half worked end to end — tap, record, send, transcript, connector,
;; Claude answered — and the reply NEVER REACHED THE PAGE. No turn line, no
;; playback, nothing in the conversation. Nothing translated an arriving channel
;; reply into a `:fact/reply-arrived` for the seat's reducer session.
;; (The sentence the page shows for that state is core's to author, not this
;; namespace's — see `status-line`; it is deliberately not quoted here.)
;;
;; `/voice-lab` never showed it because its page POLLS `/api/bridge3/replies` and
;; posts the fact FROM THE CLIENT. `/bridge3-new` is the Datastar game-engine
;; page: it has no polling loop and no client-authored facts by design, so on
;; that surface the reply had no way in at all.
;;
;; Delta 6 is the rule that fixes the class rather than the instance: EVERY reply
;; DISCOVERY TRANSPORT — client poll, SSE, native background discovery, and now
;; the server's own connector seam — submits the same `:fact/reply-arrived`
;; through the reducer, and ONLY the reducer's `:speak` effect reaches playback.
;; A transport is never a second playback engine.
;;
;; So the SERVER authors the fact, exactly the way it already authors
;; `:fact/clock`: a real `marvin.voice/1` envelope through `protocol/explain` and
;; `protocol/fact->event`, held to the same contract as client traffic.

(defn reply-envelope
  "Pure: a server-origin `:fact/reply-arrived` envelope for one reply.

   `:event/id` IS THE REPLY ID, and that is the whole idempotency story. A reply
   must never be ingested twice — not when the connector re-POSTs it, not when a
   watch fires twice, not when two processes see the same enqueue — and the reply
   id is the natural dedup identity because it is the SAME identity the ack path
   (`/api/channel/reply-ack`) and the shadow tap already key on. Deriving the
   event id from it rather than minting a fresh one means `ingest`'s ordinary
   `:seen-events` ledger answers a redelivery with the outcome the first delivery
   got, for free, with no code that knows about replies.

   Two independent windows, as everywhere else in this loop: this ledger, and the
   reducer's own `:seen-reply-ids` (P9). A defect in either degrades to the other
   rather than to a reply spoken twice."
  [session reply-id text now]
  {:protocol       protocol/protocol-version
   :kind           :fact/reply-arrived
   :session/id     (:session/id session)
   :event/id       (str "rpl-" reply-id)
   :causation/id   (str "connector-" (:seat session))
   :occurred-at-ms now
   :sequence       0
   :reply/id       (str reply-id)
   :reply/text     (str text)})

(defn ingest-reply!
  "Submit an arrived reply to `seat`'s LIVE reducer session. Returns the new
   session value, or nil when there was nothing to submit it to.

   ONLY FOR A SESSION THAT ALREADY EXISTS, deliberately — `session-for` would
   CREATE one, and a reply arriving for a seat nobody has open would then mint a
   session, start its clock, and hold state for a surface that does not exist.
   A reply with no live session is not an error and is not queued for later
   either: the reducer's own P8 rule already says what a reply that predates the
   user's intent IS. It is HISTORY. A session created afterwards starts with
   `:seeded? false`, so the first replies it sees are recorded as seen and never
   spoken — which is exactly the zz3 scar (a returning client audio-blasting its
   whole backlog) that P8 exists to prevent. Seeding a NEW session from the
   recent-replies store would re-create that scar under a new name, so this
   ingests forward only.

   TOTAL, and that matters more here than anywhere else in this namespace: this
   is called from a watch on `channel-state`, and a watch fn that throws breaks
   every subsequent `swap!` on that atom — an observer would become an outage.
   It logs and returns nil."
  [seat reply-id text]
  (try
    (when (and (valid-seat? seat) (not (str/blank? (str reply-id))))
      (when-let [session (get @sessions seat)]
        (let [env (reply-envelope session reply-id text (now-ms))]
          (if-let [ev (protocol/fact->event env)]
            (apply-events! seat [ev])
            (do (log/warn :reducer-session-reply-envelope-invalid
                          :seat seat :reply reply-id
                          :detail (pr-str (protocol/explain env)))
                nil)))))
    (catch Throwable t
      (log/warn :reducer-session-reply-ingest-error
                :seat seat :reply reply-id :err (.getMessage t))
      nil)))

;; --- Server-side TYPED SEND (bead 3he) ------------------------------------
;;
;; THE BUG THIS EXISTS FOR (Gene, field, 2026-07-27): he typed, Marvin answered,
;; and the answer showed in bridge3-new's conversation while his own words did
;; not. The say box POSTed to `/api/bridge3/say`, which enqueues the message and
;; appends the LEGACY conversation turn directly — a second product-decision lane
;; beside the reducer's (ARCHITECTURE.md D2), and the page's ledger renders only
;; what the reducer knows.
;;
;; The shape of the fix is `ingest-reply!`'s, exactly: THE SERVER AUTHORS THE
;; ENVELOPE and pushes it through `protocol/explain` + `protocol/fact->event` like
;; any client traffic. The browser sends one thing — the text it owns (Datastar
;; rule 11) — and holds no session identity, no client id, no event id and no
;; protocol version. There is nothing for a page to get wrong.

(defonce ^:private say-counter (AtomicLong. 0))

(defn say-envelope
  "Pure-ish: a server-origin `:command/say` envelope for one typed message.

   `:event/id` IS THE IDEMPOTENCY IDENTITY, and unlike a reply there is no
   natural one to derive from: two identical messages typed a minute apart are
   two messages, not a redelivery. So it is minted here — one per accepted POST —
   and it is what the reducer's `say-utt-id` derives the utterance from, which is
   how a retried POST of the SAME envelope (a proxy replay, a double-submit of one
   request) resolves to one utterance and posts once, while a second press of Send
   is honestly a second message.

   The counter is process-wide and the seat is in the id, so two seats cannot
   collide and a restart cannot reissue an id a live session has already spent."
  [session text now]
  {:protocol       protocol/protocol-version
   :kind           :command/say
   :session/id     (:session/id session)
   :event/id       (str "say-" (:seat session) "-" (.incrementAndGet ^AtomicLong say-counter))
   :causation/id   (str "typed-" (:seat session))
   :occurred-at-ms now
   :sequence       0
   ;; VERBATIM (most-original-form). The reducer trims, bounds and rules; the
   ;; transport does not pre-chew what the user typed.
   :text           (str text)})

(defn ingest-say!
  "Submit a TYPED message to `seat`'s reducer session. Returns `{:ok true :utt/id
   id}`, or `{:error <kw> :detail <string>}` — a value, never an HTTP response:
   what a refusal looks like on the wire is the endpoint's business, and this
   namespace has no opinion about status codes.

   IT CREATES THE SESSION IF THERE IS NONE, and that is the one deliberate
   difference from `ingest-reply!`. A reply arriving for a seat nobody has open is
   HISTORY (P8) and minting a session for it would resurrect the zz3 audio-blast
   scar. A typed message is the opposite in every respect: a human pressed Send a
   half-second ago, on a page that by construction already rendered — so the
   session almost always exists, and on the one path where it does not (a session
   evicted between render and Send), refusing would throw away words a human just
   wrote. NEVER SWALLOW WHAT THE USER SAID (P3) reads the same for a keyboard.

   THE BLANK AND THE BOUND ARE REFUSED HERE *AND* IN THE REDUCER, deliberately.
   The reducer's ruling is the authority — it is what makes the diagnostic visible
   on the surface — but a blank POST should not have to travel through a session
   to be told no, and the endpoint needs an answer to give the browser. Same
   policy value on both sides (`:max-say-chars`), read, never re-authored.

   TOTAL: it is called from an HTTP handler, and a typed message that explodes
   must fail as a refusal the user can see, not as a 500 with no words in it."
  [seat text]
  (try
    (let [t   (str/trim (str text))
          cap (long (or (:max-say-chars policy/current) 0))]
      (cond
        (not (valid-seat? seat))
        {:error :invalid-seat :detail "invalid seat"}

        (str/blank? t)
        {:error :blank :detail "text required"}

        (and (pos? cap) (> (count t) cap))
        {:error :too-long :detail (str "text is " (count t) " characters; the bound is " cap)}

        :else
        (let [session (session-for seat)
              env     (say-envelope session text (now-ms))]
          (if-let [ev (protocol/fact->event env)]
            (do (apply-events! seat [ev])
                {:ok true :utt/id (core/say-utt-id ev)})
            (do (log/warn :reducer-session-say-envelope-invalid
                          :seat seat :detail (pr-str (protocol/explain env)))
                {:error :protocol :detail "say envelope failed validation"})))))
    (catch Throwable t
      (log/warn :reducer-session-say-error :seat seat :err (.getMessage t))
      {:error :internal :detail (str (.getMessage t))})))

(defn- tickable-seats
  "Pure over the two stores: seats with at least one live subscriber and recent
   traffic. A session nobody is watching does not get a thread."
  [sessions' subs' now]
  (vec (for [[seat s] sessions'
             :when (and (seq (get subs' seat))
                        (< (- now (:touched-at s)) idle-session-ms))]
         seat)))

(defonce ^:private ticker
  (delay
    (doto (Thread.
           ^Runnable
           (fn []
             (loop []
               (try
                 (Thread/sleep ^long tick-ms)
                 (let [now (now-ms)]
                   (doseq [seat (tickable-seats @sessions @subscribers now)]
                     (tick! seat)))
                 (catch InterruptedException _ nil)
                 (catch Throwable t
                   (log/warn :reducer-session-tick-error :err (.getMessage t))))
               (recur)))
           "reducer-session-clock")
      (.setDaemon true)
      (.start))))

(defn start-clock!
  "Idempotent: ensure the shared clock thread is running. Called when the first
   SSE subscriber connects, so a server that nobody is talking to has no timer."
  []
  @ticker
  nil)

;; --- HTTP: POST /api/reducer-session/events -------------------------------

(defn handle-events
  "POST ?seat=X, body = marvin.voice/1 envelopes (a JSON array, or
   `{events: [...]}`).

   Fails closed, in this order: unknown seat, unparseable body, empty batch,
   schema violation, non-event kind, session mismatch. Every one of them is a
   400/409 naming the violation — D6 requires unknown privileged kinds, schema
   violations and stale leases to fail 'closed AND visibly', and a 200 that
   quietly dropped an envelope is the failure mode that produces bug reports
   nobody can reproduce.

   (Stale LEASES are the exception that proves the shape: they are not rejected
   here, because the reducer itself must see them to record the diagnostic —
   `core/step` refuses them and appends `:diag/stale-epoch`.)"
  [req]
  (let [seat (seat-of req)]
    (cond
      (not (valid-seat? seat))
      (json-response {:error "invalid seat"} 400)

      :else
      (let [parsed (parse-envelopes (body-string req))]
        (cond
          (:error parsed)
          (json-response {:error (:error parsed)} 400)

          (empty? parsed)
          (json-response {:error "no envelopes in batch"} 400)

          :else
          (if-let [bad (validate-batch parsed)]
            (do (log/info :reducer-session-rejected :seat seat :code (:code bad) :index (:index bad))
                (json-response {:error   "protocol violation"
                                :index   (:index bad)
                                :code    (:code bad)
                                :detail  (:detail bad)
                                :applied 0}
                               400))
            (let [session (session-for seat)
                  claimed (:session/id (first parsed))]
              (if (not= claimed (:session/id session))
                ;; A client from a previous server generation. Told, not
                ;; silently absorbed: its lease epochs and its cursor both
                ;; belong to a session that no longer exists.
                (json-response {:error      "session mismatch"
                                :session/id (:session/id session)
                                :applied    0}
                               409)
                (let [events  (mapv protocol/fact->event parsed)
                      session (apply-events! seat events)
                      {:keys [applied duplicates cursor replay-cursor]} (:last-ingest session)]
                  ;; THE SAME OUTCOME, NAMED (S5c delta-4). A redelivered batch
                  ;; gets the acceptance and the cursor its FIRST delivery got —
                  ;; the client's contract is satisfied without a second
                  ;; application. The `:duplicates` count is reported rather than
                  ;; hidden: a silent absorb is exactly the class of bug D6
                  ;; forbids, and a client whose outbox is retrying everything
                  ;; deserves to be able to see that from the response.
                  (json-response {:ok         true
                                  :accepted   (count events)
                                  :applied    applied
                                  :duplicates duplicates
                                  :cursor     (if (zero? (long (or applied 0)))
                                                (or replay-cursor cursor)
                                                cursor)
                                  :session/id (:session/id session)}
                                 200))))))))))

;; --- HTTP: GET /api/reducer-session/effects (SSE) -------------------------

(defn hello-payload
  "Pure: the frame a client gets on connect — who it is talking to, where the
   cursor is, what it may have missed, and the current view (so a fresh tab
   renders reducer state before it has caused any)."
  [session cursor]
  (let [{:keys [batches lost?]} (batches-after session cursor)]
    {:session/id  (:session/id session)
     :seat        (:seat session)
     :cursor      (:seq session)
     :heartbeat   heartbeat-ms
     :lost?       lost?
     :replayed    (count batches)
     :view        (or (:last-view session) (view session))}))

(defn handle-effects
  "GET ?seat=X&cursor=N -> `text/event-stream` of effect batches.

   On connect: a `hello` frame, then every retained batch after `cursor` (the
   catch-up), then live batches as they are decided, then a comment heartbeat
   every 15 s. Reconnect is therefore ordinary: send the last cursor you
   executed and you are made whole, or told you were not.

   ABSENT `cursor` MEANS 'START ME AT THE HEAD', and that distinction is
   load-bearing rather than a convenience. Catch-up exists for a client
   RESUMING work it was already responsible for; a brand-new page has no lease,
   no recorder, and no obligation to anyone's history — replaying the outbox at
   it would re-execute a `:speak` and the tab would start reading old replies
   aloud on every reload. So: no cursor = new client, head, nothing replayed;
   a cursor (0 included) = resume from exactly there.

   THE STREAM IS ALSO REGISTERED, SUPERSEDED, AND EVENTUALLY RECYCLED. Every
   open stream is one of the 300 concurrent requests this Cloud Run instance is
   allowed to hold, and an SSE stream is a request that never ends, so
   `sse-registry` is told about it at open and told again at close; a page that
   reconnects while its old stream is still draining supersedes itself by its
   `client` id rather than accumulating a second slot. The heartbeat loop below
   consults a rotation deadline on the way past, which is `datastar-kit.sse`'s
   bounded recycle rather than a second timer of ours — the heartbeat is already
   awake every 15 s holding the channel, so it is the cheapest and only place
   that needs to ask.

   ROTATION IS SAFE HERE BY CONSTRUCTION. Every client of this stream is a native
   `EventSource` — `resources/public/js/reducer-session.js` and the voice-lab
   inline engine — and `EventSource` reconnects on its own when the server closes
   the stream. The reconnect carries the `cursor` the client last EXECUTED, so
   the catch-up above replays exactly the batches it missed and the client is
   made whole; a recycle is therefore invisible rather than lossy. It exists
   because heartbeat reaping cannot see the failure that actually wedges us: a
   write to a sleeping phone's socket SUCCEEDS into the kernel send buffer for
   minutes after the peer is gone, so the heartbeat never errors and the slot is
   never returned. Cloud Run's own 300 s timeout is the only other bound on such
   a stream, and it is both later than ours and not ours to choose. The `recycle`
   frame sent just before the close is a courtesy marker for reading logs and
   nothing more; a client that ignores it reconnects on the close anyway."
  [req]
  (let [seat     (seat-of req)
        raw      (or (get-in req [:params :cursor])
                     (get-in req [:query-params "cursor"]))
        resuming (some-> raw str (as-> s (try (Long/parseLong s) (catch Exception _ nil))))]
    (if-not (valid-seat? seat)
      (json-response {:error "invalid seat"} 400)
      ;; The session is ENSURED here (a stream for a seat that has never been
      ;; touched must not race its own creation), but it is READ again inside the
      ;; push action below — reading it here and snapshotting from that reading is
      ;; precisely the gap this file used to have.
      (do
        (session-for seat)
        (http/as-channel
         req
         {:on-open
          (fn [ch]
            (http/send! ch {:status  200
                            :headers {"Content-Type"      "text/event-stream"
                                      "Cache-Control"     "no-cache, no-store"
                                      "Connection"        "keep-alive"
                                      "X-Accel-Buffering" "no"}}
                        false)
            ;; Register BEFORE the first payload byte: supersession is
            ;; close-before-serve, so the stream this page load replaces must be
            ;; gone before this one starts spending the seat's attention.
            (sse/register! {:ch          ch
                            :endpoint    "/api/reducer-session/effects"
                            :seat        seat
                            :client-id   (sse/client-id-of req)
                            :remote-addr (:remote-addr req)
                            :close!      (fn [] (http/close ch))})
            ;; THE SNAPSHOT AND THE SUBSCRIPTION ARE ONE ACTION, ON THE PUSH
            ;; AGENT (sol, 2026-07-27). They used to be three statements on this
            ;; thread: read the session, send hello + catch-up from that reading,
            ;; THEN subscribe — and a batch decided in between reached neither
            ;; stream. Not "arrived late": gone. The old stream was already closed
            ;; by supersession, this one was not yet a subscriber, and the client's
            ;; next reconnect asks from the cursor it last EXECUTED, which the lost
            ;; batch never advanced… but only if it has one at all (see the client's
            ;; hello-cursor adoption). An `:effect/speak` lost that way is a reply
            ;; Gene never hears, with nothing anywhere saying so.
            ;;
            ;; `push-off-thread!` is a SINGLE serialized agent, and `broadcast!`
            ;; is the only other thing that writes batch frames — so ordering it
            ;; through the same queue closes the gap without a lock: a batch
            ;; enqueued BEFORE this action ran before it, so the session read
            ;; below already contains it and the catch-up carries it; a batch
            ;; enqueued after finds this channel already subscribed and delivers
            ;; it live. The one residue is a DUPLICATE (broadcast enqueued between
            ;; the swap and here), and a duplicate is free: the client's cursor
            ;; guard and terminal-effect ledger both absorb it, which is exactly
            ;; the asymmetry an at-least-once transport is designed around.
            ;;
            ;; It also takes the hello/catch-up writes off the http-kit worker —
            ;; the same reason `broadcast!` is there, stated in full on that fn.
            (sse/push-off-thread!
             (fn []
               (let [session (session-for seat)
                     cursor  (or resuming (:seq session))]
                 (when (send-frame! ch (sse-frame "hello" (hello-payload session cursor)))
                   (doseq [b (:batches (batches-after session cursor))]
                     (send-frame! ch (sse-frame "batch" b)))
                   (swap! subscribers update seat (fnil conj #{}) ch)))))
            (start-clock!)
            (future
              (loop []
                (Thread/sleep ^long heartbeat-ms)
                (when (contains? (get @subscribers seat #{}) ch)
                  ;; A NAMED EVENT, not an SSE comment, and the difference is the
                  ;; whole value of the heartbeat. `EventSource` never surfaces a
                  ;; comment line to JavaScript — it keeps the socket warm and
                  ;; tells the CLIENT nothing — so a client watchdog for "the
                  ;; stream went quiet" (client contract RS-C06: absence past
                  ;; 2 x heartbeat-ms is a dead stream) had nothing to observe.
                  ;; One named frame at the same cadence makes that watchdog
                  ;; real for free. The payload is an empty object because the
                  ;; frame's MEANING is its arrival; putting state in it would
                  ;; make it a second, unversioned view channel.
                  ;;
                  ;; BOUNDED RECYCLE rides this same wakeup (see the docstring):
                  ;; a stream past its rotation deadline is closed on our terms,
                  ;; announced with a `recycle` frame, and left to the client's
                  ;; `EventSource` to re-open with the cursor it last executed.
                  (if (sse/rotate-due? ch)
                    (do (send-frame! ch (sse-frame "recycle" {}))
                        (swap! subscribers update seat #(disj (or % #{}) ch))
                        (sse/close! ch :rotation))
                    (if (send-frame! ch (sse-frame "heartbeat" {}))
                      (recur)
                      (do (swap! subscribers update seat #(disj (or % #{}) ch))
                          (sse/deregister! ch))))))))
          :on-close
          (fn [ch _status]
            (swap! subscribers update seat #(disj (or % #{}) ch))
            (sse/deregister! ch))})))))

;; --- HTTP: GET /api/reducer-session/state (diagnostic) --------------------

(defn handle-state
  "GET ?seat=X -> the session's current view + cursor, as JSON.

   Not part of the loop: the SSE stream is how a surface learns state. This is
   for a human with curl (and for a test that wants the view without opening a
   stream)."
  [req]
  (let [seat (seat-of req)]
    (if-not (valid-seat? seat)
      (json-response {:error "invalid seat"} 400)
      (let [session (session-for seat)]
        (json-response {:session/id  (:session/id session)
                        :cursor      (:seq session)
                        :view        (view session)
                        :subscribers (count (get @subscribers seat #{}))}
                       200)))))
