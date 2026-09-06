(ns clj-surgeon.mission-candidate
  "Compile candidate literal anchors against one frozen span-authorized snapshot.
   Returns future bytes or a typed refusal. Never touches the filesystem."
  (:require
   [clj-surgeon.mission-typist :as typist]
   [rewrite-clj.parser :as parser]))

(def limits {:files 64 :owners 256 :changes 128 :file-chars 262144 :total-chars 4194304})

(defn refuse [type data]
  (merge {:ok false :error-type type :mutation-attempted false} data))

(defn disjoint-spans? [owners]
  (every? (fn [spans]
            (every? (fn [[a b]] (<= (:end a) (:start b)))
                    (partition 2 1 (sort-by :start spans))))
          (vals (group-by :file owners))))

(defn valid-basis? [{:keys [sources owners budget]}]
  (and (map? sources) (seq sources) (<= (count sources) (:files limits))
       (every? (fn [[path source]]
                 (and (typist/relative-source? path) (string? source)
                      (<= (count source) (:file-chars limits)))) sources)
       (<= (reduce + (map count (vals sources))) (:total-chars limits))
       (vector? owners) (seq owners) (<= (count owners) (:owners limits))
       (every? #(typist/owner-valid? sources %) owners)
       (disjoint-spans? owners)
       (integer? (:max-files budget)) (pos? (:max-files budget))
       (<= (:max-files budget) (:files limits))
       (integer? (:max-changed-chars budget)) (pos? (:max-changed-chars budget))
       (<= (:max-changed-chars budget) (:total-chars limits))))

(defn valid-change? [{:keys [file before after] :as change}]
  (and (map? change) (= #{:file :before :after} (set (keys change)))
       (typist/relative-source? file) (string? before) (pos? (count before))
       (string? after) (not= before after)
       (<= (+ (count before) (count after)) (:file-chars limits))))

(defn anchor-hits [source before spans]
  (reduce
    (fn [hits {:keys [start end]}]
      (if (= 2 (count hits))
        (reduced hits)
        (let [bounded (subs source start end)]
          (loop [from 0 found hits]
            (let [i (.indexOf ^String bounded ^String before (int from))]
              (if (or (neg? i) (= 2 (count found)))
                found
                (recur (inc i) (conj found [(+ start i) (+ start i (count before))]))))))))
    #{} (distinct spans)))

(defn locate [{:keys [sources owners]} {:keys [file before] :as change}]
  (if-let [source (get sources file)]
    (let [hits (anchor-hits source before (filter #(= file (:file %)) owners))]
      (case (count hits)
        0 (refuse :candidate-missing-authorized-anchor {:file file})
        1 (assoc change :start (ffirst hits) :end (second (first hits)))
        (refuse :candidate-ambiguous-anchor {:file file})))
    (refuse :candidate-unauthorized-file {:file file})))

(defn overlap? [edits]
  (some (fn [[a b]] (< (:start b) (:end a)))
        (partition 2 1 (sort-by :start edits))))

(defn splice [source edits]
  (reduce (fn [s {:keys [start end after]}]
            (str (subs s 0 start) after (subs s end)))
          source (sort-by :start > edits)))

(defn compile-candidate [basis changes]
  (cond
    (not (valid-basis? basis)) (refuse :candidate-invalid-basis {})
    (not (and (vector? changes) (seq changes) (<= (count changes) (:changes limits))
              (every? valid-change? changes)))
    (refuse :candidate-invalid-changes {})
    :else
    (let [chars (reduce + (map #(+ (count (:before %)) (count (:after %))) changes))
          files (count (set (map :file changes)))
          over-budget? (or (> files (get-in basis [:budget :max-files]))
                           (> chars (get-in basis [:budget :max-changed-chars])))
          located (when-not over-budget? (mapv #(locate basis %) changes))
          error (first (filter #(false? (:ok %)) located))
          by-file (group-by :file located)]
      (cond
        over-budget? (refuse :candidate-budget-exceeded {:replacement-chars chars :files files})
        error error
        (some overlap? (vals by-file)) (refuse :candidate-overlapping-changes {})
        (or (> (count by-file) (get-in basis [:budget :max-files]))
            (> chars (get-in basis [:budget :max-changed-chars])))
        (refuse :candidate-budget-exceeded {:replacement-chars chars :files (count by-file)})
        :else
        (let [originals (select-keys (:sources basis) (keys by-file))
              future (into (sorted-map)
                           (map (fn [[file edits]]
                                  [file (splice (get originals file) edits)])) by-file)]
          (try
            (doseq [source (vals future)]
              ;; Conservative pre-parser cap. Count raw opening delimiters,
              ;; including strings/comments; false refusal is preferable to
              ;; letting a candidate exhaust the parser's stack.
              (when (> (count (take 2049 (filter #{\( \[ \{} source))) 2048)
                (throw (ex-info "Candidate delimiter budget exceeded"
                                {:error-type :candidate-parser-budget})))
              (parser/parse-string-all source))
            {:ok true :original-sources originals :future-sources future
             :changed-files (count future) :replacement-chars chars
             :mutation-attempted false}
            (catch StackOverflowError _ (refuse :candidate-parser-depth {}))
            (catch Exception e
              (refuse (or (:error-type (ex-data e)) :candidate-unparseable) {}))))))))
