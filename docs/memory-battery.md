# The memory battery

`make memory-battery` measures whether clj-surgeon's tree-scale operations size
their heap by the work they do or by the repository they are pointed at.

It is a **measurement gate**, not a unit test. It changes no operation. It runs
one JVM at a bounded heap, drives every tree-scale operation over synthetic
repositories at 100 / 1,000 / 10,000 files, and returns a receipt and a verdict.

Intents: **MCP-OP-MEM-001** (the per-operation memory/work receipt block) and
**MCP-OP-MEM-011** (this battery as a release gate) —
`docs/intent/memory-boundedness/`.

---

## How to run it

```bash
make memory-battery
```

That is the whole thing. It generates the trees if they are missing, runs the
unbounded reference pass once if its hash cache is missing, then runs the
bounded battery and prints the table.

Knobs, all optional:

| variable | default | what it does |
|---|---|---|
| `MEMBAT_ROOT` | `/home/forge/tmp/membat` | where the trees, reference cache, and receipts live |
| `MEMBAT_XMX` | `512m` | the bounded budget the battery runs at |
| `MEMBAT_REFERENCE_XMX` | `4g` | heap for the one unbounded reference pass |
| `MEMBAT_REPS` | `5` | reps per (op, N); rep 1 is `fresh`, the rest aggregate into `warm` |
| `MEMBAT_SCALES` | `100,1000,10000` | tree sizes |
| `MEMBAT_OP_TIMEOUT_MS` | `600000` | if one fresh rep exceeds this, its warm reps and every larger N for that operation are skipped |

Sub-targets, if you want them separately:

```bash
make memory-battery-generate    # build/verify the trees (~1 s for all three)
make memory-battery-reference   # (re)build the unbounded reference hashes
make memory-battery-self-test   # millisecond self-test; this one IS in `make test`
```

To force a fresh reference after changing an operation's output shape, delete
`$MEMBAT_ROOT/reference-hashes.edn`.

### Exit codes

| code | meaning |
|---|---|
| 0 | **PASS** — every pass line was observed and held |
| 1 | **FAIL** — at least one pass line failed |
| 2 | refusal — bad environment (root missing, trees incomplete, wrong heap for the mode, stale reference attestation) |
| 3 | tool failure — an operation threw something that is not an `OutOfMemoryError` |
| 4 | **INCOMPLETE** — nothing measured failed, but at least one line was never observed |

There are **three terminal states**, not two. `INCOMPLETE` is nonzero on purpose:
an all-green run that never observed a line is not evidence of boundedness, and a
release gate must block on it. It is separate from `FAIL` because nothing that was
measured actually broke — the remedy is to measure the missing line, not to fix an
operation.

---

## What the lines mean

The constants live in exactly one place: `pass-lines` in
`src/clj_surgeon/memory_battery.clj`. They are Sol's measured set, verbatim.

| line | rule |
|---|---|
| `oom` | no operation may exhaust the configured heap |
| `peak-over-budget` | sampled process-wide used-heap peak ≤ min(used-heap start + **224 MiB**, **80 %** of `-Xmx`) — about 410 MiB at 512m |
| `reserved-peak-over-budget` | attributable reserved peak ≤ **192 MiB** |
| `peak-scales-with-n` | peak at 10,000 files ≤ peak at 1,000 files + **32 MiB** |
| `retained-scales-with-n` | after-GC retention at 10,000 files ≤ after-GC retention at 1,000 files + **8 MiB** |
| `reference-mismatch` | the bounded result must hash identically to the unbounded reference result at the same N |

The key graph is peak against N. Once the bounded buffers fill it must visibly
flatten. Wall time and spill bytes may grow with N; retained heap may not.

### The three heap columns

| column | measured | what it tells you |
|---|---|---|
| `peak_mb` | continuously, every 5 ms, on a daemon thread, during the call | the process-wide used-heap high-water mark, garbage included |
| `held_mb` | after four `System/gc`s **while the result is still referenced**, minus the pre-call used heap | what the operation's result actually costs to hold — the receipt's retained size |
| `afterGC_mb` | after four more `System/gc`s once the result is dropped | leak check: caches, memoisation, thread-locals that outlive the call |

`held_mb` is the number that shows an operation sizing itself by the repository.
`afterGC_mb` staying flat is not evidence of boundedness; it only means the
operation did not leak.

### UNMEASURED is not a pass

Three lines report **UNMEASURED** rather than passing when their inputs were not
observed. Any unmeasured line sets `:complete? false`, makes `:pass?` false, and
drives the terminal state to `INCOMPLETE` (exit 4) unless a real failure outranks
it:

- `reserved-peak-over-budget` — no operation on this branch has an admission
  accountant, so nothing reports an attributable reserved peak. The sampled
  process-wide peak is a different quantity and is not substituted for it.
- `peak-scales-with-n` / `retained-scales-with-n` — no cells at 1,000 or at
  10,000 files, e.g. because a smaller N blew `MEMBAT_OP_TIMEOUT_MS`.
- `reference-mismatch` — no cached unbounded reference hash to compare against.

A run whose only problem is an unobserved line prints `verdict: INCOMPLETE` and
exits **4**. It never prints `PASS` and never exits 0. `FAIL (INCOMPLETE)` means
both happened: something measured broke *and* something else was never observed;
the failure wins the exit code. Read the UNMEASURED rows in either case.

