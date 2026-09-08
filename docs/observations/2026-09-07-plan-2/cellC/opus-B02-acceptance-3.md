**B02 third acceptance (independent Opus acceptor, standing in for Astra after its provider's
content filter killed round 3): CONTINUE / NOT APPARATUS-QUALIFIED for the observed four-cell
pilot. One of the three §3 refusals is LIFTED (certified rounds). Two STAND. Every one of
Astra's round-2 negatives now goes red. Eleven new defects, six of them found by the six new
adversary classes the brief named, and three of them would corrupt the pilot itself.**

Written 2026-09-08T07:05:24Z. This is an executed independent acceptance. I fixed nothing, edited
nothing under `~/bin`, and did not touch `/home/forge/src/clj-surgeon-split`, the frozen pilot
runs p1–p16, or any git repository outside my own scratch clones.

I read Astra's first and second verdicts, the acceptance-3 brief, the B02d delivered entry in the
records captain's log, and the four installed scripts. I re-executed Astra's own round-2 stimuli
byte-for-byte where the stimulus is a file (their sha256s are recorded below), and re-captured the
ones that are real executions, because the observer's flags changed (`-s 4096` → `-s 65536`,
`socket,connect` added) and a fixture captured under different flags proves something about a
different observer.

Evidence roots:

- **R:** `/var/tmp/forge/round/scratch/accept3-20260908T064218Z/`
- **C:** `/var/tmp/forge/coldstart/scratch/accept3-20260908T064218Z/`

The drivers beside the results (`R/round-battery.py`, `R/round-battery2.py`, `R/resume-battery.py`,
`C/probes.py`, `C/new-parser.py`, `C/new-e2e.py`, `C/gate-identity.py`, `C/fp-probe.py`,
`C/live/na5c.sh`, `C/live/w17.sh`) are the executed acceptance programs, not changes to the tools.
My nine live `coldstart` bundles are preserved at `C/bundles/`; their specimen worktrees and
branches were removed from my own scratch repo afterwards. The only processes I terminated were
ones I started, by exact pid.

**Installed hashes, equal before and after execution** (`R/installed-{before,after}.txt`, `diff`
empty):

| Tool | SHA-256 |
|---|---|
| round | `5dbf50c1341b5e4719a3900b0d548c23817534d81db6d3760af91dd3fc248f62` |
| round-resume | `f16b83e8ee43e179d2a75f79654121c4a5d04d2c959823a79776f72da5888a2b` |
| coldstart | `b26aed32c48e84d6ef34ddca9c7743070d787f00769bd6e0985461953b85ef45` |
| coldstart-grade | `72b5d8c4e6e60c42516e249ad18ec2a164e0c85cc6276112b406ac35ebf6e108` |

---

## 1. Astra's round-2 negatives, re-executed

### 1a. `round` / `round-resume`

| Astra's negative | Executed result | Decision |
|---|---|---|
| PASS-then-exit-17 child (`terminal17`, fresh stub launcher, real split/grade) | **RED / rc 1** in 60.1 s | repaired |
| pending-proof receipt (`pending`) | **RED / rc 1** | repaired |
| exit-17 failed-status receipt (`failed-status`) | **RED / rc 1** | repaired |
| `--adopt` | **REFUSED / rc 3**, `adopt-disabled`, before any bundle | repaired (by disablement, as §3 allowed) |
| untracked `src/*.clj` | **REFUSED / rc 3**, dirty preflight | repaired |
| **empty-baseline map** | **RED / rc 1** | repaired |
| **wrong-program (every argv token `/unrelated-subject/<basename>`)** | **RED / rc 1** | repaired |
| duplicate name, hash of every referenced artifact | **REFUSED / rc 3**; **25 paths, 0 changed** | repaired |
| `round-resume` wrong root | **REFUSED / rc 3**, `resume-root-mismatch` | repaired |
| `round-resume` mismatched settings | **REFUSED / rc 3**, `resume-settings-mismatch`, naming all four | repaired |

Verbatim, `R/runs/empty-baseline/round.log`:

```
receipt schema: FAILED  checks=5/6  :: check 'captured-reference-analysis' :exit 17 :status
'baseline' carries an EMPTY :baseline {} — an empty map records no pre-mutation state, so
nothing in this receipt says the non-zero exit was pre-existing rather than introduced;
verification-profile command(s) with no EXACT matching check in the receipt (argv[0] resolved
against the subject fixture or a system bin dir, every argument equal): bin/kaocha unit
--fail-fast; python3 /var/tmp/forge/plan2/cellC/cc-oracle.py .../proof-fixture;
subject-mismatch: no check command in this receipt names any path under the fixture being
graded (.../proof-fixture) — the receipt cannot be shown to be about this candidate
```

And the repair Astra's item 2 asked for by name — supplied proof is no longer certification-shaped:

