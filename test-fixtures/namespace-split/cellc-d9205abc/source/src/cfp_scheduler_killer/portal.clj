(ns cfp-scheduler-killer.portal
  "The speaker's side of the house.

   Two rules shape everything here:

   1. **The inform gate.** A speaker sees `Under review` until their decision has
      been deliberately communicated (see cfp-scheduler-killer.inform). This
      namespace never reads `:status` for display — it reads `:notified-status`,
      which only exists once someone told them.

   2. **No lock after acceptance.** swyx was explicit: accepted speakers can
      still edit. A talk is a living thing until it is delivered, and a tool that
      freezes it just moves the edits into email. Edits validate against the
      submission's own form SNAPSHOT, so the questions never shift underneath
      an answer."
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [cfp-scheduler-killer.store :as store]
            [cfp-scheduler-killer.submissions :as submissions]))

(def under-review-label "Under review")

;; --- What the speaker is allowed to know ------------------------------------

(>defn visible-status
       "The status AS THE SPEAKER MAY SEE IT.

   This is the single most important function in the speaker-facing app: it is
   the difference between a committee's working note and a promise. Until
   `notified-at` exists, everything reads 'Under review' — including a talk the
   committee accepted an hour ago."
       [submission]
       [map? => string?]
       (if (:notified-at submission)
         (or (:notified-status submission) (:status submission))
         under-review-label))

(defn informed? [submission] (some? (:notified-at submission)))

(defn accepted-and-informed? [submission]
  (and (informed? submission) (= "Accepted" (visible-status submission))))

;; --- Editing ----------------------------------------------------------------

