(ns clj-surgeon.mcp-inspect-tool
  "Imperative MCP shell for one-read project-confined Clojure inspection."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-extraction-plan :as extraction-plan]
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-prepared-confirmation :as prepared-confirmation]
   [clj-surgeon.mcp-prepared-request :as prepared-request]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-source-anchor :as source-anchor]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.parallel :as parallel]
   [clj-surgeon.quoted-var-refs :as quoted-var-refs]
   [clj-surgeon.structural-lens :as structural-lens]
   [clj-surgeon.study :as study]
   [clojure.string :as str]))

(def tool-description
  (str
    "Read Clojure structure in one bounded snapshot. Batch all known forms, "
    "outlines, exact structural matches, and X-ray requests. Use "
    "include_source=false on forms requests when only names, ranges, counts, "
    "and hashes are needed; omit it when exact form source is needed. Use "
    "mode=plan-extraction when exact roots and destination are known but caller migration evidence is not; review its required public_forms, copy its hash-bound next_call, and fill caller decisions. Use mode=prepare-change when one Var or related Var set names the goal but "
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
    "edit, then call apply_clojure_changes once. The whole request remains "
    "refused on every failure. A forms owner-selection refusal names the failed "
    "request and every failed owner, supplies the complete bounded name-only "
    "owner vocabulary, and ranks up to ten hypotheses per missing owner. "
    "After a selector-local failure, a continuation preserves completed sibling "
    "results and SHA-256 guards for every original file. Copy "
    "continuation.retry_template.arguments, replace every declared null selector "
    "hole with one exact owner, and submit that complete guarded request. Do not "
    "reconstruct aggregate expect or reread completed siblings. "
    "For structure questions grep answers wrong, use the study operations: "
    "requests items deps, topo, ls-deps, and ls-extract answer call-graph, "
    "ordering, transitive-dependency, and minimal-extractable-closure "
    "questions for one file (ls-deps and ls-extract need an exact form), and "
    "top-level mode=ls-tree returns the directory-wide namespace map for an "
    "optional project-relative dir. format=names (default when grep is "
    "absent) is a compact {file, ns, form_count, line_count} table of "
    "contents sized to fit a whole tree in one receipt; format=text "
    "(default when grep is present) or format=edn give the fuller per-form "
    "view. grep filters by file CONTENTS (ripgrep, can match comments and "
    "strings); ns_grep filters by each file's PATH/namespace instead and "
    "answers 'table of contents filtered to namespaces matching X'. "
    "Every study receipt is bounded to `limit` payload characters — 8192 by "
    "default for mode=ls-tree, 4096 for an atomic study operation; a "
    "larger result sets truncated=true and read_complete=false and returns an "
    "executable next_call, so raise limit up to 16384 or narrow the scope "
    "instead of re-reading. Every guarded file is verified before evaluation; "
    "a changed guard refuses without source or write authority. Hypotheses are "
    "never selection authority, and continuation is never write authority. "
    "read_complete=true is "
    "terminal. Never writes."))

(def ^:private positive-integer-schema {:type "integer" :minimum 1})
(def ^:private non-negative-integer-schema {:type "integer" :minimum 0})
(def ^:private snapshot-guards-schema
  {:type "object"
   :minProperties 1
   :maxProperties inspect/max-files
   :additionalProperties {:type "string" :pattern "^[0-9a-f]{64}$"}})
(def ^:private continuation-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"snapshot_bound" {:const true}
    "selector_authority" {:const false}
    "write_authority" {:const false}
    "completed_request_count" non-negative-integer-schema
    "completed_request_ids" {:type "array" :items {:type "string"}}
    "pending_request_count" positive-integer-schema
    "pending_request_ids" {:type "array" :items {:type "string"}}
    "snapshot_guards" snapshot-guards-schema
    "completed_results" {:type "array"}
    "retry_template"
    {:type "object"
     :additionalProperties false
     :properties
     {"executable" {:const false}
      "snapshot_bound" {:const true}
      "selector_authority" {:const false}
      "write_authority" {:const false}
      "arguments"
      {:type "object"
       :additionalProperties false
       :properties
       {"workspace_root" {:type "string" :minLength 1}
        "snapshot_guards" snapshot-guards-schema
        "requests" {:type "array" :minItems 1 :maxItems inspect/max-requests}
        "expect" {:type "object"
                  :additionalProperties false
                  :properties {"requests" positive-integer-schema
                               "files" positive-integer-schema}
                  :required ["requests" "files"]}}
       :required ["workspace_root" "snapshot_guards" "requests" "expect"]}
      "holes"
      {:type "array"
       :minItems 1
       :items {:type "object"
               :additionalProperties false
               :properties
               {"path" {:type "array" :minItems 4}
                "request_id" {:type "string" :minLength 1}
                "kind" {:const "exact-top-level-owner"}
                "rejected_value" {:type "string" :minLength 1}
                "must_replace" {:const true}
                "authority" {:const false}}
               :required ["path" "request_id" "kind" "rejected_value"
                          "must_replace" "authority"]}}}
     :required ["executable" "snapshot_bound" "selector_authority"
                "write_authority" "arguments" "holes"]}}
   :required ["snapshot_bound" "selector_authority" "write_authority"
              "completed_request_count" "completed_request_ids"
              "pending_request_count" "pending_request_ids"
              "snapshot_guards" "completed_results" "retry_template"]})
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
     {"id" {:type "string" :minLength 1
            :description "Optional call-local correlation ID. Supply IDs on every request or omit them from every request."}
      "operation" {:const operation}
      "file" source-file-schema}
     properties)
   :required (into ["operation" "file"] required)})

(def ^:private forms-request-properties
  {"forms" {:type "array" :minItems 1 :maxItems inspect/max-forms
            :uniqueItems true
            :items {:type "string" :minLength 1}
            :description (str "Exact unqualified top-level owner names. A multimethod "
                              "collapses every arm to one name here; read its per-arm "
                              "dispatch from an outline row's dispatch field, then address "
                              "one arm with apply_clojure_changes changes[].forms "
                              "{kind: \"defmethod\", name, dispatch}.")}
   "include_source" {:type "boolean" :default true
                     :description "Set false for metadata-only form reads; line ranges, source character counts, hashes, and source anchors remain, while exact source bodies are omitted. Omit for exact source."}
   "expect" {:type "object" :additionalProperties false
             :properties {"forms" positive-integer-schema}
             :required ["forms"]}})

(def ^:private outline-request-properties
  {"include_string_symbols"
   {:type "boolean" :default false
    :description (str "Set true to add bounded JS-ish declarations found in "
                      "Clojure string literals to each outline row. Omit to "
                      "preserve the ordinary outline response exactly.")}})

(def ^:private study-limit-properties
  {"limit" {:type "integer" :minimum 1 :maximum 16384 :default 4096
            :description (str "Maximum receipt payload characters for this study "
                              "request. A larger result returns truncated=true "
                              "with an executable next_call.")}})

(def ^:private study-form-properties
  (assoc study-limit-properties
         "form"
         {:type "string" :minLength 1
          :description (str "One exact top-level form name. Required for ls-deps "
                            "and ls-extract; optional for deps, where it narrows "
                            "the call graph to one owner.")}))

;; Outline rows for defmethod owners always carry `dispatch`, the exact source
;; spelling of that arm's dispatch value. @spec MCP-OP-DISPATCH-001

(defn- operationless-forms-request
  []
  {:type "object"
   :additionalProperties false
   :properties
   (merge
     {"id" {:type "string" :minLength 1
            :description "Optional call-local correlation ID. Supply IDs on every request or omit them from every request."}
      "file" source-file-schema}
     forms-request-properties)
   :required ["file" "forms" "expect"]})

(def typed-inspect-schema
  ;; @spec MCP-OP-READ-NORM-001
  ;; @spec MCP-OP-READ-NORM-004
  ;; @spec MCP-OP-READ-NORM-005
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "snapshot_guards"
    (assoc snapshot-guards-schema
           :description (str "Optional stateless retry fence mapping every requested file, plus any completed sibling files, "
                             "to the SHA-256 returned by a prior selector-local continuation. Every requested file must be present. "
                             "All guarded files are captured and verified before request evaluation; mismatch refuses without source or continuation."))
    "requests"
    {:type "array"
     :minItems 1
     :maxItems inspect/max-requests
     :description "Ordered all-or-nothing structural read requests. IDs must be unique when supplied; supply all or omit all."
     :items
     {:oneOf
      [(request-base "forms" forms-request-properties ["forms" "expect"])
       (operationless-forms-request)
       (request-base "outline" outline-request-properties [])
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
         ["expression"])
       (request-base "deps" study-form-properties [])
       (request-base "topo" study-limit-properties [])
       (request-base "ls-deps" study-form-properties ["form"])
       (request-base "ls-extract" study-form-properties ["form"])]}}
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

(def extraction-plan-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "mode" {:type "string" :enum ["prepare-change" "plan-extraction"]}
    "file" source-file-schema
    "to" source-file-schema
    "forms" {:type "array" :minItems 1 :uniqueItems true
             :items {:type "string" :minLength 1}}
    "require_policy" {:type "string" :enum ["minimal" "copy-all"]}}
   :required ["mode" "file" "to" "forms" "require_policy"]})

;; ============================================================
;; ls-tree mode — the one directory-scoped study operation
;; ============================================================
;; Directory-scoped, so it cannot be a `requests` item: every request item is
;; keyed by one project-relative FILE and participates in expect.files and
;; snapshot_guards. It therefore takes the shape the contract already reserves
;; for whole-project reads, a top-level `mode`, exactly like plan-extraction.

