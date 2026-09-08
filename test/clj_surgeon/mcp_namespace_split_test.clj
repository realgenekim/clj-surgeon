(ns clj-surgeon.mcp-namespace-split-test
  {:lane :fast}
  (:require [cheshire.core :as json]
            [clj-surgeon.namespace-split-io :as boundary]
            [clj-surgeon.namespace-split-test :as fixture]
            [clj-surgeon.synchronous-verification :as proof]
            [clj-surgeon.mcp-namespace-split :as tool]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(defn with-workspace [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "split-boundary-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (doseq [[path source] (assoc fixture/sources "deps.edn" "{:paths [\"src\" \"test\"]}")]
        (let [file (io/file root path)] (.mkdirs (.getParentFile file)) (spit file source)))
      (f (str root) (assoc fixture/request :workspace_root (str root)))
      (finally (doseq [file (reverse (file-seq root))] (.delete file))))))

(defn analysis [_] {:analysis fixture/analysis :check {:name "fixture-analysis" :exit 0 :duration_ms 1 :status "completed"}})
(def profiles {"unit" {:commands [["/bin/true"]]}})
(defn proved [& _]
  {:ok true :process_evidence [{:command ["fixture-proof"] :exit 0 :elapsed_ms 1 :finished? true}]})

;; @spec NS-SPLIT-013
(deftest closed-schema-and-complete-text-face
  (is (= false (:additionalProperties boundary/schema)))
  (is (empty? (boundary/validate-request fixture/request)))
  (doseq [request [(assoc fixture/request :sites [])
                   (assoc-in fixture/request [:source :extra] true)
                   (assoc-in fixture/request [:verification :commands] [["bad"]])]]
    (is (seq (boundary/validate-request request))))
  (let [result {:ok false :state "refused" :blockers [{:type "x"}] :elapsed_ms 1}]
    (is (= (json/parse-string (json/generate-string result))
           (json/parse-string (second (str/split (tool/summary result) #"\n" 2)))))))

;; @spec NS-SPLIT-011
;; @spec NS-SPLIT-015
(deftest plan-only-captures-once-and-never-publishes
  (with-workspace
    (fn [_ request]
      (with-redefs [boundary/analyze! analysis]
        (let [r (boundary/execute! (-> request (assoc :plan_only true :roots ["src"])
                                       (assoc-in [:destinations 0 :file] "test/app/util.clj")))]
          (is (false? (:ok r)))
          (is (= "destination-outside-roots" (:error_type r)))))))
  (with-workspace
    (fn [root request]
      (let [calls (atom 0)]
        (with-redefs [boundary/analyze! (fn [sources] (swap! calls inc) (analysis sources))]
          (let [r (boundary/execute! (assoc request :plan_only true))]
            (is (:ok r) (pr-str r))
            (is (:read_complete r))
            (is (= 1 @calls))
            (is (.exists (io/file root "src/app/views.clj")))
            (is (not (.exists (io/file root "src/app/util.clj"))))))))))

;; @spec NS-SPLIT-009
;; @spec NS-SPLIT-012
;; @spec NS-SPLIT-013
(deftest boundary-commits-one-split-and-reports-executed-checks
  (with-workspace
    (fn [root request]
      (with-redefs [boundary/analyze! analysis proof/verification-preflight (constantly nil) proof/run-proof! proved]
        (let [r (boundary/execute! {:verification-profiles profiles :receipt-dir (str root "/receipts")} request)]
          (is (:ok r) (pr-str r))
          (is (= "committed" (:state r)))
          (is (:verification_complete r))
          (is (:source_retired r))
          (is (every? number? (map :duration_ms (:checks r))))
          (is (not (.exists (io/file root "src/app/views.clj"))))
          (is (.exists (io/file (:undo_receipt r)))))))))

;; @spec NS-SPLIT-010
(deftest failing-proof-restores-the-entire-file-set
  (with-workspace
    (fn [root request]
      (java.nio.file.Files/setPosixFilePermissions (.toPath (io/file root "src/app/views.clj"))
                                                  (java.nio.file.attribute.PosixFilePermissions/fromString "rw-r-----"))
      (with-redefs [boundary/analyze! analysis proof/verification-preflight (constantly nil)
                    proof/run-proof! (fn [& _] {:ok false :process_evidence [{:command ["fixture-fail"] :exit 1 :elapsed_ms 1 :finished? true}]})]
        (let [r (boundary/execute! {:verification-profiles profiles :receipt-dir (str root "/receipts")} request)]
          (is (= "rolled-back" (:state r)) (pr-str r))
          (is (:restored r))
          (is (false? (:verification_complete r)))
          (doseq [[file source] fixture/sources] (is (= source (slurp (io/file root file)))))
          (is (= "rw-r-----" (java.nio.file.attribute.PosixFilePermissions/toString
                               (java.nio.file.Files/getPosixFilePermissions (.toPath (io/file root "src/app/views.clj"))
                                                                          (make-array java.nio.file.LinkOption 0)))))
          (is (not (.exists (io/file root "src/app/util.clj")))))))))

;; @spec NS-SPLIT-008
;; @spec NS-SPLIT-013
(deftest refused-callback-is-actionable-and-preserves-bytes
  (with-workspace
    (fn [root request]
      (with-redefs [boundary/analyze! analysis proof/verification-preflight (constantly nil)]
        (doseq [[invalid blocker]
                [[(assoc request :snapshot_hash "stale") :snapshot-drift]
                 [(assoc-in request [:destinations 0 :forms] []) :unmapped-owner]
                 [(assoc-in request [:destinations 0 :lib] "wrong.lib") :destination-lib-path-mismatch]
                 [(assoc request :destinations
                         [{:lib "app.a" :file "src/app/a.clj" :forms ["helper" "first-view"] :alias_policy ["a"]}
                          {:lib "app.b" :file "src/app/b.clj" :forms ["later" "other"] :alias_policy ["b"]}]) :cycle]]]
          (let [r (boundary/execute! {:verification-profiles profiles} invalid)]
            (is (= "refused" (:state r)))
            (is (false? (:mutation_attempted r)))
            (is (some #{blocker} (map :type (:blockers r))))
            (is (= (get fixture/sources "src/app/views.clj") (slurp (io/file root "src/app/views.clj"))))
            (is (not (.exists (io/file root "src/app/util.clj"))))))
        (let [r (boundary/execute! {:verification-profiles profiles} (assoc request :promotion_policy []))]
          (is (= "refused" (:state r)))
          (is (true? (get-in r [:next_call :plan_only])))
          (is (= (get fixture/sources "src/app/views.clj") (slurp (io/file root "src/app/views.clj")))))))))

;; @spec NS-SPLIT-014
(deftest cli-request-and-plan-only-share-the-boundary
  (with-workspace
    (fn [_ request]
      (with-redefs [boundary/analyze! analysis]
        (let [r (boundary/cli! {:op :split-ns! :request request :plan-only true})]
          (is (:ok r))
          (is (= :state (first (keys r))))
          (is (:read_complete r))
          (is (= 4 (get-in r [:counts :forms]))))))))

;; @spec NS-SPLIT-008
;; @spec NS-SPLIT-010
(deftest proof-cannot-mint-completion-after-snapshot-drift
  (with-workspace
    (fn [root request]
      (with-redefs [boundary/analyze! analysis proof/verification-preflight (constantly nil)
                    proof/run-proof! (fn [& _] (spit (io/file root "test/app/new_caller.clj") "(ns app.new-caller)\n") (proved))]
        (let [r (boundary/execute! {:verification-profiles profiles :receipt-dir (str root "/receipts")} request)]
          (is (= "rolled-back" (:state r)))
          (is (= "snapshot-drift" (:error_type r)))
          (is (.exists (io/file root "test/app/new_caller.clj")))))))
  (with-workspace
    (fn [root request]
      (with-redefs [boundary/analyze! analysis proof/verification-preflight (constantly nil)
                    proof/run-proof! (fn [& _] (spit (io/file root "src/app/util.clj") "(ns app.util)\n(def foreign 1)\n") (proved))]
        (let [r (boundary/execute! {:verification-profiles profiles :receipt-dir (str root "/receipts")} request)]
          (is (= "recovery-required" (:state r)))
          (is (true? (:source_retired r)))
          (is (= "snapshot-drift" (:error_type r)))
          (is (false? (:verification_complete r)))
          (is (= "(ns app.util)\n(def foreign 1)\n" (slurp (io/file root "src/app/util.clj")))))))))
