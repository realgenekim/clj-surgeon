(ns acid.fanout.ns-003
  "Namespace acid.fanout.ns-003. Historical note: find-event moved."
  (:require [acid.fanout.store :as st]
            [acid.fanout.other :as other]
            [acid.fanout.util-a :as store2]
            [acid.fanout.util-d :as u1]))

(defn local-3
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-3 "find-event")

(defn doc-3
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-3
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-003 was split

(defn site-a-3
    [id]
    (st/find-event id))

(defn site-b-3
    [id]
    (let [ev (st/find-event id)]
      (assoc ev :seen true)))

(defn site-c-3
    [ids]
    (mapv st/find-event ids))

(defn f3-0
    [x]
    (cond-> x (pos? x) inc))

(defn f3-1
    [x]
    (->> (range (max 0 x)) (filter even?) (into [])))

(defn f3-2
    [x]
    (keyword (str "k" x)))
