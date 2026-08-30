# Write-side refusal completeness audit

<!-- agent-usage-window-end: 2026-08-30T14:21:37.179314Z -->

Date: 2026-08-30 PT
Base: `origin/release/closed-relations-published` at `c55de2279826af5ed21c90981591479dd2e802b2`
Method: local source audit plus retained telemetry; zero model calls
Verdict: **yes — the write side has the same defect class.**

The measured defect is the generic `:expect-count-mismatch` refusal in
`compile-intent-edits`. The compiler has already resolved every concrete match and has
already computed `per-form-counts`, but the refusal publishes only total and per-file
counts. In the retained window this exact site fired 7 times and 4 were followed by an
`inspect_clojure` recovery read: **4/7, 57.1%**. The other three were corrected without a
read. This is not a new causal estimate. The prior causal screen at commit `c1e89d5d`
held everything but refusal completeness fixed and measured recovery reads at 0/10 with
complete owner vocabulary versus 10/10 with truncated vocabulary (-100 percentage
points). The present audit identifies the same withheld-information mechanism on a real
write refusal and measures its retained-window tax.

## Scope and classification law

The audited public entrances are `edit_clojure`, `apply_clojure_changes`, and
`transform_clojure`. The source walk followed each entrance through request admission,
workspace/path confinement, compact-field/location/relation lowering, generic and
addressed transaction compilation, retained-basis application, extraction, transform
compilation, formatting, verification, commit, rollback, and MCP result publication.
Undo-only and prepare-change-only refusals are outside the table unless a public write
entrance can invoke them during commit or rollback.

Classification is about the information available at the instant of refusal:

- **complete** — the payload returns the full finite vocabulary/candidate facts needed
  for the caller's next decision. A stale guard or I/O failure is also complete when no
  finite alternative vocabulary exists and the payload names the exact failed authority,
  hashes/stage, and safe remedy; a safety-required reread is not counted as a vocabulary
  defect.
- **truncated** — the implementation mechanically has the relevant candidates or a
  more discriminating inventory, but publishes only a count, subset, or coarser
  projection.
- **bare** — the implementation mechanically has a relevant finite vocabulary or
  candidate set and publishes none of it.

One row below is one constructor site or a cluster of adjacent sites with the same public
payload law. Line numbers are from `c55de227`.

## Deficient sites

