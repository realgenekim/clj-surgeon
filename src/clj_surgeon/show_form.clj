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

(defn- render-command
  [args]
  (str/join " " (map shell-quote args)))

(defn- selector-keys
  [opts]
  (->> [:form :line]
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
  [{:keys [form line platform] :as opts}]
  (cond-> {}
    (contains? opts :form) (assoc :form (or (normalize-form form) form))
    (contains? opts :line) (assoc :line line)
    (contains? opts :platform) (assoc :platform platform)))

(defn- command-args
  [{:keys [file form line platform] :as opts}]
  (cond-> ["clj-surgeon" ":op" ":show-form" ":file" (str file)]
    (contains? opts :form) (into [":form" (str form)])
    (contains? opts :line) (into [":line" (str line)])
    (contains? opts :platform) (into [":platform" (str platform)])))

(defn invocation-remedy
  "Return an executable show-form remedy when explicit arguments identify
   exactly one supported read selector. Pure; nil when intent is not exact."
  [{:keys [file] :as opts}]
  (let [form? (contains? opts :form)
        line? (contains? opts :line)
        normalized-line (normalize-cli-line (:line opts))]
    (when (and file
               (= 1 (count (selector-keys opts)))
               (or (and form? (some? (normalize-form (:form opts))))
                   (and line? (integer? normalized-line)
                        (pos? normalized-line)))
               (or (not (contains? opts :platform))
                   (keyword? (:platform opts))))
      (let [args (command-args opts)
            by-form? form?]
        {:operation :show-form
         :reason (if by-form?
                   "Read one named top-level form"
                   "Read the top-level form containing one line")
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
  (select-keys record [:type :name :platforms :line :end-line :comment-start]))

(defn- platform-remedy
  [matches]
  (let [platforms (->> matches (mapcat :platforms) distinct sort vec)]
    (when (> (count platforms) 1)
      {:select-platform
       {:reason "Select one reader-conditional platform"
        :platforms platforms}})))

(defn select-form
  "Select one top-level form from source by unqualified name or containing
   line. Pure: filename, source string, and options in; EDN result out."
  [file source {:keys [form line platform project-aliases] :as opts}]
  (let [base (base-result source (assoc opts :file file))
        selectors (selector-keys opts)
        normalized-form (normalize-form form)]
    (cond
      (empty? selectors)
      (error-result base :missing-selector
                    "Supply exactly one of :form or :line"
                    {:required-one-of [:form :line]})

      (> (count selectors) 1)
      (error-result base :conflicting-selectors
                    "Supply only one of :form or :line"
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

      (and (contains? opts :platform) (not (keyword? platform)))
      (error-result base :invalid-platform
                    ":platform must be a keyword such as :clj or :cljs"
                    {:platform platform})

      :else
      (try
        (let [records (outline/top-level-form-records
                        file source (or project-aliases {}))
              matches (->> records
                           (filter #(platform-match? platform %))
                           (filter (if normalized-form
                                     #(= normalized-form (:name %))
                                     #(line-match? line %)))
                           vec)]
          (cond
            (= 1 (count matches))
            (merge base
                   (select-keys (first matches)
                                [:type :name :platforms :line :end-line
                                 :comment-start :source]))

            (empty? matches)
            (let [by-name? (some? normalized-form)]
              (error-result
                base
                (if by-name? :form-not-found :line-not-in-form)
                (if by-name?
                  (str "Top-level form not found: " normalized-form)
                  (str "Line " line " is not contained by a top-level form"))
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
