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
   [clj-surgeon.mcp-test-runner :as runner]
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

(deftest every-exclusion-names-a-runner-that-actually-exists
  (testing "an exclusion is a REDIRECTION, never a declaration of orphanhood"
    ;; Round two declared `mcp-formatter-test` excluded with the reason
    ;; "required by no runner and no Make target". That made the omission
    ;; VISIBLE, which is better than silence -- but visible loss is still
    ;; loss, and the round-two review called it blocking. The instance fix is
    ;; to give that namespace a lane. THIS is the class fix: an entry in
    ;; `excluded` must name the OTHER runner that runs it, and that runner
    ;; must exist. A namespace no runner runs cannot be declared away; it can
    ;; only be adopted into a lane or deleted.
    (let [makefile (slurp (io/file "Makefile"))
          deps (slurp (io/file "deps.edn"))
          target? (fn [t] (or (re-find (re-pattern (str "(?m)^" (java.util.regex.Pattern/quote t) ":")) makefile)
                              (str/includes? makefile (str " " t " "))))
          alias? (fn [a] (str/includes? deps (str ":clj-surgeon/" a)))]
      (doseq [[s reason] lm/excluded]
        (let [targets (map second (re-seq #"`make ([a-z0-9\-]+)`" reason))
              aliases (map second (re-seq #":clj-surgeon/([a-z0-9\-]+)" reason))
              named (concat (filter target? targets) (filter alias? aliases))]
          (is (seq named)
              (str "excluded namespace " s " names no runner that exists. An "
                   "exclusion must redirect to a real `make <target>` or "
                   ":clj-surgeon/<alias> that runs it (TEST-ISO-001); its "
                   "reason was: " (pr-str reason))))))))

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
