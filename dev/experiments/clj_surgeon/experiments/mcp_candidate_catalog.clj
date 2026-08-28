(ns clj-surgeon.experiments.mcp-candidate-catalog
  "Isolated MCP catalog projections for the mutation-tool naming experiment."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.experiments.mcp-candidate-response :as response]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.string :as str]))

(def supported-catalogs [:A :L :C :M :N :O :P :Q :R :S :T :U :V :W :X])

(def role-lexicons
  "One complete public vocabulary for each alternate catalog.

  A role can intentionally map to the same tool as another role. Catalogs A,
  L, and C retain one combined semantic-change tool. Catalogs M through O
  retain one transform tool for preview and commit. The effect catalogs split
  transform preview from transform commit."
  {:A {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "apply_clojure_changes"
       :plan "apply_clojure_changes"
       :transform-preview "transform_clojure"
       :transform-commit "transform_clojure"}
   :L {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "apply_clojure_plan"
       :plan "apply_clojure_plan"
       :transform-preview "transform_clojure"
       :transform-commit "transform_clojure"}
   :C {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "refactor_clojure"
       :plan "refactor_clojure"
       :transform-preview "transform_clojure"
       :transform-commit "transform_clojure"}
   :M {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "extract_clojure"
       :plan "apply_clojure_plan"
       :transform-preview "transform_clojure"
       :transform-commit "transform_clojure"}
   :N {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "extract_clojure"
       :plan "apply_clojure_plan"
       :transform-preview "transform_clojure_with_clojure"
       :transform-commit "transform_clojure_with_clojure"}
   :O {:inspect "inspect_clojure"
       :edit "apply_clojure_edits"
       :extract "extract_clojure"
       :plan "apply_clojure_plan"
       :transform-preview "run_clojure_transform"
       :transform-commit "run_clojure_transform"}
   :P {:inspect "inspect_clojure"
       :edit "commit_clojure_edits"
       :extract "commit_clojure_extraction"
       :plan "apply_clojure_plan"
       :transform-preview "preview_clojure_transform"
       :transform-commit "commit_clojure_transform"}
   :Q {:inspect "inspect_clojure"
       :edit "clojure.edit.commit"
       :extract "clojure.extract.commit"
       :plan "clojure.plan.apply"
       :transform-preview "clojure.transform.preview"
       :transform-commit "clojure.transform.commit"}
   :R {:inspect "inspect_clojure"
       :edit "edit_clojure_commit"
       :extract "extract_clojure_commit"
       :plan "apply_clojure_plan"
       :transform-preview "transform_clojure_preview"
       :transform-commit "transform_clojure_commit"}
   :S {:inspect "inspect_clojure"
       :edit "edit_clojure_bang"
       :extract "extract_clojure_bang"
       :plan "apply_clojure_plan_bang"
       :transform-preview "transform_clojure_with_clojure"
       :transform-commit "transform_clojure_with_clojure_bang"}
   :T {:inspect "inspect_clojure"
       :edit "write_clojure_edits"
       :extract "move_clojure_owners"
       :plan "apply_clojure_plan"
       :transform-preview "preview_clojure_transform"
       :transform-commit "apply_clojure_transform"}
   :U {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "apply_clojure_changes"
       :plan "continue_clojure_plan"
       :transform-preview "transform_clojure"
       :transform-commit "transform_clojure"}
   :V {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "apply_clojure_extraction"
       :plan "continue_clojure_plan"
       :transform-preview "transform_clojure"
       :transform-commit "transform_clojure"}
   :W {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "extract_clojure"
       :plan "continue_clojure_plan"
       :transform-preview "transform_clojure"
       :transform-commit "transform_clojure"}
   :X {:inspect "inspect_clojure"
       :edit "edit_clojure"
       :extract "move_clojure_forms"
       :plan "continue_clojure_plan"
       :transform-preview "transform_clojure"
       :transform-commit "transform_clojure"}})

(def public-role-keys
  [:inspect :edit :extract :plan :transform-preview :transform-commit])

(declare catalog-instructions catalog-tools normalize-catalog)

(defn catalog-lexicon
  "Return the complete role vocabulary for one catalog."
  [catalog]
  (get role-lexicons (normalize-catalog catalog)))

(defn catalog-role-receipt
  "Return the transport-neutral role receipt consumed by benchmark scorers."
  [catalog]
  (let [catalog (normalize-catalog catalog)]
    {:catalog (name catalog)
     :roles (into (sorted-map)
                  (map (fn [[role tool-name]]
                         [role tool-name]))
                  (catalog-lexicon catalog))}))

