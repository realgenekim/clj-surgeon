(ns acid.fanout.ns-058
  (:require [acid.fanout.util-c :as u1]
            [acid.fanout.util-d :as u2]
            [acid.fanout.util-a :as u3]
            [acid.fanout.store :as store]
            [acid.fanout.other :as other]))

(defn local-58
  [id]
  (let [find-event (fn [z] {:id z :kind :local})]
    (find-event id)))

(def label-58 "find-event")

(defn doc-58
  "Superseded by fetch-event; the old name find-event stays in this docstring."
  [id]
  {:id id :kind :doc})

(defn other-58
  [id]
  (other/find-event id))

;; historical: find-event was defined here before ns-058 was split

(defn site-a-58
  [id]
  (store/find-event id))

(defn site-b-58
  [id]
  (let [ev (store/find-event id)]
    (assoc ev :seen true)))

(defn site-c-58
  [ids]
  (mapv store/find-event ids))

(defn f58-0
  [x]
  (if (even? x) x (- x)))

(defn f58-1
  [x]
  (let [y (inc x)] (* y y)))

(defn f58-2
  [x]
  (reduce + (range (max 0 x))))
