(ns sample.views-test
  (:require
   [sample.views.review :as review]))

(defn render-opinions [row]
  (#'review/opinions-block row))

(defn render-histogram [ratings]
  (review/star-histogram ratings))
