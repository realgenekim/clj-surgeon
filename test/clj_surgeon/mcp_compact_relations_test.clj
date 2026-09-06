(ns ^{:lane :fast} clj-surgeon.mcp-compact-relations-test
  (:require
   [clj-surgeon.core :as core]
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-compact-relations :as relations]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-extraction-plan :as extraction-plan]
   [clj-surgeon.mcp-schema :as schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file FileVisitResult Files LinkOption SimpleFileVisitor)
   (java.nio.file.attribute BasicFileAttributes FileAttribute)))

(def fixture-root
  "bench/fixtures/edit_portfolio/submission-row-extraction-cleanup")

(def relation-files
  ["src/sample/review_updates.clj"
   "src/sample/views/log.clj"
   "src/sample/views/people.clj"
   "src/sample/views/review.clj"
   "test/sample/board_test.clj"
   "test/sample/reviews_test.clj"
   "test/sample/status_workflow_test.clj"
   "test/sample/views_test.clj"
   "test/sample/voting_policy_test.clj"])

(def symbol-migration
  {"target_alias" "submission-row"
   "target_rule" "preserve-name"
   "columns" ["owner" "from" "matches"]
   "files"
   [["src/sample/review_updates.clj"
     [["push-person-row" "view-review/board-row" 1]
      ["push-active-row" "view-review/board-row" 1]]]
    ["src/sample/views/log.clj"
     [["describe-rating" "review/fmt-stars" 3]]]
    ["src/sample/views/people.clj"
     [["reviewer-summary" "review/fmt-mean" 2]
      ["rating-row" "review/fmt-stars" 1]]]
    ["src/sample/views/review.clj"
     [["content-status-control" "chair-on-event?" 1]
      ["submission-detail-page" "score-for-person" 1]
      ["submission-detail-page" "reviewed-by?" 1]
      ["review-summary" "fmt-aggregate" 1]
      ["review-summary" "fmt-mean" 1]
      ["board-page" "chair-on-event?" 1]
      ["board-page" "board-row" 1]]]
    ["test/sample/board_test.clj"
     [["render-unrated" "view-review/board-row" 1]
      ["render-rated" "view-review/board-row" 1]]]
    ["test/sample/reviews_test.clj"
     [["render-result" "review-view/board-row" 1]
      ["render-weighted" "review-view/board-row" 2]
      ["render-visible" "review-view/board-row" 1]]]
    ["test/sample/status_workflow_test.clj"
     [["render-status" "review-view/board-row" 1]]]
    ["test/sample/views_test.clj"
     [["render-opinions" "review/opinions-block" 1]
      ["render-histogram" "review/star-histogram" 1]]]
    ["test/sample/voting_policy_test.clj"
     [["visible-row" "review/board-row" 1]
      ["hidden-row" "review/board-row" 1]
      ["revealed-row" "review/board-row" 1]]]]})

(def require-change
  {"add" {"lib" "sample.views.submission-row" "as" "submission-row"}
   "files"
   [{"file" "src/sample/review_updates.clj"}
    {"file" "src/sample/views/log.clj"
     "remove" {"lib" "sample.views.review" "as" "review"}}
    {"file" "src/sample/views/people.clj"
     "remove" {"lib" "sample.views.review" "as" "review"}}
    {"file" "src/sample/views/review.clj"}
    {"file" "test/sample/board_test.clj"}
    {"file" "test/sample/reviews_test.clj"}
    {"file" "test/sample/status_workflow_test.clj"}
    {"file" "test/sample/views_test.clj"
     "remove" {"lib" "sample.views.review" "as" "review"}}
    {"file" "test/sample/voting_policy_test.clj"}]})

(def bespoke-edit
  {"file" "src/sample/views/review.clj"
   "within" {"form" "detail-controls"}
   "from" (str "(defn detail-controls [event row person mine]\n"
               "  (row-controls* event row person mine "
               "(chair-on-event? event person) false))")
   "to" (str "(defn detail-controls [event row person mine]\n"
             "  (submission-row/row-controls*\n"
             "    event row person mine "
             "(submission-row/chair-on-event? event person) false))")
   "matches" 1})

(def moved-owner-deletion
  {"file" "src/sample/views/review.clj"
   "forms" ["chair-on-event?" "fmt-mean" "fmt-aggregate" "fmt-stars"
            "private-note-block" "star-form" "star-histogram"
            "reviewer-input-controls" "row-controls*" "opinions-block"
            "row-controls" "reviewed-by?" "score-for-person" "board-row"]})

(def relation-request
  {"symbol_migration" symbol-migration
   "require_change" require-change
   "edits" [bespoke-edit]
   "delete_owners" [moved-owner-deletion]})

