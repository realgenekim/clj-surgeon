(ns sample.views.log
  (:require
   [sample.views.submission-row :as submission-row]))

(defn describe-rating [payload]
  (if (:previous-stars payload)
    (str "Changed " (submission-row/fmt-stars (:previous-stars payload))
         " to " (submission-row/fmt-stars (:stars payload)))
    (str "Rated " (submission-row/fmt-stars (:stars payload)) " stars")))
