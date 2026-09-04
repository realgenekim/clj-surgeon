## NO-GO

Round-one independent review of `bridge/tmp-leak-ratchet` at **09ebaade** (temp-dir hygiene ratchet).
Reviewer: Opus, forge@anvil, 2026-09-04. Sol's content filter refused this brief; paths substituted.

**Provenance.** Clone `/home/forge/tmp/sol/tmpleak1-wt`, HEAD proven and tree clean:

```
$ cd /home/forge/tmp/sol/tmpleak1-wt && git rev-parse HEAD && git status --porcelain
09ebaadebfcf1b6674d40c6e53d9649d6382a6f9
(no output — clean)
```

Nothing in the clone was modified, committed, stashed or pushed. All sabotage was done on a
`git archive HEAD` export under `/var/tmp/forge/tmpleak1-review-fx/opus`. No server contacted.

**One blocking finding.** The brief's blocking bar is *"a witness that reports GREEN while a
fixture actually leaked, or a suite that can still run with its temp dir on a tmpfs."* Finding 1
is the second of those, reproduced end to end. Everything else is GO-WITH-FIX or better, and the
core mechanism is genuinely good — see "Confirmed in the builder's favour" below, which is the
longer list.

---

## BLOCKING

### 1. The tmpfs refusal FAILS OPEN, and the fallback written to prevent that is dead code on every Linux path — the suite really does run on tmpfs

`test/clj_surgeon/tmp_leak_support.clj:62-90`. `mount-fstype` tries `findmnt`, then falls back to
parsing `/proc/mounts`, and returns `nil` if neither can answer. `tmpfs?` (line 87-90) is
`(= "tmpfs" (mount-fstype dir))`, so **`nil` — "I could not determine the filesystem" — is treated
as "not tmpfs", and `secure-tmpdir!` proceeds.** A ratchet whose whole purpose is "make it
impossible to make this mistake again" fails open on the unknown case.

That would still be defensible if the documented fallback worked. It does not. `slurp "/proc/mounts"`
throws on this box, on **both** runtimes, for every path — so `mount-fstype`'s entire second branch
is unreachable dead code and the check is single-sourced on `findmnt` with zero redundancy:

```
$ bb -e '(try (println "len" (count (slurp "/proc/mounts"))) (catch Throwable e (println "THROWS:" (class e) (.getMessage e))))'
THROWS: java.io.IOException Invalid argument

$ bb -e '(try (println "len" (count (slurp "/proc/self/mounts"))) (catch Throwable e (println "THROWS:" (class e) (.getMessage e))))'
THROWS: java.io.IOException Invalid argument

$ clojure -M -e '(try (println "len" (count (slurp "/proc/mounts"))) (catch Throwable e (println "THROWS:" (class e) (.getMessage e))))'
THROWS: java.io.IOException Invalid argument
```

I replayed the exact fallback body from lines 73-85 in isolation to be sure it is the `slurp` and
not my transcription — every path returns the exception, i.e. the `(catch Throwable _ nil)` swallows
it and the function returns `nil`:

```
$ bb /var/tmp/forge/tmpleak1-review-fx/opus/probe2.clj
/proc/mounts exists? true
fallback /tmp               => "EX:Invalid argument"
fallback /tmp/whatever      => "EX:Invalid argument"
fallback /var/tmp/forge     => "EX:Invalid argument"
fallback /dev/shm           => "EX:Invalid argument"
fallback /home/forge        => "EX:Invalid argument"
```

**The end-to-end consequence, reproduced.** `probe3.clj` calls the real `secure-tmpdir!` with a
trivial child script as the re-exec target. Arm A is the box as it stands; arm B puts a `findmnt`
on `PATH` that cannot answer (exit 1) — exactly the "neither source can answer" case the fallback
was written for:

