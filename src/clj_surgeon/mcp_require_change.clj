(ns clj-surgeon.mcp-require-change
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-operation :as operation]
   [clj-surgeon.require-change :as change]
   [clj-surgeon.require-change-io :as boundary]))

;; @spec REQUIRE-CHANGE-001
(defn summary [result]
  (str "require_change: " (:state result) "\n" (json/generate-string result {:escape-non-ascii true})))

;; @spec REQUIRE-CHANGE-001
(defn handle
  ([exchange params callback] (handle {} exchange params callback))
  ([config _exchange params callback]
   (operation/invoke!
     {:execute #(operation/canonicalize-receipt-text (boundary/execute! config params))
      :summarize summary :callback callback})))

(def tool
  {:id :require-change :name "require_change"
   :description "Add one library across an explicit nonempty file vector, without symbol edits. Send workspace_root, add {lib,alias_policy:[ordered aliases]}, files [{file,source_hash?,remove?:{lib,as}}], exact expect {files,adds,removes}, verification {profile}. Reuse a policy alias bound to the target, else choose the first free policy alias; exhaustion refuses with file/bindings. Splice one sorted line before the first greater library and its attached comment run, inheriting local indent and preserving every other byte. Explicit removals delete complete standalone lines; ambiguous, conditional, refer, prefix and unrepresentable zero-churn layouts refuse. Optional layout is {order:stable-lib-symbol,comments:attach-following,blank_lines:0}. plan_only=true previews without mutation or proof. A configured synchronous profile runs inside the apply call; any failed check rolls back or reports recovery-required. Read state, verification_complete, checks and undo_receipt; receipt_details_path retains any elided evidence. Never replay a committed operation. Undo: clj-surgeon :op :undo-extract! :receipt PATH."
   :schema change/schema :inputSchema change/schema
   :output-schema {:type "object" :required ["ok" "state" "elapsed_ms"]
                   :properties {"ok" {:type "boolean"} "state" {:type "string"}
                                "elapsed_ms" {:type "number" :minimum 0}}}
   :structured? true :outcome-classes #{:committed :typed-refusal :verification-failed :read}
   :summarize summary :tool-fn #'handle})
