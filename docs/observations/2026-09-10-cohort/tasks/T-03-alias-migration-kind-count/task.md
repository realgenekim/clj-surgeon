# Task T-03

Row-2 external-artifact containment made the entrance able to emit two more
typed refusal kinds: `receipt-dir-escapes` and `receipt-dir-inside-workspace`.

`test/clj_surgeon/mcp_alias_migration_test.clj` pins that enumeration twice
over — a frozen set of every kind, and a count. Both pins still describe the
world before those two kinds existed, so the pin no longer matches what the
entrance promises a text-reading client.

Bring the pin up to date: the two new kinds belong in the frozen set, with a
short comment saying where they came from, and the count assertion has to agree
with the set. The file's own convention is that a changed pin carries its
reason.

Do not touch any other kind, any other assertion, or any other test.

---

When you are done, the change must be in the working tree of the
repository you were given. Do not commit, and do not touch any other file.
