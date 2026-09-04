(ns acid.fanout.ns-025
  (:require [acid.fanout.util-a :as store2]
            ;; the util aliases below are shared fleet-wide
            [acid.fanout.util-b :as st2]
            [acid.fanout.util-b :as u1]
            [acid.fanout.util-c :as u2]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]))

(defn local-25
    [id]
    (let [find-event (fn [z] {:id z :kind :local})]
      (find-event id)))

(def label-25 "find-event")

(defn doc-25
    "Superseded by fetch-event; the old name find-event stays in this docstring."
    [id]
    {:id id :kind :doc})

(defn other-25
    [id]
    (other/find-event id))

;; historical: find-event was defined here before ns-025 was split

(defn site-a-25
    [id]
    (store/find-event id))

(defn site-b-25
    [id]
    (let [ev (store/find-event id)]
      (assoc ev :seen true)))

(defn site-c-25
    [ids]
    (mapv store/find-event ids))

(defn f25-0
    [x]
    (let [y (inc x)] (* y y)))

(defn f25-1
    [x]
    (reduce + (range (max 0 x))))

(defn f25-2
    [x]
    (mapv inc (range 3)))
