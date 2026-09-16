# clj-surgeon, read as an ethnographer reads a village

*Corpus: `/home/forge/src/clj-surgeon-records/docs/observations/`, 2026-03-28 → 2026-09-15. Every claim carries a dated file and a verbatim line.*

## 1. The arc of goals

The first log is the whole story in miniature: the tool moved a form correctly, then the agent hand-edited the declares — *"This is exactly the grep-read-edit loop we built clj-surgeon to eliminate"* (`2026-03-28-first-real-use.md`).

- **Aug 2–4 — editor → microkernel → oracle.** *"clj-surgeon is becoming the `ed`/REPL structural microkernel for Clojure"* (`...-the-file-became-a-structural-shell.md`); then, on blind aggregation at zero source bytes, *"the headline is not parity; it is editing without reading"* (`...-a-clean-claude-caller.md`).
- **Aug 5 — the goal becomes adoption.** Zero invocations across seven workers: *"The next product problem is activation, not another alias"*. The hinge: half the program after this is about getting agents to choose it.
- **Aug 6 — intent compiler.** *"The current tool is a strong structural microscope. The perfect tool is a structural intent compiler and transaction engine"* (`...-from-microscope-to-intent-transaction.md`). **Mover:** a naturalistic session used it as a microscope, then applied a broad native patch.
- **Aug 7–8 — gate, with a ceiling.** *"Do not build an autonomous refactoring oracle to remove the remaining model decision"*; *"the source-anchored semantic result authorized it"*.
- **Aug 24–27 — the tool is not the wall.** *"The product is not 'a Clojure IDE that the model must use'"* (`2026-08-24-ethnography-the-scalpel-must-earn-every-call.md`); then the number that should have ended the speed program — *"The surprising number was not 4.61x. It was less than one percent"*, *"Direct-wall to following-boundary ratio | 1 : 43.3"*.
- **Aug 29 — the value equation is written and ignored.** *"ADOPTION x PER-CALL GAIN = ACTUAL VALUE"* (`2026-08-29-...-six-designs-died...md`), in the same week as seven withdrawn figures.
- **Sep 2 — the reset.** *"Given the choice, the agent declines the tool, and it is right to"* (`...-the-big-aha-and-reset.md`). Gene: *"Pull 'Surgeon is available and expected' from every Clojure agent prompt today. It is a standing 2x tax with no measured return."* New shape: *"a structural gate on native patches"* whose *"value does not depend on the agent choosing to use it"* (`...-bridge-wall-clock-ideal-program.md`).
- **Sep 10 — the meter changes, on Gene's word.** *"hand-rolling these Python regex changes is insanity to me. Even if we're three times slower, we're still in seconds." … "Even though we're slower, I think it's better; it's less dangerous."* (`2026-09-10-ethnography-plan-of-record.md`). Restated: *"not speed, but a machine that knows the difference between text and a form, and a declared count that has to be true"* (`2026-09-10-python-vs-surgeon-form-edits.md`).
- **Sep 11–15 — the product becomes the practice.** *"A refusal is justified only if the same edit done natively would have produced a wrong result or a silent hazard"* (`2026-09-11-refusals-native-parity-rule.md`), plus a tower of gates, ships, fences, receipts, meters. By `2026-09-14-gene-report-48h.md` the verbs are barely mentioned.

**Six re-definitions in forty days, each correct about the failure it answered, each wider than the last. Only Sep 2 and Sep 10 shrank anything.**

## 2. How agents actually behaved

**Free choice is dead, cleanly.** 0/4, 0/3, 0/9, 0/10, 0/13, then *"**Free-choice adoption is now 0 of 19.** All nine arms — plated, bare, native — opened with `rg -n`"* … *"The tool was declined with its routing sentence in hand"* (`2026-09-03-captains-log-anvil-seat.md`). Earlier: skill visible in 17 Claude sessions, *"Sessions that loaded the skill 0"*, invocations 901 Codex / 1 Claude (`2026-08-26-three-day-progress-assessment.md`); census *"0.60% attempted write adoption and 0.34% accepted"*.

