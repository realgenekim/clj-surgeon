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
   - the walk also stops at one ENTRY past `max-walk-entries`, counting every
     entry it visits of any name, because the candidate ceiling bounds what
     the census READS and not what the walk COSTS, and each directory is
     STREAMED so the bound is charged as a name is yielded rather than after
     the complete listing has been materialised and sorted;
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
;; @spec MCP-OP-CENSUS-033
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
     :walk-exceeded? bool
     :entries-observed n
     :entries-yielded n
     :subtree-counts {project-relative directory -> candidates beneath it}
     :subtree-entries {project-relative directory -> entries beneath it}
     :partial-dirs #{directories whose counts are lower bounds}}`

   `:observed` is the candidate count the walk had seen when it stopped; when
   `:exceeded?` it is a LOWER BOUND, because the walk stops rather than
   enumerating the rest of the tree. `:entries-observed` is the same shape for
   the ENTRY bound: every entry the walk visited, of any name, and a lower
   bound once `:walk-exceeded?`. `:entries-yielded` is how many names the walk
   actually OBTAINED from the filesystem, which is at most `:entries-observed`
   — the evidence that the walk streamed the listings instead of materialising
   them and charging the bound afterwards.

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
          walk-exceeded (volatile! false)
          visited (volatile! 0)
          yielded (volatile! 0)
          per-directory (volatile! {})
          per-directory-entries (volatile! {})
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
          admit
          (fn [^File dir ^String rel-dir]
            ;; The directory is STREAMED, never materialised. `.list` asked
            ;; the filesystem for the COMPLETE name array and `sort` realised
            ;; every element of it BEFORE the counter charged the first entry:
            ;; a directory of any size was fully enumerated on the way to
            ;; refusing it, which is the one thing the entry bound exists to
            ;; prevent. `Files/newDirectoryStream` yields one name at a time,
            ;; the bound is charged as each name is yielded, and the walk stops
            ;; at one entry past the bound without asking the directory for the
            ;; rest of it.
            ;;
            ;; Only the ADMITTED names are sorted. A walk that stays under the
            ;; bound therefore visits the tree in exactly the sorted order it
            ;; always did; a walk that does not is stopping, and the order of
            ;; what it never looked at is not a promise anyone can use.
            ;;
            ;; Returns `[outcome sorted-admitted-names]`.
            (let [admitted (java.util.ArrayList.)
                  outcome
                  (try
                    (let [stream (Files/newDirectoryStream (.toPath dir))]
                      (try
                        (let [it (.iterator ^Iterable stream)]
                          (loop []
                            (if (.hasNext ^java.util.Iterator it)
                              ;; Charged BEFORE the name is pulled: the
                              ;; iterator has already told us an entry exists,
                              ;; and the bound is about what the walk may
                              ;; touch, not about what it managed to read.
                              (if (> (vswap! visited inc) census/max-walk-entries)
                                :terminate
                                (let [^Path entry (.next ^java.util.Iterator it)]
                                  (vswap! yielded inc)
                                  (.add admitted
                                        (.toString (.getFileName entry)))
                                  (vswap! per-directory-entries
                                          update rel-dir (fnil inc 0))
                                  (recur)))
                              :continue)))
                        (finally (.close ^java.io.Closeable stream))))
                    ;; A directory that cannot be opened or read contributes
                    ;; nothing; it is not a fatal refusal, exactly as an
                    ;; unlistable directory was not one before.
                    (catch java.io.IOException _ :continue))]
              [outcome (sort admitted)]))
          walk
          (fn walk [^File dir ^String rel-dir]
            (let [[outcome names] (admit dir rel-dir)]
              (if (= :terminate outcome)
                (do (vreset! walk-exceeded true)
                    (vreset! terminated-in rel-dir)
                    :terminate)
                (loop [names names]
                  (if-let [^String name (first names)]
                    (let [^File entry (File. dir name)
                          outcome (if (real-directory? entry)
                                    (if (contains? census/skipped-directories name)
                                      :continue
                                      (walk entry (child-relative rel-dir name)))
                                    (visit-file entry rel-dir))]
                      (if (= :terminate outcome)
                        :terminate
                        (recur (rest names))))
                    :continue)))))
          subtree-totals
          (fn [per-dir]
            (reduce (fn [totals [rel-dir n]]
                      (reduce (fn [totals dir]
                                (update totals dir (fnil + 0) n))
                              totals
                              (ancestor-chain rel-dir)))
                    {}
                    per-dir))]
      (walk (.toFile root) "")
      {:root (.toString root)
       :files (vec (sort found))
       :oversized (vec (sort oversized))
       :skipped-outside-root @skipped
       :duplicates @duplicates
       :exceeded? @exceeded
       :observed (cond-> (.size found) @exceeded inc)
       :walk-exceeded? @walk-exceeded
       :entries-observed @visited
       :entries-yielded @yielded
       :subtree-counts (subtree-totals @per-directory)
       :subtree-entries (subtree-totals @per-directory-entries)
       :partial-dirs (if (or @exceeded @walk-exceeded)
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
     :walk-exceeded? false
     :entries-observed 0
     :entries-yielded 0
     :subtree-counts {}
     :subtree-entries {}
     :partial-dirs #{}}))

(defn- largest-fitting-subtree
  "The largest fully-walked subtree that fits and has something to census.

   The walk STOPS one past whichever bound it hit, so the only counts it knows
   exactly are the ones for directories it FINISHED: every ancestor of the
   entry it stopped on holds a lower bound, and offering one of those as a
   narrowing would hand back a call that refuses again. Those are excluded.

   A subtree holding NO candidate Clojure source is excluded too, whatever its
   size. The entry bound fires precisely on trees of junk, so the largest
   subtree the walk finished is very often junk — and a continuation onto junk
   is not a smaller census, it is `no-fold-arms-found` on a workspace that has
   arms. `subtree-counts` is the candidate count for every directory, and the
   rule reads it whether or not it is the measure being ranked.

   Of what remains the LARGEST subtree by `measure` wins, ties broken by the
   MOST candidate sources, then deepest, then lexicographically — the richest
   of two equal subtrees is the one the caller most wants, the deepest of two
   equal subtrees is the one that excludes the most, and the name is the
   tiebreak of last resort so the answer is stable across runs.

   nil means the walk learned nothing it can promise: the caller is told that,
   and told why, rather than handed a call that cannot work."
  [measure subtree-counts partial-dirs fits?]
  (->> measure
       (keep (fn [[dir n]]
               (let [candidates (get subtree-counts dir 0)]
                 (when (and (not (str/blank? dir))
                            (not (contains? partial-dirs dir))
                            (pos? n)
                            (pos? candidates)
                            (fits? dir n))
                   [dir n candidates]))))
       (sort-by (fn [[dir n candidates]]
                  [(- n) (- candidates)
                   (- (count (str/split dir #"/"))) dir]))
       ffirst))

;; @spec MCP-OP-CENSUS-027
(defn narrowing-subtree
  "The subtree a caller should retry after the CANDIDATE ceiling, or nil."
  [{:keys [subtree-counts partial-dirs]}]
  (largest-fitting-subtree subtree-counts
                           subtree-counts
                           partial-dirs
                           (fn [_ n] (<= n census/max-scanned-files))))

;; @spec MCP-OP-CENSUS-033
(defn entry-narrowing-subtree
  "The subtree a caller should retry after the ENTRY bound, or nil.

   Ranked by the entries beneath it rather than the candidates, because
   entries are what was exhausted — but a subtree is only offered when BOTH
   bounds hold for it AND it holds at least one candidate source: a narrowing
   that replays into the candidate ceiling, or onto a subtree with nothing to
   census, is a call that cannot work, which is the one thing this must never
   hand back."
  [{:keys [subtree-entries subtree-counts partial-dirs]}]
  (largest-fitting-subtree
    subtree-entries
    subtree-counts
    partial-dirs
    (fn [dir n]
      (and (<= n census/max-walk-entries)
           (<= (get subtree-counts dir 0) census/max-scanned-files)))))
