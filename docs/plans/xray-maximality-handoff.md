# X-Ray Maximality Handoff

**Status:** Branch implementation verified; release review incomplete
**Branch:** `audit/xray-maximality`
**Head:** `477dca9` (`xray-maximality-candidate-v13`)
**Frozen base:** `fac340f` (`main`, `origin/main`, `xray-v8-kept`)

## Mission

Make clj-surgeon the fastest safe structural lens for an LLM working on
Clojure: one structural read or edit plan should replace repeated `rg`, `sed`,
whole-form reconstruction, shape probes, and repair calls. Clojure source
should arrive as ordinary Clojure data; the model should use ordinary pure
Clojure to interpret it. The kernel owns parsing, exact locations, bounded
evidence, cardinality, replay, and refusal. The model owns meaning and intent.

The priorities are strict:

1. Exact correctness and unchanged source are gates.
2. Wall-clock time is the primary efficiency metric.
3. One-shot behavior and fewer source-bearing shell calls come next.
4. Source output, total tool output, tokens, and repair behavior follow.
5. When measured performance is genuinely close, prefer fewer documented
   concepts and a smaller always-loaded skill.
6. Never weaken a test. Every change must leave the suite stronger.

## Read These First

- `AGENTS.md` and `CLAUDE.md`
- `docs/vision.md`
- `docs/testing-guidelines.md`
- `docs/plans/xray-maximality-audit.md`
- `docs/observations/2026-08-04-captains-log-one-read-surface.md`
- `skills/clj-surgeon/SKILL.md`
- `bench/README.md`

The Captain's Log is the chronological evidence record. The audit plan is the
contract. This handoff is the current-state and release-gate index. Every open
finding is separately actionable in [the must-fix register](../must-fix/README.md).

## Exact Repository State at Handoff

```text
branch                         audit/xray-maximality
HEAD                           477dca9, tagged candidate-v13
main / origin/main             fac340f
branch distance                0 behind, 14 ahead of main
upstream                       none configured
committed branch delta         21 files, +2,077 / -450
uncommitted before handoff      CHANGELOG, Captain's Log, audit plan, vision
release actions                no merge, push, install, or issue closure
```

The 14 branch commits are the 13 tagged candidates plus the tagged adversarial
benchmark revision. All candidate tags are local rollback points. Do not
rewrite or squash this experimental history; the losing candidates are useful
evidence.

The installed development boundary is surprising:

```text
~/bin/clj-surgeon
  -> adds /Users/genekim/src.local/clj-surgeon/src to the classpath

~/.codex/skills/clj-surgeon
  -> /Users/genekim/src.local/clj-surgeon/skills/clj-surgeon
```

Therefore the current checkout controls the effective installed CLI and Codex
skill without another `make install`. Treat branch switching as a deployment
change on this machine.

## Product Implemented on the Branch

The primary read algebra is one `:xray :expr` surface:

```clojure
(form 'transition) ; exact literal evidence

(-> (form 'transition)
    (match :finish)
    right) ; exact structural relationship

(-> (form 'audit-report)
    initializer
    (expect-count 1)
    (analyze (fn [[report]]
               (frequencies (map :category (:events report))))))
```

Its observable laws are:

- `analyze` always receives one ordered vector, including for zero or one
  selection; `expect-count` refines cardinality without changing that type.
- `initializer` selects a `def` right-hand side without evaluating it.
- Computed input canonicalizes a selected map literal, `(hash-map ...)`, or
  `(array-map ...)` to one map view without executing source.
- `tree-seq`, `key`, `val`, and `for` support ordinary pure Clojure analysis.
- Literal reads retain exact syntax. Computed reads return bounded EDN plus
  compact addresses, ranges, trace, selection hash, per-match hashes, and the
  whole-file hash.
- X-ray never writes source or creates a plan.
- `:edit :expr` uses the same structural builders, emits one immutable
  hash-bound plan, and relies on a separate `:replace-subform!` apply.
- CLJC named selection sees direct branch-local forms and accepts an optional
  platform.
