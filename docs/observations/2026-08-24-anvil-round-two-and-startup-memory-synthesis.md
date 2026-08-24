# Anvil round two: exact outcomes, zero compact-route adoption

**Implementation:** `5c118e185d3accaa836d7cb953c5b44fbdbc129c`
**Caller:** three fresh Codex Sol/high seats, one replicate each, no skill hint
**Task:** replace only the `:finish` status `:done` with `:complete`, preserving
the attached comment, audit payload, and every unrelated byte

## Decision summary

Two claims now have direct evidence:

1. The compact `edits` entrance is mechanically safe and byte-exact when it is
   invoked. Local tests and live self-hosting prove redundant top-level
   `expect` normalization, exact per-edit guards, stale refusal, atomic commit,
   receipt, inverse, and no whole-file formatting.
2. The entrance is not yet discoverable enough. In the matched no-skill Anvil
   canary, zero of three fresh agents chose it. All three eventually produced
   the exact target bytes, but every route needed an EOF repair after its first
   mutation.

The next product experiment should therefore change affordance, not mutation
semantics: expose the same compiler and transaction kernel through one small,
unmistakably named `edit_clojure` MCP tool, then compare it locally with the
current `apply_clojure_changes` entrance. Do not add another executor.

## Round-two scoreboard

| Seat | Experiment CWD | First read | First mutation route | Final exact | Compact `edits` | Repair-free | Evidence |
|---|---|---|---|---:|---:|---:|---|
| dev-a | `/srv/fleet/dev-a/clj-surgeon-one-shot-canary-r2-20260823` | native `rg` | native file edit | yes | no | no | Initial diff also removed the final blank line; agent restored it with two additional file changes. |
| dev-b | `/srv/fleet/dev-b/clj-surgeon-one-shot-canary-r2-20260823` | native `rg` | native file edit | yes | no | no | Initial diff also removed the final blank line; agent first called it pre-existing, then compared `HEAD` bytes and restored it. |
| dev-c | `/srv/fleet/dev-c/clj-surgeon-one-shot-canary-r2-20260823` | native `rg`, then `inspect_clojure` | direct `changes`, not compact `edits` | yes | no | no | Guarded transaction verified, but whole-file formatting removed the final blank line; several native repair attempts and a final Perl append restored it. |

All three began from SHA-256
`56a156f76bad4330ab79d6671462893374749277d1c56422e87ef19748fe81ca`
and ended at the identical exact-target SHA-256
`ac1d08366599cce00e7c6fe2440e43e83aeb8af647018bfca31f89231faf5d32`.
Their terminal processes exited zero. Mechanical final correctness is 3/3;
compact-route discovery, compact one-shot success, and repair-free first
mutation are each 0/3.

The behavior matters more than the agents' final prose. Each reported that the
first mutation declaration matched, which is true at the local replacement
guard level, but none satisfied the product definition of one-shot exactness.
The aggregate diff exposed the wider byte change and drove repair work.

## Comparison with round one

Round one at `14beaf5` produced one compact-route discovery in three, but that
caller was refused twice for a redundant aggregate `expect`; the eventual
compact/direct writes also suffered formatter drift. Round two removed both
mechanical defects for compact callers, yet produced no compact caller.

This sample is too small to claim that the revised description reduced
discovery. It is sufficient to reject the claim that description-only changes
make the route reliably self-revealing. A fresh caller still sees a broad
`apply_clojure_changes` tool, a familiar native editor, and no obvious reason
to search the former for a two-keystroke operation.

## Why a thin `edit_clojure` surface is the leading hypothesis

The desired gesture is a small, closed vocabulary:

```json
{
  "workspace_root": "/repo",
  "edits": [{
    "file": "src/bench/pair_view.clj",
    "within": {"form": "route-event"},
    "from": ":done",
    "to": ":complete"
  }]
}
```

The current implementation already compiles that request into the proven
direct-change contract. A new MCP name can be only a discoverability adapter:
the same schema branch, validator/compiler, verifier, transaction, receipts,
undo, and telemetry remain authoritative. This is not a new architecture or a
second mutation path.

The alternative is instruction-only adoption: require every repository skill
to say “use compact `edits`.” That may work in configured repositories, but it
does not meet the church-organ standard where the instrument itself advertises
the right key. The two variants should be measured rather than debated.

## Smallest local experiment

Before another Anvil run:

