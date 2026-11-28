(ns marvin-voice-remote.bridge3-new
  "/bridge3-new — S5c Phase C1: the ONE new page (D1), built as a DATASTAR
   GAME-ENGINE surface over the reducer session loop.

   Plan v2.2/v2.3/v2.4/v2.5 in one paragraph: the intermediate rung is PTT on
   the reducer, at bridge3's scope, where it has a real comparand and a real
   adoption population; it ships the NEW UI and the storyboard-A status
   vocabulary from birth; and it is built as a Datastar page, not as a
   hand-rolled successor to the legacy pages' client-rendered style.

   THE ARCHITECTURE IN ONE PICTURE — two channels, one brain, zero client
   decisions:

     tap ──command envelope──▶ POST /api/reducer-session/events
                                        │
                                  core/step (THE decision)
                                        │
       ┌────────────────────────────────┴────────────────────────────────┐
       │                                                                 │
     EFFECTS (open-mic, close-mic, speak, cue)              STATE (what is on screen)
       │  SSE /api/reducer-session/effects                    │  SSE /sse/bridge3-new
       ▼                                                      ▼
     the MediaPort executes them                      the SERVER renders HTML and
     (browser-native ops only)                        MORPHS it into the page

   Four properties are load-bearing, and each one is a rule a legacy page broke:

   1. THE SERVER RENDERS EVERYTHING VISIBLE. Status line, turn line, timer
      origin, conversation, player dock, button LABEL and button COMMAND are all
      computed here from `core/status-view` / `core/turn-view` / the session
      view, and pushed as HTML. The client stores nothing a server decision
      reads and writes no status text. The one thing the page hands the client
      is a `data-cmd` attribute saying WHICH command this tap sends — which is
      how tap-to-talk toggles without the client knowing what a bracket is.

   2. R-STATIC (Gene, 2026-07-27). This file emits HTML STRUCTURE ONLY: no
      inline <script>, no <style>, no on*= handler attributes. Every byte of CSS
      and JS lives under `resources/public/`, cache-busted, hashed into the
      asset-manifest golden, and `check-pages` fails the build if an inline
      program ever appears here (`r-static-pages`).

   3. NOTHING HERE DECIDES. Grep this file for a product threshold and you will
      not find one; the only conditionals are layout and rendering. Which
      command the button sends is a pure function of reducer state
      (`next-tap-command`); what the line says is `core/status-view` verbatim.

   4. IT TOUCHES NO EXISTING SURFACE. Additive routes, its own page-state atom,
      its own SSE endpoint. The reducer session loop it subscribes to is the one
      `/voice-lab` already uses; deleting this namespace would change no shipped
      behavior on any other page."
  (:require
   [clojure.data.json :as json]
   [marvin-voice-remote.json :as mjson]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [datastar-kit.ds :as ds]
   [hiccup2.core :as h]
   [marvin-voice-remote.friction-ui :as friction-ui]
   [marvin-voice-remote.reducer-session :as rs]
   [marvin-voice-remote.reducer.core :as core]
   [marvin-voice-remote.sse-registry :as sse]
   [org.httpkit.server :as http]
   [taoensso.timbre :as log]))

;; --- Build identity (D9: identity is READ, never invented) ----------------

