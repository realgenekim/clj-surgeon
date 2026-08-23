(ns clj-surgeon.intent-transaction
  (:require
   [clj-surgeon.binding-rename :as binding-rename]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]
   [rewrite-clj.zip :as z])
  (:import
   (java.nio.file CopyOption Files StandardCopyOption)))

(def transaction-version 1)
(def receipt-version 1)

(def ^:private supported-extensions [".clj" ".cljs" ".cljc"])
(def ^:private spec-keys #{:intents :changes :expect})
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
               (str "Unsupported Clojure source file: " (pr-str file))
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

(defn- defmethod-dispatch
  [record]
  (when (= 'defmethod (:type record))
    (some-> (:source record)
            z/of-string
            z/down
            z/right
            z/right
            z/sexpr)))

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
                       (= #{:kind :name} (set (keys owner)))
                       (= :namespace (:kind owner))
                       (symbol? (:name owner)))
          (refuse! :invalid-change-owner
                   "Change :owner must be {:kind :namespace :name <symbol>}"
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
        has-changes? (contains? spec :changes)]
    (when (and has-intents? has-changes?)
      (refuse! :mixed-transaction-modes
               "Use either :intents or :changes, not both"))
    (if has-changes?
      {:mode :changes
       :intents (validate-changes! (:changes spec))
       :expect (validate-change-aggregate-expectation! (:expect spec))}
      (do
        (when-not (and (vector? (:intents spec)) (seq (:intents spec)))
          (refuse! :invalid-intents
                   "Spec :intents must be a non-empty vector"))
        {:mode :intents
         :intents (mapv validate-intent! (:intents spec) (range))
         :expect (validate-aggregate-expectation! (:expect spec))}))))

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
                                   (when (= owner-name namespace-name)
                                     (assoc record :name namespace-name)))))
                         vec)]
        (when-not (= 1 (count matches))
          (refuse! :change-owner-mismatch
                   (str "Namespace owner " owner-name " in " file
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
                  target-owner (assoc :insert-side insert-side
                                      :insert-sources insert-sources))))
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
  [sources intent]
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
                 {:intent-index (:intent-index intent)
                  :change-id (:id intent)
                  :expected-count (:expect-count intent)
                  :actual-count actual-count
                  :per-file-counts per-file-counts}))
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
  (<= (:address-preorder right) (:end-preorder left)))

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

