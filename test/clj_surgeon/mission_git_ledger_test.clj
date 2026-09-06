(ns clj-surgeon.mission-git-ledger-test
  {:lane :battery}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-cli :as cli]
   [clj-surgeon.mission-git :as git]
   [clj-surgeon.mission-git-ledger :as ledger]
   [clj-surgeon.mission-typist-executor :as executor]
   [clj-surgeon.mission-typist-executor-test :as fixture]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(deftest saved-mission-through-real-proof-staging-and-git
  (fixture/with-fixture
    (fn [root file]
      (let [run (partial git/run-git! root)
            home (str (io/file root "state"))]
        (run ["init" "-q" "-b" "fixture"] nil)
        (run ["config" "user.name" "Mission fixture"] nil)
        (run ["config" "user.email" "mission-fixture@example.invalid"] nil)
        (run ["add" "--" "src/fixture/core.clj"] nil)
        (run ["commit" "-qm" "frozen mission fixture"] nil)
        (let [opened (cli/propose! {:verb "owner_forms" :request (fixture/request root)
                                    :profiles fixture/profiles :state-home home})
              opts {:id (:id opened) :workspace root :state-home home}
              applied (with-redefs [executor/request-candidates!
                                    (fn [_] [{:usable true :content (json/generate-string fixture/replacements)}])]
                        (cli/apply! (assoc opts :receipt-dir (str (io/file root "receipts")))))
              _ (is (= :verified (:state applied)) (pr-str (select-keys applied [:state :decision])))
              state-dir (mission/workspace-state-dir root home)
              ledger-file (mission/mission-file state-dir (:id opened))
              ledger-text (slurp ledger-file)
              saved (mission/read-mission state-dir (:id opened))
              inverse-file (get-in saved [:undo :receipt])
              inverse-text (slurp inverse-file)
              inverse (edn/read-string inverse-text)
              ledger-hash (mission/sha256 ledger-text)]
          (is (:ok (ledger/normalize saved inverse ledger-hash root)))
          (doseq [[label modified]
                  [["not verified" (assoc saved :state :failed)]
                   ["no gate result" (assoc-in saved [:receipt :candidates 0 :proof :gate :results] [])]
                   ["missing witness" (assoc-in saved [:receipt :candidates 0 :proof :acceptance :ok] false)]
                   ["wrong evidence" (assoc-in saved [:receipt :candidates 0 :proof :gate :evidence] "other")]
                   ["not committed" (assoc-in saved [:receipt :committed] false)]
                   ["wrong preimage" (assoc-in saved [:snapshot :hash] "wrong")]
                   ["wrong readback" (assoc-in saved [:receipt :verified :read-back] false)]]]
            (testing label (is (false? (:ok (ledger/normalize modified inverse ledger-hash root))))))
          (run ["add" "--" "src/fixture/core.clj"] nil)
          (let [parent (str/trim (run ["rev-parse" "HEAD"] nil))]
            (spit inverse-file (pr-str (assoc inverse :receipt-hash "tampered")))
            (is (= :git-ledger-invalid-inverse (:error-type (ledger/commit! opts))))
            (is (= parent (str/trim (run ["rev-parse" "HEAD"] nil))))
            (spit inverse-file inverse-text)
            (let [source (slurp file) result (ledger/commit! opts)]
              (is (:ok result) (pr-str result))
              (is (= parent (:parent result)))
              (is (= source (run ["show" "HEAD:src/fixture/core.clj"] nil)))
              (is (= source (slurp file)))
              (is (= "src/fixture/core.clj\n" (run ["diff-tree" "--no-commit-id" "--name-only" "-r" "HEAD"] nil)))
              (is (str/includes? (run ["show" "-s" "--format=%B" "HEAD"] nil) (str "Mission: " (:id opened))))
              (is (false? (:hooks-run result)))
              (is (false? (:source-mutation-attempted result))))))))))

(deftest public-handler-does-not-accept-proof-overrides
  (doseq [field [:proof :profiles :receipt :files :provenance]]
    (is (= :git-ledger-invalid-options
           (:error-type (ledger/commit! {:id "M-0001" :workspace "/var/tmp/forge"
                                         field {}}))))))

(deftest proof-count-is-bound-to-frozen-command-count
  (let [h (mission/sha256 "proof")
        authority {:id "gate" :evidence "saved-proof" :commands [["bb" "test.clj"]]}
        proof {:ok true :id "gate" :evidence "saved-proof"
               :results [{:finished? true :exit 0 :output-sha256 h}]}]
    (is (ledger/proof-valid? authority proof))
    (is (not (ledger/proof-valid? (update authority :commands conj ["bb" "second.clj"]) proof)))
    (is (not (ledger/proof-valid? authority (assoc-in proof [:results 0 :finished?] false))))))
