#!/usr/bin/env bb
(ns native-control.summarize
  (:require [cheshire.core :as json] [clojure.string :as str] [clojure.set :as set]
            [clojure.edn :as edn] [babashka.fs :as fs]
            [clojure.test :refer [deftest is run-tests]]))

(def empty-metrics {:actions 0 :tool-output-chars 0 :surgeon-calls 0 :surgeon-refusals 0
                    :input-tokens 0 :cached-input-tokens 0 :output-tokens 0 :answer nil :skipped-lines 0})
(defn run-metrics [events-jsonl-string]
  (reduce (fn [m line]
            (try
              (let [event (json/parse-string line true)
                    _ (when-not (map? event) (throw (ex-info "Not an event object" {})))
                    {:keys [type item usage]} event]
                (cond
                  (= type "turn.completed")
                  (reduce (fn [r [k j]] (update r k + (get usage j 0))) m
                          [[:input-tokens :input_tokens] [:cached-input-tokens :cached_input_tokens]
                           [:output-tokens :output_tokens]])
                  (not= type "item.completed") m
                  (= (:type item) "agent_message") (assoc m :answer (:text item))
                  (= (:type item) "command_execution")
                  (let [surgeon? (str/includes? (or (:command item) "") "clj-surgeon")
                        output (or (:aggregated_output item) "")]
                    (cond-> (-> m (update :actions inc) (update :tool-output-chars + (count output)))
                      surgeon? (update :surgeon-calls inc)
                      (and surgeon? (str/includes? output ":error-type")) (update :surgeon-refusals inc)))
                  :else m))
              (catch Exception _ (update m :skipped-lines inc))))
          empty-metrics (remove str/blank? (str/split-lines events-jsonl-string))))
