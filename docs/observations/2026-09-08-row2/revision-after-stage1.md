# Row 2 stage-1 revision to the frozen preregistration

**Status:** design amendment after stage 1 and before any stage-2 arm. This
document amends `/var/tmp/forge/plan2/cellC/sol-row2-design.md`; every provision
not changed below remains frozen. Stage 1 contained six fresh N controls and no
D observation. The history-isolated fixture, runner-owned O2–O5 grading,
qualification fixes, and O5 exclusion of `00SERVER-LOGS.txt` remain as recorded
in `/var/tmp/forge/row2/freeze.edn` and `results-stage1.edn`.

## Numbered changes

1. **Replace timing clause 3 with a removable-caller-work clause, while keeping
   proof-inclusive complete wall as the primary endpoint.** Define, for each
   matched pair, `N_i^A,D_i^A` as the original all-oracle wall from
   `orient-start` through `oracle-complete`, and `N_i^C,D_i^C` as caller work
   from `orient-start` through `candidate-complete`. Define `s_N^C` as the
   sample standard deviation of that caller-work endpoint in F1–F6. The amended
   primary Astra gate is conjunctive:

   1. all six D runs pass O1–O5 with no request repair, second call, operator
      intervention, or native fallback;
   2. `median_i(D_i^A / N_i^A) <= 0.75`;
   3. `median_i(1 - D_i^C / N_i^C) >= 0.50`; and
   4. `median_i(N_i^C - D_i^C) > 2 * s_N^C`.

   The registered secondary `median_i(D_i^A / N_i^A) <= 0.70` remains a
   reported complete-wall diagnostic, but is no longer described as necessarily
   stricter than the conjunction: on this floor, the new 50% caller-work clause
   is slightly more demanding.

   Rationale: the old clause 3, `median(N_i^A-D_i^A) >= 90 s`, required the tool
   to remove essentially all of the 94.5-second median removable work. The
   approximately 90.5 seconds of common suite, handoff, and runner grading
   cannot be removed by either route and cancels in the paired difference. A
   50% relative saving tests the same intended large effect on the work the
   route can change; the two-standard-deviation clause still requires the
   effect to clear the observed native floor. Keeping all-oracle wall primary
   preserves the user-facing complete-verified-task claim and prevents a fast
   mutation with slow proof from being called a win.

   The stage-1 `orient-start -> mutation-end` values are the retained
   caller-work observations because `mutation-end` froze the submitted
   candidate in F1–F6: 121.531, 122.959, 71.243, 79.909, 109.266, and 79.556
   seconds. Thus median native caller work is 94.588 seconds, `s_N^C` is 23.173
   seconds, and `2*s_N^C` is 46.346 seconds. The following are calibration
   thresholds from stage-1 medians, not substitutes for the preregistered
   pairwise statistics:

   | Clause | Stage-1 calibration | Implied D threshold |
   |---|---:|---:|
   | 2: complete-wall ratio `<= 0.75` | `0.75 * 198.1` | `D^A <= 148.575 s`, approximately `D^C <= 58.075 s` if the 90.5-second apparatus is stationary |
   | 3: caller-work saving `>= 50%` | `0.50 * 94.588` | `D^C <= 47.294 s` |
   | 4: caller-work saving `> 2*s_N^C` | `94.588 - 46.346` | `D^C < 48.242 s` |
   | Secondary: complete-wall ratio `<= 0.70` | `0.70 * 198.1` | `D^A <= 138.670 s`, approximately `D^C <= 48.170 s` if apparatus is stationary |

   At the stage-1 medians clause 3 binds: the timing conjunction requires D
   caller work at or below 47.294 seconds, in addition to the actual paired
   complete-wall ratio and acceptance clauses. Median components are not
   additive, so the apparatus-subtracted figures are explanatory estimates
   only; stage 2 computes every clause from each pair's stamped endpoints.

2. **Fix alias occupancy at planning time, and retain the six stage-1 controls.**
   Add exactly this sentence to the arm task after the ordered alias list:
   "Alias occupancy is evaluated at planning time against each namespace before
   any migration edit, so an alias occupied by the old library remains
   unavailable even though that require will be retired."

   Rationale: F2–F5 treated retirement as freeing `json`; F1 and F6, and the O4
   prose, treated the pre-migration namespace as authoritative. The new
   sentence makes those readings converge without leaking the expected output
   alias. F1–F6 remain valid native floor controls and do not need to be rerun:
   every run migrated all 21 sites, left zero live old sites, preserved the
   protected docstring, had kondo delta zero, and passed the suite, and the
   ambiguity did not change their work or wall. Their original 1/6 acceptance
   is retained as a stage-1 apparatus finding, not presented as acceptance
   under the amended task. Stage 2 uses only fresh paired N and D arms with the
   amended task hash.

