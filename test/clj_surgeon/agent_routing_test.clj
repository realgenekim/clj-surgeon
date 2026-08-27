(ns clj-surgeon.agent-routing-test
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.agent-routing :as routing]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def canonical-block
  (str routing/managed-begin "\n"
       "## Clojure structural editing\n\n"
       "- Use one compact transaction.\n"
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
    (is (str/includes? source "If work remains"))))
