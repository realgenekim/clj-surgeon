## GO-WITH-FIX

Round-two independent review of `bridge/tmp-leak-ratchet` at **86bd9de3** (temp-dir hygiene ratchet).
Reviewer: Opus, forge@anvil, 2026-09-04. Sol's content filter refused this brief; paths substituted.

**Provenance.** Clone `/home/forge/tmp/sol/tmpleak1-wt`, HEAD proven and tree clean before and after:

```
$ cd /home/forge/tmp/sol/tmpleak1-wt && git rev-parse HEAD && git status --porcelain
86bd9de3c831c1b69e7eb2175bea8f33a26d0ab6
(no output — clean)
```

Nothing in the clone was modified, committed, stashed or pushed. All sabotage was performed on a
`git archive 86bd9de3` export under `/var/tmp/forge/tmpleak2-review-fx/opus`, restored byte-identical
after each arm. No server contacted. No entry under `/tmp` or `/var/tmp/forge` that I did not create
was touched.

**Headline.** All nine round-one findings — including the blocking one — are closed, and I closed
them by execution, not by reading the diff. The mechanism is now genuinely hard to defeat: I reverted
six of the closures one at a time on the export and the new gate went RED on every one, each with a
named diagnostic. One new finding remains (the mounts-file seam grants a PASS), and it is the reason
this is GO-WITH-FIX rather than GO.

---

## Round one's findings, re-driven

| # | Round-one finding | Status at 86bd9de3 | Evidence |
|---|---|---|---|
| 1 | **BLOCKING** — tmpfs check fails open on unknown fstype; suite ran on `/tmp` | **CLOSED** | arm 4 below, exit 97 |
| 2 | Inherited sentinel makes the sweep delete the SHARED base | **CLOSED** | S1 below, base intact |
| 3 | Subprocess temp dirs escape the isolated root | **CLOSED** | `mktemp -d` now lands inside the root and is caught |
| 4 | Re-exec discards the parent's heap ceiling (512 MB → 7.8 GB) | **CLOSED** | parent 512 = child 512 |
| 5 | Re-exec drops test-selection args | **CLOSED** | args preserved incl. one containing spaces |
| 6 | Isolated root leaks permanently when the parent is killed | **CLOSED** | SIGTERM → root swept, exit 143 |
| 7 | Only 2 of 5 test entry points gated | **CLOSED** | all five exit 97 on `TMPDIR=/tmp` |
| 8 | Unwritable base → raw stack trace | **CLOSED** | typed `tmp-refused:` + exit 97 |
| 9 | Residual hard-coded `/tmp` in Makefile / heap gate | **CLOSED** in scope | see finding 2 for what remains |

### The measured facts, all reproduced

Both JDK/Substrate facts hold exactly as documented, and I add a correction against **my own**
round-one report. Round one told the builder to repair the procfs fallback with
`(line-seq (io/reader ...))`. **That advice was wrong** — it throws too. The builder found the one
approach that works and said so:

```
########## bb ##########                    ########## java (clojure -M) ##########
slurp          => THROWS: java.io.IOException - Invalid argument       (same)
readAllBytes   => THROWS: java.io.IOException - Invalid argument       (same)
line-seq io/reader => THROWS: java.io.IOException - Invalid argument   (same)
Files/lines    => OK lines/bytes: 41                                   OK lines/bytes: 41
```

`java.nio.file.Files/lines` is the only one of the four that reads `/proc/mounts`, on **both**
runtimes. The fallback at `tmp_leak_support.clj:130` is now live code, not the dead code round one
found. Facts 1a/1b/1c and fact 2 reproduce verbatim (bb ignores `JAVA_TOOL_OPTIONS` and `TMPDIR`,
prints `/tmp`; a literal `-D` at bb's own invocation works; a runtime `System/setProperty` moves
`getProperty` but real `createTempDirectory`/`createTempFile` keep writing to the startup value, on
bb **and** on a real JVM).

### The refusal gauntlet — every arm the addendum names

Driven through the real `secure-tmpdir!` via `test/tmp_leak_probe.clj`:

