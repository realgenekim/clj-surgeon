(ns acid.fanout.ns-039
  "Namespace acid.fanout.ns-039. Historical note: find-event moved."
  (:require [acid.fanout.util-d :as u1]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]))

(defn local-39
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-39 "find-event")

(defn doc-39
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-39
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-039 was split

(defn site-a-39
    [id]
    (store/find-event id))

(defn site-b-39
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-39
    [ids]
    (mapv store/find-event ids))

(defn f39-0
    [x]
    (keyword (str "k" x)))

(defn f39-1
    [x]
    (assoc {} :x x))

(defn f39-2
    [x]
    (get {:a 1} :a x))
