(ns clj-surgeon.operation-algebra
  "Transport-neutral operation identity, effect authority, and terminal facts.

   This namespace owns no source, receipt, process, or transport effect. The
   existing intent transaction remains the compiler and mutation authority."
  (:require
   [clojure.set :as set]))

(def ^:private context-keys
  #{:operation :operation-version :entrance :policy :lifecycle})

(def ^:private change-contract
  {:operation :change
   :operation-version 1
   :lifecycles #{:preview :commit}
   :maximum-effects #{:source-read
                      :source-write
                      :receipt-stage
                      :receipt-publish
                      :rollback}
   :lifecycle-effects
   {:preview #{:source-read}
    :commit #{:source-read
              :source-write
              :receipt-stage
              :receipt-publish
              :rollback}}
   :trusted-profiles
   {[:cli :cli-legacy] #{:source-read
                         :source-write
                         :receipt-stage
                         :receipt-publish
                         :rollback}
    [:mcp :mcp-strict] #{:source-read
                         :source-write
                         :receipt-stage
                         :receipt-publish
                         :formatter-launch
                         :verifier-launch
                         :rollback}}})

;; @spec OP-ALG-CATALOG-001, OP-ALG-COMPILE-001, OP-ALG-EFFECT-005
(defn change-entry
  "Bind the pure change contract to its one injected compiler."
  [compiler]
  (assoc change-contract :compiler compiler))

(defn- invalid-context
  [message data]
  (merge {:error message
          :error-type :invalid-operation-context}
         data))

;; @spec OP-ALG-EFFECT-001, OP-ALG-EFFECT-002, OP-ALG-EFFECT-004,
;; @spec OP-ALG-EFFECT-005
(defn derive-capabilities
  "Derive effects from catalog, lifecycle, and a trusted adapter context.
   Request data is never accepted as context."
  [entry context]
  (let [unknown (set/difference (set (keys context)) context-keys)
        profile-key [(:entrance context) (:policy context)]
        profile-effects (get (:trusted-profiles entry) profile-key)
        lifecycle-effects (get (:lifecycle-effects entry)
                               (:lifecycle context))]
    (cond
      (seq unknown)
      (invalid-context "Operation context contains unknown authority fields"
                       {:unknown (vec (sort unknown))})

      (not= (:operation entry) (:operation context))
      (invalid-context "Operation context names an unsupported operation"
                       {:operation (:operation context)})

      (not= (:operation-version entry) (:operation-version context))
      (invalid-context "Operation context names an unsupported version"
                       {:operation-version (:operation-version context)})

      (nil? profile-effects)
      (invalid-context "Operation context names an untrusted entrance profile"
                       {:entrance (:entrance context)
                        :policy (:policy context)})

      (nil? lifecycle-effects)
      (invalid-context "Operation context names an unsupported lifecycle"
                       {:lifecycle (:lifecycle context)})

      :else
      {:ok true
       :capabilities (set/intersection
                       (:maximum-effects entry)
                       lifecycle-effects
                       profile-effects)})))

;; @spec OP-ALG-EFFECT-001, OP-ALG-EFFECT-003
(defn authorize-effects
  "Refuse before effects when a runtime action is absent from the computed set."
  [capabilities required-effects]
  (let [missing (set/difference required-effects capabilities)]
    (if (seq missing)
      {:error "Operation context does not authorize required effects"
       :error-type :effect-capability-denied
       :missing-effects (vec (sort missing))}
      {:ok true})))

(defn- invalid-outcome
  [violations]
  {:error "Canonical operation outcome violates the terminal-state law"
   :error-type :invalid-operation-outcome
   :violations violations})

