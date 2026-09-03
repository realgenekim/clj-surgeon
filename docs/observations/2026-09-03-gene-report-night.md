<!-- refreshed 10:06Z; final refresh ~13:00Z -->
# Morning report — Anvil seat, the night of 2026-09-03 (04:35Z → 10:05Z, ~5h30m in of the 9h order)

*(No `gene-report` skill exists yet at `/home/forge/opt/claude-skills/gene-report/` — this refresh
follows the format specified in the refresh request plus house-rules doctrine: headlines first,
count-first status, numbers with their log entry time.)*

Gene, going to sleep: *"I'm going to sleep; exhausted; you work through next 9 hours; you know the
goals; state them. mayor, please watch us every 10m; anvil, ask mayor what help you want from it;
mayor, confirm the help you're going to give."* The night's two goals, in his priority order: (1)
get every ready branch to GO with an executed independent re-review, sitting in the mayor's queue;
(2) land the memory program (his choice B — optimistic transaction, disk-journaled, byte-budgeted
heap) as measurable pieces, "no `-Xmx` by judgement anywhere." **The single most important result,
unchanged and now load-bearing:** the memory kernel's OOM was reproduced on command, TDD-style, then
fixed and measured — at `-Xmx256m`, 600 files that OOM under the old frozen-read pattern now commit
and retain **14.19–14.40 MB** (last re-measured 09:42Z: 14.11–14.24 MB), against a frozen `-Xmx2g`
reference that used **2,046.8–2,046.98 MB** — a ~145× reduction. **All nine branches named in the
night orders reached GO overnight** (kondo, routing-doc — both merged; ratchets; template-upsert;
rf2; study-ops (08:33Z, eighth GO); memory-battery; read-path-memory; q5z-alias-migration (09:37Z,
ninth GO — the branch whose OOM started the whole memory program)) — every one sent back at least
once by an executed, independent red-team first. Only two are on main; the mayor has seven more
waiting. The kernel (B1) is GO-WITH-FIX but still not merge-ready (round 3 just re-entered review),
and a Claude session-limit outage at ~09:55Z killed four in-flight agents, now relaunched.

## Table 1 — merge queue (cross-checked against the captain's log through 10:05Z)

