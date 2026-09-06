(ns clj-surgeon.mcp-schema)

;; @spec MCP-OP-VERIFY-013
(def verification-profile-names
  "The verify values every entrance accepts, in one place.

  The set was spelled three times — this schema and two literals in
  mcp-contract — and drifted the moment `lint` became the only built-in
  profile: the unconfigured refusal advertised a retry the enum rejected on
  both write routes. An advertised remedy that nothing accepts is worse than
  no remedy at all."
  ["fast" "full" "exact" "lint"])

(def verification-profile-sentence
  "The same set as one sentence, so a refusal cannot disagree with the enum."
  "verify must be fast, full, exact, or lint")

(def verification-schema
  {:type "string" :enum verification-profile-names
   :description
   "Optional repository-owned verification profile named in .clj-surgeon.edn. lint is the only built-in and runs a lint and format gate, which is NOT a test profile; a name this workspace does not configure is refused before any write. exact runs one project-declared exact-exit argv against staged bytes. Formatter, commands, and hot laws roll back on failure. A configured cold job returns verification_complete=false plus one inspect next_call."})

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
    "verify" verification-schema}
   :required ["basis" "decisions"]})

(def ^:private positive-integer-schema
  {:type "integer" :minimum 1})

(def defmethod-owner-schema
  {:type "object"
   :additionalProperties false
   :description "Exact multimethod implementation owner. Name and complete dispatch form must resolve exactly once."
   :properties
   {"kind" {:type "string" :enum ["defmethod"]}
    "name" {:type "string" :minLength 1}
    "dispatch" {:type "string" :minLength 1
                :description "Exactly one complete Clojure dispatch form."}}
   :required ["kind" "name" "dispatch"]})

