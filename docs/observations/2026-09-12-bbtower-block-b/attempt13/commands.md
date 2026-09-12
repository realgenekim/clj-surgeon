# Reproduction and evidence

Unless explicitly noted, commands used:

```sh
TMPDIR=/var/tmp/forge/bbtower-fx
JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx'
```

Every direct JVM invocation added `-J-Xmx1g`; the gate keeps its own existing
bounded child heap policy. No `_JAVA_OPTIONS`, outer timeout, shared service,
push, tag, or shared installation command was used. One suite at a time was
launched; the repository coordinator owns its bounded internal lanes.

* `red.log`: cold `clojure -J-Xmx1g -M:clj-surgeon/test-deps -e ...`, requiring
  cli-dispatch-test and mission-typist-executor-test and invoking their new
  `cli-child-honours-tmpdir-at-startup` and
  `formatter-refusal-retains-native-diagnostics` Vars via `clojure.test/test-vars`.
  The red bb-command implementation returned its argv unchanged. Three failing
  assertions: child property `/tmp`, discarded native error, discarded receipt.
* `formatter-red.log`: require mcp-formatter-test, load the byte-exact
  `original-formatter.clj` from `git show b45d3eb1:src/clj_surgeon/mcp_formatter.clj`,
  then test `formatter-staging-honours-selected-root-and-cleans-up-exceptions`.
  It fails the selected-parent assertion and throws the uncaught native error.
* `green.log`: run mcp-formatter-test and mcp-process-test plus the two red
  boundary Vars with bound counters and explicit fail/error exit status.
* `formatter-path.log`: injected runner prints the exact staged filename and
  TMPDIR, then returns exit zero. The file is in the selected root.
* `readonly-jvm-tmp.log`: real `format-replacements!` with
  `JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/sys'`, TMPDIR still the allowed
  root. This is the requested Landlock-free read-only-temp stand-in, not a
  Landlock claim. Real formatter succeeds.
* `focused.sh`: exact executable command for the three previously red
  namespaces, fresh empty TMPDIR, before/after listings, result and cleanup.
  Run once. The immediate names-only difference is `tmp-new.txt`; the later
  read-only follow-up difference is `tmp-new-followup.txt`. Neither snapshot
  proves absence of create/delete activity between endpoints.
  The five large endpoint listings were losslessly archived with `gzip -n`;
  use `gzip -dc` on their `.txt.gz` files. The script produces the original
  plain-text names, and both set-difference files remain uncompressed.
* `launcher-check.py`: execute generated stable and development launchers in a
  scratch installation with all destination/root variables overridden. Check
  exact argv across eight env cases and run both real CLI `--version` paths.
  The scratch installation is deleted; shared ~/bin is untouched.
* `entrances.py`: compile the actual Python bb helper and Popen adapter AST,
  substitute only the unrelated provider guard, and execute real property-only
  bb children for five cases. Also check shell syntax and invoke mission help.
* `boundaries.log`: run mission-git-boundary-test, split-proof-gate-boundary-test,
  mcp-process-test and mcp-formatter-test on the cold JVM after the final
  subprocess changes. Explicit exit is fail+error.
* `format.log`, `format-split.log`: standard-clojure-style `fix` on changed
  Clojure files. Unrelated pre-existing formatting churn was restored.
* `lint.log`: initial lint found an existing qualified process call without an
  explicit require in typist tests. `lint-final.log` and `lint-split.log`:
  `~/bin/clj-kondo --lint` on all changed Clojure files, zero warnings/errors.
* `census.log`: `CENSUS_REGENERATE=1 bb -Xmx1g
  -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx -e ...` requiring lane-manifest-test
  and running `the-corpus-only-ever-grows-and-the-arithmetic-is-shown`.
  The reviewed diff adds exactly four witness names, removes none.
* `test-fast.log`: `make test-fast`, exactly once, exit in `test-fast.exit`.
* `gate-command.txt`: actual clock timestamp, source SHA and exact prewarm
  command. `gate.md` is its complete verbatim stdout/stderr; `gate.exit` is the
  process exit. No summarized line replaces the raw gate transcript.

The first prewarm transcript/exit/command are retained separately as
`gate-first.md`, `gate-first.exit`, `gate-first-command.txt`. It failed only
the existing CLI compact-receipt comparison, which included the live working
checkout's growing workspace-status evidence. The owned-fixture repair ran
one bb test Var with bound counters (`repair.log`: 1 test, 30 passes), followed
by formatter/lint (`format-repair.log`, `lint-repair.log`). Every original
assertion remains, including the compactness comparison. The final prewarm
is the second and last attempt; test-fast was not repeated.
