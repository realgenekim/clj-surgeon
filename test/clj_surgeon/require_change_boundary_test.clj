(ns clj-surgeon.require-change-boundary-test
  {:lane :battery}
  (:require
   [clj-surgeon.core :as core]
   [clj-surgeon.mcp-extraction :as kernel]
   [clj-surgeon.mcp-require-change :as tool]
   [clj-surgeon.require-change :as change]
   [clj-surgeon.require-change-io :as boundary]
   [clj-surgeon.synchronous-verification :as proof]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def original "(ns a\n (:require\n  [a.lib :as json]\n  ;; keep with z\n  [z.lib :as z]))\n(def x :untouched)\n")
(defn with-workspace [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "require-change-test-" (make-array java.nio.file.attribute.FileAttribute 0)))
        request {:workspace_root (str root) :add {:lib "m.target" :alias_policy ["json" "mjson"]}
                 :files [{:file "a.clj"} {:file "b.clj"}] :expect {:files 2 :adds 2 :removes 0}
                 :verification {:profile "unit"}}
        config {:receipt-dir (str (io/file root "receipts"))
                :verification-profiles {"unit" {:commands [["/bin/true"]]}}}]
    (try
      (doseq [file ["a.clj" "b.clj"]] (spit (io/file root file) original))
      (f root request config)
      (finally (doseq [file (reverse (file-seq root))] (.delete file))))))
