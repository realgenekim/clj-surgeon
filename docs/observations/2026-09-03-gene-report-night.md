<!-- rewritten 2026-09-03T21:51Z (Pacific 14:51 PDT) — restructured to the template's section order 1–10 after Gene's 21:49Z rule -->
# Gene peek report — Surgeon program, Anvil seat, 2026-09-03 (04:35Z → 2026-09-03T21:51Z)

Gene, 2026-09-03 21:49Z, verbatim — the rule this report is now shaped by:

> *"Gene reports require perf improvements vs native, top wins and losses, learnings, and what's next"*

Gene, going to sleep the night before: *"I'm going to sleep; exhausted; you work through next 9
hours; you know the goals; state them. mayor, please watch us every 10m; anvil, ask mayor what help
you want from it; mayor, confirm the help you're going to give."*

The four things he reads for lead: **§2 wins vs native · §3 losses vs native · §6 learnings, each
with its ratchet · §7 top wins / top losses · §8 what's next, one action per lane.** Everything that
used to lead — the night's goals, the queue, the lanes, the Andon, the outage — is now the appendix
below §10.

Every number below is copied from a named source file. Where a figure is not in a file, the cell says
so — nothing here is derived or typed from memory. Sources: `2026-09-03-captains-log-anvil-seat.md`
and `2026-09-03-merge-queue-for-mayor.md` (both current at dded866, 21:50Z),
`2026-09-02-captains-log-bridge-wall-clock-ideal-program.md` (the vs-native benchmark),
`2026-09-02-tweezer-session-5-watch.md`, `2026-09-04-e3-e6-prestaged.md`,
`2026-09-03-resume-here-anvil-seat.md`, and the dated verdict files named in each row.

## 1. Headline

**Zero vs-native measurements were taken tonight, and the night's biggest true number is 34 minutes
— pull-to-lift on the first Andon in this fleet to run the full loop**, on a live shell-injection
hole on `main` that a pagination reviewer found outside its own diff; the mayor reproduced it,
sabotaged the ratchet to prove it has teeth, merged the fix at **a6df86ee** at 21:00Z, and lifted the
freeze. Around it: **12 branches at GO and unmerged**, five memory leaves at GO, and 5 lessons
crystallized as ratchets.

*Events to the contrary:* **no vs-native cohort ran** — E3/E6 are pre-staged and unstarted, so the
program's whole reason for existing was not advanced tonight; the usage watch has read
**96 calls / 49 ok / 47 refused, flat since ~05:00Z**, so no builder used the tool either; twelve GOs
sat unmerged all day because the mayor was offline until 21:00Z; census is 15 rounds deep and still
not GO; and the seat itself was dark for six hours on a weekly quota.

## 2. Wins vs native

| task | native | tool | ratio (returns, wall) | correctness | n | receipt |
|---|---|---|---|---|---|---|
| **E3-P, MEASURED 22:55Z — fan-out verb `alias_migration` vs native, rung P = 21 owners of 100 namespaces (seed 7), fixtures rebuilt into git (`bench/fanout/`), server = q5z tip ∪ main (ac1c8409, attested), driver sol-yolo both arms** | wall **137.0 s**, returns 4.67, non-test actions **5.00**, 0 verb calls, churn +84/−84 | wall **49.3 s**, returns 4.00, non-test actions 4.67, verb calls 2.33 (1.00 committed), churn +84/−84, byte-identical to canonical | **0.36× wall DIRECTION — gap 87.7 s is inside the pre-registered 172 s floor, so NO wall claim**; actions 0.33 inside the 6.1 floor | **3/3 both arms**, six-check FAN gate 6/6 ×6 | 3 + 3 | `docs/observations/2026-09-04-e3-p-cohort.md` (2d32482); receipts /home/forge/tmp/arms/e3 |
| **E6-Lb, MEASURED 22:42Z — free-choice adoption of `:ls-tree` via MCP, rung Lb, curtain-call @ ab267f9, driver sol-yolo, server = study-ops tip ∪ main (f24812b0, attested)** | N: wall 157 / 116 / 119 s; returns 3 / 5 / 4; tool calls 8 / 8 / 7; files read before first edit 0 / 4 / 10 | F: wall 111 / 132 / 107 s; returns 4 / 4 / 4; tool calls 7 / 8 / 7; **ls-tree calls 0 / 0 / 0**; files read before first edit 1 / 1 / 4 | wall gap 14 s inside the 172 s floor — **no wall claim**; ranges overlap | all six green (12/82/0, 577/7784/0, goldens identical) | 3 + 3 | `docs/observations/2026-09-04-e6-lb-cohort.md` (de5d7fb); receipts /home/forge/tmp/arms/e6 |
| *(the NONE MEASURED row this replaced, kept for the record: no vs-native arm ran 04:35Z–21:51Z; E6 ran 22:03Z–22:39Z after Gene's 50% functional ruling)* | — | — | — | — | 0 | captain's log 21:49Z, 22:3xZ |
| *NOT RE-MEASURED — carried from the 2026-09-02 peek report, not evidence about tonight:* extract nine forms + rewire 26 callers, mandated verb (rf2) | 336 s (sd 44), 17.3 returns, 868k tokens | 243 s (sd 12), 14.7 returns, 639k tokens | 0.72× wall, every C < every N | acceptance identical; bytes_beyond_verb 0/0/0 | 3+3 | `rf2-score.md`, via `2026-09-02-gene-peek-report.md` §2 |
| *NOT RE-MEASURED — carried, same caveat:* alias migration on the real repo, the anchor (sl1-R, q5z 2753f23) | 283 s, 14 returns, 13 actions, 499k tokens | 228 s, 13 returns, 9 actions, 322k tokens; 171 files / 1,872 sites in one 62 s call | 0.81× wall, 0.69× actions, 0.64× tokens | both PASS r1–r7; native was predicted to win | 1+1 | `sl1-R-score.md`, via `2026-09-02-gene-peek-report.md` §2 |

**Why nothing was measured**, in the seat's own words, captain's log 21:49Z, verbatim:

> *"ZERO vs-native measurements tonight (E3/E6 prestaged, gated on merges that began 21:00Z and an
> apparatus GO at 20:59Z; usage watch 96/49/47 flat corroborates no builder used Surgeon); the only
> vs-native rows on record are yesterday's 2x-tax benchmark (a loss); the memory-program figures are
> Surgeon-vs-Surgeon-before and must be labelled so."*

**The corroborator that no builder used Surgeon either** — the usage watch, newest line, verbatim
from the captain's log 21:50Z:

