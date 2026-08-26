(ns clj-surgeon.owner-hypotheses-test
  (:require
   [clj-surgeon.owner-hypotheses :as hypotheses]
   [clojure.test :refer [deftest is]]))

(defn- records
  [names]
  (mapv (fn [line owner]
          {:type 'defn
           :name (symbol owner)
           :platforms [:clj]
           :line line
           :end-line line
           :source (str "(defn " owner " [] nil)")})
        (range 1 (inc (count names)))
        names))

(deftest ranks-each-missing-owner-over-real-unresolved-names
  ;; @spec MCP-OP-READ-HYP-001
  (let [owners (records ["editor-hybrid-schema"
                         "editor-gesture-schema"
                         "editor-programs-schema"
                         "other-owner"])
        program (hypotheses/rank-owner-hypotheses
                  "editor-program-schema" owners
                  {:resolved-names ["editor-hybrid-schema"]})
        edit (hypotheses/rank-owner-hypotheses
               "editor-edit-schema" owners
               {:resolved-names ["editor-hybrid-schema"]})]
    (is (= "editor-program-schema" (:requested-owner program)))
    (is (= "editor-edit-schema" (:requested-owner edit)))
    (is (= 4 (:available-owner-count program)))
    (is (= 1 (:excluded-resolved-owner-count program)))
    (is (= 3 (:candidate-count program)))
    (is (= ["editor-programs-schema" "editor-gesture-schema" "other-owner"]
           (mapv :owner (:did-you-mean program))))
    (is (= "editor-gesture-schema" (-> edit :did-you-mean first :owner)))
    (is (not-any? #(= "editor-hybrid-schema" (:owner %))
                  (:did-you-mean program)))))

