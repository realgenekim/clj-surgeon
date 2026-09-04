# Second landing: `bridge/integration-2026-09-03-mem003` — the MEM-005 / MEM-003 / MEM-011 contradiction, and streaming ls-tree

Opened 2026-09-04 00:50 UTC by forge@anvil, from the first integration branch's tip.

Base, proven before any edit:

```
git fetch origin bridge/integration-2026-09-03 bridge/streaming-ls-tree
  -> 2556a3838dd8df30b84c6b19d31fd9f4baf1af14   (integration)
  -> 95b0881bc6d96b61c8ae6b7b2213991d02cbbdf5   (streaming-ls-tree)
worktree-add ... bridge/integration-2026-09-03-mem003 2556a38
  -> HEAD 2556a3838dd8df30b84c6b19d31fd9f4baf1af14   (equal)
```

Nothing is pushed from here. Commits only; the seat pushes.

## The contradiction, and the ruling

Three ratified rows met on ONE field and no arrangement satisfied all three as
written:

| row | requires |
|---|---|
| `MCP-OP-MEM-005` | the scan's own cost `scan_ms` in the ls-tree receipt, UNCONDITIONALLY |
| `MCP-OP-MEM-011` | the operation's result to reproduce an unbounded reference hash exactly |
| `MCP-OP-MEM-003` | two scans of an unchanged tree byte-identical; a complete result carrying no receipt |

`scan_ms` is a wall-clock reading. The first integration branch measured the
collision from two directions — twelve `reference-mismatch` battery FAIL lines
on `cli-ls-tree` (`nondeterministic:4`: four output hashes over five reps of
ONE operation on ONE unchanged corpus), and nine test failures when
streaming-ls-tree was composed — and refused rather than amend a GO'd lane's
gate on its own authority.

**THE RULING (the seat, on the record): a measured wall-clock field can never
live inside a parity hash.** Keep MEM-005's meter — `scan_ms` is still measured
and still reported unconditionally — and move every MEASURED field off the
hashed channel. The parity hash covers the deterministic result and the
deterministic resource facts; `bytes_scanned` is a count and stays IN it,
because a denominator that moved IS a regression a parity line must catch.

The rejected alternative was narrowing MEM-005 to publish the meter only on
refused scans. It fails on MEM-005's own argument: a gauge wired to the rare
branch is a gauge nobody sees move.

## The partition

`clj-surgeon.measured` is the one place that says where a measured field may
live. One well-known key, `:measured`, marks a measured block anywhere in any
result; `hashed-channel` projects a result onto the channel every determinism,
parity and byte-identity row takes as its subject; `text-measured-prefix` puts
the same partition in the bytes for the text encodings, which have no keys.

```
:resources {:bytes_scanned 111183 :measured {:scan_ms 44.081}}
```

The projection is STRUCTURE-SHARING by construction — a sub-value carrying no
measured field comes back `identical?` — because the battery hashes while the
result is still referenced and the heap sampler is running, and a projection
that copied a 10,000-record result would move the numbers the battery exists to
measure.

Wired at four sites and no others: `parse-admission/meter-resources` builds the
block; `core/format-ls-tree-text` and the streaming `text-encoder` print the
wall-clock half on its own labelled line; `memory-battery-runner/hash-result`
digests `(measured/hashed-channel result)`.

## Red → green

```
bb -e "(clojure.test/run-tests 'clj-surgeon.measured-channel-test)"
  RED   -> Ran 3 tests containing 12 assertions. 5 failures, 0 errors.
  GREEN -> Ran 3 tests containing 12 assertions. 0 failures, 0 errors.
```

Three of the five reds are DETERMINISTIC — they do not depend on two scans
taking different amounts of time — so the red could not pass by luck, and the
witness cannot be satisfied by DELETING the meter: it asserts in the same
breath that a positive `scan_ms` is still published.

The battery is deliberately unreachable from `make test` and `make mcp-test`,
so the witness cannot call the runner. It names the runner's subject, and
`memory-battery-test/the-battery-hashes-the-hashed-channel-and-not-the-raw-result`
reads the runner's actual hashing site to bind the two. A witness that only
agrees with itself proves nothing about the battery.

## The merge

`git merge --no-ff 95b0881`, using the union resolutions the first
integration branch recorded, plus the step it began and stopped at: the MEM-005
admission meter threaded through MEM-003's streaming encoders. Before that
threading the streaming path emitted NO receipt at all for an ordinary scan —
the composition silently reverted MEM-005's unconditional meter.

