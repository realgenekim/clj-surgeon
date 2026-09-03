(ns clj-surgeon.census-pool
  "Eager, bounded parallel plan phase for relation_census.

   This namespace is the only place claypoole is used. It changes elapsed time
   and never the answer: `clj-surgeon.relation-census/merge-results` re-keys by
   path, so an unordered pool cannot reorder the census."
  (:require
   [clj-surgeon.relation-census :as census]
   [com.climate.claypoole :as cp]))

(defn default-pool-size
  "The pool used when the caller asks for none: the box's processor count."
  []
  (census/effective-pool-size census/max-pool-size))

;; @spec MCP-OP-CENSUS-010
;; @spec MCP-OP-CENSUS-020
(defn pooled-map
  "Return a map-fn that runs `f` over the inputs on a shutdown-bound pool.

   `cp/upmap` is eager and unordered; results are realized inside
   `cp/with-shutdown!` so no thread outlives the call."
  [size]
  (fn [f xs]
    ;; clj-kondo does not expand claypoole's binding macro.
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (cp/with-shutdown! [pool (cp/threadpool size)]
      (vec (cp/upmap pool f xs)))))
