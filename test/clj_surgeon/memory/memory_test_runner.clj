(ns clj-surgeon.memory.memory-test-runner
  "Runner for the memory namespaces, which are deliberately outside the fast
   suites: they spawn child JVMs, write hundreds of megabytes of fixture, and
   cost minutes of wall. `make memory-red` invokes this."
  (:require
   [clj-surgeon.memory.journal-green-test]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clj-surgeon.memory.oom-reproduction-test]
   [clojure.test :refer [run-tests]]))

;; @spec MCP-OP-TMPHYG-009
(defn -main
  [& args]
  ;; RATCHET (2026-09-04, inb-9483a4): refuse a RAM-backed temp base, isolate
  ;; the run, and fail by name on anything left behind. This lane spawns
  ;; child JVMs and writes hundreds of megabytes of fixture, so it is exactly
  ;; the lane a leak hurts most. See clj-surgeon.tmp-leak-support.
  (let [{:keys [refused root]}
        (tmp-leak/secure-tmpdir! {:main-ns "clj-surgeon.memory.memory-test-runner"} args)
        _ (when refused (System/exit 97))
        tmp-before (tmp-leak/tmp-entries)
        result (run-tests 'clj-surgeon.memory.oom-reproduction-test
                          'clj-surgeon.memory.journal-green-test)
        leak-fail (tmp-leak/report-and-sweep-leak! root tmp-before)]
    (shutdown-agents)
    (System/exit (if (and (zero? (:fail result)) (zero? (:error result))
                          (zero? leak-fail))
                   0 1))))
