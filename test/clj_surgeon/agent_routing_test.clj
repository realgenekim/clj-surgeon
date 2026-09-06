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

(deftest stale-version-block-is-refused-as-stale-and-replaced
  ;; The installed v:1 block is a superseded RULE, not merely different bytes.
  ;; It must be located, reported by version, and replaced in place -- never
  ;; left behind while a second block is appended.
  (let [v1-block (str "<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->\n"
                      "Native `rg` plus a native patch is the default route.\n"
                      "<!-- END CLJ-SURGEON ROUTING v:1 -->\n")
        source (str "before\n" v1-block "after\n")
        result (routing/upsert-routing-block source canonical-block)]
    (testing "the current intent version is 2"
      (is (= 2 routing/managed-version))
      (is (str/includes? routing/managed-begin "v:2")))
    (testing "a v:1 block is stale, not absent"
      (is (:ok result))
      (is (= :stale (:previous-state result)))
      (is (= 1 (:stale-version result)))
      (is (:changed result)))
    (testing "the stale block is replaced in place, leaving no v:1 bytes"
      (is (= (str "before\n" canonical-block "after\n") (:source result)))
      (is (not (str/includes? (:source result) "ROUTING v:1"))))))

(deftest check-fails-on-an-installed-stale-version
  (let [tmp (str (fs/create-temp-dir {:prefix "agent-routing-test"}))
        block-file (str (fs/path tmp "routing.md"))
        target (str (fs/path tmp "CLAUDE.md"))]
    (try
      (spit block-file canonical-block)
      (spit target (str "seat header\n"
                        "<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->\n"
                        "old rule\n"
                        "<!-- END CLJ-SURGEON ROUTING v:1 -->\n"))
      (let [result (routing/check-routing! block-file [target])]
        (is (false? (:ok result)))
        (is (= :agent-routing-stale-version (:error-type result)))
        (is (= 2 (:expected-version result)))
        (is (= [{:path target :previous-state :stale :changed true
                 :stale-version 1}]
               (:targets result))))
      (finally
        (fs/delete-tree tmp)))))

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

(deftest installed-plate-is-the-current-version-and-points-at-the-skill
  ;; The plate is a POINTER now. If it stops naming the skill, seats read a
  ;; compact rule with no canonical text behind it.
  (let [source (slurp "resources/clj-surgeon-agent-routing.md")]
    (is (str/starts-with? source routing/managed-begin))
    (is (str/ends-with? (str/trimr source) routing/managed-end))
    (is (not (str/includes? source "ROUTING v:1")))
    (is (str/includes? source "the `clj-surgeon` skill, section \"Edit routing\""))
    (is (str/includes? source "bin/mission"))
    (is (str/includes? source "PROTOTYPE"))
    (is (not (str/includes? source "Native `rg` plus a native patch is the default route")))
    (testing "the block stays compact enough to sit in every seat header"
      (is (<= (count (str/split-lines source)) 25)))
    (testing "the block names the document it was derived from"
      (is (str/includes?
            source
            "docs/observations/2026-09-06-clojure-edit-routing-rule.md")))))

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
