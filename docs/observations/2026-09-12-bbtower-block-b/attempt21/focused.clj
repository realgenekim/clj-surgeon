(require '[clojure.test :as t]
         'clj-surgeon.lane-manifest-test
         'clj-surgeon.ns-isolation-test
         'clj-surgeon.mcp-http-server-test
         'clj-surgeon.mcp-hot-verify-test
         'clj-surgeon.splice-envelope-test)

(let [names '[clj-surgeon.lane-manifest-test/every-manifest-entry-exists-on-disk
              clj-surgeon.ns-isolation-test/bb-ceiling-reproduces-from-calibration-under-shipped-manifest
              clj-surgeon.ns-isolation-test/spec-bb-boundary-equals-registered-ceiling
              clj-surgeon.mcp-http-server-test/probe-output-degrades-without-deleting-the-verdict
              clj-surgeon.mcp-http-server-test/probe-servlet-bounds-the-actual-writer
              clj-surgeon.mcp-http-server-test/probe-spec-receipt-shape-matches-an-executed-probe
              clj-surgeon.mcp-hot-verify-test/probe-authorizes-the-requested-test-target-before-reload
              clj-surgeon.splice-envelope-test/bounded-input-path-encoding]]
  (binding [t/*report-counters* (ref t/*initial-report-counters*)]
    (t/test-vars (mapv requiring-resolve names))
    (prn {:witnesses names :result @t/*report-counters*})
    (System/exit (if (zero? (+ (:fail @t/*report-counters*)
                              (:error @t/*report-counters*))) 0 1))))
