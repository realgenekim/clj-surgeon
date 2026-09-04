(ns clj-surgeon.measured-invariant-test
  "The ruling as an INVARIANT: a measured field enters a receipt ONLY through
  the partition, and no `System/exit` lives outside a CLI entrypoint.

  The first landing made the ruling true at four sites. A review then showed
  the same measured data reaching the hash subject by a SECOND route — the
  shared MCP operation finalizer attached its wall-clock reading as a top-level
  `:elapsed_ms`, and `hashed-channel`, which removes only a block keyed
  `:measured`, carried it straight through:

      {:public-result   {:ok true, :receipt {:stable :fact}, :elapsed_ms 2.5},
       :hashed-channel  {:ok true, :receipt {:stable :fact}, :elapsed_ms 2.5},
       :elapsed-survives-hash true}

  A site fix answers one site. What follows is the rule instead:

  1. the publication boundary partitions every measured field it publishes;
  2. a SOURCE SCAN enumerates every clock read in `src/` and classifies it, so
     a clock site nobody classified fails this file rather than shipping;
  3. the parity hash of a real public result is stable across two runs whose
     clocks tick differently;
  4. `System/exit` appears only inside a `-main`, because a library that exits
     kills the caller — and the caller is the MCP server.

  @spec MCP-OP-MEM-003
  @spec MCP-OP-MEM-005
  @spec MCP-OP-MEM-011
  @spec MCP-OP-TIME-005
  @spec MCP-OP-EXIT-001"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.measured :as measured]))

;; ============================================================
;; A source scanner: which top-level form does a line sit in
;; ============================================================

(defn- src-files
  []
  (->> (file-seq (io/file "src"))
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
       (sort-by #(.getPath ^java.io.File %))))

(defn- sites
  "Every line of `file` matching `pattern`, named by the top-level form it sits
   in. Line numbers are deliberately NOT part of the identity: an inventory
   pinned to line numbers has to be re-blessed on every unrelated edit, and one
   that is re-blessed reflexively stops being a ratchet."
  [^java.io.File file pattern]
  (:hits
   (reduce (fn [{:keys [form hits]} line]
             (let [form' (if (str/starts-with? line "(def")
                           (second (str/split (str/trim line) #"[\s\[]+"))
                           form)]
               {:form form'
                :hits (cond-> hits
                        (re-find pattern line) (conj [(.getPath file) form']))}))
           {:form nil :hits []}
           (str/split-lines (slurp file)))))

(defn- scan
  "`{[path form] reads}` over all of `src/`."
  [pattern]
  (frequencies (mapcat #(sites % pattern) (src-files))))

(def ^:private clock-pattern
  #"System/nanoTime|System/currentTimeMillis")

;; ============================================================
;; 1. The clock-site inventory
;; ============================================================

(def clock-site-inventory
  "Every clock read in `src/`, classified, with the count of reads in that form.

  `:receipt` — the value this clock produces is PUBLISHED, so it must reach the
  caller inside a `:measured` block and never beside one.

  `:control` — the value never enters a published receipt: a lease deadline, an
  expiry sweep, a retention cutoff, a transaction id, a poll loop, or the
  battery harness's own row. Naming these is half the point. An inventory that
  listed only the interesting sites would go stale silently; this one fails the
  moment `src/` grows a clock read nobody has thought about.

  Adding a clock site means adding a line here and saying which channel it is
  on. That is the whole cost, and it is the cost on purpose."
  {["src/clj_surgeon/ls_tree_snapshot.clj" "prune!"]                   {:reads 1 :channel :control}
   ["src/clj_surgeon/ls_tree_snapshot.clj" "touch!"]                   {:reads 1 :channel :control}
   ["src/clj_surgeon/ls_tree_snapshot.clj" "write-snapshot!"]          {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_change_buffer.clj" "now-ms"]                  {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_change_buffer.clj" "run-process!"]            {:reads 2 :channel :receipt}
   ["src/clj_surgeon/mcp_cold_verify.clj" "now-ms"]                    {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_cold_verify.clj" "run-job!"]                  {:reads 2 :channel :receipt}
   ["src/clj_surgeon/mcp_combinable_transaction.clj" "new-registry"]   {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_hot_verify.clj" "verify!"]                    {:reads 3 :channel :receipt}
   ["src/clj_surgeon/mcp_inspect_tool.clj" "elapsed-ms"]               {:reads 1 :channel :receipt}
   ["src/clj_surgeon/mcp_inspect_tool.clj" "execute-inspect-in-context!"] {:reads 1 :channel :receipt}
   ["src/clj_surgeon/mcp_operation.clj" "invoke!"]                     {:reads 1 :channel :receipt}
   ["src/clj_surgeon/mcp_prepared_confirmation.clj" "new-registry"]    {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_process.clj" "call-with-analyzer-contract-mission"] {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_process.clj" "claim-analyzer-mission-launch!"] {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_process.clj" "record-analyzer-mission-exit!"] {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_process.clj" "run-bounded!"]                  {:reads 2 :channel :receipt}
   ["src/clj_surgeon/mcp_telemetry.clj" "prune!"]                      {:reads 1 :channel :control}
   ["src/clj_surgeon/mcp_tool.clj" "elapsed-ms"]                       {:reads 1 :channel :receipt}
   ["src/clj_surgeon/mcp_tool.clj" "execute-request-in-context!"]      {:reads 1 :channel :receipt}
   ["src/clj_surgeon/mcp_tool.clj" "timed"]                            {:reads 1 :channel :receipt}
   ["src/clj_surgeon/memory_battery_runner.clj" "measure-once"]        {:reads 2 :channel :control}
   ["src/clj_surgeon/parse_admission.clj" "refusal"]                   {:reads 2 :channel :receipt}
   ["src/clj_surgeon/recovery.clj" "elapsed-ms"]                       {:reads 1 :channel :receipt}
   ["src/clj_surgeon/recovery.clj" "recover!"]                         {:reads 3 :channel :receipt}
   ["src/clj_surgeon/txn_journal.clj" "legacy-lock-dead?"]             {:reads 1 :channel :control}
   ["src/clj_surgeon/txn_journal.clj" "mark-break-linked!"]            {:reads 1 :channel :control}
   ["src/clj_surgeon/txn_journal.clj" "new-txid"]                      {:reads 1 :channel :control}
   ["src/clj_surgeon/txn_journal.clj" "prune-broken-locks!"]           {:reads 1 :channel :control}
   ["src/clj_surgeon/txn_journal.clj" "publish-one!"]                  {:reads 2 :channel :receipt}
   ["src/clj_surgeon/txn_journal.clj" "retained-transactions"]         {:reads 1 :channel :control}
   ["src/clj_surgeon/txn_journal.clj" "stamp-broken-at!"]              {:reads 1 :channel :control}
   ["src/clj_surgeon/txn_journal.clj" "stamp-tombstone!"]              {:reads 1 :channel :control}
   ["src/clj_surgeon/workspace_onboarding.clj" "await-cclsp-workspace!"] {:reads 2 :channel :control}})

(deftest every-clock-read-in-src-is-classified
  (testing "a clock site nobody classified fails here rather than shipping"
    (let [scanned (scan clock-pattern)
          declared (into {} (map (fn [[k v]] [k (:reads v)])) clock-site-inventory)]
      (is (= (set (keys declared)) (set (keys scanned)))
          (str "unclassified clock sites: "
               (pr-str (sort (remove (set (keys declared)) (keys scanned))))
               " ; inventoried sites that no longer exist: "
               (pr-str (sort (remove (set (keys scanned)) (keys declared))))))
      (is (= declared scanned)
          "a form's clock-read count changed; re-read it and re-classify"))))

;; ============================================================
;; 2. The publication boundary
;; ============================================================

(defn- fixed-clock
  "A clock that ticks by `delta-ns` on its second read."
  [start-ns delta-ns]
  (let [reads (atom 0)]
    #(if (zero? (first (swap-vals! reads inc))) start-ns (+ start-ns delta-ns))))

(defn- publish
  "One public MCP result, as the shared finalizer publishes it."
  [domain-result clock]
  (let [seen (atom nil)]
    (mcp-operation/invoke!
      {:clock-nanos clock
       :execute (constantly domain-result)
       :summarize (constantly "ok")
       :serialize pr-str
       :callback (fn [_ _ result] (reset! seen result))})
    @seen))

;; @spec MCP-OP-TIME-005
(deftest the-request-clock-does-not-survive-the-hashed-channel
  (testing "the reviewer's exact subject: a real public result with a receipt"
    (let [result (publish {:ok true :receipt {:stable :fact}}
                          (fixed-clock 1000000 2500000))
          hashed (measured/hashed-channel result)]
      (is (false? (contains? hashed :elapsed_ms))
          (str "the request clock is inside the hash subject: " (pr-str hashed)))
      (is (= 2.5 (measured/field result :elapsed_ms))
          "the meter went dark; MEM-005's argument is that an unpublished cost
           is one nobody notices regressing")
      (is (= {:ok true :receipt {:stable :fact}} hashed)
          "the hashed channel lost or gained a deterministic fact"))))

;; @spec MCP-OP-TIME-005
(deftest no-measured-field-is-published-outside-the-partition
  (testing "every family of measured receipt field the source scan found"
    (let [domain {:ok true
                  :operation "apply_clojure_changes"
                  :verification {:ok true :elapsed_ms 12.5 :exit 0}
                  :cold-verification {:status :passed :job_elapsed_ms 44.0}
                  :inspection_elapsed_ms 3.25
                  :receipt {:resources {:bytes_scanned 12 :scan_ms 1.5}
                            :commit-window {:max-ns 900 :reopens 0}}}
          result (publish domain (fixed-clock 0 1000000))]
      (is (= [] (measured/unpartitioned-measured-paths result))
          (str "measured fields published outside the partition: "
               (pr-str (measured/unpartitioned-measured-paths result))))
      (is (= 12.5 (measured/field (:verification result) :elapsed_ms))
          "a nested verification clock was dropped rather than partitioned")
      (is (= 1.5 (measured/field (get-in result [:receipt :resources]) :scan_ms))
          "the ls-tree meter was dropped rather than partitioned")
      (is (= 12 (get-in result [:receipt :resources :bytes_scanned]))
          "bytes_scanned is a COUNT and stays in the hashed channel")
      (is (= 0 (get-in result [:receipt :commit-window :reopens]))
          "a deterministic sibling of a measured field was moved with it"))))

;; @spec MCP-OP-MEM-011
;; @spec MCP-OP-TIME-005
(deftest the-parity-hash-is-stable-across-two-runs-with-different-clock-ticks
  (testing "one operation, one unchanged subject, two clocks, one hash"
    (let [domain {:ok true
                  :records [{:file "a.clj" :forms 3} {:file "b.clj" :forms 1}]
                  :receipt {:resources {:bytes_scanned 4096 :scan_ms 41.5}}}
          a (publish domain (fixed-clock 0 1000000))
          b (publish domain (fixed-clock 500 71000000))]
      (is (not= (measured/field a :elapsed_ms) (measured/field b :elapsed_ms))
          "the two clocks ticked identically, so this proves nothing")
      (is (true? (= (pr-str (measured/hashed-channel a))
                    (pr-str (measured/hashed-channel b))))
          (str "two publications of one result hash differently; A "
               (pr-str (measured/hashed-channel a))
               " B " (pr-str (measured/hashed-channel b)))))))

(deftest the-partition-shares-structure
  (testing "partitioning a large result may not copy it"
    (let [untouched {:file "a.clj" :forms [1 2 3]}
          x {:rows [untouched] :receipt {:scan_ms 1.0}}
          y (measured/partition-measured x)]
      (is (identical? (first (:rows x)) (first (:rows y)))
          "a sub-value carrying no measured field was rebuilt")
      (is (identical? (measured/partition-measured untouched) untouched)
          "a value with nothing to relocate is not returned identically"))))

;; ============================================================
;; 3. `System/exit` belongs to entrypoints
;; ============================================================

(def exit-allow-list
  "The ONLY forms in `src/` that may call `System/exit`: CLI entrypoints.

  A `-main` owns the process — an exit code is its return value. Anything else
  in `src/` is a library, and a library that exits kills whatever called it:
  the MCP server, a test runner, another tool's JVM. Two such calls lived in
  `run-ls-tree` and `run-fresh-scan` until this witness (`inb-eca3b1`); the
  discovery suite had to shell out to a subprocess to test the op at all."
  #{"-main"})

;; @spec MCP-OP-EXIT-001
(deftest system-exit-appears-only-inside-a-cli-entrypoint
  (testing "a library that exits kills its caller"
    (let [found (scan #"System/exit")
          offenders (sort (remove #(contains? exit-allow-list (second %))
                                  (keys found)))]
      (is (= [] offenders)
          (str "System/exit outside a -main: " (pr-str offenders)))
      (is (seq found)
          "the scanner found no System/exit at all, so it is not scanning"))))
