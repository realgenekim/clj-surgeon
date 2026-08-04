#!/usr/bin/env bb

(ns summarize-clean-codex
  (:require
   [clojure.string :as str]))

(def numeric-fields
  #{:wall-ms :input-tokens :cached-input-tokens :uncached-input-tokens
    :output-tokens :reasoning-output-tokens :shell-calls :atomic-commands
    :clj-invocations :source-commands :source-output-bytes
    :total-tool-output-bytes})

(def boolean-fields
  #{:correct :exact-correct :skill-read :show-form :grep-form :ls-used :help-used
    :text-reader :q-used :partition-all-used :plan-generated :plan-applied
    :plan-apply-separate :verified})

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
  (let [xs (vec (sort values))
        n (count xs)
        midpoint (quot n 2)]
    (if (odd? n)
      (nth xs midpoint)
      (/ (+ (nth xs (dec midpoint)) (nth xs midpoint)) 2.0))))

(defn percent [predicate rows]
  (* 100.0 (/ (count (filter predicate rows)) (count rows))))

(defn fmt-int [number]
  (format "%,.0f" (double number)))

(defn summarize-group [[[task context version] rows]]
  {:task task
   :context context
   :version version
   :runs (count rows)
   :correct (percent :correct rows)
   :exact (percent :exact-correct rows)
   :wall (median (map :wall-ms rows))
   :input (median (map :input-tokens rows))
   :uncached (median (map :uncached-input-tokens rows))
   :output (median (map :output-tokens rows))
   :commands (median (map :shell-calls rows))
   :source-bytes (median (map :source-output-bytes rows))
   :skill-read (percent :skill-read rows)
   :q-used (percent :q-used rows)
   :partition-all (percent :partition-all-used rows)
   :text-reader (percent :text-reader rows)
   :show-form (percent :show-form rows)
   :plan-separate (when (= "case-edit" task)
                    (percent :plan-apply-separate rows))})

(defn markdown [runs]
  (let [summaries (->> runs
                       (group-by (juxt :task :context :version))
                       (map summarize-group)
                       (sort-by (juxt :task :context :version)))]
    (str
      "# Clean Codex benchmark summary\n\n"
      "Correctness is a gate. Token counts are the final cumulative usage "
      "reported by each Codex session.\n\n"
      "| Task | Context | Version | n | Correct | Exact presentation | Median wall | Median input | Median uncached | Median output | Shell calls | Source output | Skill read | q | partition-all | Text reader | show-form | Separate plan/apply |\n"
      "|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n"
      (str/join
        ""
        (for [{:keys [task context version runs correct exact wall input uncached output
                      commands source-bytes skill-read q-used partition-all text-reader
                      show-form plan-separate]}
              summaries]
          (format "| %s | %s | %s | %d | %.0f%% | %.0f%% | %sms | %s | %s | %s | %s | %sB | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %.0f%% | %s |\n"
                  task context version runs correct exact (fmt-int wall) (fmt-int input)
                  (fmt-int uncached) (fmt-int output) (fmt-int commands)
                  (fmt-int source-bytes) skill-read q-used partition-all text-reader show-form
                  (if (some? plan-separate) (format "%.0f%%" plan-separate) "—")))))))

(let [[path] *command-line-args*]
  (when-not path
    (binding [*out* *err*]
      (println "Usage: bb bench/summarize_clean_codex.clj RUNS.tsv"))
    (System/exit 2))
  (print (markdown (read-runs path))))
