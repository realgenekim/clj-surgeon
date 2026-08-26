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
   [clojure.string :as str]))

(def max-requests 64)
(def max-files 32)
(def max-forms 128)
(def default-output-limits
  {:per-request-source 65536
   :per-request-result 65536
   :aggregate-result 262144})

(def ^:private top-fields #{"requests" "expect"})
(def ^:private top-expect-fields #{"requests" "files"})
(def ^:private common-request-fields #{"id" "operation" "file"})
(def ^:private operation-fields
  {"forms" (into common-request-fields ["forms" "expect" "include_source"])
   "outline" common-request-fields
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
    (keyword? key) (name key)
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
               {:missing missing}))))

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
                 {:missing (vec missing)})))
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
          {:id id :operation operation :file file}

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

(defn validate-inspect-params
  "Validate JSON-shaped inspect_clojure input and return normalized Clojure data."
  [params]
  (let [params (mcp-contract/json-containers->clj params)]
    (try
      (validate-fields! params top-fields top-fields [])
      (let [raw-requests (nonempty-array! (field params "requests") ["requests"])
            _ (when (> (count raw-requests) max-requests)
                (refuse! :too-many-requests ["requests"]
                         "Inspect batch exceeds the maximum request count"
                         {:maximum max-requests :actual (count raw-requests)}))
            requests (mapv validate-request! raw-requests (range))
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
         :params {:requests requests
                  :expect {:requests expected-requests
                           :files expected-files}}})
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
      (contains? result :expected) (assoc :expected (json-data (:expected result)))
      (contains? result :actual) (assoc :actual (json-data (:actual result)))
      (contains? result :expected-match-count)
      (assoc :expected_match_count (:expected-match-count result))
      (contains? result :actual-match-count)
      (assoc :actual_match_count (:actual-match-count result))
      (contains? result :match-count)
      (assoc :match_count (:match-count result))
      (contains? result :failure-count)
      (assoc :failure_count (:failure-count result))
      (contains? result :available-form-count)
      (assoc :available_form_count (:available-form-count result))
      (contains? result :form-candidates)
      (assoc :form_candidates (json-data (:form-candidates result))))))

(defn- forms-result
  [request snapshot]
  (let [found (show-form/select-form
                (:file request) (:source snapshot)
                {:forms (mapv symbol (:forms request))})]
    (if (:error found)
      (let [requested (mapv str (:forms request))
            available
            (->> (outline/outline-source (:file request) (:source snapshot))
                 :forms
                 (keep :name)
                 (map str)
                 distinct
                 vec)
            common-prefix-length
            (fn [left right]
              (count (take-while true? (map = left right))))
            candidates
            (->> available
                 (sort-by
                   (fn [candidate]
                     [(- (apply max 0
                                (map #(common-prefix-length % candidate)
                                     requested)))
                      candidate]))
                 (take 8)
                 vec)]
        (assoc found
               :available-form-count (count available)
               :form-candidates candidates))
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
              (outline/outline-source (:file request) (:source snapshot)))})

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
      {:error "Structural match cardinality did not meet the declared expectation"
       :error-type :inspect-cardinality-mismatch
       :expected (get-in request [:expect :matches])
       :actual (:match-count found)
       :match-count (:match-count found)}

      :else
      (let [matches (mapv (fn [match]
                            (assoc (json-data match)
                                   :hash (structural-lens/source-hash
                                           (:source match))
                                   :file_hash (:hash snapshot)))
                          (:matches found))]
        {:id (:id request)
         :operation "match"
         :file (:file request)
         :file_hash (:hash snapshot)
         :match (:match request)
         :inside (:inside request)
         :match_count (:match-count found)
         :source_character_count (reduce + 0 (map #(count (:source %))
                                                  (:matches found)))
         :matches matches}))))

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

(defn evaluate-snapshots
  "Evaluate a validated ordered request batch over supplied immutable snapshots.

  `snapshots` maps each request-relative file to `{:file :source :hash}`."
  ([params snapshots]
   (evaluate-snapshots params snapshots default-output-limits))
  ([{:keys [requests expect]} snapshots limits]
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
               (kernel-refusal request index result)
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
             {:ok true
              :operation "inspect_clojure"
              :read_complete true
              :request_count (:requests expect)
              :file_count (:files expect)
              :results results
              :file_hashes file-hashes
              :source_character_count source-count
              :result_character_count (:result_character_count budget)
              :next_action "none"})))))))

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
             " · last " (:name (last forms)))))

    "match"
    (when (:id result)
      (let [matches (:matches result)
            compact (compact-json
                      (mapv #(select-keys % [:inside :source]) matches) 1024)]
        (when compact
          (str "  " (:id result) ": "
               (plural (:match_count result) "match") " · " compact))))

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
