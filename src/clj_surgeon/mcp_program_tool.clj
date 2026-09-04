(ns clj-surgeon.mcp-program-tool
  (:require
   [cheshire.core :as json]
   [clj-surgeon.edit-dsl :as edit-dsl]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-operation :as mcp-operation]
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
    "expression uses the structural path DSL and must end in transform. "
    "Start with (form 'owner) when one top-level owner bounds the change; for example, "
    "(-> (form 'retry-policy) initializer (match :retry-delays) right "
    "(transform (fn [delays] (mapv (partial + 100) delays)))). "
    "Start with [] when the relation spans every matching owner in the file; for example, "
    "(-> [] (match :retry-delays) right "
    "(transform (fn [delays] (mapv (partial + 100) delays)))). "
    "expect.matches is the authoritative exact cardinality guard; omit redundant "
    "expect-count from the expression. expect.max_changed_characters bounds the sum "
    "of replaced source spans. Preview is the default and never writes. Set commit=true "
    "for one atomic compare-and-swap write; a successful commit returns "
    "verification_complete=true and next_action=none, which are terminal evidence. "
    "Comment-bearing selected subtrees refuse one-shot commit. SCI cannot perform "
    "I/O, start processes, load namespaces, mutate host state, or use Java interop."))

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

;; @spec MCP-OP-SCHEMA-001
(def transform-output-schema
  {:type "object"
   :properties {"ok" {:type "boolean"}
                "measured" mcp-operation/measured-output-schema}
   :required ["ok" "measured"]})

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

(defn- compiled-edits
  [compiled]
  (mapcat :edits (:files compiled)))

(defn compile-programs
  "Compile several independent transform programs against one frozen source map."
  [sources programs]
  (cond
    (not (vector? programs))
    (refusal :invalid-transform-programs
             "programs must be a non-empty array")

    (not (<= 1 (count programs) 16))
    (refusal :invalid-transform-programs
             "programs must contain between 1 and 16 items"
             {:program-count (count programs)})

    :else
    (let [plans
          (mapv
            (fn [index {:keys [file expression expect]}]
              (let [expected-matches (:matches expect)
                    budget (:max_changed_characters expect)
                    source (get sources file)
                    invalid
                    (cond
                      (not (and (string? file) (seq file)))
                      (refusal :invalid-transform-program
                               "program.file must be a non-empty string")

                      (not (string? source))
                      (refusal :transform-source-missing
                               "program.file is not present in the frozen source map")

                      (not (and (string? expression) (seq expression)))
                      (refusal :invalid-transform-program
                               "program.expression must be a non-empty string")

                      (not (and (integer? expected-matches)
                                (<= 1 expected-matches max-transform-matches)))
                      (refusal :invalid-transform-expectation
                               "program.expect.matches must be a bounded positive integer")

                      (not (and (integer? budget)
                                (<= 1 budget max-generated-characters)))
                      (refusal :invalid-transform-expectation
                               "program.expect.max_changed_characters must be a bounded positive integer"))]
                (if invalid
                  (assoc invalid :program-index index :program-file file)
                  (let [planned (compile-expression source file expression
                                                    expected-matches)]
                    (cond
                      (not (:ok planned))
                      (assoc planned :program-index index :program-file file)

                      (> (:changed-characters planned) budget)
                      (refusal :change-budget-exceeded
                               "Compiled program exceeds expect.max_changed_characters"
                               {:program-index index
                                :program-file file
                                :changed-characters (:changed-characters planned)
                                :max-changed-characters budget
                                :match-count (:match-count planned)})

                      (:comment-bearing-selection? planned)
                      (refusal :lossless-commit-refused
                               (str "Committed programs refuse selected subtrees containing "
                                    "comments; use a narrower selection or a literal edit")
                               {:program-index index
                                :program-file file
                                :match-count (:match-count planned)})

                      :else
                      (assoc planned :program-index index
                             :program-file file))))))
            (range) programs)
          failed (first (remove :ok plans))]
      (if failed
        failed
        (let [raw-edits (mapcat #(compiled-edits (:compiled %)) plans)
              edits (mapv (fn [index edit]
                            (-> edit
                                (assoc :id (str "program/" (inc index)))
                                (dissoc :intent-index)))
                          (range) raw-edits)
              changed-characters (reduce + 0 (map :changed-characters plans))]
          (cond
            (> (count edits) 256)
            (refusal :transform-batch-too-large
                     "Compiled programs exceed the concrete edit limit"
                     {:edit-count (count edits) :limit 256})

            (> changed-characters max-generated-characters)
            (refusal :change-budget-exceeded
                     "Compiled programs exceed the aggregate changed-character limit"
                     {:changed-characters changed-characters
                      :max-changed-characters max-generated-characters})

            :else
            (let [compiled (transaction/compile-addressed-transaction
                             sources edits)]
              (if-not (:ok compiled)
                (assoc compiled :ok false :source-unchanged true)
                {:ok true
                 :compiled compiled
                 :program-count (count programs)
                 :edit-count (count edits)
                 :match-count (reduce + 0 (map :match-count plans))
                 :changed-characters changed-characters}))))))))

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
     :source-unchanged true
     :verification_complete false
     :next_action "commit"}))

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
                       :receipt (transaction/build-receipt compiled)
                       :verification_complete true
                       :next_action "none"})))))))))
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
         " · " (:match-count result) " guarded edit(s) · "
         (mcp-operation/format-elapsed-ms (mcp-operation/elapsed-ms result)) "\n\n"
         (:diff result))
    (str "transform_clojure refused · " (name (:error-type result))
         " · " (mcp-operation/format-elapsed-ms (mcp-operation/elapsed-ms result))
         "\n" (:error result) "\nsource unchanged")))

(defn handle-transform-clojure
  "clojure-mcp callback handler retained as a Var for hot reload."
  [_exchange params callback]
  (mcp-operation/invoke!
    {:execute #(if-let [config @runtime-config]
                 (execute-request! config params)
                 (refusal :server-not-initialized
                          "transform_clojure server is not initialized"))
     :summarize summary
     :callback callback}))

(def transform-clojure-tool
  {:id :transform-clojure
   :name "transform_clojure"
   :description transform-tool-description
   :schema transform-tool-schema
   :output-schema transform-output-schema
   :structured? true
   :tool-fn #'handle-transform-clojure})
