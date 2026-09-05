# Fast typist — provider benchmark (2026-09-05, ordered by Gene 22:2xZ)

Gene, verbatim: "let's create code where fast typer is ideally codex spark, and then either gpt oss on groq or open router. Let's benchmark which faster." and "what matters is wall clock time; codex cli may have startup costs, which is a fact of life."

Runner: clj-surgeon bridge/mission-ledger `bin/typist-run --bench --providers groq,openrouter,spark --dossier <d> --rounds 6` — k=1, rounds interleaved round-robin across providers, same apply/gate/acceptance, wall = process spawn (spark) or request send (APIs) to first verified result, startup charged. Predictions were logged before any run (captain's log 22:2xZ): API providers win on wall for bounded dossiers; Spark ~5–10 s (process start per call); falsifier = Spark median under 3 s on onesite.

## Rows so far (Groq only; the other two blocked at run time)

| dossier | provider | model | rounds verified | first-verified wall, sorted (s) | median | max | median response wall | median tok/s (provider-reported) | refusals |
|---|---|---|---|---|---|---|---|---|---|
| onesite | groq | openai/gpt-oss-120b | 6/6 | 0.48 0.62 0.69 0.75 0.95 1.39 | 0.72 | 1.39 | 0.68 | 481 | 0 |
| fanout | groq | openai/gpt-oss-120b | 1/6 | 5.98 | 5.98 | 5.98 | 4.77 | 477 | 0 |
| onesite | openrouter | openai/gpt-oss-120b | — | no key on this seat at run time (refused pre-network, exit 4) | | | | | |
| onesite | spark | gpt-5.3-codex-spark | — | usage limit ("try again at 11:37 PM" = 23:37Z); recorded as refusal, call wall 2.8 s to the refusal | | | | | |

Groq throughput is flat across dossiers (481 vs 477 tok/s); the fan-out dossier is slower only because it emits ~7x more diff. Single-candidate hit rate on fan-out was 1/6 here and 2/6 in the earlier k=1 rounds (pooled 3/12); onesite is at the ceiling at k=1.

Codex CLI startup floor on this seat, measured separately: ~3.5 s for a trivial low-effort Sol call, and the lean flags (`--ignore-user-config --ephemeral -c project_doc_max_bytes=0`) change nothing because this seat's Codex config declares no MCP servers. Spark's row will carry that floor; that is the fact of life Gene named.

Pending: OpenRouter row (key asked of the mayor), Spark row (after 23:37Z), then the three-provider fan-out table.

## OpenRouter rows (22:44–22:52Z) — routing is the whole story

The OpenRouter key arrived (shape `{:openrouter-api-key …}`; runner accepts it). First bench with OpenRouter's DEFAULT routing, then pinned to fast upstreams (`provider.order = [Cerebras, Groq]`, no fallbacks). Groq rerun alongside in every bench (interleaved), so its rows are fresh controls.

| dossier | provider (upstream) | rounds verified | first-verified wall, sorted (s) | median | max | median tok/s |
|---|---|---|---|---|---|---|
| onesite | groq | 5/6 | 0.62 0.75 0.85 0.86 0.95 | 0.85 | 0.95 | 478 |
| onesite | openrouter, default routing (CoreWeave ×2, DeepInfra ×3, SiliconFlow ×1) | 5/6 | 2.24 2.71 3.24 4.06 9.59 | 3.24 | 9.59 | n/a |
| fanout | groq | 4/6 | 3.69 4.44 5.79 6.54 | 5.12 | 6.54 | 452 |
| fanout | openrouter, default routing (DigitalOcean ×5, DeepInfra ×1) | 2/6 | 37.28 52.03 | 44.66 | 52.03 | n/a |
| onesite | groq (control rerun) | 6/6 | 0.50 0.68 0.69 0.76 0.95 1.27 | 0.72 | 1.27 | 479 |
| onesite | **openrouter → Cerebras** (6/6 routed there) | 6/6 | 0.42 0.43 0.48 0.55 0.57 0.72 | **0.52** | 0.72 | n/a (OpenRouter reports no timing) |
| fanout | groq (control rerun) | 3/6 | 5.38 6.49 7.67 | 6.49 | 7.67 | 447 |
| fanout | **openrouter → Cerebras** (6/6 routed there) | 3/6 | 1.49 1.63 1.69 | **1.63** | 1.69 | n/a |

Reading: the model is identical; the upstream decides the wall. OpenRouter's default routing lost by 4x (onesite) and 9x (fanout) because it chose slow hosts; pinned to Cerebras it beats Groq by 1.4x on the small output and 4x on the large one (the fan-out diff is ~2.5k completion tokens; 1.6 s wall implies well over 1.5k tok/s end to end, versus Groq's ~450). Hit rates are the same across providers (same model, same temperature): fanout single-candidate pooled tonight 13/36 on Groq, 5/12 on OpenRouter. So the fastest typist tonight is gpt-oss-120b on Cerebras via OpenRouter, pinned; unpinned OpenRouter is the slowest thing measured.

Standing order for the harness: OpenRouter calls always carry `provider.order` with no fallback; a routed-elsewhere response is a protocol violation, not a datum. Spark row still pending (23:37Z).

## Cerebras k=5 on the fan-out dossier (22:49–22:51Z) — the headline row tonight

Raw log: 2026-09-05-fast-typist-fanout-cerebras-k5.log; all 30 candidates routed to Cerebras (receipts).

| arm | rounds verified | first-verified wall, sorted (s) | median | max | candidates verified |
|---|---|---|---|---|---|
| F k=5, gpt-oss-120b on Cerebras (via OpenRouter, pinned) | **6/6** | 1.86 1.86 1.88 2.02 2.58 2.81 | **1.95** | 2.81 | 13/30 (43%) |
| F k=5, gpt-oss-120b on Groq (earlier tonight) | 6/6 | 4.98 5.29 5.44 5.63 5.66 6.50 | 5.54 | 6.50 | 15/30 (50%) |
| N, cold Sol (gpt-5.6-sol), one process per round | 5/6 | 20.96 22.66 26.66 27.77 42.63 | 26.66 | 42.63 | 5/6 |

Cold vs cold, startup charged both sides, same dossier bytes, same gate, same acceptance: five fast candidates on Cerebras reach a verified three-file fan-out change in a median 1.95 s, every round; one cold Sol author in 26.66 s, five rounds of six. 13.7x on wall. The caveats from the cohort doc all still apply (five-file fixture, bb gate, no warm-Sol comparison, no real-repo claim).