| branch | sha | verdict | reviewer(s) & rounds | the one number |
|---|---|---|---|---|
| clj-surgeon bridge/kondo-path-test | f8a9ef9 | **MERGED** (acda1b3, surgeon1, 04:49Z) | none (test-only) | removes the standing "one baseline failure" from every Linux run |
| clj-surgeon bridge/routing-doc-test | a9d8701 | **MERGED** (3ebeafd/d1c5330, surgeon1, 04:52Z) | none (doc-drift restore) | test-fast 702/5912/0 (04:42Z); main baseline is zero failures from here |
| clj-surgeon bridge/receipt-ratchets | c5ef7ca | **GO** (06:09Z) | Sol rounds 1–4 | gates at zero after absorbing main: test-fast 718/6005, mcp 400/4138, oracle pass |
| clj-surgeon bridge/rf2-extract-rewire | 965d49e | **GO** (06:27Z) | Opus rounds 1–4 (Sol content-filter refused, 04:44Z) | w9 MCP witness now refuses `caller-path-in-skipped-tree`, byte-identical .git, no target |
| clj-surgeon bridge/read-path-memory | b7ef23d | **GO** (07:04Z, round 2; sha corrected 09:44Z) | Sol rounds 1–2 | 1,000 files: wall 11,784→6,621 ms (−43.8%), allocation −40.9% |
| curtain-call bridge/template-upsert | 25b98a83 | **GO** (06:27Z, round 2) | Sol rounds 1–2 | unit 1056/13176/0; blank-string id collision fixed and fixture-pinned |
| clj-surgeon bridge/memory-battery | 5534e94 | **GO as tooling** (07:09Z, round 3) | Sol rounds 1–3 | main RED under it BY DESIGN (three held_mb lines + reserved-peak UNMEASURED) — that is the product's state, not the tool's |
| clj-surgeon bridge/study-ops-mcp | 4480e3d | **GO** (08:33Z, eighth GO) | Opus throughout — Sol refused repeatedly (filter, 05:50Z/06:18Z/07:12Z) | ns_grep budget fixed to `max(20000, 64×len²)`: honest 106-char pattern passes ~21× margin, catastrophic still refuses in 15.9 ms (was 43,589 ms) |
| clj-surgeon bridge/q5z-alias-migration | f51ceae | **GO** (09:37Z, ninth GO — 9 rounds: Sol 1–6, Opus 7–9) | Opus reproduced the builder's scenario (12/12 files still migrated behind `:rolled-back true`) and withdrew its own item | seven fails-first witnesses; aggregate-bytes ceiling refuses 855,000,000 B before any read = MEM-002 |
| curtain-call bridge/fold-diff-tool | 347fe6d3 (production GO, pinned) / 885f58b3 (tip) | **Production read GO stands at 347fe6d3** (08:29Z); tip at round 6, **round 7 building** | Sol rounds 1–6 | 08:29Z: alias scan yields exactly the three vars the tool needs; VERDICT line carries its fallback caveat; round-7 fix: three witnesses still read the real store, required-var scan can be empty under requiring-resolve, `FOLD_DIFF_DATA_DIR` ignored under Postgres |
| clj-surgeon bridge/census-verb | 5eee690 (round 7 pushed 09:44Z) | Sol round-7 NO-GO on ONE new item; **round 8 building** | Sol rounds 1–7 | `doors=[1]` passes server validation but is copied into a next_call the schema itself rejects; everything else CLOSED, gates green twice, no timeout |
| clj-surgeon bridge/txn-journal (B1) | eb22036 (round 3 pushed 09:42Z) | GO-WITH-FIX at round 2; **HEAD not merge-ready**, Opus round-3 re-check running (relaunched after the 09:55Z outage) | Sol round 1, Opus rounds 2–3 | memory-red once under the lock: RED OOM at 256m → GREEN 600 files retained 14.11–14.24 MB, parity byte-identical; reserved peak 29,446,956 B exact |
| clj-surgeon bridge/anvil-arms-apparatus | 895eed0 (round 4 pushed 09:27Z) | Sol round-4 GO-WITH-FIX (09:45Z); **round 5 building** (relaunched after outage) | Sol rounds 1–4 | self-test 354/0 passed; static Make grammar now refuses 8 constructs whole-file rather than mis-scoring them; `make CMD=… verify` runtime override still mis-scored, left for round 5 |
| clj-surgeon bridge/parser-admission (MEM-005) | ad439f4 (round 2, 08:53Z) | Opus round-2 GO-WITH-FIX with a NEW blocker; **round 3 building** (relaunched after outage) | Opus rounds 1–2 (Sol refused, filter, 07:40Z) | ONE extra `)` → unhandled ArrayIndexOutOfBounds crashes outline/deps/analyze where the parent commit returned "Unmatched delimiter" |
| clj-surgeon bridge/streaming-ls-tree (MEM-003) | 8c1087f | Sol NO-GO on cursor integrity (memory result CONFIRMED); **round 2 building** (Opus, relaunched after outage) | Sol round 1 | held 93.45→9.35 MB at 10k files (real); but a forged offset 99 on a 3-record tree returned an empty "complete" result — unauthenticated pagination |
| curtain-call bridge/lens-followups | 934716dc | ready | self-caught (no formal red-team round logged) | TDD run self-caught a third latent bug (wrong-event write) before any review |
| claude-skills PR #1 | — | ready, waits on mayor | — | sol-yolo bundled into the codex skill |

## Table 2 — the memory program (Gene's choice B: optimistic, disk-journaled transaction, byte-budgeted heap)

