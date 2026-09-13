# Entrances and subjects

Started by `git checkout bb-rewrite-tower-local`; verified clean bff549ad.
No base coordinator comparison was requested by attempt23, so the base checkout
was not used or recreated. Attempt1 premises were read from the block-B root
REPORT.md and probe-contract.md (there is no attempt1/ subdirectory).

All JVM/test commands ran with TMPDIR=/var/tmp/forge/bbtower-fx,
JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx', and
_JAVA_OPTIONS unset. make nrepl used its explicit 512 MiB maximum, discovered
only the owned worktree's ephemeral port 45919, and verified user.dir before
requiring tests. It was stopped via System/exit before the full suites.
No protected shared port or shared ~/bin install was touched.

- `clj-nrepl-eval --discover-ports`, then `make nrepl`: owned warm JVM.
- JVM red and subsequent focused reloads: clj-nrepl-eval on 45919, requiring and
  running cljc.merge-test, cljc.split-test and worktree-lifecycle-prune-test.
- Recovery diagnosis: bb invoked the actual release-file-lock! Var with a real
  temporary FileChannel lock; refusal message in recovery-lock-probe.log.
- `bb attempt23/run_controls.clj [namespace ...]`: serial fresh complete
  controls; every exact child argv and subject in controls/*.command.edn.
  The first prune pair is preserved in prune-before-git-admission/. The final
  six are controls/*.control.edn, with status and observed process exit.
- `bb attempt23/fold_census.clj`: generates census.edn, portability-census.md
  and census-summary.log using the manifest's frozen/fresh control paths.
- Focused manifest checks on JVM and bb: run the Vars
  runtime-portability-controls-cover-every-assignment and
  every-manifest-entry-exists-on-disk with explicit report counters. Both
  runtime-rule logs end with 2 tests, 1028 passing assertions, no failures/errors.
- Standard Clojure Style fix on changed source and driver files; formatter logs
  retain the changed/unchanged file results. Lint only through ~/bin/clj-kondo.
- `bash attempt23/run_gate.sh test-fast fast`
- `bash attempt23/run_gate.sh test-battery battery`
- `bash attempt23/run_gate.sh landing-gate-prewarm prewarm`

Here attempt23/ abbreviates
docs/observations/2026-09-12-bbtower-block-b/attempt23/. The gate wrapper records
the exact HEAD, wall-clock start, process exit and elapsed seconds. Each named
suite starts only after the previous one finishes. No outer timeout is used.
