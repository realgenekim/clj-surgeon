#!/usr/bin/env bb
;; Standalone script: retain the sibling scripts' namespace and hyphenated directory convention.
#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns native-control.run
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]))

(def script-dir (str (fs/parent *file*)))
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

(defn git-env []
  (apply dissoc (assoc (into {} (System/getenv)) "GIT_CONFIG_GLOBAL" "/dev/null" "GIT_CONFIG_NOSYSTEM" "1")
         ["GIT_DIR" "GIT_WORK_TREE" "GIT_INDEX_FILE" "GIT_OBJECT_DIRECTORY"
          "GIT_AUTHOR_NAME" "GIT_AUTHOR_EMAIL" "GIT_COMMITTER_NAME" "GIT_COMMITTER_EMAIL"]))

(defn checked! [argv]
  (when-not (and (vector? argv) (every? string? argv))
    (throw (ex-info "argv must be a vector of strings" {})))
  (:out (p/check @(p/process argv {:in "" :out :string :err :string :env (git-env)}))))

(defn ensure-root! [root-str]
  (when-not (and (string? root-str) (not (str/blank? root-str)))
    (throw (ex-info "root must name a directory" {})))
  (when-not (fs/exists? root-str)
    (fs/create-dirs root-str {:posix-file-permissions "rwx------"}))
  (when-not (fs/directory? root-str) (throw (ex-info "root must be a directory" {})))
  (str (fs/canonicalize root-str)))

