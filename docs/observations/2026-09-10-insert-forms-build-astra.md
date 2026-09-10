# insert_forms v1 build report

Branch: fable/insert-forms. Owned tree: /home/forge/src/clj-surgeon-insert.
Base: bd124492bb557377afe94065ab58149110f21608. Final tip: 33d75cfc. Worktree clean after the final commits.
No push, merge, server operation, or other-worktree mutation was performed.

## Result and verification

Implemented one-boundary/one-file insertion, closed schema, CST planner, exact-byte
splice, guarded staged publication, read-back, durable inverse/outcome receipts,
MCP registration, both CLI request-file spellings and portable inspect projections.
No batch, preview, replacement, formatter or verification profile was added.
The operation never claims behavioral verification and never emits terminal_response.

- RED: 18 literal required deftest names in 18 namespaces committed before implementation.
  Typed :not-implemented stub loaded successfully. Initial logs red.log (17 namespaces)
  and red-boundary.log (CLI namespace): assertion failures, no compilation errors.
- Census: ran the printed CENSUS_REGENERATE=1 entrance and READ the diff: exactly 18
  additions, no removals, 1,683 ledger rows. Each new deftest has its lane/ledger row.
- Final focused run: 18 tests, 457 assertions, zero failures/errors (eighteen-last.log).
- make test-fast: 750 tests, 7,782 assertions, zero failures/errors and zero isolation
  violations across 72 namespaces (test-fast-last.log). Initial fast run exposed test
  publish-monitor leakage; fixture isolation was repaired before the passing run.
- Lint of new implementation and witnesses: zero errors/warnings (insert-lint.log).
- Frozen candidates: load and generated nested test pass (4 assertions); zero lint
  errors/warnings after correcting the artifact filename to match its namespace
  (fixture-load.log, fixture-lint.log). Base-snapshot provenance is stored in the EDN
  fixture, not represented as exact incident-time bytes.
- Intent audit: exit 0; new INSERT-FORMS-001..018 IDs have implementation and witness
  links. Existing repository deferred witness debt was not changed (intent-audit.log).
- landing-gate-prewarm: exit 2 (Make wrapper), invoked exactly once through ~/bin/suite-run
  on c5dec4ca. Failed expectations: CLI op membership (1), HTTP tool catalog/counts (5),
  MCP ordinary-refusal transport (2), and an alias artifact-root spelling assertion (1).
  Catalog/transport tests now assert the new contract. Targeted rerun: 135 tests,
  1,592 assertions, zero failures/errors, plus the artifact override witness (1 test,
  3 assertions) under /var/tmp/forge/insert-fx/clj-surgeon-artifacts.
  See prewarm-repairs.log. Prewarm was NOT rerun; do not represent the final tip as
  a green complete prewarm. Other gate components passed on the earlier snapshot.
  This prewarm is not landing authority and does not bypass frozen main.

## Contract changes and authorized seam decisions

1. ONE PARAGRAPH amendment in the owned contract copy adds preparse lexical-unit /
   reader-prefix depth limits (100,000 / 512) and a pre-materialization 16 MiB candidate
   ceiling. Reason: byte/depth limits alone admitted excessive width and indentation
   amplification. No refusal was weakened. Original records worktree was not edited.
2. The stale linked-intent-dev skill name and missing worktree linked-intent-testing
   copy are repository defects. Used the user-named canonical skill at
   /home/forge/opt/claude-skills/linked-intent-testing/SKILL.md; AGENTS.md unchanged.
3. Dedicated bounded exactly-one-value EDN reader; split reader was not reused.
4. core/-main now maps insertion refusal to exit 2 and I/O/rollback/recovery to exit 1,
   as explicitly authorized by the addendum. Success remains exit 0.
5. invoke! uses an ordinary-result adapter for domain refusals, as authorized.
6. Existing publish-lock cleanup used a runtime .release call unavailable in Babashka;
   enclosing FileChannel close now releases its lock. A FileLock class hint was also
   unavailable in SCI and was removed. JVM/BB insertion and shared fast tests pass.