**And it is not a decision.** *"**PA2 never issued a single non-Bash action**: 22 Bash calls, zero ToolSearch, zero MCP… There is no quotable reason for going native because **no decision was made**"* (`2026-09-08-row2-fresh-seat-pilot.md`). Under a stronger treatment, *"**PB4 reproduced PA2's signature exactly**… It saw the definition and still made no decision."* Routing works only when the harness routes: plate + fetched schema 3/4; mandate 4/4; description 0.

**When it paid.** Whole-intent, many-owner, one call: split *"78 s vs native 447 s (÷5.7)"*, later ÷8–9, *"0 paper cuts on all four vs 32 on the hand-made split"* (`2026-09-07-plan-2-cellC-result.md`); fan-out 1.75×, alias 1.38× (`2026-09-06-strictly-better-evidence.md`); cross-caller extraction 6.37×–9.69×.

**When it cost.** *"the Surgeon arm cost 1.8x native wall and 2.1x the actions"*; mandated 2.7×. Pair-1 falsified the fan-out route: *"native 123.7 s vs tool 181.3 s = 0.68x"*, *"116.6 vs 197.8 = 0.59x"*, 8/8 correct both arms (`2026-09-07-pair-1-result.md`). Published losses beside the wins: typist 1.83×, admit gate 1.03×, extract 1.08×.

**Recurring failure shapes.**
- **String surgery, by the tool's own builders.** 1,570 sessions: *"6,124 program edits / 2,132 editor / 220 Surgeon"*; program edits *"broke the file 3.3× more often per verified edit"*; *"`git diff` read afterwards 252 (4.1%)"*; by model *"opus-5 90%, fable-5-1 100%, sol 3%"* (`2026-09-10-ethnography-program-edits.md`). Nearly every confirmed breakage sits in a `clj-surgeon-*` worktree.
- **False greens.** E4: python rewrote 31 `events/` hits where 2 were the alias; *"the file parses, formats idempotently, and lints identically to baseline while 19 URLs now point at `/ev/`. Every gate in the pipeline said green."* Surgeon refused in 5.9 ms and 157 ms — *"expected 31 matches, found 2."* Guards don't help: *"54% carry some `assert`… the guard checks how many strings matched, never whether the result is a form."*
- **Tool-in-the-way.** *"the Surgeon arm kept the whole native loop… and added 51 Surgeon calls that displaced almost nothing"*; *"Two thirds of every refusal the agents drew was the intent grammar itself"*; a refusal *"took longer than a third of the successful run"*.
- **Apparatus scoring itself.** *"Every Surgeon row before 05:30Z had called another seat's production server while the runner printed a sha it never read"*; *"the runner's own `.codex/` was scored against the agent."*

## 3. The human side

- **Rounds.** census 15, item-3 17, fold-diff 11, txn-journal 9, block-B *"nine ship rounds, twenty-six branch attempts and eighteen check-only prewarms"* (`2026-09-13-astra-sublime-index.md`). *"Round 4 accounting: 45 min wall for < 3 min machine work"*.
- **Briefs seeding defects.** *"Two builder briefs turned 'temp under X' into product paths"*; *"The regns branch needed six rounds: my brief's temp root, the fast-lane budget…"* (`2026-09-14-gene-report-48h.md`); Cell C's manifest `:declares` was wrong for all four hand arms.
- **Meters inflated then corrected.** Driver claimed 15 returns; watcher measured 35. *"Seven withdrawn figures. All seven relayed by this seat. All seven single-source. A single-source number is a hypothesis wearing a decimal point"* (`2026-08-29-...-what-the-six-withdrawals-mean.md`). Cohort I 1.81× → 1.75×; the real-session ethnography *"WITHDRAWN in full"*.
- **Self-inflicted stops.** *"nine self-inflicted stops… Every one of the nine stops last night cost the same forty minutes"*; *"roughly a third of builder and reviewer tokens re-read the same documents per round"* (`2026-09-11-the-system-from-the-drivers-seat.md`). Ceiling *"61.2% of actions apparatus"*; and *"(b) routine is the laggard in every one of the ten half-days"* — mean 3.1/10.

## 4. What the bitter lesson has already done here

