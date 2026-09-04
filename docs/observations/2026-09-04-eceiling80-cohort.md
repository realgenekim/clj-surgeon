# E-CEILING80 — the emission law breaks between N=21 and N=80: native stops typing and starts generating, and the tool's character win at k=1 is GONE

*forge@anvil, 2026-09-04T01:33Z. Cohort E-CEILING80 = brainfleet §21 #3 merged with §22 #3
(E-SLOPE80-C) and the §26 cross-attack's rulings. Five arm-runs, serial, interleaved,
N=80 owners at k=1. Pre-registration frozen before arm 1:
`/home/forge/tmp/arms/eceiling80/preregistration.md`, sha256
`89c2af0e76fb2cfc44cd49a11f01e8a955135e058c38591fceb20a7dc923c660`, receipt
`receipts/prereg.sha256`, written with **zero arm directories on disk**. Instruments frozen
in `FROZEN.sha256`. Per-arm receipts under `/home/forge/tmp/arms/eceiling80/eceiling80-P-*/`.
Fixtures and comparator receipts in E-REG's and E-AFFORD's roots were read **read-only**;
neither was modified (proven — see §8).*

> **WALL AND LOAD ARE RECORDED UNCONDITIONALLY AND ARE DESCRIPTIVE ONLY. NO WALL CLAIM IS
> MADE ANYWHERE IN THIS DOCUMENT.** Three of five walls ran with 1-minute load above 8 and
> are starred `†`; the star marks them uninterpretable, not excluded. Every finding below
> rests on the load-immune meters: emitted characters, call counts, and strategy labels.

---

## 1. The headline

**All three pre-registered withdrawal conditions fired.** At N=80 the caller did not type a
bigger patch — it stopped typing patches at all. Three of three native arms wrote a
**program that generates the edit**; none hand-transcribed a patch body; the two clean
arms emitted **841** and **722** characters to change **80 files**, and **both emitted FEWER
characters than the tool did in the arm run minutes later.**

| what | E-REG + E-AFFORD, N=21 (8 arms) | E-CEILING80, N=80 (3 arms) |
|---|---|---|
| literal-patch arms | **4 of 8** | **0 of 3** |
| programmatic-generation arms | 4 of 8 | **3 of 3** |
| stream-edit arms | 0 of 8 | 0 of 3 |
| native mean chars | 5,262 (E-REG) / 6,979 (E-AFFORD) | **4,713** |
| native median chars | ~5,700 | **841** |
| ratio native ÷ contemporaneous tool | 7.03× / 7.22× | **3.62×** (0.60× excluding one arm) |

The N=21 literal-patch law was **449.4 characters per owner**. Extrapolated to 80 owners it
predicts **35,953 characters**. The largest native arm measured here emitted **12,576**, and
it got there by re-emitting a *generator* three times, not by typing a patch.

---

## 2. Pre-registration — the design, verbatim in its receipts

Frozen `89c2af0e…` at 2026-09-04T01:19:35Z, load 5.73, with **0** `eceiling80-*` arm
directories on disk (the receipt records the count). The full text is at
`/home/forge/tmp/arms/eceiling80/preregistration.md`; the parts that govern the scoring are
reproduced in §5 and §6 below and were not edited after arm 1 launched.

| cell | fixture | prompt | arm | n | what it is |
|---|---|---|---|---|---|
| **N** | `fanout-k1` N=80 | `ECEIL-N.md` | native | 3 | the E-AFFORD **N-weak** condition: `E3-P-N.md` §5 verbatim, licence buried in its last clause |
| **T** | `fanout-k1` N=80 | `ECEIL-T.md` | tool | 2 | E-REG/E-AFFORD's T prompt §5 verbatim, port **7951** |

Run order, fixed before arm 1 and executed exactly: **N-1, T-1, N-2, T-2, N-3** — one at a
time, each under `flock /home/forge/tmp/arms/arm.lock`, so load drift is shared across cells
rather than confounded with one of them.