;; @spec MCP-OP-STUDY-037
(def ls-tree-default-limit
  "How many payload characters one `ls-tree` receipt carries when the caller
  names no limit.

  Raised from 4096 after the E6-Lb measurement: `format=text` is the default
  once `grep` is present, and at 4096 a real ten-file `src` came back as 2 of
  10 files with `read_complete=false` — the caller's first call answered
  almost nothing, and a table of contents that needs a second call is not a
  table of contents. 8192 admits a whole small tree (a ten-file, thirty-form
  tree renders in 2,064 characters; twenty-five files in 5,279) while keeping
  the text a text-only client renders inside the 8 KB one-call budget. The
  ceiling is unchanged: a genuinely large tree still truncates and says so."
  8192)

;; @spec MCP-OP-STUDY-038
(def ls-tree-max-limit
  "The highest `limit` a study receipt may ask for.

  A boundary, not a target: a tree whose complete rendering fits comes back
  complete, and the next file over comes back truncated with a remedy naming
  what to do instead. Measured on the toy fixture the witnesses use: 77 files
  render in 16,370 characters and fit; the seventy-eighth does not."
  16384)

(def ls-tree-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string" :minLength 1
                      :description "Optional canonical absolute workspace root. Omit to use the server default. Preserve the returned workspace_root in follow-up calls."}
    "mode" {:type "string" :const "ls-tree"}
    "dir" {:type "string" :minLength 1
           :description "Project-relative directory to scan. Omit or use \".\" for the workspace root. Absolute paths and parent traversal refuse."}
    "grep" {:type "string" :minLength 1
            :description "Optional ripgrep pattern matched against file CONTENTS; only projects and files with a matching line are outlined. Can match comments, strings, and unrelated substrings — for a namespace/path filter use ns_grep instead."}
    "ns_grep" {:type "string" :minLength 1
               :description "Optional pattern matched against each file's PATH, which the Clojure require convention keeps in lockstep with its declared namespace ('_' and '-' are treated as equivalent). Narrower than grep: answers 'table of contents filtered to namespaces matching X' without matching mentions in comments or strings. Composes with grep, narrowing further."}
    "format" {:type "string" :enum ["names" "text" "edn"]
              :description "names (the default when grep is absent) is a compact table of contents — one {file, ns, form_count, line_count} row per file, sized to fit a whole tree in one bounded receipt. text (the default when grep is present) is the fuller compact per-form scanning view. edn is one fully detailed row per file. All three share the same bound/truncation contract."}
    "limit" {:type "integer" :minimum 1 :maximum 16384 :default 8192
             :description "Maximum receipt payload characters. A larger tree returns truncated=true with an executable next_call."}
    "max_files" {:type "integer" :minimum 1 :maximum 20000 :default 2000
                 :description "Maximum number of source files DISCOVERY may find before refusing with study-tree-too-large. Independent of limit, which bounds the receipt: this bounds the work done before any receipt exists. Raise it only for a tree you actually intend to scan whole."}}
   :required ["mode"]})

(def ^:private ls-tree-outline-chunk
  "How many additional files one bounded-parallel outlining batch parses.

  The receipt grows a file at a time, but the files it needs are parsed in
  batches so the loop pays for one worker pool per batch rather than one per
  file. It is an upper bound on the overshoot: at most this many files beyond
  the last that fit are ever parsed."
  16)

(defn- ls-tree-render
  [projects dir output-format total]
  (case output-format
    "edn"   (inspect/json-data (study/format-ls-tree-edn-entries projects dir))
    "names" (inspect/json-data (study/format-ls-tree-names projects dir))
    ;; The true discovered count travels with the text rendering so its total
    ;; line cannot contradict the receipt's own file_count.
    (study/format-ls-tree-text projects dir {:file-count total})))

;; @spec MCP-OP-STUDY-035
(defn- ls-tree-unresolved-paths
  "Declared source directories the scan could not walk, shaped for the wire.

  The kernel's reason is a keyword; a receipt carries the name. An empty list
  is omitted entirely rather than sent as `[]`, so the key's presence is
  itself the signal that something was skipped."
  [scan]
  (mapv (fn [{:keys [project path reason]}]
          {:project project :path path :reason (name reason)})
        (:paths-unresolved scan)))