The merge commit reproduces exactly the nine predicted failures (0 errors); the
commit after it narrows MEM-003's rows to the hashed channel and they go green.

## Duplicate ownership of MEM-001 / MEM-011

Already half-resolved on the base branch and now stated in BOTH directions.
`docs/intent/memory-boundedness/` is the SOLE home: MEM-001 and MEM-011 are the
battery's own subjects — the per-operation heap meter and the battery as a
release gate. `docs/intent/memory/` keeps both ids as `[D]` DEFERRED rows, which
is the mechanism that exists for exactly this: its kernel sources carry
`@spec MCP-OP-MEM-001` / `-011` markers for the receipt ceiling and the
attributable reserved peak, and a deferred row keeps those markers traceable
while stating no requirement and demanding no witness. The kernel is a WITNESS
to two clauses, not their author. Previously each file stated the ownership
one-way, so a reader arriving at memory-boundedness could not tell that markers
for its ids lived in sources it does not own.

## Note on the intent-audit baseline

The brief's "253 specs" is the first integrator's figure, measured at
`aadbdbc`. At this branch's base `2556a38` the audit already reads **256**,
verified against a clean `git archive` of that commit:

```
BASE ok= true specs= 256 violations= 0
```

After the merge registers MEM-003 it reads 257 / 0.

## Gates at the final sha

Every ran-line below was executed on this branch's tip.

| gate | result |
|---|---|
| `suite-run bb test/run_all.clj` | `Ran 867 tests containing 6999 assertions. 0 failures, 0 errors.` (baseline 814/6724/0) |
| `suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 601 tests containing 6326 assertions. 0 failures, 0 errors.` (baseline 597/6305/0) |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` |
| `audit-current-repository` | `ok= true specs= 257 violations= 0` (base 2556a38 reads 256/0) |
| `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` |
| `make memory-battery-self-test` | `generate_tree root-marker self-test: ok` / `generate_tree self-test: ok` / `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` |
| `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` |
| `make memory-red-kernel` (flock) | `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.` |
| `make memory-battery` (ONCE, flock, fresh `MEMBAT_ROOT=/home/forge/tmp/membat-integ2`) | **FAIL (INCOMPLETE) exit 1** — read on |

The battery ran under a compliant root; `MEMBAT_ALLOW_ANY_ROOT=1` was not
needed and not used. Its reference was rebuilt for this code by an explicit
`make memory-battery-reference` (the default `MEMBAT_REFERENCE=require`
refuses to build one as a side effect).

### The battery, before and after

**The twelve `reference-mismatch` lines are GONE. Output parity on
`cli-ls-tree` is green on every corpus and both phases.** That was the whole
subject of this branch: `nondeterministic:4` was a wall-clock reading inside
the digest, and the digest now takes the hashed channel.

**`cli-ls-tree`'s `held-scales-with-n` line is also gone**, and not by
amendment — MEM-003's merge closed it, which is what that lane was for. Held
heap at 10,000 files is now the same 9.7 MB it is at 1,000, because the result
is bounded at the 1,000-record ceiling:

```
cli-ls-tree  default   1000  fresh   held 9.7 MB
cli-ls-tree  default  10000  fresh   held 9.7 MB      (was 95.6 against a limit of 11.7)
```

Two `held-scales-with-n` FAILs remain, both pre-existing and both named as such
in the brief:

```
FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 10.0, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 40.9, :limit 6.4, :small-n-observed 4.4, :slack-mb 2.0}
```

Nine TREND lines and four UNMEASURED reserved-peak lines are reported and never
gated; the UNMEASURED lines are why the verdict is INCOMPLETE rather than FAIL,
and they are unchanged from the previous run.

Full run preserved at
`/home/forge/tmp/membat-integ2/receipts/20260904T010432.444069392Z-battery.edn`.

---

# Round 2 — closing the Sol review (NO-GO of 2026-09-04)

Sol reviewed the tip `0a38e3d` and returned NO-GO on two independent blockers.
Every numbered section was reproduced before anything was changed; the
receipts below are this seat's own runs, not the review's, except where they
are explicitly quoted as the review's.

**THE INVARIANT, in one sentence: every field a clock produced enters a public
result only inside a `measured` block, at any depth, and never beside one.**

## §1 (BLOCKING) — the partition was a site fix, not a rule

Reproduced verbatim at `0a38e3d`, with the reviewer's own command:

```
{:public-result {:ok true, :receipt {:stable :fact}, :elapsed_ms 2.5},
 :hashed-channel {:ok true, :receipt {:stable :fact}, :elapsed_ms 2.5},
 :elapsed-survives-hash true}
