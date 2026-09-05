# Completed hourly usage observation: 22:05–00:05 UTC

<!-- agent-usage-window-end: 2026-09-05T00:05:00Z -->

This completes analysis of two existing, status-ok v6 receipts. Hour 1 is **2026-09-04 22:05:00–23:05:00 UTC**, or **September 4, 15:05:00–16:05:00 PDT (UTC−07:00)**. Hour 2 is **2026-09-04 23:05:00–2026-09-05 00:05:00 UTC**, or **September 4, 16:05:00–17:05:00 PDT**. The marker advances only this completed observation window. It does not certify a source, performance, or shipping gate.

The receipts were generated at 23:56:31.873426 UTC and 00:36:52.111957 UTC respectively; their observation boundaries remain the explicit hours above. This report reuses them without recollection, transcript inspection, source inspection, collector changes or test execution, as requested while the root full gate runs. The study skill’s ordinary recollection/self-test steps were intentionally not rerun. Subsequent root verification ran the study-agent-usage self-test successfully in verification-active8. That full make test invocation later failed in the unrelated Claude harness timeout self-test; the localized repair and remaining tail checks passed separately. No single green full invocation is claimed.

## Provider aggregate: observed client records

These are provider-wide records found by the collector in each window, not this program’s server traffic, not unique people, and not a matched task cohort. Sessions can recur between hours; do not sum them into a unique-session total. Native actions are classifications, not an assertion that every action modified existing production Clojure.

| Hour/provider | Sessions / Clojure-relevant | Skill visible / loaded relevant evidence | Recognized Surgeon calls / outer actions | Native Clojure actions |
|---|---:|---:|---:|---|
| 1 Codex | 36 / 34 | 7 visible; 0 loads observed | 54 / 26 | 112 shell; 16 apply_patch |
| 1 Claude | 7 / 6 | 1 visible; 0 loads observed | none recognized | 106 shell; 1 write |
| 2 Codex | 24 / 21 | 5 visible; 1 loaded session | 3 / 3 | 119 shell; 10 apply_patch |
| 2 Claude | 4 / 4 | 1 visible; 0 loads observed | none recognized | 111 shell; 2 writes |

Visibility, loading, invocation and successful behavior are distinct stages. No recognized Claude Surgeon calls means none recorded by these classification paths; it does not establish non-use. Hour-one Codex operations are 34 `:cat`, seven `:ls`, and 13 `feature_thread`; hour two recognizes three `:cat`. Recorded refusal-bearing outer actions are 11 and two respectively, with one execution-error action in hour one. Refusal categories can overlap or lack a typed category, so their histograms are not a disjoint outcome partition. CLI-versus-MCP choice cannot establish an avoidable fallback without the caller’s catalog and task context.

Client route phases are predominantly native-read, git and verification. Hour-one Codex has 47 standalone native-read phases, 14 native-patch phases and eight standalone surgeon-read phases; hour two has 36 standalone native-read phases and 17 combined native-read/verify phases. Claude has 20 then 18 standalone native-read phases and eight then 15 combined native-read/verify phases. These show discovery and verification work, but aggregate phase frequencies do not reconstruct one causal task sequence. The retained repair ledger provides the narrower sequence: bounded structural read → attempted edit → typed unchanged refusal → complete-owner edit → behavioral replay, with native construction for newly authored forms.

Recognized Codex Surgeon-action wall sums are 23.274 s (26 actions, median 674 ms) and 11.578 s (three actions, median 305 ms). These are partial client action populations, not total MCP execution or complete task wall. Multiple operations can share an action. Claude direct-time fields and missing post-Surgeon boundaries do not justify zero-duration or zero-decision claims. Complete-turn clocks across concurrent sessions are not additive elapsed program time; unattributed time is not automatically model thinking. No API-round-trip count or causal speedup follows from these aggregates.

## Own-server telemetry is a different population

Both receipts explicitly scan the program’s own server directory, not the default global server roots. Hour one finds one start and no recorded tool events. Hour two finds seven starts and **40 recorded tool events**: 19 `inspect_clojure` and 21 events labeled `apply_clojure_changes`; 38 outcomes are okay and two refused (`invalid-intent-form`). Their recorded server-wall sum is 3.976 s, median 9 ms. Inspect batches contain 33 requests over 20 file reads, returning 59,023 source characters; this report exports no source content. Neither hour records cclsp/LSP service events; use outside the observed logs remains unknown.

Two independent visibility failures must stay separate:

