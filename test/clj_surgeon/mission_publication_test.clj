(ns clj-surgeon.mission-publication-test
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

(defn with-verified [f]
  (fixture/with-fixture
    (fn [root file]
      (let [run (partial git/run-git! root) home (str (io/file root "state"))]
        (run ["init" "-q" "-b" "fixture"] nil)
        (run ["config" "user.name" "Publication fixture"] nil)
        (run ["config" "user.email" "publication@example.invalid"] nil)
        (run ["add" "--" "src/fixture/core.clj"] nil)
        (run ["commit" "-qm" "frozen fixture"] nil)
        (let [opened (cli/propose! {:verb "owner_forms" :request (fixture/request root)
                                    :profiles fixture/profiles :state-home home})
              opts {:id (:id opened) :workspace root :state-home home}
              applied (with-redefs [executor/request-candidates!
                                    (fn [_] [{:usable true :content (json/generate-string fixture/replacements)}])]
                        (cli/apply! (assoc opts :receipt-dir (str (io/file root "receipts")))))]
          (is (= :verified (:state applied)))
          (f opts file run))))))

(defn publication [opts]
  (when-let [f (ns-resolve 'clj-surgeon.mission-git-ledger 'publication-status)] (f opts)))

(deftest public-undo-refuses-after-real-git-publication
  ;; Executed reviewer defect: undo exited0, source reverted, published HEAD stood.
  (with-verified
    (fn [opts file run]
      (run ["add" "--" "src/fixture/core.clj"] nil)
      (let [argv ["bin/mission" "commit" (:id opts) "--workspace" (:workspace opts)
                  "--state-home" (:state-home opts)]
            committed (apply shell/sh argv)
            result (edn/read-string (:out committed))
            source (slurp file) head (str/trim (run ["rev-parse" "HEAD"] nil))
            dir (mission/workspace-state-dir (:workspace opts) (:state-home opts))
            saved (mission/read-mission dir (:id opts))]
        (is (= 0 (:exit committed)) (:out committed))
        (is (= head (get-in saved [:git-publication :commit])))
        (is (= (str/trim (run ["rev-parse" "HEAD^{tree}"] nil)) (get-in saved [:git-publication :tree])))
        (is (= :published (:status (publication opts))))
        (doseq [verb ["undo" "resume"]]
          (let [r (apply shell/sh (assoc argv 1 verb)) value (edn/read-string (:out r))]
            (is (= 1 (:exit r)))
            (is (= "mission-undo-after-git-publication" (:error_type value)))
            (is (= head (:published-commit value)))
            (is (= source (slurp file)))
            (is (= head (str/trim (run ["rev-parse" "HEAD"] nil))))))
        (is (= (:commit result) (get-in (cli/show opts) [:git-publication :commit])))))))

(deftest publication-metadata-write-failure-retains-the-actual-git-outcome
  (doseq [writer [(fn [& _] (throw (ex-info "injected disk failure" {})))
                  (fn [& _] "write silently lost")]]
    (with-verified
      (fn [opts file run]
        (run ["add" "--" "src/fixture/core.clj"] nil)
        (let [source (slurp file)
              r (with-redefs [mission/write-mission! writer]
                  (ledger/commit! opts))
              head (str/trim (run ["rev-parse" "HEAD"] nil))]
          (is (false? (:ok r)))
          (is (true? (:git-ref-updated r)))
          (is (false? (:metadata-recorded r)))
          (is (= head (:commit r)))
          (is (= (str/trim (run ["rev-parse" "HEAD^{tree}"] nil)) (:tree r)))
          (is (= "mission-undo-after-git-publication" (:error_type (cli/undo! opts))))
          (is (= source (slurp file))))))))

(deftest intent-exists-before-git-and-uncertainty-blocks-undo
  ;; Includes Opus finding7: a teardown error may hide a completed ref update.
  (doseq [kind [:reported-uncertain :generic-false :throw]]
    (with-verified
      (fn [opts file _]
        (let [source (slurp file) seen (atom nil) oid (apply str (repeat 40 "a"))
              r (with-redefs [git/commit! (fn [_ _]
                                            (reset! seen (publication opts))
                                            (case kind
                                              :reported-uncertain {:ok false :error-type :git-ref-update-uncertain
                                                                   :git-ref-updated :unknown :possible-commit oid}
                                              :generic-false {:ok false :error-type :git-boundary-failed :git-ref-updated false}
                                              :throw (throw (ex-info "teardown outcome is not known" {}))))]
                  (ledger/commit! opts))]
          (is (= :pending (:status @seen)))
          (is (= :unknown (:git-ref-updated r)))
          (when (= kind :reported-uncertain) (is (= oid (:possible-commit r))))
          (is (= :uncertain (:status (publication opts))))
          (is (= "mission-undo-after-git-publication" (:error_type (cli/undo! opts))))
          (is (= source (slurp file))))))))

(deftest proven-pre-ref-refusal-clears-intent-and-permits-ordinary-undo
  (with-verified
    (fn [opts _ run]
      (let [head (str/trim (run ["rev-parse" "HEAD"] nil)) r (ledger/commit! opts)]
        (is (= :git-staged-scope (:error-type r)))
        (is (false? (:git-ref-updated r)))
        (is (nil? (publication opts)))
        (is (= :undone (:state (cli/undo! opts))))
        (is (= head (str/trim (run ["rev-parse" "HEAD"] nil))))))))

(deftest publication-markers-are-closed-and-bound-to-the-original-ledger
  (let [opts {:id "M-1" :workspace "/fixture"}
        hash (apply str (repeat 64 "a"))
        original {:hash hash}
        marker {:version 1 :id "M-1" :workspace-root "/fixture" :ledger-sha256 hash :status :pending}]
    (is (ledger/marker-valid? marker opts original))
    (doseq [bad [(assoc marker :id "M-2") (assoc marker :workspace-root "/other")
                 (assoc marker :ledger-sha256 (apply str (repeat 64 "b")))
                 (assoc marker :raw {:source "not admitted"}) (assoc marker :status :ignored)
                 (assoc marker :status :published) (assoc marker :possible-commit "bad")]]
      (is (not (ledger/marker-valid? bad opts original))))))

(deftest corrupt-mismatched-and-pending-records-refuse-before-source-undo
  (fixture/with-fixture
    (fn [root file]
      (let [opts {:workspace root :state-home (str (io/file root "state")) :id "M-1"}
            dir (mission/workspace-state-dir root (:state-home opts))
            _ (mission/write-mission! dir {:id "M-1" :root root :verb "owner_forms" :state :verified})
            original (ledger/artifact (str (mission/mission-file dir "M-1")))
            marker {:version 1 :id "M-1" :workspace-root root :ledger-sha256 (:hash original) :status :pending}
            source (slurp file)]
        (doseq [text [(pr-str marker) "{:broken" (pr-str (assoc marker :id "M-2"))
                      (pr-str (assoc marker :workspace-root "/other"))]]
          (spit (ledger/publication-path opts) text)
          (is (= "mission-undo-after-git-publication" (:error_type (cli/undo! opts))))
          (is (= source (slurp file))))
        (ledger/clear-publication! opts)
        (let [locked (ledger/with-publication-lock opts #(cli/undo! opts))]
          (is (= :mission-publication-lock-busy (:error-type locked)))
          (is (= source (slurp file))))))))

(deftest final-sidecar-failure-leaves-forced-intent-and-suppresses-resume
  (with-verified
    (fn [opts file run]
      (run ["add" "--" "src/fixture/core.clj"] nil)
      (let [writer ledger/write-publication! calls (atom 0) source (slurp file)
            r (with-redefs [ledger/write-publication!
                            (fn [options marker]
                              (if (= 1 (swap! calls inc))
                                (writer options marker)
                                (throw (ex-info "injected final marker failure" {}))))]
                (ledger/commit! opts))
            shown (with-redefs [git/run-git! (fn [& _] (throw (ex-info "show must not read Git" {})))]
                    (cli/show opts))]
        (is (false? (:ok r)))
        (is (true? (:git-ref-updated r)))
        (is (= :pending (:status (publication opts))))
        (is (= :pending (get-in shown [:git-publication :status])))
        (is (nil? (:effective_next_action shown)))
        (let [read (shell/sh "bin/mission" "show" (:id opts) "--workspace" (:workspace opts)
                             "--state-home" (:state-home opts))
              displayed (edn/read-string (:out read))]
          (is (= 0 (:exit read)))
          (is (= :pending (get-in displayed [:git-publication :status])))
          (is (nil? (:effective_next_action displayed))))
        (is (str/includes? (:publication_recovery shown "") "Inspect Git"))
        (is (= "mission-undo-after-git-publication" (:error_type (cli/undo! opts))))
        (is (= source (slurp file)))))))
