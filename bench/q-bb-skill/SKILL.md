---
name: clj-surgeon-q-bb
description: Compute one derived fact from Clojure source with a structural read piped to Babashka.
---

# Structural read plus Babashka

For a computed-read task, use one source-bearing pipeline:

```bash
clj-surgeon :op :q :file FILE :query 'QUERY' | bb -e '
  (require (quote [clojure.edn :as edn]))
  (let [result (edn/read *in*)
        selected (edn/read-string (get-in result [:matches 0 :source]))]
    (prn (COMPUTE selected)))'
```

Use `:q` to locate syntax and Babashka to compute from its emitted EDN. Keep the
pipeline in one shell call. Do not read the source separately or calculate from
visible source by eye. Refuse ambiguity instead of choosing a match. Never
modify the file. Return only the requested value.
