# Kaocha plugins plan — 2026-09-08

Timebox: three hours from session start; publish an interim report at two hours.
Own repo: /home/forge/src/kaocha-sublime. Native Clojure edits; no Surgeon route.
Commit as forge-anvil <forge-anvil@anvil>, with
Co-Authored-By: Gene Kim <genek@itrevolution.com>.

## Source contract (read before code)
Pinned Kaocha 1.91.1392 jar, extracted under the owned fixture root.
kaocha/plugin.clj names cli-options, config, pre-load, post-load, pre-run,
pre-test, post-test, post-run, wrap-run, pre-report, post-summary, main.
kaocha/api.clj: config -> pre-load -> post-load -> pre-run -> tests -> post-run
-> reporter summary. wrap-run wraps individual testables, NOT api/run.
kaocha/watch.clj: internal pre-load reloads first; config sees original tracker.
watch's try-run calls post-summary then prints a blank line; run-loop can print
status/start another run. A plugin cannot own arbitrary later stdout.
kaocha/repl.clj: run reduces results to totals, discarding extra result keys;
post-summary executes outside api/run's plugin binding in this path.
kaocha/plugin/capture_output.cljc binds captured output to leaf testables.
Sources: installed upstream jar (the actual executable source), API/plugin/watch/
repl/result/capture-output files; no speculative hook names.

## Run receipt
config: defaults dir, optional include-tests?, install version-checked REPL adapter.
pre-load: allocate fresh run state, UUID+UTC id (or KAOCHA_RUN_ID), UTC start,
monotonic timer, hash normalized config, git HEAD, JVM pid and versions.
Exclusively create <absolute-dir>/<id>.edn.tmp; reject unsafe/duplicate ids.
A fixed env id is single-use; reruns with reused id refuse rather than overwrite.
Wrap the configured reporter before Kaocha resolves it. After its summary prints,
print RUN-RECEIPT <absolute-path> and flush, only if post-run committed a receipt.
post-run: read result totals and testable event history; serialize complete EDN;
write tmp then Files.move ATOMIC_MOVE on same filesystem (no non-atomic fallback).
Return :kaocha.plugin.run-receipt/path in full result.
Small idempotent kaocha.repl/run adapter binds a per-invocation path cell and adds
that path to the returned summary; document/version-check this upstream gap.
No pre-test/post-test/wrap-run hooks needed: built-in capture and events suffice.
File: one EDN map, :run-id string, :started/:finished RFC3339 UTC strings,
:wall-ms number, :totals {:tests :assertions :pass :fail :error :pending} integers,
:failures vector of {:ns :var :message :expected :actual :file :line},
expected/actual as readable strings (exceptions as data/string, never #object),
:config-hash SHA256 hex, optional :git-sha, :jvm-pid integer,
:kaocha-version and :plugin-version strings. Optional :tests vector contains
:id, :outcome and :stdout (built-in capture output, possibly includes stderr).
Incomplete tmp is an EDN map with :run-id/:started/:status :running.
Failure policy: IO/collision/atomic-move errors fail loudly, no complete marker;
SIGKILL leaves tmp; no complete receipt for load/runner abort before post-run.
Limitation: the marker delimits a completed run, not subsequent watch lifecycle
messages or background-thread output; isolated JVMs are required for concurrency.

## Affected selection
cli-options: --affected-since SHA|:tracker. config reads CLI or namespaced config.
config snapshots files before watch reload, reads :kaocha.watch/tracker and its
:lambdaisland.tools.namespace.track/deps plus file/filemap and load/unload.
Explicit SHA uses git diff --name-only -z plus untracked files (safe argv).
Explicit :tracker with no usable tracker -> loud full fallback.
Watch compares a content snapshot carried inside the returned tracker; initial
run unknown -> full. Snapshot covers source/test roots, resources, deps.edn,
tests.edn; all detected non-namespace edits -> full. No process-global cache.
pre-load preserves snapshot in tracker after Kaocha reload; post-load/pre-run
select namespace groups from graph transitive dependents plus changed ns.
Unknown files/namespaces, deleted/renamed files, graph errors, missing coverage,
reload errors, unsupported test types -> full with a concrete reason.
A fallback clears watch's transient failed-test focus before loading/filtering.
A selected run retains configured user filters; never claims wider coverage.
Output: AFFECTED n/total namespaces from k changed files; fallback also prints
AFFECTED FULL: reason. Result carries namespaced selection data (files, selected,
closure, reason) for machine verification and receipt context.
No standalone selection file; selection data can be included in the receipt.
Limitation: static namespace dependencies do not describe runtime/dynamic coupling;
unknown or non-namespace input falls back, but undeclared dynamic edges need users
to disable narrowing or add explicit dependencies. Upstream watcher event blindness
is not repaired; benchmark uses the original in-place edit helper.

## Red-first witnesses and measurements
Commit plan + failing integration fixtures before implementation; small green
commits for receipt, selection, and specimen verification/docs.
Synthetic subprocess fixtures: pass/fail/error/pending and distinct test output;
compare reporter totals to parsed EDN; verify last run line and EDN round-trip.
Kill exact JVM pid while fixture blocks: tmp exists, edn absent after SIGKILL.
Two subprocesses in separate workspaces: unique ids, independent receipts/output.
Warm kaocha.repl/run twice: two paths, correct summaries; watch reruns -> files.
Synthetic dependency graph: leaf/src/deps/unknown/deleted and empty selection.
Read-only source repos; detached disposable worktrees from nrepl/test-alias at
/var/tmp/forge/kaocha-sublime-fx/{mvr,ccfp}; edits only in disposable fixtures.
Respect STORE_BACKEND=jsonl and mvr data dir parity; no network service/REPL ports.
One JVM per worktree, exact-pid termination; no /tmp; lint ~/bin/clj-kondo.
Specimen full counts: mvr 579; ccfp 1021. Inspect any drift, never normalize away.
ccfp witnesses: changed leaf test; version source dependents exclude unrelated;
deps.edn loud full; ReviewWriteProof field edit yields 148 namespace reload closure.
Reuse /var/tmp/forge/kaocha-watch-fx/watchbench.sh stopwatch method and inplace.py;
copy harness into owned evidence with only output paths/launch/cleanup adapted.
Ten red/green iterations each; raw TSV, median/p95 nearest-rank, original baseline
TSVs retained. Measure full fallback cost separately (save -> reporter verdict).
Baseline raw vs captain headline/idle estimates remain distinct in report.
Retain logs/receipts/counts before removing both detached worktrees.

## Deliverable
/var/tmp/forge/plan2/cellC/astra-kaocha-plugins-report.md: plan, commit SHAs,
before/after table (red median/p95 and green), exact receipt, fixture counts,
limitations, release remainder. Repo README includes local/root deps coordinates,
tests.edn enablement and coldstart boot line naming the returned receipt path.
No remote creation or push (Gene owns the remote after first report).

Implementation refinements from executable witnesses:
- REPL adapter wraps result/totals and preserves paths on tagged suite results;
  it also works when the plugin is first loaded from inside repl/run.
- Watch automatically runs again after focused green. post-run carries a one-use
  closure in the tracker; only this immediate unchanged follow-up reuses it.
  A subsequent manual run still falls back full. The initial lost arm is retained.
- Recheck destination after exclusive tmp reservation to close fixed-id races.
- Specimen watchers do not signal root deps.edn saves. Prove deps fallback via
  the explicit SHA CLI; use an existing watched resource for full-cost timing.
  Snapshot sees root config changes on the next event; no event is fabricated.
