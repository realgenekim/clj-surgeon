# Brain Fleet: The Next Five Hills

**Date:** 2026-08-27

**Stable baseline:** `stable-cross-caller-6.37x-min-20260827`

**Decision:** Preserve the Sol/high direct one-apply extraction route. Spend the
next cycle on measured boundary deletion, fleet safety, and external validity.

## Method

The review inventoried 58 plans, 90 observation documents, both active intent
documents, the README performance record, and 37 open Beads issues. A second
inventory deduplicated the full experiment portfolio. Codex Sol/high and Claude
Fable/high then ranked the same evidence independently.

The first open-ended Fable file-reading route was stopped after fourteen
minutes because it was still paying per-document tool boundaries. Fable then
received the complete evidence brief with all tools disabled. That change is
itself evidence for the leading compiled-read hypothesis: precompile the known
questions and let the model decide once.

The ranking law was:

```text
measured removable wall or failure consequence
  × frequency and caller reach
  × probability of a cheap truthful falsification
-------------------------------------------------
implementation risk + safety uncertainty + tax on the proven Sol route
```

## Consensus

Sol and Fable selected the same five hills. Their only disagreement was order.

| Hill | Sol rank | Fable rank | Measured basis | Cheapest next action |
|---|---:|---:|---|---|
| Tolerant one-shot compilation | 5 | 1 | One semantic 3-change decision paid three pre-write attempts; each recovery crosses an approximately nine-second model boundary. | Pure replay of the three retained payload failures. Normalize only the two representational cases; malformed parentheses must still refuse. |
| Compiled read-decision chord / operation-proof dossier | 1 | 3 | 75 Surgeon-read-to-Surgeon-read transitions consumed 1,084s; direct tool median 0.243s versus roughly 9.1s to next action. Generic batching and continuation already missed gates. | Offline oracle replay first: prove at least half of second reads were knowable at the first call. Then one PRE/POST dossier screen with a 30% keep gate. |
| Host-wide analyzer admission and coalescing | 2 | 2 | Five launches in 48.658s helped drive load 20.7 to 118.5. A naive cooldown would make a 32-launch suite take at least 31 minutes. | Finish the real two-process race after the 20-test/104-assertion pure/fake proof. Do not add a generic scheduler. |
| Syntax-first Var-surface ladder | 3 | 4 | Three LSP sessions served four requests; 83.46% of LSP wall was initialization. Syntax was terminal in two of four retained cases; bounded clj-kondo reproduced all four. | Run the already-specified 20-request exactness/cold-wall cohort after analyzer admission is trustworthy. |
| Generalize decision chords beyond the hero extraction | 4 | 5 | Large structural tasks produced 5.80x and 6.37x--9.69x wins; six small replacements produced only 1.05x. | Mechanically sample six historical tasks across at least three non-extraction shapes, including a native-positive small edit. |

## Recommended sequence

### Now: tolerant one-shot replay

This is the smallest change with a direct measured prize. Replay the exact
three-change/two-file incident:

1. two complete inserted `deftest` forms supplied in one string;
2. one inserted form with an extra closing parenthesis;
3. an aggregate edit count that disagreed with the mechanically compiled
   intents.

The target is not permissiveness. The compiler may split several complete
forms and derive redundant bookkeeping when every exact per-edit guard passes.
It must still refuse malformed syntax at the exact form and parser location.
It must never auto-balance parentheses, choose an owner, or change architecture.

Keep only if the two representational cases compile to the exact eventual
future bytes, the malformed case still refuses, and the full malformed-input
corpus remains green. This is `clj-surgeon-tmr.6`.

### In parallel: finish analyzer admission

The stable install already routes coding agents through the machine-wide
analyzer gate, but the remaining real multi-process contract matters. It is a
P0 fleet-safety ratchet and protects every later benchmark from host-load
confounding.

Keep only if two independent JVMs never overlap the analyzer, waiting and
cancellation are bounded, owner death releases the OS lock, exact-verifier
rollback is unchanged, and a leased serial test mission avoids the rejected
31-minute cooldown. This is `clj-surgeon-qg2`, feeding `clj-surgeon-it1`.

### Before code: falsify the read dossier offline

The dossier owns the largest possible prize but has two failed ancestors:
generic batched reads were only 13.8% faster than CLI and missed their 30% keep
gate; selector continuation became slower despite ideal two-call geometry.

Do not build a graph compiler first. Add privacy-safe target/snapshot hashes
and batch cardinality to retained clocks, then classify the 75 repeated-read
transitions:

- mechanically knowable at the first request;
- knowable only after model judgment;
- caused by hidden/insufficient result presentation;
- repeated because the caller distrusted terminal evidence;
- unrelated sequential investigation.

Proceed only if at least half of the second reads are mechanically groupable.
The oracle PRE uses ordinary inspection; POST receives one precompiled
operation-proof dossier. Require exact correctness, one read, zero fallback,
no more than 1.25x PRE unique evidence bytes, and at least 30% lower complete
wall. Deferred intent already exists as `MCP-OP-READ-MISSION-001/002`.

### Then: syntax-first semantic escalation

Use the frozen workspace snapshot to resolve definitions, fully qualified,
alias-qualified, and quoted-Var references. Publish exact bare-symbol gaps.
Escalate protocols, macros, locals/shadowing, classpath symbols, call hierarchy,
generated relations, and hard reader conditionals.

On 20 reconstructed requests, require at least 60% to terminate without cclsp,
every published location exact, and cold complete wall at least 30% lower. If
the 60% gate misses, preserve the evidence scanner but stop the replacement
claim. This is `clj-surgeon-tmr.8`.

### Finally: test whether the product generalizes

