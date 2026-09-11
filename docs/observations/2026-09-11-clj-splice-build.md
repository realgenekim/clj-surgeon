# Astra clj-splice build report

Recorded 2026-09-11T05:17:53.891771+00:00 in `/home/forge/src/clj-surgeon-splice`, branch `fable/clj-splice`, base `04648059`. **Implementation and permitted verification are complete. The requested normal fast/prewarm gates remain unrun because they conflict with the hard one-JVM/no-server fence. This is not a landing or shipping approval.** Nothing was pushed; AGENTS.md was not edited.

## Result

`libs/clj-splice/` has independent deps.edn, bb.edn, linked intents and tests. `clj-splice.core` exposes exactly `spans`, `splice`, and `recount`. Inputs are validated Unicode Strings; intervals are exclusive UTF-8 byte offsets over original text. The snapshot contains physical/effective roots, N+1 gaps, nested node IDs/parents/children, original coordinates, explicit synthetic provenance and literal facts. Only physical roots are hashed. Owner/body/alias policy remains in Surgeon. ExceptionInfo carries a namespaced category; no custom class was added.

Single and batched interval splices validate boundaries, bounds and nonoverlap. The batch is an additional arity of splice, assembled once against one snapshot. The UTF-16-to-UTF-8 map handles scalar pairs and rewrite-clj newline normalization. The executed Unicode witness remains bytes [9,18) with SHA f5c134bf6c2d0b925524242efe4dd5564dbec67f832b7e484f0a4bd1963c8939. Recount shares the parser and projection.

Rename now uses the inventory through a Surgeon projection, removing the annotate→addresses duplicate parse. The existing physical-form ordinals, trivia-skipping preorder, line evidence, byte offsets, role/binding/capture and candidate/inverse guards remain witnessed. Insertion uses nested and literal spans while retaining header/body/layout choices and its parse-valid wrong-offset mutant. Mixed LF/CRLF uses the first source newline for added separators and preserves all old bytes, including CRLF inside strings.

Both verbs have ten witness groups. Final totals: rename 215, insertion 171, shared/library 129: **515 expanded assertions**, below every allocation. The full fast inventory passes through the direct serial entrance: 755 tests, 7,931 assertions, zero isolation violations. The independent library passes on the JVM and both bb pins with identical projection hash. The 4,000-line planner bound passes at 1.585 seconds. Current intent violations are empty; serialized lint has zero errors/warnings.

## Commits

```
2b533932 docs: freeze clj-splice byte and compatibility contract
531eff28 feat: add pure clj-splice span splice and recount library
dc0286ea refactor: plan alias renames from clj-splice inventory
2e968df9 refactor: insert forms using literal spans and preserve mixed newlines
3a8f732f perf: assemble alias intervals once and pin both bb runtimes
6ac26ac9 test: consolidate splice verbs into twenty witnessed groups
65e42b5a docs: distinguish encoding hazards in legacy source refusal type
82e7aad9 test: point operation catalog at consolidated publication witnesses
707fad86 docs: record splice counts timings and execution limits
```

The report's own commit adds only this record. The tested code/catalog snapshot is `82e7aad9`; subsequent commits record measurements and this report.

## Deletions and retained boundaries

Deleted the old tree-wrapping implementation and duplicate address parse from active planning, the English-regex remedy test, superseded standalone test definitions, repeated transport grammar matrices and repeated per-field boolean scenario executions. Alias token synonyms were reduced by equivalence class. Accept/refuse helpers now assert complete outcome tuples, retaining exact expected text, state and independent oracles. The cut in assertion count is not a claim that all those assertions represented separate hazards: planner executions changed much less than CLI launches.

The named old→new map was written before deletions at `/var/tmp/forge/splice-fx/witness-map.md` and is committed as `docs/plans/clj-splice-witness-map.md`. Removed names remain as testing labels inside their destination groups. Unique role, structure, inverse, wrong-offset, disk-hash, external-write and recovery witnesses remain. The operation witness catalog and derived deftest census were updated to the new groups; the full fast run caught two stale catalog references, now repaired.

**Could not delete lexical!/shape-limit!:** rewrite-clj 1.2.50 has no demonstrated parser-level depth/node/work budget. The scanner still protects the recursive parser and the EDN request entrance. Its literal ranges no longer drive insertion indentation; spans does. A postwalk cap or caught stack overflow would not satisfy the deletion gate. No additional grammar scanner or parser-internal global redefinition was introduced.

BOM remains a library token and is refused by both verbs. Measured tab indentation, bare-CR verb admission and branch-dependent editing remain deferred. The three-function fence is unchanged. No upstream issue was filed.

