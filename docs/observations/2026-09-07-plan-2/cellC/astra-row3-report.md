**Feasibility verdict: NOT ADMISSIBLE under Sol's frozen row-3 contract. Do not start F1.**

The standalone require-only capability is implemented and tested on branch
`astra/namespace-split`, commit `3d55fa34f5a7c1d5ff51f22a69e40e9dd7feec0b`. The selected eight-file task cannot
satisfy the frozen byte and layout rules. No amended fixture, successful natural
candidate, cohort, performance estimate, or automatic routing admission is claimed.

Two concrete conflicts block admission:

- In both `capture_archive.clj` and `reducer_session.clj`, golden commit
  `9f9cf614…` puts the target on `(:require`'s opener line:
  `  (:require [marvin-voice-remote.json :as mjson]`. The design's whole-line
  deletion removes the opener. Its literal seed is syntactically invalid.
  Removing only the libspec while retaining the opener is a possible construction
  amendment, but does not meet the frozen whole-line-deletion attestation.
- `codex_app_server.clj` contains only three `clojure.*` entries, ending with
  `   [clojure.string :as str])`. O5 requires the new `marvin-voice-remote.json`
  entry last inside this clause. Putting it after that line places it outside the
  clause; putting it before that line is unsorted. Moving the closer changes an
  inherited byte forbidden by O4. Even repairing the two seed openers cannot
  resolve this independent contradiction.

Sol's whole design was read before work. Its fixture, oracle and gates remain
unchanged. The questions about possible design amendments received no answer;
no exception was inferred. A new design version must explicitly reconcile owned
opener/closer bytes with the eight-new-lines rule before natural admission can
succeed. This is a prerequisite failure, not a D1 observation or a measured loss.

Delivered branch capability:

- MCP `require_change` and CLI `clj-surgeon :op :require-change! :request-file X`
  share an independent closed schema: one target, ordered alias policy, unique
  nonempty explicit `.clj` file vector, exact touched-file/add/remove expectations,
  optional source hashes, and a named synchronous verification profile. No symbol
  request or synthetic symbol edit is involved; the old paired schema is unchanged.
- Each file reuses a target-bound policy alias or chooses the first free alias.
  Exhaustion names the file and bound aliases. Direct single-line libspecs receive
  a sorted line splice preserving inherited bytes, indentation, EOLs and comment
  attachment. Optional removal deletes a complete standalone line. Shared opener
  or closer layouts that cannot meet that ownership contract refuse before write.
- Existing transaction, snapshot guards, synchronous proof and guarded undo are
  reused. Exact count mismatch refuses before publication. Receipts retain actual
  and expected counts, touched files, per-file adds/removes, aliases and reasons,
  collisions, hashes, named checks, and durable undo. Large details use a retained
  receipt file. Failed proof rolls back or reports conflicting-byte recovery.
- HLD, owning design, EARS `REQUIRE-CHANGE-001` through `014`, direct test/code
  annotations, CLI help, README, changelog, lane/catalog census, and an independent
  Python byte/layout grader are committed. The intended Linked Intent Development
  skill was unavailable; the local linked-intent-testing skill supplied the intent
  workflow under the user's complete-build authorization. Repository-required
  clean-context help review was delegated once and passed after documented repairs.

The pure fixture contains all eight exact historical golden headers. Six unit
seed headers delete the target line; the two opener cases use exact parent
`d170f3d5…` headers to exercise valid hanging styles. That explicitly documented
unit fixture is not the frozen full seed. Seven expressible headers pass; the
original eight-header positive remains inadmissible and has a refusal witness.
The independent oracle rejects the three possible natural codex insertion
placements and protected-byte/layout mutants. It does not import Surgeon or
accept its receipt as proof.

One untimed natural hand-drive:

The full history-isolated seed is `/var/tmp/forge/plan2/cellC/row3`, seed commit
`d40b18385f87f3919fe9343032e575d2d2691bff`. It derives from the complete golden tree over parent
`d170f3d5edea6faa39396ea8b3418e29b2e2b4b1` by literal target-line deletion in exactly the eight files. The retained
attestation proves the other Git entries identical, nine `mjson/parse` occurrences,
eight occupied `json` libspecs, zero target libspecs, clean status and no remote.
The invalid openers are recorded, not silently repaired.

The owned warm nREPL at port 41453 invoked the public MCP handler **once** with
`row3-D-request.edn` and `row3-profile.edn`. This was an in-image callback boundary,
not a network transport trial or cohort arm. At 08:48:02.352182475Z the request
refused before write. Independent replay found all eight files unchanged. The
66.348738 ms wrapper wall / 64.621756 ms handler elapsed are retained diagnostic
values only; no complete-wall performance inference is valid. No undo receipt
was generated because no publication occurred. Exact MCP response:

