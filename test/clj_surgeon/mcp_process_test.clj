(ns ^{:lane :battery} clj-surgeon.mcp-process-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-process :as process]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]])
  (:import
   (java.lang ProcessHandle)
   (java.nio.file Files)))

;; RATCHET (2026-09-04, inb-9483a4): this namespace's several temp-dir
;; helpers (and a few inline deftest bodies) never deleted what they
;; created -- 22 kondo-admission / 10 fake-kondo / ... of the historical
;; leaked Anvil /tmp entries traced back here. Track every root created
;; below and sweep them after each test.
(def ^:private temp-roots (atom []))
(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots))
;; test-events-path (below) is one shared file for the whole namespace, not
;; per-test -- delete it once after every test in this ns has run.
(use-fixtures :once (fn [f]
                       (try (f)
                            (finally
                              (try (io/delete-file
                                     (io/file (System/getProperty "java.io.tmpdir")
                                              (str "clj-surgeon-kondo-test-events-"
                                                   (.pid (ProcessHandle/current)) ".jsonl"))
                                     true)
                                   (catch Throwable _ nil))))))

(defn- temporary-lock-path []
  (str (.resolve (tmp-leak/track!
                   temp-roots
                   (Files/createTempDirectory
                     "clj-surgeon-kondo-admission-"
                     (make-array java.nio.file.attribute.FileAttribute 0)))
                 "clj-kondo.lock")))

(def admission-script
  (.getCanonicalPath (io/file "resources/clj-kondo-admission.py")))

(def test-pressure-status "/definitely/missing/clj-surgeon-pressure-status.json")

(def test-events-path
  ;; RATCHET (2026-09-04): was a hard-coded "/tmp/..." string; the seat
  ;; points java.io.tmpdir at /var/tmp/forge (or the suite's isolated
  ;; sub-root) and this must land there too, never at literal /tmp.
  (str (io/file (System/getProperty "java.io.tmpdir")
                (str "clj-surgeon-kondo-test-events-"
                     (.pid (ProcessHandle/current)) ".jsonl"))))

