(ns sample.status-workflow-test
  (:require
   [sample.views.review :as review-view]))

(defn render-status [event row]
  (str (review-view/board-row event row nil)))
