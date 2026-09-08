Implemented and pushed to [marvin-openclaw777/kaocha-sublime master](https://github.com/marvin-openclaw777/kaocha-sublime/commit/1bc1ffaddc844ee7f2cec37b9fd4033952833e86). The watch-mode entrance is **never tail; ask `kaocha-status`**. The final real mvr watcher witnesses pass. Ten complete CLI processes measured **23.854421 ms median**, **25.567515 ms maximum**, all below 200 ms.

Completed 2026-09-08T17:52:18+00:00 within the requested 90-minute timebox. Repository: `/home/forge/src/kaocha-sublime`. Master and origin/master both resolve to `1bc1ffaddc844ee7f2cec37b9fd4033952833e86`; the checkout is clean.

Gene's motivating words, preserved verbatim:

> Literally, when I was running the Coucha test runner in watch mode, the coding agent would tail the test logs. Sometimes, the test output was so noisy that the coding agent would get confused about whether the test was running or whether it passed or failed. Are you telling me that's not an issue?

> if you can disambiguate test runs, that would be epic.

The four status lines below are verbatim executable output from the accepted real watcher sequence. The DONE line is the save's green run; DONE also represents completed failing runs, so the counters matter.

```text
KAOCHA-STATUS RUNNING since=2026-09-08T17:43:03.294923148Z tmp=/var/tmp/forge/kaocha-status-fx/mvr/target/kaocha-runs/2026-09-08T17-43-03-294923148Z-26650e4c-60e8-44fb-ba1c-08978a3e9b5b.edn.tmp
KAOCHA-STATUS DONE id=2026-09-08T17-43-03-432935922Z-1da26d95-81f6-4e50-9119-6258084c1ea5 finished=2026-09-08T17:43:03.513483218Z tests=6 pass=35 fail=0 error=0 closure=1/54 covers=unknown receipt=/var/tmp/forge/kaocha-status-fx/mvr/target/kaocha-runs/2026-09-08T17-43-03-432935922Z-1da26d95-81f6-4e50-9119-6258084c1ea5.edn
KAOCHA-STATUS STALE latest=2026-09-08T17:43:03.561714955Z newer_edit=/var/tmp/forge/kaocha-status-fx/mvr/test/marvin_voice_remote/auth_test.clj@2026-09-08T17:43:03.986602103Z
KAOCHA-STATUS NONE dir=/var/tmp/forge/kaocha-status-fx/mvr/target/kaocha-runs reason=interrupted
```

The specimen was a fresh detached worktree of marvin-voice-remote `nrepl/test-alias` at `9439370`, under `/var/tmp/forge/kaocha-status-fx/mvr`. It ran the real Kaocha 1.91.1392 Beholder watcher with both plugins and the branch's `:run-tests` classpath/properties. Initial baseline: 579 tests, 7833 assertions, zero failures/errors. One specimen JVM ran at a time; the accepted PID was **2902707**. No nREPL or application listener was launched; no requested forbidden service port was used. SIGKILL targeted that exact PID after a test-body ready file independently reported it, and the harness waited for its exit. The specimen file was restored, the worktree is clean, and the JVM is gone.

| Witness | Observed result | Verdict |
|---|---|---|
| Empty directory before launch | `KAOCHA-STATUS NONE dir=/var/tmp/forge/kaocha-status-fx/mvr/target/kaocha-runs` | PASS |
| Save → immediate query | RUNNING; red save call 27.243782 ms, green save call 25.893384 ms | PASS; neither immediate result was DONE |
| Red verdict | 6 tests, pass=34, fail=1, error=0; closure=1/54 | PASS; every displayed total equals its named receipt |
| Green verdict | 6 tests, pass=35, fail=0, error=0; closure=1/54 | PASS; every displayed total equals its named receipt |
| Actual watcher provenance | Both save receipts name the canonical `auth_test.clj` path in `:trigger :files`, with an acceptance stamp in `:trigger :at` | PASS |
| Automatic follow-up | A second DONE with a distinct ID, `:auto-followup? true`, empty trigger files and explicit `:followup-of` | PASS |
| `--since <my save>` after follow-up | Returns the save's green ID, not the automatic follow-up ID | PASS |
| `sed -i` append | STALE immediately and after 1.5 seconds; LATEST remained unchanged | PASS; real upstream blindness reproduced |
| Namespace outside closure | `--ns outside.this.closure` gives `covers=no` | PASS |
| SIGKILL mid-run | RUNNING before kill; RUNNING after exact-PID exit; reaper then yields NONE with `reason=interrupted` | PASS; never resurrects old DONE |
| Edit during the blocked run | STALE against the earlier start | PASS |
| Ten timed status calls | Median 23.854421 ms; max 25.567515 ms | PASS; 10/10 under 200 ms |
| Normal suite and linked intents | 25 tests, 203 assertions, zero failures/errors; five active intents checked in both directions | PASS |
| Native lint | `~/bin/clj-kondo --lint src test`: zero errors/warnings | PASS |
| Existing process witnesses | SIGKILL, isolated concurrent receipt writers, warm REPL, fixed-ID collision | PASS |
| Existing CLI witnesses | Leaf/source selection and deps/unknown/tracker/clean fallback | PASS |
| Retained prior evidence | Existing mvr/ccfp receipts and reporter totals still verify | PASS |

The ten individual wall times, in milliseconds, were:

```text
24.028868 24.561587 22.994265 24.105854 23.898101 23.189428 23.184019 20.921647 23.810741 25.567515
```

These are Python `perf_counter_ns` measurements around ten separate `bin/kaocha-status --dir ...` subprocesses, including Bash, Babashka startup, file reads, output and exit. Babashka was 1.13.219. The watcher was alive and idle; filesystem caches were warm. This establishes the requested entrance speed on this Linux seat, not a cold-disk or cross-platform bound.

The implementation reserves `<id>.edn.tmp` when the watcher accepts a rescan, before scan/reload/test work. Completion stages the EDN in `<id>.edn.ready`, atomically renames it to `<id>.edn`, publishes `<dir>/LATEST` using a separate temporary file and atomic rename, then removes the running reservation. Keeping the reservation through both publications prevents a kill between renames from exposing an older verdict. LATEST contains the absolute receipt path, run ID and finished stamp. Every completed receipt carries a trigger and an explicit auto-follow-up boolean.

The reader gets completed identity only from LATEST; it never sorts or selects EDN receipts by time. It enumerates running reservations and, only when LATEST is absent, checks whether legacy EDN files exist. The legacy case returns `NONE ... reason=no-latest-pointer`. Namespace coverage is a full subset check against the receipt's recorded closure; no request or missing closure metadata yields `unknown`. `closure=n/total` shows selected/configured test namespace counts. Invalid or ambiguous state refuses a verdict with a typed NONE line.

The save-identity rule is explicit: without `--since`, show LATEST. With `--since`, if LATEST is an automatic follow-up with no newly reported files, follow its `:followup-of` link and compare the parent's start against the supplied file mtime or UTC stamp. This keeps the save's run ID stable across the automatic rerun. A follow-up carrying new watcher files is evaluated as its own event run. This is a freshness entrance, not a search for arbitrary historical saves; retain an exact receipt path for historical identity.

The two green lines demonstrate that rule:

```text
KAOCHA-STATUS DONE id=2026-09-08T17-43-03-432935922Z-1da26d95-81f6-4e50-9119-6258084c1ea5 finished=2026-09-08T17:43:03.513483218Z tests=6 pass=35 fail=0 error=0 closure=1/54 covers=unknown receipt=/var/tmp/forge/kaocha-status-fx/mvr/target/kaocha-runs/2026-09-08T17-43-03-432935922Z-1da26d95-81f6-4e50-9119-6258084c1ea5.edn
KAOCHA-STATUS DONE id=2026-09-08T17-43-03-515530069Z-480f64db-8135-437b-8dc0-faa67e7c4a70 finished=2026-09-08T17:43:03.561714955Z tests=6 pass=35 fail=0 error=0 closure=1/54 covers=unknown receipt=/var/tmp/forge/kaocha-status-fx/mvr/target/kaocha-runs/2026-09-08T17-43-03-515530069Z-480f64db-8135-437b-8dc0-faa67e7c4a70.edn
```

After the second line, `--since <my save>` returned exactly the first line. Kaocha requested its automatic full-rerun branch; the existing affected plugin deliberately retained the same 1/54 namespace selection for that unchanged follow-up, so both receipts contain 6 tests / 35 passing assertions.

Crash cleanup is explicit: `bin/kaocha-reap --dir ...` checks the recorded local PID and process start time, never signals a process, and leaves live or unknown owners alone. It writes INTERRUPTED atomically before deleting a dead running reservation. That tombstone blocks the old green until a later completed run supersedes it. Tests additionally cover publication interruption, live owners, PID reuse, unknown owners, nanosecond file freshness, namespace subsets, odd filenames, and the no-LATEST legacy directory.

Red-first history is retained. The initial committed suite had 8 failures / 31 errors, including the not-yet-existing CLI. Real attempt 1 exposed Beholder's `java.nio.file.Path` versus the fixture's `java.io.File`; the added Path case went red before its repair. Real attempt 2 exposed a STALE window before plugin configuration; a new boundary test went red before moving reservation to watcher event acceptance. The accepted third attempt passed the full sequence. Both failed attempts remain in the repository evidence and their detached worktrees are preserved under `attempt1/` and `attempt2/` beneath the fixture root.

| Commit | Change |
|---|---|
| `429dd6e` | Linked intents and failing status/receipt tests |
| `51d8866` | Atomic LATEST, watcher provenance/reservation, status and reaper executables, regression guards |
| `1bc1ffa` | README agent entrance, real watcher harness, raw witnesses and timing evidence |

The requested [linked-intent-testing skill](/home/forge/src/claude-skills-nrepl/linked-intent-testing/SKILL.md) was applied using native Clojure reads/patches. The append-only [registry](/home/forge/src/kaocha-sublime/docs/intent/registry.edn) records atomic promises, misreadings and boundaries. Code/test tags are enforced bidirectionally by the normal suite. Traceability protects those promises from disappearing; it does not itself prove that the promises are correct. The real watcher and the independent expected status lines supply the behavioral witnesses.

Evidence: [accepted typed lines and timings](/home/forge/src/kaocha-sublime/evidence/status-watch/witnesses.jsonl), [method and reproduction](/home/forge/src/kaocha-sublime/evidence/status-method.md), [green suite](/home/forge/src/kaocha-sublime/evidence/status-green.log), [lint](/home/forge/src/kaocha-sublime/evidence/status-lint.log), [process witnesses](/home/forge/src/kaocha-sublime/evidence/status-process.log), [CLI witnesses](/home/forge/src/kaocha-sublime/evidence/status-cli.log), and [README entrance](/home/forge/src/kaocha-sublime/README.md).

What remains: no requested implementation or witness is outstanding. The documented limits remain intentional: upstream `sed -i` blindness is detected as STALE, and edits during a run are STALE by start-time design. Before an operating-system event is accepted, STALE can still be truthful; the two immediate RUNNING observations are measured witnesses, not a filesystem-delivery guarantee. The watcher integration remains pinned to Kaocha 1.91.1392; broader version/platform validation and an upstream lifecycle hook are release work. Keep one local watcher per receipt directory and retain linked receipts/crash markers. No published artifact or unrelated specimen code change was made.
