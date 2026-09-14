# Diff-impact file-content edges

The selection oracle must observe file inputs as well as namespace requirements.
Incidents inb-f85401 (splice vocabulary at df0c9e1c) and inb-1b7f3c
(test tooling at 00566756..8aedb65e) define this round's acceptance boundary.
The [design and requirements](../intent/diff-impact/design.md) own the contract.

First commit the registered class oracle red, retaining the old CLI output.
Then implement bounded file edges and an explicit empty selection. Fixture Git
diffs exercise discovery and the actual script entry point; pure source/data
matrices exercise path classification, closure, and negative cases. A test-only
environment predicate replacement permits isolated fixtures inside the caller's
temporary root without claiming launcher environment parity. Production still
uses the gated launcher. No fixture test loads its subject test namespaces.

Verify exact selected sets, edge explanations, empty-result EDN and process
exit; retain the old require-graph self-tests. Replay the two historical diffs
against their own snapshots using the new selection implementation. Format,
lint through ~/bin/clj-kondo, run affected namespaces with -Xmx1024m, regenerate
the census and inspect additions. Do not run make test or test-battery; do not
change either tracked battery observation file. Commit and push only
fable/impact-entrance. Report correctness and bounds, with no speed claim.

## Round 2

Commit DIFF-IMPACT-004 strengthened intent and assertions together, red first.
Add six inventory probes, all four non-list modes, mixed matched/unmatched,
empty diff, disconnected dependent and cyclic src closure witnesses. Implement
HOLD and src content discovery, replay both historical lists and preserve the
protected ledgers. All phases are approved in advance by the round brief.
