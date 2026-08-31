---
parent: embedded-elaborator-specs
status: frozen-red
frozen_at: 2026-08-30
---

# Embedded Spark Elaborator Frozen Red Declaration

This declaration freezes the executable red surface before elaborator product
code. It is based on release lineage `4ec9394` (published parent `9af88fba`),
ratification packet `eaba46b2`, and independent isolation receipt `3c2cc192`.

The checked-in isolation receipt is authoritative. Its receipt SHA-256 is
`289b0b4eb25fec7eb770d2c8b1dc777811ccc9f347a23fff6b60455c3edff0ad`;
its H-S hardening config SHA-256 is
`432cf51484e3c8bde2dad6874fa824a5c3429ace9072bd27cec40c179140cddf`.
The product configuration must be H-S exactly. The supervisor owns stdio JSONL,
rejects every action or non-text final item, and never exposes raw app-server
protocol.

## Frozen count

The red namespace is
`clj-surgeon.mcp-embedded-elaborator-test`. It contains exactly **19 tests** and
**367 assertions**. Against the pre-product tree it reports exactly **324
failures, 0 errors, and 43 passes**. The passes are negative-shape assertions
that already hold for an absent API; the 324 failures are the product gap.

| Spec | Test | Assertions | Frozen failures |
|---|---|---:|---:|
| MCP-OP-ELAB-001 | immutable-isolation-receipt-is-the-admission-root | 10 | 10 |
| MCP-OP-ELAB-002 | prepared-hole-admission-is-closed-and-byte-exact | 30 | 30 |
| MCP-OP-ELAB-003 | identity-is-caller-owned-and-never-enters-model-input | 19 | 10 |
| MCP-OP-ELAB-004 | hostile-output-is-consumed-once-or-rejected-whole | 24 | 19 |
| MCP-OP-ELAB-005 | accepted-body-reenters-only-the-ordinary-writer | 10 | 9 |
| MCP-OP-ELAB-006 | boot-supervisor-never-owns-server-readiness | 10 | 10 |
| MCP-OP-ELAB-007 | exact-model-pin-has-no-fallback | 18 | 18 |
| MCP-OP-ELAB-008 | one-turn-failures-never-replay-or-retain-partials | 79 | 68 |
| MCP-OP-ELAB-009 | cleanup-targets-only-the-owned-process-group | 7 | 7 |
| MCP-OP-ELAB-010 | ledger-is-append-only-attributable-and-source-free | 15 | 6 |
| MCP-OP-ELAB-011 | quota-alarm-and-circuit-thresholds-are-product-owned | 30 | 30 |
| MCP-OP-ELAB-012 | receipts-bind-generation-to-ordinary-effect-without-authority | 14 | 6 |
| MCP-OP-ELAB-013 | d1-records-real-eligibility-and-never-manufactures-a-case | 8 | 8 |
| MCP-OP-ELAB-014 | d1-passes-only-exact-one-shot-parity-and-wall-win | 21 | 21 |
| MCP-OP-ELAB-015 | d2-tripwires-open-the-circuit | 29 | 29 |
| MCP-OP-ELAB-016 | performance-claim-requires-same-stratum-complete-wall-win | 10 | 10 |
| MCP-OP-ELAB-017 | b0432c25-is-a-permanent-identity-prohibition | 8 | 8 |
| MCP-OP-ELAB-018 | permanent-falsifier-boundaries-fail-closed | 13 | 13 |
| MCP-OP-ELAB-019 | keepalive-is-disabled-and-idle-cells-are-exact | 12 | 12 |
| **Total** | **19 tests** | **367** | **324** |

The matrix binds every specification and every design falsifier, including
hostile-output rejection, healthy server behavior after boot failure, exact
Spark pinning with no fallback, one turn with no replay, exact process-group
cleanup, consume-once output, the one-hole wall-classifier boundary, receipt
hash consistency, 80/90 quota behavior, and the `b0432c25` prohibition. The
elaborator receives caller identity only at the authority firewall; its model
input contains only old body and decision, and its output cannot assert
identity.

## Reproduction

```bash
npx --no-install @chrisoakman/standard-clojure-style fix \
  src/clj_surgeon/mcp_intent_contract.clj \
  test/clj_surgeon/mcp_embedded_elaborator_test.clj \
  test/clj_surgeon/mcp_test_runner.clj

clojure -J-Xms64m -J-Xmx512m -Sdeps '{:paths ["test"]}' -M -e \
  "(require 'clj-surgeon.mcp-embedded-elaborator-test)
   (let [r (clojure.test/run-tests
             'clj-surgeon.mcp-embedded-elaborator-test)]
     (println (pr-str r))
     (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
```

Frozen output:

```text
Ran 19 tests containing 367 assertions.
324 failures, 0 errors.
{:test 19, :pass 43, :fail 324, :error 0, :type :summary}
```

No elaborator namespace, process adapter, schema branch, boot hook, ledger, or
write-path behavior exists in this red declaration. Product implementation may
begin only after this declaration is committed and pushed.
