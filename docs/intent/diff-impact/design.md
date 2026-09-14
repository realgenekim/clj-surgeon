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

Read test namespace source as syntax without evaluation. Existing file literals
under docs/ or resources/ create data-file edges. Source file literals create
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
| iv | unmatched file or empty diff | :nothing-selected, exit 0, changed paths and reason |
| v | direct/transitive require edge | existing set and cycle termination preserved |

CLI positions remain BASE OUTPUT MODE. list writes selection only; fixed-point,
before, after and merged execute the same selection. Empty selection writes
typed inventory and results without launching children or reading absent results.
Each printed reason has the shape `selected <ns> via <edge-kind> <file>`.