```
1 TMPDIR=/tmp (the original mistake)          → EXIT=97  ram-path-prefix
2 TMPDIR=/dev/shm                             → EXIT=97  ram-path-prefix
3 TMPDIR=/run/user/1002 (tmpfs, NOT by name)  → EXIT=97  tmpfs, fstype=tmpfs
4 findmnt dead + mounts file absent (arm B)   → EXIT=97  UNDETERMINABLE filesystem type
5 findmnt dead, real /proc/mounts, tmpfs base → EXIT=97  tmpfs (fallback carried it)
7 symlink ext4-path -> /tmp                   → EXIT=97  ram-path-prefix (canonicalised)
8 TMPDIR=/tmp/../var/tmp/forge                → EXIT=97  ram-path-prefix (over-refusal; see note)
```

**Arm 4 is round one's blocking arm B, and it now refuses.** That was the single NO-GO.

### The sentinel

```
S1 inherited sentinel naming the SHARED BASE  → EXIT=97 sentinel-mismatch
   --- fakebase AFTER ---  other-seat-fixture / other-seat-precious.txt   (both intact)
S2 sentinel naming an ANCESTOR of the root    → EXIT=97 sentinel-mismatch
S3 sentinel with a TRAILING SLASH             → EXIT=0  accepted (canonical equality)
S4 sentinel via a SYMLINK equal to the root   → EXIT=0  accepted (canonical equality)
```

Round one's finding 2 was destructive data loss on another tenant's working set. It is gone: the
decoy base survived intact.

### Lifecycle, isolation, forwarding

```
SIGTERM the parent mid-run:  parent exit=143 ; --- roots after SIGTERM --- (none — swept) ; no stray child
subprocess `mktemp -d`:      PROBE subprocess-tmpdir=<root>/tmp.KfIaGcC4nL
                             temp-leak: 1 entries left under <root>: tmp.KfIaGcC4nL   EXIT=1
                             --- entries left directly in the SHARED BASE --- (none)
unwritable base (chmod 500): tmp-refused: ... cannot be used as a temp base: AccessDeniedException  EXIT=97
JVM flags + argv (java):     PROBE role=parent max-mb=512 args=["argA" "arg with spaces"]
                             PROBE role=child  max-mb=512 args=["argA" "arg with spaces"]
                             parent-jvm-options -> ["-Xmx512m" "-Dmy.custom.flag=KEEPME"]
                             ROLE child my.custom.flag= KEEPME maxMB= 512
JVM flags (bb):              parent max-mb=25061  child max-mb=25061   (equal)
```

A non-tmpdir `-D` survives the re-exec, the heap ceiling survives, and an argument containing spaces
survives unsplit.

### The startup sweep guard — it cannot take a live run or a stranger

Planted six entries under a scratch base, each designed to be a different trap:

```
--- BEFORE ---                            --- AFTER (livepid=3862668 deadpid=4194301) ---
clj-surgeon-suite-3862668-aaaaaaaa (old)  clj-surgeon-suite-3862668-aaaaaaaa    ← LIVE pid, kept
clj-surgeon-suite-4194301-bbbbbbbb (old)  (swept — dead pid + old)
clj-surgeon-suite-4194301-cccccccc (new)  clj-surgeon-suite-4194301-cccccccc    ← too fresh, kept
clj-surgeon-suite-legacyhex        (old)  clj-surgeon-suite-legacyhex           ← no pid in name, kept
other-seat-fixture                 (old)  other-seat-fixture                    ← not ours, kept
other-seat-file.txt                (old)  other-seat-file.txt                   ← not ours, kept
```

Exactly one of six swept, and it is the only one that should be. On pid reuse: `/proc/sys/kernel/pid_max`
is **4194304** here, so reuse inside the 4 h window is remote, and reuse fails **safe** anyway —
a reused pid reads as alive and the root is kept, never deleted.

### The leak witness goes RED, with the entries named

Planted four leak species into a real namespace `clj-surgeon.tmp-leak-support-test` on the export and
ran the real bb suite:

```
Ran 823 tests containing 6755 assertions.
0 failures, 0 errors.
temp-leak: 3 entries left under /var/tmp/forge/clj-surgeon-suite-4045410-07166237: PLANT-A-plain-6899693897987528365, PLANT-C-deleteOnExit-1726209853449246386.txt, PLANT-D-srcstyle-96901031728637110.txt
SABOTAGED BB EXIT=1
```

Plain `createTempDirectory`, `.deleteOnExit` and `createTempFile` are all caught and named with
**zero clojure.test failures** — i.e. the leak witness is the only thing that fails the run, which is
the point. The fourth species (subprocess `mktemp`) is caught separately, shown above.

