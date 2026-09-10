# Task R-03

`test/clj_surgeon/mcp_workspace_test.clj` reaches for `clojure.string` once, in
the middle of `receipt-directories-are-deterministic-and-workspace-isolated`,
by writing the whole namespace out longhand: `clojure.string/includes?`.

Everywhere else in this codebase `clojure.string` is required with a short
alias and called through it. Make this file do the same: require it in the
namespace form under that usual alias, and change the call site to go through
the alias.

The longhand spelling should be gone afterwards. Nothing else in the file
changes — the other requires, the other tests and the assertion's own arguments
stay exactly as they are.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
