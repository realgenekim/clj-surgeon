(ns clj-surgeon.experiments.mcp-candidate-catalog
  "Isolated MCP catalog projections for the mutation-tool naming experiment."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]))

(def supported-catalogs [:A :L :C :M :N :O :P :Q :R :S :T])

(def mutation-tool-ids
  #{:edit-clojure
    :clj-change
    :candidate-extract-clojure
    :candidate-apply-clojure-plan
    :candidate-transform-commit})

(def deferred-factorials
  [{:id :complete-edit-exact-verification
    :catalogs [:M :N :O :P :Q :R :S :T]
    :hypothesis
    (str
      "Allow edit_clojure to select the project-owned exact verifier for a "
      "complete edit decision. This can preserve the editor chord when the "
      "verification gate must participate in rollback.")
    :status :not-implemented
    :reason "Requires a product contract change, not a catalog projection."}])

(def option-m-controls
  [:inspect :edit :extract :plan :transform])

(def option-m-overlap-matrix
  [{:pair [:inspect :edit]
    :classification :intentional-composition
    :boundary "Inspect returns evidence. Edit consumes a complete decision."}
   {:pair [:inspect :extract]
    :classification :defect-risk
    :boundary
    (str
      "A complete extraction must call extract directly. A genuine unknown "
      "must not trigger separate rediscovery planning.")}
   {:pair [:inspect :plan]
    :classification :intentional-composition
    :boundary "Inspect creates the retained basis and exact next call."}
   {:pair [:inspect :transform]
    :classification :orthogonal
    :boundary "Inspect reads source evidence. Transform computes one selection."}
   {:pair [:edit :extract]
    :classification :orthogonal
    :boundary
    (str
      "Edit applies supplied actions. Extract derives namespace movement, "
      "dependencies, visibility, and caller evidence.")}
   {:pair [:edit :plan]
    :classification :orthogonal-authority
    :boundary
    (str
      "Edit receives a complete decision. Plan accepts only a retained basis "
      "and its filled decisions.")}
   {:pair [:edit :transform]
    :classification :defect
    :boundary
    (str
      "A programs-only edit duplicates transform commit=true. The proposed "
      "ratchet reserves standalone programs for transform and allows edit "
      "programs only in a mixed atomic chord.")}
   {:pair [:extract :plan]
    :classification :missing-continuation-seam
    :boundary
    (str
      "Current extraction retries reuse the complete extraction request. A "
      "plan_id plus genuine decisions is not a production input.")}
   {:pair [:extract :transform]
    :classification :orthogonal
    :boundary "Extract moves named owners. Transform rewrites one selection."}
   {:pair [:plan :transform]
    :classification :orthogonal-authority
    :boundary
    "Plan consumes retained decisions. Transform executes a supplied rule."}])

(def option-m-falsifier-cards
  [{:id :standalone-program
    :given "One selection and one bounded SCI rule with no other source action."
    :must-select :transform
    :must-not-select :edit}
   {:id :mixed-computed-chord
    :given
    "One bounded SCI rule that must commit with a literal edit or owner deletion."
    :must-select :edit
    :must-not-select :transform}
   {:id :complete-extraction
    :given "Exact source, destination, owners, and every caller decision."
    :must-select :extract
    :must-not-select :inspect}
   {:id :ambiguous-extraction
    :given "One genuine caller decision is absent."
    :must-select :extract
    :must-produce :pre-write-completed-plan}
   {:id :complete-edit-with-exact-verification
    :given "Every edit is complete and project verification must roll back."
    :factorial :complete-edit-exact-verification
    :must-not-infer :semantic-plan}
   {:id :retained-basis
    :given "inspect_clojure returned one basis and exact next call."
    :must-select :plan
    :must-preserve [:workspace_root :basis :site_ids :decisions :verify]}
   {:id :invented-plan
    :given "No inspect_clojure basis or next call exists."
    :must-not-select :plan
    :must-produce :typed-pre-write-refusal}])

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

(def atomic-chord-description
  (str
    "Commit one atomic Clojure edit chord. Supply literal or structural "
    "actions, optional bounded computed programs, and exact owner deletions. "
    "Surgeon compiles all actions against one frozen snapshot. Use "
    "transform_clojure_with_clojure for a computed solo with one selection "
    "and one bounded SCI rule."))

(def computed-solo-description
  (str
    "Preview or commit one computed Clojure solo. Supply one structural "
    "selection and one bounded SCI rule. Preview is the default. Use "
    "edit_clojure when a computed program must commit atomically with literal "
    "or structural actions or exact owner deletions."))

(def transform-preview-description
  (str
    "Preview one computed Clojure transformation. Supply one structural "
    "selection and one bounded SCI rule. This control cannot commit. Use the "
    "transform commit control after the decision is complete, or use the edit "
    "commit control when the program must commit with other source actions."))

(def transform-commit-description
  (str
    "Commit one standalone computed Clojure transformation. Supply one "
    "structural selection, one bounded SCI rule, and commit=true. Use the "
    "preview control when the decision is incomplete. Use the edit commit "
    "control when this program must commit with other source actions."))

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

