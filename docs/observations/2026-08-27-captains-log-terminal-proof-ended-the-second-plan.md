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

## Next gate

Run the same-commit fused route on two Anvil lanes in opposite order, changing
only whether the terminal success response is explicit. Require every promoted
run to be semantically correct, one-shot, terminally verified, and at or below
24.456 seconds. Preserve complete phase clocks and exact evidence hashes. Only
then decide how to project the terminal success summary into the public MCP
contract and agent routing.

