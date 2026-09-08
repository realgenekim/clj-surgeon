(ns clj-surgeon.mcp-namespace-split
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-operation :as operation]
   [clj-surgeon.namespace-split-io :as split]))

;; @spec NS-SPLIT-013
(defn summary [result]
  (str "namespace_split: " (:state result) "\n" (json/generate-string result {:escape-non-ascii true})))

;; @spec NS-SPLIT-013
(defn handle [_exchange params callback]
  (operation/invoke!
    {:execute #(operation/canonicalize-receipt-text (split/execute! params))
     :summarize summary :callback callback}))

(def tool
  {:id :namespace-split :name "namespace_split"
   :description "Split one namespace into all mapped destinations in one guarded transaction. Send workspace_root, source {file,lib,retain?,alias_policy?,comment_policy?}, destinations [{file,lib,forms,alias_policy}], promotion_policy (promote-required or authorized names), source_retirement (delete or retain-empty; omit when source.retain is true), bounded roots, and verification {profile}. References are resolved once over the captured sources; the compiler derives final requires, aliases, promotions, declarations and callers. Optional constraints.forbidden_edges and expect counts are guards. plan_only=true returns nonmutating analysis; plan_only=\"facts\" returns snapshot-bound owners, exact references, retained dependencies, promotions and graph facts without invoking candidate emitters. source.retain=true keeps unmapped owners in place; comment_policy=remove-moved-invocations authorizes removing offending moved calls in source comment forms; snapshot_hash binds a reviewed plan. Verification must be a configured synchronous command profile. The receipt distinguishes committed, refused, rolled-back and recovery-required with named checks and guarded undo. Dynamic resolution is not claimed; unsupported evidence refuses."
   :schema split/schema :inputSchema split/schema
   :output-schema {:type "object" :required ["ok" "state" "elapsed_ms"]
                   :properties {"ok" {:type "boolean"} "state" {:type "string"}
                                "elapsed_ms" {:type "number" :minimum 0}}}
   :structured? true :outcome-classes #{:committed :typed-refusal :verification-failed :read}
   :summarize summary :tool-fn #'handle})
