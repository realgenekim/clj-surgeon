(ns substantiation-report
  "Pure validation and claims fold for substantiation telemetry."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-substantiation :as substantiation]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.time Duration Instant)))

;; @spec MCP-OP-SUBST-010
;; @spec MCP-OP-SUBST-011
;; @spec MCP-OP-SUBST-012
;; @spec MCP-OP-SUBST-013
;; @spec MCP-OP-SUBST-014
;; @spec MCP-OP-SUBST-015
;; @spec MCP-OP-SUBST-016
;; @spec MCP-OP-SUBST-017
;; @spec MCP-OP-SUBST-019

(def ^:private required-event-fields
  #{:schema :sequence :event_id :observed_at :phase :call_id
    :previous_event_sha256 :event_sha256 :transport :tool :operation
    :features})

(def ^:private allowed-event-fields
  (conj required-event-fields :request_shape :result_shape))

(def ^:private required-transport-fields
  #{:session_token :turn_token :key_id :client_name :client_version
    :caller_model :caller_model_source})

(def ^:private allowed-request-shape-fields
  #{:field_presence :subject_tokens :request_count :edit_count :change_count})

(def ^:private allowed-result-shape-fields
  #{:field_presence :subject_tokens :owner_token_count :location_row_count
    :duplicate_group_count :semantic_kinds :source_body_present
    :source_character_count :dependency_evidence_present
    :hash_evidence_present :location_cap_state :evidence_complete :ok
    :committed :verification_complete :source_unchanged :read_complete
    :error_type :result_count :elapsed_ms})

(def ^:private allowed-feature-fields
  #{:feature_id :stage :counts :dimensions})

(def ^:private allowed-count-fields
  #{:requests :descriptors :holes :available :returned :omitted :rows
    :continuations :candidates})

(def ^:private allowed-dimension-fields
  #{:eligible :skeleton_token :truncated :inert :query_token :complete})

(def ^:private public-tools
  #{"inspect_clojure" "edit_clojure" "apply_clojure_changes"
    "transform_clojure"})

(def ^:private allowed-request-fields
  #{"workspace_root" "operation" "requests" "file" "files" "owner" "forms"
    "within" "expect" "edits" "changes" "programs" "delete_owners" "basis"
    "site_ids" "decisions" "candidate_query_sha256" "preview" "verify"
    "receipt_out" "extraction"})

(def ^:private allowed-result-fields
  #{"ok" "operation" "elapsed_ms" "read_complete" "source_unchanged"
    "mutation_attempted" "committed" "verification_complete" "next_action"
    "error_type" "results" "prepared_request" "available_owners"
    "available_owners_omitted" "write_refusal_evidence" "changes" "edits"
    "files" "matches" "result_count" "receipt_hash" "read_back_hashes"})

(def ^:private allowed-operations
  #{"inspect_clojure" "edit_clojure" "apply_clojure_changes"
    "transform_clojure" "forms" "form" "namespace" "root" "source"
    "dependencies" "callers" "locations" "prepare-change" "basis" "other"})

(def ^:private allowed-refusal-types
  #{"mixed-request-ids" "expect-count-mismatch" "batch-form-selection-failed"
    "invalid-mcp-request" "invalid-request" "stale-source"
    "substantiation-ledger-unhealthy" "substantiation-start-append-failed"
    "other"})

(def ^:private allowed-semantic-kinds
  #{"form" "forms" "namespace" "root" "source" "dependencies" "callers"
    "locations" "other"})

