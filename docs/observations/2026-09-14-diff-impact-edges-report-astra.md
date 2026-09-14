# Round 1: diff-impact file-content edges and typed zero selection

Recorded 2026-09-14T09:43:51.276803+00:00. Branch `fable/impact-entrance`, starting snapshot
`8aedb65e`. Issues: inb-f85401 and inb-1b7f3c. No merge or push to main.

## Outcome and committed red

The class oracle was committed **before** the selection fix as `e3e93f33`.
Its commit message contains the complete captured RED output. The same output
is retained in [red.log](2026-09-14-diff-impact-edges/red.log).

| Cell | Before the fix | After the fix |
|---|---|---|
| i: docs/resources file read by path | selected empty set, exit 0 | exact reader namespace set |
| ii: non-required source text, literal/directory/composed path | selected empty set, exit 0 | exact scanner namespace set |
| iii: test-side helper with direct/transitive tests | selected empty set, exit 0 | both direct and transitive witnesses |
| iv: unmatched README change | list lacks typed status and paths; fixed-point crashes reading missing results | typed `:nothing-selected`, exact path/reason, exit 0 |
| v: source require control | selected fixture.control-test | identical selection and require path |

RED: **5 tests / 25 assertions, 13 failures, 0 errors**. Cells i–iv fail;
the require control passes. The red commit's only executable preparation was
wrapping the original script body in `main` and admitting `--library` loading.
Selection and execution were still the old implementation.

Final class oracle: **9 tests / 66 assertions**, zero failures/errors on JVM
and Babashka. The added pure matrix covers composed registry paths, source files,
source walks/helpers, normalized relative paths, absent/escaping/absolute-outside
paths, comments, regexes, reader discard/eval, nested source strings, cycles,
edge cuts, multiple seeds, deterministic reasons, empty diffs and unmatched
source namespaces with no test dependent.

## Implementation and registrations

`test/diff_impact.clj` remains the executable boundary. Its pure selector lives
in `src/clj_surgeon/diff_impact.clj`, required directly by the new class oracle.
The original require parser and fixed point moved unchanged. File discovery
reads declarations under both src/ and test/ and obtains all Git changed paths
with NUL-delimited output; test helpers can be seeds and intermediate nodes.

Test-side namespace content contributes `:data-file`, `:source-text` and
`:source-scan` edges. Data literals must name existing files under docs/ or
resources/. A source literal can name a file; a literal directory in a namespace
containing slurp/file-seq/glob/walk/source-scan vocabulary covers its descendants.
Composed paths with a literal directory prefix deliberately overapproximate:
this catches splice-envelope-test's `(str "docs/intent/" verb "/refusals.edn")`
and `(str "src/clj_surgeon/" file)`. It does not execute subject code.

The inventory reports `:status`, `:changed-files`, `:edge-counts`,
`:selection-edge-counts`, `:unmatched-files`, and each selected namespace's
`:paths` and `:reasons`. Reasons retain the seed namespace, so two distinct seeds
can produce identical printed namespace/kind/file lines. Selection counts count
these reason records; graph counts count declared require and content edges.
`:require` reasons denote the changed namespace-file seed and its reverse closure.

An unmatched nonempty diff returns, for example:

```clojure
{:status :nothing-selected
 :changed-files ["README.md"]
 :unmatched-files [{:file "README.md" :reason :no-dependency-edge}]
 :namespaces []}
```

An existing changed namespace without reachable tests instead names
`:no-test-dependent`. Fixed-point writes the typed result to its results file
and starts no children; `list` records the same typed inventory. Neither reads
an absent results file. Empty selection is not test-pass evidence.

Five registrations are present: lane_manifest.clj `:integration` entry;
namespace `{:lane :integration}`; runtime count **162 → 163**; adoption line;
and `make census-regenerate` adding **9 named tests, removing none** (5 in RED,
4 in GREEN). An explicit JVM runtime and real JVM/bb control receipts were also
added. The lane-manifest check exposed the separately maintained portability
inventory/table, which now includes the new namespace as portable. Existing
runtime assignments, historical control rows and wall observations are preserved.

Linked intent: [design](../intent/diff-impact/design.md),
[DIFF-IMPACT-001..005](../intent/diff-impact/diff-impact-specs.md),
[plan](../plans/diff-impact-edges.md). Repository intent audit reports
`{:ok true, :violations []}`.