Each verb's intent directory now has `refusals.edn` with native_failure, promise, native_method, minimal_reproducer, class, existing_check, owner, witness and retirement_condition for its refusal types. Explicit none is limited to protocol/resource/capability classes. The legacy unsupported-source type is classified with its real malformed-UTF-8 hazard and documents the capability-only variants sharing the type. Committed receipt boolean discovery is explicitly narrower than all failure-only receipt fields; publication/recovery groups cover those failure variants.

## Unfinished gate contract, recorded without stopping the round

The normal `make test-fast` target retains a JVM coordinator while launching JVM children. The requested prewarm additionally runs integration services. Executing them as configured would violate ONE JVM and no servers. I did not alter the coordinator to manufacture a landing receipt or silently run prohibited services. Normal make test-fast and `~/bin/suite-run make landing-gate-prewarm` were therefore not executed; prewarm invocation count is zero, not the requested one. The complete fast namespace inventory was instead run with `clojure -J-Xmx1g -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner fast`; its explicit SERIAL/NOT-A-GATE result is retained. The newly wired library Make prerequisite was separately executed and passed. A normal prewarm still requires an execution context/authorization compatible with its process and service requirements.

There was one execution mistake: an observational uptime check allowed a JVM command at load 20.75. It exited on missing lane registrations before tests ran and is excluded from measurements. The corrected guard serializes owned JVM launches, checks uptime and waits a full minute above load 14. Its subsequent waits and admitted loads are retained in the logs. See the measurement record for exact details.

## Three least-sure choices

1. **Retained resource preflight.** It preserves the witnessed operational bound but keeps duplicated lexical work. Removing it responsibly requires a bounded parser entrance on both runtimes, outside this block.
2. **First-newline insertion policy.** Exact old-byte preservation is witnessed, including mixed endings and multiline literals; the chosen style for newly inserted separators is deterministic rather than a claim about each user's preferred layout.
3. **Legacy refusal classification.** unsupported-source bundles actual decoding hazards with deliberate capability exclusions. The registry makes that distinction visible without changing public error vocabulary, but a future contract may benefit from distinct subcategories. Quoted-data renaming and macro-hidden alias mutation retain their existing syntactic limits.

## Measurement table and evidence

# clj-splice measurements

Recorded 2026-09-11T05:16:20.945825+00:00. Base: `04648059`; tested implementation/catalog tip: `82e7aad9`. Branch `fable/clj-splice`. No push.

## Executed assertion budget

| Inventory | Before deftests / expanded assertions | After deftests / expanded assertions | Ceiling |
|---|---:|---:|---:|
| rename_alias | 24 / 556 | 10 / 215 | 220 assertions |
| insert_forms | 24 / 548 | 10 / 171 | 220 assertions |
| Shared envelope + committed-receipt booleans | 2 / 45 | 4 / 72 | shared allocation |
| Standalone library | absent | 5 / 57 | shared allocation |
| Shared including library | 2 / 45 | 9 / 129 | 140 assertions |
| Total | 50 / 1,149 | 29 / 515 | 580 assertions |

The retained 4,000-line performance witness is separate: one deftest/two assertions, 1.585418547 seconds of planning, PASS against five seconds. It is outside both semantic totals. The 773 lane/intent audit assertions are existing repository-wide infrastructure and are also outside this relocated semantic burden.

## Cold wall and results

| Lane | Before | After | Result / receipt |
|---|---:|---:|---|
| Focused JVM, same instrumented runner | 33.54 s | 17.10 s | PASS; 50/1,149 → 24/458 without library |
| Library + corresponding pure planner groups, JVM | 7.60 s | 8.54 s | PASS; 20/412 → 13/206 |
| Same pure groups, bb 1.13.219 | 7.17 s | 7.18 s | PASS; 20/412 → 13/206 |
| Pure groups, minimum bb 1.12.209 | not measured before | 29.63 s | PASS; 13/206 |
| Final standalone library, JVM / rewrite-clj 1.2.50 | new library | 2.69 s | PASS; 5/57 |
| Final library wired Make target, bb 1.13.219 / rewrite-clj 1.2.55 | new library | 3.54 s | PASS; 5/57 |
| Library, minimum bb 1.12.209 / rewrite-clj 1.2.50 | new library | untimed independent check | PASS; 5/57 |
| Full fast inventory, direct single-JVM diagnostic | not measured | 52.00 s | PASS; 755 tests / 7,931 assertions; 61 namespaces; zero isolation violations |
| Normal `make test-fast` coordinator | NOT RUN | NOT RUN | Conflicts with one-JVM fence; direct full-inventory result above is not its gate receipt |
| `~/bin/suite-run make landing-gate-prewarm` | n/a | NOT RUN | Coordinator plus children and integration servers conflict with hard execution fence |

