(ns cfp-scheduler-killer.reviews
  "Ratings, comments, status and the board's arithmetic.

   The doctrine this implements (docs/research/cfp-flow-comparison.md): CFP
   review is a conversation among trusted peers about a shared table of
   proposals. So:
     - a rating is person × submission — NEVER person × committee-assignment;
     - every score and every comment is visible to the whole committee;
     - the two default sorts ARE the two work queues, not a preference.

   Nothing here gates visibility. Committees scope the coverage DENOMINATOR and
   the push email; they never decide who may see or rate what."
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [cfp-scheduler-killer.store :as store]))

;; --- Stars ------------------------------------------------------------------

(def min-stars 1.0)
(def max-stars 5.0)

(def star-steps
  "1.0 … 5.0 in halves. Halves came from the fulcro app — reviewers wanted to
   say 'better than a 4 but I'm not championing it'."
  (mapv #(/ % 2.0) (range 2 11)))

(defn valid-stars?
  [x]
  (and (number? x)
       (<= min-stars (double x) max-stars)
       (zero? (mod (Math/round (* 2.0 (double x))) 1))
       (== (double x) (/ (Math/round (* 2.0 (double x))) 2.0))))

(defn parse-stars
  "Parse a star value from a form param. Returns nil when it isn't a legal step."
  [s]
  (let [n (cond
            (number? s) (double s)
            (string? s) (try (Double/parseDouble (str/trim s)) (catch Exception _ nil))
            :else nil)]
    (when (and n (valid-stars? n)) n)))

(def star-scale-help
  "The scale, in the words a human would use. Kept next to the scale itself so
   the message and the rule can never drift apart."
  "1 to 5, halves allowed — 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5")

;; --- Arithmetic -------------------------------------------------------------

(defn mean
  "Arithmetic mean, or nil for no values — nil means 'nobody has said', which is
   a different fact from 0 and must never render as one."
  [xs]
  (when (seq xs)
    (/ (reduce + 0.0 (map double xs)) (count xs))))

(defn stddev
  "POPULATION standard deviation of the scores actually given. Population, not
   sample: these are all the opinions there are, not a sample of a larger pool."
  [xs]
  (when (> (count xs) 1)
    (let [m (mean xs)
          n (count xs)]
      (Math/sqrt (/ (reduce + 0.0 (map #(let [d (- (double %) m)] (* d d)) xs)) n)))))

(def split-threshold
  "SPREAD (max − min) at or above which the committee is genuinely split.

   This began as a σ threshold of 1.2 and was wrong: with three reviewers,
   population σ tops out around 0.95 for a full two-star disagreement, so the
   flag never fired on exactly the rows it was meant to catch — including the
   'someone said 2 and someone said 4' example in its own docstring. Spread is
   also what a human means by split, and what a chair can verify at a glance."
  2.0)

(defn spread
  "max − min. nil for fewer than two opinions, because one person cannot
   disagree with themselves."
  [xs]
  (when (> (count xs) 1)
    (- (apply max (map double xs)) (apply min (map double xs)))))

(defn split?
  [xs]
  (when-let [d (spread xs)]
    (>= d split-threshold)))

(defn submission-stats
  "{:n :mean :stddev :split? :ratings :comments} for one submission."
  [submission-id]
  (let [ratings (store/ratings-for-submission submission-id)
        stars (map :stars ratings)]
    {:n (count ratings)
     :mean (mean stars)
     :stddev (stddev stars)
     :spread (spread stars)
     :split? (boolean (split? stars))
     :ratings ratings
     :comments (store/comments-for-submission submission-id)}))

(defn coverage
  "How much of the event has been reviewed enough to decide.

   `target` is the committee's coverage-target (Gallimore's 2-review rule). The
   denominator is every submission in scope, so the number can only go up by
   doing the work — never by narrowing the question."
  [event-id target]
  (let [subs (store/submissions-for-event event-id)
        total (count subs)
        covered (count (filter (fn [s]
                                 (>= (count (store/ratings-for-submission (:id s)))
                                     target))
                               subs))]
    {:total total
     :covered covered
     :target target
     :pct (if (pos? total) (double (* 100 (/ covered total))) 0.0)}))

;; --- Enrichment for the board ------------------------------------------------

(defn- person-name [person-id]
  (or (:name (store/person-by-id person-id)) "Someone"))

(defn enrich
  "A submission plus everything the board shows about it, with reviewer names
   resolved. One pass per row; the board is a few hundred rows at most."
  [submission]
  (let [{:keys [n mean stddev split? ratings comments]} (submission-stats (:id submission))]
    (assoc submission
           :n n
           :mean mean
           :stddev stddev
           :split? split?
           :ratings (mapv #(assoc % :person-name (person-name (:person-id %))) ratings)
           :comments (mapv #(assoc % :person-name (person-name (:person-id %))) comments))))

(defn enriched-for-event [event-id]
  (mapv enrich (store/submissions-for-event event-id)))

;; --- The two work queues ----------------------------------------------------

(def sort-presets
  [{:key "needs-reviews" :label "Needs reviews"
    :help "Fewest ratings first — the coverage worklist."}
   {:key "ready-to-decide" :label "Ready to decide"
    :help "Best-rated first, among those with enough reviews — the decision queue."}])

(def default-sort "needs-reviews")

(def ^:private org-size-order
  "Small → large, however the option is spelled."
  {"<1,000" 0 "1,000–10,000" 1 ">10,000" 2})

(defn- column-keyfn
  "The LEDGER's sortable columns (Gene, 2026-08-09): any table heading can
   sort. nil for a name that isn't a column."
  [col]
  (case col
    "title"    #(str/lower-case (str (get-in % [:answers :talk-title])))
    "speaker"  #(str/lower-case (str (:name (first (:speakers %)))))
    ;; F/L name sorts (Gene, 2026-08-10): first = the whole name as written;
    ;; last = its final word — honest enough for a review board.
    "speaker-first" #(str/lower-case (str (:name (first (:speakers %)))))
    "speaker-last"  #(str/lower-case (str (last (str/split (str (:name (first (:speakers %)))) #"\s+"))))
    "org"      #(str/lower-case (str (:org (first (:speakers %)))))
    "org-size" #(get org-size-order (get-in % [:answers :org-size]) 9)
    "voted"    :n
    "avg"      #(or (:mean %) -1.0)
    nil))

(defn sort-board
  "The TWO presets are the two jobs. `needs-reviews` = ascending rating count
   then newest first (do the neglected ones, freshest first).
   `ready-to-decide` = descending mean among rows that have hit the coverage
   target, then descending count; rows below target sink to the bottom,
   because a 5.0 from one person is not a decision.

   Everything else is a COLUMN sort: \"<col>\" ascending, \"<col>-desc\"
   descending — the ledger's clickable headings. Unknown keys fall back to
   needs-reviews rather than throwing on a hand-typed URL."
  [rows preset target]
  (case preset
    "ready-to-decide"
    (let [target (or target 2)]
      (vec (sort-by (fn [r]
                      (let [ready? (>= (:n r) target)]
                        [(if ready? 0 1)
                         (- (or (:mean r) 0.0))
                         (- (:n r))]))
                    rows)))

    "needs-reviews"
    (vec (sort-by (fn [r] [(:n r) (- (inst-ms (or (:created-at r) (java.time.Instant/EPOCH))))])
                  rows))

    ;; column sorts + fallback
    (let [[_ col dir] (re-matches #"(.+?)(-desc)?" (str preset))
          kf (column-keyfn col)]
      (if kf
        (let [sorted (vec (sort-by kf rows))]
          (if dir (vec (rseq sorted)) sorted))
        (sort-board rows "needs-reviews" target)))))

;; --- Search + filter --------------------------------------------------------

(defn matches-query?
  "Search across title, speaker name and org — the three things an organizer
   actually remembers about a talk."
  [row q]
  (if (str/blank? q)
    true
    (let [q (str/lower-case (str/trim q))
          sp (first (:speakers row))
          hay (str/lower-case
               (str/join " " (remove nil? [(get-in row [:answers :talk-title])
                                           (:name sp) (:org sp) (:title sp)])))]
      (str/includes? hay q))))

(defn status-counts
  [rows]
  (reduce (fn [m r] (update m (:status r) (fnil inc 0))) {} rows))

(defn track-counts
  "How many rows sit in each track — nil answers count under nil, so the
   facet row can show an honest \"(no track)\" bucket for submissions that
   predate the field (form snapshots make that a first-class state)."
  [rows]
  (reduce (fn [m r] (update m (get-in r [:answers :track]) (fnil inc 0)))
          {} rows))

(defn filter-board
  [rows {:keys [q status track]}]
  (cond->> rows
    (not (str/blank? q)) (filter #(matches-query? % q))
    (not (str/blank? status)) (filter #(= status (:status %)))
    ;; \"(none)\" selects the untracked bucket explicitly.
    (not (str/blank? track)) (filter #(= (if (= "(none)" track) nil track)
                                         (get-in % [:answers :track])))
    true vec))

;; --- Mutations --------------------------------------------------------------
;;
;; Be liberal in what you accept; be LOUD about what you refuse.
;;
;; Every mutation below used to have a silent escape hatch: a star value that
;; didn't parse, a comment that was all spaces, a status the event had never
;; heard of — each one quietly did nothing while the HTTP layer answered 303, so
;; an integrator posting `status=accepted` in lowercase lost the write and was
;; told it succeeded (bd sessionize-sched-killer-xmf). It even manufactured a
;; false pass in our own e2e driver.
;;
;; So: liberal, meaning a case variant of a status the event DOES have resolves
;; to the event's own spelling. Loud, meaning anything else throws a typed
;; ex-info carrying a :user-message the web layer can show verbatim — never a
;; no-op, never a nil return that a caller has to remember to check.

(defn- refuse!
  "Throw the one shape the web layer knows how to turn into a 422 + a notice.
   `:user-message` is written for the reviewer, not for a log."
  [type message data]
  (throw (ex-info message (merge {:type type :user-message message} data))))

(>defn set-rating!
       "Record one reviewer's current opinion of one talk.

   Appended, never updated: the projection upserts so the board shows the
   latest, and the log keeps every version — 'Ann moved from 2 to 4 after
   reading Gene's comment' is the most interesting fact in the system."
       [submission-id person-id stars actor]
       [string? string? number? string? => map?]
       (when-not (valid-stars? stars)
         (throw (ex-info (str "Not a legal star value: " stars)
                         {:type :invalid-stars :stars stars})))
       (let [submission (store/submission-by-id submission-id)]
         (when-not submission
           (throw (ex-info (str "No such submission: " submission-id)
                           {:type :no-such-submission :submission-id submission-id})))
         (let [previous (store/rating-by submission-id person-id)]
           (store/append! {:type "rating.set" :actor actor
                           :event-id (:event-id submission)
                           :payload {:submission-id submission-id
                                     :person-id person-id
                                     :stars (double stars)
                                     :previous-stars (:stars previous)
                                     :at (store/now-iso)}})
           (log/info :rating-set :submission-id submission-id :person-id person-id
                     :stars stars :previous (:stars previous))
           (store/rating-by submission-id person-id))))

(defn rate!
  "Rate from a RAW form/API value — the star scale enforced at the edge.

   Separate from `set-rating!` on purpose: `set-rating!` takes a number and is
   what the rest of the system calls, while this is the one place that has to
   cope with whatever an HTTP client typed. `\"99\"`, `\"abc\"`, `\"-3\"` and
   `\"\"` are four different mistakes and all four now say so.

   EMPTY IS A REFUSAL, NOT A CLEAR (decision, 2026-08-09). Clearing a rating
   would be a real event — `rating.cleared` — with its own fold, its own effect
   on every mean and on the coverage denominator, and no control anywhere in the
   product that asks for it. Silently treating a missing param as 'withdraw my
   opinion' would be a destructive write triggered by a typo, so we say 'pick a
   rating' instead. If clearing is ever wanted, it gets its own verb."
  [submission-id person-id raw-stars actor]
  (let [raw (str raw-stars)]
    (cond
      (str/blank? raw)
      (refuse! :missing-stars
               (str "Pick a rating before you save. Ratings are " star-scale-help
                    ". (Clearing a rating isn't something this tool does yet.)")
               {:allowed star-steps})

      :else
      (if-let [stars (parse-stars raw)]
        (set-rating! submission-id person-id stars actor)
        (refuse! :invalid-stars
                 (str "\"" (str/trim raw) "\" isn't a rating on this scale. "
                      "Ratings are " star-scale-help ".")
                 {:stars raw-stars :allowed star-steps})))))

(>defn add-comment!
       "Add one comment. Comments accumulate; nothing is ever edited away.

   A comment of nothing but whitespace is REFUSED rather than dropped: it used
   to return nil and the board answered 303, so a reviewer who hit Say with an
   empty box was told their thought had been recorded."
       [submission-id person-id body actor]
       [string? string? (? string?) string? => (? map?)]
       (let [body (some-> body str/trim not-empty)
             submission (store/submission-by-id submission-id)]
         (when-not submission
           (throw (ex-info (str "No such submission: " submission-id)
                           {:type :no-such-submission :submission-id submission-id})))
         (when-not body
           (refuse! :empty-comment
                    "Say something — an empty comment isn't a comment."
                    {}))
         (when body
           (let [id (store/new-id)]
             (store/append! {:type "comment.added" :actor actor
                             :event-id (:event-id submission)
                             :payload {:id id
                                       :submission-id submission-id
                                       :person-id person-id
                                       :body body
                                       :at (store/now-iso)}})
             (log/info :comment-added :submission-id submission-id :person-id person-id)
             (get-in (store/snapshot) [:comments id])))))

(defn canonical-status
  "Resolve a submitted status against a vocabulary, ignoring case and
   surrounding whitespace. Returns the VOCABULARY'S OWN spelling, or nil.

   Case-insensitive because the vocabulary is prose an organizer typed
   ('Accept Queue'), and demanding that an integrator reproduce our
   capitalization exactly is a rule with no purpose but to lose writes. The
   canonical spelling is what gets stored, so the log never learns a second way
   to say Accepted."
  [statuses to]
  (let [t (some-> to str str/trim str/lower-case not-empty)]
    (when t
      (first (filter #(= t (str/lower-case (str %))) statuses)))))

(>defn set-status!
       "Move a submission through the status vocabulary. The vocabulary lives in the
   event's settings, so an organizer can add 'Waitlisted' without a migration.

   `to` is matched case-insensitively (see `canonical-status`); a value that is
   genuinely not on this event throws :invalid-status carrying the whole list,
   so the caller can be TOLD what the choices are instead of guessing."
       [submission-id to actor]
       [string? (? string?) string? => map?]
       (let [submission (store/submission-by-id submission-id)]
         (when-not submission
           (throw (ex-info (str "No such submission: " submission-id)
                           {:type :no-such-submission :submission-id submission-id})))
         (let [event (store/get-event-by-id (:event-id submission))
               allowed (vec (get-in event [:settings :statuses]))
               raw to
               to (canonical-status allowed raw)]
           (when-not to
             (refuse! :invalid-status
                      (if (str/blank? (str raw))
                        (str "Choose a status. This event uses: "
                             (str/join ", " allowed) ".")
                        (str "\"" (str/trim (str raw)) "\" isn't a status on this event. "
                             "It uses: " (str/join ", " allowed) "."))
                      {:status raw :allowed allowed}))
           (let [from (:status submission)]
             (store/append! {:type "submission.status-changed" :actor actor
                             :event-id (:event-id submission)
                             :payload {:submission-id submission-id
                                       :from from :to to :actor actor
                                       :at (store/now-iso)}})
             (log/info :status-changed :submission-id submission-id :from from :to to)
             (store/submission-by-id submission-id)))))

(>defn toggle-priority!
       "The 🔥 flag — 'talk about this one on the call'."
       [submission-id actor]
       [string? string? => map?]
       (let [submission (store/submission-by-id submission-id)]
         (when-not submission
           (throw (ex-info (str "No such submission: " submission-id)
                           {:type :no-such-submission :submission-id submission-id})))
         (let [next? (not (boolean (:priority submission)))]
           (store/append! {:type "submission.priority-toggled" :actor actor
                           :event-id (:event-id submission)
                           :payload {:submission-id submission-id
                                     :priority next?
                                     :at (store/now-iso)}})
           (store/submission-by-id submission-id))))

;; --- By-reviewer pivot (the person page) ------------------------------------

(defn reviewer-summary
  "One reviewer's work on one event: their ratings with the talks they belong
   to, their mean, and the committee's mean for comparison.

   The comparison is the point — 'you rate a full point below the committee' is
   the kind of thing a chair needs to know, and it is not an accusation."
  [event-id person-id]
  (let [subs-by-id (into {} (map (juxt :id identity)) (store/submissions-for-event event-id))
        their-ratings (->> (store/ratings-by-person person-id)
                           (filter #(contains? subs-by-id (:submission-id %)))
                           (mapv (fn [r]
                                   (let [s (get subs-by-id (:submission-id r))]
                                     (assoc r
                                            :title (get-in s [:answers :talk-title])
                                            :submission s
                                            :committee-mean (:mean (submission-stats (:id s))))))))
        their-comments (->> (store/comments-by-person person-id)
                            (filter #(contains? subs-by-id (:submission-id %)))
                            (mapv (fn [c]
                                    (assoc c :title (get-in (get subs-by-id (:submission-id c))
                                                            [:answers :talk-title])))))
        their-mean (mean (map :stars their-ratings))
        ;; The committee mean over the SAME talks they rated — comparing their
        ;; average against talks they never saw would be arithmetic, not insight.
        peer-mean (mean (keep :committee-mean their-ratings))]
    {:ratings their-ratings
     :comments their-comments
     :mean their-mean
     :committee-mean peer-mean
     :rated-count (count their-ratings)
     :total-submissions (count subs-by-id)}))

(comment
  (store/load!)
  (let [e (store/get-event-by-slug "enterprise-ai-summit-charlotte")]
    (coverage (:id e) 2)
    (sort-board (enriched-for-event (:id e)) "ready-to-decide" 2)))
