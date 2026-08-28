# Captain's Log: Terminal Proof Ended the Second Plan

The five-times-native campaign crossed its frozen complete-wall gate locally on
August 27, 2026. The mechanism was not another editor primitive. It was making
an already-complete transaction terminal to the coding agent.

The retained correct-native control is 122.278 seconds. Five times faster is
therefore at most 24.456 seconds. The stable Surgeon route began at 37.871
seconds (3.23x), and the replicated tool-first request chord reached 27.949
seconds (4.38x). The remaining gap looked small, but it still contained two
different costs: exact verification was a second model-managed action, and the
model reconsidered how to narrate a mutation after Surgeon had already proved
it complete.

## Two independent kernel ratchets

SURGEON1 found that quoted-Var caller proof reparsed and retraversed the same
4,594-line source once for every moved Var. The replacement builds namespace
context once per candidate file, traverses once for all subjects, and preserves
the original ordered evidence. Focused adversarial witnesses cover duplicate
subjects, alias collisions, inert quoted and discarded forms, malformed input,
budget refusal, and failure atomicity. The warm extraction compiler median fell
from 5.555 seconds to 1.086 seconds.

SURGEON2 independently proved a closed exact-verifier contract. A
repository-owned profile supplies one exact argv and bounded timeout. Surgeon
runs it against staged, read-back bytes inside the transaction. Exit zero
retains the edit. Nonzero, timeout, launch failure, or crash-style outcomes undo
the edit and preserve the distinction between failed and unverified. The
request cannot supply an arbitrary command, and clj-kondo argv is not rewritten
into the older baseline/delta acceptance rule.

The two changes compose:

```text
complete extraction decision
          |
          v
one apply_clojure_changes call
          |
          +-- compile caller proof once per source
          +-- stage extraction
          +-- format staged bytes
          +-- run repository exact verifier
          +-- commit or exact-undo
          |
          v
terminal receipt: verification_complete=true, next_action=none
```

Independent ordinary-response canaries proved the fused transaction correct,
but did not cross the gate:

| Owner | Complete wall | Initial call materialization | Server apply | Receipt interpretation |
|---|---:|---:|---:|---:|
| SURGEON1 | 27.114s | 13.406s | 5.143s | 6.258s |
| SURGEON2 | 27.828s | 14.588s | 4.720s | 6.922s |
| Mean | **27.471s** | **13.997s** | **4.932s** | **6.590s** |

Both routes used exactly one MCP mutation, zero shell commands, zero reads, zero
refusals, and the same semantic scorer. This rejected the tempting claim that
verifier fusion alone had earned 5x. The now-cheap verifier was not the next
hill.

## Gene's silence question became the winning experiment

Gene had repeatedly watched Codex appear silent after successful tools and
asked: what is it doing? Event clocks showed that after a terminal mutation
receipt, Sol/high spent another six to seven seconds deciding how to report an
already-decided outcome.

The smallest reversible experiment added one benchmark-only success-response
switch. After a successful terminal receipt, the prompt supplied the exact
short response:

```text
Done — extraction and exact verification completed.
```

This does not skip verification, weaken correctness, hide a failure, or change
the edit. It removes a second planning episode only on the already-terminal
success path. Failures remain available for ordinary model interpretation and
recovery.

## Replicated crossing of the 5x gate

| Owner | Complete wall | Initial call materialization | Server apply | Receipt interpretation | Native speedup |
|---|---:|---:|---:|---:|---:|
| SURGEON1 | 22.993s | 15.307s | 4.101s | 1.563s | 5.32x |
| SURGEON2 | 20.637s | 12.969s | 4.608s | 1.503s | 5.93x |
| Independent N=2 mean | **21.815s** | **14.138s** | **4.355s** | **1.533s** | **5.61x** |
| SURGEON1 validation replica | 21.902s | 12.170s | 5.558s | 2.042s | 5.58x |

All three terminal-response runs were correct, used exactly one
`apply_clojure_changes` call, completed exact verification inside the atomic
transaction, and used zero shell or source-reading commands. The semantic
scorer reported `parseable=true`, `meaning-preserved=true`, 15 moved owners, 63
remaining caller occurrences, and no errors.

