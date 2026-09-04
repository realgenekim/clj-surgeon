(ns clj-surgeon.mcp-paths
  "Shared project-root confinement for MCP source tools."
  (:require
   [clj-surgeon.relation-census :as census]
   [clojure.string :as str])
  (:import
   (java.nio.file Files LinkOption Path Paths)))

(def supported-source-extensions #{"clj" "cljs" "cljc" "edn"})

(defn relative-source-path?
  "True for a portable project-relative supported Clojure source path.

  This is the lexical half of confinement and performs no filesystem I/O."
  [value]
  (when (string? value)
    (let [portable (str/replace value "\\" "/")
          segments (str/split portable #"/" -1)
          extension (some-> portable (str/split #"\.") last)]
      (and (not (str/blank? portable))
           (not (str/starts-with? portable "/"))
           (not (re-find #"(?i)^[a-z]:/" portable))
           (not (str/includes? portable "\u0000"))
           (every? #(and (not (str/blank? %))
                         (not (#{"." ".."} %)))
                   segments)
           (contains? supported-source-extensions extension)))))

(defn real-root
  "Return the canonical configured project root. Performs filesystem I/O."
  ^Path [root]
  (.toRealPath
    (Paths/get (str root) (make-array String 0))
    (make-array LinkOption 0)))

(def source-refusal-causes
  "The ONE cause vocabulary both census entrances publish.

   Opus's round-sixteen item 2. The two entrances name their refusals from
   different sets BY DESIGN — MCP-OP-CENSUS-014 requires the CLI's
   `file-not-readable` to be its own name and the tool collapses every path
   refusal into `unreadable-source-path` so that a narrowing can remove the
   entry — so a witness that can only compare NAMES cannot see the two
   entrances disagreeing about WHICH FACT they observed. They did: `fs/exists?`
   follows links, so the CLI called a symlink loop and a name too long what
   they are, a path that does not resolve, while the tool called them
   unreadable and printed the exception that said so.

   The cause is carried BESIDE the name, never instead of it (MCP-OP-CENSUS-014
   again): it says what to fix, not which remedy applies."
  #{:not-found
    :parent-denied
    ;; A DIRECTORY the walk could not enter, as distinct from a directory
    ;; ABOVE a path the caller named: the subject differs, and so does what
    ;; the caller must fix.
    :directory-denied
    :permission-denied
    :not-a-regular-file
    :outside-project
    :not-a-relative-source-path
    :read-failed-after-fence
    :unresolvable-source-path})

(defn path-refusal
  ([error-type message path] (path-refusal error-type message path nil))
  ([error-type message path cause]
   (cond-> {:ok false
            :error_type (name error-type)
            :error message
            :path path
            :source_unchanged true
            :remedy "Use an existing project-relative source path inside the configured project root."}
     cause (assoc :cause (name cause)))))

(defn- exception-class-names
  "Every class name in `error`'s own hierarchy.

   Matched by NAME, and by the WHOLE hierarchy, for the two reasons this
   namespace already had one such test and needed a second. Babashka's runtime
   does not carry `java.nio.file.AccessDeniedException`, and a class literal in
   a `catch` or an `instance?` is resolved when this namespace is ANALYSED —
   the CLI entrance loads it. And a predicate that names exactly one class is
   one subclass and one sibling too narrow: round sixteen tested
   `AccessDeniedException` alone, and every other `FileSystemException` — a
   symlink loop, a name too long — walked past it into the generic catch and
   published the server's absolute root."
  [error]
  (into #{(.getName (class error))}
        (map #(.getName ^Class %))
        (supers (class error))))

(defn- filesystem-reason
  "The OS-level reason a `java.nio.file.FileSystemException` carries, or nil.

   `getMessage` on that class is `file + \": \" + reason` — the FILE is why the
   absolute root leaked. `getReason` is the half that is a fact about the
   failure rather than about the box, and it is read REFLECTIVELY so that no
   class literal enters this namespace's analysis.

   Published only when it names no path of its own: a reason is a `strerror`
   string today, and a refusal that leaks the root because the JDK changed its
   wording is the same defect arriving by a different door."
  [error]
  (try
    (let [method (.getMethod (class error) "getReason" (make-array Class 0))
          reason (.invoke method error (make-array Object 0))]
      (when (and (string? reason)
                 (not (str/blank? reason))
                 (not (str/includes? reason "/")))
        reason))
    (catch Throwable _ nil)))

(defn- unreadable-ancestor
  "The nearest existing ancestor DIRECTORY of `relative` that this process may
   neither read nor traverse, named PROJECT-RELATIVE, or nil.

   Named relative for the reason every other path this namespace publishes is:
   a refusal that leaks the server's absolute root tells the caller a fact
   about the box instead of a fact about their request."
  [^Path root relative]
  (try
    (loop [^Path dir (.getParent (.normalize (.resolve root (str relative))))]
      (when (and dir (.startsWith dir root))
        (cond
          (not (Files/exists dir (make-array LinkOption 0)))
          (recur (.getParent dir))

          ;; Opus's round-seventeen item 3, the identical missing test on this
          ;; side of the fence. It is unreachable today only because ENOTDIR
          ;; arrives here as `FileSystemException` and never as
          ;; `AccessDeniedException` — which makes it a defect waiting on a JDK
          ;; wording change rather than a defect that is not there. A regular
          ;; file in a path prefix is not an ancestor DIRECTORY this process may
          ;; not read; it is ENOTDIR, and the `not-found` branch already says so.
          (not (Files/isDirectory dir (make-array LinkOption 0)))
          nil

          (not (and (Files/isReadable dir) (Files/isExecutable dir)))
          ;; Opus's round-seventeen item 4. When the unreadable ancestor IS the
          ;; root, `relativize` yields `""` and this used to fall back to
          ;; `(.toString dir)` — the server's ABSOLUTE path, published from the
          ;; namespace whose own docstring forbids it. The root has a NAME now,
          ;; the one both entrances use.
          (census/shown-directory (.toString (.relativize root dir)))

          :else nil)))
    (catch Exception _ nil)))

(defn resolve-source-path
  "Resolve one lexically valid relative source path inside canonical root.

  Symlinks may point within the root, but escapes, non-regular files, and
  files this process may not READ refuse.

  Readability is asked HERE, beside regularity, and not in a `try` around the
  caller's `slurp`. Sol's round-fourteen item 7: a chmod-000 source passed
  every question this resolver used to ask — it exists, it is regular, its
  real path is under the root — so it resolved `:ok true` and the
  `java.io.FileNotFoundException (Permission denied)` escaped from `slurp`
  past every typed branch to the census's catch-all, which reported a
  permission bit as `census-adapter-failure` with a resource-exhaustion
  remedy. A path the process cannot read and a path that is not there are the
  SAME fact to every caller of this fn: a name the next call must not carry.
  Deciding that in one place is what makes them answer alike."
  [^Path root relative]
  (if-not (relative-source-path? relative)
    (path-refusal
      :invalid-relative-source-path
      "Expected a project-relative .clj, .cljs, .cljc, or .edn path without parent traversal"
      relative
      :not-a-relative-source-path)
    (try
      (let [lexical (.normalize (.resolve root relative))]
        (if-not (.startsWith lexical root)
          (path-refusal :path-outside-project
                        "Source path escapes the configured project root"
                        relative
                        :outside-project)
          (let [real (.toRealPath lexical (make-array LinkOption 0))]
            (cond
              (not (.startsWith real root))
              (path-refusal :path-outside-project
                            "Source symlink resolves outside the configured project root"
                            relative
                            :outside-project)

              (not (Files/isRegularFile real (make-array LinkOption 0)))
              (path-refusal :source-not-regular-file
                            "Source path is not a regular file"
                            relative
                            :not-a-regular-file)

              ;; Asked AFTER regularity so a directory is still reported as a
              ;; directory rather than as a permission problem, and BEFORE
              ;; `:ok true` so that nothing this fn admits can fail in the
              ;; caller's reader.
              (not (Files/isReadable real))
              (path-refusal :source-not-readable
                            "Source file exists but this process may not read it"
                            relative
                            :permission-denied)

              :else
              {:ok true
               :relative relative
               :path (.toString real)
               :canonical real}))))
      ;; Sol's round-fifteen item 9. A readable file under a `chmod 000`
      ;; PARENT cannot be resolved at all — `toRealPath` throws
      ;; `AccessDeniedException` on the way through the parent — and
      ;; `.getMessage` on that exception IS the path, so the generic catch
      ;; below published "/abs/src/app/locked/inner.clj" as the whole
      ;; explanation and the census printed it beside the relative path: the
      ;; path twice, once absolutely, and not one word about what may not be
      ;; read or why. The file's own bits are fine; a DIRECTORY above it is
      ;; what must change, so that is what the refusal names.
      (catch java.nio.file.NoSuchFileException _
        (path-refusal :source-file-not-found "Source file does not exist"
                      relative :not-found))
      (catch Exception error
        ;; Opus's round-sixteen item 2, blocking. The predicate here tested ONE
        ;; class name, and every other `java.nio.file.FileSystemException` fell
        ;; through to `(.getMessage error)` — which for that class is
        ;; `file + ": " + reason`, so a symlink loop published
        ;;
        ;;   "/abs/root/src/app/loopa.clj: Too many levels of symbolic links…"
        ;;
        ;; the server's absolute root, to the caller, from the one namespace
        ;; whose own docstring says a refusal must not do that. The whole
        ;; HIERARCHY is asked now, so a subclass cannot slip either.
        ;;
        ;; A `FileSystemException` that is not an access denial means the path
        ;; does not resolve to a file: a loop resolves to nothing, a name too
        ;; long names nothing. That is the SAME fact `fs/exists?` reports to
        ;; the CLI entrance about both shapes, so both entrances now publish
        ;; `not-found` for them instead of disagreeing about what they saw.
        (let [names (exception-class-names error)]
          (cond
            (contains? names "java.nio.file.AccessDeniedException")
            (path-refusal :source-not-readable
                          (str "Source file cannot be reached: the directory "
                               (or (unreadable-ancestor root relative)
                                   "containing it")
                               " may not be read by this process")
                          relative
                          :parent-denied)

            (contains? names "java.nio.file.FileSystemException")
            (path-refusal :source-file-not-found
                          (str "Source path does not resolve to a file"
                               (when-let [reason (filesystem-reason error)]
                                 (str ": " reason)))
                          relative
                          :not-found)

            :else
            ;; Not the filesystem answering, so there is no reason field to
            ;; publish and the exception's message is not one: it is written
            ;; by whatever threw, and this refusal has no way to know whether
            ;; it names a path. The class is the honest typed cause.
            (path-refusal :invalid-source-path
                          (str "Source path could not be resolved ("
                               (.getSimpleName (class error)) ")")
                          relative
                          :unresolvable-source-path)))))))

(defn resolve-new-source-path
  "Resolve one absent source path below a real project-confined ancestor.

  The destination itself must not exist. Missing parent directories are
  returned shallowest-first without being created, so the write transaction
  can create and roll them back under the same guards as the new file."
  [^Path root relative]
  (if-not (relative-source-path? relative)
    (path-refusal
      :invalid-relative-source-path
      "Expected a project-relative .clj, .cljs, .cljc, or .edn path without parent traversal"
      relative)
    (try
      (let [lexical (.normalize (.resolve root relative))
            parent (.getParent lexical)]
        (cond
          (not (.startsWith lexical root))
          (path-refusal :path-outside-project
                        "Target path escapes the configured project root"
                        relative)

          (Files/exists lexical (make-array LinkOption 0))
          (path-refusal :target-already-exists
                        "Extraction target already exists"
                        relative)

          (nil? parent)
          (path-refusal :invalid-target-path
                        "Extraction target has no parent"
                        relative)

          :else
          (let [[existing-parent missing-parents]
                (loop [candidate parent
                       missing ()]
                  (if (Files/exists candidate (make-array LinkOption 0))
                    [candidate (vec missing)]
                    (recur (.getParent candidate) (conj missing candidate))))
                real-parent (.toRealPath existing-parent
                                         (make-array LinkOption 0))]
            (cond
              (not (.startsWith real-parent root))
              (path-refusal :path-outside-project
                            "Target ancestor symlink resolves outside the configured project root"
                            relative)

              (not (Files/isDirectory real-parent (make-array LinkOption 0)))
              (path-refusal :target-parent-not-directory
                            "Extraction target ancestor is not a directory"
                            relative)

              :else
              (let [[canonical-parent canonical-missing]
                    (reduce
                      (fn [[^Path base paths] ^Path missing]
                        (let [path (.resolve base (.getFileName missing))]
                          [path (conj paths path)]))
                      [real-parent []]
                      missing-parents)
                    canonical (.resolve canonical-parent
                                        (.getFileName lexical))]
                {:ok true
                 :relative relative
                 :path (.toString canonical)
                 :canonical canonical
                 :missing-parent-directories canonical-missing})))))
      (catch Exception error
        (path-refusal :invalid-target-path (.getMessage error) relative)))))
