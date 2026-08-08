(ns clj-surgeon.mcp-tool
  (:require
   [cheshire.core :as json]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-workspace :as workspace]
   [clojure.java.io :as io])
  (:import
   (java.nio.file Files Path)
   (java.util UUID)))

(def tool-description
  (str
    "Apply one failure-atomic Clojure transaction. If inspect_clojure returned "
    "basis and next_call, preserve workspace_root, basis, site IDs, and verify; "
    "fill every decision and submit once. Otherwise, use changes for two or more "
    "exact replacements or several files. Each changes item contains id, files, "
    "expect, exactly one of forms or owner, and exactly one action: replace, "
    "insert_before, insert_after, rename_binding, or assoc_entry. Exact replacement, "
    "insertion, and assoc_entry "
    "items also contain find. Insertion actions contain one or more complete "
    "form strings and refuse comment-bearing gaps. For named top-level "
    "def or defn owners, use forms: [name]. owner is only for the namespace form "
    "and must be {kind: namespace, name: ns-name}; never pass owner as a string. "
    "find and replace must each contain one complete Clojure form. Example item: "
    "{id: status, files: [src/app.clj], forms: [render], find: :old, "
    "replace: :new, expect: {matches: 1, each_form: 1}}. "
    "For a local rename that must preserve a destructured data key, use forms plus "
    "rename_binding: {from: sort-by, to: sort-field, preserve_external_key: true}; "
    "matches counts the binding and its resolved local usages. "
    "To add one key/value to logically equal maps while preserving comments, use "
    "find with assoc_entry: {key: :status, value: :ready}. "
    "Top-level expect contains changes, edits, and files. Any mismatch refuses "
    "the whole request. Success parses and reads back every file and publishes "
    "an inverse receipt. verification_complete=true is terminal. Use native "
    "patching for prose or one arbitrary text edit."))

(def ^:private positive-integer-schema
  {:type "integer" :minimum 1})

