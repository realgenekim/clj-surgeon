(ns acid.fanout.ns-027
  "Namespace acid.fanout.ns-027. Historical note: find-event moved."
  (:require [acid.fanout.store :as store]
            [acid.fanout.other :as other]
            [acid.fanout.util-d :as u1]
            [acid.fanout.util-a :as u2]
            [acid.fanout.util-b :as u3]))

(defn local-27
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-27 "find-event")

(defn doc-27
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-27
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-027 was split

(defn site-a-27
    [id]
    (store/find-event id))

(defn site-b-27
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-27
    [ids]
    (mapv store/find-event ids))

(defn f27-0
    [x]
    (mapv inc (range 3)))

(defn f27-1
    [x]
    (some-> x inc str))

(defn f27-2
    [x]
    (cond-> x (pos? x) inc))
