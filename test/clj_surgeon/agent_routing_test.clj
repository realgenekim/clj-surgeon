(ns clj-surgeon.agent-routing-test
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.agent-routing :as routing]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def canonical-block
  ;; Carries every routing/required-sections line: an installable block that
  ;; dropped one is refused by design (:missing-required-routing-section), so a
  ;; fixture without them would exercise the refusal, not the upsert.
  (str routing/managed-begin "\n"
       "## Clojure structural editing\n\n"
       "- Use one compact transaction.\n"
       (str/join "\n" routing/required-sections) "\n"
       routing/managed-end "\n"))

(defn assert-sol-mutation-refused [old replacement]
  ;; Sol r7 review of d31c3af7: the original valid plate certified all four mutants.
  (let [plate (slurp "resources/clj-surgeon-agent-routing.md")
        mutant (str/replace plate old replacement)
        result (routing/validate-routing-block mutant)]
    (is (:ok (routing/validate-routing-block plate)))
    (is (not= plate mutant) "the mutation must actually change the valid fixture")
    (is (= :missing-required-routing-section (:error-type result)))
    (is (false? (:ok result)))))

;; @spec ROUTING-PARITY-001
(deftest sol-r7-class-count
  (assert-sol-mutation-refused "NO class is routed automatically" "ONE class is routed automatically"))

;; @spec ROUTING-SPLIT-001
(deftest sol-r7-frozen-witness
  (assert-sol-mutation-refused
    "Witness: curtaincall-cfp `d9205abc`, `src/cfp_scheduler_killer/views.clj`:" ""))

;; @spec ROUTING-SPLIT-001
(deftest sol-r7-readmission
  (assert-sol-mutation-refused "A changed mapping/policy/source shape needs new admission." ""))

;; @spec ROUTING-ALIAS-001
;; @spec ROUTING-PARITY-001
(deftest alias-migration-is-frozen-off-main
  (doseq [text ["### Alias migration -- frozen off main"
                "SUSPENDED and frozen off main 2026-11-28"
                "64a \"let's purge it from the code\""
                "75 \"frozen off main is good\""
                "Not routed, not ported to main; the verb and its tests are on `astra/namespace-split` and its archive tag `archive/astra/namespace-split`, never on main."
                "sentinel-verdict.md` on main, pull request 77"
                "was not a correctness loss of the verb"
                "every surviving output matches the golden"
                "the oracle apparatus failed and its records were reaped"]]
    (testing text
      (assert-sol-mutation-refused text ""))))