The library's Make prerequisite itself was executed as `make test-clj-splice-bb`; the normal multiprocess fast coordinator was not executed. The direct fast runner explicitly prints `SERIAL/NOT-A-GATE`.

The initial untouched isolated focus passed 50/1,149 in 37.91 s. A preliminary consolidated isolated run passed 24/469 in 19.04 s; subsequent consolidation changed that inventory, so it is not the final count. Another final instrumented run passed 24/458 in 17.58 s. These are observations under changing contention, not a six-run variance floor, native comparison, routing admission, or controlled speedup. The pure-planner wall did not improve.

## Fixture and process accounting

The same metering wrappers counted actual public planner calls, filesystem-fixture executions, and `clojure.java.shell/sh` calls. These are execution counts, not asserted counts of unique hazards; planner calls inside separate bb children are not included in the parent planner columns.

| Observed executions | Before | After |
|---|---:|---:|
| Parent insertion planner calls | 112 | 109 |
| Parent rename planner calls | 123 | 101 |
| Filesystem fixtures | 76 | 60 |
| bb CLI children | 50 | 8 |
| findmnt children | 133 | 92 |
| All observed suite child processes | 183 | 100 |

Each measured JVM lane used one suite JVM. Clojure CLI classpath/bootstrap startup is inside cold wall but its separate bootstrap process count was not instrumented. No simultaneous suite JVMs were run. Boolean scenarios execute once per verb: three insertion scenarios and five rename scenarios; the completed-write observation also supplies behavior-not-run.

Before metering used immutable `04648059` planner/test overlays under `baseline/`, including original boolean tests. Before CLI children received the same baseline classpath explicitly. Unchanged transport/runtime dependencies came from the owned checkout. After uses the committed implementation. Pure groups correspond through the witness map; grouping, redundant token spellings and newly admitted newline cases change row counts. There is no comparison of a bb subset against the complete JVM suite.

## Exact-byte and mutation evidence

- E4 rename: `02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c`; lines 153/1112 and preorders 537/4661 remain unchanged.
- E4 insertion: remove only normalized-speaker-name and its following blank gap from the frozen E4 output, then reinsert after ns; exact same complete-file SHA above. Four E4 source/anchor/count/parse refusals are retained in that group.
- All three parser/runtime combinations emit projection SHA `eb7f6099906935cd68d29b6a7c99b96a4e129fee2ef9b4640f4b70e76260b622` over the literal witnesses and both complete before/after Opus corpora.
- Additional direct zipper compatibility probe passes on leading trivia + nested discards, quote/metadata, synthetic qualifier, Unicode, and empty source (`preorder.log`).
- Actual committed-receipt class test deliberately fails three times under a temporary `unregistered_splice_probe` catalog entry and `unregistered_splice_field`: missing registry, missing scenario, missing field. Scoped redefinitions are restored; no mutant remains. The normal test is green (`final-verify.log`).
- Final lint: zero errors/warnings through `~/bin/clj-kondo`. Lane audit: 27/213 PASS; intent audit tests: 22/560 PASS; current intent violations: empty.

## Execution caveat and retained logs

At 04:42:33 UTC one command printed load 20.75 and nevertheless launched because uptime was observational rather than conditional. It exited 96 on missing new lane registrations before tests ran; it was already gone when the owned-process check ran. It is excluded from measurements. Subsequent launches use `jvm-guard.py`: an owned file lock, uptime, and a 60-second wait above load 14. All JVM commands use `-J-Xmx1g` and JAVA_TOOL_OPTIONS tempdir under this directory; bb commands set java.io.tmpdir explicitly, with BABASHKA_PRELOADS also set by the final guard for child bb processes.

Raw receipts: `baseline-focused.log`, `focused-before-meter.log`, `focused-after-meter.log`, their `.time` files, `pure-before-*-matched.*`, `pure-after-jvm.*`, `pure-after-bb-final.*`, `pure-after-bb-min.*`, `library-*-final.*`, `library-bb-min.log`, `fast-serial-final.*`, `planner-performance.log`, `final-verify.log`, `lint.log`. Scripts and the immutable baseline overlay remain beside these receipts for review.

## Named witness map

# Witness map