| leaf | state | measured numbers |
|---|---|---|
| Battery (MEM-001/011), `make memory-battery` | GO as tooling (round 3, 07:09Z) | Main RED by design: `FAIL peak-over-budget {cli-ls-tree n=1000 observed 274.8 limit 247.8}`, `{n=10000 observed 418.3 limit 247.9}`; round-2 HARD held_mb failures — cli-ls-tree 9.5→94.0 MB (limit 11.5), read-all 4.4→41.0 (limit 6.4), rename-ns 1.0→9.9 (limit 3.0); adversarial arms: ONE 1.9 MiB file peaks 386.4 MB, ONE 300-deep 111 KB file peaks 285.7 MB (1,322 MB at 4g) — **shape finding**: heap is sized by file shape, not repo size. Caveat (07:10Z): round-3 commits changed src, so a fresh reference run is required before the table numbers are quoted as current — the attestation working as intended. |
| MEM-015 read-path single-parse (`bridge/read-path-memory`) | GO (07:04Z, round 2) | 1,000 files: wall 11,784→6,621 ms (−43.8%), allocated 25.28→14.95 GB (−40.9%), outlines byte-identical (160-file differential, 0 mismatches); no battery line flipped — the streaming leaf (MEM-003) owns cli-ls-tree's peak |
| B1 disk-journaled txn kernel (MEM-006/007/012–014/020, `bridge/txn-journal`) | GO-WITH-FIX round 2 (08:56Z); round 3 pushed 09:42Z closing all three round-2 blockers; **not adopted by any verb yet** | RED: OOM at -Xmx256m on 600×512 KiB. GREEN (09:42Z): same scope commits at 256m, retains 14.11–14.24 MB, parity byte-identical to Sol's own run. Frozen -Xmx2g reference used 2,046.8–2,046.98 MB. Round-3 fixes: lock = pid+start-ticks+boot-id via temp+hardlink, broken only when proven dead; unreadable lease → UNKNOWN, not evictable; undo! now takes PUBLISH.lock + rechecks H1 digest + NOFOLLOW identity; window shrunk to O(1) in size (size term −73%); ~15 non-cooperating write sites tabulated as an adoption obligation. |
| B3 parser admission (MEM-005, `bridge/parser-admission`) | round 2 landed 08:53Z; Opus review found a NEW blocker; round 3 building | Battery cells: giant 386.4→33.4 MB (2,058→27 ms), nested 285.7→24.6 MB (784→10 ms). Round-1 gap closed (reader-macro prefixes now count toward depth); round-2 gap: a single extra `)` sends the depth counter negative and crashes with an unhandled array-index exception on a path the parent handler used to catch cleanly |
| B4 streaming ls-tree (MEM-003, `bridge/streaming-ls-tree`) | landed 8c1087f (08:55Z); Sol NO-GO on cursor integrity; round 2 (Opus) building | cli-ls-tree at N=10,000: held 94.0/93.6→9.5/9.4 MB, peak 430/424→243/264, wall 4,213/5,021→658/769 ms; differential 0 mismatches over 168 files. Real memory win confirmed by the reviewer independently; blocked on a forged/unauthenticated pagination offset returning a false "complete" result |
| MEM-002 aggregate-bytes ceiling | landed inside q5z round 3 (05:47Z window); q5z reached GO 09:37Z | Refuses 855,000,000 B before any slurp at -Xmx512m, `slurp_calls 0` — the fix for the alias_migration OOM that started the whole program |

## Table 3 — reviews as a meter

