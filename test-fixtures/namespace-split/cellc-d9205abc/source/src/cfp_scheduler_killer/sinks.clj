(ns cfp-scheduler-killer.sinks
  "Sink implementations that need the domain namespaces.

   `deliver-sink!` is a multimethod defined in `store`, deliberately with no
   methods beyond the generic ones — store must not depend on committees, mail
   or exports, or the dependency graph inverts. Methods that DO need those live
   here, and this namespace is required at boot for its side effects.

   Four sinks:
     :pc-push       the BusyConf push email — the full proposal to every
                    committee member, the moment it arrives
     :slack         the channel the committee already lives in
     :airtable      one-way row upsert, so a team's existing automations keep
                    working (bd 4u2)
     :gcs-snapshot  copy the event log off the box (bd durability)"
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [gcp-secrets.main :as gcp]
            [cfp-scheduler-killer.committees :as committees]
            [cfp-scheduler-killer.exports :as exports]
            [cfp-scheduler-killer.mail :as mail]
            [cfp-scheduler-killer.reviews :as reviews]
            [cfp-scheduler-killer.store :as store])
  (:import (java.net URI)
           (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers
                          HttpResponse$BodyHandlers)
           (java.time Duration)))

(defn public-base-url
  "The address a human would paste into a browser, for links we send OUT.

   A link in an email or a Slack message has to be absolute — a Slack message
   that says `/events/x/submissions/y` is a message nobody can click. When
   PUBLIC_BASE_URL is unset (a laptop) we fall back to the dev address rather
   than to the empty string, so the link still works for the person who is most
   likely reading it."
  []
  (or (some-> (System/getenv "PUBLIC_BASE_URL") str/trim not-empty)
      (str "http://localhost:" (or (System/getenv "PORT") "20500"))))

;; ---------------------------------------------------------------------------
;; :pc-push — the BusyConf push email
;; ---------------------------------------------------------------------------
;;
;; From the archaeology (docs/research/cfp-flow-comparison.md): BusyConf's
;; review "came to you". A New Proposal email carried the FULL proposal inline —
;; every custom field, the speaker bio, the private notes — with one orange
;; button. The committee read it over coffee, in their inbox, and the review
;; started there. Every tool since has sent "a new submission was received.
;; Log in to view it", and every one of them lost the committee to Slack.
;;
;; So this mail is deliberately long. The private notes-to-committee IS
;; included: this goes to the programming committee, which is the audience that
;; field was written for. It is the one place that answer belongs.

