(ns sample.views-test
  (:require
   [sample.views.submission-row :as submission-row]))

(defn render-opinions [row]
  (#'submission-row/opinions-block row))

(defn render-histogram [ratings]
  (submission-row/star-histogram ratings))
