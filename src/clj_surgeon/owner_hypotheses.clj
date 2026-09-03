(ns clj-surgeon.owner-hypotheses
  "Bounded owner-name hypotheses for selector refusals.

  Ranking is model-facing evidence only. It never selects an owner or creates
  executable authority."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]))

(def ^:private candidate-limit 10)
(def ^:private dispatch-vocabulary-limit 40)

(def dispatch-vocabulary-character-limit
  "Published character budget for one bounded dispatch vocabulary.

  A count bound alone is not a bound: forty long dispatch spellings are still
  kilobytes of refusal evidence."
  2048)
(def ^:private available-owner-character-limit 32768)

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

(defn- unique-records
  [records]
  (->> records
       (filter :name)
       (reduce (fn [{:keys [seen] :as result} record]
                 (let [owner (str (:name record))]
                   (if (contains? seen owner)
                     result
                     (-> result
                         (update :seen conj owner)
                         (update :records conj record)))))
               {:seen #{} :records []})
       :records))

;; @spec MCP-OP-READ-HYP-001
;; @spec MCP-OP-READ-HYP-002
(defn rank-owner-hypotheses
  "Rank up to ten real named owners for one missing owner.

  `resolved-names` removes successful sibling requests before ranking. An
  optional `platform` restricts the complete candidate universe first."
  [requested records {:keys [platform resolved-names]}]
  (let [resolved (set (map str resolved-names))
        available (->> (unique-records records)
                       (filter #(or (nil? platform)
                                    (some #{platform} (:platforms %))))
                       vec)
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

(defn- bounded-owner-names
  [owner-names]
  (loop [remaining owner-names
         returned []
         encoded-characters 2]
    (if-let [owner (first remaining)]
      (let [separator-characters (if (seq returned) 1 0)
            next-count (+ encoded-characters separator-characters
                          (count (json/generate-string owner)))]
        (if (<= next-count available-owner-character-limit)
          (recur (next remaining) (conj returned owner) next-count)
          {:owners returned
           :returned (count returned)
           :omitted (count remaining)
           :truncated true}))
      {:owners returned
       :returned (count returned)
       :omitted 0
       :truncated false})))

(declare presentation-node)

(defn- presentation-children
  "Collapse every comment and whitespace run between two children to one space."
  [children]
  (let [marked (mapv #(if (n/whitespace-or-comment? %) ::gap (presentation-node %))
                     children)
        collapsed (mapcat #(if (= ::gap (first %)) [::gap] %)
                          (partition-by #(= ::gap %) marked))
        trimmed (->> collapsed
                     (drop-while #(= ::gap %))
                     reverse
                     (drop-while #(= ::gap %))
                     reverse)]
    (mapv #(if (= ::gap %) (n/spaces 1) %) trimmed)))

(defn- unwrapped-pr-str
  "Reader-safe escaped text for `value`, without the wrapping quotes.

  `pr-str` on a string escapes every embedded newline as the two characters
  `\\n`; stripping the outer quotes leaves text `n/string-node` can wrap back
  into a normal single-line (`:token`) string node."
  [value]
  (apply str (-> value pr-str next butlast)))

(defn- presentation-node
  [node]
  (cond
    (n/inner? node)
    (n/replace-children node (presentation-children (n/children node)))

    ;; A string node whose source spans more than one physical line (a raw
    ;; newline typed inside the quotes, not an escaped `\n`) carries that
    ;; newline as literal `lines` content; `n/string` on it reproduces the
    ;; newline verbatim. Re-escape it through `pr-str` so the presented form
    ;; collapses to one physical line and still `read-string`s to the same
    ;; value.
    (= :multi-line (n/tag node))
    (n/string-node (unwrapped-pr-str (n/sexpr node)))

    :else
    node))

;; @spec MCP-OP-DISPATCH-004
(defn- presented-dispatch
  "One dispatch spelling rendered as a single comment-free line.

  The selector compares parsed dispatch values, not bytes, so dropping a
  comment, collapsing a line break, and reader-escaping an embedded raw
  newline all leave a spelling the selector still accepts — while a `;;` can
  no longer comment out the rest of a joined summary line, and no entry can
  smuggle a newline into bounded evidence."
  [spelling]
  (let [text (str spelling)]
    (try
      (n/string (presentation-node (parser/parse-string text)))
      (catch Exception _ text))))

;; @spec MCP-OP-DISPATCH-004
(defn- bounded-dispatch-vocabulary
  "Return dispatch spellings inside both the count and the character budget."
  [dispatches]
  (loop [remaining dispatches
         returned []
         encoded-characters 2]
    (if-let [dispatch (first remaining)]
      (let [separator-characters (if (seq returned) 1 0)
            next-count (+ encoded-characters separator-characters
                          (count (json/generate-string dispatch)))]
        (if (and (<= next-count dispatch-vocabulary-character-limit)
                 (< (count returned) dispatch-vocabulary-limit))
          (recur (next remaining) (conj returned dispatch) next-count)
          {:vocabulary returned
           :returned (count returned)
           :omitted (count remaining)
           :truncated true}))
      {:vocabulary returned
       :returned (count returned)
       :omitted 0
       :truncated false})))

;; @spec MCP-OP-DISPATCH-002
;; @spec MCP-OP-DISPATCH-004
(defn defmethod-owner-evidence
  "Bounded multimethod addressing evidence for one unresolved owner selector.

  `requested` is the caller's exact selector text; its leading whitespace-free
  token is the owner name and any remainder is the dispatch spelling the caller
  attempted. Returns nil unless that name owns at least one `defmethod` record
  in the frozen snapshot. The evidence is model-facing only: it never selects an
  owner and never grants write authority."
  [requested records]
  (let [text (str/trim (str requested))
        [_ owner-name remainder] (re-matches #"(?s)(\S+)(?:\s+(.*))?" text)
        arms (when owner-name
               (->> records
                    (filter #(and (= 'defmethod (:type %))
                                  (= owner-name (str (:name %)))))
                    vec))]
    (when (seq arms)
      (let [dispatches (->> arms
                            (keep :dispatch)
                            (map presented-dispatch)
                            distinct
                            vec)
            attempted (some-> remainder str/trim not-empty presented-dispatch)
            matched (some #{attempted} dispatches)
            example (or matched (first dispatches))
            bounded (bounded-dispatch-vocabulary dispatches)]
        (cond-> {:owner-kind "defmethod"
                 :name owner-name
                 :arm-count (count arms)
                 :owner-form (cond-> {:kind "defmethod" :name owner-name}
                               example (assoc :dispatch example))
                 :owner-form-is-exact (boolean matched)
                 :accepted-by "apply_clojure_changes changes[].forms"
                 :dispatch-vocabulary (:vocabulary bounded)
                 :dispatch-count (count dispatches)
                 :dispatch-vocabulary-returned (:returned bounded)
                 :dispatch-vocabulary-omitted (:omitted bounded)
                 :dispatch-vocabulary-truncated (:truncated bounded)
                 :authority false
                 :next-action "send_defmethod_owner_form"}
          attempted (assoc :attempted-dispatch attempted))))))

(defn- selection-failure
  [failure available-records resolved-names all-records]
  (let [requested (str (:form failure))
        missing? (= :form-not-found (:error-type failure))
        ranking (when missing?
                  (rank-owner-hypotheses requested available-records
                                         {:resolved-names resolved-names}))
        hypotheses (or (:did-you-mean ranking) [])
        candidate-count (or (:candidate-count ranking) 0)
        returned (count hypotheses)
        defmethod-owner (defmethod-owner-evidence requested all-records)]
    (cond-> {:requested-owner requested
             :failure-kind (:error-type failure)
             :match-count (:match-count failure)
             :hypotheses hypotheses
             :hypotheses-returned returned
             :hypotheses-omitted (- candidate-count returned)
             :hypotheses-truncated (boolean (:candidates-truncated ranking))}
      (:matches failure) (assoc :matches (:matches failure))
      defmethod-owner (assoc :defmethod-owner defmethod-owner))))

;; @spec MCP-OP-READ-PARITY-001
(defn owner-recovery-evidence
  "Compile source-free recovery evidence for one exact-owner refusal."
  [requested records failures]
  (let [available-records (unique-records records)
        available-names (mapv (comp str :name) available-records)
        bounded-names (bounded-owner-names available-names)
        failed-names (set (map (comp str :form) failures))
        resolved-names (remove failed-names (map str requested))
        selection-failures (mapv #(selection-failure
                                    % available-records resolved-names
                                    records)
                                 failures)
        first-ranked (some #(when (seq (:hypotheses %)) %) selection-failures)
        candidates (mapv :owner (:hypotheses first-ranked))]
    {:available-form-count (count available-names)
     :available-owner-count (count available-names)
     :available-owners (:owners bounded-names)
     :available-owners-returned (:returned bounded-names)
     :available-owners-omitted (:omitted bounded-names)
     :available-owners-truncated (:truncated bounded-names)
     :selection-failures selection-failures
     :form-candidates candidates
     :candidate-limit candidate-limit
     :candidates-truncated (boolean (:hypotheses-truncated first-ranked))}))
