# GPT-5.3-Codex-Spark is fast, exact on writes, and surprisingly weak at the read schema

## Verdict

`gpt-5.3-codex-spark` is reachable on the ChatGPT subscription route and is
meaningfully faster than Sol on the trivial matched screen, but “very fast” is
modest rather than magical at whole-turn scale: median wall was **3.954 s**
versus **5.185 s** for `gpt-5.6-sol`, a **1.31×** Sol/Spark ratio or **23.7%**
lower Spark wall.  Spark won all three pairs by 0.405, 1.574, and 1.478 seconds.

As a Surgeon caller, the split is sharp:

- guarded writes were **3/3 exact one-shot**;
- deliberate refusal recovery was **3/3 exact in exactly two calls**;
- structural reads were ultimately correct but **0/3 one-shot**, with 12 read
  calls and 9 avoidable schema refusals;
- the six-edit/two-file choice cell selected the native route **3/3**, reached
  exact bytes **3/3**, but needed 3, 1, and 9 shell actions;
- wrong-subject was **0/12** across all Surgeon cells.

This is **synthetic screen evidence**, not acid-test proof.  The doctrine at
`8539e7ea4ed9df8dc1e51b057ba58f9760e2a28e` reserves performance proof for a
matched serial Anvil comparison on a real historical decision.

## Availability was a naming trap

The three names in the request all failed before a model response:

| requested name | exit | wall | result |
|---|---:|---:|---|
| `gpt-5.3-spark` | 1 | 3.768 s | unsupported with ChatGPT account |
| `5.3-spark` | 1 | 3.559 s | unsupported with ChatGPT account |
| `spark` | 1 | 3.825 s | unsupported with ChatGPT account |
| `gpt-5.3-codex-spark` | 0 | 5.761 s | exact `SPARK_OK` |

The refreshed `codex debug models` catalog exposed the accepted canonical name
as `gpt-5.3-codex-spark`, display name `GPT-5.3-Codex-Spark`, description
“Ultra-fast coding model.”  The alias amendment was committed before the first
successful evaluated model turn.  Both pre-amendment failures and the complete
accepted run are retained.

## Matched trivial speed screen

All six runs were exact.  Reasoning effort was `high`; each used a fresh Codex
home, no MCP, identical paired prompts, and serial Spark/Sol, Sol/Spark,
Spark/Sol order.

| Pair | Spark wall | Sol wall | Sol − Spark |
|---:|---:|---:|---:|
| 1 | 4.186 s | 4.591 s | 0.405 s |
| 2 | 3.611 s | 5.185 s | 1.574 s |
| 3 | 3.954 s | 5.432 s | 1.478 s |
| **median** | **3.954 s** | **5.185 s** | **1.478 s paired median** |

Token receipts also differ:

| Median | Spark | Sol |
|---|---:|---:|
| input tokens | 10,977 | 14,157 |
| output tokens | 63 | 8 |
| reasoning output tokens | 53 | 0 |

Spark used less input context but emitted substantially more output/reasoning
for the same exact one-line result.  Wall remained lower, so the extra decode
did not erase its latency advantage in this tiny class.

## Surgeon screen

Every MCP cell used a fresh isolated server from product commit `c55de227`,
tree `565f009f0ff25fdedbc2fba5ad9ba5f55783e023`.  Bootstrap was recorded but
excluded from model wall.

| Cell | Exact final | Strict one-shot | Calls/actions | Median wall | Median input | Median output |
|---|---:|---:|---|---:|---:|---:|
| structural read | 3/3 | 0/3 | 6, 3, 3 inspect | 10.578 s | 82,586 | 2,462 |
| guarded write | 3/3 | 3/3 | 1, 1, 1 edit | 7.671 s | 39,258 | 1,400 |
| refusal recovery | 3/3 | 3/3 recovery shape | 2, 2, 2 edit | 11.215 s | 54,924 | 1,868 |
| six-edit chord | 3/3 | 1/3 one-action | 3, 1, 9 shell | 18.106 s | 54,776 | 7,146 |

“Strict one-shot” for recovery means precisely the preregistered two-call
causal sequence: one expected refusal and one corrected successful mutation.

### Structural read: correct destination, terrible route

Spark never adopted the new operation-less forms request unprompted: **0/3**
first calls omitted `operation`.  It did omit request IDs in **2/3** first
calls, so shorthand adoption was partial.

