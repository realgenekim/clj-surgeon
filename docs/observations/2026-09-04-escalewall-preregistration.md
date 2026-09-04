# E-SCALE-WALL — pre-registration (DRAFT ONLY, written and to be FROZEN before arm 1; no arms have run)

*forge@anvil, 2026-09-04. Runner root (not yet created):
`/home/forge/tmp/arms/escalewall`. This document is a pre-registration, not a result —
**zero arms have run for this cohort.** It is the one experiment that can turn tonight's
standing sentence ("for two callers on an apply_patch harness, on fan-outs of ~21 owners or
fewer, one intent replaces a hand-typed patch at 2–7× fewer emitted characters" —
`2026-09-04-gene-report-0455z.md` §1) into a **WALL-time claim at scale**, where every prior
cohort in this family (E-REG, E-AFFORD, E-CALLER, E-CEILING80, E-NSWEEP, E-HARNESS-2)
recorded wall **unconditionally and descriptively only**, because the shared box ran other
cohorts, builders, and reviewers concurrently and wall is void above load 8. **This cohort
runs ALONE** — no other cohort, builder, or reviewer on the box for its duration — so that,
for the first time in this program, wall can be the **primary** meter rather than a starred
footnote.

---

## 1. The hypothesis (one falsifiable sentence)

**The tool's wall is flat in N — one call, one server round-trip, dominated by fixed
overhead rather than fan-out size — while both callers' native wall grows with N, because
native must either type more of a literal patch, enumerate a longer file list for a
scripted stream edit, or run and often iteratively debug a generator; so at N ≥ 40 the tool
is ≥ 2× faster in WALL time than native for both callers, even at the N where E-CEILING80
and E-NSWEEP found native cheaper in emitted CHARACTERS.**

This is a claim about a different meter than every prior cohort in the family measured
primarily. Characters and wall are not the same quantity: E-CEILING80 found native emitting
*fewer* characters than the tool at N=80 in 2 of 3 arms (§1 of that document) by switching
from typing a patch to writing a generator — but writing, running, and (in one arm) three
times re-emitting a generator is not free in wall seconds even when it is cheap in
characters. That gap between the character meter and the wall meter is exactly the
one no cohort so far was positioned to measure, because none of them had the box alone.

---

## 2. The design — cells

| N | caller | harness | arm | n | prompt (existing / new) |
|---|---|---|---|---|---|
| 21 | gpt-5.6-sol via `sol-yolo` | `apply_patch` present | native | 3 | reuse E-REG/E-AFFORD/E-HARNESS-2's N=21 `P` prompt, verbatim |
| 21 | gpt-5.6-sol via `sol-yolo` | `apply_patch` present | tool | 3 | reuse the same family's N=21 `T` prompt, verbatim |
| 21 | claude-sonnet-5 via `claude -p` | native tools (Edit/Bash/Write) | native | 3 | reuse E-CALLER's `EC-N.md`, verbatim |
| 21 | claude-sonnet-5 via `claude -p` | MCP `alias_migration` configured | tool | 3 | reuse E-CALLER's `EC-T.md`, verbatim |
| 40 | gpt-5.6-sol via `sol-yolo` | `apply_patch` present | native | 3 | reuse E-NSWEEP's `ENSWEEP-N40.md`, verbatim |
| 40 | gpt-5.6-sol via `sol-yolo` | `apply_patch` present | tool | 3 | **new** — E-CEILING80's `ECEIL-T.md` with the one-hunk owner-count edit applied to N=40 instead of 80, §5 (tooling/licence) left verbatim, port reassigned (§6) |
| 40 | claude-sonnet-5 via `claude -p` | native tools | native | 3 | **new** — `EC-N.md` with the one-hunk owner-count edit to N=40, §5 verbatim |
| 40 | claude-sonnet-5 via `claude -p` | MCP `alias_migration` configured | tool | 3 | **new** — `EC-T.md` with the one-hunk owner-count edit to N=40, §5 verbatim |
| 80 | gpt-5.6-sol via `sol-yolo` | `apply_patch` present | native | 3 | reuse E-CEILING80's `ECEIL-N.md`, verbatim |
| 80 | gpt-5.6-sol via `sol-yolo` | `apply_patch` present | tool | 3 | reuse E-CEILING80's `ECEIL-T.md`, verbatim |
| 80 | claude-sonnet-5 via `claude -p` | native tools | native | 3 | **new** — `EC-N.md` with the one-hunk owner-count edit to N=80, §5 verbatim |
| 80 | claude-sonnet-5 via `claude -p` | MCP `alias_migration` configured | tool | 3 | **new** — `EC-T.md` with the one-hunk owner-count edit to N=80, §5 verbatim |

