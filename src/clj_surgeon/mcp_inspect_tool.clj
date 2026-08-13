(ns clj-surgeon.mcp-inspect-tool
  "Imperative MCP shell for one-read project-confined Clojure inspection."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-source-anchor :as source-anchor]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.quoted-var-refs :as quoted-var-refs]
   [clj-surgeon.structural-lens :as structural-lens]))

(def tool-description
  (str
    "Read Clojure structure in one bounded snapshot. Batch all known forms, "
    "outlines, exact structural matches, and X-ray requests. Use "
    "include_source=false on forms requests when only names, ranges, counts, "
    "and hashes are needed; omit it when exact form source is needed. Use "
    "mode=prepare-change when one Var or related Var set names the goal but "
    "exact sites are unknown. Its caller proof unions resolved references with "
    "lossless #'x and (var x) references; every surface site names its authority. "
    "Every returned named form includes a ready-to-use source_anchor for resolve_var_surface; copy it instead of making an unanchored workspace-symbol query. For an unindexed file, use file plus form to "
    "prepare one exact-source definition without claiming references. "
    "scope=definition returns every definition and "
    "reference as one compact surface vector, but attaches source only to the "
    "definition decision. scope=surface makes every site a decision. An "
    "optional label must match ^[a-z][a-z0-9-]{0,39}$ and gives sites readable names such as rename/s01. "
    "Use basis with view=sites and ordered open IDs to reopen exact named forms "
    "from the retained snapshot without file reads. When apply_clojure_changes "
    "returns verification_complete=false, copy its next_call to inspect the "
    "bounded cold verification job; do not block or rerun the edit. Copy "
    "next_call, fill every "
    "decision with keep, one complete named-form replacement, whole-site delete, or one compact "
    "edit, then call apply_clojure_changes once. The whole request refuses on "
    "ambiguity, count, path, parse, or budget failure. read_complete=true is "
    "terminal. Never writes."))

(def ^:private positive-integer-schema {:type "integer" :minimum 1})
(def ^:private non-negative-integer-schema {:type "integer" :minimum 0})
(def ^:private source-file-schema
  {:type "string"
   :minLength 1
   :pattern "^(?!/)(?![A-Za-z]:/)(?!.*(?:^|/)\\.\\.(?:/|$)).*\\.clj[sc]?$"
   :description "Project-relative .clj, .cljs, or .cljc source path."})

(defn- request-base
  [operation properties required]
  {:type "object"
   :additionalProperties false
   :properties
   (merge
     {"id" {:type "string" :minLength 1}
      "operation" {:const operation}
      "file" source-file-schema}
     properties)
   :required (into ["id" "operation" "file"] required)})

(def typed-inspect-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "requests"
    {:type "array"
     :minItems 1
     :maxItems inspect/max-requests
     :description "Ordered all-or-nothing structural read requests with unique IDs."
     :items
     {:oneOf
      [(request-base
         "forms"
         {"forms" {:type "array" :minItems 1 :maxItems inspect/max-forms
                   :uniqueItems true
                   :items {:type "string" :minLength 1}}
          "include_source" {:type "boolean" :default true
                            :description "Set false for metadata-only form reads; line ranges, source character counts, hashes, and source anchors remain, while exact source bodies are omitted. Omit for exact source."}
          "expect" {:type "object" :additionalProperties false
                    :properties {"forms" positive-integer-schema}
                    :required ["forms"]}}
         ["forms" "expect"])
       (request-base "outline" {} [])
       (request-base
         "match"
         {"match" {:type "string" :minLength 1
                   :description "Exactly one structural Clojure form pattern; _ matches one subtree."}
          "inside" {:type "string" :minLength 1}
          "expect" {:type "object" :additionalProperties false
                    :properties {"matches" non-negative-integer-schema}}}
         ["match"])
       (request-base
         "xray"
         {"expression" {:type "string" :minLength 1
                        :description (str
                                       "Existing capability-limited :xray :expr program. "
                                       "Example: (-> (form 'numeric-fields) initializer "
                                       "(expect-count 1) (analyze (fn [[fields]] "
                                       "(count fields))))")}}
         ["expression"])]}}
    "expect"
    {:type "object"
     :additionalProperties false
     :description "Exact aggregate counts. files is the number of distinct file paths across all requests; count repeated paths once."
     :properties
     {"requests" (assoc positive-integer-schema
                        :description "Exact number of request objects.")
      "files" (assoc positive-integer-schema
                     :description "Exact number of distinct request file paths; repeated paths count once.")}
     :required ["requests" "files"]}}
   :required ["requests" "expect"]})

