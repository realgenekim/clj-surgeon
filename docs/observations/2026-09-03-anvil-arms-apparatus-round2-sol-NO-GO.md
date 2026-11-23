# anvil-arms-apparatus 6c1cf0a — Sol executed round-2 review: NO-GO for a real cohort (6 closed, 6 partial, 10 items) — round 3 launched

Executed review verdict: 6 CLOSED, 6 PARTIAL, 0 fully OPEN. The repaired happy path is green, but round two introduced or retained several fail-open evidence paths. The apparatus is not safe for a real cohort.

Repository remained unchanged at `6c1cf0a`; no forbidden ports were contacted, port 7909 is free, and no review process remains. Artifacts are under [/home/forge/tmp/arms/solreview2](/home/forge/tmp/arms/solreview2).

## Round-one docket

| # | Status | Implementation and replay witness |
|---|---|---|
| 1 | CLOSED | [score.py:55](bench/anvil-arms/score.py:55) validates every JSONL record and ordering. My duplicate, out-of-order, and truncated replays returned rc 3, deleted seeded receipts, and wrote none. |
| 2 | CLOSED | [score.py:173](bench/anvil-arms/score.py:173) removes stale JSON and Markdown receipts on every abort. Missing rollout, empty watch, and failed attestation returned 3/3/2 with both receipts absent. |
| 3 | CLOSED | [watch.py:534](bench/anvil-arms/watch.py:534) and [score.py:251](bench/anvil-arms/score.py:251) reject an unanswered call. `partial-call` returned rc 3, `incomplete-run`, no receipt. |
| 4 | PARTIAL | [watch.py:155](bench/anvil-arms/watch.py:155) correctly maps ordinary `make verify`, but unknown and conditional targets fail open, and generating the map executes repo-controlled Make code. |
| 5 | PARTIAL | [watch.py:571](bench/anvil-arms/watch.py:571) creates and reaps a driver group, including SIGKILL after two seconds at [watch.py:677](bench/anvil-arms/watch.py:677). A driver-invoked `setsid`, however, escaped and survived while `driver_group_orphans=0`. |
| 6 | CLOSED | [_attest_write.py:130](bench/anvil-arms/_attest_write.py:130) binds health PID, port, and project root to independent witnesses. My live 7909 server advertising another project was refused rc 2. |
| 7 | CLOSED | [attest.sh:111](bench/anvil-arms/attest.sh:111) still refuses unverified server source and contaminated native arms. Both standing witnesses passed. |
| 8 | PARTIAL | [run-arm.sh:178](bench/anvil-arms/run-arm.sh:178) provides a private `CODEX_HOME`, and [watch.py:360](bench/anvil-arms/watch.py:360) binds announced IDs. File rotation nevertheless produced a split-brain receipt. |
| 9 | PARTIAL | [build-prompts.py:41](bench/anvil-arms/prompts/build-prompts.py:41) hashes five bounded prose sections, but the load-bearing B.4 parent prose is outside all five. My meaning-changing mutation passed `--check` with rc 0. |
| 10 | CLOSED | [run-cohort.sh:46](bench/anvil-arms/run-cohort.sh:46) refuses `n<1`; [run-cohort.sh:96](bench/anvil-arms/run-cohort.sh:96) stops after the first failed arm. Cases passed with no later arm directory. |
| 11 | PARTIAL | [stop-server.sh:31](bench/anvil-arms/stop-server.sh:31) protects against same-boot PID reuse and ignores stale `ready.edn`, but records no boot ID. A fake mismatching boot-ID field was ignored and the process was signalled. |
| 12 | PARTIAL | [run-arm.sh:77](bench/anvil-arms/run-arm.sh:77) rejects an existing symlink escape, and private Codex/check directories are repaired. But `A` is formed from unvalidated components at [run-arm.sh:86](bench/anvil-arms/run-arm.sh:86); `--exp ../component-escape` created a directory outside the supplied runner root. |

Full copied-artifact results: [replay-summary.txt](/home/forge/tmp/arms/solreview2/replay-summary.txt).

## Round-two probes

- Session binding:

  - No session ID: watcher rc 7, scorer rc 3, no receipt.
  - ID announced one second late: bound and scored normally.
  - ID after the 64 KiB banner scan ceiling: rc 7/3, no receipt—fail closed, though the diagnostic incorrectly says no ID was announced.
  - Bound empty rollout: watcher rc 4 `zero-returns`, scorer rc 3, no receipt.
  - Rotation: watcher read the old inode containing `alias_migration`; the retained `rollout.jsonl` came from the replacement inode containing `shell`. Scorer returned rc 0 with `sources.agree=true` and `writes.via_verb=1`. See [rotation receipt](/home/forge/tmp/arms/solreview2/session/rotate-session/receipt.json).

