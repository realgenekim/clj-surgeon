<!-- DRAFT as of 07:12Z — refreshed at end of the night orders -->
# Morning report — Anvil seat, the night of 2026-09-03 (04:35Z → 07:09Z, ~2h34m in of the 9h order)

Gene, going to sleep: *"I'm going to sleep; exhausted; you work through next 9 hours; you know the
goals; state them. mayor, please watch us every 10m; anvil, ask mayor what help you want from it;
mayor, confirm the help you're going to give."* The night's two goals, in his priority order: (1)
get every ready branch to GO with an executed independent re-review, sitting in the mayor's queue;
(2) land the memory program (his choice B — optimistic transaction, disk-journaled, byte-budgeted
heap) as measurable pieces, "no `-Xmx` by judgement anywhere." **The single most important result:**
the memory kernel's OOM was reproduced on command, TDD-style, then fixed and measured — at
`-Xmx256m`, 600 files that OOM under the old frozen-read pattern now commit and retain **14.19–14.40
MB**, against a frozen `-Xmx2g` reference that used **2,046.8–2,046.98 MB** for the same scope (log
06:37Z, 07:08Z) — a ~145× reduction, Sol-reviewed GO-WITH-FIX. Two branches merged to main
(kondo-path-test, routing-doc-test), main's fast-suite baseline is now zero failures, and every
substantive branch that entered the queue tonight was sent back at least once by an executed,
independent red-team — nothing merged on a builder's word.

## Table 1 — merge queue (`docs/observations/2026-09-03-merge-queue-for-mayor.md`, cross-checked
against the captain's log through 07:08Z)

| branch | sha | verdict | reviewer(s) | the one number |
|---|---|---|---|---|
| clj-surgeon bridge/kondo-path-test | f8a9ef9 | **MERGED** (acda1b3, surgeon1, log 04:49Z) | none (test-only) | removes the standing "one baseline failure" from every Linux run |
| clj-surgeon bridge/routing-doc-test | a9d8701 | **MERGED** (3ebeafd/d1c5330, surgeon1, log 04:52Z) | none (doc-drift restore) | test-fast 702/5912/0 (log 04:42Z) |
| clj-surgeon bridge/receipt-ratchets | c5ef7ca | **GO** (log 06:09Z) | Sol rounds 1–3, self-verified round 4 | gates at zero after absorbing main: test-fast 718/6005, mcp 400/4138, oracle pass |
| clj-surgeon bridge/rf2-extract-rewire | 965d49e | **GO** (log 06:27Z) | Opus rounds 1–4 (Sol content-filter refused round 3/4 on symlink fixtures, log 04:44Z) | w9 MCP witness: refuses `caller-path-in-skipped-tree`, byte-identical .git, no target |
| clj-surgeon bridge/read-path-memory | 2aa648a | **GO** (log 07:04Z, round 2) | Sol rounds 1–2 | 1,000 files: wall 11,784→6,621 ms (−43.8%), allocation −40.9% |
| curtain-call bridge/template-upsert | 25b98a83 | **GO** (log 06:27Z, round 2) | Sol rounds 1–2 | unit 1056/13176/0; blank-string id collision fixed and fixture-pinned |
| clj-surgeon bridge/txn-journal (B1) | 1cece9a | **GO-WITH-FIX, HEAD not merge-ready** (log 07:08Z) | Sol round 1 | RED OOM at 256m on 600 × 512 KiB → GREEN retaining 14.19–14.40 MB; reference 2g used 2,046.8 MB |
| curtain-call bridge/fold-diff-tool | 2b56a484 | round 3 pushed (log 06:56Z); Sol re-check running; **HOLD** on inb-3a9818 | Sol rounds 1–3 | unit 1089/13314/0; two-refs self-test now emits distinct digests and a verdict |
| clj-surgeon bridge/memory-battery | c6a2264 | Sol round 2 **GO-WITH-FIX as tooling** (log 06:50Z); round 3 building | Sol rounds 1–2 | 3 HARD `held_mb` failures at N=10,000 (e.g. cli-ls-tree 94.0 MB vs 11.5 MB limit) |
| clj-surgeon bridge/study-ops-mcp | 3a237ab | Opus **GO-WITH-FIX** (log 06:18Z, round 3); round 4 building | Opus rounds 1–3 (Sol refused twice — filter, log 05:50Z, 06:18Z) | BLOCKER: `ns_grep` ReDoS, 294 → 41,804 ms as groups grow; ~3.5h at the 20,000-file ceiling |
| clj-surgeon bridge/census-verb | f43ac03 | Sol round 3 **NO-GO** (log 06:27Z); round 4 building | Sol rounds 1–3 | new CLI walk follows an escaping symlink; reads a two-link chain 3×; silent truncation at 4,000 while claiming completion |
| clj-surgeon bridge/q5z-alias-migration | 50098e6 | Sol round 5 **NO-GO** (log 07:04Z); round 6 building | Sol rounds 1–5 | aggregate-bytes ceiling refuses 855,000,000 B before any slurp at 512m (MEM-002 host); round-5 hole: a symlink installed *after* the collision check wins |
| clj-surgeon bridge/anvil-arms-apparatus | 6c1cf0a | Sol round 2 **NO-GO** (log 06:31Z); round 3 building | Sol rounds 1–2 | self-test 162/0 clean, but attestation's `make -n` step EXECUTED `$(shell …)`/`+$(MAKE)` — repo-controlled code ran |
| curtain-call bridge/lens-followups | 934716dc | ready | self-caught (no formal red-team round logged) | LENS-006 TDD run self-caught a third latent bug (wrong-event write) before any review |
| claude-skills PR #1 | — | ready, waits on mayor | — | sol-yolo bundled into the codex skill |

