(ns my.app.platform)

#?(:clj  (defn timestamp [] (System/currentTimeMillis))
   :cljs (defn timestamp [] (.now js/Date)))