## Callers and real cases

Read the complete original script and launcher. No diff-impact invocation was
found in the Makefile, `/home/forge/bin/ship`, or `/home/forge/bin/lib`.
The existing `test/diff-impact` shell launcher is the caller and remains unchanged.
The third positional argument is a **mode**, never a target namespace:

```sh
test/diff-impact BASE OUTPUT_DIR list
test/diff-impact BASE OUTPUT_DIR fixed-point
```

The replays below use the same fixed-point selector in **list mode**, against
separate detached worktrees at the named historical snapshots. They assert
selection only, without executing the broad historical suites. Because this
round requires all scratch beneath `/var/tmp/forge/impact-fx`, the read-only
replay harness loads the current script with `--library` and replaces only
`gate-environment?` with `(constantly true)` before calling `main`. The public
launcher creates a sibling `/var/tmp/forge/clj-surgeon-suite-*` root and a lease,
so it was not run. These receipts make **no gate-envelope or test-run claim**;
production admission and child-envelope comparison were not weakened.

Exact replay body, from each historical worktree, with current repository src,
test and libs/clj-splice/src on the bb classpath and temp properties set to the
round root:

```clojure
(binding [*command-line-args* ["--library"]]
  (load-file "/home/forge/src/clj-surgeon-impact/test/diff_impact.clj"))
(with-redefs [gate-environment? (constantly true)]
  (main [BASE OUTPUT_DIR "list"]))
```

### df0c9e1c

Snapshot `df0c9e1c5f21e5a80027cf35ae68d63e1625f403`; BASE `df0c9e1c^`.
`git show df0c9e1c` changes only src/clj_surgeon/probe.clj, adding the
`forwarded-refusal-kind` marker to the actual ex-data relay. The splice witness
reads the registry and scans source text without requiring probe. It is now
selected via `:source-scan`. **72 namespaces selected**, exit 0.
Graph edge counts: `{'require': 1875, 'data-file': 19270, 'source-text': 41, 'source-scan': 5292}`.
Reason counts: `{'source-scan': 55, 'require': 40}`.

All selected namespaces, one actual reason per namespace (complete reasons and
paths in [EDN](2026-09-14-diff-impact-edges/df0c9e1c.edn) and
[stdout](2026-09-14-diff-impact-edges/df0c9e1c.log)):

```text
selected clj-surgeon.txn-journal-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.admit-patch-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.alias-migration-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.analyzer-contract-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.battery-parallel-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.cli-dispatch-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.cljc-existing-ops-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.core-discovery-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.extract-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.fast-lane-isolation-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.helper-extraction-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.insert-forms-receipt-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.install-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.jvm-error-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.lane-manifest-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.ls-tree-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-alias-migration-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-change-buffer-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-combinable-transaction-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-compact-edit-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-compact-location-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-compact-relations-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-create-files-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-expect-guard-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-extraction-plan-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-feature-thread-sed-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-feature-thread-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-formatter-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-helper-extraction-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-hot-verify-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-http-server-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-inspect-cold-job-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-inspect-tool-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-namespace-split-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-operation-async-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-operation-registry-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-prepared-confirmation-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-prepared-request-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-process-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-recovery-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-relation-census-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-server-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-tool-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mcp-write-refusal-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.memory.journal-green-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.memory.oom-reproduction-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.mission-commit-cli-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-events-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-fallback-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-git-ledger-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-phase-events-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-provider-fallback-events-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-publication-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-run-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-typist-executor-admission-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-typist-executor-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.mission-usage-executor-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.namespace-split-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.namespace-split-warm-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.ns-isolation-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.outline-corpus-integration-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.parser-admission-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.probe-state-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.reader-eval-fence-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.receipt-artifacts-boundary-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.receipt-booleans-test via require src/clj_surgeon/probe.clj
selected clj-surgeon.rename-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.scope-stream-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.splice-envelope-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.split-proof-gate-boundary-test via source-scan src/clj_surgeon/probe.clj
selected clj-surgeon.workspace-onboarding-test via source-scan src/clj_surgeon/probe.clj
```

### 00566756..8aedb65e

