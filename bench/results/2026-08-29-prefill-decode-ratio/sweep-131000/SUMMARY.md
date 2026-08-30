Model: gpt-5.6-sol | reasoning: low | profile: clean | codex: codex-cli 0.147.0
Host: anvil-server | 16 cores | loadavg at start: 1.13 1.25 1.17

| Condition | n | median wall | MAD | min | max | input tok | decoded tok |
|---|---|---|---|---|---|---|---|
| A | 5 | 4725.0 ms | 342.0 ms | 3693.0 ms | 6054.0 ms | 41861.0 | 5.0 |
| B | 0 | n/a ms | n/a ms | n/a ms | n/a ms | n/a | n/a |
| C | 5 | 4049.0 ms | 66.0 ms | 3587.0 ms | 4585.0 ms | 14403.0 | 5.0 |

FLOOR (condition C): 4049.0 ms, MAD 66.0 ms, n=5
  of which local process startup to turn.started: 277.1 ms

PREFILL: 27458.0 marginal input tokens in 676.0 ms -> 67299.0 tok/s (lower-bound, resolved=false)
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
