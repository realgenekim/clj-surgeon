(ns clj-surgeon.mcp-contract
  (:require
   [clj-surgeon.mcp-extraction :as mcp-extraction]
   [clj-surgeon.mcp-schema :as mcp-schema]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser])
  (:import
   (java.nio.file Path Paths)))

(def ^:private direct-contract mcp-schema/direct-change-contract)
(def ^:private top-fields (get-in direct-contract [:request :allowed]))
(def ^:private required-top-fields (get-in direct-contract [:request :required]))
(def ^:private change-fields (get-in direct-contract [:change :allowed]))
(def ^:private required-change-fields (get-in direct-contract [:change :required]))
(def ^:private change-expect-fields (get-in direct-contract [:expect :allowed]))
(def ^:private required-change-expect-fields
  (get-in direct-contract [:expect :required]))
(def ^:private aggregate-expect-fields
  (get-in direct-contract [:aggregate-expect :allowed]))
(def ^:private required-aggregate-expect-fields
  (get-in direct-contract [:aggregate-expect :required]))
(def ^:private owner-fields (get-in direct-contract [:owner :allowed]))
(def ^:private required-owner-fields (get-in direct-contract [:owner :required]))
(def ^:private form-owner-fields (get-in direct-contract [:form-owner :allowed]))
(def ^:private required-form-owner-fields
  (get-in direct-contract [:form-owner :required]))
(def ^:private rename-fields (get-in direct-contract [:rename-binding :allowed]))
(def ^:private required-rename-fields
  (get-in direct-contract [:rename-binding :required]))
(def ^:private entry-fields (get-in direct-contract [:assoc-entry :allowed]))
(def ^:private required-entry-fields
  (get-in direct-contract [:assoc-entry :required]))
(def ^:private editor-gesture-contract mcp-schema/editor-gesture-contract)
(def ^:private editor-top-fields
  (get-in editor-gesture-contract [:request :allowed]))
(def ^:private required-editor-top-fields
  (get-in editor-gesture-contract [:request :required]))
(def ^:private editor-fields
  (get-in editor-gesture-contract [:edit :allowed]))
(def ^:private required-editor-fields
  (get-in editor-gesture-contract [:edit :required]))
(def ^:private editor-within-fields
  (get-in editor-gesture-contract [:within :allowed]))
(def ^:private required-editor-within-fields
  (get-in editor-gesture-contract [:within :required]))
(def ^:private editor-program-fields
  (get-in editor-gesture-contract [:program :allowed]))
(def ^:private required-editor-program-fields
  (get-in editor-gesture-contract [:program :required]))
(def ^:private editor-program-expect-fields
  (get-in editor-gesture-contract [:program-expect :allowed]))
(def ^:private required-editor-program-expect-fields
  (get-in editor-gesture-contract [:program-expect :required]))
(def ^:private editor-deletion-fields
  (get-in editor-gesture-contract [:deletion :allowed]))
(def ^:private required-editor-deletion-fields
  (get-in editor-gesture-contract [:deletion :required]))
(def ^:private supported-source-extensions #{"clj" "cljs" "cljc" "edn"})

(def ^:private prewrite-error-types
  #{:invalid-mcp-request
    :invalid-transaction-spec
    :invalid-changes
    :unknown-change-arguments
    :invalid-change-id
    :duplicate-change-id
    :invalid-files
    :duplicate-file
    :ambiguous-editor-files
    :invalid-grouped-editor-scope
    :invalid-edn-editor-scope
    :invalid-root-scope
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
    :extraction-decisions-required
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

(defn- without-field
  [m key]
  (into (empty m)
        (remove (fn [[candidate _]]
                  (= key (field-name candidate))))
        m))

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
         :source-unchanged true
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
             "Expected a project-relative .clj, .cljs, .cljc, or .edn path without parent traversal"))
  value)

(defn- edn-path?
  [value]
  (str/ends-with? (str/lower-case value) ".edn"))

(defn- clojure-source-path!
  [value path]
  (let [value (source-path! value path)]
    (when (edn-path? value)
      (refuse! :invalid-edn-editor-scope path
               "EDN is supported only by root-scoped exact edits"))
    value))

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

