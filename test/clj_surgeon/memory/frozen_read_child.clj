(ns clj-surgeon.memory.frozen-read-child
  "The frozen read exactly as the alias-migration verb performs it today.

   Every scoped source is slurped into ONE realised vector that stays reachable
   for the whole call because the transaction needs it as its pre-image, and
   each retained source is then parsed. This is the defect the transaction
   journal exists to remove; the namespace is a faithful reproduction, not a
   strawman, and it must not be repaired."
  (:require
   [clj-surgeon.memory.fixture :as fixture]
   [clojure.string :as str]
   [clj-surgeon.memory.heap :as heap]
   [clj-surgeon.memory.scenario :as scenario])
  (:gen-class))

(defn frozen-read
  "Slurp every file in scope into one retained vector, then parse each."
  [root]
  (let [files (fixture/scope-files root)
        ;; the frozen read: every source retained at once
        sources (mapv (fn [path] {:file path :source (slurp path)}) files)
        ;; every plan retained at once, against those retained sources
        plans (mapv (fn [{:keys [file source]}] (scenario/plan-file file source))
                    sources)]
    {:files (count sources)
     :bytes (reduce + 0 (map #(count (.getBytes ^String (:source %) "UTF-8")) sources))
     :result-hash-count (count plans)
     ;; the reference tree hash: one digest over every result hash in path
     ;; order, so any arm can be compared to this one by a single value
     :tree-hash (scenario/sha256 (str/join "\n" (map :result-hash plans)))}))

(defn -main
  [root & _]
  (let [{:keys [result memory]} (heap/measure #(frozen-read root))]
    (heap/emit-receipt! {:arm :frozen-read
                         :files (:files result)
                         :bytes (:bytes result)
                         :result-hash-count (:result-hash-count result)
                         :tree-hash (:tree-hash result)
                         :memory memory})
    (shutdown-agents)
    (System/exit 0)))
