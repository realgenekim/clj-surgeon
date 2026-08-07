# Proof-Carrying Change Buffer

**Status:** Proposed experiment

**Motivating evidence:**

- [The 3x mechanism exists, but the product is not there](../observations/2026-08-07-captains-log-the-3x-mechanism-exists-but-the-product-is-not-there.md)
- [Three Rounds roadmap](three-rounds-roadmap.md)
- [The Code Reader/Explorer Frontier](../code-reader-explorer-frontier.md)

## Outcome

`inspect_clojure` returns a task-shaped, proof-carrying change buffer. The
buffer contains the minimum exact source and mechanical context needed for one
model decision. It also contains the exact next `apply_clojure_changes` call.

The model fills each explicit decision hole with `keep` or `replace`. The model
does not repeat files, owners, selectors, counts, hashes, before-source, or
verification commands.

The intended route is:

```text
goal
  -> prepare one change buffer
  -> fill every decision hole once
  -> apply and verify once
```

The common route has one perception action, one model decision, and one
mutation action. A prompt that already supplies the complete decision can use
the existing direct one-call mutation route.

## Terminology

- **Change buffer:** One immutable inspection result prepared for a subsequent
  mutation.
- **Basis:** The server-held source snapshot, evidence, selections, assertions,
  and verification profile for one change buffer.
- **Decision site:** One exact, lossless source selection that the model must
  keep or replace.
- **Owner projection:** A bounded structural skeleton of a top-level owner. It
  contains decision-site placeholders and mechanical negative space.
- **Decision hole:** A `null` value in the generated apply request.
- **Evidence item:** A read-only fact that informs the decision but is not a
  mutation target.
- **Uncertainty:** A fact that the named authorities could not prove. An
  uncertainty can require explicit acknowledgement before mutation.

`basis` and decision-site identifiers are short-lived continuation tokens.
They are not durable source addresses. Durable evidence continues to name
semantic owners, source hashes, and the original prepare request.

## Bitter-Lesson Boundary

The caller chooses the subject, evidence relationships, and desired source.
The tool does not interpret a natural-language goal. It does not recommend an
architecture, invent a replacement, infer which sites should change, or treat
an unresolved relationship as absent.

The tool performs mechanical work:

- resolve the explicitly requested relationships;
- anchor semantic locations to lossless source nodes;
- deduplicate sites;
- construct bounded owner projections;
- retain snapshot hashes and exact before-source;
- generate the next mutation request;
- compile, validate, commit, and verify the supplied decisions.

The first experiment supports `keep` and exact one-form `replace`. Deletion,
insertion, movement, capture transforms, and broadcast transforms are outside
this experiment. They can be added only after a frozen task proves that exact
replacement is insufficient.

## Public Contract

### Tool names

This feature does not add another MCP tool.

- `inspect_clojure` gains `mode: "prepare-change"`.
- `apply_clojure_changes` gains a basis-based request form.

Existing inspection and direct mutation requests remain valid.

### Prepare request

The common request names one fully qualified Var:

```json
{
  "request_version": 1,
  "mode": "prepare-change",
  "subject": {
    "var": "clj-surgeon.mcp/normalize-success-receipt"
  }
}
```

The repository supplies bounded defaults. The caller does not need to select
evidence relationships, projections, budgets, or verification for the common
case.

The server normalizes the common request to this complete request:

```json
{
  "request_version": 1,
  "mode": "prepare-change",
  "subject": {
    "var": "clj-surgeon.mcp/normalize-success-receipt"
  },
  "evidence": [
    "definition",
    "resolved-references",
    "direct-callers",
    "tests"
  ],
  "decision_sites": [
    "definition",
    "resolved-reference-sites",
    "test-sites"
  ],
  "context": {
    "projection": "owner-skeleton",
    "max_depth": 4
  },
  "budget": {
    "max_sites": 24,
    "max_visible_chars": 12000
  },
  "verify": "fast"
}
```

MCP tool arguments are JSON. Clojure source remains a string and must contain
exactly one complete form.

