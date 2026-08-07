# Representative edit portfolio

**Status:** Implemented

## Outcome

Create one repo-owned benchmark that measures complete clean-agent editing
workflows against representative Clojure changes. Each benchmark task is a
frozen capsule:

```text
task contract + before files + accepted after files + verification gate
```

The benchmark must compare the current clj-surgeon route with its strongest
credible alternative. It must not depend on another source repository, a live
application, or retained private transcripts. One `make` target must run the
portfolio. A separate self-test must verify fixture integrity, scoring,
isolation, and scheduling without a model call.

## Selection method

Neither commits nor prompts are sufficient alone.

- The prompt defines the judgment and information available to the caller.
- The parent snapshot defines the source the caller saw.
- The accepted diff is the byte-level correctness oracle.
- Tests or a parser/linter command prove that the accepted result works.
- The bounded tool trace reveals reads, retries, refusals, fallbacks, and the
  point at which one decision became several mechanical actions.

Candidate tasks are sampled by completed Clojure goal before their editor route
is considered. Each candidate is then labeled as native one-shot, Surgeon
one-shot, repeated Surgeon, Surgeon-to-native fallback, or refusal/recovery.
This prevents selecting only traces that already favor clj-surgeon.

The first portfolio freezes five task strata found in repository history and
recent anonymized usage studies:

| Task | Decision boundary | Why it belongs | Expected strong route |
|---|---|---|---|
| `decision-batch-edit` | Complete decision supplied | Six heterogeneous edits across two files test whether one model decision stays one transaction. Duplicate syntax makes structural owner scope material. | One `:change!`; native may need context. |
| `pair-view-expect-edit` | Owner and semantic change supplied; exact surrounding form withheld | Tests the common one-owner read/edit loop and the audit-payload preservation trap. | At most one bounded read and one guarded edit. |
| `dependency-move-edit` | Relationship determines the safe change | A valid declared program becomes invalid if one form moves without its dependency. Exact replacement cannot substitute for graph evidence. | `:mv` refusal followed by the named `:mv-with-deps` remedy, or direct explicit move-with-deps when the task supplies that consent. |
| `literal-source-edit` | Complete literal decision supplied | Reproduces the field failure where `#()` became `fn*`; exact source spelling is part of correctness. | One guarded structural edit or one native patch. |
| `native-text-edit` | Unique prose-only change supplied | Prevents the benchmark from rewarding maximal Surgeon adoption. | One native patch; Surgeon should abstain. |

The 45-form comprehension puzzle remains a synthetic skill-amortization and
transcript-capacity stress test. It is not part of this product portfolio.

## Frozen-capsule contract

Each task lives under `bench/fixtures/edit_portfolio/TASK/` and contains:

- `before/`: the complete starting workspace subset;
- `after/`: the exact accepted files;
- `task.txt`: the tool-neutral user goal;
- `capsule.edn`: decision boundary, source provenance, expected target files,
  and verification policy.

Provenance is intentionally non-sensitive. It records a public issue or local
commit when available, a prompt SHA-256 when the raw prompt cannot be retained,
and a short description of how the fixture was minimized. The capsule contains
all bytes required to run; provenance is evidence, never a runtime dependency.

A verifier must refuse duplicate task IDs, missing files, unexpected after-only
targets, invalid Clojure in either snapshot, unchanged tasks, mismatched declared
hashes, and absent verification policy.

## Treatments

The benchmark supports an explicit run matrix rather than taking a Cartesian
product of incompatible contexts:

```text
pre:matched-skill post:matched-skill native:no-skill
```

- `pre` is the tagged local microscope baseline when the task is compatible.
- `post` is the current commit or an explicit `WORKTREE` snapshot.
- `native` exposes no clj-surgeon executable or skill.

Every lane receives the same `task.txt`. Treatment-specific instructions come
only from the installed skill and available executable. `WORKTREE` snapshots
copy only repo-owned runtime, skill, and fixture inputs into the isolated setup;
the benchmark workspace remains outside repository ancestry. Each workspace is
initialized as an independent clean Git repository so ordinary status, diff,
and preservation checks behave as they do in real coding work.

## Metrics

Correctness and preservation gate every efficiency number. Record:

- complete task wall and final cumulative model tokens;
- exact before/after match for every target file;
- shell and native file-change actions;
- Surgeon reads, plans, applies, refusals, and help calls;
- source-bearing action count and source output bytes;
- failed mutation attempts;
- whether one successful `:change!` materialized the supplied multi-edit plan;
- temporary manifest creation or native patching before `:change!`;
- post-decision rereads (all source reads are post-decision for tasks whose
  decision is fully supplied);
- verification and receipt evidence.

