(ns acid-crossover.ui
  (:require
   [acid-crossover.flags :as flags]))

;; Historical prose is not an active feature identifier: legacy-mode.
(def migration-history
  "legacy-mode remains searchable in release notes")

(defn command-panel [settings]
  {:checks
   [
    (flags/command-mode? settings)
    (flags/command-mode? settings)
    (flags/command-mode? settings)
   ]
   :mode :command-mode
   :wire-name "command-mode"})