- Old `:q`, EDN query paths, `xray`, `xray-one`, `compute`, `aggregate`,
  `inspect`, `one`, and `all` remain compatibility inputs but are not the
  primary agent model.

The SCI sandbox exposes pure collection and control operations but not I/O,
processes, namespaces, mutable references, classes, or host interop. Authored
`loop`, `recur`, lazy/chunk internals, and direct macro-expansion machinery are
refused before source I/O; the same private names remain available only to
expand an authored `for`.

## What the Hill Climb Established

Every kept comparison required exact independently scored output and unchanged
source. Wall time below is the median of correct runs in the adjacent
comparison, not a cross-experiment absolute benchmark.

| Candidate | Change | Adjacent result | Decision |
|---|---|---:|---|
| v1 | CLJC forms, compact evidence, exact-one | 75.3s vs 72.7s released | Keep mechanics; API still probed |
| v2 | One X-ray read surface | 69.0s vs 75.2s, 8% faster | Keep near-tie simplification |
| v3 | `inspect` plus input summary | 69.6s vs 59.8s, 16% slower | Reject treatment |
| v4 | Isolated `inspect :one/:all` | 78.4s vs 67.7s, 16% slower | Reject spelling |
| v5 | `one` / `all` | 89.1s vs 63.0s, 41% slower | Reject spelling |
| v6 | Stable vector selection | 102.2s vs 83.9s, 22% slower | Keep type law; fix navigation |
| v7 | `initializer` plus canonical map view | 51.5s vs 75.5s, 32% faster | Decisive keep |
| v8 | Expose `tree-seq` | 43.805s vs 47.626s, 8% faster | Capability useful; weak activation |
| v9 | Executable traversal guidance | 39.432s vs 50.299s, 22% faster | Decisive keep |
| v10 | A-priori vector contract; `key`/`val`/safe `for` | 37.552s vs 50.025s, 25% faster | Decisive keep |
| v11 | Prescriptive nested `for` guidance | 35.411s vs 27.529s, 29% slower | Reject guidance; keep capability |
| v12 | 107-line progressive skill | 26.004s vs 28.285s, 8% faster | Keep one-shot simplification |
| v13 | 89-line progressive skill | 24.462s vs 23.764s, 3% slower | Selected under close-result rule |

v13 is not the measured speed winner. v12 has the lower compact-skill median.
All eight v12/v13 sessions were exact, unchanged, and two-command one-shots;
the 698 ms difference is within ordinary service variance. v13 is the selected
review candidate because it retains the tested concepts in 18 fewer lines and
711 fewer bytes. A reviewer may choose v12 instead, but must describe that as a
release tradeoff, not a correctness fix.

The audit's local-maximality stopping rule is satisfied: after v10's last
greater-than-10% win, three bounded neighbors produced a 29% loss, an 8% win
with compelling simplicity, and a 3% loss within noise. Do not continue shaving
prompt prose without a new task, field failure, or genuinely different design.

## Raw Evidence Manifest

The durable summary is the Captain's Log. Raw runs still live only in
`/private/tmp`, which is a release blocker because the audit requires prompts,
transcripts, commands, timings, token use, scorers, and fixture hashes to be
retained.

Valid result roots include:

```text
/private/tmp/clj-surgeon-q-bb-pilot-20260804
/private/tmp/clj-surgeon-xray-unified-{pilot,final}-20260804
/private/tmp/clj-surgeon-xray-self-repair-{pilot,final}-20260804
/private/tmp/clj-surgeon-xray-inspect-{pilot,final}-20260804
/private/tmp/clj-surgeon-xray-one-all-{pilot,final}-20260804
/private/tmp/clj-surgeon-xray-stable-selection-{pilot,final}-20260804
/private/tmp/clj-surgeon-xray-canonical-initializer-{pilot,final}-20260804
/private/tmp/clj-surgeon-xray-tree-seq-pilot-v2-20260804
/private/tmp/clj-surgeon-xray-tree-seq-final-20260804
/private/tmp/clj-surgeon-xray-tree-seq-activation-final-20260804
/private/tmp/clj-surgeon-xray-pure-clojure-final-20260804
/private/tmp/clj-surgeon-xray-scoped-traversal-final-20260804
/private/tmp/clj-surgeon-xray-compact-skill-final-20260804
/private/tmp/clj-surgeon-xray-ultra-compact-skill-final-20260804
```