### Sabotage: six reverted closures, six RED gates

I reverted each closure on the export, one at a time, and ran `sh test/tmp_leak_ratchet_test.sh`:

```
A: fail-OPEN on unknown fstype   → GATE EXIT=1  "3d: an undeterminable fstype must exit 97, got 0"
B: child env drops TMPDIR        → GATE EXIT=1  "5a: a subprocess temp dir ESCAPED the isolated root: ..."
C: JVM flags not forwarded       → GATE EXIT=1  "6a: the re-exec discarded the parent's heap ceiling: parent=318 MB child=7832 MB"
D: sentinel believed blindly     → GATE EXIT=1  "4a: an unowned sentinel must exit 97, got 0"
E: sweep ignores pid liveness    → GATE EXIT=1  "7b: a root whose owning process is STILL ALIVE was swept"
F: hard-coded /tmp in a gate     → GATE EXIT=1  "10: hard-coded /tmp write targets remain in Makefile / test shell gates"
```

This is the part that earns the verdict. Round one's witnesses were a unit test of a predicate and
the assertion that a green run proves the accepted path. These execute the refusing half, and they
fail first when the mechanism is removed.

---

## FINDINGS

### 1. `CLJ_SURGEON_MOUNTS_FILE` is not just a witness seam — it grants a PASS, and the suite really does run on RAM (fix before merge)

`test/clj_surgeon/tmp_leak_support.clj:101-106, 145-153`. `mount-fstype` is
`(or (findmnt-fstype dir) (mounts-table-fstype dir) :unknown)`, and `mounts-table-fstype` reads
whatever `CLJ_SURGEON_MOUNTS_FILE` names. A caller who supplies a forged table **and** a `findmnt`
that cannot answer gets a positive proof-of-disk out of a check whose entire job is to refuse
without one.

Proven end to end on a tmpfs I can actually write (`/run/user/1011`, mode 700, mine):

```
###### SEAM ESCAPE: findmnt DEAD + lying mounts file, base = a REAL WRITABLE tmpfs ######
PROBE role=child max-mb=25061 tmpdir=/run/user/1011/opus-seam-escape/clj-surgeon-suite-3835988-4c8a65d8
PROBE root=/run/user/1011/opus-seam-escape/clj-surgeon-suite-3835988-4c8a65d8
PROBE leak-exit=0
EXIT=0
--- df of that path (proof it is RAM) ---
tmpfs           3.1G   12K  3.1G   1% /run/user/1011

--- control: same base, seam UNSET, findmnt dead ---
tmp-refused: java.io.tmpdir base=/run/user/1011/opus-seam-escape is RAM-backed (tmpfs, fstype=tmpfs).
EXIT=97
```

The control is the important half: remove the seam and the identical base is refused. The seam is
what turned the refusal into a run.

**What it is not.** It cannot be used to bless `/tmp` or `/dev/shm` — I tried, and `literal-ram-path?`
refuses those by name before any fstype is consulted:

```
###### 6b SEAM + findmnt DEAD, base=/tmp (lying table says ext4) ######
tmp-refused: java.io.tmpdir base=/tmp is a RAM-backed path by name (/tmp or /dev/shm).   EXIT=97
```

And with `findmnt` present the seam is inert, because findmnt is consulted first:

```
###### 6c SEAM ALONE (findmnt ALIVE), base=/run/user/1002 ######
tmp-refused: ... is RAM-backed (tmpfs, fstype=tmpfs).   EXIT=97
```

**Why it still has to be fixed.** This is the seat's own scar, verbatim — *a gate a caller can turn
off*: I once ratified `verify:none` as a legitimate waiver because a prompt mandated the strict mode,
and agents turned verification off 3/3 and committed. An environment variable that converts
"I cannot prove this is disk" into "proven disk" is that same shape, in the one function whose
contract is `fails CLOSED: every path out of this function that is not a positive proof of real disk
is a refusal` (`base-refusal` docstring, line 172-173). The docstring is currently false in the
presence of the seam.

**Fix, one line, and it costs the witness nothing.** The gate only ever points the seam at a
*nonexistent* path, to exercise the "no source can answer" branch — it never needs the seam to
produce a PASS. So make the seam incapable of producing one: when `CLJ_SURGEON_MOUNTS_FILE` is set,
let a `tmpfs` answer refuse as normal but treat any non-tmpfs answer as `:unknown` (i.e. still a
refusal). Alternatively, consult the seam only for `/proc/mounts`-absent simulation and never let a
seam-sourced fstype satisfy `base-refusal`. Either keeps every current gate assertion green — I
checked arm 3d's expectation, which is a refusal, not a pass.