(defn- ls-tree-payload-size
  [payload output-format]
  (if (contains? #{"edn" "names"} output-format)
    (inspect/json-character-count payload)
    (count payload)))

;; @spec MCP-OP-STUDY-015
;; @spec MCP-OP-STUDY-030
;; @spec MCP-OP-STUDY-032
(defn- ls-tree-bounded
  "Grow the receipt one file at a time and stop at the first overflow, parsing
  only the files the receipt can actually carry.

  Discovery returns names; outlining opens and PARSES. Before this the kernel
  outlined the whole tree before any bound applied — 1072 files and 618 MB of
  heap to return three. Files are parsed here in bounded-parallel batches of
  `ls-tree-outline-chunk`, so the number ever parsed is at most one chunk
  beyond the number returned."
  [projects dir output-format limit total]
  (let [cache (atom {})
        attempt (fn [n map-fn]
                  (let [kept (study/outline-take projects n cache map-fn)
                        payload (ls-tree-render kept dir output-format total)]
                    {:returned n
                     :omitted (- total n)
                     :truncated (< n total)
                     :payload payload
                     :fits? (<= (ls-tree-payload-size payload output-format)
                                limit)}))
        ;; The floor. Below it no bound can go: `names`/`edn` bottom out at
        ;; the empty array's two characters (MCP-OP-STUDY-018), and `text`
        ;; bottoms out at its trailing total line plus, when discovery found
        ;; more than one project, one `0 shown` header per project. That total
        ;; line is `36 + 2 x digits(file_count)` characters — 38 for a
        ;; one-digit tree, 40 for two digits, 42 for three, 46 at the 20,000
        ;; ceiling — because `shown` is 0 and `omitted` equals `file_count`,
        ;; so the count is spelled twice. (The number here read "38 characters
        ;; for a two-digit tree", which is a one-digit tree's floor attached
        ;; to the wrong width.) At `limit 1` the text payload is therefore
        ;; LARGER than the limit, by
        ;; construction and not by accident: the alternative is a receipt that
        ;; reports nothing about what it left out, or one whose body
        ;; contradicts its own `project_count`.
        ;;
        ;; The projects are carried through `outline-take` at n = 0 rather
        ;; than rendered from an empty vector. Rendering `[]` here was the one
        ;; file below MCP-OP-STUDY-024's fix: every `n >= 1` attempt kept all
        ;; projects and only `returned = 0` dropped them, so the smallest
        ;; receipt was the one that contradicted itself.
        empty-receipt {:returned 0
                       :omitted total
                       :truncated (pos? total)
                       :payload (ls-tree-render
                                  (study/outline-take projects 0 (atom {}))
                                  dir output-format total)}]
    (loop [best empty-receipt
           fitting 0]
      (let [batch-end (min total (+ fitting ls-tree-outline-chunk))]
        (if (= fitting batch-end)
          best
          ;; One bounded-parallel batch, then the exact answer inside it.
          (let [batch (attempt batch-end parallel/bounded-map)]
            (if (:fits? batch)
              (recur (dissoc batch :fits?) batch-end)
              ;; Every file in this batch is cached now, so walking it costs
              ;; renders, not parses.
              (loop [n (inc fitting)
                     best best]
                (if (> n batch-end)
                  best
                  (let [candidate (attempt n map)]
                    (if (:fits? candidate)
                      (recur (inc n) (dissoc candidate :fits?))
                      best)))))))))))

;; @spec MCP-OP-STUDY-023
(defn- ls-tree-next-call
  "One continuation, carrying EVERY field the request carried unless an
  override replaces it.

  `:limit` was the one field left out, so a caller who spelled the default
  (`limit 4096`) made the self-returning continuation reappear: the identical-
  call check compared a request that named its limit against a continuation
  that did not, saw two different calls, and served back the call that had
  just failed. A field a request supplies is part of that request's identity
  whether or not it happens to equal a default."
  [params overrides]
  {:tool "inspect_clojure"
   :arguments (merge (cond-> {:mode "ls-tree"}
                       (:dir params) (assoc :dir (:dir params))
                       (:grep params) (assoc :grep (:grep params))
                       (:ns_grep params) (assoc :ns_grep (:ns_grep params))
                       (:format params) (assoc :format (:format params))
                       (:limit params) (assoc :limit (:limit params))
                       (:max_files params) (assoc :max_files (:max_files params)))
                     overrides)})

;; @spec MCP-OP-STUDY-019
(def ^:private ls-tree-formats
  "The complete `format` vocabulary. The ls-tree branch skips
  `validate-inspect-params`, so this is checked here or nowhere."
  #{"names" "text" "edn"})

;; @spec MCP-OP-STUDY-019
(def ^:private ls-tree-fields
  "The complete ls-tree parameter vocabulary, checked server-side. The JSON
  schema declares `additionalProperties false`, but a schema is a contract
  with a well-behaved client, not a server-side check."
  #{:mode :dir :grep :ns_grep :format :limit :max_files :workspace_root})

;; @spec MCP-OP-STUDY-022
(def ^:private ls-tree-parameter-types
  "The JSON type each ls-tree parameter must carry, checked server-side.

  The published schema's `type` is a contract with a well-behaved client, not
  a check: `grep: 5` sailed past the `^-` guard — `str/starts-with?`
  stringifies its argument — reached ripgrep as the pattern \"5\", and came
  back in a receipt whose `grep` was an integer. That violates this tool's own
  OUTPUT schema, so the caller saw `isError` and no `error_type` at all.
  `limit: \"x\"` did the same through `invalid-study-limit`, which echoed the
  string back into an integer-typed field."
  {:mode {:pred string? :expected "string"}
   :dir {:pred string? :expected "string"}
   :grep {:pred string? :expected "string"}
   :ns_grep {:pred string? :expected "string"}
   :format {:pred string? :expected "string"}
   :limit {:pred integer? :expected "integer"}
   :max_files {:pred integer? :expected "integer"}
   :workspace_root {:pred string? :expected "string"}})

(defn- json-type-name
  [value]
  (cond
    (nil? value) "null"
    (boolean? value) "boolean"
    (string? value) "string"
    (integer? value) "integer"
    (number? value) "number"
    (sequential? value) "array"
    (map? value) "object"
    :else "unknown"))

;; @spec MCP-OP-STUDY-022
(defn- ls-tree-type-errors
  "Every supplied ls-tree parameter whose JSON type is wrong, named with what
  was expected and what arrived. Values are never echoed: the whole failure
  mode being fixed is a wrongly typed value reaching the receipt."
  [params]
  (vec (for [[key {:keys [pred expected]}] (sort-by key ls-tree-parameter-types)
             :let [value (get params key)]
             :when (and (contains? params key)
                        (some? value)
                        (not (pred value)))]
         {:parameter (name key)
          :expected expected
          :actual (json-type-name value)})))

(defn- ls-tree-request-arguments
  "The arguments of the call just made, shaped as a continuation is.

  EVERY supplied field counts — an unknown or rejected key makes a request
  genuinely different from a continuation that drops it — except
  `workspace_root`, which a continuation never carries, and counting it would
  make every routed request differ from its own continuation."
  [params]
  (merge {:mode "ls-tree" :dir "."} (dissoc params :workspace_root)))

;; @spec MCP-OP-STUDY-007
(defn- ls-tree-refusal
  "A typed ls-tree refusal, with a continuation only when replaying it could
  differ from the call that just failed.

  The `{:dir \".\"}` continuation was unconditional, so `grep` at the root
  handed back the exact request just made — `no-clojure-files` at `\".\"`
  proposing `no-clojure-files` at `\".\"`. Narrowing is a caller judgment
  there, exactly as at the receipt ceiling.

  A caller may supply the continuation when the corrective move is known —
  a rejected `format` is dropped rather than echoed back."
  ([params error-type message extra]
   (ls-tree-refusal params error-type message extra
                    (ls-tree-next-call params {:dir "."})))
  ([params error-type message extra continuation]
   (let [repeats? (= (:arguments continuation)
                     (ls-tree-request-arguments params))]
    (merge
      (cond-> {:ok false
               :operation "inspect_clojure"
               :mode "ls-tree"
               :error_type (name error-type)
               :error message
               :read_complete false
               :source_unchanged true
               :next_action (if repeats? "narrow_scope" "correct_request")}
        (not repeats?) (assoc :next_call continuation))
      extra))))

;; @spec MCP-OP-STUDY-001
;; @spec MCP-OP-STUDY-006
;; @spec MCP-OP-STUDY-007
;; @spec MCP-OP-STUDY-022
;; @spec MCP-OP-STUDY-025
;; @spec MCP-OP-STUDY-026
(defn execute-ls-tree
  "Run the one study kernel over a workspace-confined directory."
  [{:keys [project-root]} params]
  (let [dir (or (:dir params) ".")
        ;; names is the default when the caller has not already narrowed the
        ;; scan with grep; grep already trims the file set, so text (the
        ;; fuller per-form view) stays the default once grep is present.
        output-format (or (:format params) (if (:grep params) "text" "names"))
        limit (or (:limit params) ls-tree-default-limit)
        max-files (or (:max_files params) study/default-max-scan-files)
        root (mcp-paths/real-root project-root)
        unknown-fields (vec (sort (map name (remove ls-tree-fields (keys params)))))
        type-errors (ls-tree-type-errors params)
        resolved (when (string? dir) (mcp-paths/resolve-directory-path root dir))]
    (cond
      ;; The ls-tree branch never reaches `validate-inspect-params`, so its own
      ;; vocabulary is checked here or nowhere. Before this, an unknown key was
      ;; silently ignored and an unknown `format` fell through the render
      ;; `case` to text while the receipt echoed the raw string back.
      (seq unknown-fields)
      (ls-tree-refusal
        params :unknown-parameter
        "inspect_clojure ls-tree received an unknown parameter"
        ;; `:dir` only when it IS a string: this branch runs before the type
        ;; check, and an integer `dir` in the receipt is the very defect the
        ;; type check exists to stop.
        (cond-> {:unknown unknown-fields
                 :supported (vec (sort (map name ls-tree-fields)))
                 :remedy "Remove the unknown parameter; the ls-tree vocabulary is fixed."}
          (string? dir) (assoc :dir dir))
        (ls-tree-next-call params {}))

      ;; Beside the format enum, and before anything interprets a parameter:
      ;; a wrongly typed value cannot be scanned with, and must never reach the
      ;; receipt, where it breaks the tool's own output schema.
      (seq type-errors)
      (ls-tree-refusal
        params :invalid-parameter-type
        "inspect_clojure ls-tree received a parameter of the wrong type"
        (cond-> {:invalid type-errors
                 :remedy (str "Send each parameter with its declared JSON "
                              "type; grep and ns_grep are strings, limit and "
                              "max_files are integers.")}
          (string? dir) (assoc :dir dir))
        ;; The corrective move is known, so the continuation drops the
        ;; rejected values instead of handing them straight back.
        (ls-tree-next-call
          (apply dissoc params (map (comp keyword :parameter) type-errors))
          {}))

      (and (contains? params :format)
           (not (contains? ls-tree-formats (:format params))))
      (ls-tree-refusal
        params :invalid-format
        "Expected format to be one of names, text, or edn"
        {:dir dir
         :format (:format params)
         :supported (vec (sort ls-tree-formats))
         :remedy "Use format=names, format=text, or format=edn."}
        ;; The corrective move is known, so the continuation drops the
        ;; rejected value instead of handing it straight back.
        (ls-tree-next-call (dissoc params :format) {}))

      (not (:ok resolved))
      (ls-tree-refusal
        params
        (keyword (:error_type resolved))
        (:error resolved)
        {:dir dir
         :remedy (str "Use an existing directory inside the configured project "
                      "root, or \".\" for the root itself.")})

      (not (and (integer? limit) (pos? limit) (<= limit ls-tree-max-limit)))
      (ls-tree-refusal params :invalid-study-limit
                       "Expected a study limit between 1 and 16384 characters"
                       {:dir dir :limit limit})

      (not (and (integer? max-files) (pos? max-files)
                (<= max-files study/max-scan-files-ceiling)))
      (ls-tree-refusal params :invalid-max-files
                       (format (str "Expected a discovery cap between 1 and %d "
                                    "files")
                               study/max-scan-files-ceiling)
                       {:dir dir :max_files max-files})

      :else
      (let [scan (study/ls-tree (cond-> {:dir (:path resolved)
                                         ;; What the CALLER named. The kernel
                                         ;; scans the canonical realpath, which
                                         ;; must never appear in a message.
                                         :dir-label dir
                                         :max-files max-files}
                                  (:grep params) (assoc :grep (:grep params))
                                  (:ns_grep params) (assoc :ns-grep (:ns_grep params))))]
        (if-not (:ok scan)
          (ls-tree-refusal params
                           (:error-type scan)
                           (:error scan)
                           (cond-> {:dir dir
                                    :remedy (or (:remedy scan)
                                                (if (or (:grep params) (:ns_grep params))
                                                  "Widen or drop grep/ns_grep, or scan a parent directory."
                                                  "Scan a directory that contains Clojure sources."))}
                             (:file-count scan) (assoc :file_count (:file-count scan))
                             (:max-files scan) (assoc :max_files (:max-files scan))
                             (contains? scan :observed-at-least)
                             (assoc :observed_at_least
                                    (:observed-at-least scan))
                             (seq (:paths-unresolved scan))
                             (assoc :paths_unresolved
                                    (ls-tree-unresolved-paths scan))
                             (:match-budget scan) (assoc :match_budget
                                                         (:match-budget scan))
                             (:grep params) (assoc :grep (:grep params))
                             (:ns_grep params) (assoc :ns_grep (:ns_grep params)))
                           ;; A rejected PATTERN gets the treatment a rejected
                           ;; `format` already got: the corrective move is
                           ;; known, so the continuation drops the value the
                           ;; refusal just named instead of handing it back to
                           ;; be sent again. Everything else keeps the
                           ;; scan-a-parent-directory continuation.
                           (case (:error-type scan)
                             :invalid-grep-pattern
                             (ls-tree-next-call (dissoc params :grep) {})
                             :invalid-ns-grep-pattern
                             (ls-tree-next-call (dissoc params :ns_grep) {})
                             ;; A pattern refused for what it COSTS is as
                             ;; unsendable as one refused for not compiling.
                             :ns-grep-match-budget-exceeded
                             (ls-tree-next-call (dissoc params :ns_grep) {})
                             (ls-tree-next-call params {:dir "."})))
          (let [projects (:projects scan)
                total (:file-count scan)
                bounded (ls-tree-bounded projects (:path resolved)
                                         output-format limit total)]
            (cond->
              {:ok true
               :operation "inspect_clojure"
               :mode "ls-tree"
               :read_complete (not (:truncated bounded))
               :dir dir
               :grep (:grep params)
               :ns_grep (:ns_grep params)
               :format output-format
               :limit limit
               :project_count (count projects)
               :file_count total
               :returned (:returned bounded)
               :omitted (:omitted bounded)
               :truncated (:truncated bounded)
               :next_action (cond
                              (not (:truncated bounded)) "none"
                              (< limit ls-tree-max-limit)
                              "raise_limit_or_narrow_scope"
                              :else "narrow_scope")}
              (contains? #{"edn" "names"} output-format) (assoc :files (:payload bounded))
              (= "text" output-format) (assoc :tree (:payload bounded))
              ;; A successful receipt still says what it could not reach: a
              ;; project with one real source directory and one symlinked one
              ;; answers, and the symlinked declaration is invisible unless
              ;; the receipt names it.
              (seq (:paths-unresolved scan))
              (assoc :paths_unresolved (ls-tree-unresolved-paths scan))
              ;; An executable continuation only while raising the limit can
              ;; still advance; a narrower dir or grep is a caller judgment.
              (and (:truncated bounded) (< limit ls-tree-max-limit))
              (assoc :next_call
                     (ls-tree-next-call params {:limit ls-tree-max-limit}))
              (and (:truncated bounded) (>= limit ls-tree-max-limit))
              (assoc :remedy
                     (str "The receipt is already at the maximum limit; scan a "
                          "subdirectory or add a grep pattern.")))))))))

;; @spec MCP-OP-STUDY-042
(def ^:private max-owner-list-characters
  "How much of an `available_owners` list the text prints before it says how
  much it printed. The list is evidence, so it is bounded rather than dropped."
  2048)

;; @spec MCP-OP-STUDY-042
(defn- owner-list-line
  "The owners a refusal's receipt lists, printed where a text-only client
  reads them.

  Field evidence (O2 re-review, 2026-09-03): this line was nested inside a
  `diagnostic?` guard that a study refusal never satisfies, while the sentence
  explaining how to USE the list was guarded only by the list being non-empty.
  A refusal therefore told a caller how to choose among owners it never
  showed. The two are one decision and now share one condition."
  [result]
  (when-let [owners (seq (:available_owners result))]
    (let [returned (or (:available_owners_returned result) (count owners))
          total (or (:available_owner_count result) returned)
          joined (str/join ", " owners)
          shown (if (<= (count joined) max-owner-list-characters)
                  joined
                  (str (subs joined 0 max-owner-list-characters) " …"))]
      (format "  available owners (%d/%d%s): %s"
              returned total
              (if (:available_owners_truncated result) "; truncated" "")
              shown))))

;; @spec MCP-OP-STUDY-042
(def refusal-structural-keys
  "Refusal keys the text already renders as STRUCTURE — the header, the cause,
  the owner evidence, the continuation coaching, the payload echo, and the
  transport fields no caller acts on.

  Everything else a refusal carries is rendered as a `key: value` detail line.
  The set is written down here, and the default is to RENDER: a new refusal
  field is carried into the text the day it is added, and only a deliberate
  entry in this set can keep it out. Field evidence (O2 re-review): the
  opposite default — an allow-list of fields to print — is how seven of nine
  modes came to refuse with a category name and an arrow."
  #{:ok :operation :mode :error :error_type :error-type :reason
    :next_action :next-action :next_call :remedy :remedies
    :read_complete :source_unchanged :source-unchanged :basis-retained
    :elapsed_ms :inspection_elapsed_ms :workspace_root :file_read_count
    :available_owners :available_owner_count :available_owners_returned
    :available_owners_truncated :available_owners_omitted
    :failed_request :failures :selection_failures :form_candidates
    :candidates_truncated :hypotheses_truncated :continuation
    :file_hashes :results :dir :grep :ns_grep :format :limit})

(def ^:private refusal-detail-order
  "The order the most-used detail lines print in. Keys outside it follow, in
  name order, so the block is stable across runs and readable across kinds."
  [:path :failed_stage :request_id :request_index :form :missing :unknown
   :supported :expected :actual :scope :required :limits :maximum :minimum])

(def ^:private max-refusal-detail-characters 512)

;; @spec MCP-OP-STUDY-046
(def max-refusal-cause-characters
  "How much of a refusal's `:error` cause the STRUCTURAL line spells before it
  is bounded with a typed marker naming the original length.

  Public because a witness has to assert AT the bound rather than about the
  constant. Field evidence (Sol O2 round-2 review, section 5): a synthetic
  10,000-character path was bounded at 512 characters in its detail line and
  then repeated in full as the cause — `cause_unbounded= true text_chars=
  10612` — so the refusal text was bounded by the caller's input."
  512)

;; @spec MCP-OP-STUDY-046
(defn- bounded-cause
  "A refusal's cause, bounded, with a typed marker naming its true length.

  Field evidence (Sol O2 round-2 review, section 5): a synthetic
  10,000-character path was bounded at 512 characters in its detail line and
  then repeated IN FULL as the cause — 10,612 characters of text — so the one
  string a caller cannot control the size of was the one the text did not
  bound."
  [cause]
  (if (<= (count cause) max-refusal-cause-characters)
    cause
    (str (subs cause 0 max-refusal-cause-characters)
         " … (" (count cause) " characters)")))

;; @spec MCP-OP-STUDY-042
(defn- refusal-detail-lines
  [result]
  (let [keys-present (remove refusal-structural-keys (keys result))
        ordered (concat (filter (set keys-present) refusal-detail-order)
                        (sort (remove (set refusal-detail-order) keys-present)))]
    (into []
          (map (fn [key]
                 (let [value (get result key)
                       rendered (if (string? value)
                                  value
                                  (json/generate-string value))
                       bounded (if (<= (count rendered)
                                       max-refusal-detail-characters)
                                 rendered
                                 (str (subs rendered 0
                                            max-refusal-detail-characters)
                                      " … (" (count rendered) " characters)"))]
                   (str "  " (name key) ": " bounded))))
          ordered)))

;; @spec MCP-OP-STUDY-042
(defn- refusal-structural-text
  "The refusal's structural rendering: the type, the CAUSE, the enumerated
  detail lines, the owner evidence, the remedy, and the next action.

  The `ls-tree` refusal branch already had this shape and the generic branch
  did not, so seven of nine modes refused with an error type and an arrow. A
  caller cannot act on a category name."
  [result extra-lines]
  (let [error-type (let [value (or (:error_type result) (:error-type result))]
                     (when value
                       (if (keyword? value) (name value) value)))
        reason (let [value (:reason result)]
                 (when value (if (keyword? value) (name value) value)))
        labels (distinct (remove nil? [error-type reason]))]
  (str/join
    "\n"
    (remove
      nil?
      (concat
        [(format "inspect_clojure%s\n  refused · %s · %s"
                 (if (:mode result) (str " · " (:mode result)) "")
                 (if (seq labels)
                   (str/join " · " labels)
                   "unknown-error")
                 (mcp-operation/format-elapsed-ms
        (mcp-operation/request-elapsed-ms result)))
         (when (:error result) "")
         ;; @spec MCP-OP-STUDY-046
         ;; The cause is bounded like every other refusal detail, with a
         ;; marker naming its original length. The COMPLETE cause still
         ;; travels, under the ordinary receipt-fact bound, so a refusal
         ;; text is bounded by a constant rather than by the caller's input.
         (when (:error result) (str "  " (bounded-cause (:error result))))]
        (refusal-detail-lines result)
        extra-lines
        [(when (:remedy result) "")
         (when (:remedy result) (str "→ " (:remedy result)))
         (when (:next_call result)
           (str "\n→ next call: " (:tool (:next_call result)) " "
                (json/generate-string (:arguments (:next_call result)))))
         (format "\n→ %s" (or (:next_action result) "correct_request"))])))))

;; @spec MCP-OP-STUDY-042
;; @spec MCP-OP-STUDY-044
(defn refusal-text
  "The complete text block for one refusal: its structural rendering, plus a
  bounded `path: value` line for every receipt leaf that rendering does not
  already carry.

  Field evidence (Sol O2 round-2 review, section 6): the round-2 ratchet
  treated every member of `refusal-structural-keys` as rendered without
  proving any renderer consumed it, so naming a new refusal fact in that set
  kept it out of the text with the whole suite green. Membership now decides
  only WHERE a fact is rendered, never WHETHER — the escape is
  unrepresentable rather than merely detected."
  [result extra-lines]
  (let [structural (refusal-structural-text result extra-lines)
        ;; @spec MCP-OP-STUDY-044
        ;; @spec MCP-OP-STUDY-040
        ;; DERIVED, never fixed. A refusal renders no rows, so the whole
        ;; allowance the fit imposes is the receipt-fact allowance; with
        ;; nothing imposed the complete rendering travels. Field evidence
        ;; (Sol O2 round-3 review, section 4): a fixed 8,192-character
        ;; allowance dropped `error`, `path` and four more leaves out of a
        ;; 21,847-byte result under a 32,768-byte budget, and declared the
        ;; drop as though the budget had forced it.
        facts (inspect/fact-section
                (inspect/fact-block
                  structural result
                  (or (:text_evidence_limit result)
                      inspect/unbounded-evidence)))]
    (if facts (str structural "\n\n" facts) structural)))

;; @spec MCP-OP-STUDY-036
;; @spec MCP-OP-STUDY-045
(defn- ls-tree-continuation-line
  "The continuation spelled for a client that reads only text.

  `next_call` is structured data. A text-only client sees `structuredContent`
  never, so a receipt that carries its continuation only there tells such a
  caller that it was truncated and nothing about what to send instead.

  It is spelled as the VERBATIM executable request — the tool, then the
  compact JSON argument object — identically to the typed path in
  `mcp-inspect/continuation-line`. Field evidence (Sol O2 round-2 review,
  section 4): this rendered `mode=ls-tree dir=. format=text limit=16384`,
  which is neither a JSON tool-argument object nor a shell command, so it was
  retypeable guidance while the typed modes published something a caller
  could paste. The fixed argument order the prose form existed for is now the
  JSON object's own key order, which the receipt already fixes."
  [result]
  (when-let [call (:next_call result)]
    (str "→ next call: " (:tool call) " "
         (json/generate-string (inspect/json-data (:arguments call))))))

;; @spec MCP-OP-STUDY-036
(defn- ls-tree-payload-text
  "The rows the bounded receipt carries, rendered for the text block.

  `text` travels as the already-rendered tree; `names` and `edn` travel as
  data and are rendered as the compact JSON a caller would otherwise have had
  to read out of `structuredContent`."
  [result]
  (cond
    (string? (:tree result)) (str/trim (:tree result))
    (contains? result :files) (json/generate-string (:files result))
    :else nil))

;; @spec MCP-OP-STUDY-040
(defn- abridged-tree-text
  "The longest whole-line prefix of a rendered tree inside `limit`."
  [tree limit]
  (let [lines (str/split-lines tree)]
    (loop [remaining lines kept [] used 0]
      (if-let [line (first remaining)]
        (let [next-used (+ used (count line) 1)]
          (if (and (seq kept) (> next-used limit))
            {:text (str/join "\n" kept) :shown (count kept)
             :total (count lines) :abridged true}
            (recur (next remaining) (conj kept line) next-used)))
        {:text (str/join "\n" kept) :shown (count kept)
         :total (count lines) :abridged false}))))

;; @spec MCP-OP-STUDY-040
(defn- abridged-files-text
  "The longest whole-entry prefix of a `names`/`edn` payload inside `limit`.

  Entries are dropped whole rather than cut mid-object: a caller must be able
  to parse what it is handed, and half a JSON object is not evidence."
  [files limit]
  (loop [n (count files)]
    (let [rendered (json/generate-string (subvec (vec files) 0 n))]
      (if (or (<= (count rendered) limit) (<= n 1))
        {:text rendered :shown n :total (count files) :abridged (< n (count files))}
        (recur (dec n))))))

;; @spec MCP-OP-STUDY-040
(defn- ls-tree-payload-block
  "The bounded payload the text block renders, and whether it was abridged.

  `:text_evidence_limit` is set by `fit-public-result` when the complete
  public result — text AND structured content together — would cross
  `max-public-result-bytes`. Without it the payload travels whole."
  [result]
  (let [limit (:text_evidence_limit result)]
    (cond
      (nil? limit)
      (when-let [payload (ls-tree-payload-text result)]
        {:text payload :abridged false})

      (string? (:tree result))
      (abridged-tree-text (str/trim (:tree result)) limit)

      (contains? result :files)
      (abridged-files-text (:files result) limit)

      :else nil)))

;; @spec MCP-OP-STUDY-036
(defn ls-tree-summary
  "The text block a client renders for one `ls-tree` result.

  Field evidence (E6-Lb, PF-4): this returned a 146-character header with
  zero rows while the whole table of contents sat in `structuredContent.tree`
  — so an agent on a text-only client was handed the shape of an answer and
  none of it, and had no way to know what to send next. The rows travel here
  now, bounded by exactly the same `limit` that bounds the payload, and a
  truncated receipt spells its continuation or its remedy in the text."
  [result]
  (if-not (:ok result)
    ;; @spec MCP-OP-STUDY-042
    (refusal-text (assoc result :mode "ls-tree") nil)
    (let [block (ls-tree-payload-block result)
          payload (:text block)
          structural
          (str/join
        "\n"
        (remove
          nil?
          [(format "inspect_clojure · ls-tree\n  %s · %d project%s · %d of %d file%s · %s"
                   (:dir result)
                   (:project_count result)
                   (if (= 1 (:project_count result)) "" "s")
                   (:returned result)
                   (:file_count result)
                   (if (= 1 (:file_count result)) "" "s")
                   (mcp-operation/format-elapsed-ms
        (mcp-operation/request-elapsed-ms result)))
           (when (seq payload) "")
           (when (seq payload) payload)
           ""
           ;; @spec MCP-OP-STUDY-040
           (when (:abridged block)
             (format (str "! text abridged · %d of %d row%s rendered · the "
                          "complete receipt is in structuredContent")
                     (:shown block) (:total block)
                     (if (= 1 (:total block)) "" "s")))
           (when (:abridged block)
             (str "→ lower limit, narrow dir, or add a grep pattern so the "
                  "complete result fits the public output budget"))
           ;; @spec MCP-OP-STUDY-040
           ;; One line, one claim. Field evidence (Sol O2 round-2 review,
           ;; section 2): this block printed `! text abridged · 97 of 200
           ;; rows rendered` and `✓ complete tree · read_complete=true`
           ;; together, so a text-only caller was told in one breath that
           ;; rows had been dropped and that it held the complete tree.
           (cond
             (:truncated result)
             ;; @spec MCP-OP-STUDY-044
             ;; Spelled from the receipt, never as a constant: the literal
             ;; `read_complete=true` is the label form the carriage predicate
             ;; looks for, and a constant that agrees with the receipt makes
             ;; the leaf look carried by a line that never read it.
             (format "! bounded receipt · %d file%s omitted · read_complete=%s"
                     (:omitted result)
                     (if (= 1 (:omitted result)) "" "s")
                     (inspect/leaf-spelling (:read_complete result)))

             (:abridged block)
             (str "! receipt complete · read_complete="
                  (inspect/leaf-spelling (:read_complete result))
                  " · this text is not")

             :else
             (str "✓ complete tree · read_complete="
                  (inspect/leaf-spelling (:read_complete result))))
           (ls-tree-continuation-line result)
           (when (and (:truncated result) (:remedy result))
             (str "→ " (:remedy result)))
           (format "→ %s" (:next_action result))]))
          ;; @spec MCP-OP-STUDY-044
          ;; Everything the tree rows do not already carry — `format`,
          ;; `limit`, `truncated`, the counts — prints as a bounded
          ;; `path: value` line, so the text is a superset of the receipt
          ;; here exactly as it is in every typed mode.
          facts (inspect/fact-section
                  (inspect/fact-block
                    structural result
                    (if (:text_evidence_limit result)
                      (max 0 (- (:text_evidence_limit result)
                                (count (or payload ""))))
                      inspect/unbounded-evidence)))]
      (if facts (str structural "\n\n" facts) structural))))

(def inspect-schema
  {:type "object"
   :additionalProperties false
   :properties (assoc (merge (:properties typed-inspect-schema)
                             (:properties prepare-change-schema)
                             (:properties basis-view-schema)
                             (:properties verification-job-schema)
                             (:properties extraction-plan-schema)
                             (:properties ls-tree-schema))
                      ;; One merged mode vocabulary; each oneOf branch below
                      ;; pins its own const.
                      "mode"
                      {:type "string"
                       :enum ["prepare-change" "plan-extraction" "ls-tree"]})
   :oneOf [{:required ["requests" "expect"]}
           {:properties {"mode" {:const "prepare-change"}}
            :required ["mode" "subject" "intent"]}
           {:properties {"mode" {:const "prepare-change"}}
            :required ["mode" "subjects" "intent"]}
           {:properties {"mode" {:const "prepare-change"}}
            :required ["mode" "file" "form" "intent"]}
           {:properties {"mode" {:const "plan-extraction"}}
            :required ["mode" "file" "to" "forms" "require_policy"]}
           {:properties {"mode" {:const "ls-tree"}}
            :required ["mode"]}
           {:required ["basis" "view" "open"]}
           {:required ["verification_job" "view"]}]})

;; @spec MCP-OP-SCHEMA-001
;; @spec MCP-OP-PREP-REQ-009
;; @spec MCP-OP-PREP-ACT-001
(def ^:private prepared-confirmation-output-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"descriptor_sha256" {:type "string" :pattern "^[0-9a-f]{64}$"}
    "expires_in_ms" {:type "integer" :enum [300000]}
    "session_bound" {:type "boolean" :enum [true]}
    "commit_single_use" {:type "boolean" :enum [true]}
    "executable" {:type "boolean" :enum [false]}
    "write_authority" {:type "boolean" :enum [false]}}
   :required ["descriptor_sha256" "expires_in_ms" "session_bound"
              "commit_single_use" "executable" "write_authority"]})

;; @spec MCP-OP-STUDY-035
(def ^:private paths-unresolved-item-schema
  "One `paths_unresolved` entry: a declared source directory `ls-tree`
   discovery could not walk. `unresolved-source-dir` (study.clj) names only
   `:symlink` today; the enum is deliberately narrow rather than an open
   string so a caller can distinguish a documented skip class from a typo
   the way `error_type` already lets it."
  {:type "object"
   :additionalProperties false
   :properties
   {"project" {:type "string"}
    "path" {:type "string"}
    "reason" {:type "string" :enum ["symlink"]}}
   :required ["project" "path" "reason"]})

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
    "snapshot_guards" snapshot-guards-schema
    "continuation" continuation-schema
    "source_character_count" {:type "integer"}
    "failed_request" {:type "object"}
    "failure_count" {:type "integer"}
    "requested_form_count" {:type "integer"}
    "resolved_form_count" {:type "integer"}
    "failures" {:type "array"}
    "available_form_count" {:type "integer"}
    "failed_stage" {:type "string"}
    "file_hash" {:type "string"}
    "available_owner_count" {:type "integer"}
    "available_owners" {:type "array"}
    "available_owners_returned" {:type "integer"}
    "available_owners_omitted" {:type "integer"}
    "available_owners_truncated" {:type "boolean"}
    "selection_failures" {:type "array"}
    "form_candidates" {:type "array"}
    "candidate_limit" {:type "integer"}
    "candidates_truncated" {:type "boolean"}
    "next_action" {:type "string"}
    "basis" {:type "string"}
    "surface" {:type "array"}
    "decision-site-ids" {:type "array"}
    "decision-sites" {:type "array"}
    "buffers" {:type "array"}
    "verification_job" {:type "string"}
    "verification_complete" {:type "boolean"}
    "elapsed_ms" {:type "number" :minimum 0}
    "inspection_elapsed_ms" {:type "number" :minimum 0}
    "job_elapsed_ms" {:type "number" :minimum 0}
    "next_call" {:type "object"}
    "mode" {:type "string"}
    "dir" {:type "string"}
    "grep" {:type ["string" "null"]}
    "ns_grep" {:type ["string" "null"]}
    "max_files" {:type "integer"}
    "match_budget" {:type "integer"}
    "observed_at_least" {:type "boolean"}
    "paths_unresolved" {:type "array" :items paths-unresolved-item-schema}
    "remedy" {:type "string"}
    "format" {:type "string"}
    "limit" {:type "integer"}
    "project_count" {:type "integer"}
    "returned" {:type "integer"}
    "omitted" {:type "integer"}
    "truncated" {:type "boolean"}
    "tree" {:type "string"}
    "files" {:type "array"}
    "prepared_request" prepared-request/prepared-request-schema
    "prepared_confirmation" prepared-confirmation-output-schema}
   :required ["ok" "operation" "elapsed_ms"]
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
           "  %s surface sites · %s decisions · %s files · basis %s · %s\n\n"
           "%s\n"
           "%s\n"
           "✓ source hashes independently verified\n"
           "✓ decision source attached once in structured content\n"
           "→ fill next_call decisions, then call apply_clojure_changes once")
      (:surface-site-count result)
      (:site-count result)
      (:file-count result)
      (:basis result)
      (mcp-operation/format-elapsed-ms
        (mcp-operation/request-elapsed-ms result))
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
           "  %s buffers · %s source characters · 0 file reads · %s\n\n"
           "%s\n"
           "✓ rendered from the immutable basis snapshot\n"
           "✓ exact named-form source attached once in structured content\n"
           "→ decide, or open another retained site")
      (:buffer-count result)
      (:source-character-count result)
      (mcp-operation/format-elapsed-ms
        (mcp-operation/request-elapsed-ms result))
      buffer-lines)))

