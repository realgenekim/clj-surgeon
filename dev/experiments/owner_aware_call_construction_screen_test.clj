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

(def sha-a (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))
(def sha-c (apply str (repeat 64 "c")))
(def sha-d (apply str (repeat 64 "d")))

(defn protocol-run [index complete-wall-ms]
  (let [{:keys [run-id] :as expected}
        (nth screen/expected-run-manifest index)]
    (assoc expected
           :correct true
           :geometry {:prompt-to-call-ms (/ complete-wall-ms 2.0)
                      :complete-wall-ms complete-wall-ms}
           :verification {:complete true
                          :canonical-transaction-sha256 sha-a
                          :future-hashes-sha256 sha-b
                          :verifier-profile-sha256 sha-c}
           :isolation {:workspace-id (str "workspace-" run-id)
                       :codex-home-id (str "home-" run-id)
                       :session-id (str "session-" run-id)
                       :receipt-dir-id (str "receipts-" run-id)
                       :server-id (str "server-" run-id)
                       :starting-tree-sha256 sha-d
                       :lifecycle-policy-sha256 sha-a})))

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
                  migration/oracle-request
                  one-action)
        candidate (screen/score-call
                    sources expected-after-hashes :candidate
                    migration/candidate-manifest
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

(deftest public-schema-and-workspace-routing-are-part-of-the-scorer
  (doseq [field ["verify" "expect"]]
    (let [score (screen/score-fixture-call
                  :control (assoc migration/oracle-request field "unexpected")
                  one-action)]
      (is (false? (:correct score)) field)
      (is (= :public-schema-denied
             (get-in score [:compiler :error-type])) field)))
  (let [exact-root (screen/score-fixture-call
                     :control migration/oracle-request one-action)
        wrong-root (screen/score-fixture-call
                     :control
                     (assoc migration/oracle-request
                            "workspace_root"
                            (.getCanonicalPath (java.io.File. "/tmp")))
                     one-action)]
    (is (:correct exact-root))
    (is (false? (:correct wrong-root)))
    (is (= :benchmark-workspace-root-mismatch
           (get-in wrong-root [:compiler :error-type])))))

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

(deftest cohort-gate-enforces-manifest-isolation-and-complete-verified-wall
  (let [times [10000.0 7000.0 7000.0 10000.0
               7000.0 10000.0 10000.0 7000.0]
        passing (mapv protocol-run (range 8) times)
        block-1 (subvec passing 0 4)
        wrong-order (vec (concat (subvec passing 1 4)
                                 [(first passing)]
                                 (subvec passing 4)))
        shared-workspace (mapv #(assoc-in % [:isolation :workspace-id] "same")
                               passing)
        catastrophic-wall (mapv #(if (= :candidate (:arm %))
                                   (assoc-in % [:geometry :complete-wall-ms] 1000000.0)
                                   %)
                                passing)
        missing-samples (mapv #(if (and (= :candidate (:arm %))
                                        (not= "b1-r1" (:run-id %)))
                                 (update % :geometry dissoc :complete-wall-ms)
                                 %)
                              passing)]
    (is (get-in (screen/cohort-report block-1) [:gate :block-2-authorized]))
    (is (false? (get-in (screen/cohort-report block-1) [:gate :pass])))
    (is (get-in (screen/cohort-report passing) [:gate :pass]))
    (is (= 0.3
           (get-in (screen/cohort-report passing)
                   [:pooled :candidate-improvement-ratio])))
    (doseq [[label runs] [[:wrong-order wrong-order]
                          [:shared-workspace shared-workspace]
                          [:catastrophic-wall catastrophic-wall]
                          [:missing-samples missing-samples]
                          [:incomplete (pop passing)]]]
      (is (false? (get-in (screen/cohort-report runs) [:gate :pass]))
          (name label)))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (clojure.test/run-tests 'owner-aware-call-construction-screen-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
