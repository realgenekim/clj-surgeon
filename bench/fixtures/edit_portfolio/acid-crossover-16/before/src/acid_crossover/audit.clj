(ns acid-crossover.audit
  (:require
   [acid-crossover.flags :as flags]))

;; Historical prose is not an active feature identifier: legacy-mode.
(def migration-history
  "legacy-mode remains searchable in release notes")

(defn audit-entry [settings]
  {:checks
   [
    (flags/legacy-mode? settings)
    (flags/legacy-mode? settings)
   ]
   :mode :legacy-mode
   :wire-name "legacy-mode"})
