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
;; Source writes are NOT here. The fallback verb only appends an explicit report.
;; open/plan/apply/resume/undo reach a planner and a
;; guarded transaction kernel and stay on the JVM.
(require '[clj-surgeon.mission :as mission]
         '[clj-surgeon.mission-display :as display]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(defn- parse-flags
  "The same order-independent, boolean-aware parse the JVM entrance uses: a
   global option may come before or after the verb, and a valueless flag does
   not swallow the next token."
  [args]
  (loop [[a b & more :as remaining] args acc {} positional []]
    (cond
      (empty? remaining) [acc positional]
      (str/starts-with? (str a) "--")
      (if (or (nil? b) (str/starts-with? (str b) "--"))
        (recur (rest remaining) (assoc acc (keyword (subs a 2)) true) positional)
        (recur more (assoc acc (keyword (subs a 2)) b) positional))
      :else (recur (rest remaining) acc (conj positional a)))))

(let [[flags positional] (parse-flags *command-line-args*)
      verb (first positional)
      args (rest positional)
      ;; @bb-help: help needs NO ledger, so the state dir is not computed for it
      state-dir (delay (mission/workspace-state-dir (:workspace flags)
                                                    (:state-home flags)))
      missions (delay (mission/read-all @state-dir))]
  (when (and (contains? #{"show" "list" "ready" "blocked"} verb)
             (not (and (string? (:workspace flags)) (seq (:workspace flags)))))
    (pp/pprint display/workspace-required)
    (System/exit 1))
  (case verb
    ("help" nil) (print (mission/help-text (first args)))

    "fallback" (let [opts (assoc flags :id (first args))
                     result ((requiring-resolve 'clj-surgeon.mission-fallback/report!) opts)]
                 (pp/pprint (display/with-recovery result opts))
                 (when (false? (:ok result)) (System/exit 1)))

    "show" (let [m (mission/read-mission @state-dir (first args))
                 result (display/show-result
                          (if (mission/refused? m)
                            m
                            (assoc (mission/show-view @missions (first args))
                                   :config_sources
                                   (mission/config-sources (:workspace flags) (:config flags))))
                          (assoc flags :id (first args)))]
             (pp/pprint result)
             (when (false? (:ok result)) (System/exit 1)))

    "list" (pp/pprint {:ok true :operation "mission"
                       :ledger (mission/missions-dir @state-dir)
                       :count (count @missions)
                       :index (mission/index-lines @missions)})

    ("ready" "blocked") (pp/pprint {:ok true :operation "mission"
                                    :ready (mission/ready-missions @missions)
                                    :waiting (mission/waiting-missions @missions)})

    ;; @bb-help. An unknown verb prints the SAME help the JVM entrance would and
    ;; exits 2 — never 0, which the first probe read as "it ran".
    (do (binding [*out* *err*]
          (println (str "bin/mission: no verb named " (pr-str verb) ".\n")))
        (print (mission/help-text nil))
        (System/exit 2))))
