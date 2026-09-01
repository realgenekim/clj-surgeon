# kc-lqbm — the combinable-transaction steering note

builder2 @ `/tmp/cc-lqbm-build`, branch `lqbm-build`, base `c44ac759`.

## What was built

`edit_clojure` gained `create_files` in `cf4798c8` so that edits and file
creation can ride one atomic transaction. The observed adoption gap is that
callers still commit an edits-only transaction and then, seconds later, a
`create_files`-only transaction in the same workspace — the exact pair the verb
exists to fuse. Gene ratified a steering note: the server should NOTICE the pair
and say so in the receipt.

New namespace `clj-surgeon.mcp-combinable-transaction`:

- **`transaction-shape`** — a pure projection of one request into
  `:edits-only`, `:create-only`, `:mixed`, or nil. Reads both keyword and
  public JSON string keys, and treats a `java.util.Map`/`java.util.Collection`
  from the SDK the same as a Clojure map/vector. `:programs`,
  `:delete_owners`, `:symbol_migration`, and `:require_change` are all
  edits-side verbs. A prepared confirmation (`confirm` + `fill`) is
  `:edits-only` by the prepared-request schema, which declares `arguments`
  with `additionalProperties false` and requires `edits`, so no `create_files`
  can reach that route.
- **A bounded per-session memo**, keyed `[boot-epoch session-key]`, holding
  `{shape, workspace-root, receipt-hash, committed-at, expires-at}`. TTL 10
  minutes. Same idioms as `mcp_prepared_confirmation.clj`: injected `clock`,
  injected `boot-epoch`, an explicit lock plus an atom, expire-on-read, and
  eviction of the oldest by an ordered key. Default cap 256 sessions.
- **`observe-transaction!`** — the single write path. A commit records the memo
  and returns the note; a refusal FORGETS the memo; anything else (a preview,
  or any result that is neither `ok+committed` nor `ok=false`) leaves the memo
  untouched.
- **`attach-note!`** — the receipt-level entrance. Adds
  `combinable_note {prior_receipt_hash, hint}` and nothing else.

Wired into `mcp_tool/handle-operation` for `edit_clojure` only, keyed by
`prepared-confirmation/exchange-session-key`, and `mcp-tool/init!` now resets
the registry so a server restart forgets every memo.

`MCP-OP-EDIT-032` filed in
`docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md` with its
adversarial row.

## Counts

### Frozen red — commit `a9a23914`, before any implementation existed

```
clj-surgeon.mcp-combinable-transaction-test
Ran 16 tests containing 23 assertions.
16 failures, 0 errors.   (7 pass)
```

The 23-assertion count is honest but understates the law surface. The repo's
`with-<api>` guard (copied from `mcp_prepared_confirmation_test.clj`) collapses
each pure-law test to a single "production entrance is absent" failure while the
namespace does not exist, so the assertions inside those tests never execute.
The one assertion that failed on real evidence rather than on absence is the
wire assertion in `real-edit-then-create-transactions-are-named-combinable`: the
second receipt did not name the first receipt's hash. The 7 passes are that
same integration test's other assertions, which hold at red because a nil note
is what red looks like from the wire.

Baseline at red, whole `mcp-test` runner: **370 tests, 3870 assertions, 0
failures, 0 errors excluding this namespace** (16 failures with it).

### Green — after implementation

```
clj-surgeon.mcp-combinable-transaction-test
Ran 16 tests containing 62 assertions.
0 failures, 0 errors.
```

Assertions went 23 → 62 because the guard no longer short-circuits.

### Full gate

- `make test-fast` — **647 tests, 5562 assertions, 0 failures, 0 errors.**
- `make mcp-test` — **370 tests, 3909 assertions, 0 failures, 0 errors**, and
  the target itself exits 0 including the heap, clj-kondo-admission-path,
  analyzer-contract-target, cclsp-start, and cclsp-client-audit self-tests.
  (3909 = the 3870 at red plus the 39 assertions that stop being
  short-circuited once the namespace exists.)
