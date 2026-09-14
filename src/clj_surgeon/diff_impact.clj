(ns clj-surgeon.diff-impact
  "Pure fixed-point test selection over require and file-content edges.
   The Git/filesystem/process boundary is test/diff_impact.clj."
  (:require
   [clj-surgeon.form-identity :as form-identity]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(defn dependencies [form]
  (into #{} (map (comp symbol :lib))
        (form-identity/source-require-entries
          (if (string? form) form (pr-str form))
          {:platform :clj :clauses #{":require" ":require-macros" ":use"}
           :on-unparsed #(throw (ex-info "Unsupported namespace dependency" {:form %}))})))

(defn reverse-closure
  "Fixed point over reverse edges. Each reachable node retains one shortest
  path to the seed; sorted neighbors make equally short witnesses deterministic.
  Visiting each node once terminates even for cycles, without a depth bound."
  [dependents seed]
  (loop [paths {seed [seed]} frontier [seed]]
    (if (empty? frontier)
      paths
      (let [[next-paths next-frontier]
            (reduce (fn [[seen pending] n]
                      (reduce (fn [[seen pending] dependent]
                                (if (contains? seen dependent)
                                  [seen pending]
                                  [(assoc seen dependent (into [dependent] (get seen n)))
                                   (conj pending dependent)]))
                              [seen pending] (sort (get dependents n))))
                    [paths []] frontier)]
        (recur next-paths next-frontier)))))

(defn impact [nodes changed]
  (let [dependents (reduce (fn [graph {:keys [namespace requires]}]
                             (reduce #(update %1 %2 (fnil conj #{}) namespace) graph requires))
                           {} nodes)
        closures (mapv #(reverse-closure dependents %) (sort changed))]
    (->> nodes
         (filter :test?)
         (keep (fn [{:keys [namespace] :as node}]
                 (let [paths (vec (sort (keep #(when-let [path (get % namespace)]
                                                 (mapv str (rest path))) closures)))]
                   (when (seq paths) (assoc node :paths paths)))))
         (sort-by #(vector (not= 'clj-surgeon.txn-journal-test (:namespace %))
                     (str (:namespace %)))) vec)))

;; @spec DIFF-IMPACT-001
;; @spec DIFF-IMPACT-002
(defn content-facts
  "Literal paths and scan vocabulary, without evaluating or resolving source.
   String contents are never parsed again; comments/regex/discards are ignored."
  [source]
  (let [syntax (tree-seq #(and (node/inner? %)
                            (not (#{:uneval :eval :regex} (node/tag %))))
                 node/children (parser/parse-string-all source))
        tokens (keep #(when (= :token (node/tag %)) (node/sexpr %)) syntax)]
    {:strings (set (filter string? tokens))
     :scan? (boolean
              (some #(and (symbol? %)
                          (re-find #"^(slurp|file-seq|glob|walk|.*scan.*|.*source-files|.*clojure-files)$"
                                   (name %))) tokens))}))

(defn repo-path [value]
  (try
    (when-not (str/blank? value)
      (let [raw (java.nio.file.Paths/get value (make-array String 0))
            path (when-not (.isAbsolute raw) (str (.normalize raw)))]
        (when (and path (re-find #"^(docs|resources|src)(/|$)" path)) path)))
    (catch Exception _ nil)))

;; @spec DIFF-IMPACT-001
;; @spec DIFF-IMPACT-002
(defn content-edges
  "Pure, bounded by the supplied existing repository file inventory. Directory
   prefixes in scanning namespaces deliberately overapproximate computed paths."
  [source existing-files]
  (let [{:keys [strings scan?]} (content-facts source)
        paths (set (keep repo-path strings))]
    (->> (for [path paths
               :let [source? (or (= path "src") (str/starts-with? path "src/"))]
               file (if (contains? existing-files path)
                      [path]
                      (when scan?
                        (filter #(str/starts-with? % (str path "/")) existing-files)))]
           {:file file :edge-kind (cond
                                    (not source?) :data-file
                                    (= path file) :source-text
                                    :else :source-scan)})
         distinct (sort-by (juxt :file :edge-kind)) vec)))

;; @spec DIFF-IMPACT-003
;; @spec DIFF-IMPACT-004
;; @spec DIFF-IMPACT-005
(defn select-impact
  "Join changed paths to namespace and content edges, then reuse the existing
   reverse fixed point. One shortest require path per seed; all seed reasons."
  [nodes changed-files]
  (let [changed-files (set changed-files)
        seeds (for [{:keys [namespace file content-edges]} nodes
                    reason (concat
                             (when (changed-files file)
                               [{:file file :edge-kind :require}])
                             (filter #(changed-files (:file %)) content-edges))]
                (assoc reason :seed namespace))
        selected (impact nodes (set (map :seed seeds)))
        selected (mapv (fn [{:keys [namespace paths] :as selected-node}]
                         (let [reachable (into #{(str namespace)} (map last paths))]
                           (-> selected-node
                               (dissoc :content-edges)
                               (assoc :reasons
                                      (vec (sort-by (juxt :file :edge-kind :seed)
                                             (filter #(reachable (str (:seed %))) seeds)))))))
                       selected)
        matched (set (map :file (mapcat :reasons selected)))
        seeded (set (map :file seeds))]
    {:status (if (seq selected) :selected :nothing-selected)
     :changed-files (vec (sort changed-files))
     :namespaces selected
     :edge-counts (merge {:require 0 :data-file 0 :source-text 0 :source-scan 0}
                    (frequencies
                      (concat (mapcat #(repeat (count (:requires %)) :require) nodes)
                              (map :edge-kind (mapcat :content-edges nodes)))))
     :selection-edge-counts (frequencies (map :edge-kind (mapcat :reasons selected)))
     :unmatched-files (mapv (fn [file]
                              {:file file :reason (if (seeded file)
                                                    :no-test-dependent :no-dependency-edge)})
                        (sort (remove matched changed-files)))}))
