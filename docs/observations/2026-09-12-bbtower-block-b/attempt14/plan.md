# Formatter launcher: attempt 14

Authorized scope: user attempt-14 red/fix/green sequence, one focused namespace,
one fast suite, at most two prewarms with one repair. Main and shared installs
remain frozen. New evidence and scripts live in this directory.

HLD: staged formatting must work within the selected writable temp envelope.
LLD: retain the existing selected-temp-root policy; configure npm cache and logs
in the process launcher. For the default formatter, resolve standard-clj from
PATH, then checkout node_modules/.bin, then fall back to the original npx argv.
Custom commands keep their argv. Report actual expanded argv and resolved status
on success, nonzero exit, timeout and launch exception.

EARS: when launching a formatter, the product shall put npm cache and logs under
the selected temp root, overriding inherited home locations. When the default
formatter has an executable standard-clj available, it shall invoke that binary.
Each invoked formatter shall retain its selected command and resolution status
in the receipt. Misreadings: staging alone confines npm; resolving npx is an
offline formatter; custom commands may be silently replaced; failure receipts
may omit command identity.

Witness matrix: real command with a read-only npm cache (red then green); PATH,
checkout and fallback selection; custom command; success/failure/timeout receipt;
inherited npm env overridden with a selected root. Existing mission typist proof
then runs with fresh TMPDIR and scratch HOME, recording endpoint listings.
