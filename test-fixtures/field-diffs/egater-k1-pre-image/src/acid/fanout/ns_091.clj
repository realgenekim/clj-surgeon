(ns acid.fanout.ns-091
  (:require [acid.fanout.util-d :as u1]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]
            [acid.fanout.util-a :as u2]))

(defn local-91
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-91 "find-event")

(defn doc-91
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-91
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-091 was split

(defn site-a-91
    [id]
    (store/find-event id))

(defn site-b-91
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-91
    [ids]
    (mapv store/find-event ids))

(defn f91-0
    [x]
    (apply + [x 1]))

(defn f91-1
    [x]
    (max x 0))

(defn f91-2
    [x]
    (min x 10))
