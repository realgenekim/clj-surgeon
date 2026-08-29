(ns extraction-tool-surface
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clj-surgeon.mcp-server :as mcp-server])
  (:import
   (java.security MessageDigest)))

(def candidate-description
  (str
    "Apply one prepared Clojure extraction atomically. Supply workspace_root, "
    "extraction {file, to, forms, require_policy, public_forms, caller_changes, "
    "ignored_caller_files}, and optional verify. The complete request uses one "
    "frozen workspace snapshot. Every discovered caller must be changed or "
    "explicitly ignored. A success formats staged files, verifies, commits, reads "
    "back, and publishes rollback evidence; verification_complete=true and "
    "next_action=none are terminal for this mutation. Any stale hash, ambiguity, "
    "count mismatch, formatter error, or verification failure refuses or rolls "
    "back the whole change."))

(def candidate-schema
  {:type "object"
   :additionalProperties false
   :properties
   (select-keys (:properties mcp-schema/clj-change-schema)
                ["workspace_root" "extraction" "verify"])
   :required ["extraction"]})

(defn production-tool []
  (or (first (filter #(= :clj-change (:id %))
                     (mcp-server/public-tool-registry)))
      (throw (ex-info "apply_clojure_changes is absent from the registry" {}))))

(defn tool-surface [arm]
  (let [base (production-tool)]
    (case arm
      :control (select-keys base [:id :name :description :schema
                                  :output-schema :annotations])
      :treatment (assoc (select-keys base [:id :name :output-schema :annotations])
                        :description candidate-description
                        :schema candidate-schema)
      (throw (ex-info "Unknown extraction surface arm" {:arm arm})))))

(defn sha256 [value]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (json/generate-string value) "UTF-8"))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn surface-report []
  (let [control (tool-surface :control)
        treatment (tool-surface :treatment)
        bytes #(count (.getBytes
                        (str (:description %) (json/generate-string (:schema %)))
                        "UTF-8"))]
    {:control-bytes (bytes control)
     :treatment-bytes (bytes treatment)
     :removed-bytes (- (bytes control) (bytes treatment))
     :removed-fraction (/ (- (bytes control) (bytes treatment))
                          (double (bytes control)))
     :control-sha256 (sha256 control)
     :treatment-sha256 (sha256 treatment)}))
