# Surgeon on the skiff: `make install` on a clean system

**Branch** `fable/skiff-install` (4 commits), worktree
`/home/forge/src/clj-surgeon-skiff`, base proven `HEAD = origin/MCP/main =
3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`. Author `forge-anvil`, Gene + Fable
trailers, **nothing pushed**. 1,126 insertions / 51 deletions across 16 files.

Gene, verbatim: *"Confirm will be usable and performant on skiff. Use max two
hours to get make install on clean system to work well with clj codebase with
all wins we've earned."*

---

## The headline

**`make test` could not have passed on your laptop, and the reason was not any
of the five surfaces the audit named.** It was the temp-directory hygiene
ratchet. `base-refusal` fails closed — anything that is not *positive proof of
real disk* is a refusal — and both of its mount authorities are Linux-only
(`findmnt` is util-linux; the table is `/proc/mounts`). On macOS neither can
answer, so it returns `:unknown` and **every JVM in the suite exits 97**.

The ratchet was not wrong. It had no authority to ask, and a gate with no
authority is a gate that refuses everything. I found it by running `make test`
as a genuinely clean user on anvil2, where 113 namespaces across three suites all
exited 97 and the operator's entire evidence was a four-kilobyte census dump.

`mount(8)` is darwin's authority, and it is now consulted exactly as `findmnt`
is on Linux — as a system source, not a seam, so the MCP-OP-TMPHYG-011 property
(a forged table can refuse but can never *prove* disk) holds unchanged.

## Verify on Anvil: `make test` green on the branch

`~/bin/suite-run make test`, backgrounded, waited on the exact pid.

```
landing-gate: {:state :passed, :landing? true, :problems [],
               :capacity {:cpus 16, :memory-mib 24366, :lanes 8,
                          :admission :abstract-socket, :heap-mib 512,
                          :reserve-mib 2048, :lane-charge-mib 1536},
               :wall-ms 166431,
               :git-head "3585c7e59880f64f53ce6d416ea4a6d3265fd84c", ...}
```

All 7 stages exit 0: `admit-transaction-recovery-battery` 10.3 s, `battery-fresh`
1.9 s, `alias-migration-test` 85.5 s, `mcp-test` 95.8 s, `test-bb` 80.8 s,
`repository-hygiene` 1.8 s, `intent-audit` 1.9 s. Pool makespan 150.2 s over 8
lanes. **`:git-head` equals the branch tip**, so the green is bound to this exact
commit and not to a stale tree. (An earlier green at `83cf6bc2` measured 170.4 s;
two runs, same verdict.)

`make landing-gate-prewarm` also verified: `:state :passed`, `:prewarm? true`,
`:landing? false` — correctly *not* a landing — `:problems []`, 153.0 s.

The new `:admission` field is in the receipt, which was the point: the guarantee
level is now evidence rather than a comment.

**Two runs were lost to my own error before these**, and it is worth recording
because the mechanism did its job: I committed while the gate was running, and
both times it refused with `:tree-changed-during-suite` rather than reporting a
result for a tree that no longer existed. The third and fourth runs were made on
a frozen, clean tree.

### The semaphore under real concurrency

A new backend deserves more than the oracle's three callers. 12 independent
processes sharing nothing but a namespace string, racing `try_acquire` at three
widths, four rounds each — **144 process-races per backend**:

| backend | width 1 | width 3 | width 5 | leftover entries |
|---|---|---|---|---|
| `abstract` (Linux, regression check) | 1/12 | 3/12 | 5/12 | 0 |
| `path-socket` (**the darwin backend**) | 1/12 | 3/12 | 5/12 | 36 (all killed holders) |

Zero bound violations in either. The 36 entries are precisely the holders that
were killed rather than released — the stale-entry case that the next acquirer
reclaims. Under the production shape (one constant namespace, orderly release)
30 acquire/release cycles leave **0 entries**, so growth is bounded, not a leak.

---

## A. Portability audit: surface → darwin path → witness

A full static sweep of every `git ls-files`-tracked script ran in parallel with
the work. The findings below are only those on the **shipped** surface (`make
install`, `make test`); the rest are triaged at the end.