Exclude these roots completely:

```text
/private/tmp/clj-surgeon-xray-tree-seq-pilot-20260804
  Concurrent resume writers corrupted this pilot. The repository was not
  affected.

/private/tmp/clj-surgeon-claude-fable-opus-20260804-v1
  The escalated execution boundary never launched a valid Claude trial. All
  fixture hashes remained unchanged and no plan was created.
```

Archive only SFW material. A repository and local-history scan at handoff found
none of the prohibited project-context terms.

## Verification Receipt

Run on the current worktree after the final documentation edits began:

```text
make test
  433 tests, 3,122 assertions, 0 failures, 0 errors
  benchmark summary self-test passed
  ops-registry scorer self-test passed
  benchmark schedule self-test passed

clj-kondo --lint src test
  0 errors, 0 warnings

shellcheck bench/run_clean_codex.sh
  clean

git diff --check
  clean
```

Rerun these gates after editing this handoff and before committing. Formatting
was already applied to the changed Clojure files in the candidate commits.

## Review Findings and Open Decisions

### Release blockers

1. **Archive the evidence.** `/private/tmp` is not durable. Preserve the valid
   raw corpus or an intentionally redacted but independently rescorable corpus
   under `bench/results/`; record hashes and exclusions.
2. **Validate Claude Code.** There is no valid Fable or Opus result. Run a
   bounded matrix only after a one-call authentication smoke succeeds. Every
   child needs its own deadline and independently streamed receipt; one stalled
   child must not hold the matrix hostage.
3. **Unify agent packaging.** Root `skill.md` is 249 lines / 1,512 words.
   Canonical `skills/clj-surgeon/SKILL.md` is 89 lines / 563 words. Decide which
   file Claude loads, add a discoverable install path if appropriate, and test
   the exact installed artifact rather than copying an experimental substitute
   only into a temp workspace.
4. **Make experiment isolation honest.** The installed CLI and Codex skill
   follow this checkout. Either document that development behavior clearly or
   install immutable/versioned artifacts before claiming release isolation.

### API decisions before release

1. `:evidence :full` is accepted by `prepare-xray-options` but omitted from the
   operation registry's argument help. Decide whether it is intentionally
   hidden compatibility or a documented public argument; then make help,
   README, tests, and changelog agree.
2. Keep the wording precise: v13 is smaller and selected; v12 was faster in
   the direct compact comparison.

### Technical watchpoints worth permanent tests

These are review findings, not demonstrated regressions in the documented
primary route:

1. The SCI sandbox is capability-limited, not termination-proof. Direct
   `loop`/`recur` is refused, but an allowed expression such as an unbounded
   reduction over `(range)` can still fail to terminate. Do not claim a general
   nontermination guarantee; consider an evaluation budget only if a field
   failure earns the complexity.
2. Platform metadata for ordinary top-level forms defaults to both `:clj` and
   `:cljs` because the pure query evaluator does not receive the file
   extension. The documented selector is for CLJC. Add a refusal or a
   file-aware test before promising meaningful platform selection on `.clj` or
   `.cljs` files.
3. Canonical map-constructor normalization is shallow: it normalizes each
   selected value when that value itself is a known constructor. Do not imply
   recursive evaluation or recursive normalization.
4. The pre-expansion forbidden-symbol scan originally rejected inert quoted
   data containing `loop`, `recur`, or a chunk-internal symbol. A concurrent
   uncommitted change now skips quoted subtrees and adds a full guarded-symbol
   matrix. Review, format, and verify that incoming fix before committing it;
   see must-fix 010.

## Claude Fable/Opus Confirmation Contract

Run at least these two tasks for both `fable` and `opus`, in parallel but with
independent 90-second deadlines and output files:

1. **Irregular read:** analyze the real `ops-registry` and return independently
   scored category frequencies, required-argument count, and paired operations.
