(ns cfp-scheduler-killer.server
  "Web server with http-kit, reitit routing, and dev auto-reload.

   Handler convention (CLAUDE.md): every handler is a named `defn handle-*`
   referenced as `#'var` in the route table, so REPL redefinition takes effect
   without a restart."
  (:require
   [cfp-scheduler-killer.auth :as auth]
   [cfp-scheduler-killer.auth-google :as auth-google]
   [cfp-scheduler-killer.committees :as committees]
   [cfp-scheduler-killer.events :as events]
   [cfp-scheduler-killer.exports :as exports]
   [cfp-scheduler-killer.forms :as forms]
   [cfp-scheduler-killer.inform :as inform]
   [cfp-scheduler-killer.mail :as mail]
   [cfp-scheduler-killer.notices :as notices]
   [cfp-scheduler-killer.portal :as portal]
   [cfp-scheduler-killer.replay :as replay]
   [cfp-scheduler-killer.reviews :as reviews]
   [cfp-scheduler-killer.schedule :as schedule]
   [cfp-scheduler-killer.sessionize-import :as sessionize-import]
   ;; required for side effects: installs :pc-push / :airtable /
   ;; :gcs-snapshot into the store's sink registry
   [cfp-scheduler-killer.sinks :as sinks]
   [cfp-scheduler-killer.sse :as sse]
   [cfp-scheduler-killer.store :as store]
   [cfp-scheduler-killer.submissions :as submissions]
   [cfp-scheduler-killer.views :as views]
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [hiccup2.core :as h]
   [org.httpkit.server :as http]
   [reitit.ring :as ring]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.response :as resp]
   [starfederation.datastar.clojure.adapter.http-kit :as hk]
   [starfederation.datastar.clojure.api :as d*]
   [taoensso.timbre :as log])
  (:gen-class))

;; --- Server lifecycle ---

(defonce server (atom nil))

;; --- Helpers ---

(def ^:private config
  "resources/config.edn, read once. Missing file = all defaults."
  (delay (or (some-> (io/resource "config.edn") slurp edn/read-string)
             {})))

(defn demo-mode?
  "Is this instance flagged as a public demo? (bd -o42 — gates the one-click
   demo sign-in.) The env var WINS in both directions — DEMO_MODE=false must
   be able to turn a baked-in config default OFF, or a production service
   built from the same image would ship with impersonation buttons."
  []
  (case (System/getenv "DEMO_MODE")
    "true"  true
    "false" false
    (boolean (:demo-mode @config))))

(defn request-host
  "The host an organizer sees in their browser, used to print the public CFP
   URL. `:public-base-url` in resources/config.edn wins when set; otherwise
   derived from the request, honoring X-Forwarded-Proto/Host so it stays right
   behind Cloud Run."
  [req]
  (or (:public-base-url @config)
      (let [headers (:headers req)
            host (or (get headers "x-forwarded-host")
                     (get headers "host")
                     "localhost")
            scheme (or (get headers "x-forwarded-proto")
                       (name (or (:scheme req) :http)))]
        (str scheme "://" host))))

(defn- not-blank [s] (when-not (str/blank? s) s))

(defn html-response
  ([body] (html-response 200 body))
  ([status body]
   {:status status
    :headers {"Content-Type" "text/html; charset=utf-8"}
    :body (str body)}))

(defn see-other
  "303 — the correct redirect after a successful form POST."
  [location]
  {:status 303 :headers {"Location" location} :body ""})

;; --- Route handlers ---

(defn handle-home
  "The front door (bd -7e1): the landing page, for EVERYONE — signed-in
   people are not redirected away from their own homepage (Gene, 2026-08-10,
   second ruling; the first draft shunted them to /events and that was
   'terrible'). The speaker card's 'see a live call' link is chosen HERE —
   first non-archived event with an open call — so the view never guesses
   at a slug."
  [req]
  (let [live (->> (events/list-events)
                  (remove :archived-at)
                  (filter #(= :open (submissions/cfp-state %)))
                  first)]
    (html-response
     (views/landing-page (when live (select-keys live [:slug :name]))
                         (auth/current-person req)))))

(defn handle-events-list [req]
  (let [person (auth/current-person req)]
    ;; Your events, not everyone's — the tenancy boundary (Gene, 2026-08-09).
    ;; The gate already refuses cross-event ACTIONS; this stops the names of
    ;; other organizers' conferences leaking into the list and the sidebar.
    (html-response (views/events-list-page
                    (if person (events/events-for-person (:id person))
                        (events/list-events))
                    person))))

(defn- archive-target
  "Resolve the event a j/k/x keystroke acted on. The client posts the focused
   INDEX; the server maps it over ITS OWN active-list order — the same
   derivation the page rendered — so no server state ever rides in the POST."
  [req]
  (let [person (auth/current-person req)
        active (vec (remove :archived-at
                            (if person (events/events-for-person (:id person))
                                (events/list-events))))
        idx (try (Long/parseLong (str (get-in req [:params :idx])))
                 (catch Exception _ nil))]
    (when (and idx (< -1 idx (count active)))
      (nth active idx))))

(defn handle-event-archive
  "POST /api/events/archive {idx} — x on the events page. Appends a fact."
  [req]
  (when-let [e (archive-target req)]
    (events/archive-event! e (or (:email (auth/current-person req)) "organizer")))
  (see-other "/events"))

(defn handle-event-unarchive
  "POST /api/events/:slug/unarchive — Restore from the archived section."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (when-let [e (events/event-by-slug slug)]
      (events/unarchive-event! e (or (:email (auth/current-person req)) "organizer")))
    (see-other "/events")))

(defonce ^{:doc "Per-person create-page drafts: {person-id {field-str value}}.
  Fed by every debounced preview POST, read back by GET /events/new, cleared on
  a successful create. Exists so a page refresh (dev hot-reload especially)
  repaints what was being typed instead of the house defaults. Deliberately an
  atom, not the event log — an abandoned draft is not history."}
  create-drafts (atom {}))

(defn handle-new-event [req]
  (let [person (auth/current-person req)
        draft  (get @create-drafts (:id person))]
    (html-response (views/new-event-page (request-host req) (or draft {}) nil person))))

(defn- add-creator-as-chair!
  "Seed a brand-new conference's committee with the one person who made it.

   Load-bearing since authorization went per-event: a conference whose roster is
   empty is a conference nobody can open. Idempotent about :already-member so a
   double-submit can't 500."
  [event person]
  (when person
    (when-let [committee (first (events/committees-for-event (:id event)))]
      (try
        (committees/add-member! (:id committee)
                                {:name (:name person)
                                 :email (:email person)
                                 :role "chair"}
                                (:email person))
        (catch clojure.lang.ExceptionInfo e
          (when-not (= :already-member (:type (ex-data e))) (throw e)))))))

(defn handle-create-event
  "Plain form POST. Valid → 303 to the new event's dashboard.
   Invalid → 422 re-render of the form with server-side messages (no client JS).

   The form is three visible fields now, so the DEFAULTS matter as much as the
   input: the call's opening moment, whether it opens at all, and the support
   address all come from `events/apply-create-defaults`. They only fill blanks,
   which is why the full-field POST the drive and the older tests send still
   behaves exactly as it always did."
  [req]
  (let [params (:params req)
        person (auth/current-person req)
        draft (events/apply-create-defaults (events/parse-form params)
                                            (:email person))
        errors (events/validation-errors draft)]
    (if errors
      (do (log/info :event-create-rejected :fields (vec (keys errors)))
          (html-response 422 (views/new-event-page (request-host req)
                                                   ;; keep what they typed, but show the slug we derived
                                                   (assoc params :slug (or (:slug draft) (:slug params)))
                                                   errors
                                                   (auth/current-person req))))
      (try
        (let [event (events/create-event! draft (or (:email person) "organizer"))]
          ;; Whoever creates a conference joins its Program Committee as chair —
          ;; the roster starts at exactly one person, and every other reviewer is
          ;; added deliberately, on this event only. This is also what keeps a
          ;; first-run organizer from locking themselves out: the first-run
          ;; window closes the moment an event exists, so the creator has to be
          ;; inside that first membership before it shuts.
          (add-creator-as-chair! event person)
          ;; The draft became a real event; forget the scratch copy.
          (swap! create-drafts dissoc (:id person))
          ;; Straight to STEP 2 (Create CFP form) — the wizard is serial, and
          ;; the dashboard is not a step (Gene, 2026-08-09). It stays one click
          ;; away in the spine.
          (see-other (str "/events/" (:slug event) "/form")))
        (catch clojure.lang.ExceptionInfo e
          (if (= :duplicate-slug (:type (ex-data e)))
            (html-response 422 (views/new-event-page
                                (request-host req)
                                (assoc params :slug (:slug draft))
                                {:slug [(str "That URL was taken a moment ago by "
                                             (or (events/display-name
                                                  (store/get-event-by-slug (:slug draft)))
                                                 (:slug draft))
                                             ". Pick a different URL in More options.")]}
                                (auth/current-person req)))
            (throw e)))))))

(defn handle-create-demo-event
  "The judge's one-click event. Same rule as a real create: whoever pressed the
   button chairs it, or nobody could get back into it."
  [req]
  (let [event (events/create-demo-event!)]
    (add-creator-as-chair! event (auth/current-person req))
    (log/info :demo-event-created :slug (:slug event))
    (see-other (str "/events/" (:slug event)))))

(defn not-found-page [slug]
  (html-response 404 (views/page-shell "Not found"
                                       [:div.ui.warning.message
                                        [:div.header "No such event"]
                                        [:p (str "Nothing is published at /events/" slug ".")]]
                                       [:a.ui.basic.button {:href "/events"} "All events"])))

(defn- dashboard-state
  "Build the Mission Control facts from the currently bound store projection."
  [req event member-form]
  (let [cs (events/committees-for-event (:id event))
        committee (first cs)]
    {:committee (when committee
                  {:committee-id (:id committee) :name (:name committee)})
     :members (if committee (committees/members-for-committee (:id committee)) [])
     :committee-count (count cs)
     :member-form member-form
     :subs (submissions/for-event (:id event))
     :sub-count (submissions/count-for-event (:id event))
     :speaker-count (submissions/unique-speaker-count (:id event))
     :cfp-state (submissions/cfp-state event)
     :alerts (inform/alert-rows event)
     :uncommunicated (count (inform/pending-decisions (:id event)))
     ;; The checklist ticks on a RECORDED fact (form.updated / form.reviewed),
     ;; never on "the organizer opened a page".
     :form-reviewed? (forms/reviewed? (:id event))
     :form-field-count (count (submissions/session-fields
                               (forms/active-fields (forms/fields-for-event (:id event)))))
     ;; Mission-control data (Gene, 2026-08-10): tiles, queues, feed.
     :enriched (reviews/enriched-for-event (:id event))
     :coverage (reviews/coverage (:id event)
                                 (or (some-> committee :coverage-target) 2))
     :recent (->> (store/log-for-event (:id event)) (take-last 12) reverse vec)
     :person-name-of (fn [pid] (get-in @store/state [:people pid :name]))
     :submission-of store/submission-by-id
     :person (auth/current-person req)}))

(defn- event-log-ats
  "The timestamps of this event's own log, in order."
  [event-id]
  (mapv :at (store/log-for-event event-id)))

(defn time-travel-context
  "Returns {:as-of :cutoff :bounds :index :total :base-path} — nil :cutoff means
   we are looking at now."
  [req event base-path]
  (let [ats (event-log-ats (:id event))
        total (count ats)
        raw (not-blank (get-in req [:params :at-index]))
        idx (when raw (try (Integer/parseInt raw) (catch Exception _ nil)))
        idx (when (and idx (< idx (dec total)) (>= idx 0)) idx)
        cutoff (when idx (nth ats idx))]
    {:as-of cutoff
     :cutoff cutoff
     :fragment-path (str base-path "/fragment")
     :bounds (when (seq ats) [(first ats) (last ats)])
     :index (or idx (max 0 (dec total)))
     :total total
     :base-path base-path}))

(defn render-event-dashboard
  "Render an event's dashboard. `member-form` carries the state of a rejected
   add-member attempt so the form comes back filled in with its errors."
  ([req event] (render-event-dashboard req event nil 200))
  ([req event member-form status]
   (let [base-path (str "/events/" (:slug event))
         tt (time-travel-context req event base-path)]
     (html-response
      status
      (views/event-dashboard-page
       (request-host req)
       event
       (assoc (dashboard-state req event member-form) :time-travel tt))))))

(defn render-committee-page
  "Render step 3's own page. `member-form` carries a rejected add's state."
  ([req event] (render-committee-page req event nil 200))
  ([req event member-form status]
   (let [committee (first (events/committees-for-event (:id event)))]
     (html-response
      status
      (views/committee-page
       event
       {:committee (when committee
                     {:committee-id (:id committee) :name (:name committee)})
        :members (if committee
                   (committees/members-for-committee (:id committee)) [])
        :member-form member-form
        :cfp-state (submissions/cfp-state event)
        :person (auth/current-person req)})))))

(defn handle-event-committee
  "GET /events/:slug/committee — create / edit the review committee, a page
   you go to rather than an anchor you scroll to (Gene, 2026-08-09)."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (render-committee-page req event)
      (not-found-page slug))))

(defn handle-event-dashboard [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (render-event-dashboard req event)
      (not-found-page slug))))

(defn handle-event-details
  "GET /events/:slug/details — the step-1 page you can COME BACK to (Gene,
   2026-08-09: 'Create and Edit Event'). Writes the pitch the public CFP
   masthead displays."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response
       (views/event-details-page
        event {:person (auth/current-person req)
               :notice (when (get-in req [:params :saved]) "Saved.")}))
      (not-found-page slug))))

(defn handle-event-details-save [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [p (:params req)
            nm (not-blank (:name p))
            starts (events/parse-date (:starts-on p))
            ends (events/parse-date (:ends-on p))
            tz (not-blank (:tz p))
            date? #(instance? java.time.LocalDate %)
            errors (cond-> {}
                     (nil? nm)
                     (assoc :name "The event needs a name — it is load-bearing everywhere.")
                     (keyword? starts) (assoc :starts-on "Not a readable date.")
                     (keyword? ends) (assoc :ends-on "Not a readable date.")
                     (and (date? starts) (date? ends)
                          (.isAfter ^java.time.LocalDate starts ^java.time.LocalDate ends))
                     (assoc :ends-on "The event ends before it starts.")
                     (and tz (not (events/valid-timezone? tz)))
                     (assoc :tz "Unknown time zone."))]
        (if (seq errors)
          (html-response 422 (views/event-details-page
                              event {:person (auth/current-person req)
                                     :errors errors :values p}))
          (do (events/update-event-details!
               (:id event)
                ;; Every field of the form is sent every time, so a cleared box
                ;; is a deliberate clear — present-with-nil erases. Dates ride
                ;; as ISO strings (the store's event.updated fold re-parses).
               {:name nm
                :starts-on (some-> starts str)
                :ends-on (some-> ends str)
                :tz tz
                :cfp-intro (not-blank (:cfp-intro p))
                :location (not-blank (:location p))
                :website-url (not-blank (:website-url p))
                :support-email (not-blank (:support-email p))}
               (or (:email (auth/current-person req)) "organizer"))
              (see-other (str "/events/" slug "/details?saved=1")))))
      (not-found-page slug))))

;; --- The create page's live marquee -----------------------------------------

