#!/usr/bin/env bb
;; Standalone script: retain the sibling scripts' namespace and hyphenated directory convention.
#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns native-control.run
  (:require [babashka.fs :as fs]
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
(defn env-config [env]
  (let [value (fn [k default]
                (let [v (get env k)]
                  (if (seq v) v default)))
        home (System/getProperty "user.home")
        cfg {:root (value "NC_RESULT_ROOT" "/var/tmp/forge/native-control")
             :model (value "NC_MODEL" "gpt-6-astra")
             :reasoning (value "NC_REASONING" "medium")
             :timeout (value "NC_TIMEOUT_S" "900")
             :keep (value "NC_KEEP" "0")
             :sandbox (value "NC_SANDBOX" "workspace-write")
             :config (value "NC_SURGEON_BB_CONFIG" (str script-dir "/../../bb.edn"))
             :auth (value "NC_CODEX_AUTH" (str (value "HOME" home) "/.codex/auth.json"))
             :codex (value "NC_CODEX" (some-> (fs/which "codex") str))
             :invoker-home home
             :kondo-path (some-> (or (fs/which "/usr/local/bin/clj-kondo") (fs/which "clj-kondo")) str)
             :tool-dirs ["/usr/bin" "/bin"]}]
    (if (re-matches #"[0-9]+" (:timeout cfg))
      cfg
      {:error "NC_TIMEOUT_S must contain digits only"})))

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
  (let [cfg (env-config {"HOME" "/credential-home"})]
    (is (= {:root "/var/tmp/forge/native-control"
            :model "gpt-6-astra"
            :reasoning "medium"
            :timeout "900"
            :keep "0"
            :sandbox "workspace-write"
            :auth "/credential-home/.codex/auth.json"
            :config (str script-dir "/../../bb.edn")
            :codex (some-> (fs/which "codex") str)
            :invoker-home (System/getProperty "user.home")
            :kondo-path (some-> (or (fs/which "/usr/local/bin/clj-kondo") (fs/which "clj-kondo")) str)
            :tool-dirs ["/usr/bin" "/bin"]}
           cfg))))

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
  (doseq [value ["-1" "+1" "1.5" " 9" "9 " "1s" "\n" "9\n" "１２"]]
    (is (= {:error "NC_TIMEOUT_S must contain digits only"}
           (env-config {"NC_TIMEOUT_S" value})))))

(deftest environment-digit-timeout
  (doseq [value ["0" "01" "900" "9223372036854775808"]]
    (is (= value (:timeout (env-config {"NC_TIMEOUT_S" value}))))))

(deftest environment-invoker-home
  (doseq [env [{} {"HOME" ""} {"HOME" "/unrelated-home"}]]
    (is (= (System/getProperty "user.home") (:invoker-home (env-config env))))))

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

(when (entry-script? (System/getProperty "babashka.file") *file*)
  (if (= ["--self-test"] (vec *command-line-args*))
    (let [r (run-tests 'native-control.run)] (exit-with (if (zero? (+ (:fail r) (:error r))) 0 1)))
    (do (binding [*out* *err*] (println "Usage: bb bench/native-control/run.clj --self-test")) (exit-with 2))))
