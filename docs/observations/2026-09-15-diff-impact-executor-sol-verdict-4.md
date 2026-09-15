NO-GO
CLASS: Nonempty selections whose `:fast` projection is empty—enumerated by `(seq classification)` plus `(empty? (project-selection classification :fast))`; the `fast-scope-total-membership` oracle must require a refusal or at least one executed namespace.

IMPACT-EXEC-02: other-lane-only fast selection falsely passes

The public coordinator in [test/diff_impact.clj](/home/forge/src/clj-surgeon-fence/test/diff_impact.clj:73) treats `(every? ... [])` as success.

Fresh classification/execution probe:

| Class | `:fast` | `:all` |
|---|---|---|
| unregistered/renamed | exit 1, `selected-namespace-unclassified` | exit 1, same |
| load-excluded | exit 1, `selected-namespace-unclassified` | exit 1, same |
| bb-ineligible | exit 1, `selected-namespace-unclassified` | exit 1, same |
| other-lane-member | **exit 0, selected `[]`, runs `[]`, child calls 0** | exit 0, selected/run `[other-test]` |

Exact red output:

```clojure
{:classification {:namespace other-test, :classification :other-lane-member,
                  :lane :integration, :suite "mcp"},
 :scope :fast, :exit 0, :selected [], :runs [], :child-calls 0}
```

This directly violates “none can reach exit 0 with nothing run.”

CLASS: Every readable `:partial true` receipt carrying a known toolchain and obligation inventory; an `installed-envelope-rejects-every-partial-receipt` oracle must drive `/home/forge/bin/gate-envelope.clj` for every partial suite writer.

ENVELOPE-01: the installed envelope accepts partial and forged evidence

A real partial run:

```text
PARTIAL_RUN_RC=0
:partial true
:state :passed
:discharged-by-this-receipt []
```

Installed consumer command:

```sh
GATE_WORKTREE="$PWD" \
GATE_RECEIPT_PATH="$partial/receipt.edn" \
GATE_OBLIGATIONS_PATH="$partial/gate-obligations.edn" \
bb /home/forge/bin/gate-envelope.clj "$partial/envelope.edn"
```

Output:

```text
state=:complete obligations=11 executions=0 pending=[]
PARTIAL_ENVELOPE_RC=0
scope={:kind :landing ...}
proof={:complete? true :pending [] ...}
```

Worse, changing only producer-controlled receipt fields to eight claimed successful stages, `:landing? true`, and invented Git identities produced:

```text
state=:complete obligations=11 executions=8 pending=[]
FORGED_ENVELOPE_RC=0
:candidate {:commit "forged-head", :tree "forged-tree", ...}
:authority {:landing? true, :prewarm? false, :debug? false}
:proof {:complete? true, :pending []}
```

Nothing cryptographically or independently binds the toolchain, stages, executions, Git identity, or inventory to processes that ran. `:producer.custody :self-reported` labels this weakness, but the installed program still exits 0 and emits complete landing authority. A downstream consumer that categorically refuses `:self-reported` would prevent discharge; this installed boundary itself does not.

Other requested evidence:

- Fresh partial-gate probes refused correctly:

```text
:battery-fresh ... :partial-receipt-not-a-gate-receipt
:census-gate [{:kind :partial-receipt-not-a-gate-receipt ...}]
:prewarm-entry ... :partial-receipt-not-a-gate-receipt
:landing-entry ... :partial-receipt-not-a-gate-receipt
:landing-receipt-chain {:eligible false ...}
```

- Census attacks were red:

```text
:selected-silently-omitted {:state :failed, ... :missing [a]}
:unselected-ran {:state :failed, ... :unexpected [b]}
```

- Fresh argv comparison: `{:argv-prefix-delta []}`.
- Fresh positive parity: `:parity :passed`, `:delta []`.
- Retained omission negative: exit 1 with
  `{:namespace clj-surgeon.fast-lane-isolation-test, :control true, :candidate false}`.
- Retained measured-result projection:

```text
D2 fast: 9 classified / 7 run, 4199 + 8148 = 12347 ms, exit 0
D3 fast: 101 classified / 51 run, 4238 + 32118 = 36356 ms, exit 1
D3 all:  101 classified / 101 run, 4480 + 773462 = 777942 ms, exit 1
```

D3 all separately reports battery `160219 ms`, dedicated `513078 ms`, fast `29559 ms`, and MCP `57471 ms`; battery catches were not blended into the fast wall.

- HOLD/nothing-selected/allowlist evidence remained byte-identical: `select-impact` SHA-256 `3d3d1bec…`, HOLD block `4bcafd9e…`, and both intent files retained identical base prefixes.
- The exact fresh prewarm command was attempted, but produced no receipt: exit 2 after namespace isolation detected the pre-existing Codex log changing during execution. The retained clean fixed control remains consumable:

```text
state=:complete obligations=11 executions=7 pending=[]
RETAINED_FULL_ENVELOPE_RC=0
```

No `make test` ran. No tracked files or patch were left; only the pre-existing untracked review log remains.

> END RECEIPT (fence-run): worktree HEAD at review exit = 13eafcb64687321cd73c1dbde0e9d933ab36cfbb = fenced sha.
