# Pure Clojure X-Ray

**Status:** Contract fixed; implementation and clean-context keep gate pending

## Outcome

A caller can select concrete Clojure syntax and compute one bounded read result
with pure Clojure in the same source invocation:

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'retry-policy) (match :delays) right (xray #(apply max (first %))))"
```

The command returns the computed `:value` together with the unchanged query,
cardinality trace, exact matches, structural addresses, and complete-file
source hash. It never writes source or a plan.

## Evidence Boundary

The earlier native-edit experiment rejected Clojure-shaped syntax as a better
way to spell a static query. Literal EDN remained shorter and produced fewer
hallucinations. Pure Clojure earned its place only when `transform` removed a
source read by computing a replacement from the selected form.

Apply the same threshold here. `:q` remains the default for literal structural
reads. `:xray` survives only if computation over selected forms removes a
separate shell command or manual reconstruction in clean-context tasks.

## Public Contract

`:xray` requires `:file` and `:expr`. It accepts no `:query` spelling. The
expression must contain exactly one form and must return this terminal builder:

```clojure
(xray path pure-function)
```

Thread-first composition is ordinary Clojure:

```clojure
(-> (form 'classify-request)
    (match 'cond)
    up
    outermost
    down
    right
    (partition-all 2)
    (xray #(mapv first %)))
```

The path uses the existing read-only lens grammar. Replacement and transform
terminals are invalid. The function receives one vector in match order:

- a node match becomes its Clojure value;
- a span or partition match becomes a vector of its Clojure values;
- zero matches invoke the function with `[]`;
- one match still arrives inside a one-element vector;
- many matches remain in stable query order.

The result is:

```clojure
{:operation :xray
 :expression "..."
 :query [...]
 :trace [...]
 :match-count N
 :matches [...]
 :source-hash "..."
 :xray {:input-shape :selected-values
         :input-count N}
 :value EDN}
```

The function result must be concrete EDN data. Lazy sequences, functions,
objects, and host values refuse as `:invalid-xray-result`. A printed result over
65,536 characters refuses as `:xray-result-too-large`. If the lens evidence is
truncated by its existing 100-result bound, `:xray` refuses as
`:xray-input-truncated` instead of computing from an incomplete vector.

## Refusals

All refusals are EDN, exit nonzero at the CLI, and expose allowed symbols,
builder signatures, and a direct remedy when expression authoring failed.

| Condition | `:error-type` | Source read? |
|---|---|---:|
| missing `:expr` | `:missing-arguments` | no |
| multiple expressions | `:invalid-xray-expression` | no |
| unsafe or unknown symbol | `:invalid-xray-expression` | no |
| expression does not return `xray` | `:invalid-xray-expression` | no |
| invalid/terminal path | `:invalid-xray-expression`, reason `:invalid-xray-path` | no |
| lens parse or navigation failure | existing lens error | yes |
| more than 100 selected matches | `:xray-input-truncated` | yes |
| selected source cannot become data | `:xray-input-invalid` | yes |
| pure function throws | `:xray-analysis-failed` | yes |
| non-EDN result | `:invalid-xray-result` | yes |
| result exceeds bound | `:xray-result-too-large` | yes |
| unknown argument | `:unsupported-arguments` | no |

## Safety Invariants

- SCI uses the existing pure capability allowlist. It exposes no I/O,
  processes, namespaces, mutable references, classes, or host interop.
- Expression validation occurs before source I/O. Project-alias initialization
  can inspect the nearest `.clj-surgeon.edn` first.
- The returned EDN never contains the analyzer function.
- Original match evidence remains available beside `:value`.
- A function cannot change match source, addresses, trace, or source hash.
- `:xray` creates no plan and writes no file.
- The existing query result bound remains authoritative.
- `:q` behavior and `:edit` compilation remain byte-for-byte compatible.

## Non-Goals

- Do not rename `:q` or make Clojure syntax mandatory for reads.
- Do not add a second query evaluator.
- Do not expose rewrite-clj nodes or zippers to SCI.
- Do not add file, process, namespace, class, interop, or mutable capabilities.
- Do not infer macro semantics or discard provenance.
- Do not support executable saved analyses or source mutation.
- Do not claim termination; the clean harness retains its process timeout.

## Pure Behavior Matrix

1. builder construction and thread-first composition;
2. zero, singleton, and many node matches;
3. span and partition inputs;
4. stable input order and duplicate-address behavior inherited from `:q`;
5. metadata, comments, strings, maps, sets, symbols, and nested collections;
6. `:outermost` composition on the real nested-`cond` fixture;
7. analyzer results for every concrete EDN collection and scalar family;
8. analyzer exception, lazy sequence, function, object, and oversized result;
9. truncated selection refusal;
10. invalid path, replacement terminal, missing function, and non-function;
11. multiple expressions, non-terminal expressions, and unsafe symbols;
12. exact evidence preservation and absence of executable values.

## Boundary and Documentation Gates

1. Add subprocess tests for the documented command, EDN stdout, zero exit, and
   unchanged source bytes.
2. Prove unsafe expressions and unknown arguments refuse before source I/O.
3. Add global help, `:xray --help`, README, canonical and legacy skills,
   repository instructions, vision, and changelog together.
4. Keep the canonical installed skill at or below 240 lines.
5. Format changed Clojure files and run clj-kondo.
6. Run focused tests and the full suite with a larger assertion count.
7. Run `make install` and execute the documented command with the installed CLI.
8. Give clean agents computed-read tasks where `:q` alone would require a
   second shell computation. Record voluntary use, correctness, source calls,
   tokens, bytes, and time.

## Keep Gate

Keep `:xray` only if the normal installed skill produces correct voluntary use
on at least three of four clean runs and removes one downstream shell command
without increasing source reads. Otherwise retain the negative evidence and
remove the operation. A pleasant name or symmetric API is not sufficient.
