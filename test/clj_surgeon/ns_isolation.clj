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

(def namespace-budget-overrides
  "@spec TEST-ISO-007 -- namespaces whose ceiling is not the default, each with
   the reason. An override is a DECLARED cost, reviewable at the pin, never a
   silent raise of the default for everybody."
  '{})

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
     clj-surgeon.ns-isolation/allocated-ports})

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

(defn- listening-inode->port
  "Parses /proc/net/tcp and /proc/net/tcp6 for sockets in state 0A (LISTEN),
   returning inode -> port. Pure file reads; no `ss`, no `netstat`, no child."
  []
  (into {}
        (comp
         (mapcat (fn [p]
                   (let [f (io/file p)]
                     (when (.isFile f)
                       (rest (str/split-lines (slurp f)))))))
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
     :processes (descendant-processes)}))

(defn probe-after
  "The paired snapshot. Identical content to `probe`; the process set is read
   FIRST so that a child this probe's own work might spawn cannot be counted
   against the namespace that just finished."
  [repo-root]
  (let [processes (descendant-processes)
        p (probe repo-root)]
    (assoc p :processes processes)))

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
  "@spec TEST-ISO-002"
  [ns-sym before after]
  (let [new-pids (set/difference (set (keys (:processes after)))
                                 (set (keys (:processes before))))]
    (mapv (fn [pid]
            (violation "TEST-ISO-002" ns-sym "process spawn"
                       (format "pid %d is a live descendant that did not exist before this namespace ran: %s"
                               pid (get-in after [:processes pid]))))
          (sort new-pids))))

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
            :when (not= n own)]
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
  [ns-sym before after]
  (for [[sym h] (:var-roots after)
        :let [b (get (:var-roots before) sym ::absent)]
        :when (and (not= b ::absent) (not= b h))]
    (violation "TEST-ISO-005" ns-sym "var root"
               (format "#'%s has a different root object than before this namespace ran -- a with-redefs or alter-var-root leaked out of its scope" sym))))

(defn global-violations
  "@spec TEST-ISO-005 -- two kinds, and only ONE of them is exemptible.

   A var ROOT that changed identity is never allowed: that is a leaked
   `with-redefs`/`alter-var-root`, and there is no legitimate reason for one to
   survive a namespace. A VALUE inside an atom the var holds may legitimately
   move (a cache, a registry), so that class alone consults the allowlist --
   and each entry there carries its reason."
  [ns-sym before after allowlist]
  (vec
   (concat
    (root-identity-violations ns-sym before after)
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

(defn thread-violations
  "@spec TEST-ISO-010"
  [ns-sym before after]
  (let [new-ids (set/difference (set (keys (:threads after)))
                                (set (keys (:threads before))))]
    (mapv (fn [id]
            (violation "TEST-ISO-010" ns-sym "non-daemon thread"
                       (format "thread %d (%s) is alive and non-daemon after this namespace finished; a leaked non-daemon thread keeps the JVM from exiting even when every test passed"
                               id (get-in after [:threads id]))))
          (sort new-ids))))

(defn violations
  "The whole fold: every violation the six witnesses find, in intent order.
   Pure -- (namespace, before, after, opts) in, verdicts out."
  ([ns-sym before after] (violations ns-sym before after {}))
  ([ns-sym before after {:keys [ledger allowlist overrides default-budget-ms]
                         :or {ledger {}
                              allowlist mutable-global-allowlist
                              overrides namespace-budget-overrides
                              default-budget-ms default-namespace-budget-ms}}]
   (vec (concat (process-violations ns-sym before after)
                (write-violations ns-sym before after)
                (listener-violations ns-sym before after ledger)
                (global-violations ns-sym before after allowlist)
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
