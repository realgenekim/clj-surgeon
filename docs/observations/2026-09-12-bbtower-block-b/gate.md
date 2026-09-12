# Gate: owed

No landing-gate-prewarm attempt was launched in this attempt. The frozen
step-one contract requires a pre-existing bb lane ceiling that the named base
does not contain. Neither permitted prewarm attempt has been consumed.

`make test-fast`, `make test-integration`, `make landing-gate-prewarm`, and
`make test-battery` remain owed to Astra after Fable resolves the contract and
the execution is eligible under the single-JVM, no-server manifest. No gate
argv, budget, runtime classification, test membership, or clock was changed.

The only execution was a zero-JVM, read-only fold of retained Block A walls:

```sh
bb -Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx docs/observations/2026-09-12-bbtower-block-b/replay.clj
```

It exited zero because the diagnostic completed; it reported a failing lane,
not a successful test suite. [Replay transcript](replay.log), verbatim budget line:

```text
   TEST-ISO-007 VIOLATION in :fast -- lane time budget: the fast lane took 68210 ms, over its 60000 ms budget
```

There are no new refusal-census lines: that check was not executed. No proposed
stable tag is issued. Fable retains landing authority; Gene retains the named
protected-action authorities.