7. Read-receipt projection is now emitted by completed structural inspect results;
   it carries only the existing captured digest and canonical root/path, no session authority.
8. Durable evidence starts in planned/await-publication state and is finalized with the
   observed outcome/hash. The finalizer reports uncertainty if artifact finalization fails.
9. TWO-SENTENCE contract clarification: required projected inline facts above 3,200
   UTF-8 bytes refuse :limit-exceeded at [:receipt] before publication, reserving the
   rest of 4 KiB for diagnostics/timing. Reason: long legal paths can exceed the inline
   envelope even after moving form ranges to detail. No required facts are dropped.

## Review and resource observations

An independent Sol reviewer executed boundary, encoding, receipt, filesystem and
resource probes. Its findings produced regression variants before the relevant fixes:
request bytes/tags, symbol names, NUL paths, metadata column, Unicode replacement,
long parser diagnostics, output amplification, detailed evidence and durable outcomes.
The reviewer reported valid guard/golden/permission/rollback/foreign-write outcomes.
Its final report turn failed with a service content-filter error twice; do not treat
this as a completed independent final-snapshot approval. The parent retained findings
and rechecked fixes. No new review claim is inferred from test success.

After indexing, the same local 10,000-literal planner probe took 0.435 s (reviewer's
prior snapshot: 10.632 s). A 40,000-root / 80,014-byte source planned successfully in
3.799 s under the capped JVM. These are local single runs, not matched-native evidence
or automatic-routing admission. The 16 MiB candidate cap refuses larger expansion
before materialization. Pure planning shares raw source strings rather than copying
full subtrees into each CST wrapper.

Clean-context caller simulation: PASS (caller-check.md): documented request-file construction, exit 0, exact 44-byte output and matching durable hash. Help points to the full contract; the page was sufficient.

A final legal long-path probe exposed a 5,397-byte committed summary. A RED witness
(5 failures, zero errors) now passes after pre-publication receipt-capacity admission.
The single prewarm had finished before this source change. The final eighteen
namespaces and targeted repairs passed; final make test-fast rerun passed with
750 tests / 7,782 assertions and zero namespace-isolation violations.

## Open risks

- Both production and independent oracle use rewrite-clj. Separate selectors, offset
  calculation, indentation and hashes reduce shared logic, not shared-parser risk.
- The write protocol is cooperative lock plus final recheck, not filesystem CAS against
  arbitrary external writers. Post-check races remain honestly qualified in receipts.
- A process crash after publication but before durable outcome finalization can leave
  planned inverse evidence; recovery must inspect the target digest before replay.
- The final independent review report is unavailable due to the reviewer service error.
- Resource ceilings are conservative engineering bounds, not an empirical claim over
  all admitted maximum-size combinations; final snapshot has no matched-native trial.

## Three choices I am least sure of

1. The 100,000-unit / 16 MiB candidate limits: useful measured guards, but real repository
   size distributions may justify a different admission envelope after more trials.
2. Reusing transaction commit with prewritten inverse evidence and outcome finalization:
   it fits existing primitives, but the crash window deserves a future journal recovery
   exercise before extending this verb's guarantees.
3. Reserving 896 bytes of the receipt envelope for diagnostics/timing is conservative.
   Long legal path requests can now refuse despite being writable; a future lossless
   compact projection might retain more of those requests without weakening the cap.

## Commits (chronological)

