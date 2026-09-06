(ns clj-surgeon.lane-manifest-test
  "TEST-ISO-001's witness. Asserts SET EQUALITY IN BOTH DIRECTIONS between
   three independent descriptions of the JVM test lanes -- the manifest, each
   namespace's own ns metadata, and the `*_test.clj` files on disk -- and that
   the runner REFUSES, by typed message, a namespace with no lane declaration.

   Why all three and not just the manifest: a manifest alone drifts silently.
   A namespace deleted from the manifest simply stops running, and the suite
   goes GREEN with less in it -- the failure mode this repo has already paid
   for once (round one's `mcp-formatter-test` was required by no runner and no
   Make target, and nothing noticed; round three adopted it into :fast and
   made exclusion-into-orphanhood unrepresentable). Set equality in both
   directions is the refusal-kind pattern: absence is as loud as presence."
  {:lane :fast}
  (:require
   [clj-surgeon.battery-ledger :as ledger]
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.mcp-test-runner :as runner]
   [clj-surgeon.runner-membership :as rm]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private test-root (io/file "test"))

(defn- test-source-files
  []
  (->> (file-seq test-root)
       (filter #(.isFile ^java.io.File %))
       (filter #(re-find #"_test\.cljc?$" (.getName ^java.io.File %)))
       sort))

(defn- first-form
  [^java.io.File f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (binding [*read-eval* false]
      (read {:read-cond :allow :eof nil} r))))

(defn- ns-sym-of
  [form]
  (when (and (seq? form) (= 'ns (first form))) (second form)))

(defn- declared-lane
  "The `:lane` in the ns form's metadata, wherever it is spelled -- on the ns
   symbol (`(ns ^{:lane :fast} foo ...)`) or in an attr-map after the name."
  [form]
  (->> (cons (meta (second form)) form)
       (some (fn [x] (when (and (map? x) (contains? x :lane)) (:lane x))))))

(def ^:private on-disk
  "ns symbol -> {:file f :lane <declared or nil>} for every test source file."
  (delay
    (into {}
          (keep (fn [f]
                  (let [form (first-form f)]
                    (when-let [s (ns-sym-of form)]
                      [s {:file (.getPath ^java.io.File f) :lane (declared-lane form)}])))
                (test-source-files)))))

(def ^:private bb-lane
  "The babashka lane's namespaces, read out of `test/run_all.clj` rather than
   restated here -- a second copy of that list would be the very drift this
   witness exists to catch."
  (delay
    (set (map symbol
              (re-seq #"clj-surgeon\.[a-z0-9.\-]+-test"
                      (slurp (io/file "test" "run_all.clj")))))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-001
;; ---------------------------------------------------------------------------

(deftest every-manifest-entry-exists-on-disk
  (testing "manifest -> disk: no phantom entries"
    (let [missing (sort (remove @on-disk (keys lm/manifest)))]
      (is (empty? missing)
          (str "lane manifest names " (count missing)
               " namespace(s) with no test source file on disk: "
               (str/join ", " missing))))))

(deftest every-test-namespace-on-disk-is-accounted-for
  (testing "disk -> manifest: a new test namespace cannot silently never run"
    (let [unaccounted (sort (remove (fn [s]
                                      (or (contains? lm/manifest s)
                                          (contains? @bb-lane s)
                                          (contains? lm/excluded s)))
                                    (keys @on-disk)))]
      (is (empty? unaccounted)
          (str (count unaccounted)
               " test namespace(s) on disk belong to no lane and are not "
               "declared in clj-surgeon.lane-manifest/excluded: "
               (str/join ", " unaccounted))))))

(deftest every-lane-declares-a-cadence-the-runner-knows
  (testing "lane -> cadence is set-equal with the lanes, both directions"
    (is (= (set lm/lanes) (set (keys lm/lane-cadence)))
        (str "a lane with no cadence, or a cadence for a lane that does not "
             "exist: lanes " (pr-str lm/lanes) " vs "
             (pr-str (sort (keys lm/lane-cadence)))))
    (doseq [[lane cadence] lm/lane-cadence]
      (is (contains? lm/cadences cadence)
          (str "lane " lane " declares cadence " (pr-str cadence)
               " which the runner does not know; known cadences are "
               (pr-str (sort (keys lm/cadences)))))))
  (testing "every cadence the manifest can name says what it MEANS"
    (doseq [[cadence prose] lm/cadences]
      (is (and (string? prose) (>= (count prose) 40))
          (str "cadence " cadence " must say when it runs, not just be named")))))

(deftest every-manifest-namespace-resolves-to-a-known-cadence
  (let [orphans (sort (remove (comp lm/cadences lm/cadence-of) (keys lm/manifest)))]
    (is (empty? orphans)
        (str (count orphans) " namespace(s) with a lane but no cadence the "
             "runner knows: " (str/join ", " orphans)))))

(deftest the-refusal-message-names-the-cadence-a-lane-costs
  (let [msg (lm/refusal-message 'clj-surgeon.no-such-test)]
    (doseq [lane lm/lanes]
      (is (str/includes? msg (str lane))
          (str "the refusal must name lane " lane)))
    (doseq [cadence (vals lm/lane-cadence)]
      (is (str/includes? msg (str cadence))
          (str "the refusal must name cadence " cadence
               " -- choosing a lane decides how often the test runs, and a "
               "refusal that hides that makes the choice look free")))))

(deftest excluded-entries-are-real-and-carry-a-reason
  (doseq [[s reason] lm/excluded]
    (is (contains? @on-disk s) (str "excluded namespace " s " is not on disk"))
    (is (and (string? reason) (>= (count reason) 20))
        (str "excluded namespace " s " must name WHY it is in no lane"))
    (is (not (contains? lm/manifest s))
        (str s " is both excluded and in the manifest"))))

(deftest every-exclusion-is-actually-run-by-the-runner-it-names
  (testing "an exclusion is a REDIRECTION, and membership -- not existence -- is the proof"
    ;; ROUND FIVE, the round-three landing review's finding 4. The predicate
    ;; this replaces asked only `does a target with this name exist?`, and the
    ;; reviewer's archive-copy sabotage walked straight through it: an
    ;; exclusion reading "`make test-fast`" was accepted for a namespace
    ;; `test-fast` does not run, because that target exists. Existence is a
    ;; SPELLING; the runner's own selection is the fact. `resolve-runner`
    ;; follows the Makefile recipe and the deps.edn alias to the concrete
    ;; namespace set, and an exclusion that is not IN that set is refused.
    (let [violations (rm/exclusion-violations lm/excluded (rm/repo-context))]
      (is (empty? violations)
          (str (count violations) " exclusion(s) that no named runner runs:\n  "
               (str/join "\n  " (map :message violations)))))))

(deftest a-false-redirection-to-an-existing-target-is-refused-by-name
  (testing "the reviewer's finding-4 sabotage, reachable without committing it"
    ;; THE SABOTAGE AS A WITNESS. `make test-fast` exists and runs the whole
    ;; :fast lane; it does not run `clj-surgeon.analyzer-contract-test`. The
    ;; old predicate said yes. This asserts the refusal, its KIND, and that
    ;; the message names the namespace -- so a future rewrite that goes back
    ;; to existence-checking fails here instead of in a reviewer's window.
    (let [saboteur {'clj-surgeon.analyzer-contract-test
                    "false redirection for sabotage -- `make test-fast`"}
          [v :as vs] (rm/exclusion-violations saboteur (rm/repo-context))]
      (is (= 1 (count vs))
          (str "the false redirection must be refused exactly once, got "
               (pr-str (mapv :kind vs))))
      (is (= :not-a-member (:kind v))
          (str "expected :not-a-member -- the target exists, and does not run "
               "it -- got " (pr-str (:kind v))))
      (is (str/includes? (str (:message v)) "clj-surgeon.analyzer-contract-test")
          "the refusal must name its subject")
      (is (str/includes? (str (:message v)) "make test-fast")
          "the refusal must name the runner that was falsely claimed"))))

(deftest an-exclusion-naming-an-unreadable-runner-fails-closed
  (testing "unproven membership is a refusal, never an assumption"
    ;; `I could not work out what that runs` must not read the same as `it
    ;; runs your namespace`. A target no rule defines resolves to nothing, and
    ;; nothing is a refusal.
    (let [saboteur {'clj-surgeon.analyzer-contract-test
                    "redirected to `make no-such-target-anywhere`"}
          [v] (rm/exclusion-violations saboteur (rm/repo-context))]
      (is (= :unresolved-runner (:kind v)) (str "got " (pr-str v)))
      (is (str/includes? (str (:message v)) "no-such-target-anywhere")))))

(deftest the-lane-runner-resolves-to-exactly-the-lane-it-names
  (testing "the resolver follows the runner's own selection, not a restatement"
    ;; The membership check is only as good as the resolution under it, so the
    ;; resolution is pinned against the manifest directly: `make test-fast`
    ;; must come back as the :fast lane and `make mcp-test` as fast+integration
    ;; -- which is also the pin that catches someone changing an alias's
    ;; :main-opts without changing what the gate is understood to cover.
    (let [ctx (rm/repo-context)]
      (is (= (set (lm/namespaces-for :fast))
             (:namespaces (rm/resolve-runner "make test-fast" ctx))))
      (is (= (into (set (lm/namespaces-for :fast)) (lm/namespaces-for :integration))
             (:namespaces (rm/resolve-runner "make mcp-test" ctx))))
      (is (= (set (lm/namespaces-for :battery))
             (:namespaces (rm/resolve-runner "make test-battery" ctx)))))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-009b -- the battery discipline ON THE LANDING PATH
;; ---------------------------------------------------------------------------

(deftest the-landing-gate-runs-both-the-merge-gate-and-the-battery-tripwire
  (testing "the freshness tripwire is a PREREQUISITE of landing, not an option"
    ;; The round-three landing review's finding 2: `make battery-fresh` exists,
    ;; and neither `~/bin/land` nor `make mcp-test` invokes it, so the eleven
    ;; namespaces moved off the merge gate are not mechanically required
    ;; before a landing. A tripwire nobody's path runs is a diary entry.
    ;;
    ;; `make landing-gate` is THE target ~/bin/land runs. It is asserted here
    ;; by RESOLUTION, not by grepping for a word: the target must exist, and
    ;; its prerequisite/recipe closure must contain both names.
    (let [{:keys [makefile-text] :as ctx} (rm/repo-context)
          rule (rm/make-target makefile-text "landing-gate")
          closure (set (concat (:prerequisites rule)
                               (map second (re-seq #"\$\(MAKE\)(?:\s+--[a-z\-]+)*\s+([a-z0-9\-]+)"
                                                   (str (:recipe rule))))))]
      (is (some? rule)
          "no rule in the Makefile defines `landing-gate` -- ~/bin/land has no gate to call")
      (is (contains? closure "battery-fresh")
          (str "`make landing-gate` must run the battery freshness tripwire; "
               "its closure is " (pr-str (sort closure))))
      (is (contains? closure "mcp-test")
          (str "`make landing-gate` must run the merge gate; its closure is "
               (pr-str (sort closure))))
      (is (str/includes? makefile-text ".PHONY: repository-hygiene")
          "sanity: the .PHONY line was found")
      (is (re-find #"(?m)^\.PHONY:.*\blanding-gate\b" makefile-text)
          "`landing-gate` must be .PHONY -- it produces no file"))))

(deftest the-landing-gate-refuses-a-stale-battery-receipt
  (testing "the refusal the landing path actually delivers, as data"
    ;; The tripwire's verdict function, driven at the exact boundary the
    ;; landing gate depends on: a receipt older than the ceiling is a refusal
    ;; that carries the remedy. Pure, so the fast lane can hold the landing
    ;; path to it without a repository shaped to produce it.
    (let [now 1000000000000
          entry {:sha "deadbeef" :started (str (java.time.Instant/ofEpochMilli
                                                 (- now (* 27 60 60 1000))))
                 :wall_s 700 :verdict :pass :host "anvil"}
          r (ledger/freshness [entry] now (constantly 0))]
      (is (false? (:ok r)))
      (is (= :stale (:reason r)))
      (is (str/includes? (:remedy r) "make test-battery")
          (str "the refusal must carry the remedy, got " (pr-str (:remedy r)))))))

(deftest every-manifest-namespace-declares-its-lane-in-its-own-ns-form
  (testing "source metadata agrees with the manifest, per namespace"
    (let [wrong (sort-by first
                         (keep (fn [[s lane]]
                                 (let [declared (:lane (get @on-disk s))]
                                   (when (not= declared lane)
                                     [s lane declared])))
                               lm/manifest))]
      (is (empty? wrong)
          (str (count wrong) " namespace(s) whose ns metadata does not declare "
               "the manifest's lane (expected/declared): "
               (str/join "; " (map (fn [[s want got]]
                                     (format "%s want %s got %s" s want (pr-str got)))
                                   (take 10 wrong)))
               (when (> (count wrong) 10) " ..."))))))

(deftest loaded-namespaces-carry-their-lane-at-runtime
  (testing "the metadata survives loading -- a source scan alone is a spelling"
    (doseq [[s lane] lm/manifest
            :when (find-ns s)]
      (is (= lane (:lane (meta (find-ns s))))
          (str s " is loaded but its runtime ns metadata :lane is "
               (pr-str (:lane (meta (find-ns s)))))))))

(deftest the-runner-refuses-an-undeclared-namespace
  (testing "an undeclared namespace is a typed refusal, never a silent skip"
    (let [result (runner/lane-namespaces [:fast] ['clj-surgeon.no-such-lane-test])]
      (is (= :lane-undeclared (:refusal result))
          (str "expected a typed :lane-undeclared refusal, got " (pr-str result)))
      (is (= ['clj-surgeon.no-such-lane-test] (:namespaces result))
          "the refusal must name its subject")
      (is (str/includes? (str (:message result)) "lane-refused:")
          (str "the refusal must carry the typed message, got "
               (pr-str (:message result)))))))

(deftest the-runner-resolves-a-declared-lane
  (let [{:keys [refusal namespaces]} (runner/lane-namespaces [:fast] nil)]
    (is (nil? refusal))
    (is (= (set (lm/namespaces-for :fast)) (set namespaces)))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-002 (source-scanning half only; the runtime half is round three)
;; ---------------------------------------------------------------------------

(def ^:private spawn-spellings
  "Names that mean `this namespace launches a child process`. A source scan is
   a SPELLING CHECK, not a proof -- a helper in another namespace defeats it.
   It is here because it is nearly free and it catches the copy-paste case;
   the runtime descendant count is round three's job."
  [#"\bProcessBuilder\b"
   #"clojure\.java\.shell"
   #"babashka\.process"
   #"\bbabashka/process\b"
   #"\bproc/(?:process|shell|sh)\b"
   #"\bsh/sh\b"])

(deftest no-fast-lane-namespace-spells-a-child-process
  (let [offenders
        (sort-by first
                 (for [[s lane] lm/manifest
                       :when (= :fast lane)
                       :let [src (slurp (:file (get @on-disk s)))]
                       re spawn-spellings
                       :when (re-find re src)]
                   [s (str re)]))]
    (is (empty? offenders)
        (str "fast-lane namespace(s) spelling a child-process launcher -- the "
             "fast lane's rule is NO child process (move it to :battery): "
             (str/join "; " (map (fn [[s re]] (str s " ~ " re)) offenders))))))

(def ^:private round-one-jvm-namespaces
  "The 49 namespaces `clojure -M:clj-surgeon/mcp-test` ran at commit c4f69081
   (round one: 865 tests, 13 023 assertions, 0 failures). Pinned so that
   PARTITIONING cannot become DROPPING: a namespace that leaves every lane
   fails here by name, and a green suite with less in it is impossible."
  '#{clj-surgeon.admit-patch-test
     clj-surgeon.census-pool-test
     clj-surgeon.core-discovery-test
     clj-surgeon.mcp-alias-migration-test
     clj-surgeon.mcp-change-buffer-test
     clj-surgeon.mcp-cold-verify-test
     clj-surgeon.mcp-combinable-transaction-test
     clj-surgeon.mcp-compact-edit-fields-test
     clj-surgeon.mcp-compact-edit-test
     clj-surgeon.mcp-compact-location-test
     clj-surgeon.mcp-compact-relations-test
     clj-surgeon.mcp-contract-test
     clj-surgeon.mcp-create-files-test
     clj-surgeon.mcp-extraction-plan-test
     clj-surgeon.mcp-extraction-test
     clj-surgeon.mcp-hot-verify-test
     clj-surgeon.mcp-http-server-test
     clj-surgeon.mcp-inspect-contract-test
     clj-surgeon.mcp-inspect-tool-test
     clj-surgeon.mcp-intent-contract-test
     clj-surgeon.mcp-operation-async-test
     clj-surgeon.mcp-operation-registry-test
     clj-surgeon.mcp-operation-test
     clj-surgeon.mcp-paths-test
     clj-surgeon.mcp-prepared-confirmation-test
     clj-surgeon.mcp-prepared-request-test
     clj-surgeon.mcp-prepared-wire-test
     clj-surgeon.mcp-process-test
     clj-surgeon.mcp-program-tool-test
     clj-surgeon.mcp-read-request-normalization-test
     clj-surgeon.mcp-recovery-test
     clj-surgeon.mcp-relation-census-launcher-test
     clj-surgeon.mcp-relation-census-round20-test
     clj-surgeon.mcp-relation-census-test
     clj-surgeon.mcp-schema-test
     clj-surgeon.mcp-semantic-client-test
     clj-surgeon.mcp-server-test
     clj-surgeon.mcp-telemetry-test
     clj-surgeon.mcp-tool-test
     clj-surgeon.mcp-workspace-test
     clj-surgeon.mcp-write-refusal-test
     clj-surgeon.outline-differential-test
     clj-surgeon.outline-memory-test
     clj-surgeon.quoted-var-refs-test
     clj-surgeon.reader-eval-fence-test
     clj-surgeon.repository-hygiene-test
     clj-surgeon.scope-stream-test
     clj-surgeon.txn-journal-test
     clj-surgeon.workspace-onboarding-test})

(deftest the-partition-drops-nothing-round-one-measured
  (let [dropped (sort (remove lm/manifest round-one-jvm-namespaces))]
    (is (= 49 (count round-one-jvm-namespaces)))
    (is (empty? dropped)
        (str (count dropped) " namespace(s) that round one MEASURED are in no "
             "lane -- partitioning must never drop: " (str/join ", " dropped)))))

(deftest the-partition-matches-round-ones-measurement
  (testing "counts are pinned so a silent re-partition is loud"
    (is (= 49 (count (lm/namespaces-for :fast))))
    (is (= 6 (count (lm/namespaces-for :integration))))
    (is (= 32 (count (lm/namespaces-for :battery))))
    (is (= 87 (count lm/manifest))
        (str "round one's 49 measured namespaces, plus the two round-two "
             "witnesses (fast-lane-isolation-test, lane-manifest-test), plus "
             "round three's adopted orphan (mcp-formatter-test) and its "
             "battery-ledger witness, plus round four's six runtime purity "
             "witnesses in ns-isolation-test, plus round five's "
             "mcp-inspect-cold-job-test -- the one inspect-tool test that "
             "spawns a child, moved out of a :fast namespace into :battery; "
             "and the trunk's mcp-feature-thread-test, adopted at this merge "
             "with its own `sed` cross-check split into "
             "mcp-feature-thread-sed-test (:battery) for the same reason; "
             "and mission-forms-source-test, the comment-preserving source "
             "lowering that replaced the blanket comment refusal"))))

(defn- deftest-count
  "How many `deftest` forms a namespace's source file declares. A SOURCE
   census, deliberately: it is the same number for every box and every load,
   whereas assertion counts are context-sensitive (the round-two review
   measured 4,319 assertions summing the lanes separately and 4,323 running
   them together) and a pin that moves with the weather teaches people to
   re-bless it."
  [ns-sym]
  (let [file (:file (get @on-disk ns-sym))]
    (count (re-seq #"(?m)^\(deftest " (slurp file)))))

(def ^:private adopted-since-round-one
  "Namespaces in a lane today that round one did NOT measure, each with the
   number of tests it brings and why it exists. This is the ONLY legal way
   the corpus grows without the arithmetic below going red."
  '{clj-surgeon.outline-corpus-integration-test 1 ; MOVED: full repository differential out of the bounded fast namespace.
    clj-surgeon.mission-candidate-race-test 5 ; Completion-order delivery, bounded cancellation and retained results.
    clj-surgeon.mission-events-test 8 ; Public completion events and isolated logging failure.
    clj-surgeon.mission-phase-events-test 7 ; Actual phase receipts, identity and isolated logging failure.
    clj-surgeon.mission-provider-fallback-events-test 8 ; Actual dispatched fallback, thread context and isolated logging.
    clj-surgeon.mission-display-test 14 ; Add historical nested refusal and incompatible-example witnesses.
    clj-surgeon.mission-fallback-test 8 ; Explicit report, actual event write and unchanged proof.
    clj-surgeon.mission-git-identity-test 3 ; Explicit seat author/committer survive subprocess sanitization.
    clj-surgeon.mission-git-submodule-test 2 ; Git config cannot hide staged gitlinks from scope guard.
    clj-surgeon.mission-publication-test 7 ; Durable publication intent blocks silent source undo.
    clj-surgeon.mission-git-test 4 ; Pure Git provenance contract.
    clj-surgeon.mission-git-boundary-test 4 ; Git tree and staged path boundaries.
    clj-surgeon.mission-git-fence-test 5 ; Identity and refusal witnesses.
    clj-surgeon.mission-git-process-test 2 ; Bounded subprocess lifecycle.
    clj-surgeon.mission-git-ledger-test 3 ; Saved receipt authority.
    clj-surgeon.mission-commit-cli-test 4 ; Actual public command behavior.
    clj-surgeon.mission-usage-test 7 ; Observed legacy/attempt usage and unknowns.
    clj-surgeon.mission-typist-executor-admission-test 2 ; Unsupported adapter refused before readiness.
    clj-surgeon.mission-usage-executor-test 2 ; Saved success/refusal usage snapshots.
    clj-surgeon.mission-run-test 11 ; One-process saved plan, refusal and CLI boundaries.
    clj-surgeon.mission-test 27 ; Adopt existing ledger orphan plus owner-forms routing and recovery witnesses.
    clj-surgeon.mission-typist-test 6 ; Pure routing/dossier and frozen generation policy boundaries.
    clj-surgeon.mission-candidate-test 5 ; Frozen span lowering boundaries.
    clj-surgeon.mission-plain-forms-test 8 ; Bounded raw definition decoding and actual escaping failure.
    clj-surgeon.mission-forms-test 5 ; Owner identity, protected syntax and lost-comment refusal.
    clj-surgeon.mission-forms-source-test 23 ; Strict comment text/attachment, whitespace identity and owner sentinel.
    clj-surgeon.mission-typist-executor-test 11 ; Add candidate diagnostic survival to proof/commit/undo and saved fallback forwarding.
    clj-surgeon.battery-ledger-test        14 ; TEST-ISO-009a/b: add strict archive classification and preserved failure/audit authority.
    clj-surgeon.fast-lane-isolation-test   4  ; TEST-ISO-006's witness (round two) + round five's finding-3 fixture-root scan
    clj-surgeon.lane-manifest-test         25 ; TEST-ISO-001's witness (round two) + round three's exclusion, arithmetic and rename pins + round five's four membership witnesses and two landing-gate witnesses
    clj-surgeon.mcp-formatter-test         3  ; the adopted orphan (round three)
    clj-surgeon.mcp-feature-thread-test    69 ; the trunk's `feature_thread` verb, adopted at round five's MCP/main merge
    clj-surgeon.mcp-feature-thread-sed-test 1 ; MOVED, not new (round five): its one `sed` cross-check, out of :fast into :battery
    clj-surgeon.mcp-inspect-cold-job-test  1  ; MOVED, not new (round five): the one inspect-tool test that drives /bin/sh, out of :fast into :battery
    clj-surgeon.ns-isolation-test          24  ; TEST-ISO-002/003/004/005/007/010's witnesses (round four) + round five's four spawn-ledger witnesses
    clj-surgeon.helper-extraction-test     34  ; MCP-OP-HELPER's pure planner witnesses, enrolled into :fast when the planner went green (it requires only the planner, the fixture and clojure.test, and spawns nothing)
    clj-surgeon.telemetry-events-test       17  ; TELEMETRY-EVENTS-001's witnesses: the box-wide JSONL ledger the public MCP fns append to as a side effect (2026-09-06, the night the hourly watch reported four figures while a dozen calls landed in launcher-chosen roots it never read)
    clj-surgeon.mcp-helper-extraction-test 51}) ; MCP-OP-HELPER's boundary witnesses, :battery because they spawn babashka children to prove fixture trees LOAD and drive real execute! transactions

(deftest the-corpus-only-ever-grows-and-the-arithmetic-is-shown
  ;; ASTRA 2026-09-06: add 27 ledger + 14 pure typist + 6 executor + 5 race = 52 tests.
  ;; ASTRA run entrance adds 8 boundary tests; current arithmetic: 921 + 281 = 1202. Counts below retain history.
  ;; THE NOTHING-DROPPED PIN, recomputed for round three.
  ;;
  ;; Round one MEASURED 865 tests / 13,023 assertions across the 49 namespaces
  ;; pinned in `round-one-jvm-namespaces`. Partitioning must never turn into
  ;; dropping, so two things are checked, and the second is the one that
  ;; actually holds the line:
  ;;
  ;;   round one's 49 namespaces, today ........... 920 deftests  (>= 865)
  ;;   adopted since round one .................... 230 deftests  (12+4+25+3+69+1+1+24+34+48+9)
  ;;                                                --------------
  ;;   total declared by the manifest ............. 1151 deftests
  ;;
  ;; ROUND SIX ADOPTED THE HELPER-EXTRACTION PAIR, 68 deftests, on the day the
  ;; planner and the boundary both went green. They had been `excluded` with
  ;; their own red targets for exactly as long as the namespaces they witness
  ;; did not exist -- the repository's pattern for a not-yet-implemented
  ;; witness -- and enrolling them retires those targets. The split is the
  ;; lanes' own rule rather than a preference: the pure half requires only the
  ;; planner, the fixture and clojure.test and spawns nothing, so it is :fast;
  ;; the boundary half launches babashka children to prove fixture trees LOAD
  ;; and drives real execute! transactions, so it is :battery.
  ;;
  ;; ROUND FIVE MOVED ONE TEST OUT of a round-one namespace, which is why the
  ;; first line went 921 -> 920, and it is worth saying plainly because it is
  ;; the exact shape this pin exists to police. It was not deleted: the one
  ;; inspect-tool test that drives /bin/sh through the production cold-verify
  ;; helper moved into clj-surgeon.mcp-inspect-cold-job-test (:battery), and
  ;; that namespace's line in `adopted-since-round-one` carries the +1. The
  ;; equality below is what makes the distinction load-bearing: a MOVE keeps
  ;; the total, a DELETION does not, and only one of them can pass here.
  ;;
  ;; A namespace leaving a lane fails `the-partition-drops-nothing-...` by
  ;; name; a namespace's tests being deleted fails the >= below; anything
  ;; joining the corpus without a line in `adopted-since-round-one` fails the
  ;; equality. Moving a test needs a reason AT the pin, which is the point.
  (let [r1 (reduce + (map deftest-count round-one-jvm-namespaces))
        adopted (reduce + (map deftest-count (keys adopted-since-round-one)))
        total (reduce + (map deftest-count (keys lm/manifest)))]
    (testing "every namespace round one measured still declares at least as much"
      (is (>= r1 865)
          (str "round one's 49 namespaces declare " r1 " tests today, fewer "
               "than the 865 it MEASURED -- tests were deleted, not moved")))
    (testing "the tests adopted since round one are exactly the declared ones"
      (doseq [[s n] adopted-since-round-one]
        (is (= n (deftest-count s))
            (str s " declares " (deftest-count s) " tests, not the pinned " n
                 " -- update the pin WITH the reason")))
      (is (= (set (keys adopted-since-round-one))
             (set (remove round-one-jvm-namespaces (keys lm/manifest))))
          (str "a namespace joined or left the corpus without a line at the "
               "pin: in a lane but unpinned "
               (pr-str (sort (remove (some-fn round-one-jvm-namespaces
                                              (set (keys adopted-since-round-one)))
                                     (keys lm/manifest))))))
      ;; One outline corpus test MOVED from its original namespace to adopted integration.
      ;; 928 original + 435 adopted = 1363: add 7 inspect owner_counts/source-omission
      ;; witnesses (5 in mcp-inspect-contract-test, 2 in mcp-inspect-tool-test), both
      ;; round-one namespaces, so the growth lands in the original half and adopted holds;
      ;; add the real two-require cardinality
      ;; regression in mcp-contract-test; retain Astra identity/receipt witnesses and trunk
      ;; helper request-shape refusals (48 -> 51), plus two battery archival-distance witnesses;
      ;; closed telemetry remains 17, not trunk
      ;; passthrough-field 18, and mission ledger remains the executor-extended 27.
      (is (= 435 adopted) (str "adopted tests: " adopted)))
    (testing "the arithmetic closes"
      ;; 1363 -> 1370: seven MCP-OP-VERIFY-011/012/013 witnesses -- four in
      ;; mcp-tool-test (success text states the verification actually
      ;; performed, failure text carries the check's own bytes, its bound cuts
      ;; at a line boundary, alias receipts do the same) and three in
      ;; mcp-http-server-test (built-in profiles are lint-only, an
      ;; unconfigured workspace refuses `verify` before any write, a
      ;; configured one is unchanged). Both are round-one namespaces, so the
      ;; growth lands in the original half and `adopted` holds at 435.
      (is (= 1370 total) (str "manifest declares " total " tests"))
      (is (= total (+ r1 adopted))
          (str total " != " r1 " + " adopted
               " -- a namespace is being counted twice or not at all")))))

;; ---------------------------------------------------------------------------
;; The intent audit for this family.
;;
;; A MARKER AUDIT IS NOT A RATCHET. This checks that every `@spec TEST-ISO-*`
;; marker in the tree is a registered requirement and that every registered
;; requirement marked implemented is claimed by at least one marker. That
;; catches an id invented in a comment and a requirement whose implementation
;; quietly vanished; it CANNOT catch a marker over code that does not do what
;; the requirement says. The behavioural witnesses above and in
;; `clj-surgeon.fast-lane-isolation-test` are the proof; this is the index.
;; ---------------------------------------------------------------------------

(def ^:private specs-doc "docs/intent/test-isolation/test-isolation-specs.md")

(defn- spec-ids-in-tree
  []
  (->> (concat (test-source-files)
               (filter #(.isFile ^java.io.File %) (file-seq (io/file "dev")))
               [(io/file "Makefile")])
       (filter #(.isFile ^java.io.File %))
       (mapcat (fn [f] (re-seq #"TEST-ISO-[A-Z0-9\-]+" (slurp f))))
       set))

(defn- registered-ids
  []
  (let [text (slurp (io/file specs-doc))]
    {:all (set (re-seq #"TEST-ISO-[A-Z0-9\-]+" text))
     :implemented (set (map second
                            (re-seq #"- \[x\] \*\*(TEST-ISO-[A-Z0-9\-]+)\*\*" text)))}))

;; @spec TEST-ISO-001
(deftest every-test-iso-marker-in-the-tree-is-a-registered-requirement
  (let [{:keys [all]} (registered-ids)
        unregistered (sort (remove all (spec-ids-in-tree)))]
    (is (empty? unregistered)
        (str "@spec marker(s) naming no requirement in " specs-doc ": "
             (str/join ", " unregistered)))))

;; @spec TEST-ISO-001
(deftest every-implemented-requirement-is-claimed-by-a-marker
  (let [{:keys [implemented]} (registered-ids)
        in-tree (spec-ids-in-tree)
        ;; The composite ids (001a, 001b) are witnessed by deftests named in
        ;; the specs document rather than by their own source marker; the
        ;; audit checks their PARENT is claimed.
        expected (remove #{"TEST-ISO-001a" "TEST-ISO-001b"} implemented)
        unclaimed (sort (remove in-tree expected))]
    (is (seq implemented) "the specs document must parse")
    (is (empty? unclaimed)
        (str "requirement(s) marked implemented that no @spec marker claims -- "
             "an implementation that vanished leaves the document lying: "
             (str/join ", " unclaimed)))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-001
;; The rename ratchet. Until 2026-09-04 the name `test-fast` MEANT
;; `bb test/run_all.clj`; it now means the JVM fast lane, and the babashka
;; corpus is `make test-bb`. A rename whose old meaning survives in living
;; prose is worse than no rename: the target still resolves, the suite is
;; still green, and the reader runs the wrong lane. Historical receipts under
;; docs/observations keep the old name and MEAN the old lane -- correct, they
;; are evidence of what ran. This covers the LIVING set only.
;;
;; The scan flags a sentence that EQUATES the two names -- the old name and a
;; babashka spelling in the same line, or wrapped across the line before it,
;; which is how all four real defects read. A window that also names
;; `test-bb` is the rename being EXPLAINED, which is what we want, so it is
;; exempt; that is why this comment names it.
;; ---------------------------------------------------------------------------

(def ^:private living-prose-roots
  ["src" "test" "dev" "bench" "skills" "docs/intent" ".github"])

(def ^:private living-prose-files
  ["Makefile" "deps.edn" "README.md"
   ;; reusable briefs and boot-read notes: prose an agent will ACT on
   "docs/observations/2026-09-02-anvil-builder-seat-brief.md"])

(defn- living-prose
  []
  (->> (concat (mapcat #(file-seq (io/file %)) living-prose-roots)
               (map io/file living-prose-files))
       (filter #(.isFile ^java.io.File %))
       (remove #(str/includes? (.getPath ^java.io.File %) "/.git/"))))

(def ^:private old-name (str "test-" "fast"))

(deftest no-living-prose-still-calls-the-bb-lane-by-its-old-name
  (let [bb-spelling #"(?i)babashka|run_all\.clj"
        offenders
        (sort-by first
                 (for [^java.io.File f (living-prose)
                       :let [lines (vec (str/split-lines (slurp f)))]
                       [i line] (map-indexed vector lines)
                       :when (str/includes? line old-name)
                       ;; the claim, plus the line before it, because a
                       ;; wrapped sentence puts "babashka's" on the line above
                       :let [claim (str/join " " (subvec lines (max 0 (dec i)) (inc i)))]
                       :when (re-find bb-spelling claim)
                       :let [nearby (str/join " " (subvec lines
                                                          (max 0 (- i 4))
                                                          (min (count lines) (+ i 5))))]
                       :when (not (str/includes? nearby "test-bb"))]
                   [(str (.getPath f) ":" (inc i)) (str/trim line)]))]
    (is (empty? offenders)
        (str (count offenders)
             " living line(s) still equating the babashka lane with the name "
             "`" old-name "`. The babashka corpus is `make test-bb` since "
             "2026-09-04 and that name is now the JVM fast lane, so this prose "
             "sends the reader to the wrong suite: "
             (str/join "; " (map (fn [[loc line]] (str loc " -- " line)) offenders))))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-007 -- ROUND FIVE, the review's non-blocking item: bounded
;; polling sleeps in the merge-gate lanes.
;;
;; A test that sleeps is asserting about a CLOCK. Sometimes that is the only
;; honest thing to do -- proving a thread is dead, or that a weak reference was
;; collected, means waiting for something this JVM does not schedule -- and the
;; right shape for those is a loop that succeeds THE INSTANT the condition
;; holds and fails at a named deadline, which is what round three's GC fix
;; installed. What must not happen is a fixed sleep quietly appearing because
;; it made a flake go away.
;;
;; So every sleep site in the fast and integration lanes is ENUMERATED here
;; with the reason it exists. A new one fails this witness by file and line
;; and has to argue for itself at the pin. This is a declared-exemption list,
;; not a ban -- the same shape as `namespace-budget-overrides`.
;; ---------------------------------------------------------------------------

(def ^:private declared-merge-gate-sleeps
  "file -> {line -> why}. The line is deliberately part of the key: moving one
   of these is an edit worth re-reading, and the pin costs one number."
  {"test/clj_surgeon/census_pool_test.clj"
   {19 "bounded poll -- succeeds the instant every worker thread is dead, fails after 100 tries"
    38 "the ONLY fixed sleep left on the gate: 5 ms inside the work fn to force the pool to spread work across more than one thread. It backs `(> (count @threads) 1)`, which is a claim about scheduling and cannot be made without one."}
   "test/clj_surgeon/scope_stream_test.clj"
   {105 "bounded poll -- System/gc then re-check reachability, succeeds immediately, fails at gc-deadline-ms (round three's fix for the two fixed `Thread/sleep 100` assertions)"}
   "test/clj_surgeon/mcp_tool_test.clj"
   {1380 "bounded poll -- succeeds as soon as the job reports complete, bounded by an attempt count"}})

(deftest every-sleep-on-the-merge-gate-is-declared-with-its-reason
  (let [sources (fn [lane]
                  (for [n (lm/namespaces-for lane)]
                    (:file (get @on-disk n))))
        ;; A CALL, not a mention. The first cut matched its own regex literal
        ;; (this very line) and a docstring that quotes the old fixed-sleep
        ;; shape it replaced -- a scanner that cannot tell code from prose
        ;; about code reports its own text and teaches people to ignore it.
        ;; So: a literal argument must follow, and a line whose first
        ;; non-blank character starts a comment is prose.
        sleep-call #"\(Thread/sleep\s+[0-9(]"
        found (for [path (concat (sources :fast) (sources :integration))
                    :let [lines (str/split-lines (slurp (io/file path)))]
                    [i line] (map-indexed vector lines)
                    :when (re-find sleep-call line)
                    ;; prose, two ways: a `;` comment, and a backtick-quoted
                    ;; CITATION of the shape inside a docstring -- which is how
                    ;; scope-stream-test records the fixed sleep it REPLACED.
                    ;; Quoting a defect in the note explaining its removal must
                    ;; not read as committing it.
                    :when (not (str/starts-with? (str/triml line) ";"))
                    :when (not (re-find #"`\(Thread/sleep" line))]
                [path (inc i) (str/trim line)])
        undeclared (remove (fn [[path line _]]
                             (get-in declared-merge-gate-sleeps [path line]))
                           found)
        stale (for [[path lines] declared-merge-gate-sleeps
                    [line _] lines
                    :when (not (some (fn [[p l _]] (and (= p path) (= l line))) found))]
                (str path ":" line))]
    (is (empty? undeclared)
        (str (count undeclared) " undeclared Thread/sleep site(s) in the "
             "merge-gate lanes. A sleep is an assertion about a clock: make it "
             "a bounded poll that succeeds on the CONDITION and fails at a "
             "named deadline, then declare it in "
             "`declared-merge-gate-sleeps` with the reason it must wait: "
             (str/join "; " (map (fn [[p l s]] (str p ":" l " -- " s)) undeclared))))
    (is (empty? stale)
        (str "declared sleep site(s) that are no longer there -- delete the "
             "line from the pin: " (str/join ", " stale)))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-001 -- ROUND FIVE: the rename scanner's REACH, pinned.
;;
;; The two sentences below are FIXTURES for that scanner, not instructions --
;; the babashka corpus is `make test-bb`, and naming it here is also what
;; exempts this block from the scanner's own window rule (a passage that names
;; `test-bb` is the rename being EXPLAINED, which is what the exemption is
;; for). Nothing in this block tells a reader to run anything.
;;
;; The round-three review's remaining non-blocking item: prose that names
;; `test-fast` but contains no Babashka spelling cannot be classified by the
;; scanner above, and that limitation is disclosed. Disclosure decays -- the
;; next reader sees a green witness called `no-living-prose-still-calls-the-
;; bb-lane-by-its-old-name` and reasonably concludes the rename is fully
;; covered. So the reach is a WITNESS: the scanner is asserted NOT to flag the
;; unreachable shape. When someone closes the gap, this test fails and they
;; delete it, which is the correct way to find out that a limit is gone.
;; ---------------------------------------------------------------------------

(deftest the-rename-scanner-cannot-see-a-bb-less-mention-and-says-so
  (let [bb-spelling #"(?i)babashka|run_all\.clj"
        ;; The two shapes, side by side, through the scanner's own predicate.
        ;; FIXTURES, not instructions: the babashka corpus is `make test-bb`.
        ;; Naming it here is also what exempts these two lines from the
        ;; scanner itself, whose window rule treats a passage that says
        ;; `test-bb` as the rename being EXPLAINED. The witness has to sit
        ;; inside its own subject's exemption to exist at all.
        classifiable "Run the babashka corpus with make test-fast."
        invisible "For the quick suite, run make test-fast."]
    (is (some? (re-find bb-spelling classifiable))
        "sanity: a sentence naming babashka IS classifiable")
    (is (nil? (re-find bb-spelling invisible))
        (str "RESIDUAL, round three review, non-blocking: a sentence that "
             "means the babashka lane without naming babashka or run_all.clj "
             "is invisible to this scanner. It is caught by a human or not at "
             "all. Closing it needs a meaning-level check, not a wider regex "
             "-- a wider regex would flag every legitimate mention of the JVM "
             "fast lane, and a witness that cries wolf gets deleted."))))
