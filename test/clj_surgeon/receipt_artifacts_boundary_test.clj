(ns clj-surgeon.receipt-artifacts-boundary-test
  "Sol r10: identical publication witnesses run red on 3d55fa34 and green on
  the repaired branch. Fixtures invoke real writers; expected locations are
  literals independent of receipt-artifacts/directory. No artifact helper is
  required here, so baseline execution cannot fail merely loading new code."
  {:lane :battery}
  (:require
   [babashka.process :as proc]
   [clj-surgeon.artifact-boundary-support :as boundary]
   [clj-surgeon.battery-parallel-runner :as bp]
   [clj-surgeon.core :as core]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-admit-tool :as admit]
   [clj-surgeon.mcp-alias-migration :as migration]
   [clj-surgeon.mcp-cold-verify]
   [clj-surgeon.mcp-extraction :as kernel]
   [clj-surgeon.mcp-namespace-split-test :as split-boundary-fixture]
   [clj-surgeon.namespace-split-io :as split]
   [clj-surgeon.namespace-split-test :as split-fixture]
   [clj-surgeon.operation-algebra :as algebra]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clj-surgeon.require-change-boundary-test :as require-fixture]
   [clj-surgeon.require-change-io :as require-change]
   [clj-surgeon.txn-journal :as journal]
   [clj-surgeon.workspace-lock :as lock]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest bb-lane-child-honours-disk-tmpdir
  ;; Packet 28bbdab4 at da247b05: bb File.createTempFile ignored TMPDIR.
  ;; Launch the runner's actual bb prefix, replacing only the test payload
  ;; with a property probe so even the red witness writes nothing to /tmp.
  (doseq [[tmpdir expected] [["/var/tmp/forge/bbtower-fx" "/var/tmp/forge/bbtower-fx"]
                             [nil "/var/tmp"] ["" "/var/tmp"]
                             ["/tmp" "/var/tmp"] ["/tmp/nested" "/var/tmp"]
                             ["/dev/shm" "/var/tmp"] ["/dev/shm/nested" "/var/tmp"]
                             ["/var/tmp/space dir" "/var/tmp/space dir"]]]
    (testing (str "TMPDIR=" (pr-str tmpdir))
      (let [command (bp/bb-lane-command tmpdir "receipt.edn" '[clj-surgeon.a-test])
            prefix (take-while #(not= "test/run_all.clj" %) command)
            env (cond-> (dissoc (into {} (System/getenv)) "TMPDIR")
                  (some? tmpdir) (assoc "TMPDIR" tmpdir))
            result @(proc/process (into (vec prefix)
                                        ["-e" "(print (System/getProperty \"java.io.tmpdir\"))"])
                                  {:env env :out :string :err :string})]
        (is (= ["test/run_all.clj" "--emit-edn" "receipt.edn" "--ns" "clj-surgeon.a-test"]
               (vec (drop (count prefix) command))))
        (is (= 0 (:exit result)) (:err result))
        (is (= expected (:out result)))))))

(defn- remove-tree! [root]
  (when (.exists (io/file root))
    (doseq [file (reverse (file-seq (io/file root)))] (.delete file))))

(defn- with-workspace [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "receipt-publication-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try (f root) (finally (remove-tree! root)))))

(defn- with-envelope [roots f]
  (let [make-envelope (ns-resolve 'clj-surgeon.receipt-artifacts 'destination-envelope)
        envelope-var (ns-resolve 'clj-surgeon.receipt-artifacts '*destination-envelope*)]
    (is (some? make-envelope) "DATACODE-ENV: trusted envelope constructor exists")
    (if (and make-envelope envelope-var)
      (with-bindings {envelope-var (make-envelope (mapv str roots) :launcher)} (f))
      (f))))

(defn- refusal-data [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

;; @spec DATACODE-ENV-001
;; @spec DATACODE-ENV-003
;; @spec DATACODE-ENV-004
(deftest destination-envelope-guards-real-publication
  (with-workspace
    (fn [base]
      (let [allowed (io/file base "allowed") outside (io/file base "outside")
            source (io/file base "a.clj") before "(ns a) (def x :old)\n"
            spec {:changes [{:id :x :in [(str source)] :forms '[x]
                             :find ":old" :do [:replace ":new"] :expect {:matches 1}}]
                  :expect {:changes 1 :edits 1 :files 1}}]
        (.mkdirs allowed)
        (.mkdirs outside)
        (spit source before)
        (spit (io/file outside "kept.edn") "outside sentinel")
        (with-envelope [allowed]
          #(doseq [destination [(io/file outside "kept.edn")
                                (io/file outside "absent" "tail" "undo.edn")]]
             (spit source before)
             (spit (io/file outside "kept.edn") "outside sentinel")
             (let [r (transaction/execute-change!
                       {:spec spec :receipt-out (str destination)})]
               (is (= :write-outside-envelope (:error-type r)) (pr-str r))
               (is (= :receipt-publish (:effect r)))
               (is (= (str destination) (:path r)))
               (is (= before (slurp source)))
               (is (= "outside sentinel" (slurp (io/file outside "kept.edn"))))
               (is (not (.exists (io/file outside "absent")))))))
        (spit source before)
        (with-envelope [allowed]
          #(let [r (transaction/execute-change!
                     {:spec spec :receipt-out (str (io/file allowed "undo.edn"))})]
             (is (:committed r) (pr-str r))
             (is (string? (:envelope-id r)))
             (is (= (:envelope-id r)
                    (:envelope-id (edn/read-string (slurp (:receipt-file r))))))))))))

