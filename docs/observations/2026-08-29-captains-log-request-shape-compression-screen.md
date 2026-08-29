# Captain's Log: Close Relations, Do Not Dictate Rows

Date: 2026-08-29

Durable owner: `clj-surgeon-45j`

Status: offline screen complete; GO for two bounded model treatments after
SURGEON1 review; NO-GO for product integration from this branch.

## Mission

Reduce first-call request materialization time for the frozen submission-row
transaction without changing its authority or route:

- one `edit_clojure` call;
- one atomic transaction;
- 51 exact matches across 9 files;
- exact target bytes and hashes;
- zero refusal, fallback, or source discovery;
- terminal evidence.

The question was not whether JSON can be made smaller. The question was
whether a closed request vocabulary can delete model construction decisions
and compile injectively to the existing canonical edit transaction.

## Immutable basis

- Product base and head before this experiment:
  `ce05f6ee099ac029d96ecb6db6f5f225e4239b96`
- Product tree:
  `ecec5aebfa0f8adb6d76eeadaf3113ff8aeb7b3d`
- Archive:
  `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/clj-surgeon-field-alias-ce05-20260829T071022Z.tar.gz`
- Archive SHA-256, independently verified:
  `e41da53fd2b973d3545f4608365416d40c20e24a8f865edccec161699563972f`
- Offline screen:
  `dev/experiments/request_shape_compression_screen.clj`
- No model tokens, product edits, install, reload, shared-port call, or process
  mutation were used.

The retained candidate arms were both exact and route-adherent:

| Arm | Complete wall | MCP observed | Server authoritative | Input / uncached / output tokens | Route |
|---|---:|---:|---:|---:|---|
| 02-B | 59.277 s | 1.260 s | 1.222 s | 67,550 / 19,166 / 2,446 | one `edit_clojure` |
| 03-B | 63.595 s | 1.162 s | 1.127 s | 55,253 / 13,013 / 3,137 | one `edit_clojure` |

Both arms scored exact on all nine target files. Their retained workspace hash
fences were:

| Arm | Start SHA-256 | Final SHA-256 |
|---|---|---|
| 02-B | `a35ef6639a05c1132ea0159d19729d059bff5e222c493bbe3900b2c645ce9731` | `383787d4b9155f70810f8d11eaffede51054982a61390cb7cb5aefdbbbe742b7` |
| 03-B | `58713b251c5eac72686f60b9ce8339fce94d9006245c2b8d4f1d53d2a614baef` | `1f52d980a57d2947532fb8f299656aa1033ec218edb8af7dea5e15794c9a273a` |

## What the clock says

```text
02-B  59.277 s complete

 turn start   first message        tool starts        tool done       final
     |            |                    |                  |             |
     +--6.216s----+------42.214s-------+----1.260s--------+--2.928s-----+
                  request construction   server 1.222s    receipt

03-B  63.595 s complete

 turn start  message 1      message 2       tool starts      done      final
     |           |              |                |             |         |
     +--4.941s---+---13.296s-----+----40.025s-----+---1.162s----+--3.372s+
                                   construction      server       receipt
                                                     1.127s
```

The event clock does not reveal private reasoning. It does establish a hard
boundary: 40.0 to 42.2 seconds elapsed after the final visible message and
before the one mutation call began. Server-authoritative work was only 1.8% to
2.1% of complete wall. This makes request construction the earned next hill.

## Exact payload anatomy

The 03-B canonical JSON payload is 6,353 UTF-8 bytes. Arm 02-B is the same
request plus `workspace_root`, 6,477 payload bytes. The archived files include
one trailing newline, so their file sizes are 6,354 and 6,478 bytes.

The canonical request contains:

- 33 edit rows and 37 declared edit matches;
- 30 redundant explicit `matches: 1` fields;
- 9 namespace require-clause edits;
- 23 owner-scoped symbol edits;
- 1 retained complete owner-form edit;
- 1 delete group with 14 exact owner names;
- 37 edit matches plus 14 deletions, for 51 total matches in 9 files.

Component sizes, measured as standalone compact JSON arrays, are:

| Component | Bytes |
|---|---:|
| 9 namespace edit rows | 2,252 |
| 23 symbol edit rows | 3,410 |
| retained complete owner edit | 389 |
| delete-owner group | 277 |

