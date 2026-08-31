# Embedded Spark elaborator production verification

Date: 2026-08-31 UTC

Branch: `feature/embedded-elaborator-20260830`

This receipt covers implementation and pre-install verification of
`MCP-OP-ELAB-001..019`. It does not install, reload, or publish a shared
runtime. Installation remains reserved for Gene.

## Frozen red

The declaration and its direct witnesses were committed and pushed before
product code at `2145b7536827f4a1af0b8c20a20f46773b4bec20`.

```text
Ran 19 tests containing 367 assertions.
324 failures, 0 errors.
```

The frozen set includes hostile-output rejection, boot-failure server health,
exact model pinning with no fallback, one-turn/no-replay, process-group cleanup,
consume-once, the one-hole wall classifier boundaries, and the `b0432c25`
identity prohibition.

## Isolation conformance

The supervisor conforms to the binding H-S strongest-off receipt at
`3c2cc192` and identifies the policy as `spark-hs-v1@3c2cc192`:

- one direct, policy-owned stdio app-server child in its own process group;
- private JSONL owned and parsed by the supervisor, never exposed to MCP
  callers;
- exact `gpt-5.3-codex-spark` at catalog admission, thread start, thread echo,
  and turn start, with provider fallback false and reroutes rejected;
- a mode-0700 temporary service root, mode-0600 managed auth copy, empty
  read-only non-ancestor workspace, explicit allowlisted environment, and no
  repository mount, MCP server, dynamic tool, app, hook, web search, or login
  shell;
- output accepted only as one final text candidate satisfying the closed
  replacement schema; action/tool items, non-text items, reroutes, malformed
  output, extra candidates, oversize output, EOF, timeout, crash, and output
  after cancellation fail closed;
- retained process-group ownership and bounded stdin-close, wait, `SIGTERM`,
  wait, exact-group `SIGKILL` cleanup;
- no source, decision, subject identity, replacement, path, workspace, email,
  or auth bytes in the append-only ledger or projected receipt.

The first real boot exposed two app-server wire-shape mismatches that the fake
had masked: `model/list` requires an object-valued `params`, and the 0.149.1
catalog identifies each row with equal `model` and `id` fields rather than the
older static-catalog `slug`. Both were repaired without weakening the exact
model pin. The fake now enforces the live `params` and catalog shapes.

## Live zero-minute cell

The source-free live harness used the exact installed CLI and generated schema
hashes, managed ChatGPT authentication, local 24-hour budget 100, and a fresh
ledger. Boot completed under the exact Spark model and H-S policy. One fixed
warm-up and two zero-minute real-shaped turns produced:

| Turn | Result | Latency | Input/output tokens | Spark 300-minute meter | Tool/reroute |
|---|---|---:|---:|---:|---|
| fixed warm-up | accepted warm-up | 2,350.978 ms | 2,510 / 75 | 58% -> 59% | none / none |
| first bang | accepted | 2,693.062 ms | 2,963 / 619 | 59% -> 59% | none / none |
| immediate mid-stream | accepted | 2,553.399 ms | 2,963 / 791 | 59% -> 60% | none / none |

Both real-shaped calls used one fresh thread and one turn. The deliberately
strict byte-identity echo comparator did not match on either accepted output;
the harness retained only hashes and did not replay. That comparator is
measurement-only and grants no write authority.

## D1 dogfood

After the valid zero-minute measurement, D1 routed one eligible real
repository edit through the optional elaboration branch. The caller selected
the existing `close-session!` owner and asked to rename only the local binding
`valid` to `ownership`. An independent deterministic comparator constructed
the complete expected owner before model contact.

| Evidence | Value |
|---|---|
| Model turns / candidates | 1 / 1 |
| Tool items / reroutes | 0 / 0 |
| Spark latency | 4,526.834 ms |
| Complete pipeline | 5,855.635 ms |
| Independently prepared ordinary write | 311.834 ms |
| Input/output tokens | 3,209 / 803 |
| Candidate SHA-256 | `963d94377a9e5eb074d21b50eeef2d125d0ccb32178bbb76328aba615fb183bd` |
| Independent expected SHA-256 | `963d94377a9e5eb074d21b50eeef2d125d0ccb32178bbb76328aba615fb183bd` |
| Ordinary receipt | `6fe2f866af1e24ff10f1a810d289270b62fe6d674d7bdb128caa2ed51d1c76f8` |
| Ordinary verification | `verification_complete=true` |
| Cleanup | exact process group empty, `cleanup_ok=true` |

The model candidate and independently derived equivalent were byte-identical.
Only after that comparison did the ordinary public `edit_clojure` compiler,
fresh capture, transaction, and read-back execute. No retry or synthetic case
was used. Correctness, authority, verification, and cleanup conditions passed,
but complete wall did not beat the independently prepared ordinary equivalent.
The product-owned D1 decision therefore returned:

```clojure
{:pass false, :promotion_blocked true}
```

This is a valid dogfood result and a binding installation stop, not a reason to
discard the implementation or hide the measured loss.

## Green gates

The formatted D1 state passed:

```text
focused embedded + stdio + HTTP: 44 tests, 667 assertions, 0 failures, 0 errors
core test-fast:                 647 tests, 5,562 assertions, 0 failures, 0 errors
analyzer contract:               4 tests,    20 assertions, 0 failures, 0 errors
MCP suite:                      347 tests, 4,142 assertions, 0 failures, 0 errors
direct clj-kondo:                 0 errors, 0 warnings
make test:                        PASS, including smoke and benchmark self-tests
```

An earlier cold full-suite attempt produced the known
`cold-clj-kondo-admission-timeout-is-unverified` race as two failed assertions:
the observed state was delegated/unverified rather than admission-timeout. The
isolated cold namespace passed 7 tests / 50 assertions, and the final cold full
suite passed 347 / 4,142. The final gate is green; the earlier result is retained
as cold-flake characterization rather than erased.

## Idle cell and promotion boundary

The same live supervisor remains running for the 60-minute first-bang and
immediate mid-stream comparison. The 240-minute point is explicitly deferred;
no keepalive cadence is enabled. D2 and installation remain outside this
branch and require the separately reserved Gene install card.
