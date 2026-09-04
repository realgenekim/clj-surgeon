(ns acid.fanout.ns-029
  (:require [acid.fanout.util-b :as u1]
            [acid.fanout.util-c :as u2]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]))

(defn local-29
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-29 "find-event")

(defn doc-29
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-29
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-029 was split

(defn site-a-29
    [id]
    (store/find-event id))

(defn site-b-29
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-29
    [ids]
    (mapv store/find-event ids))

(defn f29-0
    [x]
    (get {:a 1} :a x))

(defn f29-1
    [x]
    (vec (repeat 2 x)))

(defn f29-2
    [x]
    (apply + [x 1]))
