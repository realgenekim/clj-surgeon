# Astra: explicit receipt fixture repair

Base 1f47c694. The running merged battery exposed two older injected-executor
fixtures that omitted the newly required receipt destination. They refused before
reaching their phase/usage assertions. The production requirement is unchanged.

Each invocation now creates a unique temporary artifact parent, passes it
explicitly and removes it in finally. Artifact-directory creation is real; the
old directory stubs are removed. Existing source-write, transport and proof
injections and all original assertions remain intact. No added test or namespace.

Actual route: installed Babashka Surgeon :cat selected the two owning forms;
:change! then applied two owner-scoped exact-source replacements in one guarded
transaction. First attempt succeeded, with two read-back hashes and an inverse
receipt. No provider generation. Request/result/receipt are retained under
/var/tmp/forge/receipt-fixture-repair-artifacts/. The command used structured EDN
from a file, avoiding shell quoting of source. Standard formatting followed.

The useful property was exact two-owner/two-file cardinality with read-back and
an undo receipt. This was not a measured win against native editing. The shell
batch containing request preparation and change! reported 0.77 s, which is NOT
an isolated edit-tool timer. Worktree creation at 05:41:02.835Z to final focused
verification at 05:44:21Z was about 198 s; that bounded interval includes source
orientation, preparation, formatting and gates, but excludes the initial task
handoff. No full-task stopwatch or native control was run.

Final focused gate: 9 tests, 48 assertions, zero failures/errors; lint clean.
Both namespaces ran nice 10 through suite-run with NODE_DISABLE_COMPILE_CACHE=1
and isolated events. The tests retain their same count and assert the intended
phases/usage instead of bypassing destination admission. Gate log:
/var/tmp/forge/receipt-fixture-repair-artifacts/final-gate.log.

At this receipt's writing, the parent battery was still in reader-eval-fence;
its only reported failing namespaces were these two. This focused result does
not claim the parent battery passed. Parent root/worktree was not modified.
