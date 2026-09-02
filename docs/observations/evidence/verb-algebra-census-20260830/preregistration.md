# Transform-verb expressibility census — preregistration

Frozen before inspecting or classifying any corpus `from`/`to` payload. The earlier
write-side study was read only for its published window, population, and privacy contract.

## Question and population

Measure every syntactically valid Clojure `from`/`to` replacement pair carried by the 195
`apply_clojure_changes` calls in the exact window used by
`2026-08-29-write-side-emission-and-read-side-encoding-study.md`:

- UTC lower bound: `2026-08-22T00:00:00Z`, inclusive;
- UTC upper bound: `2026-08-30T02:09:33.141926Z`, inclusive;
- local clj-surgeon MCP service telemetry only;
- no CLI actions, no client clocks, no model calls;
- exact request deduplication and inclusion rules must reproduce 1,437 total service calls,
  1,242 `inspect_clojure`, and 195 `apply_clojure_changes` before classification runs.

Pairs come from exact replacement relations (`edits.from`/`edits.to`, aliases normalized by
the server schema, and `changes.find`/`changes.replace`). Insertions, deletions, extraction
payloads, symbol-migration tables, and other writes without a literal `from`/`to` relation
remain in the 195-write denominator as zero-pair write classes, but are not invented into
synthetic pairs. Report both pair-weighted and write-weighted results; a write is expressible
within N verbs only when it has at least one eligible pair and every eligible pair is
expressible within N non-escape verbs. Writes with no eligible pair are reported separately,
not silently counted as failures or successes.

Both sides must parse with rewrite-clj. Parse failures are reported separately and count as
escape-hatch pairs in the headline conservative result.

## Frozen closed verb algebra

No executable model code, callbacks, predicates, regexes, SCI, `eval`, host interop, or
arbitrary function bodies are arguments. Paths are arrays of zero-based semantic child
indexes (whitespace and comments are not children). Literal arguments are data.

1. `replace-value`: replace one scalar leaf at `at` with literal `value`.
2. `rename-symbol`: replace every occurrence of one exact symbol `from` with symbol `to`
   inside the subtree at `at`; at least one occurrence must change.
3. `replace-string`: in one exact string scalar at `at`, replace all occurrences of literal
   string `from` with literal string `to`; no regex.
4. `wrap-form`: replace subtree S at `at` with a collection template containing exactly one
   hole occupied by S; `tag`, `prefix`, and `suffix` are literal data.
5. `unwrap-form`: replace a collection at `at` with one designated semantic child `child`.
6. `insert-child`: insert literal child `value` at semantic index `index` in the collection
   at `at`.
7. `remove-child`: remove the child at semantic index `index` in the collection at `at`.
8. `reorder`: permute all semantic children of the collection at `at` using exact integer
   vector `order`; no child may be added, removed, or changed.
9. `thread`: convert between an exactly nested unary call chain and `->` or `->>` at `at`;
   arguments are `direction` and `style`, and the server owns the deterministic lowering.
10. `extract-binding`: introduce one `let` binding for one exact repeated subtree within the
    subtree at `at`; arguments are literal symbol `name`, occurrence paths, and deterministic
    placement owned by the server.
11. `change-arglist`: replace one function arg vector at `at` with literal vector `value`;
    eligible only when the path is an arglist position in a `fn`, `defn`, or `defn-` form.
12. `replace-subform-freeform`: replace the subtree at `at` with arbitrary parsed Clojure
    payload `value`. This is the sole escape hatch and its share is the empirical ceiling on
    the closed-algebra hypothesis.

The taxonomy is now frozen. Implementation bugs may be repaired, but no verb or semantic
case may be added after the classifier sees a corpus payload. A missed recurring shape is a
finding, not permission to move the goalposts.

## Minimal-program search and tie-breaking

The classifier searches programs of zero through three non-escape verbs. Every candidate is
replayed by the same server-owned interpreter and must reproduce the parsed `to` tree exactly.
If no exact program exists, emit one root or smallest-changed-subtree
`replace-subform-freeform`. Minimality order is:

1. fewest verbs;
2. fewest canonical UTF-8 JSON bytes;
3. lexicographic canonical JSON, for deterministic ties.

Search is complete for one-edit structural differences and for compositions obtained by
recursively resolving disjoint changed subtrees through depth three. Specialized verbs may
replace a generic multi-verb explanation only when exact replay succeeds. Report the search
coverage boundary honestly; do not describe bounded search as mathematical global minimality.

## Byte and token law

Canonical program encoding is compact JSON with sorted object keys and no insignificant
whitespace. The full payload is `{"ops":[...]}`. Each operation carries its unabbreviated
`op`, its `at` path (including `[]`), and all literal arguments required for replay. Literal
Clojure data is encoded as a JSON string containing its exact printed source when JSON has no
lossless native representation. This deliberately charges realistic grammar overhead.

Actual emitted `to` bytes are the canonical JSON scalar bytes of each original `to` value,
including JSON quoting and escaping but excluding the already-required field key. Program
bytes are the full canonical program bytes. Net bytes saved are `sum(to_bytes-program_bytes)`;
negative values remain negative. Estimated tokens use the preregistered transport estimate
`ceil(bytes / 4)` per payload, reported as an estimate rather than tokenizer ground truth.

No cost is assigned to model reasoning, server implementation, response bytes, locators, or
guards; this experiment isolates replacement-payload emission. A sensitivity table will also
show +16 and +32 bytes per program of hypothetical outer protocol overhead.

## Registered outputs and verdict gates

Report:

- eligible pair and write counts, parse failures, and zero-pair write classes;
- pair- and write-weighted shares expressible in at most 1, 2, and 3 non-escape verbs;
- escape-hatch share by pairs, writes, `to` bytes, and program bytes;
- corpus-wide actual `to` bytes, program bytes, net bytes, and estimated net tokens;
- verb frequency and cumulative coverage of the five most frequent verbs;
- write-class stratification;
- a paired comparison with the sibling splice study on the same eligible pairs and write
  classes, using its frozen metric without reinterpreting its outcomes.

Verdict gates, chosen before counting:

- **strong support:** at least 70% of eligible writes need no escape hatch and fit in at most
  three verbs, at least 50% fit in one verb, and net estimated tokens are positive;
- **qualified support:** 40–69.9% fit in at most three verbs without escape, or the share is
  at least 70% but net estimated tokens are non-positive;
- **weak/falsified for a small closed algebra:** under 40% fit in at most three verbs without
  escape;
- regardless of the gate, an escape-hatch share above 30% is a product warning and above 50%
  makes the escape hatch the real interface.

The verbs-versus-splices design fork is judged per write class: lower exact encoding bytes
wins only when both encodings reproduce the same target; otherwise the reproducing encoding
wins. Ties within 5% are reported as ties because wrapper choices dominate differences that
small.

## Privacy and reproducibility

The classifier runs locally and emits only aggregate statistics. Raw requests, source,
paths, owner names, literal payloads, project context, and prose never enter Git. An ephemeral
per-pair ledger may contain only opaque ordinal, write class, verb names/count, and byte
counts; it is deleted after aggregate reconciliation. The committed harness may contain no
corpus content and must fail closed unless the three authority counts match exactly.
