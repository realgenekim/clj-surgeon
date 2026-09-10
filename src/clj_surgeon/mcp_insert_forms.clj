(ns clj-surgeon.mcp-insert-forms
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-plan :as p]
   [clj-surgeon.insert-forms-schema :as schema]
   [clj-surgeon.mcp-operation :as operation]
   [clojure.walk :as walk]))

;; @spec INSERT-FORMS-018
;; INTENT: INSERT-FORMS-018
(defn handle [_exchange params callback]
  (operation/invoke!
    {:execute #(try
                 (p/request-shape! params)
                 (insert/execute! (walk/keywordize-keys params))
                 (catch Exception e (assoc (p/refusal e) :elapsed_ms 0.0)))
     :summarize insert/receipt-text
     ;; Domain refusals are ordinary MCP tool results, even when :ok is false.
     :callback (fn [content _is-error receipt] (callback content false receipt))}))

(def tool
  {:id :insert-forms :name "insert_forms"
   :description "Insert payload.text containing exactly payload.forms forms at one structural boundary in one existing .clj file. Supply version=1, canonical workspace_root, relative file, guard.sha256 or portable guard.read_receipt, and anchor. Top-level anchors name an exact owner {kind,name}, expect=1, position before/after. Body anchors name defn/defn-/deftest, optional 1-based arity and direct testing_path [{label,expect:1}], and boundary {position:first/last} or {position:after-child,child:N}. One atomic splice preserves every original byte and records durable inverse evidence. No batch, preview, replacement, formatter, or verification profile. Inspect state, committed, mutation_attempted, source_unchanged first. Refusals write nothing; recovery-required means never replay. Successful write_verified proves parse and preservation only; verification_complete=false and no terminal_response. CLI: clj-surgeon :insert-forms! :request-file X or :op :insert-forms! :request-file X."
   :schema schema/schema :inputSchema schema/schema
   :output-schema {:properties {"elapsed_ms" {:type "number" :minimum 0}} :type "object" :required ["state" "committed" "mutation_attempted" "source_unchanged" "ok" "elapsed_ms"]}
   :structured? true :outcome-classes #{:committed :typed-refusal :verification-failed}
   :summarize insert/receipt-text :tool-fn #'handle})
