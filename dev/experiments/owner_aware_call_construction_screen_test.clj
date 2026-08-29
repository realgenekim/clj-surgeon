(ns owner-aware-call-construction-screen-test
  (:require
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.test :refer [deftest is testing]]
   [owner-aware-call-construction-screen :as screen]
   [owner-aware-symbol-migration :as migration]))

(def one-action
  {:mcp-call-count 1
   :refusal-count 0
   :recovery-count 0
   :shell-call-count 0
   :file-change-count 0
   :prompt-to-call-ms 10000.0})

(deftest candidate-surface-adds-only-one-call-construction-field
  (let [control (screen/tool-surface :control)
        candidate (screen/tool-surface :candidate)]
    (is (= mcp-tool/edit-tool-description (:description control)))
    (is (not= mcp-tool/tool-description (:description control)))
    (is (= (dissoc control :description :schema)
           (dissoc candidate :description :schema)))
    (is (= (:description control)
           (subs (:description candidate) 0 (count (:description control)))))
    (is (= mcp-schema/editor-tool-schema (:schema control)))
    (is (= (get-in mcp-schema/editor-tool-schema
                   [:properties "edits" :items :properties "within"])
           (get-in (:schema candidate)
                   [:properties "edits" :items :properties "within"])))
    (is (= #{screen/candidate-field-name}
           (->> (keys (get-in candidate [:schema :properties]))
                (remove (set (keys (get-in control [:schema :properties]))))
                set)))
    (is (= ["target_alias" "target_rule" "columns" "files"]
           (get-in candidate [:schema :properties
                              screen/candidate-field-name :required])))
    (is (false? (get-in candidate [:schema :properties
                                   screen/candidate-field-name
                                   :additionalProperties])))))

(deftest both-call-shapes-compile-to-the-frozen-future
  (let [{:keys [sources expected-after-hashes]} (migration/load-fixture)
        control (screen/score-call
                  sources expected-after-hashes :control
                  (dissoc migration/oracle-request "workspace_root")
                  one-action)
        candidate (screen/score-call
                    sources expected-after-hashes :candidate
                    (dissoc migration/candidate-manifest "workspace_root")
                    one-action)]
    (doseq [[label score] [[:control control] [:candidate candidate]]]
      (testing (name label)
        (is (:correct score))
        (is (:first-call-valid score))
        (is (:one-action score))
        (is (= 51 (get-in score [:compiler :match-count])))
        (is (= 9 (get-in score [:compiler :changed-file-count])))
        (is (get-in score [:compiler :future-hashes-equal]))))
    (is (> (get-in control [:payload :bytes])
           migration/candidate-payload-budget))
    (is (<= (get-in candidate [:payload :bytes])
            migration/candidate-payload-budget))))

(deftest wrong-owner-remains-an-invalid-first-call
  (let [bad (assoc-in migration/candidate-manifest
                      ["symbol_migration" "files" 0 1 0 0]
                      "missing-owner")
        score (screen/score-fixture-call
                :candidate (dissoc bad "workspace_root") one-action)]
    (is (false? (:correct score)))
    (is (false? (:first-call-valid score)))
    (is (= :change-owner-mismatch
           (get-in score [:compiler :error-type])))))

(deftest cohort-gate-requires-four-correct-runs-per-arm-and-fifteen-percent
  (let [run (fn [arm ms]
              {:arm arm :correct true
               :geometry {:prompt-to-call-ms ms}})
        passing (concat (repeat 4 (run :control 10000.0))
                        (repeat 4 (run :candidate 8000.0)))
        slow (concat (repeat 4 (run :control 10000.0))
                     (repeat 4 (run :candidate 9000.0)))
        incomplete (butlast passing)]
    (is (get-in (screen/cohort-report passing) [:gate :pass]))
    (is (= 0.2
           (get-in (screen/cohort-report passing)
                   [:candidate-improvement-ratio])))
    (is (false? (get-in (screen/cohort-report slow) [:gate :pass])))
    (is (false? (get-in (screen/cohort-report incomplete) [:gate :pass])))))

(apply clojure.test/run-tests ['owner-aware-call-construction-screen-test])
