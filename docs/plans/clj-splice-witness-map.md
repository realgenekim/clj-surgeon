# Witness map

| Old witness | New group |
|---|---|
| `rename-alias-e4-verbatim` | `clj-surgeon.rename-alias-test/rename-alias-e4-verbatim` |
| `rename-alias-prefix-trap` | `clj-surgeon.rename-alias-test/rename-alias-role-selection` |
| `rename-alias-reader-roles` | `clj-surgeon.rename-alias-test/rename-alias-role-selection` |
| `rename-alias-string-decoy` | `clj-surgeon.rename-alias-test/rename-alias-role-selection` |
| `rename-alias-discard-decoy` | `clj-surgeon.rename-alias-test/rename-alias-role-selection` |
| `rename-alias-binding-matrix` | `clj-surgeon.rename-alias-test/rename-alias-binding-matrix` |
| `repository-ignores-non-clojure-symlinks` | `clj-surgeon.rename-alias-test/rename-alias-scope` |
| `comment-ancestry-exempts-mutations-but-keeps-references` | `clj-surgeon.rename-alias-test/rename-alias-scope` |
| `skipped-namespace-mutations-do-not-refuse` | `clj-surgeon.rename-alias-test/rename-alias-scope` |
| `skipped-duplicate-bindings-do-not-refuse` | `clj-surgeon.rename-alias-test/rename-alias-scope` |
| `rename-alias-scope-guards-counts` | `clj-surgeon.rename-alias-test/rename-alias-stale-and-counts` |
| `rename-alias-source-boundaries` | `clj-surgeon.rename-alias-test/rename-alias-parse` |
| `candidate-parse-refuses` | `clj-surgeon.rename-alias-test/rename-alias-parse` |
| `candidate-role-recount-refuses` | `clj-surgeon.rename-alias-test/rename-alias-candidate-integrity` |
| `candidate-form-preservation-refuses` | `clj-surgeon.rename-alias-test/rename-alias-candidate-integrity` |
| `candidate-inverse-identity-refuses` | `clj-surgeon.rename-alias-test/rename-alias-candidate-integrity` |
| `rename-alias-receipt-oracle` | `clj-surgeon.rename-alias-test/rename-alias-preservation-address` |
| `column-one-reference-addresses` | `clj-surgeon.rename-alias-test/rename-alias-preservation-address` |
| `rename-alias-transaction-faults` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `replacement-read-back-refuses` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `receipt-observes-disk-neighbor-corruption` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `receipt-observes-disk-trivia-and-discard-corruption` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `receipt-details-contains-observes-artifact` | `clj-surgeon.rename-alias-receipt-test/rename-alias-publication-evidence` |
| `insert-forms-after-prefix-named-defn` | `clj-surgeon.insert-forms-test/insert-forms-exact-root-anchor` |
| `insert-forms-comment-string-anchor-decoys` | `clj-surgeon.insert-forms-test/insert-forms-exact-root-anchor` |
| `insert-forms-anchor-cardinality` | `clj-surgeon.insert-forms-test/insert-forms-exact-root-anchor` |
| `insert-forms-candidate-envelope` | `clj-surgeon.insert-forms-test/insert-forms-exact-root-anchor` |
| `insert-forms-deftest-nested-testing` | `clj-surgeon.insert-forms-test/insert-forms-body-anchor` |
| `insert-forms-body-boundaries` | `clj-surgeon.insert-forms-test/insert-forms-body-anchor` |
| `insert-forms-defn-headers-and-arities` | `clj-surgeon.insert-forms-test/insert-forms-body-anchor` |
| `insert-forms-payload-count-and-order` | `clj-surgeon.insert-forms-test/insert-forms-payload-count-and-order` |
| `insert-forms-trivia-and-indentation` | `clj-surgeon.insert-forms-test/insert-forms-layout-literal-preservation` |
| `insert-forms-existing-separators` | `clj-surgeon.insert-forms-test/insert-forms-layout-literal-preservation` |
| `insert-forms-unbalanced-payload-refuses` | `clj-surgeon.insert-forms-test/insert-forms-parse-stages` |
| `insert-forms-parse-errors-refuse` | `clj-surgeon.insert-forms-test/insert-forms-parse-stages` |
| `insert-forms-candidate-structure-refuses` | `clj-surgeon.insert-forms-test/insert-forms-candidate-structure-refuses` |
| `insert-forms-other-top-level-hashes` | `clj-surgeon.insert-forms-test/insert-forms-other-top-level-hashes` |
| `insert-forms-stale-hash-refuses` | `clj-surgeon.insert-forms-receipt-test/insert-forms-snapshot-guard` |
| `insert-forms-portable-read-receipt` | `clj-surgeon.insert-forms-receipt-test/insert-forms-snapshot-guard` |
| `insert-forms-atomic-multiform-and-race` | `clj-surgeon.insert-forms-receipt-test/insert-forms-publication-evidence` |
| `insert-forms-planned-receipt-recovery` | `clj-surgeon.insert-forms-receipt-test/insert-forms-publication-evidence` |
| `insert-forms-receipt-accounting` | `clj-surgeon.insert-forms-receipt-test/insert-forms-publication-evidence` |
| `insert-forms-terminal-response-eligibility` | `clj-surgeon.insert-forms-receipt-test/insert-forms-publication-evidence` |
| `insert-forms-reader-safety-and-limits` | `clj-surgeon.splice-envelope-test/bounded-input-path-encoding` |
| `insert-forms-path-confinement` | `clj-surgeon.splice-envelope-test/bounded-input-path-encoding` |
| `insert-forms-refusal-remedies` | deleted: English regex; actionable state remains in anchor/stale groups |
| both `*-cli-mcp-parity` | retained entrance group, reduced dispatch and representative boundaries |
| `four-thousand-line-planning-under-five-seconds` | retained separately budgeted performance measurement |

No guard mutant is deleted. Grouping retains old names as testing labels. Shared boundary tests retain resource/path/encoding scenarios.

Execution reduction: each accept/refuse fixture now compares its complete outcome tuple in one assertion (exact bytes, role count and independent oracle still checked). Both entrances run two success spellings, one MCP success with disk detail hash, one schema refusal through CLI/MCP, and one I/O exit. Full grammar runs once in the shared envelope; rollback/foreign-byte outcomes remain in publication groups. False scenarios are memoized within each verb's class test only. No globally cached receipts survive a test.

Further named row moves: rename-alias-source-boundaries' repeated path synonyms, 513-depth/8MiB resource rows, symlink/hardlink checks and malformed UTF-8 disk case → bounded-input-path-encoding. One rename scope-path adapter case stays. The shared UTF-8 case now drives both disk entrances against the same invalid byte fixture. Malformed alias synonyms collapse to seven token equivalence classes in binding-matrix. Repeated EDN reader rows → exactly-one-edn-request. E4 insertion reconstruction plus four source/anchor/count/parse refusals were added to exact-root-anchor; the expected output remains the frozen 02332a74 file.
