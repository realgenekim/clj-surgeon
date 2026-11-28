(ns cfp-scheduler-killer.auth
  "Magic-link-lite identity for committee members.

   The rule that shapes this: **no account wall, ever, for speakers.** The
   public CFP page never asks who you are. Identity exists only so the board can
   say *whose* 4.5 that is — reviewers are named, which is the entire point of a
   conversation among trusted peers.

   Who may sign in is DERIVED, not administered: if your email is on any
   committee of any event, you can get a link. There is no user table to
   maintain, no invite to accept, no role to grant — the roster IS the
   permission (docs/design/domain-model.md).

   Tokens and sessions are SESSION STATE, not facts about the world, so they
   live in atoms and are deliberately NOT appended to the store. Restarting the
   server signs everyone out; that is correct — a login is not history."
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [cfp-scheduler-killer.mail :as mail]
            [cfp-scheduler-killer.people :as people]
            [cfp-scheduler-killer.portal :as portal]
            [cfp-scheduler-killer.store :as store]))

(def token-ttl-ms (* 24 60 60 1000))

(defonce ^{:doc "{token -> {:person-id .. :expires-at ..}}. Ephemeral by design."}
  tokens (atom {}))

(defn dev? [] (= "dev" (System/getenv "ENV")))

;; --- Who may sign in --------------------------------------------------------

(defn committee-member?
  "True when this email sits on ANY committee of ANY event."
  [email]
  (let [email (people/normalize-email email)]
    (boolean
     (when-let [person (people/by-email email)]
       (some #(= (:id person) (:person-id %))
             (vals (:memberships (store/snapshot))))))))

(defn speaker?
  "True when this email has submitted at least one talk.

   Speakers get in on the strength of having submitted — no invitation, no
   account creation, nothing to accept. They already proved who they are by
   putting work into the form."
  [email]
  (boolean
   (when-let [person (people/by-email (people/normalize-email email))]
     (portal/speaker? (:id person)))))

(defn may-sign-in?
  [email]
  (or (committee-member? email) (speaker? email)))

(defn first-run?
  "True on a brand-new instance: NO conferences exist yet.

   This is the narrow bootstrap, and the narrowing is the point. It used to key
   off 'no memberships anywhere', which had two ugly consequences: on a populated
   instance, removing the last reviewer promoted *everyone who could sign in* to
   full organizer; and a speaker on such an instance could create events. Keying
   off 'no events' can only ever be true once, at the very beginning, and it
   cannot be re-entered by deleting anything — the log always still has the
   events.

   Without SOME bootstrap a fresh install is unusable by design: organizer pages
   need a reviewer, reviewers are only added from an organizer page, so nobody
   could create the first conference. The first person through the door creates
   the first event and is auto-added as its chair (server/handle-create-event),
   which closes the window behind them."
  []
  (empty? (:events (store/snapshot))))

(defn reviewer-somewhere?
  "Does this person sit on any committee, of any conference?"
  [person]
  (boolean
   (and person
        (some #(= (:id person) (:person-id %))
              (vals (:memberships (store/snapshot)))))))

(defn organizer?
  "May this person see the organizer side AT ALL?

   Reviewer on ANY conference, OR the first-run window above. This answers 'is
   this person an organizer somewhere', which is the right question for /events
   and for CREATING a conference — and nothing else. It is emphatically NOT the
   question 'may they touch THIS conference' — see `member-of-event?`. Treating
   the two as one is the hole this file used to have: a reviewer on one event
   could read another's settings page (API token and all) and flip its
   decisions."
  [person]
  (boolean (or (reviewer-somewhere? person)
               (and person (first-run?)))))

(def may-create-events?
  "Creating a conference needs the same standing as seeing the organizer side:
   a reviewer seat somewhere, or the first-run window. Named separately because
   it is a different QUESTION that happens to have the same answer — when
   creation grows a rule of its own, it grows it here."
  organizer?)

