(ns marvin-voice-remote.server
  "Web server with http-kit, reitit routing, and Datastar SSE.
   Route handlers are named defns with #'var references for REPL reload."
  (:require
   [clojure.data.json :as json]
   [marvin-voice-remote.app-route :as app-route]
   [marvin-voice-remote.auth :as auth]
   [marvin-voice-remote.bridge3-new :as bridge3-new]
   [marvin-voice-remote.channel :as channel]
   [marvin-voice-remote.config :as config]
   [marvin-voice-remote.director-control :as director-control]
   [marvin-voice-remote.director-outbox :as director-outbox]
   [marvin-voice-remote.reducer-lab :as reducer-lab]
   [marvin-voice-remote.reducer-session :as reducer-session]
   [marvin-voice-remote.sse-registry :as sse]
   [marvin-voice-remote.store :as store]
   [org.httpkit.server :as http]
   [reitit.ring :as ring]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.util.response :as resp]
   [taoensso.timbre :as log])
  (:gen-class))

(defonce server (atom nil))

;; --- Shared app config (set at start, read by handlers) ---
(defonce app-cfg (atom nil))

;; --- JSON helpers ---

(defn- json-response [x & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json; charset=utf-8"
             "Cache-Control" "no-store"}
   :body (json/write-str x)})

;; --- Route handlers ---

(defn handle-config [req]
  (json-response (config/public-config @app-cfg)))

(defn handle-health
  "GET /api/health.

   Carries the SSE stream census, because the failure this endpoint exists to
   report is precisely the one that silences it: when held streams reach the
   instance's concurrency ceiling, Cloud Run 429s EVERY request including this
   one, and the only recovery is the platform drain. `sse/counts` is one deref
   and a frequencies over at most a few hundred records — this handler must stay
   cheap enough to answer while the instance is under load, so it does no I/O,
   takes no lock, and touches no other atom."
  [req]
  (json-response {:ok true :now (store/now-str) :sse (sse/counts)}))

;; --- Routes ---

