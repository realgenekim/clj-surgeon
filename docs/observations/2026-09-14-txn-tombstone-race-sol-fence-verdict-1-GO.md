GO

No blocking findings on sealed candidate `5ae0066c75ef3b19d28c94405aa674573ce5a29a`.

1. Caller census and vanished-path semantics

`rg -n -F 'artifacts/admit-target!' src/clj_surgeon` found 19 external direct sites across 13 namespaces. The four internal consumers are `call-with-state-home`, `admitted-file`, `directory`, and `target`. `rg -n -F 'artifacts/admitted-file'` found another 18 indirect sites in `extract`, `mission_typist_executor`, and `txn_journal`. `resolved-target` has no external production caller; only `admit-target!` resolves requested paths and envelope roots.

Every consumer either constructs an admitted name, narrows state-home authority, or admits a destination before creating/updating an artifact. None treats admission as proof that the leaf existed. Readers still perform their own read/hash operation; publishers already support an absent destination. Therefore “vanished during resolution → absent tail” is correct for the whole enumerated caller set; no caller needs a distinct `:vanished` admission result.

2. Witness seam

`git grep -n '*resolution-interleave*' 5ae0066… -- src test` returned only its definition/use and two test `ns-resolve` sites. The same search over MCP/core surfaces returned no export or binding.

Runtime inspection printed:

```clojure
{:meta {:private true, :dynamic true}, :root nil}
```

`interleave-resolution!` invokes the hook and then explicitly returns `nil`, so its return value cannot affect admission. No request field reaches the Var. Arbitrary code already executing inside the process could reflectively mutate any Var, but this adds no MCP caller-controlled channel.

3. Break protocol

Source inspection confirms the absence race is closed before the kernel protocol. Thereafter:

- marker creation returns one of four typed states;
- `createLink` maps EEXIST, missing LOCK, unsupported links, and other failures;
- link completion maps disappearance or other filesystem failure to typed outcomes;
- the unsafe move fallback also catches and types its failures.

The only upstream exceptions intentionally retained are admission/envelope and access refusals. A blanket catch in `break-lock!` would incorrectly downgrade those security refusals, so omitting it is correct.

4. Symlink coverage

The test uses `Files/createSymbolicLink`, deletes that actual link from the `:link-target` seam, and asserts its absent-tail resolution. This is executable coverage, not reasoning alone.

The focused runner reported:

```text
Ran 123 tests containing 989 assertions.
0 failures, 0 errors.
test-isolation: 0 violations
```

5. Lane declarations

The ad-hoc JVM invocation genuinely refuses the three excluded namespaces with exit 96. However, `lane_manifest/excluded` redirects them to dedicated targets, and `make --no-print-directory print-gate-stages` listed:

```text
admit-transaction-recovery-battery
battery-fresh
alias-migration-test
mcp-test
test-bb
test-bb-diagnostic
repository-hygiene
intent-audit
```

Those excluded namespaces are not selected by the normal landing coordinator, so they will not cause this landing gate to refuse.

6. Heredoc-produced bytes

All four changed Clojure files parsed completely: 33, 130, 45, and 117 top-level forms. Both commit-range `git diff --check` commands produced no output. The census diff contains exactly the eight new `deftest` names found by `rg`. Focused lint reported `0 errors, 4 warnings`; all four warnings are in pre-existing code outside the changed implementation/test additions. The intent-contract test reported `22 tests`, `567 assertions`, `0 failures`, `0 errors`.

No `make test` was run. No candidate bytes were changed; `git diff HEAD --numstat` is empty. The only status entry remains the pre-existing untracked review log.

> END RECEIPT (fence-run): worktree HEAD at review exit = 5ae0066c75ef3b19d28c94405aa674573ce5a29a = fenced sha.
