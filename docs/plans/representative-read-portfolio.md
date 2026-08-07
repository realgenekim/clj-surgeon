# Representative MCP Read Portfolio

**Status:** Accepted benchmark design

**Motivating evidence:**

- repeated structural reads paid one process and agent boundary per question;
- one observed architecture diagnosis used 26 structural invocations and
  spent 45.7 seconds in those commands;
- the first live MCP dogfood task read seven known forms from two files through
  two CLI calls before compiling one edit transaction;
- the write MCP keep gate proved that one decision-sized native tool call can
  reduce complete task wall by more than ten seconds.

## Outcome

Create a repo-owned read benchmark that can measure `inspect_clojure` against
the current CLI and native controls as soon as the MCP tool is available.

The benchmark must answer two different questions:

1. Does a hot in-process read tool remove process and shell ceremony?
2. Does batching several currently knowable questions into one call reduce
   complete agent task time?

The benchmark must not reward a tool merely for returning more source. Every
lane receives the same question and must produce the same expected facts.
Correctness gates all timing and token results.

## Representative tasks

Freeze four independent tasks. Each task must contain its prompt, source
snapshot, exact expected facts, provenance, and scoring policy.

| Task | Structural decision | Why it is representative | Strong MCP route |
|---|---|---|---|
| `known-forms-batch` | Seven named owners are already known across two files. | Reproduces the first live dogfood read. No answer depends on an earlier answer, so repeated calls are pure ceremony. | One `forms` request batch. |
| `large-outline` | The owner is unknown in a namespace over 500 lines. | Tests the required first-inspection route and output bounding on a realistic large file. | One `outline` request. |
| `structural-match` | Find real syntax while excluding strings and comments. | Text search can discover candidates but cannot prove structural cardinality. | One `match` request. |
| `computed-xray` | Aggregate structural data without returning the source collection. | Tests whether computation moves to the data instead of the model context. | One `xray` request. |

Do not add a synthetic mixed task whose independent questions exist only to
inflate batch size. The seven-form task is valid because those exact reads
occurred together during real work.

## Frozen task contract

Each task lives under `bench/fixtures/read_portfolio/TASK/` and contains:

- `source/`: the complete source subset available to every lane;
- `task.txt`: a tool-neutral question;
- `expected.edn`: the exact semantic answer;
- `capsule.edn`: task ID, provenance, source hashes, expected source files,
  allowed evidence size, and verification policy.

Final answers must end with one EDN value that can be parsed and compared with
`expected.edn`. Explanatory prose before that value is allowed but is not part
of correctness scoring.

The semantic answer must avoid presentation-only requirements. For example,
score owner names, counts, ranges, or computed values. Do not require a caller
to copy a tool-specific envelope.

## Treatments

Run three lanes against identical source bytes:

| Lane | Available surface |
|---|---|
| `mcp` | Hot persistent `inspect_clojure`; no clj-surgeon CLI or skill. |
| `cli` | Current installed CLI and canonical skill; no MCP. |
| `native` | Native shell and read tools; no clj-surgeon CLI, skill, or MCP. |

Run a second MCP activation lane only when needed:

- `mcp-hint`: one short instruction names `inspect_clojure` and says to batch
  currently knowable requests;
- `mcp-voluntary`: only the tool schema and neutral task are visible.

Do not combine activation and mechanism results. A no-hint native fallback is
valid adoption evidence but not MCP performance evidence.

Counterbalance lane order. Run sequentially unless a separate experiment proves
that concurrent callers do not contend for model or local runtime resources.

## Metrics

Record:

- exact correctness and parseable final EDN;
- complete task wall;
- daemon bootstrap, namespace load, and direct tool latency as separate clocks;
- MCP calls, shell calls, and source-bearing actions;
- distinct files read by the kernel;
- process startups;
- input, cached input, uncached input, output, and reasoning tokens;
- source characters and bytes returned;
- total tool-result bytes;
- failed calls and help calls;
- whether one call answered the complete batch;
- whether the caller reread source after a complete MCP result;
- voluntary MCP adoption.

Incorrect runs remain in the correctness denominator and never enter efficiency
medians.

