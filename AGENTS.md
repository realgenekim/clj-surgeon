# Repository Agent Instructions

Read and follow [CLAUDE.md](CLAUDE.md) before making changes. It is the
canonical repository instruction file for all coding agents, regardless of
vendor or runtime.

Its Clojure routing and Surgeon-owned semantic escalation rules apply before native
Read, Edit, grep, sed, or cat touches existing source. Load the working-tree
skill before acting.

Use the hottest capable entrance: prefer the persistent `inspect_clojure` and
`apply_clojure_changes` MCP tools. Use `~/bin/clj-surgeon` only when MCP is
unavailable, the operation is not exposed there, or the CLI itself is under
test.

For non-trivial feature work, `CLAUDE.md` requires the design, planning,
testing, documentation, and verification standards that must be satisfied
before the work is complete. Do not treat those linked documents as optional
background reading.

## LID

- Mode: Scoped
- Version: 1.3.0

## LID Scope

Paths in scope:

- `src/clj_surgeon/mcp_*.clj`
- `src/clj_surgeon/mcp_server.clj`
- `test/clj_surgeon/mcp_*_test.clj`
- `docs/high-level-design.md`
- `docs/intent/mcp-operation-contract/**`
- `docs/intent/operation-algebra/**`
- `src/clj_surgeon/operation_algebra.clj`
- `src/clj_surgeon/intent_transaction.clj`
- `src/clj_surgeon/core.clj`
- `test/clj_surgeon/operation_algebra_test.clj`
- `test/clj_surgeon/intent_transaction_test.clj`
- `test/clj_surgeon/cli_dispatch_test.clj`
- `Makefile`

## The acid test — the ultimate measure of performance (ratified Gene, 2026-08-30)

Before designing, running, or interpreting ANY experiment, screen, or benchmark, read
[.claude/skills/designing-experiments/SKILL.md](.claude/skills/designing-experiments/SKILL.md)
— the earned method: empirical-usage grounding, preregistration with kill criteria,
sub-ceiling controls, and the Anvil acid test as the only proof rung.

**A performance improvement is proven by exactly one instrument: a matched serial Anvil
comparison at an exact product commit — the Surgeon route versus a matched native route on a
REAL historical decision, both arms completing the same task, scored by the same semantic
scorer.** The README headline table is the canonical form. Nothing else is proof.

Evidence hierarchy, strongest first: (1) Anvil matched pairs on real historical decisions —
proof; report per-arm wall seconds, emitted output tokens, action count, one-shot rate, and
speedup at a named product commit. (2) Local matched pairs — replication evidence only.
(3) Synthetic screens — option-buying signals; they may kill a claim cheaply, never mint a
performance claim. (4) Priced token/byte deltas — projections until an acid-test pair
converts them.

**Every performance claim names its task class** (change count, file count, reference
count): the advantage inverts with scale. Measured map as of 2026-08-30: native wins the
small class (two files / three changes; 5-reference single-file rename); Surgeon wins
4.9–9.7× on the large mechanical class (15-form extraction / 63 callers; 51-edit nine-file
chord). The crossover is unmapped; routing guidance must not claim unmapped territory.
Regression gate for any installed change: re-run the historical benchmark decisions at the
new commit. "Faster" without an acid-test receipt is relayed as `projected`, never
`measured`.

**The skill text is the actuator.** When an acid test moves the map, the routing guidance
agents actually read — working-tree `skill.md` and the installed agent-routing text — is
rewritten in the SAME change to articulate the class boundary in decision form ("use
Surgeon when …, use native when …", with the measured counts). The map lives in the skill;
the receipts live in the README and `docs/observations/`. A measured boundary that never
reaches the skill text changes no agent's behavior and therefore bought nothing.


## Linked-Intent Development (MANDATORY)

Consult the `linked-intent-dev` skill for changes in the scoped paths. Walk
each change through the arrow of intent in one direction:

```text
HLD → LLDs → EARS → Tests → Code
```

- New features, refactors, and bug fixes use the full six-phase workflow.
- Stop after each phase for user review.
- Write design and requirement documents so they carry current intent without
  relying on conversation history.
- Within one leaf segment, cascade approved intent through requirements, tests,
  and code. Pause before crossing into another segment.

### Navigation

| What you need | Where to look |
|---|---|
| High-level design | `docs/high-level-design.md` |
| MCP operation-contract design | `docs/intent/mcp-operation-contract/` |
| EARS specifications | Beside the owning design as `*-specs.md` |
| Decision documents | `docs/decisions/` or the owning segment's `decisions/` |

### Code annotations

Use `@spec` comments to connect each specified behavior to its implementation
entry point and its direct witness tests. Do not annotate every helper.
