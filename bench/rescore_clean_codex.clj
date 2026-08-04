#!/usr/bin/env bb

(ns rescore-clean-codex
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]))

(defn read-table [path]
  (let [[header & rows] (str/split-lines (slurp path))
        fields (str/split header #"\t")]
    {:fields fields
     :rows (mapv #(zipmap fields (str/split % #"\t" -1)) rows)}))

(defn bool-string [value]
  (if value "true" "false"))

(defn successful-command? [command]
  (zero? (or (get command "exit_code") 1)))

(defn command-has? [command fragment]
  (str/includes? (get command "command" "") fragment))

(defn structural-correct? [final]
  (let [normalize #(str/replace % #"\s+" " ")
        text (normalize final)
        first-match (normalize "(select-keys state [:status :attempts])")
        second-match (normalize "(select-keys\n\n   job\n\n   [:status :attempts])")]
    (and (str/includes? text first-match)
         (str/includes? text second-match)
         (re-find #"(?i)(found|match count)[^0-9]{0,40}2" text))))

(defn structural-exact? [final]
  (and (str/includes? final "(select-keys state [:status :attempts])")
       (str/includes? final "(select-keys\n\n   job\n\n   [:status :attempts])")
       (re-find #"(?i)(found|match count)[^0-9]{0,40}2" final)))

(defn rescore-row [result-dir row]
  (let [run-dir (str result-dir "/" (get row "run_id"))
        commands (json/parse-string (slurp (str run-dir "/commands.json")))
        command-texts (mapv #(get % "command" "") commands)
        task (get row "task")
        target (case task
                 ("named-form" "semantic-form") "src/clj_surgeon/core.clj"
                 "structural-find" "src/bench/structural.clj"
                 "case-edit" "src/bench/state.clj")
        final (slurp (str run-dir "/final.txt"))
        old-correct (= "true" (get row "correct"))
        exact-correct (if (= "structural-find" task)
                        (boolean (structural-exact? final))
                        old-correct)
        correct (if (= "structural-find" task)
                  (boolean (structural-correct? final))
                  old-correct)
        plan-indexes (keep-indexed
                       (fn [index command]
                         (when (and (successful-command? command)
                                    (command-has? command "clj-surgeon")
                                    (command-has? command "replace-subform")
                                    (not (command-has? command "replace-subform!"))
                                    (not (command-has? command "--help")))
                           index))
                       commands)
        apply-indexes (keep-indexed
                        (fn [index command]
                          (when (and (successful-command? command)
                                     (command-has? command "clj-surgeon")
                                     (command-has? command "replace-subform!")
                                     (not (command-has? command "--help")))
                            index))
                        commands)
        first-apply (first apply-indexes)
        apply-output (when first-apply
                       (get (nth commands first-apply) "aggregated_output" ""))
        verified-receipt? (and first-apply
                               (successful-command? (nth commands first-apply))
                               (str/includes? apply-output ":verified")
                               (str/includes? apply-output ":whole-file-parsed true")
                               (str/includes? apply-output ":read-back-hash"))
        verified (boolean
                   (and first-apply
                        (or verified-receipt?
                            (some #(str/includes? % target)
                                  (subvec command-texts (inc first-apply))))))]
    (assoc row
           "clj_invocations" (str (count (filter #(str/includes? % "clj-surgeon ")
                                           command-texts)))
           "show_form" (bool-string (some #(and (str/includes? % "clj-surgeon")
                                             (or (str/includes? % "show-form")
                                                 (re-find #":cat([^a-zA-Z]|$)" %)))
                                      command-texts))
           "grep_form" (bool-string (some #(and (str/includes? % "clj-surgeon")
                                             (str/includes? % "grep-form"))
                                      command-texts))
           "ls_used" (bool-string (some #(and (str/includes? % "clj-surgeon")
                                           (re-find #":ls([^a-zA-Z]|$)" %))
                                    command-texts))
           "plan_generated" (bool-string (seq plan-indexes))
           "plan_applied" (bool-string (seq apply-indexes))
           "plan_apply_separate" (bool-string (and (seq plan-indexes)
                                                (seq apply-indexes)))
           "verified" (bool-string verified)
           "exact_correct" (bool-string exact-correct)
           "correct" (bool-string correct))))

(defn emit-table [{:keys [fields rows]}]
  (let [fields (if (some #{"exact_correct"} fields)
                 fields
                 (vec (mapcat #(if (= "correct" %) ["exact_correct" "correct"] [%])
                              fields)))]
    (println (str/join "\t" fields))
    (doseq [row rows]
      (println (str/join "\t" (map #(get row % "") fields))))))

(let [[result-dir] *command-line-args*]
  (when-not result-dir
    (binding [*out* *err*]
      (println "Usage: bb bench/rescore_clean_codex.clj RESULT_DIR"))
    (System/exit 2))
  (let [table (read-table (str result-dir "/runs.tsv"))]
    (emit-table (update table :rows #(mapv (partial rescore-row result-dir) %)))))
