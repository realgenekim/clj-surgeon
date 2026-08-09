(ns clj-surgeon.mcp-contract
  (:require
   [clojure.string :as str])
  (:import
   (java.nio.file Path Paths)))

(def ^:private top-fields #{"changes" "expect"})
(def ^:private change-fields
  #{"id" "files" "forms" "owner" "find" "inside" "replace"
    "insert_before" "insert_after" "rename_binding" "assoc_entry" "expect"})
(def ^:private change-expect-fields #{"matches" "each_form" "each_file"})
(def ^:private aggregate-expect-fields #{"changes" "edits" "files"})
(def ^:private supported-source-extensions #{"clj" "cljs" "cljc"})

(def ^:private prewrite-error-types
  #{:invalid-mcp-request
    :invalid-transaction-spec
    :invalid-changes
    :unknown-change-arguments
    :invalid-change-id
    :duplicate-change-id
    :invalid-files
    :duplicate-file
    :invalid-forms
    :invalid-change-forms
    :invalid-change-expectation
    :unknown-change-expectation-arguments
    :unsupported-change-operator
    :invalid-transaction-expectation
    :unknown-transaction-arguments
    :invalid-source-form
    :invalid-intent-form
    :invalid-intents
    :mixed-transaction-modes
    :no-op-intent
    :form-not-found
    :ambiguous-form
    :change-expectation-mismatch
    :expect-count-mismatch
    :change-distribution-mismatch
    :change-owner-mismatch
    :transaction-expectation-mismatch
    :overlapping-intents
    :invalid-source
    :unsupported-file
    :source-read-failed
    :source-hash-mismatch
    :unknown-arguments})

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
         :error-type :invalid-mcp-request
         :reason reason
         :path path
         :error message
         :remedy "Correct the named field and call apply_clojure_changes once. No source was changed."}
        data))))