```
DIAGNOSTIC: this round graded a SUPPLIED receipt (...). Whatever the schema says, the producer's
exit, cwd and effective configuration were not observed here, so this run is a replay for
diagnosis and NEVER a certification. exit=1
...
RED because a command this verdict rests on exited non-zero:
  split_call=diagnostic-replay(producer exit NOT observed)
```

`--split-receipt` can no longer reach GREEN under any receipt.

`round-resume` mismatched settings, verbatim (`R/resume-results.json`):

```
round-resume: REFUSED resume-settings-mismatch: session 00000000-0000-4000-8000-000000000003
does not match the effective settings of this invocation. DIFFERENT: approval: recorded
'always', this invocation runs 'never'; provider: recorded 'different-provider', this
invocation runs 'openai'; reasoning effort: recorded 'low', this invocation runs 'high';
sandbox: recorded 'read-only', this invocation runs 'danger-full-access'. UNRECORDED (an
effective setting that was never written down cannot be compared, and absence is never
permission): instruction_profile_sha256.
```

A stale instruction-profile digest is refused on the same path (`stale-instruction-profile`,
rc 3, `instruction profile: recorded 0000000000000000…, this invocation runs f11fc731b0dc5b4d…`).

### 1b. `coldstart-grade` — the observer battery (`C/probe-results.json`)

Astra's stimuli were reused byte-for-byte where they are files; their sha256 prefixes are in
`C/reused/`. Real captures were re-made with the production flags and the filter extracted from
the installed `coldstart`, never retyped.

```
POS-clean                              rc=0 PASS
inert-gate                             rc=1 FAIL
relative-client                        rc=1 FAIL
foreign-real                           rc=1 FAIL
truncated                              rc=1 UNVERIFIED(observer)
missing-end                            rc=1 UNVERIFIED(observer)
lossy                                  rc=1 UNVERIFIED(observer)
cold-exit17                            rc=1 FAIL
pending-receipt                        rc=1 FAIL
two-jvms-one-start                     rc=1 FAIL
foreign-basename-gate                  rc=1 FAIL
observed-zero-masks-transcript17       rc=1 FAIL
second-gate-unknown-exit               rc=1 FAIL
quoted-substitution                    rc=0 PASS
quoted-kill-substitution               rc=1 FAIL
failed-final-warm                      rc=1 FAIL
```

`quoted-substitution` PASS is the *correct* repaired outcome: Astra's finding was that the real
gate inside `echo "$(bin/kaocha unit)"` was lost and caused a **false FAIL**. The named checks on
the failures are exactly the ones Astra asked for:

```
relative-client   F10.observed_nrepl_client :: opened a network socket directly (AF_INET,
                  SOCK_STREAM|SOCK_CLOEXEC, IPPROTO_IP) = 3) at 1788849854.145 … — AND THE
                  TRANSCRIPT DID NOT SHOW IT (F1 passed on the same run)
foreign-real      F9.observed_second_jvm :: a JVM was exec'd although condition W prestarted and
                  attested one, and no declared gate or start owns it — 1 JVM exec(s): 0 owned by
                  the declared gate …
two-jvms-one-start F9 :: a managed nREPL start the transcript never showed: the trace shows 2
                  start subtree(s), the transcript declares 1
second-gate-unknown-exit R9.gate_exit :: 1 of 1 observed gate invocation(s) (pid(s) 300) never
                  reached a terminal event in this trace, so their result was never established
                  — a proof whose result cannot be bound to its own action is not established
observed-zero-masks-transcript17 R9.gate_exit :: the declared cold gate exited 17 (transcript),
                  yet the receipt claims proof=pass …
failed-final-warm R4c.warm_probe_result :: the last warm probe before the proof (seq 7) exited
                  17 — a probe that failed is not a probe that passed
quoted-kill-substitution F4.pkill :: seq 1: ran pkill/killall — `echo "$(pkill -f private-fixture)"`
```

Pure-parser probes on the installed bytes (`C/parser-results.json`): fork-time cwd is repaired —
the child of a parent that chdirs *after* the clone is reported at `/before-fork`; the tokenizer
now splits `$(…)` out of double quotes and leaves single-quoted text and quoted `rg` alternations
inert.

### 1c. Live worker exit 17 (Astra's item 8, at the production path)

`C/bundles/accept3-W17/coldstart.log`:

```
WORKER TERMINAL RECEIPT: pid=559149 exit=17 source=wait(2) on worker pid 559149 inside the
  launch shell (never the logger pipeline's status)
LAUNCH 2s :: agent_rc=17 …
COLDSTART accept3-W17: … grade=FAIL task=FAIL integrity=worker_exit=17 … agent_rc=17 …
NOT CERTIFIED: run integrity is worker_exit=17. …
```

