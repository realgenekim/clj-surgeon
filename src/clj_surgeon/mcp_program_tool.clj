(ns clj-surgeon.mcp-program-tool
  (:require
   [cheshire.core :as json]
   [clj-surgeon.edit-dsl :as edit-dsl]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.structural-lens :as structural-lens]
   [rewrite-clj.node :as node]
   [rewrite-clj.zip :as z]))

(def max-transform-matches 128)
(def max-generated-characters 262144)

(def transform-tool-description
  (str
    "Compile one capability-limited Clojure transform into exact guarded edits. "
    "expression uses the existing structural path DSL and must end in transform. "
    "expect.matches is authoritative: the transform runs once per selected node "
    "only when the exact count matches. expect.max_changed_characters bounds the "
    "sum of replaced source spans. Preview is the default and never writes. Set "
    "commit=true for one atomic compare-and-swap write; comment-bearing selected "
    "subtrees refuse one-shot commit. SCI cannot perform I/O, start processes, "
    "load namespaces, mutate host state, or use Java interop."))

(def transform-tool-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string"}
    "file" {:type "string"}
    "expression" {:type "string"}
    "expect" {:type "object"
              :additionalProperties false
              :properties
              {"matches" {:type "integer" :minimum 1 :maximum max-transform-matches}
               "max_changed_characters" {:type "integer" :minimum 1
                                         :maximum max-generated-characters}}
              :required ["matches" "max_changed_characters"]}
    "commit" {:type "boolean"}}
   :required ["file" "expression" "expect"]})

(def transform-output-schema
  {:type "object"
   :properties {"ok" {:type "boolean"}}
   :required ["ok"]})

(def ^:private runtime-config runtime/tool-config)

(defn init!
  "Set the live transform tool configuration. Passing nil disarms it."
  [config]
  (reset! runtime-config
          (when config
            (assoc config :workspace-router (workspace/router config)))))

(defn- refusal
  [error-type message & [data]]
  (merge {:ok false
          :error-type error-type
          :error message
          :source-unchanged true}
         data))

(defn- node-size
  [form-node]
  (if (node/whitespace-or-comment? form-node)
    0
    (inc (if (node/inner? form-node)
           (reduce + 0 (map node-size (node/children form-node)))
           0))))

(defn- contains-comment?
  [form-node]
  (or (= :comment (node/tag form-node))
      (and (node/inner? form-node)
           (boolean (some contains-comment? (node/children form-node))))))

