# Gate vs prompt — does a refusal at the tool boundary move the hand?

Pre-registration `145ef7d2`, amendment 1 `772f93e8`. 156 scored runs (6 arms x 2 callers x 13 tasks), plus 12 excluded calibration runs and the delivery probes below.

## 0. Delivery — was each intervention actually installed?

Every run fires a no-op probe through its own installed hook binary and records the hash of what was installed. A hook that is present but unreachable and a hook that is absent are indistinguishable from a transcript, so neither is inferred.

| arm | runs | hooks installed (same hash) | probe returned OK | skills offered | notes |
|---|---:|---|---|---:|---|
| AC | 26 | none | n/a | 21,30 |  |
| N | 26 | none | n/a | 21,30 |  |
| H0 | 26 | yes (9e60fe9469b82977) | 26/26 | 21,30 | hook hash 9e60fe9469b82977 |
| H | 26 | yes (f408e30a6de0fb34) | 26/26 | 21,30 | hook hash f408e30a6de0fb34 |
| G | 26 | yes (8dfeb8146b9f7b17) | 26/26 | 21,30 | hook hash 8dfeb8146b9f7b17 |
| S | 26 | none | n/a | 21,31 | skill REPLACES the one the repo already ships at every sha |

## 1. Exposure — did the hook ever fire?

Astra's registered bet: the H/H0 hook fires in **0-2 tasks per caller**. A zero here means *the deployed hook did not reach this caller on these tasks*; it never means *the model decides*.

| arm | caller | tasks where the hook fired | total refusals | blocked attempts |
|---|---|---|---:|---:|
| H0 | claude-opus-5 | **1/13** | 2 | 2 |
| H0 | claude-sonnet-5 | **0/13** | 0 | 0 |
| H | claude-opus-5 | **2/13** | 2 | 2 |
| H | claude-sonnet-5 | **0/13** | 0 | 0 |
| G | claude-opus-5 | **0/13** | 0 | 0 |
| G | claude-sonnet-5 | **0/13** | 0 | 0 |

## 2. Primary estimand — H − H0, tasks whose final bytes Surgeon produced

H and H0 share one recognizer, one scope, one exit code and one log line. The only difference is the words the caller sees: H0 says `not run`, H adds the verb and a request filled in from the attempted command and the source.

| caller | H0 Surgeon finals | H Surgeon finals | H − H0 | H0 accepted | H accepted | H − H0 accepted |
|---|---|---|---:|---|---|---:|
| claude-opus-5 | 2/13 | 1/13 | **-1** | 6/13 | 6/13 | **+0** |
| claude-sonnet-5 | 1/13 | 0/13 | **-1** | 6/13 | 6/13 | **+0** |

Astra's practical-repair bar: H must add **≥2 accepted Surgeon completions** over H0 without losing more than one accepted completion.

## 3. The bettor table — both forecasts, beside the measurement

| arm | caller | Fable predicted | Astra predicted | MEASURED Surgeon finals | held? |
|---|---|---|---|---|---|
| AC | opus-5 | 3/13 | 3 | **3/13** | Astra yes |
| AC | sonnet-5 | 0/13 | 0 | **1/13** | Astra yes |
| N | opus-5 | – | 0 | **0/13** | Astra yes |
| N | sonnet-5 | – | 0 | **0/13** | Astra yes |
| H0 | opus-5 | – | 3 | **2/13** | Astra yes |
| H0 | sonnet-5 | – | 0 | **1/13** | Astra yes |
| H | opus-5 | ≥7/13 | 4 | **1/13** | Astra no |
| H | sonnet-5 | ≥4/13 | 0 | **0/13** | Astra yes |
| G | opus-5 | ≈AC | 3 | **4/13** | Astra yes |
| G | sonnet-5 | ≈AC | 0 | **1/13** | Astra yes |
| S | opus-5 | ≈AC ±1 | 3 | **2/13** | Astra yes |
| S | sonnet-5 | ≈AC ±1 | 0 | **1/13** | Astra yes |

## 4. Mechanism and quality, all arms

| arm | caller | Surgeon final | editor final | program final | wrote nothing | oracle passed | accepted (gate-v2) | median caller wall (s) | median complete wall (s) |
|---|---|---|---|---|---:|---|---|---:|---:|
| AC | opus-5 | 3/13 | 7/13 | 3/13 | 0 | 9/13 | 6/13 | 71 | 71 |
| AC | sonnet-5 | 1/13 | 11/13 | 0/13 | 1 | 10/13 | 7/13 | 44 | 44 |
| N | opus-5 | 0/13 | 10/13 | 3/13 | 0 | 10/13 | 7/13 | 39 | 39 |
| N | sonnet-5 | 0/13 | 13/13 | 0/13 | 0 | 10/13 | 6/13 | 27 | 27 |
| H0 | opus-5 | 2/13 | 10/13 | 1/13 | 0 | 10/13 | 6/13 | 103 | 103 |
| H0 | sonnet-5 | 1/13 | 12/13 | 0/13 | 0 | 10/13 | 6/13 | 63 | 63 |
| H | opus-5 | 1/13 | 9/13 | 3/13 | 0 | 10/13 | 6/13 | 72 | 72 |
| H | sonnet-5 | 0/13 | 13/13 | 0/13 | 0 | 8/13 | 6/13 | 53 | 53 |
| G | opus-5 | 4/13 | 6/13 | 3/13 | 0 | 9/13 | 6/13 | 75 | 75 |
| G | sonnet-5 | 1/13 | 12/13 | 0/13 | 0 | 10/13 | 6/13 | 56 | 56 |
| S | opus-5 | 2/13 | 6/13 | 4/13 | 1 | 10/13 | 6/13 | 86 | 86 |
| S | sonnet-5 | 1/13 | 12/13 | 0/13 | 0 | 9/13 | 5/13 | 37 | 37 |

