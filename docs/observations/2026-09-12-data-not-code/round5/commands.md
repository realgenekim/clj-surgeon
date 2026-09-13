# Reproduction entrances

All commands run from /home/forge/src/clj-surgeon-datacode. Shell environment:
TMPDIR=/var/tmp/forge/datacode-fx and
JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/datacode-fx'.
The owned nREPL was started by make nrepl MCP_JAVA_OPTS='-J-Xms64m -J-Xmx1024m'
inside flock /home/forge/tmp/suite-1.lock; it printed port 40129. The first
focused evaluation asserted user.dir exactly equalled the worktree. No other
seat's JVM or reserved service port was used.

- red.log: require lane-manifest-test and receipt-artifacts-boundary-test, then
  clojure.test/test-vars on runtime-evidence-binds-statistics-to-receipt-files,
  destination-envelope-policy-witness-assertion-count and
  destination-envelope-refuses-hard-linked-final-ledger, before implementation.
- green-initial.log and green.log retain two delimiter errors from initial F1
  reloads; no green claim follows from those files. green-final.log reloads the
  changed source/test namespaces in one do and runs the same three witnesses
  with report counters, after the pure field matrix was added.
- focused.log: clj-nrepl-eval --port 40129 --timeout 600000
  '(load-file "/var/tmp/forge/datacode-fx/round5-focused.clj")'. Exact file bytes
  are retained here as focused.clj with an explicit clojure.test require added
  for standalone lint (the warm JVM already had it loaded). It requests census regeneration
  through the existing environment seam, then executes both whole namespaces.
  Ledger diff is three additions, no removals.
- policy-rename-red.log: clj-nrepl-eval --port 40129 --timeout 600000
  '(load-file "docs/observations/2026-09-12-data-not-code/round5/policy-rename-plant.clj")'.
  The plant walks actual source forms, renames both definition and default's
  call, unmaps the old Var, and restores production source in finally.
- focused-bb.log: bb -Xmx1g -cp src:test:libs/clj-splice/src -e
  '(require (quote clj-surgeon.lane-manifest-test)) (let [r
  (clojure.test/run-tests (quote clj-surgeon.lane-manifest-test))] (prn r)
  (System/exit (+ (:fail r) (:error r))))'.
- Formatting: standard-clj fix followed by the four paths in lint.sh.
- Lint: bash docs/observations/2026-09-12-data-not-code/round5/lint.sh.
  Both base and current use the same ~/bin/clj-kondo, invocation, worktree
  configuration and four source/test files. No blanket whole-tree lint claim.
- Gate: stop owned nREPL with (System/exit 0), then bash
  docs/observations/2026-09-12-data-not-code/round5/run-gates.sh. The script
  reacquires the same host flock, refuses a separate lease marker, and records
  monotonic wall/exit/argv. No repeated test-fast or overwritten failed log.
- Preregistration checks: Python ast.parse(measure.py), sha256 of every patch
  and source, and git apply --check on all six patches. These are static
  checks only; no init/cell invocation and no experiment arm ran.

Source-edit dogfood: all four changed Clojure files used native discovery and
batched patches under the current routing plate. No routed Surgeon mutation,
no direct cclsp/clojure-lsp client, no skill installation. Two F1 delimiter
repairs are retained as development friction, not erased successful attempts.
The user requested commit last; red and green evidence are retained together
in that final commit rather than making intermediate red commits.

After the final gate, the original Opus evidence probe was replayed verbatim:
bb -Xmx1g -cp src:test:libs/clj-splice/src -e
'(load-file "/var/tmp/forge/datacode-fx/probe/p3.clj")
(try (v forged) (catch clojure.lang.ExceptionInfo e (prn (ex-data e))))'.
redteam-p3-replay.log records its exact forged row and named field refusal,
missing-file refusal, and independent 456-receipt consistency check.
This is an evidence-validator replay, not a probe/native timing experiment.
