(ns my.app.ui
  (:require
   [com.fulcrologic.fulcro.dom-server :as dom]
   [com.fulcrologic.fulcro.components :as comp]))

(defn render-page []
  (dom/div "hello"))
