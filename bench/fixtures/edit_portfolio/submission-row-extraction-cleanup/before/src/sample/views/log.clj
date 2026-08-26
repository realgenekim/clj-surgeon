(ns sample.views.log
  (:require
   [sample.views.review :as review]))

(defn describe-rating [payload]
  (if (:previous-stars payload)
    (str "Changed " (review/fmt-stars (:previous-stars payload))
         " to " (review/fmt-stars (:stars payload)))
    (str "Rated " (review/fmt-stars (:stars payload)) " stars")))
