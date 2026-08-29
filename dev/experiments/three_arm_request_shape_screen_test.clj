(ns three-arm-request-shape-screen-test
  (:require
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clojure.test :refer [deftest is run-tests]]
   [owner-aware-symbol-migration :as migration]
   [three-arm-request-shape-screen :as screen]))

(deftest every-treatment-lowers-to-the-same-frozen-future
  (let [report (screen/prerequisite-report)]
    (is (:all-prerequisites-green report))
    (is (= [:flat :file-groups :closed-relations
            :closed-relations :file-groups :flat]
           (:order report)))
    (doseq [arm screen/arms]
      (is (get-in report [:arms arm :correct]))
      (is (get-in report [:arms arm :semantic-exact]))
      (is (get-in report [:arms arm :treatment-adherent]))
      (is (get-in report [:arms arm :decision-coverage-complete]))
      (is (get-in report [:arms arm :canonical-transaction-equal])))))

(deftest payload-bounds-match-the-frozen-screen
  (let [report (screen/prerequisite-report)]
    (is (= 6353 (get-in report [:arms :flat :argument-bytes])))
    (is (<= (get-in report [:arms :file-groups :argument-bytes])
            (* 0.82 6353)))
    (is (<= (get-in report [:arms :closed-relations :argument-bytes])
            (* 0.60 6353)))
    (is (every? pos?
                (for [arm screen/arms]
                  (get-in report [:arms arm :surface :surface-bytes]))))))

(deftest candidate-fields-are-standalone-schema-authority
  (let [a-schema (:schema (screen/tool-surface :file-groups))
        b-schema (:schema (screen/tool-surface :closed-relations))
        a-request (dissoc (screen/file-groups-request) "delete_owners")
        b-request (-> (screen/closed-relations-request)
                      (dissoc "edits" "delete_owners"))]
    (is (= {:ok true} (admission/authorize a-schema a-request)))
    (is (= {:ok true} (admission/authorize b-schema b-request)))
    (is (= {:ok true}
           (admission/authorize b-schema (screen/flat-request))))
    (doseq [partial [(dissoc b-request "require_change")
                     (dissoc b-request "symbol_migration")]]
      (is (= :public-schema-denied
             (:error-type (admission/authorize b-schema partial)))))
    (is (= :public-schema-denied
           (:error-type
            (admission/authorize
             b-schema
             (assoc-in b-request ["require_change" "surprise"] true)))))
    (is (= :public-schema-denied
           (:error-type
            (admission/authorize
             a-schema
             {"file_groups"
              [{"file" "src/a.clj"
                "edits" [{"file" "src/a.clj"
                          "from" "old"
                          "to" "new"}]}]}))))))

(deftest ambiguity-and-source-falsifiers-refuse-before-authority
  (let [falsifiers (screen/falsifier-report)]
    (is (= #{:a-mixed-flat :a-local-file :a-duplicate-group
             :b-duplicate-require-file :b-missing-source
             :b-platform-conditional :b-alias-collision
             :b-duplicate-symbol-row :b-row-permutation}
           (set (keys falsifiers))))
    (is (every? true? (vals falsifiers)))))

(deftest aggregate-requires-two-correct-adherent-runs-per-arm
  (let [run (fn [arm correct adherent prompt wall]
              {:arm arm
               :correct correct
               :treatment-adherent adherent
               :geometry {:prompt-to-call-ms prompt
                          :complete-wall-ms wall}})
        green [(run :flat true true 100.0 200.0)
               (run :file-groups true true 80.0 190.0)
               (run :closed-relations true true 75.0 180.0)
               (run :closed-relations true true 75.0 180.0)
               (run :file-groups true true 80.0 190.0)
               (run :flat true true 100.0 200.0)]
        red (assoc (vec green) 4
                   (run :file-groups true false 80.0 190.0))]
    (is (every? true? (vals (:gate (screen/cohort-report green)))))
    (is (false? (get-in (screen/cohort-report red)
                        [:gate :all-treatment-adherent])))))

