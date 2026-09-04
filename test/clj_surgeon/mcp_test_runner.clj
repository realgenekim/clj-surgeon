(ns clj-surgeon.mcp-test-runner
  "The JVM test lanes' entry point (TEST-ISO-001).

   Takes LANE NAMES as argv (`fast`, `integration`, `battery`) and runs
   exactly the namespaces `clj-surgeon.lane-manifest` declares for them, in
   manifest order. Namespaces are `require`d at RUNTIME rather than in this
   ns form on purpose: a static require of all 49 would load every battery
   namespace -- and every cold-launcher helper they pull in -- into the fast
   lane's JVM, which is the isolation the partition exists to buy.

   Temp-dir hygiene is unchanged (MCP-OP-TMPHYG-001..008, inb-9483a4):
   `tmp-leak-support/secure-tmpdir!` refuses a RAM-backed base and re-execs
   this suite as a child with a private `-Djava.io.tmpdir`."
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.ns-isolation :as iso]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.string :as str]
   [clojure.test :as t]))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-002 @spec TEST-ISO-003 @spec TEST-ISO-004
;; @spec TEST-ISO-005 @spec TEST-ISO-007 @spec TEST-ISO-010
;;
;; THE PER-NAMESPACE SNAPSHOT FIXTURE. One mechanism, six witnesses.
;;
;; `run-tests` runs the whole lane inside one pair of parentheses, which makes
;; every resource question a question about the LANE: something spawned a
;; child, something left a port open, something took too long. That is the
;; wrong grain -- a run-level answer tells you the suite is dirty and leaves
;; you to bisect 38 namespaces to find out which one. Running `test-ns` one at
;; a time with a probe on each side attributes every leak to the namespace
;; that produced it, by name, in the same run that found it.
;;
;; KNOWN REACH, stated rather than left to be discovered: namespaces are
;; `require`d BEFORE the first window opens, because TEST-ISO-001's metadata
;; refusal has to be able to refuse the whole run before any test executes.
;; A side effect at LOAD time -- a top-level `def` that binds a socket -- is
;; therefore outside every window and invisible to these six. The lane-level
;; temp ratchet still sees its leavings; nothing here sees its act.
;; ---------------------------------------------------------------------------

(defn run-namespace-with-snapshot
  "Runs ONE namespace between two probes. Returns its `clojure.test` counters,
   its wall, and the violations its lane is held to."
  [n repo-root]
  (let [before (iso/probe repo-root)
        counters (t/test-ns n)
        after (iso/probe-after repo-root)]
    {:namespace n
     :counters counters
     :elapsed-ms (quot (- (:instant-ns after) (:instant-ns before)) 1000000)
     :violations (iso/enforced (lm/lane-of n)
                               (iso/violations n before after
                                               {:ledger @iso/allocated-ports}))}))

(defn lane-budget-violations
  "@spec TEST-ISO-007 -- the per-LANE ceiling, over the namespaces that
   actually ran. Reported separately from the per-namespace budgets because it
   is a different claim: every namespace can be inside its own budget while
   the lane the fleet waits on is minutes long."
  [runs]
  (->> (group-by (comp lm/lane-of :namespace) runs)
       (keep (fn [[lane rs]]
               (iso/lane-budget-violation lane (reduce + (map :elapsed-ms rs)))))
       vec))

(defn report-isolation!
  "Prints every violation as a typed line and returns how many there were.
   A refusal nobody hears is indistinguishable from silent data loss
   (delivery invariant 17), so this prints to *err*, prints a COUNT even when
   the count is zero, and the count is what the exit code carries."
  [runs]
  (let [vs (into (vec (mapcat :violations runs)) (lane-budget-violations runs))]
    (binding [*out* *err*]
      (if (seq vs)
        (do (println (format "\nTEST-ISOLATION: %d violation(s) -- the suite's own purity rules, per namespace:" (count vs)))
            (doseq [v vs] (println "  " (iso/message v))))
        (println (format "\ntest-isolation: 0 violations across %d namespace(s) (TEST-ISO-002/003/004/005/007/010)"
                         (count runs)))))
    (count vs)))

(defn lane-namespaces
  "Resolves `lanes` (a seq of lane keywords) -- or, when `explicit` is
   non-empty, that explicit list of namespace symbols -- to the namespaces to
   run.

   Returns {:namespaces [syms]} on success."
  [lanes explicit]
  (if (seq explicit)
    ;; @spec TEST-ISO-001 -- a namespace with no lane declaration is a TYPED
    ;; REFUSAL naming its subject and its remedy, never a silent skip. A
    ;; skip is indistinguishable from a green suite with less in it, which is
    ;; the failure mode `mcp-formatter-test` lived in unnoticed until round
    ;; three adopted it into :fast.
    (let [undeclared (vec (remove lm/manifest explicit))]
      (if (seq undeclared)
        {:refusal :lane-undeclared
         :namespaces undeclared
         :message (str/join "\n" (map lm/refusal-message undeclared))}
        {:namespaces (vec explicit)}))
    (let [unknown (vec (remove (set lm/lanes) lanes))]
      (if (seq unknown)
        {:refusal :unknown-lane
         :namespaces unknown
         :message (format "lane-refused: unknown lane(s) %s; known lanes are %s."
                          (str/join ", " unknown)
                          (str/join ", " (map name lm/lanes)))}
        {:namespaces (vec (mapcat lm/namespaces-for lanes))}))))