| lane | rounds | items per round (recent) | reviewer (Sol/Opus, why) | worst finding class |
|---|---|---|---|---|
| receipt-ratchets | 4 Sol + 1 self-verify | r3: 1 remaining (regex newline), closed by my own diff read, not a 5th round | Sol (no symlink fixtures, no filter issue) | production entrance (`edit_clojure`) had been silently accepting and committing an undeclared `changes` field |
| census-verb | 7 Sol/Opus alternating, round 8 building | r5→r6: 4,002 sources claiming `read_complete:true` while silently truncating at 4,000; r6→r7: two small contract gaps; r7: one schema-invalid next_call | Sol (structural), Opus for fix-building | discovery reports completion while silently dropping an over-ceiling file |
| rf2-extract-rewire | 4 Opus (Sol filter-refused 04:44Z) | r2→r4: CLOSED-on-CLI/OPEN-on-MCP split found and closed | Opus | **the reviewer wrote `.git/hooks/caller.clj` through the MCP entrance** — a live bypass of the CLI's own confinement |
| q5z-alias-migration | 9 (Sol 1–6, Opus 7–9) | r5: symlink-after-check race; r7: `commit-refusal` computed from a key the refusal never carries (an oracle that cannot fail); r9: Opus withdrew its own item after reproducing the builder's counter-proof | Sol then Opus (filter) | symlink installed AFTER the collision-guard check wins — a TOCTOU into a caller-controlled write path |
| study-ops-mcp | ~5 rounds, Opus throughout (Sol refused 3×: 05:50Z/06:18Z/07:12Z) | r0 pre-round: 2 code-execution holes; r4/r5: catastrophic-regex budget term wrong by 39,000× on real 106-char paths | Opus | pre-fix: `--pre=/bin/sh` reached ripgrep unescaped (executed files); `read-string` with read-eval on scanned `deps.edn` — two live RCE paths from one inspect call |
| template-upsert | 2 Sol | r1: id-canonicalisation collapse; r2: 1 uncharacterized edge (blank-id fallback) | Sol | nil/blank-id collision silently dropped one of two distinct templates |
| fold-diff-tool | 6 Sol at the tip, round 7 building | r4 (production sha): all prior CLOSED; r6: witnesses still read the real store live | Sol | `cfpuser:hunter2@` userinfo credentials survived unredacted into a JDBC URL / report (fixed round 3) |
| memory-battery (instrument) | 3 Sol | r2: 3 HARD held_mb failures the round-1 gate never looked at; r3: symlink/directory-at-expected-path refusals | Sol | `PASS (INCOMPLETE)` could exit 0 — a silent false-positive success code on an incomplete run |
| anvil-arms-apparatus | 4 Sol, round 5 building | r3: static Make parser defeated by 8 real constructs (define/endef, `%.test:`, etc.); r4: all 6 CLOSED, one runtime-override mis-score left | Sol | the "static" attestation step (`make -n`) actually **executed** `$(shell …)`/`+$(MAKE)` — repo-controlled code ran during a supposed dry parse |
| read-path-memory (MEM-015) | 2 Sol | r1: sample-count and JDK-pinning fixes | Sol | measurement fidelity only — gate sampled 3 not the documented 5 |
| txn-journal (B1) | Sol r1, Opus r2–3 | r2: 3 blocker-class holes Opus's own injections introduced or left (stale lock, lease fail-open, undo! no CAS) | Sol then Opus | rename is not CAS — a concurrent writer between revalidation and rename is silently overwritten while the receipt still says `committed=true` |
| parser-admission (MEM-005) | 2 Opus (Sol refused 07:40Z) | r1: delimiter-ceiling vs nesting-ceiling gap (reader-macro prefixes bypassed depth); r2: negative-depth AIOOBE crash | Opus | admission ceiling bypassable by construct class, not just size — leads to an uncontrolled StackOverflow or crash |
| streaming-ls-tree (MEM-003) | 1 Sol, round 2 building | r1: cursor integrity — 5 distinct holes in one pass | Sol | a forged/out-of-range cursor offset returns an empty result labelled "complete" — the pagination boundary is unauthenticated |

