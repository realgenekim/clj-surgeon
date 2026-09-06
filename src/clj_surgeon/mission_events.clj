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
  (let [failed? (or threw? (false? (:ok result))
                    (#{:blocked :failed} (:state result))
                    (false? (get-in result [:receipt :committed])))
        error (scalar-name (or (:error_type result) (:error-type result)
                               (get-in result [:decision :error_type])
                               (get-in result [:receipt :error-type])
                               (get-in result [:receipt :error_type])))
        rung (or (:condition result) (get-in result [:decision :evidence :condition]))]
    (merge prior (context result)
           {:kind (get {"propose" "mission-plan" "apply" "mission-apply"
                        "undo" "mission-undo"} operation "mission-boundary")
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
