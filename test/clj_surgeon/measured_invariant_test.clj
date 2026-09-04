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
  ([] (src-files "src"))
  ([root]
   (->> (file-seq (io/file root))
        (filter #(.isFile ^java.io.File %))
        (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
        (sort-by #(.getPath ^java.io.File %)))))

(defn- site-path
  "`file` named as it would be named under `src/`, whatever root it was read
   from — so a scan of a scratch copy is comparable with a scan of the tree."
  [^java.io.File file root]
  (let [path (.getPath file)]
    (if (str/starts-with? path (str root "/"))
      (str "src/" (subs path (inc (count root))))
      path)))

(defn- sites
  "Every line of `file` matching `pattern`, named by the top-level form it sits
   in. Line numbers are deliberately NOT part of the identity: an inventory
   pinned to line numbers has to be re-blessed on every unrelated edit, and one
   that is re-blessed reflexively stops being a ratchet.

   `;;` comments are cut before matching: a comment EXPLAINING why a call was
   removed must not read as the call."
  ([^java.io.File file pattern] (sites file pattern "src"))
  ([^java.io.File file pattern root]
   (:hits
    (reduce (fn [{:keys [form hits]} line]
              (let [code (or (first (str/split line #";;")) "")
                    form' (if (str/starts-with? line "(def")
                            (second (str/split (str/trim line) #"[\s\[]+"))
                            form)]
                {:form form'
                 :hits (cond-> hits
                         (re-find pattern code)
                         (conj [(site-path file root) form']))}))
            {:form nil :hits []}
            (str/split-lines (slurp file)))))) 

(def ^:private measured-namespace-file
  "The ONE file allowed to read a clock raw."
  "src/clj_surgeon/measured.clj")

(defn- scan
  "`{[path form] hits}` over every `.clj` under `root` EXCEPT the measured
   namespace itself, which is where the raw reads are allowed to live."
  ([pattern] (scan pattern "src"))
  ([pattern root]
   (frequencies
    (mapcat #(sites % pattern root)
            (remove #(= measured-namespace-file (site-path % root))
                    (src-files root))))))

(def ^:private clock-pattern
  "Every way a JVM program reads a clock."
  #"System/nanoTime|System/currentTimeMillis|Instant/now|\.getTime")

(def ^:private escape-hatch-pattern
  "The verbs that hand back an UNTAGGED number: the raw clock reads inside
   `clj-surgeon.measured`, and `value`, which strips a reading's tag."
  #"measured/raw-nanos|measured/raw-ms|measured/value")

;; ============================================================
;; 1. No raw clock read outside `clj-surgeon.measured`
;; ============================================================

(def clock-allow-list
  "Every form in `src/` outside `clj-surgeon.measured` that may read a clock
  RAW, with the count of reads in that form and why it is allowed.

  The first repair of this invariant inventoried clock reads and classified
  them `:receipt` or `:control`, comparing only READ COUNTS per form. Sol's
  round-two review walked straight through it (2026-09-04 §1): bind one
  existing read to a local, publish it under the declared name AND an
  undeclared one, and the count is unchanged, the new name is in nobody's
  vocabulary, and the undeclared field sails into the parity hash with every
  witness green.

  So the rule is no longer 'a clock read is classified'. It is: **a clock read
  whose value can be PUBLISHED does not happen outside `clj-surgeon.measured`.**
  Receipt code calls `measured/start` and `measured/elapsed-ms`, which return a
  TAGGED reading, and the publication boundary relocates every reading it
  finds under any key at all. This list is therefore `:control` ONLY — a lease
  deadline, an expiry sweep, a retention cutoff, a transaction id, a poll loop,
  a file timestamp, the battery harness's own row. A `:receipt` entry here
  would be a contradiction and the test below refuses one.

  Adding a control clock read means adding a line here and saying why the value
  is never published. That is the whole cost, and it is the cost on purpose."
  {["src/clj_surgeon/ls_tree_snapshot.clj" "prune!"]
   {:reads 1 :channel :control :why "snapshot expiry sweep"}
   ["src/clj_surgeon/ls_tree_snapshot.clj" "touch!"]
   {:reads 1 :channel :control :why "file mtime, not a receipt field"}
   ["src/clj_surgeon/ls_tree_snapshot.clj" "write-snapshot!"]
   {:reads 1 :channel :control :why "snapshot creation stamp on disk"}
   ["src/clj_surgeon/mcp_change_buffer.clj" "now-ms"]
   {:reads 1 :channel :control :why "buffer lease clock"}
   ["src/clj_surgeon/mcp_cold_verify.clj" "now-ms"]
   {:reads 1 :channel :control :why "job store lease clock"}
   ["src/clj_surgeon/mcp_combinable_transaction.clj" "new-registry"]
   {:reads 1 :channel :control :why "registry lease clock seam"}
   ["src/clj_surgeon/mcp_prepared_confirmation.clj" "new-registry"]
   {:reads 1 :channel :control :why "registry lease clock seam"}
   ["src/clj_surgeon/mcp_process.clj" "call-with-analyzer-contract-mission"]
   {:reads 1 :channel :control :why "mission lease expiry"}
   ["src/clj_surgeon/mcp_process.clj" "claim-analyzer-mission-launch!"]
   {:reads 1 :channel :control :why "mission lease claim"}
   ["src/clj_surgeon/mcp_process.clj" "record-analyzer-mission-exit!"]
   {:reads 1 :channel :control :why "mission lease exit stamp"}
   ["src/clj_surgeon/mcp_telemetry.clj" "emit!"]
   {:reads 1 :channel :control :why "telemetry row timestamp, never a public result"}
   ["src/clj_surgeon/mcp_telemetry.clj" "prune!"]
   {:reads 1 :channel :control :why "telemetry retention cutoff"}
   ["src/clj_surgeon/memory_battery_runner.clj" "measure-once"]
   {:reads 2 :channel :control :why "the battery harness's own wall row"}
   ["src/clj_surgeon/memory_battery_runner.clj" "run-battery"]
   {:reads 2 :channel :control :why "battery run start/finish stamps"}
   ["src/clj_surgeon/memory_battery_runner.clj" "write-receipt!"]
   {:reads 1 :channel :control :why "receipt filename stamp"}
   ["src/clj_surgeon/txn_journal.clj" "begin!"]
   {:reads 1 :channel :control :why "transaction started-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "finish!"]
   {:reads 1 :channel :control :why "transaction finished-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "legacy-lock-dead?"]
   {:reads 1 :channel :control :why "lock liveness cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "lock-broken-line"]
   {:reads 1 :channel :control :why "broken-lock journal line stamp"}
   ["src/clj_surgeon/txn_journal.clj" "mark-break-linked!"]
   {:reads 1 :channel :control :why "break-link stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "new-txid"]
   {:reads 1 :channel :control :why "transaction id"}
   ["src/clj_surgeon/txn_journal.clj" "prune-broken-locks!"]
   {:reads 1 :channel :control :why "broken-lock retention cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "recover!"]
   {:reads 2 :channel :control :why "recovery stamps on disk"}
   ["src/clj_surgeon/txn_journal.clj" "release-receipt!"]
   {:reads 1 :channel :control :why "lease release stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "retained-transactions"]
   {:reads 1 :channel :control :why "retention cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "stamp-broken-at!"]
   {:reads 1 :channel :control :why "broken-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "stamp-tombstone!"]
   {:reads 1 :channel :control :why "tombstone stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "write-lease!"]
   {:reads 1 :channel :control :why "lease acquired-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "write-lock!"]
   {:reads 1 :channel :control :why "lock stamp on disk"}
   ["src/clj_surgeon/workspace_onboarding.clj" "await-cclsp-workspace!"]
   {:reads 2 :channel :control :why "readiness poll deadline"}
   ["src/clj_surgeon/worktree_lifecycle_io.clj" "capture-inventory"]
   {:reads 1 :channel :control :why "inventory captured-at stamp"}
   ["src/clj_surgeon/worktree_lifecycle_io.clj" "issue-current?"]
   {:reads 1 :channel :control :why "issue freshness cutoff"}})

(def escape-hatch-allow-list
  "Every form in `src/` that calls a verb handing back an UNTAGGED number.

  `measured/value` strips a reading's tag; `measured/raw-nanos` and
  `measured/raw-ms` read the clock without one. Laundering is legitimate — a
  sum, a comparison, a telemetry row, a clock seam a caller may inject — and it
  must be a deliberate, greppable act rather than a side effect, so every site
  is named here with the reason it needs a bare number."
  {["src/clj_surgeon/mcp_operation.clj" "invoke!"]
   {:calls 1 :why "the injectable request-clock seam: callers pass a plain-long clock"}
   ["src/clj_surgeon/mcp_operation.clj" "finalize-result"]
   {:calls 1 :why "the boundary validates finiteness before it attaches the reading"}
   ["src/clj_surgeon/mcp_change_buffer.clj" "capture-verification-baseline!"]
   {:calls 1 :why "sums the per-check clocks into one derived reading"}
   ["src/clj_surgeon/mcp_change_buffer.clj" "run-verification!"]
   {:calls 2 :why "sums hot and per-check clocks into one derived reading"}
   ["src/clj_surgeon/mcp_tool.clj" "exact-terminal-response"]
   {:calls 2 :why "compares the verification clock against zero before publication"}
   ["src/clj_surgeon/mcp_tool.clj" "record-result!"]
   {:calls 1 :why "telemetry row, never a public result"}
   ["src/clj_surgeon/mcp_tool.clj" "execute-request-in-context!"]
   {:calls 1 :why "telemetry row, never a public result"}
   ["src/clj_surgeon/mcp_inspect_tool.clj" "execute-inspect-in-context!"]
   {:calls 1 :why "telemetry row, never a public result"}
   ["src/clj_surgeon/parse_admission.clj" "refusal"]
   {:calls 1 :why "the scan meter accumulates bare nanos across many files"}
   ["src/clj_surgeon/txn_journal.clj" "commit!"]
   {:calls 1 :why "keeps the widest commit window across the published paths"}})

;; @spec MCP-OP-TIME-005
(deftest no-raw-clock-read-lives-outside-the-measured-namespace
  (testing "a published clock reading cannot be CONSTRUCTED outside the partition"
    (let [scanned (scan clock-pattern)
          declared (into {} (map (fn [[k v]] [k (:reads v)])) clock-allow-list)]
      (is (= (set (keys declared)) (set (keys scanned)))
          (str "raw clock reads with no allow-list entry: "
               (pr-str (sort (remove (set (keys declared)) (keys scanned))))
               " ; allow-listed sites that no longer exist: "
               (pr-str (sort (remove (set (keys scanned)) (keys declared))))))
      (is (= declared scanned)
          "a form's raw clock-read count changed; re-read it and re-justify")
      (is (= [] (sort (keep (fn [[site {:keys [channel]}]]
                              (when (not= :control channel) site))
                            clock-allow-list)))
          "a RECEIPT clock read may not be raw: it must return a tagged reading"))))

;; @spec MCP-OP-TIME-005
(deftest every-untagged-clock-verb-call-site-is-named
  (testing "laundering a reading back to a bare number is deliberate"
    (let [scanned (scan escape-hatch-pattern)
          declared (into {} (map (fn [[k v]] [k (:calls v)])) escape-hatch-allow-list)]
      (is (= (set (keys declared)) (set (keys scanned)))
          (str "untagged-clock verbs with no allow-list entry: "
               (pr-str (sort (remove (set (keys declared)) (keys scanned))))
               " ; allow-listed sites that no longer exist: "
               (pr-str (sort (remove (set (keys scanned)) (keys declared))))))
      (is (= declared scanned)
          "a form's untagged-clock call count changed; re-read it and re-justify"))))

;; @spec MCP-OP-TIME-005
(deftest the-measured-namespace-is-never-aliased-to-another-name
  (testing "an alias the scanner does not know is a hole in the scanner"
    (let [offenders (->> (src-files)
                         (keep (fn [^java.io.File file]
                                 (let [text (slurp file)]
                                   (when-let [m (re-find #"clj-surgeon\.measured :as ([a-z-]+)"
                                                         text)]
                                     (when (not= "measured" (second m))
                                       [(site-path file "src") (second m)])))))
                         sort)]
      (is (= [] offenders)
          (str "clj-surgeon.measured aliased to something other than `measured`: "
               (pr-str offenders))))))

;; @spec MCP-OP-TIME-005
(deftest the-clock-scanner-catches-a-planted-raw-read
  (testing "the ratchet goes RED when the defect is reintroduced"
    (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                             (str "measured-plant-" (System/nanoTime))))
          victim (io/file root "clj_surgeon" "planted.clj")]
      (.mkdirs (.getParentFile victim))
      (spit victim
            (str "(ns clj-surgeon.planted)\n\n"
                 "(defn publish-an-undeclared-clock-field\n"
                 "  [started]\n"
                 "  (let [duration-ms (/ (double (- (System/nanoTime) started))\n"
                 "                       1000000.0)]\n"
                 "    {:ok false :verification_wall_ms duration-ms}))\n"))
      (try
        (let [planted (scan clock-pattern root)]
          (is (= {["src/clj_surgeon/planted.clj" "publish-an-undeclared-clock-field"] 1}
                 planted)
              (str "the scanner did not see a planted raw clock read: "
                   (pr-str planted))))
        (finally
          (.delete victim)
          (.delete (.getParentFile victim))
          (.delete (io/file root)))))))

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