(defn extraction-plan-summary
  [result]
  (format
    (str "inspect_clojure · plan-extraction\n"
         "  %s forms · %s caller candidates · %s quoted Vars · %s public forms · %s\n\n"
         "✓ source snapshot frozen\n"
         "✓ no mutation authority retained\n"
         "→ review visibility, fill caller decisions, then call apply_clojure_changes once")
    (get-in result [:plan :form-count])
    (get-in result [:evidence_counts :caller_candidates :returned])
    (get-in result [:evidence_counts :quoted_var_references :returned])
    (count (get-in result [:plan :required-public-forms]))
    (mcp-operation/format-elapsed-ms
        (mcp-operation/request-elapsed-ms result))))

(defn verification-job-summary
  "Render one bounded cold-verification job without repeating its full output."
  [result]
  (let [status (:status result)
        terminal? (true? (:verification_complete result))
        passed? (true? (:passed result))
        clock-summary
        (if-let [job-elapsed-ms (:job_elapsed_ms result)]
          (str "request "
               (mcp-operation/format-elapsed-ms
        (mcp-operation/request-elapsed-ms result))
               " · job "
               (mcp-operation/format-elapsed-ms job-elapsed-ms))
          (str "request "
               (mcp-operation/format-elapsed-ms
        (mcp-operation/request-elapsed-ms result))))]
    (format
      (str "inspect_clojure · cold verification\n"
           "  %s · %s · %s\n\n"
           "%s\n"
           "→ %s")
      (:verification_job result)
      (name status)
      clock-summary
      (cond
        (not terminal?) "… bounded job still running"
        passed? "✓ cold verification passed"
        :else "✗ cold verification failed; edit remains committed and undo receipt is available")
      (if terminal?
        (:next_action result)
        "call next_call once after doing other useful work"))))