1. Add a thin `edit_clojure` MCP adapter backed by the existing compact compiler.
2. Prove schema parity, error parity, receipt parity, stale refusal, and exact
   byte parity against `apply_clojure_changes {edits: ...}`.
3. Hot-reload the live server without changing its PID.
4. Run three fresh local callers on the exact same no-skill task:
   current overloaded tool, thin named tool, and ordinary native control.
5. Admit the new name only if the named-tool caller chooses it in its first
   mutation, changes only the intended bytes, and needs no repair. Then run the
   formal 10/10 local gate before another remote batch.

## Startup-memory synthesis

Three independent Anvil artifacts now bound the startup problem:

- Static A, originally produced in CWD
  `/srv/fleet/dev-a/clj-surgeon-one-shot-canary-20260823`, commit `db2d37c`:
  normal startup eagerly loads the full CIDER nREPL before MCP/Jetty; clean
  startup does not scan source or initialize cclsp/LSP.
- Static B, produced in CWD
  `/srv/fleet/dev-b/clj-surgeon-one-shot-canary-20260823`, commit `78c454c`:
  startup crosses a 36-namespace, 14,965-line, 822-form eager tool-kernel
  frontier, then retains CIDER before schema/SDK/Jetty construction. First-use
  inspect/apply/reload must be included so lazy loading cannot hide a moved
  peak.
- Measurement/storyboard, produced in CWD
  `/srv/fleet/dev-c/clj-surgeon-one-shot-canary-20260823`, commit `8a439a4`:
  isolated nREPL-off 2 GiB and 1 GiB profiles both reached readiness in seven
  seconds at essentially the same peak RSS, about 580 MiB.

The 1 GiB measurement used the real MCP/Jetty/tool stack but explicitly
disabled embedded nREPL. It therefore proves that a 2 GiB heap is not intrinsic
to clj-surgeon's core server. It does not reproduce or invalidate the Mac
production-profile OOM.

The leading causal sequence is:

```text
eager 36-namespace compile/class load
  -> retained CIDER handler + nREPL server
  -> schema/JSON/SDK construction
  -> Jetty/Reactor construction at the highest live floor
  -> readiness and later GC settlement
```

Measured Anvil details reinforce the transient-overlap model:

- 2 GiB: 581 MiB peak RSS, 153 MiB used heap at readiness, 476 MiB NMT
  committed after readiness.
- 1 GiB: 579 MiB peak RSS, 48 MiB used heap at readiness, 446 MiB NMT committed
  after readiness.
- Halving `Xmx` did not materially change peak RSS or readiness time in the
  nREPL-off profile.
- Allocation samples are dominated by class/namespace loading, reflection,
  byte arrays, Clojure collections/lazy sequences, compiler analysis, ASM, and
  locks—not an already-populated workspace or source cache.

This demotes SQLite/DuckDB as the first startup fix. Disk-backed state may
later help bounded caches and steady retention, but the cold path begins with
empty workspace/basis/job stores. There is no large startup dataset to dump.
The first targets are eager code loading and retained development machinery.

## Recommended startup-memory experiment

Run one production-matched local pair, not another broad heap sweep:

1. Exact current managed profile at 2 GiB as the control.
2. Exact same profile at 1.5 GiB.
3. If 1.5 GiB fails, repeat only that cap with embedded CIDER/nREPL disabled.
4. Carry successful arms through first inspect, first apply, and `mcp-reload`.

Record phase markers after namespace load, telemetry, nREPL, tool init, MCP
specification construction, Jetty readiness, first inspect, first apply, and
first reload. For each phase distinguish allocated bytes, peak live/used heap
plus native committed memory, and post-full-GC retained heap. A lower readiness
peak that reappears on first use is relocation, not improvement.

If nREPL subtraction is decisive, make CIDER an explicit or separately leased
development attachment while preserving a small hot-reload control channel.
Then lazy-resolve the unused `clojure-mcp.core` fallback and split tool
contracts from the heavy implementation kernel. Only after 1 GiB is green
under cold, warm, and first-use gates should 512 MiB become the target.

## Imported evidence

- `docs/observations/2026-08-23-anvil-startup-memory-static-a.md`
- `docs/observations/2026-08-23-anvil-startup-memory-static-b.md`
- `docs/observations/2026-08-23-anvil-startup-memory-measurement-plan.md`
- `docs/observations/2026-08-23-anvil-startup-memory-storyboard.html`
- `docs/observations/evidence/startup-memory/`
