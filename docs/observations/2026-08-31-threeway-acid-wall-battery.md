# Three-way acid wall battery: the attractive speedups did not earn a headline

## Verdict

**This battery did not confirm a publishable speedup.** All 42 frozen caller
processes completed with exit 0, but every task class failed the preregistered
all-exact correctness gate, and FLAGSHIP also recorded one wrong-subject event.
The result is therefore **invalid for speed headlines**. The most attractive
descriptive result—LATEST was 7.92× faster than fresh NATIVE on FLAGSHIP caller
wall, with a paired log-ratio 95% interval of 4.39×–15.27×—must not be promoted
as a performance claim from this cohort.

That is the killer-acid result: the battery caught subject slippage that a
server-slice or exit-code report would have missed. It also caught defects in
the battery itself. The shared frozen scorer was an exact-tree scorer, not the
requested semantic-equivalence scorer; it rejected trailing blank-byte
differences and product formatting. The FLAGSHIP setup also placed
`.clj-surgeon.edn` in the measured workspace without including it in the
expected tree. Those are real preregistered failures, not permission to rescore
after seeing the walls.

## Quoteable table — caller wall only

Every wall below is the complete caller turn from prompt dispatch through the
caller's final completion. The bracketed split is the componentwise median in
seconds: **caller emission / transport-or-local-tool / product server**. Product
server time is explanatory only and never the headline. `B/A` and `C/A` are
median within-pair wall ratios; values above 1 favor A. The 95% interval is the
exponentiated paired Student-t interval on log ratios. Its center is the
geometric mean, while the displayed point estimate is the paired median.

| Class | A LATEST caller wall [split] | B TWO-DAYS-AGO caller wall [split] | C NATIVE caller wall [split] | B/A speedup (95% CI) | C/A speedup (95% CI) | Exact A/B/C; wrong subject | Headline status |
|---|---:|---:|---:|---:|---:|---:|---|
| FILL | 143.008s [142.835 / 0.109 / 0.074] | 59.246s [59.083 / 0.067 / 0.087] | 99.896s [99.895 / 0.001 / 0] | 0.29× (0.11–1.18×) | 0.54× (0.15–2.65×) | 3/3, 2/3, 0/3; 0 | **INVALID — no speed headline** |
| WALL | 89.522s [89.294 / 0.088 / 0.141] | 56.562s [56.353 / 0.066 / 0.140] | 83.624s [83.624 / 0.001 / 0] | 0.52× (0.04–7.32×) | 1.26× (0.05–25.09×) | 3/3, 3/3, 0/3; 0 | **INVALID — no speed headline** |
| REPAIR | 114.972s [114.722 / 0.080 / 0.162] | 96.811s [96.587 / 0.067 / 0.157] | 450.168s [450.163 / 0.007 / 0] | 0.83× (0.33–1.80×) | 3.77× (1.59–7.51×) | 5/5, 4/5, 5/5; 0 | **INVALID — no speed headline** |
| FLAGSHIP | 56.527s [53.862 / 0.054 / 2.239] | 109.342s [106.813 / 0.069 / 2.476] | 603.784s [603.580 / 0.204 / 0] | 1.93× (0.21–9.30×) | 7.92× (4.39–15.27×) | 0/3, 0/3, 1/3; 1 | **INVALID — wrong subject and exactness** |

No server slice appears as a standalone performance number. In every class the
caller-emission component dwarfed the subsecond product server component. This
directly falsifies the earlier habit of calling a roughly 117ms server slice a
roughly one-minute refactor.

## Raw walls and ranges

These are all complete walls in schedule order within each arm. They are
included because n=3 and n=5 intervals are wide and assumption-dependent.

