# Kaocha plugins report — 2026-09-08

Both plugins are implemented and locally verified. Completed 2026-09-08T17:08:03.378392+00:00, within the
three-hour timebox (under one hour). No remote was created or pushed.

Plan, written before code: [/var/tmp/forge/plan2/cellC/astra-kaocha-plugins-plan.md](/var/tmp/forge/plan2/cellC/astra-kaocha-plugins-plan.md) (113 lines after documented refinements).
Repo: `/home/forge/src/kaocha-sublime`; HEAD `f087e1e`; working tree clean.
All commits use forge-anvil <forge-anvil@anvil> and the Gene Kim trailer.

## Commits

be6fbab — test: record hook plan and red plugin witnesses
591e9ad — feat: add atomic run receipts and conservative namespace selection
c72bc3c — fix: retain affected selection through automatic green follow-up
4fcb459 — fix: preserve complete EDN under REPL printers and custom failures
a0ffb67 — fix: make config hashes independent of namespace printer settings
f087e1e — docs: record specimen measurements, receipts, setup and release boundaries

## Measured before / after

| specimen | mode | baseline red median / p95 (s) | plugin red median / p95 (s) | baseline green median / p95 (s) | plugin green median / p95 (s) |
|---|---|---:|---:|---:|---:|
| mvr | watch save → verdict | 12.4645 / 13.2830 | 0.0580 / 0.5590 | 0.0395 / 5.8670 | 0.0595 / 0.0790 |
| ccfp | watch save → verdict | 127.6800 / 130.4500 | 0.1585 / 0.2400 | 0.1465 / 0.2660 | 0.1565 / 0.2260 |

Ten iterations per color/specimen, original in-place/fsync save helper and
watchbench.sh stopwatch method; p95 is nearest rank (maximum at n=10).
No observations trimmed. Baseline mvr iteration 9 reports 13 failures, and its
5.867s green outlier stays included. Raw inputs and recomputation:
[evidence/before-after.md](/home/forge/src/kaocha-sublime/evidence/before-after.md),
[scripts/summarize.py](/home/forge/src/kaocha-sublime/scripts/summarize.py),
[method and retained attempts](/home/forge/src/kaocha-sublime/evidence/method.md).

The captain's ~6.8s / ~64s describe idle full-suite cost; the supplied ten-run
TSVs include saves queued behind Kaocha's automatic full follow-up after green.
The plugin reuses its affected closure once for that automatic follow-up. An
unchanged manual run subsequently falls back full. The baseline and plugin are
historical comparisons on a shared host, not paired simultaneous controls.
The timed implementation is c72bc3c. Later commits harden receipt EDN and hashes;
selection was unchanged. Final local validation covers a0ffb67, and full specimen
counts plus ccfp resource fallback also validated the receipt hardening at 4fcb459.
Warm probe latency (~0.14 / ~0.25s) remains baseline context, not a new measurement.

Full watch fallback cost, change / restore (two observations each):

| specimen | trigger | seconds | executed tests / assertions |
|---|---|---:|---:|
| mvr | watched CSS resource | 7.761 / 7.533 | 579 / 7833 |
| ccfp | watched CSS resource | 68.548 / 67.560 | 1021 / 12393 |

Green runs execute both tests in the changed namespace: mvr 2 / 4 and ccfp 2 / 18.
Baseline focused green executes one failing test / 3 assertions. This is a major
red improvement, not a claim that every green run got faster.

## Witnesses and counts

- Specimen SHAs: mvr `94393708b6312c4de114f4ebf31dc82313e2914e`;
  ccfp `8aec4c93c50d61266fe9de79e889a2fd919f8cef`, both nrepl/test-alias.
- Full suites: mvr **579 tests / 7833 assertions**, ccfp **1021 / 12393**, green.
- ccfp leaf edit: only version-test runs, **2 tests / 18 assertions**.
- ccfp version.clj edit/restoration: **202 namespace groups**, **698 / 8994**;
  **37.917 / 36.340s**. Unrelated blob-durability-test's three tests are excluded.
- ccfp ReviewWriteProof field addition/removal: **149 namespace groups**,
  **663 / 8804**, green in **35.591 / 34.995s**. The historical raw log ALSO has
  149: **148 dependents plus the changed namespace**. Exact namespace sets match,
  with no additions/removals. See [identity set evidence](/home/forge/src/kaocha-sublime/evidence/identity-closure.json).
- Namespace counts describe Kaocha namespace groups. These specimens scan src
  as well as test, so some groups have zero test vars; test counts are separate.
- deps.edn: real ccfp `--affected-since HEAD` loudly chose the full suite and
  passed **1021 / 12393**. A root deps.edn save alone did not generate a watch
  event in the specimen configuration. It is detected on the next watched event;
  no event was fabricated or attributed to that save.
- **74** complete specimen EDNs parsed; **71** watch summaries each have exactly
  one receipt with matching test/assertion totals; **zero** remaining partials.
  All source/identity receipt closures equal Kaocha's actual reload sets.
