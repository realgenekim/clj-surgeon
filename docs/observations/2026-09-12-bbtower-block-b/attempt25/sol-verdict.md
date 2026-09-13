NO-GO

Blocking finding F1: probe dependency discovery still permits a false green with stale code from a live classpath root.

`clojure -Spath -M:clj-surgeon/test-deps | tr ':' '\n' | rg '/dev/experiments$|^dev/experiments$'` output:

```text
dev/experiments
```

However, [mcp_hot_verify.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_hot_verify.clj:225) searches only `src`, `test`, and `libs/clj-splice/src`.

I created a temporary authorized test namespace depending on a namespace under `dev/experiments`, loaded dependency value `1`, changed its source to value `2`, then invoked `probe!` with a valid image identity. The focused `clojure -Sdeps ... -M:clj-surgeon/test-deps -e ...` reproduction output:

```clojure
Testing demo.probe-test
Ran 1 tests containing 1 assertions.
0 failures, 0 errors.
{:disk-value 2,
 :loaded-value 1,
 :probe-state :probe-passed,
 :reloaded ["demo.probe-test"],
 :closure-expected 1,
 :failures 0}
```

The dependency was silently omitted instead of reloaded or refused. This violates BB-PROBE-003’s complete-local-closure promise in [bb-probe-specs.md](/home/forge/src/clj-surgeon-fence/docs/intent/hot-verification/bb-probe-specs.md:26) and is a capability limit presented as an accepted probe.

Other requested pressure points held:

- `bb .../attempt20/fold.clj --table-only` output `{:namespaces 38, :samples 456, :failed {}}`; hashes remained `FOLD_BYTE_IDENTICAL`.
- `bb test/clj_surgeon/bb_ceiling.clj .../attempt10/ceiling-run/receipt.edn` output `{:bb-runtime-sum-ms 240989, :fast-cadence-sum-ms 42143, :ceiling-ms 343102}`.
- The accounting replay output `109` run records, `109` unique namespaces, no duplicates, sums `240989` and `42143`, with `65` namespaces intentionally present in both dimensions.
- The landing-membership query output `{:moved-to-battery 14, :present-in-landing-test-bb 14, :missing ()}`.
- A fresh four-case CLJC reader check covering unequal counts, both empty-side directions, and ordering output `:all-preserved? true` for both `:clj` and `:cljs`.
- Structural inspection of `start-http-server!`, `probe-servlet`, and `encode-response` showed no additional authorization or writer-boundary blocker: `/probe` is installed only with an image, identity is checked before reload, and encoding completes before `.getWriter`.

`git diff --check` and `git diff HEAD --numstat` produced no output. `git status --short` contains only the pre-existing untracked `docs/observations/bbtower-block-b.md.codex.log`. No patch was made and no `make test` command was run.

> END RECEIPT (fence-run): worktree HEAD at review exit = 6ebffd887673532eadc52e4b95f029477e3c9526 = fenced sha.