(def explicit-change-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "changes"
    {:type "array"
     :minItems 1
     :description "Complete structural changes. IDs must be unique. Never combine edits and changes; if any action needs changes, encode the complete transaction here."
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
                :description "Exact named top-level owners. Use a string for a unique Var or {kind:defmethod,name,dispatch} for one multimethod implementation."
                :items {:oneOf [{:type "string" :minLength 1}
                                defmethod-owner-schema]}}
       "owner" {:type "object"
                :additionalProperties false
                :description "Exact non-Var owner scope. Omit a namespace name only to resolve the file's unique ns form."
                :properties
                {"kind" {:type "string" :enum ["namespace"]}
                 "name" {:type "string" :minLength 1}}
                :required ["kind"]}
       "find" {:type "string" :minLength 1
               :description "Exactly one Clojure form in its required source spelling."}
       "inside" {:type "string" :minLength 1
                 :description "assoc_entry only. Restrict the map to this complete semantic ancestor; comments do not change identity."}
       "replace" {:type "string" :minLength 1
                  :description "Replacement action: exactly one Clojure form. Source spelling is preserved."}
       "delete" {:type "boolean" :enum [true]
                 :description "Delete every exact named owner in forms, including attached comments and separators. Does not use find or the semantic provider."}
       "insert_before" {:type "array" :minItems 1
                        :description "Insert complete Clojure forms before a nested find match, or omit find to insert before exactly one named top-level forms owner. One array item may contain several complete forms. Comment-bearing gaps refuse."
                        :items {:type "string" :minLength 1}}
       "insert_after" {:type "array" :minItems 1
                       :description "Insert complete Clojure forms after a nested find match, or omit find to insert after exactly one named top-level forms owner. One array item may contain several complete forms. Comment-bearing gaps refuse."
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
          :not {:anyOf [{:required ["delete"]}
                        {:required ["insert_before"]}
                        {:required ["insert_after"]}
                        {:required ["rename_binding"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}
         {:required ["insert_before"]
          :not {:anyOf [{:required ["replace"]}
                        {:required ["delete"]}
                        {:required ["insert_after"]}
                        {:required ["rename_binding"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}
         {:required ["insert_after"]
          :not {:anyOf [{:required ["replace"]}
                        {:required ["delete"]}
                        {:required ["insert_before"]}
                        {:required ["rename_binding"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}
         {:required ["forms" "rename_binding"]
          :not {:anyOf [{:required ["find"]}
                        {:required ["owner"]}
                        {:required ["replace"]}
                        {:required ["delete"]}
                        {:required ["insert_before"]}
                        {:required ["insert_after"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}
         {:required ["find" "assoc_entry"]
          :not {:anyOf [{:required ["replace"]}
                        {:required ["delete"]}
                        {:required ["insert_before"]}
                        {:required ["insert_after"]}
                        {:required ["rename_binding"]}]}}
         {:required ["forms" "delete"]
          :not {:anyOf [{:required ["find"]}
                        {:required ["owner"]}
                        {:required ["replace"]}
                        {:required ["insert_before"]}
                        {:required ["insert_after"]}
                        {:required ["rename_binding"]}
                        {:required ["assoc_entry"]}
                        {:required ["inside"]}]}}]}]}}
    "expect"
    {:type "object"
     :additionalProperties false
     :description "Optional redundant aggregate bookkeeping. Surgeon derives exact counts from per-change guards and reports any supplied disagreement as ignored normalization."
     :properties
     {"changes" (assoc positive-integer-schema :description "Number of change objects.")
      "edits" (assoc positive-integer-schema :description "Total exact replacements.")
      "files" (assoc positive-integer-schema :description "Total files that must change.")}
     :required ["changes" "edits" "files"]}
    "expect_matched"
    {:type "object"
     :additionalProperties false
     :description "Optional prior-match basis copied from one inspect_clojure match receipt on the same snapshot. When supplied, the receipt reports every matched site this transaction did not address. Stateless: the file hash fences it, and a file, hash, or count disagreement refuses before any write."
     :properties
     {"file" {:type "string" :minLength 1
              :description "Project-relative source path, exactly the match receipt's file."}
      "file_hash" {:type "string" :minLength 1
                   :description "The match receipt's file_hash. This transaction's pre-image hash must equal it."}
      "match" {:type "string" :minLength 1
               :description "Exactly the structural pattern the match receipt echoed."}
      "count" {:type "integer" :minimum 0
               :description "The match receipt's match_count for that pattern and snapshot."}}
     :required ["file" "file_hash" "match" "count"]}
    "verify" verification-schema}
   :required ["changes"]})

;; @spec MCP-OP-EDIT-019
(def editor-gesture-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "edits"
    {:type "array"
     :minItems 1
     :description "Exact literal replacements compiled into the same atomic transaction. Use one file with a named Clojure owner, or explicit files with root scope for a grouped Clojure/EDN edit."
     :items
     {:type "object"
      :additionalProperties false
      :properties
      {"file" {:type "string" :minLength 1
               :description "One project-relative .clj, .cljs, .cljc, or root-scoped .edn path."}
       "files" {:type "array" :minItems 1 :uniqueItems true
                :items {:type "string" :minLength 1}
                :description "Explicit project-relative paths for one grouped root-scoped edit. Every file must match independently."}
       "within" {:type "object"
                 :additionalProperties false
                 :description "Optional exact Clojure owner. Omit only when source can prove one injective owner; EDN requires root scope."
                 :properties
                 {"form" {:type "string" :minLength 1
                          :description "One exact named top-level form."}
                  "namespace" {:oneOf [{:type "string" :minLength 1}
                                       {:type "boolean" :enum [true]}]
                               :description "An exact namespace name, or true to resolve the file's unique ns form."}
                  "root" {:type "boolean" :enum [true]
                          :description "Search the complete concrete syntax tree while preserving all bytes outside exact replacements."}}
                 :oneOf [{:required ["form"]}
                         {:required ["namespace"]}
                         {:required ["root"]}]}
       "from" {:type "string" :minLength 1
               :description "Canonical source field: the exact old Clojure subtree. Its count must equal matches, which defaults to one."}
       "to" {:type "string" :minLength 1
             :description "Canonical target field: the replacement Clojure subtree."}
       "old" {:type "string" :minLength 1
              :description "Exact alias for from. Supply only with new."}
       "new" {:type "string" :minLength 1
              :description "Exact alias for to. Supply only with old."}
       "before" {:type "string" :minLength 1
                 :description "Exact alias for from. Supply only with after."}
       "after" {:type "string" :minLength 1
                :description "Exact alias for to. Supply only with before."}
       "matches" positive-integer-schema}
      :allOf
      [{:oneOf
        [{:required ["from" "to"]
          :not {:anyOf [{:required ["old"]}
                        {:required ["new"]}
                        {:required ["before"]}
                        {:required ["after"]}]}}
         {:required ["old" "new"]
          :not {:anyOf [{:required ["from"]}
                        {:required ["to"]}
                        {:required ["before"]}
                        {:required ["after"]}]}}
         {:required ["before" "after"]
          :not {:anyOf [{:required ["from"]}
                        {:required ["to"]}
                        {:required ["old"]}
                        {:required ["new"]}]}}]}
       {:oneOf [{:required ["file"]
                 :not {:required ["files"]}}
                {:required ["files"]
                 :not {:required ["file"]}}]}]}}
    "delete_owners"
    {:type "array"
     :minItems 1
     :maxItems 32
     :description "Grouped exact named top-level owners to delete in the same frozen transaction."
     :items
     {:type "object"
      :additionalProperties false
      :properties
      {"file" {:type "string" :minLength 1
               :description "One project-relative .clj, .cljs, or .cljc source path."}
       "forms" {:type "array"
                :minItems 1
                :maxItems 128
                :items {:type "string" :minLength 1}
                :description "Exact unique named top-level owners in this file."}}
      :required ["file" "forms"]}}
    "verify" verification-schema}})

;; @spec MCP-OP-EDIT-031
(def editor-create-files-schema
  {:type "array"
   :minItems 1
   :maxItems 32
   :description "Optional absent files created inside the same frozen transaction. Every target must not already exist and its content must parse as Clojure or EDN."
   :items
   {:type "object"
    :additionalProperties false
    :properties
    {"file" {:type "string" :minLength 1
             :description "One project-relative .clj, .cljs, .cljc, or .edn path that does not exist yet."}
     "content" {:type "string" :minLength 1
                :description "The complete file content, written verbatim."}}
    :required ["file" "content"]}})

(def editor-programs-schema
  {:type "array"
   :minItems 1
   :maxItems 16
   :description "Optional independent computed relations compiled against the same original snapshot as edits."
   :items
   {:type "object"
    :additionalProperties false
    :properties
    {"file" {:type "string" :minLength 1
             :description "One project-relative .clj, .cljs, or .cljc source path."}
     "expression" {:type "string" :minLength 1
                   :description "A structural path ending in (transform pure-function)."}
     "expect" {:type "object"
               :additionalProperties false
               :properties
               {"matches" {:type "integer" :minimum 1 :maximum 128}
                "max_changed_characters" {:type "integer" :minimum 1 :maximum 262144}}
               :required ["matches" "max_changed_characters"]}}
    :required ["file" "expression" "expect"]}})

(def compact-relation-lib-alias-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"lib" {:type "string" :minLength 1}
    "as" {:type "string" :minLength 1}}
   :required ["lib" "as"]})

(def symbol-migration-schema
  {:type "object"
   :additionalProperties false
   :description
   (str "Grouped exact owner-scoped symbol replacements. target_alias replaces "
        "the qualifier while preserve-name retains each from symbol's name. "
        "Each files entry is [file, sites]; each site is [owner, from, matches]. "
        "Owners, old symbols, and positive counts are authority, not discovery.")
   :properties
   {"target_alias" {:type "string" :minLength 1}
    "target_rule" {:type "string" :enum ["preserve-name"]}
    "columns" {:type "array"
               :minItems 3
               :maxItems 3
               :prefixItems [{:const "owner"}
                             {:const "from"}
                             {:const "matches"}]}
    "files" {:type "array"
             :minItems 1
             :items
             {:type "array"
              :minItems 2
              :maxItems 2
              :prefixItems
              [{:type "string" :minLength 1}
               {:type "array"
                :minItems 1
                :items
                {:type "array"
                 :minItems 3
                 :maxItems 3
                 :prefixItems [{:type "string" :minLength 1}
                               {:type "string" :minLength 1}
                               {:type "integer" :minimum 1 :maximum 128}]}}]}}}
   :required ["target_alias" "target_rule" "columns" "files"]})

(def require-change-schema
  {:type "object"
   :additionalProperties false
   :description
   (str "One explicit require migration paired with symbol_migration. add names "
        "the exact target lib and alias. files repeats the same ordered file "
        "vector and may name one exact old lib/alias to remove per file.")
   :properties
   {"add" compact-relation-lib-alias-schema
    "files"
    {:type "array"
     :minItems 1
     :items
     {:type "object"
      :additionalProperties false
      :properties
      {"file" {:type "string" :minLength 1}
       "remove" compact-relation-lib-alias-schema}
      :required ["file"]}}}
   :required ["add" "files"]})

(def editor-hybrid-schema
  (-> editor-gesture-schema
      (assoc-in [:properties "programs"] editor-programs-schema)
      (assoc-in [:properties "create_files"] editor-create-files-schema)
      (assoc-in [:properties "symbol_migration"] symbol-migration-schema)
      (assoc-in [:properties "require_change"] require-change-schema)
      (assoc :anyOf [{:required ["edits"]}
                     {:required ["programs"]}
                     {:required ["delete_owners"]}
                     {:required ["create_files"]}
                     {:required ["symbol_migration" "require_change"]}])
      (assoc :allOf
             [{:not
               {:oneOf
                [{:required ["symbol_migration"]
                  :not {:required ["require_change"]}}
                 {:required ["require_change"]
                  :not {:required ["symbol_migration"]}}]}}])))

;; @spec MCP-OP-PREP-ACT-005
;; @spec MCP-OP-PREP-ACT-015
(def prepared-confirmation-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"confirm" {:type "string" :pattern "^[0-9a-f]{64}$"}
    "fill" {:type "object"
            :minProperties 1
            :additionalProperties {:type "string" :minLength 1}}
    "preview" {:type "boolean" :enum [true]}}
   :required ["confirm" "fill"]})

(def editor-tool-schema
  (let [ordinary (-> editor-hybrid-schema
                     (update :properties dissoc "verify"))]
    {:type "object"
     :additionalProperties false
     :properties (merge (:properties ordinary)
                        (:properties prepared-confirmation-schema))
     :anyOf (conj (:anyOf ordinary) {:required ["confirm" "fill"]})
     :oneOf [ordinary prepared-confirmation-schema]}))

(def extraction-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "extraction"
    {:type "object"
     :additionalProperties false
     :properties
     {"file" {:type "string" :minLength 1
              :description "Existing project-relative source namespace."}
      "to" {:type "string" :minLength 1
            :description "Absent project-relative destination namespace."}
      "forms" {:type "array" :minItems 1 :uniqueItems true
               :items {:type "string" :minLength 1}}
      "public_forms"
      {:type "array" :uniqueItems true
       :description "Moved private defn- forms explicitly authorized to become public. Omit to derive only mechanically required visibility from the same frozen extraction snapshot; an explicit empty array remains authoritative."
       :items {:type "string" :minLength 1}}
      "require_policy" {:type "string" :enum ["minimal" "copy-all"]}
      "source_hash" {:type "string" :pattern "^[0-9a-f]{64}$"
                     :description "Optional frozen source hash returned by plan-extraction."}
      "caller_changes" (assoc (get-in explicit-change-schema
                                      [:properties "changes"])
                              :minItems 0
                              :description "Exact caller rewrites. Omit when none are known; omission never accounts for a discovered caller candidate.")
      "ignored_caller_files"
      {:type "array" :uniqueItems true
       :description "Caller candidates explicitly reviewed and intentionally left unchanged. Omit when none have been reviewed."
       :items {:type "string" :minLength 1}}
      "expect"
      {:type "object"
       :additionalProperties false
       :properties
       {"forms" positive-integer-schema
        "caller_edits" {:type "integer" :minimum 0}
        "files" positive-integer-schema}
       :required ["forms" "caller_edits" "files"]}}
     :required ["file" "to" "forms" "require_policy"]}
    "verify" verification-schema}
   :required ["extraction"]})

(def clj-change-schema
  {:type "object"
   :additionalProperties false
   :properties (merge (:properties basis-change-schema)
                      (:properties explicit-change-schema)
                      (:properties editor-hybrid-schema)
                      (:properties extraction-schema))
   :allOf (:allOf editor-hybrid-schema)
   ;; @spec MCP-OP-MATCHED-005
   ;; `expect_matched` belongs to the direct changes transaction only. Every
   ;; other branch refuses it at validation, so no other branch advertises it.
   :oneOf
   [{:required ["basis" "decisions"]
     :not {:anyOf [{:required ["changes"]} {:required ["expect"]}
                   {:required ["edits"]} {:required ["programs"]}
                   {:required ["delete_owners"]}
                   {:required ["symbol_migration"]}
                   {:required ["require_change"]}
                   {:required ["expect_matched"]}
                   {:required ["extraction"]}]}}
    {:required ["changes" "expect"]
     :not {:anyOf [{:required ["basis"]}
                   {:required ["decisions"]}
                   {:required ["edits"]}
                   {:required ["programs"]}
                   {:required ["delete_owners"]}
                   {:required ["symbol_migration"]}
                   {:required ["require_change"]}
                   {:required ["extraction"]}]}}
    {:anyOf [{:required ["edits"]}
             {:required ["programs"]}
             {:required ["delete_owners"]}
             {:required ["symbol_migration" "require_change"]}]
     :not {:anyOf [{:required ["basis"]}
                   {:required ["decisions"]}
                   {:required ["changes"]}
                   {:required ["expect"]}
                   {:required ["expect_matched"]}
                   {:required ["extraction"]}]}}
    {:required ["extraction"]
     :not {:anyOf [{:required ["basis"]}
                   {:required ["decisions"]}
                   {:required ["changes"]}
                   {:required ["edits"]}
                   {:required ["programs"]}
                   {:required ["delete_owners"]}
                   {:required ["symbol_migration"]}
                   {:required ["require_change"]}
                   {:required ["expect_matched"]}
                   {:required ["expect"]}]}}]})

(defn closed-object-shape
  "Return the accepted and required field names of one closed object schema."
  [schema]
  {:allowed (set (keys (:properties schema)))
   :required (set (:required schema))})

(defn direct-contract-shape
  "Project every closed direct-change object from the public JSON Schema.

  The request router consumes workspace_root before the direct validator runs,
  so that routing field is deliberately absent from :request."
  [schema]
  (let [change (get-in schema [:properties "changes" :items])]
    {:request (update (closed-object-shape schema)
                      :allowed disj "workspace_root")
     :change (closed-object-shape change)
     :owner (closed-object-shape (get-in change [:properties "owner"]))
     :form-owner (closed-object-shape defmethod-owner-schema)
     :expect (closed-object-shape (get-in change [:properties "expect"]))
     :aggregate-expect
     (closed-object-shape (get-in schema [:properties "expect"]))
     :expect-matched
     (closed-object-shape (get-in schema [:properties "expect_matched"]))
     :rename-binding
     (closed-object-shape (get-in change [:properties "rename_binding"]))
     :assoc-entry
     (closed-object-shape (get-in change [:properties "assoc_entry"]))}))

(def direct-change-contract
  (direct-contract-shape explicit-change-schema))

(defn editor-gesture-contract-shape
  "Project the closed one-shot editor gesture objects from public JSON Schema."
  [schema]
  (let [edit (get-in schema [:properties "edits" :items])
        deletion (get-in schema [:properties "delete_owners" :items])
        program (get-in schema [:properties "programs" :items])
        creation (get-in schema [:properties "create_files" :items])]
    {:request (update (closed-object-shape schema)
                      :allowed disj "workspace_root")
     :edit (closed-object-shape edit)
     :within (closed-object-shape (get-in edit [:properties "within"]))
     :deletion (closed-object-shape deletion)
     :creation (closed-object-shape creation)
     :program (closed-object-shape program)
     :program-expect
     (closed-object-shape (get-in program [:properties "expect"]))}))

(def editor-gesture-contract
  (editor-gesture-contract-shape editor-hybrid-schema))

;; @spec MCP-OP-ALIAS-002
;; @spec MCP-OP-ALIAS-003
(def alias-migration-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"op" {:type "string" :const "alias_migration"}
    "workspace_root" {:type "string" :minLength 1}
    "from"
    {:type "object"
     :additionalProperties false
     :properties {"lib" {:type "string" :minLength 1}
                  "var" {:type ["string" "null"] :minLength 1}}
     :required ["lib" "var"]}
    "to"
    {:type "object"
     :additionalProperties false
     :properties {"lib" {:type "string" :minLength 1}
                  "var" {:type ["string" "null"] :minLength 1}
                  "alias_policy" {:type "array"
                                  :minItems 1
                                  :items {:type "string" :minLength 1}}
                  "refer_policy" {:type "string"
                                  :enum ["preserve-refer" "alias-qualify"]}}
     :required ["lib" "var" "alias_policy"]}
    "scope"
    {:type "object"
     :additionalProperties false
     :properties {"paths" {:type "array"
                           :minItems 1
                           :items {:type "string" :minLength 1}}
                  "exclude" {:type "array"
                             :items {:type "string" :minLength 1}}}
     :required ["paths"]}
    "expect"
    {:type "object"
     :additionalProperties false
     :properties {"files" {:type "integer" :minimum 0}}
     :required ["files"]}
    "verify" {:type "string" :minLength 1}}
   :required ["from" "to" "scope" "expect"]})

