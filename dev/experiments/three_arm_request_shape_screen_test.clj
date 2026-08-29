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
  (let [run (fn [arm correct adherent]
              {:arm arm
               :correct correct
               :treatment-adherent adherent})
        green (mapcat (fn [arm] [(run arm true true) (run arm true true)])
                      screen/arms)
        red (assoc (vec green) 3 (run :file-groups true false))]
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

(let [{:keys [fail error]} (run-tests)]
  (when (pos? (+ fail error))
    (System/exit 1)))