| Source site | Entrance / refusal type | Payload actually returned | Class | Firings | Recovery-read followups |
|---|---|---|:---:|---:|---:|
| `src/clj_surgeon/intent_transaction.clj:877` | edit/apply `expect-count-mismatch` | change index/id, expected and actual totals, complete per-file counts; **drops the already-computed `per-form-counts` and every resolved match owner/location** | **truncated** | **7** | **4** |
| `src/clj_surgeon/intent_transaction.clj:617` | edit/apply `change-owner-mismatch` for named/defmethod owners | requested owner, file, actual count, and only same-name matches; zero-match refusal returns an empty `candidates` vector although `records` contains the complete owner table | **truncated** | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:649` | edit/apply `change-owner-mismatch` for namespace owner | requested namespace owner, file, actual count; drops all namespace candidates already in `matches` | **bare** | 0 | 0 |
| `src/clj_surgeon/mcp_compact_location.clj:209-216` | edit/apply `compact-location-unresolved` | change index/id and generic “could not be proven”; drops the frozen file's owners, namespace facts, clause-kind facts, and which injective rule failed | **bare** | 0 | 0 |
| `src/clj_surgeon/extract.clj:253-280` → `src/clj_surgeon/mcp_extraction.clj:203-210` | apply extraction `extraction-plan-refused` for missing forms | missing requested names appear only in the error string; drops the complete movable owner vocabulary in `all-forms` | **truncated** | 0 | 0 |
| `src/clj_surgeon/mcp_program_tool.clj:110-112` | transform `transform-selection-too-large` | complete count plus no candidate rows; `evaluate-query` had produced the bounded candidate prefix and a truncation flag | **truncated** | 0 | 0 |
| `src/clj_surgeon/mcp_program_tool.clj:115-120` | transform `expected-count-mismatch` | expected and actual counts only; drops the complete bounded `found.matches` vector | **truncated** | 0 | 0 |
| `src/clj_surgeon/mcp_program_tool.clj:244-250` | edit/apply program `lossless-commit-refused` | program index/file and total match count; drops the exact comment-bearing selections already marked in the compiled edits | **truncated** | 0 | 0 |
| `src/clj_surgeon/mcp_program_tool.clj:347-351` | one-shot transform `lossless-commit-refused` | total match count only; drops the exact comment-bearing selections | **truncated** | 0 | 0 |
| `src/clj_surgeon/mcp_change_buffer.clj:1053-1065` | basis apply compact delete `no-match` / `ambiguous-match` | match count only; `find-subforms` returned the bounded match candidates but the wrapper discards them | **truncated** | 0 | 0 |
| `src/clj_surgeon/binding_rename.clj:230-233` | edit/apply `binding-identity-ambiguous` | file, owner, binding, actual count; drops the analyzer's complete binder candidates | **truncated** | 0 | 0 |
| `src/clj_surgeon/binding_rename.clj:149-151` | edit/apply `comment-sensitive-binding` | binding name only; drops file, owner, and the exact comment-bearing destructuring candidate | **bare** | 0 | 0 |
| `src/clj_surgeon/mcp_change_buffer.clj:1312-1330,1464,1542` | apply verification `unknown-verification-profile` / `invalid-exact-verification-profile` | error type (and sometimes requested profile) only; drops the complete configured profile vocabulary or the closed list of violated exact-profile fields/rules | **bare** | 0 | 0 |

The first row is stronger than a generic “counts are insufficient” complaint. In
`compile-intent-edits`, `per-form-counts` is materialized before the guard check and later
included in a successful compiled intent, but the refusal map at line 877 omits it. No new
parse, source read, or semantic call is needed to return that information.

## Complete-site inventory

All remaining reachable refusal constructors are complete under the law above. The table
states their recovery payload, not merely their error string. Except for the one
`invalid-intent-form` event shown below, none fired as a write refusal in this retained
window.

| Source site | Refusal types / family | Complete recovery evidence | Firings | Recovery reads |
|---|---|---|---:|---:|
| `src/clj_surgeon/mcp_workspace.clj:8-38` | `invalid-workspace-root` | exact field path, rejected value, invariant, and one corrective next action | 0 | 0 |
| `src/clj_surgeon/mcp_paths.clj:35-149` | invalid/escaping/missing/non-regular source; existing/invalid target; non-directory parent | exact rejected project-relative path and the violated confinement/state invariant; path choice remains caller authority | 0 | 0 |
| `src/clj_surgeon/mcp_contract.clj:130-784` | public `invalid-mcp-request` reasons: object/field/array/string/count/path, owner/action/scope, insertion, extraction, verify, duplicate ID/file/form | exact JSON path and reason; unknown-field cases include complete `unknown` and `allowed`, missing-field cases include complete `missing`, enum/operator messages enumerate the valid vocabulary, malformed insertion may carry the preserved null-hole retry template | 0 write-side (6 read-side events excluded) | 0 |
| `src/clj_surgeon/mcp_compact_edit_fields.clj:42-87` | `invalid-editor-field-pair` | supplied value fields plus all three valid complete pairs and exact alias mapping | 0 | 0 |
| `src/clj_surgeon/mcp_compact_relations.clj:78-547` | `invalid-compact-relation`, `compact-relation-path-conflict`, `require-change-unprovable`, `compact-relation-overlap` | closed failed stage, exact field/file/row path when local, mutation/write authority false, and `correct_request`; the public schema supplies the complete closed relation vocabulary | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:43-72` | `invalid-intent-form` | exact field/change, form count or parser failure, and the one-complete-form grammar/remedy | **1** | **0** |
| `src/clj_surgeon/intent_transaction.clj:166-579` | invalid/duplicate files, invalid counts/intents/changes/owners/actions/expectations, unknown arguments, no-op, mixed modes | exact change/file/field, actual value, complete unknown set, and complete supported operator/shape vocabulary where a choice exists | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:689,728` | `delete-owner-not-addressable`, `map-key-already-present` | exact file/owner and key or failed structural authority | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:797,888,898` | `change-distribution-mismatch` | complete requested distribution and complete per-file or per-form actual-count maps; line 797 states the only valid binding distribution | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:828` | binding rename `expect-count-mismatch` | expected/actual plus complete per-file and per-form occurrence maps; binder ambiguity is independently refused before this guard | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:937` | `overlapping-intents` | both change IDs/indexes and both complete conflicting compiled edits | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:982-1038,1115` | stale path/subform/span, unsupported insertion parent, ambiguous comment-bearing gap, protected namespace delete | exact stale authority or parent; insertion refusal includes target and complete gap source plus lossless remedy | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:1194-1679` | aggregate mismatch; invalid sources/canonical effect/future sources/addressed edits; compiler failures; source/options failures | expected/actual aggregate or exact invalid item/stage/file; these are closed contract or invariant failures, not hidden alternative-owner choices | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:1685-1827` | source read/hash, invalid compiled transaction, write failure/recovery required/exception | file and expected/actual hashes where available, causal error, rollback state, and per-file recovery evidence | 0 | 0 |
| `src/clj_surgeon/intent_transaction.clj:1900-2073` | invalid receipt/path, result-hash mismatch | exact receipt invariant/path or expected/actual result authority | 0 | 0 |
| `src/clj_surgeon/binding_rename.clj:23-70,141-147,191-204,235-249` | analyzer unavailable/failure, unsupported destructuring, invalid/unsafe rename, capture risk, source drift | admission/diagnostic evidence, supported `:keys` alternative, collision owners, or exact stale owner/row/column | 0 | 0 |
| `src/clj_surgeon/mcp_program_tool.clj:99,146-175,183-235,267-287,308-376` | transform required/program failure, generated output/batch/budget/no-op, invalid program/expectation, missing source, commit/transaction failures | exact program index/file, limits and actual quantities, DSL error, or transaction authority; no finite match vocabulary is withheld outside the deficient rows above | 0 | 0 |
| `src/clj_surgeon/edit_dsl.clj:353-431` and `src/clj_surgeon/structural_lens.clj:135-280,549-610` | invalid expression/query/path/platform/step/span | complete allowed symbols, capabilities, forms, query-step vocabulary, failing step/index, limits, and platform extensions | 0 | 0 |
| `src/clj_surgeon/mcp_extraction.clj:64-190` | extraction request, paths/forms/public forms/policy/hash/expect/caller shape/count | exact invalid field/value and the complete closed request grammar; empty caller-change count returns expected/actual | 0 | 0 |
| `src/clj_surgeon/mcp_extraction.clj:190-321` excluding missing-form row | stale plan hash, invalid/unsupported/missing public forms, extraction/file counts, caller collision/decision, invalid result | expected/actual or exact invalid/missing sets; caller-decision refusal returns every genuine unknown, completed frozen plan, and exact next call | 0 | 0 |
| `src/clj_surgeon/mcp_extraction.clj:323-350,430-535` | invalid future source, stale/appeared target or parent, read-back/write/recovery failure | exact file/directory, causal type, rollback state, and recovery vector | 0 | 0 |
| `src/clj_surgeon/mcp_formatter.clj:31-83` | invalid formatter, failure, timeout | complete command-shape requirement or exit/elapsed/bounded output | 0 | 0 |
| `src/clj_surgeon/mcp_change_buffer.clj:994-1042,1079-1229,1561-1693` excluding compact-match/profile rows | basis coverage/decision/no-op/address drift/owner coverage, invalid basis request, workspace/expiry/empty change, verification/rollback | coverage refusal returns complete expected and actual site vocabularies; decision refusal returns every offending site and complete valid action vocabulary; stale/verification paths name exact authority and remedy | 0 | 0 |
| `src/clj_surgeon/mcp_change_buffer.clj:1316,1340-1657`, `src/clj_surgeon/mcp_hot_verify.clj:58-113`, `src/clj_surgeon/mcp_cold_verify.clj:89-256` excluding profile-vocabulary row | ownership, process, diagnostic, hot/cold capacity/job, baseline and verification failures | exact ownership/outcome/process/diagnostic/job or rollback evidence; no caller-selectable candidate is hidden | 0 | 0 |
| `src/clj_surgeon/mcp_tool.clj:272-385,527-805,910-940` | baseline/verification, receipt publication, adapter/server initialization, edit-verification-authority refusal | exact phase/cause, verification projection, rollback/recovery, or explicit route remedy | 0 | 0 |
| `src/clj_surgeon/mcp_operation.clj:14-39` and `src/clj_surgeon/mcp_contract.clj:1321-1337` | invalid elapsed/domain/kernel result | exact violated adapter invariant and observed type/timing; internal invariant failure has no caller vocabulary | 0 | 0 |

## Retained-window cross-check

The privacy-safe `study-agent-usage` receipt has schema
`clj-surgeon.agent-usage-ethnography.v6`, status `ok`, and this exact window:

- UTC: `2026-08-30T02:09:33.141926Z` through
  `2026-08-30T14:21:37.179314Z`
- Pacific: `2026-08-29 19:09:33 PDT` through
  `2026-08-30 07:21:37 PDT`
- lower-bound marker source:
  `2026-08-29-write-side-emission-and-read-side-encoding-study.md`

`make study-agent-usage` completed once and reported 182 service calls: 116 reads and 66
writes, with 161 successes and 21 refusals. A local aggregate-only pass through the
collector's existing parsers cross-tabulated the retained service events without emitting
request content, result content, source, paths, prose, or raw service events:

| Service tool label | Refusal type | Count |
|---|---|---:|
| `apply_clojure_changes` | `expect-count-mismatch` | 7 |
| `apply_clojure_changes` | `invalid-intent-form` | 1 |
| `inspect_clojure` | `batch-form-selection-failed` | 7 |
| `inspect_clojure` | `invalid-mcp-request` | 6 |

The shared service telemetry labels the common write kernel
`apply_clojure_changes`. The privacy-safe Codex clock and six receipt-named bounded turn
regions attribute all eight typed write refusals to client `edit_clojure` calls. Those
regions retained only the operation/error sequence for this audit. They show seven
`expect-count-mismatch` refusals, one `invalid-intent-form`, four immediate
`inspect_clojure` recovery reads after count refusals, and no recovery read after the syntax
refusal. No prompt, source, request, result, project path, or command content was retained
in this document.

One additional failed `edit_clojure` action stopped at the client tool-schema boundary in
3 ms, produced no repository refusal type, and never appeared in service telemetry. It is
excluded from source-site counts because no audited constructor ran. There were no retained
`apply_clojure_changes` client refusals and no `transform_clojure` calls. Therefore zero in
the site table means observed zero in the complete retained service window, not absence by
inference.

## Ranked fix list

The ranking rule is observed firings first, then deficient class. Every zero-firing item is
tied empirically; its displayed order is route breadth, not a measured frequency ranking.
All IDs below are **candidate EARS IDs, pre-ratification**. They authorize no tests or code.

1. **Generic count refusal — 7 firings, 4 recovery reads.**
   `MCP-OP-WRITE-REFUSAL-001` *(pre-ratification)*: When a generic scoped change's exact
   match guard fails after frozen compilation, the refusal shall return the complete bounded
   resolved match inventory grouped by file and owner, including the already-computed
   per-form counts and source-free line/address facts, while preserving zero write and retry
   authority.
2. **Named and namespace owner resolution — 0 firings.**
   `MCP-OP-WRITE-REFUSAL-002` *(pre-ratification)*: When a write owner resolves zero or
   several times, the refusal shall return the complete name-only owner vocabulary for every
   implicated frozen file plus every exact same-name candidate; hypotheses shall not become
   selection authority.
3. **Compact location proof — 0 firings.**
   `MCP-OP-WRITE-REFUSAL-003` *(pre-ratification)*: When injective compact-location lowering
   cannot prove one location, the refusal shall name the failed injective rule and publish
   the complete bounded namespace/owner/clause candidates already derived for that edit,
   without guessing or emitting an executable retry.
4. **Transform selection and comment losslessness — 0 firings.**
   `MCP-OP-WRITE-REFUSAL-004` *(pre-ratification)*: When a transform count, result bound, or
   comment-bearing commit gate refuses, the payload shall return every bounded selected or
   offending source-free candidate available from the completed query; if the candidate set
   exceeds the public bound, it shall state the complete count and provide a guarded bounded
   continuation rather than silently discard the prefix.
5. **Extraction owner selection — 0 firings.**
   `MCP-OP-WRITE-REFUSAL-005` *(pre-ratification)*: When extraction names missing forms, the
   refusal shall publish every missing requested name and the complete name/type/line
   vocabulary of movable direct owners from the frozen source, without selecting one.
6. **Retained-basis compact edits — 0 firings.**
   `MCP-OP-WRITE-REFUSAL-006` *(pre-ratification)*: When a retained-owner compact edit finds
   zero or several subforms, the refusal shall retain the complete bounded match candidates
   already produced by `find-subforms` and the retained site ID, with no write authority.
7. **Binding rename ambiguity/comments — 0 firings.**
   `MCP-OP-WRITE-REFUSAL-007` *(pre-ratification)*: When binding identity is ambiguous or a
   destructuring comment blocks lossless rename, the refusal shall publish the exact file,
   owner, binding, and every bounded binder or comment-bearing candidate already known to
   the analyzer/compiler.
8. **Verification-profile admission — 0 firings.**
   `MCP-OP-WRITE-REFUSAL-008` *(pre-ratification)*: When a requested verification profile is
   absent or malformed, the refusal shall return the complete configured profile-name
   vocabulary or every violated closed exact-profile rule, while omitting commands and
   secrets.

## Decision

The write side is not uniformly defective: most refusals already carry complete grammar,
guard, rollback, or recovery evidence. But it is not protected by the read-side law either.
Thirteen source-site rows discard mechanically available recovery candidates, and the
highest-frequency one has a measured 4-recovery-read tax in seven firings. Ratification
should start with `MCP-OP-WRITE-REFUSAL-001`; its missing payload is already computed,
observed in production telemetry, and governed by the exact causal mechanism established in
sweep lane 1.