Report per-task results and a portfolio median. Never hide a losing stratum in
an aggregate. Incorrect runs are excluded from efficiency medians but remain in
correctness rates.

## Safety invariants

- Native controls must not see a clj-surgeon executable or skill.
- Every run starts from freshly copied before files.
- A task is correct only when all target bytes equal the accepted after files.
- Extra source files, missing target files, or partial multi-file edits fail.
- No benchmark route may skip the task's parser, lint, or test gate.
- Raw events, prompts, final answers, and workspaces follow the existing
  archive-and-retain policy and are never tracked in Git.
- Existing benchmark tasks and result readers remain backward compatible.

## Test plan

### Pure fixture matrix

- valid singleton and multi-file capsules;
- duplicate ID;
- missing before or after file;
- unexpected after-only file;
- unchanged before/after set;
- invalid Clojure before and after;
- missing or wrong SHA-256;
- missing decision boundary or verification policy.

### Harness boundary

- explicit run-matrix parsing and invalid cell refusal;
- `WORKTREE` materialization without `.git`, ignored results, or unrelated
  workspace files;
- independent clean Git initialization for every task workspace;
- multi-file start/final hashing and exact scoring;
- native binary and skill isolation;
- complete terminal receipt after success and failure;
- retention self-test with the new result fields.

### Real invocation

Run `decision-batch-edit` once in current-Surgeon and native lanes. Both must be
exact before expanding to the full portfolio. The result is a pilot, not a
population claim.

## Keep gates

The supplied-decision stratum is successful when correct current-Surgeon runs:

- use one source mutation transaction;
- perform zero source reads;
- perform zero post-decision rereads and zero failed mutation attempts;
- create no temporary manifest through native patching;
- beat both the local microscope baseline and native control by at least five
  seconds median over four counterbalanced replicates.

The complete portfolio is successful only when no native-favored stratum loses
correctness and the tool-neutral portfolio median improves. The native text
control should remain native; using Surgeon there is not an adoption win.

## Bitter-Lesson boundary

The benchmark supplies goals and accepted outcomes. It does not encode which
tool should win, infer architecture, or reward a specific command. It measures
whether general structural perception and guarded transactions let a stronger
model materialize its own decision with less bookkeeping. New task strata are
added only from observed repeated work, not from imagined refactoring catalogs.

## Documentation and release checklist

- Add `make benchmark-edit-portfolio` and
  `make benchmark-edit-portfolio-self-test` to `make help`.
- Document cost, matrix overrides, `WORKTREE`, retention, and interpretation in
  `bench/README.md`.
- Add the portfolio to `docs/plans/README.md`.
- Do not update user-facing clj-surgeon help or skills merely to teach a
  benchmark answer.

## Queued infrastructure migration

`bench/run_clean_codex.sh` crossed 1,300 lines while this portfolio was added.
Its scheduler, score model, matrix, hashes, and event classification are now
data-heavy logic expressed through Bash, `jq`, `awk`, and positional TSV
columns. After the portfolio pilot, migrate it to Babashka with parity first:

1. freeze schedule, fixture, result-row, summary, lock, resume, and retention
   golden tests around the shell runner;
2. move pure matrix expansion, task metadata, event scoring, and summaries to
   Clojure data functions;
3. keep process spawning, filesystem isolation, and signals in a thin shell;
4. run both implementations over identical synthetic event streams and require
   byte-identical structured receipts;
5. switch the `make` entrance only after a paid one-task A/B produces the same
   task workspace, score, terminal state, and retained evidence.

The shared seam must serve both `run_clean_codex.sh` and
`run_clean_claude.sh`. Caller invocation and telemetry decoding may remain in
separate adapters; task catalogs, fixture rules, scoring, retention, and
receipt policy may not be copied between them.

The first strangler seam is implemented in
`bench/initialize_benchmark_workspace.clj`. Both shell harnesses call the same
Babashka boundary, and both harness self-tests prove its clean one-commit Git
contract.

The benefit is testability and one structured language across capsules,
scheduling, and scoring. Being able to inspect and edit the runner with
clj-surgeon is useful, but it is not the reason to migrate.

## Verification gates

- `make benchmark-edit-portfolio-self-test`;
- existing benchmark schedule, harness, summarizer, retention, and evidence
  self-tests;
- `shellcheck` for changed shell scripts;
- Standard Clojure Style for new or changed Clojure verifier/tests;
- targeted tests and complete `make test`;
- one correct two-lane pilot with structured results retained according to
  policy.

## Definition of done

One repo-owned command runs the frozen five-stratum portfolio with an explicit
treatment matrix, exact multi-file scoring, complete route metrics, and no
external source dependency. Its self-test proves the harness without model
calls. The first pilot is reported honestly per stratum, and subsequent product
changes can rerun the identical capsules without private logs or live repos.