**General method won, five times.** (1) Native `rg`+patch beat the structural loop at ~2×, chosen unprompted 19/19. (2) *"Native is an agent with scripts, parsers, compilers and tests… Our native controls batch all 21 files"* (`2026-09-05-astra-on-the-bitter-lesson.md`) — the straw-man native never existed. (3) The prompt lever is dead: 156 scored runs, six arms, Surgeon-final bytes 0/13 → at most 4/13, acceptance flat at 5–7/13 (`2026-09-11-gate-vs-prompt-report-claude.md`). (4) Sol 3% vs Opus 90% program share, same box, same week — a model trait that will move on its own. (5) Test **selection** lost to brute parallelism: *"cutting 94 to 45 namespaces removes 0.6 s while cutting 95 to 7 removes 22 s"*.

**Structure still won, three times — all consequence claims, none speed claims.** (1) The count contract (E4: the one place a real defect was stopped before bytes moved). (2) Whole-repository exact contracts: 141 owners, 87 caller sites, ÷5.7–÷9, zero paper cuts. (3) Ground truth at low latency: probe 662/1,170 ms vs cold JVM 3,525/4,210 (0.19×/0.28×), and *"PROBE 2/2 typed refusals versus NATIVE 0/2, where the latter exits successfully after running no tests."*

**The pattern: the bitter lesson eats every shortcut to a decision the model can make, and leaves every guarantee about a consequence the model cannot verify.**

## 5. Count-first

| Period | Believed to be | vs-native, measured | Free-choice adoption | Apparatus cost |
|---|---|---|---|---|
| 03→07 | declares-fixer, lenses | — | — | 38 production invocations total |
| 08-02→05 | shell → microkernel → activation | reads 0.96–1.84×; 27.8% fewer tokens | **0/7 workers, 0 invocations** | — |
| 08-06→12 | intent compiler / transaction | 3.48× best; 0.99× tie; 2.46× one-shot | skill loaded **0/6** Claude; 127 CLI calls, 0 x-rays | tool = **0.83–1.65%** of turn wall |
| 08-23→28 | structural camera + compiler | 0.48×–9.69×; forced arm **1.89× slower** | Codex **501** calls / Claude **0** | 70/396 calls refused (17.7%) |
| 08-29→31 | adoption × gain | 7.35× append; 6 designs dead | **0.60%** attempted write adoption | **7 figures withdrawn**; 21.2 h ceremony vs 94.4 s of work |
| 09-02→03 | gate on native patches | **1.84× wall, 2.12× actions** (slower) | **0/19**, all arms opened with `rg -n` | tool 3–4% of wall; 87% model time |
| 09-06→08 | strictly-better-or-native | +1.75×/+1.38×; −1.03×/−1.83×/−1.08×; split ÷5.7→÷9 | routed 3/4 & 3/4 with plate; 0/4 unrouted | **61.2%** actions apparatus; sublime 5.5→8.3, routine 3.1 |
| 09-10 | a count of forms, not strings | python 20–33 ms vs Surgeon 0.4–1.4 s | **220 Surgeon / 2,132 editor / 6,124 program** (28×) | 107 broken files (floor); **3 verbs with 0 calls** |
| 09-11→15 | the practice / native parity | probe **0.19×/0.28×**; selector 1.11–1.15 loss, 0.44/0.26 win | **0–4 of 13** finals, 156 runs; acceptance flat | 9 ship rounds, 26 branch attempts; **index 4, transfer 0/2** |

Rounds per landing where counted: **3–17**. Surgeon-executed refactors since 09-02: **58 accepted, ≥16 refused, none confirmed merged to any product trunk** (`2026-09-10-refactor-count.md`).

## 6. Three strategies

I would have ridiculed a Python heredoc rewriting Clojure too. The record made that expensive, because the heredocs are *ours*: 90% of Opus's edits, 100% of Fable's, and nearly every confirmed broken file in a `clj-surgeon-*` worktree. We built a scalpel and then did surgery with a bread knife — on the scalpel.

### A — The seatbelt: one contract, no editor
clj-surgeon stops being an editing interface and becomes a **write-admission contract on native patches**. Sol named the shape: *"a one-shot, `apply_patch`-compatible commit gate… I would use it on ordinary Clojure changes without first deciding to 'use Surgeon.'"*