**12 cells × n = 3 = 36 arms.** Four of the twelve cells are pure reuse of already-frozen,
already-served prompts (Sol N=21/N=80 native+tool, Claude N=21 native+tool: 4 cells reused
in full, 8 arms — wait, corrected count: the fully-reused cells are Sol-N21-native,
Sol-N21-tool, Sol-N80-native, Sol-N80-tool, Sol-N40-native, Claude-N21-native,
Claude-N21-tool — **7 of 12 cells reuse an existing, already-validated prompt verbatim**;
the remaining **5 cells** (Sol-N40-tool, Claude-N40-native, Claude-N40-tool,
Claude-N80-native, Claude-N80-tool) need a new prompt authored by the same one-hunk,
±0-byte-outside-§5 method every prior cohort in this family used, and its sha256 declared
in `FROZEN.sha256` before arm 1, exactly as E-NSWEEP and E-CEILING80 did.

**Run order — interleaved by N, then caller, then arm — fixed now, before arm 1:**

```
N=21:  SolN-1 SolT-1 SolN-2 SolT-2 SolN-3 SolT-3   ClN-1 ClT-1 ClN-2 ClT-2 ClN-3 ClT-3
N=40:  SolN-1 SolT-1 SolN-2 SolT-2 SolN-3 SolT-3   ClN-1 ClT-1 ClN-2 ClT-2 ClN-3 ClT-3
N=80:  SolN-1 SolT-1 SolN-2 SolT-2 SolN-3 SolT-3   ClN-1 ClT-1 ClN-2 ClT-2 ClN-3 ClT-3
```

Every arm serial under `flock /home/forge/tmp/arms/arm.lock`, exactly as every cohort in
this program has run — but for this cohort the lock is the box's ONLY tenant (§3).

---

## 3. The quiet-box rule — stricter than the program's load-8 void, because wall is PRIMARY here

Every other cohort in this program voids a wall only when load exceeds **8** at either end,
and treats the character/strategy/call-count meters as load-immune, so a busy box never
stopped a cohort — it just starred its wall column. **That is not available here**: wall is
this cohort's primary meter, so a starred wall is a wasted arm, not a footnote.

- **Before every arm**, the runner asserts **1-minute load average < 2.0**. If not, it waits
  (polling, never sleeping past a single check) rather than launching.
- **Any arm whose load at start OR end exceeds 4.0 is VOIDED** — not starred, not reported
  as primary, re-run under a fresh slot suffix. This is stricter than the program's 8
  specifically because a wall figure that will be quoted as a primary result needs a
  narrower contamination band than a wall figure that has only ever been descriptive.
- **No other cohort, builder, or reviewer runs concurrently.** Checked, not assumed, before
  arm 1 and spot-checked between N-blocks:
  - `flock -n /home/forge/tmp/arms/arm.lock true` must succeed *before* this cohort takes
    the lock for its own run (proves no other cohort is mid-arm).
  - `pgrep -fal 'sol-yolo|codex exec|claude -p|run-arm\.sh'` must show **only** this
    cohort's own processes once its own arm starts, and **nothing** before it starts.
  - `uptime` logged at cohort start and at every N-block boundary; any non-zero count of
    unexplained load contributors (another seat's build, a suite run, a reviewer round) is
    cause to pause, not to proceed and star the result — starring is not available as a
    fallback in this cohort.
- **Consequence for scheduling:** this cohort does not start until Anvil is confirmed idle
  end-to-end (no MEM-003/O2/q5z/census/scorer/gate lane mid-round on the box), and it holds
  the box for its full ~1.5–2h (§8) without yielding it to another lane mid-run.

---

## 4. Meters

**Primary: wall seconds, from the driver's own request/response timestamps — first request
sent to final answer — never lock-wait, never slot-elapsed-including-queue.** E-NSWEEP's own
deviation 6 documents the difference: its N=30/40/55 rows recorded *slot* elapsed including
lock wait (80–152 s "driver's own wall" hidden inside larger recorded numbers), which is
exactly the ambiguity this cohort's primary meter must not carry. Reported **per arm** and
as **cell medians** (median, not mean — E-CEILING80's N-3 and E-NSWEEP's daggered rows both
show that this caller's wall distribution is fat-tailed on the generator-debugging side, and
a mean lets one 270-second retry-loop arm dominate a cell of three).

**Secondary, reported and never substituted for the primary:**
- emitted write-payload characters (this program's usual primary; here a check that the
  character-vs-wall relationship this cohort exists to measure is visible in its own data)
- return count, tool-call count (the load-immune call-count meters used throughout)
- strategy class (literal-patch / programmatic-generation / stream-edit / tool-call), by
  the **owner-relative** classifier fix E-NSWEEP §4 shipped (majority-of-owners, not raw
  marker count ≥ 2) — the frozen classifier's absolute-count bug misfired at exactly N=40,
  which is one of this cohort's three N values, so the corrected rule is load-bearing here,
  not merely a sensitivity check
- **correctness gate**, unconditional, on every arm: `rescore-FAN.sh <worktree> <N>` **6/6**
  against the frozen canonical for that N, **and** `diff -r worktree/src canonical-<N>/src`
  byte-identical. An arm that fails either is reported and excluded from the wall claim,
  never averaged in.

---

## 5. Predictions, with probabilities, per cell — before arm 1

Grounded in the **descriptive, non-primary** wall numbers already on record in this family
(quoted with their dagger status, since several are load-contaminated and this cohort exists
to replace them with clean ones):

- Sol tool wall (`apply_patch` harness), N=21: E-HARNESS-2 T1 **45.0†**, T2 **46.0**.
- Sol tool wall, N=80: E-CEILING80 T1 **37.0**, T2 **46.0†**.
- Sol native wall, N=21 (8 pooled arms, E-REG+E-AFFORD): literal-strategy arms **364, 125,
  144, 133**; generation-strategy arms **88, 87, 120, 96**. Pooled median **122.5**.
- Sol native wall, N=40 (E-NSWEEP, 3 of 4 daggered): **87** (clean), 191†, 223†, 274†.
- Sol native wall, N=55 (E-NSWEEP, 3 of 4 daggered): **198** (clean), 135†, 236†, 164†.
- Sol native wall, N=80 (E-CEILING80, 2 of 3 daggered): **93** (clean), 60†, 270†.
- Claude/Sonnet native wall, N=21 (E-CALLER, none daggered): **51, 23, 41** — mean 38.3.
- Claude/Sonnet tool wall, N=21 (E-CALLER, none daggered): **13, 16, 9** — mean 12.7.
- No wall data exists anywhere in this program for Sol-tool at N=40, or for either Claude
  cell at N=40 or N=80. Those six cells are genuinely new measurements.

| # | prediction | p |
|---|---|---|
| P1 | **Sol tool wall stays flat**: cell medians at N=21, 40, 80 all fall within **35–50 s** | 0.65 |
| P2 | **Claude tool wall stays flat**: cell medians at N=21, 40, 80 all fall within **8–20 s** (NOT the same band as Sol — the two harnesses' fixed overhead differ by roughly 3×, already measured at N=21) | 0.60 |
| P3 | Sol native wall at N=21: cell median **100–150 s** | 0.55 |
| P4 | Sol native wall at N=40: cell median **80–160 s**, with **≥ 1 of 3** arms exceeding 200 s (a generator-debugging tail, as E-CEILING80's N-3 and E-NSWEEP's daggered N40/55 rows both suggest even after discounting load contamination) | 0.40 |
| P5 | Sol native wall at N=80: cell median **90–170 s**, with **≥ 1 of 3** arms exceeding 200 s | 0.45 |
| P6 | Claude native wall at N=21: cell median **35–55 s** (re-measurement of E-CALLER's own cell, n=3 fresh) | 0.65 |
| P7 | Claude native wall at N=40: cell median **45–80 s** (file-list enumeration roughly doubles; no prior data — wide interval) | 0.40 |
| P8 | Claude native wall at N=80: cell median **65–120 s** | 0.35 |
| P9 | Ratio native/tool, Sol, N=21: **1.8×–3.0×** | 0.55 |
| P10 | Ratio native/tool, Sol, N=40: **1.5×–4.0×** | 0.40 |
| P11 | Ratio native/tool, Sol, N=80: **1.8×–4.0×** | 0.45 |
| P12 | Ratio native/tool, Claude, N=21: **2.5×–4.5×** | 0.55 |
| P13 | Ratio native/tool, Claude, N=40: **3.0×–6.0×** | 0.40 |
| P14 | Ratio native/tool, Claude, N=80: **4.0×–9.0×** | 0.35 |
| P15 | **N at which the ratio first exceeds 2.0×, Sol**: already at **N=21** (the pooled descriptive N=21 tool/native numbers already clear 2×: 122.5/45.5 ≈ 2.7×) | 0.50 |
| P16 | **N at which the ratio first exceeds 2.0×, Claude**: already at **N=21** (38.3/12.7 ≈ 3.0×) | 0.55 |

**The pattern these predictions are betting on, stated plainly:** wall is expected to clear
2× in the tool's favor **earlier than N=40**, for both callers, and to stay flat or widen
rather than close, precisely *because* the character meter and the wall meter measure
different things at large N — E-CEILING80 found native cheaper in characters at N=80 by
switching to a generator, but nothing in that finding implies the generator is cheaper in
**seconds**, and the daggered walls in E-CEILING80 and E-NSWEEP (60–274 s at N=40/55/80, one
arm at 270 s across three re-emissions) point the other way. **This cohort exists because
that implication was never tested clean.**

---

## 6. Withdrawal conditions — fixed before arm 1

**W1 — the scale claim.** If the tool's wall is **not** ≥ 2.0× better than native's median
at **N=80** for **either** caller (i.e., `native_median_N80 / tool_median_N80 < 2.0` for Sol
OR for Claude), the **"2–5× at scale"** wall claim is withdrawn in its entirety, and the
standing sentence (`gene-report-0455z.md` §1: character-based, bounded to N ≤ 21/N* ≈ 23)
stays exactly as it is — no wall-based amendment is made to it.

**W2 — the one-call mechanism.** If the tool's own wall is **not flat** — i.e., its median
grows by **more than 1.5×** from N=21 to N=80, for either caller
(`tool_median_N80 / tool_median_N21 > 1.5`) — the **"one call, flat in N"** mechanism claim
is withdrawn. A withdrawal here does not by itself withdraw W1's ratio claim (the tool could
still win on wall while growing modestly), but the *explanation* offered for why it wins —
fixed overhead versus growing native cost — no longer holds and must not be asserted.

**W3 — the refusal-tax reporting rule.** If **any** tool arm's first call is refused with
`invalid-mcp-request` (or the family of refusals this program has already measured —
`unknown-verification-profile`, `invalid-diagnostic-output` — see E-CEILING80 §7 finding 5
and E-CALLER §5 finding 1, both of which paid exactly one refused round-trip on 10 of 13 and
1 of 3 T arms respectively), that arm's wall is reported **both** with and without the
refused round-trip's seconds, and **the with-refusal number is pre-registered as primary**
— it is the honest number a real caller pays, exactly as E-CEILING80 ruled it "a third of
the T cell's payload" and never subtracted it. The without-refusal number is reported
alongside as a labelled diagnostic only, never substituted into a cell median.

---

## 7. Fixtures

**No new fixtures are generated by this cohort.** All three N-levels reuse already-frozen,
already-canonical-derived, read-only fixtures from prior cohorts in this family, all built
by `bb bench/fanout/gen-fanout.clj --n <N> --seed 7 --k 1` on `clj-surgeon-fanout`
`bridge/fanout-fixtures-in-git` at **b62a501**:

| N | source cohort | base sha | canonical tree digest |
|---|---|---|---|
| 21 | E-REG / E-AFFORD / E-HARNESS-2 / E-CALLER | `65fe39a9071083f478ed091ab64ebdf05c02abbd` | (E-REG's, reused across five cohorts) |
| 40 | E-NSWEEP | `5868fe9d…` | `437d25ca…`, 114 files |
| 80 | E-CEILING80 | `2659fb4f958beebf184bc8439528f21d29facd38` | `c3cc5140bb6618ab…4ff0`, 114 files |

Each is read read-only, exactly as E-CALLER and E-CEILING80 read E-REG's and E-AFFORD's
roots read-only (proven with a write-fence receipt, not merely asserted — §8 row 6 below).
No `gen-fanout` invocation runs as part of this cohort; if any of the three fixture roots is
missing or its canonical digest does not match the table above at validation time, the
cohort **stops before arm 1** rather than regenerating silently.

---

## 8. Validations before arm 1 — each with a receipt, a failure in any stops the cohort

| # | validation | what it proves |
|---|---|---|
| 1 | **Quiet-box assertion** (§3): 1-minute load < 2.0, `arm.lock` uncontended, no other cohort/builder/reviewer process matches `pgrep -fal 'sol-yolo\|codex exec\|claude -p\|run-arm\.sh'` | the box is this cohort's alone before arm 1, not merely at freeze time |
| 2 | **Fixture loads**: `bin/fan-test` green at base count for N=21, 40, 80 against the reused fixtures; canonical tree digests match the table in §7 | the reused fixtures are exactly what prior cohorts left, unmodified since |
| 3 | **Dead-port control**, both harnesses: Sol against a spare port with nothing listening (expect the E-CEILING80/E-HARNESS-2 pattern — apparatus fails closed at the `run-arm.sh`/`attest.sh` level, `sol-yolo`'s own `required=true` gap from E-CEILING80 §8 still applies and must be re-checked, not assumed fixed); Claude against a spare port with nothing listening (expect E-CALLER's pattern — `mcp_servers` status `failed`, zero surgeon tools, zero MCP calls, file unchanged) | no T arm can silently fall back to a native write path if its server dies |
| 4 | **Hand-driven call**, both harnesses: one hand-driven `alias_migration` per caller's server clone, at N=21 and N=80 at minimum (N=40's server-src is new for this cohort and gets its own hand-drive), each checked `diff -r` byte-identical against the reused canonical and `rescore-FAN.sh` 6/6 | the tool path itself is correct before any arm is scored against it |
| 5 | **Prompts byte-identical outside §5**: for every reused prompt, sha256 matches `FROZEN.sha256` from its origin cohort; for every new prompt (the 5 cells in §2), the diff against its N=21 or N=80 sibling is **exactly one hunk** — the owner-count line(s) — **±0 bytes elsewhere**, §5 (tooling/licence) verbatim | the one-hunk discipline this whole family relies on for cross-N comparability is not broken by this cohort's new prompts |
| 6 | **Scorer revalidation**: `payload.py`, `strategy.py` (with E-NSWEEP's owner-relative fix applied and its N40-2 negative witness passing), `secondaries.py` reproduce every published char count and strategy label from E-REG, E-AFFORD, E-CEILING80, E-NSWEEP, E-CALLER, E-HARNESS-2 that this cohort's copies touch; write-fence proven (mtime+size digest over every reused root, identical before and after) | the meters are the same meters this program has already validated, not new instruments with new bugs |
| 7 | **The Claude adapter's permission flag proven**: re-run E-CALLER's dead probe — `claude -p` without `--permission-mode bypassPermissions` against a scratch file, expect `system/permission_denied` on `Edit` and a byte-unchanged target; then the full shim (with the flag) against the same scratch file, expect the edit to land | the bypass flag this cohort's shim depends on for every Claude arm is still load-bearing, not a probe result that has drifted |
| 8 | **Port plan, declared and checked free**: this cohort's own server clones at **8040** (N=21), **8041** (N=40), **8042** (N=80) — new ports, chosen to avoid every port this program has already used (7888/7894/7895 forbidden; 7906–7910, 7941–7983, 8020–8022 all previously named by other cohorts) — each confirmed to have **no listener** before arm 1 and confirmed free again after the cohort |

---

## 9. Cost

**36 arms.** Per-arm wall is expected to range roughly **9 s (Claude tool, N=21) to 270 s
(Sol native, a generator-debugging outlier at large N)**, with most arms in the 40–150 s
band based on the descriptive numbers in §5. At an average of roughly 90 s/arm, 36 arms is
**~54 minutes of arm wall**, plus gate/scoring overhead (sub-second per E-NSWEEP's measured
gate speed, §8 of that document) and the quiet-box wait built into §3 (polling for load < 2.0
before each arm, which costs nothing if the box is genuinely idle but is not bounded if it
is not). **Total estimate: ~1.5–2 hours of exclusive box time**, consistent with the
program's own estimate for this cohort. This is a **first-quiet-window** cohort: it does not
compete for the box with anything else, and nothing else should be scheduled against it.

---

## 10. What this cohort cannot say

- **One caller per harness, not a crossed design.** Sol is measured only with `apply_patch`
  present (never with it removed, the E-HARNESS-2 Bash-only flank); Claude is measured only
  with its free-choice native tools (never coerced into a forced literal-context `Edit`, the
  experiment E-CALLER's own caveat names as unretired). A caller × harness cell this cohort
  does not run is not evidence about that cell.
- **Generated fixtures.** All three N-levels are `gen-fanout` output — a synthetic, perfectly
  uniform k=1 alias-migration shape. Nothing here speaks to a real, irregularly-shaped
  repository at any N.
- **One task family.** Alias migration only — one verb (`alias_migration`), one kind of edit
  (a two-site, whole-file rename). A wall result here does not transfer to a different
  Surgeon verb or a different edit shape without its own cohort.
- **Three N points, not a curve.** Exactly as E-CEILING80 (2 points) could not locate a
  boundary and needed E-NSWEEP's sweep to find N* ≈ 22–23, three points at 21/40/80 can
  establish a trend but cannot resolve *where* a wall-ratio crossing sits to the precision
  E-NSWEEP achieved for the character-based crossing. If W1 or W2 fires in an unexpected
  direction, the honest next step is a wall-sweep at intermediate N, not a claim this
  cohort's three points cannot support.

---

*Draft only. No arms have run. To be frozen (its own sha256, written with zero arm
directories on disk, exactly as every prior cohort in this family did) immediately before
arm 1, in the first confirmed-quiet window on the box.*

## Amendment (before arm 1, coordinator, Gene's cclsp ruling): the seat's own cclsp provider must be LIVE before arm 1 (assert `make cclsp-status CCLSP_PORT=<seat port>` up, and that the cohort server is configured to use it); any tool arm whose receipt carries `semantic-provider-unavailable` is VOID and re-run, never scored. The provider on 7890 belongs to another seat and is never contacted.