- `make analyzer-contract-test` — **4 tests, 20 assertions, 0 failures, 0
  errors**, exit 0.
- `clj-kondo` over the three touched Clojure files — **0 errors, 0 warnings.**
- **`make test` — EXIT 0. The complete gate is green.** Every stage:

  | stage | result |
  |---|---|
  | `check-clj-surgeon-skill-mirrors` | passed |
  | `test-fast` | 647 tests, 5562 assertions, 0 failures, 0 errors |
  | `analyzer-contract-test` | 4 tests, 20 assertions, 0 failures, 0 errors |
  | `mcp-test` (incl. the Prolog operation oracle and 5 self-tests) | 370 tests, 3909 assertions, 0 failures, 0 errors |
  | `mcp-smoke` (stdio initialize + tool discovery + refusal) | passed |
  | `collect_agent_usage.py --self-test` | passed |
  | bench self-tests, edit portfolio, clean-codex, ops registry, schedule | passed |
  | performance sentinel, benchmark retention, evidence manifest | passed |

### The one failure I did not cause, and the proof

The first `make test` run stopped at `analyzer-contract-test` with 4 tests, 14
assertions, 5 failures, 2 errors. Every one of the 7 carried the same payload:

```
"error_type": "clj-kondo-pressure-deferred",
"status": "pressure-deferred",
"pressure": {"normalized_one_minute_load": 0.757, "severity": "red"}
```

That is the skiff's clj-kondo load-admission gate refusing to lint, not a code
failure — the same load-dependent class the `create_files` commit `cf4798c8`
recorded. Load average was 13.70 at that moment. Proof in both directions, run
minutes apart at load ~8.2:

| commit | load | result |
|---|---|---|
| `c44ac759` (base, before my change) | 8.58 | 4 tests, 20 assertions, **0 failures, 0 errors**, exit 0 |
| `01189c9e` (my tip) | 8.17 | 4 tests, 20 assertions, **0 failures, 0 errors**, exit 0 |

The base commit was checked out in this worktree, run, and restored. My change
touches no analyzer, lint, or diagnostic path: three lines in `mcp_tool.clj`,
one new namespace, one new test namespace, the test runner, and a spec doc.

## The one defect the tests caught, and how it hid

First green run: 15/16 green, one failure. The implementation had written the
shape-pair law as

```clojure
(contains? combinable-shape-pairs #{(:shape prior) shape})
```

A Clojure set literal built from two runtime-equal values throws
`IllegalArgumentException: Duplicate key`. So the moment a caller committed the
SAME shape twice in a row, `combinable-note` threw — and `attach-note!`'s
deliberate fail-soft `catch Exception _ result` swallowed it, skipping the memo
update. The visible symptom was not an error: the third transaction named a
receipt two commits old. Fixed by writing both orders out as vectors:

```clojure
(def ^:private combinable-shape-pairs
  #{[:edits-only :create-only]
    [:create-only :edits-only]})
```

This is worth recording because the fail-soft catch is correct policy for a
nudge and is also the thing that made a real defect invisible. See "Honest
gaps."

## Deviations from spec, with rationale

1. **A refusal is not the only thing that suppresses the note — a repeated
   shape is too, and a repeated shape still REFRESHES the memo.** The spec
   named the suppression cases; it did not say what a repeated shape does to
   the memo. I chose "record it, so the memo always holds the immediately
   prior committed transaction." One frozen-red test asserted the opposite by
   mistake — that after `edits-only, edits-only`, a following `create-only`
   gets no note. That is wrong on the spec's own terms: the immediately prior
   committed transaction WAS edits-only, so it is a genuine combinable pair.
   The test was corrected during green-up (commit `b`), and it is exactly the
   test that exposed the `Duplicate key` defect above.

2. **Only `edit_clojure` is observed, not `apply_clojure_changes`.** The spec
   says "edit_clojure transaction" and the adoption gap is on that entrance.
   `apply_clojure_changes` commits are neither noted nor recorded, so they do
   not break an `edit_clojure` pair either.

