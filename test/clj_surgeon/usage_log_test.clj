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
