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

;;; ---------------------------------------------------------------------------
;;; Round three: Sol's round-2 review (verdict at
;;; /var/tmp/forge/expectfix-r2-verdict.md) found the guard counting only
;;; lowered `changes`, so the `programs` route was invisible to it.

(def ^:private mixed-source
  "(ns gamma)\n(defn three [] :old)\n(defn four [] \"keep\")\n")

(def ^:private one-program
  {"file" "src/gamma.clj"
   "expression" "(-> (form 'four) (match \"keep\") (transform (constantly \"kept\")))"
   "expect" {"matches" 1 "max_changed_characters" 6}})

(def ^:private one-mixed-edit
  {"file" "src/gamma.clj" "within" {"form" "three"}
   "from" ":old" "to" ":new" "matches" 1})

(defn- execute-mixed
  "One workspace holding src/gamma.clj; returns the receipt and the final bytes."
  [request]
  (let [workspace (temp-dir)]
    (try
      (let [gamma (io/file workspace "src/gamma.clj")]
        (io/make-parents gamma)
        (spit gamma mixed-source)
        {:result (mcp-tool/execute-request!
                   {:project-root (.getPath workspace)
                    :receipt-dir (.getPath (io/file workspace "receipts"))}
                   (assoc request "workspace_root" (.getPath workspace)))
         :workspace-root (.getPath workspace)
         :source (slurp gamma)})
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-EDIT-039
(deftest derived-counts-cover-every-transformation-the-transaction-commits
  ;; Sol fence r2 defect 1. A mixed edit+program request declaring
  ;; {changes 1, edits 1, files 1} PASSED the guard and committed {2,2,1}:
  ;; the guard authorized one transformation and two were written. A guard
  ;; that cannot see half the transaction is not a guard.
  (testing "a one-short expect on a mixed edit+program request refuses"
    (let [{:keys [result source]}
          (execute-mixed {"edits" [one-mixed-edit]
                          "programs" [one-program]
                          "expect" {"changes" 1 "edits" 1 "files" 1}})]
      (is (false? (:ok result)) (pr-str result))
      (is (= "expect-mismatch" (:reason result)))
      (is (= [{:field "changes" :expected 1 :derived 2}
              {:field "edits" :expected 1 :derived 2}]
             (:mismatch result))
          "the program is counted as one change and its matches as edits")
      (is (true? (:source_unchanged result)))
      (is (= mixed-source source) "not one byte was written")
      (is (= 2 (get-in result [:next_call "expect" "changes"])))
      (is (= 2 (get-in result [:next_call "expect" "edits"])))
      (is (= 1 (get-in result [:next_call "expect" "files"])))))
  (testing "the matching expect commits, and the receipt's counts equal it"
    (let [{:keys [result source]}
          (execute-mixed {"edits" [one-mixed-edit]
                          "programs" [one-program]
                          "expect" {"changes" 2 "edits" 2 "files" 1}})]
      (is (:ok result) (pr-str result))
      (is (= 2 (:changes result)))
      (is (= 2 (:edits result)))
      (is (= 1 (:files result)))
      (is (str/includes? source ":new"))
      (is (str/includes? source "kept"))))
  (testing "programs-only cannot execute at all, with or without expect"
    ;; NOT a property of this guard. A request carrying `programs` and no
    ;; change lowers to an empty `changes` array, which the direct validator
    ;; has always refused; proved against the pre-guard base commit f3d922ac
    ;; with no `expect` in the request. The guard therefore stands aside and
    ;; lets the real reason surface, rather than answering an unexecutable
    ;; request with a corrected `expect` that would refuse again.
    (doseq [request [{"programs" [one-program]}
                     {"programs" [one-program]
                      "expect" {"changes" 1 "edits" 1 "files" 1}}
                     {"programs" [one-program]
                      "expect" {"changes" 2 "edits" 2 "files" 2}}]]
      (let [{:keys [result source]} (execute-mixed request)]
        (is (false? (:ok result)) (pr-str result))
        (is (= "non-empty-array" (:reason result))
            "the pre-existing reason, not an expect-mismatch")
        (is (= ["changes"] (:path result)))
        (is (= mixed-source source))))))

;; @spec MCP-OP-EDIT-039
(deftest delete-owners-are-already-inside-the-derived-counts
  ;; delete_owners lower into `changes` before the guard runs, so they were
  ;; never invisible to it. Pinned so a future lowering change cannot quietly
  ;; move them out of the count the way `programs` sat outside it.
  (let [{:keys [result source]}
        (execute-mixed {"delete_owners" [{"file" "src/gamma.clj"
                                          "forms" ["four"]}]
                        "expect" {"changes" 1 "edits" 1 "files" 1}})]
    (is (:ok result) (pr-str result))
    (is (= 1 (:changes result)))
    (is (not (str/includes? source "keep"))))
  (let [{:keys [result source]}
        (execute-mixed {"delete_owners" [{"file" "src/gamma.clj"
                                          "forms" ["four"]}]
                        "expect" {"changes" 2 "edits" 1 "files" 1}})]
    (is (false? (:ok result)) (pr-str result))
    (is (= [{:field "changes" :expected 2 :derived 1}] (:mismatch result)))
    (is (= mixed-source source))))

;; @spec MCP-OP-EDIT-039
(deftest next-call-carries-workspace-root-iff-the-caller-sent-one
  ;; Sol fence r2 defect 3: restoring the resolved root unconditionally makes
  ;; next_call differ from the caller's request by more than `expect`.
  (testing "omitted by the caller, absent from next_call"
    (let [workspace (temp-dir)]
      (try
        (let [gamma (io/file workspace "src/gamma.clj")
              _ (io/make-parents gamma)
              _ (spit gamma mixed-source)
              request {"edits" [one-mixed-edit]
                       "expect" {"changes" 1 "edits" 1 "files" 2}}
              result (mcp-tool/execute-request!
                       {:project-root (.getPath workspace)
                        :receipt-dir (.getPath (io/file workspace "receipts"))}
                       request)]
          (is (false? (:ok result)) (pr-str result))
          (is (nil? (get-in result [:next_call "workspace_root"])))
          (is (= (json-round-trip
                   (assoc request "expect" {"changes" 1 "edits" 1 "files" 1}))
                 (json-round-trip (:next_call result)))
              "the caller's exact request shape, only expect replaced"))
        (finally (delete-tree! workspace)))))
  (testing "sent by the caller, preserved in next_call"
    (let [{:keys [result workspace-root]}
          (execute-mixed {"edits" [one-mixed-edit]
                          "expect" {"changes" 1 "edits" 1 "files" 2}})]
      (is (false? (:ok result)))
      (is (= workspace-root (get-in result [:next_call "workspace_root"]))))))

;; @spec MCP-OP-EDIT-042
(deftest a-zero-declared-count-refuses-at-runtime-on-the-edits-route
  ;; Sol fence r2 probe (c): the published minimum is one, but the only
  ;; witness was schema inspection. This one is a runtime refusal.
  (let [{:keys [result source]}
        (execute-mixed {"edits" [one-mixed-edit]
                        "expect" {"changes" 0 "edits" 1 "files" 1}})]
    (is (false? (:ok result)) (pr-str result))
    (is (= "positive-integer" (:reason result)))
    (is (= ["expect" "changes"] (:path result)))
    (is (true? (:source_unchanged result)))
    (is (= mixed-source source))))

;;; ---------------------------------------------------------------------------
;;; Round four: Sol's round-3 review found the last committed-count escape.
;;; A program is flattened into ONE ADDRESSED INTENT PER CONCRETE MATCH
;;; (mcp-tool/…-with-programs), and the receipt counts those intents, so a
;;; program's contribution to `changes` is its match count, not one.

(def ^:private multi-match-source
  "(ns delta)\n(defn three [] :old)\n(defn four [] [\"keep\" \"keep\"])\n")

(def ^:private two-match-program
  {"file" "src/delta.clj"
   "expression" "(-> (form 'four) (match \"keep\") (transform (constantly \"kept\")))"
   "expect" {"matches" 2 "max_changed_characters" 24}})

(def ^:private one-multi-edit
  {"file" "src/delta.clj" "within" {"form" "three"}
   "from" ":old" "to" ":new" "matches" 1})

(defn- execute-multi
  [request]
  (let [workspace (temp-dir)]
    (try
      (let [delta (io/file workspace "src/delta.clj")]
        (io/make-parents delta)
        (spit delta multi-match-source)
        {:result (mcp-tool/execute-request!
                   {:project-root (.getPath workspace)
                    :receipt-dir (.getPath (io/file workspace "receipts"))}
                   (assoc request "workspace_root" (.getPath workspace)))
         :source (slurp delta)})
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-EDIT-039
(deftest a-multi-match-program-contributes-one-change-per-concrete-match
  ;; Sol fence r3: one literal edit (1 match) plus one program with
  ;; expect.matches=2, declaring {changes 2, edits 3, files 1}, was ACCEPTED
  ;; and committed a receipt whose intent-count was 3. Counting a program as
  ;; ONE change is the same class of blindness as not counting it at all: the
  ;; guard authorizes a size the receipt then exceeds.
  (testing "one change per program is one short, and refuses"
    (let [{:keys [result source]}
          (execute-multi {"edits" [one-multi-edit]
                          "programs" [two-match-program]
                          "expect" {"changes" 2 "edits" 3 "files" 1}})]
      (is (false? (:ok result)) (pr-str result))
      (is (= "expect-mismatch" (:reason result)))
      (is (= [{:field "changes" :expected 2 :derived 3}] (:mismatch result)))
      (is (= multi-match-source source) "not one byte was written")
      (is (= {"changes" 3 "edits" 3 "files" 1}
             (get-in result [:next_call "expect"])))))
  (testing "the per-match count commits, and the receipt reports it"
    (let [{:keys [result source]}
          (execute-multi {"edits" [one-multi-edit]
                          "programs" [two-match-program]
                          "expect" {"changes" 3 "edits" 3 "files" 1}})]
      (is (:ok result) (pr-str result))
      (is (= 3 (:changes result)))
      (is (= 3 (:edits result)))
      (is (= 1 (:files result)))
      (is (= "(ns delta)\n(defn three [] :new)\n(defn four [] [\"kept\" \"kept\"])\n"
             source)))))

;; @spec MCP-OP-EDIT-042
(deftest the-schema-refuses-programs-without-a-gesture-that-lowers-to-changes
  ;; Sol fence r3 judgment: the schema admitted programs-only while the runtime
  ;; refused `non-empty-array` at ["changes"]. Programs ride on a changes
  ;; transaction, so the boundary now says so.
  (let [admits? (fn [request]
                  (boolean (:ok (admission/authorize
                                  schema/clj-change-schema request))))
        edit {"file" "src/delta.clj" "within" {"form" "three"}
              "from" ":old" "to" ":new"}
        program {"file" "src/delta.clj"
                 "expression" "(-> (form 'four) (match \"keep\"))"
                 "expect" {"matches" 1 "max_changed_characters" 6}}]
    (is (false? (admits? {"programs" [program]}))
        "programs alone is denied at the boundary, not only at validation")
    (is (true? (admits? {"programs" [program] "edits" [edit]})))
    (is (true? (admits? {"programs" [program]
                         "delete_owners" [{"file" "src/delta.clj"
                                           "forms" ["four"]}]})))
    (is (str/includes?
          (get-in schema/clj-change-schema [:properties "programs" :description])
          "changes")
        "the description names the changes transaction programs ride on")))
