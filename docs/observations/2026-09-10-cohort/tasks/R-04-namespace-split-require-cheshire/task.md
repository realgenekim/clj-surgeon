# Task R-04

`src/clj_surgeon/namespace_split.clj` is going to need Cheshire's JSON encoder.

Add `cheshire.core` to that namespace's requires under the short name this
codebase uses for JSON, in the alphabetical position the existing list implies.

This is only the dependency: no function in the file changes behaviour, and
every require already listed keeps its exact alias.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
