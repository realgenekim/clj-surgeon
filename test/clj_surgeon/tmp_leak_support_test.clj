(ns clj-surgeon.tmp-leak-support-test
  "RATCHET (2026-09-04, inb-9483a4) witnesses for clj-surgeon.tmp-leak-support:
   `with-temp-dir` deletes its directory even when its body throws, and the
   tmpfs predicate that `secure-tmpdir!` refuses on correctly tells a
   RAM-backed directory from a real-disk one.

   `secure-tmpdir!` itself is NOT unit-tested here: on the non-refused path
   it calls (System/exit ...) after waiting on a re-exec'd child process --
   correct behaviour for a suite bootstrap, but calling it from inside a
   nested test would tear down the very suite process running this test.
   Its REFUSAL branches, its JVM-flag and argv forwarding, its descendant
   TMPDIR inheritance and its shutdown sweep are driven as real subprocesses
   by `test/tmp_leak_ratchet_test.sh` (`make tmp-leak-ratchet-self-test`,
   inside `mcp-test`). Round one claimed instead that \"every green suite run
   IS the end-to-end proof\"; the accepted path is not the requirement, and
   an independent review found the refusal failed open."
  (:require
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; @spec MCP-OP-TMPHYG-002
(deftest with-temp-dir-cleans-up-on-throw
  (testing "a deliberately failing body still deletes its temp dir"
    (let [captured (atom nil)]
      (is (thrown? clojure.lang.ExceptionInfo
            (tmp-leak/with-temp-dir [dir "tmp-leak-support-fail-witness-"]
              (reset! captured dir)
              (is (.exists dir) "the dir exists before the deliberate failure")
              (throw (ex-info "deliberate failure" {})))))
      (is (some? @captured) "the temp dir was created before the throw")
      (is (not (.exists @captured))
          "with-temp-dir deleted it in `finally` despite the throw"))))

;; @spec MCP-OP-TMPHYG-002
(deftest with-temp-dir-cleans-up-on-success
  (let [captured (atom nil)]
    (tmp-leak/with-temp-dir [dir "tmp-leak-support-ok-witness-"]
      (reset! captured dir)
      (is (.exists dir)))
    (is (not (.exists @captured)))))

;; @spec MCP-OP-TMPHYG-001
;; @spec MCP-OP-TMPHYG-003
(deftest ram-paths-are-refused-by-name-with-no-external-binary
  (testing "the literal prefixes ~/bin/seat-tmp-guard.sh already refused on"
    (is (true? (tmp-leak/literal-ram-path? "/tmp")))
    (is (true? (tmp-leak/literal-ram-path? "/tmp/clj-surgeon-suite-1-abc")))
    (is (true? (tmp-leak/literal-ram-path? "/dev/shm")))
    (is (true? (tmp-leak/literal-ram-path? "/dev/shm/anything"))))
  (testing "a real-disk path is not refused by name"
    (is (false? (tmp-leak/literal-ram-path? "/var/tmp/forge")))
    (is (false? (tmp-leak/literal-ram-path? "/var/tmpfsish")))))

;; @spec MCP-OP-TMPHYG-003
(deftest the-base-decision-is-a-typed-refusal
  (testing "a RAM-backed base refuses, and the refusal names it"
    (let [refusal (tmp-leak/base-refusal "/dev/shm")]
      (is (some? refusal))
      (is (contains? #{:ram-path-prefix :tmpfs} (:reason refusal)))
      (is (str/starts-with? (tmp-leak/refusal-message refusal) "tmp-refused: "))))
  (testing "the suite's own base is PROVEN real disk -- it would not be here otherwise"
    (is (nil? (tmp-leak/base-refusal (tmp-leak/env-or-current-tmpdir)))))
  (testing "an undeterminable fstype is a named refusal, not a pass.
            mount-fstype is TRI-STATE: nil coerced to \"not tmpfs\" is what
            failed open. The :unknown branch is EXECUTED by
            test/tmp_leak_ratchet_test.sh 3d, which shims findmnt to fail and
            points CLJ_SURGEON_MOUNTS_FILE at nothing -- the only way to reach
            it, since findmnt --target resolves any path to its nearest
            existing ancestor and the mounts table always matches / at worst."
    (let [message (tmp-leak/refusal-message
                    {:reason :unknown-fstype :base "/some/base"})]
      (is (str/starts-with? message "tmp-refused: "))
      (is (str/includes? message "UNDETERMINABLE")))))

;; @spec MCP-OP-TMPHYG-004
(deftest only-this-namespaces-own-run-roots-are-sweepable
  (testing "the shared base itself can never be swept"
    (is (false? (tmp-leak/own-isolated-root? "/var/tmp/forge")))
    (is (false? (tmp-leak/own-isolated-root? "/var/tmp/forge/other-seat-fixture"))))
  (testing "a per-run root is"
    (is (true? (tmp-leak/own-isolated-root? "/var/tmp/forge/clj-surgeon-suite-42-deadbeef")))))

(deftest tmpfs-predicate-tells-ram-from-disk
  (testing "/dev/shm is tmpfs-backed -- this is what secure-tmpdir! refuses on"
    (is (true? (tmp-leak/tmpfs? "/dev/shm"))))
  (testing "the suite's own current java.io.tmpdir base is NOT tmpfs -- the
            suite would not have gotten this far otherwise"
    (is (false? (tmp-leak/tmpfs? (tmp-leak/env-or-current-tmpdir))))))