- 2e52efc0 Specify insert_forms v1 and add typed RED harness
- dfaef0b1 Add RED witness: insert_forms_after_prefix_named_defn_test
- b6e1298e Add RED witness: insert_forms_anchor_cardinality_test
- 73014b27 Add RED witness: insert_forms_anchor_decoys_test
- c1a551b8 Add RED witness: insert_forms_atomic_test
- d76e546a Add RED witness: insert_forms_body_boundaries_test
- 1b3ad625 Add RED witness: insert_forms_defn_headers_test
- dd82ece4 Add RED witness: insert_forms_deftest_nested_testing_test
- c890bdff Add RED witness: insert_forms_parity_test
- c7019bba Add RED witness: insert_forms_paths_test
- 7c1d5932 Add RED witness: insert_forms_payload_count_test
- cfefd18d Add RED witness: insert_forms_portable_receipt_test
- 7890092f Add RED witness: insert_forms_preservation_test
- 50dd03a0 Add RED witness: insert_forms_reader_safety_test
- 7511dcb5 Add RED witness: insert_forms_receipt_test
- f70798ce Add RED witness: insert_forms_stale_hash_test
- f67df4a8 Add RED witness: insert_forms_terminal_test
- 8aede207 Add RED witness: insert_forms_trivia_test
- 1f20460b Add RED witness: insert_forms_unbalanced_payload_test
- a550fc17 Enroll eighteen insert_forms witnesses in lane and deftest census
- 5bacc2fe Implement pure insert_forms CST planner and receipt facts
- d5b4d102 Release publish locks through channel close on JVM and Babashka
- c482acd2 Publish insert_forms through guarded transaction and durable inverse receipt
- f17f99fa Keep CST spans shared and reindent payload lines in linear order
- b54ea295 Wire insert_forms MCP and both request-file CLI entrances
- fa9cc820 Independently verify insertion placement and all preserved root hashes
- 16bce438 Add RED request-byte and tagged-EDN fence variants
- 49001f8e Add RED NUL-path refusal witness
- 9fa135fb Add RED non-symbol owner-name witness
- 67a6993a Refuse oversized requests, tagged EDN, invalid paths and non-symbol owner names
- 7766755d Isolate insertion fixture artifacts and publish-lock cache
- ec8acbe9 Add RED bounded-summary and durable-detail witnesses
- d589514c Add RED indentation-amplification ceiling witness
- c1023012 Add RED metadata-owned anchor-column witness
- a60f64ea test: reject lossy Unicode payloads before publication
- c83ecf0b test: bound parser diagnostics in insertion receipts
- 07ae15c0 fix: bound insertion resources and index preservation evidence
- 24d9557f fix: keep encoding path and failure handling portable to Babashka
- cd48b352 test: require durable insertion outcomes after commit attempts
- 51edcdd4 test: consume the supporting inspect portable guard projection
- e580ad17 fix: finalize durable insertion receipts with observed outcomes
- 552fb98c feat: project portable insertion guards from completed reads
- b9fd8fc8 fix: bound request collections before MCP normalization
- 0e671357 test: freeze census-derived insertion fixture provenance
- fefe2237 test: reconstruct exact insertion indentation independently
- 01ac7937 test: finish after prefix named defn witness coverage
- 2bffff0b test: finish anchor cardinality witness coverage
- a431b4e1 test: finish anchor decoys witness coverage
- 65d6531a test: finish body boundaries witness coverage
- bdef5411 test: finish defn headers witness coverage
- e7f23557 test: finish deftest nested testing witness coverage
- 54205e20 test: finish parity witness coverage
- 716b4bc7 test: finish paths witness coverage
- 98c14780 test: finish payload count witness coverage
- c9d47738 test: finish preservation witness coverage
- fdd1ea4b test: finish stale hash witness coverage
- 4372176c test: finish terminal witness coverage
- 4588b2f1 test: finish trivia witness coverage
- f389fd8b test: finish unbalanced payload witness coverage
- ae89b2ce style: order portable receipt witness dependencies
- c5dec4ca docs: link verified insertion capability and caller contract
- 2341c0c6 test: include insertion in the complete CLI registry
- 4b1f641b test: include insertion in HTTP catalog synchronization
- 5e3e5078 test: assert ordinary insertion domain refusal transport
- 0e18b9f0 test: refuse insertion when path-bound receipt cannot fit
- 390a2eb3 fix: admit inline receipt capacity before insertion publication
- f2c96660 docs: attach CLI intent markers to their owning verbs
- 33d75cfc docs: record final insertion witness assertion count