(def alias-migration-output-schema
  {:type "object"
   :properties {"ok" {:type "boolean"}
                "elapsed_ms" {:type "number" :minimum 0}
                "files" {:type "integer" :minimum 0}
                "sites" {:type "integer" :minimum 0}
                "alias_histogram" {:type "object"}
                "collisions_resolved" {:type "integer" :minimum 0}
                "refer_sites" {:type "integer" :minimum 0}
                "lib_renamed" {:type ["object" "null"]}
                "details_path" {:type "string"}
                "details_retention" {:type "string"}
                "details_retained" {:type "integer" :minimum 0}}
   :required ["ok" "elapsed_ms"]})

;; @spec MCP-OP-HELPER-001
;; @spec MCP-OP-HELPER-002
;; @spec MCP-OP-HELPER-017
;; @spec MCP-OP-HELPER-025
(def helper-extraction-schema
  "The CLOSED field set. `additionalProperties false` at every level is what
  makes the request constant in the number of callers: a per-file, per-owner or
  per-site table has nowhere to arrive. `expect` is optional and is a strict
  guard when supplied."
  {:type "object"
   :additionalProperties false
   :properties
   {"op" {:type "string" :const "helper_extraction"}
    "workspace_root" {:type "string" :minLength 1}
    "from"
    {:type "object"
     :additionalProperties false
     :properties {"file" {:type "string" :minLength 1
                          :description "Project-relative path of the namespace the helpers leave."}}
     :required ["file"]}
    "helpers" {:type "array"
               :minItems 1
               :uniqueItems true
               :items {:type "string" :minLength 1}
               :description "The selected helper names, each resolving to exactly one top-level owner in from.file."}
    "to"
    {:type "object"
     :additionalProperties false
     :properties {"lib" {:type "string" :minLength 1
                         :description "The destination namespace. The created namespace equals this exactly and its path is project-relative."}
                  "alias_policy" {:type "array"
                                  :minItems 1
                                  :items {:type "string" :minLength 1}
                                  :description "Alias preferences in order; each rewritten file takes the first entry bound to nothing in that file."}}
     :required ["lib" "alias_policy"]}
    "scope"
    {:type "object"
     :additionalProperties false
     :properties {"paths" {:type "array"
                           :minItems 1
                           :items {:type "string" :minLength 1}
                           :description "Write-authorization subset of the admitted discovery roots. Discovery still runs over every admitted root; a supported reference outside these paths refuses."}}
     :required ["paths"]}
    "verification"
    {:type "object"
     :additionalProperties false
     :properties {"profile" {:type "string" :minLength 1
                             :description "One synchronous, rollback-capable, runnable configured profile. Validated before anything is staged."}}
     :required ["profile"]}
    "expect"
    {:type "object"
     :additionalProperties false
     :properties {"caller_files" {:type "integer" :minimum 0}}
     :required ["caller_files"]}}
   :required ["from" "helpers" "to" "scope" "verification"]})

