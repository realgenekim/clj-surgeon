#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clj-surgeon.form-identity :as form-identity]
         '[clojure.string :as str])

;; Read declarations, never require the subject or consult lane selection.
(defn declaration [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (let [form (read {:eof nil :read-cond :allow :features #{:clj}} r)]
        (when (and (seq? form) (= 'ns (first form))) form)))))

(defn dependencies [form]
  (into #{} (map (comp symbol :lib))
        (form-identity/source-require-entries
          (if (string? form) form (pr-str form))
          {:platform :clj :clauses #{":require" ":require-macros" ":use"}
           :on-unparsed #(throw (ex-info "Unsupported namespace dependency" {:form %}))})))

(defn reverse-closure
  "Fixed point over reverse edges. Each reachable node retains one shortest
  path to the seed; sorted neighbors make equally short witnesses deterministic.
  Visiting each node once terminates even for cycles, without a depth bound."
  [dependents seed]
  (loop [paths {seed [seed]} frontier [seed]]
    (if (empty? frontier)
      paths
      (let [[next-paths next-frontier]
            (reduce (fn [[seen pending] n]
                      (reduce (fn [[seen pending] dependent]
                                (if (contains? seen dependent)
                                  [seen pending]
                                  [(assoc seen dependent (into [dependent] (get seen n)))
                                   (conj pending dependent)]))
                              [seen pending] (sort (get dependents n))))
                    [paths []] frontier)]
        (recur next-paths next-frontier)))))

(defn impact [nodes changed]
  (let [dependents (reduce (fn [graph {:keys [namespace requires]}]
                             (reduce #(update %1 %2 (fnil conj #{}) namespace) graph requires))
                           {} nodes)
        closures (mapv #(reverse-closure dependents %) (sort changed))]
    (->> nodes
         (filter :test?)
         (keep (fn [{:keys [namespace] :as node}]
                 (let [paths (vec (sort (keep #(when-let [path (get % namespace)]
                                                 (mapv str (rest path))) closures)))]
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
    (when-not (and base output (#{"before" "after" "merged" "fixed-point" "list"} phase))
      (throw (ex-info "Usage: bb test/diff_impact.clj BASE OUTPUT_DIR before|after|merged|fixed-point|list" {})))
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
  ;; Sol F1, 2026-09-13: generate the dependency-depth class, then mutate
  ;; every edge. Fixture declarations are data; no fixture namespace is loaded.
  (doseq [depth (range 1 9)]
    (let [names (mapv #(symbol (str "fixture.n" %)) (range (inc depth)))
          leaf (peek names)
          nodes (mapv (fn [i n]
                        {:namespace n :test? (= n leaf)
                         :requires (dependencies
                                     (if (zero? i) (list 'ns n)
                                       (list 'ns n (list :require [(names (dec i)) :as 'dep]))))})
                      (range) names)
          nodes (conj nodes {:namespace 'unrelated-test :test? true :requires #{}})
          selected (set (map :namespace (impact nodes #{(first names)})))]
      (println "Chain depth" depth "selected" selected)
      (assert (= #{leaf} selected) (str "Every leaf selected at depth " depth))
      (doseq [edge (range 1 (inc depth))]
        (assert (empty? (impact (assoc-in nodes [edge :requires] #{}) #{(first names)}))
                (str "Cut edge " edge " disconnects depth " depth)))))
  (let [nodes [{:namespace 'source :requires #{}}
               {:namespace 'middle :requires #{'source}}
               {:namespace 'far :requires #{'middle}}
               {:namespace 'fast-test :requires #{'source} :test? true :lane :fast}
               {:namespace 'battery-test :requires #{'middle 'source} :test? true :lane :battery}
               {:namespace 'bb-excluded-test :requires #{'middle} :test? true}
               {:namespace 'too-far-test :requires #{'far} :test? true}
               {:namespace 'unrelated-test :requires #{} :test? true}]
        selected (impact nodes #{'source})]
    (assert (= #{'fast-test 'battery-test 'bb-excluded-test 'too-far-test} (set (map :namespace selected))))
    (assert (= 4 (count selected)))
    (assert (= [["source"]] (:paths (first selected))))
    (assert (empty? (impact nodes #{})))
    (assert (= #{'a.x 'a.y} (dependencies '(ns test (:require (a [x :as x] y))))))
    (assert (= #{'x 'y} (dependencies '(ns test (:require [x :as a]) (:use y))))))
  (assert (completed? 0 {:test 1 :pass 1 :fail 0 :error 0}))
  (doseq [[exit counters] [[0 nil] [1 {:test 1 :fail 0 :error 0}]
                           [0 {:test 0 :fail 0 :error 0}] [0 {:test 1 :fail 1 :error 0}]]]
    (assert (not (completed? exit counters))))
  (let [nodes [{:namespace 'changed :requires #{'cycle}}
               {:namespace 'cycle :requires #{'changed}}
               {:namespace 'helper-test :test? true :requires #{'cycle 'other}}
               {:namespace 'leaf-test :test? true :requires #{'helper-test}}]
        selected (impact nodes #{'changed 'other})]
    (assert (= #{'helper-test 'leaf-test} (set (map :namespace selected))))
    (assert (= #{["helper-test" "cycle" "changed"] ["helper-test" "other"]}
               (set (:paths (second selected)))))
    (assert (= selected (impact (reverse nodes) #{'other 'changed}))))
  (doseq [clause [":require" ":use" ":require-macros"]
          spec ["fixture.n0" "[fixture.n0]" "(fixture.n0)"
                "[fixture.n0 :as dep]" "(fixture.n0 :refer [v])"
                "[fixture [n0 :as dep]]" "(fixture n0)"
                "[fixture (n0)]" "^:fixture [fixture.n0 :as-alias dep]"
                "\"fixture.n0\"" "[\"fixture.n0\" :as dep]"
                "#?(:clj [fixture.n0] :cljs [unrelated])"
                "#?@(:clj [[fixture.n0]] :cljs [[unrelated]])"
                "#_[unrelated] [fixture.n0] :reload"]]
    (let [deps (dependencies (str "(ns fixture.leaf (" clause " " spec "))"))]
      (assert (= #{'fixture.n0} deps) (str clause " " spec))
      (assert (= ['fixture.leaf]
                 (mapv :namespace (impact [{:namespace 'fixture.leaf :test? true :requires deps}]
                                    #{'fixture.n0}))))))
  ;; The exact path in Sol's fence report, with both intermediate namespaces.
  (assert (= [["clj-surgeon.mcp-prepared-confirmation" "clj-surgeon.mcp-contract"
               "clj-surgeon.mcp-extraction"]]
             (:paths (first (impact
                              (mapv (fn [[n dep]] {:namespace n :requires #{dep}
                                                   :test? (= n 'clj-surgeon.mcp-prepared-wire-test)})
                                    '[[clj-surgeon.mcp-prepared-wire-test clj-surgeon.mcp-prepared-confirmation]
                                      [clj-surgeon.mcp-prepared-confirmation clj-surgeon.mcp-contract]
                                      [clj-surgeon.mcp-contract clj-surgeon.mcp-extraction]])
                              #{'clj-surgeon.mcp-extraction})))))
  (println "Diff-impact self-check: depths 1..8, every edge cut, unrelated negative, cycles, multiple seeds, test intermediates, shared libspec shapes, Sol F1 and completion checks passed"))