The N cell uses the **buried** licence, not the salient one, because E-AFFORD measured that
making the licence prominent moved the cell mean by 1,473 characters while arms *inside* one
cell differed by 8,221. Salience does not move this caller; the prominent variant buys
nothing and costs an arm.

---

## 3. The table

| cell | run | emitted chars | chars/s | emission gap s | patch/tool calls | strategy | re-emissions | refused 1st call | gate | wall s (descriptive) | returns | non-test | load start→end |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| N | 1 | **841** | 168.9 | 4.980 | 1 | programmatic-generation | 0 | 0 | 6/6 + bytes | 93.0 | 4 | 6 | 5.90 → 6.07 |
| T | 1 | **1,094** | 173.8 | 6.294 | 2 | tool-call | 1 | 1 | 6/6 + bytes | 37.0 | 4 | 4 | 6.35 → 7.53 |
| N | 2 | **722** | 162.6 | 4.441 | 1 | programmatic-generation | 0 | 0 | 6/6 + bytes | 60.0† | 5 | 4 | 6.45 → 10.08 |
| T | 2 | **1,508** | 174.3 | 8.650 | 2 | tool-call | 1 | 1 | 6/6 + bytes | 46.0† | 4 | 4 | 10.23 → 13.99 |
| N | 3 | **12,576** | 171.0 | 73.547 | 3 | programmatic-generation | 2 | 1 | 6/6 + bytes | 270.0† | 6 | 5 | 12.49 → 7.23 |

`†` = wall starred (1-minute load above 8 at arm start or end). Rows are in RUN order, which
is the pre-registered interleave. **5 of 5 arms passed the gate.**

### Cell means

| cell | n | mean chars | median chars | per-arm chars | mean chars/s | mean calls | strategy classes | gate |
|---|---|---|---|---|---|---|---|---|
| **N** (native, licence buried) | 3 | **4,713** | 841 | 841 / 722 / 12,576 | 167.5 | 1.7 | programmatic-generation ×3 | 3/3 green |
| **T** (tool, port 7951) | 2 | **1,301** | 1,301 | 1,094 / 1,508 | 174.1 | 2.0 | tool-call ×2 | 2/2 green |

**Ratio, native emitted chars ÷ tool emitted chars, both terms measured in this cohort
inside one 13-minute window: 3.62×.** Excluding the single three-call arm N-3, the two clean
native arms average 782 characters against the tool's 1,301 — **0.60×, i.e. the tool emitted
more.**

---

## 4. The strategy-conditional per-owner constant (§26)

§26 ruled that pooling the two strategy modes produces a mean describing neither, so the
constant is recorded **within the literal stratum only**.

| stratum | N=21 (E-REG + E-AFFORD, 8 arms) | N=80 (this cohort, 3 arms) |
|---|---|---|
| **literal-patch** | n=4, mean 9,438 chars → **449.4 chars/owner** | **n=0 — NOT COMPUTABLE** |
| **programmatic-generation** | n=4, mean 2,556 chars → 121.7 chars/owner | n=3, mean 4,713 → 58.9 chars/owner |
| programmatic, single-call arms only | — | n=2, mean 782 → **9.8 chars/owner** |

**The headline number of this section is the empty cell.** The per-owner emission constant
that square 2 was priced against exists only in the literal stratum, and at N=80 that
stratum is empty in 3 of 3 arms. A constant you cannot measure because the behaviour it
describes has stopped happening is not a constant that got smaller — it is a **law with a
domain**, and the domain ends somewhere between 21 and 80 owners.

The generation stratum is not constant either: **121.7 chars/owner at N=21 falls to 9.8 at
N=80** in the clean arms. That is the expected signature — a generated patch is O(1) in the
number of owners, so chars/owner falls as 1/N. The literal stratum is the one that was
linear, and it is the one that vanished.

---

## 5. The six pre-registered predictions, scored

