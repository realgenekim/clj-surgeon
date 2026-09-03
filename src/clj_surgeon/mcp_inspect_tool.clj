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
    "Every study receipt is bounded to 4096 payload characters by default; a "
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
            :items {:type "string" :minLength 1}}
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

(def ls-tree-default-limit 4096)
(def ls-tree-max-limit 16384)

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
    "limit" {:type "integer" :minimum 1 :maximum 16384 :default 4096
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
    "edn"   (inspect/json-data (study/format-ls-tree-edn projects dir))
    "names" (inspect/json-data (study/format-ls-tree-names projects dir))
    ;; The true discovered count travels with the text rendering so its total
    ;; line cannot contradict the receipt's own file_count.
    (study/format-ls-tree-text projects dir {:file-count total})))

(defn- ls-tree-payload-size
  [payload output-format]
  (if (contains? #{"edn" "names"} output-format)
    (inspect/json-character-count payload)
    (count payload)))

;; @spec MCP-OP-STUDY-015
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
        empty-receipt {:returned 0
                       :omitted total
                       :truncated (pos? total)
                       :payload (ls-tree-render [] dir output-format total)}]
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

(defn- ls-tree-next-call
  [params overrides]
  {:tool "inspect_clojure"
   :arguments (merge (cond-> {:mode "ls-tree"}
                       (:dir params) (assoc :dir (:dir params))
                       (:grep params) (assoc :grep (:grep params))
                       (:ns_grep params) (assoc :ns_grep (:ns_grep params))
                       (:format params) (assoc :format (:format params))
                       (:max_files params) (assoc :max_files (:max_files params)))
                     overrides)})

(def ^:private ls-tree-request-fields
  "The request fields a continuation reproduces. `workspace_root` is excluded
  on purpose: `ls-tree-next-call` never emits it, so counting it would make
  every routed request look different from its own continuation."
  [:mode :dir :grep :ns_grep :format :limit :max_files])

(defn- ls-tree-request-arguments
  "The arguments of the call just made, shaped exactly as a continuation is."
  [params]
  (merge {:mode "ls-tree" :dir "."}
         (select-keys params ls-tree-request-fields)))

;; @spec MCP-OP-STUDY-007
(defn- ls-tree-refusal
  "A typed ls-tree refusal, with a continuation only when replaying it could
  differ from the call that just failed.

  The `{:dir \".\"}` continuation was unconditional, so `grep` at the root
  handed back the exact request just made — `no-clojure-files` at `\".\"`
  proposing `no-clojure-files` at `\".\"`. Narrowing is a caller judgment
  there, exactly as at the receipt ceiling."
  [params error-type message extra]
  (let [continuation (ls-tree-next-call params {:dir "."})
        repeats? (= (:arguments continuation)
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
      extra)))

;; @spec MCP-OP-STUDY-001
;; @spec MCP-OP-STUDY-006
;; @spec MCP-OP-STUDY-007
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
        resolved (mcp-paths/resolve-directory-path root dir)]
    (cond
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
                             (:grep params) (assoc :grep (:grep params))
                             (:ns_grep params) (assoc :ns_grep (:ns_grep params))))
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
              ;; An executable continuation only while raising the limit can
              ;; still advance; a narrower dir or grep is a caller judgment.
              (and (:truncated bounded) (< limit ls-tree-max-limit))
              (assoc :next_call
                     (ls-tree-next-call params {:limit ls-tree-max-limit}))
              (and (:truncated bounded) (>= limit ls-tree-max-limit))
              (assoc :remedy
                     (str "The receipt is already at the maximum limit; scan a "
                          "subdirectory or add a grep pattern.")))))))))

(defn ls-tree-summary
  [result]
  (if-not (:ok result)
    (format (str "inspect_clojure · ls-tree\n"
                 "  refused · %s · %s\n\n%s\n\n→ %s")
            (:error_type result)
            (mcp-operation/format-elapsed-ms (:elapsed_ms result))
            (:error result)
            (:next_action result))
    (format (str "inspect_clojure · ls-tree\n"
                 "  %s · %d project%s · %d of %d file%s · %s\n\n"
                 "%s\n"
                 "→ %s")
            (:dir result)
            (:project_count result)
            (if (= 1 (:project_count result)) "" "s")
            (:returned result)
            (:file_count result)
            (if (= 1 (:file_count result)) "" "s")
            (mcp-operation/format-elapsed-ms (:elapsed_ms result))
            (if (:truncated result)
              (format "! bounded receipt · %d file%s omitted · read_complete=false"
                      (:omitted result)
                      (if (= 1 (:omitted result)) "" "s"))
              "✓ complete tree · read_complete=true")
            (:next_action result))))

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
      (mcp-operation/format-elapsed-ms (:elapsed_ms result))
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
      (mcp-operation/format-elapsed-ms (:elapsed_ms result))
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
    (mcp-operation/format-elapsed-ms (:elapsed_ms result))))

