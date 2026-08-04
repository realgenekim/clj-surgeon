# Clean Codex Structural-Lens Benchmark

**Status:** Accepted experiment design

## Outcome

Measure whether the current clj-surgeon package lets a clean Codex session
read, find, and edit Clojure with fewer tokens, less latency, fewer commands,
and stronger mutation safety than commit `19a20b0`, immediately before
`:show-form` and the associated agent-facing improvements.

The benchmark is descriptive. It records raw JSONL and exact fixtures so later
runs can reproduce or challenge the result; it does not claim statistical
significance from one run per cell.

## Treatments

Run adjacent pre/post pairs for every task under three contexts:

1. no clj-surgeon skill and an outcome-only prompt;
2. the version-matched clj-surgeon skill and the same prompt;
3. no skill plus a version-neutral prompt asking Codex to use the installed
   clj-surgeon as its primary lens.

Run the current version once more with each experimental compact skill. V1 in
`bench/compact-clj-surgeon-skill/SKILL.md` tests terse routing rules without
examples. V2 in `bench/compact-v2-clj-surgeon-skill/SKILL.md` adds one exact
command shape per route while remaining much smaller than the production
skill. Do not mention operation names in the explicit prompt. This keeps tool
discovery separate from leaking the answer.

## Tasks

- Return one named top-level form from a namespace over 500 lines.
- Find and return the containing form from distinctive text.
- Find every occurrence of a structural pattern in a file containing text and
  comment decoys plus formatting variation.
- Change one independently readable expression inside a `case`, preserving all
  unrelated bytes, then verify the edit.

## Controls

- Pin Codex model and reasoning effort.
- Use commit-specific CLI wrappers; never use `~/bin/clj-surgeon`.
- Pin each run's login-shell `PATH` through an isolated `ZDOTDIR` and assert
  that `zsh -lc` resolves the commit-specific wrapper before starting Codex.
- Use identical frozen fixtures for both versions.
- Run outside this repository's directory ancestry with no project AGENTS.md.
- Give each run a new `CODEX_HOME`, ephemeral session, and recreated fixture.
- Install only the treatment skill into that home.
- Disable user config and rules; retain only authentication.
- Prewarm both CLI versions before measured runs.
- Alternate pre/post order across blocks.
- Preserve raw JSONL, stderr, prompts, command lists, and target diffs.

## Metrics

Use the final `turn.completed` usage object; do not sum cumulative usage events.
Record input, cached input, derived uncached input, output, and reasoning-output
tokens separately. Also record wall time, shell calls, approximate atomic
commands, clj-surgeon invocations, source-inspection calls, source-output bytes,
help calls, text-range readers, and whether the skill was actually read.

Correctness is a gate, not part of a blended score. Read tasks must return the
exact source. Structural search must return both real forms and not merely text
decoys. The edit must equal one exact expected file. For the edit, separately
record whether Codex generated a saved plan, observed it before a distinct
apply command, and verified after application.

## Known Confounds

- The post commit bundles the operation, aliases, help, refusals, documentation,
  and skill. The version comparison measures the product package, not
  `:show-form` in isolation.
- Wall time includes model-service variance. Adjacent pairing reduces but does
  not eliminate it.
- Provider caching depends on run order. Cached tokens are reported separately.
- Reading a skill has a token cost; that cost is intentionally included because
  it is part of the caller experience.
- A safe bounded text read can be correct. Safety is not defined as “used
  clj-surgeon.”

## Verification Gates

- The runner refuses a dirty or missing authentication/tool prerequisite.
- Every run records its starting fixture hash and final diff.
- Raw event streams remain available for auditing aggregate metrics.
- The summarizer derives tables only from the per-run TSV.
- Run the repository test suite after adding or changing the harness.

## Definition of Done

One command produces an auditable result directory for all 32 cells, including
raw events, correctness and safety fields, and a Markdown aggregate. A dated
observation reports the actual result, limitations, and any skill/help changes
the evidence justifies.
