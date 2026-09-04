(ns acid.fanout.ns-023
  (:require [acid.fanout.util-a :as store2]
            [acid.fanout.util-b :as st2]
            [acid.fanout.store :as st]
            [acid.fanout.other :as other]
            [acid.fanout.util-c :refer [es]]
            [acid.fanout.util-d :as u1]
            [acid.fanout.util-a :as u2]))

(defn local-23
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-23 "find-event")

(defn doc-23
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-23
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-023 was split

(defn site-a-23
    [id]
    (st/find-event id))

(defn site-b-23
    [id]
    (let [ev (st/find-event id)]
      (assoc ev :seen true)))

(defn site-c-23
    [ids]
    (mapv st/find-event ids))

(defn f23-0
    [x]
    (mapv inc (range 3)))

(defn f23-1
    [x]
    (some-> x inc str))

(defn f23-2
    [x]
    (cond-> x (pos? x) inc))