(defn- complete-insertion-forms
  [source path]
  (let [source (nonblank-string! source path)]
    (try
      (let [forms (->> (node/children (parser/parse-string-all source))
                       (remove node/whitespace?)
                       vec)]
        (when (some node/comment? forms)
          (refuse! :invalid-intent-form path
                   "Insertion strings may not contain detached comments"
                   {:error-type :invalid-intent-form}))
        (when (empty? forms)
          (refuse! :invalid-intent-form path
                   "Insertion strings must contain at least one complete form"
                   {:error-type :invalid-intent-form}))
        (mapv node/string forms))
      (catch clojure.lang.ExceptionInfo error
        (if (:reason (ex-data error))
          (throw error)
          (refuse! :invalid-intent-form path
                   (str "Invalid insertion form: " (.getMessage error))
                   {:error-type :invalid-intent-form})))
      (catch Exception error
        (refuse! :invalid-intent-form path
                 (str "Invalid insertion form: " (.getMessage error))
                 {:error-type :invalid-intent-form})))))

(defn- validate-change!
  [change index]
  (let [path ["changes" index]
        binding-rename? (and (map? change)
                             (present? change "rename_binding"))
        delete? (and (map? change) (present? change "delete"))
        required (cond-> required-change-fields
                   (not (or binding-rename? delete?
                            (and (map? change)
                                 (or (present? change "insert_before")
                                     (present? change "insert_after")))))
                   (conj "find"))]
    (validate-fields! change change-fields required path)
    (let [forms? (present? change "forms")
          owner? (present? change "owner")]
      (when (and forms? owner?)
        (refuse! :ambiguous-change-owner path
                 "Provide at most one of forms or owner"))
      (when (and (or forms? owner?)
                 (some edn-path? (field change "files")))
        (refuse! :invalid-edn-editor-scope (conj path "files")
                 "EDN is supported only by root-scoped exact edits"))
      (let [actions (filterv #(present? change %)
                             ["replace" "delete" "insert_before" "insert_after"
                              "rename_binding" "assoc_entry"])]
        (when-not (= 1 (count actions))
          (refuse! :ambiguous-change-action path
                   "Provide exactly one change action"))
        (let [id (nonblank-string! (field change "id") (conj path "id"))
              files (nonempty-array! (field change "files") (conj path "files"))
              forms (when forms?
                      (let [values (nonempty-array! (field change "forms")
                                                    (conj path "forms"))]
                        (mapv
                          (fn [value form-index]
                            (let [form-path (conj path "forms" form-index)]
                              (if (map? value)
                                (do
                                  (validate-fields!
                                    value form-owner-fields
                                    required-form-owner-fields form-path)
                                  (let [kind (nonblank-string!
                                               (field value "kind")
                                               (conj form-path "kind"))
                                        name (nonblank-string!
                                               (field value "name")
                                               (conj form-path "name"))
                                        dispatch (nonblank-string!
                                                   (field value "dispatch")
                                                   (conj form-path "dispatch"))]
                                    (when-not (= "defmethod" kind)
                                      (refuse! :invalid-form-owner-kind
                                               (conj form-path "kind")
                                               "Form owner kind must be defmethod"))
                                    {:kind :defmethod
                                     :name (symbol name)
                                     :dispatch dispatch}))
                                (symbol (nonblank-string! value form-path)))))
                          values (range))))
              owner (when owner?
                      (let [value (field change "owner")
                            owner-path (conj path "owner")]
                        (validate-fields! value owner-fields required-owner-fields owner-path)
                        (let [kind (nonblank-string! (field value "kind")
                                                     (conj owner-path "kind"))
                              name (when (present? value "name")
                                     (nonblank-string! (field value "name")
                                                       (conj owner-path "name")))]
                          (when-not (= "namespace" kind)
                            (refuse! :invalid-owner-kind
                                     (conj owner-path "kind")
                                     "Owner kind must be namespace"))
                          (cond-> {:kind :namespace}
                            name (assoc :name (symbol name))))))
              action (first actions)
              action-value
              (cond
                (= "replace" action)
                (nonblank-string! (field change action) (conj path action))

                (= "delete" action)
                (let [value (field change action)]
                  (when-not (= true value)
                    (refuse! :invalid-delete-action
                             (conj path action)
                             "delete must be true"))
                  true)

                (= "rename_binding" action)
                (let [rename (field change action)
                      rename-path (conj path action)]
                  (validate-fields! rename rename-fields required-rename-fields rename-path)
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
                      entry-path (conj path action)]
                  (validate-fields! entry entry-fields required-entry-fields entry-path)
                  {:key (nonblank-string! (field entry "key")
                                          (conj entry-path "key"))
                   :value (nonblank-string! (field entry "value")
                                            (conj entry-path "value"))})

                :else
                (let [sources (nonempty-array! (field change action)
                                               (conj path action))]
                  (vec
                    (mapcat
                      (fn [source source-index]
                        (complete-insertion-forms
                          source (conj path action source-index)))
                      sources (range)))))]
          (when (and (#{"insert_before" "insert_after"} action)
                     (not (present? change "find"))
                     (not (and forms? (= 1 (count forms)))))
            (refuse! :invalid-top-level-insertion-owner
                     (conj path (if forms? "forms" "owner"))
                     "Insertion without find requires exactly one named form owner"))
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
                       change-expect-fields required-change-expect-fields
                       (conj path "expect"))}
            (present? change "find")
            (assoc :find
                   (nonblank-string! (field change "find") (conj path "find")))
            (present? change "inside")
            (assoc :inside
                   (nonblank-string! (field change "inside")
                                     (conj path "inside")))
            forms (assoc :forms forms)
            owner (assoc :owner owner)
            (= "replace" action) (assoc :replace action-value)
            (= "delete" action) (assoc :delete true)
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

(def extraction-fields
  #{"file" "to" "forms" "require_policy" "caller_changes"
    "ignored_caller_files" "public_forms" "expect" "source_hash"})

(def extraction-expect-fields #{"forms" "caller_edits" "files"})

(defn- nonnegative-integer!
  [value path]
  (when-not (and (integer? value) (<= 0 value))
    (refuse! :expected-nonnegative-integer path
             "Expected a non-negative integer" {:actual value}))
  value)

;; @spec MCP-OP-PLAN-003
;; @spec MCP-OP-PLAN-005
(defn validate-extraction-tool-params
  [params]
  (let [params (json-containers->clj params)]
    (try
      (validate-fields! params #{"workspace_root" "extraction" "verify"}
                        #{"extraction"} [])
      (let [raw (field params "extraction")]
        (validate-fields! raw extraction-fields
                          #{"file" "to" "forms" "require_policy"}
                          ["extraction"])
        (let [file (clojure-source-path! (field raw "file") ["extraction" "file"])
              to (clojure-source-path! (field raw "to") ["extraction" "to"])
              forms (mapv #(nonblank-string! % ["extraction" "forms"])
                          (nonempty-array! (field raw "forms")
                                           ["extraction" "forms"]))
              _ (when-not (= (count forms) (count (distinct forms)))
                  (refuse! :duplicate-form ["extraction" "forms"]
                           "Extraction form names must be unique"))
              public-forms-present? (present? raw "public_forms")
              public-forms
              (when public-forms-present?
                (mapv #(nonblank-string! % ["extraction" "public_forms"])
                      (field raw "public_forms")))
              _ (when (and public-forms-present?
                           (not= (count public-forms)
                                 (count (distinct public-forms))))
                  (refuse! :duplicate-form ["extraction" "public_forms"]
                           "Public form names must be unique"))
              require-policy (nonblank-string!
                               (field raw "require_policy")
                               ["extraction" "require_policy"])
              _ (when-not (#{"minimal" "copy-all"} require-policy)
                  (refuse! :invalid-enum ["extraction" "require_policy"]
                           "require_policy must be minimal or copy-all"))
              raw-callers (or (field raw "caller_changes") [])
              _ (when-not (vector? raw-callers)
                  (refuse! :expected-array ["extraction" "caller_changes"]
                           "Expected an array"))
              callers (mapv validate-change! raw-callers (range))
              ignored (mapv #(clojure-source-path! % ["extraction" "ignored_caller_files"])
                            (or (field raw "ignored_caller_files") []))
              _ (when-not (= (count ignored) (count (distinct ignored)))
                  (refuse! :duplicate-path ["extraction" "ignored_caller_files"]
                           "Ignored caller paths must be unique"))
              raw-expect (field raw "expect")
              derived-expect
              {:forms (count forms)
               :caller-edits (reduce + (map #(get-in % [:expect :matches]) callers))
               :files (count (distinct (concat [file to]
                                               (mapcat :files callers))))}
              expect
              (if (present? raw "expect")
                (do
                  (validate-fields! raw-expect extraction-expect-fields
                                    extraction-expect-fields ["extraction" "expect"])
                  {:forms (positive-integer! (field raw-expect "forms")
                                             ["extraction" "expect" "forms"])
                   :caller-edits
                   (nonnegative-integer! (field raw-expect "caller_edits")
                                         ["extraction" "expect" "caller_edits"])
                   :files (positive-integer! (field raw-expect "files")
                                             ["extraction" "expect" "files"])})
                derived-expect)
              source-hash
              (when (present? raw "source_hash")
                (nonblank-string! (field raw "source_hash")
                                  ["extraction" "source_hash"]))
              _ (when (and source-hash
                           (not (re-matches #"[0-9a-f]{64}" source-hash)))
                  (refuse! :invalid-source-hash ["extraction" "source_hash"]
                           "source_hash must be a lowercase SHA-256 hex string"))
              verify (when (present? params "verify")
                       (nonblank-string! (field params "verify") ["verify"]))
              normalized {:file file :to to :forms forms
                          :require-policy (keyword require-policy)
                          :caller-changes callers
                          :ignored-caller-files ignored
                          :expect expect}
              normalized (cond-> normalized
                           public-forms-present?
                           (assoc :public-forms public-forms)
                           source-hash (assoc :source-hash source-hash))
              validation (mcp-extraction/validate-request normalized)]
          (when-not (:ok validation)
            (refuse! (:error-type validation) ["extraction"]
                     (:error validation) validation))
          (when (and verify (not (#{"fast" "full" "exact"} verify)))
            (refuse! :invalid-enum ["verify"]
                     "verify must be fast, full, or exact"))
          {:ok true
           :params (cond-> {:extraction normalized}
                     verify (assoc :verify verify))}))
      (catch clojure.lang.ExceptionInfo error
        (ex-data error)))))

(defn- derived-aggregate-expect
  [changes]
  {:changes (count changes)
   :edits (reduce + (map #(get-in % [:expect :matches]) changes))
   :files (count (set (mapcat :files changes)))})

(defn- validate-direct-tool-params
  [params]
  (try
    (validate-fields! params top-fields required-top-fields [])
    (let [raw-changes (nonempty-array! (field params "changes") ["changes"])
          changes (mapv validate-change! raw-changes (range))
          supplied-expect
          (when (present? params "expect")
            (validate-count-map!
              (field params "expect")
              aggregate-expect-fields required-aggregate-expect-fields
              ["expect"]))
          derived-expect (derived-aggregate-expect changes)
          verify (when (present? params "verify")
                   (nonblank-string! (field params "verify") ["verify"]))]
      (when (and verify (not (#{"fast" "full" "exact"} verify)))
        (refuse! :invalid-enum ["verify"]
                 "verify must be fast, full, or exact"))
      (loop [seen #{}
             index 0]
        (when (< index (count changes))
          (let [id (:id (nth changes index))]
            (when (contains? seen id)
              (refuse! :duplicate-id ["changes" index "id"]
                       "Change IDs must be unique" {:id id}))
            (recur (conj seen id) (inc index)))))
      (cond-> {:ok true
               :params
               (cond-> {:changes changes
                        :expect derived-expect}
                 verify (assoc :verify verify))}
        (and supplied-expect (not= supplied-expect derived-expect))
        (assoc :input-normalization
               {:ignored ["expect"]
                :reason
                "aggregate counts are derived from exact change guards"})))
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

(defn- editor-gestures->direct-params
  [params]
  ;; @spec MCP-OP-EDIT-001
  ;; @spec MCP-OP-EDIT-002
  ;; @spec MCP-OP-EDIT-003
  ;; @spec MCP-OP-EDIT-004
  ;; @spec MCP-OP-EDIT-005
  (try
    (let [redundant-expect? (present? params "expect")
          params (without-field params "expect")]
      (validate-fields! params editor-top-fields required-editor-top-fields [])
      (let [edits
            (when (present? params "edits")
              (nonempty-array! (field params "edits") ["edits"]))
            edit-changes
            (mapv
              (fn [edit index]
                (let [path ["edits" index]
                      _ (validate-fields! edit editor-fields
                                          required-editor-fields path)
                      within (field edit "within")
                      _ (validate-fields! within editor-within-fields
                                          required-editor-within-fields
                                          (conj path "within"))
                      form? (present? within "form")
                      namespace? (present? within "namespace")
                      root? (present? within "root")
                      locations (count (filter true? [form? namespace? root?]))
                      _ (when-not (= 1 locations)
                          (refuse! :ambiguous-editor-location
                                   (conj path "within")
                                   "Provide exactly one of form, namespace, or root"))
                      _ (when (and root?
                                   (not= true (field within "root")))
                          (refuse! :invalid-root-scope
                                   (conj path "within" "root")
                                   "root must be true"))
                      file? (present? edit "file")
                      files? (present? edit "files")
                      _ (when (= file? files?)
                          (refuse! :ambiguous-editor-files path
                                   "Provide exactly one of file or files"))
                      files (if file?
                              [(source-path! (field edit "file")
                                             (conj path "file"))]
                              (mapv
                                (fn [file file-index]
                                  (source-path! file
                                                (conj path "files" file-index)))
                                (nonempty-array! (field edit "files")
                                                 (conj path "files"))
                                (range)))
                      _ (when-not (= (count files) (count (distinct files)))
                          (refuse! :duplicate-file (conj path "files")
                                   "Grouped edit files must be unique"))
                      _ (when (and files? (not root?))
                          (refuse! :invalid-grouped-editor-scope
                                   (conj path "within")
                                   "Grouped files require root scope"))
                      _ (when (and (some edn-path? files) (not root?))
                          (refuse! :invalid-edn-editor-scope
                                   (conj path "within")
                                   "EDN edits require root scope"))
                      matches (if (present? edit "matches")
                                (positive-integer! (field edit "matches")
                                                   (conj path "matches"))
                                1)
                      total-matches (* matches (count files))]
                  (cond->
                    {"id" (str "edit-" (inc index))
                     "files" files
                     "find" (nonblank-string! (field edit "from")
                                              (conj path "from"))
                     "replace" (nonblank-string! (field edit "to")
                                                 (conj path "to"))
                     "expect" {"matches" total-matches
                               "each_file" matches}}
                    form?
                    (assoc "forms"
                           [(nonblank-string! (field within "form")
                                              (conj path "within" "form"))]
                           "expect" {"matches" matches
                                     "each_form" matches
                                     "each_file" matches})

                    namespace?
                    (assoc
                      "owner"
                      (let [namespace (field within "namespace")]
                        (cond
                          (= true namespace)
                          {"kind" "namespace"}

                          (and (string? namespace)
                               (not (str/blank? namespace)))
                          {"kind" "namespace" "name" namespace}

                          :else
                          (refuse! :invalid-namespace-scope
                                   (conj path "within" "namespace")
                                   "namespace must be true or a non-blank name")))))))
              edits (range))
            deletion-groups
            (when (present? params "delete_owners")
              (nonempty-array! (field params "delete_owners")
                               ["delete_owners"]))
            deletion-changes
            (mapv
              (fn [deletion index]
                (let [path ["delete_owners" index]
                      _ (validate-fields!
                          deletion editor-deletion-fields
                          required-editor-deletion-fields path)
                      file (clojure-source-path! (field deletion "file")
                                                 (conj path "file"))
                      forms (mapv
                              (fn [form form-index]
                                (nonblank-string!
                                  form (conj path "forms" form-index)))
                              (nonempty-array! (field deletion "forms")
                                               (conj path "forms"))
                              (range))
                      _ (when-not (= (count forms) (count (distinct forms)))
                          (refuse! :duplicate-form (conj path "forms")
                                   "Deletion form names must be unique"))]
                  {"id" (str "delete-owners-" (inc index))
                   "files" [file]
                   "forms" forms
                   "delete" true
                   "expect" {"matches" (count forms)
                             "each_form" 1}}))
              deletion-groups (range))
            duplicate-owner
            (->> deletion-changes
                 (mapcat
                   (fn [change]
                     (map (fn [form] [(first (field change "files")) form])
                          (field change "forms"))))
                 frequencies
                 (some (fn [[owner count]] (when (< 1 count) owner))))
            _ (when duplicate-owner
                (refuse! :duplicate-form ["delete_owners"]
                         "A deletion owner may appear only once in the request"
                         {:file (first duplicate-owner)
                          :form (second duplicate-owner)}))
            changes (into edit-changes deletion-changes)
            programs
            (when (present? params "programs")
              (mapv
                (fn [program index]
                  (let [path ["programs" index]
                        _ (validate-fields! program editor-program-fields
                                            required-editor-program-fields path)
                        expect (field program "expect")
                        _ (validate-fields!
                            expect editor-program-expect-fields
                            required-editor-program-expect-fields
                            (conj path "expect"))]
                    {:file (clojure-source-path! (field program "file")
                                                 (conj path "file"))
                     :expression
                     (nonblank-string! (field program "expression")
                                       (conj path "expression"))
                     :expect
                     {:matches
                      (positive-integer! (field expect "matches")
                                         (conj path "expect" "matches"))
                      :max_changed_characters
                      (positive-integer!
                        (field expect "max_changed_characters")
                        (conj path "expect" "max_changed_characters"))}}))
                (nonempty-array! (field params "programs") ["programs"])
                (range)))
            direct
            (cond->
              {"changes" changes
               "expect" {"changes" (count changes)
                         "edits" (reduce + (map #(get-in % ["expect" "matches"])
                                                changes))
                         "files" (count (set (mapcat #(field % "files")
                                                     changes)))}}
              (present? params "verify")
              (assoc "verify" (field params "verify")))]
        (cond-> {:ok true :params direct}
          (seq programs)
          (assoc :programs programs)

          redundant-expect?
          (assoc :input-normalization
                 {:ignored ["expect"]
                  :reason "editor counts are derived"}))))
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

(defn validate-tool-params
  "Validate JSON-shaped apply_clojure_changes parameters and return normalized data.

  This function is pure. It never reads source or resolves filesystem paths."
  [params]
  (let [params (json-containers->clj params)]
    (cond
      (present? params "extraction")
      (validate-extraction-tool-params params)

      (some #(present? params %)
            ["edits" "programs" "delete_owners"])
      (let [compiled (editor-gestures->direct-params params)]
        (if (:ok compiled)
          (let [validated (validate-direct-tool-params (:params compiled))]
            (cond-> validated
              (and (:ok validated) (seq (:programs compiled)))
              (assoc-in [:params :programs] (:programs compiled))

              (:input-normalization compiled)
              (assoc :input-normalization (:input-normalization compiled))))
          compiled))

      :else
      (validate-direct-tool-params params))))

(defn tool-params->transaction
  "Compile normalized apply_clojure_changes parameters to transaction EDN."
  [{:keys [changes expect]}]
  {:changes
   (mapv
     (fn [{:keys [id files forms owner find inside replace delete insert-before insert-after
                  rename-binding assoc-entry expect]}]
       (cond->
         {:id (keyword id)
          :in files
          :find find
          :do (cond
                replace [:replace replace]
                delete [:delete true]
                insert-before [:insert-left insert-before]
                insert-after [:insert-right insert-after]
                rename-binding [:rename-binding
                                {:from (symbol (:from rename-binding))
                                 :to (symbol (:to rename-binding))
                                 :preserve-external-key
                                 (:preserve-external-key rename-binding)}]
                :else [:assoc-entry assoc-entry])
          :expect expect}
         (nil? find) (dissoc :find)
         inside (assoc :inside inside)
         forms (assoc :forms
                      (mapv #(if (map? %) % (symbol %)) forms))
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

(defn- bounded-text
  [value limit]
  (when (string? value)
    (subs value 0 (min limit (count value)))))

(defn- compact-verification
  [verification]
  (cond-> verification
    (contains? verification :checks)
    (update :checks
            (fn [checks]
              (mapv #(cond-> %
                       (:output %) (update :output bounded-text 2000))
                    checks)))

    (:diagnostics verification)
    (update :diagnostics bounded-text 2000)))

(defn- verification-remedy
  [verification]
  (if (= :exact-exit (:acceptance verification))
    (let [output (some-> (:diagnostics verification)
                         (str/replace #"\s+" " ")
                         (bounded-text 240))]
      (str (if (= :verification-failed (:error-type verification))
             "Correct the deterministic diagnostics from the project exact verifier"
             "Restore authority for the project exact verifier")
           (when (some? (:exit verification))
             (str " (exit " (:exit verification) ")"))
           (when-not (str/blank? output) (str ": " output))
           ". Submit a new snapshot-guarded request only after that condition changes."))
    (let [failed (first (remove :ok (:checks verification)))
          output (some-> (:output failed)
                         (str/replace #"\s+" " ")
                         (bounded-text 240))]
      (str "Fix the failed " (:profile verification) " verification check"
           (when-let [command (:command failed)] (str " `" command "`"))
           (when (some? (:exit failed)) (str " (exit " (:exit failed) ")"))
           (when-not (str/blank? output) (str ": " output))
           ". The transaction was rolled back; retry the same request once."))))

(defn- public-extraction-data
  [value]
  (cond
    (map? value)
    (into {}
          (map (fn [[key child]]
                 [(if (keyword? key)
                    (keyword (str/replace (name key) "-" "_"))
                    key)
                  (public-extraction-data child)]))
          value)

    (vector? value)
    (mapv public-extraction-data value)

    (sequential? value)
    (mapv public-extraction-data value)

    (keyword? value)
    (name value)

    :else value))

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
                 (when (and (= :overlapping-intents error-type)
                            (seq (:change-ids result)))
                   (str "Make changes "
                        (str/join " and " (map name (:change-ids result)))
                        " disjoint, then call apply_clojure_changes once."))
                 (when (and (#{:verification-failed
                               :verification-unverified} error-type)
                            (map? (:verification result)))
                   (verification-remedy (:verification result)))
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
      (contains? result :verification)
      (assoc :verification (compact-verification (:verification result)))
      (contains? result :cause-error) (assoc :cause_error (:cause-error result))
      (contains? result :mutation-attempted)
      (assoc :mutation_attempted (:mutation-attempted result))
      (contains? result :write-authority)
      (assoc :write_authority (:write-authority result))
      (contains? result :genuine-unknowns)
      (assoc :genuine_unknowns
             (public-extraction-data (:genuine-unknowns result)))
      (contains? result :completed-plan)
      (assoc :completed_plan
             (public-extraction-data (:completed-plan result)))
      (contains? result :next-call)
      (assoc :next_call (public-extraction-data (:next-call result)))
      (contains? result :next-action)
      (assoc :next_action (:next-action result))
      (contains? result :change-ids)
      (assoc :change_ids (mapv #(if (keyword? %) (name %) (str %))
                               (:change-ids result)))
      (contains? result :intent-indexes)
      (assoc :change_indexes (:intent-indexes result))
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
        receipt (:receipt-file result)
        cold (get-in result [:verification :cold-verification])
        verification-complete? (not= :running (:status cold))]
    (cond->
      {:ok true
       :operation "apply_clojure_changes"
       :committed true
       :changes (or (:change-count result) (:intent-count result))
       :edits (:match-count result)
       :files (:changed-file-count result)
       :verification_complete verification-complete?
       :read_back_hashes hashes
       :undo_receipt receipt
       :receipt_hash (:receipt-hash result)
       :next_action (if verification-complete?
                      "none"
                      "inspect_verification_job")}
      (:caller-proof result)
      (assoc :caller_proof
             (-> (:caller-proof result)
                 (update :level name)
                 (set/rename-keys
                   {:scan-complete :scan_complete
                    :semantic-provider-used :semantic_provider_used
                    :zero-callers-authoritative :zero_callers_authoritative})))
      (:format result) (assoc :format (:format result))
      (:verification result) (assoc :verification (:verification result))
      (and cold (not verification-complete?))
      (assoc :next_call (:next_call cold)))))

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
