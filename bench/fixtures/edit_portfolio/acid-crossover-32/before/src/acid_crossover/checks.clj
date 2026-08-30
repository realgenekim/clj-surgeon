(ns acid-crossover.checks
  (:require
   [acid-crossover.flags :as flags]))

;; Historical prose is not an active feature identifier: legacy-mode.
(def migration-history
  "legacy-mode remains searchable in release notes")

(defn acceptance-check [settings]
  {:checks
   [
    (flags/legacy-mode? settings)
    (flags/legacy-mode? settings)
    (flags/legacy-mode? settings)
    (flags/legacy-mode? settings)
    (flags/legacy-mode? settings)
    (flags/legacy-mode? settings)
   ]
   :mode :legacy-mode
   :wire-name "legacy-mode"})
