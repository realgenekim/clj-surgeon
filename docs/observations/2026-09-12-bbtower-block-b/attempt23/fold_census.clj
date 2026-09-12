(ns fold-census
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.lane-manifest :as lm]
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def root "docs/observations/2026-09-12-bbtower-block-b/attempt23")
(def rows
  (mapv (fn [[n paths]]
          (let [controls (into {} (for [[runtime path] paths :when (fs/exists? path)]
                                    [runtime (assoc (edn/read-string (slurp path)) :receipt path)]))
                refusal (lm/portability-refusal n controls)
                state (if (= :bb-load-incompatible (:reason refusal))
                        {:state :bb-load-excluded :detail (get-in controls [:bb-load :message])}
                        (lm/portability-state n controls (lm/bb-ineligibilities n)))]
            (merge state {:namespace n :assignment (lm/namespace-runtimes n)
                          :paths paths :controls controls})))
        lm/namespace-runtime-controls))
(def summary (merge {:portable 0 :bb-ineligible 0 :refused 0 :bb-load-excluded 0}
                    (frequencies (map :state rows))))
(spit (str root "/census.edn") (pr-str {:generated (str (java.time.Instant/now))
                                        :summary summary :rows rows}))
(spit (str root "/portability-census.md")
      (str "# Census fold\n\nFrozen attempt22 controls plus fresh attempt23 controls for merge, split and prune.\n"
           "Initial-load exclusions are separately accounted; they have no complete JVM controls in this census.\n\n"
           "| Namespace | Assignment | State | Reasons |\n|---|---|---|---|\n"
           (str/join "\n" (for [{:keys [namespace assignment state reasons detail]} rows]
                            (str "| " namespace " | " assignment " | " state " | "
                                 (str/replace (str (pr-str reasons) " " detail) #"[\n|]" " ") " |")))
           "\n\n" (pr-str summary) "\n"))
(prn summary)
(doseq [row rows :when (#{:bb-ineligible :refused} (:state row))]
  (prn (dissoc row :paths :controls)))