| # | Surface | Was | Darwin path | Witness |
|---|---|---|---|---|
| **0** | `tmp_leak_support/mount-fstype` — **the blocker, not in the brief** | `findmnt` + `/proc/mounts` only → `:unknown` → every JVM exits 97 | `mount(8)`, longest-mount-point-prefix, as an authority not a seam | pure parser witnessed against **real macOS `mount` bytes**: 6 cases incl. a space-containing mount point and nested-volume precedence. 13 tests / 62 assertions / 0 failures |
| 1 | `test/gate_slot.py` — abstract Unix sockets | `\0clj-surgeon-gate-slot-N`; darwin has no abstract namespace | pathname sockets under `$TMPDIR/clj-surgeon-gate/`; atomic publish via `os.link` (EEXIST, never clobber); dead-holder reclaim via `connect()`→ECONNREFUSED; inode-guarded unlink; per-process root identity ⇒ replaced root is a **typed refusal** | **the whole oracle re-run under the darwin backend on Linux: 13/13, 0 skipped**, incl. both independent-coordinator split regressions |
| 2a | capacity — `/proc/meminfo` | Linux only, in **both** implementations | `GATE_MEMAVAIL_MIB` → `/proc/meminfo` → `vm_stat` free+inactive+speculative+purgeable capped by `sysctl -n hw.memsize` → typed refusal naming the override | executed both ways on Linux; `{... :memory-mib 8192 :lanes 4 :admission :path-socket}` under forced darwin overrides |
| 2b | capacity — `nproc` | GNU coreutils | `nproc` → `sysctl -n hw.ncpu` → JVM `availableProcessors` | executed |
| 3 | `Makefile:1193,1218` — `flock /home/forge/tmp/suite.lock` | a util-linux binary macOS lacks, on a path one machine has | `bin/with-lock` (`fcntl.flock`; released by the kernel on SIGKILL exactly as `flock(1)`), path derived per-user from `SELF_TEST_TMP` | mutual exclusion proven: second holder waited for the first |
| 4 | GNU-isms in the gate DAG | `mktemp -d -t NAME.XXXXXX` ×3 (BSD takes a bare prefix ⇒ a literal `XXXXXX` in the name); bare `timeout` ×1 | explicit template; `timeout`/`gtimeout` resolved once, degrades to unwrapped | `sh -n` on all four; the tmp ratchet runs green |
| 5 | the kondo entrance | `~/bin/clj-kondo` expected | **no change needed** | with clj-kondo absent, `:outline` refuses **typed**: `{:error-type :analyzer-authority-unverified :cause-error-type :clj-kondo-executable-unavailable}`; preflight names it and gives the brew line |
| 6 | Java detection / `JAVA_TOOL_OPTIONS` | — | preflight filters the `Picked up JAVA_TOOL_OPTIONS:` line the JVM emits **before all output** and reports the variable explicitly | before the fix the preflight reported the env var *as* the Java version; after, `openjdk version "21.0.12"`. No shipped source parses `java -version` — only `System/getProperty` |
| 7 | `MEMBAT_ROOT` / the battery's own scope guard | hardcoded `/home/forge/tmp` in the **shipped** Makefile *and* in `generate_tree.clj`'s allow-list | both derived | this one was worse than a path: on any other machine **every** `MEMBAT_ROOT` was outside the allowed prefix, so the battery refused before it started. Self-test green |

### One defect my own new test caught

The first inode guard compared the gate root against a root created microseconds
earlier **in the same call** — a no-op that would have witnessed nothing. The
guard is now a per-process identity taken at first publication. The
replaced-root test went red, which is why it exists.

### Audit findings deliberately NOT fixed, and why

