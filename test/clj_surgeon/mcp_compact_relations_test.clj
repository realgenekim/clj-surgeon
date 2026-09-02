(ns clj-surgeon.mcp-compact-relations-test
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
   [cheshire.core :as json]
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
    ;; @spec MCP-OP-EDIT-039
    ;; The closed diagnostic admits expected_shape, and ONLY for the shape
    ;; refusal: a refusal that cannot show the shape teaches one field per
    ;; return, which is the ladder this change exists to remove.
    (is (every? #{:failed_stage :path :expected_shape} (keys diagnostic)))
    (if (= "invalid-compact-relation" error-type)
      (do
        (is (= relations/relation-expected-shape (:expected_shape diagnostic))
            "the public shape refusal must carry the whole accepted skeleton")
        (is (= relations/relation-fields
               (set (keys (:expected_shape diagnostic))))))
      (is (not (contains? diagnostic :expected_shape))
          (str error-type " is not a shape refusal and must omit expected_shape")))
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

(def scoped-file "test/sample/scoped_test.clj")

(def scoped-request
  {"symbol_migration"
   {"target_alias" "submission-row"
    "target_rule" "preserve-name"
    "columns" ["owner" "from" "matches"]
    "files" [[scoped-file [["render-row" "review/board-row" 1]]]]}
   "require_change"
   {"add" {"lib" "sample.views.submission-row" "as" "submission-row"}
    "files" [{"file" scoped-file}]}})

(defn- scoped-source
  [require-body]
  (str "(ns sample.scoped-test\n"
       "  (:require\n"
       require-body
       "))\n"
       "\n"
       "(deftest render-row\n"
       "  (is (= 1 (review/board-row))))\n"))

(defn- scoped-lowering
  ([source] (scoped-lowering source scoped-request))
  ([source request]
   (relations/compile-frozen
     {scoped-file source}
     (relations/compile-source-blind request))))

(deftest an-invalid-compact-relation-refusal-carries-the-expected-shape
  ;; @spec MCP-OP-EDIT-037
  (let [expected
        {"symbol_migration"
         {"target_alias" "<alias>"
          "target_rule" "preserve-name"
          "columns" ["owner" "from" "matches"]
          "files" [["<file>" [["<owner>" "<from-symbol>" 1]]]]}
         "require_change"
         {"add" {"lib" "<lib>" "as" "<alias>"}
          "files" [{"file" "<file>"
                    "remove" {"lib" "<lib>" "as" "<alias>"}}]}}]
    (doseq [[label request path]
            [[:missing-pair (dissoc relation-request "require_change")
              ["require_change"]]
             [:wrong-columns
              (assoc-in relation-request ["symbol_migration" "columns"]
                        ["from" "owner" "matches"])
              ["symbol_migration" "columns"]]
             [:wrong-file-tuple-arity
              (update-in relation-request ["symbol_migration" "files" 0] pop)
              ["symbol_migration" "files" 0]]
             [:bad-migration-symbol
              (assoc-in relation-request
                        ["symbol_migration" "files" 0 1 0 1] "one/two/three")
              ["symbol_migration" "files" 0 1 0 1]]
             [:unknown-require-field
              (assoc-in relation-request ["require_change" "extra"] true)
              ["require_change" "extra"]]
             [:unknown-require-file-field
              (assoc-in relation-request
                        ["require_change" "files" 0 "extra"] true)
              ["require_change" "files" 0 "extra"]]
             [:bad-lib-alias-object
              (assoc-in relation-request ["require_change" "add"]
                        {"lib" "sample.views.submission-row"})
              ["require_change" "add" "as"]]
             [:wrong-target-rule
              (assoc-in relation-request
                        ["symbol_migration" "target_rule"] "rename")
              ["symbol_migration" "target_rule"]]]]
      (testing (name label)
        (let [result (relations/compile-source-blind request)]
          (is (false? (:ok result)) (pr-str result))
          (is (= :invalid-compact-relation (:error-type result)))
          (is (= path (:path result)))
          (is (= expected (:expected-shape result)) (pr-str result)))))

    (testing "the skeleton is derived from the one relation schema"
      (let [shape (:expected-shape
                    (relations/compile-source-blind
                      (dissoc relation-request "require_change")))]
        (is (= (set (get-in relations/relation-schema [:request :relations]))
               (set (keys shape))))
        (is (= relations/relation-fields (set (keys shape))))
        (is (= (get-in relations/relation-schema [:migration :allowed])
               (set (keys (get shape "symbol_migration")))))
        (is (= (get-in relations/relation-schema [:migration :columns])
               (get-in shape ["symbol_migration" "columns"])))
        (is (= (get-in relations/relation-schema [:migration :target-rule])
               (get-in shape ["symbol_migration" "target_rule"])))
        (is (= (count (get-in relations/relation-schema [:migration :columns]))
               (count (get-in shape
                              ["symbol_migration" "files" 0 1 0]))))
        (is (= (get-in relations/relation-schema [:require :allowed])
               (set (keys (get shape "require_change")))))
        (is (= (get-in relations/relation-schema [:require :file :allowed])
               (set (keys (get-in shape ["require_change" "files" 0])))))
        (is (= (get-in relations/relation-schema [:lib-alias :allowed])
               (set (keys (get-in shape ["require_change" "add"])))))
        (is (= (get-in relations/relation-schema [:lib-alias :allowed])
               (set (keys (get-in shape
                                  ["require_change" "files" 0 "remove"])))))
        (is (= relations/allowed-request-fields
               (get-in relations/relation-schema [:request :allowed])))))

    (testing "only shape refusals carry expected_shape"
      (let [overlap
            (relations/compile-source-blind
              (update relation-request "edits" conj
                      {"file" "src/sample/review_updates.clj"
                       "within" {"form" "push-person-row"}
                       "from" "view-review/board-row"
                       "to" "submission-row/board-row"
                       "matches" 1}))
            source-blind (relations/compile-source-blind relation-request)
            unprovable (relations/compile-frozen {} source-blind)
            path-conflict
            (relations/validate-path-resolution
              source-blind
              {:ok false
               :raw-path (first relation-files)
               :error "A relation path is outside the workspace"})]
        (is (= :compact-relation-overlap (:error-type overlap)))
        (is (not (contains? overlap :expected-shape)) (pr-str overlap))
        (is (= :require-change-unprovable (:error-type unprovable)))
        (is (not (contains? unprovable :expected-shape)) (pr-str unprovable))
        (is (= :compact-relation-path-conflict (:error-type path-conflict)))
        (is (not (contains? path-conflict :expected-shape))
            (pr-str path-conflict))
        (is (not (contains? source-blind :expected-shape))
            (pr-str (keys source-blind)))))))

(deftest require-change-provability-is-scoped-to-the-entries-named
  ;; @spec MCP-OP-EDIT-038
  (testing "an untouched non-direct entry never blocks a provable change"
    (let [source (scoped-source
                   (str "   [clojure.test :refer [deftest is testing]]\n"
                        "   [sample.views.review :as review]"))
          result (scoped-lowering source)
          edit (first (:generated-require-edits result))]
      (is (:ok result) (pr-str result))
      (is (= 1 (count (:generated-require-edits result))))
      (is (= (str "(:require\n"
                  "   [clojure.test :refer [deftest is testing]]\n"
                  "   [sample.views.review :as review])")
             (get edit "from")))
      (is (= (str "(:require\n"
                  "   [clojure.test :refer [deftest is testing]]\n"
                  "   [sample.views.review :as review]\n"
                  "   [sample.views.submission-row :as submission-row])")
             (get edit "to")))
      (is (= 1 (get edit "matches")))
      (is (= {"namespace" true} (get edit "within")))))

  (testing "every untouched byte survives an unusual separator"
    (let [source (scoped-source
                   (str "   [clojure.test :refer [deftest is testing]] ,\n"
                        "      [sample.views.review :as review]"))
          result (scoped-lowering source)
          edit (first (:generated-require-edits result))]
      (is (:ok result) (pr-str result))
      (is (= (str "(:require\n"
                  "   [clojure.test :refer [deftest is testing]] ,\n"
                  "      [sample.views.review :as review] ,\n"
                  "      [sample.views.submission-row :as submission-row])")
             (get edit "to")))
      (is (str/includes? (get edit "to")
                         "[clojure.test :refer [deftest is testing]]"))))

  (testing "a declared removal still lowers beside an untouched entry"
    (let [source (scoped-source
                   (str "   [clojure.test :refer [deftest is testing]]\n"
                        "   [sample.views.review :as review]"))
          result (scoped-lowering
                   source
                   (assoc-in scoped-request
                             ["require_change" "files" 0 "remove"]
                             {"lib" "sample.views.review" "as" "review"}))
          edit (first (:generated-require-edits result))]
      (is (:ok result) (pr-str result))
      (is (= (str "(:require\n"
                  "   [clojure.test :refer [deftest is testing]]\n"
                  "   [sample.views.submission-row :as submission-row])")
             (get edit "to")))))

  (testing "an entry the request names must still be direct"
    (let [source (scoped-source
                   (str "   [clojure.test :refer [deftest is testing]]\n"
                        "   [sample.views.review :as review :refer [board-row]]"))
          result (scoped-lowering
                   source
                   (assoc-in scoped-request
                             ["require_change" "files" 0 "remove"]
                             {"lib" "sample.views.review" "as" "review"}))]
      (is (false? (:ok result)) (pr-str result))
      (is (= :require-change-unprovable (:error-type result)))
      (is (str/includes? (:error result)
                         "drop options the removal did not declare")
          (str "the refusal must say what it protects, not restate a rule: "
               (:error result)))
      (is (str/includes? (:error result) ":refer")
          "and must quote the entry so the caller can act on it")
      (is (= ["require_change" "files" 0 "remove"] (:path result)))))

  (testing "duplicate-require refusals survive a non-direct existing entry"
    (let [already-required
          (scoped-lowering
            (scoped-source
              (str "   [clojure.test :refer [deftest is testing]]\n"
                   "   [sample.views.submission-row :as row :refer [board-row]]")))
          alias-bound
          (scoped-lowering
            (scoped-source
              (str "   [clojure.test :refer [deftest is testing]]\n"
                   "   [sample.other :as submission-row :refer [board-row]]")))]
      (is (false? (:ok already-required)) (pr-str already-required))
      (is (= "Target namespace is already required" (:error already-required)))
      (is (false? (:ok alias-bound)) (pr-str alias-bound))
      (is (= "Target alias is already bound" (:error alias-bound)))))

  (testing "a clause with no vector libspec entry still refuses"
    (let [result (scoped-lowering
                   (scoped-source "   clojure.string"))]
      (is (false? (:ok result)) (pr-str result))
      (is (= "Require entries must be direct [lib :as alias] vectors"
             (:error result)))
      (is (= ["require_change" "files" 0] (:path result)))))

  (testing "a declared removal must still match exactly one entry"
    (let [result (scoped-lowering
                   (scoped-source
                     (str "   [clojure.test :refer [deftest is testing]]\n"
                          "   [sample.views.review :as review]"))
                   (assoc-in scoped-request
                             ["require_change" "files" 0 "remove"]
                             {"lib" "sample.views.absent" "as" "absent"}))]
      (is (false? (:ok result)) (pr-str result))
      (is (= "Declared removal must match exactly one direct libspec"
             (:error result))))))

;; ============================================================
;; rf2-3 — the rf1 payloads, replayed as the caller actually sent them.
;;
;; These are not synthetic fixtures. Every request below is the verbatim JSON a
;; Codex agent sent to `edit_clojure` during cohort rf1 on 2026-09-02, lifted
;; from the rollouts' `mcp_tool_call_end` records (the wire bytes, upstream of
;; any harness display limit) and stored under test-fixtures/rf1/.
;;
;; The measured cost: two runs each climbed a FOUR-call ladder learning one
;; field constraint per return, and in both runs the fourth call — the one that
;; finally had a valid `symbol_migration` — died on `require-change-unprovable`
;; and ENDED the structural route. Forensics on these exact payloads showed the
;; `require_change` object was byte-identical across calls 11 to 13 in g1 and
;; across all four calls in g2: the require refusal was always going to fire,
;; hidden behind the earlier rungs.
;;
;; The predicate for these tests is the one the field cares about: the LAST call
;; of each ladder must now succeed, or refuse with the whole expected shape, on
;; the FIRST try.
;; ============================================================

(defn- rf1-payload [name]
  (json/parse-string (slurp (io/file "test-fixtures/rf1" name))))

(defn- rf1-refusal-text [name]
  (let [envelope (json/parse-string (slurp (io/file "test-fixtures/rf1" name)))]
    (get-in envelope ["Ok" "structuredContent" "error"])))

;; @spec MCP-OP-EDIT-037
(deftest rf1-g1-A-1-shape-ladder-is-answered-by-one-refusal
  (let [call-10 (rf1-payload "rf1-g1-A-1-call10-args.json")
        call-11 (rf1-payload "rf1-g1-A-1-call11-args.json")
        call-12 (rf1-payload "rf1-g1-A-1-call12-args.json")]
    (testing "the field really did refuse one field at a time"
      (is (= "Closed compact relations require symbol_migration"
             (rf1-refusal-text "rf1-g1-A-1-call10-result.json")))
      (is (= "columns must be [owner, from, matches]"
             (rf1-refusal-text "rf1-g1-A-1-call11-result.json")))
      (is (= "Each migration file must be [file, rows]"
             (rf1-refusal-text "rf1-g1-A-1-call12-result.json"))))

    (testing "every rung now hands back the complete accepted skeleton, so the
              caller never has to climb it a second time"
      (doseq [[label request] [["call-10" call-10]
                               ["call-11" call-11]
                               ["call-12" call-12]]]
        (let [result (relations/compile-source-blind request)]
          (is (false? (:ok result)) label)
          (is (= :invalid-compact-relation (:error-type result)) label)
          (is (= relations/relation-expected-shape (:expected-shape result))
              (str label ": a shape refusal that cannot show the shape teaches "
                   "one field per return"))
          (is (false? (:mutation-attempted result)) label))))))

;; @spec MCP-OP-EDIT-038
(deftest rf1-require-change-refusals-were-false-and-are-gone
  (testing "rf1-g1-A-1 call 13 — the rejected file was the TEST namespace, whose
            only non-direct entry is [clojure.test :refer [deftest is testing]],
            which the call never asked to touch"
    (is (= "Require entries must be direct [lib :as alias] vectors"
           (rf1-refusal-text "rf1-g1-A-1-call13-result.json")))
    (let [request (rf1-payload "rf1-g1-A-1-call13-args.json")
          source-blind (relations/compile-source-blind request)]
      (is (:ok source-blind)
          (str "the last rung must now pass admission on the first try: "
               (pr-str (select-keys source-blind [:error :error-type :path]))))
      ;; the three named files were untouched by :extract! at that moment, so
      ;; the repository's own bytes ARE the frozen sources rf1 lowered against
      (let [sources {"src/clj_surgeon/mcp_formatter.clj"
                     (slurp "src/clj_surgeon/mcp_formatter.clj")
                     "src/clj_surgeon/mcp_tool.clj"
                     (slurp "src/clj_surgeon/mcp_tool.clj")
                     "test/clj_surgeon/mcp_change_buffer_test.clj"
                     (slurp "test/clj_surgeon/mcp_change_buffer_test.clj")}
            lowered (relations/compile-frozen sources source-blind)]
        (is (:ok lowered)
            (str "require lowering must not refuse because of an untouched "
                 "entry: " (pr-str (select-keys lowered [:error :error-type :path]))))
        (is (= 3 (count (:generated-require-edits lowered))))
        (let [test-edit (first (filter #(= "test/clj_surgeon/mcp_change_buffer_test.clj"
                                           (get % "file"))
                                       (:generated-require-edits lowered)))]
          (is (some? test-edit))
          (is (str/includes? (get test-edit "to")
                             "[clojure.test :refer [deftest is testing]]")
              "the untouched entry must survive byte for byte")
          (is (str/includes? (get test-edit "to")
                             "[clj-surgeon.mcp-exact-verify :as exact-verify]"))))))

  (testing "rf1-g2-A-1 call 13 — the rejected file was mcp_change_buffer.clj,
            whose only non-direct entry is the `:as ... :refer [...]` require
            that Surgeon's OWN :extract! had written five calls earlier"
    (is (= "Require entries must be direct [lib :as alias] vectors"
           (rf1-refusal-text "rf1-g2-A-1-call13-result.json")))
    (let [request (rf1-payload "rf1-g2-A-1-call13-args.json")
          source-blind (relations/compile-source-blind request)]
      (is (:ok source-blind)
          (str "admission on the first try: "
               (pr-str (select-keys source-blind [:error :error-type :path]))))
      (let [sources {"src/clj_surgeon/mcp_change_buffer.clj"
                     ;; the exact intermediate the old :extract! produced,
                     ;; reproduced byte for byte (target hash 3e315396...)
                     (slurp "test-fixtures/rf1/g2-A-1-change-buffer-after-old-extract.clj")
                     "src/clj_surgeon/mcp_formatter.clj"
                     (slurp "src/clj_surgeon/mcp_formatter.clj")
                     "src/clj_surgeon/mcp_tool.clj"
                     (slurp "src/clj_surgeon/mcp_tool.clj")
                     "test/clj_surgeon/mcp_change_buffer_test.clj"
                     (slurp "test/clj_surgeon/mcp_change_buffer_test.clj")}
            lowered (relations/compile-frozen sources source-blind)]
        ;; The honest outcome, and it is NOT a green. Admission now passes on
        ;; the first try, and the old blanket refusal about untouched entries is
        ;; gone. What remains is a DIFFERENT and correct refusal, scoped to the
        ;; one entry this call actually names: it declared it was removing
        ;; `[clj-surgeon.mcp-exact-verify :as mcp-exact-verify]`, but the entry
        ;; on disk is `[... :as mcp-exact-verify :refer [admission-unverified?
        ;; expand-command run-process!]]`, and deleting it would silently drop
        ;; three referred names the removal never mentioned. Fail closed is
        ;; right here; the refusal was made recoverable instead of weakened.
        ;;
        ;; The entry it trips on exists ONLY because the old :extract! wrote it.
        ;; rf2-1 stops emitting that `:refer`, so this call disappears.
        (is (false? (:ok lowered)))
        (is (= :require-change-unprovable (:error-type lowered)))
        (is (= ["require_change" "files" 0 "remove"] (:path lowered))
            "the refusal is scoped to the one entry the call names")
        (is (str/includes? (:error lowered) "drop options the removal did not declare")
            (str "a refusal must say what it is protecting: " (:error lowered)))
        (is (str/includes? (:error lowered) ":refer")
            "and it must quote the entry, so the caller can act on it")
        (is (str/includes? (:error lowered) "admission-unverified?"))))))

;; @spec MCP-OP-EDIT-038
(deftest a-re-alias-is-not-a-duplicate-require
  (testing "remove [lib :as a] and add [lib :as b] lowers; the entry being
            removed is not a duplicate of the entry replacing it"
    (let [source (str "(ns sample.core
"
                      "  (:require
"
                      "   [alpha.core :as alpha]
"
                      "   [sample.moved :as old-name]))
")
          request {"workspace_root" "/w"
                   "symbol_migration"
                   {"target_alias" "new-name" "target_rule" "preserve-name"
                    "columns" ["owner" "from" "matches"]
                    "files" [["a.clj" [["owner" "old-name/moved" 1]]]]}
                   "require_change"
                   {"add" {"lib" "sample.moved" "as" "new-name"}
                    "files" [{"file" "a.clj"
                              "remove" {"lib" "sample.moved" "as" "old-name"}}]}}
          source-blind (relations/compile-source-blind request)
          lowered (relations/compile-frozen {"a.clj" source} source-blind)]
      (is (:ok source-blind))
      (is (:ok lowered)
          (str "a re-alias must not refuse as a duplicate: "
               (pr-str (select-keys lowered [:error :error-type :path]))))
      (is (= (str "(:require\n"
                  "   [alpha.core :as alpha]\n"
                  "   [sample.moved :as new-name])")
             (get (first (:generated-require-edits lowered)) "to"))))))
