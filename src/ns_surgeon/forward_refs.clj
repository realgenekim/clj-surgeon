(ns ns-surgeon.forward-refs
  "Detect forward references using clj-kondo analysis."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [cheshire.core :as json]))

(defn- run-kondo [file]
  (let [result (shell/sh "clj-kondo" "--lint" file
                         "--config"
                         "{:output {:format :json} :analysis {:var-definitions true :var-usages true}}")]
    (when (str/blank? (:err result))
      (json/parse-string (:out result) true))))

(defn detect-forward-refs
  "Returns forward references: vars used before they're defined in the same namespace."
  [file ns-name]
  (when-let [data (run-kondo file)]
    (let [analysis (:analysis data)
          defs (into {}
                     (for [d (:var-definitions analysis)
                           :when (= (str ns-name) (str (:ns d)))]
                       [(str (:name d)) (:row d)]))
          usages (:var-usages analysis)
          ns-str (str ns-name)]
      (->> usages
           (filter (fn [u]
                     (and (= ns-str (str (:from u)))
                          (= ns-str (str (:to u)))
                          (let [def-line (get defs (str (:name u)))]
                            (and def-line (< (:row u) def-line))))))
           (map (fn [u]
                  {:name (symbol (:name u))
                   :used-at (:row u)
                   :defined-at (get defs (str (:name u)))
                   :gap (- (get defs (str (:name u))) (:row u))}))
           ;; Deduplicate: one entry per forward-ref'd var (largest gap)
           (group-by :name)
           (map (fn [[_ vs]] (apply max-key :gap vs)))
           (sort-by :gap >)
           vec))))
