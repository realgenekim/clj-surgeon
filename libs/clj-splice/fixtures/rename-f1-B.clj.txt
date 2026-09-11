(ns f1 (:require [x.events :as evq]))
;; a standalone comment mentioning events/x
(def s "events/x ) ] } ; not code\n more events/y")
#"events/regex (" \newline
#_(def dead events/gone)
(comment (defn- inner [] evq/live))
^{:doc evq/meta} (def y #::evq{:k evq/v})
(defn use [] [evq/a `(evq/b ~evq/c) ::evq/kw :events/lit])
