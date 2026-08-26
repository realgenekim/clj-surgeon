(ns sample.views.people
  (:require
   [sample.views.review :as review]))

(defn reviewer-summary [summary]
  [:div
   [:strong (or (review/fmt-mean (:mean summary)) "—")]
   [:strong (or (review/fmt-mean (:committee-mean summary)) "—")]])

(defn rating-row [rating]
  [:div "★" (review/fmt-stars (:stars rating))])
