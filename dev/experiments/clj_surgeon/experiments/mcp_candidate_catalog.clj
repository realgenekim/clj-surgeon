(ns clj-surgeon.experiments.mcp-candidate-catalog
  "Isolated MCP catalog projections for the mutation-tool naming experiment."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]))

(def supported-catalogs [:A :L :C :M :N])

(def mutation-tool-ids
  #{:edit-clojure
    :clj-change
    :candidate-extract-clojure
    :candidate-apply-clojure-plan})

(def deferred-factorials
  [{:id :complete-edit-exact-verification
    :catalogs [:M :N]
    :hypothesis
    (str
      "Allow edit_clojure to select the project-owned exact verifier for a "
      "complete edit decision. This can preserve the editor chord when the "
      "verification gate must participate in rollback.")
    :status :not-implemented
    :reason "Requires a product contract change, not a catalog projection."}])

(def compact-description
  (str
    "Commit one complete Clojure edit decision. Supply exact old and new "
    "forms, bounded computed programs, or exact owner deletions. Surgeon "
    "compiles all items against one frozen snapshot and commits them "
    "atomically. Put a computed relation in programs when it must commit with "
    "literal edits or deletions. Use transform_clojure only for one standalone "
    "computed preview or commit. Use the semantic-plan tool when Surgeon must "
    "prepare or derive semantic change facts, or when project verification "
    "must participate in rollback."))

(def plan-description
  (str
    "Compile and apply one semantic Clojure change plan. Use a prepared "
    "basis, an extraction request, or a change that requires project "
    "verification with rollback. Surgeon refuses unresolved decisions "
    "before mutation. Use edit_clojure when the request already contains "
    "every edit and guard."))

(def extraction-description
  (str
    "Extract exact named Clojure owners into one new namespace. Supply the "
    "source file, destination, ordered owners, require policy, and every "
    "known caller decision. When the request is complete, Surgeon compiles, "
    "commits, and verifies the extraction in this call. Do not request a "
    "separate public plan. Surgeon derives mechanical visibility and exact "
    "counts from one frozen workspace snapshot. Only a genuine unresolved "
    "caller decision refuses before mutation with a completed frozen plan and "
    "an exact next call."))

(def basis-plan-description
  (str
    "Apply one retained Clojure change basis after every genuine decision is "
    "filled. Preserve workspace_root, basis, site IDs, decisions, and verify "
    "from the inspect_clojure next call. Surgeon refuses stale source or an "
    "unresolved decision before mutation. Use extract_clojure for extraction, "
    "including a mechanically complete one-call extraction."))

(def explicit-edit-description
  (str
    "Commit one complete Clojure edit decision. Supply compact exact edits, "
    "bounded computed programs, exact owner deletions, or fully specified "
    "structural changes with exact aggregate counts. Surgeon compiles all "
    "items against one frozen snapshot and commits them atomically. Put a "
    "computed relation here when it must commit with a literal edit or "
    "deletion. Use transform_clojure only for one standalone computed preview "
    "or commit."))

(def transform-description
  (str
    "Preview or commit one standalone bounded SCI transformation. Use this "
    "tool when the request contains one computed relation and no literal edit "
    "or owner deletion. Put a computed relation in edit_clojure programs when "
    "all changes must commit as one atomic batch."))

(def apply-edits-description
  (str
    "Apply one complete Clojure edit decision stated as data. Supply exact "
    "old and new forms, fully specified structural changes, bounded computed "
    "programs, or exact owner deletions. Surgeon compiles all items against "
    "one frozen snapshot and commits them atomically. Use "
    "run_clojure_transform only for one standalone computed rule."))