;; @spec MCP-OP-STUDY-040
(defn- with-envelope
  "Every result the budget gate SUBSTITUTES carries the envelope of the result
  it replaces.

  The gate runs on the FINALIZED result, so a substitute it builds from
  scratch would reach the publisher with no clock at all — and the publisher
  adds nothing back, which is exactly the property that retired the 64-byte
  publish reserve (Sol O2 round-3 review, section 5). Carrying the envelope
  keeps the bytes measured and the bytes published the same bytes.

  WHICH keys are the envelope is `mcp-operation`'s to say, not this
  namespace's. Field evidence (Opus O2 round-4 review, 2026-09-04, section
  7): this copied `:elapsed_ms` by name, so the MEM-003 landing that nests
  the clock under `measured` would have made every substitute drop it
  silently — a gate that loses the envelope of the result it replaces is
  measuring one thing and publishing another."
  [substitute measured]
  (merge substitute (mcp-operation/envelope measured)))

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
        (with-envelope
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
                     [{:message "Open fewer retained site IDs in one call."}])}
          result)))))

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

;; @spec MCP-OP-READ-GUARD-001
(defn capture-snapshots
  "Resolve and read each distinct canonical file once in first-reference order.

  `read-source` is injectable for boundary tests and receives a canonical path."
  ([project-root requests read-source expected-file-count]
   (capture-snapshots project-root requests read-source expected-file-count nil))
  ([project-root requests read-source expected-file-count snapshot-guards]
   (try
     (let [root (mcp-paths/real-root project-root)
           request-files (vec (distinct (map :file requests)))
           files (vec (distinct (concat request-files (keys snapshot-guards))))
           resolved (mapv #(mcp-paths/resolve-source-path root %) files)
           refusal (first (remove :ok resolved))]
       (if refusal
         (cond-> refusal
           snapshot-guards
           (assoc :failed_stage "snapshot"
                  :read_complete false
                  :source_unchanged true))
         (let [request-resolved (subvec resolved 0 (count request-files))
               canonical-count (count (distinct (map #(str (:canonical %))
                                                     request-resolved)))
               aliases (when snapshot-guards
                         (->> resolved
                              (group-by #(str (:canonical %)))
                              vals
                              (filter #(< 1 (count (distinct (map :relative %)))))
                              first))]
           (cond
             (not= expected-file-count canonical-count)
             {:ok false
              :operation "inspect_clojure"
              :error_type "aggregate-file-expectation-mismatch"
              :error "Declared file count does not match distinct canonical files"
              :expected expected-file-count
              :actual canonical-count
              :read_complete false
              :source_unchanged true
              :next_action "correct_request"}

             aliases
             {:ok false
              :operation "inspect_clojure"
              :error_type "snapshot-guard-alias-collision"
              :error "Several supplied paths resolve to the same canonical file"
              :files (mapv :relative aliases)
              :failed_stage "snapshot"
              :read_complete false
              :source_unchanged true
              :next_action "correct_request"}

             :else
             (let [captured
                   (loop [remaining resolved
                          cache {}
                          snapshots (array-map)
                          reads 0]
                     (if-let [{:keys [relative path canonical]} (first remaining)]
                       (let [cache-key (str canonical)]
                         (if-let [snapshot (get cache cache-key)]
                           (recur (next remaining) cache
                                  (assoc snapshots relative
                                         (assoc snapshot :file relative))
                                  reads)
                           (let [source (read-source path)]
                             (when-not (string? source)
                               (throw
                                 (ex-info "Source reader must return a string"
                                          {:error-type :source-read-failed
                                           :file relative})))
                             (let [snapshot {:file relative
                                             :source source
                                             :hash (structural-lens/source-hash source)}]
                               (recur (next remaining)
                                      (assoc cache cache-key snapshot)
                                      (assoc snapshots relative snapshot)
                                      (inc reads))))))
                       {:snapshots snapshots :file_read_count reads}))
                   mismatch
                   (some (fn [[file expected-hash]]
                           (let [actual-hash (get-in captured [:snapshots file :hash])]
                             (when-not (= expected-hash actual-hash)
                               {:file file
                                :expected_hash expected-hash
                                :actual_hash actual-hash})))
                         snapshot-guards)]
               (if mismatch
                 (merge
                   {:ok false
                    :operation "inspect_clojure"
                    :error_type "snapshot-guard-mismatch"
                    :error "A guarded file changed after the prior continuation"
                    :failed_stage "snapshot"
                    :actual_state "changed"
                    :read_complete false
                    :source_unchanged true
                    :next_action "refresh_snapshot"}
                   mismatch)
                 (merge {:ok true :root root} captured)))))))
     (catch Exception error
       (cond->
         {:ok false
          :operation "inspect_clojure"
          :error_type (name (or (:error-type (ex-data error))
                                :source-read-failed))
          :error (.getMessage error)
          :file (:file (ex-data error))
          :read_complete false
          :source_unchanged true
          :next_action "correct_request"}
         snapshot-guards (assoc :failed_stage "snapshot"))))))

;; @spec MCP-OP-STUDY-039
(defn- execute-inspect-in-context!
  "Validate, confine, snapshot once, and evaluate one typed inspect request."
  [{:keys [project-root telemetry read-source output-limits semantic-resolver] :as config}
   params]
  ;; The first call routed to a workspace root announces that session. One
  ;; server serves several arms of a cohort; without this the record cannot
  ;; tell a silent connection failure from a deliberate decline.
  (telemetry/record-session-start!
    telemetry {:workspace-root project-root
               :client-run-id (:client-run-id config)})
  (let [started (System/nanoTime)
        normalized-params (json/parse-string (json/generate-string params) true)
        prepare? (= "prepare-change" (:mode normalized-params))
        extraction-plan? (= "plan-extraction" (:mode normalized-params))
        ls-tree? (= "ls-tree" (:mode normalized-params))
        basis-view? (= "sites" (:view normalized-params))
        verification-job? (= "verification" (:view normalized-params))
        validated (when-not (or prepare? extraction-plan? ls-tree?
                                basis-view? verification-job?)
                    (inspect/validate-inspect-params params))
        result
        (assoc
          (cond
            verification-job?
            (let [observed
                  (cold-verify/status project-root
                                      (:verification_job normalized-params))]
              (cond-> (assoc observed
                             :operation "inspect_clojure"
                             :mode "verification-job")
                (= :running (:status observed))
                (dissoc :job_elapsed_ms)))

            extraction-plan?
            (extraction-plan/plan! config normalized-params)

            ls-tree?
            (execute-ls-tree config normalized-params)

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
                             (get-in normalized [:expect :files])
                             (:snapshot-guards normalized))]
              (if-not (:ok captured)
                (inspect-refusal captured)
                (assoc
                  (inspect/evaluate-snapshots
                    normalized (:snapshots captured)
                    (merge inspect/default-output-limits output-limits))
                  :file_read_count (:file_read_count captured)))))
          :inspection_elapsed_ms (elapsed-ms started))]
    (when telemetry
      (telemetry/record-inspect-call!
        telemetry params result {:total_ms (:inspection_elapsed_ms result)}))
    result))

