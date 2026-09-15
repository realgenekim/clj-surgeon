GO-WITH-FIX

Finding `IMPACT-EXEC-02`: the parity oracle accepted a scoped observation with `:exit 1` and a child receipt marked `:state :failed, :problems [:tree-changed-during-suite]` whenever the retained namespace verdicts matched. Before the patch:

```text
bb -cp src:test test/diff_impact_executor_parity.clj \
  .../final-full-fast/receipt.edn .../failed-receipt-results.edn

{:parity :passed, ... :delta []}
EXIT=0
```

The uncommitted four-line repair requires a passed control, successful scoped exit, at least one child receipt, and passed/problem-free child receipts.

Pressure-point results:

- `bb -cp src:test -e '<classify-selection/project-selection probe>'` produced:

```text
fast-test       fast-member              fast / fast
other-test      other-lane-member        integration / mcp
renamed-test    unregistered/renamed     refusal
excluded-test   load-excluded            refusal
ineligible-test bb-ineligible            refusal
```

  Renamed, load-excluded, and BB-ineligible refused under both `:fast` and `:all` with `:selected-namespace-unclassified`. The exact renamed-away public call returned exit 1 and `:child-calls []`. The other-lane member intentionally produces `{}` under `:fast` and routes to `{"mcp" [other-test]}` under `:all`; this is classified deferral, not silent loss.

- `bb -cp src:test /var/tmp/forge/impact-fx/exec/round2/sol-probes.clj` returned typed refusals for battery freshness, suite/census gate validation, prewarm entry, landing entry, and landing receipt-chain. Missing selected execution and unexpected execution both produced failed receipts with namespace-census and raw-child-census problems.

- Directly launching the isolation namespace without `-Duser.home` ran 4 tests/19 assertions with 8 failures, exit 1. The parity oracle rejected it with:

```text
[{:namespace clj-surgeon.fast-lane-isolation-test,
  :control true,
  :candidate false}]
```

  `/var/tmp/forge/impact-fx/exec/round2/argv.clj` returned `{:argv-prefix-delta []}`. The repaired positive parity pair returned exit 0 and `:delta []`.

- Fresh replay results:

```text
D2 list: selected 9, invocation 4.41 s, exit 0
D2 fast: 7 fast / 2 other-lane, 7 runs,
         selection 4,199 ms, execution 8,148 ms, total 12,347 ms, exit 0

D3 list: selected 101, invocation 4.23 s, exit 0
D3 fast: 51 fast / 50 other-lane, 51 runs,
         selection 4,238 ms, execution 32,118 ms, total 36,356 ms, exit 1
         expected historic failure: clj-surgeon.lane-manifest-test
```

  The sealed D3 `:all` receipt separately reports 101 runs, 773,462 ms execution, and the three historic failures; its battery and dedicated walls are not blended into the fast wall.

- Base/candidate SHA-256 comparison gave identical hashes for the HOLD/nothing-selected block, both intent-document base prefixes, and the empty allowlist. The selector diff only appends `selected-inventory`.

Post-fix verification:

```text
positive parity: exit 0, :delta []
omitted user.home: exit 1 with isolation namespace delta
failed scoped exit: exit 1 at (zero? (:exit fast))
failed child receipt: exit 1 at passed/problem-free receipt assertion
standard-clojure-style: pass
~/bin/clj-kondo: 0 errors, 0 warnings
git diff --check: clean
```

No `make test` was run.

<!-- SHIP-FIX-BLOCK
CANDIDATE: e8dfa135a1fc25336583c302dfafa65d99047e36
FINDINGS: IMPACT-EXEC-02
REPAIR: Require successful control, scoped execution, and passed problem-free child receipts before parity can pass.
PRODUCER: Sol <sol@openai.com> model=gpt-5.6-sol session=01a0a3e8-35c3-72f3-8916-4210c70dc477
PATCH-MANIFEST:
  test/diff_impact_executor_parity.clj +4 -0
SHIP-FIX-BLOCK -->

> END RECEIPT (fence-run): worktree HEAD at review exit = e8dfa135a1fc25336583c302dfafa65d99047e36 = fenced sha.
