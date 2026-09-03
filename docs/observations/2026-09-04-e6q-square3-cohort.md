# E6-Q — square 3 measured on its own terms: **native scored 6 of 6, and so did everyone else**

*Run on Anvil as `forge`, locally, no ssh. Cohort closed 2026-09-03T23:24:34Z; written 2026-09-03T23:26:53Z.
Pre-registration: `/home/forge/tmp/arms/e6q/preregistration.md` (sha256
`b39966befca0327b1d230dd9b285b7fd130e908661ac5304ac897b42fe8713ec`), frozen before the first
arm. Design from the E6-Lb result poll: Opus **O1** and Sol **D**. Full table:
`/home/forge/tmp/arms/e6q/E6Q-table.md`.*

**The headline.** Nine arm-runs, three arms, six decoy-laden structural probes each, graded by
exact equality against a ground truth frozen before any arm ran. **Every arm scored 6 of 6.**
Native `rg`-plus-reads answered every question the square was built to own — including the
local-shadow, quoted-data, string, comment and reader-conditional decoys — in **2 to 3 tool
calls and 32 to 41 seconds**. The mandated `inspect_clojure` arm reached the same answers in
**8 to 11 tool calls and 144 to 151 seconds**.

| arm | run | wall s | returns | tool calls | inspect calls | exact | load |
|---|---|---|---|---|---|---|---|
| N | 1 | 32 | 3 | 2 | 0 | **6/6** | 6.08 |
| N | 2 | 41 | 4 | 3 | 0 | **6/6** | 7.32 |
| N | 3 | 41 | 3 | 3 | 0 | **6/6** | 5.95 |
| M | 1 | 148 | 5 | 13 | **10** | **6/6** | 4.05 |
| M | 2 | 144 † | 5 | 13 | **11** | **6/6** | 10.20 |
| M | 3 | 151 | 4 | 10 | **8** | **6/6** | 5.94 |
| F | 1 | 60 | 4 | 4 | **0** | **6/6** | 5.23 |
| F | 2 | 67 † | 4 | 5 | **2** | **6/6** | 8.92 |
| F | 3 | 47 † | 3 | 2 | **0** | **6/6** | 8.96 |

† wall VOID: load at arm start above the pre-registered ceiling of 8 (this box was shared with
the E6-C cohort). Reported, never claimed.

**One line of learning:** *the square assumed a capability gap that this caller does not have.*
Square 3 is built on the premise that an agent with only text search answers structural
questions wrongly — that it counts the shadowed local, the commented-out require, the quoted
form and the dead reader-conditional branch. A grep-shaped answer really does score **1 of 6**
against this truth set; the grader was self-tested on exactly that answer before the first arm.
But **gpt-5.6-sol does not give the grep-shaped answer.** It reads the five small files and
answers structurally, because reading a 36-line file is cheaper than reasoning about what a
regex would have missed. The decoys were aimed at a failure mode the caller does not exhibit.

**One caveat, and it is the one that decides whether this closes the square:** the subject is
five small planted files (12–36 lines each). The correctness gap the square predicts may still
exist at a scale where reading every candidate file is *not* cheaper than a structural query —
a 3,700-line `reducer/core.clj`, a symbol with 200 textual hits across 30 files. **This cohort
rules on decoy-laden questions in small files, and on nothing larger.** What it does rule out
is the cheap version of square 3: the decoys alone do not create a capability gap.

## The verdict against the pre-registered pass line

**PRIMARY: FAIL**, by its second clause, and the failure is a ceiling effect rather than a
shortfall.

> PASS was: `M ≥ 5/6 exact in ≥ 2 of 3 runs` **AND** `(M − N) ≥ +1 in ≥ 2 of 3 mirrored pairs`.

- Clause 1 **PASSES**: M scored 6/6 in **3 of 3**.
- Clause 2 **FAILS**: M−N is **[0, 0, 0]** across the three mirrored pairs, **0 of 3**. With N at
  the ceiling, no clause-2 pass was reachable. That is a legitimate pre-registered outcome and
  it is the finding, not an apparatus fault: the comparator was supposed to be beatable.

