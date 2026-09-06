(ns clj-surgeon.mission-commit-cli-test
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
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(defn commit-flags [args]
  (if-let [f (ns-resolve 'clj-surgeon.mission-cli 'commit-options)]
    (f (cli/parse-flags args))
    {:ok false :error-type :not-implemented}))

(deftest commit-options-are-closed-and-never-supply-proof
  (let [base ["commit" "M-1" "--workspace" "/fixture"]]
    (is (= {:ok true :options {:id "M-1" :workspace "/fixture"}}
           (commit-flags base)))
    (is (= {:ok true :options {:id "M-1" :workspace "/fixture" :state-home "/home-fixture"}}
           (commit-flags ["--state-home" "/home-fixture" "commit" "M-1" "--workspace" "/fixture"])))
    (doseq [bad [["commit"] ["commit" "M-1"] (conj base "extra")
                 ["commit" "../bad" "--workspace" "/fixture"]
                 ["commit" "M-1" "--workspace"]
                 (conj base "--state-home")
                 (into base ["--spec-file" "/does-not-exist"])
                 (into base ["--profiles" "override"])
                 (into base ["--config" "override"])
                 (into base ["--receipt-dir" "override"])
                 (into base ["--proof" "override"])]]
      (is (= :mission-commit-options (:error-type (commit-flags bad))) (pr-str bad)))))

(deftest dispatch-preserves-uncertain-ref-and-discloses-git-contract
  (let [called (atom [])]
    (with-redefs [ledger/commit! (fn [opts] (swap! called conj opts)
                                   {:ok false :error-type :git-ref-update-uncertain
                                    :git-ref-updated :unknown :possible-commit (apply str (repeat 40 "a"))})]
      (let [f (ns-resolve 'clj-surgeon.mission-cli 'commit!)
            result (when f (f (cli/parse-flags ["commit" "M-1" "--workspace" "/fixture"])))]
        (is (= [{:id "M-1" :workspace "/fixture"}] @called))
        (is (= :unknown (:git-ref-updated result)))
        (is (= "mission-git-commit" (:operation result)))
        (is (false? (:push-requested result)))
        (is (str/includes? (:contract result "") "hooks"))))))

(deftest public-commit-refuses-spec-without-reading-it
  (let [r (shell/sh "bin/mission" "commit" "M-1" "--workspace" "/fixture"
                    "--spec-file" "/var/tmp/forge/no-such-commit-override.edn")]
    (is (= 1 (:exit r)))
    (when (= 1 (:exit r))
      (is (= :mission-commit-options (:error-type (edn/read-string (:out r))))))
    (is (not (str/includes? (:err r) "FileNotFoundException"))))
  (let [help (:out (shell/sh "bin/mission" "help" "commit"))]
    (doseq [term ["stages nothing" "hooks" "signing" "push" "verified"]]
      (is (str/includes? help term)))))

(deftest real-public-command-publishes-only-the-saved-verified-change
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
              state-dir (mission/workspace-state-dir root home)
              ledger-file (mission/mission-file state-dir (:id opened))
              saved (slurp ledger-file)
              source (slurp file)
              parent (str/trim (run ["rev-parse" "HEAD"] nil))
              argv ["bin/mission" "commit" (:id opened) "--workspace" root "--state-home" home]]
          (is (= :verified (:state applied)))
          (let [unstaged (apply shell/sh argv)]
            (is (= 1 (:exit unstaged)))
            (when (= 1 (:exit unstaged))
              (is (false? (:git-ref-updated (edn/read-string (:out unstaged))))))
            (is (= parent (str/trim (run ["rev-parse" "HEAD"] nil))))
            (run ["add" "--" "src/fixture/core.clj"] nil)
            (let [r (shell/sh "bin/mission" "--workspace" root "--state-home" home "commit" (:id opened))]
              (is (= 0 (:exit r)) (:err r))
              (when (= 0 (:exit r))
                (let [receipt (edn/read-string (:out r))]
                  (is (true? (:git-ref-updated receipt)))
                  (is (= parent (:parent receipt)))
                  (is (= (:commit receipt) (str/trim (run ["rev-parse" "HEAD"] nil))))
                  (is (false? (:hooks-run receipt)))
                  (is (false? (:signing-requested receipt)))
                  (is (false? (:push-requested receipt)))
                  (is (false? (:index-staging receipt)))
                  (is (false? (:source-mutation-attempted receipt))))))
            (is (= source (run ["show" "HEAD:src/fixture/core.clj"] nil)))
            (is (= "src/fixture/core.clj\n" (run ["diff-tree" "--no-commit-id" "--name-only" "-r" "HEAD"] nil)))
            (is (str/includes? (run ["show" "-s" "--format=%B" "HEAD"] nil) (str "Mission: " (:id opened))))
            (is (= saved (slurp ledger-file)))
            (is (= source (slurp file)))))))))
