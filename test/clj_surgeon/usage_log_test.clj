(ns clj-surgeon.usage-log-test
  (:require
   [babashka.process :as proc]
   [cheshire.core :as json]
   [clj-surgeon.usage-log :as u]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(deftest literal-lines
  (is (= "{\"at\":\"2026-11-23T00:00:00Z\",\"op\":\"ls\",\"outcome\":\"ok\",\"code\":null,\"ms\":3,\"repo\":null,\"agent\":null}"
         (u/usage-line {:at "2026-11-23T00:00:00Z" :op :ls :outcome "ok" :ms 3})))
  (is (= "{\"at\":\"2026-11-23T00:00:00Z\",\"op\":\"ls\",\"outcome\":\"refused\",\"code\":\"missing-arguments\",\"ms\":0,\"repo\":\"repo\",\"agent\":\"codex\"}"
         (u/usage-line {:at "2026-11-23T00:00:00Z" :op "ls" :outcome "refused"
                        :code :missing-arguments :ms 0 :repo "repo" :agent "codex" :secret "omit"}))))

(deftest outcomes
  (is (= {:outcome "ok" :code nil} (u/outcome-of {})))
  (is (= {:outcome "ok" :code nil} (u/outcome-of "text")))
  (is (= {:outcome "refused" :code "missing-arguments"}
         (u/outcome-of {:error-type :missing-arguments})))
  (is (= {:outcome "error" :code "broken"}
         (u/outcome-of {:exception true :error-type :broken})))
  (doseq [error-type [nil "private caller text" 42]]
    (is (= {:outcome "refused" :code nil}
           (u/outcome-of {:error "private" :error-type error-type})))
    (is (= {:outcome "error" :code "exception"}
           (u/outcome-of {:exception true :error-type error-type}))))
  (is (= {:outcome "refused" :code nil}
         (u/outcome-of {:error-type "private caller text"}))))

(deftest environment
  (is (= "custom" (u/agent-kind {"CLJ_SURGEON_AGENT" "custom" "CODEX_HOME" "x" "CLAUDECODE" "1"})))
  (is (= "codex" (u/agent-kind {"CODEX_HOME" "x" "CLAUDECODE" "1"})))
  (is (= "codex" (u/agent-kind {"CODEX_SANDBOX_NETWORK_DISABLED" "1"})))
  (is (= "claude" (u/agent-kind {"CLAUDECODE" "1"})))
  (is (= "claude" (u/agent-kind {"CLAUDE_CODE_ENTRYPOINT" "cli"})))
  (is (= "off" (u/log-path {"CLJ_SURGEON_USAGE_LOG" "off" "HOME" "/home/a"})))
  (is (nil? (u/log-path {})))
  (is (= "/st/clj-surgeon/usage.jsonl" (u/log-path {"XDG_STATE_HOME" "/st" "HOME" "/home/a"})))
  (is (= {:outcome "ok" :code nil} (u/outcome-of {:error nil})))
  (is (nil? (u/agent-kind {"CLAUDECODE" "" "CODEX_HOME" ""})))
  (is (= {:outcome "refused" :code nil} (u/outcome-of {:error "Forms not found: nope"})))
  (is (nil? (u/agent-kind {})))
  (is (= "/home/a/.local/state/clj-surgeon/usage.jsonl" (u/log-path {"HOME" "/home/a"})))
  (is (= "/tmp/log" (u/log-path {"CLJ_SURGEON_USAGE_LOG" "/tmp/log"})))
  (is (= "off" (u/log-path {"CLJ_SURGEON_USAGE_LOG" "off"}))))

(deftest append-boundary
  (let [file (java.io.File/createTempFile "usage" ".jsonl")]
    (try
      (u/append! (str file) "one")
      (u/append! (str file) "two")
      (is (= "one\ntwo\n" (slurp file)))
      ;; A failed or disabled append leaves the filesystem exactly as it was: nothing thrown, nothing written.
      (u/append! (str file "/unwritable") "three")
      (u/append! "off" "disabled")
      (is (= "one\ntwo\n" (slurp file)))
      (is (not (.exists (java.io.File. (str file "/unwritable")))))
      (finally (.delete file)))))

(deftest in-process-run-does-not-log
  (let [file (java.io.File/createTempFile "usage-run" ".jsonl")]
    (.delete file)
    (try
      (let [{:keys [exit out err]}
            @(proc/process
               ["bb" "-e"
                "(require '[clj-surgeon.core :as core] '[clj-surgeon.usage-log :as u])
                (with-out-str (core/run {:op :ls}))
                (let [calls (atom 0)]
                  (with-redefs [u/append! (fn [& _] (swap! calls inc))]
                    (with-out-str (core/run {:op :ls})))
                  (prn @calls))"]
               {:out :string :err :string :extra-env {"CLJ_SURGEON_USAGE_LOG" (str file)}})]
        (is (zero? exit) err)
        (is (= 0 (edn/read-string out)))
        (is (not (.exists file))))
      (finally (.delete file)))))

(deftest cli-entry-logs-once
  (doseq [[args op outcomes exit-code]
          [[[":op" ":ls" ":file" "src/clj_surgeon/mcp_runtime.clj"] "ls" #{"ok"} 0]
           [[":op" ":no-such-op"] "unknown" #{"refused"} 1]
           [[":op" ":ls"] "ls" #{"refused"} 1]
           [[] nil nil 0] [["--help"] nil nil 0] [[":op" ":help"] nil nil 0]
           [["--version"] nil nil 0] [[":op" ":ls" "--help"] nil nil 0]
           [["up" "--help"] nil nil 0] [["recover" "--help"] nil nil 0]
           [["report-failure" "--help"] nil nil 0]]]
    (let [file (java.io.File/createTempFile "usage-entry" ".jsonl")]
      (.delete file)
      (try
        (let [{:keys [exit err]}
              @(proc/process (into ["bb" "-m" "clj-surgeon.core"] args)
                             {:out :string :err :string :extra-env {"CLJ_SURGEON_USAGE_LOG" (str file)}})]
          (is (= exit-code exit) (str args err))
          (if op
            (let [lines (when (.exists file) (str/split-lines (slurp file)))
                  entry (some-> (first lines) (json/parse-string true))]
              (is (= 1 (count lines)))
              (is (= op (:op entry)))
              (is (contains? outcomes (:outcome entry))))
            (is (not (.exists file)) (str args))))
        (finally (.delete file))))))

(deftest record-call-preserves-results
  (let [lines (atom [])]
    (with-redefs [u/append! (fn [_ line] (swap! lines conj (json/parse-string line true)))]
      (doseq [error-type [:broken nil "private caller text"]]
        (let [error (ex-info "private" {:error-type error-type})]
          (is (identical? error (try (u/record-call :ls #(throw error)) (catch Exception e e))))
          (is (= "error" (:outcome (last @lines))))
          (is (= (if (keyword? error-type) "broken" "exception") (:code (last @lines))))))
      (is (= :unchanged (u/record-call :ls (constantly :unchanged))))
      (is (= "ok" (:outcome (last @lines))))
      (is (= 4 (count @lines)))
      (is (every? #(and (integer? (:ms %)) (<= 0 (:ms %))) @lines)))))

(deftest logging-failure-preserves-result
  (with-redefs [u/append! (fn [& _] (throw (ex-info "disk failure" {})))]
    (is (= {:answer 42} (u/record-call :ls (constantly {:answer 42}))))
    (is (thrown? Exception (u/record-call :ls #(throw (ex-info "handler failure" {})))))))

(deftest repository-boundary
  (is (nil? (u/repo-name nil)))
  (is (= "tmp" (u/repo-name "/tmp")))
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "usage-repo-" (make-array java.nio.file.attribute.FileAttribute 0)))
        nested (java.io.File. root "src/deep")]
    (try
      (.mkdirs (java.io.File. root ".git"))
      (.mkdirs nested)
      (is (= (.getName root) (u/repo-name (str nested))) "the nearest .git ancestor names the repository")
      (finally (doseq [f (reverse (file-seq root))] (.delete f))))))

(require '[clj-surgeon.core :as core])

(deftest weekly-literals
  (let [agg (u/count-week
              ["{\"at\":\"2026-11-16T00:00:00Z\",\"op\":\"ls\",\"outcome\":\"ok\"}"
               "{\"at\":\"2026-11-22T23:59:59Z\",\"op\":\"ls\",\"outcome\":\"refused\"}"
               "{\"at\":\"2026-11-18T00:00:00Z\",\"op\":\"cat\",\"outcome\":\"error\"}"
               "{\"at\":\"2026-11-23T00:00:00Z\",\"op\":\"ls\",\"outcome\":\"ok\"}"
               "{\"at\":\"2026-11-15T23:59:59Z\",\"op\":\"ls\",\"outcome\":\"ok\"}"
               "bad" "null" "{}"]
              "2026-11-16T00:00:00Z" "2026-11-23T00:00:00Z")]
    (is (= {:ops {"ls" {:calls 2 :ok 1 :refused 1 :error 0}
                  "cat" {:calls 1 :ok 0 :refused 0 :error 1}}
            :total {:calls 3 :ok 1 :refused 1 :error 1} :malformed 3} agg))
    (is (= (str "op=ls calls=2 ok=1 refused=1 error=0\n"
             "op=cat calls=1 ok=0 refused=0 error=1\n"
             "total calls=3 ok=1 refused=1 error=1 malformed=3\n"
             "fall-backs cannot be seen from inside the tool; refusals and non-zero exits by op are the proxy")
          (u/render-week agg)))))

(deftest weekly-empty
  (let [agg (u/count-week [] "2026-11-16T00:00:00Z" "2026-11-23T00:00:00Z")]
    (is (= {:ops {} :total {:calls 0 :ok 0 :refused 0 :error 0} :malformed 0} agg))
    (is (= (str "total calls=0 ok=0 refused=0 error=0 malformed=0\n"
             "fall-backs cannot be seen from inside the tool; refusals and non-zero exits by op are the proxy")
          (u/render-week agg)))))

(deftest weekly-malformed
  (doseq [line ["" "[]" "42"
                "{\"at\":\"bad\",\"op\":\"ls\",\"outcome\":\"ok\"}"
                "{\"at\":\"2026-11-16T00:00:00Z\",\"op\":7,\"outcome\":\"ok\"}"
                "{\"at\":\"2026-11-16T00:00:00Z\",\"op\":\"ls\",\"outcome\":\"unknown\"}"]]
    (is (= {:ops {} :total {:calls 0 :ok 0 :refused 0 :error 0} :malformed 1}
          (u/count-week [line] "2026-11-16T00:00:00Z" "2026-11-23T00:00:00Z")))))

(deftest weekly-handler-arguments
  (let [handler (:handler (:usage-week core/ops-registry))]
    (doseq [opts [{:week "2026-11-17"}
                  {:week "2026-11-16" :since "2026-11-16"}
                  {:since "bad" :until "2026-11-23"}
                  {:since "2026-11-23" :until "2026-11-16"} {}]]
      (let [result (handler opts)]
        (is (= :invalid-arguments (:error-type result)))
        (is (string? (:error result)))))
    (with-redefs [u/log-path (constantly nil)]
      (is (= (handler {:week "2026-11-16"})
            (handler {:since "2026-11-16" :until "2026-11-23"}))))
    (is (= :read (:category (:usage-week core/ops-registry))))
    (is (str/includes? (core/format-global-help core/ops-registry) "usage-week"))
    (is (str/includes? (core/format-op-help :usage-week (:usage-week core/ops-registry))
          ":week 2026-11-16"))))

(deftest weekly-cli
  (let [file (java.io.File/createTempFile "usage-week" ".jsonl")]
    (try
      (spit file (str "{\"at\":\"2026-11-16T00:00:00Z\",\"op\":\"ls\",\"outcome\":\"ok\"}\n"
                   "{\"at\":\"2026-11-22T23:59:59Z\",\"op\":\"cat\",\"outcome\":\"refused\"}\n"))
      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core" ":op" ":usage-week"
                            ":log" (str file) ":since" "2026-11-16" ":until" "2026-11-23"]
               {:out :string :err :string :extra-env {"CLJ_SURGEON_USAGE_LOG" "off"}})]
        (is (= 0 exit) err)
        (is (= (str "op=cat calls=1 ok=0 refused=1 error=0\n"
                 "op=ls calls=1 ok=1 refused=0 error=0\n"
                 "total calls=2 ok=1 refused=1 error=0 malformed=0\n"
                 "fall-backs cannot be seen from inside the tool; refusals and non-zero exits by op are the proxy\n")
              out)))
      (.delete file)
      (is (str/starts-with? ((:handler (:usage-week core/ops-registry))
                             {:log (str file) :week "2026-11-16"})
            "total calls=0 ok=0 refused=0 error=0 malformed=0\n"))
      (finally (.delete file)))))
