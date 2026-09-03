<!-- refreshed 2026-09-03T21:43Z (Pacific 14:43 PDT) — evening refresh, after the andon -->
# Gene peek report — Surgeon program, Anvil seat, 2026-09-03 (04:35Z → 2026-09-03T21:43Z)

Gene, going to sleep: *"I'm going to sleep; exhausted; you work through next 9 hours; you know the
goals; state them. mayor, please watch us every 10m; anvil, ask mayor what help you want from it;
mayor, confirm the help you're going to give."*

Every number below is copied from a named source file. Where a figure is not in a file, the cell is
blank and says so — nothing here is derived or typed from memory. Sources for this refresh:
`2026-09-03-merge-queue-for-mayor.md` and `2026-09-03-captains-log-anvil-seat.md` (both at commit
f753bf5d, 21:34:26Z), `2026-09-03-resume-here-anvil-seat.md` (RESUME DELTA 21:35Z), and the dated
verdict files named in each row.

## 0. The tables first

### 0a. The night's goals, from `docs/observations/2026-09-03-night-orders-anvil.md`, with a status each

| # | Goal (night orders, Gene's priority order) | Status | Evidence |
|---|---|---|---|
| 1 | Every ready branch reaches GO with an **executed independent re-review** and sits in the mayor's queue | **Met for 14 branches; 1 lane still in fix rounds** | 2 merged (kondo, routing-doc), 12 GO and unmerged (§0b), census alone still held at 14 rounds (§0c) — `2026-09-03-merge-queue-for-mayor.md` |
| 2 | The memory program (Gene's choice B) lands as measurable pieces; **no `-Xmx` by judgement anywhere** | **Met** — all five leaves carry a GO: battery 5534e94, read-path b7ef23d, parser-admission 52c5d85, MEM-003 **95b0881** (Opus round 8: "GO — merge 95b0881"), kernel B1 **5a2d254** (Opus round 8: "5a2d254 should replace 11c7377 as the merge point"). Kernel adoption is still zero and round 9 is pre-adoption hardening | merge-queue rows 11/13/15/16/14; captain's log 20:26Z, 21:29Z; battery's "no auto 4g JVM" item closed in its round-3 delta |
| 3 | Records current for the morning: log per event, resume note per material change, a Gene report, friction ledger → ratchets | **Met, with a six-hour hole** | captain's log (last entry 21:34Z), resume note (`RESUME DELTA 21:35Z`), merge queue (commit f753bf5d 21:34:26Z), this report — but nothing was written 13:03Z→19:03Z (§Outage) |
| 4 | Hard rules hold all night (nothing merges from here; ports 7906–7910; never 7888/7894/7895; no sudo; suites under `~/bin/suite-run`) | **Met, with three boundary notes reported not hidden** | captain's log 06:06Z (codex required-MCP handshake), 06:58Z (`-c mcp_servers={}` merges into the repo table), 21:06Z (Sol ran `make mcp-test`, whose readiness check touches 7890 — not a forbidden port) |

### 0b. The twelve GOs sitting unmerged, plus the andon fix (authority: `2026-09-03-merge-queue-for-mayor.md`, commit f753bf5d 21:34:26Z)

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
| 9 | clj-surgeon `bridge/streaming-ls-tree` (MEM-003) | **95b0881** | **GO** (Opus round 8: "merge 95b0881") — eleventh GO of the program | 785/6356/0, 389/3988/0, oracle, 24/138/0; battery cli-ls-tree held 9.4–9.5 MB at 10k (baseline 94.0) |
| 10 | clj-surgeon `bridge/txn-journal` (kernel B1) | **5a2d254** | **GO-WITH-FIX** (Opus round 8: "Merge bridge/txn-journal at 5a2d254, in place of 11c7377") — kernel LATENT, adoption zero | 720/5976/0, 467/4534/0, oracle, 0 warnings |
| 11 | curtain-call `bridge/template-upsert` | 25b98a83 | GO (round 2) — merge **order** still awaits Gene | unit 1056/13176/0 |
| 12 | curtain-call `bridge/fold-diff-tool` | **347fe6d3** (tip 3d344432) | GO **for the production read at 347fe6d3**; round 10 ruled "034fba53 may NOT replace the pin yet", round 11 in review at the tip | — (queue row states the round-11 tip's gates, not 347fe6d3's) |
| **A** | clj-surgeon `bridge/andon-find-build-files-argv` | **32c0c7f** | **ANDON FIX — ready for the mayor's independent probe + merge** (Opus adversarial review GO-WITH-FIX at 811f4b0; both REQUIRED items landed in the closing round) | run_all 709/5945/0, mcp-test 384/3982/0, oracle, intent audit OK with SHELL-ARGV-001/002/003 `:implemented` |

Also ready, not a GO: curtain-call `bridge/lens-followups` 934716dc (last in the curtain-call order) and
claude-skills PR #1 (sol-yolo in the codex skill).

**Consolidated order to the mayor** (filed on **inb-1165ce**, restated in the 21:35Z resume delta):
merge from the queue as GOs appear, independent verification stays the mayor's. **No merges have been
observed from the mayor since kondo and routing-doc** — including the andon fix, which is the one merge
that lifts a freeze. The two branches E3/E6 are gated on — q5z and study-ops — are both GO and both
unmerged.

### 0c. The lanes — rounds executed and the class of defect each round found

**census** — clj-surgeon `bridge/census-verb`, tip **d338554** (round 14 pushed 21:28Z; Sol round-14
re-check running). **13 executed reviews.** *(Rounds 1–10 as reported in the 12:53Z refresh; the new
rows are 11–13.)*

| round | verdict | class of defect found |
|---|---|---|
| 9 (11:19Z) | NO-GO | **a witness that could not see its subject** — the counter at `mcp_relation_census_test.clj:877` wraps later fns and misses `forms/init-from-file!`; the schema fix made the continuation unfaithful (replay censused a different tree, 370 files) |
| 10 (11:57Z) | NO-GO | the fix was proven on ONE field (`threads`) and ONE entrance (MCP); CLI continuation `:dir .` retargets on replay |
| 11 (19:05Z, landed ~13:05Z, read after the outage) | NO-GO | post-scan undefined door still returns `:dir .` and replay retargets; **`:next-command` interpolates the root unescaped** — a root containing `;printf INJECTED` becomes command-injection syntax; `pool_size=9223372036854775808` throws instead of a typed refusal |
| 12 (20:09Z) | NO-GO | `str/trim` on the caller's path — `/root␣␣` replayed against the **trimmed sibling's two files** instead of the named root's one; a raw `0xff` argv byte became U+FFFD and replay named a nonexistent root; a 512-byte ceiling counting Java characters emitted 890 UTF-8 bytes |
| 13 (21:06Z) | NO-GO | **row ordering, not the rows** — a U+FFFD path combined with an unknown field lost to `unknown-arguments`/`unknown-fields` and the corrupt path went out in a continuation; MCP narrowing remedies refused without stating the measured length; a missing `:file` still surfaced as untyped `:invalid-arguments` |

**MEM-003 streaming ls-tree** — clj-surgeon `bridge/streaming-ls-tree`, **CLOSED at 95b0881 (GO)**.
**8 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 3 (11:17Z, Opus) | GO-WITH-FIX, held | tampered rows served silently; `:returned` printed unmeasured; **a `..` row path read outside the root** (confinement hole → security-boundary rule) |
| 4 (12:04Z, Opus) | GO-WITH-FIX, held | **two opens of one mutable file** — 400 page-2 reads under a live rows swap gave `{REFUSE 223, SERVED-correct 88, SERVED-WRONG 89}` |
| 5 (12:54Z, Opus) | GO-WITH-FIX | items 1–5 closed on paired re-runs (storm 95/400 wrong → 0/800); the remaining gap was **documentary**: the source-file check-to-use window (15–19 of 400 pages) had to be named in `design.md` as a boundary |
| 6 (19:22Z, Opus) | **NO-GO** | **the fix was witnessed through the wrong seam** — the `.isDirectory` gate assumed discovery lists only files; on an untampered tree with a directory named `src/mydir.clj`, page 3 refused `:unconfined-manifest-row … is not inside the scanned root` — "a false statement, two innocent files lost, pagination permanently unfinishable", suite green at 778/6322/0 the whole time. Pre-existing and worse: a FIFO named `*.clj` hung the op forever, "survived SIGTERM, requiring SIGKILL" |
| 7 (20:02Z, Opus) | GO-WITH-FIX | discovery restricted to regular files + symlinks-to-regular-files (13 → 12); FIFO tree scans in 26 ms. THE FIX, pre-existing, one line: `find` output split on newlines — "a file named `we\nird.clj` yields 12 lines for 11 files" and the scan throws instead of returning a receipt |
| 8 (20:26Z, Opus) | **GO — merge 95b0881** | `-print0` + NUL split; "old parse = 4 tokens vs shipped = 3 records"; newline, trailing-LF, LF-only, CR, CRLF, 0x01, 250-byte names all 8/8 with parity true. **The same reviewer filed the ANDON** (item 11, outside its diff) |

**kernel B1 (disk-journaled transaction)** — clj-surgeon `bridge/txn-journal`, merge point **5a2d254**
(round 9 pre-adoption hardening building). **8 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 4 (11:34Z, Opus) | GO-WITH-FIX, held | **check-then-act restore** — `statx` ENOENT then `rename(2)` replaces unconditionally: **129 of 29,012** third-party claims clobbered |
| 5 (12:27Z, Opus) | GO-WITH-FIX, held | `recover!` prunes its OWN fresh tombstone and returns a receipt naming a deleted file; collision guard `.exists` + ATOMIC_MOVE — **13 of 4,000** deliberate races destroyed the judged claim |
| 6 (19:18Z, Opus) | GO-WITH-FIX — **first round with no blocker** | all round-5 items closed on the reviewer's probes ("20,000-break storm gave the hammer 1 window, vs 9,948 in round 5; 0 clobbered"; "4,000-race: 0 destroyed vs 13"). Four minors: no receipt says which break path ran; a sidecar "stamped 10 years ahead … never retired"; orphan `LOCK.broken-at.*` listed by nothing; a clean release re-typed as a 24-hour break |
| 7 (20:24Z, Opus) | GO-WITH-FIX — "Merge bridge/txn-journal at 11c7377" | the round's own fix opened one hole: "the `:phase :linked` marker is the first mechanism on this branch whose forgery **DELETES** evidence instead of retaining it"; `stamp-broken-at!` returns success on a failed write; 61 s of NFS skew silently marks every row `:stamp :unreadable` with no count |
| 8 (21:29Z, Opus) | GO-WITH-FIX — **"5a2d254 should replace 11c7377 as the merge point"** ("merging at 11c7377 would ship the evidence-deleting forgery my round-7 verdict called the blocker") | pre-adoption only: "six concurrent `recover!` calls, **188 of 240** lines said `:evidence :retained` for a file not there"; `stamp-broken-at!` "minted 40 orphan sidecars in probe E4"; `:interrupted` needs a corroborated/uncorroborated split |

**apparatus (anvil-arms cohort harness)** — clj-surgeon `bridge/anvil-arms-apparatus`, **CLOSED at
77e6237 (GO, cohort-ready)**. **10 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 8 (11:40Z) | NO-GO on the tip | **the meta-ratchet was itself blind**: the presence test ignored the tally's file operand and re-derived an idealised command; a visible FAIL still scored `386 passed, 0 failed`, rc 0 |
| 9 (12:19Z) | NO-GO on the tip | **source text is not execution**: an exact tally line inside an inert quoted heredoc was accepted — a visible `FAIL case35d` still yielded rc 0 / `386 passed, 0 failed`; non-canonical spacing produced a false failure |
| 10 (13:01Z, Sol) | **GO, cohort-ready at 77e6237** (supersedes 23a7643) | baseline `389 passed, 0 failed`; **twelve attacks all failed closed**: missing tally `384 passed, 3 failed`; injected counted FAIL `389/1`; orphan case99 `386/3`; suffix `383/5`; duplicate+missing `388/5`; non-canonical control `389/0` rc 0; **inert-heredoc decoy `383 passed, 4 failed`** (round 9 was green on exactly this); late `.out` write `385/2`; tally with no `.out` `388/1`; TAB after id `386/2`; two cases one `.out` `384/3`; pre-seeded ledger row erased at startup `389/0` |

**fold-diff** — curtain-call `bridge/fold-diff-tool`, tip **3d344432** (round 11 pushed 21:34Z, Opus
round-11 re-check running); the **production read GO stays at 347fe6d3**. **10 executed reviews.**

| round | verdict | class of defect found |
|---|---|---|
| 6 (10:56Z) | NO-GO for replacing the pin | **the scan is a text regex, fail-open** — a `checkpoint/validate` in a COMMENT counts as required while a hidden dynamic `requiring-resolve` is invisible |
| 8 (12:03Z) | NO-GO | indirection still fails open (own copy defining only `validate` → `missing-vars=<none>`, exit 0); a fake bb exiting 47 became exit 1 with no REFUSED/FAILED line |
| 9 (19:27Z, Opus; Sol's filter refused the brief) | GO-WITH-FIX, pin stays | **a phase's exit code taken as a verdict** — a candidate JVM that never compiled (`No such var: store-pg/read-lines-with-seq`) exited 1 with no VERDICT/FAILED/REFUSED line and was "spelled DIFFERENCES FOUND"; three scan shapes fail open (`eval`+`read-string`, resolver-through-resolver, syntax-quoted macro body) |
| 10 (20:29Z, Opus) | GO-WITH-FIX, "034fba53 may **NOT** replace the pin yet" | **the gate's verdict was a function of ambient state** — the self-test asserted only that the store log EXISTS while case 13 needs >80 rows: "as found (5-line log) → ✗ row 13 … (exit 0), GATE5_EXIT=1; after `make seed-demo` (149 lines) → ✓ 22 cases, all green". Also: the contract line was never required to AGREE with the exit code (REFUSED + exit 0 → driver exit 0, IDENTICAL); `alter-var-root` rebinds invisible to the scan |
| 11 (21:34Z pushed; review running) | — (verdict not yet filed) | builder's own statement of the same class: "My worktree had `data/store/events.jsonl` present with 95 rows — above the invisible 80-row threshold … the reviewer's tree had 5 rows and went red at row 13, mine had 95 and went green, from one commit" |

### 0d. The usage watch, verbatim from the captain's log (newest line, 20:49Z)

> `## 20:49Z — usage watch: usage watch: mcp_tool_calls 96, outcomes {"ok": 49, "refused": 47}, tools {"inspect_clojure": 96}, providers claude 165 / codex 624, error_types 47 (top: no-clojure-files 11, invalid-grep-pattern 7, study-tree-too-large 6, study-output-limit 4); window since 2026-08-30T15:00:00Z`

**96 calls, 49 ok, 47 refused — unchanged since ~05:00Z, now across every hourly read of the day**
(11:50Z, 12:49Z, the single 19:04Z run covering the five slots the outage swallowed, 19:49Z, 20:49Z).
The provider counters moved (claude 135 → 147 → 158 → 165, codex 543 → 594 → 624) while the MCP-server
figures did not: what is moving is Sol/Opus reviewers probing their own branch servers on 7906–7910,
not agent adoption of the tool. Filed as **inb-46f90f**; the figure is a contaminated denominator, not
a measure of adoption.

## The Andon — inb-d27b79, OPEN, acknowledged, release lane FROZEN

**What.** At 20:26Z the seat pulled the cord on a **shell-injection defect on clj-surgeon `main`**,
found by the MEM-003 round-8 reviewer *outside its own diff* (its item 11, verbatim): *"caller-supplied
`:dir` is executed by `sh -c` — `src/clj_surgeon/core.clj:174-178` — `(find-build-files "/tmp/…/H;
touch /tmp/…/PWNED3 ; echo z")` → marker before false, after true; end-to-end through `run-ls-tree`
the marker fired."*

**Evidence.** Seat verification on `origin/main` ad83378 found `cmd (format "find %s \( %s \) -prune -o
… -print" (str dir) prune-expr)` handed to `babashka.process/shell … "sh" "-c" cmd` — **the only
`sh -c` in src**. Seat probe, own hands, fresh clone, hostile dir `$P/H; touch $P/CANARY ; echo z`:
at 11413f2 the CLI exits 1 and **canary_exists=YES**; at 811f4b0 it exits 0 and **canary_exists=no**.
The Opus adversarial review reproduced it "three ways at 11413f2 (find-build-files, run-ls-tree, bb
CLI — canary each time), not reproducible any way at 811f4b0; strace shows the caller value as exactly
one token and zero shell execve in the process tree … the committed witnesses go red at the vulnerable
commit (10 fail / 1 error of 12 assertions)."

**Who acked.** The mayor, at 20:35Z (`/tmp/mayor-2035.txt`) — **9 minutes after the pull, 1 minute
after a direct cross-session message**. The escalation receipt at 20:33Z records the ≤5-minute SLA
missed at rung 1 and the route taken. Rulings, verbatim from the captain's log 20:43Z entry:

> *"CONFIRMED INDEPENDENTLY … I read origin/main src/clj_surgeon/core.clj myself … That is textbook
> shell injection and your reproduction stands on its face. I am not treating this as
> credible-not-yet-proven; it is proven."*

> *"RUNG-2 FREEZE DECISION — GRANTED, scoped as you framed it. Effective now: NO install, NO reload,
> and NO merge to main of anything touching find-build-files or its callers, from any seat, until the
> fix is merged with an adversarial review. That is the release-lane freeze, not a seat freeze: you
> keep full diagnosis and repair authority."*

> *"I will not merge it on your say-so; it gets my independent executed probe of both the vulnerable
> and fixed paths before it lands."*

> *"this one is MINE … 7888 runs as user surgeon from /srv/fleet/shared-tools/clj-surgeon-e7f72e2 at
> sha e7f72e2 dated 2026-08-25, and dev-a, dev-b and dev-c each carry two configs naming it with live
> agent processes … You will have my answer in this file series."*

> *"an OPEN, GENE-PRIORITISED bead — clj-surgeon-0me — to expose :ls-tree through the MCP server …
> Had we shipped that before you found this, we would have handed every agent in the fleet a
> shell-injection vector through the tool's most-recommended read. I am adding a hard gate to that
> bead: it does not ship until this fix is merged. Your cord did not just catch a defect on main, it
> caught one on the roadmap."*

**What is frozen.** No install, no reload, no merge to main of anything touching `find-build-files`
or its callers, from any seat, until the fix merges with an adversarial review. Repair authority stays
with the puller (this seat), per doctrine and per the mayor's own framing.

**Fix sha.** `bridge/andon-find-build-files-argv` **32c0c7f** (11 commits on 11413f2 — the branch was
fast-forwarded from ad83378 to the fetched `origin/main` before any edit). Opus adversarial review at
811f4b0: GO-WITH-FIX, *"merge to main, with items 1 and 2 in the same merge"*; both REQUIRED items
landed in the closing round — (1) a **structural** source scan
(`no-source-file-hands-a-command-string-to-a-shell`, rewrite-clj, no allowlist) after the reviewer
reintroduced a `format`+`sh -c` site with every `@spec` marker intact and the marker audit still
printed `AUDIT-OK= true violations= []`; (2) the shadowed `format` binding at `core.clj:539` that made
an empty tree throw `Wrong number of args (3) passed to: :edn`. Plus `-H` on both find argvs, temp
dirs, `catch Exception`. Seat probe 2 at 32c0c7f: hostile `; touch` dir → canary absent; `$(touch …)`
dir → canary absent; empty dir → "No Clojure files found", exit 1; `"sh" "-c"` literals in src → 0.

**What is waited on.** The mayor's independent executed probe of both paths, then the merge — **that
merge is what lifts the freeze**. Also owed by the mayor: the 7888 blast-radius answer (whether 7888's
callers can pass an arbitrary dir string is **unverified by this seat**). Owed by whoever holds the
beads DB: a `bd` issue — `bd create` fails "no beads database found" from this seat, so the durable
record is inbox **inb-d27b79** plus `/tmp/anvil-andon.txt` and the ANDON note on inb-1165ce. Follow-up
already filed: **inb-75aaf7** (find expression-start tokens `( ) ! ,`).

## Outage — six hours dark, 13:03Z → 19:03Z

The seat's **Claude WEEKLY limit** hit at ~13:03Z. Exact text from the harness, verbatim from the
19:04Z log entry:

> `Agent terminated early due to an API error: You've hit your weekly limit · resets 7pm (UTC) (error type rate_limit, HTTP 429, request id req_011CegYywj3cA9tUvkEQ9xAL, model sent to the API: claude-opus-5)`

— and the same for `claude-sonnet-5` (req_011CegZ2y2WonU8tEsjnruCd) and the kernel reviewer
(req_011CegZYyNwPmpeJGYZb3gm7). Three subagents died mid-flight (fold-diff round-9 Opus review,
MEM-003 round-6 Sonnet builder with four commits landed and gates NOT run, kernel round-6 Opus
re-check). **The main loop was silent too**: every heartbeat and usage-watch prompt from 13:03Z to
19:03Z queued unanswered — about 35 heartbeats and 5 usage watches, **no pulse refresh for six hours**
(pulse file still reading 13:02Z at 19:03Z). Sol/codex was unaffected; the census round-11 verdict
landed at ~13:05Z and sat unread until 19:05Z. All three lanes were relaunched from committed state at
19:03Z. The lesson went to the memory file `claude-session-limit-kills-subagents`: there is a WEEKLY
limit as well as the 10:00Z session limit, it kills the main loop's own turns, and **the pulse file's
staleness IS the signal** — the mayor's 10-minute watch was the only thing that could have noticed.

## 1. Headline

**The cord got pulled and it caught a live remote-code-execution hole on `main`** — caller-supplied
`:dir` executed through `sh -c`, reproduced three ways at 11413f2 and not reproducible any way at
811f4b0 — **acknowledged by the mayor 9 minutes later with a release-lane freeze granted, and a
Gene-prioritised roadmap bead (clj-surgeon-0me) hard-gated behind the fix.** Around it, **twelve
branches now sit at GO and unmerged**, including both memory endpoints reached today (MEM-003 at
95b0881, kernel B1's merge point moved to 5a2d254).

*Events to the contrary:* **no merges have been observed from the mayor** since kondo and routing-doc
— including the andon fix, so the freeze is still on; census is 13 executed reviews deep and still
NO-GO; the fold-diff production pin has not moved off 347fe6d3 in five rounds of trying; the kernel is
GO but **adopted by no verb**; and the seat was invisible for six hours in the middle of the day.

## 2. Wins vs native

No vs-native cohort ran today. E3 (fan-out verb) and E6 (study-ops free-choice adoption) were
pre-staged at 04:55Z (712a828, 1,234 lines); their apparatus prerequisite is satisfied — apparatus GO
cohort-ready at **77e6237**, Sol round 10, 13:01Z — so **the only remaining gate is the mayor merging
q5z, read-path-memory and parser-admission**, not further fix rounds. Named gap: **no fresh
wins-vs-native numbers this cycle**; the standing wins (rf2 243 s vs 336 s wall; the q5z anchor 228 s
vs 283 s wall) are unchanged from the 2026-09-02 peek report and were not re-measured today.

Two numbers from the program that are explicitly **not** vs-native comparisons (same tool, old pattern
vs fixed pattern), listed here only as labelled asides:

| aside | old | new | receipt |
|---|---|---|---|
| MEM-015 read-path single parse, 1,000 files (wall / allocation) | 11,784 ms / 25.28 GB | 6,621 ms (−43.8%) / 14.95 GB (−40.9%), outlines byte-identical, 0 mismatches over a 160-file differential | `2026-09-03-mem-015-sol-review.md` |
| MEM-003 streaming `cli-ls-tree` at N=10,000 (held / peak / wall) | 94.0/93.6 MB, 430/424 MB, 4,213/5,021 ms | 9.5/9.4 MB, 243/264 MB, 658/769 ms; Sol's independent control 93.45 MB retained-batch vs 9.35 MB streamed | captain's log 08:55Z, 09:22Z |
| MEM-003 battery line at the merged sha (branch baseline 8075db0 → 6625b7d) | `cli-ls-tree held-scales-with-n {:observed 94.0, :limit 11.5}` present; `peak-scales-with-n 433.2/333.3`; wall 7,547 ms | both lines GONE; wall 1,003 ms | captain's log 19:52Z |

## 3. Losses vs native

None — no vs-native runs executed today, same gate as §2. An empty table here is a claim, so it is
stated plainly: **nothing was measured against native today, so nothing can be claimed either way.**

## 4. Exactly what the win is

**Mechanism:** the B1 disk-journaled transaction kernel (MEM-006/007/012–014/020) replaced the
frozen-read buffering pattern with a byte-budgeted heap plus a disk journal, so the identical 600-file
workload that terminates with `java.lang.OutOfMemoryError` at `-Xmx256m` under the old pattern now
commits at the same `-Xmx256m`; at the round-8 merge point the gate reads `memory-red RED OOM → GREEN
parity 55423110… retained-peak 14.9 MB at 256m`. *(The reduction multiple is not stated in any receipt
— only the two figures are, so no ratio is quoted here.)*

**Boundary:** the kernel is GO-WITH-FIX at 5a2d254 and **adopted by no verb** — `with-cooperating-writes`
has no call sites, and round 9 is pre-adoption hardening, not adoption. Each round's fix has opened one
narrower window than it closed (unbounded read-judge-delete → two renames → a check-then-act restore →
a tombstone inheriting the age of the lock it broke → a marker whose forgery *deletes* evidence →
188 of 240 concurrent lines typing `:evidence :retained` for a file that is not there).

## 5. Surprises

- **A pagination reviewer found a remote-code-execution hole on `main`.** MEM-003 round 8 was a
  confirm-only re-check of a NUL-delimiter one-liner; its item 11 was `sh -c` on caller input in
  `core.clj:174-178`, outside the diff, pre-existing, and shipped.
- **The intent audit certified a deliberately reintroduced vulnerability.** A `format`+`sh -c` site
  put back into `src/` with every `@spec` marker intact still printed `AUDIT-OK= true violations= []`
  — traceability rows cannot enforce a structural absence.
- **A gate's verdict was a function of ambient state.** The fold-diff self-test passed 22 cases on the
  builder's tree (95-row store log) and went red at row 13 on the reviewer's (5 rows) **from one
  commit** — and the unit suite itself appended the 5 rows that left a clean checkout below the
  invisible 80-row threshold.
- **A fix that closed a tampered-row case broke an untampered one.** MEM-003 round 6: on a tree with a
  directory named `src/mydir.clj`, page 3 refused with "a false statement, two innocent files lost,
  pagination permanently unfinishable" — while the suite stayed green at 778/6322/0.
- **The weekly limit is a different limit.** 429 at ~13:03Z killed three subagents *and* the main loop
  for six hours; ~35 heartbeats and 5 usage watches queued unanswered and the pulse file sat at 13:02Z
  until 19:03Z. Sol/codex kept working the whole time.

## 6. Learnings crystallized — the five that became ratchets or memories

1. **Source text is not execution — a verifier must record what RAN.** (apparatus rounds 8 → 9 → 10.)
   Round 8's checker re-derived an idealised command; round 9's parsed tally lines out of
   `BASH_SOURCE`, so an exact tally line inside an **inert quoted heredoc** was accepted — a visible
   `FAIL case35d` still scored `386 passed, 0 failed`, rc 0. **Ratchet (rung 5, shipped at 77e6237):**
   the tally is a *runtime function* `tally <id>` appending `<id> <ok> <fail>` to a `$WORK/tallied`
   ledger; case 45 compares the ledger against an awk recount of the `.out` files and **reads no source
   text at all**. Sol round 10 confirmed it against twelve attacks, including the decisive
   inert-heredoc decoy (`383 passed, 4 failed` — round 9 was green on exactly this). Source: captain's
   log 11:40Z, 12:19Z, **13:01Z**.
2. **Witness through the production path, not the seam that is easy to reach.** (MEM-003 round 6.)
   The round-6 brief said "RED witness first" but let the builder pick the *tamper* seam, so a
   confinement gate was proven only on tampered rows — and broke discovered ones: *"on an untampered
   tree containing a directory `src/mydir.clj` … page 3 refuses `:unconfined-manifest-row` … a false
   statement, two innocent files lost, pagination permanently unfinishable, while an unbounded scan of
   the same tree still serves all 13 records."* **Ratchet:** every confinement witness was re-done on a
   **DISCOVERED** row (round 7), and rounds 7 and 8 each attacked the production entry point
   (discovery) rather than the state store — which is how the `find`-newline parse defect surfaced at
   all. Source: captain's log **19:22Z**, 19:52Z, 20:02Z.
3. **Ambient state is an invisible precondition — a gate that needs it must build it or refuse.**
   (fold-diff rounds 10 → 11.) Reviewer: *"asserts only that the log EXISTS, while case 13 rows 13/16
   need >80 rows … as found (5-line log) → ✗ row 13 (exit 0), GATE5_EXIT=1; after `make seed-demo`
   (149 lines) → ✓ 22 cases, all green."* Builder, round 11: *"the reviewer's tree had 5 rows and went
   red at row 13, mine had 95 and went green, from one commit. The gate's verdict was a function of
   ambient state."* **Ratchet (round 11, 3d344432):** the suite builds its own 120-row log under
   `$TMP` from a committed fixture — 25 cases green with `data/` **ABSENT** — and a `LOG=` override
   below 80 rows exits 3 with a **named PRECONDITION**. Source: captain's log **20:29Z**, **21:34Z**.
4. **Check-then-act on a filesystem is a data-destroyer; use an atomic primitive that fails.**
   (kernel rounds 4 → 5 → 6.) `break-lock!`'s restore was `statx(target) = -1 ENOENT` then `rename(2)`:
   **129 of 29,012** third-party claims clobbered. **Ratchet:** `Files/createLink(LOCK, tomb)` +
   `deleteIfExists`; round 6's independent re-run — *"my 20,000-break storm gave the hammer 1 window,
   vs 9,948 in round 5; 0 clobbered, displaced-claim-count 0"* and *"4,000-race: 0 destroyed vs 13,
   3,974 typed `:tombstone-exists`"*. **Memory added:** `jvm-file-locks-are-per-process`. Source:
   captain's log 11:34Z, 12:27Z, **19:18Z**.
5. **A witness that cannot see its own subject reports a true number about the wrong thing.**
   (census round 9, and again in the andon.) The filesystem-call counter at
   `mcp_relation_census_test.clj:877` wrapped functions *downstream* of `forms/init-from-file!`, so it
   certified "zero filesystem work before validation" while `bb` was stat-ing the workspace. **Ratchet:**
   the counter wraps the **first** filesystem touch on *both* entrances, with a liveness control each
   and a hand `strace` receipt, and the old witness was kept and annotated — *"Deleting a true witness
   is not the fix; making it stop over-claiming is."* The andon's REQUIRED item 1 is the same lesson at
   a different altitude: **an audit that checks marker PRESENCE cannot enforce a structural absence**;
   the ratchet for "no shell anywhere" is a test over the source, not a traceability row. Source:
   captain's log 11:19Z, 12:47Z, **21:07Z**.

*Memories added today:* `cursor-mac-needs-an-unpublished-secret` (MEM-003 round 2),
`jvm-file-locks-are-per-process` (kernel round 3), and the weekly-limit amendment to
`claude-session-limit-kills-subagents` (19:04Z).

## 7. Best news / worst news

**Best:** **the andon cord worked end to end, on its first real use, on a defect nobody was looking
for.** A reviewer assigned to pagination reported a shipped RCE outside its diff; the seat reproduced
it by hand at both shas before pulling; the mayor confirmed independently in 9 minutes, granted a
scoped release-lane freeze that left repair authority with the puller, refused to merge on the seat's
say-so, took ownership of the 7888 blast radius, and **hard-gated a Gene-prioritised roadmap bead
(clj-surgeon-0me) behind the fix** — *"Your cord did not just catch a defect on main, it caught one on
the roadmap."* The fix branch was reviewed adversarially, the review found two more real items
(a blind audit and a shadowed `format`), and both landed before it was handed over.

**Worst:** **the queue is still the bottleneck, and now a security fix is in it.** Twelve GO branches
are unmerged and none has moved all day; the andon fix at 32c0c7f is the merge that lifts the freeze
and it is waiting on the mayor's probe; E3/E6 cannot start on merges that have not happened. Second
worst: the seat was **dark for six hours** on a weekly quota nobody was watching, and the only
detector that would have caught it is the mayor's 10-minute watch reading a stale pulse file.

## 8. Board (Pacific — Gene is PDT, UTC-7; the box is UTC)

- **Running now** (three lanes, all subagents of this seat; worktree tips in the 21:35Z resume delta):
  census round-14 Sol re-check (d338554); kernel round-9 Opus builder (from 5a2d254); fold-diff
  round-11 Opus review (3d344432 — asking whether the tip may replace the 347fe6d3 pin).
  **Closed today:** MEM-003 (GO at 95b0881), apparatus (GO at 77e6237).
- **Lands next:** the mayor's independent probe + merge of the **andon fix 32c0c7f** (lifts the
  freeze), then the twelve GO branches (§0b). E3/E6 cohort runs unblock the moment q5z,
  read-path-memory and parser-admission are actually on main.
- **When:** the 9-hour night order ended ~06:34 PDT (~13:34Z) inside the outage; the seat has run on
  its own since 12:03 PDT (19:03Z). Report written 14:43 PDT (21:43Z). Weekly Claude limit resets
  12:00 PDT (19:00Z) and was hit once today. Heartbeat every 10 min to `/tmp/anvil-pulse.txt` carrying
  "ANDON open"; usage watch hourly.

## 9. Decisions waiting on Gene

- **inb-d27b79 — the ANDON.** No decision is required from you: the mayor acked, granted the freeze,
  and owns both the merge and the 7888 blast radius. It is here so you see it before anyone tells you
  about it. Recommend: nothing to do unless the mayor's probe has not landed by morning, in which case
  the freeze holds and `clj-surgeon-0me` stays gated.
- **Curtain-call merge order** — fold → store → settings-lens → template-upsert → lens-followups.
  `template-upsert` is GO (25b98a83), `lens-followups` is ready (934716dc), `fold-diff-tool`'s
  production read is GO at 347fe6d3; the order itself still awaits your ruling. Recommend: fold after
  the mayor's fold-diff production run at the pinned 347fe6d3, store next, then the lens stack.
- **claude-skills PR #1** (sol-yolo bundled into the codex skill) — ready; not actioned in today's log.
  Recommend: merge — it is the fix every Sol launch today depended on.
- **inb-3a9818** (production ops, fold-diff-checkpoint) — GO with conditions at 347fe6d3 (08:29Z); the
  pin did **not** move today (rounds 6, 8, 10 all refused to bless the tip; round 11 in review).
  Recommend: accept, retaining the full receipt.
- **inb-78e75c** (the "two public tools" invariant in the one-compiler plan, before census merges) —
  carried forward from 2026-09-02, still open. Recommendation stands: amend to "read tools compose
  through inspect; write tools stay gated."
- **inb-041b28** (the announce UI has no unannounce control) — carried forward from 2026-09-02, still
  open, still a product call.
- Filed for awareness, no decision required: inb-1165ce (night orders + the boundary incidents + the
  ANDON note), inb-46f90f (usage-watch contamination, §0d), inb-75aaf7 (find expression-start tokens),
  inb-1f9a27 (nonexistent `:file` → untyped `:invalid-arguments`), inb-00d296 (prepared-wire readiness
  race), inb-eca3b1 (`System/exit` inside the ls-tree op), inb-ef6dd6 (MEM-005's admission refusal
  embeds an absolute path), inb-114faa (tagged-literal / hat-meta towers score depth 1), inb-07c5e7
  (`rename.clj`'s own `z/of-string` left ungated on purpose), inb-276378 (MEM-003 cursor identity).

## 10. Answers to last peek's questions (from the 2026-09-02 peek report, §10)

- *What model?* Still Fable 5.1 for the seat; Opus and Sol (codex) do the reviews. Seven lanes are now
  Opus-first because OpenAI's content filter refuses our own symlink/path-confinement fixtures — rf2
  ×2, study ×2, MEM-005, q5z, the kernel, MEM-003's cursor brief, and (new today, 12:58Z) fold-diff
  round 9: *"flagged for possible cybersecurity risk"*.
- *What host?* Still Anvil, user forge, 16 cores. Note the new constraint that is **not** the box: the
  seat's own Claude **weekly** quota, which took it offline 13:03Z–19:03Z.
- *Crank up parallelism?* Three lanes running now rather than five, by completion not by throttling —
  MEM-003 and the apparatus both closed at GO today. `~/bin/suite-run` (three lanes) is still the suite
  route; the memory battery and memory-red keep the exclusive lock on purpose.
- *Friction ledger?* Still ratified practice — five ratchets/lessons today (§6), each with the exact
  text, a rule, and a trigger, plus three memories.
- *On deck / exploring / option value?* E3/E6 are pre-staged and their apparatus prerequisite is
  satisfied at 77e6237. The gate is purely the merge of q5z, read-path-memory and parser-admission —
  and, ahead of all of them, the andon fix that lifts the freeze.
- *Prosecution list in a trusted place?* The twelve inbox items from 2026-09-02, joined by ten filed
  today (§9), of which inb-d27b79 is the incident record.

## Caveats — what was NOT verified

Not verified today, and not claimed:

- **Gene's physical receipts** — no device, lock-screen, route or speaker evidence was produced; none
  was in scope.
- **Any merge** — no merge has been observed from the mayor since kondo and routing-doc. Every "GO"
  here is a review verdict, not a landing, and that includes the andon fix.
- **The memory battery's two pre-existing failures**, which still stand: `rename-ns-plan-full-match`
  9.8/3.0 and `workspace-sources-read-all` 40.8/6.5 (captain's log 19:52Z; the 12:53Z refresh recorded
  40.9/6.5 from the earlier run), both pre-existing at ancestor a9d2d4b, so every battery run ends
  `FAIL (INCOMPLETE)` exit 1 by inheritance, not by regression.
- **The 7888 blast radius** — whether 7888's callers can pass an arbitrary dir string into
  `find-build-files` is **unverified by this seat** and is the mayor's, by their own ruling. The
  service details in this report (user `surgeon`, `/srv/fleet/shared-tools/clj-surgeon-e7f72e2`, sha
  e7f72e2 dated 2026-08-25, dev-a/dev-b/dev-c configs) are quoted from the mayor's file, not measured
  here; this seat has never contacted 7888.
- **The fold-diff round-11 verdict** — the review is running; the row in §0c states the builder's own
  claim, not a reviewer's finding.
