# Substantiation/W1 cross-feature witness

Date: 2026-08-30  
Status: frozen prep; no product implementation or publication authority  
W1/W2 candidate: `90486da5d4d2d4ed7a6efed443142105a2b41d2d`  
Telemetry candidate: `de70e06fdc18f832b6774eabf81453ad4af9781f`

## Binding contract

After W1 publishes and telemetry rebases, one permanent test must prove this
complete public route:

```text
actual HTTP inspect_clojure
  -> served prepared_confirmation
  -> actual HTTP edit_clojure {confirm, fill}
  -> ordinary committed transaction
  -> exactly one prepared-request consumed feature
  -> exactly one prepared-request committed feature
```

The test must not recognize a raw compact request as consumption, call private
registry functions, construct a Clojure persistent-map substitute for wire
JSON, or infer commit from the confirmation lifecycle.

## Frozen witness

The standalone test is
`test/clj_surgeon/mcp_substantiation_w1_witness_test.clj`, SHA-256
`1449b2112a33bc4418c78b6f4720083da52603f8a5a8231a3d2d15efbf8a9cff`.

It starts the real loopback HTTP server, initializes one SDK session, serves a
descriptor through `inspect_clojure`, submits `{confirm, fill}` through the
official `edit_clojure` JSON boundary, verifies the ordinary source mutation,
replays the consumed digest, and counts the append-only ledger features.

The exact focused command is:

```sh
WITNESS_CP=$(clojure -Spath -M:clj-surgeon/mcp-test)
java -Xms64m -Xmx512m -cp "$WITNESS_CP" clojure.main -e \
  '(require (quote clj-surgeon.mcp-substantiation-w1-witness-test))
   (clojure.test/run-tests
     (quote clj-surgeon.mcp-substantiation-w1-witness-test))'
```

## Expected red progression

At exact W1/W2 candidate `90486da5`, the observed frozen result is:

- 1 test;
- 12 assertions;
- 7 failures;
- 0 errors.

The first four failures are the independently found Java-map public-wire
defect: commit is refused as `invalid-prepared-confirmation`, source remains
unchanged, and replay never reaches the consumed tombstone. The remaining
three failures are zero emitted, consumed, and committed telemetry facts.

After a W1 successor closes the public-wire defect but before telemetry rebases,
the required intermediate result is 1 test, 12 assertions, exactly 3 failures,
and 0 errors: the ordinary commit and replay laws pass, while the three ledger
facts remain absent.

After the real telemetry rebase and integration, the terminal result is 1
test, 12 assertions, 0 failures, and 0 errors. The test then enters the normal
MCP runner and becomes a publication blocker.

Any successor that makes the test green by directly interpreting raw
`confirm` parameters, skipping the official route, accepting a direct
Clojure-map handler probe, or counting more than one consumption is NO-GO.
