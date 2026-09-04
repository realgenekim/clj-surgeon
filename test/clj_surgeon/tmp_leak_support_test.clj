(ns clj-surgeon.tmp-leak-support-test
  "RATCHET (2026-09-04, inb-9483a4) witnesses for clj-surgeon.tmp-leak-support:
   `with-temp-dir` deletes its directory even when its body throws, and the
   tmpfs predicate that `secure-tmpdir!` refuses on correctly tells a
   RAM-backed directory from a real-disk one.

   `secure-tmpdir!` itself is NOT unit-tested here: on the non-refused path
   it calls (System/exit ...) after waiting on a re-exec'd child process --
   correct behaviour for a suite bootstrap, but calling it from inside a
   nested test would tear down the very suite process running this test.
   It is instead verified functionally: every green run of
   `~/bin/suite-run bb test/run_all.clj` and
   `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` IS that end-to-end
   proof (both runners call it as their first act)."
  (:require
   [clj-surgeon.tmp-leak-support :as tmp-leak]
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
(deftest tmpfs-predicate-tells-ram-from-disk
  (testing "/dev/shm is tmpfs-backed -- this is what secure-tmpdir! refuses on"
    (is (true? (tmp-leak/tmpfs? "/dev/shm"))))
  (testing "the suite's own current java.io.tmpdir base is NOT tmpfs -- the
            suite would not have gotten this far otherwise"
    (is (false? (tmp-leak/tmpfs? (tmp-leak/env-or-current-tmpdir))))))