(defn- fake-clj-kondo []
  (let [directory (tmp-leak/track!
                     temp-roots
                     (Files/createTempDirectory
                       "clj-surgeon-fake-kondo-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        executable (.resolve directory "clj-kondo")]
    (Files/createSymbolicLink executable
                              (.toPath (io/file "/bin/sleep"))
                              (make-array java.nio.file.attribute.FileAttribute 0))
    (str executable)))

(defn- fake-successful-clj-kondo []
  (let [directory (tmp-leak/track!
                     temp-roots
                     (Files/createTempDirectory
                       "clj-surgeon-fake-successful-kondo-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        executable (.resolve directory "clj-kondo")]
    (Files/createSymbolicLink executable
                              (.toPath (io/file "/usr/bin/true"))
                              (make-array java.nio.file.attribute.FileAttribute 0))
    (str executable)))

(defn- fake-script [body]
  (let [directory (tmp-leak/track!
                     temp-roots
                     (Files/createTempDirectory
                       "clj-surgeon-fake-process-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        executable (.toFile (.resolve directory "probe"))]
    (spit executable (str "#!/bin/sh\n" body "\n"))
    (.setExecutable executable true)
    (.getCanonicalPath executable)))

(defn- run-admitted [command cwd timeout-ms]
  (binding [process/*pressure-status-path* test-pressure-status
            process/*maximum-normalized-load* 1000000.0
            process/*clj-kondo-events-path* test-events-path]
    (process/with-command-admission
      command cwd timeout-ms
      (fn [_remaining-ms admission]
        (apply shell/sh (concat (:command admission) [:dir cwd]))))))

(defn- wait-until [timeout-ms predicate]
  (let [deadline (+ (System/nanoTime) (* timeout-ms 1000000))]
    (loop []
      (cond
        (predicate) true
        (< (System/nanoTime) deadline) (do (Thread/sleep 5) (recur))
        :else false))))

(defn- start-process [cwd command environment]
  (let [builder (doto (ProcessBuilder. ^java.util.List command)
                  (.directory (io/file cwd)))
        child-environment (.environment builder)]
    (doseq [[key value] environment]
      (.put child-environment key value))
    (.start builder)))

(deftest effective-path-makes-agent-tools-one-shot-without-dropping-caller-path
  (let [path (process/effective-path "/home/gene" "/custom/bin:/usr/bin")
        entries (str/split path (re-pattern java.io.File/pathSeparator))]
    (is (= "/home/gene/bin" (first entries)))
    (is (= "/home/gene/.local/bin" (second entries)))
    (is (some #{"/custom/bin"} entries))
    (is (some #{"/usr/bin"} entries))
    (is (= (count entries) (count (distinct entries))))))

(deftest configure-environment-publishes-the-complete-path
  (let [environment (java.util.HashMap. {"PATH" "/custom/bin"})]
    (is (identical? environment
                    (process/configure-environment! environment)))
    (is (str/includes? (.get environment "PATH")
                       (str (System/getProperty "user.home") "/bin")))
    (is (str/ends-with? (.get environment "PATH") "/custom/bin"))))

;; RATCHET (2026-09-04, inb-9483a4, round two): -Djava.io.tmpdir is a
;; JVM-internal property no child PROCESS inherits, so every subprocess this
;; server launches picked its own temp location from the ambient TMPDIR --
;; outside any isolated root, and invisible to the leak witness.
;; @spec MCP-OP-TMPHYG-005
(deftest configure-environment-publishes-this-process-temp-directory
  (let [environment (java.util.HashMap. {"PATH" "/custom/bin"})]
    (process/configure-environment! environment)
    (is (= (System/getProperty "java.io.tmpdir") (.get environment "TMPDIR"))
        "a descendant that picks its own temp location stays where this JVM writes")))

(deftest recognizes-only-clj-kondo-executables
  (is (process/clj-kondo-command? ["clj-kondo" "--lint" "src"]))
  (is (process/clj-kondo-command?
        ["/opt/homebrew/bin/clj-kondo" "--lint" "src"]))
  (is (not (process/clj-kondo-command? ["clojure" "-M:test"])))
  (is (not (process/clj-kondo-command? []))))

(deftest host-admission-serializes-concurrent-clj-kondo-sections
  ;; @spec MCP-OP-ANALYZER-001
  (let [lock-path (temporary-lock-path)
        analyzer (fake-clj-kondo)
        start (promise)
        run! (fn []
               (future
                 @start
                 (binding [process/*clj-kondo-lock-path* lock-path
                           process/*clj-kondo-admission-path* admission-script]
                   (run-admitted [analyzer "0.08"] "/tmp" 2000))))
        a (run!)
        b (run!)]
    (deliver start true)
    (let [results [@a @b]
          acquired (sort (map #(get-in % [:admission :acquired_monotonic_ns])
                              results))]
      (is (every? #(zero? (:exit %)) results))
      (is (every? #(= :admitted (get-in % [:admission :status])) results))
      (is (> (/ (- (second acquired) (first acquired)) 1000000.0) 50.0)
          (pr-str acquired)))))

(deftest admission-timeout-launches-no-second-analyzer
  ;; @spec MCP-OP-ANALYZER-002
  ;; @spec MCP-OP-ANALYZER-003
  (let [lock-path (temporary-lock-path)
        analyzer (fake-clj-kondo)
        owner-root (str (tmp-leak/track!
                          temp-roots
                          (Files/createTempDirectory
                           "owner-repository-"
                           (make-array java.nio.file.attribute.FileAttribute 0))))
        waiter-root (str (tmp-leak/track!
                           temp-roots
                           (Files/createTempDirectory
                            "waiter-repository-"
                            (make-array java.nio.file.attribute.FileAttribute 0))))
        owner (future
                (binding [process/*clj-kondo-lock-path* lock-path
                          process/*clj-kondo-admission-path* admission-script]
                  (run-admitted [analyzer "1.00"]
                                owner-root 2000)))]
    (is (wait-until 1000
                    #(and (.isFile (io/file lock-path))
                          (str/includes? (slurp lock-path)
                                         owner-root))))
    (binding [process/*clj-kondo-lock-path* lock-path
              process/*clj-kondo-admission-path* admission-script]
      (let [result (run-admitted [analyzer "0"]
                                 waiter-root 250)]
        (is (= 75 (:exit result)))
        (is (= :admission-timeout (get-in result [:admission :status])))
        (is (= :clj-kondo-admission-timeout
               (get-in result [:admission :error-type])))
        (is (= (.getCanonicalPath (io/file owner-root))
               (get-in result [:admission :owner :cwd])))))
    @owner))

(deftest stale-owner-text-does-not-own-the-operating-system-lock
  ;; @spec MCP-OP-ANALYZER-003
  (let [lock-path (temporary-lock-path)
        analyzer (fake-clj-kondo)
        current-root (str (tmp-leak/track!
                            temp-roots
                            (Files/createTempDirectory
                             "current-owner-"
                             (make-array java.nio.file.attribute.FileAttribute 0))))]
    (spit lock-path "{:pid 999999 :cwd \"/tmp/dead-owner\"}")
    (binding [process/*clj-kondo-lock-path* lock-path
              process/*clj-kondo-admission-path* admission-script]
      (let [result (run-admitted [analyzer "0"] current-root 100)]
        (is (zero? (:exit result)))
        (is (= :admitted (get-in result [:admission :status])))
        (is (= (.getCanonicalPath (io/file current-root))
               (get-in result [:admission :cwd])))))))

(deftest exec-owner-death-releases-admission-without-stale-file-cleanup
  ;; @spec MCP-OP-ANALYZER-003
  ;; @spec MCP-OP-ANALYZER-005
  (let [lock-path (temporary-lock-path)
        owner (start-process
                "/tmp"
                [admission-script
                 "--lock" lock-path
                 "--timeout-ms" "2000"
                 "--entrance" "owner-death-test"
                 "--events" test-events-path
                 "--pressure-status" test-pressure-status
                 "--max-normalized-load" "1000000"
                 "--"
                 "/bin/sleep" "30"]
                {})]
    (is (wait-until 1000
                    #(and (.isFile (io/file lock-path))
                          (str/includes? (slurp lock-path)
                                         "owner-death-test"))))
    (.destroyForcibly owner)
    (is (.waitFor owner 1000 java.util.concurrent.TimeUnit/MILLISECONDS))
    (binding [process/*clj-kondo-lock-path* lock-path
              process/*clj-kondo-admission-path* admission-script]
      (let [analyzer (fake-clj-kondo)
            successor (run-admitted [analyzer "0"] "/tmp" 500)]
        (is (zero? (:exit successor)))
        (is (= :admitted (get-in successor [:admission :status])))))))

(deftest direct-shell-shim-uses-the-same-host-admission
  ;; @spec MCP-OP-ANALYZER-001
  ;; @spec MCP-OP-ANALYZER-005
  (let [lock-path (temporary-lock-path)
        shim-directory (tmp-leak/track!
                         temp-roots
                         (Files/createTempDirectory
                          "clj-surgeon-kondo-shim-"
                          (make-array java.nio.file.attribute.FileAttribute 0)))
        shim (.resolve shim-directory "clj-kondo")
        _ (Files/copy (.toPath (io/file admission-script)) shim
                      (make-array java.nio.file.CopyOption 0))
        _ (.setExecutable (.toFile shim) true)
        environment {"CLJ_SURGEON_CLJ_KONDO_REAL" "/bin/sleep"
                     "CLJ_SURGEON_CLJ_KONDO_LOCK" lock-path
                     "CLJ_SURGEON_CLJ_KONDO_TIMEOUT_MS" "100"
                     "CLJ_SURGEON_PRESSURE_STATUS" test-pressure-status
                     "CLJ_SURGEON_CLJ_KONDO_MAX_NORMALIZED_LOAD" "1000000"
                     "CLJ_SURGEON_CLJ_KONDO_EVENTS" test-events-path
                     "PATH" (System/getenv "PATH")}
        owner (start-process "/tmp" [(str shim) "1"] environment)]
    (is (wait-until 1000
                    #(and (.isFile (io/file lock-path))
                          (str/includes? (slurp lock-path) "agent-shell"))))
    (let [waiter (start-process "/tmp" [(str shim) "0"] environment)]
      (is (.waitFor waiter 1000 java.util.concurrent.TimeUnit/MILLISECONDS))
      (is (= 75 (.exitValue waiter))))
    (is (.waitFor owner 2000 java.util.concurrent.TimeUnit/MILLISECONDS))))

(deftest explicit-admission-skips-a-path-shadowing-shell-shim
  ;; @spec MCP-OP-ANALYZER-001
  ;; @spec MCP-OP-ANALYZER-005
  (let [lock-path (temporary-lock-path)
        shim-directory (tmp-leak/track!
                         temp-roots
                         (Files/createTempDirectory
                          "clj-surgeon-shadowing-shim-"
                          (make-array java.nio.file.attribute.FileAttribute 0)))
        analyzer-directory (tmp-leak/track!
                             temp-roots
                             (Files/createTempDirectory
                              "clj-surgeon-real-analyzer-"
                              (make-array java.nio.file.attribute.FileAttribute 0)))
        shim (.resolve shim-directory "clj-kondo")
        analyzer (.resolve analyzer-directory "clj-kondo")]
    (Files/copy (.toPath (io/file admission-script)) shim
                (make-array java.nio.file.CopyOption 0))
    (Files/createSymbolicLink analyzer
                              (.toPath (io/file "/usr/bin/true"))
                              (make-array java.nio.file.attribute.FileAttribute 0))
    (.setExecutable (.toFile shim) true)
    (binding [process/*clj-kondo-lock-path* lock-path
              process/*clj-kondo-admission-path* admission-script
              process/*clj-kondo-shim-path* (str shim)
              process/*executable-path* (str shim-directory
                                             java.io.File/pathSeparator
                                             analyzer-directory)]
      (let [result (run-admitted ["clj-kondo"] "/tmp" 500)]
        (is (zero? (:exit result)))
        (is (= (.toRealPath analyzer
                            (make-array java.nio.file.LinkOption 0))
               (java.nio.file.Path/of
                 (get-in result [:admission :executable])
                 (make-array String 0))))))))

(deftest unrelated-processes-bypass-analyzer-admission
  (binding [process/*clj-kondo-lock-path* "/dev/null/impossible.lock"]
    (is (= :ran
           (process/with-command-admission
             ["clojure" "-M:test"]
             "/tmp/project"
             100
             (fn [remaining-ms admission]
               (is (= :not-required (:status admission)))
               (is (= ["clojure" "-M:test"] (:command admission)))
               (is (pos? remaining-ms))
               :ran))))))

(deftest bounded-process-preserves-stdin-stdout-stderr-and-exit
  ;; @spec MCP-OP-ANALYZER-002
  (let [probe (fake-script
                "read line; printf 'out:%s' \"$line\"; printf 'err:%s' \"$line\" >&2; exit 17")
        result (process/run-bounded!
                 {:command [probe]
                  :cwd "/tmp"
                  :timeout-ms 1000
                  :stdin-text "hello\n"})]
    (is (:finished? result))
    (is (= 17 (:exit result)))
    (is (= "out:hello" (:out result)))
    (is (= "err:hello" (:err result)))
    (is (= :not-required (get-in result [:admission :status])))
    (is (false? (:out-truncated result)))
    (is (false? (:err-truncated result)))))

(deftest admission-wait-and-analyzer-share-one-deadline
  ;; @spec MCP-OP-ANALYZER-002
  (let [lock-path (temporary-lock-path)
        analyzer (fake-clj-kondo)
        owner (future
                (binding [process/*clj-kondo-lock-path* lock-path
                          process/*clj-kondo-admission-path* admission-script]
                  (run-admitted [analyzer "0.25"] "/tmp" 1000)))]
    (is (wait-until 1000
                    #(and (.isFile (io/file lock-path))
                          (str/includes? (slurp lock-path) "clj-surgeon"))))
    (binding [process/*clj-kondo-lock-path* lock-path
              process/*clj-kondo-admission-path* admission-script
              process/*pressure-status-path* test-pressure-status
              process/*maximum-normalized-load* 1000000.0
              process/*clj-kondo-events-path* test-events-path]
      (let [result (process/run-bounded!
                     {:command [analyzer "0.30"]
                      :cwd "/tmp"
                      :timeout-ms 350})]
        (is (false? (:finished? result)))
        (is (:termination-confirmed result))
        (is (< (:elapsed_ms result) 650.0))))
    @owner))

(deftest admission-wrapper-accepts-cold-profile-deadlines
  ;; @spec MCP-OP-ANALYZER-002
  (let [lock-path (temporary-lock-path)
        result (shell/sh admission-script
                         "--lock" lock-path
                         "--timeout-ms" "1800000"
                         "--entrance" "cold-profile-test"
                         "--events" test-events-path
                         "--pressure-status" test-pressure-status
                         "--max-normalized-load" "1000000"
                         "--" "/usr/bin/true")]
    (is (zero? (:exit result)))))

(deftest admission-wrapper-records-exec-failure
  ;; @spec MCP-OP-ANALYZER-003
  (let [directory (tmp-leak/track!
                    temp-roots
                    (Files/createTempDirectory
                     "clj-surgeon-exec-failure-"
                     (make-array java.nio.file.attribute.FileAttribute 0)))
        lock-path (str (.resolve directory "lock"))
        evidence-path (str (.resolve directory "evidence.json"))
        missing (str (.resolve directory "missing-clj-kondo"))
        result (shell/sh admission-script
                         "--lock" lock-path
                         "--timeout-ms" "1000"
                         "--entrance" "exec-failure-test"
                         "--evidence" evidence-path
                         "--events" test-events-path
                         "--pressure-status" test-pressure-status
                         "--max-normalized-load" "1000000"
                         "--" missing)]
    (is (= 126 (:exit result)))
    (is (str/includes? (slurp evidence-path) "clj-kondo-exec-failed"))))

(deftest red-pressure-defers-without-launching-the-analyzer
  ;; @spec MCP-OP-ANALYZER-006
  (let [directory (tmp-leak/track!
                    temp-roots
                    (Files/createTempDirectory
                     "clj-surgeon-pressure-defer-"
                     (make-array java.nio.file.attribute.FileAttribute 0)))
        lock-path (str (.resolve directory "lock"))
        evidence-path (str (.resolve directory "evidence.json"))
        marker-path (str (.resolve directory "must-not-exist"))
        result (shell/sh admission-script
                         "--lock" lock-path
                         "--timeout-ms" "1000"
                         "--entrance" "pressure-defer-test"
                         "--evidence" evidence-path
                         "--events" test-events-path
                         "--pressure-status" test-pressure-status
                         "--max-normalized-load" "0.000001"
                         "--" "/usr/bin/touch" marker-path)]
    (is (= 75 (:exit result)))
    (is (str/includes? (slurp evidence-path) "clj-kondo-pressure-deferred"))
    (is (false? (.exists (io/file marker-path))))))

(deftest analyzer-contract-mission-admits-exactly-five-fake-children
  ;; @spec MCP-OP-ANALYZER-009
  (let [lock-path (temporary-lock-path)
        priority-path (str lock-path ".priority")
        analyzer (fake-successful-clj-kondo)
        scope (apply str (repeat 64 "a"))
        sixth-error (atom nil)
        results
        (binding [process/*clj-kondo-lock-path* lock-path
                  process/*clj-kondo-priority-lock-path* priority-path
                  process/*clj-kondo-admission-path* admission-script]
          (process/call-with-analyzer-contract-mission
            (System/getProperty "user.dir") scope
            (fn []
              (let [first-five (mapv (fn [_]
                                       (run-admitted [analyzer] "/tmp" 1000))
                                     (range 5))]
                (try
                  (run-admitted [analyzer] "/tmp" 1000)
                  (catch clojure.lang.ExceptionInfo error
                    (reset! sixth-error (ex-data error))))
                first-five))))]
    (is (every? #(zero? (:exit %)) results))
    (is (= [1 2 3 4 5]
           (mapv #(get-in % [:admission :mission_launch_index]) results)))
    (is (every? #(= "test-mission" (get-in % [:admission :lane])) results))
    (is (= :analyzer-mission-budget-exhausted
           (:error-type @sixth-error)))))

(defn- direct-mission-command
  [lock-path priority-path events-path entrance launch-index command]
  [admission-script
   "--lock" lock-path
   "--priority-lock" priority-path
   "--timeout-ms" "2000"
   "--entrance" entrance
   "--events" events-path
   "--pressure-status" test-pressure-status
   "--max-normalized-load" "1000000"
   "--lane" "test-mission"
   "--mission-id" process/analyzer-contract-mission-id
   "--mission-owner-pid" (str (.pid (ProcessHandle/current)))
   "--mission-cwd" "/tmp"
   "--mission-command-cwd" "/tmp"
   "--mission-scope-sha256" (apply str (repeat 64 "b"))
   "--mission-launch-index" (str launch-index)
   "--mission-launch-limit" "5"
   "--mission-expires-epoch-ms" (str (+ (System/currentTimeMillis) 60000))
   "--" command])

(deftest waiting-interactive-work-runs-between-test-mission-children
  ;; @spec MCP-OP-ANALYZER-009
  (let [lock-path (temporary-lock-path)
        priority-path (str lock-path ".priority")
        events-path (str lock-path ".events")
        first-mission (start-process
                        "/tmp"
                        (conj (direct-mission-command
                                lock-path priority-path events-path
                                "mission-one" 1 "/bin/sleep")
                              "0.15")
                        {})]
    (is (wait-until 1000
                    #(and (.isFile (io/file events-path))
                          (str/includes? (slurp events-path) "mission-one"))))
    (let [interactive (start-process
                        "/tmp"
                        [admission-script
                         "--lock" lock-path
                         "--priority-lock" priority-path
                         "--timeout-ms" "2000"
                         "--entrance" "interactive"
                         "--events" events-path
                         "--pressure-status" test-pressure-status
                         "--max-normalized-load" "1000000"
                         "--" "/bin/sleep" "0.03"]
                        {})
          _ (Thread/sleep 25)
          second-mission (start-process
                           "/tmp"
                           (direct-mission-command
                             lock-path priority-path events-path
                             "mission-two" 2 "/usr/bin/true")
                           {})]
      (doseq [process [first-mission interactive second-mission]]
        (is (.waitFor process 3000 java.util.concurrent.TimeUnit/MILLISECONDS))
        (is (zero? (.exitValue process))))
      (let [entrances (->> (str/split-lines (slurp events-path))
                           (map #(json/parse-string % true))
                           (filter #(= "admitted" (:status %)))
                           (mapv :entrance))]
        (is (= ["mission-one" "interactive" "mission-two"] entrances)
            (pr-str entrances))))))

(deftest test-mission-requires-a-post-exit-pressure-observation
  ;; @spec MCP-OP-ANALYZER-009
  (let [lock-path (temporary-lock-path)
        priority-path (str lock-path ".priority")
        events-path (str lock-path ".events")
        marker-path (str lock-path ".must-not-exist")
        base (direct-mission-command
               lock-path priority-path events-path
               "post-exit" 2 "/usr/bin/touch")
        separator (.indexOf base "--")
        command (into (subvec base 0 separator)
                      ["--mission-after-epoch-ms"
                       (str (+ (System/currentTimeMillis) 60000))
                       "--" "/usr/bin/touch" marker-path])
        result (apply shell/sh (concat command [:dir "/tmp"]))]
    (is (= 75 (:exit result)))
    (is (str/includes? (:err result)
                       "clj-kondo-mission-post-exit-sample-required"))
    (is (false? (.exists (io/file marker-path))))))
