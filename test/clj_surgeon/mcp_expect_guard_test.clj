(ns ^{:lane :fast} clj-surgeon.mcp-expect-guard-test
  "`expect` is a GUARD, never bookkeeping Surgeon may discard.

   The dogfood-3 run (2026-09-07) sent the fan-out route with
   `expect {changes 3, edits 3, files 3}` and got `ok=true` plus
   `input_normalization {ignored [\"expect\"], reason \"editor counts are
   derived\"}`. The one field that binds the caller's stated fan-out size to
   the effect was accepted by the schema and then thrown away, so a caller who
   mis-states the size gets a silent success over three files."
  (:require
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

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

(deftest editor-expect-that-matches-still-succeeds
  (let [validated (contract/validate-tool-params
                    (editor-request {"changes" 3 "edits" 3 "files" 3}))]
    (is (:ok validated) (pr-str validated))
    (is (= {:changes 3 :edits 3 :files 3} (get-in validated [:params :expect])))
    (is (not (contains? validated :input-normalization)))))

(deftest editor-expect-omitted-is-unguarded-and-unreported
  (let [validated (contract/validate-tool-params (editor-request nil))]
    (is (:ok validated) (pr-str validated))
    (is (= {:changes 3 :edits 3 :files 3} (get-in validated [:params :expect])))
    (is (not (contains? validated :input-normalization))
        "nothing was ignored, so nothing is reported as ignored")))

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
