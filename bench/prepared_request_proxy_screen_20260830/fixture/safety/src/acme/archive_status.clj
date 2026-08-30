(ns acme.archive-status)

(def archive-root "archives")

(defn archive-ready? [entry]
  (and (= :complete (:state entry))
       (string? (:sha256 entry))))

(defn status-summary [entries]
  {:ready (count (filter archive-ready? entries))
   :root archive-root})
