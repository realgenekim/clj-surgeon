# Reshape clj-surgeon around where it wins (plan of record, 2026-11-22)

Epic: `clj-surgeon-98y`. Sources: the four-seat fleet review, the two Anvil handoffs, and the 2026-11-22 usage study in `docs/observations/`.

## Gene's words

- "1 surgeon: I think b or c?  Think critically about this, and activate brainfleet. Do we shrink down surgeon to match the winning moves?  I still think the :xray has to be helpful, and we should encourage it's use?  (That's a skill issue and house rules issue). We should do reshape where we choose to compete and win.  Come up with plan, along with ratcheting up to agent as orchestrator standard."
- "we have a world-class bar on testing, I think.  Make sure any code functionality we add, we match/exceed it."
- "Clj surgeon: wire up to tier 2!   Go go go !"
- "Clj sugeon: yes on plan; go on phase 1!  And maybe do the hardening first to make things safer and faster.  Right???  The hypothesis is that we need to create conditions for changes to be made sublime and fast and awesome (< 150 LoC changes, etc.)"

## Verdict

Four of four seats: reshape and continue. Shrink to the moves that remove agent actions, then build one verb
with a native control. 87% of wall is the model between actions; only fewer actions or fewer tokens read win.

| Question | fable | opus | astra | sol |
|---|---|---|---|---|
| `:xray` | Freeze; test it | Probation; computation over many forms only | Keep through its trial | Keep only if it passes |
| `split-ns!` | Keep; generate its manifest | Keep; the 63% is cleanup | Quarantine to its Cell C contract | Archive; restore on unseen wins |
| Plain form read (`cat`) | Delete | Keep, by form name | Keep one bounded form read | Delete |
| Next verb | add-require across files | add-require, if 6 of 10 are real | One verb, after the candidate check | add-require, if 6 of 12 share a contract |

## The finding that changes the order

The two winning verbs are not on main. Main has no namespace-split code. Namespace split (56 s against
447 s native) and alias migration (1.38 times) live on `astra/namespace-split`. Main is mostly the losers.

## The ten conditions

| # | Condition | Today | Issue |
|---|---|---|---|
| 1 | One main that is the tool, winners on it | No | `clj-surgeon-98y.1` |
| 2 | One canonical gate command, cold time known, same result three times | Not named, not timed | `clj-surgeon-98y.2` |
| 3 | A warm inner loop in seconds | Not measured here | `clj-surgeon-98y.3` |
| 4 | CI on every pull request; protection requires it and both reviews; proven red on a seeded failure | No workflow, no protection | `clj-surgeon-98y.4` |
| 5 | Complexity and duplication baseline, reported per change | None | `clj-surgeon-98y.5` |
| 6 | Testing bar written and measured per change | Written in CLAUDE.md today; not measured | `clj-surgeon-98y.6` |
| 7 | A native control harness | None reusable | `clj-surgeon-98y.7` |
| 8 | The change rules: 150 changed lines or fewer; one independent read before the first push; one push per review round; self-merge when green; after round 3, stop and write the design on one page for a fleet read | Not written | `clj-surgeon-98y.8` |
| 9 | Notice within a minute when a change fails or conflicts | None; needs Gene's word | `clj-surgeon-98y.9` |
| 10 | A weekly count of calls and fall-backs | One-off study script only | `clj-surgeon-98y.10` |

Anvil builder: 1 to 8 and 10. Condition 9: unassigned.

Other steps: xray experiment `.11`, fall-back label audit `.12`, winners onto main `.13` (Anvil builder);
phase 2 deletions `.14`, trigger-text pilot `.15`, add-require candidate audit `.16`, one new verb `.17`
(unassigned).

## The order

1. **Gate first.** Conditions 2, 4, 5, 6 (tier 2 handoff). Then 3, 8, 10 (phase 1).
2. **Read-only work.** The xray experiment (it builds condition 7) and the fall-back label audit.
3. **Winners onto main.** ONE fleet-reviewed pull request, the named exception to 150 lines. Not before the
   gate is green and protection is confirmed. This makes condition 1 true.
4. **Phase 2 deletions.** Only when all ten hold. Archive to a tag first. Nothing is deleted in phase 1.
5. **One new verb.** After the candidate audit, with a native control.

## The xray experiment

1. Twelve frozen tasks from two or more other repositories, each pinned at a commit.
2. Four lookups inside very large forms; four inventories over many forms; four named-form controls.
3. Three arms, fresh context, one pinned model, random order: native; plus `ls` and a form read; plus `ls` and `:xray :expr`.
4. Two repetitions: about 72 runs. Fall-back allowed and counted. Answers scored blind.
5. Measure actions, tokens read, input tokens including instructions, wall, correctness.
6. Promote xray for a task class only at 30% fewer tokens read, no more median actions, and no correctness loss against the better control. Wrong evidence stops it.

## Proposed trigger text for boot files

**Not installed. The xray sentence only if the experiment passes.** From the astra seat, section 4:

> "Use native tools by default. Use alias_migration for a matching repository-wide alias change. Use split-ns! only for its published admitted contract. Use xray only for a measured structural-query class when it replaces broader reading. Trust snapshot-bound receipts only for checks they actually ran. Repair one clear argument error; otherwise fall back."

The pilot (`.15`) randomizes this text against no text and charges its tokens on every task. It ships only
if total wall and input tokens fall at equal correctness.

## Kill criteria

| Phase | Stop when |
|---|---|
| Gate | Main not green in CI, or three cold runs disagree with no cause found. |
| xray experiment | Below the promotion rule. No encouragement; if the outline also fails, both leave in phase 2. |
| Winners onto main | The branch's suite or the Cell C receipt fails on the result. Winners stay recipes. |
| Phase 2 deletions | A surviving verb loses correctness, or a testing-bar ratio falls. |
| One new verb | Under 6 audited candidates share a contract; or, on 8 held-out native pairs, any correctness loss or under 25% median wall savings. |
| Field, four weeks after a trigger ships | Under 10 unprompted calls outside clj-surgeon repositories, or fall-back above 25%. |

**The two numbers that change the verdict to park:**

1. Net verified wall savings of 0% or less on the routing pilot, instruction tokens and failures included (astra, sol).
2. Fewer than 10 of the 48 whole-intent candidates (43 add-require, 5 rename namespace) are real by hand (opus).

## What we are deliberately not doing

- No architecture model before the shrink. A one-page CALM map comes after.
- Not fixing all five refusal classes. Three belong to operations phase 2 removes. Never weaken a valid safety refusal.
- No platform of our own: no orchestration or worktree-lifecycle code here.
