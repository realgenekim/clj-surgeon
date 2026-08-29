(ns namespace-tolerance-replay-test
  (:require
   [clojure.test :refer [deftest is run-tests testing]]
   [namespace-tolerance-replay :as replay]))

(def file "src/sample/app.clj")
(def source
  "(ns sample.app\n  (:require [old.core :as old]))\n(defn f [] 1)\n")
(def sources {file source})

(def expected-retained-captures
  [["01-control" 8775
    "2942a32a06d68c45d436ce8cc47159434fde2908d9c5f19a8e046c297fafc3b9"]
   ["02-candidate" 5724
    "4934cb85fc74c161ae321bd55d5ee0134c1647d2cefc6fa00bb38e524628ce31"]
   ["03-candidate" 5724
    "e3e487cfce6bed40cbd59793c9cae35144044335bfef9d5d44f8019fef994c9b"]
   ["04-control" 9460
    "368cae1fe8895356965e86a127625338d706aa8c32b6fa7e404ff421643965e9"]
   ["05-candidate" 5103
    "7e627736baffed2c04fe4c1a8cd7f8edb2cf6be15493a1e0c67abf0b57152506"]
   ["06-control" 9460
    "368cae1fe8895356965e86a127625338d706aa8c32b6fa7e404ff421643965e9"]
   ["07-control" 9460
    "368cae1fe8895356965e86a127625338d706aa8c32b6fa7e404ff421643965e9"]
   ["08-candidate" 5724
    "4934cb85fc74c161ae321bd55d5ee0134c1647d2cefc6fa00bb38e524628ce31"]])

(def expected-candidate-owner-match-rows
  [["src/sample/review_updates.clj" "push-person-row" 1]
   ["src/sample/review_updates.clj" "push-active-row" 1]
   ["src/sample/views/log.clj" "describe-rating" 3]
   ["src/sample/views/people.clj" "reviewer-summary" 2]
   ["src/sample/views/people.clj" "rating-row" 1]
   ["src/sample/views/review.clj" "content-status-control" 1]
   ["src/sample/views/review.clj" "submission-detail-page" 1]
   ["src/sample/views/review.clj" "submission-detail-page" 1]
   ["src/sample/views/review.clj" "review-summary" 1]
   ["src/sample/views/review.clj" "review-summary" 1]
   ["src/sample/views/review.clj" "board-page" 1]
   ["src/sample/views/review.clj" "board-page" 1]
   ["test/sample/board_test.clj" "render-unrated" 1]
   ["test/sample/board_test.clj" "render-rated" 1]
   ["test/sample/reviews_test.clj" "render-result" 1]
   ["test/sample/reviews_test.clj" "render-weighted" 2]
   ["test/sample/reviews_test.clj" "render-visible" 1]
   ["test/sample/status_workflow_test.clj" "render-status" 1]
   ["test/sample/views_test.clj" "render-opinions" 1]
   ["test/sample/views_test.clj" "render-histogram" 1]
   ["test/sample/voting_policy_test.clj" "visible-row" 1]
   ["test/sample/voting_policy_test.clj" "hidden-row" 1]
   ["test/sample/voting_policy_test.clj" "revealed-row" 1]])

(deftest law-a-requires-exact-uncontested-direct-namespace-name
  (let [edit (replay/base-edit file)
        lowered (replay/lower-law-a sources edit)]
    (is (= {"namespace" "sample.app"} (get lowered "within")))
    (is (= "sample.app" (get-in lowered ["within" "namespace"])))
    (doseq [[label candidate-sources candidate-edit]
            [[:wrong-name sources
              (assoc-in edit ["within" "form"] "sample.wrong")]
             [:competing-owner
              {file (str source "(def sample.app 1)\n")} edit]
             [:multiple-namespaces
              {file (str source "(ns sample.other)\n")} edit]
             [:reader-conditional
              {file "#?(:clj (ns sample.app (:require [old.core :as old])))\n"}
              edit]]]
      (testing (name label)
        (is (nil? (replay/lower-law-a candidate-sources candidate-edit)))))))

