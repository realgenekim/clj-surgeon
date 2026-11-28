(ns cfp-scheduler-killer.comms-test
  "The mailer and quick capture.

   The mailer's contract is mostly a promise about HONESTY: with no SMTP
   configured it must record what it would have sent and never imply it went.
   Every test here runs with no network — the configured path uses a stub
   transport."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [ring.mock.request :as mock]
            [cfp-scheduler-killer.auth :as auth]
            [cfp-scheduler-killer.committees :as committees]
            [cfp-scheduler-killer.events :as events]
            [cfp-scheduler-killer.exports :as exports]
            [cfp-scheduler-killer.inform :as inform]
            [cfp-scheduler-killer.mail :as mail]
            [cfp-scheduler-killer.reviews :as reviews]
            [cfp-scheduler-killer.server :as server]
            [cfp-scheduler-killer.store :as store]
            [cfp-scheduler-killer.submissions :as sub]
            [cfp-scheduler-killer.test-helpers :refer [with-temp-store]])
  (:import (java.time LocalDate LocalDateTime)))

(use-fixtures :each with-temp-store (fn [f] (reset! auth/tokens {}) (f)))

(defn- setup! []
  (let [event (events/create-event!
               {:name "Comms Test Summit" :slug "comms-test" :tz "America/New_York"
                :starts-on (LocalDate/of 2026 10 14) :ends-on (LocalDate/of 2026 10 15)
                :location "Charlotte, NC" :support-email "annp@example.com"
                :cfp-opens-at (LocalDateTime/of 2020 1 1 0 0)
                :cfp-closes-at (LocalDateTime/of 2099 1 1 0 0)}
               "kaocha")
        cid (:id (first (events/committees-for-event (:id event))))
        _ (committees/add-member! cid {:name "Gene Kim" :email "gene@example.com"
                                       :role "chair"} "kaocha")
        ff (:fields (events/form-for-event (:id event)))
        params {:answer-talk-title "A talk" :answer-abstract "Abstract."
                :answer-session-format "Experience Report"
                :answer-org-size ">10,000" :answer-industry "Insurance"
                :answer-ai-transformation-history "2023."
                :answer-measurable-outcomes "Numbers."
                :speaker-name "Priya Raghavan" :speaker-email "priya@example.com"
                :speaker-title "VP" :speaker-org "Meridian" :speaker-bio "Bio."}
        s (sub/create-submission! event (sub/parse-answers ff params)
                                  (sub/parse-speaker params) "form" "kaocha")]
    {:event (events/event-by-slug "comms-test") :submission s}))

