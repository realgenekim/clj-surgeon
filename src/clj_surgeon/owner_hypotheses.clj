(ns clj-surgeon.owner-hypotheses
  "Bounded owner-name hypotheses for selector refusals.

  Ranking is model-facing evidence only. It never selects an owner or creates
  executable authority."
  (:require
   [clojure.string :as str]))

(def ^:private candidate-limit 10)

(defn- normalized-name
  [value]
  (str/lower-case (str value)))

(defn- levenshtein-distance
  [left right]
  (loop [index 0
         left (seq left)
         previous (vec (range (inc (count right))))]
    (if-let [left-char (first left)]
      (recur
        (inc index)
        (next left)
        (reduce
          (fn [row [right-index right-char]]
            (conj row
                  (min (inc (peek row))
                       (inc (nth previous (inc right-index)))
                       (+ (nth previous right-index)
                          (if (= left-char right-char) 0 1)))))
          [(inc index)]
          (map-indexed vector right)))
      (peek previous))))

(defn normalized-levenshtein-score
  "Return normalized character similarity in [0, 1]."
  [requested candidate]
  (let [requested (normalized-name requested)
        candidate (normalized-name candidate)
        width (max (count requested) (count candidate))]
    (if (zero? width)
      1.0
      (- 1.0 (/ (double (levenshtein-distance requested candidate)) width)))))

(defn- candidate-evidence
  [{:keys [type platforms line end-line] :as record} rank]
  (cond-> {:owner (str (:name record))
           :rank rank
           :ranking-basis :normalized-levenshtein
           :authority false}
    type (assoc :type (str type))
    (seq platforms) (assoc :platforms (mapv clojure.core/name platforms))
    line (assoc :line line)
    end-line (assoc :end-line end-line)))

;; @spec MCP-OP-READ-HYP-001 MCP-OP-READ-HYP-002
(defn rank-owner-hypotheses
  "Rank up to ten real named owners for one missing owner.

  `resolved-names` removes successful sibling requests before ranking. An
  optional `platform` restricts the complete candidate universe first."
  [requested records {:keys [platform resolved-names]}]
  (let [resolved (set (map str resolved-names))
        available (->> records
                       (filter :name)
                       (filter #(or (nil? platform)
                                    (some #{platform} (:platforms %))))
                       (reduce (fn [{:keys [seen] :as result} record]
                                 (let [owner (str (:name record))]
                                   (if (contains? seen owner)
                                     result
                                     (-> result
                                         (update :seen conj owner)
                                         (update :records conj record)))))
                               {:seen #{} :records []})
                       :records)
        candidates (->> available
                        (remove #(contains? resolved (str (:name %))))
                        vec)
        ranked (->> candidates
                    (map #(assoc % ::score
                                 (normalized-levenshtein-score
                                   requested (:name %))))
                    (sort-by (juxt (comp - ::score) (comp str :name)
                                   (comp #(or % Long/MAX_VALUE) :line)))
                    (take candidate-limit)
                    vec)]
    {:requested-owner (str requested)
     :ranking-basis :normalized-levenshtein
     :authority false
     :available-owner-count (count available)
     :excluded-resolved-owner-count (- (count available) (count candidates))
     :candidate-count (count candidates)
     :candidates-returned (count ranked)
     :candidates-truncated (> (count candidates) candidate-limit)
     :did-you-mean
     (mapv (fn [rank candidate]
             (candidate-evidence candidate rank))
           (range 1 (inc (count ranked)))
           ranked)}))