(defn- compile-expression
  [source file expression expected-matches]
  (try
    (let [query (edit-dsl/compile-query expression)
          terminal (peek query)]
      (if-not (and (vector? terminal)
                   (= :transform (first terminal))
                   (= 2 (count terminal))
                   (ifn? (second terminal)))
        (refusal :transform-required
                 "expression must end in one (transform pure-function)")
        (let [selection-query (pop query)
              found (structural-lens/evaluate-query
                      source selection-query {:file file})
              match-count (:match-count found)]
          (cond
            (:error found)
            (assoc found :ok false :source-unchanged true)

            (:matches-truncated? found)
            (refusal :transform-selection-too-large
                     "Transform selection exceeds the bounded result limit"
                     {:match-count match-count})

            (not= expected-matches match-count)
            (refusal :expected-count-mismatch
                     (str "Expected " expected-matches
                          " transform matches, found " match-count)
                     {:expected-count expected-matches
                      :match-count match-count})

            :else
            (let [transformer (second terminal)
                  edits
                  (mapv
                    (fn [index match]
                      (let [before (:source match)
                            form-node (z/node (z/of-string before))
                            before-value (node/sexpr form-node)
                            after-value (transformer before-value)
                            after (pr-str after-value)
                            preorder (get-in match [:address :preorder])]
                        {:id (str "transform/" (inc index))
                         :file file
                         :address (:address match)
                         :line (:line match)
                         :end-line (:end-line match)
                         :end-preorder (+ preorder (node-size form-node) -1)
                         :before before
                         :after after
                         :comment-bearing? (contains-comment? form-node)}))
                    (range)
                    (:matches found))
                  generated-characters (reduce + 0 (map #(count (:after %)) edits))]
              (cond
                (> generated-characters max-generated-characters)
                (refusal :generated-source-too-large
                         "Transform output exceeds the generated-source limit"
                         {:generated-characters generated-characters
                          :limit max-generated-characters})

                (every? #(= (:before %) (:after %)) edits)
                (refusal :no-op-transform
                         "Transform produced no source changes"
                         {:match-count match-count})

                :else
                (let [compiled
                      (transaction/compile-addressed-transaction
                        {file source}
                        (mapv #(dissoc % :comment-bearing?) edits))]
                  (if-not (:ok compiled)
                    (assoc compiled :ok false :source-unchanged true)
                    {:ok true
                     :compiled compiled
                     :match-count match-count
                     :edit-count (count edits)
                     :changed-characters
                     (reduce + 0 (map #(max (count (:before %))
                                            (count (:after %)))
                                      edits))
                     :comment-bearing-selection?
                     (boolean (some :comment-bearing? edits))}))))))))
    (catch Exception error
      (merge
        (refusal :transform-program-failed
                 (str "Transform program failed: " (.getMessage error)))
        (select-keys (ex-data error) [:error-type :expression :symbol])))))

(defn- public-preview
  [{:keys [compiled match-count edit-count changed-characters
           comment-bearing-selection?]} relative-file]
  (let [file-plan (first (:files compiled))]
    {:ok true
     :operation :transform-preview
     :file relative-file
     :match-count match-count
     :edit-count edit-count
     :changed-characters changed-characters
     :comment-bearing-selection? comment-bearing-selection?
     :lossless-commit-safe (not comment-bearing-selection?)
     :source-hash (:source-hash file-plan)
     :result-hash (:result-hash file-plan)
     :diff (:diff compiled)
     :source-unchanged true}))

(defn- execute-in-context!
  [{:keys [project-root]} {:keys [file expression expect commit]}]
  (try
    (let [root (mcp-paths/real-root project-root)
          resolved (mcp-paths/resolve-source-path root file)]
      (if-not (:ok resolved)
        (assoc resolved :ok false :source-unchanged true)
        (let [absolute-file (:path resolved)
              source (slurp absolute-file)
              expected-matches (:matches expect)
              budget (:max_changed_characters expect)]
          (cond
            (not (and (integer? expected-matches)
                      (<= 1 expected-matches max-transform-matches)))
            (refusal :invalid-transform-expectation
                     "expect.matches must be a bounded positive integer")

            (not (and (integer? budget)
                      (<= 1 budget max-generated-characters)))
            (refusal :invalid-transform-expectation
                     "expect.max_changed_characters must be a bounded positive integer")

            :else
            (let [planned (compile-expression source absolute-file expression
                                              expected-matches)]
              (cond
                (not (:ok planned)) planned

                (> (:changed-characters planned) budget)
                (refusal :change-budget-exceeded
                         "Compiled edit exceeds expect.max_changed_characters"
                         {:changed-characters (:changed-characters planned)
                          :max-changed-characters budget
                          :match-count (:match-count planned)})

                (not commit)
                (public-preview planned file)

                (:comment-bearing-selection? planned)
                (refusal :lossless-commit-refused
                         (str "One-shot commit refuses selected subtrees containing "
                              "comments; preview and use a narrower lossless selection")
                         {:match-count (:match-count planned)})

                :else
                (let [compiled (:compiled planned)
                      committed (transaction/commit-compiled! compiled)]
                  (if-not (:ok committed)
                    (assoc committed
                           :ok false
                           :source-unchanged
                           (or (= :source-hash-mismatch (:error-type committed))
                               (= true (:rolled-back committed))))
                    (merge
                      committed
                      {:ok true
                       :operation :transform!
                       :file file
                       :match-count (:match-count planned)
                       :edit-count (:edit-count planned)
                       :changed-characters (:changed-characters planned)
                       :diff (:diff compiled)
                       :receipt (transaction/build-receipt compiled)})))))))))
    (catch Exception error
      (merge
        (refusal :transform-tool-failure (.getMessage error))
        (select-keys (ex-data error) [:error-type :file])))))

(defn execute-request!
  "Route and execute one transform request."
  [config params]
  (let [normalized (json/parse-string (json/generate-string params) true)
        router (or (:workspace-router config) (workspace/router config))
        routed (workspace/resolve-request router normalized)]
    (if-not (:ok routed)
      (assoc routed :ok false :source-unchanged true)
      (assoc (execute-in-context! (:config routed) (:params routed))
             :workspace-root (:workspace-root routed)))))

(defn- summary
  [result]
  (if (:ok result)
    (str "transform_clojure\n  " (name (:operation result))
         " · " (:match-count result) " guarded edit(s)\n\n"
         (:diff result))
    (str "transform_clojure refused · " (name (:error-type result))
         "\n" (:error result) "\nsource unchanged")))

(defn handle-transform-clojure
  "clojure-mcp callback handler retained as a Var for hot reload."
  [_exchange params callback]
  (let [result (if-let [config @runtime-config]
                 (execute-request! config params)
                 (refusal :server-not-initialized
                          "transform_clojure server is not initialized"))
        body (json/generate-string result)]
    (callback [(summary result)] (not (:ok result)) result)
    body))

(def transform-clojure-tool
  {:id :transform-clojure
   :name "transform_clojure"
   :description transform-tool-description
   :schema transform-tool-schema
   :output-schema transform-output-schema
   :structured? true
   :tool-fn #'handle-transform-clojure})
