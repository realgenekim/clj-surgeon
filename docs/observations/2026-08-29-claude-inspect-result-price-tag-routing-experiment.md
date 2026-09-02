# Claude chose Surgeon before the price tag arrived

**Date:** 2026-08-29 Pacific

**Claude Code:** 2.1.219

**Models:** `fable` → `claude-fable-5`; `opus` → `claude-opus-5`
**Decision:** The in-band price tag gets no causal credit in this task. Present routed to Surgeon 16/16, but absent also routed 16/16. Preserve the 100% unhinted Claude baseline and do not expand this saturated cohort.

## Answer

The price-tag line produced **zero observed routing lift**. Every one of the 32 metered attempts used the production `edit_clojure` MCP mutation route, and every final file was byte-identical to the oracle with the independent three-assertion test green.

The baseline is the important result:

- Fable absent was preregistered at 25%; observed **8/8, 100%**.
- Opus absent was preregistered at 50%; observed **8/8, 100%**.
- Pooled absent was preregistered at 37.5%; observed **16/16, 100%**.

The hint arm was also **16/16, 100%**. Its absolute adoption therefore clears Gene's reviewer threshold of at least 25% and is nowhere near the below-10% kill threshold. It does not demonstrate that the hint caused the routing: the matched control was already at the ceiling.

## Frozen question and preregistration

The experiment asked whether a successful `inspect_clojure` result would change Claude's actual mutation route if it appended exactly this line:

> batched multi-form edits: edit_clojure, ~7x faster, guarded

The line was never placed in the user prompt, system prompt, server instructions, tool description, schema, or repository doctrine. The task prompt required exactly one batched pre-mutation `inspect_clojure` consultation, then said only to choose any offered mutation route.

The preregistration was frozen at `2026-08-30T04:32:19.949121Z`, eleven minutes before the first measured process start. Its SHA-256 is `e87b3c60fa6f34d437debf0145a3d4b89f78784b1a10621d58233623beef28c8`. It fixed:

- Fable absent at 25%, Opus absent at 50%, and pooled absent at 37.5%;
- distinct `environment_valid`, `semantic_correct`, and `route_adherent` gates;
- actual first mutation tool as the primary route;
- refusals, action turns, correctness, spend, and no-mutation outcomes as secondary measures;
- a four-condition Williams order, repeated once, for n=8 per cell; and
- exactly 32 metered starts with no post-launch replacement.

The order was `A B D C / B C A D / C D B A / D A C B`, repeated, where `A=Fable/absent`, `B=Opus/present`, `C=Fable/present`, and `D=Opus/absent`.

### Two preregistration disclosures

First, the initial fixture commit instantiated eight subtree matches although the preregistration said nine. Before any model call, one second realistic `jitter-ms` reference was added to `connection-policy`, the oracle and test were updated, and a timestamped addendum froze fixture commit `f2eded29c27962d607488bffcb5097a4a33a574d`. The addendum SHA-256 is `46573511ab6e78c1249cd86eb52684757fea0cbfdaf2b7ffa564aae8591082c9`. The task, owners, prediction, intervention, gates, and order did not change.

Second, I incorrectly operationalized Gene's “predicted >=25%; kill below 10%” reviewer thresholds in the preregistration as percentage-point lift. Gene's wording referred to absolute hint-arm addressable adoption. I did not rewrite the immutable preregistration. Under the correct absolute reading, present at 100% clears the prediction. The causal contrast remains zero percentage points because absent was also 100%.

## Method

### Existing-file task

Every run received a fresh clone at one constant `/tmp` path from the same clean fixture commit. The model session began with `src/acme/retry_policy.clj` already tracked and committed. It had to change two private timeout values, rename one private Var and four references across three named callers, and change two profile string literals. That is one nine-match, six-owner batch in an existing file. No session created the target file.

The independent scorer required both:

1. exact equality of the complete target file to the frozen expected bytes, which protects every unrelated byte; and
2. `clojure -M:test` passing one test with three assertions over the policy map, backoff, and summary.

### Offered routes

Every run used the same strict MCP configuration and the same built-in list:

```text
Read, Edit, Write, ToolSearch
```

The fixture-scoped stdio proxy advertised only the production `inspect_clojure` and `edit_clojure` schemas fetched from the live clj-surgeon server, and forwarded both calls to that server. Thus native `Edit` and `Write` and the executable production MCP mutation were genuinely available on every valid run. Native tools were never invoked.

The zero-token preflight proved:

- tool-list SHA-256 `47411d72b8ea6b47b36fa92717e19f4376b0083fa697677ea78183fb7a737c22` in both arms;
- server-instructions SHA-256 `23c60f214c2bf08ae8159fdbda5c301ac1d208d98f2f3b80e257f6bfa197f537` in both arms;
- the absent emitted result equaled its own upstream production result exactly;
- the present result, after removing the exact final hint line, equaled its own upstream result exactly;
- stable source, owner, hash, count, and structured evidence matched across arms after excluding naturally varying server elapsed fields; and
- a forwarded production `edit_clojure` call produced exact oracle bytes and 3/3 passing assertions.