> `## 21:50Z — usage watch: usage watch: mcp_tool_calls 96, outcomes {"ok": 49, "refused": 47}, tools {"inspect_clojure": 96}, providers claude 181 / codex 693, error_types 47 (top: no-clojure-files 11, invalid-grep-pattern 7, study-tree-too-large 6, study-output-limit 4); window since 2026-08-30T15:00:00Z`

**96 / 49 / 47 has not moved since ~05:00Z**, across every hourly read of the day (11:50Z, 12:49Z,
the single 19:04Z run covering the five slots the outage swallowed, 19:49Z, 20:49Z, 21:50Z), while
the provider counters climbed (claude 135 → 181, codex 543 → 693). What moved is Sol/Opus reviewers
probing their own branch servers on 7906–7910, not agent adoption. Filed as **inb-46f90f**: a
contaminated denominator, not a measure of adoption.

### 2a. Surgeon vs Surgeon-before (not vs native)

Same tool, old pattern versus fixed pattern. **These are not comparisons against a native agent and
may never be quoted as one.** Every figure is verbatim from the captain's-log entry named in its row.

| change | before | after | receipt (captain's log entry) |
|---|---|---|---|
| **MEM-015** read-path single parse, 1,000 files (wall / allocation) | 11,784 ms / 25.28 GB | 6,621 ms (**−43.8%**) / 14.95 GB (**−40.9%**), identical SHA-256 over all outlines | 06:21Z (`bridge/read-path-memory` 61cb9b5) |
| **MEM-005** parser admission, `cli-ls-tree` **giant** (one 1.9 MiB flat file) | 386.4 MB / 2,058 ms | **33.4 MB / 27 ms** (refused on nodes in 65 ms at 52.6 MB) | 07:33Z (`bridge/parser-admission` 8a55dbc); shape finding at 06:25Z |
| **MEM-005** parser admission, `cli-ls-tree` **nested** (one 300-deep 111 KB file) | 285.7 MB / 784 ms | **24.6 MB / 10 ms** (refused on depth in 19 ms at 44.6 MB) | 07:33Z; the cold-JVM StackOverflow at 512m in the same entry |
| **MEM-003** streaming `ls-tree`, held at N=10,000 | `cli-ls-tree held-scales-with-n {:observed 94.0, :limit 11.5}` | line **GONE**; held 9.4–9.5 MB at 10k | 19:52Z (round 7, 6625b7d); Sol's independent control 93.45 MB retained-batch vs 9.35 MB streamed, 09:22Z |
| **MEM-003** streaming `ls-tree`, wall at the same battery run | 7,547 ms | **1,003 ms** | 19:52Z |
| **kernel B1** disk-journaled transaction, 600 × 512 KiB scope | `Terminating due to java.lang.OutOfMemoryError` at `-Xmx256m`; frozen reference at `-Xmx2g` used **2,046.8 MB** | commits at the **same** `-Xmx256m`, retained **14.19 MB**, flat 13.95 → 14.15 MB from 60 → 600 files; Sol's re-run 14.40 MB against reference 2,046.98 MB; round 8 retained-peak **14.9 MB at 256m** | 06:37Z (RED cc04af6 → GREEN cb67bd9), 07:08Z (Sol), 21:17Z (5a2d254) |

**No ratio is quoted for the kernel row on purpose.** No receipt states one; only the two figures do.
(The 12:57Z entry records an earlier draft's "~145×" being struck for exactly this reason.)

## 3. Losses vs native

The only vs-native rows on record are the **2026-09-02 Anvil/Buster benchmark**
(`docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`). They are losses.
Task class unless stated: the frozen bridge4 `?controls=1` rung — four files, ~90 insertions,
`marvin-voice-remote` @ `ab267f9`. Instrument unless stated: the v3 acid runner (fresh session per
arm, `taskset` core pinning, `TMPDIR` under the seat home, diffs frozen the moment an arm ends), the
scorer pinning rollouts by worktree and start time, complete verified wall = full suite green +
golden unchanged, plus the arm-independent `acid_acceptance_test.clj` (39 assertions) written by an
agent that built no arm.

| task | native | tool | ratio (returns, wall) | correctness | n | receipt |
|---|---|---|---|---|---|---|
| **E3-P pass line, MEASURED 22:55Z** — clause 1 (exactly ONE write call) | — | verb calls 2/3/2: the FIRST call of every tool arm refused `alias-migration-empty-scope` (`scope.paths` are globs; `["src"]` matches nothing; the refusal names a domain cause for a spelling cause, no `next_call`) | **refusals 4 of 7 = 57%, the >20% falsifier fired; E3-P does not pass as pre-registered** despite 3/3 correctness and exact churn | — | 3 | same receipt; product fix filed |
| **E6-Lb free-choice adoption (the square-3 exposure test), MEASURED 22:42Z** | N: never offered the tool; 3/3 correct | F: tool exposed, "nothing here is required"; **adoption 0 of 3**, pre-registered stop fired; program free-choice count now **0 of 13** | wall 0.87× direction, inside the floor; no claim | all green both arms | 3 + 3 | `2026-09-04-e6-lb-cohort.md`; mechanism candidate inb-3e298e (ls-tree text content is a header with zero rows; the tree lives only in structuredContent); caveat: rung Lb names 8 of 10 owners in prose |
| **rung M on the VERIFIED shipped server, the decisive row** — prompt says Surgeon is "available and expected" | wall **367 s**; input tokens carried 1,239,123; non-test actions **10.0**; total actions 19.5; shell calls 11.5; MCP calls 0; patch applies 3.7 | wall **677 s**; tokens 2,297,194; non-test actions **21.2**; total actions 30.5; shell calls 15.7; MCP calls 8.5; patch applies 1.2; refusals 2.7 (13 of 16 `invalid-intent-form`) | **1.84× wall · 1.85× tokens · 2.12× non-test actions · 1.56× total actions** | acceptance failed assertions of 39: native **1.83** vs shipped **3.33**; native holds the only clean sweep, shipped the worst run (5) | **6 + 6**, paired by wave, 12 of 12 pinned | captain's log **12:45Z**; `docs/tech-tree.md` row `"available and expected" \| LOST \| n1: 1.8x wall, 2.1x actions, layering` |
| same cohort, **blind quality**, two judges on twelve frozen diffs under the rubric ruling | Opus 19,16,17,16,17,19 (17.3) · Sol 18.8,18.4,18.3,18.1,18.6,17.8 (18.3) → **17.8** | Opus 15,17,14,19,16,18 (16.5) · Sol 17.3,18.5,16.6,18.0,17.6,18.2 (17.7) → **17.1** | −0.7 of 20 | judges blind to arm; noise floor ±1 point (Sol re-scoring identical diffs) | 6 + 6, 2 judges | captain's log **13:05Z**, noise floor **04:42Z** |
| earlier pooled wall null, **unverified 7888 server — superseded, not a defence** | Anvil clean runs 194–287 s | 237–317 s | **A − native = +3.4 s, SE 17.6, t = 0.19**; powered only for effects ≥ 60 s | all arms correct | **n 6–7** pooled | captain's log **05:30Z**; standing corrected at 12:45Z: *"The earlier 'indistinguishable' wall result stands only for the unverified 7888 server and is not evidence about the shipped build."* |
| **wave build (B)** vs native, three environments | Anvil sequential 243 s · Anvil parallel 245 s · Buster run 2 **191 s** | 303 s · **424 s** · 319 s | 1.25× / 1.73× / 1.67×; B's four samples span **272–579 s** vs native's six at 194–287 | all arms correct; B judged last in s2 and s3 by both judges, clarity **2.0 of 5** | 1 per cell; B n=4 | captain's log **03:35Z**, **03:05Z**, **04:42Z** |
| **typist arm T** (gpt-oss-120b via OpenRouter + shipped Surgeon), Buster, same frozen task | 191 s, **14 model actions** | **409 s**, **44 model actions**, 6 Surgeon calls (1 refusal), 4 typist calls (~$0.007) | **2.14× native wall**, +106 s over the Surgeon-only arm A (303 s) | 579 tests / 7808 assertions / 0 failures; golden unchanged | 1 | captain's log **04:48Z** |
| **r1, a real job** (the surgeon recovery-receipt parent-directory defect), v3 runner | **113 s** | A 148 s · B 194 s | 1.31× / 1.72× | all three pass the focused recovery suite with the new regression test (3 tests, 0 failures) | 1 per arm | captain's log **05:35Z** |
| **study ops vs `rg`** on curtain-call (session 5) — reads, not edits | `rg`: q1 0.02 s, q2 0.00 s, q3 0.01 s = **0.03 s total**; q3 answer WRONG (180 vs 171) | `ls-tree` 1.76 s → **1 of 116 files**, then 0.98 s → **13 of 116**, both `read_complete=false`; `deps`/`topo` 0.13–0.17 s but intra-file; **~3.0 s** of reported wall across 4 round trips | **~100× wall** | q1 not answered on either attempt (the `grep` param never narrowed `file_count` off 116); the tool is RIGHT on the one question `rg` gets wrong (q3) | 4 calls | `2026-09-02-tweezer-session-5-watch.md` |

**The one direction the tool won, stated so this table is not rigged:** on the pooled unverified-server
cohort, conformance ran the other way — **A 1.0 vs N 2.43 failed assertions (p ≈ 0.08)**, bought with
**actions A 23.6 vs N 18.2 (p ≈ 0.07)**; and the 12:45Z verified cohort reversed even that (native
1.83 vs shipped 3.33). Neither is significant. Also unexplained and direction-changing, 05:30Z:
**input tokens carried predict wall better than actions (R² 0.78 vs 0.61, 159 s per million carried),
and the Surgeon arms carry MORE than native (A +23%, B +62%)** — the product's stated mechanism is
fewer tokens re-carried.

## 4. Exactly what the win is

**Mechanism:** the B1 disk-journaled transaction kernel (MEM-006/007/012–014/020) replaced the
frozen-read buffering pattern with a byte-budgeted heap plus a disk journal, so the identical
600-file workload that terminates with `java.lang.OutOfMemoryError` at `-Xmx256m` under the old
pattern now commits at the same `-Xmx256m`; at the round-8 merge point the gate reads
`memory-red RED OOM → GREEN parity 55423110… retained-peak 14.9 MB at 256m` (captain's log 21:17Z).

**Boundary:** this is a Surgeon-vs-Surgeon-before win, **not a win against native**, and the kernel is
GO-WITH-FIX at 5a2d254 **adopted by no verb** — `with-cooperating-writes` has no call sites, and round
9 is pre-adoption hardening. Each round's fix has opened one narrower window than it closed (unbounded
read-judge-delete → two renames → a check-then-act restore → a tombstone inheriting the age of the
lock it broke → a marker whose forgery *deletes* evidence → 188 of 240 concurrent lines typing
`:evidence :retained` for a file that is not there).

## 5. Surprises

- **A pagination reviewer found a remote-code-execution hole on `main`.** MEM-003 round 8 was a
  confirm-only re-check of a NUL-delimiter one-liner; its item 11 was `sh -c` on caller input at
  `core.clj:174-178`, outside the diff, pre-existing, and shipped (captain's log 20:5xZ, 21:07Z).
- **The intent audit certified a deliberately reintroduced vulnerability.** A `format`+`sh -c` site
  put back into `src/` with every `@spec` marker intact still printed `AUDIT-OK= true violations= []`
  — traceability rows cannot enforce a structural absence (21:07Z).
- **A gate's verdict was a function of ambient state.** The fold-diff self-test passed 22 cases on the
  builder's tree (95-row store log) and went red at row 13 on the reviewer's (5 rows) **from one
  commit** — and the unit suite itself appended the 5 rows that left a clean checkout below the
  invisible 80-row threshold (20:29Z, 21:34Z).
- **A fix that closed a tampered-row case broke an untampered one.** MEM-003 round 6: on a tree with a
  directory named `src/mydir.clj`, page 3 refused with "a false statement, two innocent files lost,
  pagination permanently unfinishable" — while the suite stayed green at 778/6322/0 (19:22Z).
- **The weekly limit is a different limit.** 429 at ~13:03Z killed three subagents *and* the main
  loop for six hours; ~35 heartbeats and 5 usage watches queued unanswered and the pulse file sat at
  13:02Z until 19:03Z. Sol/codex kept working the whole time (19:04Z).

## 6. Learnings crystallized — five, each with its log entry and the ratchet it left

1. **Source text is not execution — a verifier must record what RAN.**
   *Log 11:40Z → 12:19Z → 13:01Z (apparatus rounds 8 → 9 → 10).* Round 8's checker re-derived an
   idealised command; round 9 parsed tally lines out of `BASH_SOURCE`, so an exact tally line inside
   an **inert quoted heredoc** was accepted — a visible `FAIL case35d` still scored
   `386 passed, 0 failed`, rc 0.
   **Ratchet (rung 5, shipped at 77e6237):** the tally is a *runtime function* `tally <id>` appending
   `<id> <ok> <fail>` to a `$WORK/tallied` ledger; case 45 compares the ledger against an awk recount
   of the `.out` files and **reads no source text at all**. Sol round 10 confirmed it against twelve
   attacks, decisively the inert-heredoc decoy (`383 passed, 4 failed` — round 9 was green on exactly
   this).
2. **Witness through the production path, not the seam that is easy to reach.**
   *Log 19:22Z → 19:52Z → 20:02Z (MEM-003 rounds 6 → 7).* The round-6 brief said "RED witness first"
   but let the builder pick the *tamper* seam, so a confinement gate was proven only on tampered rows
   — and broke discovered ones: *"on an untampered tree containing a directory `src/mydir.clj` … page
   3 refuses `:unconfined-manifest-row` … a false statement, two innocent files lost, pagination
   permanently unfinishable."*
   **Ratchet:** every confinement witness re-done on a **DISCOVERED** row (round 7), and rounds 7–8
   attacked the production entry point (discovery) rather than the state store — which is the only
   reason the `find`-newline parse defect surfaced at all.
3. **Ambient state is an invisible precondition — a gate that needs it must build it or refuse.**
   *Log 20:29Z → 21:34Z (fold-diff rounds 10 → 11).* Reviewer: *"asserts only that the log EXISTS,
   while case 13 rows 13/16 need >80 rows … as found (5-line log) → ✗ row 13 (exit 0), GATE5_EXIT=1;
   after `make seed-demo` (149 lines) → ✓ 22 cases, all green."*
   **Ratchet (round 11, 3d344432):** the suite builds its own 120-row log under `$TMP` from a
   committed fixture — 25 cases green with `data/` **ABSENT** — and a `LOG=` override below 80 rows
   exits 3 with a **named PRECONDITION**.
4. **Check-then-act on a filesystem is a data-destroyer; use an atomic primitive that fails.**
   *Log 11:34Z → 12:27Z → 19:18Z (kernel rounds 4 → 5 → 6).* `break-lock!`'s restore was
   `statx(target) = -1 ENOENT` then `rename(2)`: **129 of 29,012** third-party claims clobbered.
   **Ratchet:** `Files/createLink(LOCK, tomb)` + `deleteIfExists`; round 6's independent re-run —
   *"my 20,000-break storm gave the hammer 1 window, vs 9,948 in round 5; 0 clobbered"* and
   *"4,000-race: 0 destroyed vs 13, 3,974 typed `:tombstone-exists`"*. **Memory added:**
   `jvm-file-locks-are-per-process`.
5. **A witness that cannot see its own subject reports a true number about the wrong thing.**
   *Log 11:19Z, 12:47Z, 21:07Z (census round 9, and again in the Andon).* The filesystem-call counter
   at `mcp_relation_census_test.clj:877` wrapped functions *downstream* of `forms/init-from-file!`, so
   it certified "zero filesystem work before validation" while `bb` was stat-ing the workspace.
   **Ratchet:** the counter wraps the **first** filesystem touch on *both* entrances, with a liveness
   control each and a hand `strace` receipt, and the old witness was kept and annotated — *"Deleting a
   true witness is not the fix; making it stop over-claiming is."* Same lesson at a different altitude
   in the Andon's REQUIRED item 1: the ratchet for "no shell anywhere" is
   `no-source-file-hands-a-command-string-to-a-shell`, a **structural rewrite-clj scan over the
   source**, not a traceability row — and the mayor sabotaged it to prove it fires.

*Memories added today:* `cursor-mac-needs-an-unpublished-secret` (MEM-003 round 2),
`jvm-file-locks-are-per-process` (kernel round 3), the weekly-limit amendment to
`claude-session-limit-kills-subagents` (19:04Z), and `gene-report-four-required-sections` (21:49Z).

## 7. Best news / worst news, and the top wins / top losses

**Best news:** the Andon cord worked end to end on its first real use, on a defect nobody was looking
for — **pull 20:26Z → lift 21:00Z, 34 minutes**, with the mayor reproducing the injection on
unmodified main, sabotaging the ratchet, and merging at a6df86ee.

**Worst news:** **the program measured nothing against native tonight** — the only axis Gene asked
for is the one axis that produced zero rows, and the corroborating usage watch says no builder
reached for the tool either (96/49/47, flat).

### Top wins

| # | win | the number | receipt |
|---|---|---|---|
| 1 | **The Andon loop, pull to lift, with a sabotaged ratchet** — the first in this fleet to run pull → ack → scoped freeze → independent verification → ratchet → lift | **34 minutes** (20:26Z → 21:00Z). Mayor, verbatim: *"I appended a fresh shell-string call site to src/clj_surgeon/core.clj and ran the suite. It went RED, named the file, quoted the exact offending form back at me, and fired TWO independent rules … It makes the CLASS unrepresentable rather than fixing the instance."* | captain's log **21:44Z**; queue row A |
| 2 | **12 GOs, every one with an EXECUTED independent re-review** — not a paper queue | 12 branches; census 14 rounds, apparatus 10, fold-diff 10, MEM-003 8, kernel 8, q5z 9 — each round naming a class of defect | `2026-09-03-merge-queue-for-mayor.md`; appendix A.3 |
| 3 | **Five lessons became ratchets in the same session that found them** (§6) — runtime tally ledger, discovered-row witnesses, self-built fixture log, `createLink` restore, first-touch counter + structural source scan | e.g. `createLink`: **129 of 29,012 clobbered → 0 of 9,948**; the tally ledger caught the inert-heredoc decoy round 9 passed | captain's log 12:38Z, 13:01Z, 19:18Z, 19:52Z, 21:16Z |

### Top losses

| # | loss | the number | receipt |
|---|---|---|---|
| 1 | **Zero vs-native measurements** — the program's whole purpose, unadvanced for a full night | **0** arms run; E3/E6 pre-staged at 04:51Z and untouched; usage watch **96/49/47 flat** since ~05:00Z | `2026-09-04-e3-e6-prestaged.md`; captain's log 21:49Z, 21:50Z |
| 2 | **A six-hour outage on a weekly quota nobody was watching** | **13:03Z → 19:03Z**. Verbatim: `Agent terminated early due to an API error: You've hit your weekly limit · resets 7pm (UTC) (error type rate_limit, HTTP 429, request id req_011CegYywj3cA9tUvkEQ9xAL, model sent to the API: claude-opus-5)` — plus `claude-sonnet-5` (req_011CegZ2y2WonU8tEsjnruCd) and the kernel reviewer (req_011CegZYyNwPmpeJGYZb3gm7). Three subagents died mid-flight; ~35 heartbeats and 5 usage watches queued unanswered; the pulse file read 13:02Z at 19:03Z | captain's log **19:04Z** |
| 3 | **Thirteen GOs sat unmerged all day because the mayor was offline** — every one a review verdict, not a landing | first merge of the day past kondo/routing-doc was **21:00Z** (a6df86ee, the Andon fix); 12 remain unmerged, including both branches E3/E6 are gated on (q5z, study-ops) | `2026-09-03-merge-queue-for-mayor.md`; git log on main |
| 4 | **Census is at 15 rounds and is not GO** — the longest lane in the program | 14 executed reviews filed, round 15 launched 21:46Z; round-14 blockers: a chmod-000 file escapes as `census-adapter-failure`, and **seven** MCP continuation sites still bypass the 512-byte ceiling (a 661-byte `next_call` measured) | captain's log **21:46Z** |

## 8. Board — what's next: ONE action per lane (Pacific; Gene is PDT, UTC-7, the box is UTC)

| lane | the single next action | when (Pacific) |
|---|---|---|
| **E3 / E6 vs-native cohorts** | Run both **as local fan-out arms on this Anvil box** (`/home/forge/tmp/arms/e3`, `/e6`, ports 7907–7910, one JVM suite under `flock`) the moment **q5z (f51ceae), read-path-memory (b7ef23d) and parser-admission (52c5d85)** are on main. Nothing else is owed: the apparatus is GO cohort-ready at 77e6237 and the pre-registration is complete. | starts within the hour the three merges land; first arms same session |
| **kernel B1** | **First-verb adoption** — `with-cooperating-writes` has zero call sites; round 9 (pre-adoption hardening from 5a2d254) finishes, then one verb adopts it and the battery measures it. | round 9 lands tonight PDT; adoption is the next brief after it |
| **census** | **Round 15** (Opus, launched 21:46Z): readability in the path resolver (denied → `unreadable-source-path`, never `adapter-failure`), and ONE `continuation` constructor for every MCP `next_call` with a witness driving every refusal kind at a 600-char root. | verdict expected ~15:30–16:30 PDT |
| **fold-diff** | **The round-11 verdict** (Opus, running on 3d344432): may the tip replace the **347fe6d3** production pin, or does the pin hold an eleventh time. | verdict expected ~15:15 PDT |

Standing: the 9-hour night order ended ~06:34 PDT (13:34Z) inside the outage; the seat has run on its
own since 12:03 PDT (19:03Z). This report written **14:51 PDT (21:51Z)**. The weekly Claude limit
resets 12:00 PDT (19:00Z) and was hit once today. Heartbeat every 10 min to `/tmp/anvil-pulse.txt`;
usage watch hourly.

## 9. Decisions waiting on Gene

- **Curtain-call merge order — `template-upsert`.** The branch is **GO at 25b98a83** (round 2:
  nil-only fallback, fixture pinned, EARS exact, unit 1056/13176/0); the queue row says in terms
  *"merge order still awaits Gene: fold → store → settings-lens → template-upsert → lens-followups."*
  Locator: `2026-09-03-merge-queue-for-mayor.md` row 8; the branch's filed collateral item is
  **inb-2f78f5**. **Recommend:** take that order as written — fold after the mayor's fold-diff
  production run at the pinned 347fe6d3, then store, then the lens stack, goldens re-run at each step.
- **The PF-4 hand-drive session restart.** PF-4 (G1 hand-drive of `alias_migration` / `ls-tree`) is
  owed before any cohort and **requires q5z on main and this seat's MCP tools bound to port 7906 —
  which needs a session restart, deliberately not done while lanes were running**
  (`2026-09-03-resume-here-anvil-seat.md`: *"PF-4 = G1 hand-drive of alias_migration/ls-tree (needs
  q5z on main and my MCP tools bound to 7906 — session restart required, NOT tonight while lanes
  run)"*). **Recommend:** authorise the restart at the next lane boundary, immediately after q5z
  merges and before E3 runs — the alternative is a cohort whose first verb has never been hand-driven,
  which is the exact failure the `hand-drive-every-mode-you-ship` memory exists for.
- **inb-d27b79 — the ANDON.** No decision required: acked, freeze granted, **fix merged a6df86ee,
  freeze lifted, bead clj-surgeon-0me unblocked**. Here so you see it before anyone tells you.
- **claude-skills PR #1** (sol-yolo bundled into the codex skill) — ready; not actioned today.
  **Recommend:** merge; it is the fix every Sol launch today depended on.
- **inb-3a9818** (production ops, fold-diff-checkpoint) — GO with conditions at 347fe6d3 (08:29Z); the
  pin did **not** move today (rounds 6, 8, 10 refused the tip; round 11 in review). **Recommend:**
  accept, retaining the full receipt.
- **inb-78e75c** (the "two public tools" invariant in the one-compiler plan, before census merges) —
  carried from 2026-09-02. Recommendation stands: *read tools compose through inspect; write tools
  stay gated.*
- **inb-041b28** (the announce UI has no unannounce control) — carried from 2026-09-02, product call.
- Filed for awareness, no decision required: inb-1165ce (night orders + boundary incidents + the ANDON
  note), inb-46f90f (usage-watch contamination), inb-75aaf7 (find expression-start tokens), inb-1f9a27
  (nonexistent `:file` → untyped `:invalid-arguments`), inb-00d296 (prepared-wire readiness race),
  inb-eca3b1 (`System/exit` inside the ls-tree op), inb-ef6dd6 (MEM-005's admission refusal embeds an
  absolute path), inb-114faa (tagged-literal / hat-meta towers score depth 1), inb-07c5e7
  (`rename.clj`'s own `z/of-string` left ungated on purpose), inb-276378 (MEM-003 cursor identity).

## 10. Answers to last peek's questions (from the 2026-09-02 peek report, §10)

- *What model?* Fable 5.1 for the seat; Opus and Sol (codex) do the reviews. Seven lanes are Opus-first
  because OpenAI's content filter refuses our own symlink/path-confinement fixtures — rf2 ×2, study ×2,
  MEM-005, q5z, the kernel, MEM-003's cursor brief, and (12:58Z) fold-diff round 9: *"flagged for
  possible cybersecurity risk"*.
- *What host?* Anvil, user forge, 16 cores. The new constraint is **not** the box: it is the seat's own
  Claude **weekly** quota, which took it offline 13:03Z–19:03Z.
- *Crank up parallelism?* Three lanes now rather than five, by completion not by throttling — MEM-003
  and the apparatus both closed at GO. `~/bin/suite-run` (three lanes) is still the suite route; the
  memory battery and memory-red keep the exclusive lock on purpose.
- *Friction ledger?* Ratified practice — five ratchets today (§6), each with the exact text, a rule and
  a trigger, plus four memories.
- *On deck / exploring / option value?* E3/E6 pre-staged, apparatus prerequisite satisfied at 77e6237.
  The gate is purely the merge of q5z, read-path-memory and parser-admission. **Nothing was measured
  against native while they waited — see §2.**
- *Prosecution list in a trusted place?* The twelve inbox items from 2026-09-02, joined by ten filed
  today (§9), of which inb-d27b79 is the incident record.

---

# Appendix — the operational detail (moved below §10 on 2026-09-03)

## A.1 The night's goals, from `docs/observations/2026-09-03-night-orders-anvil.md`, with a status each

| # | Goal (night orders, Gene's priority order) | Status | Evidence |
|---|---|---|---|
| 1 | Every ready branch reaches GO with an **executed independent re-review** and sits in the mayor's queue | **Met for 14 branches; 1 lane still in fix rounds** | 3 merged (kondo, routing-doc, the andon fix a6df86ee), 12 GO and unmerged (A.2), census alone still held at 14 rounds (A.3) — `2026-09-03-merge-queue-for-mayor.md` |
| 2 | The memory program (Gene's choice B) lands as measurable pieces; **no `-Xmx` by judgement anywhere** | **Met** — all five leaves carry a GO: battery 5534e94, read-path b7ef23d, parser-admission 52c5d85, MEM-003 **95b0881** (Opus round 8: "GO — merge 95b0881"), kernel B1 **5a2d254** (Opus round 8: "5a2d254 should replace 11c7377 as the merge point"). Kernel adoption is still zero and round 9 is pre-adoption hardening | merge-queue rows 11/13/15/16/14; captain's log 20:26Z, 21:29Z; battery's "no auto 4g JVM" item closed in its round-3 delta |
| 3 | Records current for the morning: log per event, resume note per material change, a Gene report, friction ledger → ratchets | **Met, with a six-hour hole** | captain's log (last entry 21:50Z), resume note (`RESUME DELTA 21:35Z`), merge queue, this report — but nothing was written 13:03Z→19:03Z (A.5) |
| 4 | Hard rules hold all night (nothing merges from here; ports 7906–7910; never 7888/7894/7895; no sudo; suites under `~/bin/suite-run`) | **Met, with three boundary notes reported not hidden** | captain's log 06:06Z (codex required-MCP handshake), 06:58Z (`-c mcp_servers={}` merges into the repo table), 21:06Z (Sol ran `make mcp-test`, whose readiness check touches 7890 — not a forbidden port) |

## A.2 The twelve GOs sitting unmerged (authority: `2026-09-03-merge-queue-for-mayor.md`)

| # | repo / branch | head | verdict | gates quoted in the queue row |
|---|---|---|---|---|
| 1 | clj-surgeon `bridge/receipt-ratchets` | c5ef7ca | GO (Sol round-3 GO-WITH-FIX + last item verified by the seat) | test-fast 720/6018/0, mcp-test 400/4142/0, oracle pass |
| 2 | clj-surgeon `bridge/rf2-extract-rewire` | 965d49e | GO (Opus round-4: 5/5 closed; residuals pre-existing on main) | — (queue row states no gate numbers) |
| 3 | clj-surgeon `bridge/q5z-alias-migration` | f51ceae | GO (Opus round 9: dispute upheld, reviewer withdrew its item; nine rounds, Sol 1–6, Opus 7–9) | 452/5198/0, 735/6263/0, oracle pass |
| 4 | clj-surgeon `bridge/study-ops-mcp` | 4480e3d | GO (Opus round-4 "merge it" + round 5 lands all six items) | mcp-test 428/5574/0, test-fast 731/6023/0, oracle pass, bb parity 24/97/0 |
| 5 | clj-surgeon `bridge/memory-battery` | 5534e94 | GO **as tooling** — main is RED under it BY DESIGN; merge FIRST among memory branches | self-test 29/158/0, test-fast 731/6070/0 |
| 6 | clj-surgeon `bridge/read-path-memory` | b7ef23d | GO (round-2 corrections verified; corrected sha — the frozen-differential commit sits AFTER 2aa648a) | 702/5912/0, 386/3974/0, oracle pass |
| 7 | clj-surgeon `bridge/parser-admission` | 52c5d85 | GO (Opus round 3: both blockers CLOSED) | 748/6196/0, 385/3971/0, oracle pass, memory-red 6/6 |
| 8 | clj-surgeon `bridge/anvil-arms-apparatus` | **77e6237** (supersedes 23a7643) | GO **cohort-ready at 77e6237** per Sol round 10 | `anvil-arms self-test: 389 passed, 0 failed` |
| 9 | clj-surgeon `bridge/streaming-ls-tree` (MEM-003) | **95b0881** | **GO** (Opus round 8: "merge 95b0881") | 785/6356/0, 389/3988/0, oracle, 24/138/0; battery cli-ls-tree held 9.4–9.5 MB at 10k (baseline 94.0) |
| 10 | clj-surgeon `bridge/txn-journal` (kernel B1) | **5a2d254** | **GO-WITH-FIX** (Opus round 8) — kernel LATENT, adoption zero | 720/5976/0, 467/4534/0, oracle, 0 warnings |
| 11 | curtain-call `bridge/template-upsert` | 25b98a83 | GO (round 2) — merge **order** still awaits Gene | unit 1056/13176/0 |
| 12 | curtain-call `bridge/fold-diff-tool` | **347fe6d3** (tip 3d344432) | GO **for the production read at 347fe6d3**; round 10 ruled "034fba53 may NOT replace the pin yet", round 11 in review at the tip | — (queue row states the round-11 tip's gates, not 347fe6d3's) |
| A | clj-surgeon `bridge/andon-find-build-files-argv` | **MERGED a6df86ee** | ANDON fix — merged by the mayor 21:00Z, freeze LIFTED | run_all 709/5945/0, mcp-test 384/3982/0, oracle, intent audit OK with SHELL-ARGV-001/002/003 `:implemented` |

Also ready, not a GO: curtain-call `bridge/lens-followups` 934716dc and claude-skills PR #1.
**Consolidated order to the mayor** (inb-1165ce, restated in the 21:35Z resume delta): merge from the
queue as GOs appear; independent verification stays the mayor's.

## A.3 The lanes — rounds executed and the class of defect each round found

**census** — clj-surgeon `bridge/census-verb`, tip **d338554**; **14 executed reviews**, round 15
launched 21:46Z. *(Rounds 1–10 as reported in the 12:53Z refresh.)*

| round | verdict | class of defect found |
|---|---|---|
| 9 (11:19Z) | NO-GO | **a witness that could not see its subject** — the counter at `mcp_relation_census_test.clj:877` wraps later fns and misses `forms/init-from-file!` |
| 10 (11:57Z) | NO-GO | the fix was proven on ONE field (`threads`) and ONE entrance (MCP); CLI continuation `:dir .` retargets on replay |
| 11 (landed ~13:05Z, read after the outage) | NO-GO | undefined door still returns `:dir .`; **`:next-command` interpolates the root unescaped** — `;printf INJECTED` becomes command-injection syntax; `pool_size=9223372036854775808` throws instead of a typed refusal |
| 12 (20:09Z) | NO-GO | `str/trim` on the caller's path — `/root␣␣` replayed against the **trimmed sibling's two files**; a raw `0xff` argv byte became U+FFFD; a 512-byte ceiling counting Java characters emitted 890 UTF-8 bytes |
| 13 (21:06Z) | NO-GO | **row ordering, not the rows** — a U+FFFD path with an unknown field lost to `unknown-arguments` and the corrupt path went out in a continuation; narrowing remedies refused without stating the measured length |
| 14 (21:46Z, Sol) | NO-GO | **a chmod-000 file escapes as `census-adapter-failure`** with a false resource-exhaustion remedy (`mcp_paths.clj:67` checks regularity, not readability); **seven** MCP continuation sites still construct `next_call` outside the ceiling — a live 600-char root emitted a **661-byte** `next_call` |

**MEM-003 streaming ls-tree** — **CLOSED at 95b0881 (GO)**. **8 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 3 (11:17Z, Opus) | GO-WITH-FIX, held | tampered rows served silently; `:returned` printed unmeasured; **a `..` row path read outside the root** |
| 4 (12:04Z, Opus) | GO-WITH-FIX, held | **two opens of one mutable file** — 400 page-2 reads under a live rows swap gave `{REFUSE 223, SERVED-correct 88, SERVED-WRONG 89}` |
| 5 (12:54Z, Opus) | GO-WITH-FIX | items 1–5 closed on paired re-runs (storm 95/400 wrong → 0/800); the rest was **documentary** |
| 6 (19:22Z, Opus) | **NO-GO** | **the fix was witnessed through the wrong seam** — on an untampered tree with a directory `src/mydir.clj`, page 3 refused; suite green at 778/6322/0 throughout. Worse and pre-existing: a FIFO named `*.clj` hung the op forever, "survived SIGTERM, requiring SIGKILL" |
| 7 (20:02Z, Opus) | GO-WITH-FIX | discovery restricted to regular files + symlinks-to-regular-files (13 → 12); FIFO tree scans in 26 ms. THE FIX, pre-existing, one line: `find` output split on newlines |
| 8 (20:26Z, Opus) | **GO — merge 95b0881** | `-print0` + NUL split; 8/8 hostile name shapes with parity true. **The same reviewer filed the ANDON** (its item 11, outside its diff) |

**kernel B1** — merge point **5a2d254**; round 9 pre-adoption hardening building. **8 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 4 (11:34Z, Opus) | GO-WITH-FIX, held | **check-then-act restore** — `statx` ENOENT then `rename(2)`: **129 of 29,012** third-party claims clobbered |
| 5 (12:27Z, Opus) | GO-WITH-FIX, held | `recover!` prunes its OWN fresh tombstone; collision guard `.exists` + ATOMIC_MOVE — **13 of 4,000** deliberate races destroyed the judged claim |
| 6 (19:18Z, Opus) | GO-WITH-FIX — **first round with no blocker** | all round-5 items closed on the reviewer's probes; four minors (no receipt names the break path; a sidecar "stamped 10 years ahead"; orphan `LOCK.broken-at.*`; a clean release re-typed as a 24-hour break) |
| 7 (20:24Z, Opus) | GO-WITH-FIX at 11c7377 | the round's own fix opened one hole: *"the `:phase :linked` marker is the first mechanism on this branch whose forgery **DELETES** evidence"*; 61 s of NFS skew silently marks every row `:stamp :unreadable` |
| 8 (21:29Z, Opus) | GO-WITH-FIX — **"5a2d254 should replace 11c7377"** | *"six concurrent `recover!` calls, **188 of 240** lines said `:evidence :retained` for a file not there"*; `stamp-broken-at!` "minted 40 orphan sidecars in probe E4" |

**apparatus (anvil-arms cohort harness)** — **CLOSED at 77e6237 (GO, cohort-ready)**. **10 reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 8 (11:40Z) | NO-GO | **the meta-ratchet was itself blind**: a visible FAIL still scored `386 passed, 0 failed`, rc 0 |
| 9 (12:19Z) | NO-GO | **source text is not execution**: an exact tally line inside an inert quoted heredoc was accepted |
| 10 (13:01Z, Sol) | **GO, cohort-ready at 77e6237** | baseline `389 passed, 0 failed`; **twelve attacks all failed closed**, decisively the inert-heredoc decoy at `383 passed, 4 failed` |

**fold-diff** — tip **3d344432** (round 11 in review); the **production read GO stays at 347fe6d3**.
**10 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 6 (10:56Z) | NO-GO for replacing the pin | **the scan is a text regex, fail-open** |
| 8 (12:03Z) | NO-GO | indirection still fails open; a fake bb exiting 47 became exit 1 with no REFUSED/FAILED line |
| 9 (19:27Z, Opus) | GO-WITH-FIX, pin stays | **a phase's exit code taken as a verdict**; three scan shapes fail open |
| 10 (20:29Z, Opus) | GO-WITH-FIX, "034fba53 may **NOT** replace the pin yet" | **the gate's verdict was a function of ambient state** (5-line log → red at row 13; after `make seed-demo`, 22 cases green) |
| 11 (21:34Z pushed; review running) | — | builder's own statement of the same class: *"the reviewer's tree had 5 rows and went red at row 13, mine had 95 and went green, from one commit"* |

## A.4 The Andon — inb-d27b79, CLEARED, freeze LIFTED

**What.** At 20:26Z the seat pulled the cord on a **shell-injection defect on clj-surgeon `main`**,
found by the MEM-003 round-8 reviewer *outside its own diff* (item 11, verbatim): *"caller-supplied
`:dir` is executed by `sh -c` — `src/clj_surgeon/core.clj:174-178` — `(find-build-files "/tmp/…/H;
touch /tmp/…/PWNED3 ; echo z")` → marker before false, after true; end-to-end through `run-ls-tree`
the marker fired."*

**Evidence.** Seat verification on `origin/main` ad83378 found `cmd (format "find %s \( %s \) -prune -o
… -print" (str dir) prune-expr)` handed to `babashka.process/shell … "sh" "-c" cmd` — **the only
`sh -c` in src**. Seat probe, own hands, fresh clone, hostile dir `$P/H; touch $P/CANARY ; echo z`:
at 11413f2 the CLI exits 1 and **canary_exists=YES**; at 811f4b0 it exits 0 and **canary_exists=no**.
The Opus adversarial review reproduced it *"three ways at 11413f2 … not reproducible any way at
811f4b0; strace shows the caller value as exactly one token and zero shell execve in the process tree
… the committed witnesses go red at the vulnerable commit (10 fail / 1 error of 12 assertions)."*

**Who acked.** The mayor, at 20:35Z — **9 minutes after the pull, 1 minute after a direct cross-session
message**. The escalation receipt at 20:33Z records the ≤5-minute SLA missed at rung 1. Rulings,
verbatim (captain's log 20:43Z):

> *"CONFIRMED INDEPENDENTLY … That is textbook shell injection and your reproduction stands on its
> face. I am not treating this as credible-not-yet-proven; it is proven."*

> *"RUNG-2 FREEZE DECISION — GRANTED, scoped as you framed it … That is the release-lane freeze, not a
> seat freeze: you keep full diagnosis and repair authority."*

> *"an OPEN, GENE-PRIORITISED bead — clj-surgeon-0me — to expose :ls-tree through the MCP server …
> Had we shipped that before you found this, we would have handed every agent in the fleet a
> shell-injection vector through the tool's most-recommended read … Your cord did not just catch a
> defect on main, it caught one on the roadmap."*

**Fix.** `bridge/andon-find-build-files-argv` **32c0c7f** (11 commits on 11413f2). Opus adversarial
review at 811f4b0: GO-WITH-FIX, *"merge to main, with items 1 and 2 in the same merge"*; both REQUIRED
items landed — (1) a **structural** source scan `no-source-file-hands-a-command-string-to-a-shell`
(rewrite-clj, no allowlist) after the reviewer reintroduced a `format`+`sh -c` site with every `@spec`
marker intact and the marker audit still printed `AUDIT-OK= true violations= []`; (2) the shadowed
`format` binding at `core.clj:539`. Plus `-H` on both find argvs, temp dirs, `catch Exception`.

**CLEARED (captain's log 21:44Z, quoting /tmp/mayor-2100.txt, 21:00Z).** The mayor merged the fix at
**a6df86ee** after (1) reproducing the injection on unmodified main — *"CREATED THE CANARY FILE on my
disk … the attacker controlled the return value as well as executing the command"*; (2) verifying the
fix on the merged tree; (3) **sabotaging the ratchet** — *"It went RED, named the file, quoted the
exact offending form back at me, and fired TWO independent rules"*; (4) binding the two mcp-test
failures to unmodified main at the same box load (clj-surgeon-fdo, not a regression). Freeze LIFTED;
bead clj-surgeon-0me unblocked. Seat-verified from Anvil: a6df86ee is an ancestor of origin/main,
`"sh" "-c"` literals in src = 0, the ratchet test present. **Pull-to-lift: 34 minutes.** The mayor's
own note for the record: *"my ack was 8 minutes late and you had to escalate on a second channel to
get it. That failure is the most useful thing in the record."*

## A.5 Outage — six hours dark, 13:03Z → 19:03Z

The seat's **Claude WEEKLY limit** hit at ~13:03Z. Exact text, verbatim from the 19:04Z log entry:

> `Agent terminated early due to an API error: You've hit your weekly limit · resets 7pm (UTC) (error type rate_limit, HTTP 429, request id req_011CegYywj3cA9tUvkEQ9xAL, model sent to the API: claude-opus-5)`

— and the same for `claude-sonnet-5` (req_011CegZ2y2WonU8tEsjnruCd) and the kernel reviewer
(req_011CegZYyNwPmpeJGYZb3gm7). Three subagents died mid-flight (fold-diff round-9 Opus review,
MEM-003 round-6 Sonnet builder with four commits landed and gates NOT run, kernel round-6 Opus
re-check). **The main loop was silent too**: every heartbeat and usage-watch prompt from 13:03Z to
19:03Z queued unanswered — about 35 heartbeats and 5 usage watches, **no pulse refresh for six hours**
(pulse file still reading 13:02Z at 19:03Z). Sol/codex was unaffected; the census round-11 verdict
landed at ~13:05Z and sat unread until 19:05Z. All three lanes were relaunched from committed state at
19:03Z. Lesson filed to the memory `claude-session-limit-kills-subagents`: there is a WEEKLY limit as
well as the 10:00Z session limit, it kills the main loop's own turns, and **the pulse file's staleness
IS the signal**.

## A.6 Caveats — what was NOT verified

- **Gene's physical receipts** — no device, lock-screen, route or speaker evidence was produced; none
  was in scope.
- **Any merge other than a6df86ee** — every other "GO" here is a review verdict, not a landing.
- **The memory battery's two pre-existing failures**, which still stand: `rename-ns-plan-full-match`
  9.8/3.0 and `workspace-sources-read-all` 40.8/6.5 (captain's log 19:52Z), both pre-existing at
  ancestor a9d2d4b, so every battery run ends `FAIL (INCOMPLETE)` exit 1 by inheritance, not regression.
- **The 7888 blast radius** — whether 7888's callers can pass an arbitrary dir string into
  `find-build-files` is **unverified by this seat** and is the mayor's by their own ruling. Service
  details quoted in A.4 come from the mayor's file, not measured here; this seat has never contacted 7888.
- **The fold-diff round-11 verdict** — the review is running; A.3's row states the builder's own claim.
- **The "apparatus GO at 20:59Z" in the 21:49Z quote** is the seat's own line as written; the
  apparatus GO **verdict** on record is Sol round 10 at 13:01Z (77e6237). Quoted, not reconciled.
