# E6-Q2 — square 3 at scale: **native scored 6 of 6 on the big files too, in three tool calls**

*Run on Anvil as `forge`, locally, no ssh. Cohort closed 2026-09-04T00:04:28Z; written 2026-09-04T00:08:08Z.
Pre-registration: `/home/forge/tmp/arms/e6q2/preregistration.md` (sha256
`08509014c48d71899c188daceda5c93f195732fd8dd5f47ae14e34466c3839b4`), frozen before the first
arm. Full table: `/home/forge/tmp/arms/e6q2/E6Q2-table.md`. Predecessor:
`docs/observations/2026-09-04-e6q-square3-cohort.md`, whose closing caveat this cohort exists
to settle.*

**The headline.** E6-Q measured square 3 on five small planted files and every arm scored 6/6,
and it closed with one honest reservation: *"the correctness gap the square predicts may still
exist at a scale where reading every candidate file is not cheaper than a structural query — a
3,700-line `reducer/core.clj`… If native still scores 6/6 there, square 3 should be withdrawn on
the evidence."* This cohort ran exactly that experiment: the same six probe shapes, the same six
decoy kinds, planted **inside** `reducer/core.cljc` (3,780 lines) and `channel.clj` (3,736
lines), scattered from line 92 to line 3,695 of one file and line 39 to 3,408 of the other.
**Every arm scored 6 of 6 again — and the native arm did it in three tool calls, flat, in all
three runs.**

| arm | run | wall s | returns | tool calls | inspect calls | of which refused | exact | load |
|---|---|---|---|---|---|---|---|---|
| N | 1 | 64 † | 3 | **3** | 0 | — | **6/6** | 9.31 |
| N | 2 | 62 † | 4 | **3** | 0 | — | **6/6** | 14.38 |
| N | 3 | 64 † | 4 | **3** | 0 | — | **6/6** | 11.25 |
| M | 1 | 158 † | 5 | 10 | **8** | 3 | **6/6** | 9.41 |
| M | 2 | 160 † | 7 | 13 | **11** | 5 | **6/6** | 9.03 |
| M | 3 | 215 † | 4 | 18 | **15** | 8 | **6/6** | 9.08 |
| F | 1 | 101 † | 4 | 6 | **3** | 2 | **6/6** | 11.42 |
| F | 2 | 78 | 4 | 5 | **1** | 1 | **6/6** | 6.86 |
| F | 3 | 143 † | 4 | 8 | **4** | 3 | **6/6** | 18.19 |

† wall VOID: load at arm start above the pre-registered ceiling of 8 (this box was shared with
another cohort). **Eight of nine walls are void** and no wall claim is made anywhere below.

**One line of learning:** *the scale hypothesis was wrong about the strategy, not about the
size.* Square 3 assumed that at 3,700 lines an agent must choose between reading the whole file
(expensive) and text-searching it (wrong). **It chooses neither.** `N-1`'s three calls were: one
`rg` for the symbol family batched with a first orientation read; then
**`nl -ba <file> | sed -n '1,102p;235,252p;665,682p;995,1012p;1614,1627p;2042,2058p;2584,2594p;2968,2981p;3418,3432p;3747,3757p'`** —
ten numbered windows on exactly the line ranges the search had just named, both files in one
call; then `apply_patch` writing `answers.edn`. It read roughly 300 of 7,516 lines and answered
every structural question correctly. `N-2` used the same shape with `rg -n -C 3`. **File size
never enters the native cost function, because native never reads the file.** "Just read the
file stops being two tool calls" was true; it was also irrelevant, because reading the file was
never the alternative on offer.

**One caveat.** This measures **one caller** (`gpt-5.6-sol` at high reasoning effort) on
**read-only structural questions with the line numbers or the symbol name supplied**. It does
not measure a task where the agent must first decide *what to look for*, and it does not measure
editing. Square 3 was defined as the read-side square, so this closes square 3 — not the tool.

## The verdict against the pre-registered pass line

**PRIMARY: FAIL**, by its second clause, and the failure is again a ceiling effect.

> PASS was: `M ≥ 5/6 exact in ≥ 2 of 3 runs` **AND** `(M − N) ≥ +1 in ≥ 2 of 3 mirrored pairs`.

- Clause 1 **PASSES**: M scored 6/6 in **3 of 3**.
- Clause 2 **FAILS**: M−N is **[0, 0, 0]**, **0 of 3**. With N at the ceiling no clause-2 pass was
  reachable — the same shape as E6-Q, now with the comparator's advantage removed.

**THE PRE-REGISTERED DECISION FIRES.** Written into the pre-registration before the first arm:

