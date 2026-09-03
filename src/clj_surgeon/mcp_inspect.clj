(ns clj-surgeon.mcp-inspect
  "Pure contract and batch evaluator for the read-only inspect_clojure MCP tool."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.edit-dsl :as edit-dsl]
   [clj-surgeon.mcp-contract :as mcp-contract]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-source-anchor :as source-anchor]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.show-form :as show-form]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def max-requests 64)
(def max-files 32)
(def max-forms 128)
(def default-output-limits
  {:per-request-source 65536
   :per-request-result 65536
   :aggregate-result 262144})

(def ^:private top-fields #{"requests" "expect" "snapshot_guards"})
(def ^:private required-top-fields #{"requests" "expect"})
(def ^:private top-expect-fields #{"requests" "files"})
(def ^:private common-request-fields #{"id" "operation" "file"})
(def ^:private operation-fields
  {"forms" (into common-request-fields ["forms" "expect" "include_source"])
   "outline" (conj common-request-fields "include_string_symbols")
   "match" (into common-request-fields ["match" "inside" "expect"])
   "xray" (conj common-request-fields "expression")})
(def ^:private operation-required
  {"forms" (into common-request-fields ["forms" "expect"])
   "outline" common-request-fields
   "match" (conj common-request-fields "match")
   "xray" (get operation-fields "xray")})

(defn- field-name
  [key]
  (cond
    (keyword? key) (if-let [namespace (namespace key)]
                     (str namespace "/" (name key))
                     (name key))
    (string? key) key
    :else (str key)))

(defn- field
  [m key]
  (if (contains? m key)
    (get m key)
    (get m (keyword key))))

(defn- present?
  [m key]
  (or (contains? m key) (contains? m (keyword key))))

(defn- refuse!
  [reason path message & [data]]
  (throw
    (ex-info
      message
      (merge
        {:ok false
         :operation "inspect_clojure"
         :error-type :invalid-mcp-request
         :reason reason
         :path path
         :error message
         :read_complete false
         :source_unchanged true
         :next_action "correct_request"}
        data))))

(def ^:private minimal-request-examples
  {[] {"requests" [{"id" "r1" "operation" "outline" "file" "src/example.clj"}]
       "expect" {"requests" 1 "files" 1}}
   ["expect"] {"requests" 1 "files" 1}
   ["requests" :index] {"id" "r1" "operation" "outline"
                        "file" "src/example.clj"}
   ["requests" :index "expect"] {"forms" 1}})

(defn- example-path
  [path]
  (mapv #(if (integer? %) :index %) (vec path)))

;; @spec MCP-OP-FIELD-001
(defn- minimal-request-shape
  "The smallest valid object at `path`, restricted to that path's required
   fields. Returns nil when no example covers every required field, so the
   refusal never shows a shape it cannot stand behind."
  [path required]
  (when-let [example (get minimal-request-examples (example-path path))]
    (let [shape (into (sorted-map) (select-keys example (vec required)))]
      (when (= (set (keys shape)) (set required))
        shape))))

;; @spec MCP-OP-FIELD-001
(defn- missing-fields-evidence
  [path required missing]
  (cond-> {:missing (vec missing)
           :required (vec (sort required))}
    (minimal-request-shape path required)
    (assoc :minimal_request (minimal-request-shape path required))))

(defn- validate-fields!
  [value allowed required path]
  (when-not (map? value)
    (refuse! :expected-object path "Expected a JSON object"))
  (let [actual (set (map field-name (keys value)))
        unknown (vec (sort (remove allowed actual)))
        missing (vec (sort (remove actual required)))]
    (when (seq unknown)
      (refuse! :unknown-fields path "Request contains unknown fields"
               {:unknown unknown}))
    (when (seq missing)
      (refuse! :missing-fields path "Request is missing required fields"
               (missing-fields-evidence path required missing)))))

(defn- nonblank-string!
  [value path]
  (when-not (and (string? value) (not (str/blank? value)))
    (refuse! :non-blank-string path "Expected a non-blank string"))
  value)

(defn- nonempty-array!
  [value path]
  (when-not (and (vector? value) (seq value))
    (refuse! :non-empty-array path "Expected a non-empty JSON array"))
  value)

(defn- positive-integer!
  [value path]
  (when-not (and (integer? value) (pos? value))
    (refuse! :positive-integer path "Expected a positive integer"))
  value)

(defn- non-negative-integer!
  [value path]
  (when-not (and (integer? value) (not (neg? value)))
    (refuse! :non-negative-integer path "Expected a non-negative integer"))
  value)

(defn- boolean!
  [value path]
  (when-not (instance? Boolean value)
    (refuse! :boolean path "Expected a boolean"))
  value)

(defn- source-path!
  [value path]
  (when-not (mcp-paths/relative-source-path? value)
    (refuse! :invalid-relative-source-path path
             "Expected a project-relative .clj, .cljs, or .cljc path without parent traversal"))
  value)

(defn- unique-strings!
  [values path duplicate-reason]
  (loop [seen #{}
         index 0]
    (when (< index (count values))
      (let [value (nonblank-string! (nth values index) (conj path index))]
        (when (contains? seen value)
          (refuse! duplicate-reason (conj path index)
                   "Values must be unique" {:value value}))
        (recur (conj seen value) (inc index)))))
  values)

;; @spec MCP-OP-READ-GUARD-001
(defn- snapshot-guards!
  [value path]
  (when-not (map? value)
    (refuse! :expected-object path "snapshot_guards must be a JSON object"))
  (when (empty? value)
    (refuse! :empty-snapshot-guards path
             "snapshot_guards must contain at least one file hash"))
  (into
    (array-map)
    (map-indexed
      (fn [index [raw-file raw-hash]]
        (let [file (field-name raw-file)
              _ (when-not (mcp-paths/relative-source-path? file)
                  (refuse! :invalid-relative-source-path (conj path index "file")
                           "Expected a project-relative .clj, .cljs, or .cljc guard path without parent traversal"
                           {:failed-stage :snapshot :file file}))
              hash raw-hash]
          (when-not (string? hash)
            (refuse! :invalid-snapshot-hash (conj path file)
                     "Snapshot hashes must be strings"
                     {:file file}))
          (when-not (re-matches #"[0-9a-f]{64}" hash)
            (refuse! :invalid-snapshot-hash (conj path file)
                     "Snapshot hashes must be 64 lowercase hexadecimal characters"
                     {:file file}))
          [file hash])))
    value))

(defn- validate-forms-expect!
  [value path form-count]
  (validate-fields! value #{"forms"} #{"forms"} path)
  (let [expected (positive-integer! (field value "forms")
                                    (conj path "forms"))]
    (when-not (= form-count expected)
      (refuse! :request-expectation-mismatch (conj path "forms")
               "Declared form count does not match requested forms"
               {:expected expected :actual form-count}))
    {:forms expected}))

(defn- validate-match-expect!
  [value path]
  (validate-fields! value #{"matches"} #{} path)
  (cond-> {}
    (present? value "matches")
    (assoc :matches (non-negative-integer!
                      (field value "matches")
                      (conj path "matches")))))

(defn- validate-request!
  [request index]
  (let [path ["requests" index]]
    (when-not (map? request)
      (refuse! :expected-object path "Expected a JSON object"))
    (let [actual (set (map field-name (keys request)))]
      (when-let [missing (seq (sort (remove actual common-request-fields)))]
        (refuse! :missing-fields path "Request is missing required fields"
                 (missing-fields-evidence path common-request-fields missing))))
    (let [id (nonblank-string! (field request "id") (conj path "id"))
          operation (nonblank-string! (field request "operation")
                                      (conj path "operation"))
          allowed (get operation-fields operation)
          required (get operation-required operation)]
      (when-not allowed
        (refuse! :unknown-operation (conj path "operation")
                 "Unsupported inspect operation"
                 {:actual operation
                  :supported (vec (sort (keys operation-fields)))}))
      (validate-fields! request allowed required path)
      (let [file (source-path! (field request "file") (conj path "file"))]
        (case operation
          "forms"
          (let [raw-forms (nonempty-array! (field request "forms")
                                           (conj path "forms"))
                _ (when (> (count raw-forms) max-forms)
                    (refuse! :too-many-forms (conj path "forms")
                             "A forms request exceeds the maximum form count"
                             {:maximum max-forms :actual (count raw-forms)}))
                forms (unique-strings! raw-forms (conj path "forms")
                                       :duplicate-form)]
            (cond->
              {:id id :operation operation :file file :forms forms
               :expect (validate-forms-expect!
                         (field request "expect") (conj path "expect")
                         (count forms))}
              (present? request "include_source")
              (assoc :include-source
                     (boolean! (field request "include_source")
                               (conj path "include_source")))))

          "outline"
          (cond-> {:id id :operation operation :file file}
            (present? request "include_string_symbols")
            (assoc :include-string-symbols
                   (boolean! (field request "include_string_symbols")
                             (conj path "include_string_symbols"))))

          "match"
          (cond-> {:id id :operation operation :file file
                   :match (nonblank-string! (field request "match")
                                            (conj path "match"))}
            (present? request "inside")
            (assoc :inside (nonblank-string! (field request "inside")
                                             (conj path "inside")))
            (present? request "expect")
            (assoc :expect (validate-match-expect!
                             (field request "expect")
                             (conj path "expect"))))

          "xray"
          {:id id :operation operation :file file
           :expression (nonblank-string! (field request "expression")
                                         (conj path "expression"))})))))

(def ^:private operationless-forms-fields
  #{"id" "file" "forms" "expect" "include_source"})
(def ^:private operationless-forms-required
  #{"id" "file" "forms" "expect"})

(defn- normalize-request-ids!
  [requests]
  (doseq [[index request] (map-indexed vector requests)]
    (when-not (map? request)
      (refuse! :expected-object ["requests" index]
               "Expected a JSON object")))
  (let [supplied? (mapv #(present? % "id") requests)]
    (when (and (some true? supplied?) (some false? supplied?))
      (refuse! :mixed-request-ids ["requests"]
               "Request IDs must be either all supplied or all omitted"
               {:read_started false}))
    (if (every? true? supplied?)
      requests
      (mapv (fn [index request]
              (assoc request "id" (str "request-" (inc index))))
            (range)
            requests))))

(defn- complete-operationless-forms-request?
  [request]
  (let [actual (set (map field-name (keys request)))
        forms (field request "forms")
        expect (field request "expect")
        expect-fields (when (map? expect)
                        (set (map field-name (keys expect))))]
    (and (every? actual operationless-forms-required)
         (every? operationless-forms-fields actual)
         (vector? forms)
         (seq forms)
         (= #{"forms"} expect-fields))))

(defn- normalize-request-operations!
  [requests]
  (mapv
    (fn [index request]
      (if (present? request "operation")
        request
        (if (complete-operationless-forms-request? request)
          (assoc request "operation" "forms")
          (refuse! :operation-required ["requests" index "operation"]
                   "Inspect request requires an explicit operation"
                   {:read_started false
                    :supported (vec (sort (keys operation-fields)))
                    :supplied_fields
                    (vec (sort (map field-name (keys request))))}))))
    (range)
    requests))

(defn validate-inspect-params
  "Validate JSON-shaped inspect_clojure input and return normalized Clojure data."
  ;; @spec MCP-OP-READ-NORM-001
  ;; @spec MCP-OP-READ-NORM-002
  ;; @spec MCP-OP-READ-NORM-003
  ;; @spec MCP-OP-READ-NORM-004
  ;; @spec MCP-OP-READ-NORM-005
  [params]
  (let [params (mcp-contract/json-containers->clj params)]
    (try
      (validate-fields! params top-fields required-top-fields [])
      (let [raw-requests (nonempty-array! (field params "requests") ["requests"])
            _ (when (> (count raw-requests) max-requests)
                (refuse! :too-many-requests ["requests"]
                         "Inspect batch exceeds the maximum request count"
                         {:maximum max-requests :actual (count raw-requests)}))
            normalized-requests (-> raw-requests
                                    normalize-request-ids!
                                    normalize-request-operations!)
            requests (mapv validate-request! normalized-requests (range))
            snapshot-guards
            (when (present? params "snapshot_guards")
              (snapshot-guards! (field params "snapshot_guards")
                                ["snapshot_guards"]))
            request-files (set (map :file requests))
            missing-guards (when snapshot-guards
                             (vec (sort (remove #(contains? snapshot-guards %)
                                                request-files))))
            _ (when (seq missing-guards)
                (refuse! :missing-snapshot-guards ["snapshot_guards"]
                         "Every requested file must have a snapshot guard"
                         {:missing missing-guards}))
            _ (when (> (count snapshot-guards) max-files)
                (refuse! :too-many-files ["snapshot_guards"]
                         "Snapshot guards exceed the maximum file count"
                         {:maximum max-files :actual (count snapshot-guards)}))
            _ (loop [seen #{}
                     index 0]
                (when (< index (count requests))
                  (let [id (:id (nth requests index))]
                    (when (contains? seen id)
                      (refuse! :duplicate-id ["requests" index "id"]
                               "Request IDs must be unique" {:id id}))
                    (recur (conj seen id) (inc index)))))
            expected (field params "expect")
            _ (validate-fields! expected top-expect-fields
                                top-expect-fields ["expect"])
            expected-requests (positive-integer!
                                (field expected "requests")
                                ["expect" "requests"])
            expected-files (positive-integer!
                             (field expected "files")
                             ["expect" "files"])
            actual-requests (count requests)
            actual-files (count (distinct (map :file requests)))]
        (when-not (= expected-requests actual-requests)
          (refuse! :aggregate-expectation-mismatch ["expect" "requests"]
                   "Declared request count does not match the batch"
                   {:expected expected-requests :actual actual-requests}))
        (when-not (= expected-files actual-files)
          (refuse! :aggregate-expectation-mismatch ["expect" "files"]
                   "Declared file count does not match distinct request files"
                   {:expected expected-files :actual actual-files}))
        (when (> actual-files max-files)
          (refuse! :too-many-files ["expect" "files"]
                   "Inspect batch exceeds the maximum distinct file count"
                   {:maximum max-files :actual actual-files}))
        {:ok true
         :params (cond-> {:requests requests
                          :expect {:requests expected-requests
                                   :files expected-files}}
                   snapshot-guards (assoc :snapshot-guards snapshot-guards))})
      (catch clojure.lang.ExceptionInfo error
        (ex-data error)))))

(defn- json-key
  [key]
  (if (keyword? key)
    (keyword (str/replace (name key) "-" "_"))
    key))

(defn json-data
  "Recursively normalize kernel EDN into stable JSON-compatible Clojure data."
  [value]
  (cond
    (map? value)
    (into (array-map)
          (map (fn [[key child]] [(json-key key) (json-data child)]))
          value)

    (vector? value) (mapv json-data value)
    (list? value) (mapv json-data value)
    (set? value) (mapv json-data (sort-by pr-str value))
    (keyword? value) (name value)
    (symbol? value) (str value)
    :else value))

(defn json-character-count
  "Count deterministic compact JSON characters after result normalization."
  [value]
  (count (json/generate-string (json-data value))))

(defn- kernel-refusal
  [request index result]
  (let [error-type (or (:error-type result) :inspect-kernel-refusal)]
    (cond->
      {:ok false
       :operation "inspect_clojure"
       :error_type (if (keyword? error-type)
                     (name error-type)
                     (str error-type))
       :error (or (:error result) "inspect_clojure refused")
       :request_id (:id request)
       :request_index index
       :read_complete false
       :source_unchanged true
       :next_action "correct_request"}
      (= "forms" (:operation request))
      (assoc :failed_request
             {:id (:id request)
              :operation "forms"
              :file (:file request)
              :requested_forms (mapv str (:forms request))})
      (contains? result :failed-stage)
      (assoc :failed_stage (name (:failed-stage result)))
      (contains? result :file-hash)
      (assoc :file_hash (:file-hash result))
      (contains? result :expected) (assoc :expected (json-data (:expected result)))
      (contains? result :actual) (assoc :actual (json-data (:actual result)))
      (contains? result :expected-match-count)
      (assoc :expected_match_count (:expected-match-count result))
      (contains? result :actual-match-count)
      (assoc :actual_match_count (:actual-match-count result))
      (contains? result :match-count)
      (assoc :match_count (:match-count result))
      ;; @spec MCP-OP-FIELD-003
      (contains? result :note) (assoc :note (:note result))
      (contains? result :failure-count)
      (assoc :failure_count (:failure-count result))
      (contains? result :requested-form-count)
      (assoc :requested_form_count (:requested-form-count result))
      (contains? result :resolved-form-count)
      (assoc :resolved_form_count (:resolved-form-count result))
      (contains? result :failures)
      (assoc :failures (json-data (:failures result)))
      (contains? result :available-form-count)
      (assoc :available_form_count (:available-form-count result))
      (contains? result :available-owner-count)
      (assoc :available_owner_count (:available-owner-count result))
      (contains? result :available-owners)
      (assoc :available_owners (:available-owners result))
      (contains? result :available-owners-returned)
      (assoc :available_owners_returned (:available-owners-returned result))
      (contains? result :available-owners-omitted)
      (assoc :available_owners_omitted (:available-owners-omitted result))
      (contains? result :available-owners-truncated)
      (assoc :available_owners_truncated (:available-owners-truncated result))
      (contains? result :selection-failures)
      ;; @spec MCP-OP-READ-NORM-003
      (assoc :selection_failures
             (mapv #(assoc (json-data %) :request_id (:id request))
                   (:selection-failures result)))
      (contains? result :form-candidates)
      (assoc :form_candidates (json-data (:form-candidates result)))
      (contains? result :candidate-limit)
      (assoc :candidate_limit (:candidate-limit result))
      (contains? result :candidates-truncated)
      (assoc :candidates_truncated (:candidates-truncated result)))))

;; @spec MCP-OP-READ-DIAG-001
;; @spec MCP-OP-READ-DIAG-003
;; @spec MCP-OP-READ-HYP-001
;; @spec MCP-OP-READ-HYP-002
(defn- forms-result
  [request snapshot]
  (let [found (show-form/select-form
                (:file request) (:source snapshot)
                {:forms (mapv symbol (:forms request))})]
    (if (:error found)
      (assoc found
             :failed-stage :selector
             :file-hash (:hash snapshot))
      {:id (:id request)
       :operation "forms"
       :file (:file request)
       :file_hash (:hash snapshot)
       :form_count (:form-count found)
       :source_character_count (:source-char-count found)
       :forms
       (mapv (fn [form]
               (let [built-anchor
                     (source-anchor/build-form-source-anchor
                       (:file request) (:source snapshot) form)]
                 (when-not (:ok built-anchor)
                   (throw
                     (ex-info "Selected form has no exact source anchor"
                              built-anchor)))
                 (cond->
                   {:hash (structural-lens/source-hash (:source form))
                    :line (:line form)
                    :end_line (:end-line form)
                    :form_type (str (:type form))
                    :name (str (:name form))
                    :platforms (mapv name (:platforms form))
                    :file (:file request)
                    :file_hash (:hash snapshot)
                    :source_anchor (:source-anchor built-anchor)}
                   (not= false (:include-source request))
                   (assoc :source (:source form))
                   (:comment-start form)
                   (assoc :comment_start (:comment-start form)))))
             (:forms found))})))

(defn- outline-result
  [request snapshot]
  {:id (:id request)
   :operation "outline"
   :file (:file request)
   :file_hash (:hash snapshot)
   :source_character_count 0
   :outline (json-data
              (outline/outline-source
                (:file request) (:source snapshot) {}
                {:include-string-symbols
                 (:include-string-symbols request)}))})

(def ^:private wildcard-note
  "each `_` matches exactly one subtree; a longer form needs a longer pattern")

;; @spec MCP-OP-FIELD-003
;; @spec MCP-OP-FIELD-005
(defn- wildcard-pattern?
  "Does this pattern use `_` as a standalone wildcard token?

   Decided from the parsed pattern, because bytes cannot tell a wildcard from an
   underscore inside a string literal or inside one symbol, and cannot see a
   wildcard whose only neighbour is a comma. An unreadable pattern carries no
   note; the match request refuses on its own terms."
  [pattern]
  (try
    (let [form (node/sexpr (parser/parse-string (str pattern)))]
      (boolean (some #(= '_ %) (tree-seq coll? seq form))))
    (catch Exception _ false)))

(defn- match-result
  [request snapshot]
  (let [found (structural-lens/find-subforms
                (:source snapshot)
                (cond-> {:match (:match request)}
                  (:inside request) (assoc :inside (symbol (:inside request)))))]
    (cond
      (:error found) found

      (and (contains? (:expect request) :matches)
           (not= (get-in request [:expect :matches]) (:match-count found)))
      (cond-> {:error "Structural match cardinality did not meet the declared expectation"
               :error-type :inspect-cardinality-mismatch
               :expected (get-in request [:expect :matches])
               :actual (:match-count found)
               :match-count (:match-count found)}
        ;; @spec MCP-OP-FIELD-003
        (and (< (:match-count found) (get-in request [:expect :matches]))
             (wildcard-pattern? (:match request)))
        (assoc :note wildcard-note))

      :else
      (let [matches (mapv (fn [match]
                            (assoc (json-data match)
                                   :hash (structural-lens/source-hash
                                           (:source match))
                                   :file_hash (:hash snapshot)))
                          (:matches found))]
        (cond->
          {:id (:id request)
           :operation "match"
           :file (:file request)
           :file_hash (:hash snapshot)
           :match (:match request)
           :inside (:inside request)
           :match_count (:match-count found)
           :source_character_count (reduce + 0 (map #(count (:source %))
                                                    (:matches found)))
           :matches matches}
          ;; @spec MCP-OP-FIELD-003
          (and (zero? (:match-count found))
               (wildcard-pattern? (:match request)))
          (assoc :note wildcard-note))))))

(defn- xray-result
  [request snapshot]
  (let [prepared (edit-dsl/prepare-xray-options
                   {:op :xray
                    :file (:file request)
                    :expr (:expression request)})]
    (if (:error prepared)
      prepared
      (let [found (edit-dsl/evaluate-xray (:source snapshot) prepared)]
        (if (:error found)
          found
          (let [normalized (json-data found)
                sources (keep :source (:matches found))]
            (-> normalized
                (assoc :id (:id request)
                       :operation "xray"
                       :file (:file request)
                       :file_hash (:hash snapshot)
                       :source_character_count (reduce + 0 (map count sources))))))))))

(defn- evaluate-request
  [request snapshot]
  (try
    (case (:operation request)
      "forms" (forms-result request snapshot)
      "outline" (outline-result request snapshot)
      "match" (match-result request snapshot)
      "xray" (xray-result request snapshot))
    (catch Exception error
      {:error (.getMessage error)
       :error-type (or (:error-type (ex-data error)) :invalid-source)})))

(defn enforce-output-budget
  "Apply inclusive per-request source/result and aggregate result limits.

  Returns the original result vector on success and never truncates data."
  ([results]
   (enforce-output-budget results default-output-limits))
  ([results limits]
   (let [{:keys [per-request-source per-request-result aggregate-result]}
         (merge default-output-limits limits)
         failure
         (some (fn [[index result]]
                 (let [source-count (or (:source_character_count result) 0)
                       result-count (json-character-count result)]
                   (cond
                     (> source-count per-request-source)
                     {:scope "request_source" :request_index index
                      :actual source-count :limit per-request-source}

                     (> result-count per-request-result)
                     {:scope "request_result" :request_index index
                      :actual result-count :limit per-request-result})))
               (map-indexed vector results))
         aggregate-count (json-character-count results)
         failure (or failure
                     (when (> aggregate-count aggregate-result)
                       {:scope "aggregate_result"
                        :actual aggregate-count :limit aggregate-result}))]
     (if failure
       (merge
         {:ok false
          :operation "inspect_clojure"
          :error_type "inspect-output-limit"
          :error "inspect_clojure output exceeds a hard limit; no partial result is returned"
          :read_complete false
          :source_unchanged true
          :next_action "request_less_evidence"}
         failure)
       {:ok true
        :results results
        :result_character_count aggregate-count}))))

(defn- snapshot-guards-for
  [requests snapshots incoming-guards]
  (into
    (array-map)
    (map (fn [file] [file (:hash (get snapshots file))]))
    (distinct (concat (keys incoming-guards) (map :file requests)))))

;; @spec MCP-OP-READ-CONT-001 MCP-OP-READ-CONT-002
(defn- selector-retry-template
  [pending-requests selector-result snapshot-guards]
  (let [failed-values (set (map (comp str :form) (:failures selector-result)))
        failed-request (first pending-requests)
        holes (->> (:forms failed-request)
                   (map-indexed
                     (fn [index form]
                       (when (contains? failed-values form)
                         {:path ["requests" 0 "forms" index]
                          :request_id (:id failed-request)
                          :kind "exact-top-level-owner"
                          :rejected_value form
                          :must_replace true
                          :authority false})))
                   (remove nil?)
                   vec)
        pending-with-holes
        (assoc-in pending-requests [0 :forms]
                  (mapv (fn [form]
                          (when-not (contains? failed-values form) form))
                        (:forms failed-request)))]
    {:executable false
     :snapshot_bound true
     :selector_authority false
     :write_authority false
     :arguments
     {:snapshot_guards snapshot-guards
      :requests pending-with-holes
      :expect {:requests (count pending-requests)
               :files (count (distinct (map :file pending-requests)))}}
     :holes holes}))

(defn- selector-continuation
  [requests failed-index completed-results snapshots incoming-guards
   selector-result limits]
  (when (seq completed-results)
    (let [budget (enforce-output-budget completed-results limits)
          pending-requests (subvec requests failed-index)
          snapshot-guards (snapshot-guards-for requests snapshots incoming-guards)]
      (if-not (:ok budget)
        budget
        {:ok true
         :continuation
         {:snapshot_bound true
          :selector_authority false
          :write_authority false
          :completed_request_count (count completed-results)
          :completed_request_ids (mapv :id completed-results)
          :pending_request_count (count pending-requests)
          :pending_request_ids (mapv :id pending-requests)
          :snapshot_guards snapshot-guards
          :completed_results completed-results
          :retry_template
          (selector-retry-template
            pending-requests selector-result snapshot-guards)}}))))

(defn evaluate-snapshots
  "Evaluate a validated ordered request batch over supplied immutable snapshots.

  `snapshots` maps each request-relative file to `{:file :source :hash}`."
  ([params snapshots]
   (evaluate-snapshots params snapshots default-output-limits))
  ([{:keys [requests expect snapshot-guards]} snapshots limits]
   (loop [index 0
          results []]
     (if (< index (count requests))
       (let [request (nth requests index)
             snapshot (get snapshots (:file request))]
         (if-not snapshot
           {:ok false
            :operation "inspect_clojure"
            :error_type "missing-snapshot"
            :error "A validated request has no captured file snapshot"
            :request_id (:id request)
            :request_index index
            :read_complete false
            :source_unchanged true
            :next_action "retry_call"}
           (let [result (evaluate-request request snapshot)]
             (if (:error result)
               (let [refusal (kernel-refusal request index result)
                     continuation
                     (when (= :selector (:failed-stage result))
                       (selector-continuation
                         requests index results snapshots snapshot-guards
                         result limits))]
                 (cond
                   (and continuation (not (:ok continuation))) continuation
                   continuation (assoc refusal :continuation
                                       (:continuation continuation))
                   :else refusal))
               (recur (inc index) (conj results result))))))
       (let [budget (enforce-output-budget results limits)]
         (if-not (:ok budget)
           budget
           (let [files (distinct (map :file requests))
                 file-hashes (into (array-map)
                                   (map (fn [file]
                                          [file (:hash (get snapshots file))]))
                                   files)
                 source-count (reduce + 0 (map :source_character_count results))]
             (cond->
               {:ok true
                :operation "inspect_clojure"
                :read_complete true
                :request_count (:requests expect)
                :file_count (:files expect)
                :results results
                :file_hashes file-hashes
                :source_character_count source-count
                :result_character_count (:result_character_count budget)
                :next_action "none"}
               snapshot-guards (assoc :snapshot_guards snapshot-guards)))))))))

(defn- plural
  [count singular]
  (str count " " singular
       (when (not= 1 count)
         (if (= "match" singular) "es" "s"))))

(defn- compact-json
  [value limit]
  (let [rendered (json/generate-string (json-data value))]
    (when (<= (count rendered) limit) rendered)))

(defn- concise-result-line
  [result]
  (case (:operation result)
    "forms"
    (when (seq (:forms result))
      (str "  " (:id result) ": "
           (str/join ", "
                     (map #(str (:name %) "@" (:line %) "-" (:end_line %))
                          (:forms result)))
           " · " (:source_character_count result) " source characters"))

    "outline"
    (when-let [outline (:outline result)]
      (let [forms (:forms outline)]
        (str "  " (:id result) ": " (:lines outline) " lines · "
             (:form_count outline) " forms · first " (:name (first forms))
             " · last " (:name (last forms))
             (when (some #(contains? % :string_symbols) forms)
               (str " · "
                    (plural (reduce + 0 (map #(count (:string_symbols %)) forms))
                            "string symbol"))))))

    "match"
    (when (:id result)
      (let [matches (:matches result)
            compact (compact-json
                      (mapv #(select-keys % [:inside :source]) matches) 1024)]
        (when compact
          (str "  " (:id result) ": "
               (plural (:match_count result) "match") " · " compact
               ;; @spec MCP-OP-FIELD-003
               (when (:note result)
                 (str "\n    note: " (:note result)))))))

    "xray"
    (when (contains? result :value)
      (when-let [compact (compact-json (:value result) 1024)]
        (str "  " (:id result) ": value " compact)))

    nil))

(defn concise-summary
  "Render the ordinary source-free MCP text companion to structuredContent."
  [result]
  (let [forms (reduce + 0 (map #(or (:form_count %) 0) (:results result)))
        matches (reduce + 0 (map #(or (:match_count %) 0) (:results result)))
        evidence-lines (keep concise-result-line (:results result))
        facts (cond-> [(plural (:request_count result) "request")
                       (plural (:file_count result) "file")]
                (pos? forms) (conj (plural forms "form"))
                (pos? matches) (conj (plural matches "match")))
        elapsed (:elapsed_ms result)]
    (str "inspect_clojure\n"
         "  " (str/join " · " facts) "\n\n"
         "✓ all requests resolved\n"
         "✓ ordered snapshot\n"
         "✓ hashes attached\n"
         "✓ terminal evidence · read_complete=true · next action none\n"
         (when (seq evidence-lines)
           (str "\n" (str/join "\n" evidence-lines) "\n"))
         "  " (format "%,d" (long (:source_character_count result)))
         " source characters · "
         (mcp-operation/format-elapsed-ms elapsed))))
