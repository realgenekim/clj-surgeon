---
parent: relation-census-design
prefix: MCP-OP-CENSUS
status: implemented (bridge/census-verb)
---

# Relation Census Specifications

IDs are stable and must not be reused if a requirement is deleted.

## Scope

- [x] **MCP-OP-CENSUS-001**: When clj-surgeon censuses a Clojure source, it shall treat as one site every call whose head is `conj`, `cons`, `into`, or `concat`, every `(fnil conj …)`, `(fnil cons …)`, or `(fnil into …)` occupying the update-fn position of `update`, `update-in`, or `swap!`, every bare `conj`, `cons`, or `into` symbol in that same update-fn position, and every call whose head is a known identity door, and it shall classify only those sites that occur inside the body of a `defmethod fold-event` arm.
- [x] **MCP-OP-CENSUS-002**: When a site occurs outside every `defmethod fold-event` arm, clj-surgeon shall count it in `outside_arms` and shall not classify it, list it, or include it in the per-class counts.

## Classification

- [x] **MCP-OP-CENSUS-003**: When the head of a site call, or the update fn of the enclosing update form, is a known identity door, clj-surgeon shall classify that site `:door` and shall name the door, before any guard analysis.
- [x] **MCP-OP-CENSUS-004**: When a site that is not a door writes into a set literal, including `(fnil conj #{})`, clj-surgeon shall classify that site `:set` before any guard analysis.
- [x] **MCP-OP-CENSUS-005**: When a recognised guard form dominates a site by containing it in one of that form's branches, and the guard's membership idiom reads the same resolved target collection as the write, and the guard performs a keyword lookup whose keyword also occurs in the written value's expression resolved through single-assignment `let` aliases, and the guard's effective polarity for an append is absence, clj-surgeon shall classify that site `:guarded` and shall report the guard's source line, the guard's location, the matched identity expression, and the polarity.
- [x] **MCP-OP-CENSUS-006**: If a dominating guard matches the target and the written value's identity but its effective polarity is presence, clj-surgeon shall classify that site `:unknown` with reason `:polarity`, and shall classify it neither `:guarded` nor `:raw`.
- [x] **MCP-OP-CENSUS-007**: If a dominating test routes the written value's identity or the target collection through a call this version does not recognise, clj-surgeon shall classify that site `:unknown` with reason `:helper-mediated-guard` and shall name the call; if an enclosing form between the arm body and the write is not recognised, it shall classify the site `:unknown` with reason `:unsupported-container` and shall name the form; if the target expression cannot be resolved, it shall classify the site `:unknown` with reason `:unresolved-target`.
- [x] **MCP-OP-CENSUS-008**: When clj-surgeon has walked a site's complete enclosing chain within its recognised vocabulary and no dominating recognised guard satisfies MCP-OP-CENSUS-005, MCP-OP-CENSUS-006, or MCP-OP-CENSUS-007, clj-surgeon shall classify that site `:raw`; clj-surgeon shall never classify a site `:raw` because analysis was inconclusive.
- [x] **MCP-OP-CENSUS-009**: When clj-surgeon reports a site, it shall carry the write's file, line, arm event type, and one-line source, and, where resolved, the target path expression, the written value expression, the candidate identity expression, the guard source and guard line, the polarity, and the uncertainty reason; clj-surgeon shall state in its tool description and its documentation that the census locates review work, does not prove idempotency, and is not an enforcement gate.

## Plan phase

- [x] **MCP-OP-CENSUS-010**: When clj-surgeon executes the plan phase over more than one file, it shall parse and classify those files on a `com.climate.claypoole` thread pool created and shut down inside one `cp/with-shutdown!` scope, using the eager `cp/upmap`, and shall re-key and order merged results by project-relative path.
- [x] **MCP-OP-CENSUS-011**: When the same census is executed with pool size 1 and with pool size N, the complete ordered site list and the complete published receipt excluding `elapsed_ms` and `phases_elapsed_ms` shall be identical.
- [x] **MCP-OP-CENSUS-012**: If a plan-phase worker throws, clj-surgeon shall refuse the complete census with a typed reason naming the file that failed and shall publish no partial counts.

## Receipt and refusals

- [x] **MCP-OP-CENSUS-013**: When a census succeeds, clj-surgeon shall publish `census_version`, per-file counts by class, the `:raw` sites, the `:guarded` sites with their guard lines, the `:unknown` sites with their reasons, `outside_arms`, `files`, `arms`, `sites`, numeric `phases_elapsed_ms` for each phase that ran (see MCP-OP-CENSUS-023), the pool size, and a `next_action`; the published receipt shall be at most 4096 bytes, shall contain no file text beyond one-line site sources, and shall report when listed evidence was trimmed to fit.
- [x] **MCP-OP-CENSUS-014**: If the workspace root does not resolve to an existing absolute directory, if no scanned file defines `defmethod fold-event` arms, if a scanned file cannot be parsed, or if a supplied door is not a symbol, shadows a collection write head, or is defined in no scanned file, clj-surgeon shall refuse with the corresponding typed reason, shall name the offending workspace, files, or door, shall publish no counts, and shall carry an executable `next_call`.
- [x] **MCP-OP-CENSUS-015**: When clj-surgeon exposes the census, it shall expose it as the read-only `relation_census` MCP tool and as the `:relation-census` CLI op, and neither surface shall write any file.

