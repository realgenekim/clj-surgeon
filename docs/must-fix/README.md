# Must-Fix Register

This directory contains unresolved findings that must be closed or explicitly
converted into documented non-goals before `audit/xray-maximality` can ship.
“Fix” may mean implementation, a stronger test, corrected packaging, durable
evidence, or a narrower honest claim. It never means silently deleting the
finding.

Correctness and unchanged source are gates. Wall clock is the primary
efficiency metric. Existing tests may not be weakened or removed.

## P0 — Release blockers

1. [Archive benchmark evidence](001-archive-benchmark-evidence.md)
2. [Confirm bounded Claude Fable and Opus use](002-bounded-claude-fable-opus.md)
3. [Unify Claude and Codex skill packaging](003-unify-agent-skill-packaging.md) — closed 2026-08-06
4. [Isolate installed artifacts from the active checkout](004-isolate-installed-artifacts.md) — closed 2026-08-06

## P1 — Public contract and safety

5. [Decide the `:evidence` contract](005-xray-evidence-contract.md)
6. [Make benchmark result directories single-writer](006-benchmark-single-writer-isolation.md)
7. [Make platform selection file-aware or CLJC-only](007-platform-selector-file-awareness.md)
8. [State and test the sandbox termination boundary](008-sandbox-termination-contract.md) — closed 2026-08-06
9. [State canonicalization depth](009-canonicalization-depth-contract.md)
10. [Keep quoted structural symbols searchable](010-quote-aware-source-guard.md)

## P2 — Release truth and product boundary

11. [Correct performance and README claims](011-performance-and-readme-claims.md)
12. [State honest boundaries versus native tools](012-honest-product-boundaries.md)

The full state, candidate table, evidence roots, and takeover prompt are in
[the X-Ray maximality handoff](../plans/xray-maximality-handoff.md).
