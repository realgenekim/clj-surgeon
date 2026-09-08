# Astra r10 repair report

Recorded 2026-09-08T10:39:04.358905+00:00. Worktree `/home/forge/src/clj-surgeon-split`, branch
`astra/namespace-split`, reviewed base `827a751add48fbb8da364fbbf74544e1cf485d99`.
No push or actual merge. Final gate/commit/merge results are appended below.

## Sol findings reproduced and repaired

1. **Affected battery and landing gap.** The whole original alias namespace ran
   in the owned worktree nREPL: **137 tests / 3,164 assertions; 135 failures,
   nine errors** (`/var/tmp/forge/r10fix/alias-red.log`). This exceeded Sol's
   sampled failures: retention directories, configured-path attack fixtures,
   rollback assertions and the reachable refusal census had also drifted.
   The repaired whole namespace passes **137 / 3,237**, zero failures/errors
   (`alias-green2.log`). No test was removed. The renamed summary witness is
   also renamed in the independent operation witness catalog.

   Absolute detail/retirement paths are consumed directly. Retention fixtures
   share the actual external directory and count only detail documents, leaving
   inverse receipts outside that count. External fixture artifacts are cleaned.
   The two new reachable containment refusal kinds are pinned by exact membership.
   Historical configured-path/race probes inject the chosen directory at the
   retained `commit!` boundary, keeping real guards, writes and rollback active;
   public-entry tests continue to prove server configuration cannot override
   external storage. This is an explicit boundary amendment, not a stubbed success.

   **`make alias-migration-test` runs the whole alias namespace and the new
   publication namespace; `landing-gate` calls it unconditionally.** The tests
   remain classified as battery because they launch child processes. The lane
   membership witness was red before wiring (`lane-red.log`) and green afterward
   (`lane-green.log`); it resolves both `make test` and `make landing-gate`.

   **Installed launcher mismatch:** `/home/forge/bin/land` actually bypassed
   `landing-gate`, contrary to the Makefile's comment. Its local hardcoded gate
   list is replaced with `~/bin/suite-run make test`, retaining the existing
   suite-pool serialization. This retains recovery-battery execution and
   reaches the canonical landing gate. `bash -n` passes. The launcher was never
   invoked: no merge or push was performed. Its before/after bytes and diff are
   retained as `land.before`, `land.after`, `land.patch`; this local seat change is
   outside the repository commit and must accompany the branch's landing.

2. **Contradictory linked intent.** Active MCP-OP-ALIAS-019/020/026/044/045 are
   amended in place: external absolute detail/inverse/retirement paths; measured
   Git exception paths may grow with the file set; edit payloads remain external.
   MCP-OP-ALIAS-054/056 retain their commit-boundary safety contracts. HLD, LLD,
   EARS, EDN registry, witnesses, operation catalog and current receipt/lane prose
   agree. New ALIAS-MIGRATION-003 requires the affected batteries in the landing
   gate. The structural audit is `{:ok true, :violations []}`; the former green
   structural audit alone was not semantic consistency evidence.

3. **Per-verb behavioral red evidence.** The exact same new witness file ran on
   detached **3d55fa34** and the repaired worktree in separate owned nREPLs.
   **All 13 new witnesses fail for publication placement on the baseline:**
   **19 runtime tests / 155 assertions, 22 failures, zero errors**. Green:
   **19 / 156**, zero failures/errors. The 19 runtime tests include six existing
   behavior/proof/undo fixtures invoked by the 13 new tests. The extra green
   assertion belongs to an amended existing fixture, not a changed observation.
   No missing namespace, setup exception, static census, or green-only fixture is
   counted as the requested red evidence.

| Publication boundary | Behavioral probe |
|---|---|
| helper extraction | real detail and inverse publication from the happy corpus |
| compact edit | real guarded editor gesture and undo |
| general changes | real six-edit fixture and undo |
| prepared apply | retained semantic basis, real write and inverse publication |
| legacy extract! | real extraction and guarded undo |
| typist | real candidate proof, commit and guarded undo; candidate transport fixture |
| standalone require_change | real synchronous proof, detail/inverse, undo and original bytes |
| namespace_split | identical frozen analysis, real transaction, synchronous proof and undo |
| legacy change! | actual CLI registry handler, source mutation and guarded undo |
| advisory lock | actual file-lock acquisition; no bookkeeping added to workspace state dir |
| cold proof | two actual job publications; absolute paths and retained distinct contents |
| admit focused report | actual child runner writes the report; workspace report stays absent |
| default transaction journal | begin/read/pin/stage/commit; external journal and retained preimage |

