# Cohort report — programs to contracts

120 scored runs (5 arms x 2 callers x 12 tasks), plus 12 calibration runs excluded from the score.

## 1. Accepted completion — AC − B, per caller (equal T/R/I weight)

Accepted = the held-out oracle passed, the caller finished inside the deadline, **and the gate went green**. Family-weighted because the draw is 4/4/4 by construction. `AC+/B−` counts tasks AC accepted and B did not, and `AC−/B+` the reverse: every arm ran the same twelve tasks, so the discordant pairs are the signal and an unpaired difference throws away the pairing the design bought.

Two gate definitions, side by side, because the first one could not answer for most of the cohort:

- **v1 — the frozen gate.** 8 of the 12 frozen `:gate` commands do not run at their own `:sha` on the untouched tree, so their runs are `unrunnable`: **UNKNOWN, never accepted**. 44 runs that finished with a passing oracle are unaccepted under v1 for that reason alone.
- **v2 — the curator's replacement gate**, run after the fact on a reconstruction of each run's final tree. Its time is recorded separately and is **not** part of the complete wall in section 3, which was measured with the frozen gate.

Tasks whose frozen gate is unrunnable at `:sha` (8): `I-01-mission-cli-stale`, `I-02-mission-cli-admitted-profiles`, `I-03-heap-sample-retention`, `I-04-mission-display-decision-view`, `R-01-core-discovery-require-edn`, `R-02-recovery-require-measured`, `T-01-outline-memory-spec-marker`, `T-02-study-row-line-range`

Tasks rescored with gate-v2 so far: **none — v2 is not yet available**

### Acceptance, v1 — frozen gate

| caller | B | A | C | AC | N | AC − B | AC+/B− | AC−/B+ |
|---|---|---|---|---|---|---|---|---|
| claude-opus-5 | 25% (3/12) | 25% (3/12) | 25% (3/12) | 25% (3/12) | 25% (3/12) | +0 pp | 0 | 0 |
| claude-sonnet-5 | 25% (3/12) | 25% (3/12) | 25% (3/12) | 25% (3/12) | 25% (3/12) | +0 pp | 0 | 0 |

### Acceptance, v2 — replacement gate

| caller | B | A | C | AC | N | AC − B | AC+/B− | AC−/B+ |
|---|---|---|---|---|---|---|---|---|
| claude-opus-5 | 25% (3/12) | 25% (3/12) | 25% (3/12) | 25% (3/12) | 25% (3/12) | +0 pp | 0 | 0 |
| claude-sonnet-5 | 25% (3/12) | 25% (3/12) | 25% (3/12) | 25% (3/12) | 25% (3/12) | +0 pp | 0 | 0 |

### Per arm, pooled — and the oracle on its own

| arm | n | oracle passed & finished | accepted v1 | accepted v2 | v1 UNKNOWN (gate unrunnable) |
|---|---:|---|---|---|---:|
| B | 24 | 67% (16/24) [47%-82%] | 25% (6/24) [12%-45%] | 25% (6/24) [12%-45%] | 8 |
| A | 24 | 71% (17/24) [51%-85%] | 25% (6/24) [12%-45%] | 25% (6/24) [12%-45%] | 9 |
| C | 24 | 67% (16/24) [47%-82%] | 25% (6/24) [12%-45%] | 25% (6/24) [12%-45%] | 8 |
| AC | 24 | 71% (17/24) [51%-85%] | 25% (6/24) [12%-45%] | 25% (6/24) [12%-45%] | 9 |
| N | 24 | 75% (18/24) [55%-88%] | 25% (6/24) [12%-45%] | 25% (6/24) [12%-45%] | 10 |

## 2. Oracle-wrong with the gates GREEN — the silent-wrong count

The gate is scored as **no NEW failures** against a baseline captured on the untouched tree at each task's `:sha`; a gate that cannot run at `:sha` is `unrunnable` and is never counted as green. A run in the first column is a wrong file that the repository's own tests blessed.

