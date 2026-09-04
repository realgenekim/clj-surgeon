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
