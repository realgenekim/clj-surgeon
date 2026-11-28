(ns cfp-scheduler-killer.inform
  "The decision flow: telling speakers what the committee decided.

   The rule this namespace exists to enforce, taken straight from Sessionize's
   own board (docs/sessionize-recon/notes.md §04): **a decision is invisible to
   the speaker until someone deliberately communicates it.** Sessionize warns
   'Speakers are not automatically informed… until you inform them, they won't
   be able to see the status', and that second, deliberate act is exactly right.
   Accepting a talk is a committee judgement; telling someone is a promise.

   So `status` and `notified-at` are separate facts, and the portal reads the
   second one. A talk can sit at Accepted for a week while the chair decides how
   to phrase the letter, and the speaker sees 'Under review' the whole time —
   which is the truth, from where they are standing."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [cfp-scheduler-killer.exports :as exports]
            [cfp-scheduler-killer.mail :as mail]
            [cfp-scheduler-killer.store :as store]))

;; --- Which statuses are decisions? ------------------------------------------

(def informable-statuses
  "Only TERMINAL decisions are communicated. Accept Queue and Decline Queue are
   the committee's own triage — telling a speaker they are 'in the accept queue'
   would be leaking a working state as though it were a promise."
  ["Accepted" "Waitlisted" "Declined"])

(def letter-templates
  {"Accepted" "letters/accepted.txt"
   "Waitlisted" "letters/waitlisted.txt"
   "Declined" "letters/declined.txt"})

(defn informable? [status] (boolean (get letter-templates status)))

;; --- Letters ----------------------------------------------------------------

(defn render-template
  "Fill {{merge-fields}} in `template`. An unknown field renders as empty rather
   than leaving `{{gibberish}}` in a letter a human is about to send."
  [template values]
  (str/replace template
               #"\{\{([a-z-]+)\}\}"
               (fn [[_ k]] (str (get values (keyword k) "")))))

(defn load-template
  [status]
  (when-let [path (get letter-templates status)]
    (when-let [res (io/resource path)]
      (slurp res))))