(defn validate-outcome
  "Validate the synchronous slice-1 canonical terminal-state algebra."
  [{:keys [status phase source-state files receipt effects safe-to-retry]
    :as outcome}]
  (let [observed (set (:observed effects))
        terminal [status phase source-state]
        legal-terminal?
        (or (= [:ok :compile :unchanged] terminal)
            (and (= :refused status)
                 (= :unchanged source-state)
                 (contains? #{:select :snapshot :compile :receipt-stage}
                            phase))
            (= [:ok :receipt-publish :committed] terminal)
            (= [:failed :rollback :restored] terminal)
            (and (= :unverified status)
                 (= :unknown source-state)
                 (contains? #{:commit :read-back :receipt-publish :rollback}
                            phase)))
        forbidden-keys
        (set/intersection
          (set (keys outcome))
          #{:original-sources :future-sources :stdout :stderr :exit-status
            :json :callback-state :human-summary})
        wrote? (contains? observed :source-write)
        published? (contains? observed :receipt-publish)
        result-proved?
        (and (seq files) (every? :result-hash files))
        original-proved?
        (and (seq files)
             (every? #(or (:original-hash %) (:absent-before %)) files))
        publication-count (:publication-count receipt)
        violations
        (cond-> []
          (not legal-terminal?)
          (conj :illegal-terminal-state)

          (seq forbidden-keys)
          (conj :forbidden-outcome-fields)

          (and (= :unchanged source-state) wrote?)
          (conj :unchanged-after-write)

          (and (= :committed source-state)
               (not (and result-proved?
                         (= true (:published receipt))
                         (= 1 publication-count)
                         wrote?
                         published?)))
          (conj :committed-without-proof)

          (and (= :restored source-state)
               (not (and original-proved?
                         wrote?
                         (contains? observed :rollback))))
          (conj :restored-without-proof)

          (and (= :unverified status) safe-to-retry)
          (conj :unverified-safe-retry)

          (and publication-count (not= 1 publication-count))
          (conj :invalid-publication-count)

          (and (= :refused status)
               (or wrote? published? (= true (:published receipt))))
          (conj :refusal-after-effect))]
    (if (seq violations)
      (invalid-outcome violations)
      {:ok true :outcome outcome})))

(defn classify-change-terminal
  ;; @spec OP-ALG-COMMIT-003, OP-ALG-COMMIT-004, OP-ALG-OUTCOME-001,
  ;; @spec OP-ALG-OUTCOME-002, OP-ALG-REFUSE-001, OP-ALG-STALE-001
  "Convert one observed CLI change terminal into validated canonical facts."
  [{:keys [point legacy-result capabilities compiled-facts receipt-facts
           observed-effects]}]
  (let [rolled-back? (true? (:rolled-back legacy-result))
        error-type (:error-type legacy-result)
        [status phase source-state]
        (case point
          (:compile :authority) [:refused :compile :unchanged]
          :receipt-stage [:refused :receipt-stage :unchanged]
          :commit (cond
                    (= :source-hash-mismatch error-type)
                    [:refused :snapshot :unchanged]

                    rolled-back?
                    [:failed :rollback :restored]

                    :else
                    [:unverified :rollback :unknown])
          :receipt-publish (if rolled-back?
                             [:failed :rollback :restored]
                             [:unverified :rollback :unknown])
          :success [:ok :receipt-publish :committed]
          [:unverified :rollback :unknown])
        source-files (:files compiled-facts)
        files (case source-state
                :committed
                (mapv #(select-keys % [:file :result-hash]) source-files)

                :restored
                ;; @spec MCP-OP-EDIT-031
                ;; A file the transaction created has no original hash. Absence
                ;; is its original state, and restoring it means deleting it.
                (mapv (fn [{:keys [file source-hash absent-before]}]
                        (if absent-before
                          {:file file :absent-before true}
                          {:file file :original-hash source-hash}))
                      source-files)

                nil)
        receipt (when (= :committed source-state)
                  (merge {:published true :publication-count 1}
                         receipt-facts))
        outcome
        (cond-> {:operation :change
                 :operation-version 1
                 :status status
                 :phase phase
                 :source-state source-state
                 :effects {:declared capabilities
                           :observed (vec observed-effects)}}
          (:counts compiled-facts)
          (assoc :counts (:counts compiled-facts))

          (seq files)
          (assoc :files files)

          receipt
          (assoc :receipt receipt))]
    (validate-outcome outcome)))

(defn observe-change-terminal
  "Validate one canonical terminal and return the identical legacy result."
  [observation legacy-result]
  (let [classification
        (classify-change-terminal
          (assoc observation :legacy-result legacy-result))]
    (when (:error classification)
      (throw
        (ex-info (:error classification)
                 (dissoc classification :error))))
    legacy-result))

;; @spec OP-ALG-COMPILE-001, OP-ALG-PREVIEW-001, OP-ALG-OUTCOME-003
(defn compile-change
  "Compile one change through its catalog compiler and return a
   transport-neutral outcome beside the unchanged compiled transaction."
  [entry context sources spec]
  (let [authority (derive-capabilities entry context)]
    (if (:error authority)
      authority
      (let [compiled ((:compiler entry) sources spec)
            outcome {:status (if (:error compiled) :refused :ok)
                     :phase :compile
                     :source-state :unchanged
                     :effects {:observed [:source-read]}}
            validation (validate-outcome outcome)]
        (if (:error validation)
          validation
          {:ok true
           :context context
           :capabilities (:capabilities authority)
           :compiled compiled
           :outcome outcome})))))
