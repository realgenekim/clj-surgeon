GO-WITH-FIX

F1: `CHANGELOG.md` retained round 1’s false `:nothing-selected` description. I corrected it to the round 2 fail-closed contract.

Review evidence:

1. `probes.clj` reproduced all six retained reconstructions: three selected cases and three typed HOLD cases. Running every case in `fixed-point` mode returned exit 1; no non-list case exited 0 without tests. These match Astra’s retained reconstructions exactly; the original omitted Sol bodies remain unavailable for byte comparison.

2. `rg` found no Makefile or `ship` consumer. Exit 1 alone cannot distinguish HOLD from a crash; a future caller must require a parseable `results-<phase>.edn` containing `:status :hold-unmatched-files`. Direct `spit` writes are not atomic, so a mid-write kill may leave a missing/truncated receipt; that must classify as a crash and remain blocked.

3. `real-a.edn` inspection found `failure-report-test`, `quoted-var-refs-test`, and `operation-algebra-test` are conservative over-selections, not actual readers of `probe.clj`. Class: call-insensitive namespace content edges propagated through require reachability. The design states this propagation and intentional overapproximation.

4. Direct selector probes returned `:selected` with nonempty namespaces for:

   - `no-test-can-depend.edn`
   - `battery-ledger.edn`
   - `battery-namespace-walls.edn`
   - `portability-controls.edn`

   None can be masked as `:nothing-selected`.

5. `git diff -U0 c6f40787..ab3febad -- test/ docs/intent/` showed exactly three removed assertions, each replaced by its stricter HOLD/empty-diff equivalent; no coverage assertion was dropped.

Verification:

- JVM: `14 tests / 242 assertions`, 0 failures/errors.
- Babashka: `14 tests / 242 assertions`, 0 failures/errors.
- No `make test` was run.
- `git diff --check`: exit 0.
- `git diff HEAD --numstat`: `3 2 CHANGELOG.md`.

<!-- SHIP-FIX-BLOCK
CANDIDATE: c6096277aabb01a67f59921e02e450945577d5cf
FINDINGS: F1
REPAIR: Correct the changelog to document HOLD for unmatched nonempty diffs and reserve nothing-selected for empty diffs.
PRODUCER: OpenAI Codex <codex@openai.com> model=gpt-5 session=01a09f83-039d-71f1-a736-f555990de5e3
PATCH-MANIFEST:
  CHANGELOG.md +3 -2
SHIP-FIX-BLOCK -->

> END RECEIPT (fence-run): worktree HEAD at review exit = c6096277aabb01a67f59921e02e450945577d5cf = fenced sha.