The static sweep covered every `git ls-files`-tracked script. Everything above is
what `make install` or `make test` actually touches. The remainder — roughly 40
findings — lives in `bench/`, `dev/experiments/` and `docs/observations/`: seat
benchmark harnesses and lab notebooks that never ship and that no installed user
invokes. They are Linux-only in interesting ways (`/proc` process-tree walks with
no darwin equivalent, `declare -A` and `${var,,}` needing bash ≥ 4, `tac`,
`sha256sum`, `date +%s.%N`, `readlink -f`, `stat -c`, `flock`), and fixing them
would be a second day's work with no effect on the laptop. **They are listed here
so their absence is a decision, not an oversight.**

Worth stating plainly, because it is the audit's good news: **the `make install`
recipes were already clean.** They use `shasum -a 256` (not `sha256sum`), POSIX
`readlink`, `#!/usr/bin/env bash` or `#!/bin/sh` throughout, and no bash-4
syntax. Someone had already thought about macOS there. The breakage was in the
gate underneath, not the installer.

---

## B. Clean-install transcript

No Mac is reachable from Anvil. `sudo -u astra` is unavailable on anvil2 and the
`astra`/`sol` accounts refuse host-key verification, so per the brief I used a
genuinely clean **prefix**: `HOME=/home/forge/clean-home2`, an empty `~/bin`,
`PATH` reset to `/usr/local/bin:/usr/bin:/bin`, `JAVA_TOOL_OPTIONS` unset, and a
real `git clone` from a bundle so `SOURCE_COMMIT` resolves.

| step | result | wall |
|---|---|---|
| `git clone` → `HEAD 83cf6bc2` | ok | — |
| **`make install`** | **exit 0** | **25 s** |
| → CLI `~/bin/clj-surgeon` + receipt | installed | |
| → versioned package under `~/.local/share/clj-surgeon/versions/<commit>/` | installed | |
| → Claude + Codex skills + receipts | installed | |
| → routing plate into `~/.claude/CLAUDE.md`, `~/.codex/AGENTS.md` | `:changed-count 2`, both `:previous-state :absent` | |
| `make check-agent-routing` | **`:ok true, :scope :installed`**, block hash equal on both targets | <1 s |
| `clj-surgeon --version` | `{:tool "clj-surgeon", :version "0.1.0"}` | <1 s |
| `clj-surgeon :op :proof-status` | typed `{:error-type :missing-arguments :missing [:receipt]}` — a correct answer to a bare call | <1 s |
| `make print-gate-stages` | 7 stages | 5 s |
| `make landing-gate-prewarm` | see below | |
| `make test` | stages 1-2 pass (38.3 s, 4.9 s), then **refuses: swipl** | |

Note anvil2 is a **dev box, not a timing box** — I did not use it for any
performance claim. For scale only, the two stages that completed there ran
3.5x and 2.6x the Anvil wall (`admit-transaction-recovery-battery` 38.3 s vs
10.3 s; `battery-fresh` 4.9 s vs 1.9 s). Treat those as an order of magnitude,
not a measurement.

The swipl refusal, witnessed directly on the clean prefix:

```
gate-refused: SWI-Prolog (swipl) is not installed.
  It runs the MCP operation contract oracle, which is the FIRST
  stage of `make test`. Installing clj-surgeon does not need it;
  running the landing gate yourself does.
  macOS:  brew install swi-prolog
  Debian: sudo apt-get install swi-prolog-nox
```

and the CLI still answers `{:tool "clj-surgeon", :version "0.1.0"}` on that same
box, which is the point: a missing test-only prerequisite must not look like a
broken install.

### Every hand step and missing prerequisite — each one a make-install defect

1. **SWI-Prolog is absent** on a clean Ubuntu *and* on a clean macOS, and it is
   gate stage one. `swipl: command not found` is a true statement that tells the
   reader nothing. Now a named refusal with the brew and apt lines.
   **I did not add a bypass**: this oracle is a merge gate, and a gate with an
   opt-out is not a gate. Fixed in-repo (legibility); the install itself remains
   the operator's.
2. **`TMPDIR` unset + `/tmp` on tmpfs ⇒ 113 namespaces exit 97.** The refusal is
   correct; the *legibility* is the defect, and it is what led me to finding #0.
   macOS always sets a per-user `$TMPDIR` on real disk, so the skiff does not hit
   this trigger — but a clean Linux login does. **Not yet fixed in the preflight**
   (see Open, below).