(deftest hypotheses-are-bounded-deterministic-and-never-authoritative
  ;; @spec MCP-OP-READ-HYP-002
  (let [owners (-> (records (mapv #(str "owner-" %) (range 12)))
                   (assoc-in [0 :platforms] [:cljs]))
        opts {:platform :clj}
        first-result (hypotheses/rank-owner-hypotheses
                       "owner-13" owners opts)
        second-result (hypotheses/rank-owner-hypotheses
                        "owner-13" owners opts)]
    (is (= first-result second-result))
    (is (false? (:authority first-result)))
    (is (= :normalized-levenshtein (:ranking-basis first-result)))
    (is (= 11 (:available-owner-count first-result)))
    (is (= 10 (:candidates-returned first-result)))
    (is (:candidates-truncated first-result))
    (is (every? #(false? (:authority %)) (:did-you-mean first-result)))
    (is (every? #(= :normalized-levenshtein (:ranking-basis %))
                (:did-you-mean first-result)))
    (is (every? #(= ["clj"] (:platforms %))
                (:did-you-mean first-result)))
    (is (every? #(not (contains? % :source))
                (:did-you-mean first-result)))))

(def ^:private strict-corpus
  ;; Six one-to-one refusal/recovery pairs from the 2026-08-25 field corpus.
  [["resolve-source-file" "resolve-source-path"
    ["supported-source-extensions" "relative-source-path?" "real-root"
     "path-refusal" "resolve-source-path" "resolve-new-source-path"]]
   ["start-server!" "start"
    ["default-log-file" "server-instructions" "warn" "outcome-classes-by-tool"
     "public-tool-registry" "registered-tools" "make-tools" "live-tool-state"
     "tool-contract" "tool-contracts" "tool-sync-plan" "structured-call-result"
     "create-structured-async-tool" "create-async-tool" "add-tool!" "remove-tool!"
     "register-live-server!" "unregister-live-server!" "sync-tools!"
     "configure-specification" "build-stdio-server" "start-embedded-nrepl!"
     "armor-stdout!" "normalize-option" "start"]]
   ["editor-program-schema" "editor-programs-schema"
    ["verification-schema" "basis-change-schema" "positive-integer-schema"
     "defmethod-owner-schema" "explicit-change-schema" "editor-gesture-schema"
     "editor-programs-schema" "editor-hybrid-schema" "editor-tool-schema"
     "extraction-schema" "clj-change-schema" "closed-object-shape"
     "direct-contract-shape" "direct-change-contract"
     "editor-gesture-contract-shape" "editor-gesture-contract"]
    {:resolved-names ["editor-hybrid-schema"]}]
   ["editor-edit-schema" "editor-gesture-schema"
    ["verification-schema" "basis-change-schema" "positive-integer-schema"
     "defmethod-owner-schema" "explicit-change-schema" "editor-gesture-schema"
     "editor-programs-schema" "editor-hybrid-schema" "editor-tool-schema"
     "extraction-schema" "clj-change-schema" "closed-object-shape"
     "direct-contract-shape" "direct-change-contract"
     "editor-gesture-contract-shape" "editor-gesture-contract"]
    {:resolved-names ["editor-hybrid-schema"]}]
   ["upsert-managed-block" "upsert-workspace-block"
    ["managed-begin" "managed-end" "managed-placeholder" "shared-surgeon-url"
     "shared-cclsp-url" "clojure-extensions" "cclsp-health-url"
     "read-cclsp-health" "await-cclsp-workspace!" "ancestor-directories"
     "repository-root-file" "effective-kondo-config-dir" "cclsp-server"
     "cclsp-config-source" "clojure-server?" "managed-clojure-server?"
     "upsert-cclsp-workspace" "cclsp-config-locks" "cclsp-config-lock"
     "exact-server-persisted?" "install-cclsp-workspace!" "install-cclsp-config!"
     "marker-count" "loopback-mcp-url?" "workspace-mcp-block"
     "managed-tool-header?" "any-table-header?" "remove-existing-tool-tables"
     "upsert-workspace-block" "install-workspace-config!" "command-path"
     "source-tool-root" "run-command!" "require-command!" "up!" "-main"]]
   ["compiles-owner-relative-top-level-insertion"
    "validates-top-level-insertion-without-repeating-owner-source"
    ["valid-request" "gesture-request" "java-json-containers"
     "validates-and-compiles-one-typed-request"
     "validates-and-compiles-one-editor-gesture"
     "editor-gesture-compiles-a-namespace-location"
     "editor-gesture-tolerates-redundant-aggregate-expect"
     "editor-gesture-derives-aggregate-counts"
     "grouped-root-edn-edit-derives-per-file-guards"
     "grouped-root-edn-edit-refuses-ambiguous-scope"
     "editor-gesture-compiles-grouped-exact-owner-deletion"
     "editor-gesture-refuses-invalid-or-mixed-locations"
     "direct-change-accepts-only-closed-verification-profiles"
     "accepts-java-json-containers-from-the-mcp-sdk"
     "preserves-source-spelling-and-owner-punctuation"
     "validates-six-change-multi-file-request"
     "rejects-invalid-typed-requests-exhaustively"
     "normalizes-complete-success-to-terminal-evidence"
     "normalizes-a-running-cold-proof-without-pretending-it-is-terminal"
     "refuses-incomplete-or-unsafe-success-results"
     "classifies-kernel-refusal-with-the-exact-actionable-diagnostic"
     "normalized-refusal-preserves-an-actionable-compiler-diagnostic"
     "verification-refusal-names-the-failed-check-and-bounds-its-output"
     "namespace-owner-crosses-the-json-boundary-as-closed-data"
     "defmethod-owner-crosses-the-json-boundary-as-closed-data"
     "validates-and-compiles-guarded-sibling-insertion"
     "validates-top-level-insertion-without-repeating-owner-source"
     "validates-and-compiles-binding-aware-local-rename"
     "validates-and-compiles-comment-preserving-map-entry-insertion"
     "validates-and-compiles-whole-owner-deletion-without-find"]]])

(deftest strict-field-corpus-has-complete-top-ten-recall
  (let [ranks
        (mapv
          (fn [[requested intended candidates opts]]
            (some (fn [{:keys [rank owner]}]
                    (when (= intended owner) rank))
                  (:did-you-mean
                    (hypotheses/rank-owner-hypotheses
                      requested (records candidates) opts))))
          strict-corpus)]
    (is (= [1 5 1 2 1 7] ranks))
    (is (every? #(<= % 10) ranks))))
