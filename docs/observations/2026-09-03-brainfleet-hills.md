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

## 10. Result poll on E6 — Opus (file /home/forge/tmp/sol/e6poll-opus.md, 22:47Z)

# Opus — poll on the E6-Lb result (adoption 0/3)

*Anvil seat, read-only in `/home/forge/src/clj-surgeon` @ `08bfcb1`, 2026-09-04. Sources read:
`docs/observations/2026-09-04-e6-lb-cohort.md`, `2026-09-03-brainfleet-hills.md` §1–§9 (my own
ranking is §5a, prediction "0/3 at 65%" — scored right), `docs/vision.md` squares 1 and 3,
`2026-09-04-e3-e6-prestaged.md` C.1–C.3, and the six per-arm receipts under
`/home/forge/tmp/arms/e6` including all six `rollout.jsonl`.*

---

## 1. Interpretation of the 0/3 — five lines

1. **Routing and product, not client rendering and not wiring.** The decline is made *before any
   receipt is seen*; each arm is a fresh `CODEX_HOME` seeded with `auth.json` only, so a
   header-only text block the agent never saw cannot be its cause. Rendering is a downstream
   blocker, not this mechanism.
2. **The rollouts show `rg` is not a competitor to `ls-tree` — it is a strictly better version of
   it for this task.** F-1's first call is `rg -n -C 5 'System/currentTimeMillis|\(ns marvin-voice-remote' src`;
   F-3's and N-1's is `rg -n -C 8 'System/currentTimeMillis|\*now-ms\*|\(ns ' src`. One call returns
   the 22 sites **and** every `ns` form **and** the line numbers to edit by. That is `ls-tree`'s
   product plus the hit list, on a route the agent already types from memory.
3. **All six arms are step-isomorphic** — `rg` → read requires/headers → one `apply_patch` →
   `grep -rn` sweep → focused kaocha → `git diff --stat`, in 7–8 calls and 3–5 returns, F and N
   indistinguishable. `inspect_clojure` appears exactly twice in each F rollout: both are the
   prompt's own §5 text. No reasoning summary weighs the tool and rejects it; it is never
   considered.
4. **Had it been called it would have under-delivered.** PF-4's `pf4/c2.json`: at `limit=16384`
   with `grep="System/currentTimeMillis"` on a **10-file** `src`, the receipt is
   `6 of 10 files · read_complete=false · → narrow_scope`, and `content[0].text` is 146 chars of
   header with zero rows. So the fix is two defects, not one — text rendering **and** a payload
   bound that cannot serve a toy tree — and until both are fixed, square 3's foundation cannot
   answer "exactly, once" where `rg` answers completely in one call.
5. **The null is credible, not proven, per-arm.** The F arms' server telemetry contains only
   `server.start`; there is no in-arm `tools/list`/session witness bound to the arm's `run_id`,
   and `~/bin/sol-yolo`'s `-c mcp_servers.clj-surgeon.url=…` path (the one all three F arms took —
   the worktree had no `.codex/config.toml`) **omits `required = true`**, which the config-file
   path sets. A failed connection would have been silent. The wiring probe is a post-hoc
   corroborator on a different port and worktree, not the authority for these three sessions.

**Apparatus ratchet (one line, not an experiment):** set `required = true` on sol-yolo's `-c` path,
and make the apparatus assert a per-arm MCP session/tools-list telemetry event carrying that arm's
`run_id` before any adoption number is scored. Cheap; it upgrades every future free-choice null
from credible to proven.

---

## 2. Four next-wave experiments

Floors quoted and never crossed: wall **172 s**, non-test actions **6.1**, acceptance
**0→4 failures on identical inputs** (a gate, never a score). Everything below runs locally on
Anvil, servers on **7909/7910**, tip ∪ `origin/main` merged locally (the E6 server sha
`f24812b09ddd` is reusable as-is), nothing merged, nothing pushed.

### O1 — **E6-Q3: square 3 on its own terms** (correctness primary, adoption demoted)

- **Why:** free-choice adoption has now been asked 13 times and answered the same way. The question
  never asked is the one the square is actually built on: *does code-as-data answer questions grep
  answers wrong, on this repo, better than an agent with `rg`?* A mandated arm is legitimate here
  because the primary is capability, not choice.
- **Design:** read-only task, no writes, no gate ambiguity. Six probes of the shapes `docs/vision.md`
  names: (1) every caller of a named Var with the arity used; (2) which textual occurrences of a
  symbol are inside strings/comments; (3) which reader-conditional branch is live for `:clj`;
  (4) what a namespace requires and what it exposes publicly; (5) which callers use which arity of a
  multi-arity Var; (6) which namespaces break if a Var is renamed. Agent writes `answers.edn`.
  Ground truth is **built, hand-verified and frozen with its sha before any arm runs**. Three arms:
  **N** native, **M** mandated (`inspect_clojure` required), **F** free choice.
- **Pass line (machine-scorable):** `answers.edn` exact-match against the frozen truth;
  **M ≥ 5 of 6 correct in ≥ 2 of 3 runs, and M − N ≥ +1 in ≥ 2 of the 3 mirrored pairs.**
  Secondary, reported: F adoption count; returns and wall (claimed only above the floors).
- **Predicted:** N median **4 of 6** (probes 2 and 6 are where `rg` returns candidates it must then
  read to reject); M median **6 of 6**; the M−N line clears at **55%**; F adoption **0 of 3 at 80%**.
- **Cost:** 9 arm-runs (~60–120 s each, read-only) ≈ **1.0–1.5 h wall**; **2 agent-hours** to build
  and freeze the truth set + grader.
- **Tonight on:** 7909, server `f24812b09ddd` (already built and attested by E6); `marvin-voice-remote @ ab267f9`.
- **One reason it is waste:** if native scores 6/6, square 3 is empty and should be withdrawn like
  the single-edit square — a real finding, but it ends the square rather than winning it. Secondary
  risk: an arguable ground truth turns a mechanical grade into a judge call.

### O2 — **PF-5: one-call sufficiency** (the fix, benched — zero arm-runs)

- **Why:** every future square-3 experiment is uninterpretable while a call returns a 146-char
  header and `read_complete=false` on ten files. This is the gate on O1/O3, not a cohort.
- **Pass line:** on the Lb worktree, **one** `inspect_clojure mode="ls-tree" dir="src"
  grep="System/currentTimeMillis"` call returns, in `content[0].text`, **≥ 22 hit line numbers across
  ≥ 10 files with `read_complete=true`, in ≤ 8 KB**, and the identical call against `f24812b09ddd`
  fails at least one of those clauses. Both receipts frozen as JSON in the log.
- **Predicted:** pre-fix FAIL — **certain** (already measured: 6 of 10, `read_complete=false`,
  text 146 chars). Post-fix PASS at **70%**. Payload **4–8 KB** vs the `rg -n -C 8` output the arms
  actually used (~6 KB), i.e. **~1.0× not ≪1×, at 60%** — which, if true, is itself the product
  verdict for this rung.
- **Cost:** **0 arm-runs**, **2–3 agent-hours** (Sol builds: render rows into the text block, and
  charge the limit against rows rather than the header), ~45 min wall.
- **Tonight on:** 7910, hand-driven, tip ∪ main.
- **One reason it is waste:** the fix cannot move adoption by itself (a fresh session cannot know
  the payload changed), so if it is scored as an adoption experiment it will read as a 14th null.

### O3 — **E6-Bb: blind rung + a "when" plate in the tool description** (a 3-run screen)

- **Why:** it closes the receipt's own caveat (rung Lb still names 8 of 10 owner namespaces in
  clauses 3–4) and the last cheap prompt-side lever, in one screening cohort. The plate belongs in
  the **MCP tool description** — always on the model's table, and it survives when we do not own the
  prompt — not in more §5 prose.