| # | prediction | p | verdict | measured |
|---|---|---|---|---|
| **P1** | native ≥ 25,000 chars in ≥ 2/3 arms | 0.60 | **MISS** | **0/3** — 841 / 722 / 12,576 |
| **P2** | native splits into ≥ 2 patch calls in ≥ 2/3 arms | 0.60 | **MISS** | **1/3** — 1 / 1 / 3 calls |
| **P3** | tool emits 600–1,500 chars | 0.85 | **HIT** | T mean **1,301** (1,094 / 1,508) |
| **P4** | ratio ≥ 20× at the cell mean | 0.65 | **MISS** | **3.62×** |
| **P5** | ≥ 1 native re-emission across the 3 arms | 0.55 | **HIT** | **2** (both in N-3) |
| **P6** | *Sol's clause:* T at least 5× smaller in every pair | 0.80 | **MISS** | pair 1 **0.77×**, pair 2 **0.48×** — native emitted LESS than the tool in both |

Ratio is defined, as pre-registered, as **native emitted chars ÷ tool emitted chars**
(E-REG k=1: 7.03×; E-AFFORD: 7.22×).

P6 is the most informative miss in this program to date. It was written at p = 0.80 and it
did not merely fail to clear 5× — **it inverted.** In both complete interleaved pairs the
native arm emitted fewer characters than the tool arm run minutes later on the same box.
The third native arm (12,576) beats the T mean 9.67× and is recorded as a clearly-labelled
pseudo-pair, not as a pair.

---

## 6. The withdrawal conditions, applied — all three TRIGGERED

**W1 — "if ≥ 2 of 3 native arms emit < 6,000 chars (spontaneous compact generation at
scale), the emission law is BOUNDED to N ≲ 40 and square 2's product claim is restricted to
mid-size fan-out in this same document."**
Measured: **2 of 3** (841, 722; the third is 12,576). **TRIGGERED.**

> **Restriction, applied here as pre-registered: the emission law — "native emits ~450
> characters per owner and the tool emits a constant ~500–1,500" — is BOUNDED TO N ≲ 40.
> Square 2's product claim is restricted to MID-SIZE FAN-OUT and must not be quoted at
> large N.** At N=80 the same caller, the same prompt and the same task produce a
> qualitatively different behaviour, and the character advantage the claim rests on is not
> present.

**W2 — "if T is not ≥ 5× smaller in ≥ 2/3 pairs, no N=80 win is claimed."**
Measured: **1 of 3** pairs clear it (pair 1 0.77×, pair 2 0.48×, pseudo-pair 9.67×).
**TRIGGERED.**

> **No N=80 win is claimed for the tool on the character meter. None.** The tool remains
> correct, single-call, and 2/2 gate-green, and its payload is genuinely constant in N
> (534–560 characters per committing call at N=80, against 465–593 at N=21). But at N=80 it
> does not emit fewer characters than native, and this document claims nothing that rests
> on saying it does.

**W3 — "if native uses a compact generated rewrite in ≥ 2/3 arms, the linear default-policy
mechanism is WITHDRAWN."**
Measured: **3 of 3** arms are programmatic-generation. **TRIGGERED.**

> **The linear default-policy mechanism is WITHDRAWN.** The model of this caller as "emits
> a patch body whose size is linear in the number of owners, by default" is refuted at
> N=80. The correct model is now: **the caller chooses between a literal patch and a
> generator, and N is a variable that moves that choice** — decisively, at some N between
> 21 and 80.

---

## 7. What actually happened — five lines

**1. Native did not scale up its typing; it changed strategy, and the change is total.**
At N=21, across eight arms, the caller flipped a coin: 4 literal patches (mean 9,438 chars)
and 4 generated ones (mean 2,556), two disjoint clusters. At N=80 the coin stopped: **3 of
3 generated, 0 of 3 literal.** N-1 and N-2 each wrote one short shell program — `rg -l` to
find the owners, `awk` to build the hunks, piped into `apply_patch` — and committed all 80
files in **841 and 722 characters**, one call, first try, byte-identical to the canonical.
Whatever the caller is doing when it decides how to write an edit, **the number of owners is
an input to that decision**, and E-REG's k=1 result ("native typed a full patch in 8 of 8")
is a statement about N=21, not about native.

