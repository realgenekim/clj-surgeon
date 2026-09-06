(ns clj-surgeon.mission-events
  "Function-emitted completion events for the public mission boundaries.
   One invocation emits one plan/apply/undo result, including refusal or throw.
   Saved route context is projected when read, never re-read for telemetry.
   No proof or commit stage is inferred from a terminal mission state.
   Logging is best effort and cannot change the wrapped result or exception."
  (:require
   [clj-surgeon.telemetry-events :as events]))

(def ^:dynamic *context* nil)

(def error-types
  #{"mission-unknown-id" "mission-corrupt-mission" "mission-unreadable-mission"
    "mission-verification-profile-not-admitted"
    "mission-unknown-verb" "mission-not-found" "mission-invalid-id"
    "mission-corrupt" "mission-stale-snapshot" "mission-illegal-transition"
    "mission-dependency-unverified" "mission-verification-profile-required"
    "mission-verification-profile-not-found" "mission-undo-receipt-missing"
    "mission-undo-failed" "mission-not-ready" "mission-run-request"
    "typist-route-refused" "typist-incomplete-file-set" "typist-plan-invalid"
    "typist-file-budget" "typist-source-budget" "typist-duplicate-owner"
    "typist-identical-proof-commands" "typist-parser-depth"
    "forms-protected-syntax" "typist-all-candidates-rejected"
    "typist-proof-failed" "typist-acceptance-failed"
    "typist-stale-plan" "typist-format-failed" "typist-formatter-failed"
    "typist-gate-failed" "typist-candidate-format-failed"
    "typist-execution-failed" "typist-transport-cleanup-incomplete"})

(defn scalar-name [value]
  (cond (keyword? value) (name value)
        (string? value) value))

