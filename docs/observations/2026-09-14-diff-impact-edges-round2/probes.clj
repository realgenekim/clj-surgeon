(require '[clj-surgeon.diff-impact-test :as t])
(doseq [{:keys [id files changes]} t/round-two-probes]
  (let [start (System/nanoTime)
        r (t/run-fixture files changes "list")
        non-list (when (empty? (t/selected-set r)) (t/run-fixture files changes "fixed-point"))]
    (prn {:probe id :list-exit (:exit r)
          :selection (select-keys (:inventory r) [:status :reason :changed-files :unmatched-files :namespaces])
          :fixed-point (when non-list {:exit (:exit non-list) :results (:results non-list)})
          :wall-ms (quot (- (System/nanoTime) start) 1000000)})))
