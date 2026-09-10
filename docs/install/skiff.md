# Installing clj-surgeon on a Mac

For the laptop (Apple Silicon, zsh, Homebrew). Everything here has been executed
on a genuinely clean Linux user; the darwin-specific code paths were executed on
Linux under a forced backend. **Nothing in this document has run on a Mac** —
that receipt is the one you are about to produce.

## The one command block

```sh
cd ~/src/clj-surgeon        # wherever your checkout lives
git pull
make install                # ~25s on a clean machine; prints a preflight first
make test                   # the landing gate
```

`make install` never needs `sudo` and writes only under `$HOME`:

| what | where |
|---|---|
| CLI launcher | `~/bin/clj-surgeon` (+ `.receipt.edn`) |
| versioned package | `~/.local/share/clj-surgeon/versions/<commit>/` |
| Claude skill | `~/.claude/skills/clj-surgeon` |
| Codex skill | `~/.codex/skills/clj-surgeon` |
| routing plate | a managed block inside `~/.claude/CLAUDE.md` and `~/.codex/AGENTS.md` |

The routing plate edits your global instruction files. `make check-agent-routing`
proves the installed block matches the repository's plate; run it any time you
suspect drift.

## Prerequisites

`make install` **refuses** without: `bb`, `java`, `clojure`, `git`, `python3`.
The preflight names anything missing before a single byte is written.

Two more that are not needed to install, only to use:

- **`clj-kondo`** — `:ls`, `:outline` and `:fix-declares!` refuse without it.
  `brew install clj-kondo/brew/clj-kondo`
- **`swipl` (SWI-Prolog)** — **`make test` cannot run without it.** It runs the
  MCP operation contract oracle inside the `mcp-test` stage (the 4th of 7), but
  `make test` checks for it *at the gate entrance*, so you find out before
  paying the three stages before it. `brew install swi-prolog`

There is no opt-out for the Prolog oracle. It is a merge gate, and a gate with a
bypass is not a gate — so `make test` refuses by name rather than skipping.

The same entrance check also refuses a RAM-backed temp directory, the other
condition that makes every gate lane exit 97. Both name themselves; neither is
skippable.

Optional: `brew install coreutils` gives you `gtimeout`, which bounds a couple of
steps that otherwise run unbounded. Nothing fails without it.

## What to look for in the output

**From `make install` — the preflight block.** Read three lines:

```
  platform               Darwin arm64 (...)
  bash (this script)     3.2.57(1)-release          <- Apple's bash; expected
  ...
    babashka             babashka v1.13.219
    bb minimum           1.12.209 (bb.edn :min-bb-version) -- this box is newer or equal
  ...
  Gate admission
    mode                 :path-socket -- a split semaphore is DETECTED, not impossible
    socket dir           /tmp/csg-501 (worst case 54 bytes; budget 100, sun_path 104)
    memory               23493 MiB available (read by the gate's darwin reader)
    lane floor           3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
```

and, just above it:

```
  Scratch (the suite refuses a RAM-backed temp dir)
    temp base            /var/folders/.../T/ ("apfs" -- accepted)
```

`:path-socket`, a `socket dir` well inside 104 bytes, a MEASURED number of MiB,
and an accepted `apfs` temp base are the four lines that say the darwin code
paths were actually selected.

**Every one of those lines is now the gate's own answer, not a second opinion
about it.** The preflight shells out to the same functions `make test` calls
(`clj-surgeon.gate-memory`, `test/gate_slot.py`); it cannot print a source the
gate then refuses to use. That is not a style preference — on 2026-09-10 this
page's own example output said `read from vm_stat + sysctl hw.memsize` while
the gate refused `available memory is unknown` minutes later on the same box,
because the preflight had only checked that `vm_stat` EXISTED and the reader
had never successfully run `vm_stat` on any platform.

If the temp base is REFUSED, `make test` would exit 97 in every lane.

If `memory` says **REFUSED**, the line names the STEP that could not answer
(`vm_stat` missing, non-zero, or unparsed; `sysctl -n hw.memsize` missing or
unparsed) — set `GATE_MEMAVAIL_MIB` to the MiB you are willing to lend the gate
(e.g. `export GATE_MEMAVAIL_MIB=16384`) and say so.

**The floor: 3584 MiB, and it is printed before anything runs.** The gate opens
`min(max(1, cpus/2), (availableMiB - 2048) / 1536)` lanes, so the smallest box
it will open at all is `reserve 2048 + 1536 per lane` = **3584 MiB**. A grant
below that refuses:

```
gate-refused: insufficient memory for a bounded lane -- 3072 MiB available,
3584 MiB floor = reserve 2048 + 1536 per lane; GATE_MEMAVAIL_MIB=<MiB> declares
what this box may lend
```

`GATE_MEMAVAIL_MIB=3072` looks generous — it is twice a lane's charge — and it
bounces. That is why the floor and its formula are on the preflight screen, in
the refusal, and here: three places, one constant
(`clj-surgeon.gate-memory/single-lane-floor-mib`).

