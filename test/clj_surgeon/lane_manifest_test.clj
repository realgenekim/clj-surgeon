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
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]
   [rewrite-clj.zip :as z]))

;; RATCHET (2026-09-04, inb-9483a4): every fixture directory this namespace
;; creates is tracked and swept, on failure as well as on success.
(def ^:private temp-roots (atom []))
(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots))

(defn- temp-dir
  [prefix]
  (str (tmp-leak/track!
         temp-roots
         (java.nio.file.Files/createTempDirectory
           prefix (into-array java.nio.file.attribute.FileAttribute [])))))

(def ^:private test-root (io/file "test"))

(defn- test-source-files
  "Every `*_test.clj[c]` source file under `root`. Parameterised so the census
   witnesses below can drive the SAME discovery over a fixture tree in a temp
   directory -- a witness that can only run against the live tree cannot be
   made to go red on demand."
  ([] (test-source-files test-root))
  ([root]
   (->> (file-seq (io/file root))
        (filter #(.isFile ^java.io.File %))
        (filter #(re-find #"_test\.cljc?$" (.getName ^java.io.File %)))
        sort)))

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

(defn- scan-on-disk
  "ns symbol -> {:file f :lane <declared or nil>} for every test source file
   under `root`. THE TREE'S OWN ANSWER to what exists and what lane it claims,
   read at test time from the files themselves."
  [root]
  (into {}
        (keep (fn [f]
                (let [form (first-form f)]
                  (when-let [s (ns-sym-of form)]
                    [s {:file (.getPath ^java.io.File f) :lane (declared-lane form)}]))))
        (test-source-files root)))

(def ^:private on-disk
  "ns symbol -> {:file f :lane <declared or nil>} for every test source file."
  (delay (scan-on-disk test-root)))

(def ^:private bb-lane
  "The babashka lane's namespaces, read out of `test/run_all.clj` rather than
   restated here -- a second copy of that list would be the very drift this
   witness exists to catch."
  (delay
    (set (map symbol
              (re-seq #"clj-surgeon\.[a-z0-9.\-]+-test"
                      (slurp (io/file "test" "run_all.clj")))))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-015
;; INTENT: TEST-ISO-015
;;
;; THE CENSUS IS DERIVED FROM THE TREE; A COUNT IS ONLY A FLOOR.
;;
;; Until 2026-09-09 the lane census below was four pinned integers
;; (`(is (= 55 (count (lm/namespaces-for :fast))))` and three siblings). Every
;; branch that added a test namespace had to bump a number that says nothing
;; about WHICH namespace, and on 2026-09-08 the failure mode that shape
;; guarantees arrived: two branches each moved the fast pin 53 -> 54 for a
;; DIFFERENT namespace, the two literals were textually equal, git merged them
;; without a conflict, and the merged census was a namespace short while the
;; number still read as agreement. A count cannot distinguish "the same 54" from
;; "a different 54"; a set can, and the tree already knows the answer.
;;
;; So the expectation is DERIVED at test time from the same three sources the
;; manifest claims to describe -- the `*_test.clj` files on disk, each file's own
;; `{:lane ...}` ns metadata, and the manifest itself -- and compared as SETS in
;; both directions, naming the members on each side. NO COUNT IS ASSERTED, not
;; even as a `>=` floor: a floor at a historical count is the same shared number
;; under a weaker operator -- it still has to be argued about at a merge, and it
;; still blesses a corpus nobody re-derived. The one admissible guard is
;; NON-EMPTINESS, because an empty derivation would make every set comparison
;; above agree with itself.
;; ---------------------------------------------------------------------------

(defn- namespaces-declaring
  "The namespaces in `scanned` whose OWN ns metadata declares `lane`."
  [scanned lane]
  (set (for [[s info] scanned :when (= lane (:lane info))] s)))

(defn- census-diff
  "Set difference in BOTH directions between a set DERIVED from the tree and the
   set a census DECLARES. nil when they agree -- otherwise `:missing` (in the
   tree, absent from the census) and `:extra` (declared by the census, absent
   from the tree). Both directions, because absence must be as loud as presence:
   a namespace deleted from a census simply stops running and the suite goes
   green with less in it."
  [derived declared]
  (let [derived (set derived)
        declared (set declared)
        missing (vec (sort (remove declared derived)))
        extra (vec (sort (remove derived declared)))]
    (when (or (seq missing) (seq extra))
      {:missing missing :extra extra})))

(defn- census-diff-message
  "The failure text for a `census-diff`: names the subject, both differences and
   the remedy. Safe on nil, because `clojure.test` evaluates an `is` message
   whether or not the assertion failed."
  [subject diff]
  (str subject ": the census and the tree disagree. "
       "In the tree but MISSING from the census (" (count (:missing diff)) "): "
       (if (seq (:missing diff)) (str/join ", " (:missing diff)) "none")
       ". Declared by the census but ABSENT from the tree ("
       (count (:extra diff)) "): "
       (if (seq (:extra diff)) (str/join ", " (:extra diff)) "none")
       ". Add or remove the NAMED member -- never re-pin a count."))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-001
;; ---------------------------------------------------------------------------

(deftest every-manifest-entry-exists-on-disk
  (testing "Every historical bb member has a budgeted cadence; fast excludes battery"
    (let [bb-namespaces (requiring-resolve 'clj-surgeon.battery-parallel-runner/bb-namespaces)
          suite-namespaces (requiring-resolve 'clj-surgeon.battery-parallel-runner/suite-namespaces)
          defaults @(requiring-resolve 'clj-surgeon.ns-isolation/lane-default-budget-ms)]
      (is (= (set (lm/namespaces-for :fast)) (set (suite-namespaces "fast"))))
      (doseq [n (bb-namespaces)]
        (is (pos-int? (get defaults (lm/lane-of n))) (str n " requires a cadence budget")))))
  (testing "manifest -> disk: no phantom entries"
    (let [missing (sort (remove @on-disk (keys lm/manifest)))]
      (is (empty? missing)
          (str "lane manifest names " (count missing)
               " namespace(s) with no test source file on disk: "
               (str/join ", " missing)))))
  ;; @spec TEST-ISO-001 -- an explicit runtime for every namespace, independent of cadence.
  (testing "every discovered test namespace has a closed runtime declaration"
    (let [runtimes @(requiring-resolve 'clj-surgeon.lane-manifest/namespace-runtimes)]
      (is (= 159 (count runtimes)))
      (is (= (set (keys @on-disk)) (set (keys runtimes))))
      (is (= #{:bb :jvm} (set (vals runtimes))))
      (is (= :bb (get runtimes 'clj-surgeon.forms-test)))
      (is (= :jvm (get runtimes 'clj-surgeon.mcp-http-server-test)))))
  ;; @spec TEST-ISO-016 -- independent sample oracle, including planted defects.
  (testing "six controls and conservative two-sd clearance govern paired assignments"
    (let [bad (fn [runtimes measurements]
                (set (for [[n {:keys [jvm bb conservative-ratio] :as m}] measurements
                           :when (or (< (:n m 0) 6)
                                     (some #(or (< (:n % 0) 6) (< (count (:walls-ms %)) 6)) [jvm bb])
                                     (and (= :bb (get runtimes n))
                                          (> (or conservative-ratio ##Inf) 2.0)))]
                       n)))
          sample (fn [wall] {:n 6 :walls-ms (vec (repeat 6 wall)) :mean-ms (double wall) :sd-ms 0.0})
          at {:n 6 :jvm (sample 100) :bb (sample 200) :conservative-ratio 2.0}
          slow (assoc at :bb (sample 201) :conservative-ratio 2.01)
          name 'fixture/runtime-test
          stats (fn [walls]
                  (let [n (count walls)
                        mean (/ (double (reduce + walls)) n)]
                    [mean (Math/sqrt (/ (reduce + (map #(Math/pow (- % mean) 2) walls)) (dec n)))]))]
      (let [paired (set (:paired (edn/read-string
                                   (slurp "docs/observations/2026-09-12-bbtower-block-b/attempt20/baseline.edn"))))
            actual (set (keys lm/runtime-measurements))]
        (is (= paired actual)
            (str "TEST-ISO-016 paired scope changed: missing " (remove actual paired)
                 "; added " (remove paired actual))))
      (is (empty? (bad lm/namespace-runtimes lm/runtime-measurements))
          (str "TEST-ISO-016: " (bad lm/namespace-runtimes lm/runtime-measurements)))
      (is (= #{name} (bad {name :bb} {name slow})))
      (doseq [runtime [:bb :jvm]
              short [(assoc at :n 5)
                     (assoc-in at [:jvm :n] 5)
                     (assoc-in at [:bb :walls-ms] [200 200 200 200 200])]]
        (is (= #{name} (bad {name runtime} {name short})))
        (is (= :jvm (lm/measured-runtime :bb short))))
      (is (= :bb (lm/measured-runtime :bb at)))
      (is (= :jvm (lm/measured-runtime :bb slow)))
      (is (= :jvm (lm/measured-runtime :jvm at)))
      (is (= :jvm (lm/measured-runtime :bb (assoc at :contract-failure "defect"))))
      (is (= :jvm (lm/measured-runtime :bb (assoc-in at [:bb :sd-ms] 1.0))))
      (is (= :jvm (lm/measured-runtime :bb (assoc-in at [:jvm :sd-ms] 1.0))))
      (is (= :bb (lm/measured-runtime :bb
                   (assoc at :jvm (assoc (sample 1) :sd-ms 2.0) :bb (sample 2)))))
      (is (= :bb (lm/measured-runtime :bb nil)))
      (doseq [[n runtime] lm/unmeasured-runtimes]
        (is (= runtime (if (lm/bb-ineligibilities n) :jvm (lm/portability-runtimes n))) (str n)))
      (doseq [[n {:keys [jvm bb conservative-ratio] :as measurement}] lm/runtime-measurements]
        (is (= (if (lm/bb-ineligibilities n) :jvm (:runtime measurement))
               (lm/namespace-runtimes n)) (str n " assignment after capability admission"))
        (doseq [[runtime arm] [[:jvm jvm] [:bb bb]]]
          (let [rows (mapv #(edn/read-string (slurp %)) (:logs arm))
                walls (mapv :elapsed-ms rows)
                [mean sd] (stats walls)]
            (is (= (:n arm) (count rows) (count (:walls-ms arm))) (str n " " runtime))
            (is (= (count rows) (count (set (:logs arm))) (count (set (map :tmpdir rows))))
                (str n " " runtime " requires distinct run receipts"))
            (is (= walls (:walls-ms arm)) (str n " " runtime " receipt walls"))
            (is (every? #(= [n runtime] [(:namespace %) (:runtime %)]) rows) (str n))
            (when (= :bb (lm/namespace-runtimes n))
              (is (every? #(and (pos? (get-in % [:result :test] 0))
                                (zero? (+ (get-in % [:result :fail] 0) (get-in % [:result :error] 0)))) rows)
                  (str n " cannot use failed controls to certify bb")))
            (is (< (Math/abs (- mean (:mean-ms arm))) 1e-9) (str n " mean"))
            (is (< (Math/abs (- sd (:sd-ms arm))) 1e-9) (str n " sd"))))
        (is (= conservative-ratio
               (/ (+ (:mean-ms bb) (* 2 (:sd-ms bb)))
                  (max 1.0 (- (:mean-ms jvm) (* 2 (:sd-ms jvm)))))) (str n))))))

;; @spec DATACODE-ROWS-001
(deftest runtime-evidence-is-consumed-and-receipts-are-required
  (let [validate (ns-resolve 'clj-surgeon.lane-manifest 'validate-runtime-evidence!)
        evidence-path (ns-resolve 'clj-surgeon.lane-manifest 'runtime-evidence-path)
        rows lm/runtime-measurements]
    (is (some? validate) "the fold and manifest share the evidence admission value")
    (doseq [runtime [:jvm :bb]]
      (let [n (first (keys rows))
            missing (update-in rows [n runtime] dissoc :logs)
            r (try ((or validate identity) missing) nil
                   (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= :missing-evidence-receipts (:error-type r)) (str n " " runtime))
        (is (= n (:namespace r)))))
    (is (some? evidence-path) "measured facts have a data source, not a quoted source copy")
    (when validate
      (let [n (first (keys rows))]
        (doseq [bad [(update-in rows [n :jvm :mean-ms] inc)
                     (update-in rows [n :conservative-ratio] inc)
                     (update-in rows [n :runtime] {:bb :jvm :jvm :bb})]]
          (is (= :invalid-runtime-evidence
                 (:error-type (try (validate bad) nil
                                   (catch clojure.lang.ExceptionInfo e (ex-data e)))))))))
    (when evidence-path
      (let [evidence (edn/read-string (slurp @evidence-path))]
        (is (= evidence rows))
        (doseq [[n row] evidence]
          (is (= (if (lm/bb-ineligibilities n) :jvm (:runtime row))
                 (lm/namespace-runtimes n)) (str n " cannot disagree with evidence"))))))
  (let [source (slurp "docs/observations/2026-09-12-bbtower-block-b/attempt20/fold.clj")]
    (is (not (str/includes? source "*** Begin Patch"))
        "the statistical fold must not manufacture source edits")))

;; @spec DATACODE-ROWS-001
(deftest runtime-evidence-binds-statistics-to-receipt-files
  ;; Opus F1: internally consistent 6ms -> 9000ms forgery, receipts untouched.
  (let [n 'clj-surgeon.battery-ledger-test
        row (get lm/runtime-measurements n)
        forged (-> row
                   (assoc :runtime :jvm)
                   (assoc-in [:bb :walls-ms] (vec (repeat 6 9000)))
                   (assoc-in [:bb :mean-ms] 9000.0)
                   (assoc-in [:bb :sd-ms] 0.0)
                   (assoc :conservative-ratio
                          (/ 9000.0 (- (get-in row [:jvm :mean-ms])
                                       (* 2 (get-in row [:jvm :sd-ms]))))))]
    (is (= lm/runtime-measurements (lm/validate-runtime-evidence! lm/runtime-measurements))
        "all 38 shipped rows / 456 receipts remain admitted")
    (doseq [[candidate kind field]
            [[forged :invalid-runtime-evidence [:bb :walls-ms]]
             [(assoc row :runtime :jvm) :invalid-runtime-evidence :runtime]
             [(assoc-in row [:bb :logs 0] "docs/no-such-datacode-receipt.edn")
              :missing-evidence-receipts [:bb :logs]]]]
      (let [r (try (lm/validate-runtime-evidence! {n candidate}) nil
                   (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= kind (:error-type r)) (pr-str r))
        (is (= n (:namespace r)) (pr-str r))
        (is (= field (:field r)) (pr-str r))))
    (let [receipts (into {} (for [rt [:jvm :bb]]
                              [rt (mapv #(edn/read-string (slurp %))
                                        (get-in row [rt :logs]))]))]
      (doseq [[candidate evidence field]
              [[(update-in row [:bb :mean-ms] inc) receipts [:bb :mean-ms]]
               [(update-in row [:bb :sd-ms] inc) receipts [:bb :sd-ms]]
               [(update row :conservative-ratio inc) receipts :conservative-ratio]
               [(update row :n inc) receipts :n]
               [(update-in row [:bb :n] inc) receipts [:bb :n]]
               [row (assoc-in receipts [:bb 0 :namespace] 'wrong.test) [:bb :namespace]]
               [row (assoc-in receipts [:bb 0 :runtime] :jvm) [:bb :runtime]]
               [row (assoc-in receipts [:bb 0 :elapsed-ms] 9000) [:bb :walls-ms]]]]
        (let [r (try (lm/validate-runtime-row! n candidate evidence) nil
                     (catch clojure.lang.ExceptionInfo e (ex-data e)))]
          (is (= :invalid-runtime-evidence (:error-type r)) (pr-str r))
          (is (= n (:namespace r)) (pr-str r))
          (is (= field (:field r)) (pr-str r)))))))

(deftest runtime-portability-controls-cover-every-assignment
  ;; @spec TEST-ISO-016 -- all cadences, independently of the 38 cost pairs.
  (let [n 'fixture/runtime-test
        passing (fn [runtime] {:namespace n :runtime runtime :status :passed :exit 0
                               :result {:test 1 :fail 0 :error 0}})
        controls {:jvm (passing :jvm) :bb (passing :bb)}]
    (is (nil? (lm/portability-refusal n controls)))
    (let [failed (assoc (passing :bb) :status :test-failed :exit 1
                        :result {:test 1 :fail 0 :error 1})
          limited (assoc controls :bb failed)
          registration {:reasons #{:sci-host-interop} :detail "FileLockImpl.release refused"}]
      (is (= :portable (:state (lm/portability-state n controls nil))))
      (is (= :bb-ineligible (:state (lm/portability-state n limited registration))))
      (doseq [bad [nil {} (assoc registration :reasons #{:unknown})
                   (assoc registration :reasons #{}) (assoc registration :detail "")]]
        (is (= :refused (:state (lm/portability-state n limited bad)))))
      (doseq [bb [(passing :bb) failed]]
        (is (= :refused (:state (lm/portability-state n
                                  {:jvm (assoc (passing :jvm) :exit 1) :bb bb}
                                  registration)))))
      (doseq [bad [nil (assoc failed :namespace 'wrong/test)
                   (assoc failed :runtime :jvm) (assoc failed :exit nil)]]
        (is (= :refused (:state (lm/portability-state n (assoc controls :bb bad) registration))))))
    (doseq [runtime [:jvm :bb]
            bad [nil
                 (assoc (passing runtime) :status :test-failed)
                 (assoc (passing runtime) :exit 1)
                 (assoc-in (passing runtime) [:result :fail] 1)
                 (assoc-in (passing runtime) [:result :error] 1)
                 (assoc-in (passing runtime) [:result :test] 0)
                 (assoc (passing runtime) :namespace 'wrong/test)
                 (assoc (passing runtime) :runtime :wrong)]]
      (let [refusal (lm/portability-refusal n (assoc controls runtime bad))]
        (is (= :non-portable-namespace (:error-type refusal)))
        (is (= n (:namespace refusal)))
        (is (contains? (:controls refusal) runtime))))
    (is (= :bb-load-incompatible
           (:reason (lm/portability-refusal n
                      {:bb-load {:status :load-failed :mode "load" :namespace n
                                 :runtime :bb :exit 1 :message "unsupported class"}})))))
  (is (= (set (keys lm/namespace-runtimes))
         (set (keys lm/namespace-runtime-controls))))
  (doseq [[n paths] lm/namespace-runtime-controls]
    (let [controls (into {}
                         (for [[runtime path] paths
                               :when (.isFile (io/file path))]
                           [runtime (assoc (edn/read-string (slurp path)) :receipt path)]))
          refusal (lm/portability-refusal n controls)
          state (lm/portability-state n controls (lm/bb-ineligibilities n))
          unsupported-jvm-only? (and (= :bb-load-incompatible (:reason refusal))
                                     (= :jvm (lm/namespace-runtimes n))
                                     (= :jvm (lm/portability-runtimes n))
                                     (= n (get-in controls [:bb-load :namespace]))
                                     (= :bb (get-in controls [:bb-load :runtime]))
                                     (not (str/blank? (get-in controls [:bb-load :message]))))]
      (when (= :bb-ineligible (:state state))
        (println "BB-INELIGIBLE" n ":jvm" (pr-str (select-keys state [:reasons :detail :probe])))
        (is (= :jvm (lm/namespace-runtimes n)) (str n)))
      (when unsupported-jvm-only?
        (println "BB-LOAD-EXCLUDED" n (pr-str (:control refusal))))
      (is (or (nil? refusal) unsupported-jvm-only?)
          (pr-str refusal)))))

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
    (let [{:keys [makefile-text]} (rm/repo-context)
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
      ;; @spec ALIAS-MIGRATION-003
      ;; Sol r10: a fresh historical battery receipt hid a red alias battery.
      (doseq [target ["make test" "make landing-gate"]
              namespace '[clj-surgeon.mcp-alias-migration-test
                          clj-surgeon.receipt-artifacts-boundary-test]]
        (is (contains? (:namespaces (rm/resolve-runner target (rm/repo-context))) namespace)
            (str target " must execute " namespace)))
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
  (testing "loaded metadata agrees; one conditional assertion per manifest row"
    ;; @spec TEST-ISO-015 -- loaded membership varies by process partition.
    ;; The predicate is unchanged; its assertion count cannot depend on which
    ;; other namespaces share this JVM. The runner checks every selected ns too.
    (doseq [[s lane] lm/manifest]
      (let [loaded (find-ns s)]
        (is (or (nil? loaded) (= lane (:lane (meta loaded))))
            (str s " when loaded must carry :lane " lane))))))

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
  (let [code-spellings (fn [source]
                         (->> (tree-seq node/inner? node/children (parser/parse-string-all source))
                              (filter #(= :token (node/tag %)))
                              (map node/string)
                              (remove #(re-find #"^#?\"" %))
                              (str/join " ")))
        offenders
        (sort-by first
                 (for [[s lane] lm/manifest
                       :when (= :fast lane)
                       :let [raw (slurp (:file (get @on-disk s)))
                             src (if (some #(re-find % raw) spawn-spellings)
                                   (code-spellings raw) raw)]
                       re spawn-spellings
                       :when (re-find re src)]
                   [s (str re)]))]
    (testing "SCI rejection fixture strings are data; real require and call forms stay visible"
      (is (not (some #(re-find % (code-spellings "(def rejected \"(require 'clojure.java.shell)\")")) spawn-spellings)))
      (doseq [source ["(ns example (:require [clojure.java.shell :as sh]))"
                      "(proc/process [\"bb\"])" "(ProcessBuilder. argv)"]]
        (is (some #(re-find % (code-spellings source)) spawn-spellings) source)))
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

;; @spec TEST-ISO-001
;; @spec TEST-ISO-015
(deftest the-partition-matches-round-ones-measurement
  (testing "each lane's membership is DERIVED from the tree, member by member"
    (doseq [lane lm/lanes]
      (let [diff (census-diff (namespaces-declaring @on-disk lane)
                              (lm/namespaces-for lane))]
        (is (nil? diff) (census-diff-message (str "lane " lane) diff)))))
  (testing "the manifest as a whole equals every lane the tree declares"
    (let [diff (census-diff (set (keep (fn [[s info]] (when (:lane info) s)) @on-disk))
                            (keys lm/manifest))]
      (is (nil? diff) (census-diff-message "manifest" diff))))
  (testing "discovery is non-empty, so a scan that stopped working fails loud"
    ;; The ONLY legitimate use of a count here. Not a floor against a historical
    ;; corpus -- a floor blesses the old shared number under `>=` and still has
    ;; to be argued about at every merge. Membership is settled above, member by
    ;; member; all that is left is that the scan found ANYTHING at all, because
    ;; an empty derivation would make every set comparison above pass vacuously.
    (is (seq @on-disk)
        (str "discovery found no test source files under " test-root
             " -- an empty scan makes every derived comparison above agree "
             "with itself"))
    (is (seq lm/manifest) "the manifest declares no namespaces at all")
    (doseq [lane lm/lanes]
      (is (seq (lm/namespaces-for lane))
          (str "lane " lane " is empty; a lane that runs nothing is a silent "
               "hole, not a partition")))))

;; @spec TEST-ISO-015
;; INTENT-TEST: TEST-ISO-015
(deftest a-namespace-in-the-tree-but-absent-from-the-census-is-named
  ;; RED ON DEMAND, in a FIXTURE tree under java.io.tmpdir -- never the live
  ;; one. The live assertions above can only be observed green; this drives the
  ;; same discovery and the same comparator over a tree we control, so the
  ;; failure they exist to catch is exhibited rather than asserted about.
  (let [root (temp-dir "surgeon-lane-census")
        pkg (io/file root "test" "clj_surgeon")]
    (.mkdirs pkg)
    (spit (io/file pkg "enrolled_test.clj")
          "(ns clj-surgeon.fixture-enrolled-test {:lane :fast})\n")
    (spit (io/file pkg "newcomer_test.clj")
          "(ns clj-surgeon.fixture-newcomer-test {:lane :fast})\n")
    (let [scanned (scan-on-disk (io/file root "test"))
          derived (namespaces-declaring scanned :fast)
          census '#{clj-surgeon.fixture-enrolled-test}
          diff (census-diff derived census)]
      (testing "the fixture discovery sees the tree the same way the live one does"
        (is (= 2 (count scanned)))
        (is (= '#{clj-surgeon.fixture-enrolled-test clj-surgeon.fixture-newcomer-test}
               derived)))
      (testing "a namespace the tree declares and the census omits is named"
        (is (= '[clj-surgeon.fixture-newcomer-test] (:missing diff)))
        (is (empty? (:extra diff)))
        (is (str/includes? (census-diff-message "lane :fast" diff)
                           "clj-surgeon.fixture-newcomer-test")))
      (testing "and a COUNT cannot see the defect a set does"
        ;; The 2026-09-08 merge in one fixture: same size, different members.
        ;; Equal counts is exactly the evidence git had when it merged two
        ;; different 54s into one.
        (let [swapped '#{clj-surgeon.fixture-enrolled-test clj-surgeon.fixture-ghost-test}
              swap-diff (census-diff derived swapped)]
          (is (= (count derived) (count swapped))
              "the counts agree -- which is why a count pin passes here")
          (is (= {:missing '[clj-surgeon.fixture-newcomer-test]
                  :extra '[clj-surgeon.fixture-ghost-test]}
                 swap-diff)
              "...while the derived witness names both sides of the swap"))))))

(defn- deftest-names
  "The FULLY QUALIFIED names of the `deftest` forms a namespace's source file
   declares, e.g. `clj-surgeon.foo-test/bar`. A SOURCE census, deliberately: it
   is the same answer for every box and every load, whereas assertion counts are
   context-sensitive (the round-two review measured 4,319 assertions summing the
   lanes separately and 4,323 running them together).

   NAMES, not a count. A count per namespace cannot see a rename or any
   same-count replacement -- delete one member, add another, the number is
   unchanged and the ratchet is green while the promise it protected is gone.
   Sol's round-two fence proved exactly that against the count ledger: renaming
   `mcp-paths-test`'s only deftest passed all six assertions."
  [ns-sym]
  (let [file (:file (get @on-disk ns-sym))]
    (into (sorted-set)
          (map (fn [[_ nm]] (symbol (str ns-sym) nm)))
          (re-seq #"(?m)^\(deftest\s+([^\s()\[\]{}]+)" (slurp file)))))

(defn- deftest-count
  "How many deftests `ns-sym` declares, derived from `deftest-names`."
  [ns-sym]
  (count (deftest-names ns-sym)))

(def ^:private adopted-since-round-one
  "Namespaces in a lane today that round one did NOT measure, each with the
   reason it exists. The per-namespace TEST COUNTS that used to live here are
   derived from the tree and recorded, one line per namespace, in
   `census-ledger-path`; what stays here is the REASON, which no derivation can
   recover. Keyed by namespace name, so two branches adopting different
   namespaces merge without touching the same line."
  '#{;; Former bb-only members: measured cadence adoption, attempt10/lane-moves.md.
     clj-surgeon.agent-routing-test
     clj-surgeon.alias-migration-test
     clj-surgeon.analyze-test
     clj-surgeon.cli-dispatch-test
     clj-surgeon.cljc-existing-ops-test
     clj-surgeon.cljc.analyze-test
     clj-surgeon.cljc.merge-test
     clj-surgeon.cljc.require-ops-test
     clj-surgeon.cljc.split-test
     clj-surgeon.diagnostic-delta-test
     clj-surgeon.edit-dsl-test
     clj-surgeon.edit-test
     clj-surgeon.edn-config-integration-test
     clj-surgeon.extract-header-test
     clj-surgeon.extract-test
     clj-surgeon.failure-report-test
     clj-surgeon.file-ops-test
     clj-surgeon.fix-declares-test
     clj-surgeon.forms-test
     clj-surgeon.help-test
     clj-surgeon.insertion-gap-test
     clj-surgeon.install-test
     clj-surgeon.intent-transaction-test
     clj-surgeon.jvm-error-test
     clj-surgeon.lens-query-test
     clj-surgeon.ls-tree-test
     clj-surgeon.memory-battery-test
     clj-surgeon.move-dependency-test
     clj-surgeon.move-test
     clj-surgeon.operation-algebra-test
     clj-surgeon.outermost-test
     clj-surgeon.outline-test
     clj-surgeon.owner-hypotheses-test
     clj-surgeon.parser-admission-test
     clj-surgeon.partition-all-test
     clj-surgeon.platform-selector-test
     clj-surgeon.recovery-test
     clj-surgeon.relation-census-test
     clj-surgeon.rename-test
     clj-surgeon.show-form-test
     clj-surgeon.structural-lens-test
     clj-surgeon.syntax-var-refs-test
     clj-surgeon.tmp-leak-support-test
     clj-surgeon.worktree-lifecycle-cli-test
     clj-surgeon.worktree-lifecycle-io-test
     clj-surgeon.worktree-lifecycle-test
     clj-surgeon.xray-test
     clj-surgeon.receipt-booleans-test ; Cross-verb false-boolean receipt ratchet.
     clj-surgeon.rename-alias-receipt-test ; Disk-derived receipt evidence.
     clj-surgeon.insert-forms-test ; Consolidated span witnesses.
     clj-surgeon.splice-envelope-test ; Shared envelope witnesses.
     clj-surgeon.rename-alias-performance-test ; Generated 4000-line planner bound.
     clj-surgeon.rename-alias-test ; Fixed alias reader roles, E4 and transaction contract.
     clj-surgeon.rename-alias-parity-test ; Actual request-file transport parity.
     clj-surgeon.insert-forms-parity-test ; insert_forms v1 contract witness.
     clj-surgeon.insert-forms-receipt-test ; insert_forms v1 contract witness.
     clj-surgeon.receipt-artifacts-boundary-test ; Sol r10 + two Row 5 real-process witnesses (battery).
     clj-surgeon.namespace-split-test ; Batch 5 adds six ns/footprint/lint/encoding witnesses; derived by deftest-count.
     clj-surgeon.namespace-split-warm-test ; Batch 5 adds executed load/failed destination facts to the real nREPL matrix.
     clj-surgeon.mcp-namespace-split-test ; Batch 5 adds text ordering, absent-probe honesty and malformed UTF-8 refusal.
     clj-surgeon.split-proof-gate-test ; Batch 3 pure status/state matrix, plus Sol's a9da4344 temp-root admission and receipt-ceiling witnesses.
     clj-surgeon.split-proof-gate-boundary-test ; Batch 3 detached worker and caller-exit boundaries, plus Sol's a9da4344 worker-identity boundary.
     clj-surgeon.cell-b-oracle-test ; B07: shell lint mutation test and independent partial-preservation mutants; battery (Python subprocess).
     clj-surgeon.mcp-expect-guard-test ; `expect` is a guard on both write routes, not discarded bookkeeping (dogfood-3, 2026-09-07).
     clj-surgeon.outline-corpus-integration-test ; MOVED: full repository differential out of the bounded fast namespace.
     clj-surgeon.mission-candidate-race-test ; Completion-order delivery, bounded cancellation and retained results.
     clj-surgeon.mission-events-test ; Public completion events and isolated logging failure.
     clj-surgeon.mission-phase-events-test ; Actual phase receipts, identity and isolated logging failure.
     clj-surgeon.mission-provider-fallback-events-test ; Actual dispatched fallback, thread context and isolated logging.
     clj-surgeon.mission-display-test ; Add historical nested refusal and incompatible-example witnesses.
     clj-surgeon.mission-fallback-test ; Explicit report, actual event write and unchanged proof.
     clj-surgeon.mission-git-identity-test ; Explicit seat author/committer survive subprocess sanitization.
     clj-surgeon.mission-git-submodule-test ; Git config cannot hide staged gitlinks from scope guard.
     clj-surgeon.mission-publication-test ; Durable publication intent blocks silent source undo.
     clj-surgeon.mission-git-test ; Pure Git provenance contract.
     clj-surgeon.mission-git-boundary-test ; Git tree and staged path boundaries.
     clj-surgeon.mission-git-fence-test ; Identity and refusal witnesses.
     clj-surgeon.mission-git-process-test ; Bounded subprocess lifecycle.
     clj-surgeon.mission-git-ledger-test ; Saved receipt authority.
     clj-surgeon.mission-commit-cli-test ; Actual public command behavior.
     clj-surgeon.mission-usage-test ; Observed legacy/attempt usage and unknowns.
     clj-surgeon.mission-typist-executor-admission-test ; Unsupported adapter refused before readiness.
     clj-surgeon.mission-usage-executor-test ; Saved success/refusal usage snapshots.
     clj-surgeon.mission-run-test ; One-process saved plan, refusal and CLI boundaries.
     clj-surgeon.mission-test ; Adopt existing ledger orphan plus owner-forms routing and recovery witnesses.
     clj-surgeon.mission-typist-test ; Pure routing/dossier and frozen generation policy boundaries.
     clj-surgeon.mission-candidate-test ; Frozen span lowering boundaries.
     clj-surgeon.mission-plain-forms-test ; Bounded raw definition decoding and actual escaping failure.
     clj-surgeon.mission-forms-test ; Owner identity, protected syntax and lost-comment refusal.
     clj-surgeon.mission-forms-source-test ; Strict comment text/attachment, whitespace identity and owner sentinel.
     clj-surgeon.mission-typist-executor-test ; Add candidate diagnostic survival to proof/commit/undo and saved fallback forwarding.
     clj-surgeon.battery-ledger-test ; TEST-ISO-009a/b: add strict archive classification and preserved failure/audit authority.
     clj-surgeon.battery-parallel-test ; TEST-ISO-013: the battery lane run as N JVM lanes -- schedule, lane-failure classifier, shard fold, prerequisite DAG. TEST-ISO-014 (5cdd5dcc) adds two: launcher-matrix-cells-remain-independently-shardable and grouped-shards-retain-measured-per-deftest-walls.
     clj-surgeon.require-change-test ; Pure standalone require intent and strict natural-layout refusal witnesses.
     clj-surgeon.require-change-boundary-test ; Actual CLI/profile processes, confined publication, independent oracle and undo.
     clj-surgeon.fast-lane-isolation-test ; TEST-ISO-006's witness (round two) + round five's finding-3 fixture-root scan
     clj-surgeon.lane-manifest-test ; TEST-ISO-001's witness (round two) + round three's exclusion, arithmetic and rename pins + round five's four membership witnesses and two landing-gate witnesses + TEST-ISO-015's fixture-tree census witness (2026-09-09), a-namespace-in-the-tree-but-absent-from-the-census-is-named
     clj-surgeon.mcp-formatter-test ; the adopted orphan (round three)
     clj-surgeon.mcp-feature-thread-test ; the trunk's `feature_thread` verb, adopted at round five's MCP/main merge
     clj-surgeon.mcp-feature-thread-sed-test ; MOVED, not new (round five): its one `sed` cross-check, out of :fast into :battery
     clj-surgeon.mcp-inspect-cold-job-test ; MOVED, not new (round five): the one inspect-tool test that drives /bin/sh, out of :fast into :battery
     clj-surgeon.ns-isolation-test ; TEST-ISO-002/003/004/005/007/010's witnesses (round four) + round five's four spawn-ledger witnesses
     clj-surgeon.helper-extraction-test ; MCP-OP-HELPER's pure planner witnesses, enrolled into :fast when the planner went green (it requires only the planner, the fixture and clojure.test, and spawns nothing)
     clj-surgeon.telemetry-events-test ; TELEMETRY-EVENTS-001's witnesses: the box-wide JSONL ledger the public MCP fns append to as a side effect (2026-09-06, the night the hourly watch reported four figures while a dozen calls landed in launcher-chosen roots it never read)
     clj-surgeon.mcp-helper-extraction-test}) ; MCP-OP-HELPER's boundary witnesses, :battery because they spawn babashka children to prove fixture trees LOAD and drive real execute! transactions

(def ^:private census-ledger-path
  "The deftest ledger: ONE LINE PER FULLY QUALIFIED DEFTEST NAME, sorted.

   Two shapes were rejected before this one, and both rejections are the reason
   it looks like this. A repository-wide TOTAL is the merge-conflicting scalar
   class -- two branches add disjoint witnesses, both write the same next number,
   git merges the equal literals without a conflict, and the total is wrong; the
   comment history above `the-partition-matches-round-ones-measurement` records
   that happening three separate times. A ledger of `namespace -> count` fixes
   the merge but not the promise: it fails by name only at NAMESPACE grain, so a
   rename -- delete one member, add another, same count -- passes (Sol's
   round-two fence renamed `clj-surgeon.mcp-paths-test`'s only deftest and the
   ratchet stayed green through all six assertions).

   A ledger is admissible only when it is line-wise at the granularity of the
   members it promises to preserve AND names them. So: one deftest per line, the
   name fully qualified and stable, sorted. Two branches adding tests touch two
   different lines; a deleted test is a NAMED line that disappears.

   Regenerate ONLY through the direct entrance -- never inside make, see
   `regenerate-decision`:

     CENSUS_REGENERATE=1 clojure -M:clj-surgeon/test-deps -e \"(require 'clj-surgeon.lane-manifest-test 'clojure.test) (clojure.test/test-vars [#'clj-surgeon.lane-manifest-test/the-corpus-only-ever-grows-and-the-arithmetic-is-shown])\"

   and READ THE DIFF before committing it: a removed line is the deletion this
   ledger exists to make loud, not a line to re-bless."
  "test/clj_surgeon/deftest_census.edn")

(def ^:private regenerate-entrance
  "The exact command that may rewrite the ledger. Quoted in the refusal so a
   reader never has to guess what the permitted entrance is."
  (str "CENSUS_REGENERATE=1 clojure -M:clj-surgeon/test-deps -e \"(require "
       "'clj-surgeon.lane-manifest-test 'clojure.test) (clojure.test/test-vars "
       "[#'clj-surgeon.lane-manifest-test/"
       "the-corpus-only-ever-grows-and-the-arithmetic-is-shown])\""))

(defn- regenerate-decision
  "What the regenerate entrance does under environment `env`, as a pure function
   of the environment so it can be witnessed without touching the real one:

     :skip               regeneration was not requested;
     :refuse-under-make  requested, but this process was launched BY make;
     :write              requested through the direct entrance.

   CENSUS-REGENERATE-001 (Sol fence round two): a caller-supplied
   `CENSUS_REGENERATE=1` is EXPORTED into make's recipes, so `CENSUS_REGENERATE=1
   make test` reached this writer through landing-gate -> mcp-test -> the fast
   lane and could rewrite the checked-in oracle DURING THE GATE -- an oracle a
   gate can rewrite is not an oracle. `MAKELEVEL` and `MAKEFLAGS` are set by make
   in every recipe's environment (MAKELEVEL is `0` in the outermost one, which is
   why PRESENCE is the test and not truthiness), so their presence is the signal
   that this is not the deliberate, review-the-diff entrance. The Makefile itself
   is not touched: the refusal lives here, at the writer."
  [env]
  (cond
    (not= "1" (get env "CENSUS_REGENERATE")) :skip
    (or (contains? env "MAKELEVEL") (contains? env "MAKEFLAGS")) :refuse-under-make
    :else :write))

(defn- regenerate-refusal
  [env]
  (str "census-regenerate-refused: CENSUS_REGENERATE=1 was requested inside a "
       "make recipe (" (str/join ", " (sort (filter #{"MAKELEVEL" "MAKEFLAGS"}
                                                    (keys env))))
       " present). The ledger is the oracle this gate checks; a gate that can "
       "rewrite its own oracle proves nothing, so NOTHING WAS WRITTEN. Regenerate "
       "deliberately, outside make, and read the diff:\n  " regenerate-entrance))

(defn- environment
  "The process environment as a plain map, so `regenerate-decision` stays pure."
  []
  (into {} (System/getenv)))

(defn- derived-census
  "The fully qualified name of every deftest the manifest's namespaces declare."
  []
  (into (sorted-set) (mapcat deftest-names) (keys lm/manifest)))

(defn- write-census-ledger!
  "Writes `census` to `census-ledger-path`, one fully qualified deftest per line,
   sorted. Reached ONLY through `regenerate-decision` returning `:write`."
  [census]
  (spit census-ledger-path
        (str ";; Deftest census -- DERIVED, regenerated, never hand-edited.\n"
             ";; One FULLY QUALIFIED deftest per line: two branches adding tests\n"
             ";; touch two different lines, and a deleted test is a named line\n"
             ";; that disappears rather than a number that stays plausible.\n"
             ";; Regenerate: see clj-surgeon.lane-manifest-test/census-ledger-path.\n"
             "#{"
             (str/join "\n  " census)
             "}\n")))

(defn- census-ledger-diff
  "Named differences between the tree's deftests and the checked-in ledger:
   `:added` are declared in a lane but absent from the ledger, `:removed` are in
   the ledger and no longer declared anywhere. A RENAME appears as one of each,
   which is the whole reason the ledger holds names."
  [derived ledger]
  (let [added (vec (sort (remove ledger derived)))
        removed (vec (sort (remove derived ledger)))]
    (when (or (seq added) (seq removed))
      {:added added :removed removed})))

(defn- census-ledger-message
  [diff]
  (str "the tree and " census-ledger-path " disagree. "
       "Declared in a lane but NOT in the ledger (" (count (:added diff)) "): "
       (if (seq (:added diff)) (str/join ", " (:added diff)) "none")
       ". In the ledger but NO LONGER DECLARED (" (count (:removed diff)) "): "
       (if (seq (:removed diff)) (str/join ", " (:removed diff)) "none")
       ". After regenerating, READ THE LEDGER DIFF before committing it"
       ". A removed name is a deleted test -- say why, or restore it. A removed "
       "AND an added name together is a rename, which the count ledger this "
       "replaced could not see. Then regenerate: " regenerate-entrance))

;; @spec TEST-ISO-015
;; INTENT-TEST: TEST-ISO-015
(deftest the-regenerate-entrance-refuses-inside-make
  ;; CENSUS-REGENERATE-001. A pure decision over an environment MAP, so the rule
  ;; is witnessed here rather than only in whatever environment this run happens
  ;; to have. MAKELEVEL is "0" in make's outermost recipe -- presence, never
  ;; truthiness, is the signal.
  (testing "the direct entrance writes"
    (is (= :write (regenerate-decision {"CENSUS_REGENERATE" "1"}))))
  (testing "make's own environment refuses, however it is spelled"
    (doseq [env [{"CENSUS_REGENERATE" "1" "MAKELEVEL" "0"}
                 {"CENSUS_REGENERATE" "1" "MAKELEVEL" "1"}
                 {"CENSUS_REGENERATE" "1" "MAKEFLAGS" ""}
                 {"CENSUS_REGENERATE" "1" "MAKEFLAGS" "w" "MAKELEVEL" "2"}]]
      (is (= :refuse-under-make (regenerate-decision env)) (pr-str env))))
  (testing "the refusal names the subject, the reason and the permitted entrance"
    (let [msg (regenerate-refusal {"CENSUS_REGENERATE" "1" "MAKELEVEL" "0"})]
      (is (str/includes? msg "census-regenerate-refused:"))
      (is (str/includes? msg "MAKELEVEL"))
      (is (str/includes? msg "NOTHING WAS WRITTEN"))
      (is (str/includes? msg regenerate-entrance))))
  (testing "a mismatch sends the reviewer back to the named ledger diff"
    (let [msg (census-ledger-message {:added ['fixture/new]
                                      :removed ['fixture/old]})]
      (is (str/includes? msg "READ THE LEDGER DIFF"))))
  (testing "no request, no write -- inside make or outside it"
    (is (= :skip (regenerate-decision {})))
    (is (= :skip (regenerate-decision {"MAKELEVEL" "0"})))
    (is (= :skip (regenerate-decision {"CENSUS_REGENERATE" "0"})))))

;; @spec TEST-ISO-001
;; @spec TEST-ISO-015
(deftest the-corpus-only-ever-grows-and-the-arithmetic-is-shown
  ;; PARTITIONING MUST NEVER TURN INTO DROPPING. Round one MEASURED 865 tests
  ;; across the 49 namespaces in `round-one-jvm-namespaces`; every partition,
  ;; move and adoption since then has to keep every one of them running
  ;; somewhere. Two things prove it, and NEITHER is a shared number:
  ;;
  ;;   1. the per-DEFTEST ledger below -- a deletion is a NAMED line that
  ;;      disappeared, a rename is one line out and one line in, and a move
  ;;      between namespaces is neither, because the name is qualified by the
  ;;      namespace that declares it;
  ;;   2. the arithmetic, computed entirely from the tree: the round-one half
  ;;      plus the adopted half must be the whole manifest, as sets AND as sums,
  ;;      so a namespace cannot be counted twice or not at all.
  (let [derived (derived-census)
        env (environment)
        decision (regenerate-decision env)]
    (when (= :write decision)
      (write-census-ledger! derived)
      (println "CENSUS_REGENERATE=1: wrote" (count derived) "deftests to"
               census-ledger-path))
    (testing "regeneration never happens inside a make recipe"
      (is (not= :refuse-under-make decision) (regenerate-refusal env)))
    (testing "the corpus is not empty, so nothing below passes vacuously"
      (is (seq derived))
      (let [barren (sort (remove (comp seq deftest-names) (keys lm/manifest)))]
        (is (empty? barren)
            (str "namespace(s) declaring no deftests at all: "
                 (str/join ", " barren)))))
    (testing "every deftest in the tree is a named line in the ledger"
      (let [ledger (edn/read-string (slurp census-ledger-path))
            diff (census-ledger-diff derived ledger)]
        (is (set? ledger) (str census-ledger-path " must hold a set of names"))
        (is (nil? diff) (census-ledger-message diff))))
    (testing "the two halves are the whole manifest, as sets"
      (let [diff (census-diff (set (keys lm/manifest))
                              (into (set round-one-jvm-namespaces)
                                    adopted-since-round-one))]
        (is (nil? diff)
            (census-diff-message
              (str "round-one + adopted vs the manifest -- a namespace joined or "
                   "left the corpus without a reason at the pin")
              diff))))
    (testing "and as sums, all three derived from the tree"
      (let [r1 (reduce + (map deftest-count round-one-jvm-namespaces))
            adopted (reduce + (map deftest-count adopted-since-round-one))
            total (count derived)]
        (is (= total (+ r1 adopted))
            (str total " != " r1 " + " adopted
                 " -- a namespace is being counted twice or not at all"))))))

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
  "Test Var, ordinal and declared temporal purpose are identity; lines are observations."
  [{:owner 'clj-surgeon.census-pool-test/the-pool-is-bounded-and-outlives-nothing
    :ordinal 1 :purpose :poll :call '(Thread/sleep 20)
    :reason "Bounded worker-death poll; succeeds immediately, fails after 100 attempts."}
   {:owner 'clj-surgeon.census-pool-test/the-pool-is-bounded-and-outlives-nothing
    :ordinal 2 :purpose :spaced-stimulus :call '(Thread/sleep 5)
    :reason "Spread work across threads to witness the scheduling assertion."}
   {:owner 'clj-surgeon.scope-stream-test/the-reader-drops-each-source-when-its-callback-returns
    :ordinal 1 :purpose :poll :call '(Thread/sleep 10)
    :reason "Bounded collection-pressure poll for weak-reference release."}
   {:owner 'clj-surgeon.mcp-tool-test/direct-change-returns-after-hot-proof-and-publishes-a-cold-job
    :ordinal 1 :purpose :poll :call '(Thread/sleep 10)
    :reason "Poll the verification job until complete or the attempt ceiling."}
   {:owner 'clj-surgeon.mcp-hot-verify-test/hot-verification-deadline-is-not-reset-by-non-terminal-responses
    :ordinal 1 :purpose :spaced-stimulus :call '(Thread/sleep 50)
    :reason "Pump responses within the fixed verification deadline to prove it never resets."}])

;; @spec DATACODE-SLEEP-001
(defn sleep-sites [file source]
  (let [start (z/of-string source {:track-position? true})
        lib (second (z/sexpr start))
        ancestors (fn [loc] (take-while some? (iterate z/up loc)))
        quoted? (fn [loc]
                  (some #(or (#{:quote :syntax-quote :uneval} (z/tag %))
                             (and (= :list (z/tag %))
                                  (#{'quote 'clojure.core/quote} (first (z/sexpr %)))))
                        (rest (ancestors loc))))
        sites (for [loc (take-while #(not (z/end? %)) (iterate z/next start))
                    :when (= :list (z/tag loc))
                    :when (and (#{'Thread/sleep 'java.lang.Thread/sleep} (some-> loc z/down z/sexpr))
                               (not (quoted? loc)))
                    :let [form (z/sexpr loc)
                          top (last (take-while #(not= :forms (z/tag %)) (ancestors loc)))
                          owner-form (z/sexpr top) owner-name (second owner-form)]]
                {:owner (symbol (str lib) (str owner-name))
                 :test-owner? (= 'deftest (first owner-form))
                 :purpose (:temporal-purpose (meta (first form)))
                 :call form :file file :line (first (z/position loc))})]
    (:rows (reduce (fn [{:keys [counts rows]} site]
                     (let [owner (:owner site) ordinal (inc (get counts owner 0))]
                       {:counts (assoc counts owner ordinal)
                        :rows (conj rows (assoc site :ordinal ordinal))}))
                   {:counts {} :rows []} sites))))

;; @spec DATACODE-SLEEP-001
(defn sleep-pin-violations [pins sites]
  (let [identity-key (juxt :owner :ordinal)
        expected (into {} (map (juxt identity-key identity)) pins)
        actual (into {} (map (juxt identity-key identity)) sites)]
    (vec (for [key (sort-by pr-str (into (set (keys expected)) (keys actual)))
               :let [pin (expected key) site (actual key)
                     reason (cond (nil? pin) :undeclared
                                  (nil? site) :missing
                                  (not (:test-owner? site)) :not-a-test-owner
                                  (not= (:purpose pin) (:purpose site)) :purpose-changed
                                  (not= (:call pin) (:call site)) :call-changed)]
               :when reason]
           {:owner (first key) :ordinal (second key) :reason reason
            :expected pin :actual site}))))

;; @spec DATACODE-SLEEP-001
(deftest sleep-pins-survive-line-movement-and-refuse-purpose-drift
  (let [scan (ns-resolve 'clj-surgeon.lane-manifest-test 'sleep-sites)
        check (ns-resolve 'clj-surgeon.lane-manifest-test 'sleep-pin-violations)
        source "(ns fixture)\n(deftest waits (^{:temporal-purpose :poll} Thread/sleep 10))\n"
        pins [{:owner 'fixture/waits :ordinal 1 :purpose :poll :call '(Thread/sleep 10)}]]
    (is (some? scan) "sleep discovery returns owner identities")
    (is (some? check) "pin validation is a pure relation")
    (when (and scan check)
      (let [before (scan "fixture.clj" source)
            shifted (scan "fixture.clj" (str "; unrelated line\n" source))]
        (is (= [] (check pins before)))
        (is (= [] (check pins shifted)))
        (is (= 2 (:line (first before))))
        (is (= 3 (:line (first shifted))))
        (doseq [[reason changed]
                [[:purpose (str/replace source ":poll" ":spaced-stimulus")]
                 [:call (str/replace source "sleep 10" "sleep 11")]
                 [:owner (str/replace source "waits" "renamed")]
                 [:cardinality (str/replace source "sleep 10)" "sleep 10) (Thread/sleep 10)")]]]
          (let [errors (check pins (scan "fixture.clj" changed))]
            (is (seq errors) (str "changed " reason " must refuse"))
            (is (some #(= 'fixture/waits (:owner %)) errors) (pr-str errors)))))))
  (when-let [scan (ns-resolve 'clj-surgeon.lane-manifest-test 'sleep-sites)]
    (let [file "test/clj_surgeon/census_pool_test.clj"
          source (slurp file)
          pins (filter #(= "clj-surgeon.census-pool-test" (namespace (:owner %)))
                       declared-merge-gate-sleeps)
          check (ns-resolve 'clj-surgeon.lane-manifest-test 'sleep-pin-violations)]
      (is (= [] (check pins (scan file source))))
      (is (= [] (check pins (scan file (str "; unrelated fixture line\n" source)))))
      (let [errors (check pins (scan file (str/replace source ":poll" ":spaced-stimulus")))]
        (is (= [:purpose-changed] (mapv :reason errors)))
        (is (= ['clj-surgeon.census-pool-test/the-pool-is-bounded-and-outlives-nothing]
               (mapv :owner errors)))))))

(deftest every-sleep-on-the-merge-gate-is-declared-with-its-reason
  (let [files (for [lane [:fast :integration] n (lm/namespaces-for lane)]
                (:file (get @on-disk n)))
        sites (mapcat #(let [source (slurp %)]
                         (when (str/includes? source "Thread/sleep")
                           (sleep-sites % source))) files)
        errors (sleep-pin-violations declared-merge-gate-sleeps sites)]
    (is (= [] errors) (pr-str errors))))

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
