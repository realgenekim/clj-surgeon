(ns clj-surgeon.mcp-rename-alias
  (:require [clj-surgeon.rename-alias :as rename]
            [clj-surgeon.rename-alias-plan :as planner]
            [clj-surgeon.rename-alias-schema :as schema]
            [clj-surgeon.insert-forms-plan :as p]
            [clj-surgeon.mcp-operation :as operation]))
(defn request [params]
  (letfn [(convert [x path]
            (cond
              (map? x) (into {} (map (fn [[k v]]
                                      (let [key (if (#{[:guards] [:expect :references :per_file]} path) k
                                                  (if (string? k) (keyword k) k))]
                                        [key (convert v (conj path key))])) x))
              (vector? x) (mapv #(convert % path) x)
              :else x))]
    (convert params [])))
;; @spec RENAME-ALIAS-011
;; INTENT: RENAME-ALIAS-011
(defn handle [_exchange params callback]
  (operation/invoke!
    {:execute #(try (p/request-shape! params) (rename/execute! (request params))
                    (catch Exception e (assoc (planner/refuse e) :elapsed_ms 0.0)))
     :summarize rename/receipt-text
     :callback (fn [content _error receipt] (callback content false receipt))}))
(def tool
  {:id :rename-alias :name "rename_alias"
   :description "Rename a fixed library alias and all syntactic alias references in a guarded .clj snapshot. Supply version=1, workspace_root, scope {file|paths|repository,expect_files}, lib, old_alias, new_alias, expect.references {total|per_file}, and guards for every inspected file. Strings/comments/regex/discards stay byte-identical; quoted symbols and auto keywords count. Atomic per-file publication with guarded rollback; read state and mutation flags first. write_verified proves parse+byte-preservation, verification_complete=false. No terminal_response, preview, profile or automatic retry. CLI :rename-alias! :request-file X or :op :rename-alias! :request-file X."
   :schema schema/schema :inputSchema schema/schema
   :output-schema {:type "object" :properties {"elapsed_ms" {:type "number" :minimum 0}}
                   :required ["state" "committed" "mutation_attempted" "source_unchanged" "ok" "elapsed_ms"]}
   :structured? true :outcome-classes #{:committed :typed-refusal :verification-failed}
   :summarize rename/receipt-text :tool-fn #'handle})
