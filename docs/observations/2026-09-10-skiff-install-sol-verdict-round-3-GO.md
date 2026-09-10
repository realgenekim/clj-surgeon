GO

# Sol fence review, round 3: skiff install

- Reviewed sealed candidate: `6867f773be0f894f2bef0c145f3e07101365919f`
  (tip `6067f938812ddca81996b597f8e99bf07cd5b8d8`, base
  `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`). The sealed candidate and tip
  have the same tree, `3b0a4b15d428fee18217b66e3426c4a12509e03c`.
- Review completed: `2026-09-10T04:41:21Z`.
- Constraint observed: no `make test`.

## Finding disposition

`SKIFF-INSTALL-FENCE-001` is closed. The parser now stores the regex capture
`mnt` byte-for-byte. The grammar consumes exactly the one space delimiting the
option group; it does not normalize any bytes belonging to the mount point.

The focused Darwin parser witness passed for all requested cases:

- mount points ending in one space and two spaces selected `tmpfs`;
- `/Volumes/ram` and `/Volumes/ram ` in the same table remained distinct and
  selected `hfs` and `tmpfs`, respectively;
- embedded ` on `, unmatched ` (`, inner `)`, and trailing `on` mount points
  selected `tmpfs`;
- a bogus non-blank row returned `:unknown`.

The existing `darwin-has-a-mount-authority-of-its-own` focused test var also
completed without failure.

## Delta confinement and carried checks

The exact `064bc8bcdfc5441ac3b8fea7106822f6042c12e8..6067f938812ddca81996b597f8e99bf07cd5b8d8`
delta is confined to:

- `test/clj_surgeon/tmp_leak_support.clj`: +17 -3
- `test/clj_surgeon/tmp_leak_support_test.clj`: +22 -0

`git diff --check` is clean. The only implementation change is inside
`parse-darwin-mount-table`; the rest is explanatory text and the trailing-space
regression fixture. Therefore the round-2 evidence for (b) path-socket admission,
(c) `GATE_MEMAVAIL_MIB`, (d) the root-commit-keyed derived lock, (e) the named
SWI-Prolog preflight, and (f) Linux behavior remains applicable. In particular,
`test/gate_slot.py` is byte-identical at parent and tip (SHA-256
`8fbab32ed94a5b97c0c826bc3257a51cb703ff85f3c051d199f857fd6b056582`), preserving
the reviewed abstract-socket semantics and receipt admission field.

No ship fix is required.


> END RECEIPT (fence-run): worktree HEAD at review exit = 6867f773be0f894f2bef0c145f3e07101365919f = fenced sha.
