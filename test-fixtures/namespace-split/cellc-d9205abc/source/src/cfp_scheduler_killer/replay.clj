(ns cfp-scheduler-killer.replay
  "The replay simulator — three weeks of a CFP, compressed into a minute.

   This is a demo device, and it earns its place by being HONEST about what it
   is: it does not fake a screen. It plays a scripted corpus **through the real
   mutation functions** — `submissions/create-submission!`, `reviews/set-rating!`,
   `reviews/add-comment!`, `reviews/set-status!` — so every row that appears on
   the board got there the same way a real one would, and the Log afterwards is
   indistinguishable from a real CFP's.

   If replaying broke, the app would be broken too. That is the point.

   THE GUARD: replay only ever runs into an event that the replay page itself
   created (`:replay? true` in its settings). A demo that could pollute a real
   conference's data is not a demo, it is a hazard."
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [cfp-scheduler-killer.committees :as committees]
            [cfp-scheduler-killer.events :as events]
            [cfp-scheduler-killer.people :as people]
            [cfp-scheduler-killer.reviews :as reviews]
            [cfp-scheduler-killer.store :as store]
            [cfp-scheduler-killer.submissions :as submissions])
  (:import (java.time LocalDate LocalDateTime)))

(def corpus-resource "replay/aie-corpus.json")

(def speeds
  "Wall-clock seconds for the whole simulated span."
  [{:key "60" :label "3 weeks in 60 seconds" :seconds 60}
   {:key "300" :label "3 weeks in 5 minutes" :seconds 300}
   {:key "1800" :label "3 weeks in 30 minutes" :seconds 1800}])

(def default-speed 60)

;; --- The corpus -------------------------------------------------------------

(defn load-corpus
  "The scripted corpus, or nil when it hasn't been installed.

   Format is the corpus's own (resources/replay/README.md): a `timeline` of
   entries on `offset-secs` from CFP open, each `kind` one of
   submission | rating | comment | status. Review entries join to their
   submission by `on-title` and name their reviewer by email — both of which
   make the file readable, which is why it was built that way."
  []
  (when-let [res (io/resource corpus-resource)]
    (try
      (let [data (json/read-str (slurp res) :key-fn keyword)
            timeline (vec (sort-by :offset-secs (:timeline data)))]
        {:meta (:meta data)
         :entries timeline
         :window-secs (or (get-in data [:meta :window-secs])
                          (:offset-secs (last timeline))
                          1)})
      (catch Exception e
        (log/error :corpus-unreadable :msg (.getMessage e))
        nil))))

(defn corpus-available? [] (some? (io/resource corpus-resource)))

;; --- Replay state (in memory, per event) ------------------------------------

