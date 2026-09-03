# Integration branch `bridge/integration-2026-09-03` — composition record

Opened 2026-09-03 22:14 UTC by forge@anvil, at the mayor's request: compose the remaining GO
lanes onto current main, produce ONE branch that is green with all of them
together, hand back one sha.

Base, proven before any edit:

```
git fetch origin main            -> 99394bfee6e72500d24808081ab8f81e43fe31e2
git worktree add /home/forge/src/clj-surgeon-integ -b bridge/integration-2026-09-03 99394bf
git -C /home/forge/src/clj-surgeon-integ rev-parse HEAD
                                 -> 99394bfee6e72500d24808081ab8f81e43fe31e2   (equal)
```

Nothing is pushed from here. Commits only; the seat pushes.

## STEP 0 — the ratchet: the spec-document registry is DERIVED, not listed

**The mayor's finding.** `src/clj_surgeon/mcp_intent_contract.clj` carried the
audited spec documents as a literal vector inside `audit-current-repository`
(7 entries, lines ~102-116). Every lane that added an intent leaf appended a line
to that one vector, so **every lane conflicted with every other lane by
construction** — a merge conflict manufactured by the registry's shape, not by
any disagreement about behaviour.

**The fix.** `spec-doc-paths` scans `docs/intent/<leaf>/<name>-specs.md` at
audit time and returns repo-relative paths sorted lexicographically. A new lane
now adds a FILE and touches no shared line.

Scan discipline, so the derivation stays honest:

- the name pattern is `.+-specs.md` exactly, so the `-specs.from-<source>--*.md`
  provenance variants that several leaves carry are NOT swept in;
- results are sorted, so the concatenated spec text is deterministic;
- an empty scan throws `:no-spec-docs-found` — a moved intent tree must not
  quietly become an empty, trivially-passing audit;
- `excluded-spec-docs` is a map of path -> one-line reason, and an entry naming a
  file that does not exist throws `:orphan-spec-doc-listing`;
- the exclusion map is an ARGUMENT (3-arity), not only a global, so a witness can
  drive the scan against a fixture root without the repository's own exclusions
  following it there.

### What the derivation newly includes, and what it cost

The scan finds 15 spec documents where the vector listed 7. Eight leaves the
vector never mentioned:

| newly scanned leaf | MCP-OP specs it contributes | audit effect |
|---|---|---|
| `2026-08-29-ratification/measurement-evidence-specs.md` | 0 (different ID prefix) | none |
| `2026-08-30-prepared-request-ratification/prepared-request-specs.md` | 9, identical IDs to `prepared-request/` | none (duplicate IDs collapse) |
| `operation-algebra` | 0 (different ID prefix) | none |
| `performance-regression-sentinel` | 0 (different ID prefix) | none |
| `sibling-pair-edit` | 0 | none |
| `worktree-lifecycle` | 0 (different ID prefix) | none |
| **`embedded-elaborator`** | **19 `MCP-OP-ELAB-*`, all active-gap** | **19 violations** |
| **`substantiation-telemetry`** | **19 `MCP-OP-SUBST-*`, all `[x]`** | **38 violations** |

**Both failures are reported here rather than silently dropped**, per the mayor's
instruction. Measured with the exclusion map empty:

```
DERIVED-COUNT 15
OK? false SPECS 225 VIOLATIONS 57
```

- `embedded-elaborator`: 19 `missing-test-witness`. Its red namespace
  `clj-surgeon.mcp-embedded-elaborator-test` is declared frozen-red
  (`docs/intent/embedded-elaborator/frozen-red-declaration.md`, 2026-08-30) and is
  **not in this tree**; `grep -r MCP-OP-ELAB src/ test/` returns nothing.
- `substantiation-telemetry`: 19 `missing-implementation-witness` + 19
  `missing-test-witness`. Its specs are marked `[x]` to record Gene's advance
  ratification ("Go on all!!!", 2026-08-30), not shipped code;
  `grep -r MCP-OP-SUBST src/ test/` returns nothing.

Both are pre-product intent leaves. Neither is a regression this branch caused,
and neither can be repaired tonight without writing the two product surfaces, so
both are excluded **by name, with a reason, and with a re-inclusion trigger**
recorded in `excluded-spec-docs`. With those two excluded:

```
DERIVED-COUNT 13
OK? true SPECS 187 VIOLATIONS 0
```

**187 is exactly the old literal vector's spec count** — deriving the list changed
WHICH FILES are scanned, not WHICH INTENTS are audited.

### Witnesses added (`test/clj_surgeon/mcp_intent_contract_test.clj`)

| witness | proves |
|---|---|
| `a-new-intent-leaf-is-picked-up-by-adding-only-a-file` | a temp `docs/intent/temp-lane/temp-lane-specs.md` is scanned; a sibling `-design.md` and a `-specs.from-docs--x.md` variant are not |
| `an-orphan-spec-doc-listing-fails-loudly` | an exclusion naming a path that does not exist throws `:orphan-spec-doc-listing` and names the path |
| `an-empty-intent-tree-fails-loudly` | an empty intent tree throws `:no-spec-docs-found` instead of auditing nothing |
| `every-spec-doc-exclusion-carries-a-named-reason` | the exclusion set is non-empty only for named reasons: each key exists on disk, each value is a substantive reason string |
| `the-derived-spec-doc-set-matches-the-expected-set-exactly` | the 13 derived paths are asserted literally, so any drift in `docs/intent` is visible |
| `the-derived-audit-covers-exactly-the-old-literal-vector-intents` | derived intent-ID set == the old 7-file vector's intent-ID set, count 187; a diff prints added/removed |

Ran line:

```
suite-run clojure -M:test -e "(require 'clj-surgeon.mcp-intent-contract-test) (clojure.test/run-tests 'clj-surgeon.mcp-intent-contract-test)"
  -> Ran 11 tests containing 24 assertions. 0 failures, 0 errors.

suite-run bb test/run_all.clj
  -> Ran 727 tests containing 6051 assertions. 0 failures, 0 errors.  (1m6s)
```

## STEP 1 — merges

(recorded below as each merge lands)

## Conflict table

| # | merge | file : site | side kept | why |
|---|---|---|---|---|
