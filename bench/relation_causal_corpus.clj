#!/usr/bin/env bb

(ns relation-causal-corpus
  (:require
   [cheshire.core :as json]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-compact-relations :as relations]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-schema :as schema]
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   (java.security MessageDigest)))

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

(def expected-after-hashes
  {"src/sample/review_updates.clj"
   "086fc1360d56dff9f0df08f6594337c902b1f1d5851c1c22ea3c3a3c59768675"
   "src/sample/views/log.clj"
   "949c957f5518d1ea453615d6051119b3fb13ec8328a71119d429113c4b15fc40"
   "src/sample/views/people.clj"
   "113c5d105c6915af1b4f779b78aa18a174c6a977d4e89a044fb35cddb7dde202"
   "src/sample/views/review.clj"
   "a68b7584e704db831d156d6650048f5dea8963cd84363aeb21228896b4ada1e8"
   "test/sample/board_test.clj"
   "a599f0d7c6bd9883401e709878b849b0322ac755d21cfddd9b945c9acb148559"
   "test/sample/reviews_test.clj"
   "7a87ba9fded86cde0aea5d9c9bc888408826a440493437875968479531a9da0b"
   "test/sample/status_workflow_test.clj"
   "9e7de3b37c53b5fe421b486a350d176d09e1046ebbf36295f1b1bdddb26e9db8"
   "test/sample/views_test.clj"
   "0e47210a2f3d1d48e684dc32685ce487dd987e1140fd75f5d72a508d883df30f"
   "test/sample/voting_policy_test.clj"
   "0bc8b306e00a3f0dfb77952937c0af0024b181dcfe8b934251a932f0b3d174ba"})

(def exact-profile-sha256
  "8d4dddfb1cdc89e0e0261e8e9557e1bbbc6e254697dd9a7a30de0bb4313c99a1")

(def exact-profile
  {:verification-profiles
   {"exact"
    {:acceptance :exact-exit
     :timeout-ms 120000
     :commands
     [["clj-kondo" "--cache" "false" "--lint"
       "src/sample/review_updates.clj"
       "src/sample/views/log.clj"
       "src/sample/views/people.clj"
       "src/sample/views/review.clj"
       "test/sample/board_test.clj"
       "test/sample/reviews_test.clj"
       "test/sample/status_workflow_test.clj"
       "test/sample/views_test.clj"
       "test/sample/voting_policy_test.clj"
       "--fail-level" "error"]]}}})

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

(def flat-require-edits
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

(defn- flat-symbol-edits []
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

(defn normalized-flat-request [workspace-root]
  {"workspace_root" workspace-root
   "verify" "exact"
   "edits" (vec (concat flat-require-edits (flat-symbol-edits) [bespoke-edit]))
   "delete_owners" [moved-owner-deletion]})

(defn closed-relation-request [workspace-root]
  {"workspace_root" workspace-root
   "verify" "exact"
   "symbol_migration" symbol-migration
   "require_change" require-change
   "edits" [bespoke-edit]
   "delete_owners" [moved-owner-deletion]})

(def common-prompt-prefix
  (str "Apply the complete frozen submission-row cleanup in exactly one "
       "apply_clojure_changes call. Send one arguments object directly; do not "
       "wrap it in changes, representation, relations, route, arm, N, R, or any "
       "other meta-object. The exact top-level fields for N are workspace_root, "
       "verify, edits, delete_owners. The exact top-level fields for R are "
       "workspace_root, verify, symbol_migration, require_change, edits, "
       "delete_owners. Always include the explicit workspace_root and "
       "verify=exact. Use these exact value-free shapes: each edits row is "
       "{file,within:{namespace:true|form:<owner>},from,to,matches}; each "
       "delete_owners row is {file,forms:[...]}; symbol_migration is one object "
       "{target_alias,target_rule,columns,files:[[file,[[owner,from,matches]...]]...]}; "
       "require_change is one object "
       "{add:{lib,as},files:[{file,optional remove:{lib,as}}...]}. Never use "
       "before/after, a top-level owner in an edit, owners or "
       "include_attached_comments in a deletion, or arrays for "
       "symbol_migration/require_change. In N, put the repeated require changes, "
       "symbol rewrites, "
       "and the bespoke edit directly in edits; keep the moved-owner deletion "
       "explicit in delete_owners. In R, symbol_migration and require_change "
       "replace only the repeated require-change and symbol-rewrite rows; keep "
       "the same bespoke edit explicit in edits and the same moved-owner "
       "deletion explicit in delete_owners. Make the mutation call the first "
       "actionable item. Do not inspect, use shell, narrate before the call, "
       "retry, or make a second tool call. Use only the assigned representation. "
       "Assignment: "))

(def common-prompt-suffix
  ". Return exactly the tool's terminal response after success.")

(defn prompt-material [arm workspace-root]
  (let [assignment (name arm)
        request (case arm
                  :N (normalized-flat-request workspace-root)
                  :R (closed-relation-request workspace-root)
                  (throw (ex-info "Unknown relation-corpus arm" {:arm arm})))]
    {:arm arm
     :prompt (str common-prompt-prefix assignment common-prompt-suffix)
     :request request
     :request-edn (pr-str request)
     :request-json (json/generate-string request)}))

(defn- sha256 [text]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes ^String text "UTF-8"))
    (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))))

