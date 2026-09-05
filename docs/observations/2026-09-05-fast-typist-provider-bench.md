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
