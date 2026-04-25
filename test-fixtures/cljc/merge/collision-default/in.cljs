(ns my.app.platform)

(defn timestamp []
  (.now js/Date))
