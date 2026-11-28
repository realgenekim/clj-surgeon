(ns cfp-scheduler-killer.schedule
  "The blocking sheet, with superpowers.

   The design evidence (docs/research/blocking-sheet-scheduler.md): ITRev had
   BusyConf's drag-and-drop grid for three years and STILL ran scheduling in a
   Google Sheet called 'Schedule Blocking', pinned every year from 2016. The
   scheduling never happened in the CFP tool. It happened in Sheets.

   Why: schedule BUILDERS demand complete data — a session cannot exist on the
   grid without a room and a time. Schedule BUILDING is a negotiation full of
   partial states: 'maybe Tuesday', 'keynote TBD', 'one of these three, pending
   confirm'. Spreadsheets tolerate ambiguity; grids force premature commitment.

   So three rules run through this namespace:

     1. **Partial states are first-class.** A session may be placed with NO room
        (`:room-id nil`) — swyx does exactly this: 'usually initial cal invite
        has no details then we assign room later'. Unroomed is a column, not an
        error.
     2. **Conflicts are chips, never validation errors.** Every save succeeds.
        We compute the collisions and name them — who, where, when — and offer
        the fix. A tool that refuses the save just sends the work back to Sheets.
     3. **Slot arithmetic is automatic.** Jess and Ann computed 'we've filled 27
        slots and have 30 remaining' by hand in Slack every August. That is a
        `count`."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [cfp-scheduler-killer.exports :as exports]
            [cfp-scheduler-killer.store :as store])
  (:import (java.time LocalDate LocalTime)
           (java.time.format DateTimeFormatter)))

(def slot-granularity-minutes 15)

;; --- Times ------------------------------------------------------------------
;;
;; Days are ISO date strings and times are "HH:mm" strings, interpreted in the
;; EVENT's zone. Storing wall-clock avoids the class of bug where a schedule
;; silently shifts because someone's server moved zones — the grid an organizer
;; drew is the grid they get.

(defn parse-time
  "\"09:30\" -> minutes since midnight, snapped to the 15-minute grid.
   nil for anything unparseable."
  [s]
  (when-let [s (some-> s str str/trim not-empty)]
    (try
      (let [t (LocalTime/parse (if (= 5 (count s)) s (str s ":00")))
            m (+ (* 60 (.getHour t)) (.getMinute t))]
        (- m (mod m slot-granularity-minutes)))
      (catch Exception _ nil))))

(defn minutes->hhmm [m]
  (when m (format "%02d:%02d" (int (/ m 60)) (int (mod m 60)))))

(defn minutes->display
  "9:00am — the way a program is printed, not the way a machine stores it."
  [m]
  (when m
    (let [h (int (/ m 60))
          mm (int (mod m 60))
          ampm (if (< h 12) "am" "pm")
          h12 (cond (zero? h) 12 (> h 12) (- h 12) :else h)]
      (str h12 ":" (format "%02d" mm) ampm))))

(defn time-range-display [start end]
  (when start (str (minutes->display start) (when end (str "-" (minutes->display end))))))

(defn parse-day
  [s]
  (when-let [s (some-> s str str/trim not-empty)]
    (try (LocalDate/parse s) (catch Exception _ nil))))

(defn event-days
  "Every day of the event, as ISO strings. A single-day event yields one day;
   an event with no dates yields none, and the UI says so rather than inventing
   a Day 1."
  [event]
  (let [s (:starts-on event) e (or (:ends-on event) (:starts-on event))]
    (when (and s e (not (.isAfter ^LocalDate s ^LocalDate e)))
      (loop [d s acc []]
        (if (.isAfter ^LocalDate d ^LocalDate e)
          acc
          (recur (.plusDays ^LocalDate d 1) (conj acc (str d))))))))