(defn- datastar-signals
  "Datastar POSTs its signals as a JSON body (a GET puts them in the query
   string). There is no JSON middleware in this app and there should not be —
   one endpoint speaks this dialect, so it parses its own body."
  [req]
  (try
    (let [sigs (d*/get-signals req)]
      (cond
        (nil? sigs) {}
        (string? sigs) (json/read-str sigs :key-fn keyword)
        :else (with-open [r (io/reader sigs)]
                (json/read r :key-fn keyword))))
    (catch Exception e
      (log/warn :preview-unparseable-signals :msg (.getMessage e))
      {})))

(defn handle-events-preview
  "POST /api/events/preview — the marquee, re-rendered from what is being typed.

   Writes nothing, not even a draft; its only store read is the slug-owner
   lookup that powers the live available/taken line. Every
   keystroke on /events/new arrives here debounced, and the answer is the same
   `views/event-marquee` the page was first painted with, pushed down THIS
   viewer's SSE stream and nobody else's. The POST itself answers 204 — the
   server decides what the screen shows, and it says so on the stream, never in
   a response body the client would have to interpret.

   `?apply-trim=1` is the assist button: the server recomputes the same
   suggestion, applies it, pushes the corrected marquee, and then patches the
   `evname` SIGNAL so the input updates itself. No client JavaScript touches a
   value — that is the whole reason the trim is a round trip instead of a
   one-line DOM write."
  [req]
  (let [person (auth/current-person req)
        sigs   (datastar-signals req)
        trim?  (some? (get-in req [:params :apply-trim]))
        ;; The `ev*` names are the signal names the form binds (single words on
        ;; purpose — Datastar camelCases hyphens). The plain aliases are for
        ;; curl: a probe that spells them the obvious way gets a real answer
        ;; instead of a ghost marquee and a puzzling 204.
        typed  {:name      (or (:evname sigs) (:name sigs))
                :location  (or (:evloc sigs) (:location sigs))
                :starts-on (or (:evstarts sigs) (:starts-on sigs) (:starts sigs))
                :ends-on   (or (:evends sigs) (:ends-on sigs) (:ends sigs))}
        found  (events/trim-suggestion (:name typed) (:location typed))
        typed  (cond-> typed (and trim? found) (assoc :name (:trimmed found)))
        slug   (or (not-blank (:evslug sigs)) (events/derive-slug typed))
        open?  (not= "closed" (not-blank (:evcfp sigs)))
        reach  (when person
                 (sse/person-connection-count sse/new-event-channel (:id person)))]
    (if-not person
      (log/warn :preview-push-skipped :why :no-session)
      (do
        ;; Remember what is being typed, per person, so a page refresh repaints
        ;; the draft instead of the house defaults. Cleared on create.
        (swap! create-drafts update (:id person) merge
               (into {} (filter (comp some? val))
                     {:name (:name typed) :location (:location typed)
                      :starts-on (:starts-on typed) :ends-on (:ends-on typed)
                      :slug (not-blank (:evslug sigs))
                      :cfp-state (not-blank (:evcfp sigs))
                      :website-url (not-blank (:evweb sigs))
                      :cfp-closes-on (not-blank (:evcloses sigs))
                      :tz (not-blank (:evtz sigs))
                      :support-email (not-blank (:evsupport sigs))}))
        ;; A 204 that pushed to nobody is the instrument lying. Say so.
        (when (zero? reach)
          (log/warn :preview-push-no-subscriber
                    :event-id sse/new-event-channel :person-id (:id person)
                    :registrations (sse/registrations)
                    :msg "nothing is listening on this key — the marquee cannot move"))
        ;; Elements first, signals second (CLAUDE.md #9) — a signal patch that
        ;; lands before its target exists fires an effect against nothing.
        (sse/push-to-person! sse/new-event-channel (:id person) "#event-marquee"
                             #(views/event-marquee (request-host req) typed slug
                                                   (when-not trim? found) open?))
        ;; The URL field's own line answers "is this address free?" live, from
        ;; the same derivation the marquee just used and the same owner lookup
        ;; the create-time refusal uses.
        (sse/push-to-person! sse/new-event-channel (:id person) "#slug-status"
                             #(views/slug-status slug (events/slug-owner-display slug)))
        ;; Signals AFTER elements (CLAUDE.md #9): the ghost in the URL box
        ;; follows the derivation, so it never disagrees with the green line.
        (sse/push-signals-to-person! sse/new-event-channel (:id person)
                                     {:slugghost (or slug "eais-charlotte")})
        (when (and trim? found)
          (sse/push-signals-to-person! sse/new-event-channel (:id person)
                                       {:evname (:name typed)}))
        (log/debug :preview-pushed :reach reach :name (:name typed) :slug slug)))
    {:status 204 :headers {} :body ""}))

(defn handle-draft-pref
  "POST /api/events/draft-pref — tiny per-person create-page preferences.

   Today exactly one: whether More Options is expanded. The <details> element
   stays native (the browser owns the click), but it REPORTS its state here so
   a refresh re-renders the draft with the panel the way it was left."
  [req]
  (when-let [person (auth/current-person req)]
    (let [{:keys [more-open]} (datastar-signals req)]
      (swap! create-drafts update (:id person) merge {:more-open? (boolean more-open)})))
  {:status 204 :headers {} :body ""})

(defn- visible-sse-registrations
  "Only registrations this organizer is authorized to diagnose.

   Real event streams follow committee membership. The create-page pseudo-
   event is per-person, so expose only the caller's own connection."
  [person]
  (let [event-ids (into #{} (map :id) (events/events-for-person (:id person)))]
    (filterv (fn [{:keys [event-id person-id]}]
               (or (contains? event-ids event-id)
                   (and (= sse/new-event-channel event-id)
                        (= (:id person) person-id))))
             (sse/registrations))))

(defn handle-sse-state
  "GET /dev/sse-state — who is listening, and what would be pushed.

   Read-only, organizer-gated, no secrets. It exists because \"SSE isn't
   updating\" is otherwise a twenty-minute bisect: this turns it into one
   request that says whether anyone is registered, under which keys, and what
   the fragment for a given set of params would actually look like."
  [req]
  (let [person (auth/current-person req)
        registrations (visible-sse-registrations person)
        p (:params req)
        typed {:name (:name p) :location (:location p)
               :starts-on (:starts-on p) :ends-on (:ends-on p)}]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str
            {:subscribers (count registrations)
             :registrations registrations
             :you {:person-id (:id person)
                   :new-event-channel sse/new-event-channel
                   :connections-on-create-page
                   (sse/person-connection-count sse/new-event-channel (:id person))}
             :display-name (events/display-name typed)
             :derived-slug (events/derive-slug typed)
             :marquee (str (h/html (views/event-marquee (request-host req) typed
                                                        (events/derive-slug typed)
                                                        nil true)))})}))

;; --- Opening and closing the call for speakers ------------------------------

(defn handle-exports-page
  "GET /events/:slug/exports — the HTML index for the four export files.

   Note the path: the raw files live under /events/:slug/exports/<file> and are
   deliberately PUBLIC (auth/open-data-pattern matches the trailing slash). This
   page has no trailing segment, so it does not match that pattern and stays
   behind the organizer gate like every other workspace page."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response (views/exports-page (request-host req) event
                                         {:person (auth/current-person req)}))
      (not-found-page slug))))

(defn handle-cfp-open
  "POST /api/events/:slug/cfp/open — a deliberate act, by a named person."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [actor (or (:email (auth/current-person req)) "organizer")]
        (events/open-cfp! event actor)
        (log/info :cfp-opened :slug slug :actor actor)
        (see-other (str "/events/" slug)))
      (not-found-page slug))))

(defn handle-cfp-close
  "POST /api/events/:slug/cfp/close — stops new submissions immediately."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [actor (or (:email (auth/current-person req)) "organizer")]
        (events/close-cfp! event actor)
        (log/info :cfp-closed :slug slug :actor actor)
        (see-other (str "/events/" slug)))
      (not-found-page slug))))

(defn handle-cfp-close-date
  "POST /api/events/:slug/cfp/close-date — the scheduled half: set or clear the
   date the call shuts. Blank clears it and the call stays open."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [actor (or (:email (auth/current-person req)) "organizer")]
        (try
          (events/set-cfp-close-date! event (get-in req [:params :cfp-closes-on]) actor)
          (log/info :cfp-close-date-set :slug slug
                    :date (get-in req [:params :cfp-closes-on]))
          (catch clojure.lang.ExceptionInfo e
            (if (= :invalid-close-date (:type (ex-data e)))
              (log/info :cfp-close-date-rejected :slug slug :value (:value (ex-data e)))
              (throw e))))
        (see-other (str "/events/" slug "/settings")))
      (not-found-page slug))))

