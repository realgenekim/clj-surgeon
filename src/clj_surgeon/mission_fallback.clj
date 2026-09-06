(ns clj-surgeon.mission-fallback
  "Event-only mission reports, shared by Babashka and JVM entrances."
  (:require
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-display :as display]
   [clj-surgeon.telemetry-events :as telemetry-events]))

(defn report!
  "Record an explicit caller report, never a native edit or proof of adoption."
  [{:keys [workspace state-home id reason]}]
  (let [started (System/nanoTime)]
    (cond
      (not (and (string? workspace) (seq workspace))) display/workspace-required
      (not (contains? #{"refusal" "unsupported" "slower-than-native" "user-choice"} reason))
      {:ok false :recorded false :error-type :mission-fallback-reason
       :error "Use --reason refusal|unsupported|slower-than-native|user-choice."}
      (not (and (string? id) (re-matches #"M-[0-9]{1,12}" id)))
      {:ok false :recorded false :error-type :mission-fallback-id
       :error "Supply the saved mission id, for example M-1."}
      :else
      (let [m (mission/read-mission (mission/workspace-state-dir workspace state-home) id)]
        (if (mission/refused? m)
          (assoc m :recorded false)
          (let [event {:kind "mission-fallback" :tool "mission" :ok true
                       :mission_id id :mission_state (when (or (keyword? (:state m)) (string? (:state m)))
                                                       (name (:state m)))
                       :mission_verb "fallback" :fallback_kind "native-tool"
                       :report_basis "user-reported" :fallback_reason reason
                       :wall_ms (/ (- (System/nanoTime) started) 1000000.0)}
                recorded (try (telemetry-events/record! event) (catch Throwable _ nil))]
            (if (map? recorded)
              {:ok true :recorded true :id id :event recorded
               :native_edit_verified false
               :message "User-reported native-tool fallback only; no edit performed or verified. Saved mission state and proof unchanged."}
              {:ok false :recorded false :id id :native_edit_verified false
               :error-type :mission-fallback-record-failed
               :error "Fallback report was not recorded. Saved mission state and proof unchanged."})))))))
