(require '[clojure.edn :as edn]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[rewrite-clj.zip :as z])

(def root "docs/observations/2026-09-12-bbtower-block-b/attempt20/")
(def baseline (edn/read-string (slurp (str root "baseline.edn"))))

(defn stats [rows]
  (let [walls (mapv :elapsed-ms rows)
        n (count walls)
        mean (/ (double (reduce + walls)) n)
        sd (Math/sqrt (/ (reduce + (map #(let [d (- % mean)] (* d d)) walls)) (dec n)))]
    {:n n :walls-ms walls :mean-ms mean :sd-ms sd}))

(defn fold-namespace [n]
  (let [samples (into {}
                      (for [runtime [:jvm :bb]]
                        [runtime
                         (mapv (fn [run]
                                 (let [path (str root "measurements/" n "-" (name runtime) "-" run ".edn")
                                       row (edn/read-string (slurp path))]
                                   (assert (= [n runtime] [(:namespace row) (:runtime row)]) path)
                                   (assoc row :log path)))
                               (range 1 7))]))
        jvm (assoc (stats (:jvm samples)) :logs (mapv :log (:jvm samples)))
        bb (assoc (stats (:bb samples)) :logs (mapv :log (:bb samples)))
        ratio (/ (+ (:mean-ms bb) (* 2 (:sd-ms bb)))
                 (max 1.0 (- (:mean-ms jvm) (* 2 (:sd-ms jvm)))))
        failed (vec (for [[_runtime rows] samples
                          row rows
                          :when (or (not (pos? (get-in row [:result :test] 0)))
                                    (pos? (+ (get-in row [:result :fail] 0)
                                             (get-in row [:result :error] 0))))]
                      (:log row)))
        prior-defect (get-in baseline [:contract-failures n])
        runtime (if (and (= :bb (get-in baseline [:portability n]))
                         (empty? failed) (nil? prior-defect) (<= ratio 2.0)) :bb :jvm)]
    [n (cond-> {:n 6 :jvm jvm :bb bb :conservative-ratio ratio :runtime runtime
                :previous-runtime (get-in baseline [:runtimes n])}
         prior-defect (assoc :contract-failure prior-defect)
         (seq failed) (assoc :failed-samples failed))]))

(let [rows (into (sorted-map) (map fold-namespace (:paired baseline)))
      table (str "| Namespace | JVM n | JVM mean ms | JVM sd ms | bb n | bb mean ms | bb sd ms | Conservative ratio | Before | After |\n"
                 "|---|---:|---:|---:|---:|---:|---:|---:|---|---|\n")
      render (fn [[n {:keys [jvm bb conservative-ratio previous-runtime runtime]}]]
               (format "| %s | %d | %.6f | %.6f | %d | %.6f | %.6f | %.9f | %s | %s |\n"
                       n (:n jvm) (:mean-ms jvm) (:sd-ms jvm)
                       (:n bb) (:mean-ms bb) (:sd-ms bb) conservative-ratio
                       previous-runtime runtime))]
  (spit (str root "runtime-table.edn") (with-out-str (pp/pprint rows)))
  (when-not (= "--table-only" (first *command-line-args*))
    (let [file "test/clj_surgeon/lane_manifest.clj"
          loc (loop [loc (z/of-string (slurp file))]
                (assert (not (z/end? loc)) "runtime-measurements owner must exist")
                (if (and (= :list (z/tag loc))
                         (= '(def runtime-measurements) (take 2 (z/sexpr loc))))
                  loc (recur (z/right loc))))
          compact (into (sorted-map)
                        (map (fn [[n row]]
                               [n (-> row (update :jvm dissoc :logs) (update :bb dissoc :logs))])) rows)
          data (with-out-str (pp/pprint compact))
          replacement (str "(def runtime-measurements\n"
                           "  (let [rows '" (str/trim data) "]\n"
                           "    (into {}\n"
                           "          (map (fn [[n row]]\n"
                           "                 [n (reduce (fn [m runtime]\n"
                           "                              (assoc-in m [runtime :logs]\n"
                           "                                (mapv #(str \"" root "measurements/\" n \"-\" (name runtime) \"-\" % \".edn\")\n"
                           "                                      (range 1 7))))\n"
                           "                            row [:jvm :bb])]))\n"
                           "          rows)))")]
      (spit (str root "runtime-data.patch")
            (str "*** Begin Patch\n*** Update File: " file "\n@@\n"
                 (str/join "\n" (map #(str "-" %) (str/split-lines (z/string loc)))) "\n"
                 (str/join "\n" (map #(str "+" %) (str/split-lines replacement))) "\n*** End Patch\n"))))
  (spit (str root "runtime-table.md") (str "# Six-run namespace controls\n\n" table (apply str (map render rows))))
  (spit (str root "reassignments.md")
        (str "# Assignment flips\n\nComputed by fold.clj from measurements/*.edn. Sample sd uses n-1.\n\n"
             table (apply str (map render (filter (fn [[_ row]] (not= (:runtime row) (:previous-runtime row))) rows)))))
  (prn {:namespaces (count rows) :samples (* 12 (count rows))
        :failed (into {} (keep (fn [[n row]] (when (:failed-samples row) [n (:failed-samples row)]))) rows)}))
