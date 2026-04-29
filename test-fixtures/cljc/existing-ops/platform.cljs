(ns myapp.lib.platform
  "CLJS side of a dialect-split pair."
  (:require
   [clojure.string :as str]
   [goog.string :as gstr]))

(defn timestamp []
  (.now js/Date))

(defn temp-dir []
  "/tmp")

(defn hostname []
  (.-hostname js/location))
