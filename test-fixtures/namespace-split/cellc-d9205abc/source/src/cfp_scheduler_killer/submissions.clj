(ns cfp-scheduler-killer.submissions
  "The public CFP's output: talk submissions.

   The form is DATA (a vector of field defs, docs/design/form-builder.md), and
   this namespace derives everything from it — the malli schema, the answer
   parsing, the required-field messages. Add a field to the seed form and the
   validation follows automatically; there is no second list to keep in sync.

   Three rules from the design doc that this code enforces:
     1. **Snapshot the field defs with each submission.** A mid-CFP form edit
        must never change how an existing submission renders or validates, so
        the whole field vector is stored on the submission.
     2. **Answers are keyed by FIELD ID, never by label.** Labels rename freely.
     3. **Private fields are PC-only.** `:private true` fields are collected from
        the speaker but never leave the committee."
  (:require [clojure.string :as str]
            [cfp-scheduler-killer.forms :as forms]
            [malli.core :as m]
            [malli.error :as me]
            [taoensso.timbre :as log]
            [closed-record.core :as cr]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [cfp-scheduler-killer.mail :as mail]
            [cfp-scheduler-killer.people :as people]
            [cfp-scheduler-killer.store :as store])
  (:import (java.time Instant)))

(def default-status
  "New submissions land in the middle of the 7-valued vocabulary — neither
   accepted nor declined, and explicitly NOT a draft."
  "Pending")

;; --- Field-def helpers ------------------------------------------------------

