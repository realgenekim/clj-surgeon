(ns my.app.button
  (:require
   #?@(:clj  [[com.fulcrologic.fulcro.dom-server :as dom]]
       :cljs [[com.fulcrologic.fulcro.dom :as dom]
              ["react-bootstrap" :refer [Button]]])))

(defn render []
  (dom/div "btn"))
