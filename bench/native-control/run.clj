#!/usr/bin/env bb
;; Standalone script: retain the sibling scripts' namespace and hyphenated directory convention.
#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns native-control.run
(:require [clojure.string :as str] [clojure.test :refer [deftest is run-tests]] [clojure.edn :as edn] [cheshire.core
  :as json] [babashka.fs :as fs] [babashka.process :as p])) (def script-dir (str (fs/parent *file*))) (load-file (str
  script-dir "/schedule.clj")) (require '[native-control.schedule :as schedule])
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
(defn parse-schedule [text]
  (let [[header & lines] (str/split-lines text) seen (volatile! #{})]
    (if (not= "run_id\ttask\tarm\trep" header)
      {:error "line 1: expected header run_id<TAB>task<TAB>arm<TAB>rep"}
      (reduce (fn [acc [i line]]
                (if (str/blank? line) acc
                    (let [cells (str/split line #"\t" -1)
                          r (cond (not= 4 (count cells)) {:error "expected 4 tab-separated cells"}
                                  (@seen (first cells)) {:error "run_id: duplicate"}
                                  :else (apply validate-run cells))]
                      (if-let [e (:error r)] (reduced {:error (str "line " (+ i 2) ": " e)})
                              (do (vswap! seen conj (first cells)) (update acc :rows conj (:ok r)))))))
              {:rows []} (map-indexed vector lines)))))
(defn run-paths [root {:keys [run-id task arm rep] :as plan}]
  (when-not (and (integer? rep) (= plan (:ok (validate-run run-id task arm (str rep)))))
    (throw (ex-info "Expected a validated plan" {})))
  (let [dir (str root "/runs/" run-id)]
    {:run-dir dir :ws (str dir "/ws") :codex-home (str dir "/codex-home") :bin-dir (str dir "/bin")}))
(defn error-message [e] (or (.getMessage e) (.getName (class e))))
(defn with-cleanup [result cleanup!] (try (cleanup!) result (catch Throwable e
    (assoc result :exit-code (max 1 (:exit-code result)) :cleanup-error (error-message e)))))
(defn guarded-run [{:keys [setup! run! finish! cleanup! rc-atom]}]
  (let [result (atom {:exit-code 3 :failed-before-run true})]
    (try
      (setup!)
      (let [ran (select-keys (run!) [:exit-code :detail]) rc (:exit-code ran)]
        (when-not (and (integer? rc) (<= 0 rc)) (throw (ex-info "run returned no exit code" {})))
        (reset! result (assoc ran :failed-before-run false))
        (when rc-atom (reset! rc-atom rc)) (finish! ran))
      (catch Throwable e (swap! result #(assoc % :exit-code (max 1 (:exit-code %)) :error (error-message e))))
      (finally
        (reset! result (with-cleanup @result cleanup!))))
    @result))
(defn failure-receipt [{:keys [run-id task arm rep]} rc ended-at launched?] {:run_id run-id :task task :arm arm :rep
  rep :exit_code (max 1 (or rc 3)) :failed_before_run (not launched?) :ended_at ended-at})
(defn tally [results]
  (let [n (count (remove #(zero? (:exit-code %)) results))]
    {:total (count results) :failed n :exit (if (zero? n) 0 1)}))
(defn exit-with [n] (System/exit n))
(defn entry-script? [file-prop file] (= file-prop file))
(defn env-config [env] (let [cfg (into {:invoker-home (System/getProperty "user.home")} (for [[k suffix default]
  [[:root "RESULT_ROOT" "/var/tmp/forge/native-control"] [:model "MODEL" "gpt-6-astra"] [:reasoning "REASONING"
  "medium"] [:timeout "TIMEOUT_S" "900"] [:keep "KEEP" "0"] [:sandbox "SANDBOX" "workspace-write"] [:config
  "SURGEON_BB_CONFIG" (str script-dir "/../../bb.edn")] [:codex "CODEX" (some-> (fs/which "codex") str)] [:auth
  "CODEX_AUTH" (str (get env "HOME") "/.codex/auth.json")]]] [k (let [v (get env (str "NC_" suffix))] (if (seq v) v
  default))]))] (when-not (re-matches #"[1-9][0-9]*" (:timeout cfg)) (throw (ex-info
  "NC_TIMEOUT_S must be a positive integer" {:exit-code 2}))) cfg))
(defn codex-argv [{:keys [codex model reasoning sandbox invoker-home wrapper]} ws dir prompt] [codex "exec" "--json"
  "--ephemeral" "--ignore-user-config" "--skip-git-repo-check" "--sandbox" sandbox "--color" "never" "-c" (str
  "sandbox_workspace_write.writable_roots="
  ;; Analyzer admission uses passwd home; allow its shared lock/events outside the experiment tree.
  (json/generate-string (mapv #(str invoker-home "/.local/state/" %) (when (seq wrapper) ["clj-surgeon"
  "diagnose-skiff-cpu-memory"])))) "-m" model "-c" (str "model_reasoning_effort=" (pr-str reasoning)) "-C" ws "-o" (str
  dir "/answer.txt") prompt])
(defn normalize-wrapper [r w bin] (if (and (seq w) (= r (str bin "/clj-surgeon"))) :wrapper r))
(defn preflight-decision [arm resolved kondo? rg?] (cond (and (= arm "arm1") (not (str/blank? (str resolved)))) (str
  "preflight: arm1 must not see clj-surgeon, found '" resolved "'") (and (not= arm "arm1") (not= :wrapper resolved))
  (str "preflight: clj-surgeon resolves to '" resolved "', not the arm wrapper") (and (not= arm "arm1") (not kondo?))
  "preflight: clj-kondo not on the arm PATH" (not rg?)
  "preflight: rg not on the arm PATH (every arm needs the native route)" :else :ok))
(defn checked! [argv] (:out (p/check @(p/process argv {:in "" :out :string :err :string}))))
(defn delete-dirs! [dirs] (let [errors (keep #(try (fs/delete-tree %) nil (catch Throwable e (str % ": " (error-message
  e)))) dirs)] (when (seq errors) (throw (ex-info (str/join "; " errors) {})))))
(defn now [] (str (java.time.Instant/now)))
(defn run-one! [tasks {:keys [run-id task arm rep] :as plan} cfg] (let [root (str (fs/canonicalize (:root cfg))) {dir
  :run-dir ws :ws home :codex-home bins :bin-dir} (run-paths root plan) bin (str bins "/" arm) target (str dir
  "/run.json") ctx (atom {}) rc (atom nil) launched (atom false) cleanup! #(when-not (= "1" (:keep cfg)) (delete-dirs!
  [ws home])) git! (fn [& args] (checked! (into ["git" "-C" ws] args)))] (if (fs/exists? target)
      (with-cleanup (try (let [rc (:exit_code (json/parse-string (slurp target) true))]
        (when-not (integer? rc) (throw (ex-info "run.json requires an integer exit_code" {})))
        (println (str "already complete (exit " rc ")")) {:exit-code rc})
        (catch Throwable e {:exit-code 3 :error (str "Cannot read run.json exit_code: " (error-message e))})) cleanup!)
      (guarded-run {:rc-atom rc :setup! (fn [] (when-not
  (fs/exists? root) (fs/create-dirs root) (fs/set-posix-file-permissions root "rwx------")) (doseq [dir [ws home bins]]
  (fs/delete-tree dir) (fs/create-dirs dir)) (fs/set-posix-file-permissions home "rwx------") (fs/create-dirs bin)
  (doseq [name ["answer.txt" "failure.json" "run.json.tmp"]] (fs/delete-if-exists (str dir "/" name))) (let [timeout
  (or (fs/which "timeout") (throw (ex-info "Linux prerequisite: timeout required on PATH" {}))) t (schedule/find-task
  (edn/read-string (slurp tasks)) task) config (str (fs/canonicalize (:config cfg))) wrapper (schedule/wrapper arm
  config) arm-file (or (get t (keyword arm)) (str script-dir "/" (schedule/task-field t arm))) path (str bin
  ":/usr/bin:/bin:" (fs/parent (fs/which "java"))) resolve! #(str/trim (checked! ["env" "-i" (str "HOME=" home) (str
  "PATH=" path) "bash" "-lc" (str "command -v " % " || true")]))] (doseq [result (mapv deref (p/pipeline (p/pb ["git"
  "-C" (:repo t) "archive" (:sha t)] {:err :string}) (p/pb ["tar" "-x" "-C" ws] {:out :string :err :string})))]
  (p/check result)) (let [visit (fn [f _] (if (#{"AGENTS.md" "CLAUDE.md" ".codex" ".clj-surgeon.edn" ".mcp.json"}
  (fs/file-name f)) (do (fs/delete-tree f) :skip-subtree) :continue))] (fs/walk-file-tree ws {:pre-visit-dir visit
  :visit-file visit})) (fs/copy arm-file (str ws "/AGENTS.md")) (git! "init" "-q") (git! "add" "-A" "-f") (git! "-c"
  "user.name=native-control" "-c" "user.email=native-control@anvil" "-c" "commit.gpgsign=false" "commit" "-qm"
  "Frozen read experiment") (when (seq wrapper) (spit (str bin "/clj-surgeon") wrapper) (fs/set-posix-file-permissions
  (str bin "/clj-surgeon") "rwxr-xr-x") (fs/create-sym-link (str bin "/clj-kondo") (if (fs/executable?
  "/usr/local/bin/clj-kondo") "/usr/local/bin/clj-kondo" (fs/which "clj-kondo"))) (fs/create-sym-link (str bin "/bb")
  (fs/which "bb"))) (doseq [f [".profile" ".bashrc"]] (spit (str home "/" f) (str "export PATH='" (str/replace path "'"
  "'\\''") "'\n"))) (let [resolved (resolve! "clj-surgeon") decision (preflight-decision arm (normalize-wrapper
  resolved wrapper bin) (seq (resolve! "clj-kondo")) (seq (resolve! "rg")))] (spit (str dir "/preflight.txt") (str
  "resolved_clj_surgeon=" resolved "\nbb_config=" config "\n")) (when-not (= :ok decision) (throw (ex-info decision
  {})))) (fs/copy (:auth cfg) (str home "/auth.json")) (fs/set-posix-file-permissions (str home "/auth.json")
  "rw-------") (spit (str home "/config.toml") (str "model = " (pr-str (:model cfg)) "\nmodel_reasoning_effort = "
  (pr-str (:reasoning cfg)) "\n")) (reset! ctx {:task t :bytes (fs/size arm-file) :path path :timeout (str timeout)
  :wrapper wrapper}))) :run! (fn [] (reset! launched true) (let [{:keys [task path timeout wrapper] :as detail} @ctx
  start (System/currentTimeMillis) began (now) argv (codex-argv (assoc cfg :wrapper wrapper) ws dir
  (schedule/task-prompt task)) result @(p/process (into [timeout (:timeout cfg)] argv) {:in "" :out (fs/file dir
  "events.jsonl") :err (fs/file dir "stderr.txt") :extra-env {"HOME" home "CODEX_HOME" home "PATH" path}})] {:exit-code
  (:exit result) :detail (assoc detail :start-ms start :end-ms (System/currentTimeMillis) :started-at began :ended-at
  (now))})) :finish! (fn [{:keys [exit-code detail]}] (let [{:keys [task bytes start-ms end-ms started-at ended-at]}
  detail status (git! "status" "--porcelain" "--ignored" "--untracked-files=all") receipt (schedule/receipt (mapv str
  [run-id (:task plan) arm rep (:model cfg) (:reasoning cfg) (:repo task) (:sha task) start-ms end-ms exit-code status
  bytes started-at ended-at]))] (spit (str target ".tmp") (json/generate-string (assoc receipt :timeout_method
  (:timeout detail)))) (fs/move (str target ".tmp") target {:atomic-move true}))) :cleanup! (fn [] (try (when-not
  (fs/exists? target) (fs/create-dirs dir) (spit (str dir "/failure.json") (json/generate-string (failure-receipt plan
  @rc (now) @launched)))) (finally (cleanup!))))}))))
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
cleanup-throws? (assoc :exit-code (max 1 rc) :cleanup-error "java.lang.NullPointerException")) result))
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
(failure-receipt {:run-id "T01-arm1-r1" :task "T01" :arm "arm1" :rep 1} nil "end" false)))
  (doseq [[codes expected] [[[] {:total 0 :failed 0 :exit 0}]
                           [[0 0] {:total 2 :failed 0 :exit 0}]
                           [[0 7 3] {:total 3 :failed 2 :exit 1}]]]
    (is (= expected (tally (map #(hash-map :exit-code %) codes))))))
(deftest cli-load-guard
  (is (true? (entry-script? "run.clj" "run.clj")))
  (is (false? (entry-script? "other.clj" "run.clj")))
  (is (false? (entry-script? nil "run.clj"))))
(deftest runner-policies (is (thrown? Exception (env-config {"NC_TIMEOUT_S" "abc"}))) (doseq [[r w e]
  [["/b/clj-surgeon" "w" :wrapper] ["/x" "w" "/x"] ["/b/clj-surgeon" "" "/b/clj-surgeon"]]] (is (= e (normalize-wrapper
  r w "/b")))) (is (= (env-config {"HOME" "/caller"}) (env-config {"HOME" "/caller" "NC_TIMEOUT_S" ""}))) (is (= "900"
  (:timeout (env-config {})))) (is (= "/stub" (:codex (env-config {"NC_CODEX" "/stub"})))) (is (= "/x/codex" (:codex
  (env-config {"NC_CODEX" "/x/codex"})))) (is (= (System/getProperty "user.home") (:invoker-home (env-config {"HOME"
  "/wrong"})))) (is (= "/caller/.codex/auth.json" (:auth (env-config {"HOME" "/caller"})))) (doseq [arm ["arm1" "arm2"
  "arm3"] :let [argv (codex-argv (assoc (env-config {}) :invoker-home "/caller" :wrapper (schedule/wrapper arm
  "/bb.edn")) "/ws" "/run" "prompt")]] (is (= (str "sandbox_workspace_write.writable_roots=" (if (= arm "arm1") "[]"
  (str "[\"/caller/.local/state/clj-surgeon\"," "\"/caller/.local/state/diagnose-skiff-cpu-memory\"]"))) (nth argv
  11))) (is (= ["-C" "/ws" "-o" "/run/answer.txt" "prompt"] (take-last 5 argv)))) (doseq [args [["arm1" "" false true]
  ["arm2" :wrapper true true] ["arm3" :wrapper true true]]] (is (= :ok (apply preflight-decision args)))) (doseq [args
  [["arm1" "/leak" false true] ["arm2" "/leak" true true] ["arm3" :wrapper false true] ["arm1" "" false false]]] (is
  (str/starts-with? (apply preflight-decision args) "preflight:"))) (doseq [code [nil 0 7]] (let [plan {:run-id
  "T-arm1-r1" :task "T" :arm "arm1" :rep 1} written (atom nil) rc (atom nil) result (guarded-run {:rc-atom rc :setup!
  #(when (nil? code) (throw (ex-info "setup" {}))) :run! (constantly {:exit-code code}) :finish! (fn [_] (throw
  (ex-info "finish" {}))) :cleanup! #(reset! written (failure-receipt plan @rc "end" (some? code)))})] (is (= (max 1
  (or code 3)) (:exit-code result))) (is (= [(:exit-code result) (nil? code)] ((juxt :exit_code :failed_before_run)
  @written))))))
(deftest launched-then-throws (let [launched (atom false) receipt (atom nil)] (guarded-run {:setup! (constantly nil)
  :run! #(do (reset! launched true) (throw (ex-info "launched" {}))) :finish! (constantly nil) :cleanup! #(reset!
  receipt (failure-receipt {} nil "end" @launched))}) (is (false? (:failed_before_run @receipt)))))
(deftest independent-cleanup (let [calls (atom [])] (with-redefs [fs/delete-tree #(do (swap! calls conj %) (throw
  (ex-info % {})))] (is (thrown-with-msg? Exception #"ws: ws; home: home" (delete-dirs! ["ws" "home"])))) (is (= ["ws"
  "home"] @calls))))