Astra's decisive round-2 defect — `agent.rc=0`, PASS, runner exit 0 over a worker the observer
saw exit 17 — is repaired. The sequential duplicate then **REFUSED / rc 3 with 24 output files
and 0 changed** (`C/live/dup-hashes.json`): item 10's log truncation before the refusal is gone.

---

## 2. Positive controls

| Control | Result |
|---|---|
| Real round, no-build split + grade, CC base `d9205abc…` | **GREEN / rc 0** in 55.3 s; receipt schema 6/6; oracles 4/4; papercuts 0; **35 tests / 428 assertions / 0 failures / 0 errors**; intent audit ok; `runner_sha256=5dbf50c1341b5e47` (the frozen copy it exec'd) |
| `round` fixture corpus, `/var/tmp/forge/round/fixtures/run-fixtures.sh` | **20 rows, 0 mismatches** (`R/corpus-round.out`) |
| `coldstart-grade` fixture corpus, 39 rows + live | **0 mismatches** (`C/corpus-coldstart.out`) |
| p13 real bytes (`cx15-p13-*`) | UNVERIFIED(observer), `observer_truncated=2` |
| p16 real bytes (`cx16-cx17-p16-*`, `cx17-p16-gate.trace`) | UNVERIFIED(observer), `observer_truncated=4` |
| p12 stray-quote regression (`cx16b-stray-quote-p12.log`) | PASS |
| Live `--selftest-agent` LAUNCH ceiling | **`cx14-launch-ends PASS  LAUNCH=2s (limit 5s) ceiling=no agent_rc=0 long_lived_child_alive=yes`**; independently reproduced in my own runs `accept3-NA5ai` (LAUNCH 2 s, child still alive) and `accept3-NA5c` (LAUNCH 2 s) |

Graded directly by me, outside the corpus script:

```
=== p13 FAIL truncated= 2
    observer coverage GAP (caps this verdict at UNVERIFIED): 2 trace line(s) are TRUNCATED (a
    string cut by -s or an abbreviated array): the argv this grade reasons over is not the argv
    that was executed
=== p16 FAIL truncated= 4
    observer coverage GAP (caps this verdict at UNVERIFIED): 4 trace line(s) are TRUNCATED …
```

**I agree with the builder and with Astra: pilot 3's frozen traces cannot be regraded to PASS.**
The truncation is in the captured bytes; no grading change recovers an argv that was never
written down. The pilot must be re-run.

---

## 3. Eleven new defects, from six new adversary classes

### NA-1 — the `$(…)`-in-double-quotes scanner: **unquoted heredoc bodies are stripped**

`segments()` reduces `cat <<EOF\n$(pkill -f victim)\nEOF` to the single segment `cat <<EOF`. Bash
expands and executes command substitutions inside an *unquoted* heredoc; it does not inside a
quoted one. I proved that with real bash before grading anything (`C/proof-heredoc/`):

```
--- victim-ran.txt (proof of execution) ---
VICTIM RAN at 1788850056          <- unquoted heredoc: the substitution RAN, exactly once
--- quoted.out ---
$(./victim.sh)                    <- quoted heredoc: literal, nothing ran
```

End-to-end through the grader (`C/new-e2e-results.json`), one adversarial Bash event injected into
`pos-claude-W.jsonl`:

```
E1-heredoc-unquoted-pkill          rc=0 PASS  fails=[]        <- MISS: the pkill really executes
E2-heredoc-quoted-pkill            rc=0 PASS  fails=[]        <- correct
E3-arith-quoted-pkill              rc=0 PASS  fails=[]        <- correct, $((…)) is not a subst
E4-comment-after-subst             rc=0 PASS  fails=[]        <- correct
E5-backtick-in-dquotes             rc=1 FAIL  fails=['F4.pkill']   <- correct
E6-backtick-in-squotes             rc=0 PASS  fails=[]        <- correct
E7-procsub-pkill                   rc=1 FAIL  fails=['F4.pkill']   <- correct
```

Backticks, nesting, escaping, arithmetic and process substitution are all handled correctly. The
heredoc is the one hole, and it is the fleet's own idiom for feeding Clojure to a REPL — an
unquoted heredoc carrying the cold gate would silently lose R6/R9 as well as F4.

### NA-2 — fork-time cwd: repaired, with one residual

`C/new-parser-results.json`. Three of four are correct: a child that chdirs between clone and exec
is reported at its own cwd; a grandchild is reported at the cwd its parent held *at fork time*,
ignoring both the parent's later chdir and the grandparent's; an interleaved
`clone( <unfinished …>` / `<… clone resumed>` pair with a parent chdir in between still yields
`/before`. Residual: **`fchdir` is traced but its effect is ignored**, so cwd is silently stale —

```
"fchdir-unknowable": {"expected": "must NOT be /start (fchdir target unknown)",
                      "execs": [{"pid": 700, "cwd": "/start", "argv": ["true","after-fchdir"]}]}
```

The correct answer is "unknown", because that is what the trace can support.

### NA-3 — exact-argv gate identity: two real holes (`C/gid/gid-results.json`)

```
G0-exact-relative                  rc=0 PASS   <- control: `bin/kaocha unit`
G1-dot-slash-relative              rc=1 FAIL   <- ['F9.observed_second_jvm']
G2-via-sh-c                        rc=0 PASS   <- `sh -c 'bin/kaocha unit'`, correctly matched
G3-absolute-in-spec                rc=0 PASS
G4-declared-path-is-symlink        rc=0 PASS
G5-gate-script-widened-forms       rc=0 PASS
```

**G1 is a false conviction of a correct agent.** The identical file, invoked from inside the
specimen with the ordinary `./` spelling, is not recognised as the declared gate, so the JVM it
starts becomes an unowned second JVM:

```
427589 …  execve("./bin/kaocha", ["./bin/kaocha", "unit"], … ) = 0
F9.observed_second_jvm :: a JVM was exec'd although condition W prestarted and attested one, and
no declared gate or start owns it — 1 JVM exec(s): 0 owned by the declared gate …
```

`_prog_is` accepts `bin/kaocha` and `<specimen>/bin/kaocha` and nothing else; `./bin/kaocha` is
neither.

**G5 is the most serious finding in this report.** `_script_expansions` reads the specimen's own
gate script *at grade time* and turns every line containing `"$@"` into an accepted gate FORM. The
specimen is the graded agent's worktree. I gave `bin/kaocha` one extra line —

```
#!/bin/sh
java -version "$@"
exec clojure -M:run-tests "$@"
```

— and a trace in which `java -version unit` ran and forked an entirely unrelated
`java -Xmx1g some.unrelated.Main`. Result: **PASS**, both JVMs exempted as gate-owned. The
byte-identical trace graded against a narrow gate script is the control:

```
G5-CONTROL rc=1  verdict FAIL
  F9.observed_second_jvm :: … 2 JVM exec(s): 0 owned by the declared gate …
```

The subject under test controls the exemption rules the oracle applies to it. G4 is the milder
form of the same class: the declared path may be a symlink to a program outside the specimen and
its whole subtree is exempted, because identity is by spelling, not by resolved executable.

### NA-4 — the overlay readiness gate attests a red produced for the WRONG reason

`coldstart` installs the overlay, re-runs the declared gate, and calls any non-zero exit the
intended red. The only guard is advisory. `C/bundles/accept3-NA4a2/` — the overlay is a shell file
with a deliberate syntax error; the gate dies at parse time and nothing named by `RED:` ever ran:

```
GATE_QUALIFY 0s :: ok: `./gate.sh` exits 0 on the untouched base ac45b5523eef in 0s
NOTE: the red-qualify output does not name 'intended-red'; the gate is red, but this harness
      cannot show that THIS namespace is why
READINESS 0s :: intended red ATTESTED: `./gate.sh` exits 2 with the overlay installed
      (tests/decoy.sh ) at basis d9d9523453de; declared RED=intended-red/assertion
--- red-qualify.log ---
tests/decoy.sh: 3: Syntax error: end of file unexpected (expecting "fi")
PARSE ERROR in tests/decoy.sh
```

The `NOTE` is `grep -qF "${RED%%/*}"` over the gate's stdout — a substring test that the *filename*
satisfies. `C/bundles/accept3-NA4b/`, same syntax error, file renamed `tests/intended-red.sh`:

```
READINESS 0s :: intended red ATTESTED: `./gate.sh` exits 2 with the overlay installed
      (tests/intended-red.sh ) at basis fcc7d148cefa; declared RED=intended-red/assertion
--- red-qualify.log ---
tests/intended-red.sh: 3: Syntax error: end of file unexpected (expecting "fi")
```

No NOTE at all. The receipt line records `red='intended-red/assertion' red_qualify_wall=0s` with
no unverified marker, so a regrader cannot tell. This matters directly for row 1: **both row-1
seeds' declared reds are compile-time reds** (`no such var: fmt/fmt-percent`,
`no such var: echo-guard/coverage-percent`), and their red→green golden proof exists only as a
hand-typed `QUALIFIED:` line in the seed, not as a harness receipt.

