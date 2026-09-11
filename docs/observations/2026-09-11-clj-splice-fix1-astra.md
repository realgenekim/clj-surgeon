# Astra fix round 1 — clj-splice and port

Recorded 2026-09-11T05:48:33.887686+00:00. Base `4be489fee020a030255a3eb0073c4b5708972dca`;
final tested tip `de6ac88efb422c8f479b77cfc4162b24960b7e72`, branch `fable/clj-splice`.
One fix round; no push, merge, or main change. Prepared for Sol with the gate limitation recorded below.

## Fixes and RED → GREEN evidence

| Item | Regression / mutation and result | Green receipt |
|---|---|---|
| M1 | `fix1-red-1.log`: missing payload row fails the new exact-set assertion; temporarily removing rename `alias-collision` also fails by name. First run 1 failure, removal run 2 failures. Registry restored. | `fix1-green-1.log`: 39/0/0; final shared group 41/0/0 after L1. |
| M2 | `fix1-red-2.log`: the red-team interval-first/point-second example throws `:clj-splice/invalid-interval` where the boundary-point contract requires acceptance (1 test error). Both argument orders, equal points in both orders, interior points and overlapping nonempty intervals are witnessed. | `fix1-green-2.log`, then all three final library logs: 5 tests / 59 assertions, 0 failures/errors. |
| M3 | `fix1-red-3.log`: temporary inclusion of root in the address enumeration shifts every preorder +1; all three strengthened address assertions fail (0→1, 9→10, refusal evidence 9→10). Mutation restored. | `fix1-green-3.log`: 33/0/0; final preservation/address group green. |
| L1 | `fix1-red-4.log`: both disk entrances emit old `:unsupported-source`; both registries misclassify capability exclusions (4 failures). | `fix1-green-4.log`: 41/0/0; final focused suite green. |
| recount | `fix1-red-5.log`: 18 literal witnesses reject the full inventory where structural projection is required. | Final library suite on JVM and both bb versions. |
| source-path corpora | `fix1-red-6.log`: lint of the old library test path reports 12 errors / 14 warnings. | `fix1-lint.log`: 0 errors / 0 warnings, including the entire library. |

M1 derives each exact refusal vocabulary from the planner's remedy `case`, including grouped
case constants. Insert's remedy now names transport failures too; rename has an explicit
case that delegates shared remedies and supplies its own. The insert registry adds
`:payload-parse-error` with a concrete malformed-payload native failure. Final registry
sizes are insert 22 and rename 27 (including the new decoding type).

M2 sorts by `(start, end, argument-index)`. The exact strict overlap formula permits a
point at an interval boundary and refuses a point strictly inside it. Thus both orders of
`[[5 10] "X"]` and `[[5 5] "Y"]` yield `"01234YXabcdef"`. Equal points apply in argument
order. The docstring, README and SPLICE-002 boundaries state the contract.

M3 keeps the literal `events/x` on line 1 and the namespace-header fixture with `events/x`
at column 1 on line 2. It asserts exact `(line, column, end_line, preorder)` tuples
`[1 1 1 0]` and `[2 1 2 9]`, plus preorder 9 in the actual count-refusal evidence.
The `(dec end-row)` adjustment was deleted: consumed alias-reference tokens are single-line,
so none can reach that branch. Root/trivia nodes can end at column 1 on a later row, but
are not consumed reference sites. This is a consumer-reachability claim, not a claim that
no syntax node can ever satisfy the old condition.

L1 retains `:unsupported-source` as `:capability`, `native_failure :none` for BOM,
conditionals, extension and other unsupported source policies. Disk decoding emits
`:malformed-utf8`, class `:semantic`, with the concrete lossy-decoding failure. Both disk
entrances preserve the invalid byte 0xff. Malformed request encoding remains `:invalid-request`.

`recount` now returns only `:nodes`, `:roots`, `:effective-roots`, `:effective-count`, and
`:gaps` from spans. Source and coordinate-index tables are omitted; node coordinates remain.
The twelve frozen corpora moved to `libs/clj-splice/fixtures/*.clj.txt`, outside `src` and
`test` source roots. They remain standalone library data. `fix1-fixture-receipt.txt`
records byte identity of all 12 against both the prior committed library files and the
repository frozen copies. No admission of BOM, conditionals or tabs; mixed-newline policy
remains first-source-newline. The duplicate corpus copies remain; they were compared this
round, not replaced by an ongoing synchronization claim.

## Verification and budget

| Inventory | Tests | Expanded assertions | Result |
|---|---:|---:|---|
| rename_alias | 10 | 211 / 220 | PASS |
| insert_forms | 10 | 171 / 220 | PASS |
| Shared envelope + receipt booleans | 4 | 76 | PASS |
| Library | 5 | 59 | PASS |
| Shared including library | 9 | 135 / 140 | PASS |
| Total | 29 | **517 / 580** | PASS |

