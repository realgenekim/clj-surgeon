# Consumption gap: the contradiction dissolves, the causal split stays open

Date: 2026-08-30 PT  
Branch: `experiment/consumption-gap-20260830`  
Preregistration: `2ef7f7e`  
Status: **Phase 1 instrumentation stop; Phase 2 forbidden**

## Result first

The production number does not contradict sweep-1. The two measurements used
the word “reread” for different decision requirements.

Sweep-1's complete arm made an owner **name** the complete answer: names were
unique, the target token appeared verbatim, the helper accepted that token
alone, source bodies were irrelevant, and the only permitted mutation route
was the helper. Zero of ten callers reread under that names-sufficient contract.

Production's 119/137 metric counted any successful same-file inspect within
seven service calls and ten minutes, before a mutation attempt. A complete
owner vector proved vocabulary completeness; it did not prove that the caller
already had the intended source body, location, ordinal, dispatch identity, or
the exact selector syntax for its next action. The retained audit explicitly
warned that these were targeted reads, not “full file rereads.” Across the
current and legacy strata together, the matched same-file subrequests were 290
`forms` (89.8%), 26 `outline` (8.0%), and 7 `match` (2.2%); every `forms`
request carried selectors. That aggregate is consistent with callers asking
for source or position after learning the vocabulary.

The doctrine-grade resolution is therefore:

> Complete owner names transfer only to tasks whose next safe action requires
> owner names and nothing else. “Complete vocabulary” is not “complete next-
> call state.”

This dissolves the apparent 0/10-versus-119/137 contradiction. It does **not**
license a causal claim that H-LOC or H-HABIT explains the 119.

## Phase 1 coverage result

The preregistered classifier cannot be applied to the retained public artifacts.
Coverage is 0 of 119, so its verdict is
`instrumentation-repair-required`.

Three exact fields are absent:

1. The privacy-safe receipts omit request values and result content. They prove
   that a reread occurred but do not show which semantic facts its first result
   returned. The aggregate 290/26/7 operation counts combine the current and
   legacy strata and cannot be reassigned to the current 119.
2. The full MCP service telemetry has no client, provider, or model identifier.
   The agent-usage receipt does not retain an event-level join from these 119
   service calls to a caller model. A by-model split would be invented.
3. The owner-distribution receipt proves all 137 vectors were complete, zero
   owners were omitted, and none was truncated. It omits the required selector,
   unique-answer relation, and refusal answer token, so verbatim consumability
   is not measurable.

The six raw telemetry inputs are fenced by digest in the earlier audit, but no
matching copy exists under `/private/tmp`. The bounded locator examined the 97
JSONL files under `/private/tmp` that contained the typed selection-refusal
marker and matched zero of the six expected digests. Per instruction, no `$HOME`
scan or `~/src.local/clj-surgeon` access was attempted.

## Registered hypothesis verdicts

- **H-LOC:** alive but unmeasured. The current product caps ambiguous location
  rows at ten, while the complete owner-name vector is separate. The retained
  aggregate has 26 outline subrequests across both strata, but cannot say how
  many belong to the current 119 or whether they fetched rows omitted by the
  cap.
- **H-HABIT:** alive but unmeasured. A name-only reread despite a unique,
  verbatim-consumable answer would support it, but neither result semantics nor
  consumability survives in the retained projection.
- **Other information:** strongly plausible, not attributable to the current
  119. The all-strata aggregate is dominated by selector-bearing `forms`
  requests, which return intended source rather than merely repeating the owner
  vocabulary.

This is a typed non-adjudication, not a tie and not an invitation to choose the
more appealing story.

## Why Phase 2 did not run

The frozen gate requires at least 90% classifiable Phase 1 coverage before a
live screen and opens the duplicate-location A/B only when at least eight
high-confidence episodes support each registered hypothesis. Coverage is 0%,
so the live screen is forbidden. Running it would answer a synthetic duplicate-
location question while leaving the production population unjoined—the exact
fixture-transfer mistake this study was meant to prevent.

No model call, API-key route, or subscription turn was used.

## Exact repair needed to finish the causal split

Copy the six digest-fenced production telemetry files into an isolated
`/private/tmp` input directory, without paths or credentials, or produce an
equivalent event-level privacy-safe ledger. The adapter must preserve only:

- refusal and first-recovery call identity;
- equality-preserving anonymous owner and locator tokens;
- refusal location rows and cap flag;
- first-read semantic kinds and duplicate multiplicity;
- required selector, answer token, and uniqueness;
- caller model, or an explicit `unknown` if the source truly lacks it.

Run the already-pushed `classify.py` unchanged. If its coverage clears 90%, its
frozen verdict alone decides whether the live duplicate-location screen opens.
Do not tune the taxonomy or threshold after seeing the recovered rows.

## Receipts

- Phase 1 coverage:
  `docs/observations/evidence/consumption-gap-20260830/phase1-coverage.json`
- Bounded `/private/tmp` input locator:
  `docs/observations/evidence/consumption-gap-20260830/input-locator.json`
- Frozen classifier:
  `bench/consumption_gap/classify.py`
- Frozen protocol and transfer conditions:
  `docs/observations/2026-08-30-consumption-gap-preregistration.md`
