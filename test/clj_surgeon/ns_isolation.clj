(ns clj-surgeon.ns-isolation
  "ONE per-namespace snapshot fixture hosting SIX runtime purity witnesses --
   TEST-ISO-002 (process spawn), 003 (writes), 004 (ports/listeners),
   005 (global mutation), 007 (time budget), 010 (thread/executor leaks).

   One mechanism, six witnesses, because they are all the same shape: take a
   picture of a resource before a namespace runs, take another after, and
   REFUSE, typed, naming the namespace AND the resource, when the two differ.

   THE PROBE EMITS FACTS; THE FOLD EMITS VERDICTS (Gene, 2026-08-16).
   `probe` returns raw observations -- pid sets, directory listings, socket
   inodes, var-root identities, thread ids, a nanosecond instant. It contains
   no threshold, no allowlist and no notion of `pass`. `violations` is a PURE
   function of (namespace, before, after, opts) and holds every verdict. That
   split is what makes these six witnesses testable IN THE FAST LANE: a
   witness plants a synthetic `after` map and asserts the exact typed refusal,
   with no process spawned, no port bound and no day passed. A verdict baked
   at observe time could never be re-applied to a snapshot taken yesterday.

   WHY RUNTIME AND NOT A SOURCE SCAN. Round three already carries the source
   scans (`no-fast-lane-namespace-spells-a-child-process` and friends) and
   round three's own record says why they are not the proof: a scan reads a
   SPELLING, and `battery-ledger-test` is `:fast` while requiring a namespace
   whose CLI half holds a `ProcessBuilder`. A scan built on names cannot see a
   call that names nothing (the scanner-brief lesson). A descendant-pid diff
   sees the child or there was no child.

   ORDERING MATTERS, AND IS DELIBERATE. `probe` captures the process set LAST
   on the way in and FIRST on the way out, so that this fixture's own probing
   -- which touches the filesystem and, at the LANE boundary only, shells out
   to `git` -- can never appear inside the window it is measuring. Nothing in
   the per-namespace window spawns anything: the working-tree check walks the
   tree in-process rather than calling `git status`, and the listener check
   reads `/proc`, because a witness that spawns a child to prove no child was
   spawned is the verifier being blind to its own subject."
  (:require
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clj-surgeon.spawn-ledger :as spawn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.io File)
   (java.lang ProcessHandle)
   (java.nio.file Files)))

;; ---------------------------------------------------------------------------
;; Configuration -- every threshold and every exemption is DATA, in one place,
;; each carrying the reason it exists. An exemption with no reason is how a
;; witness stops being one.
;; ---------------------------------------------------------------------------

(def default-namespace-budget-ms
  "@spec TEST-ISO-007 -- the per-namespace ceiling for a lane namespace that
   declares none of its own. Round one measured 36 fast namespaces finishing
   865 tests in 20.9 s; 8 s for any ONE of them is generous by ~14x on the
   mean and still catches a namespace that has quietly become a battery."
  8000)

(def lane-budget-ms
  "@spec TEST-ISO-007 -- the per-LANE ceiling. The fast lane's whole reason to
   exist is `under 60 s cold` (lane-manifest, TEST-ISO-001). A lane budget is
   NOT implied by the per-namespace budgets: 38 namespaces each comfortably
   under 8 s can still sum to five minutes, and the number the fleet actually
   pays is the sum."
  {:fast 60000
   :integration 240000
   :battery 1800000})

(def lane-default-budget-ms
  "@spec TEST-ISO-007 -- the per-namespace default, BY LANE. A lane is a cost
   class, so one default across all three is either useless for the fast lane
   or a false alarm for the others: the first live run refused
   `mcp-hot-verify-test` at 10 128 ms against the fast lane's 8 s, and that
   namespace is `:integration` precisely because it drives an in-process
   server and waits on it.

   Measured (2026-09-04, load 5.4): the whole fast lane is 34 s across 39
   namespaces, and the slowest integration namespace is 10.1 s. The
   integration ceiling is set at 20 s -- roughly 2x the measured worst case,
   so contention on a shared box does not manufacture a refusal, while a
   namespace that has genuinely doubled still says so."
  {:fast 8000
   :integration 20000
   :battery 300000})

