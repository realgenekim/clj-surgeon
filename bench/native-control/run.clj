#!/usr/bin/env bb
;; Standalone script: retain the sibling scripts' namespace and hyphenated directory convention.
#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns native-control.run
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
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
              :keep (value "NC_KEEP" "0")
              :sandbox (value "NC_SANDBOX" "workspace-write")
              :config (value "NC_SURGEON_BB_CONFIG" (str script-dir "/../../bb.edn"))
              :auth (value "NC_CODEX_AUTH" (str (value "HOME" user-home) "/.codex/auth.json"))
              :codex (value "NC_CODEX" (some-> (which "codex") str))
              :invoker-home user-home
              :kondo-path (find-kondo which "/usr/local/bin/clj-kondo")
              :tool-dirs ["/usr/bin" "/bin"]}
         timeout (when (re-matches #"[0-9]+" (:timeout cfg)) (parse-long (:timeout cfg)))]
     (if (and timeout (<= 1 timeout 86400))
       cfg
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
  (run! p/check (mapv deref (p/pipeline (p/pb ["git" "-C" repo "archive" "--end-of-options" sha]
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
  (let [t (try (schedule/find-task (edn/read-string (slurp tasks-path)) task)
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
                 :invoker-home :kondo-path :tool-dirs))))

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

(when (entry-script? (System/getProperty "babashka.file") *file*)
  (case (vec *command-line-args*)
    ["--self-test"]
    (let [r (run-tests 'native-control.run)] (exit-with (if (zero? (+ (:fail r) (:error r))) 0 1)))
    ["config"]
    (let [cfg (env-config (System/getenv)) rc (decide-config-exit cfg)]
      (binding [*out* (if (zero? rc) *out* *err*)] (println (or (:error cfg) (render-config cfg))))
      (exit-with rc))
    (do (binding [*out* *err*] (println "Usage: bb bench/native-control/run.clj --self-test | config"))
        (exit-with 2))))
