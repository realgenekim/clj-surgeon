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
- **`swipl` (SWI-Prolog)** — **`make test` cannot run without it.** It is gate
  stage one. `brew install swi-prolog`

There is no opt-out for the Prolog oracle. It is a merge gate, and a gate with a
bypass is not a gate — so `make test` refuses by name rather than skipping.

Optional: `brew install coreutils` gives you `gtimeout`, which bounds a couple of
steps that otherwise run unbounded. Nothing fails without it.

## What to look for in the output

**From `make install` — the preflight block.** Read three lines:

```
  platform               Darwin arm64 (...)
  bash (this script)     3.2.57(1)-release          <- Apple's bash; expected
  ...
  Gate admission
    mode                 :path-socket -- a split semaphore is DETECTED, not impossible
    memory               read from vm_stat + sysctl hw.memsize
```

`:path-socket` and `vm_stat + sysctl` are the two lines that say the darwin code
paths were actually selected. If `memory` says **UNREADABLE**, that is the one
piece with no Linux witness — set `GATE_MEMAVAIL_MIB` to the MiB you are willing
to lend the gate (e.g. `export GATE_MEMAVAIL_MIB=16384`) and say so.

**From `make test` — the last line.** It is one EDN map:

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
   laptop with plenty of RAM, memory detection is reading low.
3. `:capacity :admission` — must be `:path-socket` on a Mac.
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