The model repeats the file 33 times, the scope 33 times, 33 literal from/to
pairs, and 30 default counts. Most of those repetitions are bookkeeping, not
judgment.

## Ten orthogonal shapes screened

| Option | Mechanism | Measured payload result | Construction effect | Decision |
|---|---|---:|---|---|
| 1. Omit `matches: 1` | Existing default | 5,993 B, -5.7% | Deletes 30 default fields only | Retain as control; too small alone |
| 2. File groups | State each file once, nest local edits | 5,189 B, -18.3% | Deletes 24 file repetitions and 30 defaults | Treatment A |
| 3. File index | Nine-file table plus numeric references | 5,517 B, -13.2% | Replaces paths with indices | Reject: adds index/order decisions |
| 4. Closed symbol relations | Per-file prefix pair plus owner/name rows | 4,892 B, -23.0% | Replaces 23 repeated from/to pairs with 9 prefix pairs and 23 names | Promising component |
| 5. Closed relations plus require delta | Add one require target to 9 files, name 3 removals, then symbol relations | 3,600 B, -43.3% | Deletes 18 exact require-clause strings and most repeated symbol prefixes | Treatment B |
| 6. Positional tuples | Replace object keys with array positions | 4,977 B, -21.7% | Compresses bytes, not decisions | Reject: positional mistakes become plausible |
| 7. String dictionary | Intern files, owners, prefixes, and clauses | Not promoted | Converts names into arbitrary IDs | Reject: complexity is hidden in dictionary construction |
| 8. Opaque plan/template ID | Refer to prior server state | Not promoted | Very small second call | Reject: moves complexity into another turn and violates first-call mission |
| 9. SCI/program macro | Ask the model to emit an expansion program | Not promoted | Exchanges data construction for code synthesis | Reject: larger failure surface and ambiguous authority |
| 10. More field aliases | Add spellings for the same from/to rows | Near-zero byte reduction | Improves vocabulary legibility but retains 33-row construction | Stop: ce05 already earned the canonical closed vocabulary |

All six materialized shapes re-expanded to the same multiset of 33 canonical
edit rows. The relation-plus-require-delta screen additionally compiled the
delta against a frozen map of the nine exact current require clauses and
reproduced every original namespace from/to row exactly.

## Recommended treatment A: file groups

Shape:

```json
{
  "edit_groups": [
    {
      "file": "src/sample/views/review.clj",
      "edits": [
        {"within": {"form": "render-review"},
         "from": "review/row-controls*",
         "to": "submission-row/row-controls*"}
      ]
    }
  ],
  "delete_owners": ["unchanged exact group"]
}
```

The source-blind compiler expands each local row with its group file and the
default count, then delegates to the current canonical compact-edit compiler.
It must refuse before source access when:

- a group has no file or no edits;
- a local row also supplies `file` or `files`;
- grouped and flat fields create ambiguous ownership;
- duplicate JSON keys or unsupported fields appear;
- expanded counts exceed existing bounds.

Measured reduction is 1,164 bytes, 18.3%. A deliberately optimistic linear
projection applies that reduction to the 40.0 to 42.2 second materialization
interval: 7.3 to 7.7 seconds. This is an upper-bound hypothesis, not a timing
result; the model cohort must establish the actual causal effect.

## Recommended treatment B: closed rewrite relations

Shape:

```json
{
  "require_change": {
    "add": {"lib": "sample.views.submission-row", "as": "submission-row"},
    "files": [
      {"file": "src/sample/review_updates.clj"},
      {"file": "src/sample/views/log.clj",
       "remove": {"lib": "sample.views.review", "as": "review"}}
    ]
  },
  "symbol_rewrites": [
    {
      "file": "src/sample/review_updates.clj",
      "from_prefix": "review/",
      "to_prefix": "submission-row/",
      "owners": [
        {"form": "record-submission-row", "symbols": [{"name": "row-controls*"}]}
      ]
    }
  ],
  "edits": ["one retained complete owner edit"],
  "delete_owners": ["unchanged exact group"]
}
```

The measured request has one require target, nine require files, three exact
remove pairs, nine symbol file groups, twenty owner groups, twenty-three
symbol names, and one retained literal pair. It is 3,600 bytes, saving 2,753
bytes or 43.3%.

