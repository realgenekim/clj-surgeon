(ns my.app.ui
  (:require
   [com.fulcrologic.fulcro.components :as comp]
   #?@(:clj  [[com.fulcrologic.fulcro.dom-server :as dom]]
       :cljs [[com.fulcrologic.fulcro.dom :as dom]])))

(defn render-page []
  (dom/div "hello"))