```
###### A: real findmnt present, TMPDIR=/tmp  (expect REFUSAL) ######
base = /tmp
tmpfs? = true
tmp-refused: java.io.tmpdir base=/tmp is RAM-backed (tmpfs). Launch with -Djava.io.tmpdir=/var/tmp/forge, ...
NOT REFUSED, returned: {:refused true}

###### B: findmnt shim fails (simulating no findmnt), TMPDIR=/tmp ######
base = /tmp
tmpfs? = false
CHILD RAN. java.io.tmpdir = /tmp/clj-surgeon-suite-b55ed840
```

Arm B is the blocking condition verbatim: **the suite ran, with its temp dir on the 16 GiB RAM
tmpfs that started this incident.** No refusal, no message, exit 0.

**Compounding: nothing witnesses the refusal branch.** The intent doc concedes it
(`docs/intent/temp-dir-hygiene/temp-dir-hygiene-design.md:97`): the named witness for
MCP-OP-TMPHYG-001 is `tmpfs-predicate-tells-ram-from-disk` — a unit test of the *predicate* — plus
"functionally, every green `~/bin/suite-run` invocation ... IS the accepted-path proof." The
accepted path is not the requirement. Nothing anywhere drives a runner to exit 97. Per the seat's
own rule (`fixes-add-lid-intents`), the witness must fail first and assert the behaviour *at* the
ceiling; here the refusing half of MCP-OP-TMPHYG-001 has never been executed by a test.

**Fix (all three, cheap):**
1. **Fail closed on unknown.** Return a tri-state from `mount-fstype` and refuse when the fstype
   cannot be determined, rather than coercing `nil` to "safe".
2. **Add the literal prefix refusal the seat's own guard already has.** `~/bin/seat-tmp-guard.sh:4`
   does `case "$TMPDIR" in /tmp|/tmp/*|/dev/shm|/dev/shm/*) ... exit 97;;`. That check needs no
   external binary, no procfs, and would have refused arm B. The Clojure ratchet omits it.
3. **Repair or delete the `/proc/mounts` fallback** — read it with a streaming reader
   (`(with-open [r (io/reader "/proc/mounts")] (line-seq r))`) instead of `slurp`, which sizes the
   read from `st_size` and gets 0 on procfs. Dead code that reads as defense-in-depth is worse
   than no fallback, because it is why nobody noticed the check is single-sourced.
4. **Witness it:** drive a runner with `TMPDIR` on tmpfs in a subprocess and assert exit 97 and the
   `tmp-refused:` line; and drive it with `findmnt` shimmed to fail and assert exit 97 too.

---

## GO-WITH-FIX (not blocking, but each should land before this is called finished)

### 2. The sweep will recursively delete the SHARED base — including other tenants' fixtures — if the sentinel env var is ever inherited

`tmp_leak_support.clj:152-153`. The child branch trusts the sentinel blindly and takes the root
from the property alone:

```clojure
(if (System/getenv reexec-sentinel)
  {:refused false :root (io/file (System/getProperty "java.io.tmpdir"))}
```

It never checks that the root is the private sub-directory the parent created. If
`CLJ_SURGEON_TMPDIR_REEXEC` is present in a process that is *not* the intended child, `:root`
becomes the shared base, and `report-and-sweep-leak!` (line 197, `fs/delete-tree root`) deletes it
whole — reporting every other tenant's entry as this run's leak on the way out. Reproduced
destructively on a decoy base with two planted "other seat" entries:

```
--- fakebase BEFORE ---
other-seat-file.txt
other-seat-precious-fixture
--- run: sentinel SET, base=fakebase, bb -D=fakebase ---
refused: false
root: /var/tmp/forge/tmpleak1-review-fx/opus/fakebase
entries seen as before: #{other-seat-precious-fixture other-seat-file.txt}
temp-leak: 2 entries left under /var/tmp/forge/tmpleak1-review-fx/opus/fakebase: other-seat-file.txt, other-seat-precious-fixture
sweep result: 1
--- fakebase AFTER ---
ls: cannot access '/var/tmp/forge/tmpleak1-review-fx/opus/fakebase': No such file or directory
```

**Reachability today is zero** — I checked, and nothing in the tree sets or exports the variable,
and no test spawns a nested suite that would inherit it:

```
$ rg -n 'CLJ_SURGEON_TMPDIR_REEXEC' .
./test/clj_surgeon/tmp_leak_support.clj:60:(def ^:private reexec-sentinel "CLJ_SURGEON_TMPDIR_REEXEC")
```

So this is latent, not live, and I am not calling it blocking. But this box is explicitly
multi-tenant (Gene, 2026-09-04), the failure mode is unrecoverable data loss in another seat's
working set, and the guard costs one comparison. Per "make the bad state unrepresentable":
**have the parent pass the root PATH in the env var (`CLJ_SURGEON_TMPDIR_REEXEC=<root>`), have the
child refuse unless `java.io.tmpdir` equals it, and have `report-and-sweep-leak!` refuse to
`delete-tree` a root that equals the base.**

### 3. Temp dirs created by a subprocess that picks its own location escape the isolated root entirely

Isolation is `-Djava.io.tmpdir` — a JVM-internal property no child process inherits. `secure-tmpdir!`
passes only `{reexec-sentinel "1"}` in `:extra-env` (line 158-159), never `TMPDIR`; and
`src/clj_surgeon/mcp_process.clj:357-361` `configure-environment!` sets **only** `PATH`. So a
subprocess calling `mktemp -d` / `tempfile.mkdtemp()` with no explicit directory writes to the
shared base, outside the root, invisible to the witness.

Planted four leak species on the export and ran them under the real mechanism:

```
Testing leakprobe
  subprocess mktemp -d created: /var/tmp/forge/PLANT-B-RFD8KY

Ran 4 tests containing 4 assertions.
0 failures, 0 errors.
temp-leak: 3 entries left under /var/tmp/forge/clj-surgeon-suite-73abeb03: PLANT-A-plain-17186604936671846509, PLANT-C-deleteOnExit-1510290704217050442.txt, PLANT-D-srcstyle-7602492411578675303.txt
LEAK-EXIT-CONTRIBUTION = 1
EXIT=1
```

Plain `createTempDirectory` (A), `.deleteOnExit` (C) and `createTempFile` (D) are all **caught and
named**. The subprocess leak (B) **escaped** and the witness stayed silent about it — I had to
delete `/var/tmp/forge/PLANT-B-RFD8KY` by hand.

