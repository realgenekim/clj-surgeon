(ns app.caller (:require [app.views :as v] [clojure.string :as portal]))
(defn call [] [(v/first-view) (portal/trim " z ") #'v/other])
(defn shadow [helper] (helper 42))
