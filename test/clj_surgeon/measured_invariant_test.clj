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

(def scanned-roots
  "Every directory whose Clojure this invariant governs.

  `dev/experiments` is here because the round-three review found it (§3,
  residual a): it is on the `:clj-surgeon/mcp-test` classpath (`deps.edn`), its
  capture servers REPLACE the `:tool-fn` of a registered production tool, and
  yet being outside `src/` it was reached by no scan in this file. Three of
  them read `System/nanoTime` and published a top-level `elapsed_ms` straight
  to the SDK callback, bypassing the finalizer entirely — so every arm-run
  corpus they captured was invalid against the canonical output schema, and
  the sweep table that claimed \"every other in-repository elapsed_ms reader
  was swept\" had no `dev/` row. A scan whose boundary is a directory name
  rather than a classpath is a scan with a blind spot in it."
  ["src" "dev/experiments"])

(defn- src-files
  ([] (mapcat src-files scanned-roots))
  ([root]
   (->> (file-seq (io/file root))
        (filter #(.isFile ^java.io.File %))
        (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
        (sort-by #(.getPath ^java.io.File %)))))

(defn- site-path
  "`file` named as it would be named under `src/`, whatever root it was read
   from — so a scan of a scratch copy is comparable with a scan of the tree.

  A file under one of the real `scanned-roots` keeps the name it already has;
  only a scratch root is rewritten."
  [^java.io.File file root]
  (let [path (.getPath file)]
    (cond
      (some #(str/starts-with? path (str % "/")) scanned-roots) path
      (str/starts-with? path (str root "/")) (str "src/" (subs path (inc (count root))))
      :else path)))

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

(def ^:private sanctioned-measured-require
  "The ONLY way `src/` may name the measured namespace.

  Not a style rule. Every scan in this file is written against the `measured/`
  spelling, so any OTHER way of naming the namespace is a hole in every scan at
  once — which is how the round-three review's second bypass worked
  (2026-09-04 §1b): a brand-new namespace requiring
  `[clj-surgeon.measured :as measured :refer [raw-nanos]]` satisfied the alias
  witness, called `(raw-nanos)` under a bare name the clock scan does not
  match, published the result under an undeclared key, and left all nine tests
  green with the field in the parity hash.

  So the rule is the whole line: one alias, no `:refer`, no `:use`, and no
  fully-qualified `clj-surgeon.measured/...` call either."
  "[clj-surgeon.measured :as measured]")

(defn- scan
  "`{[path form] hits}` over every `.clj` under the scanned roots EXCEPT the
   measured namespace itself, which is where the raw reads are allowed to live."
  ([pattern]
   (apply merge-with + {} (map #(scan pattern %) scanned-roots)))
  ([pattern root]
   (frequencies
    (mapcat #(sites % pattern root)
            (remove #(= measured-namespace-file (site-path % root))
                    (src-files root))))))

(def ^:private clock-source-classes
  "The JDK types a program reads a TIME from.

  The METHOD names are never typed out. The round-four review's second blocking
  finding (2026-09-04 §2) was a hand-written list of four spellings —
  `System/nanoTime|System/currentTimeMillis|Instant/now|.getTime` — that did not
  contain `.lastModified`, so `mcp_admit_tool.clj` published a file mtime into a
  receipt field inside the parity hash TWO LINES ABOVE the read the same scan
  had just caught and routed. A list of names is a list of the names somebody
  thought of; the CLASS is what the ratchet is about, so the spellings below are
  derived from these classes by reflection."
  [java.lang.System
   java.time.Instant
   java.time.Clock
   java.time.LocalDateTime
   java.time.LocalDate
   java.time.ZonedDateTime
   java.util.Date
   java.io.File
   java.nio.file.Files
   java.nio.file.attribute.FileTime
   java.nio.file.attribute.BasicFileAttributes])

(def ^:private clock-return-types
  "A method is a clock read only if it HANDS BACK a time: an epoch or duration
   number, or one of the JDK's time values. `File/length` and `Files/size` also
   return `long` and are not clock reads, which is what the name fragments
   below are for."
  #{Long/TYPE Integer/TYPE
    java.time.Instant java.time.LocalDate java.time.LocalDateTime
    java.time.LocalTime java.time.ZonedDateTime java.time.OffsetDateTime
    java.time.Clock java.util.Date java.nio.file.attribute.FileTime})

(def ^:private clock-name-fragments
  "The morphemes a JDK time accessor is spelled with. Case-carrying fragments
   (`odified`, `poch`, `ano`, `illis`) match both `lastModified` and
   `getLastModifiedTime` without a case-insensitive match that would drag in
   every `now`-containing identifier in the tree."
  ["time" "Time" "now" "Instant" "instant" "illis" "ano" "poch" "odified"
   "Date" "Clock" "clock" "ystem"])

(defn- derived-clock-expressions
  "Every spelling of a clock read, derived from `clock-source-classes` by
   reflection: `Class/method` for a static, `.method` for an instance method.

  Sorted and distinct, so the pattern is deterministic and a diff of it is
  readable."
  []
  (vec
   (sort
    (distinct
     (for [^Class c clock-source-classes
           ^java.lang.reflect.Method m (.getMethods c)
           :let [nm (.getName m)]
           :when (and (contains? clock-return-types (.getReturnType m))
                      (some #(str/includes? nm %) clock-name-fragments))]
       (if (java.lang.reflect.Modifier/isStatic (.getModifiers m))
         (str (.getSimpleName c) "/" nm)
         (str "." nm)))))))

(defn- clock-expression-alternative
  "One derived spelling as a regex alternative.

  A static spelling is quoted literally, and it deliberately matches a
  fully-qualified call too (`java.time.Instant/now` contains `Instant/now`). An
  instance spelling gets a trailing word boundary so `.lastModified` does not
  swallow `.lastModifiedTime` — both are separate alternatives and the scan
  should say which one it found."
  [expression]
  (if (str/starts-with? expression ".")
    (str "\\" expression "\\b")
    (java.util.regex.Pattern/quote expression)))

(def ^:private clock-pattern
  "Every way a JVM program reads a clock, DERIVED from the JDK rather than
   listed."
  (re-pattern (str/join "|" (map clock-expression-alternative
                                 (derived-clock-expressions)))))

(def clock-expressions-the-ratchet-must-carry
  "One representative spelling per JDK time shape, with the site that proved it
  matters.

  This is not the pattern — the pattern is derived. It is a fail-first witness
  ON the derivation: each of these is planted in a receipt-publishing function
  and the scan must name the form it sits in. A derivation that silently
  stopped producing one of these would otherwise look exactly like a clean
  tree."
  {"System/nanoTime" "the monotonic clock behind every duration"
   "System/currentTimeMillis" "the wall clock behind every epoch stamp"
   "Instant/now" "the java.time wall clock"
   "Instant/ofEpochMilli" "an epoch number turned back into a time value"
   "LocalDateTime/now" "the local-time wall clock"
   "Clock/systemUTC" "an injectable clock, still a clock"
   ".getTime" "java.util.Date's epoch accessor"
   ".lastModified" "the file mtime the round-four review found published at mcp_admit_tool.clj:737"
   ".lastModifiedTime" "the java.nio spelling of the same read"
   "Files/getLastModifiedTime" "the static java.nio spelling"
   ".toMillis" "a FileTime converted to an epoch number"
   ".toEpochMilli" "an Instant converted to an epoch number"
   "FileTime/fromMillis" "an epoch number turned back into a file time"})

(def ^:private escape-hatch-pattern
  "Every expression that hands back an UNTAGGED number from the measured
   namespace: the raw clock reads, `value`, which strips a reading's tag, and
   the two ways to reach past `value` into the opaque type — the protocol
   method it is built on (`measured/-launder`) and the private field that
   method reads (`launderable`, which babashka's interop does not enforce as
   private even though the JVM does).

   The pattern is the type's whole surface, deliberately. A door the scanner
   does not know is the exact defect the round-three review walked through."
  #"measured/raw-nanos|measured/raw-ms|measured/value|measured/-launder|launderable")

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
   {:reads 2 :channel :control :why "snapshot expiry sweep: the cutoff and each candidate file's mtime"}
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
   ["src/clj_surgeon/mcp_alias_migration.clj" "prune-details!"]
   {:reads 1 :channel :control :why "prune ordering: newest detail files kept, by file mtime"}
   ["src/clj_surgeon/mcp_telemetry.clj" "prune!"]
   {:reads 2 :channel :control :why "telemetry retention cutoff and each candidate file's mtime"}
   ["src/clj_surgeon/memory_battery_runner.clj" "measure-once"]
   {:reads 2 :channel :control :why "the battery harness's own wall row"}
   ["src/clj_surgeon/memory_battery_runner.clj" "run-battery"]
   {:reads 2 :channel :control :why "battery run start/finish stamps"}
   ["src/clj_surgeon/memory_battery_runner.clj" "write-receipt!"]
   {:reads 1 :channel :control :why "receipt filename stamp"}
   ["src/clj_surgeon/txn_journal.clj" "begin!"]
   {:reads 1 :channel :control :why "transaction started-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "evidence-stat"]
   {:reads 2 :channel :control :why "the mtime/ctime pair that dates a tombstone's evidence on disk"}
   ["src/clj_surgeon/txn_journal.clj" "finish!"]
   {:reads 1 :channel :control :why "transaction finished-at stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "legacy-lock-dead?"]
   {:reads 1 :channel :control :why "lock liveness cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "lock-age-basis-ms"]
   {:reads 2 :channel :control :why "the newest of a lock file's mtime and ctime, the liveness basis"}
   ["src/clj_surgeon/txn_journal.clj" "lock-broken-line"]
   {:reads 1 :channel :control :why "broken-lock journal line stamp"}
   ["src/clj_surgeon/txn_journal.clj" "mark-break-linked!"]
   {:reads 1 :channel :control :why "break-link stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "new-txid"]
   {:reads 1 :channel :control :why "transaction id"}
   ["src/clj_surgeon/txn_journal.clj" "path-stat"]
   {:reads 1 :channel :control :why "the on-disk stat identity of a path; :mtime-ns is an identity field, never a receipt"}
   ["src/clj_surgeon/txn_journal.clj" "process-start-ticks"]
   {:reads 1 :channel :control :why "a process's start instant, half of the holder identity that makes a lease checkable"}
   ["src/clj_surgeon/txn_journal.clj" "prune-broken-locks!"]
   {:reads 1 :channel :control :why "broken-lock retention cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "recover!"]
   {:reads 2 :channel :control :why "recovery stamps on disk"}
   ["src/clj_surgeon/txn_journal.clj" "release-receipt!"]
   {:reads 1 :channel :control :why "lease release stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "retained-transactions"]
   {:reads 1 :channel :control :why "retention cutoff"}
   ["src/clj_surgeon/txn_journal.clj" "stamp-broken-at!"]
   {:reads 2 :channel :control :why "the broken-at stamp on disk, read once and rendered once"}
   ["src/clj_surgeon/txn_journal.clj" "stamp-tombstone!"]
   {:reads 1 :channel :control :why "tombstone stamp on disk"}
   ["src/clj_surgeon/txn_journal.clj" "touch-tombstone!"]
   {:reads 1 :channel :control :why "writes that stamp onto the tombstone file; the value is the caller's, not a new read"}
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
   {:calls 1 :why "keeps the widest commit window across the published paths"}
   ["dev/experiments/formatter_process_canary.clj" "run-canary!"]
   {:calls 1 :why "a canary report line, printed to stdout, never an MCP result"}})

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
(defn- measured-naming-offence
  "Why `code` names the measured namespace outside the sanctioned require, or
   nil when it does not.

   Three doors, each of which defeats every `measured/`-spelled scan in this
   file, and prose that merely MENTIONS the namespace is none of them."
  [code]
  (cond
    (re-find #"clj-surgeon\.measured/" code) :fully-qualified
    (not (re-find #"clj-surgeon\.measured(?![-\w.])" code)) nil
    (re-find #":refer" code) :refer
    (re-find #":use" code) :use
    (when-let [m (re-find #"clj-surgeon\.measured\s+:as\s+([\w-]+)" code)]
      (not= "measured" (second m))) :alias))

(defn- measured-naming-offenders
  "Every line under `root` that names the measured namespace in any way other
   than the one sanctioned require."
  ([] (vec (mapcat measured-naming-offenders scanned-roots)))
  ([root]
   (vec
    (for [^java.io.File file (remove #(= measured-namespace-file (site-path % root))
                                     (src-files root))
          [n line] (map-indexed vector (str/split-lines (slurp file)))
          :let [code (str/trim (or (first (str/split line #";;")) ""))
                offence (measured-naming-offence code)]
          :when offence]
      [(site-path file root) (inc n) offence code]))))

;; @spec MCP-OP-TIME-005
(deftest the-measured-namespace-is-named-only-by-the-sanctioned-require
  (testing "a name the scanner does not know is a hole in every scanner"
    (let [offenders (measured-naming-offenders)]
      (is (= [] offenders)
          (str "clj-surgeon.measured named other than `"
               sanctioned-measured-require "`: " (pr-str offenders))))))

;; @spec MCP-OP-TIME-005
(deftest the-require-witness-catches-a-planted-refer
  (testing "the round-three review's second bypass, replanted"
    (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                             (str "measured-refer-plant-" (System/nanoTime))))
          victim (io/file root "clj_surgeon" "planted_refer.clj")]
      (.mkdirs (.getParentFile victim))
      (spit victim
            (str "(ns clj-surgeon.planted-refer\n"
                 "  (:require\n"
                 "   [clj-surgeon.measured :as measured :refer [raw-nanos]]))\n\n"
                 "(defn publish-an-undeclared-clock-field\n"
                 "  [started]\n"
                 "  {:ok false\n"
                 "   :verification_wall_ms (/ (double (- (raw-nanos) started))\n"
                 "                            1000000.0)})\n"))
      (try
        (let [offenders (measured-naming-offenders root)]
          (is (= [["src/clj_surgeon/planted_refer.clj" 3 :refer
                   "[clj-surgeon.measured :as measured :refer [raw-nanos]]))"]]
                 offenders)
              (str "the require witness did not see a planted :refer: "
                   (pr-str offenders))))
        (finally
          (.delete victim)
          (.delete (.getParentFile victim))
          (.delete (io/file root)))))))

;; @spec MCP-OP-TIME-005
(deftest a-reading-does-not-open-to-anything-but-the-laundering-verb
  (testing "the round-three review's first bypass is now unrepresentable"
    (let [r (measured/elapsed-ms (measured/start))]
      ;; §1a, verbatim in effect: `(:clj-surgeon.measured/reading r)` was the
      ;; whole bypass. It must not yield a number by any map-shaped route.
      (doseq [[route opened]
              [[:keyword-lookup (:clj-surgeon.measured/reading r)]
               [:get (get r :clj-surgeon.measured/reading)]
               [:get-with-default (get r :clj-surgeon.measured/reading nil)]
               [:first-entry (try (key (first r)) (catch Exception _ nil))]
               [:vals (try (first (vals r)) (catch Exception _ nil))]
               [:seq-first (try (first (seq r)) (catch Exception _ nil))]
               [:deref (try (deref r) (catch Exception _ nil))]]]
        (is (not (number? opened))
            (str "a reading opened to a bare number through " route ": "
                 (pr-str opened))))
      (is (false? (map? r)) "a reading is a map again; §1a reopens")
      (is (false? (coll? r)) "a reading is a collection again; §1a reopens")
      (is (number? (measured/value r))
          "the one laundering verb no longer works")
      (is (= "#clj-surgeon.measured/reading" (pr-str r))
          (str "a reading prints as something other than a stable opaque "
               "marker, so a leak would be non-reproducible as well as wrong: "
               (pr-str (pr-str r))))
      (is (false? (measured/reading? {:clj-surgeon.measured/reading 1.0}))
          "a literal one-key map is a reading again; the map shape is back"))))

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


;; @spec MCP-OP-TIME-007
(deftest the-derived-clock-pattern-carries-every-jdk-time-shape
  (testing "the derivation, not a list, is what makes .lastModified visible"
    (let [derived (set (derived-clock-expressions))
          missing (vec (sort (remove derived
                                     (keys clock-expressions-the-ratchet-must-carry))))]
      (is (= [] missing)
          (str "the JDK derivation no longer produces these clock spellings, so "
               "a read written with one of them would be invisible to the scan: "
               (pr-str missing)))
      (is (< 20 (count derived))
          (str "the derivation produced only " (count derived)
               " spellings; reflection is not finding the JDK time methods")))))

;; @spec MCP-OP-TIME-007
(deftest the-clock-scanner-catches-every-jdk-time-shape-planted-in-a-receipt
  (testing "each derived spelling, planted where a receipt is built"
    (doseq [[expression why] (sort clock-expressions-the-ratchet-must-carry)]
      (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                               (str "measured-clock-shape-" (System/nanoTime))))
            victim (io/file root "clj_surgeon" "planted_shape.clj")
            call (if (str/starts-with? expression ".")
                   (str "(" expression " subject)")
                   (str "(" expression ")"))]
        (.mkdirs (.getParentFile victim))
        (spit victim
              (str "(ns clj-surgeon.planted-shape)\n\n"
                   "(defn publish-a-receipt\n"
                   "  [subject]\n"
                   "  {:ok true :receipt {:written_at " call "}})\n"))
        (try
          (let [planted (scan clock-pattern root)]
            (is (= {["src/clj_surgeon/planted_shape.clj" "publish-a-receipt"] 1}
                   planted)
                (str "the clock scan did not see a planted " expression
                     " (" why "): " (pr-str planted))))
          (finally
            (.delete victim)
            (.delete (.getParentFile victim))
            (.delete (io/file root))))))))

;; ============================================================
;; 1b. The NAMESPACE's public surface, derived rather than enumerated
;; ============================================================
;;
;; The round-four review closed the TYPE and left the NAMESPACE open
;; (2026-09-04 §1). `measured/unwrap-readings` was a public verb that turned a
;; tagged reading into a bare number at any depth; the escape-hatch scan
;; enumerates laundering verbs BY NAME and had never heard of it, so a one-line
;; plant at `src/clj_surgeon/mcp_hot_verify.clj` reproduced round three's §1a
;; verbatim in effect — an undeclared clock field inside the parity hash
;; subject, `unpartitioned []`, twelve invariant tests green. `measured/field`
;; was the same class from an already-published block, and the branch's own
;; recovery fix called `unwrap-readings` twice in `src/` with no allow-list
;; entry and no witness said anything.
;;
;; The diagnosis is round three's own sentence: **a name the scanner does not
;; know is a hole in every scanner at once.** So neither of the two witnesses
;; below is a list of names.
;;
;;   (i)  a PROBE over `(ns-publics 'clj-surgeon.measured)`, by reflection:
;;        every public fn var is called, at every arity it declares, with a
;;        tagged reading carrying a sentinel number — and it is an offence for
;;        the sentinel to come back anywhere outside a `:measured` block.
;;        Adding a public verb that launders costs a `sanctioned-laundering-vars`
;;        entry, and the entry is refused unless the verb is also in
;;        `escape-hatch-pattern`, which then makes its `src/` call sites cost an
;;        allow-list line. That is the cost on purpose.
;;
;;   (ii) a SOURCE scan asserting that every `measured/<verb>` reference in the
;;        scanned roots names a var that is public in the namespace TODAY. A
;;        call to a private or absent verb is an offence with no list to
;;        consult, which is what makes the reviewer's plant go red: once
;;        `unwrap-readings` is private, `(measured/unwrap-readings ...)` in
;;        `src/` fails this witness by name-independent construction.
;;
;; RESIDUAL, declared and accepted (round-four review §3): `setAccessible` with
;; a COMPUTED field name — `(.getDeclaredField (class r) (str "launder" "able"))`
;; — reads the number and no textual scan can see it. The reviewer's sentence:
;; "Textual scanning cannot close that, and a deliberate attacker is not the
;; threat model here — record it, do not chase it." A JVM without a security
;; manager cannot prevent reflection, so this is a property of the platform,
;; not a gap in the ratchet.

(def ^:private laundering-sentinel
  "A number no clock produces and no fixture carries, so finding it outside a
   `:measured` block is proof a public verb handed a reading's number back."
  987654.321)

(defn- sentinel-outside-a-measured-block?
  "Does `laundering-sentinel` appear in `x` anywhere a caller could read it as
   an ordinary number — that is, anywhere except under `measured-key`?

  Inside the block the number is the publication itself: `measured` and
  `attach` BUILD that block and must be allowed to."
  [x]
  (letfn [(walk [node]
            (cond
              (= laundering-sentinel node) true
              (map? node) (boolean (some (fn [[k v]]
                                           (or (walk k)
                                               (and (not= k measured/measured-key)
                                                    (walk v))))
                                         node))
              (or (vector? node) (seq? node) (set? node))
              (boolean (some walk node))
              :else false))]
    (walk x)))

(defn- reading-arguments
  "The argument pool for the READING probe: the sentinel as a TAGGED reading,
   in the placements a caller has — bare, under a key, inside a vector — plus
   the key those maps use, so a two-argument reader can be handed both halves.

  Nothing in this pool carries the sentinel as a bare number, so the sentinel
  coming BACK outside a measured block means the var produced it."
  []
  [(measured/reading laundering-sentinel)
   {:probe (measured/reading laundering-sentinel)}
   [(measured/reading laundering-sentinel)]
   :probe])

(defn- block-arguments
  "The argument pool for the BLOCK probe: an already-published measured block,
   whose contents are bare numbers by construction, and its key."
  []
  [(measured/measured {:probe (measured/reading laundering-sentinel)})
   :probe])

(defn- argument-combinations
  [pool n]
  (reduce (fn [acc _] (for [args acc a pool] (conj args a)))
          [[]]
          (range n)))

(defn- probed-arities
  "Every fixed arity of `v` this probe can call. Variadic tails are dropped:
   the fixed prefix is probed by the arity that names it.

  A callable var with no `:arglists` — a protocol method under some runtimes, a
  keyword, a set — is probed at arity 1 and 2, the arities such a value has."
  [v]
  (let [declared (->> (:arglists (meta v))
                      (map #(count (take-while (complement #{'&}) %)))
                      distinct
                      sort
                      vec)]
    (if (seq declared) declared [1 2])))

(defn- yields-the-sentinel?
  "Does calling `v` over `pool` hand the sentinel back outside a measured
   block?"
  [v pool]
  (boolean
    (some (fn [n]
            (some (fn [args]
                    (try
                      (sentinel-outside-a-measured-block? (apply @v args))
                      (catch Throwable _ false)))
                  (argument-combinations pool n)))
          (probed-arities v))))

(def sanctioned-laundering-vars
  "The public vars of `clj-surgeon.measured` that MAY turn a TAGGED READING
  into a bare number, each with the reason.

  An entry here is not enough on its own: the witness below refuses one whose
  name `escape-hatch-pattern` does not match, so sanctioning a verb also makes
  every one of its `src/` call sites cost an `escape-hatch-allow-list` line."
  {"value" "THE ONE door out of a reading; every src call site is named in escape-hatch-allow-list"
   "-launder" "the protocol method `value` is built on, and the only implementation of it"})

(def sanctioned-block-readers
  "The public vars that MAY hand back a number out of an already-published
  measured block.

  DECLARED RESIDUAL, and it is a property of the wire rather than a gap: a
  measured block holds BARE NUMBERS because MEM-005 requires a plain JSON
  number in the published receipt, so `(get-in x [:measured :elapsed_ms])`
  reaches one and no type can prevent that. What this witness can hold is that
  the namespace offers no CONVENIENCE VERB for it — reaching into a block is at
  least as visible as naming the key the partition is defined by. `field` was
  such a verb and the round-four review took it (§1b)."
  {"measured-key" "the well-known key the partition is DEFINED by; its lookup is an ordinary `get`"})

(defn- public-measured-callable-vars
  "Every public var of the measured namespace whose value can be CALLED.

  `ifn?`, not `fn?`: a protocol method is not a `fn` under babashka, and
  `-launder` is precisely the door the pattern exists for — a probe that skips
  it because of its runtime representation is the blind spot again."
  []
  (->> (ns-publics 'clj-surgeon.measured)
       (filter (fn [[_ v]] (ifn? @v)))
       (sort-by first)
       vec))

(defn- public-measured-inert-vars
  "Every public var of the measured namespace the probe did NOT call, with
   whether it is callable after all — the partition has to be total."
  []
  (->> (ns-publics 'clj-surgeon.measured)
       (remove (fn [[_ v]] (ifn? @v)))
       (map (fn [[sym v]] [(name sym) (ifn? @v)]))
       (sort-by first)
       vec))

(defn- sentinel-yielding-var-names
  [pool]
  (->> (public-measured-callable-vars)
       (filter (fn [[_ v]] (yields-the-sentinel? v pool)))
       (map (comp name first))
       sort
       vec))

;; @spec MCP-OP-TIME-006
(deftest the-measured-namespace-exposes-no-unsanctioned-laundering-verb
  (testing "the public surface is derived by reflection, not by a list of names"
    (let [launderers (sentinel-yielding-var-names (reading-arguments))
          unsanctioned (vec (remove (set (keys sanctioned-laundering-vars)) launderers))
          skipped (public-measured-inert-vars)]
      (is (seq (public-measured-callable-vars))
          "the probe found no callable public vars, so it is not probing")
      (is (= [] (vec (filter second skipped)))
          (str "the probe skipped a public var that IS callable: " (pr-str skipped)))
      (is (= [] unsanctioned)
          (str "public vars of clj-surgeon.measured turn a tagged reading into a "
               "bare number outside the measured block with no sanction: "
               (pr-str unsanctioned)))
      (is (= (sort (keys sanctioned-laundering-vars)) launderers)
          (str "the sanctioned set no longer matches what the probe actually "
               "finds; a sanctioned verb that no longer launders means the probe "
               "went blind. probe found: " (pr-str launderers)))
      (is (= [] (vec (remove #(re-find escape-hatch-pattern (str "measured/" %))
                             (sort (keys sanctioned-laundering-vars)))))
          (str "a sanctioned laundering verb that escape-hatch-pattern does not "
               "match: its src call sites would cost nothing")))))

;; @spec MCP-OP-TIME-006
(deftest the-measured-namespace-exposes-no-verb-that-reads-out-of-a-block
  (testing "a published block holds bare numbers; no verb makes reaching in cheap"
    (let [readers (sentinel-yielding-var-names (block-arguments))
          unsanctioned (vec (remove (set (keys sanctioned-block-readers)) readers))]
      (is (= [] unsanctioned)
          (str "public vars of clj-surgeon.measured hand a number out of a "
               "published measured block with no sanction: " (pr-str unsanctioned)))
      (is (= (sort (keys sanctioned-block-readers)) readers)
          (str "the sanctioned block readers no longer match what the probe "
               "finds; probe found: " (pr-str readers))))))

(def ^:private measured-verb-pattern
  "Any `measured/<verb>` reference. The verb is captured so it can be checked
   against the namespace's ACTUAL public vars rather than a list."
  #"measured/([A-Za-z0-9?!*<>=+_'-]+)")

(defn- unknown-measured-verbs
  "Every `measured/<verb>` reference under `root` naming something that is not
   a public var of `clj-surgeon.measured` today."
  ([] (vec (mapcat unknown-measured-verbs scanned-roots)))
  ([root]
   (let [public-names (set (map name (keys (ns-publics 'clj-surgeon.measured))))]
     (vec
      (for [^java.io.File file (remove #(= measured-namespace-file (site-path % root))
                                       (src-files root))
            [n line] (map-indexed vector (str/split-lines (slurp file)))
            :let [code (or (first (str/split line #";;")) "")]
            verb (map second (re-seq measured-verb-pattern code))
            :when (not (contains? public-names verb))]
        [(site-path file root) (inc n) verb])))))

;; @spec MCP-OP-TIME-006
(deftest every-measured-verb-named-in-source-is-a-public-var
  (testing "a call to a private or absent measured verb is an offence by construction"
    (let [offenders (unknown-measured-verbs)]
      (is (= [] offenders)
          (str "measured/<verb> references that are not public vars of "
               "clj-surgeon.measured: " (pr-str offenders))))))

;; @spec MCP-OP-TIME-006
(deftest the-public-var-witness-catches-the-reviewers-unwrap-plant
  (testing "round-four review §1a, replanted: the laundering verb by name"
    (let [root (str (io/file (System/getProperty "java.io.tmpdir")
                             (str "measured-unwrap-plant-" (System/nanoTime))))
          victim (io/file root "clj_surgeon" "planted_unwrap.clj")]
      (.mkdirs (.getParentFile victim))
      (spit victim
            (str "(ns clj-surgeon.planted-unwrap\n"
                 "  (:require\n"
                 "   [clj-surgeon.measured :as measured]))\n\n"
                 "(defn publish-an-undeclared-clock-field\n"
                 "  [started]\n"
                 "  {:ok false\n"
                 "   :verification_wall_ms (measured/unwrap-readings\n"
                 "                           (measured/elapsed-ms started))})\n"))
      (try
        (let [offenders (unknown-measured-verbs root)]
          (is (= [["src/clj_surgeon/planted_unwrap.clj" 8 "unwrap-readings"]]
                 offenders)
              (str "the public-var witness did not see the planted laundering "
                   "verb: " (pr-str offenders))))
        (finally
          (.delete victim)
          (.delete (.getParentFile victim))
          (.delete (io/file root)))))))

;; ============================================================
;; 2. The publication boundary
;; ============================================================

(defn- measured-field
  "One field of `x`'s published measured block.

  A test-local `get-in`, not a verb from the namespace: `measured/field` was a
  public convenience for exactly this and the round-four review took it as a
  laundering route out of a published block (§1b)."
  [x k]
  (get-in x [measured/measured-key k]))

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
      (is (= 2.5 (measured-field result :elapsed_ms))
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
      (is (= 12.5 (measured-field (:verification result) :elapsed_ms))
          "a nested verification clock was dropped rather than partitioned")
      (is (= 1.5 (measured-field (get-in result [:receipt :resources]) :scan_ms))
          "the ls-tree meter was dropped rather than partitioned")
      (is (= 12 (get-in result [:receipt :resources :bytes_scanned]))
          "bytes_scanned is a COUNT and stays in the hashed channel")
      (is (= 0 (get-in result [:receipt :commit-window :reopens]))
          "a deterministic sibling of a measured field was moved with it"))))

(def ^:private sentinel
  "A number no ordinary field carries, so finding it anywhere in a hashed
   channel is proof a planted reading was laundered rather than partitioned."
  424242.5)

(defn- publish-outcome
  "Publish `domain-result` and classify what the boundary did with it.

   `[:published result]`, `[:refused error-type]`, or `[:threw class-name]` —
   and the third is a failure by itself: an unpartitionable reading must be a
   TYPED refusal, never a raw JVM exception the caller cannot dispatch on."
  [domain-result]
  (try
    [:published (publish domain-result (fixed-clock 0 1000000))]
    (catch clojure.lang.ExceptionInfo error
      (if-let [t (:error-type (ex-data error))]
        [:refused t]
        [:threw (.getName (class error))]))
    (catch Exception error
      [:threw (.getName (class error))])))

(defn- sentinel-in-hash?
  "Does the sentinel number survive anywhere in `x`'s hashed channel?"
  [x]
  (let [found (atom false)]
    ((fn walk [node]
       (cond
         @found nil
         (= sentinel node) (reset! found true)
         (map? node) (doseq [[k v] node] (walk k) (walk v))
         (or (vector? node) (seq? node) (set? node)) (doseq [v node] (walk v))
         :else nil))
     (measured/hashed-channel x))
    @found))

;; @spec MCP-OP-TIME-005
(deftest a-clock-reading-never-becomes-a-raw-number-in-any-placement
  (testing "the :control channel's promise, enforced at the boundary"
    ;; The `clock-allow-list`'s 33 `:control` entries each assert in a `:why`
    ;; string that their value is never published. The round-three review's
    ;; §1d is that nothing checked it: the test read the `:channel` keyword and
    ;; nothing else, so a control form could be edited to publish its value and
    ;; the counts would not move.
    ;;
    ;; A `:why` string cannot be made true by a test. What CAN be made true —
    ;; and is the property the `:why` strings are really claiming — is that a
    ;; reading reaching the finalizer from ANYWHERE, in any placement a control
    ;; site could contrive, either lands inside the partition or is refused by
    ;; type. Never a bare number in the hash subject, and never a raw JVM
    ;; exception.
    (doseq [[placement domain expected]
            [[:nested-under-an-undeclared-key
              {:ok true :receipt {:inner {:verification_wall_ms
                                          (measured/reading sentinel)}}}
              :published]
             [:inside-a-row-of-a-vector
              {:ok true :rows [{:file "a.clj"}
                               {:file "b.clj"
                                :lease_wall_ms (measured/reading sentinel)}]}
              :published]
             [:directly-in-a-vector
              {:ok true :rows [(measured/reading sentinel)]}
              :refused]
             [:directly-in-a-set
              {:ok true :s #{(measured/reading sentinel)}}
              :refused]
             [:as-a-map-key
              {:ok true :m {(measured/reading sentinel) :v}}
              :refused]
             [:inside-a-string-keyed-sorted-map
              {:ok true :staged (sorted-map "a.clj" 1
                                            "b.clj" (measured/reading sentinel))}
              :refused]]]
      (let [[outcome payload] (publish-outcome domain)]
        (is (= expected outcome)
            (str placement " was " outcome " (" (pr-str payload)
                 "); an unpartitionable reading must be a typed refusal and a"
                 " partitionable one must be relocated"))
        (case outcome
          :published (is (false? (sentinel-in-hash? payload))
                         (str placement ": a clock reading reached the hash "
                              "subject as a bare number: " (pr-str payload)))
          :refused (is (= :unpartitioned-measured-field payload)
                       (str placement ": the refusal is not typed: "
                            (pr-str payload)))
          :threw (is false
                     (str placement ": the boundary threw an untyped "
                          payload " instead of refusing")))))))

;; @spec MCP-OP-MEM-011
;; @spec MCP-OP-TIME-005
(deftest the-parity-hash-is-stable-across-two-runs-with-different-clock-ticks
  (testing "one operation, one unchanged subject, two clocks, one hash"
    (let [domain {:ok true
                  :records [{:file "a.clj" :forms 3} {:file "b.clj" :forms 1}]
                  :receipt {:resources {:bytes_scanned 4096 :scan_ms 41.5}}}
          a (publish domain (fixed-clock 0 1000000))
          b (publish domain (fixed-clock 500 71000000))]
      (is (not= (measured-field a :elapsed_ms) (measured-field b :elapsed_ms))
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
    ;; `src/` only: the rule is about a LIBRARY exiting under its caller. The
    ;; `dev/experiments/` scripts are standalone entrypoints run as their own
    ;; process, and an exit code is their return value.
    (let [found (scan #"System/exit" "src")
          offenders (sort (remove #(contains? exit-allow-list (second %))
                                  (keys found)))]
      (is (= [] offenders)
          (str "System/exit outside a -main: " (pr-str offenders)))
      (is (seq found)
          "the scanner found no System/exit at all, so it is not scanning"))))