(defn- first-name [full]
  (or (first (str/split (str full) #"\s+")) "there"))

(defn merge-values
  "Everything a letter can say about one decision."
  [event submission]
  (let [sp (first (:speakers submission))
        when* (cond
                (and (:starts-on event) (:location event))
                (str ", " (:location event))
                (:location event) (str ", " (:location event))
                :else "")]
    {:event-name (:name event)
     :event-when when*
     :talk-title (get-in submission [:answers :talk-title])
     :speaker-name (:name sp)
     :speaker-first-name (first-name (:name sp))
     :speaker-email (:email sp)
     :support-email (or (:support-email event) "the organizers")}))

(defn letter-for
  "The rendered letter for one pending decision, or nil when the status isn't
   one we communicate. Returns {:subject :body :to :status}."
  [event submission]
  (when-let [template (load-template (:status submission))]
    (let [rendered (render-template template (merge-values event submission))
          [subject-line & body-lines] (str/split-lines rendered)
          subject (str/replace subject-line #"^Subject:\s*" "")]
      {:status (:status submission)
       :to (:email (first (:speakers submission)))
       :subject subject
       :body (str/trim (str/join "\n" body-lines))})))

;; --- The queue --------------------------------------------------------------

(defn pending-decisions
  "Submissions whose decision has NOT been communicated yet, newest first.
   This is the number the banner shouts."
  [event-id]
  (->> (store/submissions-for-event event-id)
       (filter #(and (informable? (:status %)) (nil? (:notified-at %))))
       (sort-by :created-at)
       reverse
       vec))

(defn pending-by-status
  "The queue grouped the way the page shows it: one block per outgoing letter."
  [event-id]
  (let [pending (pending-decisions event-id)]
    (for [status informable-statuses
          :let [rows (filterv #(= status (:status %)) pending)]
          :when (seq rows)]
      {:status status :rows rows})))

(defn informed
  "Decisions already communicated — the receipts."
  [event-id]
  (->> (store/submissions-for-event event-id)
       (filter :notified-at)
       (sort-by :notified-at)
       reverse
       vec))

;; --- Informing --------------------------------------------------------------

(defn- task-events
  "The default speaker tasks for a newly-accepted talk, as store events.

   Tasks are installed on INFORM, not on accept: a speaker who hasn't been told
   they're in has nothing to do, and a checklist that appears before the news
   would be the news."
  [event submission actor]
  (when (= "Accepted" (:status submission))
    (map-indexed
     (fn [i t]
       {:type "task.installed" :actor actor :event-id (:id event)
        :payload {:submission-id (:id submission)
                  :key (:key t)
                  :label (:label t)
                  :task-type (:type t)
                  :position i
                  :value nil
                  :completed-at nil
                  :at (store/now-iso)}})
     (get-in event [:settings :default-speaker-tasks]))))

(>defn inform!
       "Communicate one decision. Appends `submission.notified` (which is what makes
   the status visible to the speaker) and, for an acceptance, installs the
   default task checklist.

   The actual email lands with the comms slice; today this records the promise
   and the Log page shows it. Recording it honestly now is what lets the portal
   be truthful."
       ([event submission] [map? map? => (? map?)] (inform! event submission "organizer"))
       ([event submission actor]
        [map? map? string? => (? map?)]
        (let [fresh (store/submission-by-id (:id submission))]
          (cond
            (nil? fresh) nil
            (:notified-at fresh)
            (do (log/info :already-informed :submission-id (:id fresh)) nil)

            (not (informable? (:status fresh)))
            (throw (ex-info (str "Not a communicable decision: " (:status fresh))
                            {:type :not-informable :status (:status fresh)}))

            :else
            (let [letter (letter-for event fresh)]
              (store/append-all!
               (into [{:type "submission.notified" :actor actor :event-id (:id event)
                       :payload {:submission-id (:id fresh)
                                 :status-at-notify (:status fresh)
                                 :to (:to letter)
                                 :subject (:subject letter)
                                 :at (store/now-iso)}}]
                     (task-events event fresh actor)))
              ;; The letter goes out (or is recorded as would-have) AFTER the
              ;; fact is stored. Informing is a decision; delivery is an
              ;; attempt, and a failed attempt must not undo the decision — it
              ;; must be visible so someone can retry.
              (mail/send!
               (cond-> {:to (:to letter)
                        :subject (:subject letter)
                        :body (:body letter)
                        :reply-to (:support-email event)}
                 ;; Only an ACCEPTANCE carries a calendar invite. Attaching one
                 ;; to a decline would be grotesque.
                 (= "Accepted" (:status fresh))
                 (assoc :ics (exports/submission-ics event fresh)
                        :ics-filename (str (:slug event) ".ics")))
               {:event-id (:id event) :kind "decision" :actor actor
                :submission-id (:id fresh)})
              (log/info :speaker-informed :submission-id (:id fresh)
                        :status (:status fresh) :to (:to letter))
              (store/submission-by-id (:id fresh)))))))

(defn inform-all!
  "Inform every pending decision for an event (optionally only one status).
   Returns how many were told."
  ([event actor] (inform-all! event actor nil))
  ([event actor status]
   (let [rows (cond->> (pending-decisions (:id event))
                status (filter #(= status (:status %))))]
     (count (keep #(inform! event % actor) rows)))))

;; --- Dashboard alert rows ---------------------------------------------------

(defn missing-speaker-details
  "Accepted-and-informed speakers whose profile still has no bio or headshot.

   Derived from the person's profile AND their submitted speaker block, because
   a bio they typed into the CFP counts — asking again for something they have
   already given us is how tools lose people."
  [event-id]
  (->> (store/submissions-for-event event-id)
       (filter #(and (= "Accepted" (:status %)) (:notified-at %)))
       (filter (fn [s]
                 (let [sp (first (:speakers s))
                       profile (:profile (store/person-by-id (:person-id sp)))
                       bio (or (not-empty (str (:bio profile))) (not-empty (str (:bio sp))))
                       shot (or (not-empty (str (:headshot-url profile)))
                                (not-empty (str (:headshot-url sp))))]
                   (or (nil? bio) (nil? shot)))))
       vec))

(defn alert-rows
  "The 'Also check' rows: count first, then the sentence, then where to go.

   Count-first is a house rule — the number a human would panic about leads.
   A row with a zero count is not rendered: a dashboard of zeroes trains people
   to stop reading it."
  [event]
  (let [event-id (:id event)
        slug (:slug event)
        awaiting (count (filter #(= "Pending" (:status %))
                                (store/submissions-for-event event-id)))
        uncommunicated (count (pending-decisions event-id))
        missing (count (missing-speaker-details event-id))]
    (cond-> []
      (pos? awaiting)
      (conj {:key :awaiting :count awaiting
             :text (str "submission" (when (not= 1 awaiting) "s") " awaiting a decision")
             :href (str "/events/" slug "/board") :link "Review Board"})

      (pos? uncommunicated)
      (conj {:key :uncommunicated :count uncommunicated :urgent? true
             :text (str "decision" (when (not= 1 uncommunicated) "s")
                        " not yet communicated")
             :href (str "/events/" slug "/inform") :link "Inform Speakers"})

      (pos? missing)
      (conj {:key :missing :count missing
             :text (str "accepted speaker" (when (not= 1 missing) "s")
                        " missing a bio or headshot")
             :href (str "/events/" slug "/submissions") :link "Submissions"}))))

(comment
  (store/load!)
  (let [e (store/get-event-by-slug "enterprise-ai-summit-charlotte")]
    (alert-rows e)
    (pending-by-status (:id e))))
