# Working rules
Answer the task by reading this repository. Do not modify any file. Be fast: read only what the question needs. End with the answer in the requested format and nothing after it.

A Clojure outline tool is on PATH: `clj-surgeon :op :ls :file <path>` prints every top-level form of a file with its name, kind, arglist and line range (EDN). It is cheaper than reading a large file; after it, read only the line range you need.

A structural read is also on PATH. `clj-surgeon :op :xray :file <path> :expr "<clojure path>"` selects inside one named top-level form and returns exact source, or a computed EDN value, without printing the form. One working example, which returns every key of the `case` inside `transition` paired with its branch:
clj-surgeon :op :xray :file src/state.clj :expr "(-> (form 'transition) (match 'case) up outermost down right right (partition-all 2) (analyze #(mapv (fn [[k v]] [k v]) %)))"
Start with `(form 'NAME)`; `(match 'sym)` or `(match :kw)` finds a nested node; `right`/`up`/`down` move; `(analyze f)` runs a pure function over the vector of matches. Prefer it when the answer is a value inside a large form or an inventory over one form. If it refuses, fall back to ordinary reading.
