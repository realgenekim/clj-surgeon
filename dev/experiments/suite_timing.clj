(ns suite-timing
  "ROUND-ONE MEASUREMENT HARNESS for the JVM test-suite spike
   (docs/observations/2026-09-04-suite-spike-spec.md).

   Read-only with respect to src/ and test/: this namespace lives under
   dev/experiments (already on the :clj-surgeon/mcp-test classpath) and
   re-implements `clj-surgeon.mcp-test-runner`'s -main with per-namespace
   instrumentation. It does NOT modify the runner it measures.

   WHAT IT MEASURES, per namespace, in the SAME order and the SAME single
   JVM the production runner uses:
     - wall ms around `clojure.test/test-ns`
     - tests / assertions / fail / error (clojure.test's own counters)
     - subprocesses spawned WHILE that namespace ran, and their argv[0..2]
     - entries created under the run's isolated java.io.tmpdir root while
       that namespace ran (and how many survived it)

   SUBPROCESS DETECTION is done by SAMPLING
   `ProcessHandle/current .descendants` from a daemon thread rather than by
   redefining `clojure.java.shell/sh` or `babashka.process/process`: a test
   that constructs a `ProcessBuilder` directly, or shells out from inside a
   library, names nothing a with-redefs could see. Sampling watches the
   kernel's answer instead of the source's spelling. Its known blind spot is
   a subprocess shorter-lived than the sample interval, so the run also
   emits a static scan (see dev/experiments/suite_classify.clj) and the two
   are reported side by side; a namespace flagged by either is treated as a
   spawner.

   USAGE (no deps.edn edit -- the alias's :main-opts is overridden by a
   second alias supplied on the command line):

     clojure -Sdeps '{:aliases {:suite-timing {:main-opts [\"-m\" \"suite-timing\"]}}}' \\
             -M:clj-surgeon/mcp-test:suite-timing <out.edn>"
  (:require
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.test :as t]))

(def ^:private runner-source "test/clj_surgeon/mcp_test_runner.clj")

(defn runner-namespaces
  "The namespace symbols the production runner passes to `run-tests`, in the
   production runner's own order, read from its SOURCE rather than restated
   here -- so this harness cannot silently drift from the lane it measures."
  []
  (let [src (slurp runner-source)
        body (second (str/split src #"\(run-tests" 2))]
    (->> (re-seq #"'(clj-surgeon\.[A-Za-z0-9._-]+)" body)
         (map second)
         (map symbol)
         vec)))

;; ---------------------------------------------------------------------------
;; subprocess sampler

(defn- descendant-snapshot
  []
  (try
    (into #{}
          (map (fn [^java.lang.ProcessHandle h]
                 (let [info (.info h)
                       cmd (or (.orElse (.command info) nil) "?")
                       args (vec (or (.orElse (.arguments info) nil) []))]
                   [(.pid h) (str/join " " (take 3 (cons cmd args)))])))
          (iterator-seq (.iterator (.descendants (java.lang.ProcessHandle/current)))))
    (catch Throwable _ #{})))

(defn- start-sampler!
  "Daemon thread sampling live descendant processes every `interval-ms` and
   accumulating them into `current-ns`'s bucket. Returns a stop fn."
  [current-ns acc interval-ms]
  (let [running (atom true)
        thread (Thread.
                 ^Runnable
                 (fn []
                   (while @running
                     (let [ns-key @current-ns
                           snap (descendant-snapshot)]
                       (when (and ns-key (seq snap))
                         (swap! acc update ns-key (fnil into #{}) (map second snap))))
                     (Thread/sleep (long interval-ms)))))]
    (.setDaemon thread true)
    (.start thread)
    (fn [] (reset! running false))))

;; ---------------------------------------------------------------------------

(defn- run-one
  [ns-sym current-ns]
  (require ns-sym)
  (let [tmp-before (tmp-leak/tmp-entries)
        _ (reset! current-ns ns-sym)
        start (System/nanoTime)
        counters (t/test-ns ns-sym)
        wall-ms (quot (- (System/nanoTime) start) 1000000)
        _ (reset! current-ns nil)
        tmp-after (tmp-leak/tmp-entries)]
    {:ns ns-sym
     :wall-ms wall-ms
     :tests (:test counters 0)
     :assertions (+ (:pass counters 0) (:fail counters 0) (:error counters 0))
     :pass (:pass counters 0)
     :fail (:fail counters 0)
     :error (:error counters 0)
     :tmp-created (sort (tmp-leak/leaked-entries tmp-before tmp-after))}))

(defn -main
  [& args]
  (let [{:keys [refused root]}
        (tmp-leak/secure-tmpdir! {:main-ns "suite-timing"} args)
        _ (when refused (System/exit 97))
        out-path (or (first args) "suite-timing.edn")
        current-ns (atom nil)
        procs (atom {})
        stop! (start-sampler! current-ns procs 40)
        namespaces (runner-namespaces)
        started (System/currentTimeMillis)
        rows (mapv #(run-one % current-ns) namespaces)
        total-ms (- (System/currentTimeMillis) started)
        _ (stop!)
        rows (mapv (fn [r] (assoc r :subprocesses (sort (get @procs (:ns r) #{})))) rows)
        result {:generated-at (str (java.time.Instant/now))
                :tmp-root (str root)
                :total-wall-ms total-ms
                :namespace-count (count rows)
                :rows rows}]
    (with-open [w (io/writer (io/file out-path))]
      (binding [*out* w] (pp/pprint result)))
    (println)
    (println (format "SUITE-TIMING total=%.1fs namespaces=%d out=%s"
                     (/ total-ms 1000.0) (count rows) out-path))
    (doseq [r (->> rows (sort-by :wall-ms >) (take 20))]
      (println (format "%8.2fs  %-58s tests=%-4d asserts=%-5d procs=%d"
                       (/ (:wall-ms r) 1000.0) (str (:ns r))
                       (:tests r) (:assertions r) (count (:subprocesses r)))))
    (let [failures (reduce + (map #(+ (:fail %) (:error %)) rows))]
      (println (format "FAILURES+ERRORS=%d" failures))
      (System/exit (min 1 failures)))))
