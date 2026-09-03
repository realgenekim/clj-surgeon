(ns shape-probe
  "Child process for the MEM-005 red witness: outline ONE file in a JVM whose
  heap ceiling is set on the command line, and report what happened.

  Reports facts, never a verdict (house rule: the probe emits facts, the fold
  emits verdicts). The parent applies the pass lines.

  Args: <source-path> <warmups>

  `warmups` matters and is not a nicety. The rewrite-clj parse of a deeply
  nested form recurses once per level; interpreted frames are far larger than
  JIT-compiled ones, so the SAME file at the SAME -Xmx either throws
  StackOverflowError (cold) or completes while consuming hundreds of MB (warm).
  Both branches are measured, because a caller cannot choose which one it gets."
  (:require [clj-surgeon.outline :as outline]
            [clj-surgeon.parse-admission :as admission]))

(def ^:private warmup-source-path "src/clj_surgeon/mcp_hot_verify.clj")

(defn- sampler
  "Continuously sampled used-heap peak, on its own thread. Matches the memory
  battery's instrument: a post-GC delta cannot see a transient parse peak."
  []
  (let [peak (java.util.concurrent.atomic.AtomicLong. 0)
        running (java.util.concurrent.atomic.AtomicBoolean. true)
        rt (Runtime/getRuntime)
        t (doto (Thread.
                  (fn []
                    (while (.get running)
                      (let [used (- (.totalMemory rt) (.freeMemory rt))]
                        (when (> used (.get peak)) (.set peak used)))
                      (try (Thread/sleep 5) (catch InterruptedException _ nil)))))
            (.setDaemon true)
            (.setPriority Thread/MAX_PRIORITY)
            (.start))]
    {:peak peak :running running :thread t}))

(defn -main [path warmups]
  (let [n (Long/parseLong warmups)]
    (when (pos? n)
      (let [warm (slurp warmup-source-path)]
        (dotimes [_ n] (outline/outline-source warmup-source-path warm))))
    (let [{:keys [^java.util.concurrent.atomic.AtomicLong peak
                  ^java.util.concurrent.atomic.AtomicBoolean running
                  ^Thread thread]} (sampler)
          bytes (.length (java.io.File. ^String path))
          ;; The source and the result live in a mutable box, never in a named
          ;; local, so the error branches can drop them before the reporting
          ;; code allocates. Under a real OutOfMemoryError even `format` and
          ;; `pr-str` can throw again. The parent additionally classifies a
          ;; child that died without reporting, from its stderr: a JVM out of
          ;; heap cannot always describe itself.
          box (object-array 1)
          t0 (System/nanoTime)
          outcome (try
                    (aset box 0 (slurp path))
                    (aset box 0 (outline/outline-source
                                  path ^String (aget box 0)))
                    (let [forms (count (:forms (aget box 0)))]
                      (aset box 0 nil)
                      {:outcome :completed :forms forms})
                    (catch OutOfMemoryError _
                      (aset box 0 nil) (System/gc) {:outcome :out-of-memory})
                    (catch StackOverflowError _
                      (aset box 0 nil) (System/gc) {:outcome :stack-overflow})
                    (catch clojure.lang.ExceptionInfo e
                      (aset box 0 nil)
                      (if (= :parser_admission_refused (:refusal (ex-data e)))
                        {:outcome :parser-admission-refused
                         :reason (:reason (ex-data e))
                         :limit (:limit (ex-data e))
                         :observed (:observed (ex-data e))}
                        {:outcome :threw :class "clojure.lang.ExceptionInfo"}))
                    (catch Throwable t
                      (aset box 0 nil)
                      {:outcome :threw :class (.getName (class t))}))
          wall-ms (quot (- (System/nanoTime) t0) 1000000)
          ;; The control's OWN cost, measured separately on the same string, so
          ;; the wall column above (which includes reading the file) cannot be
          ;; mistaken for it.
          scan-ms (let [src (slurp path)
                        t1 (System/nanoTime)]
                    (admission/scan-shape src)
                    (quot (- (System/nanoTime) t1) 1000000))]
      (.set running false)
      (.join thread 500)
      (println (pr-str (assoc outcome
                              :path path
                              :source-bytes bytes
                              :warmups n
                              :peak-mb (Double/parseDouble
                                         (format "%.1f" (/ (double (.get peak))
                                                           1048576.0)))
                              :wall-ms wall-ms
                              :scan-ms scan-ms)))
      (flush)
      (System/exit 0))))