> *If **N scores 6/6 in ≥ 2 of 3 runs**, **square 3 is WITHDRAWN on the evidence for this
> caller** — the same disposition the single-edit square received.*

**N scored 6/6 in 3 of 3. Square 3 is WITHDRAWN.** It was written down in advance precisely so
that a second null could not be re-narrated afterwards as "the questions were too easy": the
questions are the ones the square itself proposed, on the files the square itself named, with
the decoys the square itself specified, graded against a truth frozen before any arm ran.

## Secondary: the free-choice null finally broke, and it broke at scale

**F adoption is 3 of 3** — every free-choice arm reached for `inspect_clojure` unprompted. The
program's free-choice record moves from **1 of 16** to **4 of 19**. This is the first cohort in
which free choice adopted the tool in *every* run, and the difference from E6-Q (1 of 3 on small
files) is exactly the variable this cohort changed. **The shape of what they reached for is the
useful part:** F-1 and F-3 asked for `outline` plus `match` — a table of contents and a
structural search over a file they did not want to open. Not one F arm asked the tool to make a
change; all three then answered from their own reads anyway.

**But adoption did not become dependence.** F-2 issued exactly one tool call, a
`prepare-change`, it was **refused**, and F-2 still scored 6/6 — the tool contributed nothing to
that arm's answer and the arm was perfect. That is the cleanest single-arm statement of this
whole cohort.

## Secondary: **52% of `inspect_clojure` calls were refused**

22 of 42 calls across the six MCP arms came back `ok: false`:

- **`invalid-mcp-request` × 12** — a missing `expect` block, or an argument at the wrong nesting
  level (`file` at the request root instead of inside `requests`). The coordinator hit the same
  two refusals by hand in the pre-flight smoke test, on the first two calls, before any arm ran.
  The refusals are *well-typed and instructive* — they print the minimal valid shape — but they
  are a first-call tax that every arm paid.
- **`semantic-provider-unavailable` × 5** — **every `prepare-change` call in the cohort**. That
  mode needs cclsp on `127.0.0.1:7890`, which is not running on this box and which no arm could
  have started. Six `prepare-change` calls were issued on a task with **nothing to change**;
  all six failed. E6-Q flagged this routing defect as worth one bead; here it is worse, because
  the failure is a hard refusal rather than a wasted read.
- `no-clojure-files` × 4, `invalid-change-intent` × 2.

**Every arm scored 6/6 anyway.** A 52% refusal rate that costs nothing in correctness is the
signature of a tool that is not on the critical path.

## Apparatus: what makes these numbers admissible

1. **The truth was frozen before any arm.** `truth/truth.edn` sha256
   `2af31defd610a774d33ebeaa5ed9dfb5765651217908e5e3c64e4569e6a40681`, frozen 23:37:05Z, the
   first arm started 23:40:42Z. Five of six probes are the output of a babashka **reader-based**
   derivation (`read` with `:read-cond :allow :features #{:clj}`, `quote` subtrees stripped), so
   strings, comments, quoted data and the dead reader branch are excluded **by the reader**,
   never by a regex. `:p2` is hand-built and cross-checked line by line; the derivation
   independently confirms the ten pinned line numbers.
2. **The grader is byte-identical to E6-Q's** (sha256 `9f79f2554f65db…`), mechanical, never
   judgment, with the same narrow pre-registered canonicalisation. Self-tested before the first
   arm: a perfect answer **6/6**, a naive grep-shaped answer **0/6**, a missing file **0/6 with a
   typed reason**. The naive answer is the one a text-search-only agent would give — it counts
   the comment's 3-arity and the quoted 4-arity, reads the `:default` reader branch, believes
   the commented-out require and the `;; ^:private` comment, and calls every `relay-token` site a
   reference. **The decoys work; this caller is simply not fooled by them.**
3. **`required = true` on every F and M arm, proven live by a dead-port negative control** run
   before the first arm: pointed at 7909 with nothing listening, codex exec refused to create the
   session (`Failed to initialize session: required MCP servers failed to initialize:
   clj-surgeon: handshaking with MCP server failed`), rc 1, zero returns, no report file.
   Therefore **any arm that produced returns had a working MCP session**, and every declined or
   under-used tool call is a proven decline.
4. **Per-arm server telemetry** carries `event: tool.call` with `run_id` equal to the arm id, so
   every inspect call in the table is bound to its own arm. No arm is VOID for adoption counting.
5. **Prompts are byte-identical outside §5 TOOLING**, asserted mechanically: shared-prefix sha256
   `7839a4acfad387ad5e437bf822f183cd5be077b6de4b01d7206f19b3c83b436b` over the first 6,075 bytes
   of all three.
