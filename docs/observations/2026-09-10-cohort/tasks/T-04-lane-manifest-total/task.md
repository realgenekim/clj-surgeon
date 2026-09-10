# Task T-04

Round two added three more witnesses to the corpus: a percent-bearing
verification string must not throw out of the receipt renderer, the real
profile shape carries hot and cold verdicts beside `:checks`, and the
2000-character failure budget is one TOTAL rather than one per check.

`test/clj_surgeon/lane_manifest_test.clj` pins the corpus total inside
`the-corpus-only-ever-grows-and-the-arithmetic-is-shown`. It still says 1370.

Move that pin to the new total and, as the file itself insists, record the
reason for the change at the pin.

The adopted-tests pin, the round-one floor and the closing-arithmetic assertion
beside it are not part of this change and must be left alone.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
