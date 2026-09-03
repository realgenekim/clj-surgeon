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
   [clj-surgeon.relation-census :as census]
   [clojure.string :as str])
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

(defn- child-relative
  [^String parent ^String name]
  (if (str/blank? parent) name (str parent "/" name)))

(defn- ancestor-chain
  "Every directory that contains this one, this one included, root first.
   The root is the empty string."
  [^String rel]
  (if (str/blank? rel)
    [""]
    (reduce (fn [chain segment]
              (conj chain (child-relative (peek chain) segment)))
            [""]
            (str/split rel #"/"))))

;; @spec MCP-OP-CENSUS-018
;; @spec MCP-OP-CENSUS-027
;; @spec MCP-OP-CENSUS-028
;; @spec MCP-OP-CENSUS-030
;; @spec MCP-OP-CENSUS-032
(defn discover
  "Walk one workspace root and return what the census may read.

   Returns
   `{:root canonical-root-string
     :files [project-relative source paths, sorted]
     :oversized [project-relative paths above the byte cap, sorted]
     :skipped-outside-root n
     :duplicates n
     :exceeded? bool
     :observed n
     :subtree-counts {project-relative directory -> candidates beneath it}
     :partial-dirs #{directories whose counts are lower bounds}}`

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
          seen (java.util.HashSet.)
          skipped (volatile! 0)
          duplicates (volatile! 0)
          exceeded (volatile! false)
          per-directory (volatile! {})
          terminated-in (volatile! nil)
          relative (fn [^Path path] (.toString (.relativize root path)))
          visit-file
          (fn [^File file ^String rel-dir]
            (let [^Path real (real-path-of file)
                  escapes? (not (inside-root? root real))]
              (cond
                ;; An escaping path of ANY name is counted, not read: the
                ;; `dev/checkouts/foo -> ../../foo` shape costs one skip
                ;; instead of refusing the whole census.
                escapes? (do (vswap! skipped inc) :continue)

                (not (candidate-name? file)) :continue

                ;; The path SET is the set of REAL paths. A chain of links
                ;; onto one source is one source, and the count of what
                ;; collapsed is published rather than left to be inferred
                ;; from a file total the caller cannot reconcile.
                (not (.add seen (.toString real)))
                (do (vswap! duplicates inc) :continue)

                (> (Files/size real) census/max-source-bytes)
                (do (.add oversized (relative real)) :continue)

                (>= (.size found) census/max-scanned-files)
                (do (vreset! exceeded true)
                    (vreset! terminated-in rel-dir)
                    :terminate)

                :else (do (.add found (relative real))
                          (vswap! per-directory update rel-dir (fnil inc 0))
                          :continue))))
          walk
          (fn walk [^File dir ^String rel-dir]
            (loop [entries (sort-by #(.getName ^File %)
                                    (or (seq (.listFiles dir)) []))]
              (if-let [^File entry (first entries)]
                (let [name (.getName entry)
                      outcome
                      (if (real-directory? entry)
                        (if (contains? census/skipped-directories name)
                          :continue
                          (walk entry (child-relative rel-dir name)))
                        (visit-file entry rel-dir))]
                  (if (= :terminate outcome)
                    :terminate
                    (recur (rest entries))))
                :continue)))]
      (walk (.toFile root) "")
      {:root (.toString root)
       :files (vec (sort found))
       :oversized (vec (sort oversized))
       :skipped-outside-root @skipped
       :duplicates @duplicates
       :exceeded? @exceeded
       :observed (cond-> (.size found) @exceeded inc)
       :subtree-counts (reduce (fn [totals [rel-dir n]]
                                 (reduce (fn [totals dir]
                                           (update totals dir (fnil + 0) n))
                                         totals
                                         (ancestor-chain rel-dir)))
                               {}
                               @per-directory)
       :partial-dirs (if @exceeded
                       (set (ancestor-chain @terminated-in))
                       #{})})
    {:root nil
     :unresolved-root? true
     :files []
     :oversized []
     :skipped-outside-root 0
     :duplicates 0
     :exceeded? false
     :observed 0
     :subtree-counts {}
     :partial-dirs #{}}))

;; @spec MCP-OP-CENSUS-027
(defn narrowing-subtree
  "The project-relative subtree a caller should retry, or nil.

   The walk STOPS at one candidate past the ceiling, so the only counts it
   knows exactly are the ones for directories it FINISHED: every ancestor of
   the file it stopped on holds a lower bound, and offering one of those as a
   narrowing would hand back a call that refuses again. Those are excluded,
   and of what remains the LARGEST subtree that fits under the ceiling wins,
   ties broken deepest first and then lexicographically — the deepest of two
   equal subtrees is the one that excludes the most, and the name is the
   tiebreak of last resort so the answer is stable across runs.

   nil means the walk learned nothing it can promise: the caller is told that,
   and told why, rather than handed a call that cannot work."
  [{:keys [subtree-counts partial-dirs]}]
  (->> subtree-counts
       (remove (fn [[dir n]]
                 (or (str/blank? dir)
                     (contains? partial-dirs dir)
                     (not (pos? n))
                     (> n census/max-scanned-files))))
       (sort-by (fn [[dir n]]
                  [(- n) (- (count (str/split dir #"/"))) dir]))
       ffirst))
