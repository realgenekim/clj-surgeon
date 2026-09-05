#!/usr/bin/env bb
(ns suite-report
  "Renders the round-one spike measurements into the markdown tables that go
   into docs/observations/2026-09-04-suite-spike-round1.md.

   Inputs: the EDN written by dev/experiments/suite_timing.clj (runtime) and
   by dev/experiments/suite_classify.clj (static). Output: markdown on stdout.

   Usage: bb dev/experiments/suite_report.clj timing.edn classify.edn"
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]))

(defn- pct [n d] (if (zero? d) 0.0 (* 100.0 (/ (double n) d))))

(defn timing-table
  [{:keys [rows total-wall-ms]} classify-by-ns limit]
  (let [sorted (sort-by :wall-ms > rows)]
    (str
      "| # | namespace | wall s | % | cum % | tests | asserts | procs | tmp dirs | static tags |\n"
      "|---|---|---:|---:|---:|---:|---:|---:|---:|---|\n"
      (str/join
        "\n"
        (:lines
          (reduce
            (fn [{:keys [cum i lines]} r]
              (let [cum (+ cum (:wall-ms r))
                    tags (->> (get classify-by-ns (str (:ns r)))
                              :categories
                              (map #(str/replace (name %) #"^(spawns|temp|binds|shared|global|sleeps).*"
                                                 (fn [[m _]] m)))
                              distinct
                              (str/join " "))]
                {:cum cum
                 :i (inc i)
                 :lines (conj lines
                              (format "| %d | `%s` | %.1f | %.1f%% | %.1f%% | %d | %d | %d | %d | %s |"
                                      (inc i) (str (:ns r)) (/ (:wall-ms r) 1000.0)
                                      (pct (:wall-ms r) total-wall-ms)
                                      (pct cum total-wall-ms)
                                      (:tests r) (:assertions r)
                                      (count (:subprocesses r))
                                      (count (:tmp-created r))
                                      (if (str/blank? tags) "pure" tags)))}))
            {:cum 0 :i 0 :lines []}
            (take limit sorted)))))))

(defn -main [& [timing-path classify-path limit]]
  (let [timing (edn/read-string (slurp timing-path))
        classify (edn/read-string (slurp classify-path))
        by-ns (into {} (map (juxt :ns identity) (:rows classify)))]
    (println (timing-table timing by-ns (parse-long (or limit "20"))))
    (println)
    (println (format "Total wall: %.1f s over %d namespaces."
                     (/ (:total-wall-ms timing) 1000.0) (:namespace-count timing)))))

(when (= *file* (System/getProperty "babashka.file")) (apply -main *command-line-args*))
