(ns clj-surgeon.usage-log
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]))

(defn usage-line [{:keys [op outcome code ms repo agent at]}]
  (json/generate-string (array-map :at at :op (name op) :outcome outcome
                          :code (some-> code name) :ms (long ms) :repo repo :agent agent)))
(defn outcome-of
  "A thrown call is an error; a result map carrying :error or :error-type is a refusal (the CLI exits 1 on :error;
   a typed refusal without :error still names its code; many handlers return {:error ...} without a type); anything
   else is ok."
  [result]
  (let [m (if (map? result) result {})]
    {:outcome (cond (:exception m) "error" (or (some? (:error m)) (some? (:error-type m))) "refused" :else "ok")
     :code (cond (keyword? (:error-type m)) (name (:error-type m))
                 (:exception m) "exception")}))
(defn agent-kind
  "An empty variable counts as unset."
  [env]
  (let [set? #(not-empty (get env %))]
    (or (set? "CLJ_SURGEON_AGENT")
        (when (some set? ["CODEX_HOME" "CODEX_SANDBOX" "CODEX_SANDBOX_NETWORK_DISABLED"]) "codex")
        (when (some set? ["CLAUDECODE" "CLAUDE_CODE_ENTRYPOINT"]) "claude"))))
(defn log-path
  "Return nonempty CLJ_SURGEON_USAGE_LOG (including \"off\"), else $XDG_STATE_HOME/clj-surgeon/usage.jsonl,
   else $HOME/.local/state/clj-surgeon/usage.jsonl. Return nil when all three variables are absent or empty."
  [env]
  (or (not-empty (get env "CLJ_SURGEON_USAGE_LOG"))
      (when-let [state (not-empty (get env "XDG_STATE_HOME"))] (str state "/clj-surgeon/usage.jsonl"))
      (when-let [home (not-empty (get env "HOME"))] (str home "/.local/state/clj-surgeon/usage.jsonl"))))
(defn repo-name
  "Basename of the nearest ancestor of cwd holding a .git entry (a directory or a worktree's file), else of
   cwd itself. No subprocess: a log field is not worth a process spawn per call."
  [cwd]
  (when cwd
    (try (loop [dir (.getAbsoluteFile (io/file cwd))]
           (cond (nil? dir) (.getName (.getAbsoluteFile (io/file cwd)))
                 (.exists (io/file dir ".git")) (.getName dir)
                 :else (recur (.getParentFile dir))))
         (catch Throwable _ nil))))
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
                 (usage-line (merge (outcome-of (if e {:exception true :error-type (:error-type (ex-data e))} (:value result)))
                                    {:op op :at (str (java.time.Instant/now)) :ms (quot (- (System/nanoTime) start) 1000000)
                                     :repo (repo-name (System/getProperty "user.dir")) :agent (agent-kind env)}))))
      (catch Throwable _ nil))
    (if-let [e (:thrown result)] (throw e) (:value result))))
