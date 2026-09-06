(ns clj-surgeon.tmp-leak-support
  "RATCHET (2026-09-04, inb-9483a4): shared temp-dir hygiene for the test
   runners. Anvil's /tmp filled to 96% of its inodes from 82,210 leaked
   test-fixture directories -- 19,292 of them `clj-surgeon-change-buffer-*`
   from this repo's change_buffer tests -- while bytes sat at 44%. Every
   process this repo launches for tests must (a) resolve java.io.tmpdir to
   a real disk path (never tmpfs), and (b) leave nothing behind. This
   namespace is required by both test/run_all.clj (bb) and
   test/clj_surgeon/mcp_test_runner.clj (clojure -M) so both lanes share
   one enforcement path.

   TWO measured JDK/Substrate-VM facts drive this design, both confirmed
   2026-09-04 and both easy to get wrong:

   1. bb (babashka's GraalVM native image) does NOT read JAVA_TOOL_OPTIONS
      at all:
        JAVA_TOOL_OPTIONS=\"-Djava.io.tmpdir=/var/tmp/forge\" \\
          bb -e '(println (System/getProperty \"java.io.tmpdir\"))'
      still prints /tmp. Only a literal `-D` flag passed to `bb` itself
      changes it (`bb -Djava.io.tmpdir=/var/tmp/forge ...`). So
      ~/bin/suite-run's env alone does not protect `bb test/run_all.clj`.

   2. NEITHER bb NOR a real `java` (confirmed against `clojure -M`, so this
      is not GraalVM-specific) honors a RUNTIME `System/setProperty
      \"java.io.tmpdir\"` for actual temp-file/dir creation, even though
      `System/getProperty` immediately reflects the new value. Both
      `Files/createTempDirectory` and `File/createTempFile` kept writing to
      the ORIGINAL startup value after an in-process
      `(System/setProperty \"java.io.tmpdir\" ...)`, on both bb and a real
      JVM launched with JAVA_TOOL_OPTIONS already set. The JDK captures the
      property into an effectively-immutable holder at process bootstrap,
      before any user code (even `-main`) runs, and never re-reads it. A
      first cut of this namespace called `System/setProperty` at runtime to
      isolate each run into a private sub-directory; it silently did
      nothing -- `System/getProperty` and the leak witness both reported
      the fake isolated path while real fixtures kept landing in the
      SHARED base (or, for bb without the fix in (1), literally /tmp). Both
      test/run_all.clj runs during that window looked GREEN while actually
      leaking thousands of directories -- a false green from a witness that
      was watching the wrong directory.

   Because of (2), the ONLY way to isolate a run's temp-file creation into
   a private, race-free directory is to set that directory as a literal
   `-D` flag at a PROCESS's OWN startup. `secure-tmpdir!` does this by
   re-executing this suite as a CHILD process (`bb -Djava.io.tmpdir=<root>
   <script>` for bb, or a nested `java -Djava.io.tmpdir=<root> ... -m
   <main-ns>` reusing the already-resolved classpath for `clojure -M`),
   inheriting stdio, and exiting the parent with the child's exit code. A
   sentinel env var prevents infinite re-exec; the child sees it and
   proceeds directly using the java.io.tmpdir it was launched with (which
   IS honored, because it was present before that process's own bootstrap)."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.set :as set]
   [clojure.string :as str]))

(def ^:private reexec-sentinel
  "Env var the parent sets on the re-exec'd child. Its VALUE is the ABSOLUTE
   PATH of the private root the parent created -- never a bare flag. The
   child refuses unless its own java.io.tmpdir IS that path
   (MCP-OP-TMPHYG-004): a sentinel that only said \"1\" made any process that
   inherited it treat the SHARED base as its private run root, and
   `report-and-sweep-leak!` then delete-treed another tenant's working set."
  "CLJ_SURGEON_TMPDIR_REEXEC")

(def ^:private isolated-root-prefix
  "Every root this namespace creates -- and the ONLY name shape it will ever
   delete-tree -- is `clj-surgeon-suite-<pid>-<8 hex>`."
  "clj-surgeon-suite-")

(def ^:private ram-path-prefixes
  "Paths that are RAM-backed on this seat BY NAME -- checked with no external
   binary and no procfs, exactly as ~/bin/seat-tmp-guard.sh does it. This is
   the check that refuses when no mount source can answer at all."
  ["/tmp" "/dev/shm"])

;; @spec TEST-ISO-006
(def isolated-home-name
  "The throwaway `user.home` a home-isolated run is launched on, as a child of
   that run's own private temp root -- so it is created by the same act that
   creates the root and destroyed by the same `sweep-root!` that destroys it.
   A throwaway home with its own lifecycle would be a second thing to leak."
  "home")

(defn isolated-home
  [root]
  (io/file (str root) isolated-home-name))

(defn- canonical
  [dir]
  (try (.getCanonicalPath (io/file (str dir)))
       (catch Throwable _ (str dir))))

(defn- current-pid
  []
  (try (.pid (java.lang.ProcessHandle/current)) (catch Throwable _ 0)))

(defn literal-ram-path?
  "True when `dir` IS, or is under, a path this seat knows to be RAM-backed
   by name (/tmp, /dev/shm) -- checked both as written and canonicalised, so
   a symlink into /tmp cannot slip past."
  [dir]
  (let [candidates (distinct [(str dir) (canonical dir)])]
    (boolean
      (some (fn [p]
              (some (fn [prefix] (or (= p prefix) (str/starts-with? p (str prefix "/"))))
                    ram-path-prefixes))
            candidates))))

(defn- seam-mounts-file
  "The mounts-table override, or nil. `CLJ_SURGEON_MOUNTS_FILE` is a witness
   seam: the ratchet's own gate points it at a nonexistent path to execute the
   \"no mount source can answer\" branch."
  []
  (System/getenv "CLJ_SURGEON_MOUNTS_FILE"))

(defn- mounts-file
  []
  (or (seam-mounts-file) "/proc/mounts"))

(defn- findmnt-fstype
  [dir]
  (try
    (let [{:keys [exit out]} (shell/sh "findmnt" "-n" "-o" "FSTYPE" "--target" (str dir))]
      (when (and (zero? exit) (seq (str/trim out)))
        (str/trim out)))
    (catch Throwable _ nil)))

(defn- mounts-table-fstype
  "Longest-mount-point-prefix scan of the mounts table.

   MEASURED 2026-09-04, and the reason round one's fallback was unreachable
   dead code: procfs reports st_size = 0, so `slurp`, `(.readAllBytes
   (io/input-stream ...))` AND `(line-seq (io/reader ...))` all throw
   `java.io.IOException: Invalid argument` on /proc/mounts -- on bb AND on a
   real JVM. `java.nio.file.Files/lines` is the one approach that reads all
   41 lines on both runtimes, so it is the one used here."
  [dir]
  (try
    (let [file (io/file (mounts-file))]
      (when (.exists file)
        (let [target (canonical dir)
              lines (with-open [stream (java.nio.file.Files/lines (.toPath file))]
                      (vec (iterator-seq (.iterator stream))))
              best (->> lines
                        (keep (fn [line]
                                (let [[_dev mnt fstype] (str/split line #"\s+")]
                                  (when (and mnt fstype
                                             (or (= target mnt)
                                                 (= mnt "/")
                                                 (str/starts-with? target (str mnt "/"))))
                                    [mnt fstype]))))
                        (sort-by (comp count first) >)
                        first)]
          (second best))))
    (catch Throwable _ nil)))

(defn mount-fstype
  "Filesystem type for `dir` as a TRI-STATE: the fstype string when a mount
   source could answer, or `:unknown` when none could.

   Round one returned nil here and `tmpfs?` coerced nil to \"not tmpfs\",
   so an undeterminable filesystem was treated as proven-safe and the suite
   ran on RAM. `:unknown` is a refusal (see `base-refusal`), not a pass."
  [dir]
  (or (findmnt-fstype dir)
      ;; A seam-sourced fstype is NEVER positive proof of real disk. The gate
      ;; only ever needs the seam to produce a REFUSAL, so a forged table can
      ;; refuse (tmpfs) but a non-tmpfs answer from it reads as `nothing could
      ;; answer`. Without this, an operator handing the check a lying table
      ;; converts `I cannot prove this is disk` into `proven disk` -- a gate a
      ;; caller can turn off, and the review ran a whole suite on RAM that way.
      ;; @spec MCP-OP-TMPHYG-011
      (let [fstype (mounts-table-fstype dir)]
        (cond
          (nil? fstype) nil
          (= "tmpfs" fstype) fstype
          (some? (seam-mounts-file)) nil
          :else fstype))
      :unknown))

(defn tmpfs?
  "True when `dir`'s filesystem is KNOWN to be tmpfs (RAM-backed). Note that
   false here means \"not known to be tmpfs\" and is NOT on its own a licence
   to run -- `base-refusal` is the decision function."
  [dir]
  (= "tmpfs" (mount-fstype dir)))

(def ^:private refusal-remedy
  (str "Launch with -Djava.io.tmpdir=/var/tmp/forge, or export "
       "TMPDIR=/var/tmp/forge before invoking bb (bb does not read "
       "JAVA_TOOL_OPTIONS -- see ~/bin/suite-run / seat-tmp-guard.sh)."))

;; @spec MCP-OP-TMPHYG-003
(defn base-refusal
  "nil when `dir` is PROVEN to be a real-disk path; otherwise a typed refusal
   map {:reason :ram-path-prefix|:tmpfs|:unknown-fstype :base ... :fstype ...}.

   Fails CLOSED: every path out of this function that is not a positive proof
   of real disk is a refusal."
  [dir]
  (if (literal-ram-path? dir)
    {:reason :ram-path-prefix :base (str dir)}
    (let [fstype (mount-fstype dir)]
      (cond
        (= :unknown fstype) {:reason :unknown-fstype :base (str dir)}
        (= "tmpfs" fstype) {:reason :tmpfs :base (str dir) :fstype fstype}
        :else nil))))

(defn refusal-message
  [{:keys [reason base fstype detail]}]
  (format "tmp-refused: java.io.tmpdir base=%s %s %s"
          base
          (case reason
            :ram-path-prefix
            "is a RAM-backed path by name (/tmp or /dev/shm)."
            :unknown-fstype
            (str "has an UNDETERMINABLE filesystem type -- neither findmnt nor "
                 "the mounts table could answer, so nothing proves it is not RAM. "
                 "Refusing rather than assuming disk.")
            :tmpfs
            (format "is RAM-backed (tmpfs, fstype=%s)." fstype)
            :unusable-base
            (str "cannot be used as a temp base: " detail)
            :sentinel-mismatch
            (str "was handed a re-exec sentinel it does not own: " detail)
            :node-compile-cache-not-disabled
            "was launched WITHOUT NODE_DISABLE_COMPILE_CACHE=1 required by the test runner."
            :home-not-isolated
            (str "was launched WITHOUT the isolated user.home this run "
                 "requires (TEST-ISO-006): " detail)
            "is not usable as a temp base.")
          refusal-remedy))

(defn- refuse!
  [refusal]
  (binding [*out* *err*] (println (refusal-message refusal)))
  {:refused true})

(defn env-or-current-tmpdir
  "$TMPDIR when set (the seat's env, honored by a real `java` launch and by
   the re-exec'd bb child below), else whatever java.io.tmpdir already
   resolves to."
  []
  (or (System/getenv "TMPDIR") (System/getProperty "java.io.tmpdir")))

(defn- bb-runtime?
  []
  (some? (System/getProperty "babashka.version")))

;; @spec MCP-OP-TMPHYG-006
(defn parent-jvm-options
  "The JVM options THIS process was launched with, minus the ones the child
   must be given fresh (`-Djava.io.tmpdir`, which is the whole point of the
   re-exec) and minus a debugger transport, which cannot bind twice.

   Read REFLECTIVELY on purpose: `java.lang.management.ManagementFactory`
   does not exist in bb's native image, and sci rejects the symbol at
   ANALYSIS time -- before any try/catch could run -- so a literal class
   symbol here would break `bb test/run_all.clj` at load."
  []
  (try
    (let [factory (Class/forName "java.lang.management.ManagementFactory")
          bean (.invoke (.getMethod factory "getRuntimeMXBean" (into-array Class []))
                        nil (into-array Object []))
          method (.getMethod (Class/forName "java.lang.management.RuntimeMXBean")
                             "getInputArguments" (into-array Class []))]
      (->> (.invoke method bean (into-array Object []))
           (remove #(str/starts-with? % "-Djava.io.tmpdir="))
           ;; @spec TEST-ISO-006 -- like java.io.tmpdir, the child is given a
           ;; fresh one; inheriting the parent's would silently defeat the
           ;; isolation the re-exec exists to create.
           (remove #(str/starts-with? % "-Duser.home="))
           (remove #(str/starts-with? % "-agentlib:jdwp"))
           vec))
    (catch Throwable _ [])))

(defn- reexec-child-command
  "The command vector for the isolated child process: `bb -D... <script>`
   under bb, or a nested `java -D... -cp <this process's classpath>
   clojure.main -m <main-ns>` under a real JVM (avoids re-running the
   slower `clojure` CLI / deps resolution; the classpath this process
   already resolved is exactly the one the child needs)."
  [{:keys [bb-script main-ns isolate-home?]} tmp-root args]
  (let [args (mapv str args)
        ;; @spec TEST-ISO-006
        home-flags (when isolate-home?
                     [(str "-Duser.home=" (isolated-home tmp-root))])]
    (if (bb-runtime?)
      (into (into ["bb" (str "-Djava.io.tmpdir=" tmp-root)] home-flags)
            (into [bb-script] args))
      (into (into ["java" "-cp" (System/getProperty "java.class.path")]
                  (into (conj (parent-jvm-options) (str "-Djava.io.tmpdir=" tmp-root))
                        home-flags))
            (into ["clojure.main" "-m" main-ns] args)))))

;; @spec MCP-OP-TMPHYG-004
(defn own-isolated-root?
  "True only for a directory this namespace could have created: a
   `clj-surgeon-suite-*` entry that has a parent. The shared base itself can
   never satisfy this, which is what makes deleting another tenant's working
   set unrepresentable rather than merely unlikely."
  [root]
  (let [f (io/file (str root))]
    (boolean (and (some? (.getParentFile f))
                  (str/starts-with? (.getName f) isolated-root-prefix)))))

;; @spec MCP-OP-TMPHYG-004
;; @spec MCP-OP-TMPHYG-013
(defn sweep-root!
  "Deletes `root` -- but ONLY when it is one of this namespace's own private
   per-run roots. Anything else is a typed, printed refusal returning false.

   The return value is a RECEIPT of what happened, not of what was attempted:
   true only when the root is GONE afterwards. Round two returned true for
   every own-named root, so a delete that failed (an unwritable parent, a
   foreign owner) was counted as swept by `sweep-stale-roots!`."
  [root]
  (if (own-isolated-root? root)
    (do (try (fs/delete-tree root) (catch Throwable _ nil))
        (not (.exists (io/file (str root)))))
    (do (binding [*out* *err*]
          (println (format (str "tmp-refused: refusing to delete %s -- it is not a private "
                                "per-run root (its name must start with %s). Sweeping a shared "
                                "base would destroy another tenant's working set.")
                           root isolated-root-prefix)))
        false)))

;; @spec MCP-OP-TMPHYG-007
(def ^:private stale-root-hours
  "Age past which an isolated root whose OWNING PID IS DEAD is swept at the
   next run's startup. Override with CLJ_SURGEON_TMP_STALE_HOURS."
  (or (some-> (System/getenv "CLJ_SURGEON_TMP_STALE_HOURS") parse-long) 4))

(defn- root-owner-pid
  "The pid encoded in one of this namespace's own root names, or nil for any
   other name -- including a legacy `clj-surgeon-suite-<hex>` root and every
   entry another tenant created."
  [name]
  (some-> (re-matches #"clj-surgeon-suite-(\d+)-[0-9a-f]+" name) second parse-long))

(defn- pid-alive?
  "True when `pid` is a live process. Defaults to TRUE when the answer cannot
   be obtained: an unknown liveness must never authorise a delete."
  [pid]
  (try
    (let [handle (java.lang.ProcessHandle/of pid)]
      (and (.isPresent handle) (.isAlive (.get handle))))
    (catch Throwable _ true)))

;; @spec MCP-OP-TMPHYG-007
(defn sweep-stale-roots!
  "Deletes isolated roots left under `base` by runs that are gone: name shape
   ours, owning pid dead, and older than `stale-root-hours`. All three, so a
   concurrent run's root and another tenant's entries are never candidates.
   Returns the number swept."
  [base]
  (try
    (let [cutoff (- (System/currentTimeMillis) (* stale-root-hours 60 60 1000))]
      (count
        (filter
          (fn [^java.io.File f]
            (and (.isDirectory f)
                 (< (.lastModified f) cutoff)
                 (when-let [pid (root-owner-pid (.getName f))]
                   (and (not (pid-alive? pid)) (sweep-root! f)))))
          (or (seq (.listFiles (io/file (str base)))) []))))
    (catch Throwable _ 0)))

;; @spec MCP-OP-TMPHYG-007
(defn- register-root-sweep!
  "Sweeps `root` (and destroys `child`, when there is one) if this process is
   terminated in a way the VM can observe -- an external `timeout`'s SIGTERM,
   a Ctrl-C. Nothing can run after SIGKILL or an OOM."
  [root child-atom]
  (try
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread.
        ^Runnable
        (fn []
          (when-let [child @child-atom]
            (try
              (let [^Process p (:proc child)]
                (doseq [^java.lang.ProcessHandle d (reverse (vec (.toList (.descendants (.toHandle p)))))]
                  (.destroyForcibly d))
                (.destroyForcibly p)
                (.waitFor p 5 java.util.concurrent.TimeUnit/SECONDS))
              (catch Throwable _ nil)))
          (sweep-root! root))))
    (catch Throwable _ nil)))

;; @spec MCP-OP-TMPHYG-005
(defn- child-environment
  "The env the isolated child (and every descendant of it) is launched with.

   `-Djava.io.tmpdir` is a JVM-internal property no child PROCESS inherits,
   so a subprocess that picks its own temp location -- `mktemp -d`,
   `tempfile.mkdtemp` -- would write to the SHARED base, outside the isolated
   root and invisible to the leak witness. TMPDIR/TMP/TEMP put every
   descendant inside the run's own root."
  ([root] (child-environment root false))
  ([root isolate-home?]
   (cond-> {reexec-sentinel (str root)
            "TMPDIR" (str root)
            "TMP" (str root)
            "TEMP" (str root)
            ;; @spec MCP-OP-TMPHYG-005 -- test descendants must not leak npm's cache.
            "NODE_DISABLE_COMPILE_CACHE" "1"}
     ;; @spec TEST-ISO-006 -- a subprocess reads $HOME, never the JVM
     ;; property, so an isolated run that set only -Duser.home would leak
     ;; through the first thing that shells out.
     isolate-home? (assoc "HOME" (str (isolated-home root))))))

(defn secure-tmpdir!
  "Resolves the base temp directory (`env-or-current-tmpdir`) and REFUSES --
   printing a named `tmp-refused:` line to *err* and returning
   {:refused true}, on which the caller must exit 97 without running any
   test or re-exec'ing anything -- unless that base is positively proven
   usable real disk. It fails CLOSED: a RAM path by name (/tmp, /dev/shm), a
   tmpfs mount, an fstype no mount source can determine, an unusable base,
   and a re-exec sentinel this process does not own are all refusals
   (MCP-OP-TMPHYG-003, -004, -008).

   Otherwise, ensures a fresh, randomly-named isolated sub-directory of the
   base exists, then:
     - if this process has NOT already been re-exec'd (no sentinel env
       var): spawns a child process with java.io.tmpdir=<that isolated
       directory> passed as a literal startup flag (see namespace
       docstring for why this is the only mechanism that actually works),
       inherits stdio, waits for it, and calls (System/exit <child's exit
       code>) -- this function never returns in that branch.
     - if this process IS the re-exec'd child: returns
       {:refused false :root <the isolated directory, a java.io.File>}
       immediately, trusting the -D flag it was launched with (confirmed
       honored -- see docstring fact 2).

   `target` is {:bb-script <path, for bb> :main-ns <ns, for clojure -M>} --
   pass whichever the runtime needs; the other key is ignored. `args` is the
   runner's own argv, forwarded to the child."
  ([target] (secure-tmpdir! target nil))
  ([target args]
   (let [base (env-or-current-tmpdir)
         isolate-home? (boolean (:isolate-home? target))]
     (try (fs/create-dirs base) (catch Throwable _ nil))
     ;; @spec MCP-OP-TMPHYG-008
     (if-let [refusal (base-refusal base)]
       (refuse! refusal)
       (if-let [declared (System/getenv reexec-sentinel)]
         ;; CHILD branch. The sentinel is only believed when it NAMES the root
         ;; this process was actually launched on, and that root carries this
         ;; namespace's own name shape. Anything else is an inherited sentinel
         ;; in a process the parent never spawned -- refuse (MCP-OP-TMPHYG-004).
         (let [actual (System/getProperty "java.io.tmpdir")]
           (if (and (= (canonical declared) (canonical actual))
                    (own-isolated-root? actual))
             (let [root (io/file actual)]
               ;; @spec TEST-ISO-006 -- when this run asked for home isolation,
               ;; the child REFUSES unless it is actually running on the
               ;; throwaway. Without this the isolation could be silently
               ;; absent (a stripped flag, an inherited sentinel) and the lane
               ;; would run on the seat's real home while every witness that
               ;; asks the JVM what its home is agreed that it had not.
               (cond
                 ;; @spec MCP-OP-TMPHYG-005 -- never trust a stripped child environment.
                 (not= "1" (System/getenv "NODE_DISABLE_COMPILE_CACHE"))
                 (refuse! {:reason :node-compile-cache-not-disabled :base actual})

                 (and isolate-home?
                      (not= (canonical (isolated-home root))
                            (canonical (System/getProperty "user.home"))))
                 (refuse! {:reason :home-not-isolated
                           :base actual
                           :detail (format (str "user.home is %s but this run asked for "
                                                "an isolated home at %s.")
                                           (pr-str (System/getProperty "user.home"))
                                           (pr-str (str (isolated-home root))))})
                 :else
                 (do (register-root-sweep! root (atom nil))
                     {:refused false :root root})))
             (refuse! {:reason :sentinel-mismatch
                       :base actual
                       :detail (format (str "it names root=%s but this process's java.io.tmpdir "
                                            "is %s. Refusing to treat a shared base as a private "
                                            "run root.")
                                       (pr-str declared) (pr-str actual))})))
         (let [root (io/file base (format "%s%d-%s" isolated-root-prefix (current-pid)
                                          (subs (str (random-uuid)) 0 8)))]
           (sweep-stale-roots! base)
           (try
             (fs/create-dirs root)
             ;; @spec TEST-ISO-006 -- created by the same act that creates the
             ;; run root, so `sweep-root!` destroys both.
             (when isolate-home? (fs/create-dirs (isolated-home root)))
             (catch Throwable t
               (refuse! {:reason :unusable-base :base base
                         :detail (str (.getSimpleName (class t)) ": " (.getMessage t))})
               (System/exit 97)))
           (let [child (atom nil)
                 _ (register-root-sweep! root child)
                 cmd (reexec-child-command target root args)
                 proc (apply proc/process
                             {:inherit true
                              :extra-env (child-environment root isolate-home?)}
                             cmd)
                 _ (reset! child proc)
                 exit (:exit @proc)]
             (sweep-root! root)
             (System/exit exit))))))))

(defn tmp-entries
  "Names of the top-level entries currently under java.io.tmpdir."
  []
  (let [dir (io/file (System/getProperty "java.io.tmpdir"))]
    (set (some->> (.listFiles dir) seq (map #(.getName ^java.io.File %))))))

(defn leaked-entries
  "Entries present in `after` but absent from `before`."
  [before after]
  (set/difference after before))

(defn report-and-sweep-leak!
  "Given the isolated run root and its pre-run entry set (normally #{},
   since `secure-tmpdir!` just created it), lists what is left now. Prints
   a named, counted failure with (up to) the first 5 leaked names and
   returns 1 (add this to the suite's exit code) when anything survived;
   returns 0 when the run left the root empty. Either way, deletes the
   whole isolated root afterward -- reported leaks still get swept off
   real disk so a red run cannot itself become the next inode fire, and
   the exit code is the durable record that a test needs a real fix."
  [root before]
  (let [after (tmp-entries)
        leaked (sort (leaked-entries before after))
        result (if (empty? leaked)
                 0
                 (do
                   (binding [*out* *err*]
                     (println
                       (format "temp-leak: %d entries left under %s: %s%s"
                               (count leaked) root
                               (str/join ", " (take 5 leaked))
                               (if (> (count leaked) 5) ", ..." ""))))
                   1))]
    (sweep-root! root)
    result))

(defn track!
  "Records `dir` (any fs/delete-tree-able path -- a java.io.File, a Path, a
   string) onto `roots-atom` for `tracking-temp-dir-fixture` to sweep up
   after the current test, and returns `dir` unchanged so this composes
   inside a `let` that builds it. Use when a test's temp-dir helper is
   called many times across many deftests -- cheaper than threading a
   `finally` through every call site."
  [roots-atom dir]
  (swap! roots-atom conj dir)
  dir)

(defn tracking-temp-dir-fixture
  "A clojure.test `:each` fixture: resets `roots-atom` to [] before the
   test runs, then -- in a `finally`, so it runs on assertion failure or a
   thrown exception too -- deletes every directory that test recorded via
   `track!`. Pair with a private `(def temp-roots (atom []))` in the test
   namespace and `(use-fixtures :each (tracking-temp-dir-fixture temp-roots))`."
  [roots-atom]
  (fn [f]
    (reset! roots-atom [])
    (try
      (f)
      (finally
        (doseq [root @roots-atom]
          (try (fs/delete-tree root) (catch Throwable _ nil)))
        (reset! roots-atom [])))))

(defmacro with-temp-dir
  "Creates a fresh temp directory (respecting the current java.io.tmpdir)
   named with `prefix`, binds it as a java.io.File to `sym`, runs `body`,
   and recursively deletes it in a `finally` -- including when `body`
   throws or a `deftest` assertion fails. Prefer this over a bare
   `fs/create-temp-dir` / `Files/createTempDirectory` call in tests."
  [[sym prefix] & body]
  `(let [~sym (.toFile (fs/create-temp-dir {:prefix ~prefix}))]
     (try
       ~@body
       (finally
         (try (fs/delete-tree ~sym) (catch Throwable _# nil))))))
