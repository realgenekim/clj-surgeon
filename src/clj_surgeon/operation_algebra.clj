(ns clj-surgeon.operation-algebra
  "Transport-neutral operation identity, effect authority, and terminal facts.

   This namespace owns no source, receipt, process, or transport effect. The
   existing intent transaction remains the compiler and mutation authority."
  (:require
   [clojure.set :as set]))

(def ^:private context-keys
  #{:operation :operation-version :entrance :policy :lifecycle})

(def ^:private write-effects
  #{:source-write :receipt-stage :receipt-publish})

(def change-entry
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
                         :rollback}}
   :compiler (fn [sources spec]
               ((requiring-resolve
                  'clj-surgeon.intent-transaction/compile-transaction)
                sources spec))})

(defn- invalid-context
  [message data]
  (merge {:error message
          :error-type :invalid-operation-context}
         data))

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
        wrote? (contains? observed :source-write)
        published? (contains? observed :receipt-publish)
        result-proved? (boolean (some :result-hash files))
        original-proved? (boolean (some :original-hash files))
        publication-count (:publication-count receipt)
        violations
        (cond-> []
          (not (contains? #{:ok :refused :failed :unverified} status))
          (conj :invalid-status)

          (not (contains? #{:select :snapshot :compile :receipt-stage
                            :commit :read-back :receipt-publish :rollback}
                          phase))
          (conj :invalid-phase)

          (not (contains? #{:unchanged :committed :restored :unknown}
                          source-state))
          (conj :invalid-source-state)

          (and (= :unchanged source-state) wrote?)
          (conj :unchanged-after-write)

          (and (= :committed source-state)
               (not (and (= :ok status)
                         (= :receipt-publish phase)
                         result-proved?
                         (= true (:published receipt))
                         (= 1 publication-count)
                         wrote?
                         published?)))
          (conj :committed-without-proof)

          (and (= :restored source-state)
               (not (and (= :rollback phase)
                         original-proved?
                         wrote?
                         (contains? observed :rollback))))
          (conj :restored-without-proof)

          (and (= :unverified status) safe-to-retry)
          (conj :unverified-safe-retry)

          (and publication-count (not= 1 publication-count))
          (conj :invalid-publication-count)

          (and (= :ok status)
               (= :compile phase)
               (not= :unchanged source-state))
          (conj :preview-source-state)

          (and (= :refused status)
               (not= :unchanged source-state))
          (conj :refusal-source-state))]
    (if (seq violations)
      (invalid-outcome violations)
      {:ok true :outcome outcome})))

(defn compile-change
  "Compile one change through an injected or catalog compiler and return a
   transport-neutral outcome beside the unchanged compiled transaction."
  ([context sources spec]
   (compile-change context (:compiler change-entry) sources spec))
  ([context compiler sources spec]
   (let [authority (derive-capabilities change-entry context)]
     (if (:error authority)
       authority
       (let [compiled (compiler sources spec)
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
            :outcome outcome}))))))

(defn plan-change
  "CLI preview adapter over the existing authoritative implementation."
  [opts]
  ((requiring-resolve 'clj-surgeon.intent-transaction/plan-change) opts))

(defn execute-change!
  "CLI commit adapter over the existing authoritative implementation."
  [opts]
  ((requiring-resolve 'clj-surgeon.intent-transaction/execute-change!) opts))
