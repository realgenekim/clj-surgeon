<!-- refreshed 2026-09-03T12:53Z (Pacific 05:53 PDT) — final refresh of the night order -->
# Gene peek report — Surgeon program, Anvil seat, the night of 2026-09-03 (04:35Z → 2026-09-03T12:53Z)

Gene, going to sleep: *"I'm going to sleep; exhausted; you work through next 9 hours; you know the
goals; state them. mayor, please watch us every 10m; anvil, ask mayor what help you want from it;
mayor, confirm the help you're going to give."*

Every number below is copied from a named source file. Where a figure is not in a file, the cell is
blank and says so — nothing here is derived or typed from memory.

## 0. The tables first

### 0a. The night's goals, from `docs/observations/2026-09-03-night-orders-anvil.md`, with a status each

| # | Goal (night orders, Gene's priority order) | Status | Evidence |
|---|---|---|---|
| 1 | Every ready branch reaches GO with an **executed independent re-review** and sits in the mayor's queue | **Met for 12 branches; 5 lanes still in fix rounds** | 2 merged (kondo, routing-doc), 10 GO and unmerged (§0b), 5 held (§0c) — `2026-09-03-merge-queue-for-mayor.md` |
| 2 | The memory program (Gene's choice B) lands as measurable pieces; **no `-Xmx` by judgement anywhere** | **Partly met** — 3 of 5 leaves at GO (battery 5534e94, read-path b7ef23d, parser-admission 52c5d85); B1 kernel HOLD at round 5, MEM-003 HELD at round 5 | merge-queue rows 11/13/15/14/16; battery's "no auto 4g JVM" item closed in its round-3 delta |
| 3 | Records current for the morning: log per event, resume note per material change, a Gene report, friction ledger → ratchets | **Met** | captain's log (last entry 12:50Z), resume note (18 deltas + `RESUME DELTA 12:05Z`), merge queue (last commit a73a4da 12:50:21Z), this report |
| 4 | Hard rules hold all night (nothing merges from here; ports 7906–7910; never 7888/7894/7895; no sudo; suites under `~/bin/suite-run`) | **Met, with two boundary incidents reported not hidden** | captain's log 06:06Z (codex required-MCP handshake) and 06:58Z (`-c mcp_servers={}` merges into the repo table) |

### 0b. The ten GOs sitting unmerged (authority: `2026-09-03-merge-queue-for-mayor.md`, last commit 12:50:21Z)

| # | repo / branch | head | verdict | gates quoted in the queue row |
|---|---|---|---|---|
| 1 | clj-surgeon `bridge/receipt-ratchets` | c5ef7ca | GO (Sol round-3 GO-WITH-FIX + last item verified by the seat) | test-fast 720/6018/0, mcp-test 400/4142/0, oracle pass |
| 2 | clj-surgeon `bridge/rf2-extract-rewire` | 965d49e | GO (Opus round-4: 5/5 closed; residuals pre-existing on main) | — (queue row states no gate numbers) |
| 3 | clj-surgeon `bridge/q5z-alias-migration` | f51ceae | GO (Opus round 9: dispute upheld, reviewer withdrew its item; nine rounds, Sol 1–6, Opus 7–9) | 452/5198/0, 735/6263/0, oracle pass |
| 4 | clj-surgeon `bridge/study-ops-mcp` | 4480e3d | GO (Opus round-4 "merge it" + round 5 lands all six items) | mcp-test 428/5574/0, test-fast 731/6023/0, oracle pass, bb parity 24/97/0 |
| 5 | clj-surgeon `bridge/memory-battery` | 5534e94 | GO **as tooling** — main is RED under it BY DESIGN; merge FIRST among memory branches | self-test 29/158/0, test-fast 731/6070/0 |
| 6 | clj-surgeon `bridge/read-path-memory` | b7ef23d | GO (round-2 corrections verified; corrected sha — the frozen-differential commit sits AFTER 2aa648a) | 702/5912/0, 386/3974/0, oracle pass |
| 7 | clj-surgeon `bridge/parser-admission` | 52c5d85 | GO (Opus round 3: both blockers CLOSED) | 748/6196/0, 385/3971/0, oracle pass, memory-red 6/6 |
| 8 | clj-surgeon `bridge/anvil-arms-apparatus` | **23a7643** (tip 77e6237) | GO **cohort-ready at 23a7643** per Sol round 7 | `anvil-arms self-test: 384 passed, 0 failed` |
| 9 | curtain-call `bridge/template-upsert` | 25b98a83 | GO (round 2) — merge **order** still awaits Gene | unit 1056/13176/0 |
| 10 | curtain-call `bridge/fold-diff-tool` | **347fe6d3** (tip b223f64e) | GO **for the production read at 347fe6d3**, with Sol's conditions | — (queue row states the round-9 tip's gates, not 347fe6d3's) |

Also ready, not a GO: curtain-call `bridge/lens-followups` 934716dc (last in the curtain-call order) and
claude-skills PR #1 (sol-yolo in the codex skill).

**Consolidated order to the mayor** (filed on **inb-1165ce**, restated in the 12:05Z resume delta):
merge from the queue as GOs appear, independent verification stays the mayor's; merge kondo first (done);
no merges have been observed from the mayor as of the 12:05Z delta. The two branches E3/E6 are gated on —
q5z and study-ops — are both GO and both unmerged.

### 0c. The five held lanes — rounds executed and the class of defect each round found

**census** — clj-surgeon `bridge/census-verb`, tip **772b29f** (round 11 pushed 12:47Z, Sol round-11 re-check running). **10 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 1 (02:28Z) | GO-WITH-FIX | schema-only bounds; read-before-filter + uncapped requested files; symlink abort + unpruned walk; CLI crash on its own example |
| 2 (04:56Z) | NO-GO | silent truncation — 4,002 sources returned `ok:true read_complete:true` claiming 4,000; a 2,097,153-byte file silently omitted |
| 3 (06:27Z) | NO-GO | the NEW CLI walk diverges from MCP: follows an escaping symlink, reads a two-link chain three times; ceiling `next_call` carries a placeholder that replays to `unreadable-source-path` |
| 4 (07:32Z) | NO-GO | evidence dropped from `no-fold-arms-found` receipts; `.listFiles` materialises every entry (60,000 non-sources walked) |
| 5 (08:30Z) | NO-GO | `.list` + `sort` realises the whole directory before the counter charges; narrowing offered a 49,997-entry junk subtree |
| 6 (09:19Z) | NO-GO | two contract gaps only — explicit `files` with no arms hands back the identical request |
| 7 | NO-GO | `doors=[1]` copied into a **schema-invalid** `next_call` |
| 8 (10:37Z) | NO-GO on ordering only | a `newfstatat` on the workspace root BEFORE the doors refusal, against a promise of validation before filesystem work |
| 9 (11:19Z) | NO-GO | **a witness that could not see its subject** — the counter at `mcp_relation_census_test.clj:877` wraps later fns and misses `forms/init-from-file!`; and the schema fix made the continuation unfaithful (replay censused a different tree, 370 files) |
| 10 (11:57Z) | NO-GO | the fix was proven on ONE field (`threads`) and ONE entrance (MCP); CLI continuation `:dir .` retargets on replay; CLI silently accepted `:format`/`:max-files` |

**MEM-003 streaming ls-tree** — clj-surgeon `bridge/streaming-ls-tree`, tip **0914a37** (round 5 pushed 12:41Z, Opus round-5 re-check running). **3 executed reviews** (round 2 was superseded by round 3 before review).

| round | verdict | class of defect found |
|---|---|---|
| 1 (09:22Z, Sol) | NO-GO on cursor integrity; the memory result CONFIRMED independently | stat-based cursor digest (byte swap paged as unchanged); offsets neither authenticated nor range-checked; 40-digit offsets throw; 32 outlines against a documented 18-worker pool; every page re-folds all 10,000 rows |
| 3 (11:17Z, Opus) | GO-WITH-FIX, held | tampered rows served silently; `:returned` printed unmeasured; **a `..` row path read outside the root** (confinement hole → security-boundary rule, no queue until closed) |
| 4 (12:04Z, Opus) | GO-WITH-FIX, held | **two opens of one mutable file** — 400 page-2 reads under a live rows swap gave `{REFUSE 223, SERVED-correct 88, SERVED-WRONG 89}`; a symlinked dir escaped lexical confinement |

**kernel B1 (disk-journaled transaction)** — clj-surgeon `bridge/txn-journal`, tip **4b12c9e** (round 5 reviewed 12:27Z; round 6 Opus builder running). **5 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 1 (07:08Z, Sol) | GO-WITH-FIX, HEAD not merge-ready | six blockers; Sol re-ran memory-red independently and upheld the builder's instrument choice |
| 2 (08:56Z, Opus) | GO-WITH-FIX (adopted by no verb) | all six Sol blockers CLOSED against Opus's own injections; gates reproduced to the byte |
| 3 (10:32Z, Opus) | GO-WITH-FIX | **JVM file locks are per-PROCESS** → a cooperating second thread deadlocks the workspace for the life of the process; a future inside the lock writes unlocked; stale-lock break is read-then-unconditional-delete |
| 4 (11:34Z, Opus) | GO-WITH-FIX, held | **check-then-act restore** — `statx` ENOENT then `rename(2)` replaces unconditionally: **129 of 29,012** third-party claims clobbered; `finish-after-throw!` un-commits a committed transaction |
| 5 (12:27Z, Opus) | GO-WITH-FIX, held | `recover!` prunes its OWN fresh tombstone and returns a receipt naming a deleted file; two mtime reads beside the newest-of-mtime/ctime basis; collision guard `.exists` + ATOMIC_MOVE — **13 of 4,000** deliberate races destroyed the judged claim |

**apparatus (anvil-arms cohort harness)** — clj-surgeon `bridge/anvil-arms-apparatus`, tip **77e6237** (round 10 pushed 12:38Z, Sol round-10 confirm running); the **GO stays at 23a7643**. **9 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 1 (05:31Z) | NO-GO for a real cohort | `score.py` skips malformed lines and emits receipts from truncated/duplicated/reversed streams |
| 2 (06:31Z) | NO-GO (6 CLOSED, 6 PARTIAL) | JSONL validation, stale receipts, unanswered calls, health-vs-witness binding, root symlink |
| 3 (08:54Z) | NO-GO | (verdict file `anvil-arms-apparatus-round3-sol-NO-GO.md`; class not restated in tonight's log — named gap) |
| 4 (09:45Z) | GO-WITH-FIX | runtime `make` overrides are not in the resolved map; a second contradictory header (schema 999) scored rc 0 with a receipt |
| 5 (10:33Z) | GO-WITH-FIX | `MAKEFLAGS=CMD=…` **replaced the mapped recipe and still resolved** (env-carried Make semantics bypass); schema_version 3 scored rc 0; capture-mode blocking readline stalled the scan loop (4.13 s / 1 scan) |
| 6 (11:14Z) | GO-WITH-FIX | two duplicated schema literals (watch.py:66, score.py:234) — closed inline by the seat as round 7 |
| 7 (11:20Z) | **GO for cohort readiness at 23a7643** | E3/E6's "apparatus GO" prerequisite satisfied |
| 8 (11:40Z) | NO-GO on the tip | **the meta-ratchet was itself blind**: the presence test ignored the tally's file operand and re-derived an idealised command; pointing case35d's tally at case35c.out gave a visible FAIL with `386 passed, 0 failed`, rc 0 |
| 9 (12:19Z) | NO-GO on the tip | **source text is not execution**: an exact tally line inside an inert quoted heredoc was accepted — a visible `FAIL case35d` still yielded rc 0 / `386 passed, 0 failed`; non-canonical spacing produced a false failure |

**fold-diff** — curtain-call `bridge/fold-diff-tool`, tip **b223f64e** (round 9 pushed 12:50Z, Sol round-9 re-check running); the **production read GO stays at 347fe6d3**. **7 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 1 (02:51Z) | GO-WITH-FIX | read-only receipt digests the wrong file; frontier gap unmeasured; baseline compared against the whole stack (7 fixes) |
| 2 (04:30Z) | re-review filed | (verdict file `folddiff-rereview.md`; class not restated in tonight's log — named gap) |
| 3 (07:16Z) | GO-WITH-FIX for the mayor's exact production read | all six prior items CLOSED |
| 4 (08:29Z) | **GO-WITH-FIX for the production read at 347fe6d3** | residuals 7/8/9 CLOSED |
| 5 | NO-GO | three witnesses still open the real `events.jsonl`; the required-var scan can be EMPTY under `requiring-resolve` → false own-complete; `FOLD_DIFF_DATA_DIR` silently ignored under Postgres |
| 6 (10:56Z) | NO-GO for replacing the pinned read | **the scan is a text regex, fail-open** — a `checkpoint/validate` in a COMMENT counts as required while a hidden dynamic `requiring-resolve` is invisible |
| 8 (12:03Z) | NO-GO | indirection still fails open (own copy defining only `validate` → `missing-vars=<none>`, exit 0); a fake bb exiting 47 became exit 1 with no REFUSED/FAILED line; `:data-dir-with-postgres` masks both unknown-ref refusals against the documented precedence |

### 0d. The usage watch, verbatim from the captain's log (last two hourly entries)

> `## 11:50Z — usage watch: usage watch: mcp_tool_calls 96, outcomes {"ok": 49, "refused": 47}, tools {"inspect_clojure": 96}, providers claude 135 / codex 543, error_types 47 (top: no-clojure-files 11, invalid-grep-pattern 7, study-tree-too-large 6, study-output-limit 4); window since 2026-08-30T15:00:00Z`

> `## 12:49Z — usage watch: usage watch: mcp_tool_calls 96, outcomes {"ok": 49, "refused": 47}, tools {"inspect_clojure": 96}, providers claude 147 / codex 549, error_types 47 (top: no-clojure-files 11, invalid-grep-pattern 7, study-tree-too-large 6, study-output-limit 4); window since 2026-08-30T15:00:00Z`

**96 calls, 49 ok, 47 refused — unchanged since ~05:00Z, seven consecutive hourly reads.** The provider
counters moved (claude 135 → 147, codex 543 → 549) while the MCP-server figures did not: what is moving
is Sol/Opus reviewers probing their own branch servers on 7906–7910, not agent adoption of the tool.
Filed as **inb-46f90f**; the figure is a contaminated denominator, not a measure of adoption.

## 1. Headline

**Twelve branches reached GO tonight, every one sent back at least once by an executed independent
red-team first — and ten of them are still sitting unmerged in the mayor's queue.** The memory kernel's
OOM was reproduced on command, TDD-style, then fixed and measured: 600 files that terminate with
`java.lang.OutOfMemoryError` at `-Xmx256m` under the old pattern now commit at the same `-Xmx256m`
retaining **14.19 MB** (Sol's independent re-run: **14.40 MB**; round 5's flatness probe: 60 files
**14.29 MB**, 600 files **14.93 MB**), against a frozen `-Xmx2g` reference that used **2,046.8 MB**
(Sol's re-run: 2,046.98 MB), three-way digest parity `55423110…`. *(The reduction multiple is not
stated in any receipt — only the two figures are, so no ratio is quoted here.)*

*Events to the contrary:* the kernel is still HOLD, not merge-ready — round 5 came back GO-WITH-FIX at
12:27Z with a new blocker and round 6 is building; five lanes are mid fix-round; and **no merges have
been observed from the mayor** since kondo and routing-doc, so E3/E6 remain blocked on merges rather
than on further review rounds.

## 2. Wins vs native

No vs-native cohort ran tonight. E3 (fan-out verb) and E6 (study-ops free-choice adoption) were
pre-staged at 04:55Z (712a828, 1,234 lines); their apparatus prerequisite was satisfied at 11:20Z
(apparatus GO for cohort readiness at 23a7643), so **the only remaining gate is the mayor merging
q5z, read-path-memory and parser-admission** — not further fix rounds. Named gap: **no fresh
wins-vs-native numbers this cycle**; the standing wins (rf2 243 s vs 336 s wall; the q5z anchor
228 s vs 283 s wall) are unchanged from the 2026-09-02 peek report and were not re-measured tonight.

Two numbers from tonight that are explicitly **not** vs-native comparisons (same tool, old pattern vs
fixed pattern), listed here only as labelled asides:

| aside | old | new | receipt |
|---|---|---|---|
| MEM-015 read-path single parse, 1,000 files (wall / allocation) | 11,784 ms / 25.28 GB | 6,621 ms (−43.8%) / 14.95 GB (−40.9%), outlines byte-identical, 0 mismatches over a 160-file differential | `2026-09-03-mem-015-sol-review.md` |
| MEM-003 streaming `cli-ls-tree` at N=10,000 (held / peak / wall) | 94.0/93.6 MB, 430/424 MB, 4,213/5,021 ms | 9.5/9.4 MB, 243/264 MB, 658/769 ms; Sol's independent control 93.45 MB retained-batch vs 9.35 MB streamed | captain's log 08:55Z, 09:22Z |

## 3. Losses vs native

None — no vs-native runs executed tonight, same gate as §2. An empty table here is a claim, so it is
stated plainly: **nothing was measured against native tonight, so nothing can be claimed either way.**

## 4. Exactly what the win is

**Mechanism:** the B1 disk-journaled transaction kernel (MEM-006/007/012–014/020) replaced the frozen-read
buffering pattern with a byte-budgeted heap plus a disk journal, so the identical 600-file workload that
OOMs at `-Xmx256m` commits at `-Xmx256m` retaining 14.19 MB, flat 13.95 → 14.15 MB from 60 → 600 files,
with three-way digest parity and the accountant identity 29,378,776 + 68,180 = 29,446,956 to the byte.

**Boundary:** the kernel is GO-WITH-FIX at round 5 only, **adopted by no verb** (three non-cooperating
writer sites named in `docs/txn-journal.md`, including `worktree_lifecycle_io.clj:438`, a code change
owed in adoption); each round's fix has opened one narrower window than it closed (unbounded
read-judge-delete → two renames → a check-then-act restore → a tombstone that inherits the age of the
lock it broke). Two of the memory program's five leaves are still in fix rounds with unresolved
findings of their own.

## 5. Surprises

- **A meta-ratchet built to catch a false green was itself a false green, twice.** Round 8's case-45
  checker re-derived an idealised command instead of running the real one (`386 passed, 0 failed`, rc 0,
  with a visible FAIL present); round 9's fix parsed the script's source text, and an exact tally line
  inside an **inert quoted heredoc** was accepted — again rc 0 with a visible FAIL.
- Opus reproduced the builder's exact counter-proof on q5z round 9 and **withdrew its own review item**
  after 8 prior rounds — a reviewer reversing itself against live evidence.
- The usage watch has held at **96 / 49 / 47 for seven straight hourly reads** while the codex provider
  counter climbed 128 → 549: it was counting reviewers probing their own branch servers, not adoption
  (inb-46f90f).
- The kernel round-5 fix that had just declared mtime insufficient **shipped two new mtime reads 150
  lines below its own age basis** — and 822 of 9,831 rows read an absent tombstone's `.lastModified = 0`
  as infinitely old.
- MEM-003's confinement is **lexical, not realpath**, on purpose: `discover-projects` follows a symlinked
  `.clj` out of the root on a fresh scan, so the builder pinned the lexical choice with a witness rather
  than let it change silently.
- A Claude session-limit outage (~09:55Z, HTTP 429, "resets 10am UTC") killed **four in-flight agents at
  once** — the seat's own quota, not the box; all four relaunched after 10:00Z from committed state.

## 6. Learnings crystallized — the three of the night that became ratchets or memories

1. **A verifier that re-derives its subject, or reads source instead of execution, is not a verifier.**
   (apparatus rounds 8 → 9 → 10.) Round 8's checker reconstructed an idealised command against `$f`;
   round 9's parsed tally lines out of `BASH_SOURCE`. Sol killed both with a visible FAIL that still
   scored `386 passed, 0 failed`, rc 0. **Ratchet (rung 5, shipped at 77e6237):** the tally is now a
   *runtime function* `tally <id>` that appends `<id> <ok> <fail>` to a `$WORK/tallied` ledger; case 45
   compares the ledger against an awk recount of the `.out` files and **reads no source text at all**.
   An inert tally cannot register; a non-canonical one is just a function call. Red set of seven
   mutations, including the decisive inert-heredoc probe (`383 passed, 4 failed` — round 9 was green on
   exactly this). Source: captain's log 11:40Z, 12:19Z, 12:38Z; delivery invariant 20 named in the 11:40Z
   entry.
2. **Check-then-act on a filesystem is a data-destroyer; use an atomic primitive that fails.**
   (kernel rounds 4 → 5.) `break-lock!`'s restore was `statx(target) = -1 ENOENT` then `rename(2)`, which
   replaces unconditionally: **129 of 29,012** third-party claims clobbered, 20,356 `:restored false`
   outcomes where the displaced owner was never told. **Ratchet (shipped at 4b12c9e):**
   `Files/createLink(LOCK, tomb)` + `deleteIfExists` — **0 clobbered of 884**, the 30k-break probe
   564 → 0, and on the reviewer's own re-run **0 of 9,948** with all 9,947 refusals typed and counted;
   displacement is now reported to `begin!`, to `recover!`, and counted. **Memory added tonight:**
   `jvm-file-locks-are-per-process` (bash `flock` does not block `FileChannel.lock`; a second thread
   throws `OverlappingFileLockException`). Source: captain's log 11:34Z, 12:11Z, 12:27Z.
3. **A witness that cannot see its own subject reports a true number about the wrong thing.**
   (census round 9.) The filesystem-call counter at `mcp_relation_census_test.clj:877` wrapped functions
   *downstream* of `forms/init-from-file!`, so it certified "zero filesystem work before validation"
   while `bb` was stat-ing the workspace and its `.clj-surgeon.edn`. **Ratchet (shipped at 48c64ac and
   widened at 772b29f):** the counter wraps the **first** filesystem touch on *both* entrances, with a
   liveness control each, plus a hand `strace` receipt (3 → 0 workspace syscalls before the refusal), and
   the old witness was **kept and annotated** as an op-body meter — *"Deleting a true witness is not the
   fix; making it stop over-claiming is."* Source: captain's log 11:19Z, 11:42Z, 12:47Z.

*Also added as a memory tonight (from MEM-003 round 2):* `cursor-mac-needs-an-unpublished-secret` — a MAC
over published receipt fields is forgeable, so the secret is per-snapshot and cursor ids are
content-addressed.

## 7. Best news / worst news

**Best:** the delegated-review loop held for the full nine hours under real adversarial pressure. Every
substantive branch was sent back by an executed, independent red-team at least once; the finds were
genuine RCE/TOCTOU/confinement-class holes (ripgrep `--pre`, `read-eval` on a scanned `deps.edn`, a
symlink race past a collision guard, a `..` manifest row reading outside the root, a check-then-act
restore clobbering live claims); and three times tonight a reviewer's own disagreement resolved
correctly against evidence rather than seniority — Sol conceding its tie-break rule (q5z round 4), Opus
reproducing the builder's counter-proof and withdrawing (q5z round 9), and Sol's mutation set killing
two successive versions of our own meta-ratchet.

**Worst:** **the queue is the bottleneck, not the work.** Ten GO branches are unmerged; the two E3/E6 is
gated on (q5z, study-ops) are both GO and both still off main, so the morning's measurement cannot start
on merges that have not happened. The memory kernel is five rounds deep and adopted by no verb, and each
round's fix has opened a narrower window than it closed.

## 8. Board (Pacific — Gene is PDT, UTC-7; the box is UTC)

- **Running now** (five lanes, all subagents of this seat; worktree tips in the 12:05Z resume delta):
  census round-11 Sol re-check (772b29f); MEM-003 round-5 Opus re-check (0914a37); kernel round-6 Opus
  builder (from 4b12c9e); apparatus round-10 Sol confirm (77e6237); fold-diff round-9 Sol re-check
  (b223f64e).
- **Lands next:** each lane's verdict → GO note or the next round. Then the mayor's independent
  verification and merge of the ten GO branches (§0b). E3/E6 cohort runs unblock the moment q5z,
  read-path-memory and parser-admission are actually on main.
- **When:** this refresh is the final one of the night order; the 9-hour order ends ~06:34 PDT (~13:34Z).
  Load ~4–6 on 16 cores, ~20 GB free; heartbeat every 10 min to `/tmp/anvil-pulse.txt`, usage watch
  hourly at :23.

## 9. Decisions waiting on Gene

- **Curtain-call merge order** — fold → store → settings-lens → template-upsert → lens-followups.
  `template-upsert` is GO (25b98a83), `lens-followups` is ready (934716dc), `fold-diff-tool`'s production
  read is GO at 347fe6d3 with Sol's conditions; the order itself still awaits your ruling. Recommend:
  fold after the mayor's fold-diff production run at the pinned 347fe6d3, store next, then the lens stack.
- **claude-skills PR #1** (sol-yolo bundled into the codex skill) — ready; the mayor was asked to merge or
  request changes and it is not actioned in tonight's log. Recommend: merge — it is the fix every Sol
  launch tonight depended on.
- **inb-3a9818** (production ops, fold-diff-checkpoint) — GO with conditions at 347fe6d3 (08:29Z); the pin
  did **not** move tonight (rounds 6, 8 both NO-GO at the tip; round 9 in review). Sol's condition: retain
  the full receipt, since the VERDICT line alone carries no fallback caveat. Recommend: accept.
- **inb-78e75c** (the "two public tools" invariant in the one-compiler plan, before census merges) —
  carried forward from 2026-09-02, still open. That report's recommendation stands: amend to "read tools
  compose through inspect; write tools stay gated."
- **inb-041b28** (the announce UI has no unannounce control) — carried forward from 2026-09-02, still open,
  still a product call.
- Filed for awareness, no decision required: inb-1165ce (night orders + both 7888 boundary incidents),
  inb-46f90f (usage-watch contamination, §0d), inb-07c5e7 (`rename.clj`'s own `z/of-string` left ungated
  on purpose), inb-114faa (tagged-literal / hat-meta towers score depth 1), inb-276378 (MEM-003 cursor
  identity), inb-ef6dd6 (MEM-005's admission refusal embeds an absolute path, so battery parity never
  fires — found from another lane at 12:41Z).

## 10. Answers to last peek's questions (from the 2026-09-02 peek report, §10)

- *What model?* Still Fable 5.1 for the seat; Opus and Sol (codex) do the reviews. Six lanes are now
  Opus-first because OpenAI's content filter refuses our own symlink/path-confinement fixtures — rf2 ×2,
  study ×2, MEM-005, q5z, the kernel, and MEM-003's cursor brief (11:06Z: *"flagged for possible
  cybersecurity risk"*).
- *What host?* Still Anvil, user forge, 16 cores; load ~4–6, ~20 GB free.
- *Crank up parallelism?* Changed tonight: the single shared `~/tmp/suite.lock` that had 10 waiters piled
  up at load 3.6/16 is now `~/bin/suite-run` (three lanes); the memory battery and memory-red keep the
  exclusive lock on purpose, because their wall numbers are trend lines rather than gates.
- *Friction ledger?* Still ratified practice — three more ratchets tonight (§6), each with the exact text,
  a rule, and a trigger, plus two memories.
- *On deck / exploring / option value?* E3/E6 are pre-staged and their apparatus prerequisite is satisfied
  (11:20Z). The gate is now purely the merge of q5z, read-path-memory and parser-admission.
- *Prosecution list in a trusted place?* The twelve inbox items from 2026-09-02, joined by six filed
  tonight (§9).

## Caveats — what was NOT verified

Not verified tonight, and not claimed: **Gene's physical receipts** (no device, lock-screen, route or
speaker evidence was produced — none was in scope); **any merge** (no merge has been observed from the
mayor since kondo and routing-doc; every "GO" here is a review verdict, not a landing); and **the memory
battery's two pre-existing failures**, which still stand — `rename-ns-plan-full-match` 9.8/3.0 and
`workspace-sources-read-all` 40.9/6.5, both pre-existing at ancestor a9d2d4b (9.9, 41.0), so every
battery run tonight ends `FAIL (INCOMPLETE)` exit 1 by inheritance, not by regression.
