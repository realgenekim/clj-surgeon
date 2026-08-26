# Owner hallucinations need one-shot evidence

Date: 2026-08-25 Pacific time
Issues: `clj-surgeon-p24`, `clj-surgeon-wjz`

## Finding

Agents often submit a plausible top-level form name that does not exist. The
safe refusal prevents a wrong read, but the current response often causes a
second discovery route through `rg`, an outline, `sed`, or several new reads.
The mission is not to let fuzzy matching select source. The mission is to put
enough real owner evidence in the first refusal that the model can correct its
own hypothesis in one exact retry.

The usage window from `2026-08-25T00:49:16Z` through
`2026-08-26T06:19:26.362791Z` recorded 13
`batch-form-selection-failed` results in 144 MCP calls. That is 9.03% of calls.
The existing field gate is below 5%.

## Observed pairs

The following patterns come from the same Codex session. Six are strict
one-to-one corrections confirmed by a subsequent exact read. The others are
useful negative, inferred, path, or vocabulary-migration cases. Only the six
strict pairs belong in the ranker acceptance corpus.

| Requested owner or location | Subsequent route | Evidence class | Pattern |
|---|---|---|---|
| `resolve-source-file` | `resolve-source-path` | strict pair | one changed token |
| `start-server!` | `start` | strict pair | shared prefix and shortened name |
| `editor-program-schema` | `editor-programs-schema` | strict pair | one inserted character |
| `editor-edit-schema` | `editor-gesture-schema` | strict pair | shared frame, semantic middle token |
| `upsert-managed-block` | outline exposed `upsert-workspace-block` | anchor-inferred | shared frame, semantic middle token |
| `validate-request` | caller dropped the request | negative case | hallucinated extra owner |
| `validate-request!`, `compile-change`, `compile-request` | two different contract owners replaced three guesses | ambiguous vocabulary migration | older conceptual vocabulary |
| `tools-list-publishes-the-compact-editor-contract` | `exposes-exactly-four-typed-tools` | strict pair | semantic test-name paraphrase |
| `tool-profiles-expose-only-the-intended-catalog` | `tool-profiles-preserve-full-default-and-isolate-the-editor` | strict pair | semantic test-name paraphrase |
| `one-http-session-observes-live-tool-add-replace-and-remove` in `mcp_server_test.clj` | the same owner in `mcp_http_server_test.clj` | wrong-file case | correct owner, wrong file |
| `compiles-owner-relative-top-level-insertion` | native source exposed `validates-top-level-insertion-without-repeating-owner-source` | source-inferred | semantic test-name paraphrase |

These examples show two distinct jobs:

1. Character and token ranking can place a likely owner near the top.
2. The model still owns semantic interpretation and the decision to retry.

## Current ranking defect

The MCP projection currently returns one aggregate `form_candidates` vector.
The candidate score considers all requested names, including names that already
resolved. A successful requested name can therefore occupy a top candidate
position while the missing owner's useful correction falls outside the bounded
list. The response also drops the exact missing owner from the concise text.

Rank candidates independently for each missing owner. Do not rank one combined
list for the complete request.

## Proposed refusal shape

For each missing owner, return:

```text
request_id
file
requested_owner
failure_kind
available_owner_count
available_owners
did_you_mean[0..9]
  owner
  score
  ranking_basis
  authoritative = false
candidates_returned
candidates_truncated
source_hash
```

The concise result must name the failed request and missing owner, then show the
first hypothesis as a question:

```text
paths: owner `resolve-source-file` does not exist in mcp_paths.clj
I think you may have meant `resolve-source-path`? (hint only)
→ retry with one exact owner
```

The structured result returns every available owner name when the name-only
vector fits the public result budget. It also returns up to ten ranked owners
per missing owner for a fast first glance. It must disclose the complete
available-owner count and truncation state. The kernel ranks against the
complete owner universe even when presentation must omit names.

## Matching experiment

Evaluate at least these pure rankers against the observed pairs:

1. Normalized Levenshtein distance.
2. Normalized Damerau-Levenshtein distance.
3. Kebab-token overlap with ordered prefix and suffix features.
4. Character trigram similarity.
5. A deterministic hybrid of the best character and token features.

Measure rank@1, rank@3, rank@10, mean reciprocal rank, and returned characters
against the six strict pairs. Use the other patterns as adversarial examples,
not labeled corrections. Prefer the simplest ranker whose rank@10 is not
materially worse than the best hybrid.

Ranking is a hypothesis channel. No score threshold, unique top result, or gap
between scores can create selection authority. Only an exact relation over the
frozen source can emit an executable correction.

## Route gate

```text
current: refusal -> rg/outline/sed -> inspect -> exact read
target:  refusal with evidence     -> exact read
```

For mechanically recoverable and model-correctable owner mistakes, the target
is at most two Surgeon calls, zero native discovery calls, and zero false
automatic selections.

## Evidence

- Usage receipt:
  `/tmp/clj-surgeon-agent-usage-20260825T004916Z-20260826T061926362791Z.json`
- Raw refusal and recovery session:
  `/Users/genekim/.codex/sessions/2026/08/23/rollout-2026-08-23T06-18-00-01a02ec5-5a82-7870-ba86-5916e8b130ae.jsonl`
- Two motivating refusals:
  `/Users/genekim/.codex/sessions/2026/08/25/rollout-2026-08-25T23-13-44-01a03cb3-ff16-7bb3-95da-ab29207ef91a.jsonl`