;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-012
;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022
;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-010
;; @spec MCP-OP-HELPER-020
;; @spec MCP-OP-HELPER-022
(def helper-extraction-receipt-variants
  "THE COMPLETE PER-VARIANT MATRIX for one `helper_extraction` receipt.

  This is the source of truth and the schema is BUILT from it, so a variant
  cannot gain a face in prose that it lacks in the assertion, and an assertion
  cannot be added one example at a time. Every required field and every pinned
  constant of every face is here, in one place a reviewer can read end to end.

  `:constants` are PINNED, not merely typed: a field listed there must equal
  that exact value wherever it appears. `:absent` names fields the face must
  NOT carry, which is what discriminates a refusal (no `status` at all) from
  the four terminal states. `:required` names fields the face must carry.

  THE MATRIX

  | field                  | committed | verification-failed | verification-timeout | rollback-failed | refusal |
  |------------------------|-----------|---------------------|----------------------|-----------------|---------|
  | ok                     | =true R   | =false R            | =false R             | =false R        | =false R |
  | elapsed_ms             | R         | R                   | R                    | R               | R       |
  | operation              | =\"helper_extraction\" R (all five faces) ||||
  | status                 | =\"committed\" R | =\"verification-failed\" R | =\"verification-timeout\" R | =\"rollback-failed\" R | ABSENT |
  | kernel_status          | =\"committed\" R | =\"verification-failed\" R | =\"verification-timeout\" R | =\"rollback-failed\" R | -   |
  | committed              | =true R   | =false R            | =false R             | =false R        | =false  |
  | source_unchanged       | =false R  | =true R             | =true R              | =false R        | =true R |
  | restored               | -         | =true R             | =true R              | =false R        | -       |
  | destination_created    | =true R   | =false R            | =false R             | -               | -       |
  | source_retired         | R         | =0 R                | =0 R                 | ABSENT          | -       |
  | source_retired_unknown | -         | -                   | -                    | R               | -       |
  | planned_source_retired | -         | R                   | R                    | R               | -       |
  | undo_receipt           | R         | -                   | -                    | -               | -       |
  | receipt_hash           | R         | -                   | -                    | -               | -       |
  | restored_file_count    | -         | R                   | R                    | -               | -       |
  | restoration_read_back  | -         | R                   | R                    | -               | -       |
  | files                  | -         | -                   | -                    | R               | -       |
  | recovery_required      | -         | -                   | -                    | R               | -       |
  | helpers                | R         | -                   | -                    | -               | -       |
  | caller_files           | R         | -                   | -                    | -               | -       |
  | sites                  | R         | -                   | -                    | -               | -       |
  | verification           | R         | R                   | R                    | -               | -       |
  | error_type             | -         | -                   | -                    | -               | R       |
  | error                  | -         | -                   | -                    | -               | R       |
  | next_call              | -         | -                   | -                    | -               | =null R |
  | mutation_attempted     | -         | -                   | -                    | -               | =false R |
  | write_authority        | -         | -                   | -                    | -               | =false R |
  | source_retired_unknown | -         | -                   | -                    | =SENTENCE R     | -       |
  | helpers                | R         | R                   | R                    | R               | -       |
  | source_file            | R         | R                   | R                    | R               | -       |
  | closure                | R         | R                   | R                    | R               | -       |
  | destination_lib        | R         | R                   | R                    | R               | -       |
  | changed_files          | R         | -                   | -                    | -               | -       |
  | retained_sites         | R         | -                   | -                    | -               | -       |
  | alias_histogram        | R         | -                   | -                    | -               | -       |
  | partition              | R         | -                   | -                    | -               | -       |
  | planned_caller_files   | -         | R                   | R                    | R               | -       |
  | planned_changed_files  | -         | R                   | R                    | R               | -       |
  | planned_sites          | -         | R                   | R                    | R               | -       |
  | planned_retained_sites | -         | R                   | R                    | R               | -       |
  | planned_alias_histogram| -         | R                   | R                    | R               | -       |
  | planned_partition      | -         | R                   | R                    | R               | -       |

  NESTED SHAPES (`:objects`), asserted wherever the field appears:

  | field                 | required subkeys                     | pinned |
  |-----------------------|--------------------------------------|--------|
  | restoration_read_back | files, aggregate_sha256, manifest_in | manifest_in = \"details_path\" |
  | recovery_required     | receipt, reason, recovery            | -      |

  R = required · =x = pinned to that constant · ABSENT = must not be present ·
  - = permitted but not asserted by this face.

  WHY EACH PIN IS THERE, where it is not obvious:

  * `kernel_status` is pinned per face because the mapper emits it as
    `(name status)`; a receipt whose two words disagree describes two different
    transactions.
  * `committed` requires BOTH `undo_receipt` and `receipt_hash`: a receipt
    naming an undo document without the hash that binds it is an inverse nobody
    can prove they are applying to the right transaction.
  * the restored faces pin `source_retired` to 0 and require
    `planned_source_retired`: after a verified rollback nothing was retired, and
    the plan's number survives only under a name that says it was a plan.
  * `rollback-failed` keeps `files` and `recovery_required` — the one face with
    linear evidence, because a human must act on it — and FORBIDS
    `source_retired`, because how much of the source is still retired is
    genuinely unknown and must not be answered with a number in either
    direction; `source_retired_unknown` carries that instead.
  * the refusal face pins `source_unchanged` true and `committed` false, and
    requires `mutation_attempted` and `write_authority` pinned false: a refusal
    is raised before anything is staged, so a refusal claiming changed bytes, a
    commit, an attempted mutation or write authority describes a write that
    never happened.
  * `operation` is pinned on EVERY face. It is the one field naming which verb
    published the receipt, and a receipt that validates here while naming
    another operation is one this schema has no business blessing.
  * `verification` is required on all four TERMINAL faces, rollback-failed
    included: `terminal-receipt` always emits the typed verification map, and
    `{status \"unknown\"}` with no counts is itself the honest answer when
    there is no profile result. Its absence would mean the mapper was bypassed.
  * `source_retired_unknown` is PINNED to the mapper's one fixed sentence
    rather than merely typed. A stable code emitted by the mapper is the better
    shape and was offered; it would mean editing `mcp-helper-extraction`, and
    this round is schema-only. Pinning the sentence is the assertion available
    here — when the mapper gains a code, this const moves to it.
  * the plan projections are stated PER FACE because `plan-counts` projects
    them per face: `committed` carries the mutation counts under their plain
    names, every non-committed terminal face carries the same numbers under
    `planned_*`, and `helpers`, `source_file`, `closure` and `destination_lib`
    are unprefixed on both because they describe the request and the discovery
    rather than a completed write.

  THE NESTED SHAPES are asserted because a `recovery_required` of the wrong
  shape is a recovery authority a human cannot act on, and a
  `restoration_read_back` without its aggregate is the O(N) manifest this
  receipt exists to keep out."
  [{:title "committed"
    :description "The proof completed and the write stands. The plan's counts describe the tree as it now is, and undo_receipt plus receipt_hash are the authority this write can be inverted from."
    :constants {"operation" "helper_extraction"
                "status" "committed"
                "kernel_status" "committed"
                "ok" true
                "committed" true
                "source_unchanged" false
                "destination_created" true
                "source_file" 1}
    :objects {"verification"
              {:required ["status" "profile" "ok" "fresh_process"]
               :types {"status" {:type "string"} "profile" {:type "string"}
                       "ok" {:type "boolean"} "fresh_process" {:type "boolean"} "reason" {:type "string"}
                       "structural_callers" {:type "integer" :minimum 0} "helper_behaviors" {:type "integer" :minimum 0}
                       "compiled_callers" {:type "integer" :minimum 0}}
               ;; @spec MCP-OP-HELPER-022
               ;; the face has TWO shapes and they are discriminated by
               ;; `status`. Sharing one required set makes the honest not-run
               ;; face representable; without these alternatives it would also
               ;; make a not-run face carrying `structural_callers 28`
               ;; representable, which is the invented evidence 022 exists to
               ;; forbid. No code path emits that today — and "no code path
               ;; emits it" is the argument every one of these rounds has
               ;; rejected, so it is closed here instead.
               :alternatives
               [{:title "executed"
                 :required ["status"]
                 :properties {"status" {:enum ["checks-completed" "checks-failed"
                                               "unbacked-claim"]}}}
                {:title "not-run"
                 :required ["status" "ok" "fresh_process" "reason"]
                 :properties {"status" {:const "unknown"}
                              "ok" {:const false}
                              "fresh_process" {:const false}}
                 :forbidden ["structural_callers" "helper_behaviors"
                             "compiled_callers"]}]}
              "closure"
              {:required ["roots" "authorized_paths" "grammar"
                          "dynamic_references" "pruned_symlinks"]
               :constants {"dynamic_references" "not-claimed"
                           "grammar" "supported-libspecs-only"}
               :types {"roots" {:type "array" :items {:type "string"}} "authorized_paths" {:type "array" :items {:type "string"}}
                       "pruned_symlinks" {:type "integer" :minimum 0}}}
              "partition" {:required ["moved_only" "mixed" "qualified_only" "untouched"]
               :types {"moved_only" {:type "integer" :minimum 0} "mixed" {:type "integer" :minimum 0}
                       "qualified_only" {:type "integer" :minimum 0} "untouched" {:type "integer" :minimum 0}}}}
    :exactly-one [["details_path" "details_unavailable"]]
    :required ["ok" "elapsed_ms" "operation" "status" "kernel_status" "committed"
               "source_unchanged" "destination_created"
               "undo_receipt" "receipt_hash"
               "helpers" "source_retired" "caller_files" "sites"
               "source_file" "changed_files" "retained_sites"
               "alias_histogram" "partition" "closure" "destination_lib"
               "verification"]}

   {:title "verification-failed"
    :description "The proof did not pass and the kernel's inverse restored every protected byte. Nothing was retired: source_retired is the actual 0, and what the plan WOULD have done is under planned_*."
    :constants {"operation" "helper_extraction"
                "status" "verification-failed"
                "kernel_status" "verification-failed"
                "ok" false
                "committed" false
                "restored" true
                "source_unchanged" true
                "destination_created" false
                "source_retired" 0
                "source_file" 1}
    :objects {"restoration_read_back"
              {:required ["files" "aggregate_sha256" "manifest_in"]
               :constants {"manifest_in" "details_path"}
               :types {"files" {:type "integer" :minimum 0} "aggregate_sha256" {:type "string"}}}
              "verification"
              {:required ["status" "profile" "ok" "fresh_process"]
               :types {"status" {:type "string"} "profile" {:type "string"}
                       "ok" {:type "boolean"} "fresh_process" {:type "boolean"} "reason" {:type "string"}
                       "structural_callers" {:type "integer" :minimum 0} "helper_behaviors" {:type "integer" :minimum 0}
                       "compiled_callers" {:type "integer" :minimum 0}}
               ;; @spec MCP-OP-HELPER-022
               ;; the face has TWO shapes and they are discriminated by
               ;; `status`. Sharing one required set makes the honest not-run
               ;; face representable; without these alternatives it would also
               ;; make a not-run face carrying `structural_callers 28`
               ;; representable, which is the invented evidence 022 exists to
               ;; forbid. No code path emits that today — and "no code path
               ;; emits it" is the argument every one of these rounds has
               ;; rejected, so it is closed here instead.
               :alternatives
               [{:title "executed"
                 :required ["status"]
                 :properties {"status" {:enum ["checks-completed" "checks-failed"
                                               "unbacked-claim"]}}}
                {:title "not-run"
                 :required ["status" "ok" "fresh_process" "reason"]
                 :properties {"status" {:const "unknown"}
                              "ok" {:const false}
                              "fresh_process" {:const false}}
                 :forbidden ["structural_callers" "helper_behaviors"
                             "compiled_callers"]}]}
              "closure"
              {:required ["roots" "authorized_paths" "grammar"
                          "dynamic_references" "pruned_symlinks"]
               :constants {"dynamic_references" "not-claimed"
                           "grammar" "supported-libspecs-only"}
               :types {"roots" {:type "array" :items {:type "string"}} "authorized_paths" {:type "array" :items {:type "string"}}
                       "pruned_symlinks" {:type "integer" :minimum 0}}}
              "planned_partition" {:required ["moved_only" "mixed" "qualified_only" "untouched"]
               :types {"moved_only" {:type "integer" :minimum 0} "mixed" {:type "integer" :minimum 0}
                       "qualified_only" {:type "integer" :minimum 0} "untouched" {:type "integer" :minimum 0}}}}
    :exactly-one [["details_path" "details_unavailable"]]
    :required ["ok" "elapsed_ms" "operation" "status" "kernel_status" "committed"
               "restored" "source_unchanged" "destination_created"
               "source_retired" "restored_file_count" "restoration_read_back"
               "helpers" "source_file" "closure" "destination_lib"
               "planned_source_retired" "planned_caller_files"
               "planned_changed_files" "planned_sites" "planned_retained_sites"
               "planned_alias_histogram" "planned_partition"
               "verification"]}

   {:title "verification-timeout"
    :description "The proof did not return inside its profile's timeout and the kernel's inverse restored every protected byte. Identical obligations to verification-failed: a timeout is a proof that did not complete, never one that passed."
    :constants {"operation" "helper_extraction"
                "status" "verification-timeout"
                "kernel_status" "verification-timeout"
                "ok" false
                "committed" false
                "restored" true
                "source_unchanged" true
                "destination_created" false
                "source_retired" 0
                "source_file" 1}
    :objects {"restoration_read_back"
              {:required ["files" "aggregate_sha256" "manifest_in"]
               :constants {"manifest_in" "details_path"}
               :types {"files" {:type "integer" :minimum 0} "aggregate_sha256" {:type "string"}}}
              "verification"
              {:required ["status" "profile" "ok" "fresh_process"]
               :types {"status" {:type "string"} "profile" {:type "string"}
                       "ok" {:type "boolean"} "fresh_process" {:type "boolean"} "reason" {:type "string"}
                       "structural_callers" {:type "integer" :minimum 0} "helper_behaviors" {:type "integer" :minimum 0}
                       "compiled_callers" {:type "integer" :minimum 0}}
               ;; @spec MCP-OP-HELPER-022
               ;; the face has TWO shapes and they are discriminated by
               ;; `status`. Sharing one required set makes the honest not-run
               ;; face representable; without these alternatives it would also
               ;; make a not-run face carrying `structural_callers 28`
               ;; representable, which is the invented evidence 022 exists to
               ;; forbid. No code path emits that today — and "no code path
               ;; emits it" is the argument every one of these rounds has
               ;; rejected, so it is closed here instead.
               :alternatives
               [{:title "executed"
                 :required ["status"]
                 :properties {"status" {:enum ["checks-completed" "checks-failed"
                                               "unbacked-claim"]}}}
                {:title "not-run"
                 :required ["status" "ok" "fresh_process" "reason"]
                 :properties {"status" {:const "unknown"}
                              "ok" {:const false}
                              "fresh_process" {:const false}}
                 :forbidden ["structural_callers" "helper_behaviors"
                             "compiled_callers"]}]}
              "closure"
              {:required ["roots" "authorized_paths" "grammar"
                          "dynamic_references" "pruned_symlinks"]
               :constants {"dynamic_references" "not-claimed"
                           "grammar" "supported-libspecs-only"}
               :types {"roots" {:type "array" :items {:type "string"}} "authorized_paths" {:type "array" :items {:type "string"}}
                       "pruned_symlinks" {:type "integer" :minimum 0}}}
              "planned_partition" {:required ["moved_only" "mixed" "qualified_only" "untouched"]
               :types {"moved_only" {:type "integer" :minimum 0} "mixed" {:type "integer" :minimum 0}
                       "qualified_only" {:type "integer" :minimum 0} "untouched" {:type "integer" :minimum 0}}}}
    :exactly-one [["details_path" "details_unavailable"]]
    :required ["ok" "elapsed_ms" "operation" "status" "kernel_status" "committed"
               "restored" "source_unchanged" "destination_created"
               "source_retired" "restored_file_count" "restoration_read_back"
               "helpers" "source_file" "closure" "destination_lib"
               "planned_source_retired" "planned_caller_files"
               "planned_changed_files" "planned_sites" "planned_retained_sites"
               "planned_alias_histogram" "planned_partition"
               "verification"]}

   {:title "rollback-failed"
    :description "The inverse did NOT verify. The one face that keeps linear evidence, because a human has to act on it: files names what could not be restored, recovery_required carries the kernel's recovery authority, and the retirement is stated as unknown rather than as a number in either direction."
    :constants {"operation" "helper_extraction"
                "status" "rollback-failed"
                "kernel_status" "rollback-failed"
                "ok" false
                "committed" false
                "restored" false
                "source_unchanged" false
                "source_file" 1
                "source_retired_unknown" "the rollback did not verify, so how many owners the source still defines is not known from this receipt; read recovery_required"}
    :objects {"recovery_required"
              {:required ["receipt" "reason" "recovery"]
               :types {"receipt" {:type "string"} "reason" {:type "string"}
                       "recovery" {:type "object"}}}
              "verification"
              {:required ["status" "profile" "ok" "fresh_process"]
               :types {"status" {:type "string"} "profile" {:type "string"}
                       "ok" {:type "boolean"} "fresh_process" {:type "boolean"} "reason" {:type "string"}
                       "structural_callers" {:type "integer" :minimum 0} "helper_behaviors" {:type "integer" :minimum 0}
                       "compiled_callers" {:type "integer" :minimum 0}}
               ;; @spec MCP-OP-HELPER-022
               ;; the face has TWO shapes and they are discriminated by
               ;; `status`. Sharing one required set makes the honest not-run
               ;; face representable; without these alternatives it would also
               ;; make a not-run face carrying `structural_callers 28`
               ;; representable, which is the invented evidence 022 exists to
               ;; forbid. No code path emits that today — and "no code path
               ;; emits it" is the argument every one of these rounds has
               ;; rejected, so it is closed here instead.
               :alternatives
               [{:title "executed"
                 :required ["status"]
                 :properties {"status" {:enum ["checks-completed" "checks-failed"
                                               "unbacked-claim"]}}}
                {:title "not-run"
                 :required ["status" "ok" "fresh_process" "reason"]
                 :properties {"status" {:const "unknown"}
                              "ok" {:const false}
                              "fresh_process" {:const false}}
                 :forbidden ["structural_callers" "helper_behaviors"
                             "compiled_callers"]}]}
              "closure"
              {:required ["roots" "authorized_paths" "grammar"
                          "dynamic_references" "pruned_symlinks"]
               :constants {"dynamic_references" "not-claimed"
                           "grammar" "supported-libspecs-only"}
               :types {"roots" {:type "array" :items {:type "string"}} "authorized_paths" {:type "array" :items {:type "string"}}
                       "pruned_symlinks" {:type "integer" :minimum 0}}}
              "planned_partition" {:required ["moved_only" "mixed" "qualified_only" "untouched"]
               :types {"moved_only" {:type "integer" :minimum 0} "mixed" {:type "integer" :minimum 0}
                       "qualified_only" {:type "integer" :minimum 0} "untouched" {:type "integer" :minimum 0}}}}
    :exactly-one [["details_path" "details_unavailable"]]
    :absent ["source_retired"]
    :required ["ok" "elapsed_ms" "operation" "status" "kernel_status" "committed"
               "restored" "source_unchanged" "files" "recovery_required"
               "source_retired_unknown"
               "helpers" "source_file" "closure" "destination_lib"
               "planned_source_retired" "planned_caller_files"
               "planned_changed_files" "planned_sites" "planned_retained_sites"
               "planned_alias_histogram" "planned_partition"
               "verification"]}

   {:title "refusal"
    :description "A typed refusal. No terminal state was reached, so there is no status: the receipt carries the error_type, the cause, the one unresolved decision, and next_call — null in v1, always, because no refusal has a mechanically composable continuation. A REQUEST-SHAPE refusal may additionally carry `field` (the offending field path) and `example` (one complete, minimal, copy-paste-runnable request in the closed shape). Both are ALLOWED and neither is REQUIRED: a refusal about the tree has no offending field, and requiring an example would make every non-shape refusal invent one. They are typed in the outer properties map, so a refusal that carries them is still checked."
    :constants {"operation" "helper_extraction"
                "ok" false
                "committed" false
                "source_unchanged" true
                "mutation_attempted" false
                "write_authority" false
                "next_call" nil}
    :absent ["status"]
    :required ["ok" "elapsed_ms" "operation" "error_type" "error" "next_call"
               "source_unchanged" "committed"
               "mutation_attempted" "write_authority"]}])