| Old witness | New group |
|---|---|
| `rename-alias-e4-verbatim` | `clj-surgeon.rename-alias-test/rename-alias-e4-verbatim` |
| `rename-alias-prefix-trap` | `clj-surgeon.rename-alias-test/rename-alias-role-selection` |
| `rename-alias-reader-roles` | `clj-surgeon.rename-alias-test/rename-alias-role-selection` |
| `rename-alias-string-decoy` | `clj-surgeon.rename-alias-test/rename-alias-role-selection` |
| `rename-alias-discard-decoy` | `clj-surgeon.rename-alias-test/rename-alias-role-selection` |
| `rename-alias-binding-matrix` | `clj-surgeon.rename-alias-test/rename-alias-binding-matrix` |
| `repository-ignores-non-clojure-symlinks` | `clj-surgeon.rename-alias-test/rename-alias-scope` |
| `comment-ancestry-exempts-mutations-but-keeps-references` | `clj-surgeon.rename-alias-test/rename-alias-scope` |
| `skipped-namespace-mutations-do-not-refuse` | `clj-surgeon.rename-alias-test/rename-alias-scope` |
| `skipped-duplicate-bindings-do-not-refuse` | `clj-surgeon.rename-alias-test/rename-alias-scope` |
| `rename-alias-scope-guards-counts` | `clj-surgeon.rename-alias-test/rename-alias-stale-and-counts` |
| `rename-alias-source-boundaries` | `clj-surgeon.rename-alias-test/rename-alias-parse` |
| `candidate-parse-refuses` | `clj-surgeon.rename-alias-test/rename-alias-parse` |
| `candidate-role-recount-refuses` | `clj-surgeon.rename-alias-test/rename-alias-candidate-integrity` |
| `candidate-form-preservation-refuses` | `clj-surgeon.rename-alias-test/rename-alias-candidate-integrity` |
| `candidate-inverse-identity-refuses` | `clj-surgeon.rename-alias-test/rename-alias-candidate-integrity` |
| `rename-alias-receipt-oracle` | `clj-surgeon.rename-alias-test/rename-alias-preservation-address` |
| `column-one-reference-addresses` | `clj-surgeon.rename-alias-test/rename-alias-preservation-address` |
| `rename-alias-transaction-faults` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `replacement-read-back-refuses` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `receipt-observes-disk-neighbor-corruption` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `receipt-observes-disk-trivia-and-discard-corruption` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `receipt-details-contains-observes-artifact` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `insert-forms-after-prefix-named-defn` | `clj-surgeon.insert-forms-test/insert-forms-exact-root-anchor` |
| `insert-forms-comment-string-anchor-decoys` | `clj-surgeon.insert-forms-test/insert-forms-exact-root-anchor` |
| `insert-forms-anchor-cardinality` | `clj-surgeon.insert-forms-test/insert-forms-exact-root-anchor` |
| `insert-forms-candidate-envelope` | `clj-surgeon.insert-forms-test/insert-forms-exact-root-anchor` |
| `insert-forms-deftest-nested-testing` | `clj-surgeon.insert-forms-test/insert-forms-body-anchor` |
| `insert-forms-body-boundaries` | `clj-surgeon.insert-forms-test/insert-forms-body-anchor` |
| `insert-forms-defn-headers-and-arities` | `clj-surgeon.insert-forms-test/insert-forms-body-anchor` |
| `insert-forms-payload-count-and-order` | `clj-surgeon.insert-forms-test/insert-forms-payload-count-and-order` |
| `insert-forms-trivia-and-indentation` | `clj-surgeon.insert-forms-test/insert-forms-layout-literal-preservation` |
| `insert-forms-existing-separators` | `clj-surgeon.insert-forms-test/insert-forms-layout-literal-preservation` |
| `insert-forms-unbalanced-payload-refuses` | `clj-surgeon.insert-forms-test/insert-forms-parse-stages` |
| `insert-forms-parse-errors-refuse` | `clj-surgeon.insert-forms-test/insert-forms-parse-stages` |
| `insert-forms-candidate-structure-refuses` | `clj-surgeon.insert-forms-test/insert-forms-candidate-structure-refuses` |
| `insert-forms-other-top-level-hashes` | `clj-surgeon.insert-forms-test/insert-forms-other-top-level-hashes` |
| `insert-forms-stale-hash-refuses` | `clj-surgeon.insert-forms-receipt-test/insert-forms-snapshot-guard` |
| `insert-forms-portable-read-receipt` | `clj-surgeon.insert-forms-receipt-test/insert-forms-snapshot-guard` |
| `insert-forms-atomic-multiform-and-race` | `clj-surgeon.insert-forms-receipt-test/insert-forms-publication-evidence` |
| `insert-forms-planned-receipt-recovery` | `clj-surgeon.insert-forms-receipt-test/insert-forms-publication-evidence` |
| `insert-forms-receipt-accounting` | `clj-surgeon.insert-forms-receipt-test/insert-forms-publication-evidence` |
| `insert-forms-terminal-response-eligibility` | `clj-surgeon.insert-forms-receipt-test/insert-forms-publication-evidence` |
| `insert-forms-reader-safety-and-limits` | `clj-surgeon.splice-envelope-test/bounded-input-path-encoding` |
| `insert-forms-path-confinement` | `clj-surgeon.splice-envelope-test/bounded-input-path-encoding` |
| `insert-forms-refusal-remedies` | deleted: English regex; actionable state remains in anchor/stale groups |
| both `*-cli-mcp-parity` | retained entrance group, reduced dispatch and representative boundaries |
| `four-thousand-line-planning-under-five-seconds` | retained separately budgeted performance measurement |

