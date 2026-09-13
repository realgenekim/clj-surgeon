# Data-not-code round 6 — GO-WITH-OWED

Recorded 2026-09-13T02:07:50Z. Branch `fable/data-not-code-local`; input HEAD `93690642e6b975796943549df7581f78b8d64f31`.
The requested starting tip was `7182573b`; the three later instrument amendments
were already present and are preserved. Delivery is the commit containing this
report, made after verification. No push, merge, tag or install was performed.

The verifier's blocking regression is fixed, and the class-first oracle passes
all **80 impacted namespaces**. The once-only fast gate and the first prewarm
both pass. Prewarm remains `landing? false` by contract; this is a verified local
build with the explicit limits in [owed.md](owed.md), not landing authority.

## Oracle first, then repairs

`bash test/diff-impact 3b6d5357 OUTPUT_DIR before|after` is the committed,
lane-independent entrance. It reads namespace declarations as data, enumerates
direct and one-intermediate dependencies from every changed src file, and runs
all selected namespaces sequentially in fresh JVMs. Nonzero exits, zero tests or
missing counters cannot certify completion. It takes the host suite lock and
refuses an existing JVM lease or receipt-chain process. Namespace-owned child
processes remain part of their tests. Future runs use a fresh output directory.

The complete 20-file mapping, 80-namespace list, paths, counters and logs are in
[impacted.md](impacted.md). Before any production or regression-test patch, all
80 ran once: 79 passed and only `txn-journal-test` was red. All six verifier
witnesses reproduced; the concurrent-breaker witness also failed for the same
hard-link reason. The actual seven errors are retained. After the fixes, all
80 ran once and passed. The excluded 600-file memory-journal witness ran in
both phases; it was not replaced by a smaller fixture or skipped because of lane.

| Check | Executed result | Evidence |
|---|---|---|
| Impact before | 1,660 tests; 24,226 assertions; 0 failures; 7 errors | [before inventory](impact-before.edn), [results](results-before.edn) |
| Journal before → after | 80/496/0/7 → 80/545/0/0 (tests/assertions/fail/errors) | [before](before/clj-surgeon.txn-journal-test.log), [after](after/clj-surgeon.txn-journal-test.log) |
| New regression tests, red | 3 tests; 36 assertions; 20 failures; 2 errors | [red](focused-red.log) |
| Focused green | 65 tests; 1,695 assertions; 0 failures/errors | [green](focused-green.log) |
| Outside-link bypass plant | 1 test; 7 assertions; 6 failures; outside sentinel actually changed | [plant](outside-hardlink-plant-red.log) |
| Outside-link normal admission after plant | Full boundary namespace passes; outside sentinel preserved | [after boundary](after/clj-surgeon.receipt-artifacts-boundary-test.log) |
| Final BB witnesses | 3 tests; 24 assertions; 0 failures/errors | [BB](bb-witnesses-clean.log) |
| Impact after | 1,661 tests; 24,292 assertions; 0 failures/errors; 80/80 namespaces | [after inventory](impact-after.edn), [results](results-after.edn) |
| Lint | 0 errors; 0 warnings, through ~/bin/clj-kondo | [lint](lint-clean.log) |
| make test-fast, once | 1,249 tests; 12,685 assertions; 0 failures/errors, isolation violations or leaks; clj-splice prerequisite adds 5 tests/59 assertions | [log](test-fast.log), [receipt](test-fast-receipt.edn) |
| Prewarm, first attempt | All seven stages exit 0; no problems; zero repair/retry | [log](prewarm.log), [receipt](prewarm-receipt.edn) |

Worker-wall sums are 1,259,133 ms before and 1,256,554 ms after. They include
cold startup and namespace-owned children; they are not a performance comparison.
The fast command took 30074.673 ms; its measured lane
sum is 46,558 ms against the unchanged 60,000 ms ceiling. The prewarm command took
401179.207 ms (receipt gate wall 398,998 ms).

## What changed

