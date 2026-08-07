# Clean Codex benchmark

## Representative edit portfolio

Run the repo-owned editing hill-climb:

```bash
make benchmark-edit-portfolio
```

The default pilot starts 15 paid Codex sessions: five frozen tasks across the
tagged local-microscope skill, the current working tree, and a native-tools
control. The tasks cover a complete multi-file decision, a bounded owner edit,
a dependency-aware move, literal `#()` source fidelity, and a prose-only edit
that native patching should win. Every task contains its prompt, before files,
accepted after files, hashes, and verification policy under
`bench/fixtures/edit_portfolio/`. No live application or external source
repository is required.

Verify the complete fixture and harness contract without a model call:

```bash
make benchmark-edit-portfolio-self-test
```

For the smallest paid pilot, compare only the current treatment and native
control on the supplied-decision task:

```bash
BENCH_TASKS=decision-batch-edit \
BENCH_RUN_MATRIX='post:matched-skill native:no-skill' \
BENCH_REPLICATES=1 \
make benchmark-edit-portfolio
```

`BENCH_RUN_MATRIX` is an ordered list of `VERSION:CONTEXT` cells. It avoids an
invalid Cartesian product such as `native:matched-skill`. The portfolio accepts
`BENCH_POST_COMMIT=WORKTREE` so an uncommitted candidate can be measured in an
isolated copy. Every task copy is its own clean Git repository, so normal diff
and status verification remains available without exposing the source repo.
Both Codex and Claude harnesses use the shared Babashka workspace initializer
for this contract. Use a commit ID or `HEAD` for durable release evidence.

Correctness is a gate. Inspect each task as well as the portfolio median. The
runner records exact multi-file results, source-bearing calls, failed mutation
attempts, use of `:change!`, post-decision reads, temporary EDN manifest patches,
and whether one successful transaction kept a complete decision in one action.
The prose control is successful when the agent chooses the native patch; more
clj-surgeon use is not inherently better.

The selection contract and keep gates are in
[the representative edit portfolio plan](../docs/plans/representative-edit-portfolio.md).

## Bounded clean-agent skill acceptance

Use the six-session acceptance battery to exercise the installed Claude and
Codex skill surfaces without starting the full 32-session Codex experiment:

```bash
make benchmark-agent-skills
```

It runs these two independently invokable batteries in sequence:

- `make benchmark-codex-skill` starts one current-version Codex session for the
  real `ops-registry` X-ray read and one for the same comment-preserving
  `pair_view.clj` edit used by the Claude battery. Both agents receive the same
  task contract and exact expected bytes. The target uses the existing clean
  Codex runner, scorers, isolated home, and commit-specific CLI wrapper.
  Override its ordinary `BENCH_MODEL`,
  `BENCH_REASONING`, `BENCH_PARALLELISM`, or `BENCH_RESULT_DIR` settings as
  needed. `BENCH_TASKS`, `BENCH_CONTEXTS`, `BENCH_VERSIONS`, and
  `BENCH_REPLICATES` also replace the bounded defaults.
- `make benchmark-claude-skill` starts Fable and Opus on the independently
  scored real `ops-registry` X-ray read and comment-preserving `pair_view.clj`
  plan/apply edit. Every child has its own 90-second deadline, workspace, raw
  JSONL, state receipt, terminal receipt, hashes, and diff. Override
  `CLAUDE_BENCH_RESULT_DIR`, `CLAUDE_BENCH_DEADLINE_SECONDS`,
  `CLAUDE_BENCH_MODELS`, `CLAUDE_BENCH_TASKS`, or the per-model
  `CLAUDE_BENCH_FABLE_MAX_BUDGET_USD` and
  `CLAUDE_BENCH_OPUS_MAX_BUDGET_USD` limits only when intentionally creating a
  separate series.

Both are paid model-service targets. Run their fast process/isolation tests
without a model call with:

```bash
make benchmark-agent-skills-self-test
```

When a harness writes under `bench/results/`, it archives the complete result
outside Git and retains only structured scores, receipts, usage, hashes, and
summaries. Raw transcripts, prompts, final answers, and workspaces are ignored
by Git. Set `BENCH_RETENTION=local` only when you need temporary local forensic
files. For a completed older run, use:

```bash
make retain-benchmark-result RESULT_DIR=bench/results/RESULT_NAME
make verify-benchmark-retention
```

The Claude self-test deliberately stalls one fake child and proves that the
other success and failure receipts are emitted and preserved before that child
hits its independent deadline.

## Full controlled Codex comparison

Run the controlled pre/post pilot:

```bash
make benchmark-clean-codex
```

The run starts 32 ephemeral Codex sessions and can consume substantial tokens.
It runs four sessions in parallel by default. Set `BENCH_PARALLELISM=1` only
when a serial control is intentional. Override the pinned defaults only when
intentionally creating a separate benchmark series:

```bash
BENCH_MODEL=gpt-5.6-sol \
BENCH_REASONING=medium \
BENCH_PARALLELISM=4 \
BENCH_RESULT_DIR=/tmp/clj-surgeon-benchmark \
make benchmark-clean-codex
```