(defn- receipt-variant->schema
  "One matrix row as one JSON-Schema branch.

  Deliberately trivial: constants become `const` properties, `:required`
  travels as it is, and `:absent` becomes `not {anyOf [{required [f]} ...]}` —
  \"carries NONE of these\". The obvious `not {required [a b]}` would say
  \"does not carry BOTH\", which admits a receipt carrying one of them, so the
  generalization is written out rather than reached for. The mapping has no
  judgement in it: the matrix above is the whole assertion, and nothing is
  added here that a reader of the matrix would not expect."
  [{:keys [title description constants absent required objects exactly-one]}]
  (cond-> {:title title
           :description description
           :properties (merge
                         (into {}
                               (map (fn [[field value]] [field {:const value}]))
                               constants)
                         ;; nested shapes: a field whose VALUE is an object
                         ;; with its own required subkeys and pinned subvalues
                         (into {}
                               (map (fn [[field shape]]
                                      [field
                                       (cond-> {:type "object"
                                                :required (vec (:required shape))}
                                         (seq (:alternatives shape))
                                         (assoc :oneOf
                                                (mapv
                                                  (fn [alternative]
                                                    (cond-> (select-keys
                                                              alternative
                                                              [:title :required :properties])
                                                      (seq (:forbidden alternative))
                                                      (assoc :not
                                                             {:anyOf
                                                              (mapv (fn [f] {:required [f]})
                                                                    (:forbidden alternative))})))
                                                  (:alternatives shape)))

                                         (or (seq (:constants shape))
                                             (seq (:types shape)))
                                         (assoc :properties
                                                ;; a pinned CONSTANT already
                                                ;; fixes the value, so it wins
                                                ;; over the looser type
                                                (merge
                                                  (:types shape)
                                                  (into {}
                                                        (map (fn [[sub value]]
                                                               [sub {:const value}]))
                                                        (:constants shape)))))]))
                               objects))
           :required (vec required)}
    ;; the declared shapes travel ON the branch as well as into its
    ;; `:properties`. JSON Schema 2020-12 ignores keywords it does not know, so
    ;; this changes no validation — what it buys is that the branch still SAYS
    ;; which of its fields are objects with required subkeys, in one readable
    ;; row, instead of only in a `:properties` entry a generator would have to
    ;; reverse-engineer. A witness that must reconstruct an assertion from the
    ;; schema's output is a witness that stops covering the next subkey.
    (seq objects) (assoc :objects objects)
    ;; @spec MCP-OP-HELPER-009
    ;; EXACTLY ONE of a group. A terminal receipt points at its external detail
    ;; artifact or says the artifact is not there; carrying both is two answers
    ;; to one question, and carrying neither is the silent absence a caller
    ;; reads as nothing-more-to-see.
    (seq exactly-one)
    (assoc :allOf (mapv (fn [group]
                          {:anyOf (mapv (fn [field]
                                          {:required [field]
                                           :not {:anyOf
                                                 (mapv (fn [other] {:required [other]})
                                                       (remove #{field} group))}})
                                        group)})
                        exactly-one)
           :exactly-one (vec exactly-one))
    (seq absent) (assoc :not {:anyOf (mapv (fn [field] {:required [field]})
                                           absent)})))

(def helper-extraction-output-schema
  "The receipt's declared shape, in all five faces it can wear.

  Counts and histograms only; never a file list. The verification map is TYPED
  — the executed profile and its three named checks — and never a bare coverage
  integer. `ok` and `elapsed_ms` are required on every published face; the rest
  is stated PER VARIANT in `:oneOf` below, so a caller validating a receipt
  learns which fields that particular face guarantees rather than reading this
  docstring and hoping.

  THE AUTHORITY IS `helper-extraction-receipt-variants`, the complete matrix of
  every required field and every pinned constant of every face. The prose below
  summarises it; the `:oneOf` branches are BUILT from it. Where the two could
  ever disagree, the matrix is what the schema actually asserts.

  THE FIVE FACES, and what each one must carry:

  * `committed` — `committed true`, `kernel_status \"committed\"`,
    `source_unchanged false`, `undo_receipt` (the RECOVERY AUTHORITY: the
    kernel receipt this write can be inverted from), `receipt_hash`,
    `details_path`, and the counts.
  * `verification-failed` / `verification-timeout` — the proof did not pass and
    the kernel's inverse RESTORED the tree: `restored true`,
    `source_unchanged true`, `destination_created false`,
    `restored_file_count` and an O(1) `restoration_read_back`
    `{files, aggregate_sha256, manifest_in}` whose per-file manifest is in the
    detail document at `details_path`, plus `cause_error` when a throw caused
    it.
  * `rollback-failed` — the inverse did NOT verify: `restored false`,
    `source_unchanged false`, `files` naming what could not be restored, and
    `recovery_required` carrying the kernel's recovery authority. This is the
    one face that keeps linear evidence, because a human has to act on it.
  * a typed REFUSAL — `ok false`, `error_type`, `error`, `next_call` (always
    null in v1), `source_unchanged`, and the refusal's bounded evidence.

  `elapsed_ms` IS required, on every face including a pre-staging refusal. The
  reviewer read the raw domain map, which a refusal returns without it; the
  PUBLISHED receipt is not that map. `mcp-operation/invoke!` closes the request
  clock around every outcome and stamps `elapsed_ms` on all of them, and every
  other public tool in this server declares it required for exactly that
  reason. Declaring it optional here would make this one verb's schema disagree
  with the receipt it actually publishes."
  {:type "object"
   :properties {"ok" {:type "boolean"}
                "operation" {:type "string"}
                "status" {:type "string"
                          :enum ["committed" "verification-failed"
                                 "verification-timeout" "rollback-failed"
                                 "unknown"]
                          :description "The terminal state. Absent on a refusal that never staged."}
                "kernel_status" {:type "string"
                                 :description "The transaction kernel's own outcome, retained separately from the verb's."}
                "committed" {:type "boolean"}
                "restored" {:type "boolean"
                            :description "The kernel's inverse restored every protected byte. Present only when a rollback was attempted."}
                "source_unchanged" {:type "boolean"
                                    :description "Claimed only alongside a verified restoration; never true after rollback-failed."}
                "restored_file_count" {:type "integer" :minimum 0
                                       :description "How many files the verified rollback restored. The manifest is in details_path."}
                "restoration_read_back"
                {:type "object"
                 :description "O(1) evidence for the restoration: file count and one aggregate digest over the read-back. The per-file manifest is in details_path."
                 :properties {"files" {:type "integer" :minimum 0}
                              "aggregate_sha256" {:type "string"}
                              "manifest_in" {:type "string"}}}
                "files" {:type "array" :items {:type "string"}
                         :description "rollback-failed ONLY: the files the inverse could not restore."}
                "recovery_required"
                {:type "object"
                 :description "rollback-failed ONLY: the kernel's recovery authority — the receipt to invert by hand and why the automatic inverse did not verify."
                 :properties {"receipt" {:type "string"}
                              "reason" {:type "string"}
                              "recovery" {:type "object"}}}
                "cause_error" {:type "string"
                               :description "The exception that ended a staged transaction, when a throw ended it."}
                "error_type" {:type "string"}
                "error" {:type "string"}
                "next_call" {:type ["object" "null"]
                             :description "Always null in v1: no refusal has a mechanically composable continuation."}
                "limitation" {:type "string"
                              :description "Present when the refusal names a kernel limitation rather than a caller decision."}
                "decision" {:type "string"
                            :description "The one unresolved decision the refusal hands back to the caller, in one sentence."}
                "field" {:type "string"
                         :description "REQUEST-SHAPE refusals only: the offending field path, dotted (\"to\", \"scope.paths\"). Optional -- a refusal about the tree rather than the request carries none."}
                "example" {:type "object"
                           :description "REQUEST-SHAPE refusals only: one complete, minimal, copy-paste-runnable request in the closed shape. Optional, and NEVER a continuation -- next_call stays null, because an example is edited and resent by the caller rather than mechanically replayed by a client."}
                "remedy" {:type "string"}
                "elapsed_ms" {:type "number" :minimum 0}
                "helpers" {:type "integer" :minimum 0}
                "source_retired" {:type "integer" :minimum 0}
                "destination_created" {:type "boolean"}
                "caller_files" {:type "integer" :minimum 0
                                :description "EXTERNAL callers rewritten; the source is not a caller of itself."}
                "source_file" {:type "integer" :minimum 0}
                "changed_files" {:type "integer" :minimum 0}
                "partition" {:type "object"}
                "sites" {:type "integer" :minimum 0}
                "retained_sites" {:type "integer" :minimum 0}
                "alias_histogram" {:type "object"}
                "verification" {:type "object"}
                "closure" {:type "object"
                           :description "The roots the walk actually admitted, the grammar closure is exact over, and the explicit statement that dynamic references are not claimed."}
                "details_path" {:type "string"
                                :description "Per-caller detail and the restoration manifest, published under the kernel's local-state receipt directory — never inside the workspace."}
                "undo_receipt" {:type "string"
                                :description "committed ONLY: the kernel receipt this write can be inverted from."}
                "receipt_hash" {:type "string"}
                "details_unavailable"
                {:type "string"
                 :description "Stated when the external detail artifact could not be written. An absent artifact is said out loud rather than left as a missing path, because a caller reads silence as nothing-more-to-see."}
                "source_retired_unknown"
                {:type "string"
                 :description "rollback-failed ONLY: how many owners the source still defines is not knowable from this receipt, stated as unknown rather than as a number in either direction."}
                "planned_source_retired" {:type "integer" :minimum 0}
                "planned_caller_files" {:type "integer" :minimum 0}
                "planned_changed_files" {:type "integer" :minimum 0}
                "planned_sites" {:type "integer" :minimum 0}
                "planned_retained_sites" {:type "integer" :minimum 0}
                "planned_alias_histogram" {:type "object"}
                "planned_partition"
                {:type "object"
                 :description "Every count that ASSERTS a completed mutation moves under planned_* the moment the write does not stand: after a verified rollback nothing was retired, rewritten or aliased, and a plan number left under its plain name would contradict restored true in the same object."}}
   :required ["ok" "elapsed_ms"]

   ;; @spec MCP-OP-HELPER-020
   ;; THE FIVE VARIANTS, DISCRIMINATED, BUILT FROM THE MATRIX. The branches
   ;; are not written here: they are derived from
   ;; `helper-extraction-receipt-variants`, so a face cannot acquire an
   ;; assertion in one place and lack it in another, and a reviewer reads one
   ;; table instead of five literals.
   ;;
   ;; Discrimination is by `status`: every TERMINAL receipt carries exactly one
   ;; of the four states and no refusal carries `status` at all, so the
   ;; branches are disjoint and `oneOf` is exact rather than a menu.
   ;;
   ;; `elapsed_ms` is required in EVERY branch. `mcp-operation/invoke!`'s
   ;; finalizer stamps it on every published result, refusals included, so a
   ;; variant that made it optional would describe a receipt this server never
   ;; emits.
   ;;
   ;; NOT a branch: the `status "unknown"` value `terminal-receipt` answers for
   ;; empty input. That is a MAPPER-internal honesty state and never a
   ;; published receipt — every `execute!` path returns one of the five below —
   ;; so admitting it here would declare a face the wire does not have.
   :oneOf (mapv receipt-variant->schema helper-extraction-receipt-variants)})
