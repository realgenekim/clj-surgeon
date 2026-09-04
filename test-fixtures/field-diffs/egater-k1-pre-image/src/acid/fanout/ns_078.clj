(ns acid.fanout.ns-078
  "Namespace acid.fanout.ns-078. Historical note: find-event moved."
  (:require [acid.fanout.store :as store]
            [acid.fanout.other :as other]
            [acid.fanout.util-c :as u1]))

(defn local-78
  [id]
  (let [find-event (fn [z] {:id z :kind :local})]
    (find-event id)))

(def label-78 "find-event")

(defn doc-78
  "Superseded by fetch-event; the old name find-event stays in this docstring."
  [id]
  {:id id :kind :doc})

(defn other-78
  [id]
  (other/find-event id))

;; historical: find-event was defined here before ns-078 was split

(defn site-a-78
  [id]
  (store/find-event id))

(defn site-b-78
  [id]
  (let [ev (store/find-event id)]
    (assoc ev :seen true)))

(defn site-c-78
  [ids]
  (mapv store/find-event ids))

(defn f78-0
  [x]
  (count (str x)))

(defn f78-1
  [x]
  (first [x x]))

(defn f78-2
  [x]
  (second [x x]))
