# Three-Arm Request-Shape Model Screen Protocol

Date: 2026-08-29

Durable owner: `clj-surgeon-45j`

Status: protocol only. No model launch is authorized by this receipt.

## Decision

GO to prepare one capture-only, three-arm cohort after Faraday's independent
owner-aware gate is immutable and reviewed. NO-GO to launch before that gate,
or to edit product, install, reload, or use shared ports from this lane.

The three arms are:

- **F, flat control:** current canonical `edits` plus `delete_owners`;
- **A, file groups:** current tool plus closed `file_groups` expansion;
- **B, closed relations:** current tool plus Faraday's accepted
  `symbol_migration` rows verbatim and one frozen-source `require_change`.

This is a call-construction screen. The capture server records the first
arguments and writes no project source. The real offline compiler remains the
authority for exactness.

## Immutable starting evidence

- Product basis: `ce05f6ee099ac029d96ecb6db6f5f225e4239b96`
- Request-shape receipt: `e50bc08956789e63527f55890ddef13a1352c8cd`
- Production-lane preservation of that receipt: `356ae51`
- Frozen task SHA-256:
  `789809060a52d647197cf1fb5ade2cc0a76992209a0223991c7a51179f44d8e1`
- Frozen archive SHA-256:
  `e41da53fd2b973d3545f4608365416d40c20e24a8f865edccec161699563972f`

Retained 03-B payload measurements:

| Shape | Bytes | Reduction |
|---|---:|---:|
| F, flat | 6,353 | baseline |
| A, file groups | 5,189 | 18.3% |
| provisional B screen, alternative relation spelling plus require delta | 3,600 | 43.3% |

Every materialized shape re-expanded to the exact retained multiset of 33 edit
rows. That is 37 edit matches plus 14 exact owner deletions: 51 matches across
9 files.

The 3,600-byte result is a conservative shape-screen datum, not the byte count
for the candidate that may run. The exact B candidate reuses Faraday's smaller,
already-accepted `symbol_migration` language and adds only `require_change`.
Its fresh serialized byte count must be measured and retained when that
candidate becomes immutable; do not copy 3,600 into its result.

## Faraday dependency and non-overlap law

Faraday's prerequisite repair is now immutable:

```text
/private/tmp/clj-surgeon-owner-aware-screen.I5Mgyl/worktree
branch experiment/owner-aware-screen-prereq-corrected
base ce05f6ee099ac029d96ecb6db6f5f225e4239b96
head d77c65360d0784fe16dceb1c188a5c062e07ab78
tree 6f80d3e4ed1150ae4f7e73cdabc330ec5e831489
```

Exact Faraday paths and SHA-256s:

| Path | SHA-256 |
|---|---|
| `bench/run_owner_aware_call_construction_screen.sh` | `816dc5ac73f868d1d65bc50aa31660299c885e7230647f2b7079d2b6b15af2be` |
| `dev/experiments/owner_aware_call_capture_server.clj` | `688aa1bb088e4b62b0399b01cd1c10ebc5ab9b5f2271ec4e585f1b02c5eaba24` |
| `dev/experiments/owner_aware_call_capture_server_test.clj` | `4f8093500d2213cc099b89c6b43083c6b0d69466e8add63d38e807407aae5179` |
| `dev/experiments/owner_aware_call_construction_prereq.clj` | `15debe3e08a20cad7543afb19dea84b33861cc4c258c1960392b21d1881a2517` |
| `dev/experiments/owner_aware_call_construction_prereq_test.clj` | `0cd3c13d6c8593449bbab557c45461f3c5df9e101d4dd6cba7f72fd433cd9ff8` |

The committed checkpoint reports 8 retained captures, 4 unique requests, and
8/8 exact frozen futures. It adds a 24-variant canonical, `old`/`new`, and
`before`/`after` prerequisite gate. Faraday also owns the capture-clock repair,
one-tool registry parity, and the N=1-per-arm preflight controller.

This protocol must not duplicate or edit those files while Faraday works. The
future three-arm harness must start from Faraday's immutable accepted head and
reuse its:

- first-call capture server;
- capture clock;
- exact owner-row and future-hash scorer;
- field-vocabulary normalization corpus;
- one-tool client-registry observer;
- fresh-home preflight controller.

Treatment A adds only file grouping and is semantically independent of
Faraday's owner-aware relation. Treatment B must reuse Faraday's exact accepted
owner relation. Its only new compiler dimension is the require delta. If
Faraday's relation is rejected for an intrinsic first-call or ambiguity defect,
B is blocked. If it is exact but below the speed gate, B remains testable
because it deletes the separate 2,252-byte namespace-clause construction.

Exact Faraday commit `d77c653` and the hashes above are the prerequisite basis.
SURGEON1 must still accept Faraday's independent gate before any three-arm
model starts; immutability alone is not experimental approval.

## Client-visible single-tool law

Every arm exposes exactly one MCP tool named `edit_clojure` through one private,
capture-only server. Tool output schema, annotations, server instructions,
task bytes, model, effort, workspace, and terminal capture response remain
identical.