```

The first landing wired the partition at four sites and made the ruling true
at those four. `hashed-channel` removes a block whose key is literally
`:measured`; the shared MCP operation finalizer attached its wall-clock
reading as a top-level `:elapsed_ms`. The projection was blind to it, so
measured data reached the hash subject by a second route. Sol named five such
routes and one namespace collision.

**The fix is one boundary, not five sites.** `mcp-operation/invoke!` is the
single point every public MCP result passes through. `finalize-result` now
runs `measured/partition-measured` over the whole result — relocating every
declared measured field name into the `:measured` block at its OWN level, at
any depth — and then attaches the authoritative request clock there. An
operation may carry a clock reading in whatever shape suits it internally;
what it cannot do is PUBLISH one outside the partition. That closes
`mcp_operation`, `mcp_change_buffer`, `mcp_hot_verify`, `mcp_cold_verify` and
`txn_journal`'s commit window at one place, and it closes a sixth route the
review did not reach: `recovery/recover!` writes a fingerprinted receipt to
disk, so its three clock readings are partitioned at their own site.

`partition-measured` is structure-sharing like `hashed-channel` (a sub-value
with nothing to relocate comes back `identical?`) and never descends into a
`:measured` block.

The wire says what the receipts say: every output schema now requires a
`measured` object carrying `elapsed_ms` instead of a top-level `elapsed_ms`.
A caller reading the JSON can see which fields came from a clock. The intent
registry was amended rather than left silently disagreeing — RESULT-001/002/
003, TIME-003/004, SCHEMA-001 and ASYNC-001/002/004 now name the partition —
and two rows were added: **MCP-OP-TIME-005** (the invariant) and
**MCP-OP-EXIT-001** (below).

RED `ea7c6cf` → GREEN `419d3d9`.

```
RED    Ran 6 tests containing 16 assertions. 9 failures, 0 errors.
GREEN  Ran 6 tests containing 16 assertions. 0 failures, 0 errors.
```

The reviewer's command at `419d3d9`:

```
{:public-result {:ok true, :receipt {:stable :fact},
                 :measured {:elapsed_ms 2.5}},
 :hashed-channel {:ok true, :receipt {:stable :fact}},
 :elapsed-survives-hash false}
