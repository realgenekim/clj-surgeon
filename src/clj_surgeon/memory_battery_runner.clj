(ns clj-surgeon.memory-battery-runner
  "Instrumented runner for the tree-scale memory battery.

  ONE JVM, one explicit -Xmx, a continuous heap sampler on its own thread, and
  every tree-scale operation on this branch run against synthetic trees at
  100 / 1,000 / 10,000 files, fresh and warm.

  This namespace is measurement apparatus. It changes no operation. It is not
  reachable from `make test`, `make test-fast`, or `make mcp-test`; see
  `clj-surgeon.memory-battery-test`, which asserts that.

  The verdict, its constants, and the table live in the pure
  `clj-surgeon.memory-battery` namespace so they can be witnessed in the
  millisecond fast suite without a JVM.

  Measurement note carried into every receipt: `heap-used-peak-mb` is a
  CONTINUOUSLY SAMPLED process-wide peak. It is a different quantity from the
  five-System/gc used-heap delta used to derive per-byte coefficients, and it
  is process-wide, not attributable to one operation.

  @spec MCP-OP-MEM-001
  @spec MCP-OP-MEM-011"
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [clj-surgeon.memory-battery :as battery])
  (:import
   (java.io OutputStream)
   (java.lang.management ManagementFactory)
   (java.security DigestOutputStream MessageDigest)
   (java.util.concurrent.atomic AtomicBoolean AtomicLong)))

(def ^:private mib (* 1024.0 1024.0))

(defn- bytes->mb ^double [^long b]
  (/ (Math/round (/ (double b) mib 0.1)) 10.0))

;; ============================================================
;; The tree-scale operations present on this branch
;; ============================================================