**2. The tool's character win at k=1 is gone, and it inverted in both complete pairs.**
T emitted 1,094 and 1,508 characters; the native arms interleaved with them emitted 841 and
722. The tool's payload behaved exactly as designed — flat in N, 534 and 560 characters for
the committing call, essentially the same as its 465–593 at N=21 — but native's fell
*below* it. The tool did not get worse; **the comparator got much better, because it was
never obliged to be linear and at N=80 it stopped pretending to be.**

**3. Still not one stream edit — in this cohort or in any of the 22 arms now on record.**
Zero of five arms here, zero of eight at N=21, zero of nine in E-AFFORD including the three
that were handed `sed` and `perl` by name. Native used `rg` and `awk` heavily — and used
them to *generate a patch body that it then fed to `apply_patch`*. E-AFFORD's finding 3
holds and is now confirmed at 3.8× the fan-out: **`apply_patch` is not the affordance
native reaches for; it is the affordance native routes everything through.**

**4. The one expensive native arm was expensive because of a FAILED GENERATOR, not a big
patch.** N-3 emitted 12,576 characters across three calls — but every one of the three was a
*program* (4,291, 4,299, 3,986 chars), not a patch body. The first implemented the full
four-tier alias policy with per-file collision counting, and **failed**; the second was a
re-emission of essentially the same program; the third was a correction pass after the
agent noticed its alias selection did not check every candidate token in the `ns` form. So
P5 hit and P2 nearly did — but for the opposite reason to the one predicted. The
pre-registration expected a patch too large to send in one call. What happened was a
**program too subtle to get right in one try.** At N=80 the cost moved from transcription
to program correctness, and the variance moved with it: 722 to 12,576, a 17× spread inside
one cell of three.

**5. The tool's first-call schema tax is now 2 of 2, and it is the same refusal E-AFFORD
measured.** Both T arms sent `verify: "bin/fan-test"` on the first call, both were refused
`unknown-verification-profile`, both recovered in exactly one round-trip, and both then
committed 80 files / 240 sites / `{"store2" 80}` / 0 collisions in one call. That is 2/2
here, 2/3 in E-AFFORD, 6/8 in E-REG — **10 of 13 T arms across three cohorts pay one
refused round-trip on their first call.** It is cheap per arm and it is paid nearly every
time; it remains the tool's largest avoidable cost, and it is now a character cost as well
as a wall cost, because at N=80 the refused call is a third of the T cell's payload.

---

## 8. Validations run before arm 1, each with its receipt

A failure in any of these stopped the cohort. None failed; one produced an unexpected
finding that is reported in full below.

| # | validation | receipt | result |
|---|---|---|---|
| 1 | fixture loads and `bin/fan-test` green at base count | (in §9 apparatus) | `LOAD-OK namespaces=100`; `FAN-TEST tests=80 assertions=560 failures=0 errors=0` |
| 2 | canonical derived and frozen | `receipts/canonical-80.sha256`, `.treesha256` | tree digest **`c3cc5140bb6618ab…4ff0`**, 114 files; canonical itself loads and is green |
| 3 | the gate is green at N=80 | `selftest/` | `rescore-FAN.sh <canonical-applied wt> 80` → **6/6**, 408 protected regions intact |
| 4 | scorer revalidation against E-AFFORD's nine receipts | `receipts/scorer-revalidation.txt` | **all nine** char counts and **all nine** strategy labels reproduced exactly; **write fence proven** — 198 files in E-AFFORD's arm dirs, identical mtime+size digest before and after |
| 5 | secondary meter validated against a published label | `receipts/secondaries-validation.txt` | reproduces E-AFFORD's *"unknown-verification-profile refused the first call in 2 of 3 T arms"* — T-1 and T-3, and finds the one native re-emission in `eafford-Ns-N-3` |
| 6 | dead-port negative control on the spare port 7953 | `receipts/negative-control.txt` | see below — **rc 2 at the level that governs the arms; a real defect found one level down** |
| 7 | ONE hand-driven `alias_migration` on a scratch copy | `receipts/handdrive-*.{json,raw}` | **80 files · 240 sites · `{"store2" 80}` · 0 collisions · 1,739.92 ms**, then `diff -r` vs `canonical-80/src` → **byte-identical**, and `rescore-FAN.sh … 80` → **6/6** |
| 8 | prompts byte-identical outside §5 | `receipts/prompt.sha256` | shared prefix (lines 1–51) sha256 **`f7b959f0a07e3aee…e5fa`**, identical in both |

