(ns acid.fanout.ns-013
  (:require [acid.fanout.util-a :as store2]
            [acid.fanout.util-b :as st2]
            [acid.fanout.util-c :refer [es]]
            [acid.fanout.util-b :as u1]
            [acid.fanout.store :as st]
            [acid.fanout.other :as other]))

(defn local-13
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-13 "find-event")

(defn doc-13
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-13
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-013 was split

(defn site-a-13
    [id]
    (st/find-event id))

(defn site-b-13
    [id]
    (let [ev (st/find-event id)]
      (assoc ev :seen true)))

(defn site-c-13
    [ids]
    (mapv st/find-event ids))

(defn f13-0
    [x]
    (when (pos? x) x))

(defn f13-1
    [x]
    (if (even? x) x (- x)))

(defn f13-2
    [x]
    (let [y (inc x)] (* y y)))