## 5. Every hook refusal, and what the caller did next

`used-the-verb` / `used-the-editor` / `program-again` / `gave-up` is read from the first surviving write after the refusal. A blocked call is an attempt, never a write.

| run | arm | caller | refusals | after the refusal | final mechanism | accepted |
|---|---|---|---:|---|---|---|
| `I-01-mission-cli-stale__H__claude-opus-5__r1` | H | opus-5 | 1 | program-again | program | no |
| `I-02-mission-cli-admitted-profiles__H__claude-opus-5__r1` | H | opus-5 | 1 | program-again | program | no |
| `I-02-mission-cli-admitted-profiles__H0__claude-opus-5__r1` | H0 | opus-5 | 2 | already-edited-before-refusal | editor | no |

### Blind audit of every exposure

All three refusals are on **scratch fixture files under a temp directory** (`$d/a.clj` from `mktemp -d`, `/tmp/i02-fx/src/a/empty.clj`) that the caller was building to exercise a function -- not on the task's source file. The recognizer confines itself to the workspace, but a path held in an unresolved shell variable (`$d/a.clj`) cannot be placed, and it refuses when it cannot tell. That is the conservative direction, and it is still a false positive for this experiment's purpose: **in 156 runs neither caller ever attempted shell string-surgery on a task's Clojure source.** The intervention H and H0 exist to catch was never attempted, so H - H0 measures the effect of a message delivered only against fixture-building, and the conditional repair mechanism remains unidentified. Astra's exposure bet (0-2 tasks per caller) holds; the reason it holds is that the behaviour the hook targets did not occur.

### The refused commands, verbatim (for blind audit)

- `I-01-mission-cli-stale__H__claude-opus-5__r1` (pre-bash-string-surgery): `~/bin/clj-kondo --lint src/clj_surgeon/mission_cli.clj 2>&1 | tail -5; git diff --stat; d=$(mktemp -d); printf 'a' > $d/a.clj; printf 'b' > $d/b.clj; timeout 30`
- `I-02-mission-cli-admitted-profiles__H0__claude-opus-5__r1` (pre-bash-string-surgery): `~/bin/clj-kondo --lint src/clj_surgeon/mission_cli.clj 2>&1 | tail -5; git diff --stat; d=$(mktemp -d); mkdir -p $d/src/a; : > $d/src/a/empty.clj; echo '(ns a.f`
- `I-02-mission-cli-admitted-profiles__H0__claude-opus-5__r1` (pre-bash-string-surgery): `~/bin/clj-kondo --lint src/clj_surgeon/mission_cli.clj 2>&1 | tail -5; git diff --stat; d=/tmp/i02-fx; rm -rf $d; mkdir -p $d/src/a/dir.clj; : > $d/src/a/empty.`
- `I-02-mission-cli-admitted-profiles__H__claude-opus-5__r1` (pre-bash-string-surgery): `T=$(mktemp -d) && mkdir -p $T/src/a && : > $T/src/a/empty.clj && echo '(ns a.real)' > $T/src/a/real.clj && mkdir $T/src/a/dir && timeout 300 clojure -M -e " (re`

## 6. Arm G — the admission gate

**Preflight, before any caller ran** (`lib/g_preflight.log`): E4's known-wrong candidate and its correct candidate were replayed through the same admission call the hook makes.

> FINDING: the detector does NOT reject E4's known-wrong candidate (ADMITTED).

| caller | runs | writes seen | candidates rejected | restorations verified | unknown/conflicted |
|---|---:|---:|---:|---:|---:|
| claude-opus-5 | 13 | 11 | 0 | 0 | 0 |
| claude-sonnet-5 | 13 | 15 | 0 | 0 | 0 |

A rejected candidate is an **attempted defect** even when the restoration succeeded: the write happened, and the ledger keeps it countable.

## 7. Arm S — installed, offered, invoked, used

| count | value | meaning |
|---|---:|---|
| installed | 26/26 | the arm wrote the skill into the worktree |
| offered | 26/26 | the session's own init event listed it |
| invoked | 0/26 | the caller actually called the Skill tool |
| used | 3/26 | Surgeon produced the final bytes |

Preflight (`lib/s_preflight.log`): the skill is installed and offered, and its body loads when forced by name in a sacrificial session. Forcing never happens in a scored run -- an invocation the apparatus caused would answer its own question.

## 8. Calibration — 12 identical N runs on T-03, excluded from the score

| caller | n | accepted | Surgeon finals | caller wall median (s) | min-max (s) |
|---|---:|---|---|---:|---|
| claude-opus-5 | 6 | 6/6 | 0/6 | 94 | 84-119 |
| claude-sonnet-5 | 6 | 6/6 | 0/6 | 214 | 152-333 |

Identical task, identical arm, identical flags. This is the apparatus's noise floor in wall; it is **not** an equivalence margin for adoption across 13 heterogeneous tasks, and it is not borrowed as one.

## 9. Did each prediction hold?

- **AC**: Surgeon finals opus-5 3/13; sonnet-5 1/13.
- **N**: Surgeon finals opus-5 0/13; sonnet-5 0/13.
- **H0**: Surgeon finals opus-5 2/13; sonnet-5 1/13.
- **H**: Surgeon finals opus-5 1/13; sonnet-5 0/13.
- **G**: Surgeon finals opus-5 4/13; sonnet-5 1/13.
- **S**: Surgeon finals opus-5 2/13; sonnet-5 1/13.

## What this cannot show

Fresh sessions only; the census habit lives in long ones. Thirteen tasks per cell: counts and intervals, no p-values. The hook wording is one wording, not the class. A null result rules out only the registered delivered intervention on this task set.
