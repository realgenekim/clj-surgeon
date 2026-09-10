# Task I-02

When the mission entrance refuses with `target-exists`, the refusal names a
file — and a `target-exists` against a ZERO-BYTE file is almost always a
fixture artifact rather than real code in the way. Reading that refusal today
tells you nothing about which it is.

In `src/clj_surgeon/mission_cli.clj`, add a private helper that takes a refusal
map and the workspace root and reports, for every file that refusal actually
NAMES, that file's size on disk — with a note beside the zero-byte ones saying
what they usually mean. A refusal can name a file under several keys, and a
named value may be either the path itself or a map carrying the path; relative
paths are resolved against the workspace root, and a path that is not a real
file is simply not reported.

Put it immediately above `ledger-of`. `ledger-of` and everything else in the
file must be untouched, and no require may change.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