(def namespace-budget-overrides
  "@spec TEST-ISO-007 -- namespaces whose ceiling is not the default, each with
   the reason. An override is a DECLARED cost, reviewable at the pin, never a
   silent raise of the default for everybody. namespace -> ceiling in ms."
  '{;; MEASURED 47.3 s at round five's merge of MCP/main (load ~1, cores 6-9).
    ;; Sixty-nine deftests over the `feature_thread` verb, most of them
    ;; building a real multi-file workspace and threading it: the cost is
    ;; fixture construction, not a cold runtime. It is :integration rather
    ;; than :fast for exactly that reason -- at 47 s it alone would have blown
    ;; the fast lane's 60 s whole-lane ceiling, and it did: the lane ran
    ;; 69 428 ms with it in. The ceiling here is ~2x the measurement, so
    ;; contention on a shared box does not manufacture a refusal while a
    ;; namespace that has genuinely doubled still says so.
    clj-surgeon.mcp-feature-thread-test 90000
    ;; MEASURED 494 s in CI (suite-spike spec, 2026-09-04: "reader-eval-fence-test =
    ;; 494 of CI's 521 s") and 466.9 s on 2026-09-05 02:43Z (battery lane, cores 6-9,
    ;; contended by a peer full gate). About twenty launcher drives of the reader
    ;; eval fence, each a real child JVM: the cost is process spawn, not a slow
    ;; assertion. The :battery default of 300 s landed (TEST-ISO-007) AFTER the last
    ;; passing battery receipt, so at the default this namespace fails the battery
    ;; by construction -- which blocked every landing on 2026-09-05. Ceiling here is
    ;; ~2x the CI measurement, the same rule as the entry above. OWED: split the
    ;; launcher drives into matrix cells (the spec's own plan) and retire this line.
    clj-surgeon.reader-eval-fence-test 1000000})

(def mutable-global-allowlist
  "@spec TEST-ISO-005 -- vars whose deref'd value is EXPECTED to differ across
   a namespace, each with the reason. Caches and registries are legitimately
   mutable; a `with-redefs` that leaked is not, and no allowlist entry may
   cover a var ROOT identity change -- only a value change inside a container
   the var holds. That distinction is enforced below, not merely documented:
   `root-identity-violations` never consults this map."
  '#{;; the port-0 allocator's own ledger (TEST-ISO-004). It is append-only
     ;; within a run BY DESIGN -- recording an allocation is the mechanism, so
     ;; the namespace that allocates a port necessarily moves it.
     clj-surgeon.ns-isolation/allocated-ports

     ;; the prepared-change basis store: a CACHE keyed by request, populated
     ;; by any namespace that prepares a change and read back by the one that
     ;; confirms it. Its growth across a namespace is the mechanism working.
     clj-surgeon.mcp-change-buffer/basis-store

     ;; the semantic client's runtime handle: a memoised connection holder.
     ;; A namespace that asks a semantic question populates it; the value is a
     ;; handle, not test state, and it is never read as an assertion.
     clj-surgeon.mcp-semantic-client/runtime

     ;; the cclsp config lock registry: an interning table of per-path lock
     ;; objects. It only ever GROWS, by design -- two callers naming the same
     ;; path must get the same lock, which is what an interning table is for.
     clj-surgeon.workspace-onboarding/cclsp-config-locks

     ;; the spawn ledger (TEST-ISO-002). Append-only within a run BY DESIGN --
     ;; recording a launch IS the mechanism, exactly like `allocated-ports`
     ;; above, so any namespace that spawns necessarily moves it. Note the
     ;; shape of this entry: it allows the VALUE inside the atom to move; a
     ;; `with-redefs` that swapped the var's root would still be refused by
     ;; `root-identity-violations`, which never reads this map.
     clj-surgeon.spawn-ledger/ledger})