The caller can override `evidence`, `decision_sites`, `context`, `budget`, or
`verify` with values allowed by the closed schema. Omitted fields use the
repository defaults shown above. The successful response records the complete
normalized request under `reprepare`.

#### Subject

Version 1 accepts exactly one fully qualified Var in `subject.var`. The server
resolves the Var through the configured hot semantic authority. Zero or many
definitions refuse the request.

Workspace form patterns and multiple subjects are separate experiments. They
must not complicate this first contract.

#### Evidence relationships

The default relationship set is `definition`, `resolved-references`,
`direct-callers`, and `tests`. Version 1 accepts these values:

| Value | Mechanical meaning |
|---|---|
| `definition` | The resolved Var definition and its lossless top-level owner |
| `resolved-references` | All Var references returned by the configured semantic authority |
| `direct-callers` | Direct incoming call-hierarchy owners returned by the semantic authority |
| `tests` | Resolved references whose canonical file is under a repository-configured test root |

Every relationship reports its authority, returned count, omitted count, and
completion status. `complete: true` means complete only for the named authority
and query. It never means that dynamic Clojure dispatch is impossible.

#### Decision-site projections

The default decision-site set is `definition`, `resolved-reference-sites`, and
`test-sites`. Version 1 accepts these values:

| Value | Selected source |
|---|---|
| `definition` | The complete lossless top-level definition owner |
| `resolved-reference-sites` | The smallest complete parent form that contains each resolved reference |
| `test-sites` | The smallest complete parent form for each reference under a configured test root |

The server deduplicates identical file ranges. One range produces one decision
site even when several evidence relationships reach it.

#### Owner skeleton

The default context is `owner-skeleton` with `max_depth: 4`.

An owner skeleton retains:

- the top-level head, name, and argument vectors;
- the ancestor path from the owner to each decision site;
- one placeholder for each decision site;
- the heads and child counts of collapsed siblings;
- the omitted form count, omitted character count, and subtree hash.

The owner skeleton does not repeat decision-site source. Exact source appears
once in the `sites` collection.

The placeholder syntax is display-only:

- `<dN>` identifies decision site `dN`.
- `<collapsed:N>` identifies `N` collapsed sibling forms.

An owner skeleton is never parsed or applied. The server retains the exact
lossless owner and its hashes inside the basis.

#### Budget

The repository defaults are 24 sites and 12,000 visible characters. The server
applies both limits before it publishes a basis.

- `max_sites` limits deduplicated decision sites.
- `max_visible_chars` limits the complete agent-visible response, including
  exact source and owner skeletons.

If either limit is exceeded, the server returns no basis. The refusal reports
counts by file and includes one executable narrower prepare request.

#### Verification profile

`verify` names a closed repository-configured profile. The profile declares
commands, timeouts, output limits, and pass criteria. The request cannot supply
an arbitrary shell command.

If the caller omits `verify`, the server injects the repository's default
change profile. The example repository default is `fast`.

An unknown profile refuses the prepare request before a basis is published.

### Successful prepare response

The normative response is `structuredContent`:

