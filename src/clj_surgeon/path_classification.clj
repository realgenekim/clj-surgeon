(ns clj-surgeon.path-classification
  "The production fail-closed filesystem classifier: is a path proven real
   disk, or must it be refused?

   RELOCATED (2026-09-10, SPF-004): this logic used to live only in
   clj-surgeon.tmp-leak-support (test/), which the bb CLI's own classpath
   does not reach from its production entrances -- receipt_artifacts.clj
   requiring it directly there broke `bb -m clj-surgeon.core` outright
   (every verb, not only receipt writes). The fence's finding was exact: a
   SELF-CONTAINED duplicate in receipt_artifacts.clj (`ram-backed-path?`)
   was not equivalent to this namespace's `base-refusal` -- it had no
   Darwin fallback and, critically, treated `findmnt` being UNAVAILABLE as
   proof of real disk rather than as `:unknown` (a refusal). This namespace
   is the ONE authoritative implementation, living where every production
   entrance (bb CLI included) can require it; `clj-surgeon.tmp-leak-support`
   now delegates to it so the test runners' own gate behaviour is
   unchanged.

   `mount-fstype` is a TRI-STATE: the fstype string when a mount source
   could answer, or `:unknown` when none could -- `:unknown` is a REFUSAL
   (see `base-refusal`), never a pass. `findmnt` (Linux, util-linux),
   `mount(8)` by absolute path (Darwin, PATH cannot redirect it), and the
   mounts table (`/proc/mounts` or a witness seam) are each tried in turn;
   a seam-sourced fstype is never positive proof of real disk, only ever a
   refusal, so an operator handing this a lying table can make it refuse
   but never make it lie back that RAM is disk."
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]))

(def ^:private ram-path-prefixes
  "Paths that are RAM-backed on this seat BY NAME -- checked with no external
   binary and no procfs. This is the check that refuses when no mount source
   can answer at all."
  ["/tmp" "/dev/shm"])

(defn canonical
  [dir]
  (try (.getCanonicalPath (io/file (str dir)))
       (catch Throwable _ (str dir))))

(defn nearest-existing-ancestor
  "Walk up from `path` to the first ancestor that exists, or nil if none does
   (should not happen once the filesystem root is reached). Classification
   must run on an EXISTING path -- `findmnt`/`mount(8)`/the mounts table all
   answer for a mount point, not a not-yet-created leaf -- so every writer
   that wants to classify a not-yet-created target classifies its nearest
   existing ancestor instead, never the target itself."
  [path]
  (loop [f (io/file (str path))]
    (cond
      (nil? f) nil
      (.exists f) f
      :else (recur (.getParentFile f)))))

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

(defn darwin?
  []
  (str/includes? (str/lower-case (or (System/getProperty "os.name") "")) "mac"))

