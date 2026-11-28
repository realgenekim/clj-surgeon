# Read-only port move check

Contract: `bb bin/port-move-check REF PATH...` or `--manifest FILE` compares
source blobs to raw working-tree bytes. Manifest rows have two or three whitespace
separated fields; blank lines and whole-line `#` comments are skipped.
Malformed input exits nonzero. Source absence takes precedence over destination absence.
Equal hashes mean MOVE-OK; tracking and index equality are informational on stderr.
Stdout contains the requested pair lines and counts; only all-equal pairs exit 0.
Empty manifests refuse. Git runs without optional locks or lazy object fetching;
hashing uses `--no-filters` and never writes objects. CRLF and trailing-whitespace
differences are DIFFERS: bytes are bytes; `--no-filters` prevents normalization. No commits or network calls.

Pure tests cover parsing, malformed rows, all classification combinations,
each output shape, and empty/mixed footer counts. One repository boundary test
uses HEAD:deps.edn, missing source/ref/destination, and index agreement.
Verification: self-test, the requested archive comparison, paved kondo lint,
line count and test/source ratio. The standalone utility does not port code,
verify behavior, or prove that the old path was deleted.

Before hashing, NIO classifies destinations with NOFOLLOW_LINKS. Existing non-regular
paths print `NOT-REGULAR <kind> <dest>` (symlink, directory, other), increment
`not-regular=N`, and exit 1; this refusal takes precedence over missing source.
Missing destinations remain ABSENT when the source exists. The pure classification
regression uses literal regular/symlink/directory triples. Within the 90-line budget,
the symlink and directory boundaries are probed by hand in a scratch directory;
no persistent filesystem regression is added. Self-tests require a positive test
count and zero failures/errors.