- Actual SIGKILL of the blocking fixture JVM: **one .tmp, zero .edn**. Two
  simultaneous isolated JVMs sharing a receipt directory: distinct IDs and
  complete independent receipts. Same forced ID: one winner, one loud refusal.
- First lazy plugin load inside `kaocha.repl/run`, then a second warm run:
  both returned maps carry distinct `:kaocha.plugin.run-receipt/path` values.
  Per-test capture, pass/fail/error/pending, extended failure events, arbitrary
  messages, bounded REPL printers and deterministic config hashing are tested.
- Final local suite: **13 tests / 44 assertions, 0 failures/errors**.
  Six actual CLI selection/fallback cases pass. `~/bin/clj-kondo`: zero findings.

[Machine verification](/home/forge/src/kaocha-sublime/evidence/verification.edn),
[process witnesses](/home/forge/src/kaocha-sublime/evidence/process.log),
[raw watch witnesses](/home/forge/src/kaocha-sublime/evidence/watch-witnesses.tsv).
Verify retained artifacts offline with `clojure -M scripts/verify_evidence.clj`.
SHA256 inventory: [evidence/SHA256SUMS](/home/forge/src/kaocha-sublime/evidence/SHA256SUMS).

## Receipt example, verbatim

```clojure
{:started "2026-09-08T16:41:21.102175086Z", :affected {:files ["/var/tmp/forge/kaocha-sublime-fx/mvr/test/marvin_voice_remote/blob_test.clj"], :namespaces [marvin-voice-remote.blob-test], :closure [marvin-voice-remote.blob-test], :total 54, :selected 1, :reason nil}, :finished "2026-09-08T16:41:21.155920402Z", :jvm-pid 1312271, :totals {:tests 2, :assertions 4, :pass 4, :fail 0, :error 0, :pending 0}, :kaocha-version "1.91.1392", :wall-ms 53.823544, :git-sha "94393708b6312c4de114f4ebf31dc82313e2914e", :plugin-version "0.1.0-SNAPSHOT", :config-hash "7c1269caa99f5be935cddbd73b6c36539414f1e30984b4b8c98d5c07f1b93d40", :failures [], :run-id "2026-09-08T16-41-21-102175086Z-978fdd43-262d-4a6f-a116-0e3c2e5e56de"}
```

Retained copy: [receipt-example.edn](/home/forge/src/kaocha-sublime/evidence/receipt-example.edn).
The marker for this run was:

```text
RUN-RECEIPT /var/tmp/forge/kaocha-sublime-fx/mvr/target/kaocha-runs/2026-09-08T16-41-21-102175086Z-978fdd43-262d-4a6f-a116-0e3c2e5e56de.edn
```

## Enablement and release remainder

[README](/home/forge/src/kaocha-sublime/README.md) and [deps.edn](/home/forge/src/kaocha-sublime/deps.edn) are written. Current
working coordinates are local only:

```clojure
:extra-deps {local/kaocha-sublime {:local/root "/home/forge/src/kaocha-sublime"}}
```

Enable in tests.edn:

```clojure
#kaocha/v1
{:plugins [:kaocha.plugin/affected :kaocha.plugin/run-receipt]
 :kaocha.plugin.run-receipt/dir "target/kaocha-runs/"
 :kaocha.plugin.run-receipt/include-tests? true}
```

A coldstart harness should print `TEST-RECEIPT <returned absolute path>` in its
boot block, taking the value directly from the result, never from newest-file
heuristics. For example, this real warm fixture returned the path used here:

```text
TEST-RECEIPT /var/tmp/forge/kaocha-sublime-fx/process/warm/receipts/2026-09-08T16-49-41-499873816Z-c4006709-b434-48a1-8fc5-c72e2991db06.edn
```

Still required for release: Gene creates the remote; choose license and published
Maven/Git coordinates; set a release version and CI; qualify more Kaocha versions
and reporter/plugin combinations; move the REPL result-path adapter into upstream
Kaocha (or an explicitly maintained fork). Coldstart enablement is documented,
not installed into the read-only specimen repositories.

## One limitation per plugin

**run-receipt — upstream lifecycle boundary.** On Kaocha 1.91.1392, the resolved
reporter wrapper puts the marker after the run summary, but watcher lifecycle
messages and other plugins' later output can follow. A universal literal
last-stdout-line guarantee is not implemented. Warm REPL path preservation needs
a narrow, version-checked totals adapter because upstream discards extra keys.
The missing upstream contracts are a terminal after-output hook and preservation
of result metadata through repl/run; no fork was introduced.

**affected — observed static dependencies.** Selection depends on declared
namespace edges and delivered watch events. Undeclared dynamic coupling and
upstream event blindness (including the supplied sed -i observation and root
configuration coverage) are not solved. Unknown inputs it observes fall back
loudly. Use explicit SHA selection/configured paths or disable narrowing when
those assumptions do not hold.

Both detached specimen worktrees were clean before removal, their exact JVMs
were stopped, and the worktrees are removed. Logs/receipts were verified and
copied first. Original source checkouts were not edited. No prohibited ports or
/tmp were used. See [cleanup record](/home/forge/src/kaocha-sublime/evidence/cleanup.json).
