# Task R-01

`test/clj_surgeon/core_discovery_test.clj` is about to need to read EDN, so its
namespace needs `clojure.edn` available under the short name this repository
always gives it.

Add that require to the namespace form, aliased the way the rest of the
codebase aliases `clojure.edn`, and keep the require list in the alphabetical
order it is already in.

Every require already there must still be there, with the same alias, and no
test in the file may change.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
