;; Warnings-as-errors for the transaction kernel's two namespaces.
;;
;; @spec MCP-OP-MEM-020
;;
;; Reflection and boxed math are not style. A reflective call is a megamorphic
;; runtime lookup, and the two sites Sol found run PER STAGED FILE (workspace
;; confinement, twice each) and PER JOURNAL ARTIFACT (cleanup); the boxed
;; arithmetic runs once per admitted file and once per digest byte. Both are
;; invisible in a passing test suite, so they need a gate of their own.
;;
;; The dependencies are loaded first, WITHOUT the bindings, so this check gates
;; the kernel and never someone else's namespace.

(require 'clj-surgeon.txn-journal 'clj-surgeon.scope-stream)

(def gated
  '[clj-surgeon.txn-journal
    clj-surgeon.scope-stream])

(let [captured (java.io.StringWriter.)]
  (binding [*err* captured
            *warn-on-reflection* true
            *unchecked-math* :warn-on-boxed]
    (doseq [ns-sym gated]
      (require ns-sym :reload)))
  (let [lines (->> (clojure.string/split-lines (str captured))
                   (remove clojure.string/blank?)
                   distinct
                   vec)]
    (doseq [line lines]
      (println line))
    (println (format "kernel warning check: %d namespace(s), %d warning(s)"
                     (count gated) (count lines)))
    (when (seq lines)
      (println "REFUSED: the transaction kernel must compile with no reflection")
      (println "         and no boxed math. Add a type hint or a `long` coercion;")
      (println "         do not widen this gate."))
    (flush)
    (System/exit (if (seq lines) 1 0))))
