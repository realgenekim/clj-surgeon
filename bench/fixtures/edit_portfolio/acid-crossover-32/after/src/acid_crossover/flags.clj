(ns acid-crossover.flags)

(def migration-history
  "legacy-mode remains in the migration guide")

(defn command-mode? [settings]
  {:enabled? (= :command-mode (:mode settings))
   :wire-name "command-mode"})
