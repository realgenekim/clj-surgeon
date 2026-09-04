(ns clj-surgeon.mcp-telemetry-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute PosixFilePermissions)))

(def request
  {"changes"
   [{"id" "body-class"
     "files" ["src/private/customer_view.clj"]
     "forms" ["customer-page"]
     "find" "[:body secret-token]"
     "replace" "[:body.page secret-token]"
     "expect" {"matches" 1 "each_form" 1}}]
   "expect" {"changes" 1 "edits" 1 "files" 1}})

(def response
  {:ok true
   :committed true
   :verification_complete true
   :changes 1
   :edits 1
   :files 1
   :undo_receipt "/private/receipt.edn"
   :read_back_hashes {"src/private/customer_view.clj" "abc"}})

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-mcp-telemetry-test-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- events
  [state]
  (mapv #(json/parse-string % true)
        (remove empty? (str/split-lines
                         (slurp (:file state))))))

(deftest off-mode-writes-nothing
  (let [directory (temp-dir)]
    (try
      (let [state (telemetry/start! {:mode :off :directory (.getPath directory)})]
        (telemetry/emit! state :server.start {:version "test"})
        (telemetry/record-call! state request response
                                {:validation_ms 1 :confinement_ms 2
                                 :kernel_ms 3 :total_ms 6})
        (is (nil? (:file state)))
        (is (empty? (seq (.listFiles directory)))))
      (finally
        (delete-tree! directory)))))

(deftest metrics-mode-captures-shape-without-content
  (let [directory (temp-dir)]
    (try
      (let [state (telemetry/start! {:mode :metrics
                                     :directory (.getPath directory)
                                     :session-id "metrics-session"
                                     :run-id "bench-r01"})]
        (telemetry/emit! state :server.start {:version "test" :startup_ms 8})
        (telemetry/record-call! state request response
                                {:validation_ms 1 :confinement_ms 2
                                 :kernel_ms 3 :total_ms 6})
        (let [[start call] (events state)
              raw (slurp (:file state))]
          (is (= "server.start" (:event start)))
          (is (= "tool.call" (:event call)))
          (is (= "metrics-session" (:session_id call)))
          (is (= "bench-r01" (:run_id call)))
          (is (= {:changes 1 :declared_edits 1 :declared_files 1
                  :file_references 1 :form_references 1
                  :find_characters 20 :replacement_characters 25}
                 (:request_shape call)))
          (is (= 3 (get-in call [:timings_ms :kernel_ms])))
          (is (= true (get-in call [:outcome :verification_complete])))
          (is (not (contains? call :request)))
          (is (not (contains? call :response)))
          (is (not (str/includes? raw "customer_view")))
          (is (not (str/includes? raw "secret-token")))
          (is (not (str/includes? raw "receipt.edn")))))
      (finally
        (delete-tree! directory)))))

(deftest full-mode-captures-the-exact-local-interaction
  (let [directory (temp-dir)]
    (try
      (let [state (telemetry/start! {:mode "full"
                                     :directory (.getPath directory)
                                     :session-id "full-session"})]
        (telemetry/record-call! state request response {:total_ms 9})
        (let [call (first (events state))]
          (is (= (json/parse-string (json/generate-string request) true)
                 (:request call)))
          (is (= (json/parse-string (json/generate-string response) true)
                 (:response call)))
          (is (= "full" (:telemetry_mode call)))))
      (finally
        (delete-tree! directory)))))

(deftest invalid-mode-refuses-instead-of-silently-changing-policy
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Telemetry mode"
        (telemetry/start! {:mode :everything :directory "/tmp"}))))

(deftest creates-private-posix-directory-and-log-when-supported
  (let [parent (temp-dir)
        directory (io/file parent "private")]
    (try
      (let [state (telemetry/start! {:mode :metrics
                                     :directory (.getPath directory)
                                     :session-id "permissions"})]
        (telemetry/emit! state :server.start {})
        (when (contains? (set (.supportedFileAttributeViews
                                (java.nio.file.FileSystems/getDefault)))
                         "posix")
          (is (= "rwx------"
                 (PosixFilePermissions/toString
                   (Files/getPosixFilePermissions
                     (.toPath directory) (make-array java.nio.file.LinkOption 0)))))
          (is (= "rw-------"
                 (PosixFilePermissions/toString
                   (Files/getPosixFilePermissions
                     (.toPath (io/file (:file state)))
                     (make-array java.nio.file.LinkOption 0)))))))
      (finally
        (delete-tree! parent)))))

(deftest retention-removes-only-expired-jsonl-files
  (let [directory (temp-dir)
        stale (io/file directory "stale.jsonl")
        fresh (io/file directory "fresh.jsonl")
        unrelated (io/file directory "keep.txt")
        now 2000000000000
        day-ms (* 24 60 60 1000)]
    (try
      (doseq [file [stale fresh unrelated]] (spit file "x"))
      (.setLastModified stale (- now (* 31 day-ms)))
      (.setLastModified fresh (- now (* 29 day-ms)))
      (.setLastModified unrelated (- now (* 60 day-ms)))
      (let [removed (telemetry/prune! (.getPath directory) 30 now)]
        (is (= [(.getCanonicalPath stale)] removed))
        (is (not (.exists stale)))
        (is (.exists fresh))
        (is (.exists unrelated)))
      (finally
        (delete-tree! directory)))))

