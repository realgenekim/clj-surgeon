# E6-C — a WHEN-routing plate in the tool DESCRIPTION: measured 0 of 3, exactly as bare exposure

*Run locally on Anvil as `forge`, no ssh. Cohort closed 2026-09-03T23:20:45Z. Runner root
`/home/forge/tmp/arms/e6c`. Pre-registration written before any arm ran:
`/home/forge/tmp/arms/e6c/preregistration.md` (sha256
`d546a15ffd6c0cc7eee0a82bb8468c0dd02544a9b72c98cab63e820b7fe54925`). Design: Sol's candidate C
(`/home/forge/tmp/sol/e6poll-sol.md`) with Opus's amendment that the plate belongs in the MCP tool
description, not in more prompt prose (`/home/forge/tmp/sol/e6poll-opus.md` §2 O3, §3 row (c)).
Predecessor: `docs/observations/2026-09-04-e6-lb-cohort.md`.*

## The table first

| arm | run | wall s | returns | tool calls (total / non-test) | `inspect_clojure` calls | first call at return # | plate seen? | correct | load at start |
|---|---|---|---|---|---|---|---|---|---|
| B | 1 | 123 | 4 | 8 / 8 | **0** | — | no — bare, sha `f1a094b9d96e` | green | 2.62 |
| R | 1 | 112 | 4 | 8 / 8 | **0** | — | **yes** — served, sha `ba1c09497ad3` | green | 3.67 |
| R | 2 | 114 | 5 | 7 / 7 | **0** | — | **yes** — served, sha `ba1c09497ad3` | green | 6.12 |
| B | 2 | 114 | 4 | 8 / 8 | **0** | — | no — bare, sha `f1a094b9d96e` | green | 6.62 |
| B | 3 | 162 | 5 | 8 / 8 | **0** | — | no — bare, sha `f1a094b9d96e` | green | 4.05 |
| R | 3 | 108 | 4 | 7 / 7 | **0** | — | **yes** — served, sha `ba1c09497ad3` | green | 5.23 |
| N | 1 *(reused)* | 157 | 3 | 8 / 8 | **0** | — | n/a (native) | green | 1.76 |
| N | 2 *(reused)* | 116 | 5 | 8 / 8 | **0** | — | n/a (native) | green | 4.06 |
| N | 3 *(reused)* | 119 | 4 | 7 / 7 | **0** | — | n/a (native) | green | 2.60 |

**ADOPTION: R 0 of 3 · B 0 of 3.** Not one arm in either exposure issued an `inspect_clojure`
call, in any mode, early or late.

| clause of the pre-registered pass line | required | measured | verdict |
|---|---|---|---|
| 1 | adoption(R) ≥ 2 of 3 | **0 of 3** | **FAIL** |
| 2 | adoption(B) ≤ 1 of 3 | 0 of 3 | pass |
| 3 | adoption(R) − adoption(B) ≥ 2 | **0** | **FAIL** |

**PRIMARY: FAIL.** Clause 2 holds only because both arms are floored at zero, so it carries no
information; the manipulation moved nothing. Free-choice adoption in this program is now **0 of
19** — E6-Lb's 0 of 13, plus these six.

## One line of learning, and one caveat

**Learning: the plate was demonstrably on the model's table, quoted back verbatim, and it still
did not compete with a habit.** All nine arms — plated, bare and native — opened with the same
move, `rg -n … 'System/currentTimeMillis' src`. A WHEN clause is an argument, and an argument
has to be read, weighed, and preferred; `rg` is a reflex that costs no deliberation at all. The
last cheap prompt-side lever — the one that survives when we do not own the prompt — is now
measured, and it is inert on this rung.

**Caveat, and it is a real one: rung Lb does not satisfy the plate's own precondition.** The
plate says *"when a task spans owners you have not named"*; clauses 3 and 4 of the Lb prompt
still name eight of the ten owner namespaces in prose. A model that read the plate and correctly
judged it inapplicable is indistinguishable, in this data, from a model that never weighed it.
That is exactly the confound Opus flagged when he fused the plate with a blind rung in his O3.
E6-C deliberately isolated the plate to keep one variable moving; the price is that **it rules on
the plate at rung Lb only, and does not rule on a genuinely blind rung.** The rollouts contain no
reasoning summary weighing the tool and rejecting it, so we cannot distinguish the two readings
from the evidence.

## The predictions, scored

| predictor | prediction | measured | verdict |
|---|---|---|---|
| **Sol** (candidate C) | **R 2/3 vs B 0/3**, 55% | R 0/3, B 0/3 | **WRONG on R** (right on B, which was the floor either way) |
| **Opus** (§2 O3 / §3 row (c)) | **0/3** — the plate does not fire, 70% | R 0/3 | **RIGHT** |