;; @spec MCP-OP-MEM-011
(def ops
  "Every operation on this branch that walks a repository tree.

  Each entry is measured as a black box through its public entrance; the
  battery never reaches inside an operation."
  [{:id :cli-ls-tree
    :entrance "clj-surgeon.core/run-ls-tree {:dir root :format :edn}"
    :site "src/clj_surgeon/core.clj:202-250, 321-339, 463-481"
    :note "CLI :ls-tree — discovers projects, then outlines every file"
    :run (fn [root]
           ((requiring-resolve 'clj-surgeon.core/run-ls-tree)
            {:dir root :format :edn}))}

   {:id :workspace-sources-read-all
    :entrance "clj-surgeon.mcp-workspace-sources/read-all"
    :site "src/clj_surgeon/mcp_workspace_sources.clj:11-20"
    :note (str "the one deterministic source universe shared by extraction "
               "planning and apply; extract.clj:447-479 repeats the same shape "
               "inline")
    :run (fn [root]
           ((requiring-resolve 'clj-surgeon.mcp-workspace-sources/read-all)
            (.toPath (io/file root))))}

   {:id :rename-ns-plan-narrow
    :entrance "clj-surgeon.rename/plan {:from \"membat.pkg000\"}"
    :site "src/clj_surgeon/rename.clj:110-160"
    :note (str "whole-project rename planning walk with a NARROW prefix: every "
               "file is walked and parsed, but only 100 of them ever match, so "
               "the plan stays small at every N. This arm measures the walk, "
               "not the plan.")
    :run (fn [root]
           ((requiring-resolve 'clj-surgeon.rename/plan)
            {:from "membat.pkg000" :to "membat.renamed000" :root root}))}

   ;; Without this arm the narrow one reports `ok` at 10,000 files and hides
   ;; the fact that it only matched 1 percent of them. A battery that grades a
   ;; query shape rather than the operation is worse than no battery.
   {:id :rename-ns-plan-full-match
    :entrance "clj-surgeon.rename/plan {:from \"membat\"}"
    :site "src/clj_surgeon/rename.clj:110-160"
    :note (str "the same walk with a prefix that matches EVERY file, so the "
               "plan, the ns renames, the require renames, and the file-move "
               "list all grow with N")
    :run (fn [root]
           ((requiring-resolve 'clj-surgeon.rename/plan)
            {:from "membat" :to "renamedbat" :root root}))}])

;; ============================================================
;; Heap instrumentation
;; ============================================================

(defn- used-bytes ^long []
  (let [rt (Runtime/getRuntime)]
    (- (.totalMemory rt) (.freeMemory rt))))

(defn- committed-bytes ^long []
  (.getCommitted (.getHeapMemoryUsage (ManagementFactory/getMemoryMXBean))))

(defn start-sampler!
  "Continuously sample process-wide used heap on a daemon thread. Allocation
  free in the sample loop so the sampler does not perturb what it measures."
  [^long interval-ms]
  (let [peak (AtomicLong. 0)
        committed-peak (AtomicLong. 0)
        running (AtomicBoolean. true)
        bump (fn [^AtomicLong a ^long v]
               (loop []
                 (let [p (.get a)]
                   (when (and (> v p) (not (.compareAndSet a p v)))
                     (recur)))))
        thread (doto (Thread.
                       ^Runnable
                       (fn []
                         (while (.get running)
                           (bump peak (used-bytes))
                           (bump committed-peak (committed-bytes))
                           (try (Thread/sleep interval-ms)
                                (catch InterruptedException _ nil))))
                       "membat-heap-sampler")
                 (.setDaemon true)
                 (.setPriority Thread/MAX_PRIORITY)
                 (.start))]
    {:peak peak :committed-peak committed-peak :running running :thread thread}))

(defn stop-sampler! [{:keys [^AtomicBoolean running ^Thread thread]}]
  (.set running false)
  (.interrupt thread))

(defn- reset-sampler! [{:keys [^AtomicLong peak ^AtomicLong committed-peak]}]
  (.set peak (used-bytes))
  (.set committed-peak (committed-bytes)))

(defn- quiesce!
  "Force the heap toward its retained floor. Measurement apparatus only; never
  called by an operation."
  []
  (dotimes [_ 4]
    (System/gc)
    (try (Thread/sleep 120) (catch InterruptedException _ nil))))

;; ============================================================
;; Bounded result hashing
;; ============================================================

(defn- hash-result
  "SHA-256 over the streamed printed form of a result. Streams into a digest
  sink so hashing a large result does not itself materialise a large string."
  [result]
  (let [md (MessageDigest/getInstance "SHA-256")
        dos (DigestOutputStream. (OutputStream/nullOutputStream) md)]
    (with-open [w (io/writer dos)]
      (binding [*out* w *print-length* nil *print-level* nil *print-dup* false]
        (pr result)
        (flush)))
    (str/join (map #(format "%02x" %) (.digest md)))))

;; ============================================================
;; One measurement
;; ============================================================

(defn- run-into-box!
  "Invoke the operation, storing its result in a mutable box so no named local
  in the measuring frame keeps the result reachable across the final GC."
  [^objects box run root]
  (try
    (aset box 0 (run root))
    {:ok true}
    (catch OutOfMemoryError _
      (aset box 0 nil)
      {:oom? true})
    (catch Throwable e
      (aset box 0 nil)
      {:error (str (.getName (class e)) ": " (.getMessage e))})))

(defn- measure-once
  "Measure one invocation. Returns a raw reading, not yet a battery cell."
  [sampler op root]
  (quiesce!)
  (let [start-bytes (used-bytes)
        _ (reset-sampler! sampler)
        box (object-array 1)
        t0 (System/nanoTime)
        outcome (run-into-box! box (:run op) root)
        wall-ms (quot (- (System/nanoTime) t0) 1000000)
        peak-bytes (.get ^AtomicLong (:peak sampler))
        committed-peak-bytes (.get ^AtomicLong (:committed-peak sampler))
        result-hash (when (:ok outcome) (hash-result (aget box 0)))
        ;; retained while the result is still referenced: what a receipt costs
        _ (quiesce!)
        held-bytes (used-bytes)
        _ (aset box 0 nil)
        _ (quiesce!)
        after-bytes (used-bytes)]
    (merge outcome
           {:wall-ms wall-ms
            :heap-start-mb (bytes->mb start-bytes)
            :heap-used-peak-mb (bytes->mb peak-bytes)
            :heap-committed-peak-mb (bytes->mb committed-peak-bytes)
            ;; held - start: everything the call still holds while the result is
            ;; referenced. This is the receipt's cost PLUS any cache or leak the
            ;; call created, which is why the next two are recorded separately.
            :heap-result-retained-mb (bytes->mb (max 0 (- held-bytes start-bytes)))
            ;; held - after-release: result-EXCLUSIVE retention. What actually
            ;; went away when the result was dropped, so it was the result's.
            :heap-held-after-release-mb (bytes->mb (max 0 (- held-bytes after-bytes)))
            ;; after-release - start: PERSISTENT growth. What the call left
            ;; behind for good — the leak/cache figure, and the one gated.
            :heap-after-release-start-mb (bytes->mb (max 0 (- after-bytes start-bytes)))
            :heap-after-gc-mb (bytes->mb after-bytes)
            :result-hash result-hash})))

(defn- aggregate
  "Fold reps into one cell. The worst reading wins for every heap field; wall
  is the median. Per-rep detail is preserved under :reps."
  [op n phase readings reference-hash]
  (let [walls (sort (map :wall-ms readings))
        hashes (set (keep :result-hash readings))]
    {:op (:id op)
     :n n
     :phase phase
     :reps (count readings)
     :wall-ms (nth walls (quot (count walls) 2))
     :wall-ms-all (vec walls)
     :heap-start-mb (apply min (map :heap-start-mb readings))
     :heap-used-peak-mb (apply max (map :heap-used-peak-mb readings))
     :heap-committed-peak-mb (apply max (map :heap-committed-peak-mb readings))
     :heap-result-retained-mb (apply max (map :heap-result-retained-mb readings))
     :heap-held-after-release-mb (apply max (map :heap-held-after-release-mb readings))
     :heap-after-release-start-mb (apply max (map :heap-after-release-start-mb readings))
     :heap-after-gc-mb (apply max (map :heap-after-gc-mb readings))
     ;; No admission accountant exists on this branch, so no operation can
     ;; report an attributable reserved peak. Left absent on purpose: the
     ;; verdict then reports :reserved-peak-over-budget as UNMEASURED rather
     ;; than passing it on the sampled process-wide number, which is a
     ;; different quantity.
     :heap-reserved-peak-mb nil
     :oom? (boolean (some :oom? readings))
     :errors (vec (keep :error readings))
     :result-hash (cond
                    (empty? hashes) nil
                    (= 1 (count hashes)) (first hashes)
                    :else (str "nondeterministic:" (count hashes)))
     :reference-hash reference-hash}))

;; ============================================================
;; Trees and reference hashes
;; ============================================================

(defn- tree-path [root n] (str (io/file root (str n))))

(defn- read-manifest [root n]
  (let [f (io/file (tree-path root n) "manifest.edn")]
    (when (.exists f)
      (edn/read-string (slurp f)))))

(defn- reference-file [root] (io/file root "reference-hashes.edn"))

(defn- read-reference
  "The cached reference document, or nil when there is none. Legacy files hold
  the bare {op {n hash}} map with no attestation; they are returned as they are
  so `battery/reference-staleness` can name them :unattested-reference rather
  than this reader guessing."
  [root]
  (let [f (reference-file root)]
    (when (.exists f) (edn/read-string (slurp f)))))

;; ============================================================
;; Attestation — the identity of this experiment
;; ============================================================

(defn- sha256-hex ^String [^String s]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest md (.getBytes s "UTF-8"))))))

(defn- file-digest
  "path:sha256 for one file, or nil when it cannot be read."
  [^java.io.File f]
  (when (.isFile f)
    (str (.getPath f) ":" (sha256-hex (slurp f)))))

(defn- tree-digest
  "Digest over every Clojure source under `dir`, by path and content. Returns
  :unavailable rather than a plausible-looking digest when the directory is not
  there, so a run that cannot establish its own identity fails closed."
  [dir]
  (let [d (io/file dir)]
    (if-not (.isDirectory d)
      :unavailable
      (let [entries (->> (file-seq d)
                         (filter #(re-find #"\.cljc?$" (.getName ^java.io.File %)))
                         (keep file-digest)
                         sort)]
        (if (empty? entries) :unavailable (sha256-hex (str/join "\n" entries)))))))

(defn- ops-catalogue-digest []
  (sha256-hex (pr-str (mapv #(select-keys % [:id :entrance :site]) ops))))

(defn attestation
  "What this run is: which arms, which operation source, which generator, which
  corpus, which JVM. The cached unbounded reference is bound to all of it.

  `:head-sha` is recorded for forensics and is NOT compared — see
  `battery/attested-fields` for why."
  [manifests]
  {:ops (mapv :id ops)
   :ops-digest (ops-catalogue-digest)
   :src-digest (tree-digest "src/clj_surgeon")
   :generator-digest (or (some-> (file-digest (io/file "bench/memory_battery/generate_tree.clj"))
                                 sha256-hex)
                         :unavailable)
   :corpus-digests (into (sorted-map)
                         (for [[n m] manifests] [n (:digest m)]))
   :jvm (System/getProperty "java.version")
   :head-sha (or (not-empty (System/getenv "MEMBAT_HEAD_SHA")) "unknown")})

;; ============================================================
;; Driver
;; ============================================================

(defn- env
  ([k d] (or (not-empty (System/getenv k)) d)))

(defn- parse-xmx-mb
  "Configured max heap in MiB, read from the live JVM, not from the flag."
  []
  (long (/ (.getMax (.getHeapMemoryUsage (ManagementFactory/getMemoryMXBean)))
           1024 1024)))

(defn- refuse [message detail]
  (binding [*out* *err*]
    (println "REFUSED:" message)
    (when detail (println (pr-str detail))))
  (:refusal battery/exit-codes))

(defn run-battery
  "Run the battery. `mode` is :battery (the gate) or :reference (populate the
  unbounded reference hashes at a large heap)."
  [{:keys [root scales reps mode op-timeout-ms sample-interval-ms]}]
  (let [xmx-mb (parse-xmx-mb)
        manifests (into {} (for [n scales] [n (read-manifest root n)]))
        missing (remove #(let [m (get manifests %)]
                           (and m (= % (:files m))))
                        scales)
        att (attestation manifests)
        reference (read-reference root)
        staleness (battery/reference-staleness att reference)]
    (cond
      (not (.isDirectory (io/file root)))
      {:exit (refuse (str "MEMBAT_ROOT is not a directory: " root) nil)}

      (seq missing)
      {:exit (refuse "synthetic trees are missing or incomplete"
                     {:root root :missing (vec missing)
                      :remedy "make memory-battery-generate"})}

      (and (= :reference mode) (< xmx-mb 2048))
      {:exit (refuse "the reference pass needs an unbounded heap"
                     {:xmx-mb xmx-mb :required-min-mb 2048})}

      (and (= :battery mode) (> xmx-mb 1024))
      {:exit (refuse "the battery must run at the bounded budget"
                     {:xmx-mb xmx-mb :expected "<= 1024 (MEMBAT_XMX, default 512m)"})}

      ;; Output parity means nothing against a reference that measured other
      ;; code over another corpus on another JVM. Refuse; never quietly compare.
      (and (= :battery mode) staleness)
      {:exit (refuse (str "the cached unbounded reference is not attested to "
                          "this run (" (name (:reason staleness)) ")")
                     (assoc staleness
                            :reference-file (str (reference-file root))
                            :remedy "make memory-battery-reference"))}

      :else
      (let [sampler (start-sampler! sample-interval-ms)
            hashes (:hashes reference)
            started (java.time.Instant/now)]
        (try
          (let [;; Load every operation's classes and namespaces BEFORE the
                ;; first measured cell, so a "fresh" cell means a fresh tree
                ;; after a GC, not a cold JVM. Without this the first cell of
                ;; each op carries the whole clj-surgeon class-loading cost and
                ;; is not comparable with any later cell.
                _ (do (println "  warming the JVM (results discarded)...")
                      (doseq [op ops]
                        (measure-once sampler op (tree-path root (apply min scales))))
                      (flush))
                cells
                (reduce
                  (fn [acc op]
                    (:cells
                      (reduce
                        (fn [{:keys [cells stop?] :as st} n]
                          (let [tree (tree-path root n)
                                ref-hash (get-in hashes [(:id op) n])]
                            (if stop?
                              (do (println (format "  %-28s N=%-6d SKIPPED (a smaller N already exceeded MEMBAT_OP_TIMEOUT_MS=%d)"
                                                   (name (:id op)) n op-timeout-ms))
                                  (flush)
                                  (update st :cells conj
                                          {:op (:id op) :n n :phase :fresh
                                           :skipped? true
                                           :skip-reason :wall-budget-exceeded}))
                              (let [fresh (measure-once sampler op tree)
                                    blown? (> (:wall-ms fresh) op-timeout-ms)
                                    warm (when-not blown?
                                           (vec (repeatedly
                                                  (max 0 (dec reps))
                                                  #(measure-once sampler op tree))))]
                                (println (format "  %-28s N=%-6d fresh %7d ms  peak %7.1f MB  held %7.1f MB%s"
                                                 (name (:id op)) n
                                                 (:wall-ms fresh)
                                                 (:heap-used-peak-mb fresh)
                                                 (:heap-result-retained-mb fresh)
                                                 (cond
                                                   (:oom? fresh) "  OOM"
                                                   (:error fresh) (str "  ERROR " (:error fresh))
                                                   blown? "  (wall budget exceeded; warm reps and larger N skipped)"
                                                   :else "")))
                                (flush)
                                {:stop? blown?
                                 :cells (cond-> (conj cells (aggregate op n :fresh [fresh] ref-hash))
                                          (seq warm)
                                          (conj (aggregate op n :warm warm ref-hash)))}))))
                        {:cells acc :stop? false}
                        (sort scales))))
                  []
                  ops)
                errors (mapcat :errors cells)
                observation {:xmx-mb xmx-mb
                             :mode mode
                             :root root
                             :attestation att
                             :reps reps
                             :scales (vec scales)
                             :sample-interval-ms sample-interval-ms
                             :op-timeout-ms op-timeout-ms
                             :started (str started)
                             :finished (str (java.time.Instant/now))
                             :jvm (System/getProperty "java.version")
                             :measurement-note
                             (str "heap-used-peak-mb is a continuously sampled "
                                  "process-wide used-heap PEAK at "
                                  sample-interval-ms
                                  " ms; it is not a post-GC delta and is not "
                                  "attributable to a single operation. "
                                  "heap-reserved-peak-mb is absent because no "
                                  "operation on this branch has an admission "
                                  "accountant.")
                             :ops (mapv #(select-keys % [:id :entrance :site :note]) ops)
                             :trees (into {} (for [n scales]
                                               [n (dissoc (get manifests n) :reused)]))
                             :cells (mapv (fn [c]
                                            (let [m (get manifests (:n c))]
                                              (assoc c
                                                     :files (:files m)
                                                     :bytes (:bytes m)
                                                     :largest-file-bytes
                                                     (:largest-file-bytes m))))
                                          cells)}]
            (assoc observation
                   :verdict (battery/verdict observation)
                   :tool-errors (vec errors)))
          (finally (stop-sampler! sampler)))))))

(defn- write-receipt! [root observation]
  (let [dir (io/file root "receipts")
        stamp (-> (str (java.time.Instant/now))
                  (str/replace ":" "") (str/replace "-" ""))
        f (io/file dir (str stamp "-" (name (:mode observation)) ".edn"))]
    (.mkdirs dir)
    (spit f (with-out-str (pprint/pprint observation)))
    (spit (io/file dir (str "latest-" (name (:mode observation)) ".edn"))
          (with-out-str (pprint/pprint observation)))
    (str f)))

(defn- run-attest!
  "Seconds-scale: is the cached unbounded reference attested to THIS run?

  Exists so `make memory-battery` can rebuild a stale reference instead of
  refusing halfway through a minutes-long battery. Exits 0 when fresh, and the
  refusal code otherwise, naming the fields that differ."
  [root scales]
  (let [manifests (into {} (for [n scales] [n (read-manifest root n)]))
        att (attestation manifests)
        reference (read-reference root)
        staleness (battery/reference-staleness att reference)]
    (if staleness
      (do (binding [*out* *err*]
            (println "reference NOT attested to this run:"
                     (name (:reason staleness)))
            (println (pr-str (select-keys staleness [:fields :detail]))))
          (:refusal battery/exit-codes))
      (do (println "reference attested:" (pr-str (select-keys att battery/attested-fields)))
          (:pass battery/exit-codes)))))

(defn -main
  "Entry point for `make memory-battery`."
  [& _args]
  (let [root (env "MEMBAT_ROOT" "/home/forge/tmp/membat")
        mode (keyword (env "MEMBAT_MODE" "battery"))
        scales (mapv #(Long/parseLong (str/trim %))
                     (str/split (env "MEMBAT_SCALES" "100,1000,10000") #","))
        reps (Long/parseLong (env "MEMBAT_REPS" "5"))
        op-timeout-ms (Long/parseLong (env "MEMBAT_OP_TIMEOUT_MS" "600000"))
        sample-interval-ms (Long/parseLong (env "MEMBAT_SAMPLE_MS" "5"))]
    (when (= :attest mode)
      (System/exit (run-attest! root scales)))
    (println (format "memory battery: mode=%s root=%s scales=%s reps=%d Xmx=%dm"
                     (name mode) root (pr-str scales) reps (parse-xmx-mb)))
    (let [observation (run-battery {:root root :scales scales :reps reps
                                    :mode mode :op-timeout-ms op-timeout-ms
                                    :sample-interval-ms sample-interval-ms})]
      (if-let [exit (and (:exit observation) (not (:cells observation))
                         (:exit observation))]
        (System/exit exit)
        (let [receipt (write-receipt! root observation)]
          (if (= :reference mode)
            (let [hashes (reduce (fn [acc c]
                                   (assoc-in acc [(:op c) (:n c)] (:result-hash c)))
                                 {}
                                 (:cells observation))]
              ;; The hashes are only meaningful together with what produced
              ;; them, so they are never written without their attestation.
              (spit (reference-file root)
                    (with-out-str
                      (pprint/pprint {:attestation (:attestation observation)
                                      :hashes hashes})))
              (println)
              (println "reference hashes written to" (str (reference-file root)))
              (println "attested to" (pr-str (select-keys (:attestation observation)
                                                          [:head-sha :jvm])))
              (println "receipt:" receipt)
              (System/exit (if (seq (:tool-errors observation))
                             (:tool-failure battery/exit-codes)
                             (:pass battery/exit-codes))))
            (do
              (println)
              (println (battery/render-table observation))
              (println)
              (println "receipt:" receipt)
              (System/exit
                (cond
                  (seq (:tool-errors observation)) (:tool-failure battery/exit-codes)
                  :else (get-in observation [:verdict :exit]))))))))))