Against the ordinary fused N=2 mean, the terminal response saved 5.656 seconds
(20.6%). Receipt interpretation fell by 5.057 seconds while initial
materialization increased by only 0.141 seconds. Thus 89% of the observed
improvement landed at the exact boundary the experiment targeted. It did not
shift hidden work into the first decision.

## What was won, and what was not

We have locally replicated a 5x route on one frozen, representative historical
extraction. We have not proved a universal 5x product claim. The exact success
sentence is currently a benchmark affordance, not a public tool contract, and
the final promotion cohort must run on Anvil in both orders.

The result does establish a stronger design law:

> A terminal tool receipt should end the model's decision, not begin a second
> narration decision.

This is not an invitation to suppress useful user communication. The tool can
return a concise, already-human summary whose facts are mechanically derived
from the receipt. The model can relay it immediately. Genuine ambiguity,
failure, or unverified state must still return control to the model.

## Method notes

The breakthrough depended on the option-value and Kent Beck disciplines used
throughout the three-day campaign:

- Event clocks made Codex silence observable before anyone optimized it.
- SURGEON1 and SURGEON2 owned independent seams and merged evidence, not
  duplicated engines.
- Each experiment changed one boundary and retained the same task, model,
  scorer, route, and safety contract.
- Rejected ideas stayed rejected: generic `verify=fast`, broad rankers, compact
  plan handles, fewer schemas, and formatter work did not earn this hill.
- When a comparison was cumbersome, the harness gained a small reversible
  switch. That lowered the next decision cycle instead of encouraging manual
  one-off prompts.

In option-value terms, independent modules raised `N`, parallel owners raised
`K`, event clocks and frozen scorers reduced decision time `t`, and experiments
targeted the high-uncertainty seams where `sigma` was worth buying down.

## Anvil gate: the result survives both orders

Two Anvil lanes ran the same exact commit, model, task, scorer, and fused
transaction in opposite orders. The only prompt difference was the fixed
terminal-success sentence. Removing that last line reproduced the ordinary
prompt byte-for-byte.

| Lane and order | Ordinary fused | Terminal response | Saving |
|---|---:|---:|---:|
| dev-b, ordinary -> terminal | 23.984s | 20.401s | 3.583s |
| dev-c, terminal -> ordinary | 24.819s | 19.778s | 5.041s |
| Median | **24.402s** | **20.090s** | **4.312s (17.7%)** |

All four runs were semantically correct and route-adherent. Every run used one
MCP apply, zero shell calls, zero source commands, exact verification, and no
failures. All produced normalized diff SHA-256
`721dfd30e55e10353ee37c76962ca527c73fdad2404d82b7c25dbcfe08e9a14f`.
Both terminal arms cleared 24.456 seconds. Their median is **6.09x faster** than
the frozen correct-native control.

The four MCP receipts also read back identical files: source SHA-256
`6ed498052c8a30531047b1d1c9bd23c609bc32355403e8412b7cfda178a5f822`
and destination SHA-256
`bdaf9cdc5b748b22563c575d8a8278c3634ef8b44d2b187f4e23374ca9e9c0f1`.
The exact profile SHA-256 was
`c363fb29c8c5bbcbf7d8e8552a5ffd0b060140841aa55c020b7b033ec6a528ed`.
Normal and terminal prompt SHA-256 values were respectively
`b1468f14aea7c6bfdc585904ce6e9458ce47e4da6203dcbc75ca6bf179b36c13`
and `7dbed9caa9b740ad82131cf48e9b025d5ecec12b9207a1ab106ddd009340e16b`;
deleting the terminal prompt's final line reproduces the normal prompt exactly.

Both `anvil-exec` wrappers returned exit 1 after the completed experiment
because their final `git status --porcelain` considered an untracked
`.cpcache/` directory dirty. Every per-arm terminal receipt was complete with
exit 0, and tracked/index diffs were clean. This is a postflight harness defect,
not an experimental failure; fix that cleanliness check before reusing the
wrapper as a release gate.

The phase clocks again locate the mechanism:

| Median phase | Ordinary fused | Terminal response | Delta |
|---|---:|---:|---:|
| Initial call materialization | 13.277s | 14.040s | +0.764s |
| Server-authoritative apply | 2.025s | 1.895s | -0.130s |
| Receipt interpretation | 7.628s | 2.870s | **-4.758s** |

