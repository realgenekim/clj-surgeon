(ns cfp-scheduler-killer.committees
  "Programming-committee membership, on the append-only store.

   Doctrine (docs/design/domain-model.md): a committee is a ROSTER + a SCOPE
   FILTER, never a permission fortress. Adding someone here does not gate what
   anyone can see or rate; it says who is expected to show up, supplies the
   coverage denominator, and routes the push emails for that slice.

   `people` are event-independent identities keyed by lowercased email, so the
   same human recurs across years and later becomes a speaker without being
   recreated. Adding an existing person to a new committee reuses their row and
   NEVER overwrites their name — they own their identity, an organizer's typing
   does not.

   Removing a member appends a `member.removed` event. Nothing is erased: the
   log still shows they were on the roster and when they left."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [taoensso.timbre :as log]
            [closed-record.core :as cr]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [cfp-scheduler-killer.people :as people]
            [cfp-scheduler-killer.store :as store]))

;; --- Roles ------------------------------------------------------------------

(def roles
  "reviewer | chair. The chair badge is recorded NOW so the roster is honest
   about who runs the committee; chair SEMANTICS (gating notify / harden
   actions) land in a later slice.

   \"reviewer\" replaced \"member\" as the domain word (ratified 2026-08-09):
   the person's job is to review, and \"member\" collided with the membership
   ROW that records it. The old spelling is not migrated — the log is
   append-only and every historical member.added still says \"member\". It is
   accepted on read and normalised to \"reviewer\" in the projection
   (store/fold-event \"member.added\"), so a log written before today replays
   into today's vocabulary without a single event being rewritten."
  ["reviewer" "chair"])

(def default-role "reviewer")

(def legacy-role
  "The pre-2026-08-09 spelling of `reviewer`. Read-accepted, never written."
  "member")

(defn normalize-role
  "Historical roles in and today's vocabulary out."
  [role]
  (if (= legacy-role role) "reviewer" role))

;; --- Parsing + validation ---------------------------------------------------

(def email-pattern people/email-pattern)

;; Identity lives in one place for every slice — see cfp-scheduler-killer.people.
(def normalize-email people/normalize-email)

(defn- blank->nil [s]
  (when-not (str/blank? s) (str/trim s)))

(defn parse-member-form
  "Turn raw form params into a typed member draft."
  [params]
  {:name  (blank->nil (:name params))
   :email (normalize-email (:email params))
   :role  (normalize-role (or (blank->nil (:role params)) default-role))})

(def MemberDraft
  [:map {:closed false}
   [:name [:string {:min 1 :max 200
                    :error/message "A name is required."}]]
   [:email [:re {:error/message "That doesn't look like an email address."}
            email-pattern]]
   ;; "member" is still ACCEPTED — a form posted by an older page, or a replay
   ;; script written against the old vocabulary, must not fail validation.
   [:role [:enum {:error/message "Pick reviewer or chair."}
           "reviewer" "chair" "member"]]])

(defn validation-errors
  "Return {field [messages]} for a member draft, or nil when it is valid."
  [draft]
  (when-not (m/validate MemberDraft draft)
    (me/humanize (m/explain MemberDraft draft))))

;; --- Row projections --------------------------------------------------------

(def ^:private member-keys
  "Canonical keys every member closed-record carries. Views depend on these —
   a missing key throws instead of rendering a silent nil."
  [:membership-id :person-id :name :email :role :created-at])

(defn row->member [row]
  (when row
    (cr/closed-record
     (reduce (fn [m k] (if (contains? m k) m (assoc m k nil)))
             (select-keys row member-keys)
             member-keys))))

(defn- membership->member
  "Join a folded membership to its person to make a roster line."
  [membership]
  (let [person (store/person-by-id (:person-id membership))]
    (row->member {:membership-id (:id membership)
                  :person-id (:person-id membership)
                  :name (:name person)
                  :email (:email person)
                  :role (:role membership)
                  :created-at (:created-at membership)})))

;; --- Queries ----------------------------------------------------------------

(defn members-for-committee
  "The roster: everyone on this committee, oldest membership first."
  [committee-id]
  (mapv membership->member (store/memberships-for-committee committee-id)))

(def person-by-email people/by-email)

(defn membership-by-id
  "A membership joined to the event that owns it — enough to redirect back to
   the right dashboard after a mutation."
  [membership-id]
  (when-let [m (get-in (store/snapshot) [:memberships membership-id])]
    (let [committee (get-in (store/snapshot) [:committees (:committee-id m)])
          event (store/get-event-by-id (:event-id committee))
          person (store/person-by-id (:person-id m))]
      {:membership-id (:id m)
       :committee-id (:committee-id m)
       :person-id (:person-id m)
       :name (:name person)
       :email (:email person)
       :role (:role m)
       :event-id (:id event)
       :event-slug (:slug event)})))

(defn committee-by-id
  "A committee joined to its event (for the redirect and the event id)."
  [committee-id]
  (when-let [c (get-in (store/snapshot) [:committees committee-id])]
    (let [event (store/get-event-by-id (:event-id c))]
      {:committee-id (:id c)
       :name (:name c)
       :event-id (:id event)
       :event-slug (:slug event)})))

(defn person-detail
  "A person plus the committees of ONE event they sit on. Returns nil when the
   person is unknown. Committee membership is scoped to the event because that
   is the page's question — the person themself is event-independent."
  [event-id person-id]
  (when-let [person (store/person-by-id person-id)]
    (let [committees (into {} (map (juxt :id identity))
                           (store/committees-for-event event-id))
          memberships (->> (vals (:memberships (store/snapshot)))
                           (filter #(and (= person-id (:person-id %))
                                         (contains? committees (:committee-id %))))
                           (sort-by :created-at)
                           (mapv (fn [m]
                                   {:membership-id (:id m)
                                    :committee-id (:committee-id m)
                                    :committee-name (get-in committees [(:committee-id m) :name])
                                    :role (:role m)
                                    :created-at (:created-at m)})))]
      {:person person
       :memberships memberships
       :chair? (boolean (some #(= "chair" (:role %)) memberships))})))

(defn reviewer-count-for-event
  "How many people can still reach this conference at all — counted across ALL
   of its committees, because per-event authorization does not care which one
   you sit on (auth/member-of-event?)."
  [event-id]
  (let [committee-ids (into #{} (map :id) (store/committees-for-event event-id))]
    (count (filter #(contains? committee-ids (:committee-id %))
                   (vals (:memberships (store/snapshot)))))))

(defn member-on-committee?
  "Is this email already on this committee?"
  [committee-id email]
  (let [email (normalize-email email)]
    (boolean
     (some (fn [m]
             (= email (:email (store/person-by-id (:person-id m)))))
           (store/memberships-for-committee committee-id)))))

;; --- Mutations --------------------------------------------------------------

(>defn add-member!
       "Add a person to a committee. Appends `person.created` (only when the person
   is genuinely new) followed by `member.added`.

   Throws ex-info {:type :already-member} if they are already on this committee,
   and {:type :no-such-committee} if the committee id is unknown."
       ([committee-id member] [string? map? => map?] (add-member! committee-id member "organizer"))
       ([committee-id member actor]
        [string? map? string? => map?]
        (let [name* (blank->nil (:name member))
              email (normalize-email (:email member))
              role  (normalize-role (or (blank->nil (:role member)) default-role))
              committee (get-in (store/snapshot) [:committees committee-id])]
          (when-not committee
            (throw (ex-info (str "No such committee: " committee-id)
                            {:type :no-such-committee :committee-id committee-id})))
          (when (member-on-committee? committee-id email)
            (throw (ex-info (str email " is already on this committee.")
                            {:type :already-member :committee-id committee-id :email email})))
          (let [existing (people/by-email email)
                created? (nil? existing)
                person (or existing
                           {:id (store/new-id)
                            :email email
                       ;; Identity belongs to the person: an existing row is
                       ;; never renamed by an organizer's typing.
                            :name name*
                            :profile {}
                            :created-at (store/now-iso)})
                membership {:id (store/new-id)
                            :committee-id committee-id
                            :event-id (:event-id committee)
                            :person-id (:id person)
                            :role role
                            :created-at (store/now-iso)}]
            (store/append-all!
             (cond-> []
               ;; The person payload stays pure identity (no event-id — people are
          ;; event-independent); the ENVELOPE records the context they appeared in.
               created? (conj {:type "person.created" :actor actor
                               :event-id (:event-id committee) :payload person})
               true (conj {:type "member.added" :actor actor
                           :payload (assoc membership :email email :name (:name person))})))
            (log/info :member-added :email email :role role
                      :committee-id committee-id :person-created created?)
            (membership->member (get-in (store/snapshot) [:memberships (:id membership)]))))))

(>defn remove-member!
       "Remove a membership by appending `member.removed`.

   The PERSON survives on purpose: identity persists across committees and
   events, so removing someone here never erases who they are — and the log
   still records that they were once on the roster.

   Refuses to remove the LAST reviewer of an event ({:type :last-reviewer}).
   Since authorization is now per-event, an event with an empty roster is an
   event nobody can open, edit, decide or export ever again — and the only way
   back in would be a hand-edited log. A destructive action whose result is an
   unreachable conference is not a permission we should offer."
       ([membership-id] [string? => (? map?)] (remove-member! membership-id "organizer"))
       ([membership-id actor]
        [string? string? => (? map?)]
        (if-let [m (membership-by-id membership-id)]
          (do
            (when (<= (reviewer-count-for-event (:event-id m)) 1)
              (log/warn :member-remove-refused-last-reviewer
                        :membership-id membership-id :event-id (:event-id m))
              (throw (ex-info "That is the last reviewer on this event."
                              {:type :last-reviewer
                               :membership-id membership-id
                               :event-id (:event-id m)})))
            (store/append!
             {:type "member.removed" :actor actor
              :payload {:id membership-id
                        :committee-id (:committee-id m)
                        :event-id (:event-id m)
                        :person-id (:person-id m)
                        :email (:email m)
                        :role (:role m)}})
            (log/info :member-removed :email (:email m) :committee-id (:committee-id m))
            m)
          (do (log/warn :member-remove-missing :membership-id membership-id) nil))))

(comment
  (store/load!)
  (let [e (store/get-event-by-slug "enterprise-ai-summit-charlotte")
        c (first (store/committees-for-event (:id e)))]
    (add-member! (:id c) {:name "Gene Kim" :email "genek@itrevolution.net" :role "chair"})
    (members-for-committee (:id c))))