Opus's figure was stated for a *fused* plate-plus-blind screen at a lower bar (≥1 of 3); the
pre-registration fixed, before the runs, that he would be scored on the direction his number
asserts — that no plated run adopts early. It does not hold. Opus also predicted the mechanism
correctly in advance: *"A tool's presence and name are not a path"*, and a description sentence
turns out not to be a path either.

## The manipulation was delivered — three independent witnesses, not one

A null is worthless unless the thing that was supposed to move was actually present. Three
witnesses, and the third is new to this program:

1. **`required = true` on every B and R arm.** `codex` refuses to create a session at all when
   a required MCP server does not connect, so a run that produced model returns is itself the
   client-side proof the handshake succeeded. The negative control was run and recorded: with
   `required = true` pointed at a dead 7910, `codex exec` exits rc=1 with
   *"required MCP servers failed to initialize: clj-surgeon: handshaking with MCP server failed"*
   and zero returns. This closes Opus's §1.5 apparatus ratchet — E6-Lb's F arms took sol-yolo's
   `-c` path, which omits `required`, so a failed connection there would have been silent.
2. **A live `tools/list` against each arm's OWN server process**, taken while that arm's server
   was up and bound to that arm's worktree, recording the exact description text served:
   **R = 3798 chars, sha256 `ba1c09497ad356dd…`, plate present; B = 3558 chars, sha256
   `f1a094b9d96e19ab…`, plate absent.** Six for six, matching expectation.
3. **The model quoted the plate back.** `codex rollout.jsonl` does not persist the MCP tool
   payload, so witnesses 1 and 2 prove *served and connected*, not *read*. So a separate
   non-arm session was run against a plated server and asked to quote its `inspect_clojure`
   description verbatim without running any command. It answered:

```
2. First 60 words of `inspect_clojure.description`, verbatim:

> Batch known Clojure reads with inspect_clojure. Use prepare-change when one fully qualified Var names the goal but exact sites are unknown. Compile two or more exact edits or any cross-file decision into one apply_clojure_changes call. Do not repeat reads after read_complete=true or inspect writes after verification_complete=true.
>
> Call this FIRST, before rg, when a task spans owners you have not named
```

   The plate is in the model's context, first-class, immediately after the server instructions.
   **The tool was declined with its routing sentence in hand.**

## What was manipulated, exactly

One commit, four inserted lines, one file, on a throwaway local branch in a scratch COPY of the
E6 server source — `e6c-routing-plate` **`ac81331d95114cf5b9b23530e8861290ff382eca`**, never
pushed. It prepends to `clj-surgeon.mcp-inspect-tool/tool-description`:

> Call this FIRST, before rg, when a task spans owners you have not named or requires
> workspace-wide discovery (which namespaces define/require/call X); one ls-tree call returns
> every namespace, its requires and public forms with line spans.

Nothing else differs. **B and R are served by builds of the same source**, B at
`f24812b09dddc4c36c06cd362b9c285e555c6f2d` (the E6 server, unmodified, on 7909), R at
`ac81331` (on 7910). The prompt is byte-identical between them
(`E6-Lb-F.md`, sha256 `e4bf56b059250fb4e0d445a40937b2c77059c7d896ff6dc4d68c615aa037a897`),
including the A.8 ritual-strip block. **The result rendering is identical**: Opus's O2 rendering
fix is deliberately NOT included, because including it would confound the plate with the payload.

## Gate — green in all nine arms

`acid_L_acceptance_test` **12 tests, 82 assertions, 0 failures** · full `bin/kaocha` **577
tests, 7784 assertions, 0 failures** · goldens byte-identical (`✓ check-pages: all pages parse +
match golden`) · `grep -rn "System/currentTimeMillis" src/` prints **exactly 1 line**. No scored
arm was void; none was re-run for a gate failure.

## Secondary — nothing clears its floor

| secondary | R (plated) | B (bare) | N (native, reused) | claim |
|---|---|---|---|---|
| wall s | 108–114 (mean 111.3) | 114–162 (mean 133.0) | 116–157 (mean 130.7) | R–B gap 21.7 s, **inside the 172 s floor** — not a finding |
| model returns | 4–5 | 4–5 | 3–5 | ranges overlap, no claim |
| non-test actions | 7–8 | 8 | 7–8 | far inside the 6.1 floor, no claim |
| files read before first write | [11, 19, 11] | [19, 18, 7] | [9, 13, 17] | **ranges overlap, no claim** |
| churn +/− | +33/−23 | +33/−23 | +33/−23 | identical in all nine; E6-C has no churn pass line |

Load at every arm start was **1.76–6.62** on 16 cores, never above the ceiling of 8, so no wall
figure is void. (Load reached 10.19 *after* the last arm, during the six full-suite gate runs
sharing the box with cohort E6-Q; gates are not timed.)

