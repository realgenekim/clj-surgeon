(ns clj-surgeon.mcp-test-runner
  (:require
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clj-surgeon.admit-patch-test]
   [clj-surgeon.core-discovery-test]
   [clj-surgeon.mcp-alias-migration-test]
   [clj-surgeon.mcp-change-buffer-test]
   [clj-surgeon.mcp-cold-verify-test]
   [clj-surgeon.mcp-combinable-transaction-test]
   [clj-surgeon.mcp-compact-edit-fields-test]
   [clj-surgeon.mcp-compact-edit-test]
   [clj-surgeon.mcp-compact-location-test]
   [clj-surgeon.mcp-compact-relations-test]
   [clj-surgeon.mcp-contract-test]
   [clj-surgeon.mcp-create-files-test]
   [clj-surgeon.mcp-extraction-plan-test]
   [clj-surgeon.mcp-extraction-test]
   [clj-surgeon.mcp-feature-thread-test]
   [clj-surgeon.mcp-hot-verify-test]
   [clj-surgeon.mcp-http-server-test]
   [clj-surgeon.mcp-inspect-contract-test]
   [clj-surgeon.mcp-inspect-tool-test]
   [clj-surgeon.mcp-intent-contract-test]
   [clj-surgeon.mcp-operation-async-test]
   [clj-surgeon.mcp-operation-registry-test]
   [clj-surgeon.mcp-operation-test]
   [clj-surgeon.mcp-paths-test]
   [clj-surgeon.mcp-prepared-confirmation-test]
   [clj-surgeon.mcp-prepared-request-test]
   [clj-surgeon.mcp-prepared-wire-test]
   [clj-surgeon.mcp-process-test]
   [clj-surgeon.mcp-program-tool-test]
   [clj-surgeon.mcp-read-request-normalization-test]
   [clj-surgeon.mcp-recovery-test]
   [clj-surgeon.mcp-schema-test]
   [clj-surgeon.mcp-semantic-client-test]
   [clj-surgeon.mcp-server-test]
   [clj-surgeon.mcp-telemetry-test]
   [clj-surgeon.mcp-tool-test]
   [clj-surgeon.mcp-workspace-test]
   [clj-surgeon.mcp-write-refusal-test]
   [clj-surgeon.outline-differential-test]
   [clj-surgeon.outline-memory-test]
   [clj-surgeon.quoted-var-refs-test]
   [clj-surgeon.repository-hygiene-test]
   [clj-surgeon.scope-stream-test]
   [clj-surgeon.txn-journal-test]
   [clj-surgeon.workspace-onboarding-test]
   [clojure.test :refer [run-tests]]))

(defn -main
  [& args]
  ;; RATCHET (2026-09-04, inb-9483a4): same enforcement as test/run_all.clj
  ;; -- refuse on tmpfs, then isolate java.io.tmpdir into a private per-run
  ;; root (via a re-exec'd child with -Djava.io.tmpdir=<root> -- a runtime
  ;; System/setProperty is NOT honored by real temp-file creation either;
  ;; see clj-surgeon.tmp-leak-support's docstring) so leaks fail the run by
  ;; name with no cross-seat false positives.
  (let [{:keys [refused root]}
        (tmp-leak/secure-tmpdir! {:main-ns "clj-surgeon.mcp-test-runner"} args)
        _ (when refused (System/exit 97))
        tmp-root root
        tmp-before (tmp-leak/tmp-entries)
        result
        (run-tests
          'clj-surgeon.admit-patch-test
          'clj-surgeon.core-discovery-test
          'clj-surgeon.mcp-alias-migration-test
          'clj-surgeon.mcp-contract-test
          'clj-surgeon.mcp-create-files-test
          'clj-surgeon.mcp-extraction-test
          'clj-surgeon.mcp-extraction-plan-test
          'clj-surgeon.mcp-feature-thread-test
          'clj-surgeon.mcp-change-buffer-test
          'clj-surgeon.mcp-cold-verify-test
          'clj-surgeon.mcp-compact-edit-test
          'clj-surgeon.mcp-compact-edit-fields-test
          'clj-surgeon.mcp-compact-location-test
          'clj-surgeon.mcp-combinable-transaction-test
          'clj-surgeon.mcp-compact-relations-test
          'clj-surgeon.mcp-http-server-test
          'clj-surgeon.mcp-hot-verify-test
          'clj-surgeon.mcp-inspect-contract-test
          'clj-surgeon.mcp-inspect-tool-test
          'clj-surgeon.mcp-intent-contract-test
          'clj-surgeon.mcp-operation-async-test
          'clj-surgeon.mcp-operation-registry-test
          'clj-surgeon.mcp-operation-test
          'clj-surgeon.mcp-recovery-test
          'clj-surgeon.mcp-paths-test
          'clj-surgeon.mcp-process-test
          'clj-surgeon.mcp-prepared-request-test
          'clj-surgeon.mcp-prepared-confirmation-test
          'clj-surgeon.mcp-prepared-wire-test
          'clj-surgeon.mcp-program-tool-test
          'clj-surgeon.mcp-read-request-normalization-test
          'clj-surgeon.mcp-schema-test
          'clj-surgeon.mcp-telemetry-test
          'clj-surgeon.mcp-tool-test
          'clj-surgeon.mcp-workspace-test
          'clj-surgeon.mcp-write-refusal-test
          'clj-surgeon.mcp-server-test
          'clj-surgeon.mcp-semantic-client-test
          'clj-surgeon.outline-differential-test
          'clj-surgeon.outline-memory-test
          'clj-surgeon.quoted-var-refs-test
          'clj-surgeon.repository-hygiene-test
          'clj-surgeon.scope-stream-test
          'clj-surgeon.txn-journal-test
          'clj-surgeon.workspace-onboarding-test)
        leak-fail (tmp-leak/report-and-sweep-leak! tmp-root tmp-before)]
    (System/exit (+ (:fail result) (:error result) leak-fail))))
