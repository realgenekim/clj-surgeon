(ns acid.fanout.ns-043
  (:require [acid.fanout.util-d :as u1]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]))

(defn local-43
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-43 "find-event")

(defn doc-43
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-43
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-043 was split

(defn site-a-43
    [id]
    (store/find-event id))

(defn site-b-43
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-43
    [ids]
    (mapv store/find-event ids))

(defn f43-0
    [x]
    (first [x x]))

(defn f43-1
    [x]
    (second [x x]))

(defn f43-2
    [x]
    (last [x x]))
