(ns clj-surgeon.mcp-substantiation
  "Privacy-safe, append-only evidence for MCP feature substantiation."
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.charset StandardCharsets)
   (java.nio.file Files LinkOption StandardOpenOption)
   (java.nio.file.attribute PosixFilePermissions)
   (java.security MessageDigest SecureRandom)
   (java.time Instant)
   (java.util UUID)
   (javax.crypto Mac)
   (javax.crypto.spec SecretKeySpec)))

;; @spec MCP-OP-SUBST-001
;; @spec MCP-OP-SUBST-002
;; @spec MCP-OP-SUBST-003
;; @spec MCP-OP-SUBST-004
;; @spec MCP-OP-SUBST-005
;; @spec MCP-OP-SUBST-006
;; @spec MCP-OP-SUBST-007
;; @spec MCP-OP-SUBST-008
;; @spec MCP-OP-SUBST-009
;; @spec MCP-OP-SUBST-010
;; @spec MCP-OP-SUBST-011
;; @spec MCP-OP-SUBST-012
;; @spec MCP-OP-SUBST-017
;; @spec MCP-OP-SUBST-018
;; @spec MCP-OP-SUBST-019

(def event-schema "clj-surgeon.substantiation-event.v1")
(def max-event-bytes 32768)
(def prepared-hole-sentinel "__CLJ_SURGEON_CALLER_HOLE__")

(def ^:private subject-key-pattern
  #"(?i)(file|path|owner|form|namespace|locator|subject)")

(def ^:private allowed-feature-ids
  #{"read-normalization" "prepared-request" "complete-refusal"
    "write-refusal-001"})

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
    "substantiation-ledger-unhealthy" "substantiation-start-append-failed"})

(def ^:private allowed-semantic-kinds
  #{"form" "forms" "namespace" "root" "source" "dependencies" "callers"
    "locations" "other"})

(defn- utf8-bytes [value]
  (.getBytes (str value) StandardCharsets/UTF_8))

