(ns ^{:lane :fast} clj-surgeon.lane-manifest-test
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
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.battery-ledger :as ledger]
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
  '#{
   clj-surgeon.admit-patch-test
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
   clj-surgeon.workspace-onboarding-test
   })

(deftest the-partition-drops-nothing-round-one-measured
  (let [dropped (sort (remove lm/manifest round-one-jvm-namespaces))]
    (is (= 49 (count round-one-jvm-namespaces)))
    (is (empty? dropped)
        (str (count dropped) " namespace(s) that round one MEASURED are in no "
             "lane -- partitioning must never drop: " (str/join ", " dropped)))))

(deftest the-partition-matches-round-ones-measurement
  (testing "counts are pinned so a silent re-partition is loud"
    (is (= 39 (count (lm/namespaces-for :fast))))
    (is (= 4 (count (lm/namespaces-for :integration))))
    (is (= 11 (count (lm/namespaces-for :battery))))
    (is (= 54 (count lm/manifest))
        (str "round one's 49 measured namespaces, plus the two round-two "
             "witnesses (fast-lane-isolation-test, lane-manifest-test), plus "
             "round three's adopted orphan (mcp-formatter-test) and its "
             "battery-ledger witness, plus round four's six runtime purity "
             "witnesses in ns-isolation-test"))))

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
  '{clj-surgeon.battery-ledger-test      12  ; TEST-ISO-009a/b's witness (round three)
    clj-surgeon.fast-lane-isolation-test 3   ; TEST-ISO-006's witness (round two)
    clj-surgeon.lane-manifest-test       18  ; TEST-ISO-001's witness (round two) + round three's exclusion, arithmetic and rename pins
    clj-surgeon.mcp-formatter-test       3   ; the adopted orphan (round three)
    clj-surgeon.ns-isolation-test        17}) ; TEST-ISO-002/003/004/005/007/010's witnesses (round four)

(deftest the-corpus-only-ever-grows-and-the-arithmetic-is-shown
  ;; THE NOTHING-DROPPED PIN, recomputed for round three.
  ;;
  ;; Round one MEASURED 865 tests / 13,023 assertions across the 49 namespaces
  ;; pinned in `round-one-jvm-namespaces`. Partitioning must never turn into
  ;; dropping, so two things are checked, and the second is the one that
  ;; actually holds the line:
  ;;
  ;;   round one's 49 namespaces, today ........... 921 deftests  (>= 865: the
  ;;                                                trunk ADDED tests to them;
  ;;                                                it never removed any)
  ;;   adopted since round one ..................... 53 deftests  (12 + 3 + 18 + 3 + 17)
  ;;                                                --------------
  ;;   total declared by the manifest .............. 974 deftests
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
      (is (= 53 adopted) (str "adopted tests: " adopted)))
    (testing "the arithmetic closes"
      (is (= 974 total) (str "manifest declares " total " tests"))
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
