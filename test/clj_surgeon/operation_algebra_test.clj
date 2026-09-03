(ns clj-surgeon.operation-algebra-test
  (:require
   [clj-surgeon.core :as core]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.operation-algebra :as algebra]
   [clj-surgeon.outline :as outline]
   [clojure.java.io :as io]
   [clojure.string :as str]
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

(defn- required-namespaces
  [source]
  (let [ns-form (read-string source)]
    (->> ns-form
         (filter seq?)
         (filter #(= :require (first %)))
         (mapcat rest)
         (map first)
         set)))

(defn- top-level-form-index
  [file]
  (into {}
        (map (juxt :name :source))
        (outline/top-level-form-records file (slurp file))))

(defn- top-level-form-source
  [file owner]
  (or (get (top-level-form-index file) owner)
      (throw (ex-info "Architecture owner not found"
                      {:file file :owner owner}))))

(defn- architecture-references
  [source]
  (let [form (read-string source)
        all-symbols (->> (tree-seq coll? seq form)
                         (filter symbol?))
        invoked-heads (->> (tree-seq coll? seq form)
                           (keep #(when (seq? %) (first %)))
                           (filter symbol?))
        ;; @spec MCP-OP-ALIAS-056
        ;; the receipt staging write moved from `spit` on a `createTempFile`
        ;; name to an explicit CREATE_NEW open; the inventory has to KNOW that
        ;; call, or this oracle goes blind to the one write it exists to bound
        raw-effect-symbols #{'slurp 'spit 'Files/move 'Files/newOutputStream
                             'java.io.File/createTempFile 'System/exit}
        effect-head? #(re-find
                        #"(?i)(^|/)(write|publish|stage|rollback|recover|format|verif|process|launch|mcp|json|exit|move|createTempFile|newOutputStream|slurp|spit)(!|$|-)|\.delete|\.write"
                        (str %))]
    (set (concat (filter #(str/ends-with? (str %) "!") all-symbols)
                 (filter raw-effect-symbols all-symbols)
                 (filter effect-head? invoked-heads)))))

;; @spec MCP-OP-ALIAS-056
(defn- callees-in-file
  "Top-level forms of the same file this form invokes directly."
  [index owner source]
  (->> (read-string source)
       (tree-seq coll? seq)
       (keep #(when (seq? %) (first %)))
       (filter symbol?)
       (filter index)
       (remove #{owner})
       distinct))

;; @spec MCP-OP-ALIAS-056
(defn- architecture-references-one-frame-deeper
  "This form's effect symbols, plus those of the helpers it calls in this file.

  The inventory used to read ONE top-level form textually and follow no calls,
  which left it blind exactly one frame down: a `spit` added to the private
  `receipt-source` — a helper `stage-receipt!` calls on every receipt — ran on
  every run and this oracle stayed green. A bounded write is not bounded by
  reading only the form that names it. One frame is where the boundary is
  drawn: it covers the helpers the bounded forms actually delegate to, and it
  stops before the whole file collapses into one set, which would make the
  oracle assert nothing. Effects reachable two frames down are still outside
  it, and that is the stated limit rather than an oversight."
  [index owner]
  (let [source (get index owner)]
    (into (architecture-references source)
          (mapcat #(architecture-references (get index %)))
          (callees-in-file index owner source))))

(defn- runtime-architecture-inventory
  []
  (let [file "src/clj_surgeon/intent_transaction.clj"
        index (top-level-form-index file)
        refs #(architecture-references-one-frame-deeper index %)]
    {:preview (refs 'plan-change)
     :commit-adapters
     (into #{}
           (mapcat refs ['execute-change! 'execute-mcp-change!]))
     :commit-entry (refs 'execute-change-with-context!)
     :commit-runtime (refs 'commit-compiled!)
     :receipt-stage (refs 'stage-receipt!)
     :receipt-publish (refs 'publish-staged-receipt!)
     :rollback (refs 'recovery-result)}))

(deftest operation-algebra-architecture-is-transport-neutral-and-bounded
  ;; @spec OP-ALG-CATALOG-001, OP-ALG-COMPILE-001, OP-ALG-EFFECT-001,
  ;; @spec OP-ALG-EFFECT-002, OP-ALG-EFFECT-003, OP-ALG-EFFECT-004,
  ;; @spec OP-ALG-EFFECT-005
  (let [source (slurp "src/clj_surgeon/operation_algebra.clj")
        compiler transaction/compile-transaction
        catalog-entry (algebra/change-entry compiler)
        preview-entry (select-keys (get core/ops-registry :change)
                                   [:handler :canonical-operation
                                    :lifecycle :category])
        commit-entry (select-keys (get core/ops-registry :change!)
                                  [:handler :canonical-operation
                                   :lifecycle :category])]
    (testing "the algebra depends only on pure set operations"
      (is (= #{'clojure.set} (required-namespaces source)))
      (doseq [forbidden ["requiring-resolve"
                         "ns-resolve"
                         "clj-surgeon.mcp"
                         "cheshire/"
                         "clojure.data.json"
                         "json/"
                         "jsonista/"
                         "System/exit"]]
        (is (not (str/includes? source forbidden)) forbidden))
      (is (str/includes? source "@spec OP-ALG-EFFECT-005")))
    (testing "the compiler is injected without an alternate resolver"
      (is (identical? compiler (:compiler catalog-entry))))
    (testing "the public registry assigns lifecycle but category grants nothing"
      (is (= {:handler transaction/plan-change
              :canonical-operation :change
              :lifecycle :preview
              :category :write}
             preview-entry))
      (is (= {:handler transaction/execute-change!
              :canonical-operation :change
              :lifecycle :commit
              :category :write}
             commit-entry))
      (is (= (algebra/derive-capabilities catalog-entry commit-context)
             (algebra/derive-capabilities
               (assoc catalog-entry :category :read)
               commit-context))))
    (testing "the bounded entry names only the transaction effect inventory"
      (is (= #{:source-read
               :source-write
               :receipt-stage
               :receipt-publish
               :rollback}
             (:maximum-effects catalog-entry)))
      (is (= {:preview #{:source-read}
              :commit #{:source-read
                        :source-write
                        :receipt-stage
                        :receipt-publish
                        :rollback}}
             (:lifecycle-effects catalog-entry)))
      (is (= {:error "Operation context does not authorize required effects"
              :error-type :effect-capability-denied
              :missing-effects [:formatter-launch
                                :process-exit
                                :verifier-launch]}
             (algebra/authorize-effects
               (:capabilities
                 (algebra/derive-capabilities catalog-entry commit-context))
               #{:formatter-launch :process-exit :verifier-launch})))
      ;; @spec MCP-OP-ALIAS-056
      ;; The inventory reads each bounded form AND the same-file helpers it
      ;; calls, one frame down. Reading only the named form left this oracle
      ;; blind exactly where the write lives: a `spit` added to the private
      ;; `receipt-source`, which `stage-receipt!` calls on every receipt, ran
      ;; on every run and this test stayed green. The sets below are larger
      ;; for it, and that is the point — every symbol here is a call the
      ;; bounded entry can reach without another named form standing between.
      (is (= {:preview #{'refuse!
                         'slurp
                         'validate-aggregate-expectation!
                         'validate-change-aggregate-expectation!
                         'validate-changes!
                         'validate-create-files!
                         'validate-intent!
                         'validate-spec!}
              :commit-adapters #{'.delete
                                 'assert-receipt-does-not-alias-source!
                                 'commit-compiled!
                                 'execute-change!
                                 'execute-change-with-context!
                                 'execute-mcp-change!
                                 'prepare-compiled!
                                 'publish-staged-receipt!
                                 'refuse!
                                 'slurp
                                 'stage-receipt!
                                 'validate-receipt!}
              :commit-entry #{'.delete
                              '.write
                              'Files/move
                              'Files/newOutputStream
                              'assert-file-hash!
                              'assert-receipt-does-not-alias-source!
                              'commit-compiled!
                              'create-directory!
                              'create-source!
                              'default-create-directory!
                              'default-delete-file!
                              'delete-file!
                              'execute-change-with-context!
                              'execute-creations!
                              'execute-deletions!
                              'execute-writes!
                              'file-ops/atomic-create!
                              'file-ops/atomic-write!
                              'invalid-receipt!
                              'prepare-compiled!
                              'publish-staged-receipt!
                              'recover-transaction!
                              'refuse!
                              'rollback-creations!
                              'rollback-deletions!
                              'slurp
                              'stage-receipt!
                              'validate-complete-source!
                              'validate-receipt!
                              'validate-spec!
                              'write-source!}
              :commit-runtime #{'assert-file-hash!
                                'commit-compiled!
                                'execute-writes!
                                ;; @spec MCP-OP-EDIT-031
                                'execute-creations!
                                'execute-deletions!
                                'create-directory!
                                'delete-file!
                                'default-create-directory!
                                'default-delete-file!
                                'rollback-creations!
                                'rollback-deletions!
                                'recover-transaction!
                                'refuse!
                                'slurp
                                'swap!
                                ;; @spec MCP-OP-EDIT-035
                                'create-source!
                                'write-source!
                                'read-source!
                                'file-ops/atomic-create!
                                'file-ops/atomic-write!
                                'file-ops/revalidate-create-target!}
              ;; @spec MCP-OP-ALIAS-056
              ;; the staged receipt is opened CREATE_NEW rather than spat over
              ;; a createTempFile name, so an open that would follow a link
              ;; somebody else installed fails instead
              :receipt-stage #{'.delete
                               '.write
                               'refuse!
                               'slurp
                               'invalid-receipt!
                               'stage-receipt!
                               'validate-receipt!
                               'Files/newOutputStream}
              :receipt-publish #{'publish-staged-receipt! 'Files/move}
              :rollback #{'read-source! 'refuse! 'write-source!}}
             (runtime-architecture-inventory))))))

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
  ;; @spec OP-ALG-COMMIT-003, OP-ALG-RECEIPT-003, OP-ALG-REFUSE-001,
  ;; @spec OP-ALG-STALE-001
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
  ;; @spec OP-ALG-CLI-001, OP-ALG-DECODE-001
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
(deftest trusted-adapters-select-context-without-request-authority
  ;; @spec OP-ALG-CONTEXT-001, OP-ALG-CONTEXT-002, OP-ALG-MCP-001
  (let [workspace (temp-workspace)
        source-file (io/file workspace "app.clj")
        source-path (.getPath source-file)
        contexts (atom [])
        original-compile algebra/compile-change
        entrances [[transaction/execute-change! :cli :cli-legacy]
                   [transaction/execute-mcp-change! :mcp :mcp-strict]]]
    (try
      (spit source-file "(ns app)\n(defn title [] (old-title))\n")
      (doseq [[execute! entrance policy] entrances]
        (let [receipt-file
              (io/file workspace (str (name entrance) "-receipt.edn"))
              result
              (with-redefs [algebra/compile-change
                            (fn [entry context sources spec]
                              (swap! contexts conj context)
                              (original-compile entry context sources spec))]
                (execute! {:spec (file-change-spec source-path)
                           :receipt-out (.getPath receipt-file)}))]
          (is (:ok result))
          (is (:ok (transaction/execute-undo!
                     {:receipt (.getPath receipt-file)})))
          (is (= entrance (:entrance (peek @contexts))))
          (is (= policy (:policy (peek @contexts))))))
      (is (= [{:operation :change
               :operation-version 1
               :entrance :cli
               :policy :cli-legacy
               :lifecycle :commit}
              {:operation :change
               :operation-version 1
               :entrance :mcp
               :policy :mcp-strict
               :lifecycle :commit}]
             @contexts))
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