```json
{
  "schema": "clj-surgeon/change-buffer-v1",
  "status": "decision-required",
  "basis": "cb-7f3a",
  "snapshot": {
    "authority": "clj-surgeon",
    "files": 3,
    "coherent": true
  },
  "coverage": [
    {
      "relationship": "definition",
      "authority": "clojure-lsp",
      "returned": 1,
      "omitted": 0,
      "complete": true
    },
    {
      "relationship": "resolved-references",
      "authority": "clojure-lsp",
      "returned": 3,
      "omitted": 0,
      "complete": true
    },
    {
      "relationship": "tests",
      "authority": "clojure-lsp+repository-test-roots",
      "returned": 1,
      "omitted": 0,
      "complete": true
    }
  ],
  "owners": [
    {
      "owner": "clj-surgeon.mcp/normalize-success-receipt",
      "signature": "[kernel-result]",
      "projection": "<d1>",
      "negative_space": {
        "omitted_forms": 4,
        "omitted_chars": 812
      }
    },
    {
      "owner": "clj-surgeon.mcp/classify-kernel-result",
      "signature": "[result]",
      "projection": "(defn classify-kernel-result [result] (case <collapsed:3> <d2>))",
      "negative_space": {
        "omitted_forms": 7,
        "omitted_chars": 1194
      }
    },
    {
      "owner": "clj-surgeon.mcp-test/normalizes-success",
      "signature": "[]",
      "projection": "(deftest normalizes-success <d3>)",
      "negative_space": {
        "omitted_forms": 2,
        "omitted_chars": 218
      }
    }
  ],
  "sites": [
    {
      "id": "d1",
      "role": "definition",
      "owner": "clj-surgeon.mcp/normalize-success-receipt",
      "before": "(defn normalize-success-receipt [kernel-result]\n  {:status :ok :result kernel-result})"
    },
    {
      "id": "d2",
      "role": "resolved-reference-site",
      "owner": "clj-surgeon.mcp/classify-kernel-result",
      "before": "(normalize-success-receipt result)"
    },
    {
      "id": "d3",
      "role": "test-site",
      "owner": "clj-surgeon.mcp-test/normalizes-success",
      "before": "(is (= {:status :ok :result result}\n       (normalize-success-receipt result)))"
    }
  ],
  "uncertainties": [],
  "next_call": {
    "tool": "apply_clojure_changes",
    "arguments": {
      "request_version": 1,
      "basis": "cb-7f3a",
      "decisions": {
        "d1": null,
        "d2": null,
        "d3": null
      },
      "acknowledge": {},
      "verify": "fast"
    }
  },
  "reprepare": {
    "tool": "inspect_clojure",
    "arguments": {
      "request_version": 1,
      "mode": "prepare-change",
      "subject": {
        "var": "clj-surgeon.mcp/normalize-success-receipt"
      },
      "evidence": [
        "definition",
        "resolved-references",
        "direct-callers",
        "tests"
      ],
      "decision_sites": [
        "definition",
        "resolved-reference-sites",
        "test-sites"
      ],
      "context": {
        "projection": "owner-skeleton",
        "max_depth": 4
      },
      "budget": {
        "max_sites": 24,
        "max_visible_chars": 12000
      },
      "verify": "fast"
    }
  }
}
```

The MCP text content is one short status line:

```text
READY cb-7f3a | 3 decision sites | 3 owners | 0 unresolved | 10,842 visible chars. Fill every null in structuredContent.next_call.arguments.decisions.
```

Source appears only in `structuredContent`. The text content does not duplicate
source, coverage, or the next-call object.

The agent-visible result omits file paths, line numbers, source hashes,
selection hashes, and transaction internals. The basis retains them. The model
receives only facts that can change its decision or its confidence in coverage.

### The exact model action

The model copies `next_call.arguments` without changing `basis`, decision-site
identifiers, acknowledgement identifiers, or `verify`. It replaces every
decision `null` with exactly one action.

Keep a site:

```json
{"keep": true}
```

Replace a site:

```json
{"replace": "(normalize-success-receipt result context)"}
```

The action object must contain exactly one of `keep` or `replace`. `keep` must
be `true`. `replace` must contain exactly one complete Clojure form.

The caller must answer every decision site. Missing and `null` decisions
refuse. Extra decision identifiers refuse. The tool never interprets omission
as `keep`.

Unknown request and action fields refuse. The server does not silently ignore
misspelled fields.

### Uncertainty acknowledgement

An uncertainty has a stable identifier, authority, reason, and
`requires_acknowledgement` field. A required acknowledgement creates a `null`
hole under `next_call.arguments.acknowledge`:

```json
{
  "u1": null
}
```

The model must replace the value with `true` to continue. The tool does not
accept explanatory prose in this field. A missing, `null`, or false value
refuses before compilation.

Acknowledgement does not convert uncertainty into proof. The terminal receipt
retains the acknowledged uncertainty.

