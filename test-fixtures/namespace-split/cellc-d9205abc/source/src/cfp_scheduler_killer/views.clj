(ns cfp-scheduler-killer.views
  "Hiccup views — everything is server-rendered.

   House rules that shape this file (global CLAUDE.md, the Datastar NEVERs):
     - No client-side DOM mutation, no setTimeout UI, no client validation.
     - The server decides what to show; the browser just displays it.
   This slice uses plain form POST + 303 redirect. SSE arrives with the board."
  (:require
   [cfp-scheduler-killer.committees :as committees]
   [cfp-scheduler-killer.events :as events]
   [cfp-scheduler-killer.exports :as exports]
   [cfp-scheduler-killer.forms :as forms]
   [cfp-scheduler-killer.portal :as portal]
   [cfp-scheduler-killer.reviews :as reviews]
   [cfp-scheduler-killer.schedule :as schedule]
   [cfp-scheduler-killer.submissions :as submissions]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [datastar-kit.ds :as ds]
   [hiccup.page :as page]
   [hiccup2.core :as h])
  (:import
   (java.time LocalDate ZoneId)
   (java.time.format DateTimeFormatter)))

(defn versioned
  "Add cache-busting query param to a URL."
  [url]
  (str url "?v=" (System/currentTimeMillis)))

(def favicon-data-uri
  "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🎤</text></svg>")

(defn page-shell
  "HTML page skeleton with Fomantic UI. body-content is one or more hiccup forms."
  [title & body-content]
  (str
   (h/html
    (page/doctype :html5)
    [:html {:lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:title title]
      [:link {:rel "icon" :href favicon-data-uri}]
      [:link {:rel "stylesheet"
              :href "https://cdn.jsdelivr.net/npm/fomantic-ui@2.9.3/dist/semantic.min.css"}]
      [:script {:src "https://code.jquery.com/jquery-3.6.0.min.js"}]
      [:script {:src "https://cdn.jsdelivr.net/npm/fomantic-ui@2.9.3/dist/semantic.min.js"}]
      [:script {:src (versioned "/js/datastar-kit.js")}]
      [:script {:src (versioned "/js/keyboard.js") :defer true}]
      [:script {:src (versioned "/js/ghost-fill.js") :defer true}]
      [:link {:rel "stylesheet" :href (versioned "/css/app.css")}]]
     [:body
      [:div.ui.container {:style "margin-top: 2em; margin-bottom: 4em;"}
       body-content]]])))

;; --- Formatting helpers -----------------------------------------------------

(def ^:private date-fmt (DateTimeFormatter/ofPattern "MMM d, yyyy"))
(def ^:private datetime-fmt (DateTimeFormatter/ofPattern "MMM d, yyyy h:mm a"))

(defn ->local-date
  "Coerce a java.sql.Date / LocalDate / nil into a LocalDate."
  [d]
  (cond
    (nil? d) nil
    (instance? LocalDate d) d
    (instance? java.sql.Date d) (.toLocalDate ^java.sql.Date d)
    :else nil))

(defn fmt-date [d]
  (some-> (->local-date d) (.format date-fmt)))

(defn fmt-date-range
  "\"Oct 14–15, 2026\"-ish. Honest about missing halves."
  [starts ends]
  (let [s (fmt-date starts) e (fmt-date ends)]
    (cond
      (and s e (= s e)) s
      (and s e) (str s " – " e)
      s (str s " – ?")
      e (str "? – " e)
      :else nil)))

(defn fmt-instant
  "Render a timestamptz in the event's own time zone."
  [ts tz]
  (when ts
    (let [inst (cond
                 (instance? java.sql.Timestamp ts) (.toInstant ^java.sql.Timestamp ts)
                 (instance? java.time.Instant ts) ts
                 :else nil)]
      (when inst
        (str (.format (.atZone ^java.time.Instant inst
                               (ZoneId/of (if (events/valid-timezone? tz) tz "UTC")))
                      datetime-fmt))))))

(defn ->instant
  "Coerce whatever a timestamp arrived as — ISO string, Instant, sql Timestamp —
   into an Instant. nil for anything unparseable, so a bad value renders as a
   dash rather than throwing on a page."
  [x]
  (cond
    (nil? x) nil
    (instance? java.time.Instant x) x
    (instance? java.sql.Timestamp x) (.toInstant ^java.sql.Timestamp x)
    (string? x) (try (java.time.Instant/parse x) (catch Exception _ nil))
    :else nil))

(def ^:private when-fmt (DateTimeFormatter/ofPattern "MMM d, h:mm a"))

(defn relative-when
  "\"just now\" / \"5m ago\" / \"2h ago\" / \"3d ago\", or nil once it stops being
   useful. Rendered on the SERVER at request time — a JS clock would be a second
   source of truth ticking against the page."
  [inst now]
  (when (and inst now)
    (let [secs (max 0 (.between java.time.temporal.ChronoUnit/SECONDS inst now))]
      (cond
        (< secs 60) "just now"
        (< secs 3600) (str (quot secs 60) "m ago")
        (< secs 86400) (str (quot secs 3600) "h ago")
        (< secs (* 7 86400)) (str (quot secs 86400) "d ago")
        :else nil))))

(defn fmt-when
  "The one timestamp format for the whole app: \"Aug 9, 7:15 AM\" in the event's
   own zone, with a relative hint while it is still recent."
  ([x tz] (fmt-when x tz (java.time.Instant/now)))
  ([x tz now]
   (when-let [inst (->instant x)]
     (let [zone (ZoneId/of (if (events/valid-timezone? tz) tz "UTC"))
           absolute (.format (.atZone inst zone) when-fmt)]
       (if-let [rel (relative-when inst now)]
         (str absolute " · " rel)
         absolute)))))

(defn fmt-cfp-window [event]
  (let [{:keys [cfp-opens-at cfp-closes-at tz]} event
        o (fmt-instant cfp-opens-at tz)
        c (fmt-instant cfp-closes-at tz)]
    (cond
      (and o c) (str o " → " c)
      o (str "opens " o)
      c (str "closes " c)
      :else nil)))

