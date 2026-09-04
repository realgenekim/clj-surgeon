(ns acid.fanout.ns-066
  "Namespace acid.fanout.ns-066. Historical note: find-event moved."
  (:require [acid.fanout.store :as s]
            [acid.fanout.other :as other]
            [acid.fanout.util-c :as u1]
            [acid.fanout.util-d :as u2]
            [acid.fanout.util-a :as u3]))

(defn local-66
  [id]
  (let [find-event (fn [z] {:id z :kind :local})]
    (find-event id)))

(def label-66 "find-event")

(defn doc-66
  "Superseded by fetch-event; the old name find-event stays in this docstring."
  [id]
  {:id id :kind :doc})

(defn other-66
  [id]
  (other/find-event id))

;; historical: find-event was defined here before ns-066 was split

(defn site-a-66
  [id]
  (s/find-event id))

(defn site-b-66
  [id]
  (let [ev (s/find-event id)]
    (assoc ev :seen true)))

(defn site-c-66
  [ids]
  (mapv s/find-event ids))

(defn f66-0
  [x]
  (assoc {} :x x))

(defn f66-1
  [x]
  (get {:a 1} :a x))

(defn f66-2
  [x]
  (vec (repeat 2 x)))