For a two-session harness smoke test:

```bash
BENCH_TASKS=named-form \
BENCH_CONTEXTS=matched-skill \
BENCH_INCLUDE_COMPACT=false \
make benchmark-clean-codex
```

For an old-versus-now comparison with a true native-tools control, use the
`native` version only with the `no-skill` context. The pre and post arms expose
their commit-specific CLI but no skill; the native arm uses the identical
fixture, prompt, and scorer while exposing neither a `clj-surgeon` executable
nor a clj-surgeon skill:

```bash
BENCH_PRE_COMMIT=19a20b0 \
BENCH_POST_COMMIT=HEAD \
BENCH_VERSIONS='pre post native' \
BENCH_CONTEXTS=no-skill \
BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=4 \
make benchmark-clean-codex
```

The native arm fails its correctness gate if its command log mentions a
clj-surgeon invocation or records a skill read. Its isolated `PATH` is checked
before Codex starts.

The `computed-edit` task tests whether a clean agent can derive a replacement
from an unknown selected value in one plan call. It is the keep gate for the
native `transform` builder:

```bash
BENCH_TASKS=computed-edit \
BENCH_CONTEXTS=matched-skill \
BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=4 \
make benchmark-clean-codex
```

The `xray-summary` task tests routine aggregation. The `xray-checksum` stress
task tests whether one read call replaces computation that is unsafe to perform
by eye. Together they form the keep gate for the read-only `xray` builder:

```bash
BENCH_PRE_COMMIT=ad726c6 \
BENCH_POST_COMMIT=HEAD \
BENCH_TASKS='xray-summary xray-checksum' \
BENCH_CONTEXTS=matched-skill \
BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=4 \
make benchmark-clean-codex
```

The `ops-registry-xray` task is the real irregular computed-read gate. It makes
clj-surgeon analyze its own operation registry and independently scores category
frequencies, required arguments, and paired operations. Compare the normal
version-matched skill against the strongest composed baseline:

```bash
# Full-evidence release versus compact exact-one candidate.
BENCH_PRE_COMMIT=fac340f \
BENCH_POST_COMMIT=cd9244b \
BENCH_TASKS=ops-registry-xray \
BENCH_CONTEXTS=matched-skill \
BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=4 \
make benchmark-clean-codex

# One-command structural read piped to Babashka.
BENCH_PRE_COMMIT=fac340f \
BENCH_POST_COMMIT=cd9244b \
BENCH_VERSIONS=pre \
BENCH_TASKS=ops-registry-xray \
BENCH_CONTEXTS=pipeline-skill \
BENCH_INCLUDE_COMPACT=false \
BENCH_REPLICATES=4 \
make benchmark-clean-codex
```

Efficiency medians include only correct, unchanged-source runs. Replicates
counterbalance pre/post scheduling while retaining parallel execution.

Resume a stopped result directory without rerunning completed rows:

```bash
BENCH_RESUME=true \
BENCH_RESULT_DIR=/tmp/clj-surgeon-benchmark \
make benchmark-clean-codex
```

One runner owns a result directory for its full lifetime. A concurrent runner
refuses before changing `runs.tsv` or a run directory. The owner lock records
PID, host, process start, UTC start, and command metadata in
`.benchmark-owner/owner.tsv`. If an owner is dead or cannot be verified, the
runner still refuses by default. After inspecting the receipt, recover it
explicitly and preserve its metadata with:

```bash
BENCH_RECOVER_STALE_OWNER=true \
BENCH_RESUME=true \
BENCH_RESULT_DIR=/tmp/clj-surgeon-benchmark \
make benchmark-clean-codex
```

Recovery moves the old lock to a timestamped
`.benchmark-owner.recovered-*` directory. Row-lock waits are bounded to ten
seconds by default; set `BENCH_ROW_LOCK_TIMEOUT_SECONDS` to another positive
integer when slow storage requires it. Each scheduled child writes an atomic
`terminal.tsv` receipt. The supervisor waits for every child and checks every
receipt before summary generation; the summary is also replaced atomically so
a failed summarizer preserves any prior summary.

Run the fast harness tests without starting Codex model sessions:

```bash
make benchmark-harness-self-test
```

The output directory contains `runs.tsv`, `summary.md`, and one directory per
run with the exact prompt, raw JSONL, stderr, final response, command list,
fixture hashes, and diff. The runner uses isolated Codex homes and
commit-specific CLI wrappers. It never changes the checkout under test.

See [the experiment plan](../docs/plans/clean-codex-benchmark.md) for the matrix,
scoring contract, and confounds.

## Archived evidence

The raw X-Ray maximality archives have a machine-readable EDN manifest. Verify
that every path is repository-relative, every archive is listed, excluded roots
are absent, and every SHA-256 digest matches:

```bash
make verify-benchmark-evidence
```

Pass another manifest explicitly when checking a separate archive set:

```bash
bb bench/verify_evidence_manifest.clj path/to/manifest.edn
```