(def ^:private iso-date-fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(defn fmt-close-date
  "The stored close INSTANT as the yyyy-MM-dd an <input type=date> wants, read
   back in the event's own zone. The instant is 23:59:59 local, so the date it
   round-trips to is the date the organizer originally picked."
  [ts tz]
  (when-let [inst (->instant ts)]
    (.format (.toLocalDate (.atZone ^java.time.Instant inst
                                    (ZoneId/of (if (events/valid-timezone? tz) tz "UTC"))))
             iso-date-fmt)))

(defn cfp-public-url
  "The public CFP address we show organizers. `host` comes from the request."
  [host slug]
  (str host "/cfp/" slug))

(defn event-resume-path
  "Where clicking THE EVENT lands (Gene, 2026-08-09): the first unfinished
   setup step — the form until it's marked reviewed, then the committee until
   a second reviewer exists — and the dashboard only once setup is done. Same
   derivation as the sidebar's wizard; links must agree with the spine."
  [event]
  (let [slug (:slug event)]
    (cond
      (not (forms/reviewed? (:id event))) (str "/events/" slug "/form")
      (<= (committees/reviewer-count-for-event (:id event)) 1) (str "/events/" slug "/committee")
      :else (str "/events/" slug))))

(defn event-setup-done?
  "True once the wizard is over — clicking the event goes straight to the
   dashboard."
  [event]
  (= (event-resume-path event) (str "/events/" (:slug event))))

;; --- Organizer chrome -------------------------------------------------------
;;
;; The sidebar is event-scoped inside an event and minimal outside it. The
;; active item is decided SERVER-side (handlers pass :active) — nothing here
;; inspects the URL in the browser. Disabled items are greyed spans with a
;; title tooltip, never links: a nav item that goes nowhere is a lie.

(defn- sb-link [active? label href]
  [:a.sb-item {:class (when active? "active") :href href} label])

(defn- sb-out
  "A door OUT of the workspace — the speaker-facing pages. The ↗ is the whole
   signal, and the new tab is the point: an organizer checking the public page
   is not navigating away from what they were doing."
  [label href]
  [:a.sb-item.sb-out {:href href :target "_blank" :rel "noopener"} label " ↗"])

(defn- sb-group [label & items]
  (list [:div.sb-group {:key label} label] items))

;; The nav is the LIFECYCLE, in order: open the call, review what arrives,
;; decide and tell people, run the show. It used to be an alphabet-soup list of
;; twelve peers, which said nothing about what to do next — and hid the exports
;; entirely, which is how an integrator concluded we didn't have any. The three
;; housekeeping pages sit in a quiet row at the bottom, where they belong.

(declare time-travel-bar)

(defn- sidebar
  "The lifecycle spine — always exactly ONE event's spine, never a list of
   events (ratified 2026-08-09, docs/design/nav-elements.md: the rail is O(1)
   in event count; switching events is the main surface's job). On pages with
   no event in the URL the WORKING event's spine renders — last visited, else
   nearest upcoming — topped with its name when other events exist."
  [{:keys [event active person time-travel]}]
  (let [birth? (= active :new-event)
        ;; The switcher offers only LIVING events — archived ones stay
        ;; reachable from the events page's restore shelf, never from here
        ;; (Gene, 2026-08-10: the card said 52 when two are active).
        mine   (delay (remove :archived-at
                              (if person (events/events-for-person (:id person))
                                  (events/list-events))))
        current event
        event (or current
                  (when-not birth?
                    (events/working-event (some-> person :id) @mine)))]
    [:nav.sidebar
     [:div.sb-top
      [:a.sb-back {:href "/events"} "All events"]
      [:a.sb-new {:href "/events/new"} "+ New event"]]
     ;; The create page IS step one — the sidebar shows the wizard for the
     ;; event being born, not some other event's navigation (Gene: "grayed out
     ;; because we're in it right now" — the breadcrumb feeling).
     (when birth?
       ;; The full map stays visible while creating (Gene: "full context") —
       ;; the wizard is step 1, and every later group renders as a muted ghost
       ;; that comes alive the moment the event exists.
       (letfn [(ghost [label] [:span.sb-item.sb-step-done {:key label} label])]
         (list
          (sb-group "Create CFP — step 1 of 3"
                    [:span.sb-item.active {:key "s1"}
                     [:span.sb-step [:span.sb-step-n "1"] [:span.sb-step-label "Create event"]
                      [:span.sb-step-note "you are here"]]]
                    [:span.sb-item.sb-step-done {:key "s2"}
                     [:span.sb-step [:span.sb-step-n "2"] [:span.sb-step-label "Create CFP form"]
                      [:span.sb-step-note "next"]]]
                    [:span.sb-item.sb-step-done {:key "s3"}
                     [:span.sb-step [:span.sb-step-n "3"] [:span.sb-step-label "Create review committee"]]])
          (sb-group "Review CFP proposals"
                    (ghost "Review Board") (ghost "Submissions"))
          (sb-group "Decide & tell"
                    (ghost "Inform Speakers"))
          (sb-group "The show"
                    (ghost "Schedule") (ghost "Public agenda")
                    (ghost "Exports & API"))
          [:div.sb-quiet {:key "quiet-ghost"}
           [:span "Comms"] [:span.dot "·"] [:span "Log"]
           [:span.dot "·"] [:span "Settings"]])))
     nil
     (when event
       (let [slug (:slug event)
             cfp-state* (submissions/cfp-state event)
             reviewed?* (forms/reviewed? (:id event))
             n-members* (committees/reviewer-count-for-event (:id event))
             ;; The wizard tracks SETUP, not the call state (Gene, 2026-08-09:
             ;; opening the call early must not hide the remaining steps).
             launching? (or (not reviewed?*) (<= n-members* 1))]
         (list
           ;; THE EVENT MASTHEAD CARD (Gene ratified treatment A, 2026-08-09):
           ;; the rail names the room you are standing in — everything below is
           ;; THIS event's spine. A native <details> disclosure: the switcher
           ;; opens with zero JS and a morph can't break it.
          (list
           [:details.sb-event-card {:key "card"}
            [:summary
             [:span.sb-event-title (:name event)]
             (let [meta-line (str/join " · "
                                       (remove str/blank?
                                               [(str (:location event))
                                                (str (events/display-dates
                                                      (:starts-on event)
                                                      (:ends-on event)))]))]
               (when-not (str/blank? meta-line)
                 [:span.sb-event-meta meta-line]))
               ;; The count rides on the card itself (Gene, 2026-08-09):
               ;; "14 ⇅" = fourteen events live behind this switcher.
             [:span.sb-event-switch
              (when (> (count @mine) 1)
                [:span.sb-event-count (count @mine)])
              "⇅"]]
            [:div.sb-event-menu
               ;; The five most recent — the rail stays O(1) however many
               ;; events (or e2e leftovers) exist; All events holds the rest.
             (for [e (->> @mine
                          (remove #(= (:id %) (:id event)))
                          (sort-by :created-at #(compare %2 %1))
                          (take 5))]
               [:a.sb-event-opt {:key (:id e) :href (event-resume-path e)}
                (:name e)])
             [:a.sb-event-opt.all {:key "all" :href "/events"}
              (str "All " (count @mine) " events →")]]]
           (sb-link (= active :dashboard) "Dashboard" (str "/events/" slug)))

             ;; The transient wizard (Gene, 2026-08-09): while the call has never
             ;; been opened, these ARE serial steps and the sidebar says so —
             ;; numbered, with live ✓s and the launch act at the end. The moment
             ;; the call opens, the group relaxes into plain navigation: the
             ;; wizard exists exactly as long as the wizard is true.
          (if launching?
            (let [reviewed?  reviewed?*
                  n-members  n-members*
                   ;; The green ✓ says done — no "done" note needed; and the
                   ;; roster count read as noise, not signal (Gene, 2026-08-09).
                  steps      [{:done? true :label "Create / edit event"
                               :href (str "/events/" slug "/details")
                               :active? (= active :details)}
                              {:done? reviewed? :label "Create / edit CFP form"
                               :href (str "/events/" slug "/form")
                               :active? (= active :form)}
                              {:done? (> n-members 1)
                               :label "Create / edit review committee"
                               :href (str "/events/" slug "/committee")
                               :active? (= active :committee)}]
                  step-n     (inc (count (take-while :done? steps)))]
              (sb-group (if (> step-n (count steps))
                          "Create CFP — ready to open"
                          (str "Create CFP — step " (min step-n (count steps)) " of " (count steps)))
                 ;; NO wrapper spans and NO per-row notes — every row is
                 ;; the same element with the same padding, so the
                 ;; rhythm is even (Gene, 2026-08-09: "make them even").
                        (for [{:keys [done? label href active?]} steps]
                   ;; Done → green ✓; not-yet → an EMPTY spacer so the
                   ;; grid's marker column always exists and labels
                   ;; align (Gene, 2026-08-09).
                          (let [row [:span.sb-step
                                     (if done?
                                       [:span.sb-step-n.sb-ck "✓"]
                                       [:span.sb-step-sp])
                                     [:span.sb-step-label label]]]
                            (if href
                              [:a.sb-item.sb-step-item
                               {:key label :href href
                                :class (when active? "active")} row]
                              [:span.sb-item.sb-step-done.sb-step-item
                               {:key label} row])))
                 ;; A SIBLING of the steps (same marker column, same
                 ;; indent) — it belongs to the wizard, not below it
                 ;; (Gene, 2026-08-09).
                        [:a.sb-item.sb-out.sb-step-item
                         {:key "pub" :href (str "/cfp/" slug)
                          :target "_blank" :rel "noopener"}
                         [:span.sb-step [:span.sb-step-sp]
                          [:span.sb-step-label "View public CFP page ↗"]]]
                        (when (= :not-open-yet cfp-state*)
                          [:form.sb-open-form {:method "post"
                                               :action (str "/api/events/" slug "/cfp/open")}
                           [:button.sb-open {:type "submit"} "Open the call →"]])))
            (sb-group "The call"
                      (sb-link (= active :details) "Event details" (str "/events/" slug "/details"))
                      (sb-link (= active :form) "CFP Form" (str "/events/" slug "/form"))
                      (sb-link (= active :committee)
                               (str "Committee (" n-members* ")")
                               (str "/events/" slug "/committee"))
                      (sb-out "Public CFP page" (str "/cfp/" slug))))

           ;; The board superseded the Submissions page (Gene, 2026-08-10:
           ;; two near-identical tables force "which one is real?"); it now
           ;; carries the count and the old route 303s here.
          (sb-group "Review CFP proposals"
                    (sb-link (= active :board)
                             (str "Review Board ("
                                  (submissions/count-for-event (:id event)) ")")
                             (str "/events/" slug "/board")))

          (sb-group "Decide & tell"
                    (sb-link (= active :inform) "Inform Speakers" (str "/events/" slug "/inform")))

          (sb-group "The show"
                    (sb-link (= active :schedule) "Schedule" (str "/events/" slug "/schedule"))
                    (sb-out "Public agenda" (str "/agenda/" slug))
                    (sb-link (= active :exports) "Exports & API" (str "/events/" slug "/exports")))

          (when (get-in event [:settings :replay?])
            (sb-link (= active :replay) "Replay" (str "/events/" slug "/replay")))

          [:div.sb-quiet {:key "quiet"}
           [:a {:href (str "/events/" slug "/comms") :class (when (= active :comms) "on")} "Comms"]
           [:span.dot "·"]
           [:a {:href (str "/events/" slug "/log") :class (when (= active :log) "on")} "Log"]
           [:span.dot "·"]
           [:a {:href (str "/events/" slug "/settings") :class (when (= active :settings) "on")} "Settings"]]

          nil)))]))

(defn- dev-strip
  "THE DEV STRIP (Gene, 2026-08-09 — 'alongside the bottom to give us a lot
   more space'): a fixed full-width devtools bar. DEV badge, the identity
   switcher, and the time-travel scrubber stretched across the remaining
   width — a timeline is a horizontal instrument. ENV=dev only here, and the
   demo-login endpoint 404s outside demo mode, so production never grows one."
  [{:keys [event person time-travel]}]
  (when (and event (= "dev" (System/getenv "ENV")))
    (let [committee (first (events/committees-for-event (:id event)))
          members (when committee
                    (committees/members-for-committee (:id committee)))]
      ;; A native <details>, open by default — collapsing it frees the bottom
      ;; of the page (Gene, 2026-08-09: the strip hid the last rows).
      [:details.dev-strip {:open true}
       [:summary [:span.sb-dev-badge "DEV"]]
       [:div.dev-strip-body
        [:form.sb-dev-switch {:method "post" :action "/api/demo-login"}
         [:label "Review as"]
         [:select {:name "email"
                   ;; Selecting IS switching (Gene hit the pick-but-forgot-
                   ;; Switch trap, 2026-08-10). Plain-form onchange submit —
                   ;; no Datastar on this element, so the old-web idiom is
                   ;; correct here; the button stays as the no-JS fallback.
                   :onchange "this.form.submit()"}
          (for [m members]
            [:option {:key (:email m) :value (:email m)
                      :selected (when (= (:email m) (:email person)) true)}
             (:name m)])]
         [:button {:type "submit"} "Switch"]]
        (when time-travel
          [:div.dev-strip-scrub (time-travel-bar event time-travel)])]])))

(defn- breadcrumb
  "Events › <event> › <page>. The current segment is ink and unlinked — a
   breadcrumb whose last crumb is a link to where you already are is noise."
  [{:keys [event crumb]}]
  (when event
    [:div.crumbs
     [:a {:href "/events"} "Events"]
     [:span.sep "›"]
     [:a {:href (str "/events/" (:slug event))} (events/display-name event)]
     (when crumb (list [:span.sep "›"] [:span.here crumb]))]))

(defn- whoami-strip
  "Who you are signed in as, and the way out. Reviewers are NAMED — that is the
   point of the board — so the app always says whose opinions you're adding."
  [{:keys [person]}]
  (when person
    [:div.whoami
     "signed in as " [:strong (:name person)]
     [:form {:method "post" :action "/logout"}
      [:button.ui.mini.basic.button {:type "submit"} "Log out"]]]))

(defn organizer-shell
  "Page shell for ORGANIZER pages: fixed left sidebar + content.
   The public CFP page deliberately does NOT use this — speakers get a clean
   single column with nothing to navigate."
  [title nav & body-content]
  (str
   (h/html
    (page/doctype :html5)
    [:html {:lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:title title]
      [:link {:rel "icon" :href favicon-data-uri}]
      [:link {:rel "stylesheet"
              :href "https://cdn.jsdelivr.net/npm/fomantic-ui@2.9.3/dist/semantic.min.css"}]
      [:script {:src "https://code.jquery.com/jquery-3.6.0.min.js"}]
      [:script {:src "https://cdn.jsdelivr.net/npm/fomantic-ui@2.9.3/dist/semantic.min.js"}]
      [:script {:src (versioned "/js/datastar-kit.js")}]
      [:script {:src (versioned "/js/keyboard.js") :defer true}]
      [:script {:src (versioned "/js/ghost-fill.js") :defer true}]
        ;; Some pages need Datastar for one-shot actions without owning a
        ;; persistent stream. Keep runtime loading separate from SSE mounting so
        ;; live scrub cannot consume a browser connection for the page lifetime.
      (when (or (:datastar? nav) (:sse? nav))
        [:script {:type "module" :src (versioned "/vendor/datastar-aliased.js")}])
      [:link {:rel "stylesheet" :href (versioned "/css/app.css")}]]
     [:body (when-let [attrs (:body-attrs nav)] attrs)
        ;; The sidebar owns the top of the viewport (Gene, 2026-08-10: no
        ;; wasted band above it) — whoami rides the content column's first
        ;; row, sharing a line with the breadcrumb.
      [:div.ui.container.wide {:style "margin-top: 0.9em; margin-bottom: 4em;"}
       [:div.layout
        (sidebar nav)
        [:div.content
         [:div.content-top (breadcrumb nav) (whoami-strip nav)]
         body-content]]
           ;; The heartbeat target — proves the stream is alive without any JS.
       (when (:sse? nav) [:span {:id "sse-heartbeat"}])]
      (dev-strip nav)]])))

;; --- Shared bits ------------------------------------------------------------

(defn- header [title subtitle & right]
  [:div.app-header
   [:div
    [:h1.ui.header title]
    (when subtitle [:div.sub subtitle])]
   [:div right]])

(defn- not-blank [s] (when-not (str/blank? s) s))

(defn- field-errors [errors k]
  (when-let [msgs (get errors k)]
    [:div.ui.pointing.red.basic.label (str/join " " msgs)]))

;; --- 1. Events list ---------------------------------------------------------

(defn events-list-page
  "j/k walk a focus ring down the ACTIVE rows (Datastar signals — 0ms, no
   round trip); x archives the focused event — an appended FACT, never a
   deletion (Gene, 2026-08-10) — and archived events collapse into a details
   section at the bottom with Restore, exactly like retired questions."
  [evts person]
  (let [active (vec (remove :archived-at evts))
        archived (vec (filter :archived-at evts))
        max-idx (max 0 (dec (count active)))
        ;; x posts the focused INDEX; the server maps it over its own list
        ;; order. The reload after the write is delivery, not rendering — the
        ;; server decides everything on the repaint (/events has no SSE mount).
        x-act (str (ds/post-action* "/api/events/archive" {:idx (ds/js "$evIdx")})
                   ".then(()=>location.reload())")]
    (organizer-shell
     "Events — CFP Scheduler Killer"
     {:active :events :person person}
     (header "Events" "Every event you run. Create one and the CFP is live in minutes."
             (list
              [:form {:key "replay" :method "post" :action "/api/replay/start-demo"
                      :style "display:inline; margin-right:0.5em;"}
               [:button.ui.basic.button {:type "submit"
                                         :title "Creates a separate demo event — your real events are never touched"}
                "▶ Start AIE replay demo"]]
              [:a.ui.primary.button {:key "new" :href "/events/new"} "Create event"]))
     (if (empty? evts)
       [:div.ui.segment
        [:div.empty-state
         [:p [:strong "No events yet."]]
         [:p "Create one — you'll get a Program Committee and a seed form for free, "
          "and the public CFP URL goes live immediately."]
         [:a.ui.primary.button {:href "/events/new"} "Create your first event"]]]
       [:div {:data-star-signals__ifmissing "{evIdx: 0}"
              :data-star-signals (str "{evMax: " max-idx "}")
              :data-star-on:keydown__window
              (ds/keydown-expr
               []
               [(ds/on-key "j" {} (ds/signal-inc "$evIdx" max-idx))
                (ds/on-key "k" {} (ds/signal-dec "$evIdx"))
                (ds/on-key "x" {} x-act)])}
        [:table.ui.celled.table
         [:thead
          [:tr [:th "Event"] [:th "Dates"] [:th "Call for speakers"] [:th "Time zone"] [:th ""]]]
         [:tbody
          (for [[i e] (map-indexed vector active)]
            (let [resume (event-resume-path e)
                  done? (event-setup-done? e)]
              [:tr {:key (str (:id e))
                    :data-star-class:ev-focused (str "$evIdx === " i)}
               [:td [:a {:href resume} (:name e)]
                [:div.field-hint (:slug e)]]
               [:td (or (fmt-date-range (:starts-on e) (:ends-on e))
                        [:span.field-hint "not set"])]
               [:td (or (fmt-cfp-window e) [:span.field-hint "not scheduled"])]
               [:td (:tz e)]
               [:td [:a.ui.tiny.basic.button {:href resume}
                     (if done? "Open" "Resume setup →")]]]))]]
        [:div.field-hint {:style "margin-top:0.4em;"}
         [:kbd "j"] "/" [:kbd "k"] " move · " [:kbd "x"] " archive the focused event — "
         "archiving is a recorded fact, never a deletion; restore below."]
        (when (seq archived)
          [:details.ev-archived {:key "archived"}
           [:summary (str "Archived (" (count archived) ")")]
           (for [e archived]
             [:div.ev-archived-row {:key (str (:id e))}
              [:span.ev-archived-name (:name e)
               [:span.field-hint {:style "display:inline; margin-left:0.6em;"}
                (:slug e)]]
              [:form {:method "post"
                      :action (str "/api/events/" (:slug e) "/unarchive")}
               [:button.ui.mini.basic.button {:type "submit"} "Restore"]]])])]))))

;; --- 2. Create event --------------------------------------------------------

;; House defaults for a brand-new create page. Ghosted text can carry a real
;; example (Tab accepts it — see resources/public/js/ghost-fill.js), but a date
;; input has no placeholder to ghost, so the two dates are prefilled outright
;; and the marquee shows them on the FIRST paint rather than after a keystroke.
;; Nothing here is forced: they are ordinary editable values, and a POST always
;; carries whatever the browser actually holds.
(def ^:private default-start-date "2026-10-07")
(def ^:private default-end-date "2026-10-08")
(def ^:private example-name "Enterprise AI Summit")
(def ^:private example-location "Charlotte, NC")
(def ^:private example-website "https://events.itrevolution.com/2026-charlotte/")

;; The marquee is the create screen. Everything else on the page is the form
;; that feeds it — you type a name, and the headline you will see on the public
;; page, in the invites and on every email writes itself in front of you.
;;
;; It is rendered in exactly TWO places: here on first paint, and by
;; `handle-events-preview`, which re-renders this same function from the values
;; the browser posted and pushes the result down that one viewer's SSE stream.
;; No client-side templating, no second copy of the format — the marquee cannot
;; disagree with the event it is previewing because `events/display-name` is the
;; only thing that knows how an event is spelled.

(defn event-marquee
  "`draft` is {:name :location :starts-on :ends-on} of raw typed strings.
   `slug` is what the URL will be. `suggestion` is an optional
   events/trim-suggestion. `cfp-open?` colours one line of the caption."
  [host draft slug suggestion cfp-open?]
  [:div#event-marquee.marquee
   (if-let [display (events/display-name draft)]
     [:h1.marquee-title [:span.marquee-mark display]]
     [:h1.marquee-title.ghost [:span "Your event…"]])
   [:div.marquee-caption
    "↳ how your event appears everywhere: public page · invites · emails"]
   ;; Never a degenerate address. With no name there is no slug, and the line
   ;; says so in the same ghost idiom as the headline rather than inventing
   ;; something like /cfp/2026 out of the dates.
   (if-let [s (not-blank slug)]
     [:div.marquee-url
      [:span.url-text (str host "/cfp/" s)]
      ;; Clipboard is one of the few things the browser owns (global CLAUDE.md)
      ;; — this is the sanctioned inline-JS shape, via the ds helper.
      [:button.copy-url {:type "button" :title "Copy the public CFP URL"
                         :data-star-on:click
                         (ds/copy-nearest-text ".marquee-url" ".url-text"
                                               "Copied to clipboard")}
       "⧉ copy"]]
     [:div.marquee-url.ghost host "/cfp/" [:span "your-event-name…"]])
   [:div.marquee-caption
    (if cfp-open?
      "The call for speakers opens the moment you create it."
      "The call for speakers will stay closed until you open it.")]
   (when suggestion
     ;; One quiet line, never a modal, and never automatic. The button posts
     ;; back to the same preview endpoint with ?apply-trim=1 — the SERVER does
     ;; the trimming and pushes the corrected name down as a signal patch, so
     ;; no JavaScript anywhere touches the input's value.
     [:div.marquee-assist
      "Your name seems to include dates/location — we add those automatically. "
      [:button {:type "button"
                :data-star-on:click "@post('/api/events/preview?apply-trim=1')"}
       (str "Use “" (:trimmed suggestion) "”")]])])

(defn slug-status
  "The line under the Public CFP URL field, and the reason it needs no prose:
   it ANSWERS the only question the field raises. Green when the address is
   free, red naming the event that owns it when it is not, ghost guidance when
   there is no address yet. Re-pushed by handle-events-preview on every
   keystroke, same as the marquee.

   `owner-display` is (events/slug-owner-display slug) — nil when available."
  [slug owner-display]
  (cond
    (str/blank? slug)
    [:div#slug-status.field-hint
     "Derived from the name, the city and the year — or type your own. "
     "Slugs are permanent."]

    owner-display
    [:div#slug-status.field-hint.slug-bad
     (str "✗ Taken by " owner-display " — pick another address.")]

    :else
    [:div#slug-status.field-hint.slug-ok
     (str "✓ /cfp/" slug " is available.")]))

(defn new-event-page
  "Create an event. One page, three visible fields, and a headline that writes
   itself (docs/design/domain-model.md: zero-to-open-CFP in ten minutes).

   `values` are the raw submitted strings so a rejected form comes back filled
   in; `errors` is {field [messages]} from events/validation-errors."
  ([host] (new-event-page host {} nil nil))
  ([host values errors] (new-event-page host values errors nil))
  ([host values errors person]
   (let [;; A FRESH page gets the house defaults; a re-render after a rejected
         ;; submit gets exactly what was typed, blanks included. `values` is
         ;; empty only on the first paint, which is the whole distinction.
         fresh? (empty? values)
         v      #(get values % "")
         starts (if fresh? default-start-date (v :starts-on))
         ends   (if fresh? default-end-date (v :ends-on))
         err?   (seq errors)
         draft  {:name (v :name) :location (v :location)
                 :starts-on starts :ends-on ends}
         derived (events/derive-slug draft)
         slug   (or (not-blank (v :slug)) derived)
         open?  (not= "closed" (not-blank (v :cfp-state)))]
     (organizer-shell
      "Create event — CFP Scheduler Killer"
      {:active :new-event :person person
        ;; The create page streams: every keystroke is answered by a re-rendered
        ;; marquee on this connection. Same hookup as the board, on the pseudo
        ;; event that belongs to nobody (sse/new-event-channel).
       :sse? true
       :body-attrs (ds/sse-mount "new-event")}
      [:div.create-page
       (when err?
         [:div.ui.negative.message
          [:div.header "We couldn't create the event"]
          [:p "Fix the fields marked below and try again."]])

       [:div.rise (event-marquee host draft slug nil open?)]

        ;; input bubbles, so ONE debounced handler on the form covers every
        ;; field in it — and Datastar posts the whole signal set, which is
        ;; exactly what the marquee needs to re-render.
       [:form.ui.form.rise.rise-1
        {:method "post" :action "/api/events/create"
         :data-star-on:input__debounce.300ms "@post('/api/events/preview')"}

        [:div.field {:class (when (:name errors) "error")}
         [:label "Event name"]
         [:input (merge {:type "text" :name "name" :value (v :name)
                         :placeholder example-name :autofocus true
                         :data-ghost-fill ""}
                        (ds/bind :evname))]
         [:div.field-hint "Tab accepts the example text."]
         (field-errors errors :name)]

        [:div.two.fields
         [:div.field {:class (when (:starts-on errors) "error")}
          [:label "Event starts"]
          [:input (merge {:type "date" :name "starts-on" :value starts}
                         (ds/bind :evstarts))]
          (field-errors errors :starts-on)]
         [:div.field {:class (when (:ends-on errors) "error")}
          [:label "Event ends"]
          [:input (merge {:type "date" :name "ends-on" :value ends}
                         (ds/bind :evends))]
          (field-errors errors :ends-on)]]

         ;; The call-for-speakers decision is one sentence with two radios, not a
         ;; section with two timestamps. Nobody opening a CFP wants to schedule
         ;; it; they want it live, or deliberately not yet.
        [:div.cfp-choice
         [:span.lead "The call for speakers"]
          ;; Both radios bind the SAME signal — that is how Datastar reads a radio
          ;; group — so the marquee's last caption line follows the choice.
         [:label
          [:input (merge (cond-> {:type "radio" :name "cfp-state" :value "open"}
                           open? (assoc :checked true))
                         (ds/bind :evcfp))]
          "opens right away"]
         [:label
          [:input (merge (cond-> {:type "radio" :name "cfp-state" :value "closed"}
                           (not open?) (assoc :checked true))
                         (ds/bind :evcfp))]
          "stays closed for now"]
         [:div.cfp-until
          [:span "…open until"]
          [:input (merge {:type "date" :name "cfp-closes-on" :value (v :cfp-closes-on)}
                         (ds/bind :evcloses))]
          [:span "(optional — end of that day, your time zone)"]]
         (field-errors errors :cfp-closes-at)]

          ;; Native <details>. Zero JavaScript for the toggle itself, keyboard-
          ;; accessible for free, and it survives an SSE morph because the
          ;; browser owns the open/closed state — a signal-driven accordion would
          ;; not. It REPORTS each toggle to the draft stash (fire-and-forget), so
          ;; a refresh re-renders the panel the way it was left.
        [:details.create-details
         (cond-> {:data-star-on:toggle
                  (ds/post-action* "/api/events/draft-pref"
                                   {:more-open (ds/js "evt.target.open")})}
           (:more-open? values) (assoc :open true))
         [:summary "More options — location, website, URL, time zone, support email"]

         [:div.two.fields
          [:div.field {:class (when (:location errors) "error")}
           [:label "Location " [:span.optional "(optional)"]]
           [:input (merge {:type "text" :name "location" :value (v :location)
                           :placeholder example-location :data-ghost-fill ""}
                          (ds/bind :evloc))]
           [:div.field-hint "Appears in the headline above, on the public pages, "
            "and as the calendar-invite location."]
           (field-errors errors :location)]
          [:div.field {:class (when (:website-url errors) "error")}
           [:label "Event website " [:span.optional "(optional)"]]
           [:input (merge {:type "url" :name "website-url" :value (v :website-url)
                           :placeholder example-website :data-ghost-fill ""}
                          (ds/bind :evweb))]
           (field-errors errors :website-url)]]

         [:div.field {:class (when (:slug errors) "error")}
          [:label "Public CFP URL"]
          [:div.ui.labeled.input
           [:div.ui.label.slug-prefix (str host "/cfp/")]
            ;; The placeholder is the DERIVED slug, and it follows the typing:
            ;; bound to a signal the preview handler patches after each
            ;; keystroke, so the ghost in the box can never disagree with the
            ;; green line under it. Tab still accepts it (ghost-fill).
           [:input (merge {:type "text" :name "slug" :value (v :slug)
                           :placeholder (or derived "eais-charlotte")
                           :data-ghost-fill ""
                           :data-star-signals__ifmissing
                           (str "{slugghost: '" (or derived "eais-charlotte") "'}")
                            ;; `|| fallback` because the attr binding can evaluate
                            ;; BEFORE the same element's signal registers — without
                            ;; it, the first evaluation wipes the placeholder to
                            ;; "" (found live, 2026-08-09).
                           :data-star-attr:placeholder
                           (str "$slugghost || '" (or derived "eais-charlotte") "'")}
                          (ds/bind :evslug))]
            ;; Second copy spot (Gene: "it should be in two places") — same
            ;; clipboard gesture, sourcing the full URL from the marquee's own
            ;; text so the two can never disagree.
           [:button.copy-url {:type "button" :title "Copy the public CFP URL"
                              :data-star-on:click
                              (ds/copy-nearest-text "body" ".marquee-url .url-text"
                                                    "Copied to clipboard")}
            "⧉ copy"]]
          (slug-status slug (events/slug-owner-display slug))
          (field-errors errors :slug)]

         [:div.field {:class (when (:tz errors) "error")}
          [:label "Time zone"]
          [:select (merge {:name "tz"} (ds/bind :evtz))
           (let [selected (or (not-blank (v :tz)) events/default-timezone)]
             (for [tz events/common-timezones]
               [:option (cond-> {:value tz} (= tz selected) (assoc :selected true)) tz]))]
          [:div.field-hint "It's a pain to change later. Daylight savings applies "
           "automatically."]
          (field-errors errors :tz)]

         [:div.field {:class (when (:support-email errors) "error")}
          [:label "Speaker support email"]
          [:input (merge {:type "email" :name "support-email" :value (v :support-email)
                          :placeholder "annp@itrevolution.net"
                          :data-ghost-fill ""}
                         (ds/bind :evsupport))]
          [:div.field-hint "Reply-to on every email a speaker receives. "
           "Leave blank and we use your address."]
          (field-errors errors :support-email)]]

        [:button.btn-go {:type "submit"} "Create event →"]]

       [:div.create-footer.rise.rise-2
        [:p [:strong "On create:"] " a Program Committee is spawned (invite people next) · "
         "the seed form is installed (edit it anytime) · your public CFP URL goes live."]
        [:p "Target: under ten minutes from here to the first submission being possible."]
        [:div.box
         [:form {:method "post" :action "/api/events/demo"}
          [:button.btn-quiet {:type "submit"} "Create demo event"]]
         [:div.field-hint.demo-note
          "Creates \"Demo Conference\" with a Program Committee and the seed form installed, "
          "so no screen is ever empty. Fake submissions arrive in a later slice — this button "
          "does not generate any yet."]]]]))))

;; --- 3a. Event dashboard parts ----------------------------------------------

(defn alert-rows-partial
  "The 'Also check' rows. Count first — the number a human would panic about
   leads the sentence. Rows with a zero count are not rendered at all: a
   dashboard of zeroes teaches people to stop reading it."
  [rows]
  (when (seq rows)
    [:div.alerts
     (for [r rows]
       [:div.alert-row {:key (name (:key r)) :class (when (:urgent? r) "urgent")}
        [:div.alert-count (:count r)]
        [:div.alert-text (:text r)]
        [:a.alert-go {:href (:href r)} (:link r) " →"]])]))

(defn- checklist-item [done? label]
  [:li [:span.box {:class (when done? "done")} (if done? "✓" "☐")] [:span label]])

(defn- initials
  "\"Gene Kim\" → \"GK\" — the avatar disc's two letters (pre-pool fallback)."
  [nm]
  (->> (str/split (str nm) #"\s+")
       (keep #(some-> % first str str/upper-case))
       (take 2)
       (apply str)))

(def ^:private face-pool-size
  "How many pNN.jpg live in resources/public/images/people. Bump when the
   pool grows (bd 9ot)."
  48)

(defn pool-face
  "Deterministic demo headshot for anyone WITHOUT an uploaded one: the same
   id (or name) always hashes to the same face, with zero data changes — a
   real :headshot-url always wins at the call site (bd 9ot, Gene 2026-08-10).
   The faces are AI-generated people who do not exist."
  [id]
  (format "/images/people/p%02d.jpg"
          (inc (mod (Math/abs (long (hash (str id)))) face-pool-size))))

(defn- member-row
  "One roster line: face, who they are, their role, and the two actions.
   Remove is a real form POST (no client JS, nothing to go stale under a
   morph); Open is a plain link to the person page."
  [event-slug m]
  [:div.member-row {:key (str (:membership-id m))}
   [:img.member-avatar-img {:src (pool-face (:person-id m)) :alt (:name m)}]
   [:div.member-who
    [:span.member-name (:name m)]
    [:span.member-role-pill {:class (:role m)} (:role m)]
    [:div.member-email (:email m)]]
   [:div.member-actions
    [:a.ui.mini.basic.button
     {:href (str "/events/" event-slug "/people/" (:person-id m))} "Open"]
    [:form {:method "post"
            :action (str "/api/memberships/" (:membership-id m) "/remove")}
     [:button.ui.mini.basic.button {:type "submit"} "Remove"]]]])

(defn- committee-card
  "The Program Committee roster + the inline add form. `member-form` carries the
   values the organizer typed and any server-side errors, so a rejected add
   comes back filled in with the message right next to the field."
  [event-slug committee members member-form]
  (let [{:keys [values errors message]} member-form
        v #(get values % "")
        n (count members)]
    [:div {:id "committee"}
     [:div.cfp-section-title
      (str (or (:name committee) "Program committee")
           " · " n (if (= 1 n) " member" " members"))]
     [:div.roster-card
      (if (seq members)
        [:div.member-list (map (partial member-row event-slug) members)]
        [:p.field-hint
         "Nobody on the roster yet. Add the people who will read submissions — "
         "membership sets the coverage denominator and routes their email; it "
         "never gates what anyone can see."])]

     (when message
       [:div.ui.negative.message {:style "margin-top:1em;"} message])

     [:div.cfp-section-title "Add a reviewer"]
     (if committee
       [:form.ui.form.add-member-form.roster-card
        {:method "post"
         :action (str "/api/committees/" (:committee-id committee) "/members/add")}
        [:div.three.fields
         [:div.field {:class (when (:name errors) "error")}
          [:label "Name"]
          [:input {:type "text" :name "name" :value (v :name)
                   :placeholder "Ann Perry" :data-ghost-fill ""}]
          (field-errors errors :name)]
         [:div.field {:class (when (:email errors) "error")}
          [:label "Email"]
          [:input {:type "email" :name "email" :value (v :email)
                   :placeholder "annp@itrevolution.net" :data-ghost-fill ""}]
          (field-errors errors :email)]
         [:div.field {:class (when (:role errors) "error")}
          [:label "Role"]
          [:select {:name "role"}
           (let [selected (committees/normalize-role
                           (or (not-blank (v :role)) committees/default-role))]
             (for [r committees/roles]
               [:option (cond-> {:value r} (= r selected) (assoc :selected true)) r]))]
          (field-errors errors :role)]]
        [:button.btn-go.sm {:type "submit"} "Add reviewer"]
        [:div.field-hint {:style "margin-top:0.6em;"}
         "Being on this roster is what makes someone a reviewer of "
         [:strong "this"] " event, and of no other — it never gates what "
         "anyone can see."]]
       [:p.field-hint "This event has no committee — that shouldn't happen; "
        "every event spawns one at creation."])]))

(defn committee-page
  "Step 3's own page (Gene, 2026-08-09: 'create / edit review committee' is a
   place you go, not an anchor you scroll to). The same committee-card the
   dashboard shows, with the wizard's framing around it."
  [event {:keys [committee members member-form person cfp-state]}]
  (let [n (count members)
        slug (:slug event)]
    (organizer-shell
     (str "Committee — " (:name event))
     {:event event :active :committee :person person :crumb "Committee"}
     (header "Create review committee"
             (str n " on the roster · everyone on it can read and rate "
                  "every submission")
              ;; The forward act, top right like every wizard page: open the
              ;; call once the committee is real, else on to the board.
             [:div.fb-header-acts {:key "acts"}
              (if (= :not-open-yet cfp-state)
                [:form {:method "post"
                        :action (str "/api/events/" slug "/cfp/open")}
                 [:button.btn-go {:type "submit"} "Next: open the call →"]]
                [:a.btn-go {:href (str "/events/" slug "/board")}
                 "Go to the review board →"])])
     (when (< n 2)
       [:div.step-banner
        [:strong "Step 3 of 3."] " Review is a conversation among trusted "
        "peers over a shared table: everyone you add here sees every "
        "submission, every score and every comment. Add at least one "
        "colleague — the 2-review coverage rule needs two readers."])
     (committee-card slug committee members member-form))))

;; --- 3b. Submissions list ---------------------------------------------------

(defn submissions-page
  "Every submission for one event. The dense table the review board will grow
   from — for now it reads, it does not rate."
  [event subs {:keys [sub-count speaker-count cfp-state person]}]
  (organizer-shell
   (str "Submissions — " (:name event))
   {:event event :active :submissions :person person :crumb "Submissions"}
   (header "Submissions"
           (str sub-count " session" (when (not= 1 sub-count) "s")
                " · " speaker-count " speaker" (when (not= 1 speaker-count) "s")
                (when (and (pos? sub-count) (pos? speaker-count))
                  (format " · %.2f per speaker" (double (/ sub-count speaker-count)))))
           (list
            [:a.ui.primary.button {:key "cap" :href (str "/events/" (:slug event) "/capture")}
             "+ Add submission"]
            [:a.ui.basic.button {:key "pub" :href (str "/cfp/" (:slug event))
                                 :target "_blank" :rel "noopener"}
             "View public page"]))

   (if (empty? subs)
     [:div.ui.segment
      [:div.empty-state
       [:p [:strong "No submissions yet."]]
       [:p (case cfp-state
             :not-open-yet "The call for speakers hasn't opened yet."
             :closed "The call for speakers closed without any submissions."
             "The call for speakers is open and waiting for the first one.")]
       [:a.ui.primary.button {:href (str "/cfp/" (:slug event))
                              :target "_blank" :rel "noopener"}
        "Open the public CFP page"]]]
     [:table.ui.celled.table
      [:thead
       [:tr [:th "Talk"] [:th "Speaker"] [:th "Format"] [:th "Org size"]
        [:th "Status"] [:th "Submitted"]]]
      [:tbody
       (for [sb subs]
         (let [sp (first (:speakers sb))]
           [:tr {:key (str (:id sb))}
            [:td [:div.sub-title (get-in sb [:answers :talk-title])]
             (when-let [fmt (get-in sb [:answers :industry])]
               [:div.sub-meta fmt])]
            [:td (:name sp)
             [:div.sub-meta (:org sp)]]
            [:td (get-in sb [:answers :session-format])]
            [:td (get-in sb [:answers :org-size])]
            [:td [:span.ui.mini.label (:status sb)]]
            [:td (or (fmt-instant (:created-at sb) (:tz event)) "—")]]))]])

   [:div.field-hint {:style "margin-top:1em;"}
    "Rating, comments and the two work-queue sorts arrive with the review board "
    "slice. Private answers (Notes to the Planning Committee) are collected and "
    "stored, and will show on the submission detail page."]))

;; --- Shared form helpers ----------------------------------------------------
;;
;; Used by the public CFP form, the portal edit form and the profile form —
;; defined once, above all three, so a field renders identically wherever a
;; human meets it.

(defn- req-mark [required?]
  (when required? [:span.required-mark "*"]))

(defn- field-error [errors k]
  (when-let [msgs (get errors k)]
    [:div.ui.pointing.red.basic.label (str/join " " msgs)]))

(defn- answer-input
  "One field def -> one form control. `values` are the raw params of a rejected
   submission, so nothing the speaker typed is ever lost."
  [{:keys [id type label help placeholder required options max-length private
           widget]} values errors]
  (let [k (keyword (name id))
        param (keyword (str "answer-" (name id)))
        v (get values param "")
        t (name type)
        err (get errors k)]
    [:div.field {:key (name id) :id (str "pv-" (name id))
                 :class (str (when err "error ")
                             (when private "private-note"))}
     [:label label (req-mark required)]
     ;; No "max N" chip: length lives in the SHAPE of the field, not a
     ;; number a speaker thinks about (Gene, 2026-08-09). Server-side caps
     ;; remain and speak up honestly only when actually exceeded.
     (cond
       ;; Field defs arrive as strings from the log and as keywords from the
       ;; seed vector, so compare by NAME everywhere — never by identity.
       (and (= t "select") (= (some-> widget name) "radio"))
       [:div.grouped.fields {:style "margin:0;"}
        (for [o options]
          [:div.field {:key o}
           [:div.ui.radio.checkbox
            [:input (cond-> {:type "radio" :name (name param) :value o}
                      (= o v) (assoc :checked true))]
            [:label o]]])]

       (= t "select")
       [:select {:name (name param)}
        [:option {:value ""} "— choose —"]
        (for [o options]
          [:option (cond-> {:key o :value o} (= o v) (assoc :selected true)) o])]

       ;; Placeholders are ghost EXAMPLES (:placeholder on the field def) —
       ;; never the help text, which renders once below the field where it
       ;; survives typing. Tab accepts them (ghost-fill.js), same affordance
       ;; as the create page (Gene, 2026-08-09).
       (#{"textarea" "markdown"} t)
       [:textarea {:name (name param) :rows (if (= t "markdown") 8 4)
                   :class (when (= t "markdown") "prose-deep")
                   :placeholder placeholder
                   :data-ghost-fill (when placeholder "")} v]

       (= t "url")
       [:input {:type "url" :name (name param) :value v
                :placeholder (or placeholder "https://…")
                :data-ghost-fill (when placeholder "")}]

       (= t "email")
       [:input {:type "email" :name (name param) :value v
                :placeholder placeholder
                :data-ghost-fill (when placeholder "")}]

       :else
       [:input {:type "text" :name (name param) :value v
                :placeholder placeholder
                :data-ghost-fill (when placeholder "")
                :maxlength (when max-length (str max-length))}])
     (when help [:div.field-hint help])
     (field-error errors k)]))

;; --- The inform banner (shared: dashboard + board) --------------------------

(defn inform-banner
  "The Sessionize warning, adopted with pride: a decision nobody has been told
   about is not a decision anyone can act on."
  [event n]
  (when (pos? n)
    [:div.ui.warning.message
     [:div.header n " decision" (when (not= 1 n) "s") " not yet communicated"]
     [:p "Speakers are not automatically informed. Until you tell them, they see "
      [:strong "Under review"] " — whatever the committee decided."]
     [:a.ui.small.orange.button {:href (str "/events/" (:slug event) "/inform")}
      "Inform speakers"]]))

;; --- 3b2. The review board --------------------------------------------------
;;
;; The crown jewel, and the whole doctrine in one screen: every score and every
;; comment visible inline, no clicks, no assignments, no rounds. Two visual rows
;; per submission — facts on top, opinions underneath — because the fulcro app
;; proved that reviewers compare talks by reading each other, not by drilling in.
;;
;; Every control is a plain <form>. Zero client JS: POST → mutate → SSE push to
;; everyone else → 303 back for you. Datastar only receives; it never decides.

(defn fmt-mean [m] (when m (format "%.1f" (double m))))
(defn fmt-stars [s]
  (when s (let [d (double s)]
            (if (== d (Math/floor d)) (str (int d)) (format "%.1f" d)))))

(defn notice-region
  "What the server said when it would not do what you asked — or how something
   it did on your behalf turned out.

   Three things about this region are deliberate:

     1. It is ALWAYS rendered, empty when there is nothing to say. A Datastar
        patch needs its target to exist already (global CLAUDE.md, NEVER #9); an
        element conjured only when there is an error is an element the push
        cannot find.
     2. Nothing about it lives in the browser. The text, the tone and the moment
        it disappears are server state (`notices`), so there is no timer, no
        `classList.toggle`, and no response body for the client to read.
     3. Dismiss is a plain form POST like every other control on this page. It
        works with JavaScript switched off, which is the same promise the public
        CFP form makes."
  [event notice]
  [:div#validation-notice
   (when notice
     [:div.ui.message {:class (if (= :ok (:kind notice)) "positive" "warning")
                       :style "margin-bottom:1em;"}
      [:div.header (if (= :ok (:kind notice)) "Done" "That didn't go through")]
      [:p {:style "margin:0.4em 0 0.6em 0;"} (:message notice)]
      (when (:detail notice)
        [:div.field-hint {:style "margin-top:0;"} (:detail notice)])
      [:form {:method "post"
              :action (str "/api/events/" (:slug event) "/notice/dismiss")}
       [:button.ui.mini.basic.button {:type "submit"} "Dismiss"]]])])

(defn coverage-bar
  "The headline number. Wrapped in a stable id so SSE can repaint just this."
  [event coverage]
  (let [{:keys [covered total target pct]} coverage]
    [:div#coverage-bar.coverage
     [:div.coverage-headline (format "%.0f%%" pct) " reviewed"]
     [:div.coverage-track
      [:div.coverage-fill {:style (str "width:" (format "%.1f" pct) "%;")}]]
     [:div.coverage-note
      (str covered "/" total " have ≥" target " review" (when (not= 1 target) "s"))]]))

(defn- star-form
  "1.0–5.0 in halves, nine buttons firing postJSON — fire-and-forget; the SSE
   per-person push repaints the row, so rating NEVER navigates and the scroll
   position never moves (Gene, 2026-08-10: 'completely using Datastar').

   Plain onclick, NOT data-star-on — the house rule: 81 rows × 9 stars would
   be 729 Datastar expressions recompiled on every morph (NEVER #7, the
   browser-hang). postJSON + SSE push IS the sanctioned pattern here. The
   form remains as the no-JS fallback: with scripting off the buttons submit
   it and the 303 path still works."
  [row person mine]
  [:form {:method "post" :action (str "/api/submissions/" (:id row) "/rate")}
   [:span.mine-label "you:"]
   (for [s reviews/star-steps]
     [:button.star-btn {:key (str s)
                        :class (when (and mine (== (double s) (double mine))) "mine")
                        :type "submit" :name "stars" :value (str s)
                        :onclick (str "event.preventDefault();"
                                      "postJSON('/api/submissions/" (:id row)
                                      "/rate', {stars: " s "})")}
      (fmt-stars s)])])

(defn star-histogram
  "The distribution as a BAR chart — five buckets (1★…5★ left to right,
   halves folding down), bar height = ratings in the bucket. Histograms have
   bars; the bullet points belong on the comments (Gene, 2026-08-10, second
   ruling — the first draft had it backwards). Hover names every rater.
   T1 (third ruling, same night): a numeral 1–5 under each bucket — without
   the axis the bars read as meaningless dots."
  [ratings]
  (let [counts (frequencies (map #(int (Math/floor (double (:stars %)))) ratings))]
    [:span.histo {:title (str/join " · " (map #(str (:person-name %) " ★"
                                                    (fmt-stars (:stars %)))
                                              ratings))}
     (for [b (range 1 6)]
       (let [n (get counts b 0)]
         [:span.hcol {:key b}
          [:span.hbar {:class (when (zero? n) "empty")
                       :style (str "height:" (if (pos? n)
                                               (+ 3 (* 4 (min n 6)))
                                               2) "px")}]
          [:span.hnum b]]))]))

(defn- opinions-block
  "Every rating and every comment, inline — the anti-Sessionize move: opinions
   are never collapsed into a number you have to click to expand.

   The shape (Gene ratified T2, 2026-08-10): a tiny histogram of the star
   distribution, quote-lines carrying each comment WITH its author's stars,
   and an 'also rated' line so raters who didn't comment stay visible —
   every score has a name somewhere on the row."
  [row]
  (let [ratings (:ratings row)
        stars-for (fn [person-id]
                    (some #(when (= person-id (:person-id %)) (:stars %)) ratings))
        commenter-ids (set (map :person-id (:comments row)))
        silent (remove #(commenter-ids (:person-id %)) ratings)]
    [:div.opinions-t2
     (if (seq ratings)
       (star-histogram ratings)
       [:span.op-none "no ratings yet"])
     [:div.quote-lines
      ;; A person's stars ride only their FIRST comment (Gene, 2026-08-10) —
      ;; repeating them on every line reads as re-voting.
      (let [first-comment-id (into {} (map (fn [[pid cs]] [pid (:id (first cs))])
                                           (group-by :person-id (:comments row))))]
        (for [c (:comments row)]
          ;; T5 (Gene, 2026-08-10): stars sit LEFT of the name, in a
          ;; fixed-width slot so scores read as a scannable column even on
          ;; lines that carry none (second comments, unrated commenters).
          [:div.quote-line {:key (str (:id c))}
           [:span.op-slot
            (when (= (:id c) (get first-comment-id (:person-id c)))
              (when-let [st (stars-for (:person-id c))]
                [:span.op-stars "★" (fmt-stars st)]))]
           [:span.who (:person-name c)]
           " — " (:body c)]))
      (when (seq silent)
        [:div.quote-line.silent
         [:span.who "also rated: "]
         (interpose " · "
                    (for [r silent]
                      [:span {:key (str (:person-id r))}
                       (:person-name r) " "
                       [:span.op-stars "★" (fmt-stars (:stars r))]]))])]]))

(defn- private-note-block
  "The 'Notes to the Planning Committee' answer, badged. Collected on the public
   form, shown ONLY here — the BusyConf split, restored."
  [row]
  (let [field (first (filter #(:private %) (:form-snapshot row)))
        answer (when field (get (:answers row) (keyword (name (:id field)))))]
    (when answer
      [:div.pc-only
       [:span.pc-badge "PC ONLY"]
       [:span {:style "color:#666;"} (:label field) ": "] answer])))

(defn- chair-on-event?
  "Chairs decide; reviewers rate and argue (Gene, 2026-08-09). The status
   control is a chair's act — this is the view-side half; the endpoint
   enforces the same rule server-side."
  [event person]
  (boolean
   (when person
     (let [committee (first (events/committees-for-event (:id event)))]
       (some #(and (= (:person-id %) (:id person)) (= "chair" (:role %)))
             (when committee
               (committees/members-for-committee (:id committee))))))))

(declare row-controls*)

(defn- row-controls
  ([event row person mine]
   (row-controls event row person mine (chair-on-event? event person)))
  ([event row person mine chair?]
   (row-controls* event row person mine chair?)))

(defn- row-controls*
  [event row person mine chair?]
  [:div.row-controls
   (star-form row person mine)
   ;; Same zero-navigation treatment: submit (button OR Enter) posts via
   ;; postJSON, clears the box, and the SSE push repaints the row. The
   ;; onsubmit covers both paths in one place; input text is browser-owned
   ;; data, the one thing allowed in a POST body.
   [:form {:method "post" :action (str "/api/submissions/" (:id row) "/comment")
           :onsubmit (str "event.preventDefault();"
                          "if(this.body.value.trim()){"
                          "postJSON('/api/submissions/" (:id row)
                          "/comment', {body: this.body.value});"
                          "this.body.value='';}")}
    [:input.comment-input {:type "text" :name "body" :placeholder "Add a comment…"}]
    [:button.ui.mini.basic.button {:type "submit"} "Post comment"]]
   (when chair?
     [:form {:method "post" :action (str "/api/submissions/" (:id row) "/status")}
      [:select {:name "status" :style "font-size:0.85em; padding:0.2em;"}
       (for [s (get-in event [:settings :statuses])]
         [:option (cond-> {:key s :value s} (= s (:status row)) (assoc :selected true)) s])]
      [:button.ui.mini.basic.button {:type "submit"} "Set submission status"]])])

(defn board-row
  "THE LEDGER (Gene ratified treatment A, 2026-08-09): ONE line per
   submission under sortable headings, the conversation as a sub-row where
   it exists, and the controls revealed by the row's own #sub-<id> anchor —
   CSS :target, so focus costs no server state and survives every SSE morph.
   All <tr>s share one <tbody> so a single patch replaces the submission."
  ([event row person] (board-row event row person nil))
  ([event row person chair?*]
   (let [sp (first (:speakers row))
         id (:id row)
         chair? (if (nil? chair?*) (chair-on-event? event person) chair?*)
         mine (when person (:stars (first (filter #(= (:id person) (:person-id %))
                                                  (:ratings row)))))
         has-conversation? (or (seq (:ratings row)) (seq (:comments row)))]
     [:tbody.ledger {:id (str "sub-" id)}
      [:tr.ledger-row
       [:td.lg-flag
        [:form {:method "post" :action (str "/api/submissions/" id "/priority")}
         [:button.sub-flag {:class (when (:priority row) "on")
                            :type "submit" :title "Flag for discussion"} "🔥"]]]
       ;; Line 1 is the PERSON (Gene, 2026-08-10): face · name · role · org.
       ;; The whole person block is a SYNONYM for Read & rate (Gene,
       ;; 2026-08-10): photo, name, meta — one anchor to the detail page.
       [:td.lg-person
        [:a.lg-person-link
         {:href (str "/events/" (:slug event) "/submissions/" id)}
         [:img.b-face {:src (or (not-blank (:headshot-url sp))
                                (pool-face (or (:person-id sp) (:name sp))))
                       :alt (:name sp)}]
         [:div.lg-pwho
          [:div.lg-pname (:name sp)]
          ;; title on its line, ORGANIZATION always on its own (Gene,
          ;; 2026-08-10, second ruling — the one-line squeeze wrapped badly).
          (when-let [r (not-blank (:title sp))] [:div.lg-pmeta r])
          (when-let [o (not-blank (:org sp))] [:div.lg-pmeta.lg-porg o])]]]
       ;; Size LEFT of format (Gene, 2026-08-10).
       [:td.lg-size (get-in row [:answers :org-size])]
       [:td.lg-format
        (when-let [fmt (not-blank (get-in row [:answers :session-format]))]
          [:span.fmt-chip fmt])]
       [:td.lg-n (:n row)]
       [:td.lg-mean (or (fmt-mean (:mean row)) "—")
        (when (:split? row) [:span.b-split " SPLIT"])]
       [:td.lg-you (if mine (fmt-mean mine) "–")]
       [:td.lg-state [:span.ui.mini.label (:status row)]]
       ;; TWO verbs, always visible (Gene, 2026-08-10): act HERE, or go read
       ;; the whole submission. The gold one opens the inline card below
       ;; (pure CSS :target); the quiet one navigates.
       ;; Primary verb FIRST and gold: Read & rate is the default act; Quick
       ;; rate is the shortcut and stays quiet (Gene, 2026-08-10, option 1).
       [:td.lg-acts
        [:a.act-read {:href (str "/events/" (:slug event) "/submissions/" id)}
         "Read & rate →"]
        [:a.act-rate {:href (str "#sub-" id)} "Quick rate ▾"]]]
      ;; Line 2 is the TALK — the FULL table width; vertical space is precious.
      [:tr.ledger-title-row
       [:td.lg-spacer]
       [:td.lg-title-cell {:colspan 8}
        ;; The indent lives on an INNER wrapper: td padding rules fight each
        ;; other across layers/importants; a div has no competitors. It puts
        ;; the title on the same LEFT RAIL as the name — the photo is an
        ;; ornament column and text never aligns to it (Gene, 2026-08-10).
        ;; No chevron (Gene, 2026-08-10) — the focus affordance lives on the
        ;; YOU cell instead: click your own rating to rate.
        [:div.lg-title-indent
         [:a.lg-title-link {:href (str "/events/" (:slug event) "/submissions/" id)}
          (get-in row [:answers :talk-title])]
         (when-let [t (get-in row [:answers :track])]
           [:span.b-facts {:style "margin-left:0.7em;"} t])]]]
      (when has-conversation?
        [:tr.ledger-sub
         [:td.lg-spacer]
         [:td {:colspan 8}
          (opinions-block row)
          (private-note-block row)]])
      (when person
        [:tr.ledger-controls
         [:td.lg-spacer]
         [:td {:colspan 8}
          ;; The open card sits INSIDE the submission's white envelope (the
          ;; whole tbody surfaces on :target) and names itself with a title
          ;; bar (Gene, 2026-08-10). href \"#\" clears the :target — ✕ tucks
          ;; the card away.
          [:div.rate-card
           [:div.rate-card-head
            [:span.rate-card-title "Quick rate"
             ;; A persistent truth, not a toast: your rating IS saved.
             (when mine [:span.rate-card-saved "Saved ✓"])]
            [:span.card-close-group
             [:kbd.esc-hint "esc"]
             [:a.card-close {:href "#" :title "Close (esc)"} "✕"]]]
           (row-controls event row person mine chair?)]]])])))

(defn- sort-chip [current preset slug q status]
  (let [qs (str "?sort=" (:key preset)
                (when (not-blank q) (str "&q=" (java.net.URLEncoder/encode q "UTF-8")))
                (when (not-blank status) (str "&status=" (java.net.URLEncoder/encode status "UTF-8"))))]
    [:a.chip {:key (:key preset)
              :class (when (= current (:key preset)) "on")
              :href (str "/events/" slug "/board" qs)
              :title (:help preset)}
     (:label preset)]))

(defn- status-chip [current-status slug label count* sort-key q]
  (let [target (when (not= current-status label) label)
        qs (str "?sort=" sort-key
                (when (not-blank q) (str "&q=" (java.net.URLEncoder/encode q "UTF-8")))
                (when target (str "&status=" (java.net.URLEncoder/encode target "UTF-8"))))]
    [:a.chip {:key label
              :class (when (= current-status label) "on")
              :href (str "/events/" slug "/board" qs)}
     label " " count*]))

(defn submissions-sparkline
  "Submissions over the CFP's life as a tiny server-drawn SVG (Gene,
   2026-08-09: 'a sparkline of how many submissions we have over time').
   Cumulative count from the call's open (or the first submission) to its
   close (or now) — a rising line IS the momentum story. No JS: the server
   draws, the browser displays."
  [event rows coverage]
  (let [target (:target coverage)
        ats (sort (keep :created-at rows))]
    (when (seq ats)
      (let [now (cfp-scheduler-killer.store/now-inst)
            t0-inst (or (:cfp-opens-at event) (first ats))
            t-end-inst (or (:cfp-closes-at event) now)
            t0 (inst-ms t0-inst)
            t-end (inst-ms t-end-inst)
            t-max (max t-end (inst-ms (last ats)))
            span (double (max 1 (- t-max t0)))
            w 240.0 h 34.0 pad 3.0
            n (count ats)
            x #(-> (- (inst-ms %) t0) (/ span) (* (- w (* 2 pad))) (+ pad))
            y #(- h pad (* (- h (* 2 pad)) (/ (double %) n)))
            xy (fn [at k] (str (format "%.1f" (x at)) "," (format "%.1f" (y k))))
            pts (map-indexed (fn [i at] (xy at (inc i))) ats)
            ;; carry the line flat to \"now\" so the reader sees where we ARE
            now-x (format "%.1f" (min (- w pad) (max pad (x now))))
            path (str (format "%.1f" pad) "," (format "%.1f" (y 0)) " "
                      (str/join " " pts) " " now-x "," (format "%.1f" (y n)))
            ;; THE REVIEWED FILL (Gene, 2026-08-09; recolored 2026-08-10:
            ;; submissions RED, reviewed GREEN): when each submission reached
            ;; the coverage target — the instant its TARGET-th rating landed.
            ;; The wedge between red line and green fill IS the backlog.
            target (or target 2)
            reviewed-ats (sort (keep (fn [r]
                                       (let [rats (sort (keep :at (:ratings r)))]
                                         (when (>= (count rats) target)
                                           (nth rats (dec target)))))
                                     rows))
            rn (count reviewed-ats)
            r-pts (map-indexed (fn [i at] (xy at (inc i))) reviewed-ats)
            fill (when (pos? rn)
                   (str (format "%.1f" pad) "," (format "%.1f" (y 0)) " "
                        (str/join " " r-pts) " "
                        now-x "," (format "%.1f" (y rn)) " "
                        now-x "," (format "%.1f" (y 0))))
            days-left (when (:cfp-closes-at event)
                        (let [d (.toDays (java.time.Duration/between
                                          now (:cfp-closes-at event)))]
                          (when (pos? d) d)))
            ;; the third series (Gene, 2026-08-10): cumulative RATINGS in
            ;; amber, scaled to its own max — reviewing effort overlaid on
            ;; arrivals, so the wedge between curves is the story
            rating-ats (sort (keep :at (mapcat :ratings rows)))
            rk (count rating-ats)
            yr (fn [k] (- h pad (* (- h (* 2 pad)) (/ (double k) (max 1 rk)))))
            rate-path (when (pos? rk)
                        (str (format "%.1f" pad) "," (format "%.1f" (yr 0)) " "
                             (str/join " " (map-indexed
                                            (fn [i at]
                                              (str (format "%.1f" (x at)) ","
                                                   (format "%.1f" (yr (inc i)))))
                                            rating-ats)) " "
                             now-x "," (format "%.1f" (yr rk))))]
        ;; Emitted as an <img data:> URI, NOT inline SVG: Datastar's morph is
        ;; unreliable at patching inline-SVG attributes, which is why the
        ;; time-travel scrub moved the table but froze the sparkline (Gene,
        ;; 2026-08-10). A src attribute swap always repaints. Colors inline —
        ;; stylesheet rules cannot reach inside an <img>.
        [:div.spark-block
         [:img.spark
          {:width (int w) :height (int h) :alt ""
           :src (str "data:image/svg+xml;base64,"
                     (.encodeToString
                      (java.util.Base64/getEncoder)
                      (.getBytes
                       (str "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 "
                            (int w) " " (int h) "' preserveAspectRatio='none'>"
                            (when fill
                              (str "<polygon points='" fill
                                   "' fill='#1B7A4B' fill-opacity='0.22'/>"))
                            (when rate-path
                              (str "<polyline points='" rate-path
                                   "' fill='none' stroke='#D4880F' stroke-width='1.3'/>"))
                            "<polyline points='" path
                            "' fill='none' stroke='#B3261E' stroke-width='1.6'/>"
                            "<circle cx='" now-x "' cy='" (format "%.1f" (y n))
                            "' r='2.5' fill='#B3261E'/></svg>")
                       "UTF-8")))}]
         [:div.spark-label
          [:strong.spark-subs n] " submission" (when (not= 1 n) "s")
          [:span.sep "·"] [:strong.spark-rev rn] " fully reviewed"
          (when-let [pct (:pct coverage)]
            (str " (" (Math/round (double pct)) "%)"))
          (when days-left
            (list [:span.sep "·"] (str days-left " days left")))]
         ;; The raw work ledger under the headline: every star and every
         ;; comment counted (Gene, 2026-08-10).
         (let [n-ratings (reduce + 0 (map (comp count :ratings) rows))
               n-comments (reduce + 0 (map (comp count :comments) rows))]
           [:div.spark-label.spark-counts
            (str "(" n-ratings " rating" (when (not= 1 n-ratings) "s")
                 " · " n-comments " comment" (when (not= 1 n-comments) "s") ")")])
         ;; The axis: when the line starts and when the call closes — the
         ;; two dates that give the shape its meaning.
         (let [zone (try (java.time.ZoneId/of (or (:tz event) "UTC"))
                         (catch Exception _ (java.time.ZoneId/of "UTC")))
               fmt (java.time.format.DateTimeFormatter/ofPattern "MMM d")
               day (fn [^java.time.Instant i] (.format fmt (.atZone i zone)))]
           [:div.spark-axis
            [:span (day t0-inst)]
            [:span (day t-end-inst)]])]))))

(defn- board-qs
  "The board's WHOLE view state as a query string — the URL is the state
   (Gene, 2026-08-10: 'give it to you and you can replicate it')."
  [sort-key q status track]
  (str "?sort=" sort-key
       (when (not-blank q) (str "&q=" (java.net.URLEncoder/encode q "UTF-8")))
       (when (not-blank status)
         (str "&status=" (java.net.URLEncoder/encode status "UTF-8")))
       (when (not-blank track)
         (str "&track=" (java.net.URLEncoder/encode track "UTF-8")))))

(defn- sort-click
  "One sorting <a>: click POSTs the sort (server re-renders and pushes
   #board-region down THIS viewer's SSE stream — no reload) and stamps the
   full state into the URL with replaceState (browser-owned URL bar; every
   value baked server-side at render). href is the no-JS fallback: the same
   state as a plain GET. Plain onclick, not data-star-on — these live inside
   the morphed region and must cost nothing per push."
  [label next-key arrow slug q status track]
  (let [qs (board-qs next-key q status track)]
    [:a {:href (str "/events/" slug "/board" qs)
         :onclick (str "postJSON('/api/events/" slug "/board/sort',"
                       (json/write-str {:sort next-key :q (or q "")
                                        :status (or status "") :track (or track "")})
                       ");history.replaceState(null,'','" qs "');return false")}
     label arrow]))

(defn- sort-th
  "A sortable ledger heading: ascending, click again for descending."
  [label col current slug q status track]
  (let [desc (str col "-desc")
        next-key (if (= current col) desc col)
        arrow (cond (= current col) " ↑"
                    (= current desc) " ↓"
                    :else "")]
    [:th {:key col :class (str "th-" col (when (str/starts-with? (str current) col) " on"))}
     (sort-click label next-key arrow slug q status track)]))

(defn- track-chip
  "One facet chip in the Track row (Gene ratified 2026-08-09: tracks FILTER,
   never gate — everyone sees everything; a chip just narrows). `label` nil
   renders the honest \"(no track)\" bucket for submissions whose form
   snapshot predates the field; its query value is \"(none)\"."
  [current-track slug label count* sort-key q status]
  (let [value (or label "(none)")
        on? (= current-track value)
        qs (str "?sort=" sort-key
                (when (not-blank q) (str "&q=" (java.net.URLEncoder/encode q "UTF-8")))
                (when (not-blank status)
                  (str "&status=" (java.net.URLEncoder/encode status "UTF-8")))
                (when-not on? (str "&track=" (java.net.URLEncoder/encode value "UTF-8"))))]
    [:a.chip {:key value
              :class (when on? "on")
              :href (str "/events/" slug "/board" qs)}
     (or label "no track") " " count*]))

(defn time-travel-bar
  "The event-sourcing party trick, made draggable.

   The slider spans the log's own first and last moments; sliding it re-folds a
   prefix into a throwaway projection and renders THAT. Nothing is copied,
   snapshotted or restored, because the log was always the truth and the screen
   was always derived from it."
  [event {:keys [as-of bounds index total base-path fragment-path]}]
  (when bounds
    (let [[first-at last-at] bounds]
      [:div.timetravel
       [:div.row1
        (if as-of
          [:div "Viewing as of " [:span.tt-when as-of]
           [:span.field-hint {:style "margin-left:0.6em;"} "read-only"]]
          [:div [:strong "Now"] [:span.field-hint {:style "margin-left:0.6em;"}
                                 "drag to watch this CFP happen"]])
        (when as-of
          [:a.ui.mini.button {:href base-path} "Return to now"])]
       ;; PURE SSE SCRUB (Gene, 2026-08-09): the old release-submit forced a
       ;; full page load the moment the handle was let go, wiping the live
       ;; patches — deleted. Dragging fires a throttled @get whose response
       ;; patches #board-region; releasing does nothing, because everything
       ;; already happened. The form remains only as the no-JS fallback.
       ;; The slider lives OUTSIDE the patched region: patching an element you
       ;; are mid-drag on cancels the gesture (the same reason joe-payne keeps
       ;; the reply box outside its SSE region).
       [:form {:method "get" :action base-path}
        [:input (merge
                 {:type "range" :name "at-index" :min 0 :max (max 0 (dec total))
                  :value (or index (max 0 (dec total)))}
                 (when fragment-path
                    ;; single-word signal on purpose — Datastar camelCases
                    ;; hyphens, so $at-index would silently become $atIndex.
                    ;; THROTTLE, not debounce: repaint continuously WHILE
                    ;; dragging — scrubbing video, not poking checkpoints.
                   (ds/live-scrub
                    :atidx
                    (str fragment-path "?at-index="))))]
        [:noscript [:button.ui.mini.button {:type "submit"} "Go"]]]
       [:div.field-hint {:style "display:flex; justify-content:space-between;"}
        [:span first-at] [:span (str total " recorded events")] [:span last-at]]])))

(defn board-region
  "Everything the time-travel slider repaints — and NOT the slider itself.
   Rendered whole on page load and again per scrub tick; one id, one patch."
  [event {:keys [rows coverage sort-key q status status-counts person sort-presets total
                 uncommunicated notice track track-counts]}]
  [:div#board-region
   (notice-region event notice)
   (inform-banner event (or uncommunicated 0))
   ;; One row: search on the left, the momentum sparkline docked right — the
   ;; coverage NUMBER lives in the sparkline's own label now, so the old bar
   ;; row is gone (Gene, 2026-08-09: save the vertical space).
   [:div.board-toprow
    [:form.board-controls {:method "get" :action (str "/events/" (:slug event) "/board")}
     [:input {:type "hidden" :name "sort" :value sort-key}]
     (when (not-blank status) [:input {:type "hidden" :name "status" :value status}])
     (when (not-blank track) [:input {:type "hidden" :name "track" :value track}])
     [:input {:type "search" :name "q" :value (or q "")
              :placeholder "Search title, speaker, org…"
              :style "padding:0.35em 0.6em; width:18em;"}]
     [:button.ui.mini.button {:type "submit"} "Search"]
     (when (not-blank q)
       [:a.chip {:href (str "/events/" (:slug event) "/board?sort=" sort-key)} "clear"])]
    ;; ALL submissions over the call's life, never the filtered view (a
    ;; filter is a lens, the sparkline is the weather).
    (submissions-sparkline event
                           (reviews/enriched-for-event (:id event))
                           coverage)]

   [:div.board-controls
    [:span.field-hint {:style "margin:0;"} "Work queue:"]
    (for [p sort-presets] (sort-chip sort-key p (:slug event) q status))
    [:span {:style "width:1em;"}]
    [:span.field-hint {:style "margin:0;"} "Status:"]
    (for [s (get-in event [:settings :statuses])
          :let [c (get status-counts s 0)]
          :when (pos? c)]
      (status-chip status (:slug event) s c sort-key q))]

   ;; The track facet row — options in the FORM's own order, then the honest
   ;; untracked bucket. Only renders once the form has a track field.
   (let [track-opts (some #(when (= "track" (name (:id %))) (:options %))
                          (forms/active-fields (forms/fields-for-event (:id event))))]
     (when (seq track-opts)
       [:div.board-controls
        [:span.field-hint {:style "margin:0;"} "Track:"]
        (for [t track-opts
              :let [c (get track-counts t 0)]
              :when (pos? c)]
          (track-chip track (:slug event) t c sort-key q status))
        (when-let [c (get track-counts nil)]
          (track-chip track (:slug event) nil c sort-key q status))]))

   (if (empty? rows)
     [:div.ui.segment
      [:div.empty-state
       (if (pos? total)
         "No submissions match that search."
         "No submissions yet — the board fills as talks arrive.")]]
     (let [chair? (chair-on-event? event person)
           slug (:slug event)]
       [:table.board-table.ledger-table
        [:thead
         [:tr
          [:th]
          [:th.lg-th-person {:class (when (or (str/starts-with? (str sort-key) "speaker")
                                              (str/starts-with? (str sort-key) "org")) "on")}
           (let [ctl (fn [label col]
                       (sort-click label
                                   (if (= sort-key col) (str col "-desc") col)
                                   (cond (= sort-key col) " ↑"
                                         (= sort-key (str col "-desc")) " ↓"
                                         :else "")
                                   slug q status track))]
             (list (ctl "Speaker Fname" "speaker-first")
                   [:span.dot " · "]
                   (ctl "Lname" "speaker-last")
                   [:span.dot " · "]
                   (ctl "Org" "org")))]
          ;; Size LEFT of format (Gene, 2026-08-10).
          (sort-th "Size" "org-size" sort-key slug q status track)
          [:th.th-format "Format"]
          (sort-th "Voted" "voted" sort-key slug q status track)
          (sort-th "Avg" "avg" sort-key slug q status track)
          [:th "You"]
          [:th "State"]
          [:th]]]
        (for [row rows] (board-row event row person chair?))]))

   [:div.field-hint {:style "margin-top:1.2em;"}
    (str (count rows) " of " total " shown")
    " · Ratings are 1–5 with halves; SPLIT marks a spread of "
    reviews/split-threshold " stars or more — the rows worth arguing about on the call."
    (when-not person " · Sign in to rate and comment.")]])

(defn board-page
  "`opts` = {:rows [enriched] :coverage {..} :sort-key :q :status :status-counts
             :person :sort-presets :total :time-travel}"
  [event {:keys [person time-travel] :as opts}]
  (organizer-shell
   (str "Review Board — " (:name event))
   {:event event :active :board :person person :crumb "Review Board" :sse? true
     ;; The scrubber rides the DEV STRIP at the viewport's foot (Gene,
     ;; 2026-08-09), not the working surface.
    :time-travel time-travel
     ;; The board is the one page that streams. data-star-init opens the SSE
     ;; connection; everything after that is the server pushing HTML.
    :body-attrs (ds/sse-mount (:id event))}

   (header "Review Board"
           "Every score and every comment, on one page. No assignments, no rounds."
           [:a.ui.basic.button {:href (str "/events/" (:slug event) "/capture")}
            "+ Add submission"])

   (board-region event opts)))

;; --- 3c. Event log ----------------------------------------------------------
;;
;; Nearly free: the store already holds every event in order, so this page is a
;; filter over the fold input. It is also the most honest thing in the app — a
;; visible audit trail of who did what, when.

(defn log-summary
  "One human line per stored event. Falls back to the type when we meet an
   event this build doesn't have a sentence for."
  [{:keys [type payload]}]
  (case type
    "event.created" (str "Created \"" (:name payload) "\" (" (:slug payload) ")")
    "event.updated" (str "Updated " (str/join ", " (:changed payload)))
    "committee.created" (str "Spawned committee \"" (:name payload) "\"")
    "form.installed" (str "Installed the " (or (:template payload) "seed")
                          " form (" (count (:fields payload)) " fields)")
    "person.created" (str "First saw " (:name payload) " <" (:email payload) ">")
    "member.added" (str "Added " (or (:name payload) (:email payload))
                        " to the committee as " (:role payload))
    "member.removed" (str "Removed " (or (:name payload) (:email payload))
                          " from the committee")
    "submission.created" (if (str/starts-with? (str (:source payload)) "on-behalf-of")
                           (str "Captured on behalf of "
                                (or (:name (first (:speakers payload))) "someone")
                                " — \"" (get-in payload [:answers :talk-title]) "\"")
                           (str "\"" (get-in payload [:answers :talk-title]) "\" submitted by "
                                (or (:name (first (:speakers payload))) "someone")))
    "comms.rendered" (str "Would send: \"" (:subject payload) "\" to " (:to payload)
                          (when (:has-ics? payload) " (with calendar invite)"))
    "comms.sent" (str "Emailed \"" (:subject payload) "\" to " (:to payload)
                      (when (:has-ics? payload) " (with calendar invite)"))
    "comms.failed" (str "FAILED to email " (:to payload) ": " (:error payload))
    "rating.set" (str (if (:previous-stars payload)
                        (str "Changed a rating from " (fmt-stars (:previous-stars payload))
                             " to " (fmt-stars (:stars payload)))
                        (str "Rated " (fmt-stars (:stars payload)) " stars")))
    "comment.added" (str "Commented: " (let [b (str (:body payload))]
                                         (if (> (count b) 90)
                                           (str (subs b 0 90) "…") b)))
    "submission.status-changed" (str "Status " (:from payload) " → " (:to payload))
    "submission.priority-toggled" (if (:priority payload)
                                    "Flagged for discussion"
                                    "Unflagged")
    "submission.notified" (str "Informed " (:to payload) " — \"" (:subject payload) "\"")
    "submission.answers-updated" (str "Speaker edited " (str/join ", " (:changed payload)))
    "person.profile-updated" (str "Profile updated: " (str/join ", " (:changed payload)))
    "task.installed" (str "Task added: " (:label payload))
    "task.completed" (str "Task done: " (:key payload)
                          (when (:value payload) (str " — " (:value payload))))
    type))

(defn log-region
  "The part of the log page the time-travel slider repaints."
  [event log-entries]
  [:div#log-region
   [:div.ui.segment
    (if (empty? log-entries)
      [:div.empty-state "Nothing recorded yet."]
      (for [e (reverse log-entries)]
        [:div.log-row
         [:div.log-when (or (fmt-when (:at e) (:tz event)) (:at e))]
         [:div.log-type (:type e)]
         [:div.log-what (log-summary e)]
         [:div.log-actor (:actor e)]]))]
   [:div.field-hint
    "This is the actual event log the app runs on — not a report generated "
    "beside it. Every screen in this tool is derived by replaying these rows, "
    "so nothing can happen without appearing here."]])

(defn log-page
  [event log-entries person & [time-travel]]
  (organizer-shell
   (str "Log — " (:name event))
   {:event event :active :log :person person :crumb "Log"}
   (header "Log"
           (str (count log-entries) " recorded event"
                (when (not= 1 (count log-entries)) "s") " — newest first"))

   (time-travel-bar event time-travel)
   (log-region event log-entries)))

;; --- 3d. Submission detail --------------------------------------------------

(defn submission-detail-page
  "One talk, in full — every answer under its own snapshot label, the speaker
   block, and the same inline controls as the board so you never have to go
   back to act on what you just read."
  [event row {:keys [person coverage-target notice]}]
  (let [sp (first (:speakers row))
        mine (when person (:stars (first (filter #(= (:id person) (:person-id %))
                                                 (:ratings row)))))]
    (organizer-shell
     (str (get-in row [:answers :talk-title]) " — " (:name event))
     {:event event :active :board :person person :crumb "Review Board"}
     (notice-region event notice)
     (header (get-in row [:answers :talk-title])
             (str (:name sp) (when (:org sp) (str " · " (:org sp)))
                  (when (submissions/captured? row)
                    (str " · captured on their behalf ("
                         (str/replace (str (:source row)) #"^on-behalf-of:" "") ")")))
             [:a.ui.basic.button {:href (str "/events/" (:slug event) "/board")}
              "← Board"])

      ;; TREATMENT B, "Basecamp" (Gene ratified 2026-08-10): the proposal at a
      ;; comfortable reading measure with the speaker in the rail, and the
      ;; committee as a full-width THREAD at the page's foot — you walk through
      ;; the talk before you reach the room's opinions. The geometry is the
      ;; anti-anchoring.
     [:div.b-facts {:style "margin:-0.6em 0 1.2em;"}
      (get-in row [:answers :session-format])
      (when-let [t (get-in row [:answers :track])] (str " · " t))
      (when-let [o (get-in row [:answers :org-size])] (str " · " o))
      (when-let [i (get-in row [:answers :industry])] (str " · " i))]

     [:div.sd-layout
      [:div.sd-main
       [:div.cfp-section-title "The proposal"]
        ;; Rendered from the SNAPSHOT, so this reads exactly as it did the day
        ;; it was submitted even if the live form has moved on.
       [:dl.facts.sd-prose
        (for [f (submissions/session-fields (:form-snapshot row))
              :let [a (get (:answers row) (keyword (name (:id f))))
                    captured? (= :captured-text (keyword (name (:id f))))]
              :when a]
          (list [:dt {:key (str "t" (name (:id f)))}
                 (:label f)
                 (when (:private f) [:span.pc-badge {:style "margin-left:0.5em;"} "PC ONLY"])]
                [:dd {:key (str "d" (name (:id f)))
                      :class (when (:private f) "private-note")
                      :style (if captured?
                                ;; The raw paste, verbatim and visibly so.
                               "font-weight:400; white-space:pre-wrap; font-family:ui-monospace,Menlo,monospace; font-size:0.86em; background:#fafafa; padding:0.6em 0.8em; border-radius:4px;"
                               "font-weight:400; white-space:pre-wrap;")}
                 a]))]]

       ;; EVERYTHING we hold about the speaker, labeled (Gene, 2026-08-10:
       ;; "all the speaker information — especially LinkedIn").
      [:div.sd-rail
       [:div.roster-card
        [:img.sd-photo {:src (or (not-blank (:headshot-url sp))
                                 (pool-face (or (:person-id sp) (:name sp))))
                        :alt (:name sp)}]
        [:div.sp-name (:name sp)]
        (when-let [t (not-blank (:title sp))] [:div.sp-meta t])
        (when-let [o (not-blank (:org sp))] [:div.sp-meta o])
        [:dl.facts.sp-facts
         [:dt "Email"]
         [:dd [:a {:href (str "mailto:" (:email sp))} (:email sp)]]
         (when-let [u (not-blank (:linkedin-url sp))]
           (list [:dt {:key "lt"} "LinkedIn"]
                 [:dd {:key "ld"}
                  [:a {:href u :target "_blank" :rel "noopener"}
                   (str/replace u #"^https?://(www\.)?" "")]]))
         (when-let [u (not-blank (:sessionize-url sp))]
           (list [:dt {:key "st"} "Sessionize"]
                 [:dd {:key "sd"}
                  [:a {:href u :target "_blank" :rel "noopener"}
                   (str/replace u #"^https?://(www\.)?" "")]]))
         (when-let [u (not-blank (:website-url sp))]
           (list [:dt {:key "wt"} "Website"]
                 [:dd {:key "wd"}
                  [:a {:href u :target "_blank" :rel "noopener"}
                   (str/replace u #"^https?://(www\.)?" "")]]))
         (when-let [u (not-blank (:twitter-url sp))]
           (list [:dt {:key "xt"} "Twitter / X"]
                 [:dd {:key "xd"}
                  [:a {:href u :target "_blank" :rel "noopener"}
                   (str/replace u #"^https?://(www\.)?" "")]]))]
        (when (not-blank (:bio sp))
          (list [:div.cfp-section-title {:key "bh" :style "margin-top:0.9em;"} "Bio"]
                [:div.sp-bio {:key "bb"} (:bio sp)]))]]]

      ;; The conversation, at the artifact's foot.
     [:div.sd-thread {:id (str "sub-" (:id row))}
      [:div.cfp-section-title "The committee — the conversation so far"]
      [:div.verdict-line
       [:span.avg (or (fmt-mean (:mean row)) "—")]
       [:span (:n row) " vote" (when (not= 1 (:n row)) "s")]
       [:span.sep "·"]
       [:span (count (:comments row)) " comment"
        (when (not= 1 (count (:comments row))) "s")]
       (when (:split? row) [:span.b-split "SPLIT"])]
      (let [items (sort-by :at
                           (concat (map #(assoc % :kind :rating) (:ratings row))
                                   (map #(assoc % :kind :comment) (:comments row))))]
        (if (empty? items)
          [:p.field-hint "Nobody has weighed in yet — you're first."]
          (for [[i it] (map-indexed vector items)]
            [:div.bubble {:key i}
             [:span.sd-avatar (initials (:person-name it))]
             [:div.balloon
              [:span.who (:person-name it)]
              [:span.when (fmt-instant (:at it) (:tz event))]
              (if (= :rating (:kind it))
                [:span.stars-inline " rated ★ " (fmt-mean (:stars it))]
                [:div.balloon-body (:body it)])]])))
      (when person
        [:div.rate-strip
         [:span.lbl "Your take" (when mine [:span.rate-card-saved " · Saved ✓"])]
         (row-controls event row person mine)])
      [:div.field-hint {:style "margin-top:0.8em;"}
       "Coverage target for this event is " (or coverage-target 2)
       " review" (when (not= 1 coverage-target) "s") "."]])))

;; --- 3e2. Settings: exports, API token, webhooks ----------------------------

(defn mask-webhook-url
  "A Slack webhook URL is a CREDENTIAL — anyone holding it can post to the
   channel — so the page proves which one is configured without handing it back
   out. Host and the first path segment stay (enough to recognise it); the two
   secret segments are dots."
  [url]
  (let [s (str url)]
    (if-let [[_ head] (re-find #"^(https?://[^/]+/services/)" s)]
      (let [parts (str/split (subs s (count head)) #"/")]
        (str head (first parts) "/••••••/••••••••••"))
      (if (> (count s) 24) (str (subs s 0 24) "…") s))))

(defn- slack-form
  "Paste-a-URL + tick-the-moments. One form, one Save — the same shape as every
   other integration on this page."
  [event slack-groups current]
  (let [chosen (if current
                 (set (map str (or (:groups current) [])))
                 (set (map :key (filter :default? slack-groups))))]
    [:form.ui.form.add-member-form
     {:method "post" :action (str "/api/events/" (:slug event) "/slack/set")}
     [:div.field
      [:label "Incoming webhook URL"]
      [:input {:type "url" :name "webhook-url" :required true
               :placeholder "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXX"}]]
     [:div.field
      [:label "Post to Slack when…"]
      (for [g slack-groups]
        [:div.field {:key (:key g) :style "margin:0.25em 0 0.25em 0;"}
         [:label {:style "font-weight:400; cursor:pointer;"}
          [:input (cond-> {:type "checkbox" :name "groups" :value (:key g)
                           :style "margin-right:0.5em;"}
                    (chosen (:key g)) (assoc :checked true))]
          [:strong (:label g)]
          [:span.field-hint {:style "margin:0 0 0 0.5em; display:inline;"} (:help g)]]])]
     [:button.ui.small.primary.button {:type "submit"} "Save Slack settings"]]))

(defn exports-page
  "Exports & API — the one page an integrator or a judge needs to FIND.

   These four files and the REST base already existed, addressable and public,
   but only Settings mentioned them and only halfway down. A capability nobody
   can find is a capability you don't have, so it gets a nav item and a page of
   its own. Nothing here is new machinery: every link is a URL that already
   worked."
  [host event {:keys [person]}]
  (let [base (str host)
        slug (:slug event)
        export (fn [f] (str base "/events/" slug "/exports/" f))]
    (organizer-shell
     (str "Exports & API — " (:name event))
     {:event event :active :exports :person person :crumb "Exports & API"}
     (header "Exports & API"
             "Public URLs, no authentication, shaped like ai.engineer's own.")

     [:div.ui.segment
      [:p.field-hint
       "Every file below describes the " [:strong "published program only"] " — "
       "a session appears once it is accepted AND its speaker has been informed. "
       "Nothing is published before the speaker knows."]
      [:dl.facts
       [:dt "sessions.json"]
       [:dd [:a.cfp-url {:href (export "sessions.json") :target "_blank" :rel "noopener"}
             (export "sessions.json")]
        [:div.field-hint "Every published session: title, abstract, format, room and time."]]
       [:dt "speakers.json"]
       [:dd [:a.cfp-url {:href (export "speakers.json") :target "_blank" :rel "noopener"}
             (export "speakers.json")]
        [:div.field-hint "Every published speaker: name, org, title, bio, links."]]
       [:dt "calendar.ics"]
       [:dd [:a.cfp-url {:href (export "calendar.ics") :target "_blank" :rel "noopener"}
             (export "calendar.ics")]
        [:div.field-hint "Subscribable calendar with STABLE UIDs — a room assigned "
         "late amends the invite instead of duplicating it."]]
       [:dt "llms.txt"]
       [:dd [:a.cfp-url {:href (str base "/events/" slug "/llms.txt")
                         :target "_blank" :rel "noopener"}
             (str base "/events/" slug "/llms.txt")]
        [:div.field-hint "The whole program as plain text, for an agent to read."]]]]

     [:div.ui.segment
      [:h4.ui.header "REST API"]
      [:p.field-hint
       "Same data plus unpublished rows and per-submission detail, with a key. "
       [:strong "Private fields are never returned"] " — notes to the committee "
       "stay with the committee, key or not."]
      [:dl.facts
       [:dt "Base URL"]
       [:dd [:div.cfp-url (str base "/api/v1/events/" slug)]]
       [:dt "Reference"]
       [:dd [:a.cfp-url {:href (str base "/api/v1/events/" slug "/docs")
                         :target "_blank" :rel "noopener"}
             (str base "/api/v1/events/" slug "/docs")]
        [:div.field-hint "Every endpoint, with a curl line for each. "
         "Public — you can send that link to an integrator."]]
       [:dt "Endpoints"]
       [:dd (for [{:keys [method path]} exports/api-endpoints
                  :when (str/starts-with? path "/api/v1")]
              [:div.cfp-url {:key path}
               method " " (str/replace path "{slug}" slug)])]]
      [:div.field-hint
       "Keys are minted and revoked on "
       [:a {:href (str "/events/" slug "/settings")} "Settings → API keys"] "."]])))

(defn api-docs-page
  "The API reference — PUBLIC, and served from under /api/v1 so it inherits the
   same reachability as the endpoints it documents. A reference an integrator
   has to log in to read is a reference nobody reads.

   Generated from `exports/api-endpoints`, so a new endpoint cannot ship
   undocumented: the table IS the docs, and the service index at /api/v1/ is
   rendered from the same vector.

   The framing is deliberate. This page is written for the person Gene was for
   fifteen years — someone about to write a scraper — and its first job is to
   tell them they do not need one."
  [host event]
  (let [base (str host)
        slug (:slug event)
        api (str base "/api/v1/events/" slug)
        curl (fn [path & [{:keys [token query]}]]
               (str "curl -s "
                    (when token "-H \"Authorization: Bearer $TOKEN\" ")
                    "'" base (str/replace path "{slug}" slug) (or query "") "'"))
        ;; The endpoint table is shared with the JSON service index, where
        ;; markdown backticks are the right way to mark a field name. Here they
        ;; become <code> rather than showing up as literal grave accents.
        prose (fn [s]
                (map-indexed (fn [i part]
                               (if (odd? i) [:code {:key i} part] part))
                             (str/split (str s) #"`")))]
    (page-shell
     (str (events/display-name event) " — API reference")
     [:div.ui.container {:style "max-width:860px; margin-top:2em;"}
      [:div.cfp-masthead
       [:h1.ui.header (events/display-name event)]
       [:div.cfp-meta "API reference · v1"]]

      [:div.ui.segment
       [:h3.ui.header "You do not need a scraper"]
       [:p "Everything on the public program is available as JSON, ICS and markdown, "
        "with no key and no account. A key widens what you can read — the submissions "
        "that are not public yet, the statuses, the change feed."]
       [:p [:strong "Every entity carries a stable id, and every join carries the id on "
            "both sides."] " A session lists " [:code "speakerIds"] "; a speaker lists "
        [:code "sessionIds"] ". Person ids are stable across events, so the same human at "
        "two conferences is the same id. Never match on a name — that is how one speaker "
        "becomes three."]
       [:dl.facts
        [:dt "Base URL"] [:dd [:div.cfp-url api]]
        [:dt "Start here"]
        [:dd [:a.cfp-url {:href api} api]
         [:div.field-hint "The discovery document: ids, dates, timezone, CFP state, "
          "and a link to every endpoint below."]]
        [:dt "Service index"]
        [:dd [:a.cfp-url {:href (str base "/api/v1/")} (str base "/api/v1/")]]]]

      [:div.ui.segment
       [:h3.ui.header "Authentication"]
       [:p "A key goes in a header, or in " [:code "?token="] " when a header is "
        "inconvenient:"]
       [:pre.cfp-url (curl "/api/v1/events/{slug}/submissions" {:token true})]
       [:pre.cfp-url (curl "/api/v1/events/{slug}/submissions" {:query "?token=$TOKEN"})]
       [:p.field-hint
        [:strong "A token widens; it never unlocks."] " Answers the event's form marks "
        "private — notes to the committee — are absent from every response, "
        "authenticated or not. They were promised to the committee, and a key does not "
        "change who promised."]
       [:p.field-hint "Organizers mint and revoke named keys on the event's Settings "
        "page. Revoking one key never disturbs another."]]

      [:div.ui.segment
       [:h3.ui.header "What is public"]
       [:p "Public responses describe the " [:strong "published program"] ": a session "
        "appears once it is accepted " [:em "and"] " its speaker has been informed — and "
        "not while it sits in a known scheduling conflict. Nothing is published before "
        "the speaker knows."]
       [:p "A session that has no room or time yet is still published, with "
        [:code "placed: false"] " and null times. That is honest; an invented time would "
        "propagate into someone else's site."]]

      [:div.ui.segment
       [:h3.ui.header "Endpoints"]
       (for [{:keys [method path auth summary notes params]} exports/api-endpoints]
         [:div.api-endpoint {:key path}
          [:h4.ui.header
           [:span.cfp-url method " " (str/replace path "{slug}" slug)]
           (when (= :token auth) [:span.field-hint " · token required"])]
          [:p (prose summary)]
          (when notes [:p.field-hint (prose notes)])
          (when (seq params)
            [:dl.facts
             (for [[p desc] params]
               (list [:dt {:key (str p "-t")} [:code p]]
                     [:dd {:key (str p "-d")} [:span.field-hint (prose desc)]]))])
          [:pre.cfp-url (curl path (when (= :token auth) {:token true}))]])]

      [:div.ui.segment
       [:h3.ui.header "Polling without being rude"]
       [:p "Every response carries an " [:code "ETag"] ". Send it back and an unchanged "
        "program answers " [:code "304"] " with no body:"]
       [:pre.cfp-url (str "curl -s -H 'If-None-Match: \"$ETAG\"' -o /dev/null -w '%{http_code}\\n' '"
                          api "/sessions'")]
       [:p "Or read " [:code "scheduleVersion"] " — it bumps on any change to the event — "
        "and ask the change feed what moved:"]
       [:pre.cfp-url (curl "/api/v1/events/{slug}/changes" {:token true :query "?since=0"})]
       [:p.field-hint "The change feed returns ids only — what changed and when, never "
        "the contents. Re-read the entity it names."]]

      [:div.ui.segment
       [:h3.ui.header "Push, instead of polling"]
       [:p "Webhooks deliver the same facts as they happen. An organizer registers a URL "
        "on the event's Settings page and picks which event types to receive; deliveries "
        "and their responses are visible there too."]]

      [:div.field-hint {:style "margin-top:2.5em;"}
       [:a {:href (str base "/agenda/" slug)} "Public agenda"] " · "
       [:a {:href (str base "/events/" slug "/llms.txt")} "llms.txt"] " · "
       [:a {:href (str base "/cfp/" slug)} "Call for speakers"]]])))

(defn settings-page
  "Everything an integrator needs, in one place: the open-data URLs, the API
   token, Slack, and the webhooks. Event details are read-only here for now —
   editing them lives on the create form until there is a reason to duplicate
   it."
  [host event {:keys [person webhooks deliveries notice slack-groups
                      api-keys new-key confirming-key]}]
  (let [base (str host)
        export (fn [f] (str base "/events/" (:slug event) "/exports/" f))
        token (get-in event [:settings :api-token])]
    (organizer-shell
     (str "Settings — " (:name event))
     {:event event :active :settings :person person :crumb "Settings"}
     (notice-region event notice)
     (header "Settings" "Exports, API access, Slack and webhooks.")

      ;; The close DATE lives here, post-create. The create form only ever asks
      ;; the one-sentence version ("…open until"); this is where an organizer
      ;; extends the deadline the week everyone always asks for it, or clears it
      ;; and leaves the call open. Opening and closing outright are the two
      ;; buttons on the dashboard — this is the scheduled half, they are the
      ;; deliberate half.
     [:div.ui.segment
      [:h4.ui.header "Call for speakers"]
      [:dl.facts
       [:dt "Right now"]
       [:dd (case (submissions/cfp-state event)
              :open "Open — accepting submissions"
              :not-open-yet "Not open yet"
              :closed "Closed to new submissions")]]
      [:form.ui.form {:method "post"
                      :action (str "/api/events/" (:slug event) "/cfp/close-date")}
       [:div.field
        [:label "Closes end of day (" (:tz event) ")"]
        [:input {:type "date" :name "cfp-closes-on"
                 :value (or (some-> (:cfp-closes-at event)
                                    (fmt-close-date (:tz event)))
                            "")}]
        [:div.field-hint "Leave blank to keep the call open indefinitely. "
         "A date in the past means the call is closed."]]
       [:button.ui.small.button {:type "submit"} "Save close date"]]]

     [:div.ui.segment
      [:h4.ui.header "Open data"]
      [:p.field-hint
       "Public URLs, no authentication. They describe the "
       [:strong "published program only"] " — a session appears once it is "
       "accepted AND its speaker has been informed. Nothing is published before "
       "the speaker knows."]
      [:dl.facts
       [:dt "sessions.json"]
       [:dd [:a.cfp-url {:href (export "sessions.json") :target "_blank"} (export "sessions.json")]]
       [:dt "speakers.json"]
       [:dd [:a.cfp-url {:href (export "speakers.json") :target "_blank"} (export "speakers.json")]]
       [:dt "calendar.ics"]
       [:dd [:a.cfp-url {:href (export "calendar.ics") :target "_blank"} (export "calendar.ics")]]
       [:dt "llms.txt"]
       [:dd [:a.cfp-url {:href (str base "/events/" (:slug event) "/llms.txt") :target "_blank"}
             (str base "/events/" (:slug event) "/llms.txt")]]]
      [:div.field-hint
       "These match the shapes ai.engineer already publishes, so they drop into "
       "an existing pipeline without a migration."]]

     [:div.ui.segment
      [:h4.ui.header "API token"]
      [:p.field-hint
       "Reads the same data plus unpublished rows and per-submission detail. "
       [:strong "Private fields are never returned"] " — notes to the planning "
       "committee stay with the committee, token or not."]
      [:div.cfp-url {:style "background:#f7f7f8; padding:0.6em 0.8em; border-radius:4px;
                             word-break:break-all;"}
       token]
      [:div.field-hint {:style "margin-top:0.6em;"} "Try it:"]
      [:pre {:style "background:#f7f7f8; padding:0.7em 0.9em; border-radius:4px;
                     overflow-x:auto; font-size:0.8em;"}
       (str "curl -H 'Authorization: Bearer " token "' \\\n"
            "  " base "/api/v1/events/" (:slug event) "/sessions?status=all")]
      [:dl.facts {:style "margin-top:0.8em;"}
       [:dt "Endpoints"]
       [:dd [:div.cfp-url "GET /api/v1/events/" (:slug event) "/sessions"]
        [:div.cfp-url "GET /api/v1/events/" (:slug event) "/speakers"]
        [:div.cfp-url "GET /api/v1/events/" (:slug event) "/submissions/:id"]]]]

      ;; --- API keys ------------------------------------------------------
      ;;
      ;; The list shows a PREFIX, never the material. One shared token meant
      ;; revoking the leaked integration also revoked the chair's curl line, so
      ;; nobody revoked anything; named keys make revocation small enough to
      ;; actually do. Every control here is a plain form POST — the whole page
      ;; re-renders, which is why the new key can be shown once without any
      ;; client-side state to hold it.
     [:div.ui.segment
      [:h4.ui.header "API keys"]
      [:p.field-hint
       "Named keys for this event, each revocable on its own. A key buys the "
       "same reads as the token above — unpublished rows and per-submission "
       "detail. " [:strong "Private fields are never returned"] ", key or not."]

      (when new-key
        [:div.ui.positive.message {:style "margin-bottom:1em;"}
         [:div.header (str "Copy “" (:label new-key) "” now")]
         [:p {:style "margin:0.4em 0 0.6em 0;"}
          "This is the only time we will show it. We store it, but the page "
          "never prints it again — if you lose it, revoke it and make another."]
         [:div.cfp-url {:style "background:#fff; padding:0.6em 0.8em;
                                border-radius:4px; word-break:break-all;
                                font-weight:600;"}
          (:key new-key)]])

      (if (seq api-keys)
        [:div.member-list
         (for [k api-keys]
           [:div.member-row {:key (str (:id k))}
            [:div.member-who
             [:span.member-name (:label k)]
             [:div.member-email
              [:span.cfp-url (exports/key-prefix (:key k))]
              (when-let [at (:created-at k)]
                (str " · added " (or (fmt-when at (:tz event)) at)))]]
            (if (= confirming-key (:id k))
               ;; Two-step confirmation, rendered by the SERVER. No confirm()
               ;; dialog: a modal blocks the SSE stream, and "am I confirming?"
               ;; is state the browser has no business owning.
              [:div {:style "display:flex; gap:0.4em; align-items:center;"}
               [:span.field-hint {:style "margin:0;"} "Revoke it?"]
               [:form {:method "post"
                       :action (str "/api/events/" (:slug event) "/api-keys/revoke")}
                [:input {:type "hidden" :name "id" :value (:id k)}]
                [:input {:type "hidden" :name "confirm" :value "yes"}]
                [:button.ui.mini.negative.button {:type "submit"} "Revoke"]]
               [:a.ui.mini.basic.button {:href (str "/events/" (:slug event) "/settings")}
                "Keep it"]]
              [:form {:method "post"
                      :action (str "/api/events/" (:slug event) "/api-keys/revoke")}
               [:input {:type "hidden" :name "id" :value (:id k)}]
               [:button.ui.mini.basic.button {:type "submit"} "Revoke"]])])]
        [:p.field-hint "No named keys yet."])

      [:form.ui.form.add-member-form {:method "post"
                                      :action (str "/api/events/" (:slug event) "/api-keys/create")}
       [:div.field
        [:label "Label"]
        [:input {:type "text" :name "label"
                 :placeholder "Zapier · the schedule site · Ann's laptop"}]]
       [:button.ui.small.primary.button {:type "submit"} "Create key"]
       [:div.field-hint {:style "margin-top:0.5em;"}
        "Name it after where it will live. That name is what turns “a key "
        "leaked” into “that key leaked”."]]]

     (let [slack (get-in event [:settings :slack])
           chosen (set (or (:groups slack) []))]
       [:div.ui.segment
        [:h4.ui.header "Slack"]
        [:p.field-hint
         "Post to the channel your committee is already in. Paste an "
         [:a {:href "https://api.slack.com/messaging/webhooks" :target "_blank"
              :rel "noopener"} "incoming-webhook URL"]
         " and pick which moments are worth interrupting people for. "
         [:strong "Private answers are never posted"]
         " — a channel is a wider room than the programming committee, so the "
         "notes-to-committee field stays out of it. A failing post is logged and "
         "dropped; it can never fail a speaker's submission."]
        (if (not-blank (:webhook-url slack))
          [:div
           [:dl.facts
            [:dt "Webhook"] [:dd [:span.cfp-url (mask-webhook-url (:webhook-url slack))]]
            [:dt "Posts on"]
            [:dd (if (seq (:groups slack))
                   (str/join ", " (for [g slack-groups
                                        :when (chosen (:key g))]
                                    (:label g)))
                   "nothing selected — no messages will be sent")]]
           [:div {:style "display:flex; gap:0.5em; align-items:center;"}
            [:form {:method "post" :action (str "/api/events/" (:slug event) "/slack/test")}
             [:button.ui.small.primary.button {:type "submit"} "Send a test message"]]
            [:form {:method "post" :action (str "/api/events/" (:slug event) "/slack/remove")}
             [:button.ui.mini.basic.button {:type "submit"} "Remove"]]]
           [:div.field-hint {:style "margin-top:0.6em;"}
            "The test posts a real message to the channel, and says here whether "
            "Slack took it."]
           [:details {:style "margin-top:0.9em;"}
            [:summary {:style "cursor:pointer; font-size:0.9em; color:#666;"}
             "Change the URL or what gets posted"]
            (slack-form event slack-groups slack)]]
          (slack-form event slack-groups nil))])

     [:div.ui.segment
      [:h4.ui.header "Webhooks"]
      [:p.field-hint
       "Every stored event can be POSTed as JSON to a URL you control — "
       "Zapier, Make, n8n, or your own endpoint. A failing webhook is logged and "
       "dropped; it can never fail a speaker's submission."]
      (if (seq webhooks)
        [:div.member-list
         (for [w webhooks]
           [:div.member-row {:key (str (:id w))}
            [:div.member-who
             [:span.member-name.cfp-url (:url w)]
             [:div.member-email
              (if (seq (:types w))
                (str "only: " (str/join ", " (:types w)))
                "every event type")]]
            [:form {:method "post"
                    :action (str "/api/events/" (:slug event) "/webhooks/remove")}
             [:input {:type "hidden" :name "id" :value (:id w)}]
             [:button.ui.mini.basic.button {:type "submit"} "Remove"]]])]
        [:p.field-hint "No webhooks yet."])

      [:form.ui.form.add-member-form {:method "post"
                                      :action (str "/api/events/" (:slug event) "/webhooks/add")}
       [:div.two.fields
        [:div.field
         [:label "URL"]
         [:input {:type "url" :name "url" :placeholder "https://hooks.example.com/cfp"}]]
        [:div.field
         [:label "Event types " [:span.optional "(optional, comma separated)"]]
         [:input {:type "text" :name "types"
                  :placeholder "submission.created, submission.notified"}]]]
       [:button.ui.small.primary.button {:type "submit"} "Add webhook"]
       [:div.field-hint {:style "margin-top:0.5em;"}
        "Leave types blank to receive everything."]]]

     [:div.ui.segment
      [:h4.ui.header "Airtable"]
      [:p.field-hint
       "Mirror submissions one-way into a base, so automations you already have "
       "keep firing. We never read Airtable back — two systems that both think "
       "they own a record is how you get a data-loss story. "
       [:strong "Private fields are never sent."]]
      (if-let [a (get-in event [:settings :airtable])]
        [:div
         [:dl.facts
          [:dt "Base"] [:dd [:span.cfp-url (:base-id a)]]
          [:dt "Table"] [:dd (:table a)]]
         [:form {:method "post" :action (str "/api/events/" (:slug event) "/airtable/remove")}
          [:button.ui.mini.basic.button {:type "submit"} "Disconnect"]]]
        [:form.ui.form.add-member-form
         {:method "post" :action (str "/api/events/" (:slug event) "/airtable/set")}
         [:div.three.fields
          [:div.field [:label "Base ID"]
           [:input {:type "text" :name "base-id" :placeholder "appXXXXXXXXXXXXXX"}]]
          [:div.field [:label "Table"]
           [:input {:type "text" :name "table" :placeholder "Submissions"}]]
          [:div.field [:label "Personal access token"]
           [:input {:type "password" :name "token" :placeholder "pat…"}]]]
         [:button.ui.small.primary.button {:type "submit"} "Connect Airtable"]])]

     [:div.ui.segment
      [:h4.ui.header "Committee push email"]
      [:p.field-hint
       "When a submission arrives, every committee member gets the "
       [:strong "whole proposal inline"] " — every answer under its label, the "
       "speaker block, and the private notes to the committee. This is BusyConf's "
       "pattern, and it is why their committees actually reviewed: the work came "
       "to the inbox instead of asking for a login."]
      [:div.field-hint
       (if (not (false? (get-in event [:settings :pc-push-enabled])))
         "Enabled for this event."
         "Disabled for this event.")
       " Delivery follows the same SMTP setting as everything else."]]

     [:div.ui.segment
      [:h4.ui.header "Recent deliveries"]
      (if (seq deliveries)
        (for [d (take 20 deliveries)]
          [:div.log-row
           [:div.log-when (or (fmt-when (:at d) (:tz event)) (:at d))]
           [:div.log-type (:event-type d)]
           [:div.log-what.cfp-url (:url d)
            (when (:error d) [:div.b-split (:error d)])]
           [:div.log-actor (if (:ok d) "ok" "failed") " · " (:ms d) "ms"]])
        [:div.empty-state "No deliveries yet."])
      [:div.field-hint
       "This list lives in memory and is forgotten on restart — it is a debugging "
       "aid, not a delivery record. A durable one arrives with the comms slice."]]

     [:div.ui.segment
      [:h4.ui.header "Event details"]
      [:dl.facts
       [:dt "Name"] [:dd (:name event)]
       [:dt "Slug"] [:dd (:slug event)]
       [:dt "Dates"] [:dd (or (fmt-date-range (:starts-on event) (:ends-on event)) "not set")]
       [:dt "Location"] [:dd (or (:location event) "not set")]
       [:dt "Time zone"] [:dd (:tz event)]
       [:dt "Speaker support email"] [:dd (or (:support-email event) "not set")]]
      [:div.field-hint "Read-only for now — editing arrives with the settings slice."]])))

;; --- 3e3. The schedule builder ----------------------------------------------
;;
;; The blocking sheet with superpowers. Partial states are first-class: a
;; session can be placed with no room (its own column), and a conflict is a chip
;; that names the collision — never a validation error that blocks the save.

(defn- day-tab [event day active-day]
  [:a.chip {:key day
            :class (when (= day active-day) "on")
            :href (str "/events/" (:slug event) "/schedule?day=" day)}
   (or (schedule/day-label event day) day)])

(defn schedule-status-bar
  [stats]
  [:div#schedule-status.status-bar
   [:div [:span.n (:placed stats)] "/" (:accepted stats) " "
    [:span.lbl "accepted placed"]]
   [:span.status-sep "·"]
   [:div [:span.n (:unplaced stats)] " " [:span.lbl "in the tray"]]
   [:span.status-sep "·"]
   [:div [:span.n (:unroomed stats)] " " [:span.lbl "unroomed"]]
   [:span.status-sep "·"]
   [:div [:span.n {:style (when (pos? (:conflicts stats)) "color:#f2711c;")}
          (:conflicts stats)] " " [:span.lbl "conflicts"]]
   (for [d (:per-day stats)]
     (list [:span.status-sep {:key (str "s" (:day d))} "·"]
           [:div {:key (:day d)}
            [:span.lbl (:label d) ": "]
            [:span.n (:sessions d)] [:span.lbl " sessions"]
            (when (pos? (:blocks d))
              (list " " [:span.n (:blocks d)] [:span.lbl " blocks"]))]))])

(defn conflict-chips
  "Named collisions with BOTH fixes offered, because the tool doesn't know which
   side should move."
  [event conflicts]
  (when (seq conflicts)
    [:div {:style "margin-bottom:1em;"}
     (for [[i c] (map-indexed vector conflicts)]
       [:div.conflict-chip {:key (str i)}
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

(defn- room-options [rooms selected]
  (list
   [:option {:key "none" :value "" :selected (nil? selected)} "no room yet"]
   (for [r rooms]
     [:option (cond-> {:key (:id r) :value (:id r)}
                (= (:id r) selected) (assoc :selected true))
      (:name r)])))

(defn- placed-card
  [event p rooms conflicted?]
  [:div.sched-card {:class (when conflicted? "conflicted")}
   [:div [:a {:href (str "/events/" (:slug event) "/submissions/" (:submission-id p))}
          (:title p)]]
   [:div.who (str/join ", " (map :name (:speakers p)))]
   [:div.acts
    ;; Quick room re-assign: the late-room-assignment workflow, one control.
    [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/place")}
     [:input {:type "hidden" :name "submission-id" :value (:submission-id p)}]
     [:input {:type "hidden" :name "day" :value (:day p)}]
     [:input {:type "hidden" :name "start" :value (schedule/minutes->hhmm (:start p))}]
     [:input {:type "hidden" :name "duration" :value (- (:end p) (:start p))}]
     [:select {:name "room-id" :onchange "this.form.submit()"}
      (room-options rooms (:room-id p))]]
    [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/clear")}
     [:input {:type "hidden" :name "submission-id" :value (:submission-id p)}]
     [:button {:type "submit"} "Clear"]]]])

(defn- block-card [event b]
  [:div.sched-card.block
   [:div (:label b)]
   [:div.acts
    [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/block-remove")}
     [:input {:type "hidden" :name "block-id" :value (:id b)}]
     [:button {:type "submit"} "Remove"]]]])

(defn schedule-grid
  "Rooms as columns plus an Unroomed column; one row per occupied start time.
   Compressed to what is actually there — an empty 15-minute lattice is a lot of
   scrolling to say nothing."
  [event day {:keys [rooms placed blocks conflicted-ids locked?]}]
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
         (for [c cols] [:th {:key (str (:id c))} (:name c)])]]
       [:tbody
        (for [t starts]
          [:tr {:key (str t)}
           [:td.sched-time (schedule/minutes->display t)]
           (for [c cols]
             [:td {:key (str (:id c)) :class (when (nil? (:id c)) "unroomed-col")}
              (for [p day-placed
                    :when (and (= t (:start p)) (= (:room-id p) (:id c)))]
                (placed-card event p rooms (contains? conflicted-ids (:submission-id p))))
              (for [b day-blocks
                    :when (and (= t (:start b)) (= (:room-id b) (:id c)))]
                (block-card event b))])])]])))

(defn- place-form
  [event submission day rooms]
  [:form.place-form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/place")}
   [:input {:type "hidden" :name "submission-id" :value (:id submission)}]
   [:select {:name "day"}
    (for [d (schedule/event-days event)]
      [:option (cond-> {:key d :value d} (= d day) (assoc :selected true))
       (schedule/day-label event d)])]
   [:input {:type "time" :name "start" :step "900" :value "09:00" :required true}]
   [:input {:type "number" :name "duration" :min "15" :step "15"
            :value (schedule/duration-for event submission) :style "width:5em;"}]
   [:select {:name "room-id"} (room-options rooms nil)]
   [:button.ui.mini.primary.button {:type "submit"} "Place"]])

(defn schedule-page
  [event {:keys [day stats conflicts rooms placed blocks trayed conflicted-ids
                 person locked? lock-version withheld-count]}]
  (organizer-shell
   (str "Schedule — " (:name event))
   {:event event :active :schedule :person person :crumb "Schedule" :sse? true
    :body-attrs (ds/sse-mount (:id event))}

   (header "Schedule"
           "Draft-first. Place things half-decided; the arithmetic and the clashes keep up.")

   (when locked?
     [:div.locked-banner
      [:div [:strong "Locked — " (or lock-version "v1")]
       [:div.field-hint "The draft is frozen. Unlock to keep moving things."]]
      [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/unlock")}
       [:button.ui.small.button {:type "submit"} "Unlock"]]])

   (schedule-status-bar stats)
   (list
    (conflict-chips event conflicts)
    (when (pos? (or withheld-count 0))
        ;; The loud half of "withhold, loudly": conflicted sessions leave the
        ;; public agenda/exports, and this line is why nothing vanishes silently.
      [:div.withheld-note {:key "withheld"}
       [:strong (str withheld-count " session" (when (not= 1 withheld-count) "s"))]
       " held back from the public agenda and exports until the conflict is
       resolved — attendees never see a schedule we know is wrong."]))

   (if (empty? (schedule/event-days event))
     [:div.ui.warning.message
      [:div.header "This event has no dates yet"]
      [:p "Set start and end dates and the day tabs will appear."]]

     (list
      [:div.day-tabs {:key "tabs"}
       (for [d (schedule/event-days event)] (day-tab event d day))]

      [:div {:key "grid"} (schedule-grid event day {:rooms rooms :placed placed
                                                    :blocks blocks
                                                    :conflicted-ids conflicted-ids
                                                    :locked? locked?})]

      (when-not locked?
        [:div.tray {:key "tray"}
         [:h4.ui.header {:style "margin-bottom:0.3em;"}
          "Accepted (unscheduled): " (count trayed)]
         [:div.field-hint {:style "margin-bottom:0.6em;"}
          "Only accepted speakers who have been informed appear here — the agenda "
          "is downstream of the promise, never ahead of it."]
         (if (empty? trayed)
           [:div.empty-state "Everything accepted has a place. "
            (when (pos? (:unroomed stats))
              (str (:unroomed stats) " still need a room, which is fine."))]
           (for [sub trayed]
             [:div.tray-card {:key (str (:id sub))}
              [:div [:strong (get-in sub [:answers :talk-title])]]
              [:div.sub-meta (:name (first (:speakers sub)))
               " · " (get-in sub [:answers :session-format])]
              (place-form event sub day rooms)]))])

      (when-not locked?
        [:div.ui.segment {:key "extras"}
         [:h4.ui.header "Rooms & blocks"]
         [:div {:style "display:flex; gap:2em; flex-wrap:wrap;"}
          [:div {:style "flex:1; min-width:16em;"}
           [:h5.ui.header "Rooms"]
           (if (seq rooms)
             [:div.member-list
              (for [r rooms]
                [:div.member-row {:key (:id r)}
                 [:div.member-who [:span.member-name (:name r)]]
                 [:form {:method "post"
                         :action (str "/api/events/" (:slug event) "/schedule/room-remove")}
                  [:input {:type "hidden" :name "room-id" :value (:id r)}]
                  [:button.ui.mini.basic.button {:type "submit"} "Remove"]]])]
             [:p.field-hint "No rooms yet — sessions can still be placed unroomed."])
           [:form.place-form {:method "post"
                              :action (str "/api/events/" (:slug event) "/schedule/room-add")}
            [:input {:type "text" :name "name" :placeholder "Main Stage" :required true}]
            [:button.ui.mini.button {:type "submit"} "Add room"]]]

          [:div {:style "flex:1; min-width:20em;"}
           [:h5.ui.header "Add a block"]
           [:div.field-hint {:style "margin-bottom:0.4em;"}
            "Lunch, Keynote TBD, Break — placeholders that hold space for "
            "something not yet decided."]
           [:form.place-form {:method "post"
                              :action (str "/api/events/" (:slug event) "/schedule/block-add")}
            [:input {:type "text" :name "label" :placeholder "Lunch" :required true}]
            [:select {:name "day"}
             (for [d (schedule/event-days event)]
               [:option (cond-> {:key d :value d} (= d day) (assoc :selected true))
                (schedule/day-label event d)])]
            [:input {:type "time" :name "start" :step "900" :value "12:00" :required true}]
            [:input {:type "number" :name "duration" :min "15" :step "15" :value "60"
                     :style "width:5em;"}]
            [:select {:name "room-id"} (room-options rooms nil)]
            [:button.ui.mini.button {:type "submit"} "Add block"]]]]])

      (when-not locked?
        [:div {:key "lock" :style "margin-top:1.5em;"}
         [:form {:method "post" :action (str "/api/events/" (:slug event) "/schedule/lock")}
          [:button.ui.button {:type "submit"} "Lock schedule"]]
         [:div.field-hint {:style "margin-top:0.4em;"}
          "Locking freezes the draft and stamps a version. The "
          [:a {:href (str "/events/" (:slug event) "/log")} "Log"] " narrates every change."]])

      [:div {:key "agenda-link" :style "margin-top:1.5em;"}
       [:a.ui.basic.button {:href (str "/agenda/" (:slug event)) :target "_blank"}
        "View the public agenda"]]))))

;; --- 3h. Public agenda ------------------------------------------------------

(defn agenda-page
  "Speaker/attendee-facing. Single column, mobile-first, no organizer chrome —
   and only the published program."
  [event days active-day]
  (page-shell
   (str (events/display-name event) " — Agenda")
   [:div.ui.container {:style "max-width:760px; margin-top:2em;"}
    [:div.cfp-masthead
     [:h1.ui.header (events/display-name event)]
     [:div.cfp-meta "Agenda"]
     (when (:website-url event)
       [:div.cfp-limit [:a {:href (:website-url event)} "Official event website ↗"]])]

    (let [shown (filter #(seq (:items %)) days)]
      (if (empty? shown)
        [:div.ui.segment
         [:div.empty-state
          [:p [:strong "The agenda isn't published yet."]]
          [:p "Sessions appear here once they're scheduled. In the meantime, the "
           [:a {:href (str "/cfp/" (:slug event))} "call for speakers"] " may still be open."]]]

        (list
         (when (> (count shown) 1)
           [:div.day-tabs {:key "tabs"}
            (for [d shown]
              [:a.chip {:key (:day d)
                        :class (when (= (:day d) active-day) "on")
                        :href (str "/agenda/" (:slug event) "?day=" (:day d))}
               (:label d)])])

         (for [d (if active-day (filter #(= active-day (:day %)) shown) shown)]
           [:div {:key (:day d) :style "margin-top:1.5em;"}
            [:h3.ui.header (:label d)]
            (for [item (:items d)]
              [:div.agenda-item {:key (str (:start item) (:title item))}
               [:div.agenda-time (schedule/time-range-display (:start item) (:end item))]
               [:div.agenda-body
                (if (= :block (:kind item))
                  [:div.agenda-block (:title item)]
                  (list
                   [:div.agenda-title {:key "t"} (:title item)]
                   (when (seq (:speakers item))
                     [:div.agenda-who {:key "w"} (str/join ", " (:speakers item))])))
                (when (:room item) [:div.agenda-room (:room item)])]])]))))

    [:div.field-hint {:style "margin-top:2.5em;"}
     "Times are local to the event (" (:tz event) "). "
     [:a {:href (str "/events/" (:slug event) "/exports/calendar.ics")} "Subscribe by calendar"]
     " · "
     [:a {:href (str "/events/" (:slug event) "/exports/sessions.json")} "Session data"]]]))

;; --- 3f. Inform Speakers ----------------------------------------------------

(defn- letter-block [row letter]
  [:div.letter
   [:div.letter-subject (:subject letter)]
   [:div.letter-to "To: " (:to letter)
    " · reply-to comes from the event's speaker support address"]
   [:div.letter-body (:body letter)]])

(defn inform-page
  "Queued decisions, grouped by the letter each will send, each shown IN FULL
   before anyone presses a button. You should never send a letter you haven't
   read."
  [event {:keys [groups informed person dev? mail-configured? mail-status]}]
  (organizer-shell
   (str "Inform Speakers — " (:name event))
   {:event event :active :inform :person person :crumb "Inform Speakers"}
   (header "Inform Speakers"
           "Nothing here has been sent. Read each letter, then send it.")

    ;; States the ACTUAL delivery configuration, always — not only when there is
    ;; a queue. Claiming "sent" when no mailer is configured is the single most
    ;; damaging lie this tool could tell an organizer: they would stop chasing a
    ;; speaker who never heard from them.
   [:div.ui.info.message
    (if mail-configured?
      [:p [:strong "Letters will be emailed."] " " mail-status
       " · Reply-to is this event's speaker support address, so a reply reaches "
       "a person. Every send is recorded on the "
       [:a {:href (str "/events/" (:slug event) "/comms")} "Comms"] " page."]
      [:p [:strong "SMTP is not configured, so nothing is emailed."]
       " Informing still records the decision — the speaker sees their real "
       "status immediately — and the exact letter is kept on the "
       [:a {:href (str "/events/" (:slug event) "/comms")} "Comms"]
       " page so you can send it by hand."])]

   (if (empty? groups)
     [:div.ui.segment
      [:div.empty-state
       [:p [:strong "Nothing waiting."]]
       [:p "Every decision made so far has been communicated. "
        "Decisions appear here when a submission is set to Accepted, "
        "Waitlisted or Declined on the "
        [:a {:href (str "/events/" (:slug event) "/board")} "review board"] "."]]]

     (list
      (for [{:keys [status rows]} groups]
        [:div.inform-group {:key status}
         [:div.inform-head
          [:h3.ui.header {:style "margin:0;"}
           status " — " (count rows) " speaker" (when (not= 1 (count rows)) "s")]
          [:form {:method "post" :action (str "/api/events/" (:slug event) "/inform-all")}
           [:input {:type "hidden" :name "status" :value status}]
           [:button.ui.small.primary.button {:type "submit"}
            "Inform all " (count rows)]]]

         (for [{:keys [submission letter]} rows]
           [:div {:key (str (:id submission)) :style "margin-bottom:1.2em;"}
            [:div {:style "display:flex; align-items:baseline; gap:0.8em;"}
             [:div {:style "flex:1;"}
              [:strong (get-in submission [:answers :talk-title])]
              [:div.sub-meta (:name (first (:speakers submission)))
               " · " (:org (first (:speakers submission)))]]
             [:form {:method "post"
                     :action (str "/api/submissions/" (:id submission) "/inform")}
              [:button.ui.mini.primary.button {:type "submit"} "Inform"]]]
            (letter-block submission letter)])])))

   (when (seq informed)
     [:div {:style "margin-top:2.5em;"}
      [:h4.ui.header "Already informed (" (count informed) ")"]
      (for [s informed]
        [:div.sub-row {:key (str (:id s))}
         [:div.sub-title (get-in s [:answers :talk-title])]
         [:div.sub-meta
          (:notified-status s) " · told "
          (or (fmt-when (:notified-at s) (:tz event)) "—")
          " · " (:email (first (:speakers s)))]])])))

;; --- 3b3. Quick capture -----------------------------------------------------

(defn capture-page
  "Ten seconds from 'a talk arrived in my DMs' to a row on the board.

   Deliberately parses NOTHING. Gene's actual failure mode is that a good talk
   arrives as an email or a LinkedIn message and dies in the inbox because
   getting it into the tool costs more than it is worth. So: paste it, name it
   if you can be bothered, done. The committee can tidy it later — a messy row
   on the board beats a perfect one that was never created."
  [event {:keys [person values errors]}]
  (let [v #(get values % "")]
    (organizer-shell
     (str "Capture a submission — " (:name event))
     {:event event :active :submissions :person person :crumb "Submissions"}
     (header "Add a submission on someone's behalf"
             "Paste what you got. Nothing else is required."
             [:a.ui.basic.button {:href (str "/events/" (:slug event) "/submissions")}
              "All submissions"])

     (when (:_ errors) [:div.ui.negative.message (str/join " " (:_ errors))])

     [:form.ui.form {:method "post" :action (str "/api/events/" (:slug event) "/capture")}
      [:div.field {:class (when (:captured-text errors) "error")}
       [:label "Paste the email or DM"]
       [:textarea {:name "captured-text" :rows 10 :autofocus true
                   :placeholder "Hi Gene — I'd love to talk about how we rebuilt…"}
        (v :captured-text)]
       [:div.field-hint "Stored verbatim. We don't parse it — you or the committee "
        "can fill the real fields in later."]
       (field-error errors :captured-text)]

      [:div.three.fields
       [:div.field
        [:label "Talk title " [:span.optional "(optional)"]]
        [:input {:type "text" :name "title" :value (v :title)}]]
       [:div.field
        [:label "Speaker name " [:span.optional "(optional)"]]
        [:input {:type "text" :name "speaker-name" :value (v :speaker-name)}]]
       [:div.field {:class (when (:speaker-email errors) "error")}
        [:label "Speaker email " [:span.optional "(optional)"]]
        [:input {:type "email" :name "speaker-email" :value (v :speaker-email)}]
        (field-error errors :speaker-email)]]

      [:div.two.fields
       [:div.field
        [:label "Organization " [:span.optional "(optional)"]]
        [:input {:type "text" :name "speaker-org" :value (v :speaker-org)}]]
       [:div.field
        [:label "Where did this come from?"]
        [:select {:name "source"}
         (for [[val label] [["email" "Email"] ["linkedin-dm" "LinkedIn DM"]
                            ["other" "Somewhere else"]]]
           [:option (cond-> {:key val :value val}
                      (= val (v :source)) (assoc :selected true))
            label])]]]

      [:button.ui.primary.button {:type "submit"} "Capture it"]
      [:div.field-hint {:style "margin-top:0.6em;"}
       "It lands on the review board as Pending, flagged as captured on someone's "
       "behalf. No email is sent to the speaker — this is your note, not their "
       "submission."]])))

(defn replay-progress-bar
  "Pushed over SSE on every tick. Its own id so the rest of the page stays put."
  [event {:keys [status day days idx total submissions total-submissions reviews pct]}]
  [:div#replay-progress
   [:div.replay-progress
    [:div.replay-fill {:style (str "width:" (format "%.1f" (double (or pct 0))) "%;")}]]
   [:div.field-hint
    (case status
      :playing "▶ playing · "
      :paused "⏸ paused · "
      :done "finished · "
      "")
    "day " day " of " days
    " · " submissions "/" total-submissions " submissions"
    " · " reviews " review events"
    " · " idx "/" total " total"]])

(defn replay-page
  [event {:keys [person progress corpus-available? speeds speed running?]}]
  (organizer-shell
   (str "Replay — " (:name event))
   {:event event :active :replay :person person :crumb "Replay" :sse? true
    :body-attrs (ds/sse-mount (:id event))}

   (header "Replay simulator"
           "Three weeks of a real CFP, compressed. Played through the actual mutations — nothing here is faked.")

   (if-not corpus-available?
     [:div.ui.warning.message
      [:div.header "The corpus isn't installed"]
      [:p "Drop the scripted corpus at " [:code "resources/replay/aie-corpus.json"]
       " and reload. Without it there is nothing to play — and rather than "
       "invent filler, this page says so."]]

     [:div.replay-bar
      (replay-progress-bar event progress)
      [:div.replay-controls {:style "margin-top:0.6em;"}
       (if running?
         [:form {:method "post" :action (str "/api/events/" (:slug event) "/replay/pause")}
          [:button.ui.button {:type "submit"} "⏸ Pause"]]
         [:form {:method "post" :action (str "/api/events/" (:slug event) "/replay/play")}
          [:select {:name "speed"}
           (for [sp speeds]
             [:option (cond-> {:key (:key sp) :value (:key sp)}
                        (= (:seconds sp) speed) (assoc :selected true))
              (:label sp)])]
          [:button.ui.primary.button {:type "submit"} "▶ Play"]])
       [:form {:method "post" :action (str "/api/events/" (:slug event) "/replay/skip")}
        [:button.ui.button {:type "submit"} "Skip to end"]]
       [:a.ui.basic.button {:href (str "/events/" (:slug event) "/board")}
        "Watch the board"]
       [:a.ui.basic.button {:href (str "/events/" (:slug event) "/log")} "Watch the log"]]])

   [:div.ui.info.message {:style "margin-top:1.2em;"}
    [:p "Open the "
     [:a {:href (str "/events/" (:slug event) "/board")} "review board"]
     " in another tab while this plays. Rows appear, ratings land, coverage climbs — "
     "live, over the same SSE the app already uses. Afterwards, drag the slider on "
     "the board or the log to replay it at your own pace."]
    [:p.field-hint
     "Every entry goes through the same functions a real submission does. If the "
     "replay worked, the app works."]]))

;; --- 3f2. Comms -------------------------------------------------------------

(defn comms-page
  "Every letter this event has produced, and the templates that produce them.
   The question this page answers is Ann's: 'did the letter actually go out?'"
  [event {:keys [person history mail-configured? mail-status templates]}]
  (organizer-shell
   (str "Comms — " (:name event))
   {:event event :active :comms :person person :crumb "Comms"}
   (header "Comms" mail-status)

   (when-not mail-configured?
     [:div.ui.warning.message
      [:div.header "Nothing is being emailed"]
      [:p "Add " [:code "secrets/smtp.edn"] " (" [:code "{:host :port :user :pass :from}"]
       ") or set " [:code "SMTP_HOST"] " and friends, then restart. Until then every "
       "letter is rendered and recorded below, exactly as it would have been sent, "
       "so you can copy one out and send it by hand."]])

   [:div.ui.segment
    [:h4.ui.header "Send history (" (count history) ")"]
    (if (empty? history)
      [:div.empty-state "Nothing yet. Letters appear here when you inform speakers "
       "or when a submission arrives."]
        ;; A real table with fixed columns — when · to · subject · status. The
        ;; previous flowing layout meant no two rows lined up, which is exactly
        ;; the thing that makes a send log unreadable at a glance.
      [:table.ui.celled.table.comms-table
       [:thead
        [:tr [:th.c-when "When"] [:th.c-to "To"] [:th "Subject"] [:th.c-status "Status"]]]
       [:tbody
        (for [h history]
          [:tr
           [:td.c-when (or (fmt-when (:when h) (:tz event)) (:when h))]
           [:td.c-to (:to h)]
           [:td [:div (:subject h)]
            [:div.sub-meta
             (when (:kind h) (:kind h))
             (when (:has-ics? h) " · calendar invite attached")]
            (when (:error h) [:div.b-split (:error h)])]
           [:td.c-status
            [:span.ui.mini.label
             {:class (case (:type h)
                       "comms.sent" "green"
                       "comms.failed" "red"
                       nil)}
             (case (:type h)
               "comms.sent" "sent"
               "comms.failed" "failed"
               "would send")]]])]])]

   [:div.ui.segment
    [:h4.ui.header "Letter templates"]
    [:p.field-hint
     "Merge fields are filled per speaker. Editing these in the app arrives "
     "later; for now they live in " [:code "resources/letters/"] "."]
    (for [[status body] templates]
      [:div {:key status :style "margin-bottom:1.2em;"}
       [:h5.ui.header status]
       [:div.letter [:div.letter-body body]]])]))

;; The speaker-facing LIVE LANE is defined once, in the Public CFP section
;; below, and used by both surfaces: the portal's talk-edit form and the public
;; submission form must show the same feedback, computed by the same server
;; code, or a speaker gets told "looks fine" here and refused there. Forward
;; declared rather than moved, so the definitions stay beside the page that
;; explains them.
(declare datastar-script cfp-note portal-draft-status)

;; --- 3g. Speaker portal -----------------------------------------------------
;;
;; Speaker-facing: single column, no organizer sidebar, nothing about anyone
;; else's talk. A speaker should never be able to tell how many other people
;; submitted, let alone what the committee said about them.

(defn- status-pill [visible-status]
  (let [cls (case visible-status
              "Accepted" "accepted"
              "Waitlisted" "waitlisted"
              "Declined" "declined"
              "review")]
    [:span.status-pill {:class cls} visible-status]))

(defn- task-row [submission-id t]
  [:li.task-row {:key (:key t) :class (when (:done? t) "done")}
   [:span.task-tick (if (:done? t) "✓" "☐")]
   [:span.task-label (:label t)]
   (if (= "url" (:task-type t))
     [:form.task-form {:method "post"
                       :action (str "/api/submissions/" submission-id "/task")}
      [:input {:type "hidden" :name "key" :value (:key t)}]
      [:input {:type "url" :name "value" :value (or (:value t) "")
               :placeholder "https://…"}]
      [:button.ui.mini.basic.button {:type "submit"} "Save"]]
     (when-not (:done? t)
       [:form.task-form {:method "post"
                         :action (str "/api/submissions/" submission-id "/task")}
        [:input {:type "hidden" :name "key" :value (:key t)}]
        [:button.ui.mini.basic.button {:type "submit"} "Mark done"]]))])

(defn- portal-submission [{:keys [submission event visible-status informed?
                                  editable? tasks progress]} editing-id]
  (let [sid (:id submission)]
    [:div.portal-sub {:key (str sid)}
     [:h3 (get-in submission [:answers :talk-title])]
     [:div.portal-event (:name event)
      (when-let [d (fmt-date-range (:starts-on event) (:ends-on event))] (str " · " d))]

     (status-pill visible-status)
     (when-not informed?
       [:span.field-hint {:style "margin-left:0.6em;"}
        "The committee will be in touch — you'll hear from us either way."])

     (when (and informed? (= "Accepted" visible-status))
       [:div.accepted-banner {:style "margin-top:0.8em;"}
        [:strong "You're in! 🎉"]
        [:div "We'd love to have this talk. A few small things below when you have a moment."]])

     (when (and informed? (= "Waitlisted" visible-status))
       [:div.ui.message {:style "margin-top:0.8em;"}
        [:strong "You're on the waitlist."]
        [:div "We had more talks we wanted than slots. If one opens we'll come straight to you."]])

     (when (and informed? (= "Declined" visible-status))
       [:div.ui.message {:style "margin-top:0.8em;"}
        [:div "We couldn't fit this one into the program this year. "
         "Thank you for offering it — we hope you'll submit again."]])

     (when (and informed? (= "Accepted" visible-status) (seq tasks))
       [:div {:style "margin-top:1em;"}
        [:h5.ui.header {:style "margin-bottom:0.2em;"}
         "Your checklist"
         [:span.field-hint {:style "margin-left:0.5em; font-weight:400;"}
          (:done progress) " of " (:total progress) " done"]]
        [:ul.task-list (for [t tasks] (task-row sid t))]
        [:div.field-hint "More task types (travel, release form, tech check) arrive "
         "with the onboarding slice."]])

     [:div {:style "margin-top:0.9em;"}
      (if (= editing-id sid)
        [:a.ui.mini.basic.button {:href "/portal"} "Cancel edit"]
        (when editable?
          [:a.ui.mini.basic.button {:href (str "/portal?edit=" sid)} "Edit this talk"]))]]))

(defn- edit-form [{:keys [submission]} errors values]
  (let [snapshot (:form-snapshot submission)
        answers (:answers submission)]
    [:div.portal-sub {:style "border-color:#2185d0;"}
     [:h3 "Editing: " (get-in submission [:answers :talk-title])]
     [:div.field-hint {:style "margin-bottom:0.8em;"}
      "You can edit right up to the event — being accepted doesn't lock your talk. "
      "These are the questions exactly as you answered them; if the organizers have "
      "since changed the form, your talk keeps the version you saw."]
     (when (:_ errors) [:div.ui.negative.message (str/join " " (:_ errors))])
     ;; Same live lane as the public page: novalidate (the server validates),
     ;; a debounced whole-form POST, and a hidden `dscope` so the server knows
     ;; WHICH open form these keystrokes belong to.
     [:form.ui.form {:method "post" :action (str "/api/submissions/" (:id submission) "/answers")
                     :novalidate "novalidate"
                     :data-star-on:input__debounce.300ms
                     "@post('/portal/draft', {contentType: 'form'})"}
      [:input {:type "hidden" :name "dscope" :value (:id submission)}]
      (portal-draft-status (:id submission) false)
      ;; Empty note landing pads. Nothing to say on first paint — these fill in
      ;; from the stream the moment the speaker types (CLAUDE.md #9: the target
      ;; must exist before the push).
      (for [f (submissions/session-fields snapshot)]
        (list (answer-input f
                            (merge (into {} (map (fn [[k v]]
                                                   [(keyword (str "answer-" (name k))) v]))
                                         answers)
                                   values)
                            errors)
              (cfp-note (keyword (str "answer-" (name (:id f)))) nil)))
      [:button.ui.primary.button {:type "submit"} "Save changes"]
      [:a.ui.basic.button {:href "/portal" :style "margin-left:0.5em;"} "Cancel"]]]))

(defn- profile-form [person errors values]
  (let [profile (:profile person)
        v #(or (get values %) (get profile %) "")]
    [:div.portal-sub
     [:h3 "Your profile"]
     [:div.field-hint {:style "margin-bottom:0.8em;"}
      "This is what gets printed and published. It also prefills your next "
      "submission, so you never type your bio twice."]
     [:form.ui.form {:method "post" :action "/api/profile"
                     :novalidate "novalidate"
                     :data-star-on:input__debounce.300ms
                     "@post('/portal/draft', {contentType: 'form'})"}
      [:input {:type "hidden" :name "dscope" :value "profile"}]
      (portal-draft-status "profile" false)
      [:div.field
       [:label "Name"]
       [:input {:type "text" :value (:name person) :disabled true}]
       [:div.field-hint "Ask the organizers if your name needs changing."]]
      (for [{:keys [key label type help]} portal/profile-fields]
        (list
         [:div.field {:key (name key) :class (when (get errors key) "error")}
          [:label label]
          (if (= "textarea" type)
            [:textarea {:name (name key) :rows 5} (v key)]
            [:input {:type type :name (name key) :value (v key)}])
          (when help [:div.field-hint help])
          (field-error errors key)]
         (cfp-note key nil)))
      [:button.ui.primary.button {:type "submit"} "Save profile"]]]))

(defn portal-page
  "`opts` = {:person :submissions [..] :editing-id :errors :values :message}"
  [{:keys [person submissions editing-id errors values message profile-errors
           profile-values]}]
  (page-shell
   "Your speaker portal"
     ;; The portal streams too — same mount-inside-the-page trick as the CFP, so
     ;; the shared page-shell (and the auth lane's login page) stays untouched.
   [:div#portal-live (ds/sse-mount-url "/portal/stream")
     ;; The 15-second heartbeat patches #sse-heartbeat. A streaming page without
     ;; this span logs PatchElementsNoTargetsFound to the console four times a
     ;; minute — noise that trains everyone to ignore the console.
    [:span#sse-heartbeat]]
   (datastar-script)
   [:div.ui.container {:style "max-width:720px; margin-top:2em;"}
    [:div.cfp-masthead
     [:h1.ui.header "Your speaker portal"]
     [:div.cfp-meta (:name person) " · " (:email person)
      [:form {:method "post" :action "/logout" :style "display:inline; margin-left:0.8em;"}
       [:button.ui.mini.basic.button {:type "submit"} "Log out"]]]]

    (when message [:div.toast message])

    (if (empty? submissions)
      [:div.ui.segment
       [:div.empty-state
        [:p [:strong "No submissions yet."]]
        [:p "When you submit a talk it will show up here, with its status and "
         "anything we need from you."]]]
      (list
       [:h4.ui.header {:key "h"} "Your submissions"]
       (for [s submissions]
         (if (= editing-id (:id (:submission s)))
           (edit-form s errors values)
           (portal-submission s editing-id)))))

    (profile-form person profile-errors profile-values)

    [:div.field-hint {:style "margin-top:2em;"}
     "Questions about your talk? Reply to any email from the organizers — "
     "it reaches a real person."]]))

;; --- 3e. Login --------------------------------------------------------------

(defn landing-page
  "The public front door (bd -7e1, Gene ratified 2026-08-10): Zen Paper hero
   + the hate/proud mirror + organizer/speaker duo cards + the Ledger tape as
   the trust section. Served signed-OUT only — signed-in / goes to /events.
   `live-cfp` is {:slug .. :name ..} for an open call chosen by the server,
   or nil (the speaker link hides rather than 404s). Everyone sees this page
   — signed-in or not (Gene, 2026-08-10: / IS the landing, no exceptions);
   `person` only changes the top-right door."
  [live-cfp person]
  (page-shell
   "Curtain Call — calls for papers, without the paperwork"
   [:div.landing
    [:div.landing-top
     [:span.landing-brand "Curtain Call"]
     (if person
       [:a.landing-signin {:href "/events"} "Your events →"]
       [:a.landing-signin {:href "/login"} "Sign in →"])]
    [:div.kicker "Curtain Call · calls for papers, without the paperwork"]
    [:h1 "The CFP tool organizers have dreamed of for fifteen years." [:br]
     [:b "We were the ones complaining."]]
    [:p.confess
     "24 conferences over 12 years, on five of some of the worst tools on the "
     "planet. We know exactly what conference organizers hate — we have "
     "personally gnashed our teeth about these problems for over a decade."]
    [:div.mirror
     [:div.hate
      [:h3 "Everything we hate about CFP tools"]
      [:ul
       [:li "Reviewers can't actually talk to each other — opinions go to die in silos"]
       [:li "Committee members forced to log in to yet another tool just to leave a comment"]
       [:li "Form-filling, forever. It turns the best part of running a conference into a chore"]
       [:li "Deciding and notifying smeared into one terrifying button"]
       [:li "Your program, trapped — export means copy-paste"]
       [:li.ex "Exhibit A of our desperation: Trello boards, Zapier glue, one Google Sheet "
        "running the schedule for ten straight years, and an entire front-end we built "
        "by scraping our own CFP tool"]]]
     [:div.proud
      [:h3 "What we're proud of in this one"]
      [:ul
       [:li "Review is a conversation — every score and comment on one shared page"]
       [:li "Committee members click one link. Speakers never make an account at all"]
       [:li "Zero to open CFP in ten minutes — the acceptance test is a stopwatch"]
       [:li "Decide quietly, then tell everyone deliberately — and it remembers who's been told"]
       [:li "sessions.json, speakers.json, calendar.ics, a real API — your site drinks directly"]
       [:li "Append-only ledger: nothing is ever deleted, everything can be rewound"]]]]
    [:div.duo
     [:div.card.org
      [:div.aud "For organizers — you choose the tool"]
      [:h2 "Run the whole call on one calm page."]
      [:p "Built from 20,000 committee Slack messages' worth of scar tissue, "
       "after swyx dared the internet to kill his SaaS bill."]
      [:a {:href "/login?next=%2Fevents"} "Open your call for papers →"]]
     [:div.card.spk
      [:div.aud "For speakers — the people it must not lose"]
      [:h2 "The easiest submission you'll ever make."]
      [:p "One page. No account until you press submit. A half-typed abstract "
       "that survives anything. Edit until the call closes."]
      (when live-cfp
        [:a {:href (str "/cfp/" (:slug live-cfp))}
         "See a live call — " (:name live-cfp) " →"])]]
    [:a.cta {:href "/login?next=%2Fevents%2Fnew"} "Create your event — live in ten minutes"]
    [:span.quiet "win or lose, the $10K prize goes to STEM charity"]
    [:div.landing-ledger
     [:div
      [:h2 "Fifteen years, five tools, one lesson: " [:span "never lose the work."]]
      [:p.sub
       "Curtain Call is an append-only ledger wearing a friendly face. The story "
       "of how it came to exist is best told the way the tool itself would record "
       "it — and everything on this tape really happened."]]
     [:div.tape
      [:div.h "HISTORY.LOG — THE CONFERENCE DESK, 2011→"]
      [:div "2016  eventpower.adopted   review=thumbs-tally"]
      [:div.dead "2017  eventpower.abandoned  → cvent.adopted"]
      [:div.dead "2017  committee.refers-to-speakers-by-sheets-row"]
      [:div.love "2018  busyconf.adopted      \"the beloved era\""]
      [:div.dead "2023  busyconf.died         cause=heroku-repricing"]
      [:div "2021  sessionize.adopted + scraper.written (self-defense)"]
      [:div "2014-24  workarounds.written  trello, zapier, the-Sheet"]
      [:div "2026-08-09  swyx.dares-internet  \"kill my SaaS\""]
      [:div "2026-08-09  slack.messages.reviewed  n=20,000"]
      [:div.hot "2026-08-10  curtaincall.created  — you are here"]
      [:div.foot "append-only · nothing above can be edited or deleted · that's the product"]]]]))

(defn login-page
  "Magic-link-lite. In dev the link is printed on the page, because pretending
   an email went out when no mailer exists would be a lie."
  [{:keys [message link sent-to next dev? prefill-email demo? google?]}]
  (page-shell
   "Sign in — CFP Scheduler Killer"
   [:div.ui.container {:style "max-width:520px; margin-top:3em;"}
    [:h1.ui.header "Sign in"]
    [:p.field-hint
     "Committee members only. There is no password and no account to create — "
     "if your email is on a committee, you get a link."]
    [:p.field-hint
     "Speakers never sign in: the "
     [:strong "call for speakers page is public"] "."]

    (when message
      [:div.ui.info.message
       [:p message]
       (when link
         [:div {:style "margin-top:0.8em;"}
          [:div.field-hint "Email sending arrives with the comms slice — your link:"]
          [:a.ui.primary.button {:href link :style "margin-top:0.4em; word-break:break-all;"}
           "Sign in as " (or sent-to "yourself")]
          [:div.field-hint {:style "margin-top:0.5em; font-family:monospace; font-size:0.75em;
                                    word-break:break-all;"}
           link]])])

     ;; Google proves who you are; the committee roster still decides whether
     ;; you get in. One link, zero JavaScript — the whole flow is redirects.
    (when google?
      [:div {:style "margin-top:1.5em;"}
       [:a.ui.primary.button {:href "/auth/google"} "Sign in with Google"]
       [:div.field-hint {:style "margin-top:0.4em;"}
        "Committee members only — your Google email must be on an event's roster."]])

     ;; Demo mode (bd -o42): a deployed demo has no SMTP, so a stranger — swyx,
     ;; a judge, the eval kit's pre-auth human — needs a door that doesn't
     ;; involve an inbox. One click per seeded role, and the page says plainly
     ;; what this is. Renders ONLY when the instance is flagged as a demo.
    (when demo?
      [:div.demo-signin
       [:div.demo-lead "This is a demo instance — sign in as one of the seeded people:"]
       (for [[role label] [["organizer" "Organizer (Gene, chair)"]
                           ["reviewer" "Reviewer (Ann)"]
                           ["speaker" "Speaker (Priya)"]]]
         [:form {:key role :method "post" :action (str "/api/demo-login?role=" role)
                 :style "display:inline-block; margin:0 0.5em 0.5em 0;"}
          [:button.ui.button {:type "submit"} label]])])

    [:form.ui.form {:method "post" :action "/api/login" :style "margin-top:1.5em;"}
     (when next [:input {:type "hidden" :name "next" :value next}])
     [:div.field
      [:label "Your email"]
      [:input {:type "email" :name "email" :placeholder "you@example.com"
               :value (or sent-to prefill-email "") :autofocus true}]]
     [:button.ui.primary.button {:type "submit"} "Send me a link"]]

    (when dev?
      [:div.field-hint {:style "margin-top:2em;"}
       "Dev mode: seeded committee members are genek@itrevolution.net, "
       "annp@itrevolution.net and alex@itrevolution.net."])]))

;; --- 3i. The form builder ---------------------------------------------------
;;
;; CRUD over the vector of field defs, and nothing else — the form IS data, so
;; this page is a list, four buttons and one add form. Everything is a plain
;; server-rendered POST: no drag-drop, no client rules engine, no JS at all.
;;
;; Three regions, and WHICH region is which matters:
;;   #form-edit-panel  — outside the pushed area, so a colleague's edit landing
;;                       over SSE can never wipe what you are typing.
;;   #form-fields      — the list. Pushed PER VIEWER, because the two-step
;;                       retire prompt belongs to one person, not the room.
;;   #form-preview     — the public page's own renderer, pushed to everyone.

(defn- type-label [f]
  (let [t (forms/field-type f)]
    (or (:label (first (filter #(= t (:value %)) forms/editable-types)))
        (case t
          "group" "Repeatable block"
          "file" "File upload"
          t))))

(defn- fb-tags [f]
  (list
   (when (:required f) [:span.fb-tag.req {:key "r"} "REQUIRED"])
   (when (:private f) [:span.fb-tag.priv {:key "p"} "PC ONLY"])
   (when (forms/locked? f) [:span.fb-tag.lock {:key "l"} "LOCKED"])
   (when (forms/retired? f) [:span.fb-tag.gone {:key "x"} "RETIRED"])))

(defn- fb-post
  "One-button form POST. Every action on this page is one of these — a button
   that changes server state and forgets, which is the whole client contract."
  [action label params & [{:keys [class title]}]]
  [:form {:method "post" :action action}
   (for [[k v] params]
     [:input {:key (name k) :type "hidden" :name (name k) :value (str v)}])
   [:button.ui.mini.basic.button
    {:type "submit" :class class :title title} label]])

(defn- field-row
  "One field def as a line. `confirming` is the field-id this viewer is being
   asked about — server state, rendered server-side, so there is no confirm()
   anywhere near the SSE stream."
  [event f {:keys [index last-session? confirming]}]
  (let [slug (:slug event)
        id (forms/field-id f)
        group? (forms/group? f)
        retired? (forms/retired? f)
        base (str "/api/events/" slug "/form/")]
    (list
      ;; A question is a full-width label with its actions UNDERNEATH (Gene,
      ;; 2026-08-09: a long label like #6 must not fight the buttons for the
      ;; row). Two calm lines of content, then a quiet button strip. The label
      ;; anchors to its preview twin (#pv-<id>) — click a row, the right pane
      ;; scrolls to that field. Plain HTML, zero JS.
     [:div.fb-row {:key id :class (when retired? "retired")}
       ;; Lock sits to the LEFT of the number (Gene, 2026-08-09) — a locked
       ;; question reads as pinned before you even reach its number.
      [:div.fb-lock (when (forms/locked? f)
                      [:span {:title "Locked — the spine of the form"} "🔒"])]
      [:div.fb-ord (if (or group? retired?) "—" (inc index))]
      [:div.fb-body
       [:a.fb-label {:href (str "#pv-" id) :title "Show in the preview"}
        (:label f) (req-mark (:required f))
        (when (:private f) [:span.fb-tag.priv {:key "p" :title "Committee only"} "PC"])
        (when retired? [:span.fb-tag.gone {:key "x"} "RETIRED"])]
       [:span.fb-shape
        (if group?
           ;; Plain human sentence, not "Repeatable block · structural" (Gene).
          (str "There are " (count (:fields f)) " speaker profile questions")
          (list (type-label f)
                (when (seq (:options f)) (str " · " (count (:options f)) " options"))))]
       [:div.fb-acts
        (when-not (or group? retired?)
          (list
           (fb-post (str base "move") "↑" {:field-id id :direction "up"}
                    {:class (when (zero? index) "disabled") :title "Move up"})
           (fb-post (str base "move") "↓" {:field-id id :direction "down"}
                    {:class (when last-session? "disabled") :title "Move down"})))
        (when-not group?
          [:a.ui.mini.basic.button {:href (str "/events/" slug "/form?edit=" id)} "Edit"])
        (cond
          group? nil
          retired? (fb-post (str base "restore") "Restore" {:field-id id})
          (forms/locked? f) [:span.field-hint {:style "margin:0;"} "can't be removed"]
          :else (fb-post (str base "retire-ask") "Retire" {:field-id id}))]]]

      ;; Step two of the delete: server-rendered, never a modal dialog.
     (when (= confirming id)
       [:div.fb-confirm {:key (str "c-" id)}
        [:strong "Retire “" (:label f) "”?"]
        " It stops appearing on the public form. Nothing is erased — the field "
        "id " [:code.fb-id id] " is permanent, and every answer already given to "
        "it stays readable on its submission."
        [:div {:style "margin-top:0.5em;"}
         (fb-post (str base "retire") "Yes, retire it" {:field-id id})
         (fb-post (str base "retire-cancel") "Cancel" {})]]))))

(defn form-grid-region
  "Questions and their live preview INTERLEAVED into one CSS grid, so each
   question shares a grid ROW with its preview twin and the two can never drift
   out of vertical step (Gene, 2026-08-09: 'get q form and q preview
   valigned'). One pushable region (#fb-grid): a structural change or a live
   edit re-renders the whole grid. Cells are top-aligned, so a tall preview
   input (the Abstract textarea) makes the row tall and its builder row sits at
   the row's top, right beside it.

   `fields` may already carry an in-progress edit (the preview handler applies
   it before calling). `ghost` is the not-yet-added field from the add panel."
  [event {:keys [fields confirming ghost]}]
  (let [session (submissions/session-fields (forms/active-fields fields))
        last-live-id (forms/field-id (last session))
        group (first (filter forms/group? fields))
        retired (vec (filter forms/retired? fields))]
    [:div#fb-grid.fb-grid
     [:h4.ui.header.fb-col-head {:key "hq"} "Questions"]
     [:h4.ui.header.fb-col-head {:key "hp"} "What speakers see"]
     ;; Each session question: LEFT builder cell, then RIGHT preview cell —
     ;; grid auto-flow drops them onto the same row.
     (map-indexed
      (fn [i f]
        (list
         [:div.fb-gl {:key (str "l-" (forms/field-id f))}
          (field-row event f {:index i
                              :last-session? (= (forms/field-id f) last-live-id)
                              :confirming confirming})]
         [:div.fb-gr {:key (str "r-" (forms/field-id f))}
          [:span.pv-num (inc i)]
          [:div.pv-field.ui.form.fb-preview (answer-input f {} {})]]))
      session)
     ;; The add-preview ghost, if any, as a trailing right cell.
     (when ghost
       (list [:div.fb-gl {:key "gl"}]
             [:div.fb-gr {:key "gr"}
              [:div.fb-ghost.ui.form.fb-preview (answer-input ghost {} {})
               [:div.field-hint "Not added yet — appears when you press Add question."]]]))
     ;; The speaker block pairs with the "About you" section of the preview.
     (when group
       (list
        [:div.fb-gl {:key "spk-l"}
         (field-row event group {:index 0 :confirming confirming})]
        [:div.fb-gr {:key "spk-r"}
         [:div.ui.form.fb-preview
          [:div.cfp-section-title {:style "margin-top:0;"} "About you"]
          [:div.field-hint "Name, email, title, organization, bio, headshot & "
           "LinkedIn — filled in by each speaker (co-presenters welcome)."]]]))
     ;; Retired questions collapse full-width below the aligned pairs.
     (when (seq retired)
       [:details.fb-retired.fb-span {:key "retired"}
        [:summary (str "Show " (count retired) " retired question"
                       (when (not= 1 (count retired)) "s"))]
        (map-indexed
         (fn [i f] (field-row event f {:index i :confirming confirming}))
         retired)])]))

(defn form-fields-region
  "The field list. Pushed per viewer over SSE. Retired fields collapse into a
   <details> at the bottom so the working list is exactly what speakers see."
  [event {:keys [fields confirming]}]
  (let [session (vec (remove forms/group? fields))
        live (vec (remove forms/retired? session))
        last-live-id (forms/field-id (last live))
        active (vec (remove forms/retired? fields))
        retired (vec (filter forms/retired? fields))]
    [:div#form-fields
     (if (empty? fields)
       [:div.empty-state "This event has no form — that shouldn't happen."]
       (list
        (map-indexed
         (fn [i f]
           (field-row event f {:index i
                               :last-session? (= (forms/field-id f) last-live-id)
                               :confirming confirming}))
         active)
        (when (seq retired)
          [:details.fb-retired {:key "retired"}
           [:summary (str "Show " (count retired) " retired question"
                          (when (not= 1 (count retired)) "s"))]
           (map-indexed
            (fn [i f]
              (field-row event f {:index i :confirming confirming}))
            retired)])))]))

(defn form-preview-region
  "The public CFP page's OWN renderer, run over the live field defs. Not a
   mock-up of the form — literally `answer-input`, the function the speaker's
   browser gets, so a preview that looks right cannot be lying. `ghost` is the
   would-be field from the add panel, shown appended before it exists."
  ([event fields] (form-preview-region event fields nil))
  ([event fields {:keys [ghost]}]
   [:div#form-preview
    [:div.ui.form.fb-preview
     [:div.cfp-section-title {:style "margin-top:0;"} "Your talk"]
     ;; Numbered to MATCH the builder rows on the left (Gene, 2026-08-09): the
     ;; number is the correspondence key, so the two columns line up by eye
     ;; without any brittle height-sync. The number wraps answer-input rather
     ;; than living inside it — answer-input is the SAME fn the public page
     ;; uses, and speakers must never see question numbers.
     (map-indexed
      (fn [i f]
        [:div.pv-item {:key (str "pv-" i)}
         [:span.pv-num (inc i)]
         [:div.pv-field (answer-input f {} {})]])
      (submissions/session-fields (forms/active-fields fields)))
     (when ghost
       [:div.fb-ghost {:key "ghost"}
        (answer-input ghost {} {})
        [:div.field-hint "Not added yet — appears here when you press Add question."]])
     [:div.cfp-section-title "About you"]
     [:div.field-hint
      "The repeatable speaker block — name, email, title, organization, bio, "
      "headshot, LinkedIn — renders here on the real page."]]
    [:div.field-hint {:style "margin-top:0.7em;"}
     "This is the public page's renderer, not a mock-up. "
     [:a {:href (str "/cfp/" (:slug event)) :target "_blank" :rel "noopener"}
      "Open the real thing →"]]]))

(defn- field-form-fields
  "The shared body of the add and edit forms. `f` is the field being edited, or
   nil when adding."
  [f values errors & [sig]]
  (let [v #(or (get values %) (when f (get f %)) "")
        checked? #(if (contains? values %) (boolean (get values %)) (boolean (when f (get f %))))
        select? (= "select" (str (or (get values :type) (some-> f forms/field-type))))
        sig* (fn [suffix] (if sig (ds/bind (keyword (str sig suffix))) {}))]
    (list
     [:div.field {:key "label" :class (when (:label errors) "error")}
      [:label "Question" (req-mark true)]
      [:input (merge {:type "text" :name "label" :value (v :label)
                      :placeholder "What would you tell a peer CTO to do differently?"}
                     (sig* "label"))]
      (field-errors errors :label)]

     (if f
        ;; The type is fixed at birth: changing it under stored answers is how a
        ;; form builder corrupts its own history.
       [:div.field {:key "type"}
        [:label "Type"]
        [:div.field-hint (type-label f) " — a field's type is permanent, because "
         "answers are already stored in its shape. Retire it and add a new one "
         "if you need a different kind of answer."]]
       [:div.field {:key "type" :class (when (:type errors) "error")}
        [:label "Type" (req-mark true)]
        [:select (merge {:name "type"} (sig* "type"))
         (let [selected (str (or (get values :type) "text"))]
           (for [t forms/editable-types]
             [:option (cond-> {:key (:value t) :value (:value t)}
                        (= (:value t) selected) (assoc :selected true))
              (:label t)]))]
        (field-errors errors :type)])

     [:div.field {:key "help"}
      [:label "Help text " [:span.optional "(optional)"]]
      [:input (merge {:type "text" :name "help" :value (v :help)
                      :placeholder "Specific numbers beat adjectives."}
                     (sig* "help"))]]

      ;; No character-limit box. The answer's SHAPE (its type) says how long in
      ;; human words; invisible server-side caps guard against abuse (Gene,
      ;; 2026-08-09: "we are not DBAs"). Numbers stay out of the UI entirely.
     [:div.field {:key "limits"}
      [:div.field
       [:label "Flags"]
       [:div.ui.checkbox {:style "margin-right:1.2em;"}
        [:input (cond-> (merge {:type "checkbox" :name "required"} (sig* "req"))
                  (checked? :required) (assoc :checked true))]
        [:label "Required"]]
       [:div.ui.checkbox
        [:input (cond-> (merge {:type "checkbox" :name "private"} (sig* "priv"))
                  (checked? :private) (assoc :checked true))]
        [:label "Private (committee only)"]]
       [:div.field-hint "A private answer is collected from the speaker and "
        "never appears in any export, the public API or the agenda."]]]

      ;; Only meaningful for "Choose one". On an EDIT the type is already known,
      ;; so a text field never shows an options box it would silently ignore.
     (when (or (nil? f) select?)
       [:div.field {:key "options" :class (when (:options errors) "error")}
        [:label "Options " [:span.optional "(one per line — 'Choose one' only)"]]
        [:textarea (merge {:name "options" :rows 4
                           :placeholder "Experience Report\nSME talk\nPanel"}
                          (sig* "opts"))
         (let [o (or (get values :options) (when f (:options f)))]
           (if (sequential? o) (str/join "\n" o) (str (or o ""))))]
        (field-errors errors :options)
        [:div.ui.checkbox {:style "margin-top:0.5em;"}
         [:input (cond-> (merge {:type "checkbox" :name "widget" :value "radio"} (sig* "widget"))
                   (= "radio" (str (or (get values :widget)
                                       (some-> f :widget name))))
                   (assoc :checked true))]
         [:label "Show as radio buttons instead of a dropdown"]]]))))

(defn form-edit-panel
  "The open editor for ONE field as a MODAL (Gene, 2026-08-09) — you edit one
   question at a time, so it takes the foreground over a dimmed backdrop rather
   than pushing the list around. Still 100% server-rendered and server-owned
   (the `?edit=<id>` in the URL is the state); the backdrop is a real link that
   cancels, so there is no confirm()/JS anywhere near the SSE stream.

   Deliberately OUTSIDE #form-fields: an SSE push must never land on half-typed
   text."
  [event f {:keys [errors values]}]
  (when f
    (let [cancel-href (str "/events/" (:slug event) "/form")]
      [:div.fb-modal-backdrop
       ;; Click the dimmed area = cancel. A plain anchor, no listener.
       [:a.fb-modal-scrim {:href cancel-href :aria-label "Cancel"}]
       [:div#form-edit-panel.fb-modal.ui.segment.fb-card
        [:div.fb-modal-head
         [:h4.ui.header {:style "margin:0;"} "Edit question"
          [:span.fb-meta {:style "margin-left:0.6em; font-weight:400;"}
           [:code.fb-id (forms/field-id f)] " — permanent"]]
         [:a.fb-modal-x {:href cancel-href :title "Close" :aria-label "Close"} "×"]]
        [:form.ui.form {:method "post" :action (str "/api/events/" (:slug event) "/form/update")
                        :data-star-on:input__debounce.300ms
                        (str "@post('/api/events/" (:slug event) "/form/preview?field-id="
                             (forms/field-id f) "')")
                        ;; Cmd-S saves the open question — same idiom as the
                        ;; details page (ds/on-meta, browser-owned submit).
                        :data-star-on:keydown__window (ds/on-meta "s" "el.requestSubmit()")}
         [:input {:type "hidden" :name "field-id" :value (forms/field-id f)}]
         (field-form-fields f values errors "fbe")
         [:div.fb-modal-foot
          [:button.ui.small.primary.button {:type "submit"} "Save changes"]
          [:a.ui.small.basic.button {:href cancel-href} "Cancel"]]]]])))

(defn- finish-cfp-bar
  "The wizard's forward act — the same green button top and bottom of the form
   page (Gene, 2026-08-09), always visible whether or not the form was already
   marked reviewed. Not-yet-reviewed → a POST that records the review and lands
   on step 3; already reviewed → a plain link to step 3. `pos` is :top or
   :bottom, only for a class hook."
  [event reviewed? pos]
  [:div.fb-next {:class (name pos)}
   (if reviewed?
     [:a.btn-go {:href (str "/events/" (:slug event) "/committee")}
      "Next: create the review committee →"]
     [:form {:method "post" :action (str "/api/events/" (:slug event) "/form/reviewed")}
      [:button.btn-go {:type "submit"}
       "The form looks right — create the review committee →"]])])

(defn form-builder-page
  "`opts` = {:fields [..] :person p :editing field-def :confirming field-id
             :add-form {:values .. :errors ..} :edit-form {:values .. :errors ..}
             :reviewed? bool :submission-count n}"
  [event {:keys [fields person editing confirming add-form edit-form reviewed?
                 submission-count saved-toast]
          :as opts}]
  (organizer-shell
   (str "Form — " (:name event))
   {:event event :active :form :person person :crumb "Form" :sse? true
    :body-attrs (ds/sse-mount (:id event))}

   (when saved-toast [:div.toast {:key "toast"} saved-toast])

   (header "Create CFP Form"
           (let [active (submissions/session-fields (forms/active-fields fields))
                 retired (forms/retired-fields fields)
                 private (filter :private active)]
             (str (count active) " active questions plus the speaker profile"
                  (when (pos? (count retired))
                    (str " · " (count retired) " retired"))
                  (when (pos? (count private))
                    (str " · " (count private) " committee-only"))))
            ;; The wizard's forward act sits ABOVE "View public page" in the
            ;; header stack (Gene, 2026-08-09); the same button repeats at the
            ;; bottom of the page via `finish-cfp-bar`.
           [:div.fb-header-acts {:key "acts"}
            (finish-cfp-bar event reviewed? :top)
            [:a.ui.basic.button {:href (str "/cfp/" (:slug event))
                                 :target "_blank" :rel "noopener"}
             "View public page"]])

    ;; The wizard banner: this page IS step 2, and it says what the act is.
   (when-not reviewed?
     [:div.step-banner
      [:strong "Step 2 of 3."] " Your speakers will answer exactly these "
      "questions. The seed set is proven over 15 years of our CFPs — edit "
      "anything (the preview on the right is the real public page), or say "
      "the form looks right and move on. You can come back anytime, even after "
      "submissions arrive."])

   (when (pos? (or submission-count 0))
     [:div.ui.info.message
      [:div.header (str submission-count " submission"
                        (when (not= 1 submission-count) "s") " already exist"
                        (when (= 1 submission-count) "s"))]
      [:p "Editing the form now is safe: every submission carries its own "
       "snapshot of the questions it was answered against, so nothing you do "
       "here rewrites what a speaker already said."]])

   (form-edit-panel event editing edit-form)

    ;; The interleaved grid: each question and its live preview share a row and
    ;; stay vertically aligned (Gene, 2026-08-09). Column headers are the grid's
    ;; own top row, so they align too.
   (form-grid-region event opts)

   [:div.field-hint {:style "margin-top:0.8em;"}
    "Field ids are permanent — rename a label as often as you like, the id "
    "never moves, so answers keep their meaning. Removing is retiring: the "
    "question leaves the public form and its answers stay readable."]

   [:div.fb-add-block
    [:h4.ui.header "Add a question"]
    [:form.ui.form {:method "post" :action (str "/api/events/" (:slug event) "/form/add")
                    :data-star-on:input__debounce.300ms
                    (str "@post('/api/events/" (:slug event) "/form/preview?mode=add')")}
     (field-form-fields nil (:values add-form) (:errors add-form) "fba")
     [:button.ui.small.primary.button {:type "submit"} "Add question"]]]

   (finish-cfp-bar event reviewed? :bottom)))

;; --- 4. Public CFP page -----------------------------------------------------
;;
;; Rendered ENTIRELY from the form's field defs — add a field to the seed form
;; and it appears here, validated, with no view change. Everything is a plain
;; server-rendered form: no client validation, no client state.

(defn- datastar-script
  "The Datastar runtime. `page-shell` deliberately doesn't load it — only the
   organizer shell does — so the two speaker-facing pages that stream bring
   their own. A module script is deferred by definition, so it runs after the
   element carrying data-star-init has been parsed."
  []
  [:script {:type "module" :src (versioned "/vendor/datastar-aliased.js")}])

(defn cfp-note
  "The live, server-pushed line under one field.

   ALWAYS rendered, even when there is nothing to say, because a Datastar push
   needs a target that already exists (CLAUDE.md #9) — an empty note is an
   invisible landing pad. `note` is {:level :warn|:ok :text \"…\"} or nil."
  [param note]
  [:div.cfp-note {:id (str "cfp-note-" (name param))
                  :class (when note (name (:level note)))}
   (:text note)])

(defn cfp-draft-status
  "The one line that tells a speaker their typing is safe.

   This is the whole point of the draft stash made visible: without it the
   feature is invisible and nobody trusts the tab. Pushed on every debounced
   keystroke, so 'saved' is a fact about the last keystroke, not a promise."
  [{:keys [answered total]} saved?]
  [:div#cfp-draft-status.cfp-draft-status {:class (when saved? "saved")}
   (when (pos? (or total 0))
     [:span.cfp-progress answered " of " total " answered"])
   (when saved?
     [:span.cfp-saved "Saved. Close the tab if you like — this comes back."])])

(defn portal-draft-status
  "The portal's version of the same promise, one per open form. `scope` is
   \"profile\" or a submission id, so the two forms have separate targets and a
   keystroke in the bio never repaints the talk."
  [scope saved?]
  [:div.cfp-draft-status {:id (str "portal-status-" scope)
                          :class (when saved? "saved")}
   (when saved?
     [:span.cfp-saved "Saved as a draft — press the button below to make it real."])])

(defn- speaker-input
  "One ABOUT YOU input. `field-key` is the bare speaker attribute; errors for it
   are keyed :speaker-<field-key> so they can't collide with a form field."
  [{:keys [field-key label param input-type required help placeholder]} values errors]
  (let [k (keyword (str "speaker-" field-key))
        v (get values param "")]
    [:div.field {:key (str param) :class (when (get errors k) "error")}
     [:label label (req-mark required)]
     [:input {:type (or input-type "text") :name (str (symbol param)) :value v
              :placeholder placeholder
              :data-ghost-fill (when placeholder "")}]
     (when help [:div.field-hint help])
     (field-error errors k)]))

(def ^:private speaker-inputs
  [{:field-key "name"  :param :speaker-name  :label "Name" :required true
    :placeholder "Grace Hopper"}
   {:field-key "email" :param :speaker-email :label "Email" :input-type "email" :required true
    :help "How we reach you. Your speaker portal is tied to this address."
    :placeholder "you@company.com"}
   {:field-key "title" :param :speaker-title :label "Title / tagline" :required true
    :placeholder "SVP Engineering"}
   {:field-key "org"   :param :speaker-org   :label "Organization" :required true
    :placeholder "Acme Insurance"}])

(defn cfp-closed-notice
  "What a speaker sees when they can't submit. Both messages are warm and both
   name a fact — never a bare 'closed'.

   :not-open-yet covers two situations that read the same to a speaker: an
   organizer who created the event with the call deliberately shut, and (for
   older events) a call scheduled to open later. Only the second one can name a
   date, so only the second one does."
  [event state]
  (case state
    :not-open-yet
    (let [opens (fmt-instant (:cfp-opens-at event) (:tz event))
          later? (and (:cfp-opens-at event)
                      (.isAfter ^java.time.Instant (:cfp-opens-at event)
                                (java.time.Instant/now)))]
      [:div.ui.info.message
       [:div.header "The call for speakers isn't open yet"]
       [:p (if (and later? opens)
             (str "It opens " opens ". Check back then.")
             "Check back soon — the organizers haven't opened it yet.")]])
    :closed
    [:div.ui.warning.message
     [:div.header "The call for speakers has closed"]
     [:p "Submissions closed "
      (or (fmt-instant (:cfp-closes-at event) (:tz event)) "recently")
      ". Thank you to everyone who submitted."]]
    nil))

(def ^:private md-token
  "The three inline spellings md-lite understands: [text](url), **bold**,
   *emphasis*. Deliberately tiny — organizer copy, not documents."
  #"\[([^\]]+)\]\((https?://[^\s)]+)\)|\*\*([^*]+)\*\*|\*([^*]+)\*")

(defn- md-inline
  "One paragraph's inline markdown → hiccup children. Plain segments stay
   strings, so hiccup escapes them — pasted HTML renders as text, never runs."
  [s]
  (loop [s s out []]
    (if-let [m (re-find md-token s)]
      (let [whole (first m)
            idx (str/index-of s whole)
            [_ ltext lurl btext etext] m]
        (recur (subs s (+ idx (count whole)))
               (-> out
                   (conj (subs s 0 idx))
                   (conj (cond
                           ltext [:a {:href lurl :target "_blank"
                                      :rel "noopener"} ltext]
                           btext [:strong btext]
                           :else [:em etext])))))
      (conj out s))))

(defn md-lite
  "Markdown-ish plain text → hiccup: blank lines split paragraphs, plus the
   inline set in `md-token`. The fallback renderer for organizer copy."
  [s]
  (for [para (str/split (str s) #"\n\s*\n")
        :when (not (str/blank? para))]
    ;; seq, not vector — hiccup reads a vector as an ELEMENT (first item
    ;; becomes a tag), which both mangles the markup and skips escaping.
    [:p (seq (md-inline (str/trim para)))]))

(defn render-markdown
  "Organizer copy → hiccup. Uses markdown-clj (the renderer social-media-writer
   ships — server-side, never client) when it is on the classpath; until the
   JVM restarts with the new dep, `md-lite` covers the same copy. The source is
   entity-escaped FIRST either way: organizer text sells, it never scripts."
  [s]
  (let [escaped (-> (str s)
                    (str/replace "&" "&amp;")
                    (str/replace "<" "&lt;")
                    (str/replace ">" "&gt;"))]
    (if-let [md (try (requiring-resolve 'markdown.core/md-to-html-string)
                     (catch Throwable _ nil))]
      (h/raw (md escaped))
      (md-lite s))))

(defn cfp-about-you
  "The ABOUT YOU block, addressable as #cfp-about-you so the Sessionize import
   can morph it over SSE — filled fields appear in place, no page reload
   (Gene, 2026-08-09). Everything per-viewer: the import handler pushes only
   down the requesting viewer's stream, so two anonymous speakers on the same
   page never see each other's profile.

   The import button is a plain @post: the input is part of the MAIN form, so
   its value is already in the per-viewer draft stash by the time the button is
   pressed — the server reads its own state, nothing rides in the request."
  [event values errors notes import-message]
  (let [v #(get values % "")
        note #(cfp-note % (get notes %))
        slug (:slug event)
        import-post (str "@post('/api/cfp/" slug "/import-live')")]
    [:div#cfp-about-you
     [:div.import-box
      [:div.title "Have a Sessionize profile?"]
      [:div.ui.action.input.fluid
       ;; type=text, NOT url — the browser's native url validation would
       ;; reject a bare handle ("realgenekim") before the server sees it.
       ;; Enter imports rather than submitting the whole talk.
       [:input {:type "text" :name "speaker-sessionize-url"
                :value (v :speaker-sessionize-url)
                :placeholder "realgenekim"
                :data-star-on:keydown
                (str "if(evt.key==='Enter'){evt.preventDefault();"
                     import-post "}")}]
       [:button.ui.button {:type "button"
                           :data-star-on:click import-post}
        "Import"]]
      (note :speaker-sessionize-url)
      [:div.field-hint "Fills in your bio, photo and links below — you confirm "
       "before submitting. We never post anything or log in as you."]
      (when import-message
        [:div.ui.small.message {:style "margin-top:0.6em;"} import-message])]

     [:div.two.fields
      (speaker-input (nth speaker-inputs 0) values errors)
      (speaker-input (nth speaker-inputs 1) values errors)]
     [:div.two.fields
      (speaker-input (nth speaker-inputs 2) values errors)
      (speaker-input (nth speaker-inputs 3) values errors)]

     [:div.field {:class (when (:speaker-bio errors) "error")}
      [:label "Bio" (req-mark true)]
      [:textarea {:name "speaker-bio" :rows 5 :class "prose-deep"
                  :data-ghost-fill ""
                  :placeholder (str "Maria Chen leads platform engineering at "
                                    "Acme Insurance, where her team of 120 "
                                    "ships underwriting and claims systems. "
                                    "Her AI-assisted delivery program won "
                                    "Acme's 2025 Chairman's Award.")}
       (v :speaker-bio)]
      (field-error errors :speaker-bio)]

     [:div.two.fields
      [:div.field {:class (when (:speaker-headshot-url errors) "error")}
       [:label "Headshot URL " [:span.optional "(optional)"]]
       [:input {:type "url" :name "speaker-headshot-url" :value (v :speaker-headshot-url)}]
       (note :speaker-headshot-url)
       [:div.field-hint "Paste a link to a photo — imports fill this in for you."]
       (field-error errors :speaker-headshot-url)]
      [:div.field {:class (when (:speaker-linkedin-url errors) "error")}
       [:label "LinkedIn " [:span.optional "(optional)"]]
       [:input {:type "url" :name "speaker-linkedin" :value (v :speaker-linkedin)}]
       (note :speaker-linkedin)
       (field-error errors :speaker-linkedin-url)]]

     ;; Carry the pasted profile URL through a normal submit too, so it is
     ;; recorded on the submission even if they never pressed Import.
     [:input {:type "hidden" :name "speaker-sessionize-url-carry"
              :value (v :speaker-sessionize-url)}]]))

(defn cfp-page
  "The public submission page (docs/design/submission-page-wireframe.md).

   `opts` = {:state :open|:not-open-yet|:closed
             :form-fields [...] :values {..} :errors {..}
             :message <string> :import-message <string> :cap n
             :notes {param note} :progress {:answered n :total n}
             :restored? truthy-when-a-draft-was-found}"
  [event {:keys [state form-fields values errors message import-message cap
                 notes progress restored?]}]
  (let [v #(get values % "")
        open? (= :open state)
        note #(cfp-note % (get notes %))
        slug (:slug event)]
    (page-shell
      ;; `events/display-name` is the ONE place that knows how an event is
      ;; spelled — same string the organizer watched write itself on the create
      ;; page, same string on the agenda, same string in the tab title.
     (str (events/display-name event) " — Call for Speakers")

      ;; The live lane. The stream is opened by an element inside the page rather
      ;; than on <body>, because page-shell takes no body attrs — data-star-init
      ;; works anywhere, and this keeps the shared shell (and the auth lane's
      ;; login page) untouched.
     (when open? [:div#cfp-live (ds/sse-mount-url (str "/api/cfp/" slug "/stream"))
                  [:span#sse-heartbeat]])
     (when open? (datastar-script))

      ;; The hero SELLS the event with every fact we hold (Gene, 2026-08-09):
      ;; kicker, the full marquee (name — city · dates), the organizer's pitch
      ;; rendered as markdown, then the practical facts in one quiet line.
     [:div.cfp-masthead
      [:div.cfp-kicker "Call for Speakers"]
       ;; Plain title — the highlighter mark read as "a weird yellow bar" out
       ;; here (Gene, 2026-08-09); the gesture belongs on the create page only.
      [:h1.masthead-title (events/display-name event)]
      (when-let [intro (not-blank (:cfp-intro event))]
        [:div.cfp-intro (render-markdown intro)])
      [:div.cfp-meta
       (when (and (= :open state) (:cfp-closes-at event))
         (when-let [c (fmt-instant (:cfp-closes-at event) (:tz event))]
           [:span [:strong "Submissions close " c]]))
       (when cap
         (list [:span.sep "·"] "up to " cap " talk"
               (when (not= 1 cap) "s") " per person"))
       (when (:website-url event)
         (list [:span.sep "·"]
               [:a {:href (:website-url event) :target "_blank" :rel "noopener"}
                "Official event website ↗"]))]]

     (cfp-closed-notice event state)

     (when message [:div.ui.negative.message message])

     (when (and open? restored?)
       [:div.cfp-restored
        "Picked up where you left off — everything you typed on this device is "
        "still here."])

     (when open?
        ;; Every debounced keystroke posts the WHOLE form to the draft endpoint.
        ;; contentType 'form' (this Datastar build serializes the enclosing form
        ;; as urlencoded) means the draft params are spelled exactly like a real
        ;; submit — one vocabulary for the stash, the prefill and the 422.
        ;;
        ;; :novalidate is REQUIRED, not cosmetic: Datastar's form path calls
        ;; checkValidity() and would pop reportValidity() on every keystroke into
        ;; a half-typed type="url" field. Validation is the server's job here
        ;; anyway — it is the same code the 422 uses, pushed down the stream.
       [:form.ui.form {:id (str "cfp-form-" slug)
                       :method "post" :action (str "/api/cfp/" slug "/submit")
                       :novalidate "novalidate"
                       :data-star-on:input__debounce.300ms
                       (str "@post('/api/cfp/" slug "/draft', {contentType: 'form'})")}
        [:div.cfp-section-title "Your talk"]
        (cfp-draft-status progress false)
        (for [f (submissions/session-fields form-fields)]
          (list (answer-input f values errors)
                (note (keyword (str "answer-" (name (:id f)))))))

        [:div.cfp-section-title "About you"]

         ;; The import box + speaker fields live in an addressable block the
         ;; Sessionize import morphs over SSE — see `cfp-about-you`.
        (cfp-about-you event values errors notes import-message)

         ;; No disabled ghost buttons and no roadmap talk at the moment of
         ;; maximum trust (Gene, 2026-08-09) — one green act, one promise we
         ;; keep on the very next page.
        [:div {:style "margin-top:1.4em;"}
         [:button.btn-go {:type "submit"} "Submit talk"]
         [:div.field-hint {:style "margin-top:0.6em;"}
          "You'll land on a confirmation page with a link to your speaker "
          "portal, where you can edit this submission anytime."]]])

     [:div {:style "margin-top:3em; color:#aaa; font-size:0.85em;"}
      (when (:support-email event)
        (list "Questions? "
              [:a {:href (str "mailto:" (:support-email event))} (:support-email event)]))])))

(defn cfp-success-page
  "Honest confirmation: what we DID do, and what isn't wired up yet."
  [event submission]
  (page-shell
   (str "Submitted — " (:name event))
   [:div.cfp-masthead
    [:h1.ui.header "Thanks — your talk is in."]]
   [:div.ui.success.message
    [:div.header (get-in submission [:answers :talk-title])]
    [:p "Submitted to " (:name event) "."]]
   [:div.ui.segment
    [:h4.ui.header "What happens next"]
    [:ul
     [:li "The Program Committee reads every submission — real people, not a filter."]
     [:li "You can submit another talk from the "
      [:a {:href (str "/cfp/" (:slug event))} "call for speakers page"] "."]]
    [:div.field-hint {:style "margin-top:1em;"}
     "Not yet wired up: the confirmation email and your speaker portal link. "
     "They arrive in the comms slice — for now, keep this page or email "
     (or (:support-email event) "the organizers") " if you need to change anything."]]))

;; --- 5. Person detail -------------------------------------------------------

(defn- profile-links
  "Render whatever links a profile happens to carry. Empty until the
   Sessionize import and the speaker portal start filling profiles in."
  [links]
  (when (seq links)
    [:div {:style "margin-top:0.6em;"}
     (for [{:keys [label url]} links]
       [:a.ui.mini.basic.button {:key url :href url :target "_blank" :rel "noopener"}
        (or (not-blank label) url)])]))

(defn person-page
  "One person, seen through the lens of one event: who they are, what profile we
   hold, and which of this event's committees they sit on.

   The reviews/comments sections are deliberately present and deliberately
   empty — an honest 'not yet' beats a section that quietly doesn't exist."
  [event {:keys [person memberships chair? review-summary viewer]}]
  (let [{:keys [name email profile created-at]} person
        {:keys [headshot-url tagline bio location links]} profile]
    (organizer-shell
     (str name " — " (:name event))
     {:event event :active :committee :crumb "Person"}
     (header name
             (or (not-blank tagline) email)
             [:a.ui.basic.button {:href (str "/events/" (:slug event))}
              "Back to " (:name event)])

     [:div.ui.stackable.two.column.grid
      [:div.column
       [:div.ui.segment
        [:h4.ui.header "Person"
         (when chair? [:span.ui.mini.blue.label {:style "margin-left:0.5em;"} "chair"])]
        [:img.ui.small.rounded.image {:src (or (not-blank headshot-url) (pool-face (:id person)))
                                      :alt name
                                      :style "margin-bottom:1em; max-width:160px; border-radius:10px;"}]
        [:dl.facts
         [:dt "Email"] [:dd [:a {:href (str "mailto:" email)} email]]
         (when (not-blank tagline) (list [:dt "Tagline"] [:dd tagline]))
         (when (not-blank location) (list [:dt "Location"] [:dd location]))
         [:dt "Known since"]
         [:dd (or (fmt-when created-at (:tz event))
                  [:span.field-hint "unknown"])]]
        (when (not-blank bio)
          [:div [:h5.ui.header "Bio"] [:p {:style "white-space:pre-wrap;"} bio]])
        (profile-links links)
        (when-not (or (not-blank bio) (not-blank tagline) (not-blank headshot-url))
          [:div.field-hint
           "No profile details yet — these fill in when they import a Sessionize "
           "profile or submit a talk."])]]

      [:div.column
       [:div.ui.segment
        [:h4.ui.header "Committees on this event"]
        (if (seq memberships)
          [:div.member-list
           (for [m memberships]
             [:div.member-row {:key (str (:membership-id m))}
              [:div.member-who
               [:span.member-name (:committee-name m)]
               (when (= "chair" (:role m)) [:span.ui.mini.blue.label "chair"])
               [:div.member-email
                "member since " (or (fmt-when (:created-at m) (:tz event)) "—")]]])]
          [:p.field-hint "Not on any committee for this event."])]

       [:div.ui.segment
        [:h4.ui.header "Their reviews"
         (when (pos? (:rated-count review-summary))
           [:span.b-facts {:style "margin-left:0.6em; font-weight:400;"}
            (:rated-count review-summary) " of " (:total-submissions review-summary)])]
        (if (seq (:ratings review-summary))
          [:div
            ;; Their average against the COMMITTEE's average on the same talks.
            ;; A whole-event average would be arithmetic, not insight.
           [:div.field-hint {:style "margin-bottom:0.7em;"}
            "Their mean " [:strong (or (fmt-mean (:mean review-summary)) "—")]
            " · committee mean on the same talks "
            [:strong (or (fmt-mean (:committee-mean review-summary)) "—")]
            (when-let [d (when (and (:mean review-summary) (:committee-mean review-summary))
                           (- (:mean review-summary) (:committee-mean review-summary)))]
              (cond
                (>= d 0.5) " — rates higher than the room"
                (<= d -0.5) " — rates lower than the room"
                :else " — in line with the room"))]
           (for [r (:ratings review-summary)]
             [:div.member-row {:key (str (:submission-id r))}
              [:div.member-who
               [:a {:href (str "/events/" (:slug event) "/submissions/" (:submission-id r))}
                (:title r)]
               [:div.member-email
                "committee mean " (or (fmt-mean (:committee-mean r)) "—")]]
              [:div.op-stars {:style "font-weight:700;"} "★" (fmt-stars (:stars r))]])]
          [:div.empty-state "They haven't rated anything on this event yet."])]

       [:div.ui.segment
        [:h4.ui.header "Their comments"]
        (if (seq (:comments review-summary))
          (for [c (:comments review-summary)]
            [:div.sub-row {:key (str (:id c))}
             [:div.sub-title
              [:a {:href (str "/events/" (:slug event) "/submissions/" (:submission-id c))}
               (:title c)]]
             [:div {:style "margin-top:0.2em;"} (:body c)]])
          [:div.empty-state "No comments on this event yet."])]]])))

;; --- Event details (the step-1 page you can COME BACK to) --------------------

(defn event-details-page
  "Every fact from Create event, revisitable (Gene, 2026-08-09: 'it shouldn't
   be a subset'). Each fact is either editable or VISIBLY locked with its
   reason: the slug never changes (permalinks + calendar UIDs), and the call's
   open/close is a deliberate act with its own controls on the dashboard. The
   pitch is the headline: it renders atop the public CFP masthead."
  [event {:keys [person notice errors values]}]
  (let [v (fn [k] (str (or (get values k) (get event k) "")))
        err (fn [k] (when-let [e (get errors k)]
                      [:div.field-hint.slug-bad e]))]
    (organizer-shell
     (str "Event details — " (:name event))
     {:event event :active :details :person person :crumb "Event details"}
     (header "Event details"
             "Everything you said at create, revisitable — editable anytime.")
     (when notice
        ;; The save toast — THE standard save confirmation for every edit form
        ;; (Gene, 2026-08-09): server-rendered on the post-save reload,
        ;; CSS-only lifecycle. Reuse .toast wherever a form 303s with ?saved=1.
       [:div.toast {:key "saved"} notice])
     [:div.ui.segment.fb-card
      [:form.ui.form {:method "post"
                      :action (str "/api/events/" (:slug event) "/details")
                       ;; Cmd-S / Ctrl-S saves (Gene, 2026-08-09) — the ds/on-meta
                       ;; house idiom; `el` is this form, requestSubmit is the
                       ;; same browser-owned submit the button does.
                      :data-star-on:keydown__window (ds/on-meta "s" "el.requestSubmit()")}
       [:div.field
        [:label "Sell the conference "
         [:span.optional "(shown at the top of the public CFP page)"]]
        [:textarea {:name "cfp-intro" :rows 6 :class "prose-deep"
                    :placeholder (str "Two days with the leaders actually rewiring "
                                      "their enterprises with AI — real numbers, "
                                      "real scars, no vendor decks.")}
         (v :cfp-intro)]]
       [:div.field {:class (when (:name errors) "error")}
        [:label "Event name" (req-mark true)]
        [:input {:type "text" :name "name" :value (v :name)}]
        (err :name)]
       [:div.two.fields
        [:div.field {:class (when (:starts-on errors) "error")}
         [:label "Starts"]
         [:input {:type "date" :name "starts-on" :value (v :starts-on)}]
         (err :starts-on)]
        [:div.field {:class (when (:ends-on errors) "error")}
         [:label "Ends"]
         [:input {:type "date" :name "ends-on" :value (v :ends-on)}]
         (err :ends-on)]]
       [:div.two.fields
        [:div.field
         [:label "Location"]
         [:input {:type "text" :name "location" :value (v :location)}]]
        [:div.field {:class (when (:tz errors) "error")}
         [:label "Time zone"]
         [:select {:name "tz"}
          (for [z events/common-timezones]
            [:option (cond-> {:key z :value z}
                       (= z (or (get values :tz) (:tz event))) (assoc :selected true))
             z])]
         (err :tz)
         [:div.field-hint "Changing the zone reinterprets every stored "
          "wall-clock time — fine before submissions, careful after."]]]
       [:div.two.fields
        [:div.field
         [:label "Event website"]
         [:input {:type "url" :name "website-url" :value (v :website-url)}]]
        [:div.field
         [:label "Speaker support email"]
         [:input {:type "email" :name "support-email" :value (v :support-email)}]]]
       [:div.field
        [:label "Public address " [:span.optional "(permanent)"]]
        [:div.field-hint [:span.cfp-url "/cfp/" (:slug event)]
         " — permalinks and calendar UIDs are woven into it, so it never changes."]]
       [:button.ui.small.primary.button {:type "submit"} "Save details"]
       [:a.ui.small.basic.button {:href (str "/cfp/" (:slug event))
                                  :target "_blank" :rel "noopener"}
        "See the public page →"]]]
     [:div.field-hint
      "The call's open/close controls live on the "
      [:a {:href (str "/events/" (:slug event))} "dashboard"] "."])))

;; --- 3. Event dashboard -----------------------------------------------------

(defn- dash-days-left
  "Days until the call closes — honest when there is no close date (the
   '26441 days left' class of nonsense, bead 5nr, dies here)."
  [event cfp-state]
  (let [closes (:cfp-closes-at event)]
    (cond
      (= :closed cfp-state) {:n "—" :hint "call closed"}
      (nil? closes) {:n "—" :hint "no close date set"}
      :else
      (let [d (.between java.time.temporal.ChronoUnit/DAYS
                        (-> (cfp-scheduler-killer.store/now-inst)
                            (.atZone (java.time.ZoneId/of "UTC"))
                            (.toLocalDate))
                        (java.time.LocalDate/parse (subs (str closes) 0 10)))]
        (cond
          (neg? d) {:n "0" :hint "past the close date"}
          ;; a demo/far-future close date is "no deadline", not "26,441 days"
          (> d 365) {:n "—" :hint "no deadline pressure"}
          :else {:n (str d) :hint (str "closes " (subs (str closes) 0 10))})))))

(defn- dash-recent-line
  "One log fact as one human sentence — nil for facts nobody scans for."
  [person-name-of e]
  (let [p (:payload e)
        who #(or (person-name-of (:person-id p)) "Someone")]
    (case (:type e)
      "submission.created" "New submission"
      "rating.set" (str (who) " rated ★" (:stars p))
      "comment.added" (str (who) " commented")
      "submission.status-changed" (str "Status changed → " (or (:to p) (:status p)))
      "submission.answers-updated" "A speaker revised their submission"
      "committee.member-added" (str (or (:name p) (:email p) "A reviewer")
                                    " joined the committee")
      "event.cfp-opened" "The call for speakers opened"
      "event.cfp-closed" "The call for speakers closed"
      nil)))

(defn- dash-feed-talk
  "WHAT the action landed on — the review-board row's identity, compact:
   the speaker's face, the talk title (a door to the submission), and
   name · org (Gene, 2026-08-10: 'we need to know what they commented on')."
  [slug submission-of e]
  (let [p (:payload e)
        sid (or (:submission-id p) (:id p))
        s (when (and sid submission-of) (submission-of sid))]
    (when s
      (let [sp (first (:speakers s))
            title (get-in s [:answers :talk-title])]
        [:span.dash-feed-talk
         [:img.dash-feed-face
          {:src (or (not-blank (:headshot-url sp))
                    (pool-face (or (:person-id sp) (:name sp))))
           :alt (or (:name sp) "")}]
         [:a.dash-feed-title {:href (str "/events/" slug "/submissions/" (:id s))}
          (str "“" title "”")]
         [:span.dash-feed-who
          (str (:name sp) (when (not-blank (:org sp)) (str " · " (:org sp))))]]))))

(defn- dash-rel-time
  "2m / 3h / 4d ago, from whatever timestamp the fact carries."
  [e]
  (when-let [ts (or (:at e) (:created-at e) (get-in e [:payload :at]))]
    (try
      (let [then (java.time.Instant/parse (str ts))
            mins (.toMinutes (java.time.Duration/between then (java.time.Instant/now)))]
        (cond (< mins 1) "now"
              (< mins 60) (str mins "m")
              (< mins 1440) (str (quot mins 60) "h")
              :else (str (quot mins 1440) "d")))
      (catch Exception _ nil))))

(defn event-dashboard-region
  "The replaceable facts inside Mission Control. The DEV strip and its live
   scrubber deliberately live outside this region so a patch cannot interrupt
   the slider gesture."
  [host event {:keys [members committee-count
                      sub-count speaker-count cfp-state
                      alerts uncommunicated form-reviewed? form-field-count
                      enriched coverage recent person-name-of submission-of]}]
  (let [{:keys [slug tz]} event
        n-subs (or sub-count 0)
        reviewed-pct (if (pos? (or (:total coverage) 0))
                       (int (* 100 (/ (:covered coverage) (:total coverage))))
                       0)
        needs-2nd (count (filter #(< (:n %) (or (:target coverage) 2)) enriched))
        days (dash-days-left event cfp-state)
        board-url (str "/events/" slug "/board")
        setup-incomplete? (or (not= :open cfp-state) (zero? n-subs))]
    [:div#dashboard-region
     ;; The launch strip stays: when the call is not open, the dashboard's
     ;; job is to walk you to launch — loudly.
     (when (= :not-open-yet cfp-state)
       [:div.launch-strip
        [:div.launch-lead "Your call for speakers isn't open yet. Three steps to launch:"]
        [:div.launch-steps
         [:a.launch-step {:href (str "/events/" slug "/form")}
          [:span.n "1"] "Review the form"
          [:span.launch-note (if form-reviewed? "reviewed ✓"
                                 (str (or form-field-count 11) " seed questions ready"))]]
         [:a.launch-step {:href (str "/events/" slug "/committee")}
          [:span.n "2"] "Add reviewers"
          [:span.launch-note (str (or committee-count 1) " on the roster")]]
         [:form.launch-step-form {:method "post"
                                  :action (str "/api/events/" slug "/cfp/open")}
          [:button.launch-step.go {:type "submit"}
           [:span.n "3"] "Open the call →"]]]])

     (inform-banner event (or uncommunicated 0))
     (alert-rows-partial alerts)

     ;; The stat tiles — the five-second answer to "how is my call doing?"
     [:div.dash-tiles
      [:div.dash-tile
       [:div.dash-n (str n-subs)]
       [:div.dash-l (str "submission" (when (not= 1 n-subs) "s"))]
       (when (pos? n-subs)
         [:div.dash-spark (submissions-sparkline event enriched coverage)])]
      [:div.dash-tile
       [:div.dash-n (:n days)]
       [:div.dash-l "days left"]
       [:div.dash-h (:hint days)]]
      [:div.dash-tile
       [:div.dash-n (str reviewed-pct "%")]
       [:div.dash-l "fully reviewed"]
       [:div.dash-h (str "≥" (or (:target coverage) 2) " reviews each")]]
      (let [n-ratings (reduce + (map :n enriched))
            n-comments (reduce + (map (comp count :comments) enriched))]
        [:div.dash-tile
         [:div.dash-n (str n-ratings)]
         [:div.dash-l (str "rating" (when (not= 1 n-ratings) "s"))]
         [:div.dash-h (str n-comments " comment" (when (not= 1 n-comments) "s"))]])]

     ;; The setup checklist, ONLY while it still has work to do.
     (when setup-incomplete?
       [:div.dash-card
        [:h4.ui.header "Get the call open"]
        [:ul.checklist
         (checklist-item true "Create event")
         (checklist-item (boolean form-reviewed?)
                         [:span "Create CFP form — review the "
                          (or form-field-count 11) " seed questions: "
                          [:a {:href (str "/events/" slug "/form")} "open the form editor"]])
         (checklist-item (boolean (seq members))
                         [:span "Create review committee — "
                          [:a {:href (str "/events/" slug "/committee")} "invite reviewers"]])
         (checklist-item (= :open cfp-state)
                         (case cfp-state
                           :open (str "Call for speakers is open"
                                      (when-let [o (fmt-instant (:cfp-opens-at event) tz)]
                                        (str " — since " o)))
                           :closed "Call for speakers is closed"
                           "Open the call for speakers"))
         (checklist-item (pos? n-subs) "First submission arrives")]
        [:div.field-hint {:style "margin-top:0.8em;"}
         "Every row tracks real state — nothing here is decorative."]])

     ;; What needs you — each row is a work queue with a door.
     [:div.dash-card
      [:h4.ui.header "What needs you"]
      (let [rows (remove nil?
                         [(when (zero? n-subs)
                            [:div.dash-need
                             [:span "Share the public link — this is the URL speakers need: "]
                             [:a.cfp-url {:href (str "/cfp/" slug) :target "_blank" :rel "noopener"}
                              (cfp-public-url host slug)]
                             [:button.copy-url {:type "button" :title "Copy the public CFP URL"
                                                :data-star-on:click
                                                (ds/copy-nearest-text "div" ".cfp-url"
                                                                      "Copied to clipboard")}
                              "⧉ copy"]])
                          (when (pos? needs-2nd)
                            [:a.dash-need {:href (str board-url "?sort=needs-reviews")}
                             (str needs-2nd " submission" (when (not= 1 needs-2nd) "s")
                                  " still below " (or (:target coverage) 2) " reviews")
                             [:span.dash-go "coverage queue →"]])
                          (when (pos? (or uncommunicated 0))
                            [:a.dash-need {:href (str "/events/" slug "/inform")}
                             (str uncommunicated " decision" (when (not= 1 uncommunicated) "s")
                                  " made but not yet told")
                             [:span.dash-go "inform speakers →"]])])]
        (if (seq rows)
          rows
          [:div.field-hint "The queues are clear — nothing is waiting on you."]))]

     ;; Recent — the call breathing.
     (when (seq recent)
       [:div.dash-card
        [:h4.ui.header "Recent"]
        ;; Basecamp shape (Gene, 2026-08-10: 'the winning ticket'): row one
        ;; is WHO did WHAT, row two — slightly indented — is what it landed
        ;; on: the talk, then its speaker · org.
        (let [lines (->> recent
                         (keep (fn [e]
                                 (when-let [line (dash-recent-line person-name-of e)]
                                   [:div.dash-recent-entry
                                    [:div.dash-recent-act
                                     [:span.dash-recent-what line]
                                     (when-let [t (dash-rel-time e)]
                                       [:span.dash-recent-when t])]
                                    (when-let [talk (dash-feed-talk slug submission-of e)]
                                      [:div.dash-recent-on talk])])))
                         (take 6)
                         seq)]
          (or lines [:div.field-hint "Nothing yet."]))
        [:div.field-hint {:style "margin-top:0.6em;"}
         [:a {:href (str "/events/" slug "/log")} "open the full log →"]]])

     ;; The call's open/close stays reachable — a deliberate act by a named
     ;; person, one quiet line instead of a whole card.
     ;; The public URL stays grabbable here always — organizers hand this
     ;; link out constantly, not only before the first submission.
     [:div.dash-call-line
      [:span (case cfp-state
               :open (str "The call is open"
                          (when-let [c (fmt-instant (:cfp-closes-at event) tz)]
                            (str " until " c)))
               :not-open-yet "The call is not open yet"
               :closed "The call is closed"
               "The call is open")]
      [:a.cfp-url {:href (str "/cfp/" slug) :target "_blank" :rel "noopener"}
       (cfp-public-url host slug)]
      [:button.copy-url {:type "button" :title "Copy the public CFP URL"
                         :data-star-on:click
                         (ds/copy-nearest-text "div" ".cfp-url" "Copied to clipboard")}
       "⧉ copy"]
      [:form.cfp-act {:method "post"
                      :action (str "/api/events/" slug "/cfp/"
                                   (if (= :open cfp-state) "close" "open"))}
       [:button.ui.tiny.basic.button {:type "submit"}
        (if (= :open cfp-state) "Close the call" "Open the call")]]]]))

(defn event-dashboard-page
  "Mission control (Gene ratified 2026-08-10, over ASCII mockups): the call
   at a glance — stat tiles with the momentum sparkline, a what-needs-you
   queue, and the recent activity feed. The setup checklist appears only
   while setup is incomplete, then vanishes. No duplication: Event details
   owns the metadata, Committee owns the roster, the board owns the list."
  [host event {:keys [person time-travel]
               :as opts}]
  (when-not (and (map? time-travel)
                 (string? (:base-path time-travel))
                 (string? (:fragment-path time-travel)))
    (throw (ex-info "event-dashboard-page requires a complete :time-travel contract"
                    {:required [:base-path :fragment-path]
                     :received time-travel})))
  (let [{:keys [name starts-on ends-on]} event]
    (organizer-shell
     (str name " — CFP Scheduler Killer")
     {:event event
      :active :dashboard
      :person person
      :time-travel time-travel
      :datastar? true}
     (header name
             (let [dates (or (fmt-date-range starts-on ends-on) "Dates not set yet")]
               (if (:location event) (str dates " · " (:location event)) dates))
             [:a.ui.primary.button {:href (str "/events/" (:slug event) "/board")}
              "Go to the review board →"])
     (event-dashboard-region host event opts))))
