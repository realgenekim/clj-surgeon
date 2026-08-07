(ns clj-surgeon.show-form
  "Read one or more exact top-level forms from one source snapshot."
  (:require
   [clj-surgeon.forms :as forms]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.zip :as z]))

(def ^:private max-candidate-locations 10)
(def ^:private max-batch-forms 50)
(def ^:private max-batch-files 20)
(def ^:private max-batch-source-chars 65536)

(defn read-source
  "Read one source snapshot. Public so the I/O contract can be instrumented."
  [file]
  (slurp file))

(defn- shell-quote
  [value]
  (let [s (str value)]
    (if (re-matches #"[A-Za-z0-9_./:+=-]+" s)
      s
      (str "'" (str/replace s "'" "'\"'\"'") "'"))))

(defn render-command
  "Render command argument data as one shell-safe command string."
  [args]
  (str/join " " (map shell-quote args)))

(defn- selector-keys
  [opts]
  (->> [:form :forms :line :contains]
       (filter #(contains? opts %))
       vec))

(defn- normalize-cli-line
  [line]
  (if (and (string? line) (re-matches #"[0-9]+" line))
    (try
      (Long/parseLong line)
      (catch NumberFormatException _ line))
    line))

(defn- normalize-form
  [form]
  (cond
    (and (symbol? form) (nil? (namespace form))) form
    (and (string? form)
         (not (str/blank? form))
         (not (str/starts-with? form ":"))
         (or (= "/" form)
             (re-matches #"[^\s/]+" form)))
    (symbol form)
    :else nil))

(defn- normalize-forms
  [forms]
  (when (vector? forms)
    (mapv normalize-form forms)))

(defn- normalized-selector
  [{:keys [form forms line contains platform] :as opts}]
  (cond-> {}
    (contains? opts :form) (assoc :form (or (normalize-form form) form))
    (contains? opts :forms) (assoc :forms
                                   (if (vector? forms)
                                     (mapv #(or (normalize-form %) %) forms)
                                     forms))
    (contains? opts :line) (assoc :line line)
    (contains? opts :contains) (assoc :contains contains)
    (contains? opts :platform) (assoc :platform platform)))

(defn- command-args
  [{:keys [file form forms line contains platform] :as opts}]
  (cond-> ["clj-surgeon" ":op" ":cat" ":file" (str file)]
    (contains? opts :form) (into [":form" (str form)])
    (contains? opts :forms) (into [":forms" (pr-str forms)])
    (contains? opts :line) (into [":line" (str line)])
    (contains? opts :contains) (into [":contains" (str contains)])
    (contains? opts :platform) (into [":platform" (str platform)])))

(defn invocation-remedy
  "Return an executable cat remedy when explicit arguments identify exactly
   one supported read selector. Treat :name as a recoverable :form spelling.
   This pure function returns nil when intent is not exact."
  [{:keys [file] :as supplied-opts}]
  (let [opts (if (and (contains? supplied-opts :name)
                      (not-any? #(contains? supplied-opts %)
                                [:form :forms :line :contains]))
               (-> supplied-opts
                   (assoc :form (:name supplied-opts))
                   (dissoc :name))
               supplied-opts)
        form? (contains? opts :form)
        forms? (contains? opts :forms)
        line? (contains? opts :line)
        contains-selector? (contains? opts :contains)
        normalized-forms (normalize-forms (:forms opts))
        normalized-line (normalize-cli-line (:line opts))]
    (when (and file
               (= 1 (count (selector-keys opts)))
               (or (and form? (some? (normalize-form (:form opts))))
                   (and forms?
                        (seq normalized-forms)
                        (every? some? normalized-forms)
                        (= (count normalized-forms)
                           (count (distinct normalized-forms))))
                   (and line? (integer? normalized-line)
                        (pos? normalized-line))
                   (and contains-selector? (string? (:contains opts))
                        (not (str/blank? (:contains opts)))))
               (or (not (contains? opts :platform))
                   (keyword? (:platform opts))))
      (let [args (command-args opts)
            reason (cond
                     form? "Read one named top-level form"
                     forms? "Read several named top-level forms from one snapshot"
                     line? "Read the top-level form containing one line"
                     :else "Read the one top-level form containing literal text")]
        {:operation :cat
         :reason reason
         :command (render-command args)
         :command-args args}))))

(defn- list-forms-remedy
  [file]
  (let [args ["clj-surgeon" ":op" ":ls" ":file" (str file)]]
    {:operation :ls
     :reason "List available top-level forms and line ranges"
     :command (render-command args)
     :command-args args}))

(defn refusal-context
  "Canonical operation context for dispatch-level show-form refusals."
  [{:keys [file] :as opts}]
  {:operation :show-form
   :file file
   :selector (normalized-selector opts)})

(defn- base-result
  [source opts]
  (assoc (refusal-context opts)
         :source-hash (structural-lens/source-hash source)))

(defn- error-result
  [base error-type message fields]
  (merge base
         {:error message
          :error-type error-type}
         fields))

(defn- platform-match?
  [platform record]
  (or (nil? platform)
      (some #{platform} (:platforms record))))

(defn- line-match?
  [line record]
  (let [start (or (:comment-start record) (:line record))]
    (and start (:end-line record)
         (<= start line (:end-line record)))))

(defn- candidate-location
  [record]
  (select-keys record [:type :name :platforms :line :end-line :comment-start
                       :occurrence-count]))

(def ^:private selected-record-keys
  [:type :name :platforms :line :end-line :comment-start :source])

(defn- duplicate-forms
  [forms]
  (let [counts (frequencies forms)]
    (->> forms
         (filter #(> (get counts %) 1))
         distinct
         vec)))

(defn literal-occurrences
  "Return one-based line/column locations for a nonblank literal in source.
   Pure: source text, its one-based starting line, and literal in; data out."
  [source start-line literal]
  (if (or (not (string? literal)) (str/blank? literal))
    []
    (loop [from-index 0
           locations []]
      (if-let [offset (str/index-of source literal from-index)]
        (let [prefix (subs source 0 offset)
              newline-count (count (filter #(= \newline %) prefix))
              last-newline (str/last-index-of prefix "\n")
              column (if last-newline
                       (- offset last-newline)
                       (inc offset))]
          (recur (inc offset)
                 (conj locations {:line (+ start-line newline-count)
                                  :column column})))
        locations))))

(defn- owned-source
  [source record]
  (if-let [comment-start (:comment-start record)]
    (let [lines (str/split source #"\n" -1)
          comment-lines (subvec lines (dec comment-start) (dec (:line record)))]
      (str (str/join "\n" comment-lines) "\n" (:source record)))
    (:source record)))

(defn- with-literal-occurrences
  [source record literal]
  (let [locations (literal-occurrences
                    (owned-source source record)
                    (or (:comment-start record) (:line record))
                    literal)]
    (when (seq locations)
      (assoc record
             :occurrence-count (count locations)
             :occurrences locations))))

(defn- platform-remedy
  [matches]
  (let [platforms (->> matches (mapcat :platforms) distinct sort vec)]
    (when (> (count platforms) 1)
      {:select-platform
       {:reason "Select one reader-conditional platform"
        :platforms platforms}})))

(defn- batch-form-failure
  [form matches]
  (if (empty? matches)
    {:form form
     :error-type :form-not-found
     :match-count 0}
    (cond-> {:form form
             :error-type :ambiguous-form
             :match-count (count matches)
             :candidate-limit max-candidate-locations
             :matches-truncated? (> (count matches) max-candidate-locations)
             :matches (->> matches
                           (take max-candidate-locations)
                           (mapv candidate-location))}
      (platform-remedy matches) (assoc :remedies (platform-remedy matches)))))

(defn- select-named-forms
  [base file records requested-forms]
  (let [matches-by-name (group-by :name records)
        selections (mapv (fn [form]
                           {:form form
                            :matches (get matches-by-name form [])})
                         requested-forms)
        failures (->> selections
                      (keep (fn [{:keys [form matches]}]
                              (when (not= 1 (count matches))
                                (batch-form-failure form matches))))
                      vec)]
    (if (seq failures)
      (error-result
        base
        :batch-form-selection-failed
        "One or more requested top-level forms could not be selected exactly"
        {:requested-form-count (count requested-forms)
         :resolved-form-count (- (count requested-forms) (count failures))
         :failure-count (count failures)
         :failures failures
         :remedies {:list-forms (list-forms-remedy file)}})
      (let [selected-forms (mapv (fn [{:keys [matches]}]
                                   (select-keys (first matches)
                                                selected-record-keys))
                                 selections)
            source-char-count (reduce + (map #(count (:source %))
                                             selected-forms))]
        (if (> source-char-count max-batch-source-chars)
          (error-result
            base
            :batch-source-limit-exceeded
            "Combined batch source exceeds the safe output limit"
            {:requested-form-count (count requested-forms)
             :source-char-count source-char-count
             :source-char-limit max-batch-source-chars
             :remedy "Request fewer forms in each batch"})
          (merge base
                 {:form-count (count selections)
                  :source-char-count source-char-count
                  :forms selected-forms}))))))

(defn select-form
  "Select one top-level form, or an ordered batch of named forms, from one
   source snapshot. Pure: filename, source string, and options in; EDN out."
  [file source {:keys [form forms line contains platform project-aliases] :as opts}]
  (let [base (base-result source (assoc opts :file file))
        selectors (selector-keys opts)
        normalized-form (normalize-form form)
        normalized-forms (normalize-forms forms)]
    (cond
      (empty? selectors)
      (error-result base :missing-selector
                    "Supply exactly one of :form, :forms, :line, or :contains"
                    {:required-one-of [:form :forms :line :contains]})

      (> (count selectors) 1)
      (error-result base :conflicting-selectors
                    "Supply only one of :form, :forms, :line, or :contains"
                    {:supplied-selectors selectors})

      (and (contains? opts :form) (nil? normalized-form))
      (error-result base :invalid-form-selector
                    ":form must be an unqualified symbol or nonblank string"
                    {:form form})

      (and (contains? opts :forms)
           (or (not (vector? forms))
               (empty? forms)
               (> (count forms) max-batch-forms)
               (some nil? normalized-forms)))
      (error-result base :invalid-forms-selector
                    (str ":forms must be a nonempty EDN vector of at most "
                         max-batch-forms " unqualified names")
                    {:forms forms
                     :max-form-count max-batch-forms
                     :invalid-entries
                     (when (vector? forms)
                       (->> normalized-forms
                            (map-indexed vector)
                            (keep (fn [[index normalized]]
                                    (when (nil? normalized)
                                      {:index index :value (nth forms index)})))
                            vec))})

      (and (contains? opts :forms)
           (seq (duplicate-forms normalized-forms)))
      (error-result base :duplicate-form-selectors
                    ":forms must not contain duplicate names"
                    {:duplicate-forms (duplicate-forms normalized-forms)})

      (and (contains? opts :line)
           (not (and (integer? line) (pos? line))))
      (error-result base :invalid-line
                    ":line must be a positive integer"
                    {:line line})

      (and (contains? opts :contains)
           (not (and (string? contains) (not (str/blank? contains)))))
      (error-result base :invalid-contains-selector
                    ":contains must be a nonblank literal string"
                    {:contains contains})

      (and (contains? opts :platform) (not (keyword? platform)))
      (error-result base :invalid-platform
                    ":platform must be a keyword such as :clj or :cljs"
                    {:platform platform})

      :else
      (try
        (let [contains-selector? (contains? opts :contains)
              records (outline/top-level-form-records
                        file source (or project-aliases {}))
              platform-records (->> records
                                    (filter #(platform-match? platform %))
                                    vec)
              matches (when-not (contains? opts :forms)
                        (->> platform-records
                             (keep (cond
                                     normalized-form
                                     #(when (= normalized-form (:name %)) %)

                                     contains-selector?
                                     #(with-literal-occurrences source % contains)

                                     :else
                                     #(when (line-match? line %) %)))
                             vec))]
          (if (contains? opts :forms)
            (select-named-forms base file platform-records normalized-forms)
            (cond
              (= 1 (count matches))
              (merge base
                     (select-keys (first matches)
                                  (into selected-record-keys
                                        [:occurrence-count :occurrences])))

              (empty? matches)
              (let [by-name? (some? normalized-form)
                    error-type (cond
                                 by-name? :form-not-found
                                 contains-selector? :contains-not-found
                                 :else :line-not-in-form)
                    message (cond
                              by-name? (str "Top-level form not found: " normalized-form)
                              contains-selector?
                              (str "Literal text not found in a top-level form: "
                                   (pr-str contains))
                              :else (str "Line " line
                                         " is not contained by a top-level form"))]
                (error-result
                  base
                  error-type
                  message
                  {:match-count 0
                   :remedies {:list-forms (list-forms-remedy file)}}))

              :else
              (error-result
                base :ambiguous-form
                (str "Selector matched " (count matches) " top-level forms")
                (cond-> {:match-count (count matches)
                         :candidate-limit max-candidate-locations
                         :matches-truncated? (> (count matches)
                                                max-candidate-locations)
                         :matches (->> matches
                                       (take max-candidate-locations)
                                       (mapv candidate-location))}
                  (platform-remedy matches) (assoc :remedies
                                                   (platform-remedy matches)))))))
        (catch Exception e
          (error-result base :invalid-source (.getMessage e) {}))))))

(defn show-file
  "Thin file I/O wrapper over `select-form`."
  [{:keys [file] :as opts}]
  (let [opts (cond-> opts
               (contains? opts :line) (update :line normalize-cli-line)
               true (assoc :project-aliases @forms/project-aliases))]
    (try
      (select-form file (read-source file) opts)
      (catch Exception e
        {:operation :show-form
         :file file
         :selector (normalized-selector opts)
         :error (str "Cannot read source file: " (.getMessage e))
         :error-type :file-read-failed}))))

(def ^:private read-spec-keys #{:reads :expect :limits})
(def ^:private read-entry-keys #{:file :forms :platform})
(def ^:private read-expect-keys #{:file-count :form-count})
(def ^:private read-limit-keys #{:source-chars})
(def ^:private direct-read-keys #{:file :form :forms :line :contains :platform})
(def ^:private supported-output-formats #{:edn :semantic})

(defn- unknown-keys [value allowed]
  (when (map? value)
    (->> (keys value) (remove allowed) (sort-by pr-str) vec)))

(defn- spec-error [error-type message fields]
  (error-result {:operation :show-form
                 :selector {:spec :cross-file}}
                error-type message fields))

(defn- validate-read-spec
  [spec]
  (let [reads (:reads spec)
        expect (:expect spec)
        limits (:limits spec)
        root-unknown (unknown-keys spec read-spec-keys)
        expect-unknown (unknown-keys expect read-expect-keys)
        limit-unknown (unknown-keys limits read-limit-keys)
        entry-failures
        (when (vector? reads)
          (->> reads
               (map-indexed
                 (fn [index read]
                   (let [unknown (unknown-keys read read-entry-keys)]
                     (cond
                       (not (map? read))
                       {:index index :error-type :invalid-read-entry}

                       (seq unknown)
                       {:index index :error-type :unknown-read-entry-keys
                        :unknown-keys unknown}

                       (not (and (string? (:file read))
                                 (not (str/blank? (:file read)))))
                       {:index index :error-type :invalid-read-file
                        :file (:file read)}

                       (not (vector? (:forms read)))
                       {:index index :error-type :invalid-read-forms
                        :file (:file read)}

                       :else
                       (let [probe (select-form (:file read) "(ns probe)"
                                                (select-keys read [:forms :platform]))]
                         (when (#{:invalid-forms-selector
                                  :duplicate-form-selectors
                                  :invalid-platform}
                                (:error-type probe))
                           {:index index :file (:file read)
                            :error-type (:error-type probe)
                            :error (:error probe)}))))))
               (remove nil?)
               vec))
        canonical-files
        (when (and (vector? reads) (empty? entry-failures))
          (mapv #(-> (:file %) io/file .getCanonicalPath) reads))
        duplicates
        (when canonical-files
          (->> canonical-files frequencies
               (keep (fn [[file count]] (when (> count 1) file)))
               sort vec))
        requested-file-count (when (vector? reads) (count reads))
        requested-form-count (when (vector? reads)
                               (reduce + 0 (map #(count (:forms %)) reads)))
        source-limit (get limits :source-chars max-batch-source-chars)]
    (cond
      (not (map? spec))
      (spec-error :invalid-read-spec "Read spec must be an EDN map" {})

      (seq root-unknown)
      (spec-error :unknown-read-spec-keys "Read spec contains unknown keys"
                  {:unknown-keys root-unknown})

      (or (not (vector? reads)) (empty? reads) (> (count reads) max-batch-files))
      (spec-error :invalid-reads
                  (str ":reads must be a nonempty vector of at most "
                       max-batch-files " file reads")
                  {:max-file-count max-batch-files})

      (seq entry-failures)
      (spec-error :invalid-read-entries "One or more file reads are invalid"
                  {:failure-count (count entry-failures)
                   :failures entry-failures})

      (seq duplicates)
      (spec-error :duplicate-read-files
                  "Each physical file may appear only once in a read transaction"
                  {:duplicate-files duplicates})

      (not (map? expect))
      (spec-error :invalid-read-expectation
                  ":expect must declare exact :file-count and :form-count" {})

      (seq expect-unknown)
      (spec-error :unknown-read-expectation-keys
                  ":expect contains unknown keys"
                  {:unknown-keys expect-unknown})

      (not= #{:file-count :form-count} (set (keys expect)))
      (spec-error :invalid-read-expectation
                  ":expect must declare exactly :file-count and :form-count" {})

      (or (not (pos-int? (:file-count expect)))
          (not (pos-int? (:form-count expect))))
      (spec-error :invalid-read-expectation
                  "Expected file and form counts must be positive integers" {})

      (or (not= requested-file-count (:file-count expect))
          (not= requested-form-count (:form-count expect)))
      (spec-error :read-expectation-mismatch
                  "Declared read counts do not match the manifest"
                  {:expected expect
                   :actual {:file-count requested-file-count
                            :form-count requested-form-count}})

      (and (contains? spec :limits) (not (map? limits)))
      (spec-error :invalid-read-limits ":limits must be an EDN map" {})

      (seq limit-unknown)
      (spec-error :unknown-read-limit-keys ":limits contains unknown keys"
                  {:unknown-keys limit-unknown})

      (not (and (pos-int? source-limit)
                (<= source-limit max-batch-source-chars)))
      (spec-error :invalid-read-source-limit
                  (str ":limits :source-chars must be a positive integer no greater than "
                       max-batch-source-chars)
                  {:source-char-limit max-batch-source-chars})

      :else
      {:reads reads
       :expect expect
       :source-limit source-limit})))

(defn show-files
  "Read exact named forms across several files as one all-or-nothing result."
  [spec]
  (let [validated (validate-read-spec spec)]
    (if (:error validated)
      validated
      (let [{:keys [reads expect source-limit]} validated
            results (mapv (fn [{:keys [file] :as read}]
                            (try
                              (forms/init-from-file! file)
                              (show-file read)
                              (catch Exception exception
                                {:operation :show-form
                                 :file file
                                 :selector (select-keys read [:forms :platform])
                                 :error (str "Cannot initialize source file: "
                                             (.getMessage exception))
                                 :error-type :file-read-failed})))
                          reads)
            failures (->> results
                          (map-indexed
                            (fn [index result]
                              (when (:error result)
                                (merge {:index index :file (:file result)}
                                       (select-keys result
                                                    [:error :error-type
                                                     :requested-form-count
                                                     :resolved-form-count
                                                     :failure-count
                                                     :failures])))))
                          (remove nil?) vec)
            source-char-count (reduce + 0 (keep :source-char-count results))]
        (cond
          (seq failures)
          (spec-error :read-transaction-failed
                      "One or more file reads failed; no source is returned"
                      {:expected expect
                       :failure-count (count failures)
                       :failures failures})

          (> source-char-count source-limit)
          (spec-error :read-source-limit-exceeded
                      "Combined cross-file source exceeds the declared limit"
                      {:expected expect
                       :source-char-count source-char-count
                       :source-char-limit source-limit
                       :remedy "Request fewer or smaller forms"})

          :else
          {:operation :show-form
           :selector {:spec :cross-file}
           :file-count (:file-count expect)
           :form-count (:form-count expect)
           :source-char-count source-char-count
           :source-char-limit source-limit
           :files results})))))

(defn- semantic-form
  [form]
  (let [semantic-source (-> (:source form) z/of-string z/sexpr pr-str)]
    {:header {:name (:name form)
              :line (:line form)
              :end-line (:end-line form)}
     :source semantic-source}))

(defn- semantic-file
  [index file-result]
  {:header {:index index
            :file (:file file-result)
            :source-hash (:source-hash file-result)
            :form-count (:form-count file-result)}
   :forms (mapv semantic-form (:forms file-result))})

(defn render-semantic
  "Render a successful cross-file result as compact canonical Clojure data.
   Comments and layout are omitted; the complete-file hashes fence the source."
  [result]
  (try
    (let [files (mapv semantic-file (range 1 (inc (:file-count result)))
                      (:files result))
          semantic-char-count
          (reduce + 0 (for [file files form (:forms file)]
                        (count (:source form))))
          header {:version 1
                  :file-count (:file-count result)
                  :form-count (:form-count result)
                  :source-char-count (:source-char-count result)
                  :semantic-char-count semantic-char-count}
          output
          (with-out-str
            (println "CLJ-SURGEON-SEMANTIC" (pr-str header))
            (doseq [file files]
              (println "FILE"
                       (get-in file [:header :index])
                       (pr-str (get-in file [:header :file]))
                       (get-in file [:header :source-hash]))
              (doseq [[index form] (map-indexed vector (:forms file))]
                (println "FORM"
                         (inc index)
                         (pr-str (get-in form [:header :name]))
                         (str (get-in form [:header :line]) ":"
                              (get-in form [:header :end-line])))
                (println (:source form)))))]
      (if (> (count output) max-batch-source-chars)
        (spec-error :semantic-output-limit-exceeded
                    "Semantic read output exceeds the safe transcript limit"
                    {:semantic-output-char-count (count output)
                     :semantic-output-char-limit max-batch-source-chars
                     :remedy "Request fewer forms"})
        output))
    (catch Exception exception
      (spec-error :semantic-render-failed
                  "A selected form cannot be represented as canonical Clojure data"
                  {:reason (.getMessage exception)}))))

(defn show
  "Dispatch one-file selectors or one cross-file read manifest."
  [{:keys [spec format] :as opts}]
  (if (contains? opts :spec)
    (let [conflicts (->> direct-read-keys (filter #(contains? opts %)) sort vec)]
      (cond
        (seq conflicts)
        (spec-error :conflicting-read-inputs
                    "Do not combine :spec or :spec-file with direct read arguments"
                    {:conflicting-keys conflicts})

        (and (contains? opts :format)
             (not (supported-output-formats format)))
        (spec-error :invalid-read-format
                    ":format must be :edn or :semantic"
                    {:format format :supported-formats (sort supported-output-formats)})

        :else
        (let [result (show-files spec)]
          (if (and (= :semantic format) (not (:error result)))
            (render-semantic result)
            result))))
    (if (contains? opts :format)
      (spec-error :read-format-requires-spec
                  ":format is available only with :spec or :spec-file" {})
      (show-file opts))))
