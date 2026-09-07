(ns ^{:lane :fast} clj-surgeon.mcp-expect-guard-test
  "`expect` is a GUARD, never bookkeeping Surgeon may discard.

   The dogfood-3 run (2026-09-07) sent the fan-out route with
   `expect {changes 3, edits 3, files 3}` and got `ok=true` plus
   `input_normalization {ignored [\"expect\"], reason \"editor counts are
   derived\"}`. The one field that binds the caller's stated fan-out size to
   the effect was accepted by the schema and then thrown away, so a caller who
   mis-states the size gets a silent success over three files."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.experiments.mcp-candidate-admission :as admission]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-schema :as schema]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def ^:private fan-out-edits
  [{"file" "src/a.clj" "within" {"form" "alpha"}
    "from" "current-speaker-identity" "to" "ident/current-speaker-identity"
    "matches" 1}
   {"file" "src/b.clj" "within" {"form" "beta"}
    "from" "current-speaker-identity" "to" "ident/current-speaker-identity"
    "matches" 1}
   {"file" "src/c.clj" "within" {"form" "gamma"}
    "from" "current-speaker-identity" "to" "ident/current-speaker-identity"
    "matches" 1}])

(defn- editor-request
  [expect]
  (cond-> {"edits" fan-out-edits}
    expect (assoc "expect" expect)))

;; @spec MCP-OP-EDIT-039
(deftest editor-expect-mismatch-refuses-before-any-write
  (let [validated (contract/validate-tool-params
                    (editor-request {"changes" 3 "edits" 3 "files" 2}))
        public (contract/normalize-refusal validated)]
    (is (false? (:ok validated)) (pr-str validated))
    (is (= :expect-mismatch (:reason validated)))
    (is (= ["expect"] (:path validated)))
    (is (= [{:field "files" :expected 2 :derived 3}] (:mismatch validated))
        "only the disagreeing field is named, with both values")
    (is (true? (:source-unchanged validated)))
    (is (= 3 (get-in validated [:next-call "expect" "files"]))
        "the composed next_call carries the corrected expect")
    (is (= 3 (get-in validated [:next-call "expect" "changes"])))
    (is (= 3 (get-in validated [:next-call "expect" "edits"])))
    (is (= fan-out-edits (get-in validated [:next-call "edits"]))
        "the corrected call is the caller's own request, expect repaired")
    (testing "the public refusal surface"
      (is (false? (:ok public)))
      (is (= "invalid-mcp-request" (:error_type public))
          "the namespace's own typed-refusal shape: class in :reason")
      (is (= "expect-mismatch" (:reason public)))
      (is (true? (:source_unchanged public)))
      (is (false? (:mutation_attempted public)))
      (is (= [{:field "files" :expected 2 :derived 3}] (:mismatch public)))
      (is (= 3 (get-in public [:next_call "expect" "files"]))))))

;; @spec MCP-OP-EDIT-039
(deftest editor-expect-mismatch-text-carries-the-structured-values
  ;; text ⊇ structured: a client that reads only content[0].text must not be
  ;; told less than one that reads structuredContent.
  (let [public (contract/normalize-refusal
                 (contract/validate-tool-params
                   (editor-request {"changes" 3 "edits" 3 "files" 2})))
        text (mcp-tool/concise-summary (assoc public :elapsed_ms 1.0))]
    (is (str/includes? text "expect-mismatch"))
    (is (str/includes? text "files") "the mismatching field name")
    (is (str/includes? text "expected 2") "the caller's stated value")
    (is (str/includes? text "derived 3") "the value Surgeon derived")
    (is (str/includes? text "✓ source unchanged"))))

;; @spec MCP-OP-EDIT-040
(deftest editor-expect-that-matches-still-succeeds
  (let [validated (contract/validate-tool-params
                    (editor-request {"changes" 3 "edits" 3 "files" 3}))]
    (is (:ok validated) (pr-str validated))
    (is (= {:changes 3 :edits 3 :files 3} (get-in validated [:params :expect])))
    (is (not (contains? validated :input-normalization)))))

;; @spec MCP-OP-EDIT-006
(deftest editor-expect-omitted-is-unguarded-and-unreported
  (let [validated (contract/validate-tool-params (editor-request nil))]
    (is (:ok validated) (pr-str validated))
    (is (= {:changes 3 :edits 3 :files 3} (get-in validated [:params :expect])))
    (is (not (contains? validated :input-normalization))
        "nothing was ignored, so nothing is reported as ignored")))

