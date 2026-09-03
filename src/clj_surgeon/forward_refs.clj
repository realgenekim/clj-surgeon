(ns clj-surgeon.forward-refs
  "Detect forward references using clj-kondo analysis."
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clj-surgeon.mcp-process :as process-env]
   [clojure.string :as str]))

(def ^:private project-root-markers
  [".clj-kondo" "deps.edn" "project.clj" "bb.edn"])

(defn project-root-for
  "Return the nearest ancestor directory of `file` holding a clj-kondo
  configuration or a build file, or nil when no ancestor holds one.

  `exists?` decides existence for one path string, so the walk is testable
  without a filesystem. clj-kondo resolves `.clj-kondo/config.edn` by walking up
  from its own working directory, so the linted file's project root is the only
  working directory that finds the configuration written for that file. Nearest
  wins, which selects the inner project when one repository nests several."
  [file exists?]
  (loop [dir (fs/parent (fs/absolutize file))]
    (when dir
      (if (some (fn [marker] (exists? (str (fs/path dir marker))))
                project-root-markers)
        (str dir)
        (recur (fs/parent dir))))))

(defn analysis-from-result
  "Return clj-kondo's parsed output from one bounded-process `result`.

  The exit code is deliberately not consulted. clj-kondo exits 2 for warnings
  and 3 for errors, and `detect-forward-refs` reads only `:analysis`, so a lint
  finding says nothing about whether the analysis is usable. An analyzer that
  did not finish, or that produced output carrying no `:analysis`, is still a
  failure, and both carry the exit code and stderr so the caller can tell which."
  [result]
  (when-not (= :admitted (get-in result [:admission :status]))
    (throw (ex-info "Forward-reference analyzer authority is unverified"
                    {:error-type :analyzer-authority-unverified
                     :admission (:admission result)})))
  (when-not (:finished? result)
    (throw (ex-info "Forward-reference analysis failed"
                    {:error-type :forward-reference-analysis-failed
                     :exit (:exit result)
                     :diagnostic (str/trim (or (:err result) ""))})))
  (let [invalid (fn [error]
                  (throw (ex-info "Forward-reference analyzer returned invalid JSON"
                                  {:error-type :forward-reference-analysis-invalid
                                   :exit (:exit result)
                                   :diagnostic (str/trim (or (:err result) ""))}
                                  error)))
        parsed (try
                 (json/parse-string (:out result) true)
                 (catch Exception error
                   (invalid error)))]
    (if (:analysis parsed)
      parsed
      (invalid nil))))

(defn- run-kondo [file]
  (let [command ["clj-kondo" "--lint" (str (fs/absolutize file))
                 "--config"
                 "{:output {:format :json} :analysis {:var-definitions true :var-usages true}}"]
        result (try
                 (process-env/run-bounded!
                   {:command command
                    :cwd (or (project-root-for file fs/exists?)
                             (System/getProperty "user.dir"))
                    :timeout-ms 120000
                    :visible-byte-limit (* 1024 1024)})
                 (catch Exception error
                   (throw (ex-info
                            "Forward-reference analyzer authority is unavailable"
                            {:error-type :analyzer-authority-unverified
                             :cause-error-type (:error-type (ex-data error))}
                            error))))]
    (analysis-from-result result)))

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
