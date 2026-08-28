(ns clj-surgeon.operation-algebra-test
  (:require
   [clj-surgeon.core :as core]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.operation-algebra :as algebra]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(def ^:private preview-context
  {:operation :change
   :operation-version 1
   :entrance :cli
   :policy :cli-legacy
   :lifecycle :preview})

(def ^:private commit-context
  (assoc preview-context :lifecycle :commit))

(def ^:private sources
  {"src/app.clj" "(ns app)\n(defn title [] (old-title))\n"})

(def ^:private transaction-spec
  {:intents [{:files ["src/app.clj"]
              :from "(old-title)"
              :to "(new-title)"
              :expect-count 1}]
   :expect {:intent-count 1
            :edit-count 1
            :changed-file-count 1}})

(defn- entry
  ([] (entry transaction/compile-transaction))
  ([compiler] (algebra/change-entry compiler)))

;; @spec OP-ALG-CATALOG-001, OP-ALG-CONTEXT-001, OP-ALG-CONTEXT-002,
;; @spec OP-ALG-EFFECT-001, OP-ALG-EFFECT-002, OP-ALG-EFFECT-004
(deftest derives-capabilities-only-from-catalog-lifecycle-and-trusted-profile
  (is (= {:ok true :capabilities #{:source-read}}
         (algebra/derive-capabilities (entry) preview-context)))
  (is (= {:ok true
          :capabilities #{:source-read
                          :source-write
                          :receipt-stage
                          :receipt-publish
                          :rollback}}
         (algebra/derive-capabilities (entry) commit-context)))
  (testing "presentation metadata cannot grant authority"
    (is (= (algebra/derive-capabilities (entry) commit-context)
           (algebra/derive-capabilities
             (assoc (entry) :category :read)
             commit-context))))
  (testing "catalog maximum can only narrow authority"
    (is (= {:ok true :capabilities #{:source-read}}
           (algebra/derive-capabilities
             (assoc (entry) :maximum-effects #{:source-read})
             commit-context))))
  (doseq [context [(assoc preview-context :operation :change!)
                   (assoc preview-context :operation-version 2)
                   (assoc preview-context :policy :caller-selected)
                   (assoc preview-context :lifecycle :unknown)
                   (assoc preview-context :effects #{:source-write})]]
    (is (= :invalid-operation-context
           (:error-type
             (algebra/derive-capabilities (entry) context))))))

;; @spec OP-ALG-OUTCOME-001, OP-ALG-OUTCOME-002, OP-ALG-COMMIT-004
(deftest accepts-only-legal-canonical-terminal-states
  (doseq [outcome [{:status :ok
                    :phase :compile
                    :source-state :unchanged
                    :effects {:observed [:source-read]}}
                   {:status :refused
                    :phase :compile
                    :source-state :unchanged
                    :effects {:observed [:source-read]}}
                   {:status :ok
                    :phase :receipt-publish
                    :source-state :committed
                    :files [{:file "src/app.clj"
                             :result-hash "result"}]
                    :receipt {:published true :publication-count 1}
                    :effects {:observed [:source-read
                                         :receipt-stage
                                         :source-write
                                         :receipt-publish]}}
                   {:status :failed
                    :phase :rollback
                    :source-state :restored
                    :files [{:file "src/app.clj"
                             :original-hash "original"}]
                    :effects {:observed [:source-write :rollback]}}
                   {:status :unverified
                    :phase :rollback
                    :source-state :unknown
                    :effects {:observed [:source-write :rollback]}}]]
    (is (= {:ok true :outcome outcome}
           (algebra/validate-outcome outcome))))
  (doseq [outcome [{:status :ok
                    :phase :receipt-publish
                    :source-state :committed
                    :effects {:observed [:source-write]}}
                   {:status :refused
                    :phase :compile
                    :source-state :unchanged
                    :effects {:observed [:source-write]}}
                   {:status :failed
                    :phase :rollback
                    :source-state :restored
                    :effects {:observed [:source-write :rollback]}}
                   {:status :unverified
                    :phase :rollback
                    :source-state :unknown
                    :safe-to-retry true
                    :effects {:observed [:source-write :rollback]}}
                   {:status :ok
                    :phase :receipt-publish
                    :source-state :committed
                    :files [{:file "src/app.clj" :result-hash "result"}]
                    :receipt {:published true :publication-count 2}
                    :effects {:observed [:source-write :receipt-publish]}}
                   {:status :failed
                    :phase :compile
                    :source-state :unchanged
                    :effects {:observed [:source-read]}}
                   {:status :ok
                    :phase :rollback
                    :source-state :restored
                    :files [{:file "src/app.clj" :original-hash "original"}]
                    :effects {:observed [:source-write :rollback]}}
                   {:status :refused
                    :phase :receipt-publish
                    :source-state :unchanged
                    :receipt {:published true :publication-count 1}
                    :effects {:observed [:receipt-publish]}}
                   {:status :ok
                    :phase :compile
                    :source-state :unchanged
                    :future-sources {"src/app.clj" "(ns app)"}
                    :stdout "transport data"
                    :effects {:observed [:source-read]}}
                   {:status :ok
                    :phase :receipt-publish
                    :source-state :committed
                    :files [{:file "src/a.clj" :result-hash "a"}
                            {:file "src/b.clj"}]
                    :receipt {:published true :publication-count 1}
                    :effects {:observed [:source-write :receipt-publish]}}]]
    (is (= :invalid-operation-outcome
           (:error-type (algebra/validate-outcome outcome))))))

(deftest classifies-every-cli-terminal-without-changing-the-legacy-result
  (let [capabilities #{:source-read :source-write :receipt-stage
                       :receipt-publish :rollback}
        compiled-facts {:counts {:changes 1 :edits 1 :files 1}
                        :files [{:file "src/app.clj"
                                 :source-hash "original"
                                 :result-hash "result"}]}
        receipt-facts {:path ".clj-surgeon-receipts/receipt.edn"}
        rows [{:point :compile
               :legacy-result {:error-type :invalid-change}
               :observed-effects [:source-read]
               :expected [:refused :compile :unchanged]}
              {:point :authority
               :legacy-result {:error-type :effect-capability-denied}
               :observed-effects [:source-read]
               :expected [:refused :compile :unchanged]}
              {:point :receipt-stage
               :legacy-result {:error-type :receipt-stage-failed}
               :observed-effects [:source-read :receipt-stage]
               :expected [:refused :receipt-stage :unchanged]}
              {:point :commit
               :legacy-result {:error-type :source-hash-mismatch}
               :observed-effects [:source-read :receipt-stage]
               :expected [:refused :snapshot :unchanged]}
              {:point :commit
               :legacy-result {:error-type :write-failed :rolled-back true}
               :observed-effects [:source-read :receipt-stage
                                  :source-write :rollback]
               :expected [:failed :rollback :restored]}
              {:point :commit
               :legacy-result {:error-type :write-failed}
               :observed-effects [:source-read :receipt-stage
                                  :source-write :rollback]
               :expected [:unverified :rollback :unknown]}
              {:point :receipt-publish
               :legacy-result {:error-type :publish-failed :rolled-back true}
               :observed-effects [:source-read :receipt-stage :source-write
                                  :receipt-publish :rollback]
               :expected [:failed :rollback :restored]}
              {:point :receipt-publish
               :legacy-result {:error-type :publish-failed}
               :observed-effects [:source-read :receipt-stage :source-write
                                  :receipt-publish :rollback]
               :expected [:unverified :rollback :unknown]}
              {:point :success
               :legacy-result {:ok true :receipt "legacy"}
               :observed-effects [:source-read :receipt-stage
                                  :source-write :receipt-publish]
               :expected [:ok :receipt-publish :committed]}]]
    (doseq [{:keys [point legacy-result observed-effects expected]} rows]
      (let [observation {:point point
                         :capabilities capabilities
                         :compiled-facts compiled-facts
                         :receipt-facts receipt-facts
                         :observed-effects observed-effects}
            classification
            (algebra/classify-change-terminal
              (assoc observation :legacy-result legacy-result))
            outcome (:outcome classification)]
        (is (= {:ok true :outcome outcome} classification))
        (is (= expected
               ((juxt :status :phase :source-state) outcome)))
        (is (= legacy-result
               (algebra/observe-change-terminal observation legacy-result)))
        (is (not-any? #(contains? outcome %)
                      [:original-sources :future-sources :stdout :stderr
                       :json :callback-state :human-summary]))
        (when (= :unknown (:source-state outcome))
          (is (nil? (:files outcome)))
          (is (not (true? (:safe-to-retry outcome)))))))))

;; @spec OP-ALG-COMPILE-001, OP-ALG-PREVIEW-001, OP-ALG-OUTCOME-003
(deftest change-preview-compiles-once-without-copying-transaction-state
  (let [calls (atom 0)
        compiler (fn [source-map spec]
                   (swap! calls inc)
                   (transaction/compile-transaction source-map spec))
        result (algebra/compile-change
                 (entry compiler) preview-context sources transaction-spec)]
    (is (= 1 @calls))
    (is (= (transaction/compile-transaction sources transaction-spec)
           (:compiled result)))
    (is (= {:status :ok
            :phase :compile
            :source-state :unchanged
            :effects {:observed [:source-read]}}
           (:outcome result)))
    (is (not (contains? (:outcome result) :original-sources)))
    (is (not (contains? (:outcome result) :future-sources)))))

(defn- temp-workspace
  []
  (.toFile
    (java.nio.file.Files/createTempDirectory
      "clj-surgeon-operation-algebra-"
      (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- file-change-spec
  [source-path]
  {:intents [{:files [source-path]
              :from "(old-title)"
              :to "(new-title)"
              :expect-count 1}]
   :expect {:intent-count 1
            :edit-count 1
            :changed-file-count 1}})

;; @spec OP-ALG-IDENTITY-001, OP-ALG-PREVIEW-002, OP-ALG-PARITY-001
(deftest cli-registry-routes-change-lifecycles-through-the-algebra
  (let [registry @(ns-resolve 'clj-surgeon.core 'ops-registry)]
    (is (= transaction/plan-change (get-in registry [:change :handler])))
    (is (= :change (get-in registry [:change :canonical-operation])))
    (is (= :preview (get-in registry [:change :lifecycle])))
    (is (= transaction/execute-change! (get-in registry [:change! :handler])))
    (is (= :change (get-in registry [:change! :canonical-operation])))
    (is (= :commit (get-in registry [:change! :lifecycle])))
    (is (= :write (get-in registry [:change :category])))
    (is (= :write (get-in registry [:change! :category])))))

;; @spec OP-ALG-COMPILE-001, OP-ALG-PREVIEW-001, OP-ALG-RUNTIME-001
(deftest public-preview-enters-the-algebra-once-and-performs-no-write
  (let [workspace (temp-workspace)
        source-file (io/file workspace "app.clj")
        source-path (.getPath source-file)
        original-source "(ns app)\n(defn title [] (old-title))\n"
        calls (atom 0)
        original-compile algebra/compile-change]
    (try
      (spit source-file original-source)
      (let [result
            (with-redefs [algebra/compile-change
                          (fn [& args]
                            (swap! calls inc)
                            (apply original-compile args))]
              (transaction/plan-change
                {:spec (file-change-spec source-path)}))]
        (is (= 1 @calls))
        (is (:ok result))
        (is (= :change (:operation result)))
        (is (= original-source (slurp source-file)))
        (is (not (.exists (io/file workspace "receipt.edn")))))
      (finally
        (doseq [file (reverse (file-seq workspace))]
          (io/delete-file file true))))))

;; @spec OP-ALG-COMMIT-001, OP-ALG-RECEIPT-001, OP-ALG-RECEIPT-002,
;; @spec OP-ALG-SHADOW-001, OP-ALG-RUNTIME-001
(deftest public-commit-has-one-runtime-path-and-existing-undo-restores-bytes
  (let [workspace (temp-workspace)
        source-file (io/file workspace "app.clj")
        receipt-file (io/file workspace "receipt.edn")
        source-path (.getPath source-file)
        original-source "(ns app)\n(defn title [] (old-title))\n"
        expected-source "(ns app)\n(defn title [] (new-title))\n"
        calls (atom {:compile 0 :commit 0 :stage 0 :publish 0})
        original-compile algebra/compile-change
        original-commit transaction/commit-compiled!
        stage-var (ns-resolve
                    'clj-surgeon.intent-transaction 'stage-receipt!)
        publish-var (ns-resolve
                      'clj-surgeon.intent-transaction
                      'publish-staged-receipt!)
        original-stage @stage-var
        original-publish @publish-var]
    (try
      (spit source-file original-source)
      (let [result
            (with-redefs-fn
              {#'algebra/compile-change
               (fn [& args]
                 (swap! calls update :compile inc)
                 (apply original-compile args))
               #'transaction/commit-compiled!
               (fn [& args]
                 (when (= 1 (count args))
                   (swap! calls update :commit inc))
                 (apply original-commit args))
               stage-var
               (fn [& args]
                 (swap! calls update :stage inc)
                 (apply original-stage args))
               publish-var
               (fn [& args]
                 (swap! calls update :publish inc)
                 (apply original-publish args))}
              #(transaction/execute-change!
                 {:spec (file-change-spec source-path)
                  :receipt-out (.getPath receipt-file)}))]
        (is (:ok result))
        (is (= {:compile 1 :commit 1 :stage 1 :publish 1} @calls))
        (is (= expected-source (slurp source-file)))
        (is (.exists receipt-file))
        (is (= (:receipt-hash result)
               (:receipt-hash (read-string (slurp receipt-file)))))
        (let [undo (transaction/execute-undo!
                     {:receipt (.getPath receipt-file)})]
          (is (:ok undo))
          (is (= original-source (slurp source-file)))))
      (finally
        (doseq [file (reverse (file-seq workspace))]
          (io/delete-file file true))))))
(deftest public-commit-validates-one-canonical-terminal-before-return
  (let [workspace (temp-workspace)
        source-file (io/file workspace "app.clj")
        receipt-file (io/file workspace "receipt.edn")
        source-path (.getPath source-file)
        calls (atom [])
        original-observer algebra/observe-change-terminal]
    (try
      (spit source-file "(ns app)\n(defn title [] (old-title))\n")
      (let [result
            (with-redefs [algebra/observe-change-terminal
                          (fn [observation legacy-result]
                            (swap! calls conj
                                   [(:point observation)
                                    (:status
                                      (:outcome
                                        (algebra/classify-change-terminal
                                          (assoc observation
                                                 :legacy-result
                                                 legacy-result))))])
                            (original-observer observation legacy-result))]
              (transaction/execute-change!
                {:spec (file-change-spec source-path)
                 :receipt-out (.getPath receipt-file)}))]
        (is (:ok result))
        (is (= [[:success :ok]] @calls)))
      (finally
        (doseq [file (reverse (file-seq workspace))]
          (io/delete-file file true))))))
(deftest public-commit-refuses-before-effects-when-catalog-denies-authority
  ;; @spec OP-ALG-EFFECT-001, OP-ALG-EFFECT-003, OP-ALG-COMMIT-002
  (let [workspace (temp-workspace)
        source-file (io/file workspace "app.clj")
        receipt-file (io/file workspace "receipt.edn")
        source-path (.getPath source-file)
        original-source "(ns app)\n(defn title [] (old-title))\n"
        original-entry algebra/change-entry]
    (try
      (spit source-file original-source)
      (let [result
            (with-redefs [algebra/change-entry
                          (fn [compiler]
                            (assoc (original-entry compiler)
                                   :maximum-effects #{:source-read}))]
              (transaction/execute-change!
                {:spec (file-change-spec source-path)
                 :receipt-out (.getPath receipt-file)}))]
        (is (= :effect-capability-denied (:error-type result)))
        (is (= original-source (slurp source-file)))
        (is (not (.exists receipt-file))))
      (finally
        (doseq [file (reverse (file-seq workspace))]
          (io/delete-file file true))))))