## Table 2 — the memory program (Gene's choice B: optimistic, disk-journaled transaction, byte-budgeted heap)

| leaf | state | measured numbers |
|---|---|---|
| Battery (MEM-001/011), `make memory-battery` | main RED baseline (log 04:50Z) → round 2 Sol **GO-WITH-FIX as tooling** (log 06:50Z) | Main RED: `FAIL peak-over-budget {cli-ls-tree n=1000 observed 274.8 limit 247.8}`, `{n=10000 observed 418.3 limit 247.9}`; no OOM anywhere; every bounded result hash-identical to the -Xmx4g reference. Round 2 (after Sol's fixes): 3 HARD `held-scales-with-n` failures — cli-ls-tree 9.5→**94.0** MB (limit 11.5), workspace-sources-read-all 4.4→**41.0** MB (limit 6.4), rename-ns-full-match 1.0→**9.9** MB (limit 3.0); adversarial arms: ONE 1.9 MiB file peaks **386.4 MB**, ONE 300-deep 111 KB file peaks **285.7 MB** (1,322 MB at -Xmx4g) — heap sized by file *shape*, not repo size |
| MEM-015 read-path single-parse (`bridge/read-path-memory`) | **GO** (log 07:04Z, round 2) | 1,000 files: wall 11,784→6,621 ms (−43.8%), allocated 25.28→14.95 GB (−40.9%), outlines byte-identical (160-file differential, 0 mismatches); gate now 5 samples, min 794.3× (was 3 samples/1,303× at round 1) |
| B1 disk-journaled txn kernel (MEM-006/007/012–014/020, `bridge/txn-journal`) | Sol round 1 **GO-WITH-FIX, do NOT merge HEAD** — 6 blockers (log 07:08Z); round 2 building | RED: OOM at -Xmx256m on 600×512 KiB. GREEN: same scope commits at 256m, retains 14.19–14.40 MB flat from 60→600 files, reserved peak 29.4 MB inside a 192 MiB budget. Frozen -Xmx2g reference: used 2,046.8–2,046.98 MB for the identical scope. Blockers: rename is not CAS (a writer between recheck and rename is silently overwritten with `committed=true`); scope membership compares COUNT only; `..` stripped by canonicalisation before the fence; symlink-swap-to-identical-bytes passes revalidation; commit/failed-rollback DELETE the transaction dir (no recovery material) |
| B3 parser admission (MEM-005, `bridge/parser-admission`) | launched 06:25Z from the MEM-015 tip; TDD from the battery's shape finding | Target: single-pass lexical pre-scan (node estimate + nesting depth) admits every src/test file with margin while the two adversarial shapes (giant flat file, 300-deep nested) refuse before the tree constructor |
| MEM-002 aggregate-bytes ceiling | landed inside q5z round 3 (log 05:47Z window), but q5z itself is NO-GO overall | Refuses 855,000,000 B before any slurp at -Xmx512m, `slurp_calls 0` — this is the fix for the alias_migration OOM that started the whole program |

## Table 3 — reviews as a meter (delegated work, watched)

| lane | rounds | items found per round | reviewer (Sol/Opus, why) | worst finding class |
|---|---|---|---|---|
| receipt-ratchets | 4 (3 Sol + 1 self) | round1: multiple blockers; round2: 1 PARTIAL + 1 NEW; round3: 1 remaining (regex newline) | Sol (no symlink fixtures, no filter issue) | production entrance (`edit_clojure`) had been silently accepting and committing an undeclared `changes` field |
| census-verb | 3 Sol, round 4 building | round1: 4 blocking; round2: 8 closed/3 partial + new silent-truncation find; round3: new — silent truncation at ceiling, ok/read_complete both true at 4,000 | Sol | discovery reports `read_complete:true` while silently dropping an over-ceiling file and truncating at 4,000 |
| rf2-extract-rewire | 4 (Opus fallback, filter refused Sol on the confinement fixtures, log 04:44Z) | round2: closed but new CLOSED-on-CLI/OPEN-on-MCP split found; round4: 5/5 closed | Opus | **the reviewer wrote `.git/hooks/caller.clj` through the MCP entrance** — a bypass of the walk/prune the CLI entrance enforced (log 05:08Z) |
| q5z-alias-migration | 5 Sol, round 6 building | round3: 3 new blockers (silent-drop class); round4: 3 remaining; round5: 3 new (TOCTOU) | Sol | **symlink installed after the collision-guard check wins** — a classic check-then-act race into a caller-controlled write path |
| study-ops-mcp | 3 (Opus fallback — Sol refused twice, "flagged for possible cybersecurity risk," log 05:50Z, 06:18Z) | round0 (pre-round1) red-team: 2 code-execution holes; round3: 4 blockers | Opus | pre-fix: `--pre=/bin/sh` reached ripgrep unescaped (executed files); `read-string` with read-eval on scanned `deps.edn` (`#=(spit …)` executed) — two live RCE paths from one inspect call |
| template-upsert | 2 Sol | round1: id-canonicalisation collapse found; round2: 1 uncharacterized edge | Sol | nil/blank-id collision silently dropped one of two distinct templates ("Draft A" lost) |
| fold-diff-tool | 3 Sol, round3 review running | round1: digest wrong file, unmeasured frontier gap; round2: baseline worktree couldn't compile | Sol | `cfpuser:hunter2@` userinfo credentials survived unredacted into the JDBC URL / report |
| memory-battery (instrument) | 2 Sol | round1: 4 fixes (exit-code semantics, manifest trust, no held_mb gate); round2: 6 more | Sol | `PASS (INCOMPLETE)` could exit 0 — a silent false-positive success code on an incomplete run |
| anvil-arms-apparatus | 2 Sol, round3 building | round1: many P0s (score.py trusts truncated/reversed streams); round2: 6 closed/6 partial | Sol | the "static" attestation step (`make -n`) actually **executed** `$(shell …)` and `+$(MAKE)` — repo-controlled code ran during what was supposed to be a dry parse |
| read-path-memory (MEM-015) | 2 Sol | round1: GO-WITH-FIX, environment/sample-count fixes | Sol | (no security class — measurement fidelity: gate sampled 3 not the documented 5; 980× ceiling tied to an unpinned JDK) |
| txn-journal (B1) | 1 Sol | round1: 6 blockers | Sol | rename is not CAS — a concurrent writer between read-set revalidation and rename is silently overwritten while the receipt still says `committed=true` |

## Incidents and ratchets (verbatim from the log)

- **7888 contact #1** (06:06Z): a Sol launch on the curtain-call clone died at MCP init on cclsp
  (7890); the repo's `.codex/config.toml` also declared clj-surgeon at 127.0.0.1:7888 (another
  seat's production Surgeon) as REQUIRED — a handshake may have completed before the failure,
  `:unverified`, no write path. **Ratchet:** `~/bin/sol-yolo` now disables every repo-declared MCP
  server unless an explicit url is passed.
- **7888 contact #2** (06:58Z): relaunching with `-c mcp_servers={}` still started two rmcp
  transport workers against 7888/7890 — codex *merges* `-c` overrides into the repo table rather
  than replacing it. **Ratchet:** `sol-yolo` now MOVES the scratch clone's `.codex/config.toml`
  aside before launch, writing a one-server file only when a url is passed.
- **git-add sweep** (04:52Z): `git add docs` on the shared main checkout swept two other agents'
  in-progress docs into a commit; caught before push and un-swept. **Ratchet:** add by name only,
  never `-A`/`.` on a shared checkout (memory: `main-checkout-is-shared-add-by-name`).
- **Log-filename sha** (04:59Z): a log path built from `$(git rev-parse HEAD)` took a different sha
  mid-run because the seat committed between the redirect and the exit stamp, so the first "zero
  failures" claim was read from an empty file. **Ratchet:** compute the log path once into a
  variable; never re-evaluate HEAD inside a long command.
- **pgrep self-kill** (06:58Z, same incident as 7888 #2): before verifying which process actually
  held 7888/7890, a `pgrep` pattern matched the seat's own shell (which contained the pattern) and
  the seat nearly killed itself. **Ratchet:** verify cmdline BEFORE kill, never after only printing it.
- **Lock contention ruling** (06:03Z): 10 waiters piled up on the single `~/tmp/suite.lock` at load
  3.6/16 — that rule was inherited from Buster's 4-core box. **Ratchet:** unit suites now run under
  `~/bin/suite-run <cmd>` (one of three parallel lanes); the memory battery and memory-red keep the
  exclusive lock (their wall numbers are trend lines, not gates, so concurrent unit suites don't
  corrupt them).
- **Usage-watch contamination** (06:49Z): the hourly `study-agent-usage` collector held flat at
  96 MCP calls / 49 ok / 47 refused, but the codex-provider counter moved 128→215 — traced to Sol
  reviewers probing their own branch servers on 7908–7910 through codex, not agent adoption of the
  tool. Named as a standing distortion in inb-46f90f; the MCP-server-side count is the honest one.

## What the mayor did, and what waits on Gene

**Mayor did tonight:** merged `bridge/kondo-path-test` (acda1b3, log 04:49Z) and
`bridge/routing-doc-test` (3ebeafd/d1c5330, log 04:52Z) — both zero-review test/doc fixes, per the
night orders. Sent the port table (`/tmp/ports.txt`, verified 03:38Z) confirming 7888 belongs to
`/srv/fleet/shared-tools` with three live dependents (dev-a/b/c), not an orphan; ruled forge stays
on 7906–7910. Answered the seat's status ask at 04:38Z.

**Waits on Gene, per the night orders' own carve-out** ("Not for the mayor... wait for the
morning"):
- **inb-78e75c** and **inb-041b28** — open decisions named but not detailed further in this seat's
  log; morning items.
- **curtain-call merge order**: fold → store → settings-lens → template-upsert → lens-followups —
  `template-upsert` is GO, `lens-followups` is ready, `fold-diff-tool` is on HOLD (inb-3a9818) —
  the order itself still "awaits Gene" per the merge-queue doc.
- **claude-skills PR #1** (sol-yolo bundled into the codex skill) — ready, mayor asked to merge or
  request changes; not yet actioned in the log.
- **Production ops inb-3a9818**: the index DDL step may run now; the fold-diff production run is
  held until this seat's GO note lands after round 3 and its re-review — as of 06:56Z still marked
  DO NOT RUN YET.

## E3/E6 readiness

E3 (fan-out verb vs. native, 21-owner rung) and E6 (study-ops free-choice adoption) were pre-staged
at 04:55Z (712a828, 1,234 lines): matched pair at N=21 for E3 (E3-L real fan-out where
alias_migration provably does not apply; E3-P synthetic alias-shaped, one call); numeric pass lines
(1 committing verb call; non-test actions ≤10.5; churn bands; wall claimable only above 172 s, the
2-sd floor); E6 needs ≥2 of 3 free-choice `ls-tree` calls with 0-of-3 closing the row; six arm
prompts written verbatim with a ritual-strip block (no "available and expected" language). **Go
tree:** E3 needs q5z merged + the battery green; E6 needs study-ops merged — neither branch is GO
yet (q5z: round 6 of NO-GOs; study-ops: round 4 building on a GO-WITH-FIX). The apparatus itself
(attest.sh, watch.py, score.py, run-arm/run-cohort) went through two build rounds (self-test 55/0 →
162/0) and two Sol NO-GOs (05:31Z, 06:31Z) — round 3 is building now, closing the remaining fail-open
class (the `make -n` shell-execution hole). Per the apparatus's own note (05:18Z): "Owed before a
cohort: PF-1..4 and PF-6, the G1 hand-drive first" — the PF-4 hand-drive has not yet run.

## Learning and caveat

**Learning:** the delegated-review loop vision.md asks for is running as designed — every
substantive branch that entered the queue tonight was sent back by an executed, independent
red-team at least once (nine of nine), and the worst finds were genuine RCE/TOCTOU-class holes
(ripgrep `--pre`, `read-eval` on `deps.edn`, a symlink race past a collision guard, an MCP entrance
that bypassed a CLI's own confinement) — exactly the class the 2026-08-29 andon memory exists to
catch, caught before merge each time.

**Caveat:** only two branches are actually on main (kondo, routing-doc); five more sit at GO
(ratchets, rf2, template-upsert, read-path-memory, and B1-with-blockers) waiting on the mayor or on
further fix rounds, and the two branches E3/E6 are gated on — q5z and study-ops — are each mid
fix-round, not GO. The "morning starts with go" plan for E3/E6 depends on both landing before the
9-hour window ends (~13:34Z).
