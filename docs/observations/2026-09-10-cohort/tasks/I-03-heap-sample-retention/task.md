# Task I-03

`test/clj_surgeon/memory/heap.clj` reports a used-heap peak sampled on a timer.
At a small heap that number says how close allocation ran to the ceiling, not
what the arm is actually holding: an eight-file control that RETAINED twelve
megabytes peaked at two hundred and fifty-one.

Add an honest retention meter to this namespace: a public checkpoint function
that forces a full collection, lets it settle, reads the heap, remembers the
highest such reading across the run in private state, and returns the reading
it just took. Its docstring should say plainly that it is a stop-the-world
call, to be used at checkpoints and not in a loop.

Put it directly after `to-mb`, before `measure`. `measure`, `emit-receipt!`,
the existing private helpers and the namespace form must all be left exactly as
they are.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