```

### The witness is a rule, not an assertion

`test/clj_surgeon/measured_invariant_test.clj` carries four:

1. **A SOURCE SCAN of every clock read in `src/`.** All 34 sites are
   enumerated and classified `:receipt` (the value is published, so it must
   ride the partition) or `:control` (a lease deadline, an expiry sweep, a
   retention cutoff, a transaction id, a poll loop, the battery harness's own
   row). A clock read nobody classified fails this file rather than shipping.
   Identity is `[file, enclosing form]` and never a line number: an inventory
   that must be re-blessed on unrelated edits gets re-blessed reflexively and
   stops being a ratchet. The classified sites are:

   `:receipt` — `mcp_change_buffer/run-process!`, `mcp_cold_verify/run-job!`,
   `mcp_hot_verify/verify!`, `mcp_inspect_tool/elapsed-ms`,
   `mcp_inspect_tool/execute-inspect-in-context!`, `mcp_operation/invoke!`,
   `mcp_process/run-bounded!`, `mcp_tool/elapsed-ms`,
   `mcp_tool/execute-request-in-context!`, `mcp_tool/timed`,
   `parse_admission/refusal`, `txn_journal/publish-one!`,
   `recovery/elapsed-ms`, `recovery/recover!`.

   `:control` — `ls_tree_snapshot/{prune!,touch!,write-snapshot!}`,
   `mcp_change_buffer/now-ms`, `mcp_cold_verify/now-ms`,
   `mcp_combinable_transaction/new-registry`,
   `mcp_prepared_confirmation/new-registry`,
   `mcp_process/{call-with-analyzer-contract-mission,
   claim-analyzer-mission-launch!,record-analyzer-mission-exit!}`,
   `mcp_telemetry/prune!`, `memory_battery_runner/measure-once`,
   `txn_journal/{legacy-lock-dead?,mark-break-linked!,new-txid,
   prune-broken-locks!,retained-transactions,stamp-broken-at!,
   stamp-tombstone!}`, `workspace_onboarding/await-cclsp-workspace!`.

2. **No measured field name published outside the partition**, at any depth,
   in a result the finalizer publishes — asserted over a result carrying all
   five families the scan found (`elapsed_ms`, `job_elapsed_ms`,
   `inspection_elapsed_ms`, `scan_ms`, `commit-window/max-ns`), and asserted
   in the same breath that `bytes_scanned`, a COUNT, stays in the hashed
   channel and that a deterministic sibling is not dragged along with its
   measured neighbour.

3. **The parity hash is stable across two publications whose clocks tick
   differently** (1.0 ms against 71.0 ms), with an anti-vacuity assertion that
   the two clocks really did differ.

4. **`System/exit` only inside a `-main`** (see §3).

### The partition pays for itself immediately

`mcp_prepared_request_test`'s byte-identity check neutralized the clock by
assoc'ing zeros over two field names. That stops neutralizing anything the
moment a third measured field appears — the exact defect class this branch
exists for, sitting in a witness. It reads `measured/hashed-channel` now: one
projection everybody shares, which is what the first landing said and did not
yet enforce.

## §3 — the "no `System/exit`" claim was false. The decision: an allow-list of `-main`, and two removals.

The claim as written was not true and could not be made true by wording: a
CLI entrypoint legitimately exits, and twelve calls live in `src/`. Ten were
already inside a `-main`. **The other two were not, and they are gone rather
than allow-listed** — `run-fresh-scan`'s empty-scan branch and
`run-ls-tree`'s missing-`:dir` branch, both library functions.

This was not a documentation choice. An empty scan or a missing `:dir` killed
the JVM of whatever called the op — in production the MCP server, and in the
suite `core_discovery_test`, which had to shell out to a subprocess to test
the op at all (`inb-eca3b1`, and the reviewer quoted that comment). Both are
typed refusals now. The CLI still exits 1, because `-main` exits 1 on any
result carrying `:error`, and the two existing empty-scan tests — which
assert exit 1 AND the message on stdout — pass unchanged.

The EARS text states the allow-list: **MCP-OP-EXIT-001**.

## §7 — `memory-red` was RED for the reviewer and GREEN for the builder. Both readings were honest.

The review's receipt at `0a38e3d`:

```
FAIL giant 128m: admission scan under 50 ms {:wall-ms 104, :scan-ms 52}
memory-red: 5/6 assertions held (expect=green) — FAIL
```

Reproduced on fresh roots under `flock`, same code, same host, three
consecutive runs: **`scan-ms` 13 (6/6), 14 (6/6), 60 (5/6 — the reviewer's
failure, reproduced exactly).**

**Attribution: environment — and specifically a gate that could not tell
environment from regression.** Two of the six assertions read a wall clock
ONCE on a 16-core box shared with other JVM lanes and compared that single
sample against a fixed 50 ms bound. Scheduler contention decided the verdict.

**The threshold is not relaxed.** A gate that went red on a reviewer's run is
not one to soften in the same round, and relaxing it would destroy what it is
for. The MEASUREMENT is repeated instead: each timing cell runs `--reps`
(default 3) probes and asserts on the BEST reading, because noise only ever
adds time. Every rep is printed, so a genuine regression (all reps slow)
still reads differently from contention (one rep slow):

```
host — 16 cores, load 7.07 7.12 7.51 9/1940 3732036
PASS   giant 128m: admission scan under 50 ms
       {:best-scan-ms 28, :scan-ms [65 47 28], :wall-ms [116 110 105]}