Snapshot `8aedb65e2c742592effe5926c45b78e6d73b03a7`; BASE `00566756`.
The full historical diff changes 24 files, including battery_parallel_runner.clj,
lane_manifest.clj, tests, registry observations and portability records. It does
**not** change battery_ledger.clj in this particular interval. Battery-ledger-test
is selected through the changed ledger data path; battery-parallel-test and
lane-manifest-test have changed test-tooling require seeds. The class oracle
separately isolates a helper-only diff and proves direct/transitive selection.
**9 namespaces selected**, including all three named by Sol; exit 0.
Graph edge counts: `{'require': 1888, 'data-file': 19356, 'source-text': 41, 'source-scan': 5292}`.
Reason counts: `{'data-file': 126, 'require': 11}`.

```text
selected clj-surgeon.agent-routing-test via data-file docs/observations/battery-ledger.edn
selected clj-surgeon.battery-ledger-test via data-file docs/observations/battery-ledger.edn
selected clj-surgeon.battery-parallel-test via require test/clj_surgeon/battery_parallel_runner.clj
selected clj-surgeon.battery-state-admission-test via require test/clj_surgeon/battery_parallel_runner.clj
selected clj-surgeon.lane-manifest-test via require test/clj_surgeon/lane_manifest.clj
selected clj-surgeon.mcp-intent-contract-test via require test/clj_surgeon/lane_manifest.clj
selected clj-surgeon.ns-isolation-test via require test/clj_surgeon/lane_manifest.clj
selected clj-surgeon.receipt-artifacts-boundary-test via require test/clj_surgeon/battery_parallel_runner.clj
selected clj-surgeon.splice-envelope-test via data-file docs/intent/battery-ledger/battery-ledger-design.md
```

[Complete EDN](2026-09-14-diff-impact-edges/00566756--8aedb65e.edn) and
[stdout](2026-09-14-diff-impact-edges/00566756--8aedb65e.log) preserve every
changed path, require path and seed reason, including additional conservative
file-content selections.

## Verification and retained failures

Every JVM invocation uses `-Xmx1024m` and java.io.tmpdir under the round root;
Babashka receives the matching explicit temp property. No make test or
test-battery was run.

- New class oracle: JVM **9 / 66**, bb **9 / 66**, zero failures/errors.
  [JVM control](2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.diff-impact-test-jvm-test.control.edn),
  [bb control](2026-09-12-bbtower-block-b/attempt22/controls/clj-surgeon.diff-impact-test-bb-test.control.edn).
  Each records its actual argv, source hashes, result, exit and measured wall.
- Four affected namespaces (lane-manifest, battery-ledger, battery-parallel,
  splice-envelope): **94 / 2,429**, initially one failure and no errors.
  The failure was the missing portability inventory entry for the new namespace;
  the other three namespaces passed. [Retained run](2026-09-14-diff-impact-edges/affected.log).
- After inventory repair, lane-manifest: **35 / 1,935**, zero failures/errors.
  [Final run](2026-09-14-diff-impact-edges/lane-final.log). The other three
  namespaces' code and tests were unchanged by that repair.
- Original script `--self-test`: exit 0; depths 1..8, every cut, cycles, multiple
  seeds, libspec shapes, completion and environment checks retained.
- `standard-clj fix` on changed executable Clojure files; registration patches
  retain surrounding formatting. `~/bin/clj-kondo --lint` on the pure selector,
  script, class oracle and both manifest files: **0 errors, 0 warnings**, exit 0.
- `make census-regenerate`: exit 0, first +5/-0 then +4/-0. Diff contains exactly
  the nine new class-oracle names. Intent audit and `git diff --check`: exit 0.

## Bounds and preserved bytes

This is a conservative lexical dependency oracle. It operates on repository
relative literal paths, current declarations and existing regular files beneath
src/test/docs/resources, refusing escaped real paths and test source over 8 MiB
before full content parsing. Directory literals plus scan vocabulary can select
extra tests: 19,356 data edges and 5,292 source-scan edges in the second snapshot
are explicitly visible, not a claim of precise runtime I/O tracing. Roots computed
without a literal anchor, external/classpath resolution, removed namespace
reconstruction, and dynamic requires remain outside the contract. Missing files
cannot contribute current-snapshot content edges. No performance claim follows.

