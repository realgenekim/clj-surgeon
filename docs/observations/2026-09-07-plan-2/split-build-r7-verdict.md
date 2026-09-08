# NO-GO

Do not land `d31c3af7` yet. I did not install the plate or modify either global instruction file.

Blocking findings:

1. The verifier does not enforce the complete routing doctrine. `validate-routing-block` returned `:ok true` for plates that:

   - changed “TWO automatic classes” to three;
   - removed the exact `d9205abc` / `views.clj` witness;
   - removed “changed mapping/policy/source shape needs new admission”;
   - replaced the alias-migration operation.

   This violates the validation promises in [agent-routing-specs.md](/home/forge/src/clj-surgeon-fence/docs/intent/agent-routing/agent-routing-specs.md:5). The incomplete needles are in [agent_routing.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/agent_routing.clj:26).

2. The proof rule does not state the requested “nonzero exit is excused only when baseline-attributed” rule. [The plate](/home/forge/src/clj-surgeon-fence/resources/clj-surgeon-agent-routing.md:78) requires exit zero only for “required profile checks.” Consequently, a non-profile `{:exit 3 :status "passed"}` check without `:baseline` is not unambiguously refused, while the real Cell C receipts contain baseline-attributed exit-3 checks.

3. The working-tree [clj-surgeon skill](/home/forge/src/clj-surgeon-fence/skill.md:8)—which repository instructions say supersedes installed skill copies—still routes fan-out and alias migration as the two automatic classes and does not admit namespace split. Installing the new plate would leave conflicting instructions.

4. After fetching, `origin/MCP/main` is `997fb591`. The dry merge conflicts in `docs/observations/battery-ledger.edn`: trunk added receipts `9d132e7f`/`8795741f`, while the branch added `ec64f322`. They need an append-preserving reconciliation.

5. `git diff --check 4856f5aa..d31c3af7` reports an extra blank line at EOF in [agent-routing-design.md](/home/forge/src/clj-surgeon-fence/docs/intent/agent-routing/agent-routing-design.md:54).

## Probe table

| Probe | Result |
|---|---|
| Delta stat | 10 files; 320 insertions, 114 deletions |
| Human plate: exact Cell C contract | PASS: `d9205abc`, views source, 141 owners, 20 absent destinations, 87 sites, five callers, policies, roots, deletion and named cold verification |
| Human plate: route boundary | PASS: exactly alias migration + exact namespace split; informed fan-out suspended but schema retained; unknown-owner discovery not routed |
| Proof mutants | `committed-probe-only`, `verification_complete=false`, nonempty `proof_pending`, missing profile check, nonzero profile exit, and failed/missing registered papercuts are textually refused |
| Baseline-only exception | FAIL: not explicitly encoded |
| Plate-only check | PASS: 183 lines, 10,661 bytes, hash `57a2abb…237bbde`, doctrine `3c2eb6a9` |
| Budget | PASS against the 188-line / 10,738-byte measured cap defined in [agent-routing-design.md](/home/forge/src/clj-surgeon-fence/docs/intent/agent-routing/agent-routing-design.md:33) and enforced in [agent_routing.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/agent_routing.clj:130) |
| Stale installed-block simulation | PASS: Make variable override supported; copied/one-byte-altered current Claude block refused with exit 2 and `:missing-required-routing-section` |
| Red witness on `4856f5aa` | PASS: 4 selected tests, 24 intended failures, 0 errors |
| Green witness at tip | PASS: 10 tests / 142 assertions |
| Intent audit | PASS: `ok=true`, no violations; all three ROUTING IDs have implementation and test witnesses; 144 existing pending-witness violations retained |
| `make test` | PASS: JVM 795/10,166 and Babashka 878/7,675; zero failures/errors |
| Dry merge | FAIL: battery-ledger conflict |
| Global files | Unchanged; no installation performed |

Required repair: strengthen the verifier and adversarial witnesses, specify the baseline exception precisely, synchronize `skill.md`, reconcile current trunk, remove the whitespace error, then rerun the focused witnesses, plate-only check, intent audit, `make test`, and dry merge.

The working-tree `clj-surgeon` skill materially influenced the verdict by exposing the live routing contradiction. The mandated `linked-intent-dev` skill was unavailable, so I audited HLD → design → EARS → tests → implementation directly.

> END RECEIPT (fence-run): worktree HEAD at review exit = d31c3af77d3830e43f6fd762a71166224dd51614 = fenced sha.
