(ns f1 (:require [x.events :as events]))
;; a standalone comment mentioning events/x
(def s "events/x ) ] } ; not code\n more events/y")
#"events/regex (" \newline
#_(def dead events/gone)
(comment (defn- inner [] events/live))
^{:doc events/meta} (def y #::events{:k events/v})
(defn use [] [events/a `(events/b ~events/c) ::events/kw :events/lit])
