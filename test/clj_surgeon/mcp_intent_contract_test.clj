(ns clj-surgeon.mcp-intent-contract-test
  {:lane :fast}
  (:require
   [clj-surgeon.mcp-intent-contract]
   [clj-surgeon.runner-membership :as membership]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]))

;; RATCHET (2026-09-04, inb-9483a4): `temp-dir` below never deleted what it
;; created (surgeon-intent-scan / surgeon-intent-empty). Track and sweep.
(def ^:private temp-roots (atom []))
(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots))

(defn- audit-contract
  [input]
  ((requiring-resolve 'clj-surgeon.mcp-intent-contract/audit-contract) input))

(defn- violations
  [result]
  (set (map #(select-keys % [:type :intent :source-kind])
            (:violations result))))

(defn- spec-line
  [status intent]
  (str "- [" status "] **" intent "**: fixture requirement\n"))

(defn- annotation
  [intent]
  (str ";; @" "spec " intent "\n"))

;; @spec MCP-OP-TRACE-006
(deftest sentinel-intent-audit-is-required-by-the-merge-gate
  (let [makefile (slurp "Makefile")
        target "performance-regression-sentinel-intent-test"
        merge-gate (membership/make-target makefile "mcp-test")
        sentinel (membership/make-target makefile "performance-regression-sentinel-test")
        audit (membership/make-target makefile target)]
    (is (some #{target} (:prerequisites merge-gate))
        "the ordinary merge gate must execute the sentinel witness audit")
    (is (some #{target} (:prerequisites sentinel))
        "the full sentinel suite must reuse the same audit")
    (is (= ["bash test/performance_regression_sentinel_intent_test.sh"
            "bash test/performance_regression_sentinel_intent_self_test.sh"]
           (mapv str/trim (str/split-lines (or (:recipe audit) ""))))
        "the prerequisite must execute the audit and propagate its exit")))

;; @spec MCP-OP-TRACE-005
(deftest non-mcp-intent-with-missing-witnesses-is-reported
  ;; September 7 audit gap 1: a literal WTL-shaped fixture, not live WTL edits.
  (let [intent "WTL-FIXTURE-001"]
    (is (= [{:type :missing-implementation-witness
             :intent intent :source-kind :implementation}
            {:type :missing-test-witness
             :intent intent :source-kind :test}]
           (:violations
             (audit-contract {:spec-text (spec-line "x" intent)
                              :implementation-sources {}
                              :test-sources {}}))))))

;; @spec MCP-OP-TRACE-005
(deftest prefix-agnostic-witness-rules-preserve-the-legacy-contract
  (doseq [intent ["MCP-OP-FIXTURE-001" "MCP-OP-LEGACY-NONNUMERIC"
                  "MCP-OP-LEGACY-1" "WTL-APPLY-001" "PERF-SENT-TIME-001"
                  "OP-ALG-COMMIT-001" "TEST-ISO-003" "MEASURE-WALL-001"
                  "TELEMETRY-EVENTS-001" "FUTURE2-001" "A-001"]
          [status implementation? test? expected]
          [["x" false false [:missing-implementation-witness :missing-test-witness]]
           ["x" true false [:missing-test-witness]]
           ["x" false true [:missing-implementation-witness]]
           ["x" true true []]
           [" " false false [:missing-test-witness]]
           [" " true false [:missing-test-witness]]
           [" " false true []]
           [" " true true []]
           ["D" false false []]
           ["D" true false []]
           ["D" false true []]
           ["D" true true []]]]
    (testing (str [intent status implementation? test?])
      (let [result (audit-contract
                     {:spec-text (spec-line status intent)
                      :implementation-sources (if implementation? {"f.clj" (annotation intent)} {})
                      :test-sources (if test? {"t.clj" (annotation intent)} {})})]
        (is (= [intent] (vec (keys (:specs result)))))
        (is (= expected (mapv :type (:violations result))))
        (is (= (empty? expected) (:ok result))))))
  (doseq [intent ["WTL-UNKNOWN-999" "FUTURE2-999" "TELEMETRY-EVENTS-001"]]
    (is (= [{:type :unknown-intent-witness :intent intent :source-kind :implementation}
            {:type :unknown-intent-witness :intent intent :source-kind :test}]
           (:violations
             (audit-contract {:spec-text ""
                              :implementation-sources {"f.clj" (annotation intent)}
                              :test-sources {"t.clj" (annotation intent)}}))))))

;; @spec MCP-OP-TRACE-005
(deftest generic-witnesses-require-complete-numeric-identifiers
  (doseq [intent ["PERF-SENT-" "TEST-ISO-" "WTL-0012" "WTL-001-MORE" "WTL-01"]]
    (is (empty? (:implementation-witnesses
                  (audit-contract {:spec-text ""
                                   :implementation-sources {"f.clj" (annotation intent)}
                                   :test-sources {}})))))
  (is (= #{"TEST-ISO-002"}
         (:implementation-witnesses
           (audit-contract {:spec-text ""
                            :implementation-sources {"f.clj" (annotation "TEST-ISO-002/003/004")}
                            :test-sources {}})))))

;; @spec MCP-OP-TRACE-005
(deftest legacy-mcp-amendment-spelling-keeps-its-historical-result
  ;; The legacy row parser ignores lowercase suffixes; its witness parser reads
  ;; the uppercase/numeric stem. Widening must not reinterpret that MCP behavior.
  (is (:ok (audit-contract
             {:spec-text (spec-line "x" "MCP-OP-FIXTURE-001")
              :implementation-sources {"f.clj" (annotation "MCP-OP-FIXTURE-001a")}
              :test-sources {"t.clj" (annotation "MCP-OP-FIXTURE-001a")}})))
  (is (= {:ok false :specs {}
          :implementation-witnesses #{"MCP-OP-FIXTURE-001"}
          :test-witnesses #{}
          :violations [{:type :unknown-intent-witness
                        :intent "MCP-OP-FIXTURE-001" :source-kind :implementation}]}
         (audit-contract {:spec-text (spec-line "x" "MCP-OP-FIXTURE-001a")
                          :implementation-sources {"f.clj" (annotation "MCP-OP-FIXTURE-001a")}
                          :test-sources {}}))))

;; @spec MCP-OP-TRACE-005
(deftest non-mcp-amendment-identifiers-are-not-parent-witnesses
  (let [parent "TEST-ISO-001"
        amendment "TEST-ISO-001a"
        result (audit-contract
                 {:spec-text (str (spec-line "x" parent) (spec-line "x" amendment))
                  :implementation-sources {"f.clj" (annotation amendment)}
                  :test-sources {"t.clj" (annotation amendment)}})]
    (is (= {parent :implemented amendment :implemented} (:specs result)))
    (is (= #{amendment} (:implementation-witnesses result)))
    (is (= #{amendment} (:test-witnesses result)))
    (is (= [{:type :missing-implementation-witness :intent parent :source-kind :implementation}
            {:type :missing-test-witness :intent parent :source-kind :test}]
           (:violations result)))))

;; @spec MCP-OP-TRACE-005
(deftest exact-witness-debt-rejects-growth-repairs-and-orphans
  ;; Sol's September 7 review: removing WTL-APPLY-001's real test marker
  ;; grew debt 144 -> 145; repairing MEASURE-EVID-001 did not retire its debt.
  (let [gate (requiring-resolve 'clj-surgeon.mcp-intent-contract/defer-missing-witnesses)
        intent "WTL-APPLY-001"
        raw (fn [status impl? test?]
              (audit-contract
                {:spec-text (spec-line status intent)
                 :implementation-sources (if impl? {"f.clj" (annotation intent)} {})
                 :test-sources (if test? {"t.clj" (annotation intent)} {})}))]
    (doseq [[kind impl? test?] [[:implementation false true] [:test true false]]]
      (testing (str "new missing witness remains blocking: " kind)
        (is (:ok (gate (raw "x" true true) {})))
        (is (false? (:ok (gate (raw "x" impl? test?) {})))))
      (testing (str "one exact missing pair can be deferred, then must retire: " kind)
        (is (:ok (gate (raw "x" impl? test?) {intent #{kind}})))
        (is (= [{:type :stale-witness-debt :intent intent :source-kind kind}]
               (:violations (gate (raw "x" true true) {intent #{kind}})))))
      (testing "the other witness kind cannot silently join the ledger"
        (is (false? (:ok (gate (raw "x" false false) {intent #{kind}}))))))
    (testing "partial repair forces removal of that pair, retaining the other debt"
      (is (= [{:type :stale-witness-debt :intent intent :source-kind :implementation}]
             (:violations (gate (raw "x" true false) {intent #{:implementation :test}}))))
      (is (:ok (gate (raw "x" true false) {intent #{:test}}))))
    (testing "status changes cannot retain obsolete exceptions"
      (is (false? (:ok (gate (raw " " false false) {intent #{:implementation :test}}))))
      (is (false? (:ok (gate (raw "D" false false) {intent #{:test}})))))
    (testing "an orphan ledger ID is a failure even without any source annotations"
      (is (= [{:type :unknown-intent-debt :intent "WTL-GONE-999"}]
             (:violations (gate (raw "x" true true) {"WTL-GONE-999" #{:test}})))))))

;; @spec MCP-OP-TRACE-005
(deftest invalid-witness-debt-ledgers-fail-closed
  (let [gate (requiring-resolve 'clj-surgeon.mcp-intent-contract/defer-missing-witnesses)
        raw (audit-contract {:spec-text "" :implementation-sources {} :test-sources {}})]
    (doseq [ledger [nil [] {"WTL-001" #{}} {"WTL-001" #{:other}}
                    {"WTL-001" [:test]} {:WTL-001 #{:test}}]]
      (is (= :invalid-witness-debt-ledger
             (try (gate raw ledger) nil
                  (catch clojure.lang.ExceptionInfo e (:type (ex-data e)))))
          (pr-str ledger)))))

;; @spec MCP-OP-TRACE-005
(deftest explicit-id-debt-never-hides-unknown-or-new-prefixes
  (let [defer-missing-witnesses
        (requiring-resolve 'clj-surgeon.mcp-intent-contract/defer-missing-witnesses)
        debt-id "WTL-FIXTURE-001"
        future-id "FUTURE2-001"
        unknown-id "WTL-UNKNOWN-999"
        raw (audit-contract
              {:spec-text (str (spec-line "x" debt-id) (spec-line "x" future-id))
               :implementation-sources {"f.clj" (annotation unknown-id)}
               :test-sources {"t.clj" (annotation unknown-id)}})
        allowed {debt-id #{:implementation :test}}
        gated (defer-missing-witnesses raw allowed)]
    (is (= raw (defer-missing-witnesses raw {})))
    (is (false? (:ok gated)))
    (is (= [{:type :missing-implementation-witness :intent debt-id :source-kind :implementation}
            {:type :missing-test-witness :intent debt-id :source-kind :test}]
           (:pending-witness-violations gated)))
    (is (= [{:type :missing-implementation-witness :intent future-id :source-kind :implementation}
            {:type :missing-test-witness :intent future-id :source-kind :test}
            {:type :unknown-intent-witness :intent unknown-id :source-kind :implementation}
            {:type :unknown-intent-witness :intent unknown-id :source-kind :test}]
           (:violations gated)))
    (is (= (select-keys raw [:specs :implementation-witnesses :test-witnesses])
           (select-keys gated [:specs :implementation-witnesses :test-witnesses])))
    (is (= allowed (:witness-debt-ledger gated)))
    (let [only-debt (audit-contract {:spec-text (spec-line "x" debt-id)
                                     :implementation-sources {} :test-sources {}})]
      (is (:ok (defer-missing-witnesses only-debt allowed)))
      (is (false? (:ok only-debt))))))

;; @spec MCP-OP-TRACE-001
(deftest active-gap-requires-a-direct-test-witness
  (let [intent "MCP-OP-FIXTURE-001"
        spec (spec-line " " intent)]
    (is (= #{{:type :missing-test-witness
              :intent intent
              :source-kind :test}}
           (violations
             (audit-contract {:spec-text spec
                              :implementation-sources {}
                              :test-sources {}}))))
    (is (:ok (audit-contract
               {:spec-text spec
                :implementation-sources {}
                :test-sources {"fixture_test.clj" (annotation intent)}})))))

;; @spec MCP-OP-TRACE-002
(deftest unknown-implementation-and-test-annotations-are-rejected
  (let [known "MCP-OP-FIXTURE-001"
        unknown "MCP-OP-FIXTURE-999"
        result (audit-contract
                 {:spec-text (spec-line "D" known)
                  :implementation-sources
                  {"fixture.clj" (annotation unknown)}
                  :test-sources
                  {"fixture_test.clj" (annotation unknown)}})]
    (is (= #{{:type :unknown-intent-witness
              :intent unknown
              :source-kind :implementation}
             {:type :unknown-intent-witness
              :intent unknown
              :source-kind :test}}
           (violations result)))))

;; @spec MCP-OP-TRACE-003
(deftest implemented-intent-requires-both-implementation-and-test-witnesses
  (let [intent "MCP-OP-FIXTURE-001"
        spec (spec-line "x" intent)]
    (is (= #{{:type :missing-implementation-witness
              :intent intent
              :source-kind :implementation}
             {:type :missing-test-witness
              :intent intent
              :source-kind :test}}
           (violations
             (audit-contract {:spec-text spec
                              :implementation-sources {}
                              :test-sources {}}))))
    (is (:ok
          (audit-contract
            {:spec-text spec
             :implementation-sources {"fixture.clj" (annotation intent)}
             :test-sources {"fixture_test.clj" (annotation intent)}})))))

;; @spec MCP-OP-TRACE-004
(deftest deferred-intent-needs-no-placeholder-witness
  (let [result (audit-contract
                 {:spec-text (spec-line "D" "MCP-OP-FIXTURE-001")
                  :implementation-sources {}
                  :test-sources {}})]
    (is (:ok result))
    (is (empty? (:violations result)))))

(deftest repository-operation-intent-contract-is-coherent
  (let [audit-current-repository
        (requiring-resolve
          'clj-surgeon.mcp-intent-contract/audit-current-repository)
        result (audit-current-repository)]
    (is (:ok result) (pr-str (:violations result)))
    (is (= (set (for [[intent kinds] (:witness-debt-ledger result)
                      kind kinds]
                  [intent kind]))
           (set (map (juxt :intent :source-kind) (:pending-witness-violations result))))
        "every exact debt pair remains missing; remove repaired or orphan entries")
    (let [unrestricted (audit-current-repository "." {})]
      (is (= (empty? (:pending-witness-violations result)) (:ok unrestricted)))
      (is (= (vec (:pending-witness-violations result)) (:violations unrestricted)))
      (is (= (:specs result) (:specs unrestricted))))))

;; ---------------------------------------------------------------------------
;; The spec-document registry is DERIVED, not listed.
;;
;; Ratchet, 2026-09-03 (integration branch): the audited spec documents used to
;; live in a literal vector inside `audit-current-repository`. Every lane that
;; added an intent leaf appended a line to that one vector, so every lane
;; conflicted with every other lane by construction. The list is now scanned from
;; docs/intent/<leaf>/<name>-specs.md, so a new lane adds a FILE and touches no
;; shared line. These witnesses keep the scan honest: pickup, loud failure on an
;; orphan listing, named reasons for every exclusion, and an EXACT expected set
;; so drift is visible rather than silent.
;; ---------------------------------------------------------------------------

(defn- spec-doc-paths
  ([] ((requiring-resolve 'clj-surgeon.mcp-intent-contract/spec-doc-paths)))
  ([root] ((requiring-resolve 'clj-surgeon.mcp-intent-contract/spec-doc-paths) root))
  ([root excluded]
   ((requiring-resolve 'clj-surgeon.mcp-intent-contract/spec-doc-paths) root excluded)))

(defn- excluded-spec-docs
  []
  @(requiring-resolve 'clj-surgeon.mcp-intent-contract/excluded-spec-docs))

(defn- spec-ids
  "The intent IDs the audit would parse out of these repo-relative files."
  [root paths]
  (set (keys (:specs (audit-contract
                       {:spec-text (str/join
                                     "\n"
                                     (map #(slurp (io/file root %)) paths))
                        :implementation-sources {}
                        :test-sources {}})))))

(defn- temp-dir
  [prefix]
  (str (tmp-leak/track!
         temp-roots
         (java.nio.file.Files/createTempDirectory
           prefix (into-array java.nio.file.attribute.FileAttribute [])))))

(defn- witness-side-spec-docs
  "The `*-specs.md` documents under `<root>/docs/intent`, discovered by a
   RECURSIVE walk written independently of `spec-doc-paths` (which lists exactly
   two levels). Repo-relative, sorted, `excluded` removed."
  [root excluded]
  (let [base (io/file root "docs" "intent")
        prefix (str (.getPath base) java.io.File/separator)]
    (->> (file-seq base)
         (filter #(.isFile ^java.io.File %))
         (filter #(re-matches #".+-specs\.md" (.getName ^java.io.File %)))
         (map (fn [^java.io.File f]
                (str "docs/intent/" (subs (.getPath f) (count prefix)))))
         (remove (set (keys excluded)))
         sort
         vec)))

(defn- witness-side-ids
  "The intent ids `paths` register, parsed by this witness's own spelling of the
   ledger row rule rather than by the production parser."
  [root paths]
  (set (for [path paths
             [_ id] (re-seq #"(?m)^- \[[ xD]\] \*\*([A-Z][A-Z0-9-]*-[0-9]{3}[a-z]?)\*\*:"
                            (slurp (io/file root path)))]
         id)))

(defn- prefix-histogram
  "id-prefix -> how many rows carry it, DERIVED from the ids themselves: the
   prefix is everything before an id's trailing `-NNN[a]`. Replaces a frozen map
   of seven remembered counts, so a new family appears here by being registered
   and none of them is a line two branches must both edit."
  [ids]
  (into (sorted-map)
        (frequencies (keep #(second (re-matches #"(.+-)[0-9]{3}[a-z]?" %)) ids))))

(defn- ledger-diff
  "Set difference in BOTH directions between two derivations of the ledger. nil
   when they agree; otherwise `:missing` (the witness sees it, the audit does
   not -- a registered intent the audit is blind to) and `:extra` (the audit
   sees it, the witness does not)."
  [witness production]
  (let [witness (set witness)
        production (set production)
        missing (vec (sort (remove production witness)))
        extra (vec (sort (remove witness production)))]
    (when (or (seq missing) (seq extra))
      {:missing missing :extra extra})))

(defn- ledger-diff-message
  [subject diff]
  (str subject ": the two independent derivations of the intent ledger disagree. "
       "Registered in docs/intent but INVISIBLE to the audit ("
       (count (:missing diff)) "): "
       (if (seq (:missing diff)) (str/join ", " (:missing diff)) "none")
       ". Seen by the audit but not by this witness's own walk ("
       (count (:extra diff)) "): "
       (if (seq (:extra diff)) (str/join ", " (:extra diff)) "none")
       ". Name the row -- never re-pin a count."))

(deftest a-new-intent-leaf-is-picked-up-by-adding-only-a-file
  (testing "a lane adds docs/intent/<leaf>/<leaf>-specs.md and nothing else"
    (let [root (temp-dir "surgeon-intent-scan")
          leaf (io/file root "docs" "intent" "temp-lane")]
      (.mkdirs leaf)
      (spit (io/file leaf "temp-lane-specs.md")
            (str (spec-line "x" "MCP-OP-TEMPLANE-001")
                 (spec-line "x" "FUTURE2-001")))
      ;; a sibling that is NOT a spec document, and a `-specs.from-*.md` variant,
      ;; must both be ignored.
      (spit (io/file leaf "temp-lane-design.md") "design\n")
      (spit (io/file leaf "temp-lane-specs.from-docs--x.md")
            (spec-line "x" "MCP-OP-TEMPLANE-999"))
      (is (= ["docs/intent/temp-lane/temp-lane-specs.md"] (spec-doc-paths root {})))
      (is (= #{"MCP-OP-TEMPLANE-001" "FUTURE2-001"}
             (spec-ids root (spec-doc-paths root {})))))
    ;; @spec TEST-ISO-015
    ;; INTENT-TEST: TEST-ISO-015
    ;; ZERO EDITS UNDER test/. A SECOND leaf, added to the same fixture tree with
    ;; no change to any witness datum, must be picked up by BOTH derivations and
    ;; leave them agreeing -- which is the property the two hand-kept vectors
    ;; (`expected-spec-docs`, `lanes-added-since-derivation`) used to break: each
    ;; new leaf meant a line in a shared witness list, and two branches adding two
    ;; leaves collided there.
    (testing "adding a second leaf keeps both derivations agreeing, no test edit"
      (let [root (temp-dir "surgeon-intent-newleaf")
            first-leaf (io/file root "docs" "intent" "one-lane")
            second-leaf (io/file root "docs" "intent" "two-lane")]
        (.mkdirs first-leaf)
        (spit (io/file first-leaf "one-lane-specs.md") (spec-line "x" "ONELANE-001"))
        (is (nil? (ledger-diff (witness-side-spec-docs root {}) (spec-doc-paths root {})))
            "one leaf: the two derivations must already agree")
        (.mkdirs second-leaf)
        (spit (io/file second-leaf "two-lane-specs.md")
              (str (spec-line "x" "TWOLANE-001") (spec-line "x" "MCP-OP-TWOLANE-001")))
        (let [paths (spec-doc-paths root {})]
          (is (nil? (ledger-diff (witness-side-spec-docs root {}) paths))
              "two leaves: still agreeing, with nothing edited under test/")
          (is (= ["docs/intent/one-lane/one-lane-specs.md"
                  "docs/intent/two-lane/two-lane-specs.md"]
                 paths))
          (is (= #{"ONELANE-001" "TWOLANE-001" "MCP-OP-TWOLANE-001"}
                 (spec-ids root paths))
              "the new leaf's rows join the ledger by existing, not by a bump")
          (is (= {"ONELANE-" 1 "TWOLANE-" 1}
                 (prefix-histogram (remove #(str/starts-with? % "MCP-OP-")
                                           (spec-ids root paths))))
              "and the prefix breakdown grows a key rather than needing one"))))))

(deftest an-orphan-spec-doc-listing-fails-loudly
  (testing "an exclusion naming a file that does not exist throws, never shrinks silently"
    (let [thrown (try
                   (spec-doc-paths "." {"docs/intent/no-such-leaf/no-such-leaf-specs.md"
                                        "deliberately absent fixture"})
                   nil
                   (catch clojure.lang.ExceptionInfo e e))]
      (is (some? thrown) "an orphan listing must throw")
      (is (= :orphan-spec-doc-listing (:type (ex-data thrown))))
      (is (= ["docs/intent/no-such-leaf/no-such-leaf-specs.md"]
             (:paths (ex-data thrown)))))))

(deftest an-empty-intent-tree-fails-loudly
  (testing "a moved or emptied docs/intent is an error, not an empty audit"
    (let [root (temp-dir "surgeon-intent-empty")
          thrown (try (spec-doc-paths root {}) nil
                      (catch clojure.lang.ExceptionInfo e e))]
      (is (some? thrown))
      (is (= :no-spec-docs-found (:type (ex-data thrown)))))))

(deftest every-spec-doc-exclusion-carries-a-named-reason
  (testing "the exclusion set is non-empty only for named, existing reasons"
    (doseq [[path reason] (excluded-spec-docs)]
      (is (string? path))
      (is (.isFile (io/file "." path))
          (str "an excluded spec document must exist: " path))
      (is (and (string? reason) (<= 40 (count (str/trim reason))))
          (str "exclusion needs a substantive one-line reason: " path)))))

(def ^:private pre-derivation-literal-vector
  "The literal vector `audit-current-repository` carried before the registry was
   derived (main @ 99394bf). FROZEN HISTORY -- it is a record of what the audit
   used to cover, never a description of the tree today, so it is never edited
   again. Adding an intent leaf does not touch it; that is the whole point."
  ["docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md"
   "docs/intent/read-request-normalization/read-request-normalization-specs.md"
   "docs/intent/prepared-request/prepared-request-specs.md"
   "docs/intent/prepared-request-actions/prepared-request-actions-specs.md"
   "docs/intent/write-refusal-completeness/write-refusal-completeness-specs.md"
   "docs/intent/insertion-boundary-and-gap/insertion-boundary-and-gap-specs.md"
   "docs/intent/shell-argv-safety/shell-argv-safety-specs.md"])

;; @spec TEST-ISO-015
(deftest the-derived-spec-doc-set-matches-the-expected-set-exactly
  ;; This used to compare the scan against a hand-kept vector of thirty paths,
  ;; whose own docstring told a lane adding an intent leaf to append a line to it
  ;; -- the additive shared-line class, inside the witness that exists to kill it.
  ;; The expectation is now an INDEPENDENT WALK of the same tree, so a new leaf
  ;; is picked up by both sides with zero edits under test/.
  (testing "the production registry equals an independent walk of docs/intent"
    (let [diff (ledger-diff (witness-side-spec-docs "." (excluded-spec-docs))
                            (spec-doc-paths "."))]
      (is (nil? diff) (ledger-diff-message "spec documents" diff))))
  (testing "an exclusion hides a document that IS there, and only that document"
    ;; The one thing the two walks cannot check about each other, because both
    ;; apply the same exclusion map: that an excluded path names a real file the
    ;; unfiltered walk finds. Without this an exclusion could quietly cover a
    ;; typo instead of a document.
    (let [unfiltered (set (witness-side-spec-docs "." {}))
          excluded (set (keys (excluded-spec-docs)))]
      (is (empty? (remove unfiltered excluded))
          (str "excluded spec document(s) that the unfiltered walk does not "
               "find: " (str/join ", " (sort (remove unfiltered excluded)))))
      (is (= (set (spec-doc-paths ".")) (set (remove excluded unfiltered)))
          "the scan must be exactly the tree minus the declared exclusions"))))

;; @spec TEST-ISO-015
(deftest the-derived-audit-preserves-the-registered-mcp-intents
  ;; Was: the derived MCP-OP set must EQUAL the ids of a frozen vector plus a
  ;; hand-kept `lanes-added-since-derivation` list -- so every branch that added
  ;; a leaf with MCP-OP rows had to edit that list. The guarantee that actually
  ;; matters is one-directional and needs no list: nothing the audit covered
  ;; BEFORE the registry was derived may be uncovered now. Widening is free;
  ;; losing coverage is named.
  (testing "no MCP-OP intent the pre-derivation audit covered is now unaudited"
    (let [registered (spec-ids "." pre-derivation-literal-vector)
          derived (spec-ids "." (spec-doc-paths "."))
          lost (sort (remove derived
                             (filter #(str/starts-with? % "MCP-OP-") registered)))]
      (is (seq registered) "the frozen pre-derivation documents must still parse")
      (is (empty? lost)
          (str (count lost) " MCP-OP intent(s) the audit covered before the "
               "registry was derived are no longer discovered: "
               (str/join ", " lost))))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-015
;; INTENT: TEST-ISO-015
;;
;; THE LEDGER IS DERIVED FROM docs/intent, NEVER PINNED AS A COUNT.
;;
;; Until 2026-09-09 the assertion below was `(is (= 253 (count non-mcp)))` plus a
;; frozen per-prefix map. Every branch that registered an intent had to bump a
;; number that names no intent, and the comment block this replaces records the
;; consequence twice over: on 2026-09-08 the merge-base ledger was 238, one side
;; moved it to 245 and the other to 239 for DISJOINT ids, and the only way anyone
;; could resolve it was to recount the tree by hand -- exactly the derivation the
;; witness should have been doing in the first place. A count is also blind to a
;; SWAP: an id that vanishes while another is registered leaves the total intact.
;;
;; So the expectation is derived twice, INDEPENDENTLY, and compared as sets:
;;   production -- `clj-surgeon.mcp-intent-contract/spec-doc-paths`, the registry
;;                 the audit itself walks (two-level `docs/intent/<leaf>/`);
;;   witness    -- a recursive walk written here, with its own spelling of the
;;                 `*-specs.md` rule and its own id regex.
;; Two derivations that must agree catch what one cannot: a spec document the
;; production scan cannot reach (nested a directory deeper, say) is invisible to
;; the audit while looking perfectly registered to a reader. NO COUNT IS
;; ASSERTED, not even as a `>=` floor: a floor at a historical count is the same
;; shared number under a weaker operator. The one admissible guard is
;; NON-EMPTINESS, so an emptied tree fails loudly instead of agreeing with itself.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-TRACE-005
;; @spec TEST-ISO-015
(deftest the-derived-audit-includes-every-previously-invisible-row
  (let [production-paths (spec-doc-paths ".")
        witness-paths (witness-side-spec-docs "." (excluded-spec-docs))
        ids (spec-ids "." production-paths)
        non-mcp (set (remove #(str/starts-with? % "MCP-OP-") ids))
        witness-ids (witness-side-ids "." witness-paths)
        witness-non-mcp (set (remove #(str/starts-with? % "MCP-OP-") witness-ids))]
    (testing "the registry the audit walks equals an independent walk of the tree"
      (let [diff (ledger-diff witness-paths production-paths)]
        (is (nil? diff) (ledger-diff-message "spec documents" diff))))
    (testing "every row registered in the tree is a row the audit derives"
      (let [diff (ledger-diff witness-non-mcp non-mcp)]
        (is (nil? diff) (ledger-diff-message "non-MCP intent rows" diff))))
    (testing "the per-prefix breakdown agrees between the two derivations"
      ;; The prefix histogram used to be a frozen map of seven counts. It is now
      ;; derived on BOTH sides from the ids themselves: a prefix appears because
      ;; rows carry it, and its size is whatever the tree says. Two independent
      ;; derivations agreeing is the check; a remembered number never was one.
      (is (= (prefix-histogram witness-non-mcp) (prefix-histogram non-mcp))
          (str "the two derivations disagree on the per-prefix breakdown: "
               "witness " (pr-str (prefix-histogram witness-non-mcp))
               " vs audit " (pr-str (prefix-histogram non-mcp)))))
    (testing "discovery is non-empty, so nothing above passes vacuously"
      ;; The ONLY legitimate count here. No floor against the 2026-09-08 corpus:
      ;; a floor blesses the old shared number under `>=` and still has to be
      ;; argued about at every merge. Membership is settled member by member
      ;; above; all that is left is that the scan found anything at all.
      (is (seq production-paths) "the registry discovered no spec documents")
      (is (seq non-mcp) "the derived ledger holds no non-MCP rows at all")
      (doseq [[prefix n] (prefix-histogram non-mcp)]
        (is (pos? n)
            (str "prefix " prefix " exists in the tree but derives no rows"))))))

;; @spec TEST-ISO-015
;; INTENT-TEST: TEST-ISO-015
(deftest an-intent-the-registry-cannot-reach-is-named-not-silently-dropped
  ;; RED ON DEMAND, in a FIXTURE tree under java.io.tmpdir -- never the live one.
  ;; The defect: a lane registers intents in a spec document the production scan
  ;; cannot reach (here, nested one directory deeper than its two-level walk).
  ;; The document reads as registered, the audit never sees the ids, and a COUNT
  ;; pin agrees with itself while the promises go unwitnessed.
  (let [root (temp-dir "surgeon-intent-census")
        leaf (io/file root "docs" "intent" "reachable-lane")
        nested (io/file root "docs" "intent" "nested-lane" "deeper")]
    (.mkdirs leaf)
    (.mkdirs nested)
    (spit (io/file leaf "reachable-lane-specs.md") (spec-line "x" "FIXTURE-001"))
    (spit (io/file nested "nested-lane-specs.md") (spec-line "x" "FIXTURE-002"))
    (let [production-paths (spec-doc-paths root {})
          witness-paths (witness-side-spec-docs root {})
          production-ids (spec-ids root production-paths)
          witness-ids (witness-side-ids root witness-paths)
          diff (ledger-diff witness-ids production-ids)]
      (testing "the production registry reaches only the two-level document"
        (is (= ["docs/intent/reachable-lane/reachable-lane-specs.md"] production-paths))
        (is (= #{"FIXTURE-001"} production-ids)))
      (testing "the independent walk reaches both, and the diff NAMES the gap"
        (is (= ["docs/intent/nested-lane/deeper/nested-lane-specs.md"
                "docs/intent/reachable-lane/reachable-lane-specs.md"]
               witness-paths))
        (is (= {:missing ["FIXTURE-002"] :extra []} diff))
        (is (str/includes? (ledger-diff-message "non-MCP intent rows" diff)
                           "FIXTURE-002")))
      (testing "and a count pin cannot see it"
        ;; One row registered, one row audited: the totals a count would compare
        ;; are both 1 -- the very agreement that hid the 2026-09-08 merge.
        (is (= 1 (count production-ids)))
        (is (= 2 (count witness-ids))
            "the tree registers two rows; only a SET comparison says which one is lost")))))
