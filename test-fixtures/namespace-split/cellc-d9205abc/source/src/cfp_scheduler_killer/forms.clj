(ns cfp-scheduler-killer.forms
  "The CFP form builder — CRUD over the vector of field definitions.

   A form is DATA (docs/design/form-builder.md): one vector of field defs drives
   public rendering, malli validation, the board columns, exports and 'copy from
   last year'. This namespace is the only thing allowed to change that vector,
   and every change is an appended `form.updated` event carrying the COMPLETE
   new vector — so re-folding the log alone reproduces the edited form exactly.

   Four rules from the design doc are enforced HERE, not in the UI:

     1. **Field IDs are forever.** `mint-field-id` derives a kebab id from the
        label ONCE, at add time, and uniquifies it against every id the form has
        ever carried (retired fields included). Renaming a label never re-keys —
        `update-field!` cannot write :id, so answers keyed by that id keep
        meaning for the life of the conference.

     2. **Deleting is RETIRING.** Old submissions reference field ids, so an
        erased field would orphan their answers. `retire-field!` sets
        `:retired true`; the field stops being collected (see `active-fields`)
        and stops rendering on the public page, and every stored answer stays
        readable against the submission's own snapshot.

     3. **Locked fields are undeletable.** Talk title, abstract and the speaker
        block are the spine of the product; retiring them would leave a CFP that
        cannot describe a talk.

     4. **The type is fixed at birth.** Editing a field changes its label, help,
        options, limits and flags — never its :type or :id. Changing the type
        under stored answers is how a form builder corrupts its own history."
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [cfp-scheduler-killer.events :as events]
            [cfp-scheduler-killer.store :as store]))

;; --- Vocabulary -------------------------------------------------------------