(defn normalize-catalog
  "Return one supported catalog keyword or refuse unsupported input."
  [catalog]
  (let [candidate (cond
                    (keyword? catalog) catalog
                    (string? catalog) (keyword catalog)
                    :else catalog)]
    (if (some #{candidate} supported-catalogs)
      candidate
      (throw (ex-info "Unsupported MCP candidate catalog"
                      {:catalog catalog
                       :supported supported-catalogs})))))

(defn- tool-by-id
  [tools id]
  (or (some #(when (= id (:id %)) %) tools)
      (throw (ex-info "Canonical MCP tool is absent"
                      {:id id
                       :available (mapv :id tools)}))))

(defn- project-tool
  [tool name description]
  (assoc tool :name name :description description))

(defn- restricted-schema
  [schema property-names branches]
  (assoc (select-keys schema [:type :additionalProperties])
         :properties (select-keys (:properties schema) property-names)
         :oneOf (vec branches)))

(defn- split-catalog-tools
  [base-tools]
  (let [compact (tool-by-id base-tools :edit-clojure)
        change (tool-by-id base-tools :clj-change)
        schema (:schema change)
        [basis-branch explicit-branch compact-branch extraction-branch]
        (:oneOf schema)
        edit-schema
        (restricted-schema
          schema
          ["workspace_root" "edits" "programs" "delete_owners"
           "changes" "expect"]
          [compact-branch explicit-branch])
        extraction-schema
        (restricted-schema
          schema
          ["workspace_root" "extraction" "verify"]
          [extraction-branch])
        basis-schema
        (restricted-schema
          schema
          ["workspace_root" "basis" "decisions" "verify"]
          [basis-branch])
        edit-tool (assoc compact
                         :description explicit-edit-description
                         :schema edit-schema)
        extraction-tool (assoc change
                               :id :candidate-extract-clojure
                               :name "extract_clojure"
                               :description extraction-description
                               :schema extraction-schema)
        plan-tool (assoc change
                         :id :candidate-apply-clojure-plan
                         :name "apply_clojure_plan"
                         :description basis-plan-description
                         :schema basis-schema)]
    (->> base-tools
         (mapcat
           (fn [tool]
             (case (:id tool)
               :clj-change []
               :edit-clojure [edit-tool extraction-tool plan-tool]
               :transform-clojure [(assoc tool :description transform-description)]
               [tool])))
         vec)))

(defn catalog-tools
  "Project one candidate catalog over canonical public tool entries.

  The projection changes only public names, descriptions, and schemas. Every
  projected mutation tool retains the canonical handler Var and output schema."
  ([catalog]
   (catalog-tools catalog (mcp-server/public-tool-registry)))
  ([catalog base-tools]
   (let [catalog (normalize-catalog catalog)]
     (if (#{:M :N} catalog)
       (cond->> (split-catalog-tools base-tools)
         (= :N catalog)
         (mapv (fn [tool]
                 (case (:id tool)
                   :edit-clojure
                   (project-tool tool "apply_clojure_edits"
                                 apply-edits-description)

                   :transform-clojure
                   (project-tool tool "run_clojure_transform"
                                 transform-description)

                   tool))))
       (mapv
         (fn [tool]
           (case (:id tool)
             :edit-clojure
             (project-tool tool "edit_clojure" compact-description)

             :clj-change
             (case catalog
               :A (project-tool tool "apply_clojure_changes" plan-description)
               :L (project-tool tool "apply_clojure_plan" plan-description)
               :C (project-tool tool "refactor_clojure" plan-description))

             :transform-clojure
             (assoc tool :description transform-description)

             tool))
         base-tools)))))

(defn schema-characters
  "Return serialized input-schema characters for each tool and the catalog."
  [tools]
  (let [by-tool (into (sorted-map)
                      (map (fn [{:keys [name schema]}]
                             [name (count (json/generate-string schema))]))
                      tools)]
    {:by-tool by-tool
     :total (reduce + (vals by-tool))
     :mutation-total
     (reduce +
             (for [{:keys [id name]} tools
                   :when (mutation-tool-ids id)]
               (get by-tool name)))}))

(defn catalog-report
  "Return no-model catalog names, schema sizes, and handler identities."
  [catalog]
  (let [base (mcp-server/public-tool-registry)
        tools (catalog-tools catalog base)]
    {:catalog (normalize-catalog catalog)
     :names (mapv :name tools)
     :schema-characters (schema-characters tools)
     :deferred-factorials (if (#{:M :N} (normalize-catalog catalog))
                            deferred-factorials
                            [])
     :handlers
     (mapv (fn [{:keys [id name tool-fn]}]
             {:id id
              :name name
              :handler-var (str tool-fn)})
           tools)}))

(defn start
  "Start one isolated HTTP MCP server with a projected candidate catalog.

  This experiment supports only the full tool profile. It does not install,
  reload, or change the production dispatcher."
  [{:keys [catalog tool-profile] :as opts}]
  (when (and tool-profile (not= :full tool-profile))
    (throw (ex-info "Candidate catalogs require the full tool profile"
                    {:tool-profile tool-profile
                     :supported [:full]})))
  (let [catalog (normalize-catalog catalog)
        projected-tools (catalog-tools catalog)
        server-opts (dissoc opts :catalog)]
    (binding [*out* *err*]
      (println "clj-surgeon candidate MCP catalog:" (name catalog)))
    (with-redefs [mcp-tool/all-tools (constantly projected-tools)]
      (http-server/start server-opts))))
