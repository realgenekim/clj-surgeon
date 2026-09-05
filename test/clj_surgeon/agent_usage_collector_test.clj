(ns clj-surgeon.agent-usage-collector-test
  "The reviewer's hour: a telemetry root holding ONE alias_migration event must
   report one alias_migration call, attributed to its session and its root.

   Earned 2026-09-05. `make study-agent-usage` reported zero tool calls for an
   hour that contained an attested alias_migration call — the tool wrote no
   event, and the collector's shape could not have attributed it anyway: it
   counted only the legacy per-handler stream, reported no per-session or
   per-root breakdown, and reported an empty root as a measured zero. A
   reporting surface that can say ZERO for a busy hour is worse than none,
   because zero terminates investigation."
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def ^:private collector
  "skills/study-agent-usage/scripts/collect_agent_usage.py")

(defn- repo-root [] (System/getProperty "user.dir"))

(defn- temp-dir
  [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- alias-event
  "One tool.dispatch event exactly as the server's dispatch boundary writes it."
  [timestamp session-id]
  {:telemetry_schema 1
   :timestamp timestamp
   :event "tool.dispatch"
   :session_id session-id
   :telemetry_mode "metrics"
   :tool "alias_migration"
   :request_id "req-0001"
   :outcome "ok"
   :started_ns 1000000
   :finished_ns 4000000
   :wall_ms 3.0
   :bytes_in 128
   :bytes_out 512})

(defn- run-collector!
  "Run the collector bounded to fixture roots. It must never walk this box's
   real transcript trees from a unit test."
  [surgeon-root]
  (let [sandbox (temp-dir "clj-surgeon-collector-sandbox-")
        receipt (io/file sandbox "receipt.json")]
    (doseq [child ["codex" "claude" "observations"]]
      (.mkdirs (io/file sandbox child)))
    (try
      (let [result (shell/sh "python3" collector
                             "--full"
                             "--since" "2026-09-05T00:00:00+00:00"
                             "--until" "2026-09-05T01:00:00+00:00"
                             "--observations-root" (.getPath (io/file sandbox "observations"))
                             "--codex-root" (.getPath (io/file sandbox "codex"))
                             "--claude-root" (.getPath (io/file sandbox "claude"))
                             "--cclsp-log" (.getPath (io/file sandbox "absent.log"))
                             "--surgeon-telemetry-root" (str surgeon-root)
                             "--receipt-out" (.getPath receipt)
                             :dir (repo-root))]
        (assoc result :receipt (when (.exists receipt)
                                 (json/parse-string (slurp receipt) true))))
      (finally
        (delete-tree! sandbox)))))

;; @spec MCP-OP-TELCOV-005
;; @spec MCP-OP-TELCOV-006
(deftest the-reviewers-hour-reports-one-alias-migration-call
  (let [root (temp-dir "clj-surgeon-collector-root-")
        session-id "reviewer-hour-session"]
    (try
      (spit (io/file root (str session-id ".jsonl"))
            (str (json/generate-string
                   (alias-event "2026-09-05T00:11:00Z" session-id))
                 "\n"))
      (let [{:keys [out err receipt]} (run-collector! (.getPath root))
            surgeon (get-in receipt [:services :clj_surgeon_mcp])]
        (is (some? receipt) (str "collector produced no receipt: " err))

        ;; The defect, stated as a number.
        (is (= 1 (:mcp_tool_calls surgeon))
            (str "the reviewer's hour reported " (:mcp_tool_calls surgeon)
                 " calls; stdout: " out))
        (is (= {:alias_migration 1} (:tools surgeon)))

        ;; Per session id, read from the event, not from a launcher name.
        (is (= 1 (get-in surgeon [:by_session (keyword session-id) :mcp_tool_calls])))
        (is (= {:alias_migration 1}
               (get-in surgeon [:by_session (keyword session-id) :tools])))

        ;; Per root, with the root the collector actually read printed.
        (is (= [(.getPath root)] (mapv :root (:roots surgeon))))
        (is (= "ok" (:status (first (:roots surgeon)))))
        (is (= 1 (:mcp_tool_calls (first (:roots surgeon)))))
        (is (= [session-id] (:sessions (first (:roots surgeon)))))

        ;; The window is stated in the receipt, not implied by the caller.
        (is (str/starts-with? (get-in surgeon [:window :since]) "2026-09-05T00:00"))
        (is (str/starts-with? (get-in surgeon [:window :until]) "2026-09-05T01:00")))
      (finally
        (delete-tree! root)))))

;; @spec MCP-OP-TELCOV-006
(deftest an-empty-root-is-named-not-counted-as-zero
  (let [root (temp-dir "clj-surgeon-collector-empty-root-")]
    (try
      (let [{:keys [err receipt]} (run-collector! (.getPath root))
            surgeon (get-in receipt [:services :clj_surgeon_mcp])
            entry (first (:roots surgeon))]
        (is (some? receipt) (str "collector produced no receipt: " err))
        (is (= "no-files" (:status entry)))
        (is (= (str "no files under " (.getPath root)) (:note entry))
            "a present but empty root must be named, never reported as zero")
        (is (= 0 (:mcp_tool_calls entry))))
      (finally
        (delete-tree! root)))))

;; @spec MCP-OP-TELCOV-005
(deftest the-collectors-own-self-test-stays-green
  (let [{:keys [exit out err]} (shell/sh "python3" collector "--self-test"
                                         :dir (repo-root))]
    (is (zero? exit) (str out err))))