**SECONDARY — the first non-zero free-choice adoption in this program.** F adoption is
**1 of 3**: `F-2` issued two `inspect_clojure` calls (one `ls-tree`, one `outline` request of
three operations) before answering, and scored 6/6. F-1 and F-3 declined. The program's
free-choice record moves from **0 of 13** to **1 of 16**. One call in three runs on a
read-only structural task is not adoption, but it is the first time the null has been broken,
and the shape is worth keeping: the one arm that reached for the tool reached for a *table of
contents plus an outline*, not for a search.

**SECONDARY — cost.** Against the E6-Lb floors (wall **172 s**, non-test actions **6.1**): the
M−N wall gap (~110 s) is **inside** the 172 s floor and is **not claimable**. The M−N
tool-action gap (N 2–3 vs M 10–13) is **≈ 9, outside the 6.1 floor, and is claimable**:
**the mandate costs about 4× the tool actions for zero correctness gain.** This reproduces the
2026-09-02 measured finding that a Clojure agent told the tool is expected pays a layering tax,
now on the read side and on a task the tool was designed for.

Two of the three M arms also spent calls on **`prepare-change`** — a *write*-preparation verb —
on a task with nothing to write. That is a routing defect worth one bead: a mandate to use a
tool sends the agent through the tool's whole surface, not through its relevant part.

## Predictions, scored

| source | prediction | measured | verdict |
|---|---|---|---|
| Opus O1 | N median **4/6** | 6/6 | **WRONG** |
| Opus O1 | M median **6/6** | 6/6 | **RIGHT** |
| Opus O1 | M−N clause clears at 55% | 0 of 3 | **WRONG** |
| Opus O1 | F adoption **0 of 3** at 80% | **1 of 3** | **WRONG** |
| Sol D | F exact **3/3** | 3/3 | **RIGHT** |
| Sol D | N exact **≤ 1/3** | **3/3** | **WRONG** |

Both seats were wrong in the same direction and about the same thing: **they modelled the
native arm as grep-shaped.** Opus put N at 4/6, Sol at ≤1/3; N came in at 6/6, three times.
Opus's own O1 anticipated this exact case in its "one reason it is waste" clause — *"if native
scores 6/6, square 3 is empty and should be withdrawn like the single-edit square"* — and that
clause is now the finding. Note also that Opus's F-adoption prediction was the one it had
scored **right** five times running (0 of 13 before tonight) and it is the one that broke here.

## Apparatus: the ratchet Opus asked for, delivered and proven

E6-Lb's null was *credible* rather than *proven* because `sol-yolo`'s `-c` path omits
`required = true`, so a failed MCP connection would have been silent. This cohort closes it:

1. **Every F and M arm ran under `required = true`**, written by `sol-yolo`'s config-file path
   (a placeholder `.codex/config.toml` is planted in the arm worktree before the driver, which
   `sol-yolo` then rewrites with the arm URL **and** `required = true`).
2. **A dead-port negative control proves `required = true` is live**, not decorative. Pointed at
   an unused port, codex exec **refuses to create the session**:
   `Failed to initialize session: required MCP servers failed to initialize: clj-surgeon:
   handshaking with MCP server failed`, rc 1, zero returns. Therefore **any arm that produced
   returns had a working MCP session** — which upgrades F-1 and F-3's zero-call runs from
   "credible decline" to **proven decline**.
3. **Per-arm server telemetry** carries `event: tool.call` with `run_id` equal to the arm id, so
   every inspect call in the table is bound to its own arm. No arm is VOID for adoption counting.

## Deviations, every one of them

1. **The tool port moved to 7911 for three pre-flight arms, then back to 7909.** When the
   wiring server was stopped, the **E6-C cohort seized 7909 within seconds** (a live server,
   pid 3156361, bound to `/home/forge/tmp/arms/e6c/e6c-LbB-F-1/worktree`). Rather than signal a
   process this seat did not start, the cohort was rebound to 7911. The coordinator then ruled
   that this seat's ports are 7906–7910 only and that arms must serialise with E6-C via
   `flock /home/forge/tmp/arms/arm.lock`. **The three 7911 arms were VOIDED and re-run on 7909
   under the lock**; the java server on 7911 (pid 3285011) was this seat's own F-1 arm server and
   had already been stopped by the apparatus's `stop-server.sh` before the ruling arrived —
   nothing was left running and no foreign process was ever signalled.
   **The voided pre-flight results are reported here so nothing looks selected:** N-1 6/6
   (3 calls, 41 s), M-1 6/6 (16 calls, 14 inspect, 156 s), F-1 6/6 (4 calls, **0 inspect**, 61 s).
   They agree with the reported cohort in every respect, including the F decline.
   Artifacts kept at `/home/forge/tmp/arms/e6q/preflight-7911/`.