(defn catalog-runtime-receipt
  "Return the exact advertised catalog surface used for client parity gates."
  [catalog]
  (let [catalog (normalize-catalog catalog)
        tools (catalog-tools catalog)]
    (assoc (catalog-role-receipt catalog)
           :instructions (catalog-instructions catalog)
           :tool-order (mapv :name tools)
           :tools
           (mapv (fn [{:keys [name description schema output-schema
                              annotations]}]
                   {:name name
                    :description description
                    :input-schema schema
                    :output-schema output-schema
                    :annotations annotations})
                 tools))))

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

(defn- catalog-instructions*
  [{:keys [inspect edit extract plan transform-preview transform-commit]}]
  (str
    "Batch known Clojure reads with " inspect ". Use " edit " when the "
    "request already contains every edit and guard. Use " extract " for "
    "named-owner namespace movement. Use " plan " only for a retained basis "
    "and its filled decisions. Use " transform-preview " to preview one "
    "standalone bounded computed relation"
    (if (= transform-preview transform-commit)
      " and commit it only when the request explicitly authorizes commit. "
      (str "; use " transform-commit " to commit that computed relation. "))
    "Do not repeat reads after read_complete=true or writes after "
    "verification_complete=true."))

(defn catalog-instructions
  "Return initialize instructions written entirely in one catalog lexicon."
  [catalog]
  (catalog-instructions* (catalog-lexicon catalog)))

(defn- edit-description*
  [{:keys [edit plan transform-preview]}]
  (str
    "Commit one complete Clojure edit decision with " edit ". Supply exact "
    "old and new forms, bounded computed programs, exact owner deletions, or "
    "fully specified structural changes. Surgeon compiles all items against "
    "one frozen snapshot and commits them atomically. Put a computed relation "
    "here only when it must commit with another source action. Use "
    transform-preview " for one standalone computed relation. Use " plan
    " only for a retained semantic basis and filled decisions."))

(defn- combined-plan-description*
  [{:keys [edit extract plan inspect]}]
  (str
    "Compile and apply one semantic Clojure change with " plan ". Use the "
    "retained basis returned by " inspect " or use this same control as "
    extract " for a direct extraction request. Surgeon refuses unresolved "
    "decisions before mutation. Use " edit " when the request already "
    "contains every edit and guard."))

(defn- extraction-description*
  [{:keys [extract]}]
  (str
    "Use " extract " to move exact named Clojure owners into one new "
    "namespace. Supply the source file, destination, ordered owners, require "
    "policy, and every known caller decision. When the request is complete, "
    "Surgeon compiles, commits, and verifies the extraction in this call. A "
    "genuine unresolved caller decision refuses before mutation with a "
    "completed frozen plan and an exact next call to " extract "."))

(defn- basis-plan-description*
  [{:keys [inspect extract plan]}]
  (str
    "Use " plan " only to apply one retained Clojure change basis after "
    "every genuine decision is filled. Preserve workspace_root, basis, site "
    "IDs, decisions, and verify from the " inspect " next call. Surgeon "
    "refuses stale source or an unresolved decision before mutation. Use "
    extract " for extraction, including a mechanically complete one-call "
    "extraction."))

(defn- transform-preview-description*
  [{:keys [edit transform-preview transform-commit]}]
  (str
    "Use " transform-preview " to preview one standalone bounded SCI "
    "transformation. Supply one structural selection and one bounded rule. "
    (if (= transform-preview transform-commit)
      "This same control commits only when commit=true. "
      (str "This control cannot commit. Use " transform-commit
           " after the decision is complete. "))
    "Use " edit " when the program must commit with another source action."))

(defn- transform-commit-description*
  [{:keys [edit transform-preview transform-commit]}]
  (str
    "Use " transform-commit " to commit one standalone bounded SCI "
    "transformation. Supply one structural selection, one bounded rule, and "
    "commit=true. Use " transform-preview " while the decision is incomplete. "
    "Use " edit " when the program must commit with another source action."))

(defn- replace-canonical-tool-names
  [text lexicon operation-role]
  (let [transform-role (if (= operation-role :transform-commit)
                         :transform-commit
                         :transform-preview)
        change-role (if (= operation-role :extract) :extract :plan)]
    (reduce (fn [result [old role]]
              (str/replace result old (get lexicon role)))
            text
            [["inspect_clojure" :inspect]
             ["apply_clojure_changes" change-role]
             ["edit_clojure" :edit]
             ["transform_clojure" transform-role]])))

