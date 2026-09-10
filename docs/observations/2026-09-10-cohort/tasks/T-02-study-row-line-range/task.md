# Task T-02

In `test/clj_surgeon/mcp_study_test.clj`, the test
`forms-text-carries-the-source-a-caller-asked-for` ends by asserting that the
rendered text contains the literal string `"reader-cond?@37-39"`.

That pins the lines the fixture happens to occupy today. The claim we actually
want to make is that the row NAMES the form's own line range — so when
something is added above `reader-cond?` in the fixture and it slides down the
file, this test should still pass, and should still fail if the row stops
carrying the range.

Rewrite that one assertion so the expected range is taken from the form the
test already has in hand rather than written out as a constant.

Nothing else in the test, and nothing else in the file, may change.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
