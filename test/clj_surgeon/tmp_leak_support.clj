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

(def ^:private reexec-sentinel "CLJ_SURGEON_TMPDIR_REEXEC")

(defn- mount-fstype
  "Best-effort filesystem type for `dir` via `findmnt`, falling back to a
   longest-prefix scan of /proc/mounts. Returns nil if neither source can
   answer (e.g. neither findmnt nor /proc/mounts exists on this platform)."
  [dir]
  (or
    (try
      (let [{:keys [exit out]} (shell/sh "findmnt" "-n" "-o" "FSTYPE" "--target" (str dir))]
        (when (and (zero? exit) (seq (str/trim out)))
          (str/trim out)))
      (catch Throwable _ nil))
    (try
      (when (.exists (io/file "/proc/mounts"))
        (let [target (.getCanonicalPath (io/file (str dir)))
              lines (str/split-lines (slurp "/proc/mounts"))
              best (->> lines
                        (keep (fn [line]
                                (let [[_dev mnt fstype] (str/split line #"\s+")]
                                  (when (and mnt (str/starts-with? target mnt))
                                    [mnt fstype]))))
                        (sort-by (comp count first) >)
                        first)]
          (second best)))
      (catch Throwable _ nil))))

(defn tmpfs?
  "True when `dir`'s filesystem is tmpfs (RAM-backed)."
  [dir]
  (= "tmpfs" (mount-fstype dir)))

(defn env-or-current-tmpdir
  "$TMPDIR when set (the seat's env, honored by a real `java` launch and by
   the re-exec'd bb child below), else whatever java.io.tmpdir already
   resolves to."
  []
  (or (System/getenv "TMPDIR") (System/getProperty "java.io.tmpdir")))

(defn- bb-runtime?
  []
  (some? (System/getProperty "babashka.version")))

(defn- reexec-child-command
  "The command vector for the isolated child process: `bb -D... <script>`
   under bb, or a nested `java -D... -cp <this process's classpath>
   clojure.main -m <main-ns>` under a real JVM (avoids re-running the
   slower `clojure` CLI / deps resolution; the classpath this process
   already resolved is exactly the one the child needs)."
  [{:keys [bb-script main-ns]} tmp-root]
  (if (bb-runtime?)
    ["bb" (str "-Djava.io.tmpdir=" tmp-root) bb-script]
    ["java" "-cp" (System/getProperty "java.class.path")
     (str "-Djava.io.tmpdir=" tmp-root)
     "clojure.main" "-m" main-ns]))

(defn secure-tmpdir!
  "Resolves the base temp directory (`env-or-current-tmpdir`). If it is
   tmpfs-backed, prints a named refusal to *err* and returns
   {:refused true} -- the caller must exit non-zero without running any
   tests or re-exec'ing anything.

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
   pass whichever the runtime needs; the other key is ignored."
  [target]
  (let [base (env-or-current-tmpdir)]
    (try (fs/create-dirs base) (catch Throwable _ nil))
    (if (tmpfs? base)
      (do
        (binding [*out* *err*]
          (println
            (format
              (str "tmp-refused: java.io.tmpdir base=%s is RAM-backed (tmpfs). "
                   "Launch with -Djava.io.tmpdir=/var/tmp/forge, or export "
                   "TMPDIR=/var/tmp/forge before invoking bb (bb does not "
                   "read JAVA_TOOL_OPTIONS -- see ~/bin/suite-run / "
                   "seat-tmp-guard.sh).")
              base)))
        {:refused true})
      (if (System/getenv reexec-sentinel)
        {:refused false :root (io/file (System/getProperty "java.io.tmpdir"))}
        (let [root (io/file base (str "clj-surgeon-suite-" (subs (str (random-uuid)) 0 8)))]
          (fs/create-dirs root)
          (let [cmd (reexec-child-command target root)
                {:keys [exit]} (apply proc/shell
                                       {:continue true
                                        :extra-env {reexec-sentinel "1"}}
                                       cmd)]
            (try (fs/delete-tree root) (catch Throwable _ nil))
            (System/exit exit)))))))

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
    (try (fs/delete-tree root) (catch Throwable _ nil))
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
