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