(defn- attach-workspace-root
  [result workspace-root]
  (cond-> (assoc result :workspace_root workspace-root)
    (get-in result [:continuation :retry_template :arguments])
    (assoc-in [:continuation :retry_template :arguments :workspace_root]
              workspace-root)))

(defn execute-inspect!
  "Route one inspect request to a canonical workspace context, then execute it."
  [config params]
  (let [normalized (json/parse-string (json/generate-string params) true)
        explicit-root? (contains? normalized :workspace_root)]
    (if-not explicit-root?
      (let [result (execute-inspect-in-context! config normalized)
            resolved (workspace/canonical-root (:project-root config))]
        (cond-> result
          (:ok resolved) (attach-workspace-root (:workspace-root resolved))))
      (let [workspace-router (or (:workspace-router config)
                                 (workspace/router config))
            routed (workspace/resolve-request workspace-router normalized)]
        (if-not (:ok routed)
          routed
          (attach-workspace-root
            ;; @spec MCP-OP-STUDY-043
            ;; The client run travels with the ROUTE: a request routed to
            ;; another workspace is still the same client session, and a
            ;; router-built config would otherwise drop the identity.
            (execute-inspect-in-context!
              (cond-> (:config routed)
                (:client-run-id config)
                (assoc :client-run-id (:client-run-id config)))
              (:params routed))
            (:workspace-root routed)))))))

