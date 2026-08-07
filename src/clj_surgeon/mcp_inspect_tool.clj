(ns clj-surgeon.mcp-inspect-tool
  "Imperative MCP shell for one-read project-confined Clojure inspection."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.structural-lens :as structural-lens]))

(def tool-description
  (str
    "PREFER inspect_clojure over repeated source reads, grep, or CLI calls for "
    "Clojure structural questions that are known up front. Batch named forms, "
    "outlines, exact structural matches, and capability-limited X-ray analysis "
    "in one ordered read-only call. Each distinct project-relative source file "
    "is read once; every result is hash-bound to that coherent snapshot. The "
    "complete batch refuses on any missing or ambiguous form, invalid pattern, "
    "cardinality mismatch, path escape, parse failure, or output limit. It never "
    "writes source, plans, receipts, or manifests. Use forms for exact named "
    "top-level source, outline for compact :ls-equivalent structure, match for "
    "Clojure syntax rather than text, and xray for shipped sandboxed analysis."
    " One success with read_complete=true is terminal; do not repeat the call."
    " For example, count one def initializer with: (-> (form 'numeric-fields) "
    "initializer (expect-count 1) (analyze (fn [[fields]] (count fields))))."))

(def ^:private positive-integer-schema {:type "integer" :minimum 1})
(def ^:private non-negative-integer-schema {:type "integer" :minimum 0})
(def ^:private source-file-schema
  {:type "string"
   :minLength 1
   :pattern "^(?!/)(?![A-Za-z]:/)(?!.*(?:^|/)\\.\\.(?:/|$)).*\\.clj[sc]?$"
   :description "Project-relative .clj, .cljs, or .cljc source path."})

(defn- request-base
  [operation properties required]
  {:type "object"
   :additionalProperties false
   :properties
   (merge
     {"id" {:type "string" :minLength 1}
      "operation" {:const operation}
      "file" source-file-schema}
     properties)
   :required (into ["id" "operation" "file"] required)})

(def inspect-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"requests"
    {:type "array"
     :minItems 1
     :maxItems inspect/max-requests
     :description "Ordered all-or-nothing structural read requests with unique IDs."
     :items
     {:oneOf
      [(request-base
         "forms"
         {"forms" {:type "array" :minItems 1 :maxItems inspect/max-forms
                   :uniqueItems true
                   :items {:type "string" :minLength 1}}
          "expect" {:type "object" :additionalProperties false
                    :properties {"forms" positive-integer-schema}
                    :required ["forms"]}}
         ["forms" "expect"])
       (request-base "outline" {} [])
       (request-base
         "match"
         {"match" {:type "string" :minLength 1
                   :description "Exactly one structural Clojure form pattern; _ matches one subtree."}
          "inside" {:type "string" :minLength 1}
          "expect" {:type "object" :additionalProperties false
                    :properties {"matches" non-negative-integer-schema}}}
         ["match"])
       (request-base
         "xray"
         {"expression" {:type "string" :minLength 1
                        :description (str
                                       "Existing capability-limited :xray :expr program. "
                                       "Example: (-> (form 'numeric-fields) initializer "
                                       "(expect-count 1) (analyze (fn [[fields]] "
                                       "(count fields))))")}}
         ["expression"])]}}
    "expect"
    {:type "object"
     :additionalProperties false
     :description "Exact aggregate counts. files is the number of distinct file paths across all requests; count repeated paths once."
     :properties
     {"requests" (assoc positive-integer-schema
                        :description "Exact number of request objects.")
      "files" (assoc positive-integer-schema
                     :description "Exact number of distinct request file paths; repeated paths count once.")}
     :required ["requests" "files"]}}
   :required ["requests" "expect"]})

(def inspect-output-schema
  {:type "object"
   :additionalProperties true
   :properties
   {"ok" {:type "boolean"}
    "operation" {:const "inspect_clojure"}
    "read_complete" {:type "boolean"}
    "request_count" {:type "integer"}
    "file_count" {:type "integer"}
    "results" {:type "array"}
    "file_hashes" {:type "object"}
    "source_character_count" {:type "integer"}
    "next_action" {:type "string"}}
   :required ["ok" "operation" "read_complete"]})

(def inspect-annotations
  {:title "Inspect Clojure"
   :read-only true
   :destructive false
   :idempotent true
   :open-world false
   :return-direct false})

(defonce ^:private runtime-config (atom nil))