(defn lane-metadata-refusal
  "@spec TEST-ISO-001 -- after loading, every namespace must CARRY the lane
   the manifest assigned it. Checked at runtime, not only by a source scan:
   a scan reads a spelling, `the-ns` reads what the JVM actually loaded, and
   only the second can catch a file whose ns form the reader took differently
   than the scan did. Returns nil when every namespace agrees."
  [namespaces]
  (let [wrong (vec (keep (fn [n]
                           (let [want (lm/lane-of n)
                                 got (:lane (meta (find-ns n)))]
                             (when (not= want got)
                               (format "%s declares :lane %s but the manifest assigns %s"
                                       n (pr-str got) (pr-str want)))))
                         namespaces))]
    (when (seq wrong)
      {:refusal :lane-metadata-mismatch
       :namespaces wrong
       :message (str "lane-refused: " (count wrong)
                     " namespace(s) whose own ns metadata disagrees with "
                     "clj-surgeon.lane-manifest (TEST-ISO-001):\n  "
                     (str/join "\n  " wrong))})))

(defn- parse-lanes
  [args]
  (mapv (comp keyword str/lower-case str) args))

(defn parse-args
  "`--ns a.b-test c.d-test` selects namespaces explicitly (still subject to the
   manifest's refusal -- that is the point of the flag: the refusal has to be
   reachable from the command line or it is dead code a witness alone keeps
   alive). Anything else is a list of lane names."
  [args]
  (let [args (mapv str args)]
    (if (= "--ns" (first args))
      {:explicit (mapv symbol (rest args))}
      {:lanes (parse-lanes args)})))

(defn -main
  [& args]
  (let [{:keys [lanes explicit]} (parse-args args)
        ;; Resolve the namespace set FIRST, but do not ACT on it yet. The home
        ;; decision below is a property of WHAT IS ABOUT TO RUN, not of how the
        ;; caller spelled the invocation, and `lane-namespaces` is pure -- it
        ;; reads the manifest and loads nothing.
        resolved (lane-namespaces lanes explicit)
        ;; @spec TEST-ISO-006 -- a run is launched on a throwaway user.home
        ;; unless a BATTERY namespace is in it. Battery namespaces launch cold
        ;; `clojure`/`bb`/`git` children that legitimately need the seat's
        ;; ~/.m2 and ~/.gitlibs; a throwaway home would not isolate those, it
        ;; would make them re-download the world. Nothing in the fast or
        ;; integration lanes needs any of it.
        ;;
        ;; Deliberately a property of the RESOLVED SET rather than of the lane
        ;; names. The first cut keyed on `(= #{:fast} (set lanes))`, so
        ;; `make mcp-test` -- fast + integration, THE MERGE GATE -- ran without
        ;; isolation while a fast-lane witness asserted it had it, and all four
        ;; clones of the concurrency battery failed identically. A rule that
        ;; holds under one spelling of an invocation and not another is the
        ;; same class as doctrine that disagrees with the installed prompt:
        ;; true where it is written, false where it takes effect.
        isolate-home? (and (seq (:namespaces resolved))
                           (not-any? #(= :battery (lm/lane-of %))
                                     (:namespaces resolved)))
        ;; @spec MCP-OP-TMPHYG-003 -- THE TEMP GUARD RUNS FIRST, before any
        ;; lane refusal, unconditionally. It is the check that refuses to write
        ;; test fixtures into RAM, and a run that is about to be refused for
        ;; some other reason must still not be allowed to reach a tmpfs on its
        ;; way there. A first cut of the lane runner hoisted the "no lane
        ;; named" refusal above this call; the tmp-leak ratchet's step 9 caught
        ;; it immediately -- `ran (exit 96) with TMPDIR=/tmp instead of
        ;; refusing` -- in all four clones of the concurrency battery.
        {:keys [refused root]}
        (tmp-leak/secure-tmpdir! {:main-ns "clj-surgeon.mcp-test-runner"
                                  :isolate-home? isolate-home?}
                                 args)
        _ (when refused (System/exit 97))
        _ (when (and (empty? lanes) (empty? explicit))
            (binding [*out* *err*]
              (println (str "lane-refused: no lane named. Usage: -m "
                            "clj-surgeon.mcp-test-runner <lane>... where lane is "
                            (str/join ", " (map name lm/lanes))
                            " -- or --ns <namespace>...")))
            (System/exit 96))
        _ (when (:refusal resolved)
            (binding [*out* *err*] (println (:message resolved)))
            (System/exit 96))
        namespaces (:namespaces resolved)
        _ (println (format "lanes: %s -- %d namespace(s), home-isolated %s"
                           (if (seq lanes) (str/join "+" (map name lanes)) "--ns")
                           (count namespaces) isolate-home?))
        _ (doseq [n namespaces] (require n))
        _ (when-let [{:keys [message]} (lane-metadata-refusal namespaces)]
            (binding [*out* *err*] (println message))
            (System/exit 96))
        tmp-root root
        tmp-before (tmp-leak/tmp-entries)
        repo-root (System/getProperty "user.dir")
        runs (mapv #(run-namespace-with-snapshot % repo-root) namespaces)
        result (apply merge-with + (map :counters runs))
        _ (t/do-report (assoc result :type :summary))
        iso-fail (report-isolation! runs)
        leak-fail (tmp-leak/report-and-sweep-leak! tmp-root tmp-before)]
    (System/exit (+ (:fail result) (:error result) iso-fail leak-fail))))
