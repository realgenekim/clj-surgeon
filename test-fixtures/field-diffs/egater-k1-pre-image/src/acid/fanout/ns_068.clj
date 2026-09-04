(ns acid.fanout.ns-068
  (:require [acid.fanout.store :as store]
            [acid.fanout.other :as other]
            [acid.fanout.util-a :as u1]))

(defn local-68
  [id]
  (let [find-event (fn [z] {:id z :kind :local})]
    (find-event id)))

(def label-68 "find-event")

(defn doc-68
  "Superseded by fetch-event; the old name find-event stays in this docstring."
  [id]
  {:id id :kind :doc})

(defn other-68
  [id]
  (other/find-event id))

;; historical: find-event was defined here before ns-068 was split

(defn site-a-68
  [id]
  (store/find-event id))

(defn site-b-68
  [id]
  (let [ev (store/find-event id)]
    (assoc ev :seen true)))

(defn site-c-68
  [ids]
  (mapv store/find-event ids))

(defn f68-0
  [x]
  (vec (seq [x])))

(defn f68-1
  [x]
  (vec (set [x])))

(defn f68-2
  [x]
  (vec (sort [x 1])))
