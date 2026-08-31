(ns clj-surgeon.mcp-elaborator-intent
  "Pure authority firewall for the one-hole embedded elaborator."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str])
  (:import
   (java.nio.charset StandardCharsets)
   (java.security MessageDigest)))

(def operation-version "clj-surgeon.edit-clojure-elaboration.v1")
(def minimum-old-body-bytes 1024)
(def maximum-decision-bytes 512)

(defn utf8-bytes
  [value]
  (alength (.getBytes (str value) StandardCharsets/UTF_8)))

(defn sha256
  [value]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (str value) StandardCharsets/UTF_8))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- canonical-value
  [value]
  (cond
    (map? value) (into (sorted-map)
                       (map (fn [[key item]] [(name key) (canonical-value item)]))
                       value)
    (vector? value) (mapv canonical-value value)
    (sequential? value) (mapv canonical-value value)
    :else value))

(defn canonical-json
  [value]
  (json/generate-string (canonical-value value)))

(defn- refusal
  [error-type error]
  {:ok false
   :error_type error-type
   :error error
   :source_unchanged true
   :mutation_attempted false
   :write_authority false
   :ordinary_path_available true})

(defn- project-relative-file?
  [value]
  (and (string? value)
       (not (str/blank? value))
       (not (str/starts-with? value "/"))
       (not-any? #{".."} (str/split value #"/"))))

(defn- canonical-absolute-root?
  [value]
  (and (string? value)
       (str/starts-with? value "/")
       (= value (str (.normalize (java.nio.file.Paths/get value
                                                          (make-array String 0)))))))

(defn- occurrence-count
  [text needle]
  (loop [offset 0
         count 0]
    (let [found (.indexOf ^String text ^String needle (int offset))]
      (if (neg? found)
        count
        (recur (+ found (.length ^String needle)) (inc count))))))

;; @spec MCP-OP-ELAB-002
;; @spec MCP-OP-ELAB-018
(defn validate-request
  "Validate the closed one-hole wall-class request without contacting a model."
  [request]
  (let [top-keys (set (keys request))
        edits (:edits request)
        edit (when (= 1 (count edits)) (first edits))
        elaborate (:elaborate request)
        decision (:decision elaborate)
        old (:from edit)
        old-bytes (when (string? old) (utf8-bytes old))
        decision-bytes (when (string? decision) (utf8-bytes decision))]
    (cond
      (not= top-keys #{:workspace_root :edits :elaborate})
      (refusal "caller-control-forbidden"
               "Elaboration accepts only workspace_root, edits, and elaborate")

      (not= (set (keys elaborate)) #{:decision})
      (refusal "unknown-elaboration-field"
               "Elaborate is closed and accepts only decision")

      (not= 1 (count edits))
      (refusal "exactly-one-edit-required"
               "Elaboration accepts exactly one edit")

      (not= (set (keys edit)) #{:file :within :from :to :matches})
      (refusal "unknown-edit-field"
               "The elaborated edit is a closed file/within/from/to/matches object")

      (some? (:to edit))
      (refusal "mixed-edit-authority"
               "Elaboration requires one null to hole and accepts no literal candidate")

      (not= 1 (:matches edit))
      (refusal "exactly-one-match-required"
               "Elaboration requires matches=1")

      (or (not= #{:form} (set (keys (:within edit))))
          (not (string? (get-in edit [:within :form])))
          (str/blank? (get-in edit [:within :form])))
      (refusal "named-owner-required"
               "Elaboration requires exactly one named within.form owner")

      (not (canonical-absolute-root? (:workspace_root request)))
      (refusal "canonical-workspace-root-required"
               "Elaboration requires a canonical absolute workspace_root")

      (not (project-relative-file? (:file edit)))
      (refusal "project-relative-file-required"
               "Elaboration requires one project-relative file")

      (or (not (string? old)) (str/blank? old))
      (refusal "nonempty-old-body-required"
               "Elaboration requires an exact nonempty from body")

      (< old-bytes minimum-old-body-bytes)
      (refusal "old-body-too-small"
               "The old body must contain at least 1024 UTF-8 bytes")

      (or (not (string? decision)) (str/blank? decision))
      (refusal "nonblank-decision-required"
               "Elaboration requires one nonblank decision")

      (> decision-bytes maximum-decision-bytes)
      (refusal "decision-too-large"
               "The decision may contain at most 512 UTF-8 bytes")

      (> (* 4 decision-bytes) old-bytes)
      (refusal "decision-ratio-exceeded"
               "The decision may contain at most one quarter of the old-body bytes")

      :else
      {:ok true
       :request request
       :old_body_bytes old-bytes
       :decision_bytes decision-bytes
       :wall_class true
       :source_unchanged true
       :write_authority false})))

;; @spec MCP-OP-ELAB-003
;; @spec MCP-OP-ELAB-017
(defn capture-intent
  "Bind caller authority to a read-only capture and project identity-free model input."
  [request capture]
  (let [validated (validate-request request)
        edit (first (:edits request))]
    (cond
      (not (:ok validated)) validated
      (not= (:workspace_root request) (:workspace_root capture))
      (refusal "capture-identity-mismatch" "Workspace capture does not match caller authority")
      (not= (:file edit) (:file capture))
      (refusal "capture-identity-mismatch" "File capture does not match caller authority")
      (not= (get-in edit [:within :form]) (:owner capture))
      (refusal "capture-identity-mismatch" "Owner capture does not match caller authority")
      (not= 1 (occurrence-count (:owner_source capture) (:from edit)))
      (refusal "capture-guard-mismatch"
               "The exact old-body guard must occur once in the captured owner")
      (not (re-matches #"[0-9a-f]{64}" (or (:source_sha256 capture) "")))
      (refusal "capture-hash-required" "Capture requires one SHA-256 source hash")
      :else
      (let [authority {:operation_version operation-version
                       :workspace_root (:workspace_root request)
                       :file (:file edit)
                       :owner (get-in edit [:within :form])
                       :old_body (:from edit)
                       :matches (:matches edit)
                       :decision (get-in request [:elaborate :decision])
                       :source_sha256 (:source_sha256 capture)}]
        {:ok true
         :authority_intent authority
         :intent_sha256 (sha256 (canonical-json authority))
         :model_input {:old_body (:from edit)
                       :decision (get-in request [:elaborate :decision])}
         :source_unchanged true
         :write_authority false}))))

;; @spec MCP-OP-ELAB-005
(defn complete-request
  "Fill only the caller-selected null body and return an ordinary edit request."
  [request replacement]
  (let [validated (validate-request request)]
    (if-not (:ok validated)
      validated
      {:ok true
       :request (-> request
                    (assoc-in [:edits 0 :to] replacement)
                    (dissoc :elaborate))
       :ordinary_operation "edit_clojure"
       :fresh_capture_required true
       :write_authority false})))

;; @spec MCP-OP-ELAB-006
;; @spec MCP-OP-ELAB-018
(defn classify-request
  [request]
  (if (contains? request :elaborate)
    {:path :elaborator :contact_child true}
    {:path :ordinary
     :contact_child false
     :expected_result (when (some #(nil? (:to %)) (:edits request))
                        "ordinary-null-refusal")}))

;; @spec MCP-OP-ELAB-017
(defn identity-falsifier
  []
  {:receipt "b0432c25"
   :observed_failure "wrong-explicit-file-with-correct-reference"
   :may_assert_reference false
   :may_assert_file false
   :may_assert_owner false
   :may_assert_subject_identity false
   :identity_received_from_caller true
   :later_hardening_and_ratification_required true})