memory-red: 6/6 assertions held (expect=green)
```

The host's cores and load are printed with every run, because these are
wall-clock assertions on a shared box and the box's state is part of every
reading — neither the reviewer's run nor the builder's recorded it, which is
why the disagreement could not be settled from the receipts. Commit
`e073262`.

## §5 — the battery, under a root the guard accepts

The review could not run it: its safety constraint put the fixture under
`/tmp`, and the generator hard-codes `/home/forge/tmp` as its only allowed
prefix, so the corpus was never created and the claimed lines were correctly
NOT presented as reproduced. Run here ONCE, under `flock`, with a fresh
`MEMBAT_ROOT=/home/forge/tmp/membat-mem003r2`, never
`MEMBAT_ALLOW_ANY_ROOT`. The reference was built by an explicit
`make memory-battery-reference` first, because the default
`MEMBAT_REFERENCE=require` refuses to launch that 4g JVM as a side effect.

```
verdict: FAIL (INCOMPLETE)   exit 1
```

**Zero `reference-mismatch` lines. `grep -c reference-mismatch` over the whole
run returns 0.** Output parity on `cli-ls-tree` is green on every corpus and
both phases — that was the whole subject of this branch, and it holds under a
root the guard accepts, with a reference this seat built explicitly for this
code.

`cli-ls-tree`'s held heap is flat across a 10x corpus, so its
`held-scales-with-n` line stays gone:

```
cli-ls-tree  default   1000  fresh   held 9.5 MB      1000 files,  4,045,282 bytes
cli-ls-tree  default  10000  fresh   held 9.6 MB     10000 files, 40,472,773 bytes
```

**The two remaining `held-scales-with-n` FAILs are PRE-EXISTING at the base
`2556a38`, and the first landing's own record proves it** — this run's figures
sit within measurement noise of the ones that record quotes:

```
this run   FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :observed  9.9, :limit 3.0, :small-n-observed 1.0}
2556a38    FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :observed  9.8, :limit 3.0, :small-n-observed 1.0}
this run   FAIL held-scales-with-n {:op :workspace-sources-read-all, :observed 41.0, :limit 6.5, :small-n-observed 4.5}
2556a38    FAIL held-scales-with-n {:op :workspace-sources-read-all, :observed 40.9, :limit 6.5, :small-n-observed 4.5}
```

(`docs/observations/2026-09-03-integration-branch.md:338-342`, which also
records the third — `cli-ls-tree` at 95.6 against 11.7 — that this branch's
merge closed.)

Ten TREND lines and four UNMEASURED `reserved-peak-over-budget` lines are
reported and never gated. The UNMEASURED lines are why the verdict is
INCOMPLETE rather than FAIL, and they are unchanged: no operation on this
branch reports an attributable reserved peak, which the reviewer independently
confirmed is an honest non-pass rather than a false green.

Full run preserved at
`/home/forge/tmp/membat-mem003r2/receipts/20260904T023919.743046461Z-battery.edn`;
its reference at `.../20260904T023250.367116763Z-reference.edn`.

## §1's converse — the collision the partition had in the other direction

Parser admission published `{:parse-nodes, :parse-depth}` — two DETERMINISTIC
counts — under `:measured`, so the projector dropped them from the hashed
channel and a refused file's node count could never be a parity subject. The
key is `:shape` now, with a witness that the counts survive `hashed-channel`.
A key that names a partition has to mean one thing in both directions, or it
is a convention rather than a rule. Commit `079e567`.

## Gates at the round-2 tip

| gate | result |
|---|---|
| `suite-run bb test/run_all.clj` | `Ran 873 tests containing 7016 assertions. 0 failures, 0 errors.` |
| `suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 601 tests containing 6359 assertions. 0 failures, 0 errors.` |
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` |
| `audit-current-repository` | `ok= true specs= 259 violations= 0` (257 at `0a38e3d`; +MCP-OP-TIME-005, +MCP-OP-EXIT-001) |
| `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` |
| `make memory-battery-self-test` | `generate_tree self-test: ok` / `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` |
| `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` — now min-of-3, all reps printed |
| `make memory-red-kernel` (flock) | `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.` (`heap-used-peak` 253.3 / 254.2 MB at Xmx 256) |
| `make memory-battery` (ONCE, flock, fresh `MEMBAT_ROOT=/home/forge/tmp/membat-mem003r2`) | **FAIL (INCOMPLETE) exit 1** — zero `reference-mismatch`; two pre-existing `held-scales-with-n`; four UNMEASURED |

No Surgeon MCP server was started. `MEMBAT_ALLOW_ANY_ROOT` was not used.

## What is NOT closed

- The four `UNMEASURED reserved-peak-over-budget` lines. The battery cannot go
  green until an operation reports an attributable reserved peak; that is
  MEM-001's subject and belongs to the memory-boundedness lane, not to this
  branch.
- The two pre-existing `held-scales-with-n` FAILs on `rename-ns-plan-full-match`
  and `workspace-sources-read-all`. Named as known at `2556a38`; untouched here.