### The dead-port negative control, and the defect it found

The control was run at two levels, and they disagree — which is the reason to run it.

**At the `run-arm.sh` level — the level every arm in this cohort actually runs at — the
apparatus fails closed, exactly as required.** Pointed at port 7953 with nothing listening:
`ATTEST-MISMATCH port-pid-unverified healthz-unverified ready-project-root-unverified
server-sha-unverified`, **rc 2, no `rollout.jsonl`, the driver was never launched, zero
returns.** (The brief predicted rc 1; the apparatus's refusal code is 2. The substantive
requirement — driver never launched, zero returns — is met.)

**One level down, at `~/bin/sol-yolo`, it does NOT fail closed, and this is a real finding
about every T arm in this fixture family.** Driving `sol-yolo` directly at dead port 7953:
**rc 0, 7 s, one completed model turn.** The cause, read from the script: `required = true`
is written into the worktree's `.codex/config.toml` **only when the worktree already has
one to neutralise**. The fanout fixture has no `.codex/` directory, so the `elif` branch
fires and passes only `-c mcp_servers.clj-surgeon.url=…` — **no `required` flag at all**.
codex logged four `rmcp` transport failures against the dead port and carried on.

So: a T arm whose server died *after* attestation would not fail — it would silently
continue with native tools. **No arm in this cohort did** (`via_verb` = 2 and
`native_apply_patch_clj` = 0 in both T arms, and both healthz/server-sha attestations are
green), and E-REG and E-AFFORD both checked the same thing post-hoc. But the guard is a
post-hoc audit, not a precondition, and it should not be described as one. **Ratchet
proposed, not applied here (it touches a shared script another cohort was mid-run on):
`sol-yolo` should write `required = true` whenever an mcp-url is passed, not only when it
is neutralising an existing config file.**

---

## 9. The apparatus

- **Fixtures.** `bb bench/fanout/gen-fanout.clj --n 80 --seed 7 --k 1` on
  `clj-surgeon-fanout` `bridge/fanout-fixtures-in-git` at **b62a501** — E-REG's script,
  branch and commit, with only `--n` changed from 21 to 80. Measured shape: **80 targets of
  100 namespaces, old-alias histogram `{"store" 80}`, new-alias histogram `{"store2" 80}`,
  0 collisions** — the perfectly uniform k=1 shape at 3.81× E-REG's size. Fixture base sha
  `2659fb4f958beebf184bc8439528f21d29facd38`.
- **The canonical is DERIVED, never hand-written** — the generator's own post-state rendered
  with `post?=true`, exactly as E-REG derived `canonical-21`. Canonical churn is
  **+320/−320** (80 files × 4 lines), the same per-file edit size as E-REG's +84/−84 at
  N=21: same task, more owners. Churn band `256,384,256,384` (±20%); **all five arms
  measured exactly 320/320.**
