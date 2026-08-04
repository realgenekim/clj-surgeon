# Clean Codex Benchmark: Before and After the Structural Shell

Date: 2026-08-03

## Question

Does the current clj-surgeon package reduce the time, tokens, commands, and
source exposure required for a clean Codex agent to read, find, and edit
Clojure compared with commit `19a20b0`, immediately before `:show-form` and the
new agent-facing guidance?

## Method

Thirty-two ephemeral Codex sessions ran with Codex CLI `0.146.0`, model
`gpt-5.6-sol`, and medium reasoning. Every run used a new Codex home, an
identical frozen fixture, a commit-specific CLI wrapper, and no repository or
global agent instructions. A per-run login-shell assertion prevented the
versioned wrapper from falling through to `~/bin/clj-surgeon`.

The four tasks were:

1. return one named top-level form;
2. find its containing form from distinctive text;
3. find two structural occurrences while excluding string/comment decoys;
4. change one expression inside a `case` while preserving a similar expression
   elsewhere and every unrelated byte.

Pre/post pairs ran under no skill, the version-matched skill, and a
version-neutral “use installed clj-surgeon as the primary lens” prompt. The
current version also ran with two 1 KB experimental compact skills. V1 named
the routes without exact invocations. V2 added one exact command shape per
route.

Correctness was scored before efficiency. Structural match-set correctness is
separate from byte-exact Markdown presentation. Edit correctness required the
exact expected file; safety separately required a successful saved plan,
distinct later application, and post-apply verification.

## Primary Result: Version-Matched Skills

Every pre and current run was correct. Both edit runs generated and applied a
saved plan in separate commands and verified afterward.

| Task | Input tokens | Wall time | Shell calls | Source-output bytes |
|---|---:|---:|---:|---:|
| Named form | 64,361 → 45,917 (-28.7%) | 26.6s → 24.8s (-6.9%) | 3 → 2 | 6,775 → 1,695 (-75.0%) |
| Semantic clue | 65,773 → 45,970 (-30.1%) | 36.4s → 27.2s (-25.2%) | 4 → 3 | 6,971 → 1,738 (-75.1%) |
| Structural find | 60,770 → 45,600 (-25.0%) | 26.6s → 22.6s (-15.2%) | 3 → 2 | 538 → 538 |
| `case` edit | 188,529 → 136,604 (-27.5%) | 57.5s → 43.6s (-24.2%) | 11 → 9 | 33,954 → 9,473 (-72.1%) |

Across all four tasks, current versus pre used:

- **27.8% fewer cumulative input tokens**: 379,433 → 274,091;
- **23.1% fewer uncached input tokens**: 55,849 → 42,923;
- **22.0% fewer output tokens**: 4,945 → 3,859;
- **19.7% less wall time**: 147.1s → 118.1s;
- **23.8% fewer shell calls**: 21 → 16;
- **72.1% fewer source-output bytes**: 48,238 → 13,444.

The named-form route changed from skill → `:ls` → bounded text read to skill →
one `:show-form`. The semantic route changed from skill → lexical lookup →
outline/range recovery to skill → `rg -n` → `:show-form :line`. Structural
search changed from a help detour plus `:find-subform` to one `:grep-form` after
the required skill read. The current edit used `rg -n`, `:show-form`, plan,
apply, and verification; the pre edit needed outline and text-range recovery as
well.

## Prompt and Discoverability Result

With no skill and an outcome-only prompt, the known-form pair was essentially
unchanged: both versions used generic bounded text tools in two commands and
about 41K input tokens. A better binary on `PATH` does not advertise itself.

With no skill but the version-neutral instruction to use clj-surgeon, the three
read tasks changed from 14 to 7 commands and from 252,222 to 143,083 input
tokens (**43.3% fewer**). Wall time fell from 132.0s to 68.5s (**48.1% less**).
No operation names were supplied in the prompt; the difference came from live
help and the tool contract.