- **Design:** rung **Bb** = Lb minus the namespace names in clauses 3/4 and minus the "22 textual
  occurrences" count, with definition-of-done restated as the sweep the acceptance suite already
  runs. Tool description gains one WHEN sentence ("call this when you do not yet know which files
  define or use a symbol; it answers in one call what a grep answers in three"). F only, n=3,
  against the frozen E6 N arms as the comparator.
- **Pass line:** **≥ 1 of 3 F runs issues an `ls-tree` call within its first 3 model returns.**
  (Deliberately lower than E6's 2/3: this is a go/no-go on spending 6 more runs to de-confound the
  two levers.)
- **Predicted:** **0 of 3 at 70%**, ≥1 of 3 at **30%**. If it fires, de-confound with 3 + 3
  (plate-only, blind-only).
- **Cost:** **3 arm-runs** ≈ 40 min wall; **1 agent-hour** (rung diff + description edit, both
  deletions-plus-one-sentence, asserted mechanically).
- **Tonight on:** 7909, tip ∪ main; needs O2 first only if a *positive* is to be interpretable.
- **One reason it is waste:** two levers move at once, and the prior says the result is the
  fourteenth null — a confounded null teaches nothing a clean null would not have.

### O4 — **Route, don't ask: the read-side hook** (square 3's version of hill 4)

- **Why:** `docs/vision.md` already rules the mechanism — *"A tool's presence and name are not a
  path; a harness that routes the write through the verb is"* and *"sit on the agent's route."*
  Thirteen nulls say asking does not work. The read side has never been hooked.
- **Design:** a shim early on the arm worktree's `PATH` that intercepts `rg`/`grep` invocations
  scoped to `.clj`/`.cljc`, serves them through the Surgeon read path, and appends the structural
  rows to the ordinary hit list. **Surgeon is never named in the prompt.** Wrapper keeps a log;
  fail-open on any refusal (the E1 grammar scar, read side).
- **Pass line:** **100% of clj-scoped grep invocations routed** (routed count = clj-scoped count,
  deterministic from the wrapper log); acceptance gate green in 3 of 3; wall within the **172 s**
  floor of the unhooked N arms; **zero** runs in which the agent abandons the hooked path
  (fallback count = 0); non-test actions within the **6.1** floor.
- **Predicted:** coverage **100% at 85%**; correctness green **3 of 3 at 80%**; **returns saved: 0
  (median), at 60%** — my honest prediction is that enriching an `rg` answer changes bytes, not
  round-trips, and vision's own constraint is *count returns, not milliseconds*. A measured 0 here
  is the most valuable number on this list, because it distinguishes "agents won't choose us" from
  "there was nothing to choose."
- **Cost:** **2–3 agent-hours** build; **6 arm-runs** ≈ 1.5 h wall.
- **Tonight on:** 7909, tip ∪ main; build tonight, cohort after O2.
- **One reason it is waste:** if the hook is fully transparent it measures nothing new; if it is not
  transparent it is an outage on the agent's only discovery path.

---

## 3. Rating the four candidates against mine

| candidate | verdict | rank | why |
|---|---|---|---|
| **(d)** drop free choice; square 3 as correctness on a read-only task, N vs F | **Best of the four — this is my O1**, upgraded with a **mandated M arm** | **1** | It changes the primary from a question answered 13 times to the one never asked. Without an M arm a decline in F makes F ≡ N and the cohort measures nothing — the exact failure mode E6-Lb just demonstrated. |
| **(a)** fix text rendering, re-run 3 F arms on the same rung | **Split it: the fix is essential (my O2); the re-run is near-certain waste** | fix **2**, re-run **4** | The agent never called the tool, so the rendering it never saw cannot be the mechanism, and a fresh session cannot know the payload changed. Predicted re-run adoption **0/3 at 85%**. Also: rendering is only half the defect — `limit=16384` still returns 6 of 10 files on a 10-file tree. |
| **(b)** a truly blind rung, N vs F | **Worth 3 screening runs, not 6** — my O3 | **3** | It closes the receipt's own caveat honestly, but the task stays grep-shaped: "find every `System/currentTimeMillis`" is exactly the question `rg` answers completely in one call, blind or not. Blindness fixes fairness, not the square. |
| **(c)** routing plate (the description says WHEN) vs bare exposure | **Fuse with (b); put the plate in the tool description, not the prompt** | **3 (tied, same cohort)** | Prompt mandates are a measured loser and the prompt is not ours in the field; the tool description is always on the table. Predicted **0/3 at 70%** on its own — it earns its 3 runs only because it is the last cheap prompt-side lever and a positive would be genuinely new. |

**Ranked order I would run:** **O2 (fix + bench, 0 arm-runs) → O1 (= candidate d, with an M arm)
→ O4 (build tonight, cohort after O2) → O3 (= b+c fused, 3-run screen).** And the apparatus ratchet
from §1.5 ships with whichever cohort runs first, because without an in-arm connection witness every
adoption null in this program is credible rather than proven.

## 11. Result poll on E6 — Sol (file /home/forge/tmp/sol/e6poll-sol.md, 22:49Z)

## Interpretation of the 0/3

This is primarily a **routing null**: no arm called the tool, so post-call rendering cannot explain the initial choice.  
The prompt/rung weakened the test by naming 8/10 owner namespaces; native `rg` was already a complete, cheap route.  
Header-only text is nevertheless a real **product/client-rendering defect** that would cripple value after a call.  
Thus 0/3 closes bare exposure on Lb—not square 3’s capability, a genuinely blind task, or explicit conditional routing.

## Next-wave experiments

### A. Rendered rows, same rung — rating: low causal value, high hygiene value

- **Pass line:** `content[0].text` contains at least one bounded namespace/form row and agrees with `structuredContent.tree`; then ≥2/3 F arms call `ls-tree`, with ≥1 call by return 3; all gates green.
- **Prediction:** **0/3 adoption, 85% confidence.** The rendering fix occurs after the choice being measured.
- **Cost:** 3 arm-runs; 2–3 agent-hours including linked intent, witnesses, build and scoring; 2–3 hours wall.
- **Tonight:** Attested successor built from `bridge/study-ops-mcp 4480e3d ∪ origin/main 08bfcb1` plus the rendering leaf, on **7910**; reuse the exact Lb F prompt and caller.
- **Could be waste because:** It cannot causally change the first call when the model has not yet seen any result content.

### B. E6-Blind: workspace discovery without owner leakage — rating: high diagnostic value

- **Pass line:** Prompt lint finds **0/10 owner namespaces and zero owner paths**; ≥2/3 F arms call `ls-tree`, with ≥1 by return 3; both arms are 3/3 exact under the unchanged oracle. Claim fewer reads only if `max(F pre-write files) < min(N pre-write files)`.
- **Prediction:** **1/3 F adoption, 45% confidence**; both arms 3/3 correct and read ranges still overlap. Probability of passing ≥2/3: about 20%.
- **Cost:** 6 arm-runs; 1–1.5 agent-hours; 1–1.5 hours wall.
- **Tonight:** N has no MCP; F uses the unchanged bare-exposure server `4480e3d ∪ 08bfcb1` on **7909**. Keeping the header-only response unchanged isolates the rung/prompt variable.
- **Could be waste because:** A manufactured blind rung may still measure benchmark construction rather than organic repository navigation.

### C. WHEN-routing plate versus bare exposure — rating: high routing value

- **Pass line:** On byte-identical Lb tasks, plated arm R has ≥2/3 early `ls-tree` adoptions, bare arm B has ≤1/3, and `adoption(R) − adoption(B) ≥ 2`; all R/B/N gates green.
- **Prediction:** **R 2/3 versus B 0/3, 55% confidence.**
- **Cost:** 9 arm-runs—3 R, 3 B, 3 native controls; about 2 agent-hours; about 2 hours wall.
- **Tonight:** Two fresh-session catalogs from otherwise identical `4480e3d ∪ 08bfcb1` servers: bare description on **7909**, plated description on **7910**. The plate says when to call first for workspace-wide owner discovery; result rendering remains identical.
- **Could be waste because:** A WHEN clause may be a soft mandate that measures obedience and recreates the previously measured layering tax.

### D. E6-Q: “who calls X, at which arities?” — rating: high square-3 product value, build-gated

- **Pass line:** Against an arm-independent sabotaged oracle containing real calls plus string, comment, quoted-data, local-shadow and reader-conditional decoys: F exact answer **3/3**, N exact answer **≤1/3**, hence paired exactness advantage ≥2; F uses exactly one read request and reports precision=recall=1.0 with every arity and source anchor correct.
- **Prediction:** **F 3/3 versus N 1/3 exact, 65% confidence.**
- **Cost:** 6 arm-runs plus G1; 3–5 agent-hours because the arity-bearing `called-by` projection is not yet a public inspected view; 3–4 hours wall.
- **Tonight:** N uses `rg` plus ordinary reads. F uses a read-only `called-by` successor stacked on `4480e3d ∪ 08bfcb1`, attested on **7910**; no writes or acceptance JVM are needed.
- **Could be waste because:** A mandated capability win would not reverse the 0/13 evidence that the capability remains unshipped without routing.
## 12. Next-wave verdict (Fable, 22:49Z)

| question | Opus | Sol | verdict |
|---|---|---|---|
| what the 0/3 means | routing/product; rendering cannot be the cause (never seen) | routing null; rendering a real post-call defect | agreed — both |
| re-run same rung after the rendering fix | waste, 0/3 at 85% | waste, 0/3 at 85% | NOT run; O2 fix built anyway (product) |
| square 3 on its own terms | O1: N / M(mandated) / F, existing server, 9 arm-runs | D: F vs N with a decoy-laden sabotaged oracle, needs a called-by build (3–5 h) | **E6-Q launched**: three arms incl. M, Sol's decoys, existing study ops only (no build gate), frozen truth, mechanical scoring |
| routing plate | O3 fused with a blind rung, 0/3 at 70% | C: plate vs bare, R 2/3 vs B 0/3 at 55% | **E6-C launched**: plate in the tool DESCRIPTION (Opus's placement), Sol's design, per-arm proof the plate was served |
| blind rung | 3-run screen | B: 1/3 at 45% | deferred to the next wave |
| route-don't-ask read hook (O4) | unique to Opus, "most valuable number" | — | build tomorrow (not a measurement tonight) |
| ratchet | required=true + per-arm connection proof | — | adopted in both cohorts: an arm without proof is VOID |

## 13. Second result: E3-P (22:55Z) — 0.36× wall direction inside the floor, 3/3 correct both, pass line FAILS on refusals (57%)

| arm | n | wall s | returns | non-test actions | verb calls (committed) | churn | correct |
|---|---|---|---|---|---|---|---|
| N native | 3 | 137.0 | 4.67 | 5.00 | 0 | +84/−84 | 3/3 |
| T tool | 3 | 49.3 | 4.00 | 4.67 | 2.33 (1.00) | +84/−84 | 3/3 |

Predictions scored: Fable "≤ 0.5x at N=21; tool 2–3 returns vs native 9–10" — wall direction right (0.36×) but inside the floor, returns wrong (native 4.67, not 9–10: Sol closes fan-out fast); Opus "wall 0.65–0.85×, all six lines pass 55%" — wall better than predicted, lines failed on refusals; Sol "60–90 s saved, inside the 172 s floor" — exactly right (87.7 s). Product defect inb-b873d0. Receipt docs/observations/2026-09-04-e3-p-cohort.md (2d32482).

## 14. Result poll on E3-P — Sol (recovered from the codex log; the output file was overwritten by a pointer, 22:59Z)

## Interpretation (4 lines)

1. **The 0.36× is a candidate signal, not yet a measured mechanism:** 87.7 s is below the 172 s floor, and T removed only 0.67 return / 0.33 non-test action, so model/driver variance can explain the wall gap.
2. **The fan-out mechanism itself is real at the deterministic meter:** one committing call found 21 owners / 63 sites, resolved 30 collisions, landed canonical bytes 3/3, and needed zero native fallback; a 700 ms warm-server execution cannot by itself explain 87.7 s saved.
3. **The 57% refusal is the refusal contract failing in front of a successful verb:** all four first callers made the same natural `scope.paths=["src"]` request, received the wrong domain diagnosis, and got no executable `next_call`; this is a repeatable interface tax, not stochastic closure failure.
4. It still makes the product fail: auto-normalizing a bare directory can recover clause 1, while a corrected `next_call` alone still costs two verb calls and therefore still fails clause 1; PF-4's quoted-symbol narrowing is a separate semantic blocker for the real-repo claim.

## Next-wave experiment cards

Ratings below are evidence-value ratings, not a selection; all four remain independent options.

### A. Scope-recovery replication — rating: **very high diagnostic value / low cost**

- **Machine-scorable pass line:** First bench two request-boundary cases: `scope.paths=["src"]` must normalize to `src/**` and reach the same plan as the explicit glob; a genuinely zero-match glob must refuse with a distinct spelling/scope reason and an executable `next_call` that succeeds unchanged. Then run three fresh T arms. All three must have exactly **one total `alias_migration` call, one commit, zero refusals, zero native `.clj` fallback, +84/−84 churn, and 6/6 FAN checks green**. Report the old frozen N mean against the new T mean; wall clears only if `137.0 − mean(T) > 172 s` (it cannot be rescued by the ratio alone).
- **Prediction:** clause 1 passes **3/3 (85% confidence)**; T mean **40 s** (central ratio **0.29×**, about **97 s saved**); probability the gap exceeds 172 s is only **5%**. Correctness remains 3/3 at **95%**. A fix that supplies only `next_call` predicts 0/3 clause-1 passes by definition.
- **Cost:** roughly **2–3 agent-hours** for the linked request/refusal fix, witnesses, and review; **3 arm-runs / 30–45 min wall**, one serialized acceptance JVM at a time.
- **Runs tonight on:** the q5z scope-fix successor composed with fetched `origin/main`, attested on **7907**, using the existing `bench/fanout` N=21 seed-7 fixture, exact E3-P T prompt, `sol-yolo`, and the repaired apparatus. Preserve the original three N receipts as the declared comparator; if the host/driver epoch cannot be matched, add three fresh N arms and label the cost six runs.
- **Could be waste because:** it is almost certain to repair a deterministic count while leaving the only disputed quantity—wall—inside the floor; it validates interface hygiene, not scaling.

### B. N=40 / N=80 slope replication — rating: **very high mechanism value / high syntheticity**

- **Machine-scorable pass line:** At each N, obtain three valid N and three valid T runs on nested seed-7 targets. Every T run must make one committing call, refuse 0 times, use zero native fallback, and both arms must pass all six FAN checks. Define `gap_N = mean(wall_N) − mean(wall_T)`. The slope passes only if **`gap_80 > 172 s`, `gap_80 > gap_40`, T/N ≤ 0.50 at both N, and T/N ≤ 0.35 at N=80**; also report pre-write source files opened and returns so the claimed cause is site discovery rather than patch bytes.
- **Prediction:** N=40 **175 s** versus T **55 s** (`gap_40=120 s`, 0.31×); N=80 **310 s** versus T **65 s** (`gap_80=245 s`, 0.21×). Probability the full pass line clears is **55% confidence**; probability only the N=80 172-second floor clears is **65%**. The alternative is Sol's earlier logarithmic-native case: `rg` plus a generated patch keeps N=80 below about 220 s and the level remains unresolved.
- **Cost:** **12 arm-runs**, about **1 agent-hour** of fixture/prompt validation and **3–5 h wall**; two server ports, with all load/test JVMs serialized. A lower-cost n=1-per-point screen is legitimate for slope direction but cannot promote a wall level.
- **Runs tonight on:** fixed q5z successor on **7907/7908**, `bench/fanout` generator/scorer at N=40 and N=80, identical caller and mirrored order, load ≤8, no concurrent timed cohorts.
- **Could be waste because:** the generator deliberately manufactures per-file alias irregularity absent from the largest real repositories; output truncation or the model deciding to write a generator can create a driver/context threshold that looks like a product slope.

### C. CFP store→event-store, 170-file anchor — rating: **highest external-validity value / semantic-fix gated**

- **Machine-scorable pass line:** Freeze the pinned CFP base, exact 170-file / 2,056-site manifest, and a sabotaged oracle before arms. For three mirrored N/T pairs, both arms must pass compile/tests and exact site/alias/protected-region checks. Each T must have **one committing call, A=0 native bytes after the verb, B=0 model returns from receipt to first compile/test, zero fallback**, and a ≤4 KB receipt. Crucially, every quoted fully-qualified datum must remain fully qualified to the new namespace and an outside-namespace `requiring-resolve` oracle must pass. Speed is a win only if `mean(N) − mean(T) > 172 s`.
- **Prediction:** the current verb scores **0/3 semantic passes (95% confidence)** because PF-4 already demonstrates narrowing. After a quoted-data-preservation fix, T correctness is **3/3 at 70% confidence**; central wall is **T 150 s versus N 255 s** (105 s saved, 0.59×), with only **20%** probability of clearing the floor.
- **Cost:** first a **20–30 min G1** correctness hand-drive; if red, **3–5 agent-hours** for the semantic fix and witnesses; then **6 arm-runs / 2–3 h wall** with the repository suite serialized.
- **Runs tonight on:** CFP pinned at the catalogue's store→event-store base in throwaway clones, never the working checkout; attested q5z semantic successor ∪ `origin/main` on **7907**, with N having no MCP endpoint. The cohort starts only after the G1 external-resolution check is green.
- **Could be waste because:** this anchor is alias-uniform—the real case where `rg`/`sed` is right—so even a correct 170-file one-call result may repeat the known direction while remaining below the wall floor.

### D. E3-L closability boundary control — rating: **high falsification value / medium incremental value**

- **Machine-scorable pass line:** Three mirrored N/T pairs on rung L. Every T run must use exactly **one successful `require_change`** for the ten `ns` forms, make no post-receipt reread of those forms, and use native patching only for the 21 host-interop sites. Require T mean non-test actions **≤10.5**, churn inside **+47..71 / −27..41**, and both arms green at **12/82/0**, **577/7784/0**, goldens byte-identical, with exactly one remaining `System/currentTimeMillis` line. The boundary is confirmed if `|mean(T)−mean(N)| ≤172 s`; layering is separately called if T actions exceed **16.6**.
- **Prediction:** operational pass **2/3 tool arms (65% confidence)** because `invalid-compact-relation` remains a live prior; conditional on zero refusals, T **245 s versus N 215 s** (1.14×, +30 s), both 3/3 correct, and the boundary stays inside the floor at **90% confidence**. This is what the law of decisions predicts: the verb closes ten mechanical require edits but cannot discover or decide the 21 host-interop replacements.
- **Cost:** **6 arm-runs**, about **1 agent-hour** setup and **1.5–2.5 h wall**, with one suite JVM at a time.
- **Runs tonight on:** `marvin-voice-remote @ ab267f9`, the frozen rung-L prompt/oracle, tool server with the composition-fixed `require_change` on **7908**, same `sol-yolo` caller and mirrored order as E3-P.
- **Could be waste because:** it deliberately exercises a boundary already predicted by n1 and the withdrawn single-edit square; if the live refusal fires, the cohort mostly re-measures an old request grammar defect rather than closability.


## 15. Result poll on E3-P — Opus (file /home/forge/tmp/sol/e3poll-opus.md, 23:01Z)

# Opus — poll on the E3-P result (0.36x wall, inside the floor, 57% refusals)

*forge@anvil, 2026-09-04, read-only in clj-surgeon @ 31886f8. Evidence: the six
`rollout.jsonl` under `/home/forge/tmp/arms/e3/e3-P-*/`, timestamps and payload
character counts computed from them, not from the report.*

## 0. A correction to the record, before any interpretation

**`docs/observations/2026-09-04-e3-p-cohort.md` says the refusal "carries no `remedy` and no
executable `next_call`." That is false.** All four `alias-migration-empty-scope` refusals carried
a complete, executable `next_call` in `structuredContent`, with the corrected globs, the right
`expect.files: 21`, the arm's own `workspace_root`, and the arm's own `refer_policy` echoed back
(`preserve-refer` for T-1/T-3, `alias-qualify` for T-2 — the tool got even that right):

```json
"next_call": {"op":"alias_migration",
  "from":{"lib":"acid.fanout.store","var":"find-event"},
  "to":{"lib":"acid.fanout.store2","var":"fetch-event",
        "alias_policy":["store2","st2","es","store-2"],"refer_policy":"preserve-refer"},
  "scope":{"paths":["src/**","test/**"]},"expect":{"files":21},
  "workspace_root":"/home/forge/tmp/arms/e3/e3-P-T-1/worktree"}
```

T-1 and T-3 said so out loud — *"returned a corrected executable call"*, *"Per its required
recovery path, I'm sending the returned executable `next_call` once"* — and executed it verbatim
on the next turn in **3.3 s**. So `docs/vision.md`'s constraint *"every refusal carries a
next_call the agent can execute unchanged"* **was satisfied**, 2 of 3, and the vision's own
recovery contract worked as designed. The real defect is narrower: the **`content.text` block**
renders only the domain sentence (`→ No namespace under scope requires acid.fanout.store`) and
drops both the remedy and the two fields that distinguish the causes (`found_files: 0`,
`scanned_files: 0` — the tool has the evidence and mis-narrates it). Only T-2 was fooled: it
ignored the `next_call`, ran a native `rg`, guessed an absolute path, was refused again, and only
then used the globs.

**This is the same defect class as E6-Lb's mechanism candidate inb-3e298e ("text content
header-only"), found the same night on a different square.** One ratchet — *the text block is a
lossy projection of `structuredContent` and must never omit a field that changes the next call* —
closes both. That is the strongest cross-experiment finding of the night and it is worth more
than either cohort's headline.

## 1. Interpretation — five lines

1. **0.36x is a real mechanism, and the mechanism is not site discovery — it is write-payload
   emission.** Native's discovery is *fast*: one or two `rg` calls, 8–13 s total, and all three
   native arms had the correct 21-file inventory inside 30 s. What costs 137 s is **typing the
   patch**: 8,649 / 8,160+8,161 / 9,531 characters of `apply_patch` body, emitted at a
   near-constant **136.7 / 140.8 / 136.8 chars per second** (CV 1.7% across four independent
   emissions). Per arm: **63.3 s, 115.9 s, 69.7 s of pure payload emission — mean 83.0 s.** The
   tool arms emitted 465–593-character JSON requests at 166–174 chars/s: **6.6 / 8.8 / 6.7 s,
   mean 7.4 s.** The difference is **75.6 s of the 87.7 s wall gap — 86% of it.** Everything else
   (discovery, `bin/fan-test`, the final message) is the same in both arms. This is not a warm-server
   artefact: server time was 618–666 ms, 1.3% of the tool arm's wall.
2. **So the fan-out law is `native payload = N x ~412 chars = ~3.01 s per owner; verb payload =
   ~550 chars, constant`.** That is a slope with a measured intercept, and it is measurable as a
   **deterministic character count**, which is immune to the 172 s wall floor. The cohort measured
   the right thing with the wrong instrument.
3. **But the slope's magnitude is set by irregularity, not by N.** Rung P has six distinct old
   aliases (`st db s store repo k`) and a four-way new-alias policy with 30 collisions — a shape
   `sed` cannot close, so native must emit per-site bytes. On a *regular* fan-out, native's payload
   collapses to one ~120-character `sed` command and its emission cost goes to ~1 s **at any N**.
   C1 already measured real repos as alias-uniform. **The product claim square 2 can actually
   defend is not "fan-out" — it is "fan-out a regex cannot close."** That boundary is measurable
   tonight and nobody has measured it.
4. **The 57% says the falsifier was written in the wrong unit.** Refusals are not fungible: T's
   4-in-7 refusals cost **3.3 s each (~10 s total)**; native's own patch rejection — N-2's `.cljc`
   extension miss — cost **58.0 s**, a full re-emission, and native's rejection rate was 1 in 4
   patch submissions (25%). **A 57% refusal rate at 3 s beats a 25% rate at 58 s**, and the
   pre-registered falsifier "refuses more than 20% of its calls" cannot see that. Refusal *price*
   (returns x re-emitted payload), not refusal *rate*, is the meter — and it is the same meter as
   line 1, which is why it is the honest one.
5. **Clause 1 fails on a two-line bug, and fixing it changes the pass line and nothing else.**
   Glob-normalising a bare `src` to `src/**` removes all four refusals and one model return per
   arm — worth ~3 s of 49. **It buys clause 1; it does not buy the square.**

## 2. Four next-wave experiments

Floors quoted and never crossed: wall 172 s at n=3, non-test actions 6.1, acceptance a gate not a
score (v1, receipt `3e26e1c`). Every pass line below leads with a **count**, not a wall statistic.

---

### O1 — **E-REG: the regularity sweep** (new; the crossover that decides the square)

| | |
|---|---|
| **question** | At fixed N=21, how irregular must a fan-out be before native's payload stops collapsing to one `sed`? And: is the pre-call gap *emission* or *thinking*? |
| **design** | Extend `bench/fanout/gen-fanout.clj` with `--k` = number of distinct old alias bindings across the 21 owners. Cells k=1 (uniform, sed-closable), k=2, k=3, k=6 (rung P as shipped). T and N arms per cell, n=2. Identical prompt, identical N, identical site count — **only regularity varies.** |
| **pass line (machine-scorable)** | Primary, deterministic, claimable at n=1: **emitted write-payload characters per arm** (sum of `custom_tool_call.input` length for every call whose body contains `Begin Patch`, a `sed`/`perl` rewrite, or an `alias_migration` request). Predicted native: k=1 <= 400 chars; k=6 >= 8,000. **The crossover k* is the smallest k where native payload exceeds 2,000 chars.** Secondary: the pre-call gap in seconds must track payload at 137 +/- 15 chars/s in **every** cell — if the k=1 native gap stays above 30 s at <400 chars, the mechanism is *thinking*, not emission, and line 1 of this poll is withdrawn. Gate: `rescore-FAN.sh <wt> 21` 6/6 in all cells, both arms. |
| **prediction** | k*=3, **55%** (k*=2, 25%; k*=4+, 20%). Native payload k=1 = **180 +/- 120 chars**, 70%. Emission-rate constant holds within +/-15 chars/s in all eight native cells: **65%**. At k=1, **T loses**: T/N wall 1.0–1.4x, **75%**. |
| **cost** | `--k` knob + canonical re-derivation ~1 agent-hour (Sol); 16 arm-runs ~50 min wall; one JVM under `flock`. |
| **runs tonight on** | Branch `bridge/fanout-fixtures-in-git` (local + origin), server `ac1c8409` as already attested. No merge, no new repo. |
| **one reason it is waste** | If k* turns out to be 2, the answer is "almost every real fan-out", which is too good to believe from a synthetic generator and will be attacked as fixture-shaped. Counter: it is the only experiment here that can *falsify my own mechanism claim* in the k=1 cell, and a mechanism nobody tried to kill is not a finding. |

---

### O2 — **E-SLOPE80: N=80, not N=40** (= the brief's (b), with the rung corrected)

| | |
|---|---|
| **question** | Does the gap grow past the floor, and does it grow at the predicted 3.01 s per owner? |
| **why 80 and not 40** | Arithmetic, decided before the run: predicted gap = 87.7 + 3.01 x (N-21). **At N=40 that is 145 s — inside the 172 s floor, so N=40 is a run that cannot produce a claimable wall number.** Break-even is N=49. **N=80 predicts 266 s, 1.55x the floor.** Targets nest (N=21 subset of N=80) by generator construction, so it is the same files growing. |
| **pass line** | Primary (floor-free): **native emitted write-payload chars = 33,000 +/- 4,000; tool = 550 +/- 150; ratio >= 40x** — deterministic, claimable at n=1. Secondary (wall, claimable **only** because it is predicted above the floor): T vs N gap **>= 172 s** at n=3 each. Tertiary: **native patch-rejection rate** — 1 of 4 at N=21; a re-emission at N=80 costs ~240 s, so this is where native's variance actually lives. Gate: `rescore-FAN.sh <wt> 80` 6/6 both arms. |
| **prediction** | char ratio >= 40x: **85%**. Wall gap >= 172 s: **75%**. Native >= 1 patch rejection in 3 arms: **65%**. Native splits into 2+ patch calls (context/output limits): **45%** — and if it does, **that is a finding, not a nuisance**: it is the first evidence of a hard ceiling on native fan-out. |
| **cost** | Generator already takes `--n`; scorer already takes N as an argument. ~25 min setup; 6 arm-runs, native ~300 s each => **~40 min wall**. |
| **runs tonight on** | Same branch, same attested server. Nothing merged. |
| **one reason it is waste** | N=80 with six distinct aliases is further from any real repo than N=21 was, and C1 says real fan-outs are uniform. A 40x char ratio on a fixture nobody would ever hand a human is a **mechanism** result that a product reviewer can dismiss in one line. It only pays if O1 has already located k* on the real side of the crossover — **so O1 outranks it, and if the box can only run one, run O1.** |

---

### O3 — **E-ANCHOR: the real-repo anchor, fix first** (= the brief's (c), re-ordered)

| | |
|---|---|
| **question** | Does the verb produce *correct bytes* on the cfp `store -> event-store` shape (170 files)? |
| **why it must not run as written** | PF-4 already measured the answer to the speed question and to the correctness question. **Correctness: a quoted fully-qualified symbol in data position — `'acid.fanout.store/find-event` — comes back alias-qualified as `'store2/fetch-event`, and `requiring-resolve` of it from outside the defining namespace now fails.** The cfp anchor is *made of* those shapes. Running T vs N today measures a known defect. **Speed: cfp is alias-uniform, so by O1's mechanism native closes it with one `sed` and T loses by construction.** The pre-registration already calls this "the case native should win." So: **build the fix, then run this as a binary correctness gate, and do not report a wall row at all.** |
| **pass line** | Binary, correctness only: **zero unmigrated or narrowed sites of the two failing shapes** (qualified symbol in binding-vector position; quoted fully-qualified symbol in data position), verified by `requiring-resolve` from a *different* namespace, not by grep; r1–r7 green; **A = 0** native bytes landing after the verb; **B = 0** model returns between the receipt and the first compile. **No wall claim is made or accepted.** |
| **prediction** | With the fix: T passes correctness **55%**. Without the fix: T **fails, 85%** (PF-4 measured it). T wall >= N on this anchor: **80%** — stated in advance so a loss is not news. |
| **cost** | Fix (preserve a quoted fully-qualified symbol as fully-qualified; it is a *data* position, not a call site) ~1.5 agent-hours to Sol; anchor scorer ~1 agent-hour; 2 arm-runs ~15 min. Repo present at `/home/forge/src/curtaincall-cfp`. |
| **runs tonight on** | The fix branch off q5z; anchor cloned to `/home/forge/tmp/arms/anchor`, the working checkout never touched. |
| **one reason it is waste** | The record already carries **two contradictory sl1-R readings** and this settles the contradiction rather than winning a square — and it costs the night's only substantive build. Counter: a program cannot ship on a contradiction, and this is the single defect on the board that would corrupt a real user's repo. **If it is not fixed, the verb should not be offered on real code, whatever the slope says.** |

---

### O4 — **E-SCOPE: fix the scope refusal, 3 T arms** (= the brief's (a), demoted to a chore)

| | |
|---|---|
| **question** | Does clause 1 pass once the bare-directory spelling is accepted? |
| **honest ranking** | This is **a pass-line repair, not an experiment.** Its expected wall saving is ~3.3 s of 49.3. Rate it last of the four and run it as a 20-minute chore *inside* O1's setup, not as a cohort of its own. |
| **the fix, three rungs** | (1) glob-normalise: a `scope.paths` entry with no glob metacharacter that names an existing directory is read as `<dir>/**`; (2) **type the two states apart** — `found_files: 0 && scanned_files: 0` is `alias-migration-scope-matched-nothing` (a spelling refusal), `scanned_files > 0 && found_files == 0` is `alias-migration-empty-scope` (the true domain refusal); (3) **the text block must render the remedy** — *"`scope.paths` are globs; `src` matched 0 files; did you mean `src/**`?"* — plus an example test at the request boundary asserting that a bare directory commits and that the two error types are never confused. Rung (3) is the one that also closes E6's inb-3e298e. |
| **pass line** | **Zero `alias-migration-empty-scope` refusals in 3 T arms; exactly 1 `alias_migration` call per arm, committed** (clause 1 PASS). Deterministic. Plus: the example test fails first, on the current server, with the observed bytes. |
| **prediction** | Clause 1 passes: **90%.** T wall 46 +/- 6 s. **And the wall gap still does NOT clear the floor**: 91 s against 172 s at n=3, and against ~122 s even if T is pooled to n=6 — **85% confident this experiment does not produce a claimable wall number**, which is precisely why it must not be sold as one. To clear the floor at this gap you need n ~ 14 per arm, or a bigger rung. That is O2's job. |
| **cost** | Fix + test ~40 min (Sol); 3 arm-runs ~10 min. |
| **runs tonight on** | q5z tip merged with `origin/main`, same as the cohort. |
| **one reason it is waste** | It repairs the experiment's scoreboard without moving the product, and a clean 6-of-6 pass line will read as a stronger result than it is. **If it is run, the report must state that the pass came from a two-line spelling fix and that the wall gap is still inside the floor.** |

---

## 3. Rating the brief's four, and the ride-along

| brief item | my rating | why |
|---|---|---|
| **(a)** fix scope, re-run 3 T | **4th — a chore, not an experiment** | Clause 1 passes at 90%; wall gap still inside the floor at n=6 (85%). Fold into O1's setup. |
| **(b)** scale to N=40/80 | **2nd, and N=40 is a dead cell** | Predicted N=40 gap 145 s < 172 s floor. Go straight to N=80 (266 s), and lead with emitted characters, not wall. |
| **(c)** real-repo anchor | **3rd, and must be re-ordered** | Fix the quoted-symbol narrowing first; then run it as a **correctness gate with no wall row**. Running it today measures a known defect. |
| **(d)** E3-L boundary control | **not a standalone experiment — it is O1's k=1 cell** | With the mechanism known, its result is now *derivable*: native's payload on a single known-site edit is ~100 chars, ~1 s of emission, so there is nothing for the verb to remove and it must lose by the round-trip. Predicted T/N = **1.05–1.35x, 80%.** Running it separately buys a confirmation of arithmetic. Its one real use — does the 137 chars/s constant hold at the low end — is free inside O1. |

**If the box runs only one thing: O1.** It is the only experiment that can falsify my own mechanism
claim, it locates the crossover that decides whether square 2 has a product behind it, and it
makes (b) and (d) fall out as cells rather than cohorts.

## 4. One line for Gene

The tool arm did not win by finding the sites faster — native found them in 30 seconds; it won
because native had to **type 8,600 characters of patch at 137 characters a second and the verb
typed 550**, which is 86% of the whole gap and grows about three seconds per additional file —
so the real question is not how many files, it is **how irregular they are**, because one `sed`
closes a tidy fan-out at any size.

## 16. E3 next-wave verdict (Fable, 23:01Z)

| question | Sol | Opus | verdict |
|---|---|---|---|
| what 0.36× is | candidate signal inside the floor; mechanism real at the meter but wall could be variance | a real mechanism: WRITE-PAYLOAD EMISSION — native types 8–9.5 KB at 137 chars/s (83 s), tool emits 550 chars (7 s) = 86% of the gap; server time 1.3% | Opus's claim is falsifiable with a deterministic meter (chars emitted) — test it |
| what sets the magnitude | N (site discovery is native's cost) → slope N=40/80 | alias IRREGULARITY k, not N — on a regular fan-out native uses one sed at any N; real repos are alias-uniform (C1) | the disagreement IS the experiment: E-REG (k = 1/2/3/6 at N=21) first, then N=80 |
| the 57% refusals | the refusal contract failing in front of a successful verb; bare dir must be accepted or clause 1 is unreachable | the record was WRONG: next_call existed in structuredContent, executed 2/3; the text block dropped it; refusal PRICE (3 s) beats native's own patch rejection (58 s) | correction filed; q5z fix retargeted to the text-rendering CLASS ratchet + bare dir |
| N=40 | part of the slope | a dead cell (gap 145 s < floor) | not run |
| real-repo anchor | gated on the quoted-symbol fix; 0.59× | must not run as written; correctness gate only, no wall row | after the fix, as a correctness gate |
| E3-L | 2/3 operational, 1.14× | = E-REG's k=1 cell, derivable | folded into E-REG |

**Launch order:** (1) `--k` knob on the fixture branch (Sonnet, building); (2) the q5z fix (bare dir + text rendering of refusals, building); (3) **E-REG** — N=21, k ∈ {1,2,3,6}, T+N, n=2, primary meter = emitted write-payload chars (floor-free), plus wall and correctness; Opus predicts crossover k*=3 and a tool LOSS at k=1 (T/N 1.0–1.4×) — the result that decides whether square 2 has a product; (4) **E-SLOPE80** — N=80 only, n=3 pairs, does the wall gap clear 172 s (Opus 75%, Sol 65%).

## 17. Third result: E6-C (23:22Z) — routing plate 0/3, bare 0/3; adoption 0/19 program-wide

| arm | run | wall s | returns | inspect calls | plate seen? | correct |
|---|---|---|---|---|---|---|
| B bare | 1/2/3 | 123 / 114 / 162 | 4 / 4 / 5 | 0 / 0 / 0 | no (f1a094b9) | green ×3 |
| R plated | 1/2/3 | 112 / 114 / 108 | 4 / 5 / 4 | 0 / 0 / 0 | YES (ba1c0949) ×3 | green ×3 |
| N (reused) | 1/2/3 | 157 / 116 / 119 | 3 / 5 / 4 | 0 | n/a | green ×3 |

Predictions scored: Opus "0/3 at 70%" RIGHT (and the mechanism: "a name and presence are not a path, and a description sentence is not one either"); Sol "R 2/3 vs B 0/3 at 55%" WRONG on R. Ratchet delivered: required=true + per-arm tools/list proof (E6-Lb's F arms lacked it). Caveat: rung Lb is not blind. Receipt docs/observations/2026-09-04-e6c-routing-plate-cohort.md (7e34015).

**Fable's call, without a fourth poll on the same square (deviation from poll-each-result, reason: the fleet already rated every remaining routing lever tonight — blind rung Sol 1/3 at 45% / Opus 0/3 at 70%; route-don't-ask hook O4 is a build, not a measurement):** no further free-choice routing cohort tonight. Square 3 is decided by E6-Q (capability with a mandated arm, running); square 2 by E-REG + E-SLOPE80 (queued behind the q5z fix). Tomorrow's first build is O4 — route without asking (a read-side hook that serves clj-scoped rg/grep through the Surgeon read path, Surgeon never named), the only routing lever with a predicted non-zero number (100% routed at 85%).

## 18. Fourth result: E6-Q (23:28Z) — square 3 on its own terms: a CEILING (9/9 at 6/6)

| arm | run | wall s | returns | tool calls | inspect calls | probes exact | load |
|---|---|---|---|---|---|---|---|
| N | 1/2/3 | 32 / 41 / 41 | 3 / 4 / 3 | 2 / 3 / 3 | 0 | 6/6 ×3 | 6.1 / 7.3 / 6.0 |
| M | 1/2/3 | 148 / 144† / 151 | 5 / 5 / 4 | 13 / 13 / 10 | 10 / 11 / 8 | 6/6 ×3 | 4.1 / 10.2 / 5.9 |
| F | 1/2/3 | 60 / 67† / 47† | 4 / 4 / 3 | 4 / 5 / 2 | 0 / 2 / 0 | 6/6 ×3 | 5.2 / 8.9 / 9.0 |

† wall void (load > 8). Predictions: Opus N 4/6 WRONG (6/6), M 6/6 right, M−N clears WRONG, F 0/3 at 80% WRONG (1/3); Sol F 3/3 exact right, N ≤ 1/3 WRONG. "Both seats modelled the native arm as grep-shaped; both were wrong the same way." Opus's own waste clause is now the finding: "if native scores 6/6, square 3 is empty" — on FIVE SMALL FILES. First non-zero free-choice adoption: 1/16. Apparatus ratchet proven: required=true + dead-port negative control (codex refuses to create the session).

**Fable's call:** run the runner's recommended decisive variant NOW — E6-Q2, the same six probe shapes against reducer/core.clj (3,720 lines) and channel.clj (3,694 lines), N vs M vs F, correctness primary; if native is still 6/6, withdraw square 3 on the evidence. Runs on 7909 concurrently with E-REG (both primaries are load-insensitive; walls void above 8 and not claimed).

## 19. Fifth result: E6-Q2 (00:1xZ, 2026-09-04) — square 3 on the big files: the pre-registered decision fires, SQUARE 3 WITHDRAWN

| arm | run | wall s | tool calls | inspect calls | refused | exact | load |
|---|---|---|---|---|---|---|---|
| N | 1/2/3 | 64† / 62† / 64† | 3 / 3 / 3 | 0 | — | 6/6 ×3 | 9.3 / 14.4 / 11.3 |
| M | 1/2/3 | 158† / 160† / 215† | 10 / 13 / 18 | 8 / 11 / 15 | 3 / 5 / 8 | 6/6 ×3 | 9.4 / 9.0 / 9.1 |
| F | 1/2/3 | 101† / 78 / 143† | 6 / 5 / 8 | 3 / 1 / 4 | 2 / 1 / 3 | 6/6 ×3 | 11.4 / 6.9 / 18.2 |

† wall void (load > 8); 8 of 9 void, no wall claim. 54/54 probes correct. Primary FAIL on clause 2 (M−N = [0,0,0]). Pre-registration (sha 08509014c48d…, before the first arm): N 6/6 in ≥ 2/3 → withdraw. N 6/6 in 3/3.

**The learning that closes the square (runner, verbatim):** "The scale hypothesis was wrong about the *strategy*, not the size. Native never reads the big file … ten numbered windows on exactly the ranges the search named, both files in one call … ~300 of 7,516 lines read. File size never enters native's cost function." §18's Fable call predicted the big-file variant would be decisive; it was — in the direction of withdrawal. Fable's implicit model (native must read more of a big file) was wrong for the same reason Opus's "N 4/6" was wrong in §18: both modelled native as a file reader; it is a windowed reader over search hits.

Secondaries worth carrying: (1) F adoption 3/3 — the first cohort where free choice reached for the tool, and it broke at exactly the variable this cohort changed (file size), for READS only (outline, match), never for a change; F-2's only call was a refused prepare-change and it still scored 6/6, so adoption ≠ dependence. (2) 22/42 inspect calls refused (52%) at zero correctness cost: invalid-mcp-request ×12 (missing `expect`, wrong nesting — the runner hit both by hand), semantic-provider-unavailable ×5 (every prepare-change), no-clojure-files ×4, invalid-change-intent ×2. The refusal PRICE is paid in actions (M's 3.3–6×) while correctness is unmoved — square 3's cost is the mandate, not the tool.

**Standing squares after five results:** 1 gate (E3-P pass line still failing on first-call refusals; O2 r2 / q5z r11 building), 2 fan-out (E-REG running; E-SLOPE80 after), 4 proof before write (unmeasured). Withdrawn: single edit at a known site; square 3 (this caller). **Fleet poll deferred to E-REG's landing** — the next-wave question ("where, if anywhere, does the tool beat a windowed reader over rg hits?") is the same for both results and one poll on the pair costs half.

## 20. Sixth result: E-REG (00:30Z, 2026-09-04) — square 2 on a load-immune meter: the tool wins every cell; the irregularity story is refuted

| k | N chars (2 runs) | N chars/s | N emission gap s | T chars (2 runs) | T chars/s | T emission gap s | T/N wall (void) | correct |
|---|---|---|---|---|---|---|---|---|
| 1 | 8,594 / 1,929 | 150.5 | 58.5 / 12.5 | 1,013 / 485 | 171.5 | 5.7 / 2.9 | 0.31× | 6/6 all |
| 2 | 9,724 / 7,377 | 140.8 | 70.1 / 51.6 | 1,019 / 1,260 | 177.3 | 5.6 / 7.3 | 0.38× | 6/6 all |
| 3 | 8,202 / 8,179 | 141.6 | 57.9 / 57.8 | 1,067 / 912 | 185.6 | 5.9 / 4.8 | 0.38× | 6/6 all |
| 6 | 16,531 / 9,636 | 139.6 | 116.6 / 70.1 | 1,075 / 580 | 174.7 | 6.0 / 3.4 | 0.24× | 6/6 all |

Wall void in 11/16 (E6-Q2 alongside; load to 18.1). Headline rests on emitted characters, which load cannot move.

**What it says.** (a) The write-payload EMISSION mechanism from §16 is confirmed: native pays 137–154 chars/s to type the patch, every cell inside the pre-registered band. (b) The IRREGULARITY driver is refuted: k6/k1 = 2.49× (needed ≥ 3×), per-patch payload flat across k, and **no native arm reached for sed or perl even at k=1** — the uniform case Opus said one sed closes. Native types the patch by hand regardless of how regular the change is. (c) Crossover k* ≤ 1: the tool wins at the smallest k tested, so the sweep cannot find where it stops winning. Pre-registered withdrawal did not trigger (k=1 native emitted 8,594 and 1,929, not < 400), so the recorded verdict is INCONCLUSIVE on the mechanism claim and the honest reading is: emission confirmed, irregularity refuted.

**Why this matters for the claim.** "Fan-out a regex cannot close" was the defensible square-2 claim from §16. E-REG says the claim is both weaker and stronger: weaker, because regularity was never what native was paying for; stronger, because native does not use the regex even when it could — so the tool's win is against native's actual behaviour (hand-typed patches at ~145 chars/s), not against an idealised sed. The cost the tool removes is typing, and it scales with patch size, not with irregularity. The remaining avoidable tool cost is the first-call refusal: 6/8 T arms paid one (invalid-mcp-request flat argument shape; unknown-verification-profile) — a schema/description fix with a measurable payoff (one refused call ≈ 5–8 s of the tool's 32–43 s wall).

**Apparatus lessons (runner):** three distinct spellings of the same tool call across 16 arms; a syntax-keyed scorer reported real T arms as writes=0 — now keyed on emitted request content ("a meter that recognises a call by its syntax measures the model's phrasing, not its work"). One completed arm destroyed by a runner whose first act was rm -rf; ratchet applied (refuses to delete a dir holding rollout.jsonl without FORCE=1). Driver fault WATCH-ABORT rollout-rotated voided one arm twice; re-run clean.

**Standing squares:** 1 gate (E3-P pass line; O2 r2 / q5z r11 in review), **2 fan-out — first WIN on record, load-immune**, 4 proof before write (unmeasured). Withdrawn: single edit at a known site; square 3 (E6-Q, E6-Q2). Fleet poll on the E6-Q2 + E-REG pair launched (Sol + Opus): what drives native's payload if not irregularity, is E-SLOPE80 still worth its wall, and is the first-call refusal a schema or a description fix.

## 21. Fleet poll on the E6-Q2 + E-REG pair — Opus, verbatim (00:37Z)

# Opus — poll on the E6-Q2 + E-REG pair (square 3 withdrawn; square 2's first load-immune win)

*forge@anvil, 2026-09-04. Read-only: `2026-09-04-e6q2-bigfile-cohort.md`,
`2026-09-04-ereg-irregularity-cohort.md`, `2026-09-03-brainfleet-hills.md` §16–§20 (plus §15,
which is my own E3-P poll and is the claim under attack here). No code run, nothing edited.*

**Standing disclosure before anything else:** E-REG was built to kill my mechanism claim and it
killed half of it. P1, P2 and P3 all MISSED. The irregularity story was mine, it was wrong, and
the honest reading is the runner's, not a rescue of mine. Everything below is written with that
in front of me.

---

## 1. If irregularity is not the driver of native's payload, what is?

**It is the transcription volume of the patch artefact itself — line count × line width — which
is set by the harness's literal-context patch format, not by any property of the change.** The
agent is not paying for the *semantics* of the rename; it is paying to retype the file's
neighbourhood around each site because `apply_patch` demands literal context.

The arithmetic is already in E-REG and nobody has written it down. Canonical churn is
**+84/−84 at every k** — same N, same files, same sites, same edit size, only the alias *names*
vary. That is 168 diff lines over 21 files = 8 changed lines per file; add a path header and the
~3 context lines per hunk that `apply_patch` requires and you get ~15 emitted lines per file,
~315 lines per patch. Measured mean per patch call: **8,550 / 8,190 / 8,951 chars at k = 2 / 3 /
6 → ~27 chars per emitted line, and ~410 chars per owner.** The per-owner constant is flat
across the sweep precisely because the *line count* is flat across the sweep. k moves the
characters inside the changed lines; it cannot move the number of lines, and the number of lines
is the bill.

**Falsifiable statement (F1).** *Native's emitted write payload is linear in the transcribed
patch's line count at 27 ± 5 chars/line, with a fixed per-file overhead of ~1 header + 6 context
lines, and is invariant to semantic irregularity. Concretely: at fixed N = 21 owners and fixed k,
doubling the changed lines per file (two migration sites per owner instead of one) moves native's
payload from ~8,500 to **15,000–18,000 chars**, while the tool's stays at 900 ± 400.*

**Cheapest experiment that refutes it — E-HUNK, 4 arm-runs.** Same generator, same 21 owners,
k = 2 fixed. Two cells: H1 = 1 site per owner (the shipped shape, +84/−84), H2 = 2 sites per
owner (+168/−168, *identical* file count, identical alias set, identical discovery problem).
**Native arms only, n = 2 per cell** — the claim is about native's payload, so tool arms buy
nothing and the E-REG T cells are already the comparator. Primary meter: emitted write-payload
chars (load-immune). **Refutation:** if H2's mean payload is inside ±20% of H1's, line count is
not the driver and the cost is per-file overhead or discovery, and F1 dies. ~15 min wall.

**Named rider hypothesis (F2), because it is the one that actually endangers the program.**
The reason native never reached for `sed` — including at k = 1, where one would close it — may
not be capability. `ereg-k1-N-2` *did* compress, programmatically, once in two runs. **The
default patch affordance is a prompt-level fact, and it was never varied.** F2: *native's literal
transcription at k = 1 is an artefact of `apply_patch` being the offered write path; explicitly
licensing a scripted rewrite collapses it.* This is experiment #1 in §5, and it matters more than
F1 because it attacks tonight's headline rather than a mechanism footnote.

---

## 2. Is "fan-out a regex cannot close" still the right claim for square 2?

**No. Retire it.** It was my sentence, it was a claim about *irregularity*, and E-REG refuted the
irregularity driver (k6/k1 = 2.49×, needed ≥3×; per-patch payload flat at 5,262/8,550/8,190/8,951;
zero `sed` in 8 of 8 native arms *including k = 1*). Continuing to say "a regex cannot close it"
now asserts the opposite of what we measured — the regex *could* close k = 1 and native still
didn't use it. Keeping the sentence would be re-narrating a refuted mechanism as a survivor.

**The restatement a sceptic cannot dodge:**

> On a 21-owner alias migration, **8 of 8 unprompted native runs hand-transcribed a full patch —
> 7,377 to 16,531 emitted characters at a measured 137–154 chars/s — including the perfectly
> uniform k = 1 case that a single `sed` would close, while the verb committed a byte-identical
> result in one request of 485–1,260 characters; both arms passed the same 6/6 gate and were
> `diff -r` clean against the same frozen canonical.**

It is dodge-proof because every noun in it is a count from a frozen receipt: characters, not wall
(11 of 16 walls void); byte-identity against a canonical, not a judge; 8 of 8, not a mean. The
claim is now about **how agents actually behave**, not about what a script could theoretically do
— which is weaker as a theorem and stronger as a product claim, since a product is sold against
observed behaviour.

**What it still lacks, in the order a reviewer will attack it:**

1. **The counterfactual.** No native arm was ever *offered* a scripted rewrite. Until E-AFFORD
   runs, "native does not use the regex even when it could" means "even when nobody mentioned
   it," and that is one prompt line away from collapsing. This is the load-bearing gap.
2. **A second caller.** Every character in the claim is `gpt-5.6-sol` at high reasoning effort.
   A behavioural law measured on one model is a model fact, not an agent fact.
3. **A second harness.** `apply_patch` is the affordance under test; a native arm with
   Bash + Edit may price the same job differently.
4. **A real repo.** Fixtures are generated, and C1 says real fan-outs are alias-uniform — i.e.
   **the real-world cell is k = 1, the exact cell most exposed to attacks 1 and 3.**
5. **Any N but 21.** The law is linear in N by construction; nobody has looked for where it breaks.

---

## 3. E-SLOPE80 — for, against, verdict

**For.** (a) The meter changed under it in the right direction: emitted chars are load-immune, so
N = 80 is claimable at n = 1–2 without a single valid wall, which is the opposite of the problem
that gated it. (b) It is the only place the emission law can *break*: ~33,000 chars is at or past
the caller's comfortable single-output size, so N = 80 is where native most plausibly splits its
patch or spontaneously scripts — and a spontaneous script at scale is a **product boundary**, not
a nuisance. (c) Setup is nearly free: the generator already takes `--n`, the scorer already takes
N, the canonical derivation is proven at four k values.

**Against.** (a) Its entire original justification was wall — "does the gap clear the 172 s floor"
— and that justification is dead: wall is void under load and the box is shared. (b) The char
prediction is **arithmetic, not an experiment**: 80 × 410 = 32,800, and I already predicted
33,000 ± 4,000 in O2 before any of this ran. Measuring a number you can compute is the definition
of waste. (c) It is the most expensive cell on the board — native arms at N = 80 run ~300 s. (d)
k = 6 at N = 80 is further from any real repo than N = 21 was; a reviewer dismisses it in one
line, exactly as I wrote in O2's own waste clause. (e) The fleet's evidence says the exposed
flank is the affordance question, and N = 80 does not touch it.

**COMMIT: RE-DESIGN.** Do not run it as written. The wall primary, the n = 3 pairs, and the k = 6
shape are all obsolete; the strategy-switch question is the only non-derivable thing left in it.

**The re-design — E-CEILING80.** N = 80, **k = 1** (the real-repo shape, per C1), n = 3 native
+ 2 tool = **5 arm-runs**, no wall claim anywhere in the document. Primary meter (load-immune):
emitted write-payload chars, **plus patch-call count, plus a three-way strategy classifier**
(literal-patch / programmatic-generation / stream-edit) keyed on emitted *request content* — never
on call syntax, per E-REG's own apparatus lesson that three spellings of one call appeared across
sixteen arms. Secondary: native patch-rejection/re-emission count. The experiment is no longer
"how big is the slope"; it is **"does native change strategy when the patch stops fitting."**

---

## 4. The first-call refusal (6 of 8 T arms): schema, description, or routing?

**It is a SCHEMA fix, and the evidence that decides it is that the model already had the
description.** MCP ships the JSON schema in `tools/list`; the model read it and still emitted a
flat `old_lib/old_var/new_lib/new_var/alias_policy/expected_files` shape in 4 of 8 E-REG arms —
and the same class fired 12 times in E6-Q2 (`file` at the request root instead of inside
`requests`; missing `expect`), *and the coordinator hit both by hand on his first two calls
before any arm ran*. Three independent callers, one human and two model contexts, all guessed
**flat when the tool demands nested groups**. When the documentation is present and the prior
still wins three-quarters of the time, the prior is the specification. **Meet it.**

| # | fix | what it is | predicted effect on tool wall | predicted effect on the *claimable* meter | cost | payoff/cost |
|---|---|---|---|---|---|---|
| 1 | **Schema: accept-and-normalise** | Accept the flat alias-migration shape and map it to nested; hoist a root-level `file` into a one-element `requests`; treat an unknown `verification_profile` as the default with a `warning` field instead of a hard refusal. Return `ok: true` with `normalized_from: "flat-v0"` in **both** structuredContent and the text block | mean T wall **34 s → 27–29 s (−15 to −20%)**; T returns 3–5 → 3 modal; refused first calls **6/8 → ≤1/8** (75%) | **~0.** The refusal costs ~120 chars of re-emission out of ~926 | small: a coercion layer + boundary tests | **highest** |
| 1b | **Capability advertisement** | Do not advertise a verb whose provider is down (`prepare-change` → `semantic-provider-unavailable` ×5 in E6-Q2 = *every* call, on a task with nothing to change) | removes 5 refusals and ~6 wasted actions per E6-Q2-shaped cohort | ~0 | small | high |
| 2 | **Description: worked nested example** | A full example request in the tool description | −0 to −3 s; refusals −25% relative (40%) | ~0 | ~20 min | low — do it *inside* fix 1, never instead of it |
| 3 | **Routing** | Any exposure/plate intervention | **0 s** | 0 | — | **last** |

Routing is last on measured evidence, not taste: adoption is **0/19 on routing interventions**
(§17, plate 0/3 and bare 0/3), and this is not a routing failure at all — the agent already chose
the verb; it mis-typed the arguments.

**The uncomfortable corollary, stated plainly so nobody sells the fix as a result:** the refusal
is a **wall cost, and wall is void**. On emitted characters — the only meter this program can
currently claim — fixing it buys roughly nothing. It is a **build worth doing for the product**
(it removes a tax every real user pays on their first call), and it is **not an experiment**. It
must not be reported as a cohort win, for the same reason O4 was demoted to a chore in §15.

---

## 5. The next three experiments, ranked

Floors respected and never crossed: wall 172 s at n = 3 (so no wall is primary anywhere below),
non-test actions 6.1, acceptance is a gate and never a score. Every primary below is a count.
**Total cost: 20 arm-runs.** All three are vs-native functional measurements, so the launch mix
stays at 100% functional.

### #1 — E-AFFORD: the `sed` counterfactual (falsifies tonight's headline before Gene quotes it)

| | |
|---|---|
| **hypothesis** | Native's literal transcription at k = 1 is an artefact of the offered write path, not of capability. Explicitly licensing a scripted/stream-edit rewrite collapses native's payload in the uniform cell and erases the tool's character win there — the cell C1 says real repos live in. `ereg-k1-N-2` (1,929 chars, generated programmatically, once in two runs) is the existence proof that the capability is present and merely not default. |
| **primary meter (load-immune)** | Emitted write-payload characters per arm, plus a 3-way strategy classifier (literal-patch / programmatic-generation / stream-edit) keyed on emitted **request content**, never on call syntax. |
| **n** | 3 cells × n = 3 native arms: {k=1 default}, {k=1 sed-licensed}, {k=6 sed-licensed}. Zero new tool arms — E-REG's T cells (749 chars at k=1, 828 at k=6) are the comparator. |
| **predicted numbers** | k=1 default replicates at **5,000 ± 3,000 chars**, ≥2 of 3 literal-patch (80%). k=1 sed-licensed: **median 1,100 chars, ≥2 of 3 arms under 2,000** (70%). **T/N char ratio at k=1 falls from 7.0× to 1.0–1.8×** (65%). k=6 sed-licensed: **3,500 ± 2,000**, ratio still ≥3× (60%) — a stream editor cannot close 6 aliases with 30 collisions in one command. |
| **cost** | **9 arm-runs**, ~35 min wall; no build (one prompt §5 variant), reuses the E-REG fixtures and payload scorer unchanged. |
| **withdrawal condition (written first)** | If sed-licensed native at k=1 emits **≥4,000 chars in ≥2 of 3 arms**, F2 is dead, the affordance flank is closed, and "native does not reach for the regex even when it could" stands as a behavioural law for this caller — stop attacking it. Conversely, if the k=1 ratio falls **below 1.5×**, square 2's headline is rewritten to *"irregular or large fan-out"* **before** any report quotes the uniform cell, and E-REG's k=1 row is annotated in place. |

### #2 — E-EXTRACT: square 4, the verbs with no native equivalent

| | |
|---|---|
| **hypothesis** | E6-Q2's own closing question. On `:extract!` (move a var family from a 3,700-line namespace into a new one and repair every caller, require, and declare), native must emit the moved code **twice** — deleted from source, added to target — so its payload is ~2× the moved bytes while the tool's stays constant; and this is the first task since sl1 where native's **correctness** is at genuine risk (cyclic requires, `declare` repair, `refer` chains) rather than at a ceiling. |
| **primary meter (load-immune)** | (i) **Binary correctness gate**: the namespace loads and the worktree is `diff -r` byte-identical to a frozen canonical (the E-REG gate shape, which is deterministic and order-free); (ii) emitted write-payload chars. No wall claim. |
| **n** | 3 native + 3 tool = 6 arms, on `reducer/core.cljc` and `channel.clj` — the same two big files E6-Q2 already planted and proved loadable, so the fixture cost is mostly paid. |
| **predicted numbers** | 250-line extraction → native **15,000 ± 6,000 chars**; tool **500–900**; ratio **≥18× (75%)**. The number that decides the square: **native correct ≤2 of 3 (60%)**, tool **3 of 3 (65%)**. |
| **cost** | **6 arm-runs** + ~1.5 agent-hours to freeze the canonical and the loader gate. |
| **withdrawal condition (written first)** | If native scores **3/3 correct AND emits <5,000 chars** in ≥2 of 3 arms, **square 4 is withdrawn on the evidence for this caller**, on exactly the terms square 3 was withdrawn tonight — written down now so a second null cannot be re-narrated afterwards as "the extraction was too easy." |

### #3 — E-CEILING80: where the emission law breaks (the re-designed E-SLOPE80)

| | |
|---|---|
| **hypothesis** | The ~410-chars-per-owner law is linear only while the patch fits one comfortable output. At N = 80 (~33,000 chars) native either splits into multiple patch calls or spontaneously scripts. Either answer bounds the tool's win region, which is a product fact nobody has. |
| **primary meter (load-immune)** | Emitted write-payload chars **+ patch-call count + strategy classifier** (same content-keyed classifier as #1). Secondary: native patch-rejection/re-emission count. |
| **n** | 3 native + 2 tool at N = 80, **k = 1**. |
| **predicted numbers** | Native **≥25,000 chars in ≥2 of 3** (60%); native **splits into ≥2 patch calls in ≥2 of 3** (60%); tool **600–1,500 chars** (85%); ratio **≥20×** (65%); ≥1 native re-emission in 3 arms (55%). |
| **cost** | **5 arm-runs**, ~30 min wall + ~25 min setup (`--n` exists, scorer takes N). |
| **withdrawal condition (written first)** | If **≥2 of 3 native arms emit <6,000 chars at N = 80** (spontaneous scripting at scale), the emission law is **bounded to N ≲ 40**, square 2's product claim is restricted to mid-size fan-out in the same document that reports it, and no N = 80 win is claimed. |

**Explicitly not ranked, and why:** the schema fix (§4) is a build, not an experiment — on the
claimable meter it buys ~0. Any further routing or free-choice-exposure cohort: 0/19 on record;
route without asking, or ride the gate. E-ANCHOR still gates on the quoted-symbol fix and remains
a correctness gate with no wall row, unchanged from §15.

---

## 6. One sentence for Gene

Tonight proved the fan-out win is real and **load-proof** — on the same 21-file rename the plain
agent hand-types 7,377 to 16,531 characters at about 145 a second while the verb sends 485 to
1,260, and it wins every cell including the perfectly tidy one — and it proved the **read** side
is not ours, because on the two biggest files in the repo the plain agent answered **54 of 54**
structural questions in **three tool calls** by windowing over its own search hits, so square 3
is withdrawn on its own pre-registered terms; what tonight did **not** prove is that the fan-out
win survives an agent that is simply *told it may use `sed`* — nobody ever offered it one — and
that is the one experiment I would run before either of us quotes the number.

## 22. Fleet poll on the E6-Q2 + E-REG pair — Sol, verbatim (00:38Z; Sol wrote to its own path /home/forge/tmp/sol/e6q2-ereg-poll.md and left a pointer at the given one — second time tonight, the waiter rule holds)

# Fleet poll: E6-Q2 + E-REG

1. **Native payload is driven by literal changed surface under its default editing policy, not by alias irregularity.** For this caller, the default policy is to emit a full `apply_patch`; conditional on that choice, write payload should be approximately linear in owner count at roughly 350–450 emitted characters per owner, while the occasional compact program changes the intercept/regime entirely. This is falsifiable: hold `k=1` and bytes changed per owner fixed, vary only `N`, and measure emitted write characters. The cheapest useful refutation is native-only `N={5,21}`, `n=2` per cell (4 arm-runs). The claim loses if the N=21 mean is less than 3× the N=5 mean (4.2× predicted), or if compact generation appears in a majority of arms; either result says owner count under the default policy is not the driver.

2. **No: “fan-out a regex cannot close” is no longer the defensible claim.** The claim is: *for this caller on a 21-owner alias migration, clj-surgeon produced byte-identical correct output while emitting 7–16× fewer write-payload characters than autonomous native editing in every tested regularity cell, including the fully regex-closeable cell; the product advantage is compressing a many-owner edit into one intent.* A sceptic can propose a better native strategy, but cannot erase the measured strategy. What is still missing is replication across `N`, repositories, callers, and operation families; a direct comparison after making a compact native rewrite salient; and a correctness win on a genuinely semantic fan-out rather than merely an emission win on alias migration.

3. **For running E-SLOPE80 now:** N=80 is the cleanest test of the new owner-count/patch-surface explanation, may expose a strategy switch to generated editing, and should amplify the character gap enough to make the mechanism unmistakable. **Against:** its registered wall primary is unusable on the loaded box, E-REG has already established the N=21 emission win, and a wall rerun would spend six arms to generate another potentially void headline. **Commit: re-design, then run now.** Keep N=80 and three N/T pairs, use the fully regular `k=1` fixture so native has every opportunity to compress, make total emitted write-payload characters primary, record edit representation as a pre-registered mediator, and make wall descriptive unless every included pair meets the load rule. Prediction: native median 30,000 characters, tool median at most 1,200, T/N at most 0.05, with native still choosing literal patches in at least 2/3 arms. This preserves the six-arm cost and turns load from a fatal defect into a secondary nuisance.

4. **This is first a description fix, second a compatibility-schema fix, and not a routing fix.** The model already routes to the right operation; it guesses the envelope and profile vocabulary wrong.

   1. **Description:** put one copyable nested request and the allowed verification-profile values in the tool description. Predicted result: refusals fall from 6/8 to 1–2/8; affected arms save 5–8 seconds and mean tool wall falls about 4–6 seconds. This has the best payoff/cost because it is tiny, fixes both observed error classes, and does not widen the contract.
   2. **Schema compatibility:** normalize the invented flat alias fields into the canonical nested request, with explicit conflict rejection. This should eliminate the 4/8 flat-shape refusals but leave the 2/8 unknown-profile refusals: mean wall improves roughly 2.5–4 seconds. It is more deterministic but costs code, tests, permanent surface area, and ambiguity rules. Do it only if the description canary fails.
   3. **Routing:** predicted refusal rate remains 6/8 and wall improves 0 seconds. The correct verb is already being reached, so routing has no mechanism of action here.

5. **Next three experiments, in order:**

   1. **E-FIRSTCALL-DESC.** Hypothesis: a copyable nested example plus enumerated profile values removes the first-guess tax without schema expansion. Primary meter: first call accepted, a load-immune binary. `n=8` new T arms across the existing E-REG fixture cells; predicted result 7/8 accepted first try (at most 1/8 refused); cost 8 arm-runs. **Withdrawal condition:** before arm 1, declare description-only remediation withdrawn if at least 4/8 first calls are refused or if more than one new refusal class appears.

   2. **E-PREWRITE.** Hypothesis: on a change with a planted failing postcondition, the transactional tool can validate the prospective state and refuse while the real worktree remains byte-pristine, whereas autonomous native editing writes before it can obtain equivalent proof. Primary meter: proof/refusal precedes the first worktree mutation, witnessed by transcript plus filesystem hash; load-immune. `n=3` N/T pairs; predicted result T 3/3 proof-before-write, N 0/3; cost 6 arm-runs. **Withdrawal condition:** withdraw square 4 if T fails to prove-before-write in any arm, or if N achieves proof-before-write in at least 2/3 arms.

   3. **E-SLOPE80-C.** Hypothesis: absent a strategy switch, native emission scales with owner count while one intent keeps tool emission nearly constant. Primary meter: total emitted write-payload characters; `n=3` N/T pairs at N=80, k=1; predicted medians N 30,000 and T 1,000 characters, with T at least 20× smaller in all three pairs; cost 6 arm-runs. **Withdrawal condition:** withdraw the linear default-policy mechanism if native uses a compact generated rewrite in at least 2/3 arms, and withdraw the N=80 emission-win claim if T is not at least 5× smaller in at least 2/3 pairs.

6. **Gene: tonight’s six results proved that disciplined, pre-registered falsification was worth it—it killed square 3 and the irregularity story while exposing a real load-immune fan-out emission win—but they did not prove a general wall-speed advantage, a correctness advantage, or a product win beyond this caller and task family.**

## 23. Verdict on the pair (Fable, 00:38Z) — agreements, the disagreement table, what launches

**Where the two seats converge without seeing each other (the strong signal):** (a) retire "fan-out a regex cannot close" — both restate square 2 as a claim about *measured behaviour* (7–16× fewer emitted write characters, byte-identical, every cell including the regex-closeable one); (b) the driver of native's payload is the literal changed surface under its default patch policy (~350–450 chars per owner), not irregularity; (c) E-SLOPE80 is re-designed, not run as written: k = 1, emitted chars primary, edit representation / strategy as a pre-registered mediator, wall descriptive; (d) routing is last, with zero mechanism of action here; (e) the missing evidence is the same list: a native arm with a compact rewrite made salient, another caller, another N, a real repo, a correctness (not emission) win.

| question | Opus | Sol | verdict |
|---|---|---|---|
| first experiment | **E-AFFORD**: license sed explicitly, 9 native arms, no build — "falsifies tonight's headline before Gene quotes it" | E-FIRSTCALL-DESC (8 T arms testing a description fix) | **E-AFFORD launches now.** Both seats name the salient-compact-rewrite counterfactual as the load-bearing gap; only Opus makes it the first experiment. Sol's first pick measures a build, not native. |
| square 4 | E-EXTRACT: `:extract!` on the E6-Q2 big files, correctness gate + chars, 6 arms + 1.5 h fixture | **E-PREWRITE**: planted failing postcondition, proof-before-write witnessed by transcript + filesystem hash, 6 arms | **E-PREWRITE first** (cheaper, binary load-immune meter, measures square 4's actual claim); E-EXTRACT after it, as the correctness-at-risk variant. Pre-registration drafted now by the cross-attack agent. |
| N = 80 | E-CEILING80: k=1, chars + patch-call count + strategy classifier, 5 arms | E-SLOPE80-C: k=1, chars primary, 3 pairs, 6 arms | **Same design; take Opus's classifier and Sol's withdrawal clauses.** Runs after E-AFFORD (shared classifier), only when load allows the ~300 s native arms. |
| first-call refusal | **schema** accept-and-normalise (the description was already in tools/list and the flat prior still won 3/4 of the time across three callers) | **description** first as a canary; schema only if it fails | **Build both in one small branch**: the worked nested example in the description AND accept-and-normalise with a `normalized_from` field in both text and structured; acceptance witness = Sol's E-FIRSTCALL (8 T arms on the E-REG fixtures) run AFTER the build, not as an experiment before it. It is a product chore, not a cohort win (Opus's corollary: on the claimable meter it buys ~0). Filed as an inbox item. |
| the sentence for Gene | the fan-out win is real and load-proof; the read side is not ours; the sed counterfactual must run before anyone quotes the number | pre-registered falsification was worth it (killed square 3 and the irregularity story, exposed a real emission win); no wall-speed, correctness, or beyond-this-caller claim | Both true; Opus's names the one experiment that can still take the headline away, so it goes first. |

**Launched at 00:38Z:** E-AFFORD (Opus runner, 9 native arms on the E-REG fixtures, pre-registration before the first arm); the cross-attack (Opus attacks Sol's answer and drafts the merged E-PREWRITE pre-registration; Sol attacks Opus's answer); inbox item for the first-call schema+description branch. Functional share of launched work stays above 50%.

## 24. Cross-attack — Sol on Opus, verbatim (00:44Z; recovered from the codex log: `codex exec -o` clobbered the file Sol wrote at the same path — sol-yolo fixed the same minute)

# Cross-attack of Opus's E6-Q2 + E-REG poll

## 1. F1 and E-HUNK

**Strongest objection.** The claimed **27 +/- 5 characters per emitted line** is not a measurement. The receipt measures emitted characters and canonical churn, but not emitted patch-body lines. Opus manufactures the denominator: 168 canonical diff lines plus an assumed seven overhead lines per file gives `168 + 21*7 = 315`, and `8,550/315 = 27.1`. That cannot distinguish changed-line cost, hunk-context cost, path/header cost, or strategy choice. Its own prediction is internally inconsistent: with a *fixed per-file* seven-line overhead, H2 has `336 + 21*7 = 483` lines, so F1 predicts about **13,041 characters**, or **10,626-15,456** across its stated band—not 15,000-18,000. The higher prediction silently assumes a second hunk and therefore a second context allowance. E-HUNK never says whether the two sites coalesce into one hunk or are far enough apart to create two.

**Does the objection survive the receipts? Yes.** E-REG holds canonical churn at **+84/-84 for every k**, yet individual native emissions range from **1,929 to 16,531 characters**. Its measured per-patch cell means are **5,262 / 8,550 / 8,190 / 8,951**, not a measured line-rate series. The 1,929-character arm is especially fatal to an unconditional line-count law: the cohort says that arm generated its patch programmatically. The same output diff therefore does not imply the same emitted representation.

**Design change.** Replace E-HUNK with a small factorial and meter the actual request: one site/one hunk, two adjacent sites/one hunk, and two separated sites/two hunks. Record emitted patch lines, changed lines, context lines, headers, request characters, patch-call count, and the content-based strategy class. Pre-register the line-rate only within literal-patch arms and treat strategy switching as a separate outcome. With the observed 1,929-versus-8,594 split at k=1, `n=2` per cell cannot support a +/-20% refutation threshold; use at least six native arms per cell or present the result as a strategy-distribution probe, not a fitted law.

## 2. The restated square-2 claim

**Strongest objection.** A sceptic can dodge it because one of its supposedly receipt-bound clauses is false and another is overbroad. Opus says **8 of 8** native runs "hand-transcribed a full patch." E-REG explicitly says `ereg-k1-N-2` **"did not hand-type its patch"**; it emitted a compact JS table and generated the patch body. A sceptic can then correctly restate the result as a harness- and prompt-conditioned default-policy observation, not an inherent native limitation: one caller, generated fixtures, N=21, and a prompt in which `apply_patch` was salient while a stream rewrite was not. Likewise, "one request" is defensible only as *one committing request*: **6 of 8** tool arms first paid a refused request.

**Does the objection survive the receipts? Yes.** The honest native range is **1,929-16,531 characters**, with literal patching in **7 of 8**, not hand transcription in 8 of 8. All **16 valid arms** were 6/6 and byte-identical, so the established advantage is emission under the observed policy, not correctness. The tool's **485-1,260** character range is real, but so are the **6/8** first-call refusals.

**Design change.** The surviving claim should be: *For this caller, harness, N=21 alias-migration family, and default native prompt, 7/8 native arms emitted literal patches and one generated a compact patch; native write requests totaled 1,929-16,531 characters versus 485-1,260 for the tool, and every valid arm was byte-identical to canonical.* Then run the salience counterfactual before calling the behavior a product boundary.

## 3. E-AFFORD and the meaning of “native”

**Strongest objection.** Naming or licensing `sed` is an intervention, not contamination in the capability sense. The arm still uses no clj-surgeon and is therefore native, but it is no longer the autonomous native control measured by E-REG. It must be labelled **native-salient**, not silently pooled with N. The causal question is precisely whether prompt salience changes strategy, so the manipulation is legitimate only if permissions, task text, gate, and available tools are otherwise identical.

Use this exact intervention line:

> Clarification: as in every native arm, you may use any available native write path, including `apply_patch`, `sed`, `perl`, or a generated patch. Choose whichever method you judge fastest and safest; none is required or preferred. Do not use clj-surgeon.

That wording names the alternatives without asserting that a regex is correct, supplying a command, or relaxing the acceptance gate.

**Does the objection survive the receipts? Yes.** E-REG observed **zero `sed` or `perl` rewrites in 8 of 8 native arms**, but it also observed compact programmatic generation in **1 of 2 k=1 arms**. This is exactly why salience is a treatment variable. The existing cohort proves default behavior; it does not prove behavior after the intervention.

**Design change.** Nine new native arms are valid for a same-wave comparison of default versus salient native behavior only if a fresh default cell is included and allocation is interleaved. They are **not** valid for Opus's predicted new T/N ratio: old T arms are historical context, not a contemporaneous comparator. Keep the nine-arm cost but run k=1 only: `N-default`, `N-salient`, and `T`, `n=3` each. Defer k=6. That directly tests both the prompt effect and whether the tool's emission advantage survives it.

## 4. Schema-first versus the description-first canary

**Strongest objection.** "The schema was present" does not establish that the caller read, understood, or could conveniently instantiate it. It establishes only availability. The same observation also cuts against Opus: a schema that was already in `tools/list` did not prevent repeated wrong envelopes. Accepting every guessed shape would mechanically suppress refusals, but it permanently widens the contract; silently treating an unknown `verification_profile` as the default is worse, because a typo can weaken requested verification while returning success.

**Does the objection survive the receipts? Yes, but it narrows my position.** E6-Q2 recorded **12 `invalid-mcp-request` refusals among 42 inspect calls**, for missing `expect` or root-level `file`; the coordinator made the same first two mistakes. E-REG then recorded a first-call refusal in **6 of 8** tool arms, including **4** invented flat shapes and **2** unknown profiles. Those figures prove that the current advertised contract does not overcome the caller's prior. They do **not** distinguish a copyable-description fix from compatibility normalization, because neither intervention was randomized.

**Design change.** Keep the description-first canary because it is reversible and discriminating: add one complete nested request plus the enumerated profile values, then pre-register first-call acceptance over eight fresh T arms. If at least 4/8 still refuse, add compatibility only for lossless, unambiguous syntactic sugar: flat alias fields may normalize when no nested request is present, and root-level `file` may hoist only when conflict-free. Continue to require `expect`; reject unknown verification profiles with the allowed values. Opus is right that compatibility is the deterministic fallback, but wrong to rank a safety-weakening default ahead of this canary.

## 5. E-EXTRACT versus E-PREWRITE

**Strongest objection.** E-EXTRACT does not, by itself, measure square 4. It measures a write-side semantic operation, fan-out, final correctness, and emission. Square 4 is **proof before write in the warm JVM**; E-PREWRITE measures the ordering and pristine-worktree guarantee directly. Calling extraction "the verbs with no native equivalent" does not turn final byte identity into evidence that proof preceded mutation.

**Does the objection survive the receipts? Yes.** E6-Q2 closes square 3 after **54/54 total probes**, and merely names `:extract!` among the remaining open write-side verbs; it contains no extraction result. E-REG reports **all 16 valid arms 6/6 and byte-identical**, so it supplies no existing native correctness deficit either. Opus's predicted native `<=2/3` extraction correctness is therefore a hypothesis, not a continuation of a measured gap.

**Design change: one pre-registration satisfying both.** Register one extraction cohort with two frozen sibling fixtures and `n=3` N/T pairs per fixture:

- **Valid fixture:** perform the 250-line family extraction. Gate on load plus byte identity to a frozen canonical; measure emitted write characters. This is E-EXTRACT.
- **Poisoned fixture:** the same extraction with one planted postcondition that must fail. The primary is an ordered witness that proof/refusal occurs before the first real-worktree mutation, plus an unchanged pre/post tree hash. This is E-PREWRITE and square 4.

Keep the decisions independent and written before arm 1: withdraw the square-4 claim if T mutates before proof in any poisoned arm or N proves before mutation in at least 2/3; withdraw the extraction advantage if T is not 3/3 correct on the valid fixture, or if N is 3/3 correct and under 5,000 emitted characters in at least 2/3. No wall primary. One document, two sibling cells, no post-hoc relabelling.

## 6. The sentence for Gene

**Strongest objection.** The sentence compresses away exactly the caveats Gene needs. It assigns **54/54** to the plain agent, although native contributed **18/18** and 54/54 is the total over all nine arms. It gives native's range as 7,377-16,531, omitting the 1,929-character compact native arm, and calls the result load-proof while sliding from a load-immune character measurement toward the broader word "win." It also says the read side "is not ours" without the cohort's one-caller, supplied-symbol/line-number boundary.

**Does the objection survive the receipts? Yes.** E6-Q2 says native was **6/6 in each of three runs, with three tool calls flat**; the whole cohort was 54/54. E-REG says **11 of 16 walls were void**, every arm was correct, and native emitted **1,929-16,531** characters. Those figures support an emission result and a scoped square-3 withdrawal, not a general speed or correctness win.

**Replacement sentence.**

> Tonight withdrew square 3 for this caller after native answered 18/18 supplied-symbol structural probes on two roughly 3,700-line files in three calls per run, and E-REG killed the irregularity story while showing a real default-policy emission gap—1,929-16,531 native write characters versus 485-1,260 for the verb, with every valid arm byte-correct—but it did not establish a wall, correctness, cross-caller, or sed-salient win.

## What I now concede to Opus

I concede that “a regex cannot close” must be retired; the defensible result is behavioral and prompt-conditioned.

I concede that emitted request characters are the right load-immune primary meter for this loaded-box follow-up.

I concede that E-AFFORD is the highest-value immediate falsifier of the current square-2 headline.

I concede that unambiguous schema normalization is likely the right fallback if a description-only canary fails.

I concede that E-EXTRACT is the strongest next write-side capability test, though it is not square 4 without an event-order witness.

**Committed recommendation for the next 60 minutes of arm time:** run the nine-arm, same-wave k=1 E-AFFORD cohort now—three interleaved `N-default`, three `N-salient` using the exact line above, and three `T`—with emitted write characters and content-based strategy class primary, byte identity as the gate, and no wall claim.

**Accepted into the running E-AFFORD before arm 1 (amendment 1):** Sol's fairness wording (names apply_patch as an equal option, "none is required or preferred"), the cell labelled N-salient and never pooled with N-default, cell C (k=6) dropped for a contemporaneous T k=1 ×3 cell so the ratio has a live comparator, cells interleaved. Also accepted: Opus's "8 of 8 hand-transcribed" is false (k1-N-2 generated its patch programmatically, 1,929 chars) — the report row already carries both k=1 figures; Sol's replacement sentence for Gene is the more exact one and goes in the next Gene report.

## 25. Riff for Gene ("Excite me with possibilities of future involvements", 00:5xZ 2026-09-04) — chronicle material

Grounded in the night's one hard fact: the agent's cost is typing the patch, and it types even when it could script.

Riding tonight's batch: E-AFFORD decides the headline (agents do not reach for the tool they are not handed, or the win narrows to irregular/large fan-out); E-PREWRITE turns "faster" into "the agent never leaves your tree broken" (proof in a warm JVM before mutation, worktree byte-pristine at refusal); E-CEILING80 turns the win into a boundary (at ~33,000 emitted chars native splits or scripts) and makes a monorepo-wide migration the flagship demo; the first-call refusal fix removes a tax every real user pays on call one.

Newly cheap: every fan-out is the same product (keyword renames across maps, deprecation sweeps, arity changes, require reordering, spec/malli schema sweeps — one intent across N owners, each measurable in an evening); the apparatus itself is a product (pre-registration, load-immune meters, two seats polled blind then cross-attacked, withdrawal conditions before the data — what anyone shipping an MCP tool needs to make an undodgeable claim; LIVE → TEACH in one box); the affordance result is a Pandora scene ("affordance" is Gibson/Norman's term, not ours) — a multi-caller sweep (Sonnet/Opus/Haiku/Sol as the native arm) gives a which-models-reach-for-which-tools table nobody else has.

Further out, filed: fan-out where native cannot follow (cross-repo; semantic collision resolution — 30 collisions in one call tonight); the linked-intent ratchet as the merge gate (no change lands without a named intent and a witness the verb alone can produce); voice on top of the remote ("migrate store-2 to es across the app", one verified commit, receipt read back).

Cherry-pick: tonight = E-AFFORD, E-PREWRITE, E-CEILING80, the first-call fix. Next session = multi-caller sweep; one more fan-out verb chosen for a real migration in one of Gene's repos; the apparatus write-up as a workshop unit.

## 26. Cross-attack — Opus on Sol, verbatim (00:57Z). Its §0 CORRECTS §21/§23 and the 00:55Z Gene report: E3-P-N.md §5 already licensed a scripted edit; E-AFFORD is a salience gradient, not a counterfactual

# Cross-attack: Opus red-teams SOL's E6-Q2 + E-REG poll

*forge@anvil, 2026-09-04. Read-only. Sources read with `cat`/`sed` only, no Surgeon MCP call,
nothing edited, no code run: `/home/forge/tmp/sol/e6q2-ereg-poll.md`,
`/home/forge/tmp/sol/regpoll-opus.md`, `docs/observations/2026-09-04-e6q2-bigfile-cohort.md`,
`docs/observations/2026-09-04-ereg-irregularity-cohort.md`,
`docs/observations/2026-09-03-brainfleet-hills.md` §16–§23, plus the frozen apparatus under
`/home/forge/tmp/arms/{ereg,e6q2}` and the prompt files at
`clj-surgeon-arms/bench/anvil-arms/prompts/E3-P-{N,T}.md`.*

**Rule I am holding myself to:** every objection below is scored against a figure that exists in
a frozen receipt, and where the receipt kills my own objection I say so in the same paragraph.
Two of the six objections die on the receipts. One of them kills a sentence of *mine* that is
already in the brainfleet doc and is about to be quoted to Gene — that correction is §0.

---

## 0. Before the six: a correction that outranks the whole attack

**The claim "native was never offered a scripted rewrite" is false, and it is my sentence.**

`clj-surgeon-arms/bench/anvil-arms/prompts/E3-P-N.md` (sha256
`9ab5267a77a2a02bb5bf4e4833d2bcbcb5055550f2830bdff092f316baa638f6`), §5 TOOLING, **verbatim, the
prompt every one of the 8 E-REG native arms ran**:

> You have your ordinary native tools only: shell, rg, sed, and apply_patch. There is no
> structural editing server available. **Use whatever route you judge fastest and safe,
> including a scripted edit if you believe it is correct for this tree.**

`sed` is *named in the tool list* and a scripted edit is *explicitly licensed*. My poll §2 said
the counterfactual gap was that "no native arm was ever *offered* a scripted rewrite… that is one
prompt line away from collapsing," and my §6 sentence for Gene said "nobody ever offered it one."
Both are wrong on the frozen prompt. What is true is weaker and still interesting: **native was
licensed to script and declined 8 of 8 times anyway** (E-REG: "Zero of eight native arms used a
`sed` or `perl` rewrite at any k — including k=1").

Three consequences, all of which land tonight:

1. **Square 2's claim gets *stronger*, not weaker.** "Native does not reach for the regex even
   when it may" is now a measured behaviour under an explicit licence, not an artefact of silence.
2. **E-AFFORD — launched at 00:38Z per §23 on my recommendation — is no longer a counterfactual.**
   Its control cell is not "unlicensed"; it is "weakly licensed." E-AFFORD is now a **salience
   gradient** experiment (licence buried in a clause vs licence made the headline of §5), which is
   a smaller and more fragile claim. Its pre-registration must say so **before it lands**, or its
   result will be reported against a control that does not exist.
3. **§21 and §23 of the brainfleet doc, and my §6 sentence, need the correction in place before
   anyone quotes the number to Gene.** This is exactly the class of defect the program has been
   catching all night: a document asserting a state nobody verified at the place it takes effect.

I am not asking Sol to own this. It is mine. It changes what I recommend at the end.

---

## 1. "350–450 chars per owner; refute with native-only N={5,21}, n=2, 4 arm-runs"

**Sol's claim.** Payload is ~linear in owner count at 350–450 emitted chars per owner under the
default patch policy; refute by holding k=1 and bytes-per-owner fixed, varying only N, 4 native
arm-runs; the claim loses if N=21's mean is under 3× N=5's (4.2× predicted) or if compact
generation appears in a majority of arms.

### Objection A — the band as written covers **half** the arms it was fit to

Per-owner chars, computed from E-REG's own table at N=21 (chars ÷ 21), per arm:

| k | arm | chars | chars/owner | inside 350–450? |
|---|---|---|---|---|
| 1 | N-1 | 8,594 | **409** | yes |
| 1 | N-2 | 1,929 | **92** | no |
| 2 | N-1 | 9,724 | **463** | no |
| 2 | N-2 | 7,377 | **351** | yes |
| 3 | N-1 | 8,202 | **391** | yes |
| 3 | N-2 | 8,179 | **389** | yes |
| 6 | N-1 | 16,531 (2 patch calls; 8,951/call) | **787** (426/call) | no (yes per call) |
| 6 | N-2 | 9,636 | **459** | no |

**4 of 8 arms inside the band; 5 of 8 if k=6's two-call arm is scored per patch call.** And the
k=1 *cell mean* is **5,262 / 21 = 251 chars per owner** — outside Sol's band entirely, because the
one compact arm halves it. **Survives.** A band that already misses half its own training data
cannot be the pre-registered predictor for a new sweep. *Change:* state the law **within the
literal-patch stratum only** — there it is 389–463 chars/owner across 6 arms, a genuinely tight
band — and report the compact-generation rate as a **separate** outcome, not as a loss condition
bolted onto the same estimate.

### Objection B — n=2 cannot see a 3× threshold through a bimodal outcome

E-REG's k=1 native cell is **8,594 and 1,929 — a 4.5× spread inside one cell at n=2**. Sol's own
loss condition ("compact generation appears in a majority of arms") concedes the contaminant
exists, but with n=2 per cell "majority" is one arm. Worked case: if the N=21 cell draws two
literal arms (~8,600 mean) and N=5 draws two literal arms (5 × 410 ≈ 2,050), the ratio is 4.2×
and the claim passes. If N=21 draws *one* compact arm, its mean falls to ~5,250 and the ratio is
2.6× — **the claim is refuted by a coin flip on a strategy that has nothing to do with N.**
**Survives.** *Change:* n=3 per cell minimum, **median not mean**, and the strategy classifier
pre-registered as a stratifier rather than as a footnote.

### Objection C — the strategy mediator is **correlated with the manipulated variable**

This is the one that breaks the design rather than resizing it. A compact program (the
`ereg-k1-N-2` shape: "a compact JS table of 21 filenames" generating the patch body) has a fixed
cost of roughly 1,500–2,000 characters and pays off only when the literal patch it replaces is
larger than that. At **N=5** the literal patch is ~2,050 chars — *the compact program is not
cheaper*, so no rational caller writes one. At **N=80** the literal patch is ~33,000 chars — the
compact program is 15× cheaper and a strategy switch is the rational move. So the manipulation
(N) **drives the mediator (strategy)** in a known direction, and a design that reports one mean
per cell cannot separate "payload is linear in N" from "strategy switches with N." **Survives,
and it is fatal to the 2-cell shape.** *Change:* three cells, classifier-stratified, and report
the per-owner constant only within-stratum.

### Objection D — "does N=5 even produce a patch big enough to measure?"

**This objection FAILS on the receipts, and I withdraw it as posed.** The meter resolves to the
character; the smallest native payload ever recorded in this program is **1,929 chars over a
12.529 s emission gap at 154.0 chars/s** — comfortably measurable, and N=5's predicted ~2,050
chars is larger than that. Measurability is not the problem.

**But the receipt that saves it kills the cell for a different reason.** N=5's predicted literal
patch (~2,050 chars) and the compact-program floor already observed at N=21 (**1,929 chars**) are
**the same number**. The low cell sits exactly where its own two competing strategies become
indistinguishable on the primary meter. **Drop N=5.** *Change:* the informative contrast is
**N=21 (already have 8 native arms, free) vs N=80 (33,000 vs a ~2,000-char compact floor, a 16×
separation)**. This also removes Sol's re-run of N=21 — 4 arm-runs recovered before anything
launches.

### Net for §1
Objections A, B, C survive; D fails as posed and returns a stronger conclusion. **What to change:**
do not run N={5,21} n=2. Reuse E-REG's 8 native N=21 arms as the anchor, spend the arms at N=80,
n=3, k=1, classifier-stratified, median-scored.

---

## 2. "Retire 'a regex cannot close it'; the restated claim is 7–16× fewer chars in every cell"

**We agree on the disposition** — I retired the same sentence in my §2 for the same reason. So
the attack is on the *restatement*, which is what will be quoted.

### Objection A — "7–16× … in every tested regularity cell" is a cell-mean statement wearing a per-arm uniform

Cell means: 5,262/749 = **7.0×** (k=1), 8,550/1,140 = **7.5×**, 8,190/990 = **8.3×**,
13,084/828 = **15.8×**. So 7–16× is right *for cell means*. Per **arm pairing** at k=1 the worst
case is `ereg-k1-N-2` 1,929 vs `ereg-k1-T-1` 1,013 = **1.9×**. **Survives.** A sceptic reading
"in every tested cell, 7–16×" and then finding a 1.9× pairing in the same table will discard the
whole claim, and will be right to. *Change:* say it as I did — "**7.0× to 15.8× at the cell mean,
never below 1.9× in any arm pairing**." The floor is unimpressive and it is the number that makes
the claim un-dodgeable.

### Objection B — the compact arm is missing from the restatement

`ereg-k1-N-2` is the single most load-bearing arm in the cohort: the runner calls it "the one
partial collapse… the most interesting arm in the cohort, and it still lost." Sol's restatement
does not mention that native compressed once in two runs in the cell most favourable to it.
**Survives.** *Change:* the restatement must carry "compressed once in two k=1 runs and still lost
by 1.9×" — omitting it is the difference between a claim that survives review and one that gets
called selective.

### Objection C — the missing-evidence list omits the affordance fact from §0 above

Sol lists "a direct comparison after making a compact native rewrite salient" as missing, which is
correct as far as it goes, but §0 shows the licence was **already in the prompt**. **Survives, and
it lands on both seats equally.** *Change:* the missing item is not "offer native a script"; it is
"**make the licence salient rather than buried, and see whether salience alone moves 8/8**." That
is a much narrower claim, and it changes what E-AFFORD can conclude.

### Net for §2
All three survive; none is fatal. Sol's disposition is right, the wording is over-claimed in one
direction and under-claimed in another.

---

## 3. "Re-design, then run now" for N=80, given ~300 s native arms on a shared box

### Objection A — "run now" contends for a lock another cohort already holds

E-AFFORD launched at 00:38Z (§23) with 9 native arms, and every arm in this apparatus serialises
on `flock /home/forge/tmp/arms/arm.lock` (`run-ereg.sh`, verbatim: "Serialised with E6-Q2 (port
7909) via flock /home/forge/tmp/arms/arm.lock, PER ARM"). Six N=80 arms at ~300 s native +
~40 s tool is **≈ 20 minutes of exclusive arm time queued behind ~20 minutes of E-AFFORD**.
"Run now" is not available; the honest phrasing is "run next." **Survives as a scheduling
correction**, not as a design objection.

### Objection B — **the ordering is wrong, and this is the real hit**

E-AFFORD is testing whether one line of §5 changes native's editing strategy. N=80's entire
prediction ("native median 30,000 characters… with native still choosing literal patches in at
least 2/3 arms") is **conditional on that strategy**. Running N=80 before E-AFFORD lands means, if
E-AFFORD moves the strategy, the N=80 result measures a policy a one-line prompt edit removes —
and the whole cohort has to be re-run against the new default. **Survives.** *Change:* N=80 runs
**after** E-AFFORD, and inherits E-AFFORD's classifier and its winning §5 text as the pre-registered
native condition.

### Objection C — "measuring a number you can compute" — **this one FAILS**, and I concede it

I made this objection in my own §3 ("80 × 410 = 32,800… the definition of waste"). Sol's re-design
answers it exactly: the char total is derivable, but **the strategy switch is not**, and Sol keeps
it as a pre-registered mediator. Objection C dies on Sol's own design. Concede.

### Objection D — the wall clause is doing no work

Sol says "make wall descriptive unless every included pair meets the load rule." Tonight's record:
E-REG **11 of 16 walls void**, E6-Q2 **8 of 9 void** — **19 of 25**. The probability that three
N=80 pairs all clear load 8 on a box that reached **18.19** tonight is small enough that the clause
is decoration. **Survives, weakly.** *Change:* drop the conditional. Declare wall descriptive
unconditionally at N=80 and stop spending sentences on it.

### Net for §3
Verdict **re-design** is right and I hold the same one. "Run now" is wrong on two counts —
the lock, and the dependency on E-AFFORD. **Run third, not first.**

---

## 4. "Description-first canary" vs the E6-Q2 fact that 12 `invalid-mcp-request` refusals happened with the schema in `tools/list`

### The receipt, verbatim (E6-Q2 cohort, "Secondary: 52% of `inspect_clojure` calls were refused")

> **`invalid-mcp-request` × 12** — a missing `expect` block, or an argument at the wrong nesting
> level (`file` at the request root instead of inside `requests`). **The coordinator hit the same
> two refusals by hand in the pre-flight smoke test, on the first two calls, before any arm ran.**

### Objection A — "the documentation was already there" is **not** what that receipt says. This objection FAILS.

I made this argument in my §4 ("MCP ships the JSON schema in `tools/list`; the model read it and
still emitted a flat shape"). It does not survive contact with the distinction Sol is drawing.
**A JSON schema is not a worked example.** `tools/list` carries types and required keys; it does
not carry one copyable nested request, and it does not enumerate the legal
`verification_profile` values — which is exactly the second refusal class
(**`unknown-verification-profile` × 2** in E-REG). Sol's fix targets the two things the schema
provably does not supply. **My objection dies; Sol's canary is not refuted by the E6-Q2 fact.**
I concede §4's core.

### Objection B — but the human datapoint still bites, in a different place

Three independent callers — one human coordinator and two model contexts, on two different verbs
(`inspect_clojure` in E6-Q2, `alias_migration` in E-REG) — all guessed **flat where the tool
demands nested**, and E-REG records "**the agent's first guess at the request shape is wrong
three-quarters of the time**" (6 of 8 T arms). When a prior beats the specification across a human
and two model contexts, "one more sentence" is a *hypothesis about that prior*, not a fix.
**Survives as a probability statement, not as a refutation.** §23 already resolved it correctly —
build both in one branch. Sol and I are both wrong to have litigated the order.

### Objection C — the real error is spending **8 arm-runs** on the canary

Sol's §5 #1 (E-FIRSTCALL-DESC) costs 8 arm-runs. Its primary — "first call accepted" — is binary
and load-immune, so I concede it is *claimable*. But: (a) §23 has already ordered **both** fixes
built, so the 8 arms are an **acceptance witness for a chore**, not an experiment; (b) my §4
corollary stands unchallenged — on emitted characters, the only meter this program can currently
claim, fixing it buys **~0** (the refusal costs ~120 chars of re-emission out of ~926); and (c) 8
arms is roughly two-thirds of a night's arm budget. **Survives.** *Change:* **4 arms, one per
E-REG fixture cell (k ∈ {1,2,3,6})**, run after the build, reported as an acceptance receipt and
never as a cohort win.

### Objection D — the predicted saving is 1.7–2.7× the runner's own measured figure

Sol: "affected arms save 5–8 seconds and mean tool wall falls about 4–6 seconds." The E-REG cohort
doc measures the price directly: "**The price is small (one call, ~3 s inside a 5.6–7.3 s total
emission gap)**." Sol's 5–8 s is the brainfleet §20 paraphrase ("one refused call ≈ 5–8 s of the
tool's 32–43 s wall"), not the runner's own number. And a predicted 4–6 s improvement on a
34–40 s base is ~15% — **on a wall meter with a measured 172 s floor at n=3 and 19 of 25 walls
void tonight**. **Survives.** *Change:* delete the wall prediction entirely; predict only the
binary ("first call accepted, 7/8 → target ≥7/8"), which is the meter that survives the floor.

### Net for §4
Sol wins the description-vs-schema argument and I concede it. Sol loses on cost (8 arms → 4) and
on the wall prediction (delete it).

---

## 5. E-PREWRITE — is "proof/refusal precedes the first worktree mutation" measurable for a native arm that never intends to prove anything, and is N 0/3 a tautology?

### Objection A — **as worded, yes, it is a tautology, on BOTH sides.** Survives, and it is the sharpest objection in this document.

Read the tool's own refusal constructor, `clj_surgeon/alias_migration.clj`, verbatim:

```clojure
(defn- refusal
  [error-type message extra next-call]
  (merge {:ok false
          :operation "alias_migration"
          :error_type (name error-type)
          :error message
          :source_unchanged true
          :mutation_attempted false
          :write_authority false
          ...
```

and `plan`, which folds **every** source file into one result and short-circuits on the first
refusal *before any write happens at all*. **T proving-before-write is an architectural invariant
of the verb, not an empirical outcome.** Meanwhile, on a task every arm can complete, native's only
"proof" is `bin/fan-test` — which the shared §3 DONE MEANS places *after* the edit. So on a task as
Sol describes it, **T 3/3 and N 0/3 are both known before arm 1**, and six arm-runs buy a
restatement of an implementation detail.

*Change (this is what deliverable 2 fixes):* the task must be one where **the correct outcome is
to change nothing and report**, the block must be **cheaply discoverable by a read**, and the
prompt must state the all-or-nothing rule identically in both arms. Then N can win by two cheap
actions — one `rg` and one sentence — and N 0/3 is a **behaviour**, not a definition.

### Objection B — the receipt that makes N a live contender, so 0/3 would be a finding

E6-Q2's native arm: "**three tool calls, flat, in all three runs**", "**~300 of 7,516 lines
read**", **54/54 probes correct**, using `rg` then ten numbered `sed` windows. This caller is a
**very** strong read-first agent. If a native arm that reads that well still writes before it
reports a block it could have found with one `rg`, that is a genuine, quotable behavioural finding
about square 4. **The objection to Sol here is not that N 0/3 is uninteresting — it is that Sol's
task design makes it unearnable.**

### Objection C — "witnessed by transcript plus filesystem hash" has no sampling procedure

A hash at start and a hash at end **cannot detect a write-then-revert**, which is precisely the
failure mode the meter exists to catch. Neither `inotifywait` nor `fswatch` is installed on this
box (checked). **Survives.** *Change:* a driver-side 250 ms hash poller writing a change-only
ledger, **plus** an independent second predicate at arm exit — `find src -newer <t0 stamp>`, since
**ctime changes even when content is restored**. Two predicates, disagreement reported typed,
never silently resolved.

### Objection D — the T-side escape hatch is unpriced

E3-P-T.md §5, verbatim: "**You still have your native tools; use them if the tool cannot complete
the task.**" So a T arm can take the refusal and then hand-patch 21 files — which passes the
proof-before-write primary and **fails the task**. Sol's withdrawal clause does not cover it.
**Survives.** *Change:* pre-register it — if T proves-before-write 3/3 but fails the correctness
gate in ≥2/3 arms, the product claim is withdrawn: *a refusal the agent immediately routes around
is not proof before write, it is a speed bump.*

### Objection E — Sol's withdrawal clause is half dead weight

"withdraw square 4 if T fails to prove-before-write in any arm" is, per Objection A, near-impossible
by construction; "or if N achieves it in ≥2/3" is the whole informative half. **Survives, weakly.**
*Change:* keep both verbatim (a withdrawal condition written before arm 1 is not to be edited after)
and **add** the two above, also before arm 1.

### Net for §5
Objection A is fatal to the experiment **as worded** and repairable **as designed** — which is what
`/home/forge/tmp/sol/eprewrite-prereg.md` does. Sol's instinct (cheap, binary, load-immune, 6 arms)
is right and beats my E-EXTRACT as the first square-4 experiment; Sol's task is the part that
cannot survive.

---

## 6. The sentence for Gene

Sol's sentence:

> Gene: tonight's six results proved that disciplined, pre-registered falsification was worth
> it—it killed square 3 and the irregularity story while exposing a real load-immune fan-out
> emission win—but they did not prove a general wall-speed advantage, a correctness advantage, or
> a product win beyond this caller and task family.

### Objection A — it contains **zero numbers**, and Gene has ruled twice on exactly this

Standing policy 3, count-first status: "every status line leads with the number a human would panic
about." His 2026-09-02 ruling in my own memory: "every experiment report opens with the table
(arm, wall, correct?) + one line of learning + one caveat; A vs B vs native timings first." Sol's
sentence leads with a **methodological virtue**. **Survives.** *Change:* lead with 7,377–16,531 vs
485–1,260, and 54/54 in three tool calls.

### Objection B — "did not prove a general wall-speed advantage" understates the truth

Nineteen of twenty-five walls tonight are **void**, not merely inconclusive: E-REG 11/16, E6-Q2
8/9. "Did not prove" reads as "the evidence leaned the wrong way." The honest and stronger
statement is "**no wall was claimable at all; every headline tonight rests on characters and
counts, which load cannot move.**" **Survives.**

### Objection C — it omits the experiment that can still take the headline away

Mine named it; Sol's does not. Post-§0, the item has changed but has not gone away: the licence was
buried in §5's last clause, salience was never varied, and 8/8 declined anyway. **Survives.**

### Objection D — "a product win beyond this caller and task family" is doing too much work in one clause

It bundles four distinct exposures the cohort docs list separately (another caller, another harness,
another N, a real repo). One clause hides four. **Survives, mildly.**

### The replacement sentence I would send

> **On the same 21-file rename, the plain agent hand-typed 7,377 to 16,531 characters at about
> 145 a second while the verb sent 485 to 1,260 and both landed byte-identical trees — the verb
> won all four cells including the perfectly tidy one — and on the two biggest files in the repo
> the plain agent answered 54 of 54 structural questions in three tool calls, so the read-side
> square is withdrawn on its own pre-registered terms; no wall figure is claimed anywhere tonight
> because 19 of 25 walls were void under load, and the one thing that could still take the
> character win away is that the native prompt already licensed a scripted edit in its last line
> and the agent declined it 8 times out of 8 — nobody has yet tested whether making that licence
> prominent changes the number.**

---

## What I CONCEDE to Sol — one line each

1. **§4 core:** a JSON schema in `tools/list` is not a worked example; my "the model already had
   the description" overstated the receipt, and Sol's description-first canary is not refuted by
   the E6-Q2 human datapoint.
2. **§5 ordering:** E-PREWRITE beats my E-EXTRACT as the first square-4 experiment — binary,
   load-immune, 6 arms, no 1.5-hour fixture build.
3. **§3 objection C is dead:** the N=80 char total is derivable but the strategy switch is not, and
   Sol's mediator design answers my own "measuring a number you can compute" objection.
4. **§1 framing:** "per owner, conditional on the default editing policy" is the better statement of
   the law than my "per emitted patch line" — it names the conditioning clause instead of the format.
5. **§3/§5 withdrawal clauses:** Sol's are sharper than mine ("T not ≥5× smaller in ≥2/3 pairs";
   "N achieves proof-before-write in ≥2/3") and I adopt both verbatim rather than re-wording them.
6. **Wall posture:** Sol's "make wall descriptive" is the right default given 19 of 25 void tonight;
   I was still arguing about wall in places where the meter no longer exists.

---

## One committed recommendation for the next 60 minutes of arm time

**Do these three things, in this order, and start nothing else.**

1. **(0–5 min, no arms) Land the §0 correction before any result is quoted.** Amend brainfleet §21
   and §23 and my §6 sentence to say that E3-P-N.md §5 already licensed a scripted edit, and
   re-label E-AFFORD's cells **weakly-licensed vs salient** in its pre-registration — *while it is
   still running*, not after. An experiment whose control is mis-described in the doc that reports
   it is the exact defect this program spent the night ratcheting against.
2. **(5–25 min) Let E-AFFORD finish; do not add load.** It holds `arm.lock`; its 9 native arms are
   the gate on both square 2's headline and N=80's native condition. Nothing else touches the box.
3. **(25–60 min) Run E-PREWRITE from the merged pre-registration** at
   `/home/forge/tmp/sol/eprewrite-prereg.md` — 6 arms (3 N + 3 T), the trap fixture derived from
   the frozen `fanout-k2` tree, port **7941**, primary = worktree content hash unchanged at the
   first proof/refusal. Its arms are short (the correct answer is "change nothing"), it needs no
   new fixture generation, and it is the only unmeasured square left standing.

**Explicitly NOT in the next 60 minutes:** N=80 in any spelling (it depends on E-AFFORD's result
and costs ~20 min of exclusive lock); Sol's N=5 cell (its two strategies are indistinguishable at
~2,000 chars — drop it permanently, and reuse E-REG's existing 8 native N=21 arms as the anchor);
E-FIRSTCALL-DESC (it is a chore's acceptance witness, it runs after the build, and at 4 arms, not 8).

**Coordinator actions on receipt (00:57Z):** E-AFFORD amendment 2 sent before its affected arms (cells relabelled N-weak / N-salient; no prompt bytes, predictions, or withdrawal clauses changed); the 00:55Z Gene report corrected in place; the merged E-PREWRITE pre-registration copied to docs/observations/2026-09-04-eprewrite-preregistration.md and its runner launched (setup + the six §12 validations now; arms queue behind E-AFFORD on arm.lock). N=5 dropped permanently; N=80 waits for E-AFFORD; the description canary runs at 4 arms after the build.

## 27. Seventh result: E-AFFORD (01:11Z) — the salience gradient: INCONCLUSIVE by its own wording; the tool's win survives with a clean denominator

| cell | run | chars | chars/s | gap s | wall | strategy | gate | load |
|---|---|---|---|---|---|---|---|---|
| N-weak | 1 | 8,977 | 141.5 | 63.4 | 125† | literal | 6/6 | 5.6→8.2 |
| N-salient | 1 | 2,822 | 137.4 | 20.5 | 119 | generated | 6/6 | 7.8→6.5 |
| T | 1 | 1,214 | 172.9 | 7.0 | 37 | tool | 6/6 | 6.4→6.9 |
| N-weak | 2 | 1,869 | 151.8 | 12.3 | 86 | generated | 6/6 | 6.5→5.0 |
| N-salient | 2 | 10,090 | 141.8 | 71.2 | 133 | literal | 6/6 | 5.0→6.2 |
| T | 2 | 549 | 165.2 | 3.3 | 32† | tool | 6/6 | 6.5→9.9 |
| N-weak | 3 | 10,090 | 142.1 | 71.0 | 143† | literal | 6/6 | 9.9→9.0 |
| N-salient | 3 | 3,604 | 145.9 | 24.7 | 95† | generated | 6/6 | 8.2→8.0 |
| T | 3 | 1,136 | 181.1 | 6.3 | 42 | tool | 6/6 | 8.0→7.2 |

Cell means N-weak 6,979 · N-salient 5,505 · T 966; ratios 7.22× / 5.70×. P1 HIT, P2 MISS, P3 MISS, P4 HIT. Withdrawal: neither clause fired (clause 1 missed by one arm, 1/3 ≥ 4,000). INCONCLUSIVE, reported as such.

**What it found instead (runner):** between-cell difference 1,473 chars against within-cell spreads of 8,221 and 7,268 — the noise is ~5× the effect, interleaving rules out load drift. Strategy is bimodal and disjoint across all eight k=1 native arms of both cohorts: 4 literal (mean 9,438), 4 programmatic (mean 2,556), 0 stream-edit; the smallest literal (8,594) exceeds the largest generated (3,604). sed/perl/awk appeared in 5/6 native arms, always as a READER; the one perl edit printed a patch and fed it to apply_patch — native routes every write through apply_patch even when told it need not. The contemporaneous T cell (966) shows E-REG's historical comparator (749) overstated every ratio by 29%. Even native's best mode is 2.6× the tool.

**Learning (runner, verbatim):** "a 'counterfactual' that varies the salience of a permission the control already had measures the noise floor, not the mechanism." The true zero rung (no licence at all) has never been run and is not worth running: the licensed cells already decline. **Caveat that becomes the next variable:** the HARNESS, not the prompt — an arm whose only write path is Bash with no apply_patch (E-HARNESS, filed for the next poll). Apparatus: the runner overwrote E-REG's 16 payload receipts during validation and restored them within the minute; both scorers now refuse to write outside their own root; E-REG's scorer would have scored an `awk -i` or JS rewrite as ZERO chars — a silent zero in exactly the licensed cell — fixed before arm 1.

**Standing:** square 2's headline stands with the honest pair (7.2×/5.7× cell means; worst pairing 1.9×; strategy bimodal). Next on arm.lock: E-PREWRITE (square 4, running its validations), then E-CEILING80 (N=80, k=1, chars + strategy classifier; launched to queue behind E-PREWRITE).

## 28. Eighth result: E-PREWRITE (01:18Z) — square 4 WITHDRAWN on its own pre-registered terms

| arm | PBW | strict | composite | t_mutation | S1 | chars | non-test | T fallback | wall s | load |
|---|---|---|---|---|---|---|---|---|---|---|
| N-1 | TRUE | TRUE | agree_clean | null | PASS | 0 | 1 | 0 | 19 | 4.9→4.6 |
| T-1 | TRUE | TRUE | agree_clean | null | PASS | 555 | 2 | 0 | 24 | 4.6→5.7 |
| T-2 | TRUE | TRUE | agree_clean | null | PASS | 1,146 | 2 | 0 | 28 | 5.7→6.5 |
| N-2 | TRUE | TRUE | agree_clean | null | PASS | 0 | 2 | 0 | 24 | 6.5→6.7 |
| N-3 | TRUE | TRUE | agree_clean | null | PASS | 0 | 1 | 0 | 17 | 6.7→7.3 |
| T-3 | TRUE | TRUE | agree_clean | null | PASS | 966 | 3 | 0 | 40 | 7.3→6.8 |

Predictions 7 HIT / 5 MISS; the 25% tail (N 3/3) was the one that landed. §9 verbatim: Sol's clause fires, the symmetric-null clause fires, the speed-bump clause does not (T's S1 3/3, no fallback patching). **SQUARE 4 WITHDRAWN for this caller.** Nothing rounded.

**What it means.** The verb's proof-before-write is real and architectural; it is not a differentiator against this caller, which reads first when the task tells it a blocker may exist. The anti-tautology clause (one rg + one sentence pre-registered as a live native route) is what makes this a behaviour finding. Caveat: PBW was satisfied through the "never mutated" disjunct in 6/6 — "neither writes", not "who proves first". Two defects found live, routed to q5z r12 (items 9a/9b): the refusal text drops mutation_attempted/write_authority; the refusal's next_call proposed store-2-2, outside the caller's four-entry policy (all three T arms declined it, obeying the task over the "send that" instruction). Apparatus catch: sol-yolo sets required=true only when the worktree carries a .codex/config.toml — validation 4 failed first, fixed before arm 1, pre-correction ledger preserved.

**Standing after eight results.** Square 1 gate: unmeasured (O2 r2 / q5z r12 in flight). **Square 2 fan-out: the program's one win** (7.2×/5.7× cell means on emitted chars; worst pairing 1.9×; strategy bimodal; E-CEILING80 running for the boundary). Withdrawn for this caller: single edit at a known site, square 3 (E6-Q, E6-Q2), square 4 (E-PREWRITE). Fleet poll on the E-AFFORD + E-PREWRITE pair launched (Sol + Opus): with three squares withdrawn, what is square 1's experiment, is E-HARNESS (Bash-only native, no apply_patch) the next variable, and what does the program tell Gene now.

## 29. Fleet poll on the E-AFFORD + E-PREWRITE pair — Opus, verbatim (01:27Z)

# Opus — poll on the E-AFFORD + E-PREWRITE pair (three squares withdrawn; what is left, and square 1's experiment)

*forge@anvil, 2026-09-04. Read-only: `2026-09-04-eafford-sed-counterfactual-cohort.md`,
`2026-09-04-eprewrite-cohort.md`, `2026-09-03-brainfleet-hills.md` §19–§28, `docs/vision.md`
(the four squares), `docs/tech-tree.md` (the gate's status line). No code run, nothing edited,
no Surgeon MCP tool called.*

**Standing disclosure.** E-AFFORD was my #1 and my F2, run to kill my own headline. It did not
kill it, and it did not vindicate it either: **INCONCLUSIVE by its own pre-registered wording**,
clause 1 missed by a single arm. Its real finding is that my experiment measured the noise
floor — between-cell effect 1,473 chars against within-cell spreads of 8,221 and 7,268. I also
own the §0 correction that reclassified it from counterfactual to salience gradient. Everything
below is written with that in front of me.

**One correction to the state-of-play paragraph, before anything else, because a sceptic will
find it in ten seconds.** "Worst pairing 1.9×" is min-native ÷ **mean**-T (1,869 ÷ 966 = 1.93×).
The worst **arm-to-arm** pairing on the receipts is 1,869 ÷ 1,214 = **1.54×**. Both numbers are
in the E-AFFORD table; quote 1.54× as the floor, or a reviewer will compute it and ask why we
didn't.

---

## 1. What clj-surgeon is for, in one receipted sentence

> **clj-surgeon is for committing one intent across many owners in a single request: on a
> 21-owner alias migration, under the same 6/6 gate and byte-identical `diff -r` against the
> same frozen canonical, the verb committed in 549–1,214 emitted characters (mean 966) while
> the unaided agent — with `sed` and `perl` named to it by the prompt — emitted 1,869–10,090
> (cell means 6,979 and 5,505; ratios 7.22× and 5.70×, floor 1.54× arm-to-arm), flipping
> between two disjoint hand-written strategies (4 literal patches, mean 9,438; 4 generated,
> mean 2,556) and routing every write through `apply_patch` in 14 of 14 native write arms.**

Clause-by-clause receipts, all from tonight:

| clause | receipt |
|---|---|
| "one intent … single request" | E-AFFORD §7: every T arm, **exactly one committing `alias_migration` call**, zero native `apply_patch` fallback |
| "same 6/6 gate, byte-identical" | E-AFFORD §7: 9/9 arms `rescore-FAN.sh 21` 6/6 **and** `diff -r` clean; 21 files, 0 extras, 106/106 protected regions, `FAN-TEST 147 assertions, 0 failures` |
| "549–1,214, mean 966" | E-AFFORD table, T rows, contemporaneous with the native arms they are divided into |
| "1,869–10,090; 6,979 and 5,505" | E-AFFORD cell means |
| "7.22× / 5.70× / 1.54×" | E-AFFORD §2 ratios; the floor computed from the same table |
| "`sed` and `perl` named to it" | The +249-byte diff `@@ -54,0 +55 @@`, sha `bdb56497…` vs `9ab5267a…` |
| "two disjoint strategies, 9,438 / 2,556" | E-AFFORD §5 finding 2, pooled n=8 k=1 native arms; smallest literal (8,594) > largest generated (3,604) |
| "14 of 14 native write arms" | E-REG 8 + E-AFFORD 6; **0 stream-edit**, classifier validated 8/8 on hand-driven `sed -i` / `perl -pi` / `awk -i` plus 3 negatives |

### The sentence a sceptic uses against it

> "Characters are not seconds, dollars, or defects. You voided 15 of your 25 walls and claim no
> wall anywhere; the plain agent passed **every** correctness gate you ever ran it through —
> 14/14 patches byte-identical, 54/54 structural probes, 3/3 planted blockers found before
> writing a byte; three of your four squares withdrew against this same caller in one night;
> and the survivor is one model, one patch format, one generated fixture, one operation."

### Do the receipts answer it? **One clause of four. Say so in the report.**

- **"Characters are not seconds" — ANSWERED, and this is the strongest unused asset on the
  board.** Native chars/s stayed inside **137.4–151.8** and tool inside **165.2–181.1** across
  loads **5.02 → 9.87** in the interleaved E-AFFORD run. Emission rate is approximately
  load-invariant in the measured band, so the character gap converts: measured **emission gap
  12.3–71.2 s native vs 3.3–7.0 s tool**. That is observed duration of the model's own output,
  not box wall, and it survives the load rule that voided everything else. *Nobody has scored
  chars/s against load as a pre-registered hypothesis. It is a free regression over 25 existing
  arms and it upgrades every character claim to a time claim.*
- **"No correctness advantage" — NOT ANSWERED and not answerable from these receipts.** Native
  is 100% on every gate this program has ever run. This is the clause that will decide square 1
  (§2).
- **"Three squares withdrew" — answered in the sceptic's favour on breadth, in ours on
  credibility.** Pre-registration is why the survivor is worth anything; it is also why we can
  only claim one square.
- **"One caller, one harness, one fixture, one operation" — NOT ANSWERED.** This is the entire
  next wave (§5).

---

## 2. Square 1 (the gate) — the experiment, and my prediction that it withdraws too

**Can square 1 be made to differ from square 4 for this caller? YES — the meters are genuinely
different — but I predict it reaches the same verdict for a different reason, at 60%.**

*Why they differ:* square 4's meter was an **ordering** property (proof precedes the first
mutation). The verb satisfies it architecturally (`refusal` sets `mutation_attempted false`
before `plan` can write), and native satisfied it by never writing — so the meter was true on
both sides for reasons that had nothing to do with the product. Square 1's meter is a
**detection** property on a patch that has *already landed and already looks green*. No
architectural invariant supplies it; native cannot satisfy it by declining to write, because it
has written. Different meter, live on both sides.

*Why I still expect a null:* square 4 died because this caller **reads before writing**. Square 1
will most likely die because this caller's patches are **correct** — 14/14 byte-identical to a
frozen canonical, 54/54 probes, 3/3 blockers. **A differential detector needs a non-zero defect
base rate, and the measured base rate on this program's task family is zero.** That is a
structural problem no amount of arm time fixes.

*And the E-PREWRITE lesson names the cheapest native route explicitly, which is the second
problem:* the gate's substantive surface is, by the tech-tree's own status line, **"kondo delta +
focused suite in one receipt (BUILDING, inside the gate)"**, and z7c's single substantive catch
in six runs was literally typed `blocking-lint-findings`. **`clj-kondo` is installed on this box
and shimmed by this project's own Makefile** (`install-clj-kondo-admission`, `~/bin/clj-kondo`).
So the honest comparator for square 1 is not "native with nothing" — it is **native's own free
verification stack: reload + focused test + `clj-kondo`**. If the gate's catch set is a subset of
that, the differential is zero by construction and square 1 is a definition, not a finding — the
exact tautology that nearly voided E-PREWRITE.

So: **run the free half first, and let it decide whether the paid half runs at all.**

### E-GATE-R — the replay half. **0 arm-runs.** Run this first.

| | |
|---|---|
| **hypothesis** | `admit_clojure_patch` names ≥1 **substantive** hazard on a native patch that (a) the arm declared done, (b) the acceptance gate passed, and (c) `clj-kondo` **and** the focused suite do **not** name. |
| **corpus** | Every frozen native final-state patch on record: **E-REG 8 + E-AFFORD 6 = 14**, all 6/6 and byte-identical. (E-PREWRITE's 3 native arms wrote nothing; E6-Q2's are read-only. Excluded, and the exclusion is pre-registered.) |
| **load-immune meter** | Per patch, replayed over frozen bytes — deterministic, re-runnable, no arm time: `class ∈ {shape-refusal, substantive-catch, clean}`, plus the binary **`differential` = substantive-catch AND NOT named by `clj-kondo --lint` on the same tree AND NOT named by `bin/fan-test`**. Three predicates, disagreement reported typed. |
| **n** | 14 patches × 3 predicates = 42 replays. |
| **predicted numbers** | shape-refusal **0–1 of 14** (p=60% it is ≤1; the E1 grammar scar is the live risk — round five accepts both formats but has never been fed 14 real V4A bodies). Substantive catches on green patches **0 of 14** (p=70%). **`differential` = 0 of 14 (p=85%).** |
| **cost** | **0 arm-runs.** ~1 agent-hour, 1 JVM, ~30 min wall. Cheapest item ever put on this board. |
| **withdrawal condition, written before replay 1** | If `differential` = **0 of 14**, square 1's **detection** claim is withdrawn for this caller and task family, E-GATE-D does not run, and the gate is re-scoped in the same document to a **coverage/compliance** claim (Hill 4: 100% of `.clj` writes pass through it) — which is a **build result, not a vs-native square**. If additionally shape-refusals ≥ 2 of 14, the gate is **not merge-ready** and that is filed as a defect, not a result. |

### E-GATE-D — the paid half. **3 arm-runs. Conditional on E-GATE-R showing a live base rate, or on the planted fixture below.**

| | |
|---|---|
| **hypothesis** | On a task whose natural cheapest anchor produces a **false green**, native ships a patch that loads, passes the shipped suite, and passes `clj-kondo`, yet is semantically wrong — and the gate names it. |
| **what is planted** | 21 owners, each with the required change belonging **inside the `:else` branch of a `cond`**. The shipped `bin/fan-test` exercises only the first branch (147 assertions, all green either way). The cheapest `apply_patch` literal-context anchor is the **top of the fn**, one form above the branch. A held-out oracle suite the arm never sees exercises the `:else` path. Kondo is clean on both spellings — a misplaced guard is not a lint finding. |
| **load-immune meter** | Two binaries per arm, both replays over frozen bytes: **`FALSE_GREEN`** = (arm declared done) ∧ (shipped suite green) ∧ (kondo clean) ∧ (held-out oracle **red**); **`GATE_CATCH`** = `admit_clojure_patch`, fed that arm's exact frozen patch, returns a substantive refusal naming ≥1 wrong-anchor owner. Neither touches wall. |
| **the cheapest native route, left open and pre-registered as a live win (the E-PREWRITE clause)** | One `rg` for the branch keyword plus one windowed `sed -n` per owner — **exactly the E6-Q2 behaviour that withdrew square 3** (three tool calls, ~300 of 7,516 lines, 54/54). If native reads the `cond` before anchoring, `FALSE_GREEN` = 0/3 and that is a **behaviour**, not a definition. Stated before arm 1 so a third null cannot be re-narrated as "the trap was too obvious." |
| **n** | 3 native arms. (The gate replay on each is free; no tool arms — square 1's claim is about native's own route, so a T cell buys nothing.) |
| **predicted numbers** | `FALSE_GREEN` **1 of 3 (40%)**; 0 of 3 (**45%**); ≥2 of 3 (15%). Conditional on a false green existing, **`GATE_CATCH` on it: 55%** — a wrong-anchor change is invisible to kondo and to the focused suite, so the catch rests entirely on the untouched-node/owner-delta proof, which **has no declared intent to compare against on a native patch**. Joint P(≥1 arm where native false-greens **and** the gate catches it) ≈ **0.40 × 0.55 = 22%**. |
| **cost** | **3 arm-runs** + ~2 agent-hours to build the fixture and freeze the held-out oracle. |
| **withdrawal condition, written before arm 1** | `FALSE_GREEN` = **0 of 3** → **square 1 is withdrawn** on exactly the terms squares 3 and 4 were: a gate that only fires on defects this caller does not produce is insurance with no measured premium. `FALSE_GREEN` ≥ 1 **but** `GATE_CATCH` = 0 on every false green → **square 1 is withdrawn harder**: the defect exists, is exactly the class `vision.md` names ("a guard placed at the cheap top anchor instead of inside the branch"), and the gate missed it. Either way, the gate remains a **build** worth shipping for coverage; it stops being a square. |

**The uncomfortable thing I will say out loud so nobody sells it later:** 22% is the honest joint
probability that square 1 produces a win. I am recommending it anyway (§5, rank 2) **because the
free half costs zero arm-runs and settles the "is the gate just clj-kondo in a hat" question that
will otherwise be the first thing a reviewer asks.**

---

## 3. E-HARNESS (Bash-only native, no `apply_patch`)

### For

1. **E-AFFORD's own caveat names it, and it is the one manipulation shown capable of moving the
   mechanism.** 5 of 6 native arms invoked `sed`/`perl`/`awk` — every time as a *reader*; the one
   arm that used perl for the edit **printed a patch and fed it to `apply_patch`**. The write path
   is not a preference the prompt can move; it is a **route**. Removing the route is the only
   untried lever.
2. It is attack #3 on my own §21 list ("a second harness"), and a sceptic's first line is "your
   win is against a harness quirk."
3. Cheap: no new fixture, no new scorer, native arms only.

### Against

1. **It measures a harness nobody runs, and its result is unbankable.** Every real caller has
   `apply_patch` or `Edit`. E-AFFORD's own restatement is the argument against it: the claim is
   "weaker as a theorem and stronger as a product claim, since a product is sold against observed
   behaviour." A Bash-only agent is not observed behaviour; it is a laboratory animal.
2. **The direction is close to pre-computable, and it is the safe direction.** Strip `apply_patch`
   and the agent writes whole files with `cat > f <<'EOF'` heredocs — **more** characters than
   hunks, not fewer. Expected finding: native gets worse, the ratio goes **up**. A win we cannot
   quote, in the flank that cannot hurt us.
3. **It tests the wrong flank.** The harness that endangers 7.2× is not one with *no* write verb —
   it is one with a **better** write verb: a minimal-context `old_string`/`new_string` edit, which
   is what the second-largest coding harness in the world actually ships. Bash-only skips the
   dangerous flank and runs at the safe one.
4. Degenerate-arm risk: correctness collapses, arms fail the gate, and we score a null on a broken
   harness — the same shape as E-AFFORD's noise floor, at real arm cost.

### **COMMIT: DROP E-HARNESS as specified. RUN its dangerous twin, E-HARNESS-MIN.**

Argument 3 is decisive: the point of a harness cohort is to find where the win *stops*, and
Bash-only moves in the direction where it grows.

### E-HARNESS-MIN — design

| | |
|---|---|
| **hypothesis** | Native routes writes through `apply_patch` because **literal context is its correctness proof, not because it lacks alternatives.** Offer a write verb that supplies the *same* proof at a quarter of the characters — a literal single-occurrence replacement that **refuses on 0 or >1 matches** — and native's payload collapses toward the tool's. If it does **not**, the routing is habit and square 2 is safe against every harness, which is a stronger result than the ratio itself. |
| **the intervention** | A ~20-line `bin/edit <file> <old> <new>` pre-installed in the fixture (literal, unique-or-refuse — Claude Code `Edit` semantics reachable through Bash, no harness surgery, no custom MCP tool). Named in §5 alongside `apply_patch` with **no preference**, per E-AFFORD Amendment 1's discipline; the diff against the E-AFFORD Ns prompt is one paragraph and its sha is frozen. |
| **cells** | `N-min` (bin/edit available, k=1, n=3) and a **contemporaneous** `T` (n=2), interleaved. E-AFFORD's T is *not* the comparator — its own Amendment 1 proved a historical denominator overstated every ratio by 29%. |
| **load-immune meter** | Emitted write-payload chars + the **same content-keyed strategy classifier**, extended with a fourth class **`minimal-edit`** (a `bin/edit` invocation with no literal patch body). The new branch is **hand-driven before arm 1 on three spellings plus three negatives**, per `hand-drive-every-mode-you-ship` — E-AFFORD's `stream-edit` branch was the one that had never fired and it is why that classifier was trusted. Gate: 6/6 + `diff -r` byte-identical. No wall claimed. |
| **n** | 3 N-min + 2 T = **5 arm-runs**. |
| **predicted numbers** | P(≥2 of 3 arms actually use `bin/edit` for the majority of write chars) = **40%** — E-AFFORD's whole lesson is that naming a path does not move this caller. Conditional on adoption: native mean **3,200 chars** (band 2,000–5,500, 65%), ratio falls **7.2× → 2.0–4.0× (60%)**, still ≥1.5× (80%). Conditional on non-adoption (60% of the mass): native mean **6,500 ± 3,500**, ratio holds **4–8×**, and `apply_patch` routing is proven harness-independent. **0 stream-edit again: 85%.** |
| **withdrawal condition, before arm 1** | If ≥2 of 3 arms use `bin/edit` **and** the ratio falls **below 2.0×**, square 2's headline is restricted **in the same document** to *"harnesses whose write verb demands literal context"*, and the 7.2× is annotated in place with the minimal-edit number beside it. If ≥2 of 3 arms **decline** `bin/edit` and hand-type a patch anyway, **the harness flank is closed** — habit, not affordance, not capability — and no further write-path cohort runs. Both outcomes are decisions; that is the test that E-AFFORD failed. |

---

## 4. E-CEILING80 — predictions on record, before it lands

*N=80, k=1, 3 N + 2 T. Derived from: per-owner literal rate ~450 chars (E-REG cell means ÷ 21);
generated mode decomposed as ~1,950 fixed program + ~29/owner (from 2,556 at N=21); T flat across
k in both cohorts.*

| quantity | point prediction | band | p |
|---|---|---|---|
| **native, literal-patch arms** | **33,000 chars** | 26,000–42,000 | 65% |
| **native, programmatic arms** | **4,300 chars** | 3,000–6,500 | 60% |
| **strategy split (3 arms)** | **2 programmatic / 1 literal** | — | 40% modal (1P/2L 30%, 3P 20%, 0P 10%) |
| **≥1 programmatic arm in 3** | yes | — | **90%** (scale pushes toward generation) |
| **stream-edit arms** | **0 of 3** | — | 85% |
| **native mean (3 arms)** | **14,000 chars** | 5,000–23,000 | 55% |
| **native median (3 arms)** | **6,000 chars** | 4,000–33,000 (bimodal — the median is the honest statistic, the mean is not) | 50% |
| **≥1 native arm ≥ 25,000 chars** | yes | — | **60%** |
| **patch calls, literal arms** | **2–4** (mean 2.7) | — | 60% |
| **patch calls, programmatic arms** | **1** | — | 75% |
| **≥2 patch calls in ≥2 of 3 arms** | **no** | — | only **40%** — I am **lowering** my §21 figure of 60%, because bimodality was unknown then and the generated mode makes exactly one call |
| **native re-emission / patch rejection** | ≥1 across 3 arms | — | 50% |
| **T chars, mean of 2** | **1,050** | 600–1,800 | 75% |
| **T chars if `expected_files` enumerates 80 paths** | **~3,200** | — | 25% (the branch that would halve the ratio) |
| **T patch calls** | **1 committing call each** | — | 85% |
| **T first-call refusal** | **≥1 of 2 arms** | — | 65% (6/8 in E-REG, 2/3 in E-AFFORD) |
| **ratio on cell means** | **13×** | 5–30× | 60% |
| **ratio on medians** | **5.7×** | — | the number I would actually quote |
| **gate 5/5 green** | yes | — | 70% |
| **≥1 native arm fails 6/6 or byte-identity at N=80** | — | — | **30%** (native is 30-for-30 on correctness, but 80 owners is a new scale and the first place transcription volume could truncate) |

**The one number that matters:** if native's median lands near **4,300** rather than **33,000**,
the emission law is bounded and square 2's claim shrinks to mid-size fan-out — that is the
pre-registered withdrawal, and I expect it at roughly **45%**, which is much higher than the
"< 6,000 chars in ≥2 of 3" clause implied when it was written at 50/50 strategy odds. **Say so
now, before the table lands.**

---

## 5. The next three experiments, ranked

*Floors respected: no wall primary anywhere; every meter below is a count or a binary, replayable
over frozen bytes. **Total: 11 arm-runs.** All three are vs-native functional measurements —
launch mix stays 100% functional. Square 1 is **included** (rank 2). E-HARNESS as specified is
**excluded**; its redesign E-HARNESS-MIN is **included** (rank 3).*

### #1 — E-CALLER-K1: a second caller, which is also a second harness, for the price of one

| | |
|---|---|
| **hypothesis** | Every character in tonight's headline is `gpt-5.6-sol` at high reasoning effort writing `apply_patch`. A different caller arrives **with a different write verb** (minimal-context `Edit`/`MultiEdit` rather than literal-context hunks), so the two biggest holes in the claim — one caller, one harness — close in the same 6 arms. This is the only experiment on the board that can move the headline in the direction that costs us. |
| **load-immune meter** | Emitted write-payload chars + the content-keyed strategy classifier (with the `minimal-edit` class from §3, hand-driven before arm 1) + the 6/6 + `diff -r` byte-identity gate. Same fixture (`fanout-k1`, read-only), same task text, same churn band. |
| **n** | 3 native + 3 tool of the **second caller**, interleaved with each other; **no cross-caller ratio is quoted** — each caller is divided only by its own contemporaneous T. **6 arm-runs.** |
| **predicted numbers** | Second-caller native mean **3,800 chars** (band 1,500–9,000, 60%); its contemporaneous T **1,100 ± 500** (70%); ratio **3.5×** (band 1.3–8×, 55%); **P(ratio < 2.0×) = 30%** — the single most dangerous number in the program. Strategy: ≥2 of 3 arms **minimal-edit rather than literal-patch (65%)**; stream-edit **0 of 3 (80%)**. Correctness 3/3 (70%). |
| **cost** | **6 arm-runs**, no build beyond a driver swap and the classifier's fourth class; ~40 min. |
| **withdrawal condition, before arm 1** | If the second caller's ratio is **< 2.0× in ≥2 of 3 pairings**, square 2's claim is restricted **in the same document that reports it** to *"callers whose write verb demands literal context"*, and the 7.2×/5.7× figures are annotated in place, never re-quoted bare. If the second caller's **correctness** falls below 2 of 3 while the tool holds 3 of 3, that is a **new square-1-adjacent finding** and is reported as such rather than folded into square 2. |

### #2 — E-GATE-R, then conditionally E-GATE-D (square 1) — full design in §2

Ranked second only because it costs **zero arm-runs** and therefore cannot contend with #1 for the
box; in value-per-hour it is first. Summary line: **`differential` = 0 of 14 predicted at 85%**;
0 arm-runs; withdrawal = square 1's detection claim withdrawn and the gate re-scoped to a
coverage build. E-GATE-D (**3 arm-runs**, joint P(win) ≈ 22%) runs **only** if E-GATE-R shows a
non-zero differential, or on the planted false-green fixture with its withdrawal clause frozen
first.

### #3 — E-HARNESS-MIN (the `bin/edit` write path) — full design in §3

**5 arm-runs.** P(adoption) 40%; conditional ratio 2.0–4.0× at 60%; **both** outcomes are
pre-registered decisions (restrict the headline, or close the harness flank permanently).

### Explicitly excluded, and why

- **E-HARNESS as specified (Bash-only, no `apply_patch`)** — tests the flank that cannot hurt us
  and predicts native gets *worse*; unbankable because no shipped harness looks like that (§3).
- **A true zero-licence rung** (no scripted-edit permission at all) — E-AFFORD's runner is right:
  the licensed cells already decline, so the unlicensed one is arithmetic.
- **N = 5** — dropped permanently at 00:57Z; the two strategies are indistinguishable at ~2,000
  chars.
- **Any further routing / free-choice-exposure cohort** — adoption 0/19 on record; route without
  asking, or ride the gate.
- **E-EXTRACT** — still the best *correctness*-at-risk candidate and the only place native's
  accuracy has never been stressed, but it costs ~1.5 agent-hours of canonical freezing and
  E-CALLER buys more per arm. **File it as #4**, not dropped.
- **The first-call schema/description build** — a chore with a 4-arm acceptance witness after the
  build. On the claimable meter it buys ~0. Never reported as a cohort win.
- **One free non-cohort item worth an hour:** score **chars/s against load across all 25 existing
  arms**. Zero arm-runs, zero JVMs, and it is what converts every character claim into a time
  claim (§1). If chars/s proves load-sensitive, the conversion in §1 must be struck from the
  report — which is exactly why it should be checked before anyone makes it.

---

## 6. One sentence for Gene

Eight pre-registered cohorts tonight left **one** square standing and killed three: on the same
21-file rename, the verb commits in **549–1,214 characters** where the plain agent types
**1,869–10,090** — **7.2× and 5.7×** at the cell means, **1.5×** at the worst single pairing —
both byte-identical against the same frozen canonical, both 6/6, and the plain agent never once
let a stream editor touch a file in 14 write arms even when the prompt handed it `sed` and `perl`
by name; **the read side, the proof-before-write side, and the single-edit side are all withdrawn
because the plain agent did them as well or better** (54/54 structural probes in three calls, 3/3
planted blockers found with one `rg` and one sentence, zero characters written). **The one thing
that would change this sentence is a second caller** — every number in it is one model writing one
patch format, and a caller whose write verb needs no literal context could take the 7.2× down to
2×, which is why six of the next eleven arm-runs are pointed at exactly that.

---

*Written read-only; no code run, nothing edited, no Surgeon MCP tool called. Predictions above are
on record before E-CEILING80's table lands.*

**Coordinator note (01:27Z):** the floor correction is adopted — the worst arm-to-arm pairing is 1,869 ÷ 1,214 = **1.54×**, not 1.9× (which was min-native ÷ mean-T); every report from here quotes 1.54×. E-GATE-R (zero arm-runs: replay `admit_clojure_patch` over the 14 frozen native patches with the kondo and focused-suite predicates) launched immediately, together with the free chars/s-vs-load regression over the 25 existing arms. Sol's half pending before the ranking verdict.

## 30. Fleet poll on the E-AFFORD + E-PREWRITE pair — Sol, verbatim (01:28Z; written to the requested path this time — the sol-yolo fix holds)

# Fleet poll: E-AFFORD + E-PREWRITE

1. **What is left.** For this caller, clj-surgeon is for a mandated atomic fan-out write: one alias-migration intent replaces 21 owner-by-owner patch representations and lands the byte-canonical tree in one committing call, using 966 emitted characters against contemporaneous native cell means of 5,505 and 6,979—5.7× and 7.2× less.  The sceptic's sentence is: *“That is an `apply_patch`-harness result, not a structural-tool result; remove `apply_patch` and native will write a short Bash rewrite.”*  The receipts do **not** answer it: they show a strong behavioural prior—0 stream edits in 14 native arms, despite an explicit scripting licence, and even native's generated-patch mode averaged 2,556 characters, 2.6× the contemporaneous tool mean—but every arm still offered `apply_patch`; E-HARNESS is the missing intervention.

2. **Square 1 — run E-ADMIT-SEED, not another E-PREWRITE.**

   - **Task.** Freeze one real, same-caller `apply_patch`-format candidate with a subtle semantic regression and a minimally repaired clean twin. The arm is told to review and land the candidate only if it is correct, repairing it if necessary. Use three matched fixture variants of the same defect, not three unrelated tricks. T's ordinary write path is intercepted by the `admit_clojure_patch` hook; N retains ordinary `apply_patch`, Bash, and the same verifier but no Surgeon.
   - **Plant.** The bad candidate is parseable, applies cleanly, introduces no gross textual marker, and changes an early-return/control-flow relation so that an existing state reset is skipped—the previously observed stale-onset class. It must fail exactly one frozen focused behavioural assertion in a prospective overlay while passing parse and the cheap surface checks. The clean twin differs only by carrying the reset into the new guard. Before arm 1, hand-drive both twins: bad fails the named assertion; clean passes; the gate refuses bad with the typed class and admits clean; neither result may depend on patch grammar.
   - **Load-immune binary meter.** `PRELAND_CATCH=1` only when the exact bad candidate is presented, the bad post-image hash is never observed in the real `src/` tree at any write boundary, a diagnostic naming the planted invariant exists before the first real-tree source mutation, and the repaired final tree passes the arm-independent oracle. Capture every write-call payload plus before/after source hashes; a 250 ms hashwatch and exit ctime ledger are corroboration, not the sole witness. Report `BAD_EVER_LANDED` separately from final correctness, so “wrote bad, tested, reverted” is a failure even if the final tree is green.
   - **Cheapest native route.** One Bash call can create a disposable git worktree, apply `candidate.patch`, run the exact same focused verifier there, and then apply only the repaired patch to the real tree. That route is legal and stated in the native tooling block. Thus N's null is behavioural—whether this caller chooses a cheap staging route—not defined away. This differs from square 4: E-PREWRITE's correct result was “write nothing,” and a static blocker was visible to one `rg`; here a concrete candidate exists, the task requires a write, and the defect is established only in the prospective post-image.
   - **Design and n.** Four cells, interleaved: bad-T, bad-N, clean-T, clean-N; **n=3 per cell, 12 arm-runs**. The bad cells decide sensitivity; clean cells decide false-refusal cost. No wall or action claim.
   - **Prediction.** Bad-T **3/3 pre-land catches (90%)**; bad-N **0/3 (55%)**, 1/3 (30%), at least 2/3 (15%); clean-T **3/3 admitted without false refusal (80%)**; clean-N **3/3**; final oracle **12/12** after permitted repair. My point prediction is T 3/3 versus N 0/3, with 0/3 clean false refusals and 12/12 final correctness. Cost: **12 arm-runs**, about 2–3 hours of serialized arm time plus roughly one hour to freeze and validate the twins.
   - **Withdrawal before arm 1.** Square 1 wins only if bad-T is 3/3, bad-N is at most 1/3, clean-T false-refuses 0/3, and all final trees are green. **Withdraw square 1 for this caller if N pre-land-catches at least 2/3 bad candidates.** Treat any bad-T landing or any clean-T false refusal as a gate defect and make no product claim; treat failure to present the frozen candidate exactly as a void arm, not a miss.

3. **E-HARNESS — RUN.** The case for it is direct: E-AFFORD changed words and barely moved the mean, while all 14 native arms had the same dominant write affordance; removing that affordance is the only clean test of the sceptic's surviving causal story. The case against it is also real: Bash-only is coercion, not autonomous native choice; zero patch calls is true by construction; and a short direct rewrite may trade away safety, so characters without the common 6/6-plus-byte gate would be meaningless. I still run it because square 2 is the sole win and its harness confound is now more valuable to resolve than another prompt or N=21 replication.

   Design three interleaved N=21, k=1 cells: **P** = standard native with `apply_patch`; **B** = native with Bash as the only write-capable path, no `apply_patch` tool and no Surgeon; **T** = `alias_migration`; **n=3 each, 9 arm-runs**. Reads remain identical, B may use `sed -i`, Perl, Awk, or a short read/replace/write program, and the scorer must count the complete emitted input of every Bash call that can write. Primary meter: emitted write characters, content-based strategy, and the existing binary 6/6-plus-byte-identity gate; patch-call count is descriptive because B's zero is imposed. Before arm 1, hand-drive every counted Bash write form and prove the wrapper cannot invoke `apply_patch` indirectly.

   Point prediction: P **6,800 mean characters**, split 2 literal/1 generated; B **2,200 mean** (1,800/2,100/2,700), 3/3 direct-script or stream-write and 0 patch calls; T **1,000 mean**; therefore P/T **6.8×** and B/T **2.2×**, all 9 correct. I assign 65% to B/T remaining above 1.5× and 25% to B erasing the gap. **Withdrawal:** if B is 3/3 correct and B/T mean is below **1.5×**, withdraw the harness-general square-2 sentence and restrict the win to the current `apply_patch` harness; if at least 2/3 B arms fail the common correctness gate, the cohort does not answer efficiency and no character comparison is claimed; if B/T is at least **3×** with B 3/3 correct, close the harness flank for this caller.

4. **E-CEILING80 prediction on record.** I predict scale changes the strategy distribution, not that all three arms keep typing literal patches:

   | arm | emitted chars | patch/tool calls | strategy |
   |---|---:|---:|---|
   | N-1 | **4,800** | **1** patch | programmatic generation |
   | N-2 | **34,800** | **4** patches | literal patch, split to fit |
   | N-3 | **5,400** | **1** patch | programmatic generation |
   | T-1 | **1,050** | **1** committing tool call | tool |
   | T-2 | **1,250** | **1** committing tool call | tool |

   Native mean **15,000**, median **5,400**, total patch calls **6**, strategy split **1 literal / 2 generated / 0 stream**; T mean **1,150**; mean-character ratio **13.0×**. I put 60% on at least 2/3 native arms generating programmatically, 30% on 2/3 or more staying literal, and 10% on any stream edit. Thus I predict the pre-registered “≥25,000 in at least 2/3” line misses, while the N=80 emission win survives comfortably because even the generated representation grows with the 80-file table.

5. **Next three experiments, ranked.** E-CEILING80 is excluded because it is already running; first-call repair is excluded because it is a build acceptance witness, not a product experiment.

   1. **E-ADMIT-SEED (include square 1).** Hypothesis: an enforced prospective gate prevents a known-bad agent patch from ever entering the real tree, while optional native staging is usually skipped. Meter: `PRELAND_CATCH` plus clean false-refusal rate and final binary oracle. **n=12** across bad/clean × T/N. Prediction: bad **T 3/3, N 0/3**, clean false refusals **0/3**, final **12/12**. Cost: **12 arm-runs** plus about one fixture hour. Withdrawal: N at least **2/3**, any bad-T landing, or any clean-T false refusal; candidate-presentation failures are void.
   2. **E-HARNESS (include and run).** Hypothesis: removing `apply_patch` forces a compact native representation but does not fully erase the intent-call advantage. Meter: emitted write characters + content strategy, gated by byte identity. **n=9**: P/B/T ×3. Prediction: **6,800 / 2,200 / 1,000** mean characters, B/T **2.2×**, all 9 green. Cost: **9 arm-runs**. Withdrawal: a green B cell below **1.5×** T restricts square 2 to the apply-patch harness; two B correctness failures make the efficiency comparison unclaimable.
   3. **E-CALLER2.** Hypothesis: the N=21 fan-out character advantage survives a second caller under the identical standard harness, even if its native strategy mixture differs. Meter: emitted write characters and content strategy, with 6/6-plus-byte identity as a hard gate. **n=3 N + 3 T = 6 arm-runs**, k=1, interleaved. Prediction: native median **6,000**, T median **1,100**, mean ratio **at least 3×**, 6/6 correct; probability of clearing 3× **60%**. Cost: **6 arm-runs** plus no new fixture work. Withdrawal: if T is not 3/3 correct, no comparison; if both arms are correct and native/T is below **1.5×**, withdraw any cross-caller square-2 claim and label the current win `gpt-5.6-sol`-specific.

6. **Gene:** After eight cohorts, clj-surgeon is one measured product for this caller—a mandated 21-owner intent writer that emits **5.7×–7.2× fewer write characters at the cell mean (1.9× worst pairing) with byte-identical correctness**, while the known-site, read-question, and pre-write-proof squares are withdrawn—and the sentence changes if a 3/3-correct Bash-only native cell comes within **1.5×** of the tool.

## 31. Verdict on the second pair (Fable, 01:28Z) — agreements, disagreements, what launches

**Converged blind:** (a) square 2 restated as a behavioural, receipt-bound claim (Opus: 549–1,214 vs 1,869–10,090, 7.22×/5.70×, floor 1.54×; Sol: 966 vs 5,505/6,979); (b) the sceptic's surviving sentence is the HARNESS ("an apply_patch result, not a structural-tool result") and the receipts do not answer it; (c) a second caller is on both lists; (d) square 1 needs a design where a null is a behaviour (both cite the E-PREWRITE lesson); (e) E-CEILING80 predictions on record from both — both now expect the strategy distribution to move to generation at N=80 (Opus 2P/1L modal, native median ~6,000; Sol 1L/2P, median 5,400, mean 15,000, ratio 13×), i.e. both predict the pre-registered "≥ 25,000 in ≥ 2/3" line MISSES while the emission win survives.

| question | Opus | Sol | verdict |
|---|---|---|---|
| square 1 design | E-GATE-R: replay admit_clojure_patch over the 14 frozen native patches vs kondo + focused suite (0 arms; predicts differential 0/14 at 85%) → conditionally E-GATE-D, a planted false-green (3 arms, joint P(win) 22%) | E-ADMIT-SEED: a frozen bad candidate with a subtle control-flow regression + a clean twin; the task REQUIRES a write; native's cheap staging route (a disposable worktree) stated as legal; 4 cells × 3 = 12 arms; PRELAND_CATCH + BAD_EVER_LANDED | **E-GATE-R now (free, launched).** Then Sol's design trimmed: the two BAD cells first (bad-T ×3, bad-N ×3 = 6 arms); the clean cells only if bad-T catches ≥ 2/3 (false-refusal cost is meaningless for a gate that catches nothing). Sol's is the better square-1 design because the task demands a write, which is exactly what square 4 lacked. |
| harness | DROP Bash-only (safe flank, unbankable); RUN E-HARNESS-MIN: a `bin/edit old new` unique-or-refuse write verb (the Claude-Code Edit semantics) — the dangerous flank | RUN Bash-only (P/B/T ×3 = 9 arms; predicts B 2,200 chars, B/T 2.2×) | **One cohort, both flanks: E-HARNESS-2** = P (apply_patch, replication) ×2, B (Bash-only) ×3, M (bin/edit) ×3, T ×2 contemporaneous = 10 arms, interleaved; classifier gains `minimal-edit` hand-driven first; both seats' withdrawal clauses verbatim. Opus is right that M is the flank that can hurt; Sol is right that B is the sceptic's literal sentence. Ten arms buys both. |
| second caller | E-CALLER-K1, 6 arms, a caller whose write verb needs no literal context; P(ratio < 2×) = 30% "the most dangerous number in the program" | E-CALLER2, 6 arms, same harness | **Run after E-HARNESS-2** — it needs a second driver (claude -p with Edit); feasibility is checked by the E-HARNESS-2 runner as a side task (the M cell is the same write verb without the caller change, so the two cohorts share the classifier). |
| free item | chars/s vs load over the 25 existing arms (converts every char claim to a time claim if load-invariant) | — | **Folded into E-GATE-R** (zero arms, one JVM). |
| the floor | 1.54× arm-to-arm | 1.9× | 1.54× (Opus computed it from the table; adopted). |

**Order on the arm lock:** E-CEILING80 (running) → E-HARNESS-2 (10) → E-ADMIT-SEED bad cells (6) → E-CALLER (6, if a driver exists). E-GATE-R runs beside them (no lock). Functional share stays 100% of launched arm-runs.

## 32. Ninth result: E-CEILING80 (01:37Z) — all three withdrawal conditions fire; square 2 bounded to N ≲ 40

N 841 / 722 / 12,576 chars (3/3 programmatic generation, 0 literal — the literal stratum is EMPTY at N=80, so the §26 per-owner constant is not computable); T 1,094 / 1,508 (each paid one refused first call). Ratio 3.62× on means, 0.60× excluding the three-call arm; pairs 0.77× and 0.48×. W1 (≥2/3 native < 6,000) TRIGGERED → bounded to N ≲ 40; W2 (T not ≥5× in ≥2/3 pairs) TRIGGERED → no N=80 win; W3 (≥2/3 generated) TRIGGERED → linear default-policy mechanism withdrawn. Predictions on record: Opus (§29 §4) called ≥1 programmatic at 90%, median ~6,000, ratio 5.7× on medians, and put 45% on the bounded outcome; Sol (§30 §4) called 1L/2P, mean 15,000, ratio 13×. Both got the direction (strategy shifts to generation); neither predicted the inversion (native cheaper than the tool in 2/3).

**What it means.** N changed the KIND of native's output, not its size: at N=21 the caller flipped a coin between a hand-typed patch (~450 chars/owner) and a generator; at N=80 the coin stopped. The tool's character win therefore lives where native still types — mid-size fan-out — and the extrapolated 35,953-char prediction was a law quoted outside its domain. The runner's line: "a behavioural law measured at one problem size is a law with a domain, and the domain is the first thing to test." Still zero stream edits in 22 native arms; native routes every write, including generated ones, through apply_patch — the E-HARNESS-2 flank stays live.

**Standing.** Square 2: WIN, bounded (7.2×/5.7× at N=21, floor 1.54×; ≤ 1× at N=80). Square 1: E-GATE-R replay running. Withdrawn: single edit, 3, 4. Next on the lock: E-HARNESS-2 (running setup); then the boundary sweep E-NSWEEP (N ∈ {21, 30, 40, 55, 80}, n ≥ 4 native per N, scored on the strategy label, crossover = where the literal fraction crosses one half; ~20 native arms, no T needed — T is flat in N) is now ranked ABOVE E-ADMIT-SEED: knowing where the one win stops is worth more than a 22%-joint square-1 shot.

**Apparatus finding (runner):** sol-yolo writes `required = true` only when neutralising an existing .codex/config.toml; a fixture without one lets codex log transport failures against a dead port and complete a turn anyway (rc 0). A T arm whose server died after attestation would silently fall back to native (none did here: via_verb=2, native_apply_patch=0). Ratchet applied by the coordinator via atomic swap of the wrapper (running invocations keep the old inode).

## 33. Tenth and eleventh results: E-GATE-R (01:45Z) — square 1 detection WITHDRAWN (0/14 differential); the gate's kondo half was dead; chars/s is load-invariant per mode

E-GATE-R: 14 patches × 3 predicates; 294 hazards, every one class `note` (dead-require removal); kondo Δ0 in 14/14; fan-test 14/14; differential 0/14. P1/P2/P3 HIT. W1 fires: detection claim withdrawn, E-GATE-D does not run, the gate is a coverage build. W2 does not fire (0 shape refusals — the dual-grammar work holds). **Defect:** `exact-verification-visible-bytes = 12000` truncates the analyzer EDN → `:clj-kondo-unavailable` on every 21-file patch (k=1: 11,999 vs 12,043 bytes decided by a temp-dir suffix digit; k ≥ 2 always). The null survives only on a labelled post-hoc cap-lifted replay (ran true, 79 baseline, 0 introduced) corroborated by predicate 2. Filed inb-2f150d with the ratchet ladder; fix round launched. Lesson: a null from an instrument that never took a reading settles nothing — the replay's first finding was that the instrument was off.

Chars/s: H0 load-invariance supported over a 5× load range (native b −0.31, rho −0.04; tool b +0.77, rho +0.19). Per-mode rates: apply_patch native 143.1, literal 141.8, programmatic 156.3, tool 175.7. B3 MISS (three generated arms above 160) → the single native band is struck; conversions quote the mode's rate. Sanity: E-AFFORD's chars ÷ rate reproduces its measured gaps (3.1–6.9 s tool, 13.1–70.5 s native vs measured 3.3–7.0 and 12.3–71.2).

**Standing:** square 2 stands (bounded N ≲ 40; harness flank under test); square 1 = coverage build only; single-edit, 3, 4 withdrawn. E-ADMIT-SEED (Sol's square-1 design) is now moot for the detection claim — dropped; E-NSWEEP and E-HARNESS-2 carry the remaining questions.