### Basis-based apply request

After the model fills the holes, the complete call is:

```json
{
  "request_version": 1,
  "basis": "cb-7f3a",
  "decisions": {
    "d1": {"replace": "(defn normalize-success-receipt [kernel-result]\n  {:status :ok :value kernel-result})"},
    "d2": {"replace": "(normalize-success-receipt result context)"},
    "d3": {"keep": true}
  },
  "acknowledge": {},
  "verify": "fast"
}
```

The apply request does not contain files, owners, selectors, before-source,
expected counts, source hashes, result hashes, commands, plan paths, or receipt
paths.

### Apply behavior

`apply_clojure_changes` performs these phases inside one tool call:

1. Resolve the immutable basis.
2. Validate the complete decision and acknowledgement sets.
3. Confirm every file and selection hash against the current workspace.
4. Parse every replacement as exactly one complete form.
5. Compile all replacements against the retained lossless selections.
6. Parse every complete candidate file.
7. Commit the combined candidate with existing transaction atomicity.
8. Run the named verification profile after the commit.
9. Return one terminal receipt.

The server does not rerun the original semantic query during apply. It applies
the exact retained selections or refuses on drift.

### Successful terminal receipt

```json
{
  "schema": "clj-surgeon/change-buffer-receipt-v1",
  "status": "applied",
  "basis": "cb-7f3a",
  "changed": {
    "files": 2,
    "sites": 2,
    "kept_sites": 1
  },
  "source": {
    "snapshot_guard": "passed",
    "parsed": true,
    "atomic_write": true,
    "read_back_verified": true
  },
  "verification": {
    "profile": "fast",
    "status": "passed",
    "elapsed_ms": 1840,
    "summary": "42 tests, 318 assertions"
  },
  "acknowledged_uncertainties": [],
  "undo": {
    "receipt": "change-91af"
  }
}
```

The MCP text content is one line:

```text
APPLIED change-91af | 2 sites / 2 files | parse PASS | fast PASS: 42 tests, 318 assertions | 1.84 s
```

The default receipt omits the diff and unchanged source. The full transaction
receipt, hashes, diff, and inverse remain available by receipt identifier.

### No-change receipt

If every decision is `keep`, the tool performs no write and returns:

```json
{
  "schema": "clj-surgeon/change-buffer-receipt-v1",
  "status": "no-change",
  "basis": "cb-7f3a",
  "changed": {"files": 0, "sites": 0, "kept_sites": 3},
  "verification": {"profile": "fast", "status": "not-run"}
}
```

An exact replacement equal to the retained before-source is normalized to
`keep`.

### Verification failure

Verification runs after a successful source commit. Version 1 does not
automatically undo a commit after verification failure because external tests
can have effects that the source transaction cannot roll back.

The receipt uses `status: "applied-verification-failed"`. It reports the source
proof, failed verification proof, bounded output, and executable
`undo-change!` receipt. It never reports that the complete operation rolled
back.

### Refusal contract

Every refusal returns a stable `error_type`, performs no source write when
compilation has not committed, and includes one executable next call.

| Condition | `error_type` | Remedy |
|---|---|---|
| Subject resolves zero times | `subject-not-found` | Corrected prepare request |
| Subject resolves many times | `ambiguous-subject` | Prepare request with canonical Var identity |
| Semantic and lossless snapshots disagree | `authority-snapshot-drift` | Repeat the normalized prepare request |
| Site or output budget exceeded | `change-buffer-budget-exceeded` | Narrower prepare request with per-file counts |
| Unknown verification profile | `unknown-verification-profile` | Request listing configured profiles |
| Basis expired or server restarted | `basis-unavailable` | Exact `reprepare` call |
| Source changed after prepare | `basis-stale` | Exact `reprepare` call |
| Missing or `null` decision | `incomplete-decision` | Same apply request with remaining holes |
| Unknown decision identifier | `unknown-decision-site` | Original generated next call |
| Missing uncertainty acknowledgement | `unacknowledged-uncertainty` | Same apply request with acknowledgement holes |
| Invalid or multi-form replacement | `invalid-replacement-form` | Same apply request with the offending decision identified |
| Candidate file does not parse | `candidate-parse-failed` | Same apply request with the failing decision and parser diagnostic |

