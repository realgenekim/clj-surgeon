(ns clj-surgeon.parallel
  "Bounded parallelism for read-side fan-out.

   Deliberately NOT required by `clj-surgeon.study` or `clj-surgeon.core`:
   claypoole ships Java classes that babashka cannot load, and the CLI runs
   under babashka (`bb.edn`, `:bbin/bin`). The kernel therefore takes a
   mapping strategy as an argument and defaults to serial `map`; this
   namespace supplies the JVM entrance's strategy. A strategy changes the
   order work is done in, never the answer."
  (:require
   [com.climate.claypoole :as cp]))

(def pool-size
  "Bounded worker count. Capped well below the host's core count because the
   MCP server shares its box with other work; `pmap`'s chunked, unbounded
   fan-out over a whole source tree is what this replaces."
  (max 1 (min 8 (cp/ncpus))))

(defn bounded-map
  "Eagerly map f over coll on a bounded claypoole pool that is always shut
   down, and return a fully realized vector.

   `upmap` yields in completion order, so f must return a value that carries
   its own identity (callers re-key the results)."
  [f coll]
  (if (empty? coll)
    []
    ;; clj-kondo does not expand claypoole's binding macro.
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (cp/with-shutdown! [pool (cp/threadpool pool-size)]
      (vec (cp/upmap pool f coll)))))