3. **A preview neither names nor forgets.** The spec listed commits and
   refusals. A preview is neither: it takes no write authority and produces no
   receipt hash. Treating it as a refusal would silently break legitimate
   pairs; treating it as a commit would name a receipt that does not exist.
   Witnessed by `a-preview-neither-names-nor-forgets`.

4. **Shape is read from the caller's supplied verbs, not from the kernel's
   `:edits` / `:created` counts.** The note is about what the caller asked
   for. A count-derived shape would misread a `delete_owners`-only
   transaction and would make the classification depend on kernel internals.

5. **`registry-stats` publishes `memo-count`, not the memos.** Session keys
   and workspace roots stay private, matching
   `prepared-confirmation/registry-stats`.

## Files touched

- `src/clj_surgeon/mcp_combinable_transaction.clj` — new, the whole feature.
- `src/clj_surgeon/mcp_tool.clj` — three edits: one require, one
  `reset-registry!` in `init!`, one `cond->` in `handle-operation`.
- `test/clj_surgeon/mcp_combinable_transaction_test.clj` — new, 16 tests.
- `test/clj_surgeon/mcp_test_runner.clj` — register the namespace.
- `docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md` —
  `MCP-OP-EDIT-032` plus its adversarial row.
- `LQBM-REPORT.md` — this file.

Commits, all local, none pushed:

| sha | commit |
|---|---|
| `a9a23914` | frozen red — 16 tests, 23 assertions, 16 failures |
| `01189c9e` | implementation — 16 tests, 62 assertions, 0 failures |
| (this file) | the report |

Author and committer on all three: `builder2 <builder2@skiff>`, trailer
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`.

A PostToolUse formatter hook reindented the whole of `mcp_tool.clj` on the
first edit (534 changed lines, semantically identical). That reformat was
reverted and the three edits reapplied out-of-band, so the committed diff to
`mcp_tool.clj` is 14 lines.

## Honest gaps and limitations

- **The fail-soft catch is a blind spot, by design.** `attach-note!` swallows
  any exception and returns the kernel's result untouched. That is right for a
  nudge — a steering bug must never cost a caller a commit — but it means a
  broken note is indistinguishable from "no pair," with no counter, no log
  line, and no owner. The test suite is currently the only detector, and it
  only detected this one because a downstream assertion disagreed about WHICH
  receipt was named. If the note ever matters more than steering, that catch
  needs a counter.
- **The note is advice with no evidence that it works.** Nothing measures
  whether callers who see it start batching. There is no telemetry field and
  no adoption denominator. "Zero notes emitted in a week" is currently
  indistinguishable from "callers fixed their habit."
- **Session identity is the SDK session, so a caller that reconnects loses its
  memo.** That is deliberate — a new session is a new caller as far as the
  server can prove — but it means the true rate of combinable pairs is
  under-counted, never over-counted. The note is conservative in the right
  direction.
- **Ten minutes is a guess.** It is short enough that a memo is plausibly one
  caller gesture and long enough to cover a slow turn, but no measurement
  chose it. It is a single private constant if it needs to move.
- **`workspace_root` equality is string equality on the already-canonicalized
  root the result publishes.** Two spellings of the same directory that
  canonicalize differently would suppress the note. Suppression is the safe
  failure.
- **Only the immediately prior committed transaction is considered.** A caller
  who interleaves edits-only, create-only, edits-only gets a note on each of
  the last two, which is correct but chatty. There is no per-session rate
  limit on the note.
- **`transaction-shape` classifies a prepared confirmation as `:edits-only` by
  reading the prepared-request SCHEMA rather than the reconstructed
  arguments.** That is sound today because the schema forbids `create_files`
  in `arguments`. If the prepared-request schema ever admits another verb, this
  classification silently becomes wrong. It is witnessed by a test, not by a
  type.