**Why this is GO-WITH-FIX and not NO-GO.** The brief's blocking bar is "a suite that can still run
with its temp dir on a tmpfs." Read literally, this arm clears it. I am not calling it blocking
because the bar exists to catch the **accident** that burned the box, and every accidental path is
now closed and proven closed: no missing binary, no unusual mount, no unset variable, and no
plausible misconfiguration reaches RAM any more — round one's arm B needed nothing but an absent
`findmnt`, and this one needs an operator to hand the check a forged filesystem table. That is
self-sabotage, not a mistake. Fix it before merge; it does not warrant another round.

### 2. `make test` reaches a bench harness that hard-codes `/tmp` and ignores `TMPDIR` — the declared exception is one script wider than declared

`bench/retain_benchmark_result.sh:116`:

```
root=$(mktemp -d /tmp/clj-surgeon-retention-self-test.XXXXXX)
```

The design doc (`temp-dir-hygiene-design.md:144-153`) declares `bench/*.sh` internals out of scope on
the grounds that "those scripts are not required by `test/run_all.clj` or `mcp_test_runner.clj` and
are not covered by this leaf's gates." True for those two runners — but the `test:` target itself runs

```
bash bench/retain_benchmark_result.sh --self-test
bash bench/retain_benchmark_result.sh --verify-tracked
```

so `make test` does create a directory in RAM by name, with `TMPDIR` set to `/var/tmp/forge` and
ignored. **Ruling: the exception stands, the sentence justifying it does not.** It is one directory,
it is removed by `trap ... EXIT` at line 121, and it is not the inode-fire class — but the doc should
say "reached by `make test`, self-cleaning via trap" rather than "not covered by this leaf's gates,"
because a reader who trusts that sentence will not look. The full fix is a `${TMPDIR:-/var/tmp}` on
that one `mktemp` plus `bench/*.sh` added to the MCP-OP-TMPHYG-010 grep — cheap enough that I would
take it now rather than file it.

### 3. The MCP-OP-TMPHYG-010 audit explicitly exempts `${TMPDIR:-/tmp}`, which is RAM whenever `TMPDIR` is unset

`test/clj_surgeon/tmp_leak_support_test.clj:114` — `(not (str/includes? line "TMPDIR:-/tmp"))` — and
the same exemption in the shell scan. Nine live sites use that form, four of them under `test/` and
one in the Makefile:

```
test/performance_regression_sentinel_runner_test.sh:6   test/performance_regression_sentinel_intent_test.sh:17
test/direct_cclsp_client_audit_test.sh:5                test/relation_causal_cohort_runner_test.sh:6
Makefile:762
```

Under `~/bin/suite-run` (which sources `seat-tmp-guard.sh`) `TMPDIR` is always set and these are
correct. A bare `make test` in a shell without the guard writes them to RAM. The blast radius is
small — self-cleaning `mktemp` roots, and the Clojure runners refuse and abort the run shortly after —
but the exemption is doctrinally backwards: the audit is written to permit the exact fallback that
names the RAM path. Change the fallback to `/var/tmp` in those five places and delete the exemption;
the ratchet then means what it says.

### 4. `SELF_TEST_TMP` inherits `TMPDIR=/tmp` with no refusal

`Makefile:39` — `SELF_TEST_TMP ?= $(or $(TMPDIR),/var/tmp)`. The default is right, but if `TMPDIR` is
`/tmp` the Make layer propagates it happily; only the Clojure layer refuses. Same class as finding 3,
same one-line remedy shape (`$(if $(filter /tmp,$(TMPDIR)),/var/tmp,...)` or an explicit refusal).

### 5. `sweep-root!` counts a delete it did not perform

`tmp_leak_support.clj:273` — `(do (try (fs/delete-tree root) (catch Throwable _ nil)) true)`. A root
owned by another user (or otherwise undeletable) returns `true` and is counted in
`sweep-stale-roots!`'s return. Cosmetic today — nothing consumes the count — but it is a receipt that
names a subject it did not act on. Return `(not (.exists root))`.

### 6. Two notes, neither a defect