(defn init!
  "Set the live inspect configuration. Passing nil disarms the handler."
  [config]
  (reset! runtime-config config))

(defn- elapsed-ms
  [started-ns]
  (/ (double (- (System/nanoTime) started-ns)) 1000000.0))

(defn- inspect-refusal
  [result]
  (let [normalized (inspect/json-data result)]
    (merge
      {:ok false
       :operation "inspect_clojure"
       :read_complete false
       :source_unchanged true
       :next_action "correct_request"}
      normalized)))

(defn capture-snapshots
  "Resolve and read each distinct canonical file once in first-reference order.

  `read-source` is injectable for boundary tests and receives a canonical path."
  [project-root requests read-source expected-file-count]
  (try
    (let [root (mcp-paths/real-root project-root)
          files (vec (distinct (map :file requests)))
          resolved (mapv #(mcp-paths/resolve-source-path root %) files)
          refusal (first (remove :ok resolved))]
      (if refusal
        refusal
        (let [canonical-count (count (distinct (map #(str (:canonical %))
                                                    resolved)))]
          (if-not (= expected-file-count canonical-count)
            {:ok false
             :operation "inspect_clojure"
             :error_type "aggregate-file-expectation-mismatch"
             :error "Declared file count does not match distinct canonical files"
             :expected expected-file-count
             :actual canonical-count
             :read_complete false
             :source_unchanged true
             :next_action "correct_request"}
            (loop [remaining resolved
                   cache {}
                   snapshots (array-map)
                   reads 0]
              (if-let [{:keys [relative path canonical]} (first remaining)]
                (let [cache-key (str canonical)]
                  (if-let [captured (get cache cache-key)]
                    (recur (next remaining) cache
                           (assoc snapshots relative
                                  (assoc captured :file relative))
                           reads)
                    (let [source (read-source path)]
                      (when-not (string? source)
                        (throw
                          (ex-info "Source reader must return a string"
                                   {:error-type :source-read-failed
                                    :file relative})))
                      (let [captured {:file relative
                                      :source source
                                      :hash (structural-lens/source-hash source)}]
                        (recur (next remaining)
                               (assoc cache cache-key captured)
                               (assoc snapshots relative captured)
                               (inc reads))))))
                {:ok true
                 :root root
                 :snapshots snapshots
                 :file_read_count reads}))))))
    (catch Exception error
      {:ok false
       :operation "inspect_clojure"
       :error_type (name (or (:error-type (ex-data error))
                             :source-read-failed))
       :error (.getMessage error)
       :file (:file (ex-data error))
       :read_complete false
       :source_unchanged true
       :next_action "correct_request"})))

(defn execute-inspect!
  "Validate, confine, snapshot once, and evaluate one typed inspect request."
  [{:keys [project-root telemetry read-source output-limits]} params]
  (let [started (System/nanoTime)
        validated (inspect/validate-inspect-params params)
        result
        (assoc
          (if-not (:ok validated)
            (inspect-refusal validated)
            (let [normalized (:params validated)
                  captured (capture-snapshots
                             project-root (:requests normalized)
                             (or read-source slurp)
                             (get-in normalized [:expect :files]))]
              (if-not (:ok captured)
                (inspect-refusal captured)
                (assoc
                  (inspect/evaluate-snapshots
                    normalized (:snapshots captured)
                    (merge inspect/default-output-limits output-limits))
                  :file_read_count (:file_read_count captured)))))
          :elapsed_ms (elapsed-ms started))]
    (when telemetry
      (telemetry/record-inspect-call!
        telemetry params result {:total_ms (:elapsed_ms result)}))
    result))

(defn handle-inspect
  "Structured clojure-mcp callback handler, retained as a Var for hot reload."
  [_exchange params callback]
  (let [result (if-let [config @runtime-config]
                 (execute-inspect! config params)
                 {:ok false
                  :operation "inspect_clojure"
                  :error_type "server-not-initialized"
                  :error "inspect_clojure server is not initialized"
                  :read_complete false
                  :source_unchanged true
                  :next_action "restart_server"})
        summary (if (:ok result)
                  (inspect/concise-summary result)
                  (json/generate-string result))]
    (callback [summary] (not (:ok result)) result)
    (json/generate-string result)))

(def inspect-tool
  {:id :inspect-clojure
   :name "inspect_clojure"
   :description tool-description
   :schema inspect-schema
   :output-schema inspect-output-schema
   :annotations inspect-annotations
   :structured? true
   :tool-fn #'handle-inspect})
