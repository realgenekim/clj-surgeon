# OpenRouter fast-elaborator shootout — sub-second bangs found

*mayor@skiff, 2026-08-31 ~21:45 PT. Gene supplied a shortlist of Spark-class alternatives
(Kimi K2.6/Cerebras, gpt-oss-120B/Cerebras, Gemini 3.5 Flash) and the OpenRouter account.
Eleven minutes later the fastest complete loop of the program was committing real edits.
Measured live; receipts are the commands below; costs are metered-API pennies, bounded runs
only per house economics.*

## The shootout (one fixed ~450-token generation, measured E2E from this laptop)

| Model @ provider | tok/s (measured) | wall | verdict |
|---|---:|---:|---|
| **openai/gpt-oss-120b @ Cerebras** | **291** | **1.24s** | the champion — 3.5x terra, 5.5x sol |
| google/gemini-3.5-flash @ Google | 199* | 7.78s | *burned 1,546 thinking tokens; wall poor |
| qwen/qwen3-coder-flash @ Alibaba | 85 | 5.78s | terra-class, no gain |
| moonshotai/kimi-k2.7-code @ Alibaba | 54 | 8.89s | slow |
| moonshotai/kimi-k2.6 @ Novita | 29 | 24.0s | no Cerebras route on OpenRouter; dead here |
| (reference) gpt-5.6-terra, ChatGPT sub | ~83 | 9.4s | tonight's codex fallback pin |
| (reference) gpt-5.3-codex-spark | 1,000+ rpt | — | quota-drained until 00:42 |

Kimi-on-Cerebras (the article's pick) is not reachable via OpenRouter provider routing
today — "No allowed providers available." gpt-oss-120b IS Cerebras-served and answered
first try.

## or-bang: the adapter (~60 lines, /tmp/or-bang.sh)

read the owner form via inspect_clojure (held MCP session) -> one closed worksheet to
gpt-oss-120b (form + decision, output ONLY the replacement form) -> apply via the
installed production edit_clojure with full from/to guards, matches=1. The model never
holds authority; the ordinary transaction judges its output exactly like any caller's.

## Live results (4/4 committed, production guards)

| Fill | total | split |
|---|---:|---|
| literal swap (verified byte-exact) | **0.84s** | read .11 / elaborate .58 / apply .09 |
| wall-class: 4th arity + docstring rewrite | **0.92s** | read .15 / elaborate .61 / apply .08 |
| cross-file docstring add | **1.04s** | read .13 / elaborate .68 / apply .15 |

Sub-second decision-to-verified-commit. Warm Spark measured 2.29s on this class; terra
1.68s. The elaborate step — the wall itself — costs ~0.6s at Cerebras speed.

## Honest boundaries

- n=4 screen-grade; the codex-variant battery lane (experiment/elaborator-fallback-battery)
  owns the rigorous version; these results extend its table, addendum-style.
- Metered API: pennies per fill, but the meter rule applies — bounded runs, no unbounded
  loops, alarm discipline same as the elaborator specs (80/90).
- Provider variance is real (same model, different host = 10x speed swing); any product
  pin must name model AND provider and treat provider loss as a degrade event.
- The fallback ladder as of tonight: oss-120b@Cerebras (fast lane, metered pennies) >
  terra (sub, free) > sol (sub, free) > spark (sub, free, returns 00:42). The grammar
  carries the trust; the pin is a fuel selector; receipts name which mind typed.
