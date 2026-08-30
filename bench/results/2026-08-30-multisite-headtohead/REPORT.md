# Multi-site head-to-head report

## Verdict

The preregistered claim is **unsupported on this fixture**. Surgeon was not
faster in either primary metric. It emitted a larger median mutation payload
and took more than twice as long as native patching.

All 12 confirmatory episodes were environment-valid, byte-exact, and
route-adherent. The kill criterion did not trigger.

| Primary result | Native `apply_patch` | Surgeon `edit_clojure` | S - N | `(S - N) / N` |
|---|---:|---:|---:|---:|
| Median mutation-payload bytes | 607 | 1,133 | +526 | +86.66% |
| Median mutation-payload tokens | 171 | 318 | +147 | +85.96% |
| Median wall time | 9.054 s | 21.402 s | +12.348 s | +136.38% |
| Median mutation calls to success | 1 | 1 | 0 | 0% |

Native succeeded in one mutation call in 6/6 episodes. Surgeon succeeded in
one call in 5/6 episodes and made one retry in total. Thus, the registered call
prediction cleared, while both direction and magnitude predictions for payload
and time failed.

## Episode results

The primary token count is the model-emitted mutation-tool argument, encoded
with `o200k_base`. It excludes final prose and other provider output.

| Episode | Arm | Payload bytes | Payload tokens | Wall seconds | Calls | Retries | Valid / exact / adherent |
|---:|:---:|---:|---:|---:|---:|---:|:---:|
| 1 | N | 607 | 171 | 9.254324 | 1 | 0 | yes / yes / yes |
| 2 | S | 1,133 | 318 | 21.263092 | 1 | 0 | yes / yes / yes |
| 3 | S | 465 | 132 | 17.172115 | 1 | 0 | yes / yes / yes |
| 4 | N | 651 | 183 | 9.159303 | 1 | 0 | yes / yes / yes |
| 5 | S | 1,133 | 318 | 21.540913 | 1 | 0 | yes / yes / yes |
| 6 | N | 607 | 171 | 10.380677 | 1 | 0 | yes / yes / yes |
| 7 | N | 607 | 171 | 8.530506 | 1 | 0 | yes / yes / yes |
| 8 | S | 2,258 | 632 | 29.323731 | 2 | 1 | yes / yes / yes |
| 9 | N | 607 | 171 | 8.290067 | 1 | 0 | yes / yes / yes |
| 10 | S | 1,133 | 318 | 23.269862 | 1 | 0 | yes / yes / yes |
| 11 | S | 311 | 84 | 14.596589 | 1 | 0 | yes / yes / yes |
| 12 | N | 607 | 171 | 8.949124 | 1 | 0 | yes / yes / yes |

Every final source has SHA-256
`08ae69de42b1f17dfe854a2e8c63503cb90290c94b2438d95981d7cecde89348`,
the registered byte oracle.

## What produced the difference

Native emitted a compact seven-hunk patch. Five of its six payloads were 607
bytes and 171 tokens.

Surgeon showed two argument strategies:

- Four of six sessions enumerated the definition and five referencing owners.
  A successful call of this shape was 1,133 bytes and 318 tokens.
- Two sessions used a root-scoped `matches=6` rename plus one docstring edit.
  Those compact calls were 465 bytes / 132 tokens and 311 bytes / 84 tokens.

The shorter root-scoped calls show the mechanism behind the original claim,
but the model selected that shape in only 2/6 sessions. Five Surgeon sessions
also emitted the optional absolute `workspace_root`. The shortest call omitted
it. Episode 8 first supplied the docstring without Clojure string quotes. The
guard rejected the request without changing the source, and the corrected
second call succeeded.

The successful Surgeon kernel calls had a median reported execution time of
0.207 seconds. Therefore, the 12.348-second median wall gap did not come mainly
from applying the edit. It arose before and around the call: constructing the
larger request, producing more total model output, processing the tool surface,
and completing the MCP exchange.

The preregistered cheap screens point the same way but are not an additive
causal decomposition:

- The median argument gap was +526 bytes. At 3.5237 ms per emitted byte, that
  corresponds to a 1.853-second Surgeon penalty.
- The median argument gap was +147 tokens. At 56.5 decoded tokens per second,
  that corresponds to 2.602 seconds.
- Median provider-reported total output was 257.5 tokens for N and 727.5 for S.
  The +470-token gap corresponds to 8.319 seconds at the same decode screen.

The large common input did not rescue Surgeon, consistent with the registered
1,284x input/output asymmetry screen.

## Statistical scope

The exact label-permutation descriptions were `p=0.060606` for mutation-payload
tokens and `p=0.012987` for wall time. They are secondary to the preregistered
median gates. In particular, the token result is not evidence of equivalence.

With `n=6` per arm, the registered equal-variance normal approximation has 80%
power only for a standardized effect of at least 1.79554. The observed median
directions are opposite the claim, but this small experiment does not estimate
a stable population effect over other edit shapes.

## Would this survive task-shape variation?

No general effect is established. The result is strong for this exact 137-line,
six-occurrence fixture, model, tool schemas, and warm-server protocol, but its
magnitude should not be projected unchanged.

- **Reference count:** Native patch payload grows with added hunks. A compact
  root-scoped Surgeon rename can stay nearly constant, so a crossover at a
  larger occurrence count is plausible but unmeasured. Owner-by-owner Surgeon
  arguments also grow with reference count and were larger here.
- **Owner spacing:** Wider spacing adds patch anchors and may favor a root
  match. It has little effect on root-scoped Surgeon arguments, but it does not
  remove model or MCP overhead.
- **Identifier length:** Both routes repeat old and new identifiers. The verbose
  owner-scoped Surgeon shape repeats them once per edit and may grow faster than
  the patch.
- **Docstring size:** Both routes emit old and new strings. Changing its length
  is unlikely by itself to reverse this result.
- **Retries:** One guarded rejection doubled one Surgeon episode's mutation
  payload and increased its tail latency. Different retry rates can materially
  change both medians in a small sample.
- **Server warmth:** JVM startup was excluded. Including the observed 4.3-5.2
  second starts would make Surgeon slower here. A persistent Codex-to-MCP
  connection could reduce handshake overhead, but this experiment did not
  measure it.

The honest routing conclusion is narrow: at five references plus a docstring,
do not route to `edit_clojure` for speed or emitted action-token savings unless
the compact root-scoped request is already determined for another reason. Test
a preregistered reference-count sweep before claiming a general crossover.

## Launch audit and receipts

Two earlier fixed schedules are retained as killed launch cohorts. The first
failed on obsolete `update_plan_enabled`. The second failed on rejected
`tools.view_image`. All 24 processes stopped during strict config parsing, before
any executor-model event, hook, provider usage, or mutation. Their zero payloads
and sub-second process times are not combined with the confirmatory cohort.

The final preregistration boundary is
`fef881231a44561624084a7b35e87919270182e7`. Raw prompts, catalogs, tool-hook
payloads, Codex event streams, MCP telemetry, final sources, per-episode scores,
aggregates, config preflights, and SHA-256 manifests are stored beside this
report. `REPLAY-EXEC-VALID.md` contains the final verification and reproduction
commands.
