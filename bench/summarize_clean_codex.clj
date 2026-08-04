#!/usr/bin/env bb

(ns summarize-clean-codex
  (:require
   [clojure.string :as str]))

(def numeric-fields
  #{:wall-ms :input-tokens :cached-input-tokens :uncached-input-tokens
    :output-tokens :reasoning-output-tokens :shell-calls :file-changes :atomic-commands
    :clj-invocations :source-commands :source-output-bytes
    :total-tool-output-bytes})

(def boolean-fields
  #{:correct :exact-correct :skill-read :show-form :grep-form :ls-used :help-used
    :text-reader :q-used :xray-used :partition-all-used :edit-used :expr-used :first-source-edit
    :plan-generated :plan-applied :plan-apply-separate :verified})

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
     :file-changes (median (map :file-changes correct-rows))
     :source-bytes (median (map :source-output-bytes correct-rows))
     :skill-read (percent :skill-read rows)
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
      "| Task | Context | Version | n | Efficiency n | Correct | Exact presentation | Median wall | Median input | Median uncached | Median output | Shell calls | File changes | Source output | Skill read | q | xray | partition-all | edit | expr | First source edit | Text reader | show-form | Separate plan/apply |\n"
      "|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n"
      (str/join
        ""
        (for [{:keys [task context version runs efficiency-runs correct exact wall input uncached output
                      commands file-changes source-bytes skill-read q-used xray-used partition-all edit-used expr-used
                      first-edit text-reader show-form plan-separate]}
              summaries]
          (format "| %s | %s | %s | %d | %d | %.0f%% | %.0f%% | %sms | %s | %s | %s | %s | %s | %sB | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %s |\n"
                  task context version runs efficiency-runs correct exact (fmt-int wall) (fmt-int input)
                  (fmt-int uncached) (fmt-int output) (fmt-int commands) (fmt-int file-changes)
                  (fmt-int source-bytes) skill-read q-used xray-used partition-all edit-used expr-used first-edit
                  text-reader show-form
                  (if (some? plan-separate) (format "%.0f%%" plan-separate) "—")))))))

(defn self-test []
  (let [rows [{:task "task" :context "context" :version "pre"
               :correct true :exact-correct true :wall-ms 100
               :input-tokens 10 :uncached-input-tokens 5 :output-tokens 2
               :shell-calls 1 :file-changes 0 :source-output-bytes 20}
              {:task "task" :context "context" :version "pre"
               :correct false :exact-correct false :wall-ms 1
               :input-tokens 1 :uncached-input-tokens 1 :output-tokens 1
               :shell-calls 9 :file-changes 9 :source-output-bytes 1}
              {:task "task" :context "context" :version "pre"
               :correct true :exact-correct true :wall-ms 300
               :input-tokens 30 :uncached-input-tokens 15 :output-tokens 6
               :shell-calls 3 :file-changes 0 :source-output-bytes 40}]
        summary (summarize-group [["task" "context" "pre"] rows])]
    (assert (= 3 (:runs summary)))
    (assert (= 2 (:efficiency-runs summary)))
    (assert (= 200.0 (:wall summary)))
    (assert (= 20.0 (:input summary)))
    (assert (= 2.0 (:commands summary)))
    (assert (= 30.0 (:source-bytes summary)))
    (assert (str/includes? (markdown rows) "Efficiency n"))
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
