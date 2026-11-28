(ns cfp-scheduler-killer.mail
  "Outbound email — configured or honestly not.

   Two modes, and the app never blurs them:

     **dev-render** (no SMTP configured, the default on a fresh clone): every
     send appends a `comms.rendered` event carrying the exact letter that WOULD
     have gone out. The UI says 'SMTP not configured'. Nothing anywhere claims a
     letter was sent, because claiming that is the single most damaging lie a
     conference tool can tell an organizer — they will stop chasing a speaker who
     never heard from them.

     **send** (SMTP configured): a real message goes out and we append
     `comms.sent` with the message id. That per-speaker send log is a direct ask
     from the team-planning ethnography — Ann needs to answer 'did the letter
     actually go out?' without trusting her memory, because Sessionize's mail
     landed in spam and she re-sent by hand.

   The `from` is ours; the **reply-to is the event's speaker support address**,
   so a speaker hitting reply reaches a real person. That is BusyConf's pattern
   and Sessionize's, and both were right.

   postal is resolved LAZILY: a clone with no SMTP config never loads it."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [cfp-scheduler-killer.store :as store]))

;; --- Configuration ----------------------------------------------------------

(defn- env [k] (some-> (System/getenv k) str/trim not-empty))

(defn config
  "SMTP settings from secrets/smtp.edn or the environment. nil when unconfigured
   — which is a supported state, not a failure."
  []
  (let [file (try (let [f (io/file "secrets/smtp.edn")]
                    (when (.exists f) (read-string (slurp f))))
                  (catch Exception e
                    (log/warn :smtp-edn-unreadable :msg (.getMessage e))
                    nil))
        from-env (when (env "SMTP_HOST")
                   {:host (env "SMTP_HOST")
                    :port (some-> (env "SMTP_PORT") Integer/parseInt)
                    :user (env "SMTP_USER")
                    :pass (env "SMTP_PASS")
                    :from (env "SMTP_FROM")})
        cfg (or file from-env)]
    (when (and cfg (not (str/blank? (str (:host cfg)))))
      (merge {:port 587} cfg))))

(defn configured? [] (some? (config)))

(defn status-line
  "What the UI says about delivery. Never optimistic."
  []
  (if-let [c (config)]
    (str "Sending via " (:host c) " as " (or (:from c) (:user c)))
    "SMTP not configured — letters render below and are recorded, not sent"))

;; --- Sending ----------------------------------------------------------------

(defn- send-via-postal!
  "Resolved lazily so the dependency is never touched on the zero-setup path."
  [cfg message]
  (let [send-message (requiring-resolve 'postal.core/send-message)]
    (send-message {:host (:host cfg)
                   :port (:port cfg)
                   :user (:user cfg)
                   :pass (:pass cfg)
                   :tls true}
                  message)))

(defn- ->postal-message
  [cfg {:keys [to subject body reply-to ics ics-filename]}]
  (cond-> {:from (or (:from cfg) (:user cfg))
           :to to
           :subject subject}
    reply-to (assoc :reply-to reply-to)
    ;; A plain body when there's no attachment; multipart when there is. Some
    ;; clients render a single-part multipart badly, so we don't wrap needlessly.
    (nil? ics) (assoc :body body)
    ics (assoc :body [{:type "text/plain; charset=utf-8" :content body}
                      {:type :attachment
                       :content-type "text/calendar; method=PUBLISH; charset=utf-8"
                       :file-name (or ics-filename "invite.ics")
                       :content (.getBytes ^String ics "UTF-8")}])))

(defn send!
  "Send a letter, or record that we would have.

   `letter` = {:to :subject :body :reply-to :ics :ics-filename}.
   `context` = {:event-id :kind :submission-id :person-id} — whatever makes the
   entry findable later.

   Returns {:mode :sent|:rendered|:failed …}. NEVER throws: a mail failure must
   not roll back the domain act that triggered it. Informing a speaker is a fact
   even if the SMTP server was down; the send log records the failure so someone
   can retry, which is exactly what Ann does by hand today."
  [letter context]
  (let [cfg (config)
        base (merge {:to (:to letter)
                     :subject (:subject letter)
                     :body (:body letter)
                     :reply-to (:reply-to letter)
                     :has-ics? (some? (:ics letter))
                     :kind (:kind context)
                     :submission-id (:submission-id context)
                     :person-id (:person-id context)
                     :at (store/now-iso)}
                    {})]
    (if-not cfg
      (do
        (store/append! {:type "comms.rendered" :actor (or (:actor context) "system")
                        :event-id (:event-id context)
                        :payload base})
        (log/info :comms-rendered :to (:to letter) :kind (:kind context))
        {:mode :rendered})
      (try
        (let [result (send-via-postal! cfg (->postal-message cfg letter))]
          (if (= :SUCCESS (:error result))
            (do
              (store/append! {:type "comms.sent" :actor (or (:actor context) "system")
                              :event-id (:event-id context)
                              :payload (assoc base :message-id
                                              (or (first (:message result))
                                                  (str (:code result))))})
              (log/info :comms-sent :to (:to letter) :kind (:kind context))
              {:mode :sent :result result})
            (do
              (store/append! {:type "comms.failed" :actor (or (:actor context) "system")
                              :event-id (:event-id context)
                              :payload (assoc base :error (str (:message result)))})
              (log/warn :comms-failed :to (:to letter) :error (:message result))
              {:mode :failed :result result})))
        (catch Exception e
          (store/append! {:type "comms.failed" :actor (or (:actor context) "system")
                          :event-id (:event-id context)
                          :payload (assoc base :error (.getMessage e))})
          (log/error :comms-error :to (:to letter) :msg (.getMessage e))
          {:mode :failed :error (.getMessage e)})))))

;; --- The send log -----------------------------------------------------------

(def comms-types #{"comms.sent" "comms.rendered" "comms.failed"})

(defn history
  "Every letter for one event, newest first. The answer to 'did it go out?'"
  [event-id]
  (->> (store/log-for-event event-id)
       (filter #(contains? comms-types (:type %)))
       reverse
       (mapv (fn [e]
               (assoc (:payload e)
                      :type (:type e)
                      :sent? (= "comms.sent" (:type e))
                      :actor (:actor e)
                      :when (:at e))))))

(defn history-for-person
  [event-id email]
  (filterv #(= (str/lower-case (str (:to %))) (str/lower-case (str email)))
           (history event-id)))