(def effect-names
  {:P {:edit "commit_clojure_edits"
       :extract "commit_clojure_extraction"
       :plan "apply_clojure_plan"
       :preview "preview_clojure_transform"
       :transform "commit_clojure_transform"}
   :Q {:edit "clojure.edit.commit"
       :extract "clojure.extract.commit"
       :plan "clojure.plan.apply"
       :preview "clojure.transform.preview"
       :transform "clojure.transform.commit"}
   :R {:edit "edit_clojure_commit"
       :extract "extract_clojure_commit"
       :plan "apply_clojure_plan"
       :preview "transform_clojure_preview"
       :transform "transform_clojure_commit"}
   :S {:edit "edit_clojure_bang"
       :extract "extract_clojure_bang"
       :plan "apply_clojure_plan_bang"
       :preview "transform_clojure_with_clojure"
       :transform "transform_clojure_with_clojure_bang"}
   :T {:edit "write_clojure_edits"
       :extract "move_clojure_owners"
       :plan "apply_clojure_plan"
       :preview "preview_clojure_transform"
       :transform "apply_clojure_transform"}})

(def effect-titles
  {:R {:edit "edit_clojure!"
       :extract "extract_clojure!"
       :plan "apply_clojure_plan!"
       :preview "transform_clojure"
       :transform "transform_clojure!"}
   :S {:edit "edit_clojure!"
       :extract "extract_clojure!"
       :plan "apply_clojure_plan!"
       :preview "transform_clojure_with_clojure"
       :transform "transform_clojure_with_clojure!"}
   :T {:edit "write_clojure_edits!"
       :extract "move_clojure_owners!"
       :plan "apply_clojure_plan!"
       :preview "preview_clojure_transform"
       :transform "apply_clojure_transform!"}})

(def read-only-annotations
  {:read-only true
   :destructive false
   :idempotent true
   :open-world false
   :return-direct false})

(def mutation-annotations
  {:read-only false
   :destructive true
   :idempotent false
   :open-world false
   :return-direct false})

(defn- with-effect-annotations
  [tool annotations title]
  (assoc tool :annotations
         (cond-> annotations
           title (assoc :title title))))

(defn- effect-catalog-tools
  [catalog base-tools]
  (let [names (get effect-names catalog)
        titles (get effect-titles catalog)
        split (split-catalog-tools base-tools)
        transform (tool-by-id split :transform-clojure)
        preview-schema (update (:schema transform) :properties dissoc "commit")
        commit-schema (-> (:schema transform)
                          (assoc-in [:properties "commit"]
                                    {:type "boolean" :const true})
                          (update :required (fnil conj []) "commit"))
        preview-tool (-> transform
                         (assoc :id :candidate-transform-preview
                                :name (:preview names)
                                :description transform-preview-description
                                :schema preview-schema)
                         (with-effect-annotations
                           read-only-annotations
                           (:preview titles)))
        commit-tool (-> transform
                        (assoc :id :candidate-transform-commit
                               :name (:transform names)
                               :description transform-commit-description
                               :schema commit-schema)
                        (with-effect-annotations
                          mutation-annotations
                          (:transform titles)))]
    (->> split
         (mapcat
           (fn [tool]
             (case (:id tool)
               :edit-clojure
               [(-> tool
                    (assoc :name (:edit names))
                    (with-effect-annotations
                      mutation-annotations
                      (:edit titles)))]

               :candidate-extract-clojure
               [(-> tool
                    (assoc :name (:extract names))
                    (with-effect-annotations
                      mutation-annotations
                      (:extract titles)))]

               :candidate-apply-clojure-plan
               [(-> tool
                    (assoc :name (:plan names))
                    (with-effect-annotations
                      mutation-annotations
                      (:plan titles)))]

               :transform-clojure [preview-tool commit-tool]
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
     (if (#{:P :Q :R :S :T} catalog)
       (effect-catalog-tools catalog base-tools)
       (if (#{:M :N :O} catalog)
         (cond->> (split-catalog-tools base-tools)
           (= :N catalog)
           (mapv (fn [tool]
                   (case (:id tool)
                     :edit-clojure
                     (assoc tool :description atomic-chord-description)

                     :transform-clojure
                     (project-tool tool "transform_clojure_with_clojure"
                                   computed-solo-description)

                     tool)))

           (= :O catalog)
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
           base-tools))))))

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
     :deferred-factorials (if (#{:M :N :O :P :Q :R :S :T}
                               (normalize-catalog catalog))
                            deferred-factorials
                            [])
     :orthogonality (when (#{:M :N :O :P :Q :R :S :T}
                           (normalize-catalog catalog))
                      {:controls option-m-controls
                       :overlap-matrix option-m-overlap-matrix
                       :falsifier-cards option-m-falsifier-cards})
     :handlers
     (mapv (fn [{:keys [id name tool-fn]}]
             {:id id
              :name name
              :handler-var (str tool-fn)})
           tools)
     :annotations
     (mapv (fn [{:keys [name annotations]}]
             {:name name :annotations annotations})
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
