#!/usr/bin/env bb
(ns native-control.schedule
  (:require [clojure.edn :as edn] [clojure.string :as str]
            [cheshire.core :as json] [clojure.test :refer [deftest is run-tests]]))

(defn schedule [tasks arms reps seed]
  (let [rng (java.util.Random. (long seed))]
    (loop [v (vec (for [task tasks arm arms rep (range 1 (inc reps))
                       :let [id (if (map? task) (:id task) task)]]
                   {:run-id (str id "-" arm "-r" rep) :task id :arm arm :rep rep}))
           i (dec (* (count tasks) (count arms) reps))]
      (if (pos? i)
        (let [j (.nextInt rng (inc i))]
          (recur (assoc v i (v j) j (v i)) (dec i))) v))))
(defn task-prompt [task] (str (:prompt task) "\n\n" (:answer-format task)))
(defn find-task [tasks id]
  (or (some #(when (= id (:id %)) %) tasks)
      (throw (ex-info "Unknown task" {:id id}))))
(defn task-field [task k]
  (if (#{"arm1" "arm2" "arm3" "arm4"} k) (str "arms/" k ".md")
      (or (get task (keyword k)) (throw (ex-info "Missing task field" {:key k})))))
(defn keep-workspace? [run-id keep]
  (when-not (re-matches #"[A-Za-z0-9_-]+" run-id)
    (throw (ex-info "Invalid run ID" {:run-id run-id})))
  (= "1" keep))
(defn receipt [[id task arm rep model reasoning repo sha start end code status bytes began ended]]
  {:run_id id :task task :arm arm :rep (parse-long rep) :model model :reasoning reasoning
   :repo repo :sha sha :wall_ms (- (parse-long end) (parse-long start))
   :exit_code (parse-long code) :tree_changed (not (str/blank? status))
   :arm_text_chars (parse-long (str/trim bytes)) :started_at began :ended_at ended})
(defn wrapper [arm config]
  (when-not (#{"arm1" "arm2" "arm3" "arm4"} arm) (throw (ex-info "Unknown arm" {:arm arm})))
  (if (= arm "arm1") ""
      (str "#!/bin/bash\nexec bb --config '" (str/replace config "'" "'\\''")
           "' -m clj-surgeon.core \"$@\"\n")))

(deftest schedule-matrix
  (let [tasks ["T01" "T02" "T03" "T04" "T05" "T06"
               "T07" "T08" "T09" "T10" "T11" "T12"]
        arms ["arm1" "arm2" "arm3"]
        rows (schedule tasks arms 2 42)]
    (is (= 72 (count rows)))
    (is (= 72 (count (set rows))))
    (is (= 72 (count (set (map :run-id rows)))))
    (is (= rows (schedule tasks arms 2 42)))
    (is (not= rows (schedule tasks arms 2 43)))
    (is (= (zipmap tasks (repeat 6)) (frequencies (map :task rows))))
    (is (= {"arm1" 24 "arm2" 24 "arm3" 24} (frequencies (map :arm rows))))
    (is (= {1 36 2 36} (frequencies (map :rep rows))))))
(deftest schedule-boundaries
  (is (= [] (schedule [] ["arm1"] 1 0)))
  (is (= [] (schedule ["T01"] [] 1 0)))
  (is (= [] (schedule ["T01"] ["arm1"] 0 0)))
  (is (= [{:run-id "T01-arm2-r1" :task "T01" :arm "arm2" :rep 1}]
         (schedule [{:id "T01"}] ["arm2"] 1 5))))
(deftest prompt-and-fields
  (is (= "Find x.\n\nOne line." (task-prompt {:prompt "Find x." :answer-format "One line."})))
  (is (= "\n\n" (task-prompt {:prompt "" :answer-format ""})))
  (is (= {:id "T01"} (find-task [{:id "T01"}] "T01")))
  (is (thrown? Exception (find-task [] "missing")))
  (is (= "/repo" (task-field {:repo "/repo"} "repo")))
  (is (= "abc" (task-field {:sha "abc"} "sha")))
  (is (= :inventory (task-field {:class :inventory} "class")))
  (is (thrown? Exception (task-field {} "missing")))
  (is (= "arms/arm3.md" (task-field {} "arm3")))
  (is (= "arms/arm4.md" (task-field {} "arm4"))))
(deftest retention-and-paths
  (is (true? (keep-workspace? "T01-arm1-r1" "1")))
  (is (false? (keep-workspace? "T01-arm1-r1" "0")))
  (is (false? (keep-workspace? "T01-arm1-r1" nil)))
  (is (false? (keep-workspace? "T01-arm1-r1" "true")))
  (doseq [id ["" "." ".." "../x" "/x" "x/y" "a b" "x\ny"]]
    (is (thrown? Exception (keep-workspace? id "0")))))
(deftest process-data
  (is (= "" (wrapper "arm1" "/repo/bb.edn")))
  (is (= "#!/bin/bash\nexec bb --config '/a b/bb.edn' -m clj-surgeon.core \"$@\"\n"
         (wrapper "arm2" "/a b/bb.edn")))
  (is (str/includes? (wrapper "arm3" "/a'b") "'/a'\\''b'"))
  (is (str/includes? (wrapper "arm4" "/bb.edn") "clj-surgeon.core"))
  (is (thrown? Exception (wrapper "oops" "/bb.edn")))
  (let [args ["T01-arm1-r1" "T01" "arm1" "1" "model" "medium" "/repo" "abc"
              "100" "350" "124" "" " 42\n" "start" "end"]]
    (is (= {:run_id "T01-arm1-r1" :task "T01" :arm "arm1" :rep 1
            :model "model" :reasoning "medium" :repo "/repo" :sha "abc"
            :wall_ms 250 :exit_code 124 :tree_changed false :arm_text_chars 42
            :started_at "start" :ended_at "end"} (receipt args)))
    (is (:tree_changed (receipt (assoc args 11 "?? new"))))
    (is (false? (:tree_changed (receipt (assoc args 11 "\n")))))))

(let [[cmd a b c] *command-line-args*]
  (case cmd
    "--self-test" (let [r (run-tests 'native-control.schedule)]
                    (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))
    "keep" (println (keep-workspace? a b))
    "receipt" (println (json/generate-string (receipt (rest *command-line-args*))))
    "wrapper" (print (wrapper a b))
    (let [tasks (edn/read-string (slurp (if (#{"prompt" "task"} cmd) a cmd)))]
      (case cmd
        "prompt" (println (task-prompt (find-task tasks b)))
        "task" (println (task-field (find-task tasks b) c))
        (do (println "run_id\ttask\tarm\trep")
            (doseq [row (schedule tasks (str/split a #",") (parse-long b) (parse-long c))]
              (println (str/join "\t" ((juxt :run-id :task :arm :rep) row)))))))))
