Model: gpt-5.6-sol | reasoning: low | profile: clean | codex: codex-cli 0.147.0
Host: anvil-server | 16 cores | loadavg at start: 1.22 1.06 1.08

| Condition | n | median wall | MAD | min | max | input tok | decoded tok |
|---|---|---|---|---|---|---|---|
| A | 9 | 6954.0 ms | 441.0 ms | 6513.0 ms | 9521.0 ms | 233949.0 | 5.0 |
| B | 9 | 25781.0 ms | 264.0 ms | 25303.0 ms | 28636.0 ms | 14425.0 | 1239.0 |
| C | 9 | 3927.0 ms | 280.0 ms | 3285.0 ms | 5752.0 ms | 14405.0 | 5.0 |

FLOOR (condition C): 3927.0 ms, MAD 280.0 ms, n=9
  of which local process startup to turn.started: 321.0 ms

PREFILL: 219544.0 marginal input tokens in 3027.0 ms -> 72529.0 tok/s (point, resolved=true)
DECODE:  1234.0 marginal decoded tokens in 21854.0 ms -> 56.5 tok/s (point)
RATIO:   1284.5x (point)

Confounds this measurement cannot separate:
  - Wall clock includes network transfer of the prompt. Condition A ships ~1 MB, condition C ships ~1 KB, so (A - C) contains upload time as well as prefill. Prefill rate is therefore a LOWER bound even when the delta resolves.
  - Server-side batching and queueing are invisible from the client. A turn may wait behind other tenants; that time lands in the floor and in both deltas.
  - Provider prefix caching is defeated for condition A's filler (fresh random words per replicate) but the codex system prompt and tool definitions are cached identically across all conditions, so the cached prefix cancels in the subtraction.
  - Reasoning tokens are counted as decode because they are produced serially, but they are not visible, so condition B's decode total is trusted from the provider's own usage report rather than from the text.
  - Token counts come from the provider's usage report, not from a local tokenizer. They are authoritative for billing and are assumed authoritative for work.
  - A single provider, model, and datacentre. Nothing here generalises to other hardware, other models, or the same model under different load.
  - codex exec is an agent wrapper, not a raw completion. The floor includes CLI process start, config load, and session setup, which a raw API call would not pay.
