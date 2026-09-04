(ns acid.fanout.ns-026
  (:require [acid.fanout.util-c :as u1]
            [acid.fanout.util-d :as u2]
            [acid.fanout.store :as repo]
            [acid.fanout.other :as other]))

(defn local-26
  [id]
  (let [find-event (fn [z] {:id z :kind :local})]
    (find-event id)))

(def label-26 "find-event")

(defn doc-26
  "Superseded by fetch-event; the old name find-event stays in this docstring."
  [id]
  {:id id :kind :doc})

(defn other-26
  [id]
  (other/find-event id))

;; historical: find-event was defined here before ns-026 was split

(defn site-a-26
  [id]
  (repo/find-event id))

(defn site-b-26
  [id]
  (let [ev (repo/find-event id)]
    (assoc ev :seen true)))

(defn site-c-26
  [ids]
  (mapv repo/find-event ids))

(defn f26-0
  [x]
  (dec x))

(defn f26-1
  [x]
  (* x 2))

(defn f26-2
  [x]
  (str x "-a"))
