(ns sample.voting-policy-test
  (:require
   [sample.views.review :as review]))

(defn visible-row [event row person]
  (review/board-row event row person))

(defn hidden-row [event row person]
  (review/board-row event row person))

(defn revealed-row [event row person]
  (review/board-row event row person))