| arm | caller | oracle-wrong & gate GREEN | oracle-wrong (all) | gate unrunnable | gate red |
|---|---|---:|---|---:|---:|
| B | claude-opus-5 | **0** | 33% (4/12) [14%-61%] | 8 | 1 |
| B | claude-sonnet-5 | **0** | 33% (4/12) [14%-61%] | 8 | 1 |
| A | claude-opus-5 | **0** | 33% (4/12) [14%-61%] | 8 | 1 |
| A | claude-sonnet-5 | **0** | 25% (3/12) [9%-53%] | 8 | 1 |
| C | claude-opus-5 | **0** | 25% (3/12) [9%-53%] | 8 | 1 |
| C | claude-sonnet-5 | **0** | 42% (5/12) [19%-68%] | 8 | 1 |
| AC | claude-opus-5 | **0** | 25% (3/12) [9%-53%] | 8 | 1 |
| AC | claude-sonnet-5 | **0** | 33% (4/12) [14%-61%] | 8 | 1 |
| N | claude-opus-5 | **0** | 25% (3/12) [9%-53%] | 8 | 1 |
| N | claude-sonnet-5 | **0** | 25% (3/12) [9%-53%] | 8 | 1 |

## 3. Complete wall, including gates

A fast wrong answer is not a fast answer, so failed-run time is a separate column, and a run killed at the 900 s deadline is in neither (it is a missing measurement, not a slow one). The wall here is the run as it was actually measured: caller + the FROZEN gate. **The gate-v2 column is not part of it** — that pass happened afterwards, on a reconstruction, and folding it in would be a different measurement wearing this one's name.

| arm | caller | median wall, oracle-PASSED (s) | median wall, FAILED (s) | median gate (s) | gate-v2 (s, NOT in wall) | timeouts | caller errors |
|---|---|---:|---:|---:|---:|---:|---:|
| B | claude-opus-5 | 37.8 | 101.4 | 8.1 | - | 0 | 0 |
| B | claude-sonnet-5 | 26.1 | 122.7 | 7.8 | - | 0 | 0 |
| A | claude-opus-5 | 33.3 | 102.7 | 7.1 | - | 0 | 0 |
| A | claude-sonnet-5 | 33.7 | 87.7 | 8.1 | - | 0 | 0 |
| C | claude-opus-5 | 37.9 | 123.1 | 7.0 | - | 0 | 0 |
| C | claude-sonnet-5 | 23.3 | 104.9 | 7.9 | - | 0 | 0 |
| AC | claude-opus-5 | 50.3 | 112.4 | 7.1 | - | 0 | 0 |
| AC | claude-sonnet-5 | 29.2 | 152.5 | 8.2 | - | 0 | 0 |
| N | claude-opus-5 | 32.4 | 71.4 | 7.5 | - | 0 | 0 |
| N | claude-sonnet-5 | 29.2 | 112.0 | 7.3 | - | 0 | 0 |

## 4. Method — sequence, task-level program use, census-compatible call share

Three different questions, deliberately not merged. **Final mechanism** is what produced the bytes the oracle judged. **Task-level program use** is whether a program touched the target at any point in the run — the run-level question the session census could not ask. **Call share** counts one call as one unit, the census's own rule, so the two corpora are comparable.

| arm | caller | final: program | final: editor | final: surgeon | wrote nothing | task-level program use | call share program |
|---|---|---|---|---|---:|---|---|
| B | claude-opus-5 | 17% (2/12) [5%-45%] | 83% (10/12) [55%-95%] | 0% (0/12) [0%-24%] | 0 | 17% (2/12) [5%-45%] | 18% (3/17) |
| B | claude-sonnet-5 | 0% (0/12) [0%-24%] | 100% (12/12) [76%-100%] | 0% (0/12) [0%-24%] | 0 | 0% (0/12) [0%-24%] | 0% (0/17) |
| A | claude-opus-5 | 25% (3/12) [9%-53%] | 75% (9/12) [47%-91%] | 0% (0/12) [0%-24%] | 0 | 25% (3/12) [9%-53%] | 21% (4/19) |
| A | claude-sonnet-5 | 0% (0/12) [0%-24%] | 100% (12/12) [76%-100%] | 0% (0/12) [0%-24%] | 0 | 0% (0/12) [0%-24%] | 0% (0/14) |
| C | claude-opus-5 | 25% (3/12) [9%-53%] | 58% (7/12) [32%-81%] | 17% (2/12) [5%-45%] | 0 | 33% (4/12) [14%-61%] | 21% (4/19) |
| C | claude-sonnet-5 | 0% (0/12) [0%-24%] | 100% (12/12) [76%-100%] | 0% (0/12) [0%-24%] | 0 | 0% (0/12) [0%-24%] | 0% (0/15) |
| AC | claude-opus-5 | 0% (0/12) [0%-24%] | 75% (9/12) [47%-91%] | 25% (3/12) [9%-53%] | 0 | 0% (0/12) [0%-24%] | 0% (0/17) |
| AC | claude-sonnet-5 | 0% (0/12) [0%-24%] | 100% (12/12) [76%-100%] | 0% (0/12) [0%-24%] | 0 | 0% (0/12) [0%-24%] | 0% (0/15) |
| N | claude-opus-5 | 8% (1/12) [1%-35%] | 92% (11/12) [65%-99%] | 0% (0/12) [0%-24%] | 0 | 8% (1/12) [1%-35%] | 6% (1/16) |
| N | claude-sonnet-5 | 0% (0/12) [0%-24%] | 100% (12/12) [76%-100%] | 0% (0/12) [0%-24%] | 0 | 0% (0/12) [0%-24%] | 0% (0/15) |