The two protected observation files remain byte-identical to the start:

```text
c7fb402d7580ace47675d02f7b813bcd5837bdfab5788c0c78f477721f72b0d9  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
```

Historical scratch worktrees are removed after retaining their selection evidence.
All temporary work was confined to `/var/tmp/forge/impact-fx`.


## Round 2

Recorded 2026-09-14T10:19:09.340977+00:00. Starting snapshot `c6f407872f9ca19e87fa7660714660a705f1c44a`;
branch `fable/impact-entrance`. This section supersedes Round 1's exit-zero
contract for unmatched changes. Sol's full [NO-GO verdict](2026-09-14-diff-impact-edges-round2/sol-verdict-1.md)
was read before work. All phases were approved in advance by the round brief.

### Contract and red-first evidence

Chose **(b) HOLD**. Every non-list mode (`before`, `after`, `merged`,
`fixed-point`) now writes and prints `:status :hold-unmatched-files`,
`:reason :unmatched-files`, and every unmatched file with its reason, then exits
**1 before launching any test**, including when other changed files select tests.
List is selection evidence only and exits 0 with the same typed inventory.

A conservative fallback's wall is **unknown, not measured**. The existing runner
starts separate JVMs across all selected lanes; a safe full-manifest fallback
would include the battery lane, which this round explicitly excludes. There is
no established bounded minimum-lane wall for this gate. HOLD closes the failure
without asserting an unmeasured fallback budget or silently choosing a narrower
suite. No `make test` or `test-battery` was run.

`:nothing-selected` is now possible only for an empty diff. The tracked
[no-test-can-depend allowlist](../intent/diff-impact/no-test-can-depend.edn) is
explicitly empty, with its own named test: no path is exempt, including README,
observations, or the allowlist itself. It is a policy inventory, not an editable
runtime configuration granting exemptions. Adding an exemption requires a new
implementation, intent and witnesses.

**Red was committed first as `2c8ea3c6`. DIFF-IMPACT-004 and its two existing
witness tests were amended in that same commit, only by strengthening.** The
three changed assertions now demand HOLD/status and non-list exit 1 for unmatched
changes; all changed-file, per-file-reason, empty-vector, and empty-diff exit-zero
assertions remain. No existing assertion was deleted or weakened. All other
original test assertions remain unchanged. The red commit also contains the
full captured output: **14 tests / 205 assertions / 97 failures / 0 errors**.
[Red log](2026-09-14-diff-impact-edges-round2/red.log).

The class oracle quantifies all six changed-file inventories in all four
non-list modes with endpoints disconnected, then tests the three residual
unmatched fixtures unchanged in all four modes. Mixed selected/unmatched diffs
also HOLD in every mode. The source-closure witness covers transitive paths,
cycles, unrelated endpoints, cut paths and unreachable dependents. During
implementation review, an extra red assertion caught an isolated namespace
that still had an unreachable content reader; its [red output](2026-09-14-diff-impact-edges-round2/content-dependent-red.log)
is retained. A dependent through either require or content edges now yields
`:no-dependency-edge` when unreachable. `:no-test-dependent` remains only for
an isolated namespace seed with neither kind of dependent.

### Bounded propagation and residuals

Discovery now reads content facts from both src/ and test/. The same reverse
require fixed point carries file edges to reachable tests; it does not attach
every source constant or scanner to every test. A literal can name any existing
repository file, with witnesses for `Makefile` and `config/settings.edn` as well
as resources. Git supplies tracked and unignored untracked paths, with existing
regular-file and inside-root realpath checks. Directory expansion remains bounded
to literal docs/resources/src roots and the existing inventory; source-scan
vocabulary and normalization rules are unchanged.

EDN configuration value flow and relative `io/resource` classpath names remain
unresolved. Deleted inputs and dynamically constructed paths with no literal
anchor also remain outside this analysis. Their unmatched changed files HOLD.
This is a bounded static dependency approximation, not a completeness claim for
all dynamic dependencies when a file already has another matched edge. Intent:
[design](../intent/diff-impact/design.md), [DIFF-IMPACT-004/006](../intent/diff-impact/diff-impact-specs.md).

### Six pressure-point results

