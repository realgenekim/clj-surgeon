(ns acid.fanout.ns-083
  (:require [acid.fanout.util-d :as u1]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]
            [acid.fanout.util-a :as u2]))

(defn local-83
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-83 "find-event")

(defn doc-83
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-83
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-083 was split

(defn site-a-83
    [id]
    (store/find-event id))

(defn site-b-83
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-83
    [ids]
    (mapv store/find-event ids))

(defn f83-0
    [x]
    (vec (set [x])))

(defn f83-1
    [x]
    (vec (sort [x 1])))

(defn f83-2
    [x]
    (vec (distinct [x x])))