The explicit pre-version edit generated a valid saved plan but stopped without
applying it. The current version discovered the read, find, plan, apply, and
verification commands through help and completed the exact edit. This is a
correctness improvement, so its efficiency is not meaningfully comparable.

## Skill Experiment: Short Is Not the Same as Precise

| Current skill | Bytes | Correct | Input tokens | Wall time | Calls |
|---|---:|---:|---:|---:|---:|
| Production | 8,046 | 4/4 | 274,091 | 118.1s | 16 |
| Compact V1: route names, no exact syntax | 1,015 | 4/4 | 476,444 | 205.2s | 29 |
| Compact V2: exact command shapes | 1,112 | 4/4 | 265,824 | 138.4s | 15 |

Compact V1 caused agents to guess invalid command shapes, call help, search for
documentation that was absent from the neutral fixture, and retry. It consumed
74% more input tokens and 81% more commands than the production skill despite
being one eighth its size.

Adding only 97 bytes of exact command examples in V2 restored one-shot routes.
Compared with the production skill, V2 used 3.0% fewer cumulative input tokens,
18.7% fewer uncached input tokens, and one fewer command across four tasks.
Its measured wall time was 17.2% higher, which is not interpretable from one
run per cell. V2 is a promising replication candidate, not evidence that the
production skill should be replaced wholesale: the full skill also carries
safety contracts for operations outside these four tasks.

## Correctness and Safety Findings

- All 32 sessions found the correct read result or structural match set except
  two pre-version edit cells.
- The no-skill pre edit changed the intended expression but removed the final
  newline, failing exact-byte preservation.
- The explicit-tool pre edit stopped after plan generation and left the file
  unchanged.
- All current edit treatments produced the exact expected file.
- The production pre/current skills and both compact skills kept plan and apply
  in separate process invocations. The current explicit-help route also did so.
- Structural Markdown sometimes indented an exact multiline source snippet.
  The benchmark therefore reports structural correctness and exact presentation
  separately instead of falsely marking the match set wrong.

## Interpretation

The current package is materially more useful to a clean agent. Its largest
gain is not raw parser speed; it is route compression. Exact source becomes one
addressable object, a semantic clue becomes one lexical coordinate plus one
structural read, file-wide structure no longer requires guessing a parent, and
mutation help exposes the full safe lifecycle.

The skill is part of the product. No-skill results show that undiscovered
capability creates little benefit. The compact experiment sharpens that lesson:
an agent-facing skill can be small, but it must include executable syntax and
decision boundaries. Operation names alone make the model independently guess
both the feature and its invocation.

## Limits and Next Experiment

This is a controlled descriptive pilot with one run per cell, not a statistical
study. Wall time includes service variance, and cached input depends on run
order. The post commit bundles code, aliases, help, remedies, README, and skill,
so the pre/post result measures the shipped package rather than `:show-form`
alone.

The next highest-value experiment is three adjacent repetitions of production
skill versus compact V2 on these four tasks, followed by additional tasks for
dependency-aware moves, CLJC ambiguity, extraction, and refusal recovery. Do
not change the production skill from this pilot alone.

## Evidence

- Reproducible runner: [`bench/run_clean_codex.sh`](../../bench/run_clean_codex.sh)
- Rescored per-run TSV: [`runs.tsv`](../../bench/results/2026-08-03-gpt-5.6-sol-medium/runs.tsv)
- Generated table: [`generated-summary.md`](../../bench/results/2026-08-03-gpt-5.6-sol-medium/generated-summary.md)
- Experiment plan: [`clean-codex-benchmark.md`](../plans/clean-codex-benchmark.md)
- Raw run directory on the benchmark host: `/tmp/clj-surgeon-clean-codex-20260803-v1`
- `runs.tsv` SHA-256: `49b333045dc4b879e2699ce4167f57eda7f63168c9e853606449791973433447`