3. **Make O2 alias-convention-neutral and remove the historical-output
   confound.** O2 continues to enforce the exact changed-path set, 21-to-zero
   live-site closure, unchanged protected/unowned forms, and one permissible
   target libspec. For each touched form it now compares a Clojure token stream:
   target-alias tokens in the target libspec and corresponding live qualified
   symbols are classified and omitted from the golden equality comparison;
   every non-alias token must be byte-identical and occur in the same order.
   Layout trivia is not an O2 byte-identity subject. O4 alone decides whether
   the candidate alias obeys the ordered policy and the planning-time occupancy
   rule. O2 therefore accepts every policy-consistent alias and cannot award a
   result merely because it reproduces the historical `mjson` spelling.

   Rationale: golden commit `5a4b4bcf` is the tool's own earlier
   `alias_migration` output. Its `{"mjson" 9}` histogram is useful provenance,
   but it is not an independent quality oracle. Comparing all non-alias tokens
   retains the golden's closure and non-interference value while moving alias
   judgment to the arm-independent policy oracle. The unchanged O5 rubric may
   still report or reject avoidable whitespace/layout churn; it no longer
   reaches that result through an alias-convention-biased O2 gate.

4. **Do not exclude the approximately 90-second apparatus from the primary
   clock; add a prospective boundary instead of subtracting it.** Beginning
   with stage 2, stamp `candidate-complete` immediately after the caller's last
   allowed inspection, mutation, or rework, once candidate bytes are frozen,
   and before runner handoff, repository load/suite, or O2–O5 grading. No source
   mutation is permitted after that stamp. Continue the same monotonic clock
   through `oracle-complete`.

   Rationale: validation and handoff are fixed with respect to this treatment,
   but they are still part of delivered complete-task latency and can fail or
   drift. Removing them from the primary would change the claim from "faster
   complete verified task" to "faster candidate production" and would make the
   0.75 ratio easier merely by changing the denominator. The tech tree's meter
   definition for this and later rows is therefore two prospective endpoints,
   always reported together: (1) **primary complete verified wall**,
   `orient-start -> oracle-complete`; and (2) **removable caller work**,
   `orient-start -> candidate-complete`. Never infer caller work by subtracting
   separately aggregated phase medians. A row may place a preregistered effect
   clause on the caller-work endpoint only when the post-candidate apparatus is
   identical across arms; acceptance and the primary complete-wall result
   remain mandatory.

## Amended one-screen cohort table

| Stage | IDs | Count/order | Status and freshness | Primary complete-wall endpoint | Paired caller-work endpoint |
|---|---|---:|---|---|---|
| Native floor | F1–F6 | 6 N | complete; retained, no rerun | recorded `orient-start -> oracle-complete` | retained `orient-start -> mutation-end` candidate freeze |
| Pair 1 | P1-N, P1-D | N->D | fresh/fresh, sequential | `orient-start -> oracle-complete` | `orient-start -> candidate-complete` |
| Pair 2 | P2-D, P2-N | D->N | fresh/fresh, sequential | same | same |
| Pair 3 | P3-N, P3-D | N->D | fresh/fresh, sequential | same | same |
| Pair 4 | P4-D, P4-N | D->N | fresh/fresh, sequential | same | same |
| Pair 5 | P5-N, P5-D | N->D | fresh/fresh, sequential | same | same |
| Pair 6 | P6-D, P6-N | D->N | fresh/fresh, sequential | same | same |
| **Total** | F1–F6 plus P1–P6 | **18 attempts: 12 N, 6 D** | no overlap; one history-isolated seed | all O1–O5 complete | candidate bytes frozen before apparatus |

## Amended exact `task.md` received byte-for-byte by every stage-2 arm

```markdown
# Task: centralize JSON serialization

The destination function `marvin-voice-remote.json/write` already exists and
preserves the arguments and result of `clojure.data.json/write-str`.

Migrate every live Clojure reference under `src` from
`clojure.data.json/write-str` to `marvin-voice-remote.json/write`, excluding
`src/marvin_voice_remote/json.clj` itself. There are exactly 21 live sites in
exactly 9 caller files.

For the target namespace, use this ordered alias preference:

1. `json`
2. `mjson`
3. `m-json`
4. `json-policy`

Alias occupancy is evaluated at planning time against each namespace before
any migration edit, so an alias occupied by the old library remains unavailable
even though that require will be retired.

In each namespace, reuse a preferred alias already bound to the target
namespace; otherwise choose the first preferred alias not already bound in that
namespace. Do not shadow or replace an alias bound to another namespace. Retire
the old require when its final live use is gone, and leave exactly one effective
target libspec.

Do not rewrite strings, docstrings, comments, reader-discarded forms, metadata,
or unrelated code. In particular, prose that mentions `json/write-str` is
historical explanation and must remain unchanged. Do not modify the destination
helper, tests, configuration, documentation, or files outside the nine callers.

Finish only when the repository load and full suite pass and the submitted diff
contains only the intended minimal migration.
```

## Stage-2 go/no-go

**GO.** Retain F1–F6 as the frozen native floor. Before launching P1, freeze the
amended design/task hashes, add and qualify the prospective
`candidate-complete` stamp, and qualify the revised token-based O2 against at
least the golden candidate, a policy-consistent alternate-alias fixture, an
off-policy alias fixture, a non-alias token change, and an untouched seed.
Those are apparatus conformance checks, not replacement control arms. Once they
pass, run the six counterbalanced matched pairs under the amended conjunctive
gate above; every original stopping and hard-kill condition remains in force.