1. **Client aggregate gateway invisibility.** The program sent retained HTTP `tools/call` requests through Python gateways wrapped in ordinary shell/exec actions. Those calls are not identified as public Surgeon operations in the client aggregate. The three recognized hour-two client `:cat` calls cannot be substituted for the own program’s true call count. An opaque shell route can be visible as shell activity while its nested MCP semantics remain unclassified.
2. **Service emitter coverage and naming.** The existing diagnosis establishes that alias migration bypasses the service event emitter. Public `edit_clojure` reaches a shared mutation emitter labeled `apply_clojure_changes` before its public response is relabeled. Changing a client classifier cannot create these missing service events or correct their historical public identity without separate receipts. A full-telemetry hour-one alias success at 22:21:03.094631 UTC changed 21 files/63 sites in about 1.309 s, yet has no tool event. Thus no recorded events is demonstrably not no calls. Early routing/validation and adapter failures also have coverage gaps.

The 40 hour-two service events numerically match the repair ledger’s 19 inspections and 21 public edits; the 17 alias calls in that ledger are absent from the public-tool histogram. This reconciles that retained subset, not all own-server traffic: separate handdrives, reviewer calls and model-comparison arms also lie outside the ledger. Do not add provider calls, service events and raw ledger calls as independent observations.

## Retained repair ledger: 57 direct calls

| Scope | Public tools | Direct tools/call wall |
|---|---|---:|
| Implementation: 14 | 6 inspect; 8 edit (2 unchanged refusals) | 3.901969 s |
| Fresh-server quality replay: 43 | 17 alias; 13 inspect; 13 edit retirements | 1.013282 s |
| Retained total: 57 | 19 inspect; 21 edit; 17 alias | 4.915251 s |

Implementation initialization adds a separately recorded 0.131335 s; replay initialization was not separately retained. This is a bounded author-owned ledger, excluding independent reviews, initial bug discovery, other applications, full-gate preparation and comparison arms. Its first request is 23:10:40.806252 UTC (16:10:40.806252 PDT); final implementation completion is approximately 23:33:37.014548 UTC (16:33:37.014548 PDT), a 1,376.208296 s receipt envelope. The wire aggregate was written at 23:41:07.869803 UTC (16:41:07.869803 PDT), yielding a 1,827.063551 s first-request-to-aggregate envelope. Neither envelope measures the complete repair task. Planning preceded it; review, construction, formatting and resource waiting are not individually attributed.

The first refusal rejected insertion of several new top-level forms through a single-form replacement surface. Native construction subsequently inserted newly authored tests/helpers; its exact call count and duration are not retained. The other refusal rejected an incomplete subtree fragment; complete-owner replacements succeeded through the editor, with no native production-body fallback. Existing production-owner semantic replacements remained structural edits; whole-file formatting is a separate write route. Mechanical edit success alone did not prove behavior.

On the pinned repaired candidate, the quality replay records 13 successful migrations that preserved behavior after old selected-definition retirement and four typed refusals that preserved bytes and behavior. Three selected-rename refusals offered no executable next call. This is self-hosting and bounded mechanism/quality evidence, not a controlled efficiency result or source-shipping approval.

## Falsifiable next improvement and limits

The smallest measurement improvement is exactly one privacy-preserving event per public tool invocation at the common dispatch boundary, with the actual public name and terminal outcome, including early and adapter failures; nested emitters must not double-count it. A later fixed-count replay should demonstrate that alias successes/refusals and public edits each produce one correctly named event. Client gateway classification requires a separate provenance path. Neither fix should rewrite frozen primary meters or infer missing historical calls.

The practical product boundary remains complete verified task completion: compact structural operations worked, but new-form insertion and fragmented replacement caused recoveries. Better contracts might remove a caller decision or recovery; milliseconds of direct tool execution alone do not measure that prize. No matched native counterfactual is provided by this hourly census. The separate primary audit supplies controlled task measurements; this observation does not import its later arms into these time windows.

## Evidence and privacy

Exact retained inputs: `/var/tmp/forge/astra-program/usage-hour-1.json`, `usage-hour-2.json`, `repair-dogfood-ledger.md`, `repair-dogfood-summary.json`, `repair-dogfood-implementation-data.json`, `repair-dogfood-wire-data.json`, and `telemetry-coverage-diagnosis.md`. The ledger and diagnosis retain exact request/response and event-file pointers for local verification. This completed report contains aggregate counts and typed outcomes only: no raw prompts, commands, source bodies, session identities or private reasoning. Existing receipts remain unchanged; coverage limitations are annotations, not fabricated corrections.
