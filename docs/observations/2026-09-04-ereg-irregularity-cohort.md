# E-REG — the alias-irregularity sweep: the mechanism claim survives its emission half and loses its irregularity half

*forge@anvil, 2026-09-04T00:28:54Z. Cohort E-REG = Opus's **O1** from the E3-P result poll
(`/home/forge/tmp/sol/e3poll-opus.md`), selected by the fleet in
`docs/observations/2026-09-03-brainfleet-hills.md` §16 as the one experiment that could
falsify Opus's own mechanism claim. Pre-registration written before the first arm:
`/home/forge/tmp/arms/ereg/preregistration.md`. Per-arm receipts under
`/home/forge/tmp/arms/ereg/ereg-k<K>-<arm>-<slot>/`; table
`/home/forge/tmp/arms/ereg/EREG-table.md`.*

## The table

| k | arm | run | emitted chars | chars/s | emission gap s | wall s | returns | non-test | correct | load start→end |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | N | 1 | **8,594** | 147.0 | 58.461 | 137* | 4 | 5 | 6/6 + bytes | 10.38 → 13.9 |
| 1 | N | 2 | **1,929** | 154.0 | 12.529 | 88 | 3 | 5 | 6/6 + bytes | 6.0 → 6.86 |
| 1 | T | 1 | **1,013** | 178.1 | 5.689 | 37* | 4 | 3 | 6/6 + bytes | 13.9 → 11.76 |
| 1 | T | 2 | **485** | 164.9 | 2.941 | 32* | 3 | 3 | 6/6 + bytes | 11.76 → 6.0 |
| 2 | N | 1 | **9,724** | 138.6 | 70.135 | 115 | 5 | 4 | 6/6 + bytes | 5.48 → 3.32 |
| 2 | N | 2 | **7,377** | 142.9 | 51.612 | 99 | 4 | 5 | 6/6 + bytes | 3.32 → 6.22 |
| 2 | T | 1 | **1,019** | 182.2 | 5.594 | 38 | 4 | 3 | 6/6 + bytes | 6.86 → 5.48 |
| 2 | T | 3 | **1,260** | 172.4 | 7.307 | 43 | 3 | 4 | 6/6 + bytes | 3.0 → 4.15 |
| 3 | N | 1 | **8,202** | 141.7 | 57.9 | 105* | 4 | 4 | 6/6 + bytes | 7.7 → 11.05 |
| 3 | N | 2 | **8,179** | 141.5 | 57.787 | 107* | 4 | 5 | 6/6 + bytes | 17.4 → 18.1 |
| 3 | T | 1 | **1,067** | 182.1 | 5.861 | 39* | 4 | 3 | 6/6 + bytes | 11.05 → 14.63 |
| 3 | T | 2 | **912** | 189.1 | 4.823 | 42* | 5 | 4 | 6/6 + bytes | 14.63 → 17.4 |
| 6 | N | 1 | **16,531** | 141.7 | 116.631 | 169* | 5 | 6 | 6/6 + bytes | 13.96 → 10.78 |
| 6 | N | 2 | **9,636** | 137.5 | 70.077 | 109* | 4 | 4 | 6/6 + bytes | 10.78 → 8.09 |
| 6 | T | 1 | **1,075** | 179.0 | 6.005 | 33* | 4 | 3 | 6/6 + bytes | 18.1 → 13.96 |
| 6 | T | 2 | **580** | 170.3 | 3.405 | 35* | 3 | 3 | 6/6 + bytes | 8.09 → 7.72 |

`*` = wall VOID (1-min load average above 8 at arm start or end).

### cell means

| k | native chars | native chars **per patch call** | native chars/s | tool chars | tool chars/s | native wall | tool wall | T/N wall |
|---|---|---|---|---|---|---|---|---|
| 1 | 5,262 | 5,262 | 150.5 | 749 | 171.5 | 112 | 34 | 0.31× |
| 2 | 8,550 | 8,550 | 140.8 | 1,140 | 177.3 | 107 | 40 | 0.38× |
| 3 | 8,190 | 8,190 | 141.6 | 990 | 185.6 | 106 | 40 | 0.38× |
| 6 | 13,084 | 8,951 | 139.6 | 828 | 174.7 | 139 | 34 | 0.24× |