(defn make-routes []
  ;; Root goes to /bridge4 — today's daily driver. (It was /voice until 2026-07-26,
  ;; when /voice and /bridge2 were deleted: zero traffic in 7 days.) /bridge4 takes
  ;; no required params, so there is nothing to preserve across the redirect.
  [["/" {:get {:handler (fn [_] (resp/redirect "/bridge4"))}}]
   ["/login" {:get  {:handler #'auth/handle-login-page}
              :post {:handler #'auth/handle-login-submit}}]
   ["/logout" {:get  {:handler #'auth/handle-logout}
               :post {:handler #'auth/handle-logout}}]
   ["/api/config" {:get {:handler #'handle-config}}]
   ["/api/health" {:get {:handler #'handle-health}}]
   ;; /reducer-lab: shadow console for the pure reducer (S4). Purely additive — reads only the pure reducer/harness/policy; touches no existing page, atom, or pipeline.
   ["/reducer-lab" {:get {:handler #'reducer-lab/handle-reducer-lab}}]
   ["/reducer-lab/act" {:post {:handler #'reducer-lab/handle-reducer-lab-act}}]
   ;; /reducer-lab/shadow: the FIELD side of S4 — the bounded capture journals the
   ;; taps fill, replayed through the reducer and scored with the five statuses
   ;; (match / explained / unexplained / coverage-gap / incomparable). Read-only
   ;; over the live pipeline; the taps that fill it are total, nil-returning
   ;; observers in channel.clj.
   ["/reducer-lab/shadow" {:get {:handler #'reducer-lab/handle-shadow}}]
   ["/reducer-lab/shadow/act" {:post {:handler #'reducer-lab/handle-shadow-act}}]
   ["/reducer-lab/shadow.edn" {:get {:handler #'reducer-lab/handle-shadow-edn}}]
   ;; /app: D1 canonical route. S1 = alias-only redirect to /bridge4 (today's daily
   ;; driver), preserving ?seat=. Purely additive — never redirects the other way
   ;; until S5c flips /bridge4 onto the reducer.
   ["/app" {:get {:handler #'app-route/handle-app}}]
   ["/tap-to-talk" {:get {:handler #'app-route/handle-tap-to-talk}}]
   ["/ptt" {:get {:handler #'app-route/handle-tap-to-talk}}]
   ["/hf" {:get {:handler #'app-route/handle-hf}}]
   ["/code-director"
    {:get {:handler (fn [request]
                      (channel/handle-code-director-page
                        (app-route/director-request request)))}}]
   ;; /api/release-manifest: D9 honest self-identity manifest. Purely additive —
   ;; reads only baked build resources + pure policy/protocol data.
   ["/api/release-manifest" {:get {:handler #'app-route/handle-release-manifest}}]
   ;; --- channel relay (marvin-channel connector) — additive, bearer-token auth ---
   ["/api/channel/messages" {:get  {:handler #'channel/handle-get-messages}
                             :post {:handler #'channel/handle-post-message}}]
   ["/api/channel/replies" {:get  {:handler #'channel/handle-get-replies}
                            :post {:handler #'channel/handle-post-reply}}]
   ["/api/channel/stats" {:get {:handler #'channel/handle-channel-stats}}]
   ["/api/channel/reply-ack" {:post {:handler #'channel/handle-reply-ack}}]
   ;; Director delivery is a separate, durable, acked lane. It observes only the
   ;; fixed code-director-sse seat and never drains or addresses bridge-cos.
   ["/api/code-director/events" {:get {:handler #'director-outbox/handle-events}}]
   ["/api/code-director/ack" {:post {:handler #'director-outbox/handle-ack}}]
   ["/api/code-director/state" {:get {:handler #'director-outbox/handle-state}}]
   ["/api/code-director/latest-transcription"
    {:get {:handler #'channel/handle-code-director-latest-transcription}}]
   ;; Reverse control is a separate acknowledged lane. The connector may issue a
   ;; command, but only the uniquely active capture client may execute and ack it.
   ["/api/code-director/control" {:post {:handler #'director-control/handle-command}}]
   ["/api/code-director/control/events" {:get {:handler #'director-control/handle-events}}]
   ["/api/code-director/control/ack" {:post {:handler #'director-control/handle-ack}}]
   ["/api/code-director/control/state" {:get {:handler #'director-control/handle-state}}]
   ["/api/code-director/control/:id" {:get {:handler #'director-control/handle-status}}]
   ;; --- direct-to-bridge dictation page (cookie-authed; one tap, no seat dropdown) ---
   ["/bridge" {:get {:handler #'channel/handle-bridge-page}}]
   ;; The /api/bridge/* write path is shared by /bridge (v1) and bridge3/bridge4's
   ;; TTS. (/api/bridge/convo and /api/bridge/recordings were bridge2-only and went
   ;; with it on 2026-07-26.)
   ["/api/bridge/dictate" {:post {:handler #'channel/handle-bridge-dictate}}]
   ["/api/bridge/replies" {:get {:handler #'channel/handle-bridge-replies}}]
   ["/api/bridge/say" {:post {:handler #'channel/handle-bridge-say}}]
   ["/api/bridge/tts" {:get {:handler #'channel/handle-bridge-tts}}]
   ["/api/bridge/voice" {:post {:handler #'channel/handle-bridge-voice}}]
   ["/api/bridge/text-size" {:post {:handler #'channel/handle-bridge-text-size}}]
   ;; --- /bridge3: SEAT-AWARE + MULTIPLAYER dictation tool. Own page, own
   ;; seat-parameterized endpoints, own per-seat SSE fan-out, own broadcast (not
   ;; claim-once) reply path. ---
   ["/bridge3" {:get {:handler #'channel/handle-bridge3-page}}]
   ["/bridge3/sessions" {:get {:handler #'channel/handle-bridge3-sessions-page}}]
   ["/sse/bridge3" {:get {:handler #'channel/handle-sse-bridge3}}]
   ["/api/bridge3/dictate" {:post {:handler #'channel/handle-bridge3-dictate}}]
   ["/api/bridge3/say" {:post {:handler #'channel/handle-bridge3-say}}]
   ["/api/bridge3/convo" {:get {:handler #'channel/handle-bridge3-convo}}]
   ["/api/bridge3/state" {:get {:handler #'channel/handle-bridge3-state}}]
   ["/api/bridge3/sessions" {:get  {:handler #'channel/handle-bridge3-sessions-list}
                             :post {:handler #'channel/handle-bridge3-sessions-mutate}}]
   ;; --- /voice-lab: HANDS-FREE (VAD, no push-to-talk) PROTOTYPE — purely additive.
   ;; Own page + own seat-aware dictate/replies endpoints; never touches bridge3. ---
   ["/voice-lab" {:get {:handler #'channel/handle-voice-lab-page}}]
   ;; S5a: /voice-lab is now the REDUCER surface (ARCHITECTURE.md §7 S5a, D11 —
   ;; the lab flips first, /bridge4 last). The pre-S5a engine is preserved
   ;; byte-for-byte at /voice-lab-legacy as the receipt window.
   ["/voice-lab-legacy" {:get {:handler #'channel/handle-voice-lab-legacy-page}}]
   ;; The session loop the S5a surface talks to: envelopes IN, effect batches OUT.
   ["/api/reducer-session/events" {:post {:handler #'reducer-session/handle-events}}]
   ["/api/reducer-session/effects" {:get {:handler #'reducer-session/handle-effects}}]
   ["/api/reducer-session/state" {:get {:handler #'reducer-session/handle-state}}]
   ["/api/voice-lab/dictate" {:post {:handler #'channel/handle-voice-lab-dictate}}]
   ["/api/voice-lab/keyword-check" {:post {:handler #'channel/handle-voice-lab-keyword-check}}]
   ["/api/voice-lab/replies" {:get {:handler #'channel/handle-voice-lab-replies}}]
   ["/api/voice-lab/upload-probe" {:post {:handler #'channel/handle-voice-lab-upload-probe}}]
   ["/api/voice-lab/clientlog" {:post {:handler #'channel/handle-voice-lab-clientlog}}]
   ["/api/voice-lab/diag" {:get {:handler #'channel/handle-voice-lab-diag}}]
   ;; --- /bridge4: bridge3's seat-aware MULTIPLAYER backend/UI + the /voice-lab HANDS-FREE
   ;; input engine (VAD + keyword "over"+pause) instead of push-to-talk. PURELY ADDITIVE:
   ;; the page REUSES bridge3's /sse/bridge3, /api/bridge3/dictate|say|convo|state|sessions;
   ;; the ONLY new endpoint is /api/bridge4/keyword-check. Never touches bridge3/voice-lab. ---
   ["/bridge4" {:get {:handler #'channel/handle-bridge4-page}}]
   ["/api/bridge4/keyword-check" {:post {:handler #'channel/handle-bridge4-keyword-check}}]
   ;; --- /api/capture-archive/*: RECOVERY over the append-only durable capture
   ;; archive (bd marvin-voice-remote-0ar — the multi-minute recording lost on
   ;; 2026-07-26 that the server had already received AND already transcribed).
   ;; PURELY ADDITIVE: no page changes (the recovery UI is a later slice), no
   ;; existing endpoint or state touched. ---
   ["/api/capture-archive/recent" {:get {:handler #'channel/handle-capture-archive-recent}}]
   ["/api/capture-archive/recover" {:post {:handler #'channel/handle-capture-archive-recover}}]
   ;; --- /bridge3-new: S5c Phase C1 — the ONE new page (D1). Tap-to-talk on the
   ;; REDUCER, the storyboard-A status vocabulary from birth, and a Datastar
   ;; game-engine surface: the server renders every visible thing and morphs it
   ;; over /sse/bridge3-new, while media effects ride the existing session loop's
   ;; own stream. PURELY ADDITIVE: new page, new SSE, two page-state POSTs; it
   ;; reuses /api/reducer-session/* and /api/bridge3/dictate unchanged and
   ;; touches no existing page or atom. Its POSTs are the layout toggle and typed
   ;; send; the conversation is no longer a toggle (Gene, 2026-07-28 — the full
   ;; log is the only state worth having, so the route went with it). ---
   ["/bridge3-new" {:get {:handler #'bridge3-new/handle-page}}]
   ["/ttt" {:get {:handler #'app-route/handle-tap-to-talk}}]
   ["/sse/bridge3-new" {:get {:handler #'bridge3-new/handle-sse}}]
   ;; /api/bridge3-new/capture: the THIN capture/STT PORT (scar B59). Raw bytes in,
   ;; RAW transcript out — no noise filter, no OVER check, no partials stash, no
   ;; post. Every one of those is `reducer/core`'s `handle-transcript` now, and the
   ;; post reaches Marvin through `reducer-session`'s post seam. The legacy
   ;; /api/bridge3/dictate stays exactly as it is for /bridge3 and /bridge4.
   ["/api/bridge3-new/capture" {:post {:handler #'channel/handle-bridge3-capture}}]
   ["/api/bridge3-new/layout" {:post {:handler #'bridge3-new/handle-layout}}]
   ;; /api/bridge3-new/say: TYPED SEND through the reducer (bead 3he). The server
   ;; authors a `:command/say` envelope and ingests it on the ordinary session
   ;; path, so a typed turn is ruled on, journalled, shown and delivered exactly
   ;; as a spoken one is. The legacy /api/bridge3/say still serves /bridge3 and
   ;; /bridge4's shared player, and is now bridge3-new-free.
   ["/api/bridge3-new/say" {:post {:handler #'bridge3-new/handle-say}}]
   ;; --- /bridge5: V5 front door = the channel picker (sessions screen). Pick a session, then
   ;; tap hands-free or push-to-talk. Same handler as /bridge3/sessions; gives V5 a real URL. ---
   ["/bridge5" {:get {:handler #'channel/handle-bridge3-sessions-page}}]])

;; INTENT: ONE-ROUTER-CONSTRUCTOR-M01
(defn make-router
  "THE one way this app's reitit router is constructed — production and tests alike.

   Static control children (/api/code-director/control/{events,ack,state})
   intentionally overlap /api/code-director/control/:id. Reitit's mixed router
   still prefers each exact static segment at dispatch time, so `{:conflicts nil}`
   ACCEPTS that overlap deliberately instead of failing the route table.

   INVARIANT: tests and production construct the router HERE, through this one
   function. A route table that would fail in production therefore cannot pass
   in tests, and a route table that passes in tests cannot fail in production.
   (Scar, 2026-09-01: three tests built their own `(ring/router (make-routes))`
   with no options and died on the intentional overlap the moment they could
   compile again — a divergence that had been invisible for weeks.)"
  []
  (ring/router (make-routes) {:conflicts nil}))

(defn- make-ring-handler []
  (ring/ring-handler
    (make-router)
    (ring/routes
      ;; /bridge4/ (trailing slash, common from phones) -> redirect to /bridge4
      ;; instead of falling through to the catch-all 404.
      (ring/redirect-trailing-slash-handler {:method :strip})
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler
        {:not-found (fn [_] (json-response {:error "not found"} 404))}))))

(defn create-app []
  (let [is-dev? (= "dev" (System/getenv "ENV"))
        handler (if is-dev?
                  (ring/reloading-ring-handler #'make-ring-handler)
                  (make-ring-handler))]
    (cond-> (-> handler
                wrap-keyword-params
                wrap-params
                wrap-multipart-params)
      (not is-dev?) auth/wrap-require-auth
      ;; outermost so OPTIONS preflights are answered BEFORE the auth gate
      true auth/wrap-cors-api)))

(defn create-app-dev []
  (let [app (create-app)]
    (cond-> app
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
  ([] (start-server! (get-in @app-cfg [:server :port])))
  ([port]
   (let [is-dev? (= "dev" (System/getenv "ENV"))
         app     (if is-dev? (create-app-dev) (create-app))
         stop-fn (http/run-server app {:port port :thread 8})]
     (log/info :server-started :port port :dev? is-dev?)
     (when is-dev?
       (try
         (when-let [start-watcher (requiring-resolve 'browser-reload.core/start-file-watcher!)]
           (start-watcher ["src" "resources"] #{"clj" "css" "js"}))
         (catch Exception _)))
     (reset! server stop-fn)
     stop-fn)))

(defn stop-server! []
  (when-let [stop-fn @server]
    (stop-fn)
    (try
      (when-let [stop-watcher (requiring-resolve 'browser-reload.core/stop-file-watcher!)]
        (stop-watcher))
      (catch Exception _))
    (reset! server nil)
    (log/info :server-stopped)))

(defn -main [& _args]
  (start-server!))

(comment
  (start-server! 8080)
  (stop-server!))
