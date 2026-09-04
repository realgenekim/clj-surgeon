(ns cfp-scheduler-killer.helpers-only
  "A projection helper file that defines no fold arms. Used to witness the
   :no-fold-arms-found refusal against real, parseable bytes."
  (:require
   [clojure.string :as str]))

(defn- conj-once
  [coll x]
  (let [coll (vec (or coll []))]
    (if (some #(= x %) coll) coll (conj coll x))))

(defn normalized-name
  [name]
  (-> (or name "") str str/trim str/lower-case (str/replace #"\s+" " ")))

(defn record-window
  [state person-id window]
  (update-in state [:speakers person-id :windows] conj-once window))