(def ^:private allowed-subject-kinds
  #{"file" "path" "owner" "form" "namespace" "locator" "subject"})

(declare invalid!)

(defn- sha256? [value]
  (and (string? value) (boolean (re-matches #"[0-9a-f]{64}" value))))

(defn- uuid-string? [value]
  (and (string? value)
       (boolean
         (re-matches
           #"[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
           value))))

(defn- instant? [value]
  (and (string? value)
       (try
         (Instant/parse value)
         true
         (catch java.time.format.DateTimeParseException _ false))))

(defn- nonnegative-integer? [value]
  (and (integer? value) (not (neg? value))))

(defn- finite-nonnegative? [value]
  (and (number? value)
       (Double/isFinite (double value))
       (not (neg? (double value)))))

(defn- closed-string-vector? [value allowed]
  (and (vector? value)
       (every? #(and (string? %) (contains? allowed %)) value)
       (= value (vec (sort (distinct value))))))

(defn- closed-map!
  [value required allowed message data]
  (when-not (map? value)
    (invalid! message (assoc data :reason :not-a-map)))
  (let [fields (set (keys value))
        missing (set/difference required fields)
        unknown (set/difference fields allowed)]
    (when (or (seq missing) (seq unknown))
      (invalid! message (assoc data :missing missing :unknown unknown))))
  value)

(defn- validate-subject-token! [subject data]
  (closed-map! subject #{:kind :token} #{:kind :token}
               "Substantiation subject token is not closed" data)
  (when-not (and (contains? allowed-subject-kinds (:kind subject))
                 (sha256? (:token subject)))
    (invalid! "Substantiation subject token is invalid" data)))

(defn- validate-feature! [feature data]
  (closed-map! feature allowed-feature-fields allowed-feature-fields
               "Substantiation feature envelope is not closed" data)
  (closed-map! (:counts feature) #{} allowed-count-fields
               "Substantiation feature counts are not closed" data)
  (closed-map! (:dimensions feature) #{} allowed-dimension-fields
               "Substantiation feature dimensions are not closed" data)
  (when-not (and (string? (:feature_id feature))
                 (boolean (re-matches #"[a-z0-9][a-z0-9-]{0,63}"
                                      (:feature_id feature)))
                 (string? (:stage feature))
                 (boolean (re-matches #"[a-z0-9][a-z0-9-]{0,63}"
                                      (:stage feature)))
                 (every? #(and (integer? %) (not (neg? %)))
                         (vals (:counts feature)))
                 (every? boolean?
                         (vals (dissoc (:dimensions feature)
                                       :skeleton_token :query_token))))
    (invalid! "Substantiation feature values are invalid" data))
  (doseq [[key value] (:dimensions feature)
          :when (str/ends-with? (name key) "token")]
    (when-not (sha256? value)
      (invalid! "Substantiation feature token is invalid"
                (assoc data :dimension key)))))

(defn- validate-request-shape! [shape data]
  (closed-map! shape allowed-request-shape-fields allowed-request-shape-fields
               "Substantiation request shape is not closed" data)
  (when-not (and (closed-string-vector? (:field_presence shape)
                                        allowed-request-fields)
                 (vector? (:subject_tokens shape))
                 (every? nonnegative-integer?
                         ((juxt :request_count :edit_count :change_count) shape)))
    (invalid! "Substantiation request shape values are invalid" data))
  (doseq [subject (:subject_tokens shape)]
    (validate-subject-token! subject data)))

(defn- validate-result-shape! [shape data]
  (closed-map! shape allowed-result-shape-fields allowed-result-shape-fields
               "Substantiation result shape is not closed" data)
  (when-not
    (and (closed-string-vector? (:field_presence shape) allowed-result-fields)
         (vector? (:subject_tokens shape))
         (every? nonnegative-integer?
                 ((juxt :owner_token_count :location_row_count
                        :duplicate_group_count :source_character_count
                        :result_count) shape))
         (closed-string-vector? (:semantic_kinds shape) allowed-semantic-kinds)
         (every? boolean?
                 ((juxt :source_body_present :dependency_evidence_present
                        :hash_evidence_present :evidence_complete :ok :committed
                        :verification_complete :source_unchanged :read_complete)
                  shape))
         (contains? #{"reached" "not-reached"} (:location_cap_state shape))
         (or (nil? (:error_type shape))
             (contains? allowed-refusal-types (:error_type shape)))
         (finite-nonnegative? (:elapsed_ms shape)))
    (invalid! "Substantiation result shape values are invalid" data))
  (doseq [subject (:subject_tokens shape)]
    (validate-subject-token! subject data)))

(defn- validate-event-shape! [event expected-sequence]
  (let [data {:sequence expected-sequence}
        transport (:transport event)
        start? (= "start" (:phase event))]
    (closed-map! transport required-transport-fields required-transport-fields
                 "Substantiation transport is not closed" data)
    (when-not (and (sha256? (:session_token transport))
                   (or (nil? (:turn_token transport))
                       (sha256? (:turn_token transport)))
                   (sha256? (:key_id transport))
                   (string? (:client_name transport))
                   (string? (:client_version transport))
                   (= "unknown" (:caller_model transport))
                   (= "not-exposed" (:caller_model_source transport)))
      (invalid! "Substantiation transport identity is invalid" data))
    (when-not (and (uuid-string? (:event_id event))
                   (uuid-string? (:call_id event))
                   (instant? (:observed_at event))
                   (contains? #{"start" "finish"} (:phase event))
                   (contains? public-tools (:tool event))
                   (contains? allowed-operations (:operation event))
                   (vector? (:features event)))
      (invalid! "Substantiation event values are invalid" data))
    (if start?
      (do
        (when (contains? event :result_shape)
          (invalid! "Start event contains result shape" data))
        (validate-request-shape! (:request_shape event) data))
      (do
        (when (contains? event :request_shape)
          (invalid! "Finish event contains request shape" data))
        (validate-result-shape! (:result_shape event) data)))
    (doseq [feature (:features event)]
      (validate-feature! feature data))))

(defn- invalid! [message data]
  (throw (ex-info message (assoc data :error-type :invalid-substantiation-ledger))))

(defn parse-lines [text]
  (->> (str/split-lines text)
       (remove str/blank?)
       (mapv #(json/parse-string % true))))

(defn validate-chain
  "Validate the closed schema, digest chain, phase lifecycle, and call identity."
  [events]
  (loop [remaining events
         expected-sequence 1
         previous-sha nil
         call-phases {}]
    (if-let [event (first remaining)]
      (let [fields (set (keys event))
            missing (set/difference required-event-fields fields)
            unknown (set/difference fields allowed-event-fields)
            event-sha (:event_sha256 event)
            calculated (substantiation/sha256
                         (substantiation/canonical-json
                           (dissoc event :event_sha256)))
            call-id (:call_id event)
            phase (:phase event)
            prior-phase (get call-phases call-id)]
        (when (seq missing)
          (invalid! "Substantiation event is missing closed fields"
                    {:sequence expected-sequence :missing missing}))
        (when (seq unknown)
          (invalid! "Substantiation event contains unknown fields"
                    {:sequence expected-sequence :unknown unknown}))
        (when-not (= substantiation/event-schema (:schema event))
          (invalid! "Substantiation event schema mismatch"
                    {:sequence expected-sequence}))
        (when-not (= expected-sequence (:sequence event))
          (invalid! "Substantiation sequence gap or reorder"
                    {:expected expected-sequence :actual (:sequence event)}))
        (when-not (= previous-sha (:previous_event_sha256 event))
          (invalid! "Substantiation prior digest mismatch"
                    {:sequence expected-sequence}))
        (when-not (= calculated event-sha)
          (invalid! "Substantiation event digest mismatch"
                    {:sequence expected-sequence}))
        (when-not (or (and (= "start" phase) (nil? prior-phase))
                      (and (= "finish" phase) (= "start" prior-phase)))
          (invalid! "Substantiation call lifecycle is invalid"
                    {:sequence expected-sequence
                     :call-id call-id
                     :phase phase
                     :prior-phase prior-phase}))
        (validate-event-shape! event expected-sequence)
        (recur (next remaining)
               (inc expected-sequence)
               event-sha
               (assoc call-phases call-id phase)))
      (do
        (when-let [unmatched (seq (keep (fn [[call-id phase]]
                                          (when (= "start" phase) call-id))
                                        call-phases))]
          (invalid! "Substantiation ledger contains unmatched starts"
                    {:unmatched-call-ids (vec unmatched)}))
        events))))

(defn- registry-feature? [registry feature-id]
  (or (contains? (:features registry) feature-id)
      (and (str/starts-with? feature-id "elaborator.")
           (contains? (:features registry) "elaborator.*"))))

(defn- validate-registry-features! [registry events]
  (doseq [{:keys [sequence features]} events
          {:keys [feature_id stage]} features
          :let [definition (or (get-in registry [:features feature_id])
                               (when (str/starts-with? feature_id "elaborator.")
                                 (get-in registry [:features "elaborator.*"])))]
          :when definition]
    (when-not (contains? (set (:stages definition)) stage)
      (invalid! "Substantiation feature stage is not registered"
                {:sequence sequence
                 :feature-id feature_id
                 :stage stage}))))

(defn- zero-counts [registry]
  (into (sorted-map)
        (for [[feature-id {:keys [stages]}] (:features registry)
              stage stages]
          [[feature-id stage] 0])))

(defn- feature-counts [registry events]
  (reduce
    (fn [counts {:keys [features]}]
      (reduce
        (fn [acc feature]
          (let [feature-id (:feature_id feature)
                stage (:stage feature)]
            (if (registry-feature? registry feature-id)
              (update acc [feature-id stage] (fnil inc 0))
              acc)))
        counts
        features))
    (zero-counts registry)
    events))

(defn- feature-sums [registry events]
  (reduce
    (fn [sums {:keys [features]}]
      (reduce
        (fn [acc feature]
          (if (registry-feature? registry (:feature_id feature))
            (reduce-kv
              (fn [nested key value]
                (if (and (integer? value) (not (neg? value)))
                  (update nested [(:feature_id feature) (:stage feature) key]
                          (fnil + 0) value)
                  nested))
              acc
              (:counts feature))
            acc))
        sums
        features))
    (sorted-map)
    events))

(defn- completed-calls [events]
  (let [starts (into {} (map (juxt :call_id identity))
                     (filter #(= "start" (:phase %)) events))]
    (mapv (fn [finish]
            {:call-id (:call_id finish)
             :start (get starts (:call_id finish))
             :finish finish})
          (filter #(= "finish" (:phase %)) events))))

(defn- subject-token-set [shape]
  (set (map :token (:subject_tokens shape))))

(defn- same-session? [left right]
  (= (get-in left [:transport :session_token])
     (get-in right [:transport :session_token])))

(defn- within-ten-minutes? [left right]
  (try
    (<= (.toMillis
          (Duration/between (Instant/parse (:observed_at left))
                            (Instant/parse (:observed_at right))))
        600000)
    (catch Exception _ false)))

(defn- classifier-result [shape]
  {:owner-names (->> (:subject_tokens shape)
                     (filter #(re-find #"owner|form|namespace" (:kind %)))
                     (mapv :token))
   :location-rows (:location_row_count shape)
   :duplicate-groups (:duplicate_group_count shape)
   :resolved-duplicate "unknown"
   :semantic-kinds (:semantic_kinds shape)
   :candidate-cap-reached (= "reached" (:location_cap_state shape))
   :required-selector "unknown"
   :answer-token "unknown"
   :answer-unique "unknown"
   :evidence-complete (:evidence_complete shape)})

(defn recovery-episodes
  "Classify the next qualifying completed action within seven calls/ten minutes."
  [events]
  (let [calls (completed-calls events)]
    (->> calls
         (keep-indexed
           (fn [index call]
             (when (some #(and (= "complete-refusal" (:feature_id %))
                               (= "fired" (:stage %)))
                         (get-in call [:finish :features]))
               (let [baseline-subjects
                     (subject-token-set (get-in call [:start :request_shape]))
                     candidates (subvec calls (inc index)
                                        (min (count calls) (+ index 8)))
                     match (first
                             (filter
                               #(and (same-session? (:finish call) (:finish %))
                                     (within-ten-minutes? (:finish call) (:finish %)))
                               candidates))
                     match-subjects
                     (subject-token-set (get-in match [:start :request_shape]))
                     same-subject? (seq (set/intersection baseline-subjects
                                                          match-subjects))
                     outcome
                     (cond
                       (nil? match) "abandoned"
                       (and same-subject?
                            (= "inspect_clojure" (get-in match [:start :tool])))
                       "same-file-reread"
                       (and same-subject?
                            (contains? #{"edit_clojure" "apply_clojure_changes"}
                                       (get-in match [:start :tool])))
                       "direct-corrected-retry"
                       :else "other-next-action")]
                 {:episode_id (get-in call [:finish :call_id])
                  :caller_model (get-in call [:finish :transport :caller_model]
                                        "unknown")
                  :outcome outcome
                  :reread (= outcome "same-file-reread")
                  :refusal (classifier-result
                             (get-in call [:finish :result_shape]))
                  :recovery_read
                  (if (= outcome "same-file-reread")
                    (classifier-result (get-in match [:finish :result_shape]))
                    {:owner-names []
                     :location-rows 0
                     :duplicate-groups "unknown"
                     :resolved-duplicate "unknown"
                     :semantic-kinds []
                     :evidence-complete false})}))))
         vec)))

(defn compile-report
  [{:keys [events registry baseline marker ledger-bytes]}]
  (validate-chain events)
  (validate-registry-features! registry events)
  (let [counts (feature-counts registry events)
        sums (feature-sums registry events)
        calls (completed-calls events)
        episodes (recovery-episodes events)
        completed (count calls)
        client-strata
        (frequencies
          (map (fn [{:keys [finish]}]
                 [(get-in finish [:transport :client_name] "unknown")
                  (get-in finish [:transport :client_version] "unknown")
                  (get-in finish [:transport :caller_model] "unknown")])
               calls))]
    {:schema "clj-surgeon.substantiation-report.v1"
     :window {:start_sequence (or (:start_sequence marker)
                                  (:start-sequence marker))
              :end_sequence (or (:end_sequence marker) (:end-sequence marker))
              :marker_sha256 (:sha256 marker)}
     :ledger {:bytes ledger-bytes
              :completed_calls completed
              :bytes_per_completed_call
              (if (pos? completed) (/ (double ledger-bytes) completed) 0.0)}
     :features counts
     :feature_count_sums sums
     :client_strata client-strata
     :coverage {:complete_calls completed
                :ledger_gaps 0
                :classifier_episodes (count episodes)}
     :recovery {:episodes (count episodes)
                :same_file_rereads (count (filter :reread episodes))}
     :baseline {:marker (:marker baseline)
                :comparison_evidence_class :observed-before-after}
     :claims {:counts :measured
              :durations :measured
              :historical_comparison :observed-before-after
              :decode_seconds :projected
              :unavailable :unavailable}
     :projection {:emitted_byte_ms (get-in baseline [:projection_rates :emitted_byte_ms])
                  :evidence_class :projected}
     :promotion_authority false
     :episodes episodes}))