Initial materialization became slightly slower, and server time was effectively
flat. The entire useful effect remained at the terminal receipt boundary.

## Product gate: the tool now owns the chord

The public candidate projects one constant `terminal_response` only from a
complete apply receipt with project-owned exact verification. The visible MCP
receipt preserves elapsed time and its existing warnings, then says:

```text
If this mutation completes all remaining work, return exactly: Done — changes committed and exact verification completed.
If work remains, continue.
```

The benchmark success-response sentence was absent. A counterbalanced Anvil
cohort compared the first product field with the executable visible chord:

| Lane and order | Before chord | Executable chord | Saving |
|---|---:|---:|---:|
| dev-b, before -> chord | 27.822s | 21.428s | 6.394s |
| dev-c, chord -> before | 22.310s | 17.004s | 5.306s |
| Median | **25.066s** | **19.216s** | **5.850s (23.3%)** |

Both chord arms returned assistant text exactly equal to structured
`terminal_response`; both earlier arms paraphrased. All four mutations remained
semantically correct, meaning-preserving, and one-shot, with zero shell or
source commands. The product median is **6.36x faster** than the 122.278-second
native control. The fixed-prompt experiment had a 20.090-second median, so the
product affordance reproduced rather than merely approximated that result.

One final hardened local positive control completed in 22.038 seconds, returned
the field byte-for-byte, and remained 5.55x faster than native. Its visible
receipt included the explicit false branch without losing the win.

## Adversarial safety gate

The first implementation was fast but not fail-closed enough. Codex Sol found
that it accepted placeholder hashes, ignored contradictory failure and recovery
fields, did not require the complete exact-verifier receipt, and could project
the relay for an operation other than `apply_clojure_changes`. Permanent tests
now require:

- exact `apply_clojure_changes` operation identity;
- lowercase 64-character SHA-256 values for every read-back, receipt, profile,
  and verifier-output hash;
- complete exact argv, cwd, elapsed, output-byte, and truncation evidence; and
- absence of error, recovery, next-call, source-unchanged, and rollback
  contradictions.

`next_action=none` and `terminal_response` describe only the mutation. They do
not prove that the user's complete request is finished. Three clean compound
sentinel callers received the same terminal mutation receipt and still
performed a required second file-change action. Each reported the sentinel
instead of returning the short terminal sentence; every extraction remained
semantically correct. A separate report-only caller also continued and reported
the exact profile and elapsed time.

The original portfolio scorer reports `correct=false` for sentinel runs because
the deliberate extra file is outside its original two-file accepted result.
That is not the safety score. The retained events show a completed second
`file_change`, a non-terminal final answer, and a green extraction semantic
score in all three runs. The harness now accepts a bounded prompt suffix so this
compound falsifier is cheap to repeat.

## Release-gate resolution

The original LID gate asked for median receipt interpretation below 3.000
seconds. It **failed**: the Anvil product median was 3.154 seconds, a 154 ms or
5.1% miss. Local readings of 2.955 and 3.083 seconds straddled the line. This
criterion is not recorded as passed.

Both independent reviewers recommended an explicit intent correction. Receipt
interpretation includes model and scheduler latency, so its absolute value is
environmentally confounded. The user-visible absolute law remains complete
verified task wall. RELAY-005 now requires receipt interpretation to improve
against its contemporaneous counterbalanced pre-product control, while every
promoted arm must still finish in at most 24.456 seconds. No post-hoc percentage
floor was invented.

The amended law passes: receipt interpretation fell 5.693 -> 3.154 seconds
(44.6%), and the individual product arms completed in 21.428 and 17.004 seconds.
The product, compound-safety, cold MCP, and full repository gates are green.
The complete cold suite passes 617 core tests / 5,301 assertions and 243 MCP
tests / 2,060 assertions, plus smoke, heap, cclsp, skill, portfolio, harness,
retention, and evidence-manifest gates.

Before the live publication below, shared `:7888` had not been reloaded and the
candidate had not been installed. Publication claims remain scoped to this
frozen historical extraction cohort; this is not a universal sub-three-second
receipt claim.

## Live publication receipt

