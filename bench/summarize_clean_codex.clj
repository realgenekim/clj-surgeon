#!/usr/bin/env bb

(ns summarize-clean-codex
  (:require
   [clojure.string :as str]))

(def numeric-fields
  #{:wall-ms :input-tokens :cached-input-tokens :uncached-input-tokens
    :output-tokens :reasoning-output-tokens :shell-calls :file-changes :atomic-commands
    :clj-invocations :source-commands :source-output-bytes
    :total-tool-output-bytes :post-decision-source-commands
    :change-apply-successes :failed-mutation-actions
    :mcp-calls :mcp-successes :mcp-failures :mcp-tool-output-bytes})

(def boolean-fields
  #{:correct :exact-correct :skill-read :show-form :grep-form :ls-used :help-used
    :text-reader :q-used :xray-used :partition-all-used :edit-used :expr-used :first-source-edit
    :plan-generated :plan-applied :plan-apply-separate :verified
    :decision-supplied :change-used :change-apply-used :temp-manifest-patch
    :single-change-transaction :mcp-first-mutation})

(defn parse-value [field value]
  (cond
    (numeric-fields field) (parse-long value)
    (boolean-fields field) (= "true" value)
    :else value))

(defn read-runs [path]
  (let [[header & rows] (str/split-lines (slurp path))
        fields (mapv #(-> % (str/replace "_" "-") keyword)
                     (str/split header #"\t"))]
    (mapv (fn [row]
            (into {}
                  (map (fn [field value]
                         [field (parse-value field value)])
                       fields
                       (str/split row #"\t" -1))))
          rows)))

(defn median [values]
  (let [xs (vec (sort (keep identity values)))
        n (count xs)
        midpoint (quot n 2)]
    (when (pos? n)
      (if (odd? n)
        (nth xs midpoint)
        (/ (+ (nth xs (dec midpoint)) (nth xs midpoint)) 2.0)))))

(defn percent [predicate rows]
  (* 100.0 (/ (count (filter predicate rows)) (count rows))))

(defn fmt-int [number]
  (if (some? number)
    (format "%,.0f" (double number))
    "—"))

(defn summarize-group [[[task context version] rows]]
  (let [correct-rows (filterv :correct rows)]
    {:task task
     :context context
     :version version
     :runs (count rows)
     :efficiency-runs (count correct-rows)
     :correct (percent :correct rows)
     :exact (percent :exact-correct rows)
     ;; Correctness is a gate: wrong answers never make a route look faster.
     :wall (median (map :wall-ms correct-rows))
     :input (median (map :input-tokens correct-rows))
     :uncached (median (map :uncached-input-tokens correct-rows))
     :output (median (map :output-tokens correct-rows))
     :commands (median (map :shell-calls correct-rows))
     :mcp-calls (median (map :mcp-calls correct-rows))
     :file-changes (median (map :file-changes correct-rows))
     :source-bytes (median (map :source-output-bytes correct-rows))
     :mcp-output-bytes (median (map :mcp-tool-output-bytes correct-rows))
     :post-decision-reads
     (median (map :post-decision-source-commands correct-rows))
     :failed-mutations (median (map :failed-mutation-actions correct-rows))
     :mcp-failures (median (map :mcp-failures correct-rows))
     :skill-read (percent :skill-read rows)
     :change-used (percent :change-used rows)
     :change-apply (percent :change-apply-used rows)
     :single-change (percent :single-change-transaction rows)
     :mcp-first-mutation (percent :mcp-first-mutation rows)
     :temp-manifest (percent :temp-manifest-patch rows)
     :q-used (percent :q-used rows)
     :xray-used (percent :xray-used rows)
     :partition-all (percent :partition-all-used rows)
     :edit-used (percent :edit-used rows)
     :expr-used (percent :expr-used rows)
     :first-edit (percent :first-source-edit rows)
     :text-reader (percent :text-reader rows)
     :show-form (percent :show-form rows)
     :plan-separate (when (str/ends-with? task "-edit")
                      (percent :plan-apply-separate rows))}))

(defn markdown [runs]
  (let [summaries (->> runs
                       (group-by (juxt :task :context :version))
                       (map summarize-group)
                       (sort-by (juxt :task :context :version)))]
    (str
      "# Clean Codex benchmark summary\n\n"
      "Correctness is a gate. Efficiency medians include only correct runs. "
      "Token counts are the final cumulative usage reported by each Codex session.\n\n"
      "| Task | Context | Version | n | Efficiency n | Correct | Exact presentation | Median wall | Median input | Median uncached | Median output | Shell calls | MCP calls | File changes | Source output | MCP output | Post-decision reads | Failed mutations | MCP failures | Skill read | change | change! | Single change transaction | MCP first mutation | Temp manifest patch | q | xray | partition-all | edit | expr | First source edit | Text reader | show-form | Separate plan/apply |\n"
      "|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n"
      (str/join
        ""
        (for [{:keys [task context version runs efficiency-runs correct exact wall input uncached output
                      commands mcp-calls file-changes source-bytes mcp-output-bytes
                      post-decision-reads failed-mutations mcp-failures
                      skill-read change-used change-apply single-change mcp-first-mutation temp-manifest
                      q-used xray-used partition-all edit-used expr-used first-edit
                      text-reader show-form plan-separate]}
              summaries]
          (format "| %s | %s | %s | %d | %d | %.0f%% | %.0f%% | %sms | %s | %s | %s | %s | %s | %s | %sB | %sB | %s | %s | %s | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %s |\n"
                  task context version runs efficiency-runs correct exact (fmt-int wall) (fmt-int input)
                  (fmt-int uncached) (fmt-int output) (fmt-int commands) (fmt-int mcp-calls)
                  (fmt-int file-changes) (fmt-int source-bytes) (fmt-int mcp-output-bytes)
                  (fmt-int post-decision-reads) (fmt-int failed-mutations) (fmt-int mcp-failures)
                  skill-read change-used change-apply single-change mcp-first-mutation temp-manifest
                  q-used xray-used partition-all edit-used expr-used first-edit text-reader show-form
                  (if (some? plan-separate) (format "%.0f%%" plan-separate) "—")))))))

(defn self-test []
  (let [rows [{:task "task" :context "context" :version "pre"
               :correct true :exact-correct true :wall-ms 100
               :input-tokens 10 :uncached-input-tokens 5 :output-tokens 2
               :shell-calls 1 :file-changes 0 :source-output-bytes 20
               :mcp-calls 1 :mcp-successes 1 :mcp-failures 0 :mcp-tool-output-bytes 200
               :post-decision-source-commands 0 :failed-mutation-actions 0
               :change-used true :change-apply-used true
               :single-change-transaction true :mcp-first-mutation true
               :temp-manifest-patch false}
              {:task "task" :context "context" :version "pre"
               :correct false :exact-correct false :wall-ms 1
               :input-tokens 1 :uncached-input-tokens 1 :output-tokens 1
               :shell-calls 9 :file-changes 9 :source-output-bytes 1
               :mcp-calls 1 :mcp-successes 0 :mcp-failures 1 :mcp-tool-output-bytes 100
               :post-decision-source-commands 9 :failed-mutation-actions 2
               :mcp-first-mutation false}
              {:task "task" :context "context" :version "pre"
               :correct true :exact-correct true :wall-ms 300
               :input-tokens 30 :uncached-input-tokens 15 :output-tokens 6
               :shell-calls 3 :file-changes 0 :source-output-bytes 40
               :mcp-calls 1 :mcp-successes 1 :mcp-failures 0 :mcp-tool-output-bytes 400
               :post-decision-source-commands 2 :failed-mutation-actions 0
               :change-used true :change-apply-used true
               :single-change-transaction true :mcp-first-mutation true
               :temp-manifest-patch false}]
        summary (summarize-group [["task" "context" "pre"] rows])]
    (assert (= 3 (:runs summary)))
    (assert (= 2 (:efficiency-runs summary)))
    (assert (= 200.0 (:wall summary)))
    (assert (= 20.0 (:input summary)))
    (assert (= 2.0 (:commands summary)))
    (assert (= 1.0 (:mcp-calls summary)))
    (assert (= 30.0 (:source-bytes summary)))
    (assert (= 300.0 (:mcp-output-bytes summary)))
    (assert (= 1.0 (:post-decision-reads summary)))
    (assert (= 0.0 (:failed-mutations summary)))
    (assert (= 0.0 (:mcp-failures summary)))
    (assert (< (Math/abs (- (/ 200.0 3.0) (:single-change summary))) 0.001))
    (assert (< (Math/abs (- (/ 200.0 3.0) (:mcp-first-mutation summary))) 0.001))
    (assert (str/includes? (markdown rows) "MCP first mutation"))
    (println "benchmark summary self-test passed")))

(let [[argument] *command-line-args*]
  (cond
    (= "--self-test" argument) (self-test)
    argument (print (markdown (read-runs argument)))
    :else
    (do
      (binding [*out* *err*]
        (println "Usage: bb bench/summarize_clean_codex.clj RUNS.tsv"))
      (System/exit 2))))
