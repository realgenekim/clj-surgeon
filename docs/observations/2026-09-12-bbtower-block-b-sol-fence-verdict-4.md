NO-GO

Blocking finding F1: the probe can report green while using stale dependency code.

[probe-reload-order](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_hot_verify.clj:201) does not correctly traverse valid Clojure prefix-list requires. For `(foo [bar :as b])`, it searches for namespace `foo`; when that file is absent, it silently omits the actual dependency `foo.bar`. `probe!` then reloads only the incomplete order.

Reproduction command using a temporary project:

```text
clojure -M:clj-surgeon/test-deps -e '<create foo.bar and demo.probe-test using (:require (foo [bar :as b])); print probe-reload-order>'
```

Output:

```clojure
{:order [demo.probe-test],
 :expected [foo.bar demo.probe-test]}
```

This violates BB-PROBE-003’s promise to “reload its local dependency closure” in [bb-probe-specs.md](/home/forge/src/clj-surgeon-fence/docs/intent/hot-verification/bb-probe-specs.md:26). Because `require target :reload` does not reload an already-loaded dependency, a changed `foo.bar` can remain stale while the target’s tests pass. The request is accepted rather than refused as an unsupported dependency shape, so this is an undeclared capability limit.

Other requested checks held:

- `bb .../attempt20/fold.clj --table-only` output `{:namespaces 38, :samples 456, :failed {}}`; before/after hashes were identical (`FOLD_BYTE_IDENTICAL`). It uses six samples per runtime, sample SD (`n-1`), and the stated two-SD conservative ratio.
- `bb test/clj_surgeon/bb_ceiling.clj <calibration-receipt>` output `{:bb-runtime-sum-ms 240989, :fast-cadence-sum-ms 42143, :ceiling-ms 343102}`.
- The lane-coverage query output `:present-in-landing-test-bb 14, :missing []`; all 14 moved namespaces still run through landing’s `test-bb`, with `intent-transaction-test` using its JVM fallback.
- The focused nine-witness JVM run covering CLJC round trips, accounting, portability, authorization, and HTTP bounding output `{:test 9, :pass 1135, :fail 0, :error 0}`.
- Static inspection showed the `:fast` cadence and `:bb` runtime sums are separately grouped over the same unique run records, never added together.
- Static inspection plus the focused probe tests found no authorization or response-bound bypass: `/probe` is loopback-only, conditionally installed, requires the exact live image identity, and encodes before obtaining the writer.
- `git diff --check` passed. `git diff --numstat HEAD --` was empty; the only status entry remains the pre-existing untracked `docs/observations/bbtower-block-b.md.codex.log`.

No patch was made. Under the stated delta rule, this new contract finding requires `NO-GO`, not an inline fix attempt.

> END RECEIPT (fence-run): worktree HEAD at review exit = e3ffc6a77cd366472425c2ea6a7d268777ec2399 = fenced sha.
