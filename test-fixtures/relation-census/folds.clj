(ns cfp-scheduler-killer.folds
  "REAL-BYTES fixture for relation_census.

   Provenance: every form below except the three marked CONSTRUCTED is copied
   verbatim from curtaincall-cfp-lens src/cfp_scheduler_killer/folds.clj at
   commit 963875358a37c48ab6175ea1bea22633e4fd0306 (2026-09-02). The only edit
   applied to the copied forms is that the body of the private helper
   fold-task-chase has been inlined into its own arm, because the census scopes
   sites by arm and the real repository routes that arm through a helper.

   The three CONSTRUCTED arms are marked in place with their intent."
  (:require
   [clojure.string :as str]))

(defmulti fold-event (fn [_state event] (:type event)))

(defmethod fold-event :default [state _event] state)

;; ---------------------------------------------------------------------------
;; Identity doors (verbatim). Their own writes are OUTSIDE every arm and are
;; therefore counted, never classified.
;; ---------------------------------------------------------------------------

;; INTENT: FOLD-IDEM-002
(defn- conj-once
  "IMMUTABLE policy. Append `x` unless an EQUAL member is already recorded."
  [coll x]
  (let [coll (vec (or coll []))]
    (if (some #(= x %) coll) coll (conj coll x))))

;; INTENT: FOLD-IDEM-002
(defn- cons-once
  "IMMUTABLE policy, newest-first: prepend `x` unless it is already recorded."
  [coll x]
  (let [coll (vec (or coll []))]
    (if (some #(= x %) coll) coll (vec (cons x coll)))))

;; INTENT: FOLD-IDEM-002
(defn- upsert-by
  "UPSERT policy. Replace the member whose identity matches `x`'s, in place;
   append when there is none.

   `identity-fn` must be TOTAL — a member whose key fields are absent falls back
   to its own value, so two anonymous members never collide."
  [identity-fn coll x]
  (let [coll (vec (or coll []))
        id (identity-fn x)
        i (first (keep-indexed (fn [i y] (when (= id (identity-fn y)) i)) coll))]
    (if i (assoc coll i x) (conj coll x))))

(defn- blank->nil [s] (when-not (str/blank? (str s)) s))

(defn- submission-speaker-identity
  "A submission speaker is the person, or the email when no person id exists —
   submissions/add-speaker! already refuses two speakers sharing an email — and
   otherwise the speaker block itself."
  [speaker]
  (or (blank->nil (:person-id speaker))
      (some-> (:email speaker) str str/trim str/lower-case blank->nil)
      speaker))

(defn- update-present [m k f] (if (contains? m k) (update m k f) m))
(defn- <-iso-instant [s] s)
(defn- event-by-id [state event-id] (get-in state [:events-by-id event-id]))
(defn- reminder-already-logged? [state reminder-id]
  (boolean (get-in state [:reminder-index reminder-id])))

;; ---------------------------------------------------------------------------
;; Arms
;; ---------------------------------------------------------------------------

;; VERBATIM body of fold-task-chase, inlined into its arm.
;; Expected class: :guarded — the not-any? on :chase-id dominates the write.
(defmethod fold-event "task.chase-recorded" [state {:keys [payload]}]
  (let [k [(:submission-id payload) (:key payload)]]
    (if (and (get-in state [:tasks k])
             (not (str/blank? (str (:chase-id payload))))
             (not-any? #(= (:chase-id payload) (:chase-id %))
                       (get-in state [:tasks k :chases])))
      (update-in state [:tasks k]
                 (fn [task]
                   (let [chase (update-present
                                 (select-keys payload [:chase-id :note :actor :at
                                                       :medium :subject :body
                                                       :delivery-mode])
                                 :at
                                 <-iso-instant)]
                     (-> task
                         (update :chases (fnil conj []) chase)
                         (assoc :last-chased-at (:at chase)
                                :chase-count (inc (or (:chase-count task) 0)))))))
      state)))

;; VERBATIM. Expected class: :set — the target is (fnil conj #{}).
(defmethod fold-event "agenda.session-starred"
  [state {:keys [event-id payload]}]
  (update-in state [:agenda-selections [event-id (:viewer-id payload)]]
             (fnil conj #{}) (:submission-id payload)))

;; VERBATIM. disj is not a collection write; this arm contributes no site.
(defmethod fold-event "agenda.session-unstarred"
  [state {:keys [event-id payload]}]
  (update-in state [:agenda-selections [event-id (:viewer-id payload)]]
             (fnil disj #{}) (:submission-id payload)))

;; VERBATIM. Expected class: :door — the call head is upsert-by.
(defmethod fold-event "submission.speaker-added" [state {:keys [payload]}]
  ;; INTENT: FOLD-IDEM-002 — UPSERT: one row per person (or per email), the
  ;; later fact winning in place, because presenter order is product-visible.
  (update-in state [:submissions (:submission-id payload) :speakers]
             #(upsert-by submission-speaker-identity % (:speaker payload))))

;; VERBATIM. Expected class: :door — the call head is conj-once.
(defmethod fold-event "speaker.blackout-window" [state {:keys [payload]}]
  (update-in state [:speakers [(:event-id payload) (:person-id payload)]
                    :blackout-windows]
             #(conj-once % (:window payload))))

;; CONSTRUCTED, modelled on the pre-fix announced-speaker-added arm.
;; Expected class: :raw — the only :raw site in this fixture. The if-let test
;; touches neither the target collection nor the written value's identity.
(defmethod fold-event "event.speaker-announced" [state {:keys [payload]}]
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (update-in state [:events slug :settings :announced-speakers]
               (fnil conj []) (select-keys payload [:person-id :name :title]))
    state))

;; CONSTRUCTED. Expected class: :unknown, reason :helper-mediated-guard —
;; the dominating test is a helper call carrying the written value's identity.
(defmethod fold-event "speaker.reminder-logged" [state {:keys [payload]}]
  (if (reminder-already-logged? state (:reminder-id payload))
    state
    (update-in state [:speakers (:person-id payload) :reminders]
               (fnil conj []) (select-keys payload [:reminder-id :at]))))

;; CONSTRUCTED. Expected class: :unknown, reason :polarity — the guard matches
;; target and identity but adds on PRESENCE, which is the wrong sense for an add.
(defmethod fold-event "task.chase-replayed" [state {:keys [payload]}]
  (let [k [(:submission-id payload) (:key payload)]]
    (if (some #(= (:chase-id payload) (:chase-id %))
              (get-in state [:tasks k :chases]))
      (update-in state [:tasks k :chases]
                 (fnil conj []) (select-keys payload [:chase-id :note]))
      state)))