3. `clj-kondo` absent ⇒ typed refusal, already correct. Reported by preflight.
4. No other hand steps were required. `make install` needed no `sudo` and wrote
   only under `$HOME`.

### The clean prefix's other 24 failures — investigated, and NOT mine

`cli-dispatch-test` failed 24 assertions on the clean prefix. I bisected it
rather than assuming: **trunk `3ea3803e` fails identically, 24 failures / 0
errors, in the same clean prefix.** My branch does not regress it.

The mechanism, chased to the end because "probably environmental" is not a
finding:

- `src/clj_surgeon/namespace_split_io.clj:168` invokes the analyzer as
  `<user.home>/bin/clj-kondo` — the paved serializing entrance — and
  `resolve-clj-kondo-analyzer` deliberately EXCLUDES that path from resolution
  so the shim cannot recurse into itself.
- On anvil2, `/home/forge/bin/clj-kondo` is a **symlink to
  `/usr/local/bin/clj-kondo`**, the only clj-kondo on the box. The exclusion is
  by canonical path, so excluding the shim also excludes the real binary, and
  the analyzer resolves to nothing → `:clj-kondo-executable-unavailable`.
- A second, unrelated fact this exposed: **`user.home` and `$HOME` disagree** on
  a prefix-style clean install. `user.home` came back `/home/forge` (the passwd
  entry) while `$HOME` was `/home/forge/clean-home2`. `make install` writes to
  `$(HOME)`; the runtime reads `user.home`. On a normal login they agree, and on
  Gene's Mac they will — but it is why a *prefix* is not a perfect stand-in for
  a *user*, and I would rather say that than let the transcript imply otherwise.

**What this means for the skiff:** nothing, unless Gene symlinks
`~/bin/clj-kondo` at his Homebrew clj-kondo — in which case he would hit exactly
this. The safe instruction, which is what `docs/install/skiff.md` gives, is to
install clj-kondo via brew and leave `~/bin/clj-kondo` alone, or run
`make install-with-analyzer`, which writes a real serializing shim there rather
than a symlink.

I did **not** change the analyzer resolution. The self-exclusion is correct and
the serializer is deliberate doctrine; widening it to fall back to PATH would
silently bypass the analyzer serialization the paved entrance exists to enforce.
That is a doctrine call, not a portability fix, and it belongs to whoever owns
that gate.

---

## C. The skiff receipt

`docs/install/skiff.md` is on the branch. The block:

```sh
cd ~/src/clj-surgeon
git pull
make install          # ~25s; prints a preflight BEFORE writing anything
make test             # the landing gate
```

Prerequisites `make install` **refuses** without: `bb`, `java`, `clojure`,
`git`, `python3`. Two more are needed to *use*, not to install:
`brew install clj-kondo/brew/clj-kondo` and — for `make test` only —
`brew install swi-prolog`.

**What to look for.** In the preflight, three lines say the darwin paths were
actually selected:

```
  platform               Darwin arm64 (...)
  bash (this script)     3.2.57(1)-release      <- Apple's bash; expected
    mode                 :path-socket -- a split semaphore is DETECTED, not impossible
    memory               read from vm_stat + sysctl hw.memsize
```

In `make test`, the last line is one EDN map; read four fields in order:
`:state :passed` and `:landing? true`; `:capacity :lanes` (the **derived
width**, `min(max(1, cpus/2), (availMiB-2048)/1536)` — there is no width flag);
`:capacity :admission` (must be `:path-socket` on a Mac); `:wall-ms`.

**What to paste back:**

```sh
make install 2>&1 | sed -n '/preflight/,/^OK to install\|^REFUSED/p'
make test 2>&1 | tail -1
sw_vers; uname -m; sysctl -n hw.ncpu; sysctl -n hw.memsize
```

---

## What I could NOT verify without a Mac — plainly

No Mac is reachable from Anvil, so **nothing here has executed on darwin.**
Each claim is one of three kinds, and the difference matters:

- **Executed, same code path, forced backend on Linux** — the gate slot
  semaphore (13/13 under `path-socket`), the capacity overrides, the derived
  lock and roots, every GNU-ism fix. Strong.
- **Executed against real darwin *bytes*** — the `mount(8)` parser. The bytes
  are real macOS output; the *shell-out that produces them* has not run.
- **Read, not executed** — and there is exactly one: **the `vm_stat` + `sysctl
  -n hw.memsize` memory read.** I have no darwin `vm_stat` output to parse, so
  its regexes are a static reading of documented format. This is precisely why
  `GATE_MEMAVAIL_MIB` exists and is honoured *first on every platform*, and why
  the preflight prints which source answered. If the preflight says
  `memory  UNREADABLE`, that is this gap surfacing, and one env var closes it.

Also unverified without the device: that Apple's bash 3.2 runs
`bin/install-preflight` (it is written for 3.2 — no `declare -A`, no `${var,,}`,
no `mapfile` — and `bash -n` passes, but 3.2 itself was not the interpreter),
and that Homebrew's `clojure`/`bb`/`swipl` behave as their Linux builds do.

---

## Defects fixed, in one table

| # | Defect | Where | Shipped? | Fixed |
|---|---|---|---|---|
| 1 | Suite refuses on macOS — no mount authority | `tmp_leak_support.clj` | yes | `mount(8)` as darwin's authority |
| 2 | Refusal remedy names `/var/tmp/forge` to every operator alive | `tmp_leak_support.clj` | yes | derived base + the macOS-true sentence |
| 3 | RAM-backed TMPDIR ⇒ 113 lanes exit 97, no line naming the cause | preflight (absent) | yes | preflight executes the ratchet's own decision |
| 4 | Gate slots need an abstract Unix namespace | `gate_slot.py` | yes | `path-socket` backend, guarantee in the receipt |
| 5 | `/proc/meminfo` + `nproc` | `gate_slot.py`, `battery_parallel_runner.clj` | yes | `vm_stat`/`sysctl`, `GATE_MEMAVAIL_MIB` |
| 6 | `flock /home/forge/tmp/suite.lock` | `Makefile:1193,1218` | yes | `bin/with-lock`, derived path |
| 7 | `MEMBAT_ROOT` + the battery's own allow-list pinned to `/home/forge/tmp` | Makefile, `generate_tree.clj` | yes | derived; the battery could not have started on any other box |
| 8 | `mktemp -d -t` (BSD takes a bare prefix) | 3 gate scripts | yes | explicit template |
| 9 | bare `timeout` | `tmp_leak_ratchet_test.sh` | yes | `timeout`/`gtimeout`, degrades |
| 10 | swipl absence unreadable | `Makefile:206` | yes | named refusal + remedy, **no bypass** |
| 11 | `head -1` reports `JAVA_TOOL_OPTIONS` as the Java version | preflight | yes | filtered, and the variable reported |
| 12 | memory-test temp roots pinned to `/home/forge/tmp` | 2 memory test ns | yes | derived |

**Found and deliberately not fixed:** the analyzer self-exclusion vs a symlinked
`~/bin/clj-kondo` (doctrine, not portability — see above); ~40 GNU-isms in
`bench/`, `dev/experiments/` and `docs/observations/`, none of which an
installed user touches.

---

## Open

1. **The one command block is unrun on darwin.** Everything else here has a
   witness; this needs Gene's machine.
2. If the preflight reports `memory  UNREADABLE` on the Mac, the `vm_stat`
   parse is wrong and `GATE_MEMAVAIL_MIB` is the immediate unblock. Send me the
   raw `vm_stat` output and it is a five-minute fix with a real witness.
3. Whether `install` should imply `install-with-analyzer` on a box with no
   `~/bin/clj-kondo` is a doctrine question I left alone.

**Boundaries honoured:** no push, no `pkill`/`pgrep -f` (I used one `pgrep -f`
by reflex, caught it, and recovered the pid via `ps` — recorded here rather than
quietly), temp under `/var/tmp/forge`, no forbidden ports, `-Xmx` bounded
throughout, anvil2 used as a dev box only.
