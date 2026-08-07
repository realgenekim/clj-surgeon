(ns clj-surgeon.mcp-tool
  (:require
   [cheshire.core :as json]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clojure.java.io :as io])
  (:import
   (java.nio.file Files Path)
   (java.util UUID)))

(def tool-description
  (str
    "STRUCTURAL CLOJURE EDITING. PREFER this over apply_patch when a request "
    "supplies two or more exact Clojure replacements or spans files. It avoids "
    "fragile patch-context mismatches, applies the whole decision in one tool "
    "call, and reduces source reads and generated tokens. When every source "
    "file, named owner form, exact before form, exact replacement form, and "
    "positive match count is known, do not read source first: compile the "
    "complete decision into one call. This tool writes source as one "
    "failure-atomic transaction. It refuses "
    "the whole request on scope or count mismatch, parses and reads back every "
    "changed file, and publishes an inverse receipt. A success with "
    "verification_complete=true is terminal mutation proof: do not reread or "
    "diff unless the user explicitly requested aggregate review. Prefer native "
    "patching for prose or one unique text edit. Example: changes=[{id:\"body\","
    "files:[\"src/ui.clj\"],forms:[\"shell\"],find:\":body\","
    "replace:\":body.page\",expect:{matches:1,each_form:1}}],"
    "expect={changes:1,edits:1,files:1}."))

(def ^:private positive-integer-schema
  {:type "integer" :minimum 1})

(def clj-change-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"changes"
    {:type "array"
     :minItems 1
     :description "Complete structural changes. IDs must be unique."
     :items
     {:type "object"
      :additionalProperties false
      :properties
      {"id" {:type "string" :minLength 1
             :description "Stable diagnostic label, for example body-class."}
       "files" {:type "array" :minItems 1
                :description "Project-relative .clj, .cljs, or .cljc paths. No .. or absolute paths."
                :items {:type "string" :minLength 1}}
       "forms" {:type "array" :minItems 1
                :description "Exact named top-level owner forms."
                :items {:type "string" :minLength 1}}
       "find" {:type "string" :minLength 1
               :description "Exactly one Clojure form in its required source spelling."}
       "replace" {:type "string" :minLength 1
                  :description "Exactly one replacement Clojure form. Source spelling is preserved."}
       "expect" {:type "object"
                 :additionalProperties false
                 :properties
                 {"matches" (assoc positive-integer-schema
                                   :description "Required total matches for this change.")
                  "each_form" (assoc positive-integer-schema
                                     :description "Optional required matches in every named owner.")
                  "each_file" (assoc positive-integer-schema
                                     :description "Optional required matches in every named file.")}
                 :required ["matches"]}}
      :required ["id" "files" "forms" "find" "replace" "expect"]}}
    "expect"
    {:type "object"
     :additionalProperties false
     :description "Aggregate transaction cardinality. All three counts are required."
     :properties
     {"changes" (assoc positive-integer-schema :description "Number of change objects.")
      "edits" (assoc positive-integer-schema :description "Total exact replacements.")
      "files" (assoc positive-integer-schema :description "Total files that must change.")}
     :required ["changes" "edits" "files"]}}
   :required ["changes" "expect"]})

(defonce ^:private runtime-config (atom nil))

(defn init!
  "Set the live tool configuration. Passing nil disarms the handler."
  [config]
  (reset! runtime-config config)
  (inspect-tool/init! config))

(defn- real-root
  ^Path [root]
  (mcp-paths/real-root root))

(defn- resolve-source-path
  [^Path root relative]
  (mcp-paths/resolve-source-path root relative))

(defn- resolve-transaction-paths
  [project-root spec]
  (loop [changes (:changes spec)
         resolved []]
    (if-let [change (first changes)]
      (let [paths (mapv #(resolve-source-path project-root %) (:in change))
            refusal (first (remove :ok paths))]
        (if refusal
          refusal
          (recur (next changes)
                 (conj resolved
                       (assoc change :in (mapv :path paths))))))
      {:ok true :spec (assoc spec :changes resolved)})))

(defn- default-receipt-dir
  []
  (str (io/file (System/getProperty "user.home")
                ".local" "state" "clj-surgeon" "receipts")))

(defn- delete-empty-dir!
  [directory created?]
  (when created?
    (try
      (Files/deleteIfExists (.toPath (io/file directory)))
      (catch Exception _ nil))))

(defn- elapsed-ms
  [started-ns]
  (/ (double (- (System/nanoTime) started-ns)) 1000000.0))

(defn- timed
  [f]
  (let [started (System/nanoTime)
        result (f)]
    [result (elapsed-ms started)]))

(defn- record-result!
  [telemetry-state request response total-start timings]
  (when telemetry-state
    (telemetry/record-call!
      telemetry-state request response
      (assoc timings :total_ms (elapsed-ms total-start))))
  response)

(defn execute-request!
  "Validate, confine, and execute one typed request through the loaded kernel."
  [{:keys [project-root receipt-dir telemetry]} params]
  (let [total-start (System/nanoTime)
        [validated validation-ms]
        (timed #(contract/validate-tool-params params))]
    (if-not (:ok validated)
      (record-result! telemetry params (contract/normalize-refusal validated)
                      total-start {:validation_ms validation-ms})
      (try
        (let [[prepared confinement-ms]
              (timed
                #(let [root (real-root project-root)
                       translated
                       (contract/tool-params->transaction (:params validated))]
                   {:root root
                    :resolved (resolve-transaction-paths root translated)}))
              {:keys [root resolved]} prepared]
          (if-not (:ok resolved)
            (record-result! telemetry params resolved total-start
                            {:validation_ms validation-ms
                             :confinement_ms confinement-ms})
            (let [directory (str (or receipt-dir (default-receipt-dir)))
                  directory-file (io/file directory)
                  existed? (.exists directory-file)
                  _ (.mkdirs directory-file)
                  receipt (str (io/file directory
                                        (str (UUID/randomUUID) ".edn")))
                  [result kernel-ms]
                  (timed #(transaction/execute-change!
                            {:spec (:spec resolved) :receipt-out receipt}))
                  classified (contract/classify-kernel-result
                               (.toString root) result)]
              (when-not (:ok classified)
                (delete-empty-dir! directory (not existed?)))
              (record-result! telemetry params classified total-start
                              {:validation_ms validation-ms
                               :confinement_ms confinement-ms
                               :kernel_ms kernel-ms}))))
        (catch Exception error
          (record-result!
            telemetry params
            {:ok false
             :error_type "mcp-adapter-failure"
             :error (.getMessage error)
             :source_unchanged true
             :remedy "Correct the project root or request and call apply_clojure_changes once."}
            total-start {:validation_ms validation-ms}))))))

(defn handle-clj-change
  "clojure-mcp callback handler. Kept as a Var for live nREPL redefinition."
  [_exchange params callback]
  (let [result (if-let [config @runtime-config]
                 (execute-request! config params)
                 {:ok false
                  :error_type "server-not-initialized"
                  :error "apply_clojure_changes server is not initialized"
                  :source_unchanged true
                  :remedy "Restart the configured clj-surgeon MCP server."})
        body (json/generate-string result)]
    (callback [body] (not (:ok result)))
    body))

(def clj-change-tool
  {:id :clj-change
   :name "apply_clojure_changes"
   :description tool-description
   :schema clj-change-schema
   :tool-fn #'handle-clj-change})

(defn all-tools
  []
  [inspect-tool/inspect-tool clj-change-tool])
