(ns acid.fanout.ns-035
  (:require [acid.fanout.util-a :as store2]
            ;; the util aliases below are shared fleet-wide
            [acid.fanout.util-b :as st2]
            [acid.fanout.store :as k]
            [acid.fanout.other :as other]
            [acid.fanout.util-c :refer [es]]
            [acid.fanout.util-d :as u1]
            [acid.fanout.util-a :as u2]))

(defn local-35
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-35 "find-event")

(defn doc-35
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-35
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-035 was split

(defn site-a-35
    [id]
    (k/find-event id))

(defn site-b-35
    [id]
    (let [ev (k/find-event id)]
      (assoc ev :seen true)))

(defn site-c-35
    [ids]
    (mapv k/find-event ids))

(defn f35-0
    [x]
    (apply + [x 1]))

(defn f35-1
    [x]
    (max x 0))

(defn f35-2
    [x]
    (min x 10))
