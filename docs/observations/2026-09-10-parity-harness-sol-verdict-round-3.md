NO-GO

# Sol fence review, round 3: byte-parity harness `0d58fda2`

Reviewed sealed candidate `9d3fd5255e6d54132ec6be1104347564e15fd992`
(base `13635f748c58767aa37d261ebb98b9e0a03465f5`, branch tip
`0d58fda2460af92f3908e921a7798be2cffe28ca`) on 2026-09-10. The round-2
repairs close PARITY-FENCE-003/004, but a fresh control still produces false
`PARITY`. The required repair touches `bin/`, which the ship protocol classifies
as `HOLD reason=oracle-changed`; no self-authorizing repair was applied.

## Findings

### PARITY-FENCE-005 — CRITICAL — evidence is not bound to the rule it authorizes

`bin/parity/compare.clj:124-165` verifies that the rule cites a retained path
which differs, but never verifies that the rule's selector matches that path or
that replaying this particular rule explains that retained difference. Any real
volatile path can therefore authorize normalization of an unrelated invariant.

Fresh control: I planted a different `:candidate_hash` in B's `stdout.edn` and
`receipt.edn`. With the shipped declaration the comparator returned
`DIVERGENCE 2`, naming `[:candidate_hash]` in both objects. I then added a
`:candidate_hash` rule whose reason claimed wall-clock volatility and whose
evidence cited the genuine retained `[:elapsed_ms]` observation. All new digest
and retained-byte checks passed, and the comparator returned:

```text
PARITY
laundered_rc=0
```

Evidence is retained at `/var/tmp/forge/parity-fence-r3-rEdknt` (`baseline.out`
and `laundered.out`). This also confirms that `:candidate_hash` is not volatile
in the shipped declaration and detects a content change until an unrelated
observation is laundered through a new rule. `:map_hash` and `:snapshot_hash`
are likewise absent from the volatile list; `:receipt_hash` alone is declared
volatile as documented.

Required repair: validate the relationship between each rule and its cited
observation. At minimum, a key rule must cite the same key/path shape. More
generally, replay the individual rule against its retained raw A/B bytes and
prove that it removes the cited difference, without letting an unrelated
difference pay for the permission. Add the exact `:candidate_hash`/`:elapsed_ms`
false-parity control to `bin/parity/self-test`.

### PARITY-FENCE-006 — HIGH — generator provenance is self-consistent, not commit-bound

`bin/parity/compare.clj:131-137` checks the evidence document's generator hash
against the generator beside the comparator. It does not bind that generator to
a named repository commit. `verify-capture` does not inspect a capture's
`:stable-build` or `:generator-sha256` at all. The top-level `:stable-build` is
also not checked. Indeed, stable build `59d8bc0c` does not contain
`bin/parity/observe-volatility.clj`, so it cannot be the generator provenance
authority represented by the current metadata.

Fresh control: in a copied harness I modified the generator, updated the
top-level generator digest, and changed both top-level and capture
`:stable-build` values to `"NOT-A-COMMIT"` plus each capture's generator digest.
The shipped stable pair still returned `PARITY` with exit 0. Evidence is
`/var/tmp/forge/parity-fence-r3-rEdknt/generator-rebound.out`.

Required repair: record the commit which owns the generator and verify the
generator bytes from that commit, then require every capture's stable-build and
generator identity to agree with the authenticated top-level declaration. Add
a modified-generator/regenerated-metadata refusal witness.

### PARITY-FENCE-007 — MEDIUM — the child can resolve the installed launcher

The actual private-server child had the expected isolated build cwd and a
classpath beginning with that build's `src`; it had no open connection to the
running port 7906 during the probe. However, its inherited `PATH` contains
`/home/forge/bin`, and `clj-surgeon` resolves there. Thus the statement that the
isolated execution can never reach `~/bin/clj-surgeon` is not enforced by the
child environment. Evidence is under `/var/tmp/forge/parity-r3-child-env`.

Required repair: launch specimen children with a minimal explicit `PATH` that
contains the required runtimes and utilities but excludes the installed
launcher directory, and add a child-environment assertion.

## Controls and bounded verification

`bin/parity/self-test` passed 15/15. This reran both round-2 controls: a forged
observation with a repaired digest but no retained capture refused naming the
run, and receipts present under `:expects-receipt false` diverged naming the
declaration. The hand-edited evidence, corrupted retained capture, published
receipt path, asymmetric absence, and undeclared-specimen witnesses also passed.

The occupied-port control passed: with a process listening on 7951 and both
harness slots pointed there, `bin/parity-run` refused A and B before starting a
server and ended with a non-parity `REFUSED` result (exit 2). Evidence is
`/var/tmp/forge/parity-r3-port-guard/probe.out`. Default ports are 7951/7952;
`bin/parity-run:213` rejects 7888, 7890, 7894, 7895, and the full 8300–8339
range before launch.

The sealed delta `4b594926..0d58fda2` changes exactly the 21 reported parity
harness/evidence files. `git diff --check`, shell syntax checks, Python
compilation of `mcp-call.py`, and EDN reads of all three declarations passed.
Per the brief, `make test` was not run.


> END RECEIPT (fence-run): worktree HEAD at review exit = 9d3fd5255e6d54132ec6be1104347564e15fd992 = fenced sha.