- **Over-refusal.** `TMPDIR=/tmp/../var/tmp/forge` refuses as `ram-path-prefix` even though it
  canonicalises to `/var/tmp/forge`, because `literal-ram-path?` tests the raw string too. It errs
  toward refusal, which is the correct direction; I mention it only so the next person who hits it
  does not read it as a bug.
- **The intent audit's implementation witnesses for -004/-006/-007/-008 are Makefile markers, not
  code.** The commit message for 86bd9de3 says so plainly, and the reason is structural (the audit
  scans `src/**` + `Makefile`; this mechanism lives under `test/`). Worth naming because a
  marker-presence audit is not a ratchet: the audit would stay green with `tmp_leak_support.clj`
  gutted. What actually protects this leaf is `tmp-leak-ratchet-self-test`, and I proved that goes RED
  on six different mutilations. The composite is sound; the audit alone is not the assurance.

---

## Gates, verbatim

```
$ ~/bin/suite-run bb test/run_all.clj
Ran 822 tests containing 6752 assertions.
0 failures, 0 errors.
BB EXIT=0                                    (no temp-leak line)

$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test          ×3
run 1: Ran 712 tests containing 8437 assertions.  0 failures, 0 errors.
run 2: Ran 712 tests containing 8437 assertions.  0 failures, 0 errors.
run 3: Ran 712 tests containing 8437 assertions.  0 failures, 0 errors.
                                             (no temp-leak line, all three)

$ sh test/tmp_leak_ratchet_test.sh
tmp-leak ratchet witness passed              real 0m44.7s   GATE EXIT=0

$ make mcp-operation-oracle
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]   EXIT=0

$ make repository-hygiene
repository hygiene: no machine-local build cache is tracked at any depth                        EXIT=0
```

The builder's claimed counts (822/6752 and 712/8437) match to the assertion. **The unreproduced
mcp-test failure the builder saw under load 14 did not recur in three consecutive runs**; I cannot
confirm or refute it, and it left no trace here.

**The `make test` EXIT=2 attribution is correct.** `gtimeout` is not installed on this box (it is the
macOS coreutils name), `bench/run_clean_claude.sh` requires it at line 133, and the file is byte-identical
between the tip and a clean `origin/MCP/main` archive:

```
$ (cd <clean origin/MCP/main archive> && CLAUDE_BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_claude.sh)   TRUNK EXIT=1
$ (cd <branch tip>                    && CLAUDE_BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_claude.sh)   TIP   EXIT=1
$ diff -q <trunk>/bench/run_clean_claude.sh bench/run_clean_claude.sh
SAME
```

Scope of that check, stated honestly: I proved the *cause* is pre-existing and identical on trunk. I
did not run the full `make test` to observe the aggregate `2`.

## Mergeability

**Trunk has moved.** `origin/MCP/main` is now **96d12a45bcedca87ce0ccc800c6ebbe01bac4ebf**, not the
`96b35b4b` the builder reported against — the conflict shape is unchanged, but the report was against
a stale base.

```
$ git merge-tree --write-tree HEAD origin/MCP/main
CONFLICT (content): Merge conflict in Makefile        (the only CONFLICT line; the only file listed)
$ git merge-file -p ours base theirs | grep -c '^<<<<<<<'
1
--- tokens only in OURS ---     tmp-leak-ratchet-self-test
--- tokens only in THEIRS ---   fanout-selftests
```

Confirmed: **one conflict, one hunk, one line** — the `.PHONY` list, ours adding
`tmp-leak-ratchet-self-test` and trunk adding `fanout-selftests`. Resolution is to keep both target
names. Nothing else conflicts.

## Fixture cleanup

```
$ ls -d /var/tmp/forge/clj-surgeon-suite-* 2>/dev/null   (none in /var/tmp/forge)
$ ls -d /tmp/clj-surgeon-suite-* 2>/dev/null             (none in /tmp)
$ ls -d /run/user/1011/opus-seam-escape 2>/dev/null      (seam fixture gone)
$ ls /var/tmp/forge/tmpleak2-review-fx                   (removed at end of review — see final ls)
```

---

## GO-WITH-FIX

**Mergeability:** this tip is GO for `origin/MCP/main` (trunk `96d12a45`) once finding 1's one-line
seam fix lands, subject to the single trivial `.PHONY` conflict resolved by keeping both
`tmp-leak-ratchet-self-test` and `fanout-selftests`.
