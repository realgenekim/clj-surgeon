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
          (is (= "3c2eb6a9" (:doctrine-commit checked)))
          (is (= [{:path codex-file :block-hash (:block-hash checked)}
                  {:path claude-file :block-hash (:block-hash checked)}]
                 (:targets checked))))
        (spit codex-file (str/replace first-codex "compact transaction" "drifted transaction"))
        (is (= :agent-routing-drift
               (:error-type (routing/check-routing! block-file [codex-file])))))
      (finally
        (fs/delete-tree tmp)))))

(deftest terminal-response-routing-is-conditional-on-complete-user-work
  ;; @spec MCP-OP-RELAY-004
  (let [source (slurp "resources/clj-surgeon-agent-routing.md")]
    (is (str/includes? source "If `terminal_response` is present"))
    (is (re-find #"completes all remaining\s+user-requested work" source))
    (is (str/includes? source "return its value exactly"))
    (is (str/includes? source "If work remains"))
    (is (re-find
          #"They never prove\s+that the complete user request is finished\."
          source))))

(deftest doctrine-sections-are-required-in-every-block
  (testing "the shipped plate carries every required section byte-exact"
    (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
      (is (seq routing/required-sections))
      (doseq [section routing/required-sections]
        (is (str/includes? plate section) section))))
  (testing "the plate routes only witnessed contracts and says when a route retires"
    (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
      (is (str/includes? plate "**Strictly better, or native.**"))
      (is (str/includes? plate "**Kill switch.**"))
      (testing "retained capability, alias route and optional supporting read"
        (is (str/includes? plate "\"within\": {\"form\": \"add\"}"))
        (is (str/includes? plate "\"op\": \"alias_migration\", \"workspace_root\""))
        (testing "and for the optional supporting read, with expect at the ROOT"
          (is (str/includes? plate "\"expect\": {\"requests\": 3, \"files\": 3}"))))
      (testing "every published example carries workspace_root"
        (is (= 3 (count (re-seq #"\"workspace_root\"" plate)))))
      (testing "the receipt scope tests a VALUE, not a field name"
        (is (str/includes? plate "verification_complete=true"))
        (is (str/includes? plate "prove the WRITE, not task")))
      (testing "the escape rule reads commit status before falling back"
        (is (str/includes? plate "a refusal does not imply that nothing was written")))))
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
    (is (str/includes? plate "\"within\": {\"form\": \"add\"}"))))

  ;; @spec ROUTING-SPLIT-001
(deftest split-route-is-exact-contract-only
  (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
    (doseq [text ["### Namespace split -- EXACT witnessed contract only"
                  "Only the frozen Cell C source shape AND manifest qualify"
                  "141 named owners / 20 absent destinations / 87 static sites / five caller files"
                  "`promotion_policy=promote-required`, `source_retirement=delete`"
                  "`roots=[src,test]` and a named cold `verification.profile`"
                  "clj-surgeon :op :split-ns! :request-file X"
                  "MCP `namespace_split`"
                  "`state=committed`, `verification_complete=true`, `proof_pending=[]`"
                  "every required profile check present with `:exit 0`"
                  "`papercuts` must pass when the profile carries that oracle"
                  "plan-only first when the mapping is uncertain"
                  "repair once from `next_call`, then native"
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
    (is (= "3c2eb6a9" (:doctrine-commit result)))
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
