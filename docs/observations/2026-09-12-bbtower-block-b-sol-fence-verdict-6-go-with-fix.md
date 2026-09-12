GO-WITH-FIX

F1: the servlet converted an oversized HTTP request’s registered `:probe-message-too-large` refusal into `:invalid-probe-request`. The pre-fix direct servlet probe output:

```clojure
{:state :probe-refused,
 :error-type :invalid-probe-request,
 :error "Probe message exceeds bound",
 :proof_pending [:landing-gate]}
```

The uncommitted patch preserves typed exception data at the servlet crossing and adds a regression witness.

Verification:

- Focused eleven-var JVM command: `{:test 11, :pass 1152, :fail 0, :error 0}`. This covered CLJC unequal/empty-side round trips, lane accounting, runtime controls, probe authorization/dependency resolution, and HTTP bounds.
- Fold replay: `{:namespaces 38, :samples 456, :failed {}}`; runtime-table hashes remained unchanged, and all 38 shipped assignments matched the folded receipts.
- `bb test/clj_surgeon/bb_ceiling.clj .../ceiling-run/receipt.edn`: `{:bb-runtime-sum-ms 240989, :fast-cadence-sum-ms 42143, :ceiling-ms 343102}`.
- Landing-membership query: `{:requested-moved 14, :present-in-landing-test-bb 14, :missing ()}`.
- Formatter: both changed files formatted successfully.
- `~/bin/clj-kondo --lint ...`: zero errors and warnings.
- `git diff --check` and patch-against-index check: passed.
- No `make test` was run.

The `linked-intent-testing` skill named by `AGENTS.md` was unavailable; the existing BB-PROBE requirements already specify the typed-boundary behavior, so this patch restores that contract without changing intent.

<!-- SHIP-FIX-BLOCK
CANDIDATE: 1251dc7697f2b7ba255330aa9fccb0e4751400a1
FINDINGS: F1
REPAIR: Preserve typed probe request-bound refusals through the HTTP servlet and add a direct writer-boundary regression.
PRODUCER: OpenAI Codex <forge-anvil@anvil> model=gpt-5 session=01a097a9-662c-7393-8378-e08ba68b9b3f
PATCH-MANIFEST:
  src/clj_surgeon/mcp_http_server.clj +2 -1
  test/clj_surgeon/mcp_http_server_test.clj +24 -0
SHIP-FIX-BLOCK -->

> END RECEIPT (fence-run): worktree HEAD at review exit = 1251dc7697f2b7ba255330aa9fccb0e4751400a1 = fenced sha.
