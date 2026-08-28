(ns clj-surgeon.operation-algebra-test
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.operation-algebra :as algebra]
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

;; @spec OP-ALG-CATALOG-001, OP-ALG-CONTEXT-001, OP-ALG-CONTEXT-002,
;; @spec OP-ALG-EFFECT-001, OP-ALG-EFFECT-002, OP-ALG-EFFECT-004
(deftest derives-capabilities-only-from-catalog-lifecycle-and-trusted-profile
  (is (= {:ok true :capabilities #{:source-read}}
         (algebra/derive-capabilities algebra/change-entry preview-context)))
  (is (= {:ok true
          :capabilities #{:source-read
                          :source-write
                          :receipt-stage
                          :receipt-publish
                          :rollback}}
         (algebra/derive-capabilities algebra/change-entry commit-context)))
  (testing "presentation metadata cannot grant authority"
    (is (= (algebra/derive-capabilities algebra/change-entry commit-context)
           (algebra/derive-capabilities
             (assoc algebra/change-entry :category :read)
             commit-context))))
  (testing "catalog maximum can only narrow authority"
    (is (= {:ok true :capabilities #{:source-read}}
           (algebra/derive-capabilities
             (assoc algebra/change-entry :maximum-effects #{:source-read})
             commit-context))))
  (doseq [context [(assoc preview-context :operation :change!)
                   (assoc preview-context :operation-version 2)
                   (assoc preview-context :policy :caller-selected)
                   (assoc preview-context :lifecycle :unknown)
                   (assoc preview-context :effects #{:source-write})]]
    (is (= :invalid-operation-context
           (:error-type
             (algebra/derive-capabilities algebra/change-entry context))))))

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
                    :effects {:observed [:source-write :receipt-publish]}}]]
    (is (= :invalid-operation-outcome
           (:error-type (algebra/validate-outcome outcome))))))

;; @spec OP-ALG-COMPILE-001, OP-ALG-PREVIEW-001, OP-ALG-OUTCOME-003
(deftest change-preview-compiles-once-without-copying-transaction-state
  (let [calls (atom 0)
        compiler (fn [source-map spec]
                   (swap! calls inc)
                   (transaction/compile-transaction source-map spec))
        result (algebra/compile-change
                 preview-context compiler sources transaction-spec)]
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