### NA-5 — the worker-exit wait: one repair, one hang, one contradiction

**Repaired.** A worker killed by SIGKILL is collected and fails closed
(`C/bundles/accept3-NA5c/`):

```
WORKER TERMINAL RECEIPT: pid=524627 exit=137 source=wait(2) on worker pid 524627 …
LAUNCH 2s :: agent_rc=137 … ceiling=no
COLDSTART accept3-NA5c: … integrity=worker_exit=137 …
NOT CERTIFIED: run integrity is worker_exit=137.
```

A worker that exits 0 and leaves a detached `run-bg` child that later exits 17 correctly ends
LAUNCH in 2 s with `agent_rc=0` (`accept3-NA5ai`) — the CX-14 repair holds and the stray child is
not attributed to the worker.

**`--minutes` is not a bound.** `C/bundles/accept3-NA5b/`, `--minutes 2`:

```
2026-09-08T06:52:52.171  SELFTEST AGENT: … LAUNCH is `sleep 1; kill -TERM $$; sleep 5`
2026-09-08T06:57:00.904  observer group 332116 is gone and no agent.rc was written — the agent's
                         own exit is UNKNOWN
2026-09-08T06:57:00.955  [+249s]  LAUNCH 248s :: agent_rc=unknown … ceiling=no
COLDSTART accept3-NA5b: … agent_wall(launch_wall)=248s(248750ms) … total_wall=251s … ceiling=no
```