The Codex client registry receipt must prove the exact server name and the
exact one-tool set before each model starts. The observer may normalize only
the two already-proved client projections:

1. `annotations: null` to `{}`; and
2. removal of the input schema's top-level `anyOf`.

Any other description, nested schema, output schema, annotation, tool-set, or
provenance difference invalidates the run before model launch.

The prompt is identical across arms. It names the complete semantic decision
and says to call the single tool exactly once. It must not contain
`file_groups`, `symbol_migration`, `require_change`, or candidate examples.

### F: flat control surface

Use the exact accepted product description and input schema. The caller must
emit the canonical 33-row `edits` request and the exact deletion group.

### A: file-group surface

Add one closed top-level property to the control tool:

```json
{
  "file_groups": [
    {
      "file": "src/sample/views/review.clj",
      "edits": [
        {
          "within": {"form": "render-review"},
          "from": "review/row-controls*",
          "to": "submission-row/row-controls*"
        }
      ]
    }
  ]
}
```

The public-realistic candidate retains the existing flat fields for backward
compatibility. It exposes A, but does not expose B. The description states one
bounded rule: use `file_groups` when several exact edits share a file. A local
row cannot contain `file` or `files`. Omitted `matches` means exactly one.

The run is treatment-adherent only when the first call uses `file_groups` for
all 33 edits. A correct flat call in arm A is recorded as semantically exact
but does not count as an A success; it did not test the representation.

### B: closed-relation surface

Add only these two closed properties to the control tool:

```json
{
  "require_change": {
    "add": {
      "lib": "sample.views.submission-row",
      "as": "submission-row"
    },
    "files": [
      {"file": "src/sample/review_updates.clj"},
      {
        "file": "src/sample/views/log.clj",
        "remove": {"lib": "sample.views.review", "as": "review"}
      }
    ]
  },
  "symbol_migration": {
    "target_alias": "submission-row",
    "target_rule": "preserve-name",
    "columns": ["owner", "from", "matches"],
    "files": [
      ["src/sample/views/log.clj", [
        ["describe-rating", "review/fmt-stars", 3]
      ]]
    ]
  }
}
```

The public-realistic candidate retains flat `edits` for the one bespoke owner
replacement and retains the existing exact deletion group. It exposes B, but
does not expose A. It reuses Faraday's accepted `symbol_migration` row language
verbatim. A second `symbol_rewrites` property or alternative owner table is a
protocol violation.

The run is treatment-adherent only when the first call contains:

- one add target;
- nine require files and exactly three declared removals;
- nine `symbol_migration` file groups;
- twenty-three ordered owner/from/count rows and twenty-seven declared symbol
  matches;
- one retained complete owner edit;
- the exact fourteen-owner deletion group.

A correct flat call in arm B does not count as a B success.

## B must delete decisions, not conceal them

B passes this law only if the captured call explicitly supplies every semantic
choice that the flat oracle supplied:

| Decision | Must remain model-authored | May be compiled mechanically |
|---|---|---|
| changed files | all 9 exact paths | repeated file fields |
| symbol sites | all 23 ordered owner/from/count rows and 27 declared matches | target alias and complete to strings under exact `preserve-name` |
| cardinality | all 3 non-default counts; defaults are exactly 1 | insertion of default `matches: 1` |
| new namespace | exact lib and alias | repeated require entry text |
| old namespaces | all 3 exact lib/alias removals | lookup of the declared entry in frozen clauses |
| bespoke edit | exact complete old and new owner form | nothing |
| deleted owners | all 14 exact names | repeated transaction bookkeeping |

The B compiler accepts `(closed-request, frozen-source-map)` and returns the
canonical flat request or a typed refusal. It has no model, plan cache,
similarity search, semantic provider, or write authority.

The scorer must emit a `decision_coverage` receipt with exact sets and counts.
It must prove:

- every flat-oracle file, owner, old symbol, non-default count, require removal,
  bespoke edit, and deletion appears exactly once in B's explicit decision
  inventory;
- the compiler derives only repeated syntax listed in the right column above;
- expansion equals the canonical transaction and all nine frozen future
  hashes;
- no opaque plan identifier, prior result, server cache, or second model turn
  contributes information.

If the model emits a visible explanation before the tool call, its elapsed
time remains inside prompt-to-first-call measurement. B cannot improve its
score by moving construction into narration.

## Fail-closed compiler laws

The capture scorer uses the same laws that a later product compiler would need:

1. Validate the closed request shape and all bounds before reading source.
2. Freeze all nine named sources and exact hashes once.
3. Expand A source-blind into canonical local rows.
4. Expand B as a pure function of the request and frozen source map.
5. Refuse duplicate, overlapping, or non-injective expanded rows.
6. Refuse a missing, duplicate, aliased, platform-conditional, or already-added
   require target unless exactly declared and uniquely resolved.
7. Refuse a missing or duplicate owner/from pair or a cardinality mismatch.
8. Never use similarity or choose the nearest owner.
9. Compile through the unchanged validator and transaction compiler.
10. Require the exact 51-match, 9-file future and all frozen hashes.