Across measured calls, all 16 successful present inspections visibly ended in the hint, while 0/16 absent inspections contained it. No model dry run occurred. Two harness-only, zero-model preflight refusals are retained: the first used a stale direct `owners` spelling; the second checker compared naturally different elapsed fields rather than each emitted result with its own upstream result. Both were repaired before the metered cohort.

### Caller identity and window

The resolved Claude executable was `/opt/homebrew/lib/node_modules/@anthropic-ai/claude-code/bin/claude.exe`, SHA-256 `a8e806faaefac53c7a0f26523d8a45c60dbef3407b14ef990c75765d08febc82`. The CLI was invoked with `--model fable` or `--model opus`, `--effort high`, no fallback model, stream JSON output, strict MCP config, and no session persistence.

- UTC: `2026-08-30T04:43:14.856393Z` through `2026-08-30T04:57:11.348742Z`.
- Pacific: `2026-08-29T21:43:14.856393-07:00` through `2026-08-29T21:57:11.348742-07:00`.
- Metered Claude starts: exactly 32.
- Replacements or extra cohort runs: zero.

Each primary identity resolved exactly as requested. Claude also reported a small `claude-haiku-4-5-20251001` catalog-helper charge on all 32 runs; it was retained in usage and cost rather than hidden as part of the primary model.

## Results

### All attempts, with losses retained

| Cell | Surgeon first | Environment valid | Exact semantics | Route adherent | Refusals | Median turns | Median tool uses | CLI cost |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Fable absent | **8/8 (100%)** | 8/8 | 8/8 | 8/8 | 0 | 4 | 3 | $2.242316 |
| Fable present | **8/8 (100%)** | 8/8 | 8/8 | 8/8 | 0 | 4 | 3 | $2.033527 |
| Opus absent | **8/8 (100%)** | 8/8 | 8/8 | 8/8 | 0 | 4 | 3 | $1.095796 |
| Opus present | **8/8 (100%)** | 8/8 | 8/8 | **7/8** | **1** | 4 | 3 | $1.527974 |
| All | **32/32 (100%)** | 32/32 | 32/32 | **31/32** | **1** | 4 | 3 | **$6.899613** |

There were no native-first and no no-mutation attempts. `inspect_clojure` reads are not counted as the mutation route; every primary mutation was the subsequent `mcp__clj-surgeon__edit_clojure` tool use.

Run 2 is the retained loss. Opus first emitted the obsolete direct `owners` inspect shape and received one production refusal. It then obtained the current batched `requests/forms` schema, performed the required successful inspection, received the hint, and completed a correct Surgeon edit. Its primary route and semantics are positive, but `route_adherent=false` because it used two inspections, one refusal, six tool uses, and seven turns.

### Baseline and causal contrast

| Measure | Preregistered | Observed | 95% Wilson interval |
|---|---:|---:|---:|
| Fable absent | 2/8 (25%) | **8/8 (100%)** | 67.6%–100% |
| Opus absent | 4/8 (50%) | **8/8 (100%)** | 67.6%–100% |
| Pooled absent | 6/16 (37.5%) | **16/16 (100%)** | 80.6%–100% |
| Pooled present | absolute prediction ≥25% | **16/16 (100%)** | 80.6%–100% |

The observed present-minus-absent difference is **0 percentage points**. A score-based interval obtained from the two pooled Wilson bounds is approximately **−19.4 to +19.4 points**. With n=8 per cell, one observation is 12.5 points and each perfect cell still has a lower bound of only 67.6%. The null contrast is not equivalence; it says this cohort cannot distinguish a useful hint from no effect once the control is saturated.

The most plausible bounded explanation is visible in the design, not hidden model intent. The prompt forced an exact pre-edit structural inspection on an explicitly addressable multi-form batch; the production tool description itself says `edit_clojure` commits one atomic guarded transaction. That combination already reset Claude's native-edit prior in every control run. No causal claim should be assigned to the extra line.

## Tool-inventory confound

Native `Edit` and `Write` were visible in the client init event for 32/32 runs. The client obtained the exact same MCP tool-list hash in 32/32 runs. Initial presentation nevertheless differed:

- Fable init listed both direct MCP tool names in 16/16 runs.
- Opus init listed both direct MCP tool names in 15/16 runs.
- Opus run 2 initialized the MCP server as `pending` and listed only `Edit`, `Read`, `ToolSearch`, and `Write`; its subsequent `ToolSearch` obtained the exact production MCP inventory and both MCP calls succeeded.

Run 2 is environment-valid under the preregistered ultimate-availability gate because both native routes were visible, Claude requested the MCP inventory, the exact schema hash matched, and the transport executed the tools. The `pending` init state and missing direct names remain an explicit model/tool-set confound. Do not use this cohort to claim a Fable-versus-Opus routing difference.

## Direct tool wall, complete wall, and turns

