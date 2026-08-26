(ns sample.board-test
  (:require
   [sample.views.review :as view-review]))

(defn render-unrated [event row person]
  (str (view-review/board-row event row person)))

(defn render-rated [event row person]
  (str (view-review/board-row event row person)))
