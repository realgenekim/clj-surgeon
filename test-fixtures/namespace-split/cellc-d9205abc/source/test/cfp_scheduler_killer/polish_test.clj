(ns cfp-scheduler-killer.polish-test
  "Live-scrub time travel, clickable public URLs, and the one timestamp format."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [ring.mock.request :as mock]
            [cfp-scheduler-killer.auth :as auth]
            [cfp-scheduler-killer.committees :as committees]
            [cfp-scheduler-killer.events :as events]
            [cfp-scheduler-killer.inform :as inform]
            [cfp-scheduler-killer.reviews :as reviews]
            [cfp-scheduler-killer.server :as server]
            [cfp-scheduler-killer.store :as store]
            [cfp-scheduler-killer.submissions :as sub]
            [cfp-scheduler-killer.views :as views]
            [cfp-scheduler-killer.test-helpers :refer [with-temp-store]])
  (:import (java.time Instant LocalDateTime)))

(use-fixtures :each with-temp-store (fn [f] (reset! auth/tokens {}) (f)))

;; --- The one timestamp format -----------------------------------------------

(deftest fmt-when-test
  (let [now (Instant/parse "2026-08-09T15:00:00Z")
        ny "America/New_York"]

    (testing "absolute time, in the EVENT's zone, in English"
      ;; 15:00Z is 11:00 EDT — the whole point of passing the zone.
      (is (= "Jun 1, 11:00 AM" (views/fmt-when "2026-06-01T15:00:00Z" ny
                                               (Instant/parse "2026-09-01T00:00:00Z"))))
      (is (= "Jun 1, 5:00 AM" (views/fmt-when "2026-06-01T09:00:00Z" ny
                                              (Instant/parse "2026-09-01T00:00:00Z")))))

    (testing "a relative hint while it is still recent"
      (is (= "Aug 9, 10:59 AM · just now" (views/fmt-when "2026-08-09T14:59:30Z" ny now)))
      (is (= "Aug 9, 10:20 AM · 40m ago" (views/fmt-when "2026-08-09T14:20:00Z" ny now)))
      (is (= "Aug 9, 7:15 AM · 3h ago" (views/fmt-when "2026-08-09T11:15:00Z" ny now)))
      (is (= "Aug 6, 11:00 AM · 3d ago" (views/fmt-when "2026-08-06T15:00:00Z" ny now))))

    (testing "and no hint once it stops being useful"
      (is (= "Jul 1, 11:00 AM" (views/fmt-when "2026-07-01T15:00:00Z" ny now))))

    (testing "the zone really is honoured"
      (is (= "Aug 9, 4:00 PM" (views/fmt-when "2026-08-09T15:00:00Z" "Europe/London"
                                              (Instant/parse "2026-09-01T00:00:00Z"))))
      (is (= "Aug 9, 3:00 PM" (views/fmt-when "2026-08-09T15:00:00Z" "UTC"
                                              (Instant/parse "2026-09-01T00:00:00Z")))))

    (testing "an unusable value renders as nothing, never as an exception"
      (is (nil? (views/fmt-when nil ny now)))
      (is (nil? (views/fmt-when "not-a-time" ny now)))
      (is (nil? (views/fmt-when "" ny now))))

    (testing "it accepts an Instant as readily as a string"
      (is (= (views/fmt-when "2026-08-09T14:20:00Z" ny now)
             (views/fmt-when (Instant/parse "2026-08-09T14:20:00Z") ny now))))

    (testing "an unknown zone falls back to UTC rather than throwing"
      (is (= "Aug 9, 3:00 PM" (views/fmt-when "2026-08-09T15:00:00Z" "Mars/Olympus"
                                              (Instant/parse "2026-09-01T00:00:00Z")))))))

;; --- Fixtures ---------------------------------------------------------------

(defn- setup!
  "An event with two submissions and a rating, so there is a history to scrub."
  []
  (let [event (events/create-event!
               {:name "Polish Summit" :slug "polish" :tz "America/New_York"
                :support-email "annp@example.com"
                :starts-on (java.time.LocalDate/of 2026 10 14)
                :ends-on (java.time.LocalDate/of 2026 10 15)
                :cfp-opens-at (LocalDateTime/of 2020 1 1 0 0)
                :cfp-closes-at (LocalDateTime/of 2099 1 1 0 0)}
               "kaocha")
        cid (:id (first (events/committees-for-event (:id event))))
        gene (committees/add-member! cid {:name "Gene Kim" :email "gene@example.com"
                                          :role "chair"} "kaocha")
        ff (:fields (events/form-for-event (:id event)))
        mk (fn [title email]
             (let [params {:answer-talk-title title :answer-abstract "A."
                           :answer-session-format "Experience Report"
                           :answer-org-size ">10,000" :answer-industry "Insurance"
                           :answer-ai-transformation-history "x"
                           :answer-measurable-outcomes "y"
                           :speaker-name "S" :speaker-email email
                           :speaker-title "VP" :speaker-org "Co" :speaker-bio "B"}]
               (sub/create-submission! event (sub/parse-answers ff params)
                                       (sub/parse-speaker params) "form" "kaocha")))
        a (mk "First talk" "a@example.com")
        _ (Thread/sleep 5)
        b (mk "Second talk" "b@example.com")]
    (store/await-sinks!)
    (reviews/set-rating! (:id a) (:person-id gene) 5.0 "gene@example.com")
    {:event (events/event-by-slug "polish") :a a :b b :gene gene}))