The supplied verdict's command is literally
`bb --classpath src:test:libs/clj-splice/src -e '<five run-fixture cases>'`.
It contains **no five case bodies**. Therefore these are faithful, explicitly
retained reconstructions of the five reported shapes, plus Makefile; byte-for-byte
replay of Sol's omitted bodies cannot be claimed. The reusable fixture data lives
in `round-two-probes` in test/clj_surgeon/diff_impact_test.clj. Exact executed
[probe driver](2026-09-14-diff-impact-edges-round2/probes.clj) and
[results](2026-09-14-diff-impact-edges-round2/probes.edn) are retained. Invocation:

```sh
TMPDIR=/var/tmp/forge/impact-fx \
JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/impact-fx' \
bb --classpath src:test:libs/clj-splice/src /var/tmp/forge/impact-fx/round2-probes.clj
```

Each result below is pasted verbatim. `:fixed-point nil` means a selected-set
probe only; its synthetic namespace was not run. The three unmatched fixtures
were also run in fixed-point mode and each exited 1. `:wall-ms` covers each
list invocation plus its fixed-point invocation when present, not a fallback wall.

```clojure
{:probe :src-constant, :list-exit 0, :selection {:status :selected, :changed-files ["resources/registry.edn"], :unmatched-files [], :namespaces [{:file "test/fixture/reader_test.clj", :namespace fixture.reader-test, :lane nil, :test? true, :requires #{fixture.middle}, :paths [["fixture.middle" "fixture.helper"]], :reasons [{:file "resources/registry.edn", :edge-kind :data-file, :seed fixture.helper}]}]}, :fixed-point nil, :wall-ms 142}
```

```clojure
{:probe :edn-config, :list-exit 0, :selection {:status :hold-unmatched-files, :reason :unmatched-files, :changed-files ["resources/registry.edn"], :unmatched-files [{:file "resources/registry.edn", :reason :no-dependency-edge}], :namespaces []}, :fixed-point {:exit 1, :results {:status :hold-unmatched-files, :reason :unmatched-files, :changed-files ["resources/registry.edn"], :unmatched-files [{:file "resources/registry.edn", :reason :no-dependency-edge}], :namespaces []}}, :wall-ms 297}
```

```clojure
{:probe :io-resource, :list-exit 0, :selection {:status :hold-unmatched-files, :reason :unmatched-files, :changed-files ["resources/registry.edn"], :unmatched-files [{:file "resources/registry.edn", :reason :no-dependency-edge}], :namespaces []}, :fixed-point {:exit 1, :results {:status :hold-unmatched-files, :reason :unmatched-files, :changed-files ["resources/registry.edn"], :unmatched-files [{:file "resources/registry.edn", :reason :no-dependency-edge}], :namespaces []}}, :wall-ms 255}
```

```clojure
{:probe :test-scan, :list-exit 0, :selection {:status :selected, :changed-files ["src/fixture/unrequired.clj"], :unmatched-files [], :namespaces [{:file "test/fixture/reader_test.clj", :namespace fixture.reader-test, :lane nil, :test? true, :requires #{fixture.middle}, :paths [["fixture.middle" "fixture.helper"]], :reasons [{:file "src/fixture/unrequired.clj", :edge-kind :source-scan, :seed fixture.helper}]}]}, :fixed-point nil, :wall-ms 135}
```

```clojure
{:probe :src-scan, :list-exit 0, :selection {:status :selected, :changed-files ["src/fixture/unrequired.clj"], :unmatched-files [], :namespaces [{:file "test/fixture/reader_test.clj", :namespace fixture.reader-test, :lane nil, :test? true, :requires #{fixture.middle}, :paths [["fixture.middle" "fixture.helper"]], :reasons [{:file "src/fixture/unrequired.clj", :edge-kind :source-scan, :seed fixture.helper}]}]}, :fixed-point nil, :wall-ms 140}
```

```clojure
{:probe :makefile, :list-exit 0, :selection {:status :hold-unmatched-files, :reason :unmatched-files, :changed-files ["Makefile"], :unmatched-files [{:file "Makefile", :reason :no-dependency-edge}], :namespaces []}, :fixed-point {:exit 1, :results {:status :hold-unmatched-files, :reason :unmatched-files, :changed-files ["Makefile"], :unmatched-files [{:file "Makefile", :reason :no-dependency-edge}], :namespaces []}}, :wall-ms 267}
```