| Cell | Median successful inspect tool | Median edit tool | Median complete Claude process |
|---|---:|---:|---:|
| Fable absent | 0.0110s | 0.0494s | 21.900s |
| Fable present | 0.0105s | 0.0531s | 21.996s |
| Opus absent | 0.0141s | 0.0457s | 23.682s |
| Opus present | 0.0122s | 0.0459s | 25.692s |

Thirty-one attempts followed `ToolSearch → inspect_clojure → edit_clojure` in four Claude turns and three tool uses. Run 2 added the refused inspection and recovery search. The complete process wall summed to 791.930 seconds; direct production read and edit execution remained tens of milliseconds. Timing is secondary here and does not rescue a causal hint effect.

## Usage and spend

These are Claude Code's own `modelUsage` token counts and `costUSD`, not a rate-card estimate. They sum exactly to the CLI's `total_cost_usd` of **$6.899613**. This is the client's metered API-equivalent cost and was not independently reconciled to an invoice, so no external pricing lookup was needed.

| Reported model | Runs | Noncached input | Cache creation input | Cache-read input | Output | Cost |
|---|---:|---:|---:|---:|---:|---:|
| `claude-fable-5` | 16 | 128 | 120,012 | 522,188 | 26,750 | $4.261208 |
| `claude-opus-5` | 16 | 130 | 141,559 | 521,380 | 37,286 | $2.609080 |
| `claude-haiku-4-5-20251001` helper | 32 | 26,720 | 0 | 0 | 521 | $0.029325 |

All runs reported standard service tier and zero web-search requests.

## Post-cohort scorer correction

The immutable raw events were never changed. The first aggregate incorrectly labeled run 2 `hint_observed=false` because the scorer inspected only its first, failed inspect result. It also labeled the environment invalid from init `pending` even though the client later obtained the exact inventory and executed both MCP tools.

After all 32 measured processes had ended, the scorer was corrected to:

1. recognize the hint on any successful inspect result; and
2. recognize ultimate MCP availability from the client's `tools/list`, exact production surface hash, and working transport while retaining init status separately.

The original per-run scores and aggregate are preserved beside `rescoring.json`. The correction changes run 2's `hint_observed` and `environment_valid` labels; it does not change its primary route, semantic correctness, refusal, turns, non-adherence, or any model event.

## Raw run appendix

| Run | Cell | Hint | Route | Exact | Adherent | Inspect | Refusals | Turns | Cost |
|---:|---|---|---|---|---|---:|---:|---:|---:|
| 1 | fable/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.387691 |
| 2 | opus/present | yes | Surgeon | yes | **no** | **2** | **1** | **7** | $0.296639 |
| 3 | opus/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.194367 |
| 4 | fable/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.347725 |
| 5 | opus/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.194467 |
| 6 | fable/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.337898 |
| 7 | fable/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.201220 |
| 8 | opus/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.116707 |
| 9 | fable/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.332064 |
| 10 | opus/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.123142 |
| 11 | opus/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.183085 |
| 12 | fable/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.199971 |
| 13 | opus/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.113202 |
| 14 | fable/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.333556 |
| 15 | fable/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.219821 |
| 16 | opus/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.228236 |
| 17 | fable/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.209311 |
| 18 | opus/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.208378 |
| 19 | opus/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.206559 |
| 20 | fable/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.194094 |
| 21 | opus/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.119525 |
| 22 | fable/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.195123 |
| 23 | fable/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.199080 |
| 24 | opus/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.114896 |
| 25 | fable/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.208871 |
| 26 | opus/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.110905 |
| 27 | opus/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.116228 |
| 28 | fable/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.353311 |
| 29 | opus/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.116018 |
| 30 | fable/absent | no | Surgeon | yes | yes | 1 | 0 | 4 | $0.358176 |
| 31 | fable/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.197931 |
| 32 | opus/present | yes | Surgeon | yes | yes | 1 | 0 | 4 | $0.181416 |

## Evidence and claim boundary

The bounded harness and receipts are outside `bench/` at:

```text
/tmp/clj-surgeon-claude-route-hint.PfLCzA/experiment
```

Key authorities are `preregistration.md`, `preflight/preflight.json`, `all-attempts.jsonl`, every `runs/NNN/{launch,process,server,stream,test,score}.json*`, `rescoring.json`, `run-table.tsv`, `aggregate.json`, and `receipt-manifest.txt`.

A compressed copy is retained at:

```text
/tmp/clj-surgeon-claude-route-hint.PfLCzA/claude-route-hint-receipts-20260829.tar.gz
SHA-256 b89152eaec3fc29ff4332eb8fee0e020a6373d895a61abb165c81b7bed068843
```

This proves compatibility and a 100% unhinted routing baseline for one explicit, forced-inspection, addressable batch under Claude Code 2.1.219. It does not prove the hint has no effect on harder or less explicit tasks, does not establish equivalence, and does not transfer the route rate to production traffic. A future causal test would need an unsaturated control. This cohort itself must not be widened or rerun into a more favorable contrast.