## Input bounds

- [x] **MCP-OP-CENSUS-016**: When clj-surgeon receives a census request, it shall validate server-side, before any filesystem work, that no unknown field is present, that `files` when supplied is a non-empty array of at most 512 non-blank strings, that `doors` when supplied is an array of at most 32 entries, and that `pool_size` when supplied is an integer between 1 and 64, and it shall refuse a violation with a typed reason, the offending bound, and an executable `next_call`; when a valid `pool_size` exceeds the box's available processors, clj-surgeon shall run the plan phase on a pool of the available processors and shall publish both the pool it used and the pool that was requested.

- [x] **MCP-OP-CENSUS-017**: When clj-surgeon reads a scanned source, it shall test that source for `defmethod` arms as it is read and shall retain the text only of the sources that define arms; it shall refuse a requested source larger than `max-source-bytes` with a typed reason naming the file, its size, and the cap, before reading it; and if any `Throwable` escapes the census, clj-surgeon shall publish a typed refusal — `census-resource-exhausted` for a runtime resource failure and `census-adapter-failure` otherwise — carrying an executable narrower `next_call` and no counts.

- [x] **MCP-OP-CENSUS-018**: When clj-surgeon discovers the files to census, it shall walk the workspace without following symbolic links, shall prune a skipped directory before reading it, shall stop the walk once the scanned-file cap is reached, and shall skip and count in `skipped_outside_root` every discovered path whose real location escapes the workspace root rather than refusing the census.

- [x] **MCP-OP-CENSUS-019**: When clj-surgeon executes the `:relation-census` CLI op, it shall parse `:threads` through the same pool-size kernel as the MCP tool and refuse an out-of-range or non-integer value typed, shall validate `:doors` through the same door kernel and refuse a door that shadows a collection write head, and shall bound its scan by the same scanned-file and source-byte caps, refusing a named source above the byte cap.

- [x] **MCP-OP-CENSUS-020**: When clj-surgeon witnesses that the census answer is pool-size independent, it shall run at least two arm-defining files through the plan phase, and the plan-phase pool shall map every input exactly once, shall use no more threads than the pool size it was given, and shall leave no worker thread alive after the call returns.

- [x] **MCP-OP-CENSUS-021**: When clj-surgeon runs the plan phase from either entrance, it shall run it on the bounded `census_pool` pool whenever a pool larger than one is both requested and available, and the receipt's pool size shall be the pool that actually ran; when the pool that ran is smaller than the pool requested, clj-surgeon shall publish the requested size alongside it.

- [x] **MCP-OP-CENSUS-022**: When clj-surgeon bounds a census receipt, the 4096-byte budget shall hold for the receipt as PUBLISHED, including the workspace root and elapsed time appended after bounding, and clj-surgeon shall trim listed sites first and per-file counts last, marking any trimmed receipt `receipt_truncated`.

- [x] **MCP-OP-CENSUS-023**: When no `doors` are supplied, clj-surgeon shall not run a declaration pass and shall parse each censused file exactly once; when `doors` are supplied, clj-surgeon shall check the door symbols before the census and confirm that each is defined using the declarations the plan phase already returns; and `phases_elapsed_ms` shall name only phases that ran — `discover` only when a tree was walked, `read` for resolving and reading the scan, and `classify` and `merge` from the plan.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-CENSUS-001` | A textual match on `conj` is a sufficient site finder. | `(fnil disj #{})` contributes no site; a door call is a site; a bare `conj` update fn is a site. |
| `MCP-OP-CENSUS-002` | A helper's write is the arm's write. | The real `conj-once`/`upsert-by` bodies counted, never classified. |
| `MCP-OP-CENSUS-005` | A structural match on the write is enough. | The real task-chase arm: the `not-any?` on `:chase-id` three lines above the write. |
| `MCP-OP-CENSUS-006` | A guard on the right key is a guard. | A `some` test that adds on presence. |
| `MCP-OP-CENSUS-007` | An analyzer may reason through a helper predicate. | A helper carrying the written identity; an unresolvable target in a real repository. |
| `MCP-OP-CENSUS-008` | Unclassifiable means vulnerable. | The pre-fix announced-speaker shape is the only `:raw` in the fixture. |
| `MCP-OP-CENSUS-011` | A thread pool may reorder a report. | Pool 1 and pool N receipts compared byte for byte minus timings. |
| `MCP-OP-CENSUS-014` | A refusal may be untyped or advisory. | Missing workspace; no arms; unparseable file; unknown door. |
