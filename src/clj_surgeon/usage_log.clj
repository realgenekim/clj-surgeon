(ns clj-surgeon.usage-log
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]))

(defn usage-line [{:keys [op outcome code ms repo agent at]}]
  (json/generate-string (array-map :at at :op (name op) :outcome outcome
                          :code (some-> code name) :ms (long ms) :repo repo :agent agent)))
(defn outcome-of
  "A thrown call is an error; a result map carrying :error or :error-type is a refusal (the CLI exits 1 on
   either, and many handlers return {:error ...} without a type); anything else is ok."
  [result]
  (let [m (if (map? result) result {})]
    {:outcome (cond (:exception m) "error" (or (contains? m :error) (contains? m :error-type)) "refused" :else "ok")
     :code (some-> (:error-type m) name)}))
(defn agent-kind
  "An empty variable counts as unset."
  [env]
  (let [set? #(not-empty (get env %))]
    (or (set? "CLJ_SURGEON_AGENT")
        (when (some set? ["CODEX_HOME" "CODEX_SANDBOX" "CODEX_SANDBOX_NETWORK_DISABLED"]) "codex")
        (when (some set? ["CLAUDECODE" "CLAUDE_CODE_ENTRYPOINT"]) "claude"))))
(defn log-path
  "CLJ_SURGEON_USAGE_LOG wins (\"off\" disables; the Makefile test recipes set it so suite calls, in-process and
   subprocess alike, never reach the real log); without HOME there is no default path and nothing is written."
  [env]
  (or (not-empty (get env "CLJ_SURGEON_USAGE_LOG"))
      (when-let [home (not-empty (get env "HOME"))] (str home "/.local/state/clj-surgeon/usage.jsonl"))))
(defn repo-name [cwd]
  (when cwd
    (let [root (try (let [{:keys [exit out]} (sh/sh "git" "rev-parse" "--show-toplevel" :dir cwd)]
                      (when (zero? exit) (str/trim out))) (catch Throwable _ nil))]
      (try (.getName (io/file (or root cwd))) (catch Throwable _ nil)))))
(defn append! [path line]
  (try (when (and path (not= "off" path))
         (io/make-parents path) (spit path (str line "\n") :append true))
       (catch Throwable _ nil)) nil)
(defn record-call [op f]
  (let [start (System/nanoTime)
        result (try {:value (f)} (catch Throwable e {:thrown e}))]
    (try
      (let [env (into {} (System/getenv)) e (:thrown result)]
        (append! (log-path env)
                 (usage-line (merge (outcome-of (if e {:exception true :error-type (or (:error-type (ex-data e)) :exception)} (:value result)))
                                    {:op op :at (str (java.time.Instant/now)) :ms (quot (- (System/nanoTime) start) 1000000)
                                     :repo (repo-name (System/getProperty "user.dir")) :agent (agent-kind env)}))))
      (catch Throwable _ nil))
    (if-let [e (:thrown result)] (throw e) (:value result))))
