#!/usr/bin/env bb
;; Standalone script: retain the sibling scripts' namespace and hyphenated directory convention.
#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns native-control.run
  (:refer-clojure :exclude [run!])
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]))

(def script-dir (str (fs/parent *file*)))
(load-file (str script-dir "/schedule.clj"))
(require '[native-control.schedule :as schedule])
(defn valid-component? [s]
  (boolean (and (string? s) (re-matches #"[A-Za-z0-9._-]+" s) (not (#{"." ".."} s)))))
(defn validate-run [run-id task-id arm rep-str]
  (let [bad (some (fn [[k v]] (when-not (valid-component? v) k))
                  [["task_id" task-id] ["arm" arm] ["run_id" run-id]])
        rep (when (and (string? rep-str) (re-matches #"[1-9][0-9]*" rep-str)) (parse-long rep-str))]
    (cond bad {:error (str bad ": expected a safe path component, excluding . and ..")}
          (not (and rep (pos? rep))) {:error "rep: expected a positive integer within signed 64-bit range"}
          (not= run-id (str task-id "-" arm "-r" rep)) {:error "run_id: does not match task_id, arm and rep"}
          :else {:ok {:run-id run-id :task task-id :arm arm :rep rep}})))
(defn parse-schedule [text & [task-ids]]
  (let [[header & lines] (str/split-lines text) seen (volatile! #{})]
    (if (not= "run_id\ttask\tarm\trep" header)
      {:error "line 1: expected header run_id<TAB>task<TAB>arm<TAB>rep"}
      (reduce (fn [acc [i line]]
                (if (str/blank? line) acc
                    (let [cells (str/split line #"\t" -1)
                          r (cond (not= 4 (count cells)) {:error "expected 4 tab-separated cells"}
                                  (@seen (first cells)) {:error "run_id: duplicate"}
                                  :else (let [r (apply validate-run cells)]
                                          (if (or (:error r) (nil? task-ids) (task-ids (second cells))) r
                                              {:error (str "task_id: unknown " (second cells))})))]
                      (if-let [e (:error r)] (reduced {:error (str "line " (+ i 2) ": " e)})
                              (do (vswap! seen conj (first cells)) (update acc :rows conj (:ok r)))))))
              {:rows []} (map-indexed vector lines)))))
(defn run-paths [root {:keys [run-id task arm rep] :as plan}]
  (when-not (and (integer? rep) (= plan (:ok (validate-run run-id task arm (str rep)))))
    (throw (ex-info "Expected a validated plan" {})))
  (let [dir (str root "/runs/" run-id)]
    {:run-dir dir :ws (str dir "/ws") :codex-home (str dir "/codex-home") :bin-dir (str dir "/bin")}))
(defn error-message [e] (or (.getMessage e) (.getName (class e))))
(defn guarded-run [{:keys [setup! run! finish! cleanup!]}]
  (let [result (atom {:exit-code 3 :failed-before-run true})]
    (try
      (setup!)
      (let [ran (select-keys (run!) [:exit-code :detail]) rc (:exit-code ran)]
        (when-not (and (integer? rc) (<= 0 rc)) (throw (ex-info "run returned no exit code" {})))
        (reset! result (assoc ran :failed-before-run false))
        (finish! ran))
      (catch Throwable e (swap! result #(assoc % :exit-code (max 1 (:exit-code %)) :error (error-message e))))
      (finally
        (try (cleanup!) (catch Throwable e (swap! result assoc :cleanup-error (error-message e))))))
    @result))
(defn failure-receipt [{:keys [run-id task arm rep]} rc ended-at]
  {:run_id run-id :task task :arm arm :rep rep :exit_code rc :failed_before_run true :ended_at ended-at})
(defn tally [results]
  (let [n (count (remove #(zero? (:exit-code %)) results))]
    {:total (count results) :failed n :exit (if (zero? n) 0 1)}))
(defn exit-with [n] (System/exit n))
(defn entry-script? [file-prop file] (= file-prop file))
(defn find-kondo [which file]
  (some-> (if (and (fs/regular-file? file) (fs/executable? file)) file (which "clj-kondo")) str))
(defn env-config
  ([env] (env-config env {}))
  ([env {:keys [which user-home script-dir]
         :or {which fs/which user-home (System/getProperty "user.home") script-dir script-dir}}]
   (let [value #(or (not-empty (get env %1)) %2)
         cfg {:root (value "NC_RESULT_ROOT" "/var/tmp/forge/native-control")
              :model (value "NC_MODEL" "gpt-6-astra")
              :reasoning (value "NC_REASONING" "medium")
              :timeout (value "NC_TIMEOUT_S" "900")
              :kill-after (value "NC_KILL_AFTER_S" "30")
              :locale (into {} (filter (comp seq val)) (select-keys env ["LANG" "LC_ALL"]))
              :keep (value "NC_KEEP" "0")
              :sandbox (value "NC_SANDBOX" "workspace-write")
              :config (value "NC_SURGEON_BB_CONFIG" (str script-dir "/../../bb.edn"))
              :auth (value "NC_CODEX_AUTH" (str (value "HOME" user-home) "/.codex/auth.json"))
              :codex (value "NC_CODEX" (some-> (which "codex") str))
              :invoker-home user-home
              :kondo-path (find-kondo which "/usr/local/bin/clj-kondo")
              :tool-dirs ["/usr/bin" "/bin"]}
         kill-after (when (re-matches #"[0-9]+" (:kill-after cfg)) (parse-long (:kill-after cfg)))
         timeout (when (re-matches #"[0-9]+" (:timeout cfg)) (parse-long (:timeout cfg)))]
     (if (and timeout (<= 1 timeout 86400))
       (if (and kill-after (<= 1 kill-after 3600)) cfg {:error "NC_KILL_AFTER_S must be an integer 1..3600"})
       {:error "NC_TIMEOUT_S must be an integer 1..86400"}))))
(defn render-config [cfg]
  (str/join "\n" (map (fn [[k v]] (str (name k) "=" v)) (sort-by key cfg))))
(defn decide-config-exit [cfg] (if (:error cfg) 2 0))

;; arm1 has no outline tool, so needs no kondo; every arm needs rg for the native route.
;; The runner is Linux-only: tool-dirs stays ["/usr/bin" "/bin"].
(defn preflight-decision [arm resolved kondo? rg?]
  (cond
    (and (= arm "arm1") (not (str/blank? (str resolved))))
    (str "preflight: arm1 must not see clj-surgeon, found '" resolved "'")
    (and (not= arm "arm1") (not= :wrapper resolved))
    (str "preflight: clj-surgeon resolves to '" resolved "', not the arm wrapper")
    (and (not= arm "arm1") (not kondo?))
    "preflight: clj-kondo not on the arm PATH"
    (not rg?)
    "preflight: rg not on the arm PATH (every arm needs the native route)"
    :else :ok))

(defn git-env [source-env]
  (assoc (select-keys source-env ["PATH" "HOME" "LANG" "LC_ALL"])
         "GIT_CONFIG_GLOBAL" "/dev/null" "GIT_CONFIG_NOSYSTEM" "1"))
(defn checked! [argv & [env]]
  (when-not (and (vector? argv) (every? string? argv)) (throw (ex-info "argv must be a vector of strings" {})))
  (:out (p/check @(p/process argv {:in "" :out :string :err :string :env (git-env (or env (System/getenv)))}))))
(defn ensure-root! [root-str]
  (when-not (and (string? root-str) (not (str/blank? root-str)))
    (throw (ex-info "root must name a directory" {})))
  (when-not (fs/exists? root-str) (fs/create-dirs root-str {:posix-file-permissions "rwx------"}))
  (when-not (fs/directory? root-str) (throw (ex-info "root must be a directory" {})))
  (str (fs/canonicalize root-str)))
(defn materialize-workspace! [{:keys [repo sha]} arm-file ws & [{:keys [env] :or {env (System/getenv)}}]]
  (when-not (and (string? ws) (not (str/blank? ws))) (throw (ex-info "ws must be a non-blank string" {})))
  (doseq [[valid message] [[(and (string? sha) (re-matches #"[0-9a-f]{40}" sha)) "sha must be 40 lowercase hex digits"]
                          [(and repo (fs/directory? repo)) "repo directory must exist"]
                          [(and arm-file (fs/regular-file? arm-file)) "arm file must exist"]]]
    (when-not valid (throw (ex-info message {}))))
  (let [r (fs/canonicalize repo) w (fs/canonicalize ws) a (fs/canonicalize arm-file)]
    (when (or (fs/starts-with? r w) (fs/starts-with? w r))
      (throw (ex-info "ws must not equal repo or be its ancestor or descendant" {})))
    (when (fs/starts-with? a w) (throw (ex-info "arm file must be outside ws" {}))))
  (fs/delete-tree ws)
  (fs/create-dirs ws)
  (fs/set-posix-file-permissions ws "rwx------")
  (clojure.core/run! p/check (mapv deref (p/pipeline (p/pb ["git" "-C" repo "archive" "--end-of-options" sha]
                                          {:in "" :err :string :env (git-env env)})
                                    (p/pb ["tar" "-xf" "-" "-C" ws] {:out :string :err :string :env (git-env env)}))))
  (let [visit (fn [f _]
                (if (#{"AGENTS.md" "CLAUDE.md" ".codex" ".clj-surgeon.edn" ".mcp.json"} (fs/file-name f))
                  (do (fs/delete-tree f)
                      :skip-subtree)
                  :continue))
        git! (fn [& args] (checked! (into ["git" "-C" ws] args) env))]
    (fs/walk-file-tree ws {:pre-visit-dir visit :visit-file visit})
    (fs/copy arm-file (str ws "/AGENTS.md"))
    (git! "init" "-q" "--template=")
    (git! "add" "-A" "-f")
    (git! "-c" "user.name=native-control" "-c" "user.email=native-control@anvil"
          "-c" "commit.gpgsign=false" "commit" "-qm" "Frozen read experiment")
    (str/trim (git! "rev-parse" "HEAD"))))

(defn install-arm-tools! [cfg arm wrapper bin-dir]
  (let [java (or (fs/which "java") (throw (ex-info "preflight: java not found on invoker PATH" {})))]
    (when (and (not= arm "arm1") (seq wrapper))
      (let [bb (or (fs/which "bb") (throw (ex-info "preflight: bb not found on invoker PATH" {})))
            kondo (or (when-let [k (:kondo-path cfg)]
                        (when (and (fs/regular-file? k) (fs/executable? k)) k))
                      (fs/which "clj-kondo" {:paths (:tool-dirs cfg)})
                      (throw (ex-info "preflight: clj-kondo not found" {})))]
        (spit (str bin-dir "/clj-surgeon") wrapper)
        (fs/set-posix-file-permissions (str bin-dir "/clj-surgeon") "rwxr-xr-x")
        (fs/create-sym-link (str bin-dir "/clj-kondo") kondo)
        (fs/create-sym-link (str bin-dir "/bb") bb)))
    (str/join ":" (concat [bin-dir] (:tool-dirs cfg) [(fs/parent java)]))))

(defn write-profile! [codex-home path]
  (fs/set-posix-file-permissions codex-home "rwx------")
  (doseq [name [".profile" ".bashrc"]]
    (spit (str codex-home "/" name) (str "export PATH='" (str/replace path "'" "'\\''") "'\n"))))

(defn resolve-on-path [codex-home path name]
  (when-not (#{"clj-surgeon" "clj-kondo" "rg"} name)
    (throw (ex-info "Unsupported preflight command" {:name name})))
  (str/trim (checked! ["env" "-i" (str "HOME=" codex-home) (str "PATH=" path)
                       "bash" "-lc" (str "command -v " name " || true")])))

(defn preflight! [arm wrapper bin-dir codex-home path run-dir config]
  (let [[resolved kondo rg] (mapv #(resolve-on-path codex-home path %) ["clj-surgeon" "clj-kondo" "rg"])
        normalized (if (and (seq wrapper) (= resolved (str bin-dir "/clj-surgeon"))) :wrapper resolved)
        decision (preflight-decision arm normalized (seq kondo) (seq rg))]
    (spit (str run-dir "/preflight.txt")
          (str "resolved_clj_surgeon=" resolved "\nresolved_clj_kondo=" kondo
               "\nresolved_rg=" rg "\nbb_config=" config "\n"))
    (when-not (= :ok decision)
      (throw (ex-info decision {})))))

(defn install-credential! [auth-src codex-home cfg]
  (when-not (and auth-src (fs/regular-file? auth-src)
                 (or (fs/readable? auth-src) (= \r (first (fs/posix->str (fs/posix-file-permissions auth-src))))))
    (throw (ex-info (str "credential must be a regular readable file: " auth-src) {})))
  (doseq [key [:model :reasoning] :let [value (get cfg key)]]
    (when-not (and (string? value) (not (re-find #"\p{Cc}" value)))
      (throw (ex-info (str (name key) " must be a string without control characters") {}))))
  (let [dest (str codex-home "/auth.json")]
    (fs/delete-if-exists dest)
    (fs/create-file dest {:posix-file-permissions "rw-------"})
    ;; fs/copy with :replace-existing recreates the file with the source mode.
    (io/copy (io/file auth-src) (io/file dest)))
  (spit (str codex-home "/config.toml")
        (str "model = " (pr-str (:model cfg)) "\nmodel_reasoning_effort = " (pr-str (:reasoning cfg)) "\n")))
(defn setup! [tasks-path {:keys [task arm] :as plan} cfg paths]
  (when-not (and (seq (:tool-dirs cfg)) (fs/which "timeout" {:paths (:tool-dirs cfg)}))
    (throw (ex-info "Linux prerequisite: timeout required on PATH" {})))
  (when-not (and (:config cfg) (fs/regular-file? (:config cfg)))
    (throw (ex-info (str "bb config must be a regular file: " (:config cfg)) {})))
  (when-not (= paths (run-paths (:root cfg) plan))
    (throw (ex-info "Setup paths must match run-paths before canonicalization" {})))
  (let [t (try (schedule/find-task (or (:tasks cfg) (edn/read-string (slurp tasks-path))) task)
               (catch Exception e (throw (ex-info (str "Task " task " in " tasks-path ": " (ex-message e)) {} e))))
        root (ensure-root! (:root cfg))
        {:keys [run-dir ws codex-home bin-dir] :as paths} (run-paths root plan)
        config (str (fs/canonicalize (:config cfg)))
        wrapper (schedule/wrapper arm config)
        script-dir (or (:script-dir cfg) script-dir)
        arm-file (str (fs/canonicalize (str script-dir "/" (schedule/task-field t arm))))]
    (when-not (fs/starts-with? arm-file (fs/path (fs/canonicalize script-dir) "arms"))
      (throw (ex-info "arm file must be under script-dir/arms" {:arm arm :file arm-file})))
    (doseq [dir [codex-home bin-dir]]
      (fs/delete-tree dir)
      (fs/create-dirs dir {:posix-file-permissions "rwx------"}))
    (doseq [name ["answer.txt" "failure.json" "run.json" "run.json.tmp" "preflight.txt" "events.jsonl" "stderr.txt"]]
      (fs/delete-if-exists (str run-dir "/" name)))
    (materialize-workspace! t arm-file ws)
    (let [path (install-arm-tools! cfg arm wrapper bin-dir)]
      (write-profile! codex-home path)
      (preflight! arm wrapper bin-dir codex-home path run-dir config)
      (install-credential! (:auth cfg) codex-home cfg)
      {:path path :task t :arm-bytes (fs/size arm-file) :config config :paths paths})))

(defn codex-argv [{:keys [timeout-path timeout kill-after codex sandbox wrapper invoker-home model reasoning]}
                  ws run-dir prompt]
  [timeout-path (str "--kill-after=" kill-after) timeout codex "exec" "--json" "--ephemeral"
   "--ignore-user-config" "--skip-git-repo-check" "--sandbox" sandbox "--color" "never" "-c"
   (str "sandbox_workspace_write.writable_roots="
        (json/generate-string (mapv #(str invoker-home "/.local/state/" %)
                                    (when (seq wrapper) ["clj-surgeon" "diagnose-skiff-cpu-memory"]))))
   "-m" model "-c" (str "model_reasoning_effort=" (pr-str reasoning))
   "-C" ws "-o" (str run-dir "/answer.txt") prompt])
(defn codex-env [cfg codex-home path] (merge {"HOME" codex-home "CODEX_HOME" codex-home "PATH" path "TERM" "dumb"}
         (select-keys (:locale cfg) ["LANG" "LC_ALL"])))
(defn run! [cfg {:keys [paths task path] :as ctx} launched]
  (let [{:keys [ws run-dir codex-home]} paths
        cfg (assoc cfg :timeout-path (str (fs/which "timeout" {:paths (:tool-dirs cfg)})))
        argv (codex-argv cfg ws run-dir (schedule/task-prompt task))
        start (System/currentTimeMillis)]
    (reset! launched true)
    {:exit-code (:exit @(p/process argv {:in "" :out (fs/file run-dir "events.jsonl")
                                        :err (fs/file run-dir "stderr.txt") :env (codex-env cfg codex-home path)}))
     :detail (assoc ctx :start start :end (System/currentTimeMillis) :timeout-path (:timeout-path cfg))}))
(defn finish! [plan cfg {:keys [exit-code detail]}]
  (let [{:keys [task arm-bytes start end paths timeout-path]} detail
        target (str (:run-dir paths) "/run.json")
        status (checked! ["git" "-C" (:ws paths) "status" "--porcelain" "--ignored" "--untracked-files=all"])
        receipt (schedule/receipt (mapv str [(:run-id plan) (:task plan) (:arm plan) (:rep plan)
                                             (:model cfg) (:reasoning cfg) (:repo task) (:sha task)
                                          start end exit-code status arm-bytes (java.time.Instant/ofEpochMilli start)
                                             (java.time.Instant/ofEpochMilli end)]))]
    (spit (str target ".tmp") (json/generate-string
                              (assoc receipt :timeout_method timeout-path :kill_after_s (:kill-after cfg))))
    (fs/move (str target ".tmp") target {:atomic-move true})))
(defn cleanup! [cfg {:keys [ws codex-home]}]
  (when-not (= "1" (:keep cfg))
    (when-let [errors (seq (keep #(try (fs/delete-tree %)
                                      nil
                                      (catch Throwable e (str % ": " (error-message e)))) [ws codex-home]))]
      (throw (ex-info (str/join "; " errors) {})))))
(defn run-one! [tasks-path plan cfg]
  (let [paths (run-paths (:root cfg) plan) target (str (:run-dir paths) "/run.json")
        complete? (fs/exists? target) ctx (atom nil) launched (atom false)
        result (guarded-run
                 {:setup! #(when-not complete? (reset! ctx (setup! tasks-path plan cfg paths)))
                  :run! #(if complete? {:exit-code (:exit_code (json/parse-string (slurp target) true))}
                             (run! (assoc cfg :wrapper (schedule/wrapper (:arm plan) (:config @ctx))) @ctx launched))
                  :finish! #(when-not complete? (finish! plan cfg %))
                  :cleanup! #(cleanup! cfg paths)})
        rc (if (:cleanup-error result) (max 1 (:exit-code result)) (:exit-code result))]
    (doseq [error (keep result [:error :cleanup-error])] (binding [*out* *err*] (println error)))
    (when-not (fs/exists? target)
      (spit (fs/file (fs/create-dirs (:run-dir paths)) "failure.json")
            (json/generate-string (assoc (failure-receipt plan rc (str (java.time.Instant/now)))
                                    :failed_before_run (not @launched)))))
    (assoc result :exit-code rc :failed-before-run (not @launched))))
(defn run-schedule! [rows root run-fn]
  (tally (mapv (fn [row] (try (run-fn row)
                        (catch Throwable e
                          (binding [*out* *err*] (println (str (:run-id row) ": " (error-message e))))
                          (try (let [dir (:run-dir (run-paths root row))]
                              (when-not (fs/exists? (str dir "/run.json"))
                                  (spit (fs/file (fs/create-dirs dir) "failure.json")
                                        (json/generate-string (failure-receipt row 3 (str (java.time.Instant/now)))))))
                              (catch Throwable e
                              (binding [*out* *err*]
                                (println (str (:run-id row) ": failure receipt not written: " (error-message e))))))
                          {:exit-code 3}))) rows)))
(defn run-decision [args]
  (if (= 5 (count args)) (apply validate-run (rest args))
      {:error "Usage: run TASKS.edn RUN_ID TASK_ID ARM REP"}))
(defn read-input! [kind path parse]
  (try (parse (slurp path))
       (catch Throwable e (throw (ex-info (str kind ": cannot read " path ": " (error-message e)) {} e)))))
(defn run-command! [command args]
  (let [decided? (atom false)]
    (try
      (let [initial (case command
                      "run" (run-decision args)
                      "schedule" (when-not (= 2 (count args)) {:error "Usage: schedule TASKS.edn SCHEDULE.tsv"})
                      {:error "Usage: bb bench/native-control/run.clj --self-test | config | run | schedule"})
            _ (when-let [e (:error initial)] (throw (ex-info e {})))
            tasks (read-input! "tasks" (first args) edn/read-string)
            cfg (assoc (env-config (System/getenv)) :tasks tasks)
            decision (if (= command "schedule")
                       (parse-schedule (read-input! "schedule" (second args) identity) (set (map :id tasks)))
                       initial)]
        (when-let [e (or (:error cfg) (:error decision))] (throw (ex-info e {})))
        (when (= command "run") (schedule/find-task tasks (:task (:ok decision))))
        (reset! decided? true)
        (if (= command "run")
          (:exit-code (run-one! (first args) (:ok decision) cfg))
          (let [{:keys [total failed exit]} (run-schedule! (:rows decision) (:root cfg)
                                                        #(run-one! (first args) % cfg))]
            (println (if (zero? total) "schedule: 0 runs (nothing to run)"
                         (str "schedule: " total " runs, " failed " failed")))
            (if (zero? total) 1 exit))))
      (catch Throwable e (binding [*out* *err*] (println (error-message e)))
        (if @decided? 3 2)))))
;; PR 35 regressions: traversal, last-line loss, setup cleanup and lost run status.
(deftest components
  (doseq [s [nil "" "." ".." "../../x" "a/b" "a b" "a\nb"]] (is (false? (valid-component? s))))
  (doseq [s ["T01" "a.b_c-d" "..."]] (is (true? (valid-component? s)))))
(deftest validation
  (is (= {:ok {:run-id "T01-arm1-r1" :task "T01" :arm "arm1" :rep 1}}
         (validate-run "T01-arm1-r1" "T01" "arm1" "1")))
  (doseq [[id task arm rep field] [["../../x-arm1-r1" "../../x" "arm1" "1" "task_id"]
                                 ["T01-a/b-r1" "T01" "a/b" "1" "arm"]
                                 ["../x" "T01" "arm1" "1" "run_id"]
                                 ["wrong" "T01" "arm1" "1" "run_id"]]]
    (is (str/starts-with? (:error (validate-run id task arm rep)) field)))
  (doseq [rep ["01" "+1" "0" "-1" "1.5" "" "9223372036854775808" nil]]
    (is (str/starts-with? (:error (validate-run "T01-arm1-r1" "T01" "arm1" rep)) "rep"))))
(deftest schedules
  (let [header "run_id\ttask\tarm\trep"
        rows ["T01-arm1-r1\tT01\tarm1\t1" "T02-arm2-r2\tT02\tarm2\t2"]
        expected {:rows [{:run-id "T01-arm1-r1" :task "T01" :arm "arm1" :rep 1}
                         {:run-id "T02-arm2-r2" :task "T02" :arm "arm2" :rep 2}]}]
    (doseq [sep ["\n" "\r\n"] tail ["" sep]]
      (is (= expected (parse-schedule (str (str/join sep [header (first rows) "" (second rows)]) tail)))))
    (is (= {:rows []} (parse-schedule header)))
    (is (= {:error "line 3: expected 4 tab-separated cells"}
           (parse-schedule (str header "\n\nshort\trow")))))
  (doseq [header ["" "garbage" "run_id\ttask\tarm\trep "]]
    (is (= {:error "line 1: expected header run_id<TAB>task<TAB>arm<TAB>rep"} (parse-schedule header))))
  (is (= {:error "line 4: run_id: duplicate"}
         (parse-schedule "run_id\ttask\tarm\trep\nT-a-r1\tT\ta\t1\n\nT-a-r1\tT\ta\t1")))
  (is (str/starts-with? (:error (parse-schedule "run_id\ttask\tarm\trep\n../../x-a-r1\t../../x\ta\t1"))
                        "line 2: task_id")))
(deftest paths
  (let [plan (:ok (validate-run "T01-arm1-r1" "T01" "arm1" "1")) d "/root/runs/T01-arm1-r1"]
    (is (= {:run-dir d :ws (str d "/ws") :codex-home (str d "/codex-home") :bin-dir (str d "/bin")}
           (run-paths "/root" plan)))
    (doseq [bad [nil {} (assoc plan :task "../x") (assoc plan :rep "1")]]
      (is (thrown? Exception (run-paths "/root" bad))))))
(deftest setup-and-run-throws
  (doseq [stage [:setup :run]]
    (let [calls (atom []) step (fn [s] (swap! calls conj s) (when (= stage s) (throw (ex-info "boom" {}))))
          result (guarded-run {:setup! #(step :setup) :run! #(step :run)
                               :finish! (fn [_] (step :finish)) :cleanup! #(step :cleanup)})]
      (is (= {:exit-code 3 :failed-before-run true :error "boom"} result))
      (is (= (if (= stage :setup) [:setup :cleanup] [:setup :run :cleanup]) @calls)))))
(deftest status-and-cleanup
  (doseq [rc [0 7] cleanup-throws? [false true]]
    (let [calls (atom []) returned {:exit-code rc :detail "kept"}
          result (guarded-run
                   {:setup! #(swap! calls conj :setup)
                    :run! #(do (swap! calls conj :run) returned)
                    :finish! #(swap! calls conj [:finish %])
                    :cleanup! #(do (swap! calls conj :cleanup)
                                   (when cleanup-throws? (throw (NullPointerException.))))})]
      (is (= (cond-> (assoc returned :failed-before-run false)
               cleanup-throws? (assoc :cleanup-error "java.lang.NullPointerException")) result))
      (is (= [:setup :run [:finish returned] :cleanup] @calls)))))
(deftest finish-throws
  (doseq [rc [0 7]]
    (let [cleaned (atom false)]
      (is (= {:exit-code (max 1 rc) :failed-before-run false :error "java.lang.NullPointerException"}
             (guarded-run {:setup! (constantly nil) :run! (constantly {:exit-code rc})
                           :finish! (fn [_] (throw (NullPointerException.))) :cleanup! #(reset! cleaned true)})))
      (is @cleaned))))
(deftest invalid-run-results
  (doseq [ran [nil {} {:exit-code nil} {:exit-code "0"} {:exit-code 0.5} {:exit-code -1}]]
    (is (= {:exit-code 3 :failed-before-run true :error "run returned no exit code"}
           (guarded-run {:setup! (constantly nil) :run! (constantly ran)
                         :finish! (fn [_] (throw (AssertionError.))) :cleanup! (constantly nil)}))))
  (is (= {:exit-code 0 :failed-before-run false}
         (guarded-run {:setup! (constantly nil) :run! (constantly {:exit-code 0 :error "x" :unknown true})
                       :finish! (constantly nil) :cleanup! (constantly nil)}))))
(deftest setup-and-cleanup-both-throw
  (is (= {:exit-code 3 :failed-before-run true :error "setup" :cleanup-error "cleanup"}
         (guarded-run {:setup! #(throw (ex-info "setup" {}))
                       :run! #(throw (AssertionError. "must not run"))
                       :finish! (fn [_] (throw (AssertionError. "must not finish")))
                       :cleanup! #(throw (ex-info "cleanup" {}))}))))
(deftest schedule-invalid-cells
  (doseq [[row reason] [["T-a-r1\tT\ta\t" "rep"] ["T-a-r1\tT\ta\t1\textra" "expected 4"]
                        ["T-a-r2\tT\ta\t1" "run_id"] ["T-a/b-r1\tT\ta/b\t1" "arm"]]]
    (is (str/starts-with? (:error (parse-schedule (str "run_id\ttask\tarm\trep\n" row))) (str "line 2: " reason)))))
(deftest receipts-and-tallies
  (is (= {:run_id "T01-arm1-r1" :task "T01" :arm "arm1" :rep 1
          :exit_code 3 :failed_before_run true :ended_at "end"}
         (failure-receipt {:run-id "T01-arm1-r1" :task "T01" :arm "arm1" :rep 1} 3 "end")))
  (doseq [[codes expected] [[[] {:total 0 :failed 0 :exit 0}]
                           [[0 0] {:total 2 :failed 0 :exit 0}]
                           [[0 7 3] {:total 3 :failed 2 :exit 1}]]]
    (is (= expected (tally (map #(hash-map :exit-code %) codes))))))
(deftest cli-load-guard
  (is (true? (entry-script? "run.clj" "run.clj")))
  (is (false? (entry-script? "other.clj" "run.clj")))
  (is (false? (entry-script? nil "run.clj"))))
(deftest environment-defaults
  (with-redefs [fs/regular-file? (constantly false)]
    (let [cfg (env-config {"HOME" "/credential-home"}
                          {:which {"codex" "/tools/codex" "clj-kondo" "/tools/kondo"}
                           :user-home "/invoker" :script-dir "/bench"})]
      (is (= {:root "/var/tmp/forge/native-control"
              :model "gpt-6-astra"
              :reasoning "medium"
              :timeout "900"
              :kill-after "30" :locale {}
              :keep "0"
              :sandbox "workspace-write"
              :auth "/credential-home/.codex/auth.json"
              :config "/bench/../../bb.edn"
              :codex "/tools/codex"
              :invoker-home "/invoker"
              :kondo-path "/tools/kondo"
              :tool-dirs ["/usr/bin" "/bin"]}
             cfg)))))

(deftest environment-overrides
  (is (= {:root "/results"
          :model "test-model"
          :reasoning "high"
          :timeout "12"
          :keep "1"
          :sandbox "read-only"
          :config "/config.edn"
          :auth "/auth.json"
          :codex "/codex"}
         (dissoc (env-config {"NC_RESULT_ROOT" "/results"
                              "NC_MODEL" "test-model"
                              "NC_REASONING" "high"
                              "NC_TIMEOUT_S" "12"
                              "NC_KEEP" "1"
                              "NC_SANDBOX" "read-only"
                              "NC_SURGEON_BB_CONFIG" "/config.edn"
                              "NC_CODEX_AUTH" "/auth.json"
                              "NC_CODEX" "/codex"})
                 :invoker-home :kondo-path :tool-dirs :kill-after :locale))))

(deftest environment-empty-values
  (is (= (env-config {})
         (env-config {"NC_RESULT_ROOT" "" "NC_MODEL" "" "NC_REASONING" ""
                      "NC_TIMEOUT_S" "" "NC_KEEP" "" "NC_SANDBOX" ""
                      "NC_SURGEON_BB_CONFIG" "" "NC_CODEX_AUTH" "" "NC_CODEX" "" "HOME" ""}))))

(deftest environment-bad-timeout
  (doseq [value ["0" "86401" "9223372036854775808" "-1" "+1" "1.5" " 9" "9 " "1s" "\n" "9\n" "１２"]]
    (is (= {:error "NC_TIMEOUT_S must be an integer 1..86400"}
           (env-config {"NC_TIMEOUT_S" value})))))

(deftest environment-digit-timeout
  (doseq [value ["1" "01" "900" "86400"]]
    (is (= value (:timeout (env-config {"NC_TIMEOUT_S" value}))))))

(deftest environment-invoker-home
  (doseq [env [{} {"HOME" ""} {"HOME" "/unrelated-home"}]]
    (is (= "/invoker" (:invoker-home (env-config env {:user-home "/invoker"}))))))

(deftest kondo-executable-file
  (let [file (fs/create-temp-file) which {"clj-kondo" "/tools/kondo"}]
    (try
      (fs/set-posix-file-permissions file "rw-------")
      (is (= "/tools/kondo" (find-kondo which file)))
      (fs/set-posix-file-permissions file "rwx------")
      (is (= (str file) (find-kondo (fn [_] (throw (AssertionError. "unexpected PATH lookup"))) file)))
      (is (= "/tools/kondo" (find-kondo which (fs/parent file))))
      (fs/delete file)
      (is (= "/tools/kondo" (find-kondo which file)))
      (is (nil? (find-kondo (constantly nil) file)))
      (finally (fs/delete-if-exists file)))))

(deftest config-output-and-exit
  (is (= "auth=/auth.json\ninvoker-home=/invoker\nmodel=test\ntool-dirs=[\"/usr/bin\" \"/bin\"]"
         (render-config {:model "test" :tool-dirs ["/usr/bin" "/bin"]
                         :invoker-home "/invoker" :auth "/auth.json"})))
  (is (= 0 (decide-config-exit {:model "test"})))
  (is (= 2 (decide-config-exit {:error "invalid timeout"}))))

(deftest preflight-refusals
  (doseq [[arm resolved kondo? rg? message]
          [["arm1" "/leaked" true true "preflight: arm1 must not see clj-surgeon, found '/leaked'"]
           ["arm2" "/wrong" true true "preflight: clj-surgeon resolves to '/wrong', not the arm wrapper"]
           ["arm2" :wrapper false true "preflight: clj-kondo not on the arm PATH"]
           ["arm1" "" false false "preflight: rg not on the arm PATH (every arm needs the native route)"]]]
    (is (= message (preflight-decision arm resolved kondo? rg?)))))

(deftest preflight-accepted
  (doseq [resolved [nil "" " \n"] kondo? [false true]]
    (is (= :ok (preflight-decision "arm1" resolved kondo? true))))
  (doseq [arm ["arm2" "arm3"]]
    (is (= :ok (preflight-decision arm :wrapper true true)))))

(deftest git-environment-allow-list-pr71
  (doseq [allowed [{} {"PATH" "/usr/bin" "HOME" "/h"} {"LANG" "C" "LC_ALL" "C.UTF-8"}]]
    (is (= (merge {"GIT_CONFIG_GLOBAL" "/dev/null" "GIT_CONFIG_NOSYSTEM" "1"} allowed)
           (git-env (merge allowed {"GIT_CONFIG_COUNT" "1" "GIT_CONFIG_KEY_0" "core.hooksPath"
                                   "GIT_CONFIG_VALUE_0" "/x" "GIT_CONFIG_PARAMETERS" "..."
                                   "GIT_COMMON_DIR" "/g" "GIT_ALTERNATE_OBJECT_DIRECTORIES" "/o"
                                   "GIT_CEILING_DIRECTORIES" "/c" "GIT_NAMESPACE" "poison"
                                   "GIT_TEMPLATE_DIR" "/t" "GIT_DIR" "/g"}))))))
(deftest checked-process-streams-and-failure
  (doseq [argv ["echo unsafe" '("echo") ["echo" nil] nil]]
    (with-redefs [p/process (fn [& _] (throw (AssertionError. "process ran")))]
      (is (thrown-with-msg? Exception #"vector of strings" (checked! argv)))))
  (is (= "" (checked! ["sh" "-c" "cat"])))
  (is (= "literal $value\n" (checked! ["printf" "%s\n" "literal $value"])))
  (is (thrown-with-msg? Exception #"process stderr" (checked! ["sh" "-c" "echo 'process stderr' >&2; exit 7"]))))
(deftest result-root-permissions
  (let [tmp (fs/create-temp-dir {:prefix "nc-root-"}) root (str tmp "/results")
        mode #(fs/posix->str (fs/posix-file-permissions %))]
    (try
      (spit (str tmp "/file") "regular file")
      (doseq [invalid [nil (str tmp "/file")]]
        (is (thrown-with-msg? Exception #"root.*directory" (ensure-root! invalid))))
      (is (= (str (fs/canonicalize root)) (ensure-root! root)))
      (is (= "rwx------" (mode root)))
      (fs/set-posix-file-permissions root "rwxr-x---")
      (is (= (str (fs/canonicalize root)) (ensure-root! (str root "/../results"))))
      (is (= "rwxr-x---" (mode root)))
      (finally (fs/delete-tree tmp)))))
;; Reduced from the PR 35 setup integration fixture: inherited instructions and ignored files.
(defn with-workspace-fixture [f]
  (doseq [tool ["git" "tar"]] (is (some? (fs/which tool)) (str "Missing integration prerequisite: " tool)))
  (let [tmp (str (fs/create-temp-dir {:prefix "nc-materialize-"})) repo (str tmp "/repo") ws (str tmp "/ws")
        arm-file (str tmp "/arm.md") arm-text "Native-control fixture instructions: λ\n"
        git! (fn [& args] (checked! (into ["git" "-C" repo] args)))
        workspace-git! (fn [& args] (checked! (into ["git" "-C" ws] args)))]
    (try
      (fs/create-dirs (str repo "/nested/.codex"))
      (doseq [name ["AGENTS.md" "CLAUDE.md" ".clj-surgeon.edn" ".mcp.json" ".codex/config.toml"]]
        (spit (str repo "/nested/" name) "inherited instructions"))
      (spit (str repo "/AGENTS.md") "old root instructions")
      (spit (str repo "/.gitignore") "AGENTS.md\nignored.txt\n")
      (spit (str repo "/tracked.txt") "frozen source\n")
      (spit (str repo "/ignored.txt") "forced into baseline\n")
      (spit arm-file arm-text)
      (git! "init" "-q")
      (git! "add" "-A" "-f")
      (git! "-c" "user.name=fixture" "-c" "user.email=fixture@example.test"
            "-c" "commit.gpgsign=false" "commit" "-qm" "Fixture")
      (fs/create-dirs (str ws "/.git"))
      (spit (str ws "/stale.txt") "stale workspace")
      (spit (str ws "/arm.md") arm-text)
      (f {:tmp tmp :repo repo :ws ws :arm-file arm-file :arm-text arm-text
          :git! git! :workspace-git! workspace-git!})
      (finally (fs/delete-tree tmp)))))
(deftest materialize-workspace-integration-requires-git
  (with-workspace-fixture
    (fn [{:keys [tmp repo ws arm-file arm-text git! workspace-git!]}]
      (let [task {:repo repo :sha (str/trim (git! "rev-parse" "HEAD"))} pb p/pb]
        ;; PR 71: refusal precedes mutation; inherited config and templates cannot run or install hooks.
        (doseq [[t arm dest message] [[(assoc task :sha "--output=x") arm-file ws #"sha"]
                                    [(assoc task :sha "HEAD") arm-file ws #"sha"]
                                    [task (str tmp "/missing") ws #"arm file"]
                                    [task (str ws "/arm.md") ws #"arm file must be outside ws"]
                                    [(assoc task :repo arm-file) arm-file ws #"repo directory"]
                                    [(assoc task :repo (str tmp "/missing")) arm-file ws #"repo directory"]
                                    [task arm-file repo #"ancestor"] [task arm-file tmp #"ancestor"]
                                    [task arm-file (str repo "/nested") #"descendant"]
                                    [{} nil "" #"ws must"] [{} nil nil #"ws must"] [{} nil " " #"ws must"]]]
          (let [unexpected (fn [& _] (throw (AssertionError. "side effect before refusal")))]
            (with-redefs [p/process unexpected p/pipeline unexpected]
              (is (thrown-with-msg? Exception message (materialize-workspace! t arm dest)))))
          (is (= arm-text (slurp (str ws "/arm.md"))))
          (is (= "stale workspace" (slurp (str ws "/stale.txt")))))
        (is (= "inherited instructions" (slurp (str repo "/nested/AGENTS.md"))))
        (doseq [dir ["hooks" "template/hooks"]]
          (fs/create-dirs (str tmp "/" dir))
          (spit (str tmp "/" dir "/pre-commit") (str "#!/bin/sh\ntouch '" tmp "/" dir "/ran'\n"))
          (fs/set-posix-file-permissions (str tmp "/" dir "/pre-commit") "rwx------"))
        (doseq [fresh? [false true]]
          (when fresh? (fs/delete-tree ws))
          (is (= (not fresh?) (fs/exists? ws)))
          (let [env (merge (into {} (System/getenv))
                           {"GIT_DIR" (str repo "/.git") "GIT_WORK_TREE" repo
                            "GIT_INDEX_FILE" (str repo "/.git/index")
                            "GIT_OBJECT_DIRECTORY" (str repo "/.git/objects")
                            "GIT_CONFIG_GLOBAL" arm-file "GIT_CONFIG_NOSYSTEM" "0"
                            "GIT_CONFIG_COUNT" "1" "GIT_CONFIG_KEY_0" "core.hooksPath"
                            "GIT_CONFIG_VALUE_0" (str tmp "/hooks") "GIT_TEMPLATE_DIR" (str tmp "/template")})
                baseline (materialize-workspace! task arm-file ws {:env env})]
            (is (= (:sha task) (str/trim (git! "rev-parse" "HEAD"))))
            (is (fs/directory? (str ws "/.git")))
            (is (= baseline (str/trim (workspace-git! "rev-parse" "HEAD"))))
            (is (= "rwx------" (fs/posix->str (fs/posix-file-permissions ws))))
            (is (= "commit\n" (workspace-git! "cat-file" "-t" baseline)))))
        (doseq [path ["hooks/ran" "template/hooks/ran" "ws/.git/hooks/pre-commit"]]
          (is (not (fs/exists? (str tmp "/" path)))))
        (is (= arm-text (slurp (str ws "/AGENTS.md"))))
        (is (= "frozen source\n" (slurp (str ws "/tracked.txt"))))
        (is (= "forced into baseline\n" (workspace-git! "show" "HEAD:ignored.txt")))
        (is (not (fs/exists? (str ws "/stale.txt"))))
        (is (empty? (fs/list-dir (str ws "/nested"))))
        (is (empty? (filter #(#{"CLAUDE.md" ".codex"} (fs/file-name %)) (fs/glob ws "**" {:hidden true}))))
        (is (= "native-control <native-control@anvil>\nnative-control <native-control@anvil>\n"
               (workspace-git! "log" "-1" "--format=%an <%ae>%n%cn <%ce>")))
        (is (= "Frozen read experiment\n" (workspace-git! "log" "-1" "--format=%s")))
        (is (= arm-text (workspace-git! "show" "HEAD:AGENTS.md")))
        (is (= "" (workspace-git! "status" "--porcelain" "--ignored" "--untracked-files=all")))
        (doseq [[sha message] [[(:sha task) #"tar-failed"] [(apply str (repeat 40 "0")) #"not a tree object"]]]
          (with-redefs [p/pb #(pb (if (and (= sha (:sha task)) (= "tar" (first %1)))
                                   ["sh" "-c" "cat >/dev/null; echo tar-failed >&2; exit 9"] %1) %2)]
            (is (thrown-with-msg? Exception message
                                 (materialize-workspace! (assoc task :sha sha) arm-file ws))))
          (is (not (fs/exists? (str ws "/.git")))))))))
(deftest setup-integration-requires-git
  (with-workspace-fixture
    (fn [{:keys [tmp repo arm-file arm-text git!]}]
      (let [tools (str tmp "/tools") auth (str tmp "/auth.json") tasks (str tmp "/tasks.edn")
            task {:id "T01" :repo repo :sha (str/trim (git! "rev-parse" "HEAD"))}
            stale ["answer.txt" "failure.json" "run.json" "run.json.tmp" "events.jsonl" "stderr.txt"]
            mode #(fs/posix->str (fs/posix-file-permissions %))]
        (doseq [tool ["timeout" "java" "bb" "bash" "env"]] (is (some? (fs/which tool)) tool))
        (fs/create-dirs tools)
        (fs/create-dirs (str tmp "/arms"))
        (doseq [name ["rg" "clj-kondo"]]
          (spit (str tools "/" name) "#!/bin/sh\nexit 0\n")
          (fs/set-posix-file-permissions (str tools "/" name) "rwxr-xr-x"))
        (spit auth "{\"fixture\":true}")
        (spit tasks (pr-str [task]))
        (doseq [arm ["arm1" "arm2"]]
          (fs/copy arm-file (str tmp "/" (schedule/task-field task arm)) {:replace-existing true})
          (let [cfg (assoc (env-config {}) :root (str tmp "/./" arm) :auth auth :script-dir tmp
                           :kondo-path (str tools "/clj-kondo") :tool-dirs [tools "/usr/bin" "/bin"])
                plan (:ok (validate-run (str "T01-" arm "-r1") "T01" arm "1"))
                input-paths (run-paths (:root cfg) plan)
                {:keys [run-dir ws codex-home bin-dir] :as paths} (run-paths (str tmp "/" arm) plan)
                config (str (fs/canonicalize (:config cfg)))
                path (str bin-dir ":" tools ":/usr/bin:/bin:" (fs/parent (fs/which "java")))]
            (spit (str tmp "/invalid.edn") "[")
            (doseq [[overrides source id message]
                    [[{:tool-dirs nil} tasks "T01" #"Linux prerequisite: timeout"]
                     [{:tool-dirs [] :config "missing"} tasks "T01" #"Linux prerequisite: timeout"]
                     [{:config (str tmp "/missing-bb.edn")} tasks "T01" #"config.*regular file"]
                     [{:config tools} tasks "T01" #"config.*regular file"]
                     [{} (str tmp "/missing.edn") "T01" #"missing.edn"]
                     [{} (str tmp "/invalid.edn") "T01" #"invalid.edn"]
                     [{} tasks "unknown-id" #"unknown-id"]]]
              (let [plan (:ok (validate-run (str id "-" arm "-r1") id arm "1"))]
                (is (thrown-with-msg? Exception message
                      (setup! source plan (merge cfg overrides) (run-paths (:root cfg) plan)))))
              (is (not (fs/exists? (:root cfg))) "prerequisite refusal must precede every write"))
            (doseq [file (concat (map #(str % "/stale") [codex-home bin-dir])
                                 (map #(str run-dir "/" %) (conj stale "preflight.txt")))]
              (fs/create-dirs (fs/parent file))
              (spit file "old"))
            (is (= {:path path :task task :arm-bytes (fs/size arm-file) :config config :paths paths}
                   (setup! tasks plan cfg input-paths)))
            (is (= arm-text (slurp (str ws "/AGENTS.md"))))
            (is (= "rwx------" (mode codex-home)))
            (is (= "rw-------" (mode (str codex-home "/auth.json"))))
            (is (= #{".profile" ".bashrc" "auth.json" "config.toml"}
                   (set (map fs/file-name (fs/list-dir codex-home)))))
            (is (= (slurp auth) (slurp (str codex-home "/auth.json"))))
            (is (not (fs/same-file? auth (str codex-home "/auth.json"))))
            (is (= "model = \"gpt-6-astra\"\nmodel_reasoning_effort = \"medium\"\n"
                   (slurp (str codex-home "/config.toml"))))
            (is (every? #(not (fs/exists? (str run-dir "/" %)))
                        (into stale ["bin/stale" "codex-home/stale"])))
            (is (str/includes? (slurp (str run-dir "/preflight.txt")) (str "resolved_rg=" tools "/rg")))
            (when (= arm "arm1")
              (fs/copy (str tools "/rg") (str tools "/clj-surgeon"))
              (spit (str run-dir "/run.json") "old success")
              (is (thrown-with-msg? Exception #"preflight: arm1 must not see clj-surgeon"
                    (setup! tasks plan cfg input-paths)))
              (doseq [file ["codex-home/.profile" "preflight.txt" "ws/AGENTS.md"]]
                (is (fs/regular-file? (str run-dir "/" file))))
              (doseq [file ["codex-home/auth.json" "codex-home/config.toml" "run.json"]]
                (is (not (fs/exists? (str run-dir "/" file)))))
              (fs/delete (str tools "/clj-surgeon")))
            (fs/delete (str tmp "/arms/" arm ".md"))
            (fs/create-sym-link (str tmp "/arms/" arm ".md") arm-file)
            (is (thrown-with-msg? Exception #"arm file must be under script-dir/arms"
                  (setup! tasks plan cfg input-paths)))))))))
;; Directory/missing/unreadable credentials and invalid TOML values must refuse before copying.
(deftest credential-refusals
  (let [tmp (str (fs/create-temp-dir {:prefix "nc-credential-"}))
        auth (str tmp "/credential.json") home (str (fs/create-dirs (str tmp "/home")))
        cfg {:model "test" :reasoning "medium"} root? (= "0" (str/trim (checked! ["id" "-u"])))]
    (try
      (spit auth "fixture")
      (fs/set-posix-file-permissions auth "---------")
      (when root? (binding [*out* *err*] (println "SKIP unreadable credential: uid 0 bypasses file permissions")))
      (doseq [source (cond-> [tmp (str tmp "/missing-credential.json")] (not root?) (conj auth))]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
              (re-pattern (str "credential.*regular readable file: " (java.util.regex.Pattern/quote source)))
              (install-credential! source home cfg)))
        (is (empty? (fs/list-dir home))))
      (fs/set-posix-file-permissions auth "rw-------")
      (doseq [key [:model :reasoning]
              value (cons nil (map #(str "bad" (char %)) (concat (range 32) (range 127 160))))]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo (re-pattern (name key))
              (install-credential! auth home (assoc cfg key value))))
        (is (empty? (fs/list-dir home))))
      (install-credential! auth home (assoc cfg :model "test\"\\λ"))
      (install-credential! auth home (assoc cfg :model "test\"\\λ"))
      (is (= "model = \"test\\\"\\\\λ\"\nmodel_reasoning_effort = \"medium\"\n"
             (slurp (str home "/config.toml"))))
      (finally (fs/delete-tree tmp)))))

;; Independent 2a-iii probe regressions: missing invoker tools and directory-valued kondo.
(deftest install-tools-refusals
  (let [tmp (str (fs/create-temp-dir {:prefix "nc-tools-"}))
        which fs/which
        cfg {:kondo-path (str (which "bb")) :tool-dirs []}]
    (try
      (doseq [[missing arms] [["java" ["arm1" "arm2" "arm3"]] ["bb" ["arm2" "arm3"]]]
              arm arms]
        (with-redefs [fs/which (fn [name & opts] (when-not (= missing name) (apply which name opts)))]
          (is (thrown-with-msg? Exception (re-pattern (str "preflight: " missing " not found"))
                (install-arm-tools! cfg arm "wrapper" tmp))))
        (is (empty? (fs/list-dir tmp))))
      (with-redefs [fs/which (fn [name & opts] (when-not (= "bb" name) (apply which name opts)))]
        (doseq [arm ["arm1" "arm2" "arm3"]]
          (is (string? (install-arm-tools! cfg arm "" tmp)))))
      (spit (str tmp "/not-executable") "fixture")
      (doseq [kondo [nil tmp (str tmp "/not-executable") (str tmp "/missing")]]
        (is (thrown-with-msg? Exception #"preflight: clj-kondo not found"
              (install-arm-tools! (assoc cfg :kondo-path kondo) "arm2" "wrapper" tmp)))
        (is (= ["not-executable"] (mapv fs/file-name (fs/list-dir tmp)))))
      (finally
        (fs/delete-tree tmp)))))

(deftest arm-tools-profile-and-preflight
  (let [tmp (str (fs/create-temp-dir {:prefix "nc-preflight-"}))
        tools (str tmp "/tools'quoted")
        home (str tmp "/home")
        bin (str tmp "/bin")
        wrapper "#!/bin/sh\nexit 0\n"
        mode #(fs/posix->str (fs/posix-file-permissions %))]
    (try
      (doseq [dir [tools home]] (fs/create-dirs dir))
      (doseq [name ["clj-kondo" "rg"]]
        (spit (str tools "/" name) wrapper)
        (fs/set-posix-file-permissions (str tools "/" name) "rwxr-xr-x"))
      (doseq [arm ["arm1" "arm2" "arm3"] kondo [nil tools (str tools "/clj-kondo")]]
        (fs/delete-tree bin)
        (fs/create-dirs bin)
        (let [cfg {:kondo-path kondo :tool-dirs [tools "/usr/bin" "/bin"]}
              path (install-arm-tools! cfg arm wrapper bin)
              wrapped? (not= arm "arm1")]
          (is (= (str bin ":" tools ":/usr/bin:/bin:" (fs/parent (fs/which "java"))) path))
          (write-profile! home path)
          (is (= path (checked! ["env" "-i" (str "HOME=" home) (str "PATH=" path)
                                 "bash" "-lc" "printf %s \"$PATH\""])))
          (is (= (slurp (str home "/.profile")) (slurp (str home "/.bashrc"))))
          (is (= "rwx------" (mode home)))
          (is (nil? (preflight! arm wrapper bin home path tmp "fixture-config")))
          (is (= (str "resolved_clj_surgeon=" (when wrapped? (str bin "/clj-surgeon"))
                      "\nresolved_clj_kondo=" (if wrapped? bin tools) "/clj-kondo"
                      "\nresolved_rg=" tools "/rg\nbb_config=fixture-config\n")
                 (slurp (str tmp "/preflight.txt"))))
          (is (thrown-with-msg? Exception #"Unsupported preflight command"
                (resolve-on-path home path "rg; false")))
          (doseq [name ["clj-surgeon" "clj-kondo" "bb"]]
            (is (= wrapped? (fs/executable? (str bin "/" name))))
            (is (= (and wrapped? (not= name "clj-surgeon")) (fs/sym-link? (str bin "/" name)))))
          (when wrapped?
            (is (= "rwxr-xr-x" (mode (str bin "/clj-surgeon"))))
            (is (= wrapper (slurp (str bin "/clj-surgeon"))))
            (is (= (str (fs/which "bb")) (str (fs/read-link (str bin "/bb")))))
            (is (= (str tools "/clj-kondo") (str (fs/read-link (str bin "/clj-kondo")))))
            (is (thrown-with-msg? Exception #"preflight: arm1 must not see clj-surgeon"
                  (preflight! "arm1" "" bin home path tmp "fixture-config"))))))
      (finally
        (fs/delete-tree tmp)))))

(deftest launch-values
  (doseq [value ["abc" "0" "3601" "9223372036854775808" "-1" "+1" "1.5" " 9" "9\n"]]
    (is (= {:error "NC_KILL_AFTER_S must be an integer 1..3600"} (env-config {"NC_KILL_AFTER_S" value}))))
  (doseq [[wrapper roots] [[nil "[]"] ["" "[]"]
                           ["w" "[\"/h/.local/state/clj-surgeon\",\"/h/.local/state/diagnose-skiff-cpu-memory\"]"]]]
    (is (= ["/timeout" "--kill-after=1" "2" "/codex" "exec" "--json" "--ephemeral"
            "--ignore-user-config" "--skip-git-repo-check" "--sandbox" "workspace-write" "--color" "never" "-c"
            (str "sandbox_workspace_write.writable_roots=" roots)
            "-m" "model" "-c" "model_reasoning_effort=\"high\"" "-C" "/ws" "-o" "/r/answer.txt" "prompt"]
           (codex-argv {:timeout-path "/timeout" :timeout "2" :kill-after "1" :codex "/codex" :wrapper wrapper
                        :sandbox "workspace-write" :invoker-home "/h" :model "model" :reasoning "high"}
                       "/ws" "/r" "prompt"))))
  (doseq [locale [{} {"LANG" "C"} {"LC_ALL" "C.UTF-8"} {"LANG" "C" "LC_ALL" "C.UTF-8"} {"LANG" "" "LC_ALL" ""}]]
    (is (= (merge (into {} (filter (comp seq val)) locale) {"HOME" "/h" "CODEX_HOME" "/h" "PATH" "/p" "TERM" "dumb"})
           (codex-env (env-config (assoc locale "BASH_ENV" "poison")) "/h" "/p")))))
(deftest launch-integration
  (with-workspace-fixture
    (fn [{:keys [tmp repo arm-file git!]}]
      (let [tasks (str tmp "/tasks.edn") auth (str tmp "/auth") stub (str tmp "/codex")
            plan (:ok (validate-run "T-arm1-r1" "T" "arm1" "1"))
            cfg (assoc (env-config {"NC_TIMEOUT_S" "1" "NC_KILL_AFTER_S" "1"})
                       :root tmp :auth auth :codex stub :script-dir tmp
                       :tool-dirs [(str (fs/parent (fs/which "rg"))) "/usr/bin" "/bin"])
            {:keys [run-dir ws codex-home]} (run-paths tmp plan)
            target (str run-dir "/run.json") read-json #(json/parse-string (slurp %) true)
            process p/process move fs/move delete fs/delete-tree
            bounded #(let [proc (process %1 %2)] (delay (or (deref proc 20000 nil)
                                  (do (doseq [p (reverse (cons (:proc proc) (.toArray (.descendants (:proc proc)))))]
                                        (.destroyForcibly p))
                                      (throw (ex-info "launch-integration: process deadline exceeded" {}))))))]
        (fs/create-dirs (str tmp "/arms"))
        (fs/copy arm-file (str tmp "/" (schedule/task-field {} "arm1")))
        (spit tasks (pr-str [{:id "T" :repo repo :sha (str/trim (git! "rev-parse" "HEAD"))}]))
        (spit auth "{}")
        (doseq [mode [:exit :resume :corrupt :timeout :setup :launch :finish :cleanup-ws :cleanup-home]]
          (when-not (#{:resume :corrupt} mode) (fs/delete-tree run-dir))
          (doseq [dir [ws codex-home]] (fs/create-dirs dir))
          (when (#{:corrupt :cleanup-ws :cleanup-home} mode)
            (spit target (if (= :corrupt mode) "unreadable" "{\"exit_code\":0}")))
          (spit stub (str "#!/bin/bash\nwhile [[ $1 != -o ]]; do shift; done\n"
                          "echo answer > \"$2\"\nd=${2%/*}\nenv > \"$d/env.txt\"\necho x >> \"$d/marker\"\n"
                          (if (= :timeout mode) "trap '' TERM\nwhile :; do sleep 1; done\n" "exit 7\n")))
          (fs/set-posix-file-permissions stub "rwx------")
          (let [start (System/currentTimeMillis) err (java.io.StringWriter.) moves (atom 0)
                failed-dir (case mode :cleanup-ws ws :cleanup-home codex-home nil)
                child-env (assoc (into {} (System/getenv)) "BASH_ENV" "/nc-poison")
                child-expr (pr-str `(do (load-file ~*file*) (prn (run-one! ~tasks ~plan ~cfg))))
                result (binding [*err* err]
                         (with-redefs [p/process (fn [argv opts]
                                                   (if (and (= :launch mode)
                                                            (= "timeout" (fs/file-name (first argv))))
                                                     (throw (ex-info "launch" {})) (bounded argv opts)))
                                       fs/move (fn [src dst opts]
                                                 (swap! moves inc)
                                                 (is (= [(str target ".tmp") target {:atomic-move true}]
                                                        [src dst opts]))
                                                 (is (integer? (:exit_code (read-json src))))
                                                 (when (= :finish mode) (throw (ex-info "finish" {})))
                                                 (move src dst opts))
                                       fs/delete-tree (fn [dir]
                                                        (if (= dir failed-dir) (throw (ex-info "cleanup" {}))
                                                            (delete dir)))]
                           (if (= :exit mode)
                             (edn/read-string (:out @(bounded [(str (fs/which "bb")) "-e" child-expr]
                                                             {:env child-env :out :string :err :string})))
                             (run-one! tasks plan (cond-> cfg (= :setup mode) (assoc :auth "missing"))))))
                rc (:exit-code result) failure? (#{:setup :launch :finish} mode)]
            (is (= (if (#{:timeout :finish} mode) 1 0) @moves))
            (is (contains? (case mode :timeout #{124 137} (:setup :launch :corrupt) #{3}
                                 (:cleanup-ws :cleanup-home) #{1} #{7}) rc) (pr-str result))
            (is (= (boolean failure?) (fs/exists? (str run-dir "/failure.json"))))
            (is (= (mapv #(= failed-dir %) [ws codex-home]) (mapv fs/exists? [ws codex-home])))
            (when failed-dir (is (str/includes? (str err) "cleanup")))
            (if failure?
              (is (= [rc (= :setup mode)] ((juxt :exit_code :failed_before_run)
                                         (read-json (str run-dir "/failure.json")))))
              (when (#{:exit :resume :timeout} mode)
                (is (= [rc "/usr/bin/timeout" "1"]
                       ((juxt :exit_code :timeout_method :kill_after_s) (read-json target))))
                (is (= ["answer\n" "x\n"] (mapv #(slurp (str run-dir "/" %)) ["answer.txt" "marker"])))
                (is (not (str/includes? (slurp (str run-dir "/env.txt")) "BASH_ENV=")))
                (is (contains? (set (str/split-lines (slurp (str run-dir "/env.txt"))))
                               (str "HOME=" codex-home)))))
            (when (= :timeout mode) (is (< (- (System/currentTimeMillis) start) 5000)))))))))
(deftest run-arguments
  (doseq [args [[] ["tasks"] ["tasks" "id" "T" "arm1"] ["tasks" "id" "T" "arm1" "1" "extra"]]]
    (is (= {:error "Usage: run TASKS.edn RUN_ID TASK_ID ARM REP"} (run-decision args))))
  (is (= {:ok {:run-id "T-arm1-r1" :task "T" :arm "arm1" :rep 1}}
         (run-decision ["tasks" "T-arm1-r1" "T" "arm1" "1"])))
  (is (= {:error "task_id: expected a safe path component, excluding . and .."}
         (run-decision ["tasks" "../x-arm1-r1" "../x" "arm1" "1"]))))
(deftest pr75-schedule-order-and-receipt-failure-tally
  (let [rows (mapv #(-> (validate-run (str "T-arm" % "-r1") "T" (str "arm" %) "1") :ok) [1 2 3])]
    (doseq [codes [[0 7 0] [0 0 0] [3 7 1] []]]
      (let [calls (atom [])]
        (is (= (tally (mapv #(hash-map :exit-code %) codes))
               (run-schedule! (take (count codes) rows) nil
                              #(do (swap! calls conj %) {:exit-code (codes (dec (count @calls)))}))))
        (is (= (take (count codes) rows) @calls))))
    (let [root (str (fs/create-temp-dir)) dir (:run-dir (run-paths root (second rows)))
          write! spit mkdir! fs/create-dirs fail! (fn [& _] (throw (ex-info "unwritable" {})))]
      (try
        (doseq [mode [:write :mkdir :receipt :completed]]
          (let [err (java.io.StringWriter.) calls (atom []) broken? (#{:write :mkdir} mode)]
            (when (= :completed mode)
              (fs/delete (str dir "/failure.json")) (spit (str dir "/run.json") "existing"))
            (binding [*err* err]
              (with-redefs [spit (if (= :write mode) fail! write!) fs/create-dirs (if (= :mkdir mode) fail! mkdir!)]
                (is (= {:total 3 :failed 1 :exit 1}
                       (run-schedule! rows root #(do (swap! calls conj %)
                                                    (if (= % (second rows)) (throw (ex-info "escaped" {}))
                                                        {:exit-code 0})))))))
            (is (= rows @calls))
            (is (= (str "T-arm2-r1: escaped\n"
                        (when broken? "T-arm2-r1: failure receipt not written: unwritable\n")) (str err)))
            (is (= (= :receipt mode) (fs/exists? (str dir "/failure.json"))))
            (when (= :receipt mode)
              (is (= (dissoc (failure-receipt (second rows) 3 nil) :ended_at)
                     (dissoc (json/parse-string (slurp (str dir "/failure.json")) true) :ended_at))))))
        (is (= "existing" (slurp (str dir "/run.json"))))
        (finally (fs/delete-tree root))))))
(deftest environment-pure-boundaries
  (let [opts {:which {"codex" "/codex"} :user-home "/invoker" :script-dir "/bench"}]
    (doseq [env [{} {"HOME" ""} {"NC_KILL_AFTER_S" ""}]]
      (is (= ["/invoker/.codex/auth.json" "30" {}] ((juxt :auth :kill-after :locale) (env-config env opts)))))
    (doseq [seconds ["1" "3600"]]
      (is (= ["/custom/.codex/auth.json" seconds {"LANG" "C"} "/invoker"]
             ((juxt :auth :kill-after :locale :invoker-home)
              (env-config {"HOME" "/custom" "NC_KILL_AFTER_S" seconds "LANG" "C" "LC_ALL" ""} opts)))))
    (is (nil? (:codex (env-config {} (assoc opts :which (constantly nil))))))))
(deftest command-dispatch
  (let [header "run_id\ttask\tarm\trep\n" row "T-arm1-r1\tT\tarm1\t1"
        run-args ["tasks" "T-arm1-r1" "T" "arm1" "1"]]
    (doseq [[cmd args inputs cfg code message]
            [["run" [] {} {} 2 "Usage:"] ["schedule" ["tasks"] {} {} 2 "Usage:"]
             ["unknown" [] {} {} 2 "Usage:"]
             ["schedule" ["tasks" "schedule"] {"tasks" :missing} {} 2 "tasks: cannot read tasks: missing"]
             ["schedule" ["tasks" "schedule"] {"tasks" "["} {} 2 "tasks: cannot read tasks:"]
             ["schedule" ["tasks" "schedule"] {"schedule" :missing} {} 2 "schedule: cannot read schedule: missing"]
             ["schedule" ["tasks" "schedule"] {"schedule" "bad"} {} 2 "line 1:"]
             ["schedule" ["tasks" "schedule"]
              {"schedule" (str header row "\n\nX-arm1-r1\tX\tarm1\t1")} {} 2 "line 4: task_id: unknown X"]
             ["schedule" ["tasks" "schedule"] {"schedule" (str header "../x-arm1-r1\t../x\tarm1\t1")}
              {} 2 "line 2: task_id: expected a safe path component"]
             ["run" run-args {} {:error "config"} 2 "config"]
             ["run" run-args {} {} 7 ""] ["run" run-args {} {:throw true} 3 "escaped"]
             ["schedule" ["tasks" "schedule"] {} {} 1 ""]]]
      (let [calls (atom []) reads (atom []) err (java.io.StringWriter.)]
        (with-redefs [env-config (constantly cfg)
                      slurp #(let [v (get (merge {"tasks" "[{:id \"T\"}]" "schedule" (str header row)} inputs) %)]
                               (swap! reads conj %)
                               (if (= :missing v) (throw (java.io.FileNotFoundException. "missing")) v))
                      run-one! (fn [_ plan cfg]
                                 (swap! calls conj plan)
                                 (is (= [{:id "T"}] (:tasks cfg)))
                                 (if (:throw cfg) (throw (ex-info "escaped" {})) {:exit-code 7}))]
          (binding [*err* err]
            (let [out (with-out-str (is (= code (run-command! cmd args))))]
              (is (= (if (= code 1) "schedule: 1 runs, 1 failed\n" "") out))))
          (is (str/starts-with? (str err) message))
          (is (= (if (= code 2) 0 1) (count @calls)))
          (when (seq @calls) (is (= 1 (count (filter #{"tasks"} @reads))))))))))
(deftest real-entry-subprocess
  (let [root (fs/create-temp-dir) tasks (str root "/tasks.edn") schedule (str root "/schedule.tsv")
        script (str (fs/absolutize *file*)) codex (str root "/codex") auth (str root "/auth.json")]
    (try
      (doseq [[path content] [[codex "#!/bin/sh\nexit 0\n"] [auth "{}"] [tasks "[]"]
                             [schedule "run_id\ttask\tarm\trep\n"]]] (spit path content))
      (fs/set-posix-file-permissions codex "rwx------")
      (doseq [[args code out err] [[["run" "a" "b"] 2 "" "Usage: run TASKS.edn RUN_ID TASK_ID ARM REP\n"]
                                 [["schedule" tasks schedule] 1 "schedule: 0 runs (nothing to run)\n" ""]]]
        (is (= [code out err]
               ((juxt :exit :out :err)
                @(p/process (into [(str (fs/which "bb")) script] args)
                            {:in "" :out :string :err :string :dir (str root)
                             :env {"PATH" "/usr/bin:/bin" "HOME" (str root) "NC_CODEX" codex
                                   "NC_CODEX_AUTH" auth "NC_RESULT_ROOT" (str root)}})))))
      (finally (fs/delete-tree root)))))
(when (entry-script? (System/getProperty "babashka.file") *file*)
  (case (vec *command-line-args*)
    ["--self-test"]
    (let [r (run-tests 'native-control.run)] (exit-with (if (zero? (+ (:fail r) (:error r))) 0 1)))
    ["config"]
    (let [cfg (env-config (System/getenv)) rc (decide-config-exit cfg)]
      (binding [*out* (if (zero? rc) *out* *err*)] (println (or (:error cfg) (render-config cfg))))
      (exit-with rc))
    (exit-with (run-command! (first *command-line-args*) (rest *command-line-args*)))))
