(ns cfp-scheduler-killer.sse
  "SSE infrastructure — Datastar HTML fragment push to connected browsers.

   Ported from joe-payne-app's esr-dashboard.sse, including the lesson that
   produced it: push is performed OFF the request thread. All fan-out writes go
   through a single-threaded `push-agent` via `send-off`, so an http-kit worker
   returns immediately and one slow/half-dead client can never block it. Writing
   to every subscriber synchronously on the caller's thread starved http-kit's
   small worker pool there, and unrelated POSTs started returning 503.

   Serializing ALL writes (pushes AND heartbeats) through the one agent also
   means we never call `patch-elements!` on the same sse-gen from two threads at
   once. Every push carries the FULL current fragment (idempotent), so a client
   that drops and reconnects just re-paints the truth.

   One addition over the original: subscribers are tagged with the event they
   are watching, so a rating on one conference never fans out to a committee
   looking at another."
  (:require [clojure.data.json :as json]
            [starfederation.datastar.clojure.api :as d*]
            [starfederation.datastar.clojure.adapter.http-kit :as hk]
            [hiccup2.core :as h]
            [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Subscriber tracking — {sse-gen -> event-id}
;; ---------------------------------------------------------------------------

(defonce ^{:doc "{sse-gen -> {:event-id .. :person-id ..}}"} subscribers (atom {}))

(defn add-subscriber! [sse-gen event-id person-id]
  (swap! subscribers assoc sse-gen {:event-id event-id :person-id person-id})
  (log/debug :sse :subscribe :event-id event-id :count (count @subscribers)))

(defn remove-subscriber! [sse-gen]
  (swap! subscribers dissoc sse-gen)
  (log/debug :sse :unsubscribe :count (count @subscribers)))

(defn subscriber-count
  ([] (count @subscribers))
  ([event-id] (count (filter #(= event-id (:event-id (val %))) @subscribers))))

;; ---------------------------------------------------------------------------
;; Off-thread push agent — the single thread that does ALL fan-out writes
;; ---------------------------------------------------------------------------

(defonce ^:private push-agent (agent nil))

(defn- reap-dead!
  "Drop subscribers whose write failed (closed/half-dead sockets)."
  [dead]
  (when (seq dead)
    (swap! subscribers #(apply dissoc % dead))
    (log/debug :sse :reaped (count dead) :remaining (count @subscribers))))

(defn- targets
  "The [sse-gen info] pairs that should receive this push. nil = everyone."
  [event-id]
  (if (nil? event-id)
    (seq @subscribers)
    (filter (fn [[_ info]] (= event-id (:event-id info))) @subscribers)))

(defn- broadcast!*
  "Runs ON the push-agent thread. `html-for` is (fn [person-id] html-string) —
   a function, not a string, because some fragments are PERSONAL.

   The board's star widget highlights the viewer's OWN rating; broadcasting one
   render would show everyone the actor's highlight (and, worse, everyone would
   see it as their own opinion). So each subscriber gets a fragment rendered for
   them. It costs one render per connected reviewer, which is nothing, and it
   makes 'the server decides what YOU see' literally true."
  [html-for selector patch-mode event-id]
  (let [dead (reduce (fn [d [sub info]]
                       (if (try
                             (d*/patch-elements! sub (html-for (:person-id info))
                                                 {d*/selector selector
                                                  d*/patch-mode patch-mode})
                             true
                             (catch Exception _ false))
                         d
                         (conj d sub)))
                     #{}
                     (targets event-id))]
    (reap-dead! dead)))

;; ---------------------------------------------------------------------------
;; Heartbeat — keeps SSE alive, detects dead connections
;; ---------------------------------------------------------------------------

(defonce heartbeat-timer (atom nil))

(defn send-heartbeat! []
  (let [ts   (str (System/currentTimeMillis))
        html (str "<span id=\"sse-heartbeat\" data-ts=\"" ts "\"></span>")]
    (send-off push-agent
              (fn [_] (broadcast!* (constantly html) "#sse-heartbeat" d*/pm-outer nil) nil))))

(defn start-heartbeat!
  "Start the 15s heartbeat timer ONCE. Idempotent, so calling it defensively on
   every connect never cancels+recreates a live timer (that raced under
   concurrent opens in the original)."
  []
  (when-not @heartbeat-timer
    (let [timer (java.util.Timer. "sse-heartbeat" true)
          task  (proxy [java.util.TimerTask] []
                  (run []
                    (try (send-heartbeat!)
                         (catch Exception e
                           (log/warn :sse :heartbeat-error :msg (.getMessage e))))))]
      (.scheduleAtFixedRate timer task (long 5000) (long 15000))
      (reset! heartbeat-timer timer)
      (log/info :sse :heartbeat-started))))

(defn stop-heartbeat! []
  (when-let [t @heartbeat-timer]
    (.cancel ^java.util.Timer t)
    (reset! heartbeat-timer nil)))

;; ---------------------------------------------------------------------------
;; Push helpers — render on the caller (cheap), fan out off-thread (the slow part)
;; ---------------------------------------------------------------------------

(defn push-fragment!
  "Push ONE identical fragment to everyone watching `event-id` (coverage bar,
   headline counts — anything impersonal). Rendered once, on the caller's cheap
   thread; fanned out off-thread."
  ([event-id selector hiccup-fn] (push-fragment! event-id selector hiccup-fn d*/pm-outer))
  ([event-id selector hiccup-fn patch-mode]
   (let [html-str (str (h/html (hiccup-fn)))]
     (send-off push-agent
               (fn [_] (broadcast!* (constantly html-str) selector patch-mode event-id) nil))
     html-str)))

(defn push-personal-fragment!
  "Push a fragment rendered PER VIEWER. `hiccup-for` is (fn [person-id] hiccup).
   Use for anything that reflects who is looking — the board row does, because
   it shows you your own rating."
  ([event-id selector hiccup-for] (push-personal-fragment! event-id selector hiccup-for d*/pm-outer))
  ([event-id selector hiccup-for patch-mode]
   ;; Render once per DISTINCT viewer, on the caller thread (request-bound state
   ;; is still available here), then hand the writes to the agent.
   (let [by-person (into {}
                         (map (fn [pid] [pid (str (h/html (hiccup-for pid)))]))
                         (distinct (map (comp :person-id val) (targets event-id))))]
     (send-off push-agent
               (fn [_] (broadcast!* #(get by-person %) selector patch-mode event-id) nil))
     by-person)))

;; ---------------------------------------------------------------------------
;; One viewer, one screen — the create page's live marquee
;; ---------------------------------------------------------------------------

(def new-event-channel
  "The pseudo-event the CREATE page subscribes to. It is a string, and every
   real event-id is a uuid, so the two can never collide.

   The create page has no event yet, but it still needs a stream: the marquee is
   re-rendered by the server on every keystroke. Subscribing it to `nil` would
   have put it in the everyone-bucket that heartbeats use, and two organizers
   drafting at the same time would type into each other's headline."
  "new-event")

(defn- person-targets [event-id person-id]
  (filter (fn [[_ info]] (and (= event-id (:event-id info))
                              (= person-id (:person-id info))))
          @subscribers))

(defn person-connection-count
  "How many live connections this push would actually reach.

   Exists so a handler can refuse to succeed quietly. A push to a key nobody
   registered under is indistinguishable, from the outside, from a push that
   worked — the endpoint still answers 204 and the screen still doesn't move.
   That is the instrument-lying failure this repo keeps rediscovering, so the
   count is a fact the caller can log."
  [event-id person-id]
  (count (person-targets event-id person-id)))

(defn registrations
  "Every live subscription as plain data: [{:event-id .. :person-id ..} ..].
   Read-only, for /dev/sse-state."
  []
  (mapv (fn [[_ info]] info) @subscribers))

(defn push-to-person!
  "Push a fragment to ONE person's connections on `event-id` — nobody else's.

   `push-personal-fragment!` renders per viewer but still fans out to everyone
   watching the event; this is the narrower thing, for a fragment that reflects
   what THIS person is typing rather than what happened to the event."
  ([event-id person-id selector hiccup-fn]
   (push-to-person! event-id person-id selector hiccup-fn d*/pm-outer))
  ([event-id person-id selector hiccup-fn patch-mode]
   (let [html-str (str (h/html (hiccup-fn)))]
     (send-off push-agent
               (fn [_]
                 (let [dead (reduce (fn [d [sub _]]
                                      (if (try
                                            (d*/patch-elements! sub html-str
                                                                {d*/selector selector
                                                                 d*/patch-mode patch-mode})
                                            true
                                            (catch Exception _ false))
                                        d
                                        (conj d sub)))
                                    #{}
                                    (person-targets event-id person-id))]
                   (reap-dead! dead))
                 nil))
     html-str)))

(defn push-signals-to-person!
  "Patch Datastar SIGNALS on ONE person's connections.

   Used when the server changes something the browser is holding in a bound
   input — the trim assist rewrites the event name. The alternative would be
   client JS reaching into the DOM to set `.value`, which is the anti-pattern
   this whole architecture exists to avoid: the server owns the value, so the
   server sends the value.

   Call it AFTER pushing the elements the signals belong to (CLAUDE.md #9)."
  [event-id person-id signals-map]
  (let [payload (json/write-str signals-map)]
    (send-off push-agent
              (fn [_]
                (let [dead (reduce (fn [d [sub _]]
                                     (if (try (d*/patch-signals! sub payload)
                                              true
                                              (catch Exception _ false))
                                       d
                                       (conj d sub)))
                                   #{}
                                   (person-targets event-id person-id))]
                  (reap-dead! dead))
                nil))
    payload))

(defn await-pushes!
  "Block until the push agent has drained. TESTS ONLY — production never waits
   on the fan-out, that is the whole point of the agent."
  ([] (await-pushes! 2000))
  ([_timeout-ms] (await push-agent)))

;; ---------------------------------------------------------------------------
;; SSE endpoint
;; ---------------------------------------------------------------------------

(defn viewer-key
  "Who is this stream FOR? The signed-in person when there is one, otherwise the
   anonymous viewer id minted into the ring session on the public CFP page.

   Speakers are anonymous until submit (account-on-submit doctrine), so the
   public call-for-speakers page has no person-id to key a personal push on. Two
   strangers typing at the same time would both register under `nil` and the
   server would push each one's live feedback into the other's browser. The
   session id is the identity that exists before an account does, so it is the
   one everything per-viewer keys on: the draft stash AND the SSE registration.

   Signed-in behaviour is unchanged — person-id still wins — so every existing
   caller of push-to-person!/push-signals-to-person! keeps working as written."
  [request]
  (or (get-in request [:session :person-id])
      (get-in request [:session :viewer-id])))

(defn handle-sse
  "GET /api/sse?event-id=… — the board's live connection."
  [request]
  (let [event-id (get-in request [:params :event-id])
        person-id (viewer-key request)]
    (hk/->sse-response request
                       {hk/on-open  (fn [sse-gen]
                                      (add-subscriber! sse-gen event-id person-id)
                                      (start-heartbeat!))
                        hk/on-close (fn [sse-gen _]
                                      (remove-subscriber! sse-gen))})))