**N1.** A link count above one now requires accounting for all links inside the
envelope union. Siblings are examined first for the journal's LOCK protocol;
other roots are searched when necessary. Parent inode plus basename identifies
a distinct directory entry, preventing overlapping roots/aliases from inflating
the count. Traversal does not follow directory or file symlinks. Outside or
unaccounted links retain the required typed refusal and diagnostic link evidence.
Unavailable inode evidence refuses with `:link-evidence :unavailable`.
Ordinary path escapes are checked first and no longer acquire an unrelated
`:reason :hard-link`. Filesystem races remain outside the contract.

**N2.** The manifest derives failed samples from timing receipt results and
opens the registered attempt-8 CLI controls behind the contract escape. It
verifies both actual raw-output/saved-receipt lengths, successful control exits,
operation identity and matching receipt identity: 26,581 versus 2,876 characters
on both runtimes. A row cannot erase or invent contract failure, failed samples,
or portability state to steer the runtime. This backs the existing shared
output-defect escape; it does not claim JVM fixes that separate defect.

**N3.** All receipt reads resolve beneath explicit repository retention roots:
`attempt20/measurements` and the registered `attempt8` controls. A valid receipt
copied to `/var/tmp` is refused by namespace and field. DATACODE-ROWS-001 now
states the bindings and **receipts are evidence, not attestation**. The 38 rows
and 456 timing receipts are unchanged. The old policy witness still requires
its named Var and all 12 assertions. Census regeneration adds exactly three
named regressions and removes none.

**Preregistration.** Both bettors' numeric bounds are unchanged and explicitly
bound the mechanical git-apply→durable-verdict interval; the block-A meter was
also mechanical. The bias from excluding editing is named. The secondary unbet
interval starts immediately before durable publication of the runner's edit
instruction and ends at the same verdict T1. It does not claim to include model
composition. NATIVE has no scored refusal concept; successful planted executions
are `executed-without-refusal`. PROBE must refuse both planted targets with typed
kinds. Eight pure instrument cases pass; no `init`, `cell`, warm image, or timing
experiment was run in this build.

Amendment 1's measurement subject remains `759974c7`. T4/T5 intentionally differ
from this instrument branch. [Current-branch checks](task-byte-checks-current.json)
retain that mismatch; [frozen-subject checks](task-byte-checks-frozen.json) pass all
six patch hashes, before hashes and index-only apply checks. No task bytes were
changed and no patch was applied during these checks.

## Repairs, proof binding and editing method

The first BB check caught `java.io.UncheckedIOException`, a classname absent
from SCI. The portable catch repair passed the same witnesses, including the
unavailable-attribute seam. A combined lint found three duplicate-require
warnings from script namespaces; a dedicated BB witness namespace removed them.
All failed logs remain retained. Neither repair consumed a prewarm retry:
prewarm passed first time.

Fast and prewarm receipts share source digest
`d01ad3839686cd0581d121039dce0cee952a591699c96705da1970dbb671b844`. [source-binding.edn](source-binding.edn) independently
recomputes that value over the current runner-defined input set. Their Git HEAD
fields name the pre-commit parent; the digest binds the verified uncommitted
source snapshot. Reports and this tech-tree entry do not change that input set.

| Surface | Method | Verification |
|---|---|---|
| Artifact admission and runtime evidence | Native reads and native patches at known forms; outside the two automatically routed classes | Red/green witnesses, all impacted namespaces, fast and prewarm |
| Regression tests | Native additions preserving existing witnesses | Named failures, generated census +3/−0, final counts |
| Oracle | Native new script; declarations read as data without loading subject namespaces | Selection/completion self-check, actual before/after execution across all lanes |
| Preregistration/instrument | Native prose/Python changes; frozen tasks preserved | Pure verdict/clock checks and read-only frozen-byte checks; no experiment |

No routed alias migration or namespace split applied, so no routed fallback or
routing admission is claimed. Exact remaining limits, uncovered writers and
review/experiment debt are in [owed.md](owed.md). Machine-readable results are
in [report.edn](report.edn).
