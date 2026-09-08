(ns clj-surgeon.receipt-artifacts-boundary-test
  "Sol r10: identical publication witnesses run red on 3d55fa34 and green on
  the repaired branch. Fixtures invoke real writers; expected locations are
  literals independent of receipt-artifacts/directory. No artifact helper is
  required here, so baseline execution cannot fail merely loading new code."
  {:lane :battery}
  (:require
   [clj-surgeon.core :as core]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-admit-tool :as admit]
   [clj-surgeon.mcp-cold-verify]
   [clj-surgeon.mcp-extraction :as kernel]
   [clj-surgeon.mcp-namespace-split-test :as split-boundary-fixture]
   [clj-surgeon.namespace-split-io :as split]
   [clj-surgeon.namespace-split-test :as split-fixture]
   [clj-surgeon.require-change-boundary-test :as require-fixture]
   [clj-surgeon.require-change-io :as require-change]
   [clj-surgeon.txn-journal :as journal]
   [clj-surgeon.workspace-lock :as lock]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is]]))

(defn- remove-tree! [root]
  (when (.exists (io/file root))
    (doseq [file (reverse (file-seq (io/file root)))] (.delete file))))

(defn- with-workspace [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "receipt-publication-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try (f root) (finally (remove-tree! root)))))

(defn- assert-artifact! [verb path]
  (is (string? path) (str verb " must return its artifact path"))
  (when (string? path)
    (is (str/starts-with? path (str "/var/tmp/forge/" verb "-receipts/"))
        (str verb " published at " path))
    (is (.isAbsolute (io/file path)))
    (is (.isFile (io/file path)) (str "published artifact exists: " path))))

(defn- observe-publication!
  "Run an existing behavioral fixture and observe its real writer result.
  Existing source/undo/proof assertions remain active. Only observation is
  wrapped; no directory, receipt, write, proof or result is replaced."
  [verb operation witness keys]
  (require (symbol (namespace operation)) (symbol (namespace witness)))
  (let [op (resolve operation)
        original @op
        observed (atom [])]
    (try
      (with-redefs-fn
        {op (fn [& args]
              (let [result (apply original args)]
                (when (or (:committed result) (= "committed" (:state result))
                          (and (:receipt-file result) (get-in result [:verified :read-back])))
                  (swap! observed conj result)
                  (doseq [key keys] (assert-artifact! verb (get result key))))
                result))}
        #(t/test-vars [(resolve witness)]))
      (is (= 1 (count @observed)) (str verb " fixture must commit exactly once"))
      (finally
        (doseq [result @observed key keys :let [path (get result key)] :when path]
          (.delete (io/file path)))))))

;; @spec ALIAS-MIGRATION-001
(deftest helper-extraction-publishes-external-detail-and-inverse
  (observe-publication! "helper-extraction"
    'clj-surgeon.mcp-helper-extraction/execute!
    'clj-surgeon.mcp-helper-extraction-test/the-details-path-is-published-outside-the-workspace
    [:details_path :undo_receipt]))

;; @spec ALIAS-MIGRATION-001
(deftest compact-edit-publishes-external-inverse
  (observe-publication! "edit-clojure"
    'clj-surgeon.mcp-tool/execute-request!
    'clj-surgeon.mcp-tool-test/editor-gesture-is-exact-guarded-and-undoable
    [:undo_receipt]))

;; @spec ALIAS-MIGRATION-001
(deftest general-change-publishes-external-inverse
  (observe-publication! "edit-clojure"
    'clj-surgeon.mcp-tool/execute-request!
    'clj-surgeon.mcp-tool-test/commits-the-real-six-edit-fixture-and-undoes-it
    [:undo_receipt]))

;; @spec ALIAS-MIGRATION-001
(deftest prepared-apply-publishes-external-inverse
  (observe-publication! "apply-clojure-changes"
    'clj-surgeon.mcp-change-buffer/apply-basis!
    'clj-surgeon.mcp-change-buffer-test/apply-uses-the-retained-basis-once-and-preserves-kept-sites
    [:receipt-file]))

;; @spec ALIAS-MIGRATION-001
(deftest legacy-extraction-publishes-external-inverse
  (observe-publication! "extract"
    'clj-surgeon.extract/execute!
    'clj-surgeon.extract-test/test-extraction-receipt-undo-restores-the-original-source
    [:receipt-file]))

;; @spec ALIAS-MIGRATION-001
(deftest typist-publishes-external-inverse-after-real-proof
  (observe-publication! "typist"
    'clj-surgeon.mission-typist-executor/execute!
    'clj-surgeon.mission-typist-executor-test/real-proof-commit-and-undo
    [:undo_receipt]))

;; @spec ALIAS-MIGRATION-001
(deftest standalone-require-change-publishes-external-detail-and-inverse
  (require-fixture/with-workspace
    (fn [root request config]
      (let [result (require-change/execute! config request)]
        (try
          (is (= "committed" (:state result)) (pr-str result))
          (doseq [key [:details_path :undo_receipt]]
            (assert-artifact! "require-change" (get result key)))
          (is (not (.exists (io/file root "receipts"))))
          (is (:ok (kernel/undo! (edn/read-string (slurp (:undo_receipt result))))))
          (is (require-fixture/unchanged? root))
          (finally
            (doseq [key [:details_path :undo_receipt] :let [path (get result key)] :when path]
              (.delete (io/file path)))))))))

;; @spec ALIAS-MIGRATION-001
(deftest namespace-split-publishes-external-detail-and-inverse
  (split-fixture/with-paper-workspace
    (fn [root request]
      ;; The frozen analysis controls planning identically on both snapshots;
      ;; transaction, publication, synchronous proof and undo are real.
      (with-redefs [split/analyze! (fn [_] {:analysis split-fixture/analysis
                                            :findings [] :check {:exit 0 :duration_ms 0}})]
        (let [result (split/execute!
                       {:receipt-dir (str root "/receipts")
                        :verification-profiles {"unit" {:commands [["/bin/true"]]}}}
                       request)]
          (try
            (is (= "committed" (:state result)) (pr-str result))
            (doseq [key [:details_path :undo_receipt]]
              (assert-artifact! "namespace-split" (get result key)))
            (is (not (.exists (io/file root "receipts"))))
            (is (:ok (kernel/undo! (edn/read-string (slurp (:undo_receipt result))))))
            (finally
              (doseq [key [:details_path :undo_receipt] :let [path (get result key)] :when path]
                (.delete (io/file path))))))))))

;; @spec ALIAS-MIGRATION-001
(deftest cli-change-publishes-external-inverse
  (with-workspace
    (fn [root]
      (let [source (io/file root "a.clj")
            before "(ns a)\n(defn f [] :old)\n"]
        (spit source before)
        (let [result ((get-in core/ops-registry [:change! :handler])
                      {:receipt-out (str (io/file root "undo.edn"))
                       :spec {:changes [{:id :f :in [(str source)] :forms '[f]
                                         :find ":old" :do [:replace ":new"] :expect {:matches 1}}]
                              :expect {:changes 1 :edits 1 :files 1}}})]
          (try
            (is (:committed result) (pr-str result))
            (assert-artifact! "change" (:receipt-file result))
            (is (not (.exists (io/file root "undo.edn"))))
            (is (str/includes? (slurp source) ":new"))
            (is (:ok (transaction/execute-undo! {:receipt (:receipt-file result)})))
            (is (= before (slurp source)))
            (finally (when-let [path (:receipt-file result)] (.delete (io/file path))))))))))

;; @spec ALIAS-MIGRATION-001
(deftest advisory-lock-creates-only-an-external-lock-file
  (with-workspace
    (fn [root]
      (.mkdirs (io/file root ".clj-surgeon"))
      (let [path (str (lock/advisory-lock-file root))]
        (try
          (is (= :locked (lock/call-with-workspace-write-lock root (fn [] :locked))))
          (assert-artifact! "workspace-lock" path)
          (is (empty? (seq (.listFiles (io/file root ".clj-surgeon")))))
          (finally (.delete (io/file path))))))))

;; @spec ALIAS-MIGRATION-001
(deftest cold-proof-publication-is-external-and-workspace-isolated
  (with-workspace
    (fn [root]
      (let [first-root (io/file root "first") second-root (io/file root "second")
            receipt-file (ns-resolve 'clj-surgeon.mcp-cold-verify 'receipt-file)
            publish! (ns-resolve 'clj-surgeon.mcp-cold-verify 'publish!)]
        (.mkdirs first-root) (.mkdirs second-root)
        (let [first-path (receipt-file (str first-root) "verify/r10-shared")
              second-path (receipt-file (str second-root) "verify/r10-shared")]
          (try
            (publish! {:job "verify/r10-shared" :receipt-file first-path :status :passed :marker :first})
            (publish! {:job "verify/r10-shared" :receipt-file second-path :status :passed :marker :second})
            (is (not= (str first-path) (str second-path)))
            (doseq [path [first-path second-path]] (assert-artifact! "edit-clojure" (str path)))
            (is (= :first (:marker (edn/read-string (slurp first-path)))))
            (is (= :second (:marker (edn/read-string (slurp second-path)))))
            (finally (.delete first-path) (.delete second-path))))))))

;; @spec ALIAS-MIGRATION-001
(deftest admit-focused-runner-publishes-an-external-report
  (with-workspace
    (fn [root]
      (let [result (admit/default-test-runner
                     {:project-root (str root)
                      :focused-test {:command ["bb" "-e"
                                               "(spit (second *command-line-args*) (pr-str {\"a-test\" {:tests 1 :failures 0 :errors 0}}))"
                                               "{snapshot}" "{report}"]}}
                     {:snapshot-root (str root) :namespaces ["a-test"]})
            path (:report_file result)]
        (try
          (is (:ran result) (pr-str result))
          (is (:report_written result) (pr-str result))
          (assert-artifact! "admit-clojure-patch" path)
          (is (not (.exists (io/file root ".clj-surgeon-focused-test-report"))))
          (finally (when path (.delete (io/file path)))))))))

;; @spec ALIAS-MIGRATION-001
(deftest default-transaction-publishes-external-journal-and-preimage
  (with-workspace
    (fn [root]
      (let [source (io/file root "a.clj") before "(ns a) (def x :old)\n"]
        (spit source before)
        (let [txn (journal/begin! (str root) {})]
          (try
            (is (nil? (:error-type txn)) (pr-str (dissoc txn :state)))
            (journal/record-read! txn (str source))
            (journal/seal-read-set! txn)
            (journal/pin! txn (str source))
            (journal/stage! txn (str source) "(ns a) (def x :new)\n")
            (let [result (journal/commit! txn)]
              (is (:ok result) (pr-str result))
              (assert-artifact! "transaction" (str (io/file (:dir txn) "journal.log")))
              (is (some #(and (.isFile %) (= before (slurp %)))
                        (file-seq (io/file (:objects-dir txn)))))
              (is (= "(ns a) (def x :new)\n" (slurp source))))
            (finally (remove-tree! (:transactions-dir txn)))))))))

;; @spec NS-SPLIT-047
;; INTENT-TEST: NS-SPLIT-047
(deftest external-profile-is-read-without-workspace-artifacts
  (split-boundary-fixture/with-workspace
    (fn [root request]
      (let [external (java.io.File/createTempFile "split-external-" ".edn")]
        (try
          (spit external (pr-str {:verification-profiles {"unit" {:commands [["/bin/true"]]}}}))
          (with-redefs [split/analyze! split-boundary-fixture/analysis]
            (let [r (split/execute! {:verification-profiles {"unit" {:commands [["/bin/false"]]}}}
                                       (assoc-in request [:verification :profile-file] (str external)))
                  after (set (for [f (file-seq (io/file root)) :when (.isFile f)]
                               (str (.relativize (.toPath (io/file root)) (.toPath f)))))]
              (is (:ok r) (pr-str r))
              (is (= "committed" (:state r)))
              (is (= #{"deps.edn" "src/app/util.clj" "src/app/portal.clj" "src/app/other.clj" "test/app/caller.clj"} after))
              (is (= "{:paths [\"src\" \"test\"]}" (slurp (io/file root "deps.edn"))))
              (is (not (.exists (io/file root ".clj-surgeon.edn"))))))
          (finally (.delete external)))))))

;; @spec NS-SPLIT-048
;; INTENT-TEST: NS-SPLIT-048
(deftest row5-trivial-profile-receipt-is-honest
  ;; Exact D2-D6 profile and public receipt fields; a real /bin/true process.
  (split-boundary-fixture/with-workspace
    (fn [_ request]
      (with-redefs [split/analyze! split-boundary-fixture/analysis]
        (let [r (split/execute! {:verification-profiles {"b07-cell-b" {:commands [["/bin/true"]]}}}
                                   (assoc request :verification {:profile "b07-cell-b"}))
              check (first (filter :command (:checks r)))]
          (is (= {:state "committed" :committed true :ok true :mutation_attempted true
                  :verification_complete false :proof_pending ["cold-suite"]}
                 (select-keys r [:state :committed :ok :mutation_attempted :verification_complete :proof_pending])))
          (is (= {:name "true" :profile "b07-cell-b" :command ["/bin/true"] :exit 0 :status "passed"}
                 (dissoc check :duration_ms)))
          (is (number? (:duration_ms check))))))))