(def cold-runtime-command
  "@spec TEST-ISO-002 -- commands that are NEVER allowlistable in the fast or
   integration lanes, whatever any allowlist says. These are the 674 s round
   one measured: a cold JVM, a cold `bb`, a CLI of ours, the linter, `git`.
   The rule they carry is the partition's whole reason to exist, so the
   allowlist below is written so that it CANNOT reach them."
  #"(?:^|/)(?:java|clojure|clj|bb|babashka|clj-kondo|git|node|npm|python3?|make)(?:\s|$)")

(def fast-lane-spawn-allowlist
  "@spec TEST-ISO-002 -- the ONLY child processes a merge-gate namespace may
   start: namespace -> [[exact command prefix, reason] ...].

   WHY THIS EXISTS, and why it is not a loosening. The round-three landing
   review's finding 6 asked for one of two things: reclassify the offending
   namespace, or `change the lane contract so declared isolation matches
   execution`. Round five's spawn ledger then found that the problem was
   larger than the one namespace the review read -- five more spawns across
   two more `:fast` namespaces, none of them visible to any earlier control.

   Two of the discovered drives were reclassified, because they belong to
   subsystems that have a battery home: the cold-verification job
   (`mcp-inspect-cold-job-test`) and the `sed` oracle
   (`mcp-feature-thread-sed-test`). The rest are different in kind: the
   command IS the subject. `run-exact-verification!` is a runner of
   user-supplied verify commands, and a test of it that never runs one is a
   test of a mock. Those cannot be moved without moving the boundary they
   prove off the merge gate -- which is precisely the coverage loss the review
   objected to.

   So the contract becomes precise instead of loose. It reads: NO child
   process, except these exact commands, in these exact namespaces, for these
   reasons -- and anything else is refused by pid and command line. Three
   properties keep it a ratchet rather than an escape hatch:

     1. `cold-runtime-command` can never be allowlisted. A JVM, `bb`,
        `clj-kondo` or `git` is refused even if someone lists it here, so the
        674 s this partition exists to remove cannot come back through this
        door.
     2. It is a PREFIX match on the command line, per namespace. A new command
        in an allowlisted namespace still fails by name.
     3. A LIVE child is refused regardless -- this map only ever excuses a
        child that was launched and reaped inside the same namespace."
  '{clj-surgeon.mcp-change-buffer-test
    [["/usr/bin/printf" "the verify-command runner's own boundary: a passing profile whose output exceeds the visible byte limit, proving truncation and the sha256 of the full stream"]
     ["/usr/bin/false" "the ordinary-nonzero outcome of the same runner"]
     ["/bin/sleep" "the timeout outcome of the same runner, at :timeout-ms 1"]]

    clj-surgeon.mcp-compact-relations-test
    [["/usr/bin/true" "the project-owned exact-verify profile actually running, so `verify exact` is proved end to end rather than mocked"]
     ["/usr/bin/false" "the same profile failing, so a refusal is proved by the same path"]]

    ;; The INTEGRATION lane, same rule and same reason. Its lane text already
    ;; says `still no cold child JVM`, and these are not: they are the
    ;; user-supplied verify commands whose handling is the subject.
    clj-surgeon.mcp-http-server-test
    [["/usr/bin/true" "the verification profile the HTTP surface is asked to run, actually run, so the wire contract is proved over a real process result"]]

    clj-surgeon.mcp-tool-test
    [["/usr/bin/true" "a passing project verify profile, run for real through the tool boundary"]
     ["/usr/bin/false" "the same boundary refusing, by the same path"]
     ["/bin/test" "the profile asserting a file the change was supposed to produce -- the check a user would actually configure"]
     ["/bin/sh -c sleep 0.05;" "the cold-verification proof: a command that outlives the call, so `verification_pending` is a real state rather than a mocked one"]]})

(defn allowlisted-spawn?
  "@spec TEST-ISO-002 -- true when `command` is a declared fixture command for
   `ns-sym`. A cold runtime is never allowlisted, whatever the map says."
  [ns-sym command]
  (let [cmd (str command)]
    (and (not (re-find cold-runtime-command cmd))
         (boolean (some (fn [[prefix _reason]] (str/starts-with? cmd prefix))
                        (get fast-lane-spawn-allowlist ns-sym))))))