(def expected-relation-normalization
  {:version 1
   :relations ["symbol_migration" "require_change"]
   :target_rule "preserve-name"
   :files relation-files
   :migration_rows 23
   :require_files 9
   :literal_edits 1
   :deleted_owners 14
   :declared_matches 51
   :expanded_edits 47
   :edit_ids (vec (concat (map #(str "relation/require/" %) (range 9))
                          (map #(str "relation/symbol/" %) (range 23))))})

(def expected-require-edits
  [{"file" "src/sample/review_updates.clj"
    "within" {"namespace" true}
    "from" (str "(:require\n"
                "   [sample.reviews :as reviews]\n"
                "   [sample.views.review :as view-review])")
    "to" (str "(:require\n"
              "   [sample.reviews :as reviews]\n"
              "   [sample.views.review :as view-review]\n"
              "   [sample.views.submission-row :as submission-row])")
    "matches" 1}
   {"file" "src/sample/views/log.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review])"
    "to" "(:require\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}
   {"file" "src/sample/views/people.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review])"
    "to" "(:require\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}
   {"file" "src/sample/views/review.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.events :as events]\n   [sample.store :as store])"
    "to" (str "(:require\n"
              "   [sample.events :as events]\n"
              "   [sample.store :as store]\n"
              "   [sample.views.submission-row :as submission-row])")
    "matches" 1}
   {"file" "test/sample/board_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as view-review])"
    "to" (str "(:require\n"
              "   [sample.views.review :as view-review]\n"
              "   [sample.views.submission-row :as submission-row])")
    "matches" 1}
   {"file" "test/sample/reviews_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review-view])"
    "to" (str "(:require\n"
              "   [sample.views.review :as review-view]\n"
              "   [sample.views.submission-row :as submission-row])")
    "matches" 1}
   {"file" "test/sample/status_workflow_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review-view])"
    "to" (str "(:require\n"
              "   [sample.views.review :as review-view]\n"
              "   [sample.views.submission-row :as submission-row])")
    "matches" 1}
   {"file" "test/sample/views_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review])"
    "to" "(:require\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}
   {"file" "test/sample/voting_policy_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review])"
    "to" (str "(:require\n"
              "   [sample.views.review :as review]\n"
              "   [sample.views.submission-row :as submission-row])")
    "matches" 1}])

(defn- fixture []
  (let [capsule (edn/read-string (slurp (io/file fixture-root "capsule.edn")))]
    {:sources
     (into {}
           (map (fn [file]
                  [file (slurp (io/file fixture-root "before" file))]))
           relation-files)
     :expected-after-hashes
     (into {}
           (map (fn [[file hashes]] [file (:after hashes)]))
           (:hashes capsule))
     :expected-before-hashes
     (into {}
           (map (fn [[file hashes]] [file (:before hashes)]))
           (:hashes capsule))}))

(defn- compiled-file-hashes [compiled]
  (into {} (map (juxt :file :result-hash)) (:files compiled)))

(defn- compile-ordinary-request [sources request]
  (let [validated (contract/validate-tool-params request)
        spec (when (:ok validated)
               (contract/tool-params->transaction (:params validated)))
        prepared (when spec
                   (compact-location/normalize-spec
                     sources spec (:compact-location-normalization validated)))
        compiled (when (and prepared (not (:error prepared)))
                   (transaction/compile-transaction sources (:spec prepared)))]
    {:validated validated :prepared prepared :compiled compiled}))

(defn- expected-symbol-edits []
  (->> (get symbol-migration "files")
       (mapcat
         (fn [[file rows]]
           (map (fn [[owner from matches]]
                  {"file" file
                   "within" {"form" owner}
                   "from" from
                   "to" (str "submission-row/" (name (symbol from)))
                   "matches" matches})
                rows)))
       vec))

(def normalized-flat-request
  {"edits" (vec (concat expected-require-edits
                        (expected-symbol-edits)
                        [bespoke-edit]))
   "delete_owners" [moved-owner-deletion]})

(defn- temp-dir []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-compact-relations-test-"
             (make-array FileAttribute 0))))

(defn- delete-tree! [file]
  (when (.exists (io/file file))
    (Files/walkFileTree
      (.toPath (io/file file))
      (proxy [SimpleFileVisitor] []
        (visitFile [path _attributes]
          (Files/deleteIfExists path)
          FileVisitResult/CONTINUE)
        (postVisitDirectory [path error]
          (when error (throw error))
          (Files/deleteIfExists path)
          FileVisitResult/CONTINUE)))))

(defn- copy-tree! [from to]
  (doseq [source (file-seq (io/file from))]
    (let [relative (.relativize (.toPath (io/file from)) (.toPath source))
          target (.toFile (.resolve (.toPath (io/file to)) relative))]
      (if (.isDirectory source)
        (.mkdirs target)
        (do
          (.mkdirs (.getParentFile target))
          (io/copy source target))))))

(defn- invoke-public-tool! [tool config params]
  (mcp-tool/init! config)
  (let [result (promise)]
    ((:tool-fn tool)
     nil params
     (fn [content error? structured]
       (deliver result {:content content
                        :error? error?
                        :structured structured})))
    @result))

(defn- assert-public-relation-refusal!
  [result error-type failed-stage path]
  (let [{:keys [error? structured]} result
        diagnostic (:compact_relation_diagnostic structured)]
    (is error? (pr-str result))
    (is (false? (:ok structured)) (pr-str structured))
    (is (= error-type (:error_type structured)))
    (is (string? (:error structured)))
    (is (<= (count (:error structured)) 1024))
    (is (= failed-stage (:failed_stage diagnostic)))
    (is (every? #{:failed_stage :path} (keys diagnostic)))
    (cond
      (= ::optional path)
      (when-let [actual-path (:path diagnostic)]
        (is (every? #{:field :file_index :row_index}
                    (keys actual-path))))

      path
      (is (= path (:path diagnostic)))

      :else
      (is (not (contains? diagnostic :path))))
    (is (false? (:mutation_attempted structured)))
    (is (false? (:write_authority structured)))
    (is (= "correct_request" (:next_action structured)))
    (doseq [forbidden [:source :sources :request :generated_request
                       :next_call :terminal_response]]
      (is (not (contains? structured forbidden)) (name forbidden)))))

(deftest paired-relation-schema-is-closed
  ;; @spec MCP-OP-EDIT-020
  ;; @spec MCP-OP-EDIT-026
  ;; @spec MCP-OP-EDIT-027
  (let [valid (relations/compile-source-blind relation-request)]
    (is (:ok valid) (pr-str valid))
    (is (contains? (:properties schema/editor-tool-schema) "symbol_migration"))
    (is (contains? (:properties schema/editor-tool-schema) "require_change"))
    (is (contains? (get-in schema/clj-change-schema [:properties])
                   "symbol_migration"))
    (is (contains? (get-in schema/clj-change-schema [:properties])
                   "require_change"))
    (is (= [{:const "owner"} {:const "from"} {:const "matches"}]
           (get-in schema/symbol-migration-schema
                   [:properties "columns" :prefixItems])))
    (is (= [{:type "string" :minLength 1}
            {:type "string" :minLength 1}
            {:type "integer" :minimum 1 :maximum 128}]
           (get-in schema/symbol-migration-schema
                   [:properties "files" :items :prefixItems 1
                    :items :prefixItems])))
    (is (= (:allOf schema/editor-hybrid-schema)
           (:allOf schema/clj-change-schema)))
    (is (= [{:not
             {:oneOf
              [{:required ["symbol_migration"]
                :not {:required ["require_change"]}}
               {:required ["require_change"]
                :not {:required ["symbol_migration"]}}]}}]
           (:allOf schema/editor-hybrid-schema)))
    (doseq [description [mcp-tool/edit-tool-description
                         mcp-tool/tool-description]]
      (is (str/includes? description "symbol_migration"))
      (is (str/includes? description "require_change"))))
  (doseq [[label request path]
          [[:missing-pair (dissoc relation-request "require_change")
            ["require_change"]]
           [:null-relation (assoc relation-request "symbol_migration" nil)
            ["symbol_migration"]]
           [:unknown-nested
            (assoc-in relation-request ["symbol_migration" "unexpected"] true)
            ["symbol_migration" "unexpected"]]
           [:wrong-file-tuple-arity
            (update-in relation-request ["symbol_migration" "files" 0] pop)
            ["symbol_migration" "files" 0]]
           [:wrong-row-arity
            (update-in relation-request ["symbol_migration" "files" 0 1 0] pop)
            ["symbol_migration" "files" 0 1 0]]
           [:mismatched-file-order
            (update-in relation-request ["require_change" "files"]
                       #(vec (reverse %)))
            ["require_change" "files"]]
           [:alias-mismatch
            (assoc-in relation-request ["require_change" "add" "as"] "other")
            ["require_change" "add" "as"]]
           [:duplicate-row
            (update-in relation-request ["symbol_migration" "files" 0 1]
                       #(conj % (first %)))
            ["symbol_migration" "files" 0 1 2]]
           [:disallowed-route (assoc relation-request "changes" []) ["changes"]]
           [:legacy-mask (assoc relation-request "symbol_rewrites" {})
            ["symbol_rewrites"]]
           [:multiply-qualified-symbol
            (assoc-in relation-request
                      ["symbol_migration" "files" 0 1 0 1]
                      "one/two/three")
            ["symbol_migration" "files" 0 1 0 1]]]]
    (testing (name label)
      (let [result (relations/compile-source-blind request)]
        (is (false? (:ok result)) (pr-str result))
        (is (= :invalid-compact-relation (:error-type result)))
        (is (= :relation-admission (:failed-stage result)))
        (is (= path (:path result)))
        (is (false? (:mutation-attempted result)))
        (is (false? (:write-authority result)))
        (is (not (contains? result :request))))))
  (testing "the advertised schema denies malformed or partial relations"
    (is (:ok (admission/authorize schema/editor-tool-schema
                                  relation-request)))
    (doseq [request
            [(dissoc relation-request "require_change")
             (assoc-in relation-request
                       ["symbol_migration" "columns"] ["x" "y" "z"])
             (assoc-in relation-request
                       ["symbol_migration" "files" 0 1 0] [42])
             (assoc-in relation-request
                       ["symbol_migration" "files" 0 1 0 0] 42)
             (assoc-in relation-request
                       ["symbol_migration" "files" 0 1 0 1] 42)
             (assoc-in relation-request
                       ["symbol_migration" "files" 0 1 0 2] 0)]]
      (is (false? (:ok (admission/authorize
                         schema/editor-tool-schema request)))
          (pr-str request))))
  (testing "public reader forms refuse without evaluation"
    (doseq [request
            (concat
              [(assoc-in relation-request
                         ["symbol_migration" "files" 0 1 0 1]
                         "#=(println \"READER-EVAL-EXECUTED\")")
               (assoc-in relation-request ["require_change" "add" "lib"]
                         "#=(println \"READER-EVAL-EXECUTED\")")]
              (mapcat
                (fn [token]
                  [(assoc-in relation-request
                             ["symbol_migration" "files" 0 1 0 1]
                             token)
                   (assoc-in relation-request
                             ["symbol_migration" "target_alias"] token)
                   (assoc-in relation-request
                             ["require_change" "add" "lib"] token)])
                ["nil" "true" "false"]))]
      (let [result (atom nil)
            output (with-out-str
                     (reset! result (relations/compile-source-blind request)))]
        (is (empty? output))
        (is (false? (:ok @result))))))
  (let [workspace (temp-dir)
        config {:project-root (.getPath workspace)}
        invalid (dissoc relation-request "require_change")]
    (try
      (doseq [tool [mcp-tool/edit-clojure-tool mcp-tool/clj-change-tool]]
        (assert-public-relation-refusal!
          (invoke-public-tool! tool config invalid)
          "invalid-compact-relation"
          "relation-admission"
          {:field "require_change"}))
      (finally
        (mcp-tool/init! nil)
        (delete-tree! workspace)))))

(deftest preserve-name-relation-lowers-exact-rows
  ;; @spec MCP-OP-EDIT-021
  (let [result (relations/compile-source-blind relation-request)
        generated (:generated-symbol-edits result)
        request (:request result)]
    (is (:ok result) (pr-str result))
    (is (= relation-files (:relation-files result)))
    (is (= 23 (count generated)))
    (is (= 27 (reduce + (map #(get % "matches") generated))))
    (is (= (expected-symbol-edits) generated))
    (is (= require-change (:pending-require result)))
    (is (= [bespoke-edit] (get request "edits")))
    (is (= [moved-owner-deletion] (get request "delete_owners")))
    (is (not (contains? request "symbol_migration")))
    (is (not (contains? request "require_change")))
    (is (= 23 (get-in result [:relation-normalization :migration_rows])))
    (is (= 27 (get-in result [:relation-normalization :declared_matches]))))
  (testing "Phase A declares literal and deletion files outside the relation pair without I/O"
    (let [literal-file "src/phase_a_only/literal.clj"
          deletion-file "src/phase_a_only/deletion.clj"
          request
          (-> relation-request
              (update "edits" conj
                      {"file" literal-file
                       "within" {"form" "literal-owner"}
                       "from" ":old"
                       "to" ":new"
                       "matches" 1})
              (update "delete_owners" conj
                      {"file" deletion-file
                       "forms" ["deleted-owner"]}))
          result (relations/compile-source-blind request)]
      (is (:ok result) (pr-str result))
      (is (= (vec (concat relation-files [literal-file deletion-file]))
             (:declared-files result)))
      (is (= literal-file
             (get-in result [:request "edits" 1 "file"])))
      (is (= deletion-file
             (get-in result [:request "delete_owners" 1 "file"]))))))
(testing "grouped literal edits contribute matches for every declared file"
  (let [{:keys [sources]} (fixture)
        request
        (update relation-request "edits" conj
                {"files" ["src/extra/a.clj" "src/extra/b.clj"]
                 "within" {"root" true}
                 "from" ":old"
                 "to" ":new"
                 "matches" 2})
        result (relations/compile-frozen
                 sources (relations/compile-source-blind request))]
    (is (:ok result) (pr-str result))
    (is (= 55
           (get-in result
                   [:relation-normalization :declared_matches])))
    (is (= 48
           (get-in result
                   [:relation-normalization :expanded_edits])))))

(deftest require-delta-lowers-from-one-frozen-map
  ;; @spec MCP-OP-EDIT-022
  (let [{:keys [sources]} (fixture)
        source-blind (relations/compile-source-blind relation-request)
        result (relations/compile-frozen sources source-blind)]
    (is (:ok result) (pr-str result))
    (is (= expected-require-edits (:generated-require-edits result)))
    (is (= 9 (count (:generated-require-edits result))))
    (is (= 3 (count (filter #(get % "remove")
                            (get require-change "files")))))
    (is (not (contains? (:request result) "require_change")))
    (is (= 9 (get-in result [:relation-normalization :require_files])))
    (doseq [[label changed]
            [[:missing-source (dissoc sources (first relation-files))]
             [:target-already-present
              (update sources (first relation-files)
                      str/replace
                      (get (first expected-require-edits) "from")
                      (get (first expected-require-edits) "to"))]
             [:commented-layout
              (update sources (first relation-files)
                      str/replace
                      "[sample.reviews :as reviews]"
                      "[sample.reviews :as reviews] ;; keep")]
             [:reader-conditional
              (assoc sources (first relation-files)
                     "(ns sample.review-updates (:require #?(:clj [sample.reviews :as reviews])))")]]]
      (testing (name label)
        (let [refusal (relations/compile-frozen changed source-blind)]
          (is (false? (:ok refusal)) (pr-str refusal))
          (is (= :require-change-unprovable (:error-type refusal)))
          (is (= :require-lowering (:failed-stage refusal)))
          (is (not (contains? refusal :request)))))))
  (testing "a require-lowering refusal keeps the public envelope closed"
    (let [workspace (temp-dir)
          receipt-dir (io/file workspace "receipts")
          source-file (io/file workspace (first relation-files))]
      (try
        (copy-tree! (io/file fixture-root "before") workspace)
        (spit source-file
              (str/replace (slurp source-file)
                           "[sample.reviews :as reviews]"
                           "[sample.reviews :as reviews] ;; keep"))
        (assert-public-relation-refusal!
          (invoke-public-tool!
            mcp-tool/clj-change-tool
            {:project-root (.getPath workspace)
             :receipt-dir (.getPath receipt-dir)}
            relation-request)
          "require-change-unprovable"
          "require-lowering"
          {:field "require_change" :file_index 0})
        (is (empty? (or (seq (.listFiles receipt-dir)) [])))
        (finally
          (mcp-tool/init! nil)
          (delete-tree! workspace)))))
  (testing "the existing resolver rejects escape and alias spellings before capture"
    (let [workspace (temp-dir)
          receipt-dir (io/file workspace "receipts")
          target (io/file workspace (first relation-files))
          symlink-path "src/sample/review_updates_alias.clj"
          symlink-file (io/file workspace symlink-path)
          external-edit
          (fn [file]
            {"file" file
             "within" {"form" "outside"}
             "from" ":old"
             "to" ":new"
             "matches" 1})]
      (try
        (copy-tree! (io/file fixture-root "before") workspace)
        (Files/createSymbolicLink
          (.toPath symlink-file)
          (.getFileName (.toPath target))
          (make-array FileAttribute 0))
        (doseq [[label request expected-error expected-path]
                [[:root-escape
                  (update relation-request "edits" conj
                          (external-edit "../outside.clj"))
                  "invalid-mcp-request"
                  {:field "edits" :file_index 1}]
                 [:lexical-canonical-alias
                  (update relation-request "edits" conj
                          (external-edit
                            "src/sample/./review_updates.clj"))
                  "invalid-mcp-request"
                  nil]
                 [:symlink-canonical-alias
                  (-> relation-request
                      (update-in ["symbol_migration" "files"] conj
                                 [symlink-path
                                  [["push-person-row"
                                    "view-review/board-row" 1]]])
                      (update-in ["require_change" "files"] conj
                                 {"file" symlink-path}))
                  "compact-relation-path-conflict"
                  nil]]]
          (testing (name label)
            (let [result
                  (mcp-tool/execute-request!
                    {:project-root (.getPath workspace)
                     :receipt-dir (.getPath receipt-dir)}
                    request)]
              (is (false? (:ok result)) (pr-str result))
              (is (= expected-error (:error_type result)))
              (is (string? (:error result)))
              (is (<= (count (:error result)) 1024))
              (if (= "compact-relation-path-conflict" expected-error)
                (do
                  (is (= "path-resolution"
                         (get-in result
                                 [:compact_relation_diagnostic
                                  :failed_stage])))
                  (is (every? #{:failed_stage :path}
                              (keys (:compact_relation_diagnostic result))))
                  (when-let [path
                             (get-in result
                                     [:compact_relation_diagnostic :path])]
                    (is (every? #{:field :file_index :row_index}
                                (keys path))))
                  (when expected-path
                    (is (= expected-path
                           (get-in result
                                   [:compact_relation_diagnostic :path]))))
                  (is (false? (:mutation_attempted result)))
                  (is (false? (:write_authority result)))
                  (is (= "correct_request" (:next_action result))))
                (do
                  (is (:source_unchanged result))
                  (is (not (contains? result
                                      :compact_relation_diagnostic)))))
              (doseq [forbidden [:source :sources :request :generated_request
                                 :next_call :terminal_response]]
                (is (not (contains? result forbidden)) (name forbidden)))
              (is (empty? (or (seq (.listFiles receipt-dir)) []))))))
        (testing "one symlink spelling publishes only canonical relation evidence"
          (let [request (-> relation-request
                            (assoc-in ["symbol_migration" "files" 0 0]
                                      symlink-path)
                            (assoc-in ["require_change" "files" 0 "file"]
                                      symlink-path))
                {:keys [error? structured] :as public-result}
                (invoke-public-tool!
                  mcp-tool/edit-clojure-tool
                  {:project-root (.getPath workspace)
                   :receipt-dir (.getPath receipt-dir)}
                  request)]
            (is (false? error?) (pr-str public-result))
            (is (= relation-files
                   (get-in structured
                           [:compact_relation_normalization :files])))
            (is (= (set relation-files)
                   (set (keys (:read_back_hashes structured)))))
            (is (:ok (transaction/execute-undo!
                       {:receipt (:undo_receipt structured)})))))
        (finally
          (mcp-tool/init! nil)
          (delete-tree! workspace))))))

(deftest closed-relations-compose-to-frozen-future
  ;; @spec MCP-OP-EDIT-023
  ;; @spec MCP-OP-EDIT-025
  (let [{:keys [sources expected-after-hashes]} (fixture)
        source-blind (relations/compile-source-blind relation-request)
        frozen (relations/compile-frozen sources source-blind)
        {:keys [validated compiled]}
        (compile-ordinary-request sources (:request frozen))]
    (is (:ok source-blind) (pr-str source-blind))
    (is (:ok frozen) (pr-str frozen))
    (is (:ok validated) (pr-str validated))
    (is (:ok compiled) (pr-str compiled))
    (is (= 51 (:match-count compiled)))
    (is (= 9 (:changed-file-count compiled)))
    (is (= expected-after-hashes (compiled-file-hashes compiled)))
    (is (= expected-relation-normalization
           (:relation-normalization frozen)))))

(deftest relation-overlap-refuses-atomically
  ;; @spec MCP-OP-EDIT-023
  (let [{:keys [sources]} (fixture)
        first-row (get-in relation-request ["symbol_migration" "files" 0 1 0])
        duplicate-row
        (update-in relation-request ["symbol_migration" "files" 0 1]
                   conj first-row)
        stale-count
        (assoc-in relation-request ["symbol_migration" "files" 0 1 0 2] 2)
        overlapping-literal
        (update relation-request "edits" conj
                {"file" "src/sample/review_updates.clj"
                 "within" {"form" "push-person-row"}
                 "from" "view-review/board-row"
                 "to" "submission-row/board-row"
                 "matches" 1})]
    (doseq [[label request] [[:duplicate duplicate-row]
                             [:stale-count stale-count]
                             [:literal-overlap overlapping-literal]]]
      (testing (name label)
        (let [before sources
              source-blind (relations/compile-source-blind request)
              frozen (when (:ok source-blind)
                       (relations/compile-frozen sources source-blind))
              compiled (when (:ok frozen)
                         (:compiled
                           (compile-ordinary-request sources (:request frozen))))]
          (is (or (false? (:ok source-blind))
                  (false? (:ok frozen))
                  (:error compiled))
              (pr-str [source-blind frozen compiled]))
          (is (= before sources)))))
    (testing "composition overlap refuses through the public boundary"
      (let [workspace (temp-dir)
            receipt-dir (io/file workspace "receipts")]
        (try
          (copy-tree! (io/file fixture-root "before") workspace)
          (assert-public-relation-refusal!
            (invoke-public-tool!
              mcp-tool/edit-clojure-tool
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)}
              overlapping-literal)
            "compact-relation-overlap"
            "relation-composition"
            ::optional)
          (is (empty? (or (seq (.listFiles receipt-dir)) [])))
          (finally
            (mcp-tool/init! nil)
            (delete-tree! workspace)))))))

(deftest mcp-relation-mutation-is-undoable-and-exact-verifiable
  ;; @spec MCP-OP-EDIT-024
  ;; @spec MCP-OP-EDIT-030
  (let [{:keys [expected-after-hashes expected-before-hashes]} (fixture)
        normalizations (atom [])
        identities (atom [])]
    (is (= expected-require-edits
           (subvec (get normalized-flat-request "edits") 0 9))
        "the retained flat corpus emits require rows first")
    (is (= (expected-symbol-edits)
           (subvec (get normalized-flat-request "edits") 9 32))
        "the retained flat corpus emits symbol rows second")
    (doseq [[label tool operation request exact? relation?]
            [[:normalized-flat mcp-tool/clj-change-tool
              "apply_clojure_changes"
              (assoc normalized-flat-request "verify" "exact") true false]
             [:relation-edit mcp-tool/edit-clojure-tool "edit_clojure"
              relation-request false true]
             [:relation-compact mcp-tool/clj-change-tool
              "apply_clojure_changes"
              (assoc relation-request "verify" "exact") true true]]]
      (testing (name label)
        (let [workspace (temp-dir)
              receipt-dir (io/file workspace "receipts")
              config
              {:project-root (.getPath workspace)
               :receipt-dir (.getPath receipt-dir)
               :verification-profile-source :project
               :verification-profiles
               {"exact" {:acceptance :exact-exit
                         :timeout-ms 120000
                         :commands [["/usr/bin/true"]]}}}]
          (try
            (copy-tree! (io/file fixture-root "before") workspace)
            (let [capture-var
                  #'clj-surgeon.intent-transaction/read-sources
                  original-capture @capture-var
                  capture-calls (atom [])
                  redefinitions
                  (cond->
                    {capture-var
                     (fn [files]
                       (swap! capture-calls conj (vec files))
                       (original-capture files))}
                    (not relation?)
                    (assoc #'relations/compile-source-blind
                           (fn [& _]
                             (throw
                               (ex-info
                                 "flat corpus must not invoke relation lowering"
                                 {})))))
                  {:keys [error? structured] :as public-result}
                  (with-redefs-fn
                    redefinitions
                    #(invoke-public-tool! tool config request))
                  receipt-file (when (string? (:undo_receipt structured))
                                 (io/file (:undo_receipt structured)))
                  receipt (when (and receipt-file (.isFile receipt-file))
                            (edn/read-string (slurp receipt-file)))]
              (is (false? error?) (pr-str public-result))
              (is (:ok structured) (pr-str structured))
              (is (= operation (:operation structured)))
              (is (:verification_complete structured))
              (is (= "none" (:next_action structured)))
              (if relation?
                (do
                  (is (= (set (keys expected-relation-normalization))
                         (set (keys
                                (:compact_relation_normalization structured)))))
                  (is (= expected-relation-normalization
                         (:compact_relation_normalization structured)))
                  (swap! normalizations conj
                         (:compact_relation_normalization structured)))
                (is (not (contains? structured
                                    :compact_relation_normalization))))
              (is (= #{:version :sha256 :files :effects}
                     (set (keys (:canonical_effect_identity structured)))))
              (is (= 9 (get-in structured
                               [:canonical_effect_identity :files])))
              (is (= 51 (get-in structured
                                [:canonical_effect_identity :effects])))
              (is (re-matches
                    #"[0-9a-f]{64}"
                    (get-in structured
                            [:canonical_effect_identity :sha256])))
              (is (= 1 (count @capture-calls))
                  (str "one transaction capture must feed Phase B: "
                       (pr-str @capture-calls)))
              (swap! identities conj
                     (:canonical_effect_identity structured))
              (is (= expected-after-hashes (:read_back_hashes structured)))
              (is (and (string? (:receipt_hash structured))
                       (re-matches #"[0-9a-f]{64}"
                                   (:receipt_hash structured))))
              (is (and receipt-file (.isFile receipt-file)))
              (let [workspace-path (.toRealPath (.toPath workspace)
                                                (make-array LinkOption 0))
                    relative-file
                    #(str (.relativize workspace-path
                                       (.toRealPath
                                         (.toPath (io/file (:file %)))
                                         (make-array LinkOption 0))))]
                (is (= (set relation-files)
                       (set (map relative-file (:files receipt)))))
                (is (= expected-before-hashes
                       (into {}
                             (map (juxt relative-file :source-hash))
                             (:files receipt))))
                (is (= expected-after-hashes
                       (into {}
                             (map (juxt relative-file :result-hash))
                             (:files receipt)))))
              (when exact?
                (is (= :exact-exit
                       (get-in structured [:verification :acceptance])))
                (is (= :project
                       (get-in structured [:verification :profile-source])))
                (is (= :pass
                       (get-in structured [:verification :process-outcome])))
                (is (= 0 (get-in structured [:verification :exit])))
                (is (= ["/usr/bin/true"]
                       (get-in structured [:verification :argv])))
                (is (= 0
                       (get-in structured [:verification :output-bytes])))
                (is (number?
                      (get-in structured [:verification :elapsed_ms])))
                (is (and
                      (string? (get-in structured
                                       [:verification :profile-sha256]))
                      (re-matches
                        #"[0-9a-f]{64}"
                        (get-in structured
                                [:verification :profile-sha256]))))
                (is (and
                      (string? (get-in structured
                                       [:verification :output-sha256]))
                      (re-matches
                        #"[0-9a-f]{64}"
                        (get-in structured
                                [:verification :output-sha256]))))
                (is (false?
                      (get-in structured [:verification :output-truncated]))))
              (doseq [file relation-files]
                (is (= (slurp (io/file fixture-root "after" file))
                       (slurp (io/file workspace file)))))
              (is (:ok (transaction/execute-undo!
                         {:receipt (:undo_receipt structured)})))
              (doseq [file relation-files]
                (is (= (slurp (io/file fixture-root "before" file))
                       (slurp (io/file workspace file))))))
            (finally
              (mcp-tool/init! nil)
              (delete-tree! workspace))))))
    (is (= 2 (count @normalizations)))
    (is (apply = @normalizations))
    (is (= 3 (count @identities)))
    (is (apply = @identities)
        "normalized-flat and relation requests share one effect identity across entrances"))
  (testing "edit_clojure refuses verification before capture"
    (let [workspace (temp-dir)
          receipt-dir (io/file workspace "receipts")
          calls (atom 0)]
      (try
        (copy-tree! (io/file fixture-root "before") workspace)
        (let [{:keys [error? structured]}
              (with-redefs [relations/compile-source-blind
                            (fn [& _]
                              (swap! calls inc)
                              (throw (ex-info "must not lower" {})))]
                (invoke-public-tool!
                  mcp-tool/edit-clojure-tool
                  {:project-root (.getPath workspace)
                   :receipt-dir (.getPath receipt-dir)}
                  (assoc relation-request "verify" "exact")))]
          (is error?)
          (is (= "edit_clojure" (:operation structured)))
          (is (= "invalid-mcp-request" (:error_type structured)))
          (is (false? (:mutation_attempted structured)))
          (is (false? (:write_authority structured)))
          (is (zero? @calls))
          (is (empty? (or (seq (.listFiles receipt-dir)) []))))
        (finally
          (mcp-tool/init! nil)
          (delete-tree! workspace)))))
  (testing "a verifier failure rolls back and keeps verifier authority"
    (let [workspace (temp-dir)
          receipt-dir (io/file workspace "receipts")]
      (try
        (copy-tree! (io/file fixture-root "before") workspace)
        (let [result
              (mcp-tool/execute-request!
                {:project-root (.getPath workspace)
                 :receipt-dir (.getPath receipt-dir)
                 :verification-profile-source :project
                 :verification-profiles
                 {"exact" {:acceptance :exact-exit
                           :timeout-ms 120000
                           :commands [["/usr/bin/false"]]}}}
                (assoc relation-request "verify" "exact"))]
          (is (false? (:ok result)))
          (is (= "verification-failed" (:error_type result)))
          (is (= :ordinary-nonzero
                 (get-in result [:verification :process-outcome])))
          (is (not (contains? result :canonical_effect_identity)))
          (is (:rolled_back result))
          (is (not (contains? result :compact_relation_diagnostic)))
          (doseq [file relation-files]
            (is (= (slurp (io/file fixture-root "before" file))
                   (slurp (io/file workspace file))))))
        (finally
          (delete-tree! workspace))))))

(deftest stale-source-refuses-before-write
  ;; @spec MCP-OP-EDIT-023
  ;; @spec MCP-OP-EDIT-024
  (let [{:keys [sources]} (fixture)
        source-blind (relations/compile-source-blind relation-request)
        frozen (relations/compile-frozen sources source-blind)
        compiled (:compiled (compile-ordinary-request sources (:request frozen)))
        stale-file (first relation-files)
        competing-source (str (get sources stale-file) "\n;; competing writer\n")
        observed-sources (assoc sources stale-file competing-source)
        writes (atom [])
        result
        (transaction/commit-compiled!
          compiled
          {:read-source #(get observed-sources %)
           :write-source! (fn [file source]
                            (swap! writes conj [file source]))})]
    (is (:ok compiled) (pr-str compiled))
    (is (not (:ok result)) (pr-str result))
    (is (= :source-hash-mismatch (:error-type result)))
    (is (= stale-file (:file result)))
    (is (empty? @writes))
    (is (= competing-source (get observed-sources stale-file)))
    (is (= (dissoc sources stale-file)
           (dissoc observed-sources stale-file)))))

(deftest nonrelation-routes-never-call-lowerer
  ;; @spec MCP-OP-EDIT-025
  (let [calls (atom [])
        fail-if-called
        (fn [& args]
          (swap! calls conj args)
          (throw (ex-info "relation lowerer must not run" {})))
        workspace (temp-dir)
        receipt-dir (io/file workspace "receipts")
        source-file (io/file workspace "src/sample/app.clj")
        cljc-file (io/file workspace "src/sample/cross.cljc")]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file "(ns sample.app)\n(defn f [] :old)\n")
      (spit cljc-file
            "(ns sample.cross #?(:clj (:require [clojure.string :as str])))\n(defn f [] :old)\n")
      (with-redefs [relations/compile-source-blind fail-if-called]
        (doseq [[label tool]
                [[:edit-clojure-entrance mcp-tool/edit-clojure-tool]
                 [:compact-apply-entrance mcp-tool/clj-change-tool]]]
          (testing (name label)
            (let [{:keys [error? structured] :as public-result}
                  (invoke-public-tool!
                    tool
                    {:project-root (.getPath workspace)
                     :receipt-dir (.getPath receipt-dir)}
                    {"edits" [{"file" "src/sample/app.clj"
                               "within" {"form" "f"}
                               "from" ":old" "to" ":new"}]})]
              (is (false? error?) (pr-str public-result))
              (is (:ok structured) (pr-str structured))
              (is (not (contains? structured
                                  :compact_relation_normalization)))
              (is (:ok (transaction/execute-undo!
                         {:receipt (:undo_receipt structured)}))))))
        (testing "generic changes"
          (let [result
                (mcp-tool/execute-request!
                  {:project-root (.getPath workspace)
                   :receipt-dir (.getPath receipt-dir)}
                  {"changes" [{"id" "generic"
                               "files" ["src/sample/app.clj"]
                               "forms" ["f"]
                               "find" ":old"
                               "replace" ":new"
                               "expect" {"matches" 1 "each_form" 1}}]
                   "expect" {"changes" 1 "edits" 1 "files" 1}})]
            (is (:ok result) (pr-str result))
            (is (not (contains? result :canonical_effect_identity)))
            (is (:ok (transaction/execute-undo!
                       {:receipt (:undo_receipt result)})))))
        (testing "programs refuse on their own route"
          (is (false? (:ok (mcp-tool/execute-request!
                             {:project-root (.getPath workspace)}
                             {"programs" [{"file" "src/sample/app.clj"
                                           "expression" "(form 'f)"
                                           "expect" {"matches" 1
                                                     "max_changed_characters" 1}}]})))))
        (testing "retained basis executes on its own route"
          (change-buffer/clear-bases!)
          (let [prepared
                (change-buffer/prepare-change!
                  {:project-root (.getPath workspace)}
                  {:file "src/sample/app.clj" :form "f" :intent "change f"})
                site (get-in prepared [:decision-sites 0 :id])
                result
                (mcp-tool/execute-request!
                  {:project-root (.getPath workspace)
                   :receipt-dir (.getPath receipt-dir)
                   :verify! (fn [_ _ _ files] {:ok true :files files})}
                  {"basis" (:basis prepared)
                   "decisions" [{"site" site
                                 "replace" "(defn f [] :basis)"}]})]
            (is (:ok prepared) (pr-str prepared))
            (is (:ok result) (pr-str result))
            (is (not (contains? result :canonical_effect_identity)))
            (is (:ok (transaction/execute-undo!
                       {:receipt (:receipt-file result)})))))
        (testing "CLI preview does not enter the MCP relation facade"
          (let [result (atom nil)]
            (with-out-str
              (reset! result
                      (core/run
                        {:op :change
                         :spec
                         {:intents [{:files [(.getPath source-file)]
                                     :from ":old"
                                     :to ":cli"
                                     :expect-count 1}]
                          :expect {:intent-count 1
                                   :edit-count 1
                                   :changed-file-count 1}}})))
            (is (:ok @result) (pr-str @result))))
        (testing "extraction planning remains a separate read-only route"
          (is (map?
                (extraction-plan/plan!
                  {:project-root (.getPath workspace)}
                  {:mode "plan-extraction"
                   :file "src/sample/app.clj"
                   :to "src/sample/planned.clj"
                   :forms ["f"]
                   :require_policy "minimal"}))))
        (testing "extraction proceeds on its own route"
          (is (map? (mcp-tool/execute-request!
                      {:project-root (.getPath workspace)}
                      {"extraction" {"file" "src/sample/app.clj"
                                     "to" "src/sample/moved.clj"
                                     "forms" ["f"]
                                     "require_policy" "minimal"}}))))
        (testing "relation-absent CLJC does not activate relation lowering"
          (is (map? (mcp-tool/execute-request!
                      {:project-root (.getPath workspace)
                       :receipt-dir (.getPath receipt-dir)}
                      {"edits" [{"file" "src/sample/cross.cljc"
                                 "within" {"form" "f"}
                                 "from" ":old" "to" ":new"}]})))))
      (is (empty? @calls))
      (finally
        (change-buffer/clear-bases!)
        (mcp-tool/init! nil)
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; text ⊇ structured for compact-relation refusals
;;
;; Field finding (fan-out B, D1/D2, 2026-09-06): apply_clojure_changes refused
;; with error_type invalid-compact-relation and structuredContent carrying
;; `error "Each migration file must be [file, rows]"`, but the TEXT block the
;; model reads named only the error_type, the path, and the remedy. The shape
;; was invisible, and a fresh session retried the same wrong shape eight times.
;; The 2026-09-03 "text ⊇ structured" ratchet did not reach this refusal class.

(defn- refusal-text
  "The visible text block a caller receives for one compact-relation refusal."
  [request]
  (let [normalized (assoc (contract/normalize-refusal
                            (relations/compile-source-blind request))
                          :elapsed_ms 3.29)]
    {:structured normalized
     :text (mcp-tool/concise-summary normalized)}))

(def ^:private d1-bare-file-request
  "The exact D1 shape: a `files` entry that is a bare path string, not [file rows]."
  (assoc-in relation-request ["symbol_migration" "files" 0]
            "src/sample/review_updates.clj"))

(deftest refusal-text-carries-the-structured-error-sentence
  ;; @spec MCP-OP-EDIT-037
  (testing "every enumerable compact-relation admission refusal shows its error sentence"
    (doseq [[label request]
            [[:d1-bare-file-entry d1-bare-file-request]
             [:missing-pair (dissoc relation-request "require_change")]
             [:null-relation (assoc relation-request "symbol_migration" nil)]
             [:unknown-nested
              (assoc-in relation-request ["symbol_migration" "unexpected"] true)]
             [:wrong-file-tuple-arity
              (update-in relation-request ["symbol_migration" "files" 0] pop)]
             [:wrong-row-arity
              (update-in relation-request ["symbol_migration" "files" 0 1 0] pop)]
             [:empty-rows
              (assoc-in relation-request ["symbol_migration" "files" 0 1] [])]
             [:duplicate-file
              (update-in relation-request ["symbol_migration" "files"]
                         #(assoc % 1 (first %)))]
             [:mismatched-file-order
              (update-in relation-request ["require_change" "files"]
                         (comp vec reverse))]
             [:alias-mismatch
              (assoc-in relation-request ["require_change" "add" "as"] "other")]
             [:duplicate-row
              (update-in relation-request ["symbol_migration" "files" 0 1]
                         #(conj % (first %)))]
             [:disallowed-route (assoc relation-request "changes" [])]
             [:legacy-mask (assoc relation-request "symbol_rewrites" {})]
             [:multiply-qualified-symbol
              (assoc-in relation-request
                        ["symbol_migration" "files" 0 1 0 1] "one/two/three")]
             [:bad-target-rule
              (assoc-in relation-request
                        ["symbol_migration" "target_rule"] "rename")]
             [:bad-columns
              (assoc-in relation-request
                        ["symbol_migration" "columns"] ["a" "b" "c"])]]]
      (testing (name label)
        (let [{:keys [structured text]} (refusal-text request)
              sentence (:error structured)]
          (is (false? (:ok structured)) (pr-str structured))
          (is (string? sentence))
          (is (str/includes? text sentence)
              (str "text must carry the structured error sentence "
                   (pr-str sentence) " · text was:\n" text)))))))

(deftest d1-bare-file-entry-names-the-shape-and-one-filled-example
  ;; @spec MCP-OP-EDIT-037
  (let [{:keys [structured text]} (refusal-text d1-bare-file-request)
        example (:expected_shape_example structured)]
    (testing "the refusal is still the same typed, source-safe refusal"
      (is (= "invalid-compact-relation" (:error_type structured)))
      (is (= ["symbol_migration" "files" 0] (:path structured)))
      (is (true? (:source_unchanged structured))))
    (testing "structuredContent gains one bounded, filled example"
      (is (string? example))
      (is (<= (count example) 200))
      (is (str/includes? example "src/sample/review_updates.clj"))
      (is (= example (pr-str (edn/read-string example))))
      (let [[file rows] (edn/read-string example)]
        (is (string? file))
        (is (= 1 (count rows)))
        (is (= 3 (count (first rows))))))
    (testing "the text block carries both the sentence and the example"
      (is (str/includes? text "Each migration file must be [file, rows]"))
      (is (str/includes? text (str "expected: " example)))
      (is (str/includes?
            text
            (str "  refused · invalid-compact-relation"
                 " at [\"symbol_migration\" \"files\" 0] · 3.29 ms\n"
                 "  Each migration file must be [file, rows]\n"
                 "  expected: " example "\n"))))))

(deftest refusal-without-an-error-sentence-renders-unchanged
  ;; @spec MCP-OP-EDIT-037
  ;; The generic rule adds nothing when the receipt carries no error sentence.
  (is (= (str "apply_clojure_changes\n"
              "  refused · invalid-intent-form · 2.50 ms\n"
              "  change 0 · gallery-resolver · field :find\n\n"
              "✓ source unchanged\n"
              "→ Pass exactly one complete parseable Clojure form in :find for change 0 (gallery-resolver).")
         (mcp-tool/concise-summary
           {:ok false
            :error_type "invalid-intent-form"
            :reason "invalid-intent-form"
            :elapsed_ms 2.5
            :change_index 0
            :change_id "gallery-resolver"
            :field :find
            :source_unchanged true
            :remedy "Pass exactly one complete parseable Clojure form in :find for change 0 (gallery-resolver)."})))
  (testing "a placeholder error sentence adds no line either"
    (is (not (str/includes?
               (mcp-tool/concise-summary
                 {:ok false
                  :error_type "verification-failed"
                  :error "apply_clojure_changes refused"
                  :elapsed_ms 1.25
                  :rolled-back true})
               "\n  apply_clojure_changes refused\n")))))

;; ---------------------------------------------------------------------------
;; The example is BOUNDED and TOTAL.
;;
;; Fence r1 (Sol, 2026-09-06) found the first cut of this change returned nil
;; whenever the rendered example exceeded 200 characters: a 228-character caller
;; path made `expected_shape_example` vanish entirely, which is precisely the
;; state the field exists to prevent. A ceiling must shorten the example, never
;; delete it.

(defn- d1-request-with-path
  "The D1 shape (a bare path string where [file rows] belongs) at one exact path."
  [path]
  (assoc-in relation-request ["symbol_migration" "files" 0] path))

(defn- example-for-path [path]
  (:expected_shape_example (:structured (refusal-text (d1-request-with-path path)))))

(defn- ascii-path-rendering-an-example-of-length
  "An ASCII path chosen so the rendered example is exactly `total` characters.
   ASCII needs no pr-str escaping, so one path character is one rendered
   character and the offset is exact rather than approximate."
  [total]
  (let [probe "src/a.clj"
        base (count (example-for-path probe))
        pad (- total base)]
    (assert (pos? pad) "probe example is already longer than the target")
    (str "src/" (apply str (repeat (inc pad) \a)) ".clj")))

(deftest expected-shape-example-is-bounded-at-the-ceiling-boundary
  ;; @spec MCP-OP-EDIT-037
  (testing "exactly at the ceiling the caller's own path is quoted whole"
    (let [path (ascii-path-rendering-an-example-of-length 200)
          example (example-for-path path)]
      (is (string? example))
      (is (= 200 (count example)))
      (is (str/includes? example path))
      (is (not (str/includes? example "…")))
      (is (= example (pr-str (edn/read-string example))))))
  (testing "one character over the ceiling the example shortens, it does not vanish"
    (let [path (ascii-path-rendering-an-example-of-length 201)
          example (example-for-path path)]
      (is (string? example))
      (is (<= (count example) 200))
      (is (str/includes? example "…")
          "the cut must be visible, never a silent truncation")
      (is (= example (pr-str (edn/read-string example))))
      (let [[file rows] (edn/read-string example)]
        (is (str/starts-with? file "src/"))
        (is (str/ends-with? file ".clj"))
        (is (= 3 (count (first rows)))))))
  (testing "the text block shows the same bounded example the receipt carries"
    (let [path (ascii-path-rendering-an-example-of-length 201)
          {:keys [structured text]} (refusal-text (d1-request-with-path path))]
      (is (str/includes? text (str "expected: "
                                   (:expected_shape_example structured)))))))

(deftest expected-shape-example-survives-an-oversized-caller-path
  ;; @spec MCP-OP-EDIT-037
  ;; The exact r1 counterexample class: a caller path far past the ceiling.
  (doseq [length [228 500 4000]]
    (testing (str "caller path of " length " characters")
      (let [path (str "src/" (apply str (repeat (- length 8) \z)) ".clj")
            {:keys [structured text]} (refusal-text (d1-request-with-path path))
            example (:expected_shape_example structured)]
        (is (= length (count path)))
        (is (string? example) "the example must exist for every applicable refusal")
        (is (<= (count example) 200))
        (is (str/includes? example "…"))
        (is (= example (pr-str (edn/read-string example))))
        (is (str/includes? text (str "expected: " example)))
        (is (str/includes? text "Each migration file must be [file, rows]"))))))

(deftest expected-shape-example-reads-no-source-and-leaks-nothing
  ;; @spec MCP-OP-EDIT-037
  (testing "rendering performs no source read at all"
    ;; An executed probe, not a source scan: if any stage reached the
    ;; filesystem through slurp or io/reader this throws instead of rendering.
    (with-redefs [slurp (fn [& _]
                          (throw (ex-info "source read during refusal rendering" {})))
                  io/reader (fn [& _]
                              (throw (ex-info "source read during refusal rendering" {})))]
      (let [{:keys [structured text]} (refusal-text d1-bare-file-request)]
        (is (string? (:expected_shape_example structured)))
        (is (str/includes? text (:expected_shape_example structured))))))
  (testing "the example carries only values the caller supplied in this request"
    (let [{:keys [structured]} (refusal-text d1-bare-file-request)
          example (:expected_shape_example structured)
          [file rows] (edn/read-string example)
          request-strings (fn collect [value]
                            (cond
                              (string? value) #{value}
                              (map? value) (into #{} (mapcat collect) (vals value))
                              (sequential? value) (into #{} (mapcat collect) value)
                              :else #{}))
          supplied (request-strings d1-bare-file-request)]
      (is (contains? supplied file))
      (doseq [token (first rows) :when (string? token)]
        (is (contains? supplied token)
            (str "example token " (pr-str token)
                 " was not supplied by the caller")))))
  (testing "no workspace path, file content, or receipt path can appear"
    (let [example (:expected_shape_example
                    (:structured (refusal-text d1-bare-file-request)))]
      (doseq [forbidden ["/home/" "/var/" "/tmp" "file:" ".git"
                         "workspace_root" "\n"]]
        (is (not (str/includes? example forbidden))
            (str "example leaked " (pr-str forbidden)))))))