(defn day-label
  "\"Day 1 — Oct 14\", matching ai.engineer's `day` string shape."
  [event day-str]
  (let [days (vec (event-days event))
        idx (.indexOf days (str day-str))
        d (parse-day day-str)]
    (when d
      (str (when (>= idx 0) (str "Day " (inc idx) " — "))
           (.format d (DateTimeFormatter/ofPattern "MMM d"))))))

(defn in-bounds?
  [event day-str]
  (boolean (some #(= (str day-str) %) (event-days event))))

;; --- Durations --------------------------------------------------------------

(def default-durations
  "Minutes by session format. Overridable per event in settings."
  {"Panel" 45 "Workshop" 90 :default 30})

(defn duration-for
  [event submission]
  (let [conf (merge default-durations (get-in event [:settings :default-durations]))
        fmt (get-in submission [:answers :session-format])]
    (or (get conf fmt) (get conf :default) 30)))

;; --- Queries ----------------------------------------------------------------

(defn locked?
  [event]
  (boolean (get-in event [:settings :schedule-lock :locked?])))

(defn lock-label [event] (get-in event [:settings :schedule-lock :version-label]))

(defn schedulable
  "The sessions eligible for the grid: accepted AND informed — the same gate the
   exports use. We do not place a talk whose speaker has not been told; the
   agenda is downstream of the promise, not ahead of it."
  [event-id]
  (exports/published-sessions event-id))

(defn placements
  "Every placed session, enriched with its submission and speakers."
  [event-id]
  (let [by-id (into {} (map (juxt :id identity)) (store/submissions-for-event event-id))]
    (->> (store/slots-for-event event-id)
         (keep (fn [slot]
                 (when-let [sub (get by-id (:submission-id slot))]
                   (assoc slot
                          :submission sub
                          :title (get-in sub [:answers :talk-title])
                          :speakers (:speakers sub)))))
         (sort-by (juxt :day :start))
         vec)))

(defn tray
  "Accepted + informed, not yet placed. The tray is where a session waits — not
   an error state, just work not done."
  [event-id]
  (let [placed (set (map :submission-id (store/slots-for-event event-id)))]
    (->> (schedulable event-id)
         (remove #(contains? placed (:id %)))
         vec)))

;; --- Conflicts --------------------------------------------------------------
;;
;; Named, never blocking. Each conflict says WHO and WHERE and offers both
;; sides, because "move this" and "move that" are equally valid answers and the
;; tool does not know which.

(defn- overlap?
  [a b]
  (and (= (:day a) (:day b))
       (< (or (:start a) 0) (or (:end b) 0))
       (< (or (:start b) 0) (or (:end a) 0))))

(defn- room-name [event-id room-id]
  (or (:name (first (filter #(= room-id (:id %)) (store/rooms-for-event event-id))))
      "no room yet"))

(defn conflicts
  "Every collision in the CURRENT partial schedule. Pure over the placements."
  [event]
  (let [event-id (:id event)
        placed (placements event-id)
        pairs (for [[i a] (map-indexed vector placed)
                    [j b] (map-indexed vector placed)
                    :when (< i j)]
                [a b])]
    (vec
     (concat
      ;; A person cannot be in two rooms at once. This is the one that actually
      ;; bites at a real conference, and the one the spreadsheet never caught.
      (for [[a b] pairs
            :let [sa (set (keep :person-id (:speakers a)))
                  sb (set (keep :person-id (:speakers b)))
                  shared (set/intersection sa sb)]
            :when (and (seq shared) (overlap? a b))
            :let [pid (first shared)
                  who (:name (store/person-by-id pid))]]
        {:type :speaker
         :severity :high
         :person-id pid
         :message (str "⚡ " who " is also in " (room-name event-id (:room-id b))
                       " at " (minutes->display (:start b)))
         :a a :b b})

      ;; Two sessions in one room. Unroomed sessions cannot double-book a room —
      ;; that is the whole point of letting a placement have no room.
      (for [[a b] pairs
            :when (and (:room-id a) (= (:room-id a) (:room-id b)) (overlap? a b))]
        {:type :room
         :severity :high
         :message (str "Room double-booked: " (room-name event-id (:room-id a))
                       " at " (minutes->display (max (:start a) (:start b))))
         :a a :b b})

      ;; A session parked on a day the event does not run.
      (for [p placed
            :when (not (in-bounds? event (:day p)))]
        {:type :bounds
         :severity :medium
         :message (str "\"" (:title p) "\" is on " (:day p)
                       ", which is outside the event dates")
         :a p})))))

(defn conflicted-submission-ids
  [event]
  (into #{} (mapcat (fn [c] (keep :submission-id [(:a c) (:b c)]))) (conflicts event)))

;; --- Slot arithmetic --------------------------------------------------------

(defn stats
  "The status bar. The arithmetic Jess and Ann did by hand every August."
  [event]
  (let [event-id (:id event)
        accepted (schedulable event-id)
        placed (placements event-id)
        blocks (store/blocks-for-event event-id)
        cs (conflicts event)]
    {:accepted (count accepted)
     :placed (count placed)
     :unplaced (count (tray event-id))
     :unroomed (count (remove :room-id placed))
     :conflicts (count cs)
     :rooms (count (store/rooms-for-event event-id))
     :per-day (vec (for [d (event-days event)]
                     {:day d
                      :label (day-label event d)
                      :sessions (count (filter #(= d (:day %)) placed))
                      :blocks (count (filter #(= d (:day %)) blocks))}))}))

;; --- Mutations --------------------------------------------------------------

(defn- ensure-unlocked!
  "A locked schedule refuses mutations. This is the harden step from the design
   doc: draft → published is a deliberate transition, not a continuous leak."
  [event]
  (when (locked? event)
    (throw (ex-info "The schedule is locked."
                    {:type :schedule-locked :version (lock-label event)}))))

(>defn add-room!
       [event name* actor]
       [map? string? string? => (? map?)]
       (ensure-unlocked! event)
       (when-let [nm (some-> name* str/trim not-empty)]
         (let [id (store/new-id)]
           (store/append! {:type "room.added" :actor actor :event-id (:id event)
                           :payload {:id id :event-id (:id event) :name nm
                                     :order (count (store/rooms-for-event (:id event)))
                                     :at (store/now-iso)}})
           (get-in (store/snapshot) [:rooms id]))))

(>defn rename-room!
       [event room-id name* actor]
       [map? string? string? string? => (? map?)]
       (ensure-unlocked! event)
       (when-let [nm (some-> name* str/trim not-empty)]
         (store/append! {:type "room.renamed" :actor actor :event-id (:id event)
                         :payload {:id room-id :event-id (:id event) :name nm
                                   :at (store/now-iso)}})
         (get-in (store/snapshot) [:rooms room-id])))

(>defn remove-room!
       [event room-id actor]
       [map? string? string? => any?]
       (ensure-unlocked! event)
       (store/append! {:type "room.removed" :actor actor :event-id (:id event)
                       :payload {:id room-id :event-id (:id event) :at (store/now-iso)}})
       room-id)

(>defn place!
       "Put a session on the grid. `room-id` may be nil — scheduled-but-unroomed is a
   real, intentional state. Conflicts do NOT prevent the save; they are reported
   on the next render."
       [event submission-id {:keys [day start duration room-id]} actor]
       [map? string? map? string? => (? map?)]
       (ensure-unlocked! event)
       (let [submission (store/submission-by-id submission-id)
             start-m (parse-time start)
             dur (or (when (number? duration) duration)
                     (when-let [d (some-> duration str str/trim not-empty)]
                       (try (Integer/parseInt d) (catch Exception _ nil)))
                     (when submission (duration-for event submission)))]
         (when (and submission start-m (parse-day day))
           (store/append!
            {:type "slot.assigned" :actor actor :event-id (:id event)
             :payload {:submission-id submission-id
                       :event-id (:id event)
                       :day (str (parse-day day))
                       :start start-m
                       :end (+ start-m (max slot-granularity-minutes dur))
                       :room-id (some-> room-id str not-empty)
                       :at (store/now-iso)}})
           (log/info :session-placed :submission-id submission-id :day day :start start-m)
           (store/slot-for submission-id))))

(>defn clear-slot!
       [event submission-id actor]
       [map? string? string? => any?]
       (ensure-unlocked! event)
       (store/append! {:type "slot.cleared" :actor actor :event-id (:id event)
                       :payload {:submission-id submission-id :event-id (:id event)
                                 :at (store/now-iso)}})
       submission-id)

(>defn add-block!
       "A placeholder that is not a submission: Lunch, Keynote TBD, Break. Blocks are
   how a draft schedule holds space for something not yet decided."
       [event {:keys [day start duration room-id label]} actor]
       [map? map? string? => (? map?)]
       (ensure-unlocked! event)
       (let [start-m (parse-time start)
             dur (or (some-> duration str str/trim not-empty
                             (as-> d (try (Integer/parseInt d) (catch Exception _ nil))))
                     30)
             lbl (or (some-> label str/trim not-empty) "Block")]
         (when (and start-m (parse-day day))
           (let [id (store/new-id)]
             (store/append! {:type "block.added" :actor actor :event-id (:id event)
                             :payload {:id id :event-id (:id event)
                                       :day (str (parse-day day))
                                       :start start-m :end (+ start-m dur)
                                       :room-id (some-> room-id str not-empty)
                                       :label lbl
                                       :at (store/now-iso)}})
             (get-in (store/snapshot) [:blocks id])))))

(>defn remove-block!
       [event block-id actor]
       [map? string? string? => any?]
       (ensure-unlocked! event)
       (store/append! {:type "block.removed" :actor actor :event-id (:id event)
                       :payload {:id block-id :event-id (:id event) :at (store/now-iso)}})
       block-id)

(>defn lock!
       [event actor]
       [map? string? => any?]
       (let [n (inc (count (filter #(= "schedule.locked" (:type %))
                                   (store/log-for-event (:id event)))))
             label (str "v" n)]
         (store/append! {:type "schedule.locked" :actor actor :event-id (:id event)
                         :payload {:event-id (:id event) :version-label label
                                   :at (store/now-iso)}})
         label))

(>defn unlock!
       [event actor]
       [map? string? => any?]
       (store/append! {:type "schedule.unlocked" :actor actor :event-id (:id event)
                       :payload {:event-id (:id event) :version-label (lock-label event)
                                 :at (store/now-iso)}})
       true)

;; --- The public agenda ------------------------------------------------------

(defn agenda
  "The published program, by day, in time order — placed sessions and blocks
   interleaved. Only accepted+informed sessions appear (the export gate)."
  [event]
  (let [event-id (:id event)
        published (set (map :id (exports/publishable-sessions event)))
        placed (filterv #(contains? published (:submission-id %)) (placements event-id))
        blocks (store/blocks-for-event event-id)]
    (vec
     (for [d (or (seq (event-days event)) (distinct (map :day placed)))]
       {:day d
        :label (day-label event d)
        :items (->> (concat
                     (for [p placed :when (= d (:day p))]
                       {:kind :session
                        :start (:start p) :end (:end p)
                        :title (:title p)
                        :room (when (:room-id p) (room-name event-id (:room-id p)))
                        :speakers (mapv :name (:speakers p))
                        :submission-id (:submission-id p)
                        :abstract (:abstract (exports/public-answers (:submission p)))})
                     (for [b blocks :when (= d (:day b))]
                       {:kind :block
                        :start (:start b) :end (:end b)
                        :title (:label b)
                        :room (when (:room-id b) (room-name event-id (:room-id b)))}))
                    (sort-by (juxt :start :title))
                    vec)}))))

(comment
  (store/load!)
  (let [e (store/get-event-by-slug "enterprise-ai-summit-charlotte")]
    (stats e)
    (conflicts e)
    (agenda e)))
