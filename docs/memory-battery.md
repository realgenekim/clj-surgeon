# The memory battery

`make memory-battery` measures whether clj-surgeon's tree-scale operations size
their heap by the work they do or by the repository they are pointed at.

It is a **measurement gate**, not a unit test. It changes no operation. It runs
one JVM at a bounded heap, drives every tree-scale operation over synthetic
repositories at 100 / 1,000 / 10,000 files plus three adversarial corpora, and
returns a receipt and a verdict.

Intents: **MCP-OP-MEM-001** (the per-operation memory/work receipt block) and
**MCP-OP-MEM-011** (this battery as a release gate) —
`docs/intent/memory-boundedness/`.

---

## How to run it

```bash
make memory-battery
```

That is the whole thing. It builds and **verifies** the corpora, checks that the
cached unbounded reference is attested to this run (rebuilding it if not), then
runs the bounded battery and prints the table.

Knobs, all optional:

| variable | default | what it does |
|---|---|---|
| `MEMBAT_ROOT` | `/home/forge/tmp/membat` | where the trees, reference cache, and receipts live |
| `MEMBAT_XMX` | `512m` | the bounded budget the battery runs at |
| `MEMBAT_REFERENCE_XMX` | `4g` | heap for the one unbounded reference pass |
| `MEMBAT_REPS` | `5` | reps per (op, N); rep 1 is `fresh`, the rest aggregate into `warm` |
| `MEMBAT_SCALES` | `100,1000,10000` | default-corpus tree sizes (the adversarial arms are fixed) |
| `MEMBAT_OP_TIMEOUT_MS` | `600000` | if one fresh rep exceeds this, its warm reps and every larger N for that operation are skipped |

Sub-targets, if you want them separately:

```bash
make memory-battery-generate    # build/verify every corpus (~3 s when intact)
make memory-battery-attest      # seconds: is the cached reference bound to this run?
make memory-battery-reference   # (re)build the unbounded reference hashes
make memory-battery-self-test   # millisecond self-test; this one IS in `make test`
```

### The reference is attested, not merely present

Output parity is only a pass line against a reference that measured **this**
experiment. `reference-hashes.edn` therefore carries an `:attestation` alongside
its `:hashes`, and the battery **refuses** (exit 2) rather than compare against
one that does not match:

| bound field | why |
|---|---|
| `:ops` / `:ops-digest` | which arms were measured, and through which entrances |
| `:src-digest` | every `src/clj_surgeon` source: any change that could alter an operation's output invalidates the reference |
| `:generator-digest` | the corpus generator's own source |
| `:corpus-digests` | each tree's content manifest digest |
| `:jvm` | `java.version` |

`:head-sha` is **recorded but not compared**. Binding parity to HEAD would
invalidate the reference on every unrelated commit, and rebuilding it is a
minutes-long 4 GiB pass; `:src-digest` already covers everything that could
change an operation's output. A reference file with no attestation at all (the
pre-attestation format) is `:unattested-reference` and is never trusted.

`make memory-battery` runs `memory-battery-attest` first and rebuilds the
reference when it is stale, so the refusal is normally invisible — it fires when
the reference cannot be rebuilt, or when the battery is run directly.

This matters because `MEMBAT_ROOT` defaults to a path **shared between
worktrees**: before attestation, a `reference-hashes.edn` written from another
branch over a different corpus was accepted simply because the file existed.

To force a fresh reference by hand, delete `$MEMBAT_ROOT/reference-hashes.edn`
or run `make memory-battery-reference`.

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

Each line is either **HARD** (it decides the verdict) or a **TREND** (measured
and reported, never gated).

| line | kind | rule |
|---|---|---|
| `oom` | HARD | no operation may exhaust the configured heap |
| `reference-mismatch` | HARD | the bounded result must hash identically to the attested unbounded reference at the same N |
| `reserved-peak-over-budget` | HARD (once measurable) | attributable reserved peak ≤ **192 MiB** |
| `held-scales-with-n` | HARD | `max(held_mb at N=10,000)` ≤ `max(held_mb at N=1,000)` + **2.0 MiB** |
| `retained-scales-with-n` | HARD | persistent growth (`grow_mb`) at 10,000 files ≤ persistent growth at 1,000 files + **8 MiB** |
| `peak-over-budget` | TREND | sampled process-wide used-heap peak vs min(used-heap start + **224 MiB**, **80 %** of `-Xmx`) |
| `peak-scales-with-n` | TREND | peak at 10,000 files vs peak at 1,000 files + **32 MiB** |

**The `min` binds at the start term, not the `-Xmx` fraction.** At `-Xmx512m`,
80 % is 409.6 MiB, but with a JVM starting near 24 MiB of used heap the enforced
figure is `start + 224` ≈ **248 MiB**. (An earlier version of this document said
"about 410 MiB at 512m"; that was arithmetic about the wrong term of the `min`.)

### Why the peak lines are trends and not gates

