#!/usr/bin/env bb

(ns relation-causal-corpus-test
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is run-tests testing]]
   [relation-causal-corpus :as corpus]))

(def fixture-root
  "bench/fixtures/edit_portfolio/submission-row-extraction-cleanup")

(def workspace-root "/private/tmp/edit025-workspace")

(defn- fixture []
  (let [capsule (edn/read-string (slurp (io/file fixture-root "capsule.edn")))]
    {:sources
     (into {}
           (map (fn [file]
                  [file (slurp (io/file fixture-root "before" file))]))
           corpus/relation-files)
     :profile-text (slurp (io/file fixture-root "exact-profile.edn"))
     :expected-after-hashes
     (into {}
           (map (fn [[file hashes]] [file (:after hashes)]))
           (:hashes capsule))}))

(deftest requests-are-root-parameterized-and-representation-pure
  (let [flat (corpus/normalized-flat-request workspace-root)
        relation (corpus/closed-relation-request workspace-root)]
    (is (= workspace-root (get flat "workspace_root")))
    (is (= workspace-root (get relation "workspace_root")))
    (is (= "exact" (get flat "verify") (get relation "verify")))
    (is (= #{"workspace_root" "verify" "edits" "delete_owners"}
           (set (keys flat))))
    (is (= #{"workspace_root" "verify" "symbol_migration" "require_change"
             "edits" "delete_owners"}
           (set (keys relation))))
    (is (= 33 (count (get flat "edits"))))
    (is (= 1 (count (get relation "edits"))))
    (is (= 23 (count (mapcat second
                             (get-in relation ["symbol_migration" "files"])))))
    (is (= 9 (count (get-in relation ["require_change" "files"]))))
    (is (nil? (get flat "symbol_migration")))
    (is (nil? (get flat "require_change")))
    (is (not-any? #(contains? % "symbol_migration") (get flat "edits")))
    (is (not-any? #(contains? % "require_change") (get flat "edits")))))

(deftest public-schema-gate-fails-closed
  (let [{:keys [sources]} (fixture)
        flat (corpus/normalized-flat-request workspace-root)
        relation (corpus/closed-relation-request workspace-root)]
    (is (:ok (corpus/public-schema-report flat)))
    (is (:ok (corpus/public-schema-report relation)))
    (is (false? (:ok (corpus/public-schema-report
                       (assoc flat "unexpected" true)))))
    (is (false? (:ok (corpus/public-schema-report
                       (dissoc relation "require_change")))))
    (do
      (is (false? (:ok (corpus/public-schema-report
                         (assoc flat "expect" {"changes" 51})))))
      (doseq [[label invented]
              [[:changes-wrapper
                {"workspace_root" workspace-root
                 "changes" [flat]
                 "representation" "N"}]
               [:relations-wrapper
                {"workspace_root" workspace-root
                 "changes" [relation]
                 "relations" true}]
               [:arm-wrapper
                {"workspace_root" workspace-root "R" relation}]
               [:route-wrapper
                {"workspace_root" workspace-root
                 "route" "R"
                 "request" relation}]
               [:legacy-edit-shape
                (assoc-in flat ["edits" 0]
                          {"file" "src/example.clj"
                           "owner" "example-owner"
                           "before" "old"
                           "after" "new"
                           "matches" 1})]
               [:top-level-edit-owner
                (assoc-in flat ["edits" 0 "owner"] "example-owner")]
               [:legacy-delete-shape
                (assoc-in flat ["delete_owners" 0]
                          {"file" "src/example.clj"
                           "owner" "obsolete"
                           "owners" ["obsolete"]
                           "include_attached_comments" true})]
               [:symbol-migration-array
                (assoc relation "symbol_migration" [])]
               [:require-change-array
                (assoc relation "require_change" [])]
               [:invented-target-rule
                (assoc-in relation ["symbol_migration" "target_rule"]
                          "rename")]
               [:reordered-columns
                (assoc-in relation ["symbol_migration" "columns"]
                          ["from" "owner" "matches"])]]]
        (let [compiled (corpus/compile-request sources invented)]
          (is (false? (:ok (corpus/public-schema-report invented)))
              (name label))
          (is (false? (get-in compiled [:runtime-contract :ok]))
              (name label)))))))

(deftest prompt-assignment-is-the-only-prompt-byte-difference
  (let [flat (corpus/prompt-material :N workspace-root)
        relation (corpus/prompt-material :R workspace-root)
        flat-bytes (.getBytes ^String (:prompt flat) "UTF-8")
        relation-bytes (.getBytes ^String (:prompt relation) "UTF-8")
        differences
        (keep-indexed (fn [index byte]
                        (when (not= byte (aget relation-bytes index)) index))
                      flat-bytes)]
    (is (= (alength flat-bytes) (alength relation-bytes)))
    (is (= 1 (count differences)))
    (is (= (int \N) (aget flat-bytes (first differences))))
    (is (= (int \R) (aget relation-bytes (first differences))))
    (is (= (corpus/normalized-flat-request workspace-root) (:request flat)))
    (is (= (corpus/closed-relation-request workspace-root) (:request relation)))
    (is (= (:request flat) (edn/read-string (:request-edn flat))))
    (is (= (:request relation) (edn/read-string (:request-edn relation))))
    (is (= (:request flat) (json/parse-string (:request-json flat))))
    (do
      (is (= (:request relation) (json/parse-string (:request-json relation))))
      (doseq [required ["Send one arguments object directly"
                        "do not wrap it in changes, representation, relations, route, arm, N, R"
                        "exact top-level fields for N are workspace_root, verify, edits, delete_owners"
                        "exact top-level fields for R are workspace_root, verify, symbol_migration, require_change, edits, delete_owners"
                        "Always include the explicit workspace_root and verify=exact"
                        "each edits row is {file,within:{namespace:true|form:<owner>},from,to,matches}"
                        "each delete_owners row is {file,forms:[...]}"
                        "symbol_migration is one object with target_alias derived from the supplied target symbols"
                        "target_rule exactly \"preserve-name\""
                        "columns exactly [\"owner\",\"from\",\"matches\"]"
                        "files shaped [[file,[[owner,from,matches]...]]...]"
                        "require_change is one object {add:{lib,as},files:[{file,optional remove:{lib,as}}...]}"
                        "Never use before/after, a top-level owner in an edit"
                        "owners or include_attached_comments in a deletion"
                        "arrays for symbol_migration/require_change"
                        "replace only the repeated require-change and symbol-rewrite rows"
                        "same bespoke edit explicit in edits"
                        "same moved-owner deletion explicit in delete_owners"]]
        (is (str/includes? (:prompt flat) required)))
      (doseq [payload-fragment ["push-person-row"
                                "submission-row cleanup"
                                "src/sample/review_updates.clj"
                                "chair-on-event?"]]
        (is (not (str/includes? (:prompt flat) payload-fragment)))))))

(deftest exact-profile-is-closed-and-bound-to-the-corpus
  (let [{:keys [profile-text]} (fixture)
        report (corpus/exact-profile-report profile-text)]
    (is (:ok report))
    (is (= corpus/exact-profile-sha256 (:sha256 report)))
    (is (= :exact-exit (:acceptance report)))
    (is (= 120000 (:timeout-ms report)))
    (is (= ["clj-kondo" "--cache" "false" "--lint"
            "src/sample/review_updates.clj"
            "src/sample/views/log.clj"
            "src/sample/views/people.clj"
            "src/sample/views/review.clj"
            "test/sample/board_test.clj"
            "test/sample/reviews_test.clj"
            "test/sample/status_workflow_test.clj"
            "test/sample/views_test.clj"
            "test/sample/voting_policy_test.clj"
            "--fail-level" "error"]
           (:argv report)))
    (is (= corpus/relation-files
           (subvec (:argv report) 4 13)))))

(deftest frozen-corpus-has-public-runtime-and-compiler-parity
  (let [{:keys [sources profile-text expected-after-hashes]} (fixture)
        result (corpus/report workspace-root sources profile-text)]
    (is (:all-correct result))
    (is (= 0 (:model-calls result)))
    (is (= 0 (:mutation-actions result)))
    (is (= corpus/exact-profile-sha256
           (get-in result [:verification-profile :sha256])))
    (is (= expected-after-hashes corpus/expected-after-hashes))
    (doseq [arm [:N :R]]
      (testing (name arm)
        (is (get-in result [:arms arm :public-schema-ok]))
        (is (get-in result [:arms arm :runtime-contract-ok]))
        (is (get-in result [:arms arm :compiler-ok]))
        (is (= 51 (get-in result [:arms arm :match-count])))
        (is (= 9 (get-in result [:arms arm :changed-file-count])))
        (is (= corpus/expected-after-hashes
               (get-in result [:arms arm :future-hashes])))))
    (is (get-in result [:parity :canonical-transaction-equal]))
    (is (get-in result [:parity :future-hashes-equal]))
    (is (get-in result [:parity :expected-future-hashes-equal]))))

(deftest command-line-projection-is-stable
  (let [flat-prompt (with-out-str (corpus/-main "--prompt" "N" workspace-root))
        relation-json (with-out-str
                        (corpus/-main "--request-json" "R" workspace-root))
        report-edn (with-out-str (corpus/-main "--report" workspace-root))]
    (is (= (str (:prompt (corpus/prompt-material :N workspace-root)) "\n")
           flat-prompt))
    (is (= (corpus/closed-relation-request workspace-root)
           (json/parse-string relation-json)))
    (is (:all-correct (edn/read-string report-edn)))))

(defn -main [& _]
  (let [{:keys [fail error]} (run-tests 'relation-causal-corpus-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))

(apply -main *command-line-args*)