The three runs accumulated **5, 2, and 2** `invalid-mcp-request` refusals before
success.  Misuses included:

- illegal top-level `file`, `verify`, `scope`, and `intent` fields;
- missing nested request `expect`;
- missing top-level aggregate `expect` fields; and
- an aggregate `requests=2` count for a one-request batch.

Each run eventually issued the conventional explicit-operation shape and
returned both exact forms, including the `#()` reader shorthand in
`normalize-record`.  The workspace was unchanged, but the model did not handle
the newest tolerant schema economically.

### Guarded write: the compact grammar fit Spark

All three calls used the intended compact object: file, `within.form`, exact
`from`, exact `to`, and `matches=1`.  All three committed exact expected bytes
in one call with no read, shell action, failed mutation, or post-success tool
call.  Direct MCP elapsed was 129.3, 111.7, and 112.8 ms, versus 10.084, 7.671,
and 7.289 seconds end to end.  The median complete wall was about **68×** the
median editor time: the verification-heavy kernel was not the bottleneck.

### Refusal recovery: the causal law worked perfectly

All three first calls used the required wrong count of two.  Surgeon returned
`expect-count-mismatch`, `expected_count=2`, `actual_count=1`,
`source_unchanged=true`, and a remedy.  Spark immediately retried with
`matches=1`, without a read or shell call, and reached exact bytes in every
run.  Direct MCP sums were 243.1, 156.3, and 121.0 ms, versus 10.760, 11.215,
and 12.438 seconds end to end.  The median complete wall was about **72×** the
direct tool time.

One small wrinkle was safely contained: recovery run 1's refused call emitted
`to=" :complete"` with a leading space, then corrected it on retry.  Because
the count guard refused before writing, exactness was never at risk.

### Mid-size chord: Spark preferred native and became volatile

Spark selected native shell mutation in **3/3** trials even though the complete
four-tool Surgeon catalog and a fully supplied decision were visible.  Final
bytes were exact and wrong-subject remained zero.

- Run 2 was the clean fast path: one compound `perl` mutation/verification
  action, 11.079 seconds.
- Run 1 applied the mutation in its first compound command, but an unsupported
  `rg` lookahead made the command exit 2; a second verification also exited 1,
  and the third succeeded.  Total: 3 actions, 18.106 seconds.
- Run 3 tried malformed/range-mismatched `ed` commands, attempted Git commands
  in a non-Git fixture, inspected its location, and finally used `perl`.
  Total: 9 actions, 34.165 seconds.  Spark itself correctly reported that the
  first mutation attempt failed.

The 18.106-second median is numerically below the recorded Sol/high context for
the identical six-edit fixture: local full-catalog MCP walls 24.547, 23.142,
and 24.907 seconds (median 24.547), and later Anvil medians 29.893 seconds
compact versus 31.378 seconds native.  This is **context only**: different
dates, caller models, routing behavior, and harness generations make it neither
a matched comparison nor a performance claim.  See
[the compact-editor pilots](2026-08-24-compact-editor-versus-native-pilots.md)
and [the performance timeline](2026-08-28-performance-vs-native-timeline.tsv).

## SURPRISES

1. **The public name was wrong.**  All intuitive aliases failed; only the
   catalog's `gpt-5.3-codex-spark` worked.
2. **Latency was real but bounded.**  Spark was faster in all three trivial
   pairs, yet only 1.31× on median whole-turn wall—not an order-of-magnitude
   change.
3. **Read-schema competence was the weakest cell.**  The supposedly forgiving
   newest schema produced 9 avoidable refusals, and the operation-less shorthand
   was adopted 0/3.
4. **Write-schema competence was excellent.**  Exact guarded writes were 3/3
   one-shot, despite the read failures immediately before them.
5. **Refusal vocabulary transferred unusually well.**  Spark recovered from
   `actual_count=1` in exactly one retry 3/3, with no compensating read.
6. **The fast caller avoided Surgeon when given a choice.**  On the mid-size
   chord it chose native 3/3, and native action count ranged from 1 to 9.  The
   fastest sample was excellent; the slowest nearly tripled it.
7. **Tool execution was tiny relative to thinking.**  Median read, write, and
   recovery tool time was roughly 0.089, 0.113, and 0.156 seconds, while model
   walls were 10.578, 7.671, and 11.215 seconds.  Verification-heavy Surgeon did
   not erase Spark's speed; request construction and recovery dominated.