This literally violates MCP-OP-TMPHYG-002 as written ("shall have isolated **that run's**
temp-file/temp-directory creation into a private, per-run root").

**In the builder's favour, and the reason this is not blocking:** I tested whether any real test
exercises it, by running both full suites with `TMPDIR` pointed at a private base of mine, so any
subprocess escape would be visible as an entry sitting directly in that base. **Both lanes left
nothing outside their isolated root:**

```
$ TMPDIR=.../privbase  ~/bin/suite-run bb test/run_all.clj
Ran 817 tests containing 6732 assertions.
0 failures, 0 errors.
EXIT=0
--- LEFT IN PRIVATE BASE AFTER RUN ---
(nothing)

$ TMPDIR=.../privbase2 ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 711 tests containing 8436 assertions.
0 failures, 0 errors.
EXIT=0
--- LEFT IN PRIVATE BASE AFTER RUN ---
(nothing)
```

So today the witness does not report green over a real leak; the gap is latent. The `.waiters`
case in the commit message is instructive and is *not* an instance of this: the python wrapper
derives that path from a lock path the JVM handed it, so it lands inside the root and the witness
caught it correctly.

**Fix: one line.** Add `"TMPDIR" (str root)` (and `TMP`/`TEMP`) to the `:extra-env` map at
`tmp_leak_support.clj:158-159`, so every descendant of the run inherits the isolated root.

### 4. The re-exec silently discards every JVM option — `make mcp-test` no longer runs at 512 MB, and the gate that checks that still passes

`reexec-child-command` (line 103-114) rebuilds the child as
`java -cp <classpath> -Djava.io.tmpdir=<root> clojure.main -m <ns>`, dropping whatever the parent
was launched with. `Makefile:35,198` runs the MCP suite as
`clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test`. Measured, parent vs child:

```
PARENT maxMemory(MB) = 512 | args seen = ("--selection-arg-A" "--selection-arg-B")
CHILD  maxMemory(MB) = 7832 | args seen = nil | tmpdir = /var/tmp/forge/clj-surgeon-suite-a243b26d
```

**The suite that was pinned at 512 MB now runs at 7.8 GB** (default ¼ of RAM), in a repo where
heap ceilings are a first-class gate (`make admit-analyzer-memory-self-test` at `-Xmx512m`, the
memory battery's `MEMBAT_XMX`). And `test/mcp_heap_config_test.sh:31-33` asserts only that
`make -n mcp-test` *prints* `-J-Xms64m -J-Xmx512m` — so the gate is green while execution has
changed underneath it. That is the seat's own scar: source text is not execution.

**Fix:** forward the parent's real flags —
`(.getInputArguments (java.lang.management.ManagementFactory/getRuntimeMXBean))` — into the child
command ahead of `clojure.main`, and add an assertion that the child's `maxMemory` matches.

### 5. Test-selection args are dropped by the re-exec

Same measurement: `args seen = nil` in the child. Harmless **today** — `run_all.clj` takes no args
and `mcp_test_runner.clj:51` is `(defn -main [& _])`, so the brief's "does it preserve the test
selection args" question has no current subject. But the re-exec is now a silent arg sink: the day
someone adds `-m runner some.ns`, it will vanish with no error. Forward `args` (bb: append after
the script; JVM: append after `-m <ns>`).

### 6. The isolated root leaks permanently if the parent is killed

No shutdown hook; `fs/delete-tree root` (line 161) only runs on the normal return path.
`kill -TERM` on the parent mid-run:

```
CHILD tmpdir= /var/tmp/forge/clj-surgeon-suite-7b99016a
--- killing PARENT with SIGTERM ---
parent gone, exit=143
--- roots left behind under /var/tmp/forge (before=0) ---
/var/tmp/forge/clj-surgeon-suite-7b99016a
-rw-r--r-- 1 forge forge 1 Sep  4 06:46 fixture-that-would-leak.txt
```

One directory per killed run, on ext4 rather than tmpfs, so it is not the inode-fire class — but
`~/bin/suite-run` jobs are routinely wrapped in `timeout` on this box, and a CI-style
`timeout 900` kill is a SIGTERM. **Fix:** `(.addShutdownHook (Runtime/getRuntime) ...)` around the
root, plus a startup sweep of `clj-surgeon-suite-*` roots under the base older than N hours
(carefully — see finding 2 about deleting things you did not create).

### 7. The ratchet covers 2 of the repo's 5 test entry points

```
$ rg -n 'secure-tmpdir!' . | grep -v tmp_leak_support
./test/run_all.clj:58
./test/clj_surgeon/mcp_test_runner.clj:59
```

`test/analyzer_contract_test_runner.clj`, `test/clj_surgeon/memory/memory_test_runner.clj` and
`src/clj_surgeon/memory_battery_runner.clj` have no call, and **`make test` runs
`analyzer-contract-test`** (`Makefile:976-982`) between the two protected lanes. Gene's ask was
"make it impossible to make this mistake again"; three runners can still make it. (Credit where
due: `-e` does *not* bypass, because `-M` appends command-line opts to the alias's `:main-opts` —
I confirmed `clojure -M:clj-surgeon/mcp-test -e '...'` still ran the full 711-test suite through
`-main`.)

### 8. An unwritable base crashes with a raw stack trace instead of a typed refusal

`TMPDIR` on a `chmod 500` directory:

```
babashka.fs/create-dirs                     - babashka/fs.cljc:894:1
clj-surgeon.tmp-leak-support/secure-tmpdir! - .../tmp_leak_support.clj:155:11
```

It fails closed, which is the important half. But the ratchet's other refusal is a named,
counted message; this one is an `AccessDeniedException` traceback. Make it symmetrical.

### 9. Residual hard-coded `/tmp` (the builder's claim is accurate but narrow)

The claim — "two hard-coded `/tmp/...` mktemp calls in `test/*_runner_test.sh`" — is **correct for
`mktemp`**; I checked every shell test and the rest already use `${TMPDIR:-/tmp}` or bare
`mktemp -d`/`-t`, which honour `TMPDIR`. But `rg -n '"/tmp' src test bench bin` (no `bin/` exists)
still finds paths that are *written to*, outside the two gated runners:

- `Makefile:735-739` — three benchmark self-test roots hard-coded to
  `/tmp/clj-surgeon-anvil-pair-self-test`, `/tmp/clj-surgeon-public-cfp-self-test`,
  `/tmp/clj-surgeon-format-extraction-self-test`.
- `test/mcp_heap_config_test.sh:23-27` — `MCP_STATE_DIR='/tmp/clj-surgeon-mcp-lifecycle-test'`.

The remaining ~60 hits are inert data strings (fake workspace roots, `:cwd "/tmp"` in assertion
fixtures, `"/tmp/receipt.edn"`), which I read individually and which create nothing.

On the src question: **one** `createTemp` in `src/` has no finally-delete —
`src/clj_surgeon/mcp_process.clj:125-126`, `extract-packaged-wrapper!`, which uses `.deleteOnExit`.
It is arguably correct there (the wrapper must outlive the call), but it means a long-running or
`kill -9`'d MCP server leaves one `clj-kondo-admission-*.py` behind per start. The other src sites
are clean — I read `mcp_process.clj:420-423`, whose `finally` at 480-483 deletes all three.

---

## Confirmed in the builder's favour (every claim reproduced by running it)

**Both JDK/Substrate facts are exactly as documented — and both are load-bearing.**

Fact (a), bb ignores `JAVA_TOOL_OPTIONS` and `TMPDIR`; only a literal `-D` at its own invocation works:

```
=== FACT 1a: bb ignores JAVA_TOOL_OPTIONS ===          bb tmpdir: /tmp
=== FACT 1b: bb ignores TMPDIR ===                     bb tmpdir: /tmp
=== FACT 1c: literal -D at bb startup works ===        bb tmpdir: /var/tmp/forge
```

Fact (b), neither runtime honours a runtime `System/setProperty` for real temp creation — bb:

```
getProperty says: /var/tmp/forge/tmpleak1-review-fx/opus/f1
actual createTempDirectory: /tmp/probe-bb-17764952614803267389
actual createTempFile: /tmp/probe-bb-8532047971638160783.txt
```

and a real JDK (so it is not GraalVM-specific), started with `TMPDIR`/`JAVA_TOOL_OPTIONS` already set:

```
startup java.io.tmpdir: /var/tmp/forge
after setProperty getProperty: /var/tmp/forge/tmpleak1-review-fx/opus/f2
actual createTempDirectory: /var/tmp/forge/probe-java-15574586274273117840
actual createTempFile: /var/tmp/forge/probe-java-17890537042596795933.txt
```

The builder's account of the false-green first cut is therefore not just plausible, it is forced by
the runtime. The re-exec is the right mechanism.

**A precedence hazard I went looking for and did NOT find.** `seat-tmp-guard.sh:6` exports
`JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge`. If that had outranked the child's `-D`, every
`make mcp-test` would have swept the shared base. It does not — the command line wins:

```
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
startup java.io.tmpdir: /var/tmp/forge/tmpleak1-review-fx/opus/f2
```

**The re-exec is well-behaved on the paths that matter.** Exit code preserved, stdio inherited and
correctly ordered, root swept on normal exit:

```
CHILD-STDERR-LINE
CHILD-STDOUT-LINE tmpdir= /var/tmp/forge/clj-surgeon-suite-f05f1d9c
PARENT EXIT = 42
```
```
$ ls -d /tmp/clj-surgeon-suite-b55ed840
ls: cannot access '/tmp/clj-surgeon-suite-b55ed840': No such file or directory
```

**tmpfs refusal survives the shapes I attacked it with** (finding 1 is about the *unknown* case, not
these). A symlink from an ext4 path to a tmpfs is correctly resolved and refused — `findmnt --target`
follows it:

```
TMPDIR=/tmp (the original mistake)     fstype=tmpfs    tmpfs?=true
/dev/shm                               fstype=tmpfs    tmpfs?=true
ext4 real base                         fstype=ext4     tmpfs?=false
SYMLINK ext4-path -> /dev/shm          fstype=tmpfs    tmpfs?=true
```

A base that does not exist yet is created by `fs/create-dirs` *before* the check, then correctly
evaluated — so the stale-looking `tmpfs?=false` on a nonexistent path is not reachable through
`secure-tmpdir!`. (I could not test a bind mount: no sudo on this seat. Stated as a gap in my
coverage, not a pass.)

**The leak witness is real, and its RED reproduces the builder's own number exactly.** I reverted
`mcp_change_buffer_test.clj` to its pre-fix content from `4699321e` on the export and ran the real
mcp runner. The commit message claims that namespace leaked **28**:

```
Ran 711 tests containing 8433 assertions.
1 failures, 0 errors.
temp-leak: 28 entries left under /var/tmp/forge/clj-surgeon-suite-3f2bdb76: clj-surgeon-change-buffer-10688645806625971789, clj-surgeon-change-buffer-11905580396495218746, clj-surgeon-change-buffer-12839633871075146017, clj-surgeon-change-buffer-13755780848107738284, clj-surgeon-change-buffer-14443513520989246684, ...
EXIT=2
```

28, named, counted, non-zero exit, and the root swept anyway. (The full RED counts of 1 and 66
could not be checked out — the whole ratchet is a single commit, so `4699321e` has no witness at
all — but this is the dominant contributor and it matches to the entry.) `.deleteOnExit` is caught
too, which is the subtle one, and the intent doc explains why correctly.

**Multi-tenant honesty holds on the normal path.** The witness looks only inside its private root,
so a concurrent seat's entries in `/var/tmp/forge` are neither reported nor deleted — my own
`PLANT-B` sat in the shared base untouched through a full witness cycle. The builder's 67
false-positive measurement and the isolation fix are sound. (Finding 2 is the sentinel path, which
is a different door into the same room.)

**Gates, verbatim, run by me at this tip.**

```
$ ~/bin/suite-run bb test/run_all.clj
Ran 817 tests containing 6732 assertions.
0 failures, 0 errors.
EXIT=0                                    (no temp-leak line → 0 leaks)

$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 711 tests containing 8436 assertions.
0 failures, 0 errors.
EXIT=0                                    (no temp-leak line → 0 leaks)

$ make mcp-operation-oracle
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
ORACLE EXIT=0

$ make repository-hygiene
repository hygiene: no machine-local build cache is tracked at any depth
HYGIENE EXIT=0
```

817/0/0-leaks and 711/0/0-leaks match the builder's claims exactly. The intent audit
(`clj-surgeon.mcp-intent-contract-test`, whose closed-world spec-doc-set assertions the commit
updated for the new leaf) rides inside that green mcp-test run. The new intent leaf exists with
both IDs marked `[x]`, and `@spec MCP-OP-TMPHYG-001/002` markers are present in `Makefile`
(`mcp-test`, `test-fast`) and `tmp_leak_support_test.clj`.

---

## Verdict

**## NO-GO** — one blocking: the tmpfs refusal fails open whenever the filesystem type cannot be
determined, and the `/proc/mounts` fallback written to cover exactly that case is dead code on both
bb and the JVM, so a suite demonstrably still runs with its temp dir on tmpfs (finding 1, arm B),
with no witness anywhere driving the refusal branch. Findings 2-9 are GO-WITH-FIX; the mechanism
itself — the re-exec, the isolation, the naming-and-counting witness, and the ten namespace fixes —
is correct, well-evidenced, and reproduced the builder's RED to the entry.

**Mergeability:** this tip merges into `origin/MCP/main` (`8aa45491`) cleanly on its own —
`git merge-tree --write-tree HEAD origin/MCP/main` exited 0 with tree `24c34e0a` and no conflicts —
so the only thing standing between it and `MCP/main` is finding 1.

Fixtures removed: `/var/tmp/forge/tmpleak1-review-fx` (proof in the final message).
