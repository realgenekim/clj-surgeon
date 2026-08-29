(ns owner-aware-symbol-migration
  (:require
   [cheshire.core :as json]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-contract :as mcp-contract]
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(def base-commit "e9f8e100c839b4664505b45f15f7bb7d44eef9a3")
(def oracle-payload-bytes 6409)
(def candidate-payload-budget 4500)
(def canonical-workspace "/Users/genekim/src.local/clj-surgeon")

(def fixture-root
  "bench/fixtures/edit_portfolio/submission-row-extraction-cleanup")

(def target-files
  ["src/sample/review_updates.clj"
   "src/sample/views/log.clj"
   "src/sample/views/people.clj"
   "src/sample/views/review.clj"
   "test/sample/board_test.clj"
   "test/sample/reviews_test.clj"
   "test/sample/status_workflow_test.clj"
   "test/sample/views_test.clj"
   "test/sample/voting_policy_test.clj"])

(def namespace-edits
  [{"file" "src/sample/review_updates.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.reviews :as reviews]\n   [sample.views.review :as view-review])"
    "to" "(:require\n   [sample.reviews :as reviews]\n   [sample.views.review :as view-review]\n   [sample.views.submission-row :as submission-row])"
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
    "to" "(:require\n   [sample.events :as events]\n   [sample.store :as store]\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}
   {"file" "test/sample/board_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as view-review])"
    "to" "(:require\n   [sample.views.review :as view-review]\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}
   {"file" "test/sample/reviews_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review-view])"
    "to" "(:require\n   [sample.views.review :as review-view]\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}
   {"file" "test/sample/status_workflow_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review-view])"
    "to" "(:require\n   [sample.views.review :as review-view]\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}
   {"file" "test/sample/views_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review])"
    "to" "(:require\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}
   {"file" "test/sample/voting_policy_test.clj"
    "within" {"namespace" true}
    "from" "(:require\n   [sample.views.review :as review])"
    "to" "(:require\n   [sample.views.review :as review]\n   [sample.views.submission-row :as submission-row])"
    "matches" 1}])

(def moved-owner-deletion
  {"file" "src/sample/views/review.clj"
   "forms" ["chair-on-event?" "fmt-mean" "fmt-aggregate" "fmt-stars"
            "private-note-block" "star-form" "star-histogram"
            "reviewer-input-controls" "row-controls*" "opinions-block"
            "row-controls" "reviewed-by?" "score-for-person" "board-row"]})

(def bespoke-edit
  {"file" "src/sample/views/review.clj"
   "within" {"form" "detail-controls"}
   "from" "(defn detail-controls [event row person mine]\n  (row-controls* event row person mine (chair-on-event? event person) false))"
   "to" "(defn detail-controls [event row person mine]\n  (submission-row/row-controls*\n    event row person mine (submission-row/chair-on-event? event person) false))"
   "matches" 1})

(def oracle-rewrite-edits
  [{"file" "src/sample/review_updates.clj" "within" {"form" "push-person-row"}
    "from" "view-review/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "src/sample/review_updates.clj" "within" {"form" "push-active-row"}
    "from" "view-review/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "src/sample/views/log.clj" "within" {"form" "describe-rating"}
    "from" "review/fmt-stars" "to" "submission-row/fmt-stars" "matches" 3}
   {"file" "src/sample/views/people.clj" "within" {"form" "reviewer-summary"}
    "from" "review/fmt-mean" "to" "submission-row/fmt-mean" "matches" 2}
   {"file" "src/sample/views/people.clj" "within" {"form" "rating-row"}
    "from" "review/fmt-stars" "to" "submission-row/fmt-stars" "matches" 1}
   {"file" "src/sample/views/review.clj" "within" {"form" "content-status-control"}
    "from" "chair-on-event?" "to" "submission-row/chair-on-event?" "matches" 1}
   {"file" "src/sample/views/review.clj" "within" {"form" "submission-detail-page"}
    "from" "score-for-person" "to" "submission-row/score-for-person" "matches" 1}
   {"file" "src/sample/views/review.clj" "within" {"form" "submission-detail-page"}
    "from" "reviewed-by?" "to" "submission-row/reviewed-by?" "matches" 1}
   {"file" "src/sample/views/review.clj" "within" {"form" "review-summary"}
    "from" "fmt-aggregate" "to" "submission-row/fmt-aggregate" "matches" 1}
   {"file" "src/sample/views/review.clj" "within" {"form" "review-summary"}
    "from" "fmt-mean" "to" "submission-row/fmt-mean" "matches" 1}
   {"file" "src/sample/views/review.clj" "within" {"form" "board-page"}
    "from" "chair-on-event?" "to" "submission-row/chair-on-event?" "matches" 1}
   {"file" "src/sample/views/review.clj" "within" {"form" "board-page"}
    "from" "board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "test/sample/board_test.clj" "within" {"form" "render-unrated"}
    "from" "view-review/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "test/sample/board_test.clj" "within" {"form" "render-rated"}
    "from" "view-review/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "test/sample/reviews_test.clj" "within" {"form" "render-result"}
    "from" "review-view/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "test/sample/reviews_test.clj" "within" {"form" "render-weighted"}
    "from" "review-view/board-row" "to" "submission-row/board-row" "matches" 2}
   {"file" "test/sample/reviews_test.clj" "within" {"form" "render-visible"}
    "from" "review-view/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "test/sample/status_workflow_test.clj" "within" {"form" "render-status"}
    "from" "review-view/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "test/sample/views_test.clj" "within" {"form" "render-opinions"}
    "from" "review/opinions-block" "to" "submission-row/opinions-block" "matches" 1}
   {"file" "test/sample/views_test.clj" "within" {"form" "render-histogram"}
    "from" "review/star-histogram" "to" "submission-row/star-histogram" "matches" 1}
   {"file" "test/sample/voting_policy_test.clj" "within" {"form" "visible-row"}
    "from" "review/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "test/sample/voting_policy_test.clj" "within" {"form" "hidden-row"}
    "from" "review/board-row" "to" "submission-row/board-row" "matches" 1}
   {"file" "test/sample/voting_policy_test.clj" "within" {"form" "revealed-row"}
    "from" "review/board-row" "to" "submission-row/board-row" "matches" 1}])

(def oracle-request
  {"workspace_root" canonical-workspace
   "edits" (vec (concat namespace-edits [bespoke-edit] oracle-rewrite-edits))
   "delete_owners" [moved-owner-deletion]})

(def symbol-migration-table
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

(def candidate-manifest
  {"workspace_root" canonical-workspace
   "edits" (conj namespace-edits bespoke-edit)
   "delete_owners" [moved-owner-deletion]
   "symbol_migration" symbol-migration-table})

(defn refusal
  [error-type error data]
  (merge {:ok false :error-type error-type :error error} data))

(defn valid-site?
  [[owner from matches :as site]]
  (and (= 3 (count site))
       (string? owner) (seq owner)
       (string? from) (seq from)
       (integer? matches) (pos? matches)))

(defn migration-edit
  [target-alias file [owner from matches]]
  {"file" file
   "within" {"form" owner}
   "from" from
   "to" (str target-alias "/" (name (symbol from)))
   "matches" matches})

(defn compile-manifest
  "Expand one grouped owner-aware symbol migration into today's edit request.
   This prototype is pure and has no public product or schema authority."
  [manifest]
  (let [migration (get manifest "symbol_migration")
        target-alias (get migration "target_alias")
        target-rule (get migration "target_rule")
        columns (get migration "columns")
        file-groups (get migration "files")]
    (cond
      (not= ["owner" "from" "matches"] columns)
      (refusal :unsupported-columns
               "symbol_migration.columns must preserve owner, from, and matches"
               {:columns columns})

      (not= "preserve-name" target-rule)
      (refusal :unsupported-target-rule
               "Only the explicit preserve-name target relation is supported"
               {:target-rule target-rule})

      (not (and (string? target-alias) (seq target-alias)))
      (refusal :invalid-target-alias
               "symbol_migration.target_alias must be a non-empty string"
               {:target-alias target-alias})

      (not (and (vector? file-groups) (seq file-groups)))
      (refusal :invalid-file-groups
               "symbol_migration.files must be a non-empty vector"
               {})

      :else
      (let [invalid-group
            (first
              (keep-indexed
                (fn [file-index group]
                  (let [[file sites :as complete] group]
                    (when-not (and (= 2 (count complete))
                                   (string? file) (seq file)
                                   (vector? sites) (seq sites)
                                   (every? valid-site? sites))
                      {:file-index file-index :group group})))
                file-groups))
            files (mapv first file-groups)]
        (cond
          invalid-group
          (refusal :invalid-site
                   "Every file group and owner-aware site must be complete"
                   invalid-group)

          (not= (count files) (count (distinct files)))
          (refusal :duplicate-file-group
                   "Each migration file must appear exactly once"
                   {:files files})

          :else
          (let [migration-edits
                (mapv
                  (fn [[file site]]
                    (migration-edit target-alias file site))
                  (mapcat
                    (fn [[file sites]]
                      (map (fn [site] [file site]) sites))
                    file-groups))]
            {:ok true
             :request
             (-> manifest
                 (dissoc "symbol_migration")
                 (update "edits" (fnil into []) migration-edits))
             :owner-row-count (count migration-edits)
             :declared-match-count (reduce + (map #(get % "matches")
                                                  migration-edits))}))))))

(defn json-byte-count
  [value]
  (alength (.getBytes (json/generate-string value) "UTF-8")))

(defn normalized-transaction
  [request]
  ;; execute-request! performs this JSON normalization and workspace routing
  ;; before the current product validator receives request parameters.
  (let [product-params (-> request
                           json/generate-string
                           (json/parse-string true)
                           (dissoc :workspace_root))
        validated (mcp-contract/validate-tool-params product-params)]
    (if (:ok validated)
      {:ok true
       :normalized (:params validated)
       :transaction (mcp-contract/tool-params->transaction (:params validated))}
      validated)))

(defn compile-request
  [sources request]
  (let [normalized (normalized-transaction request)]
    (if (:ok normalized)
      (assoc normalized
             :compiled
             (transaction/compile-transaction sources (:transaction normalized)))
      normalized)))

(defn load-fixture
  []
  (let [capsule (edn/read-string (slurp (io/file fixture-root "capsule.edn")))]
    {:sources
     (into {}
           (map (fn [file]
                  [file (slurp (io/file fixture-root "before" file))]))
           target-files)
     :expected-after-hashes
     (into {}
           (map (fn [[file hashes]] [file (:after hashes)]))
           (:hashes capsule))}))

(defn file-hashes
  [compiled]
  (into {} (map (juxt :file :result-hash)) (:files compiled)))

(defn addressed-replacements
  [compiled]
  (mapv #(select-keys % [:file :match-count :edits]) (:files compiled)))

(defn compiled-candidate
  [sources manifest]
  (let [expanded (compile-manifest manifest)]
    (if (:ok expanded)
      (compile-request sources (:request expanded))
      expanded)))

(defn refusal-evidence
  [compiled]
  {:refused (not (true? (get-in compiled [:compiled :ok])))
   :error-type (or (get-in compiled [:compiled :error-type])
                   (:error-type compiled))})

(defn parity-report
  [sources expected-after-hashes]
  (let [expanded (compile-manifest candidate-manifest)
        oracle (compile-request sources oracle-request)
        candidate (compile-request sources (:request expanded))
        oracle-compiled (:compiled oracle)
        candidate-compiled (:compiled candidate)
        wrong-owner
        (compiled-candidate
          sources
          (assoc-in candidate-manifest
                    ["symbol_migration" "files" 0 1 0 0]
                    "missing-owner"))
        wrong-count
        (compiled-candidate
          sources
          (assoc-in candidate-manifest
                    ["symbol_migration" "files" 0 1 0 2]
                    2))
        payload {:oracle-bytes (json-byte-count oracle-request)
                 :candidate-bytes (json-byte-count candidate-manifest)
                 :budget candidate-payload-budget}
        parity
        {:normalized-transaction-equal
         (= (:transaction oracle) (:transaction candidate))
         :addressed-replacements-equal
         (= (addressed-replacements oracle-compiled)
            (addressed-replacements candidate-compiled))
         :future-hashes-equal
         (= (file-hashes oracle-compiled)
            (file-hashes candidate-compiled))
         :expected-after-hashes-equal
         (= expected-after-hashes (file-hashes candidate-compiled))
         :match-count (:match-count candidate-compiled)
         :changed-file-count (:changed-file-count candidate-compiled)}
        decision {:owner-row-count (:owner-row-count expanded)
                  :declared-match-count (:declared-match-count expanded)}
        refusals {:wrong-owner (refusal-evidence wrong-owner)
                  :wrong-count (refusal-evidence wrong-count)}
        result
        {:schema :clj-surgeon.owner-aware-symbol-migration-experiment/v1
         :base-commit base-commit
         :model-calls 0
         :mutation-actions 0
         :payload payload
         :decision decision
         :parity parity
         :refusals refusals}]
    (assoc result
           :all-correct
           (every?
             true?
             [(= oracle-payload-bytes (:oracle-bytes payload))
              (<= (:candidate-bytes payload) candidate-payload-budget)
              (= 23 (:owner-row-count decision))
              (= 27 (:declared-match-count decision))
              (= 51 (:match-count parity))
              (= 9 (:changed-file-count parity))
              (:normalized-transaction-equal parity)
              (:addressed-replacements-equal parity)
              (:future-hashes-equal parity)
              (:expected-after-hashes-equal parity)
              (get-in refusals [:wrong-owner :refused])
              (get-in refusals [:wrong-count :refused])]))))

(defn report
  []
  (let [{:keys [sources expected-after-hashes]} (load-fixture)]
    (parity-report sources expected-after-hashes)))

(defn -main
  [& _args]
  (let [result (report)]
    (prn result)
    (when-not (:all-correct result)
      (System/exit 1))))
