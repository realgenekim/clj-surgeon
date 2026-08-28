(ns clj-surgeon.experiments.mcp-candidate-response
  "Typed post-call projection for isolated naming catalogs."
  (:require
   [clojure.string :as str]))

(def routing-templates
  #{"Correct the named field and call apply_clojure_changes once. No source was changed."
    "Correct the project root or request and call apply_clojure_changes once."
    "Fill each caller disposition in next_call and call apply_clojure_changes once."
    "Set the decision to keep, one complete named-form replacement, whole-site delete, or one compact edit; then call apply_clojure_changes once."
    "Set every decision to keep, one complete named-form replacement, whole-site delete, or one compact edit; then call apply_clojure_changes once."
    "Set every decision to keep, one complete named-form replacement, whole-site delete, or one compact edit containing find and replace/delete; then call apply_clojure_changes once."
    "Call inspect_clojure prepare-change again."
    "Retry inspect_clojure prepare-change after the language server catches up."
    "Call inspect_clojure prepare-change again, then submit its returned next_call."})

(def known-errors
  #{"apply_clojure_changes refused"
    "apply_clojure_changes server is not initialized"
    "edit_clojure server is not initialized"
    "inspect_clojure server is not initialized"
    "transform_clojure server is not initialized"})

(def action-lines
  #{"→ fill next_call decisions, then call apply_clojure_changes once"
    "→ review visibility, fill caller decisions, then call apply_clojure_changes once"
    "→ copy next_call to inspect_clojure after doing other useful work"})

(defn- action-role [invoked-role result]
  (if (= :inspect invoked-role)
    (cond
      (contains? (:next_call result) :extraction) :extract
      (contains? (:next_call result) :basis) :plan
      :else :plan)
    invoked-role))

(defn- replace-token [text old-name new-name]
  (str/replace
    text
    (re-pattern
      (str "(?<![A-Za-z0-9_.!_-])"
           (java.util.regex.Pattern/quote old-name)
           "(?![A-Za-z0-9_.!_-])"))
    (java.util.regex.Matcher/quoteReplacement new-name)))

(defn- replace-names [text lexicon invoked-role result]
  (let [roles {"inspect_clojure" :inspect
               "edit_clojure" :edit
               "apply_clojure_changes" (action-role invoked-role result)
               "transform_clojure" (if (= :transform-commit invoked-role)
                                     :transform-commit
                                     :transform-preview)}]
    (reduce-kv
      (fn [projected old-name role]
        (replace-token projected old-name (get lexicon role)))
      text
      roles)))

(defn- project-routing-value [value project-text]
  (cond
    (and (string? value) (routing-templates value)) (project-text value)
    (map? value) (reduce-kv
                   (fn [result key child]
                     (assoc result key (project-routing-value child project-text)))
                   (empty value)
                   value)
    (vector? value) (mapv #(project-routing-value % project-text) value)
    :else value))

(defn- project-next-call [next-call lexicon invoked-role result]
  (if-not (and (map? next-call) (contains? next-call :tool))
    next-call
    (update next-call :tool
            #(case %
               "inspect_clojure" (:inspect lexicon)
               "edit_clojure" (:edit lexicon)
               "apply_clojure_changes" (get lexicon
                                            (action-role invoked-role result))
               "transform_clojure" (get lexicon
                                        (if (= :transform-commit invoked-role)
                                          :transform-commit
                                          :transform-preview))
               %))))

(defn project-structured
  "Project typed public slots; leave evidence-bearing values unchanged."
  [lexicon invoked-role result]
  (let [project-text #(replace-names % lexicon invoked-role result)
        public-operation (get lexicon invoked-role)]
    (cond->
      (reduce (fn [projected field]
                (if (contains? projected field)
                  (update projected field project-routing-value project-text)
                  projected))
              result
              [:remedy :decision-rule :next_action :remedies])
      (and (contains? result :operation)
           (#{:edit :extract :plan} invoked-role)
           (#{"edit_clojure" "apply_clojure_changes"} (:operation result)))
      (assoc :operation public-operation)

      (and (contains? result :error) (known-errors (:error result)))
      (update :error project-text)

      (contains? result :next_call)
      (update :next_call project-next-call lexicon invoked-role result))))

(defn- project-summary [text lexicon invoked-role result]
  (let [project-text #(replace-names % lexicon invoked-role result)
        remedy-line (when (and (string? (:remedy result))
                               (routing-templates (:remedy result)))
                      (str "→ " (:remedy result)))
        [summary evidence] (if (#{:transform-preview :transform-commit}
                                invoked-role)
                             (let [boundary (.indexOf ^String text "\n\n")]
                               (if (neg? boundary)
                                 [text ""]
                                 [(subs text 0 boundary)
                                  (subs text boundary)]))
                             [text ""])
        projected
        (->> (str/split summary #"\n" -1)
             (map-indexed
               (fn [index line]
                 (cond
                   (zero? index) (project-text line)
                   (or (action-lines line) (= remedy-line line))
                   (project-text line)
                   :else line)))
             (str/join "\n"))]
    (str projected evidence)))

(defn project-callback
  "Project the actual vector-of-summary-strings callback shape."
  [lexicon invoked-role content error? structured]
  {:content (mapv #(if (string? %)
                     (project-summary % lexicon invoked-role structured)
                     %)
                  content)
   :error? error?
   :structured (project-structured lexicon invoked-role structured)})

(defn wrap-handler [lexicon invoked-role handler]
  (with-meta
    (fn [exchange params callback]
      (handler exchange params
               (fn [content error? structured]
                 (let [{:keys [content error? structured]}
                       (project-callback lexicon invoked-role
                                         content error? structured)]
                   (callback content error? structured)))))
    {::canonical-handler handler
     ::role invoked-role}))