## Safety and isolation invariants

- Every run uses a fresh isolated workspace and agent home.
- Native and CLI controls cannot see the MCP registration.
- Native controls cannot see a clj-surgeon executable or skill.
- MCP controls cannot see the CLI or skill.
- The task clock begins only after the persistent server passes readiness and
  health checks. Bootstrap remains reported separately.
- Every lane receives byte-identical sources and expected facts.
- Raw events, prompts, tool payloads, and workspaces remain outside Git.
- Retained evidence includes structured rows, task receipts, hashes, summaries,
  and the exact benchmark configuration.
- Existing edit benchmark columns and historical readers remain compatible.

## Pure verifier matrix

Add a pure read-capsule verifier with literal-data tests for:

- valid singleton and multi-file capsules;
- duplicate task IDs;
- missing task, expected answer, capsule, or source;
- unexpected source files;
- blank and parent-traversing paths;
- wrong declared source hash;
- invalid Clojure source;
- invalid or non-EDN expected answer;
- missing evidence budget;
- duplicate expected owner records;
- a task whose expected answer cannot be produced from its declared sources.

The last condition can use task-specific pure scorers. Do not pretend a generic
validator can prove arbitrary semantic questions.

## Boundary tests

- initialize every workspace as an independent clean Git repository;
- prove all lanes receive the same source hashes;
- parse final-answer EDN after optional prose;
- distinguish MCP calls from shell commands in Codex event streams;
- count MCP success only when `read_complete=true` is present;
- record one MCP batch as one source-bearing action;
- reject a claimed batch success with missing results or file hashes;
- preserve complete terminal receipts on caller success and failure;
- test resume, retention, and interrupted-child cleanup;
- run the complete portfolio self-test without a model call.

## Keep gates

Treat twofold speed as a hypothesis, not an assumed result.

The batchable known-forms task passes its mechanism gate when four
counterbalanced correct MCP runs:

- use one MCP call;
- use zero shell calls and zero failed calls;
- read each distinct file once;
- perform no post-result source read;
- return no more source than the CLI lane;
- reduce median complete task wall by at least 30% versus CLI.

A median at or below half the CLI wall establishes the stronger twofold claim.

The complete portfolio passes only when MCP preserves correctness on all four
tasks and improves the read-heavy median. Report any task where native or CLI
wins. Do not hide a losing task in the portfolio aggregate.

Voluntary adoption is a separate gate: four no-hint callers must select
`inspect_clojure` when the task is batchable. A routing hint can establish the
mechanism ceiling but cannot satisfy adoption.

## Bitter-Lesson boundary

The benchmark supplies questions and exact answers. It does not reward a named
tool, infer the next useful question, or encode architectural judgment.

`inspect_clojure` may remove mechanics:

- process startup;
- shell quoting;
- repeated file reads;
- repeated tool turns for independent known questions;
- returning source when a bounded computation is sufficient.

It must not decide what code means, which form matters, or what change should
follow the read.

## Documentation and evidence

- Add the portfolio command and self-test to `make help` and `bench/README.md`.
- Add this plan to `docs/plans/README.md`.
- Record each hill-climb stage in the August 7 Captain's Log.
- Keep mechanism, activation, and cold-start results in separate tables.
- Do not claim a twofold improvement until four correct counterbalanced runs
  establish it.

## Verification gates

- Standard Clojure Style on changed verifier and scorer namespaces;
- pure verifier and event-classification self-tests;
- existing edit-portfolio and retention self-tests;
- `bash -n` and `shellcheck` for changed shell adapters;
- clj-kondo on changed Clojure code;
- complete `make test`;
- one correct single-replicate pilot before the paid four-run gate;
- `git diff --check` and proof that raw logs are not tracked.

## Definition of done

One repo-owned command runs four frozen read tasks across explicit MCP, CLI,
and native lanes. It records correctness-gated wall time, calls, tokens,
evidence bytes, startup, and voluntary adoption. A fast self-test proves
fixtures, scoring, isolation, event classification, scheduling, and retention
without a model call. The reported result states whether batching—not merely a
hot process—made structural reading materially faster.
