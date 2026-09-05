(ns ^{:lane :fast} clj-surgeon.outline-memory-test
  "Read-path allocation and parse-count witnesses for the outline projection.

   These are JVM-only witnesses: they read
   `com.sun.management.ThreadMXBean/getThreadAllocatedBytes`, which babashka's
   runner (`make test-bb`) cannot provide. They live in the JVM lanes; this
   namespace is `:fast`."
  (:require
   [clj-surgeon.outline :as outline]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

(def fixture-path
  "test-fixtures/memory/mem_015_outline_fixture.clj")

(def ^:private fixture-source (delay (slurp fixture-path)))

(def ^:private fixture-bytes
  (delay (alength (.getBytes ^String @fixture-source "UTF-8"))))

(def allocated-bytes-per-source-byte-ceiling
  "Allocation ceiling for one `outline-source` call, as a multiple of the
   source's UTF-8 byte count.

   Measured on this fixture (48,097 bytes) on 2026-09-03, anvil, JDK
   21.0.12, rewrite-clj 1.2.50, -Xmx1g, min of five calls after three
   warm-ups:

     two-parse path with a per-form `:source` string : 62,686,992 B = 1303.3x
     single-parse path with no discarded `:source`   : 38,201,624 B =  794.3x
     parse + walk alone (the irreducible node tree)  : 36,078,320 B =  750.1x

   The single-parse row above is the gate's own five-sample minimum
   (superseding an earlier three-sample reading of 782.6x taken the same
   day, before the gate below was corrected to actually run five samples).
   The ceiling stays at the value set from that earlier reading —
   782.6 x 1.25 = 978.3, rounded to 980 — because 794.3x remains
   comfortably under it; this re-measurement is not a rebaseline. It is
   deliberately far above the
   `30x` figure a reader might expect: a rewrite-clj node tree costs ~48x the
   source in retained bytes and ~750x in transient allocation, and this intent
   does not promise to replace that parser.

   This ceiling is tied to JDK and rewrite-clj allocation behaviour, not
   just to this leaf's code. rewrite-clj is version-pinned above (1.2.50);
   the JDK is not. If a JDK upgrade moves the measured ratio and this gate
   trips, that is a real signal, not noise to silence: re-baseline
   explicitly (measure the new floor with five samples after three
   warm-ups, set the ceiling to floor x 1.25, and update this docstring)
   — never bump the constant to make a red gate pass."
  980)

(def ^:private ^java.lang.management.ThreadMXBean thread-mx-bean
  (java.lang.management.ManagementFactory/getThreadMXBean))

(defn- allocated-bytes
  "Bytes allocated by the current thread since JVM start."
  ^long []
  (.getThreadAllocatedBytes
    ^com.sun.management.ThreadMXBean thread-mx-bean
    (.getId (Thread/currentThread))))

(defn- min-allocation
  "Smallest per-call allocation over `samples` calls, after `warmups` calls."
  [warmups samples f]
  (dotimes [_ warmups] (f))
  (reduce min
          (for [_ (range samples)]
            (let [before (allocated-bytes)
                  _ (f)
                  after (allocated-bytes)]
              (- after before)))))

(deftest thread-allocation-meter-is-available
  (is (instance? com.sun.management.ThreadMXBean thread-mx-bean)
      "the allocation witnesses below are void without this bean")
  (is (.isThreadAllocatedMemoryEnabled
        ^com.sun.management.ThreadMXBean thread-mx-bean)))


;; @spec MCP-OP-MEM-015
;; This witness asserts only the allocation ratio. `outline-parses-each-
;; file-exactly-once` below asserts the parse-count invariant as its own,
;; separate deftest — deliberately, so a JDK-driven allocation-ratio drift
;; on this test can never mask a second parse regression hiding on that one.
(deftest outline-of-one-file-allocates-within-its-ceiling
  (testing "outline-source builds no per-form source text it does not return"
    (let [source @fixture-source
          bytes @fixture-bytes
          allocated (min-allocation 3 5
                                    #(outline/outline-source
                                       fixture-path source))
          ratio (double (/ allocated bytes))]
      (is (<= ratio (double allocated-bytes-per-source-byte-ceiling))
          (str "outline-source allocated " allocated " bytes for "
               bytes " source bytes = " (format "%.1f" ratio)
               "x, ceiling " allocated-bytes-per-source-byte-ceiling "x. "
               "This ceiling is tied to JDK + rewrite-clj allocation "
               "behaviour; rewrite-clj is version-pinned, the JDK is not. "
               "If the JDK moved, re-baseline explicitly (measure the "
               "floor, set ceiling = floor x 1.25, update the docstring "
               "on allocated-bytes-per-source-byte-ceiling) — never bump "
               "the constant to make a red gate pass.")))))


;; @spec MCP-OP-MEM-015
(deftest outline-parses-each-file-exactly-once
  (testing "one outline call reaches the rewrite-clj parse entry once"
    (let [source @fixture-source
          calls (atom 0)
          real-of-string z/of-string]
      (with-redefs [z/of-string (fn [& args]
                                  (swap! calls inc)
                                  (apply real-of-string args))]
        (outline/outline-source fixture-path source))
      (is (= 1 @calls)
          (str "outline-source called rewrite-clj.zip/of-string "
               @calls " times for one file")))))

(deftest outline-of-a-fixture-is-non-trivial
  (testing "the fixture is large enough for the ceiling to mean something"
    (is (<= 40000 @fixture-bytes))
    (let [result (outline/outline-source fixture-path @fixture-source)]
      (is (pos? (count (:forms result))))
      (is (every? #(not (contains? % :source)) (:forms result)))
      (is (not (str/blank? (str (:ns result))))))))
