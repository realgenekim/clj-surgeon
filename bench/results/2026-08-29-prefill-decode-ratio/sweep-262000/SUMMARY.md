Model: gpt-5.6-sol | reasoning: low | profile: clean | codex: codex-cli 0.147.0
Host: anvil-server | 16 cores | loadavg at start: 1.24 1.27 1.18

| Condition | n | median wall | MAD | min | max | input tok | decoded tok |
|---|---|---|---|---|---|---|---|
| A | 5 | 4979.0 ms | 220.0 ms | 4398.0 ms | 5199.0 ms | 69262.0 | 5.0 |
| B | 0 | n/a ms | n/a ms | n/a ms | n/a ms | n/a | n/a |
| C | 5 | 3897.0 ms | 72.0 ms | 3705.0 ms | 7780.0 ms | 14405.0 | 5.0 |

FLOOR (condition C): 3897.0 ms, MAD 72.0 ms, n=5
  of which local process startup to turn.started: 304.6 ms

PREFILL: 54857.0 marginal input tokens in 1082.0 ms -> 50700.0 tok/s (point, resolved=true)
DECODE:  n/a marginal decoded tokens in n/a ms -> n/a tok/s (unresolved)
RATIO:   n/ax (unresolved)

Confounds this measurement cannot separate:
  - Wall clock includes network transfer of the prompt. Condition A ships ~1 MB, condition C ships ~1 KB, so (A - C) contains upload time as well as prefill. Prefill rate is therefore a LOWER bound even when the delta resolves.
  - Server-side batching and queueing are invisible from the client. A turn may wait behind other tenants; that time lands in the floor and in both deltas.
  - Provider prefix caching is defeated for condition A's filler (fresh random words per replicate) but the codex system prompt and tool definitions are cached identically across all conditions, so the cached prefix cancels in the subtraction.
  - Reasoning tokens are counted as decode because they are produced serially, but they are not visible, so condition B's decode total is trusted from the provider's own usage report rather than from the text.
  - Token counts come from the provider's usage report, not from a local tokenizer. They are authoritative for billing and are assumed authoritative for work.
  - A single provider, model, and datacentre. Nothing here generalises to other hardware, other models, or the same model under different load.
  - codex exec is an agent wrapper, not a raw completion. The floor includes CLI process start, config load, and session setup, which a raw API call would not pay.