;; @spec MCP-OP-EDIT-039
(deftest direct-changes-route-guards-expect-too
  ;; The same field, declared on the same verb's other route.
  (let [request {"changes"
                 [{"id" "one"
                   "files" ["src/a.clj"]
                   "forms" ["alpha"]
                   "find" ":old"
                   "replace" ":new"
                   "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
                 "expect" {"changes" 2 "edits" 1 "files" 1}}
        validated (contract/validate-tool-params request)]
    (is (false? (:ok validated)) (pr-str validated))
    (is (= :expect-mismatch (:reason validated)))
    (is (= [{:field "changes" :expected 2 :derived 1}] (:mismatch validated)))
    (is (true? (:source-unchanged validated)))
    (is (= 1 (get-in validated [:next-call "expect" "changes"])))))

;;; ---------------------------------------------------------------------------
;;; Round two: the four defects Sol's executed fence review returned NO-GO on
;;; (2026-09-07, verdict at /var/tmp/forge/expectfix-r1-verdict.md).

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-expect-guard-test-"
             (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- json-round-trip
  "What a client actually receives: the value through JSON and back."
  [value]
  (json/parse-string (json/generate-string value)))

(defn- execute-in-workspace
  "Run one public request against a throwaway workspace holding two sources."
  [request]
  (let [workspace (temp-dir)]
    (try
      (let [alpha (io/file workspace "src/alpha.clj")
            beta (io/file workspace "src/beta.clj")]
        (io/make-parents alpha)
        (spit alpha "(ns alpha)\n(defn one [] :old)\n")
        (spit beta "(ns beta)\n(defn two [] :old)\n")
        {:result (mcp-tool/execute-request!
                   {:project-root (.getPath workspace)
                    :receipt-dir (.getPath (io/file workspace "receipts"))}
                   (assoc request "workspace_root" (.getPath workspace)))
         :workspace-root (.getPath workspace)
         :sources {"src/alpha.clj" (slurp alpha) "src/beta.clj" (slurp beta)}})
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-EDIT-039
(deftest next-call-is-the-callers-own-request-with-only-expect-replaced
  ;; Sol fence r1 defect 1: public execution keywordizes the request and the
  ;; workspace router strips workspace_root, so the guard's string-keyed
  ;; "expect" landed BESIDE the surviving :expect and serialized as duplicate
  ;; JSON keys, with no workspace_root at all. Round-trip equality is the
  ;; witness because duplicate keys are invisible in the Clojure map and fatal
  ;; in the JSON.
  (testing "editor edits route"
    (let [request {"edits" [{"file" "src/alpha.clj" "within" {"form" "one"}
                             "from" ":old" "to" ":new" "matches" 1}
                            {"file" "src/beta.clj" "within" {"form" "two"}
                             "from" ":old" "to" ":new" "matches" 1}]
                   "expect" {"changes" 2 "edits" 2 "files" 1}}
          {:keys [result workspace-root sources]} (execute-in-workspace request)
          next-call (:next_call result)]
      (is (false? (:ok result)) (pr-str result))
      (is (= "expect-mismatch" (:reason result)))
      (is (= [{:field "files" :expected 1 :derived 2}] (:mismatch result)))
      (is (= workspace-root (get (json-round-trip next-call) "workspace_root"))
          "workspace_root survives the router that stripped it")
      (is (= (json-round-trip
               (assoc request
                      "workspace_root" workspace-root
                      "expect" {"changes" 2 "edits" 2 "files" 2}))
             (json-round-trip next-call))
          "the caller's exact request shape, only expect replaced")
      (is (= "(ns alpha)\n(defn one [] :old)\n" (get sources "src/alpha.clj")))
      (is (= "(ns beta)\n(defn two [] :old)\n" (get sources "src/beta.clj")))))
  (testing "direct changes route"
    (let [request {"changes"
                   [{"id" "one"
                     "files" ["src/alpha.clj"]
                     "forms" ["one"]
                     "find" ":old"
                     "replace" ":new"
                     "expect" {"matches" 1 "each_form" 1 "each_file" 1}}]
                   "expect" {"changes" 1 "edits" 2 "files" 1}}
          {:keys [result workspace-root]} (execute-in-workspace request)
          next-call (:next_call result)]
      (is (false? (:ok result)) (pr-str result))
      (is (= [{:field "edits" :expected 2 :derived 1}] (:mismatch result)))
      (is (= workspace-root (get (json-round-trip next-call) "workspace_root")))
      (is (= (json-round-trip
               (assoc request
                      "workspace_root" workspace-root
                      "expect" {"changes" 1 "edits" 1 "files" 1}))
             (json-round-trip next-call))))))

;; @spec MCP-OP-EDIT-042
(deftest published-schema-admits-expect-as-optional-on-every-write-route
  ;; Sol fence r1 defect 2: the boundary REQUIRED expect with changes and
  ;; FORBADE it with edits, and omitted create_files from the route list, so
  ;; neither half of the new contract could be sent at all.
  (let [edit {"file" "src/alpha.clj" "within" {"form" "one"}
              "from" ":old" "to" ":new"}
        change {"id" "one" "files" ["src/alpha.clj"] "forms" ["one"]
                "find" ":old" "replace" ":new"
                "expect" {"matches" 1 "each_form" 1 "each_file" 1}}
        creation {"file" "src/created.clj" "content" "(ns created)\n"}
        expect {"changes" 1 "edits" 1 "files" 1}
        admits? (fn [request]
                  (boolean (:ok (admission/authorize
                                  schema/clj-change-schema request))))]
    (testing "edits, with and without expect"
      (is (true? (admits? {"edits" [edit]})))
      (is (true? (admits? {"edits" [edit] "expect" expect}))))
    (testing "changes, with and without expect"
      (is (true? (admits? {"changes" [change]})))
      (is (true? (admits? {"changes" [change] "expect" expect}))))
    (testing "create-only, with and without expect"
      (is (true? (admits? {"create_files" [creation]})))
      (is (true? (admits? {"create_files" [creation] "expect" expect}))))
    (testing "the compact-relation route still refuses expect at the boundary"
      ;; mcp-compact-relations/allowed-request-fields rejects it, so the schema
      ;; must not promise a shape the adapter refuses.
      (is (false? (admits? {"symbol_migration" [] "require_change" {}
                            "expect" expect}))))
    (testing "each declared count is a positive integer"
      (is (= 1 (get-in schema/clj-change-schema
                       [:properties "expect" :properties "changes" :minimum])))
      (is (= 1 (get-in schema/clj-change-schema
                       [:properties "expect" :properties "edits" :minimum])))
      (is (= 1 (get-in schema/clj-change-schema
                       [:properties "expect" :properties "files" :minimum]))))))

;; @spec MCP-OP-EDIT-041
(deftest expect-on-a-create-only-transaction-is-refused-as-unsupported
  ;; Sol fence r1 defect 3: derived counts for create-only are all zero, so
  ;; every "corrected" expect the guard could compose is one the schema's own
  ;; minimum of 1 rejects. The chosen rule is an explicit unsupported-on-route
  ;; refusal with NO next_call, never a remedy the boundary would reject.
  (let [request {"create_files" [{"file" "src/created.clj"
                                  "content" "(ns created)\n"}]
                 "expect" {"changes" 1 "edits" 1 "files" 1}}
        validated (contract/validate-tool-params request)
        public (contract/normalize-refusal validated)]
    (is (false? (:ok validated)) (pr-str validated))
    (is (= :expect-unsupported-on-route (:reason validated)))
    (is (= ["expect"] (:path validated)))
    (is (true? (:source-unchanged validated)))
    (is (not (contains? validated :next-call))
        "no corrected next_call: every candidate violates the schema minimum")
    (is (str/includes? (:error validated)
                       "expect unsupported on route create_files"))
    (testing "the public surface and its text"
      (is (false? (:mutation_attempted public)))
      (is (not (contains? public :next_call)))
      (let [text (mcp-tool/concise-summary (assoc public :elapsed_ms 1.0))]
        (is (str/includes? text "expect-unsupported-on-route"))
        (is (str/includes? text "expect unsupported on route create_files"))
        (is (str/includes? text "✓ source unchanged")))))
  (testing "create-only without expect is unchanged"
    (let [validated (contract/validate-tool-params
                      {"create_files" [{"file" "src/created.clj"
                                        "content" "(ns created)\n"}]})]
      (is (:ok validated) (pr-str validated)))))
