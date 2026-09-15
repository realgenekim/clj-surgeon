NO-GO
CLASS: Any selected namespace lacking admitted lane membership is silently discarded by `:fast`; oracle: `fast-scope-total-membership`, which must classify every selected namespace before projection and refuse every unclassified member.

Finding `IMPACT-EXEC-01`: [`selection-suites`](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/battery_parallel_runner.clj:750) filters to fast members before validating membership. The existing test at [diff_impact_test.clj:305](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/diff_impact_test.clj:305) even expects an unknown fast member to become `{}`, contradicting DIFF-IMPACT-007.

Reproduction:

```text
bb ... (user/run-selected-scope!
  [{:namespace 'clj-surgeon.renamed-away-test}] :fast ...)
=> {:selected [], :receipts [], :runs [], :exit 0, ...}
```

Thus a renamed, load-excluded, or otherwise unregistered selected namespace can produce a false-green partial observation without running anything.

Other attacks passed:

- `bb ... /var/tmp/forge/impact-review-probes.clj`
  - battery freshness: `:partial-receipt-not-a-gate-receipt`
  - census gate: `{:kind :partial-receipt-not-a-gate-receipt}`
  - prewarm entry: typed refusal
  - landing entry/receipt chain: typed refusal and `:eligible false`
  - omitted selected result: `:state :failed`
  - unselected result: `:state :failed`, `:unexpected [b]`
- Parity checker: baseline exit 0; retained `-Duser.home`-omission outcome exited 1 with:
  `{:namespace clj-surgeon.fast-lane-isolation-test, :control true, :candidate false}`.
- Child-command comparison: `:argv-prefix-delta []`; only the namespace tail differed.
- D2:
  - list: 9 selected, 4,360 ms
  - fast: 7 executed, 13,082 ms selection+execution, exit 0
- D3:
  - list: 101 selected, 4,293 ms
  - fast: 51 executed, 36,626 ms, expected `lane-manifest-test` red
  - all: 101 executed, 790,778 ms; separately reported battery catches included `txn-journal-test` and `receipt-artifacts-boundary-test`
- HOLD/nothing-selected checks:
  - `select-impact` hashes matched: `a925e441…`
  - HOLD/nothing block hashes matched: `4bcafd9e…`
  - both intent files matched their complete base prefixes (`cmp` exit 0)
  - allowlist diff exit 0

No repair was left: correcting the contradictory existing test requires replacing/deleting lines under `test/`, which the supplied ship contract classifies as `HOLD reason=oracle-changed`. `git diff HEAD --numstat` is empty; the pre-existing untracked Codex log remains untouched.

> END RECEIPT (fence-run): worktree HEAD at review exit = ebf01a960683eb6f419f7cee15e9fcd792aed7dd = fenced sha.