Raw evidence: `/var/tmp/forge/r10fix/verbs-red-final.log`, `verbs-green-final.log`,
`per-verb-evidence.json`. Witness source SHA256:
`33308562fdd6d944a3256faa83ba0f5c66181fe2f267eff732a7edfd7bdd3484`.
The baseline contains only the overlaid new test file; production remains at
3d55fa34. Only worktree-owned nREPLs were used; shared services were not reloaded.

## Verification and limits

- Standard Clojure Style formatted the new namespace and complete changed
  top-level owners in existing files. Untouched owners were preserved. Formatter
  commands/results are retained in `format-owners.clj`, `format-result.log` and
  `formatter-owners.log`.
- Serialized `~/bin/clj-kondo`: seven changed Clojure files, zero errors, zero
  warnings, four informational findings (`lint-final.json`).
- First cold `make test`: affected batteries **156 / 3,393**, zero failures/errors;
  five fast-lane bookkeeping failures then exposed the new namespace/intent count
  pins and the renamed witness catalog entry. Those fail-first gate results are
  retained in `make-test.log`; focused repaired pins pass **3 tests / 87 assertions**.
- Direct intent audit is clean (`intent-final.log`). `git diff --check` is clean.
- The staged repair snapshot dry-merges cleanly against fetched origin/MCP/main
  `5e7cc7f7453ca20018dfb71c7b786ac5b067250b`, a descendant of 1ab53e0e.
  `snapshot-dry-merge.log` records its temporary review commit and merged tree;
  final committed-SHA verification follows below.
- No production mutation algorithm changed in this repair. Row-2 source layout
  and N-arm layout are unchanged. No new comparative timing, routing admission,
  or row-3 admission claim is made. The original Row-2 performance claim remains
  conditional on an authorized landing of the tested repair.

The repository's working-tree `skill.md` was applied. No installed
`linked-intent-dev/SKILL.md` was found; the available
`/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md` supplied the matching
registry/witness practice, alongside the repository's HLD → LLD → EARS → tests →
code instructions. Gene's r10 brief authorizes this bounded repair through commit.

Baseline cleanup 2026-09-08T10:41:36.859497+00:00: stopped the owned nREPL at 33569 and removed the detached baseline worktree after freezing identical witness hashes and red logs. The primary worktree nREPL remains available until final verification finishes.

## Final accepted result — 2026-09-08T10:43:52.768614+00:00

- **Commit:** `398679fa05bbe42a4aeda36a2442341af149aec0` on `astra/namespace-split`.
- **Author and committer:** `forge-anvil <forge-anvil@anvil>`.
- **Trailer:** `Co-Authored-By: Gene Kim <genek@itrevolution.com>`.
- **`make test` exit 0:** affected batteries **156 tests / 3,393 assertions**;
  JVM fast/integration **814 / 10,377**; Babashka **888 / 7,858**. All have zero
  failures and errors. Isolation, required oracles, recovery battery and hygiene
  pass. Full accepted log: `/var/tmp/forge/r10fix/make-test-final.log`.
- **Final intent audit:** `{:ok true, :violations []}` over the committed tree.
- **Final serialized lint:** seven Clojure files, zero errors/warnings, four infos.
- **Final dry merge:** `git merge-tree --write-tree origin/MCP/main HEAD`, exit 0,
  against freshly fetched `1ffbac2ab30e688e68dc75b90e1b6e20b870b4d6`. Ancestor check confirms it contains
  `1ab53e0e`. Merged tree: `704ae6a53c4bc05e8da4210c17c2c8d3702ab0ec`. No actual merge was performed.
- **Working tree clean; no push.** Only the local installed launcher repair lives
  outside the repository commit. Its final SHA256 is
  `c6131fabd851614504bf95228f1a374c0655bcbaa98dc03bfe6a919868839037`;
  syntax check passes and the gate runs through the existing suite-pool wrapper.
- Both task-owned nREPLs have been stopped; the detached baseline fixture is
  removed. Evidence logs, baseline commit identity, identical witness source hash,
  launcher before/after diff and the final Git receipt remain under
  `/var/tmp/forge/r10fix/`. No shared runtime was restarted or stopped.

Completed within the 45-minute brief (work began 2026-09-08 10:13:10 UTC).
No unresolved Sol r10 finding remains in this repair; landing itself is still
for the authorized operator, and this report does not claim a pushed build.

Final merge attribution was rechecked with literal commit arguments: the shared origin/MCP/main ref advanced while the first receipt was being assembled. The pinned check against 1ffbac2ab30e688e68dc75b90e1b6e20b870b4d6 exits 0 and yields 704ae6a53c4bc05e8da4210c17c2c8d3702ab0ec. The final Git JSON and dry-merge log record these exact arguments.