**Shrink.** Discovery from twelve verbs to **four**: `admit_clojure_patch`, `edit_clojure` (`within`+`from`/`to` only — the zero-churn path, 3/3 lines), `alias_migration`, `namespace_split` under its frozen contract. Retire from discovery `transform_clojure`, `require_change`, `relation_census`, `feature_thread`, `helper_extraction`, `apply_clojure_changes` with a namespace owner, and the CLI wrapper — three of those recorded **zero calls in 1,570 sessions**, and the namespace-owner path re-prints 412 lines to change 5. Build the **one missing verb class** the census priced: insert/delete a form, repo-wide rename, `.edn`, create-ns — *"Those five account for 1,240 program edits that had nowhere else to go."* Delete the routing plates and every prompt sentence about the tool.

**Prediction.** Adoption stays near zero and stops mattering: a gate is reached by the harness, not chosen. The E4 class becomes unrepresentable. **The honest risk is that the yield is near zero too** — in 156 fresh runs *"neither caller ever attempted shell string-surgery on a task's Clojure source"*, and the 120-run cohort had *"ZERO silent breakages in any arm."* Damage is concentrated in long agentic sessions on this box: the population never measured. Second risk, observed: *"Confirmed the MCP path is blocked again by the same rf1 false refusal, so I'll close it out and rely on the mechanical fix instead"* — **refusal romanticism turns a seatbelt into a bypass.** The native-parity rule is the antidote, already written.

### B — Ground truth, not structure
Concede editing to the model; build what it cannot have: **the state of the running system, in a second.** Probe is the only thing in the record that beats a strong native control unmandated (0.19×/0.28×) and refuses where native exits 0 having run no tests.

**Shrink.** Retire the diff-impact selector (its own replay: *"native routing stays the default"*). Delete trunk/window/index; keep accepted-task wall, escaped-damage rate, recurrence. Collapse the gate to **one registration, one runner, one receipt**; cut the packet/envelope/ledger handoffs that re-carry the same evidence. Cap review at **one independent round plus one delta**, then park — nine-to-seventeen-round items are September's largest documented cost.

**Prediction.** Models keep closing the editing gap (Sol is at 3% program share with no tool); they will not start knowing what the test run just did. But probe's win is **against a cold JVM, not the best warm native entrance**, and *"Earlier evidence found a cold-bb probe much cheaper on a different, smaller task."* B's first act is to lose that comparison honestly, not defend 0.19×.

### C — The practice is the product
Freeze the verbs; extract the language-agnostic rails (typed refusals naming their native failure, receipts naming subject and evidence, encounter scoring, install gates) and prove them on a second codebase. **Prediction: not yet.** The daily meter's first real number is *"0 of 2 registered findings transferred — both recurred inside the fixes meant to close them"*; independence 0/10; fleet unknown. A kit extracted on that evidence is a kit nobody has used twice.

### Committed recommendation

**A as the product, B as the only funded measurement, C as the chronicle — and change the meter first, because the meter is the actual bug in this project's history.**

Forty days of speed apparatus were spent *after* the wall was shown to be 1–4% tool time and 87% model time, and *after* the value equation was written down. It kept happening because wall was the only number anyone could produce. Gene's Sep 10 ruling retired it; `2026-09-10-ethnography-plan-of-record.md` settled it — *"Wall is a diagnostic column, never the verdict."* The 48-hour report three days later still opens with a wall table.

This week: (1) publish **escaped-damage rate per 1,000 edits, by mechanism**, mined from the same session roots — the number A exists to move, already baselined (107 broken files, 1.7% of program edits, 3.3× the editor rate), and the only one in the record that cannot be argued with; (2) cut discovery to four verbs plus the insert class, delete the plates; (3) cap rounds at two; (4) run B's one honest re-measure against a warm native entrance. If escaped damage does not fall within two weeks **on this box** — where Fable writes 100% of its Clojure edits as programs — the seatbelt protects nobody and C is the graceful exit.

**Do not start a new block, a new verb, or a new meter until the damage number exists.** Every re-definition in this project's history was made in the absence of the number that would have settled it.