- Make mapping:

  - `_make_targets.py` returned rc 0 while actually creating both `shell-expansion-ran` through `$(shell …)` and `recursive-recipe-ran` through `+$(MAKE)`.
  - `make conditional` and an unknown `make verify` both produced rc-0 receipts with `test_actions=0`, `non_test_actions=1`. They are not treated as `incomplete-run`.
  - Evidence: [map.json](/home/forge/tmp/arms/solreview2/make-probe/map.json).

- Reaping:

  - A TERM-resistant process in the driver group received SIGKILL after the two-second grace period; driver rc was `-9`, no survivor.
  - A driver invoking `setsid` left PID 3289785 alive after abort while reporting zero group orphans. I killed that exact review-owned process group afterward.

- Governing prose:

  The manifest hashes the prose blobs for A.8, B.4.1, B.4.2, B.4.3, and B.4.4 at [MANIFEST.sha256:5](bench/anvil-arms/prompts/MANIFEST.sha256:5). It does not hash the B.4 parent paragraph at [prestage doc:643](docs/observations/2026-09-04-e3-e6-prestaged.md:643): “Both arms are byte-identical outside §5.” Changing that to permit differences passed `--check`; see [probe output](/home/forge/tmp/arms/solreview2/b4-unhashed-probe.out).

- Server identity:

  - A live server with correct PID/port but another project root was refused rc 2: `healthz-project-root-ne-worktree`.
  - Correct health plus stale `ready.edn` was refused rc 2 on root, HEAD, and PID mismatches.
  - `/proc` start time protects only within one boot; `/proc/sys/kernel/random/boot_id` is neither recorded nor checked.

- Root confinement:

  - A permitted-root symlink to this checkout was resolved and refused rc 2 before arm creation.
  - Unvalidated identity components can still traverse outside the supplied root.

- Additional receipt failures:

  - One return followed by an idle timeout produced watcher rc 5, then scorer rc 0 and a receipt.
  - Removing the final `end` record from a good watch stream still produced scorer rc 0 and a receipt.

## Self-test

The checked-in test forcibly sets port 7907 at [self-test.sh:61](bench/anvil-arms/self-test.sh:61), overriding `COHORT_PORTS=7909`. I therefore could not run the literal Make target without violating your port boundary. I ran a mechanically identical harness copy with only its 7907 literals changed to 7909; all checkout apparatus files remained unchanged. Full log: [selftest3.log](/home/forge/tmp/arms/solreview2/selftest3.log).

`anvil-arms self-test: 162 passed, 0 failed  (workdir /home/forge/tmp/arms/solreview2/selftest3)`

NO-GO — do not use this apparatus on a real cohort.

1. [_make_targets.py:93](bench/anvil-arms/_make_targets.py:93) — attest-time `make -n` executed both `$(shell …)` and a `+$(MAKE)` recipe.
2. [watch.py:718](bench/anvil-arms/watch.py:718) — rollout rotation mixed an old watched inode with a new retained inode and emitted a contradictory rc-0 receipt.
3. [score.py:251](bench/anvil-arms/score.py:251) — an `idle-stop` abort is merely noted, then scored; watcher rc 5 became scorer/run receipt rc 0.
4. [score.py:128](bench/anvil-arms/score.py:128) — `validate_watch` never requires its documented final `end`; deleting it still scored rc 0.
5. [watch.py:597](bench/anvil-arms/watch.py:597) — cleanup reaches only the original PGID; a descendant calling `setsid` survived and was reported as zero orphans.
6. [watch.py:184](bench/anvil-arms/watch.py:184) — unresolved and conditional Make targets fail open as non-test actions instead of making the run incomplete.
7. [build-prompts.py:44](bench/anvil-arms/prompts/build-prompts.py:44) — the hashed subsection list omits the load-bearing B.4 parent paragraph; a meaning change passed rc 0.
8. [stop-server.sh:31](bench/anvil-arms/stop-server.sh:31) — PID plus start ticks cannot distinguish reuse across reboot because boot ID is absent.
9. [run-arm.sh:86](bench/anvil-arms/run-arm.sh:86) — unchecked experiment/rung/slot path components can escape the caller’s validated runner root.
10. [self-test.sh:61](bench/anvil-arms/self-test.sh:61) — the smoke test overrides the caller’s port scope, preventing safe use on a shared review box.