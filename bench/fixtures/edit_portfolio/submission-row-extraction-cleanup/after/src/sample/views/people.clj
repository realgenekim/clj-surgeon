(ns sample.views.people
  (:require
   [sample.views.submission-row :as submission-row]))

(defn reviewer-summary [summary]
  [:div
   [:strong (or (submission-row/fmt-mean (:mean summary)) "—")]
   [:strong (or (submission-row/fmt-mean (:committee-mean summary)) "—")]])

(defn rating-row [rating]
  [:div "★" (submission-row/fmt-stars (:stars rating))])
