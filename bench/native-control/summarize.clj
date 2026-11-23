#!/usr/bin/env bb
(ns native-control.summarize
  (:require [cheshire.core :as json] [clojure.string :as str] [clojure.set :as set]
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
(defn entry-script? [file-prop file] (= file-prop file))
(deftest cli-load-guard
  (is (true? (entry-script? "summarize.clj" "summarize.clj")))
  (is (false? (entry-script? "required.clj" "summarize.clj")))
  (is (false? (entry-script? nil "summarize.clj"))))

(when (entry-script? (System/getProperty "babashka.file") *file*)
  (let [[tasks-file] *command-line-args*]
    (if (= tasks-file "--self-test")
      (let [r (run-tests 'native-control.summarize)]
        (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))
      (do
        (binding [*out* *err*]
          (println (str "summarize: this slice carries the metrics and scoring only; "
                        "the summary CLI lands in the next slice")))
        (System/exit 2)))))