(deftest run-one-integration (if-let [missing (seq (concat (remove fs/which ["git" "bash" "timeout"]) (when-not (some
  fs/executable? ["/usr/bin/rg" "/bin/rg"]) ["rg in /usr/bin or /bin"])))] (do (binding [*out* *err*] (println (str
  "SKIP run-one-integration: " (str/join ", " missing)))) (is true)) (let [root (str (fs/create-temp-dir)) repo (str
  root "/repo") tasks (str root "/tasks.edn") arm (str root "/arm1.md") stub (str root "/codex") marker (str root
  "/calls") auth (str root "/auth") plan (:ok (validate-run "T-arm1-r1" "T" "arm1" "1")) cfg (assoc (env-config {})
  :root root :auth auth :codex stub) {dir :run-dir ws :ws home :codex-home} (run-paths root plan) target (str dir
  "/run.json") git! #(checked! (into ["git" "-C" repo] %))] (try (fs/create-dirs repo) (spit (str repo "/README")
  "fixture") (spit arm "Use rg; no wrapper.\n") (spit auth "{}") (git! ["init" "-q"]) (git! ["add" "README"]) (git!
  ["-c" "user.name=test" "-c" "user.email=test@local" "-c" "commit.gpgsign=false" "commit" "-qm" "fixture"]) (spit
  tasks (pr-str [{:id "T" :repo repo :sha (str/trim (git! ["rev-parse" "HEAD"])) :arm1 arm}])) (spit stub (str
  "#!/bin/sh\necho call >> '" marker "'\n"
  "while [ $# -gt 0 ]; do if [ \"$1\" = -o ]; then shift; echo answer > \"$1\"; fi; shift; done\n" "exit 7\n"))
  (fs/set-posix-file-permissions stub "rwx------") (fs/set-posix-file-permissions root "rwxr-x---") (is (= 7
  (:exit-code (run-one! tasks plan cfg)))) (is (fs/exists? target)) (is (= "rwxr-x---" (fs/posix->str
  (fs/posix-file-permissions root)))) (is (= 7 (:exit_code (json/parse-string (slurp target) true)))) (is (not-any?
  fs/exists? [ws home (str home "/auth.json")])) (doseq [d [ws home]] (fs/create-dirs d)) (spit (str home "/auth.json")
  "secret") (spit target "{\"exit_code\":0}")
  (let [delete! fs/delete-tree] (with-redefs [fs/delete-tree #(if (= % ws) (throw (ex-info "blocked" {}))
  (delete! %))] (with-out-str (let [r (run-one! tasks plan cfg)] (is (= 1 (:exit-code r))) (is (str/includes?
  (:cleanup-error r) "blocked")))))) (is (not (fs/exists? home))) (spit target "{\"exit_code\":7}")
  (is (= "already complete (exit 7)\n" (with-out-str
  (is (= 7 (:exit-code (run-one! tasks plan cfg))))))) (is (not (fs/exists? ws))) (doseq [bad ["{}"
  "{\"exit_code\":\"7\"}" "{\"exit_code\":7.5}" "broken"]] (spit target bad) (let [r (run-one! tasks plan cfg)] (is (=
  3 (:exit-code r))) (is (seq (:error r))))) (is (= "call\n" (slurp marker))) (finally (fs/delete-tree root))))))