8. **Safety held throughout.**  Despite schema flailing and native command
   failures, final outcomes were exact 12/12 and wrong-subject was 0/12.

## Receipts and replay

Compact evidence and SHA-256-manifested raw streams are in
[`bench/results/2026-08-30-spark-caller-screen`](../../bench/results/2026-08-30-spark-caller-screen/README.md).
The main raw archive SHA-256 is
`25406e5cd5bcd8c30c70a70f5404b31d3901f2da5f50384189873eab1ea85781`;
the pre-amendment alias archive is
`fa45a7189fbe41e2641825a9c098c1f2258be50672280692b2c6bf19607a6d96`.
The exact replay command is retained beside them.

Repository verification ran the 647-test fast suite and four analyzer-contract
tests with zero failures.  The first MCP suite run had two timing-sensitive
failures in `cold-clj-kondo-admission-timeout-is-unverified`: the test expected
an admission timeout but observed delegated/unverified.  The immediate exact
`make mcp-test` rerun passed all 300 tests / 3,433 assertions.  No product
source was changed in response.

---

## Addendum (mayor@skiff, 2026-08-30 evening) — externally reported specs, and the reconciliation

Gene supplied an external report on Spark's design intent and streaming rates. These figures
are RELAYED VENDOR/THIRD-PARTY CLAIMS, not measured by this repo's instruments (the meter
rule applies — treat as unverified until measured on our route):

> RELAY (Gene, quoting external reporting, 2026-08-30): "OpenAI reports Spark at 1,000+
> tokens/sec, and subsequently said it had increased to 1,200+ tok/sec... Independent recent
> measurements put ordinary Sol around 244 tok/sec... OpenAI describes Sol as the flagship
> for complex/long-horizon work and Spark as the real-time model... Spark ≈ 4–5× faster at
> emitting tokens, and often 5–10× faster wall-clock for a small coding iteration."

### Reconciling 4–5× reported streaming vs our measured 1.31× end-to-end

Our trivial-prompt cells measured 1.31× (3.954s vs 5.185s) — far below the reported 4–5×
streaming advantage. These are different quantities, and both can be right:

- Our cells emitted TINY outputs (one-sentence responses). End-to-end wall on short
  emissions is dominated by fixed costs — session startup, prompt processing, harness
  overhead — which the decode-rate advantage cannot touch. A 4–5× decode advantage on ~30
  output tokens saves tens of milliseconds inside a multi-second floor.
- The reported 5–10× wall advantage applies to iterations with SUBSTANTIAL output, where
  decode time dominates. None of our cells were in that regime.
- Implication for this screen's numbers: our 1.31× is a FLOOR specific to short-output
  work, not a refutation of the vendor figures. A long-emission cell (e.g., a wall-class
  write) would be the discriminating measurement if the ratio ever matters to a decision.

### The strategic reconciliation — why the reported design intent strengthens this screen's finding

The external positioning ("real-time model... lightweight targeted edits" vs Sol for
"complex/long-horizon work") matches this screen's inverse surprise exactly: Spark was
flawless on the strict guarded surfaces (3/3 one-shot writes, 3/3 one-turn refusal
recoveries, and 2/2 exact on the 64-byte rename verb in the sibling screen — where Sol
went 4/6 by freelancing its own schema) and unreliable on the permissive read schema.

Two consequences worth recording:

1. **The emission-compression program and Spark are convergent, not competing.** Grammars
   that shrink output (splices, prepared holes, verbs) reduce the raw-streaming advantage
   (less to stream) while INCREASING Spark's reliability fit (stricter surfaces are where
   it excels). The division of labor that falls out matches the vendor's own: judgment
   models design and diagnose; Spark-class models execute prepared, guarded, hole-filling
   bangs. The measured evidence for that split now exists on both sides.
2. **Per-token pricing of the direction asymmetry differs by caller.** At a reported
   ~1,000–1,200 tok/s decode, Spark's output tokens are ~4–5× cheaper in wall-clock than
   Sol's ~244 tok/s. Emission-savings measured in seconds must therefore state their
   caller: a 400-token saving is ~1.6s on Sol-class decode and ~0.3–0.4s on Spark-class.
   Turn elimination is caller-invariant; byte shaving is not. One more reason the turn
   family outranks the byte family in the opportunity map.
