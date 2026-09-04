(ns clj-surgeon.intent-transaction
  (:require
   [clj-surgeon.binding-rename :as binding-rename]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mcp-write-refusal :as write-refusal]
   [clj-surgeon.operation-algebra :as operation-algebra]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]
   [rewrite-clj.zip :as z])
  (:import
   (java.nio.file CopyOption Files OpenOption StandardCopyOption
                  StandardOpenOption)
   (java.util UUID)))

(def transaction-version 1)
(def receipt-version 1)

(def ^:private supported-extensions [".clj" ".cljs" ".cljc" ".edn"])
(def ^:private spec-keys #{:intents :changes :expect :create-files})
(def ^:private create-file-keys #{:file :content :directories :workspace-root})
(def ^:private intent-keys #{:files :from :to :expect-count})
(def ^:private expectation-keys
  #{:intent-count :edit-count :changed-file-count})
(def ^:private change-keys #{:id :in :forms :owner :find :inside :do :expect})
(def ^:private change-expectation-keys #{:matches :each-form :each-file})
(def ^:private change-aggregate-expectation-keys #{:changes :edits :files})

(defn- refuse!
  [error-type message & [data]]
  (throw (ex-info message (merge {:error-type error-type} data))))

(defn- lossless-node-fingerprint
  "Return a syntax fingerprint that ignores whitespace but retains comments,
   metadata, reader macros, token spelling, and tree position."
  [form-node]
  (when-not (node/whitespace? form-node)
    [(node/tag form-node)
     (if (node/inner? form-node)
       (vec (keep lossless-node-fingerprint (node/children form-node)))
       (node/string form-node))]))

(defn- parse-one-form
  [source label]
  (when-not (string? source)
    (refuse! :invalid-intent-form
             (str label " must be a source string")
             {:field label :actual source}))
  (try
    (let [root (parser/parse-string-all source)
          forms (->> (node/children root)
                     (remove node/whitespace?)
                     vec)]
      (when-not (and (= 1 (count forms))
                     (not (node/comment? (first forms))))
        (refuse! :invalid-intent-form
                 (str label
                      " must contain exactly one complete form with no detached comments")
                 {:field label :form-count (count forms)}))
      (let [form-node (first forms)]
        {:node form-node
         :source (node/string form-node)
         :fingerprint (lossless-node-fingerprint form-node)}))
    (catch clojure.lang.ExceptionInfo e
      (if (:error-type (ex-data e))
        (throw e)
        (refuse! :invalid-intent-form
                 (str "Invalid " label ": " (.getMessage e))
                 {:field label})))
    (catch Exception e
      (refuse! :invalid-intent-form
               (str "Invalid " label ": " (.getMessage e))
               {:field label}))))

(defn- validate-complete-source!
  [file source error-type]
  (when-not (string? source)
    (refuse! error-type
             (str "Source is missing for " file)
             {:file file}))
  (try
    (parser/parse-string-all source)
    source
    (catch Exception e
      (refuse! error-type
               (str "Invalid source in " file ": " (.getMessage e))
               {:file file}))))

(defn- zipper-locations
  [zloc]
  (take-while (complement z/end?) (iterate z/next zloc)))

(defn- node-size
  [form-node]
  (if (node/whitespace-or-comment? form-node)
    0
    (inc (if (node/inner? form-node)
           (reduce + 0 (map node-size (node/children form-node)))
           0))))

(defn- sibling-index
  [zloc]
  (loop [current zloc
         index 0]
    (if-let [left (z/left current)]
      (recur left (inc index))
      index)))

(defn- location-path
  [zloc]
  (loop [current zloc
         path ()]
    (let [path (conj path (sibling-index current))]
      (if-let [parent (z/up current)]
        (recur parent path)
        (vec path)))))

(defn- position<=?
  [left-line left-character right-line right-character]
  (not (pos? (compare [left-line left-character]
                      [right-line right-character]))))

(defn- contains-position?
  [form-node line character]
  (let [{:keys [row col end-row end-col]} (meta form-node)]
    (and row col end-row end-col
         (position<=? row col line character)
         (position<=? line character end-row end-col))))

(defn addressed-form-at
  "Return the smallest complete collection form containing a 1-indexed source
   position, together with its stable structural address."
  [source {:keys [line character]}]
  (when (and (string? source) (pos-int? line) (pos-int? character))
    (let [root (z/of-string source {:track-position? true})]
      (some->
        (->> (zipper-locations root)
             (map-indexed vector)
             (keep (fn [[preorder candidate]]
                     (let [candidate-node (z/node candidate)]
                       (when (and (node/inner? candidate-node)
                                  (not (node/whitespace-or-comment? candidate-node))
                                  (contains-position? candidate-node line character))
                         (let [{:keys [row end-row]} (meta candidate-node)
                               size (node-size candidate-node)]
                           {:address {:preorder preorder}
                            :path (location-path candidate)
                            :end-preorder (+ preorder size -1)
                            :line row
                            :end-line end-row
                            :before (z/string candidate)
                            :node-size size})))))
             (sort-by :node-size)
             first)
        (dissoc :node-size)))))

(defn- supported-file?
  [file]
  (and (string? file)
       (some #(str/ends-with? file %) supported-extensions)))

(defn- normalized-path
  [file]
  (str (.normalize (java.nio.file.Paths/get file (make-array String 0)))))

(defn- validate-files!
  [files intent-index]
  (when-not (and (vector? files) (seq files))
    (refuse! :invalid-files
             "Intent :files must be a non-empty vector"
             {:intent-index intent-index :files files}))
  (doseq [file files]
    (when-not (supported-file? file)
      (refuse! :unsupported-file
               (str "Unsupported Clojure or EDN source file: " (pr-str file))
               {:intent-index intent-index :file file})))
  (let [normalized (mapv normalized-path files)]
    (when-not (= (count normalized) (count (distinct normalized)))
      (refuse! :duplicate-file
               "Intent contains duplicate or aliased file paths"
               {:intent-index intent-index :files files})))
  files)

(defn- validate-expect-count!
  [expected intent-index]
  (when-not (and (integer? expected) (pos? expected))
    (refuse! :invalid-expect-count
             "Intent :expect-count must be a positive integer"
             {:intent-index intent-index :expected-count expected}))
  expected)

(defn- validate-intent!
  [intent intent-index]
  (when-not (map? intent)
    (refuse! :invalid-intents
             "Every intent must be a map"
             {:intent-index intent-index :actual intent}))
  (let [unknown (vec (sort (remove intent-keys (keys intent))))]
    (when (seq unknown)
      (refuse! :unknown-intent-arguments
               (str "Unknown intent arguments: " (str/join ", " unknown))
               {:intent-index intent-index :unknown unknown})))
  (let [files (validate-files! (:files intent) intent-index)
        from (parse-one-form (:from intent) ":from")
        to (parse-one-form (:to intent) ":to")
        expected-count (validate-expect-count! (:expect-count intent)
                                               intent-index)]
    (when (= (:fingerprint from) (:fingerprint to))
      (refuse! :no-op-intent
               "Intent :from and :to are losslessly equal"
               {:intent-index intent-index}))
    {:intent-index intent-index
     :files files
     :from from
     :to to
     :expect-count expected-count}))

(defn- positive-integer?
  [value]
  (and (integer? value) (pos? value)))

(defn- validate-change-forms!
  [forms change-index]
  (when (some? forms)
    (when-not (and (vector? forms)
                   (seq forms)
                   (every? #(or (symbol? %)
                                (and (map? %)
                                     (= #{:kind :name :dispatch} (set (keys %)))
                                     (= :defmethod (:kind %))
                                     (symbol? (:name %))
                                     (string? (:dispatch %))))
                           forms)
                   (= (count forms) (count (distinct forms))))
      (refuse! :invalid-change-forms
               "Change :forms must contain distinct symbols or exact defmethod owners"
               {:change-index change-index :forms forms})))
  (doseq [form-owner forms
          :when (map? form-owner)]
    (parse-one-form (:dispatch form-owner) ":forms dispatch"))
  forms)

(def ^:private unreadable-dispatch
  "Identity sentinel for an arm whose dispatch value cannot be read.

   It is never equal to a caller's parsed dispatch, so one unreadable arm can
   never match a selector — and, unlike a thrown exception, can never make every
   other arm in that file unaddressable."
  (Object.))

;; @spec MCP-OP-DISPATCH-005
(defn- defmethod-dispatch
  [record]
  (when (= 'defmethod (:type record))
    (try
      (some-> (:source record)
              z/of-string
              outline/defmethod-dispatch-location
              z/sexpr)
      (catch Exception _ unreadable-dispatch))))

(defn- owner-identity
  [owner-record]
  (or (:selector owner-record) (:name owner-record)))

(defn- validate-change-expectation!
  [expectation forms change-index change-id]
  (when-not (map? expectation)
    (refuse! :invalid-change-expectation
             "Change :expect must be a map"
             {:change-index change-index :change-id change-id}))
  (let [unknown (vec (sort (remove change-expectation-keys
                                   (keys expectation))))]
    (when (seq unknown)
      (refuse! :unknown-change-expectation-arguments
               (str "Unknown change expectation fields: "
                    (str/join ", " unknown))
               {:change-index change-index
                :change-id change-id
                :unknown unknown})))
  (when-not (positive-integer? (:matches expectation))
    (refuse! :invalid-change-expectation
             "Change :expect :matches must be a positive integer"
             {:change-index change-index
              :change-id change-id
              :actual (:matches expectation)}))
  (doseq [field [:each-form :each-file]
          :let [value (get expectation field)]
          :when (some? value)]
    (when-not (positive-integer? value)
      (refuse! :invalid-change-expectation
               (str "Change :expect " field " must be a positive integer")
               {:change-index change-index
                :change-id change-id
                :field field
                :actual value})))
  (when (and (contains? expectation :each-form) (nil? forms))
    (refuse! :invalid-change-expectation
             "Change :expect :each-form requires explicit :forms"
             {:change-index change-index :change-id change-id}))
  expectation)

(defn- validate-change-operator!
  [operator change-index change-id]
  (let [kind (first operator)
        value (second operator)]
    (cond
      (and (vector? operator) (= 2 (count operator)) (= :replace kind))
      {:kind :replace
       :form (parse-one-form value ":do replacement")}

      (and (vector? operator) (= [:delete true] operator))
      {:kind :delete}

      (and (vector? operator)
           (= 2 (count operator))
           (#{:insert-left :insert-right} kind)
           (vector? value)
           (seq value))
      {:kind kind
       :forms (mapv #(parse-one-form % ":do insertion") value)}

      (and (vector? operator)
           (= 2 (count operator))
           (= :rename-binding kind)
           (map? value))
      (let [allowed #{:from :to :preserve-external-key}
            unknown (vec (sort (remove allowed (keys value))))
            from (:from value)
            to (:to value)]
        (when (seq unknown)
          (refuse! :invalid-binding-rename
                   "Binding rename contains unknown fields"
                   {:change-index change-index :change-id change-id
                    :unknown unknown}))
        (when-not (and (= allowed (set (keys value)))
                       (symbol? from) (nil? (namespace from))
                       (symbol? to) (nil? (namespace to))
                       (not= from to)
                       (= true (:preserve-external-key value)))
          (refuse! :invalid-binding-rename
                   "Binding rename requires distinct unqualified :from and :to symbols and :preserve-external-key true"
                   {:change-index change-index :change-id change-id
                    :rename value}))
        {:kind :rename-binding
         :rename value})

      (and (vector? operator)
           (= 2 (count operator))
           (= :assoc-entry kind)
           (map? value))
      (let [allowed #{:key :value}
            unknown (vec (sort (remove allowed (keys value))))]
        (when (or (seq unknown) (not= allowed (set (keys value))))
          (refuse! :invalid-assoc-entry
                   "Map entry insertion requires exactly :key and :value"
                   {:change-index change-index :change-id change-id
                    :unknown unknown}))
        {:kind :assoc-entry
         :key (parse-one-form (:key value) ":do :assoc-entry :key")
         :value (parse-one-form (:value value) ":do :assoc-entry :value")})

      :else
      (refuse! :unsupported-change-operator
               "Structural changes support replacement, deletion, sibling insertion, map entry insertion, or binding rename"
               {:change-index change-index
                :change-id change-id
                :operator operator
                :supported [[:replace "SOURCE"]
                            [:delete true]
                            [:insert-left ["SOURCE" "..."]]
                            [:insert-right ["SOURCE" "..."]]
                            [:rename-binding
                             {:from 'old
                              :to 'new
                              :preserve-external-key true}]
                            [:assoc-entry {:key ":field" :value "VALUE"}]]}))))

(defn- validate-change!
  [change change-index]
  (when-not (map? change)
    (refuse! :invalid-changes
             "Every change must be a map"
             {:change-index change-index :actual change}))
  (let [unknown (vec (sort (remove change-keys (keys change))))]
    (when (seq unknown)
      (refuse! :unknown-change-arguments
               (str "Unknown change arguments: " (str/join ", " unknown))
               {:change-index change-index :unknown unknown})))
  (let [change-id (:id change)]
    (when (and (some? change-id) (not (keyword? change-id)))
      (refuse! :invalid-change-id
               "Change :id must be a keyword"
               {:change-index change-index :actual change-id}))
    (let [files (validate-files! (:in change) change-index)
          forms (validate-change-forms! (:forms change) change-index)
          owner (:owner change)]
      (when (and forms owner)
        (refuse! :ambiguous-change-owner
                 "Change must use either :forms or :owner, not both"
                 {:change-index change-index :change-id change-id}))
      (when owner
        (when-not (and (map? owner)
                       (#{#{:kind} #{:kind :name}} (set (keys owner)))
                       (= :namespace (:kind owner))
                       (or (not (contains? owner :name))
                           (symbol? (:name owner))))
          (refuse! :invalid-change-owner
                   "Change :owner must be {:kind :namespace} with an optional symbolic :name"
                   {:change-index change-index
                    :change-id change-id
                    :owner owner})))
      (let [operator (validate-change-operator! (:do change) change-index change-id)
            binding-rename? (= :rename-binding (:kind operator))
            delete? (= :delete (:kind operator))
            from (when-not (or binding-rename?
                               delete?
                               (and (#{:insert-left :insert-right}
                                     (:kind operator))
                                    (nil? (:find change))))
                   (parse-one-form (:find change) ":find"))
            inside (when-let [inside-source (:inside change)]
                     (when-not (= :assoc-entry (:kind operator))
                       (refuse! :invalid-ancestor-selector
                                ":inside is only valid with :assoc-entry"
                                {:change-index change-index :change-id change-id}))
                     (parse-one-form inside-source ":inside"))
            expectation (validate-change-expectation!
                          (:expect change) forms change-index change-id)]
        (when (and (#{:insert-left :insert-right} (:kind operator))
                   (nil? (:find change))
                   (or owner (not= 1 (count forms))))
          (refuse! :invalid-top-level-insertion-owner
                   "Top-level insertion without :find requires exactly one named :forms owner"
                   {:change-index change-index
                    :change-id change-id
                    :forms forms
                    :owner owner}))
        (when (and binding-rename? (or (nil? forms) owner))
          (refuse! :invalid-binding-rename-owner
                   "Binding rename requires exact named :forms"
                   {:change-index change-index :change-id change-id}))
        (when (and binding-rename? (some? (:find change)))
          (refuse! :invalid-binding-rename
                   "Binding rename does not accept :find"
                   {:change-index change-index :change-id change-id}))
        (when (and delete? (or (nil? forms) owner))
          (refuse! :invalid-delete-owner
                   "Whole-owner deletion requires exact named :forms"
                   {:change-index change-index :change-id change-id}))
        (when (and delete? (some? (:find change)))
          (refuse! :invalid-delete-find
                   "Whole-owner deletion does not accept :find"
                   {:change-index change-index :change-id change-id}))
        (when (and (= :replace (:kind operator))
                   (= (:fingerprint from)
                      (get-in operator [:form :fingerprint])))
          (refuse! :no-op-intent
                   "Change :find and replacement are losslessly equal"
                   {:change-index change-index :change-id change-id}))
        (cond-> {:kind (if binding-rename?
                         :binding-rename
                         :scoped-change)
                 :id change-id
                 :intent-index change-index
                 :files files
                 :forms forms
                 :operator (:kind operator)
                 :expect-count (:matches expectation)
                 :each-form (:each-form expectation)
                 :each-file (:each-file expectation)}
          from (assoc :from from)
          inside (assoc :inside inside)
          owner (assoc :owner owner)
          binding-rename? (assoc :rename (:rename operator))
          (= :assoc-entry (:kind operator))
          (assoc :assoc-key (:key operator)
                 :assoc-value (:value operator))
          (= :replace (:kind operator)) (assoc :to (:form operator))
          (#{:insert-left :insert-right} (:kind operator))
          (assoc :insert-side (:kind operator)
                 :insert-sources (mapv :source (:forms operator)))
          (and (#{:insert-left :insert-right} (:kind operator))
               (nil? (:find change)))
          (assoc :target-owner true))))))

(defn- validate-changes!
  [changes]
  (when-not (and (vector? changes) (seq changes))
    (refuse! :invalid-changes "Spec :changes must be a non-empty vector"))
  (let [validated (mapv (fn [change change-index]
                          (try
                            (validate-change! change change-index)
                            (catch clojure.lang.ExceptionInfo error
                              (throw
                                (ex-info (.getMessage error)
                                         (merge {:change-index change-index
                                                 :change-id (:id change)}
                                                (ex-data error))
                                         error)))))
                        changes
                        (range))
        ids (keep :id validated)
        duplicate-id (first (for [[id occurrences] (frequencies ids)
                                  :when (> occurrences 1)]
                              id))]
    (when duplicate-id
      (refuse! :duplicate-change-id
               (str "Duplicate change id: " duplicate-id)
               {:change-id duplicate-id}))
    validated))

(defn- validate-aggregate-expectation!
  [expectation]
  (when-not (map? expectation)
    (refuse! :invalid-transaction-expectation
             "Spec :expect must be a map"
             {:expected-fields expectation-keys}))
  (let [unknown (vec (sort (remove expectation-keys (keys expectation))))
        missing (vec (sort (remove #(contains? expectation %) expectation-keys)))]
    (when (seq unknown)
      (refuse! :invalid-transaction-expectation
               (str "Unknown transaction expectation fields: "
                    (str/join ", " unknown))
               {:unknown unknown}))
    (when (seq missing)
      (refuse! :invalid-transaction-expectation
               (str "Missing transaction expectation fields: "
                    (str/join ", " missing))
               {:missing missing})))
  (doseq [[field value] expectation]
    (when-not (and (integer? value) (pos? value))
      (refuse! :invalid-transaction-expectation
               (str field " must be a positive integer")
               {:field field :actual value})))
  expectation)

(defn- validate-change-aggregate-expectation!
  [expectation]
  (when-not (map? expectation)
    (refuse! :invalid-transaction-expectation
             "Spec :expect must be a map"
             {:expected-fields change-aggregate-expectation-keys}))
  (let [unknown (vec (sort (remove change-aggregate-expectation-keys
                                   (keys expectation))))
        missing (vec (sort (remove #(contains? expectation %)
                                   change-aggregate-expectation-keys)))]
    (when (seq unknown)
      (refuse! :invalid-transaction-expectation
               (str "Unknown transaction expectation fields: "
                    (str/join ", " unknown))
               {:unknown unknown}))
    (when (seq missing)
      (refuse! :invalid-transaction-expectation
               (str "Missing transaction expectation fields: "
                    (str/join ", " missing))
               {:missing missing})))
  (doseq [[field value] expectation]
    (when-not (positive-integer? value)
      (refuse! :invalid-transaction-expectation
               (str field " must be a positive integer")
               {:field field :actual value})))
  {:intent-count (:changes expectation)
   :edit-count (:edits expectation)
   :changed-file-count (:files expectation)})

(defn- validate-create-files!
  ;; @spec MCP-OP-EDIT-031
  "Validate the exact create_files decision. Pure: performs no filesystem I/O.

   Every guard here refuses the whole transaction and names the offending
   path, so no creation is ever written on a partially valid request."
  [create-files]
  (when-not (or (nil? create-files) (vector? create-files))
    (refuse! :invalid-create-files
             "Spec :create-files must be a vector"))
  (let [validated
        (mapv
          (fn [creation]
            (when-not (map? creation)
              (refuse! :invalid-create-files
                       "Each :create-files entry must be a map"))
            (let [unknown (vec (sort (remove create-file-keys (keys creation))))]
              (when (seq unknown)
                (refuse! :invalid-create-files
                         (str "Unknown :create-files arguments: "
                              (str/join ", " unknown))
                         {:unknown unknown})))
            (let [{:keys [file content directories workspace-root]} creation]
              (when-not (and (string? file) (not (str/blank? file)))
                (refuse! :invalid-create-files
                         "Each :create-files entry needs a non-blank :file"))
              (when-not (supported-file? file)
                (refuse! :unsupported-file
                         (str "Unsupported Clojure or EDN target file: "
                              (pr-str file))
                         {:file file}))
              (when-not (string? content)
                (refuse! :invalid-created-source
                         (str "Created content must be text for " file)
                         {:file file}))
              ;; The created content passes through the same complete-source
              ;; parser that every edited future file must satisfy.
              (validate-complete-source! file content :invalid-created-source)
              (when-not (or (nil? directories) (vector? directories))
                (refuse! :invalid-create-files
                         (str "Created :directories must be a vector for " file)
                         {:file file}))
              (doseq [directory (or directories [])]
                (when-not (and (string? directory) (not (str/blank? directory)))
                  (refuse! :invalid-create-files
                           (str "Created :directories must be paths for " file)
                           {:file file})))
              (when-not (or (nil? workspace-root)
                            (and (string? workspace-root)
                                 (not (str/blank? workspace-root))))
                (refuse! :invalid-create-files
                         (str "Created :workspace-root must be a path for " file)
                         {:file file}))
              (cond->
                {:file file
                 :content content
                 :directories (vec (or directories []))}
                workspace-root
                (assoc :workspace-root workspace-root))))
          (or create-files []))
        normalized (mapv (comp normalized-path :file) validated)]
    (when-not (= (count normalized) (count (distinct normalized)))
      (refuse! :duplicate-file
               "Transaction contains duplicate or aliased created file paths"
               {:files (mapv :file validated)}))
    validated))

(defn- validate-spec!
  [spec]
  (when-not (map? spec)
    (refuse! :invalid-transaction-spec "Transaction spec must be a map"))
  (let [unknown (vec (sort (remove spec-keys (keys spec))))]
    (when (seq unknown)
      (refuse! :unknown-transaction-arguments
               (str "Unknown transaction arguments: "
                    (str/join ", " unknown))
               {:unknown unknown})))
  (let [has-intents? (contains? spec :intents)
        has-changes? (contains? spec :changes)
        create-files (validate-create-files! (:create-files spec))]
    (when (and has-intents? has-changes?)
      (refuse! :mixed-transaction-modes
               "Use either :intents or :changes, not both"))
    (cond
      ;; @spec MCP-OP-EDIT-031
      ;; A create-only transaction is legal and carries no edit expectation.
      (and (seq create-files)
           (empty? (:changes spec))
           (empty? (:intents spec)))
      {:mode :changes
       :intents []
       :expect {:intent-count 0 :edit-count 0 :changed-file-count 0}
       :create-files create-files}

      has-changes?
      {:mode :changes
       :intents (validate-changes! (:changes spec))
       :expect (validate-change-aggregate-expectation! (:expect spec))
       :create-files create-files}

      :else
      (do
        (when-not (and (vector? (:intents spec)) (seq (:intents spec)))
          (refuse! :invalid-intents
                   "Spec :intents must be a non-empty vector"))
        {:mode :intents
         :intents (mapv validate-intent! (:intents spec) (range))
         :expect (validate-aggregate-expectation! (:expect spec))
         :create-files create-files}))))

(defn- ordered-scoped-files
  [intents]
  (reduce (fn [result file]
            (if (some #{file} result) result (conj result file)))
          []
          (mapcat :files intents)))

(defn- scoped-owner-records!
  [source file {:keys [forms owner intent-index id]}]
  (let [records (outline/top-level-form-records file source)]
    (cond
      forms
      (let [by-name (group-by :name records)]
        (mapv
          (fn [form-owner]
            (let [form-name (if (map? form-owner)
                              (:name form-owner)
                              form-owner)
                  dispatch (when (map? form-owner)
                             (-> (parse-one-form (:dispatch form-owner)
                                                 ":forms dispatch")
                                 :node
                                 node/sexpr))
                  matches (cond->> (get by-name form-name [])
                            (map? form-owner)
                            (filter #(and (= 'defmethod (:type %))
                                          (= dispatch (defmethod-dispatch %)))))
                  matches (mapv #(cond-> %
                                   (map? form-owner)
                                   (assoc :selector form-owner))
                                matches)]
              (when-not (= 1 (count matches))
                (refuse! :change-owner-mismatch
                         (str "Change owner " form-owner " in " file
                              " must resolve exactly once, found "
                              (count matches))
                         {:change-index intent-index
                          :change-id id
                          :file file
                          :owner form-owner
                          :actual-count (count matches)
                          :candidates (mapv #(select-keys %
                                                          [:type :name :platforms
                                                           :line :end-line])
                                            matches)}))
              (first matches)))
          forms))

      owner
      (let [owner-name (:name owner)
            matches (->> records
                         (filter #(= 'ns (:type %)))
                         (keep (fn [record]
                                 (let [namespace-name
                                       (some-> (:source record)
                                               z/of-string
                                               z/down
                                               z/right
                                               z/sexpr)]
                                   (when (or (nil? owner-name)
                                             (= owner-name namespace-name))
                                     (assoc record :name namespace-name)))))
                         vec)]
        (when-not (= 1 (count matches))
          (refuse! :change-owner-mismatch
                   (str (if owner-name
                          (str "Namespace owner " owner-name)
                          "Unique namespace owner")
                        " in " file
                        " must resolve exactly once, found " (count matches))
                   {:change-index intent-index
                    :change-id id
                    :file file
                    :owner owner
                    :actual-count (count matches)}))
        matches)

      :else nil)))

(defn- containing-owner
  [owner-records candidate-node]
  (when owner-records
    (let [{:keys [row end-row]} (meta candidate-node)]
      (some (fn [{:keys [line end-line] :as owner}]
              (when (and row end-row line end-line
                         (<= line row)
                         (<= end-row end-line))
                owner))
            owner-records))))

(defn- matching-edits
  [source file {:keys [intent-index from to forms owner operator insert-side
                       insert-sources assoc-key assoc-value inside id target-owner]
                :as intent}]
  (let [root (z/of-string source {:track-position? true})
        all-owner-records (when target-owner
                            (outline/top-level-form-records file source))
        owner-records (scoped-owner-records! source file intent)
        scoped? (or forms owner)
        semantic-find (when assoc-key (node/sexpr (:node from)))
        semantic-inside (when inside (node/sexpr (:node inside)))]
    (if (or (= :delete operator) target-owner)
      (mapv (fn [owner-record]
              (let [addressed (addressed-form-at
                                source {:line (:line owner-record) :character 1})]
                (when-not addressed
                  (refuse! :delete-owner-not-addressable
                           "The exact named owner has no stable structural address"
                           {:file file :owner (:name owner-record)}))
                (cond-> (assoc addressed
                               :intent-index intent-index
                               :change-id id
                               :owner (owner-identity owner-record)
                               :file file)
                  (= :delete operator) (assoc :delete true)
                  target-owner
                  (assoc :insert-side insert-side
                         :insert-sources insert-sources
                         :target-owner true
                         :owner-context
                         (let [owner-index (.indexOf all-owner-records owner-record)
                               addressed-owner
                               (fn [record]
                                 (when record
                                   (assoc (addressed-form-at
                                            source
                                            {:line (:line record) :character 1})
                                          :comment-start
                                          (:comment-start record))))]
                           {:current-comment-start (:comment-start owner-record)
                            :previous
                            (addressed-owner
                              (get all-owner-records (dec owner-index)))
                            :next
                            (addressed-owner
                              (get all-owner-records (inc owner-index)))})))))
            owner-records)
      (->> (zipper-locations root)
           (map-indexed vector)
           (keep (fn [[preorder candidate]]
                   (let [candidate-node (z/node candidate)
                         containing (containing-owner owner-records candidate-node)]
                     (when (and (if assoc-key
                                  (and (= :map (node/tag candidate-node))
                                       (= semantic-find
                                          (node/sexpr candidate-node)))
                                  (= (:fingerprint from)
                                     (lossless-node-fingerprint candidate-node)))
                                (or (not scoped?) containing)
                                (or (nil? semantic-inside)
                                    (some #(= semantic-inside
                                              (node/sexpr (z/node %)))
                                          (rest (take-while some?
                                                            (iterate z/up candidate))))))
                       (let [{:keys [row end-row]} (meta candidate-node)
                             size (node-size candidate-node)
                             candidate-source (z/string candidate)
                             candidate-value (when assoc-key
                                               (node/sexpr candidate-node))
                             key-value (when assoc-key
                                         (node/sexpr (:node assoc-key)))
                             closing (when assoc-key
                                       (str/last-index-of candidate-source "}"))]
                         (when (and assoc-key (contains? candidate-value key-value))
                           (refuse! :map-key-already-present
                                    "Matched map already contains the requested key"
                                    {:file file :owner (:name containing)
                                     :key (:source assoc-key)}))
                         (cond->
                           {:intent-index intent-index
                            :change-id id
                            :owner (owner-identity containing)
                            :file file
                            :address {:preorder preorder}
                            :path (location-path candidate)
                            :end-preorder (+ preorder size -1)
                            :line row
                            :end-line end-row
                            :before candidate-source}
                           to (assoc :after (:source to))
                           assoc-key
                           (assoc :after
                                  (str (subs candidate-source 0 closing)
                                       " " (:source assoc-key)
                                       " " (:source assoc-value)
                                       (subs candidate-source closing)))
                           insert-side (assoc :insert-side insert-side
                                              :insert-sources insert-sources)))))))
           vec))))

(def ^:dynamic *binding-analyzer*
  "Test seam for binding analysis of an exact source snapshot."
  binding-rename/analyze-source)

(defn- binding-target-edit
  [source file intent-index change-id {:keys [row col before after owner]}]
  (let [root (z/of-string source {:track-position? true})
        matches
        (->> (zipper-locations root)
             (map-indexed vector)
             (filter
               (fn [[_ location]]
                 (let [form-node (z/node location)
                       node-meta (meta form-node)]
                   (and (= row (:row node-meta))
                        (= col (:col node-meta))
                        (= before (z/string location))))))
             vec)]
    (when-not (= 1 (count matches))
      (refuse! :binding-source-drift
               "Binding rename target must resolve to one exact source node"
               {:file file :owner owner :row row :col col
                :actual-count (count matches)}))
    (let [[preorder location] (first matches)
          form-node (z/node location)
          {:keys [end-row]} (meta form-node)
          size (node-size form-node)]
      {:intent-index intent-index
       :change-id change-id
       :owner owner
       :file file
       :address {:preorder preorder}
       :path (location-path location)
       :end-preorder (+ preorder size -1)
       :line row
       :end-line end-row
       :before before
       :after after})))

(defn- compile-binding-rename-edits
  [sources {:keys [files forms intent-index id rename expect-count each-form]
            :as intent}]
  (when (and each-form (not= 1 each-form))
    (refuse! :change-distribution-mismatch
             "Binding rename requires :each-form 1"
             {:change-index intent-index :change-id id
              :distribution :each-form :expected each-form}))
  (let [compiled
        (mapv
          (fn [file]
            (let [source (get sources file)
                  owners (scoped-owner-records! source file intent)
                  analysis (*binding-analyzer* file source)
                  result
                  (binding-rename/compile-targets
                    {:file file
                     :source source
                     :owners owners
                     :analysis analysis
                     :from (:from rename)
                     :to (:to rename)
                     :preserve-external-key
                     (:preserve-external-key rename)})]
              {:file file
               :occurrence-count (:occurrence-count result)
               :binding-count (:binding-count result)
               :per-form-counts (:per-form-counts result)
               :edits (mapv #(binding-target-edit source file intent-index id %)
                            (:targets result))}))
          files)
        occurrence-count (reduce + (map :occurrence-count compiled))
        per-file-counts (into {} (map (juxt :file :occurrence-count) compiled))
        per-form-counts (into {} (map (juxt :file :per-form-counts) compiled))]
    (when-not (= expect-count occurrence-count)
      (refuse! :expect-count-mismatch
               (str "Binding rename expected " expect-count
                    " occurrences, found " occurrence-count)
               {:intent-index intent-index
                :change-id id
                :expected-count expect-count
                :actual-count occurrence-count
                :per-file-counts per-file-counts
                :per-form-counts per-form-counts}))
    {:kind :binding-rename
     :id id
     :intent-index intent-index
     :files files
     :forms forms
     :operator :rename-binding
     :from (str (:from rename))
     :to (str (:to rename))
     :expected-count expect-count
     :match-count occurrence-count
     :per-file-counts per-file-counts
     :per-form-counts per-form-counts
     :binding-count (reduce + (map :binding-count compiled))
     :edits (vec (mapcat :edits compiled))}))

(defn- compile-intent-edits
  [sources intent write-refusal-context]
  (if (= :binding-rename (:kind intent))
    (compile-binding-rename-edits sources intent)
    (let [by-file (mapv (fn [file]
                          [file (matching-edits (get sources file) file intent)])
                        (:files intent))
          edits (vec (mapcat second by-file))
          actual-count (count edits)
          per-file-counts (into {} (map (fn [[file matches]]
                                          [file (count matches)]))
                                by-file)
          per-form-counts
          (when (:forms intent)
            (into {}
                  (map (fn [[file matches]]
                         [file
                          (into {}
                                (map (fn [form-name]
                                       [form-name
                                        (count (filter #(= form-name (:owner %))
                                                       matches))])
                                     (:forms intent)))])
                       by-file)))]
      (when-not (= (:expect-count intent) actual-count)
        (refuse! :expect-count-mismatch
                 (str "Intent " (:intent-index intent) " expected "
                      (:expect-count intent) " matches, found " actual-count)
                 (cond->
                   {:intent-index (:intent-index intent)
                    :change-id (:id intent)
                    :expected-count (:expect-count intent)
                    :actual-count actual-count
                    :per-file-counts per-file-counts}
                   (and write-refusal-context per-form-counts)
                   (assoc :per-form-counts
                          (write-refusal/project-relative-counts
                            (:project-root write-refusal-context)
                            per-form-counts))

                   write-refusal-context
                   (assoc
                     :write-refusal-evidence
                     (write-refusal/generic-count-mismatch-evidence
                       {:operation (:operation write-refusal-context)
                        :project-root (:project-root write-refusal-context)
                        :change-index (:intent-index intent)
                        :change-id (:id intent)
                        :files (:files intent)
                        :scope (cond
                                 (:forms intent)
                                 {:kind :form :forms (:forms intent)}

                                 (:owner intent)
                                 {:kind :namespace
                                  :name (get-in intent [:owner :name])}

                                 :else {:kind :root})
                        :matcher (cond->
                                   {:from (get-in intent [:from :source])
                                    :operator (:operator intent)}
                                   (:inside intent)
                                   (assoc :inside
                                          (get-in intent [:inside :source])))
                        :expectation
                        (select-keys intent
                                     [:expect-count :each-file :each-form])
                        :expected-count (:expect-count intent)
                        :actual-count actual-count
                        :per-file-counts per-file-counts
                        :per-form-counts per-form-counts
                        :items edits
                        :snapshot-guards
                        (into {}
                              (map (fn [file]
                                     [file
                                      (structural-lens/source-hash
                                        (get sources file))]))
                              (:files intent))})))))
      (when (and (:each-file intent)
                 (some #(not= (:each-file intent) %)
                       (vals per-file-counts)))
        (refuse! :change-distribution-mismatch
                 "Change matches do not satisfy :each-file"
                 {:change-index (:intent-index intent)
                  :change-id (:id intent)
                  :distribution :each-file
                  :expected (:each-file intent)
                  :actual per-file-counts}))
      (when (and (:each-form intent)
                 (some #(not= (:each-form intent) %)
                       (mapcat vals (vals per-form-counts))))
        (refuse! :change-distribution-mismatch
                 "Change matches do not satisfy :each-form"
                 {:change-index (:intent-index intent)
                  :change-id (:id intent)
                  :distribution :each-form
                  :expected (:each-form intent)
                  :actual per-form-counts}))
      (cond->
        {:intent-index (:intent-index intent)
         :files (:files intent)
         :from (get-in intent [:from :source])
         :to (or (get-in intent [:to :source]) (:insert-sources intent))
         :operator (:operator intent)
         :expected-count (:expect-count intent)
         :match-count actual-count
         :per-file-counts per-file-counts
         :edits edits}
        (= :scoped-change (:kind intent))
        (assoc :kind :scoped-change
               :id (:id intent)
               :forms (:forms intent)
               :per-form-counts per-form-counts)

        (:owner intent)
        (assoc :owner (:owner intent))))))

(defn- overlap?
  [left right]
  (letfn [(owner-boundary-insertion-disjoint? [insertion interior]
            (let [insertion-path (:path insertion)
                  interior-path (:path interior)]
              (and (:insert-side insertion)
                   (= 2 (count insertion-path))
                   (= (:owner insertion) (:owner interior))
                   (< (:address-preorder insertion)
                      (:address-preorder interior))
                   (< (:end-preorder interior)
                      (:end-preorder insertion))
                   (<= (count insertion-path) (count interior-path))
                   (= insertion-path
                      (subvec interior-path 0 (count insertion-path))))))]
    (and (<= (:address-preorder right) (:end-preorder left))
         (not (or (owner-boundary-insertion-disjoint? left right)
                  (owner-boundary-insertion-disjoint? right left))))))

;; @spec MCP-OP-INSERT-001
;; @spec MCP-OP-INSERT-002
;; @spec MCP-OP-INSERT-006
(defn- assert-disjoint-edits!
  [file edits]
  (let [ordered (->> edits
                     (map #(assoc % :address-preorder
                                  (get-in % [:address :preorder])))
                     (sort-by :address-preorder)
                     vec)]
    (doseq [[left right] (partition 2 1 ordered)]
      (when (overlap? left right)
        (refuse! :overlapping-intents
                 (str "Changes overlap in " file)
                 {:file file
                  :intent-indexes [(:intent-index left)
                                   (:intent-index right)]
                  :change-ids [(:change-id left) (:change-id right)]
                  :edits [(dissoc left :address-preorder)
                          (dissoc right :address-preorder)]})))
    (mapv #(dissoc % :address-preorder) ordered)))

(defn- replacement-node
  [source]
  (:node (parse-one-form source ":to")))

(defn- move-right
  [zloc n]
  (nth (iterate z/right zloc) n nil))

(defn- location-at-path
  [source path]
  (when (and (vector? path) (seq path) (every? nat-int? path))
    (let [first-form (z/of-string source {:track-position? true})
          forms-root (z/up first-form)
          [root-index & child-indexes] path]
      (when (and forms-root (zero? root-index))
        (reduce (fn [parent index]
                  (some-> parent z/down (move-right index)))
                forms-root
                child-indexes)))))

(defn- line-offsets
  [source]
  (loop [offsets [0]
         index 0]
    (if-let [newline (str/index-of source "\n" index)]
      (recur (conj offsets (inc newline)) (inc newline))
      offsets)))

(defn- addressed-target
  [source {:keys [address path before]}]
  (let [root (z/of-string source {:track-position? true})
        target (if (some? path)
                 (location-at-path source path)
                 (nth (zipper-locations root) (:preorder address) nil))]
    (when-not target
      (refuse! :stale-path
               (str "Planned address no longer exists: " (:preorder address))))
    (when-not (= before (z/string target))
      (refuse! :stale-subform
               "Source at planned address does not match edit"))
    target))

(defn- node-offsets
  [source target]
  (let [{:keys [row col end-row end-col]} (meta (z/node target))
        offsets (line-offsets source)]
    {:start (+ (nth offsets (dec row)) (dec col))
     :end (+ (nth offsets (dec end-row)) (dec end-col))
     :row row}))

;; @spec MCP-OP-INSERT-003
;; @spec MCP-OP-INSERT-004
;; @spec MCP-OP-INSERT-005
;; @spec MCP-OP-INSERT-006
(defn- insertion-gap
  [source target side edit]
  ;; @spec MCP-OP-INSERT-007
  ;; @spec MCP-OP-INSERT-008
  ;; @spec MCP-OP-INSERT-009
  ;; @spec MCP-OP-INSERT-010
  (let [parent (z/up target)
        parent-tag (some-> parent z/tag)
        top-level? (= :forms parent-tag)]
    (when-not (#{:forms :list :vector :map :set} parent-tag)
      (refuse! :unsupported-insertion-parent
               "Sibling insertion requires a top-level form sequence, list, vector, map, or set parent"
               {:change-id (:change-id edit)
                :file (:file edit)
                :parent-tag parent-tag}))
    (let [{target-start :start target-end :end} (node-offsets source target)
          {parent-start :start parent-end :end} (node-offsets source parent)
          parent-source (z/string parent)
          opening-boundary (if top-level? parent-start
                             (+ parent-start
                                (if (str/starts-with? parent-source "#{") 2 1)))
          closing-boundary (if top-level? parent-end (dec parent-end))
          line-start (fn [line] (nth (line-offsets source) (dec line)))
          trailing-comment-end
          (fn [start limit]
            (let [comment (re-find #"^[ \t]*;[^\n\r]*"
                                   (subs source start limit))]
              (+ start (count (or comment "")))))
          accepted-gap
          (fn [gap]
            (when-not (re-matches #"[\s,]*" gap)
              (refuse! :ambiguous-insertion-gap
                       "The sibling gap contains comments or detached source"
                       {:change-id (:change-id edit)
                        :file (:file edit)
                        :target (:before edit)
                        :gap gap
                        :remedy "Replace a larger exact span that declares comment placement."}))
            gap)
          anchor-line-start
          (inc (or (when (pos? target-start)
                     (str/last-index-of source "\n" (dec target-start)))
                   -1))
          anchor-prefix (subs source anchor-line-start target-start)
          anchor-indentation
          (if (re-matches #"[ \t]*" anchor-prefix) anchor-prefix "")]
      (if (and top-level? (:target-owner edit))
        (let [{:keys [current-comment-start previous next]} (:owner-context edit)
              previous-target (when previous (addressed-target source previous))
              next-target (when next (addressed-target source next))
              previous-end (if previous-target
                             (:end (node-offsets source previous-target))
                             opening-boundary)
              next-start (if next-target
                           (:start (node-offsets source next-target))
                           closing-boundary)
              current-owned-start
              (if current-comment-start
                (line-start current-comment-start)
                target-start)
              next-owned-start
              (if-let [comment-start (:comment-start next)]
                (line-start comment-start)
                next-start)
              [offset gap]
              (if (= :insert-left side)
                (let [left-owned-end
                      (trailing-comment-end previous-end current-owned-start)]
                  [current-owned-start
                   (accepted-gap
                     (subs source left-owned-end current-owned-start))])
                (let [left-owned-end
                      (trailing-comment-end target-end next-owned-start)]
                  [left-owned-end
                   (accepted-gap
                     (subs source left-owned-end next-owned-start))]))]
          {:offset offset :separator (if (seq gap) gap " ")})
        (let [neighbor (if (= :insert-left side) (z/left target) (z/right target))
              neighbor-offsets (when neighbor (node-offsets source neighbor))
              opposite-neighbor
              (if (= :insert-left side) (z/right target) (z/left target))
              opposite-offsets
              (when opposite-neighbor (node-offsets source opposite-neighbor))
              [gap-start gap-end]
              (if (= :insert-left side)
                [(or (:end neighbor-offsets) opening-boundary) target-start]
                [target-end (or (:start neighbor-offsets) closing-boundary)])
              gap (accepted-gap (subs source gap-start gap-end))
              opposite-gap
              (if (= :insert-left side)
                (subs source target-end
                      (or (:start opposite-offsets) closing-boundary))
                (subs source
                      (or (:end opposite-offsets) opening-boundary)
                      target-start))]
          {:offset (if (= :insert-left side) target-start target-end)
           :separator
           (cond
             (seq gap) gap
             (str/includes? opposite-gap "\n") (str "\n" anchor-indentation)
             :else " ")})))))

(defn- deletion-offsets
  [source target]
  (when (= "ns" (some-> target z/down z/string))
    (refuse! :protected-namespace-form
             "A whole-site delete cannot remove the namespace form"))
  (let [{target-start :start target-end :end row :row}
        (node-offsets source target)
        offsets (line-offsets source)
        lines (str/split source #"\n" -1)
        line-start (nth offsets (dec row))
        line-oriented? (str/blank? (subs source line-start target-start))
        first-comment-line
        (when line-oriented?
          (loop [line-index (- row 2)
                 first-index nil]
            (if (and (>= line-index 0)
                     (re-matches #"\s*;+.*" (nth lines line-index)))
              (recur (dec line-index) line-index)
              first-index)))
        raw-start
        (if line-oriented?
          (nth offsets (or first-comment-line (dec row)))
          (loop [at target-start]
            (if (and (pos? at)
                     (contains? #{\space \tab}
                                (.charAt ^String source (dec at))))
              (recur (dec at))
              at)))
        delete-start
        (if (and (> raw-start 1)
                 (= \newline (.charAt ^String source (dec raw-start)))
                 (= \newline (.charAt ^String source (- raw-start 2))))
          (dec raw-start)
          raw-start)
        line-end (or (str/index-of source "\n" target-end) (count source))
        trailing-source (subs source target-end line-end)
        delete-end
        (if line-oriented?
          (let [content-end (if (re-matches #"\s*;+.*" trailing-source)
                              line-end
                              target-end)]
            (if (and (< content-end (count source))
                     (= \newline (.charAt ^String source content-end)))
              (inc content-end)
              content-end))
          target-end)]
    {:start delete-start :end delete-end}))

(defn- prepare-raw-addressed-edit
  [source {:keys [delete after insert-side insert-sources] :as edit}]
  (let [target (addressed-target source edit)]
    (if insert-side
      (let [{:keys [offset separator]}
            (insertion-gap source target insert-side edit)
            inserted (str/join separator insert-sources)
            insertion (if (= :insert-left insert-side)
                        (str inserted separator)
                        (str separator inserted))]
        (assoc edit
               :raw true
               :offset offset
               :before ""
               :after insertion))
      (let [{:keys [start end]}
            (if delete
              (deletion-offsets source target)
              (node-offsets source target))]
        (assoc edit
               :raw true
               :offset start
               :before (subs source start end)
               :after (if delete "" after))))))

(defn- apply-raw-edit
  [source {:keys [offset before after]}]
  (let [end (+ offset (count before))]
    (when-not (and (nat-int? offset)
                   (<= end (count source))
                   (= before (subs source offset end)))
      (refuse! :stale-raw-span
               "Source at the retained byte span does not match edit"))
    (str (subs source 0 offset) after (subs source end))))

(defn- replace-at-address
  [source {:keys [raw after] :as edit}]
  (if raw
    (apply-raw-edit source edit)
    (-> (addressed-target source edit)
        (z/replace (replacement-node after))
        z/root-string)))

(defn- apply-edits
  [source edits]
  (reduce replace-at-address
          source
          (sort-by #(if (:raw %)
                      [(:offset %)
                       (count (:before %))
                       (or (:source-offset %) 0)]
                      [(or (get-in % [:address :preorder]) 0) 0])
                   (fn [left right] (compare right left))
                   edits)))

(defn- prefixed-lines
  [prefix source]
  (->> (str/split source #"\n" -1)
       (map #(str prefix %))
       (str/join "\n")))

(defn- edit-diff
  [{:keys [line before after intent-index]}]
  (str "@@ intent " intent-index " line " line " @@\n"
       (prefixed-lines "-" before) "\n"
       (prefixed-lines "+" after) "\n"))

(defn- file-diff
  [file edits]
  (str "--- a/" file "\n"
       "+++ b/" file "\n"
       (apply str (map edit-diff edits))))

(defn- compile-file
  [file source edits]
  (let [ordered-edits (assert-disjoint-edits! file edits)
        effective-edits (if (some #(or (:delete %)
                                       (:insert-side %)
                                       (and (:address %) (not (:path %))))
                                  ordered-edits)
                          (mapv #(prepare-raw-addressed-edit source %)
                                ordered-edits)
                          ordered-edits)
        result-source (if (seq effective-edits)
                        (apply-edits source effective-edits)
                        source)]
    (validate-complete-source! file result-source :invalid-result-source)
    {:file file
     :match-count (count effective-edits)
     :source-hash (structural-lens/source-hash source)
     :result-hash (structural-lens/source-hash result-source)
     :edits effective-edits
     :diff (when (seq effective-edits) (file-diff file effective-edits))
     :result-source result-source}))

(defn- compile-transaction*
  [sources spec options]
  (let [{:keys [mode intents expect create-files]} (validate-spec! spec)
        files (ordered-scoped-files intents)
        _ (doseq [file files]
            (validate-complete-source! file (get sources file) :invalid-source))
        compiled-intents (mapv #(compile-intent-edits
                                  sources % (:write-refusal-context options))
                               intents)
        edits-by-file (group-by :file (mapcat :edits compiled-intents))
        compiled-files (mapv #(compile-file % (get sources %) (get edits-by-file % []))
                             files)
        actual {:intent-count (count compiled-intents)
                :edit-count (reduce + (map :match-count compiled-intents))
                :changed-file-count (count (filter (comp pos? :match-count)
                                                   compiled-files))}]
    (when-not (= expect actual)
      (refuse! :transaction-expectation-mismatch
               "Compiled transaction does not match aggregate expectations"
               {:expected expect :actual actual}))
    (cond->
      {:ok true
       :operation :change
       :transaction-version transaction-version
       :intent-count (:intent-count actual)
       :match-count (:edit-count actual)
       :changed-file-count (:changed-file-count actual)
       :intents (mapv #(dissoc % :edits) compiled-intents)
       :files (mapv #(dissoc % :result-source :diff) compiled-files)
       :diff (apply str (keep :diff compiled-files))
       :original-sources (select-keys sources files)
       :future-sources (into {} (map (juxt :file :result-source) compiled-files))
       :validated {:whole-files-parsed true
                   :file-count (count compiled-files)}}
      (= :changes mode)
      (assoc :change-count (:intent-count actual)
             :changes (mapv #(dissoc % :edits) compiled-intents))

      ;; @spec MCP-OP-EDIT-031
      ;; Creations ride the same frozen compile as the edits. They are kept
      ;; out of :original-sources and :future-sources because those maps model
      ;; files that already exist and are replaced in place.
      (seq create-files)
      (assoc :created-files
             (mapv (fn [{:keys [file content workspace-root]}]
                     (cond->
                       {:file file
                        :result-hash (structural-lens/source-hash content)
                        :content content}
                       workspace-root
                       (assoc :workspace-root workspace-root)))
                   create-files)
             :created-file-count (count create-files)
             :created-directories
             (->> create-files
                  (mapcat :directories)
                  distinct
                  vec)))))

(defn compile-transaction
  "Compile explicit exact structural intents against an in-memory file map.
   Returns a complete future state or one structured refusal. Performs no I/O."
  ([sources spec]
   (compile-transaction sources spec nil))
  ([sources spec options]
   (try
     (when-not (map? sources)
       (refuse! :invalid-sources "Sources must be a map of file path to source"))
     (compile-transaction* sources spec options)
     (catch clojure.lang.ExceptionInfo e
       (merge {:error (.getMessage e)} (ex-data e)))
     (catch Exception e
       {:error (.getMessage e)
        :error-type :intent-compiler-failure}))))

(def ^:private unaddressed-match-limit 20)

;; @spec MCP-OP-MATCHED-004
(defn- preorder-subtree-ends
  "Map one snapshot's preorder index to the last preorder index of its subtree.

   Matched sites and compiled edits are numbered by the same whitespace-skipping
   preorder walk, so this turns a site's `:address {:preorder}` into the closed
   span an edit's `[:address :preorder]`/`:end-preorder` span is compared with.
   Line numbers cannot do that: two sites on one line share a line span."
  [source]
  (persistent!
    (reduce (fn [ends [preorder location]]
              (assoc! ends preorder
                      (+ preorder (node-size (z/node location)) -1)))
            (transient {})
            (map-indexed vector
                         (zipper-locations
                           (z/of-string source {:track-position? true}))))))

(defn- edit-preorder-span
  "Pre-image preorder span [start end] of one compiled edit."
  [{:keys [address end-preorder]}]
  (let [start (:preorder address)]
    (when (and (integer? start) (integer? end-preorder))
      [start end-preorder])))

(defn- site-preorder-span
  "Pre-image preorder span [start end] of one matched site."
  [ends site]
  (let [start (get-in site [:address :preorder])]
    (when (integer? start)
      [start (get ends start start)])))

(defn- spans-intersect?
  [[left-start left-end] [right-start right-end]]
  (and (<= left-start right-end) (<= right-start left-end)))

;; @spec MCP-OP-MATCHED-001
;; @spec MCP-OP-MATCHED-002
;; @spec MCP-OP-MATCHED-003
;; @spec MCP-OP-MATCHED-004
(defn matched-basis-evidence
  "Pure: one compiled transaction plus one prior-match basis in ; receipt
   evidence or one typed pre-write refusal out. Performs no I/O.

   The basis is exactly what an `inspect_clojure` `match` receipt already
   returned for one file: its project-relative path, its `file_hash`, the exact
   pattern, and the match count. The transaction's own frozen pre-image is the
   only snapshot consulted, so the two calls are joined by the caller's hash and
   the server retains no session state between them. A site is addressed when
   its pre-image preorder span intersects a compiled edit's preorder span."
  [compiled {:keys [file file-hash match public] expected-count :count}]
  (let [source (get (:original-sources compiled) file)
        public-file (get public :file file)
        public-files (or (:files public)
                         (vec (sort (keys (:original-sources compiled)))))
        actual-hash (when source (structural-lens/source-hash source))
        stale (fn [mismatch message extra]
                (merge {:error-type :expect-matched-stale
                        :error message
                        :mismatch mismatch
                        :file public-file
                        :source-unchanged true
                        :remedy (str "Re-run the inspect_clojure match against the "
                                     "current snapshot and resend expect_matched "
                                     "from that receipt, or omit expect_matched.")}
                       extra))]
    (cond
      (nil? source)
      (stale "file_not_in_transaction"
             (str "expect_matched names " public-file
                  ", which this transaction did not read")
             {:transaction-files public-files})

      (not= file-hash actual-hash)
      (stale "file_hash"
             (str "expect_matched was taken from a different snapshot of "
                  public-file)
             {:expected-file-hash file-hash :actual-file-hash actual-hash})

      :else
      (let [found (structural-lens/find-subforms source {:match match})]
        (cond
          ;; The pattern and the file are separate causes and get separate
          ;; labels: only the caller can fix an unusable pattern.
          (= :invalid-match (:error-type found))
          {:error-type :expect-matched-invalid-pattern
           :error (str "expect_matched.match must be exactly one complete "
                       "Clojure form: " (:error found))
           :file public-file
           :match match
           :source-unchanged true
           :remedy "Resend the exact pattern string the match receipt echoed."}

          (:error found)
          {:error-type :expect-matched-unreadable-source
           :error (str "expect_matched could not be evaluated against "
                       public-file ": " (:error found))
           :file public-file
           :match match
           :source-unchanged true
           :remedy (str "Repair " public-file " so it parses, re-run the "
                        "inspect_clojure match, and resend expect_matched from "
                        "that receipt.")}

          (not= expected-count (:match-count found))
          (stale "match_count"
                 (str "expect_matched declared " expected-count
                      " matches; this snapshot has " (:match-count found))
                 {:expected-match-count expected-count
                  :actual-match-count (:match-count found)})

          :else
          (let [spans (->> (:files compiled)
                           (filter #(= file (:file %)))
                           (mapcat :edits)
                           (keep edit-preorder-span)
                           vec)
                ends (preorder-subtree-ends source)
                unaddressed
                (->> (:matches found)
                     (remove (fn [site]
                               (when-let [span (site-preorder-span ends site)]
                                 (boolean (some #(spans-intersect? span %)
                                                spans)))))
                     (mapv (fn [site]
                             {:line (:line site)
                              :hash (structural-lens/source-hash
                                      (:source site))})))
                returned (vec (take unaddressed-match-limit unaddressed))]
            {:ok true
             :evidence
             {:expect-matched {:file public-file
                               :match (:match found)
                               :file-hash actual-hash
                               :count expected-count}
              :matched-count (:match-count found)
              :addressed-matches (- (:match-count found) (count unaddressed))
              :unaddressed-match-count (count unaddressed)
              :unaddressed-matches returned
              :unaddressed-matches-truncated (> (count unaddressed)
                                                unaddressed-match-limit)}}))))))

(defn- canonical-effect-refusal!
  [message data]
  (refuse! :invalid-canonical-effect-input
           message
           (assoc data
                  :source-unchanged true
                  :mutation-attempted false
                  :write-authority false)))

(defn- project-relative-file
  [canonical-project-root file]
  (when-not (and (string? canonical-project-root)
                 (not (str/blank? canonical-project-root))
                 (string? file)
                 (not (str/blank? file)))
    (canonical-effect-refusal!
      "Canonical effect identity requires non-empty project and file paths"
      {:file file}))
  (try
    (let [supplied-root (java.nio.file.Paths/get canonical-project-root
                                                 (make-array String 0))
          _ (when-not (.isAbsolute supplied-root)
              (canonical-effect-refusal!
                "Canonical effect project root must be absolute"
                {}))
          root (.normalize supplied-root)
          source (java.nio.file.Paths/get file (make-array String 0))
          _ (when-not (.isAbsolute source)
              (canonical-effect-refusal!
                "Canonical effect file path must be absolute"
                {:file file}))
          resolved (-> (if (.isAbsolute source)
                         source
                         (.resolve root source))
                       .normalize)]
      (when-not (.startsWith resolved root)
        (canonical-effect-refusal!
          "Canonical effect file resolves outside the project root"
          {:file file}))
      (let [relative (-> (str (.relativize root resolved))
                         (str/replace "\\" "/"))]
        (when (str/blank? relative)
          (canonical-effect-refusal!
            "Canonical effect file identity is empty"
            {:file file}))
        relative))
    (catch clojure.lang.ExceptionInfo error
      (throw error))
    (catch Exception error
      (canonical-effect-refusal!
        "Canonical effect file identity is invalid"
        {:file file :cause (.getMessage error)}))))

(defn- canonical-effect-row
  [file edit]
  (let [path (:path edit)
        preorder (get-in edit [:address :preorder])
        end-preorder (:end-preorder edit)
        raw-offset (:offset edit)
        raw? (:raw edit)
        before (:before edit)
        after (:after edit)
        insert-side (:insert-side edit)
        delete? (:delete edit)
        _ (when (or (and insert-side delete?)
                    (and insert-side
                         (not (#{:insert-left :insert-right} insert-side)))
                    (and (some? delete?) (not (true? delete?))))
            (canonical-effect-refusal!
              "Canonical effect has an invalid edit-kind combination"
              {:file file}))
        kind (cond
               insert-side insert-side

               delete? :delete
               :else :replace)
        natural-offset? (and (integer? raw-offset) (not (neg? raw-offset)))
        kind-evidence?
        (and
          (string? before)
          (string? after)
          (case kind
            (:insert-left :insert-right)
            (and (true? raw?)
                 natural-offset?
                 (empty? before)
                 (not (str/blank? after)))

            :delete
            (and (true? raw?)
                 natural-offset?
                 (not (str/blank? before))
                 (empty? after))

            :replace
            (and (not (str/blank? before))
                 (not (str/blank? after))
                 (or (and (nil? raw?) (nil? raw-offset))
                     (and (true? raw?) natural-offset?)))))]
    (when-not (and (vector? path)
                   (seq path)
                   (every? #(and (integer? %) (not (neg? %))) path)
                   (integer? preorder)
                   (not (neg? preorder))
                   (integer? end-preorder)
                   (<= preorder end-preorder)
                   (or (nil? raw-offset) natural-offset?)
                   (string? before)
                   (string? after)
                   kind-evidence?)
      (canonical-effect-refusal!
        "Canonical effect lacks a complete resolved structural identity"
        {:file file}))
    {:sort-key [path preorder end-preorder raw-offset kind before after]
     :row [:effect kind path preorder end-preorder raw-offset before after]}))

(defn canonical-effect-identity
  ;; @spec MCP-OP-EDIT-028
  ;; @spec MCP-OP-EDIT-029
  "Derive a lossless order-independent identity from one successful compiled transaction."
  [canonical-project-root compiled]
  (when-not (and (map? compiled)
                 (true? (:ok compiled))
                 (= :change (:operation compiled))
                 (= transaction-version (:transaction-version compiled))
                 (integer? (:intent-count compiled))
                 (pos? (:intent-count compiled))
                 (integer? (:match-count compiled))
                 (pos? (:match-count compiled))
                 (integer? (:changed-file-count compiled))
                 (pos? (:changed-file-count compiled))
                 (vector? (:intents compiled))
                 (= (:intent-count compiled) (count (:intents compiled)))
                 (string? (:diff compiled))
                 (map? (:validated compiled))
                 (true? (get-in compiled [:validated :whole-files-parsed]))
                 (map? (:original-sources compiled))
                 (map? (:future-sources compiled)))
    (canonical-effect-refusal!
      "Canonical effect identity requires a successful compiled transaction"
      {}))
  (let [compiled-files (:files compiled)]
    (when-not (and (vector? compiled-files) (seq compiled-files))
      (canonical-effect-refusal!
        "Canonical effect identity requires compiled files"
        {}))
    (let [compiled-file-set (set (map :file compiled-files))
          original-file-set (set (keys (:original-sources compiled)))
          future-file-set (set (keys (:future-sources compiled)))
          _ (when-not (and (= (count compiled-files)
                              (get-in compiled [:validated :file-count]))
                           (= compiled-file-set original-file-set)
                           (= compiled-file-set future-file-set))
              (canonical-effect-refusal!
                "Canonical effect identity requires one complete compiler snapshot"
                {:compiled-files (count compiled-files)
                 :validated-files (get-in compiled [:validated :file-count])
                 :original-files (count original-file-set)
                 :future-files (count future-file-set)}))
          file-records
          (mapv
            (fn [{:keys [file source-hash result-hash match-count edits]}]
              (let [original-source (get (:original-sources compiled) file)
                    future-source (get (:future-sources compiled) file)]
                (when-not (and (string? file)
                               (string? original-source)
                               (string? future-source)
                               (string? source-hash)
                               (re-matches #"[0-9a-f]{64}" source-hash)
                               (string? result-hash)
                               (re-matches #"[0-9a-f]{64}" result-hash)
                               (= source-hash
                                  (structural-lens/source-hash original-source))
                               (= result-hash
                                  (structural-lens/source-hash future-source))
                               (vector? edits)
                               (integer? match-count)
                               (not (neg? match-count))
                               (= match-count (count edits)))
                  (canonical-effect-refusal!
                    "Canonical effect file lacks complete snapshot or edit evidence"
                    {:file file}))
                (let [effect-records (mapv #(canonical-effect-row file %) edits)
                      relative-file
                      (project-relative-file canonical-project-root file)]
                  (assert-disjoint-edits! file edits)
                  {:sort-key relative-file
                   :row [:file
                         relative-file
                         source-hash
                         result-hash
                         (->> effect-records
                              (sort-by :sort-key)
                              (mapv :row))]
                   :effect-count (count effect-records)})))
            compiled-files)
          effect-count (reduce + (map :effect-count file-records))
          changed-file-count (count (filter (comp pos? :effect-count)
                                            file-records))
          duplicate-files
          (->> file-records
               (map :sort-key)
               frequencies
               (keep (fn [[file count]] (when (> count 1) file)))
               sort
               vec)]
      (when-not (pos? effect-count)
        (canonical-effect-refusal!
          "Canonical effect identity requires at least one concrete effect"
          {}))
      (when (seq duplicate-files)
        (canonical-effect-refusal!
          "Canonical effect identity contains duplicate project-relative files"
          {:files duplicate-files}))
      (when (not= effect-count (:match-count compiled))
        (canonical-effect-refusal!
          "Canonical effect count does not match the compiled transaction"
          {:expected (:match-count compiled) :actual effect-count}))
      (when (not= changed-file-count (:changed-file-count compiled))
        (canonical-effect-refusal!
          "Canonical effect file count does not match the compiled transaction"
          {:expected (:changed-file-count compiled)
           :actual changed-file-count}))
      (let [ordered-files (->> file-records
                               (filter (comp pos? :effect-count))
                               (sort-by :sort-key)
                               (mapv :row))
            projection [:canonical-effect/v1
                        ordered-files
                        (count ordered-files)
                        effect-count]]
        {:version 1
         :sha256 (structural-lens/source-hash (pr-str projection))
         :files (count ordered-files)
         :effects effect-count
         :projection projection}))))

(defn with-future-sources
  "Replace a compiled transaction's candidate sources with staged transformed
   sources. A changed candidate becomes one hash-guarded raw edit so inverse
   receipts restore the exact original bytes. Performs no I/O."
  [compiled future-sources]
  (try
    (when-not (and (:ok compiled)
                   (map? future-sources)
                   (= (set (keys (:future-sources compiled)))
                      (set (keys future-sources))))
      (refuse! :invalid-future-sources
               "Transformed future sources must cover the compiled file set exactly"))
    (let [file-plans
          (mapv
            (fn [plan]
              (let [file (:file plan)
                    original (get-in compiled [:original-sources file])
                    candidate (get-in compiled [:future-sources file])
                    transformed (get future-sources file)]
                (validate-complete-source! file transformed :invalid-result-source)
                (if (= candidate transformed)
                  plan
                  (let [edit {:intent-index -1
                              :raw true
                              :offset 0
                              :source-offset 0
                              :line 1
                              :end-line (max 1 (count (str/split-lines original)))
                              :before original
                              :after transformed}]
                    (assoc plan
                           :result-hash (structural-lens/source-hash transformed)
                           :edits [edit])))))
            (:files compiled))]
      (assoc compiled
             :files file-plans
             :future-sources future-sources
             :diff (apply str
                          (map (fn [{:keys [file edits]}]
                                 (file-diff file edits))
                               file-plans))
             :format {:status :complete
                      :file-count (count future-sources)
                      :changed-file-count
                      (count (filter (fn [[file source]]
                                       (not= source
                                             (get-in compiled
                                                     [:future-sources file])))
                                     future-sources))}))
    (catch clojure.lang.ExceptionInfo error
      (merge {:error (.getMessage error)} (ex-data error)))
    (catch Exception error
      {:error (.getMessage error)
       :error-type :future-source-transformation-failed})))

(defn compile-addressed-transaction
  "Compile retained structural addresses without rerunning a selector. Each edit
   must contain :id, :file, :address or :path, :before, :line, :end-line, and
   :end-preorder, plus exactly one non-empty :after form or :delete true."
  [sources edits]
  (try
    (when-not (and (map? sources) (vector? edits) (seq edits))
      (refuse! :invalid-addressed-transaction
               "Addressed compilation requires source snapshots and at least one edit"))
    (let [ids (mapv :id edits)]
      (when-not (and (every? #(and (string? %) (seq %)) ids)
                     (= (count ids) (count (distinct ids))))
        (refuse! :invalid-addressed-transaction
                 "Addressed edit IDs must be non-empty and unique")))
    (let [prepared
          (mapv
            (fn [intent-index {:keys [file before after address path
                                      line end-line end-preorder delete] :as edit}]
              (let [replace? (and (string? after) (seq after))
                    delete? (= true delete)]
                (when-not (and (string? file)
                               (string? (get sources file))
                               (string? before)
                               (not= (boolean replace?) delete?)
                               (or (map? address) (vector? path))
                               (pos-int? line)
                               (pos-int? end-line)
                               (nat-int? end-preorder))
                  (refuse! :invalid-addressed-edit
                           "Addressed edit is missing a retained source, address, or range"
                           {:intent-index intent-index :id (:id edit)}))
                (parse-one-form before ":before")
                (when replace?
                  (parse-one-form after ":after"))
                (assoc edit :intent-index intent-index)))
            (range)
            edits)
          files (vec (distinct (map :file prepared)))
          _ (doseq [file files]
              (validate-complete-source! file (get sources file) :invalid-source))
          edits-by-file (group-by :file prepared)
          compiled-files (mapv #(compile-file % (get sources %) (get edits-by-file %))
                               files)
          intents (mapv (fn [{:keys [id file]}]
                          {:id id :files [file] :match-count 1})
                        prepared)]
      {:ok true
       :operation :change
       :transaction-version transaction-version
       :intent-count (count prepared)
       :match-count (count prepared)
       :changed-file-count (count compiled-files)
       :intents intents
       :files (mapv #(dissoc % :result-source :diff) compiled-files)
       :diff (apply str (keep :diff compiled-files))
       :original-sources (select-keys sources files)
       :future-sources (into {} (map (juxt :file :result-source) compiled-files))
       :validated {:whole-files-parsed true
                   :file-count (count compiled-files)
                   :retained-addresses true}})
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e) :error-type :intent-compiler-failure})))

(defn- spec-files
  [spec]
  (->> (or (:intents spec) (:changes spec))
       (mapcat (if (:changes spec) :in :files))
       distinct
       vec))

(defn- canonical-file
  [file]
  (try
    (.getCanonicalPath (java.io.File. file))
    (catch Exception e
      (refuse! :invalid-files
               (str "Cannot canonicalize source path " (pr-str file)
                    ": " (.getMessage e))
               {:file file}))))

(defn- canonicalize-spec
  [spec]
  (cond-> spec
    (:intents spec)
    (update :intents
            (fn [intents]
              (mapv #(update % :files (partial mapv canonical-file))
                    intents)))

    (:changes spec)
    (update :changes
            (fn [changes]
              (mapv #(update % :in (partial mapv canonical-file))
                    changes)))))

(defn- read-sources
  [files]
  (reduce (fn [sources file]
            (try
              (assoc sources file (slurp file))
              (catch Exception e
                (refuse! :invalid-source
                         (str "Cannot read source " file ": " (.getMessage e))
                         {:file file}))))
          {}
          files))

(defn- public-plan
  [compiled]
  (-> compiled
      (dissoc :original-sources :future-sources)
      (update :files
              (fn [files]
                (mapv #(update % :edits
                               (fn [edits]
                                 (mapv (fn [edit]
                                         (select-keys edit
                                                      [:intent-index :address
                                                       :line :end-line]))
                                       edits)))
                      files)))))

(defn plan-change
  "Read the explicit files in :spec and compile one non-mutating transaction
   plan. The public result retains concrete edits and hashes but omits complete
   future-file source."
  [{:keys [spec] :as opts}]
  (try
    (let [unknown (vec (sort (remove #{:op :spec} (keys opts))))]
      (when (seq unknown)
        (refuse! :unknown-arguments
                 (str "Unknown :change arguments: " (str/join ", " unknown))
                 {:unknown unknown})))
    (when-not (map? spec)
      (refuse! :invalid-transaction-spec ":spec must be an EDN map"))
    ;; Reject malformed proposal data before touching the filesystem. Compile
    ;; validates again after canonicalization so aliased paths cannot evade the
    ;; exact same contract.
    (validate-spec! spec)
    (let [canonical-spec (canonicalize-spec spec)
          sources (read-sources (spec-files canonical-spec))]
      (public-plan
        (:compiled
          (operation-algebra/compile-change
            (operation-algebra/change-entry compile-transaction)
            {:operation :change
             :operation-version 1
             :entrance :cli
             :policy :cli-legacy
             :lifecycle :preview}
            sources
            canonical-spec))))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e)
       :error-type :intent-compiler-failure})))

(defn- changed-file-plans
  [compiled]
  (filterv (comp pos? :match-count) (:files compiled)))

(defn- read-source!
  [read-source file]
  (try
    (let [source (read-source file)]
      (when-not (string? source)
        (refuse! :source-read-failed
                 (str "Source reader did not return text for " file)
                 {:file file}))
      source)
    (catch clojure.lang.ExceptionInfo e
      (throw e))
    (catch Exception e
      (refuse! :source-read-failed
               (str "Cannot read source " file ": " (.getMessage e))
               {:file file}))))

(defn- assert-file-hash!
  [read-source file expected error-type]
  (let [source (read-source! read-source file)
        actual (structural-lens/source-hash source)]
    (when-not (= expected actual)
      (refuse! error-type
               (str "Source hash mismatch for " file)
               {:file file :expected-hash expected :actual-hash actual}))
    source))

(defn- recovery-result
  [read-source write-source! originals
   {:keys [file source-hash result-hash]}]
  (try
    (let [current (read-source! read-source file)
          current-hash (structural-lens/source-hash current)]
      (cond
        (= source-hash current-hash)
        {:file file :status :original :source-hash current-hash}

        (= result-hash current-hash)
        (try
          (write-source! file (get originals file))
          (let [restored (read-source! read-source file)
                restored-hash (structural-lens/source-hash restored)]
            (if (= source-hash restored-hash)
              {:file file :status :restored :source-hash restored-hash}
              {:file file :status :restore-hash-mismatch
               :expected-hash source-hash :actual-hash restored-hash}))
          (catch Exception e
            {:file file :status :restore-failed :error (.getMessage e)}))

        :else
        {:file file :status :unexpected-source
         :original-hash source-hash
         :result-hash result-hash
         :actual-hash current-hash}))
    (catch Exception e
      {:file file :status :recovery-read-failed :error (.getMessage e)})))

(defn- recover-transaction!
  [read-source write-source! originals file-plans]
  (mapv #(recovery-result read-source write-source! originals %)
        (reverse file-plans)))

(def ^:private recovered-statuses
  ;; @spec MCP-OP-EDIT-031
  ;; :original and :restored belong to edited files; :absent and :deleted to
  ;; creations that were removed again; :present and :restored to deletions an
  ;; inverse transaction had already applied.
  #{:original :restored :absent :deleted :present})

(defn- recovered?
  [recovery]
  (every? #(contains? recovered-statuses (:status %)) recovery))

(defn- execute-writes!
  [read-source write-source! futures file-plans]
  (doseq [{:keys [file source-hash result-hash]} file-plans]
    ;; Recheck immediately before each replacement. If a later file goes stale
    ;; after an earlier write, the caller enters the same recovery protocol.
    (assert-file-hash! read-source file source-hash :source-hash-mismatch)
    (write-source! file (get futures file))
    (assert-file-hash! read-source file result-hash :read-back-hash-mismatch)))

(defn- default-exists?
  [file]
  (.exists (io/file file)))

(defn- default-delete-file!
  [file]
  (Files/deleteIfExists (.toPath (io/file file))))

(defn- default-create-directory!
  [directory]
  (Files/createDirectory
    (.toPath (io/file directory))
    (make-array java.nio.file.attribute.FileAttribute 0)))

;; @spec MCP-OP-EDIT-031
(defn- execute-creations!
  "Create every absent target after the edits have committed and read back.

   Directories are revalidated and created shallowest-first. Directories and
   files are recorded only as they land, so recovery removes exactly what this
   call made."
  [read-source create-source! exists? create-directory!
   directories created-files written-directories written-files]
  (doseq [directory directories]
    (let [directory-path (.normalize
                           (.toAbsolutePath (.toPath (io/file directory))))
          workspace-root
          (some (fn [{:keys [file workspace-root]}]
                  (when (and workspace-root
                             (.startsWith
                               (.normalize
                                 (.toAbsolutePath (.toPath (io/file file))))
                               directory-path))
                    workspace-root))
                created-files)]
      ;; @spec MCP-OP-EDIT-036
      (file-ops/revalidate-create-target! workspace-root directory)
      (when-not (exists? directory)
        (create-directory! directory)
        (swap! written-directories conj directory)
        (when-not (exists? directory)
          (refuse! :target-parent-create-failed
                   (str "Could not verify created directory " directory)
                   {:directory directory})))))
  (doseq [{:keys [file content result-hash workspace-root]} created-files]
    (when (exists? file)
      (refuse! :target-already-exists
               (str "Creation target appeared during commit: " file)
               {:file file :path file}))
    ;; @spec MCP-OP-EDIT-036
    (file-ops/revalidate-create-target! workspace-root file)
    ;; @spec MCP-OP-EDIT-035
    (create-source! file content)
    (swap! written-files conj {:file file :content content})
    (assert-file-hash! read-source file result-hash :read-back-hash-mismatch)))

;; @spec MCP-OP-EDIT-031
(defn- execute-deletions!
  "Remove every file and directory an inverse transaction retires."
  [read-source exists? delete-file! deleted-files directories deleted]
  (doseq [{:keys [file result-hash]} deleted-files]
    (when (exists? file)
      (assert-file-hash! read-source file result-hash :result-hash-mismatch))
    (delete-file! file)
    (swap! deleted conj file)
    (when (exists? file)
      (refuse! :target-delete-failed
               (str "Could not verify deleted file " file)
               {:file file :path file})))
  ;; A directory that has since gained unrelated files is left in place. The
  ;; hash-fenced file deletion is the inverse; the directory is a convenience.
  (doseq [directory (reverse directories)]
    (when (exists? directory)
      (try (delete-file! directory) (catch Exception _ nil)))))

;; @spec MCP-OP-EDIT-031
(defn- rollback-creations!
  "Remove only the files and directories this commit actually created.

   A created file whose bytes are no longer ours is never removed; it is
   reported as unknown bytes so the caller refuses instead of guessing."
  [read-source exists? delete-file! written-files written-directories]
  (into
    (mapv
      (fn [{:keys [file content]}]
        (let [current (when (exists? file)
                        (try (read-source file) (catch Exception _ ::unreadable)))]
          (cond
            (nil? current)
            {:file file :status :absent}

            (= content current)
            (do (try (delete-file! file) (catch Exception _ nil))
                {:file file :status (if (exists? file) :delete-failed :deleted)})

            :else
            {:file file :status :unexpected-source})))
      (reverse written-files))
    (mapv
      (fn [directory]
        (if-not (exists? directory)
          {:directory directory :status :absent}
          (try
            (delete-file! directory)
            {:directory directory
             :status (if (exists? directory) :delete-failed :deleted)}
            (catch Exception error
              {:directory directory
               :status :not-empty-or-changed
               :error (.getMessage error)}))))
      (reverse written-directories))))

;; @spec MCP-OP-EDIT-031
(defn- rollback-deletions!
  "Restore every file an inverse transaction had already deleted."
  [read-source write-source! exists? deleted-files deleted]
  (let [by-file (into {} (map (juxt :file identity)) deleted-files)]
    (mapv
      (fn [file]
        (let [{:keys [result-source]} (get by-file file)]
          (cond
            (exists? file)
            {:file file :status :present}

            (string? result-source)
            (try
              (write-source! file result-source)
              {:file file
               :status (if (= result-source (read-source file))
                         :restored
                         :restore-hash-mismatch)}
              (catch Exception error
                {:file file :status :restore-failed :error (.getMessage error)}))

            :else
            {:file file :status :restore-source-unavailable})))
      (reverse deleted))))

(defn- verified-hashes
  [read-source file-plans]
  (into {}
        (map (fn [{:keys [file result-hash]}]
               (assert-file-hash! read-source file result-hash
                                  :read-back-hash-mismatch)
               [file result-hash]))
        file-plans))

(def ^:dynamic *on-write-boundary*
  "Called once, immediately before a transaction writes its first source byte.

  A DYNAMIC binding rather than an extra argument, deliberately. `change!` has
  exactly one runtime path — the single-arity `commit-compiled!` — and that is
  a ratchet the architecture tests hold: threading this through the injected io
  map would fork the public commit onto a second arity, and hoisting the io map
  out of `commit-compiled!` to share it would move the raw filesystem effects
  out of the one form whose effect inventory is bounded. Nothing here is
  asynchronous, so a thread-local binding is exactly scoped to the commit it
  wraps.

  It exists because heap exhaustion has to be answerable: everything before
  this point — spec validation, the frozen read, compilation, receipt staging,
  the whole-file hash preflight — leaves the tree byte-identical, and a caller
  that reports `mutation_attempted` from its own call site cannot tell the
  difference."
  nil)

(defn commit-compiled!
  "Commit a successfully compiled transaction through injected source I/O.
   Ordinary handled failures restore files that still equal either the original
   or transaction result. Unexpected bytes are never overwritten.

   A compiled transaction may also carry :created-files, which are written only
   after every edit has committed and read back, and :deleted-files, which an
   inverse transaction uses to retire what a forward creation made.

   `*on-write-boundary*`, when bound, is called exactly once immediately before
   the first source byte is written and after every read-only preflight has
   passed. It is the transaction's own write boundary: a caller that has to tell
   an operator whether its tree may have been mutated reads it from here rather
   than from the entrance of the call, which precedes spec validation, the
   frozen read, compilation, receipt staging, and the whole-file hash
   preflight."
  ([compiled]
   (commit-compiled! compiled
                     {:read-source slurp
                      :write-source! file-ops/atomic-write!
                      :create-source! file-ops/atomic-create!}))
  ([compiled {:keys [read-source write-source! create-source!] :as io}]
   (let [exists? (or (:exists? io) default-exists?)
         delete-file! (or (:delete-file! io) default-delete-file!)
         create-directory! (or (:create-directory! io) default-create-directory!)]
     (try
       (when-not (and (:ok compiled)
                      (map? (:original-sources compiled))
                      (map? (:future-sources compiled))
                      (ifn? read-source)
                      (ifn? write-source!))
         (refuse! :invalid-compiled-transaction
                  "Commit requires one complete compiled transaction and source I/O"))
       (let [file-plans (changed-file-plans compiled)
             originals (:original-sources compiled)
             futures (:future-sources compiled)
             created-files (vec (:created-files compiled))
             planned-directories (vec (:created-directories compiled))
             deleted-files (vec (:deleted-files compiled))
             removed-directories (vec (:removed-directories compiled))
             written-directories (atom [])
             written-files (atom [])
             deleted (atom [])]
         ;; The all-file preflight is outside the recovery block because it has
         ;; not written anything.
         (doseq [{:keys [file source-hash]} file-plans]
           (assert-file-hash! read-source file source-hash
                              :source-hash-mismatch))
         (doseq [{:keys [file]} created-files]
           (when (exists? file)
             (refuse! :target-already-exists
                      (str "Creation target already exists: " file)
                      {:file file :path file})))
         ;; the write boundary: every refusal above this line leaves the tree
         ;; exactly as the caller left it, and every byte written is below it
         (when-let [notify *on-write-boundary*]
           (notify))
         (try
           (execute-writes! read-source write-source! futures file-plans)
           (execute-creations! read-source (or create-source! write-source!)
                               exists? create-directory!
                               planned-directories created-files
                               written-directories written-files)
           (execute-deletions! read-source exists? delete-file!
                               deleted-files removed-directories deleted)
           (let [hashes (verified-hashes read-source file-plans)
                 created-hashes (into {} (map (juxt :file :result-hash))
                                      created-files)]
             (cond->
               {:ok true
                :operation :change!
                :transaction-version transaction-version
                :committed true
                :changed-file-count (count file-plans)
                :verified {:whole-files true
                           :file-count (+ (count file-plans) (count created-files))
                           :read-back-hashes (merge hashes created-hashes)}}
               (seq created-files)
               (assoc :created-file-count (count created-files))

               (seq deleted-files)
               (assoc :deleted-file-count (count deleted-files))))
           (catch Exception cause
             (let [recovery (-> (recover-transaction!
                                  read-source write-source! originals file-plans)
                                (into (rollback-creations!
                                        read-source exists? delete-file!
                                        @written-files @written-directories))
                                (into (rollback-deletions!
                                        read-source write-source! exists?
                                        deleted-files @deleted)))
                   rolled-back? (recovered? recovery)
                   cause-data (ex-data cause)]
               (merge
                 {:error (if rolled-back?
                           "Transaction write failed; all files restored"
                           "Transaction write failed; manual recovery required")
                  :error-type (if rolled-back?
                                :transaction-write-failed
                                :transaction-recovery-required)
                  :cause-error (.getMessage cause)
                  :cause-error-type (or (:error-type cause-data)
                                        :transaction-write-exception)
                  :rolled-back rolled-back?
                  :recovery recovery}
                 (select-keys cause-data
                              [:file :expected-hash :actual-hash]))))))
       (catch clojure.lang.ExceptionInfo e
         (merge {:error (.getMessage e)} (ex-data e)))
       (catch Exception e
         {:error (.getMessage e)
          :error-type :transaction-write-exception})))))

(defn- reverse-edit
  [{:keys [intent-index address path line end-line before after]}]
  {:intent-index intent-index
   :address address
   :path path
   :line line
   :end-line end-line
   :before after
   :after before})

(defn- reverse-raw-edits
  [edits]
  (loop [remaining (sort-by :offset edits)
         cumulative-delta 0
         inverse []]
    (if-let [{:keys [intent-index line end-line offset before after]}
             (first remaining)]
      (let [result-offset (+ offset cumulative-delta)
            delta (- (count after) (count before))]
        (recur (next remaining)
               (+ cumulative-delta delta)
               (conj inverse
                     {:intent-index intent-index
                      :raw true
                      :offset result-offset
                      :source-offset offset
                      :line line
                      :end-line end-line
                      :before after
                      :after before})))
      inverse)))

(defn- receipt-hash
  [receipt]
  (structural-lens/source-hash (pr-str (dissoc receipt :receipt-hash))))

(defn build-receipt
  "Build the durable forward evidence and concrete inverse edits for one
   compiled transaction. Full original and future files are intentionally not
   embedded."
  [compiled]
  (let [files (->> (changed-file-plans compiled)
                   (mapv (fn [{:keys [file source-hash result-hash edits]}]
                           {:file file
                            :source-hash source-hash
                            :result-hash result-hash
                            :inverse-edits
                            (if (some :raw edits)
                              (reverse-raw-edits edits)
                              (mapv reverse-edit edits))})))
        inverse-edit-count
        (reduce + 0 (map #(count (:inverse-edits %)) files))
        logical-match-count?
        (some #(= :rename-binding (:operator %)) (:intents compiled))
        receipt {:receipt-version receipt-version
                 :transaction-version transaction-version
                 :operation :change!
                 :intent-count (:intent-count compiled)
                 :match-count (:match-count compiled)
                 :inverse-edit-count inverse-edit-count
                 :changed-file-count (:changed-file-count compiled)
                 :files files
                 :intents (:intents compiled)
                 :diff (:diff compiled)
                 :inverse {:operation :undo-change!
                           :guarded-file-count (count files)}}
        receipt (cond-> receipt
                  logical-match-count?
                  (assoc :match-count-kind :binding-occurrences)

                  ;; @spec MCP-OP-EDIT-031
                  ;; A created file has no inverse edit, so its content is the
                  ;; only evidence that can prove the bytes about to be deleted
                  ;; are still ours. It is recorded here for that reason alone.
                  (seq (:created-files compiled))
                  (assoc :created-files
                         (mapv (fn [{:keys [file result-hash content]}]
                                 {:file file
                                  :result-hash result-hash
                                  :result-source content})
                               (:created-files compiled))
                         :created-directories
                         (vec (:created-directories compiled))))]
    (assoc receipt :receipt-hash (receipt-hash receipt))))

(defn- invalid-receipt!
  [message & [data]]
  (refuse! :invalid-transaction-receipt message data))

(defn- validate-receipt!
  [receipt]
  (when-not (map? receipt)
    (invalid-receipt! "Transaction receipt must be an EDN map"))
  (when-not (= receipt-version (:receipt-version receipt))
    (invalid-receipt!
      (str "Unsupported receipt version: " (pr-str (:receipt-version receipt)))
      {:supported-receipt-version receipt-version}))
  (when-not (= transaction-version (:transaction-version receipt))
    (invalid-receipt!
      (str "Unsupported transaction version: "
           (pr-str (:transaction-version receipt)))
      {:supported-transaction-version transaction-version}))
  (when-not (= :change! (:operation receipt))
    (invalid-receipt! "Receipt operation must be :change!"))
  ;; @spec MCP-OP-EDIT-031
  (let [created (:created-files receipt)]
    (when-not (or (nil? created) (vector? created))
      (invalid-receipt! "Receipt :created-files must be a vector"))
    (when-not (or (nil? (:created-directories receipt))
                  (vector? (:created-directories receipt)))
      (invalid-receipt! "Receipt :created-directories must be a vector"))
    (when-not (and (vector? (:files receipt))
                   (or (seq (:files receipt)) (seq created)))
      (invalid-receipt!
        "Receipt :files must be a non-empty vector unless it creates files"))
    (doseq [{:keys [file result-hash result-source]} created]
      (when-not (and (string? file)
                     (string? result-hash)
                     (re-matches #"[0-9a-f]{64}" result-hash)
                     (string? result-source)
                     (= result-hash (structural-lens/source-hash result-source)))
        (invalid-receipt! "Receipt created-file entry is incomplete"
                          {:file file})))
    (let [created-paths (mapv :file created)]
      (when-not (= (count created-paths) (count (distinct created-paths)))
        (invalid-receipt! "Receipt created-file paths must be distinct"))))
  (when-not (= (:receipt-hash receipt) (receipt-hash receipt))
    (invalid-receipt! "Receipt hash does not match its contents"
                      {:expected-hash (:receipt-hash receipt)
                       :actual-hash (receipt-hash receipt)}))
  (let [files (mapv :file (:files receipt))]
    (when-not (and (every? string? files)
                   (= (count files) (count (distinct files))))
      (invalid-receipt! "Receipt file paths must be distinct strings")))
  (when-not (= (:changed-file-count receipt) (count (:files receipt)))
    (invalid-receipt! "Receipt changed-file count does not match its files"))
  (let [intents (:intents receipt)
        intent-match-counts (mapv :match-count intents)]
    (when-not (= (:intent-count receipt) (count intents))
      (invalid-receipt! "Receipt intent count does not match its intents"))
    (when-not (every? pos-int? intent-match-counts)
      (invalid-receipt! "Receipt intent match counts must be positive integers"))
    (when-not (= (:match-count receipt)
                 (reduce + 0 intent-match-counts))
      (invalid-receipt! "Receipt logical match count does not match its intents")))
  (let [logical-match-count? (= :binding-occurrences
                                (:match-count-kind receipt))
        rename-intent? (some #(= :rename-binding (:operator %))
                             (:intents receipt))
        _ (when-not (= logical-match-count? (boolean rename-intent?))
            (invalid-receipt!
              "Receipt logical match-count evidence does not match its intents"))
        inverse-edit-count? (contains? receipt :inverse-edit-count)
        ;; @spec MCP-OP-EDIT-031
        ;; A create-only transaction has no inverse edit; its inverse is a
        ;; deletion, so zero is the only correct inverse edit count.
        creates-files? (boolean (seq (:created-files receipt)))
        _ (when (and logical-match-count? (not inverse-edit-count?))
            (invalid-receipt!
              "Receipt binding-occurrence evidence requires an inverse edit count"))
        _ (when (and inverse-edit-count?
                     (not (if creates-files?
                            (nat-int? (:inverse-edit-count receipt))
                            (pos-int? (:inverse-edit-count receipt)))))
            (invalid-receipt!
              "Receipt inverse edit count must be a positive integer"))
        actual-inverse-count
        (reduce + 0 (map #(count (:inverse-edits %)) (:files receipt)))
        expected-inverse-count
        (if inverse-edit-count?
          (:inverse-edit-count receipt)
          (:match-count receipt))]
    (when-not (= expected-inverse-count actual-inverse-count)
      (invalid-receipt!
        "Receipt match count or inverse edit count does not match its inverse edits")))
  (when-not (= {:operation :undo-change!
                :guarded-file-count (count (:files receipt))}
               (:inverse receipt))
    (invalid-receipt! "Receipt inverse summary does not match its files"))
  (doseq [{:keys [file source-hash result-hash inverse-edits]} (:files receipt)]
    (when-not (and (string? source-hash)
                   (string? result-hash)
                   (vector? inverse-edits)
                   (seq inverse-edits))
      (invalid-receipt! "Receipt file entry is incomplete" {:file file}))
    (doseq [{:keys [raw offset path before after]} inverse-edits]
      (when-not (and (string? before)
                     (string? after)
                     (if raw
                       (nat-int? offset)
                       (and (vector? path) (seq path)
                            (every? nat-int? path))))
        (invalid-receipt! "Receipt inverse edit is incomplete" {:file file}))))
  receipt)

(defn compile-inverse
  "Compile a receipt's concrete reverse edits against current in-memory source.
   Every current file must match its forward result hash, and every reconstructed
   file must match its original hash."
  [receipt sources]
  (try
    (validate-receipt! receipt)
    (when-not (map? sources)
      (invalid-receipt! "Inverse sources must be a file-to-source map"))
    (let [compiled-files
          (mapv
            (fn [{:keys [file source-hash result-hash inverse-edits]}]
              (let [current (get sources file)]
                (validate-complete-source! file current :invalid-source)
                (let [actual-hash (structural-lens/source-hash current)]
                  (when-not (= result-hash actual-hash)
                    (refuse! :result-hash-mismatch
                             (str "Current source does not match transaction result: "
                                  file)
                             {:file file :expected-hash result-hash
                              :actual-hash actual-hash})))
                (let [restored
                      (try
                        (apply-edits current inverse-edits)
                        (catch clojure.lang.ExceptionInfo e
                          (invalid-receipt!
                            (str "Inverse edit does not match its recorded path: "
                                 file)
                            {:file file
                             :cause-error-type (:error-type (ex-data e))})))
                      _ (validate-complete-source!
                          file restored :invalid-result-source)
                      restored-hash (structural-lens/source-hash restored)]
                  (when-not (= source-hash restored-hash)
                    (invalid-receipt!
                      (str "Inverse result hash does not match original: " file)
                      {:file file :expected-hash source-hash
                       :actual-hash restored-hash}))
                  {:file file
                   :match-count (count inverse-edits)
                   :source-hash result-hash
                   :result-hash source-hash
                   :edits inverse-edits
                   :result-source restored})))
            (:files receipt))
          files (mapv :file compiled-files)
          ;; @spec MCP-OP-EDIT-031
          ;; The inverse of a creation is a hash-fenced deletion. The current
          ;; bytes must still equal the forward result before it may be removed.
          created (vec (:created-files receipt))
          _ (doseq [{:keys [file result-hash]} created]
              (let [current (get sources file)]
                (when-not (string? current)
                  (refuse! :invalid-source
                           (str "Created file is missing for undo: " file)
                           {:file file}))
                (let [actual-hash (structural-lens/source-hash current)]
                  (when-not (= result-hash actual-hash)
                    (refuse! :result-hash-mismatch
                             (str "Created file does not match transaction result: "
                                  file)
                             {:file file :expected-hash result-hash
                              :actual-hash actual-hash})))))]
      (cond->
        {:ok true
         :operation :undo-change!
         :transaction-version transaction-version
         :intent-count (:intent-count receipt)
         :match-count (:match-count receipt)
         :changed-file-count (count files)
         :files (mapv #(dissoc % :result-source) compiled-files)
         :original-sources (select-keys sources files)
         :future-sources (into {} (map (juxt :file :result-source) compiled-files))
         :validated {:whole-files-parsed true :file-count (count files)}}
        (seq created)
        (assoc :deleted-files created
               :removed-directories (vec (:created-directories receipt)))))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e) :error-type :invalid-transaction-receipt})))

(defn- valid-edn-path?
  [path]
  (and (string? path) (str/ends-with? path ".edn")))

(defn- canonical-receipt-path
  [path]
  (when-not (valid-edn-path? path)
    (refuse! :invalid-receipt-path
             ":receipt-out and :receipt must name an .edn file"
             {:path path}))
  (canonical-file path))

(defn- assert-receipt-does-not-alias-source!
  [receipt-path spec]
  (when (some #{receipt-path} (concat (spec-files spec)
                                      (keep :file (:create-files spec))))
    (refuse! :invalid-receipt-path
             "Receipt path must not alias a source file"
             {:path receipt-path})))

(defn- receipt-source
  [receipt]
  (str (pr-str receipt) "\n"))

;; @spec MCP-OP-ALIAS-056
(defn- stage-receipt!
  "Write one receipt to a staging file beside its destination.

  The staging file is opened CREATE_NEW: an open that FAILS when anything
  already holds the STAGING name — a regular file, or a symlink pointing
  somewhere else — rather than following it, and the publish below is an
  ATOMIC_MOVE off that name.

  That pair protects the staging name and NOT the destination name. The
  receipt path arrives here already canonicalised (`canonical-receipt-path`
  → `getCanonicalPath`), so a link sitting on the DESTINATION name was
  resolved before this function computed its parent: there is no link left for
  the rename to replace, and both the staging file and the published receipt
  land in the link's target directory. Detecting that redirect is
  MCP-OP-ALIAS-056's post-write proof, which reads the published file's real
  path — parent AND `.edn` extension — and rolls the transaction back rather
  than reporting ok over a receipt no undo can read."
  [receipt-path receipt]
  (let [target (io/file receipt-path)
        parent (.getParentFile (.getAbsoluteFile target))]
    (when-not (and parent (.exists parent) (.isDirectory parent))
      (refuse! :invalid-receipt-path
               "Receipt parent directory does not exist"
               {:path receipt-path}))
    (let [staged (io/file parent (str ".clj-surgeon-receipt-"
                                      (UUID/randomUUID) ".edn"))]
      (try
        (with-open [^java.io.OutputStream out
                    (Files/newOutputStream
                      (.toPath staged)
                      (into-array OpenOption [StandardOpenOption/CREATE_NEW
                                              StandardOpenOption/WRITE]))]
          (.write out (.getBytes ^String (receipt-source receipt) "UTF-8")))
        (validate-receipt! (edn/read-string (slurp staged)))
        staged
        (catch Exception e
          (.delete staged)
          (throw e))))))

(defn- publish-staged-receipt!
  [staged receipt-path]
  (Files/move (.toPath staged)
              (.toPath (io/file receipt-path))
              (into-array CopyOption
                          [StandardCopyOption/ATOMIC_MOVE
                           StandardCopyOption/REPLACE_EXISTING])))

(defn- terminal-observed-effects
  [point legacy-result]
  (case point
    (:compile :authority) [:source-read]
    :receipt-stage [:source-read :receipt-stage]
    :commit (if (= :source-hash-mismatch (:error-type legacy-result))
              [:source-read :receipt-stage]
              [:source-read :receipt-stage :source-write :rollback])
    :receipt-publish [:source-read :receipt-stage :source-write
                      :receipt-publish :rollback]
    :success [:source-read :receipt-stage :source-write :receipt-publish]
    [:source-read]))

(defn- observe-change-result
  [point capabilities compiled receipt-facts legacy-result]
  (let [counts (select-keys compiled
                            [:intent-count :match-count
                             :change-count :changed-file-count])
        files (when (:ok compiled)
                ;; @spec MCP-OP-EDIT-031
                (into (mapv #(select-keys % [:file :source-hash :result-hash])
                            (changed-file-plans compiled))
                      (mapv (fn [{:keys [file result-hash]}]
                              {:file file
                               :result-hash result-hash
                               :absent-before true})
                            (:created-files compiled))))]
    (operation-algebra/observe-change-terminal
      {:point point
       :capabilities capabilities
       :compiled-facts (cond-> {:files files}
                         (seq counts) (assoc :counts counts))
       :receipt-facts receipt-facts
       :observed-effects (terminal-observed-effects point legacy-result)}
      legacy-result)))

(def ^:private cli-change-context
  {:operation :change
   :operation-version 1
   :entrance :cli
   :policy :cli-legacy
   :lifecycle :commit})

(def ^:private mcp-change-context
  {:operation :change
   :operation-version 1
   :entrance :mcp
   :policy :mcp-strict
   :lifecycle :commit})

(defn- compile-change-spec
  [context spec prepare-spec write-refusal-context]
  (validate-spec! spec)
  (let [canonical-spec (canonicalize-spec spec)
        sources (read-sources (spec-files canonical-spec))
        prepared (if prepare-spec
                   (prepare-spec sources canonical-spec)
                   {:ok true :spec canonical-spec})
        _ (when (:error prepared)
            (refuse! (:error-type prepared)
                     (:error prepared)
                     (dissoc prepared :error :error-type)))
        prepared-spec (:spec prepared)
        _ (validate-spec! prepared-spec)
        algebra-result
        (operation-algebra/compile-change
          (operation-algebra/change-entry
            #(compile-transaction
               %1 %2 {:write-refusal-context write-refusal-context}))
          context
          sources
          prepared-spec)]
    {:spec prepared-spec
     :compiled (cond-> (:compiled algebra-result)
                 (:location-normalization prepared)
                 (assoc :location-normalization
                        (:location-normalization prepared)))
     :capabilities (:capabilities algebra-result)
     :authority-error (when (:error algebra-result) algebra-result)}))

(defn execute-change-with-context!
  ;; @spec OP-ALG-COMMIT-001,
  ;; @spec OP-ALG-COMMIT-002,
  ;; @spec OP-ALG-CONTEXT-001,
  ;; @spec OP-ALG-CONTEXT-002,
  ;; @spec OP-ALG-IDENTITY-001,
  ;; @spec OP-ALG-RECEIPT-003,
  ;; @spec OP-ALG-RUNTIME-001
  "Compile, commit, verify, and publish one durable inverse receipt."
  [context
   {:keys [spec receipt-out prepare-compiled! prepare-spec
           write-refusal-context expect-matched on-write-boundary]
    :as opts}]
  (try
    (let [unknown (vec (sort (remove #{:op :spec :receipt-out :prepare-compiled! :prepare-spec
                                       :write-refusal-context :expect-matched
                                       :on-write-boundary}
                                     (keys opts))))]
      (when (seq unknown)
        (refuse! :unknown-arguments
                 (str "Unknown :change! arguments: " (str/join ", " unknown))
                 {:unknown unknown})))
    (when-not (map? spec)
      (refuse! :invalid-transaction-spec ":spec must be an EDN map"))
    (let [receipt-path (canonical-receipt-path receipt-out)
          {:keys [spec compiled capabilities authority-error]} (compile-change-spec
                                                                 context spec prepare-spec
                                                                 write-refusal-context)
          compiled (if (and (nil? (:error compiled)) prepare-compiled!)
                     (prepare-compiled! compiled)
                     compiled)
          ;; @spec MCP-OP-MATCHED-001
          ;; Computed once, against this transaction's own frozen pre-image,
          ;; before any effect is authorized.
          matched-basis (when (and expect-matched (nil? (:error compiled)))
                          (matched-basis-evidence compiled expect-matched))]
      (assert-receipt-does-not-alias-source! receipt-path spec)
      (cond
        authority-error
        (observe-change-result
          :authority capabilities compiled nil authority-error)

        (:error compiled)
        (observe-change-result
          :compile capabilities compiled nil
          (assoc compiled :phase :compile :source-unchanged true))

        ;; @spec MCP-OP-MATCHED-002
        ;; @spec MCP-OP-MATCHED-003
        (:error-type matched-basis)
        (observe-change-result
          :compile capabilities compiled nil
          (assoc matched-basis :phase :compile :source-unchanged true))

        :else
        (let [matched-evidence (:evidence matched-basis)
              authorization
              (operation-algebra/authorize-effects
                capabilities
                #{:source-write
                  :receipt-stage
                  :receipt-publish
                  :rollback})]
          (if (:error authorization)
            (observe-change-result
              :authority capabilities compiled nil authorization)
            (let [receipt (build-receipt compiled)
                  staged-result
                  (try
                    {:staged (stage-receipt! receipt-path receipt)}
                    (catch clojure.lang.ExceptionInfo e
                      {:error-result
                       (merge {:error (.getMessage e)} (ex-data e))})
                    (catch Exception e
                      {:error-result
                       {:error (.getMessage e)
                        :error-type :transaction-write-exception}}))]
              (if-let [stage-error (:error-result staged-result)]
                (observe-change-result
                  :receipt-stage capabilities compiled nil stage-error)
                (let [staged (:staged staged-result)]
                  (try
                    (let [commit (binding [*on-write-boundary* on-write-boundary]
                                   (commit-compiled! compiled))]
                      (if (:error commit)
                        (observe-change-result
                          :commit capabilities compiled nil commit)
                        (try
                          (publish-staged-receipt! staged receipt-path)
                          (let [published (edn/read-string (slurp receipt-path))]
                            (validate-receipt! published)
                            (let [result
                                  (merge
                                    commit
                                    (cond->
                                      {:receipt-file receipt-path
                                       :receipt-hash (:receipt-hash receipt)
                                       :intent-count (:intent-count compiled)
                                       :match-count (:match-count compiled)
                                       :inverse (:inverse receipt)}
                                      (:change-count compiled)
                                      (assoc :change-count
                                             (:change-count compiled))

                                      (:format compiled)
                                      (assoc :format (:format compiled))

                                      (:location-normalization compiled)
                                      (assoc :location-normalization
                                             (:location-normalization compiled))

                                      (:canonical-effect-identity compiled)
                                      (assoc :canonical-effect-identity
                                             (:canonical-effect-identity compiled))

                                      ;; @spec MCP-OP-MATCHED-001
                                      matched-evidence
                                      (assoc :matched-evidence
                                             matched-evidence)))]
                              (observe-change-result
                                :success capabilities compiled
                                {:path receipt-path
                                 :hash (:receipt-hash receipt)}
                                result)))
                          (catch Exception publish-error
                            (let [;; @spec MCP-OP-EDIT-034
                                  ;; Creations are current sources during a
                                  ;; publication rollback, just as edited
                                  ;; future sources are current sources.
                                  rollback-sources
                                  (into (:future-sources compiled)
                                        (map (fn [{:keys [file]}]
                                               [file
                                                (try
                                                  (slurp file)
                                                  (catch Exception _ nil))]))
                                        (:created-files compiled))
                                  inverse
                                  (compile-inverse
                                    receipt rollback-sources)
                                  rollback (if (:ok inverse)
                                             (commit-compiled! inverse)
                                             inverse)
                                  result
                                  {:error (if (:ok rollback)
                                            "Receipt publication failed; all files restored"
                                            "Receipt publication failed; manual recovery required")
                                   :error-type
                                   (if (:ok rollback)
                                     :receipt-write-failed
                                     :transaction-recovery-required)
                                   :cause-error (.getMessage publish-error)
                                   :rolled-back (boolean (:ok rollback))
                                   :recovery rollback}]
                              (observe-change-result
                                :receipt-publish capabilities compiled
                                {:path receipt-path
                                 :hash (:receipt-hash receipt)}
                                result))))))
                    (finally
                      (when (.exists staged) (.delete staged)))))))))))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e) :error-type :transaction-write-exception})))

(defn execute-change!
  "Execute one CLI-legacy change transaction."
  [opts]
  (execute-change-with-context! cli-change-context opts))

(defn execute-mcp-change!
  "Execute one MCP-strict change transaction."
  [opts]
  (execute-change-with-context! mcp-change-context opts))

(defn execute-undo!
  "Apply the hash-fenced inverse from a durable :change! receipt."
  [{:keys [receipt] :as opts}]
  (try
    (let [unknown (vec (sort (remove #{:op :receipt} (keys opts))))]
      (when (seq unknown)
        (refuse! :unknown-arguments
                 (str "Unknown :undo-change! arguments: "
                      (str/join ", " unknown))
                 {:unknown unknown})))
    (let [receipt-path (canonical-receipt-path receipt)
          saved (try
                  (edn/read-string (slurp receipt-path))
                  (catch Exception e
                    (invalid-receipt!
                      (str "Cannot read transaction receipt: " (.getMessage e))
                      {:receipt receipt-path})))
          _ (validate-receipt! saved)
          files (mapv :file (:files saved))
          created (mapv :file (:created-files saved))
          sources (read-sources (into files created))
          inverse (compile-inverse saved sources)]
      (if (:error inverse)
        inverse
        (let [commit (commit-compiled! inverse)]
          (if (:error commit)
            commit
            (-> commit
                (assoc :operation :undo-change!
                       :receipt-file receipt-path
                       :receipt-hash (:receipt-hash saved)
                       :restored-original-hashes
                       (into {} (map (juxt :file :result-hash)
                                     (:files inverse)))))))))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e) :error-type :invalid-transaction-receipt})))
