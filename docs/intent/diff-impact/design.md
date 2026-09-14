# Diff-impact selection

The test oracle computes a fixed point over namespace requirements and explicit
file-content inputs. Its purpose is to name witnesses that must run when Git
reports a changed path, independently of test lane or runtime. This extends the
verification boundary in [the vision](../../vision.md) and preserves the narrow
execution environment required by DATACODE-ENV-002.

## Selection model

Inventory declarations under both src/ and test/. Namespace edges retain the
existing libspec grammar, cycles and unbounded transitive closure. Test-side
helper namespaces participate even when they are not themselves test endpoints.
Git changed paths are not restricted to src/.

Read src/ and test/ namespace source as syntax without evaluation. Existing non-source file literals create data-file edges, including files
at the repository root and under config/. Git supplies tracked and unignored
untracked paths; only existing regular files whose realpaths stay in the
repository enter the inventory. Source file literals create
source-text edges; source directory literals in a namespace with slurp, file-seq
or source-scanning helpers create source-scan edges to files under that root.
Composed paths with a literal root (the splice witness's docs/intent/ and
src/clj_surgeon/ prefixes) conservatively cover existing descendants of that
root. This is intentionally an overapproximation, not arbitrary value flow or
execution of repository code. Comments and regex contents are not string inputs.
Paths outside the repository and absent literal files do not create edges.

The pure selector is src/clj_surgeon/diff_impact.clj. The script retains Git,
filesystem inventory and execution, while its class oracle requires the pure
namespace directly. This also makes changes to the selector select its own tests.

Every selected namespace retains require paths and deterministic reasons naming
the changed file and edge kind. Counts distinguish inventory edge kinds from
selection reasons. No dynamic reference completeness or behavioral proof follows
from selection. Deleted namespace definitions and computed roots with no literal
anchor remain outside this round's current-snapshot analysis.

## Requirements

See [EARS](diff-impact-specs.md). The direct witnesses are
test/clj_surgeon/diff_impact_test.clj; the executable is test/diff_impact.clj.

| Cell | Input | Required result |
|---|---|---|
| i | changed existing docs/resources input | exact reader test set |
| ii | changed source text below scan root | scanner test set, including non-required source |
| iii | changed helper under test/ | all transitive requiring tests; no unrelated test |
| iv | unmatched file, including mixed selection | :hold-unmatched-files; non-list exit 1 before children; list exit 0 |
| vi | empty diff | :nothing-selected, exit 0, empty paths |
| v | direct/transitive require edge | existing set and cycle termination preserved |

CLI positions remain BASE OUTPUT MODE. list writes selection only; fixed-point,
before, after and merged execute the same selection. Unmatched files HOLD before execution, including when other files selected tests.
Only an empty diff returns :nothing-selected. The tracked no-test-can-depend.edn
allowlist is empty: no Markdown or observation exemption is asserted.
HOLD writes typed inventory and results without reading absent results.
Each printed reason has the shape `selected <ns> via <edge-kind> <file>`.

## Round 2 boundary

Choose HOLD (exit 1), not a fallback lane: the minimum lane has no established
bounded wall for this gate, and measuring the full manifest is outside this round.
Source content edges seed the existing reverse require closure; no unrelated
source namespace can contribute a reason to a test outside that closure.
EDN configuration value flow, relative io/resource classpath resolution, deleted
inputs and unanchored dynamic paths remain unresolved; unmatched changes HOLD.
A seed with declared require or content dependents but no reachable test is
:no-dependency-edge;
:no-test-dependent describes only an isolated namespace seed.
