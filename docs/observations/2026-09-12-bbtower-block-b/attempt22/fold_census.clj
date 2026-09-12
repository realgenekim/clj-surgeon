(ns fold-census
  (:require [babashka.fs :as fs]
            [clj-surgeon.lane-manifest :as lm]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def root "docs/observations/2026-09-12-bbtower-block-b/attempt22")
(doseq [entry (map edn/read-string (str/split-lines (slurp (str root "/controls/index.edn"))))
        row (:rows entry)]
  (let [raw (:receipt row)
        command-path (str/replace raw #"\.edn$" ".command.edn")
        command (edn/read-string (slurp command-path))]
    (spit (str/replace raw #"\.edn$" ".control.edn")
          (pr-str (merge row command {:raw-receipt raw})))))
(defn read-row [path]
  (when (fs/exists? path) (edn/read-string (slurp path))))
(defn paths [n]
  (into {} (for [[k suffix] [[:jvm "jvm-test"] [:bb "bb-test"] [:bb-load "bb-load"]]]
             [k (str root "/controls/" n "-" suffix
                     (if (= k :bb-load) ".edn" ".control.edn"))])))
(defn status [row]
  (if row
    (str (name (:status row))
         (when-let [r (:result row)]
           (str " (" (:test r) "/" (:fail r) "/" (:error r) ")")))
    "not run"))

(defn cell [row path]
  (if row
    (str "[" (status row) "](" (str/replace path (str root "/") "") ")")
    "not run"))

(let [rows (for [[n runtime] (sort-by key lm/namespace-runtimes)
                 :let [p (paths n)
                       jvm (read-row (:jvm p))
                       bb (read-row (:bb p))
                       load-row (read-row (:bb-load p))
                       classification (cond
                                        (= :load-failed (:status load-row)) :bb-load-incompatible
                                        (and (= :passed (:status jvm)) (= :passed (:status bb))
                                             (= 0 (:exit jvm) (:exit bb))) :portable
                                        (and jvm bb) :non-portable
                                        :else :incomplete)]]
             {:namespace n :runtime runtime :cadence (or (lm/lane-of n) :dedicated)
              :classification classification :paths p :jvm jvm :bb bb :bb-load load-row})
      inventory (into (sorted-map)
                      (map (fn [{:keys [namespace paths classification]}]
                             [namespace (assoc paths :classification classification)])) rows)]
  (spit (str root "/portability-controls.edn") (pr-str inventory))
  (spit (str root "/portability-census.md")
        (str "# Portability census\n\nGenerated " (java.time.Instant/now)
             ". Counts in cells are tests/failures/errors. All 159 assigned namespaces are listed.\n\n"
             "Commands and subjects are in controls/*.command.edn; each .edn has its adjacent .log. "
             "The configured bb runtime is load-probed first. A load incompatibility is explicitly "
             "accounted for, with no passing test or alternate-runtime claim. Every namespace that "
             "loads on bb receives a complete JVM and bb control, serially.\n\n"
             "A non-portable result refuses portability by namespace until repaired. No runtime "
             "assignment is changed to the passing side. Historical cost measurements are unchanged.\n\n"
             "| Namespace | Assignment | Cadence | JVM | bb | Status / account |\n"
             "|---|---|---|---|---|---|\n"
             (str/join "\n"
                       (for [{:keys [namespace runtime cadence classification jvm bb bb-load paths]} rows]
                         (str "| " namespace " | " runtime " | " cadence
                              " | " (cell jvm (:jvm paths))
                              " | " (cell (or bb bb-load) (if bb (:bb paths) (:bb-load paths)))
                              " | " (name classification)
                              (when (= :bb-load-incompatible classification)
                                (str ": " (str/replace (str/join " / " (:causes bb-load)) #"[\n|]" " ")))
                              " |")))
             "\n\nSummary: " (pr-str (frequencies (map :classification rows))) "\n"))
  (prn (frequencies (map :classification rows))))
