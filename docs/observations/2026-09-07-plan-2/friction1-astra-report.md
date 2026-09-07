# Friction batch 1 — matcher and cardinality

Generated 2026-09-07T21:29:55+00:00.
Branch: `astra/friction-matcher-and-cardinality`; base: `2717629758d67a1bc4d2d4ea5444e98fdeb7449d`.
Commit: `7acf599b721a780019b554e2d15d1700c9a7481b`. All gates passed; worktree clean. No push.
Author: `forge-anvil <forge-anvil@anvil>`. Both requested co-author trailers verified.

## Result

Implemented Gene-approved Recommendation 4, batch 1 (inb-f313b8).
The matcher now compares anonymous-function bodies without losing the original
reader bytes, address, owner, or hashes. A `(mapv f _)` pattern finds `#(mapv f %)`.
Cardinality refusals identify evaluated failing requests in input order, with
expected and actual counts in both structured data and text. Arity guidance is
conditional on source evidence, with a factual count/scope note otherwise.

Evidence read first: `/var/tmp/forge/pair-1/A-T1/request-1.json` through
`response-3.json`; retained pair-1/A-T1 records and the pair result's friction
notes under `/home/forge/src/clj-surgeon-records/docs/observations/`.

## Red → green, by item

| Item | Red evidence | Green evidence | EARS |
|---|---|---|---|
| Anonymous call-body match | `red-matcher.log`: 18 tests / 204 assertions, 30 failures, no errors. The new literal/wildcard call and anonymous-wrapper cases fail; bare symbols and the other reader forms pass. `red-inspect.log` also has four public-result assertions failing for the anonymous call. | `green-matcher-final.log`: 18 tests / 216 assertions, all green, including the literal `#(mapv f %)` witness. Public inspect owner-count/source/hash witness is green in the final inspect suite. | MCP-OP-MATCH-001 |
| Cardinality identity/counts and text | `red-inspect.log`: ten failing assertions in the new three-request cardinality witness (only r2 mismatches, then all three mismatch). The retained field receipt also demonstrates the missing text identity/counts. | `green-inspect-final.log`: 75 tests / 733 assertions, all green. Includes a real public handler callback, only r2 failing, text/data parity, all three failing, zero/excess expectations, and unchanged source bytes. | MCP-OP-MATCH-002 |
| Conditional wildcard guidance | `red-inspect.log`: eighteen failing assertions in six arity-correct/no-candidate/shortfall/scoped cases. The old unconditional longer-pattern note is observed. | Final inspect suite green. All historical longer-pattern controls remain byte-identical and pass; the old sentence is retained only with scoped longer-prefix evidence and no same-head/same-arity candidate. | MCP-OP-MATCH-003; amended MCP-OP-FIELD-003 |

`red-inspect.log` totals 73 tests / 699 assertions, 32 failures and no errors.
Final review found an interaction introduced by collecting multiple cardinality
failures: a later selector failure could issue a continuation omitting the
prior cardinality failure. `red-cardinality-continuation.log` records 28 tests /
339 assertions with four failures. The added direct witness is now green:
evaluation stops at the later non-cardinality error and returns the accumulated
cardinality refusal, without successful partial results or continuation.

## Gates and commands

Every command uses `TMPDIR=/var/tmp/forge/plan2` and
`JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/plan2` where applicable.

