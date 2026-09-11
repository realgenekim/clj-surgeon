# Parsed synthetic qualifier nodes lack source positions; support original-source spans across normalized newlines

Tested rewrite-clj 1.2.50 on Clojure 1.12.1 and babashka 1.12.209, and bundled rewrite-clj 1.2.55 on babashka 1.13.219. This is an issue draft; no issue has been filed and no maintainer response is assumed.

```clojure
(require '[rewrite-clj.parser :as p] '[rewrite-clj.node :as n])
(doseq [source ["#::ev{}" "#:é{}" "#?(:clj 1)" "#?@(:clj [1])"
                "\"é😀\" (def x 1)" "\"a\r\nb\" (def x 1)"]]
  (doseq [node (tree-seq n/inner? n/children (p/parse-string-all source))]
    (prn [(n/tag node) (n/string node) (meta node)])))
```

Observed: `#::ev{}` contains a `:map-qualifier` whose rendering is `::ev` and metadata is nil. `#:é{}` has the same coordinate gap. Synthetic `?`/`?@` token children of reader conditionals also have nil metadata. Ordinary nodes have exclusive row/column endpoints. Their columns count UTF-16 code units on these runtimes: in `"é😀" (def x 1)`, the list starts at row 1, column 7, UTF-16 index 6, UTF-8 byte 9. The list occupies bytes [9,18).

Separately, node rendering normalizes CRLF, including inside multiline strings: parsing `"a\r\nb"` yields node/string containing `"a\nb"`. Therefore rendering length cannot be used to infer original source intervals. LF, CRLF, bare CR and CRFF need an explicit coordinate convention when recovering original offsets.

Could synthetic prefix nodes first receive source metadata with exclusive coordinates, consistent with neighboring parsed nodes? For `#::ev{}`, the qualifier should occupy row 1 columns [2,6), original UTF-8 bytes [1,5). For `#:é{}`, columns [2,4), bytes [1,4). Conditional prefix tokens should be similarly locatable without implying platform branch semantics.

Would a separate opt-in original-source offset facility be appropriate, with documented UTF-16 column and newline-normalization behavior? Compatibility matters: existing node/string normalization and zipper tracked positions need not change. Parser-created metadata describes the original snapshot, whereas tracked zipper positions can follow edits. An offset API should distinguish those contracts and specify malformed Unicode handling.

I can offer small synthetic-coordinate and Unicode/newline regression tests and a narrowly scoped patch. The proposed scope excludes hashes, owner classification, form ordinals, receipt schemas, alias policies and transaction semantics; those belong to consumers. This is not a request to upstream a general application inventory or rename zipper z/splice.
