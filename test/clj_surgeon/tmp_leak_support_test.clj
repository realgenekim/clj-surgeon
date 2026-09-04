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
   [clojure.java.io :as io]
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

;; @spec MCP-OP-TMPHYG-004
(deftest sweep-root-refuses-a-directory-it-did-not-create
  (testing "a directory that is not one of this namespace's per-run roots
            survives sweep-root!, which returns false"
    (tmp-leak/with-temp-dir [parent "tmp-leak-sweep-guard-"]
      (let [foreign (io/file parent "other-seat-precious-fixture")]
        (.mkdirs foreign)
        (is (false? (tmp-leak/sweep-root! foreign)))
        (is (.exists foreign) "the sweep must not delete what it did not create"))
      (let [ours (io/file parent "clj-surgeon-suite-42-deadbeef")]
        (.mkdirs ours)
        (is (true? (tmp-leak/sweep-root! ours)))
        (is (not (.exists ours)))))))

;; @spec MCP-OP-TMPHYG-011
(deftest a-seam-sourced-fstype-can-never-prove-real-disk
  (let [findmnt (ns-resolve 'clj-surgeon.tmp-leak-support 'findmnt-fstype)
        table (ns-resolve 'clj-surgeon.tmp-leak-support 'mounts-table-fstype)
        seam (ns-resolve 'clj-surgeon.tmp-leak-support 'seam-mounts-file)
        disk "/var/tmp/forge"]
    (testing "with no seam set, a mounts-table ext4 answer IS proof of disk"
      (with-redefs-fn {findmnt (constantly nil)
                       table (constantly "ext4")
                       seam (constantly nil)}
        #(do (is (= "ext4" (tmp-leak/mount-fstype disk)))
             (is (nil? (tmp-leak/base-refusal disk))))))
    (testing "the SAME answer, sourced from the seam, is :unknown -- a refusal.
              An operator handing the check a forged table must not be able to
              convert `I cannot prove this is disk` into `proven disk`."
      (with-redefs-fn {findmnt (constantly nil)
                       table (constantly "ext4")
                       seam (constantly "/forged/mounts")}
        #(do (is (= :unknown (tmp-leak/mount-fstype disk)))
             (is (= :unknown-fstype (:reason (tmp-leak/base-refusal disk)))))))
    (testing "and the seam is still sound in the REFUSING direction"
      (with-redefs-fn {findmnt (constantly nil)
                       table (constantly "tmpfs")
                       seam (constantly "/forged/mounts")}
        #(is (= :tmpfs (:reason (tmp-leak/base-refusal disk))))))))

;; @spec MCP-OP-TMPHYG-010
(deftest no-gate-names-a-hard-coded-ram-path
  (testing "a refusal at the runner is worthless if the gates around it create
            directories in RAM by name. `${TMPDIR:-...}` forms and prose
            mentions of /tmp are not matches; only a literal /tmp/<name> used
            as a path is, and `/home/x/tmp/y` is not one."
    (let [pattern #"(?:^|[^A-Za-z0-9_.-])/tmp/[A-Za-z0-9_.]"
          files (cons (io/file "Makefile")
                      (filter #(str/ends-with? (.getName ^java.io.File %) ".sh")
                              (.listFiles (io/file "test"))))
          offenders (for [^java.io.File f files
                          :when (.exists f)
                          [n line] (map-indexed (fn [i l] [(inc i) l])
                                                (str/split-lines (slurp f)))
                          :when (and (re-find pattern line)
                                     (not (str/includes? line "TMPDIR:-/tmp")))]
                      (str (.getPath f) ":" n ": " (str/trim line)))]
      (is (empty? offenders)
          (str "hard-coded /tmp write targets: " (pr-str (vec offenders)))))))

(deftest tmpfs-predicate-tells-ram-from-disk
  (testing "/dev/shm is tmpfs-backed -- this is what secure-tmpdir! refuses on"
    (is (true? (tmp-leak/tmpfs? "/dev/shm"))))
  (testing "the suite's own current java.io.tmpdir base is NOT tmpfs -- the
            suite would not have gotten this far otherwise"
    (is (false? (tmp-leak/tmpfs? (tmp-leak/env-or-current-tmpdir))))))
