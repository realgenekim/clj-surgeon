(ns sample.status-workflow-test
  (:require
   [sample.views.review :as review-view]
   [sample.views.submission-row :as submission-row]))

(defn render-status [event row]
  (str (submission-row/board-row event row nil)))