(defn- insertion-gap
  [source target side edit]
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
          opening-boundary (if top-level?
                             parent-start
                             (+ parent-start
                                (if (str/starts-with? parent-source "#{") 2 1)))
          closing-boundary (if top-level? parent-end (dec parent-end))
          neighbor (if (= :insert-left side) (z/left target) (z/right target))
          neighbor-offsets (when neighbor (node-offsets source neighbor))
          [gap-start gap-end]
          (if (= :insert-left side)
            [(or (:end neighbor-offsets) opening-boundary)
             target-start]
            [target-end
             (or (:start neighbor-offsets) closing-boundary)])
          gap (subs source gap-start gap-end)]
      (when-not (re-matches #"[\s,]*" gap)
        (refuse! :ambiguous-insertion-gap
                 "The sibling gap contains comments or detached source"
                 {:change-id (:change-id edit)
                  :file (:file edit)
                  :target (:before edit)
                  :gap gap
                  :remedy "Replace a larger exact span that declares comment placement."}))
      (if (seq gap) gap " "))))

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
      (let [{:keys [start end]} (node-offsets source target)
            gap (insertion-gap source target insert-side edit)
            inserted (str/join gap insert-sources)
            offset (if (= :insert-left insert-side) start end)
            insertion (if (= :insert-left insert-side)
                        (str inserted gap)
                        (str gap inserted))]
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
        effective-edits (if (some #(or (:delete %) (:insert-side %))
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
  [sources spec]
  (let [{:keys [mode intents expect]} (validate-spec! spec)
        files (ordered-scoped-files intents)
        _ (doseq [file files]
            (validate-complete-source! file (get sources file) :invalid-source))
        compiled-intents (mapv #(compile-intent-edits sources %) intents)
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
             :changes (mapv #(dissoc % :edits) compiled-intents)))))

(defn compile-transaction
  "Compile explicit exact structural intents against an in-memory file map.
   Returns a complete future state or one structured refusal. Performs no I/O."
  [sources spec]
  (try
    (when-not (map? sources)
      (refuse! :invalid-sources "Sources must be a map of file path to source"))
    (compile-transaction* sources spec)
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e)
       :error-type :intent-compiler-failure})))

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
      (public-plan (compile-transaction sources canonical-spec)))
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

(defn- recovered?
  [recovery]
  (every? #(#{:original :restored} (:status %)) recovery))

(defn- execute-writes!
  [read-source write-source! futures file-plans]
  (doseq [{:keys [file source-hash result-hash]} file-plans]
    ;; Recheck immediately before each replacement. If a later file goes stale
    ;; after an earlier write, the caller enters the same recovery protocol.
    (assert-file-hash! read-source file source-hash :source-hash-mismatch)
    (write-source! file (get futures file))
    (assert-file-hash! read-source file result-hash :read-back-hash-mismatch)))

(defn- verified-hashes
  [read-source file-plans]
  (into {}
        (map (fn [{:keys [file result-hash]}]
               (assert-file-hash! read-source file result-hash
                                  :read-back-hash-mismatch)
               [file result-hash]))
        file-plans))

(defn commit-compiled!
  "Commit a successfully compiled transaction through injected source I/O.
   Ordinary handled failures restore files that still equal either the original
   or transaction result. Unexpected bytes are never overwritten."
  ([compiled]
   (commit-compiled! compiled
                     {:read-source slurp
                      :write-source! file-ops/atomic-write!}))
  ([compiled {:keys [read-source write-source!]}]
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
           futures (:future-sources compiled)]
       ;; The all-file preflight is outside the recovery block because it has
       ;; not written anything.
       (doseq [{:keys [file source-hash]} file-plans]
         (assert-file-hash! read-source file source-hash
                            :source-hash-mismatch))
       (try
         (execute-writes! read-source write-source! futures file-plans)
         (let [hashes (verified-hashes read-source file-plans)]
           {:ok true
            :operation :change!
            :transaction-version transaction-version
            :committed true
            :changed-file-count (count file-plans)
            :verified {:whole-files true
                       :file-count (count file-plans)
                       :read-back-hashes hashes}})
         (catch Exception cause
           (let [recovery (recover-transaction!
                            read-source write-source! originals file-plans)
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
        :error-type :transaction-write-exception}))))

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
                  (assoc :match-count-kind :binding-occurrences))]
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
  (when-not (and (vector? (:files receipt)) (seq (:files receipt)))
    (invalid-receipt! "Receipt :files must be a non-empty vector"))
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
        _ (when (and logical-match-count? (not inverse-edit-count?))
            (invalid-receipt!
              "Receipt binding-occurrence evidence requires an inverse edit count"))
        _ (when (and inverse-edit-count?
                     (not (pos-int? (:inverse-edit-count receipt))))
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
          files (mapv :file compiled-files)]
      {:ok true
       :operation :undo-change!
       :transaction-version transaction-version
       :intent-count (:intent-count receipt)
       :match-count (:match-count receipt)
       :changed-file-count (count files)
       :files (mapv #(dissoc % :result-source) compiled-files)
       :original-sources (select-keys sources files)
       :future-sources (into {} (map (juxt :file :result-source) compiled-files))
       :validated {:whole-files-parsed true :file-count (count files)}})
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
  (when (some #{receipt-path} (spec-files spec))
    (refuse! :invalid-receipt-path
             "Receipt path must not alias a source file"
             {:path receipt-path})))

(defn- receipt-source
  [receipt]
  (str (pr-str receipt) "\n"))

(defn- stage-receipt!
  [receipt-path receipt]
  (let [target (io/file receipt-path)
        parent (.getParentFile (.getAbsoluteFile target))]
    (when-not (and parent (.exists parent) (.isDirectory parent))
      (refuse! :invalid-receipt-path
               "Receipt parent directory does not exist"
               {:path receipt-path}))
    (let [staged (java.io.File/createTempFile
                   ".clj-surgeon-receipt-" ".edn" parent)]
      (try
        (spit staged (receipt-source receipt))
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

(defn- compile-change-spec
  [spec]
  (validate-spec! spec)
  (let [canonical-spec (canonicalize-spec spec)
        sources (read-sources (spec-files canonical-spec))]
    {:spec canonical-spec
     :compiled (compile-transaction sources canonical-spec)}))

(defn execute-change!
  "Compile, commit, verify, and publish one durable inverse receipt."
  [{:keys [spec receipt-out prepare-compiled!] :as opts}]
  (try
    (let [unknown (vec (sort (remove #{:op :spec :receipt-out :prepare-compiled!}
                                     (keys opts))))]
      (when (seq unknown)
        (refuse! :unknown-arguments
                 (str "Unknown :change! arguments: " (str/join ", " unknown))
                 {:unknown unknown})))
    (when-not (map? spec)
      (refuse! :invalid-transaction-spec ":spec must be an EDN map"))
    (let [receipt-path (canonical-receipt-path receipt-out)
          {:keys [spec compiled]} (compile-change-spec spec)
          compiled (if (and (nil? (:error compiled)) prepare-compiled!)
                     (prepare-compiled! compiled)
                     compiled)]
      (assert-receipt-does-not-alias-source! receipt-path spec)
      (if (:error compiled)
        (assoc compiled :phase :compile :source-unchanged true)
        (let [receipt (build-receipt compiled)
              staged (stage-receipt! receipt-path receipt)]
          (try
            (let [commit (commit-compiled! compiled)]
              (if (:error commit)
                commit
                (try
                  (publish-staged-receipt! staged receipt-path)
                  (let [published (edn/read-string (slurp receipt-path))]
                    (validate-receipt! published)
                    (merge commit
                           (cond-> {:receipt-file receipt-path
                                    :receipt-hash (:receipt-hash receipt)
                                    :intent-count (:intent-count compiled)
                                    :match-count (:match-count compiled)
                                    :inverse (:inverse receipt)}
                             (:change-count compiled)
                             (assoc :change-count (:change-count compiled))

                             (:format compiled)
                             (assoc :format (:format compiled)))))
                  (catch Exception publish-error
                    (let [inverse (compile-inverse
                                    receipt (:future-sources compiled))
                          rollback (if (:ok inverse)
                                     (commit-compiled! inverse)
                                     inverse)]
                      {:error (if (:ok rollback)
                                "Receipt publication failed; all files restored"
                                "Receipt publication failed; manual recovery required")
                       :error-type (if (:ok rollback)
                                     :receipt-write-failed
                                     :transaction-recovery-required)
                       :cause-error (.getMessage publish-error)
                       :rolled-back (boolean (:ok rollback))
                       :recovery rollback})))))
            (finally
              (when (.exists staged) (.delete staged)))))))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e) :error-type :transaction-write-exception})))

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
          sources (read-sources files)
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