(def editable-types
  "The types an organizer may add from the builder, in menu order.

   `:file` and `:group` are deliberately absent: uploads arrive with the uploads
   slice, and the repeatable speaker block is structural, not something you add
   twice."
  ;; Labels are HUMAN answer shapes, not datatypes (Gene, 2026-08-09: "we are
  ;; not DBAs") — the shape IS the length, so no character-limit box exists in
  ;; the builder and no "max N" chip appears to speakers. `default-cap` below
  ;; supplies the invisible server-side guard for each shape.
  [{:value "text"     :label "One line"}
   {:value "textarea" :label "A paragraph"}
   {:value "markdown" :label "Long answer"}
   {:value "select"   :label "Choose one"}
   {:value "url"      :label "A link"}
   {:value "email"    :label "Email"}])

(def editable-type-values (into #{} (map :value) editable-types))

(def default-cap
  "The invisible per-shape guard, applied when a field carries no explicit
   :max-length (seed fields keep theirs). Generous on purpose: these exist to
   stop a runaway paste, never to reject a good answer that runs a little
   long. Nobody sees these numbers — a 422 names them only when actually hit."
  {"text" 300 "url" 500 "email" 255 "textarea" 4000 "markdown" 20000})

(defn effective-cap
  "The cap that actually guards this field: its own, else the shape default."
  [f]
  (or (:max-length f) (get default-cap (name (or (:type f) "text")))))

(def editable-keys
  "What `update-field!` may write. :id and :type are absent ON PURPOSE — rules
   1 and 4 above. A whitelist, never (dissoc attrs :id), so a stray key from a
   form post can never reach a stored field def."
  [:label :help :placeholder :required :private :options :max-length :widget])

;; --- Field-def helpers (pure) ----------------------------------------------
;;
;; Field defs arrive as KEYWORDS from the seed vector and as STRINGS once they
;; have been through the log (JSON has no keywords). Everything here compares by
;; `name`, never by identity — that asymmetry has bitten this codebase before.

(defn field-id [f] (some-> (:id f) name))

(defn field-type [f] (some-> (:type f) name))

(defn group? [f] (= "group" (field-type f)))

(defn retired? [f] (boolean (:retired f)))

(defn locked? [f] (boolean (:locked f)))

(defn active-fields
  "The fields a speaker actually meets: everything not retired.

   This is the ONE filter the live CFP page, its parser and its validator all go
   through, so a retired field is never rendered, never collected and never
   validated. The group is kept — dropping it here would hide the speaker block."
  [fields]
  (vec (remove retired? fields)))

(defn retired-fields [fields]
  (vec (filter retired? fields)))

(defn find-field [fields id]
  (first (filter #(= (name id) (field-id %)) fields)))

(defn field-index [fields id]
  (first (keep-indexed (fn [i f] (when (= (name id) (field-id f)) i)) fields)))

(defn all-field-ids
  "Every id the form carries, including retired fields and the ids nested inside
   the speaker group. Uniqueness is checked against ALL of them — a new field
   called 'Name' must not collide with the speaker block's `speaker-name`."
  [fields]
  (into #{}
        (mapcat (fn [f] (cons (field-id f) (keep field-id (:fields f)))))
        fields))

(>defn mint-field-id
       "A stable kebab id derived from the label, uniquified with -2, -3, …

   Called ONCE, when the field is born. Reuses `events/slugify` (the same
   derivation the public CFP address uses) rather than inventing a second
   kebab-caser."
       [label fields]
       [(? string?) sequential? => string?]
       (let [taken (all-field-ids fields)
             base (let [s (events/slugify label)]
                    (if (str/blank? s) "field" s))]
         (if-not (contains? taken base)
           base
           (loop [n 2]
             (let [candidate (str base "-" n)]
               (if (contains? taken candidate) (recur (inc n)) candidate))))))

;; --- Reading the live form --------------------------------------------------

(defn form-for-event
  "The event's current form record. `last` because a re-install (copy from last
   year) appends a new form; the newest one is the live one."
  [event-id]
  (last (store/forms-for-event event-id)))

(defn fields-for-event [event-id]
  (vec (:fields (form-for-event event-id))))

(defn reviewed?
  "Has anyone actually looked at this form? True once the organizer edited it or
   pressed 'Looks right'. Both are recorded facts in the log — this never
   guesses, which is why the dashboard checklist can honestly tick on it."
  [event-id]
  (let [form (form-for-event event-id)]
    (boolean (or (:reviewed-at form) (:edited-at form)))))

;; --- Parsing + validating a field form (pure) -------------------------------

(defn- blank->nil [s]
  (when (string? s) (let [t (str/trim s)] (when-not (str/blank? t) t))))

(defn- checked?
  "An HTML checkbox posts \"on\" when ticked and nothing at all when not."
  [v]
  (boolean (blank->nil (str v))))

(defn- parse-max-length [s]
  (when-let [s (blank->nil s)]
    (try (Long/parseLong s) (catch Exception _ ::invalid))))

(defn parse-options
  "One option per line — the only editor that never needs a JS widget."
  [s]
  (->> (str/split-lines (str (or s "")))
       (map str/trim)
       (remove str/blank?)
       vec))

(defn parse-field-params
  "Raw form params -> a field attribute map. Kept public so the validation can
   be tested without a request."
  [params]
  {:label (blank->nil (:label params))
   :type (or (blank->nil (:type params)) "text")
   :required (checked? (:required params))
   :private (checked? (:private params))
   :help (blank->nil (:help params))
   :max-length (parse-max-length (:max-length params))
   :widget (when (= "radio" (blank->nil (:widget params))) "radio")
   :options (parse-options (:options params))})

(defn field-validation-errors
  "{field [messages]} or nil. Every message names the thing the organizer typed."
  [{:keys [label type max-length options]} & [{:keys [new?]}]]
  (let [errs (cond-> {}
               (str/blank? (str label))
               (assoc :label ["A question needs a label — it's what the speaker reads."])

               (and new? (not (contains? editable-type-values (str type))))
               (assoc :type ["Pick one of the listed field types."])

               (= ::invalid max-length)
               (assoc :max-length ["The character limit has to be a whole number."])

               (and (integer? max-length) (not (pos? max-length)))
               (assoc :max-length ["The character limit has to be greater than zero."])

               (and (= "select" (str type)) (empty? options))
               (assoc :options ["List at least one option, one per line."]))]
    (when (seq errs) errs)))

;; --- The write path ---------------------------------------------------------
;;
;; Every mutation lands as ONE `form.updated` event carrying the complete new
;; field vector. Two reasons for the whole vector rather than a per-key delta:
;; the store's rule 2 (payloads are COMPLETE — there is no other table to join
;; against later), and reorder, which is a statement about the WHOLE vector and
;; has no honest delta representation.

(defn- save-fields!
  "Append the new vector and return it as the store folded it (strings, not
   keywords — the canonical form, so callers never see the in-process shape)."
  [event new-fields change field-id* actor]
  (let [form (form-for-event (:id event))]
    (when-not form
      (throw (ex-info (str "No form installed on " (:slug event))
                      {:type :no-such-form :event-id (:id event)})))
    (store/append!
     {:type "form.updated"
      :actor (or actor "organizer")
      :event-id (:id event)
      :payload {:id (:id form)
                :event-id (:id event)
                :change change
                :field-id field-id*
                :at (store/now-iso)
                :fields (vec new-fields)}})
    (log/info :form-updated :event (:slug event) :change change :field field-id*)
    (fields-for-event (:id event))))

(>defn add-field!
       "Mint an id, append the field at the end of the session block, return the new
   field's id. `attrs` is the output of `parse-field-params` (already validated)."
       [event attrs actor]
       [map? map? (? string?) => string?]
       (let [fields (fields-for-event (:id event))
             id (mint-field-id (:label attrs) fields)
             {:keys [label type required private help max-length options widget]} attrs
             field (cond-> {:id id
                            :type (str type)
                            :label label
                            :required (boolean required)}
                     private (assoc :private true)
                     help (assoc :help help)
                     (integer? max-length) (assoc :max-length max-length)
                     (= "select" (str type)) (assoc :options (vec options))
                     (and (= "select" (str type)) (= "radio" widget)) (assoc :widget "radio"))
             ;; New questions land AFTER the last session field and BEFORE the
             ;; speaker block, which always reads last on the public page.
             [session others] (split-with (complement group?) fields)]
         (save-fields! event (vec (concat session [field] others)) "add-field" id actor)
         id))

(>defn update-field!
       "Rewrite the editable attributes of one field. Never :id, never :type."
       [event id attrs actor]
       [map? string? map? (? string?) => (? vector?)]
       (let [fields (fields-for-event (:id event))]
         (when-let [i (field-index fields id)]
           (let [before (nth fields i)
                 select? (= "select" (field-type before))
                 patch (cond-> {:label (:label attrs)
                                :required (boolean (:required attrs))
                                :private (boolean (:private attrs))}
                         true (assoc :help (:help attrs))
                         true (assoc :placeholder (:placeholder attrs))
                         true (assoc :max-length (when (integer? (:max-length attrs))
                                                   (:max-length attrs)))
                         select? (assoc :options (vec (:options attrs)))
                         select? (assoc :widget (when (= "radio" (:widget attrs)) "radio")))
                 ;; nil-valued keys are REMOVED rather than stored as null, so a
                 ;; cleared help text leaves a field def shaped like one that
                 ;; never had help — the seed shape and an edited shape agree.
                 merged (into (apply dissoc before (keys patch))
                              (remove (comp nil? val))
                              (select-keys patch editable-keys))]
             (save-fields! event (assoc fields i merged) "update-field" id actor)))))

(>defn move-field!
       "Swap a field with its neighbour. `direction` is \"up\" or \"down\".

   Refuses to swap across the speaker group: the group is structural and always
   reads last, so 'down' on the last question is a no-op, not a reshuffle."
       [event id direction actor]
       [map? string? string? (? string?) => (? vector?)]
       (let [fields (fields-for-event (:id event))
             i (field-index fields id)
             j (when i (if (= "up" direction) (dec i) (inc i)))]
         (when (and i j (<= 0 j) (< j (count fields))
                    (not (group? (nth fields i)))
                    (not (group? (nth fields j))))
           (save-fields! event
                         (assoc fields i (nth fields j) j (nth fields i))
                         (str "move-" direction) id actor))))

(>defn retire-field!
       "Hide a field from the live form WITHOUT erasing it. Throws
   {:type :locked-field} for a locked field — rule 3."
       [event id actor]
       [map? string? (? string?) => (? vector?)]
       (let [fields (fields-for-event (:id event))
             i (field-index fields id)]
         (when i
           (let [f (nth fields i)]
             (when (locked? f)
               (throw (ex-info (str "Locked field cannot be retired: " id)
                               {:type :locked-field :field-id id})))
             (save-fields! event
                           (assoc fields i (assoc f :retired true
                                                  :retired-at (store/now-iso)))
                           "retire-field" id actor)))))

(>defn restore-field!
       "Put a retired field back on the live form. Its id — and therefore every
   answer ever given to it — comes back with it."
       [event id actor]
       [map? string? (? string?) => (? vector?)]
       (let [fields (fields-for-event (:id event))
             i (field-index fields id)]
         (when i
           (save-fields! event
                         (assoc fields i (dissoc (nth fields i) :retired :retired-at))
                         "restore-field" id actor))))

(>defn mark-reviewed!
       "The organizer says they have read the form. A deliberate, recorded second
   act — the dashboard checklist ticks on this, never on a page view."
       [event actor]
       [map? (? string?) => any?]
       (when-let [form (form-for-event (:id event))]
         (store/append!
          {:type "form.reviewed"
           :actor (or actor "organizer")
           :event-id (:id event)
           :payload {:id (:id form) :event-id (:id event) :at (store/now-iso)}})))

;; --- Two-step delete confirmation (server state, per viewer) ----------------
;;
;; House rule: NEVER confirm() — a modal blocks the SSE stream. The server holds
;; "this person is being asked about this field", renders Confirm/Cancel from it,
;; and clears it when the answer arrives. It is keyed by PERSON so one
;; organizer's prompt never appears on another organizer's screen when the
;; region is pushed.
;;
;; It is session state, not a fact about the world, so — like login tokens — it
;; deliberately never reaches the log.

(defonce ^{:doc "{[person-id form-id] -> field-id}"} confirming-retire (atom {}))

(defn confirming-field
  "The field this person is currently being asked to confirm, or nil."
  [person-id form-id]
  (get @confirming-retire [person-id form-id]))

(defn ask-confirm-retire! [person-id form-id field-id*]
  (swap! confirming-retire assoc [person-id form-id] field-id*)
  nil)

(defn clear-confirm-retire! [person-id form-id]
  (swap! confirming-retire dissoc [person-id form-id])
  nil)

(comment
  (store/load!)
  (let [e (events/event-by-slug "enterprise-ai-summit-charlotte")]
    (fields-for-event (:id e))
    (mint-field-id "Talk title" (fields-for-event (:id e)))))
