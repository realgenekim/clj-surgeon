(ns relation-causal-score-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [relation-causal-score :as score]))

(def sha-a (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))
(def sha-c (apply str (repeat 64 "c")))
(def sha-d (apply str (repeat 64 "d")))
(def sha-e (apply str (repeat 64 "e")))
(def sha-f (apply str (repeat 64 "f")))

(def expected-evidence
  {:canonical-transaction-sha256 sha-a
   :future-hashes-sha256 sha-b
   :receipt-sha256 sha-c
   :read-back-sha256 sha-d
   :edit-count 51
   :file-count 9
   :verification-complete true
   :next-action :none
   :verifier {:profile-sha256 sha-e
              :output-sha256 sha-f
              :exit 0}})

(defn- arguments [arm workspace]
  (cond-> {"workspace_root" workspace
           "edits" [{"file" "src/sample/views/review.clj"
                     "from" "old"
                     "to" "new"
                     "matches" 1}]
           "delete_owners" [{"file" "src/sample/views/review.clj"
                             "forms" ["old-owner"]}]
           "verify" "exact"}
    (= arm :R)
    (assoc "symbol_migration" {"target_alias" "submission-row"
                               "files" [["src/sample/views/review.clj"
                                         [["owner" "review/owner" 1]]]]}
           "require_change" {"add" {"lib" "sample.views.submission-row"
                                    "as" "submission-row"}
                             "files" [{"file" "src/sample/views/review.clj"}]})))

(defn- row
  ([run-id block position arm t-emit-ms t-verified-ms]
   (row run-id block position arm t-emit-ms t-verified-ms {}))
  ([run-id block position arm t-emit-ms t-verified-ms overrides]
   (let [workspace (str "/private/tmp/edit025-" run-id)
         start-ns 1000000000
         call-start-ns (+ start-ns (long (* t-emit-ms 1000000)))
         call-complete-ns (+ call-start-ns 1000000)
         turn-complete-ns (+ start-ns (long (* t-verified-ms 1000000)))
         call-id (str "call-" run-id)
         base {:run-id run-id
               :block block
               :position position
               :arm arm
               :workspace-root workspace
               :expected-evidence expected-evidence
               :clocks {:turn-start-ns start-ns
                        :turn-completed-ns turn-complete-ns}
               :events [{:event :turn-started
                         :observer-monotonic-ns start-ns}
                        {:event :item-started
                         :observer-monotonic-ns call-start-ns
                         :item {:id call-id
                                :type :mcp-tool-call
                                :server "clj-surgeon"
                                :tool "apply_clojure_changes"
                                :arguments (arguments arm workspace)}}
                        {:event :item-completed
                         :observer-monotonic-ns call-complete-ns
                         :item {:id call-id
                                :type :mcp-tool-call
                                :server "clj-surgeon"
                                :tool "apply_clojure_changes"
                                :status :completed
                                :evidence expected-evidence}}
                        {:event :item-completed
                         :observer-monotonic-ns (dec turn-complete-ns)
                         :item {:id (str "message-" run-id)
                                :type :agent-message}}
                        {:event :turn-completed
                         :observer-monotonic-ns turn-complete-ns}]}
         merged (merge base overrides)]
     (if-let [event-update (:event-update overrides)]
       (-> merged
           (dissoc :event-update)
           (update :events event-update))
       merged))))

(def passing
  [(row "b1-n1" 1 1 :N 100.0 1000.0)
   (row "b1-r1" 1 2 :R 70.0 700.0)
   (row "b1-r2" 1 3 :R 70.0 700.0)
   (row "b1-n2" 1 4 :N 100.0 1000.0)
   (row "b2-r1" 2 1 :R 70.0 700.0)
   (row "b2-n1" 2 2 :N 100.0 1000.0)
   (row "b2-n2" 2 3 :N 100.0 1000.0)
   (row "b2-r2" 2 4 :R 70.0 700.0)])