### Opus's four predictions

| # | prediction | verdict | measured |
|---|---|---|---|
| P1 | crossover k*=3 (55%) | **MISS** | k* = 1 (tool wins at every k tested, k=1 included); k*_chars = 1 |
| P2 | native payload at k=1 = 180 ± 120 chars (70%) | **MISS** | measured 8,594, 1,929 chars — 6–29× the top of the band |
| P3 | at k=1 T loses, T/N wall 1.0–1.4× (75%) | **MISS** | T/N = 0.31× — the tool won |
| P4 | native emission rate 137 +/- 15 chars/s in every native cell (65%) | **HIT** | cell means k=1: 150.5, k=2: 140.8, k=3: 141.6, k=6: 139.6 — all inside 122-152. Per-arm: 147.0, 154.0, 138.6, 142.9, 141.7, 141.5, 141.7, 137.5; 7/8 individual arms also inside the band (the exception: 154.0) |

## The three pre-registered pass lines, scored

**(a) MECHANISM CLAIM.** native k=6 mean 13,084 chars vs k=1 mean 5,262 chars = **2.49×**, needed ≥3× → growth **NOT MET**. Tool cell means 749, 1,140, 990, 828 (grand mean 926); spread inside ±25% → MET. Withdrawal condition (both k=1 native runs: gap >30 s AND <400 chars) → NOT triggered (k=1 native runs: 8,594 chars / 58.5 s, 1,929 chars / 12.5 s). **VERDICT: NOT SUSTAINED (and not withdrawn) — INCONCLUSIVE by the pre-registered wording.**

**(b) CROSSOVER.** Strict k\* (every T run in the cell faster than every N run) = **1** — the tool already wins at the smallest k tested, so the crossover is at or below k=1 and this sweep cannot resolve it further. Floor-free k\*_chars (smallest k with mean native chars > 2,000) = **1**. Caveat as pre-registered: wall at n=2 is inside the 172 s floor measured for this apparatus at n=3, and 11 of 16 arms ran above load 8.

**(c) k=1 — DOES THE TOOL LOSE?** mean wall T 34 s vs N 112 s, **T/N = 0.31×**. Pre-registered rule: the tool LOSES iff T/N ≥ 1.0 → **the tool WINS at k=1.**

## What actually happened — five lines

1. **The emission half of Opus's mechanism claim survives, and survives well.** Emitted
   write-payload characters predict the pre-call gap at a near-constant rate in **every**
   native cell: k=1 150.5, k=2 140.8, k=3 141.6, k=6 139.6 chars/s — all four inside the
   pre-registered 122–152 band, and 7 of 8 individual arms inside it too (the exception,
   154.0, misses by 2.0). Server time is not the story and neither is discovery: the
   tool's whole wall at k=1 is **34 s**, and native's write-emission gap *alone* at k=1
   averages **35.5 s**. **Native spends longer typing than the tool spends existing.**
2. **The irregularity half is FALSIFIED.** Opus predicted that on a uniform fan-out
   native's payload "collapses to one ~120-character `sed` command … at any N".
   **Zero of eight native arms used a `sed` or `perl` rewrite at any k — including k=1,
   which has one old alias, one new alias and zero collisions, the shape the hypothesis
   says a single `sed` closes.** Native emitted a full `apply_patch` body in 8 of 8 arms.
   Per-patch payload is flat across the whole sweep: **5,262 / 8,550 / 8,190 / 8,951
   chars** at k = 1 / 2 / 3 / 6. Irregularity is not what sets the slope; it barely moves it.
3. **The crossover the cohort was built to locate does not exist inside the sweep.** k\* = 1:
   the tool wins every cell, including the fully uniform one, on wall (T/N 0.31× at k=1)
   and by 7–16× on the load-immune character meter. Opus's O1 was designed on the premise
   that k=1 was "the case native should win" and that E3-L's boundary control was
   "derivable" from it. Both premises are wrong, and the k=1 cell is the reason to run
   experiments instead of deriving them.
