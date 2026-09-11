(ns cfp-scheduler-killer.views.schedule
  "Organizer schedule and public agenda views."
  (:require
   [cfp-scheduler-killer.events :as events]
   [cfp-scheduler-killer.public-catalog :as public-catalog]
   [cfp-scheduler-killer.schedule :as schedule]
   [cfp-scheduler-killer.views.organizer-layout :as organizer-layout]
   [cfp-scheduler-killer.views.personal-schedule :as personal-schedule]
   [clojure.string :as str]
   [datastar-kit.ds :as ds]))

(defn- normalized-speaker-name [name]
  (some-> name str str/trim str/lower-case not-empty))

;; INTENT: AGENDA-PUBLIC-003
(defn- agenda-speaker-index
  [event]
  ;; @spec AGENDA-PUBLIC-003
  (into {}
        (keep (fn [speaker]
                (when-let [name (normalized-speaker-name (:name speaker))]
                  [name {:presenter-context (let [parts (->> [(:tagline speaker)
                                                              (:company speaker)]
                                                             (map #(some-> % str str/trim))
                                                             (remove str/blank?))]
                                              (when (seq parts)
                                                (str "(" (str/join ", " parts) ")")))
                         ;; Speaker-name blocks predate first-class talks and
                         ;; retain their established title + company context.
                         :block-context (->> [(:tagline speaker) (:company speaker)]
                                             (remove str/blank?)
                                             (str/join ", "))}]))
              (public-catalog/program-speakers event))))

(defn- agenda-session-context
  [speaker-index {:keys [title speakers]}]
  ;; @spec AGENDA-PUBLIC-003
  ;; @spec AGENDA-PUBLIC-004
  (let [names (vec (remove str/blank? speakers))
        fallback-headings (set [(str/join ", " names) (str/join " + " names)])
        presenters (mapv (fn [name]
                           {:name name
                            :context (get-in speaker-index
                                             [(normalized-speaker-name name)
                                              :presenter-context])})
                         names)]
    {:fallback-heading? (contains? fallback-headings title)
     :presenters presenters}))

;; INTENT: AGENDA-PUBLIC-006
(defn- agenda-speaker-address-index
  "person id -> the handle that ANSWERS at /agenda/:slug/speakers/<handle>.

   Built from `public-speakers` — the PUBLISHED roster — because that is what
   handle-public-speaker searches. A placement may know a person id the public
   speaker page will not serve, and an id that 404s is a broken deep link
   handed to every reader at once (the same gate views.public-widgets/
   roster-lookups documents).

   BOTH handles are KEYS, because handle-public-speaker resolves either one
   (`speaker-in-roster` matches on slug OR id) and a roster entry may carry
   only one of them: an announced speaker whose person record has not been
   adopted yet is addressed entirely by the id sitting in its `:slug` field.

   The VALUE is always the slug when one exists: the slug form returns 200
   outright while the id form 301s to it, and a consumer proxying us
   same-origin follows that redirect out of its own namespace."
  [event]
  (into {}
        (mapcat (fn [speaker]
                  (let [id (some-> (:id speaker) str str/trim not-empty)
                        slug (some-> (:slug speaker) str str/trim not-empty)]
                    (when-let [address (or slug id)]
                      (keep #(when % [% address]) [id slug])))))
        (public-catalog/public-speakers event)))

;; INTENT: AGENDA-PUBLIC-006
(defn- agenda-session-href
  "Where a program card points — or nil, which renders it unlinked.

   A submission-backed session links to its session page. A CANONICAL TALK has
   no submission, and this cut invents no talk-detail route, so the only honest
   target is its ONE presenter's public speaker page. `:speaker-ids` is
   published positionally beside `:speakers` precisely so a consumer never has
   to fuzzy-match a human being, so the id is read, never the name.

   Everything else is nil ON PURPOSE. Building the href unconditionally from
   `:submission-id` shipped eighteen anchors ending in `/sessions/?from=agenda`
   — an empty id, a dead link on the judge-facing program. An honest non-link
   beats a dead one."
  [event day speaker-address {:keys [submission-id speakers speaker-ids]}]
  (if submission-id
    (str "/agenda/" (:slug event) "/sessions/" submission-id
         "?from=agenda&day=" day)
    ;; Zipped, not filtered-then-indexed: `:speaker-ids` is positional against
    ;; the RAW `:speakers`, so dropping blank names first would shift the ids.
    (let [presenters (->> (map vector speakers (concat speaker-ids (repeat nil)))
                          (remove #(str/blank? (str (first %)))))]
      ;; Two presenters and one card cannot resolve to one person's page; a
      ;; guess that lands on a REAL person is worse than no link at all.
      (when (= 1 (count presenters))
        (when-let [handle (get speaker-address
                               (some-> (second (first presenters)) str str/trim
                                       not-empty))]
          (str "/agenda/" (:slug event) "/speakers/" handle))))))

(defn- agenda-presenter-list
  [presenters session-href]
  ;; @spec AGENDA-PUBLIC-003
  [:div.agenda-presenter-list
   (for [{:keys [name context]} presenters]
     [:div.agenda-presenter-block
      [:div.agenda-presenter-name
       (if session-href
         [:a {:href session-href} name]
         name)]
      (when context
        [:div.agenda-speaker-context context])])])

(defn agenda-days
  "The published agenda as framed day sections (Gene ratified 2026-08-11):
   every event day renders inside its programming-hours frame; a day without
   sessions says so instead of vanishing; an all-empty agenda offers the
   hold-the-dates calendar subscription."
  [event days active-day & [selected-ids {:keys [locked?]}]]
  (let [selected-ids (or selected-ids #{})
        ;; The whole program on one page, always — no day tabs (Gene,
        ;; 2026-08-18: "make all days visible -- no tabbing!"). The nav
        ;; block below self-hides; ?day= deep links still scroll via anchors.
        all-days? true
        selected-day (or (some #(when (= active-day (:day %)) (:day %)) days)
                         (:day (first days)))
        selected-index (first (keep-indexed #(when (= selected-day (:day %2)) %1) days))
        day-link (fn [day]
                   (str "/program/" (:slug event) "?day=" (:day day) "#agenda"))
        ampm (fn [hhmm]
               (let [[h m] (map #(Integer/parseInt %) (str/split hhmm #":"))
                     h12 (cond (zero? h) 12 (> h 12) (- h 12) :else h)]
                 (str h12 (when (pos? m) (format ":%02d" m))
                      (if (< h 12) "am" "pm"))))
        ;; Speaker-name blocks join against the program-speaker roster once per
        ;; render: a block whose title IS a program speaker renders as a proper
        ;; session card with an org sub-line; every other block is operations.
        speaker-index (agenda-speaker-index event)
        ;; Resolved once per render, for the same reason: a canonical talk's
        ;; card links to a person only when that person's page actually answers.
        speaker-address (agenda-speaker-address-index event)
        block-speaker (fn [title]
                        (get speaker-index (normalized-speaker-name title)))
        ops-social? (fn [title]
                      (boolean (re-find #"(?i)networking|party|reception|social|happy\s*hour|dinner"
                                        (str title))))
        hours (events/day-hours event)
        frame (str "Programming " (ampm (:day-start hours))
                   " – " (ampm (:day-end hours)))
        any-items? (boolean (some #(seq (:items %)) days))]
    (list
      ;; The frame states itself ONCE while every day shares the same hours
      ;; (Gene, 2026-08-11: "why listed twice?") — per-day only on divergence,
      ;; when per-day overrides exist.
     [:div.agenda-frame-note frame
      (when (> (count days) 1) (str ", " (if (= 2 (count days)) "both days" "every day")))]
      ;; A draft agenda says it is one. Quietly — one field-hint line, gone the
      ;; moment the schedule locks (callers that know the lock pass :locked?).
     (when-not locked?
       [:div.field-hint.agenda-subject-to-change
        "Schedule subject to change — sessions may shift as the program takes shape."])
     (when (and (not all-days?) (> (count days) 1))
       [:nav.agenda-day-controls {:aria-label "Agenda days"}
        (when (pos? selected-index)
          [:a.ui.button.agenda-day-previous {:href (day-link (nth days (dec selected-index)))}
           "← Previous day"])
        [:div.agenda-day-tabs {:role "tablist" :aria-label "Agenda days"}
         (for [d days]
           [:a.agenda-day-tab {:href (day-link d)
                               :role "tab"
                               :aria-current (when (= selected-day (:day d)) "page")
                               :class (when (= selected-day (:day d)) "active")}
            (:label d)])]
        (when (< selected-index (dec (count days)))
          [:a.ui.button.agenda-day-next {:href (day-link (nth days (inc selected-index)))}
           "Next day →"])])
     (when-not any-items?
       [:p.agenda-soon "The agenda will be published soon. "
        [:a {:href (str "/events/" (:slug event) "/exports/calendar.ics")}
         "Hold the dates →"]])
     (for [d (if all-days? days (filter #(= selected-day (:day %)) days))]
       [:div {:id (str "agenda-day-" (:day d))
              :class "agenda-day-section"}
        [:h3.ui.header (:label d)]
        (if (empty? (:items d))
          [:div.agenda-day-empty
           "Sessions coming — the "
           [:a {:href (str "/cfp/" (:slug event))} "call for speakers"]
           " is open."]
          (for [item (:items d)]
            (let [block? (= :block (:kind item))
                  speaker (when block? (block-speaker (:title item)))
                  org (:block-context speaker)
                  session-context (when-not block?
                                    (agenda-session-context speaker-index item))
                  session-href (when-not block?
                                 (agenda-session-href event (:day d)
                                                      speaker-address item))
                  ;; My-schedule is keyed by submission id end to end (the
                  ;; button, the cookie, the /my page, the .ics export). With
                  ;; no submission id the control posts nothing and renders an
                  ;; attribute-less button that silently never works, so a
                  ;; canonical talk gets no control rather than a broken one.
                  session-actions (when (and (not block?) (:submission-id item))
                                    [:div.agenda-session-actions
                                     ;; @spec AGENDA-PUBLIC-005
                                     (personal-schedule/toggle-control
                                      event (:submission-id item)
                                      (contains? selected-ids (:submission-id item))
                                      {:compact? true})])
                  tba? (and block? (= "Keynote" (:title item)))
                  body-class (cond
                               speaker "agenda-card agenda-speaker-card"
                               tba? "agenda-band agenda-band-ops agenda-band-tba"
                               block? (str "agenda-band "
                                           (if (ops-social? (:title item))
                                             "agenda-band-social"
                                             "agenda-band-ops"))
                               :else "agenda-card agenda-session-card")]
              [:div.agenda-item {:class (when (and block? (not speaker)) "agenda-item-ops")}
               [:div.agenda-time (schedule/time-range-display (:start item) (:end item))]
               [:div.agenda-body {:class body-class}
                (if block?
                  (if speaker
                    (list
                     [:div.agenda-speaker-name (:title item)]
                     (when-not (str/blank? org)
                       [:div.agenda-speaker-org org]))
                     ;; Iterated live with Gene: invisible → blank band → this.
                     ;; A held seat says so, at whisper volume (.agenda-band-tba
                     ;; = 50% opacity italic), so ten of them read as texture,
                     ;; not nagging.
                    [:div.agenda-band-label (if tba? "To be announced" (:title item))])
                  (list
                   [:div.agenda-session-heading
                    (if (:fallback-heading? session-context)
                      (agenda-presenter-list (:presenters session-context)
                                             session-href)
                      [:div.agenda-title
                       (if session-href
                         [:a {:href session-href} (:title item)]
                         (:title item))])
                    session-actions]
                   (when-not (:fallback-heading? session-context)
                     (agenda-presenter-list (:presenters session-context) nil))
                   (when-not (str/blank? (:abstract item))
                     [:details.public-show-more
                      [:summary
                       [:span.public-disclosure-more "Show more"]
                       [:span.public-disclosure-less "Show less"]]
                      [:p (:abstract item)]])
                   [:div.public-chips
                    (when-not (str/blank? (:format item))
                      [:span.public-chip (str "Format: " (:format item))])
                    (when-not (str/blank? (:track item))
                      [:span.public-chip (str "Track: " (:track item))])]))
                (when (:room item) [:div.agenda-room (:room item)])]])))]))))

(defn agenda-export-hint [event]
  [:div.field-hint {:style "margin-top:2.5em;"}
   "Times are local to the event (" (:tz event) "). "
   [:a {:href (str "/events/" (:slug event) "/exports/calendar.ics")} "Subscribe by calendar"]
   " · "
   [:a {:href (str "/events/" (:slug event) "/exports/sessions.json")} "Session data"]])

(defn- block-card [event editable? b trayed]
  [:div.sched-card.block
   [:div (:label b)]
   (when editable?
     [:div.acts
      ;; The Fill gesture (v0): placeholder → real session at the block's exact
      ;; time, placeholder retires, one POST. Only offered when someone is
      ;; actually placeable.
      (when (seq trayed)
        [:form.place-form {:method "post"
                           :action (str "/api/events/" (:slug event) "/schedule/block-fill")}
         [:input {:type "hidden" :name "block-id" :value (:id b)}]
         ;; Default is EMPTY — a slot with nobody in it must look empty, never
         ;; pre-assigned (Gene+Ann, live 2026-08-18: 24 pre-filled dropdowns
         ;; read as 24 pieces of fake work).
         [:select {:name "submission-id"}
          [:option {:value "" :selected true} "＋ fill this slot…"]
          (for [s trayed]
            [:option {:value (:id s)}
             (str (:name (first (:speakers s)))
                  " — " (get-in s [:answers :talk-title]))])]
         [:button.ui.mini.button {:type "submit"} "Fill"]])
      [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/block-remove")}
       [:input {:type "hidden" :name "block-id" :value (:id b)}]
       [:button {:type "submit"} "Remove"]]])])

(defn conflict-chips
  "Named collisions with BOTH fixes offered, because the tool doesn't know which
   side should move."
  [event conflicts]
  (when (seq conflicts)
    [:div {:style "margin-bottom:1em;"}
     (for [c conflicts]
       [:div.conflict-chip {}
        (:message c)
        [:span.fixes
         (when-let [a (:a c)]
           [:form {:method "post"
                   :action (str "/api/events/" (:slug event) "/schedule/clear")}
            [:input {:type "hidden" :name "submission-id" :value (:submission-id a)}]
            [:button.ui.mini.basic.button {:type "submit"}
             "move \"" (let [t (str (:title a))]
                         (if (> (count t) 28) (str (subs t 0 28) "…") t)) "\""]])
         (when-let [b (:b c)]
           [:form {:method "post"
                   :action (str "/api/events/" (:slug event) "/schedule/clear")}
            [:input {:type "hidden" :name "submission-id" :value (:submission-id b)}]
            [:button.ui.mini.basic.button {:type "submit"}
             "move \"" (let [t (str (:title b))]
                         (if (> (count t) 28) (str (subs t 0 28) "…") t)) "\""]])]])]))

(defn- day-tab
  "Day pill; with `counts` ({day {:filled n :total n}}) the pill carries its
   own arithmetic — 'Day 1 — Oct 7 · 12 of 13' (Gene: the pills should say it
   so the day-tab split never reads as missing slots)."
  [event day active-day & [counts]]
  [:a.chip {:class (when (= day active-day) "on")
            :href (str "/events/" (:slug event) "/schedule?day=" day)}
   (or (schedule/day-label event day) day)
   (when-let [{:keys [filled total]} (get counts day)]
     (str " · " filled " of " total))])

(defn- room-options [rooms selected]
  (list
   [:option {:value "" :selected (nil? selected)} "no room yet"]
   (for [r rooms]
     [:option (cond-> {:value (:id r)}
                (= (:id r) selected) (assoc :selected true))
      (:name r)])))

(defn- duration-label
  [minutes]
  (let [hours (quot minutes 60)
        minutes (mod minutes 60)]
    (str (when (pos? hours) (str hours "h"))
         (when (and (pos? hours) (pos? minutes)) " ")
         (when (or (zero? hours) (pos? minutes)) (str minutes "m")))))

(defn schedule-status-bar
  [stats]
  [:div#schedule-status.status-bar
   [:div [:span.n (:placed stats)] "/" (:accepted stats) " "
    [:span.lbl "accepted placed"]]
   [:span.status-sep "·"]
   [:div [:span.n (:unplaced stats)] " " [:span.lbl "in the tray"]]
   ;; Only ever shown when it is non-zero: a standing zero trains people to
   ;; stop reading the bar (inform/alert-rows house rule). Non-zero, it is the
   ;; one number explaining why the tray is shorter than the accept count.
   (when (pos? (or (:awaiting-inform stats) 0))
     (list [:span.status-sep {} "·"]
           [:div {} [:span.n (:awaiting-inform stats)] " "
            [:span.lbl "accepted, not yet informed"]]))
   [:span.status-sep "·"]
   [:div [:span.n (:unroomed stats)] " " [:span.lbl "unroomed"]]
   [:span.status-sep "·"]
   [:div [:span.n {:style (when (pos? (:conflicts stats)) "color:#f2711c;")}
          (:conflicts stats)] " " [:span.lbl "conflicts"]]
   (when (pos? (get-in stats [:room-time :capacity] 0))
     (list [:span.status-sep {} "·"]
           [:div.slot-math
            [:span.n (duration-label (get-in stats [:room-time :filled]))]
            " / "
            [:span.n (duration-label (get-in stats [:room-time :capacity]))]
            " " [:span.lbl "room time filled"]
            ", "
            [:span.n (duration-label (get-in stats [:room-time :open]))]
            " " [:span.lbl "open"]]))
   (for [d (:per-day stats)
         :let [room-day (some #(when (= (:day d) (:day %)) %)
                              (get-in stats [:room-time :per-day]))]]
     (list [:span.status-sep {} "·"]
           [:div {}
            [:span.lbl (:label d) ": "]
            [:span.n (:sessions d)] [:span.lbl " sessions"]
            (when (pos? (:blocks d))
              (list " " [:span.n (:blocks d)] [:span.lbl " blocks"]))
            (when (pos? (or (:capacity room-day) 0))
              (list ", " [:span.n (duration-label (:open room-day))]
                    [:span.lbl " room time open"]))]))])

(defn- place-form
  [event submission day rooms]
  [:form.place-form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/place")}
   [:input {:type "hidden" :name "submission-id" :value (:id submission)}]
   [:select {:name "day"}
    (for [d (schedule/event-days event)]
      [:option (cond-> {:value d} (= d day) (assoc :selected true))
       (schedule/day-label event d)])]
   [:input {:type "time" :name "start" :step "60" :value "09:00" :required true}]
   [:input {:type "number" :name "duration" :min "5" :step "5"
            :value (schedule/duration-for event submission) :style "width:5em;"}]
   [:select {:name "room-id"} (room-options rooms nil)]
   [:button.ui.mini.primary.button {:type "submit"} "Place"]])

(defn- placed-card
  [event editable? p rooms conflicted?]
  [:div.sched-card {:class (when conflicted? "conflicted")}
   [:div [:a {:href (str "/events/" (:slug event) "/submissions/" (:submission-id p))}
          (:title p)]]
   [:div.who (str/join ", " (map :name (:speakers p)))]
   (when editable?
     [:div.acts]
     ;; Quick room re-assign: the late-room-assignment workflow, one control.
     [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/place")}
      [:input {:type "hidden" :name "submission-id" :value (:submission-id p)}]
      [:input {:type "hidden" :name "day" :value (:day p)}]
      [:input {:type "hidden" :name "start" :value (schedule/minutes->hhmm (:start p))}]
      [:input {:type "hidden" :name "duration" :value (- (:end p) (:start p))}]
      [:select {:name "room-id" :onchange "this.form.submit()"}
       (room-options rooms (:room-id p))]]
     [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/clear")}
      [:input {:type "hidden" :name "submission-id" :value (:submission-id p)}
       [:button {:type "submit"} "Clear"]]])])

(defn schedule-grid
  "Rooms as columns plus an Unroomed column; one row per occupied start time.
   Compressed to what is actually there — an empty 15-minute lattice is a lot of
   scrolling to say nothing."
  [event day {:keys [rooms placed blocks conflicted-ids editable? trayed]}]
  (let [day-placed (filter #(= day (:day %)) placed)
        day-blocks (filter #(= day (:day %)) blocks)
        starts (sort (distinct (concat (map :start day-placed) (map :start day-blocks))))
        cols (concat (map (fn [r] {:id (:id r) :name (:name r)}) rooms)
                     [{:id nil :name "Unroomed"}])]
    (if (empty? starts)
      [:div.empty-state
       "Nothing placed on this day yet. Use " [:strong "Place"] " on a tray card below."]
      [:table.sched-grid
       [:thead
        [:tr [:th.sched-time "Time"]
         (for [c cols] [:th {} (:name c)])]]
       [:tbody
        (for [t starts]
          [:tr {}
           [:td.sched-time (schedule/minutes->display t)]
           (for [c cols]
             [:td {:class (when (nil? (:id c)) "unroomed-col")}
              (for [p day-placed
                    :when (and (= t (:start p)) (= (:room-id p) (:id c)))]
                (placed-card event editable? p rooms (contains? conflicted-ids (:submission-id p))))
              (for [b day-blocks
                    :when (and (= t (:start b)) (= (:room-id b) (:id c)))]
                (block-card event editable? b trayed))])])]])))

;; ── Rough-cut board (Ann-call live build, 2026-08-18) ────────────────────────
;; "It's almost like a Trello lane. Yes, that's exactly it." Two lanes (Day 1 /
;; Day 2), AM/PM halves, text-only cards, drag from the rail. A drop creates a
;; NAMED BLOCK at a sentinel time (AM 09:00 / PM 13:00) — holding space is what
;; blocks are for; tweezers mode assigns real times later.

(def ^:private rough-open-label
  "Skeleton labels that count as open slots rather than speaker cards."
  "Keynote")

(defn- rough-half [minutes] (if (< minutes 720) :am :pm))

(defn- rough-label-chips
  "Thin color bars, one per active balance label — organizer-private, board
   only. `title` carries the (per-event) name for hover; order follows the
   palette."
  [event labels]
  (for [{:keys [key name color]} (schedule/rough-labels-for event)
        :when (contains? labels key)]
    [:span {:title name
            :style (str "display:inline-block; width:14px; height:4px; "
                        "border-radius:2px; background:" color ";")}]))

(defn- rough-insert-target
  [{:keys [kind before-id before-name seat]}]
  ;; @spec ROUGH-DRAG-002
  ;; @spec ROUGH-DRAG-003
  ;; @spec ROUGH-DRAG-006
  [:div.rough-insert-target
   (cond-> {:data-drop-kind (name kind)
            :data-target-seat seat}
     before-id (assoc :data-before-id before-id)
     before-name (assoc :data-before-name before-name))
   [:span.rough-target-copy
    (case kind
      :rail (str "Insert before " before-name)
      :end "Insert at end")]])

(defn- rough-gap-card
  "A visible OPEN SEAT (Gene+Ann amendment, live): each unconsumed Keynote
   skeleton slot renders as a blank soft-pink card at its real seat position.
   MOVABLE within its half (ratified: \"we want Hendrickson to close out the
   morning, so I want to move the open slot around her\") — draggable, and
   the drag sends gap-move=<id> (main seat's JS). A speaker drop ON it still
   targets exactly this seat via data-gap-block-id (slot=<id>); stamp-mode
   ignores it (data-kind gap)."
  [b]
  [:div.rough-card.rough-gap
   ;; Same padding + two-line min-height as a real speaker card (Gene: "the
   ;; open slots need to be the same height as the real speakers").
   {:id (str "rc-" (:id b))
    :style (str "cursor:grab; margin-bottom:0.4em; background:#fdf0f2; "
                "border:1px dashed #e6a5b2; border-radius:6px; "
                "padding:0.4em 0.6em 0.95em; position:relative; min-height:3.6em; "
                "box-sizing:border-box;")
    :draggable "true"
    :data-kind "gap"
    :data-gap "1"
    :data-name "Keynote"
    :data-block-id (:id b)
    :data-gap-block-id (:id b)
    :data-item-id (:id b)
    :data-seat (:start b)
    :data-source-ordinary (not (contains? #{540 780} (:start b)))
    :data-drop-kind "exact"}
   [:div {:style "color:#c46a7c; font-size:0.88em;"} "Open slot"]
   [:div.rough-time {:style "position:absolute; bottom:0.3em; right:0.45em; color:#e0b6bf; font-size:0.72em;"}
    (schedule/minutes->display (:start b))]])

(defn- ghost-promote-menu
  "The placeholder card's one gesture beyond drag — treatment B2 (Gene ratified
   2026-08-25, bead 7cun.1, after he field-tested the first cut and could not
   SEE the trigger: \"the ⋯ so light, I couldn't see it\").

   The trigger is now a BORDERED 22px square that reads on the gray ghost fill.
   Still a plain details/summary — no server UI state, no open/close JS —
   exactly like the balance-label popover beside it.

   The menu holds two things:

   1. `Make a real speaker →`, a plain LINK to the canonical create-speaker
      page, prefilled from the card's own label and carrying the block id.
      Deliberately NOT a form: the menu renders zero fields, so the create form
      it opens cannot drift from the real one (which owns headshot paste/drag,
      duplicate detection and the 422 re-render).
   2. The event's balance labels as TOGGLE CHIPS — tagging is a burst action,
      so it gets two clicks, never an `Add tags…` hop. Each chip is a
      fetch-POST through the SAME `schedLabel` / `handle-label-toggle` /
      `schedule/toggle-label!` path the ＋ popover and the Stamp flow use — no
      new fact type — and the 204 is answered by an SSE board repaint. Chips
      already on the block wear the green ✓ on-state here; the card FACE keeps
      showing them through `rough-label-chips`, unchanged.

   `variant` picks the wrapper's anchoring class (:lane | :rail); `kind` is the
   card kind the toggle POST addresses (ghosts are always blocks today)."
  [event block {:keys [labels kind variant]}]
  (let [labels (set labels)
        id (:id block)
        kind-name (clojure.core/name (or kind :block))]
    [:details {:class (str "ghost-menu ghost-menu--" (clojure.core/name variant))}
     ;; Fomantic ships an UNLAYERED `summary{display:list-item}`, which beats
     ;; anything in @layer components — so the bordered box lives on an inner
     ;; span the reset cannot reach, and the summary only loses its marker.
     [:summary.ghost-menu-trigger {:draggable "false"
                                   :title "Placeholder actions"}
      [:span.ghost-menu-trigger-face "⋯"]]
     [:div.ghost-menu-pop
      [:a.ghost-menu-promote {:href (schedule/promote-url event block)
                              :target "_blank"
                              :rel "noopener"
                              :draggable "false"}
       "Make a real speaker →"]
      [:hr.ghost-menu-rule]
      [:div.ghost-menu-heading "Tags"]
      [:div.ghost-menu-chips
       (for [{:keys [key name color]} (schedule/rough-labels-for event)
             :let [active? (contains? labels key)]]
         [:button {:type "button"
                   :draggable "false"
                   :class (str "ghost-menu-chip" (when active? " is-on"))
                   :title (str (if active? "Remove " "Add ") name)
                   :onclick (str "schedLabel('" id "','" key "','" kind-name "')")}
          [:span.ghost-menu-dot {:style (str "background:" color ";")}]
          name
          (when active? [:span.ghost-menu-check "✓"])])]]]))

(defn- rough-card
  "One Trello-sized card in a lane: a named block, a placed session, or a GHOST
   reservation (dashed border — a spot held for someone not yet onboarded).
   Ghosts stay draggable; the move handler carries the marker through.
   `labels` = active balance-label keys (organizer-private, bead 49bd)."
  [event {:keys [kind id title who ghost? star? time start labels]}]
  (let [labels (set labels)]
    [:div.rough-card
     ;; Ghosts read as not-yet-real: gray fill + dashed edge (Gene, live,
     ;; re-confirmed: gray = "still waiting for them"). Starred cards stay
     ;; PLAIN — the gold ★ glyph alone carries the meaning (Gene: "they don't
     ;; need to be that prominent").
     ;; Stable server-rendered :id — the Datastar morph law: keyed elements
     ;; survive SSE reconciliation (unkeyed cards caused the 3-card drag ghost).
     (cond-> {:id (str "rc-" id)
              :style (str "cursor:grab; margin-bottom:0.4em; "
                          (if ghost? "background:#f3f1ed; " "background:#fff; ")
                          (if ghost?
                            "border:1px dashed #b3aea6; "
                            "border:1px solid #d9d7d2; ")
                          ;; UNIFORM card height (Gene: "why are some taller?"):
                          ;; every lane card — with org, without org, ghost,
                          ;; gap — shares the same two-line min-height and the
                          ;; same bottom strip (reserved for label chips + soft
                          ;; time), so nothing collides and nothing varies.
                          "border-radius:6px; padding:0.4em 0.6em 0.95em; box-shadow:0 1px 2px rgba(0,0,0,0.06); position:relative; min-height:3.6em; box-sizing:border-box;")
              :draggable (str (not= :placed kind))
              :data-kind (name kind)
              :data-item-id id
              :data-name title
              :data-seat start
              :data-source-ordinary (not (contains? #{540 780} start))}
       (= kind :block) (assoc :data-block-id id)
       (= kind :talk) (assoc :data-talk-id id))
     ;; ★ = organizer-only "special role" marker (multiple per section is
     ;; expected — "three bookends for a killer opening"). Never public.
     ;; Generous hit target + draggable=false so the grab-hand never fights the
     ;; star click (Gene: "the hand is obscuring the star").
     (when (contains? #{:block :talk} kind)
       [:button {:type "button"
                 :draggable "false"
                 :onclick (str "schedStar('" id "','" (clojure.core/name kind) "')")
                 :title (if star? "Unstar" "Mark special role")
                 :style (str "position:absolute; top:0; right:0; border:none; "
                             "background:none; cursor:pointer; font-size:1.15em; "
                             "padding:0.25em 0.45em; line-height:1; "
                             (if star? "color:#c9a227; opacity:1;" "color:#bbb; opacity:0.6;"))}
        (if star? "★" "☆")])
     ;; ＋ = the balance-label popover (Option A, bead 49bd). A plain HTML
     ;; details/summary disclosure — NO server UI state, NO open/close JS; the
     ;; server only re-renders active/inactive rows on SSE repaint.
     (when (contains? #{:block :talk} kind)
       [:details {:style "position:absolute; top:0; right:1.5em;"}
        [:summary {:draggable "false"
                   :title "Balance labels"
                   :style (str "list-style:none; cursor:pointer; font-size:1.05em; "
                               "line-height:1; padding:0.3em 0.35em; "
                               (if (seq labels) "color:#8a8578; opacity:1;" "color:#bbb; opacity:0.6;"))}
         "＋"]
        [:div {:style (str "position:absolute; right:0; top:1.6em; z-index:30; "
                           "background:#fff; border:1px solid #d9d7d2; border-radius:6px; "
                           "box-shadow:0 4px 12px rgba(0,0,0,0.15); padding:0.35em; "
                           "min-width:10em;")}
         (for [{:keys [key name color]} (schedule/rough-labels-for event)
               :let [active? (contains? labels key)]]
           [:button {:type "button"
                     :draggable "false"
                     :onclick (str "schedLabel('" id "','" key "','"
                                   (clojure.core/name kind) "')")
                     :style (str "display:flex; align-items:center; gap:0.4em; width:100%; "
                                 "text-align:left; border:none; cursor:pointer; "
                                 "padding:0.25em 0.35em; border-radius:4px; font-size:0.85em; "
                                 (if active? "background:#f0ede7; font-weight:700;" "background:none;"))}
            [:span {:style (str "width:0.7em; height:0.7em; border-radius:50%; flex:none; "
                                "background:" color ";")}]
            name
            (when active? [:span {:style "margin-left:auto;"} "✓"])])]])
     ;; A ghost is a person who has not been onboarded yet — the ⋯ menu is how
     ;; they stop being one, without a second form.
     (when ghost?
       (ghost-promote-menu event {:id id :label title}
                           {:labels labels :kind kind :variant :lane}))
     ;; The seat's computed time, projected softly — orientation, not commitment
     ;; (Gene: "make it really soft and grayed out").
     (when time
       [:div.rough-time {:style "position:absolute; bottom:0.3em; right:0.45em; color:#c4c0b8; font-size:0.72em;"}
        time])
     ;; Active balance labels as thin color chips, bottom-left (soft time stays
     ;; clear on the right).
     (when (seq labels)
       [:div {:style "position:absolute; bottom:0.35em; left:0.6em; display:flex; gap:3px;"}
        (rough-label-chips event labels)])
     [:div {:style "padding-right:1.2em;"} [:strong title]]
     (when who [:div.sub-meta who])
     (when ghost? [:div.sub-meta {:style "opacity:0.7;"} "placeholder"])]))

(defn- rough-lane
  "One half-day dropzone: speaker cards + a filled-of-capacity count that goes
   red when the half is over-stuffed (Gene, live: '0 of 6… and if we're over,
   make it red')."
  [event day half {:keys [placed blocks speaker-names orgs]}]
  (let [half-blocks (filter #(= half (rough-half (:start %))) blocks)
        half-placed (filter #(= half (rough-half (:start %))) placed)
        ;; A card is a speaker-named block OR a ghost reservation — a ghost's
        ;; label ("Jitesh — Highland Software") is deliberately NOT a program
        ;; speaker, so it earns its lane seat by the marker instead.
        speaker-blocks (filter #(or (:ghost %)
                                    (contains? speaker-names (:label %)))
                               half-blocks)
        open-blocks (filter #(= rough-open-label (:label %)) half-blocks)
        ;; @spec ROUGH-DRAG-012
        ;; A clock time is not globally over-capacity: Charlotte legitimately
        ;; uses 09:00. Only two filled occupants sharing one rendered seat
        ;; coordinate make the lane over-full.
        {:keys [filled open total over?]}
        (schedule/rough-lane-meter (concat speaker-blocks half-placed) open-blocks)
        lane-items (sort-by (juxt :start #(or (:talk-id %) (:submission-id %) (:id %)))
                            (concat (map #(assoc % :rough-kind
                                                 (if (:talk-id %) :talk :placed))
                                         half-placed)
                                    (map #(assoc % :rough-kind :block) speaker-blocks)
                                    (map #(assoc % :rough-kind :open)
                                         (filter #(= rough-open-label (:label %))
                                                 half-blocks))))]
    ;; min-height keeps Morning/Afternoon aligned across day lanes (Ann: the
    ;; different sizes were jarring).
    [:div.rough-lane {:style "flex:1; min-width:11em; min-height:13em; border:1px solid #d9d7d2; border-radius:8px; padding:0.6em; background:rgba(0,0,0,0.02);"
                      :data-day day
                      :data-half (name half)
                      :data-sentinel-seat (if (= :am half) 540 780)}
     [:div {:style "display:flex; justify-content:space-between; align-items:baseline; margin-bottom:0.4em;"}
      [:strong (if (= :am half) "Morning" "Afternoon")]
      [:span.field-hint {:style (when over? "color:#d13438; font-weight:700;")}
       (str filled " of " total " slot" (when (not= 1 total) "s") " filled"
            (when over? " — over"))]]
     ;; One time-sorted stream is the run of show. Canonical talks, historical
     ;; speaker/ghost blocks, and open seats must remain interleaved during the
     ;; additive migration; grouping by storage type would visibly reorder the
     ;; same schedule after adoption.
     [:div.rough-items
      (mapcat
       (fn [item]
         (let [kind (:rough-kind item)
               item-id (or (:talk-id item) (:submission-id item) (:id item))
               item-title (if (= :block kind)
                            (:label item)
                            (str/join ", " (map :name (:speakers item))))
               card (case kind
                      :open (rough-gap-card item)
                      (:talk :placed)
                      (rough-card event {:kind kind
                                         :id item-id
                                         :title item-title
                                          ;; @spec ROUGH-DRAG-013
                                          ;; Keep the same identity context that
                                          ;; the working rail showed. Presenter
                                          ;; order is stable; `distinct` collapses
                                          ;; a shared duo organization once.
                                         :who (->> (:speakers item)
                                                   (keep #(get orgs (:name %)))
                                                   (remove str/blank?)
                                                   distinct
                                                   (str/join ", ")
                                                   not-empty)
                                         :star? (:star item)
                                         :labels (:labels item)
                                         :start (:start item)
                                         :time (schedule/minutes->display (:start item))})
                      :block
                      (rough-card event {:kind :block
                                         :id item-id
                                         :title item-title
                                         :who (get orgs (:label item))
                                         :ghost? (boolean (:ghost item))
                                         :star? (boolean (:star item))
                                         :labels (schedule/block-labels item)
                                         :start (:start item)
                                         :time (schedule/minutes->display (:start item))}))]
           (if (= :open kind)
             [card]
             [(rough-insert-target
               {:kind :rail
                :before-id item-id
                :before-name item-title
                :seat (:start item)})
              card])))
       lane-items)
      (rough-insert-target {:kind :end})]]))

(defn- published-when-str
  "The publication moment in the EVENT's timezone — \"Sat 3:41pm\". A published
   time rendered in the server's zone is a fact about the machine, not the
   conference."
  [event inst]
  (if-not (instance? java.time.Instant inst)
    (str inst)
    (let [zone (try (java.time.ZoneId/of (str (or (:tz event) "UTC")))
                    (catch Exception _ (java.time.ZoneId/of "UTC")))]
      (-> (java.time.format.DateTimeFormatter/ofPattern
           "EEE h:mma" java.util.Locale/ENGLISH)
          (.withZone zone)
          (.format ^java.time.Instant inst)
          (str/replace "AM" "am")
          (str/replace "PM" "pm")))))

(defn unpublished-drift-banner
  "\"7 changes since you last published · Sat 3:41pm  [Publish schedule]\".

   Counts SCHEDULE-SHAPING facts only (schedule/schedule-shaping-fact-types) —
   a reviewer scoring a talk must never light this up, or it is ignored inside
   a day. Silent when the draft and the published agenda agree; a quieter
   never-published line before the first handoff. Lives INSIDE #rough-board so
   every board mutation repaints it over SSE with the lanes it describes."
  [event]
  (let [{:keys [published? published-at]
         drift-count :count} (schedule/unpublished-drift event)
        publish-button
        [:form {:method "post"
                :action (str "/api/events/" (:slug event) "/schedule/publish")
                :style "flex:none;"}
         [:button.ui.mini.primary.button {:type "submit"} "Publish schedule"]]]
    (cond
      (not published?)
      [:div.field-hint {:style (str "display:flex; gap:0.6em; align-items:center; "
                                    "margin-bottom:0.6em;")}
       [:span "Never published — attendees cannot see this agenda yet."]
       publish-button]

      (pos? drift-count)
      [:div {:style (str "display:flex; gap:0.6em; align-items:center; "
                         "margin-bottom:0.6em; padding:0.4em 0.6em; "
                         "border:1px solid #e0d5a8; border-radius:6px; "
                         "background:#fdf8e6;")}
       [:span {:style "font-weight:700;"}
        (str "⚠ " drift-count " change" (when (not= 1 drift-count) "s")
             " since you last published")]
       [:span.field-hint (str "· " (published-when-str event published-at))]
       publish-button]

      :else nil)))

(defn rough-board-lanes
  "The LANES morph target (#rough-board): red remaining count + day columns.
   Pushed alone over SSE (morph-safe restructure, Gene live): the static rail
   controls (Reserve form, Stamp palette) live OUTSIDE any morph id, so
   typing and armed stamps survive every repaint."
  [event {:keys [placed blocks program-speakers trayed]}]
  (let [speaker-names (into (set (map :name program-speakers))
                            (map #(:name (first (:speakers %))) trayed))
        ctx {:placed placed :blocks blocks :speaker-names speaker-names
             :orgs (into {} (map (juxt :name :company)) program-speakers)}]
    [:div#rough-board {:style "flex:1; min-width:0;"}
     ;; Draft vs published: the board says so before the organizer has to ask.
     (unpublished-drift-banner event)
     ;; The remaining-work meter (Gene+Ann amendment, live): open Keynote
     ;; seats across ALL event days (parking day excluded by construction —
     ;; only real event days are counted). Inside #rough-board so every SSE
     ;; repaint keeps it fresh.
     (let [days (schedule/event-days event)
           open-by-day (into {}
                             (for [d days]
                               [d (count (filter #(and (= d (:day %))
                                                       (= rough-open-label (:label %)))
                                                 blocks))]))
           total-open (reduce + 0 (vals open-by-day))]
       [:div {:style "margin-bottom:0.6em;"}
        (if (pos? total-open)
          (list
           [:span {:style "color:#d13438; font-weight:800; font-size:1.05em;"}
            (str total-open " slot" (when (not= 1 total-open) "s") " still to fill")]
           [:span.field-hint {:style "margin-left:0.5em;"}
            (str/join " " (for [d days]
                            (str "· " (schedule/day-label event d) ": "
                                 (get open-by-day d 0))))])
          [:span {:style "color:#1baf7a; font-weight:600;"} "Every slot filled"])])
     ;; Trello orientation (Ann, live): one VERTICAL lane per day, side by
     ;; side — Day 1 is lane one, Day 2 is lane two; AM over PM inside each.
     [:div {:style "display:flex; gap:1em; align-items:flex-start;"}
      (for [d (schedule/event-days event)]
        [:div {:style "flex:1; min-width:16em; display:flex; flex-direction:column; gap:0.7em;"}
         [:h4.ui.header {:style "margin:0 0 0.1em;"} (schedule/day-label event d)]
         (rough-lane event d :am (assoc ctx
                                        :placed (filter #(= d (:day %)) placed)
                                        :blocks (filter #(= d (:day %)) blocks)))
         (rough-lane event d :pm (assoc ctx
                                        :placed (filter #(= d (:day %)) placed)
                                        :blocks (filter #(= d (:day %)) blocks)))])]]))

(defn rough-rail-list
  "The SPEAKER-LIST morph target (#rough-rail): parked ghost reservations +
   the program-speaker rail. Separate from the static Reserve/Stamp controls
   above it so their browser-owned state is never morphed away."
  [event {:keys [placed blocks program-speakers]}]
  (let [on-board (into (set (map :label blocks))
                       (mapcat #(map :name (:speakers %)) placed))]
    [:div#rough-rail
     [:h4.ui.header {:style "margin:0 0 0.3em;"} "Speakers · " (count program-speakers)]
     [:div.field-hint {:style "margin-bottom:0.6em;"} "Drag onto a lane."]
     ;; Parked ghost reservations: draggable exactly like a speaker (the drop
     ;; is the ordinary rough-place move path, which preserves the marker).
     (for [g (filter #(and (:ghost %) (= schedule/rough-parking-day (:day %))) blocks)]
       ;; Ratified ghost treatment, unified with lane ghosts: gray fill +
       ;; dashed border + "placeholder" sub-line (gray = "still waiting").
       [:div.rail-speaker.rail-ghost
        {:style "cursor:grab; margin-bottom:0.4em; background:#f3f1ed; border:1px dashed #b3aea6; border-radius:6px; padding:0.4em 0.6em; box-shadow:0 1px 2px rgba(0,0,0,0.06);"
         :draggable "true"
         :data-name (:label g)
         :data-block-id (:id g)}
        [:div {:style "display:flex; justify-content:space-between; gap:0.4em; align-items:baseline;"}
         [:strong (:label g)]
         ;; Same ⋯ menu the lane ghosts carry — a parked placeholder is the
         ;; commonest place to promote from, because it is where a reserved
         ;; name lands the moment it is created.
         (ghost-promote-menu event g {:labels (schedule/block-labels g)
                                      :kind :block :variant :rail})
         ;; fetch-POST via schedRemoveGhost (main seat's JS): a bare form
         ;; navigation to the 204 showed Chrome's failed-download bar.
         [:button {:type "button" :title "Remove reservation"
                   :draggable "false"
                   :onclick (str "schedRemoveGhost('" (:id g) "')")
                   :style "border:none; background:none; cursor:pointer; padding:0; line-height:1;"}
          "✕"]]
        [:div.sub-meta {:style "opacity:0.7;"} "placeholder"]
        ;; Balance labels stamped while still parked stay visible in the rail.
        (let [glabels (schedule/block-labels g)]
          (when (seq glabels)
            [:div {:style "display:flex; gap:3px; margin-top:0.25em;"}
             (rough-label-chips event glabels)]))])
     ;; Placed speakers sink to the bottom, grayed — the working pool stays
     ;; on top (Ann: "push them to the bottom").
     ;; Unplaced = full cards (the working pool). Placed = small quiet chips,
     ;; half-width, plain weight (Gene: filled speakers need no bold, small
     ;; box) — presence acknowledged, attention released.
     [:div {:style "display:flex; flex-wrap:wrap; gap:0.35em; align-items:flex-start;"}
      (for [sp (sort-by (fn [sp] [(if (contains? on-board (:name sp)) 1 0) (:name sp)])
                        program-speakers)
            :let [placed? (contains? on-board (:name sp))]]
        (if placed?
          [:div.rail-speaker
           {:style "cursor:grab; flex:0 0 calc(50% - 0.2em); box-sizing:border-box; background:#f7f5f1; border:1px solid #e4e1da; border-radius:5px; padding:0.2em 0.45em; font-size:0.8em; color:#8a877f; opacity:0.75; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;"
            :draggable "true"
            :data-name (:name sp)
            :data-person-id (:id sp)
            :title (str (:name sp) " — placed")}
           (:name sp)]
          [:div.rail-speaker
           {:style "cursor:grab; flex:0 0 100%; box-sizing:border-box; background:#fff; border:1px solid #d9d7d2; border-radius:6px; padding:0.4em 0.6em; box-shadow:0 1px 2px rgba(0,0,0,0.06);"
            :draggable "true"
            :data-name (:name sp)
            :data-person-id (:id sp)}
           [:div [:strong (:name sp)]]
           [:div.sub-meta (:company sp)]]))]]))

(defn rough-stamps
  "The Stamp palette fragment (#rough-stamps) — its own morph target, pushed
   ONLY by the rename flows (never by board mutations, preserving the
   morph-safe typing/armed-stamp guarantee; a rename push DOES reset the
   armed data-armed attribute — accepted). `editing-key` (per-person, from
   the handler's edit-state atom) swaps that chip for an inline rename form:
   server-owned editing state, the live-drafts house pattern, not client DOM."
  [event editing-key]
  [:div#rough-stamps {:style "margin-bottom:0.8em; padding:0.5em 0.6em; border:1px solid #d9d7d2; border-radius:6px;"}
   [:div {:style "font-weight:700; margin-bottom:0.3em;"} "Stamp"]
   [:div.field-hint {:style "margin-bottom:0.35em;"}
    "Arm a label, then click cards. Esc stops. Double-click a chip to rename."]
   [:div {:style "display:flex; flex-wrap:wrap; gap:0.3em;"}
    (for [{:keys [key name color visibility]} (schedule/rough-labels-for event)]
      (if (= key editing-key)
        ;; Inline rename form. NEVER a bare form POST (the Chrome-204
        ;; download bar): schedRenameSave/schedRenameCancel fetch-POST.
        [:form {:onsubmit (str "return schedRenameSave(this,'" key "')")
                :style "display:flex; gap:0.25em; align-items:center; flex-wrap:wrap;"}
         [:span {:style (str "width:0.65em; height:0.65em; border-radius:50%; flex:none; "
                             "background:" color ";")}]
         [:input {:type "text" :name "name" :value name :maxlength "24"
                  :autofocus true
                  :style "width:7.5em; font-size:0.82em;"}]
         [:select {:name "visibility" :style "font-size:0.82em;"}
          [:option {:value "organizer" :selected (= :organizer visibility)}
           "Organizer only"]
          [:option {:value "committee-ok" :selected (= :committee-ok visibility)}
           "Committee may see"]]
         [:button.ui.mini.button {:type "submit"} "Save"]
         [:button.ui.mini.button {:type "button" :onclick "schedRenameCancel()"}
          "Cancel"]]
        [:button.sched-stamp-chip {:type "button"
                                   :draggable "false"
                                   :data-stamp-key key
                                   :onclick (str "schedStampArm('" key "')")
                                   :ondblclick (str "schedRenameOpen('" key "')")
                                   :title (if (= :organizer visibility)
                                            (str name " — organizer only. Double-click to rename.")
                                            (str name " — committee may see. Double-click to rename."))
                                   :style (str "display:inline-flex; align-items:center; gap:0.3em; "
                                               "border:1px solid #d9d7d2; border-radius:999px; "
                                               "background:#fff; cursor:pointer; font-size:0.78em; "
                                               "padding:0.2em 0.55em;")}
         [:span {:style (str "width:0.65em; height:0.65em; border-radius:50%; flex:none; "
                             "background:" color ";")}]
         name
         ;; 🔒 = organizer-only visibility (the only visibility UI for now).
         (when (= :organizer visibility)
           [:span {:style "font-size:0.85em; opacity:0.7;"} "🔒"])]))]])

(defn rough-board
  "Lanes left (Day 1 / Day 2, AM+PM), speaker rail right. MORPH-SAFE SPLIT
   (Gene live): the outer wrapper is static; #rough-board (lanes) and
   #rough-rail (speaker list) are the two SSE morph targets, while the
   Reserve form and Stamp palette render once per page load so typing and
   armed stamps survive every repaint."
  [event model]
  [:div {:style "display:flex; gap:1.2em; align-items:flex-start;"}
   (rough-board-lanes event model)
   [:div {:style "width:15em; flex:none;"}
    ;; Reserve a spot (Gene+Ann, live 2026-08-18): NAME ONLY — "just another
    ;; card". Submitting parks a GHOST block on schedule/rough-parking-day;
    ;; it appears below as a draggable rail card and reaches a lane only by
    ;; drag. No day/half/constraint fields here — deliberately. STATIC: never
    ;; inside a morph target, so mid-typing input survives SSE pushes.
    ;; fetch-POST via schedReserve (main seat's JS): a bare form navigation
    ;; to the 204 showed Chrome's failed-download bar (Gene hit it 3x).
    [:form {:onsubmit "return schedReserve(this)"
            :style "margin-bottom:0.8em; padding:0.5em 0.6em; border:1px dashed #b3aea6; border-radius:6px;"}
     [:div {:style "font-weight:700; margin-bottom:0.3em;"} "Create placeholder speaker"]
     [:input {:type "text" :name "name"
              :placeholder "Jitesh — Highland Software"
              :data-ghost-fill ""
              :style "width:100%; margin-bottom:0.3em;"}]
     [:button.ui.mini.button {:type "submit"} "Create"]]
    ;; Stamp mode (Option C, bead 49bd): arm a balance label, then click
    ;; cards to apply it. The armed state is EPHEMERAL browser-owned tool
    ;; mode (like a cursor) — a JS var in sched-rough.js, never the server;
    ;; the label facts themselves are server-owned. The palette lives in its
    ;; OWN fragment (#rough-stamps), pushed only by the rename flows.
    (rough-stamps event nil)
    (rough-rail-list event model)]
   [:div {:hidden true}
    [:script {:src "/js/sched-rough-plan.js?v=1" :defer true}]
    [:script {:src "/js/sched-rough.js?v=14" :defer true}]]])

(defn- ft-row
  "Fine Tune run-of-show row (Gene-ratified mocks, 2026-08-18): computed time
   in gray, DURATION box right beside it (the only editable number), name+org,
   then the one removal affordance — [rail] for people/ghosts, tiny x for ops
   rows. Times never typed; no ripple yet (cascade = bead hdra)."
  [event {:keys [b kind who]}]
  (let [dur (- (:end b) (:start b))
        speaker? (= kind :speaker)
        talk? (= :talk (:item-kind b))
        ghost? (:ghost b)
        open? (= kind :open)]
    [:div {:id (str "ft-" (:id b))
           :style (str "display:flex; align-items:center; gap:0.8em; "
                       "padding:0.45em 0.6em; border:1px solid "
                       (cond open? "#e6a5b2; background:#fdf0f2; border-style:dashed;"
                             ghost? "#b3aea6; background:#f3f1ed; border-style:dashed;"
                             (= kind :ops) "#e2ddd4; background:#f7f5f1;"
                             :else "#d9d7d2; background:#fff;")
                       " border-radius:6px; margin-bottom:0.35em;")}
     [:span {:style "width:5.2em; flex:none; color:#9a968e; font-variant-numeric:tabular-nums;"}
      (schedule/minutes->display (:start b))]
     [:input {:type "number" :min "5" :max "480" :step "5" :value dur
              :onchange (str "schedDur(this,'" (:id b) "','"
                             (if talk? "talk" "block") "')")
              :style "width:4.2em; flex:none; font-size:0.85em; padding:0.15em 0.3em;"
              :title "Duration (minutes) — the only editable number"}]
     [:div {:style "flex:1; min-width:0;"}
      [:strong {:style (when open? "color:#c46a7c; font-weight:600;")}
       (if open? "Open slot" (:label b))]
      (cond
        ghost? [:span.sub-meta {:style "margin-left:0.6em; opacity:0.7;"} "placeholder"]
        (and speaker? who) [:span.sub-meta {:style "margin-left:0.6em;"} who])]
     (when (:star b) [:span {:style "color:#c9a227;"} "\u2605"])
     (cond
       (or speaker? ghost?)
       [:button {:type "button"
                 :onclick (str "schedToRail('" (:id b) "','"
                               (if talk? "talk" "block") "')")
                 :title "Return to the rail — the seat reopens"
                 :style "border:1px solid #d9d7d2; background:none; border-radius:5px; font-size:0.8em; padding:0.15em 0.6em; cursor:pointer; color:#6b6a65;"}
        "rail"]
       (= kind :ops)
       [:button {:type "button" :onclick (str "schedRemoveGhost('" (:id b) "')")
                 :title "Delete this row"
                 :style "border:none; background:none; cursor:pointer; color:#bbb; font-size:0.9em;"}
        "\u2715"])]))

(defn- fine-tune-list
  "Single-track Fine Tune: the day as one run-of-show list + the SAME rail as
   Rough Blocking (DRY — one rail, both modes). Replaces the room-grid when
   the event has no rooms; roomed events keep the legacy grid."
  [event day {:keys [placed blocks program-speakers] :as model}]
  (let [speakers (into #{} (map :name) program-speakers)
        orgs (into {} (map (juxt :name :company)) program-speakers)
        day-blocks (filter #(= day (:day %)) blocks)
        day-talks (->> placed
                       (filter #(and (= day (:day %)) (:talk-id %)))
                       (map #(assoc %
                                    :id (:talk-id %)
                                    :label (str/join ", " (map :name (:speakers %)))
                                    :item-kind :talk)))
        day-items (sort-by :start (concat day-blocks day-talks))]
    [:div {:style "display:flex; gap:1.2em; align-items:flex-start;"}
     [:div#ft-list {:style "flex:1; min-width:0;"}
      [:h4.ui.header {:style "margin:0 0 0.4em;"}
       "Run of show — " (schedule/day-label event day)]
      ;; Column headers (Gene: one header naming time/duration/speaker)
      [:div {:style "display:flex; gap:0.8em; padding:0 0.6em 0.25em; font-size:0.72em; letter-spacing:0.05em; text-transform:uppercase; color:#9a968e;"}
       [:span {:style "width:5.2em; flex:none;"} "Time"]
       [:span {:style "width:4.2em; flex:none;"} "Min"]
       [:span {:style "flex:1;"} "Speaker / session"]]
      (when (empty? day-items)
        [:div.empty-state
         "Nothing placed on this day yet. Drag speakers in from Rough Blocking."])
      (for [b day-items]
        (ft-row event {:b b
                       :kind (cond (= :talk (:item-kind b)) :speaker
                                   (:ghost b) :ghost
                                   (speakers (:label b)) :speaker
                                   (= "Keynote" (:label b)) :open
                                   :else :ops)
                       :who (get orgs (:label b))}))]
     [:div {:style "width:15em; flex:none;"}
      (rough-rail-list event model)]]))

(defn schedule-page
  [event {:keys [day stats conflicts rooms tracks placed blocks trayed placement-chase-list conflicted-ids
                 person locked? lock-version withheld-count published-at auto-place-report editable?
                 mode program-speakers] :as model}]
  (let [editable? (not= false editable?)
        {:keys [day-start day-end]} (events/day-hours event)
        ;; Board slot arithmetic (Gene: the top line + the pills own it):
        ;; a SEAT is a card-block (speaker/ghost) or an open Keynote; parking
        ;; never counts.
        speaker-names (into #{} (map :name) program-speakers)
        seat-blocks (remove #(= schedule/rough-parking-day (:day %)) blocks)
        card? #(or (:ghost %) (contains? speaker-names (:label %)))
        seats (filter #(or (card? %) (= "Keynote" (:label %))) seat-blocks)
        slot-summary {:total (count seats)
                      :filled (count (filter card? seats))
                      :open (count (filter #(= "Keynote" (:label %)) seats))}
        day-counts (into {}
                         (for [[d bs] (group-by :day seats)]
                           [d {:filled (count (filter card? bs))
                               :total (count bs)}]))]
    (organizer-layout/organizer-shell
     (str "Schedule — " (:name event))
     {:event event :active :schedule :person person :crumb "Schedule" :sse? true
      :body-attrs (ds/sse-mount (:id event))}

      ;; Publish rides the upper right (Gene, live 2026-08-18) — reachable from
      ;; BOTH modes; the bottom "Public agenda" segment keeps the full story.
     [:div {:style "display:flex; gap:1em; align-items:flex-start;"}
      [:div {:style "flex:1; min-width:0;"}
       (organizer-layout/header "Schedule"
                                (if editable?
                                  "Draft-first. Place things half-decided; the arithmetic and the clashes keep up."
                                  "The working program, rooms, and conflicts — view only."))]
      (when editable?
        [:div {:style "flex:none; text-align:right;"}
         [:form {:method "post"
                 :action (str "/api/events/" (:slug event) "/schedule/publish")}
          [:button.ui.primary.button {:type "submit"} "Publish"]]
         (when published-at
           [:div.field-hint {:style "margin-top:0.3em;"} "Published ✓"])
          ;; What attendees see (or will see) — always available, new tab.
         [:div.field-hint {:style "margin-top:0.3em;"}
          [:a {:href (str "/program/" (:slug event) "#agenda") :target "_blank"}
           "Preview →"]]])]

      ;; The sibling of policy/review-workflow-positioning: same box, same
      ;; register, the scheduling story. Copy = Gene-picked Draft B, amended
      ;; live ("it's Ann and me figuring out the rough shape... then Ann
      ;; painfully inputting it"). details/summary: quiet for humans, fully
      ;; present in the DOM for agents reading source.
     [:aside#schedule-positioning.ui.info.message
      [:div.header "How Curtain Call schedules"]
      [:p
       "For over a decade, every program started the same way: Gene and Ann
        working out the rough shape in a Google Sheet \u2014 ten-plus years of
        sensibilities about who opens, who anchors, who can only speak
        mornings \u2014 and then Ann painfully re-keying the result into the
        scheduling tool: 37 sessions, hand-clicked one at a time against a
        15-minute picker. About 2.5 hours, twice a year, every year. The
        first time she dragged a speaker onto this board and the schedule
        just happened, she went quiet in a way we\u2019re still not over."]
      [:details.review-achieve
       [:summary "Why scheduling tools get this wrong, and what we built
        instead. More\u2026"]
       [:p "Schedule builders demand complete data \u2014 a session can\u2019t
        exist without a room and a time \u2014 but schedule BUILDING is a
        negotiation full of half-decisions. So this board works the way the
        negotiation does: rough-block speakers into mornings and afternoons
        like cards on a table; reserve gray placeholders for people whose
        yes hasn\u2019t arrived (they never touch a public page); keep the
        open slots visible and movable, because deciding where the hole
        goes is a scheduling decision; then fine-tune durations while every
        start time computes itself. The full story of the day this was
        built \u2014 with the mockups that specced it \u2014 is in "
        [:a {:href "https://claude.ai/code/artifact/b9e56535-530a-423d-915e-84d97f4a0667"
             :target "_blank" :rel "noopener"}
         "the design deck"] "."]]]

     (when locked?
       [:div.locked-banner
        [:div [:strong "Locked — " (or lock-version "v1")]
         [:div.field-hint "The draft is frozen. Unlock to keep moving things."]]
        (when editable?
          [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/unlock")}
           [:button.ui.small.button {:type "submit"} "Unlock"]])])

      ;; The one line Gene reads first: talks / filled / open, whole event.
     (when (pos? (:total slot-summary))
       [:div {:style "display:flex; gap:0.6em; align-items:baseline; margin-bottom:0.4em; font-size:1.05em;"}
        [:span [:strong (:total slot-summary)] " talk slots"]
        [:span {:style "color:#b3aea6;"} "\u00b7"]
        [:span [:strong (:filled slot-summary)] " filled"]
        [:span {:style "color:#b3aea6;"} "\u00b7"]
        [:span {:style (when (pos? (:open slot-summary)) "color:#d13438; font-weight:700;")}
         (:open slot-summary) " open"]])
      ;; The empty-tray truth, restored to the new surface (doctrine #3 /
      ;; CI pin): accepted-but-not-told is the one number that explains a
      ;; thinner-than-expected rail.
      ;; The GOOD-news half of the empty-tray truth (its pin): everyone
      ;; accepted and informed has a seat.
     (when (and (empty? trayed) (zero? (or (:awaiting-inform stats) 0))
                (pos? (:accepted stats)))
       [:div.field-hint {:style "margin-bottom:0.4em;"}
        "Everything accepted has a place."])
     (when (pos? (or (:awaiting-inform stats) 0))
       (let [n (:awaiting-inform stats) many? (not= 1 n)]
         [:div {:style "border-left:3px solid #b45309; background:#fdf6ec; padding:0.5em 0.8em; border-radius:0 6px 6px 0; margin-bottom:0.5em; font-size:0.92em;"}
          [:strong (str n " accepted talk" (when many? "s") " cannot be scheduled yet")]
          (str " — the speaker" (when many? "s") " ha" (if many? "ve" "s") " not been told. ")
          [:a {:href (str "/events/" (:slug event) "/inform")} "Inform speakers \u2192"]]))
     (schedule-status-bar stats)
     (list
      (when auto-place-report
        (let [{:keys [placed unplaceable]} auto-place-report
              placed-one? (= "1" placed)
              unplaceable-one? (= "1" unplaceable)]
          [:div.ui.message
           [:div.header (str placed " unscheduled session"
                             (when-not placed-one? "s") " auto-placed")]
           (if (= "0" unplaceable)
             "Every schedulable session now has a place."
             (str unplaceable " session" (when-not unplaceable-one? "s")
                  " could not be placed in an open room/time slot. Add capacity or place "
                  (if unplaceable-one? "it" "them") " manually."))]))
      (conflict-chips event conflicts)
      (when (pos? (or withheld-count 0))
          ;; The loud half of "withhold, loudly": conflicted sessions leave the
          ;; public agenda/exports, and this line is why nothing vanishes silently.
        [:div.withheld-note {}
         [:strong (str withheld-count " session" (when (not= 1 withheld-count) "s"))]
         " held back from the public agenda and exports until the conflict is
       resolved — attendees never see a schedule we know is wrong."]))

     (if (empty? (schedule/event-days event))
       [:div.ui.warning.message
        [:div.header "This event has no dates yet"]
        [:p "Set start and end dates and the day tabs will appear."]]

       (list
          ;; Mode tabs: rough cut (Trello lanes, no times) vs tweezers (the grid).
          ;; Rough cut leads (Gene, live 2026-08-18: "You never use tweezers for
          ;; a rough cut") — same hrefs, work-order order.
        [:div.day-tabs {:style "margin-bottom:0.6em;"}
         [:a.chip {:class (when (= "rough" mode) "on")
                   :href (str "/events/" (:slug event) "/schedule?mode=rough")} "Rough Blocking"]
         [:a.chip {:class (when (not= "rough" mode) "on")
                   :href (str "/events/" (:slug event) "/schedule?mode=fine")} "Fine Tune Schedule"]]

        (if (= "rough" mode)
          (rough-board event model)
          (list
           [:div.day-tabs {}
            (for [d (schedule/event-days event)] (day-tab event d day day-counts))]

              ;; v0 rail (Ann-call, 2026-08-18): grid left, speakers right. The rail
              ;; is the placeable pool — accepted + informed, not yet on the grid.
           (if (and (empty? rooms) (empty? placed))
                ;; ROLLOUT SAFETY (Gene, hyper-paranoid by request): the run-of-
                ;; show list renders BLOCKS; an existing user whose program is
                ;; placed SESSIONS must keep the surface that shows their work.
                ;; Fine Tune's list activates only where it can show everything.
                ;; Single-track Fine Tune (Gene-ratified mocks): run-of-show rows
                ;; with time+duration, shared rail. The Fill-dropdown era ends here.
             (fine-tune-list event day model)
             [:div {:style "display:flex; gap:1.5em; align-items:flex-start;"}
              [:div {:style "flex:1; min-width:0;"}
               (schedule-grid event day {:rooms rooms :placed placed
                                         :blocks blocks
                                         :conflicted-ids conflicted-ids
                                         :editable? editable?
                                         :locked? locked?
                                         :trayed (when (and editable? (not locked?)) trayed)})]
              (when (and editable? (not locked?))
                [:div {:style "width:16em; flex:none;"}
                 [:h4.ui.header {:style "margin:0 0 0.3em;"}
                  "Speakers · " (count trayed)]
                 [:div.field-hint {:style "margin-bottom:0.6em;"}
                  "Accepted & informed, not yet placed. Use "
                  [:strong "Fill"] " on a slot to the left."]
                 (if (empty? trayed)
                   [:div.empty-state "No unplaced speakers."]
                   (for [sub trayed]
                     [:div.tray-card {}
                      [:div [:strong (:name (first (:speakers sub)))]]
                      [:div.sub-meta (get-in sub [:answers :talk-title])]
                      [:div.sub-meta (get-in sub [:answers :session-format])]]))])])))

        (when (and editable? (not locked?) (seq trayed) (seq rooms))
          [:div.ui.info.message
           [:div.header "Auto-place unscheduled"]
           [:p (str "Greedily fill open " day-start "–" day-end " room slots without moving anything already placed. ")
            "Sessions that do not fit remain in the tray with an honest count after this action."]
           [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/suggest")}
            [:input {:type "hidden" :name "day" :value day}]
            [:button.ui.button {:type "submit"} "Auto-place unscheduled"]]])

        (when (seq placement-chase-list)
          [:div.ui.segment
           [:h4.ui.header "Placement confirmations: " (count placement-chase-list)]
           [:div.field-hint
            "These informed speakers have a current placement but have not responded. "
            "This is a chase list only; it never sends an automatic reminder."]
           [:ul
            (for [{:keys [speaker-name title day start end]} placement-chase-list]
              [:li [:strong speaker-name] " — " title " · "
               (schedule/day-label event day) " · "
               (schedule/time-range-display start end)])]])

        (when (and editable? (not locked?) (seq rooms))
          [:div.tray {}
           [:h4.ui.header {:style "margin-bottom:0.3em;"}
            "Accepted (unscheduled): " (count trayed)]
           [:div.field-hint {:style "margin-bottom:0.6em;"}
            "Only accepted speakers who have been informed appear here — the agenda "
            "is downstream of the promise, never ahead of it."]
           (if (empty? trayed)
               ;; An empty tray has two very different causes and only one of
               ;; them is good news. Saying "everything accepted has a place"
               ;; to an organizer who has accepted ten talks and informed none
               ;; of them is a lie that dead-ends the whole downstream walk —
               ;; nothing schedules, so nothing publishes, so every public
               ;; surface is empty for a reason the page refused to name.
             (let [awaiting (or (:awaiting-inform stats) 0)
                   many? (not= 1 awaiting)]
               (if (pos? awaiting)
                 [:div.empty-state
                  [:strong (str awaiting " accepted talk" (when many? "s")
                                " cannot be scheduled yet")]
                  (str " — the speaker" (when many? "s") " ha" (if many? "ve" "s")
                       " not been told. Accepting is the decision; informing is "
                       "the promise, and the agenda follows the promise. ")
                  [:a {:href (str "/events/" (:slug event) "/inform")}
                   "Inform speakers →"]
                  " and they land in this tray."]
                 [:div.empty-state "Everything accepted has a place. "
                  (when (pos? (:unroomed stats))
                    (str (:unroomed stats) " still need a room, which is fine."))]))
             (for [sub trayed]
               [:div.tray-card {}
                [:div [:strong (get-in sub [:answers :talk-title])]]
                [:div.sub-meta (:name (first (:speakers sub)))
                 " · " (get-in sub [:answers :session-format])]
                (place-form event sub day rooms)]))])

        (when (and editable? (not locked?))
          [:div.ui.segment {}
           [:h4.ui.header "Rooms & blocks"]
           [:div {:style "display:flex; gap:2em; flex-wrap:wrap;"}
            [:div {:style "flex:1; min-width:16em;"}
             [:h5.ui.header "Rooms"]
             (if (seq rooms)
               [:div.member-list
                (for [r rooms]
                  [:div.member-row {}
                   [:div.member-who [:span.member-name (:name r)]]
                   [:form {:method "post"
                           :action (str "/api/events/" (:slug event) "/schedule/room-remove")}
                    [:input {:type "hidden" :name "room-id" :value (:id r)}]
                    [:button.ui.mini.basic.button {:type "submit"} "Remove"]]])]
               [:p.field-hint "No rooms yet — sessions can still be placed unroomed."])
             [:form.place-form {:method "post"
                                :action (str "/api/events/" (:slug event) "/schedule/room-add")}
              [:input {:type "text" :name "name" :placeholder "Main Stage" :required true
                       :data-ghost-fill ""}]
              [:button.ui.mini.button {:type "submit"} "Add room"]]]

            [:div {:style "flex:1; min-width:20em;"}
             [:h5.ui.header "Add a block"]
             [:div.field-hint {:style "margin-bottom:0.4em;"}
              "Lunch, Keynote TBD, Break — placeholders that hold space for "
              "something not yet decided."]
             [:form.place-form {:method "post"
                                :action (str "/api/events/" (:slug event) "/schedule/block-add")}
              [:input {:type "text" :name "label" :placeholder "Lunch" :required true
                       :data-ghost-fill ""}]
              [:select {:name "day"}
               (for [d (schedule/event-days event)]
                 [:option (cond-> {:value d} (= d day) (assoc :selected true))
                  (schedule/day-label event d)])]
              [:input {:type "time" :name "start" :step "60" :value "12:00" :required true}]
              [:input {:type "number" :name "duration" :min "5" :step "5" :value "60"
                       :style "width:5em;"}]
              [:select {:name "room-id"} (room-options rooms nil)]
              [:button.ui.mini.button {:type "submit"} "Add block"]]]

            [:div {:style "flex:1; min-width:20em;"}
             [:h5.ui.header "Track management"]
             [:div.field-hint {:style "margin-bottom:0.4em;"}
              "One canonical track list feeds the CFP, review filters, schedule, and public facets."]
             (if (seq tracks)
               [:div.member-list
                (for [track tracks]
                  [:div.member-row {}
                   [:form.place-form {:method "post"
                                      :action (str "/api/events/" (:slug event) "/schedule/track-rename")}
                    [:input {:type "hidden" :name "old-label" :value track}]
                    [:input {:type "text" :name "new-label" :value track :required true}]
                    [:button.ui.mini.basic.button {:type "submit"} "Rename"]]
                   [:form {:method "post"
                           :action (str "/api/events/" (:slug event) "/schedule/track-retire")}
                    [:input {:type "hidden" :name "label" :value track}]
                    [:button.ui.mini.basic.button {:type "submit"} "Retire"]]])]
               [:p.field-hint "No active tracks."])
             [:form.place-form {:method "post"
                                :action (str "/api/events/" (:slug event) "/schedule/track-add")}
              [:input {:type "text" :name "label" :placeholder "Platform Engineering" :required true
                       :data-ghost-fill ""}]
              [:button.ui.mini.button {:type "submit"} "Add track"]]]]])

        (when (and editable? (not locked?))
          [:div {:style "margin-top:1.5em;"}
           [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/lock")}
            [:button.ui.button {:type "submit"} "Lock schedule"]]
           [:div.field-hint {:style "margin-top:0.4em;"}
            "Locking freezes the draft and stamps a version. The "
            [:a {:href (str "/events/" (:slug event) "/log")} "Log"] " narrates every change."]])

        [:div.ui.segment {}
         [:h4.ui.header "Public agenda"]
         (if published-at
           [:div.ui.positive.message "Published ✓"]
           [:div.field-hint "Only accepted speakers who have been informed will appear."])
         (when editable?
           [:form {:method "post"
                   :action (str "/api/events/" (:slug event) "/schedule/publish")}
            [:button.ui.primary.button {:type "submit"} "Publish"]])
         [:a.ui.basic.button {:href (str "/agenda/" (:slug event)) :target "_blank"}
          "View the public agenda"]])))))