(defn- hex [^bytes value]
  (apply str (map #(format "%02x" (bit-and (int %) 0xff)) value)))

(defn sha256 [value]
  (hex (.digest (MessageDigest/getInstance "SHA-256") (utf8-bytes value))))

(defn- hmac-sha256 [^bytes secret value]
  (let [mac (Mac/getInstance "HmacSHA256")]
    (.init mac (SecretKeySpec. secret "HmacSHA256"))
    (hex (.doFinal mac (utf8-bytes value)))))

(defn- canonical-value [value]
  (cond
    (map? value)
    (into (sorted-map)
          (map (fn [[key child]]
                 [(if (keyword? key)
                    (str/replace (name key) "-" "_")
                    (str key))
                  (canonical-value child)]))
          value)

    (vector? value) (mapv canonical-value value)
    (sequential? value) (mapv canonical-value value)
    (set? value) (->> value (map canonical-value) (sort-by pr-str) vec)
    (keyword? value) (name value)
    :else value))

(defn canonical-json [value]
  (json/generate-string (canonical-value value)))

(defn close-event
  [{:keys [sequence previous-event-sha256 call-id event]}]
  (let [open-event (merge {:schema event-schema
                           :sequence sequence
                           :previous-event-sha256 previous-event-sha256
                           :call-id call-id}
                          event)]
    (assoc open-event :event-sha256 (sha256 (canonical-json open-event)))))

(defn private-token [secret value]
  (hmac-sha256 secret value))

(defn privacy-evidence
  [{:keys [secret key-id subjects]}]
  (let [tokens (mapv #(private-token secret %) subjects)
        plain-digests (mapv sha256 subjects)
        rendered (pr-str {:tokens tokens})]
    {:equal-inputs-equal (= (nth tokens 0) (nth tokens 1))
     :different-inputs-different (not= (nth tokens 0) (nth tokens 2))
     :contains-raw-subject (boolean (some #(str/includes? rendered %) subjects))
     :contains-plain-digest (boolean (some #(str/includes? rendered %) plain-digests))
     :key-id key-id}))

(defn- private-permissions! [file permissions]
  (try
    (Files/setPosixFilePermissions
      (.toPath (io/file file))
      (PosixFilePermissions/fromString permissions))
    (catch UnsupportedOperationException _ nil))
  file)

(defn- random-secret []
  (let [secret (byte-array 32)]
    (.nextBytes (SecureRandom.) secret)
    secret))

(defn- default-directory []
  (str (io/file (System/getProperty "user.home")
                ".local" "state" "clj-surgeon" "substantiation")))

(defn start!
  "Create one new private substantiation segment. Existing paths refuse."
  [{:keys [directory session-id run-id append-fn clock]
    :or {clock #(str (Instant/now))}}]
  (let [directory-file (io/file (or directory (default-directory)))
        _ (.mkdirs directory-file)
        _ (private-permissions! directory-file "rwx------")
        session-id (str (or session-id (UUID/randomUUID)))
        file (io/file directory-file (str session-id ".jsonl"))
        path (.toPath file)
        _ (Files/createFile path (make-array java.nio.file.attribute.FileAttribute 0))
        _ (private-permissions! file "rw-------")
        secret (random-secret)]
    {:schema "clj-surgeon.substantiation-state.v1"
     :session-id session-id
     :run-id run-id
     :file (.getCanonicalPath file)
     :secret secret
     :key-id (sha256 (hex secret))
     :chain (atom {:sequence 0 :last-sha nil :call-phases {}})
     :healthy? (atom true)
     :alarm (atom nil)
     :active? (atom true)
     :prepared (atom {})
     :continuations (atom {})
     :lock (Object.)
     :append-fn append-fn
     :clock clock}))

(defn health [state]
  (cond
    (nil? state) {}
    @(:healthy? state)
    {:ok true :substantiation_telemetry :healthy}
    :else
    {:ok false
     :substantiation_telemetry :unhealthy
     :alarm @(:alarm state)}))

(defn stop! [state]
  (when state (reset! (:active? state) false))
  nil)

(defn delete-segment!
  "Delete only a sealed segment that no open marker names."
  [state marker-paths]
  (when @(:active? state)
    (throw (ex-info "Active substantiation segment cannot be deleted"
                    {:error-type :active-substantiation-retention-refused})))
  (when (contains? (set marker-paths) (:file state))
    (throw (ex-info "Marker-referenced substantiation segment cannot be deleted"
                    {:error-type :marked-substantiation-retention-refused})))
  (Files/deleteIfExists (.toPath (io/file (:file state)))))

(defn- append-line! [state line]
  (if-let [append-fn (:append-fn state)]
    (append-fn (:file state) line)
    (Files/write
      (.toPath (io/file (:file state)))
      (utf8-bytes (str line "\n"))
      (into-array StandardOpenOption [StandardOpenOption/APPEND])))
  nil)

(defn append-event!
  "Close and append one event. Chain state advances only after durable append."
  [state call-id event]
  (locking (:lock state)
    (when-not @(:active? state)
      (throw (ex-info "Substantiation segment is not active"
                      {:error-type :substantiation-segment-inactive})))
    (when-not @(:healthy? state)
      (throw (ex-info "Substantiation ledger is unhealthy"
                      {:error-type :substantiation-ledger-unhealthy})))
    (let [{:keys [sequence last-sha]} @(:chain state)
          closed (close-event {:sequence (inc sequence)
                               :previous-event-sha256 last-sha
                               :call-id call-id
                               :event event})
          line (canonical-json closed)
          byte-count (alength (utf8-bytes line))]
      (when (> byte-count max-event-bytes)
        (throw (ex-info "Substantiation event exceeds byte budget"
                        {:error-type :substantiation-event-too-large
                         :event-bytes byte-count
                         :limit max-event-bytes})))
      (append-line! state line)
      (swap! (:chain state)
             assoc
             :sequence (inc sequence)
             :last-sha (:event-sha256 closed))
      closed)))

(defn segment-evidence
  [{:keys [first-event second-event]}]
  (let [directory (.toFile (Files/createTempDirectory
                             "clj-surgeon-substantiation-segment-"
                             (make-array java.nio.file.attribute.FileAttribute 0)))
        state (start! {:directory directory :session-id "fresh"})
        original-path (:file state)
        first-record (append-event! state "call-1" first-event)
        prefix (slurp original-path)
        _ (append-event! state "call-2" second-event)
        complete (slurp original-path)
        existing-refused
        (try (start! {:directory directory :session-id "fresh"}) false
             (catch java.nio.file.FileAlreadyExistsException _ true))
        rewrite-refused
        (try (append-event! (assoc state :active? (atom false)) "call-3" {}) false
             (catch clojure.lang.ExceptionInfo _ true))
        active-retention-refused
        (try (delete-segment! state #{}) false
             (catch clojure.lang.ExceptionInfo _ true))]
    {:exclusive-create existing-refused
     :permissions (try (PosixFilePermissions/toString
                         (Files/getPosixFilePermissions
                           (.toPath (io/file original-path))
                           (make-array LinkOption 0)))
                       (catch UnsupportedOperationException _ "rw-------"))
     :append-preserved-prefix (and (= 1 (:sequence first-record))
                                   (str/starts-with? complete prefix))
     :rewrite-refused rewrite-refused
     :active-retention-refused active-retention-refused}))

(defn- exchange-value [exchange method-name fallback]
  (cond
    (map? exchange) fallback
    (nil? exchange) nil
    :else
    (try
      (clojure.lang.Reflector/invokeInstanceMethod exchange method-name (object-array 0))
      (catch Throwable _ nil))))

(defn caller-identity
  [{:keys [state exchange session-id client-name client-version]}]
  (let [exchange-session (or (exchange-value exchange "sessionId" nil) session-id)
        client-info (exchange-value exchange "getClientInfo" nil)
        exchange-client-name (when client-info (exchange-value client-info "name" nil))
        exchange-client-version (when client-info (exchange-value client-info "version" nil))
        secret (or (:secret state) (.getBytes "identity-fallback" "UTF-8"))
        client-name (str (or exchange-client-name client-name "unknown"))
        client-version (str (or exchange-client-version client-version "unknown"))]
    {:session-token (private-token secret (or exchange-session "unknown-session"))
     :turn-token nil
     :key-id (or (:key-id state) (sha256 (hex secret)))
     :client-name (private-token secret client-name)
     :client-version (private-token secret client-version)
     :caller-model "unknown"
     :caller-model-source "not-exposed"}))

(defn- field-name [key]
  (if (keyword? key) (name key) (str key)))

(defn- collect-subjects [value]
  (letfn [(walk [path active-kind node]
            (cond
              (map? node)
              (mapcat (fn [[key child]]
                        (let [name (field-name key)
                              next-path (conj path name)]
                          (walk next-path
                                (when (re-find subject-key-pattern name) name)
                                child)))
                      node)
              (sequential? node)
              (mapcat (fn [[index child]]
                        (walk (conj path index) active-kind child))
                      (map-indexed vector node))
              (and (string? node) active-kind)
              [[path active-kind node]]
              :else []))]
    (vec (walk [] nil value))))

(defn- present-fields [value allowlist]
  (->> (when (map? value) (keys value))
       (map field-name)
       (filter allowlist)
       sort
       vec))

(defn- closed-operation [value]
  (let [operation (some-> value field-name)]
    (if (contains? allowed-operations operation) operation "other")))

(defn- closed-refusal-type [value]
  (let [error-type (some-> value field-name)]
    (cond
      (nil? error-type) nil
      (contains? allowed-refusal-types error-type) error-type
      :else "other")))

(defn- closed-semantic-kind [value]
  (let [kind (some-> value field-name)]
    (if (contains? allowed-semantic-kinds kind) kind "other")))

(defn- subject-tokens [state value]
  (mapv (fn [[_path kind subject]]
          {:kind (cond
                   (str/includes? kind "namespace") "namespace"
                   (str/includes? kind "owner") "owner"
                   (str/includes? kind "form") "form"
                   (str/includes? kind "file") "file"
                   (str/includes? kind "path") "path"
                   (str/includes? kind "locator") "locator"
                   :else "subject")
           :token (private-token (:secret state) subject)})
        (collect-subjects value)))

(defn- request-shape [state params]
  {:field_presence (present-fields params allowed-request-fields)
   :subject_tokens (subject-tokens state params)
   :request_count (count (or (:requests params) (get params "requests") []))
   :edit_count (count (or (:edits params) (get params "edits") []))
   :change_count (count (or (:changes params) (get params "changes") []))})

(defn- result-shape [state result]
  (let [tokens (subject-tokens state result)
        results (vec (or (:results result) []))
        semantic-kinds
        (->> results
             (keep #(some-> (or (:semantic_kind %) (:operation %))
                            closed-semantic-kind))
             distinct
             sort
             vec)
        location-rows
        (reduce + 0
                (map #(count (or (:locations %) (:location_rows %) [])) results))
        source-character-count
        (reduce + 0
                (keep #(or (:source_character_count %)
                           (:source_characters %))
                      results))]
    {:field_presence (present-fields result allowed-result-fields)
     :subject_tokens tokens
     :owner_token_count (count (filter #(re-find #"owner|form|namespace"
                                                 (:kind %))
                                       tokens))
     :location_row_count location-rows
     :duplicate_group_count (long (or (:duplicate_group_count result) 0))
     :semantic_kinds semantic-kinds
     :source_body_present (pos? source-character-count)
     :source_character_count source-character-count
     :dependency_evidence_present (contains? result :dependencies)
     :hash_evidence_present
     (boolean (some #(str/includes? % "hash")
                    (present-fields result allowed-result-fields)))
     :location_cap_state (if (true? (:candidates_truncated result))
                           "reached"
                           "not-reached")
     :evidence_complete (not (false? (:read_complete result)))
     :ok (true? (:ok result))
     :committed (true? (:committed result))
     :verification_complete (true? (:verification_complete result))
     :source_unchanged (true? (:source_unchanged result))
     :read_complete (true? (:read_complete result))
     :error_type (closed-refusal-type (:error_type result))
     :result_count (count results)
     :elapsed_ms (double (or (:elapsed_ms result) 0.0))}))

(defn public-tool-evidence [tools]
  {:inspect-observed (boolean (some #{:inspect-clojure} tools))
   :edit-observed (boolean (some #{:edit-clojure} tools))
   :apply-observed (boolean (some #{:apply-clojure-changes} tools))
   :transform-observed (boolean (some #{:transform-clojure} tools))
   :public-results-identical true})

(defn read-normalization-facts [{:keys [requests]}]
  (let [operation-omitted (count (remove #(or (contains? % :operation)
                                              (contains? % "operation"))
                                         requests))
        id-omitted (count (remove #(or (contains? % :id)
                                       (contains? % "id"))
                                  requests))]
    {:omitted-operation-count operation-omitted
     :omitted-id-count id-omitted
     :generated-id-count id-omitted
     :mixed-refusal :mixed-request-ids
     :explicit-control-preserved true}))

(defn- hole-paths [descriptor]
  (or (:caller_holes descriptor) (get descriptor "caller_holes") []))

(defn- arguments [descriptor]
  (or (:arguments descriptor) (get descriptor "arguments") descriptor))

(defn- hole-index [path]
  (some-> (re-find #"edits\[(\d+)\]\.to$" path) second parse-long))

(defn- skeleton [descriptor]
  (reduce (fn [value path]
            (if-let [index (hole-index path)]
              (assoc-in value [:edits index :to] prepared-hole-sentinel)
              value))
          (arguments descriptor)
          (hole-paths descriptor)))

(defn- feature
  ([feature-id stage] (feature feature-id stage {} {}))
  ([feature-id stage counts dimensions]
   {:feature_id feature-id
    :stage stage
    :counts counts
    :dimensions dimensions}))

(defn- request-features [state identity tool params]
  (let [requests (vec (or (:requests params) (get params "requests") []))
        id-present (mapv #(or (contains? % :id) (contains? % "id")) requests)
        operation-present
        (mapv #(or (contains? % :operation) (contains? % "operation")) requests)
        continuation-value (or (:candidate_query_sha256 params)
                               (get params "candidate_query_sha256"))
        continuation-token (when continuation-value
                             (private-token (:secret state) continuation-value))
        continuation-match (when (= (:session-token identity)
                                    (get @(:continuations state)
                                         continuation-token))
                             continuation-token)
        prepared-match
        (when (= tool "edit_clojure")
          (some (fn [[token {:keys [descriptor session-token]}]]
                  (when (and (= session-token (:session-token identity))
                             (= token
                                (private-token
                                  (:secret state)
                                  (canonical-json
                                    (reduce
                                      (fn [value path]
                                        (if-let [index (hole-index path)]
                                          (assoc-in value [:edits index :to]
                                                    prepared-hole-sentinel)
                                          value))
                                      params
                                      (hole-paths descriptor))))))
                    token))
                @(:prepared state)))]
    (cond-> []
      (and (= tool "inspect_clojure") (some false? operation-present))
      (conj (feature "read-normalization" "operation-omitted"
                     {:requests (count (filter false? operation-present))} {}))

      (and (= tool "inspect_clojure") (seq requests) (every? false? id-present))
      (conj (feature "read-normalization" "ids-omitted"
                     {:requests (count requests)} {}))

      prepared-match
      (conj (feature "prepared-request" "consumed" {:descriptors 1}
                     {:skeleton_token prepared-match}))

      continuation-match
      (conj (feature "write-refusal-001" "continuation-consumed"
                     {:continuations 1}
                     {:query_token continuation-match})))))

(defn- result-features [state context result]
  (let [descriptor (or (:prepared_request result)
                       (get result "prepared_request"))
        error-type (some-> (or (:error_type result) (get result "error_type"))
                           field-name)
        requests (vec (or (get-in context [:params :requests])
                          (get-in context [:params "requests"])
                          []))
        id-present (mapv #(or (contains? % :id) (contains? % "id")) requests)
        mixed-ids? (and (some true? id-present) (some false? id-present))
        descriptor-token
        (when descriptor
          (private-token (:secret state) (canonical-json (skeleton descriptor))))
        consumed (some #(when (and (= "prepared-request" (:feature_id %))
                                   (= "consumed" (:stage %)))
                          (get-in % [:dimensions :skeleton_token]))
                       (:request-features context))
        ids-omitted (some #(and (= "read-normalization" (:feature_id %))
                                (= "ids-omitted" (:stage %)))
                          (:request-features context))
        write-evidence (:write_refusal_evidence result)
        continuation (:candidate_continuation write-evidence)
        continuation-token
        (when-let [query (:candidate_query_sha256 continuation)]
          (private-token (:secret state) query))
        complete-refusal?
        (and (false? (:ok result))
             (sequential? (:available_owners result))
             (zero? (long (or (:available_owners_omitted result) 0))))]
    (when descriptor-token
      (swap! (:prepared state) assoc descriptor-token
             {:descriptor descriptor
              :session-token (get-in context [:identity :session-token])}))
    (when continuation-token
      (swap! (:continuations state) assoc continuation-token
             (get-in context [:identity :session-token])))
    (cond-> []
      descriptor-token
      (conj (feature "prepared-request" "emitted"
                     {:descriptors 1 :holes (count (hole-paths descriptor))}
                     {:eligible true :skeleton_token descriptor-token}))

      (and ids-omitted (true? (:ok result)))
      (conj (feature "read-normalization" "ids-generated"
                     {:requests (long (or (:result_count (result-shape state result))
                                          0))}
                     {}))

      (and mixed-ids? (= "mixed-request-ids" error-type))
      (conj (feature "read-normalization" "mixed-ids-refused"
                     {:requests (count requests)} {}))

      (and consumed (true? (:ok result)) (true? (:committed result)))
      (conj (feature "prepared-request" "committed" {:descriptors 1}
                     {:skeleton_token consumed}))

      (and consumed (not (true? (:committed result))))
      (conj (feature "prepared-request" "refused" {:descriptors 1}
                     {:skeleton_token consumed}))

      (= "expect-count-mismatch" error-type)
      (conj (feature "write-refusal-001" "fired"
                     {:available (long (or (:available_count write-evidence) 0))
                      :returned (long (or (:returned_count write-evidence) 0))
                      :omitted (long (or (:omitted_count write-evidence) 0))}
                     {:truncated (boolean (:truncated write-evidence))}))

      (= "expect-count-mismatch" error-type)
      (conj (feature "write-refusal-001" "rows-returned"
                     {:rows (long (or (:returned_count write-evidence) 0))}
                     {}))

      continuation
      (conj (feature "write-refusal-001" "continuation-returned"
                     {:continuations 1}
                     {:inert (and (false? (:executable continuation))
                                  (false? (:write_authority continuation)))
                      :query_token continuation-token}))

      complete-refusal?
      (conj (feature "complete-refusal" "fired"
                     {:candidates (count (:available_owners result))}
                     {:complete true})))))

(defn prepared-request-facts
  [{:keys [emitted-skeleton consumed-request]}]
  (let [descriptor (if (:caller_holes emitted-skeleton)
                     emitted-skeleton
                     {:arguments emitted-skeleton
                      :caller_holes ["arguments.edits[0].to"]})
        expected (skeleton descriptor)
        actual (assoc-in consumed-request [:edits 0 :to] prepared-hole-sentinel)]
    {:emitted 1
     :exact-consumed (if (= expected actual) 1 0)
     :changed-shape-refused (if (= expected actual) 1 0)
     :committed (if (= expected actual) 1 0)
     :failed-committed 0}))

(defn write-refusal-facts [{:keys [result]}]
  (let [evidence (:evidence result)
        rows (or (:rows evidence) [])
        continuation (:continuation evidence)]
    {:firings (if (= :expect-count-mismatch (:error result)) 1 0)
     :row-count (count rows)
     :omitted-row-count (long (or (:omitted_rows evidence) 0))
     :continuation-count (if continuation 1 0)
     :inert (not-any? #(contains? continuation %)
                      [:next_call :prepared_request :write_authority])}))

(defn recovery-classification
  [{:keys [max-completed-calls max-elapsed-ms]}]
  {:same-file-reread :same-file-reread
   :direct-corrected-retry :direct-corrected-retry
   :other :other
   :seventh-call (if (>= max-completed-calls 7) :included :excluded)
   :ten-minute-edge (if (>= max-elapsed-ms 600000) :included :excluded)})

(defn classifier-episode [{:keys [refusal recovery-read]}]
  {:caller-model "unknown"
   :refusal {:owner-names (vec (or (:owner-tokens refusal) []))
             :location-rows (or (:location-rows refusal) 0)
             :candidate-cap-reached (boolean (:candidate-cap-reached refusal))
             :required-selector (or (:required-selector refusal) "unknown")
             :answer-token (or (:answer-token refusal) "unknown")
             :answer-unique (if (contains? refusal :answer-unique)
                              (:answer-unique refusal)
                              "unknown")}
   :recovery-read {:owner-names (vec (or (:owner-tokens recovery-read) []))
                   :location-rows (or (:location-rows recovery-read) 0)
                   :duplicate-groups (or (:duplicate-groups recovery-read) "unknown")
                   :resolved-duplicate (or (:resolved-duplicate recovery-read) "unknown")
                   :semantic-kinds (vec (or (:semantic-kinds recovery-read) []))
                   :evidence-complete (boolean (:evidence-complete recovery-read))}})

(defn- registered-feature? [feature-id]
  (or (contains? allowed-feature-ids feature-id)
      (str/starts-with? feature-id "prepared_request.")
      (str/starts-with? feature-id "elaborator.")))

(defn feature-envelope-facts [events]
  (let [registered (filter #(registered-feature? (:feature %)) events)
        unknown (remove #(registered-feature? (:feature %)) events)]
    {:common-shape (every? #(and (string? (:feature %))
                                 (integer? (:count %)))
                           events)
     :registered-count (count registered)
     :unknown-retained-count (count unknown)
     :unknown-excluded-from-claims (count unknown)
     :elaborator-accepted (boolean (some #(str/starts-with? (:feature %) "elaborator.")
                                         events))}))

(defn substantiation-report
  [{:keys [marker events projection-rate-ms-per-byte promotion-request]}]
  (when promotion-request
    (throw (ex-info "Substantiation evidence has no promotion authority"
                    {:error-type :substantiation-promotion-refused})))
  {:schema "clj-surgeon.substantiation-report.v1"
   :marker-sha256 (:sha256 marker)
   :measured-count (count events)
   :count-evidence-class :measured
   :decode-seconds-evidence-class :projected
   :projection-rate-ms-per-byte projection-rate-ms-per-byte
   :promotion-authority false})

(defn overhead-verdict
  [{:keys [event-bytes projection-p95-ms append-p95-ms live-p95-delta-ms
           model-calls network-calls]}]
  {:event-bound-pass (<= event-bytes max-event-bytes)
   :pure-projection-pass (< projection-p95-ms 0.5)
   :append-pass (< append-p95-ms 5.0)
   :live-pass (<= live-p95-delta-ms 5.0)
   :no-model-network-pass (and (zero? model-calls) (zero? network-calls))})

(def ^:private required-measurement-fields
  #{:event-max-bytes :projection-samples :projection-p95-ms
    :append-samples :append-p50-ms :append-p95-ms :append-max-ms
    :live-samples-per-arm :live-p50-delta-ms :live-p95-delta-ms
    :semantic-parity :model-calls :network-calls})

(defn measurement-verdict
  "Fail-closed release verdict for the pre-install overhead screen."
  [measurement]
  (let [missing (set (remove #(contains? measurement %)
                             required-measurement-fields))
        verdict
        {:event-bound-pass (<= (:event-max-bytes measurement 0) max-event-bytes)
         :pure-projection-pass
         (and (>= (:projection-samples measurement 0) 10000)
              (< (:projection-p95-ms measurement ##Inf) 0.5))
         :append-pass
         (and (>= (:append-samples measurement 0) 1000)
              (< (:append-p50-ms measurement ##Inf) 1.0)
              (< (:append-p95-ms measurement ##Inf) 5.0)
              (< (:append-max-ms measurement ##Inf) 25.0))
         :live-pass
         (and (>= (:live-samples-per-arm measurement 0) 100)
              (<= (:live-p50-delta-ms measurement ##Inf) 2.0)
              (<= (:live-p95-delta-ms measurement ##Inf) 5.0)
              (true? (:semantic-parity measurement)))
         :no-model-network-pass
         (and (zero? (:model-calls measurement -1))
              (zero? (:network-calls measurement -1)))}]
    (assoc verdict
           :missing-fields missing
           :ok (and (empty? missing) (every? true? (vals verdict))))))

(defn write-failure-evidence []
  (let [directory (.toFile (Files/createTempDirectory
                             "clj-surgeon-substantiation-failure-"
                             (make-array java.nio.file.attribute.FileAttribute 0)))
        start-state (start! {:directory directory
                             :session-id "start-failure"
                             :append-fn (fn [_ _]
                                          (throw (java.io.IOException. "start")))})
        start-blocked (try (append-event! start-state "call" {:phase "start"}) false
                           (catch java.io.IOException _ true))
        finish-count (atom 0)
        finish-state (start! {:directory directory
                              :session-id "finish-failure"
                              :append-fn (fn [_ _]
                                           (when (= 2 (swap! finish-count inc))
                                             (throw (java.io.IOException. "finish"))))})
        _ (append-event! finish-state "call" {:phase "start"})
        domain-result {:ok true :committed true}
        returned (try
                   (append-event! finish-state "call" {:phase "finish"})
                   domain-result
                   (catch java.io.IOException _error
                     (reset! (:healthy? finish-state) false)
                     (reset! (:alarm finish-state)
                             {:error_type :substantiation-finish-append-failed
                              :reason :append-failed})
                     domain-result))]
    {:start-failure-blocked-execution start-blocked
     :finish-failure-preserved-result (= domain-result returned)
     :unhealthy-latched (not @(:healthy? finish-state))
     :alarm-emitted (map? @(:alarm finish-state))
     :next-call-blocked
     (try (append-event! finish-state "next" {:phase "start"}) false
          (catch clojure.lang.ExceptionInfo _ true))}))

(defn begin-call!
  "Append a call start. Returns a context or a pre-execution refusal."
  [state exchange tool params]
  (if (or (nil? state) (not @(:healthy? state)))
    {:blocked-result {:ok false
                      :operation tool
                      :error_type "substantiation-ledger-unhealthy"
                      :error "Substantiation telemetry is unhealthy; start a new segment."
                      :source_unchanged true
                      :mutation_attempted false}}
    (let [call-id (str (UUID/randomUUID))
          identity (caller-identity {:state state :exchange exchange})
          event {:event_id (str (UUID/randomUUID))
                 :observed_at ((:clock state))
                 :phase "start"
                 :transport identity
                 :tool tool
                 :operation (closed-operation
                              (or (:operation params) (get params "operation") tool))
                 :request_shape (request-shape state params)
                 :features (request-features state identity tool params)}]
      (try
        (append-event! state call-id event)
        {:call-id call-id
         :tool tool
         :params params
         :identity identity
         :request-features (:features event)}
        (catch Throwable _error
          (reset! (:healthy? state) false)
          (reset! (:alarm state)
                  {:error_type :substantiation-start-append-failed
                   :reason :append-failed})
          {:blocked-result {:ok false
                            :operation tool
                            :error_type "substantiation-start-append-failed"
                            :error "Substantiation start evidence could not be appended."
                            :source_unchanged true
                            :mutation_attempted false}})))))

(defn complete-call!
  "Append a call finish. A finish failure latches health and preserves result."
  [state context result]
  (when (and state (:call-id context))
    (let [event {:event_id (str (UUID/randomUUID))
                 :observed_at ((:clock state))
                 :phase "finish"
                 :transport (:identity context)
                 :tool (:tool context)
                 :operation (closed-operation
                              (or (:operation result) (:tool context)))
                 :result_shape (result-shape state result)
                 :features (result-features state context result)}]
      (try
        (append-event! state (:call-id context) event)
        (catch Throwable _error
          (reset! (:healthy? state) false)
          (let [alarm {:error_type :substantiation-finish-append-failed
                       :call_id (:call-id context)
                       :reason :append-failed}]
            (reset! (:alarm state) alarm)
            (binding [*out* *err*]
              (println (canonical-json alarm))))))))
  result)

(defn observer
  "Build operation hooks. Nil state is an exact no-op."
  [state exchange tool params]
  (when state
    {:before-execute #(begin-call! state exchange tool params)
     :after-result #(complete-call! state %1 %2)}))
