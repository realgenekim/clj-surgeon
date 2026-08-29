#!/usr/bin/env bb

(ns edit-portfolio-run-score
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.set :as set]
   [clojure.string :as str]))

(def source-extensions
  #{"bb" "clj" "cljc" "cljs" "edn"})

(defn source-path?
  [path]
  (let [relative (str path)]
    (and (contains? source-extensions (fs/extension relative))
         (not-any? #(str/starts-with? % ".")
                   (str/split relative #"/")))))

(defn relative-source-paths
  [root]
  (->> (fs/glob root "**")
       (filter fs/regular-file?)
       (map #(str (fs/relativize root %)))
       (filter source-path?)
       sort
       vec))

(defn compare-source-paths
  [expected actual]
  (let [expected-set (set expected)
        actual-set (set actual)
        unexpected (sort (set/difference actual-set expected-set))
        missing (sort (set/difference expected-set actual-set))]
    {:source-set-exact (boolean (and (empty? unexpected) (empty? missing)))
     :expected (vec (sort expected-set))
     :actual (vec (sort actual-set))
     :unexpected (vec unexpected)
     :missing (vec missing)}))

(defn compare-source-inventory
  [expected-root actual-root]
  (compare-source-paths (relative-source-paths expected-root)
                        (relative-source-paths actual-root)))

(def compact-route-contract
  {:mcp-calls 1
   :inspect-calls 0
   :edit-calls 1
   :extraction-calls 0
   :plan-calls 0
   :transform-calls 0
   :file-changes 0
   :shell-calls 0
   :mcp-successes 1
   :mcp-failures 0
   :failed-mutation-actions 0
   :post-decision-source-commands 0
   :verified true
   :single-change-transaction true})

(defn compact-route-adherent?
  [metrics]
  (= compact-route-contract
     (select-keys metrics (keys compact-route-contract))))

(defn finalize-outcome
  [{:keys [target-semantic-correct target-exact-correct route-adherent
           source-set-exact treatment-adherent]}]
  (let [semantic-correct (boolean (and target-semantic-correct source-set-exact))
        exact-correct (boolean (and target-exact-correct source-set-exact))
        route-adherent (boolean route-adherent)]
    {:semantic-correct semantic-correct
     :exact-correct exact-correct
     :route-adherent route-adherent
     :source-set-exact (boolean source-set-exact)
     :correct (boolean (and semantic-correct route-adherent treatment-adherent))}))

(defn read-edn-file
  [path]
  (edn/read-string (slurp path)))

(defn -main
  [& [operation argument second-argument]]
  (case operation
    "--source-inventory"
    (prn (compare-source-inventory argument second-argument))

    "--compact-route"
    (println (compact-route-adherent? (read-edn-file argument)))

    "--outcome-tsv"
    (let [{:keys [semantic-correct exact-correct route-adherent
                  source-set-exact correct]}
          (finalize-outcome (read-edn-file argument))]
      (println (str/join "\t"
                         [semantic-correct exact-correct
                          route-adherent source-set-exact correct])))

    (throw
      (ex-info
        (str "Usage: edit_portfolio_run_score.clj "
             "--source-inventory EXPECTED_ROOT ACTUAL_ROOT | "
             "--compact-route METRICS_EDN | --outcome-tsv OUTCOME_EDN")
        {:operation operation}))))

(when (some? *command-line-args*)
  (apply -main *command-line-args*))
