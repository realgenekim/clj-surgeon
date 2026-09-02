(ns clj-surgeon.forward-refs
  "Detect forward references using clj-kondo analysis."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-process :as process-env]
   [clojure.string :as str]))

(defn- unread-subject?
  "Pure: true when the analyzer reported that it could not read its subject.
  A file-level error finding means the payload describes nothing."
  [data]
  (boolean
    (some #(and (= "file" (str (:type %))) (= "error" (str (:level %))))
          (:findings data))))

;; @spec MCP-OP-LS-003
(defn- analyzer-diagnostic
  "Pure: one non-empty diagnostic drawn from the analyzer's OWN output.
  clj-kondo writes its findings to stdout, so a diagnostic read only from
  stderr is always empty and can never be recovered from."
  [{:keys [out err exit]} data]
  (let [findings (->> (:findings data)
                      (map #(str (:filename %) ": " (:level %) ": " (:message %)))
                      (remove str/blank?))]
    (or (not-empty (str/join "; " findings))
        (not-empty (str/trim (str err)))
        (not-empty (str/trim (str out)))
        (str "the analyzer exited " exit " without output"))))

(defn- run-kondo [file]
  (let [command ["clj-kondo" "--lint" file
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
    ;; @spec MCP-OP-LS-001
    ;; The analyzer's exit status counts FINDINGS, not failures: clj-kondo exits
    ;; 2 for warnings and 3 for errors on a file it read and analyzed perfectly.
    ;; Judge the analysis by whether it produced an analysis payload for a file
    ;; the analyzer actually read.
    (when-not (:finished? result)
      (throw (ex-info "Forward-reference analysis failed"
                      {:error-type :forward-reference-analysis-failed
                       :exit (:exit result)
                       :diagnostic (analyzer-diagnostic result nil)})))
    (let [data (try
                 (json/parse-string (:out result) true)
                 (catch Exception error
                   (throw (ex-info
                            "Forward-reference analyzer returned invalid JSON"
                            {:error-type :forward-reference-analysis-invalid
                             :exit (:exit result)
                             :diagnostic (analyzer-diagnostic result nil)}
                            error))))]
      (when-not (map? (:analysis data))
        (throw (ex-info "Forward-reference analysis failed"
                        {:error-type :forward-reference-analysis-failed
                         :exit (:exit result)
                         :diagnostic (analyzer-diagnostic result data)})))
      ;; A finding of type "file" at error level means the analyzer never read
      ;; the subject. An analysis payload that omits its own subject is
      ;; :unverified, never a silent zero-forward-reference success.
      (when (unread-subject? data)
        (throw (ex-info "Forward-reference analysis failed"
                        {:error-type :forward-reference-analysis-failed
                         :exit (:exit result)
                         :diagnostic (analyzer-diagnostic result data)})))
      data)))

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

;; @spec MCP-OP-LS-002
(defn try-detect-forward-refs
  "Run forward-reference analysis without granting it authority to fail a caller.

   Returns {:ok true :forward-refs [...]} or a typed
   {:ok false :error-type ... :diagnostic ... :note ...}. An outline that parses
   must never be withheld because this optional decoration was unavailable."
  [file ns-name]
  (try
    {:ok true :forward-refs (detect-forward-refs file ns-name)}
    (catch Exception error
      (let [data (ex-data error)
            error-type (or (:error-type data) :forward-reference-analysis-failed)
            diagnostic (or (not-empty (str/trim (str (:diagnostic data))))
                           (not-empty (str/trim (str (.getMessage error))))
                           "no diagnostic was produced")]
        {:ok false
         :error-type error-type
         :exit (:exit data)
         :diagnostic diagnostic
         :note (str "Forward-reference analysis was unavailable ("
                    (name error-type) "); the outline is complete and unaffected.")}))))