(def prepare-change-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "mode" {:type "string" :const "prepare-change"}
    "scope" {:type "string" :enum ["definition" "surface"]
             :description "Semantic preparation defaults to surface. Exact file/form preparation supports and defaults to definition."}
    "label" {:type "string" :pattern "^[a-z][a-z0-9-]{0,39}$"
             :description "Optional cosmetic site namespace. Must match ^[a-z][a-z0-9-]{0,39}$; for example rename produces rename/s01. Omit for change/s01."}
    "file" (assoc source-file-schema
                  :description "Exact-source route only: project-relative file containing the named form. Use together with form instead of subject/subjects.")
    "form" {:type "string" :minLength 1
            :description "Exact-source route only: one exact named top-level form. Use together with file instead of subject/subjects."}
    "owners" {:type "array" :minItems 1 :uniqueItems true
              :description "Exact-source route only: ordered project-relative file/form owners compiled into one basis without a semantic index."
              :items {:type "object"
                      :additionalProperties false
                      :properties
                      {"file" source-file-schema
                       "form" {:type "string" :minLength 1}}
                      :required ["file" "form"]}}
    "subject" {:type "string" :minLength 3
               :description "One fully qualified Clojure Var: namespace/name."}
    "subjects" {:type "array" :minItems 1 :uniqueItems true
                :items {:type "string" :minLength 3}
                :description "Related fully qualified Vars to resolve as one proof union."}
    "intent" {:type "string" :minLength 1
              :description "One concise semantic change decision."}
    "verify" {:type "string" :enum ["fast" "full"] :default "fast"
              :description "Omit for changed-file verification. Use full only when the user explicitly requests the complete repository suite."}}
   :required ["mode" "intent"]
   :oneOf
   [{:required ["subject"]
     :not {:anyOf [{:required ["subjects"]}
                   {:required ["file"]}
                   {:required ["form"]}]}}
    {:required ["subjects"]
     :not {:anyOf [{:required ["subject"]}
                   {:required ["file"]}
                   {:required ["form"]}]}}
    {:required ["file" "form"]
     :not {:anyOf [{:required ["subject"]}
                   {:required ["subjects"]}
                   {:required ["owners"]}]}}
    {:required ["owners"]
     :not {:anyOf [{:required ["subject"]}
                   {:required ["subjects"]}
                   {:required ["file"]}
                   {:required ["form"]}]}}]})

(def basis-view-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "basis" {:type "string" :minLength 1
             :description "Opaque retained basis from prepare-change."}
    "view" {:type "string" :enum ["sites" "verification"]}
    "open" {:type "array" :minItems 1 :uniqueItems true
            :items {:type "string" :minLength 3}
            :description "Ordered retained site IDs, for example rename/s01."}
    "context" {:type "string" :const "form" :default "form"
               :description "Return the complete named form from the retained snapshot."}}
   :required ["basis" "view" "open"]})

(def verification-job-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "verification_job" {:type "string" :pattern "^verify/.+"}}
   :required ["verification_job" "view"]})

(def inspect-schema
  {:type "object"
   :additionalProperties false
   :properties (merge (:properties typed-inspect-schema)
                      (:properties prepare-change-schema)
                      (:properties basis-view-schema)
                      (:properties verification-job-schema))
   :oneOf [{:required ["requests" "expect"]}
           {:required ["mode" "subject" "intent"]}
           {:required ["mode" "subjects" "intent"]}
           {:required ["mode" "file" "form" "intent"]}
           {:required ["basis" "view" "open"]}
           {:required ["verification_job" "view"]}]})

