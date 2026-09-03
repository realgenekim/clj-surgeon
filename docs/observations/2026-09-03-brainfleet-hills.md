# Brainfleet: what's next — three independent rankings, one attack round, one verdict

*Gene, 2026-09-03 ~22:00Z, verbatim: "Look at potential hill climbs. Activate brainfleet to
prioritize what's next and go! Mayor (opus), feel free to chime in". Protocol (house rules,
"the mayor does not select either" + `fleet-poll-each-result`): Fable, Sol and Opus rank
independently WITHOUT seeing each other; each then attacks the others' top two; the
disagreement is the signal; Fable commits to a verdict and launches. The mayor contributes
live state (drain order, seats about to consume a square). Written by Fable BEFORE reading
Sol's or Opus's answer.*

## 0. Facts every ranking must respect (from the tech tree and tonight's log)

- The only vs-native result on record is the 2026-09-02 benchmark: 2x wall, 2x actions, no
  quality gain, when the tool is "available and expected"; free choice 0/10 (rf2 G5/G5b).
- Existing WINS vs native are on square 2 (fan-out) and correctness: sl1 — tool 1 call, 2–3
  returns, 24–27 s, 6/6 acceptance vs native 3–11 returns, 55–127 s, 2/6 passes (wall 4.5x,
  correctness decisive); E1 gate — wall-neutral (0.975x, p 0.79), correctness 6/6, catches
  hazards the suite passes.
- Tonight: ZERO vs-native measurements; usage watch 96/49/47 flat; 12 branches at GO
  unmerged (drain began 21:00Z, 0 merges landed by 21:52Z); apparatus GO at 77e6237 (tip).
- Everything runs locally on Anvil; arm servers 7907–7910; branch tips can be served from
  worktrees — a merge changes the receipt's provenance line, not the measurement.

## 1. Fable's ranking (written first)

| # | hill | square | pass line (machine-scorable) | prediction | cost | gated on | runnable tonight on tips? | one reason it could be waste |
|---|---|---|---|---|---|---|---|---|
| 1 | **E3 fan-out verb vs native, 21-owner rung** | 2 | tool: ONE write call; non-test actions ≤ 10.5; churn within 20%; correctness ≥ native; wall reported both ways | tool 2–3 returns vs native 9–10; wall ≤ 0.5x at N=21; tool 6/6 correct vs native ≤ 3/6 (sl1 precedent) | 12 arm-runs, 2 JVMs, ~2 h | q5z f51ceae (tip) + apparatus 77e6237 (tip) | YES | native lands the 21-owner change in one patch cell (it did once) and the tool's win is only returns, not wall |
| 2 | **E6 free-choice `:ls-tree` adoption via MCP** | 3 | agents call ls-tree once at the start and read fewer files; adoption count n/6; reads before first edit | adoption ≤ 2/6 (rf2 free choice 0/10); reads −30% in the adopters | 6 arm-runs, 1 JVM, ~1 h | study-ops 4480e3d (tip) + MEM-003 95b0881 (tip) | YES | if adoption is 0/6 the square is unmeasurable by free choice and needs a routing-prompt experiment first (E11's lesson: skill lines are ignored) |
| 3 | **E7 `prove`: load the candidate into the warm JVM, run named vars** | 4 | one return replaces the focused-suite return; catches ≥ 1 class the suite misses; false-green rate on a load-order fixture = 0 | prototype in one evening; 1 hazard class caught on the E5 stale-onset fixtures | prototype + 6 arm-runs | kernel adoption by a verb (5a2d254 latent) | NO (prototype yes; cohort no) | load order makes a false green; the square nobody has measured may be the square nobody needs |
| 4 | **Kernel first-verb adoption** (`require_change` under `with-cooperating-writes`) | enabler for 1 and 4 | verb commits through the journal; memory-red parity; zero behaviour change on the verb's golden | 4 h build + 2 review rounds | kernel 5a2d254 merged (or tip) | build yes; merge no | the kernel's value is only visible once a write fails mid-way — until then it is cost |
| 5 | **E4 T2**: intent by the strong model, hunks by the typist, verification by the gate | 1+2 | strong-model tokens −50%; wall break-even | wins on tokens only | 12 arm-runs | E3 | NO | E1 already says the gate is wall-neutral; T2 may just move cost between meters |
| 6 | **Census / fold-diff to GO** | none (hardening) | GO verdict | 1–2 more rounds each | 2 agent-hours | — | YES | fifteen rounds in, the reviewer widens the input space one notch per round; the return per round is falling |

**Two launches in the next hour, in parallel on Anvil: E3 and E6.** Both are pre-registered
(`2026-09-04-e3-e6-prestaged.md`), both run on tips, both produce a vs-native table for the
next Gene report, and they are the two squares the vision names as winnable. Everything else
either gates on them or is not on the vs-native axis.

**What Fable predicts the fleet will disagree on:** Sol will rank E7 higher (it is the novel
square); Opus will rank the kernel adoption higher (it turns latent work into value). Both are
right in a week; neither produces a number tonight.

## 4. Gene's ruling (before the fleet's answers were read), verbatim

> "I favor main functionality of speedups vs native, especially after so much non functional (but
> important) work. Let's prove it was worth it!!!! Let's target 50% functional work. But your call."
> "Let's target 50% functional work (overall). But your call."

**Fable's call:** E3 and E6 launch NOW on branch tips (q5z f51ceae + rf2 965d49e served for E3;
study-ops 4480e3d + MEM-003 95b0881 for E6; apparatus 77e6237), with the tip sha in every
attestation instead of waiting for the merges — a merge changes provenance, not the measurement.
The three hardening lanes in flight (census r15, kernel r9, fold-diff r11 — the last just closed
GO) get no successor round tonight unless a reviewer names a blocker; their remaining findings go
to the ledger. Until the Gene report opens with a vs-native table, the launch mix is 100%
functional; thereafter ≥ 50%. Sol's and Opus's rankings are appended below when read; they may
add a third hill, they do not delay these two.

## 5. The fleet's answers, verbatim (read AFTER §1 and §4 were written)

### 5a. Opus (file /home/forge/tmp/sol/fleet-opus.md)

# Opus — hill climbs for tonight (2026-09-03T22:0xZ, Anvil seat, read-only in clj-surgeon @ 3366992)

Independent planner: Opus. Gene, 22:00Z: *"Look at potential hill climbs. Activate brainfleet to
prioritize what's next and go!"*

