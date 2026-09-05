(ns ^{:lane :fast} clj-surgeon.ns-isolation-test
  "The six runtime purity witnesses on one per-namespace snapshot fixture --
   TEST-ISO-002, 003, 004, 005, 007, 010.

   EVERY WITNESS HERE IS REACHABLE WITHOUT COMMITTING THE VIOLATION IT
   DETECTS. That is the whole reason `clj-surgeon.ns-isolation` splits the
   probe from the fold: a witness for `a child process leaked` that had to
   spawn a child process would be a battery test, would cost a cold JVM, and
   would put the exact defect it hunts INTO the fast lane. Instead the probe's
   output shape is a plain map, and each witness plants the `after` map a real
   violation would have produced and asserts the exact typed refusal.

   THAT IS NOT THE WHOLE PROOF, AND THIS DOCSTRING SAYS SO RATHER THAN LETTING
   A READER ASSUME IT. A planted map proves the FOLD. It cannot prove the
   PROBE sees what it claims to see -- `own-listeners` could return an empty
   map forever and every witness below would still be green. The probe is
   proved two other ways: `the-fixture-catches-a-real-write-...` drives a real
   file through the real probe end to end, and the round-four record carries
   the planted-violation runs on an archive copy of the tree, where a
   throwaway namespace really does spawn, really does bind, and really does
   leak, and the lane really does refuse. A witness that can only go red at
   authoring time is not a ratchet (the marker-audit lesson)."
  (:require
   [clj-surgeon.ns-isolation :as iso]
   [clj-surgeon.spawn-ledger :as spawn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def ^:private subject 'clj-surgeon.planted-test)

(defn- empty-snapshot
  "A snapshot in which nothing is happening: every probe is empty and the
   clock has not moved. Each witness below perturbs exactly ONE key, so a
   failure names the resource that was perturbed and never a neighbour."
  []
  {:instant-ns 0
   :tmp-entries {}
   :target-entries {}
   :worktree {}
   :listeners {}
   :var-roots {}
   :globals {}
   :threads {}
   :processes {}
   :spawns []})

(defn- messages [vs] (mapv iso/message vs))

(defn- of-intent [vs id] (filterv #(= id (:intent %)) vs))

;; ---------------------------------------------------------------------------
;; TEST-ISO-002 -- process spawn
;; ---------------------------------------------------------------------------

;; @spec TEST-ISO-002
(deftest a-child-process-fails-the-namespace-by-pid-and-command-line
  (let [before (empty-snapshot)
        after (assoc (empty-snapshot) :processes
                     {4242 "/usr/bin/env bb test/run_all.clj"})
        vs (iso/violations subject before after)
        proc (of-intent vs "TEST-ISO-002")]
    (testing "the spawn is refused, once, naming the namespace"
      (is (= 1 (count proc)))
      (is (= subject (:namespace (first proc))))
      (is (= "process spawn" (:resource (first proc)))))
    (testing "the refusal carries the PID and the COMMAND LINE, not just a count"
      (let [m (iso/message (first proc))]
        (is (str/includes? m "4242"))
        (is (str/includes? m "bb test/run_all.clj")
            (str "the refusal must name what was spawned, or the reader has to "
                 "go find it themselves: " m))))
    (testing "a child that was ALREADY running is not attributed to this namespace"
      (let [pre (assoc (empty-snapshot) :processes {4242 "an inherited child"})]
        (is (empty? (of-intent (iso/violations subject pre after) "TEST-ISO-002")))))))


;; @spec TEST-ISO-002
(deftest a-child-that-already-exited-fails-by-pid-and-command-line
  ;; THE ROUND-THREE LANDING REVIEW'S FINDING 6, as a witness. The pid diff
  ;; above can only see a child that is STILL RUNNING. `mcp-inspect-tool-test`
  ;; drove `/bin/sh -c 'printf cold-ok'` through the production cold-verify
  ;; helper and WAITED for it, so at the closing snapshot there was nothing to
  ;; see -- a declared `:fast` namespace, whose lane rule reads `No child
  ;; process`, spawning one that no control could observe. Recording the
  ;; LAUNCH is what survives the child.
  (let [before (empty-snapshot)
        after (assoc (empty-snapshot) :spawns
                     [{:pid 5150 :command "/bin/sh -c printf cold-ok" :at-ns 1}])
        vs (of-intent (iso/violations subject before after) "TEST-ISO-002")]
    (is (= 1 (count vs)))
    (is (= "process spawn" (:resource (first vs))))
    (let [m (iso/message (first vs))]
      (is (str/includes? m "5150"))
      (is (str/includes? m "/bin/sh -c printf cold-ok")
          (str "the refusal must name what was spawned: " m))
      (is (str/includes? m "already exited")
          (str "and must say why no snapshot could have seen it: " m)))))

;; @spec TEST-ISO-002
(deftest a-spawn-seen-by-both-observations-is-reported-once
  ;; A child that is still alive is in the pid diff AND in the ledger. Two
  ;; lines for one child would teach a reader to skim the count.
  (let [before (empty-snapshot)
        after (assoc (empty-snapshot)
                     :processes {5150 "/bin/sh -c sleep 60"}
                     :spawns [{:pid 5150 :command "/bin/sh -c sleep 60" :at-ns 1}])
        vs (of-intent (iso/violations subject before after) "TEST-ISO-002")]
    (is (= 1 (count vs)) (pr-str (messages vs)))
    (is (str/includes? (iso/message (first vs)) "live descendant")
        "the more serious kind wins")))

;; @spec TEST-ISO-002
(deftest the-probe-really-reads-the-ledger-not-only-the-planted-map
  ;; THE PROBE, END TO END, WITHOUT SPAWNING ANYTHING. A planted map proves
  ;; the fold; it cannot prove `probe` looks at the ledger at all -- `:spawns`
  ;; could return `[]` forever and every witness above would stay green. This
  ;; drives the REAL ledger through the REAL probes: `record!` is the exact
  ;; call the four production spawn helpers make, so what is exercised here is
  ;; the wiring, not a stand-in. Fast-lane-safe because appending to an atom
  ;; is not a child process.
  ;; The ledger is rebound to a throwaway atom: this witness must exercise
  ;; the real `record!`/`snapshot`/`probe` path without APPENDING to the run's
  ;; real ledger, or the fixture would (correctly) attribute its own test
  ;; fixture to this namespace on the next window. A witness that pollutes the
  ;; thing it observes is not an observation.
  (with-redefs [spawn/ledger (atom [])]
    (let [repo (System/getProperty "user.dir")
          before (iso/probe repo)
          _ (spawn/record! 424242 ["/bin/sh" "-c" "witness-only-never-executed"])
          after (iso/probe-after repo)
          vs (of-intent (iso/violations subject before after) "TEST-ISO-002")]
      (is (= 1 (count vs))
          (str "the probe did not carry the ledger across the window: "
               (pr-str (messages vs))))
      (is (str/includes? (iso/message (first vs)) "424242"))
      (is (str/includes? (iso/message (first vs)) "witness-only-never-executed")))))

;; @spec TEST-ISO-002
(deftest the-spawn-allowlist-is-exact-per-namespace-and-cannot-reach-a-cold-runtime
  ;; The contract amendment, held to its three properties. It reads: NO child
  ;; process, EXCEPT these exact commands in these exact namespaces -- so the
  ;; interesting assertions are the ones about what it still refuses.
  (testing "a declared fixture command in its declared namespace is allowed"
    (is (iso/allowlisted-spawn? 'clj-surgeon.mcp-change-buffer-test
                                "/usr/bin/printf %s xxxx")))
  (testing "the SAME command in another namespace is not"
    (is (not (iso/allowlisted-spawn? 'clj-surgeon.mcp-paths-test
                                     "/usr/bin/printf %s xxxx"))
        "the allowlist is per namespace, or it is a global escape hatch"))
  (testing "an undeclared command in an allowlisted namespace is not"
    (is (not (iso/allowlisted-spawn? 'clj-surgeon.mcp-change-buffer-test
                                     "/usr/bin/env curl https://example.com"))))
  (testing "a COLD RUNTIME is refused even when someone lists it"
    ;; the 674 s the partition exists to remove cannot come back through here
    (with-redefs [iso/fast-lane-spawn-allowlist
                  '{clj-surgeon.mcp-change-buffer-test
                    [["/usr/bin/clojure" "someone tried to allowlist a cold JVM"]
                     ["/usr/local/bin/bb" "and a cold bb"]]}]
      (is (not (iso/allowlisted-spawn? 'clj-surgeon.mcp-change-buffer-test
                                       "/usr/bin/clojure -M:foo")))
      (is (not (iso/allowlisted-spawn? 'clj-surgeon.mcp-change-buffer-test
                                       "/usr/local/bin/bb test/run_all.clj")))))
  (testing "every allowlist entry carries a reason, and none names a cold runtime"
    (doseq [[n entries] iso/fast-lane-spawn-allowlist
            [prefix reason] entries]
      (is (and (string? reason) (>= (count reason) 20))
          (str n " allowlists " prefix " with no reason worth reading"))
      (is (nil? (re-find iso/cold-runtime-command prefix))
          (str n " allowlists the cold runtime " prefix)))))

;; @spec TEST-ISO-002
(deftest an-allowlisted-command-that-is-still-running-is-refused-anyway
  ;; The allowlist excuses a child that was launched AND reaped inside the
  ;; namespace. A live one is a leak whatever it is.
  (let [before (empty-snapshot)
        after (assoc (empty-snapshot)
                     :processes {7777 "/bin/sleep 1"}
                     :spawns [{:pid 7777 :command "/bin/sleep 1" :at-ns 1}])
        vs (of-intent (iso/violations 'clj-surgeon.mcp-change-buffer-test
                                      before after)
                      "TEST-ISO-002")]
    (is (= 1 (count vs)) (pr-str (messages vs)))
    (is (str/includes? (iso/message (first vs)) "live descendant"))))

;; @spec TEST-ISO-002
(deftest every-src-spawn-site-records-into-the-ledger
  ;; THE SRC HALF, held closed by enumeration. The ledger can only see a spawn
  ;; a helper reports, so a new a raw builder construction in src/ that does not call
  ;; `record!` re-opens exactly the hole finding 6 came through -- silently,
  ;; because nothing would go red.
  ;;
  ;; This is a SOURCE SCAN and says so: it enumerates the spelling
  ;; the raw builder-class spelling and requires the same file to spell `spawn/record!`. It
  ;; cannot see a spawn through reflection, through a library, or through a
  ;; name it does not know, and it does not check that the record is on the
  ;; same code path. The behavioural half is the probe witness above; this is
  ;; the index that keeps the enumeration honest.
  (let [;; assembled from parts so that this witness's own scan string is not
        ;; itself a "spelling" that `no-fast-lane-namespace-spells-a-child-
        ;; process` reports -- the same trick `lane-manifest-test` already uses
        ;; for the renamed target. A scanner that reads its own source has to
        ;; be told which of its words are subjects and which are data.
        spawn-spelling (str "Process" "Builder.")
        files (->> (file-seq (io/file "src"))
                   (filter #(.isFile ^java.io.File %))
                   (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
                   sort)
        offenders (for [^java.io.File f files
                        :let [src (slurp f)]
                        :when (str/includes? src spawn-spelling)
                        :when (not (str/includes? src "spawn/record!"))]
                    (.getPath f))]
    (is (empty? offenders)
        (str (count offenders) " src spawn site(s) that do not append to "
             "clj-surgeon.spawn-ledger, so a child they start and reap is "
             "invisible to TEST-ISO-002: " (str/join ", " offenders)))))

;; ---------------------------------------------------------------------------
;; TEST-ISO-003 -- writes
;; ---------------------------------------------------------------------------

;; @spec TEST-ISO-003
(deftest a-write-outside-the-namespaces-own-tmp-subdir-fails-with-the-path
  (let [own (iso/namespace-tmp-dir-name subject)
        before (empty-snapshot)]
    (testing "the namespace's OWN subdir appearing is not a violation"
      (is (empty? (of-intent (iso/violations subject before
                                             (assoc (empty-snapshot) :tmp-entries {own 1}))
                             "TEST-ISO-003"))))
    (testing "any other new top-level temp entry is refused, by name"
      (let [vs (of-intent (iso/violations subject before
                                          (assoc (empty-snapshot) :tmp-entries
                                                 {"scratch-9f2" 1}))
                          "TEST-ISO-003")]
        (is (= 1 (count vs)))
        (is (= "temp root" (:resource (first vs))))
        (is (str/includes? (iso/message (first vs)) "scratch-9f2"))
        (is (str/includes? (iso/message (first vs)) own)
            "the refusal must name the subdir the namespace was allowed to use")))
    (testing "a write into target/ is refused -- it is shared by every lane on this checkout"
      (let [vs (of-intent (iso/violations subject before
                                          (assoc (empty-snapshot) :target-entries
                                                 {"census-receipt.edn" 1}))
                          "TEST-ISO-003")]
        (is (= 1 (count vs)))
        (is (= "target/" (:resource (first vs))))
        (is (str/includes? (iso/message (first vs)) "target/census-receipt.edn"))))
    (testing "the working tree is watched in all three directions"
      (let [base (assoc (empty-snapshot) :worktree {"src/a.clj" [10 1] "src/b.clj" [10 1]})
            after (assoc (empty-snapshot) :worktree {"src/a.clj" [99 2] "src/c.clj" [1 1]})
            ms (messages (of-intent (iso/violations subject base after) "TEST-ISO-003"))]
        (is (= 3 (count ms)) (pr-str ms))
        (is (some #(str/includes? % "src/c.clj was created") ms))
        (is (some #(str/includes? % "src/b.clj was deleted") ms))
        (is (some #(str/includes? % "src/a.clj was modified") ms))))))

;; @spec TEST-ISO-003
(deftest the-fixture-catches-a-real-write-outside-the-namespaces-own-subdir
  ;; THE PROBE, END TO END, WITH NO PLANTED MAP. This is the witness that the
  ;; observation half is not a stub: a real file is created under the real run
  ;; temp root through the real `probe`, and the real fold names its real path.
  ;; It is fast-lane-safe because a file is not a child process, a socket or a
  ;; thread -- the other five probes cannot be proved this cheaply, which is
  ;; why the archive-copy planted runs exist.
  (let [repo (System/getProperty "user.dir")
        own (iso/namespace-tmp-dir subject)
        stray (io/file (iso/tmp-root) "nsiso-stray-probe-witness")]
    (try
      (let [before (iso/probe repo)
            _ (spit (io/file own "inside.txt") "allowed")
            _ (.mkdirs stray)
            after (iso/probe-after repo)
            vs (of-intent (iso/violations subject before after) "TEST-ISO-003")]
        (testing "the stray directory is named"
          (is (some #(str/includes? % "nsiso-stray-probe-witness") (messages vs))
              (str "the real probe did not see a real write under "
                   (.getPath (iso/tmp-root)) "; observed violations: "
                   (pr-str (messages vs)))))
        (testing "the namespace's own subdir is NOT named"
          (is (not-any? #(str/includes? % (str "temp root: " (iso/namespace-tmp-dir-name subject) " "))
                        (messages vs)))))
      (finally
        (doseq [^java.io.File f [(io/file own "inside.txt") stray own]]
          (when (.exists f) (.delete f)))))))

;; ---------------------------------------------------------------------------
;; TEST-ISO-004 -- ports and listeners
;; ---------------------------------------------------------------------------

;; @spec TEST-ISO-004
(deftest a-leaked-listener-fails-naming-the-port-and-whether-it-was-allocated
  (let [before (empty-snapshot)
        after (assoc (empty-snapshot) :listeners {77771 8173})]
    (testing "a listener with no ledger entry is refused as a FIXED port"
      (let [vs (of-intent (iso/violations subject before after {}) "TEST-ISO-004")
            m (iso/message (first vs))]
        (is (= 1 (count vs)))
        (is (= "listening socket" (:resource (first vs))))
        (is (str/includes? m "8173"))
        (is (str/includes? m "NOT allocated through the port-0 allocator")
            (str "the refusal must distinguish a leaked allocation from a fixed "
                 "port literal -- they have different remedies: " m))))
    (testing "a listener the allocator HANDED OUT is refused as a leak with an owner"
      (let [vs (of-intent (iso/violations subject before after
                                          {:ledger {[subject 1] 8173}})
                          "TEST-ISO-004")]
        (is (str/includes? (iso/message (first vs))
                           "allocated through the port-0 allocator and never closed"))))
    (testing "a listener that was already open is not attributed to this namespace"
      (is (empty? (of-intent (iso/violations subject after after) "TEST-ISO-004"))))))

;; @spec TEST-ISO-004
(deftest the-port-allocator-hands-out-ephemeral-ports-and-records-every-one
  (let [p (iso/allocate-port! subject)]
    (testing "the port is real and ephemeral, never a literal"
      (is (pos? p))
      (is (> p 1024) (str "allocated " p ", which is a privileged port")))
    (testing "the allocation is in the ledger, so a later leak has an owner"
      (is (contains? (set (vals @iso/allocated-ports)) p)))))

;; ---------------------------------------------------------------------------
;; TEST-ISO-005 -- global mutation leaks
;; ---------------------------------------------------------------------------

;; @spec TEST-ISO-005
(deftest a-leaked-with-redefs-fails-naming-the-var
  (let [before (assoc (empty-snapshot) :var-roots {'clj-surgeon.edit/apply-change 111})
        after (assoc (empty-snapshot) :var-roots {'clj-surgeon.edit/apply-change 222})
        vs (of-intent (iso/violations subject before after) "TEST-ISO-005")]
    (is (= 1 (count vs)))
    (is (= "var root" (:resource (first vs))))
    (is (str/includes? (iso/message (first vs)) "#'clj-surgeon.edit/apply-change"))
    (is (str/includes? (iso/message (first vs)) "with-redefs, an alter-var-root or a :reload leaked"))))

;; @spec TEST-ISO-005
(deftest a-mutated-global-atom-fails-unless-it-is-declared-mutable-with-a-reason
  (let [before (assoc (empty-snapshot) :globals {'clj-surgeon.cache/entries 1})
        after (assoc (empty-snapshot) :globals {'clj-surgeon.cache/entries 2})]
    (testing "an undeclared global that moved is refused, naming the remedy"
      (let [vs (of-intent (iso/violations subject before after) "TEST-ISO-005")]
        (is (= 1 (count vs)))
        (is (= "global container" (:resource (first vs))))
        (is (str/includes? (iso/message (first vs)) "mutable-global-allowlist"))))
    (testing "a DECLARED mutable container is allowed"
      (is (empty? (of-intent (iso/violations subject before after
                                             {:allowlist '#{clj-surgeon.cache/entries}})
                             "TEST-ISO-005"))))))

(def ^:private reload-declaration
  '{clj-surgeon.reloader-test {clj-surgeon.handler "the reload IS the behaviour under test"}})

;; @spec TEST-ISO-005
(deftest a-declared-reload-is-exempt-for-that-namespace-and-nobody-else
  ;; Reloading a production namespace replaces every var root in it, so the
  ;; ONE namespace in this tree whose subject is the hot-reload path would
  ;; otherwise fail with ~45 refusals every run -- and a witness that is red on
  ;; correct behaviour every run is a witness somebody turns off. The
  ;; exemption is therefore scoped to a NAMED production namespace for a NAMED
  ;; test namespace, and the three ways it must NOT widen are asserted here.
  (let [before (assoc (empty-snapshot) :var-roots '{clj-surgeon.handler/handle 1
                                                    clj-surgeon.other/f 1})
        after (assoc (empty-snapshot) :var-roots '{clj-surgeon.handler/handle 2
                                                   clj-surgeon.other/f 2})
        for-ns (fn [n] (of-intent (iso/violations n before after {:reloads reload-declaration})
                                  "TEST-ISO-005"))]
    (testing "the declaring namespace is exempt for the namespace it named -- and ONLY that one"
      (let [vs (for-ns 'clj-surgeon.reloader-test)]
        (is (= 1 (count vs)))
        (is (str/includes? (iso/message (first vs)) "clj-surgeon.other/f")
            "a reload declaration must not cover a leak in a DIFFERENT namespace")))
    (testing "a namespace that declared nothing is exempt from nothing"
      (is (= 2 (count (for-ns 'clj-surgeon.someone-else-test)))))
    (testing "the declaration in the tree is narrow and every entry carries a reason"
      (doseq [[test-ns reloaded] iso/declared-namespace-reloads]
        (is (symbol? test-ns))
        (doseq [[prod reason] reloaded]
          (is (symbol? prod))
          (is (and (string? reason) (> (count reason) 30))
              (str test-ns " declares a reload of " prod
                   " with no real reason -- an exemption with no reason is how a "
                   "witness stops being one")))))))

;; @spec TEST-ISO-005
(deftest the-allowlist-cannot-exempt-a-leaked-var-root
  ;; THE EXEMPTION HAS A CEILING, AND THE CEILING IS TESTED. An allowlist that
  ;; can cover every class it is adjacent to is not an allowlist, it is an off
  ;; switch -- and the first person under deadline will use it as one. A
  ;; leaked var root has no legitimate form, so naming it here must not help.
  (let [before (assoc (empty-snapshot) :var-roots {'clj-surgeon.edit/apply-change 111})
        after (assoc (empty-snapshot) :var-roots {'clj-surgeon.edit/apply-change 222})
        vs (of-intent (iso/violations subject before after
                                      {:allowlist '#{clj-surgeon.edit/apply-change}})
                      "TEST-ISO-005")]
    (is (= 1 (count vs))
        "naming a var in the mutable-global allowlist must NOT exempt its root being swapped")))

;; ---------------------------------------------------------------------------
;; TEST-ISO-007 -- time budgets
;; ---------------------------------------------------------------------------

;; @spec TEST-ISO-007
(deftest a-namespace-over-its-budget-fails-with-its-wall
  (let [ms->ns (fn [ms] (* ms 1000000))
        before (empty-snapshot)
        after (assoc (empty-snapshot) :instant-ns (ms->ns 9000))]
    (testing "over the default budget, refused with the measured wall AND the budget"
      (let [vs (of-intent (iso/violations subject before after) "TEST-ISO-007")
            m (iso/message (first vs))]
        (is (= 1 (count vs)))
        (is (str/includes? m "9000 ms"))
        (is (str/includes? m (str iso/default-namespace-budget-ms " ms")))
        (is (str/includes? m "namespace-budget-overrides")
            (str "a budget refusal must print the remedy or it reads as "
                 "`your test is slow, good luck`: " m))))
    (testing "EXACTLY AT the budget passes and one ms past it refuses"
      ;; The ceiling witness shape: at the limit succeeds, one unit past it
      ;; refuses. An assertion on a number far from the boundary cannot tell a
      ;; correct `>` from an off-by-one `>=`.
      (let [at (assoc (empty-snapshot) :instant-ns (ms->ns iso/default-namespace-budget-ms))
            past (assoc (empty-snapshot) :instant-ns (ms->ns (inc iso/default-namespace-budget-ms)))]
        (is (empty? (of-intent (iso/violations subject before at) "TEST-ISO-007")))
        (is (= 1 (count (of-intent (iso/violations subject before past) "TEST-ISO-007"))))))
    (testing "a declared override raises the ceiling for that namespace only"
      (is (empty? (of-intent (iso/violations subject before after
                                             {:overrides {subject 20000}})
                             "TEST-ISO-007")))
      (is (seq (of-intent (iso/violations 'clj-surgeon.other-test before after
                                          {:overrides {subject 20000}})
                          "TEST-ISO-007"))))))

;; @spec TEST-ISO-007
(deftest the-lane-total-has-its-own-budget-because-the-sum-is-what-the-fleet-pays
  (testing "the fast lane's ceiling is the 60 s the partition exists to buy"
    (is (= 60000 (get iso/lane-budget-ms :fast))))
  (testing "at the ceiling passes; past it refuses, naming the lane and both numbers"
    (is (nil? (iso/lane-budget-violation :fast 60000)))
    (let [m (iso/message (iso/lane-budget-violation :fast 60001))]
      (is (str/includes? m "fast lane took 60001 ms"))
      (is (str/includes? m "60000 ms"))))
  (testing "an unbudgeted lane is not silently unbounded -- it is simply not this witness's subject"
    (is (nil? (iso/lane-budget-violation :no-such-lane 999999999)))))

;; ---------------------------------------------------------------------------
;; TEST-ISO-010 -- thread and executor leaks
;; ---------------------------------------------------------------------------

;; @spec TEST-ISO-010
(deftest a-leaked-non-daemon-thread-fails-naming-it
  (let [before (empty-snapshot)
        after (assoc (empty-snapshot) :threads {91 "clj-surgeon-census-pool-3"})
        vs (of-intent (iso/violations subject before after) "TEST-ISO-010")
        m (iso/message (first vs))]
    (is (= 1 (count vs)))
    (is (= "non-daemon thread" (:resource (first vs))))
    (is (str/includes? m "clj-surgeon-census-pool-3"))
    (is (str/includes? m "keeps the JVM from exiting")
        (str "the refusal must say why a leaked non-daemon thread matters, "
             "because 0 failures plus a hung runner reads as a CI fault: " m))))

;; @spec TEST-ISO-010
(deftest the-thread-probe-sees-this-jvms-real-threads
  ;; The probe half, cheaply: this suite is itself running on a live
  ;; non-daemon thread, so an empty result would be a stubbed probe.
  (let [t (iso/live-non-daemon-threads)]
    (is (seq t) "live-non-daemon-threads returned nothing while this test was running on one")
    (is (every? string? (vals t)))))

;; ---------------------------------------------------------------------------
;; The receipt shape, across all six
;; ---------------------------------------------------------------------------

;; @spec TEST-ISO-002
;; @spec TEST-ISO-003
;; @spec TEST-ISO-004
;; @spec TEST-ISO-005
;; @spec TEST-ISO-007
;; @spec TEST-ISO-010
(deftest every-violation-names-its-intent-its-namespace-and-its-resource
  ;; Delivery invariant 20: a receipt that does not name its subject and its
  ;; evidence source is `:unverified`, never a finding. Six intents fire at
  ;; once here, so the shape is checked on the whole family rather than on the
  ;; one that happened to be written last.
  (let [before (empty-snapshot)
        after {:instant-ns (* 60000 1000000)
               :tmp-entries {"stray" 1}
               :target-entries {"out" 1}
               :worktree {"README.md" [1 1]}
               :listeners {5 9999}
               :var-roots {}
               :globals {}
               :threads {7 "leaked"}
               :processes {9 "/bin/sh -c make"}}
        vs (iso/violations subject before after)]
    (testing "all five detectable-from-this-map intents fired"
      (is (= #{"TEST-ISO-002" "TEST-ISO-003" "TEST-ISO-004" "TEST-ISO-007" "TEST-ISO-010"}
             (set (map :intent vs)))))
    (doseq [v vs]
      (is (= subject (:namespace v)) (pr-str v))
      (is (string? (:resource v)) (pr-str v))
      (is (str/starts-with? (iso/message v) (:intent v)) (iso/message v))
      (is (str/includes? (iso/message v) (str subject)) (iso/message v)))))

;; @spec TEST-ISO-002
;; @spec TEST-ISO-003
;; @spec TEST-ISO-004
;; @spec TEST-ISO-005
;; @spec TEST-ISO-007
;; @spec TEST-ISO-010
(deftest a-clean-namespace-produces-no-violations-at-all
  ;; The other half of the ceiling: a witness that fires on everything is a
  ;; witness nobody keeps. A real snapshot pair taken back to back around no
  ;; work at all must be silent -- including its OWN probing, which is the
  ;; ordering guarantee `probe`/`probe-after` exist to give.
  (let [repo (System/getProperty "user.dir")
        before (iso/probe repo)
        after (iso/probe-after repo)
        vs (iso/violations subject before after)]
    (is (empty? vs)
        (str "the fixture accuses a namespace that did nothing; the probe is "
             "observing its own work: " (pr-str (messages vs))))))

;; ---------------------------------------------------------------------------
;; The fixture's REACH, pinned
;; ---------------------------------------------------------------------------

;; @spec TEST-ISO-002
;; @spec TEST-ISO-003
;; @spec TEST-ISO-004
;; @spec TEST-ISO-005
;; @spec TEST-ISO-007
;; @spec TEST-ISO-010
(deftest each-lane-is-held-only-to-the-rules-that-lane-can-keep
  ;; What is NOT checked has to be visible without inferring it from silence.
  ;; The battery lane exists to launch cold child JVMs; holding it to
  ;; TEST-ISO-002 would make the witness fire on the lane's definition, and a
  ;; witness that fires on correct behaviour is one somebody deletes.
  (testing "the fast lane is held to all six -- they ARE the fast lane's rules"
    (is (= #{"TEST-ISO-002" "TEST-ISO-003" "TEST-ISO-004" "TEST-ISO-005"
             "TEST-ISO-007" "TEST-ISO-010"}
           (get iso/enforced-intents-by-lane :fast))))
  (testing "the integration lane keeps no-child-process, the budget and thread leaks"
    (is (= #{"TEST-ISO-002" "TEST-ISO-007" "TEST-ISO-010"}
           (get iso/enforced-intents-by-lane :integration))))
  (testing "the battery lane keeps only the budget"
    (is (= #{"TEST-ISO-007"} (get iso/enforced-intents-by-lane :battery))))
  (testing "filtering DROPS the rule but never invents one"
    (let [vs [{:intent "TEST-ISO-003" :namespace subject :resource "temp root" :detail "x"}
              {:intent "TEST-ISO-007" :namespace subject :resource "time budget" :detail "y"}]]
      (is (= 1 (count (iso/enforced :integration vs))))
      (is (= "TEST-ISO-007" (:intent (first (iso/enforced :integration vs)))))
      (is (= 2 (count (iso/enforced :fast vs))))
      (is (empty? (iso/enforced :no-such-lane vs))
          "an unknown lane must enforce NOTHING rather than everything")))
  (testing "every lane in the manifest's lane list has a declared reach"
    (is (= #{:fast :integration :battery} (set (keys iso/enforced-intents-by-lane))))))

;; @spec TEST-ISO-007
(deftest each-lane-has-its-own-default-budget-because-a-lane-is-a-cost-class
  ;; ONE default across three lanes is either useless for the fast lane or a
  ;; false alarm for the others. The first live run proved the second half:
  ;; `mcp-hot-verify-test` was refused at 10 128 ms against the fast lane's
  ;; 8 s ceiling, and it is `:integration` precisely because it drives an
  ;; in-process server and waits on it. A ceiling that fires on a lane's
  ;; definition is not a ratchet.
  (testing "the fast lane keeps the tight default"
    (is (= iso/default-namespace-budget-ms (get iso/lane-default-budget-ms :fast))))
  (testing "each lane declares one, and they widen in cost order"
    (is (= #{:fast :integration :battery} (set (keys iso/lane-default-budget-ms))))
    (is (< (get iso/lane-default-budget-ms :fast)
           (get iso/lane-default-budget-ms :integration)
           (get iso/lane-default-budget-ms :battery))))
  (testing "the integration ceiling leaves headroom over the measured worst case but is not unbounded"
    (is (<= 15000 (get iso/lane-default-budget-ms :integration) 30000)))
  (testing "the ceiling still bites at its own boundary, whichever lane's it is"
    (let [ms->ns #(* % 1000000)
          before (empty-snapshot)
          budget (get iso/lane-default-budget-ms :integration)
          at (assoc (empty-snapshot) :instant-ns (ms->ns budget))
          past (assoc (empty-snapshot) :instant-ns (ms->ns (inc budget)))]
      (is (empty? (of-intent (iso/violations subject before at {:default-budget-ms budget})
                             "TEST-ISO-007")))
      (is (= 1 (count (of-intent (iso/violations subject before past {:default-budget-ms budget})
                                 "TEST-ISO-007")))))))