(def inspect-output-schema
  {:type "object"
   :additionalProperties true
   :properties
   {"ok" {:type "boolean"}
    "operation" {:type "string"}
    "read_complete" {:type "boolean"}
    "request_count" {:type "integer"}
    "file_count" {:type "integer"}
    "results" {:type "array"}
    "file_hashes" {:type "object"}
    "source_character_count" {:type "integer"}
    "next_action" {:type "string"}
    "basis" {:type "string"}
    "surface" {:type "array"}
    "decision-site-ids" {:type "array"}
    "decision-sites" {:type "array"}
    "buffers" {:type "array"}
    "verification_job" {:type "string"}
    "verification_complete" {:type "boolean"}
    "next_call" {:type "object"}}
   :required ["ok" "operation"]
   :anyOf [{:required ["read_complete"]}
           {:required ["basis" "surface" "decision-site-ids"
                       "decision-sites" "next_call"]}
           {:required ["basis" "buffers" "read_complete"]}
           {:required ["verification_job" "verification_complete"]}]})

(def inspect-annotations
  {:title "Inspect Clojure"
   :read-only true
   :destructive false
   :idempotent true
   :open-world false
   :return-direct false})

(def ^:private runtime-config runtime/tool-config)

(def max-public-result-bytes (* 32 1024))