(defn- project-schema-prose
  "Replace only descriptive schema strings, never enum/const response data."
  [value lexicon operation-role]
  (cond
    (map? value)
    (into (empty value)
          (map (fn [[key child]]
                 [key (if (and (#{:description "description" :title "title"} key)
                               (string? child))
                        (replace-canonical-tool-names
                          child lexicon operation-role)
                        (project-schema-prose child lexicon operation-role))]))
          value)

    (vector? value)
    (mapv #(project-schema-prose % lexicon operation-role) value)

    (seq? value)
    (doall (map #(project-schema-prose % lexicon operation-role) value))

    :else value))

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
        edit-tool (assoc compact :schema edit-schema)
        extraction-tool (assoc change
                               :id :candidate-extract-clojure
                               :name "extract_clojure"
                               :schema extraction-schema)
        plan-tool (assoc change
                         :id :candidate-apply-clojure-plan
                         :name "apply_clojure_plan"
                         :schema basis-schema)]
    (->> base-tools
         (mapcat
           (fn [tool]
             (case (:id tool)
               :clj-change []
               :edit-clojure [edit-tool extraction-tool plan-tool]
               :transform-clojure [tool]
               [tool])))
         vec)))

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
  (let [lexicon (catalog-lexicon catalog)
        names {:edit (:edit lexicon)
               :extract (:extract lexicon)
               :plan (:plan lexicon)
               :preview (:transform-preview lexicon)
               :transform (:transform-commit lexicon)}
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
                                :schema preview-schema)
                         (with-effect-annotations
                           read-only-annotations
                           (:preview titles)))
        commit-tool (-> transform
                        (assoc :id :candidate-transform-commit
                               :name (:transform names)
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

(defn- operation-role
  [{:keys [id]}]
  (case id
    :inspect-clojure :inspect
    :edit-clojure :edit
    :clj-change :plan
    :candidate-extract-clojure :extract
    :candidate-apply-clojure-plan :plan
    :transform-clojure :transform-preview
    :candidate-transform-preview :transform-preview
    :candidate-transform-commit :transform-commit))

(defn- role-description
  [tool role lexicon]
  (case role
    :inspect
    (replace-canonical-tool-names (:description tool) lexicon :plan)

    :edit (edit-description* lexicon)
    :extract (extraction-description* lexicon)
    :plan (combined-plan-description* lexicon)
    :transform-preview (transform-preview-description* lexicon)
    :transform-commit (transform-commit-description* lexicon)))

(defn- project-tool-surface
  [lexicon tool]
  (let [role (operation-role tool)
        description (if (= (:id tool) :candidate-apply-clojure-plan)
                      (basis-plan-description* lexicon)
                      (role-description tool role lexicon))]
    (-> tool
        (assoc :name (get lexicon role)
               :description description)
        (update :tool-fn #(response/wrap-handler lexicon role (:schema tool) %))
        (update :schema project-schema-prose lexicon role)
        (update :output-schema project-schema-prose lexicon role)
        (update :annotations project-schema-prose lexicon role))))

(def neutral-role-lexicon
  {:inspect "the inspection tool"
   :edit "the exact-edit tool"
   :extract "the extraction tool"
   :plan "the plan-continuation tool"
   :transform-preview "the transform tool"
   :transform-commit "the transform tool"})

(defn- project-name-only-tool-surface
  "Project stable prose while varying only the public extraction name."
  [public-lexicon tool]
  (let [role (operation-role tool)
        description (if (= (:id tool) :candidate-apply-clojure-plan)
                      (basis-plan-description* neutral-role-lexicon)
                      (role-description tool role neutral-role-lexicon))]
    (-> tool
        (assoc :name (get public-lexicon role)
               :description description)
        (update :tool-fn #(response/wrap-handler public-lexicon role
                                                 (:schema tool) %))
        (update :schema project-schema-prose neutral-role-lexicon role)
        (update :output-schema project-schema-prose neutral-role-lexicon role)
        (update :annotations project-schema-prose neutral-role-lexicon role))))

(defn catalog-tools
  "Project one candidate catalog over canonical public tool entries.

  The projection changes only public names, descriptions, and schemas. Every
  projected mutation tool retains the canonical handler Var and output schema."
  ([catalog]
   (catalog-tools catalog (mcp-server/public-tool-registry)))
  ([catalog base-tools]
   (let [catalog (normalize-catalog catalog)
         lexicon (catalog-lexicon catalog)
         name-only? (#{:U :V :W :X} catalog)
         tools (cond
                 (#{:P :Q :R :S :T} catalog)
                 (effect-catalog-tools catalog base-tools)

                 (#{:M :N :O :U :V :W :X} catalog)
                 (split-catalog-tools base-tools)

                 :else base-tools)]
     (if name-only?
       (mapv #(project-name-only-tool-surface lexicon %) tools)
       (mapv #(project-tool-surface lexicon %) tools)))))

(defn- schema-facing-prose
  [value]
  (cond
    (map? value)
    (mapcat (fn [[key child]]
              (if (and (#{:description "description" :title "title"} key)
                       (string? child))
                [child]
                (schema-facing-prose child)))
            value)

    (sequential? value) (mapcat schema-facing-prose value)
    :else []))

(defn caller-visible-surface
  "Return every pre-first-call string controlled by the candidate server.

  This intentionally excludes post-call result content. Legacy operation
  fields and human summaries remain a later complete-route projection gate."
  [catalog]
  (let [tools (catalog-tools catalog)]
    {:initialize-instructions (catalog-instructions catalog)
     :tools
     (mapv (fn [{:keys [name description schema output-schema annotations]}]
             {:name name
              :title (:title annotations)
              :description description
              :schema-prose (vec (concat (schema-facing-prose schema)
                                         (schema-facing-prose output-schema)))})
           tools)}))

(defn caller-visible-strings
  "Flatten the pre-first-call surface for exact catalog leak checks."
  [catalog]
  (let [{:keys [initialize-instructions tools]}
        (caller-visible-surface catalog)]
    (into [initialize-instructions]
          (mapcat (fn [{:keys [name title description schema-prose]}]
                    (remove nil? (concat [name title description]
                                         schema-prose))))
          tools)))

(defn unavailable-public-name-leaks
  "Return unavailable mutation identifiers visible before a first call."
  [catalog]
  (let [available-tools (set (map :name (catalog-tools catalog)))
        mutation-name-universe
        (->> role-lexicons
             vals
             (mapcat #(vals (dissoc % :inspect)))
             (into #{"apply_clojure_changes"
                     "edit_clojure"
                     "transform_clojure"}))
        unavailable (remove available-tools mutation-name-universe)
        strings (caller-visible-strings catalog)
        name-pattern
        (fn [tool-name]
          (re-pattern
            (str "(?<![A-Za-z0-9_.!_-])"
                 (java.util.regex.Pattern/quote tool-name)
                 "(?![A-Za-z0-9_.!_-])")))]
    (->> unavailable
         (keep (fn [tool-name]
                 (let [evidence (filterv #(re-find (name-pattern tool-name) %)
                                         strings)]
                   (when (seq evidence)
                     {:name tool-name :evidence evidence}))))
         (sort-by :name)
         vec)))

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
           tools)
     :route-gates
     {:post-call-response-projection
      {:status :deferred
       :reason
       (str
         "Legacy operation fields, known routing templates, next-call tool "
         "names, and summary action lines now use the isolated callback "
         "projection. Clean-context complete-route behavior remains a later "
         "gate before any performance or publication claim.")}}}))

(defn start
  "Start one isolated HTTP MCP server with a projected candidate catalog.

  This experiment supports only the full tool profile. It does not install,
  reload, or change the production dispatcher."
  [{:keys [catalog catalog-receipt-file tool-profile] :as opts}]
  (when (and tool-profile (not= :full tool-profile))
    (throw (ex-info "Candidate catalogs require the full tool profile"
                    {:tool-profile tool-profile
                     :supported [:full]})))
  (let [catalog (normalize-catalog catalog)
        projected-tools (catalog-tools catalog)
        projected-instructions (catalog-instructions catalog)
        server-opts (dissoc opts :catalog :catalog-receipt-file)]
    (binding [*out* *err*]
      (println "clj-surgeon candidate MCP catalog:" (name catalog)))
    (when catalog-receipt-file
      (spit catalog-receipt-file
            (json/generate-string (catalog-runtime-receipt catalog))))
    (with-redefs [mcp-tool/all-tools (constantly projected-tools)
                  mcp-server/server-instructions projected-instructions]
      (http-server/start server-opts))))