;; @spec MCP-OP-FIELD-001
(defn- missing-field-lines
  "Name the omitted fields, their path, and the minimal valid object there."
  [result]
  (when (and (= "missing-fields" (some-> (:reason result) name))
             (seq (:missing result)))
    (let [path (:path result)]
      (str
        (format "  missing required field%s at %s: %s\n"
                (if (= 1 (count (:missing result))) "" "s")
                (if (seq path)
                  (str/join "." (map str path))
                  "the request root")
                (str/join ", " (:missing result)))
        (when (seq (:required result))
          (format "  required there: %s\n"
                  (str/join ", " (:required result))))
        (when (:minimal_request result)
          (format "  minimal valid shape: %s\n"
                  (json/generate-string (:minimal_request result))))))))

;; @spec MCP-OP-FIELD-002
(defn- named-field-lines
  "Name the refused field and the values it accepts."
  [result]
  (when (and (:field result) (seq (:accepted result)))
    (format "  field %s accepts: %s%s\n"
            (:field result)
            (str/join ", " (:accepted result))
            (if (contains? result :actual)
              (str " · received " (pr-str (:actual result)))
              ""))))

;; @spec MCP-OP-DISPATCH-003
(defn- defmethod-owner-lines
  "Render the exact multimethod owner form and its bounded dispatch vocabulary."
  [owner]
  (when owner
    (let [{:keys [kind name dispatch]} (:owner_form owner)
          vocabulary (:dispatch_vocabulary owner)]
      (str
        (format "  owner is a multimethod · %s the name %s\n"
                (if (= 1 (:arm_count owner))
                  "1 defmethod arm shares"
                  (str (:arm_count owner) " defmethod arms share"))
                (:name owner))
        (format "  send this exact owner form to %s: {kind: %s, name: %s%s}\n"
                (:accepted_by owner)
                (pr-str kind)
                (pr-str name)
                (if dispatch
                  (str ", dispatch: " (pr-str dispatch))
                  ""))
        (when (seq vocabulary)
          (format "  dispatch values (%d/%d%s): %s\n"
                  (:dispatch_vocabulary_returned owner)
                  (:dispatch_count owner)
                  (if (:dispatch_vocabulary_truncated owner) "; truncated" "")
                  (str/join ", " vocabulary)))))))

;; @spec MCP-OP-READ-DIAG-002
;; @spec MCP-OP-DISPATCH-003
;; @spec MCP-OP-PREP-REQ-005

(defn inspect-summary
  "The exact `content[0].text` a client renders for one inspect_clojure result.

  Public because the public MCP result is text AND structured content
  together: nothing can bound the pair, and no witness can assert that the
  text carries what the receipt carries, while the renderer is private."
  [result]
  (cond
    ;; @spec MCP-OP-STUDY-040
    ;; The last two rungs of the fit ladder, before any mode dispatch: a
    ;; receipt that leaves no room to render itself still names the tool and
    ;; points at the receipt, in every mode.
    (= "notice" (:text_omitted result)) inspect/text-omitted-notice
    (= "name" (:text_omitted result)) inspect/minimum-text-block

    (and (= "verification-job" (:mode result)) (:status result))
    (verification-job-summary result)

    (= "ls-tree" (:mode result))
    (ls-tree-summary result)

    ;; @spec MCP-OP-STUDY-042
    (not (:ok result))
    (let [failed-request (:failed_request result)
          failure (first (:failures result))
          selection-failure (first (:selection_failures result))
          hypothesis (first (:hypotheses selection-failure))
          candidate (or (:owner hypothesis) (first (:form_candidates result)))
          hypotheses-truncated (or (:hypotheses_truncated selection-failure)
                                   (:candidates_truncated result))
          defmethod-owner (:defmethod_owner selection-failure)
          available-count (or (:available_owner_count result)
                              (count (:available_owners result)))
          continuation (:continuation result)
          completed-count (:completed_request_count continuation)
          pending-ids (:pending_request_ids continuation)
          failure-label (if (= "ambiguous-form" (:error_type failure))
                          "ambiguous form"
                          "missing form")
          diagnostic? (and failed-request failure)
          owners (owner-list-line result)]
      (refusal-text
        result
        [(when diagnostic? "")
         (when diagnostic?
           (format "  request %s · %s"
                   (:id failed-request) (:file failed-request)))
         (when (and diagnostic? failure)
           (format "  %s %s" failure-label (:form failure)))
         (when (and diagnostic? candidate)
           (format "  I think you may have meant %s? (hypothesis only)"
                   candidate))
         (when (and diagnostic? hypotheses-truncated)
           (format "  hypotheses truncated · showing %d of %d owners"
                   (:hypotheses_returned selection-failure)
                   available-count))
         (when (and owners (not diagnostic?)) "")
         owners
         ;; @spec MCP-OP-DISPATCH-003
         (when-let [lines (defmethod-owner-lines defmethod-owner)]
           (str/trimr lines))
         ;; @spec MCP-OP-FIELD-001
         (when-let [lines (missing-field-lines result)]
           (str/trimr lines))
         ;; @spec MCP-OP-FIELD-002
         (when-let [lines (named-field-lines result)]
           (str/trimr lines))
         ;; @spec MCP-OP-FIELD-003
         (when (:note result) (format "  note: %s" (:note result)))
         ;; The sentence and the list share one condition: a sentence about a
         ;; list that was not printed is worse than silence.
         (when owners
           (str "\n  All listed owners are real snapshot evidence; "
                "ranking is non-authoritative. Semantic selection "
                "among them is allowed; the exact retry verifies "
                "the selection."))
         (when continuation
           (format (str "\n  preserved %d completed request%s from the frozen snapshot\n"
                        "  retry only %s; do not reread before the guarded retry")
                   completed-count
                   (if (= 1 completed-count) "" "s")
                   (str/join ", " pending-ids)))
         (cond
           continuation
           (str "\n→ copy continuation.retry_template.arguments, fill only "
                "its null selector holes, and submit it")
           (and diagnostic? defmethod-owner)
           "\n→ send the exact defmethod owner form above, or choose one exact owner and retry"
           diagnostic? "\n→ choose one exact owner and retry"
           ;; @spec MCP-OP-FIELD-001
           (= "missing-fields" (some-> (:reason result) name))
           "\n→ add the named field(s) in the minimal valid shape above and call inspect_clojure once")]))

    (= "prepare-change" (:mode result))
    (prepare-change-summary result)

    (= "plan-extraction" (:mode result))
    (extraction-plan-summary result)

    (= "ls-tree" (:mode result))
    (ls-tree-summary result)

    (= "basis-view" (:mode result))
    (basis-view-summary result)

    :else
    (let [summary (inspect/concise-summary result)]
      (if (:prepared_request result)
        (str summary "\n\n" prepared-request/coaching-text)
        summary))))

;; @spec MCP-OP-READ-CONT-002
;; @spec MCP-OP-PREP-REQ-001
;; @spec MCP-OP-PREP-REQ-006

;; @spec MCP-OP-STUDY-040
(def max-fitted-result-bytes
  "The budget a fitted result is measured against: the declared public budget,
  with NOTHING held back.

  Round three held back a 64-byte `publish-reserve`, because
  `fit-public-result` measured with `elapsed_ms` zeroed — the clock had not
  stopped — while `mcp-operation/invoke!` published the same result with the
  real elapsed time in the text block and in `structuredContent`. Field
  evidence (Sol O2 round-3 review, section 5): the envelope's clock contract
  accepts any finite non-negative number, a `1.0E308` elapsed renders 309
  characters, and four near-boundary receipts published 32,860 / 32,841 /
  32,912 / 32,996 bytes against the 32,768-byte budget. A reserve is a
  constant taken from one observation; a bigger constant would only move the
  escape.

  The fit now runs on the FINALIZED result inside `mcp-operation/invoke!`, so
  the bytes it measures are the bytes that are published — envelope, clock,
  and whatever shape a later wire gives them. There is nothing left to
  reserve."
  max-public-result-bytes)

