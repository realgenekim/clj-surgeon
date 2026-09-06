---
name: clj-surgeon
description: >-
  Use for advanced clj-surgeon workflows: semantic preparation, computed preview, extraction or movement, CLI fallback, MCP recovery, and troubleshooting. Do not invoke for ordinary inspect_clojure or edit_clojure calls; always-loaded routing and tool schemas cover them.
---

# Strictly better, or native
Route automatically ONLY when the task matches a witnessed contract below and the
complete receipt path is available; otherwise native. Strictly better means better
EXPECTED complete verified wall on an eligible task with a bounded escape — not a
per-invocation guarantee. The always-loaded routing plate carries one complete call
for each class; act from the plate, and open this file only for the unusual case.
- READ structure: one `inspect_clojure` batch, outline once, `match` per file, root
  `expect` — 20 files/59 sites/0.33 s; ~150x fewer tokens (house rule, older). The
  claim is tokens and determinism, NOT wall: served discovery was neutral (J).
- FAN-OUT, known old/new intent across many NAMED owners: one
  `apply_clojure_changes` — 1.75x, cohort I.
- ALIAS migration, known old/new alias intent and eligible scope (no proof profile
  or pre-enumerated match set): one `alias_migration` — 1.38x, 3/3 pairs and six
  controls (fixed NO-COLLISION fixture; collisions NOT witnessed; 1.5x missed).
NATIVE by rule everywhere else: single-feature work (1.03x and 1.83x AS LONG), small
edits, extraction (105.5 s vs native 98.0 s median), and anything outside a route's
own preconditions — fan-out additionally needs a complete bounded match set and a
valid proof profile. If a precondition is unavailable, native IS the fast path.

## One repair, then native

On one clear argument or refusal error, repair once from the refusal text. Then take
the documented native fallback, record the exact refusal, and count zero
tool-committed sites. A second refusal, a stale-source refusal, or an unavailable
verb leaves the route; never loop.

## Receipts retire only the proof they contain

Optimize complete verified task time — orientation, refusals, retries, emission and
proof. Server runtime was about 2% of every measured wall, so the route around the
tool is the cost; never subtract tool runtime from a wall. A receipt retires exactly
the proof it names over its exact snapshot, and nothing else. BYTE-LEVEL, do not
re-verify: `written bytes read back and verified`, `verification_complete`. NOT
semantic proof: `caller proof · structural candidates only; not semantic
completeness`, and `caller proof unavailable` — for those, run the repository's own
tests ONCE and stop. No receipt retires user-required review, independent
acceptance, or any proof that was not performed. Stop on `:error`; keep one coherent
operation per commit.

## Meter and kill switch

The usage collector reports, per routed class, first-attempt success, refusal rate,
fallback rate, and full request-to-verified wall; discovery, schema repair and any
second read are charged to the route. Each class also carries a periodic
preregistered native pair. Stop routing a class and re-run a preregistered pair when
the weekly real-work meter shows it losing its native control, its fallback or
refusal rate rising, or its telemetry unknown.

## Syntax trip-wire

Every CLI call is `:op <name>` plus key-value pairs; positional guesses produce
`Unknown op`. Put any nontrivial plan on stdin with `:spec-file -`. Smoke test:

```bash
clj-surgeon :op :ls :file src/my/ns.clj
```

## References

The unusual case only; never a prerequisite for a routed call. Read
[CLI fallback](references/cli-fallback.md) for full syntax and receipts,
[advanced CLI operations](references/advanced-operations.md) for extraction,
moves, renames, or CLJC, and [advanced MCP routes](references/mcp-advanced.md)
only for explicit development-service work. Do not reopen a reference already consumed.