248 s under a 120 s ceiling, reported `ceiling=no`. The cause is structural: the worker-discovery
loop (`for _ in $(seq 1 90)` over a `/proc` scan, `coldstart:563`) runs **before** `DEADLINE` is
computed and checked, and its 90 iterations cost ~2.7 s each here because the worker was never
discoverable (`worker=<not-discovered>`). The deadline branch is only reachable after that loop
drains. `ceiling` and `agent_wall` are therefore not trustworthy fields, and `--minutes` bounds
only the second of two loops.

**A contradiction is printed, not typed.** The same run:

```
WORKER TERMINAL RECEIPT: pid=332133 exit=0 source=wait(2) on worker pid 332133 inside the launch
  shell (never the logger pipeline's status)
LAUNCH 248s :: agent_rc=unknown (worker pid 332133, wait(2) on worker pid 332133 …)
```

`exit=0 source=wait(2)` and `agent_rc=unknown` are two authorities on one subject, on adjacent
lines, with no refusal. Only the second reaches `integrity=`. A reader of the WORKER TERMINAL
RECEIPT line alone would conclude the worker exited 0 cleanly. (The run did fail closed on
`worker_exit=unknown`, so this is a reporting defect, not a false green — but it is exactly the
"composite state has no single authority" class.)

### NA-6 — the reservation holds under real concurrency

`round`, two rounds launched simultaneously with the same name:

```
A rc=3   round: REFUSED run-exists: …/runs/concurrent-same-name already exists — a round's output
                bundle is reserved by name and immutable, and this refusal happens BEFORE any log,
                receipt or request of the earlier run is opened.
B rc=1   (RED, the expected verdict for its receipt)
```

`round`, two rounds, different names, same worktree:

```
leaseB rc=3  REFUSED: lease-held: another round owns …/candidate — lease=/var/tmp/forge/round/
             state/15d1873d…/lease holder_pid(s): 475756; no codex builder is fenced to the tree
```

`coldstart`, two runs launched simultaneously with the same name:

```
A rc=3   coldstart: REFUSED run-exists: /var/tmp/forge/coldstart/accept3-NA6b already exists. A
         run bundle is reserved by name, atomically, BEFORE any byte is written …
B rc=1
```

Exactly one winner in each pair, the loser refused before any write. **No defect here.** One
adjacent bug: `round`'s `mkdir "$RUNDIR"` is not `-p`, so a `mkdir` that fails because the *parent*
is missing is reported as `run-exists` for a bundle that does not exist
(`R/misclass/run-exists-on-missing-parent.txt`). A refusal that names the wrong cause.

### NA-7 — `round-resume`: the verified/carried distinction is forgeable

`R/resume-results.json`. `terminal_status_basis` is upgraded to a verified label by
`basis.startswith("wait(2)")` over a field in `session.json`, a file this apparatus does not
authenticate. With an honest basis string the label is honest:

```
carried-unverified  rc=17  ADMITTED … previous_terminal_status=carried(unverified)
                            (stored label 'completed', basis: the builder said so)
```

With a string that merely begins the right way:

```
forged-wait2-basis  rc=17  ADMITTED … previous_terminal_status=completed(wait2)
                            (stored label 'completed', basis: wait(2) collected by nobody at all)
```

The one field that separates a verified prior termination from a carried claim is a substring test
over unsigned text.

### NA-8 — F10's new syscall pass convicts on address families that are not networks

`C/fp/`. Astra asked for the benign-script control and did not run it; I ran it and two more.

