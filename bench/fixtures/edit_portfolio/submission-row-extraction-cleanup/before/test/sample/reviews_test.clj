(ns sample.reviews-test
  (:require
   [sample.views.review :as review-view]))

(defn render-result [event row]
  (review-view/board-row event row nil true))

(defn render-weighted [event weighted-row criterion-row]
  [(review-view/board-row event weighted-row nil true)
   (review-view/board-row event criterion-row nil true)])

(defn render-visible [event row reviewer visibility-context]
  (review-view/board-row event row reviewer false visibility-context))
