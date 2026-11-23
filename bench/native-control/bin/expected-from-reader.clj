(require '[babashka.process :as process]
         '[clojure.data :as data]
         '[clojure.string :as str])

(defn require! [ok message]
  (when-not ok (throw (ex-info message {}))))

(defn forms [source]
  ;; Core read supports #' and reader conditionals; EDN does not. Never evaluate input.
  (binding [*read-eval* false]
    (with-open [r (java.io.PushbackReader. (java.io.StringReader. source))]
      (let [eof (Object.)]
        (loop [acc [] aliases {:current 'user}]
          (let [f (clojure.core/read {:read-cond :allow :eof eof :auto-resolve aliases} r)]
            (if (identical? eof f) acc
                (recur (conj acc f)
                       (if (and (seq? f) (= 'ns (first f)))
                         (into {:current (second f)}
                               (for [v (tree-seq coll? seq f)
                                     :when (and (vector? v) (symbol? (first v)))
                                     [k a] (partition 2 (rest v)) :when (#{:as :as-alias} k)]
                                 [a (first v)]))
                         aliases)))))))))

(defn one [label xs]
  (require! (= 1 (count xs)) (str "Missing or ambiguous " label " (found " (count xs) ")"))
  (first xs))

(defn named [fs nm]
  (one (str "form " nm)
       (filter #(and (seq? %) (#{'defn 'defn- 'def '>defn} (first %))
                     (= nm (second %))) fs)))

(defn nodes [x] (tree-seq coll? seq x))
(defn head? [h x] (and (seq? x) (= h (first x))))
(defn var-name [h] (str (if (head? 'var h) (second h) h)))
(defn routes->lines [v]
  (require! (vector? v) "Expected routes vector")
  (vec (for [[path m] v, [method {h :handler}] (sort-by str m)]
         (str (name method) " " path " " (var-name h)))))

(defn make-routes [form]
  (require! (and (= 4 (count form)) (= '(defn make-routes []) (take 3 form))
                 (head? 'additive-routes/with-additive (nth form 3)) (= 2 (count (nth form 3)))
                 (vector? (second (nth form 3))))
            "Unexpected make-routes shape; expected (defn make-routes [] (with-additive [...]))")
  (second (nth form 3)))

(defn case-pairs [form selector]
  (let [c (one "case form" (filter #(and (head? 'case %) (selector %)) (nodes form)))
        pairs (partition 2 (drop 2 c))]
    ;; These pins have no grouped case keys. Refuse them rather than silently omit them.
    (require! (not-any? #(seq? (first %)) pairs) "Grouped case keys are unsupported")
    (require! (every? #(keyword? (first %)) pairs) "Expected keyword case keys")
    (mapv vec pairs)))

(defn expected [{:keys [id form expected]} fs]
  (let [f (when (and form (not= form "ns")) (named fs (symbol form)))]
    (case id
      "T01" (:body (get (into {} (case-pairs f #(= '(:type (ex-data e)) (second %))))
                        :store/stale-command))
      "T02" (some-> (get (into {} (case-pairs f (constantly true))) :cmd/latest-reply) str)
      "T03" (:src (second (one "schedule-page sched-tabs script"
                              (filter #(and (vector? %) (= :script (first %))
                                            (string? (:src (second %)))
                                            (str/starts-with? (:src (second %)) "/js/sched-tabs"))
                                      (nodes f)))))
      "T04" (some-> (get-in (into {} (make-routes f)) ["/people/outreach" :get :handler]) var-name)
      "T05" (routes->lines (make-routes f))
      "T06" (routes->lines (nth f 2))
      "T07" (mapv (fn [[k v]] (str k " " v)) (case-pairs f (constantly true)))
      "T08" (let [public (filter #(and (head? 'defn %) (not (:private (meta (second %))))) fs)]
              (require! (seq public) "Missing exports.clj public defn forms")
              (doseq [nm expected] (named fs (symbol nm)))
              (mapv #(str (second %)) public))
      ("T09" "T10" "T12")
      (pr-str (one (str "argument vector for " form) (take 1 (filter vector? (drop 2 f)))))
      "T11" (let [ns-form (one "ns form" (filter #(head? 'ns %) fs))
                  lib (one "cfp-scheduler-killer.schedule require"
                           (filter #(and (vector? %) (= 'cfp-scheduler-killer.schedule (first %)))
                                   (nodes ns-form)))]
              (some-> (get (apply hash-map (rest lib)) :as) str)))))

(def pinned-forms
  (memoize (fn [repo sha file]
             (forms (:out (process/shell {:out :string :err :string}
                                        "git" "-C" repo "show" (str sha ":" file)))))))

(defn main [args]
  (require! (= 1 (count args)) "Usage: bb expected-from-reader.clj tasks.edn")
  (let [tasks (one "task catalog" (forms (slurp (first args))))
        _ (require! (= (set (map #(format "T%02d" %) (range 1 13))) (set (map :id tasks)))
                    "Expected tasks T01 through T12")
        _ (require! (= 12 (count tasks)) "Expected exactly twelve tasks")
        results (mapv (fn [{:keys [id repo sha file answer-kind] :as task}]
                        (let [actual (expected task (pinned-forms repo sha file))
                              normalize #(if (= :set answer-kind) (set %) %)]
                          (require! (some? actual) (str id ": missing answer in " file))
                          [id (normalize (:expected task)) (normalize actual)])) tasks)
        failures (filter (fn [[_ want got]] (not= want got)) results)]
    (doseq [[id want got] failures]
      (let [[removed added] (data/diff want got)]
        (println (str id " diff (- tasks.edn, + pinned sha):"))
        (println "-" (pr-str removed))
        (println "+" (pr-str added))))
    (println (- 12 (count failures)) "of 12 expected values reproduced from the pinned shas")
    (if (seq failures) 1 0)))

(when (= *file* (System/getProperty "babashka.file"))
  (System/exit (main *command-line-args*)))