;; ============================================================
;; Proving the connection, per arm
;; ============================================================
;; Field evidence (E6-Lb): three free-choice arms shared one server on 7909.
;; Their telemetry held `server.start` (one PROCESS started) and, had they
;; called, `tool.call` (SOME caller called) — neither of which distinguishes
;; one client session from another. A silent connection failure and a
;; deliberate decline are the same record, so the adoption null was credible
;; rather than proven.

;; @spec MCP-OP-STUDY-039
(deftest session-start-names-each-workspace-a-server-serves
  (let [directory (temp-dir)]
    (try
      (let [state (telemetry/start! {:mode :metrics :directory (.getPath directory)
                                     :run-id "arm-run-1"})]
        (telemetry/record-session-start! state {:workspace-root "/tmp/arms/e6/f1"})
        (telemetry/record-session-start! state {:workspace-root "/tmp/arms/e6/f2"})
        (testing "a second call on a root already seen emits nothing"
          (telemetry/record-session-start! state {:workspace-root "/tmp/arms/e6/f1"}))
        (let [started (filterv #(= "session.start" (:event %)) (events state))]
          (is (= 2 (count started))
              "one event per distinct workspace root, and only the first time")
          (is (= 2 (count (distinct (map :workspace_key started))))
              "each arm is distinguishable by its workspace key")
          (is (every? #(re-matches #"[0-9a-f]{16}" (:workspace_key %)) started)
              "the key is a stable digest prefix, not a path")
          (is (every? #(= "arm-run-1" (:run_id %)) started)
              "the server run id still travels with it")
          (testing "metrics mode never writes a path"
            (is (every? #(nil? (:workspace_root %)) started))
            (is (not (str/includes? (slurp (:file state)) "/tmp/arms"))))))
      (finally
        (delete-tree! directory)))))

;; @spec MCP-OP-STUDY-039
(deftest session-start-carries-the-root-in-full-mode-and-a-client-run-id
  (let [directory (temp-dir)]
    (try
      (let [state (telemetry/start! {:mode :full :directory (.getPath directory)})]
        (telemetry/record-session-start!
          state {:workspace-root "/tmp/arms/e6/f3" :client-run-id "e6-f3"})
        (let [event (first (filterv #(= "session.start" (:event %)) (events state)))]
          (is (= "/tmp/arms/e6/f3" (:workspace_root event))
              "full mode names the root")
          (is (= "e6-f3" (:client_run_id event))
              "a caller-supplied run id is carried when present")
          (is (= (telemetry/workspace-key "/tmp/arms/e6/f3") (:workspace_key event))
              "and the key a cohort can recompute is the same one")))
      (finally
        (delete-tree! directory)))))

;; @spec MCP-OP-STUDY-039
(deftest session-start-writes-nothing-in-off-mode
  (let [directory (temp-dir)]
    (try
      (let [state (telemetry/start! {:mode :off :directory (.getPath directory)})]
        (telemetry/record-session-start! state {:workspace-root "/tmp/arms/e6/f4"})
        (is (nil? (:file state)))
        (is (empty? (seq (.listFiles directory)))))
      (finally
        (delete-tree! directory)))))

;; ============================================================
;; One server, several client sessions, one root (O2 round 2)
;; ============================================================
;; Reviewer findings 2 and 3 (2026-09-03): the dedupe key was `workspace-key`
;; ALONE, held in a process-lifetime atom, so `session.start` distinguished
;; arms only when every arm got its own worktree path. Two arms in one root, or
;; one client reconnecting, still left identical bytes — the exact failure the
;; event was built to end. Measured: three `execute-inspect!` calls, one root,
;; one telemetry state → `tool.call events=3, session.start events=1`.
;; `client_run_id` would have fixed it and had NO production caller: both
;; entrances passed `{:workspace-root project-root}` and nothing else, and the
;; field was not part of the dedupe key, so supplying it would not have
;; re-armed the event either.

;; @spec MCP-OP-STUDY-043
(deftest session-start-distinguishes-two-client-runs-on-one-root
  (let [directory (temp-dir)]
    (try
      (let [state (telemetry/start! {:mode :metrics
                                     :directory (.getPath directory)
                                     :run-id "server-run"})
            root "/tmp/arms/e6/shared"]
        (telemetry/record-session-start!
          state {:workspace-root root :client-run-id "arm-a"})
        (telemetry/record-session-start!
          state {:workspace-root root :client-run-id "arm-b"})
        (testing "the same client run on the same root still emits once"
          (telemetry/record-session-start!
            state {:workspace-root root :client-run-id "arm-a"}))
        (let [started (filterv #(= "session.start" (:event %)) (events state))]
          (is (= 2 (count started))
              "two client runs on one root are two sessions, not one")
          (is (= #{"arm-a" "arm-b"} (set (map :client_run_id started))))
          (is (= 1 (count (distinct (map :workspace_key started))))
              "and both name the same root")))
      (finally
        (delete-tree! directory)))))

;; @spec MCP-OP-STUDY-043
(deftest session-start-without-a-client-run-id-still-dedupes-per-root
  (let [directory (temp-dir)]
    (try
      (let [state (telemetry/start! {:mode :metrics
                                     :directory (.getPath directory)})]
        (telemetry/record-session-start! state {:workspace-root "/tmp/arms/e6/g1"})
        (telemetry/record-session-start! state {:workspace-root "/tmp/arms/e6/g1"})
        (is (= 1 (count (filterv #(= "session.start" (:event %)) (events state))))
            "an anonymous connection is still one session per root"))
      (finally
        (delete-tree! directory)))))
