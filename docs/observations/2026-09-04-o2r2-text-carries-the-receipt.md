# O2 round 2 — the text block carries the receipt, for every mode and every refusal

Branch `bridge/study-ops-mcp`, built on the O2 tip `26e4810`. Written 2026-09-04 00:41 UTC.
Driven by the independent Opus re-review at `/home/forge/tmp/sol/o2-opus-review.md`
(GO-WITH-FIX, ten numbered findings).

## What this round closes

O2 proved one thing on one mode: an `ls-tree` receipt's rows belong in
`content[0].text`, because a text-only client sees `structuredContent` never.
The re-review found the same defect standing on eight of the other nine modes,
from one function, plus a bound the new rendering silently crossed.

The invariant this round holds, for every mode and every refusal:

> `content[0].text` carries the facts `structuredContent` carries — the rows,
> the answer, the cause, the remedy, and the executable continuation when one
> exists — bounded by the same public budget, and declaring in the text when it
> was bounded.

## Measured, before and after

Public MCP result, real E6 tree `/home/forge/tmp/arms/e6/pf3`,
`ls-tree dir=src grep=defn limit=16384`:

| | bytes | TEXT |
|---|---|---|
| before | **34,042** (budget 32,768) | 16,471 |
| after | **32,526** | 14,974 |

TEXT block, `src/clj_surgeon/analyze.clj`:

| mode | before | after |
|---|---|---|
| `deps limit=16384` | 241 | 1,922 |
| `deps limit=200` (truncated) | 286 | 632 |
| `topo limit=16384` | 241 | 1,128 |
| `ls-deps` | 231 | 1,803 |
| `ls-extract` | 243 | 293 |
| `outline` | 273 | 1,556 |
| `forms include_source` | 258 | 432 |
| `match` (7 matches, 3,529 chars) | 206 | 4,073 |
| `ls-deps` refusal | 238 | 1,127 |
| `missing-fields` refusal | 71 | 174 |

## What this round does NOT establish

- **It does not prove the budget bites in the field.** The largest real reads in
  this repository stay well inside 32,768 bytes — `outline` of the 126,596-byte
  `intent_transaction.clj` measures 23,678, and a twenty-owner `forms` read
  reaches an earlier gate (`batch-source-limit-exceeded`) first. The typed
  refusal branch is witnessed directly, on a synthetic oversized receipt,
  because no real read in this tree reaches it.
- **It does not prove an agent reads the rows.** It proves they are there. The
  adoption question — whether a free-choice arm that could not act on a
  146-character header now acts on a 1,900-character one — is an E6 question and
  needs an E6 run.
- **It does not audit `prepare-change`, `plan-extraction`, `basis-view`, or
  `verification-job`.** Those carry their own summaries and their own suites.
  They are named in the class ratchet's mode table so they cannot become
  unclassified, not witnessed by it.
- **The `client_run_id` now comes from the transport's SDK session.** That
  identifies a CONNECTION, not an experimental arm. A cohort that wants
  per-arm telemetry still has to give each arm its own client session — which
  it now can, and previously could not.
- **The text allowance (8,192 characters, floor 512 per result in a batch) is a
  judgment, not a measurement.** It was chosen to match the `ls-tree` default
  limit O2 witnessed. A batch of 32 files divides it to the floor.

## Deliberate contract reversal

The text block was documented as a "source-free companion" to
`structuredContent`. It is not one any more: `forms` returns source by default,
so the text carries it. Three assertions were inverted or renamed —
`mcp_inspect_tool_test.clj:302`, `mcp_http_server_test.clj:296`, and
`summaries-are-stable-concise-and-source-free` (renamed; it asserts the
envelope of a result carrying no rows, and still passes byte-for-byte).

## Gates

```
suite-run bb test/run_all.clj              731 tests / 6,023 assertions / 0 / 0
suite-run clojure -M:clj-surgeon/mcp-test  462 tests / 5,998 assertions / 0 / 0
make mcp-operation-oracle                  pass
mcp-intent-contract/audit-current-repository  0 violations
CLI goldens (4 files)                      byte-identical to the GO tip 4480e3d
```
