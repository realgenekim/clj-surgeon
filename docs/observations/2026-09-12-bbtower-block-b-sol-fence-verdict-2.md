NO-GO

Blocking finding F3 — the probe admits unauthorized non-test targets.

The contract limits probes to a “local test namespace” ([bb-probe-specs.md](/home/forge/src/clj-surgeon-fence/docs/intent/hot-verification/bb-probe-specs.md:26)), but `probe-reload-order` searches `src`, `test`, and `libs/clj-splice/src` without separately authorizing the requested target ([mcp_hot_verify.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_hot_verify.clj:215)).

I ran:

```text
TMPDIR=/var/tmp/forge/sol-fence-09486a6f \
JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/sol-fence-09486a6f' \
clojure -M:clj-surgeon/test-deps -e '…request clj-surgeon.core…'
```

Output:

```clojure
{:request-problem nil,
 :target-source "src/clj_surgeon/core.clj",
 :accepted-reload-count 36,
 :accepted-target clj-surgeon.core}
```

I then drove `probe!` with the valid live-image identity. Output:

```text
Testing clj-surgeon.core
Ran 0 tests containing 0 assertions.
0 failures, 0 errors.
{:state :probe-failed,
 :reloaded [... "clj-surgeon.core"],
 :tests 0,
 :failures 0,
 :proof_pending [:landing-gate]}
```

Thus a production namespace reaches reload and execution, returning `:probe-failed` rather than a typed refusal. The valid image identity prevents a seat-authentication bypass, but there is no target-authorization boundary. The repair must authorize the requested target from the test inventory or test root before traversal, while still allowing its dependencies under `src`/`libs`, and add a typed refusal plus a witness proving a production namespace is never reloaded. This is a new finding, so the delta rules require `NO-GO`, not a reviewer patch.

The requested round-two checks otherwise held:

- Isolated `fold.clj --table-only` replay output `{:namespaces 38, :samples 456, :failed {}}`; generated SHA-256 hashes matched all three sealed runtime artifacts byte-for-byte.
- `bb test/clj_surgeon/bb_ceiling.clj …/receipt.edn` returned exactly `{:bb-runtime-sum-ms 240989, :fast-cadence-sum-ms 42143, :ceiling-ms 343102}`.
- The 14-demotion census returned `:all-in-test-bb? true`, `:runtime-counts {:bb 13, :jvm 1}`, `:cadence-counts {:battery 14}`, `:landing-has-test-bb? true`, and `:missing []`.
- The focused six-witness command covering runtime policy, ceiling/spec agreement, encoded response bounds, the servlet writer, and receipt shape returned `{:test 6, :pass 860, :fail 0, :error 0}`.
- Reading both whole-gate receipts and recomputing the current source digest returned `:state :passed` and identical digest `23390c9b…` for restricted, prewarm, and the sealed source.
- `git rev-parse HEAD` returned `09486a6f0a91dca53455a22237b4332795ba95ff`; `git diff HEAD --numstat` produced no output. `git status --short` still shows only the pre-existing untracked review log.

No `make test` was run.

> END RECEIPT (fence-run): worktree HEAD at review exit = 09486a6f0a91dca53455a22237b4332795ba95ff = fenced sha.
