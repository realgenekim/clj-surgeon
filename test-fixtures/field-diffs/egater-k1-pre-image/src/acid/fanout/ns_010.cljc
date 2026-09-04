(ns acid.fanout.ns-010
  (:require [acid.fanout.store :as store]
            ;; the util aliases below are shared fleet-wide
            [acid.fanout.other :as other]
            [acid.fanout.util-c :as u1]
            [acid.fanout.util-d :as u2]
            [acid.fanout.util-a :as u3]))

(defn local-10
  [id]
  (let [find-event (fn [z] {:id z :kind :local})]
    (find-event id)))

(def label-10 "find-event")

(defn doc-10
  "Superseded by fetch-event; the old name find-event stays in this docstring."
  [id]
  {:id id :kind :doc})

(defn other-10
  [id]
  (other/find-event id))

;; historical: find-event was defined here before ns-010 was split

(defn platform-10
  [x]
  #?(:clj (str "jvm-" x "-find-event")
     :cljs (str "js-" x "-find-event")))

(defn site-a-10
  [id]
  (store/find-event id))

(defn site-b-10
  [id]
  (let [ev (store/find-event id)]
    (assoc ev :seen true)))

(defn site-c-10
  [ids]
  (mapv store/find-event ids))

(defn f10-0
  [x]
  (dec x))

(defn f10-1
  [x]
  (* x 2))

(defn f10-2
  [x]
  (str x "-a"))