(def explicit-change-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "changes"
    {:type "array"
     :minItems 1
     :description "Complete structural changes. IDs must be unique."
     :items
     {:type "object"
      :additionalProperties false
      :properties
      {"id" {:type "string" :minLength 1
             :description "Stable diagnostic label, for example body-class."}
       "files" {:type "array" :minItems 1
                :description "Project-relative .clj, .cljs, or .cljc paths. No .. or absolute paths."
                :items {:type "string" :minLength 1}}
       "forms" {:type "array" :minItems 1
                :description "Exact named top-level owner forms."
                :items {:type "string" :minLength 1}}
       "owner" {:type "object"
                :additionalProperties false
                :description "Exact non-Var owner scope."
                :properties
                {"kind" {:type "string" :enum ["namespace"]}
                 "name" {:type "string" :minLength 1}}
                :required ["kind" "name"]}
       "find" {:type "string" :minLength 1
               :description "Exactly one Clojure form in its required source spelling."}
       "inside" {:type "string" :minLength 1
                 :description "assoc_entry only. Restrict the map to this complete semantic ancestor; comments do not change identity."}
       "replace" {:type "string" :minLength 1
                  :description "Replacement action: exactly one Clojure form. Source spelling is preserved."}
       "insert_before" {:type "array" :minItems 1
                        :description "Insertion action: one or more complete Clojure forms to insert before every selected sibling. Comment-bearing gaps refuse."
                        :items {:type "string" :minLength 1}}
       "insert_after" {:type "array" :minItems 1
                       :description "Insertion action: one or more complete Clojure forms to insert after every selected sibling. Comment-bearing gaps refuse."
                       :items {:type "string" :minLength 1}}
       "rename_binding"
       {:type "object"
        :additionalProperties false
        :description "Binding-aware local rename. Preserves :keys data keys and renames only resolved local usages."
        :properties
        {"from" {:type "string" :minLength 1
                 :description "Existing unqualified local binding name."}
         "to" {:type "string" :minLength 1
               :description "New unqualified local binding name."}
         "preserve_external_key"
         {:type "boolean"
          :enum [true]
          :description "Required true. Keep external destructuring keys unchanged."}}
        :required ["from" "to" "preserve_external_key"]}
       "assoc_entry"
       {:type "object"
        :additionalProperties false
        :description "Insert one key/value into structurally equal maps while preserving their existing comments and source spelling."
        :properties
        {"key" {:type "string" :minLength 1
                :description "Exactly one Clojure map key form."}
         "value" {:type "string" :minLength 1
                  :description "Exactly one Clojure value form."}}
        :required ["key" "value"]}
       "expect" {:type "object"
                 :additionalProperties false
                 :properties
                 {"matches" (assoc positive-integer-schema
                                   :description "Required total matches for this change.")
                  "each_form" (assoc positive-integer-schema
                                     :description "Optional required matches in every named owner.")
                  "each_file" (assoc positive-integer-schema
                                     :description "Optional required matches in every named file.")}
                 :required ["matches"]}}
      :required ["id" "files" "expect"]
      :allOf
      [{:oneOf [{:required ["forms"]}
                {:required ["owner"]}]}
       {:oneOf
        [{:required ["find" "replace"]
          :not {:anyOf [{:required ["insert_before"]}
                        {:required ["insert_after"]}
                        {:required ["rename_binding"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}
         {:required ["find" "insert_before"]
          :not {:anyOf [{:required ["replace"]}
                        {:required ["insert_after"]}
                        {:required ["rename_binding"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}
         {:required ["find" "insert_after"]
          :not {:anyOf [{:required ["replace"]}
                        {:required ["insert_before"]}
                        {:required ["rename_binding"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}
         {:required ["forms" "rename_binding"]
          :not {:anyOf [{:required ["find"]}
                        {:required ["owner"]}
                        {:required ["replace"]}
                        {:required ["insert_before"]}
                        {:required ["insert_after"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}
         {:required ["find" "assoc_entry"]
          :not {:anyOf [{:required ["replace"]}
                        {:required ["insert_before"]}
                        {:required ["insert_after"]}
                        {:required ["rename_binding"]}]}}]}]}}
    "expect"
    {:type "object"
     :additionalProperties false
     :description "Aggregate transaction cardinality. All three counts are required."
     :properties
     {"changes" (assoc positive-integer-schema :description "Number of change objects.")
      "edits" (assoc positive-integer-schema :description "Total exact replacements.")
      "files" (assoc positive-integer-schema :description "Total files that must change.")}
     :required ["changes" "edits" "files"]}}
   :required ["changes" "expect"]})

(def basis-change-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "basis" {:type "string" :pattern "^cb-"
             :description "Opaque basis returned by inspect_clojure prepare-change."}
    "decisions"
    {:type "array" :minItems 1
     :description "Exactly one keep, owner replacement, whole-site delete, or compact nested edit per site."
     :items
     {:type "object"
      :additionalProperties false
      :properties
      {"site" {:type "string" :minLength 1}
       "keep" {:type "boolean" :const true}
       "replace" {:type "string" :minLength 1}
       "delete" {:type "boolean" :const true
                 :description "Delete the prepared owner and its contiguous leading comment block."}
       "edit"
       {:type "object"
        :additionalProperties false
        :description "Compile one exact nested replacement or deletion inside retained source."
        :properties
        {"find" {:type "string" :minLength 1
                 :description "Exactly one structural Clojure form."}
         "replace" {:type "string" :minLength 1
                    :description "Exactly one replacement form."}
         "delete" {:type "boolean" :const true
                   :description "Delete the match and its contiguous leading comment block."}}
        :required ["find"]
        :oneOf [{:required ["replace"]} {:required ["delete"]}]}}
      :required ["site"]
      :oneOf [{:required ["keep"]}
              {:required ["replace"]}
              {:required ["delete"]}
              {:required ["edit"]}]}}
    "verify" {:type "string" :enum ["fast" "full"] :default "fast"
              :description "Basis route only. Omit when changes is present."}}
   :required ["basis" "decisions"]})

(def clj-change-schema
  {:type "object"
   :additionalProperties false
   :properties (merge (:properties basis-change-schema)
                      (:properties explicit-change-schema))
   :oneOf
   [{:required ["basis" "decisions"]
     :not {:anyOf [{:required ["changes"]} {:required ["expect"]}]}}
    {:required ["changes" "expect"]
     :not {:anyOf [{:required ["basis"]}
                   {:required ["decisions"]}
                   {:required ["verify"]}]}}]})

(def clj-change-output-schema
  {:type "object"
   :properties {"ok" {:type "boolean"}}
   :required ["ok"]})

(def ^:private runtime-config runtime/tool-config)

(defn init!
  "Set the live tool configuration. Passing nil disarms the handler."
  [config]
  (let [configured (when config
                     (assoc config :workspace-router
                            (workspace/router config)))]
    (reset! runtime-config configured)
    (inspect-tool/init! configured)))

(defn- real-root
  ^Path [root]
  (mcp-paths/real-root root))

(defn- resolve-source-path
  [^Path root relative]
  (mcp-paths/resolve-source-path root relative))

(defn- resolve-transaction-paths
  [project-root spec]
  (loop [changes (:changes spec)
         resolved []]
    (if-let [change (first changes)]
      (let [paths (mapv #(resolve-source-path project-root %) (:in change))
            refusal (first (remove :ok paths))]
        (if refusal
          refusal
          (recur (next changes)
                 (conj resolved
                       (assoc change :in (mapv :path paths))))))
      {:ok true :spec (assoc spec :changes resolved)})))

(defn- default-receipt-dir
  [project-root]
  (workspace/receipt-dir project-root))

(defn- delete-empty-dir!
  [directory created?]
  (when created?
    (try
      (Files/deleteIfExists (.toPath (io/file directory)))
      (catch Exception _ nil))))

(defn- elapsed-ms
  [started-ns]
  (/ (double (- (System/nanoTime) started-ns)) 1000000.0))

(defn- timed
  [f]
  (let [started (System/nanoTime)
        result (f)]
    [result (elapsed-ms started)]))

(defn- record-result!
  [telemetry-state request response total-start timings]
  (when telemetry-state
    (telemetry/record-call!
      telemetry-state request response
      (assoc timings :total_ms (elapsed-ms total-start))))
  response)

(defn- execute-request-in-context!
  "Validate, confine, and execute one typed request through the loaded kernel."
  [{:keys [project-root receipt-dir telemetry] :as config} params]
  (let [config (if-let [profiles-fn (:verification-profiles-fn config)]
                 (assoc config :verification-profiles (profiles-fn))
                 config)
        normalized-params (json/parse-string (json/generate-string params) true)
        basis? (string? (:basis normalized-params))
        total-start (System/nanoTime)
        [validated validation-ms]
        (timed #(if basis?
                  (change-buffer/validate-basis-request normalized-params)
                  (contract/validate-tool-params params)))]
    (if basis?
      (record-result!
        telemetry params
        (if (:ok validated)
          (change-buffer/apply-basis! config normalized-params)
          validated)
        total-start {:validation_ms validation-ms})
      (if-not (:ok validated)
        (record-result! telemetry params (contract/normalize-refusal validated)
                        total-start {:validation_ms validation-ms})
        (try
          (let [[prepared confinement-ms]
                (timed
                  #(let [root (real-root project-root)
                         translated
                         (contract/tool-params->transaction (:params validated))]
                     {:root root
                      :resolved (resolve-transaction-paths root translated)}))
                {:keys [root resolved]} prepared]
            (if-not (:ok resolved)
              (record-result! telemetry params resolved total-start
                              {:validation_ms validation-ms
                               :confinement_ms confinement-ms})
              (let [directory (str (or receipt-dir (default-receipt-dir project-root)))
                    directory-file (io/file directory)
                    existed? (.exists directory-file)
                    _ (.mkdirs directory-file)
                    receipt (str (io/file directory
                                          (str (UUID/randomUUID) ".edn")))
                    [result kernel-ms]
                    (timed #(transaction/execute-change!
                              {:spec (:spec resolved) :receipt-out receipt}))
                    classified (contract/classify-kernel-result
                                 (.toString root) result)]
                (when-not (:ok classified)
                  (delete-empty-dir! directory (not existed?)))
                (record-result! telemetry params classified total-start
                                {:validation_ms validation-ms
                                 :confinement_ms confinement-ms
                                 :kernel_ms kernel-ms}))))
          (catch Exception error
            (record-result!
              telemetry params
              {:ok false
               :error_type "mcp-adapter-failure"
               :error (.getMessage error)
               :source_unchanged true
               :remedy "Correct the project root or request and call apply_clojure_changes once."}
              total-start {:validation_ms validation-ms})))))))

(defn execute-request!
  "Route one request to a canonical workspace context, then execute it."
  [config params]
  (let [normalized (json/parse-string (json/generate-string params) true)
        explicit-root? (contains? normalized :workspace_root)]
    (if-not explicit-root?
      (let [result (execute-request-in-context! config normalized)
            resolved (workspace/canonical-root (:project-root config))]
        (cond-> result
          (:ok resolved) (assoc :workspace_root (:workspace-root resolved))))
      (let [workspace-router (or (:workspace-router config)
                                 (workspace/router config))
            routed (workspace/resolve-request workspace-router normalized)]
        (if-not (:ok routed)
          routed
          (assoc (execute-request-in-context! (:config routed) (:params routed))
                 :workspace_root (:workspace-root routed)))))))

(defn concise-summary
  "Render compact visible content; the full receipt remains structuredContent."
  [result]
  (if (:ok result)
    (format (str "apply_clojure_changes\n"
                 "  %s edits · %s files\n\n"
                 "✓ atomic commit complete\n"
                 "✓ written bytes read back and verified\n"
                 "✓ terminal evidence · verification_complete=true · next action none")
            (or (:edits result) (:match-count result) 0)
            (or (:files result) (:changed-file-count result) 0))
    (let [reason (or (:reason result) (:error-type result)
                     (:error_type result) "unknown-error")
          reason (if (keyword? reason) (name reason) reason)
          path (or (:path result) (:error-path result) (:error_path result))
          change-index (or (:change-index result) (:change_index result))
          change-id (or (:change-id result) (:change_id result))
          field (:field result)
          change-line (when (or (some? change-index) change-id field)
                        (format "  change %s%s%s\n"
                                (if (some? change-index) change-index "unknown")
                                (if change-id (str " · " change-id) "")
                                (if field (str " · field " field) "")))
          source-safe? (or (:source-unchanged result)
                           (:source_unchanged result)
                           (:rolled-back result))]
      (format (str "apply_clojure_changes\n"
                   "  refused · %s%s\n"
                   "%s\n"
                   "%s\n"
                   "→ %s")
              reason
              (if path (str " at " (pr-str path)) "")
              (or change-line "")
              (if source-safe?
                "✓ source unchanged"
                "⚠ source state requires structured receipt review")
              (or (:remedy result) (:next_action result)
                  "Correct the request and retry once.")))))

(defn handle-clj-change
  "clojure-mcp callback handler. Kept as a Var for live nREPL redefinition."
  [_exchange params callback]
  (let [result (if-let [config @runtime-config]
                 (execute-request! config params)
                 {:ok false
                  :error_type "server-not-initialized"
                  :error "apply_clojure_changes server is not initialized"
                  :source_unchanged true
                  :remedy "Restart the configured clj-surgeon MCP server."})
        body (json/generate-string result)
        summary (concise-summary result)]
    (callback [summary] (not (:ok result)) result)
    body))

(def clj-change-tool
  {:id :clj-change
   :name "apply_clojure_changes"
   :description tool-description
   :schema clj-change-schema
   :output-schema clj-change-output-schema
   :structured? true
   :tool-fn #'handle-clj-change})

(defn all-tools
  []
  [inspect-tool/inspect-tool clj-change-tool])
