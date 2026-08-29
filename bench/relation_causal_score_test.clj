(ns relation-causal-score-test
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [relation-causal-corpus :as corpus]
   [relation-causal-score :as score])
  (:import
   (java.nio.file Files)
   (java.security MessageDigest)))

(def sha-a (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))
(def sha-c (apply str (repeat 64 "c")))
(def sha-d (apply str (repeat 64 "d")))
(def sha-e (apply str (repeat 64 "e")))
(def sha-f (apply str (repeat 64 "f")))

(defn- sha256 [text]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes ^String text "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

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
               :expected-arguments (arguments arm workspace)
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
                              shared)
                    (assoc-in [7 :expected-arguments "workspace_root"]
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

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "relation-score-"
                                      (make-array
                                        java.nio.file.attribute.FileAttribute
                                        0))))

(defn- delete-tree! [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

(defn- write-event-artifacts! [directory events clocks]
  (.mkdirs directory)
  (let [events-file (io/file directory "events.jsonl")
        clock-file (io/file directory "event-clock.tsv")
        lines (mapv #(str (json/generate-string %) "\n") events)]
    (spit events-file (apply str lines))
    (spit clock-file
          (apply str
                 (map-indexed
                   (fn [index [monotonic line]]
                     (str (inc index) "\t" monotonic "\t" (+ 1000 index)
                          "\t" (alength (.getBytes ^String line "UTF-8"))
                          "\n"))
                   (map vector clocks lines))))
    {:events-path (.getPath events-file)
     :event-clock-path (.getPath clock-file)}))

(defn- raw-run! [root run-id block position arm t-emit-ms t-verified-ms]
  (let [workspace (io/file root (str "workspace-" run-id))
        _ (.mkdirs workspace)
        workspace-path (.getCanonicalPath workspace)
        receipt-file (io/file workspace "receipt.edn")
        request (case arm
                  :N (corpus/normalized-flat-request workspace-path)
                  :R (corpus/closed-relation-request workspace-path))
        {:keys [sources]} (corpus/load-fixture)
        compiled-request (corpus/compile-request sources request)
        canonical-transaction (:canonical-transaction compiled-request)
        result-hashes (:future-hashes compiled-request)
        receipt-base {:receipt-version 2
                      :transaction-version 1
                      :operation :change!
                      :intent-count 1
                      :match-count 51
                      :changed-file-count 9
                      :files (mapv (fn [[file result-hash]]
                                     {:file (str workspace-path "/" file)
                                      :source-hash sha-f
                                      :result-hash result-hash
                                      :inverse-edits []})
                                   result-hashes)
                      :intents (:changes canonical-transaction)
                      :diff []
                      :inverse {:operation :undo-change!
                                :guarded-file-count 9}}
        receipt-hash (sha256 (pr-str receipt-base))
        receipt (assoc receipt-base :receipt-hash receipt-hash)
        _ (spit receipt-file (pr-str receipt))
        call-id (str "call-" run-id)
        structured {:ok true
                    :operation "apply_clojure_changes"
                    :committed true
                    :edits 51
                    :files 9
                    :verification_complete true
                    :read_back_hashes result-hashes
                    :undo_receipt (.getPath receipt-file)
                    :receipt_hash receipt-hash
                    :next_action "none"
                    :verification {:profile-sha256 sha-e
                                   :output-sha256 sha-f
                                   :exit 0}}
        events [{:type "thread.started" :thread_id (str "thread-" run-id)}
                {:type "turn.started"}
                {:type "item.started"
                 :item {:id call-id :type "mcp_tool_call"
                        :server "clj-surgeon"
                        :tool "apply_clojure_changes"
                        :arguments request}}
                {:type "item.completed"
                 :item {:id call-id :type "mcp_tool_call"
                        :status "completed"
                        :result {:structured_content structured}}}
                {:type "item.completed"
                 :item {:id (str "message-" run-id)
                        :type "agent_message" :text "Done."}}
                {:type "turn.completed"}]
        start 1000000000
        clocks [900000000
                start
                (+ start (long (* t-emit-ms 1000000)))
                (+ start (long (* (+ t-emit-ms 1.0) 1000000)))
                (+ start (long (* (- t-verified-ms 1.0) 1000000)))
                (+ start (long (* t-verified-ms 1000000)))]
        artifacts (write-event-artifacts! (io/file root run-id) events clocks)
        future-sha (score/canonical-sha256 result-hashes)
        expected {:canonical-transaction-sha256
                  (score/canonical-sha256 canonical-transaction)
                  :future-hashes-sha256 future-sha
                  :read-back-sha256 future-sha
                  :verifier {:profile-sha256 sha-e}}]
    (merge {:run-id run-id
            :block block
            :position position
            :arm arm
            :workspace-root workspace-path
            :expected-arguments request
            :expected-evidence expected}
           artifacts)))

(defn- raw-cohort! [root]
  (let [descriptors [["b1-n1" 1 1 :N]
                     ["b1-r1" 1 2 :R]
                     ["b1-r2" 1 3 :R]
                     ["b1-n2" 1 4 :N]
                     ["b2-r1" 2 1 :R]
                     ["b2-n1" 2 2 :N]
                     ["b2-n2" 2 3 :N]
                     ["b2-r2" 2 4 :R]]]
    (mapv (fn [[run-id block position arm]]
            (raw-run! root run-id block position arm
                      (if (= :R arm) 70.0 100.0)
                      (if (= :R arm) 700.0 1000.0)))
          descriptors)))

(deftest raw-event-clock-adapter-is-exact-and-fail-closed
  (let [root (temp-dir)]
    (try
      (let [entry (first (raw-cohort! root))
            joined (score/join-event-clock-files
                     (:events-path entry) (:event-clock-path entry))
            mapped (score/manifest-entry->row entry nil)]
        (is (:ok joined))
        (is (= 6 (count (:events joined))))
        (is (:ok mapped))
        (is (:ok (score/score-run (:row mapped))))
        (testing "manifest workspace must be its canonical directory spelling"
          (let [noncanonical (str (:workspace-root entry) "/.")
                result (score/manifest-entry->row
                         (assoc entry :workspace-root noncanonical)
                         nil)]
            (is (false? (:ok result)))
            (is (= [:workspace-not-canonical] (:errors result)))))
        (testing "missing clock rows and byte-count drift refuse"
          (let [clock (:event-clock-path entry)
                original (slurp clock)
                lines (str/split-lines original)]
            (spit clock (str (str/join "\n" (butlast lines)) "\n"))
            (is (false? (:ok (score/join-event-clock-files
                               (:events-path entry) clock))))
            (spit clock (str/replace-first original #"\t[0-9]+\n"
                                           "\t999999\n"))
            (is (false? (:ok (score/join-event-clock-files
                               (:events-path entry) clock))))))
        (testing "completion IDs and structured receipt evidence cannot drift"
          (let [raw-lines (str/split-lines (slurp (:events-path entry)))
                parsed (mapv #(json/parse-string % true) raw-lines)
                wrong-id (assoc-in parsed [3 :item :id] "different")
                wrong-receipt (assoc-in parsed
                                        [3 :item :result :structured_content
                                         :receipt_hash]
                                        sha-a)]
            (doseq [[label events expected-error]
                    [[:id wrong-id :call-id-mismatch]
                     [:receipt wrong-receipt :evidence-incomplete]]]
              (let [artifacts (write-event-artifacts!
                                (io/file root (str "bad-" (name label)))
                                events
                                [900000000 1000000000 1100000000
                                 1101000000 1999000000 2000000000])
                    result (-> (merge entry artifacts)
                               (score/manifest-entry->row nil)
                               :row
                               score/score-run)]
                (is (false? (:ok result)) (name label))
                (is (some #{expected-error} (:errors result))
                    (name label)))))))
      (finally
        (delete-tree! root)))))

(deftest coordinator-cli-scores-block-one-and-final-manifests
  (let [root (temp-dir)]
    (try
      (let [runs (raw-cohort! root)
            block-1-file (io/file root "block1.edn")
            block-2-file (io/file root "block2.edn")
            block-1-output (io/file root "block1-report.edn")
            final-output (io/file root "final-report.edn")]
        (spit block-1-file (pr-str {:runs (subvec runs 0 4)}))
        (spit block-2-file (pr-str {:runs (subvec runs 4 8)}))
        (let [block-result
              (score/run-cli!
                ["--phase" "block1"
                 "--manifest" (.getPath block-1-file)
                 "--output" (.getPath block-1-output)])
              final-result
              (score/run-cli!
                ["--phase" "final"
                 "--block1-manifest" (.getPath block-1-file)
                 "--block2-manifest" (.getPath block-2-file)
                 "--output" (.getPath final-output)])]
          (is (zero? (:exit block-result)))
          (is (get-in (edn/read-string (slurp block-1-output))
                      [:gate :block-2-authorized]))
          (is (zero? (:exit final-result)))
          (is (get-in (edn/read-string (slurp final-output))
                      [:gate :promote]))
          (is (= 2 (:exit (score/run-cli!
                            ["--phase" "final"
                             "--block1-manifest" (.getPath block-1-file)
                             "--output" (.getPath final-output)]))))))
      (finally
        (delete-tree! root)))))

(defn -main [& _]
  (let [{:keys [fail error]}
        (clojure.test/run-tests 'relation-causal-score-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