| Gate | Result | Receipt |
|---|---|---|
| Affected matcher namespace, `bb` + `clojure.test/run-tests` | Exit 0; 18 tests / 216 assertions | `green-matcher-final.log` |
| Affected inspect namespaces, test-fast dependencies with runner `--ns clj-surgeon.mcp-inspect-contract-test clj-surgeon.mcp-inspect-tool-test` | Exit 0; 75 tests / 733 assertions; zero isolation violations | `green-inspect-final.log`, `.exit` |
| `make mcp-operation-oracle` | Exit 0; oracle pass, legacy counterexamples retained | `oracle.log` |
| `clojure -M:clj-surgeon/test-fast` | Exit 0; 588 tests / 6,134 assertions; zero failures/errors, skipped/failed preconditions, or isolation violations | `test-fast-final.log`, `.exit` |
| `bb test/run_all.clj` | Exit 0; 873 tests / 7,589 assertions; zero failures/errors | `test-bb-final.log`, `.exit` |
| `~/bin/clj-kondo --lint` all seven changed Clojure source/test files | Exit 0; zero errors/warnings (four pre-existing informational findings) | `lint-final.log`, `.exit` |
| Standard Clojure Style v0.29.0 | Exit 0; changed source formatted, new test suffixes formatted independently to preserve old tests | `format.log`, `format-final.log` |
| `make sync-clj-surgeon-skill`; `make check-clj-surgeon-skill-mirrors` | Both exit 0; canonical, Claude mirror and root entrance each 70 lines | `skill-mirrors.log` |
| Existing match-test bytes; `git diff --cached --check` | Pass | `existing-tests-preserved.log` |

The first broad fast run exposed its pinned source-census assertion (1,442 →
1,446, finally 1,447 after the interaction witness). Its arithmetic was updated;
no lane was added or removed. The first lint run exited 2 on a pre-existing
unused `mcp-cold-verify` require. Removed only that namespace require; every
existing match-test body remains byte-identical. Final lint exits 0.

## Receipt and boundary details / doubts

- Baseline bare-symbol matching inside `#()` already worked locally. The failure
  reproduced for the anonymous call pattern; no claim is made that this branch
  repaired an independently reproduced bare-symbol failure.
- `#{}`, quoted lists and syntax-quoted lists already allowed descendant matches;
  they now have explicit regression coverage alongside `#()`, with and without
  `inside`. This is syntax evidence, not a claim that quoted code executes.
  Reader-discard behavior and semantic/macro expansion are not widened.
- An anonymous body hit returns the **whole original `#(...)` node** as source
  and keeps its existing preorder address. It does not invent a body-only
  location. Literal source omission still uses exact byte equality, so these
  body hits retain source even when the pattern itself contains no wildcard.
- The baseline kernel already carried first-failure ID/count scalars, while the
  visible renderer dropped them. The new `cardinality_failures` vector is
  additive; legacy first-failure scalars remain. Later non-cardinality errors
  stop collection; `read_complete` remains false and the receipt makes no claim
  to have evaluated the remaining batch.
- No live transport or service verification was attempted: no forbidden ports
  were contacted, no server was started, and no battery or push ran. Public
  callback behavior was exercised directly. No performance gain is claimed.
- The named `linked-intent-dev` skill is absent, as the repository's existing
  ratification README records. Used the embedded repository LID contract,
  existing design/registry conventions and installed `linked-intent-testing`
  guidance. The approved execution brief authorized the HLD → LLD → EARS →
  tests → code cascade without new phase approvals.

## Diff stat

```text
 .claude/skills/clj-surgeon/SKILL.md                |  2 +-
 docs/high-level-design.md                          |  5 ++
 .../mcp-operation-contract-design.md               | 32 +++++++
 .../mcp-operation-contract-specs.md                |  9 +-
 docs/plans/friction-matcher-and-cardinality.md     | 18 ++++
 docs/tech-tree.md                                  | 13 +++
 skill.md                                           |  2 +-
 skills/clj-surgeon/SKILL.md                        |  2 +-
 src/clj_surgeon/mcp_inspect.clj                    | 98 +++++++++++++++-------
 src/clj_surgeon/mcp_inspect_tool.clj               | 15 +++-
 src/clj_surgeon/structural_lens.clj                | 65 +++++++++++---
 test/clj_surgeon/lane_manifest_test.clj            |  4 +-
 test/clj_surgeon/mcp_inspect_contract_test.clj     | 63 ++++++++++++++
 test/clj_surgeon/mcp_inspect_tool_test.clj         | 71 +++++++++++++++-
 test/clj_surgeon/structural_lens_test.clj          | 33 ++++++++
 15 files changed, 382 insertions(+), 50 deletions(-)
```

Completed 2026-09-07T21:31:00+00:00.
