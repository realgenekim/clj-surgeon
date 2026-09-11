(ns run-all
  (:require
   [clj-surgeon.namespace-execution :as execution]
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.test :as t]))

;; The sole BB inventory; the shared coordinator reads this EDN vector.
(def namespaces
  '[clj-surgeon.jvm-error-test
    clj-surgeon.tmp-leak-support-test
    clj-surgeon.forms-test
    clj-surgeon.alias-migration-test
    clj-surgeon.agent-routing-test
    clj-surgeon.outline-test
    clj-surgeon.move-test
    clj-surgeon.operation-algebra-test
    clj-surgeon.move-dependency-test
    clj-surgeon.analyze-test
    clj-surgeon.diagnostic-delta-test
    clj-surgeon.rename-test
    clj-surgeon.fix-declares-test
    clj-surgeon.extract-header-test
    clj-surgeon.extract-test
    clj-surgeon.failure-report-test
    clj-surgeon.file-ops-test
    clj-surgeon.show-form-test
    clj-surgeon.structural-lens-test
    clj-surgeon.syntax-var-refs-test
    clj-surgeon.lens-query-test
    clj-surgeon.memory-battery-test
    clj-surgeon.cljc.merge-test
    clj-surgeon.cljc.split-test
    clj-surgeon.cljc.require-ops-test
    clj-surgeon.cljc.analyze-test
    clj-surgeon.edn-config-integration-test
    clj-surgeon.edit-test
    clj-surgeon.edit-dsl-test
    clj-surgeon.cljc-existing-ops-test
    clj-surgeon.ls-tree-test
    clj-surgeon.outermost-test
    clj-surgeon.owner-hypotheses-test
    clj-surgeon.parser-admission-test
    clj-surgeon.partition-all-test
    clj-surgeon.platform-selector-test
    clj-surgeon.quoted-var-refs-test
    clj-surgeon.xray-test
    clj-surgeon.help-test
    clj-surgeon.install-test
    clj-surgeon.insertion-gap-test
    clj-surgeon.intent-transaction-test
    clj-surgeon.workspace-onboarding-test
    clj-surgeon.worktree-lifecycle-test
    clj-surgeon.worktree-lifecycle-io-test
    clj-surgeon.worktree-lifecycle-cli-test
    clj-surgeon.recovery-test
    clj-surgeon.relation-census-test
    clj-surgeon.cli-dispatch-test
    clj-surgeon.core-discovery-test])

;; @spec TEST-ISO-015 -- whole namespace children preserve fixtures/hooks.
(let [args (vec *command-line-args*)
      emit? (= "--emit-edn" (first args))
      output (when emit? (second args))
      selected (if emit? (mapv symbol (drop 3 args)) namespaces)
      {:keys [refused root]} (tmp-leak/secure-tmpdir! {:bb-script *file* :bb-heap-mib 1024
                                                    :isolate-home? (every? #(contains? #{:fast :integration} (lm/lane-of %)) selected)} args)]
  (when refused (System/exit 97))
  (when-not (and (seq selected)
              (= (count selected) (count (set selected)))
              (every? #(= :bb (get lm/namespace-runtimes %)) selected)
              (or (not emit?) (= "--ns" (nth args 2 nil))))
    (binding [*out* *err*] (println "bb-lane-refused: invalid or missing namespace selection"))
    (System/exit 96))
  (when-not emit? (println "SERIAL/NOT-A-GATE: direct Babashka diagnostic"))
  (let [before (tmp-leak/tmp-entries)
        _ (doseq [n selected]
            (try (require n)
                 (catch Throwable e
                   (throw (ex-info (str "bb-portable-load-failed: " n)
                                   {:namespace n :error-type :bb-portable-load-failed} e)))))
        runs (mapv (fn [n]
                     (let [start (System/nanoTime)
                           facts (execution/run-observed n nil #(t/test-ns n))]
                       {:namespace n :counters (:counters facts)
                        :expected-vars (:expected-vars facts)
                        :executed-vars (:executed-vars facts)
                        :elapsed-ms (quot (- (System/nanoTime) start) 1000000)
                        :violations []})) selected)
        result (apply merge-with + (map :counters runs))
        leaks (tmp-leak/report-and-sweep-leak! root before)]
    (if output
      (spit output (pr-str {:namespaces selected :runs runs :result result
                            :notes {} :leak-fail leaks}))
      (t/do-report (assoc result :type :summary)))
    (System/exit (if (zero? (+ (:fail result) (:error result) leaks)) 0 1))))
