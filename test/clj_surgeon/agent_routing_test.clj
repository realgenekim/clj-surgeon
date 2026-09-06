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
    (is (str/includes? source "the `clj-surgeon` skill, section \"Edit routing (policy revision 1, 2026-09-06)\""))
    (is (str/includes? source "bin/mission"))
    (is (str/includes? source "PROTOTYPE"))
    (is (not (str/includes? source "Native `rg` plus a native patch is the default route")))
    (testing "the block stays compact enough to sit in every seat header"
      (is (<= (count (str/split-lines source)) 25)))
    (testing "the block names the document it was derived from"
      (is (str/includes?
            source
            "docs/observations/2026-09-06-routing-prompt-surfaces.md")))))

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

;; ---------------------------------------------------------------------------
;; Astra's three probes, 2026-09-06 03:33Z, verbatim as witnesses.
;;
;; Executed against c1d6028a they returned: (a) :current with :changed false,
;; leaving a contradictory v:1 rule installed beside the v:2 one; (b) :absent,
;; so a THIRD block was appended next to two v:1 blocks; (c) accepted as stale
;; and replaced, trusting a BEGIN v:1 / END v:3 pair to bound the region.
;; Marker state must require exactly one well-formed matching pair across ALL
;; versions and refuse everything else without touching the file.

(defn- v-block [version body]
  (str "<!-- BEGIN CLJ-SURGEON ROUTING v:" version " -->\n"
       body "\n"
       "<!-- END CLJ-SURGEON ROUTING v:" version " -->\n"))

(deftest astra-probe-a-current-block-beside-an-old-block-refuses
  (let [source (str "seat header\n"
                    (v-block 2 "the current rule")
                    "\n"
                    (v-block 1 "Native `rg` plus a native patch is the default route.")
                    "tail\n")
        result (routing/upsert-routing-block source canonical-block)]
    (testing "not :current, not :changed false -- a typed refusal"
      (is (false? (:ok result)))
      (is (= :invalid-managed-routing (:error-type result)))
      (is (nil? (:previous-state result)))
      (is (not= :current (:previous-state result))))
    (testing "the diagnosis names both versions it found"
      (is (= [2 1] (:begin-versions result)))
      (is (= [2 1] (:end-versions result)))
      (is (str/includes? (:diagnosis result) "exactly one")))
    (testing "the file is NOT modified"
      (is (= source (:source result))))))

(deftest astra-probe-b-two-old-blocks-are-not-absent
  (let [source (str "seat header\n"
                    (v-block 1 "first old rule")
                    "\n"
                    (v-block 1 "second old rule")
                    "tail\n")
        result (routing/upsert-routing-block source canonical-block)]
    (testing "not :absent -- refused, so no third block is appended"
      (is (false? (:ok result)))
      (is (= :invalid-managed-routing (:error-type result)))
      (is (= 2 (:begin-count result)))
      (is (= 2 (:end-count result))))
    (testing "the file is NOT modified"
      (is (= source (:source result)))
      (is (not (str/includes? (:source result) "ROUTING v:2"))))))

(deftest astra-probe-c-mismatched-pair-versions-refuse
  (let [source (str "seat header\n"
                    "<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->\n"
                    "a rule bounded by markers that disagree\n"
                    "<!-- END CLJ-SURGEON ROUTING v:3 -->\n"
                    "tail\n")
        result (routing/upsert-routing-block source canonical-block)]
    (testing "not accepted as stale"
      (is (false? (:ok result)))
      (is (= :invalid-managed-routing (:error-type result)))
      (is (not= :stale (:previous-state result)))
      (is (nil? (:stale-version result))))
    (testing "the diagnosis names both marker versions"
      (is (= [1] (:begin-versions result)))
      (is (= [3] (:end-versions result)))
      (is (str/includes? (:diagnosis result) "BEGIN is v:1"))
      (is (str/includes? (:diagnosis result) "END is v:3")))
    (testing "the file is NOT modified"
      (is (= source (:source result))))))