4. **The one partial collapse is the most interesting arm in the cohort, and it still lost.**
   `ereg-k1-N-2` did not hand-type its patch: it emitted a compact JS table of 21 filenames
   and generated the patch body programmatically — 1,929 chars instead of 8,594, at 154
   chars/s, in 12.5 s. That is the behaviour the sed hypothesis predicts, in the cell it
   predicts it, **once in two runs** — and even that arm's wall (88 s) was 2.6× the tool's.
   So the model *can* compress a uniform fan-out; it usually does not, and compressing was
   not enough. A capability the agent exercises half the time in the cell most favourable
   to it is not a mechanism you can price a product against.
5. **The q5z round-10 scope fix landed, and moved the refusal rather than removing it.**
   **Zero `alias-migration-empty-scope` refusals in 8 T arms** — `scope: {paths: ["src"]}`
   now commits, which was E3-P clause 1. But 6 of 8 arms still pay exactly one refused
   round-trip on their *first* call, now for two new reasons: `invalid-mcp-request` (4 arms —
   the model invents a flat `old_lib/old_var/new_lib/new_var/alias_policy/expected_files`
   argument shape) and `unknown-verification-profile` (2 arms). Both refusals carry a
   rendered remedy — *"Send exactly op, workspace_…"* — so the round-10 refusal-text ratchet
   is working; what has not been fixed is that **the agent's first guess at the request
   shape is wrong three-quarters of the time.** The price is small (one call, ~3 s inside a
   5.6–7.3 s total emission gap) but it is paid almost every run, and it is now the tool's
   largest remaining avoidable cost.

## Correctness

**All 16 valid arms passed `rescore-FAN.sh <worktree> 21` 6/6 against that k's own
`canonical-21`, AND were byte-identical to it** (`diff -r worktree/src canonical-21/src`
clean). Every T arm: server sha `33a8236` matching the expected sha, port 7907, healthz
ok, **exactly one committing `alias_migration` call, zero native fallback** (0
`apply_patch` calls in all 8).

## The apparatus, and what I changed

- **Fixtures.** `gen-fanout.clj --n 21 --seed 7 --k K` on `bridge/fanout-fixtures-in-git`
  at `b62a501`, into `fanout-k{1,2,3,6}`. Measured shapes: k=1 `{"store" 21}` / 0
  collisions; k=2 `{"st" 10, "store" 11}` / 10; k=3 `{"st" 7,"store" 7,"s" 7}` / 21; k=6
  `{"st" 4,"db" 3,"s" 4,"store" 4,"repo" 3,"k" 3}` / 30. **The k=6 `repo-21/` and
  `canonical-21/` trees are byte-identical to the E3-P cohort's**, so the k=6 cell is a
  replication of rung P, not a new rung. **Canonical churn is +84/−84 at every k** — same
  N, same files, same sites, same edit size; only the alias *names* vary. That is the
  control the design needed and it is measured, not assumed.
- **`sabotage-FAN.sh --selftest-k 21 7`: 11 passed, 0 failed** — byte-identical
  regeneration at each k, the histogram/collision witnesses, and `rescore-FAN.sh` 6/6
  against each k's own canonical.
- **The scorer: what I changed.** The pinned meter
  (`clj-surgeon-arms/bench/anvil-arms/score.py` @ `89295d8`) does **not** count emitted
  characters. Rather than mutate a pinned instrument I added
  `/home/forge/tmp/arms/ereg/payload.py`, an additive scorer over the same
  `rollout.jsonl` writing `payload.json` beside each receipt. **Validation, run before the
  cohort was trusted: it reproduces Opus's six E3-P figures exactly** — native 8,649 /
  16,321 (=8,160+8,161) / 9,531 chars at 136.7 / 140.8 / 136.8 chars/s over gaps of 63.281
  / 115.893 / 69.689 s; tool 465–593 chars per call, 6.7 / 8.8 / 6.6 s, 174.1 / 166.3 /
  172.8 chars/s. A scorer that cannot reproduce the number it audits is not a meter.
  - **Two classifier defects the live arms found, both fixed and both re-validated against
    E3-P.** (i) Matching the bare token `alias_migration` counted a tool-*discovery* probe
    (`ALL_TOOLS.filter(x => x.name.includes("alias_migration"))`) as a write, inflating T by
    135 chars. (ii) Keying on the invocation *syntax* then missed two further spellings —
    `const toolName = "mcp__…"; await tools[toolName]({…})` and `const migrate = tools.mcp__…;
    await migrate({…})` — reporting three genuine T arms as `writes=0`. **Three distinct call
    spellings appeared across sixteen arms.** The meter now keys on the emitted *request
    content* (`alias_policy` + the new lib + an `await`), which is form-independent and is
    what the unit is actually about. Generalised: **a meter that recognises a call by its
    syntax measures the model's phrasing, not its work.**
