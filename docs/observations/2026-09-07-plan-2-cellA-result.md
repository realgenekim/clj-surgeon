# plan-2 Cell A result — the views.clj split, native vs current Surgeon surface (2026-09-07 21:16–21:34Z)

Task: split src/cfp_scheduler_killer/views.clj (4594 lines, 129 forms) into 20 namespaces per the historical mapping (plan SUPPLIED to every arm); rewrite server.clj + 4 test callers; delete the source. Base split/base d9205abc. Four fresh Opus callers, concurrent, 90-min cap. Oracle: suite (bin/kaocha unit --fail-fast, ~22 s) + views.clj gone + every form once in its listed file + the historical view_architecture_test (namespace set, acyclic graph via clj-kondo, layer rules). Stamps by the process (seconds from each arm's orient-start).

| arm | plan | moves | callers | first suite green | oracle (all four) | correct |
|---|---|---|---|---|---|---|
| N2 native | 183 | 183 | 248 | 270 | **331** | yes |
| N1 native | 248 | 282 | 338 | 359 | **393** | yes |
| T1 tool | 231 | 660 | 934 | 978 | **1066** | yes |
| T2 tool | 221 | 750 | 958 | 983 | **1039** | yes |

Medians: native 362 s, tool 1052 s → native 0.34x of the tool wall (tool 2.9x slower). Correctness 4/4. Falsifier for "tool wins" met by a wide margin; Astra's killer threshold not approached.

Where the time went: planning was equal (183–248 s in all four arms — every caller computed the dependency graph itself; the tool arms ALSO wrote analyze/deps scripts because no verb returns edges, promotions or a cycle verdict from a mapping). Native then executed the whole move as one scripted pass (0–34 s) and rewrote callers in one pass (55–65 s). The tool arms paid one guarded extraction per destination namespace (7–9 minutes for 20 destinations) and then 4.5 minutes of caller rewriting. Server time per call was small; the loop was the cost.

Labels: throughput cell (4 concurrent runs), plan supplied (execution of a known plan, not discovery), Opus callers only, n=2 per arm. Receipts under 2026-09-07-plan-2/cellA/<arm>/ (stamps, MCP request/response JSON, the callers' scripts).

Interpretation and design response: design-fable-split-verb.md (one-call split_plan read + split_namespace write, MCP + CLI), Astra's opinion astra-opine.md (pending at write time), program-of-record.md.
