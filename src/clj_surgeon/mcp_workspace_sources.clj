(ns clj-surgeon.mcp-workspace-sources
  "One deterministic source universe shared by extraction planning and apply."
  (:require
   [clj-surgeon.extract :as extract])
  (:import
   (java.nio.file Path Paths)))

;; @spec MCP-OP-PLAN-001
;; @spec MCP-OP-EXTRACT-037
(defn read-all
  "Return canonical source paths and exact bytes for one confined workspace,
  or the discovery kernel's own typed refusal.

  The walk is NOT this namespace's. It was, and that was the defect: a
  `file-seq` (which follows directory symlinks) filtered by the LEXICAL test
  `(str/includes? path \"/.git/\")` and then keyed by `getCanonicalPath`, so
  `src/app/alias_caller.clj -> ../../.git/hooks/caller.clj` passed the filter
  under its link spelling and entered the universe under its canonical `.git`
  name -- where the extraction apply wrote it. Meanwhile `:extract!` walked
  with NOFOLLOW, pruned the root build trees, and refused exactly that shape.
  One rule with two implementations is one rule and one hole, so discovery now
  has a single kernel and this namespace calls it.

  Returns `{:ok true :sources <sorted-map> :paths [...] :discovery {...}}` or
  one typed refusal. The shape is a MAP WITH `:ok`, not a bare source map,
  because a refusal returned as an empty universe is a refusal nobody reads."
  [^Path root]
  (let [found (extract/discover-workspace-sources (.toFile root))]
    (if-not (:ok found)
      found
      (assoc found
             :sources (into (sorted-map)
                            (map (fn [path] [path (slurp path)]))
                            (:paths found))))))

(defn relative-paths
  "Map each canonical source path to its project-relative spelling."
  [^Path root sources]
  (let [canonical-root (.toRealPath root (make-array java.nio.file.LinkOption 0))]
    (into {}
          (map (fn [source-path]
                 [source-path
                  (-> canonical-root
                      (.relativize (Paths/get source-path (make-array String 0)))
                      str)]))
          (keys sources))))
