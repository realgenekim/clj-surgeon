Model: gpt-5.6-sol | reasoning: low | profile: clean | codex: codex-cli 0.147.0
Host: anvil-server | 16 cores | loadavg at start: 1.12 1.10 1.17

| Condition | n | median wall | MAD | min | max | input tok | decoded tok |
|---|---|---|---|---|---|---|---|
| A | 0 | n/a ms | n/a ms | n/a ms | n/a ms | n/a | n/a |
| B | 9 | 25742.0 ms | 210.0 ms | 25518.0 ms | 26705.0 ms | 14423.0 | 1239.0 |
| C | 9 | 3854.0 ms | 357.0 ms | 3497.0 ms | 4910.0 ms | 14403.0 | 5.0 |

FLOOR (condition C): 3854.0 ms, MAD 357.0 ms, n=9
  of which local process startup to turn.started: 314.7 ms

PREFILL: n/a marginal input tokens in n/a ms -> n/a tok/s (lower-bound, resolved=false)
DECODE:  1234.0 marginal decoded tokens in 21888.0 ms -> 56.4 tok/s (point)
RATIO:   n/ax (unresolved)

COPY VERSUS COMPOSE
| Cond | Kind | n | route-adherent | not reproduced | decoded tok | delta | tok/s |
|---|---|---|---|---|---|---|---|
| B | compose | 9 | 1.0 | 0 | 1234.0 | 21888.0 ms | 56.4 |
| D | copy/unpredictable | 9 | 1.0 | 0 | 1228.0 | 22757.0 ms | 54.0 |
| E | copy/same-content | 9 | 1.0 | 0 | 1198.0 | 22167.0 ms | 54.0 |

  copy(unpredictable) / compose = 0.96x  [token counts matched: true; 1228.0 vs 1234.0 tokens]
  copy(same content) / compose = 0.96x  [token counts matched: true; 1198.0 vs 1234.0 tokens]
  copy(unpredictable) / copy(predictable) = 1.0x  [token counts matched: true; 1228.0 vs 1198.0 tokens]

Confounds this measurement cannot separate:
  - Wall clock includes network transfer of the prompt. Condition A ships ~1 MB, condition C ships ~1 KB, so (A - C) contains upload time as well as prefill. Prefill rate is therefore a LOWER bound even when the delta resolves.
  - Server-side batching and queueing are invisible from the client. A turn may wait behind other tenants; that time lands in the floor and in both deltas.
  - Provider prefix caching is defeated for condition A's filler (fresh random words per replicate) but the codex system prompt and tool definitions are cached identically across all conditions, so the cached prefix cancels in the subtraction.
  - Reasoning tokens are counted as decode because they are produced serially, but they are not visible, so condition B's decode total is trusted from the provider's own usage report rather than from the text.
  - Token counts come from the provider's usage report, not from a local tokenizer. They are authoritative for billing and are assumed authoritative for work.
  - A single provider, model, and datacentre. Nothing here generalises to other hardware, other models, or the same model under different load.
  - codex exec is an agent wrapper, not a raw completion. The floor includes CLI process start, config load, and session setup, which a raw API call would not pay.