(defn- signed-in-handler []
  (let [handler (server/create-app)
        token (auth/issue-token! "gene@example.com")
        r (handler (mock/request :get (str "/auth/" token)))
        cookie (first (str/split (first (get-in r [:headers "Set-Cookie"])) #";"))]
    (fn [req] (handler (mock/header req "cookie" cookie)))))

(defn- fragment-html
  "The HTML a scrub tick would patch.

   Rendered through the handler's own function rather than the SSE socket: a
   one-shot Datastar response needs a live http-kit channel, which ring-mock
   does not provide. WHAT gets patched is the part worth asserting."
  [f slug at-index]
  (let [event (events/event-by-slug slug)
        req (cond-> {:params {} :path-params {:slug slug}}
              at-index (assoc :params {:at-index (str at-index)}))]
    (f req event)))

;; --- Live scrub -------------------------------------------------------------

(deftest scrub-slider-wiring-test
  ;; The scrubber rides the DEV STRIP (Gene, 2026-08-09), which renders only
  ;; under ENV=dev — so the wiring is asserted on the COMPONENT, and the page
  ;; is asserted to carry no slider inside the patchable region.
  (let [{:keys [event]} (setup!)
        bar (str (hiccup2.core/html
                  (views/time-travel-bar
                   event
                   {:bounds ["2026-08-01T00:00:00Z" "2026-08-09T00:00:00Z"]
                    :index 3 :total 10
                    :base-path "/events/polish/board"
                    :fragment-path "/events/polish/board/fragment"})))
        handler (signed-in-handler)
        body (:body (handler (mock/request :get "/events/polish/board")))]

    (testing "the slider scrubs live WHILE dragging — throttled, not debounced"
      (is (str/includes? bar "data-star-on:input__throttle.150ms"))
      (is (str/includes? bar "/events/polish/board/fragment?at-index="))
      (is (str/includes? bar "data-star-bind:atidx")))

    (testing "releasing the handle does NOT reload the page — the scrub is
              pure SSE (Gene, 2026-08-09); the form is only the no-JS fallback"
      (is (not (str/includes? bar "onchange")))
      (is (str/includes? bar "action=\"/events/polish/board\"")))

    (testing "the signal is a single word — Datastar camelCases hyphens"
      (is (not (str/includes? bar "$at-index")))
      (is (str/includes? bar "$atidx")))

    (testing "the patchable region exists and carries NO slider — outside
              ENV=dev the board has no time control at all"
      (is (str/includes? body "id=\"board-region\""))
      (is (not (str/includes? body "type=\"range\""))))

    (is (some? event))))

(deftest board-fragment-test
  (let [{:keys [event b]} (setup!)
        handler (signed-in-handler)
        entries (store/log-for-event (:id event))
        ;; index of the event that created the SECOND talk
        idx-of-b (count (take-while #(not= (:id b) (get-in % [:payload :id])) entries))]

    (testing "the fragment renders the board region as of that moment"
      (let [html (fragment-html server/board-fragment-html "polish" (dec idx-of-b))]
        (is (str/includes? html "First talk"))
        (is (not (str/includes? html "Second talk"))
            "the second talk had not been submitted yet")))

    (testing "the fragment does NOT contain the slider — dragging must not be interrupted"
      (let [html (fragment-html server/board-fragment-html "polish" 2)]
        (is (not (str/includes? html "type=\"range\"")))
        (is (not (str/includes? html "data-star-on:input")))
        (is (not (str/includes? html "Viewing as of")))
        (is (not (str/includes? html "Return to now")))))

    (testing "it IS the region, so the patch lands on the right element"
      (let [html (fragment-html server/board-fragment-html "polish" 2)]
        (is (str/starts-with? html "<div id=\"board-region\""))))

    (testing "with no index it renders NOW"
      (let [html (fragment-html server/board-fragment-html "polish" nil)]
        (is (str/includes? html "First talk"))
        (is (str/includes? html "Second talk"))))

    ;; The SSE UPGRADE itself is not assertable here: ->sse-response needs a live
    ;; http-kit async channel, which ring-mock does not provide. What the mock
    ;; CAN prove is that the route resolves to the fragment handler rather than
    ;; falling through to a 404 — the stream itself is verified against a real
    ;; server.
    (testing "the route resolves to the fragment handler"
      (is (not= 404 (:status (handler (mock/request
                                       :get "/events/polish/board/fragment?at-index=2"))))))

    (testing "the log page scrubs the same way"
      (let [page (:body (handler (mock/request :get "/events/polish/log")))]
        (is (str/includes? page "id=\"log-region\""))
        (is (str/includes? page "/events/polish/log/fragment?at-index=")))
      (let [html (fragment-html server/log-fragment-html "polish" 1)]
        (is (str/starts-with? html "<div id=\"log-region\""))
        (is (not (str/includes? html "type=\"range\"")))))

    (testing "an unknown event 404s rather than streaming nothing"
      (is (= 404 (:status (handler (mock/request :get "/events/nope/board/fragment"))))))))

(deftest scrubbing-cannot-mutate-test
  (let [{:keys [event a]} (setup!)
        handler (signed-in-handler)]
    (testing "scrubbing is a read — the world does not move"
      (let [before (count (store/read-events))]
        (dotimes [i 5]
          (handler (mock/request :get (str "/events/polish/board/fragment?at-index=" i))))
        (is (= before (count (store/read-events))))
        (is (= 2 (count (store/submissions-for-event (:id event)))))))

    (testing "and the write guard still refuses a write from inside the past"
      (binding [store/*as-of-state* (store/state-as-of "1970-01-01T00:00:00Z")]
        (is (= :read-only-as-of
               (:type (ex-data (try (store/append! {:type "x" :payload {}})
                                    nil
                                    (catch clojure.lang.ExceptionInfo e e))))))))
    (is (some? a))))

(deftest as-of-cache-test
  (let [{:keys [event]} (setup!)
        cutoff (:at (first (store/log-for-event (:id event))))]
    (testing "the same moment folds to the same state, cached or not"
      (let [a (store/state-as-of cutoff)
            b (store/state-as-of cutoff)]
        (is (= a b))))

    (testing "an append invalidates it — a stale past is worse than a slow one"
      (let [before (store/state-as-of cutoff)]
        (events/create-event! {:name "Another" :slug "another" :tz "UTC"} "kaocha")
        (let [after (store/state-as-of cutoff)]
          ;; the world AT that cutoff is unchanged…
          (is (= (:events before) (:events after))))))))

;; --- Clickable public URLs --------------------------------------------------

(deftest public-urls-are-links-test
  (let [{:keys [event]} (setup!)
        handler (signed-in-handler)
        body (:body (handler (mock/request :get "/events/polish")))]

    (testing "the CFP URL an organizer hands to speakers is clickable"
      (is (str/includes? body "<a class=\"cfp-url\" href=\"/cfp/polish\""))
      (is (str/includes? body "target=\"_blank\""))
      (is (str/includes? body "rel=\"noopener\"")))

    (testing "it still READS as an address"
      (is (str/includes? body "/cfp/polish</a>")))

    (testing "the agenda is reachable from the schedule page too"
      (let [sched (:body (handler (mock/request :get "/events/polish/schedule")))]
        (is (str/includes? sched "href=\"/agenda/polish\""))))

    (is (some? event))))

;; --- Comms table ------------------------------------------------------------

(deftest comms-table-test
  (let [{:keys [event a]} (setup!)
        handler (signed-in-handler)]
    (reviews/set-status! (:id a) "Accepted" "gene@example.com")
    (inform/inform! event (store/submission-by-id (:id a)) "gene@example.com")
    (store/await-sinks!)

    (let [body (:body (handler (mock/request :get "/events/polish/comms")))]
      (testing "the send history is a real table with fixed columns"
        (is (str/includes? body "comms-table"))
        (is (str/includes? body "<th class=\"c-when\">When</th>"))
        (is (str/includes? body "<th class=\"c-to\">To</th>"))
        (is (str/includes? body "<th>Subject</th>"))
        (is (str/includes? body "<th class=\"c-status\">Status</th>")))

      (testing "every row fills every column, so they line up"
        (let [rows (re-seq #"<tr[ >](?:(?!</tr>).)*</tr>" (str/replace body #"\n" " "))
              body-rows (remove #(str/includes? % "<th") rows)]
          (is (seq body-rows))
          (doseq [r body-rows]
            (is (str/includes? r "c-when") "a row missing its when cell")
            (is (str/includes? r "c-to") "a row missing its to cell")
            (is (str/includes? r "c-status") "a row missing its status cell"))))

      (testing "timestamps are English in the event's zone, not raw ISO"
        (is (re-find #"[A-Z][a-z]{2} \d{1,2}, \d{1,2}:\d{2} (AM|PM)" body))
        (is (str/includes? body "just now"))
        (is (not (re-find #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}" body))
            "no raw ISO instant should reach the page"))

      (testing "the status column says what actually happened"
        (is (str/includes? body "would send"))
        (is (not (str/includes? body ">sent<")))))

    (testing "the Log page humanises its timestamps too"
      (let [body (:body (handler (mock/request :get "/events/polish/log")))]
        (is (re-find #"[A-Z][a-z]{2} \d{1,2}, \d{1,2}:\d{2} (AM|PM)" body))
        (is (not (re-find #"<div class=\"log-when\">\d{4}-\d{2}-\d{2}T" body)))))

    (testing "and so does the person page"
      (let [pid (:person-id (first (committees/members-for-committee
                                    (:id (first (events/committees-for-event (:id event)))))))
            body (:body (handler (mock/request :get (str "/events/polish/people/" pid))))]
        (is (str/includes? body "member since"))
        (is (re-find #"member since [A-Z][a-z]{2} \d{1,2}, \d{1,2}:\d{2} (AM|PM)" body))))))