(defn context
  "Whitelist a saved mission's identity and planned route, never its source."
  [mission]
  (let [route (get-in mission [:plan :typist :route])
        id (:id mission)]
    (events/mission-fields
      {:mission_id (when (and (string? id) (re-matches #"M-[0-9]{1,12}" id)) id)
       :mission_state (scalar-name (:state mission))
       :mission_verb (:verb mission)
       :executor (scalar-name (:executor route))
       :candidate_count (:k route)
       :provider (scalar-name (get-in route [:provider :id]))
       :model (get-in route [:provider :model])
       :upstream (get-in route [:provider :upstream])})))

(defn safe-context [mission]
  (try (context mission) (catch Throwable _ {})))

(defn remember! [mission]
  (try (when *context* (swap! *context* merge (safe-context mission)))
       (catch Throwable _ nil))
  mission)

(defn event
  "Pure result projection; the id binds this event to the saved receipt."
  [operation prior result wall-ms threw?]
  (let [phase? (contains? #{"verify" "commit"} operation)
        failed? (or threw?
                    (and (= "verify" operation) (not (true? (:ok result))))
                    (and (= "commit" operation) (not (true? (:committed result))))
                    (false? (:ok result))
                    (#{:blocked :failed} (:state result))
                    (false? (get-in result [:receipt :committed])))
        error (scalar-name (or (:error_type result) (:error-type result)
                               (get-in result [:decision :error_type])
                               (get-in result [:receipt :error-type])
                               (get-in result [:receipt :error_type])))
        rung (or (:condition result) (get-in result [:decision :evidence :condition]))]
    (merge prior (when-not phase? (context result))
           {:kind (get {"propose" "mission-plan" "apply" "mission-apply"
                        "undo" "mission-undo" "verify" "mission-verify"
                        "commit" "mission-commit"} operation "mission-boundary")
            :tool "mission"
            :ok (not failed?)
            :wall_ms wall-ms
            :error_type (when failed? (cond threw? "mission-exception"
                                            (error-types error) error
                                            :else "mission-refused"))}
           (events/mission-fields {:refused_rung (scalar-name rung)}))))

(defn emit! [operation prior result started threw?]
  (try
    (events/record! (event operation prior result
                           (/ (- (System/nanoTime) started) 1000000.0) threw?))
    (catch Throwable _ nil)))

(defn observe!
  "Execute f exactly once; logging cannot turn a successful write into failure."
  [operation initial f]
  (let [started (System/nanoTime)]
    (binding [*context* (atom (safe-context initial))]
      (try
        (let [result (f)]
          (emit! operation @*context* result started false)
          result)
        (catch Throwable failure
          (emit! operation @*context* nil started true)
          (throw failure))))))

(defn observe-phase!
  "Wrap actual verification or commit work, never a derived terminal state.
   Inherit only safe id/route context; the outer boundary owns mission state.
   A direct executor call has no mission id unless its caller bound one."
  [phase f]
  (let [started (System/nanoTime)
        prior (try (dissoc (events/mission-fields (when *context* @*context*))
                     :mission_state)
                   (catch Throwable _ {}))]
    (try
      (let [result (f)]
        (emit! phase prior result started false)
        result)
      (catch Throwable failure
        (emit! phase prior nil started true)
        (throw failure)))))

(def provider-error-types
  #{"provider-rate-limited" "provider-unavailable" "http-error" "timeout"
    "provider-error" "provider-refusal" "model-mismatch" "upstream-mismatch"
    "invalid-response" "invalid-choices" "invalid-message" "output-length"
    "nonterminal-output" "empty-content" "response-too-large" "secret-in-response"
    "redirect-refused" "transport-refused" "transport-or-response-error"})

(defn finite-nonnegative [value]
  (when (and (number? value) (Double/isFinite (double value)) (<= 0 value))
    (double value)))

(defn provider-fallback-event
  "Project only a returned, actually dispatched provider fallback receipt.
   This is not native-tool fallback, planned routing, or a mission state change."
  [prior candidate]
  (let [attempts (:attempts candidate)]
    (when (and (vector? attempts) (= 2 (count attempts)))
      (let [[primary fallback] attempts
            eligible {429 "provider-rate-limited" 503 "provider-unavailable"}]
        (when (and (= "openrouter-cerebras" (:route primary))
                   (true? (:request_started primary))
                   (contains? eligible (:http_status primary))
                   (= (get eligible (:http_status primary)) (:error_type primary))
                   (= "groq" (:route fallback)) (true? (:request_started fallback)))
          (let [attested? (and (= "openai/gpt-oss-120b" (:model fallback))
                               (= "Groq" (:upstream fallback)))
                ok? (and attested? (true? (:usable fallback)))
                seconds (finite-nonnegative (:request_wall_s fallback))
                millis (when seconds (* 1000.0 seconds))
                cost (when (= "provider-reported" (:cost_source fallback))
                       (finite-nonnegative (:cost_usd fallback)))
                token (fn [v] (when (and (integer? v) (<= 0 v Long/MAX_VALUE)) (long v)))]
            (merge (dissoc (events/mission-fields prior) :mission_state :provider :model
                     :upstream :cost_source)
                   {:kind "mission-provider-fallback" :tool "mission" :provider "groq"
                    :ok ok?
                    :error_type (when-not ok?
                                  (cond (provider-error-types (:error_type fallback)) (:error_type fallback)
                                        (true? (:usable fallback)) "provider-identity-unverified"
                                        :else "provider-fallback-refused"))
                    :wall_ms (when (and millis (Double/isFinite millis) (<= millis Long/MAX_VALUE)) millis)
                    :prompt_tokens (token (:prompt_tokens fallback))
                    :completion_tokens (token (:completion_tokens fallback))
                    :reasoning_tokens (token (:reasoning_tokens fallback))
                    :cost_usd cost :cost_source (when cost "provider-reported")}
                   (when attested? {:model "openai/gpt-oss-120b" :upstream "Groq"}))))))))

(defn record-provider-fallback! [candidate]
  (try
    (when-let [event (provider-fallback-event (when *context* @*context*) candidate)]
      (events/record! event))
    (catch Throwable _ nil)))