This is real representation compression because the model states the semantic
relation once. The server reconstructs repeated canonical rows in the same
call. There is no plan handle, prior discovery call, stateful template, or
second model turn.

The linear upper-bound projection against the observed materialization interval
is 17.3 to 18.3 seconds. Again, this is projected, not measured. The principal
expected win is decision deletion: the model no longer authors eighteen exact
require-clause strings or twenty-three repeated prefix pairs.

## Pure compiler boundary

```text
closed request shape
        |
        v
validate closed vocabulary and cardinality       source-blind, pure
        |
        v
freeze every named file and hash once             existing transaction context
        |
        v
expand-file-groups / compile-rewrite-relations    pure(request, frozen snapshot)
        |
        +-- ambiguous/missing/non-injective ------> refuse, source_unchanged
        |
        v
canonical edits + delete_owners                   byte-equal to today's request
        |
        v
existing editor-gestures->direct-params
        |
        v
existing compact location normalization
        |
        v
existing atomic transaction + exact evidence
```

The pure compiler has no write authority. It accepts data plus frozen source
bytes and returns canonical data or a typed refusal. It does not infer by edit
distance, select a similar owner, call a semantic provider, or mutate source.

The require compiler must use a lossless structural representation in product,
not the fixture screen's exact string splice. It may compile only when:

- the file has exactly one applicable namespace owner;
- the declared target require is absent;
- a declared removal resolves exactly once with the exact lib and alias;
- no platform-conditional or duplicate clause makes the location ambiguous;
- the transformed clause reparses and retains all unrelated bytes;
- the canonical expansion preserves the frozen file hash and exact match count.

The symbol compiler may compile only when every owner and exact from symbol has
the declared cardinality. Prefix/name concatenation must be injective, and two
relations must not lower to the same canonical target row.

## Falsifiers

Any item below makes the treatment NO-GO:

1. Expansion is not byte-equivalent to the retained canonical edit multiset.
2. Final hashes differ from the unchanged scorer target.
3. One input relation can lower to more than one canonical transaction.
4. Duplicate owners, CLJC branches, aliases, or require clauses are selected by
   heuristic instead of refusing.
5. A relation overlaps a retained literal edit or another relation.
6. Failure happens after any write, or refusal omits `source_unchanged=true`.
7. The model uses discovery, a preparatory plan, shell, fallback, or more than
   one MCP action.
8. The transaction loses the exact 51-match, 9-file accounting or terminal
   evidence.
9. The new schema makes control callers slower or less first-call-correct.
10. Two correct counterbalanced model pairs show less than a material complete
    wall improvement despite the smaller representation.

## Product overlap if SURGEON1 promotes an option

Likely shared files:

- `src/clj_surgeon/mcp_schema.clj`
- `src/clj_surgeon/mcp_contract.clj`
- `src/clj_surgeon/mcp_compact_edit_fields.clj`
- a new pure relation compiler, preferable to growing the existing contract
  namespace;
- `src/clj_surgeon/mcp_tool.clj` only if description or frozen-source routing
  must change;
- matching `test/clj_surgeon/mcp_*_test.clj` witnesses;
- `docs/high-level-design.md` and
  `docs/intent/mcp-operation-contract/**` through the required LID chain.

This branch changes none of those product files.

## Decision and next experiment

GO, after SURGEON1 review, for the smallest counterbalanced model screen:

```text
pair 1: current flat request -> file groups
pair 2: closed relations + require delta -> current flat request
```

Freeze candidate commit, task, scorer, model/effort, prompt, workspace snapshot,
one-call route, and terminal evidence. Measure time to first call, emitted
argument bytes, mutation-call materialization, server wall, receipt time,
complete wall, refusal/fallback count, and final hashes.

Promotion gate: both treatment runs must be exact on the first call. A shape
earns product work only if it removes at least 20% complete wall or materially
improves first-call correctness/recovery. A payload-size win alone is not a
product win.

NO-GO for file indices, positional tuples, dictionaries, opaque plan IDs, or
program synthesis. They compress syntax while adding construction choices or
moving the same work across another model boundary.

## What became cheaper

The retained script can screen a new closed representation against both exact
candidate requests in under one second without a model run. It reports payload
bytes, decision geometry, and exact canonical-edit equivalence. This makes the
next high-cost model experiment smaller, falsifiable, and reversible.