(deftest final-agent-message-is-part-of-correctness
  (let [{:keys [sources expected-after-hashes]} (migration/load-fixture)
        base-geometry {:mcp-call-count 1
                       :refusal-count 0
                       :recovery-count 0
                       :shell-call-count 0
                       :file-change-count 0
                       :prompt-to-call-ms 0.0}
        score (fn [message]
                (screen/score-call
                 sources expected-after-hashes :flat (screen/flat-request)
                 (assoc base-geometry :final-agent-message message)))
        exact (score "call captured")
        varied (score "Captured.")]
    (is (:correct exact))
    (is (get-in exact [:final-agent-message :exact]))
    (is (= "call captured" (get-in exact [:final-agent-message :actual])))
    (is (false? (:correct varied)))
    (is (false? (get-in varied [:final-agent-message :exact])))))

(deftest schema-admission-is-part-of-scored-correctness
  (let [{:keys [sources expected-after-hashes]} (migration/load-fixture)
        request (assoc-in (screen/closed-relations-request)
                          ["require_change" "surprise"] true)
        geometry {:mcp-call-count 1
                  :refusal-count 0
                  :recovery-count 0
                  :shell-call-count 0
                  :file-change-count 0
                  :final-agent-message "call captured"
                  :prompt-to-call-ms 10.0
                  :complete-wall-ms 20.0}
        score (screen/score-call sources expected-after-hashes
                                 :closed-relations request geometry)]
    (is (false? (:correct score)))
    (is (= :public-schema-denied (get-in score [:admission :error-type])))
    (is (false? (get-in score [:admission :mutation-attempted])))
    (is (false? (get-in score [:admission :write-authority])))))

(defn- performance-run [arm prompt-ms wall-ms]
  {:arm arm
   :correct true
   :treatment-adherent true
   :geometry {:prompt-to-call-ms prompt-ms
              :complete-wall-ms wall-ms}})

(def passing-performance-runs
  [(performance-run :flat 100.0 200.0)
   (performance-run :file-groups 80.0 190.0)
   (performance-run :closed-relations 75.0 180.0)
   (performance-run :closed-relations 75.0 180.0)
   (performance-run :file-groups 80.0 190.0)
   (performance-run :flat 100.0 200.0)])

(deftest cohort-enforces-frozen-order-and-performance-gates
  (let [green (screen/cohort-report passing-performance-runs)
        wrong-order (screen/cohort-report
                     (assoc passing-performance-runs
                            0 (nth passing-performance-runs 1)
                            1 (nth passing-performance-runs 0)))
        slow-a-prompt (screen/cohort-report
                       (mapv #(if (= :file-groups (:arm %))
                                (assoc-in % [:geometry :prompt-to-call-ms] 90.0)
                                %)
                             passing-performance-runs))
        slow-b-prompt (screen/cohort-report
                       (mapv #(if (= :closed-relations (:arm %))
                                (assoc-in % [:geometry :prompt-to-call-ms] 85.0)
                                %)
                             passing-performance-runs))
        slow-a-wall (screen/cohort-report
                     (mapv #(if (= :file-groups (:arm %))
                              (assoc-in % [:geometry :complete-wall-ms] 210.0)
                              %)
                           passing-performance-runs))
        slow-b-wall (screen/cohort-report
                     (mapv #(if (= :closed-relations (:arm %))
                              (assoc-in % [:geometry :complete-wall-ms] 210.0)
                              %)
                           passing-performance-runs))]
    (is (every? true? (vals (:gate green))))
    (is (false? (get-in wrong-order [:gate :exact-order])))
    (is (false? (get-in slow-a-prompt
                        [:gate :a-prompt-at-least-15-percent-lower])))
    (is (false? (get-in slow-b-prompt
                        [:gate :b-prompt-at-least-20-percent-lower])))
    (is (false? (get-in slow-a-wall
                        [:gate :a-complete-wall-not-regressed])))
    (is (false? (get-in slow-b-wall
                        [:gate :b-complete-wall-not-regressed])))
    (is (= 20.0
           (get-in green [:comparisons :file-groups-vs-flat
                          :prompt-to-first-call :improvement-ms])))
    (is (= 25.0
           (get-in green [:comparisons :closed-relations-vs-flat
                          :prompt-to-first-call :percent-lower])))))

(let [{:keys [fail error]} (run-tests)]
  (when (pos? (+ fail error))
    (System/exit 1)))
