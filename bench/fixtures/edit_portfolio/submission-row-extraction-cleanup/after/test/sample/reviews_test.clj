(ns sample.reviews-test
  (:require
   [sample.views.review :as review-view]
   [sample.views.submission-row :as submission-row]))

(defn render-result [event row]
  (submission-row/board-row event row nil true))

(defn render-weighted [event weighted-row criterion-row]
  [(submission-row/board-row event weighted-row nil true)
   (submission-row/board-row event criterion-row nil true)])

(defn render-visible [event row reviewer visibility-context]
  (submission-row/board-row event row reviewer false visibility-context))
