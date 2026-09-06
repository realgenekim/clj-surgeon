# Connect your existing tests to patch admission

Development MCP integration guide. This documents the existing contract; it does not supply a generic test runner.

`admit_clojure_patch` can verify a native-authored patch using your project's
trusted test runner. A passing command against the live checkout is insufficient:
the runner must test the proposed snapshot and write actual test results to the
provided report path. Normal commit requires complete verification; `verify=none` is
for preview, not a shortcut to commit.

## Configure the existing runner

The repository profile is `.clj-surgeon/focused-test.edn`. Repository values
override server `:focused-test` configuration separately for `:command`,
`:timeout-ms`, and `:namespaces`. If a server already supplies a suitable command,
the repository can supply only its real source-to-suite mapping. Do not create
a replacement command or invent a mapping merely to get a passing receipt.

The [profile template](../examples/focused-test-profile/focused-test.edn.example) is not an installed executable. Replace its
runner path and mapping with actual repository-owned equivalents. Its command
is argv, not shell text: spaces in paths do not require shell quoting.

The runner starts with the live project root as cwd. Literal argv elements
`{snapshot}` and `{report}` are required; `{namespaces}` expands into separate
suite arguments. Do not embed these placeholders inside another argument.

## Bind tests to the candidate

The snapshot contains changed, nondeleted file post-images, NOT a full checkout.
An existing-file-only integration can copy a validated unchanged source/test/
resource closure into a private directory, then overlay captured candidate bytes
last. Its classpath must resolve that overlay instead of live source files. Keep
unchanged tests and dependencies bound to the intended project; a changed test
must not silently certify itself. Reject unsupported changes before execution.

The demonstrated example supports exactly one existing source file. It does not
support deletions, new files, symlinks, arbitrary project reconstruction, or
dynamic dependency resolution. The ABI provides no deletion tombstone to this
runner; do not promise deletion coverage by overlaying live files.

Run the actual selected suites and write an EDN namespace map to the supplied
report path, with numeric `:tests`, `:failures`, and `:errors` per namespace.
For clojure.test, these come from each actual `run-tests` result's `:test`,
`:fail`, and `:error` values. Assertion failures need not equal failed test count.
Do not hardcode counts or parse a printed success sentence. A load failure,
test failure, timeout, or cleanup failure must not yield a successful runner exit.
Retain bounded failure details locally; reports and diagnostic stdout serve
different purposes. This is trusted project verification, not a hostile-code sandbox.

## Validate the integration once before reuse

Use fresh isolated fixtures and the same real suites:

1. Correct candidate with a deliberately broken live target must pass.
2. Broken candidate with a correct live target must fail actual assertions.
3. Unsupported extra source, symlink, and unrelated source drift must refuse.
4. Exercise public admission too: direct runner success alone misses integration
   bookkeeping and does not prove a verified commit.

Inspect the named suites, real counts, lint evidence, verification completeness,
and committed status in the public receipt. Do not substitute a prepared profile
for that evidence. Charge initial integration and repairs separately from reuse.

## What was demonstrated

The restricted Maven example used the unchanged `maven.recording-query-test`
suite and a private candidate-last overlay. Both candidate/live polarity checks
worked. First public admission refused on two gate-created bookkeeping files;
after an exact two-path repair, the same patch committed with two tests and clean
lint. This demonstrates the ABI, not a portable Maven installation or a native
speedup. The first-call failure remains part of the result.

The [actual utility report](observations/2026-09-06-astra-real-profile-utility.md) supersedes the experiment README's historical preparation labels:
all five v3 invalid-input preflights refused, and the public retry completed.
The same profile subsequently admitted a second real-source clarity refactor on
its first call: share timing-pair conversion between the two existing consumers.
These are observed restricted-fixture uses, not generic runner validation. Preserve
the original preparation records and failed receipt; the utility report links
the subsequent receipts and accounting.

The experiment's absolute paths, 66 pinned cached jars, Linux process controls,
and shared-box scheduler are not installation instructions. Consult your existing
runner owner when these contracts are not already met; there is no universal
`clojure -M:test` substitution that proves snapshot binding.
