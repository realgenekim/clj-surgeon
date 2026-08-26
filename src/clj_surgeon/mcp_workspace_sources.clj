(ns clj-surgeon.mcp-workspace-sources
  "One deterministic source universe shared by extraction planning and apply."
  (:require
   [clojure.string :as str])
  (:import
   (java.nio.file Path Paths)))

(def source-file-pattern #".*\.clj[sc]?$")

;; @spec MCP-OP-PLAN-001
(defn read-all
  "Return canonical source paths and exact bytes for one confined workspace."
  [^Path root]
  (->> (file-seq (.toFile root))
       (filter #(.isFile %))
       (filter #(re-matches source-file-pattern (.getName %)))
       (remove #(str/includes? (.getPath %) "/.git/"))
       (map (fn [file] [(.getCanonicalPath file) (slurp file)]))
       (into (sorted-map))))

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
