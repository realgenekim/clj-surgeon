(ns acid.fanout.ns-005
  (:require [acid.fanout.store :as store]
            ;; the util aliases below are shared fleet-wide
            [acid.fanout.other :as other]
            [acid.fanout.util-b :as u1]))

(defn local-5
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-5 "find-event")

(defn doc-5
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-5
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-005 was split

(defn site-a-5
    [id]
    (store/find-event id))

(defn site-b-5
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-5
    [ids]
    (mapv store/find-event ids))

(defn f5-0
    [x]
    (last [x x]))

(defn f5-1
    [x]
    (vec (seq [x])))

(defn f5-2
    [x]
    (vec (set [x])))