```clojure
{:content ["require_change: refused\n{\"elapsed_ms\":64.621756,\"evidence\":{\"msg\":\"Unmatched delimiter: )\",\"row\":42,\"col\":49},\"verification_complete\":false,\"mutation_attempted\":false,\"state\":\"refused\",\"error_type\":\"require-parse-failed\",\"committed\":false,\"source_unchanged\":true,\"ok\":false,\"error\":\"Unmatched delimiter: ) [at line 42, column 49]\"}"], :isError true, :structuredContent {:elapsed_ms 64.621756, :evidence {:msg "Unmatched delimiter: )", :row 42, :col 49}, :verification_complete false, :mutation_attempted false, :state "refused", :error_type "require-parse-failed", :committed false, :source_unchanged true, :ok false, :error "Unmatched delimiter: ) [at line 42, column 49]"}}
```

The design's independent oracle lines on that unchanged failed candidate are:

```text
O2 GOLDEN: FAIL
O4 ZERO-CHURN: FAIL
O5 REQUIRE-BLOCK PAPERCUTS: 9
```

There are zero changed paths, two missing direct require clauses and six missing
targets; the scope mismatch supplies the ninth finding. These are retained
failures, not accepted output. Because compilation refused, none of the in-call
profile commands ran. The profile contains the exact load, both full Marvin test
commands, serialized golden lint delta, and independent byte/layout grader.
Candidate O1, candidate O3 and both blind O5 reviews remain **unearned**. The
exact golden alone loaded with exit 0; its serialized lint reference exited 3
with 37 errors, 99 warnings and 23 infos. No lint error was waived in a candidate.

Verification receipts:

| Check | Result | Retained evidence |
|---|---|---|
| Fail-first old paired compiler seam | 6 tests / 79 assertions; 52 failures, 0 errors | `row3-red.log` |
| Final warm focused strict capability | 21 tests / 209 assertions; pass | `row3-strict-green3.log` |
| New cold subprocess boundary lane | 12 tests / 89 assertions; pass; no isolation violation | `row3-boundary-cold.log`, `.exit` |
| Final repository `make test` | exit 0; JVM 814 / 10,373 and Babashka 887 / 7,853; zero failures/errors | `row3-make-test3.log`, `.exit` |
| JVM isolation | zero violations across 60 namespaces | same full-gate log |
| Independent Python oracle witnesses | 4 tests; pass, including protected-byte mutants and natural closer contradiction | same full-gate log |
| Help assertions / separate clean-context caller | 58 tests / 454 assertions; reviewer PASS after repairs | `row3-help-test.log`, `row3-help-review.md` |
| New Clojure files, serialized lint | zero errors/warnings | `row3-code-kondo.log` |
| All changed tracked Clojure files, lint delta | added = removed = empty; five pre-existing warnings retained | `row3-code-kondo-baseline-delta.json` |
| Isolated packaged CLI with identical runtime source bytes | preview, source-hash-pinned apply, named proof, exact candidate bytes, CLI undo and exact restored bytes pass | `row3-package-smoke.log`, `row3-package-smoke-result.edn`, package stdout/exit files |

The first two full-gate attempts are retained: attempt 1 exposed census pins,
subprocess lane classification and a temporary receipt leak; attempt 2 passed JVM
checks but failed Babashka's exact help operation set. Those defects were repaired
before the final successful gate. All intermediate red/green/boundary logs remain.
The historical full battery was not rerun; the ordinary gate used its existing
accepted battery evidence, and the newly added boundary namespace ran cold
explicitly. The synthetic CLI smoke is separate from the one natural call.

Freeze and handoff:

`row3-freeze.edn` seals the unchanged design, exact task, request/schema/tool,
profile, implementation commit and runtime source hashes, seed manifest and
attestation, oracle, hand-drive response, verification logs and this report.
It explicitly records `cohort_ready=false` and `cohort_authorized=false`; this is
a blocked evidence freeze, not the admission addendum required before F1.
`row3-D-request.edn` is the exact exercised request with ordered files, source
hashes and no per-file alias answers. Full originals and negative receipts are
retained for diagnosis. No cohort statistics are available or imputed.

Author and committer: `forge-anvil <forge-anvil@anvil>`; trailer:
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`. The worktree is clean. Nothing
was pushed or merged; no shared CLI/MCP installation or routing plate changed.
The optional package is isolated beneath this artifact directory. It records
pre-amend commit `ff3f3a8e`; the final amendment corrects design prose only, and
all runtime source hashes are identical. The code tested by the gate is unchanged.

Work began 2026-09-08 08:11:35 UTC. Report sealed 2026-09-08T09:08:44.607470+00:00, within the 90-minute box.
