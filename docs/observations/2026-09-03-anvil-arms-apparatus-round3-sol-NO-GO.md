# anvil-arms-apparatus f7b217e — Sol executed round-3 review: NO-GO for a cohort (7 closed, 3 partial; static Make parser defeated by 8 constructs; sub-second fork escape; unversioned streams rescore) — round 4 launched

# Round-three executed review

Verdict: **NO-GO** for a real cohort.

Checkout remained clean at `f7b217ebe2235e0d304ed7e0b9b05739aa836c2a`. No forbidden port was contacted, port 7909 is free, and no review-owned process remains. Evidence is under [/home/forge/tmp/arms/solreview3](/home/forge/tmp/arms/solreview3/).

I followed the repository’s working-tree instructions in [skill.md](skill.md); no Clojure source inspection or repository edits were needed.

## Round-two docket

| # | Status | Implementation and my replay |
|---|---|---|
| 1 | **CLOSED** | [_make_targets.py:139](bench/anvil-arms/_make_targets.py:139) reads text without invoking Make. My process trace contains only Python; neither side-effect marker was created, `recursive → dynamic:+$(MAKE)`, and `conditional → shell-conditional`. [Trace](/home/forge/tmp/arms/solreview3/make-parser-process.trace) · [map](/home/forge/tmp/arms/solreview3/make-parser-strace-map.json) |
| 2 | **PARTIAL** | [watch.py:398](bench/anvil-arms/watch.py:398) detects live replacement and [watch.py:970](bench/anvil-arms/watch.py:970) retains bytes from the open fd. Replay: watcher rc 8, `rollout-rotated`, `inode-changed`, correct retained copy. However, the scorer still accepts the old split-brain artifact; see round-three finding (e). [Replay](/home/forge/tmp/arms/solreview3/selftest-retained/case23.out) |
| 3 | **CLOSED** | [score.py:300](bench/anvil-arms/score.py:300) makes every watcher abort terminal. Idle-stop replay returned `SCORE-ABORT` rc 3 and deleted the seeded receipt. [Replay](/home/forge/tmp/arms/solreview3/selftest-retained/case24-idle-stop.out) |
| 4 | **CLOSED** | [score.py:184](bench/anvil-arms/score.py:184) requires the final `end`, integer `driver_rc`, and numeric wall. Dropping it returned rc 3 `watch-unterminated` and removed the receipt. [Replay](/home/forge/tmp/arms/solreview3/selftest-retained/case25-drop.out) |
| 5 | **PARTIAL** | [watch.py:788](bench/anvil-arms/watch.py:788) reaps observed descendants; the builder fixture recorded 3 and killed its setsid escapee. But the once-per-second scan at [watch.py:876](bench/anvil-arms/watch.py:876) misses a child that forks and exits between scans; my grandchild survived while the receipt machinery reported `descendants_recorded=1`, `orphans_after_reap=0`. [My measurement](/home/forge/tmp/arms/solreview3/fast-fork/result.json) |
| 6 | **PARTIAL** | [watch.py:202](bench/anvil-arms/watch.py:202) now refuses directly typed unknown targets, and `make ghost` replayed as incomplete-run. It does not propagate unresolved/refused targets through resolved prerequisites or recursive Make recipes, allowing `verify → make hidden` while `hidden` is refused. [Direct replay](/home/forge/tmp/arms/solreview3/selftest-retained/case27.out) · [counterexample](/home/forge/tmp/arms/solreview3/static-parser-results.json) |
| 7 | **CLOSED** | [build-prompts.py:52](bench/anvil-arms/prompts/build-prompts.py:52) includes the B.4 preamble; [MANIFEST.sha256:6](bench/anvil-arms/prompts/MANIFEST.sha256:6) carries its hash. Parent mutation returned `PROMPT-DRIFT` rc 3. [Replay](/home/forge/tmp/arms/solreview3/selftest-retained/case28.out) |
| 8 | **CLOSED** | [run-arm.sh:180](bench/anvil-arms/run-arm.sh:180) records boot ID and [stop-server.sh:46](bench/anvil-arms/stop-server.sh:46) requires it to match. A fabricated boot ID returned rc 3 and the process was not signalled. [Replay](/home/forge/tmp/arms/solreview3/selftest-retained/case29b.out) |
| 9 | **CLOSED** | [run-arm.sh:95](bench/anvil-arms/run-arm.sh:95) enforces ASCII `[A-Za-z0-9._-]{1,40}` before creating anything. `../x`, `é`, and `α` were refused; exactly 40 ASCII characters passed dry-run and 41 failed. No root was created. [Results](/home/forge/tmp/arms/solreview3/identity-results.json) |
| 10 | **CLOSED** | [self-test.sh:76](bench/anvil-arms/self-test.sh:76) honors the caller’s ports and preflights them. The literal target ran successfully with only 7909. [Full retained log](/home/forge/tmp/arms/solreview3/selftest.log) |
| Diagnostic wording | **CLOSED** | [watch.py:737](bench/anvil-arms/watch.py:737) performs a bounded diagnostic scan after refusal. Replay named the announced UUID, approximate byte 70013, and the 65536-byte ceiling without claiming silence. [Replay](/home/forge/tmp/arms/solreview3/selftest-retained/case32.out) |

