# Pre-registration: long-context replication of the program-writing habit (Claude side) — 2026-09-12T03:06Z

Gene: "Go on next stage of plan." The cohort and the gate-vs-prompt experiment ran fresh sessions and found no habit to move (2026-09-11-cohort-report-v2.md; 2026-09-11-gate-vs-prompt-report-claude.md). The census found the habit in LONG sessions with the shell-preference harness line (2026-09-10-ethnography-program-edits.md: Opus 90%, Sonnet 17%). This experiment asks whether the habit appears as a session lengthens.

## Design
One session per cell: the 13 frozen tasks (2026-09-10-cohort/tasks + example-R-e4) delivered in sequence inside a single `claude -p` session on one worktree at the base sha, each task applied to the previous result; oracles held out per task and scored on the frozen first submission of each; gate-v2 per task. Callers claude-sonnet-5 and claude-opus-5. Arms: B (the attested morning prompt INCLUDING the shell-preference harness line the census sessions carried), AC (exception + plate, no harness line), H (AC + the refusing hook with repair; hook exposure recorded per task). 3 × 2 = 6 sessions × 13 tasks = 78 task outcomes; one control session per caller with the 13 tasks in a different seeded order under B. Meter per task POSITION (bins 1–4, 5–9, 10–13): program share, oracle-wrong with gates green, hook exposure (H), context length at task start, complete wall per task.

## Fable's bets (registered before Astra's, before any run)
| arm × caller | program share 1–4 | 5–9 | 10–13 | oracle-wrong & gates-green (of 13) | H hook fires |
|---|---|---|---|---|---|
| B Opus | 25% | 45% | 60% | 1 | – |
| B Sonnet | 0% | 10% | 25% | 0 | – |
| AC Opus | 0% | 10% | 25% | 0 | – |
| AC Sonnet | 0% | 0% | 10% | 0 | – |
| H Opus | 0% | 5% | 10% | 0 | 2–4, all in bins 2–3 |
| H Sonnet | 0% | 0% | 0% | 0 | 0–1 |
Falsification: if B Opus program share in bin 10–13 stays under 25%, the census habit is not a length effect and the harness line alone explains it (test: the B arm without the line, one extra session). If AC Opus bin 10–13 exceeds B's, the plate is worse than nothing at length. Astra's bets are appended as an amendment before the first run; the registry is frozen at that point.

## What this cannot show
n = 1 session per cell; order effects are controlled by one reordered session per caller only; oracle carry-over between tasks is bounded by the frozen expected outputs but a wrong earlier edit can make a later task's oracle unreachable (scored as unknown, never pass); context overflow ends the session (recorded as noncompletion from that position).

## Amendment 1 (2026-09-12T03:09Z) — Astra's bets appended; registry FROZEN here, before any session
Astra (2026-09-12-longctx-astra-bets.md): B Opus 25/40/50%, AC Opus 5/10/20%, H Opus 0/5/10%; B Sonnet 0/5/15%, AC Sonnet 0/0/5%, H Sonnet 0/0/0%; oracle-wrong-with-gates-green: B Opus 1, all others 0; H hook exposure Opus 0/1/1 per bin, Sonnet 0/0/0. Its decision bars are descriptive (six sessions are six units; 78 task outcomes are not 78 replications). Its attacks, adopted: position confounds task identity, learning and context pressure — the reordered-B control is kept at one session per caller (two total, as budgeted) and the tasks' fixed order is published; B's instruction text is hashed and quoted in the report so "the harness line" is a known byte string, not a memory; oracle carry-over is scored unknown, never pass; context overflow is noncompletion from that position. Astra's least-sure: whether thirteen bounded tasks supply enough retained context to change Opus's route at all — its late 50% is the number it expects to revise downward. Fable's least-sure is the same number from the other side (60%).