;; @spec ROUTING-PARITY-001
(deftest registry-controls-every-promise
  (if-let [parse (ns-resolve 'clj-surgeon.agent-routing 'registry-sections)]
    (let [registry (slurp "docs/intent/agent-routing/agent-routing-specs.md")
          plate (slurp "resources/clj-surgeon-agent-routing.md")
          sections (parse registry)]
      (is (= sections routing/required-sections))
      (doseq [section sections]
        (let [result (routing/validate-routing-block (str/replace plate section ""))]
          (is (= :missing-required-routing-section (:error-type result)) section)
          (is (some #{section} (:missing result)) section)))
      (testing "a new registry promise is enforced without editing a code list"
        (let [extended (str/replace registry "```edn routing-requirements\n{"
                         "```edn routing-requirements\n{\"ROUTING-NEW-001\" [\"A new mandatory promise.\"]\n")
              extended (str "- [x] **ROUTING-NEW-001**: New promise.\n" extended)]
          (with-redefs [routing/required-sections (parse extended)]
            (is (= ["A new mandatory promise."]
                   (:missing (routing/validate-routing-block plate)))))))
      (doseq [broken ["" (str registry "\n```edn routing-requirements\n{}\n```\n")
                      (str/replace registry "\"ROUTING-SPLIT-001\"" "\"ROUTING-UNKNOWN-001\"")
                      (str/replace registry "```edn routing-requirements\n{" "```edn routing-requirements\n{broken ")
                      "- [x] **ROUTING-EMPTY-001**: Required.\n```edn routing-requirements\n{\"ROUTING-EMPTY-001\" []}\n```\n"
                      "- [x] **ROUTING-EMPTY-001**: Required.\n```edn routing-requirements\n{\"ROUTING-EMPTY-001\" [\"\"]}\n```\n"]]
        (is (= :invalid-routing-registry
               (try (parse broken) :accepted
                    (catch clojure.lang.ExceptionInfo e (:error-type (ex-data e))))))))
    (is false "requirements must derive from the intent registry")))

;; @spec ROUTING-SPLIT-001
(deftest split-baseline-exception-is-explicit
  (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
    (doseq [needle ["all failed checks refuse."
                    "Nonzero exit is excused ONLY with a `:baseline` map on `captured-reference-analysis`"
                    "or `candidate-lint-delta`. Any other nonzero exit refuses, even `:status \"passed\"`."]]
      (is (str/includes? plate needle) needle))))

;; @spec ROUTING-FANOUT-001
;; @spec ROUTING-SPLIT-001
;; @spec ROUTING-RETIRED-001
;; @spec ROUTING-PARITY-001
(deftest working-tree-skill-matches-plate
  (doseq [path ["skills/clj-surgeon/SKILL.md" "skill.md" ".claude/skills/clj-surgeon/SKILL.md"]]
    (let [skill (slurp path)]
      (is (<= (count (str/split-lines skill)) 70) path)
      (doseq [needle ["FAN-OUT: SUSPENDED" "ALIAS migration" "NAMESPACE SPLIT: exact frozen Cell C"
                      "NO class is routed automatically." "2026-11-28, ruling 71 b"
                      "d9205abc" "141 owners / 20 absent destinations / 87 static sites / five callers"
                      "changed mapping/policy/source shape needs new admission"
                      "Any other nonzero exit refuses"]]
        (is (str/includes? skill needle) (str path ": " needle)))
      (is (nil? (re-find #"inspect_clojure|apply_clojure_changes|edit_clojure|transform_clojure|require_change|terminal_response|next_call|namespace_split|relation_census|feature_thread|alias_migration"
                  skill)) path)
      (is (not (str/includes? skill "Route only\n  when discovery is the cost"))))))

;; @spec ROUTING-PARITY-001
(deftest routing-prep-artifacts-preserve-evidence
  ;; B03b: both trunk receipts must survive before this branch's later receipt.
  (let [rows (mapv edn/read-string (str/split-lines (slurp "docs/observations/battery-ledger.edn")))
        shas (mapv :sha rows)
        receipts ["9d132e7f9a8a5887970adbfbbf5297e48e2c4915"
                  "8795741f07b0991642b28116730f82e25df84b26"
                  "ec64f322499c397de972f0b1cbb8c29342d7f145"]]
    (doseq [sha receipts] (is (= 1 (count (filter #{sha} shas))) sha))
    (is (apply < (map #(.indexOf shas %) receipts))))
  (let [design (slurp "docs/intent/agent-routing/agent-routing-design.md")]
    (is (str/ends-with? design "\n"))
    (is (not (str/ends-with? design "\n\n")))))

(deftest upsert-routing-block-preserves-unmanaged-bytes
  (testing "missing block appends after one blank line"
    (let [result (routing/upsert-routing-block "alpha\n" canonical-block)]
      (is (:ok result))
      (is (= :absent (:previous-state result)))
      (is (:changed result))
      (is (= (str "alpha\n\n" canonical-block) (:source result)))))
  (testing "empty source becomes exactly the canonical block"
    (is (= canonical-block
           (:source (routing/upsert-routing-block "" canonical-block)))))
  (testing "one old block is replaced without changing surrounding bytes"
    (let [source (str "before\n" routing/managed-begin "\nold\n"
                      routing/managed-end "\nafter\n")
          result (routing/upsert-routing-block source canonical-block)]
      (is (= :replaced (:previous-state result)))
      (is (= (str "before\n" canonical-block "after\n")
             (:source result)))))
  (testing "the current block is byte-idempotent"
    (let [source (str "before\n" canonical-block "after\n")
          result (routing/upsert-routing-block source canonical-block)]
      (is (= :current (:previous-state result)))
      (is (false? (:changed result)))
      (is (= source (:source result))))))

(deftest malformed-managed-markers-refuse
  (doseq [[label source]
          [["begin only" (str "x\n" routing/managed-begin "\n")]
           ["end only" (str "x\n" routing/managed-end "\n")]
           ["duplicate begin" (str routing/managed-begin "\n"
                                   routing/managed-begin "\n"
                                   routing/managed-end "\n")]
           ["duplicate end" (str routing/managed-begin "\n"
                                 routing/managed-end "\n"
                                 routing/managed-end "\n")]
           ["reversed" (str routing/managed-end "\n"
                            routing/managed-begin "\n")]]]
    (testing label
      (let [result (routing/upsert-routing-block source canonical-block)]
        (is (false? (:ok result)))
        (is (= :invalid-managed-routing (:error-type result)))
        (is (= source (:source result)))))))

(deftest install-preflights-all-targets-before-writing
  (let [tmp (str (fs/create-temp-dir {:prefix "agent-routing-test"}))
        block-file (str (fs/path tmp "routing.md"))
        codex-file (str (fs/path tmp "codex" "AGENTS.md"))
        claude-file (str (fs/path tmp "claude" "CLAUDE.md"))]
    (try
      (fs/create-dirs (fs/parent block-file))
      (spit block-file canonical-block)
      (fs/create-dirs (fs/parent codex-file))
      (spit codex-file "codex-original\n")
      (fs/create-dirs (fs/parent claude-file))
      (spit claude-file (str "claude-original\n" routing/managed-begin "\n"))
      (let [result (routing/install-routing! block-file
                                             [codex-file claude-file])]
        (is (false? (:ok result)))
        (is (= :invalid-managed-routing (:error-type result)))
        (is (= "codex-original\n" (slurp codex-file)))
        (is (= (str "claude-original\n" routing/managed-begin "\n")
               (slurp claude-file))))
      (finally
        (fs/delete-tree tmp)))))

;; @spec ROUTING-PARITY-001
(deftest install-and-check-routing-end-to-end
  (let [tmp (str (fs/create-temp-dir {:prefix "agent-routing-test"}))
        block-file (str (fs/path tmp "routing.md"))
        codex-file (str (fs/path tmp "codex" "AGENTS.md"))
        claude-file (str (fs/path tmp "claude" "CLAUDE.md"))]
    (try
      (spit block-file canonical-block)
      (fs/create-dirs (fs/parent codex-file))
      (spit codex-file "preserve-codex\n")
      (let [first-result (routing/install-routing! block-file
                                                   [codex-file claude-file])
            first-codex (slurp codex-file)
            first-claude (slurp claude-file)
            second-result (routing/install-routing! block-file
                                                    [codex-file claude-file])]
        (is (:ok first-result))
        (is (= 2 (:changed-count first-result)))
        (is (str/starts-with? first-codex "preserve-codex\n"))
        (is (= canonical-block first-claude))
        (is (:ok second-result))
        (is (zero? (:changed-count second-result)))
        (is (= first-codex (slurp codex-file)))
        (is (= first-claude (slurp claude-file)))
        (let [checked (routing/check-routing! block-file [codex-file claude-file])]
          (is (:ok checked))
          (is (= (:block-hash first-result) (:block-hash checked)))
          (is (= "2b405234" (:doctrine-commit checked)))
          (is (= [{:path codex-file :block-hash (:block-hash checked)}
                  {:path claude-file :block-hash (:block-hash checked)}]
                 (:targets checked))))
        (spit codex-file (str/replace first-codex "compact transaction" "drifted transaction"))
        (is (= :agent-routing-drift
               (:error-type (routing/check-routing! block-file [codex-file])))))
      (finally
        (fs/delete-tree tmp)))))

;; @spec ROUTING-RETIRED-001
;; @spec MCP-OP-RELAY-004
(deftest mcp-surface-is-retired-everywhere
  (let [plate (slurp "resources/clj-surgeon-agent-routing.md")
        retirement "**The MCP surface is retired everywhere (Gene, 2026-11-28, ruling 71 b: \"ready to retire permanently right now\").** No clj-surgeon MCP server is registered or started by any seat; every operation below is the CLI (`clj-surgeon :op ...`). Nothing on main is deleted by this ruling."]
    (is (str/includes? plate retirement))
    (assert-sol-mutation-refused retirement "")
    (doseq [tool-name ["inspect_clojure" "apply_clojure_changes" "edit_clojure"
                       "transform_clojure" "require_change" "terminal_response"
                       "next_call" "namespace_split" "relation_census" "feature_thread"
                       "alias_migration"]]
      (is (not (str/includes? plate tool-name)) tool-name)
      (let [mutant (str/replace plate routing/managed-end
                     (str "Call " tool-name " for supporting reads.\n" routing/managed-end))
            result (routing/validate-routing-block mutant)]
        (is (= {:ok false :error-type :routing-names-mcp-tool :tool-name tool-name}
               result))))))

(deftest doctrine-sections-are-required-in-every-block
  (testing "the shipped plate carries every required section byte-exact"
    (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
      (is (seq routing/required-sections))
      (doseq [section routing/required-sections]
        (is (str/includes? plate section) section))))
  (testing "the plate suspends automatic routing and retains the split port contract"
    (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
      (is (str/includes? plate "**Strictly better, or native.**"))
      (is (str/includes? plate "**Kill switch.**"))
      (testing "fan-out request removed and alias frozen"
        (is (not (str/includes? plate "```json")))
        (is (not (str/includes? plate "\"op\": \"alias_migration\""))))
      (testing "no executable JSON request remains"
        (is (not (str/includes? plate "\"workspace_root\""))))
      (testing "the split port must prove behavior and inspect mutation status"
        (is (str/includes? plate "verification_complete=true"))
        (is (str/includes? plate "a commit alone proves no behavior"))
        (is (str/includes? plate "Read mutation/commit status FIRST")))))
  (testing "the plate names the doctrine commit it derives from"
    (is (re-find #"Derived from doctrine commit [0-9a-f]{8}"
                 (slurp "resources/clj-surgeon-agent-routing.md"))))
  (testing "a canonical block missing the doctrine is refused, nothing written"
    (let [tmp (str (fs/create-temp-dir {:prefix "agent-routing-fanout"}))
          block-file (str (fs/path tmp "routing.md"))
          claude-file (str (fs/path tmp "claude" "CLAUDE.md"))]
      (try
        (spit block-file (str routing/managed-begin "\n"
                              "## Clojure structural editing\n"
                              routing/managed-end "\n"))
        (fs/create-dirs (fs/parent claude-file))
        (spit claude-file "claude-original\n")
        (let [result (routing/install-routing! block-file [claude-file])]
          (is (false? (:ok result)))
          (is (= :missing-required-routing-section (:error-type result)))
          (is (= :canonical (:scope result)))
          (is (= routing/required-sections (:missing result)))
          (is (= "claude-original\n" (slurp claude-file))))
        (finally (fs/delete-tree tmp)))))
  (testing "check refuses an installed block whose suspension drifted"
    (let [tmp (str (fs/create-temp-dir {:prefix "agent-routing-fanout"}))
          block-file (str (fs/path tmp "routing.md"))
          claude-file (str (fs/path tmp "claude" "CLAUDE.md"))
          full-block (str routing/managed-begin "\n"
                          (str/join "\n" routing/required-sections) "\n"
                          routing/managed-end "\n")]
      (try
        (spit block-file full-block)
        (fs/create-dirs (fs/parent claude-file))
        (let [installed (routing/install-routing! block-file [claude-file])
              ok (routing/check-routing! block-file [claude-file])]
          (is (:ok installed))
          (is (:ok ok))
          (spit claude-file (str/replace (slurp claude-file)
                                         (first routing/required-sections)
                                         "## Fan-out route (paraphrased)"))
          (let [drifted (routing/check-routing! block-file [claude-file])]
            (is (false? (:ok drifted)))
            (is (= :missing-required-routing-section (:error-type drifted)))
            (is (= :installed (:scope drifted)))
            (is (= claude-file (:target drifted))))
          (spit claude-file (str (slurp claude-file) (first routing/required-sections)))
          (is (= :missing-required-routing-section
                 (:error-type (routing/check-routing! block-file [claude-file])))
              "doctrine outside the managed block cannot supply missing proof")
          (is (= :installed (:scope (routing/check-routing! block-file [claude-file])))))
        (finally (fs/delete-tree tmp))))))

  ;; @spec ROUTING-FANOUT-001
(deftest informed-fanout-is-suspended
  (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
    (is (str/includes? plate "SUSPENDED 2026-09-08 (pair-1: tool 0.68×/0.59× native at 3 and 21 sites, equal acceptance; retest only as a whole-intent redesign)"))
    (is (str/includes? plate "Capability example only; no automatic fan-out route."))
    (is (not (str/includes? plate "## Fan-out route (experimental default")))
    (is (str/includes? plate "0.68×/0.59× are native/tool wall ratios"))
    (is (not (str/includes? plate "```json")))))

  ;; @spec ROUTING-SPLIT-001
(deftest split-route-is-exact-contract-only
  (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
    (doseq [text ["### Namespace split -- EXACT witnessed contract only"
                  "Only the frozen Cell C source shape AND manifest qualify"
                  "141 named owners / 20 absent destinations / 87 static sites / five caller files"
                  "`promotion_policy=promote-required`, `source_retirement=delete`"
                  "`roots=[src,test]` and a named cold `verification.profile`"
                  "clj-surgeon :op :split-ns! :request-file X"
                  "One call, once the port lands: `clj-surgeon :op :split-ns! :request-file X`."
                  "NO class is routed automatically. Namespace split is being ported to the CLI"
                  "and is not routed until that port lands; alias migration is frozen off main (below)."
                  "`state=committed`, `verification_complete=true`, `proof_pending=[]`"
                  "every required profile check present with `:exit 0`"
                  "`papercuts` must pass when the profile carries that oracle"
                  "plan-only first when the mapping is uncertain"
                  "repair once from the refusal's named next step, then native"
                  "Never re-run a committed split"
                  "wall loss vs the registered controls suspends this route"
                  "dynamic references, open-ended decomposition, other source grammars"
                  "A changed mapping/policy/source shape needs new admission"]]
      (is (str/includes? plate text) text))))

  ;; @spec ROUTING-PARITY-001
(deftest plate-only-check-is-explicit-and-read-only
  (let [read-file slurp
        reads (atom [])
        output (with-redefs [clojure.core/slurp
                             (fn [path]
                               (swap! reads conj path)
                               (read-file path))
                             clojure.core/spit
                             (fn [& _] (throw (ex-info "check must not write" {})))]
                 (with-out-str
                   (routing/-main "check" "resources/clj-surgeon-agent-routing.md" "--plate-only")))
        result (edn/read-string output)]
    (is (= ["resources/clj-surgeon-agent-routing.md"] @reads))
    (is (:ok result))
    (is (= :plate-only (:scope result)))
    (is (= 0 (:target-count result)))
    (is (= "2b405234" (:doctrine-commit result)))
    (is (re-matches #"[0-9a-f]{64}" (or (:block-hash result) "")))))

  ;; @spec ROUTING-PARITY-001
(deftest canonical-routing-refusals
  (if-let [validate (ns-resolve 'clj-surgeon.agent-routing 'validate-routing-block)]
    (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
      (is (:ok (validate plate)))
      (doseq [section routing/required-sections]
        (is (false? (:ok (validate (str/replace plate section "")))) section))
      (doseq [broken [(str/replace plate routing/managed-begin "")
                      (str/replace plate routing/managed-end "")
                      (str routing/managed-begin "\n" plate)
                      (str "outside\n" plate)]]
        (is (= :invalid-canonical-routing (:error-type (validate broken)))))
      (doseq [extra [(apply str (repeat 189 "\n")) (apply str (repeat 10739 "x"))]]
        (is (= :routing-budget-exceeded
               (:error-type (validate (str/replace plate routing/managed-end
                                        (str extra routing/managed-end))))))))
    (is false "plate-only must validate canonical markers, doctrine and budget"))
  (is (= :missing-routing-targets
         (:error-type (routing/check-routing! "resources/clj-surgeon-agent-routing.md" [])))))
