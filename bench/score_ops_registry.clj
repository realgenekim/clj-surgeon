#!/usr/bin/env bb

(ns score-ops-registry
  (:require
   [clojure.edn :as edn]
   [rewrite-clj.zip :as z]))

(defn- locations [root]
  (take-while (complement z/end?) (iterate z/next root)))

(defn- named-definition [source name]
  (->> (locations (z/of-string source))
       (filter z/list?)
       (filter #(= 'def (some-> % z/down z/sexpr)))
       (filter #(= name (some-> % z/down z/right z/sexpr)))
       first
       z/sexpr))

(defn ops-registry-facts [source]
  (let [definition (named-definition source 'ops-registry)
        map-form (first (filter #(and (seq? %)
                                      (= 'hash-map (first %)))
                                (tree-seq coll? seq definition)))
        entries (apply hash-map (rest map-form))
        specs (vals entries)]
    {:category-frequencies (frequencies (map :category specs))
     :required-arg-count (reduce + (map #(count (filter :required
                                                       (vals (:args %))))
                                        specs))
     :paired-ops (vec (sort (keep (fn [[op spec]]
                                    (when (:pair spec) op))
                                  entries)))}))

(defn self-test []
  (let [source (str "(ns score.fixture)\n"
                    "(def ops-registry (hash-map\n"
                    "  :read {:category :read :args {:file {:required true}}}\n"
                    "  :plan {:category :write :args {} :pair :plan!}\n"
                    "  :plan! {:category :write :args {:file {:required true}} :pair :plan}))\n")]
    (assert (= {:category-frequencies {:read 1 :write 2}
                :required-arg-count 2
                :paired-ops [:plan :plan!]}
               (ops-registry-facts source)))
    (println "ops-registry scorer self-test passed")))

(let [[source-path answer-path] *command-line-args*]
  (cond
    (= "--self-test" source-path) (self-test)
    (and source-path answer-path)
    (try
      (println (= (ops-registry-facts (slurp source-path))
                  (edn/read-string (slurp answer-path))))
      (catch Exception _
        (println false)))
    :else
    (do
      (binding [*out* *err*]
        (println "Usage: bb bench/score_ops_registry.clj SOURCE ANSWER"))
      (System/exit 2))))