## 5. Refusals, diff-read, unintended changes, tokens

Refusals are **typed only**: a Surgeon refusal payload, or the caller declining with nothing written. A shell command that exits non-zero is a tool error and is counted separately — it is not policy behaviour. Warrant is **UNKNOWN** for every refusal: whether one was warranted depends on what the refused request would have produced, and that was never executed.

| arm | caller | typed refusals (w/u/UNKNOWN) | tool errors | diff read | files touched >1 | median turns | est. cost/run |
|---|---|---|---:|---|---:|---:|---:|
| B | claude-opus-5 | 0/0/**0** | 1 | 92% (11/12) [65%-99%] | 0 | 10 | $0.30 |
| B | claude-sonnet-5 | 0/0/**0** | 3 | 50% (6/12) [25%-75%] | 0 | 12 | $0.21 |
| A | claude-opus-5 | 0/0/**0** | 1 | 92% (11/12) [65%-99%] | 0 | 10 | $0.33 |
| A | claude-sonnet-5 | 0/0/**0** | 2 | 50% (6/12) [25%-75%] | 0 | 10 | $0.17 |
| C | claude-opus-5 | 0/0/**0** | 0 | 75% (9/12) [47%-91%] | 0 | 9 | $0.38 |
| C | claude-sonnet-5 | 0/0/**0** | 2 | 50% (6/12) [25%-75%] | 0 | 10 | $0.18 |
| AC | claude-opus-5 | 0/0/**0** | 1 | 100% (12/12) [76%-100%] | 0 | 12 | $0.41 |
| AC | claude-sonnet-5 | 0/0/**0** | 1 | 50% (6/12) [25%-75%] | 0 | 12 | $0.18 |
| N | claude-opus-5 | 0/0/**0** | 0 | 100% (12/12) [76%-100%] | 0 | 8 | $0.33 |
| N | claude-sonnet-5 | 0/0/**0** | 0 | 100% (12/12) [76%-100%] | 0 | 10 | $0.19 |

## Calibration — 12 identical runs, arm N, excluded from the 120

| caller | n | accepted | final mechanism | wall median (s) | wall min–max (s) |
|---|---:|---|---|---:|---|
| claude-opus-5 | 6 | 6/6 | editor 5, program 1 | 33.5 | 30.8–37.7 |
| claude-sonnet-5 | 6 | 6/6 | editor 6 | 22.1 | 17.7–28.3 |

Identical inputs, identical arm: this is the apparatus's own noise floor. Any arm-to-arm difference smaller than this spread is not a finding.

## Harness attestation

- **Arms**: the five frozen bundles, sha256 verified against `clj-surgeon-records .../2026-09-10-cohort/arms.sha256` (5/5 OK) before the first run. Each is written verbatim as the worktree's `CLAUDE.md`; the task text only ever arrives on the command line.
- **Tasks**: 108/108 frozen digests verified against `tasks.sha256` (records `8cb6ec43`), and re-verified after the gate baselines were written.
- **Plan**: 12 balanced blocks from the published seed (`curation/seed.txt`), block order and within-block order both shuffled; regenerate with `lib/plan_cohort.py` to check it.
- **MCP servers seen at init across all runs**: `clj-surgeon=connected`
- **Hooks fired per run**: 0 (HOOKS=drop)
- **Model echoed back at init**: `claude-opus-5`, `claude-sonnet-5`
- **Auth**: subscription (`apiKeySource: none`); no metered API key was used.
- **Memory**: `--setting-sources project`, so the only memory in context is the arm; each run records its own `:memory-paths` as the evidence.

