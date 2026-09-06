(ns clj-surgeon.agent-routing-test
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.agent-routing :as routing]
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
        (is (:ok (routing/check-routing! block-file
                                         [codex-file claude-file]))))
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

(deftest fan-out-route-section-is-required-in-every-block
  (testing "the shipped plate carries every required section byte-exact"
    (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
      (is (seq routing/required-sections))
      (doseq [section routing/required-sections]
        (is (str/includes? plate section) section))))
  (testing "the plate routes only witnessed contracts and says when a route retires"
    (let [plate (slurp "resources/clj-surgeon-agent-routing.md")]
      (is (str/includes? plate "**Strictly better, or native.**"))
      (is (str/includes? plate "**Kill switch.**"))
      (testing "one complete valid call is present for each automatic class"
        (is (str/includes? plate "\"within\": {\"form\": \"handle-event\"}"))
        (is (str/includes? plate "\"op\":\"alias_migration\""))
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
  (testing "a canonical block missing the fan-out route is refused, nothing written"
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
  (testing "check refuses an installed block whose fan-out section drifted"
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
            (is (= claude-file (:target drifted)))))
        (finally (fs/delete-tree tmp))))))
