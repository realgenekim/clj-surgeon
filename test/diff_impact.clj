#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.set :as set]
         '[clojure.string :as str])

;; Read declarations, never require the subject or consult lane selection.
(defn declaration [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (let [form (read {:eof nil :read-cond :allow :features #{:clj}} r)]
        (when (and (seq? form) (= 'ns (first form))) form)))))

(defn libspecs [spec]
  (cond
    (symbol? spec) #{spec}
    (and (sequential? spec) (symbol? (first spec)))
    (if (or (empty? (rest spec)) (keyword? (second spec)))
      #{(first spec)}
      (into #{} (mapcat (fn [child]
                          (map #(symbol (str (first spec) "." %)) (libspecs child))))
            (rest spec)))
    :else (throw (ex-info "Unsupported namespace libspec" {:spec spec}))))

(defn dependencies [form]
  (into #{} (mapcat #(mapcat libspecs (rest %)))
        (filter #(and (seq? %) (#{:require :use} (first %))) (drop 2 form))))

(defn impact [nodes changed]
  (let [by-name (into {} (map (juxt :namespace identity)) nodes)]
    (->> nodes
         (filter :test?)
         (keep (fn [{:keys [requires] :as node}]
                 (let [paths (vec (sort
                                    (concat
                                      (for [n (set/intersection requires changed)] [(str n)])
                                      (for [via requires
                                            n (set/intersection (get-in by-name [via :requires] #{}) changed)]
                                        [(str via) (str n)]))))]
                   (when (seq paths) (assoc node :paths paths)))))
         (sort-by #(vector (not= 'clj-surgeon.txn-journal-test (:namespace %))
                     (str (:namespace %)))) vec)))

(defn command! [args]
  (:out (process/check @(process/process (vec args) {:out :string :err :string}))))

(defn completed? [process-exit counters]
  (and (= 0 process-exit) (pos-int? (:test counters))
       (= 0 (:fail counters) (:error counters))))

(when-not (= ["--self-test"] *command-line-args*)
  (let [[base output phase] *command-line-args*]
    (when-not (and base output (#{"before" "after" "merged" "list"} phase))
      (throw (ex-info "Usage: bb test/diff_impact.clj BASE OUTPUT_DIR before|after|merged|list" {})))
    (let [files (sort (map str (mapcat #(fs/glob % "**.{clj,cljc}") ["src" "test"])))
          nodes (vec (keep (fn [file]
                             (when-let [form (declaration file)]
                               {:file file :namespace (second form)
                                :lane (or (:lane (meta (second form)))
                                          (:lane (first (filter map? (drop 2 form)))))
                                :test? (and (str/starts-with? file "test/")
                                            (str/ends-with? (str (second form)) "-test"))
                                :requires (dependencies form)})) files))
          changed-files (set (str/split-lines (command! ["git" "diff" "--name-only" base "--" "src"])))
          changed (set (map :namespace (filter #(changed-files (:file %)) nodes)))
          selected (impact nodes changed)
          inventory {:base base :head (str/trim (command! ["git" "rev-parse" "HEAD"]))
                     :changed-files (sort changed-files) :namespaces selected}]
      (fs/create-dirs output)
      (spit (str output "/impact-" phase ".edn") (pr-str inventory))
      (spit (str output "/impact-" phase ".json") (json/generate-string inventory {:pretty true}))
      (println "Selected" (count selected) "test namespaces across all lanes")
      (when-not (= phase "list")
        (doseq [{n :namespace} selected]
          (let [log (str output "/" phase "/" n ".log")
                receipt (str output "/" phase "/" n ".edn")
                code (str "(require '[clojure.test :as t]) "
                          "(let [r (try (require '" n ") (t/run-tests '" n ") "
                          "(catch Throwable e (.printStackTrace e) {:test 0 :pass 0 :fail 0 :error 1}))] "
                          "(spit " (pr-str receipt) " (pr-str r)) "
                          "(shutdown-agents) (System/exit (if (and (pos? (:test r)) "
                          "(zero? (+ (:fail r) (:error r)))) 0 1)))")
                _ (fs/create-dirs (fs/parent log))
                _ (when (fs/exists? log) (throw (ex-info "Never overwrite a run" {:log log})))
                start (System/nanoTime)
                result (with-open [out (io/writer log)]
                         @(process/process ["clojure" "-J-Xmx1g" "-M:clj-surgeon/test-deps" "-e" code]
                                           {:out out :err :out}))
                counters (when (fs/exists? receipt)
                           (try (read-string (slurp receipt)) (catch Exception _ nil)))
                observation {:namespace n :exit (if (completed? (:exit result) counters) 0 1)
                             :process-exit (:exit result) :counters counters
                             :wall-ms (quot (- (System/nanoTime) start) 1000000)
                             :log log :receipt receipt}]
            (spit (str output "/results-" phase ".edn") (str (pr-str observation) "\n") :append true)
            (println (pr-str observation))
            (flush)))
        (let [results (str/split-lines (slurp (str output "/results-" phase ".edn")))]
          (System/exit (if (every? #(zero? (:exit (read-string %))) results) 0 1)))))))

(when (= ["--self-test"] *command-line-args*)
  (let [nodes [{:namespace 'source :requires #{}}
               {:namespace 'middle :requires #{'source}}
               {:namespace 'far :requires #{'middle}}
               {:namespace 'fast-test :requires #{'source} :test? true :lane :fast}
               {:namespace 'battery-test :requires #{'middle 'source} :test? true :lane :battery}
               {:namespace 'bb-excluded-test :requires #{'middle} :test? true}
               {:namespace 'too-far-test :requires #{'far} :test? true}
               {:namespace 'unrelated-test :requires #{} :test? true}]
        selected (impact nodes #{'source})]
    (assert (= #{'fast-test 'battery-test 'bb-excluded-test} (set (map :namespace selected))))
    (assert (= 3 (count selected)))
    (assert (= #{["middle" "source"] ["source"]} (set (:paths (first selected)))))
    (assert (empty? (impact nodes #{})))
    (assert (= #{'a.x 'a.y} (libspecs '(a [x :as x] y))))
    (assert (= #{'x 'y} (dependencies '(ns test (:require [x :as a]) (:use y))))))
  (assert (completed? 0 {:test 1 :pass 1 :fail 0 :error 0}))
  (doseq [[exit counters] [[0 nil] [1 {:test 1 :fail 0 :error 0}]
                           [0 {:test 0 :fail 0 :error 0}] [0 {:test 1 :fail 1 :error 0}]]]
    (assert (not (completed? exit counters))))
  (println "Diff-impact self-check: direct, transitive, lane-independent, unique, bounded, prefix and use cases passed"))