(def editable-statuses
  "Who may still edit. A withdrawn or declined talk is closed — editing it would
   imply it is still in play, which would be unkind rather than permissive."
  #{"Pending" "Accept Queue" "Accepted" "Waitlisted" "Decline Queue" "Draft"})

(defn editable?
  [submission]
  (contains? editable-statuses (:status submission)))

(>defn update-answers!
       "Apply a speaker's edits to their own submission.

   Validation runs against the submission's FORM SNAPSHOT, not the live form —
   an organizer who added a question last week cannot retroactively make an old
   submission invalid. Returns {:ok true :submission ..} or
   {:ok false :errors {..}}."
       ([submission-id params] [string? map? => map?] (update-answers! submission-id params "speaker"))
       ([submission-id params actor]
        [string? map? string? => map?]
        (let [submission (store/submission-by-id submission-id)]
          (cond
            (nil? submission)
            {:ok false :errors {:_ ["That submission no longer exists."]}}

            (not (editable? submission))
            {:ok false :errors {:_ [(str "This submission can no longer be edited (it is "
                                         (:status submission) ").")]}}

            :else
            (let [snapshot (:form-snapshot submission)
                  ;; Only fields the request ACTUALLY carried count as edits.
                  ;; `parse-answers` fills every field key (nil when blank) so
                  ;; validation can name a missing required answer — but feeding
                  ;; that straight into an update would blank every answer the
                  ;; caller didn't mention. The portal form posts all fields, so
                  ;; this never bites in a browser; it would bite everything else.
                  submitted (select-keys
                             (submissions/parse-answers snapshot params)
                             (keep (fn [f]
                                     (let [k (keyword (name (:id f)))]
                                       (when (contains? params
                                                        (keyword (str "answer-" (name k))))
                                         k)))
                                   (submissions/session-fields snapshot)))
                  answers (merge (:answers submission) submitted)
                  speaker (first (:speakers submission))
             ;; Only the ANSWERS are being edited here, so the speaker block is
             ;; validated as-is rather than re-collected from the form.
                  errors (submissions/validation-errors snapshot answers
                                                        (select-keys speaker
                                                                     [:name :email :title
                                                                      :org :bio :headshot-url
                                                                      :linkedin-url]))]
              (if errors
                {:ok false :errors errors :answers answers}
                (let [before (select-keys (:answers submission) (keys submitted))
                      changed (into {} (remove (fn [[k v]] (= v (get before k)))) submitted)]
                  (if (empty? changed)
                    {:ok true :submission submission :unchanged? true}
                    (do
                      (store/append!
                       {:type "submission.answers-updated" :actor actor
                        :event-id (:event-id submission)
                        :payload {:submission-id submission-id
                                  :changed (mapv name (keys changed))
                                  :before (select-keys before (keys changed))
                                  :changes changed
                                  :at (store/now-iso)}})
                      (log/info :answers-updated :submission-id submission-id
                                :fields (mapv name (keys changed)))
                      {:ok true :submission (store/submission-by-id submission-id)})))))))))

;; --- Profile ----------------------------------------------------------------

(def profile-fields
  "What a speaker maintains about themselves. Kept small on purpose: every field
   here is one a conference actually reprints."
  [{:key :tagline :label "Tagline" :type "text"
    :help "One line, as it appears under your name in the program."}
   {:key :bio :label "Bio" :type "textarea"}
   {:key :headshot-url :label "Headshot URL" :type "url"}
   {:key :linkedin-url :label "LinkedIn" :type "url"}
   {:key :website-url :label "Website" :type "url"}])

(def ^:private url-keys #{:headshot-url :linkedin-url :website-url})

(defn parse-profile [params]
  (into {}
        (keep (fn [{:keys [key]}]
                (let [v (get params key)]
                  (when (some? v)
                    [key (when-not (str/blank? v) (str/trim v))]))))
        profile-fields))

(defn profile-errors
  [profile]
  (let [errs (into {}
                   (keep (fn [[k v]]
                           (when (and (contains? url-keys k) v
                                      (not (re-matches submissions/url-pattern v)))
                             [k ["Enter a full URL starting with http:// or https://"]])))
                   profile)]
    (when (seq errs) errs)))

(>defn update-profile!
       "A speaker editing their own description of themselves. Merges the keys they
   sent; a field they left blank is CLEARED (they meant to remove it), a field
   the form didn't include is untouched."
       ([person-id params] [string? map? => map?] (update-profile! person-id params "speaker"))
       ([person-id params actor]
        [string? map? string? => map?]
        (let [person (store/person-by-id person-id)
              profile (parse-profile params)
              errors (profile-errors profile)]
          (cond
            (nil? person) {:ok false :errors {:_ ["We don't know who you are."]}}
            errors {:ok false :errors errors :profile profile}
            :else
            (let [before (select-keys (:profile person) (keys profile))
                  changed (into {} (remove (fn [[k v]] (= v (get before k)))) profile)]
              (if (empty? changed)
                {:ok true :person person :unchanged? true}
                (do
                  (store/append!
                   {:type "person.profile-updated" :actor actor
                    :payload {:person-id person-id
                              :changed (mapv name (keys changed))
                              :before before
                              :changes changed
                              :at (store/now-iso)}})
                  (log/info :profile-updated :person-id person-id
                            :fields (mapv name (keys changed)))
                  {:ok true :person (store/person-by-id person-id)})))))))

(defn prefill-from-profile
  "Speaker-block defaults for a NEW submission, from what they already told us.
   Their profile is the reason they shouldn't have to type their bio twice."
  [person]
  (let [p (:profile person)]
    (into {}
          (remove (comp str/blank? str val))
          {:speaker-name (:name person)
           :speaker-email (:email person)
           :speaker-title (:tagline p)
           :speaker-bio (:bio p)
           :speaker-headshot-url (:headshot-url p)
           :speaker-linkedin (:linkedin-url p)})))

;; --- Tasks ------------------------------------------------------------------

(>defn complete-task!
       "Tick a checkbox or save a URL. `value` is nil for a plain check."
       ([submission-id task-key value]
        [string? string? (? string?) => (? map?)]
        (complete-task! submission-id task-key value "speaker"))
       ([submission-id task-key value actor]
        [string? string? (? string?) string? => (? map?)]
        (let [task (get-in (store/snapshot) [:tasks [submission-id task-key]])
              submission (store/submission-by-id submission-id)]
          (when (and task submission)
            (store/append!
             {:type "task.completed" :actor actor
              :event-id (:event-id submission)
              :payload {:submission-id submission-id
                        :key task-key
                        :value (some-> value str/trim not-empty)
                        :at (store/now-iso)}})
            (log/info :task-completed :submission-id submission-id :key task-key)
            (get-in (store/snapshot) [:tasks [submission-id task-key]])))))

(defn task-done?
  [task]
  (if (= "url" (:task-type task))
    (some? (not-empty (str (:value task))))
    (some? (:completed-at task))))

(defn tasks-for
  [submission-id]
  (mapv #(assoc % :done? (task-done? %)) (store/tasks-for-submission submission-id)))

(defn task-progress
  [submission-id]
  (let [ts (tasks-for submission-id)]
    {:total (count ts) :done (count (filter :done? ts))}))

;; --- The portal view model --------------------------------------------------

(defn my-submissions
  "Everything this person submitted, with the speaker-safe status and their
   tasks. One shape, so the view never has to decide what a speaker may see."
  [person-id]
  (mapv (fn [s]
          (let [event (store/get-event-by-id (:event-id s))]
            {:submission s
             :event event
             :visible-status (visible-status s)
             :informed? (informed? s)
             :editable? (editable? s)
             :tasks (tasks-for (:id s))
             :progress (task-progress (:id s))}))
        (store/submissions-for-person person-id)))

(defn speaker?
  "Has this person ever submitted anything? (The other way into the app.)"
  [person-id]
  (boolean (seq (store/submissions-for-person person-id))))

(comment
  (store/load!)
  (let [p (store/person-by-email "priya.raghavan@meridianmutual.example.com")]
    (my-submissions (:id p))))
