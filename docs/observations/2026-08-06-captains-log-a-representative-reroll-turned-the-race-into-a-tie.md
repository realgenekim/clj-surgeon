# Captain's Log: A Representative Reroll Turned the Race Into a Tie

**Date:** 2026-08-06

**Question:** How do we choose editing benchmarks that guide clj-surgeon
toward real comparative advantage instead of rewarding tasks selected because
the tool already won them?

## The unit of evidence

A commit is not enough. It records accepted bytes but usually loses the
decision the caller was given. A prompt is not enough. It records the goal but
not the exact accepted result. A tool transcript is not enough. It records
friction but selects for the route that happened to run.

The useful unit is:

```text
prompt + parent snapshot + accepted diff + verification contract
```

Sample goals and accepted changes first. Label the historical route afterward.
This avoids constructing a suite from successful Surgeon calls and then
mistaking tool-selection bias for product advantage.

## The frozen portfolio

The repo now contains five independent capsules:

| Task | Decision boundary | Why it belongs |
|---|---|---|
| Complete six-edit batch | The files, owners, old forms, replacements, and counts are supplied. | Tests whether one decision remains one transaction. |
| Owner-bounded edit | The owner and requested value change are known, but the full surrounding result is withheld. | Tests safe bounded inspection and preservation. |
| Dependency-aware move | The requested move is known; source relationships determine the safe closure. | Tests graph advantage rather than literal replacement. |
| Literal-source edit | The exact shorthand form and owner are known. | Tests whether `#()` and unrelated duplicates remain exact. |
| Native prose edit | A unique docstring sentence must change. | Gives native patching a task it should win. |

The earlier 45-form puzzle is not in the product portfolio. It was a useful
skill-amortization and transcript-stress experiment, but it is not
representative of ordinary edits.

Every capsule carries its prompt, before files, accepted after files, hashes,
provenance, expected work counts, and parser policy. A Babashka verifier checks
the complete on-disk file sets instead of trusting declared targets. Its pure
self-test covers singleton and multi-file success plus missing prompts,
provenance, targets, snapshots, counts, hashes, parser validity, and weakened
verification. Extra before or after files fail.

## The first pilot found a benchmark bug

The first two-lane run produced exact source in both lanes:

| Lane | Wall | Shell calls | Input tokens |
|---|---:|---:|---:|
| Current Surgeon skill | 73.973 s | 8 | 122,600 |
| Native control | 63.683 s | 5 | 113,068 |

Those numbers were not a valid product comparison. The copied task directory
was outside a Git repository. Both callers attempted normal Git verification.
Native received one status failure. The Surgeon caller's two-path `git diff`
degraded into a comparison of the two files, looked like corruption, and
provoked a needless correction attempt and reread.

The benchmark had changed the task environment in a way that punished normal
agent behavior. Exact-output scoring caught the final bytes, but only the
ethnographic command trace explained the extra actions.

The runner now initializes every isolated task copy as an independent clean
one-commit Git repository. The source repo remains hidden, while status and
diff behave as they do in real coding work. A permanent harness self-test
proves the initial worktree is clean. The policy lives in one Babashka
component used by both Codex and Claude harnesses; it is the first strangler
seam removed from the Bash runners.

## The corrected reroll

The same prompt, fixtures, current tool, model, and parallel two-lane schedule
then produced:

| Measure | Current Surgeon | Native control | Difference |
|---|---:|---:|---:|
| Exact accepted bytes | yes | yes | tied |
| Wall | 46.154 s | **45.777 s** | Surgeon +0.377 s |
| Input tokens | **65,501** | 74,710 | Surgeon −9,209 (−12.3%) |
| Uncached input | **10,205** | 11,478 | Surgeon −1,273 (−11.1%) |
| Output tokens | 1,593 | **1,462** | Surgeon +131 |
| Shell calls | 3 | **2** | Surgeon +1 |
| Native file changes | **0** | 1 | expected route difference |
| Source output | **2,194 bytes** | 3,477 bytes | Surgeon −36.9% |
| Failed mutations | 0 | 0 | tied |

The Surgeon route was the intended route:

```text
read skill
  -> one stdin :change! transaction for six owner-scoped edits
  -> one aggregate git diff
```

It created no temporary manifest. The transaction succeeded on the first
attempt, committed both files atomically, parsed and read back both whole
files, and returned an inverse receipt. The native route read bounded context,
made one two-file native change, and checked the aggregate diff.

## What the reroll means

The current result is a tie, not the desired win. It does not pass the
portfolio's five-second keep gate, and one roll is not a population estimate.
It does show three useful facts:

1. The transaction language can compile a heterogeneous six-edit decision in
   one successful source mutation without reading source first.
2. That route can match native patching while using less source context and
   fewer input tokens.
3. Native patching is a much stronger control when the benchmark supplies a
   normal Git worktree and enough cheap context for one patch.

The earlier 3.48× result remains valid for its narrower task: native first
attempted a context-free patch that refused, then paid to rediscover line
context. The new task does not reproduce that fumble. Together the results
locate the boundary better than either result alone. Surgeon wins big when
structural addresses replace missing line context; it merely ties when native
can acquire and apply stable context in one read.

## Next hill-climb

Do not optimize the parser for subsecond gains. The remaining opportunity is
agent ceremony:

- treat a successful whole-file `:change!` read-back receipt as verification
  and avoid a redundant source diff unless aggregate review is required;
- reduce or amortize the full skill read without leaking benchmark answers;
- run four counterbalanced repetitions before claiming a wall-time win;
- expand to all five strata and keep every per-task loss visible;
- add the same portfolio contract to the Claude adapter through shared
  Babashka components, not copied Bash.

The 1,300-line Codex runner is now itself an architectural signal. New shared
policy must enter through tested Babashka seams. Bash remains a thin adapter
for process launch, environment isolation, redirection, and signals. Migration
will use a strangler pattern with byte-identical receipts, not a wholesale
rewrite.

## Bottom line

Representative changesets come from real goals and accepted diffs, not from
favorite commands. Prompts recover the decision boundary. Parent snapshots
and accepted diffs provide the oracle. Tool traces explain friction after the
sample is chosen.

On the first valid representative reroll, clj-surgeon did not beat native
patching by 2–4×. It tied wall time, used 12.3% fewer input tokens, emitted
36.9% less source, and delivered stronger transactional proof. That is a solid
local optimum—and an honest starting line for the next hill climb.