2. **The mandated arm is spelled `T` on disk.** `run-arm.sh` accepts only `N`, `T` and `F`;
   `T` is the apparatus's existing name for the tool arm. It is labelled **M** everywhere in the
   tables. Arm directories are `e6q-Q-T-{1,2,3}`.
3. **The A.8 ritual-strip block is present verbatim in all three prompts, with NO substitution.**
   A.8's two allowed substitutions are rung-L test commands and this task runs no suite, so the
   block was left in its committed `bin/fan-test` form. It therefore refers to `apply_patch` and
   a load check that do not exist here — **identically in all three arms**, which is what makes
   them comparable. Recorded because a reader must not take it as a live instruction.
4. **Per-arm `CODEX_HOME` had to be seeded with `~/.codex/auth.json` (mode 600)** by the cohort
   runner. This is E6-Lb deviation 5's first bullet and it is *not* in the apparatus: the first
   N-1 attempt died at `401 Unauthorized` with zero returns, and `score.py` correctly refused to
   write a receipt (`SCORE-ABORT zero-returns`).
5. **The subject is a planted decoy set, not stock `ab267f9`.** Base
   `ab267f9371201fe92b77b3aee2f207b1244d79e2` plus one commit
   **`5b4e928f6a8b8c5f6ff86441d532b33e1f3fe9c1`**: five new files under
   `src/marvin_voice_remote/` (`pulse.clj`, `pulse_sink.cljc`, `pulse_admin.clj`,
   `pulse_report.clj`, `pulse_auth.clj`) carrying Sol's five decoy kinds, plus one comment line
   each in `channel.clj` and `reducer_lab.clj` so a repo-wide `rg` returns cross-file noise.
   Every arm cloned the same sha.
6. **Ground truth is machine-derived, not hand-typed, for five of six probes.**
   `truth/derive.clj` (babashka) reads the forms with `read` under
   `{:read-cond :allow :features #{:clj}}` and strips `quote` subtrees, so strings, comments and
   quoted data are excluded **by the reader**. `:p2` (the classification probe) is hand-built and
   cross-checked line by line. `truth.edn` sha256
   `b95818c2b9fd1b6f86c1bee1f9c564a211306183a77ca4ca9cd3a02fa668ccea`, frozen 22:53:11Z —
   **before** the first arm.
7. **The grader's canonicalisation was pre-registered and is narrow:** a sequential collection
   where the spec names a set becomes a set; a name written as a string where the spec names a
   symbol becomes a symbol; integers compare by value. Nothing else. A keyword is never coerced
   to a symbol. Self-test before the first arm: a perfect answer **6/6**, a naive grep-shaped
   answer **1/6**, a missing file **0/6 with a typed reason**.
8. **Prompts are byte-identical outside §5 TOOLING**, asserted mechanically (shared-prefix
   sha256 `b4bacea575da088252d6c1f77f8a3977e4b0c9b73c3fa49473d9c70968ba1f0c` for all three).
9. **Load exceeded the pre-registered ceiling of 8 at three arm starts** (M-2 10.20, F-2 8.92,
   F-3 8.96) because E6-C was running on the same 16 cores. Those three walls are **VOID**; the
   correctness numbers are unaffected, and no wall claim is made anywhere in this document.
10. **`/home/forge/bin/trust-dir` was run on every directory this session created.**

## What this says about the program

`docs/vision.md` says *"free-choice adoption is the acceptance test."* Square 3's claim was that
a mandate would at least be **worth** granting — that the capability is real even if the routing
is not. On this subject, with this caller, **it is not**: the capability gap is zero, the
mandate costs 4× the actions, and the one free-choice arm that used the tool would have been
right without it.

The honest next question is not another routing lever. It is **whether the gap appears at
scale**: the same six probe shapes against `reducer/core.clj` (3,720 lines, 100 forms) and
`channel.clj` (3,694 lines, 167 forms), where "just read the file" stops being two tool calls.
If native still scores 6/6 there, square 3 should be withdrawn on the evidence.
