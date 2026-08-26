(ns sample.views.review
  (:require
   [sample.events :as events]
   [sample.store :as store]
   [sample.views.submission-row :as submission-row]))

(defn keep-header [] [:header "Review"])

(defn content-status-control [event person]
  (when (submission-row/chair-on-event? event person)
    [:button "Set status"]))

(defn detail-controls [event row person mine]
  (submission-row/row-controls*
    event row person mine (submission-row/chair-on-event? event person) false))

(defn submission-detail-page [event row person]
  {:mine (submission-row/score-for-person (:ratings row) person)
   :reviewed? (submission-row/reviewed-by? row person)})

(defn review-summary [row]
  [:div
   [:span (or (submission-row/fmt-aggregate (:aggregate-score row)) "—")]
   [:span (or (submission-row/fmt-mean (:mean row)) "—")]])

(defn board-page [event rows person]
  (let [chair? (submission-row/chair-on-event? event person)]
    [:table
     (for [row rows]
       (submission-row/board-row event row person chair? nil false))]))

(defn keep-footer [] [:footer "Done"])
