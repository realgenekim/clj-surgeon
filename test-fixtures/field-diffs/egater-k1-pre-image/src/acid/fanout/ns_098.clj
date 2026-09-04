(ns acid.fanout.ns-098
  (:require [acid.fanout.util-c :as u1]
            [acid.fanout.util-d :as u2]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]))

(defn local-98
  [id]
  (let [find-event (fn [z] {:id z :kind :local})]
    (find-event id)))

(def label-98 "find-event")

(defn doc-98
  "Superseded by fetch-event; the old name find-event stays in this docstring."
  [id]
  {:id id :kind :doc})

(defn other-98
  [id]
  (other/find-event id))

;; historical: find-event was defined here before ns-098 was split

(defn site-a-98
  [id]
  (store/find-event id))

(defn site-b-98
  [id]
  (let [ev (store/find-event id)]
    (assoc ev :seen true)))

(defn site-c-98
  [ids]
  (mapv store/find-event ids))

(defn f98-0
  [x]
  (second [x x]))

(defn f98-1
  [x]
  (last [x x]))

(defn f98-2
  [x]
  (vec (seq [x])))
