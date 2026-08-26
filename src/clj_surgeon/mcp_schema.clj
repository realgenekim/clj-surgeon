(ns clj-surgeon.mcp-schema)

(def verification-schema
  {:type "string" :enum ["fast" "full"]
   :description
   "Optional repository-owned verification profile. Formatter, commands, and hot laws roll back on failure. A configured cold job returns verification_complete=false plus one inspect next_call."})

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
                :description "Exact named top-level owners. Use a string for a unique Var or {kind:defmethod,name,dispatch} for one multimethod implementation."
                :items {:oneOf [{:type "string" :minLength 1}
                                defmethod-owner-schema]}}
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
       "delete" {:type "boolean" :enum [true]
                 :description "Delete every exact named owner in forms, including attached comments and separators. Does not use find or the semantic provider."}
       "insert_before" {:type "array" :minItems 1
                        :description "Insert complete Clojure forms before a nested find match, or omit find to insert before exactly one named top-level forms owner. Comment-bearing gaps refuse."
                        :items {:type "string" :minLength 1}}
       "insert_after" {:type "array" :minItems 1
                       :description "Insert complete Clojure forms after a nested find match, or omit find to insert after exactly one named top-level forms owner. Comment-bearing gaps refuse."
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
     :description "Aggregate transaction cardinality. All three counts are required."
     :properties
     {"changes" (assoc positive-integer-schema :description "Number of change objects.")
      "edits" (assoc positive-integer-schema :description "Total exact replacements.")
      "files" (assoc positive-integer-schema :description "Total files that must change.")}
     :required ["changes" "edits" "files"]}
    "verify" verification-schema}
   :required ["changes" "expect"]})

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
                 :description "Exactly one named Clojure owner or the complete syntax-tree root. EDN requires root scope."
                 :properties
                 {"form" {:type "string" :minLength 1
                          :description "One exact named top-level form."}
                  "namespace" {:type "string" :minLength 1
                               :description "One exact namespace name for editing its ns form."}
                  "root" {:type "boolean" :enum [true]
                          :description "Search the complete concrete syntax tree while preserving all bytes outside exact replacements."}}
                 :oneOf [{:required ["form"]}
                         {:required ["namespace"]}
                         {:required ["root"]}]}
       "from" {:type "string" :minLength 1
               :description "The exact old Clojure subtree. Its count must equal matches, which defaults to one."}
       "to" {:type "string" :minLength 1
             :description "The replacement Clojure subtree."}
       "matches" positive-integer-schema}
      :required ["within" "from" "to"]
      :oneOf [{:required ["file"]
               :not {:required ["files"]}}
              {:required ["files"]
               :not {:required ["file"]}}]}}
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

(def editor-hybrid-schema
  (-> editor-gesture-schema
      (assoc-in [:properties "programs"] editor-programs-schema)
      (assoc :anyOf [{:required ["edits"]}
                     {:required ["programs"]}
                     {:required ["delete_owners"]}])))

(def editor-tool-schema
  (-> editor-hybrid-schema
      (update :properties dissoc "verify")))

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
      "require_policy" {:type "string" :enum ["minimal" "copy-all"]}
      "caller_changes" (assoc (get-in explicit-change-schema
                                      [:properties "changes"])
                              :minItems 0)
      "ignored_caller_files"
      {:type "array" :uniqueItems true
       :items {:type "string" :minLength 1}}
      "expect"
      {:type "object"
       :additionalProperties false
       :properties
       {"forms" positive-integer-schema
        "caller_edits" {:type "integer" :minimum 0}
        "files" positive-integer-schema}
       :required ["forms" "caller_edits" "files"]}}
     :required ["file" "to" "forms" "require_policy" "caller_changes"
                "ignored_caller_files" "expect"]}
    "verify" verification-schema}
   :required ["extraction"]})

(def clj-change-schema
  {:type "object"
   :additionalProperties false
   :properties (merge (:properties basis-change-schema)
                      (:properties explicit-change-schema)
                      (:properties editor-hybrid-schema)
                      (:properties extraction-schema))
   :oneOf
   [{:required ["basis" "decisions"]
     :not {:anyOf [{:required ["changes"]} {:required ["expect"]}
                   {:required ["edits"]} {:required ["programs"]}
                   {:required ["delete_owners"]} {:required ["extraction"]}]}}
    {:required ["changes" "expect"]
     :not {:anyOf [{:required ["basis"]}
                   {:required ["decisions"]}
                   {:required ["edits"]}
                   {:required ["programs"]}
                   {:required ["delete_owners"]}
                   {:required ["extraction"]}]}}
    {:anyOf [{:required ["edits"]}
             {:required ["programs"]}
             {:required ["delete_owners"]}]
     :not {:anyOf [{:required ["basis"]}
                   {:required ["decisions"]}
                   {:required ["changes"]}
                   {:required ["expect"]}
                   {:required ["extraction"]}]}}
    {:required ["extraction"]
     :not {:anyOf [{:required ["basis"]}
                   {:required ["decisions"]}
                   {:required ["changes"]}
                   {:required ["edits"]}
                   {:required ["programs"]}
                   {:required ["delete_owners"]}
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
        program (get-in schema [:properties "programs" :items])]
    {:request (update (closed-object-shape schema)
                      :allowed disj "workspace_root")
     :edit (closed-object-shape edit)
     :within (closed-object-shape (get-in edit [:properties "within"]))
     :deletion (closed-object-shape deletion)
     :program (closed-object-shape program)
     :program-expect
     (closed-object-shape (get-in program [:properties "expect"]))}))

(def editor-gesture-contract
  (editor-gesture-contract-shape editor-hybrid-schema))