(defn- public-key [value]
  (if (keyword? value) (name value) (str value)))

(defn- object-value? [value]
  (or (map? value) (instance? java.util.Map value)))

(defn- array-value? [value]
  (or (sequential? value) (instance? java.util.List value)))

(defn- public-map [value]
  (when (object-value? value)
    (into {} (map (fn [[key child]] [(public-key key) child])) value)))

(declare public-schema-valid?)

(defn- type-valid? [expected value]
  (case expected
    "object" (object-value? value)
    "array" (array-value? value)
    "string" (string? value)
    "integer" (integer? value)
    "number" (number? value)
    "boolean" (instance? Boolean value)
    false))

(def supported-schema-keywords
  #{:type :description :title :additionalProperties :properties :required
    :items :prefixItems :minItems :maxItems :uniqueItems :minLength :pattern :minimum
    :maximum :const :enum :allOf :anyOf :oneOf :not :default
    :minProperties :maxProperties})

(defn- unsupported-schema-keywords [public-schema]
  (let [local (remove supported-schema-keywords (keys public-schema))
        property-children (vals (:properties public-schema))
        direct-children (remove nil? [(:items public-schema) (:not public-schema)
                                      (when (map? (:additionalProperties public-schema))
                                        (:additionalProperties public-schema))])
        prefix-children (or (:prefixItems public-schema) [])
        branch-children (mapcat #(or (% public-schema) []) [:allOf :anyOf :oneOf])]
    (into (set local)
          (mapcat unsupported-schema-keywords)
          (concat property-children direct-children prefix-children branch-children))))

(defn- object-valid? [public-schema value]
  (if-not (object-value? value)
    true
    (let [value (public-map value)
          properties (public-map (:properties public-schema))
          required (map public-key (:required public-schema))
          unexpected (remove (set (keys properties)) (keys value))
          additional-schema (when (map? (:additionalProperties public-schema))
                              (:additionalProperties public-schema))]
      (and
        (every? #(contains? value %) required)
        (or (not (:minProperties public-schema))
            (<= (:minProperties public-schema) (count value)))
        (or (not (:maxProperties public-schema))
            (<= (count value) (:maxProperties public-schema)))
        (or (not= false (:additionalProperties public-schema))
            (empty? unexpected))
        (or (not additional-schema)
            (every? #(public-schema-valid? additional-schema (get value %)) unexpected))
        (every? (fn [[key child-schema]]
                  (or (not (contains? value key))
                      (public-schema-valid? child-schema (get value key))))
                properties)))))

(defn- array-valid? [public-schema value]
  (if-not (array-value? value)
    true
    (let [value (vec value)
          prefix-items (vec (or (:prefixItems public-schema) []))
          remaining (if (seq prefix-items)
                      (subvec value (min (count value) (count prefix-items)))
                      value)]
      (and
        (or (not (:minItems public-schema))
            (<= (:minItems public-schema) (count value)))
        (or (not (:maxItems public-schema))
            (<= (count value) (:maxItems public-schema)))
        (or (not (:uniqueItems public-schema))
            (= (count value) (count (distinct value))))
        (every? true? (map public-schema-valid? prefix-items value))
        (or (not (:items public-schema))
            (every? #(public-schema-valid? (:items public-schema) %) remaining))))))

(defn- scalar-valid? [public-schema value]
  (and
    (or (not (contains? public-schema :const)) (= (:const public-schema) value))
    (or (not (:enum public-schema)) (some #(= value %) (:enum public-schema)))
    (or (not (:minLength public-schema))
        (and (string? value) (<= (:minLength public-schema) (count value))))
    (or (not (:pattern public-schema))
        (and (string? value)
             (boolean (re-find (re-pattern (:pattern public-schema)) value))))
    (or (not (:minimum public-schema))
        (and (number? value) (<= (:minimum public-schema) value)))
    (or (not (:maximum public-schema))
        (and (number? value) (<= value (:maximum public-schema))))))

(defn- public-schema-valid? [public-schema value]
  (and
    (or (not (:type public-schema)) (type-valid? (:type public-schema) value))
    (object-valid? public-schema value)
    (array-valid? public-schema value)
    (scalar-valid? public-schema value)
    (or (not (:allOf public-schema))
        (every? #(public-schema-valid? % value) (:allOf public-schema)))
    (or (not (:anyOf public-schema))
        (some #(public-schema-valid? % value) (:anyOf public-schema)))
    (or (not (:oneOf public-schema))
        (= 1 (count (filter #(public-schema-valid? % value)
                            (:oneOf public-schema)))))
    (or (not (:not public-schema))
        (not (public-schema-valid? (:not public-schema) value)))))

(defn public-schema-report [params]
  "Validate one request against the exact public apply_clojure_changes schema."
  (let [public-schema schema/clj-change-schema
        unsupported (unsupported-schema-keywords public-schema)]
    {:ok (and (empty? unsupported) (public-schema-valid? public-schema params))
     :unsupported-schema-keywords (vec (sort unsupported))}))

(defn exact-profile-report [profile-text]
  (try
    (let [parsed (edn/read-string profile-text)
          profile (get-in parsed [:verification-profiles "exact"])
          commands (:commands profile)
          argv (first commands)
          digest (sha256 profile-text)]
      {:ok (and (= exact-profile parsed)
                (= exact-profile-sha256 digest)
                (= 1 (count commands)))
       :sha256 digest
       :acceptance (:acceptance profile)
       :timeout-ms (:timeout-ms profile)
       :argv argv})
    (catch Exception error
      {:ok false :error (.getMessage error)})))

(defn compile-request [sources request]
  (let [public (public-schema-report request)
        routed-request (dissoc request "workspace_root")
        runtime (contract/validate-tool-params routed-request)
        source-blind (:compact-relation-plan runtime)
        frozen (when source-blind
                 (relations/compile-frozen sources source-blind))
        executable-request (if source-blind (:request frozen) request)
        executable-runtime
        (if source-blind
          (when (:ok frozen)
            (contract/validate-tool-params executable-request))
          runtime)
        spec (when (:ok executable-runtime)
               (contract/tool-params->transaction (:params executable-runtime)))
        prepared (when spec
                   (compact-location/normalize-spec
                     sources spec
                     (:compact-location-normalization executable-runtime)))
        compiled (when (and prepared (not (:error prepared)))
                   (transaction/compile-transaction sources (:spec prepared)))
        future-hashes
        (when (and compiled (not (:error compiled)))
          (into {} (map (juxt :file :result-hash)) (:files compiled)))]
    {:public-schema public
     :runtime-contract runtime
     :frozen-relation frozen
     :executable-runtime executable-runtime
     :canonical-transaction (:spec prepared)
     :compiled compiled
     :future-hashes future-hashes}))

(defn- arm-report [compiled]
  (let [product (:compiled compiled)]
    {:public-schema-ok (true? (get-in compiled [:public-schema :ok]))
     :runtime-contract-ok (true? (get-in compiled [:runtime-contract :ok]))
     :compiler-ok (and (map? product) (nil? (:error product)))
     :match-count (:match-count product)
     :changed-file-count (:changed-file-count product)
     :future-hashes (:future-hashes compiled)}))

(defn report [workspace-root sources profile-text]
  (let [flat (compile-request sources (normalized-flat-request workspace-root))
        relation (compile-request sources (closed-relation-request workspace-root))
        flat-report (arm-report flat)
        relation-report (arm-report relation)
        profile (exact-profile-report profile-text)
        parity
        {:canonical-transaction-equal
         (= (:canonical-transaction flat) (:canonical-transaction relation))
         :future-hashes-equal
         (= (:future-hashes flat) (:future-hashes relation))
         :expected-future-hashes-equal
         (= expected-after-hashes (:future-hashes relation))}
        result
        {:schema :clj-surgeon.edit-025-relation-causal-corpus/v1
         :model-calls 0
         :mutation-actions 0
         :verification-profile profile
         :arms {:N flat-report :R relation-report}
         :parity parity}]
    (assoc result :all-correct
           (every? true?
                   [(:ok profile)
                    (:public-schema-ok flat-report)
                    (:runtime-contract-ok flat-report)
                    (:compiler-ok flat-report)
                    (= 51 (:match-count flat-report))
                    (= 9 (:changed-file-count flat-report))
                    (:public-schema-ok relation-report)
                    (:runtime-contract-ok relation-report)
                    (:compiler-ok relation-report)
                    (= 51 (:match-count relation-report))
                    (= 9 (:changed-file-count relation-report))
                    (:canonical-transaction-equal parity)
                    (:future-hashes-equal parity)
                    (:expected-future-hashes-equal parity)]))))

(defn load-fixture []
  {:sources
   (into {}
         (map (fn [file]
                [file (slurp (io/file fixture-root "before" file))]))
         relation-files)
   :profile-text (slurp (io/file fixture-root "exact-profile.edn"))})

(defn -main [& [operation arm-or-root workspace-root]]
  (case operation
    "--prompt"
    (println (:prompt (prompt-material (keyword arm-or-root) workspace-root)))

    "--request-json"
    (println (:request-json
               (prompt-material (keyword arm-or-root) workspace-root)))

    "--request-edn"
    (println (:request-edn
               (prompt-material (keyword arm-or-root) workspace-root)))

    (let [{:keys [sources profile-text]} (load-fixture)
          root (if (#{"--report" "--report-json"} operation)
                 arm-or-root
                 "/workspace")
          result (report (or root "/workspace") sources profile-text)]
      (println (if (= "--report-json" operation)
                 (json/generate-string result {:pretty true})
                 (pr-str result)))
      (when-not (:all-correct result)
        (System/exit 1)))))

(when-let [entry-file (System/getProperty "babashka.file")]
  (when (= (.getCanonicalPath (io/file *file*))
           (.getCanonicalPath (io/file entry-file)))
    (apply -main *command-line-args*)))
