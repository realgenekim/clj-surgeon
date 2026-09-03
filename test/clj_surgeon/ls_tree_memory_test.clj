(ns clj-surgeon.ls-tree-memory-test
  "Retained-heap witnesses for the bounded `ls-tree` output budget, and the
   differential that proves the streaming encoder reproduces the batch one.

   These are JVM-only: they force garbage collection and read
   `Runtime.totalMemory - Runtime.freeMemory`, which is the memory battery's
   own held/excl method reduced to one process and one fixture. They live in
   the MCP JVM suite beside `clj-surgeon.outline-memory-test`.

   The claim under test is a DERIVATIVE, not a level: what an `ls-tree` result
   retains must track the result ceiling `R` and NOT the number of files
   scanned. A level would be meaningless — a bigger corpus makes any absolute
   figure bigger — which is exactly how the unbounded encoder passed every
   existing test while retaining 94.0 MB at 10,000 files."
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.core :as core]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ============================================================
;; The battery's held/excl method, one process
;; ============================================================

(defn- used-bytes ^long []
  (let [rt (Runtime/getRuntime)]
    (- (.totalMemory rt) (.freeMemory rt))))

(defn- settle!
  "Five `System/gc` calls, as the battery does. `System/gc` is advisory, so
   this is a best-effort quiesce and the pass lines below carry slack for it."
  []
  (dotimes [_ 5]
    (System/gc)
    (Thread/sleep 10)))

(defn- held-bytes
  "After-GC used heap while `f`'s result is still REFERENCED, minus the
   after-GC baseline taken before the call. The result is pinned in an array
   so the JIT cannot drop it, then released and re-measured, which is the
   battery's `held` / `after-release` pair."
  [f]
  (settle!)
  (let [before (used-bytes)
        box (object-array 1)]
    (aset box 0 (f))
    (settle!)
    (let [held (- (used-bytes) before)]
      (aset box 0 nil)
      (settle!)
      {:held held
       :after-release (- (used-bytes) before)})))

(def ^:private mib (* 1024.0 1024.0))
(defn- mb [^long b] (/ (Math/round (/ (double b) mib 0.01)) 100.0))

;; ============================================================
;; Fixture
;; ============================================================

(def ^:private forms-per-file 60)

(defn- fixture-source [i]
  (str (format "(ns fixt.mod%05d\n  (:require [clojure.string :as str]))\n\n" i)
       (str/join
         (for [j (range forms-per-file)]
           (format "(defn fn%05d-%02d\n  \"doc %d\"\n  [alpha beta]\n  (str alpha beta %d))\n\n"
                   i j j (+ i j))))))

(defn- make-fixture!
  "A project of `n` fixture files. Deliberately uniform: every record costs the
   same, so a retained-heap difference between two runs is a difference in the
   NUMBER of records retained and nothing else."
  [n]
  (let [dir (str (fs/create-temp-dir {:prefix "ls-tree-mem"}))
        src (str dir "/src/fixt")]
    (fs/create-dirs src)
    (spit (str dir "/deps.edn") "{:paths [\"src\"]}")
    (dotimes [i n]
      (spit (format "%s/mod%05d.clj" src i) (fixture-source i)))
    dir))

(def ^:private scan-n
  "Files in the large fixture. It must exceed `R` plus the materialiser window
   by enough that the unbounded encoder cannot pass the bound by accident: on
   a 16-core box the window alone is 72 records, so a 200-file fixture would
   leave the two paths within noise of each other."
  400)

(def ^:private ceiling 50)

(def ^:private slack-bytes
  "Heap-measurement slack. The battery observed at most 0.2 MiB of after-GC
   jitter in a bounded arm; this is 2.5x that. It must stay well BELOW the
   signal — the unbounded encoder retains about (400 - 50) records more than
   the bounded one — or the witness stops falsifying anything."
  (long (* 0.5 mib)))

;; ============================================================
;; The witnesses
;; ============================================================

;; @spec MCP-OP-MEM-003
(deftest retained-heap-tracks-the-ceiling-and-not-the-file-count
  (let [big (make-fixture! scan-n)
        small (make-fixture! ceiling)]
    (try
      (let [warm (core/run-ls-tree {:dir small :format :edn})
            _ (is (seq warm) "fixture scans")
            big-bounded (held-bytes #(core/run-ls-tree
                                       {:dir big :format :edn
                                        :max-results ceiling}))
            small-bounded (held-bytes #(core/run-ls-tree
                                         {:dir small :format :edn
                                          :max-results ceiling}))
            big-unbounded (held-bytes #(core/run-ls-tree
                                         {:dir big :format :edn
                                          :max-results scan-n}))
            per-record (/ (double (:held big-unbounded)) scan-n)
            window (long core/outline-window-size)
            bound (long (+ (* (+ ceiling window) per-record) slack-bytes))]

        (testing "the encoder holds R records, not N"
          (is (<= (:held big-bounded)
                  (+ (:held small-bounded) slack-bytes))
              (str "scanning " scan-n " files at ceiling " ceiling
                   " retained " (mb (:held big-bounded)) " MB; scanning only "
                   ceiling " files at the same ceiling retained "
                   (mb (:held small-bounded)) " MB. Retention must depend on "
                   "the ceiling, not on the number of files walked.")))

        (testing "retention is bounded by the output encoder plus the active
                  materialiser window"
          (is (<= (:held big-bounded) bound)
              (str "retained " (mb (:held big-bounded)) " MB against a bound of "
                   (mb bound) " MB = (" ceiling " encoder records + " window
                   " window records) x " (long per-record) " B/record + "
                   (mb slack-bytes) " MB slack")))

        (testing "the unbounded scan is the control: it retains what the
                  bounded one refuses to"
          (is (> (:held big-unbounded)
                 (+ (:held big-bounded) slack-bytes))
              (str "unbounded " (mb (:held big-unbounded))
                   " MB vs bounded " (mb (:held big-bounded))
                   " MB — if these are equal the witness is measuring nothing")))

        (testing "the scan leaves nothing behind once the result is released"
          (is (<= (:after-release big-bounded) slack-bytes)
              (str "after releasing the result the scan still held "
                   (mb (:after-release big-bounded)) " MB — a cache or a leak"))))
      (finally
        (fs/delete-tree big)
        (fs/delete-tree small)))))