`peak_mb` is an honest measurement and a poor requirement. It is a 5 ms sampled,
**process-wide** used-heap peak that includes garbage, and G1 moves it with
`-Xmx` and collector scheduling. Re-running an identical cell — `cli-ls-tree`,
N=1,000, fresh — moved it from 274.8 to 246.5 MB: a **28.3 MB swing that crossed
the verdict line on work that had not changed at all**.

A gate that flips on a rerun of the same work is a flaky gate, and a flaky gate
is eventually disabled, taking the honest signal with it. So the peak numbers are
kept, printed, and compared run-to-run under identical JVM, collector and heap
settings — as regression signals, not as proofs of live boundedness. What stays
HARD is what does not drift: exhausting the heap, output parity, the attributable
reserved peak (once an admission accountant exists), and the two cross-N
retention lines, whose measured values reproduced to within 0.2 MiB across runs.

The key graph is **held heap** against N: once the bounded buffers fill, what an
operation retains must visibly flatten. Wall time and spill bytes may grow with
N; retained heap may not. Peak against N is the same graph drawn with a noisier
instrument, which is why it informs rather than decides.

### The three heap columns

| column | measured | what it tells you |
|---|---|---|
| `peak_mb` | continuously, every 5 ms, on a daemon thread, during the call | the process-wide used-heap high-water mark, garbage included |
| `held_mb` | after four `System/gc`s **while the result is still referenced**, minus the pre-call used heap | what the call still holds with the result live — the receipt's retained size **plus** any cache or leak the call created |
| `excl_mb` | `held` − `after-release` | result-**exclusive** retention: what actually went away when the result was dropped, so it was the result's |
| `grow_mb` | `after-release` − `start` | **persistent growth**: what the call left behind for good. This is the gated leak figure |
| `afterGC_mb` | after four more `System/gc`s once the result is dropped | the absolute post-release used heap (context for the two figures above) |

`held_mb` is not precisely "result-retained size" — it is retention *while the
result is referenced*, which includes a cache or a leak the call created. The two
components are therefore recorded separately, and the leak line gates `grow_mb`,
not the absolute `afterGC_mb`: two cells can end at the same used heap while one
call left five times as much behind it, and comparing absolutes cannot see that.
A fixed leak, or one established before the 1,000-file cell, still hides from a
cross-N comparison; that is a boundary on MCP-OP-MEM-011, not a claim.

`held_mb` is the number that shows an operation sizing itself by the repository.
`afterGC_mb` staying flat is not evidence of boundedness; it only means the
operation did not leak.

**`held_mb` is gated across N** by `held-scales-with-n`. The 2.0 MiB slack is
derived from measurement, not taste: it is twice the full-match rename arm's
1.0 MiB held value at N=1,000 and ten times the largest 0.2 MiB jitter any
bounded arm showed — wide enough not to fire on noise, tight enough to catch the
measured 1.0 → 9.8 MiB growth that the first battery reported as `ok` because no
line looked at it. Before this line existed, the battery gated `afterGC_mb` (a
leak check) and never gated what the result itself costs to hold, so an
operation whose answer grew ~10× with the repository passed.

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

### The corpus arms

Every operation above is run against each corpus below. They are **separate
corpora, not extra files in the default tree**: an operation can be bounded over
10,000 ordinary files and unbounded over one pathological file, and mixing them
averages away exactly the case worth measuring.

| corpus | dir | shape |
|---|---|---|
| `default` | `100`, `1000`, `10000` | representative small/medium Clojure, mean ~4 KB/file |
| `cljc` | `cljc-100` | 100 `.cljc` files, every form behind `#?` / `#?@` reader conditionals |
| `giant` | `giant-1` | **one ~1.9 MiB source file** — the per-file case, not the per-tree one |
| `nested` | `nested-1` | **one adversarial file**: 300-deep nesting plus a 20,000-token literal, so node count dwarfs its 111 KB |

**Cross-N lines compare the default corpus only.** The adversarial arms exist at
one size each; comparing a 1.9 MiB single file against 10,000 ordinary ones
would be a statement about two different corpora, not about scaling. They still
carry every per-cell line: OOM, output parity, and the peak trend.

Two shapes Sol named are deliberately **not** arms, and stay boundaries on
MCP-OP-MEM-011:

- a **17 KiB-mean** profile matching this repository's real file-size
  distribution — easy to generate, but roughly 4× the weight of the
  10,000-file battery;
- **450 × 1.9 MiB** (~855 MiB of source). Expensive until aggregate admission
  exists; once it does, this becomes a *cheap* refusal arm, because parsing
  should never start.

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

The `cljc`, `giant` and `nested` corpora above are built by the same generator
and verified the same way, each in its own directory with its own manifest.

## The receipt

`$MEMBAT_ROOT/receipts/<timestamp>-battery.edn`, plus
`latest-battery.edn`. It carries the pass lines in force, the JVM version, the
tree manifests, every per-(op, N, phase) cell with its per-rep wall times, the
verdict with each failure's observed and limit values, every unmeasured line
with its reason, and a `:measurement-note` stating that `heap-used-peak-mb` is a
sampled process-wide peak rather than a post-GC delta or a per-operation figure.
