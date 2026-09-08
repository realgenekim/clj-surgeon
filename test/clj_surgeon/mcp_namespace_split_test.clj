(ns clj-surgeon.mcp-namespace-split-test
  {:lane :fast}
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-namespace-split :as tool]
   [clj-surgeon.namespace-split :as split]
   [clj-surgeon.namespace-split-io :as boundary]
   [clj-surgeon.namespace-split-test :as fixture]
   [clj-surgeon.namespace-split-warm :as warm]
   [clj-surgeon.synchronous-verification :as proof]
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

;; @spec NS-SPLIT-040
;; @spec NS-SPLIT-041
(deftest facts-boundary-does-not-emit-discover-or-publish
  (with-workspace
    (fn [root request]
      (let [calls (atom 0) forbidden (fn [& _] (throw (ex-info "facts crossed the effect boundary" {})))]
        (with-redefs [boundary/analyze! (fn [s] (swap! calls inc) (analysis s))
                      split/emit-split forbidden
                      warm/discover! forbidden
                      boundary/publish! forbidden proof/verification-preflight forbidden]
          (let [r (boundary/cli! {:op :split-ns! :request request :facts-only true})]
            (is (:ok r) (pr-str r))
            (is (= 1 @calls))
            (is (true? (:read_complete r)))
            (is (false? (:mutation_attempted r)))
            (is (false? (:committed r)))
            (is (= (:snapshot_hash r) (get-in r [:facts :snapshot_hash])))
            (is (= 4 (count (get-in r [:facts :owners]))))
            (is (nil? (:analysis r)))
            (is (.exists (io/file root "src/app/views.clj")))
            (is (not (.exists (io/file root "src/app/util.clj"))))))))))


;; @spec NS-SPLIT-047
;; INTENT-TEST: NS-SPLIT-047
(deftest external-profile-invalid-paths-refuse
  (with-workspace
    (fn [root request]
      (let [inside (io/file root "profile.edn")]
        (spit inside "{:verification-profiles {}}")
        (doseq [path ["profile.edn" (str inside) "/var/tmp/forge/rows-sublime/no-such-profile.edn"]]
          (let [r (boundary/execute! (assoc-in request [:verification :profile-file] path))]
            (is (= "invalid-profile-file" (:error_type r)) (pr-str r))
            (is (false? (:mutation_attempted r)))
            (is (= fixture/sources (boundary/capture! (.toPath (io/file root)) ["src" "test"])))))))))

;; @spec NS-SPLIT-047
;; INTENT-TEST: NS-SPLIT-047
(deftest cli-external-profile-option
  (let [seen (atom nil)]
    (with-redefs [boundary/execute! (fn [request] (reset! seen request) {:state "refused"})]
      (boundary/cli! {:op :split-ns! :request fixture/request :profile-file "/var/tmp/proof.edn"})
      (is (= "/var/tmp/proof.edn" (get-in @seen [:verification :profile-file])))
      (is (not (contains? @seen :profile-file))))))


;; @spec NS-SPLIT-049
;; INTENT-TEST: NS-SPLIT-049
(deftest empty-profile-refuses-with-a-specific-reason
  (is (contains? (set ((requiring-resolve 'clj-surgeon.mcp-helper-extraction/refusal-types)))
                 "helper-extraction-verification-empty-profile"))
  (let [p (proof/verification-preflight {"empty" {:commands []}} "empty" true)]
    (is (= "helper-extraction-verification-empty-profile" (:error_type p)))
    (is (= ["cold-suite"] (:proof_pending p))))
  (with-workspace
    (fn [root request]
      (let [r (boundary/execute! {:verification-profiles {"unit" {:commands []}}} request)]
        (is (= "verification-empty-profile" (:error_type r)))
        (is (false? (:verification_complete r)))
        (is (= ["cold-suite"] (:proof_pending r)))
        (is (false? (:mutation_attempted r)))
        (is (= fixture/sources (boundary/capture! (.toPath (io/file root)) ["src" "test"])))))))

;; @spec NS-SPLIT-047
;; INTENT-TEST: NS-SPLIT-047
(deftest external-profile-data-and-symlinks-fail-closed
  (with-workspace
    (fn [root request]
      (let [external (java.io.File/createTempFile "split-profile-data-" ".edn")
            inside (io/file root "inside.edn")
            link (io/file (.getParentFile external) (str (.getName external) "-link"))]
        (try
          (doseq [data ["#=(System/exit 99)" "{:verification-profiles" "[]"
                        "{:verification-profiles {42 {:commands []}}}"
                        (apply str (repeat (inc boundary/max-profile-bytes) "x"))]]
            (spit external data)
            (let [r (boundary/execute! (assoc-in request [:verification :profile-file] (str external)))]
              (is (= "invalid-profile-file" (:error_type r)))
              (is (false? (:mutation_attempted r)))))
          (spit inside "{:verification-profiles {}}")
          (java.nio.file.Files/createSymbolicLink (.toPath link) (.toPath inside)
            (make-array java.nio.file.attribute.FileAttribute 0))
          (let [r (boundary/execute! (assoc-in request [:verification :profile-file] (str link)))]
            (is (= "invalid-profile-file" (:error_type r)))
            (is (= fixture/sources (boundary/capture! (.toPath (io/file root)) ["src" "test"]))))
          (finally (.delete link) (.delete external)))))))

;; @spec NS-SPLIT-048
;; INTENT-TEST: NS-SPLIT-048
(deftest proof-completion-requires-executed-cold-evidence
  (doseq [[capability verification expected]
          [[{:proof :cold} {:ok true :process_evidence []}
            {:verification_complete false :proof_pending ["cold-suite"]}]
           [{:proof :cold} {:ok true :process_evidence [{:command ["true"] :exit 0 :finished? true}]}
            {:verification_complete false :proof_pending ["cold-suite"]}]
           [{:proof :cold} {:ok false :process_evidence [{:command ["bin/kaocha" "unit"] :exit 1 :finished? true}]}
            {:verification_complete false :proof_pending ["cold-suite"]}]
           [{:proof :cold} {:ok true :process_evidence [{:command ["bin/kaocha" "unit"] :exit 0 :finished? true}]}
            {:verification_complete true :proof_pending []}]
           [{:proof :warm :pending-commands [["bin/kaocha" "unit"]]} {:ok true}
            {:verification_complete false :proof_pending ["bin/kaocha unit"]}]
           [{:proof :warm :pending-commands []} {:ok true}
            {:verification_complete false :proof_pending ["cold-suite"]}]]]
    (is (= expected (boundary/proof-completion capability verification)))))