### Historical list-mode replay

Both replays used detached snapshots beneath /var/tmp/forge/impact-fx, the current
selector on the classpath, and the same library-mode harness as Round 1, replacing
only `gate-environment?`. They make no launcher-envelope or historical test-run
claim. Exact bases: `df0c9e1c^` at `df0c9e1c`, and `00566756` at `8aedb65e`.

- **df0c9e1c: 72 → 92 namespaces**, no removals, list exit 0, unmatched empty.
  All 20 additions have source-scan reasons seeded in required src namespaces.
  Existing literal `src` roots in core, extract, failure-report, mcp-intent-contract,
  mcp-recovery, mcp-source-anchor and rename explain the added selections.
  Removing only src-side content edges restores **exactly the original set of 72**;
  the [counterfactual](2026-09-14-diff-impact-edges-round2/attribution.edn) proves the
  delta is propagation, not widened Git inventory. Broad literal roots remain
  deliberately conservative. Splice-envelope-test remains selected.
- **00566756..8aedb65e: 9 → 9 namespaces**, identical set, no additions or removals,
  list exit 0, unmatched empty. All three previously named battery witnesses remain.

The 20 added namespaces at df0c9e1c:

```text
clj-surgeon.cljc.split-test
clj-surgeon.edit-test
clj-surgeon.failure-report-test
clj-surgeon.help-test
clj-surgeon.intent-transaction-test
clj-surgeon.mcp-contract-test
clj-surgeon.mcp-extraction-test
clj-surgeon.mcp-inspect-contract-test
clj-surgeon.mcp-intent-contract-test
clj-surgeon.mcp-prepared-wire-test
clj-surgeon.mcp-read-request-normalization-test
clj-surgeon.mcp-relation-census-launcher-test
clj-surgeon.mcp-relation-census-round20-test
clj-surgeon.operation-algebra-test
clj-surgeon.quoted-var-refs-test
clj-surgeon.recovery-test
clj-surgeon.require-change-boundary-test
clj-surgeon.require-change-test
clj-surgeon.show-form-test
clj-surgeon.xray-test
```

Full selections, paths and reasons: [df0c9e1c EDN](2026-09-14-diff-impact-edges-round2/real-a.edn),
[stdout](2026-09-14-diff-impact-edges-round2/real-a.log),
[tooling EDN](2026-09-14-diff-impact-edges-round2/real-b.edn),
[stdout](2026-09-14-diff-impact-edges-round2/real-b.log),
[set comparison](2026-09-14-diff-impact-edges-round2/compare.edn).

### Verification

- Formatter: standard-clojure-style v0.29.0 on the three changed Clojure files.
- Lint through `~/bin/clj-kondo`: **0 errors, 0 warnings**.
- JVM, `-J-Xmx1024m -M:clj-surgeon/test-deps`, diff-impact-test and lane-manifest-test:
  **49 tests / 2,177 assertions, 0 failures/errors, 12.86 s wall**.
- bb, `--classpath src:test:libs/clj-splice/src`, diff-impact-test:
  **14 tests / 242 assertions, 0 failures/errors, 7.75 s wall**.
- Original `test/diff_impact.clj --self-test`: exit 0; require graph and completion
  assertions remain intact.
- `make census-regenerate`: **exit 0, +5/-0**; only the five new test names appear.
- Repository intent audit: `:ok true`, no violations (existing witness-debt ledger
  remains reported separately).
- All scratch/process temp properties were set beneath `/var/tmp/forge/impact-fx`.
  Historical worktrees were removed after retaining their selection evidence.
- Protected ledger hashes remain byte-identical to the starting snapshot:

```text
c7fb402d7580ace47675d02f7b813bcd5837bdfab5788c0c78f477721f72b0d9  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
```

Logs: [JVM](2026-09-14-diff-impact-edges-round2/jvm-final.log),
[bb](2026-09-14-diff-impact-edges-round2/bb-final.log),
[lint](2026-09-14-diff-impact-edges-round2/lint-final.log),
[census](2026-09-14-diff-impact-edges-round2/census.log).
No full-suite, fallback-performance, or independent fence GO claim is made.
