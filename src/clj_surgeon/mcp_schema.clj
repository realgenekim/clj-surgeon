(ns clj-surgeon.mcp-schema)

(def verification-schema
  {:type "string" :enum ["fast" "full" "exact"]
   :description
   "Optional repository-owned verification profile. exact runs one project-declared exact-exit argv against staged bytes. Formatter, commands, and hot laws roll back on failure. A configured cold job returns verification_complete=false plus one inspect next_call."})

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
(def helper-extraction-output-schema
  "The receipt's declared shape, in all five faces it can wear.

  Counts and histograms only; never a file list. The verification map is TYPED
  — the executed profile and its three named checks — and never a bare coverage
  integer. `ok` is the only universally required property: a PRE-STAGING refusal
  is answered before the request clock has anything to report, so requiring
  `elapsed_ms` of it would declare a field the honest refusal does not carry.

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
                "receipt_hash" {:type "string"}}
   :required ["ok" "elapsed_ms"]})
