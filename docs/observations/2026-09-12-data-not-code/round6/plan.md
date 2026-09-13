# Round 6 contract and verification plan

Parents: round2/envelope-design.md, round3/requirements.md and the canonical
DATACODE-ENV / DATACODE-ROWS specifications. The binding round-3 approval and
round-6 brief authorize requirements, red witnesses, implementation and gates.
Starting checkout is 93690642, preserving the three measurement amendments
after the requested 7182573b. No measurement experiment runs in this build.

First deliverable: `bash test/diff-impact` discovers all test namespaces whose
declarations require a source namespace changed since 3b6d5357, directly or
through one intermediate namespace. Cadence and runtime exclusions cannot
remove a witness. Discovery reads namespace forms as data. Each selected
namespace runs once per before/after phase, sequentially in a fresh JVM. Each
run retains its log, counters, exit and wall; failures do not skip later tests.
The journal regression runs first with its temp root inside the envelope.

N1: admit a multiply linked final file only when all its inode links can be
accounted for as distinct directory entries inside the union of envelope roots.
Do not follow directory symlinks or count overlapping roots twice. Unknown
link evidence refuses. Keep outside-link APPEND bytes unchanged and admit the
journal's interrupted-break links. Filesystem races remain outside this claim.

N2/N3: runtime-steering state must derive from retained, opened control receipts;
row disagreement refuses by namespace/field. Restrict receipt paths to retained
repository evidence roots and put the evidence/attestation limit in
DATACODE-ROWS-001. Preserve the existing statistics and 456 timing receipts.

Preregistration: PROBE's two typed non-test refusals are scored; NATIVE has no
refusal concept and is unscored. Keep both bettors' numeric bounds and state
their mechanical interval, editing-exclusion bias, and secondary interval.

After fixes: serialized lint, the full impact oracle, once-only test-fast and
one prewarm (at most one repair and final retry), under the lease. Retain every
failure, report complete results in impacted.md, REPORT.md and report.edn,
commit last on the owned branch, never push.