Focused receipt: `fix1-focused.log` (all 20 verb groups + four shared groups, 458 assertions).
E4 and existing candidate/receipt/publication witnesses are included. No new or removed
deftest names. The four fewer address assertions now compare exact tuples; two registry
completeness and two classification assertions offset that reduction. Library overlap
witnesses add two table assertions covering eight cases.

- JVM library: `fix1-library-jvm.log`, rewrite-clj 1.2.50, 5/59 PASS.
- bb 1.13.219: `fix1-library-bb.log`, 5/59 PASS.
- bb 1.12.209: `fix1-library-bb-min.log`, 5/59 PASS.
- All three projection hashes: `eb7f6099906935cd68d29b6a7c99b96a4e129fee2ef9b4640f4b70e76260b622`.
- Lint through `~/bin/clj-kondo`: entire `libs/clj-splice` plus changed production/test
  Clojure files, **0 errors / 0 warnings** (`fix1-lint.log`). One existing informational
  redundant-boolean diagnostic in insert_forms.clj is not a warning.
- Census: deliberate direct regeneration wrote 1,690 deftests; read the resulting empty
  diff against the base. **Additions: none. Removals: none.** `fix1-census.log` and
  `fix1-census.diff`. Fixture renames are data moves, not removed test witnesses.

## Gate receipts

1. `make test-fast`: **PASS**, exit 0, real wall 33.50 s. JVM 755 tests / 7,931 assertions,
   0 failures/errors; library prerequisite 5/59. Eight lanes all exit 0; makespan 28,504 ms;
   zero isolation violations across 61 namespaces. `fix1-test-fast.log`.
2. `~/bin/suite-run make landing-gate-prewarm`: **FAILED**, invoked exactly once on final
   tip, exit 2, real wall 136.31 s (`fix1-landing-gate-prewarm.log`). The wrapper inherited
   `BABASHKA_PRELOADS` from the earlier build's JVM guard; that preload reset bb child
   `java.io.tmpdir` to the shared base after the coordinator had assigned a private root.
   All eight bb lanes correctly refused the resulting re-exec sentinel mismatch (exit 97)
   before tests ran. This was my launch-environment error, not a product assertion failure.
   Transaction recovery passed (10,946 ms), alias and MCP suite receipts both say
   `:state :passed`, `:problems []`. The run directory is
   `target/gate-prewarm/8544b1d9-b940-4a3a-8416-1f21cec5316d/`.
   Respecting the one-run limit, I did not rerun the full prewarm and do not claim a passing
   prewarm receipt. The failed bb stage passed separately after removing the override: `python3 jvm-guard.py env -u BABASHKA_PRELOADS /usr/bin/time -p
   ~/bin/suite-run make test-bb` (`fix1-bb-stage-repair.log`). Exit 0, wall 82.04 s,
   **898 tests / 7,957 assertions, 0 failures/errors**, all eight lanes exit 0.
   `make repository-hygiene intent-audit` then passed, exit 0; audit `:ok true`.
   Receipt: `fix1-remaining-stages.log`. Thus every named component has passing
   evidence on the unchanged tip, but there is **no passing full prewarm receipt**.
   Gate proper (`:landing? true`) was not run; no landing authority is claimed.

The gates run sequentially under the user's explicit authorization. Earlier focused JVMs
used the brief's load gate, one at a time, `-J-Xmx1g`, with temp under this fixture root.
The exact Make gate commands own their bounded child JVM configuration. No server was
started for the inner loop. An initial library JVM invocation used the repository cwd
and exited at an empty REPL; it is not counted as a suite run. The real standalone suite
was then run from `libs/clj-splice` and its successful receipt is listed above.

## Small named-file commits

```text
24a30de8 fix: make both refusal registries complete against remedy vocabulary
f7183217 fix: pin column-one reference addresses and remove dead end-line adjustment
ee6b8f9f fix: order batch intervals consistently and specify point insertions
77aa963c fix: distinguish malformed UTF-8 from source capability refusals
154f86af fix: restrict recount and keep frozen corpora off source paths
de6ac88e docs: record clj-splice red-team fixes and verification contract
```

No automatic Sol delegation or external message was sent; this report is the handoff.

Finalized 2026-09-11T05:52:24.398863+00:00. Working tree clean; no mutant remains.

Component receipt summary (`fix1-gate-components.edn`):

```edn
"alias" {:state :passed, :problems [], :result {:test 181, :pass 3566, :fail 0, :error 0}, :wall-ms 93192}
"mcp" {:state :passed, :problems [], :result {:test 923, :pass 11727, :fail 0, :error 0}, :wall-ms 89181}
```
