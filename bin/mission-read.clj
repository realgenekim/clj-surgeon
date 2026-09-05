#!/usr/bin/env bb
;; The mission ledger's READ path, under babashka.
;;
;; WHY THIS FILE EXISTS, measured on this box: every verb through the JVM
;; entrance costs ~6 s of JVM start and namespace loading before it does its
;; work, and `show`, `list` and `ready` do 1.0, 1.4 and 1.7 ms of work. That is
;; ~6,000x overhead on precisely the three verbs an agent calls most, and it is
;; overhead the ledger does not need: reading a mission touches ONLY
;; `clj-surgeon.mission`, which is why that namespace was kept babashka-safe.
;;
;; Writes are NOT here. open/plan/apply/resume/undo reach a planner and a
;; guarded transaction kernel and stay on the JVM.
(require '[clj-surgeon.mission :as mission]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(defn- parse-flags
  [args]
  (loop [[a b & more :as remaining] args acc {} positional []]
    (cond
      (empty? remaining) [acc positional]
      (str/starts-with? (str a) "--") (recur more (assoc acc (keyword (subs a 2)) b) positional)
      :else (recur (rest remaining) acc (conj positional a)))))

(let [[verb & args] *command-line-args*
      [flags positional] (parse-flags args)
      state-dir (mission/workspace-state-dir (:workspace flags) (:state-home flags))
      missions (mission/read-all state-dir)]
  (case verb
    "show" (let [m (mission/read-mission state-dir (first positional))]
             (pp/pprint (if (mission/refused? m)
                          m
                          (mission/show-view missions (first positional)))))
    "list" (pp/pprint {:ok true :operation "mission"
                       :ledger (mission/missions-dir state-dir)
                       :count (count missions)
                       :index (mission/index-lines missions)})
    ("ready" "blocked") (pp/pprint {:ok true :operation "mission"
                                    :ready (mission/ready-missions missions)
                                    :waiting (mission/waiting-missions missions)})
    (do (println "read verbs: show <id> | list | ready|blocked  (--workspace R [--state-home H])")
        (System/exit 2))))