Diagnostics do not dump the complete basis. They identify one violated
contract and return one bounded executable remedy.

## Safety Invariants

1. Prepare is read-only.
2. Prepare reads each canonical file at most once for one coherent snapshot.
3. Source appears at most once in the agent-visible prepare result.
4. The server publishes no basis when a visible budget is exceeded.
5. Every decision site has one exact file range, before-source, and hash.
6. Apply requires one explicit decision for every site.
7. Apply requires explicit acknowledgement for every required uncertainty.
8. Apply never reruns a selector against potentially changed source.
9. Any pre-commit refusal leaves every source byte unchanged.
10. A successful multi-file commit preserves existing transaction atomicity.
11. Verification proof remains distinct from source-transaction proof.
12. Verification failure never implies that external effects were rolled back.
13. A missing server-side basis always has an executable reprepare path.
14. The default response never repeats a diff or unchanged source.

## Implementation Shape

### Functional core

Public pure functions take data and return data:

- normalize and validate prepare requests;
- join semantic locations to lossless owners and selections;
- deduplicate relationship results by canonical file range;
- construct owner skeletons and negative-space records;
- enforce visible budgets;
- generate exact apply templates;
- validate complete decisions and acknowledgements;
- compile decision actions into the existing transaction manifest;
- normalize terminal receipts and refusal remedies.

Pure tests use literal semantic results and parsed source strings. They do not
create temporary files or start MCP, cclsp, or verification processes.

### Imperative shell

Thin boundary functions:

- query the configured hot semantic authority;
- read canonical files once;
- publish and resolve immutable basis records;
- call the existing intent transaction compiler and commit path;
- invoke one repository-configured verification profile;
- emit MCP text and structured content.

The basis store is bounded by count, total bytes, and time to live. Eviction is
observable as `basis-unavailable`; it is never interpreted as source drift.

### Reuse

The implementation must reuse:

- cclsp or clojure-lsp for semantic relationships;
- rewrite-clj snapshots and existing structural addresses;
- the existing MCP path confinement and output budgets;
- the intent transaction compiler, candidate validation, atomic commit,
  receipts, and undo;
- repository-owned verification configuration.

It must not add another semantic index, mutation engine, or address language.

## Test Plan

### Pure behavior matrix

| Dimension | Required cases |
|---|---|
| Subject resolution | zero, one, many, wrong platform |
| Relationship result | empty, singleton, several, duplicate routes to one range |
| Authority | complete, partial, unavailable, stale snapshot |
| Test classification | configured test root, source root, path lookalike outside root |
| Selection | definition, direct call, higher-order reference, metadata-wrapped form, reader conditional |
| Owner projection | one site, several sites, nested sites, collapsed siblings, depth boundary |
| Negative space | zero omitted, omitted forms, omitted characters, stable hash |
| Budget | exact boundary, one site over, one character over, per-file refusal counts |
| Decision set | complete, missing, `null`, extra, duplicate JSON key after parsing |
| Action | keep, exact replacement, identical replacement, empty string, malformed form, several forms |
| Uncertainty | none, optional, required and accepted, required and missing |
| Basis | present, expired, evicted, stale file, stale selection, unknown identifier |
| Candidate | one file, several files, parse success, parse failure, overlapping replacements |
| Verification | pass, fail, timeout, unavailable command, output truncation |
| Receipt | applied, no-change, applied-verification-failed, bounded refusal |

Every pre-commit refusal asserts byte-for-byte unchanged source in the pure
candidate model. Boundary tests assert the same property on copied fixtures
only where filesystem behavior is under test.

### Field-failure regression

Freeze the real `normalize-success-receipt` return-contract benchmark that
currently requires relationship discovery, owner recovery, manifest
construction, and separate verification.