**The socket directory: `/tmp/csg-$UID`, and why it is not `$TMPDIR`.** darwin
caps a Unix-domain socket path at 104 bytes (`sun_path`); linux allows 108. The
macOS per-user `TMPDIR` is about 46 bytes of random directory
(`/var/folders/wc/…/T/`), and the gate used to append `clj-surgeon-gate/` to it
— `AF_UNIX path too long`, the gate refusing in 163 ms.

The name that overflowed is worth knowing, because it is not the one anybody
would have guessed. A slot leaf (`clj-surgeon-gate-admission`) lands at 92
bytes there and is fine. What `bind()` refused is the `.pending-<32 hex>`
private name the gate binds **before** linking a slot into place — atomic
publication, so two racing claimants cannot both believe they won — at 107
bytes. The fit check is therefore sized to the longest name the module BINDS
(41 bytes), not to the longest it advertises.

The gate now publishes its slots under a SHORT per-user root, `/tmp/csg-<uid>`,
created `0700` and refused outright if it is owned by anybody else — `/tmp` is
world-writable and sticky, so ownership is proven rather than assumed. On macOS
`/tmp` is a symlink to `/private/tmp` and is disk-backed, so this does not
violate the suite's no-RAM-temp rule; it holds **sockets and nothing else** —
the temp-hygiene rule is unchanged and scratch files still belong under
`TMPDIR`. A linux box keeps its `$TMPDIR/clj-surgeon-gate` root as long as that
root fits, and goes short when it does not.

`CLJ_SURGEON_GATE_ROOT` overrides all of it and is honoured, never silently
relocated; a path that still would not fit is a typed refusal naming the byte
count, the budget and the cap rather than an `OSError` from `bind`.

**The babashka floor: `bb.edn`'s `:min-bb-version`, and why a preflight says
it out loud.** babashka's SCI carries an allowlist of host classes, and
v1.12.209 has no `StackOverflowError` and no `OutOfMemoryError` in ANY spelling
— fully qualifying does not help, because the class is absent from the map
rather than unqualified. SCI resolves classnames at ANALYSIS time, so on
2026-09-10 one `catch` in `src/clj_surgeon/core.clj` made that namespace
unloadable and took **all 49 bb test namespaces** with it before a test ran.
The tree no longer names a class SCI may lack (`clj-surgeon.jvm-error` holds
the predicates, and `clj-surgeon.jvm-error-test` scans the babashka closure for
the spellings). babashka itself warns when it is older than `:min-bb-version`;
the preflight says it louder, because a warning inside a suite that then fails
is a warning nobody reads.

**From `make test` — the first line and the last line.**

The FIRST line is the capacity the run was admitted on:

```
gate-capacity: 24576 MiB available, 12 cpus, 6 lane(s); slots path-socket
/tmp/csg-501; 3584 MiB floor = reserve 2048 + 1536 per lane;
GATE_MEMAVAIL_MIB=<MiB> declares what this box may lend
```

The LAST line is one EDN map:

```
landing-gate: {:state :passed, :landing? true, ...
               :capacity {:cpus 12, :memory-mib 24576, :lanes 6,
                          :admission :path-socket, ...},
               :wall-ms 512345, ...}
```

Four things to check, in this order:

1. `:state :passed` and `:landing? true` — anything else is a failure, and
   `:problems` says what.
2. `:capacity :lanes` — the **derived width**, `min(max(1, cpus/2),
   (availableMiB - 2048) / 1536)`. There is no width flag; if this is 1 on a
   laptop with plenty of RAM, memory detection is reading low. The same
   arithmetic, with the floor, is printed as `gate-capacity:` on the first line
   of the run.
3. `:capacity :admission` — must be `:path-socket` on a Mac, and
   `:capacity :slot-root` names the directory the slots were published under.
4. `:wall-ms` — the number worth comparing.

## What to paste back

```sh
make install 2>&1 | sed -n '/preflight/,/^OK to install\|^REFUSED/p'
make test 2>&1 | tail -1
sw_vers; uname -m; sysctl -n hw.ncpu; sysctl -n hw.memsize
```

The preflight block, the `landing-gate:` line, and the machine facts. If
anything refuses, paste the refusal verbatim — every refusal in this repository
is typed and names its own remedy, so the text is the diagnosis.

## The one guarantee that is weaker on a Mac, stated plainly

The gate admits test workers through a box-wide semaphore. On Linux a slot is a
name in the **abstract** Unix-domain socket namespace: no path, no inode, no
directory. Two coordinators that share nothing still contend for the same kernel
names, so a split semaphore is *unrepresentable*.

macOS has no abstract namespace. Slots there are pathname sockets under
`$TMPDIR/clj-surgeon-gate/`, and the guarantee drops from *unrepresentable* to
*detected*: publication is atomic, a dead holder is detected by the kernel, and a
replaced root is a typed refusal — but a root **deleted** between the identity
check and the publication can still split the semaphore for a process that has
nothing to compare against.

In practice that means: **do not delete `$TMPDIR/clj-surgeon-gate` while a gate
is running.** The receipt tells you which guarantee you got, every run, in
`:capacity :admission`.
