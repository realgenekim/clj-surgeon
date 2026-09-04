(ns acid.fanout.ns-017
  (:require [acid.fanout.util-b :as u1]
            [acid.fanout.util-c :as u2]
            [acid.fanout.util-d :as u3]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]))

(defn local-17
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-17 "find-event")

(defn doc-17
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-17
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-017 was split

(defn site-a-17
    [id]
    (store/find-event id))

(defn site-b-17
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-17
    [ids]
    (mapv store/find-event ids))

(defn f17-0
    [x]
    (vec (distinct [x x])))

(defn f17-1
    [x]
    (into #{} [x]))

(defn f17-2
    [x]
    (inc x))