(def ^:private darwin-mount-row
  "`DEVICE on MOUNTPOINT (fstype, flag, flag)`, anchored at BOTH ends.

   The mount point is whatever lies between the device token and the trailing
   parenthesised group -- it is never found by searching for a separator, because
   a mount point may legally CONTAIN the separator. Exactly ONE byte is removed,
   the single space delimiting the option group; a mount point that legally ends
   in a space keeps it. The trailing group is the
   last `(...)` on the line and may not itself contain parentheses, so a mount
   point that contains them (`/Volumes/My (Disk)`) still parses.

   The device is one token, except the automounter's `map <name>` form."
  #"^(?:map \S+|\S+) on (.*) \(([^()]+)\)$")

(defn parse-darwin-mount-table
  "The fstype covering `target`: an fstype string, nil when no row covers it, or
   `:unknown` when the text is not a mount table this parser fully understands.

   THE SAFETY PROPERTY, and the reason this is not a `keep`. Sol
   SKIFF-INSTALL-FENCE-001: an earlier version split each line on the LAST
   literal ` on `, so a mount point containing those bytes --

       /dev/ram on /private/var/folders/evil on ram (tmpfs, local)

   -- was discarded as malformed, and the surviving `/` row then answered
   \"apfs\" for a target on that tmpfs. Crafted mount text could make the ratchet
   PROVE real disk. Discarding a row you do not understand is the bug: the row
   you cannot read is exactly the row that may be covering your target.

   So ANY non-blank line that does not match the anchored grammar poisons the
   WHOLE table to `:unknown`, and longest-prefix selection runs only over a table
   that parsed completely. Refuse, or answer; never prove past an ambiguity.

   PURE and public, so the property is witnessed from a Linux box."
  [text target]
  (let [lines (remove str/blank? (str/split-lines (or text "")))
        rows (reduce (fn [acc line]
                       (if-let [[_ mnt types] (re-matches darwin-mount-row line)]
                         (let [fstype (str/trim (first (str/split types #",")))]
                           ;; `mnt` is stored BYTE-FOR-BYTE. Sol
                           ;; SKIFF-INSTALL-FENCE-001 round 2: this used to be
                           ;; `(str/trim mnt)`, which silently contradicted the
                           ;; exact-bytes contract two lines above it. A trailing
                           ;; space is a legal Unix path byte, so trimming turned
                           ;; the covering row `/Volumes/ram ` into
                           ;; `/Volumes/ram`, it stopped covering the real target,
                           ;; and the `/` row proved apfs for a tmpfs path. The
                           ;; grammar already removes the one delimiter space
                           ;; before the option group and nothing else; any
                           ;; further normalisation here is a second, invisible
                           ;; parser disagreeing with the first.
                           (if (and (not (str/blank? mnt)) (seq fstype))
                             (conj acc [mnt fstype])
                             (reduced :unknown)))
                         (reduced :unknown)))
                     [] lines)]
    (cond
      (= :unknown rows) :unknown
      (empty? rows) nil
      :else (->> rows
                 (filter (fn [[mnt _]]
                           (or (= target mnt)
                               (= mnt "/")
                               (str/starts-with? target (str mnt "/")))))
                 (sort-by (comp count first) >)
                 first
                 second))))

(def ^:private darwin-mount-binaries
  "`mount(8)` by ABSOLUTE path, never by bare name.

   Sol SKIFF-INSTALL-FENCE-001, second half: the earlier version shelled out to
   `mount` through PATH, and the claim in its docstring that this was a
   non-redirectable system source was simply FALSE -- a `mount` earlier on PATH
   printing `/dev/fake on / (apfs, local)` made base-refusal return nil. An
   absolute path cannot be redirected by the environment; writing to /sbin needs
   privileges that already defeat every check in this namespace."
  ["/sbin/mount" "/bin/mount"])

(defn- darwin-mount-fstype
  "Longest-mount-point-prefix scan of `mount(8)`, the darwin equivalent of
   findmnt.

   The binary is named by ABSOLUTE path, so PATH cannot redirect it, and a
   table this parser cannot fully read answers `:unknown` rather than
   falling back to a covering row."
  [dir]
  (when (darwin?)
    (try
      (let [binary (first (filter #(.canExecute (io/file %)) darwin-mount-binaries))]
        (when binary
          (let [{:keys [exit out]} (shell/sh binary)]
            (when (zero? exit)
              (parse-darwin-mount-table out (canonical dir))))))
      (catch Throwable _ nil))))

(defn- mounts-table-fstype
  "Longest-mount-point-prefix scan of the mounts table.

   procfs reports st_size = 0, so `slurp`, `(.readAllBytes (io/input-stream
   ...))` AND `(line-seq (io/reader ...))` all throw `java.io.IOException:
   Invalid argument` on /proc/mounts -- on bb AND on a real JVM.
   `java.nio.file.Files/lines` is the one approach that reads it on both
   runtimes, so it is the one used here."
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

   An undeterminable filesystem must never be treated as proven-safe.
   `:unknown` is a refusal (see `base-refusal`), not a pass -- and `dir`
   must exist for a mount source to answer at all; callers that want to
   classify a not-yet-created path must resolve `nearest-existing-ancestor`
   first."
  [dir]
  (or (findmnt-fstype dir)
      ;; darwin's own authority, for the same reason findmnt is Linux's.
      (darwin-mount-fstype dir)
      ;; A seam-sourced fstype is NEVER positive proof of real disk. The gate
      ;; only ever needs the seam to produce a REFUSAL, so a forged table can
      ;; refuse (tmpfs) but a non-tmpfs answer from it reads as `nothing could
      ;; answer`. Without this, an operator handing the check a lying table
      ;; converts `I cannot prove this is disk` into `proven disk`.
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

(defn suggested-tmp-base
  "A real-disk scratch base to SUGGEST in a refusal, never a literal naming
   one machine unconditionally."
  []
  (let [candidates (cond-> ["/var/tmp"] (darwin?) (conj "/private/var/tmp"))]
    (or (first (filter #(.isDirectory (io/file %)) candidates)) "/var/tmp")))

(defn refusal-remedy
  []
  (let [base (suggested-tmp-base)]
    (str "Launch with -Djava.io.tmpdir=" base "/clj-surgeon, or export TMPDIR="
         base "/clj-surgeon before invoking bb (bb does not read "
         "JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already "
         "sets is real disk and needs no override -- an EMPTY TMPDIR is the "
         "usual cause of this refusal, not a wrong one.")))

;; @spec MCP-OP-TMPHYG-003
(defn base-refusal
  "nil when `dir` is PROVEN to be a real-disk path; otherwise a typed refusal
   map {:reason :ram-path-prefix|:tmpfs|:unknown-fstype :base ... :fstype ...}.

   Fails CLOSED: every path out of this function that is not a positive proof
   of real disk is a refusal. `dir` should be an EXISTING path (see
   `nearest-existing-ancestor`) -- classifying a not-yet-created leaf answers
   `:unknown-fstype`, which is also a refusal, so this never fails open on a
   missing target either way."
  [dir]
  (if (literal-ram-path? dir)
    {:reason :ram-path-prefix :base (str dir)}
    (let [fstype (mount-fstype dir)]
      (cond
        (= :unknown fstype) {:reason :unknown-fstype :base (str dir)}
        (= "tmpfs" fstype) {:reason :tmpfs :base (str dir) :fstype fstype}
        :else nil))))

(defn refusal-message
  "Formats a `base-refusal` map. Scoped to the three reasons `base-refusal`
   itself can produce; `clj-surgeon.tmp-leak-support/refusal-message` handles
   a broader set of reasons for the test-runner hygiene ratchet and delegates
   the three below to this function so the wording stays in one place."
  [{:keys [reason base fstype]}]
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
            "is not usable as a temp base.")
          (refusal-remedy)))