(defn- validate-fields!
  [m allowed required path]
  (when-not (map? m)
    (refuse! :expected-object path "Expected a JSON object"))
  (let [actual (set (map field-name (keys m)))
        unknown (vec (sort (remove allowed actual)))
        missing (vec (sort (remove #(contains? actual %) required)))]
    (when (seq unknown)
      (refuse! :unknown-fields path "Request contains unknown fields"
               {:unknown unknown
                :allowed (vec (sort allowed))}))
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

(defn- relative-source-path?
  [value]
  (when (string? value)
    (let [portable (str/replace value "\\" "/")
          segments (str/split portable #"/" -1)
          extension (some-> portable (str/split #"\.") last)]
      (and (not (str/blank? portable))
           (not (str/starts-with? portable "/"))
           (not (re-find #"(?i)^[a-z]:/" portable))
           (not (str/includes? portable "\u0000"))
           (every? #(and (not (str/blank? %))
                         (not (#{"." ".."} %)))
                   segments)
           (contains? supported-source-extensions extension)))))

(defn- source-path!
  [value path]
  (when-not (relative-source-path? value)
    (refuse! :invalid-relative-source-path path
             "Expected a project-relative .clj, .cljs, or .cljc path without parent traversal"))
  value)

(defn- validate-count-map!
  [value allowed required path]
  (validate-fields! value allowed required path)
  (reduce
    (fn [result key]
      (if (present? value key)
        (assoc result
               (keyword (str/replace key "_" "-"))
               (positive-integer! (field value key) (conj path key)))
        result))
    {}
    (sort allowed)))

(defn- validate-change!
  [change index]
  (let [path ["changes" index]
        binding-rename? (and (map? change)
                             (present? change "rename_binding"))
        required (cond-> #{"id" "files" "expect"}
                   (not binding-rename?) (conj "find"))]
    (validate-fields! change change-fields required path)
    (let [forms? (present? change "forms")
          owner? (present? change "owner")]
      (when (= forms? owner?)
        (refuse! :ambiguous-change-owner path
                 "Provide exactly one of forms or owner"))
      (let [actions (filterv #(present? change %)
                             ["replace" "insert_before" "insert_after"
                              "rename_binding" "assoc_entry"])]
        (when-not (= 1 (count actions))
          (refuse! :ambiguous-change-action path
                   "Provide exactly one change action"))
        (let [id (nonblank-string! (field change "id") (conj path "id"))
              files (nonempty-array! (field change "files") (conj path "files"))
              forms (when forms?
                      (nonempty-array! (field change "forms")
                                       (conj path "forms")))
              owner (when owner?
                      (let [value (field change "owner")
                            owner-path (conj path "owner")
                            fields #{"kind" "name"}]
                        (validate-fields! value fields fields owner-path)
                        (let [kind (nonblank-string! (field value "kind")
                                                     (conj owner-path "kind"))
                              name (nonblank-string! (field value "name")
                                                     (conj owner-path "name"))]
                          (when-not (= "namespace" kind)
                            (refuse! :invalid-owner-kind
                                     (conj owner-path "kind")
                                     "Owner kind must be namespace"))
                          {:kind :namespace :name (symbol name)})))
              action (first actions)
              action-value
              (cond
                (= "replace" action)
                (nonblank-string! (field change action) (conj path action))

                (= "rename_binding" action)
                (let [rename (field change action)
                      rename-path (conj path action)
                      rename-fields #{"from" "to" "preserve_external_key"}]
                  (validate-fields! rename rename-fields rename-fields rename-path)
                  (let [from (nonblank-string! (field rename "from")
                                               (conj rename-path "from"))
                        to (nonblank-string! (field rename "to")
                                             (conj rename-path "to"))
                        preserve? (field rename "preserve_external_key")]
                    (when-not (= true preserve?)
                      (refuse! :unsafe-binding-rename
                               (conj rename-path "preserve_external_key")
                               "Binding rename requires preserve_external_key=true"))
                    {:from from :to to :preserve-external-key true}))

                (= "assoc_entry" action)
                (let [entry (field change action)
                      entry-path (conj path action)
                      entry-fields #{"key" "value"}]
                  (validate-fields! entry entry-fields entry-fields entry-path)
                  {:key (nonblank-string! (field entry "key")
                                          (conj entry-path "key"))
                   :value (nonblank-string! (field entry "value")
                                            (conj entry-path "value"))})

                :else
                (let [sources (nonempty-array! (field change action)
                                               (conj path action))]
                  (mapv (fn [source source-index]
                          (nonblank-string! source
                                            (conj path action source-index)))
                        sources (range))))]
          (when (and (= "rename_binding" action) (not forms?))
            (refuse! :invalid-binding-rename-owner path
                     "rename_binding requires exact named forms"))
          (when (and (= "rename_binding" action) (present? change "find"))
            (refuse! :unexpected-binding-rename-find path
                     "rename_binding does not accept find"))
          (when (and (present? change "inside")
                     (not= "assoc_entry" action))
            (refuse! :invalid-ancestor-selector path
                     "inside is only valid with assoc_entry"))
          (cond->
            {:id id
             :files (mapv (fn [file file-index]
                            (source-path! file (conj path "files" file-index)))
                          files (range))
             :expect (validate-count-map!
                       (field change "expect")
                       change-expect-fields #{"matches"}
                       (conj path "expect"))}
            (not= "rename_binding" action)
            (assoc :find
                   (nonblank-string! (field change "find") (conj path "find")))
            (present? change "inside")
            (assoc :inside
                   (nonblank-string! (field change "inside")
                                     (conj path "inside")))
            forms (assoc :forms
                         (mapv (fn [form form-index]
                                 (nonblank-string!
                                   form
                                   (conj path "forms" form-index)))
                               forms (range)))
            owner (assoc :owner owner)
            (= "replace" action) (assoc :replace action-value)
            (= "insert_before" action) (assoc :insert-before action-value)
            (= "insert_after" action) (assoc :insert-after action-value)
            (= "rename_binding" action) (assoc :rename-binding action-value)
            (= "assoc_entry" action) (assoc :assoc-entry action-value)))))))

(defn json-containers->clj
  "Recursively convert Java JSON containers from MCP SDKs to Clojure values."
  [value]
  (cond
    (instance? java.util.Map value)
    (into {} (map (fn [[key child]]
                    [key (json-containers->clj child)]))
          value)

    (instance? java.util.List value)
    (mapv json-containers->clj value)

    :else value))

(defn validate-tool-params
  "Validate JSON-shaped apply_clojure_changes parameters and return normalized data.

  This function is pure. It never reads source or resolves filesystem paths."
  [params]
  (let [params (json-containers->clj params)]
    (try
      (validate-fields! params top-fields top-fields [])
      (let [raw-changes (nonempty-array! (field params "changes") ["changes"])
            changes (mapv validate-change! raw-changes (range))]
        (loop [seen #{}
               index 0]
          (when (< index (count changes))
            (let [id (:id (nth changes index))]
              (when (contains? seen id)
                (refuse! :duplicate-id ["changes" index "id"]
                         "Change IDs must be unique" {:id id}))
              (recur (conj seen id) (inc index)))))
        {:ok true
         :params
         {:changes changes
          :expect (validate-count-map!
                    (field params "expect")
                    aggregate-expect-fields aggregate-expect-fields
                    ["expect"])}})
      (catch clojure.lang.ExceptionInfo error
        (ex-data error)))))

(defn tool-params->transaction
  "Compile normalized apply_clojure_changes parameters to transaction EDN."
  [{:keys [changes expect]}]
  {:changes
   (mapv
     (fn [{:keys [id files forms owner find inside replace insert-before insert-after
                  rename-binding assoc-entry expect]}]
       (cond->
         {:id (keyword id)
          :in files
          :find find
          :do (cond
                replace [:replace replace]
                insert-before [:insert-left insert-before]
                insert-after [:insert-right insert-after]
                rename-binding [:rename-binding
                                {:from (symbol (:from rename-binding))
                                 :to (symbol (:to rename-binding))
                                 :preserve-external-key
                                 (:preserve-external-key rename-binding)}]
                :else [:assoc-entry assoc-entry])
          :expect expect}
         rename-binding (dissoc :find)
         inside (assoc :inside inside)
         forms (assoc :forms (mapv symbol forms))
         owner (assoc :owner owner)))
     changes)
   :expect expect})

(defn- normalized-root
  ^Path [root]
  (.normalize (.toAbsolutePath (Paths/get (str root) (make-array String 0)))))

(defn- relative-path
  [root path]
  (when (and (string? path) (not (str/blank? path)))
    (let [root-path (normalized-root root)
          candidate (let [p (Paths/get path (make-array String 0))]
                      (.normalize
                        (if (.isAbsolute p) p (.resolve root-path p))))]
      (when (.startsWith candidate root-path)
        (-> (.toString (.relativize root-path candidate))
            (str/replace "\\" "/"))))))

(defn normalize-refusal
  "Return the stable, compact refusal surface used by the MCP callback."
  [result]
  (let [error-type (:error-type result)
        change-index (if (contains? result :change-index)
                       (:change-index result)
                       (:intent-index result))
        change-id (let [value (:change-id result)]
                    (if (keyword? value) (name value) value))
        field (:field result)
        remedy (or
                 (:remedy result)
                 (when (= :invalid-intent-form error-type)
                   (format
                     "Correct %s for change %s%s. Complete-input parser: %s Submit exactly one complete Clojure form."
                     (or field "the named field")
                     (if (some? change-index) change-index "unknown")
                     (if change-id (str " (" change-id ")") "")
                     (or (:error result) "the input was not exactly one form.")))
                 "Correct the declared scope or count and call apply_clojure_changes once.")]
    (cond->
      {:ok false
       :error_type (if (keyword? error-type) (name error-type) (str error-type))
       :error (or (:error result) "apply_clojure_changes refused")
       :source_unchanged
       (if (contains? result :source-unchanged)
         (boolean (:source-unchanged result))
         (boolean
           (or (:rolled-back result)
               (contains? prewrite-error-types error-type))))
       :remedy remedy}
      (contains? result :expected) (assoc :expected (:expected result))
      (contains? result :actual) (assoc :actual (:actual result))
      (contains? result :reason) (assoc :reason (some-> (:reason result) name))
      (contains? result :path) (assoc :path (:path result))
      (contains? result :unknown) (assoc :unknown (:unknown result))
      (contains? result :allowed) (assoc :allowed (:allowed result))
      (contains? result :missing) (assoc :missing (:missing result))
      (some? change-index) (assoc :change_index change-index)
      (contains? result :change-id) (assoc :change_id change-id)
      (contains? result :field) (assoc :field field)
      (contains? result :form-count) (assoc :form_count (:form-count result))
      (contains? result :expected-count) (assoc :expected_count (:expected-count result))
      (contains? result :actual-count) (assoc :actual_count (:actual-count result))
      (contains? result :per-file-counts) (assoc :per_file_counts (:per-file-counts result))
      (contains? result :per-form-counts) (assoc :per_form_counts (:per-form-counts result))
      (contains? result :distribution) (assoc :distribution (some-> (:distribution result) name))
      (contains? result :operation) (assoc :kernel_operation (some-> (:operation result) name))
      (contains? result :phase) (assoc :kernel_phase (some-> (:phase result) name))
      (contains? result :owner) (assoc :owner (:owner result))
      (contains? result :file) (assoc :file (:file result))
      (contains? result :rolled-back) (assoc :rolled_back (:rolled-back result))
      (contains? result :cause-error) (assoc :cause_error (:cause-error result))
      (contains? result :remedies) (assoc :remedies (:remedies result)))))

(defn normalize-success-receipt
  "Reduce a complete kernel result to terminal verification evidence. Requires read-back hashes and an inverse receipt."
  [project-root result]
  (when-not (and (:ok result)
                 (:committed result)
                 (true? (get-in result [:verified :whole-files]))
                 (map? (get-in result [:verified :read-back-hashes]))
                 (string? (:receipt-file result))
                 (not (str/blank? (:receipt-file result))))
    (throw (ex-info "Kernel success lacks terminal verification"
                    {:reason :incomplete-verification})))
  (let [hashes
        (reduce-kv
          (fn [normalized path hash]
            (if-let [relative (relative-path project-root path)]
              (assoc normalized relative hash)
              (throw (ex-info "Read-back hash path is outside the project root"
                              {:reason :path-outside-project :path path}))))
          (sorted-map)
          (get-in result [:verified :read-back-hashes]))
        receipt (:receipt-file result)]
    {:ok true
     :operation "apply_clojure_changes"
     :committed true
     :changes (or (:change-count result) (:intent-count result))
     :edits (:match-count result)
     :files (:changed-file-count result)
     :verification_complete true
     :read_back_hashes hashes
     :undo_receipt receipt
     :receipt_hash (:receipt-hash result)
     :next_action "none"}))

(defn classify-kernel-result
  "Classify one direct kernel result without weakening incomplete success."
  [project-root result]
  (if (:error result)
    (let [normalized (normalize-refusal result)]
      (cond-> (assoc normalized :phase "kernel")
        (not (contains? normalized :reason))
        (assoc :reason (:error_type normalized))))
    (try
      (normalize-success-receipt project-root result)
      (catch clojure.lang.ExceptionInfo error
        {:ok false
         :error_type "invalid-kernel-result"
         :error (.getMessage error)
         :reason (:reason (ex-data error))
         :source_unchanged false
         :remedy "Treat this result as failed verification; inspect the MCP server log."}))))
