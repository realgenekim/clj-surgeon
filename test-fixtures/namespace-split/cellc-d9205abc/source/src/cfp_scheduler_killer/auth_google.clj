(ns cfp-scheduler-killer.auth-google
  "Sign in with Google — server-side OIDC code flow, ZERO client JavaScript.

   Two redirects and two server-to-server calls, nothing else:
     GET /auth/google           → 303 to Google's consent page (state in session)
     GET /auth/google/callback  → exchange code for tokens (server→Google),
                                  verify the id_token via Google's tokeninfo
                                  endpoint (server→Google), then the EXISTING
                                  rule decides: an email on a committee roster
                                  (or the first-run window) gets a session,
                                  anyone else gets an honest no.

   Google proves WHO; the roster still decides WHETHER — identity and
   authorization stay separate, exactly like the magic-link path. tokeninfo
   verification instead of local JWT crypto: one HTTPS call, zero deps, and
   Google checks the signature so we don't have to (fine at our scale; the
   id_token never leaves the server).

   The client ID is public by design (it's in every redirect URL) and lives in
   config.edn. The client secret is a real secret: secrets.clj — local file in
   dev, Secret Manager on Cloud Run."
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [cfp-scheduler-killer.auth :as auth]
            [cfp-scheduler-killer.people :as people]
            [cfp-scheduler-killer.secrets :as secrets]
            [cfp-scheduler-killer.store :as store]
            [taoensso.timbre :as log]))

(def ^:private config
  (delay (or (some-> (io/resource "config.edn") slurp edn/read-string) {})))

(def ^:private client-id
  (delay (or (System/getenv "GOOGLE_CLIENT_ID") (:google-client-id @config))))

(def ^:private client-secret
  (delay (secrets/load-secret-or-nil "google-oauth-client"
                                     "secrets/google-oauth-client.txt")))

(defn enabled?
  "Both halves of the client present? The login page only offers the button
   when this is true, so an instance without the secret degrades to exactly
   what it was before Google existed."
  []
  (boolean (and @client-id @client-secret)))

(defn- request-host
  "Same derivation as server/request-host (X-Forwarded aware), duplicated
   here because server requires THIS namespace."
  [req]
  (let [headers (:headers req)
        host (or (get headers "x-forwarded-host") (get headers "host") "localhost")
        scheme (or (get headers "x-forwarded-proto") (name (or (:scheme req) :http)))]
    (str scheme "://" host)))

(defn- callback-uri [req] (str (request-host req) "/auth/google/callback"))

(defn handle-start
  "303 to Google. The `state` nonce goes in the session and must come back —
   standard CSRF protection for the code flow."
  [req]
  (if-not (enabled?)
    {:status 404 :headers {"Content-Type" "text/plain"} :body "Google sign-in is not configured on this instance.\n"}
    (let [state (str (java.util.UUID/randomUUID))]
      {:status 303
       :session (assoc (:session req) :oauth-state state)
       :headers {"Location"
                 (str "https://accounts.google.com/o/oauth2/v2/auth"
                      "?client_id=" (java.net.URLEncoder/encode ^String @client-id "UTF-8")
                      "&redirect_uri=" (java.net.URLEncoder/encode (callback-uri req) "UTF-8")
                      "&response_type=code"
                      "&scope=openid%20email%20profile"
                      "&prompt=select_account"
                      "&state=" state)}
       :body ""})))

(defn- exchange-code!
  "code → tokens, server to server. Returns the parsed body map or nil."
  [req code]
  (let [{:keys [status body error]}
        @(http/post "https://oauth2.googleapis.com/token"
                    {:form-params {"code" code
                                   "client_id" @client-id
                                   "client_secret" @client-secret
                                   "redirect_uri" (callback-uri req)
                                   "grant_type" "authorization_code"}
                     :timeout 10000})]
    (if (and (nil? error) (= 200 status))
      (json/read-str body :key-fn keyword)
      (do (log/warn :google-token-exchange-failed :status status
                    :error (some-> error .getMessage))
          nil))))

(defn- verify-id-token!
  "Ask Google what this id_token says. Returns {:email .. :name ..} only when
   the token is valid, meant for OUR client, and the email is verified."
  [id-token]
  (let [{:keys [status body error]}
        @(http/get "https://oauth2.googleapis.com/tokeninfo"
                   {:query-params {"id_token" id-token} :timeout 10000})]
    (when (and (nil? error) (= 200 status))
      (let [claims (json/read-str body :key-fn keyword)]
        (when (and (= @client-id (:aud claims))
                   (contains? #{"true" true} (:email_verified claims))
                   (seq (:email claims)))
          {:email (str/lower-case (:email claims))
           :name (:name claims)})))))

(defn- find-or-create-person!
  "Same act as the magic-link path's first-run registration: identity is a
   real fact, so it goes in the log."
  [{:keys [email name]}]
  (or (store/person-by-email email)
      (let [person (people/new-person email (or name (first (str/split email #"@"))))]
        (store/append! {:type "person.created" :actor "google" :payload person})
        person)))

(defn- refused-page []
  {:status 403
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str "<!doctype html><html><body style=\"font-family:sans-serif; max-width:34em; margin:4em auto;\">"
              "<h2>That Google account isn't on a committee here</h2>"
              "<p>You signed in fine — but this email isn't on any event's "
              "Program Committee roster, and the roster is the permission. "
              "Ask an organizer to add you, then try again.</p>"
              "<p><a href=\"/login\">Back to sign in</a></p></body></html>")})

(defn handle-callback
  [req]
  ;; :keys, not :strs — wrap-keyword-params has already keywordized these.
  (let [{:keys [code state error]} (:params req)
        want-state (get-in req [:session :oauth-state])]
    (cond
      (some? error)
      (do (log/info :google-callback-denied :error error)
          {:status 303 :headers {"Location" "/login"} :body ""})

      (or (str/blank? code) (str/blank? state) (not= state want-state))
      (do (log/warn :google-callback-bad-state)
          {:status 303 :headers {"Location" "/login"} :body ""})

      :else
      (let [tokens (exchange-code! req code)
            claims (some-> (:id_token tokens) verify-id-token!)]
        (cond
          (nil? claims)
          (do (log/warn :google-callback-unverified)
              {:status 303 :headers {"Location" "/login"} :body ""})

          ;; Google proved WHO — now the roster decides WHETHER, identical to
          ;; the magic-link rule (plus the same first-run window).
          (or (auth/may-sign-in? (:email claims)) (auth/first-run?))
          (let [person (find-or-create-person! claims)]
            (log/info :google-signed-in :email (:email claims) :person-id (:id person))
            (auth/sign-in {:status 303
                           :session (dissoc (:session req) :oauth-state)
                           :headers {"Location" (auth/home-path person)}
                           :body ""}
                          (:id person)))

          :else
          (do (log/info :google-refused :email (:email claims))
              (refused-page)))))))