;; @spec MCP-OP-EDIT-025
(deftest score-run-requires-one-joined-first-action
  (is (:ok (score/score-run (first passing))))
  (testing "a preamble is a route failure"
    (let [preamble {:event :item-completed
                    :observer-monotonic-ns 1000000001
                    :item {:id "preamble" :type :agent-message}}
          result (score/score-run
                   (row "b1-n1" 1 1 :N 100.0 1000.0
                        {:event-update #(vec (concat [(first %)]
                                                     [preamble]
                                                     (rest %)))}))]
      (is (false? (:ok result)))
      (is (some #{:first-action-not-apply} (:errors result)))))
  (testing "start and completion must join exactly once, in order"
    (doseq [[label update-event expected-error]
            [[:different-id #(assoc-in % [2 :item :id] "other") :call-id-mismatch]
             [:completion-before-start #(vec [(nth % 0) (nth % 2) (nth % 1)
                                              (nth % 3) (nth % 4)])
              :call-lifecycle-invalid]
             [:duplicate-start #(vec (concat (subvec % 0 2) [(nth % 1)] (subvec % 2)))
              :mcp-call-count-invalid]
             [:second-call #(vec (concat (subvec % 0 3)
                                         [(assoc (nth % 1)
                                                 :observer-monotonic-ns 1200000000)]
                                         (subvec % 3)))
              :mcp-call-count-invalid]]]
      (let [result (score/score-run
                     (row "b1-n1" 1 1 :N 100.0 1000.0
                          {:event-update update-event}))]
        (is (false? (:ok result)) (name label))
        (is (some #{expected-error} (:errors result)) (name label)))))
  (testing "turn boundaries and event order are raw evidence"
    (doseq [[label candidate expected-error]
            [[:missing-turn-start
              (update (first passing) :events #(vec (rest %)))
              :turn-lifecycle-invalid]
             [:clock-not-bound
              (assoc-in (first passing) [:clocks :turn-start-ns] 900000000)
              :turn-lifecycle-invalid]
             [:nonmonotonic-reasoning
              (update (first passing) :events
                      #(vec (concat (subvec % 0 1)
                                    [{:event :item-completed
                                      :observer-monotonic-ns 2000000000
                                      :item {:id "reasoning"
                                             :type :reasoning}}]
                                    (subvec % 1))))
              :event-order-invalid]]]
      (let [result (score/score-run candidate)]
        (is (false? (:ok result)) (name label))
        (is (some #{expected-error} (:errors result)) (name label)))))
  (testing "shell and file actions are forbidden in either event direction"
    (doseq [item-type [:command-execution :file-change]]
      (let [forbidden {:event :item-completed
                       :observer-monotonic-ns 1200000000
                       :item {:id (name item-type) :type item-type}}
            result (score/score-run
                     (row "b1-n1" 1 1 :N 100.0 1000.0
                          {:event-update #(conj (vec %) forbidden)}))]
        (is (false? (:ok result)))
        (is (some #{:forbidden-action} (:errors result)))))))

(deftest score-run-requires-assigned-representation-workspace-and-exact-verifier
  (testing "N omits relations and R supplies the complete pair"
    (doseq [[label candidate expected-error]
            [[:n-with-relations
              (assoc-in (first passing) [:events 1 :item :arguments]
                        (arguments :R "/private/tmp/edit025-b1-n1"))
              :representation-mismatch]
             [:r-with-flat-only
              (assoc-in (second passing) [:events 1 :item :arguments]
                        (arguments :N "/private/tmp/edit025-b1-r1"))
              :representation-mismatch]
             [:r-with-partial-relation
              (update-in (second passing) [:events 1 :item :arguments]
                         dissoc "require_change")
              :representation-mismatch]]]
      (let [result (score/score-run candidate)]
        (is (false? (:ok result)) (name label))
        (is (some #{expected-error} (:errors result)) (name label)))))
  (testing "workspace identity and lexical canonicality are mandatory"
    (doseq [workspace ["relative/workspace"
                       "/private/tmp/../tmp/edit025-b1-n1"
                       "/private//tmp/edit025-b1-n1"]]
      (let [result (score/score-run (assoc (first passing)
                                           :workspace-root workspace))]
        (is (false? (:ok result)))
        (is (some #{:workspace-not-canonical} (:errors result)))))
    (let [result (score/score-run
                   (assoc-in (first passing)
                             [:events 1 :item :arguments "workspace_root"]
                             "/private/tmp/different"))]
      (is (false? (:ok result)))
      (is (some #{:workspace-mismatch} (:errors result)))))
  (testing "verify exact and exact evidence equality are not summary booleans"
    (doseq [[label candidate expected-error]
            [[:verify-fast
              (assoc-in (first passing)
                        [:events 1 :item :arguments "verify"] "fast")
              :verify-not-exact]
             [:wrong-future
              (assoc-in (first passing)
                        [:events 2 :item :evidence :future-hashes-sha256] sha-a)
              :evidence-mismatch]
             [:missing-receipt
              (update-in (first passing)
                         [:events 2 :item :evidence] dissoc :receipt-sha256)
              :evidence-incomplete]
             [:unverified
              (assoc-in (first passing)
                        [:events 2 :item :evidence :verification-complete] false)
              :evidence-incomplete]
             [:bad-verifier-exit
              (assoc-in (first passing)
                        [:events 2 :item :evidence :verifier :exit] 3)
              :evidence-incomplete]
             [:missing-verifier-exit
              (update-in (first passing)
                         [:events 2 :item :evidence :verifier] dissoc :exit)
              :evidence-incomplete]]]
      (let [result (score/score-run candidate)]
        (is (false? (:ok result)) (name label))
        (is (some #{expected-error} (:errors result)) (name label))))))

(deftest score-run-derives-positive-finite-clocks
  (let [result (score/score-run (first passing))]
    (is (= 100.0 (get-in result [:metrics :t-emit-ms])))
    (is (= 1000.0 (get-in result [:metrics :t-verified-ms]))))
  (doseq [[label candidate]
          [[:zero-emit
            (assoc-in (first passing) [:events 1 :observer-monotonic-ns]
                      1000000000)]
           [:negative-verified
            (assoc-in (first passing) [:clocks :turn-completed-ns] 999999999)]
           [:nan-clock
            (assoc-in (first passing) [:clocks :turn-completed-ns] Double/NaN)]
           [:completion-after-turn
            (assoc-in (first passing) [:events 2 :observer-monotonic-ns]
                      3000000000)]]]
    (let [result (score/score-run candidate)]
      (is (false? (:ok result)) (name label))
      (is (some #{:clock-invalid} (:errors result)) (name label)))))

;; @spec MCP-OP-EDIT-025
(deftest cohort-enforces-ledger-schedules-stop-law-and-promotion-gates
  (let [block-1 (subvec passing 0 4)
        block-report (score/cohort-report block-1)
        full-report (score/cohort-report passing)]
    (is (:ok block-report))
    (is (get-in block-report [:gate :block-2-authorized]))
    (is (false? (get-in block-report [:gate :promote])))
    (is (:ok full-report))
    (is (get-in full-report [:gate :promote]))
    (is (= 0.3 (get-in full-report [:pooled :t-emit-improvement])))
    (is (= 0.3 (get-in full-report [:pooled :t-verified-improvement]))))
  (testing "missing, extra, duplicate, and reordered rows fail closed"
    (doseq [[label rows]
            [[:missing (pop passing)]
             [:extra (conj passing (last passing))]
             [:duplicate-id (assoc-in passing [7 :run-id] "b2-r1")]
             [:shared-workspace
              (let [shared (:workspace-root (first passing))]
                (-> passing
                    (assoc-in [7 :workspace-root] shared)
                    (assoc-in [7 :events 1 :item :arguments "workspace_root"]
                              shared)))]
             [:wrong-order (assoc passing 1 (nth passing 2)
                                  2 (nth passing 1))]]]
      (let [report (score/cohort-report rows)]
        (is (false? (:ok report)) (name label))
        (is (false? (get-in report [:gate :promote])) (name label))
        (when (= label :shared-workspace)
          (is (some #{:workspace-reused} (:errors report)))))))
  (testing "block one stops unless verified improves 15% and R emits sooner"
    (doseq [rows [(mapv #(if (= :R (:arm %))
                           (assoc-in % [:clocks :turn-completed-ns]
                                     1900000000)
                           %)
                        (subvec passing 0 4))
                  (mapv #(if (= :R (:arm %))
                           (assoc-in % [:events 1 :observer-monotonic-ns]
                                     1100000000)
                           %)
                        (subvec passing 0 4))]]
      (let [report (score/cohort-report rows)]
        (is (false? (get-in report [:gate :block-2-authorized])))
        (is (false? (get-in report [:gate :promote]))))))
  (testing "promotion needs 20% emit in each block and pooled"
    (let [weak-block-2
          (mapv #(if (and (= 2 (:block %)) (= :R (:arm %)))
                   (row (:run-id %) (:block %) (:position %) (:arm %)
                        85.0 700.0)
                   %)
                passing)
          report (score/cohort-report weak-block-2)]
      (is (false? (get-in report [:gate :promote])))
      (is (< (get-in report [:blocks 2 :t-emit-improvement]) 0.2))))
  (testing "verified wall must favor R in each block and improve 20% pooled"
    (let [block-2-loss
          (mapv #(if (and (= 2 (:block %)) (= :R (:arm %)))
                   (assoc-in % [:clocks :turn-completed-ns] 2100000000)
                   %)
                passing)]
      (is (false? (get-in (score/cohort-report block-2-loss)
                          [:gate :promote]))))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (clojure.test/run-tests 'relation-causal-score-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
