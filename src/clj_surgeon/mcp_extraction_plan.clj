(ns clj-surgeon.mcp-extraction-plan
  "Bounded, non-mutating extraction planning over the production compiler."
  (:require
   [clj-surgeon.extract :as extract]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-workspace-sources :as workspace-sources]))

(def private-plan-fields
  #{:_form-texts :_new-file-content :_source :_source-hash
    :_source-referred-forms})

(def allowed-request-fields
  #{:workspace_root :mode :file :to :forms :require_policy})

(defn- refusal
  [error-type error & [data]]
  (merge {:ok false
          :operation "inspect_clojure"
          :mode "plan-extraction"
          :error_type (name error-type)
          :error error
          :read_complete false
          :source_unchanged true
          :next_action "correct_request"}
         data))

(defn validate-request
  [params]
  (let [unknown (vec (sort (remove allowed-request-fields (keys params))))]
    (cond
      (seq unknown)
      (refusal :unknown-fields "Extraction plan contains unknown fields"
               {:unknown_fields unknown})

      (not= "plan-extraction" (:mode params))
      (refusal :invalid-mode "mode must be plan-extraction")

      (not (string? (:file params)))
      (refusal :invalid-extraction-file "file must be a project-relative source path")

      (not (string? (:to params)))
      (refusal :invalid-extraction-target "to must be a project-relative absent source path")

      (or (not (vector? (:forms params)))
          (empty? (:forms params))
          (not-every? string? (:forms params))
          (not= (count (:forms params)) (count (distinct (:forms params)))))
      (refusal :invalid-extraction-forms "forms must contain distinct form names")

      (not (#{"minimal" "copy-all"} (:require_policy params)))
      (refusal :invalid-require-policy "require_policy must be minimal or copy-all")

      :else
      {:ok true})))

(defn- public-reference
  [relative-paths reference]
  (cond-> reference
    (:file reference) (update :file #(get relative-paths % %))))

;; @spec MCP-OP-PLAN-001
;; @spec MCP-OP-PLAN-002
;; @spec MCP-OP-PLAN-003
(defn compile-plan-response
  [{:keys [workspace-root file to source-path target-path source forms target-ns
           workspace-sources relative-paths require-policy]}]
  (try
    (let [compiled
          (extract/compile-plan
            {:file source-path
             :source source
             :forms forms
             :public-forms []
             :to target-path
             :target-ns target-ns
             :workspace-sources workspace-sources
             :require-policy require-policy})]
      (if (:error compiled)
        (refusal :extraction-plan-refused (:error compiled))
        (let [callers (mapv #(get relative-paths % %) (:callers-to-review compiled))
              quoted (mapv #(public-reference relative-paths %)
                           (:quoted-var-references compiled))
              source-hash (:_source-hash compiled)
              public-plan (-> (apply dissoc compiled private-plan-fields)
                              (assoc :file file
                                     :to to
                                     :callers-to-review callers
                                     :quoted-var-references quoted))]
          {:ok true
           :operation "inspect_clojure"
           :mode "plan-extraction"
           :plan public-plan
           :source_hash source-hash
           :evidence_counts
           {:caller_candidates {:returned (count callers)
                                :omitted 0
                                :truncated false}
            :quoted_var_references {:returned (count quoted)
                                    :omitted 0
                                    :truncated false}}
           :next_call
           {:workspace_root workspace-root
            :verify "fast"
            :extraction
            {:file file
             :to to
             :forms forms
             :public_forms (:required-public-forms compiled)
             :require_policy (name require-policy)
             :source_hash source-hash
             :caller_changes []
             :ignored_caller_files []}}
           :read_complete true
           :source_unchanged true
           :next_action "fill_caller_decisions_then_apply_once"})))
    (catch Exception error
      (refusal :extraction-plan-failed (.getMessage error)))))

(defn plan!
  [{:keys [project-root]} params]
  (let [validation (validate-request params)]
    (if-not (:ok validation)
      validation
      (let [root (mcp-paths/real-root project-root)
            source-result (mcp-paths/resolve-source-path root (:file params))
            target-result (mcp-paths/resolve-new-source-path root (:to params))
            path-refusal (first (remove :ok [source-result target-result]))]
        (if path-refusal
          (merge path-refusal
                 {:operation "inspect_clojure"
                  :mode "plan-extraction"
                  :read_complete false
                  :source_unchanged true
                  :next_action "correct_request"})
          (let [sources (workspace-sources/read-all root)
                source-path (str (:path source-result))
                target-path (str (:path target-result))]
            (compile-plan-response
              {:workspace-root (str root)
               :file (:file params)
               :to (:to params)
               :source-path source-path
               :target-path target-path
               :source (get sources source-path)
               :forms (:forms params)
               :target-ns (extract/file-path->ns-name
                            target-path ["src" "test" "dev"])
               :workspace-sources sources
               :relative-paths (workspace-sources/relative-paths root sources)
               :require-policy (keyword (:require_policy params))})))))))