(defn session-fields
  "The non-group fields — everything in the YOUR TALK part of the page."
  [form-fields]
  (remove #(= "group" (name (:type %))) form-fields))

(defn speaker-group
  "The repeatable speaker block (ABOUT YOU)."
  [form-fields]
  (first (filter #(= "group" (name (:type %))) form-fields)))

(defn public-fields
  "Fields safe to show outside the committee (exports, public agenda)."
  [form-fields]
  (remove :private (session-fields form-fields)))

(defn field-by-id [form-fields id]
  (first (filter #(= (name (:id %)) (name id)) form-fields)))

;; --- Parsing ----------------------------------------------------------------

(defn- blank->nil [s]
  (when (string? s) (let [t (str/trim s)] (when-not (str/blank? t) t))))

(defn parse-answers
  "Pull one answer per session field out of the raw params, keyed by field id.

   Params arrive as `answer-<field-id>`; namespacing them keeps a field called
   `name` from colliding with the speaker's name."
  [form-fields params]
  (reduce (fn [acc field]
            (let [id (keyword (name (:id field)))
                  v (blank->nil (get params (keyword (str "answer-" (name id)))))]
              ;; EVERY field key is present, nil when blank. If we omitted blanks,
              ;; malli would report "missing required key" instead of the human
              ;; message the field def carries.
              (assoc acc id v)))
          {}
          (session-fields form-fields)))

(defn parse-speaker
  "The owning speaker's details from the ABOUT YOU block."
  [params]
  {:name (blank->nil (:speaker-name params))
   :email (people/normalize-email (:speaker-email params))
   :title (blank->nil (:speaker-title params))
   :org (blank->nil (:speaker-org params))
   :bio (blank->nil (:speaker-bio params))
   :headshot-url (blank->nil (:speaker-headshot-url params))
   :linkedin-url (blank->nil (:speaker-linkedin params))
   :sessionize-url (blank->nil (:speaker-sessionize-url params))})

;; --- Validation (derived from the field defs) -------------------------------

(def url-pattern #"^https?://[^\s/$.?#][^\s]*\.[^\s]*$")

(defn- field-schema
  "One malli entry for one field def. Required-ness, max length and the option
   list all come from the DATA, so the form and its validation can never drift."
  [{:keys [id type label required max-length options]}]
  (let [k (keyword (name id))
        t (name type)
        base (cond
               ;; :enum takes its values SPLICED, not as a nested vector — a
               ;; wrapped vector silently matches nothing.
               (= t "select")
               (into [:enum {:error/message (str label ": pick one of the listed options.")}]
                     options)

               (= t "url")
               [:re {:error/message (str label ": enter a full URL starting with http:// or https://")}
                url-pattern]

               (= t "email")
               [:re {:error/message (str label ": that doesn't look like an email address.")}
                people/email-pattern]

               ;; Length is NOT enforced here: `max-length-errors` reports it
               ;; with the count the speaker actually typed, which is the
               ;; difference between a useful message and a scolding.
               :else
               [:string {:min 1 :error/message (str label " is required.")}])]
    (if required
      [k base]
      [k {:optional true} [:maybe base]])))

(defn answers-schema
  "A malli map schema for a form's session fields."
  [form-fields]
  (into [:map {:closed false}]
        (map field-schema)
        (session-fields form-fields)))

(def SpeakerDraft
  "The ABOUT YOU block. Locked core fields are required; the rest are optional
   but validated when present."
  [:map {:closed false}
   [:name [:string {:min 1 :max 120 :error/message "Your name is required."}]]
   [:email [:re {:error/message "A valid email address is required — it's how we reach you."}
            people/email-pattern]]
   [:title [:string {:min 1 :max 180 :error/message "Your title or tagline is required."}]]
   [:org [:string {:min 1 :max 180 :error/message "Your organization is required."}]]
   [:bio [:string {:min 1 :max 2000 :error/message "A short bio is required."}]]
   [:headshot-url {:optional true}
    [:maybe [:re {:error/message "Headshot must be a full URL."} url-pattern]]]
   [:linkedin-url {:optional true}
    [:maybe [:re {:error/message "LinkedIn must be a full URL."} url-pattern]]]
   [:sessionize-url {:optional true} [:maybe :string]]])

(defn max-length-errors
  "Length checks reported per field, because malli's :max message can't name the
   count the speaker actually typed."
  [form-fields answers]
  (into {}
        (keep (fn [{:keys [id label] :as f}]
                ;; The SHAPE supplies an invisible cap when the field has no
                ;; explicit one (forms/default-cap) — generous, anti-runaway,
                ;; and named only here, at the moment it is actually exceeded.
                (let [k (keyword (name id))
                      v (get answers k)
                      cap (forms/effective-cap f)]
                  (when (and cap v (> (count v) cap))
                    [k [(str label " is " (count v) " characters — we have room "
                             "for " cap ". Trim it a little.")]]))))
        (session-fields form-fields)))

(defn validation-errors
  "All server-side validation for one submission: {field [messages]} or nil.

   Answer errors are keyed by field id; speaker errors by `:speaker-<field>` so
   a form field called `bio` and the speaker's `bio` never collide."
  [form-fields answers speaker]
  (let [answer-errs (when-not (m/validate (answers-schema form-fields) answers)
                      (me/humanize (m/explain (answers-schema form-fields) answers)))
        speaker-errs (when-not (m/validate SpeakerDraft speaker)
                       (me/humanize (m/explain SpeakerDraft speaker)))
        all (merge-with into
                        (or answer-errs {})
                        (max-length-errors form-fields answers)
                        (into {} (map (fn [[k v]] [(keyword (str "speaker-" (name k))) v]))
                              (or speaker-errs {})))]
    (when (seq all) all)))

;; --- CFP window + cap -------------------------------------------------------

(defn cfp-state
  "Is this CFP :not-open-yet, :open, or :closed? A missing window means open —
   an organizer who didn't set dates did not mean 'reject everyone'.

   The middle clause is how \"stays closed for now\" is read back. Creating an
   event with that box ticked records a call that closes the SAME INSTANT it
   opens (events/apply-create-defaults) — no extra flag, no sentinel date, just
   a window of zero width. A window that never had any width was never open, so
   the honest word for it is :not-open-yet, and the public page says 'isn't open
   yet' rather than 'has closed'. A close date that merely fell in the past is a
   different story and still reads :closed."
  ([event] (cfp-state event (Instant/now)))
  ([event now]
   (let [{:keys [cfp-opens-at cfp-closes-at]} event]
     (cond
       (and cfp-opens-at (.isBefore now ^Instant cfp-opens-at)) :not-open-yet
       (and cfp-opens-at cfp-closes-at (= cfp-opens-at cfp-closes-at)) :not-open-yet
       (and cfp-closes-at (.isAfter now ^Instant cfp-closes-at)) :closed
       :else :open))))

(defn accepting?
  ([event] (= :open (cfp-state event)))
  ([event now] (= :open (cfp-state event now))))

(defn submission-cap
  "Per-person cap from the event's settings; nil means no cap."
  [event]
  (get-in event [:settings :submissions-per-person-cap]))

(defn submission-count-for-email
  "How many talks this person has already submitted to this event."
  [event-id email]
  (let [email (people/normalize-email email)]
    (count (filter (fn [s]
                     (= email (people/normalize-email
                               (:email (first (:speakers s))))))
                   (store/submissions-for-event event-id)))))

(defn cap-reached?
  [event email]
  (when-let [cap (submission-cap event)]
    (>= (submission-count-for-email (:id event) email) cap)))

;; --- Row projection ---------------------------------------------------------

(def ^:private submission-keys
  [:id :event-id :form-snapshot :answers :speakers :status :priority
   :notified-at :source :created-at])

(defn row->submission [row]
  (when row
    (cr/closed-record
     (reduce (fn [m k] (if (contains? m k) m (assoc m k nil)))
             (select-keys row submission-keys)
             submission-keys))))

(defn title-of
  "The talk title, for lists and Slack messages."
  [submission]
  (or (get-in submission [:answers :talk-title]) "(untitled)"))

;; --- Mutations --------------------------------------------------------------

(>defn create-submission!
       "Record a talk submission. Appends `person.created` (only when the speaker is
   new to us) then `submission.created`, whose payload carries the COMPLETE
   record: the form snapshot, the answers, the speaker display block and the
   source. Sinks fire on submission.created.

   Throws ex-info {:type :cfp-closed} or {:type :cap-reached}."
       ([event answers speaker] [map? map? map? => map?]
                                (create-submission! event answers speaker "form" "speaker"))
       ([event answers speaker source actor]
        [map? map? map? string? string? => map?]
        (let [form (last (store/forms-for-event (:id event)))
              form-fields (:fields form)
              email (people/normalize-email (:email speaker))]
          (when-not (accepting? event)
            (throw (ex-info "The call for speakers is not open."
                            {:type :cfp-closed :state (cfp-state event)})))
          (when (cap-reached? event email)
            (throw (ex-info (str "Submission limit reached for " email)
                            {:type :cap-reached :cap (submission-cap event) :email email})))
          (let [[person created?] (people/find-or-new email (:name speaker))
                submission {:id (store/new-id)
                            :event-id (:id event)
                       ;; Rule 1: the defs travel WITH the submission, forever.
                            :form-snapshot form-fields
                            :answers answers
                       ;; The speaker block AS SUBMITTED — a snapshot, so a later
                       ;; profile edit never rewrites history on an accepted talk.
                            :speakers [(assoc speaker
                                              :person-id (:id person)
                                              :position 0)]
                            :status default-status
                            :priority false
                            :notified-at nil
                            :source source
                            :created-at (store/now-iso)}]
            (store/append-all!
             (cond-> []
               created? (conj {:type "person.created" :actor actor
                               :event-id (:id event) :payload person})
               true (conj {:type "submission.created" :actor actor
                           :event-id (:id event) :payload submission})))
            ;; The confirmation a speaker actually wants: proof it arrived, the
            ;; title we recorded, and a reply-to that reaches a person.
            (mail/send!
             {:to email
              :subject (str "We received your talk for " (:name event))
              :reply-to (:support-email event)
              :body (str "Hi " (or (first (str/split (str (:name speaker)) #"\s+")) "there") ",\n\n"
                         "We have your submission for " (:name event) ":\n\n"
                         "    \"" (get answers :talk-title) "\"\n\n"
                         "The programming committee reads every submission — real "
                         "people, not a filter — and you will hear from us either "
                         "way. You do not need to chase us.\n\n"
                         "If you need to change anything, reply to this note.\n\n"
                         (or (:support-email event) ""))}
             {:event-id (:id event) :kind "submission-confirmation" :actor actor
              :submission-id (:id submission)})
            (log/info :submission-created :event (:slug event) :email email
                      :title (get answers :talk-title) :person-created created?)
            (row->submission (store/submission-by-id (:id submission)))))))

;; --- Queries ----------------------------------------------------------------

(defn for-event
  "Every submission for an event, newest first."
  [event-id]
  (->> (store/submissions-for-event event-id)
       reverse
       (mapv row->submission)))

(defn by-id [submission-id]
  (row->submission (store/submission-by-id submission-id)))

(defn count-for-event [event-id]
  (count (store/submissions-for-event event-id)))

(defn unique-speaker-count
  [event-id]
  (->> (store/submissions-for-event event-id)
       (map #(:email (first (:speakers %))))
       (remove nil?)
       distinct
       count))

(comment
  (store/load!)
  (let [e (store/get-event-by-slug "enterprise-ai-summit-charlotte")]
    (cfp-state e)
    (for-event (:id e))))

;; --- Quick capture (bd d9o) -------------------------------------------------
;;
;; A good talk arrives as an email or a LinkedIn DM and dies in the inbox,
;; because getting it into the tool costs more than it is worth. So capture
;; parses NOTHING: paste it, name it if convenient, done. A messy row on the
;; board beats a perfect one that was never created.

(def captured-text-field
  "A synthetic field def so the raw paste is a first-class answer — snapshot,
   rendering and export machinery all work on it without special cases. It is
   :private, so a captured DM never appears on a public page."
  {:id :captured-text
   :type :textarea
   :label "As received"
   :private true
   :help "The message exactly as it arrived."})

(defn placeholder-email
  "When we have no email, invent a stable local one so the submission still has
   an owning identity. Never routable: nothing must ever try to mail it."
  [event]
  (str "capture+" (subs (str (random-uuid)) 0 8) "@" (:slug event) ".invalid"))

(>defn capture!
       "Create a submission on someone's behalf. Bypasses the CFP window and the
   per-person cap on purpose: an organizer capturing an email is not a speaker
   submitting, and the rules that protect the public form would only get in the
   way here."
       [event {:keys [captured-text title speaker-name speaker-email speaker-org source]} actor]
       [map? map? string? => (? map?)]
       (let [text (some-> captured-text str/trim not-empty)]
         (when text
           (let [form (last (store/forms-for-event (:id event)))
            ;; The snapshot gains the synthetic field, so the detail view can
            ;; render "As received" from the submission's own defs.
                 snapshot (vec (cons captured-text-field (:fields form)))
                 email (or (people/normalize-email speaker-email) (placeholder-email event))
                 nm (or (some-> speaker-name str/trim not-empty) "Unknown speaker")
                 [person created?] (people/find-or-new email nm)
                 submission {:id (store/new-id)
                             :event-id (:id event)
                             :form-snapshot snapshot
                             :answers {:captured-text text
                                       :talk-title (or (some-> title str/trim not-empty)
                                                       (str "Captured from " nm))}
                             :speakers [{:name nm
                                         :email email
                                         :org (some-> speaker-org str/trim not-empty)
                                         :person-id (:id person)
                                         :position 0}]
                             :status default-status
                             :priority false
                             :notified-at nil
                             :source (str "on-behalf-of:" (or (some-> source str/trim not-empty)
                                                              "other"))
                             :created-at (store/now-iso)}]
             (store/append-all!
              (cond-> []
                created? (conj {:type "person.created" :actor actor
                                :event-id (:id event) :payload person})
                true (conj {:type "submission.created" :actor actor
                            :event-id (:id event) :payload submission})))
             (log/info :submission-captured :event (:slug event) :by actor :source source)
        ;; NO confirmation email: this is the organizer's note, not the
        ;; speaker's submission, and the speaker may not know it exists yet.
             (row->submission (store/submission-by-id (:id submission)))))))

(defn captured?
  [submission]
  (str/starts-with? (str (:source submission)) "on-behalf-of"))
