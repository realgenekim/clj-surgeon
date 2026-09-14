GO-WITH-FIX

CLASS: F1 — `passwd-home` fallback edges that either run a later resolver after success or misclassify an uninspectable command path as absence. Oracle: the call-ledger and real uninspectable-path assertions in `passwd-home-fallback-is-lazy-platform-bounded-and-fail-loud`.

The sealed candidate eagerly called `dscl` after successful `getent`. The pre-fix probe returned:

```clojure
{:home "/getent/home",
 :calls [["getent" "passwd" "forge"]
         ["dscl" "." "-read" "/Users/forge" "NFSHomeDirectory"]]}
```

It also returned `nil` for a real inaccessible-PATH failure: `{:present? false, :result nil}`.

The uncommitted repair:

- Stops after successful `getent`.
- Treats inaccessible or non-directory command ancestors as operational failures, preserving the original `IOException`.
- Adds regressions without deleting test coverage.

Verification:

- Focused JVM tests: `{:test 3, :pass 29, :fail 0, :error 0}`.
- Repaired ordering probe: `{:home "/getent/home", :calls [["getent" "passwd" "forge"]]}`.
- Inaccessible-PATH probe: `{:present? true, :result :ioexception}`.
- Cold Babashka load with unusable `PATH`: `:namespace-loaded-without-path`.
- `~/bin/clj-kondo --lint …`: `errors: 0, warnings: 0`.
- `git diff --check`: clean.
- No `make test` was run.

The remaining resolver is lazy: namespace loading does not spawn a process; the lookup happens only on first policy-envelope use. Failure receipts contain only source, status, and exit code—not username, home, stdout, or stderr. `darwin?` still trusts the JVM’s Mac-prefixed `os.name`; spoofed platform properties are outside the supported runtime contract.

<!-- SHIP-FIX-BLOCK
CANDIDATE: 023e3fc92ff7a86a62a603082448e71907fb5020
FINDINGS: F1
REPAIR: Make passwd-home short-circuit after getent success and rethrow operational failures from uninspectable command paths.
PRODUCER: Codex <forge-anvil@anvil> model=gpt-5.6-sol session=01a09da1-3ef9-7072-9f84-16b0f3824bfd
PATCH-MANIFEST:
  src/clj_surgeon/receipt_artifacts.clj +26 -14
  test/clj_surgeon/receipt_artifacts_boundary_test.clj +18 -0
SHIP-FIX-BLOCK -->

> END RECEIPT (fence-run): worktree HEAD at review exit = 023e3fc92ff7a86a62a603082448e71907fb5020 = fenced sha.
