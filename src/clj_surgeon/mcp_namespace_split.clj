(ns clj-surgeon.mcp-namespace-split
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-operation :as operation]
   [clj-surgeon.namespace-split-io :as split]))

;; @spec NS-SPLIT-013
(defn summary [result]
  (str "namespace_split: " (:state result)
       (when (= :pending (get-in result [:proof :status]))
         (str "; split committed; proof pending (" (name (get-in result [:proof :tier]))
              "); next step :proof-status (see proof.next_call)"))
       "\n" (json/generate-string result {:escape-non-ascii true})))

;; @spec NS-SPLIT-013
(defn handle [_exchange params callback]
  (operation/invoke!
    {:execute #(operation/canonicalize-receipt-text (split/execute! params))
     :summarize summary :callback callback}))

(def tool
  {:id :namespace-split :name "namespace_split"
   :description "Split one namespace into all mapped destinations in one guarded transaction. Send workspace_root, source {file,lib,retain?,alias_policy?,comment_policy?}, destinations [{file,lib,forms,alias_policy}], promotion_policy (promote-required or authorized names), source_retirement (delete or retain-empty; omit when source.retain is true), bounded roots, and verification {profile,profile-file?}. profile-file is an absolute external EDN config containing :verification-profiles; it overrides workspace configuration. References are resolved once over the captured sources; the compiler derives final requires, aliases, promotions, declarations and callers. Optional constraints.forbidden_edges and expect counts are guards. plan_only=true returns nonmutating analysis; plan_only=\"facts\" returns snapshot-bound owners, exact references, retained dependencies, promotions and graph facts without invoking candidate emitters; after commit the same request reads committed receipt facts, or refuses committed-facts-stale with closure_receipt. Pre-commit facts include a valid manifest with verification. source.retain=true keeps unmapped owners in place; comment_policy=remove-moved-invocations authorizes removing offending moved calls in source comment forms; snapshot_hash binds a reviewed plan. Verification must be a configured synchronous command profile. Empty commands refuse verification-empty-profile. A true-only profile commits with verification_complete=false and cold-suite pending. Profiles may select :proof :warm with :gate :background: a detached pid runs pending commands and writes closure_receipt; original receipt_path remains incomplete forever. CLI :op :proof-status :receipt ORIGINAL joins them as complete/pending/failed/stale. A background gate requires java.io.tmpdir to be an existing directory below /var/tmp: launch Babashka with TMPDIR=/var/tmp/<owned-dir> and the JVM or MCP server with -Djava.io.tmpdir=/var/tmp/<owned-dir>; otherwise the call refuses background-gate-unsafe-tmpdir before any mutation and next_call names that repair. Committed receipts carry proof {tier,status,execution,next_call}: verification_complete=false means pending proof, not a failed split; the named next read is :proof-status and it never reruns the mutation. Negative facts include comment_edits as an owner/content diff grouped by file/after_file; edits group changes by owner: moved {from,to} has corresponding line vectors (inclusive ranges allowed), changed {before,after} has text vectors and line/after_line or moved locations; declared before/after encodings preserve indentation losslessly, added, deleted, or removed_occurrence when duplicate content survives. Line shifts alone are omitted; comment_policy names the applied policy, scoped stale_references and facades, exactly_once definition counts with pre-existing namesakes identified, and bodies_preserved SHA-256 rows separating raw equality from authorized replay. New fact strings use declared lossless JSON string-content encoding. Contradictory candidates refuse before write. Committed facts include per-destination owners_moved and static_sites_rewritten per caller file, retained_vars and unexpected_paths within the receipt budget. The receipt distinguishes committed, refused, rolled-back and recovery-required with named checks and guarded undo. Dynamic resolution is not claimed; unsupported evidence refuses."
   :schema split/schema :inputSchema split/schema
   :output-schema {:type "object" :required ["ok" "state" "elapsed_ms"]
                   :properties {"ok" {:type "boolean"} "state" {:type "string"}
                                "elapsed_ms" {:type "number" :minimum 0}}}
   :structured? true :outcome-classes #{:committed :typed-refusal :verification-failed :read}
   :summarize summary :tool-fn #'handle})
