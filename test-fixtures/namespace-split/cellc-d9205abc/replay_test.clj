(ns replay-test
  (:require    [babashka.fs :as fs]
   [clojure.test :refer [deftest is run-tests]]))
(load-file (str (fs/path (fs/parent *file*) "replay.clj")))
(require '[replay :as r])
(deftest root-only-rebinding
  (let [request {:workspace_root "/old" :source {:file "views.clj"}
                 :destinations [{:lib "app.views" :forms ["view"]}]
                 :verification {:profile "split-unit"} :plan_only false}
        rebound (r/rebind-request request "/new")]
    (is (= "/new" (:workspace_root rebound)))
    (is (= (dissoc request :workspace_root) (dissoc rebound :workspace_root)))))
;; Literal receipt projection: sentinel-20261025-split-receipt.edn field shapes.
(def recorded {:state "committed" :verification_complete true
   :counts {:caller_sites 87 :caller_files 5 :destinations 20}
   :facts {:footprint {:created [2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21]}
           :unexpected_paths [".clj-surgeon.edn" "00SERVER-LOGS.txt" "test/view_architecture_test.clj"]}
   :unrequired_qualified_refs [{:file "views/dashboard.clj" :line 44 :token "store/now-inst"}]})
(deftest literal-facts
  (is (= {:state "committed" :verification_complete true :caller_sites 87 :caller_files 5
          :destinations 20 :created 20
          :unexpected_paths [".clj-surgeon.edn" "00SERVER-LOGS.txt" "test/view_architecture_test.clj"]
          :unrequired_qualified_refs [{:file "views/dashboard.clj" :line 44 :token "store/now-inst"}]}
         (r/receipt-facts recorded)))
  (is (= 0 (:created (r/receipt-facts {:facts {:footprint {:created []}}})))))
(deftest comparison-matrix
  (let [facts (r/receipt-facts recorded)]
    (is (= ["caller_files same" "caller_sites same" "created same" "destinations same" "state same"
            "unexpected_paths expected-different recorded artefacts: 3, replay: 3"
            "unrequired_qualified_refs same" "verification_complete same"]
           (r/compare-facts recorded facts)))
    (doseq [[k v] (dissoc facts :unexpected_paths)]
      (is (some #{(str (name k) " DIFFERS " (pr-str v) " :changed")}
                (r/compare-facts recorded (assoc facts k :changed)))))))
(deftest cold-run2-created-and-artefacts
  ;; 2026-11-28 outside-sandbox receipt: facts.destinations replaces footprint.created.
  (let [now (-> recorded (assoc :facts {:destinations (vec (repeat 20 {:file "views/auth.clj"}))})
                (assoc :workspace_status {:unexpected_paths nil}))
        lines (r/compare-facts recorded (r/receipt-facts now))]
    (is (some #{"created same"} lines))
    (is (some #{"unexpected_paths expected-different recorded artefacts: 3, replay: 0"} lines))
    (is (= 0 (r/comparison-exit lines)))
    (doseq [created [nil [] [{:file "one.clj"}]]]
      (let [changed (assoc-in now [:facts :destinations] created)]
        (is (= 1 (r/comparison-exit (r/compare-facts recorded (r/receipt-facts changed)))))))))
(deftest missing-is-not-success
  (is (every? #(.contains % " DIFFERS ") (r/compare-facts {} (r/receipt-facts {}))))
  (is (= :replay/missing (:created (r/receipt-facts {}))))
  (is (= 1 (r/comparison-exit (r/compare-facts recorded (r/receipt-facts {}))))))
(deftest planned-and-refused-stay-different
  (doseq [state ["planned" "refused"]]
    (let [now (assoc recorded :state state :verification_complete false)
          lines (r/compare-facts recorded (r/receipt-facts now))]
      (is (some #{(str "state DIFFERS \"committed\" " (pr-str state))} lines))
      (is (some #{"verification_complete DIFFERS true false"} lines))))
  (is (= (r/receipt-facts recorded) (r/receipt-facts (assoc recorded :elapsed_ms 900 :receipt_id "new")))))
(deftest profile-paths-and-literal-preservation
  (let [config {:verification-profiles {"split-unit"
                                        {:commands [["bin/kaocha" "unit" "--fail-fast"]
                                                    ["python3" "{{fixture}}/cc-oracle.py" "{{scratch}}"]]}}}
        commands (get-in (r/rebind-config config "/a space" "/f\"x")
                         [:verification-profiles "split-unit" :commands])]
    (is (= ["bin/kaocha" "unit" "--fail-fast"] (first commands)))
    (is (= ["python3" "/f\"x/cc-oracle.py" "/a space"] (second commands)))
    (is (= {} (r/rebind-config {} "/s" "/f")))))
(deftest only-unmutated-typed-refusals-fall-back
  (let [r {:state "refused" :error_type "verification-unavailable"
           :mutation_attempted false :committed false}]
    (is (r/plan-fallback? r))
    (doseq [untyped [nil false "" :unknown]]
      (is (not (r/plan-fallback? (assoc r :error_type untyped)))))
    (doseq [k (keys r)] (is (not (r/plan-fallback? (dissoc r k)))))
    (doseq [r [(assoc r :state "rolled-back") (assoc r :state "committed")
               (assoc r :mutation_attempted true) (assoc r :committed true)]]
      (is (not (r/plan-fallback? r))))))
(let [{:keys [fail error]} (run-tests 'replay-test)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