No capture-only success response can substitute for this offline proof.

## Minimal counterbalanced cohort

Use six fresh, serial Sol/high callers in this palindrome:

```text
F A B B A F
```

Each arm appears twice. Each pair's positions sum to seven, so monotonic seat
warming or cooling has the same mean position for every arm. Every run uses a
fresh Codex home, fresh private capture server, read-only empty workspace, and
the same Anvil seat. There is no cross-run model or server state.

This N=6 screen is the smallest symmetric three-arm cohort with a replicate
for first-call correctness. It is an admission screen, not a publication-grade
performance estimate. Do not add runs selectively. If an admission identity or
client-surface check fails, stop before that model and invalidate the cohort.

If all correctness gates pass but the timing result is within five percentage
points of a promotion threshold, do not tune the prompt or schema. Freeze the
result and decide separately whether a second predeclared six-run block is
worth its model cost.

## Required capture per run

Retain and hash:

- exact product, Faraday prerequisite, protocol, harness, scorer, task, and
  fixture commits or file hashes;
- advertised tool surface and raw Codex `mcpServerStatus/list` receipt;
- exact normalized client-visible tool description, input schema, output
  schema, annotations, and total surface bytes;
- prompt bytes and SHA-256;
- raw event JSONL and observer clock;
- the first tool name and raw argument bytes;
- canonicalized argument bytes and treatment-adherence result;
- decision-coverage receipt;
- expanded canonical request and its SHA-256;
- compiled transaction hash, 51-match/9-file counts, and nine future hashes;
- MCP, shell, file-change, refusal, recovery, and source-read counts;
- prompt-to-first-call, tool-response, response-to-final, and complete wall;
- every failed or invalid attempt. Never replace an ugly run.

## Correctness gates

Every one of the six runs must satisfy all of these:

1. exactly one visible MCP tool and one first `edit_clojure` call;
2. zero shell, file-change, discovery, refusal, recovery, or fallback actions;
3. valid arm-specific schema and treatment adherence;
4. exact decision coverage;
5. exact canonical transaction equality;
6. 51 matches across exactly 9 files;
7. all nine frozen future hashes;
8. capture-only terminal response and no second call.

A denominator containing an incorrect or non-adherent run is forbidden.

## Performance gates

Measure pair midpoints, not the fastest run.

- A earns a product-design step only if its prompt-to-first-call midpoint is at
  least 15% lower than F and complete capture-turn wall does not regress.
- B earns a product-design step only if its prompt-to-first-call midpoint is at
  least 20% lower than F, complete capture-turn wall does not regress, and its
  decision-coverage receipt proves the complexity was not hidden.
- A arguments must be no more than 82% of F's canonical bytes.
- B arguments must be no more than 60% of F's canonical bytes.
- Schema and description growth are charged as client-visible surface bytes;
  payload reduction is never reported without that cost.

The 15% and 20% emission gates are causal-screen thresholds. Before product
integration, a winner still needs a realistic mutation cohort and the standing
20% complete-wall or material correctness/recovery gate.

## Outcome matrix

| Observation | Decision |
|---|---|
| Any arm is not 2/2 first-call exact | NO-GO for that shape |
| A is exact and clears 15% | GO for bounded product design; no integration yet |
| B is exact, clears 20%, and decision coverage is complete | GO for bounded product design; no integration yet |
| B is smaller but does not reduce prompt-to-call | NO-GO; byte compression did not delete model work |
| B needs a prior plan, cache, or second turn | NO-GO; complexity was hidden |
| Candidate chooses flat fields | Semantically score it, but mark treatment non-adherent and NO-GO |
| Surface observer sees any unapproved projection | Invalidate before model launch |
| Faraday rejects the owner relation intrinsically | Block B; A remains independent |
| Faraday accepts exactness but misses speed | B may proceed because require delta is an orthogonal deletion |

## Exact reuse recommendation

Do not write a new controller. After Faraday's immutable gate, make the
smallest experiment-only extension to the accepted capture harness:

- generalize the arm catalog from two to `F`, `A`, and `B`;
- inject the two new exact schemas and descriptions;
- dispatch the existing scorer through the A or B pure expander;
- add the decision-coverage projection;
- change only the order and three-arm aggregate report.

The observer, event clock, registry capture, task fixture, process isolation,
and capture-only handler remain unchanged. Any extension that grows a second
transaction compiler or product handler is a NO-GO.

## Current receipt boundary

This document is the complete deliverable for this turn. It launches no model
and changes no product, harness, installation, runtime, port, or process. The
next action belongs to SURGEON1: accept or reject Faraday's independent gate,
then authorize or reject this capture-only cohort.

Pure protocol checks passed:

- `F A B B A F` contains two runs per arm;
- each arm's positions sum to 7 and have mean position 3.5;
- the frozen task contains none of `file_groups`, `symbol_migration`, or
  `require_change`;
- the request-shape screen still expands both retained candidate payloads to
  the exact canonical edit multiset;
- `git diff --check` is clean.