(defn- field-lines
  "Every answer under its own label, in the submission's own snapshot order."
  [submission]
  (let [answers (:answers submission)]
    (for [f (:form-snapshot submission)
          :when (not= "group" (name (:type f)))
          :let [v (get answers (keyword (name (:id f))))]
          :when (and v (not (str/blank? (str v))))]
      (str (:label f)
           (when (:private f) "  [PC ONLY]")
           "\n"
           (str/replace (str v) #"(?m)^" "    ")))))

(defn- speaker-lines [submission]
  (for [sp (:speakers submission)]
    (str/join "\n"
              (remove nil?
                      [(str "    " (:name sp) (when (:org sp) (str " — " (:org sp))))
                       (when (:title sp) (str "    " (:title sp)))
                       (str "    " (:email sp))
                       (when (not (str/blank? (str (:bio sp))))
                         (str "\n" (str/replace (str (:bio sp)) #"(?m)^" "    ")))
                       (when (:linkedin-url sp) (str "    " (:linkedin-url sp)))]))))

(defn push-email-body
  "The whole proposal, as text, ready to read without clicking anything.
   Public so it can be tested directly."
  [event submission board-url]
  (str "A new submission arrived for " (:name event) ".\n\n"
       (str/join "\n\n" (field-lines submission))
       "\n\nSPEAKER\n"
       (str/join "\n\n" (speaker-lines submission))
       "\n\n"
       "Rate and comment: " board-url "\n\n"
       "You are getting this because you are on the programming committee for "
       (:name event) ". Reply to this note and it reaches the organizers.\n"))

(defn pc-push-enabled?
  [event]
  (not (false? (get-in event [:settings :pc-push-enabled]))))

(defn committee-recipients
  "Every member of every committee on this event, deduped by email."
  [event]
  (->> (store/committees-for-event (:id event))
       (mapcat #(committees/members-for-committee (:id %)))
       (map (fn [m] {:name (:name m) :email (:email m)}))
       (remove #(str/blank? (str (:email %))))
       (reduce (fn [acc m]
                 (if (some #(= (:email %) (:email m)) acc) acc (conj acc m)))
               [])))

(defmethod store/deliver-sink! :pc-push
  [{:keys [base-url]} event-record]
  (when (= "submission.created" (:type event-record))
    (let [submission (:payload event-record)
          event (store/get-event-by-id (:event-id submission))]
      (when (and event (pc-push-enabled? event))
        (let [board-url (str (or base-url "") "/events/" (:slug event)
                             "/submissions/" (:id submission))
              body (push-email-body event submission board-url)
              title (get-in submission [:answers :talk-title])]
          (doseq [m (committee-recipients event)]
            (mail/send!
             {:to (:email m)
              :subject (str "[" (:name event) "] New submission: " title)
              :reply-to (:support-email event)
              :body body}
             {:event-id (:id event) :kind "pc-push" :actor "system"
              :submission-id (:id submission)})))))))

;; ---------------------------------------------------------------------------
;; :slack — the channel the committee already lives in
;; ---------------------------------------------------------------------------
;;
;; The archaeology (docs/research/cfp-flow-comparison.md) says every tool since
;; BusyConf lost its committee to Slack. Rather than fight that, meet them
;; there: the channel gets a message worth reading — the talk, the speaker, the
;; new average, and a link straight to the row — so the conversation starts in
;; Slack and finishes on the board.
;;
;; Two rules shape everything below:
;;
;;   1. NO PRIVATE ANSWERS, EVER. A Slack channel is a wider audience than the
;;      programming committee (integrations, guests, exports, search). The
;;      notes-to-committee field is exactly the thing that must not leak, so the
;;      messages here carry title / speaker / org / format and nothing else from
;;      the form. The push EMAIL may carry the private note because its audience
;;      is the committee by construction; a channel is not.
;;   2. COMPACT. This lands in a busy channel. Two blocks, one link, no
;;      paragraph of an abstract.

(def slack-event-groups
  "What an organizer chooses from — four things that happen at a CFP, not a list
   of our event-type strings.

   Grouping is the whole design here. 'submission.status-changed,
   submission.priority-toggled' is our vocabulary; 'A decision is made' is
   theirs, and it is the one they can answer 'yes, tell me about that' to.
   Arrival and decisions are on by default: the two moments somebody who is not
   in the tool still needs to know about."
  [{:key "arrival"  :label "A talk arrives"
    :help "Every new submission, as it lands."
    :types ["submission.created"] :default? true}
   {:key "review"   :label "The committee acts"
    :help "Ratings and comments. Chatty on a big CFP — a channel of its own suits this one."
    :types ["rating.set" "comment.added"] :default? false}
   {:key "decision" :label "A decision is made"
    :help "Accepted, declined, queued, or flagged for the call."
    :types ["submission.status-changed" "submission.priority-toggled"] :default? true}
   {:key "notified" :label "A speaker is told"
    :help "The moment a decision actually reaches the speaker."
    :types ["submission.notified"] :default? false}])

(def default-slack-groups
  (mapv :key (filter :default? slack-event-groups)))

(defn types-for-groups
  "The event types behind a set of group keys, in group order. An unknown key is
   ignored rather than throwing — the checkbox list is user input."
  [groups]
  (let [chosen (set (map str groups))]
    (vec (mapcat :types (filter #(contains? chosen (:key %)) slack-event-groups)))))

(defn groups-from-types
  "The inverse, for re-rendering the Settings form from what was stored."
  [types]
  (let [ts (set (map str types))]
    (mapv :key (filter #(some ts (:types %)) slack-event-groups))))

;; --- Message building (pure — no network, fully testable) -------------------

(def ^:private max-comment-chars 280)

(defn- truncate [s n]
  (let [s (str s)]
    (if (> (count s) n) (str (subs s 0 (dec n)) "…") s)))

(defn- esc
  "Slack mrkdwn escaping — only the three characters that carry meaning."
  [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn submission-url
  [base-url event submission]
  (str base-url "/events/" (:slug event) "/submissions/" (:id submission)))

(defn- link
  "Slack's link syntax. The talk title IS the link — nobody wants a bare URL."
  [url label]
  (str "<" url "|" (esc (truncate label 120)) ">"))

(defn- section [mrkdwn]
  {"type" "section" "text" {"type" "mrkdwn" "text" mrkdwn}})

(defn- context [mrkdwn]
  {"type" "context" "elements" [{"type" "mrkdwn" "text" mrkdwn}]})

(defn- speaker-line [submission]
  (let [sp (first (:speakers submission))]
    (str/join " · " (remove str/blank?
                            [(str (:name sp))
                             (str (or (:org sp) ""))
                             (str (or (get-in submission [:answers :session-format]) ""))]))))

(defn- who [person-id actor]
  (or (:name (store/person-by-id person-id))
      (some-> actor str not-empty)
      "Someone"))

(defn- fmt-stars [x]
  (when x (let [d (double x)]
            (if (== d (Math/floor d)) (str (int d)) (format "%.1f" d)))))

(defn slack-message
  "The message for one stored event, as Slack's JSON body.

   `:text` is the notification preview (the line in the sidebar and on a phone),
   `:blocks` is what renders in the channel. Both are built here so a message
   can be asserted in a test without a socket, which is how the shapes below are
   covered.

   Returns nil for an event this sink has nothing to say about — a nil is how
   the sink declines to post rather than posting 'Event: form.updated'."
  [base-url event-record]
  (let [type (:type event-record)
        payload (:payload event-record)
        actor (:actor event-record)
        sid (or (:submission-id payload) (when (= "submission.created" type) (:id payload)))
        submission (when sid (store/submission-by-id sid))
        event (when submission (store/get-event-by-id (:event-id submission)))
        title (or (get-in submission [:answers :talk-title]) "(untitled)")
        url (when (and event submission) (submission-url base-url event submission))
        ev-name (or (:name event) "")]
    (when submission
      (case type
        "submission.created"
        {"text" (str "New submission: " title " — " (speaker-line submission))
         "blocks" [(section (str "📥 *New submission*\n" (link url title)))
                   (context (str (esc (speaker-line submission))
                                 (when (seq ev-name) (str "  ·  " (esc ev-name)))))]}

        "rating.set"
        (let [stats (reviews/submission-stats sid)
              prev (:previous-stars payload)]
          {"text" (str (who (:person-id payload) actor) " rated " title " "
                       (fmt-stars (:stars payload))
                       " — now averaging " (or (fmt-stars (:mean stats)) "—")
                       " from " (:n stats))
           "blocks" [(section (str "★ *" (fmt-stars (:stars payload)) "* from *"
                                   (esc (who (:person-id payload) actor)) "*"
                                   (when prev (str " (was " (fmt-stars prev) ")"))
                                   " on " (link url title)))
                     (context (str "Now averaging *" (or (fmt-stars (:mean stats)) "—")
                                   "* from " (:n stats) " review"
                                   (when (not= 1 (:n stats)) "s")
                                   (when (:split? stats) "  ·  *split*")
                                   (when (seq ev-name) (str "  ·  " (esc ev-name)))))]})

        "comment.added"
        {"text" (str (who (:person-id payload) actor) " commented on " title ": "
                     (truncate (:body payload) max-comment-chars))
         "blocks" [(section (str "💬 *" (esc (who (:person-id payload) actor)) "* on "
                                 (link url title)
                                 "\n>" (esc (truncate (:body payload) max-comment-chars))))
                   (context (esc ev-name))]}

        "submission.status-changed"
        {"text" (str title ": " (:from payload) " → " (:to payload)
                     " (by " (who nil (or (:actor payload) actor)) ")")
         "blocks" [(section (str "*" (esc (str (:from payload))) "* → *"
                                 (esc (str (:to payload))) "*  " (link url title)))
                   (context (str "by " (esc (who nil (or (:actor payload) actor)))
                                 (when (seq ev-name) (str "  ·  " (esc ev-name)))))]}

        "submission.priority-toggled"
        (if (:priority payload)
          {"text" (str "Flagged for discussion: " title)
           "blocks" [(section (str "🔥 *Flagged for the call*  " (link url title)))
                     (context (str "by " (esc (who nil actor))
                                   (when (seq ev-name) (str "  ·  " (esc ev-name)))))]}
          {"text" (str "Unflagged: " title)
           "blocks" [(section (str "Unflagged  " (link url title)))
                     (context (str "by " (esc (who nil actor))))]})

        "submission.notified"
        {"text" (str "Speaker told — " (:status-at-notify payload) ": " title)
         "blocks" [(section (str "📣 *Speaker told — " (esc (str (:status-at-notify payload)))
                                 "*  " (link url title)))
                   (context (str (esc (str (:to payload)))
                                 (when (seq ev-name) (str "  ·  " (esc ev-name)))))]}

        ;; Anything else: the plain sentence `store` already knows how to write.
        {"text" (store/slack-text event-record)}))))

;; --- Delivery ---------------------------------------------------------------

(def ^:dynamic *slack-post*
  "Rebindable so tests (and `bin/sink_probe.py`) can capture the exact body
   without a network. (fn [url body-map] => {:status int :body string})."
  nil)

(defn slack-post!
  "POST a JSON body to an incoming-webhook URL.

   Slack answers 200 with the body `ok`, or a 4xx whose BODY is the actual
   reason (`invalid_payload`, `no_service`, `channel_not_found`). That body is
   the only useful diagnostic there is, so it is carried back rather than
   swallowed — the Settings page shows it to the organizer verbatim."
  [url body]
  (let [client (-> (HttpClient/newBuilder)
                   (.connectTimeout (Duration/ofSeconds 5))
                   (.build))
        req (-> (HttpRequest/newBuilder (URI/create url))
                (.timeout (Duration/ofSeconds 10))
                (.header "Content-Type" "application/json")
                (.POST (HttpRequest$BodyPublishers/ofString (json/write-str body)))
                (.build))
        resp (.send client req (HttpResponse$BodyHandlers/ofString))]
    {:status (.statusCode resp) :body (.body resp)}))

(defn post-slack!
  "One place that decides which poster to use, so every caller — the sink, the
   test button, the tests — goes through the same door."
  [url body]
  ((or *slack-post* slack-post!) url body))

(defmethod store/deliver-sink! :slack
  [{:keys [webhook-url base-url]} event-record]
  ;; A failing Slack post must NEVER fail a speaker's submission. It doesn't:
  ;; `store/fire-sinks!` runs every sink in its own future and catches, so the
  ;; throw below only ever marks the delivery failed on the Settings page. This
  ;; sink therefore throws LOUDLY on a 4xx rather than returning quietly —
  ;; being loud here is safe precisely because the boundary is elsewhere.
  (when-let [body (slack-message (or base-url (public-base-url)) event-record)]
    (let [{:keys [status] :as resp} (post-slack! webhook-url body)]
      (when (and status (>= status 400))
        (throw (ex-info (str "Slack returned " status ": " (:body resp))
                        {:type :slack-error :status status :body (:body resp)})))
      resp)))

(defn test-message
  "What the 'Send a test message' button posts. Deliberately shaped like a real
   one — same two blocks — so what an organizer sees in the channel is what they
   will get at 2am when a talk arrives."
  [event who-said]
  {"text" (str "Test message from the CFP tool for " (:name event))
   "blocks" [(section (str "✅ *This channel is wired up for " (esc (:name event)) "*"))
             (context (str "Sent by " (esc (str who-said))
                           " from Settings. Real messages carry the talk, the "
                           "speaker and a link straight to the row."))]})

(defn send-test-message!
  "Post the test and say, in plain language, what happened. Never throws — the
   Settings page needs an answer, not a stack trace."
  [event webhook-url who-said]
  (cond
    (str/blank? (str webhook-url))
    {:ok false :message "No Slack webhook is set for this event yet."}

    :else
    (try
      (let [{:keys [status body]} (post-slack! webhook-url (test-message event who-said))]
        (if (and status (< status 400))
          {:ok true :message "Sent. Check the channel — it should be there now."}
          {:ok false
           :message (str "Slack refused it (HTTP " status
                         (when-not (str/blank? (str body)) (str ": " (str/trim (str body))))
                         "). "
                         (case (str/trim (str body))
                           "no_service" "That webhook has been deleted in Slack — create a new one."
                           "channel_not_found" "The channel the webhook points at is gone."
                           "invalid_payload" "Slack did not accept the message body; this is our bug — please report it."
                           "Check the URL is the full https://hooks.slack.com/services/… address."))}))
      (catch Exception e
        (log/warn :slack-test-failed :msg (.getMessage e))
        {:ok false :message (str "Could not reach Slack: " (.getMessage e))}))))

;; ---------------------------------------------------------------------------
;; :airtable — one-way row sync
;; ---------------------------------------------------------------------------
;;
;; A team's "when a new row appears, do X" automations are load-bearing and were
;; built long before us. Mirroring rows one-way keeps them working, so adopting
;; this tool costs them nothing they already rely on.
;;
;; ONE-WAY on purpose: we never read Airtable back. Two-way sync between two
;; systems that both think they own the record is how you get a data-loss story.

(def ^:dynamic *http-post*
  "Rebindable so tests can assert the payload without a network."
  nil)

(defn- http-post-json
  [url token body]
  (let [client (-> (HttpClient/newBuilder)
                   (.connectTimeout (Duration/ofSeconds 5))
                   (.build))
        req (-> (HttpRequest/newBuilder (URI/create url))
                (.timeout (Duration/ofSeconds 10))
                (.header "Authorization" (str "Bearer " token))
                (.header "Content-Type" "application/json")
                (.POST (HttpRequest$BodyPublishers/ofString (json/write-str body)))
                (.build))
        resp (.send client req (HttpResponse$BodyHandlers/ofString))]
    {:status (.statusCode resp) :body (.body resp)}))

(defn airtable-fields
  "The row we mirror. PUBLIC answers only — the same exclusion the exports use,
   so a private note cannot reach a third-party base by any path."
  [event submission base-url]
  (let [answers (exports/public-answers submission)
        sp (first (:speakers submission))]
    {"Title" (str (:talk-title answers))
     "Speaker" (str (:name sp))
     "Speaker Email" (str (:email sp))
     "Organization" (str (or (:org sp) ""))
     "Format" (str (or (:session-format answers) ""))
     "Org Size" (str (or (:org-size answers) ""))
     "Industry" (str (or (:industry answers) ""))
     "Status" (str (:status submission))
     "Notified" (boolean (:notified-at submission))
     "Submitted At" (str (:created-at submission))
     "URL" (str (or base-url "") "/events/" (:slug event)
                "/submissions/" (:id submission))
     "Submission ID" (str (:id submission))}))

(defn airtable-payload
  "Airtable's upsert shape: merge on our own id so a status change updates the
   existing row instead of appending a second one."
  [event submission base-url]
  {"performUpsert" {"fieldsToMergeOn" ["Submission ID"]}
   "records" [{"fields" (airtable-fields event submission base-url)}]})

(def airtable-event-types
  #{"submission.created" "submission.status-changed" "submission.notified"})

(defmethod store/deliver-sink! :airtable
  [{:keys [base-id table token base-url]} event-record]
  (when (contains? airtable-event-types (:type event-record))
    (let [payload (:payload event-record)
          sid (or (:id payload) (:submission-id payload))
          submission (store/submission-by-id sid)
          event (when submission (store/get-event-by-id (:event-id submission)))]
      (when (and submission event)
        (let [url (str "https://api.airtable.com/v0/" base-id "/"
                       (java.net.URLEncoder/encode (str table) "UTF-8"))
              body (airtable-payload event submission base-url)
              post (or *http-post* http-post-json)
              {:keys [status] :as resp} (post url token body)]
          (when (and status (>= status 400))
            (throw (ex-info (str "Airtable returned " status)
                            {:type :airtable-error :status status :body (:body resp)})))
          resp)))))

;; ---------------------------------------------------------------------------
;; :gcs-snapshot — get the log off the box
;; ---------------------------------------------------------------------------
;;
;; The store is one append-only file. That is a feature until the disk goes
;; away, so we copy it somewhere durable after writes settle.
;;
;; DEBOUNCED: a replay appends 190 events in a second; copying 190 times would
;; be absurd. We copy at most once per quiet period, and always copy the whole
;; current file, so a missed intermediate state costs nothing.

(def snapshot-debounce-ms 5000)

(defonce ^:private snapshot-pending (atom nil))

(defn gcloud-copy!
  "Shell out to `gcloud storage cp` — the laptop fallback. The DEPLOYED
   container is distroless and has no gcloud binary, which is why rest-copy!
   below exists and is the default."
  [src dest]
  (let [{:keys [exit err]} (shell/sh "gcloud" "storage" "cp" src dest)]
    (if (zero? exit)
      {:ok true}
      (do (log/warn :gcs-snapshot-failed :dest dest :err (str/trim (str err)))
          {:ok false :error err}))))

(defn parse-gs-uri
  "gs://bucket/path/to/object -> {:bucket .. :object ..}; nil for anything else."
  [s]
  (when-let [[_ b o] (re-matches #"gs://([^/]+)/(.+)" (str s))]
    {:bucket b :object o}))

(defn- gcs-object-url
  "The JSON-API URL for an object; object names are URL-encoded whole (slashes
   become %2F — that is what the API requires in the path position)."
  [{:keys [bucket object]} & [upload?]]
  (if upload?
    (str "https://storage.googleapis.com/upload/storage/v1/b/" bucket
         "/o?uploadType=media&name=" (java.net.URLEncoder/encode ^String object "UTF-8"))
    (str "https://storage.googleapis.com/storage/v1/b/" bucket
         "/o/" (java.net.URLEncoder/encode ^String object "UTF-8") "?alt=media")))

(defn rest-copy!
  "Copy via the GCS JSON API over java.net.http — zero new deps, no gcloud
   binary, which is the whole point: this is the path that works inside the
   distroless Cloud Run container (bd iy9, ratified 2026-08-09). Auth comes
   from gcp-secrets/get-token (ADC on the laptop, metadata server on Cloud
   Run). Direction is derived from which side is gs://. Same {:ok bool}
   contract as gcloud-copy!."
  [src dest]
  (try
    (let [up   (parse-gs-uri dest)
          down (parse-gs-uri src)]
      (if-not (or up down)
        (do (log/warn :gcs-copy-no-gs-side :src src :dest dest)
            {:ok false :error "neither side is gs://"})
        (let [token  (gcp/get-token)
              client (java.net.http.HttpClient/newHttpClient)]
          (if up
            (let [req  (-> (java.net.http.HttpRequest/newBuilder
                            (java.net.URI/create (gcs-object-url up :upload)))
                           (.header "Authorization" (str "Bearer " token))
                           (.header "Content-Type" "application/octet-stream")
                           (.POST (java.net.http.HttpRequest$BodyPublishers/ofFile
                                   (.toPath (io/file src))))
                           (.build))
                  resp (.send client req (java.net.http.HttpResponse$BodyHandlers/ofString))]
              (if (< (.statusCode resp) 300)
                {:ok true}
                (do (log/warn :gcs-rest-upload-failed :status (.statusCode resp) :dest dest)
                    {:ok false :error (str "HTTP " (.statusCode resp))})))
            (let [req  (-> (java.net.http.HttpRequest/newBuilder
                            (java.net.URI/create (gcs-object-url down)))
                           (.header "Authorization" (str "Bearer " token))
                           (.GET)
                           (.build))
                  resp (.send client req (java.net.http.HttpResponse$BodyHandlers/ofByteArray))]
              (if (< (.statusCode resp) 300)
                (do (io/make-parents (io/file dest))
                    (with-open [out (io/output-stream (io/file dest))]
                      (.write out ^bytes (.body resp)))
                    {:ok true})
                (do (log/warn :gcs-rest-download-failed :status (.statusCode resp) :src src)
                    {:ok false :error (str "HTTP " (.statusCode resp))})))))))
    (catch Exception e
      (log/warn :gcs-rest-copy-error :msg (.getMessage e))
      {:ok false :error (.getMessage e)})))

(defn copy!
  "REST first — the only path that exists in the deployed container — with the
   gcloud shell as a laptop fallback for a machine without usable ADC."
  [src dest]
  (let [r (rest-copy! src dest)]
    (if (:ok r) r (gcloud-copy! src dest))))

(def ^:dynamic *copy-fn*
  "Rebindable for tests: (fn [src dest] {:ok bool})."
  copy!)

(defn snapshot-destination
  ([] (snapshot-destination (System/getenv "GCS_SNAPSHOT_BUCKET")))
  ([bucket]
   (when-let [b (some-> bucket str/trim not-empty)]
     (str (if (str/starts-with? b "gs://") b (str "gs://" b))
          "/store/events.jsonl"))))

(defn snapshot-now!
  "Copy the current log to its destination. Returns nil when unconfigured."
  ([] (snapshot-now! (snapshot-destination) @store/store-path))
  ([dest src]
   (when (and dest src (.exists (io/file src)))
     (let [r (*copy-fn* src dest)]
       (when (:ok r) (log/info :gcs-snapshot-written :dest dest))
       r))))

(defmethod store/deliver-sink! :gcs-snapshot
  [{:keys [dest debounce-ms]} _event]
  (let [dest (or dest (snapshot-destination))
        wait (or debounce-ms snapshot-debounce-ms)]
    (when dest
      ;; Coalesce: one timer at a time, restarted by each new append.
      (let [token (Object.)]
        (reset! snapshot-pending token)
        (future
          (Thread/sleep wait)
          (when (identical? token @snapshot-pending)
            (snapshot-now! dest @store/store-path)))))))

(defn restore-from-snapshot!
  "Boot-time restore. Default rule: pull the snapshot only when there is NO
   local log — the laptop's local log is the authority, and clobbering it with
   a possibly-older copy is the accident this prevents.

   The deployed container inverts that: its local file is the IMMUTABLE BAKED
   SEED, identical on every cold start, so 'local exists' is true forever and
   the judge's state would never come back (found at the meter, 2026-08-10:
   :gcs-restore-skipped on every boot). GCS_RESTORE_MODE=prefer-snapshot makes
   the snapshot win at boot; a failed/absent download leaves the local file
   untouched (rest-copy! writes only on 2xx), so a fresh bucket still boots
   the baked seed."
  ([] (restore-from-snapshot! (snapshot-destination) @store/store-path
                              (System/getenv "GCS_RESTORE_MODE")))
  ([dest src] (restore-from-snapshot! dest src nil))
  ([dest src mode]
   (when (and dest src)
     (let [f (io/file src)
           prefer? (= "prefer-snapshot" mode)]
       (cond
         (and (.exists f) (not prefer?))
         (do (log/info :gcs-restore-skipped :reason "local store already exists") nil)

         :else
         (do (io/make-parents f)
             (let [r (*copy-fn* dest src)]
               (if (:ok r)
                 (do (log/info :gcs-restore-ok :from dest
                               :mode (if prefer? "prefer-snapshot" "fresh-box"))
                     true)
                 (do (log/warn :gcs-restore-failed :from dest
                               :keeping-local (.exists f))
                     nil)))))))))

;; --- Default sinks ----------------------------------------------------------

(defn default-sinks
  "Sinks every event gets without being configured: the PC push email, and the
   durability snapshot when a bucket is set."
  [event base-url]
  (cond-> []
    (pc-push-enabled? event)
    (conj {:type :pc-push :base-url base-url :events ["submission.created"]})

    (snapshot-destination)
    (conj {:type :gcs-snapshot})))

;; Install the defaults into the store's fan-out. Done here (not in store) so
;; the dependency only ever points one way: sinks -> store.
(alter-var-root #'store/*default-sinks-fn*
                (constantly (fn [event] (default-sinks event (System/getenv "PUBLIC_BASE_URL")))))
