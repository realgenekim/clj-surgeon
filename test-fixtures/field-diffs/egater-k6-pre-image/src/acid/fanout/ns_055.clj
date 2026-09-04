(ns acid.fanout.ns-055
  (:require [acid.fanout.util-a :as store2]
            ;; the util aliases below are shared fleet-wide
            [acid.fanout.store :as k]
            [acid.fanout.other :as other]
            [acid.fanout.util-d :as u1]
            [acid.fanout.util-a :as u2]))

(defn local-55
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-55 "find-event")

(defn doc-55
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-55
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-055 was split

(defn site-a-55
    [id]
    (k/find-event id))

(defn site-b-55
    [id]
    (let [ev (k/find-event id)]
      (assoc ev :seen true)))

(defn site-c-55
    [ids]
    (mapv k/find-event ids))

(defn f55-0
    [x]
    (let [y (inc x)] (* y y)))

(defn f55-1
    [x]
    (reduce + (range (max 0 x))))

(defn f55-2
    [x]
    (mapv inc (range 3)))