(defn- semantic-init!
  [config]
  ((requiring-resolve 'clj-surgeon.mcp-semantic-client/init!) config))

(defn- local-source-anchors
  [{:keys [project-root read-source project-aliases]} subject]
  (let [root (mcp-paths/real-root project-root)]
    (->> (source-anchor/candidate-relative-files project-root subject)
         (keep
           (fn [relative-file]
             (let [resolved (mcp-paths/resolve-source-path root relative-file)]
               (when (:ok resolved)
                 (let [source ((or read-source slurp) (:path resolved))
                       aliases (or project-aliases
                                   (forms/load-project-aliases (:path resolved)))
                       built (source-anchor/build-source-anchor
                               subject relative-file source aliases)]
                   (when (:ok built)
                     (:source-anchor built)))))))
         vec)))

(defn- confirm-source-anchor
  [provider subject anchor]
  (let [anchored (provider anchor)]
    (cond
      (not (:ok anchored))
      anchored

      (not= "source-anchor" (:resolution anchored))
      {:ok false
       :error-type :semantic-source-anchor-not-confirmed
       :error "cclsp did not confirm source-anchor resolution"
       :subject subject
       :file (:file anchor)
       :source-unchanged true}

      (not= anchor (:source_anchor anchored))
      {:ok false
       :error-type :semantic-source-anchor-mismatch
       :error "cclsp returned a different source anchor"
       :subject subject
       :file (:file anchor)
       :source-unchanged true}

      :else
      anchored)))

(defn resolve-var!
  "Resolve one Var through a candidate-file discovery and an exact source anchor."
  [{:keys [project-root read-source project-aliases semantic-provider]} subject]
  (let [provider
        (or semantic-provider
            (fn [source-anchor]
              ((requiring-resolve 'clj-surgeon.mcp-semantic-client/resolve-var!)
               project-root subject source-anchor)))
        local-anchors (local-source-anchors
                        {:project-root project-root
                         :read-source read-source
                         :project-aliases project-aliases}
                        subject)]
    (cond
      (= 1 (count local-anchors))
      (confirm-source-anchor provider subject (first local-anchors))

      (> (count local-anchors) 1)
      {:ok false
       :error-type :semantic-local-definition-ambiguous
       :error "More than one conventional source file defines the requested Var"
       :subject subject
       :files (mapv :file local-anchors)
       :source-unchanged true}

      :else
      (let [discovery (provider nil)]
        (if-not (:ok discovery)
          discovery
          (let [candidate-file (get-in discovery [:definition :file])
                root (mcp-paths/real-root project-root)
                resolved (when (string? candidate-file)
                           (mcp-paths/resolve-source-path root candidate-file))]
            (cond
              (not (string? candidate-file))
              {:ok false
               :error-type :semantic-candidate-file-missing
               :error "The semantic discovery result did not name a candidate definition file"
               :subject subject
               :source-unchanged true}

              (not (:ok resolved))
              (assoc resolved :subject subject :source-unchanged true)

              :else
              (let [source ((or read-source slurp) (:path resolved))
                    aliases (or project-aliases
                                (forms/load-project-aliases (:path resolved)))
                    built (source-anchor/build-source-anchor
                            subject candidate-file source aliases)]
                (if-not (:ok built)
                  built
                  (confirm-source-anchor provider subject
                                         (:source-anchor built)))))))))))

(defn init!
  "Set the live inspect configuration. Passing nil disarms the handler."
  [config]
  (let [configured (when config
                     (if (:workspace-router config)
                       config
                       (assoc config :workspace-router
                              (workspace/router config))))]
    (when-let [cclsp-url (:cclsp-url configured)]
      (semantic-init! {:url cclsp-url}))
    (reset! runtime-config configured)))

(defn- elapsed-ms
  [started-ns]
  (/ (double (- (System/nanoTime) started-ns)) 1000000.0))

(defn mcp-result-byte-count
  "Measure the complete public MCP result shape, including its text summary."
  [summary result]
  (count
    (.getBytes
      (json/generate-string
        {:content [{:type "text" :text summary}]
         :structuredContent result
         :isError (not (:ok result))})
      "UTF-8")))

(defn prepare-change-summary
  "Render the compact quickfix-style surface without repeating source bodies."
  [result]
  (let [exact-source? (= :exact-source (:authority result))
        surface-lines
        (apply str
               (map (fn [{:keys [id role file form line authority]}]
                      (format "  :%-18s %-10s %s:%s · %s · %s\n"
                              id (name role) file line form (name authority)))
                    (:surface result)))]
    (format
      (str "inspect_clojure · prepare-change\n"
           "  %s surface sites · %s decisions · %s files · basis %s\n\n"
           "%s\n"
           "%s\n"
           "✓ source hashes independently verified\n"
           "✓ decision source attached once in structured content\n"
           "→ fill next_call decisions, then call apply_clojure_changes once")
      (:surface-site-count result)
      (:site-count result)
      (:file-count result)
      (:basis result)
      surface-lines
      (if exact-source?
        "✓ exact named owner · no semantic index required"
        "✓ complete caller surface · resolved references + exact quoted Vars"))))

(defn basis-view-summary
  "Render retained structural buffers without duplicating their source."
  [result]
  (let [buffer-lines
        (apply str
               (map (fn [{:keys [id role file form line]}]
                      (format "  :%-18s %-10s %s:%s · %s\n"
                              id (name role) file line form))
                    (:buffers result)))]
    (format
      (str "inspect_clojure · retained buffers\n"
           "  %s buffers · %s source characters · 0 file reads\n\n"
           "%s\n"
           "✓ rendered from the immutable basis snapshot\n"
           "✓ exact named-form source attached once in structured content\n"
           "→ decide, or open another retained site")
      (:buffer-count result)
      (:source-character-count result)
      buffer-lines)))

(defn verification-job-summary
  "Render one bounded cold-verification job without repeating its full output."
  [result]
  (let [status (:status result)
        terminal? (true? (:verification_complete result))
        passed? (true? (:passed result))]
    (format
      (str "inspect_clojure · cold verification\n"
           "  %s · %s · %.2f ms\n\n"
           "%s\n"
           "→ %s")
      (:verification_job result)
      (name status)
      (double (or (:job_elapsed_ms result) 0.0))
      (cond
        (not terminal?) "… bounded job still running"
        passed? "✓ cold verification passed"
        :else "✗ cold verification failed; edit remains committed and undo receipt is available")
      (if terminal?
        (:next_action result)
        "call next_call once after doing other useful work"))))

(defn enforce-public-result-budget
  "Refuse an oversized public result without returning partial source."
  [summary result]
  (let [required-bytes (mcp-result-byte-count summary result)
        prepare? (= "prepare-change" (:mode result))]
    (if (<= required-bytes max-public-result-bytes)
      result
      (do
        (when (and prepare? (:basis result))
          (change-buffer/discard-basis! (:basis result)))
        {:ok false
         :operation "inspect_clojure"
         :mode (:mode result)
         :error-type (if prepare?
                       :decision-output-budget-exceeded
                       :structural-buffer-output-budget-exceeded)
         :error (if prepare?
                  "The complete decision packet exceeds the public MCP output budget"
                  "The requested structural buffers exceed the public MCP output budget")
         :required (cond-> {:public-result-bytes required-bytes}
                     prepare?
                     (assoc :surface-sites (:surface-site-count result)
                            :decision-sites (:site-count result)
                            :decision-source-characters
                            (:visible-character-count result))
                     (not prepare?)
                     (assoc :buffers (:buffer-count result)
                            :source-characters (:source-character-count result)))
         :limits {:public-result-bytes max-public-result-bytes}
         :source-unchanged true
         :basis-retained (not prepare?)
         :next-action (if prepare?
                        "narrow-decision-surface"
                        "open-fewer-buffers")
         :remedies (if prepare?
                     [{:scope "definition"}
                      {:message "Narrow the subjects or add an exact structural focus."}]
                     [{:message "Open fewer retained site IDs in one call."}])}))))

(defn- inspect-refusal
  [result]
  (let [normalized (inspect/json-data result)]
    (merge
      {:ok false
       :operation "inspect_clojure"
       :read_complete false
       :source_unchanged true
       :next_action "correct_request"}
      normalized)))

(defn capture-snapshots
  "Resolve and read each distinct canonical file once in first-reference order.

  `read-source` is injectable for boundary tests and receives a canonical path."
  [project-root requests read-source expected-file-count]
  (try
    (let [root (mcp-paths/real-root project-root)
          files (vec (distinct (map :file requests)))
          resolved (mapv #(mcp-paths/resolve-source-path root %) files)
          refusal (first (remove :ok resolved))]
      (if refusal
        refusal
        (let [canonical-count (count (distinct (map #(str (:canonical %))
                                                    resolved)))]
          (if-not (= expected-file-count canonical-count)
            {:ok false
             :operation "inspect_clojure"
             :error_type "aggregate-file-expectation-mismatch"
             :error "Declared file count does not match distinct canonical files"
             :expected expected-file-count
             :actual canonical-count
             :read_complete false
             :source_unchanged true
             :next_action "correct_request"}
            (loop [remaining resolved
                   cache {}
                   snapshots (array-map)
                   reads 0]
              (if-let [{:keys [relative path canonical]} (first remaining)]
                (let [cache-key (str canonical)]
                  (if-let [captured (get cache cache-key)]
                    (recur (next remaining) cache
                           (assoc snapshots relative
                                  (assoc captured :file relative))
                           reads)
                    (let [source (read-source path)]
                      (when-not (string? source)
                        (throw
                          (ex-info "Source reader must return a string"
                                   {:error-type :source-read-failed
                                    :file relative})))
                      (let [captured {:file relative
                                      :source source
                                      :hash (structural-lens/source-hash source)}]
                        (recur (next remaining)
                               (assoc cache cache-key captured)
                               (assoc snapshots relative captured)
                               (inc reads))))))
                {:ok true
                 :root root
                 :snapshots snapshots
                 :file_read_count reads}))))))
    (catch Exception error
      {:ok false
       :operation "inspect_clojure"
       :error_type (name (or (:error-type (ex-data error))
                             :source-read-failed))
       :error (.getMessage error)
       :file (:file (ex-data error))
       :read_complete false
       :source_unchanged true
       :next_action "correct_request"})))

(defn- execute-inspect-in-context!
  "Validate, confine, snapshot once, and evaluate one typed inspect request."
  [{:keys [project-root telemetry read-source output-limits semantic-resolver] :as config}
   params]
  (let [started (System/nanoTime)
        normalized-params (json/parse-string (json/generate-string params) true)
        prepare? (= "prepare-change" (:mode normalized-params))
        basis-view? (= "sites" (:view normalized-params))
        verification-job? (= "verification" (:view normalized-params))
        validated (when-not (or prepare? basis-view? verification-job?)
                    (inspect/validate-inspect-params params))
        result
        (assoc
          (cond
            verification-job?
            (assoc
              (cold-verify/status project-root
                                  (:verification_job normalized-params))
              :operation "inspect_clojure"
              :mode "verification-job")

            prepare?
            (change-buffer/prepare-change!
              (assoc config
                     :semantic-resolver (or semantic-resolver
                                            (partial resolve-var! config))
                     :structural-reference-resolver
                     (or (:structural-reference-resolver config)
                         (fn [subjects]
                           (quoted-var-refs/scan-workspace
                             project-root subjects (or read-source slurp)))))
              normalized-params)

            basis-view?
            (change-buffer/open-basis-sites!
              project-root
              (:basis normalized-params)
              (:open normalized-params)
              (or (:context normalized-params) "form"))

            (not (:ok validated))
            (inspect-refusal validated)

            :else
            (let [normalized (:params validated)
                  captured (capture-snapshots
                             project-root (:requests normalized)
                             (or read-source slurp)
                             (get-in normalized [:expect :files]))]
              (if-not (:ok captured)
                (inspect-refusal captured)
                (assoc
                  (inspect/evaluate-snapshots
                    normalized (:snapshots captured)
                    (merge inspect/default-output-limits output-limits))
                  :file_read_count (:file_read_count captured)))))
          :elapsed_ms (elapsed-ms started))]
    (when telemetry
      (telemetry/record-inspect-call!
        telemetry params result {:total_ms (:elapsed_ms result)}))
    result))

(defn execute-inspect!
  "Route one inspect request to a canonical workspace context, then execute it."
  [config params]
  (let [normalized (json/parse-string (json/generate-string params) true)
        explicit-root? (contains? normalized :workspace_root)]
    (if-not explicit-root?
      (let [result (execute-inspect-in-context! config normalized)
            resolved (workspace/canonical-root (:project-root config))]
        (cond-> result
          (:ok resolved) (assoc :workspace_root (:workspace-root resolved))))
      (let [workspace-router (or (:workspace-router config)
                                 (workspace/router config))
            routed (workspace/resolve-request workspace-router normalized)]
        (if-not (:ok routed)
          routed
          (assoc (execute-inspect-in-context! (:config routed) (:params routed))
                 :workspace_root (:workspace-root routed)))))))

(defn handle-inspect
  "Structured clojure-mcp callback handler, retained as a Var for hot reload."
  [_exchange params callback]
  (let [raw-result
        (if-let [config @runtime-config]
          (execute-inspect! config params)
          {:ok false
           :operation "inspect_clojure"
           :error_type "server-not-initialized"
           :error "inspect_clojure server is not initialized"
           :read_complete false
           :source_unchanged true
           :next_action "restart_server"})
        raw-summary
        (cond
          (not (:ok raw-result)) (json/generate-string raw-result)
          (= "prepare-change" (:mode raw-result))
          (prepare-change-summary raw-result)
          (= "basis-view" (:mode raw-result))
          (basis-view-summary raw-result)
          (= "verification-job" (:mode raw-result))
          (verification-job-summary raw-result)
          :else (inspect/concise-summary raw-result))
        result
        (if (and (:ok raw-result)
                 (#{"prepare-change" "basis-view"} (:mode raw-result)))
          (enforce-public-result-budget raw-summary raw-result)
          raw-result)
        summary
        (if (:ok result)
          raw-summary
          (json/generate-string result))]
    (callback [summary] (not (:ok result)) result)
    (json/generate-string result)))

(def inspect-tool
  {:id :inspect-clojure
   :name "inspect_clojure"
   :description tool-description
   :schema inspect-schema
   :output-schema inspect-output-schema
   :annotations inspect-annotations
   :structured? true
   :tool-fn #'handle-inspect})