- **Prompts, proven byte-exact rather than asserted.** `ECEIL-N.md` sha256
  `bc9fc5638e52b6b0…721c`, `ECEIL-T.md` `bb1686bf85e16e9c…f34ba`. The unified diff from each
  pinned prompt (`clj-surgeon-arms` `89295d8`, whose committed `.sha256` sidecars match) to
  its counterpart here is **exactly one hunk — `21`→`80` and `79`→`20`, ±0 bytes, no other
  change.** §5, the TOOLING section carrying the licence and the tool instruction, is
  **verbatim** in both. The two prompts' shared prefix (lines 1–51) is byte-identical,
  sha256 `f7b959f0a07e3aee…e5fa`; they diverge at line 52 and nowhere else.
- **Server.** This cohort's **own clone** of E-AFFORD's build **`33a8236`** (E-REG's build)
  at `server-src`, so no JVM ran inside another cohort's checkout. Explicit **`:port 7951`**,
  `:telemetry :full`, started per T arm and stopped by the pid this apparatus recorded that
  it forked (pid + start ticks + boot id, all three checked before any signal). Server sha
  read back from `/proc/<pid>/cwd` on both T arms and equal to the expected sha; 7951 free
  after every T arm and after the cohort. `COHORT_PORTS="7951 7952 7953"`. **7888, 7894,
  7895, 7906–7910, 7941–7943 and 7947 were never named by this cohort and never contacted**
  — the three native arms' attestations each record *"no mcp url configured; cohort ports
  (7951 7952 7953) show no listener."*
- **Scorers.** E-AFFORD's `payload.py` and `strategy.py`, copied with **exactly two lines
  changed each** (the write-fence root), so E-AFFORD's deviation-1 ratchet travels with
  them: a scorer refuses to write outside `/home/forge/tmp/arms/eceiling80`. Proven, not
  asserted — running both over E-AFFORD's nine arms left its 198 files' mtime+size digest
  identical and created nothing.
- **`secondaries.py`, new.** Per arm, from `rollout.jsonl` only: write calls, patch calls,
  tool calls, failed write calls (paired `custom_tool_call_output` carrying an explicit
  failure token or non-zero `exit_code`), re-emissions (write calls following a failed
  one), first-call refusals, and the verbatim refusal types. **A write call with no paired
  output is counted in `unpaired_write_calls` and is never scored as success or failure** —
  a missing receipt is `:unverified`, not a zero. Validated against a published label
  before arm 1 (§8 row 5).
- **Gate.** `rescore-FAN.sh <worktree> 80` **6/6** against `canonical-80`, **plus**
  `diff -r worktree/src canonical-80/src` byte-identical, run post-hoc under
  `flock /home/forge/tmp/suite.lock` so it never contends with a driver arm. Arms serialised
  with every other cohort on the box via `flock /home/forge/tmp/arms/arm.lock`; E-PREWRITE's
  six arms completed at 01:13:14Z and this cohort's arm 1 launched at 01:20:12Z.

## 10. Correctness

**All 5 arms passed `rescore-FAN.sh <worktree> 80` 6/6 against `canonical-80`, AND were
byte-identical to it.** Every arm: `CHECK 1` changed=80 expected=80, 0 missing, 0 extras;
`CHECK 2` form-equality 80/80; `CHECK 3` 408/408 protected regions intact, 0 damaged;
`CHECK 6` residue-and-alias 0 old-lib hits, 0 old-site residue, 0 wrong-or-missing aliases,
0 shadowing; `CHECK 4` load 100 namespaces; `CHECK 5` `FAN-TEST tests=80 assertions=560
failures=0 errors=0`; churn +320/−320. **No arm was excluded.** Both T arms: server sha
`33a8236` read back from the running process's `/proc/<pid>/cwd`, port 7951, healthz ok,
exactly one committing `alias_migration` call, **zero native `apply_patch` fallback**.

## 11. Deviations, all of them

1. **The prompts are not byte-identical to `E3-P-N.md` / `E3-P-T.md`, and cannot be.** Those
   files hard-code *"There are exactly 21 such namespaces … the other 79 must not change."*
   The change is one hunk in each, ±0 bytes; §5 is verbatim. Declared in the
   pre-registration before arm 1.
2. **n = 3 native, 2 tool — five arm-runs.** The native cell's spread (722 to 12,576, 17×)
   is far larger than n=3 can resolve, so the honest output about the *magnitude* of native's
   payload at N=80 is a bound, not an estimate. **The strategy result is not subject to that
   caveat**: 3 of 3 is a categorical count of a binary choice, and it agrees with the
   direction of the N=21 baseline (4/8) rather than being a small-sample coincidence in the
   opposite direction.
3. **Three of five walls are starred** (load reached 13.99 with other work on the box). Load
   recorded at both ends of every arm. This is exactly why the primary meter is emitted
   characters. **No wall is claimed.**
4. **`table.py` — the report renderer and prediction scorer — was rewritten after arm 1
   launched**, because `FROZEN.sha256` recorded E-AFFORD's copy when it was copied with the
   other scorers. It was rewritten **before any arm result existed on disk**, and the proof
   is a computed receipt (`receipts/FROZEN-addendum-table.txt`: 0 `payload.json` and 0
   `rollout.jsonl` files in this root at the time). The three *measuring* instruments —
   `payload.py`, `strategy.py`, `secondaries.py` — are unchanged from `FROZEN.sha256` and
   re-hashed in that addendum.
5. **The dead-port negative control returned rc 2, not the rc 1 the brief predicted**, and
   found that the fail-closed guard lives in `run-arm.sh`/`attest.sh` rather than in
   `sol-yolo`. Reported in full in §8 rather than trimmed to a pass.
6. **The hand-driven `alias_migration` was sent over HTTP JSON-RPC to this cohort's own
   server on 7952**, not through the session's MCP tool binding, which points at a different
   server (7906) on an unknown build with an unrelated project-dir. Driving the pinned build
   on this cohort's own port is the stronger evidence; the session's tool binding would have
   proved something about a different server.
7. **Fixture `repo-80` was `git init`-ed by this runner** (the generator emits a tree, not a
   repo, and `run-arm.sh` clones from a repo). Authored `forge-anvil <forge-anvil@anvil>`.
8. **The `.codex/config.toml` `required = true` gap (§8) was NOT fixed here.** `sol-yolo` is
   a shared script and other cohorts were live on this box; the ratchet is proposed, with
   its exact one-line shape, rather than applied mid-flight.

## 12. One line of learning

**A behavioural law measured at one problem size is a law with a domain, and the domain is
the first thing to test — not the last.** We priced a product claim on "native emits ~450
characters per owner", verified it twice at N=21 across seventeen arms, and then discovered
that at N=80 the behaviour it describes **does not occur at all**: 0 of 3 arms typed a
patch, 3 of 3 wrote a generator, two of them beat the tool's own payload, and the constant
became literally uncomputable because its stratum was empty. The variable that broke the law
was never in the model — it was **N itself**, changing not the size of the output but the
*kind* of output, and the only way to find that was to run the same experiment at a size
nobody had run it at.

## 13. One caveat

**Three arms cannot tell you where the boundary is, or that it is a boundary rather than a
slope.** This cohort establishes that the N=21 behaviour does not hold at N=80; it does not
locate the crossing, and with N-3 emitting 12,576 characters it does not even establish that
the literal stratum is *empty* at N=80 rather than rare. Everything here is one caller
(`gpt-5.6-sol`, high reasoning effort), one harness (`apply_patch` as the offered write
verb), one task family, one k, one N, and generated fixtures. The experiment this one
implies is a **sweep, not a point**: N ∈ {21, 30, 40, 55, 80} at n ≥ 4, scored on the
strategy label rather than on characters, to find the N at which the literal-patch fraction
crosses one half. Until that runs, "the emission law is bounded to N ≲ 40" is a **pre-
registered restriction honouring a triggered withdrawal condition, not a measurement.**