The regression must prove:

- all resolved callers and tests appear in one buffer;
- owner projections provide enough context for a correct decision;
- the response contains the exact next apply call;
- the caller supplies no mechanical address or count;
- one apply call changes all selected sites and verifies the result;
- the caller performs no post-success read or diff.

### Real-program-derived fixtures

Add minimized fixtures for:

- a direct caller and a higher-order reference in different files;
- two relationships that resolve to the same source range;
- one test reference under a configured test root;
- comments, metadata, reader shorthand, and reader conditionals around a
  selected site;
- a stale semantic position that must refuse before a basis is published.

Record the source snapshot and the behavior preserved by each fixture.

### Boundary tests

Test only contracts that cross a real boundary:

- one hot semantic query joins to one coherent lossless snapshot;
- canonical path confinement rejects files outside the workspace;
- basis eviction returns the exact reprepare call;
- apply delegates to the existing atomic transaction path;
- verification profiles run the configured command and bound output;
- MCP annotations remain read-only for prepare and destructive for apply;
- text content contains no duplicate source;
- the documented prepare and apply calls work over stdio and HTTP.

### Clean-context caller simulation

Give a fresh Codex and a fresh Claude caller:

- the repository instruction that names the two MCP tools;
- one goal-level return-contract prompt;
- no expected patch, implementation source, plan, or prior transcript.

Require each caller to:

1. Make one prepare call.
2. Fill every decision and acknowledgement hole.
3. Make the generated apply call.
4. Stop after the terminal receipt.

Any guessed address, source reread, omitted hole, manual diff, separate test
call, or post-success inspection is a product defect.

## Performance Experiment

Run four counterbalanced correct replications of:

- native tools;
- current cclsp plus `inspect_clojure` plus direct
  `apply_clojure_changes`;
- the proof-carrying change buffer.

Use the frozen return-contract goal and starting commit. Do not expose the
expected patch.

Keep the feature only if:

- every treatment run is correct;
- treatment median is at most 14.397 seconds against the existing
  43.190-second native median, or at least 3x faster than a newly replicated
  best correct control;
- the treatment removes at least 10.1 seconds from the current 24.530-second
  MCP write route;
- each treatment run uses one prepare call and one apply call;
- the model receives no more than 12,000 visible characters;
- source appears once;
- the model repeats no files, owners, selectors, counts, hashes, commands, or
  receipt paths;
- no treatment run performs a post-success source read, diff, or test call.

If the treatment is correct but misses the wall-time gate, retain the raw
results outside the repository and diagnose time by model phase before adding
features.

## Documentation and Release Checklist

- Update MCP tool schemas and server instructions.
- Add the short common request and generated next-call example to README.
- Update the repository Codex and Claude skill from one shared source.
- Document repository verification-profile configuration.
- Document basis lifetime and exact recovery behavior.
- Add the refusal table to help without duplicating the complete design plan.
- Record the benchmark in a Captain's Log.
- Run `make install` only after the feature passes its keep gate.

## Verification Gates

Before completion:

1. Format all changed Clojure files with the repository formatter.
2. Run focused pure tests for request normalization, projections, decisions,
   budgets, and receipts.
3. Run MCP HTTP, stdio, hot-reload, confinement, and annotation tests.
4. Run the real-program regression and candidate compilation check.
5. Run clj-kondo with zero errors and zero warnings on changed code.
6. Run `make test`.
7. Run `git diff --check`.
8. Run the clean Codex and Claude simulations.
9. Run the counterbalanced performance experiment.

## Definition of Done

The feature is complete when a fresh caller receives one bounded change
buffer, fills only explicit `keep` or `replace` holes, submits the generated
apply request unchanged otherwise, and receives one terminal source and
verification receipt. All treatment runs must be correct. The replicated
treatment median must be at least 3x faster than the best correct control and
must remove at least 10.1 seconds from the current MCP route. Any caller-managed
address, count, hash, manifest reconstruction, diff, or separate verification
round means the implementation is incomplete.