(defn- comms-events [event-id kind]
  (filterv #(= kind (:kind %)) (mail/history event-id)))

;; --- Dev-render mode --------------------------------------------------------

(deftest dev-render-mode-test
  (testing "with no SMTP config we are in render mode, and say so"
    (is (not (mail/configured?)))
    (is (str/includes? (mail/status-line) "not configured"))
    (is (not (str/includes? (mail/status-line) "Sending"))))

  (let [{:keys [event submission]} (setup!)]
    (testing "a submission produced exactly ONE confirmation, recorded not sent"
      (let [confs (comms-events (:id event) "submission-confirmation")]
        (is (= 1 (count confs)))
        (is (= "priya@example.com" (:to (first confs))))
        (is (str/includes? (:subject (first confs)) "We received your talk"))
        (is (= "comms.rendered" (:type (first confs))))
        (is (false? (:sent? (first confs))))))

    (testing "the recorded letter is the WHOLE letter, so it can be sent by hand"
      (let [c (first (comms-events (:id event) "submission-confirmation"))]
        (is (str/includes? (:body c) "Hi Priya,"))
        (is (str/includes? (:body c) "\"A talk\""))
        (is (str/includes? (:body c) "annp@example.com"))))

    (testing "NOTHING anywhere claims it was sent"
      (is (empty? (filter #(= "comms.sent" (:type %)) (mail/history (:id event))))))

    (testing "the Log narrates it as 'Would send'"
      (let [e (first (filter #(= "comms.rendered" (:type %))
                             (store/log-for-event (:id event))))]
        (is (str/includes? (@#'cfp-scheduler-killer.views/log-summary e) "Would send"))))))

(deftest inform-sends-one-letter-test
  (let [{:keys [event submission]} (setup!)]
    (reviews/set-status! (:id submission) "Accepted" "gene@example.com")
    (inform/inform! event (store/submission-by-id (:id submission)) "gene@example.com")

    (testing "informing produced exactly one decision letter"
      (let [ds (comms-events (:id event) "decision")]
        (is (= 1 (count ds)))
        (is (= "priya@example.com" (:to (first ds))))
        (is (str/includes? (:subject (first ds)) "accepted"))))

    (testing "an ACCEPTANCE carries the calendar invite"
      (is (true? (:has-ics? (first (comms-events (:id event) "decision"))))))

    (testing "reply-to is the event's speaker support address — a reply reaches a person"
      (is (= "annp@example.com" (:reply-to (first (comms-events (:id event) "decision"))))))

    (testing "informing twice does not send twice"
      (inform/inform! event (store/submission-by-id (:id submission)) "gene@example.com")
      (is (= 1 (count (comms-events (:id event) "decision")))))))

(deftest decline-carries-no-invite-test
  (let [{:keys [event submission]} (setup!)]
    (reviews/set-status! (:id submission) "Declined" "gene@example.com")
    (inform/inform! event (store/submission-by-id (:id submission)) "gene@example.com")
    (testing "a decline is a letter, never a calendar invite"
      (let [d (first (comms-events (:id event) "decision"))]
        (is (some? d))
        (is (false? (boolean (:has-ics? d))))))))

(deftest ics-attachment-matches-the-feed-test
  (let [{:keys [event submission]} (setup!)]
    (reviews/set-status! (:id submission) "Accepted" "gene@example.com")
    (inform/inform! event (store/submission-by-id (:id submission)) "gene@example.com")
    (let [fresh (store/submission-by-id (:id submission))
          attached (exports/submission-ics event fresh)
          feed (exports/calendar-ics event)
          uid (exports/ics-uid fresh)]

      (testing "the attached invite is a complete, single-event calendar"
        (is (str/starts-with? attached "BEGIN:VCALENDAR\r\n"))
        (is (str/ends-with? attached "END:VCALENDAR\r\n"))
        (is (= 1 (count (re-seq #"BEGIN:VEVENT" attached)))))

      (testing "its UID is IDENTICAL to the public feed's — the same calendar entry"
        (is (str/includes? attached (str "UID:" uid)))
        (is (str/includes? feed (str "UID:" uid))))

      (testing "and so is its SEQUENCE, so a later move amends what they already have"
        (let [seq-of (fn [ics] (second (re-find #"SEQUENCE:(\d+)" ics)))]
          (is (= (seq-of attached) (seq-of feed))))))))

(deftest magic-link-email-test
  (let [{:keys [event]} (setup!)]
    (auth/issue-token! "gene@example.com")
    (testing "requesting a link produces exactly one magic-link letter"
      (let [ls (filterv #(= "magic-link" (:kind %)) (mail/history nil))]
        ;; magic-link has no event context, so it is not in the event's history
        (is (empty? (comms-events (:id event) "magic-link")))))

    (testing "…and it is in the global log with the token in the body"
      (let [e (last (filter #(= "magic-link" (get-in % [:payload :kind]))
                            (store/read-events)))]
        (is (some? e))
        (is (= "gene@example.com" (get-in e [:payload :to])))
        (is (str/includes? (get-in e [:payload :body]) "/auth/"))
        (is (= "comms.rendered" (:type e)) "not claimed as sent")))))

;; --- Configured mode (stubbed transport, no network) ------------------------

(def ^:private smtp-config
  {:host "smtp.example.com" :port 587 :user "u" :pass "p"
   :from "cfp@example.com"})

(deftest configured-send-test
  (let [{:keys [event submission]} (setup!)
        ;; Drain the submission's async PC push BEFORE configuring SMTP, so we
        ;; are measuring the decision letter and not a straggler from setup.
        _ (store/await-sinks!)
        sent (atom [])]
    (with-redefs [mail/config (constantly smtp-config)
                  ;; Stand in for the network. postal is never loaded.
                  cfp-scheduler-killer.mail/send-via-postal!
                  (fn [_cfg message]
                    (swap! sent conj message)
                    {:error :SUCCESS :message ["<abc123@example.com>"]})]

      (testing "the status line now names the host and the from address"
        (is (mail/configured?))
        (is (str/includes? (mail/status-line) "smtp.example.com"))
        (is (str/includes? (mail/status-line) "cfp@example.com")))

      (reviews/set-status! (:id submission) "Accepted" "gene@example.com")
      (inform/inform! event (store/submission-by-id (:id submission)) "gene@example.com")

      (testing "a real message was handed to the transport"
        (is (= 1 (count @sent)))
        (let [m (first @sent)]
          (is (= "priya@example.com" (:to m)))
          (is (= "cfp@example.com" (:from m)) "from is OURS")
          (is (= "annp@example.com" (:reply-to m)) "reply-to is the EVENT's")
          (is (str/includes? (:subject m) "accepted"))))

      (testing "the acceptance is multipart with a text/calendar attachment"
        (let [body (:body (first @sent))]
          (is (vector? body))
          (is (= 2 (count body)))
          (is (str/includes? (:type (first body)) "text/plain"))
          (is (= :attachment (:type (second body))))
          (is (str/includes? (:content-type (second body)) "text/calendar"))
          (is (str/includes? (String. ^bytes (:content (second body)) "UTF-8")
                             "BEGIN:VCALENDAR"))))

      (testing "it is recorded as SENT, with the message id"
        (let [d (first (comms-events (:id event) "decision"))]
          (is (= "comms.sent" (:type d)))
          (is (true? (:sent? d)))
          (is (= "<abc123@example.com>" (:message-id d))))))))

(deftest send-failure-is-recorded-not-thrown-test
  (let [{:keys [event submission]} (setup!)
        _ (store/await-sinks!)]
    (with-redefs [mail/config (constantly smtp-config)
                  cfp-scheduler-killer.mail/send-via-postal!
                  (fn [_ _] (throw (RuntimeException. "connection refused")))]
      (reviews/set-status! (:id submission) "Accepted" "gene@example.com")

      (testing "a dead SMTP server does not roll back the decision"
        (is (some? (inform/inform! event (store/submission-by-id (:id submission))
                                   "gene@example.com")))
        (is (some? (:notified-at (store/submission-by-id (:id submission))))
            "the speaker IS informed — the fact survives the failed delivery"))

      (testing "and the failure is visible so someone can retry"
        (let [d (first (comms-events (:id event) "decision"))]
          (is (= "comms.failed" (:type d)))
          (is (false? (:sent? d)))
          (is (str/includes? (:error d) "connection refused")))))))

(deftest comms-page-test
  (let [{:keys [event submission]} (setup!)
        handler (server/create-app)
        cookie (let [t (auth/issue-token! "gene@example.com")
                     r (handler (mock/request :get (str "/auth/" t)))]
                 (first (str/split (first (get-in r [:headers "Set-Cookie"])) #";")))
        as #(mock/header % "cookie" cookie)]
    (reviews/set-status! (:id submission) "Accepted" "gene@example.com")
    (inform/inform! event (store/submission-by-id (:id submission)) "gene@example.com")
    (store/await-sinks!)

    (let [body (:body (handler (as (mock/request :get "/events/comms-test/comms"))))]
      (testing "the page lists every letter with its true state"
        ;; confirmation to the speaker · PC push to the one committee member ·
        ;; the decision letter
        (is (str/includes? body "Send history (3)"))
        (is (str/includes? body "submission-confirmation"))
        (is (str/includes? body "pc-push"))
        (is (str/includes? body "decision"))
        (is (str/includes? body "would send"))
        (is (not (str/includes? body ">sent<")))
        (is (str/includes? body "priya@example.com"))
        (is (str/includes? body "calendar invite attached")))

      (testing "it is loud about not being configured"
        (is (str/includes? body "Nothing is being emailed"))
        (is (str/includes? body "secrets/smtp.edn")))

      (testing "the templates are shown read-only"
        (is (str/includes? body "Letter templates"))
        (is (str/includes? body "Editing these in the app arrives later"))))

    (testing "the inform page banner reflects the config state"
      (let [body (:body (handler (as (mock/request :get "/events/comms-test/inform"))))]
        (is (str/includes? body "SMTP is not configured, so nothing is emailed"))))))

;; --- Quick capture ----------------------------------------------------------

(deftest capture-test
  (let [{:keys [event]} (setup!)
        paste "Hi Gene — I'd love to talk about how we rebuilt our deploy pipeline.\n\n— Sam"]

    (testing "a capture with only a paste still creates a real submission"
      (let [s (sub/capture! event {:captured-text paste} "gene@example.com")]
        (is (some? s))
        (is (= "Pending" (:status s)))
        (is (str/starts-with? (:source s) "on-behalf-of"))
        (is (sub/captured? s))
        (is (= paste (get-in s [:answers :captured-text])))
        (is (str/includes? (get-in s [:answers :talk-title]) "Captured from"))

        (testing "with a placeholder identity that can never be mailed"
          (let [sp (first (:speakers s))]
            (is (= "Unknown speaker" (:name sp)))
            (is (str/ends-with? (:email sp) ".invalid"))
            (is (some? (:person-id sp)))))

        (testing "the raw paste is a PRIVATE field, so it never reaches a public surface"
          (let [f (first (filter #(= :captured-text (keyword (name (:id %))))
                                 (:form-snapshot s)))]
            (is (:private f))
            (is (= "As received" (:label f))))
          (is (not (contains? (exports/public-answers s) :captured-text))))))

    (testing "a capture WITH details uses them"
      (let [s (sub/capture! event {:captured-text paste
                                   :title "Rebuilding our deploy pipeline"
                                   :speaker-name "Sam Okafor"
                                   :speaker-email "SAM@example.com"
                                   :speaker-org "Northwind"
                                   :source "linkedin-dm"}
                            "gene@example.com")
            sp (first (:speakers s))]
        (is (= "Rebuilding our deploy pipeline" (get-in s [:answers :talk-title])))
        (is (= "Sam Okafor" (:name sp)))
        (is (= "sam@example.com" (:email sp)) "email is normalised like everywhere else")
        (is (= "Northwind" (:org sp)))
        (is (= "on-behalf-of:linkedin-dm" (:source s)))))

    (testing "an empty paste captures nothing"
      (is (nil? (sub/capture! event {:captured-text "   "} "gene@example.com"))))

    (testing "capture sends NO email — it is the organizer's note, not a submission"
      (is (empty? (filter #(= "submission-confirmation" (:kind %))
                          (filterv #(str/includes? (str (:to %)) "sam@")
                                   (mail/history (:id event)))))))

    (testing "captures appear on the board like anything else"
      (let [rows (reviews/enriched-for-event (:id event))]
        (is (= 3 (count rows)))
        (is (some #(= "Rebuilding our deploy pipeline" (get-in % [:answers :talk-title])) rows))))

    (testing "the Log narrates it as a capture, not a submission"
      (let [e (last (filter #(and (= "submission.created" (:type %))
                                  (str/starts-with? (str (get-in % [:payload :source]))
                                                    "on-behalf-of"))
                            (store/log-for-event (:id event))))]
        (is (str/includes? (@#'cfp-scheduler-killer.views/log-summary e)
                           "Captured on behalf of"))))

    (testing "and it all survives a reload"
      (store/load!)
      (is (= 3 (count (store/submissions-for-event (:id event))))))))

(deftest capture-routes-test
  (let [{:keys [event]} (setup!)
        handler (server/create-app)
        cookie (let [t (auth/issue-token! "gene@example.com")
                     r (handler (mock/request :get (str "/auth/" t)))]
                 (first (str/split (first (get-in r [:headers "Set-Cookie"])) #";")))
        as #(mock/header % "cookie" cookie)]

    (testing "the capture page is reachable and organizer-only"
      (is (= 200 (:status (handler (as (mock/request :get "/events/comms-test/capture"))))))
      (is (= 302 (:status (handler (mock/request :get "/events/comms-test/capture"))))))

    (testing "the board offers it — and the retired submissions page 303s there"
      (is (str/includes? (:body (handler (as (mock/request :get "/events/comms-test/board"))))
                         "+ Add submission"))
      (let [resp (handler (as (mock/request :get "/events/comms-test/submissions")))]
        (is (= 303 (:status resp)))
        (is (str/ends-with? (get-in resp [:headers "Location"]) "/board"))))

    (testing "an empty paste is refused with a message, not a blank row"
      (let [resp (handler (as (mock/request :post "/api/events/comms-test/capture"
                                            {"captured-text" ""})))]
        (is (= 422 (:status resp)))
        (is (str/includes? (:body resp) "Paste something"))))

    (testing "a real capture lands on its detail page with the raw text shown"
      (let [resp (handler (as (mock/request :post "/api/events/comms-test/capture"
                                            {"captured-text" "Pasted from an email."
                                             "speaker-name" "Sam Okafor"
                                             "source" "email"})))
            location (get-in resp [:headers "Location"])
            detail (:body (handler (as (mock/request :get location))))]
        (is (= 303 (:status resp)))
        (is (str/includes? detail "As received"))
        (is (str/includes? detail "Pasted from an email."))
        (is (str/includes? detail "captured on their behalf (email)"))
        (is (str/includes? detail "PC ONLY"))))

    (testing "the captured text NEVER reaches a public surface"
      (doseq [path ["/events/comms-test/exports/sessions.json"
                    "/agenda/comms-test"
                    "/cfp/comms-test"
                    "/api/v1/events/comms-test/sessions"]]
        (is (not (str/includes? (str (:body (handler (mock/request :get path))))
                                "Pasted from an email."))
            path)))))