;; ============================================================
;; Differential: the streaming encoder against the batch encoder
;; ============================================================

(defn- batch-result
  "The unbounded batch encoder — discover, outline everything into one retained
   vector, format the whole set. This is the path `run-ls-tree` used before the
   output budget, and it is the oracle the streaming encoder must reproduce
   byte for byte."
  [dir output-format]
  (let [abs (str (fs/absolutize dir))
        projects (#'core/outline-all-files (#'core/discover-projects abs))]
    (if (= :edn output-format)
      (core/format-ls-tree-edn projects abs)
      (core/format-ls-tree-text projects abs))))

;; @spec MCP-OP-MEM-003
(deftest streaming-and-batch-encoders-agree-over-this-repository
  (testing "every result under the ceiling is byte-identical to the batch path"
    (doseq [dir ["src" "test"]]
      (let [expected-text (batch-result dir :text)
            actual-text (core/run-ls-tree {:dir dir})
            expected-edn (batch-result dir :edn)
            actual-edn (core/run-ls-tree {:dir dir :format :edn})]
        (is (= expected-text actual-text)
            (str dir ": streamed text differs from the batch text"))
        (is (= expected-edn actual-edn)
            (str dir ": streamed EDN differs from the batch EDN"))
        (is (= (mapv :file expected-edn) (mapv :file actual-edn))
            (str dir ": record ORDER differs"))))))

;; ============================================================
;; The DECLARED worker bound, measured
;;
;; Sol finding 6/9: the streaming materialiser documents a pool of
;; `outline-pool-size` (18 on a 16-core box) and a window of `4 x pool`, and
;; measured 32 outlines simultaneously active. `pmap` realises its input in
;; CHUNKS of 32, so chunked realisation outruns the bound the docstring
;; states. Nothing was wrong with the output; what was wrong is that the
;; parser-peak bound the whole memory row rests on was a claim, not a control.
;;
;; The window must also NOT scale with N, or boundedness is defeated by
;; growth. `outline-window-size` is `4 x pool` and independent of the corpus;
;; this witness pins that too.
;; ============================================================

(defn- max-concurrent-outlines
  "Run `f` with `safe-outline` instrumented, and return the greatest number of
   outlines that were ever simultaneously in flight.

   Each outline holds for 2 ms so overlap is observable at all: without a
   dwell, workers finish faster than they are scheduled and every
   implementation looks serial."
  [f]
  (let [active (atom 0)
        peak (atom 0)
        v #'core/safe-outline
        real @v]
    (alter-var-root
      v (constantly
          (fn [file]
            (let [n (swap! active inc)]
              (swap! peak max n)
              (try (Thread/sleep 2) (real file)
                   (finally (swap! active dec)))))))
    (try (f) (finally (alter-var-root v (constantly real))))
    @peak))

;; @spec MCP-OP-MEM-003
(deftest outline-concurrency-never-exceeds-the-declared-pool
  (let [dir (make-fixture! 240)]
    (try
      (let [pool core/outline-pool-size
            peak (max-concurrent-outlines
                   #(core/run-ls-tree {:dir dir :format :edn :max-results 200}))]
        (is (pos? peak) "the meter saw the scan at all")
        (is (> peak 1)
            "the scan really is concurrent — a serial peak would make the
             bound below vacuous")
        (is (<= peak pool)
            (format "measured %d outlines active against a declared pool of %d"
                    peak pool)))
      (finally (fs/delete-tree dir)))))

;; @spec MCP-OP-MEM-003
(deftest the-materialisation-window-does-not-scale-with-n
  (is (= (* 4 core/outline-pool-size) core/outline-window-size)
      "the window is a fixed multiple of the pool")
  (let [small (make-fixture! 20)
        large (make-fixture! 240)]
    (try
      (let [w core/outline-window-size]
        (core/run-ls-tree {:dir small :format :edn})
        (is (= w core/outline-window-size))
        (core/run-ls-tree {:dir large :format :edn :max-results 200})
        (is (= w core/outline-window-size)
            "a bigger corpus does not widen the window; if it did, retained
             heap would grow with N through the materialiser even with the
             result ceiling holding"))
      (finally (fs/delete-tree small) (fs/delete-tree large)))))