| Class | A raw walls, seconds (range) | B raw walls, seconds (range) | C raw walls, seconds (range) |
|---|---|---|---|
| FILL | 96.570, 255.080, 143.008 (96.570–255.080) | 59.246, 63.088, 41.644 (41.644–63.088) | 115.962, 99.896, 77.345 (77.345–115.962) |
| WALL | 266.886, 61.847, 89.522 (61.847–266.886) | 56.562, 102.113, 46.869 (46.869–102.113) | 83.624, 77.904, 335.990 (77.904–335.990) |
| REPAIR | 114.972, 68.265, 140.804, 263.136, 83.052 (68.265–263.136) | 87.787, 100.492, 117.139, 65.773, 96.811 (65.773–117.139) | 536.965, 450.168, 481.197, 325.790, 312.691 (312.691–536.965) |
| FLAGSHIP | 56.527, 124.979, 53.454 (53.454–124.979) | 109.342, 73.908, 130.353 (73.908–130.353) | 603.784, 811.208, 423.398 (423.398–811.208) |

## Class verdicts

**FILL — killed, then invalidated.** The modern arm was slower in every matched
pair: B/A ratios were 0.61×, 0.25×, and 0.29×. A used the held-session
confirm+fill surface in all three rows, but needed a median six actions versus
three for B and emitted a median 3,550 output tokens versus 1,125. One A row hit
`invalid-prepared-confirmation`; the other rows still spent almost all wall in
caller emission. The preregistered A/B ≥1.10× claim is killed even before the
correctness invalidation. B pair 3 made the wrong semantic replacement, and
all three native rows removed the fixture's trailing blank line, so the class
is not eligible for a speed headline.

**WALL — killed, then invalidated.** A did reduce median MCP argument bytes by
31.5% versus B (1,682 versus 2,457), which is the intended prepared-form
mechanism, but that saving did not survive caller wall: the median paired B/A
ratio was 0.52×. The three pair ratios ranged from 0.21× to 1.65×, producing the
0.04×–7.32× small-n interval. All A and B trees were exact, while every native
row removed the trailing blank line. The registered A/B wall claim is killed,
and the class is invalid for a headline.

**REPAIR — dramatic versus native, not versus the old product, and invalid.**
Descriptively, A used a median four actions and 2,690 output tokens versus
native's 32 actions and 10,488 tokens; complete caller wall favored A over
native by a paired median 3.77×, with a 1.59×–7.51× interval. But A was slower
than B on the paired median (B/A 0.83×), killing the registered causal claim
that A beat both controls by at least 1.30×. The manipulation also failed to
isolate the intended refusal: only one of five A rows' first refusals was
`expect-count-mismatch`; four were generic `invalid-mcp-request`. One B row
removed the trailing blank line, so the all-exact gate also invalidates the
class.

**FLAGSHIP — large descriptive wall separation, no earned claim.** A used one
transaction and a median 1,221 output tokens; B used two actions and 2,587
tokens; native used 25 actions and 13,053 tokens. The descriptive paired
medians favored A by 1.93× over B and 7.92× over fresh native. However, no A or
B row matched the expected tree: the harness-created `.clj-surgeon.edn` was not
part of the expected tree, and the transaction's configured formatting changed
the large source file relative to the historical accepted fixture. Native pair
2 additionally read `src/cfp_scheduler_killer/handlers.clj` and the benchmark
checkout's `src/clj_surgeon/mcp_formatter.clj`, producing the mandatory
wrong-subject failure. The fresh native walls remain descriptive only; the
historic 122.278s and 207.898s native anchors were retained as context and were
not substituted into this cohort.

## Correctness and battery defects

All 42 `codex exec` processes exited zero, which demonstrates why process exit
cannot stand in for the shared scorer. The frozen result was
`all_episodes_exact=false` and `wrong_subject_total=1`.

- Native FILL and WALL missed exactness by deleting one trailing blank line.
- B FILL pair 3 changed `(#(str "/assets" %) "/command-center.css")` to
  `"/command-center.css"`, a semantic error rather than whitespace.
- B REPAIR pair 3 deleted one trailing blank line.
- FLAGSHIP's expected tree omitted the harness-created `.clj-surgeon.edn`.
  A and B also produced formatter-normalized source bytes that differed from
  the accepted historical fixture even though their product verification
  receipts reported committed, read-back, exact-exit lint success.
