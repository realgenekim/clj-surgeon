# Full-corpus outline witness belongs to the integration lane

## Observable contract and current intent

The full repository differential still enumerates every `.clj`, `.cljc`, and `.cljs` file under src/ and test/, in the same sorted order. It compares pr-str output from the unchanged frozen two-parse legacy oracle against the current outline, retaining the minimum 100-file guard and exact mismatch assertion. No sampling, cache substitution, budget override, or product code change is authorized.

The actual field failures were 8.030 s and 8.312 s against the 8 s fast-namespace budget. Independent diagnosis found 273 files / 7,150,798 bytes at 2cc32d3a versus 211 files / 5,423,443 bytes at lane enrollment, with no accidental duplicate corpus pass. This supports a lane correction, not a production performance regression claim.

## Bounded implementation

Move only source-files and the repository-wide deftest into outline-corpus-integration-test (:integration). Reuse legacy-outline-source by making that existing test helper public; its body and dependent frozen helpers remain unchanged. Keep the four bounded witnesses and their assertions in outline-differential-test (:fast). New integration test refers the legacy helper under its existing local name so the moved test and source-files can remain exactly identical.

Retain the historical round-one namespace set. Register one new integration namespace and one adopted test. Current accounting becomes 920 original + 433 adopted = 1353, with 87 namespaces (49 fast, 6 integration, 32 battery). This is one moved test, not test growth. Ordinary merge/landing gates already include fast+integration; no cadence target changes are needed.

## Verification

Before commit compare extracted old/new source-files and corpus witness exactly; verify frozen oracle body and remaining four witnesses unchanged. Format changed Clojure. Run focused bounded/oracle witnesses and lane-manifest accounting through the existing runner with isolated events/private temp/nice10; run existing fast and integration lane gates where feasible. No providers, broad battery, services, install, or deadline/budget changes.

Native literal edits are appropriate after retained Surgeon owner reads: this is a test-namespace partition and registry bookkeeping, not a source semantic mutation that the mechanical execution interface supports.

## Retained verification

Exact owner-string comparison passed for the moved source-files and corpus deftest. All nine frozen helpers retain their source bodies (legacy-outline-source changes only defn- to defn), and all four bounded deftests are identical. The formatter normalizes the old namespace lane metadata into its explicit metadata map; :fast is unchanged.

Focused existing runner: 30 tests, 133 assertions, 0 failures/errors and 0 isolation violations across the two outline namespaces and lane-manifest-test. The initial command repeated --ns, which the existing runner treats as a namespace token; it refused before tests. Corrected command supplies one --ns followed by the three names. Both receipts remain. The initial invariant script inspected the first node instead of its forms parent; the corrected check requires nonnil source and explicitly verifies every preserved owner. Only invariants-verified.log is authoritative.

Receipts: /var/tmp/forge/outline-lane-split/. No product source, Makefile, budgets, exclusions, provider calls or service configuration changed.

The first complete mcp-test run executed 662 tests / 8,011 assertions with zero failures/errors or precondition skips/failures, but FAILED one working-tree isolation check because the builder edited this plan while the gate was running. That failed receipt is retained as merge-gate.log; it is not a green gate. The corrected full run freezes all worktree files throughout execution and writes only external receipts. Its terminal status is recorded in /var/tmp/forge/outline-lane-split/merge-gate-frozen.log and the final external verification receipt. Final merged-tree make test belongs to the landing owner; no duplicate battery or full make test is run here.

The frozen rerun had already started when the lead instructed the builder to avoid duplicating the designated merged-tree gate. It was interrupted at the lead's direction (exit 130); its partial merge-gate-frozen.log is not a pass. No matching owned runner process remained afterward. CLEAN FULL GATE STATUS: HOLD, required from the landing owner's actual merged-tree make test. The focused 30/133 green and exact invariant checks are the completed branch evidence; the full 662/8011 run remains gate-FAIL for the documented concurrent plan edit.
