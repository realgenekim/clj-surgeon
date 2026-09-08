NO-GO

# Sol delta fence 2: `fable/battery-parallel` `10b1e6ab`

Reviewed only `119e0c61..10b1e6ab`. The functional patch and the retained
default-path run are acceptable, but the assertion-count correction is not
complete inside this delta. The false count remains in three newly changed
locations, including immediately above the correction itself. Do not land this
tip until those statements agree with the measured contract: 168 tests / 4,325
assertions in both prerequisite modes; only `:skipped` changes.

## Delta identity: confirmed

The range is exactly one commit and six files. Its three code files
(`Makefile`, `battery_parallel_runner.clj`, and `battery_parallel_test.clj`) are
byte-identical to `/var/tmp/forge/battery-fx/sol-fence.patch`: 222 lines,
12,134 bytes, SHA-256
`7e0b701feb6e2e40db7077ac2f864c424e0fba0c2e6894c17ebbec06ce15a999`;
`cmp` exits 0. The only other changes are the expected ledger append, rewritten
namespace-walls receipt, and parity-document correction. No unrelated file is
present.

The Sol patch closes the requested mechanisms:

- `shard-refusal` refuses a namespace resolving `test-ns-hook`, with a witness
  that executes the disagreeing `test-ns` and `test-vars` paths.
- `write-walls!` retains prior fine samples and updates only isolated singleton
  var measurements.
- the Makefile and runner defaults are 8 lanes and prerequisites on.

## Receipt and walls: confirmed

The appended ledger line is exactly the supplied run:
`a3893ba6ee1e482aabda5bbb417d3f7ba0a5e7e5`, started
`2026-09-08T19:23:37Z`, 229 s, pass, 8 lanes, 0 skipped. The parity document
accounts for the merged-tree increase as the added
`split-proof-gate-boundary-test` at 3 tests / 69 assertions: 746 tests / 13,825
assertions across 36 namespaces.

The rewritten walls file contains all seven fine per-var costs and they remain
unequal, ranging from 96 ms to 202,428 ms; the aggregate run did not flatten
them.

## Blocking correction

The same delta says both that prerequisites change the assertion count and that
they do not:

- `Makefile:1043-1044` newly says 4,141 assertions without the receipt and
  4,143 with it.
- `test/clj_surgeon/battery_parallel_runner.clj:136-138` newly repeats that
  claim.
- `docs/observations/2026-09-08-battery-parallel-verdict-parity.md:77-81`
  retains the false mode description immediately before lines 88-104 declare
  it stale and say that only `:skipped` changes.

This is a direct contradiction in the reviewed delta and in the operator-facing
default documentation. Remove or correct all three statements, then a
doc-only delta recheck is sufficient; the accepted implementation and run do
not need to be repeated for this finding.

Per instruction, `make test` was not run.


> END RECEIPT (fence-run): worktree HEAD at review exit = 0fa7f138d6dbb3e50a44d4f87af98edba88c2369 = fenced sha.