### Two of Sol's lines this battery does not run

Recorded as boundaries on MCP-OP-MEM-011, not as constants here:

- the 450 × 1.9 MiB aggregate-admission case (refuse with < 32 MiB heap growth,
  or complete under a larger aggregate-I/O allowance without raising the heap
  line);
- injected conflict at staging, validation, every commit boundary, and rollback
  (zero writes, or verified restoration to every H0 hash).

Both need the transactional kernel, which does not exist on this branch. Do not
claim them from this battery's output.

---

## Why it is not in `make test`

Two reasons, and both are load-bearing:

1. **It is minutes-scale.** `make test-fast` is a seconds-scale inner loop. A
   gate that makes the fast loop slow is a gate that gets commented out.
2. **It needs a dedicated JVM at a specific `-Xmx`.** It cannot share a heap
   with a test runner without measuring the test runner.

So `make test` runs `memory-battery-self-test` instead — the generator's
determinism check plus `clj-surgeon.memory-battery-test`, a millisecond witness
that feeds hand-written synthetic numbers to the pure verdict function and
asserts the pass lines are applied exactly.

That witness also asserts the gate itself:

- `memory-battery` **exists** as a Makefile target and carries
  `@spec MCP-OP-MEM-011` in its recipe;
- `memory-battery` is **not** in the transitive target closure of `test`,
  `test-fast`, `mcp-test`, or `runtests`.

Delete the Makefile target and the fast suite goes red. Delete the witness
namespace and the intent contract audit
(`clj-surgeon.mcp-intent-contract/audit-current-repository`) goes red for a
missing test witness. Dropping the battery is loud in both directions.

---

## The rule

**Any change that touches memory posture in a tree-scale operation re-runs
`make memory-battery` before merge, and the receipt goes in the PR.**

That means: discovery and walking, slurping, parsing, projection, caching,
spill, receipt shape, parallelism, or any `-Xmx`/heap-budget default. Compare
the new table against
`docs/observations/2026-09-03-memory-battery-baseline.md` (the RED baseline) or
against the last accepted receipt, whichever is more recent.

A green compile, a green `make test`, and a faster wall clock are not evidence
about memory. Only the battery is.

---

## The arms

`ops` in `src/clj_surgeon/memory_battery_runner.clj`. Each is driven as a black
box through its public entrance; the battery never reaches inside an operation
and never changes one.

| arm | entrance | what it stresses |
|---|---|---|
| `cli-ls-tree` | `core/run-ls-tree {:dir root :format :edn}` | discovery, `pmap` outline over every file, all outlines held in one vector |
| `workspace-sources-read-all` | `mcp-workspace-sources/read-all` | every source slurped into one `sorted-map` (`extract/plan` repeats this shape inline) |
| `rename-ns-plan-narrow` | `rename/plan {:from "membat.pkg000"}` | the walk: every file parsed, only 100 ever match |
| `rename-ns-plan-full-match` | `rename/plan {:from "membat"}` | the plan: every file matches, so renames and file moves grow with N |

The two rename arms exist because a single narrow arm reports `ok` at 10,000
files while only matching 1 % of them. **A battery that grades the query shape
rather than the operation is worse than no battery.** When you add an arm, ask
what query would make its result grow with N, and add that one too.

## The synthetic trees

`bench/memory_battery/generate_tree.clj` writes deterministic namespaces with
requires, a def, private helpers, a public entry point, a three-arm multimethod,
and padding to a size bucket. 60 % are ~1.5 KB, 30 % ~5 KB, 10 % ~15 KB —
mean 4,047 B/file, which is deliberately **smaller** than this repository's own
17 KB mean, so the battery cannot be accused of being sized to fail.

All three trees build in about five seconds and occupy 70 MB.

Re-running `memory-battery-generate` **verifies**; it does not assume. For every
file the generator promises at that N it checks the file exists, its byte count
is exact, and its content digest matches the deterministic source — then it
rejects any file under `src/` the generator never wrote, and rechecks the
manifest's own digest (which covers file *contents*, not just paths and sizes)
against the bytes on disk. Bad or missing bytes regenerate the tree; unexpected
files **refuse** with exit 2, because regeneration would not remove them and
deleting files under `MEMBAT_ROOT` on the corpus's own say-so is worse than
stopping.

Verification costs about 3 s for all three trees. That is the price of a table
that cannot print `N=10,000` over a corpus of 9,999 files: the previous no-op
compared only `generator-version`, `n`, and the manifest's *claimed* file count,
so one deleted file was invisible and its claim was copied straight into the
receipt.

They do **not** include token-dense or deeply nested adversarial files. That is
a separate arm.

## The receipt

`$MEMBAT_ROOT/receipts/<timestamp>-battery.edn`, plus
`latest-battery.edn`. It carries the pass lines in force, the JVM version, the
tree manifests, every per-(op, N, phase) cell with its per-rep wall times, the
verdict with each failure's observed and limit values, every unmeasured line
with its reason, and a `:measurement-note` stating that `heap-used-peak-mb` is a
sampled process-wide peak rather than a post-GC delta or a per-operation figure.
