(ns summarize-inspect-mcp-benchmark
  (:require
   [clojure.string :as str]))

(def numeric-fields
  #{:replicate :order :wall-ms :exit-code :input-tokens :cached-input-tokens
    :output-tokens :shell-calls :mcp-calls :mcp-successes :mcp-failures
    :source-bearing-actions :process-startups :request-bytes :result-bytes})

(defn parse-row
  [fields line]
  (into {}
        (map (fn [field raw]
               (let [key (keyword (str/replace field "_" "-"))]
                 [key (cond
                        (= key :correct) (= "true" raw)
                        (contains? numeric-fields key) (parse-long raw)
                        :else raw)]))
             fields (str/split line #"\t" -1))))

(defn read-runs
  [path]
  (let [[header & lines] (str/split-lines (slurp path))
        fields (str/split header #"\t")]
    (mapv #(parse-row fields %) lines)))

(defn median
  [values]
  (let [ordered (vec (sort values))
        n (count ordered)
        middle (quot n 2)]
    (cond
      (zero? n) nil
      (odd? n) (nth ordered middle)
      :else (/ (+ (nth ordered (dec middle)) (nth ordered middle)) 2.0))))

(defn summarize-lane
  [[lane runs]]
  (let [correct (filterv :correct runs)]
    {:lane lane
     :runs (count runs)
     :correct (count correct)
     :wall-ms (median (map :wall-ms correct))
     :input-tokens (median (map :input-tokens correct))
     :output-tokens (median (map :output-tokens correct))
     :shell-calls (median (map :shell-calls correct))
     :mcp-calls (median (map :mcp-calls correct))
     :mcp-failures (reduce + 0 (map :mcp-failures runs))
     :source-actions (median (map :source-bearing-actions correct))
     :process-startups (median (map :process-startups correct))
     :request-bytes (median (map :request-bytes correct))
     :result-bytes (median (map :result-bytes correct))}))

(defn percent-lower
  [candidate control]
  (when (and candidate control (pos? control))
    (* 100.0 (/ (- control candidate) control))))

(defn comparison
  [label candidate control]
  (when (and candidate control)
    (let [change (percent-lower candidate control)]
      (if (neg? change)
        (format "MCP versus %s: %.1f%% higher median wall (%sms versus %sms).\n"
                label (- change) candidate control)
        (format "MCP versus %s: %.1f%% lower median wall (%sms versus %sms).\n"
                label change candidate control)))))

(defn markdown
  [runs]
  (let [summaries (->> runs (group-by :lane) (map summarize-lane)
                       (sort-by :lane))
        by-lane (into {} (map (juxt :lane identity)) summaries)
        mcp (:wall-ms (get by-lane "mcp"))
        cli (:wall-ms (get by-lane "cli"))
        native (:wall-ms (get by-lane "native"))]
    (str
      "# inspect_clojure clean-agent benchmark\n\n"
      "Correctness is a gate; medians use correct runs only.\n\n"
      "| Lane | Correct | Median wall | Shell calls | MCP calls | MCP failures | Source actions | Process startups | Input tokens | Output tokens | Request bytes | Result bytes |\n"
      "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n"
      (str/join
        ""
        (for [{:keys [lane runs correct wall-ms shell-calls mcp-calls
                      mcp-failures source-actions process-startups input-tokens
                      output-tokens request-bytes result-bytes]} summaries]
          (format "| %s | %d/%d | %sms | %s | %s | %d | %s | %s | %s | %s | %s | %s |\n"
                  lane correct runs wall-ms shell-calls mcp-calls mcp-failures
                  source-actions process-startups input-tokens output-tokens
                  request-bytes result-bytes)))
      "\n"
      (comparison "CLI" mcp cli)
      (when (and mcp cli native) "\n")
      (comparison "native" mcp native))))

(defn -main
  [& [path]]
  (print (markdown (read-runs path))))

(apply -main *command-line-args*)