(defn member-of-event?
  "Is this person on a committee OF THIS conference?

   The roster is still the permission (domain-model.md) — this only says WHICH
   roster. Committees remain 'a roster + a scope filter, never a permission
   fortress': every committee of an event has the same reach over that event.
   The boundary we draw is between conferences, not between committees."
  ([person event-id] (member-of-event? (store/snapshot) person event-id))
  ([snapshot person event-id]
   (boolean
    (when (and person event-id)
      (let [committee-ids (into #{}
                                (comp (filter #(= event-id (:event-id %)))
                                      (map :id))
                                (vals (:committees snapshot)))]
        (some (fn [m]
                (and (contains? committee-ids (:committee-id m))
                     (= (:id person) (:person-id m))))
              (vals (:memberships snapshot))))))))

(defn home-path
  "Where this person belongs after signing in.

   A person can be BOTH — Gene submits talks to other people's events — so
   organizer wins: it is the view with a way to reach everything else."
  [person]
  (if (organizer? person) "/events" "/portal"))

(defn- purge-expired! []
  (let [now (System/currentTimeMillis)]
    (swap! tokens #(into {} (remove (fn [[_ v]] (< (:expires-at v) now))) %))))

(defn- register-person!
  "Append a person for a first-run organizer. Their identity is a real fact, so
   it belongs in the log like any other."
  [email]
  (let [person (people/new-person email (first (str/split email #"@")))]
    (store/append! {:type "person.created" :actor "bootstrap" :payload person})
    person))

(defn issue-token!
  "Mint a login token for a committee member. Returns the token, or nil when the
   email isn't on any roster (except in first-run bootstrap, see above).

   Deliberately does NOT reveal which it was to the caller's UI — see the
   handler: an unknown email gets the same neutral response, so this endpoint
   can't be used to enumerate who is on a committee."
  [email]
  (purge-expired!)
  (when-let [email (people/normalize-email email)]
    (let [bootstrap? (and (first-run?) (not (may-sign-in? email)))]
      (when bootstrap?
        (log/warn :login-bootstrap-mode :email email
                  :msg "no committees exist yet — admitting the first organizer"))
      (when (or bootstrap? (may-sign-in? email))
        (let [person (or (people/by-email email)
                         (register-person! email))
              token (str (random-uuid))]
          (swap! tokens assoc token {:person-id (:id person)
                                     :email email
                                     :expires-at (+ (System/currentTimeMillis) token-ttl-ms)})
          ;; In prod the link arrives by mail; in dev it is ALSO shown on the
          ;; page, because a dev with no SMTP would otherwise be locked out of
          ;; their own app.
          (mail/send!
           {:to email
            :subject "Your sign-in link"
            :body (str "Hi,\n\nHere is your sign-in link. It works once and "
                       "expires in 24 hours:\n\n    /auth/" token
                       "\n\nIf you did not ask for this, you can ignore it — "
                       "nothing has changed.\n")}
           {:kind "magic-link" :actor "system" :person-id (:id person)})
          (log/info :login-token-issued :email email :bootstrap bootstrap?)
          token)))))

(defn redeem-token!
  "Exchange a token for a person-id. Single use: redeeming removes it, so a link
   that leaks into a Slack channel can't be replayed."
  [token]
  (purge-expired!)
  (when-let [entry (get @tokens token)]
    (swap! tokens dissoc token)
    (when (>= (:expires-at entry) (System/currentTimeMillis))
      (log/info :login-token-redeemed :email (:email entry))
      (:person-id entry))))

;; --- Session ----------------------------------------------------------------

(defn current-person
  "The signed-in person, or nil."
  [req]
  (when-let [pid (get-in req [:session :person-id])]
    (store/person-by-id pid)))

(defn signed-in? [req] (some? (current-person req)))

(defn sign-in
  "Attach the session to a response."
  [resp person-id]
  (assoc resp :session {:person-id person-id}))

(defn sign-out [resp]
  (assoc resp :session nil))

;; --- The gate ---------------------------------------------------------------

(def public-prefixes
  "Everything a speaker or a stranger may reach without signing in. The CFP and
   its assets are public on purpose — an account wall at submission time is the
   incumbent behaviour we are replacing."
  ["/cfp" "/agenda" "/api/cfp" "/login" "/api/login" "/api/demo-login" "/auth" "/logout"
   "/js/" "/vendor/" "/css/" "/favicon.ico" "/dev/reload-check"])

(defn- prefix-match?
  "Prefix matching AT A SEGMENT BOUNDARY.

   `str/starts-with?` was too generous, and on an allowlist too generous means
   open: \"/cfp\" matched \"/cfpanything\", so any future route whose name merely
   began with a public one would have been public too — reachable with no
   session at all. A prefix now covers itself and everything BELOW it, and
   nothing that merely shares its spelling.

   Prefixes written with a trailing slash (\"/js/\") are asset roots and are
   already boundary-safe, so they keep plain prefix semantics."
  [uri prefix]
  (if (str/ends-with? prefix "/")
    (str/starts-with? uri prefix)
    (or (= uri prefix)
        (str/starts-with? uri (str prefix "/")))))

(def speaker-prefixes
  "The signed-in surfaces a SPEAKER legitimately owns.

   This list is the ONLY thing standing between a speaker and the organizer
   side, because the gate below is default-deny: anything signed-in that is not
   named here requires committee membership. Adding a route does not quietly
   open it — you have to come here and say so."
  ["/portal" "/api/profile" "/logout"])

(def ^:private speaker-owned-pattern
  "A speaker acting on their OWN submission: editing their answers, ticking off
   the tasks we gave them. Deliberately a pattern and not a prefix, because
   /api/submissions/:id/ ALSO carries rate, comment, status, priority and
   inform — committee verbs that share the same stem.

   Ownership of the specific submission is the handler's job; this only says
   'a speaker may reach this verb at all'."
  #"^/api/submissions/[^/]+/(answers|task)$")

(defn speaker-path?
  "May a signed-in non-committee person reach this? Home is exact-match: as a
   prefix, \"/\" would match every URI on the site."
  [uri]
  (boolean (or (= uri "/")
               (some #(prefix-match? uri %) speaker-prefixes)
               (re-find speaker-owned-pattern uri))))

(def ^:private open-data-pattern
  "Static exports and the versioned REST API are PUBLIC surfaces. The exports
   describe only the published program (accepted + informed), and the API
   defaults to the same set — a token widens what you can read, it is not what
   makes the endpoint reachable."
  #"^/(events/[^/]+/(exports/|llms\.txt)|api/v1/)")

(defn public-path?
  [uri]
  ;; \"/\" is EXACT-match public (bd -7e1: the landing page is the front
  ;; door). It must never join public-prefixes: as a trailing-slash prefix
  ;; it would match every URI on the site and open everything.
  (boolean (or (= uri "/")
               (some #(prefix-match? uri %) public-prefixes)
               (re-find open-data-pattern uri))))

;; --- Which conference is this request about? --------------------------------
;;
;; Authorization needs a subject (the person) and an OBJECT. The object here is
;; always a conference, but the URL names it five different ways — by slug, or
;; through a submission, a committee or a membership id. Resolving that in ONE
;; place means a new route inherits the scoping instead of having to remember
;; it, which is the same property that makes the gate itself default-deny.

(def ^:private unscoped-paths
  "Organizer surfaces that belong to no single conference: the list of events,
   creating one, and the shared SSE stream. Any organizer may reach these.

   An explicit set, not a pattern, because /api/events/create,
   /api/events/demo and /api/events/preview are spelled exactly like
   /api/events/:slug/… and would otherwise resolve to a conference named
   \"create\" — which does not exist, so `event-id-for-uri` would answer
   :unknown and the gate would refuse the write. The create page's live marquee
   would go silent in exactly the way a broken SSE connection looks."
  #{"/" "/events" "/events/new"
    "/api/events/create" "/api/events/demo" "/api/events/preview"
    "/api/events/draft-pref"
    "/api/replay/start-demo"
    "/api/sse" "/dev/sse-state"
    "/portal" "/api/profile" "/logout"})

(def ^:private scope-patterns
  "[regex kind] — the first capture group names the thing, `kind` says how to
   turn it into a conference."
  [[#"^/events/([^/]+)(?:/|$)"        :slug]
   [#"^/agenda/([^/]+)(?:/|$)"        :slug]
   [#"^/cfp/([^/]+)(?:/|$)"           :slug]
   [#"^/api/cfp/([^/]+)(?:/|$)"       :slug]
   [#"^/api/v1/events/([^/]+)(?:/|$)" :slug]
   [#"^/api/events/([^/]+)(?:/|$)"    :slug]
   [#"^/api/submissions/([^/]+)(?:/|$)" :submission]
   [#"^/api/committees/([^/]+)(?:/|$)"  :committee]
   [#"^/api/memberships/([^/]+)(?:/|$)" :membership]])

(defn event-id-for-uri
  "Which conference does this URI act on?

     nil       — not conference-scoped (see `unscoped-paths`)
     <id>      — this conference
     :unknown  — it NAMES one, but no such slug/submission/committee/membership

   :unknown is deliberately a third answer rather than nil. Collapsing it into
   'unscoped' would mean a typo'd slug re-opened every route to any organizer;
   collapsing it into a refusal would turn every 404 into a 403 and make the
   organizer's mistyped URL look like a permissions problem."
  [snapshot uri]
  (when-not (contains? unscoped-paths uri)
    (letfn [(by-slug [slug] (get-in snapshot [:events slug]))
            (committee-event [cid] (get-in snapshot [:committees cid :event-id]))]
      (some (fn [[re kind]]
              (when-let [[_ id] (re-find re uri)]
                (or (case kind
                      :slug       (:id (by-slug id))
                      :submission (get-in snapshot [:submissions id :event-id])
                      :committee  (committee-event id)
                      :membership (some-> (get-in snapshot [:memberships id :committee-id])
                                          committee-event))
                    :unknown)))
            scope-patterns))))

(defn- redirect [location] {:status 302 :headers {"Location" location} :body ""})

(defn- forbidden
  "Refusing a WRITE must look nothing like performing it.

   The old gate matched organizer PAGES by URL prefix (\"/events\"), which left
   every mutation under /api/ guarded by nothing but 'is signed in'. A speaker —
   and everyone who submits a talk becomes one — could rate proposals, flip a
   talk from Accepted to Declined, and lock the schedule. Each one answered 303,
   indistinguishable from success.

   So a denied write says 403 and says why."
  []
  {:status 403
   :headers {"Content-Type" "text/plain; charset=utf-8"}
   :body (str "This action needs a program-committee reviewer of this event.\n"
              "If you should be one, ask an organizer to add you.\n")})

(defn- wrong-event
  "Refusing an organizer is a different sentence from refusing a stranger.

   They ARE on a committee — just not this conference's — so the message says
   which fact is missing instead of implying they have no business here at all.
   Same 403 for a GET as for a POST: a page that renders this event's API token
   is as much a leak as a write."
  []
  {:status 403
   :headers {"Content-Type" "text/plain; charset=utf-8"}
   :body (str "This action needs a program-committee reviewer of THIS event.\n"
              "You review for another conference on this instance, which is not\n"
              "the same thing. Ask one of this event's organizers to add you.\n")})

(declare gate)

(defn wrap-require-login
  "The gate, and it is DEFAULT-DENY — twice over.

   Four tiers, in order: public paths need no session; the speaker's own
   surfaces need only a session; the organizer side requires committee
   membership; and a conference-scoped URL requires membership **on that
   conference**. Every route added after this was written inherits all four.
   That ordering is the whole point. An allowlist of protected paths is how the
   original hole appeared: organizer pages sat under /events and were gated,
   while the mutations that actually change decisions sat under /api and were
   not. Naming what is OPEN cannot fail that way.

   The speaker tier is checked BEFORE the organizer tier because a person is
   frequently both — Gene chairs one conference and submits to another. Their
   portal and their own submission must not depend on which committees they sit
   on. Ownership of the specific submission is still the handler's job.

   The second default-deny is the conference: `event-id-for-uri` returns nil
   only for the handful of routes that genuinely belong to no conference, so a
   new /api/events/:slug/… verb is cross-event-refused on the day it is written.
   A URL that names a conference we don't have is refused for writes and passed
   through for reads, so the router can answer the honest 404.

   A speaker who follows a link to an organizer PAGE is not scolded — they are
   sent to their portal, which is where they meant to go. A speaker who POSTs at
   a committee verb is refused outright.

   ---

   The DECISION lives in `gate`, one var away, and that separation is not
   stylistic. `create-app` composes this middleware ONCE at boot, so the
   returned closure is frozen: a running dev server kept answering with the old
   cond for an hour after the new one was written, and the drive against it
   reported six false failures. Calling a var per request means redefining
   `gate` takes effect on the next request, like every route handler in this
   codebase (CLAUDE.md, the #'var convention). A security rule you cannot
   observe changing is a security rule you cannot verify."
  [handler]
  (fn [req] (gate handler req)))

(defn gate
  "One request against the four tiers. See `wrap-require-login` for the why."
  [handler req]
  (let [uri (:uri req)
        person (current-person req)
        get? (= :get (:request-method req))]
    (cond
      (public-path? uri) (handler req)

      (nil? person)
      (redirect (str "/login?next=" (java.net.URLEncoder/encode uri "UTF-8")))

      (speaker-path? uri) (handler req)

      (not (organizer? person))
      (if get? (redirect "/portal") (forbidden))

      :else
      (let [snapshot (store/snapshot)
            scope (event-id-for-uri snapshot uri)]
        (cond
            ;; Not about one conference (the events list, creating one, SSE).
            ;; `organizer?` — which for these paths IS `may-create-events?` —
            ;; has already been satisfied above.
          (nil? scope) (handler req)

          (= :unknown scope)
          (if get?
            (handler req)               ; let the router answer 404 honestly
            (forbidden))

          (member-of-event? snapshot person scope) (handler req)

          :else
          (do (log/info :authz-cross-event-refused
                        :person-id (:id person) :uri uri
                        :method (:request-method req) :event-id scope)
              (wrong-event)))))))