No guard mutant is deleted. Grouping retains old names as testing labels. Shared boundary tests retain resource/path/encoding scenarios.

Execution reduction: each accept/refuse fixture now compares its complete outcome tuple in one assertion (exact bytes, role count and independent oracle still checked). Both entrances run two success spellings, one MCP success with disk detail hash, one schema refusal through CLI/MCP, and one I/O exit. Full grammar runs once in the shared envelope; rollback/foreign-byte outcomes remain in publication groups. False scenarios are memoized within each verb's class test only. No globally cached receipts survive a test.

Further named row moves: rename-alias-source-boundaries' repeated path synonyms, 513-depth/8MiB resource rows, symlink/hardlink checks and malformed UTF-8 disk case → bounded-input-path-encoding. One rename scope-path adapter case stays. The shared UTF-8 case now drives both disk entrances against the same invalid byte fixture. Malformed alias synonyms collapse to seven token equivalence classes in binding-matrix. Repeated EDN reader rows → exactly-one-edn-request. E4 insertion reconstruction plus four source/anchor/count/parse refusals were added to exact-root-anchor; the expected output remains the frozen 02332a74 file.

## Upstream issue text — ready to file

# Parsed synthetic qualifier nodes lack source positions; support original-source spans across normalized newlines

Tested rewrite-clj 1.2.50 on Clojure 1.12.1 and babashka 1.12.209, and bundled rewrite-clj 1.2.55 on babashka 1.13.219. This is an issue draft; no issue has been filed and no maintainer response is assumed.

```clojure
(require '[rewrite-clj.parser :as p] '[rewrite-clj.node :as n])
(doseq [source ["#::ev{}" "#:é{}" "#?(:clj 1)" "#?@(:clj [1])"
                "\"é😀\" (def x 1)" "\"a\r\nb\" (def x 1)"]]
  (doseq [node (tree-seq n/inner? n/children (p/parse-string-all source))]
    (prn [(n/tag node) (n/string node) (meta node)])))
```

Observed: `#::ev{}` contains a `:map-qualifier` whose rendering is `::ev` and metadata is nil. `#:é{}` has the same coordinate gap. Synthetic `?`/`?@` token children of reader conditionals also have nil metadata. Ordinary nodes have exclusive row/column endpoints. Their columns count UTF-16 code units on these runtimes: in `"é😀" (def x 1)`, the list starts at row 1, column 7, UTF-16 index 6, UTF-8 byte 9. The list occupies bytes [9,18).

Separately, node rendering normalizes CRLF, including inside multiline strings: parsing `"a\r\nb"` yields node/string containing `"a\nb"`. Therefore rendering length cannot be used to infer original source intervals. LF, CRLF, bare CR and CRFF need an explicit coordinate convention when recovering original offsets.

Could synthetic prefix nodes first receive source metadata with exclusive coordinates, consistent with neighboring parsed nodes? For `#::ev{}`, the qualifier should occupy row 1 columns [2,6), original UTF-8 bytes [1,5). For `#:é{}`, columns [2,4), bytes [1,4). Conditional prefix tokens should be similarly locatable without implying platform branch semantics.

Would a separate opt-in original-source offset facility be appropriate, with documented UTF-16 column and newline-normalization behavior? Compatibility matters: existing node/string normalization and zipper tracked positions need not change. Parser-created metadata describes the original snapshot, whereas tracked zipper positions can follow edits. An offset API should distinguish those contracts and specify malformed Unicode handling.

I can offer small synthetic-coordinate and Unicode/newline regression tests and a narrowly scoped patch. The proposed scope excludes hashes, owner classification, form ordinals, receipt schemas, alias policies and transaction semantics; those belong to consumers. This is not a request to upstream a general application inventory or rename zipper z/splice.