(defn verification-job-summary
  "Render one bounded cold-verification job without repeating its full output."
  [result]
  (let [status (:status result)
        terminal? (true? (:verification_complete result))
        passed? (true? (:passed result))
        clock-summary
        (if-let [job-elapsed-ms (:job_elapsed_ms result)]
          (str "request "
               (mcp-operation/format-elapsed-ms (:elapsed_ms result))
               " · job "
               (mcp-operation/format-elapsed-ms job-elapsed-ms))
          (str "request "
               (mcp-operation/format-elapsed-ms (:elapsed_ms result))))]
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

(defn- execute-inspect-in-context!
  "Validate, confine, snapshot once, and evaluate one typed inspect request."
  [{:keys [project-root telemetry read-source output-limits semantic-resolver] :as config}
   params]
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
            (execute-inspect-in-context! (:config routed) (:params routed))
            (:workspace-root routed)))))))

;; @spec MCP-OP-READ-DIAG-002
;; @spec MCP-OP-PREP-REQ-005
(defn- inspect-summary
  [result]
  (cond
    (and (= "verification-job" (:mode result)) (:status result))
    (verification-job-summary result)

    (= "ls-tree" (:mode result))
    (ls-tree-summary result)

    (not (:ok result))
    (let [reason (or (:reason result) (:error-type result)
                     (:error_type result) "unknown-error")
          failed-request (:failed_request result)
          failure (first (:failures result))
          selection-failure (first (:selection_failures result))
          hypothesis (first (:hypotheses selection-failure))
          candidate (or (:owner hypothesis) (first (:form_candidates result)))
          hypotheses-truncated (or (:hypotheses_truncated selection-failure)
                                   (:candidates_truncated result))
          available-owners (:available_owners result)
          available-returned (or (:available_owners_returned result)
                                 (count available-owners))
          available-count (or (:available_owner_count result)
                              available-returned)
          continuation (:continuation result)
          completed-count (:completed_request_count continuation)
          pending-ids (:pending_request_ids continuation)
          failure-label (if (= "ambiguous-form" (:error_type failure))
                          "ambiguous form"
                          "missing form")
          diagnostic? (and failed-request failure)]
      (str
        (format (str "inspect_clojure\n"
                     "  refused · %s · %s\n")
                (if (keyword? reason) (name reason) reason)
                (mcp-operation/format-elapsed-ms (:elapsed_ms result)))
        (when diagnostic?
          (str
            (format "  request %s · %s\n"
                    (:id failed-request) (:file failed-request))
            (when failure
              (format "  %s %s\n" failure-label (:form failure)))
            (when candidate
              (format "  I think you may have meant %s? (hypothesis only)\n"
                      candidate))
            (when hypotheses-truncated
              (format "  hypotheses truncated · showing %d of %d owners\n"
                      (:hypotheses_returned selection-failure)
                      available-count))
            (when (seq available-owners)
              (format "  available owners (%d/%d%s): %s\n"
                      available-returned
                      available-count
                      (if (:available_owners_truncated result)
                        "; truncated"
                        "")
                      (str/join ", " available-owners)))))
        (str (when (seq available-owners)
               (str "\n  All listed owners are real snapshot evidence; "
                    "ranking is non-authoritative. Semantic selection "
                    "among them is allowed; the exact retry verifies "
                    "the selection.\n"))
             (when continuation
               (format (str "  preserved %d completed request%s from the frozen snapshot\n"
                            "  retry only %s; do not reread before the guarded retry\n")
                       completed-count
                       (if (= 1 completed-count) "" "s")
                       (str/join ", " pending-ids)))
             (cond
               continuation "\n→ copy continuation.retry_template.arguments, fill only its null selector holes, and submit it"
               diagnostic? "\n→ choose one exact owner and retry"
               :else (format "\n→ %s" (or (:next_action result) "correct_request"))))))

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
(defn- enforce-result-budget
  [ordinary-result raw-result]
  (cond
    (:prepared_request raw-result)
    (let [normalized (assoc raw-result :elapsed_ms 0.0)
          required-bytes
          (mcp-result-byte-count (inspect-summary normalized) normalized)]
      (if (<= required-bytes max-public-result-bytes)
        raw-result
        ordinary-result))

    (and (:ok raw-result)
         (#{"prepare-change" "basis-view" "plan-extraction"} (:mode raw-result)))
    (enforce-public-result-budget
      (inspect-summary (assoc raw-result :elapsed_ms 0.0))
      raw-result)

    (:continuation raw-result)
    (let [required-bytes
          (mcp-result-byte-count
            (inspect-summary (assoc raw-result :elapsed_ms 0.0))
            raw-result)]
      (if (<= required-bytes max-public-result-bytes)
        raw-result
        {:ok false
         :operation "inspect_clojure"
         :error_type "inspect-output-limit"
         :error "The complete selector refusal and continuation exceed the public MCP output budget"
         :failed_stage "output-budget"
         :required {:public-result-bytes required-bytes}
         :limits {:public-result-bytes max-public-result-bytes}
         :read_complete false
         :source_unchanged true
         :next_action "narrow_request"}))

    :else raw-result))

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
        (let [normalized (assoc candidate :elapsed_ms 0.0)]
          (mcp-result-byte-count (inspect-summary normalized) normalized))))))

(defn handle-inspect
  "Structured clojure-mcp callback handler, retained as a Var for hot reload."
  [exchange params callback]
  (mcp-operation/invoke!
    {:execute
     #(let [ordinary-result
            (if-let [config @runtime-config]
              (execute-inspect! config params)
              {:ok false
               :operation "inspect_clojure"
               :error_type "server-not-initialized"
               :error "inspect_clojure server is not initialized"
               :read_complete false
               :source_unchanged true
               :next_action "restart_server"})]
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