- The shared scorer enforced exact tree identity. That is stricter than, and
  not a substitute for, the semantic-equivalence scorer requested by the acid
  doctrine. A rerun should freeze a semantic scorer first and keep harness
  files outside the scored workspace.
- REPAIR did not reliably deliver the intended bad-expect refusal family, so it
  did not cleanly estimate the causal installed-refusal change.

These defects are not repaired post hoc. Under the frozen kill rules, they make
the current battery invalid and define the next battery's admission fixes.

## Protocol, identities, and session boundary

The preregistration, fixtures, prompts, 42-row rotating schedule, proxy, runner,
and scorer were frozen before the first model call at commit
`57ea69ea6fffcdd85ed63577c472a635062250b0`. The remote token-free preflight
reported `model_calls=0`. Immediately before launch, dev-a showed
`gpt-5.6-sol high`, subscription auth, 100% weekly quota, and no meter alarms.

The three arms were:

- A LATEST: origin `release/closed-relations-published` at
  `05f5a1962e5a0c5aa0365c673994eca9024c1a44`, on the `9af88fba` refusal lineage.
- B TWO-DAYS-AGO: exact divergent commit
  `19ab864889799b0028a5f7cb66c63b957ff7b973`.
- C NATIVE: the same frozen caller with no MCP server and only bounded shell
  reads, `apply_patch` or shell-native edits, and shell verification.

A and B each used a fresh private server per episode and the session-holding
stdio-to-HTTP proxy. The held session makes confirm+fill W1 possible here.
Per-call-session callers cannot use W1, as established by audit `c8b07168`;
this result must not be generalized to them.

Two fail-closed provisioning events occurred before any benchmark model call:
the initial dispatch expanded the frozen short SHA incorrectly, and the first
remote clone did not yet contain divergent arm B. Both refusals recorded
ordinal 0 and `model_calls=0`; the frozen harness bytes were not changed. Arm B
was then advertised temporarily under a benchmark provisioning ref and fetched
by exact SHA.

## Receipts and replay

Committed compact evidence and the full replay archive live in
[`bench/results/2026-08-31-threeway-acid-wall-battery-raw/`](../../bench/results/2026-08-31-threeway-acid-wall-battery-raw/):

- archive `57ea69ea-run1.tar.gz`:
  `b101f6dd0480740a6fda49993226d07f6c3cfab805726e5dfbaab6cb70a31cf0`;
- summary:
  `9a0ef212e9c34a954396f6fee3d0733bf3e4c03d7cecdece6a855fa094926784`;
- episode table:
  `70986675b9cbe6ce2f9811bbd7ca1a1069b652ae904c234886f73f4793b5d72a`;
- frozen scorer:
  `845e9a15b1f702c6fa93bed5b28c116ad8d6f89beae1f82c7e287458e0993ba4`;
- preflight receipt:
  `a1bac526ee8fd1a490f4920e7d2c21c00e2b85479d44c8b4298cca969496aaf0`.

The archive preserves absolute dev-a workspace paths in `meta.json`. A local
replay therefore relocates only that prefix in the extracted copy, then runs
the unchanged frozen scorer:

```bash
acid_replay_dir=$(mktemp -d)
tar -xzf bench/results/2026-08-31-threeway-acid-wall-battery-raw/57ea69ea-run1.tar.gz -C "$acid_replay_dir"
perl -pi -e "s#/srv/fleet/dev-a/clj-surgeon-threeway-acid-results/57ea69ea-run1#$acid_replay_dir/57ea69ea-run1#g" "$acid_replay_dir"/57ea69ea-run1/runs/*/meta.json
python3 bench/score_threeway_acid_wall_battery.py "$acid_replay_dir/57ea69ea-run1"
shasum -a 256 "$acid_replay_dir/57ea69ea-run1/summary.json" "$acid_replay_dir/57ea69ea-run1/episodes.tsv"
```

That replay reproduced the remote summary and episode table byte-for-byte at
the hashes above. `SHA256SUMS`, the full manifest, both prelaunch refusal
receipts, and the machine-readable `receipt.json` are beside the archive.
