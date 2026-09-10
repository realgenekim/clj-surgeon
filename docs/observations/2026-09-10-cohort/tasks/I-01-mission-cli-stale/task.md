# Task I-01

`clj-surgeon.mission` can already tell whether the tree has drifted from a
mission's snapshot: it has `drift`, which needs a snapshot and a function that
reads a file's current contents (returning nothing when the file is gone), and
it has `stale-refusal`, which turns a drift result into the typed refusal.

The entrance, `src/clj_surgeon/mission_cli.clj`, does not use either. Give it a
`stale?` check it can call BEFORE anything is staged: hand a mission, get back
the typed refusal when the snapshot no longer holds, and nothing at all when it
still does. It needs a small private reader helper to pass to `drift` — one
that returns the file's contents when the path is a real file and nothing
otherwise.

Put both directly above `apply!`, which is the verb that will call it. `apply!`
itself, and every other function in the file, must be left exactly as it is,
and no require may change.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