---

## 0. Three findings that reorder everything before the list starts

**F1 — `~/acid` DOES NOT EXIST for this seat.** `ls /home/*/acid` → no such directory. The
pre-staged E3 file depends on two fixtures it says to *verify, not assume* — `~/acid/fanout/gen-fanout.clj`
and `~/acid/fanout/rescore-FAN.sh` — and on `~/acid/receipts/`. All absent. By the pre-registration's
own abort rule (`B.5`: *"If `gen-fanout.clj` is absent or not runnable by the seat, E3-P does not run"*),
**E3-P is dead as written.** Every frozen native diff from the 2026-09-02 cohorts is also unreachable,
which kills any zero-cost replay experiment. This is the single largest hidden blocker on the board and
nobody has written it down. The corollary is a doctrine item: **a fixture that lives in another seat's
home directory is not a fixture.** The rebuild belongs in git, under `bench/fanout/`.

**F2 — the "wait for the merge" gate is self-imposed, and it cost the whole of today.** The report's
headline is *ZERO vs-native measurements*, and §2 says why: *"Both are gated on merges that did not begin
until 21:00Z."* But validity in this program comes from **attestation** — `docs/vision.md`: *"server
identity read from the server, prompt hash, worktree commit, per arm, or the receipt is blind"* — not
from the server's source having been merged. `bridge/study-ops-mcp 4480e3d` and `bridge/q5z-alias-migration
f51ceae` are both **GO after 4 and 9 executed review rounds**, both fetched locally, both startable on
7907–7910 right now. **Ruling I am taking: arm servers are built from the GO branch tip, the tip's sha is
recorded as `:expected-server-sha` and read back from the server, and one line in the receipt states the
deviation** ("server built from bridge/study-ops-mcp 4480e3d, GO but unmerged; main at 3366992"). The
mayor's queue is not on the critical path for measurement. Nothing merges from Anvil and nothing here does.

**F3 — the box is at load 11.7, above the pre-registration's own PF-6 ceiling of 8**, driven by the review
lanes' JVMs (three suites live at 21:56Z). Arm walls are only comparable when slots run serially on a quiet
box (bridge log 07:25Z: *"walls here are contended and not comparable to sequential runs"*). Either the
review lanes park for the cohort window or every wall number tonight is void. **Only the deterministic
observables — adoption counts, write-call counts, native-fallback counts, acceptance gates — survive a busy
box.** I have deliberately made both launches' PRIMARY pass lines deterministic ones for exactly this reason.

Also standing: the Claude weekly limit fired once today. **Drive every arm with `~/bin/sol-yolo`** (codex,
gpt-5.6-sol, subscription, no limit). A limit hit mid-cohort voids the arms it lands on.

---

## 1. The ranked six

Floors quoted in every table below and never crossed: **wall 172 s, non-test actions 6.1, acceptance
0→4 failures on identical inputs (a gate, never a score)** — v1, receipt `3e26e1c`.

---

### Hill 1 — E6-Lb: study-ops free-choice adoption, with a native positive control

| | |
|---|---|
| **square** | 3 (questions grep answers wrong) — and the meta-test the whole program is judged by: *"Free-choice adoption is the acceptance test"* |
| **arm / control** | F = free choice, MCP at 7909 built from `bridge/study-ops-mcp 4480e3d`, n=3 · N = native positive control, no MCP in the driver config, `rg` + `apply_patch`, byte-identical prompt outside §5, n=3 |
| **pass line (machine-scorable)** | **≥2 of 3 F runs issue ≥1 `inspect_clojure` with `mode="ls-tree"`, and ≥1 of those first calls lands within the agent's first 3 model returns.** Deterministic; no variance floor; claimable at n=1. Secondary: distinct `.clj`/`.cljc` files opened before the first write, F vs N, claimed **only if the two ranges do not overlap**. GATE both arms: `acid_L_acceptance_test` 12/82/0, full kaocha 577/7784/0, goldens byte-identical, `grep -rn "System/currentTimeMillis" src/` = exactly 1 line |
| **prediction** | **adoption 0 of 3 — 65%**; 1 of 3 — 25%; ≥2 of 3 — 10%. Reads: F 9–13 files vs N 10–14, **ranges overlap, no claim**. Wall F within the 172 s floor of N (N ≈ 330–430 s on this rung). Prior is 0 of 10 free-choice adoptions across the program, the last with the exact one-call command named in the task's own terms |
| **cost** | 6 arm-runs serial ≈ **2.0–2.5 h wall**; ~1 agent-hour of setup; one JVM suite at a time under `flock` |
| **gated on** | **Nothing merged.** Apparatus `bridge/anvil-arms-apparatus 77e6237` (GO, self-tested), server `bridge/study-ops-mcp 4480e3d` (GO), rung L prompt + acceptance suite on `main`, `marvin-voice-remote` @ `ab267f9` present. **Runs tonight, on branch tips.** |
| **one reason it is a waste** | A 0/3 is the **eleventh consecutive** free-choice null. It buys confirmation of a prior `docs/vision.md` already asserts, and spends 2.5 h doing it. Counter: it is the *only* experiment on the board with zero prerequisites, and its native arm's three frozen diffs are Hill 5's entire input. |

---

### Hill 2 — E3-P: the fan-out verb vs native at N=21 (fixtures rebuilt into git first)

| | |
|---|---|
| **square** | 2 (fan-out) — *"the only square where wall can go positive, and only here"* |
| **arm / control** | T = write routed through `alias_migration`, server at `bridge/q5z-alias-migration f51ceae` on 7907, n=3 · N = native, n=3. Mirrored order `N-1 T-1 T-2 N-2 N-3 T-3` (the z7c correction — z7b's 0.76× was withdrawn because its native arm ran slow) |
| **pass line** | All six of B.2: (1) **exactly 1** committing `alias_migration` call in T (a refusal plus its own `next_call` counts as two and fails); (2) T non-test actions **≤ 10.5** per run — native's own n1 figure — with **>16.6 = layering**, the shipped per-form editor's signature; (3) churn within **±20 %** of the canonical, band written down before any arm runs; (4) **zero** native `apply_patch` hunks landing functional `.clj` bytes in T; (5) `rescore-FAN.sh <worktree> 21` green on all six checks, both arms; (6) wall reported, claimed only above 172 s |
| **prediction** | T = **1 write call (85%)**; T non-test actions **6–9** vs N **11–14**; T wall **0.65–0.85×** N; **all six lines pass: 55%**. Prior is strong and synthetic: sl1 measured T at 2–3 returns / 24–27 s / **6-of-6 acceptance / 0 refusals** at every N against native's 3–11 returns / 55–127 s and **2-of-6 acceptance** |
| **cost** | fixture rebuild ≈ **1.5 agent-hours**; 12 arm-runs ≈ **3–4 h wall**; one JVM at a time |
| **gated on** | The FAN generator + scorer, which **do not exist for this seat** (F1) and must be rebuilt — buildable tonight, no merge involved. Server = q5z tip `f51ceae` (GO). **Second half runs tonight, after the rebuild.** |
| **one reason it is a waste** | Rung P is synthetic, and C1 already found real repos are **alias-uniform** (median closable 3.4×, only 24 % clear 5×) — *"the 10× slope must be synthetic."* The spec says it against itself: *"a synthetic-only win is a finding about irregular fan-out, not a product claim."* |

---

### Hill 3 — sl1-R: the real-repo anchor, re-run against the fix

| | |
|---|---|
| **square** | 2, on the **real** shape — the one point in the whole slope experiment that is not synthetic |
| **arm / control** | cfp @ `d9afe8e9` (present locally), cloned to `/home/forge/tmp/arms/anchor`, the bridge checkout never modified. T at q5z `f51ceae` vs N native. The anchor is pre-registered as *"deliberately the adversarial anchor: the regular case where `sed` is right and native should win"* |
| **pass line** | Binary and correctness-only: **zero unmigrated sites of the two shapes that failed at load** — a qualified symbol in binding-vector position, and a quoted fully-qualified symbol in data position — plus r1–r7 green, **A = 0** native bytes landing after the verb, **B = 0** model returns between the receipt and the first compile (the `docs/vision.md` promotion instruments) |
| **prediction** | **T passes correctness: 60%.** Wall **T ≥ N** by design (native ~122–283 s across the two conflicting readings on record). The deliverable is a correctness receipt, not a speed row |
| **cost** | anchor scorer ≈ 1 agent-hour; **2 arm-runs ≈ 30 min**; collapses to near-zero if run as Hill 2's PF-4 hand-drive (see Launch B) |
| **gated on** | Nothing merged. **Runs tonight.** |
| **one reason it is a waste** | It is pre-registered as the case native should win, so a T loss is not news and a T win was never claimed. Only the binary — did the q5z fix hold on real bytes — is new. Counter: the record currently carries **two contradictory sl1-R readings** (283 s/228 s "both PASS" vs "native PASSES, tool arm FAILS at load"), and a program cannot ship on a contradiction. |

---

### Hill 4 — the gate on the route: a harness hook, not a prompt

| | |
|---|---|
| **square** | 1 (the gate), and the answer to the adoption problem the other five hills only measure |
| **the thesis** | Free-choice adoption is **0 of 10**, and `docs/vision.md` already names the fix in one clause: *"A tool's presence and name are not a path; **a harness that routes the write through the verb is.**"* Square 1's own claim is *"taken on the route the agent already uses, its value does not depend on the agent choosing us."* z7c already proved the gate is **wall-neutral** (0.975×, Welch p 0.79, n=9 vs 12) with correctness holding 6/6 and the gate at 7.4 % of wall. **Everything is in place except the routing.** So: install `admit_clojure_patch` as a pre-write hook in the harness, and stop asking agents to choose it |
| **pass line** | With the hook installed and Surgeon **never mentioned in the prompt**, on rung L: **100 % of `.clj` writes pass through the gate** (count of gated writes = count of `.clj` patches, deterministic); non-test actions and wall stay **within the floors** of an unhooked native run; acceptance gate green; **zero** false refusals on a clean run (a hook that blocks a correct patch is an outage, not a gate) |
| **prediction** | Routing coverage **100 % (85%)**; wall **+5–10 %** (the gate's measured 7.4 %); false-refusal rate **0 in 3 runs (70%)**; 1–2 substantive hazards named across 3 runs (**40%**) |
| **cost** | build ≈ **2 agent-hours** (Sol/Opus) + 6 arm-runs ≈ 2 h wall; gate branch `bridge/admit-gate 17125fe` exists at origin |
| **gated on** | Nothing merged, but it is a **build before it is a measurement**, so it does not produce a vs-native number tonight |
| **one reason it is a waste** | If the E1 grammar scar repeats — the gate refusing real `apply_patch` V4A bytes it has never been fed — the hook becomes a hard outage on the agent's only write path, i.e. it converts a wall-neutral tool into a blocker. Mitigation is non-negotiable: **fail-open on any shape refusal**, fail-closed only on a named substantive hazard. |

---

### Hill 5 — gate defect-differential replay over tonight's frozen native diffs

| | |
|---|---|
| **square** | 1 — the gate's actual claim is **correctness, not speed** (z7c) and the catch rate has never been measured on patches written by agents who were never told a gate existed |
| **arm / control** | No arms of its own. Take every `diff.patch` frozen by Hills 1–3's **native** slots (≥9 by dawn), feed each to `admit_clojure_patch` at `17125fe`, and compare its verdict against that arm's acceptance-suite verdict |
| **pass line** | **≥1 substantive hazard** (not a grammar/shape refusal) named on **≥20 %** of native diffs that the acceptance suite **passed**; shape-refusal rate **<10 %** (E1's hole must be gone); **zero** refusals on diffs a hand read finds clean |
| **prediction** | substantive catches **1–2 of 9 ≈ 15 %**; clears the 20 % line: **45%**. Shape refusals 0–1. Prior: z7c found exactly 1 substantive catch (blocking-lint-findings) in 6 runs, plus 7 shape refusals at 0.013 s |
| **cost** | **1 agent-hour, 1 JVM, ~30 min wall, ZERO arm-run budget** — the cheapest item on this list |
| **gated on** | Hills 1–3 having produced and frozen native diffs. Would have been free tonight if the historical diffs survived; they died with `~/acid` (F1) |
| **one reason it is a waste** | n≈9 against a comparator that spans 0→4 failures on identical inputs. A 1-of-9 result is inside everyone's noise and settles nothing. |

---

### Hill 6 — the adoption denominator (inb-46f90f): partition the usage watch by caller

| | |
|---|---|
| **square** | **None directly** — it is the instrument for loop rule 3 (*"watch how Surgeon is or isn't used, on a clock"*), not a competitor square |
| **pass line** | `make study-agent-usage` returns calls split into ≥2 **named** buckets (reviewer/probe sessions vs builder/agent sessions) over the identical window; the buckets **sum to 96**; the builder bucket is a number a reader can act on |
| **prediction** | builder bucket **≤10 of 96 (80%)**; reviewer/probe **≥85**. The 96/49/47 line has been flat since ~05:00Z while provider counters climbed claude 135→181, codex 543→693 — the movement was reviewers probing their own branch servers on 7906–7910 |
| **cost** | **~1 agent-hour, no JVM, 30–40 min** |
| **gated on** | Nothing. Runs tonight, and it is the only item here that does not contend for the box |
| **one reason it is a waste** | The contaminated meter already told the truth by being **flat** — nobody was fooled, and no decision tonight turns on the split. It is hygiene, filed and correct, but it wins no square. |

---

### Considered and deliberately NOT in the six

- **E3-L alone (the boundary control).** Runs tonight with zero fixtures, and I predict it **loses** (T ≥ N, 70%) — which is exactly what n1 already measured on this repo at 2.12× non-test actions. Running a predicted loss *without* the positive rung is spending the night re-proving the square we withdrew from. It rides along with Hill 2 or it does not run.
- **The naive-reader receipt probe** (fresh model, receipt only, "what is your next call?"). Prediction 3–4 of 6 pass, ≥1 receipt over the 4 KB bound. Cheap and real, but it yields a design finding on the night whose report leads with *no vs-native number*. File it for the morning.
- **Draining the merge queue.** Twelve GOs, five memory leaves, and it is the mayor's work. Nothing merges from Anvil. Attesting against branch tips (F2) removes it from tonight's critical path entirely.

---

## 2. The two I would launch in the next hour, in parallel

**Launch A = Hill 1 (E6-Lb).** **Launch B = Hill 2's preflight, which is also Hill 3.**

**Why these two.** A is the only experiment on the board that needs **nothing rebuilt and nothing merged**,
so it converts tonight's headline from *ZERO vs-native measurements* into a row before dawn; its primary
observable is a **count**, which survives the busy box (F3); and its native arm hands Hill 5 its entire
input for free. B removes the **only** blocker on the **only** square where wall can go positive (F1), and
its mandatory PF-4 hand-drive is Hill 3's correctness question answered by hand before a single arm-run of
budget is spent — the exact discipline whose absence cost the program a night on E1 (*"three red teams fed
the gate unified diffs and the field's first real payload was apply_patch V4A; the free-choice arm would
have found it in one run"*). They are **resource-disjoint**: A is serial arm-runs on port 7909 needing the
suite lock; B is a build plus one warm server on 7907 needing no suite. Disjoint file sets: A owns
`/home/forge/tmp/arms/e6/**`; B owns `/home/forge/tmp/arms/e3/**`, `/home/forge/tmp/arms/anchor/**` and a
new branch cut off `77e6237`. Neither merges anything; neither touches `~/acid`, 7888, 7894 or 7895.

---

### Launch A — E6-Lb, exact arm and control design

**Base.** `marvin-voice-remote` @ `ab267f9`; **six scratch clones**, one per arm-run, never shared
(the YOLO fence), under `/home/forge/tmp/arms/e6/slots/<arm>-<i>/worktree`.

**Rung Lb** = `docs/observations/2026-09-02-acid-rung-L/L-prompt-main.md` with **deletions only**:
CLAUSE 8 entire (the 21-owner `file:line` table — that is precisely the discovery `ls-tree` exists to
remove, and leaving it in guarantees zero adoption for a reason unrelated to the tool); CLAUSE 2's
ten-file enumeration (keep the instruction); any surviving `file:line` in CLAUSE 4. **Keep** the
"22 textual occurrences of `System/currentTimeMillis` under src/" count — CLAUSE 9 needs it or the task
becomes unbounded rather than discovery-heavy. Verify with `diff` and **assert zero added lines**.

**Prompt hygiene, identical bytes outside §5.** The A.8 ritual-strip block verbatim, with the one allowed
substitution `bin/fan-test` → `bin/kaocha --focus marvin-voice-remote.bridge3-new-test`; the
`TOOLCALLS: <n>` line, never `TURNS:` (the old line returned `1` from 6 of 6 runs — a null instrument);
and **no arm prompt mentions that Surgeon is "available and expected"** — that phrasing is a standing
1.8–2.1× tax Gene pulled from every Clojure prompt on 2026-09-02. Both prompts hashed; the hash goes into
every attestation.

**Arms.**
- **N (positive control)** — native. `rg` + `apply_patch`. **No MCP server in the driver config at all**,
  not a server the agent is told to ignore. n=3.
- **F (free choice)** — MCP `clj-surgeon` at `http://127.0.0.1:7909`, server built from
  `bridge/study-ops-mcp 4480e3d`, exposed and **not named as preferred**; neutral wording only. n=3.

**Order:** mirrored, serial, one at a time — `Lb-N-1, Lb-F-1, Lb-F-2, Lb-N-2, Lb-N-3, Lb-F-3`.

**Driver:** `~/bin/sol-yolo` (codex exec, gpt-5.6-sol, `model_reasoning_effort=high`) for all six, fixed
and recorded per arm. Rationale: the Claude weekly limit already fired once today.

**Meter:** `bench/anvil-arms` from a worktree at `77e6237` — `attest.sh` (writes `attest.json` **before**
the driver starts, exits 2 on `ATTEST-MISMATCH`) → `watch.py` (inode-bound rollout tail, child subreaper,
typed aborts on zero returns / rotated rollout / unbound session) → freeze `diff.patch` → `score.py`
(**no receipt at all** from a stream that does not validate). Attestation records: server sha **read back
from the server** and required to equal `4480e3d`, prompt hash, worktree commit, port, driver, boot id —
plus the one-line F2 deviation ("GO but unmerged; main at 3366992").

**Correctness predicate — arm-independent GATE, never a score.** In order, under `flock`:
`acid_L_acceptance_test.clj` copied in → `bin/kaocha --focus marvin-voice-remote.acid-l-acceptance-test`
= **12 tests / 82 assertions / 0 failures** → remove it → `bin/kaocha` = **577 / 7784 / 0** →
`clojure -J-Dmvr.data.dir=target/check-pages-data -M -e '(load-file "scripts/check_pages.clj")'` goldens
byte-identical → `grep -rn "System/currentTimeMillis" src/` prints **exactly one line**. **An arm that
fails its gate is VOID and re-run, never scored.**

**Scoring.** Primary = count of `inspect_clojure` calls with `mode="ls-tree"` in F, plus the **ordinal of
the first** among assistant returns (a call at return 14, after every file is read, scores `late`, not a
pass). Group-sequential, declared now so it is not p-hacking: **1 or 2 of 3 → run 3 more F slots, line
becomes ≥4 of 6 with ≥2 early; 0 of 3 → STOP, do not extend** (the prior is 0/10; three more nulls buy
nothing). Secondary reported as min–max ranges, never a bare mean.

**Byproduct to preserve deliberately:** freeze all three N-arm `diff.patch` files — they are Hill 5's
input, and the reason Hill 5 is not free tonight is that the last set died with `~/acid`.

**Load discipline:** `uptime` before every slot; **do not start a slot above load 8**. At 21:56Z the box
read **11.73** with three review JVMs live. Either the review lanes park for the cohort window, or the
wall column is void and only the deterministic columns are reported — say which, in the receipt.

---

### Launch B — rebuild the FAN fixtures into git, then hand-drive the verb on the real anchor

**B1 — the generator, in the repo, on a branch off `77e6237` (Sonnet or Sol, ~45 min, no JVM).**
Re-implement `gen-fanout.clj` from the sl1 spec: pure, deterministic, `--n N --seed 7`, **nested** target
sets (N=5 ⊂ 10 ⊂ 20 ⊂ 40 ⊂ 80 — the slope is the same files growing, not five unrelated tasks), emitting
`repo-N/`, `canonical-N/` (the oracle **derived** by applying the policy at generation time, never
hand-written), `manifest-N.edn` (targets, per-file alias, site coordinates, sha256 of every protected
decoy region) and the generated one-behaviour-per-site suite. **Land it under `bench/fanout/`, not in a
home directory** — F1 is the whole reason tonight's E3 cannot run.

**B2 — `rescore-FAN.sh <worktree> <N>` (~30 min), the spec's six checks verbatim:** file set equals the
manifest's target set exactly with no extras; **form equality as THE gate** (every changed file parses and
its form tree equals the canonical's modulo whitespace, comments, metadata, with `#_` discards present and
in place); protected-region sha256 equality (the sed-catcher); one process requires all namespaces with
zero errors; the generated suite at base count with an empty base failure set; residue
`rg -c 'store/find-event|acid\.fanout\.store\b'` over `src/` = 0 **and** no introduced alias shadowing an
existing binding.

**B3 — sabotage the scorer before trusting it (mandatory).** Generate at N=21 seed 7; prove
`rescore-FAN.sh` returns **6/6 GREEN on the canonical** and **RED on six single-property sabotages, one per
check**. A scorer that has never gone red is a verdict label, not a scorer — the exact scar where a chain
logged "pass" as a hardcoded word over a missing score file and no arm had ever produced a diff.

**B4 — the G1 hand-drive, watcher on (~20–30 min). This is PF-4 and it is non-negotiable.** Start a
Surgeon from `bridge/q5z-alias-migration f51ceae` on **7907**. Drive `alias_migration` **one call at a
time**: first on the synthetic N=21, then on the **real anchor** — cfp @ `d9afe8e9` cloned to
`/home/forge/tmp/arms/anchor`, the `~/src/curtaincall-cfp` checkout never modified — and specifically
re-test **the two shapes that failed at load**: a qualified symbol in **binding-vector position**, and a
**quoted fully-qualified symbol in data position**. The watcher's figure is the report, never the driver's
own count. Every refusal paid by hand goes into the friction ledger with its **exact text, a ratchet, and a
trigger**. This single cell answers Hill 3's binary before any arm-run budget is spent.

**B5 — PF-3 and PF-5.** Compute the churn band (`git diff --shortstat` between `repo-21/` and
`canonical-21/`, ±20 %) and **write it into the pre-registration before any arm runs**. Then run every A.7
predicate over the B4 rollout and confirm each returns a plausible **non-zero** number — **abort the
morning if any silently returns 0.**

**Exit condition:** E3-P armed with a fixture that lives in git; the anchor's correctness question answered
by hand; the churn band and the scorer both pre-registered and both proven able to fail.

---

## 3. What I would tell Gene in one line

The tool has two measured, real, vs-native wins on the board (rf2 0.72× wall with bytes_beyond_verb 0/0/0;
alias migration 0.81× wall and 171 files in one 62 s call) and **zero** free-choice adoptions in ten tries —
so the product problem is not capability any more, it is **routing**, and the two hills that matter are
measuring the routing (Hill 1, tonight) and building it into the harness instead of the prompt (Hill 4,
this week).


### 5b. Sol (file /home/forge/tmp/sol/fleet-sol-answer.md)

# Sol's hill-climb priority, 2026-09-03 ~22:00Z

## Read of the field

There is no fresh win-vs-native result tonight. The usage meter stayed at **96
`inspect_clojure` calls / 49 ok / 47 refused** while the Claude and Codex provider
counters kept moving: no builder adopted the seat's Surgeon. The only clean current
comparison against the requested native floor (`grep`/`rg` + one `apply_patch`) remains
the 2026-09-02 result in which the shipped per-form route cost about **2x wall** by
layering. The memory numbers are Surgeon-before versus Surgeon-after, not vs-native.

The branch state matters to every recommendation below. `origin/main` contains the
ANDON fix at `a6df86ee`, but **zero of the twelve queued GO branches has landed**.
In particular, q5z `f51ceae`, study-ops `4480e3d`, read-path `b7ef23d`, parser-admission
`52c5d85`, apparatus `77e6237`, MEM-003 `95b0881`, rf2 `965d49e`, and the approved
kernel merge point `5a2d254` are not on main. A raw feature tip also predates the
ANDON fix. The apparatus tip is usable as measuring equipment; a raw feature tip is
not an acceptable server subject for a scored cohort.

For every cohort below the machine receipt should lead with at least:

```text
{arm, base_sha, server_sha, prompt_sha, wall_s, model_returns,
 non_test_actions, correct, native_clj_apply_patch_count, refusal_counts}
```

`correct` is the arm-independent acceptance gate, never a quality score. At n=3, a
wall gap below **172 s** and a non-test-action gap below **6.1** are reported as inside
the measured floor. A winner additionally requires the vision's composition law:
**A = 0 native bytes after the verb, B = 0 model returns from receipt to first
compile/test, equal acceptance, and zero native fallback.**

## Ranked hills

### 1. E3-P: one structural fan-out intent, N=21

- **Vision square:** 2, fan-out across N owners with tool-side discovery. This is the
  only square currently predicted to make wall positive.
- **Machine-scored hypothesis/pass line:** Run matched N and T arms on the same
  generated `fanout-21`, seed 7 base. Each row reports `(arm, wall_s,
  model_returns, non_test_actions, correct)`. T passes only if all three valid runs
  have exactly **one committing `alias_migration` call**, mean non-test actions
  **<=10.5**, **zero** functional `.clj` hunks through native `apply_patch`, churn
  within **+/-20%** of `canonical-21`, typed refusals at most **20%** of tool calls,
  and the six-check FAN gate green. N and T must both be correct. Wall is a win only
  if the T/N gap exceeds **172 s**; otherwise record direction only. Run the E3-L
  boundary control afterward: one `require_change` for ten ns forms, native
  `apply_patch` for the 21 host-interop sites, the canonical +59/-34 churn band, and
  acceptance green.
- **Concrete prompts/control:** The shared task retires
  `acid.fanout.store/find-event` for `acid.fanout.store2/fetch-event` in exactly 21 of
  100 namespaces, using the per-file alias order `store2`, `st2`, `es`, `store-2`,
  while preserving the local binding, strings, docstrings, comments, metadata,
  reader conditionals, discards, and same-named vars from other libs. T says:
  “Route the write through one `alias_migration` call; it discovers all owners and
  sites; trust its receipt; send an executable `next_call` once if refused; use
  native tools only if it cannot complete.” N has no MCP URL and says: “Use `rg` or
  `grep` to discover the same sites and one batched `apply_patch` to land the change.”
  Both run the same `bin/fan-test` once.
- **Predicted result:** T **3/3 one-call, 0 fallback, 3/3 correct**, about **3-5
  non-test actions and 25-35 s**; N about **7-11 actions and 80-120 s**. Central
  estimate is roughly **60-90 s saved**, directionally good but still inside the
  172 s wall floor. E3-L should be parity or a small T loss because the verb cannot
  discover the host-interop sites.
- **Cost:** Full registered pair: **12 arm-runs**, about **3-4 agent-hours** and
  **2.5-3.5 h wall**, six transient tool-server JVM lifetimes, with every acceptance
  JVM serialized under `/home/forge/tmp/suite.lock`. First useful slice is the
  15-20 minute G1 plus the first mirrored P N/T pair.
- **Gate / branch-tip tonight:** Apparatus `77e6237` is ready as tooling. Scored E3-P
  waits for q5z `f51ceae`, read-path `b7ef23d`, and parser-admission `52c5d85` to be
  merged, then `alias_migration` must plan the N=21 rung at `-Xmx512m`; rf2 `965d49e`
  should precede E3-L or that arm must carry the “demoted `require_change`” caveat.
  **No scored cohort on a raw tip tonight.** A raw q5z tip lacks the ANDON fix and a
  locally composed SHA would be an unreviewed, different subject. Prepare fixtures,
  prompts, attestations, and scorers now; run G1 only after there is an attested safe
  server SHA.
- **Why it could be waste:** E3-P is synthetic. A beautiful one-call result may say
  only that irregular alias fan-out is closable; the catalogue says the largest real
  class, parameter threading, is not closable, and the wall effect may be below the
  variance floor.

### 2. E6-Lb: free-choice adoption of workspace `ls-tree`

- **Vision square:** 3, questions grep answers wrong.
- **Machine-scored hypothesis/pass line:** On the same `ab267f9` Lb base, F passes if
  at least **2/3** runs call `inspect_clojure` with `mode="ls-tree"` and at least one
  first calls it within the first **3 model returns**. If adoption is exactly 1/3 or
  2/3, extend F to n=6 and require **>=4/6**, with at least two early; **0/3 stops**
  and closes the exposure. Both arms must pass the same acceptance gate. Report the
  min-max of distinct source files opened before the first write; claim fewer reads
  only if the ranges do not overlap. Report wall/actions under the 172 s/6.1 floors.
- **Concrete prompts/control:** Remove only CLAUSE 8's owner map and CLAUSE 2's
  ten-file enumeration from rung L; retain the task and the count of 22 textual
  occurrences. F says that `inspect_clojure` can provide `ls-tree`, `deps`, `topo`,
  `ls-deps`, `ls-extract`, `outline`, `forms`, and `owners`, then ends: “Use whatever
  route you judge fastest and safe. Nothing here is required.” N receives the exact
  same Lb task, no MCP configuration, and: “Use `rg`/`grep` for discovery and one
  batched `apply_patch` for the change.” Both use the identical ritual-strip block
  and the same acceptance suite.
- **Predicted result:** **0/3 F adoption**, hence an immediate measured negative and
  the next entry in the existing 0/10 free-choice prior. Predict overlapping read and
  action ranges, no wall claim, and equal valid-arm correctness. That is still a
  decisive result: the MCP exposure has not shipped.
- **Cost:** **6 arm-runs**, about **2 agent-hours / 1.5 h wall**, three transient
  server JVMs; add three F runs only under the registered extension rule.
- **Gate / branch-tip tonight:** Official E6 is hard-gated on study-ops `4480e3d`
  being merged after its executed review; read-path/parser admission and streaming
  `95b0881` should be in the attested server composition. **No scored raw-tip run
  tonight.** This is especially strict here: `ls-tree` directly reaches the code
  class fixed by the ANDON, while `4480e3d` predates `a6df86ee`. Build and smoke the
  Lb prompt and scorer now; do not point an autonomous arm at the raw study tip.
- **Why it could be waste:** The 0/10 prior and today's flat usage meter already make
  a null result likely. Lb is also an engineered “map removed” task, not organic
  repository navigation.

### 3. Real-repository q5z anchor: composition, not another synthetic slope point

- **Vision square:** 2, fan-out; it is the real-repo check that can turn E3-P from an
  apparatus finding into a product claim.
- **Machine-scored hypothesis/pass line:** On one pinned real base, compare N
  (`rg`/`grep` + one `apply_patch`) with T (one mandated `alias_migration`) for the
  catalogue's store -> event-store rename. T passes at 3/3 only with exactly one
  committing call, **A=0**, **B=0**, zero native fallback, exact alias/site oracle,
  bounded receipt, and the same green compile/tests as N. Report
  `(arm, wall_s, returns, actions, correct)`; speed needs >172 s and actions >6.1.
- **Concrete prompts/control:** Both prompts specify the old/new lib and var, ordered
  per-file alias policy, protected strings/comments/discards/locals, and identical
  done conditions. T routes the entire write through the verb and treats the receipt
  as terminal. N discovers with grep and lands one atomic batched patch; no scripted
  third method and no MCP endpoint.
- **Predicted result:** Repeat the existing direction, approximately **228 s / 9
  actions T versus 283 s / 13 actions N**, with **3/3 A=B=0 and correct**. The likely
  55 s / 4-action gaps remain inside both floors, so composition is the claim, not
  speed.
- **Cost:** **6 arm-runs**, about **2 agent-hours / 1.5-2 h wall**, three server JVMs
  and serialized suite JVMs.
- **Gate / branch-tip tonight:** q5z `f51ceae`, rf2 `965d49e`, read-path `b7ef23d`,
  and parser admission `52c5d85` must land in the attested server. **No valid raw-tip
  product cohort tonight**; a separately pre-registered engineering preview could
  use a reviewed integration SHA, not any current raw tip.
- **Why it could be waste:** It partly repeats the unreplicated 228-vs-283 anchor and
  is unlikely to clear either variance floor at n=3.

### 4. Gate sensitivity: prove correctness on seeded hazards

- **Vision square:** 1, verification after the agent's own patch.
- **Machine-scored hypothesis/pass line:** For paired clean and defect-seeded patches,
  T must submit the native patch once to `admit_clojure_patch`; all stale-onset and
  shadowed-kwCheck seeds must be refused with the correct typed reason and executable
  `next_call`, all clean patches must commit with complete verification, `verify
  none` must occur **0** times, post-write rereads/diffs must be **0**, and final
  acceptance must be green. N uses grep + `apply_patch` and its normal focused test.
  Score catch rate and false-refusal rate first; wall is secondary.
- **Concrete prompts/control:** Same requested change and same base in both arms. T:
  “Use ordinary `apply_patch`, then pass that exact apply-patch-format patch to the
  gate once; the receipt replaces reread, diff, and focused test.” N: “Locate with
  grep, apply once, then run the named focused test.” The harness, not either prompt,
  injects the known hazard variants and holds the oracle blind.
- **Predicted result:** **6/6 seeded hazards caught, 6/6 clean patches admitted,
  0 post-write probes**, but wall about **0.98x** on the long rung and about **1.9x
  worse** on the two-minute rung. This would establish a correctness product, not a
  speed product.
- **Cost:** A focused 12-run replay is about **3 agent-hours / 2-3 h wall**, six
  transient server JVMs plus one suite JVM at a time.
- **Gate / branch-tip tonight:** Needs the final gate line (the tech tree names
  `17125fe`) plus receipt-ratchets `c5ef7ca` and the merged ANDON-safe main, followed
  by an attested hand-drive. It is not in the current GO drain as a complete
  composition. **Not runnable as a scored branch-tip cohort tonight.**
- **Why it could be waste:** A curated replay can overfit two known defects while the
  gate still costs 1.9x on ordinary small work.

### 5. E7 `prove`: exercise unwritten candidate bytes in the warm JVM

- **Vision square:** 4, proof before write.
- **Machine-scored hypothesis/pass line:** T makes exactly one `prove` call with
  candidate bytes and named vars, writes **0 bytes before verdict**, issues no
  separate focused-suite action, returns the correct verdict on **6/6** good/bad
  cases, and then lands a patch whose final acceptance is green. N uses grep +
  `apply_patch` followed by the same focused test. Pass requires at least **one model
  return removed per valid T run**; wall is reported separately.
- **Concrete prompts/control:** Give both arms the same small semantic change and
  named behavioral assertions. T says: “Before writing, send the candidate and these
  exact vars to `prove`; if green, land exactly those bytes once.” N says: “Discover
  with grep, apply once, then run the named focused test.” Include adversarial load
  order and stale-var cases in the arm-independent oracle.
- **Predicted result:** **1 return removed/run**, **6/6** correct verdicts, and roughly
  **9-50 s** saved depending on suite duration; no wall claim is expected at n=3.
- **Cost:** Prototype plus six arms: **8-12 agent-hours**, roughly **4-6 h wall**, a
  warm server/nREPL JVM and serialized acceptance JVMs.
- **Gate / branch-tip tonight:** Gated on the gate substrate and the full LID
  design->LLD->EARS->tests->code sequence. No implementation exists. **Cannot run on
  a branch tip tonight; only write the experiment/design brief.**
- **Why it could be waste:** Load-order or stale-runtime false greens could make it a
  second, weaker test runner that adds a return instead of removing one.

### 6. Adopt the latent disk-journal kernel in the fan-out verb

- **Vision square:** 2 enabler: safe, bounded fan-out on repositories larger than the
  in-memory transaction can admit.
- **Machine-scored hypothesis/pass line:** Compare the current transaction with
  `alias_migration` routed through B1 on the identical 600-file fixture at
  `-Xmx256m`. The old arm must reproduce OOM/refusal; the adopted arm must commit,
  produce exact result-hash parity, retain **<=20 MB** (prior kernel result **14.9
  MB**), leave no half state under every injected interruption, and still satisfy
  **A=B=0**. This is a Surgeon-vs-Surgeon mechanism gate; a later N/T cohort is still
  required for any vs-native claim.
- **Concrete prompts/control:** This is initially a harness/kernel test, not an agent
  prompt. The later task prompt is the same whole-intent alias migration as hill 3;
  T receives one journal-backed verb, while N receives only grep + one batched
  `apply_patch` on the same base.
- **Predicted result:** Old path OOM/refusal; adopted path **100% commit parity at
  about 15 MB retained** and zero half states. No immediate wall win is claimed.
- **Cost:** **8-14 agent-hours / 1-2 days**, repeated bounded-heap JVMs and the one
  suite lock.
- **Gate / branch-tip tonight:** Pin the approved kernel at **`5a2d254`**, not its
  later unapproved branch head; q5z must also be merged. Adoption changes scoped MCP
  code and therefore requires the six-phase LID stops and a new independent red team.
  **A local stacked-tip prototype could be built later, but no valid product or
  cohort run exists tonight.**
- **Why it could be waste:** q5z already refuses over its aggregate-byte ceiling;
  normal real repositories may never need the kernel, while adoption imports a large
  concurrency and recovery surface.

## The two launches for the next hour

I would launch **the E3-P lane and the E6-Lb lane**, in parallel only through their
non-measurement preflight, using the cohort-ready apparatus at `77e6237`:

1. **E3-P preparation lane**, root `/home/forge/tmp/arms/e3`, reserving port **7907**:
   materialize the exact N=21/seed=7 fixture without writing any `~/acid` GO/lock
   file, install and hash the N/T prompts above, compute canonical churn, and smoke
   every scorer against retained/synthetic rollout data. The native prompt has no
   MCP stanza and explicitly uses grep + one batched `apply_patch`.
2. **E6-Lb preparation lane**, root `/home/forge/tmp/arms/e6`, reserving port **7909**:
   produce Lb by deletions only, install and hash the N/F prompts, and smoke the
   ordered-pass adoption counter so an empty rollout aborts instead of scoring zero.
   Its native prompt likewise has no MCP stanza and uses grep + one batched
   `apply_patch`.

These are the two because E3 tests the only square with a credible wall mechanism,
while E6 can cheaply decide whether the read surface is taken at all. Together they
answer the two facts tonight lacks: **a current matched native comparison** and **a
real free-choice adoption count**. Hill 3 duplicates an anchor before E3 establishes
the synthetic mechanism; hill 4 is already wall-neutral and needs a clean gate
composition; hill 5 has no prototype; hill 6 is latent infrastructure with no
current native bottleneck.

The red line: **do not start either scored arm on today's raw feature tips, and do not
run the two timed cohorts concurrently.** The formal E3/E6 pre-registration says E3
arms are serial and E6 may enter only E3's scoring gaps; contended E3 wall is void.
When the mayor lands the required stack, attest the merged server SHA, run PF/G1,
then start E3 first. E6 can use scoring gaps, with every JVM suite taking
`/home/forge/tmp/suite.lock`. Port **7908** remains E3-L, **7910** is spare. Never
contact 7888/7894/7895, never touch `~/acid/GO-*` or its cohort lock, and nothing is
merged from Anvil.

If “launch” is intended to mean “start two autonomous timed arms now,” my answer is
**none**: doing that would violate the attestation/pre-registration and host-contention
rules. The productive next-hour move is to make both meters ready and stop each at
its merge gate, so the first mayor landing turns immediately into GO rather than
another setup hour.


## 6. Where the three disagree — the signal

| question | Fable | Opus | Sol |
|---|---|---|---|
| first launch | E3 + E6 in parallel | E6-Lb now; E3 preflight (rebuild fixtures) | E3-P first, but NOT tonight |
| run on branch tips tonight? | YES, tip sha attested | YES — "validity comes from attestation, not merge" | NO — "a raw q5z tip lacks the ANDON fix … an unreviewed, different subject" |
| E6 prediction | adoption ≤ 2/3 | 0/3 at 65% | 0/3 — "an immediate measured negative" |
| E3 wall prediction | ≤ 0.5x at N=21 | 0.65–0.85x, all six lines pass 55% | 60–90 s saved, inside the 172 s floor |
| what nobody else saw | — | ~/acid does not exist for this seat → E3-P dead as written; load 11.7 > PF-6 ceiling | tips predate the andon fix; ls-tree reaches the fixed function |

## 7. Verdict (Fable, 22:05Z)

Both cohorts run TONIGHT (Gene: "prove it was worth it"; 50% functional floor). Sol's objection is honored the cheap way: each arm server is built from the tip MERGED LOCALLY with current origin/main so it carries a6df86ee, and the attestation records tip sha, main sha and the merge sha as read back from the server, with a deviation line "not a reviewed sha". Opus's finding is honored: E3 rebuilds gen-fanout/rescore-FAN into git under bench/fanout on bridge/fanout-fixtures-in-git, sabotages the scorer six ways, and hand-drives alias_migration at the tip before any arm; the cohort follows only if PF-1..6 are green. E6-Lb runs first (nothing to rebuild), group-sequential, 0/3 stops. Wall is reported but void above load 8; the primary pass lines are counts. Sol's hill 3 (real-repo q5z anchor) and Opus's hill 4 (gate as a harness hook) are the next two after these land. Nobody picked one; all three said the product problem is now ROUTING, not capability.

## 8. The mayor's chime-in (/tmp/mayor-2210.txt, read 22:06Z) and where it lands

The mayor's three survivors of Gene's 2026-09-02 routing ruling: (a) the six measured winners;
(b) the READ/discovery path ("every order-of-magnitude win tonight is on the READ side");
(c) the GATE ("the ONLY square that works WITH the native-default ruling"). Its fleet-state ask:
cap new launches until the drain is under four; spend builders on census and fold-diff.
**Fable's ruling stands** — E6 measures exactly (b) by free choice, and Gene's own words
tonight outrank the cap: "prove it was worth it", "target 50% functional". The gate square (c)
is the fleet's shared next hill after E3/E6: Opus's hill 4 (gate as a harness hook) and Sol's
hill 4 (gate sensitivity on seeded hazards) are the same square from two sides. The drain ask
is honored the way the mayor asked at 2230Z: one integration branch, composed here.

## 9. First result: E6-Lb (22:42Z) — adoption 0/3, falsifier fired, 0/13 program-wide

| arm | wall s | returns | tool calls | ls-tree calls | files read before first edit | correct |
|---|---|---|---|---|---|---|
| N-1 | 157 | 3 | 8 | 0 | 0 | green |
| F-1 | 111 | 4 | 7 | 0 | 1 | green |
| F-2 | 132 | 4 | 8 | 0 | 1 | green |
| N-2 | 116 | 5 | 8 | 0 | 4 | green |
| N-3 | 119 | 4 | 7 | 0 | 10 | green |
| F-3 | 107 | 4 | 7 | 0 | 4 | green |

Predictions scored: Opus "0/3 at 65%" — right; Sol "0/3 — an immediate measured negative" — right; Fable "≤ 2/3" — right but loosest. Mechanism candidate inb-3e298e (text content header-only). Caveat: Lb not blind. Receipt: docs/observations/2026-09-04-e6-lb-cohort.md (de5d7fb). Next wave polled from the fleet (§10 when it lands).
