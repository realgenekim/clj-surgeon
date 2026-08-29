# Captain's Log: the general read mission stops; the materialization hill opens

Date: 2026-08-29

## Decision

Do not build a general compiled read-mission graph from the retained agent
corpus. The mechanically knowable follow-ups are too small a minority. Move the
active experiment to the dominant pre-first-call interval on the successful
15-owner extraction route.

## Fresh bounded observation

The privacy-safe `study-agent-usage` collector covered the exact window from
`2026-08-29T07:32:24.734205Z` through
`2026-08-29T10:36:32.586284Z` and wrote:

`/tmp/clj-surgeon-agent-usage-20260829T073224734205Z-20260829T103632586284Z.json`

It observed 114 Codex Surgeon calls: 62 `inspect_clojure`, 5 `edit_clojure`, 36
CLI `:cat`, and 11 CLI `:ls`. Across 86 post-Surgeon boundaries, the median was
8.784 seconds, p90 was 25.334 seconds, and cumulative boundary wall was
1,229.822 seconds. Direct MCP execution was not the dominant cost: 67 calls had
a median of 89 ms and consumed 8.827 seconds in total.

Thirty fresh boundaries ended in another Surgeon read. Their relation to the
previous read was:

| Relation | Pairs | Median boundary | Total boundary wall |
|---|---:|---:|---:|
| Disjoint files | 5 | 21.928 s | 106.146 s |
| Exact target | 1 | 6.302 s | 6.302 s |
| Overlapping files | 7 | 7.148 s | 57.801 s |
| Same files | 7 | 7.277 s | 57.160 s |
| CLI identity unavailable | 10 | 10.460 s | 102.505 s |

Fourteen of the first reads refused, eight succeeded, and eight were CLI calls
without typed outcome identity. File overlap alone is not evidence that the
later question or selector was knowable before the first result.

## Retained falsifier

The earlier 24-hour schema-v5 replay already classified the general hypothesis:

| Classification | Pairs | Mechanically groupable | Boundary wall |
|---|---:|---|---:|
| Exact repeat | 2 | yes | 13.382 s |
| Already-requested subset | 1 | yes | 5.864 s |
| Exact source-linked follow-up | 7 | yes | 52.944 s |
| Refusal recovery | 13 | no | 85.189 s |
| Outline-driven selection | 11 | no | 120.133 s |
| Judgment-dependent or unrelated | 17 | no | 492.551 s |

Only 10 of 51 classifiable MCP pairs were mechanically groupable: 19.6 percent
of pairs and 9.4 percent of their boundary wall. The predeclared gate required
at least 26 of 51. A general read graph would return more evidence for the
majority of routes that still require a model decision. That is a clear stop,
not an implementation backlog.

## Next falsifiable hill

The successful frozen extraction route has a much stronger concentration of
removable time:

| Phase | Retained product median |
|---|---:|
| Initial call materialization | 14.040 s |
| Server transaction | 1.895 s |
| Receipt interpretation | 2.870 s |
| Complete wall | 19.216 s |

The next isolated verified-mutation screen asks whether the initial interval is request
construction or mostly fixed model/service latency. It keeps the same frozen
task, model, reasoning, product tool surface, and exact extraction call:

- Control: the current tool-first prompt supplies all decisions and an object
  shape; the model constructs the populated call.
- Literal relay: the same prompt also supplies the fully populated call
  arguments; the model relays them as its first action.

Both arms must produce the same exact first call, one successful atomic
transaction, no discovery or fallback, the same semantic result, and the same
verification evidence. Initial materialization is the primary mechanism clock;
complete verified wall remains the product outcome. A faster but different or
incorrect request is a failure.

If literal relay does not materially reduce the initial interval, request-shape
compression is not the next hill. If it does, the result earns a bounded design
question: which mechanically complete decisions should the caller or server
compile into a ready-to-relay call without adding another turn?

## Scope

This observation used collector receipts, retained phase clocks, and bounded
experiment artifacts. It did not read broad private transcript prose. It made
no product edit, install, reload, shared-port change, or cclsp/LSP request.

<!-- agent-usage-window-end: 2026-08-29T10:36:32.586284Z -->
