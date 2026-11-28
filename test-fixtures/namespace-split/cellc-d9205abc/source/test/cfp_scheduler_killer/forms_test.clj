(ns cfp-scheduler-killer.forms-test
  "The form builder.

   Four properties carry this namespace, and every one of them is a NEGATIVE —
   the failure mode is silent corruption of history, not a broken page:

     1. A field id, once minted, never moves. Renaming a label must not re-key.
     2. Removing a field is RETIRING it. Nothing is erased, because stored
        answers reference the id forever.
     3. Locked fields cannot be removed at all.
     4. Re-folding the log alone reproduces the edited form exactly — otherwise
        the projection and the store have quietly diverged."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [ring.mock.request :as mock]
            [cfp-scheduler-killer.auth :as auth]
            [cfp-scheduler-killer.committees :as committees]
            [cfp-scheduler-killer.events :as events]
            [cfp-scheduler-killer.exports :as exports]
            [cfp-scheduler-killer.forms :as forms]
            [cfp-scheduler-killer.reviews :as reviews]
            [cfp-scheduler-killer.server :as server]
            [cfp-scheduler-killer.store :as store]
            [cfp-scheduler-killer.submissions :as sub]
            [cfp-scheduler-killer.views :as views]
            [cfp-scheduler-killer.test-helpers :refer [with-temp-store]])
  (:import (java.time LocalDate LocalDateTime)))

(use-fixtures :each with-temp-store (fn [f] (reset! auth/tokens {}) (f)))

(defn- make-event!
  "The event, plus the one reviewer who runs it.

   The membership is not decoration: authorization is per-event, so an event
   created straight through `events/create-event!` (no HTTP handler to
   auto-chair the creator) has an empty roster and is unreachable by design.
   Every route test in this namespace drives it as organizer@example.com, so
   that address has to be ON it — which is also exactly what the real create
   handler does for whoever presses the button."
  []
  (let [event (events/create-event!
               {:name "Form Builder Summit" :slug "form-test" :tz "America/New_York"
                :starts-on (LocalDate/of 2026 10 14) :ends-on (LocalDate/of 2026 10 15)
                :support-email "support@example.com"
                :cfp-opens-at (LocalDateTime/of 2020 1 1 0 0)
                :cfp-closes-at (LocalDateTime/of 2099 1 1 0 0)}
               "kaocha")]
    (committees/add-member! (:id (first (events/committees-for-event (:id event))))
                            {:name "The Organizer" :email "organizer@example.com"
                             :role "chair"}
                            "kaocha")
    event))

(def good-params
  {:answer-talk-title "Scaling AI at BigCo"
   :answer-abstract "How we did it, and what broke."
   :answer-session-format "Experience Report"
   :answer-track "Developer Practices"
   :answer-org-size ">10,000"
   :answer-industry "Insurance"
   :answer-ai-transformation-history "Started 2023; three systems in production."
   :answer-measurable-outcomes "40% faster cycle time, $2M saved."
   :answer-notes-to-committee "Please don't schedule me first thing."
   :speaker-name "Ann Perry"
   :speaker-email "ann@example.com"
   :speaker-title "VP Engineering"
   :speaker-org "BigCo"
   :speaker-bio "Ann runs platform engineering at BigCo."})

(defn- live-fields [event] (forms/fields-for-event (:id event)))

(defn- submit!
  "Submit against the LIVE form, exactly the way the public handler does —
   active fields only."
  ([event] (submit! event good-params))
  ([event params]
   (let [ff (forms/active-fields (live-fields event))]
     (sub/create-submission! event
                             (sub/parse-answers ff params)
                             (sub/parse-speaker params)
                             "form" "speaker"))))

(defn- ids [fields] (mapv forms/field-id fields))

(defn- add! [event attrs]
  (forms/add-field! event (merge {:type "text" :required false :private false
                                  :options []}
                                 attrs)
                    "kaocha"))

;; --- Rule 1: field ids are forever ------------------------------------------

(deftest mint-field-id-test
  (testing "the id is a kebab derivation of the label"
    (is (= "what-broke" (forms/mint-field-id "What broke?" [])))
    (is (= "budget-in-us" (forms/mint-field-id "Budget (in US$)" []))))

  (testing "a blank label still yields a usable id rather than an empty key"
    (is (= "field" (forms/mint-field-id "" [])))
    (is (= "field" (forms/mint-field-id "!!!" []))))

  (testing "collisions are suffixed, never silently reused"
    (let [fields [{:id "talk-title"} {:id "talk-title-2"}]]
      (is (= "talk-title-3" (forms/mint-field-id "Talk title" fields)))))

  (testing "ids nested in the speaker group are taken too"
    (is (= "speaker-name-2"
           (forms/mint-field-id "Speaker name" [{:id "speakers" :type "group"
                                                 :fields [{:id "speaker-name"}]}])))))