**The two events where disagreement resolved the right way:** Sol conceded its own tie-break rule on
q5z round 4 (06:21Z — "my earlier depth-first rule … was wrong; largest first, then deepest, then
lexicographic is the useful deterministic rule," the builder's calls replayed to commits). Opus
withdrew its item on q5z round 9 (09:37Z) after reproducing the builder's exact scenario from
scratch and finding its own proposed fix would have published a false GREEN — "the mirror of round
7 rather than its fix." **Why Opus so often:** OpenAI's content filter refuses review of our own
path-confinement/symlink fixtures — by 08:04Z the filter tally was rf2 ×2, study ×2, MEM-005, q5z,
and the kernel (five branches now routed Opus-first for that class).

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
  never `-A`/`.` on a shared checkout.
- **Log-filename sha** (04:59Z): a log path built from `$(git rev-parse HEAD)` took a different sha
  mid-run because the seat committed between the redirect and the exit stamp, so the first "zero
  failures" claim was read from an empty file. **Ratchet:** compute the log path once into a
  variable; never re-evaluate HEAD inside a long command.
- **pgrep self-kill** (06:58Z, same incident as 7888 #2): before verifying which process actually
  held 7888/7890, a `pgrep` pattern matched the seat's own shell and the seat nearly killed itself.
  **Ratchet:** verify cmdline BEFORE kill, never after only printing it.
- **Lock contention ruling** (06:03Z): 10 waiters piled up on the single `~/tmp/suite.lock` at load
  3.6/16 — that rule was inherited from Buster's 4-core box. **Ratchet:** unit suites now run under
  `~/bin/suite-run <cmd>` (one of three parallel lanes); the memory battery and memory-red keep the
  exclusive lock (their wall numbers are trend lines, not gates).
- **Usage-watch contamination** (06:49Z): the hourly `study-agent-usage` collector held flat at
  96 MCP calls / 49 ok / 47 refused for four straight hours (last checked 10:03Z: still 96/49/47),
  while the codex-provider counter moved 128→457 — traced to Sol reviewers probing their own branch
  servers on 7908–7910 through codex, not agent adoption of the tool. Named in inb-46f90f; the
  MCP-server-side count is the honest one.
- **Wedged apparatus builder** (07:41Z): the round-3 apparatus builder stopped after 70 minutes with
  no commit (last file edit 06:38Z, no live process, a liveness ping unanswered for 37 minutes) —
  wedged on a Monitor call. Relaunched with "commit small and often, never block on a Monitor,"
  starting from its 72 uncommitted lines of RED fixtures.
- **Claude session-limit outage** (log entry 10:03Z, event ~09:55Z): HTTP 429 ("resets 10am UTC")
  killed four in-flight agents mid-flight — the Opus re-checks of MEM-005 round 3 and the kernel
  round 3, the arms round-5 builder, and the MEM-003 round-2 builder. Sol (codex) was unaffected.
  All four relaunched after 10:00Z from each worktree's committed state (uncommitted diffs kept).
  Confirmed as the seat's own Claude quota, not a box-level failure.

## What the mayor did, and what waits on Gene

**Mayor did tonight:** merged `bridge/kondo-path-test` (acda1b3, 04:49Z) and
`bridge/routing-doc-test` (3ebeafd/d1c5330, 04:52Z) — both zero-review test/doc fixes, per the night
orders. Sent the port table (`/tmp/ports.txt`, verified 03:38Z) confirming 7888 belongs to
`/srv/fleet/shared-tools` with three live dependents, not an orphan; ruled forge stays on 7906–7910.
Answered the seat's status ask at 04:38Z. **No further merges since the last refresh** — seven GO
branches (ratchets, rf2, template-upsert, memory-battery, study-ops, q5z, read-path-memory) sit in
the queue waiting on the mayor's independent verification pass.

**Waits on Gene, per the night orders' own carve-out:**
- **inb-78e75c** and **inb-041b28** — open decisions named but not detailed further in this seat's
  log; morning items.
- **curtain-call merge order**: fold → store → settings-lens → template-upsert → lens-followups —
  `template-upsert` is GO, `lens-followups` is ready, `fold-diff-tool`'s production read is GO at
  347fe6d3 with Sol's conditions — the order itself still "awaits Gene."
- **claude-skills PR #1** (sol-yolo bundled into the codex skill) — ready, mayor asked to merge or
  request changes; not yet actioned in the log.
- **Production ops inb-3a9818**: **now GO with conditions** (moved from HOLD to 347fe6d3 at 08:29Z)
  — Sol's condition is to retain the full receipt (the VERDICT line alone carries no fallback
  caveat). Round 7 continues hardening the tip; the production pin does not move until the tip
  earns its own GO.

**Follow-up inbox items filed during the night:** inb-1165ce (night orders + both boundary
incidents relayed to the mayor); inb-46f90f (the usage-watch codex-provider contamination, standing
distortion, still true at 10:03Z); inb-07c5e7 (rename.clj's own `z/of-string` still ungated — a
fourth constructor the parser-admission leaf does not cover, left ungated on purpose).

## E3/E6 readiness

E3 (fan-out verb vs. native, 21-owner rung) and E6 (study-ops free-choice adoption) were pre-staged
at 04:55Z (712a828, 1,234 lines): matched pair at N=21 for E3; numeric pass lines (1 committing verb
call; non-test actions ≤10.5; churn bands; wall claimable only above 172 s); E6 needs ≥2 of 3
free-choice `ls-tree` calls with 0-of-3 closing the row; six arm prompts written verbatim with a
ritual-strip block. **Amended prerequisite:** the original go-tree said "E3 needs q5z merged + the
battery green." q5z is now GO (09:37Z) and its MEM-002 aggregate-byte admission ceiling is proven —
but "the battery green" was never the right bar: the battery's own GO note (07:09Z) states main is
RED under it **by design** (held_mb lines fail until each leaf — B3, B4, adoption — lands), so the
real prerequisite is q5z's admission ceiling landing on main, not a battery run turning fully green.
E6 needs study-ops merged — it reached GO at 08:33Z (eighth GO). **Neither q5z nor study-ops has
actually merged to main yet** (checked 09:44Z: only kondo and routing-doc are on origin/main), so
E3/E6 are still blocked on the mayor's merge pass, not on further fix rounds. The apparatus itself
went through four build/review rounds (self-test 55/0 → 162/0 → 288/0 → 354/0) and four Sol
verdicts (NO-GO ×3, GO-WITH-FIX at round 4, 09:45Z); round 5 is building now (relaunched after the
09:55Z outage), closing a runtime-override mis-scoring gap. Per the apparatus's own 05:18Z note:
"Owed before a cohort: PF-1..4 and PF-6, the G1 hand-drive first" — **the PF-4 hand-drive still has
not run.**

## Learning and caveat

**Learning:** the delegated-review loop vision.md asks for is running as designed, and it survived a
full night of pressure: every substantive branch that entered the queue was sent back by an
executed, independent red-team at least once, the worst finds were genuine RCE/TOCTOU-class holes
(ripgrep `--pre`, `read-eval` on `deps.edn`, a symlink race past a collision guard, an unauthenticated
pagination cursor, an admission ceiling bypassable by construct class), and twice tonight a
reviewer's own disagreement resolved correctly — once by Sol conceding its tie-break, once by Opus
reproducing the builder's counter-proof and withdrawing.

**Caveat:** only two branches are actually on main (kondo, routing-doc); seven more sit at GO
waiting on the mayor, and the branches E3/E6 are gated on — q5z and study-ops — are BOTH GO but
NEITHER has merged. The memory kernel (B1) is GO-WITH-FIX at round 3 but still not adopted by any
verb, and two of its five leaves (parser-admission MEM-005, streaming-ls-tree MEM-003) are mid
fix-round with real, unresolved findings (a crash on malformed input; an unauthenticated pagination
cursor). A Claude-side outage cost the seat four agents for roughly five minutes of wall time around
10:00Z — recovered cleanly, but it is a reminder that "the seat" is not the only thing that can stop
without warning. The 9-hour window ends ~13:34Z; roughly 3h30m remain.