(def ^:private id-pattern
  #"(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

(defn- clean-id
  "Ids travel as strings (the log is JSON). Reject anything that isn't shaped
   like one rather than letting junk reach a lookup."
  [s]
  (when (and s (re-matches id-pattern (str s))) (str s)))

;; --- Identity ---------------------------------------------------------------

(defn handle-login-page [req]
  ;; Dev-only cycle-time saver: prefill the email box with the first reviewer
  ;; on file (in the seeded world, Gene), so signing in is Enter, not typing.
  ;; Production renders an empty field — prefilling anyone's address on a
  ;; public page would leak it.
  (let [prefill (when (auth/dev?)
                  (let [snap (store/snapshot)
                        pid  (some-> snap :memberships vals first :person-id)]
                    (get-in snap [:people pid :email])))]
    (html-response (views/login-page {:next (get-in req [:params :next])
                                      :dev? (auth/dev?)
                                      :demo? (demo-mode?)
                                      :google? (auth-google/enabled?)
                                      :prefill-email prefill}))))

(defn handle-login
  "Mint a link for a committee member. An unknown email gets the SAME neutral
   message — this endpoint must not become a way to enumerate who is on a
   committee.

   Where the link GOES depends on the deployment, and the page never lies about
   it:
     dev                 → shown on the page (a developer with no mailer would
                           otherwise be locked out of their own app)
     prod + SMTP         → emailed; the page says to check
     prod + no SMTP      → NEITHER. Printing a working credential on an
                           unauthenticated page in production would hand an
                           account to anyone who can guess an email address."
  [req]
  (let [email (get-in req [:params :email])
        next* (get-in req [:params :next])
        token (auth/issue-token! email)
        dev? (auth/dev?)
        smtp? (mail/configured?)]
    (html-response
     (views/login-page
      (cond-> {:sent-to email :dev? dev?
               :next next*
               :message
               (cond
                 (and token dev?) "Found you on a committee."
                 (and token smtp?) (str "If " email
                                        " is on a committee, a link is on its way. "
                                        "It works once and expires in 24 hours.")
                 token (str "You're on a committee, but this deployment has no "
                            "mail configured — so no link can be sent. Ask an "
                            "organizer to send you one from the Comms page.")
                 :else (str "If " email " is on a committee, a link is on its way."))}
          ;; ONLY in dev.
        (and token dev?)
        (assoc :link (str "/auth/" token
                          (when (not-blank next*)
                            (str "?next=" (java.net.URLEncoder/encode next* "UTF-8"))))))))))

(defn handle-auth-token
  "Redeem a link and start a session."
  [req]
  (let [token (get-in req [:path-params :token])
        next* (get-in req [:params :next])]
    (if-let [person-id (auth/redeem-token! token)]
      (auth/sign-in {:status 303
                     :headers {"Location" (if (not-blank next*)
                                            next*
                                            ;; Committee members land on the
                                            ;; organizer view; speakers on their
                                            ;; own portal.
                                            (auth/home-path (store/person-by-id person-id)))}
                     :body ""}
                    person-id)
      (html-response 400 (views/login-page
                          {:dev? (auth/dev?)
                           :message "That link has expired or was already used. Ask for a new one."})))))

(defn handle-logout [_req]
  (auth/sign-out {:status 303 :headers {"Location" "/login"} :body ""}))

;; --- Demo mode: judge-friendly sign-in (bd -o42) -----------------------------
;;
;; A deployed demo has no SMTP, so magic links can't reach a judge's inbox and
;; the login page renders them only under ENV=dev. Demo mode is the deliberate,
;; flagged third state: one-click sign-in AS the seeded demo people, for
;; instances whose whole purpose is being driven by strangers (swyx, the eval
;; kit's manual pre-auth step). Never on by default; the login page says
;; plainly that it's a demo instance.

(def ^:private demo-roles
  "role → seeded demo-world email. These people exist after `make seed-demo`."
  {"organizer" "genek@itrevolution.net"
   "reviewer"  "annp@itrevolution.net"
   "speaker"   "priya.raghavan@meridianmutual.example.com"})

(defn handle-demo-login
  "POST /api/demo-login?role=organizer|reviewer|speaker — demo mode only.

   404 when the flag is off (the endpoint does not exist as far as a
   production instance is concerned); 422 with a plain answer when the demo
   world isn't seeded."
  [req]
  (if-not (demo-mode?)
    (not-found-page "demo-login")
    ;; `email` (the dev drawer's identity switcher, Gene 2026-08-09) wins over
    ;; the role shortcut. Demo-mode-gated like everything else here: on a
    ;; production instance this endpoint does not exist.
    (let [role  (get-in req [:params :role])
          email (or (not-blank (get-in req [:params :email]))
                    (get demo-roles role))]
      (if-let [person (and email (store/person-by-email email))]
        (do (log/info :demo-login :role role :person-id (:id person))
            ;; Switching identity from the dev strip returns you to the PAGE
            ;; you were on (Gene, 2026-08-10) — you switch to keep reviewing,
            ;; not to start over. The role-button path (login page) keeps its
            ;; home-path behaviour via the referer fallback.
            (auth/sign-in {:status 303
                           :headers {"Location"
                                     (let [ref (get-in req [:headers "referer"])]
                                       (if (and ref (not (str/includes? ref "/login")))
                                         ref
                                         (auth/home-path person)))}
                           :body ""}
                          (:id person)))
        (html-response 422 (views/login-page
                            {:dev? (auth/dev?)
                             :message (str "The demo world isn't seeded yet — "
                                           "run `make seed-demo` first.")}))))))

;; --- The review board -------------------------------------------------------

(defn- coverage-target-for [event-id]
  (or (:coverage-target (first (store/committees-for-event event-id))) 2))

(defn- board-state
  "Everything the board page needs, from the query params."
  [req event]
  (let [params (:params req)
        target (coverage-target-for (:id event))
        all (reviews/enriched-for-event (:id event))
        sort-key (or (not-blank (:sort params)) reviews/default-sort)
        q (:q params)
        status (:status params)
        track (:track params)
        filtered (reviews/filter-board all {:q q :status status :track track})]
    {:rows (reviews/sort-board filtered sort-key target)
     :total (count all)
     :coverage (reviews/coverage (:id event) target)
     :sort-key sort-key
     :q q
     :status status
     :track track
     :track-counts (reviews/track-counts all)
     :status-counts (reviews/status-counts all)
     :sort-presets reviews/sort-presets
     :uncommunicated (count (inform/pending-decisions (:id event)))
     ;; Whatever the server last refused to do for THIS person on THIS event.
     ;; Read on every board render, so the message survives the 303 that a form
     ;; POST turns into — and disappears the moment the server clears it.
     :notice (notices/notice-for (:id event) (:id (auth/current-person req)))
     :person (auth/current-person req)}))

;; --- Time travel ------------------------------------------------------------
;;
;; `?at-index=N` renders the world after the first N+1 stored events for this
;; event. An index rather than a raw timestamp because it makes the slider
;; honest: every notch is a real thing that happened, and dragging one notch
;; always moves the picture.

(defmacro with-as-of
  "Render `body` against the world as of `cutoff` (or now when nil)."
  [cutoff & body]
  `(if-let [c# ~cutoff]
     (binding [store/*as-of-state* (store/state-as-of c#)
               store/*as-of-cutoff* c#]
       ~@body)
     (do ~@body)))

(defn- sse-fragment-response
  "A ONE-SHOT Datastar response: patch one region, then close.

   Used by the live time-travel scrub. It cannot go over the shared board SSE
   connection, because that broadcasts to everyone watching the event — one
   organizer dragging a slider must not move anybody else's screen."
  [req selector html]
  (hk/->sse-response
   req
   {hk/on-open
    (fn [sse]
      (d*/with-open-sse sse
        (d*/patch-elements! sse html
                            {d*/selector selector
                             d*/patch-mode d*/pm-outer})))}))

(defn board-fragment-html
  "The board region as of the slider position, as an HTML string.

   Separate from the transport on purpose: an SSE response needs a live
   http-kit channel, so the interesting part — WHAT gets patched — would
   otherwise only be observable through a real socket. This is the part worth
   testing, and the handler below is then trivially correct."
  [req event]
  (let [tt (time-travel-context req event (str "/events/" (:slug event) "/board"))]
    (with-as-of (:cutoff tt)
      (let [past-event (or (events/event-by-slug (:slug event)) event)]
        (str (h/html (views/board-region past-event (board-state req past-event))))))))

(defn dashboard-fragment-html
  "The Mission Control facts as of the slider position. The page shell and
   scrubber stay outside this fragment so a patch cannot interrupt dragging."
  [req event]
  (let [tt (time-travel-context req event (str "/events/" (:slug event)))]
    (with-as-of (:cutoff tt)
      (let [past-event (or (events/event-by-slug (:slug event)) event)]
        (str (h/html
              (views/event-dashboard-region
               (request-host req)
               past-event
               (dashboard-state req past-event nil))))))))

(defn log-fragment-html
  [req event]
  (let [tt (time-travel-context req event (str "/events/" (:slug event) "/log"))]
    (with-as-of (:cutoff tt)
      (let [past-event (or (events/event-by-slug (:slug event)) event)]
        (str (h/html (views/log-region past-event
                                       (events/log-for-event (:id past-event)))))))))

(defn- text-response
  ([content-type body] (text-response 200 content-type body))
  ([status content-type body]
   {:status status
    :headers {"Content-Type" content-type
              ;; Anyone may fetch the published program — that is the point.
              "Access-Control-Allow-Origin" "*"
              "Cache-Control" "public, max-age=60"}
    :body body}))

(defn- json-response
  ([data] (json-response 200 data))
  ([status data] (text-response status "application/json; charset=utf-8" (exports/->json data))))

(defn- with-event [req f]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (f event)
      (json-response 404 {"error" "no such event" "slug" slug}))))

(defn handle-board-sort
  "POST /api/events/:slug/board/sort {sort q status track} — re-sort THIS
   viewer's board and push #board-region down their open SSE stream. No
   reload; the client stamps the same state into the URL with replaceState
   (the URL is the state — Gene, 2026-08-10)."
  [req]
  (with-event req
    (fn [event]
      (let [body (try (json/read (io/reader (:body req)) :key-fn keyword)
                      (catch Exception _ {}))
            req' (update req :params merge
                         (into {} (filter (comp not-blank str val))
                               (select-keys body [:sort :q :status :track])))
            person (auth/current-person req)]
        (if-not person
          (log/warn :board-sort-skipped :why :no-session)
          (sse/push-to-person! (:id event) (:id person) "#board-region"
                               (fn [] (h/raw (board-fragment-html req' event)))))
        {:status 204 :headers {} :body ""}))))

(defn handle-board-fragment
  "Patches the board region as of the slider position. The slider itself is NOT
   in this fragment — patching an element mid-drag cancels the gesture."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (sse-fragment-response req "#board-region" (board-fragment-html req event))
      (not-found-page slug))))

(defn handle-dashboard-fragment
  "Patches only Mission Control facts for this organizer's scrub position."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (sse-fragment-response req "#dashboard-region" (dashboard-fragment-html req event))
      (not-found-page slug))))

(defn handle-log-fragment [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (sse-fragment-response req "#log-region" (log-fragment-html req event))
      (not-found-page slug))))

(defn handle-board [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [tt (time-travel-context req event (str "/events/" slug "/board"))]
        (with-as-of (:cutoff tt)
          ;; Re-read the event INSIDE the binding: as of that moment it may have
          ;; had a different name, or not existed at all.
          (let [past-event (or (events/event-by-slug slug) event)]
            (html-response
             (views/board-page past-event
                               (assoc (board-state req past-event) :time-travel tt))))))
      (not-found-page slug))))

(defn handle-submission-detail [req]
  (let [slug (get-in req [:path-params :slug])
        sub-id (clean-id (get-in req [:path-params :submission-id]))
        event (events/event-by-slug slug)
        row (when sub-id (store/submission-by-id sub-id))]
    (if (and event row (= (:id event) (:event-id row)))
      (html-response
       (views/submission-detail-page
        event (reviews/enrich row)
        {:person (auth/current-person req)
         :coverage-target (coverage-target-for (:id event))
         :notice (notices/notice-for (:id event) (:id (auth/current-person req)))}))
      (not-found-page slug))))

(defn- detail-page-response
  "The submission page at an arbitrary status — used to answer a rejected write
   with the page the reviewer was actually looking at."
  [req event row status]
  (let [person (auth/current-person req)]
    (html-response
     status
     (views/submission-detail-page
      event (reviews/enrich row)
      {:person person
       :coverage-target (coverage-target-for (:id event))
       :notice (notices/notice-for (:id event) (:id person))}))))

;; --- Review mutations: mutate → push everyone → 303 the caller ---------------
;;
;; No client JS anywhere. The actor gets a fresh page from the redirect; every
;; OTHER connected reviewer gets the row and the coverage bar pushed over SSE.

(defn push-board-updates!
  "Repaint one submission row (rendered per viewer, because it shows each
   reviewer their own rating) and the coverage headline."
  [event submission-id]
  (when-let [row (store/submission-by-id submission-id)]
    (let [enriched (reviews/enrich row)
          target (coverage-target-for (:id event))]
      (sse/push-personal-fragment!
       (:id event) (str "#sub-" submission-id)
       (fn [person-id]
         (views/board-row event enriched (when person-id (store/person-by-id person-id)))))
      (sse/push-fragment!
       (:id event) "#coverage-bar"
       (fn [] (views/coverage-bar event (reviews/coverage (:id event) target)))))))

;; --- Refusing a bad value, on both surfaces at once --------------------------
;;
;; A rejected write used to answer 303 and do nothing (bd sessionize-sched-
;; killer-xmf). It now answers 422 — but 422 to WHOM decides what the body is:
;;
;;   a browser  → the page they were on, re-rendered, with the reason at the top
;;                and their work intact. Plus the same notice pushed over SSE to
;;                every other tab of theirs on that board, server-rendered.
;;   anything   → JSON: {"error": ..., "allowed": [...]}. The REST API is an
;;   else         advertised feature and must not get worse so the UI can get
;;                nicer, so the honest status and the machine-readable reason
;;                are the same on both paths.
;;
;; No client JavaScript is involved on either path. The browser POSTs a plain
;; form and the server decides everything: the words, the status code, which
;; page comes back, and when the message goes away.

(def ^:private value-rejections
  "Failures caused by a bad VALUE the caller sent — as opposed to a bug. Each
   one carries a :user-message written for a human."
  #{:invalid-status :invalid-stars :missing-stars :empty-comment})

(defn- wants-html?
  "A browser says so in Accept. curl (`*/*`) and every HTTP client we have met
   do not, and they get JSON."
  [req]
  (str/includes? (str (get-in req [:headers "accept"])) "text/html"))

(defn push-notice!
  "Repaint the notice region for everyone on this board — each of them seeing
   THEIR OWN notice, which is almost always nothing.

   Personal on purpose: a refusal is one person's business. Broadcasting one
   render would show the whole committee somebody else's mistake, the same way
   broadcasting one board row would show everyone the actor's star highlight."
  [event]
  (sse/push-personal-fragment!
   (:id event) "#validation-notice"
   (fn [person-id]
     (views/notice-region event (notices/notice-for (:id event) person-id)))))

(defn- reject-value!
  "Remember the refusal, push it, and answer 422 in the caller's own language."
  [req event row person message data]
  (notices/set-notice! (:id event) (:id person)
                       {:kind :error
                        :message message
                        :submission-id (:id row)
                        :at (store/now-iso)})
  (push-notice! event)
  (log/info :write-refused :submission-id (:id row) :type (:type data)
            :actor (:email person))
  (if (wants-html? req)
    (if (str/includes? (str (get-in req [:headers "referer"]))
                       (str "/submissions/" (:id row)))
      (detail-page-response req event row 422)
      ;; Render the board from a request with NO params. The params here are the
      ;; POST body — rendering with them made `status=Bogus` a board FILTER, so
      ;; the reviewer's answer to a bad status was an empty board that said "no
      ;; submissions match that search". The board's own query state belongs to
      ;; the GET that drew it, never to the form that failed.
      (let [clean (assoc req :params {})
            base-path (str "/events/" (:slug event) "/board")]
        (html-response 422 (views/board-page
                            event
                            (assoc (board-state clean event)
                                   :time-travel (time-travel-context clean event base-path))))))
    ;; Deliberately NOT the shared `json-response` helper: that one sets
    ;; `Cache-Control: public, max-age=60` for the open-data endpoints, and a
    ;; cached refusal is a refusal you can't retry.
    {:status 422
     :headers {"Content-Type" "application/json; charset=utf-8"
               "Cache-Control" "no-store"}
     :body (exports/->json
            (cond-> {"error" message
                     "submission-id" (:id row)}
              (:allowed data) (assoc "allowed" (vec (:allowed data)))))}))

(defn- with-submission
  "Resolve event + submission + signed-in person, then run `f`. Every review
   mutation needs the same three things and the same failure modes.

   `f` either does the write or throws a typed refusal (see `reviews/refuse!`).
   Success clears any standing notice — the thing the person was told about is
   now done, so the server takes the message down. That is the whole lifecycle
   of the message, and none of it is a timer in a browser."
  [req f]
  (let [sub-id (clean-id (get-in req [:path-params :submission-id]))
        row (when sub-id (store/submission-by-id sub-id))
        person (auth/current-person req)
        event (when row (events/event-by-id (:event-id row)))]
    (cond
      (nil? row) (not-found-page (str "submission " sub-id))
      (nil? person) {:status 302 :headers {"Location" "/login"} :body ""}
      :else
      (try
        (f event row person)
        (notices/clear-notice! (:id event) (:id person))
        (push-board-updates! event sub-id)
        (push-notice! event)
        ;; postJSON callers (the board's stars + comments) navigate NOWHERE:
        ;; 204, and the SSE per-person push repaints the row. Form fallbacks
        ;; still 303 — with the row anchor re-appended, because referer
        ;; headers never carry fragments and without it the quick-rate card
        ;; folds mid-thought (Gene, 2026-08-10).
        (if (some-> (get-in req [:headers "content-type"])
                    (str/includes? "application/json"))
          {:status 204 :headers {} :body ""}
          (see-other (str (or (get-in req [:headers "referer"])
                              (str "/events/" (:slug event) "/board"))
                          "#sub-" sub-id)))
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            (if (contains? value-rejections (:type data))
              (reject-value! req event row person
                             (or (:user-message data) (.getMessage e)) data)
              (throw e))))))))

(defn- absorb-json-params
  "postJSON callers arrive as application/json — merge the body into :params
   so handlers read stars/body/status identically for form and fetch. Done in
   the HANDLER (not with-submission) because the handler fns close over THIS
   req; a rebind inside the wrapper never reaches them (the closure-capture
   lesson, 2026-08-10)."
  [req]
  (if (some-> (get-in req [:headers "content-type"])
              (str/includes? "application/json"))
    (update req :params merge
            (try (json/read (io/reader (:body req)) :key-fn keyword)
                 (catch Exception _ {})))
    req))

(defn handle-rate [req*]
  (let [req (absorb-json-params req*)]
    (with-submission
      req
      (fn [_event row person]
        (reviews/rate! (:id row) (:id person)
                       (get-in req [:params :stars]) (:email person))))))

(defn handle-comment [req*]
  (let [req (absorb-json-params req*)]
    (with-submission
      req
      (fn [_event row person]
        (reviews/add-comment! (:id row) (:id person)
                              (get-in req [:params :body]) (:email person))))))

(defn- chair-on-event?
  "Server-side half of the chair gate (Gene, 2026-08-09: reviewers rate and
   argue; chairs decide). The view hides the control; this refuses the write."
  [event person]
  (boolean
   (when person
     (let [committee (first (events/committees-for-event (:id event)))]
       (some #(and (= (:person-id %) (:id person)) (= "chair" (:role %)))
             (when committee
               (committees/members-for-committee (:id committee))))))))

(defn handle-status [req]
  (with-submission
    req
    (fn [event row person]
      (when-not (chair-on-event? event person)
        (throw (ex-info "chair only"
                        {:type :not-chair
                         :user-message (str "Setting a decision is the chair's "
                                            "act — rate and comment to make "
                                            "the case.")})))
      (reviews/set-status! (:id row) (get-in req [:params :status]) (:email person)))))

(defn handle-notice-dismiss
  "Take the message down because the reader asked. A POST like everything else —
   no JavaScript, works with scripting off."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [person (auth/current-person req)]
        (notices/clear-notice! (:id event) (:id person))
        (push-notice! event)
        (see-other (or (get-in req [:headers "referer"])
                       (str "/events/" slug "/board"))))
      (not-found-page slug))))

(defn handle-priority [req]
  (with-submission
    req
    (fn [_event row person]
      (reviews/toggle-priority! (:id row) (:email person)))))

;; --- Speaker portal ---------------------------------------------------------

(defonce ^{:doc "Per-person portal drafts: {person-id {scope {param value}}}.

  `scope` is \"profile\" or a submission id, because the portal can have two
  forms open at once and a bio must never leak into a talk. Same contract as the
  public page's `cfp-drafts`: a refresh repaints what was being typed. The
  portal's viewer always HAS an account, so this keys on person-id — which is
  strictly better than a cookie, since it survives a new browser."}
  portal-drafts (atom {}))

(defonce ^{:doc "Last portal note pushed, per person: {person-id {param note}}."}
  portal-notes-sent (atom {}))

(defn portal-channel
  "The SSE key for one speaker's portal. Per PERSON, not per event — a speaker's
   portal spans every conference they submitted to, and two speakers must never
   land on the same key."
  [person-id]
  (str "portal-" person-id))

(defn portal-draft-for
  "What this person has typed into one portal form, as {param value}."
  [person scope]
  (get-in @portal-drafts [(:id person) scope] {}))

(defn stash-portal-draft!
  [person scope params]
  (when (and person scope)
    (swap! portal-drafts update-in [(:id person) scope] merge
           (into {} (filter (fn [[_ v]] (string? v))) (dissoc params :dscope)))))

(defn clear-portal-draft!
  [person scope]
  (when (and person scope)
    (swap! portal-drafts update (:id person) dissoc scope)
    (swap! portal-notes-sent dissoc (:id person))))

(defn render-portal
  ([req] (render-portal req {} 200))
  ([req extra status]
   (let [person (auth/current-person req)
         editing-id (clean-id (get-in req [:params :edit]))]
     (html-response
      status
      (views/portal-page
       (merge {:person person
               :submissions (portal/my-submissions (:id person))
               :editing-id editing-id
               :message (when (get-in req [:params :saved]) "Saved.")
                 ;; Same three-layer prefill as the public page, and for the same
                 ;; reason: half-typed text is the speaker's, not ours to drop.
               :profile-values (merge (portal-draft-for person "profile")
                                      (:profile-values extra))
               :values (merge (when editing-id (portal-draft-for person editing-id))
                              (:values extra))}
              (dissoc extra :profile-values :values)))))))

(defn handle-portal [req] (render-portal req))

(defn- own-submission
  "The submission, but ONLY if the signed-in person is a speaker on it. A
   speaker must never be able to reach someone else's talk by guessing an id."
  [req submission-id]
  (let [person (auth/current-person req)
        row (when submission-id (store/submission-by-id submission-id))]
    (when (and person row
               (some #(= (:id person) (:person-id %)) (:speakers row)))
      row)))

(defn handle-portal-answers
  "A speaker editing their own talk. Accepted speakers can still edit (swyx)."
  [req]
  (let [sid (clean-id (get-in req [:path-params :submission-id]))]
    (if-let [row (own-submission req sid)]
      (let [person (auth/current-person req)
            result (portal/update-answers! (:id row) (:params req) "speaker")]
        (if (:ok result)
          (do (clear-portal-draft! person (:id row))
              (see-other "/portal?saved=1"))
          (do (stash-portal-draft! person (:id row) (:params req))
              (render-portal req {:editing-id (:id row)
                                  :errors (:errors result)
                                  :values (:params req)}
                             422))))
      (not-found-page "that submission"))))

(defn handle-portal-profile [req]
  (let [person (auth/current-person req)
        result (portal/update-profile! (:id person) (:params req) "speaker")]
    (if (:ok result)
      (do (clear-portal-draft! person "profile")
          (see-other "/portal?saved=1"))
      (do (stash-portal-draft! person "profile" (:params req))
          (render-portal req {:profile-errors (:errors result)
                              :profile-values (:profile result)}
                         422)))))

(defn handle-portal-task [req]
  (let [sid (clean-id (get-in req [:path-params :submission-id]))]
    (if-let [row (own-submission req sid)]
      (do (portal/complete-task! (:id row)
                                 (get-in req [:params :key])
                                 (get-in req [:params :value])
                                 "speaker")
          (see-other "/portal"))
      (not-found-page "that submission"))))

;; --- Inform Speakers --------------------------------------------------------

(defn handle-comms [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response
       (views/comms-page event
                         {:person (auth/current-person req)
                          :history (mail/history (:id event))
                          :mail-configured? (mail/configured?)
                          :mail-status (mail/status-line)
                          :templates (keep (fn [st]
                                             (when-let [t (inform/load-template st)]
                                               [st t]))
                                           inform/informable-statuses)}))
      (not-found-page slug))))

;; --- Quick capture ----------------------------------------------------------

(defn handle-capture-page [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response (views/capture-page event {:person (auth/current-person req)
                                                :values {}}))
      (not-found-page slug))))

(defn handle-capture [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [params (:params req)]
        (if (str/blank? (str (:captured-text params)))
          (html-response 422 (views/capture-page
                              event
                              {:person (auth/current-person req)
                               :values params
                               :errors {:captured-text ["Paste something — that is the whole point."]}}))
          (let [sub (submissions/capture! event params
                                          (:email (auth/current-person req)))]
            (see-other (str "/events/" slug "/submissions/" (:id sub))))))
      (not-found-page slug))))

(defn handle-inform-page [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response
       (views/inform-page
        event
        {:groups (for [{:keys [status rows]} (inform/pending-by-status (:id event))]
                   {:status status
                    :rows (mapv (fn [r] {:submission r :letter (inform/letter-for event r)})
                                rows)})
         :informed (inform/informed (:id event))
         :person (auth/current-person req)
         :dev? (auth/dev?)
         :mail-configured? (mail/configured?)
         :mail-status (mail/status-line)}))
      (not-found-page slug))))

(defn handle-inform-one [req]
  (let [sid (clean-id (get-in req [:path-params :submission-id]))
        row (when sid (store/submission-by-id sid))
        event (when row (events/event-by-id (:event-id row)))]
    (if-not (and row event)
      (not-found-page "that submission")
      (do
        (try (inform/inform! event row (:email (auth/current-person req)))
             (catch clojure.lang.ExceptionInfo e
               (if (= :not-informable (:type (ex-data e)))
                 (log/warn :inform-rejected :status (:status row))
                 (throw e))))
        (push-board-updates! event sid)
        (see-other (str "/events/" (:slug event) "/inform"))))))

(defn handle-inform-all [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [status (not-blank (get-in req [:params :status]))
            n (inform/inform-all! event (:email (auth/current-person req)) status)]
        (log/info :informed-batch :slug slug :status status :count n)
        (see-other (str "/events/" (:slug event) "/inform")))
      (not-found-page slug))))

;; --- Static exports (public) ------------------------------------------------

(defn- with-etag
  "Content-hash ETag + conditional GET on every open-data response.

   A scraper polls. The honest thing to hand someone who polls is a cheap way to
   learn NOTHING CHANGED — otherwise a well-behaved consumer re-downloads the
   whole program every minute and an ill-behaved one gets rate-limited for doing
   the same thing. Hashing the rendered body rather than `scheduleVersion` means
   the tag is right even for responses that version does not describe (a
   speakers list, the docs page), and it can never claim 'unchanged' about bytes
   that did change.

   A handler that has already set its own ETag keeps it — the ics feed does,
   because RFC 5545 requires a DTSTAMP of NOW in every VEVENT, so its bytes
   differ on every fetch even when the program has not moved. Content-hashing
   that would be technically correct and practically useless: a calendar client
   polling hourly would never once get a 304."
  [req resp]
  (if (not= 200 (:status resp))
    resp
    (let [etag (or (get-in resp [:headers "ETag"])
                   (str "W/\"" (format "%08x" (hash (str (:body resp)))) "\""))]
      (if (= etag (get-in req [:headers "if-none-match"]))
        {:status 304
         :headers (assoc (:headers resp) "ETag" etag "Content-Length" "0")
         :body ""}
        (assoc-in resp [:headers "ETag"] etag)))))

(defn handle-export-sessions [req]
  (with-etag req (with-event req (fn [event] (json-response (exports/sessions-json-data event))))))

(defn handle-export-speakers [req]
  (with-etag req (with-event req (fn [event] (json-response (exports/speakers-json-data event))))))

(defn handle-export-ics [req]
  (with-etag
    req
    (with-event req
      (fn [event]
        (-> (text-response "text/calendar; charset=utf-8" (exports/calendar-ics event))
            (assoc-in [:headers "Content-Disposition"]
                      (str "inline; filename=\"" (:slug event) ".ics\""))
            ;; Version-based, not content-based — see `with-etag` on DTSTAMP.
            (assoc-in [:headers "ETag"]
                      (str "W/\"ics-" (exports/schedule-version (:id event)) "\"")))))))

(defn handle-llms-txt [req]
  (with-etag
    req
    (with-event req
      (fn [event]
        (text-response "text/markdown; charset=utf-8"
                       (exports/llms-txt event (request-host req)))))))

;; --- REST API v1 (token widens, it does not unlock) --------------------------

(defn- api-authed? [req event]
  (exports/valid-token? event (exports/bearer-token req)))

(def ^:private token-hint
  "Authorization: Bearer <token> — mint one on the event's Settings page (API keys).")

(defn- needs-token
  [message]
  (json-response 401 {"error" message "hint" token-hint}))

(defn handle-api-index
  "GET /api/v1/ — the service root. One request teaches a consumer the whole
   surface, including the parts a token would open."
  [req]
  (with-etag req (json-response (exports/api-index (request-host req)))))

(defn handle-api-event
  "GET /api/v1/events/:slug — the discovery document for one conference."
  [req]
  (with-etag req
    (with-event req
      (fn [event]
        (json-response (exports/api-event event (request-host req)
                                          (api-authed? req event)))))))

(defn handle-api-sessions
  "Public callers get the published program. A token additionally allows
   ?status=all or a specific status. Private fields are excluded either way."
  [req]
  (with-etag req
    (with-event req
      (fn [event]
        (let [authed? (api-authed? req event)
              asked (not-blank (get-in req [:params :status]))]
          (cond
            (and asked (not authed?))
            (needs-token "a token is required to filter by status")

            :else
            (json-response
             (exports/api-sessions event
                                   (when authed?
                                     (cond (nil? asked) nil
                                           (= "all" (str/lower-case asked)) :all
                                           :else asked))
                                   authed?))))))))

(defn handle-api-submissions
  "GET /api/v1/events/:slug/submissions — the CFP inbox, every state.

   /sessions is the PROGRAM; /submissions is the FUNNEL. They can return the
   same rows and still be two different questions, and a consumer that has to
   remember `?status=all` to ask the second one will forget."
  [req]
  (with-etag req
    (with-event req
      (fn [event]
        (if-not (api-authed? req event)
          (needs-token "a token is required to read submissions")
          (let [asked (not-blank (get-in req [:params :status]))]
            (json-response
             (exports/api-sessions event
                                   (if (and asked (not= "all" (str/lower-case asked)))
                                     asked
                                     :all)
                                   true))))))))

(defn handle-api-speakers [req]
  (with-etag req
    (with-event req
      (fn [event]
        (let [authed? (api-authed? req event)
              asked (not-blank (get-in req [:params :status]))]
          (if (and asked (not authed?))
            (needs-token "a token is required to list speakers who are not on the program")
            (json-response (exports/api-speakers event (boolean (and authed? asked))))))))))

(defn handle-api-speaker
  "GET /api/v1/events/:slug/speakers/:person-id — one human, by the id that does
   not change when they change employer."
  [req]
  (with-etag req
    (with-event req
      (fn [event]
        (let [pid (clean-id (get-in req [:path-params :person-id]))
              authed? (api-authed? req event)
              row (when pid (exports/api-speaker event pid authed?))]
          (if row
            (json-response row)
            ;; 404 whether they do not exist or are not published: a public
            ;; caller must not be able to probe for unannounced speakers.
            (json-response 404 {"error" "no such speaker on this event"})))))))

(defn handle-api-schedule
  "GET /api/v1/events/:slug/schedule — the grid, joined by id."
  [req]
  (with-etag req
    (with-event req
      (fn [event]
        (json-response (exports/api-schedule event (api-authed? req event)))))))

(defn handle-api-rooms [req]
  (with-etag req (with-event req (fn [event] (json-response (exports/api-rooms event))))))

(defn handle-api-changes
  "GET /api/v1/events/:slug/changes?since=N — ids only, monotonic seq."
  [req]
  (with-etag req
    (with-event req
      (fn [event]
        (if-not (api-authed? req event)
          (needs-token "a token is required for the change feed")
          (let [raw (not-blank (get-in req [:params :since]))
                since (try (Long/parseLong (str raw)) (catch Exception _ 0))]
            (json-response (exports/api-changes event since))))))))

(defn handle-api-docs
  "GET /api/v1/events/:slug/docs — the human page, deliberately UNDER /api/v1 so
   it inherits the same public reachability as the endpoints it documents. A
   reference an integrator has to log in to read is a reference nobody reads."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (with-etag req
        (-> (html-response (views/api-docs-page (request-host req) event))
            (assoc-in [:headers "Access-Control-Allow-Origin"] "*")))
      (json-response 404 {"error" "no such event" "slug" slug}))))

(defn handle-api-submission
  "One submission in full. Unpublished rows require a token."
  [req]
  (with-etag req
    (with-event req
      (fn [event]
        (let [sid (clean-id (get-in req [:path-params :submission-id]))
              row (when sid (store/submission-by-id sid))]
          (cond
            (or (nil? row) (not= (:id event) (:event-id row)))
            (json-response 404 {"error" "no such submission"})

            (and (not (exports/published? row)) (not (api-authed? req event)))
            (needs-token "a token is required for unpublished submissions")

            :else (json-response (exports/api-session event row))))))))

;; --- Settings ---------------------------------------------------------------

(defn- settings-response
  "`extra` carries the one-shot state of a just-POSTed control — the freshly
   minted key we will never show again, or which key is asking to be confirmed.
   It is passed through the render and never stored, because neither fact
   outlives the response."
  ([req event] (settings-response req event 200 nil))
  ([req event status] (settings-response req event status nil))
  ([req event status extra]
   (let [person (auth/current-person req)]
     (html-response
      status
      (views/settings-page (request-host req) event
                           (merge {:person person
                                   :webhooks (exports/webhooks-for event)
                                   :api-keys (exports/api-keys-for event)
                                   :deliveries @store/deliveries
                                   :slack-groups sinks/slack-event-groups
                                   :notice (notices/notice-for (:id event) (:id person))}
                                  extra))))))

(defn handle-settings [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (settings-response req event)
      (not-found-page slug))))

;; --- API keys ---------------------------------------------------------------

(defn handle-api-key-create
  "Mint a key and re-render Settings with the material visible ONCE. Not a 303:
   a redirect would drop the only copy of the secret on the floor."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [row (exports/create-api-key! event
                                         (get-in req [:params :label])
                                         (:email (auth/current-person req)))]
        (settings-response req (events/event-by-slug slug) 200
                           {:new-key row}))
      (not-found-page slug))))

(defn handle-api-key-revoke
  "Two steps, both server-rendered. The first POST carries no `confirm` and only
   re-renders the page with that row asking; the second does the work. No
   confirm() — a modal dialog blocks the SSE stream (global CLAUDE.md, NEVER #2)
   and, more simply, is state living in the browser."
  [req]
  (let [slug (get-in req [:path-params :slug])
        key-id (clean-id (get-in req [:params :id]))]
    (if-let [event (events/event-by-slug slug)]
      (cond
        (nil? (get-in event [:settings :api-keys key-id]))
        (settings-response req event 404)

        (not-blank (get-in req [:params :confirm]))
        (do (exports/revoke-api-key! event key-id (:email (auth/current-person req)))
            (see-other (str "/events/" slug "/settings")))

        :else
        (settings-response req event 200 {:confirming-key key-id}))
      (not-found-page slug))))

;; --- Slack ------------------------------------------------------------------
;;
;; Config is stored on the EVENT (like Airtable), as an appended event.updated,
;; so it travels with the conference and shows up in the log like everything
;; else. The stored shape is {:webhook-url .. :groups [..] :events [..]}: the
;; GROUPS are what the organizer chose and what the form re-renders from, and
;; :events is the flattened type list the sink fan-out filters on. Both are
;; written at save time rather than derived at delivery time, because the sink
;; runs inside the write path and should not be re-deriving vocabulary there.

(defn- update-event-settings!
  "Append one event.updated carrying the whole new settings map."
  [event actor changed f]
  (store/append!
   {:type "event.updated" :actor actor :event-id (:id event)
    :payload {:id (:id event) :slug (:slug event)
              :changed [changed] :before {}
              :changes {:settings (f (:settings event))}}}))

(defn handle-slack-set [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [person (auth/current-person req)
            url (not-blank (get-in req [:params :webhook-url]))
            ;; A checkbox group arrives as a string for one box and a vector for
            ;; several — the classic Ring form-encoding trap.
            raw (get-in req [:params :groups])
            groups (vec (cond (nil? raw) []
                              (sequential? raw) (map str raw)
                              :else [(str raw)]))]
        (cond
          (nil? url)
          (do (notices/set-notice!
               (:id event) (:id person)
               {:kind :error
                :message "Paste the incoming-webhook URL Slack gave you (it starts https://hooks.slack.com/services/)."})
              (settings-response req event 422))

          (empty? groups)
          (do (notices/set-notice!
               (:id event) (:id person)
               {:kind :error
                :message "Pick at least one moment to post about — a Slack hook that posts about nothing is just a stored secret."})
              (settings-response req event 422))

          :else
          (do (update-event-settings!
               event (:email person) "slack"
               (fn [settings]
                 (assoc settings :slack {:webhook-url url
                                         :groups groups
                                         :events (sinks/types-for-groups groups)})))
              (notices/set-notice!
               (:id event) (:id person)
               {:kind :ok :message "Slack saved. Send a test message to be sure."})
              (see-other (str "/events/" slug "/settings")))))
      (not-found-page slug))))

(defn handle-slack-remove [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [person (auth/current-person req)]
        (update-event-settings! event (:email person) "slack" #(dissoc % :slack))
        (notices/clear-notice! (:id event) (:id person))
        (see-other (str "/events/" slug "/settings")))
      (not-found-page slug))))

(defn handle-slack-test
  "Post a real message to the real channel and say what came back.

   Synchronous on purpose: the whole point of this button is that the organizer
   finds out NOW whether the hook works, which is the opposite of the
   fire-and-forget contract every other delivery has."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [person (auth/current-person req)
            url (get-in event [:settings :slack :webhook-url])
            result (sinks/send-test-message! event url (or (:name person) (:email person)))]
        (notices/set-notice! (:id event) (:id person)
                             {:kind (if (:ok result) :ok :error)
                              :message (:message result)})
        (log/info :slack-test :slug slug :ok (:ok result))
        (if (:ok result)
          (see-other (str "/events/" slug "/settings"))
          (settings-response req event 422)))
      (not-found-page slug))))

(defn handle-webhook-add [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (do
        (exports/register-webhook! event
                                   (get-in req [:params :url])
                                   (map str/trim (str/split (str (get-in req [:params :types])) #","))
                                   (:email (auth/current-person req)))
        (see-other (str "/events/" slug "/settings")))
      (not-found-page slug))))

(defn handle-airtable-set [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (do
        (when-let [base-id (not-blank (get-in req [:params :base-id]))]
          (store/append!
           {:type "event.updated" :actor (:email (auth/current-person req))
            :event-id (:id event)
            :payload {:id (:id event) :slug slug
                      :changed ["airtable"]
                      :before {}
                      :changes {:settings
                                (assoc (:settings event)
                                       :airtable
                                       {:base-id base-id
                                        :table (or (not-blank (get-in req [:params :table]))
                                                   "Submissions")
                                        :token (get-in req [:params :token])})}}}))
        (see-other (str "/events/" slug "/settings")))
      (not-found-page slug))))

(defn handle-airtable-remove [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (do (store/append!
           {:type "event.updated" :actor (:email (auth/current-person req))
            :event-id (:id event)
            :payload {:id (:id event) :slug slug
                      :changed ["airtable"]
                      :before {}
                      :changes {:settings (dissoc (:settings event) :airtable)}}})
          (see-other (str "/events/" slug "/settings")))
      (not-found-page slug))))

(defn handle-webhook-remove [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (do (exports/remove-webhook! event (get-in req [:params :id])
                                   (:email (auth/current-person req)))
          (see-other (str "/events/" slug "/settings")))
      (not-found-page slug))))

;; --- The form builder -------------------------------------------------------
;;
;; Same shape as every other mutation in this file: mutate → push everyone
;; watching → 303 the caller. The browser POSTs and forgets; the server decides
;; what the page says. Nothing on this page runs a line of JavaScript.

(defn- form-model
  "Everything `views/form-builder-page` needs. `extra` carries the state of a
   rejected add/edit so a bad field comes back filled in with its messages."
  ([req event] (form-model req event nil))
  ([req event extra]
   (let [fields (forms/fields-for-event (:id event))
         person (auth/current-person req)
         form (forms/form-for-event (:id event))]
     (merge {:fields fields
             :person person
             :editing (when-let [e (not-blank (get-in req [:params :edit]))]
                        (forms/find-field fields e))
             :confirming (forms/confirming-field (:id person) (:id form))
             :reviewed? (forms/reviewed? (:id event))
             :submission-count (submissions/count-for-event (:id event))}
            extra))))

(defn push-form-updates!
  "Repaint the field list and the preview for everyone else on this page.

   The list goes out PER VIEWER because the two-step retire prompt is one
   person's business — broadcasting one render would ask the whole committee to
   confirm somebody else's decision. The preview is impersonal: it is the public
   page, and the public page looks the same to everyone."
  [event]
  (let [fields (forms/fields-for-event (:id event))
        form-id (:id (forms/form-for-event (:id event)))]
    ;; One interleaved region now (#fb-grid) — questions and their preview are
    ;; the same grid, so both update together. Per-viewer for the retire-prompt.
    (sse/push-personal-fragment!
     (:id event) "#fb-grid"
     (fn [person-id]
       (views/form-grid-region event {:fields fields
                                      :confirming (forms/confirming-field person-id form-id)})))))

(defn handle-form-builder [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response
       (views/form-builder-page
        event (form-model req event
                          {:saved-toast
                           (cond (get-in req [:params :saved]) "Saved."
                                 (get-in req [:params :added]) "Question added.")})))
      (not-found-page slug))))

(defn- with-form
  "Resolve the event, run `f`, push, and send the organizer back to the editor.
   `f` may return a response to short-circuit (a rejected form re-render)."
  [req f]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [resp (try (f event)
                      (catch clojure.lang.ExceptionInfo e
                        (if (= :locked-field (:type (ex-data e)))
                          {:status 409
                           :headers {"Content-Type" "text/html; charset=utf-8"}
                           :body (views/page-shell
                                  "Locked field"
                                  [:div.ui.warning.message
                                   [:div.header "That field can't be removed"]
                                   [:p "Talk title, abstract and the speaker block are the "
                                    "spine of a call for speakers — a CFP without them "
                                    "can't describe a talk."]]
                                  [:a.ui.basic.button {:href (str "/events/" slug "/form")}
                                   "Back to the form"])}
                          (throw e))))]
        (push-form-updates! event)
        (or resp (see-other (str "/events/" slug "/form"))))
      (not-found-page slug))))

(defn- field-values
  "What the organizer typed, for a re-render. `max-length` keeps its RAW string
   so a rejected \"lots\" comes back as \"lots\", not as a parse marker."
  [req attrs]
  (assoc attrs :max-length (get-in req [:params :max-length])))

(defn handle-form-add [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [attrs (forms/parse-field-params (:params req))
            errors (forms/field-validation-errors attrs {:new? true})]
        (if errors
          (do (log/info :form-field-rejected :slug slug :fields (vec (keys errors)))
              (html-response
               422
               (views/form-builder-page
                event (form-model req event
                                  {:add-form {:values (field-values req attrs)
                                              :errors errors}}))))
          (do (forms/add-field! event attrs (:email (auth/current-person req)))
              (push-form-updates! event)
              (see-other (str "/events/" slug "/form?added=1")))))
      (not-found-page slug))))

(defn handle-form-update [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [field-id (not-blank (get-in req [:params :field-id]))
            existing (when field-id
                       (forms/find-field (forms/fields-for-event (:id event)) field-id))]
        (if-not existing
          (see-other (str "/events/" slug "/form?saved=1"))
          ;; The field's OWN type drives validation — an edit never posts one,
          ;; because a type is fixed at birth.
          (let [attrs (assoc (forms/parse-field-params (:params req))
                             :type (forms/field-type existing))
                errors (forms/field-validation-errors attrs)]
            (if errors
              (html-response
               422
               (views/form-builder-page
                event (form-model req event
                                  {:editing existing
                                   :edit-form {:values (field-values req attrs)
                                               :errors errors}})))
              (do (forms/update-field! event field-id attrs
                                       (:email (auth/current-person req)))
                  (push-form-updates! event)
                  (see-other (str "/events/" slug "/form?saved=1")))))))
      (not-found-page slug))))

(defn handle-form-move [req]
  (with-form req
    (fn [event]
      (when-let [field-id (not-blank (get-in req [:params :field-id]))]
        (forms/move-field! event field-id
                           (if (= "up" (get-in req [:params :direction])) "up" "down")
                           (:email (auth/current-person req))))
      nil)))

;; Step one of the two-step delete: record that THIS person is being asked.
;; Server state, server-rendered — never confirm(), which would block the SSE
;; stream the rest of this page depends on.
(defn handle-form-retire-ask [req]
  (with-form req
    (fn [event]
      (forms/ask-confirm-retire! (:id (auth/current-person req))
                                 (:id (forms/form-for-event (:id event)))
                                 (not-blank (get-in req [:params :field-id])))
      nil)))

(defn handle-form-retire-cancel [req]
  (with-form req
    (fn [event]
      (forms/clear-confirm-retire! (:id (auth/current-person req))
                                   (:id (forms/form-for-event (:id event))))
      nil)))

(defn handle-form-retire [req]
  (with-form req
    (fn [event]
      (let [form-id (:id (forms/form-for-event (:id event)))]
        (try
          (when-let [field-id (not-blank (get-in req [:params :field-id]))]
            (forms/retire-field! event field-id (:email (auth/current-person req))))
          (finally
            ;; The question has been answered either way — never leave a stale
            ;; prompt on a screen.
            (forms/clear-confirm-retire! (:id (auth/current-person req)) form-id))))
      nil)))

(defn handle-form-restore [req]
  (with-form req
    (fn [event]
      (when-let [field-id (not-blank (get-in req [:params :field-id]))]
        (forms/restore-field! event field-id (:email (auth/current-person req))))
      nil)))

(defn handle-form-preview
  "POST /api/events/:slug/form/preview — the living 'What speakers see' pane.

   Stateless: reads the bound edit/add signals, renders the public form with
   the in-progress typing applied on top of the saved fields, and pushes
   #form-preview down THIS viewer's SSE stream only — half-typed edits are one
   person's business (saves broadcast to everyone via push-form-updates!). The
   POST answers 204; the stream is the answer.

   `?field-id=X` is the edit panel (signals fbe*); the existing field's own
   type drives the render, because a type is fixed at birth. `?mode=add` is
   the add panel (signals fba*): a non-blank label shows as a ghost row
   appended to the preview before the field exists."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [person (auth/current-person req)
            sigs (datastar-signals req)
            field-id (not-blank (get-in req [:params :field-id]))
            add? (some? (get-in req [:params :mode]))
            fields (forms/fields-for-event (:id event))
            typed (fn [pfx]
                    (let [sig (fn [suffix] (get sigs (keyword (str pfx suffix))))
                          attrs (forms/parse-field-params
                                 {:label (sig "label")
                                  :type (sig "type")
                                  :help (sig "help")
                                  :max-length (str (or (sig "max") ""))
                                  :required (when (true? (sig "req")) "on")
                                  :private (when (true? (sig "priv")) "on")
                                  :widget (when (true? (sig "widget")) "radio")
                                  :options (sig "opts")})]
                      ;; A half-typed "20x" limit must not crash the render.
                      (cond-> attrs
                        (keyword? (:max-length attrs)) (assoc :max-length nil))))]
        (if-not person
          (log/warn :form-preview-skipped :why :no-session)
          (let [existing (when field-id (forms/find-field fields field-id))
                attrs (cond
                        existing (assoc (typed "fbe") :type (forms/field-type existing))
                        add? (typed "fba"))
                ghost (when (and add? (:label attrs))
                        (assoc attrs :id "ghost-preview"))
                fields' (if existing
                          (mapv #(if (= (forms/field-id %) field-id) (merge % attrs) %)
                                fields)
                          fields)
                reach (sse/person-connection-count (:id event) (:id person))]
            (when (zero? reach)
              (log/warn :form-preview-push-no-subscriber
                        :event-id (:id event) :person-id (:id person)
                        :msg "nothing is listening — the preview cannot move"))
            (sse/push-to-person! (:id event) (:id person) "#fb-grid"
                                 #(views/form-grid-region event {:fields fields'
                                                                 :confirming nil
                                                                 :ghost ghost}))
            (log/debug :form-preview-pushed :reach reach :edit field-id :add add?)))
        {:status 204 :headers {} :body ""})
      (not-found-page slug))))

(defn handle-form-reviewed [req]
  (with-form req
    (fn [event]
      (forms/mark-reviewed! event (:email (auth/current-person req)))
      ;; Step 2 done → walk straight to step 3's own page.
      (see-other (str "/events/" (:slug event) "/committee")))))

;; --- The schedule builder ---------------------------------------------------

(defn- schedule-model [req event]
  (let [days (schedule/event-days event)
        day (or (not-blank (get-in req [:params :day])) (first days))]
    {:day day
     :stats (schedule/stats event)
     :conflicts (schedule/conflicts event)
     :conflicted-ids (schedule/conflicted-submission-ids event)
     ;; Withheld = published-but-conflicted: what the public surfaces are
     ;; holding back (Gene's ruling 2026-08-09 — withhold, loudly).
     :withheld-count (let [conflicted (schedule/conflicted-submission-ids event)]
                       (count (filter #(and (contains? conflicted (:id %))
                                            (exports/published? %))
                                      (store/submissions-for-event (:id event)))))
     :rooms (store/rooms-for-event (:id event))
     :placed (schedule/placements (:id event))
     :blocks (store/blocks-for-event (:id event))
     :trayed (schedule/tray (:id event))
     :locked? (schedule/locked? event)
     :lock-version (schedule/lock-label event)
     :person (auth/current-person req)}))

(defn handle-schedule [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response (views/schedule-page event (schedule-model req event)))
      (not-found-page slug))))

(defn push-schedule!
  "Repaint the grid and the status bar for everyone else watching this event.
   Impersonal — unlike a board row, the grid looks the same to every organizer."
  [req event]
  (let [model (schedule-model req event)]
    (sse/push-fragment! (:id event) "#schedule-status"
                        (fn [] (views/schedule-status-bar (:stats model))))))

(defn- with-schedule
  "Every schedule mutation shares the same shape: resolve the event, refuse if
   locked (409 — the caller asked for something the current state forbids),
   act, push, and send the organizer back to the day they were looking at."
  [req f]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (try
        (f event)
        (push-schedule! req event)
        (see-other (str "/events/" slug "/schedule"
                        (when-let [d (not-blank (get-in req [:params :day]))]
                          (str "?day=" d))))
        (catch clojure.lang.ExceptionInfo e
          (if (= :schedule-locked (:type (ex-data e)))
            {:status 409
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body (views/page-shell
                    "Schedule locked"
                    [:div.ui.warning.message
                     [:div.header "The schedule is locked"]
                     [:p "It was frozen as " (or (:version (ex-data e)) "a version")
                      ". Unlock it to keep editing."]]
                    [:a.ui.basic.button {:href (str "/events/" slug "/schedule")}
                     "Back to the schedule"])}
            (throw e))))
      (not-found-page slug))))

(defn handle-schedule-place [req]
  (with-schedule req
    (fn [event]
      (schedule/place! event
                       (get-in req [:params :submission-id])
                       {:day (get-in req [:params :day])
                        :start (get-in req [:params :start])
                        :duration (get-in req [:params :duration])
                        :room-id (not-blank (get-in req [:params :room-id]))}
                       (:email (auth/current-person req))))))

(defn handle-schedule-clear [req]
  (with-schedule req
    (fn [event]
      (schedule/clear-slot! event (get-in req [:params :submission-id])
                            (:email (auth/current-person req))))))

(defn handle-room-add [req]
  (with-schedule req
    (fn [event]
      (schedule/add-room! event (get-in req [:params :name])
                          (:email (auth/current-person req))))))

(defn handle-room-remove [req]
  (with-schedule req
    (fn [event]
      (schedule/remove-room! event (get-in req [:params :room-id])
                             (:email (auth/current-person req))))))

(defn handle-block-add [req]
  (with-schedule req
    (fn [event]
      (schedule/add-block! event
                           {:day (get-in req [:params :day])
                            :start (get-in req [:params :start])
                            :duration (get-in req [:params :duration])
                            :room-id (not-blank (get-in req [:params :room-id]))
                            :label (get-in req [:params :label])}
                           (:email (auth/current-person req))))))

(defn handle-block-remove [req]
  (with-schedule req
    (fn [event]
      (schedule/remove-block! event (get-in req [:params :block-id])
                              (:email (auth/current-person req))))))

(defn handle-schedule-lock [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (do (schedule/lock! event (:email (auth/current-person req)))
          (see-other (str "/events/" slug "/schedule")))
      (not-found-page slug))))

(defn handle-schedule-unlock [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (do (schedule/unlock! event (:email (auth/current-person req)))
          (see-other (str "/events/" slug "/schedule")))
      (not-found-page slug))))

;; --- The public agenda ------------------------------------------------------

(defn handle-agenda
  "Public. Same publication gate as the exports."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response
       (views/agenda-page event (schedule/agenda event)
                          (not-blank (get-in req [:params :day]))))
      (html-response 404 (views/page-shell
                          "No such agenda"
                          [:div.ui.warning.message
                           [:div.header "Nothing here"]
                           [:p (str "There is no event at /agenda/" slug ".")]])))))

;; --- The replay simulator ---------------------------------------------------

(defn- replay-model [req event]
  (let [run (replay/state-for (:id event))]
    {:person (auth/current-person req)
     :progress (replay/progress (:id event))
     :corpus-available? (replay/corpus-available?)
     :speeds replay/speeds
     :speed (or (:speed run) replay/default-speed)
     :running? (= :playing (:status run))}))

(defn handle-replay-page [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (html-response (views/replay-page event (replay-model req event)))
      (not-found-page slug))))

(defn handle-replay-start-demo
  "Creates a FRESH event to replay into. Never touches an existing one — that
   guard is the difference between a demo and a hazard."
  [req]
  (let [person (auth/current-person req)
        event (replay/create-demo-event! (or (:email person) "replay") person)]
    (see-other (str "/events/" (:slug event) "/replay"))))

(defn- push-replay! [event]
  (sse/push-fragment! (:id event) "#replay-progress"
                      (fn [] (views/replay-progress-bar
                              event (replay/progress (:id event)))))
  ;; The board is watching too — repaint its headline as rows land.
  (sse/push-fragment! (:id event) "#coverage-bar"
                      (fn [] (views/coverage-bar
                              event (reviews/coverage
                                     (:id event) (coverage-target-for (:id event)))))))

(defn- with-replay [req f]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (try
        (f event)
        (see-other (str "/events/" slug "/replay"))
        (catch clojure.lang.ExceptionInfo e
          (if (= :not-a-replay-event (:type (ex-data e)))
            {:status 409
             :headers {"Content-Type" "text/html; charset=utf-8"}
             :body (views/page-shell
                    "Not a replay event"
                    [:div.ui.warning.message
                     [:div.header "This event is not a replay target"]
                     [:p "The simulator only plays into an event it created, so a "
                      "demo can never pollute a real conference's data."]]
                    [:a.ui.basic.button {:href (str "/events/" slug)} "Back to the event"])}
            (throw e))))
      (not-found-page slug))))

(defn handle-replay-play [req]
  (with-replay req
    (fn [event]
      (let [speed (or (try (Integer/parseInt (str (get-in req [:params :speed])))
                           (catch Exception _ nil))
                      replay/default-speed)]
        (replay/start! event speed #(push-replay! event))))))

(defn handle-replay-pause [req]
  (with-replay req (fn [event] (replay/pause! event))))

(defn handle-replay-skip [req]
  (with-replay req
    (fn [event]
      (replay/skip-to-end! event)
      (push-replay! event))))

(defn handle-submissions-list
  "RETIRED page (Gene, 2026-08-10): the board strictly superseded it — same
   rows, more power — and two near-identical tables force every reviewer to
   ask which one is real. The route 303s so every old link keeps working."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (see-other (str "/events/" slug "/board"))
      (not-found-page slug))))

(defn handle-event-log
  "This event's slice of the store, rendered as readable history. Nearly free:
   it is a filter over the same rows the whole app is folded from."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (let [tt (time-travel-context req event (str "/events/" slug "/log"))]
        (with-as-of (:cutoff tt)
          (let [past-event (or (events/event-by-slug slug) event)]
            (html-response (views/log-page past-event
                                           (events/log-for-event (:id past-event))
                                           (auth/current-person req)
                                           tt)))))
      (not-found-page slug))))

(defn handle-person-detail
  "One person, in the context of one event."
  [req]
  (let [slug (get-in req [:path-params :slug])
        person-id (clean-id (get-in req [:path-params :person-id]))
        event (events/event-by-slug slug)]
    (cond
      (nil? event) (not-found-page slug)
      (nil? person-id) (not-found-page (str slug "/people/…"))
      :else
      (if-let [detail (committees/person-detail (:id event) person-id)]
        (html-response (views/person-page
                        event
                        (assoc detail
                               :viewer (auth/current-person req)
                               :review-summary
                               (reviews/reviewer-summary (:id event) person-id))))
        (html-response 404 (views/page-shell
                            "Person not found"
                            [:div.ui.warning.message
                             [:div.header "No such person"]
                             [:p "We don't have anyone with that id."]]
                            [:a.ui.basic.button {:href (str "/events/" slug)}
                             "Back to the event"]))))))

(defn handle-add-member
  "Plain form POST. Valid → 303 back to the event dashboard.
   Invalid / already a member → 422 re-render of the dashboard with the message
   next to the add form. All validation is server-side."
  [req]
  (let [committee-id (clean-id (get-in req [:path-params :committee-id]))
        committee (when committee-id (committees/committee-by-id committee-id))]
    (if-not committee
      (not-found-page (str "committee " (get-in req [:path-params :committee-id])))
      (let [event (events/event-by-slug (:event-slug committee))
            params (:params req)
            draft (committees/parse-member-form params)
            errors (committees/validation-errors draft)
            reject (fn [errs message]
                     (render-committee-page req event
                                            {:values params :errors errs :message message}
                                            422))]
        (if errors
          (do (log/info :member-add-rejected :fields (vec (keys errors)))
              (reject errors nil))
          (try
            (committees/add-member! committee-id draft)
            (see-other (str "/events/" (:event-slug committee) "/committee"))
            (catch clojure.lang.ExceptionInfo e
              (if (= :already-member (:type (ex-data e)))
                (reject nil (str (:email draft) " is already on this committee."))
                (throw e)))))))))

(defn handle-remove-member
  "Delete a membership and 303 back to the dashboard. The person row survives —
   identity persists across committees and events.

   Removing the LAST reviewer is refused with a 422 and a sentence, not a stack
   trace: since authorization became per-event, that click would have locked
   everyone out of the conference for good."
  [req]
  (let [membership-id (clean-id (get-in req [:path-params :membership-id]))
        m (when membership-id (committees/membership-by-id membership-id))]
    (if-not m
      (not-found-page (str "membership " (get-in req [:path-params :membership-id])))
      (try
        (committees/remove-member! membership-id (:email (auth/current-person req)))
        (see-other (str "/events/" (:event-slug m) "/committee"))
        (catch clojure.lang.ExceptionInfo e
          (if (= :last-reviewer (:type (ex-data e)))
            (render-committee-page
             req (events/event-by-slug (:event-slug m))
             {:message (str "You are the last reviewer on this event — removing "
                            "the last one would lock everyone out of it. Add "
                            "someone else to the program committee first.")}
             422)
            (throw e)))))))

;; --- Public CFP -------------------------------------------------------------

(defn- cfp-not-found [slug]
  (html-response 404 (views/page-shell
                      "No such call for speakers"
                      [:div.ui.warning.message
                       [:div.header "Nothing here"]
                       [:p (str "There's no call for speakers at /cfp/" slug ".")]])))

;; --- The public CFP's live lane ---------------------------------------------
;;
;; The create page proved the shape: debounced POST -> server state -> SSE
;; fragment to THIS viewer -> 204. The speaker side needs the same thing with
;; one extra problem the organizer side never had: THERE IS NO ACCOUNT YET.
;; Everything below exists to give an anonymous stranger a stable identity that
;; is theirs alone for the length of a browser session.

(defonce ^{:doc "Per-viewer CFP drafts: {viewer-key {event-id {param value}}}.

  Fed by every debounced keystroke on /cfp/<slug>, read back by the GET, cleared
  on a successful submit. This is why killing the tab mid-abstract costs
  nothing. Deliberately an atom and NOT the event log: an abandoned draft is not
  history, and a stranger who wandered off should leave no trace in the record."}
  cfp-drafts (atom {}))

(defonce ^{:doc "The last note pushed per field, per viewer: {viewer-key {param note}}.
  Kept so a keystroke pushes only the lines that CHANGED — a twenty-question
  form would otherwise repaint twenty fragments three times a second."}
  cfp-notes-sent (atom {}))

(defn cfp-channel
  "The SSE key for one event's public page. A string, and every real event-id is
   a uuid, so a CFP viewer can never land in the board's fan-out (the board
   pushes #board-region to everyone watching the raw event-id)."
  [event-id]
  (str "cfp-" event-id))

(defn- mint-viewer-id
  "The id that stands in for an account the speaker doesn't have yet."
  [req]
  (or (get-in req [:session :viewer-id]) (str (java.util.UUID/randomUUID))))

(defn- with-viewer-session
  "Put `vid` in the response's session so the next request carries it. Merges
   onto the REQUEST's session — dropping it would sign an organizer out just for
   looking at their own public page."
  [response req vid]
  (assoc response :session (assoc (:session req) :viewer-id vid)))

(defn cfp-viewer
  "Who this request is, for draft + push purposes: the signed-in person if there
   is one, else the anonymous session id. Same rule as `sse/viewer-key`, so the
   key a fragment is pushed to is always the key the stream registered under."
  [req]
  (or (:id (auth/current-person req)) (get-in req [:session :viewer-id])))

(def ^:private draft-param?
  "The params worth remembering — the speaker's own answers, nothing else."
  (fn [k] (let [n (name k)]
            (or (str/starts-with? n "answer-")
                (str/starts-with? n "speaker-")))))

(defn cfp-draft-for
  "What this viewer has typed on this event, as {param value}."
  [req event]
  (get-in @cfp-drafts [(cfp-viewer req) (:id event)] {}))

(defn stash-cfp-draft!
  "Remember what is being typed. Blank values are kept (clearing a field is a
   thing the speaker meant), unknown params are not."
  [req event params]
  (when-let [key (cfp-viewer req)]
    (swap! cfp-drafts update-in [key (:id event)] merge
           (into {} (filter (fn [[k v]] (and (draft-param? k) (string? v)))) params))))

(defn clear-cfp-draft!
  "The talk is in the log — its answers must go, or the next submission starts
   pre-filled with the last one.

   The ABOUT YOU block deliberately STAYS. Every event here has a per-person cap
   above one, so 'submit another talk' is a normal thing to do, and making
   someone retype their bio and headshot to do it is the exact behaviour this
   product exists to replace (docs: never type your bio twice). Their answers
   are gone; who they are is not."
  [req event]
  (when-let [key (cfp-viewer req)]
    (swap! cfp-drafts update-in [key (:id event)]
           #(into {} (remove (fn [[k _]] (str/starts-with? (name k) "answer-"))) %))
    (swap! cfp-notes-sent dissoc key)))

(defn cfp-live-notes
  "The live line under each field, computed from what is typed RIGHT NOW:
   {param {:level :warn|:ok :text \"…\"}}.

   Silence is the default. A speaker mid-sentence is not doing anything wrong,
   so a field with nothing to say says nothing — the only lines that appear are
   the two a speaker genuinely wants early: you are past the room we have, and
   that link isn't a link yet. Both are the SAME rules the 422 uses, computed by
   the same code, so the live lane can never promise something submit refuses."
  [form-fields values]
  (into {}
        (concat
         (keep (fn [f]
                 (let [param (keyword (str "answer-" (name (:id f))))
                       v (get values param)
                       cap (forms/effective-cap f)
                       t (name (or (:type f) "text"))]
                   (cond
                     (str/blank? v) nil

                     (and cap (> (count v) cap))
                     [param {:level :warn
                             :text (str (count v) " characters — we have room for "
                                        cap ". Trim it a little.")}]

                     (and (= t "url") (not (re-matches submissions/url-pattern v)))
                     [param {:level :warn
                             :text "That needs to be a full link, starting with https://"}]

                      ;; The last stretch of a long answer is the one place a
                      ;; count helps rather than nags.
                     (and cap (> (count v) (* 0.9 cap)))
                     [param {:level :ok
                             :text (str (- cap (count v)) " characters left")}])))
               (submissions/session-fields form-fields))
         (keep (fn [param]
                 (let [v (get values param)]
                   (when (and (not (str/blank? v))
                              (not (re-matches submissions/url-pattern v)))
                     [param {:level :warn
                             :text "That needs to be a full link, starting with https://"}])))
               [:speaker-headshot-url :speaker-linkedin])
          ;; The Sessionize box takes a bare handle OR a URL (Gene, 2026-08-09),
          ;; so it validates with the import normalizer, not the url pattern.
         (let [v (:speaker-sessionize-url values)]
           (when (and (not (str/blank? v))
                      (nil? (sessionize-import/normalize-profile-url v)))
             [[:speaker-sessionize-url
               {:level :warn
                :text "Paste your Sessionize profile URL — or just your username."}]])))))

(defn cfp-progress
  "How far along this submission is — answered vs asked, over the fields the
   speaker can actually see."
  [form-fields values]
  (let [fields (submissions/session-fields form-fields)
        answered (count (filter (fn [f]
                                  (not (str/blank?
                                        (get values (keyword (str "answer-" (name (:id f))))))))
                                fields))]
    {:answered answered :total (count fields)}))

(defn render-cfp
  "Render the public page for `event`. `extra` carries the state of a rejected
   submission (values + errors) or an import result, so a round-trip never
   costs the speaker their typing.

   Three sources of prefill, weakest first: what we already know about a
   signed-in speaker (never type your bio twice), what they have typed into the
   draft stash this session, and finally whatever this particular round trip is
   carrying (a 422's params, an import result). The strongest wins per field."
  ([req event] (render-cfp req event {} 200))
  ([req event extra status]
   (let [form (events/form-for-event (:id event))
         fields (forms/active-fields (:fields form))
         person (auth/current-person req)
         vid (mint-viewer-id req)
         values (merge (when person (portal/prefill-from-profile person))
                       (cfp-draft-for req event)
                       (:values extra))]
     (-> (html-response
          status
          (views/cfp-page event
                          (merge {:state (submissions/cfp-state event)
                                   ;; RETIRED fields never reach a speaker. The filter
                                   ;; lives at the one seam where the live form is
                                   ;; read, so rendering, parsing and validation can
                                   ;; never disagree about which questions exist.
                                  :form-fields fields
                                  :cap (submissions/submission-cap event)}
                                 extra
                                 {:values values
                                  :notes (cfp-live-notes fields values)
                                  :progress (cfp-progress fields values)
                                   ;; Only ANSWERS count as "we saved your place".
                                   ;; The speaker block survives a submit on
                                   ;; purpose, and a fresh second talk should not
                                   ;; be greeted as a resumed one.
                                  :restored? (some #(str/starts-with? (name %) "answer-")
                                                   (keys (cfp-draft-for req event)))})))
         (with-viewer-session req vid)))))

(defn handle-public-cfp
  "The public call-for-speakers page. No login, no account wall.

   This GET is where an anonymous speaker acquires the only identity they will
   have until they submit: a viewer id in the ring session. Everything
   per-viewer downstream — the draft stash, the SSE registration, the live
   notes — hangs off it."
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [event (events/event-by-slug slug)]
      (render-cfp req event)
      (cfp-not-found slug))))

(defn handle-cfp-stream
  "GET /api/cfp/:slug/stream — the public page's own SSE connection.

   A separate route from /api/sse on purpose: the gate is default-deny and
   /api/sse is organizer-only, so a stranger opening it gets a 302 to /login and
   the page would sit there with a dead stream. /api/cfp/* is already a public
   prefix, so this one is reachable by exactly the people the page is for. It
   delegates to the same `sse/handle-sse`, which registers this connection under
   the viewer key."
  [req]
  (let [slug (get-in req [:path-params :slug])
        event (events/event-by-slug slug)]
    (if-not event
      {:status 404 :headers {"Content-Type" "text/plain"} :body "no such call"}
      (sse/handle-sse (assoc-in req [:params :event-id] (cfp-channel (:id event)))))))

(defn handle-cfp-draft
  "POST /api/cfp/:slug/draft — one debounced keystroke's worth of the form.

   Datastar posts the whole form (contentType 'form'), so the params here are
   spelled exactly like a real submit. The handler stashes them per viewer,
   recomputes the live feedback with the SAME functions the 422 uses, and pushes
   only the lines that changed down THIS viewer's stream. It answers 204: the
   server decides what the screen says, and it says it on the stream."
  [req]
  (let [slug (get-in req [:path-params :slug])
        event (events/event-by-slug slug)]
    (if-not event
      {:status 404 :headers {} :body ""}
      (let [vid (mint-viewer-id req)
            req (assoc-in req [:session :viewer-id] vid)
            key (cfp-viewer req)
            chan (cfp-channel (:id event))
            form (events/form-for-event (:id event))
            fields (forms/active-fields (:fields form))
            _ (stash-cfp-draft! req event (:params req))
            values (cfp-draft-for req event)
            notes (cfp-live-notes fields values)
            was (get @cfp-notes-sent key {})
            reach (sse/person-connection-count chan key)
            ;; Every field that HAD a note and every field that has one now —
            ;; the difference is what has to be repainted, and a note that just
            ;; went away has to be repainted as empty or it lingers forever.
            changed (remove #(= (get was %) (get notes %))
                            (distinct (concat (keys was) (keys notes))))]
        (swap! cfp-notes-sent assoc key notes)
        ;; A 204 that pushed to nobody is the instrument lying. Say so.
        (when (zero? reach)
          (log/warn :cfp-draft-push-no-subscriber
                    :event-id chan :viewer key
                    :registrations (sse/registrations)
                    :msg "nothing is listening on this key — the live lane cannot move"))
        (doseq [param changed]
          (sse/push-to-person! chan key (str "#cfp-note-" (name param))
                               #(views/cfp-note param (get notes param))))
        (sse/push-to-person! chan key "#cfp-draft-status"
                             #(views/cfp-draft-status (cfp-progress fields values) true))
        (log/debug :cfp-draft-stashed :slug slug :viewer key :reach reach
                   :fields (count values) :notes (count notes))
        (-> {:status 204 :headers {} :body ""}
            (with-viewer-session req vid))))))

;; --- The speaker portal's live lane -----------------------------------------
;;
;; The same three moves as the public page, one tier up: the portal's viewer
;; always has an account, so everything keys on person-id and a draft survives a
;; new browser rather than just a refresh. Defined here, after `cfp-live-notes`,
;; because the talk-edit form on the portal is validated by exactly the same
;; rules as the public form — one implementation, or the two surfaces drift.

(def ^:private profile-url-keys [:headshot-url :linkedin-url :website-url])

(defn portal-live-notes
  "Live feedback for one portal form. `scope` is \"profile\" or a submission id."
  [scope values submission]
  (if (= "profile" scope)
    (into {}
          (keep (fn [k]
                  (let [v (get values k)]
                    (when (and (not (str/blank? v))
                               (not (re-matches submissions/url-pattern v)))
                      [k {:level :warn
                          :text "That needs to be a full link, starting with https://"}]))))
          profile-url-keys)
    ;; A talk edit is validated against the SNAPSHOT the speaker answered, not
    ;; today's form — the same rule `edit-form` renders by.
    (cfp-live-notes (:form-snapshot submission) values)))

(defn handle-portal-stream
  "GET /portal/stream — the speaker portal's SSE connection.

   Under /portal on purpose: /api/sse is organizer-only (auth/gate is
   default-deny and a speaker is not on any committee), so a speaker opening it
   is bounced to /portal and the page streams nothing. /portal is already in
   `auth/speaker-prefixes`."
  [req]
  (if-let [person (auth/current-person req)]
    (sse/handle-sse (assoc-in req [:params :event-id] (portal-channel (:id person))))
    {:status 403 :headers {"Content-Type" "text/plain"} :body "sign in first"}))

(defn handle-portal-draft
  "POST /portal/draft — one debounced keystroke from the profile form or from a
   talk-edit form. `dscope` (a hidden field, so it rides along in the form
   serialization) says which, because both can be open at once and a bio must
   never end up in a talk."
  [req]
  (let [person (auth/current-person req)
        scope (not-blank (get-in req [:params :dscope]))]
    (if-not (and person scope)
      {:status 204 :headers {} :body ""}
      (let [submission (when (not= "profile" scope) (own-submission req scope))]
        (if (and (not= "profile" scope) (nil? submission))
          ;; Someone else's talk. Refuse plainly rather than stash it.
          {:status 403 :headers {"Content-Type" "text/plain"} :body "not your talk"}
          (let [chan (portal-channel (:id person))
                _ (stash-portal-draft! person scope (:params req))
                values (portal-draft-for person scope)
                notes (portal-live-notes scope values submission)
                was (get @portal-notes-sent (:id person) {})
                reach (sse/person-connection-count chan (:id person))
                changed (remove #(= (get was %) (get notes %))
                                (distinct (concat (keys was) (keys notes))))]
            (swap! portal-notes-sent assoc (:id person) notes)
            (when (zero? reach)
              (log/warn :portal-draft-push-no-subscriber
                        :event-id chan :person-id (:id person)
                        :registrations (sse/registrations)
                        :msg "nothing is listening on this key — the portal cannot move"))
            (doseq [param changed]
              (sse/push-to-person! chan (:id person) (str "#cfp-note-" (name param))
                                   #(views/cfp-note param (get notes param))))
            (sse/push-to-person! chan (:id person) (str "#portal-status-" scope)
                                 #(views/portal-draft-status scope true))
            (log/debug :portal-draft-stashed :person-id (:id person) :scope scope
                       :reach reach :fields (count values) :notes (count notes))
            {:status 204 :headers {} :body ""}))))))

(defn handle-cfp-import
  "Sessionize import: fetch + parse their public profile, then re-render the
   WHOLE page with the fields filled in. No client JS, no partial update — the
   server decides what the page says."
  [req]
  (let [slug (get-in req [:path-params :slug])
        event (events/event-by-slug slug)]
    (if-not event
      (cfp-not-found slug)
      (let [params (:params req)
            url (:speaker-sessionize-url params)
            result (sessionize-import/import-profile url)]
        (if (:ok result)
          (let [s (:speaker result)
                ;; Imported values are DEFAULTS: anything already typed wins,
                ;; because the speaker is the authority on their own details.
                filled (merge {:speaker-name (:name s)
                               :speaker-title (:tagline s)
                               :speaker-bio (:bio s)
                               :speaker-headshot-url (:headshot-url s)
                               :speaker-linkedin (:linkedin-url s)
                               :speaker-sessionize-url (:url result)}
                              (into {} (remove (comp str/blank? val)) params))]
            (log/info :cfp-import-ok :slug slug :name (:name s))
            ;; An import IS typing, as far as the draft stash is concerned: a
            ;; refresh after importing must not throw the imported bio away.
            (stash-cfp-draft! req event filled)
            (render-cfp req event
                        {:values filled
                         :import-message (str "Imported from Sessionize: " (:name s)
                                              ". Check it over — you can edit anything below.")}
                        200))
          (do (log/info :cfp-import-failed :slug slug :error (:error result))
              (render-cfp req event
                          {:values params :import-message (:message result)}
                          200)))))))

(defn handle-cfp-import-live
  "POST /api/cfp/:slug/import-live — the no-reload Sessionize import.

   The game-engine version of `handle-cfp-import`: the profile input is part
   of the main form, so its value is already in this viewer's draft stash by
   the time the button is pressed — the request carries NOTHING. The handler
   reads its own state, imports, merges (typed wins over imported, same rule
   as the full-page path), stashes, and morphs #cfp-about-you down this
   viewer's stream. 204 either way; failures speak through the same block."
  [req]
  (let [slug (get-in req [:path-params :slug])
        event (events/event-by-slug slug)]
    (if-not event
      {:status 404 :headers {} :body ""}
      (let [vid (mint-viewer-id req)
            req (assoc-in req [:session :viewer-id] vid)
            key (cfp-viewer req)
            chan (cfp-channel (:id event))
            form (events/form-for-event (:id event))
            fields (forms/active-fields (:fields form))
            draft (cfp-draft-for req event)
            url (:speaker-sessionize-url draft)
            result (sessionize-import/import-profile url)
            msg (if (:ok result)
                  (str "Imported from Sessionize: "
                       (get-in result [:speaker :name])
                       ". Check it over — you can edit anything below.")
                  (:message result))]
        (when (:ok result)
          (let [s (:speaker result)
                filled (merge {:speaker-name (:name s)
                               :speaker-title (:tagline s)
                               :speaker-bio (:bio s)
                               :speaker-headshot-url (:headshot-url s)
                               :speaker-linkedin (:linkedin-url s)
                               :speaker-sessionize-url (:url result)}
                              (into {} (remove (comp str/blank? val)) draft))]
            (log/info :cfp-import-live-ok :slug slug :name (:name s))
            (stash-cfp-draft! req event filled)))
        (let [values (cfp-draft-for req event)
              notes (cfp-live-notes fields values)
              reach (sse/person-connection-count chan key)]
          ;; A 204 that pushed to nobody is the instrument lying. Say so.
          (when (zero? reach)
            (log/warn :cfp-import-live-push-no-subscriber
                      :event-id chan :viewer key
                      :registrations (sse/registrations)))
          (sse/push-to-person! chan key "#cfp-about-you"
                               #(views/cfp-about-you event values {} notes msg)))
        (-> {:status 204 :headers {} :body ""}
            (with-viewer-session req vid))))))

(defn cfp-refusal-message
  "Plain English for a speaker whose submission we will not take, naming the
   date wherever there is one. Never the word 'error' — nothing they did was
   wrong; they arrived outside the window."
  [event]
  (case (submissions/cfp-state event)
    :closed (str "The call for speakers closed"
                 (when-let [c (views/fmt-instant (:cfp-closes-at event) (:tz event))]
                   (str " on " c))
                 ", so this wasn't submitted. Nothing was recorded — please "
                 "contact the organizers if you need an exception.")
    :not-open-yet (str "The call for speakers isn't open yet, so this wasn't "
                       "submitted. Nothing was recorded — please check back, "
                       "or contact the organizers.")
    "This event isn't accepting submissions right now, so nothing was recorded."))

(defn handle-cfp-submit
  "Plain form POST. Valid → 303 to the confirmation page.
   Invalid / closed / over cap → 422 re-render with server-side messages."
  [req]
  (let [slug (get-in req [:path-params :slug])
        event (events/event-by-slug slug)]
    (if-not event
      (cfp-not-found slug)
      (let [params (:params req)
            form (events/form-for-event (:id event))
            ;; Parse and validate against the SAME set the speaker was shown —
            ;; a retired question must not be collected or demanded.
            form-fields (forms/active-fields (:fields form))
            answers (submissions/parse-answers form-fields params)
            speaker (submissions/parse-speaker
                      ;; the pasted profile URL rides along on a normal submit
                     (update params :speaker-sessionize-url
                             #(or % (:speaker-sessionize-url-carry params))))
            errors (submissions/validation-errors form-fields answers speaker)
            reject (fn [errs message]
                     ;; A refusal is still typing worth keeping — stash before
                     ;; re-rendering so a refresh on the 422 page is safe too.
                     (stash-cfp-draft! req event params)
                     (render-cfp req event
                                 {:values params :errors errs :message message}
                                 422))]
        (cond
          ;; A refusal must LOOK like a refusal. This answered 200 with the
          ;; closed page, which reads to a script — and to a speaker who
          ;; scrolled — as if the submission had been taken. It was not: nothing
          ;; was appended. 422 plus a plain sentence naming the close date is
          ;; the honest answer, and the log still grows by zero.
          (not (submissions/accepting? event))
          (reject nil (cfp-refusal-message event))

          errors
          (do (log/info :submission-rejected :slug slug :fields (vec (keys errors)))
              (reject errors "Almost there — a few fields need attention."))

          :else
          (try
            (let [sub (submissions/create-submission! event answers speaker "form" "speaker")]
              ;; The submit itself is the last thing they typed, and the ABOUT
              ;; YOU half of it is worth keeping: stash the whole post, then
              ;; clear only the answers. A second talk (the cap here is above
              ;; one) starts with a blank form and a filled-in speaker.
              (stash-cfp-draft! req event params)
              (clear-cfp-draft! req event)
              (see-other (str "/cfp/" slug "/submitted/" (:id sub))))
            (catch clojure.lang.ExceptionInfo e
              (case (:type (ex-data e))
                :cap-reached
                (reject nil (str "You've reached this event's limit of "
                                 (:cap (ex-data e)) " submissions. "
                                 "Contact the organizers if you need an exception."))
                ;; The window shut between the check above and the append.
                :cfp-closed
                (reject nil (cfp-refusal-message event))
                (throw e)))))))))

(defn handle-cfp-submitted
  "The confirmation page, addressable so a refresh doesn't resubmit."
  [req]
  (let [slug (get-in req [:path-params :slug])
        submission-id (clean-id (get-in req [:path-params :submission-id]))
        event (events/event-by-slug slug)
        submission (when submission-id (submissions/by-id submission-id))]
    (if (and event submission (= (:id event) (:event-id submission)))
      (html-response (views/cfp-success-page event submission))
      (cfp-not-found slug))))

(defn handle-reload-check [req]
  (if-let [handler (requiring-resolve 'browser-reload.core/reload-check-handler)]
    (handler req)
    {:status 200 :body "no-reload"}))

(defn make-routes []
  [["/" {:get {:handler #'handle-home}}]
   ["/events" {:get {:handler #'handle-events-list}}]
   ["/events/new" {:get {:handler #'handle-new-event}}]
   ["/events/:slug" {:get {:handler #'handle-event-dashboard}}]
   ["/events/:slug/fragment" {:get {:handler #'handle-dashboard-fragment}}]
   ["/events/:slug/details" {:get {:handler #'handle-event-details}}]
   ["/events/:slug/committee" {:get {:handler #'handle-event-committee}}]
   ["/api/events/:slug/details" {:post {:handler #'handle-event-details-save}}]
   ["/events/:slug/submissions" {:get {:handler #'handle-submissions-list}}]
   ["/events/:slug/submissions/:submission-id" {:get {:handler #'handle-submission-detail}}]
   ["/events/:slug/board" {:get {:handler #'handle-board}}]
   ["/events/:slug/board/fragment" {:get {:handler #'handle-board-fragment}}]
   ["/events/:slug/log/fragment" {:get {:handler #'handle-log-fragment}}]
   ["/events/:slug/inform" {:get {:handler #'handle-inform-page}}]
   ["/events/:slug/form" {:get {:handler #'handle-form-builder}}]
   ["/events/:slug/settings" {:get {:handler #'handle-settings}}]
   ["/events/:slug/comms" {:get {:handler #'handle-comms}}]
   ["/events/:slug/replay" {:get {:handler #'handle-replay-page}}]
   ["/events/:slug/capture" {:get {:handler #'handle-capture-page}}]
   ["/events/:slug/schedule" {:get {:handler #'handle-schedule}}]
   ["/agenda/:slug" {:get {:handler #'handle-agenda}}]
   ;; NOTE the missing trailing segment: this is the organizer-gated HTML index.
   ;; /events/:slug/exports/<file> below are the PUBLIC raw files.
   ["/events/:slug/exports" {:get {:handler #'handle-exports-page}}]
   ["/events/:slug/exports/sessions.json" {:get {:handler #'handle-export-sessions}}]
   ["/events/:slug/exports/speakers.json" {:get {:handler #'handle-export-speakers}}]
   ["/events/:slug/exports/calendar.ics" {:get {:handler #'handle-export-ics}}]
   ["/events/:slug/llms.txt" {:get {:handler #'handle-llms-txt}}]
   ;; The read API. Everything under /api/v1/ is a PUBLIC surface by construction
   ;; (auth/open-data-pattern) — the handlers, not the router, decide what a
   ;; token widens. NOTE the trailing slash on the service index: the pattern
   ;; matches "api/v1/", so "/api/v1" alone would be gated. Both are routed; the
   ;; slashed one is the documented spelling.
   ["/api/v1/" {:get {:handler #'handle-api-index}}]
   ["/api/v1" {:get {:handler #'handle-api-index}}]
   ["/api/v1/events/:slug" {:get {:handler #'handle-api-event}}]
   ["/api/v1/events/:slug/docs" {:get {:handler #'handle-api-docs}}]
   ["/api/v1/events/:slug/sessions" {:get {:handler #'handle-api-sessions}}]
   ["/api/v1/events/:slug/speakers" {:get {:handler #'handle-api-speakers}}]
   ["/api/v1/events/:slug/speakers/:person-id" {:get {:handler #'handle-api-speaker}}]
   ["/api/v1/events/:slug/schedule" {:get {:handler #'handle-api-schedule}}]
   ["/api/v1/events/:slug/rooms" {:get {:handler #'handle-api-rooms}}]
   ["/api/v1/events/:slug/changes" {:get {:handler #'handle-api-changes}}]
   ["/api/v1/events/:slug/submissions" {:get {:handler #'handle-api-submissions}}]
   ["/api/v1/events/:slug/submissions/:submission-id" {:get {:handler #'handle-api-submission}}]
   ["/portal" {:get {:handler #'handle-portal}}]
   ;; The portal's live lane. Under /portal, not /api/*, because that prefix is
   ;; what auth/speaker-prefixes already opens to a speaker (see the handlers).
   ["/portal/stream" {:get {:handler #'handle-portal-stream}}]
   ["/portal/draft" {:post {:handler #'handle-portal-draft}}]
   ["/events/:slug/log" {:get {:handler #'handle-event-log}}]
   ["/events/:slug/people/:person-id" {:get {:handler #'handle-person-detail}}]
   ["/cfp/:slug" {:get {:handler #'handle-public-cfp}}]
   ["/cfp/:slug/submitted/:submission-id" {:get {:handler #'handle-cfp-submitted}}]
   ["/api/cfp/:slug/submit" {:post {:handler #'handle-cfp-submit}}]
   ["/api/cfp/:slug/import-sessionize" {:post {:handler #'handle-cfp-import}}]
   ["/api/cfp/:slug/import-live" {:post {:handler #'handle-cfp-import-live}}]
   ;; The speaker side's live lane. Both sit under the PUBLIC /api/cfp prefix
   ;; because the people they serve have no account yet (auth/public-prefixes).
   ["/api/cfp/:slug/draft" {:post {:handler #'handle-cfp-draft}}]
   ["/api/cfp/:slug/stream" {:get {:handler #'handle-cfp-stream}}]
   ["/api/events/create" {:post {:handler #'handle-create-event}}]
   ["/api/events/demo" {:post {:handler #'handle-create-demo-event}}]
   ;; Organizer-side but belongs to NO event — see auth/unscoped-paths.
   ["/api/events/preview" {:post {:handler #'handle-events-preview}}]
   ["/api/events/archive" {:post {:handler #'handle-event-archive}}]
   ["/api/events/:slug/unarchive" {:post {:handler #'handle-event-unarchive}}]
   ["/api/events/draft-pref" {:post {:handler #'handle-draft-pref}}]
   ["/api/events/:slug/cfp/open" {:post {:handler #'handle-cfp-open}}]
   ["/api/events/:slug/cfp/close" {:post {:handler #'handle-cfp-close}}]
   ["/api/events/:slug/cfp/close-date" {:post {:handler #'handle-cfp-close-date}}]
   ["/api/committees/:committee-id/members/add" {:post {:handler #'handle-add-member}}]
   ["/api/memberships/:membership-id/remove" {:post {:handler #'handle-remove-member}}]
   ["/login" {:get {:handler #'handle-login-page}}]
   ["/api/login" {:post {:handler #'handle-login}}]
   ;; /auth/google MUST precede /auth/:token or "google" is read as a token.
   ["/auth/google" {:get {:handler #'auth-google/handle-start}}]
   ["/auth/google/callback" {:get {:handler #'auth-google/handle-callback}}]
   ["/auth/:token" {:get {:handler #'handle-auth-token}}]
   ["/api/demo-login" {:post {:handler #'handle-demo-login}}]
   ["/logout" {:post {:handler #'handle-logout}}]
   ["/api/sse" {:get {:handler #'sse/handle-sse}}]
   ["/api/events/:slug/board/sort" {:post {:handler #'handle-board-sort}}]
   ["/api/submissions/:submission-id/rate" {:post {:handler #'handle-rate}}]
   ["/api/submissions/:submission-id/comment" {:post {:handler #'handle-comment}}]
   ["/api/submissions/:submission-id/status" {:post {:handler #'handle-status}}]
   ["/api/submissions/:submission-id/priority" {:post {:handler #'handle-priority}}]
   ["/api/submissions/:submission-id/inform" {:post {:handler #'handle-inform-one}}]
   ["/api/submissions/:submission-id/answers" {:post {:handler #'handle-portal-answers}}]
   ["/api/submissions/:submission-id/task" {:post {:handler #'handle-portal-task}}]
   ["/api/events/:slug/inform-all" {:post {:handler #'handle-inform-all}}]
   ["/api/profile" {:post {:handler #'handle-portal-profile}}]
   ["/api/events/:slug/schedule/place" {:post {:handler #'handle-schedule-place}}]
   ["/api/events/:slug/schedule/clear" {:post {:handler #'handle-schedule-clear}}]
   ["/api/events/:slug/schedule/room-add" {:post {:handler #'handle-room-add}}]
   ["/api/events/:slug/schedule/room-remove" {:post {:handler #'handle-room-remove}}]
   ["/api/events/:slug/schedule/block-add" {:post {:handler #'handle-block-add}}]
   ["/api/events/:slug/schedule/block-remove" {:post {:handler #'handle-block-remove}}]
   ["/api/events/:slug/schedule/lock" {:post {:handler #'handle-schedule-lock}}]
   ["/api/events/:slug/schedule/unlock" {:post {:handler #'handle-schedule-unlock}}]
   ["/api/events/:slug/form/preview" {:post {:handler #'handle-form-preview}}]
   ["/api/events/:slug/form/add" {:post {:handler #'handle-form-add}}]
   ["/api/events/:slug/form/update" {:post {:handler #'handle-form-update}}]
   ["/api/events/:slug/form/move" {:post {:handler #'handle-form-move}}]
   ["/api/events/:slug/form/retire-ask" {:post {:handler #'handle-form-retire-ask}}]
   ["/api/events/:slug/form/retire" {:post {:handler #'handle-form-retire}}]
   ["/api/events/:slug/form/retire-cancel" {:post {:handler #'handle-form-retire-cancel}}]
   ["/api/events/:slug/form/restore" {:post {:handler #'handle-form-restore}}]
   ["/api/events/:slug/form/reviewed" {:post {:handler #'handle-form-reviewed}}]
   ["/api/events/:slug/capture" {:post {:handler #'handle-capture}}]
   ["/api/replay/start-demo" {:post {:handler #'handle-replay-start-demo}}]
   ["/api/events/:slug/replay/play" {:post {:handler #'handle-replay-play}}]
   ["/api/events/:slug/replay/pause" {:post {:handler #'handle-replay-pause}}]
   ["/api/events/:slug/replay/skip" {:post {:handler #'handle-replay-skip}}]
   ["/api/events/:slug/airtable/set" {:post {:handler #'handle-airtable-set}}]
   ["/api/events/:slug/airtable/remove" {:post {:handler #'handle-airtable-remove}}]
   ["/api/events/:slug/slack/set" {:post {:handler #'handle-slack-set}}]
   ["/api/events/:slug/slack/remove" {:post {:handler #'handle-slack-remove}}]
   ["/api/events/:slug/slack/test" {:post {:handler #'handle-slack-test}}]
   ["/api/events/:slug/notice/dismiss" {:post {:handler #'handle-notice-dismiss}}]
   ["/api/events/:slug/api-keys/create" {:post {:handler #'handle-api-key-create}}]
   ["/api/events/:slug/api-keys/revoke" {:post {:handler #'handle-api-key-revoke}}]
   ["/api/events/:slug/webhooks/add" {:post {:handler #'handle-webhook-add}}]
   ["/api/events/:slug/webhooks/remove" {:post {:handler #'handle-webhook-remove}}]
   ["/dev/reload-check" {:get {:handler #'handle-reload-check}}]
   ["/dev/sse-state" {:get {:handler #'handle-sse-state}}]])

(defn- wrap-remember-working-event
  "Router-level: a signed-in GET of any /events/:slug page marks that event as
   the person's working event, so the sidebar's spine follows them onto pages
   with no event in the URL (docs/design/nav-elements.md)."
  [handler]
  (fn [req]
    (when (and (= :get (:request-method req))
               (str/starts-with? (:uri req) "/events/"))
      (when-let [slug (get-in req [:path-params :slug])]
        (when-let [person (auth/current-person req)]
          (when-let [event (events/event-by-slug slug)]
            (events/remember-working-event! (:id person) (:id event))))))
    (handler req)))

(defn- make-ring-handler []
  (ring/ring-handler
    ;; /events/new deliberately overlaps /events/:slug. reitit's mixed router
    ;; always prefers the static segment, so we tell it the overlap is intended
    ;; (:conflicts nil) instead of it refusing to build. Covered by
    ;; server-test/routes-build-and-resolve-test.
   (ring/router (make-routes) {:conflicts nil
                               :data {:middleware [wrap-remember-working-event]}})
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler
     {:not-found (fn [_] {:status 404 :body "Not found"})}))))

(defn wrap-store-refresh
  "Re-fold the log if another process appended to it (e.g. `make seed-demo`
   while the dev server is running). One File.length() per request."
  [handler]
  (fn [req]
    (store/refresh-if-changed!)
    (handler req)))

(defn create-app []
  (let [is-dev? (= "dev" (System/getenv "ENV"))
        handler (if is-dev?
                  (ring/reloading-ring-handler #'make-ring-handler) ;; rebuilds router every request — new routes work without restart
                  (make-ring-handler))]
    (-> handler
        auth/wrap-require-login          ; innermost of the three below
        wrap-keyword-params
        wrap-params
        ;; session must be OUTSIDE the gate (the gate reads :session) and
        ;; outside params (auth reads nothing from params).
        ;;
        ;; Explicit cookie attrs are REQUIRED for the Google OAuth flow: the
        ;; browser returns from accounts.google.com via a cross-site top-level
        ;; navigation, and only a SameSite=Lax cookie rides along — the default
        ;; (unset) was dropped on that hop, so the :oauth-state nonce vanished
        ;; and every callback failed :bad-state (prod, 2026-08-09). Secure is
        ;; on in prod (https) and OFF in dev (http, or the cookie is never set).
        (wrap-session {:cookie-attrs {:http-only true
                                      :same-site :lax
                                      :secure (not is-dev?)}})
        wrap-store-refresh
        wrap-content-type)))

(defn create-app-dev []
  (let [app (create-app)]
    (cond-> app
      ;; browser-reload and ring-devel in SEPARATE try blocks
      ;; so one missing dep doesn't break the other
      true (as-> handler
                 (try
                   (let [wrap-reload-script (requiring-resolve 'browser-reload.core/wrap-reload-script)]
                     (wrap-reload-script handler))
                   (catch Exception e
                     (log/warn :browser-reload-unavailable :msg (.getMessage e))
                     handler)))
      true (as-> handler
                 (try
                   (let [wrap-reload (requiring-resolve 'ring.middleware.reload/wrap-reload)]
                     (wrap-reload handler {:dirs ["src" "resources"]
                                           :reload-compile-errors? true}))
                   (catch Exception e
                     (log/warn :ring-reload-unavailable :msg (.getMessage e))
                     handler))))))

(defn start-server!
  ([] (start-server! (Integer/parseInt (or (System/getenv "PORT") "3001"))))
  ([port]
   (let [is-dev? (= "dev" (System/getenv "ENV"))
         ;; If a snapshot bucket is configured and this box has no local log
         ;; yet (a fresh container), pull it down BEFORE folding. Refuses to
         ;; overwrite an existing local log — see sinks/restore-from-snapshot!.
         _ (try (sinks/restore-from-snapshot!)
                (catch Exception e
                  (log/warn :gcs-restore-error :msg (.getMessage e))))
         ;; Zero setup: replay the append-only log into memory. No database to
         ;; install, no connection string, nothing to start first.
         _ (store/load!)
         app     (if is-dev? (create-app-dev) (create-app))
         stop-fn (http/run-server app {:port port :thread 8})]
     (log/info :server-started :port port :dev is-dev?)
     (when is-dev?
       ;; Guarded like the middleware above it: ENV=dev without the :dev alias
       ;; is a legitimate way to run (dev behaviour, no reload tooling), and it
       ;; used to take the whole server down here while the middleware calls
       ;; degraded gracefully three lines earlier.
       (try
         (when-let [start-watcher (requiring-resolve 'browser-reload.core/start-file-watcher!)]
           (start-watcher ["src" "resources"] #{"clj" "css" "js"}))
         (catch Exception e
           (log/warn :file-watcher-unavailable :msg (.getMessage e)))))
     (reset! server stop-fn)
     stop-fn)))

(defn stop-server! []
  (when-let [stop-fn @server]
    (stop-fn)
    (when-let [stop-watcher (requiring-resolve 'browser-reload.core/stop-file-watcher!)]
      (stop-watcher))
    (reset! server nil)
    (log/info :server-stopped)))

(defn -main [& _args]
  (start-server!))

(comment
  (start-server! 20500)
  (stop-server!))