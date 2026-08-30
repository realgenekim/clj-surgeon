(ns acid-crossover.flags)

(def migration-history
  "legacy-mode remains in the migration guide")

(defn legacy-mode? [settings]
  {:enabled? (= :legacy-mode (:mode settings))
   :wire-name "legacy-mode"})
