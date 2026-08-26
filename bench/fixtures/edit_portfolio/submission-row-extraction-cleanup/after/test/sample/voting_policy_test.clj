(ns sample.voting-policy-test
  (:require
   [sample.views.review :as review]
   [sample.views.submission-row :as submission-row]))

(defn visible-row [event row person]
  (submission-row/board-row event row person))

(defn hidden-row [event row person]
  (submission-row/board-row event row person))

(defn revealed-row [event row person]
  (submission-row/board-row event row person))