That is **7 CLOSED, 3 PARTIAL**, plus the diagnostic **CLOSED**.

## Round-three probes

### (a) Static Makefile parser — OPEN

Using harmless `bin/kaocha` stubs as the GNU Make oracle, eight constructs produced a false resolved non-test:

| Construct | Static result | GNU Make |
|---|---|---|
| `define`/`endef` override | `echo safe` | ran Kaocha stub |
| `:=` before later `=` reassignment | `echo safe` | ran stub |
| target-specific variable | `echo safe` | ran stub |
| `%.test:` supplying an explicit target’s recipe | `make dep` | ran stub |
| recipe continuation splitting `if`/`then` | `i f …; th en …` | ran stub |
| `$(MAKE)` without `+` into a refused conditional target | `make hidden` | conditionally ran stub |
| existing hard include | `echo safe` | ran stub |
| generated missing `-include` | `echo safe` | rebuilt include, then ran stub |

Ordinary `.PHONY` and recursive `=` expansion classified correctly. `$$` command substitution failed closed as unresolved.

The core causes are assignment flavors being collapsed at [_make_targets.py:211](bench/anvil-arms/_make_targets.py:211), patterns being skipped at [_make_targets.py:155](bench/anvil-arms/_make_targets.py:155), recipe continuations being rewritten at [_make_targets.py:94](bench/anvil-arms/_make_targets.py:94), and includes being recorded but never parsed at [_make_targets.py:187](bench/anvil-arms/_make_targets.py:187).

[Complete parser results](/home/forge/tmp/arms/solreview3/static-parser-results.json)

### (b) Once-per-second descendant walk — OPEN

A child spawned after the initial scan, forked a grandchild, and exited before the next one-second scan. The grandchild reparented away and escaped via `setsid`.

Measured outcome:

```text
watch_rc=5
abort=idle-stop
descendants_recorded=1
orphans_after_reap=0
escaped_alive_after_watcher=true
```

I SIGKILLed that exact review-owned PID afterward and verified it disappeared. [Result](/home/forge/tmp/arms/solreview3/fast-fork/result.json)

### (c) Inode-binding failure typing — PARTIAL

A normal `(st_dev, st_ino)` change is typed as `inode-changed` and becomes rc 8. But both path `stat()` and fd `fstat()` errors are silently converted to “no rotation” at [watch.py:407](bench/anvil-arms/watch.py:407) and [watch.py:414](bench/anvil-arms/watch.py:414). Mocked `ESTALE` returned `null`, so the NFS-style failure path is not fail-closed or typed. [Results](/home/forge/tmp/arms/solreview3/inode-probe/result.json)

### (d) Identity boundary — CLOSED

Unicode letters are rejected, 40 ASCII characters are accepted, 41 are rejected, and `../x` creates nothing. [Results](/home/forge/tmp/arms/solreview3/identity-results.json)

### (e) Old rotation artifact rescoring — OPEN

The current scorer returned rc 0 on a copied round-two split-brain artifact:

```text
watch_has_schema_version=false
watch_has_inode_fields=false
run_has_rollout_rotation=false
receipt_written=true
receipt_sources_agree=true
receipt_via_verb=1
```

That is not acceptable. [score.py:134](bench/anvil-arms/score.py:134) must reject evidence lacking a supported schema/version and inode-binding provenance. The new watcher should emit the schema and bound rollout identity; the scorer should require them, rather than assuming any syntactically valid old stream was produced by the repaired watcher. [Rescore result](/home/forge/tmp/arms/solreview3/old-rotation-rescore/rescore-result.json)

## Required self-test line

```text
anvil-arms self-test: 288 passed, 0 failed  (workdir /home/forge/tmp/arms/selftest.Ar7aMQ)
```

The target exited 0, but the same run also emitted `self-test.sh: line 941: end: command not found`: backticks inside the double-quoted case header execute `end`. The README also still claims 278 rather than 288 assertions.

## Final gate

**NO-GO — do not use this apparatus on a real cohort.**

1. **OPEN —** [_make_targets.py:211](bench/anvil-arms/_make_targets.py:211): eight controlled fixtures were falsely resolved as non-tests although GNU Make executed the Kaocha stub.
2. **OPEN —** [watch.py:876](bench/anvil-arms/watch.py:876): a sub-second fork/reparent escaped unobserved and survived while `orphans_after_reap=0`.
3. **OPEN —** [score.py:134](bench/anvil-arms/score.py:134): an unversioned old split-brain stream rescored rc 0 with `sources.agree=true`.
4. **PARTIAL —** [watch.py:407](bench/anvil-arms/watch.py:407): inode changes are typed, but `stat`/`fstat` failures such as `ESTALE` silently pass.
5. **OPEN —** [self-test.sh:941](bench/anvil-arms/self-test.sh:941): the green suite executes the backticked word `end`, emits a shell error, and still reports zero failures.
6. **OPEN —** [README.md:11](bench/anvil-arms/README.md:11): documentation claims 278 assertions while the executed target reports 288.