The cross-caller extraction is a decisive existence proof, not a distribution.
Mechanically sample commits before inspecting tool fit. Include extraction,
rename/caller migration, namespace surgery, heterogeneous batch edits,
movement, and one small native-positive edit. Use current APIs before adding a
primitive.

Retain every loss. Require at least five of six tasks correct, four materially
faster, and no 2x cross-task claim before eight tasks. This is
`clj-surgeon-tmr.7` and the commit-counterfactual portfolio.

## CLI and MCP: important, but a separate program

CLI/MCP convergence did not make the top five as a direct speed hill because
its isolated performance prize is not yet proven. It is a high-leverage
cost-of-change multiplier and now has a dedicated
[gap analysis and experiment plan](cli-public-operation-envelope-gap-analysis.md).

Current honest evidence:

- closest matched read: MCP 27.969s versus CLI 32.442s, only 13.8% faster;
- MCP used one call versus ten CLI shell calls and returned 27.4% fewer bytes;
- installed-skill mutation routes measured MCP 52.275s/three actions versus
  CLI 82.519s/12 actions, but coaching, help, references, and quoting were
  confounded;
- inline EDN over-escaping and invalid scalar replacement are real CLI failure
  modes, while `:spec-file -` removed one shell layer and later dogfood became
  one-shot;
- MCP terminal evidence cut receipt interpretation from 6.590s to 1.533s;
- MCP discovery is also a tax: historical callers used CLI before or instead
  of discovering the available MCP.

Therefore the shell-escaping-boofarama hypothesis is narrow and falsifiable:
it likely matters most for heterogeneous and SCI payloads, but is not yet a
general explanation of MCP performance.

SURGEON2 owns the comparison. The first experiment is no-model differential
proof:

```text
canonical intent
  -> CLI renderer / shell or stdin / parser
  -> MCP normalizer
  -> identical compiled IR, domain outcome, and future hashes
```

Only after an exact-commit Linux CLI launcher exists should Anvil run serial
Sol/high pairs. Start with batched read, generic `change`/`change!`, and shared
selector refusal. Add compact edit, SCI, extraction, verification, and undo only
after semantic parity exists. Preserve CLI wins. Stop as parity after two
correct counterbalanced pairs differ by less than 10% with identical geometry;
require at least a 20% complete-wall win or a material correctness/recovery
advantage for a route recommendation.

The architecture hypothesis is one operation algebra, canonical domain
outcome, and trusted entrance policies with thin projections. It is not CLI
over HTTP and not a giant universal schema. The smallest refactor shadows
`change` preview and then `change!`, requiring byte-compatible CLI behavior,
identical future hashes, one compiler/commit, no new process, and no more than
5% CLI p50/p95 regression.

## Binding stop ledger

Do not reopen these without new causal evidence:

| Idea | Why stopped |
|---|---|
| Fewer MCP schemas | Reduced catalog text 90.7% but was 15.6% slower. |
| Mandatory public planning | Added one model boundary and 12.070s to a complete decision. |
| Compact `plan_id` handle | Reduced visible bytes but missed decision-time and keep gates. |
| Generic `verify=fast` | Changed the repository's warning and exit semantics. |
| Broad fuzzy ranker | Added roughly 780 lines without adding authority; complete owner vocabulary carried the value. |
| Faster/cheaper post-decision model | Saved only 2.853s in a confounded model-plus-effort screen. |
| Formatter as the extraction prize | Formatter work was not the remaining 13-second interval. |
| Selector continuation as a speed feature | Correct two-call route was 3.958s slower; retain safety, stop the speed claim. |
| Generic dependency closure for dossiers | Existing `:ls-deps` mis-resolved a named definition to a later anonymous expression. |
| Universal SCI/editor catalogs | Repeated-site scaling was promising, but small edits did not earn the ceremony and broad transforms risk comments. |
| More native prompt golf | Native repeatedly rebuilt one-off structural editors; the campaign optimizes Surgeon. |

## Course

The next move is intentionally two-speed:

```text
fast local proof:  tolerant one-shot replay
parallel safety:   real analyzer-admission gate
zero-code oracle:  classify 75 repeated-read transitions
then build:        only the mechanism whose gate clears
```

This preserves the magical method of the last three days: maximize independent
options, make each decision cycle cheap, retain ugly counterevidence, and let a
small verified result earn the next architectural step.

## Sources

- [Terminal proof ended the second plan](../observations/2026-08-27-captains-log-terminal-proof-ended-the-second-plan.md)
- [The model boundary dwarfed the scalpel](../observations/2026-08-27-captains-log-the-model-boundary-dwarfed-the-scalpel.md)
- [Nine seconds is the agent boundary](../observations/2026-08-27-captains-log-nine-seconds-is-the-agent-boundary-not-surgeon.md)
- [Agent usage release window](../observations/2026-08-27-agent-usage-release-window.md)
- [Analyzer took the lock with it](../observations/2026-08-27-captains-log-the-analyzer-took-the-lock-with-it.md)
- [Analyzer trigger taxonomy](../observations/2026-08-27-clj-kondo-trigger-taxonomy-and-test-pyramid.md)
- [Language server was mostly starting itself](../observations/2026-08-27-captains-log-the-language-server-was-mostly-starting-itself.md)
- [Var surface was a small index](../observations/2026-08-27-captains-log-var-surface-was-a-small-index.md)
- [Next-hill experiment portfolio](2026-08-26-next-hill-experiment-portfolio.md)
- [Representative read portfolio](representative-read-portfolio.md)
- [Representative edit portfolio](representative-edit-portfolio.md)
- [Commit-counterfactual replay](commit-counterfactual-replay.md)
- [Cross-caller MCP extraction benchmark](cross-caller-mcp-extraction-benchmark.md)