**Metric note, so nobody compares the wrong columns:** the "files read before first edit" row
above is the apparatus field `receipt.reads.clj_files_before_first_write`, reported uniformly for
all nine arms including the reused N ones. E6-Lb's published table used a different, undocumented
extraction ([1, 1, 4] / [0, 4, 10]); **that column and this one are not comparable.**

## Deviations, every one of them

1. **The three N arms were REUSED from E6-Lb, not re-run**, as the brief permits. Every input is
   byte-identical: base `ab267f9`, prompt `E6-Lb-N.md` sha256 `a5ab4f0f…`, driver
   `~/bin/sol-yolo` / `gpt-5.6-sol` / `model_reasoning_effort=high`, meter
   `bench/anvil-arms 77e6237` via the same patched scratch copy, same gate sequence. The
   receipts are copied into the runner root as `e6c-Lb-N-{1,2,3}` each carrying a
   `REUSED-FROM.md` marker. N does not enter the primary pass line at all. **Six fresh arm-runs
   were made, not nine.**
2. **The first attempt at B-1 died at `401 Unauthorized` with zero returns and is preserved,
   void, as `void-e6c-LbB-F-1-noauth`.** The apparatus gives each arm a private `CODEX_HOME`,
   which has no credentials; E6-Lb seeded `auth.json` by hand and the patch never reached
   `run-arm.sh`. The runner now seeds it. That void run is still evidence for one thing: it
   reached the model endpoint, which means the `required = true` MCP handshake against the bare
   server had already succeeded.
3. **`required = true` was delivered without modifying the shared `~/bin/sol-yolo`**, because
   other cohorts were running against it concurrently. `sol-yolo` sets `required = true` only
   on its config-FILE path; `marvin-voice-remote` has no `.codex/config.toml`, so E6-Lb took
   the `-c` path. Each B/R worktree is therefore seeded with a placeholder
   `.codex/config.toml` before the driver starts; `sol-yolo` renames it to
   `.disabled-by-sol-yolo` and writes the arm's own config with `required = true`. The
   placeholder is untracked and cannot appear in `git diff <base>`. **Recorded as a difference
   from E6-Lb's F arms**, and as a small information difference visible to the agent (a config
   file naming the MCP url) — identical in B and R, so it cannot explain the null.
4. **Goldens ran with a private temp prefix from the start** (E6-Lb deviation 7, carried
   forward): `scripts/check_pages.clj` writes to hardcoded `/tmp/check-pages-*` paths owned by
   another user on this shared box, and a clean `ab267f9` baseline fails identically. Only the
   temp prefix is redirected; the comparison logic is byte-for-byte the script's own.
5. **Churn is +33/−23 in all nine arms, outside the canonical +59/−34 band**, because
   `git diff <base>` omits the new untracked `src/marvin_voice_remote/clock.clj`. All arms are
   affected identically; E6-C has no churn pass line.
6. **E6-Lb's server-provenance deviations carry over unchanged**, and a reader must not take any
   row here as a statement about `main`: the E6 server is `bridge/study-ops-mcp 4480e3d`
   merged locally with `origin/main bb4ae9af` (sha `f24812b0`), `bridge/streaming-ls-tree`
   was not merged, and C.6's "E6 runs only after study-ops is merged to main" is still not
   satisfied. R is that same tree plus four lines.
7. **A `java` server from `/home/forge/tmp/arms/e6/server-src` was seen on port 7911
   (pid 3285011). I did not start it and did not signal it.** Every server this cohort spawned is
   recorded in `<arm>/server/spawned.pid` and in `<arm>/server/ready.edn`: three on 7909,
   three on 7910, each stopped by the runner that forked it. The probe refuses any port outside
   7907–7910 by construction. The pid was already gone when checked.
8. **The extra plate-delivery witness (§ above) is a NON-ARM session** — one short `codex exec`
   against a plated server on 7910, no worktree mutation, not scored, not counted in any adoption
   figure. It is apparatus verification, recorded here because it is the strongest evidence in
   the cohort.
9. **`/home/forge/bin/trust-dir` was run on every directory this session created** (seat rule).
   Nothing was pushed from the scratch server copy; ports 7888/7894/7895/7906/7907/7908 were
   never contacted.

## What this closes, and what it does not

It closes the **prompt-side** lever list. Thirteen free-choice nulls said asking in the prompt
does not work; six more now say asking in the tool description does not work either, with proof
the description reached the model. What remains untested from Opus's ranking is **O4 — route,
don't ask**: a read-side hook that sits on the agent's existing `rg` route instead of competing
with it, and **O1 — square 3 on its own terms**, where the primary is capability rather than
choice. Both were ranked above this cohort; E6-C was the cheap screen that had to be run first
because a positive would have been genuinely new. It is not positive.

The one thing E6-C does NOT license is a claim about a blind rung. The plate's own precondition
was not met by rung Lb. If that experiment is ever run, it must move exactly one variable and
say so.
