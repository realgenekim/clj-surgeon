(ns clj-surgeon.usage-log-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.core :as core]
   [clj-surgeon.usage-log :as u]
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
         (u/outcome-of {:exception true :error-type :broken}))))

(deftest environment
  (is (= "custom" (u/agent-kind {"CLJ_SURGEON_AGENT" "custom" "CODEX_HOME" "x" "CLAUDECODE" "1"})))
  (is (= "codex" (u/agent-kind {"CODEX_HOME" "x" "CLAUDECODE" "1"})))
  (is (= "codex" (u/agent-kind {"CODEX_SANDBOX_NETWORK_DISABLED" "1"})))
  (is (= "claude" (u/agent-kind {"CLAUDECODE" "1"})))
  (is (= "claude" (u/agent-kind {"CLAUDE_CODE_ENTRYPOINT" "cli"})))
  (is (= "off" (u/log-path {"CLJ_SURGEON_USAGE_LOG" "off" "HOME" "/home/a"})))
  (is (nil? (u/log-path {})))
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
      (is (not (.exists (java.io.File. "off"))))
      (finally (.delete file)))))

(deftest dispatch-boundary
  (let [lines (atom [])]
    (with-redefs [u/append! (fn [_ line] (swap! lines conj (json/parse-string line true)))]
      (with-out-str (core/run {:op :ls}) (core/run {:op "no-such-operation"}))
      (is (= ["ls" "unknown"] (mapv :op @lines)))
      (is (= ["missing-arguments" "unknown-operation"] (mapv :code @lines)))
      (is (every? #(= "refused" (:outcome %)) @lines))
      (is (every? #(and (integer? (:ms %)) (<= 0 (:ms %))) @lines))
      (is (thrown? Exception (u/record-call :ls #(throw (ex-info "private" {:error-type :broken})))))
      (is (= 3 (count @lines)))
      (is (= "error" (:outcome (last @lines))))
      (is (= "broken" (:code (last @lines))))
      (is (= :unchanged (u/record-call :ls (constantly :unchanged))))
      (is (= "ok" (:outcome (last @lines)))))))

(deftest logging-failure-preserves-result
  (with-redefs [u/append! (fn [& _] (throw (ex-info "disk failure" {})))]
    (is (= {:answer 42} (u/record-call :ls (constantly {:answer 42}))))
    (is (thrown? Exception (u/record-call :ls #(throw (ex-info "handler failure" {})))))))

(deftest repository-boundary
  (is (nil? (u/repo-name nil)))
  (is (= "tmp" (u/repo-name "/tmp"))))