(deftest renaming-a-label-never-rekeys-test
  (let [event (make-event!)
        id (add! event {:label "What would you tell a peer CTO?" :required false})]
    (is (= "what-would-you-tell-a-peer-cto" id))

    (testing "an answer is stored under that id"
      (let [s (submit! event (assoc good-params
                                    (keyword (str "answer-" id)) "Start smaller."))]
        (is (= "Start smaller." (get (:answers s) (keyword id))))

        (testing "renaming the label leaves the id — and the answer — alone"
          (forms/update-field! event id {:label "What would you tell a peer CIO?"
                                         :required false :private false :options []}
                               "kaocha")
          (let [f (forms/find-field (live-fields event) id)]
            (is (= id (forms/field-id f)) "the id did not move")
            (is (= "What would you tell a peer CIO?" (:label f)) "but the label did"))
          (is (= "Start smaller."
                 (get (:answers (sub/by-id (:id s))) (keyword id)))
              "the stored answer is still reachable by the same key"))))))

(deftest update-field-cannot-change-id-or-type-test
  (let [event (make-event!)
        id (add! event {:label "Prior talk video" :type "url"})]
    (forms/update-field! event id {:label "A different label"
                                   :id "hijacked" :type "textarea"
                                   :required true :private false :options []}
                         "kaocha")
    (let [f (forms/find-field (live-fields event) id)]
      (is (= id (forms/field-id f)))
      (is (= "url" (forms/field-type f)) "the type is fixed at birth")
      (is (nil? (forms/find-field (live-fields event) "hijacked"))))))

;; --- Rule 2: removing is retiring -------------------------------------------

(deftest retire-hides-without-erasing-test
  (let [event (make-event!)
        id (add! event {:label "Extra question"})
        s (submit! event (assoc good-params (keyword (str "answer-" id)) "An answer."))]

    (forms/retire-field! event id "kaocha")

    (testing "the field is gone from the live form"
      (is (not-any? #(= id (forms/field-id %))
                    (forms/active-fields (live-fields event)))))

    (testing "…but it is still IN the form, marked retired — the id is permanent"
      (let [f (forms/find-field (live-fields event) id)]
        (is (some? f))
        (is (true? (:retired f)))
        (is (some? (:retired-at f)))))

    (testing "and it no longer renders on the public CFP page"
      (let [body (:body (server/render-cfp {} (events/event-by-slug "form-test") {} 200))]
        (is (not (str/includes? body "Extra question")))
        (is (str/includes? body "Talk title") "the rest of the form is untouched")))

    (testing "the existing answer is untouched and still readable on the detail page"
      (is (= "An answer." (get (:answers (sub/by-id (:id s))) (keyword id))))
      (let [html (views/submission-detail-page (events/event-by-slug "form-test")
                                               (reviews/enrich (store/submission-by-id (:id s)))
                                               {:person nil :coverage-target 2})]
        (is (str/includes? html "Extra question"))
        (is (str/includes? html "An answer."))))

    (testing "restoring brings the same id back"
      (forms/restore-field! event id "kaocha")
      (let [f (forms/find-field (forms/active-fields (live-fields event)) id)]
        (is (some? f))
        (is (nil? (:retired f)))))))

(deftest retired-field-is-not-collected-or-demanded-test
  (let [event (make-event!)
        id (add! event {:label "Mandatory extra" :required true})]
    (testing "while live, it is required"
      (let [ff (forms/active-fields (live-fields event))]
        (is (contains? (sub/validation-errors ff (sub/parse-answers ff good-params)
                                              (sub/parse-speaker good-params))
                       (keyword id)))))
    (testing "once retired, a submission without it validates"
      (forms/retire-field! event id "kaocha")
      (let [ff (forms/active-fields (live-fields event))]
        (is (nil? (sub/validation-errors ff (sub/parse-answers ff good-params)
                                         (sub/parse-speaker good-params))))
        (is (not (contains? (sub/parse-answers ff good-params) (keyword id)))
            "and the retired answer is not collected at all")))))

;; --- Rule 3: locked fields are undeletable ----------------------------------

(deftest locked-fields-cannot-be-retired-test
  (let [event (make-event!)]
    (doseq [locked ["talk-title" "abstract" "speakers"]]
      (testing (str locked " refuses to be retired")
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Locked field"
                              (forms/retire-field! event locked "kaocha")))
        (is (nil? (:retired (forms/find-field (live-fields event) locked))))))

    (testing "and nothing was appended for the refused attempt"
      (is (not-any? #(= "form.updated" (:type %))
                    (events/log-for-event (:id event)))))))