(defonce ^{:doc "{event-id -> {:status :idx :day :speed :started-at :corpus …}}.
                 Runtime state, deliberately NOT stored: a replay is a thing
                 happening now, not a fact about the world."}
  runs (atom {}))

(defn state-for [event-id] (get @runs event-id))

(defn progress
  [event-id]
  (let [r (state-for event-id)
        corpus (:corpus r)
        entries (:entries corpus)
        idx (or (:idx r) 0)
        window (or (:window-secs corpus) 1)
        played (take idx entries)
        at-secs (or (:offset-secs (last played)) 0)
        kind-of (fn [e] (:kind e))]
    {:status (or (:status r) :idle)
     ;; Days are derived from the corpus's own offsets, so "day 12 of 21" means
     ;; the simulated season, not wall-clock.
     :day (inc (long (/ at-secs 86400)))
     :days (max 1 (long (Math/ceil (/ (double window) 86400.0))))
     :idx idx
     :total (count entries)
     :submissions (count (filter #(= "submission" (kind-of %)) played))
     :total-submissions (count (filter #(= "submission" (kind-of %)) entries))
     :reviews (count (filter #(#{"rating" "comment" "status"} (kind-of %)) played))
     :pct (if (seq entries) (* 100.0 (/ idx (count entries))) 0.0)}))

;; --- The guard --------------------------------------------------------------

(defn replay-event?
  "Only an event the replay page created may be replayed into."
  [event]
  (boolean (get-in event [:settings :replay?])))

(defn- ensure-replayable! [event]
  (when-not (replay-event? event)
    (throw (ex-info "Refusing to replay into an event the simulator did not create."
                    {:type :not-a-replay-event :slug (:slug event)}))))

;; --- Creating the demo event ------------------------------------------------

(def demo-committee
  [{:name "Gene Kim" :email "genek@itrevolution.net" :role "chair"}
   {:name "Ann Perry" :email "annp@itrevolution.net" :role "member"}
   {:name "Alex Broderick-Forster" :email "alex@itrevolution.net" :role "member"}])

(defn create-demo-event!
  "A fresh, clearly-labelled event to replay into. Never an existing one."
  [actor signed-in-person]
  (let [today (LocalDate/now)
        slug (str "aie-replay-" (events/random-suffix 6))
        event (events/create-event!
               {:name "AI Engineer World's Fair (replay demo)"
                :slug slug
                :tz "America/Los_Angeles"
                :starts-on (.plusMonths today 2)
                :ends-on (.plusDays (.plusMonths today 2) 2)
                :cfp-opens-at (.atTime (.minusDays today 21) 9 0)
                :cfp-closes-at (.atTime (.plusMonths today 1) 23 59)
                :support-email "speakers@ai.engineer"
                :location "San Francisco, CA"}
               actor)
        committee-id (:id (first (events/committees-for-event (:id event))))]
    ;; Mark it as a replay target — this single event IS the whole guard.
    (store/append! {:type "replay.marked" :actor actor :event-id (:id event)
                    :payload {:event-id (:id event) :at (store/now-iso)}})
    (doseq [m demo-committee]
      (try (committees/add-member! committee-id m actor)
           (catch clojure.lang.ExceptionInfo _ nil)))
    (when (and signed-in-person
               (not (some #(= (:email signed-in-person) (:email %)) demo-committee)))
      (try (committees/add-member! committee-id
                                   {:name (:name signed-in-person)
                                    :email (:email signed-in-person)
                                    :role "chair"}
                                   actor)
           (catch clojure.lang.ExceptionInfo _ nil)))
    (events/event-by-slug slug)))

;; --- Playing one entry ------------------------------------------------------

(defn- reviewer-by-email
  "Corpus review entries name their reviewer by email — the three PC addresses.
   Falls back to any committee member so a corpus with an unexpected address
   still plays rather than silently dropping half its events."
  [event-id email]
  (let [members (mapcat #(committees/members-for-committee (:id %))
                        (store/committees-for-event event-id))
        want (people/normalize-email email)]
    (:person-id (or (first (filter #(= want (people/normalize-email (:email %))) members))
                    (first members)))))

(defn- submission-by-title
  "Review entries join by talk title (unique within the corpus)."
  [event-id title]
  (:id (first (filter #(= (str title) (str (get-in % [:answers :talk-title])))
                      (store/submissions-for-event event-id)))))

(defn play-entry!
  "Apply ONE corpus entry through the real mutation functions — the same ones a
   speaker or a committee member drives. Nothing here writes to the store
   directly."
  [event entry]
  (let [event-id (:id event)]
    (case (:kind entry)
      "submission"
      (let [sub (:submission entry)
            answers (:answers sub)
            sp (:speaker sub)
            form-fields (:fields (events/form-for-event event-id))
            ;; The corpus already keys answers by FIELD ID, so this is a
            ;; straight hand-off to the same parse the public form uses.
            params (reduce-kv (fn [m k v] (assoc m (keyword (str "answer-" (name k))) v))
                              {:speaker-name (:name sp)
                               :speaker-email (:email sp)
                               :speaker-title (:title sp)
                               :speaker-org (:org sp)
                               :speaker-bio (:bio sp)}
                              answers)]
        (submissions/create-submission!
         event
         (submissions/parse-answers form-fields params)
         (submissions/parse-speaker params)
         "form" "replay")
        nil)

      "rating"
      (when-let [sid (submission-by-title event-id (:on-title entry))]
        (when-let [pid (reviewer-by-email event-id (:by entry))]
          (reviews/set-rating! sid pid (double (:stars entry)) (str (:by entry))))
        nil)

      "comment"
      (when-let [sid (submission-by-title event-id (:on-title entry))]
        (when-let [pid (reviewer-by-email event-id (:by entry))]
          (reviews/add-comment! sid pid (:body entry) (str (:by entry))))
        nil)

      "status"
      (when-let [sid (submission-by-title event-id (:on-title entry))]
        (try (reviews/set-status! sid (:to entry) (str (:by entry)))
             (catch clojure.lang.ExceptionInfo _ nil))
        nil)

      nil)))

(defn tick!
  "Advance the replay to `target-idx`, applying every entry in between.

   Public and pure-ish on purpose: the test drives this directly instead of
   waiting on wall-clock, and the running loop calls exactly the same function."
  [event target-idx]
  (ensure-replayable! event)
  (let [event-id (:id event)]
    (loop []
      (let [run (state-for event-id)
            entries (get-in run [:corpus :entries])
            idx (:idx run 0)]
        (when (and run (< idx (min target-idx (count entries))))
          (let [entry (nth entries idx)]
            (try (play-entry! event entry)
                 (catch Exception e
                   ;; One bad entry must not stall the whole replay — log it,
                   ;; move on, and let the progress bar keep telling the truth.
                   (log/warn :replay-entry-failed :idx idx :msg (.getMessage e))))
            (swap! runs update event-id
                   (fn [r] (-> r (update :idx inc)
                               (assoc :at-secs (:offset-secs entry)))))
            (recur)))))
    (progress event-id)))

;; --- The loop ---------------------------------------------------------------

(defn- entries-due
  "How many entries should have played by now, given elapsed wall time."
  [run now-ms]
  (let [{:keys [started-at speed corpus paused-ms]} run
        entries (:entries corpus)
        elapsed (- now-ms started-at (or paused-ms 0))
        fraction (min 1.0 (/ (double elapsed) (* 1000.0 speed)))]
    (long (Math/ceil (* fraction (count entries))))))

(defn start!
  "Begin (or resume) a replay. Runs on a future; each tick applies whatever is
   due and pushes progress. `on-tick` is called after every advance so the
   caller can push SSE without this namespace knowing about SSE."
  [event speed on-tick]
  (ensure-replayable! event)
  (let [event-id (:id event)
        corpus (load-corpus)]
    (when corpus
      (swap! runs update event-id
             (fn [r]
               (merge {:idx 0 :at-secs 0}
                      r
                      {:status :playing
                       :speed (or speed default-speed)
                       :corpus corpus
                       :started-at (- (System/currentTimeMillis)
                                      ;; resume where we left off
                                      (long (* 1000.0 (or speed default-speed)
                                               (/ (double (:idx r 0))
                                                  (max 1 (count (:entries corpus)))))))
                       :paused-ms 0})))
      (future
        (try
          (loop []
            (let [run (state-for event-id)]
              (when (= :playing (:status run))
                (let [due (entries-due run (System/currentTimeMillis))]
                  (when (> due (:idx run 0))
                    (tick! event due)
                    (when on-tick (on-tick)))
                  (if (>= (:idx (state-for event-id) 0)
                          (count (get-in run [:corpus :entries])))
                    (do (swap! runs assoc-in [event-id :status] :done)
                        (when on-tick (on-tick))
                        (log/info :replay-finished :event-id event-id))
                    (do (Thread/sleep 250) (recur)))))))
          (catch Exception e
            (log/error :replay-loop-failed :msg (.getMessage e) :error e)
            (swap! runs assoc-in [event-id :status] :error))))
      (progress event-id))))

(defn pause!
  [event]
  (let [event-id (:id event)]
    (swap! runs update event-id
           (fn [r] (when r (assoc r :status :paused))))
    (progress event-id)))

(defn skip-to-end!
  "Play every remaining entry immediately — the impatient-judge button."
  [event]
  (ensure-replayable! event)
  (let [event-id (:id event)
        corpus (or (get-in @runs [event-id :corpus]) (load-corpus))]
    (when corpus
      (swap! runs update event-id
             (fn [r] (merge {:idx 0 :at-secs 0} r
                            {:corpus corpus :status :playing})))
      (tick! event (count (:entries corpus)))
      (swap! runs assoc-in [event-id :status] :done)
      (progress event-id))))

(defn reset!
  "Forget the run state. Does NOT undo what was played — the log is
   append-only, and pretending otherwise would be the one dishonest thing this
   namespace could do."
  [event-id]
  (swap! runs dissoc event-id)
  nil)

(comment
  (store/load!)
  (corpus-available?)
  (let [e (create-demo-event! "gene@example.com" nil)]
    (skip-to-end! e)))