(defn normalize-line
  "Normalize answer decoration and whitespace; trailing periods remain strict."
  [s]
  (-> (or s "") str/trim (str/replace #"^[`\"']+|[`\"']+$" "")
      (str/replace #"^(?:[-*•]|\d+[.)])\s+" "")
      (str/replace #"^#'" "")
      (str/replace #"^[`\"']+|[`\"']+$" "") str/trim (str/replace #"\s+" " ")))
(defn score [task answer]
  (let [answer (when (some? answer)
                 (str/join "\n" (remove #(re-matches #"^```\w*$" %) (str/split-lines answer))))
        exact? (= :exact (:answer-kind task))
        expected (set (map normalize-line (if exact? [(:expected task)] (:expected task))))
        actual (if (nil? answer) #{} (if exact? #{(normalize-line answer)}
                    (set (remove str/blank? (map normalize-line (str/split-lines answer))))))]
    {:correct (and (some? answer) (= expected actual)) :expected-count (count expected)
     :answer-count (count actual) :missing (vec (sort (set/difference expected actual)))
     :extra (vec (sort (set/difference actual expected)))}))
(defn run-row [task run-json metrics scoring]
  (merge (into {} (map (fn [[k v]] [(keyword (str/replace (name k) "_" "-")) v]) run-json))
         metrics {:task (:id task) :class (:class task) :correct (:correct scoring)
                  :missing-count (count (:missing scoring)) :extra-count (count (:extra scoring))}))
(defn median [xs]
  (let [v (vec (sort xs)) n (count v) h (quot n 2)]
    (when (pos? n) (if (odd? n) (v h) (/ (+ (v (dec h)) (v h)) 2.0)))))
(defn medians [rows]
  (into (sorted-map)
        (for [[k rs] (group-by (juxt :class :arm) rows)]
          [k (merge {:n (count rs) :correct (count (filter :correct rs))
                     :invalid (count (filter #(or (not= 0 (:exit-code %)) (true? (:tree-changed %))) rs))}
                    (into {} (for [field [:actions :tool-output-chars :input-tokens :wall-ms]]
                               [(keyword (str "median-" (name field))) (median (map field rs))])))])))
(def promotion-rule {:min-cut 30 :max-actions-delta 0 :min-correctness-delta 0})
(defn expected-coverage [tasks reps]
  (set (for [task tasks arm ["arm1" "arm2" "arm3"] rep (range 1 (inc reps))]
         [(:id task) arm rep])))
(defn coverage-error [rows expected reps]
  (let [runs (frequencies (map (juxt :task :arm :rep) rows))
        missing (set/difference expected (set (keys runs)))
        duplicates (keep (fn [[triple n]] (when (> n 1) triple)) runs)
        outside (filter (fn [[_ _ rep]] (not (and (integer? rep) (<= 1 rep reps)))) (keys runs))
        messages (for [[label triples] [["incomplete coverage: missing " missing]
                                       ["duplicate runs: " duplicates]
                                       [(str "rep outside 1.." reps ": ") outside]]
                       :when (seq triples)]
                   (str label (pr-str (vec (sort-by pr-str triples)))))]
    (when (seq messages) {:promote false :reason (str/join "; " messages)})))
(defn expected-counts [tasks reps]
  (into {} (for [[class n] (frequencies (map :class tasks)) arm ["arm1" "arm2" "arm3"]]
             [[class arm] (* n reps)])))
(defn parse-reps [args]
  (if (empty? args) 2
    (let [[flag value] args n (when (and value (re-matches #"[0-9]+" value)) (parse-long value))]
      (if (and (= 2 (count args)) (= "--reps" flag) n (pos? n)) n
        (throw (ex-info "Usage: summarize.clj tasks.edn ROOT [--reps N]; N must be positive" {}))))))
(defn verdict
  ([summaries class treatment controls] (verdict summaries class treatment controls nil))
  ([summaries class treatment controls expected]
  (let [t (get summaries [class treatment])
        arms (cons treatment controls)
        invalid (into {} (map (fn [a] [a (get-in summaries [[class a] :invalid] 0)]) arms))
        cs (map #(get summaries [class %]) controls)
        c (first (sort-by :median-tool-output-chars (remove nil? cs)))]
    (cond
      (some pos? (vals invalid))
      {:promote false :reason "invalid runs present" :invalid invalid}
      (and expected (some #(not= (get expected [class %]) (get-in summaries [[class %] :n] 0)) arms))
      {:promote false :reason "incomplete coverage"
       :expected (select-keys expected (map #(vector class %) arms))
       :observed (into {} (map #(vector [class %] (get-in summaries [[class %] :n] 0)) arms))}
      (or (nil? t) (some nil? cs) (nil? c) (zero? (:median-tool-output-chars c)))
      {:promote false :tool-output-cut nil :actions-delta nil :correctness-delta nil :reason "Insufficient control data"}
      (some #(not= (:n t) (:n %)) cs)
      {:promote false :tool-output-cut nil :actions-delta nil :correctness-delta nil :reason "unequal n"}
      :else
      (let [cut (* 100.0 (- 1 (/ (:median-tool-output-chars t) (:median-tool-output-chars c))))
            actions (- (:median-actions t) (:median-actions c))
            correct (- (:correct t) (:correct c))
            failures (cond-> []
                       (< cut (:min-cut promotion-rule)) (conj "tool-output cut below threshold")
                       (> actions (:max-actions-delta promotion-rule)) (conj "actions increased")
                       (< correct (:min-correctness-delta promotion-rule)) (conj "correctness decreased"))]
        {:promote (empty? failures) :tool-output-cut cut :actions-delta actions
         :correctness-delta correct :reason (if (seq failures) (str/join "; " failures) "Meets promotion rule")})))))
(defn rows-tsv [rows]
  (let [fields (sort (set (mapcat keys rows)))
        cell #(str/escape (if (nil? %) "" (str %)) {\\ "\\\\" \tab "\\t" \newline "\\n" \return "\\r"})]
    (str (str/join "\n" (cons (str/join "\t" (map name fields))
                             (map #(str/join "\t" (map (comp cell %) fields)) rows))) "\n")))
(defn table
  ([summaries] (table summaries nil 0))
  ([summaries expected incomplete] (table summaries expected incomplete nil))
  ([summaries expected incomplete coverage]
  (str "Incomplete runs: " incomplete "\n\n"
       "| Class | Arm | n | Correct | Invalid | Actions | Tool chars | Input tokens | Wall ms |\n"
       "|---|---|---:|---:|---:|---:|---:|---:|---:|\n"
       (apply str (for [[[c a] m] summaries]
                    (str "| " (str/join " | " (concat [c a] ((juxt :n :correct :invalid :median-actions
                              :median-tool-output-chars :median-input-tokens :median-wall-ms) m))) " |\n")))
       (apply str (for [c (sort (set (map first (concat (keys summaries) (keys expected)))))
                        [t controls] [["arm3" ["arm1" "arm2"]] ["arm2" ["arm1"]]]]
                    (str "\n" c " " t " vs best of " (str/join "/" controls) ": "
                         (pr-str (or coverage
                                     (if (pos? incomplete)
                                       {:promote false :reason "incomplete coverage" :incomplete incomplete}
                                       (verdict summaries c t controls expected)))) "\n"))))))

(deftest event-stream
  (let [events (str "{\"type\":\"item.completed\",\"item\":{\"type\":\"command_execution\",\"command\":\"cat x\",\"aggregated_output\":\"abc\",\"exit_code\":0}}\n"
                    "{\"type\":\"item.completed\",\"item\":{\"type\":\"command_execution\",\"command\":\"clj-surgeon :op :ls\",\"aggregated_output\":\":error-type\",\"exit_code\":1}}\n"
                    "{\"type\":\"item.completed\",\"item\":{\"type\":\"agent_message\",\"text\":\"early\"}}\n"
                    "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":10,\"cached_input_tokens\":4,\"output_tokens\":2}}\n"
                    "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":20,\"cached_input_tokens\":6,\"output_tokens\":3}}\n"
                    "bad json\n"
                    "{\"type\":\"item.completed\",\"item\":{\"type\":\"agent_message\",\"text\":\"final\"}}\n")]
    (is (= {:actions 2 :tool-output-chars 14 :surgeon-calls 1 :surgeon-refusals 1
            :input-tokens 30 :cached-input-tokens 10 :output-tokens 5
            :answer "final" :skipped-lines 1} (run-metrics events)))))
(deftest event-boundaries
  (is (= empty-metrics (run-metrics "")))
  (is (= empty-metrics (run-metrics "{\"type\":\"item.started\"}\n")))
  (is (= empty-metrics (run-metrics "{\"type\":\"turn.completed\",\"usage\":{}}")))
  (is (= 3 (:skipped-lines (run-metrics "null\n[]\nbroken"))))
  (doseq [s ["\n" " \t\r\n\n  "]]
    (is (= empty-metrics (run-metrics s))))
  (is (= 1 (:skipped-lines (run-metrics " \n bad json\n\t"))))
  (is (= (assoc empty-metrics :actions 1 :tool-output-chars 11)
         (run-metrics "{\"type\":\"item.completed\",\"item\":{\"type\":\"command_execution\",\"command\":\"echo\",\"aggregated_output\":\":error-type\"}}")))
  (is (= (assoc empty-metrics :actions 1 :surgeon-calls 1)
         (run-metrics "{\"type\":\"item.completed\",\"item\":{\"type\":\"command_execution\",\"command\":\"clj-surgeon\"}}"))))
(deftest normalization
  (doseq [[input expected] [["  hello  world " "hello world"]
                            ["- `alpha`" "alpha"]
                            ["* \"alpha\"" "alpha"]
                            ["1. 'alpha'" "alpha"]
                            ["12) alpha" "alpha"]
                            ["• alpha" "alpha"] ["#'alpha" "alpha"]
                            ["• #'alpha" "alpha"] ["alpha." "alpha."]
                            ["`#'foo`" "foo"] ["\"- foo\"" "foo"]
                            ["`a\t b`" "a b"]
                            ["-not-a-bullet" "-not-a-bullet"]
                            ["1.2" "1.2"]
                            ["a\nb" "a b"]
                            [nil ""] [" " ""]]]
    (is (= expected (normalize-line input)))))
(deftest exact-scoring
  (let [task {:answer-kind :exact :expected "hello world"}]
    (is (= {:correct true :expected-count 1 :answer-count 1 :missing [] :extra []}
           (score task "  `hello   world` ")))
    (is (= {:correct false :expected-count 1 :answer-count 1
            :missing ["hello world"] :extra ["other"]}
           (score task "other")))
    (is (= {:correct false :expected-count 1 :answer-count 0
            :missing ["hello world"] :extra []} (score task nil)))
    (is (false? (:correct (score task "Hello world"))))
    (is (:correct (score {:answer-kind :exact :expected ""} "")))
    (is (false? (:correct (score {:answer-kind :exact :expected ""} nil))))))
(deftest set-scoring
  (let [task {:answer-kind :set :expected ["a" "b"]}]
    (is (= {:correct true :expected-count 2 :answer-count 2 :missing [] :extra []}
           (score task "- b\n\n1) `a`\na")))
    (is (= {:correct false :expected-count 2 :answer-count 2 :missing ["b"] :extra ["c"]}
           (score task "a\nc")))
    (is (= {:correct false :expected-count 2 :answer-count 0 :missing ["a" "b"] :extra []}
           (score task nil)))
    (is (= ["b"] (:missing (score task "a"))))
    (is (= ["c"] (:extra (score task "a\nb\nc"))))
    (is (:correct (score {:answer-kind :set :expected []} "\n")))
    (is (false? (:correct (score {:answer-kind :set :expected []} nil))))
    (is (= 1 (:expected-count (score {:answer-kind :set :expected ["a" "`a`"]} "a"))))))
(deftest fenced-answers
  (is (:correct (score {:answer-kind :set :expected ["a" "b"]} "```text\na\nb\n```")))
  (is (:correct (score {:answer-kind :exact :expected "[req]"} "```clojure\n[req]\n```")))
  (is (:correct (score {:answer-kind :set :expected ["a" "b"]} "```\na\n```\n```text\nb\n```")))
  (is (false? (:correct (score {:answer-kind :exact :expected "alpha"} "alpha.")))))
(deftest flat-row
  (is (= {:run-id "r" :task "T01" :class :inventory :arm "arm1" :rep 1 :wall-ms 200
          :exit-code 0 :actions 2 :answer "a" :correct false :missing-count 1 :extra-count 0}
         (run-row {:id "T01" :class :inventory}
                  {:run_id "r" :task "T01" :arm "arm1" :rep 1 :wall_ms 200 :exit_code 0}
                  {:actions 2 :answer "a"} {:correct false :missing ["b"] :extra []}))))
(deftest median-boundaries
  (is (nil? (median [])))
  (is (= 7 (median [7])))
  (is (= 2 (median [100 1 2])))
  (is (= 2.5 (median [4 1 3 2])))
  (is (= 0 (median [0 0 100])))
  (is (= {} (medians []))))
(deftest grouped-medians
  (let [row {:class "c" :arm "arm1" :actions 2 :tool-output-chars 100
             :input-tokens 10 :wall-ms 500 :correct true :exit-code 0 :tree-changed false}
        summary (medians [row (assoc row :actions 4 :correct false)
                          (assoc row :arm "arm2" :actions 9)])]
    (is (= {["c" "arm1"] {:n 2 :correct 1 :invalid 0 :median-actions 3.0 :median-tool-output-chars 100.0
                            :median-input-tokens 10.0 :median-wall-ms 500.0}
            ["c" "arm2"] {:n 1 :correct 1 :invalid 0 :median-actions 9 :median-tool-output-chars 100
                            :median-input-tokens 10 :median-wall-ms 500}} summary))
    (is (= 2 (count (medians [row (assoc row :class "other")]))))
    (doseq [[exit changed invalid] [[0 false 0] [1 false 1] [-1 false 1] [0 true 1] [1 true 1]]]
      (is (= {:n 1 :invalid invalid}
             (select-keys (get (medians [(assoc row :exit-code exit :tree-changed changed)])
                              ["c" "arm1"]) [:n :invalid]))))))
(deftest verdict-gates
  (let [base {:n 2 :median-tool-output-chars 100 :median-actions 3 :correct 2}
        summaries {["c" "arm1"] (assoc base :median-tool-output-chars 200)
                   ["c" "arm2"] base ["c" "arm3"] (assoc base :median-tool-output-chars 70)}
        judge #(verdict % "c" "arm3" ["arm1" "arm2"])]
    (is (= {:promote true :tool-output-cut 30.0 :actions-delta 0
            :correctness-delta 0 :reason "Meets promotion rule"} (judge summaries)))
    (doseq [n [1 3]]
      (is (= {:promote false :tool-output-cut nil :actions-delta nil
              :correctness-delta nil :reason "unequal n"}
             (judge (assoc-in summaries [["c" "arm3"] :n] n)))))
    (doseq [[field value reason] [[:median-tool-output-chars 71 "tool-output cut below threshold"]
                                  [:median-actions 4 "actions increased"]
                                  [:correct 1 "correctness decreased"]]]
      (let [result (judge (assoc-in summaries [["c" "arm3"] field] value))]
        (is (false? (:promote result)))
        (is (= reason (:reason result)))))
    (is (:promote (judge (assoc-in summaries [["c" "arm3"] :correct] 3))))
    (is (= -1 (:actions-delta (judge (assoc-in summaries [["c" "arm3"] :median-actions] 2)))))
    (is (false? (:promote (judge (dissoc summaries ["c" "arm2"]))))))
  (is (false? (:promote (verdict {} "c" "arm3" ["arm1"])))))
;; PR 34 round 1: passing thresholds must not hide invalid or interrupted runs.
(deftest invalid-and-coverage-regressions
  (let [base {:n 2 :invalid 0 :median-tool-output-chars 100 :median-actions 3 :correct 2}
        summaries {["c" "arm1"] (assoc base :median-tool-output-chars 200)
                   ["c" "arm2"] base ["c" "arm3"] (assoc base :median-tool-output-chars 70)}
        expected {["c" "arm1"] 2 ["c" "arm2"] 2 ["c" "arm3"] 2}
        judge #(verdict % "c" "arm3" ["arm1" "arm2"] expected)]
    (is (:promote (judge summaries)))
    (doseq [arm ["arm1" "arm2" "arm3"]]
      (is (= {:promote false :reason "invalid runs present"
              :invalid (assoc {"arm1" 0 "arm2" 0 "arm3" 0} arm 1)}
             (judge (assoc-in summaries [["c" arm] :invalid] 1))))
      (doseq [n [1 3]]
        (let [changed (assoc-in summaries [["c" arm] :n] n)]
          (is (= {:promote false :reason "incomplete coverage" :expected expected
                  :observed (assoc expected ["c" arm] n)} (judge changed)))
          (is (= "unequal n" (:reason (verdict changed "c" "arm3" ["arm1" "arm2"])))))))
    (is (= "incomplete coverage" (:reason (judge {}))))
    (is (= "incomplete coverage"
           (:reason (judge (into {} (map (fn [[k v]] [k (assoc v :n 1)]) summaries))))))
    (let [report (table summaries expected 1)]
      (is (str/includes? report "Incomplete runs: 1"))
      (is (str/includes? report ":reason \"incomplete coverage\""))
      (is (not (str/includes? report ":promote true"))))
    (is (str/includes? (table {} expected 0) "incomplete coverage"))
    (is (str/includes? (table summaries expected 0) ":promote true"))))
(deftest expected-coverage-and-reps
  (is (= {["c" "arm1"] 4 ["c" "arm2"] 4 ["c" "arm3"] 4
          ["d" "arm1"] 2 ["d" "arm2"] 2 ["d" "arm3"] 2}
         (expected-counts [{:class "c"} {:class "c"} {:class "d"}] 2)))
  (is (= {} (expected-counts [] 2)))
  (is (= 2 (parse-reps [])))
  (is (= 3 (parse-reps ["--reps" "3"])))
  (doseq [args [["--reps"] ["--reps" "0"] ["--reps" "-1"] ["--reps" "1.5"]
                ["--reps" "no"] ["--other" "2"] ["--reps" "2" "extra"]]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"N must be positive" (parse-reps args)))))
;; PR 34 round 2: equal class/arm counts hid a missing task behind duplicate runs.
(deftest exact-run-coverage-regression
  (let [tasks [{:id "A" :class "c"} {:id "B" :class "c"}]
        identities [["A" "arm1" 1] ["A" "arm1" 2] ["B" "arm1" 1] ["B" "arm1" 2]
                    ["A" "arm2" 1] ["A" "arm2" 2] ["B" "arm2" 1] ["B" "arm2" 2]
                    ["A" "arm3" 1] ["A" "arm3" 2] ["B" "arm3" 1] ["B" "arm3" 2]]
        rows (mapv (fn [[task arm rep]]
                     {:task task :class "c" :arm arm :rep rep :exit-code 0 :correct true
                      :actions 2 :tool-output-chars (if (= arm "arm3") 50 100)
                      :input-tokens 10 :wall-ms 20}) identities)
        report #(table (medians %) (expected-counts tasks 2) 0
                       (coverage-error % (expected-coverage tasks 2) 2))
        duplicates (mapv #(if (= "B" (:task %)) (assoc % :task "A") %) rows)]
    (is (= (set identities) (expected-coverage tasks 2)))
    (is (= #{} (expected-coverage [] 2)))
    (is (nil? (coverage-error rows (expected-coverage tasks 2) 2)))
    (is (str/includes? (report rows) ":promote true"))
    (is (= (medians rows) (medians duplicates)))
    (doseq [[bad message] [[duplicates "duplicate runs:"]
                           [(pop rows) "incomplete coverage: missing"]
                           [(conj rows (first rows)) "duplicate runs:"]]]
      (is (str/includes? (report bad) message))
      (is (not (str/includes? (report bad) ":promote true"))))
    (is (str/includes? (report duplicates) "incomplete coverage: missing"))
    (is (= "incomplete coverage: missing [[\"B\" \"arm3\" 2]]"
           (:reason (coverage-error (pop rows) (expected-coverage tasks 2) 2))))
    (doseq [rep [0 3 -1 1.5 nil]]
      (let [bad (assoc-in rows [0 :rep] rep)]
        (is (str/includes? (report bad) "rep outside 1..2:"))
        (is (not (str/includes? (report bad) ":promote true")))))))
(deftest zero-baseline
  (is (= "Insufficient control data"
         (:reason (verdict {["c" "a"] {:median-tool-output-chars 0}}
                           "c" "a" ["a"])))))
(deftest renderers
  (is (= "a\tb\nline\\nnext\\tcell\ttrue\n" (rows-tsv [{:a "line\nnext\tcell" :b true}])))
  (is (= "a\n\n" (rows-tsv [{:a nil}]))))
(deftest table-verdicts
  (let [text (table {["c" "arm1"] {:n 1 :correct 1 :invalid 0 :median-actions 2
                                    :median-tool-output-chars 100 :median-input-tokens 9 :median-wall-ms 20}})]
    (is (str/includes? text "| Correct | Invalid | Actions |"))
    (is (str/includes? text "| c | arm1 | 1 | 1 | 0 | 2 | 100 | 9 | 20 |"))
    (is (str/includes? text "c arm3 vs best of arm1/arm2"))
    (is (str/includes? text "c arm2 vs best of arm1"))
    (is (str/includes? text ":promote false"))))
(defn entry-script? [file-prop file] (= file-prop file))
(deftest cli-load-guard
  (is (true? (entry-script? "summarize.clj" "summarize.clj")))
  (is (false? (entry-script? "required.clj" "summarize.clj")))
  (is (false? (entry-script? nil "summarize.clj"))))

(when (entry-script? (System/getProperty "babashka.file") *file*)
  (let [[tasks-file root & options] *command-line-args*]
    (if (= tasks-file "--self-test")
      (let [r (run-tests 'native-control.summarize)]
        (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))
      (let [tasks (into {} (map (juxt :id identity) (edn/read-string (slurp tasks-file))))
            reps (parse-reps options)
            expected (expected-counts (vals tasks) reps)
            dirs (group-by #(every? (fn [f] (fs/exists? (fs/path % f))) ["run.json" "events.jsonl"])
                           (filter fs/directory? (fs/list-dir (str root "/runs"))))
            incomplete (count (get dirs false))
            rows (for [dir (sort (get dirs true))
                       :let [run (json/parse-string (slurp (str (fs/path dir "run.json"))) true)
                             task (or (tasks (:task run)) (throw (ex-info "Unknown task" run)))
                             metrics (run-metrics (slurp (str (fs/path dir "events.jsonl"))))
                             metrics (if (fs/exists? (fs/path dir "answer.txt"))
                                       (assoc metrics :answer (slurp (str (fs/path dir "answer.txt")))) metrics)]]
                   (run-row task run metrics (score task (:answer metrics))))
            report (table (medians rows) expected incomplete
                          (coverage-error rows (expected-coverage (vals tasks) reps) reps))]
        (spit (str root "/rows.tsv") (rows-tsv rows))
        (spit (str root "/table.md") report)
        (print report)))))
