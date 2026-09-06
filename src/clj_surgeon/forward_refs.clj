(ns clj-surgeon.forward-refs
  "Detect forward references using clj-kondo analysis."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-process :as process-env]
   [clojure.string :as str]))

(defn validated-analysis [data]
  (let [analysis (:analysis data)
        error-count (get-in data [:summary :error])]
    (when-not (and (map? data) (map? analysis)
                   (every? #(and (vector? %) (every? map? %))
                           [(:var-definitions analysis) (:var-usages analysis)]))
      (throw (ex-info "Forward-reference analyzer returned incomplete analysis"
                      {:error-type :forward-reference-analysis-invalid})))
    (when (or (and (number? error-count) (pos? error-count))
              (some #(= "error" (:level %)) (:findings data)))
      (throw (ex-info "Forward-reference analyzer reported errors"
                      {:error-type :forward-reference-analysis-failed})))
    data))

(defn- run-kondo [file]
  (let [command ["clj-kondo" "--lint" file "--fail-level" "error"
                 "--config"
                 "{:output {:format :json} :analysis {:var-definitions true :var-usages true}}"]
        result (try
                 (process-env/run-bounded!
                   {:command command
                    :cwd (System/getProperty "user.dir")
                    :timeout-ms 120000
                    :visible-byte-limit (* 1024 1024)})
                 (catch Exception error
                   (throw (ex-info
                            "Forward-reference analyzer authority is unavailable"
                            {:error-type :analyzer-authority-unverified
                             :cause-error-type (:error-type (ex-data error))}
                            error))))]
    (when-not (= :admitted (get-in result [:admission :status]))
      (throw (ex-info "Forward-reference analyzer authority is unverified"
                      {:error-type :analyzer-authority-unverified
                       :admission (:admission result)})))
    (when-not (and (:finished? result) (= 0 (:exit result)))
      (throw (ex-info "Forward-reference analysis failed"
                      {:error-type :forward-reference-analysis-failed
                       :exit (:exit result)
                       :diagnostic (str/trim (or (:err result) ""))})))
    (validated-analysis
      (try
        (json/parse-string (:out result) true)
        (catch Exception error
          (throw (ex-info "Forward-reference analyzer returned invalid JSON"
                          {:error-type :forward-reference-analysis-invalid}
                          error)))))))

(defn detect-forward-refs
  "Returns forward references: vars used before they're defined in the same namespace."
  [file ns-name]
  (let [data (run-kondo file)]
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
