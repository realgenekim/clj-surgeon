# Task I-04

A saved mission decision can be confusing to read back. Two ways: its error
type may be buried inside `:evidence` instead of sitting at the top of the map,
and the worked `:example` it carries may have been recorded for a DIFFERENT
mission verb than the one being displayed, in which case the example is
actively misleading.

In `src/clj_surgeon/mission_display.clj`, add a function that takes a saved
decision and the verb being displayed and returns a clarified view of that
decision — without changing what the decision actually says. It should lift a
buried error type up to the top level and record that it came from the saved
evidence; and when the example was recorded for another verb, drop the example,
say why it was dropped, and offer a runnable help command for the right verb
instead. Anything that is not a map comes back untouched.

Put it directly above `show-result`. `show-result` and everything else in the
file must be untouched.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