(defn run-command! [[tasks id task arm rep :as args]] (let [{:keys [ok error]} (if (= 5 (count args)) (validate-run id
  task arm rep) {:error "Usage: run TASKS.edn RUN_ID TASK_ID ARM REP"})] (if error (do (binding [*out* *err*] (println
  error)) 2) (let [result (try (run-one! tasks ok (env-config (System/getenv))) (catch Throwable e {:exit-code (or
  (:exit-code (ex-data e)) 3) :error (error-message e)}))] (doseq [e (keep result [:error :cleanup-error])] (binding
  [*out* *err*] (println e))) (:exit-code result)))))
(deftest cleanup-stderr (let [err (java.io.StringWriter.)] (with-redefs [run-one! (fn [& _] {:exit-code 1
  :cleanup-error "cleanup failed"})] (binding [*err* err] (is (= 1 (run-command! ["tasks" "T-arm1-r1" "T" "arm1"
  "1"]))))) (is (= "cleanup failed\n" (str err)))))
(when (entry-script? (System/getProperty "babashka.file") *file*)
  (cond (= ["--self-test"] (vec *command-line-args*))
    (let [r (run-tests 'native-control.run)] (exit-with (if (zero? (+ (:fail r) (:error r))) 0 1)))
    (= "run" (first *command-line-args*)) (exit-with (run-command! (rest *command-line-args*)))
    :else (do (binding [*out* *err*] (println "Usage: run TASKS.edn RUN_ID TASK_ID ARM REP | --self-test"))
      (exit-with 2))))
