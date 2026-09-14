(ns clj-surgeon.portability-summary
  "Population text shared by the portability inventory generator.")

;; @spec STATE-HOME-013
(defn population-line [inventory]
  (str "All " (count inventory) " assigned namespaces are listed."))
