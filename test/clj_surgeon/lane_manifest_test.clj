(ns ^{:lane :fast} clj-surgeon.lane-manifest-test
  "TEST-ISO-001's witness. Asserts SET EQUALITY IN BOTH DIRECTIONS between
   three independent descriptions of the JVM test lanes -- the manifest, each
   namespace's own ns metadata, and the `*_test.clj` files on disk -- and that
   the runner REFUSES, by typed message, a namespace with no lane declaration.

   Why all three and not just the manifest: a manifest alone drifts silently.
   A namespace deleted from the manifest simply stops running, and the suite
   goes GREEN with less in it -- the failure mode this repo has already paid
   for once (round one's `mcp-formatter-test` is required by no runner and no
   Make target, and nothing noticed). Set equality in both directions is the
   refusal-kind pattern: absence is as loud as presence."
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

(deftest excluded-entries-are-real-and-carry-a-reason
  (doseq [[s reason] lm/excluded]
    (is (contains? @on-disk s) (str "excluded namespace " s " is not on disk"))
    (is (and (string? reason) (>= (count reason) 20))
        (str "excluded namespace " s " must name WHY it is in no lane"))
    (is (not (contains? lm/manifest s))
        (str s " is both excluded and in the manifest"))))

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
    (is (= 36 (count (lm/namespaces-for :fast))))
    (is (= 4 (count (lm/namespaces-for :integration))))
    (is (= 11 (count (lm/namespaces-for :battery))))
    (is (= 51 (count lm/manifest))
        "round one's 49 measured namespaces plus the two round-two witnesses")))
