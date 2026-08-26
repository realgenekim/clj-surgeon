(ns sample.review-updates
  (:require
   [sample.reviews :as reviews]
   [sample.views.review :as view-review]
   [sample.views.submission-row :as submission-row]))

(defn push-person-row [event row person]
  (submission-row/board-row event (reviews/enrich row) person nil nil true))

(defn push-active-row [event row person]
  (submission-row/board-row event (reviews/enrich row) person nil nil true))