;; @spec DATACODE-ENV-001
(deftest destination-envelope-resolves-ancestor-and-final-symlinks
  (with-workspace
    (fn [base]
      (let [allowed (io/file base "allowed") outside (io/file base "outside")
            workspace (io/file base "workspace")]
        (.mkdirs allowed) (.mkdirs outside) (.mkdirs workspace)
        (spit (io/file outside "kept.edn") "sentinel")
        (binding [artifacts/*artifact-root* (str allowed)]
          (let [dir (io/file (artifacts/directory "envelope" workspace))]
            (.mkdirs dir)
            (doseq [[name destination] [["ancestor" outside]
                                        ["file.edn" (io/file outside "kept.edn")]]]
              (java.nio.file.Files/createSymbolicLink
                (.toPath (io/file dir name)) (.toPath destination)
                (make-array java.nio.file.attribute.FileAttribute 0)))
            (with-envelope [allowed]
              #(doseq [tail ["ancestor/absent/undo.edn" "file.edn"]]
                 (let [r (refusal-data (fn [] (artifacts/target "envelope" workspace tail)))]
                   (is (= :write-outside-envelope (:error-type r)) (pr-str r))
                   (is (= :receipt-publish (:effect r)))
                   (is (string? (:resolved-path r))))))
            (is (= "sentinel" (slurp (io/file outside "kept.edn"))))
            (is (not (.exists (io/file outside "absent"))))))))))

;; @spec DATACODE-ENV-001
(deftest destination-envelope-admits-ledger-before-creation
  (with-workspace
    (fn [base]
      (let [allowed (io/file base "allowed") outside (io/file base "outside")]
        (.mkdirs allowed)
        (with-envelope [allowed]
          #(binding [artifacts/*artifact-root* (str outside)]
             (let [r (refusal-data (fn [] (migration/append-telemetry! {:witness true})))]
               (is (= :write-outside-envelope (:error-type r)) (pr-str r))
               (is (= :telemetry-append (:effect r)))
               (is (not (.exists outside))))))
        (.mkdirs outside)
        (spit (io/file outside "kept.edn") "sentinel")
        (.mkdirs (io/file allowed "alias-migration-receipts"))
        (java.nio.file.Files/createSymbolicLink
          (.toPath (io/file allowed "alias-migration-receipts" "ledger.edn"))
          (.toPath (io/file outside "kept.edn"))
          (make-array java.nio.file.attribute.FileAttribute 0))
        (with-envelope [allowed]
          #(binding [artifacts/*artifact-root* (str allowed)]
             (let [r (refusal-data (fn [] (migration/append-telemetry! {:witness true})))]
               (is (= :write-outside-envelope (:error-type r)) (pr-str r))
               (is (= :telemetry-append (:effect r)))
               (is (= "sentinel" (slurp (io/file outside "kept.edn")))))))))))

;; @spec DATACODE-ENV-002
;; @spec DATACODE-ENV-003
(deftest destination-envelope-is-trusted-context-only
  (let [context {:operation :change :operation-version 1 :entrance :cli
                 :policy :cli-legacy :lifecycle :commit
                 :destination-envelope {:id "92973cc3973923e812d4f77d80bfb4ea1a518b32890bff2d0462de570ba683a9"
                                        :roots ["/var/tmp/owned"] :source :launcher}}
        r (algebra/derive-capabilities (algebra/change-entry identity) context)]
    (is (:ok r) (pr-str r))
    (is (= (:destination-envelope context) (:destination-envelope r)))
    (is (= :unknown-arguments
           (:error-type (transaction/execute-change!
                          {:destination-envelope (:destination-envelope context)})))))
  (when-let [policy (ns-resolve 'clj-surgeon.receipt-artifacts 'policy-envelope-roots)]
    (is (= ["/var/tmp" "/home/seat/.local/state/clj-surgeon" "/work"]
           (policy {:tmpdir "/tmp/unsafe" :home "/home/seat" :workspace "/work"})))
    (is (= ["/disk/tmp" "/disk/artifacts" "/work"]
           (policy {:tmpdir "/disk/tmp" :home "/home/seat" :workspace "/work"
                    :artifact-root "/disk/artifacts"}))))
  (testing "missing launcher authority uses the bounded default; startup authority is retained"
    (with-redefs-fn
      {(ns-resolve 'clj-surgeon.receipt-artifacts 'launcher-envelope) (atom nil)}
      #(binding [artifacts/*destination-envelope* nil]
         (let [default (artifacts/current-envelope)
               narrow (artifacts/destination-envelope ["/var/tmp/owned"] :launcher)]
           (is (= :policy-default (:source default)))
           (is (= 3 (count (:roots default))))
           (is (algebra/valid-destination-envelope? default))
           (is (= narrow (artifacts/initialize-envelope! "/work" narrow)))
           (is (= narrow (artifacts/current-envelope)))
           (is (= narrow (artifacts/initialize-envelope! "/different-workspace"))))))))

;; @spec DATACODE-ENV-001
;; @spec DATACODE-ENV-004
(deftest destination-envelope-final-filename-consumer-matrix
  ;; Real publication seams, independent of the path constructor they use.
  (with-workspace
    (fn [base]
      (let [allowed (io/file base "allowed") outside (io/file base "kept.edn")]
        (.mkdirs allowed)
        (spit outside "sentinel")
        (java.nio.file.Files/createSymbolicLink
          (.toPath (io/file allowed "detail.edn")) (.toPath outside)
          (make-array java.nio.file.attribute.FileAttribute 0))
        (doseq [[owner args]
                [['clj-surgeon.require-change-io/save! [(str allowed) "detail.edn" {}]]
                 ['clj-surgeon.namespace-split-io/save! [(str allowed) "detail.edn" {}]]
                 ['clj-surgeon.rename-alias/write-detail! [(str (io/file allowed "detail.edn")) {}]]
                 ['clj-surgeon.mcp-cold-verify/publish!
                  [{:receipt-file (str (io/file allowed "detail.edn")) :job "verify/test"}]]]]
          (require (symbol (namespace owner)))
          (with-envelope [allowed]
            #(let [r (refusal-data (fn [] (apply (resolve owner) args)))]
               (is (= :write-outside-envelope (:error-type r)) (str owner " " r))
               (is (= "sentinel" (slurp outside)) (str owner " changed outside bytes")))))))))

(defn- assert-artifact! [verb path]
  (is (string? path) (str verb " must return its artifact path"))
  (when (string? path)
    (is (boundary/published-under-root? verb path)
        (str verb " published at " path " -- outside "
             (boundary/verb-receipt-root verb)))
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
;; The skiff's alias lane, exit 39, three FAILs in this namespace: receipts
;; published at `/private/var/tmp/forge/...` while the assertion demanded a
;; literal `/var/tmp/forge/` prefix. darwin canonicalizes `/var/tmp` through a
;; symlink; the writer already canonicalizes (it has to, to prove the receipt
;; directory does not resolve inside the workspace) and the check did not.
;;
;; No Mac is reachable from here, so the symlink is MADE. This is the darwin
;; topology exactly: a declared root that is a symlink to where the bytes
;; actually land.
(deftest the-artifact-boundary-canonicalizes-both-sides
  (with-workspace
    (fn [base]
      (let [real (io/file base "private-real")
            link (io/file base "declared-link")
            workspace (io/file base "ws")]
        (.mkdirs real)
        (.mkdirs workspace)
        (java.nio.file.Files/createSymbolicLink
          (.toPath link) (.toPath real)
          (make-array java.nio.file.attribute.FileAttribute 0))
        (binding [artifacts/*artifact-root* (str link)]
          (let [dir (artifacts/directory "edit-clojure" (str workspace))
                published (io/file dir "undo.edn")]
            (.mkdirs (io/file dir))
            (spit published "{}")
            (testing "the RED fact: the writer's answer is NOT under the declared root"
              (is (str/starts-with? dir (str real)))
              (is (not (str/starts-with?
                         dir (str link java.io.File/separator "edit-clojure-receipts")))
                  "a literal starts-with against the declared root is what failed on darwin"))
            (testing "canonical on both sides accepts it"
              (is (boundary/published-under-root? "edit-clojure" (str published))))
            (testing "a plausible but nonexistent artifact fails closed by name"
              (let [error (try
                            (boundary/published-under-root?
                              "edit-clojure" (str (io/file dir "missing.edn")))
                            nil
                            (catch clojure.lang.ExceptionInfo caught caught))]
                (is (= :artifact-path-unresolvable (:error-type (ex-data error))))
                (is (= (str (io/file dir "missing.edn")) (:path (ex-data error))))))
            (testing "and still refuses a genuine escape"
              (spit (io/file workspace "undo.edn") "{}")
              (is (not (boundary/published-under-root? "edit-clojure" (str workspace "/undo.edn"))))
              (.mkdirs (io/file real "namespace-split-receipts"))
              (is (not (boundary/published-under-root? "namespace-split" (str published)))
                  "another verb's directory is not this verb's"))))))))

;; @spec ALIAS-MIGRATION-001
;; THE CLASS, not the instance. The skiff found this comparison written out
;; longhand in THREE namespaces and failed on the one whose lane it reached
;; first. A literal receipt root in a test is a darwin failure waiting for the
;; next lane to run, so no test source may carry one: every witness asks
;; `clj-surgeon.artifact-boundary-support`, which canonicalizes both sides.
(deftest no-witness-compares-an-artifact-path-against-a-literal-root
  ;; A COMPARISON against a literal root, not a literal anywhere: a fixture
  ;; that merely NAMES a receipt path is data, and forbidding data would make
  ;; this scanner wrong in the direction that gets scanners switched off.
  (let [pattern #"(?:starts-with\?|startsWith|includes\?)[^)]{0,160}\"/(?:private/)?var/tmp/[^\"]*-receipts/"
        offenders (vec (sort (for [file (file-seq (io/file "test"))
                                   :when (and (.isFile ^java.io.File file)
                                              (re-find #"\.cljc?$" (.getName ^java.io.File file))
                                              (not= "receipt_artifacts_boundary_test.clj"
                                                    (.getName ^java.io.File file)))
                                   :let [hit (re-find pattern (slurp file))]
                                   :when hit]
                               (str file " :: " hit))))]
    (is (= [] offenders)
        (str "a literal receipt root canonicalizes differently on darwin "
             "(/var/tmp -> /private/var/tmp) and the writer canonicalizes, so "
             "this comparison is red on macOS and green here. Use "
             "clj-surgeon.artifact-boundary-support/published-under-root?"))
    ;; The scanner watched going red, on its own text rather than on a file.
    (is (re-find pattern "(is (.startsWith p \"/var/tmp/forge/edit-clojure-receipts/\"))"))
    (is (re-find pattern "(str/starts-with? p \"/private/var/tmp/forge/typist-receipts/\"))"))
    (is (not (re-find pattern "(boundary/published-under-root? \"typist\" p)")))
    (is (not (re-find pattern "{:details_path \"/var/tmp/forge/require-change-receipts/d.edn\"}"))
        "a fixture VALUE is data, not a comparison")))

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
