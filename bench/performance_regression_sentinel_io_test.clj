(ns performance-regression-sentinel-io-test
  (:require
   [clojure.java.io :as jio]
   [clojure.test :refer [deftest is testing]]
   [performance-regression-sentinel-io :as sentinel-io])
  (:import
   (java.nio.file Files Path)))

(def sha-a (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))
(def sha-c (apply str (repeat 64 "c")))

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "perf-sentinel-io-"
                                      (make-array
                                        java.nio.file.attribute.FileAttribute
                                        0))))

(defn- delete-tree! [root]
  (doseq [file (reverse (file-seq root))]
    (Files/deleteIfExists (.toPath file))))

(defmacro with-temp-dir [[binding] & body]
  `(let [~binding (temp-dir)]
     (try
       ~@body
       (finally
         (delete-tree! ~binding)))))

(def scope
  {:route-id :sessionize-format-extraction/apply-v1
   :task-sha256 sha-a
   :policy-version 1})

(def stable {:commit (apply str (repeat 40 "a"))
             :tree (apply str (repeat 40 "b"))})

(def candidate {:commit (apply str (repeat 40 "c"))
                :tree (apply str (repeat 40 "d"))})

(defn- body [invocation-id status]
  {:schema :clj-surgeon.performance-regression-sentinel-event-body/v1
   :invocation-id invocation-id
   :scope scope
   :stable stable
   :candidate candidate
   :status status
   :promotion-authority false
   :evidence-sha256 sha-b})

;; @spec PERF-SENT-LEDGER-ROOT-001
(deftest ledger-root-is-controller-owned-and-canonically-confined
  (with-temp-dir [root]
    (let [workspace (jio/file root "workspace")
          result-root (jio/file root "result")
          ledger (jio/file root "state/ledger")]
      (.mkdirs workspace)
      (.mkdirs result-root)
      (is (:ok (sentinel-io/validate-ledger-root
                 {:ledger-root (.getPath ledger)
                  :workspace (.getPath workspace)
                  :result-root (.getPath result-root)})))
      (doseq [invalid ["/" (.getPath workspace) (.getPath result-root)]]
        (is (= :ledger-root-refused
               (:type (sentinel-io/validate-ledger-root
                        {:ledger-root invalid
                         :workspace (.getPath workspace)
                         :result-root (.getPath result-root)})))))
      (when-not (System/getProperty "os.name")
        (is false "operating system identity must be available")))))

;; @spec PERF-SENT-AUTH-001
;; @spec PERF-SENT-AUTH-002
;; @spec PERF-SENT-AUTH-003
;; @spec PERF-SENT-RECOVERY-001
;; @spec PERF-SENT-RECOVERY-003
(deftest launch-admission-binds-authority-without-holding-the-remote-lock
  (with-temp-dir [root]
    (let [ledger (jio/file root "ledger")
          invocation-id (random-uuid)
          request {:invocation-id invocation-id
                   :mode :recovery
                   :scope scope
                   :stable stable
                   :blocked-candidate candidate
                   :candidate candidate
                   :red-event-id (random-uuid)
                   :owner {:id :release-owner
                           :authorization-receipt-sha256 sha-c}
                   :expires-at-ms (+ (System/currentTimeMillis) 60000)}
          issued (sentinel-io/issue-launch-admission!
                   {:ledger-root (.getPath ledger)
                    :request request})]
      (is (:ok issued))
      (is (false? (:lock-held-after-return issued)))
      (is (:ok (sentinel-io/validate-launch-admission
                 (:receipt issued)
                 {:expected-sha256 (:receipt-sha256 issued)
                  :invocation-id invocation-id})))
      (testing "tamper, expiry, and identity drift refuse"
        (doseq [receipt [(assoc (:receipt issued) :nonce "tampered")
                         (assoc (:receipt issued) :expires-at-ms 0)
                         (assoc-in (:receipt issued) [:stable :tree]
                                   (apply str (repeat 40 "f")))]]
          (is (false? (:ok (sentinel-io/validate-launch-admission
                             receipt
                             {:expected-sha256 (:receipt-sha256 issued)
                              :invocation-id invocation-id}))))))
      (is (= :identity-drift
             (:type (sentinel-io/revalidate-launch-authority!
                      {:ledger-root (.getPath ledger)
                       :receipt (:receipt issued)
                       :current-state {:state :clear}})))))))

;; @spec PERF-SENT-LEDGER-001
;; @spec PERF-SENT-LEDGER-002
;; @spec PERF-SENT-LEDGER-003
;; @spec PERF-SENT-LEDGER-004
(deftest append-is-atomic-idempotent-and-chain-authoritative
  (with-temp-dir [root]
    (let [ledger (jio/file root "ledger")
          invocation-id (random-uuid)
          event-body (body invocation-id :green)
          first-result (sentinel-io/append-event!
                         {:ledger-root (.getPath ledger)
                          :event-body event-body})
          replay (sentinel-io/append-event!
                   {:ledger-root (.getPath ledger)
                    :event-body event-body})]
      (is (:ok first-result))
      (is (= (:event-sha256 first-result) (:event-sha256 replay)))
      (is (= (:append-receipt-sha256 first-result)
             (:append-receipt-sha256 replay)))
      (is (= 1 (:event-count (sentinel-io/read-ledger (.getPath ledger)))))
      (is (= :invocation-replay-mismatch
             (:type (sentinel-io/append-event!
                      {:ledger-root (.getPath ledger)
                       :event-body (assoc event-body :evidence-sha256 sha-c)})))))))

;; @spec PERF-SENT-LEDGER-002
(deftest crash-after-event-write-repairs-only-the-head-cache
  (with-temp-dir [root]
    (let [ledger (jio/file root "ledger")
          event-body (body (random-uuid) :yellow)
          interrupted (sentinel-io/append-event!
                        {:ledger-root (.getPath ledger)
                         :event-body event-body
                         :test-fail-after-event-write true})]
      (is (= :coordinator-boundary-failure (:type interrupted)))
      (let [reconciled (sentinel-io/append-event!
                         {:ledger-root (.getPath ledger)
                          :event-body event-body})]
        (is (:ok reconciled))
        (is (:head-cache-repaired reconciled))
        (is (= 1 (:event-count (sentinel-io/read-ledger (.getPath ledger)))))))))

;; @spec PERF-SENT-LEDGER-002
(deftest concurrent-appends-serialize-without-lost-events
  (with-temp-dir [root]
    (let [ledger (jio/file root "ledger")
          results (->> (range 8)
                       (mapv (fn [i]
                               (future
                                 (sentinel-io/append-event!
                                   {:ledger-root (.getPath ledger)
                                    :event-body
                                    (body (random-uuid)
                                          (if (even? i) :green :yellow))}))))
                       (mapv deref))]
      (is (every? :ok results))
      (is (= 8 (:event-count (sentinel-io/read-ledger (.getPath ledger)))))
      (is (:chain-valid (sentinel-io/read-ledger (.getPath ledger)))))))

;; @spec PERF-SENT-PROJECT-001
;; @spec PERF-SENT-RECONCILE-001
(deftest projections-replay-by-event-without-changing-ledger-state
  (with-temp-dir [root]
    (let [ledger (jio/file root "ledger")
          event-body (body (random-uuid) :red)
          appended (sentinel-io/append-event!
                     {:ledger-root (.getPath ledger)
                      :event-body event-body})
          failed (sentinel-io/project-event!
                   {:ledger-root (.getPath ledger)
                    :event-id (:event-id appended)
                    :test-projection-exit 17})
          before (sentinel-io/read-ledger (.getPath ledger))
          replay (sentinel-io/project-event!
                   {:ledger-root (.getPath ledger)
                    :event-id (:event-id appended)})
          after (sentinel-io/read-ledger (.getPath ledger))]
      (is (= :projection-failure (:type failed)))
      (is (= (:head-event-sha256 before) (:head-event-sha256 after)))
      (is (:ok replay))
      (is (= (:projection-receipt-sha256 replay)
             (:projection-receipt-sha256
               (sentinel-io/project-event!
                 {:ledger-root (.getPath ledger)
                  :event-id (:event-id appended)})))))))

;; @spec PERF-SENT-IMPORT-001
;; @spec PERF-SENT-IMPORT-002
;; @spec PERF-SENT-RETAIN-001
;; @spec PERF-SENT-RECEIPT-001
;; @spec PERF-SENT-RECEIPT-002
(deftest import-recompiles-before-authority-and-receipt-is-external
  (with-temp-dir [root]
    (let [archive (jio/file root "remote.tar.gz")
          transfer (jio/file root "transfer.edn")
          local-root (jio/file root "import")]
      (spit archive "frozen archive fixture")
      (spit transfer (pr-str {:archive-sha256
                              (sentinel-io/sha256-file archive)}))
      (let [result (sentinel-io/import-result!
                     {:archive (.getPath archive)
                      :transfer-manifest (.getPath transfer)
                      :local-root (.getPath local-root)
                      :test-fixture :valid-import})]
        (is (:ok result))
        (is (:attempts-recompiled result))
        (is (:schedule-recompiled result))
        (is (not= (:publication-receipt-path result)
                  (:archive-path result)))
        (is (= (:archive-sha256 result)
               (get-in result [:publication-receipt :archive-sha256]))))
      (spit archive "tampered")
      (is (= :import-failure
             (:type (sentinel-io/import-result!
                      {:archive (.getPath archive)
                       :transfer-manifest (.getPath transfer)
                       :local-root (.getPath local-root)})))))))

;; @spec PERF-SENT-RELEASE-001
;; @spec PERF-SENT-BASELINE-001
;; @spec PERF-SENT-CUTOVER-001
;; @spec PERF-SENT-CUTOVER-002
;; @spec PERF-SENT-CUTOVER-003
(deftest release-validation-is-model-free-and-candidate-exact
  (let [receipt {:schema :clj-surgeon.performance-regression-publication/v1
                 :candidate candidate
                 :publication {:state :allowed}
                 :ledger {:state :clear}
                 :projection-complete true
                 :promotion-authority false}]
    (is (:ok (sentinel-io/verify-release receipt candidate)))
    (is (= :identity-drift
           (:type (sentinel-io/verify-release
                    receipt
                    (assoc candidate :tree (apply str (repeat 40 "f")))))))
    (is (= :publication-blocked
           (:type (sentinel-io/verify-release
                    (assoc-in receipt [:ledger :state] :blocked)
                    candidate))))
    (is (= :cutover-evidence-incomplete
           (:type (sentinel-io/verify-cutover
                    {:no-model-contract-receipt {:ok true}
                     :external-anvil-receipt nil}))))
    (is (:ok (sentinel-io/verify-cutover
               {:no-model-contract-receipt
                {:ok true :candidate candidate :sha256 sha-a}
                :external-anvil-receipt
                {:ok true
                 :candidate candidate
                 :attempt-joins-proved true
                 :adaptive-stop-proved true
                 :reverse-schedule-proved true
                 :invalid-typing-proved true
                 :retention-import-proved true
                 :ledger-projection-proved true
                 :exit-behavior-proved true
                 :promotion-refusal-proved true
                 :sha256 sha-b}})))
    (is (zero? (:model-launch-count
                 (sentinel-io/verify-release receipt candidate))))))
