# Task T-01

`test/clj_surgeon/outline_memory_test.clj` has a test called
`outline-of-one-file-allocates-within-its-ceiling`. It is the witness for
requirement **MCP-OP-MEM-015**, but nothing in the file says so, so a reader
grepping for that requirement finds nothing.

Mark it: put the requirement's spec marker comment — `;; @spec MCP-OP-MEM-015`
— on the line immediately above that test, the way this repository marks a
witness with the requirement it proves.

Everything else in the file must be left exactly as it is: no other test, no
other comment, no other line.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
