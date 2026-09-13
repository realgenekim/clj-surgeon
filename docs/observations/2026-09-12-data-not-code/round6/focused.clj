(require '[clojure.test :as t]
         '[clj-surgeon.receipt-artifacts-boundary-test :as boundary]
         '[clj-surgeon.lane-manifest-test :as lanes])

(let [mode (first *command-line-args*)
      result
      (if (= "green" mode)
        (t/run-tests 'clj-surgeon.receipt-artifacts-boundary-test 'clj-surgeon.lane-manifest-test)
        (binding [t/*report-counters* (ref t/*initial-report-counters*)]
          (case mode
            "red" (t/test-vars [#'boundary/destination-envelope-admits-only-accounted-inode-links
                                #'lanes/runtime-steering-fields-cannot-outvote-control-receipts
                                #'lanes/runtime-receipts-must-stay-in-retained-evidence-roots])
            "outside-plant"
            (let [checker (ns-resolve 'clj-surgeon.receipt-artifacts 'contained-inode-links?)]
              (assert checker "the inode accounting plant must resolve by name")
              (with-redefs-fn {checker (constantly true)}
                #(t/test-vars [#'boundary/destination-envelope-refuses-hard-linked-final-ledger]))))
          (t/do-report (assoc @t/*report-counters* :type :summary))
          @t/*report-counters*))]
  (spit (str "docs/observations/2026-09-12-data-not-code/round6/focused-" mode ".edn")
        (pr-str result))
  (shutdown-agents)
  (System/exit (if (zero? (+ (:fail result) (:error result))) 0 1)))