(def enforced-intents-by-lane
  "WHICH of the six intents each lane is HELD TO, and why the answer differs.

   The six rules are the FAST lane's rules; applying all of them everywhere
   would make the witnesses fire on lanes that are defined by doing the thing
   the rule forbids -- the battery lane exists to launch cold child JVMs, and a
   witness that refuses it for launching one is not a ratchet, it is noise that
   teaches people to delete witnesses.

   The integration lane keeps 002 (its lane rule already says `still no cold
   child JVM`), 007 and 010; it is exempt from 003 and 004 because binding an
   EPHEMERAL port and writing a per-test workspace into the repository root
   are precisely what puts a namespace in it. The battery lane keeps only 007,
   because a wall is meaningful for any lane and a namespace that has doubled
   in cost should say so wherever it lives.

   This map is the honest statement of the fixture's REACH. A reader must be
   able to see what is NOT being checked without inferring it from silence."
  {:fast #{"TEST-ISO-002" "TEST-ISO-003" "TEST-ISO-004" "TEST-ISO-005"
           "TEST-ISO-007" "TEST-ISO-010"}
   :integration #{"TEST-ISO-002" "TEST-ISO-007" "TEST-ISO-010"}
   :battery #{"TEST-ISO-007"}})

(defn enforced
  "The violations `lane` is actually held to. Anything filtered out here is
   still OBSERVED and still reportable -- it is a fact either way; it simply
   does not fail the lane."
  [lane vs]
  (let [ids (get enforced-intents-by-lane lane #{})]
    (vec (filter (comp ids :intent) vs))))

(def structural-tmp-entries
  "@spec TEST-ISO-003 -- top-level names under the run's temp root that the RUN
   itself owns, and which therefore belong to no namespace. Today that is the
   throwaway `user.home` TEST-ISO-006 launches the JVM on: it is created by the
   same act that creates the root and destroyed by the same sweep, and a test
   that writes into it is writing into a directory that will not outlive the
   run -- which is the whole point of isolating the home.

   Read from `tmp-leak-support` rather than spelled here, because a witness
   built on a spelling of another mechanism's name breaks silently when that
   mechanism is renamed and reports the rename as a purity violation."
  #{tmp-leak/isolated-home-name})

(def declared-namespace-reloads
  "@spec TEST-ISO-005 -- test namespace -> {production namespace -> reason}.

   Reloading a production namespace replaces EVERY var root in it, which is
   the loudest possible TEST-ISO-005 signal and, for one namespace in this
   tree, is the behaviour under test: `mcp-inspect-tool-test` exercises the
   hot-reload path the MCP server actually uses. Exempting it wholesale would
   turn the rule off for that namespace; exempting a NAMED production
   namespace for a NAMED test namespace keeps every other leak in it -- and
   every leak of any other namespace -- still failing.

   This is deliberately narrow and deliberately noisy to add to. A second
   entry here should provoke the question `why is a fast-lane test reloading
   production code?` rather than being routine paperwork."
  '{clj-surgeon.mcp-inspect-tool-test
    {clj-surgeon.mcp-inspect-tool
     "handler-namespace-reload-preserves-the-live-runtime deliberately calls (require ... :reload) -- reloading the handler IS the behaviour under test"}})

(def ^:private worktree-skip-dirs
  "Directories excluded from the working-tree walk. `.git` because its
   contents change on every read of a ref and it is not the subject; `target`
   because it is probed separately with its own intent; the caches because a
   compile is not a test writing to the tree."
  #{".git" "target" "node_modules" ".cpcache" ".clj-kondo" ".lsp" ".portal"})

;; ---------------------------------------------------------------------------
;; The probes. Facts only.
;; ---------------------------------------------------------------------------

(defn descendant-processes
  "@spec TEST-ISO-002 -- every LIVE descendant of this JVM right now, as
   {pid -> command line}. `ProcessHandle/descendants` is transitive, so a
   grandchild spawned by a `bash -c` wrapper is seen too; a source scan for
   `ProcessBuilder` in the test file would see neither."
  []
  (into {}
        (map (fn [^ProcessHandle h]
               [(.pid h)
                (or (.orElse (.commandLine (.info h)) nil)
                    (.orElse (.command (.info h)) nil)
                    "<command line unavailable>")]))
        (iterator-seq (.iterator (.descendants (ProcessHandle/current))))))

(defn- dir-entries
  "Top-level names in `dir` with each entry's last-modified stamp. Directory
   mtimes move when an entry is added or removed inside them, so depth 1 with
   mtimes detects a write one level down without walking a build output."
  [^File dir]
  (if (.isDirectory dir)
    (into {} (map (fn [^File f] [(.getName f) (.lastModified f)]))
          (or (.listFiles dir) []))
    {}))

(defn tmp-root
  "The run's private temp root. TEST-ISO-006 guarantees this is a throwaway
   created for this run, so its top-level listing is a closed world: anything
   new in it was put there by the namespace that just ran."
  ^File []
  (io/file (System/getProperty "java.io.tmpdir")))

(defn namespace-tmp-dir-name
  "@spec TEST-ISO-003 -- the ONE top-level name under the run's temp root that
   a given namespace is allowed to create. Deterministic from the namespace
   symbol, so the fold can decide `is this new entry yours?` without the probe
   having to know whose window it is in."
  [ns-sym]
  (str "nsiso-" ns-sym))

(defn namespace-tmp-dir
  "Creates (and returns) this namespace's own subdir of the run's temp root.
   A test that needs scratch space in the fast lane should take it from here;
   anything it leaves behind is attributed to it by name instead of becoming
   an anonymous entry the run-level tmp ratchet reports without an owner."
  ^File [ns-sym]
  (doto (io/file (tmp-root) (namespace-tmp-dir-name ns-sym))
    (.mkdirs)))

(defn- walk-worktree
  "path -> [size mtime] for every regular file in the repository working tree,
   skipping `worktree-skip-dirs`. In-process on purpose: `git status` would be
   a child process inside the very window TEST-ISO-002 is measuring."
  [^File root]
  ;; A VOLATILE around the transient, not a bare local. `assoc!` may RETURN a
  ;; different object when the map outgrows its array-map representation, so
  ;; ignoring its return value silently drops every entry past the eighth --
  ;; which would leave this probe reporting a nine-file repository as clean.
  (let [out (volatile! (transient {}))
        root-path (.getCanonicalPath root)]
    (letfn [(walk [^File d]
              (doseq [^File f (or (.listFiles d) [])]
                (cond
                  (Files/isSymbolicLink (.toPath f)) nil
                  (.isDirectory f) (when-not (worktree-skip-dirs (.getName f))
                                     (walk f))
                  (.isFile f)
                  (let [p (.getPath f)
                        rel (if (str/starts-with? p root-path)
                              (subs p (min (count p) (inc (count root-path))))
                              p)]
                    (vswap! out assoc! rel [(.length f) (.lastModified f)])))))]
      (walk root))
    (persistent! @out)))

(defn- socket-inodes-of-this-process
  "Inode numbers of every socket this JVM holds open, read from /proc/self/fd.
   Restricting the listener scan to OUR inodes is what makes it a witness
   about this suite rather than about the box: 29 other seats run on Anvil and
   their listeners must not fail our lane."
  []
  (let [d (io/file "/proc/self/fd")]
    (into #{}
          (keep (fn [^File f]
                  (try
                    (let [target (str (Files/readSymbolicLink (.toPath f)))]
                      (when-let [[_ inode] (re-matches #"socket:\[(\d+)\]" target)]
                        (Long/parseLong inode)))
                    (catch Exception _ nil))))
          (or (.listFiles d) []))))

(defn- read-proc-file
  "Reads a /proc file. NOT `slurp`: measured on this box, `slurp` on
   /proc/net/tcp throws `IOException: Invalid argument` out of
   `FileInputStream.available0` -- a procfs entry reports a zero length and
   does not answer `available()`. `Files/readAllBytes` reads it correctly.
   Returns nil when the file is absent, so a non-Linux box degrades to `no
   listeners observed` rather than to an exception inside every namespace."
  [path]
  (try
    (let [f (io/file path)]
      (when (.exists f)
        (String. (Files/readAllBytes (.toPath f)))))
    (catch Exception _ nil)))

(defn- listening-inode->port
  "Parses /proc/net/tcp and /proc/net/tcp6 for sockets in state 0A (LISTEN),
   returning inode -> port. Pure file reads; no `ss`, no `netstat`, no child."
  []
  (into {}
        (comp
         (mapcat (fn [p]
                   (when-let [text (read-proc-file p)]
                     (rest (str/split-lines text)))))
         (keep (fn [line]
                 (let [cols (str/split (str/trim line) #"\s+")]
                   (when (and (>= (count cols) 10) (= "0A" (nth cols 3)))
                     (let [local (nth cols 1)
                           port (Long/parseLong (second (str/split local #":")) 16)
                           inode (Long/parseLong (nth cols 9))]
                       [inode port]))))))
        ["/proc/net/tcp" "/proc/net/tcp6"]))

(defn own-listeners
  "@spec TEST-ISO-004 -- {inode -> port} for every listening socket THIS JVM
   owns. A server left listening after a namespace finishes is a leaked
   resource that makes the next N-wide run flaky in a way that reproduces
   only under concurrency."
  []
  (let [mine (socket-inodes-of-this-process)]
    (into {} (filter (comp mine key)) (listening-inode->port))))

(defn- project-vars
  "Every var interned in a loaded `clj-surgeon.*` namespace."
  []
  (for [n (all-ns)
        :when (str/starts-with? (str (ns-name n)) "clj-surgeon.")
        [sym v] (ns-interns n)]
    [(symbol (str (ns-name n)) (str sym)) v]))

(defn var-root-identities
  "@spec TEST-ISO-005 -- fully-qualified var symbol -> IDENTITY hash of its
   root value. Identity, not equality, and the raw root, not a deref: a
   `with-redefs` that leaked (a test that threw inside the body, a future that
   outlived it) swaps the root for a different object, which identity sees and
   value equality can miss. `getRawRoot` also never FORCES anything -- a
   `delay` or a `promise` held in a var is observed, not realised, which a
   `deref`-based probe would corrupt on the way to measuring it."
  []
  (into {}
        (map (fn [[sym ^clojure.lang.Var v]]
               [sym (System/identityHashCode (.getRawRoot v))]))
        (project-vars)))

(defn global-container-values
  "@spec TEST-ISO-005 -- fully-qualified var symbol -> the VALUE currently held
   by an atom/ref/volatile the var's root IS. Deliberately restricted to those
   three container types: they are the ones a test mutates without rebinding
   anything, so a root-identity probe cannot see it. Delays, promises, futures
   and other `IDeref`s are excluded because observing them can realise them."
  []
  (into {}
        (keep (fn [[sym ^clojure.lang.Var v]]
                (let [root (.getRawRoot v)]
                  (when (or (instance? clojure.lang.Atom root)
                            (instance? clojure.lang.Ref root)
                            (instance? clojure.lang.Volatile root))
                    (try [sym (hash @root)] (catch Throwable _ nil))))))
        (project-vars)))

(defn live-non-daemon-threads
  "@spec TEST-ISO-010 -- {thread id -> name} for every LIVE non-daemon thread.
   Non-daemon specifically: a leaked non-daemon thread is the one that keeps
   the JVM from exiting, which is how a suite that reports 0 failures still
   hangs a CI runner until its timeout."
  []
  (into {}
        (keep (fn [^Thread t]
                (when (and (.isAlive t) (not (.isDaemon t)))
                  [(.threadId t) (.getName t)])))
        (keys (Thread/getAllStackTraces))))

(defn probe
  "One snapshot of every resource the six witnesses watch.

   The process set is captured LAST here and FIRST in the paired call (see
   `probe-after`), so this fixture's own filesystem and /proc reads can never
   land inside the window it measures."
  [repo-root]
  (let [tmp (tmp-root)]
    {:instant-ns (System/nanoTime)
     :tmp-entries (dir-entries tmp)
     :target-entries (dir-entries (io/file repo-root "target"))
     :worktree (walk-worktree (io/file repo-root))
     :listeners (own-listeners)
     :var-roots (var-root-identities)
     :globals (global-container-values)
     :threads (live-non-daemon-threads)
     :processes (descendant-processes)
     ;; @spec TEST-ISO-002 -- the append-only launch record. Cheap (a deref)
     ;; and, unlike the pid set, not erased by the child exiting.
     :spawns (spawn/snapshot)}))

(defn probe-after
  "The paired snapshot. Identical content to `probe`; the process set is read
   FIRST so that a child this probe's own work might spawn cannot be counted
   against the namespace that just finished."
  [repo-root]
  (let [processes (descendant-processes)
        spawns (spawn/snapshot)
        p (probe repo-root)]
    (assoc p :processes processes :spawns spawns)))

;; ---------------------------------------------------------------------------
;; The fold. Verdicts only, and PURE -- every one of these is reachable from a
;; witness that plants a synthetic map, with nothing spawned and nothing bound.
;; ---------------------------------------------------------------------------

(defn- violation
  [intent ns-sym resource detail]
  {:intent intent :namespace ns-sym :resource resource :detail detail})

(defn message
  "The typed refusal line. Every violation names the INTENT, the NAMESPACE and
   the RESOURCE -- delivery invariant 20: a receipt must name its subject and
   its evidence source, or it is not a receipt."
  [{:keys [intent namespace resource detail]}]
  (format "%s VIOLATION in %s -- %s: %s" intent namespace resource detail))

(defn process-violations
  "@spec TEST-ISO-002 -- TWO independent observations of the same rule, because
   neither one alone can see a child process.

   the LIVE PID DIFF   `ProcessHandle/descendants` across the window. Sees a
                       child that is still running -- a server, a hung
                       analyzer, anything the namespace forgot to reap. It
                       CANNOT see a child that already exited, and a test that
                       waits for its child is precisely that case.

   the SPAWN LEDGER    `clj-surgeon.spawn-ledger`, appended to by every
                       repository-owned spawn helper at the moment of launch.
                       Sees the EVENT, so exiting does not erase it. It cannot
                       see a raw `ProcessBuilder` a test builds itself -- that
                       is the source scan's and the pid diff's half.

   The round-three landing review's finding 6 is the case where only the
   second one fires: `/bin/sh -c 'printf cold-ok'` through the production
   cold-verify helper, waited on to completion, inside a `:fast` namespace
   whose lane rule reads `No child process`.

   A pid seen by both is reported ONCE, as the live-descendant kind, which is
   the more serious of the two."
  [ns-sym before after]
  (let [new-pids (set/difference (set (keys (:processes after)))
                                 (set (keys (:processes before))))
        recorded (spawn/recorded-between (:spawns before) (:spawns after))
        exited (->> recorded
                    (remove (comp new-pids :pid))
                    (remove #(allowlisted-spawn? ns-sym (:command %))))]
    (into
     (mapv (fn [pid]
             (violation "TEST-ISO-002" ns-sym "process spawn"
                        (format "pid %d is a live descendant that did not exist before this namespace ran: %s"
                                pid (get-in after [:processes pid]))))
           (sort new-pids))
     (mapv (fn [{:keys [pid command]}]
             (violation "TEST-ISO-002" ns-sym "process spawn"
                        (format (str "pid %d was launched by this namespace through a "
                                     "repository spawn helper and has already exited, so no "
                                     "live-descendant snapshot can see it: %s")
                                pid command)))
           exited))))

(defn- entry-diff
  "Names that appeared, or whose stamp moved, between two `dir-entries` maps."
  [before after]
  (sort (concat (remove (set (keys before)) (keys after))
                (keep (fn [[k v]]
                        (when (and (contains? before k) (not= v (get before k))) k))
                      after))))

(defn write-violations
  "@spec TEST-ISO-003 -- three subjects, one intent: the run's temp root,
   `target/`, and the repository working tree. A new top-level temp entry is
   allowed ONLY when it is this namespace's own allocated subdir; anything
   else is a write outside its own subtree, named with its path."
  [ns-sym before after]
  (let [own (namespace-tmp-dir-name ns-sym)]
    (vec
     (concat
      (for [n (entry-diff (:tmp-entries before) (:tmp-entries after))
            :when (and (not= n own) (not (structural-tmp-entries n)))]
        (violation "TEST-ISO-003" ns-sym "temp root"
                   (format "%s appeared or changed directly under java.io.tmpdir (%s); a fast-lane namespace may write only inside its own subdir %s"
                           n (System/getProperty "java.io.tmpdir") own)))
      (for [n (entry-diff (:target-entries before) (:target-entries after))]
        (violation "TEST-ISO-003" ns-sym "target/"
                   (format "target/%s appeared or changed; the build output is shared by every lane running from this checkout" n)))
      (let [b (:worktree before) a (:worktree after)
            added (sort (remove (set (keys b)) (keys a)))
            removed (sort (remove (set (keys a)) (keys b)))
            changed (sort (keep (fn [[k v]] (when (and (contains? b k) (not= v (get b k))) k)) a))]
        (concat
         (for [p added] (violation "TEST-ISO-003" ns-sym "working tree"
                                   (format "%s was created in the repository working tree" p)))
         (for [p removed] (violation "TEST-ISO-003" ns-sym "working tree"
                                     (format "%s was deleted from the repository working tree" p)))
         (for [p changed] (violation "TEST-ISO-003" ns-sym "working tree"
                                     (format "%s was modified in the repository working tree" p)))))))))

(defn listener-violations
  "@spec TEST-ISO-004 -- a socket this JVM is still listening on that it was
   not listening on before, named with its port. The port-0 allocator's ledger
   (`allocated-ports`) is consulted only to say WHICH allocation leaked -- a
   port that is in the ledger and still listening is a leak with an owner, and
   one that is not in the ledger is a leak AND a fixed port."
  [ns-sym before after ledger]
  (let [new-inodes (set/difference (set (keys (:listeners after)))
                                   (set (keys (:listeners before))))]
    (mapv (fn [inode]
            (let [port (get-in after [:listeners inode])]
              (violation "TEST-ISO-004" ns-sym "listening socket"
                         (format "port %d is still listening after this namespace finished (%s)"
                                 port
                                 (if (contains? (set (vals ledger)) port)
                                   "allocated through the port-0 allocator and never closed"
                                   "NOT allocated through the port-0 allocator -- a fixed port literal is the usual cause")))))
          (sort new-inodes))))

(defn- root-identity-violations
  [ns-sym before after reloads]
  (let [declared (set (keys (get reloads ns-sym)))]
    (for [[sym h] (:var-roots after)
          :let [b (get (:var-roots before) sym ::absent)]
          :when (and (not= b ::absent) (not= b h)
                     (not (declared (symbol (namespace sym)))))]
      (violation "TEST-ISO-005" ns-sym "var root"
                 (format "#'%s has a different root object than before this namespace ran -- a with-redefs, an alter-var-root or a :reload leaked out of its scope" sym)))))

(defn global-violations
  "@spec TEST-ISO-005 -- two kinds, and only ONE of them is exemptible.

   A var ROOT that changed identity is never allowed: that is a leaked
   `with-redefs`/`alter-var-root`, and there is no legitimate reason for one to
   survive a namespace. A VALUE inside an atom the var holds may legitimately
   move (a cache, a registry), so that class alone consults the allowlist --
   and each entry there carries its reason."
  [ns-sym before after allowlist reloads]
  (vec
   (concat
    (root-identity-violations ns-sym before after reloads)
    (for [[sym h] (:globals after)
          :let [b (get (:globals before) sym ::absent)]
          :when (and (not= b ::absent) (not= b h) (not (contains? allowlist sym)))]
      (violation "TEST-ISO-005" ns-sym "global container"
                 (format "the value held by #'%s changed across this namespace; if that is legitimate, add it to clj-surgeon.ns-isolation/mutable-global-allowlist WITH the reason" sym))))))

(defn budget-violations
  "@spec TEST-ISO-007"
  [ns-sym before after overrides default-ms]
  (let [ms (quot (- (:instant-ns after) (:instant-ns before)) 1000000)
        budget (get overrides ns-sym default-ms)]
    (if (> ms budget)
      [(violation "TEST-ISO-007" ns-sym "time budget"
                  (format "ran for %d ms, over its %d ms budget; declare an override in clj-surgeon.ns-isolation/namespace-budget-overrides WITH the reason, or move it to a slower lane"
                          ms budget))]
      [])))

(def shared-pool-thread-name
  "@spec TEST-ISO-010 -- thread names that belong to a JVM-GLOBAL pool rather
   than to the namespace that happened to be running when the pool grew.

   `clojure-agent-send-off-pool-N` is Clojure's own cached pool behind
   `send-off` and `future`. It is non-daemon by design, it has a 60 second
   keep-alive, and it is shared by every namespace in the run -- so which
   namespace `created` a worker is an artefact of scheduling, not of
   ownership. Measured on this branch: the same thread was attributed to
   `workspace-onboarding-test` on one run and to `mcp-tool-test` on the next,
   and a third run reported none. A witness whose verdict moves with the
   weather is not a ratchet; it is a thing people learn to re-run until it is
   green, and that habit is worse than the check is worth.

   The rule it protects is unchanged and still enforced: a namespace that
   starts its OWN non-daemon thread and leaves it running is refused by id and
   name. The only remedy for the shared pool is `shutdown-agents`, which would
   break every namespace that runs after it -- the correct place for that is
   the end of the whole run, not the end of a namespace."
  #"^clojure-agent-send-off-pool-\d+$")

(defn thread-violations
  "@spec TEST-ISO-010"
  [ns-sym before after]
  (let [new-ids (->> (set/difference (set (keys (:threads after)))
                                     (set (keys (:threads before))))
                     (remove #(re-matches shared-pool-thread-name
                                          (str (get-in after [:threads %])))))]
    (mapv (fn [id]
            (violation "TEST-ISO-010" ns-sym "non-daemon thread"
                       (format "thread %d (%s) is alive and non-daemon after this namespace finished; a leaked non-daemon thread keeps the JVM from exiting even when every test passed"
                               id (get-in after [:threads id]))))
          (sort new-ids))))

(defn violations
  "The whole fold: every violation the six witnesses find, in intent order.
   Pure -- (namespace, before, after, opts) in, verdicts out."
  ([ns-sym before after] (violations ns-sym before after {}))
  ([ns-sym before after {:keys [ledger allowlist overrides default-budget-ms reloads]
                         :or {ledger {}
                              allowlist mutable-global-allowlist
                              overrides namespace-budget-overrides
                              reloads declared-namespace-reloads
                              default-budget-ms default-namespace-budget-ms}}]
   (vec (concat (process-violations ns-sym before after)
                (write-violations ns-sym before after)
                (listener-violations ns-sym before after ledger)
                (global-violations ns-sym before after allowlist reloads)
                (budget-violations ns-sym before after overrides default-budget-ms)
                (thread-violations ns-sym before after)))))

(defn lane-budget-violation
  "@spec TEST-ISO-007 -- the lane total, which is the number the fleet pays."
  [lane total-ms]
  (when-let [budget (get lane-budget-ms lane)]
    (when (> total-ms budget)
      (violation "TEST-ISO-007" lane "lane time budget"
                 (format "the %s lane took %d ms, over its %d ms budget"
                         (name lane) total-ms budget)))))

;; ---------------------------------------------------------------------------
;; @spec TEST-ISO-004 -- the port-0 allocator and its ledger.
;;
;; The witness above DETECTS a leaked listener. This makes the good path the
;; easy one: a test that needs a port asks for one, gets an ephemeral one the
;; kernel chose, and is recorded. A fixed port literal is then not merely
;; discouraged, it is the only way to get a port that has no ledger entry --
;; which is exactly what the refusal above names.
;; ---------------------------------------------------------------------------

(defonce ^{:doc "allocation id -> port, append-only within one run."}
  allocated-ports
  (atom {}))

(defn allocate-port!
  "Binds port 0, reads back the ephemeral port the kernel chose, closes the
   socket and records the allocation. Returns the port.

   Yes, there is a race between closing and rebinding; it is the standard one
   and it is bounded by the ephemeral range being large and the window being
   microseconds. The alternative -- a fixed port -- is not a smaller race, it
   is a certainty of collision the moment two clones run at once, which is the
   failure round one measured."
  [owner]
  (with-open [s (java.net.ServerSocket. 0)]
    (let [port (.getLocalPort s)]
      (swap! allocated-ports assoc [owner (System/nanoTime)] port)
      port)))