2. **Guarded edit:** change only the `:finish` result in the real-program-derived
   `pair_view.clj`, preserving its attached comment and every unrelated byte,
   using a separate plan and apply.

The matched-skill confirmation may explicitly say to use the installed skill.
After it passes, an optional choice trial may offer ordinary readers/patches
and clj-surgeon without forcing the choice.

Record for every child:

- requested alias and resolved model ID;
- prompt and exact installed skill hash;
- exit state, wall time, turns, and tool calls;
- commands and bounded tool output;
- final answer and independent score;
- start/final source hashes and exact diff;
- whether the run was a true one-shot after skill loading;
- timeout or infrastructure failure as a first-class receipt.

Never wrap all children in one blocking `Promise.all` without streaming or
per-child deadlines. The failed attempt did exactly that and hid every partial
result until the last child exited.

## Definition of Done

The branch is ready for a human release decision only when the valid benchmark
corpus is durable, Fable and Opus each pass the bounded read and edit matrix,
Claude and Codex use explicitly identified tested skill artifacts, help and
README agree about every accepted X-ray argument, all new field failures have
permanent tests, and the verification receipt remains at least 433 tests /
3,122 assertions with no existing test weakened. Main stays frozen until Gene
reviews that evidence and explicitly authorizes merge, install, push, or issue
closure.

## Ready-to-Paste Takeover Prompt

```text
You are taking over the clj-surgeon X-ray maximality audit in
/Users/genekim/src.local/clj-surgeon.

Start by reading, in order:
1. AGENTS.md and CLAUDE.md
2. docs/vision.md
3. docs/testing-guidelines.md
4. docs/plans/xray-maximality-audit.md
5. docs/plans/xray-maximality-handoff.md
6. docs/observations/2026-08-04-captains-log-one-read-surface.md
7. skills/clj-surgeon/SKILL.md
8. bench/README.md

Then establish state with git status, branch -vv, and log. Preserve all existing
changes. Work only on audit/xray-maximality. Do not switch main, merge, install,
push, rewrite history, delete tags, close issues, or discard any user changes
without Gene's explicit authorization.

Our goal is the fastest safe one-shot structural lens for LLMs editing Clojure.
Correctness and unchanged source are absolute gates. Wall clock is the primary
metric. Then optimize source-bearing calls, source/tool output, tokens, and
repair behavior. Prefer a smaller API only when performance is genuinely close.
Never weaken tests; the suite must finish stronger than it started.

The implementation is green at 433 tests / 3,122 assertions. v13 is the selected
89-line review candidate, but v12 was 3% faster in their direct comparison. Do
not call v13 the speed winner. The local hill-climb stopping rule is satisfied;
do not resume prompt shaving without a new task or field failure.

Resolve the release gates in docs/plans/xray-maximality-handoff.md. First make
the raw benchmark evidence durable and preserve the two explicitly excluded
invalid roots. Then build or repair a bounded clean-Claude entrance and confirm
both fable and opus on the irregular X-ray read and guarded structural edit.
Run children in parallel with independent 90-second deadlines and streamed
receipts; one child must never block the others. Score outputs independently
and preserve source hashes. Next reconcile the 249-line Claude skill.md with
the measured 89-line canonical skill and decide whether :evidence is hidden
compatibility or public help surface. Add permanent tests for every defect.

Be careful: ~/bin/clj-surgeon and ~/.codex/skills/clj-surgeon point into this
checkout, so changing branches changes the effective installed tool even
without make install. The previous Claude attempt is invalid because the
escalated execution boundary never started the bounded command. It provides no
evidence about Claude or clj-surgeon.

After changes, format only changed Clojure files, run targeted tests, make test,
clj-kondo --lint src test, shellcheck bench/run_clean_codex.sh, the skill
validator, and git diff --check. Keep the total at or above 433 tests / 3,122
assertions and explain every added assertion. Scan the worktree and commit
messages for the prohibited project-context terms; none may appear.

Document all positive, negative, and infrastructure results in the Captain's
Log. Commit and tag only on the audit branch after the evidence is complete.
Stop before push, merge, install, or main and present Gene with the exact
release tradeoffs and verification receipt.
```
