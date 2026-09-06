# gpt-oss token and cost tally (2026-09-06, 02:1xZ) — Gene: "I just got $3.50"

Prices, dated 2026-09-06, quoted from the sources: Groq openai/gpt-oss-120b $0.15/M input, **$0.60/M output** (console.groq.com/docs/models; corroborated by OpenRouter's Groq endpoint pricing; the earlier $0.75 figure in my brief was wrong); OpenRouter → Cerebras $0.35/M in, $0.75/M out (openrouter.ai/api/v1/models/openai/gpt-oss-120b/endpoints). Reasoning tokens are billed as output on both and are already inside completion_tokens — never add them twice.

## What the receipts on this box record (three fx trees; computed twice, independently, same answer)

| provider / upstream | calls | prompt tokens | completion (incl. reasoning) | reasoning | no usage fields | est. USD |
|---|---|---|---|---|---|---|
| groq | 176 | 235,432 | 201,222 | 133,248 | 2 | 0.156 |
| openrouter → Cerebras | 164 | 267,033 | 353,813 | 235,091 | 0 | 0.359 |
| openrouter → DigitalOcean / DeepInfra / CoreWeave / SiliconFlow | 12 | 16,034 | 15,274 | 9,714 | 0 | 0.006 |
| openrouter, usage unreported | 13 | ? | ? | ? | 13 | unknown |
| spark (codex, subscription) | 60 | — | — | — | 60 | 0 |
| fake / replay (no model call) | 196 | 0 | 0 | 0 | — | 0 |
| **total** | 621 | | | | | **0.52 + unknown** |

## Versus $3.50
The visible spend is about a seventh of the meter, and every unknown sits on the under-counted side: 13 real OpenRouter calls carry no usage fields; runs under other TYPIST_FX roots or cleaned trees are invisible; OpenRouter adds a ~5% credit-purchase fee above inference; and the same OpenRouter key has carried earlier program spend (the 2026-08-31 elaborator shootout on Cerebras and the 2026-09-02 arm T on Buster) that no receipt on Anvil can see. The meter is the record; the receipts are a floor. Closing the gap needs the OpenRouter activity export (key on the skiff) or --cost-report pointed at every fx tree on every box.

## From now on
Every candidate carries :cost_usd and :cost_source ("provider" when OpenRouter's usage.cost is present — proved live: 806 prompt + 236 completion tokens → 0.0004591 USD, provider-reported and table-computed agree to seven decimals — else "table:2026-09-06"); receipts carry :run_cost_usd; the summary line prints cost=; `bin/typist-run --cost-report [--fx DIR …]` prints this table and writes cost-report-<epoch>.edn; the events ledger line carries prompt_tokens, completion_tokens, reasoning_tokens, cost_usd, provider, upstream (null when the caller has none). One bug caught by the change: the receipt writer rounded floats to three places, so the first priced run recorded 0.001 for a true 0.000521 — fixed before it could mislead.