6. **Read-only compliance is 9 of 9**: `git diff` against the base is empty in every arm and the
   only untracked file is `answers.edn`.

## Deviations, every one of them

1. **`reducer/core.clj` is renamed `reducer/core.cljc` by the fixture commit.** Clojure
   **refuses** a reader conditional in a `.clj` file (`Syntax error … Conditional read not
   allowed`, verified directly), so the sixth decoy kind could not legally live in a `.clj`. The
   alternative — planting the reader-conditional probe in a small new `.cljc` file — would have
   made P3 a small-file probe again and defeated the point of the cohort. The namespace, the
   size, the content and every other property are unchanged, and the whole namespace still
   loads: `(require 'marvin-voice-remote.channel)` returns clean with `pulse-sink` =
   `:jvm-pulse-sink`, `pulse-emit` live at both arities. All six decoy kinds are inside the two
   big files; no new file carries any probe.
2. **The mandated arm is spelled `T` on disk.** `run-arm.sh` accepts only `N`, `T` and `F`; it is
   labelled **M** in every table. Arm directories are `e6q2-Q-T-{1,2,3}`.
3. **The A.8 ritual-strip block is present verbatim in all three prompts, with NO substitution.**
   A.8's two allowed substitutions are rung-L test commands and this task runs no suite, so the
   block was left in its committed `bin/fan-test` form. It therefore refers to `apply_patch` and a
   load check that do not exist here — **identically in all three arms**, which is what makes
   them comparable. Recorded so a reader does not take it as a live instruction.
4. **Per-arm `CODEX_HOME` is seeded with `~/.codex/auth.json` (mode 600)** by the cohort runner;
   without it every arm dies at 401 with zero returns. Carried over from E6-Q deviation 4.
5. **A first planting pass was discarded before the fixture commit.** The channel.clj anchor
   line numbers had been surveyed against the *E6-Q* base, which carries one extra comment line,
   so every channel insertion landed one line late — inside the following form rather than
   before it. The files still *read* (balanced parens), which is exactly why it was caught by
   **loading** them rather than by reading them: `(require 'marvin-voice-remote.channel)` failed
   with a `defn-` spec error. The base was reset to `ab267f9` and re-planted from scratch. No
   arm ever saw the discarded fixture; nothing was selected.
6. **P4/P5 are scoped to the planted `pulse-` family.** Asking for *every* public owner of a
   3,780-line file would be a transcription exercise, not a structural one. `:requires` is the
   full, unscoped require set of the `ns` form.
7. **Eight of nine arm walls are VOID on load** (only F-2 started under the ceiling of 8; the box
   was shared and load reached 18.19 at F-3's start). Correctness and tool-call counts are
   unaffected; **no wall claim is made anywhere in this document.**
8. **`prepare-change` could not work in this environment** (no cclsp on 7890). This is reported
   as a measured refusal, not corrected: it is identical across all six MCP arms, and starting a
   semantic provider mid-cohort would have made the arms incomparable.
9. **`/home/forge/bin/trust-dir` was run on every directory this session created.** The tool
   server ran on **7909 only**, started and stopped by this seat;
   7888 / 7894 / 7895 / 7906 / 7907 / 7908 / 7910 were never contacted, and no process this seat
   did not start was ever signalled.

## What this says about the program

`docs/vision.md` says *"free-choice adoption is the acceptance test."* On that test this cohort
is the program's **best** result to date — 3 of 3 arms adopted, unprompted, and reached for
`outline` and `match` rather than for a search. On the test square 3 was actually built to pass —
*does the tool make an answer possible that native gets wrong* — it is the program's **clearest
null**: nine arms, six decoy-laden structural probes each, on the two largest files in the repo,
**54 of 54 probes correct**, with the native arm at **three tool calls flat**.

Both facts are true and they point the same way. The tool is worth *reaching for* — three of
three free-choice agents did. It is not worth *mandating*: at scale the mandate cost 3.3× to 6×
the tool actions (N 3, M 10–18; outside the 6.1-action floor, so this gap is claimable) and
bought nothing, while more than half its calls were refused.

**Square 3 is withdrawn on its own pre-registered terms.** The open question is no longer whether
the tool answers read-only structural questions better than `rg` plus a windowed `sed` — it does
not, at either scale. It is whether the verbs with **no native equivalent** (`require_change`,
`:extract!`, `:rename-ns!`, `:fix-declares!`) earn their place on the write side, where "read
three hundred numbered lines and answer" has no counterpart.
