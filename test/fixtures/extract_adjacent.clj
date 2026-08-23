(ns fixtures.extract-adjacent
  (:require
   [clojure.string :as str]))

(defn stage-01 [x] (str/trim x))
(defn stage-02 [x] (stage-01 x))
(defn stage-03 [x] (stage-02 x))
(defn stage-04 [x] (stage-03 x))
(defn stage-05 [x] (stage-04 x))
(defn stage-06 [x] (stage-05 x))
(defn stage-07 [x] (stage-06 x))
(defn stage-08 [x] (stage-07 x))
(defn stage-09 [x] (stage-08 x))
(defn stage-10 [x] (stage-09 x))
(defn stage-11 [x] (stage-10 x))
(defn stage-12 [x] (stage-11 x))
(defn stage-13 [x] (stage-12 x))
(defn stage-14 [x] (stage-13 x))
(defn stage-15 [x] (stage-14 x))
;; This documented neighbor has no sacrificial blank line before it.
(defn event-resume-path [event]
  (:resume-path event))
