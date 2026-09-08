(ns app.views (:require [clojure.string :as portal]))
(declare later)
;; helper comment travels.
(defn- helper [x] (portal/trim x))
(defn first-view [] (later))
(defn later [] (helper " x "))
(defn other [] (helper " y "))