;; @spec MCP-OP-STUDY-040
(defn- public-budget-refusal
  "The typed refusal for a receipt that cannot fit even with no evidence text.

  Reached only when `structuredContent` ALONE crosses the declared budget: at
  that point no rendering choice can help, and a caller that is handed the
  result anyway has been told a bound the tool does not keep."
  [result required-bytes]
  (cond-> (with-envelope
            {:ok false
             :operation "inspect_clojure"
             :error_type "inspect-output-limit"
             :scope "public_result"
             :error (format (str "The complete inspect_clojure result is %d bytes; "
                                 "the public MCP output budget is %d")
                            required-bytes max-public-result-bytes)
             :required {:public_result_bytes required-bytes}
             :limits {:public_result_bytes max-public-result-bytes}
             :read_complete false
             :source_unchanged true
             :remedy (str "Lower limit, narrow dir/grep/ns_grep, or request "
                          "fewer forms so the complete result fits the public "
                          "output budget.")
             :next_action "narrow_scope"}
            result)
    (:mode result) (assoc :mode (:mode result))))

;; @spec MCP-OP-STUDY-040
(def public-fit-samples
  "How many evidence allowances the fit MEASURES per pass.

  The fit runs only when the complete rendering overshoots the budget, so this
  buys correctness on the rare path with dozens of renderings rather than a
  dozen. Two passes of this many — a coarse sweep of the whole band and one
  refinement around its winner — cost about sixty-six measurements and depend
  on no ordering property of `fits?` at all."
  32)

;; @spec MCP-OP-STUDY-040
(defn fit-public-result
  "Bound the complete public MCP result — TEXT BLOCK INCLUDED — by the budget.

  The TEXT is bounded first, because the rows are a rendering of evidence the
  caller already has in `structuredContent`, and a typed truncation keeps an
  answer where a refusal returns none. The search is a BISECTION over the
  evidence allowance down to zero, and below zero rows it falls through two
  further rungs — a notice naming where the receipt is, then the tool's own
  name — so a receipt that fits is never refused for want of a rendering.

  A refusal is reached only when `structuredContent` ALONE, plus the
  fifteen-byte tool identity the text must always carry, crosses the budget:
  at that point no rendering choice can help.

  Field evidence (Sol O2 round-2 review, section 2): the previous
  implementation halved a reserved allowance four times and then refused. One
  byte over the budget, with a receipt of 32,558 bytes under a 32,768 byte
  budget, it returned `inspect-output-limit`; a 32-result batch whose receipt
  measured 31,549 bytes was refused at every payload size, because
  `min-evidence-characters` held a 512-character floor per result that the
  budget was never allowed to lower."
  [raw-result]
  ;; @spec MCP-OP-STUDY-040
  ;; The fit measures the FINAL envelope. A result with no envelope is not one
  ;; the publisher could publish, and measuring it would reintroduce exactly
  ;; the gap the publish reserve used to paper over — so it is a typed
  ;; refusal here rather than a silent 17-byte error later. The question is
  ;; asked about the ENVELOPE, never about one of its shapes: naming
  ;; `:elapsed_ms` here would have thrown on the first request the moment the
  ;; wire nested the clock under `measured`.
  (when-not (mcp-operation/finalized? raw-result)
    (throw (IllegalArgumentException.
             (str "fit-public-result measures the published envelope and "
                  "needs the finalized result: it carries none of "
                  (pr-str mcp-operation/envelope-keys)))))
  (let [measure (fn [result]
                  (mcp-result-byte-count (inspect-summary result) result))
        required (measure raw-result)]
    (if (<= required max-fitted-result-bytes)
      raw-result
      (let [fits? (fn [candidate]
                    (<= (measure candidate) max-fitted-result-bytes))
            structured-bytes (mcp-result-byte-count "" raw-result)
            with-limit (fn [n] (assoc raw-result :text_evidence_limit n))
            ceiling (max 0 (- max-fitted-result-bytes structured-bytes))
            ;; @spec MCP-OP-STUDY-040
            ;; A SCAN, not a bisection. Bisection answers "the largest
            ;; allowance that fits" only where `fits?` is monotone, and it
            ;; answers it by discarding half the band on one probe — so a
            ;; single allowance that renders badly condemns every allowance
            ;; below it. Round four's unbudgeted `dropped:` line made the
            ;; rendering grow as the allowance fell; the first probe missed,
            ;; the search recurred into the half that could never fit, and an
            ;; ordinary two-file batch published 151 characters with 9,251
            ;; bytes unspent (Opus O2 round-4 review, sections 2 and 3).
            ;; `fact-block` now charges every rendered byte, so the rendering
            ;; does shrink with the allowance — and this search no longer
            ;; DEPENDS on that being true: every candidate is measured, and
            ;; the winner is the measured candidate that carries the most.
            scan (fn [low high]
                   (let [step (max 1 (quot (- high low) public-fit-samples))
                         points (distinct (concat (range low (inc high) step)
                                                  [high]))]
                     (reduce
                       (fn [best point]
                         (let [candidate (with-limit point)
                               measured (measure candidate)]
                           (if (and (<= measured max-fitted-result-bytes)
                                    (or (nil? best) (> measured (:bytes best))))
                             {:bytes measured :limit point :result candidate}
                             best)))
                       nil
                       points)))
            coarse (scan 0 ceiling)
            step (max 1 (quot ceiling public-fit-samples))
            ;; One refinement pass between the coarse winner's neighbours, so
            ;; the published rendering is within a fraction of a step of the
            ;; best the band holds rather than within a whole step of it.
            refined (when coarse
                      (scan (max 0 (- (:limit coarse) step))
                            (min ceiling (+ (:limit coarse) step))))
            best (last (sort-by :bytes (remove nil? [coarse refined])))
            notice (assoc raw-result :text_omitted "notice")
            named (assoc raw-result :text_omitted "name")]
        (cond
          best (:result best)
          (fits? notice) notice
          (fits? named) named
          :else (public-budget-refusal raw-result required))))))

(defn enforce-result-budget
  "Bound the complete public MCP result — its text block included."
  [ordinary-result raw-result]
  (cond
    (:prepared_request raw-result)
    (let [required-bytes
          (mcp-result-byte-count (inspect-summary raw-result) raw-result)]
      (if (<= required-bytes max-public-result-bytes)
        raw-result
        ordinary-result))

    (and (:ok raw-result)
         (#{"prepare-change" "basis-view" "plan-extraction"} (:mode raw-result)))
    (enforce-public-result-budget (inspect-summary raw-result) raw-result)

    (:continuation raw-result)
    (let [required-bytes
          (mcp-result-byte-count (inspect-summary raw-result) raw-result)]
      (if (<= required-bytes max-public-result-bytes)
        raw-result
        (with-envelope
          {:ok false
           :operation "inspect_clojure"
           :error_type "inspect-output-limit"
           :error "The complete selector refusal and continuation exceed the public MCP output budget"
           :failed_stage "output-budget"
           :required {:public-result-bytes required-bytes}
           :limits {:public-result-bytes max-public-result-bytes}
           :read_complete false
           :source_unchanged true
           :next_action "narrow_request"}
          raw-result)))

    ;; @spec MCP-OP-STUDY-040
    ;; Every other mode — `ls-tree` and every typed study read — used to fall
    ;; through unenforced.
    :else (fit-public-result raw-result)))

;; @spec MCP-OP-TIME-004
;; @spec MCP-OP-ASYNC-001
;; @spec MCP-OP-ASYNC-002
;; @spec MCP-OP-ASYNC-003
;; @spec MCP-OP-ASYNC-004
;; @spec MCP-OP-ASYNC-005
(defn attach-prepared-confirmation!
  "Publish and retain confirmation only after both complete result gates pass."
  [exchange ordinary-result projected-result]
  ;; @spec MCP-OP-PREP-ACT-001
  ;; @spec MCP-OP-PREP-ACT-004
  ;; @spec MCP-OP-PREP-ACT-014
  (let [prepared-result (enforce-result-budget ordinary-result projected-result)
        session-key (prepared-confirmation/exchange-session-key exchange)]
    (prepared-confirmation/attach-confirmation!
      prepared-confirmation/process-registry session-key prepared-result
      (fn [candidate]
        (mcp-result-byte-count (inspect-summary candidate) candidate)))))

(defn handle-inspect
  "Structured clojure-mcp callback handler, retained as a Var for hot reload."
  [exchange params callback]
  (mcp-operation/invoke!
    {:execute
     #(let [ordinary-result
            (if-let [config @runtime-config]
              ;; @spec MCP-OP-STUDY-043
              ;; The MCP request shape cannot carry a run id — `inspect-schema`
              ;; is `additionalProperties false` — and a caller-supplied one
              ;; could name another arm. The transport's session is the id.
              (execute-inspect!
                (cond-> config
                  (prepared-confirmation/exchange-session-key exchange)
                  (assoc :client-run-id
                         (prepared-confirmation/exchange-session-key exchange)))
                params)
              {:ok false
               :operation "inspect_clojure"
               :error_type "server-not-initialized"
               :error "inspect_clojure server is not initialized"
               :read_complete false
               :source_unchanged true
               :next_action "restart_server"})]
        ordinary-result)
     ;; @spec MCP-OP-STUDY-040
     ;; The budget gate runs on the FINALIZED result, after the clock has
     ;; stopped, so the bytes it measures are the bytes this callback
     ;; publishes. Nothing is added afterwards, which is what retired the
     ;; 64-byte publish reserve (Sol O2 round-3 review, section 5).
     :fit (fn [ordinary-result]
            (attach-prepared-confirmation!
              exchange ordinary-result
              (prepared-request/project-result ordinary-result)))
     :summarize inspect-summary
     :callback callback}))

(def inspect-tool
  {:id :inspect-clojure
   :name "inspect_clojure"
   :description tool-description
   :schema inspect-schema
   :output-schema inspect-output-schema
   :annotations inspect-annotations
   :structured? true
   :tool-fn #'handle-inspect})
