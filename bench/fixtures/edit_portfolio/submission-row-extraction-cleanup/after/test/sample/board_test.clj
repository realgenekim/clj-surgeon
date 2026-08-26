(ns sample.board-test
  (:require
   [sample.views.review :as view-review]
   [sample.views.submission-row :as submission-row]))

(defn render-unrated [event row person]
  (str (submission-row/board-row event row person)))

(defn render-rated [event row person]
  (str (submission-row/board-row event row person)))
