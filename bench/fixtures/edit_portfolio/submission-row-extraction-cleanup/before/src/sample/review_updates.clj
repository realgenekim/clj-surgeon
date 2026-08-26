(ns sample.review-updates
  (:require
   [sample.reviews :as reviews]
   [sample.views.review :as view-review]))

(defn push-person-row [event row person]
  (view-review/board-row event (reviews/enrich row) person nil nil true))

(defn push-active-row [event row person]
  (view-review/board-row event (reviews/enrich row) person nil nil true))