- **Emission gap** of a write call = `call.timestamp − timestamp of the immediately
  preceding rollout record`. Validated against E3-P N-1 to one decimal (136.7 chars/s).
- **Server.** q5z tip `0085db8` merged locally with `origin/main` `4be8566` → merge
  `33a8236`, read back from the running server's `/proc/<pid>/cwd`
  (`/home/forge/tmp/arms/ereg/server-src`) on every T arm. Three additive merge conflicts
  resolved keep-both (a test-runner require pair, an `intent_transaction` opts-key pair, an
  `mcp_intent_contract` doc-path pair). Port 7907 only; `required = true`; 7888/7894/7895/
  7906/7909/7910 never contacted. Arms serialised with the live E6-Q2 cohort on 7909 via
  `flock /home/forge/tmp/arms/arm.lock`.

## Deviations, all of them

1. **Wall is void in 11 of 16 arms.** The E6-Q2 cohort ran concurrently on the same box;
   1-minute load reached 18.1. Load is recorded at start and end of every arm and voided
   walls are starred in the table. **This is exactly why the primary meter is emitted
   characters** — the character counts and the 122–152 chars/s band are load-immune, and
   every headline above rests on them. The wall figures are reported and are not claimed.
2. **`ereg-k2-T-2` is VOID twice**, for the same harness fault both times: `WATCH-ABORT
   rollout-rotated` — codex truncated its own session rollout in place (60,341→59,960 and
   36,864→35,446 bytes), the watcher correctly refused to meter a rotated file, and the
   worktree was left at the pre state. Evidence retained as
   `ereg-k2-T-2.VOID-rollout-rotated{,-2}`. The slot was re-run a third time as
   **`ereg-k2-T-3`**, which is clean and is the k=2 T second run in the table. **Not a tool
   failure and not a model failure; a driver-side fault the apparatus caught rather than
   scored.**
3. **I destroyed one completed arm-run myself.** A smoke arm (`ereg-k1-N-1`, 83 s) was
   wiped when I re-launched the runner to debug it and its first act was `rm -rf "$A"`.
   Ratchet applied the same hour: `run-ereg.sh` now refuses to delete an arm directory that
   already holds a `rollout.jsonl` unless `FORCE=1`.
4. **The gate was moved out of the driver loop.** The first runner ran `rescore-FAN.sh`
   inline under `flock /home/forge/tmp/suite.lock`, which E6-Q2 holds for minutes — a
   driver arm would have blocked on another cohort's JVM and E-REG's own wall numbers would
   have been at that cohort's mercy. Gates now run post-hoc in `gate-ereg.sh`; they are
   deterministic and order-free.
5. **n=2, not E3-P's n=3**, as the design specified. The primary meter is claimable at n=1
   by Opus's own construction; the wall figures are not claimed.
6. **P4 was scored on cell means**, which is the pre-registered unit ("every native cell's
   mean rate inside 137 ± 15"). My first scorer pass checked per-arm rates and returned
   MISS; the pre-registration governs, and it says HIT. Per-arm rates are printed in the
   table so the reader can see the one 154.0 outlier for themselves.
7. **O1 named server `ac1c8409`**; the brief directed the newer `0085db8`. Declared in the
   pre-registration in advance, with the consequence named in advance: the round-10 scope
   fix removes E3-P's four scope refusals, which changes T's returns but cannot touch the
   primary meter.

## One line for Gene

Opus said the tool's win came from native having to type 8,600 characters of patch, and
that a *tidy* fan-out would let native close the whole job with one `sed` — so we built
four versions of the same 21-file job, from perfectly uniform to maximally messy, and ran
sixteen arms: **he was right about the typing and wrong about the tidiness.** Native typed
roughly 8,000–9,000 characters per patch no matter how tidy the job was, never once reached
for `sed`, and lost every cell — including the tidy one, where the time it spent purely
typing was longer than the tool took to do the entire task.
