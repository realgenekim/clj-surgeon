# Whole namespace split

Design of record: Astra's 2026-09-07 opinion, ratified by Gene's two-batch build
brief. This implements a single-source partition compiler, not a sequence of
extractions. The model owns the mapping and policies; the compiler owns their
mechanical closure over one captured source map. Native editing remains the default.

Batch 1 repairs workspace-relative namespace identity and the helper planner's
caller classification handoff (Andon inb-731473). Batch 2 exposes a pure mapping
to relations to future-source compiler through MCP and CLI, sharing the existing
transaction, proof and guarded inverse. Analysis is its nonmutating projection.

Inputs identify workspace, source file/lib, destination file/lib/forms/alias policy,
promotion authorization, retirement, bounded roots, constraints and verification.
The final graph is built simultaneously. Unmapped/duplicate owners, cycles, drift,
undecided promotions and namespace/path disagreement refuse before publishing.
Declarations are forward-reference evidence, not duplicate definition identities.

Witness matrix: nested src ancestor and configured roots; English-word docstring
bystander; three-way split, forward reference, private crossing, alias collision,
test caller; each typed refusal leaves bytes unchanged; failed publish/proof restores
the snapshot using existing rollback. Real acceptance uses the 141-owner views
fixture and its four independent oracles. Receipts distinguish write proof from
executed behavioral checks and report unknown coverage honestly.

Non-goals: architectural advice, new evaluator/reference engine/transaction system,
site-table inputs, per-destination transactions, dynamic caller guarantees, batteries,
or performance superiority inferred from compiler time alone. Gates and measured
wall are recorded in /var/tmp/forge/plan2/split-build-report.md.

The implemented leaf is [namespace-split-design](../intent/helper-extraction/namespace-split-design.md),
with NS-SPLIT-001..015 and direct @spec witnesses. The CLI and MCP share the pure
compiler and existing extraction inverse; the common synchronous proof code was
moved into dependency-light leaves so Babashka does not load the MCP/nREPL facade.
