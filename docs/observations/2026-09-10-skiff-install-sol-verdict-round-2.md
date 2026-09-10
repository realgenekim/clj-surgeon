NO-GO

# Sol fence review, round 2: skiff install

- Reviewed sealed candidate: `ea061e9dab6cc4213f2fb6a4a4ca8bd91e874768`
  (tip `064bc8bcdfc5441ac3b8fea7106822f6042c12e8`, base
  `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`). The sealed tree is
  byte-identical to the tip.
- Review completed: `2026-09-10T04:33:49Z`.
- Constraint observed: no full `make test`. Only the two gate-entrance refusal
  probes were invoked; both stopped before the coordinator or any Clojure lane.

## Blocking finding

### SKIFF-INSTALL-FENCE-001 — the parser still changes mount-point bytes and can prove the wrong filesystem

The new grammar is anchored, understands the requested ` on ` and parenthesis
cases, and poisons the whole table on an unparseable non-blank row. But after
capturing the mount point it stores `(str/trim mnt)`, contradicting the stated
contract that the mount point is the exact bytes between the device separator
and trailing option group.

A trailing space is legal in a Unix path. In the second row below there are two
spaces before `(tmpfs`: the first is the final byte of the mount point and the
second is the mount/option delimiter.

```text
/dev/root on / (apfs, local)
/dev/ram on /Volumes/ram  (tmpfs, local)
```

For target `/Volumes/ram /T`, the executed parser returned `"apfs"`. It had
changed the nested mount point from `/Volumes/ram ` to `/Volumes/ram`, so that
row no longer covered the real target and the root row became positive proof of
disk. This is the same safety-class failure as round one's crafted ` on ` row:
a fully parseable mount table can still make the ratchet prove past the actual
covering tmpfs mount.

The direct repair is to retain `mnt` byte-for-byte and add this regression. The
implementation itself lives under `test/`; making that correction normally
replaces/deletes a line under `test/`. The ship contract declares such a
reviewer patch `HOLD reason=oracle-changed` and explicitly directs `NO-GO` when
the repair genuinely needs it, so I did not leave an ineligible self-fix.

## Requested checks

### (a) Darwin mount grammar

Executed adversarial mount points containing an unmatched ` (`, a `)`, a
trailing `on`, embedded ` on `, and a bogus non-blank row. The first four
correctly selected `"tmpfs"`; the bogus row returned `:unknown`. The absolute
mount authorities are `/sbin/mount` and `/bin/mount`, so `PATH` cannot redirect
them. The trailing-space counterexample above remains blocking.

### (b) Path-socket backend

The focused oracle passed all 13 rows under forced `path-socket`; the abstract
backend passed all applicable rows (13 run, 2 backend-specific skips). The
receipt path still reports `:capacity :admission` via `gate-admission-mode`.
The documented weaker guarantee remains accurate: same-process root replacement
is detected, while a fresh independent coordinator after deletion can split the
pathname semaphore. The repair diff does not touch `test/gate_slot.py`.

### (c) `GATE_MEMAVAIL_MIB`

Still open and accurately named as operator authority. The relevant Python and
Clojure coordinator paths are unchanged from round 1: zero refuses through the
derived-width bound, negative refuses, and an absurd positive declaration is
trusted but cannot widen beyond the CPU half-width cap.

### (d) Derived suite lock

Passes the requested topology. It is keyed by numeric UID plus the first 16 hex
digits of the repository's root commit, under the selected scratch base. A
checkout, its worktree, and a separate clone produced the same lock. A separate
fork worktree with an extra fork commit also shared because its root commit was
the same; an unrelated-root repository produced a different lock.

That fork collision is the correct conservative choice for this heap witness:
the root commit identifies the suite lineage whose neighbour JVM can corrupt the
measurement. It may serialize divergent forks unnecessarily, but it avoids
publishing contaminated heap evidence. A non-git tree takes the documented path
hash fallback.

### (e) Gate entrance and install preflight

Passes. With `swipl` absent, entrance-only `make test` exited 2 after naming
SWI-Prolog, the fourth-stage oracle, and both brew/apt remedies; no coordinator
or Clojure lane started. With `TMPDIR=/dev/shm`, it exited 2 at
`gate-prerequisites`, named the RAM-backed base and exit-97 consequence, and
again started no lane. There is no bypass in either target.

`make install-preflight` names missing `swipl` and both remedies while correctly
allowing installation, because Prolog is a test-gate dependency rather than an
install/runtime dependency. A dry run of `make install` puts that preflight
before every write recipe.

### (f) Linux path preservation

Passes. `test/gate_slot.py` is byte-identical between `3585c7e5` and
`064bc8bc` (both SHA-256
`8fbab32ed94a5b97c0c826bc3257a51cb703ff85f3c051d199f857fd6b056582`), so
the Linux abstract-socket implementation is exactly the reviewed round-3
semantics. The forced abstract oracle passed, and the round-2 repair adds only
the intended prerequisite/lock changes plus Darwin mount handling and docs.

> END RECEIPT (fence-run): worktree HEAD at review exit =
> `ea061e9dab6cc4213f2fb6a4a4ca8bd91e874768` = fenced sha.


> END RECEIPT (fence-run): worktree HEAD at review exit = ea061e9dab6cc4213f2fb6a4a4ca8bd91e874768 = fenced sha.
