(ns clj-surgeon.census-discovery
  "The census discovery kernel: ONE bounded, root-confined walk for BOTH
   entrances.

   The `relation_census` MCP tool and the `:relation-census` CLI op used to
   walk the tree with two different implementations — `Files/walkFileTree`
   with a `SimpleFileVisitor` on the JVM, `fs/walk-file-tree` in the CLI so
   the op keeps running under babashka. Two implementations of one rule is
   two rules: the CLI's copy asked only what a directory entry was NAMED, so
   a link inside the workspace read bytes outside it and a two-link chain to
   one real file was censused three times, while the tool refused both.

   This namespace is the single kernel both entrances call. It uses only
   `java.io.File` listing plus `java.nio.file.Files` predicates, so it loads
   under babashka — which has no `SimpleFileVisitor` — and on the JVM, and
   neither entrance is allowed a discovery rule of its own.

   The rules it enforces, once:

   - the workspace ROOT is canonicalised before the walk begins, so a `:dir`
     or a project root that names a symlink walks the workspace it points at;
   - a symbolic link is never followed out of the canonical root: a path whose
     real location escapes is a typed, COUNTED skip, never a read and never a
     fatal refusal;
   - a skipped directory is pruned before it is read;
   - the walk STOPS at one candidate past `max-scanned-files` rather than
     enumerating the tree and truncating the result, and reaching the ceiling
     is reported as `:exceeded?` with nothing read;
   - a source above `max-source-bytes` is collected BY NAME instead of read,
     so the receipt can say what it did not look at."
  (:require
   [clj-surgeon.relation-census :as census])
  (:import
   (java.io File)
   (java.nio.file Files LinkOption Path Paths)))

(def ^:private follow-links
  "`toRealPath` resolves every link in the path; that is the point."
  (make-array LinkOption 0))

(def ^:private nofollow-links
  (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn- ^Path as-path
  [value]
  (Paths/get (str value) (make-array String 0)))

(defn canonical-root
  "The canonical real path of a workspace root, or nil when it does not
   resolve to an existing directory."
  ^Path [root]
  (try
    (let [real (.toRealPath (as-path root) follow-links)]
      (when (Files/isDirectory real follow-links) real))
    (catch Throwable _ nil)))

(defn- ^Path real-path-of
  [^File file]
  (try (.toRealPath (.toPath file) follow-links) (catch Throwable _ nil)))

(defn- symbolic-link?
  [^File file]
  (try (Files/isSymbolicLink (.toPath file)) (catch Throwable _ false)))

(defn- real-directory?
  "A DIRECTORY, not a link to one: a link is never descended."
  [^File file]
  (try (Files/isDirectory (.toPath file) nofollow-links) (catch Throwable _ false)))

(defn- candidate-name?
  [^File file]
  (boolean (re-find census/source-name-pattern (.getName file))))

(defn- inside-root?
  [^Path root ^Path candidate]
  (and (some? candidate) (.startsWith candidate root)))

;; @spec MCP-OP-CENSUS-018
;; @spec MCP-OP-CENSUS-027
;; @spec MCP-OP-CENSUS-028
;; @spec MCP-OP-CENSUS-032
(defn discover
  "Walk one workspace root and return what the census may read.

   Returns
   `{:root canonical-root-string
     :files [project-relative source paths, sorted]
     :oversized [project-relative paths above the byte cap, sorted]
     :skipped-outside-root n
     :exceeded? bool
     :observed n}`

   `:observed` is the candidate count the walk had seen when it stopped; when
   `:exceeded?` it is a LOWER BOUND, because the walk stops rather than
   enumerating the rest of the tree.

   A root that does not resolve to a directory yields an empty walk with
   `:unresolved-root? true`; deciding what to say about that belongs to the
   entrance, which knows whether it is answering a tool call or a CLI op."
  [root-arg]
  (if-let [^Path root (canonical-root root-arg)]
    (let [found (java.util.ArrayList.)
          oversized (java.util.ArrayList.)
          skipped (volatile! 0)
          exceeded (volatile! false)
          relative (fn [^Path path] (.toString (.relativize root path)))
          visit-file
          (fn [^File file]
            (let [link? (symbolic-link? file)
                  real (real-path-of file)
                  escapes? (not (inside-root? root real))]
              (cond
                ;; An escaping path of ANY name is counted, not read: the
                ;; `dev/checkouts/foo -> ../../foo` shape costs one skip
                ;; instead of refusing the whole census.
                escapes? (do (vswap! skipped inc) :continue)

                ;; A link that stays inside the root is left to the walk: the
                ;; file it names is discovered where it really lives.
                link? :continue

                (not (candidate-name? file)) :continue

                (> (Files/size real) census/max-source-bytes)
                (do (.add oversized (relative real)) :continue)

                (>= (.size found) census/max-scanned-files)
                (do (vreset! exceeded true) :terminate)

                :else (do (.add found (relative real)) :continue))))
          walk
          (fn walk [^File dir]
            (loop [entries (sort-by #(.getName ^File %)
                                    (or (seq (.listFiles dir)) []))]
              (if-let [^File entry (first entries)]
                (let [outcome
                      (if (real-directory? entry)
                        (if (contains? census/skipped-directories
                                       (.getName entry))
                          :continue
                          (walk entry))
                        (visit-file entry))]
                  (if (= :terminate outcome)
                    :terminate
                    (recur (rest entries))))
                :continue)))]
      (walk (.toFile root))
      {:root (.toString root)
       :files (vec (sort found))
       :oversized (vec (sort oversized))
       :skipped-outside-root @skipped
       :exceeded? @exceeded
       :observed (cond-> (.size found) @exceeded inc)})
    {:root nil
     :unresolved-root? true
     :files []
     :oversized []
     :skipped-outside-root 0
     :exceeded? false
     :observed 0}))
