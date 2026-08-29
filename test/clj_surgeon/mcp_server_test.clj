(ns clj-surgeon.mcp-server-test
  (:require
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-program-tool :as program-tool]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-server :as server]
   [clj-surgeon.mcp-tool :as tool]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [nrepl.core :as nrepl]
   [nrepl.server :as nrepl-server])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-mcp-server-test-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(deftest tool-profiles-preserve-full-default-and-isolate-the-editor
  (is (= ["inspect_clojure" "apply_clojure_changes" "edit_clojure"
          "transform_clojure"]
         (mapv :name (tool/tools-for-profile :full))))
  (is (= ["edit_clojure"]
         (mapv :name (tool/tools-for-profile :edit))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Unsupported MCP tool profile"
                        (tool/tools-for-profile :unknown))))

;; @spec MCP-OP-SCHEMA-001
(deftest exposes-exactly-four-typed-tools
  (let [tools (server/make-tools nil ".")]
    (is (= 4 (count tools)))
    (is (= ["inspect_clojure" "apply_clojure_changes" "edit_clojure"
            "transform_clojure"]
           (mapv :name tools)))
    (doseq [{:keys [output-schema]} tools]
      (is (= {:type "number" :minimum 0}
             (get-in output-schema [:properties "elapsed_ms"])))
      (is (some #{"elapsed_ms"} (:required output-schema))))
    (is (= #'inspect-tool/handle-inspect (:tool-fn (first tools))))
    (is (= #'tool/handle-clj-change (:tool-fn (second tools))))
    (is (= #'tool/handle-clj-change (:tool-fn (nth tools 2))))
    (is (= #'program-tool/handle-transform-clojure
           (:tool-fn (nth tools 3))))
    (is (= false (get-in tools [3 :schema :additionalProperties])))
    (is (= ["file" "expression" "expect"]
           (get-in tools [3 :schema :required])))
    (is (= false (get-in tools [2 :schema :additionalProperties])))
    (is (= #{"workspace_root" "edits" "programs" "delete_owners"
             "symbol_migration" "require_change"}
           (set (keys (get-in tools [2 :schema :properties])))))
    (is (= [{:required ["edits"]}
            {:required ["programs"]}
            {:required ["delete_owners"]}
            {:required ["symbol_migration" "require_change"]}]
           (get-in tools [2 :schema :anyOf])))
    (is (str/includes? (:description (nth tools 2))
                       "exact old subtree"))
    (is (= false (get-in tools [0 :schema :additionalProperties])))
    (is (= "^[0-9a-f]{64}$"
           (get-in tools [0 :schema :properties "snapshot_guards"
                          :additionalProperties :pattern])))
    (is (= #{"snapshot_bound" "selector_authority" "write_authority"
             "completed_request_count" "completed_request_ids"
             "pending_request_count" "pending_request_ids"
             "snapshot_guards" "completed_results" "retry_template"}
           (set (get-in tools [0 :output-schema :properties "continuation"
                               :required]))))
    (is (= false
           (get-in tools [0 :output-schema :properties "continuation"
                          :properties "write_authority" :const])))
    (is (= false
           (get-in tools [0 :output-schema :properties "continuation"
                          :properties "retry_template" :properties
                          "executable" :const])))
    (is (= ["workspace_root" "snapshot_guards" "requests" "expect"]
           (get-in tools [0 :output-schema :properties "continuation"
                          :properties "retry_template" :properties
                          "arguments" :required])))
    (is (= false (get-in tools [1 :schema :additionalProperties])))
    (is (= #{"basis" "decisions" "verify" "changes" "expect" "edits"
             "programs" "delete_owners" "extraction" "workspace_root"
             "symbol_migration" "require_change"}
           (set (keys (get-in tools [1 :schema :properties])))))
    (is (= 4 (count (get-in tools [1 :schema :oneOf]))))
    (testing "the direct route accepts the same verify field it publishes"
      (let [direct-route (second (get-in tools [1 :schema :oneOf]))
            excluded (set (map (comp set :required)
                               (get-in direct-route [:not :anyOf])))]
        (is (= #{#{"basis"} #{"decisions"} #{"edits"} #{"programs"}
                 #{"delete_owners"} #{"extraction"}
                 #{"symbol_migration"} #{"require_change"}}
               excluded))))
    (is (str/includes?
          (get-in tools [1 :schema :properties "verify" :description])
          "cold job"))
    (is (str/includes?
          (get-in tools [1 :schema :properties "verify" :description])
          "hot laws roll back"))
    (is (= inspect-tool/inspect-annotations
           (:annotations (first tools))))
    (is (str/includes? inspect-tool/tool-description
                       "mode=prepare-change"))
    (is (str/includes? inspect-tool/tool-description
                       "read_complete=true is terminal"))
    (testing "prepare-change publishes its exact-source route and label grammar"
      (is (some #(= {:properties {"mode" {:const "prepare-change"}}
                     :required ["mode" "file" "form" "intent"]} %)
                (get-in tools [0 :schema :oneOf])))
      (is (some #(= {:properties {"mode" {:const "plan-extraction"}}
                     :required ["mode" "file" "to" "forms" "require_policy"]} %)
                (get-in tools [0 :schema :oneOf])))
      (is (some #(= {:required ["verification_job" "view"]} %)
                (get-in tools [0 :schema :oneOf])))
      (is (= "^[a-z][a-z0-9-]{0,39}$"
             (get-in tools [0 :schema :properties "label" :pattern])))
      (is (str/includes?
            (get-in tools [0 :schema :properties "label" :description])
            "Must match ^[a-z][a-z0-9-]{0,39}$")))
    (is (< (count server/server-instructions) 512))
    (is (str/includes? server/server-instructions
                       "Batch known Clojure reads"))
    (is (str/includes? tool/tool-description
                       "failure-atomic Clojure transaction"))
    (is (str/includes? server/server-instructions
                       "two or more exact edits"))
    (testing "a clean caller can construct both requests from tools/list"
      (is (str/includes? server/server-instructions
                         "verification_complete=true"))
      (is (= #{"id" "files" "forms" "owner" "find" "inside" "replace" "delete"
               "insert_before" "insert_after" "rename_binding"
               "assoc_entry" "expect"}
             (set (keys (get-in tools [1 :schema :properties "changes" :items :properties])))))
      (is (= false
             (get-in tools [1 :schema :properties "changes" :items :additionalProperties])))
      (is (= #{"file" "to" "forms" "require_policy"}
             (set (get-in tools [1 :schema :properties "extraction" :required]))))
      (let [[owner-rule action-rule]
            (get-in tools [1 :schema :properties "changes" :items :allOf])]
        (is (= #{#{"forms"} #{"owner"}}
               (set (map (comp set :required) (:oneOf owner-rule)))))
        (is (= #{#{"find" "replace"}
                 #{"forms" "delete"}
                 #{"insert_before"}
                 #{"insert_after"}
                 #{"forms" "rename_binding"}
                 #{"find" "assoc_entry"}}
               (set (map (comp set :required) (:oneOf action-rule))))))
      (is (str/includes? tool/tool-description
                         "For named top-level def or defn owners, use forms: [name]"))
      (is (str/includes? tool/tool-description
                         "kind: defmethod"))
      (is (str/includes? tool/tool-description
                         "verification_complete=false"))
      (is (str/includes? tool/tool-description
                         "exactly one action"))
      (is (str/includes? tool/tool-description
                         "delete: true once"))
      (is (str/includes? tool/tool-description
                         "refuse comment-bearing gaps"))
      (doseq [tool-index [0 1]]
        (let [description
              (get-in tools [tool-index :schema :properties "workspace_root" :description])]
          (is (str/includes? description "Omit to use the server default"))
          (is (str/includes? description "Preserve the returned workspace_root")))))))

(deftest live-tool-sync-plans-contract-changes-without-handler-churn
  (let [handler-a (fn [& _])
        handler-b (fn [& _])
        inspect {:name "inspect" :description "read"
                 :schema {:type "object"}
                 :annotations {:read-only true}
                 :tool-fn handler-a}
        apply-tool {:name "apply" :description "write"
                    :schema {:type "object"}
                    :annotations {:read-only false}
                    :tool-fn handler-a}
        registered (server/tool-contracts [inspect apply-tool])]
    (testing "handler redefinition is already hot and does not churn tools/list"
      (is (= {:remove [] :upsert []}
             (server/tool-sync-plan
               registered
               [(assoc inspect :tool-fn handler-b) apply-tool]))))
    (testing "schema, description, and annotation changes replace the tool"
      (doseq [changed [(assoc inspect :schema {:type "object" :required ["x"]})
                       (assoc inspect :description "better read")
                       (assoc inspect :annotations {:read-only false})]]
        (is (= {:remove [] :upsert ["inspect"]}
               (server/tool-sync-plan registered [changed apply-tool])))))
    (testing "addition and removal are explicit and stably ordered"
      (is (= {:remove ["apply"] :upsert ["third"]}
             (server/tool-sync-plan
               registered
               [inspect (assoc apply-tool :name "third")]))))))

(deftest embedded-nrepl-starts-without-resolving-cider
  (let [directory (temp-dir)
        port-file (io/file directory ".nrepl-port")
        resolve-var clojure.core/requiring-resolve
        embedded
        (with-redefs [clojure.core/requiring-resolve
                      (fn [symbol]
                        (if (= 'cider.nrepl/cider-nrepl-handler symbol)
                          (throw (ex-info "CIDER must not load on startup"
                                          {:symbol symbol}))
                          (resolve-var symbol)))]
          (server/start-embedded-nrepl! 0 (.getPath port-file)))]
    (try
      (is (some? embedded))
      (finally
        (when embedded (nrepl-server/stop-server embedded))
        (delete-tree! directory)))))

(deftest embedded-nrepl-redefines-the-live-handler-var
  (let [directory (temp-dir)
        port-file (io/file directory ".nrepl-port")
        original @#'tool/handle-clj-change
        embedded (server/start-embedded-nrepl! 0 (.getPath port-file))]
    (try
      (is (some? embedded))
      (is (= (:port embedded) (parse-long (slurp port-file))))
      (with-open [connection (nrepl/connect :port (:port embedded))]
        (let [client (nrepl/client connection 5000)
              code
              (str "(alter-var-root #'clj-surgeon.mcp-tool/handle-clj-change "
                   "(constantly (fn [_ _ callback] "
                   "(callback [\"hot-handler\"] false))))")
              replies (doall (nrepl/message client {:op "eval" :code code}))]
          (is (some #(contains? (set (:status %)) "done") replies))))
      (let [callback-result (atom nil)]
        ((:tool-fn tool/clj-change-tool)
         nil {} (fn [content error?]
                  (reset! callback-result {:content content :error? error?})))
        (is (= {:content ["hot-handler"] :error? false}
               @callback-result)))
      (finally
        (alter-var-root #'tool/handle-clj-change (constantly original))
        (when embedded (nrepl-server/stop-server embedded))
        (delete-tree! directory)))))

(deftest nested-live-server-registration-restores-the-outer-server
  (let [original @runtime/live-tool-state
        outer (Object.)
        inner (Object.)]
    (try
      (reset! runtime/live-tool-state nil)
      (server/register-live-server! outer)
      (server/register-live-server! inner)
      (is (identical? inner (:server @runtime/live-tool-state)))
      (server/unregister-live-server! inner)
      (is (identical? outer (:server @runtime/live-tool-state)))
      (server/unregister-live-server! outer)
      (is (nil? @runtime/live-tool-state))
      (finally
        (reset! runtime/live-tool-state original)))))
