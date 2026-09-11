# Pre-registration: does a gate move the agent's hand where a prompt does not? (2026-09-11T02:59Z)

Gene: "Wow? Wut?? Really? Do some experiments. Prove your thesis."

## Thesis under test
Fable's claim after the cohort (2026-09-11-cohort-report-v2.md): prompts (house rules, skills, routing plates) do not change which mechanism an agent uses to edit Clojure; a refusal with its repair at the tool boundary does. Evidence so far is correlational (plate arm AC: Opus took the Surgeon route in 3/12 tasks, Sonnet 0/12; census: require_change 0 calls in 6,124 edits; 2026-09-03 routing-plate cohort 0/19).

## Design (same frozen apparatus, same 12 tasks + E4 as task 13, same two callers, same 900 s deadline, HOOKS=arm)
Three new arms, each = the AC prompt bundle (exception line + plate) plus ONE mechanism; controls are the cohort's existing B and AC runs (not re-run):
- **H (hook-refuse):** a PreToolUse hook on Bash that refuses any command writing a .clj/.cljc/.cljs/.edn file with sed/perl/awk/python/heredoc/redirect, exit 2, with a typed message that names the verb and a request-file example filled in for the file and form the command targeted. Nothing else changes. Tests: a refusal that carries its repair moves the hand.
- **G (gate):** a PostToolUse hook on Edit/Write for .clj files that runs admit_clojure_patch (the existing commit gate) on the resulting bytes and, on a refusal (form broken, unintended owner changed), reverts the write and returns the refusal text. No hook on Bash. Tests: whether a gate catches damage without changing the hand.
- **S (skill):** AC plus the clj-surgeon skill installed project-scoped (.claude/skills/clj-surgeon) plus the house-rule line "Clojure files are forms, never strings" in the project CLAUDE.md. No hooks. Tests Gene's question directly: do skills/house rules move it.
13 tasks x 3 arms x 2 callers = 78 scored runs; plus 6 identical calibration runs on H (E4 task, Sonnet), excluded.

## Predictions, written before any run
| arm | Opus Surgeon final-mechanism | Sonnet Surgeon final-mechanism | program share | oracle pass | accepted |
|---|---|---|---|---|---|
| AC (control, already measured) | 25% (3/12) | 0% (0/12) | Opus 0%, Sonnet 0% | 71% | 46% |
| H | ≥ 50% | ≥ 25% | 0% both (refused at the boundary) | within ±10 pp of AC | within ±10 pp of AC |
| G | ≈ AC (no change; the gate is after the hand) | ≈ AC | ≈ AC | ≈ AC; catches the E4 mistaken request if any run makes it | ≈ AC |
| S | ≈ AC (within calibration noise, ±1 task) | ≈ AC | ≈ AC | ≈ AC | ≈ AC |

Falsification: if H does not raise Sonnet's Surgeon use above 0/13 or Opus's above 3/12, the thesis is wrong for that caller and the answer to Gene is "prompts, hooks, neither; the model decides". If S moves either caller by more than the calibration spread, the thesis is wrong the other way and skills/house rules are the lever. Wall and refusal counts (warranted/unwarranted, UNKNOWN unless adjudicated) reported beside; a hook refusal that leaves the task unfinished counts as noncompletion, not as a win.

## What this cannot show
Fresh sessions only; the census's habit lives in long sessions. n=13 per cell: report counts and intervals, no p-values. The hook text is Fable's; a better-worded refusal could do better, a worse one worse — the arm tests the mechanism class, not the wording.