(deftest a-lone-well-formed-pair-is-still-handled
  (testing "a lone stale pair is replaced in place"
    (let [source (str "head\n" (v-block 1 "old rule") "tail\n")
          result (routing/upsert-routing-block source canonical-block)]
      (is (:ok result))
      (is (= :stale (:previous-state result)))
      (is (= 1 (:stale-version result)))
      (is (= (str "head\n" canonical-block "tail\n") (:source result)))))
  (testing "a lone current pair is :current and byte-idempotent"
    (let [source (str "head\n" canonical-block "tail\n")
          result (routing/upsert-routing-block source canonical-block)]
      (is (:ok result))
      (is (= :current (:previous-state result)))
      (is (false? (:changed result)))
      (is (= source (:source result))))))

(deftest no-old-version-bytes-survive-a-replacement
  ;; A replacement that appended instead of replacing would keep the old rule
  ;; readable in the same file. Assert on the BYTES, at every older version.
  (doseq [old-version [1 3 11]]
    (testing (str "v:" old-version " leaves nothing behind")
      (let [marker-body (str "SUPERSEDED-RULE-" old-version)
            source (str "head\n" (v-block old-version marker-body) "tail\n")
            result (routing/upsert-routing-block source canonical-block)
            updated (:source result)]
        (is (:ok result))
        (is (= :stale (:previous-state result)))
        (is (= old-version (:stale-version result)))
        (is (not (str/includes? updated marker-body)))
        (is (not (str/includes? updated (str "ROUTING v:" old-version))))
        (is (= 1 (count (re-seq #"BEGIN CLJ-SURGEON ROUTING" updated))))
        (is (= 1 (count (re-seq #"END CLJ-SURGEON ROUTING" updated))))
        (is (= (str "head\n" canonical-block "tail\n") updated))))))

(deftest the-plate-pointer-and-its-citations-resolve
  ;; A pointer that names a heading nobody wrote, or a document nobody
  ;; committed, is worse than no pointer: the seat reads a compact rule and
  ;; believes fuller text exists behind it. The plate cited
  ;; docs/observations/2026-09-06-clojure-edit-routing-rule.md, which was never
  ;; written. `bb bin/check-routing-parity.clj` is the same guard over all the
  ;; hand-copied table renderings.
  (let [plate (slurp "resources/clj-surgeon-agent-routing.md")
        canonical (slurp "skills/clj-surgeon/SKILL.md")
        heading "## Edit routing (policy revision 1, 2026-09-06)"]
    (testing "the plate names the canonical heading and that heading exists"
      (is (str/includes? plate "Edit routing (policy revision 1, 2026-09-06)"))
      (is (str/includes? canonical heading)))
    (testing "every document the plate cites exists on disk"
      (let [cited (set (re-seq #"docs/observations/[A-Za-z0-9._-]+\.md" plate))]
        (is (seq cited))
        (doseq [doc cited]
          (is (fs/exists? doc) (str "the plate cites a missing document: " doc)))))
    (testing "the plate carries no executor-first rule for production"
      (is (str/includes? plate "There is no executor-first rule in production"))
      (is (str/includes? plate "EXPERIMENT ONLY")))
    (testing "the plate names the real refusal shapes, not only mission-*"
      (is (str/includes? plate ":forms-protected-syntax"))
      (is (str/includes? plate "mission-workspace-required"))
      (is (str/includes? plate "NESTED diagnostics")))
    (testing "no unsourced 11x figure survives on any surface"
      (is (not (str/includes? plate "11x")))
      (is (not (str/includes? canonical "| Bench harness wall | 11x |"))))))

;; ---------------------------------------------------------------------------
;; Sol fence r4, finding 1: the marker contract must FAIL CLOSED on a version
;; it cannot parse. Before this, `marker-scan` matched only `v:\d+`, so a line
;; a human obviously meant as a managed marker -- `<!-- BEGIN CLJ-SURGEON
;; ROUTING v:x -->` -- was invisible: the file read `:absent` and the installer
;; APPENDED a second block beside a region it could not bound, preserving the
;; malformed one. `:absent` is the one state a malformed marker may never
;; produce.

(defn- malformed-refusal?
  [result]
  (and (false? (:ok result))
       (= :invalid-managed-routing (:error-type result))
       (string? (:diagnosis result))
       (seq (:malformed-markers result))))

(deftest a-marker-whose-version-is-not-a-positive-integer-refuses
  (doseq [[label source]
          [["v:x on both markers"
            (str "head\n<!-- BEGIN CLJ-SURGEON ROUTING v:x -->\nMALFORMED-BODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v:x -->\ntail\n")]
           ["a BEGIN with no version at all"
            "head\n<!-- BEGIN CLJ-SURGEON ROUTING -->\nMALFORMED-BODY\ntail\n"]
           ["a well-formed BEGIN with a malformed END"
            (str "head\n" routing/managed-begin "\nBODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v:2a -->\ntail\n")]
           ["an empty version"
            (str "head\n<!-- BEGIN CLJ-SURGEON ROUTING v: -->\nBODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v: -->\ntail\n")]
           ["a zero version is not positive"
            (str "head\n<!-- BEGIN CLJ-SURGEON ROUTING v:0 -->\nBODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v:0 -->\ntail\n")]]]
    (testing label
      (let [result (routing/upsert-routing-block source canonical-block)]
        (is (malformed-refusal? result)
            (str label " -- expected :invalid-managed-routing, got "
                 (pr-str (dissoc result :source))))
        (testing "the file's bytes are untouched"
          (is (= source (:source result))))
        (testing "it is never reported absent"
          (is (not= :absent (:previous-state result))))))))

(deftest install-refuses-a-malformed-marker-file-and-appends-nothing
  ;; The end-to-end half: `install` is what actually wrote the second block in
  ;; Sol's reproduction, so the witness drives install!, not just the pure fn.
  (let [dir (fs/create-temp-dir {:prefix "clj-surgeon-routing-malformed"})
        block-file (str (fs/path dir "block.md"))
        target (str (fs/path dir "target.md"))
        original (str "keep me\n<!-- BEGIN CLJ-SURGEON ROUTING v:x -->\n"
                      "MALFORMED-BODY\n<!-- END CLJ-SURGEON ROUTING v:x -->\n")]
    (try
      (spit block-file canonical-block)
      (spit target original)
      (let [result (routing/install-routing! block-file [target])
            after (slurp target)]
        (is (false? (:ok result)))
        (is (= :invalid-managed-routing (:error-type result)))
        (is (= :install-agent-routing (:operation result)))
        (is (= target (:target result)))
        (testing "not one byte was appended"
          (is (= original after))
          (is (= 1 (count (re-seq #"BEGIN CLJ-SURGEON ROUTING" after))))
          (is (not (str/includes? after (str "ROUTING v:" routing/managed-version))))
          (is (str/includes? after "MALFORMED-BODY"))))
      (finally (fs/delete-tree dir)))))

(deftest a-well-formed-file-still-reads-present-stale-and-absent
  ;; The fail-closed rule must not swallow the states it exists to protect.
  (testing "absent stays absent when no marker prefix appears at all"
    (is (= :absent (:previous-state (routing/upsert-routing-block "plain\n"
                                                                 canonical-block)))))
  (testing "the canonical block still reads present/current"
    (is (= :current (:previous-state (routing/upsert-routing-block canonical-block
                                                                  canonical-block)))))
  (testing "a lone v:1 pair still reads stale and is replaced in place"
    (let [source (str "head\n<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->\nOLD-BYTES\n"
                      "<!-- END CLJ-SURGEON ROUTING v:1 -->\ntail\n")
          result (routing/upsert-routing-block source canonical-block)]
      (is (:ok result))
      (is (= :stale (:previous-state result)))
      (is (= 1 (:stale-version result)))
      (is (not (str/includes? (:source result) "OLD-BYTES"))))))

;; ---------------------------------------------------------------------------
;; Sol fence r4, finding 5: a target list is AUTHORITY. The default Make
;; invocation named only the two global instruction files, but both the Make
;; overrides and the installer CLI accepted arbitrary destinations, and Sol's
;; scratch installation outside those paths succeeded. Confinement lives at the
;; CLI boundary (`confine-targets`), so the Make recipe is unchanged and an
;; override that names another path is refused at runtime by the tool itself.

(def ^:private codex-global
  (str (fs/path (System/getProperty "user.home") ".codex" "AGENTS.md")))
(def ^:private claude-global
  (str (fs/path (System/getProperty "user.home") ".claude" "CLAUDE.md")))

(deftest only-the-two-global-files-are-installable-without-scratch
  (testing "the two real targets are accepted"
    (is (:ok (routing/confine-targets [codex-global claude-global] false))))
  (doseq [bad ["/var/tmp/forge/x.md"
               "/home/forge/other.md"
               "/etc/passwd"
               (str codex-global "/../../../etc/passwd")]]
    (testing (str bad " is refused")
      (let [r (routing/confine-targets [bad] false)]
        (is (false? (:ok r)))
        (is (= :agent-routing-target-refused (:error-type r)))
        (is (= [bad] (mapv :path (:refused r)))
            "the refusal names the path the caller typed")
        (is (string? (:diagnosis r)))))))

(deftest scratch-confines-to-var-tmp-forge-and-nowhere-else
  (testing "a scratch target under /var/tmp/forge is accepted"
    (is (:ok (routing/confine-targets ["/var/tmp/forge/routing-fixture.md"] true))))
  (testing "--scratch does not unlock the home directory"
    (let [r (routing/confine-targets ["/home/forge/other.md"] true)]
      (is (false? (:ok r)))
      (is (= :agent-routing-target-refused (:error-type r)))))
  (testing "--scratch does not unlock the two global files either"
    (is (false? (:ok (routing/confine-targets [codex-global] true)))))
  (testing "a traversal out of the scratch root is refused by CANONICAL path"
    (let [r (routing/confine-targets ["/var/tmp/forge/../../home/forge/other.md"] true)]
      (is (false? (:ok r)))
      (is (= ["/var/home/forge/other.md"] (mapv :canonical-path (:refused r)))
          "string comparison would have let this through"))))

(deftest one-refused-target-refuses-the-whole-install
  ;; All-or-nothing: the installer's preflight is already all-or-nothing, and a
  ;; confinement that let the legal half through would write half an install.
  (let [r (routing/confine-targets [codex-global "/var/tmp/forge/x.md"] false)]
    (is (false? (:ok r)))
    (is (= ["/var/tmp/forge/x.md"] (mapv :path (:refused r))))))

(deftest an-unparseable-oversized-version-refuses-and-never-compares-nils
  ;; Sol fence r2, gap 1: `parse-long` returns nil for a version too large for a
  ;; Long, so TWO DIFFERENT oversized versions compared equal (nil = nil), the
  ;; pair read as a matching stale pair, and the installer REWROTE the file and
  ;; lost the version. A version the installer cannot represent is malformed.
  (doseq [[label source]
          [["two DIFFERENT oversized versions must not compare equal as nil"
            (str "head\n"
                 "<!-- BEGIN CLJ-SURGEON ROUTING v:999999999999999999999999999998 -->\n"
                 "BODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v:999999999999999999999999999999 -->\n"
                 "tail\n")]
           ["a single oversized version on a matching pair"
            (str "head\n"
                 "<!-- BEGIN CLJ-SURGEON ROUTING v:99999999999999999999 -->\n"
                 "BODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v:99999999999999999999 -->\n"
                 "tail\n")]
           ["a version of exactly ten digits is over the accepted ceiling"
            (str "head\n<!-- BEGIN CLJ-SURGEON ROUTING v:1000000000 -->\nBODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v:1000000000 -->\ntail\n")]
           ["version 0 is not positive"
            (str "head\n<!-- BEGIN CLJ-SURGEON ROUTING v:0 -->\nBODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v:0 -->\ntail\n")]
           ["version 01 has a leading zero"
            (str "head\n<!-- BEGIN CLJ-SURGEON ROUTING v:01 -->\nBODY\n"
                 "<!-- END CLJ-SURGEON ROUTING v:01 -->\ntail\n")]]]
    (testing label
      (let [result (routing/upsert-routing-block source canonical-block)]
        (is (malformed-refusal? result)
            (str label " -- expected :invalid-managed-routing, got "
                 (pr-str (dissoc result :source))))
        (testing "it is never reported stale"
          (is (not= :stale (:previous-state result)))
          (is (nil? (:stale-version result))))
        (testing "the file's bytes are untouched"
          (is (= source (:source result))))))))

(deftest trailing-bytes-after-the-marker-arrow-refuse
  ;; Sol fence r2, gap 2: `.lookingAt` accepted anything after `-->`, so a
  ;; marker line that is NOT exactly the managed marker still bounded a managed
  ;; region and the installer rewrote it. The marker line must match EXACTLY.
  (doseq [[label source]
          [["a trailing word"
            (str "head\n" routing/managed-begin "TRAILING-BYTES\nBODY\n"
                 routing/managed-end "\ntail\n")]
           ["a trailing space"
            (str "head\n" routing/managed-begin " \nBODY\n"
                 routing/managed-end "\ntail\n")]
           ["a trailing x on the END marker"
            (str "head\n" routing/managed-begin "\nBODY\n"
                 routing/managed-end "x\ntail\n")]
           ["a trailing carriage return"
            (str "head\n" routing/managed-begin "\r\nBODY\n"
                 routing/managed-end "\r\ntail\n")]]]
    (testing label
      (let [result (routing/upsert-routing-block source canonical-block)]
        (is (malformed-refusal? result)
            (str label " -- expected :invalid-managed-routing, got "
                 (pr-str (dissoc result :source))))
        (testing "the file's bytes are untouched"
          (is (= source (:source result))))
        (testing "it is never reported absent, current or replaced"
          (is (nil? (:previous-state result))))))))

(deftest install-refuses-an-oversized-version-and-writes-nothing
  (let [dir (fs/create-temp-dir {:prefix "clj-surgeon-routing-oversized"})
        block-file (str (fs/path dir "block.md"))
        target (str (fs/path dir "target.md"))
        original (str "keep me\n"
                      "<!-- BEGIN CLJ-SURGEON ROUTING v:999999999999999999999999999998 -->\n"
                      "OLD-BODY\n"
                      "<!-- END CLJ-SURGEON ROUTING v:999999999999999999999999999999 -->\n")]
    (try
      (spit block-file canonical-block)
      (spit target original)
      (let [result (routing/install-routing! block-file [target])]
        (is (false? (:ok result)))
        (is (= :invalid-managed-routing (:error-type result)))
        (testing "the target is byte-identical"
          (is (= original (slurp target)))))
      (finally (fs/delete-tree dir)))))

(deftest install-refuses-trailing-marker-bytes-and-writes-nothing
  (let [dir (fs/create-temp-dir {:prefix "clj-surgeon-routing-trailing"})
        block-file (str (fs/path dir "block.md"))
        target (str (fs/path dir "target.md"))
        original (str "keep me\n" routing/managed-begin "TRAILING-BYTES\n"
                      "OLD-BODY\n" routing/managed-end "\n")]
    (try
      (spit block-file canonical-block)
      (spit target original)
      (let [result (routing/install-routing! block-file [target])]
        (is (false? (:ok result)))
        (is (= :invalid-managed-routing (:error-type result)))
        (testing "the target is byte-identical"
          (is (= original (slurp target)))))
      (finally (fs/delete-tree dir)))))

;; ---------------------------------------------------------------------------
;; Sol fence r3: the existing witnesses cover TRAILING bytes but not LEADING
;; ones. `.indexOf` finds the prefix anywhere on a line, and the matcher's
;; region began at that hit -- so `  <!-- BEGIN CLJ-SURGEON ROUTING v:2 -->`,
;; `> <!-- BEGIN ... -->` in a quoted block, or a BOM before the first marker
;; line all matched as WELL FORMED and bounded a managed region the installer
;; then rewrote. A marker is well formed only if it begins at COLUMN 0 of its
;; own line: idx == line-start. Any leading text or whitespace is MALFORMED --
;; a refusal, never `:absent` and never a stale rewrite.

(deftest leading-bytes-before-the-marker-refuse
  (doseq [[label source]
          [["a leading space before BEGIN"
            (str "head\n " routing/managed-begin "\nBODY\n"
                 routing/managed-end "\ntail\n")]
           ["a leading tab before BEGIN"
            (str "head\n\t" routing/managed-begin "\nBODY\n"
                 routing/managed-end "\ntail\n")]
           ["leading text on the marker line"
            (str "head\nx" routing/managed-begin "\nBODY\n"
                 routing/managed-end "\ntail\n")]
           ["a markdown quote prefix"
            (str "head\n> " routing/managed-begin "\n> BODY\n> "
                 routing/managed-end "\ntail\n")]
           ["a BOM before the first marker line"
            (str "﻿" routing/managed-begin "\nBODY\n"
                 routing/managed-end "\n")]
           ["a leading space on the END marker only"
            (str "head\n" routing/managed-begin "\nBODY\n "
                 routing/managed-end "\ntail\n")]
           ["a leading space on a STALE pair is a refusal, not a rewrite"
            (str "head\n <!-- BEGIN CLJ-SURGEON ROUTING v:1 -->\nOLD-BYTES\n "
                 "<!-- END CLJ-SURGEON ROUTING v:1 -->\ntail\n")]]]
    (testing label
      (let [result (routing/upsert-routing-block source canonical-block)]
        (is (malformed-refusal? result)
            (str label " -- expected :invalid-managed-routing, got "
                 (pr-str (dissoc result :source))))
        (testing "the file's bytes are untouched"
          (is (= source (:source result))))
        (testing "it is never absent, current, replaced or stale"
          (is (nil? (:previous-state result)))
          (is (nil? (:stale-version result))))))))

(deftest a-marker-at-column-zero-is-still-well-formed
  ;; The fail-closed rule must not swallow the two states it exists to protect.
  (testing "the current pair still reads current and rewrites nothing"
    (let [source (str "before\n" canonical-block "after\n")
          result (routing/upsert-routing-block source canonical-block)]
      (is (:ok result))
      (is (= :current (:previous-state result)))
      (is (false? (:changed result)))
      (is (= source (:source result)))))
  (testing "a lone valid stale pair still reads stale and is replaced in place"
    (let [source (str "head\n<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->\nOLD-BYTES\n"
                      "<!-- END CLJ-SURGEON ROUTING v:1 -->\ntail\n")
          result (routing/upsert-routing-block source canonical-block)]
      (is (:ok result))
      (is (= :stale (:previous-state result)))
      (is (= 1 (:stale-version result)))
      (is (not (str/includes? (:source result) "OLD-BYTES")))))
  (testing "a marker at the very first byte of the file is well formed"
    (is (= :current (:previous-state
                     (routing/upsert-routing-block canonical-block
                                                   canonical-block))))))

(deftest install-refuses-a-leading-space-marker-and-writes-nothing
  (let [dir (fs/create-temp-dir {:prefix "clj-surgeon-routing-leading"})
        block-file (str (fs/path dir "block.md"))
        target (str (fs/path dir "target.md"))
        original (str "keep me\n " routing/managed-begin "\n"
                      "OLD-BODY\n " routing/managed-end "\n")]
    (try
      (spit block-file canonical-block)
      (spit target original)
      (let [result (routing/install-routing! block-file [target])
            after (slurp target)]
        (is (false? (:ok result)))
        (is (= :invalid-managed-routing (:error-type result)))
        (is (= :install-agent-routing (:operation result)))
        (is (= target (:target result)))
        (testing "not one byte was written"
          (is (= original after))
          (is (= 1 (count (re-seq #"BEGIN CLJ-SURGEON ROUTING" after))))
          (is (str/includes? after "OLD-BODY"))))
      (finally (fs/delete-tree dir)))))

;; ---------------------------------------------------------------------------
;; Sol fence r4, BLOCKING finding: `confine-targets` canonicalized BOTH the
;; allowed path and the requested target. When `$HOME/.codex` is a symlink to a
;; directory outside the home, the two canonicalize to the SAME outside path,
;; the membership test passes, and the installer writes outside the home --
;; Sol's reproduction wrote `<outside>/AGENTS.md` with exit 0. Confinement by
;; canonical path alone is not confinement; it is a comparison of two escapes.
;;
;; The authorized targets are LEXICAL paths under the canonical home, and every
;; component from the home down to the target must be a real directory.

(defn- with-home
  "Run `f` with `user.home` pointed at `home`, restoring the real value after.
   The installer derives its entire authority from this property, so a witness
   that cannot move it cannot see the escape."
  [home f]
  (let [previous (System/getProperty "user.home")]
    (try
      (System/setProperty "user.home" (str home))
      (f)
      (finally (System/setProperty "user.home" previous)))))

(defn- redteam-home
  "A scratch home under /var/tmp/forge plus an `outside` directory next to it.
   Returns [home outside]."
  [label]
  (let [root (fs/create-temp-dir {:dir "/var/tmp/forge" :prefix (str "routing-" label "-")})
        home (fs/create-dirs (fs/path root "home"))
        outside (fs/create-dirs (fs/path root "outside"))]
    [home outside]))

(deftest a-symlinked-dot-codex-pointing-outside-the-home-is-refused
  (let [[home outside] (redteam-home "symlink-escape")]
    (try
      (fs/create-sym-link (fs/path home ".codex") outside)
      (with-home home
        (fn []
          (let [target (str (fs/path home ".codex" "AGENTS.md"))
                r (routing/confine-targets [target] false)]
            (is (false? (:ok r))
                "canonicalizing both sides made this escape look authorized")
            (is (= :agent-routing-target-refused (:error-type r)))
            (is (= [:symlinked-component] (mapv :reason (:refused r))))
            (is (= [(str (fs/path home ".codex"))]
                   (mapv :component (:refused r)))
                "the refusal names the offending component")
            (is (str/includes? (:diagnosis r) "must be a real directory")))))
      (is (empty? (fs/list-dir outside))
          "nothing was written outside the home")
      (finally (fs/delete-tree (fs/parent home))))))

(deftest a-symlinked-agents-md-inside-a-real-dot-codex-is-refused
  (let [[home outside] (redteam-home "final-symlink")
        escape (fs/path outside "AGENTS.md")]
    (try
      (fs/create-dirs (fs/path home ".codex"))
      (spit (fs/file escape) "untouched\n")
      (fs/create-sym-link (fs/path home ".codex" "AGENTS.md") escape)
      (with-home home
        (fn []
          (let [r (routing/confine-targets [(str (fs/path home ".codex" "AGENTS.md"))] false)]
            (is (false? (:ok r)))
            (is (= [:symlinked-component] (mapv :reason (:refused r)))))))
      (is (= "untouched\n" (slurp (fs/file escape))))
      (finally (fs/delete-tree (fs/parent home))))))

(deftest a-symlinked-component-is-refused-even-when-it-points-inside-the-home
  ;; The rule is "no symlinked component", stated plainly -- not "no component
  ;; that points somewhere we dislike". A rule about where a link POINTS has to
  ;; be re-derived at every call and gets one case wrong; a rule about what a
  ;; component IS does not.
  (let [[home _outside] (redteam-home "inside-symlink")]
    (try
      (fs/create-dirs (fs/path home ".codex.real"))
      (fs/create-sym-link (fs/path home ".codex") (fs/path home ".codex.real"))
      (with-home home
        (fn []
          (let [r (routing/confine-targets [(str (fs/path home ".codex" "AGENTS.md"))] false)]
            (is (false? (:ok r)))
            (is (= [:symlinked-component] (mapv :reason (:refused r)))))))
      (finally (fs/delete-tree (fs/parent home))))))

(deftest plain-real-directories-under-the-home-are-still-installable
  (let [[home _outside] (redteam-home "real-dirs")]
    (try
      (fs/create-dirs (fs/path home ".codex"))
      (fs/create-dirs (fs/path home ".claude"))
      (with-home home
        (fn []
          (is (:ok (routing/confine-targets
                    [(str (fs/path home ".codex" "AGENTS.md"))
                     (str (fs/path home ".claude" "CLAUDE.md"))]
                    false))
              "a missing final file is an install that creates it")))
      (finally (fs/delete-tree (fs/parent home))))))

(deftest a-missing-parent-directory-is-not-an-authorized-place-to-create-a-file
  (let [[home _outside] (redteam-home "missing-parent")]
    (try
      (with-home home
        (fn []
          (let [r (routing/confine-targets [(str (fs/path home ".codex" "AGENTS.md"))] false)]
            (is (false? (:ok r)))
            (is (= [:missing-parent] (mapv :reason (:refused r)))))))
      (finally (fs/delete-tree (fs/parent home))))))

(deftest scratch-targets-refuse-symlinked-components-too
  (let [root (fs/create-temp-dir {:dir "/var/tmp/forge" :prefix "routing-scratch-link-"})
        outside (fs/create-dirs (fs/path root "outside"))
        nest (fs/create-dirs (fs/path root "nest"))]
    (try
      (fs/create-sym-link (fs/path nest "link") outside)
      (let [r (routing/confine-targets [(str (fs/path nest "link" "AGENTS.md"))] true)]
        (is (false? (:ok r))
            "--scratch keeps the /var/tmp/forge rule AND refuses symlinked components")
        (is (= :agent-routing-target-refused (:error-type r)))
        (is (= [:symlinked-component] (mapv :reason (:refused r)))))
      (is (empty? (fs/list-dir outside)))
      (finally (fs/delete-tree root)))))