(defn unchanged? [root] (every? #(= original (slurp (io/file root %))) ["a.clj" "b.clj"]))
(defn expand-receipt [r]
  (if (:receipt_details_path r) (edn/read-string (slurp (:receipt_details_path r))) r))

;; @spec REQUIRE-CHANGE-001
;; @spec REQUIRE-CHANGE-012
(deftest standalone-mcp-commits-and-retains-undo
  (with-workspace
    (fn [root request config]
      (let [answer (promise)]
        (tool/handle config nil request (fn [text error? result] (deliver answer {:text text :isError error? :structuredContent result})))
        (let [r (expand-receipt (:structuredContent @answer))]
          (is (= "committed" (:state r)) (pr-str @answer))
          (is (:verification_complete r))
          (is (= {:files 2 :adds 2 :removes 0} (:counts r)))
          (is (= ["mjson" "mjson"] (mapv :alias (:decisions r))))
          (is (every? #(= 0 (:exit %)) (:checks r)))
          (is (<= (count (pr-str (:structuredContent @answer))) 4096))
          (is (:ok (kernel/undo! (edn/read-string (slurp (:undo_receipt r))))))
          (is (unchanged? root)))))))

;; @spec REQUIRE-CHANGE-011
(deftest failing-proof-rolls-back-all-files
  (with-workspace
    (fn [root request config]
      (let [r (boundary/execute! (assoc-in config [:verification-profiles "unit" :commands] [["/bin/false"]]) request)]
        (is (= "rolled-back" (:state r)) (pr-str r))
        (is (:restored r)) (is (not (:verification_complete r)))
        (is (unchanged? root))
        (is (= 1 (get-in r [:checks 0 :exit])))))))

;; @spec REQUIRE-CHANGE-011
(deftest foreign-proof-write-is-recovery-required
  (with-workspace
    (fn [root request config]
      (with-redefs [proof/run-proof! (fn [& _]
                                       (spit (io/file root "a.clj") "foreign bytes")
                                       {:ok true :process_evidence [{:command ["/bin/true"] :finished? true :exit 0 :elapsed_ms 0}]})]
        (let [r (boundary/execute! config request)]
          (is (= "recovery-required" (:state r)))
          (is (false? (:verification_complete r)))
          (is (= "foreign bytes" (slurp (io/file root "a.clj"))))
          (is (string? (:undo_receipt r))))))))

;; @spec REQUIRE-CHANGE-004
;; @spec REQUIRE-CHANGE-008
;; @spec REQUIRE-CHANGE-009
;; @spec REQUIRE-CHANGE-010
(deftest atomic-prewrite-refusals
  (with-workspace
    (fn [root request config]
      (doseq [r [(assoc-in request [:expect :adds] 3)
                 (assoc-in request [:files 1 :source_hash] (apply str (repeat 64 "0")))
                 (assoc-in request [:add :alias_policy] ["json"])
                 (assoc-in request [:files 1 :file] "../outside.clj")
                 (assoc-in request [:files 1 :file] "a.clj")]]
        (let [result (boundary/execute! config r)]
          (is (= "refused" (:state result))) (is (:source_unchanged result))
          (is (false? (:mutation_attempted result))) (is (unchanged? root))))
      (java.nio.file.Files/createSymbolicLink (.toPath (io/file root "link.clj")) (.toPath (io/file root "a.clj"))
        (make-array java.nio.file.attribute.FileAttribute 0))
      (is (= "unconfined-source" (:error_type (boundary/execute! config (assoc-in request [:files 1 :file] "link.clj")))))
      (is (unchanged? root)))))

;; @spec REQUIRE-CHANGE-006
(deftest poisoned-candidate-cannot-publish
  (with-workspace
    (fn [root request config]
      (let [compile change/compile-change]
        (with-redefs [change/compile-change (fn [r sources]
                                              (update-in (compile r sources) [:future-sources "a.clj"]
                                                         str/replace ";; keep with z" ";; lost attachment"))]
          (let [r (boundary/execute! config request)]
            (is (= "protected-byte-change" (:error_type r)))
            (is (unchanged? root))))))))

;; @spec REQUIRE-CHANGE-013
(deftest preview-does-not-publish-or-prove
  (with-workspace
    (fn [root request _config]
      (with-redefs [kernel/commit! (fn [& _] (throw (Exception. "must not publish")))
                    proof/run-proof! (fn [& _] (throw (Exception. "must not prove")))]
        (let [r (boundary/execute! (assoc request :plan_only true))]
          (is (= "planned" (:state r))) (is (:read_complete r))
          (is (false? (:verification_complete r))) (is (unchanged? root)))))))

;; @spec REQUIRE-CHANGE-001
(deftest closed-schema-and-cli-dispatch
  (doseq [r [{} {:add {:lib "m" :alias_policy []}}]]
    (is (seq (change/validate-request r))))
  (with-workspace
    (fn [root request _config]
      (doseq [r [(assoc request :symbol_migration {}) (assoc-in request [:add :as] "mjson")
                 (assoc request :files []) (assoc-in request [:expect :bogus] 0)
                 (assoc-in request [:layout :order] "sort-everything")]]
        (is (= "invalid-request" (:error_type (boundary/execute! r)))))
      (let [request-file (io/file root "request.edn")]
        (spit request-file (pr-str request))
        (is (= "planned" (:state (boundary/cli! {:request-file (str request-file) :plan-only true})))))
      (is (= :require-change! (core/resolve-op "require-change!")))
      (let [help (core/format-global-help core/ops-registry)]
        (is (str/includes? help "require-change!")))
      (is (unchanged? root)))))

;; @spec REQUIRE-CHANGE-014
(deftest independent-oracle-rejects-protected-byte-mutants
  (let [r (proof/run-proof! (System/getProperty "user.dir") "independent-oracle"
            {:commands [["python3" "-B" "-m" "unittest" "discover" "-s" "test/oracles" "-p" "test_require_change_oracle.py"]]})]
    (is (:ok r) (pr-str r))))

;; @spec REQUIRE-CHANGE-010
(deftest capture-rejects-oversize-and-invalid-utf8
  (with-workspace
    (fn [root request config]
      (with-redefs [change/max-file-bytes 5]
        (is (= "file-byte-bound" (:error_type (boundary/execute! config request))))
        (is (unchanged? root)))
      (java.nio.file.Files/write (.toPath (io/file root "b.clj")) (byte-array [(unchecked-byte 255)])
        (make-array java.nio.file.OpenOption 0))
      (is (= "invalid-utf8" (:error_type (boundary/execute! config request))))
      (is (= original (slurp (io/file root "a.clj")))))))

;; @spec REQUIRE-CHANGE-001
(deftest public-cli-request-file-edn-and-exit
  (with-workspace
    (fn [root request config]
      (let [request-file (io/file root "request.edn")
            launch (fn [& extra]
                     (let [r (proof/run-proof! (System/getProperty "user.dir") "cli-boundary"
                               {:commands [(into ["env" (str "TMPDIR=" root) "bb" "-m" "clj-surgeon.core" ":op" ":require-change!"
                                                  ":request-file" (str request-file)] extra)]})
                           process (first (:process_evidence r))]
                       [(:exit process) (edn/read-string (:output process))]))]
        (spit request-file (pr-str request))
        (spit (io/file root ".clj-surgeon.edn") (pr-str (select-keys config [:verification-profiles])))
        (let [[exit result] (launch ":plan-only" "true")]
          (is (= 0 exit)) (is (= "planned" (:state result))) (is (unchanged? root)))
        (spit request-file (pr-str (assoc-in request [:expect :adds] 3)))
        (let [[exit result] (launch)]
          (is (pos? exit)) (is (= "refused" (:state result))) (is (unchanged? root)))
        (spit request-file (pr-str request))
        (let [[exit result] (launch)]
          (is (= 0 exit)) (is (= "committed" (:state result)))
          (is (:verification_complete result))
          (is (:ok (kernel/undo! (edn/read-string (slurp (:undo_receipt result))))))
          (is (unchanged? root)))))))

;; @spec REQUIRE-CHANGE-001
(deftest help-makes-preview-pinning-and-proof-review-concrete
  (let [help (core/format-op-help :require-change! (get core/ops-registry :require-change!))]
    (doseq [phrase ["workspace-relative .clj" "first policy alias" "comment attachment"
                    ":source_hash (SHA-256" ":result_hash" "no committed-but-pending mode"
                    ":receipt_details_path" ":undo-extract!"]]
      (is (str/includes? help phrase) phrase))))

;; @spec REQUIRE-CHANGE-012
(deftest bounded-receipt-retains-complete-file-decisions-and-proof
  (with-workspace
    (fn [root request config]
      (let [files (mapv #(str "long_explicit_namespace_" % ".clj") (range 12))
            request (assoc request :files (mapv #(hash-map :file %) files)
                                   :expect {:files 12 :adds 12 :removes 0})]
        (doseq [file files] (spit (io/file root file) original))
        (let [r (boundary/execute! config request)
              full (expand-receipt r)]
          (is (= "committed" (:state r)))
          (is (:details_elided r))
          (is (<= (count (.getBytes (pr-str r) "UTF-8")) 4096))
          (is (= 12 (count (:decisions full))))
          (is (= (set files) (set (:files_touched full))))
          (is (every? #(and (= 64 (count (:source_hash %))) (= 64 (count (:result_hash %)))) (:decisions full)))
          (is (every? #(= 0 (:exit %)) (:checks full)))
          (is (:ok (kernel/undo! (edn/read-string (slurp (:undo_receipt r))))))
          (is (every? #(= original (slurp (io/file root %))) files)))))))
