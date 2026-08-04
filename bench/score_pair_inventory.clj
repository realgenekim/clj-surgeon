#!/usr/bin/env bb

(ns score-pair-inventory
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]))

(defn normalize [source]
  (-> source
      (str/replace #"\s+" " ")
      str/trim))

(defn expected-value [path]
  (let [result (edn/read-string (slurp path))]
    {:pairs (->> (:results result)
                 (filter #(= 2 (:count %)))
                 (mapv (fn [{:keys [forms]}]
                         {:left-source (first forms)
                          :right-source (second forms)})))
     :tail-source (some (fn [{:keys [count forms]}]
                          (when (= 1 count)
                            (first forms)))
                        (:results result))}))

(defn submitted-value [path]
  (let [value (edn/read-string (slurp path))]
    (select-keys value [:pairs :tail-source])))

(defn transform-value [f value]
  (-> value
      (update :pairs
              (fn [pairs]
                (mapv (fn [pair]
                        (-> pair
                            (update :left-source f)
                            (update :right-source f)))
                      pairs)))
      (update :tail-source #(some-> % f))))

(defn correct? [_shape expected-path answer-path mode]
  (let [transform (if (= mode "normalized") normalize identity)]
    (= (transform-value transform (expected-value expected-path))
       (transform-value transform (submitted-value answer-path)))))

(let [[shape expected-path answer-path mode] *command-line-args*]
  (when-not (and (#{"case" "cond" "binding"} shape)
                 expected-path
                 answer-path
                 (#{"exact" "normalized"} mode))
    (binding [*out* *err*]
      (println "Usage: score_pair_inventory.clj case|cond EXPECTED.edn ANSWER.txt exact|normalized"))
    (System/exit 2))
  (println (boolean (correct? shape expected-path answer-path mode))))