(def ^:dynamic *now-ms*
  "THE page's clock, as a 0-arg fn of epoch milliseconds.

   A var for exactly one reason: `check-pages` renders this page into a GOLDEN,
   and a golden that embeds a wall clock is a golden that fails every second.
   Binding it (with `reducer-session/*now-ms*`, the session's own clock) makes
   the whole page a pure function of state. Production never rebinds it."
  #(System/currentTimeMillis))

(defn- resource-line
  [path]
  (try
    (some-> (io/resource path) slurp str/trim not-empty)
    (catch Exception _ nil)))

(def ^:private build-identity
  "{:sha .. :built-at ..} when BOTH halves were baked into the artifact, else
   nil. Same discipline as `reducer-lab`: half an identity is not one, and a
   page that says 'deployed just now' about a build with no recorded identity is
   the exact S13 lie D9 was written against."
  (let [sha (resource-line "build-sha.txt")
        t   (try (some-> (resource-line "build-time.txt") java.time.Instant/parse)
                 (catch Exception _ nil))]
    (when (and sha t) {:sha sha :built-at t})))

(defn- time-ago [^java.time.Instant inst ^java.time.Instant now]
  (let [mins (.toMinutes (java.time.Duration/between inst now))]
    (cond
      (< mins 1)    "just now"
      (< mins 60)   (str mins "m ago")
      (< mins 1440) (str (quot mins 60) "h ago")
      :else         (str (quot mins 1440) "d ago"))))

(defn version-badge
  "Pure: the build chip. A missing identity renders VISIBLY and says so in words
   — never an age, never a sha-shaped placeholder."
  [identity ^java.time.Instant now]
  (if identity
    [:span {:class "b3n-ver-ok"} (str "v" (:sha identity)
                                      " (deployed " (time-ago (:built-at identity) now) ")")]
    [:span {:class "b3n-ver-bad"} "BUILD IDENTITY UNAVAILABLE — no build-sha.txt / build-time.txt"]))

;; --- Page state (server-owned; per seat) ----------------------------------
;;
;; LAYOUT IS SERVER STATE, and that is the whole point of the ruling (Gene,
;; 2026-07-27). A layout toggle held in a JS variable would be a second source
;; of truth the SSE morphs could not see: the next status push would render the
;; layout the server believed in and the screen would flip underneath the user.
;; So the toggle POSTs, the server re-renders the whole fragment in the other
;; layout, and the client makes no layout decision at all — Datastar doctrine
;; applied to the one piece of UI state that is genuinely the user's choice.

;; THE CONVERSATION IS NOT PAGE STATE ANY MORE (Gene, 2026-07-28: "open full
;; should be default"). `:convo-open?` used to live here, and with it a POST route
;; and a link. All three are gone rather than defaulted to true: a toggle whose
;; other position is never the right one is not a control, it is a way to break
;; the page. See `convo-block` for the ruling and for why there is no scroll box
;; either.

(def default-layout
  "TURN LEDGER is the default for the trial (Gene, 2026-07-27: 'so we can see
   the machinery'). Radio Deck — the glanceable one — is behind the toggle and
   becomes the default when the machinery stops being the interesting part."
  :ledger)

(def layouts
  "The closed set, in toggle order. A closed set because the layout name arrives
   from a request parameter and `(keyword)` of user input grows the keyword
   table with garbage (the same rule `reducer-lab` follows)."
  [:ledger :radio])

(def layout-names
  {:ledger "TURN LEDGER" :radio "RADIO DECK"})

(def ^:private max-pages
  "Seat names arrive from a query parameter; cap how many page states are held
   and evict the least-recently-touched, so a scanner cannot grow this atom."
  32)

(defonce pages
  ;; seat -> {:layout <kw> :touched-at ms}
  (atom {}))

(defn new-page-state [now] {:layout default-layout :touched-at now})

(defn page-state-for
  "Pure over the map: the page state for `seat`, created if absent, bounded."
  [m seat now]
  (if (get m seat)
    m
    (let [m' (assoc m seat (new-page-state now))]
      (if (> (count m') max-pages)
        (dissoc m' (key (apply min-key #(:touched-at (val %) 0) (dissoc m' seat))))
        m'))))

(defn next-layout
  "Pure: the layout a toggle moves to. A cycle rather than a boolean flip, so a
   third layout costs one entry in `layouts` and no branching anywhere else."
  [layout]
  (let [i (or (first (keep-indexed #(when (= %2 layout) %1) layouts)) 0)]
    (nth layouts (mod (inc i) (count layouts)))))

(defn page-for [seat]
  (let [now (long (*now-ms*))]
    (get (swap! pages page-state-for seat now) seat)))

;; --- The pure display decisions (all of them) -----------------------------

(defn next-tap-command
  "Pure: WHICH command the next tap on the talk button sends.

   TAP-TO-TALK (plan v2.3, ratified by Gene): tap to start, tap to send. The
   reducer's bracket is unchanged — `:cmd/ptt-start` then `:cmd/ptt-stop`, the
   best-tested region it has — and the TOGGLE is expressed by the server
   rendering the next command into the button rather than by the client
   remembering which half of a bracket it is in. That is the difference between
   a page with state and a page with a label: a reload, a reconnect, or a second
   tab all show the same button because they all render the same reducer state.

   `:hf` returns nil: free mode owns the mic on this session, `handle-ptt-start`
   would refuse the command as `:diag/ignored-command`, and a button that posts a
   command the reducer will refuse is a button that lies. C1 is PTT-only by
   ruling (v2.2: 'never a half-ported hybrid'), so this is the disabled case."
  [view]
  (case (:mode view)
    :ptt :command/ptt-stop
    :hf  nil
    :command/ptt-start))

(defn tap-label
  "Pure: what the talk button SAYS. Derived from the same state as the command it
   carries, so the two can never disagree."
  [view]
  (case (next-tap-command view)
    :command/ptt-stop  "TAP TO SEND"
    :command/ptt-start (if (:speaking view) "TAP TO INTERRUPT" "TAP TO TALK")
    "HANDS-FREE — NOT THIS SURFACE"))

(defn last-reply-id
  "Pure: the newest reply in the display conversation, which is what the dock's
   Replay acts on. Nil when Marvin has not spoken — the button renders disabled
   rather than posting a `command/replay-reply` with no id."
  [view]
  (:reply/id (last (filter #(= :marvin (:who %)) (:convo view)))))

(defn ago-str
  "Pure: how long ago something happened, as a short relative string.

   RELATIVE, not a wall-clock time, and that is not cosmetic: a formatted
   HH:mm:ss carries the SERVER's timezone into a page rendered for a phone in
   another one, and it makes the check-pages golden depend on the machine that
   ran it. An age is true in every zone."
  [at now]
  (cond
    (not (and (number? at) (number? now))) "—"
    (< (- now at) 1000)   "now"
    (< (- now at) 60000)  (str (quot (- now at) 1000) "s ago")
    (< (- now at) 3600000) (str (quot (- now at) 60000) "m ago")
    :else                 (str (quot (- now at) 3600000) "h ago")))

(defn elapsed-str
  "Pure: m:ss since the current state began, or nil when the origin is unknown.

   The ORIGIN is a server stamp (`view :since`, the newest transition row) and
   the value is computed at render time, so the number on screen is true at
   every push. A display clock may tick it between pushes from `data-since` —
   that is browser-owned (a derived display clock over a server timestamp) and
   it is the one piece of ticking this page allows.

   ONE FORMATTER, in `core/duration-str`. The capture length core now names in
   the in-flight status rows is the same m:ss as the timer above it, and two
   copies of `format \"%d:%02d\"` is two things that have to stay identical.
   (The sentences themselves are core's; this page does not quote them, not even
   in a comment — `the-page-authors-no-user-visible-status-string` says so.)"
  [since now]
  (when (and (number? since) (number? now) (>= now since))
    (core/duration-str (- now since))))

;; --- Fragments (pure hiccup over the view) --------------------------------

(defn- cmd-attrs
  "THE TAP CONTRACT with `resources/public/js/bridge3-new.js`.

   An element carrying `data-mv-command` is a reducer COMMAND button. The entry
   delegates ONE click listener over the whole surface, reads the kind (and an
   optional JSON `data-mv-payload`, whose keys are merged into the envelope) off
   the element the user actually activated, and emits it through the
   ReducerSessionClient. It never decides WHICH command — the server already
   did, from reducer state, which is how tap-to-talk toggles without the client
   knowing what a bracket is.

   A control with no `data-mv-command` sends nothing. That is why every disabled
   control here is rendered WITHOUT one rather than with a command the reducer
   would refuse.

   THE ONE NON-COMMAND CONTROL is `data-mv-seek` (see `player-dock`): ±10 s
   inside the reply already playing is media-element mechanics that
   `docs/contracts/playback-port.md` §2 keeps client-side by name ('the seek
   buttons ... stay'), so it moves `currentTime` and touches no reducer state.
   Every OTHER transport control — reply selection and the pause bit — is
   reducer authority and rides `data-mv-command`.

   THE KEY IS A STRING, AND THAT IS THE WHOLE OF BEAD 1hz (Gene, field,
   2026-07-29 15:36 UTC). Every tap of a replay button answered with
   `protocol-violation: :command/replay-reply missing required payload key
   :reply/id` — from the dock as well as from the conversation rows, and on rows
   that plainly HAD ids. Nothing was nil: this line wrote the map with the
   KEYWORD `(keyword \"reply/id\")`, and `clojure.data.json` serializes a keyword
   key by its NAME. The namespace was dropped on the way out — the attribute read
   `{\"id\":\"1.1\"}` — and `reducer-session` read it back with `:key-fn keyword`
   as `:id`, so the one field the command requires never existed on the wire.
   A string key survives verbatim, which is all this ever needed.

   IT WAS INVISIBLE BECAUSE THE PIN ROUND-TRIPPED. The test guarding this built
   its expectation by calling the same `json/write-str` on the same map, so it
   compared the defect against itself and stayed green for the whole life of the
   bug. Its replacement asserts the LITERAL attribute text, and a second test
   runs the rendered payload through `protocol/valid?` — the only two checks that
   could ever have caught it.

   A REQUIRED-PAYLOAD COMMAND WITH NO PAYLOAD IS UNARMED (sol's adversarial pass,
   same day). `:command/replay-reply` is the one kind here whose envelope the
   protocol REFUSES without a field only the caller can supply, and until now this
   fn had no kind-aware check at all: any call site that forgot the id got a
   button carrying a command and no payload — armed, and guaranteed to violate on
   every tap. That made the bug class recurrent by construction rather than a
   mistake at one call site, so the guard lives HERE, where the pairing is known,
   as well as at the call sites that can see whether they have an id."
  ([kind] (cmd-attrs kind nil))
  ([kind reply-id]
   (let [;; the kinds whose payload is not optional. A command in this set with
         ;; nothing to name is not rendered as a command at all.
         unarmed? (and (= :command/replay-reply kind) (nil? reply-id))]
     (cond-> {}
       (and kind (not unarmed?))
       (assoc :data-mv-command (subs (str kind) 1))
       ;; The payload is JSON because the envelope's field is `reply/id` — a
       ;; namespaced key no HTML attribute name can carry. The key is a STRING so
       ;; the namespace reaches the server; see the docstring. `:escape-slash
       ;; false` is not cosmetic either: the default writes `reply\/id`, which
       ;; JSON.parse unescapes correctly but which no human — and no test author
       ;; writing the literal this attribute must contain — reads as the key it is.
       ;; The pin for this bug is an exact-text assertion, so the text has to be
       ;; the obvious one.
       reply-id
       (assoc :data-mv-payload (json/write-str {"reply/id" reply-id} :escape-slash false))))))

;; THE ELAPSED CLOCK GETS THE RECORDING'S PROMINENCE WHILE THE MIC IS THE USER'S
;; (bead dws; Gene, 2026-07-29: "When recording, I'd like to see a timer").
;;
;; The number was always on the page — and at 13px of dim monospace it answered a
;; question nobody could find. Nothing about the clock itself changes below: the
;; same `data-since` stamp, the same server clock domain, the same ticker in
;; `display-clock.js`, which is adjudicated-frozen and stays a ticker. What
;; changes is SIZE, and only while `:you` owns the turn — because that is exactly
;; the interval in which elapsed time stops being trivia about the session and
;; becomes the LENGTH OF THE RECORDING, the one number a person wants mid-sentence.
;;
;; THE SERVER DECIDES WHEN IT IS BIG. `core/turn-view` is already the grouping the
;; whole surface reads (`:you` covers a mic that is hot AND a mic only REQUESTED —
;; P4: an opening mic is the user's turn arriving), so this adds no state and no
;; second reading of `:hf :phase`. A client-side "am I recording" flag would be
;; that second reading, and a page that holds one is the page this one exists not
;; to be.
;;
;; Both layouts carry it, for the reason both layouts carry every control: they
;; differ in emphasis, never in what is on screen.

(defn turn-block
  "Storyboard B: the huge turn line plus the fine state under it."
  [view now]
  (let [{:keys [turn text]} (:turn view)]
    [:div {:id "b3n-turnblock" :class (str "b3n-turnblock b3n-turn-" (name (or turn :standby)))}
     [:div {:id "b3n-turn" :class "b3n-turn"} text]
     [:div {:class "b3n-fine"}
      [:span {:class "b3n-fine-icon"} (:status-icon view)]
      [:span {:id "b3n-fine-text" :class "b3n-fine-text"} (:status-text view)]
      ;; ONE LINE, deliberately — `the-stylesheet-covers-the-rendered-structure`
      ;; harvests class names out of this file with a line-scoped regex, so a
      ;; class wrapped onto its own line is a class that silently stops being
      ;; checked for a rule.
      [:span {:id "b3n-timer" :class (str "b3n-timer" (when (= :you turn) " b3n-timer-live")) :data-since (str (:since view))}
       (elapsed-str (:since view) now)]]]))

(defn status-card
  "Storyboard A: one big line, the system's own voice, and the elapsed clock."
  [view now]
  [:div {:id "b3n-statuscard" :class "b3n-statuscard"}
   [:div {:class "b3n-status-line"}
    [:span {:class "b3n-status-icon"} (:status-icon view)]
    [:span {:id "b3n-status-text" :class "b3n-status-text"} (:status-text view)]]
   ;; ONE LINE — see `turn-block`'s note on the class-name harvest.
   [:div {:id "b3n-timer" :class (str "b3n-timer" (when (= :you (:turn (:turn view))) " b3n-timer-live")) :data-since (str (:since view))}
    (elapsed-str (:since view) now)]])

(defn transition-tape
  "Storyboard B's live tape: the last few things the surface SAID, newest first,
   with the server clock time each one began.

   This is the reducer's thinking made visible (Gene, 2026-07-27) — and it is
   only honest because the rows are the rendered sentences, not internal phases:
   a tape of `:hf :phase` values would show a machine nobody was ever shown.

   THE RECEIPT ROW (Gene, live trial: 'no visual indication of any receipt from
   server'). A row carrying `:heard` is not a state change at all — it is the
   session's ACKNOWLEDGEMENT that a command envelope arrived and was accepted,
   stamped by `reducer-session` at ingest. It renders distinctly (`✓`, its own
   class, and the command's own name) because it answers a different question
   than the rest of the tape: not 'what is the machine doing' but 'did my tap
   reach it'. A tap whose receipt never appears is a tap that was lost, and that
   is now visible instead of inferred. Rows without `:heard` render exactly as
   they always have.

   IT SHOWS IN BOTH LAYOUTS. The receipt is the whole point, so Radio Deck gets
   the tape too — compact (fewer rows, tighter type), never absent. A layout that
   hid the receipt would be a layout where a lost tap looks like a working one."
  ([view now] (transition-tape view now false))
  ([view now compact?]
   [:div {:id "b3n-tape" :class (str "b3n-tape" (when compact? " b3n-tape-compact"))}
    [:div {:class "b3n-tape-head"} "recent state"]
    (let [rows (reverse (take-last (if compact? 2 3) (:transitions view)))]
      (if (seq rows)
        (for [r rows]
          (let [heard  (:heard r)
                stale? (:stale? r)
                named  (when heard (if (keyword? heard) (subs (str heard) 1) (str heard)))]
            ;; ONE LINE, deliberately: `the-stylesheet-covers-the-rendered-structure`
            ;; harvests class names out of this file with a line-scoped regex, so a
            ;; class wrapped onto its own line is a class that silently stops being
            ;; checked for a rule.
            [:div {:class (str "b3n-tape-row" (when (and heard (not stale?)) " b3n-tape-heard") (when stale? " b3n-tape-stale"))}
             [:span {:class "b3n-tape-at" :data-at (str (:at r))} (ago-str (:at r) now)]
             [:span {:class "b3n-tape-icon"}
              (cond stale? "⊘" heard "✓" :else (get-in r [:status :icon]))]
             [:span {:class "b3n-tape-text"}
              (cond
                ;; THE STALE RECEIPT (sol 4.2). A tap authored on a render the
                ;; session has already moved past is REFUSED — and it says so on
                ;; the same tape the accepted ones appear on, because a refusal
                ;; the user cannot see is indistinguishable from a tap that was
                ;; lost, which is the failure this whole row exists to close.
                stale? (str "stale tap ignored: " named)
                heard  (str "heard: " named)
                :else  (get-in r [:status :text]))]]))
        [:div {:class "b3n-tape-row b3n-muted"} "nothing yet"]))]))

(defn talk-button
  "The one control this surface exists for. Disabled state, label and command
   all come from the same pushed view."
  [view]
  (let [cmd (next-tap-command view)]
    [:button (merge {:id "b3n-talk"
                     :class (str "b3n-talk"
                                 (when (= :command/ptt-stop cmd) " b3n-talk-live"))
                     :type "button"}
                    (cmd-attrs cmd)
                    (when-not cmd {:disabled "disabled"}))
     (tap-label view)]))

(defn- dock-btn
  "Pure: one transport button, rendered from a command that may be nil.

   The nil case is the whole reason this exists: a control with no command is
   rendered `disabled` AND without `data-mv-command`, per `cmd-attrs`' standing
   rule. There is no third state where a button looks live and posts nothing."
  [id label title cmd & [reply-id]]
  [:button (merge {:id id :class "b3n-dock-btn" :type "button" :title title}
                  (if cmd (cmd-attrs cmd reply-id) {:disabled "disabled"}))
   label])

(defn- capture-btn
  "Pure: one manual-turn button (OVER or CANCEL), rendered from a command that
   may be nil — same standing rule as `dock-btn`: a nil command is `disabled`
   AND carries no `data-mv-command`, never a live-looking button the reducer
   would refuse (P4). It wears `b3n-capture-btn` so CSS can style the turn
   controls apart from the transport dock."
  [id label title cmd]
  [:button (merge {:id id :class "b3n-capture-btn" :type "button" :title title}
                  (if cmd (cmd-attrs cmd) {:disabled "disabled"}))
   label])

(defn capture-controls
  "The manual TURN controls — OVER and CANCEL — for the phone-in-a-pouch case
   where speaking the `:end-words`/`:cancel-words` keyword is not an option.
   They sit in the CAPTURE zone WITH the talk button (not the player dock, which
   is playback transport), so ending or scrapping a turn is within the same
   thumb reach as starting one.

   ARMED-NESS IS THE REDUCER'S RULING, rendered — `core/capture-view`'s
   `:can-over?`/`:can-cancel?` (D2). OVER is live when there is a turn to post (a
   speaking capture or a stash); CANCEL is live when there is anything to discard.
   When neither can act the button is disabled and silent, exactly as the dock's
   are, so a tap can never post a command `handle-over`/`handle-cancel` would
   no-op."
  [view]
  (let [c (:capture view)]
    [:div {:class "b3n-capture-row"}
     (capture-btn "b3n-over" "✔ OVER" "Send this turn now"
                  (when (:can-over? c) :command/over))
     (capture-btn "b3n-cancel" "✕ CANCEL" "Discard this turn"
                  (when (:can-cancel? c) :command/cancel))]))

(defn player-dock
  "The transport strip — the FULL legacy set (Gene: 'all buttons from legacy;
   they're all important'), in one row, rendered DIRECTLY UNDER the talk button
   in both layouts so every control is within thumb reach.

   THE DISPOSITION RULING — why two different kinds of control share one strip:

   REPLY SELECTION (⏮ prev, ⏭ next, ⏭ latest, ↺ replay, ✕ skip) and THE PAUSE
   BIT (⏯) are REDUCER AUTHORITY. They choose WHICH reply is current and whether
   it is playing, which is product state, not media mechanics —
   `docs/contracts/playback-port.md` §2 puts them on the reducer's side of the
   line by name: 'the queue, the dedup, the autoplay decision, the pause bit, the
   retry policy, the status text, the history cursor, and every DOM-scrape
   transport go.' So each is a server-rendered `data-mv-command` control: the tap
   posts an envelope, `core/step` decides, and the resulting speak / stop-speech
   effects ride the EFFECTS SSE to the MediaPort. This page never pauses audio
   and never picks a reply.

   WITHIN-AUDIO SEEK (⏪ / ⏩, ±10 s) is MEDIA-ELEMENT MECHANICS, and it is the
   ONE transport control the MediaPort performs directly. The same contract
   paragraph keeps it client-side explicitly: 'the media object, the gesture
   window, the Web Audio graph, the cue synthesis, the clipboard, THE SEEK
   BUTTONS, and the telemetry stay.' A seek moves `currentTime` inside the reply
   already playing — it selects nothing, changes no reducer state and produces no
   event — so these two carry NO `data-mv-command` at all. They carry
   `data-mv-seek` (a signed offset in seconds), and they are NEVER disabled: with
   no audio loaded a seek is already a no-op at the media element, which is a
   truthful outcome rather than a refused command.

   SKIP IS NO LONGER THE NAMED GAP. bridge4-chrome §8 recorded that
   `marvin.voice/1` had no `command/skip`, so this dock shipped it visible,
   disabled and labelled with the gap rather than as client-side queue surgery.
   That gap is CLOSED: skip is a reducer command now, so the button carries it
   like every other selection control and the `data-protocol-gap` marking is
   gone. The chrome was kept through the gap exactly so this day cost one line.

   ENABLED/DISABLED IS NOT DECIDED HERE. `core/transport-view` decides it and the
   session carries it on the view under `:transport`:
   `{:paused? :can-prev? :can-next? :can-latest? :can-replay? :can-skip?
     :history-count :cursor}`. A false flag — or an ABSENT `:transport`, which
   means the same thing (the reducer has not said yes) — renders that control
   without a command. Nothing here invents a default.

   REPLAY IS THE ONE CONTROL WITH AN AUTHORITY OF ITS OWN, and it is unchanged:
   its subject is a `reply/id` read from the conversation ledger
   (`last-reply-id`), because `command/replay-reply` REQUIRES one and no flag can
   conjure an id the page does not have. `:can-replay?` ('history exists') and
   'the ledger holds a Marvin reply' are the same condition observed from two
   sides, so consulting the flag as well would add no truth — only a second way
   for the two to disagree."
  [view]
  (let [t      (:transport view)
        rid    (last-reply-id view)
        muted? (:muted? view)
        q      (or (:queue view) 0)]
    [:div {:id "b3n-player" :class "b3n-player"}
     ;; MEDIA MECHANICS — no command, never disabled (playback-port.md §2).
     [:button {:id "b3n-seek-back" :class "b3n-dock-btn" :type "button"
               :title "Back 10 seconds" :data-mv-seek "-10"}
      "⏪"]
     (dock-btn "b3n-prev" "◀" "Previous reply"
               (when (:can-prev? t) :command/prev-reply))
     ;; PAUSE HOLDS, PLAY CONTINUES (Gene, 2026-07-28). The label is two glyphs
     ;; rather than one shared `⏯` because the two states now mean different
     ;; things to the audio — hold it where it is, pick it up from there — and a
     ;; single ambiguous glyph was readable as "toggle" right up until the message
     ;; restarted from the top. The COMMAND is unchanged and still the reducer's:
     ;; the button renders whichever one `:paused?` says is next.
     (dock-btn "b3n-playpause"
               (if (:paused? t) "▶ Resume" "⏸ Pause")
               (if (:paused? t) "Continue where it stopped" "Hold playback here")
               (when t (if (:paused? t) :command/resume :command/pause)))
     (dock-btn "b3n-next" "▶" "Next reply"
               (when (:can-next? t) :command/next-reply))
     [:button {:id "b3n-seek-fwd" :class "b3n-dock-btn" :type "button"
               :title "Forward 10 seconds" :data-mv-seek "10"}
      "⏩"]
     (dock-btn "b3n-latest" "⏭" "Jump to the latest reply"
               (when (:can-latest? t) :command/latest-reply))
     (dock-btn "b3n-replay" "↺ Replay" "Replay Marvin's last reply"
               (when rid :command/replay-reply)
               rid)
     (dock-btn "b3n-skip" "✕ Skip" "Skip the current reply"
               (when (:can-skip? t) :command/skip))
     [:button (merge {:id "b3n-mute" :class "b3n-dock-btn" :type "button"}
                     (cmd-attrs (if muted? :command/unmute :command/mute)))
      (if muted? "🔊 Unmute" "🔇 Mute")]
     [:span {:id "b3n-queue" :class "b3n-queue"}
      (str q " queued")]]))

(defn convo-row
  "One conversation row: who said it, what was said, and what can be done to it.

   THE ▶ NEEDS AN ID, AND A ROW MAY NOT HAVE ONE (bead 1hz; Gene, field,
   2026-07-29 15:36 UTC). Every tap of a per-message replay button after a reload
   produced `protocol-violation: :command/replay-reply missing required payload
   key :reply/id`, the session went degraded, and the whole surface felt frozen.
   The row was a Marvin row with a nil `:reply/id`; `cmd-attrs` then emitted
   `data-mv-command` with NO `data-mv-payload`, which is an ARMED button whose
   envelope the protocol is obliged to refuse — every tap a fresh violation.

   So the button is rendered only when its subject exists. That is the same rule
   `player-dock` already obeys for the rest of the transport ('A false flag ...
   renders that control without a command. Nothing here invents a default'), and
   the dock's own replay states the id half of it outright: `command/replay-reply`
   REQUIRES a reply id and no flag can conjure one the page does not have. A
   Marvin row with no id gets no replay affordance, which is a control that is
   honestly absent rather than one that is present and poisoned.

   WHY THE ROWS LACK AN ID IS A SEPARATE QUESTION and is under investigation; this
   is the half that stops the page from minting invalid envelopes about it."
  [row]
  (let [who   (name (or (:who row) "you"))
        mine? (= "you" who)]
    [:div {:class (str "b3n-msg b3n-msg-" who)}
     [:div {:class "b3n-msg-who"} (if mine? "you" "marvin")]
     [:div {:class "b3n-msg-text"} (:text row)]
     [:div {:class "b3n-msg-actions"}
      (when (and (not mine?) (:reply/id row))
        [:button (merge {:class "b3n-msg-btn" :type "button" :title "Replay this reply"}
                        (cmd-attrs :command/replay-reply (:reply/id row)))
         "▶"])
      ;; KEEP-AS-IS (bridge4-chrome #16): copy is browser-native and rides a user
      ;; gesture. The page marks WHAT to copy; the entry does the clipboard call.
      [:button {:class "b3n-msg-btn" :type "button" :title "Copy"
                :data-mv-copy (str (:text row))}
       "⧉"]]]))

(defn convo-block
  "The conversation — ALL OF IT, always (Gene, 2026-07-28: 'open full should be
   default'; 'simpler is better; no additional scrollbar').

   THERE IS NO COLLAPSED MODE AND NO TOGGLE, and deleting them is the fix rather
   than a side effect of it. This block used to show the LAST TWO rows by default
   behind an `open full` link, which meant the surface's default answer to 'what
   have we said?' was two lines and a button — on the one page whose whole job is
   to be read at arm's length. Two of the three states the toggle could be in were
   wrong for Gene, and the third needed a tap to reach.

   THE LEDGER'S OWN BOUND IS THE TRUNCATION POLICY, and it is the only one. The
   session keeps 24 turns (`reducer-session/max-convo`) because a remote's log is
   a glance surface and an unbounded one is a leak (S7). A SECOND cap here — the
   old last-12 — was a display quietly deciding to show less than the server had
   decided to keep, in a file whose first rule is that nothing here decides.

   NO SCROLL CONTAINER EITHER (Gene's amendment, same day). A `max-height` +
   `overflow-y` box would put a second scrollbar inside a page that already
   scrolls, and on a phone the inner one is the harder of the two to grab. The
   page flows; 24 bounded rows is a page, not a hunt. What makes that safe is the
   ORDER of `shell-body`: talk button, transport dock and say row are all rendered
   ABOVE this block, so the controls stay in thumb reach no matter how long the
   conversation gets — the ergonomics live in the layout, not in a clipped box.

   NEWEST FIRST (Gene, live trial). Only the render ORDER is reversed, which keeps
   this a display decision and not a second query. It makes the page consistent
   with itself: the transition tape already reads newest-first, so a conversation
   reading oldest-first put the two live things on a phone screen — the newest
   reply and the newest state — at opposite ends of it. On a surface read at arm's
   length by a thumb, the latest message belongs at the top, where the eye already
   is."
  [view _layout]
  (let [shown (reverse (vec (:convo view)))]
    [:div {:id "b3n-convo" :class "b3n-convo"}
     [:div {:class "b3n-convo-head"}
      [:span "conversation"]]
     (if (seq shown)
       (for [r shown] (convo-row r))
       [:div {:class "b3n-msg b3n-muted"} "nothing said yet"])]))

(defn alerts-block
  "Exceptions as STATES (bead 9zu): a capture that never reached Marvin is listed
   here for as long as it is unrecovered, next to the status row that counts it.
   `core/upload-failures` is the authority — a count in a line cannot say WHICH
   capture, and recovery reads the list."
  [view]
  [:div {:id "b3n-alerts" :class "b3n-alerts"}
   (for [f (:upload-failures view)]
     [:div {:class "b3n-alert"}
      (str "⚠ not sent — " (:utt/id f)
           " · " (name (or (:error/kind f) :unknown))
           (when-let [s (:http/status f)] (str " " s))
           " · attempt " (:attempt f)
           ;; WHAT was saved, in the units the status line uses — same
           ;; measurement, same formatter (`core/upload-failures` serves it), so
           ;; the row and the line can never disagree about one capture.
           " — audio saved" (when-let [c (:clip f)] (str " (" c ")")))])])

(defn shell-body
  "Pure: the ENTIRE dynamic surface, in one fragment.

   ONE morph target rather than a dozen, deliberately. Datastar morphs by id and
   leaves unchanged nodes alone, so a status push does not disturb the
   conversation's scroll or the focus ring; and one fragment means one ordering,
   which is what makes a LAYOUT SWITCH atomic — every element arrives already
   agreeing about which layout it is in. (Ratchet BC-V02/V03: a status render
   must never reset scroll, remount, or navigate.)

   BOTH LAYOUTS KEEP THE FULL CONTROL SET (Gene, 2026-07-27): the talk button,
   the full transport dock, the conversation, the transition tape, the alerts,
   the build indicator and the layout toggle are rendered in both. They differ in
   EMPHASIS and ORDER, never in capability, and never in wording — both read the
   same `status-view`.

   THE DOCK SITS DIRECTLY UNDER THE TALK BUTTON (Gene, live trial: 'always within
   thumb reach') in both layouts. It used to be last, above the foot, where a
   long conversation could push it off the bottom of a phone screen — so the
   controls a user reaches for WHILE Marvin is talking were the ones furthest
   from the thumb that was already on the talk button. Order here is the whole
   ergonomic decision, which is why it is one list in one fragment: talk, then
   transport, then the exceptions, then the log."
  [view layout now]
  (let [ledger? (= :ledger layout)]
    [:div {:class (str "b3n-deck b3n-layout-" (name layout))}
     (if ledger?
       [:div {:class "b3n-head"}
        (turn-block view now)
        (transition-tape view now)]
       [:div {:class "b3n-head"}
        (status-card view now)
        ;; compact, but PRESENT: the receipt row is why the tape exists in a
        ;; glance layout at all (see `transition-tape`).
        (transition-tape view now true)])
     [:div {:class "b3n-stage"}
      (talk-button view)
      (capture-controls view)]
     (player-dock view)
     ;; TYPED SEND (Gene, 2026-07-27: "I need a place to type stuff!!!") —
     ;; chrome inventory KEEP #17. The input's VALUE is browser-owned data
     ;; (Datastar rule 11 allows it in a POST body); say-box.js posts it and
     ;; forgets, and the reply returns through the reducer's reply ingress like
     ;; every other reply. The page decides nothing.
     ;;
     ;; IT POSTS TO THE REDUCER'S OWN LANE NOW (bead 3he). It used to post to
     ;; `/api/bridge3/say`, whose handler enqueues the message and appends the
     ;; legacy conversation turn DIRECTLY — no command, no reducer decision. That
     ;; is the second product-decision lane D2 forbids, and Gene met it in the
     ;; field on 2026-07-27: he typed, Marvin answered, and only the ANSWER showed
     ;; up in the conversation below, because this page's ledger renders what the
     ;; REDUCER knows and the reducer was never told. `/api/bridge3-new/say`
     ;; authors a `:command/say` envelope and ingests it through the ordinary
     ;; session path, so a typed turn and a spoken one are the same thing from
     ;; here on down.
     [:div {:class "b3n-say-row"}
      [:input {:id "b3n-say" :class "b3n-say" :type "text"
               :placeholder "…or type to bridge" :autocomplete "off"
               :data-mv-say-input "true"}]
      [:button {:id "b3n-say-send" :class "b3n-dock-btn b3n-say-btn"
                :type "button" :data-mv-say "/api/bridge3-new/say"
                :data-mv-seat (or (:seat view) "bridge")}
       "Send"]]
     (alerts-block view)
     (convo-block view layout)
     (friction-ui/note-view (friction-ui/effective-state :bridge3-new))
     [:div {:id "b3n-foot" :class "b3n-foot"}
      ;; NOTE: `shell` (below) is what carries the render epoch and the server
      ;; clock; this fragment is its body and stamps neither.
      ;; bridge4-chrome #26 (seat, KEEP-AS-IS) and #19 (Config link, KEEP-AS-IS
      ;; until /bridge5 is resolved). The PTT/HF surface links are deliberately
      ;; NOT here: this page IS the PTT surface (C1), and HF is C2 work — a link
      ;; to a half-ported hybrid is the thing v2.2 forbids.
      [:div {:class "b3n-context"}
       [:span "session: " [:b (:seat view)]]
       [:a {:class "b3n-link" :href "/bridge5"} "Config"]]
      [:button {:id "b3n-layout" :class "b3n-link-btn" :type "button"
                :data-star-on:click (str "postJSON('/api/bridge3-new/layout?seat="
                                         (:seat view) "',{})")}
       (str "layout: " (get layout-names layout (name layout))
            " → " (get layout-names (next-layout layout)))]
      [:span {:id "b3n-ver" :class "b3n-ver"}
       (version-badge build-identity (java.time.Instant/ofEpochMilli (long now)))]]]))

(defn shell
  "THE MORPH TARGET, rendered by exactly one function so the page and the stream
   cannot disagree about it (sol finding 2.3).

   THE DEFECT THIS CLOSES. The page emitted `<main id=\"b3n-shell\"
   class=\"b3n-shell\">` and the SSE fragment emitted `<div id=\"b3n-shell\">`
   around the same body — a different TAG and no class. Datastar's default
   outer-morph replaces the element it matched, so the very first push swapped
   the `<main>` for a `<div>`, dropped `b3n-shell`'s styling, and turned every
   subsequent 'morph in place' into a full outer replacement of the exact node
   the ratchet (BC-V02/V03) calls stable. One function, one element, both
   channels: the only way this can drift again is by editing it here.

   IT CARRIES THE TWO STAMPS THE WHOLE SURFACE SHARES:

   `data-mv-epoch` — the render cursor (`view :epoch`). Every control inside is
   tapped ON this render, so the epoch belongs to the render, not to each
   button: the entry walks up to the nearest declaring ancestor (it already does
   exactly that for `data-mv-command`) and echoes it back as `:ui/epoch`. That is
   what lets `reducer-session` refuse a SECOND tap on a superseded surface,
   visibly, without the page ever deciding anything (sol 4.2).

   `data-server-now` — the server's clock at render time. `display-clock.js`
   ticks `[data-since]` / `[data-at]` between pushes, and until now it ticked
   them against `Date.now()` on a phone whose clock has no obligation to agree
   with Cloud Run's: a device 40 s fast showed `0:40` the instant a capture
   began. The offset is computed from this stamp on every tick, so the derived
   clock is anchored in the SAME domain as the timestamps it renders (sol 2.4)."
  [view layout now]
  [:main {:id              "b3n-shell"
          :class           "b3n-shell"
          :data-mv-epoch   (str (:epoch view))
          :data-server-now (str (long now))}
   (shell-body view layout now)])

;; --- Rendering the whole page ---------------------------------------------

(defn render-view
  "The display value for one seat: the session view plus the two things the
   FRAGMENT needs that the view proper does not carry — the seat (for its POST
   urls) and the render EPOCH. Read, never computed.

   THE EPOCH IS THE SESSION'S OWN CURSOR (`:seq`), and it is deliberately NOT
   part of `rs/view`: `ingest` decides whether to bump `:seq` by asking whether
   the view CHANGED, so a view that contained the seq would differ from its
   predecessor on every single tick and the 4 Hz clock would become a 4 Hz HTML
   push. It is read here, one layer out, where that circularity cannot form."
  [seat]
  (let [session (rs/session-for seat)]
    (assoc (rs/view session) :seat seat :epoch (:seq session))))

(def state-stale-after-ms
  "How long the page may go without a state-feed heartbeat before it says so
   (A51). DERIVED from the heartbeat cadence, never chosen: at exactly twice the
   interval, one dropped frame is tolerated and two are not — the smallest bound
   that does not fire on ordinary jitter. Tying it to `rs/heartbeat-ms` is the
   point: moving the cadence moves the threshold, and no client-side constant can
   fall out of step with it."
  (* 2 rs/heartbeat-ms))

(defn page-html
  "The FULL page. HTML structure only — R-STATIC (Gene, 2026-07-27): zero inline
   <script>, zero <style>, zero on*= handler attributes. `check-pages` asserts
   this on every run (`r-static-pages`), and the asset manifest pins the SHA-256
   of every file linked below, so behavior-bearing bytes cannot drift without a
   deliberate golden acceptance (delta 8)."
  [seat view page]
  (let [now    (long (*now-ms*))
        v      (str "?v=" now)
        layout (:layout page)
        ;; ONE identity per page LOAD, minted here and handed to both of this
        ;; page's streams. It is what lets the server recognise a reconnect —
        ;; `sse-registry/register!` closes the stream it replaces rather than
        ;; letting the abandoned one keep a Cloud Run concurrency slot — and a
        ;; per-load id (not a per-seat or per-IP one) is deliberate: two tabs on
        ;; one seat must coexist rather than evict each other.
        client (sse/new-client-id)]
    (str
      "<!DOCTYPE html>"
      (h/html
        [:html {:lang "en"}
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
          [:meta {:name "robots" :content "noindex"}]
          [:title (str "Marvin · " seat)]
          [:link {:rel "stylesheet" :href (str "/css/bridge3-new.css" v)}]
          [:script {:type "module" :src (str "/vendor/datastar-aliased.js" v)}]
          [:script {:src (str "/js/datastar-kit.js" v) :defer "defer"}]
          ;; The two ports and the entry, in dependency order (the entry reads the
          ;; `MarvinMediaPort` / `MarvinReducerSession` globals the first two
          ;; define). `defer` preserves that order and runs them after the document
          ;; is parsed, while keeping `document.currentScript` valid — which is
          ;; where the entry reads its boot record from.
          [:script {:src (str "/js/media-port.js" v) :defer "defer"}]
          [:script {:src (str "/js/reducer-session.js" v) :defer "defer"}]
          ;; The display clock is its OWN file with its own pinned negative
          ;; space (ticks [data-since]/[data-at] textContent, nothing else) —
          ;; kept out of bridge3-new.js so the entry's zero-DOM pins stay
          ;; absolute. See display-clock.js's header.
          [:script {:src (str "/js/display-clock.js" v) :defer "defer"}]
          ;; say-box: its OWN file with its own pinned negative space (typed
          ;; send — the entry's one-element rule stays absolute).
          [:script {:src (str "/js/say-box.js" v) :defer "defer"}]
          ;; THE BOOT RECORD lives on the entry's own <script> tag, because that is
          ;; where the entry reads it (`document.currentScript.dataset`) — delta 9:
          ;; a module that self-boots from its own attributes needs no boot line in
          ;; the HTML, and there is nowhere for one to hide. Every URL the client
          ;; may talk to is DECLARED here, as data: the ports invent no endpoint.
          [:script {:src            (str "/js/bridge3-new.js" v)
                    :defer          "defer"
                    :data-seat      seat
                    :data-build     (str (:sha build-identity))
                    ;; The SAME id the state channel's url carries below. The entry
                    ;; falls back to minting its own when this is absent, and two
                    ;; minted ids would split one page into two unrelated client
                    ;; identities — the effects stream would then never be
                    ;; superseded by its own reconnect.
                    :data-client-id client
                    ;; THE THIN CAPTURE PORT (scar B59), not the legacy dictate
                    ;; endpoint. `/api/bridge3/dictate` transcribes AND decides —
                    ;; noise filter, OVER sign-off, partials stash, strip, post —
                    ;; every one of which `reducer/core` now owns. Two authorities
                    ;; for one decision is what this URL change removes.
                    ;; INTENT: THIN-CAPTURE-PORT-B59
                    :data-dictate-url  "/api/bridge3-new/capture"
                    :data-tts-url      "/api/bridge/tts"
                    :data-clientlog-url "/api/voice-lab/clientlog"
                    ;; F1 (fable's adversarial review): under the native shell the
                    ;; browser port must not interpose in the capture path. C1 is
                    ;; the WEB lane — declared false, never sniffed.
                    :data-native-capture "false"}]]
         [:body {:class "b3n-body"}
          ;; The Datastar STATE channel: server-rendered HTML morphs.
          ;;
          ;; `retry:'always'` IS LOAD-BEARING, and it is what makes server-side
          ;; rotation survivable. Datastar's fetch-based SSE defaults to
          ;; `retry:'auto'`, and reading the vendored
          ;; `resources/public/vendor/datastar-aliased.js` shows that `auto` only
          ;; re-opens a request that ERRORED — a stream the SERVER ends cleanly
          ;; looks like a completed response and is never re-opened. The registry
          ;; recycles every stream at its 240–270 s deadline by closing it
          ;; cleanly, so with `auto` this page would go permanently deaf about four
          ;; minutes after load. `retryInterval:3000` is the reconnect delay after
          ;; a clean recycle, and it doubles as a floor on how fast a client
          ;; re-attempts while the service is 429ing — Datastar does not back that
          ;; path off on its own.
          [:div {:id "b3n-sse" :class "b3n-hidden"
                 :data-star-init (str "@get('/sse/bridge3-new?seat=" seat "&client=" client "', "
                                      "{retry:'always',retryInterval:3000})")}]
          ;; THE DISCONNECT BANNER, outside the morph target on purpose.
          ;;
          ;; It lives in <body> as a SIBLING of #b3n-shell because the state
          ;; channel replaces #b3n-shell wholesale on every push: anything inside
          ;; it is re-rendered from server state, and the one thing the server
          ;; cannot render is that the client stopped hearing from the server. So
          ;; this element is emitted ONCE, by the page, and never morphed.
          ;;
          ;; It is inert HTML. The entry toggles exactly one attribute on it —
          ;; `hidden` — from the SSE connection state it already tracks. That is
          ;; the narrowest possible exception to 'the client makes no display
          ;; decision', and it is the only honest one: connection liveness is a
          ;; browser fact the server is by definition unable to observe. The words,
          ;; the styling and the position are all decided here.
          [:div {:id "b3n-offline" :class "b3n-offline" :hidden "hidden"}
           "⚡ disconnected — reconnecting"]
          ;; THE PROTOCOL-VIOLATION RING (sol 2026-07-29, bead q8t). A sibling of
          ;; the banner above for the same structural reason, but it reports a
          ;; DIFFERENT failure: the stream is healthy, frames keep arriving, and
          ;; the only thing that stopped is that an envelope this page sent was
          ;; refused. The offline banner cannot cover it — it is shown only when
          ;; the transport is DOWN — so the violation was invisible and the page
          ;; read as simply frozen (Gene's 15:36 field report: every replay tap
          ;; refused, nothing on screen said so). The words are decided here;
          ;; bridge3-new.js toggles `hidden` and nothing else, exactly as above.
          ;;
          ;; ADJUDICATED — SECOND SANCTIONED ELEMENT (Gene, 2026-07-29). The
          ;; 2026-07-27 ruling bounded the hidden-toggle exception to ONE
          ;; element and said widening it is a new behavior needing its own
          ;; ruling. This widening was named in the wave plan ("becomes visible
          ;; while connected — server-rendered badge; the client only
          ;; feature-detects it") and Gene ordered the wave with "fix as many as
          ;; you can." The bound is otherwise unchanged: server-rendered words,
          ;; `hidden` only, feature-detected, and a THIRD element is again a new
          ;; ruling, not a precedent.
          [:div {:id "b3n-degraded" :class "b3n-offline b3n-degraded" :hidden "hidden"}
           "⚠ the server refused something this page sent — retrying the rest"]
          ;; THE STATE-FEED LIVENESS PAIR (A51), siblings of the banner above and
          ;; for exactly the same reason: #b3n-shell is replaced wholesale on every
          ;; push, so anything the CLIENT must be able to observe across pushes has
          ;; to live outside it.
          ;;
          ;; THE SCAR (Rule 13 — a stale surface that still looks plausible). The
          ;; state stream was kept alive with a bare SSE COMMENT, and `EventSource`
          ;; never surfaces a comment to JavaScript. So when this channel died, the
          ;; page had no way to know: it kept showing the last frame it received —
          ;; a status line, an elapsed timer, a turn — all of them wrong, none of
          ;; them marked. The disconnect banner does not cover it either: that one
          ;; tracks the connection the browser can see, and a proxy that holds a
          ;; socket open while delivering nothing is CONNECTED and SILENT.
          ;;
          ;; So the heartbeat becomes an OBSERVABLE frame — a real
          ;; `datastar-patch-elements` re-render of #b3n-state-alive with a fresh
          ;; `data-at` — and the page's only job is to notice that `data-at` has
          ;; stopped advancing. The THRESHOLD IS SERVER-AUTHORED
          ;; (`data-stale-after-ms`, twice the heartbeat cadence) so the client
          ;; holds no number of its own: a client-side constant here would be a
          ;; second authority on how often the server speaks (D3 rule 1).
          [:span {:id                  "b3n-state-alive"
                  :class               "b3n-hidden"
                  :data-at             (str (long now))
                  :data-stale-after-ms (str state-stale-after-ms)
                  :data-stale-badge    "b3n-state-stale"}]
          [:div {:id "b3n-state-stale" :class "b3n-offline" :hidden "hidden"}
           "⚠ Status feed stale — the page is not hearing from the server"]
          (shell view layout now)]]))))

;; --- SSE fan-out (the STATE channel) --------------------------------------

(defonce ^:private subscribers
  ;; seat -> #{http-kit channel}
  (atom {}))

(defonce ^:private write-lock (Object.))

(defn- send! [ch payload]
  (locking write-lock
    (try (http/send! ch payload false) (catch Exception _ false))))

(defn frames
  "One seat's current surface as a Datastar patch-elements event.

   `ds/sse-raw` rather than `ds/sse-fragment`, and the difference is finding 2.3.
   `sse-fragment` builds the morph target FOR you — it wraps the html in a
   `<div id=…>` — which is exactly how the stream came to push a `<div>` at a
   `<main>`. `sse-raw` takes the element WE render (it asserts the id is really
   there, so the morph target cannot go missing) and emits it verbatim, which
   means `shell` is the only definition of that element in the codebase.

   Both constructors emit multi-line `data:` continuation, which is load-bearing:
   a single `data:` line is TRUNCATED at the first newline on the wire, and this
   fragment is a whole conversation."
  [seat]
  (let [view (render-view seat)
        page (page-for seat)]
    (ds/sse-raw (str (h/html (shell view (:layout page) (long (*now-ms*))))))))

(defn heartbeat-frame
  "The state channel's KEEP-ALIVE, made observable (A51).

   Two things in one write, and both are load-bearing:

   - a real `datastar-patch-elements` frame re-rendering ONLY #b3n-state-alive
     with a fresh `data-at`. It is a morph like any other, so it goes through the
     same `ds/sse-raw` constructor the surface push uses — there is exactly one
     SSE writer on this stream;
   - the `: heartbeat` COMMENT line the loop always sent, kept because that is
     what holds a proxy's idle timer open. It is retained for the pipe and is no
     longer relied on for meaning: `EventSource` never surfaces a comment to
     JavaScript, which is precisely why a comment-only heartbeat left the page
     unable to tell a live feed from a dead one.

   The element is TINY on purpose — one span, no state — so the keep-alive costs
   a few bytes and can never race, reorder, or partially overwrite the surface
   morph."
  []
  (str (ds/sse-raw (str (h/html [:span {:id                  "b3n-state-alive"
                                        :class               "b3n-hidden"
                                        :data-at             (str (long (*now-ms*)))
                                        :data-stale-after-ms (str state-stale-after-ms)
                                        :data-stale-badge    "b3n-state-stale"}])))
       ": heartbeat\n\n"))

(defn broadcast!
  "Push the current surface to every subscriber on `seat`; reap dead channels.

   TOTAL by construction. This runs from a watch on the session atom — i.e. on
   the thread that just applied a client's events — so a rendering or socket
   problem here must never be able to reach the session loop. It logs and
   returns nil.

   RENDER HERE, WRITE THERE. That same fact — this runs on the HTTP handler
   thread that just applied a POST — is why the socket writes are handed to the
   shared push agent instead of being done inline: one slow or half-dead client
   would otherwise block a POST handler, and this server runs http-kit with
   `:thread 8`, so a handful of such writes starves the whole worker pool and
   unrelated requests begin timing out. The RENDER stays on the caller's thread
   because it is cheap and because it pins the frame to the state as it was at
   broadcast time, which is what stops two changes in quick succession from
   painting out of order; the single agent then serialises the writes, so the
   order the renders were taken in is the order they reach the wire."
  [seat]
  (try
    (when (seq (get @subscribers seat #{}))
      (let [f (frames seat)]
        (sse/push-off-thread!
          (fn []
            (let [subs (get @subscribers seat #{})
                  dead (reduce (fn [d ch] (if (send! ch f) d (conj d ch))) #{} subs)]
              (when (seq dead)
                (swap! subscribers update seat #(reduce disj (or % #{}) dead))
                ;; The census must lose them too, or /api/health over-reports the
                ;; live stream count and the 250-of-300 alarm reads a number that
                ;; no longer means anything.
                (run! #(sse/deregister! %) dead)))))))
    (catch Throwable t
      (log/warn :bridge3-new-broadcast-error :seat seat :err (.getMessage t)))))

;; THE WATCH. `:seq` is the session's own "something a client must see happened"
;; counter — `ingest` bumps it exactly when a step produced effects OR the
;; display view changed. Keying the broadcast on it is what stops the 250 ms
;; `:fact/clock` tick from becoming a 4 Hz HTML push: a tick that changes nothing
;; leaves `:seq` alone, so nothing is rendered and nothing is sent.
(defn- seats-to-push [before after]
  (for [[seat s] after
        :let [b (get before seat)]
        :when (or (nil? b) (not= (:seq b) (:seq s)))]
    seat))

(defonce ^:private watch-installed
  (delay
    (add-watch rs/sessions ::bridge3-new
               (fn [_ _ before after]
                 (doseq [seat (seats-to-push before after)]
                   (broadcast! seat))))
    (add-watch pages ::bridge3-new
               (fn [_ _ before after]
                 (doseq [[seat p] after
                         :when (not= (dissoc (get before seat) :touched-at)
                                     (dissoc p :touched-at))]
                   (broadcast! seat))))
    true))

(defn ensure-watch! [] @watch-installed nil)

;; --- Handlers -------------------------------------------------------------

(defn- param [req k]
  (let [n (name k)]
    (or (get (:query-params req) n)
        (get (:query-params req) (keyword n))
        (get (:params req) n)
        (get (:params req) (keyword n)))))

(defn seat-of
  "The seat this request is for. Sanitized against the app-wide slug rule, and a
   junk seat falls back to the default rather than 400-ing a PAGE — but the SSE
   and the POSTs refuse it outright (see below), because a stream or a mutation
   on a seat nobody named is a bug worth seeing."
  [req]
  (let [s (some-> (param req "seat") str str/trim)]
    (if (rs/valid-seat? s) s "bridge")))

(defn handle-page
  "GET /bridge3-new?seat=X"
  [req]
  (ensure-watch!)
  (let [seat (seat-of req)
        page (page-for seat)]
    {:status  200
     :headers {"Content-Type"  "text/html; charset=utf-8"
               "Cache-Control" "no-store"}
     :body    (page-html seat (render-view seat) page)}))

(defn handle-sse
  "GET /sse/bridge3-new?seat=X — the STATE channel.

   Current frames on connect (so a fresh tab is correct before it causes
   anything), then a morph on every session change, then an OBSERVABLE heartbeat
   (A51 — `heartbeat-frame`, a real morph plus the comment the proxy needs).
   This carries NO effects: `:open-mic`, `:speak` and friends ride the session
   loop's own stream. Two channels, one brain.

   THE STREAM IS DELIBERATELY RECYCLED at the deadline `sse-registry` gave it
   (240–270 s, under Cloud Run's 300 s request timeout). Nothing is lost by
   that: this endpoint sends the full current `frames` on every connect, so a
   client that re-opens is re-painted the whole surface, and the page asks
   Datastar to retry `always` precisely so a clean server-side close is
   re-opened rather than mistaken for a finished response.

   Recycling exists because heartbeat reaping alone cannot return a slot. A
   write to a sleeping phone's socket SUCCEEDS into the kernel send buffer long
   after the peer is gone, so the heartbeat below never errors on the one kind
   of client that most needs collecting, and that ghost holds one of the
   instance's 300 concurrent requests until the platform times it out. Three
   times in the field that has walked the whole service into `429 Rate
   exceeded` (bead marvin-voice-remote-gyb). A bounded lifetime is the only
   thing that bounds it."
  [req]
  (ensure-watch!)
  (let [seat (some-> (param req "seat") str str/trim)]
    (if-not (rs/valid-seat? seat)
      {:status 400 :headers {"Content-Type" "application/json"} :body "{\"error\":\"invalid seat\"}"}
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
           ;; ORDER IS THE CONTRACT: header, then REGISTER, then the initial
           ;; frame, then subscribe, then the heartbeat. Registering before the
           ;; first frame is what makes supersession close-before-serve — the
           ;; stream this one replaces is shut while its replacement is still
           ;; being set up, so the two never both count against the 300.
           (sse/register! {:ch          ch
                           :endpoint    "/sse/bridge3-new"
                           :seat        seat
                           :client-id   (sse/client-id-of req)
                           :remote-addr (:remote-addr req)
                           :close!      (fn [] (http/close ch))})
           (send! ch (frames seat))
           (swap! subscribers update seat (fnil conj #{}) ch)
           (future
             (loop []
               (Thread/sleep ^long rs/heartbeat-ms)
               (when (contains? (get @subscribers seat #{}) ch)
                 ;; BOUNDED RECYCLE, asked of the loop that is already awake with
                 ;; the channel in hand rather than of a second timer of our own
                 ;; (the shape `datastar-kit.sse/start-heartbeat!` established).
                 (if (sse/rotate-due? ch)
                   (do (send! ch ": recycle\n\n")
                       (swap! subscribers update seat #(disj (or % #{}) ch))
                       (sse/close! ch :rotation))
                   ;; A51: an OBSERVABLE heartbeat, not a bare comment. See
                   ;; `heartbeat-frame` — a comment cannot reach JavaScript, so a
                   ;; comment-only keep-alive left "the feed stopped" indistinguishable
                   ;; from "nothing has changed".
                   (if (send! ch (heartbeat-frame))
                     (recur)
                     (do (swap! subscribers update seat #(disj (or % #{}) ch))
                         (sse/deregister! ch))))))))
         :on-close
         (fn [ch _status]
           (swap! subscribers update seat #(disj (or % #{}) ch))
           (sse/deregister! ch))}))))

(defn- mutate-page!
  "Apply a pure fn to one seat's page state and answer 204. The push is the
   WATCH's job, not this handler's — POST and forget, the server pushes the
   result (Datastar doctrine)."
  [req f]
  (let [seat (some-> (param req "seat") str str/trim)]
    (if-not (rs/valid-seat? seat)
      {:status 400 :headers {"Content-Type" "application/json"} :body "{\"error\":\"invalid seat\"}"}
      (do (swap! pages (fn [m]
                         (let [now (long (*now-ms*))
                               m'  (page-state-for m seat now)]
                           (update m' seat #(assoc (f %) :touched-at now)))))
          {:status 204 :headers {"Cache-Control" "no-store"} :body ""}))))

(defn handle-layout
  "POST /api/bridge3-new/layout?seat=X — cycle the layout."
  [req]
  (ensure-watch!)
  (mutate-page! req #(update % :layout next-layout)))

(defn handle-say
  "POST /api/bridge3-new/say?seat=X, body `{\"text\": \"...\"}` — TYPED SEND
   (bead 3he).

   THE THINNEST POSSIBLE ENDPOINT, and every thing it does NOT do is deliberate.
   It does not enqueue a message, does not append a conversation turn, does not
   decide whether the text is a turn, and does not choose the identity of
   anything: it reads the ONE browser-owned value on the say control (the text
   Gene typed — Datastar rule 11, and the seat rides the URL) and hands it to
   `reducer-session/ingest-say!`, which authors a `:command/say` envelope and
   pushes it through the same validate-then-step path every tap takes. Exactly the
   `ingest-reply!` shape, from the other direction.

   THE BROWSER HOLDS NO SESSION IDENTITY. It sends no `session/id`, no
   `client/id`, no `event/id`, no protocol version — the server mints all four,
   because the say box is one input and one button and giving it a protocol codec
   would be handing an adapter the thing D6 keeps on the wire.

   REFUSALS ARE STATUSES, DECIDED THERE AND MAPPED HERE. `ingest-say!` returns a
   value naming the refusal; this turns it into 400 (the request was wrong: no
   text, too long, junk seat) or 500 (we were wrong). It never invents a refusal
   of its own — a page that refused text the reducer would have accepted would be
   the third opinion in a system that is trying to have one.

   The BODY is answered rather than 204'd, unlike the layout POST: `say-box.js`
   fires and forgets, but a curl, a native shell or a test asking whether the
   words landed deserves an answer that says so."
  [req]
  (ensure-watch!)
  (let [seat (some-> (param req "seat") str str/trim)
        text (try (:text (mjson/parse (let [b (:body req)]
                                          (if (string? b) b (slurp b)))))
                  (catch Exception _ nil))
        res  (rs/ingest-say! seat text)]
    (if (:ok res)
      {:status  200
       :headers {"Content-Type" "application/json; charset=utf-8"
                 "Cache-Control" "no-store"}
       :body    (json/write-str {:ok true :utt (:utt/id res)})}
      {:status  (if (= :internal (:error res)) 500 400)
       :headers {"Content-Type" "application/json; charset=utf-8"
                 "Cache-Control" "no-store"}
       :body    (json/write-str {:error (name (:error res)) :detail (:detail res)})})))
