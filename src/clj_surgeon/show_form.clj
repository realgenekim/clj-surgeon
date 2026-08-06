(ns clj-surgeon.show-form
  "Read one exact top-level form by name or containing line."
  (:require
   [clj-surgeon.forms :as forms]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str]))

(def ^:private max-candidate-locations 10)

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
  (->> [:form :line :contains]
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

(defn- normalized-selector
  [{:keys [form line contains platform] :as opts}]
  (cond-> {}
    (contains? opts :form) (assoc :form (or (normalize-form form) form))
    (contains? opts :line) (assoc :line line)
    (contains? opts :contains) (assoc :contains contains)
    (contains? opts :platform) (assoc :platform platform)))

(defn- command-args
  [{:keys [file form line contains platform] :as opts}]
  (cond-> ["clj-surgeon" ":op" ":cat" ":file" (str file)]
    (contains? opts :form) (into [":form" (str form)])
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
                                [:form :line :contains]))
               (-> supplied-opts
                   (assoc :form (:name supplied-opts))
                   (dissoc :name))
               supplied-opts)
        form? (contains? opts :form)
        line? (contains? opts :line)
        contains-selector? (contains? opts :contains)
        normalized-line (normalize-cli-line (:line opts))]
    (when (and file
               (= 1 (count (selector-keys opts)))
               (or (and form? (some? (normalize-form (:form opts))))
                   (and line? (integer? normalized-line)
                        (pos? normalized-line))
                   (and contains-selector? (string? (:contains opts))
                        (not (str/blank? (:contains opts)))))
               (or (not (contains? opts :platform))
                   (keyword? (:platform opts))))
      (let [args (command-args opts)
            reason (cond
                     form? "Read one named top-level form"
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

(defn select-form
  "Select one top-level form by unqualified name, containing line, or literal
   source text. Pure: filename, source string, and options in; EDN result out."
  [file source {:keys [form line contains platform project-aliases] :as opts}]
  (let [base (base-result source (assoc opts :file file))
        selectors (selector-keys opts)
        normalized-form (normalize-form form)]
    (cond
      (empty? selectors)
      (error-result base :missing-selector
                    "Supply exactly one of :form, :line, or :contains"
                    {:required-one-of [:form :line :contains]})

      (> (count selectors) 1)
      (error-result base :conflicting-selectors
                    "Supply only one of :form, :line, or :contains"
                    {:supplied-selectors selectors})

      (and (contains? opts :form) (nil? normalized-form))
      (error-result base :invalid-form-selector
                    ":form must be an unqualified symbol or nonblank string"
                    {:form form})

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
              matches (->> records
                           (filter #(platform-match? platform %))
                           (keep (cond
                                   normalized-form
                                   #(when (= normalized-form (:name %)) %)

                                   contains-selector?
                                   #(with-literal-occurrences source % contains)

                                   :else
                                   #(when (line-match? line %) %)))
                           vec)]
          (cond
            (= 1 (count matches))
            (merge base
                   (select-keys (first matches)
                                [:type :name :platforms :line :end-line
                                 :comment-start :source :occurrence-count
                                 :occurrences]))

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
                                                 (platform-remedy matches))))))
        (catch Exception e
          (error-result base :invalid-source (.getMessage e) {}))))))

(defn show-file
  "Thin file I/O wrapper over `select-form`."
  [{:keys [file] :as opts}]
  (let [opts (cond-> opts
               (contains? opts :line) (update :line normalize-cli-line)
               true (assoc :project-aliases @forms/project-aliases))]
    (try
      (select-form file (slurp file) opts)
      (catch Exception e
        {:operation :show-form
         :file file
         :selector (normalized-selector opts)
         :error (str "Cannot read source file: " (.getMessage e))
         :error-type :file-read-failed}))))