Immutable head `9ace623d365fe61258b502ac4809d250fbe16cac` was installed as the
stable CLI, Codex skill, and Claude skill. The managed routing block was updated
in both global agent instruction files. One hot reload returned
`status=:synchronized`, `ok=true`, `server-restart-required=false`, with
contract hash `0d4b26cb` -> `a41ca0f6`.

The shared server process remained PID 65458 with CWD
`/Users/genekim/src.local/clj-surgeon` before and after the reload. A
pre-existing Codex MCP session remained valid and completed an
`inspect_clojure` call with `read_complete=true` in 317.12 ms.

Live `tools/list` through `http://127.0.0.1:7888/mcp` proved that
`apply_clojure_changes` publishes:

- the terminal-response instruction in its description;
- `verify` enum values `fast`, `full`, and `exact`; and
- optional string output field `terminal_response`.

Two live mutation probes exercised both sides of the evidence law:

1. An exact-verified `edits` request normalized to public operation
   `edit_clojure`. It committed in 389.616 ms and correctly omitted
   `terminal_response`.
2. A fresh isolated extraction normalized to `apply_clojure_changes`. It
   committed and exact-verified in 1,438.363 ms, returned
   `verification_complete=true` and `next_action=none`, and published the fixed
   response in structured content. Visible content contained the conditional
   chord and the constant exactly once.

The true-branch read-back hashes were:

- `src/sample/source.clj`:
  `844a5485c804fa9c0b0defcaa30eafea8cbc51d2e74781ccdcd50dd8e359eb17`
- `src/sample/extracted.clj`:
  `d5c45b6433dd72be96a5f49d53027ca328716ac9347fac9369efea31fcd1dc25`
- receipt:
  `266611831031f83e7bfc5c02af110599d3b5340c3ca0e4e581d915343333e0bf`

The publication window ran from `2026-08-27T07:24:51Z` through
`2026-08-27T07:29:05Z`, 4 minutes 14 seconds. No pre-existing MCP session was
invalidated, the server did not restart, and `main` was not mutated. The named
rollback anchor is immutable commit `66b8a60`; restoration would use
`make install` from a detached worktree at that commit followed by one hot
reload.

## Greatest hits against native

The native control remains the same matched-correctness, same-model historical
15-form extraction at 122.278 seconds. The speed hill advanced in earned steps:

| Route | Complete wall | Relative to native |
|---|---:|---:|
| Native edit route | 122.278 s | 1.00x |
| Direct one-shot Surgeon extraction | 37.871 s | 3.23x |
| Fused exact verification, ordinary response | 27.471 s mean | 4.45x |
| Fused verification plus terminal response | 21.815 s mean | 5.61x |
| Promoted product cohort | 19.216 s median | 6.36x |

The win did not come from making one parser or formatter dramatically faster.
It came from deleting model-managed boundaries. Mechanical planning moved into
the frozen extraction transaction. Exact repository verification joined the
same rollback boundary. A complete terminal receipt then let the caller relay
one bounded response instead of spending another five seconds interpreting and
narrating already-proved evidence.

This is not a universal Surgeon-over-native claim. Native patching remains the
right fearsome competitor for a visible literal change. Surgeon wins when one
understood decision spans enough owners, callers, files, formatting, and proof
that native editing pays repeated decision and tool boundaries.

## Next hill: compile the operation proof dossier

A bounded review of the 12 highest-cost retained read chains classified 37
Surgeon reads and 25 possible read-to-read boundaries. Eight chains repeated
one product-shaped mission:

```text
public operation
-> registry / schema / handler
-> generic operation envelope
-> compiler or process
-> outcome / receipt normalization
-> declared witnesses
```

The next experiment is an oracle upper-bound test, not a new compiler. PRE gets
ordinary `inspect_clojure`; POST gets one precompiled single-call dossier for
the elapsed-time instrumentation path across the generic envelope and all
three public MCP tools. The model still decides architectural meaning. The
kernel may include only literal registry membership, declared handlers,
schemas, outcome maps, witness maps, exact source anchors, and one frozen
snapshot.

The hill stops unless POST is exactly correct, uses one Surgeon read with zero
fallback or follow-up reads, stays within 1.25x PRE's unique evidence bytes,
and completes at least 30 percent faster. Generic dependency closure is not
the first experiment: the current `:ls-deps` feasibility probe mis-resolved a
named definition to a later anonymous top-level expression, so it has not
earned authority.