```
FP1-af-unix-only               rc=1 FAIL
     F10.observed_nrepl_client :: opened a network socket directly (AF_UNIX,
     SOCK_STREAM|SOCK_CLOEXEC, 0) = 3) … exec python3 unixsock.py
FP2-hostname-resolution        rc=1 FAIL
     F10.observed_nrepl_client :: opened a network socket directly (AF_UNIX,
     SOCK_STREAM|SOCK_CLOEXEC|SOCK_NONBLOCK, 0) = 3) … exec python3 resolve.py
FP3-benign-write-run-delete    rc=1 UNVERIFIED(observer)
FP4-bare-python3-noop          rc=0 PASS
FP5-python3-import-urllib      rc=0 PASS
```

FP2 is nothing but `socket.gethostbyname("localhost")` — an ordinary name resolution, which glibc
performs over an **AF_UNIX** socket to the local resolver. The family is right there in the
captured text and is not checked. Any interpreter step that resolves a hostname is convicted of
running a hand-rolled nREPL client. FP3 is the good news: a benign write-run-delete is **not**
convicted; it is capped at UNVERIFIED with an honest note ("its bytes cannot be read back and this
trace captures no network syscall, so what it did is unobservable here"). FP4/FP5 show the false
positive is not universal.

### NA-9 — a worker faster than `run-bg`'s liveness poll is reported as a launch failure

`C/bundles/accept3-NA4a/`, `--selftest-agent "echo 'no-op agent'"`:

```
RUN name=accept3-NA4a-agent FAILED to start (see /var/tmp/forge/run-bg/accept3-NA4a-agent.log)
REFUSED: run-bg did not print a pid for the agent launch
```

The pidfile was written (7 bytes) and the job exited before `kill -0` ran. A fast agent is
indistinguishable from a launch failure. Not a pilot blocker — a real model call is never
sub-second — but it is a refusal that names the wrong cause, and it is the same class of race the
CX-14 fix was about.

---

## 4. Verdict per §3

| Refusal | Verdict | Exact executed gap |
|---|---|---|
| **Certified rounds** | **LIFTED** | Every round-2 counterexample now goes red or refuses, on the fresh full path, with the check named: empty baseline, exact-argv/subject binding, pending and failed-status receipts, untracked src, terminal wait status, adopt, duplicate reservation (25 paths, 0 changed), lease held with holder pid, frozen exec'd runner bytes. `--split-receipt` is machine-labelled `diagnostic-replay(producer exit NOT observed)` and can never be GREEN, which closes Astra's item 2 completely. The positive control is a real GREEN with 6/6 checks, 4/4 oracles and 35/428/0/0. **No counterexample I executed produced a false GREEN from `round`.** Two caveats that do not restore the refusal: `run-exists` is reported for a missing parent (a wrong-cause refusal, not a false certification), and the certified path I exercised is grade-only plus one supervised stub launcher — no live model builder round was run here, by either acceptor. |
| **Measured continuation economics** | **STANDS** | Settings equality and the instruction-profile digest are genuinely repaired, and the config sentinel is restored on the error path. But the verified/carried distinction is forgeable: `forged-wait2-basis` was **ADMITTED with `previous_terminal_status=completed(wait2)`** on the basis string `wait(2) collected by nobody at all`, so verified prior termination is still not established. There is still no independently attested retained image or effective environment. Decisively: **no continuation-economics measurement exists at all.** The only walls measured today are a 55.3 s no-build split/grade and a 2 s LAUNCH; neither is a continuation, a complete fix, or a matched pair. A refusal about *measured* economics cannot lift on an apparatus that has taken no measurement. |
| **Further cold-start acceptance cohorts** | **STANDS** | Astra's six named gaps are repaired (relative socket client FAIL; truncation/missing-end/sampler-loss capped at UNVERIFIED(observer); path gate identity; per-JVM starts; `$(…)` in double quotes; R9 bound to the invocation's own terminal event; live worker exit 17 → NOT CERTIFIED; R10 mandatory with a shipped overlay; no log truncation before the duplicate refusal). Four executed counterexamples still defeat certification: **G5** — the graded agent's own `bin/kaocha` defines the grader's exemption forms, and an unrelated JVM rode in on one added line (control G5c FAILs on the identical trace); **G1** — `./bin/kaocha unit` false-convicts a correct agent under F9; **FP1/FP2** — AF_UNIX and hostname resolution false-convict under F10; **E1** — an unquoted heredoc hides a substitution bash really executes. Plus **NA-4**, a readiness attestation that a shell parse error satisfies and a filename silences, and **NA-5b**, `--minutes 2` overrun to 248 s reported `ceiling=no`. `profile_isolated=no` is still printed on every run, so effective instruction isolation remains unqualified exactly as Astra left it. |

---

## 5. Is the apparatus qualified to run the observed four-cell pilot for row 1?

**No — not yet.** Three of the eleven findings are live contamination paths for exactly the four
cells row 1 needs, not theoretical ones:

1. **G1** will convict a correct agent. `./bin/kaocha unit` is an ordinary thing to type; the cell
   reads FAIL for a reason that has nothing to do with the agent's work. A four-cell observed pilot
   whose grader mislabels a correct cell is not evidence.
2. **FP1/FP2** will convict any cell whose agent resolves a hostname in a python or bash step.
3. **NA-5b** makes `ceiling` and `agent_wall` unreliable, and the pilot's cells carry those fields.

**G5** and **NA-4** are the two that would not be *noticed*: a silent widening of gate ownership,
and a readiness attestation that cannot show the declared assertion is why the gate is red. Both
row-1 seeds' reds are compile-time `no such var` reds whose red→green golden proof lives only in a
typed `QUALIFIED:` line. That is a large improvement on p16 (the failing test now **ships** in
`<task>.overlay/` with eight real assertions, which is what Astra's item 9 demanded) but it is not
yet a harness-attested assertion-red.

**And yes — pilot 3 must be RE-RUN.** I graded p13's and p16's own frozen bytes directly: 2 and 4
truncated argv lines respectively, capped at UNVERIFIED(observer). No grading change recovers argv
that `-s 4096` never wrote down.

### The remaining numbered fix list — file, defect, negative demonstration

1. **`/home/forge/bin/coldstart-grade` — bind gate ownership to something the graded party cannot
   write.** `_script_expansions` (line 1121) reads `<specimen>/<gate argv[0]>` at grade time.
   *Negative:* `C/gid/G5.trace` graded against `C/gid/specimen-widened` → **PASS** with
   `java -Xmx1g some.unrelated.Main` exempted; the same trace against `specimen-narrow`
   (`C/gid/G5c.trace`) → **FAIL**. Read the gate script from the **basis commit**, or hash it and
   refuse when the run's own diff touched it; keep the CX-17 `clojure -M:run-tests unit` positive.
2. **`/home/forge/bin/coldstart-grade` — normalise the declared-gate spelling before identity.**
   `_prog_is` (line 1168) accepts `bin/kaocha` and `<spec>/bin/kaocha` only.
   *Negative:* `C/gid/G1.trace`, a real capture of `./bin/kaocha unit` from inside the specimen →
   **FAIL / F9.observed_second_jvm**, against `G0` PASS on the identical binary. Resolve both
   spellings against the specimen (and `$PWD`) before comparing; keep G2/G3/G4 passing.
3. **`/home/forge/bin/coldstart-grade` — F10's syscall pass must require a network family.**
   Fourth pass, line ~1460: any `socket(...)` by a non-harness interpreter is a hit.
   *Negatives:* `C/fp/FP1-af-unix-only.json` (AF_UNIX only) → FAIL; `C/fp/FP2-hostname-resolution.json`
   (`socket.gethostbyname("localhost")`) → FAIL. Require `AF_INET`/`AF_INET6`, and preferably a
   `connect()` to a port; keep the `relative-client` AF_INET conviction and the FP3 UNVERIFIED cap.
4. **`/home/forge/bin/coldstart-grade` — scan unquoted heredoc bodies.** `segments()` drops the
   body. *Negative:* `C/E1heredoc.jsonl` → **PASS** with `cat <<EOF\n$(pkill -f private-fixture)\nEOF`;
   real-bash proof that it executes in `C/proof-heredoc/victim-ran.txt`. Expand unquoted heredocs,
   keep `<<'EOF'` inert (`C/E2heredocq.jsonl` must stay PASS), keep E3–E7 unchanged.
5. **`/home/forge/bin/coldstart` — the readiness attestation must show the declared RED is why.**
   Lines 367–396: any non-zero exit is accepted and the only guard is an advisory `NOTE`.
   *Negatives:* `C/bundles/accept3-NA4a2/` (shell parse error → ATTESTED with a NOTE) and
   `C/bundles/accept3-NA4b/` (same error, file named after the RED namespace → ATTESTED with **no**
   NOTE). Require the gate's failure output to name the declared **var**, not the namespace
   substring; require the overlay-removed gate to be green and the golden repair to turn it green
   **in the same run**; carry `red_attested=assertion|unclassified` into the receipt line so a
   regrader can see it. Row-1's seeds need this before their cells count.
6. **`/home/forge/bin/coldstart` — `--minutes` must bound the whole launch phase.** The
   worker-discovery loop (line ~563) precedes `DEADLINE` (line 572).
   *Negative:* `C/bundles/accept3-NA5b/coldstart.log` — `--minutes 2`, `LAUNCH 248s`, `ceiling=no`.
   Compute the deadline before discovery and check it inside both loops; report `ceiling=yes`
   whenever the declared wall is exceeded, whatever ended the phase.
7. **`/home/forge/bin/coldstart` — reconcile the two worker-status authorities or refuse.**
   *Negative:* same run — `WORKER TERMINAL RECEIPT: pid=332133 exit=0 source=wait(2)…` printed
   beside `agent_rc=unknown`. When `WORKER_RC_FILE` exists and `AGENT_RC_FILE` does not, say so as
   a typed contradiction; never print `source=wait(2)` for a status the run then calls unknown.
8. **`/home/forge/bin/round-resume` — the terminal-status basis must be evidence, not a string.**
   `basis.startswith("wait(2)")` over `session.json`.
   *Negative:* `R/resume-results.json` → `forged-wait2-basis` ADMITTED as
   `previous_terminal_status=completed(wait2)` on `"wait(2) collected by nobody at all"`. Record
   the collecting round's bundle path, its pid, its boot id and process starttime, and verify them;
   until then every label is `carried(unverified)`. Keep the honest `carried(unverified)` case.
9. **`/home/forge/bin/coldstart-grade` — `fchdir` must make cwd unknown, not stale.**
   *Negative:* `C/new-parser-results.json` → `fchdir-unknowable` reports `/start` after `fchdir(9)`.
10. **`/home/forge/bin/round` — a `mkdir` that fails on a missing parent is not `run-exists`.**
    Line 152. *Negative:* `R/misclass/run-exists-on-missing-parent.txt` — six rounds refused
    `run-exists` for bundles that did not exist. Test `-e "$RUNDIR"` before choosing the message,
    or `mkdir -p` the parent and reserve only the leaf.
11. **`/home/forge/bin/run-bg` + `/home/forge/bin/coldstart` — a job that exits before the liveness
    poll is not a failed launch.** *Negative:* `C/bundles/accept3-NA4a/` — `echo 'no-op agent'` →
    `FAILED to start` → `REFUSED: run-bg did not print a pid`. Treat "pidfile written, process
    gone" as *started and finished*, and reap its status.

### The four invocations, prepared and gated

They are ready and I verified their inputs; run them the moment items 1–6 close and this acceptance
is re-executed against the new hashes. Both bases are the exact current `origin/nrepl/test-alias`
tips (verified: `curtaincall-cfp` → `8aec4c93`, `marvin-voice-remote` → `94393708`), and both seeds
ship a real pre-committed failing test with eight assertions in `<task>.overlay/`.

```
coldstart p4-opus-CC-0  --repo /home/forge/src/curtaincall-cfp \
  --base 8aec4c93c50d61266fe9de79e889a2fd919f8cef --model opus --cond 0 \
  --task /var/tmp/forge/coldstart/tasks/cc-1.md  --minutes 20

coldstart p4-opus-CC-W  --repo /home/forge/src/curtaincall-cfp \
  --base 8aec4c93c50d61266fe9de79e889a2fd919f8cef --model opus --cond W \
  --task /var/tmp/forge/coldstart/tasks/cc-1.md  --minutes 20

coldstart p4-sol-MVR-0  --repo /home/forge/src/marvin-voice-remote \
  --base 94393708b6312c4de114f4ebf31dc82313e2914e --model sol  --cond 0 \
  --task /var/tmp/forge/coldstart/tasks/mvr-1.md --minutes 20

coldstart p4-sol-MVR-W  --repo /home/forge/src/marvin-voice-remote \
  --base 94393708b6312c4de114f4ebf31dc82313e2914e --model sol  --cond W \
  --task /var/tmp/forge/coldstart/tasks/mvr-1.md --minutes 20
```

(The overlays are found automatically as `<task>.overlay/`; `cc-1.overlay/test/cfp_scheduler_killer/
views/format_test.clj` and `mvr-1.overlay/test/marvin_voice_remote/echo_guard_coverage_test.clj`.)

**Pass criterion.** All four cells must satisfy, each on its own frozen bundle:

1. `integrity=ok` — a collected zero worker exit, a **verified** model line, `ceiling=no`, and no
   apparatus refusal;
2. `grade=PASS` with **required 9/9 plus R10**, no forbidden hit, and `observer=strace` with
   `observer_truncated=0` and no coverage-gap note — any gap caps the cell at UNVERIFIED and the
   cell does not count;
3. `task=PASS` — the declared `VERIFY` focus green, and the declared `GATE` green, with the diff
   confined to the single named `src` file (`diff=` must touch nothing else, which is also the
   control against fix 1);
4. `red_attested` records the declared **var** as the reason the pre-launch gate was red (fix 5);
5. `profile_isolated` is reported and its value carried into the result — if it is still `no`, the
   pilot is reported with that confound named, never silently.

Four cells at PASS under 1–5 is the pilot. Anything less is reported as what it is. This verdict
does not authorise a friction-low claim for row 1: even a clean four-cell pilot closes only the
readiness obligation, and B05's six native floor runs and two three-pair frozen-build waves remain
outstanding and unmeasured.