;; --- Reordering -------------------------------------------------------------

(deftest move-field-reorders-test
  (let [event (make-event!)
        before (ids (live-fields event))]
    (testing "moving down swaps with the next field"
      (forms/move-field! event "abstract" "down" "kaocha")
      (let [after (ids (live-fields event))]
        (is (= "session-format" (second after)))
        (is (= "abstract" (nth after 2)))
        (is (= (set before) (set after)) "a move never adds or drops a field")))

    (testing "moving back up restores the original order exactly"
      (forms/move-field! event "abstract" "up" "kaocha")
      (is (= before (ids (live-fields event)))))

    (testing "the first field cannot move up, and nothing is appended"
      (let [n (count (filter #(= "form.updated" (:type %)) (events/log-for-event (:id event))))]
        (is (nil? (forms/move-field! event "talk-title" "up" "kaocha")))
        (is (= n (count (filter #(= "form.updated" (:type %))
                                (events/log-for-event (:id event))))))))

    (testing "a question never swaps across the speaker block — it always reads last"
      (is (nil? (forms/move-field! event "notes-to-committee" "down" "kaocha")))
      (is (= "speakers" (last (ids (live-fields event))))))))

(deftest new-fields-land-before-the-speaker-block-test
  (let [event (make-event!)
        id (add! event {:label "One more thing"})
        order (ids (live-fields event))]
    (is (= "speakers" (last order)))
    (is (= id (nth order (- (count order) 2))))))

;; --- Rule 4: the log IS the form --------------------------------------------

(deftest refold-reproduces-the-edited-form-test
  (let [event (make-event!)
        added (add! event {:label "Budget owner" :type "select" :required true
                           :options ["CIO" "CTO" "CFO"] :widget "radio"
                           :help "Who signs?"})]
    (forms/update-field! event "advice-to-peer"
                         {:label "What would you tell a peer CIO to do differently?"
                          :required true :private false :max-length 900 :options []}
                         "kaocha")
    (forms/move-field! event added "up" "kaocha")
    (forms/retire-field! event "business-co-presenter" "kaocha")

    (let [live (live-fields event)]
      (testing "the projection has all four edits"
        (is (= "CIO" (first (:options (forms/find-field live added)))))
        (is (= "radio" (:widget (forms/find-field live added))))
        (is (= 900 (:max-length (forms/find-field live "advice-to-peer"))))
        (is (true? (:retired (forms/find-field live "business-co-presenter")))))

      (testing "and a cold re-fold of the log alone reproduces it EXACTLY"
        (store/load!)
        (is (= live (forms/fields-for-event (:id event))))))))

(deftest every-edit-appends-exactly-one-event-test
  (let [event (make-event!)
        n0 (count (events/log-for-event (:id event)))]
    (add! event {:label "A"})
    (forms/update-field! event "a" {:label "B" :required false :private false :options []} "kaocha")
    (forms/move-field! event "a" "up" "kaocha")
    (forms/retire-field! event "a" "kaocha")
    (let [appended (filter #(= "form.updated" (:type %))
                           (drop n0 (events/log-for-event (:id event))))]
      (is (= 4 (count appended)))
      (is (= ["add-field" "update-field" "move-up" "retire-field"]
             (mapv #(get-in % [:payload :change]) appended)))
      (testing "every payload carries the COMPLETE vector, not a delta"
        (is (every? #(seq (get-in % [:payload :fields])) appended))))))

;; --- Snapshot immunity ------------------------------------------------------

(deftest editing-the-form-never-rewrites-an-existing-submission-test
  (let [event (make-event!)
        s (submit! event)
        snapshot-before (:form-snapshot (sub/by-id (:id s)))]

    (testing "rename a question, retire another, and add a third AFTER the submission"
      (forms/update-field! event "measurable-outcomes"
                           {:label "COMPLETELY DIFFERENT QUESTION"
                            :required true :private false :options []}
                           "kaocha")
      (forms/retire-field! event "advice-to-peer" "kaocha")
      (add! event {:label "Brand new question" :required true}))

    (testing "the submission's snapshot is byte-identical to what it was"
      (is (= snapshot-before (:form-snapshot (sub/by-id (:id s))))))

    (testing "and it still renders its ORIGINAL labels, not the live ones"
      (let [html (views/submission-detail-page
                  (events/event-by-slug "form-test")
                  (reviews/enrich (store/submission-by-id (:id s)))
                  {:person nil :coverage-target 2})]
        (is (str/includes? html "What measurable outcomes can you share?"))
        (is (not (str/includes? html "COMPLETELY DIFFERENT QUESTION")))
        (is (not (str/includes? html "Brand new question")))))

    (testing "while the LIVE form has moved on"
      (is (= "COMPLETELY DIFFERENT QUESTION"
             (:label (forms/find-field (live-fields event) "measurable-outcomes")))))))

;; --- Private fields never leak ----------------------------------------------

(deftest private-fields-never-leak-test
  (let [event (make-event!)
        id (add! event {:label "Salary expectations" :private true :required false})
        secret "PC eyes only: I need business class."
        s (submit! event (assoc good-params (keyword (str "answer-" id)) secret))]

    (testing "the committee sees it — that is the point of collecting it"
      (is (= secret (get (:answers (sub/by-id (:id s))) (keyword id)))))

    (testing "the shared exclusion fn drops it from the public projection"
      (is (not-any? #(= id (forms/field-id %))
                    (sub/public-fields (live-fields event))))
      (is (not (contains? (exports/public-answers (store/submission-by-id (:id s)))
                          (keyword id)))))

    ;; Publish it: Accepted AND informed, the only state exports describe.
    (store/append! {:type "submission.status-changed" :actor "kaocha"
                    :event-id (:id event)
                    :payload {:submission-id (:id s) :to "Accepted"}})
    (store/append! {:type "submission.notified" :actor "kaocha"
                    :event-id (:id event)
                    :payload {:submission-id (:id s) :at (store/now-iso)
                              :status-at-notify "Accepted"}})

    (testing "and it is absent from every published surface"
      (let [ev (events/event-by-slug "form-test")]
        (doseq [[what data] [["sessions.json" (exports/sessions-json-data ev)]
                             ["the API (token holders included)" (exports/api-sessions ev :all)]
                             ["llms.txt" (exports/llms-txt ev "http://localhost")]
                             ["speakers.json" (exports/speakers-json-data ev)]]]
          (is (not (str/includes? (pr-str data) secret)) what))))))

;; --- Validation -------------------------------------------------------------

(deftest field-validation-test
  (testing "a question needs a label"
    (is (contains? (forms/field-validation-errors
                    (forms/parse-field-params {:label "  " :type "text"}) {:new? true})
                   :label)))

  (testing "'choose one' needs options"
    (is (contains? (forms/field-validation-errors
                    (forms/parse-field-params {:label "Track" :type "select"}) {:new? true})
                   :options))
    (is (nil? (forms/field-validation-errors
               (forms/parse-field-params {:label "Track" :type "select"
                                          :options "A\n\nB\n"})
               {:new? true}))))

  (testing "the character limit has to be a positive whole number"
    (is (contains? (forms/field-validation-errors
                    (forms/parse-field-params {:label "X" :type "text" :max-length "lots"})
                    {:new? true})
                   :max-length))
    (is (contains? (forms/field-validation-errors
                    (forms/parse-field-params {:label "X" :type "text" :max-length "0"})
                    {:new? true})
                   :max-length)))

  (testing "an unknown type is refused rather than stored"
    (is (contains? (forms/field-validation-errors
                    (forms/parse-field-params {:label "X" :type "sql"}) {:new? true})
                   :type)))

  (testing "options are one per line, blanks dropped"
    (is (= ["A" "B"] (forms/parse-options " A \n\n  B  \n")))))

;; --- Routes -----------------------------------------------------------------

(defn- raw-app [] (server/create-app))

(defn- app []
  (let [raw (raw-app)
        token (auth/issue-token! "organizer@example.com")
        resp (raw (mock/request :get (str "/auth/" token)))
        cookie (first (str/split (first (get-in resp [:headers "Set-Cookie"])) #";"))]
    (fn [req] (raw (mock/header req "cookie" cookie)))))

(defn- post [handler path params]
  (handler (mock/request :post path params)))

(deftest form-builder-route-test
  (let [handler (app)
        event (make-event!)]

    (testing "the page renders, and the left nav no longer greys Form out"
      (let [resp (handler (mock/request :get "/events/form-test/form"))]
        (is (= 200 (:status resp)))
        (is (str/includes? (:body resp) "What speakers see"))
        (is (str/includes? (:body resp) "Add a question"))
        (is (str/includes? (:body resp) "href=\"/events/form-test/form\""))))

    (testing "adding a field 303s back and puts it on the PUBLIC page"
      (let [resp (post handler "/api/events/form-test/form/add"
                       {:label "What is your biggest risk?" :type "textarea"
                        :required "on" :help "One sentence."})]
        (is (= 303 (:status resp)))
        ;; ?added=1 makes the form page confirm the save with a toast (dec73fc)
        (is (= "/events/form-test/form?added=1" (get-in resp [:headers "Location"]))))
      (let [public (handler (mock/request :get "/cfp/form-test"))]
        (is (str/includes? (:body public) "What is your biggest risk?"))
        (is (str/includes? (:body public) "answer-what-is-your-biggest-risk"))))

    (testing "a bad add comes back 422 with the message and what was typed"
      (let [resp (post handler "/api/events/form-test/form/add" {:label "" :type "text"})]
        (is (= 422 (:status resp)))
        (is (str/includes? (:body resp) "A question needs a label"))))

    (testing "the delete is TWO steps, both server-rendered — no confirm() anywhere"
      (let [page (:body (handler (mock/request :get "/events/form-test/form")))]
        (is (not (str/includes? page "confirm(")))
        (is (not (str/includes? page "Yes, retire it"))))
      (post handler "/api/events/form-test/form/retire-ask"
            {:field-id "what-is-your-biggest-risk"})
      (let [page (:body (handler (mock/request :get "/events/form-test/form")))]
        (is (str/includes? page "Yes, retire it"))
        (is (str/includes? page "Cancel")))
      (testing "cancelling clears the prompt and changes nothing"
        (post handler "/api/events/form-test/form/retire-cancel" {})
        (let [page (:body (handler (mock/request :get "/events/form-test/form")))]
          (is (not (str/includes? page "Yes, retire it"))))
        (is (some? (forms/find-field (forms/active-fields (live-fields event))
                                     "what-is-your-biggest-risk"))))
      (testing "confirming retires it, and the public page drops it"
        (post handler "/api/events/form-test/form/retire-ask"
              {:field-id "what-is-your-biggest-risk"})
        (post handler "/api/events/form-test/form/retire"
              {:field-id "what-is-your-biggest-risk"})
        (is (nil? (forms/find-field (forms/active-fields (live-fields event))
                                    "what-is-your-biggest-risk")))
        (is (not (str/includes? (:body (handler (mock/request :get "/cfp/form-test")))
                                "What is your biggest risk?")))))

    (testing "retiring a locked field is refused with a 409, not a stack trace"
      (let [resp (post handler "/api/events/form-test/form/retire" {:field-id "talk-title"})]
        (is (= 409 (:status resp)))
        (is (str/includes? (:body resp) "spine of a call for speakers"))))

    (testing "reorder is a plain POST"
      (post handler "/api/events/form-test/form/move"
            {:field-id "abstract" :direction "down"})
      (is (= "session-format" (second (ids (live-fields event))))))))

(deftest dashboard-checklist-tracks-the-form-test
  (let [handler (app)
        event (make-event!)]
    (testing "before anyone touches the form, the row is unticked"
      (is (false? (forms/reviewed? (:id event))))
      (let [body (:body (handler (mock/request :get "/events/form-test")))]
        ;; The checklist speaks the wizard's step names now (DRY, 2026-08-09).
        (is (str/includes? body "Create CFP form"))
        (is (str/includes? body "open the form editor"))))

    (testing "opening the page is NOT a review — nothing is claimed"
      (handler (mock/request :get "/events/form-test/form"))
      (is (false? (forms/reviewed? (:id event)))))

    (testing "pressing 'Looks right' records the fact and ticks the row"
      (post handler "/api/events/form-test/form/reviewed" {})
      (is (true? (forms/reviewed? (:id event))))
      (is (= "form.reviewed" (:type (last (events/log-for-event (:id event)))))))

    (testing "editing the form counts as reviewing it too"
      (let [e2 (events/create-event!
                {:name "Second" :slug "form-test-2" :tz "UTC"} "kaocha")]
        (is (false? (forms/reviewed? (:id e2))))
        (add! e2 {:label "Anything"})
        (is (true? (forms/reviewed? (:id e2))))))))

(deftest form-page-renders-the-real-public-renderer-test
  (let [event (make-event!)
        html (str (h/html (views/form-preview-region (events/event-by-slug "form-test")
                                                     (live-fields event))))]
    (testing "the preview is the public page's own field renderer"
      (is (str/includes? html "answer-talk-title"))
      (is (str/includes? html "Experience Report") "select options render as they will publicly"))
    (testing "and a retired field disappears from it"
      (forms/retire-field! event "prior-talk-video" "kaocha")
      (let [html2 (str (h/html (views/form-preview-region (events/event-by-slug "form-test")
                                                          (live-fields event))))]
        (is (not (str/includes? html2 "Link to a video of a prior talk")))))))