(deftest law-b-requires-direct-uncontested-namespace-clause-children
  (let [edit {"files" [file]
              "from" "(:require [old.core :as old])"
              "to" "(:require [new.core :as new])"
              "matches" 1}
        lowered (replay/lower-law-b sources edit)]
    (is (= file (get lowered "file")))
    (is (nil? (get lowered "files")))
    (is (= {"namespace" true} (get lowered "within")))
    (doseq [[label candidate-sources candidate]
            [[:non-namespace sources
              (assoc edit "from" "(defn f [] 1)" "to" "(defn f [] 2)")]
             [:stale-count sources (assoc edit "matches" 2)]
             [:kind-mismatch sources
              (assoc edit "to" "(:import java.time.Instant)")]
             [:nested-only
              {file "(ns sample.app {:probe (:require [old.core :as old])})\n"}
              edit]
             [:competing-outside
              {file (str source
                         "(def competing '(:require [old.core :as old]))\n")}
              edit]
             [:empty-files sources (assoc edit "files" [])]
             [:many-files sources
              (assoc edit "files" [file "src/sample/other.clj"])]
             [:file-and-files sources (assoc edit "file" file)]]]
      (testing (name label)
        (is (nil? (replay/lower-law-b candidate-sources candidate)))))))

(deftest optional-law-c-requires-the-same-unique-complete-named-owner
  (let [edit {"files" [file]
              "from" "(defn f [] 1)"
              "to" "(defn f [] 2)"
              "matches" 1}
        lowered (replay/lower-law-c sources edit)]
    (is (= file (get lowered "file")))
    (is (= {"form" "f"} (get lowered "within")))
    (doseq [[label candidate-sources candidate]
            [[:zero {file "(ns sample.app)\n(defn other [] 1)\n"} edit]
             [:many {file (str source "(defn f [] 1)\n")} edit]
             [:anonymous sources
              (assoc edit "from" "(+ 1 2)" "to" "(+ 2 3)")]
             [:different-kind sources (assoc edit "to" "(def f 2)")]
             [:different-name sources (assoc edit "to" "(defn g [] 2)")]
             [:nested-only
              {file "(ns sample.app)\n(def nested '(defn f [] 1))\n"} edit]
             [:stale-count sources (assoc edit "matches" 2)]]]
      (testing (name label)
        (is (nil? (replay/lower-law-c candidate-sources candidate)))))))

(deftest retained-replay-preserves-the-two-law-stop-and-separates-law-c
  (let [result (replay/report)]
    (is (:experiment-green result))
    (is (= 8 (:capture-count result)))
    (is (= 7 (get-in result [:two-law :exact-run-count])))
    (is (false? (get-in result [:two-law :all-eight-exact])))
    (is (= 8 (get-in result [:optional-law-c :exact-run-count])))
    (is (get-in result [:optional-law-c :all-eight-exact]))
    (is (get-in result [:candidate-migration :expanded-before-tolerance]))
    (is (get-in result
                [:candidate-migration :all-preserve-23-owners-27-matches]))
    (is (:all-falsifiers-refuse result))
    (is (zero? (:model-calls result)))
    (is (zero? (:mutation-actions result)))))

(deftest product-normalizer-replays-all-eight-retained-calls
  ;; @spec MCP-OP-EDIT-016
  (let [result (replay/product-report)]
    (is (= 8 (:capture-count result)))
    (is (= 4 (:unique-request-count result)))
    (is (:raw-corpus-bound result))
    (is (:request-hashes-equal result))
    (is (:owner-match-rows-exact result))
    (is (= expected-candidate-owner-match-rows
           (:candidate-owner-match-rows result)))
    (is (= expected-retained-captures
           (mapv (juxt :run :actual-capture-bytes
                       :actual-capture-sha256)
                 (:runs result))))
    (is (= 8 (:exact-run-count result)))
    (is (:all-eight-exact result))
    (doseq [run (:runs result)]
      (is (:ok run) (pr-str run))
      (is (:schema-valid run) (pr-str run))
      (is (:capture-bytes-equal run) (pr-str run))
      (is (:capture-hash-equal run) (pr-str run))
      (is (:request-hash-equal run) (pr-str run))
      (is (:owner-match-rows-preserved run) (pr-str run))
      (if (= :candidate (:base run))
        (do
          (is (= 23 (:owner-row-count run)))
          (is (= 27 (:declared-match-count run)))
          (is (= expected-candidate-owner-match-rows
                 (:owner-match-rows run))))
        (do
          (is (zero? (:owner-row-count run)))
          (is (zero? (:declared-match-count run)))
          (is (empty? (:owner-match-rows run)))))
      (is (= 51 (:match-count run)))
      (is (= 9 (:changed-file-count run)))
      (is (:future-hashes-equal run)))))

(let [{:keys [fail error]} (run-tests)]
  (when (pos? (+ fail error))
    (System/exit 1)))
