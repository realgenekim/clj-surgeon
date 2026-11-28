(ns replay-test
  (:require [babashka.fs :as fs] [clojure.edn :as edn]
            [clojure.test :refer [deftest is run-tests]]))
(load-file (str (fs/path (fs/parent *file*) "replay.clj")))
(require '[replay :as r])
(def fixture (fs/parent (fs/absolutize *file*)))
(def recorded (edn/read-string (slurp (str (fs/path fixture "sentinel-20261025-split-receipt.edn")))))
(def facts (r/receipt-facts recorded))
(deftest root-only-rebinding
  (let [request {:workspace_root "/old" :source {:file "views.clj"} :verification {:profile "split-unit"}}]
    (is (= (assoc request :workspace_root "/new") (r/rebind-request request "/new")))))
(deftest frozen-facts-and-comparison-matrix
  (is (= {:state "committed" :verification_complete true :caller_sites 87 :caller_files 5
          :destinations 20 :created 20} (dissoc facts :unexpected_paths :unrequired_qualified_refs)))
  (is (= 3 (count (:unrequired_qualified_refs facts))))
  (is (= ["caller_files same" "caller_sites same" "created same (20 destination files present on disk)"
          "destinations same" "state same"
          "unexpected_paths expected-different recorded artefacts: 3, replay: 3"
          "unrequired_qualified_refs same" "verification_complete same"] (r/compare-facts recorded facts)))
  (doseq [[k v] (dissoc facts :unexpected_paths)]
    (is (some #{(str (name k) " DIFFERS " (pr-str v) " :changed")}
              (r/compare-facts recorded (assoc facts k :changed))))))
(deftest created-count-from-literal-files
  (doseq [[before after n] [[[] [] 0] [[] ["a" "b" "other"] 2] [["a"] ["a" "b"] 1]]]
    (is (= n (r/created-count ["a" "b" "a"] before after)))
    (is (some #{(str "created DIFFERS 20 " n)} (r/compare-facts recorded (assoc facts :created n))))))
(deftest unrelated-unexpected-file-fails
  (doseq [paths [nil [] [".clj-surgeon.edn"] (:unexpected_paths facts)]]
    (is (= 0 (r/comparison-exit (r/compare-facts recorded (assoc facts :unexpected_paths paths))))))
  (doseq [paths [["unrelated.txt"] ["00SERVER-LOGS.txt" "unrelated.txt"] :invalid]]
    (let [lines (r/compare-facts recorded (assoc facts :unexpected_paths paths))]
      (is (= 1 (r/comparison-exit lines))) (is (some #(.contains % (pr-str paths)) lines)))))
(deftest matching-receipt-nonzero-exit-fails
  (let [lines (r/compare-facts recorded facts)]
    (is (= 0 (r/comparison-exit lines 0)))
    (doseq [exit [1 97 nil]] (is (= 1 (r/comparison-exit lines exit))))))
(deftest missing-is-not-success
  (is (every? #(.contains % " DIFFERS ") (r/compare-facts {} (r/receipt-facts {}))))
  (is (= :replay/missing (:created (r/receipt-facts {}))))
  (is (= 1 (r/comparison-exit (r/compare-facts recorded (r/receipt-facts {}))))))
(deftest planned-and-refused-stay-different
  (doseq [state ["planned" "refused"]]
    (let [lines (r/compare-facts recorded (r/receipt-facts (assoc recorded :state state)))]
      (is (some #{(str "state DIFFERS \"committed\" " (pr-str state))} lines))))
  (is (= facts (r/receipt-facts (assoc recorded :elapsed_ms 900 :receipt_id "new")))))
(deftest frozen-profile-paths
  (let [config (edn/read-string (slurp (str (fs/path fixture "workspace.clj-surgeon.edn"))))
        commands (get-in (r/rebind-config config "/a space" "/f\"x")
                         [:verification-profiles "split-unit" :commands])]
    (is (= [["bin/kaocha" "unit" "--fail-fast"]
            ["python3" "/f\"x/cc-oracle-replay.py" "/a space" "/f\"x/cc-split.json"]] commands))))
(deftest only-unmutated-typed-refusals-fall-back
  (let [receipt {:state "refused" :error_type "verification-unavailable"
                 :mutation_attempted false :committed false}]
    (is (r/plan-fallback? receipt))
    (doseq [untyped [nil false "" :unknown]]
      (is (not (r/plan-fallback? (assoc receipt :error_type untyped)))))
    (doseq [k (keys receipt)] (is (not (r/plan-fallback? (dissoc receipt k)))))
    (doseq [change [{:state "rolled-back"} {:state "committed"} {:mutation_attempted true} {:committed true}]]
      (is (not (r/plan-fallback? (merge receipt change)))))))
(deftest frozen-digests (is (nil? (r/verify-manifest! fixture))))
(let [{:keys [fail error]} (run-tests 'replay-test)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