(defn materialize-workspace! [{:keys [repo sha]} arm-file ws]
  (doseq [[valid message] [[(and (string? sha) (re-matches #"[0-9a-f]{40}" sha)) "sha must be 40 lowercase hex digits"]
                          [(and repo (fs/directory? repo)) "repo directory must exist"]
                          [(and arm-file (fs/regular-file? arm-file)) "arm file must exist"]]]
    (when-not valid (throw (ex-info message {}))))
  (when (fs/starts-with? (fs/canonicalize repo) (fs/canonicalize ws))
    (throw (ex-info "ws must not equal repo or be its ancestor" {})))
  (fs/delete-tree ws)
  (fs/create-dirs ws)
  (doseq [result (mapv deref (p/pipeline
                               (p/pb ["git" "-C" repo "archive" "--end-of-options" sha]
                                     {:in "" :err :string :env (git-env)})
                               (p/pb ["tar" "-x" "-C" ws] {:out :string :err :string})))]
    (p/check result))
  (let [visit (fn [f _]
                (if (#{"AGENTS.md" "CLAUDE.md" ".codex" ".clj-surgeon.edn" ".mcp.json"} (fs/file-name f))
                  (do
                    (fs/delete-tree f)
                    :skip-subtree)
                  :continue))
        git! (fn [& args] (checked! (into ["git" "-C" ws] args)))]
    (fs/walk-file-tree ws {:pre-visit-dir visit :visit-file visit})
    (fs/copy arm-file (str ws "/AGENTS.md"))
    (git! "init" "-q")
    (git! "add" "-A" "-f")
    (git! "-c" "user.name=native-control" "-c" "user.email=native-control@anvil"
          "-c" "commit.gpgsign=false" "commit" "-qm" "Frozen read experiment")
    (str/trim (git! "rev-parse" "HEAD"))))

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

(deftest checked-process-streams-and-failure
  (doseq [argv ["echo unsafe" '("echo") ["echo" nil] nil]]
    (with-redefs [p/process (fn [& _] (throw (AssertionError. "process ran")))]
      (is (thrown-with-msg? Exception #"vector of strings" (checked! argv)))))
  (is (= "" (checked! ["sh" "-c" "cat"])))
  (is (= "literal $value\n" (checked! ["printf" "%s\n" "literal $value"])))
  (is (thrown-with-msg? Exception #"process stderr"
        (checked! ["sh" "-c" "printf 'process stderr' >&2; exit 7"]))))

(deftest result-root-permissions
  (let [tmp (fs/create-temp-dir {:prefix "nc-root-"})
        root (str tmp "/results")
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
      (finally
        (fs/delete-tree tmp)))))

;; Reduced from the PR 35 setup integration fixture: inherited instructions and ignored files.
(deftest materialize-workspace-integration-requires-git
  (doseq [tool ["git" "tar"]]
    (is (some? (fs/which tool)) (str "Missing integration prerequisite: " tool))
    (when-not (fs/which tool)
      (throw (ex-info (str "Missing integration prerequisite: " tool) {}))))
  (let [tmp (str (fs/create-temp-dir {:prefix "nc-materialize-"}))
        repo (str tmp "/repo")
        ws (str tmp "/ws")
        arm-file (str tmp "/arm.md")
        arm-text "Native-control fixture instructions: λ\n"
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
      (let [task {:repo repo :sha (str/trim (git! "rev-parse" "HEAD"))}]
        ;; Independent pre-review probes: refusal must precede deletion and process launch.
        (doseq [[t arm dest message] [[(assoc task :sha "--output=x") arm-file ws #"sha"]
                                    [(assoc task :sha "HEAD") arm-file ws #"sha"]
                                    [task (str tmp "/missing") ws #"arm file"]
                                    [(assoc task :repo arm-file) arm-file ws #"repo directory"]
                                    [(assoc task :repo (str tmp "/missing")) arm-file ws #"repo directory"]
                                    [task arm-file repo #"ancestor"]
                                    [task arm-file tmp #"ancestor"]]]
          (let [unexpected (fn [& _] (throw (AssertionError. "side effect before refusal")))]
            (with-redefs [fs/delete-tree unexpected p/process unexpected p/pipeline unexpected]
              (is (thrown-with-msg? Exception message (materialize-workspace! t arm dest)))))
          (is (= "stale workspace" (slurp (str ws "/stale.txt")))))
        (let [expr (list 'do (list 'load-file (str (fs/absolutize *file*)))
                         (list 'print (list 'native-control.run/materialize-workspace! task arm-file ws)))
              baseline (:out (p/check @(p/process ["bb" "-e" (pr-str expr)]
                                                 {:extra-env {"GIT_DIR" (str repo "/.git")
                                                              "GIT_WORK_TREE" repo
                                                              "GIT_INDEX_FILE" (str repo "/.git/index")
                                                              "GIT_OBJECT_DIRECTORY" (str repo "/.git/objects")
                                                              "GIT_CONFIG_GLOBAL" arm-file "GIT_CONFIG_NOSYSTEM" "0"}
                                                  :out :string :err :string})))]
          (is (= (:sha task) (str/trim (git! "rev-parse" "HEAD"))))
          (is (fs/directory? (str ws "/.git")))
          (is (= baseline (str/trim (workspace-git! "rev-parse" "HEAD"))))
          (is (= "commit\n" (workspace-git! "cat-file" "-t" baseline))))
        (is (= arm-text (slurp (str ws "/AGENTS.md"))))
        (is (= "frozen source\n" (slurp (str ws "/tracked.txt"))))
        (is (= "forced into baseline\n" (workspace-git! "show" "HEAD:ignored.txt")))
        (is (not (fs/exists? (str ws "/stale.txt"))))
        (is (empty? (fs/list-dir (str ws "/nested"))))
        (is (empty? (filter #(#{"CLAUDE.md" ".codex"} (fs/file-name %))
                           (fs/glob ws "**" {:hidden true}))))
        (is (= "native-control <native-control@anvil>\nnative-control <native-control@anvil>\n"
               (workspace-git! "log" "-1" "--format=%an <%ae>%n%cn <%ce>")))
        (is (= "Frozen read experiment\n" (workspace-git! "log" "-1" "--format=%s")))
        (is (= arm-text (workspace-git! "show" "HEAD:AGENTS.md")))
        (is (= "" (workspace-git! "status" "--porcelain" "--ignored" "--untracked-files=all")))
        (let [pb p/pb]
          (with-redefs [p/pb (fn [argv opts]
                               (pb (if (= "tar" (first argv))
                                     ["sh" "-c" "cat >/dev/null; echo tar-failed >&2; exit 9"]
                                     argv)
                                   opts))]
            (is (thrown-with-msg? Exception #"tar-failed" (materialize-workspace! task arm-file ws)))))
        (is (not (fs/exists? (str ws "/.git")))))
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
