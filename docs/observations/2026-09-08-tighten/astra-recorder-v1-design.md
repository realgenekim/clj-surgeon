# Recorder v1: committed design before build

Steward/builder: Astra, as assigned by Gene's current request. Design timestamp is
the filesystem creation time recorded by the build, not a typed observation time.
Normative sources: Sol's `/var/tmp/forge/plan2/cellC/sol-tighten-review.md` §§3–4,
edits 5–7; `/home/forge/src/claude-skills-nrepl/tighten-the-loop/SEAT-RECEIPT.md`
and `CHANGE-RULE.md`. No owner appointment or epoch ratification is inferred.

## Decision and threat model

There is NO demonstrated independent producer/acceptor on this one-box,
one-UID deployment. Build candidate (a) as a **local, explicitly unverified
attestation instrument**, not as satisfaction of Sol condition 2. Its useful
minimum is a separately invoked recorder, an unpublished random HMAC key,
prospective local challenge, immutable-by-protocol evidence snapshots, and
serialized issue/check replay state. It cannot issue `observed`, `ok`, or
`independent=true`. There is no command-line independence override, trusted-seat
name switch, or configurable roster that promotes a local claim.

Unix modes 0700/0600 separate other UIDs; they do not separate two processes with
UID 1011. A directory outside the subject's declared path set prevents accidental
overlap only. We enforce that exclusion, disclose the broader actual same-UID
read/write authority, and never equate the two. In particular, the fixture
"secret readable by subject path set" tests declared path exposure; if "path
set" means ALL OS-readable paths, every key here is exposed and authoritative
issuance is impossible. A local artifact may be signed, but remains unverified.

| Candidate | Real today | What it fails to prove |
|---|---|---|
| (a) separate process, private directory, HMAC | Buildable here; process separation and keyed byte binding only | Same UID can read key, chmod/edit state and binaries, fabricate logs, replace verifier, rewind replay ledger, choose omitted paths/assignments, rerun issuance with a new state. No adversarial independence, trusted clock, complete events, actual prompt consumption or independent acceptance. |
| (b) mayor or Buster, inbox transport | Only a different host/account/service with keys, assignment/observation/acceptance state and authority inaccessible to subject credentials is a real boundary. No such appointment/access/control proof is supplied today. A mayor/Opus PID on this account is still (a). | Inbox delivery is not origin authentication. Subject may control remote credentials, substitute received keys, feed invented events, withhold replies, omit assignments before sealing. Needs pinned prospective authority, nonce binding, independently captured events and acceptance, retained replay state and readback. HMAC key must stay at remote verifier too; public signatures are preferable for offline transport. |
| (c) records git clock and signed tag | Existing Git publication preserves an externally retained object once received. No independent signer/remote clock API is instantiated. | Author/committer/tagger timestamps are caller supplied; local reflog is mutable; ordinary clones do not receive server reflog. Remote arrival time proves arrival, not launch/uptake/coverage. A signature proves possession of a key, whose independent custody still must be established. Current seat has write credentials; git cannot create the missing authority. |

A forger with this seat's actual authority can manufacture a valid MAC by reading
the key, rewrite the challenge and evidence history, suppress attempts and clocks,
reset consumption, and change the binaries to print success. The unchanged v1
verifier will still label a valid local MAC unverified. A verifier controlled by
the forger has no enforceable guarantee. No claim of protecting a malicious UID
from itself is made. Never publish the key or its bytes in logs or fixtures.

## Concrete v1 interfaces and scope

Two Python-standard-library one-shots, no JVM, network, daemon, or ports:

* `/home/forge/bin/receipt-attest prepare --state DIR --context FILE` seals one
  locally prospective assignment/session/challenge. Creates private state/key on
  first use. Context contains full `subject_seat_id`, `subject_session_id`,
  `issuer_id`, `assignment_manifest` and `effective_prompt` absolute file paths,
  `subject_paths` (nonempty absolute roots), and `expected_sources` (nonempty IDs).
* `/home/forge/bin/receipt-attest issue --state DIR --challenge ID --evidence FILE`
  snapshots JSONL launch, prompt-consumed, first-task and terminal events, each
  bound to the challenge, seat and session. Evidence JSON contains `events_file`.
  Events contain `kind`, `source`, `at` (UTC RFC3339), and the three binding IDs;
  launch/consumed events additionally bind `effective_prompt_hash`. Require that
  sequence, no extra event kinds, strictly ordered timestamps, and exactly the
  prospectively named source set. These are validations of supplied claims, not
  independent observations of the process tree. Retain exact supplied bytes.
* `/home/forge/bin/receipt-verify --state DIR --attestation FILE --subject-seat ID
  --subject-session ID` checks schema, authority consistency against the locally
  sealed challenge, MAC, full binding, retained bytes, issuance and time. It
  consumes a local challenge/session pair once even though the result remains
  unverified. A second check is `refused/replay`. This is a stateful check, not a
  read-only inspection. Missing key/state is `unverified/secret-absent` (exit 3),
  never key initialization. A copied attestation alone cannot prove freshness.

State default: `/home/forge/.local/share/receipt-recorder-v1`, outside tighten and
records. Explicit fixture state lives under `/var/tmp/forge/receipt-recorder-fx-*`,
also outside `/var/tmp/forge/tighten`. Reject state under declared subject roots,
records, default tighten roots, or absolute path-valued `TIGHTEN_*` variables;
check again at issue. Reject symlink components, permissive key/state modes,
non-regular/hardlinked secret files, unsafe IDs, malformed/duplicate-key JSON,
oversized inputs and invalid event ordering. Do not copy or hash key bytes as
evidence input. No secret is accepted via argv or environment.

The canonical signed JSON body binds schema/domain, issuer/key ID, random
challenge, subject seat/session, assignment manifest hash, launch event hash,
effective prompt hash, consumption time, first task hash, covered event-range
hash, expected source set, sealed time, issued time, and `shared-write-boundary`.
HMAC-SHA256 over sorted compact ASCII JSON is domain-separated by artifact type.
Key ID is a random identifier, not the key or a published key hash. All hashes are
full SHA-256. Times come from the system clock; no trusted-clock claim.

Each command prints one JSON receipt line. `prepare` exits 0 for a written local
challenge with status `prepared` (not an evidence verdict). Successful `issue`
and first `verify` exit **3**, status `unverified`, `signature_valid=true`,
`observed=false`, `reason=shared-write-boundary` (or `self-issued` when IDs match).
Refusals exit 2. Missing evidence/key exits 3 with signature validity unknown.
No invocation with absent arguments starts work; help exits 0.

Private per-challenge directories hold snapshots and result. Exclusive create
plus flock serialize transitions. Completed files use write-once publication;
issue reservation is recorded before evidence publication, so a crash may leave
a pending/refused challenge, never a reusable successful one. Recovery reads
retained state; this v1 provides no automatic deletion/retry bypass. Validation
refusals before reservation preserve bytes. Filesystem failures after reservation
report possible mutation. Replay protection assumes retained, unchanged state.

## Intent, tests, and completion gates

Use the explicitly authorized linked-intent-testing skill. Register stable
REC-TRUST-001, REC-BIND-001, REC-REPLAY-001, REC-PATH-001, REC-ABSENT-001,
REC-INPUT-001, REC-RETAIN-001 intents, EARS/misreadings/boundaries before code in
`/var/tmp/forge/tighten/fixtures/recorder-v1-intents.json`. Both-direction code/test
tag audit runs in `/var/tmp/forge/tighten/fixtures/run-attest.sh`, including
deleted-witness/dangling-ID mutations. The records doc retains the registry and
source artifacts (no secrets) for durability within the authorized one-doc scope.

At least twelve executable rows through installed entrances: local MAC-valid
unverified; self-issued unverified; issue replay; verify replay across processes;
wrong seat; wrong session; each signed field tampered; readable declared-path
secret refused at issue; missing key unverified; changed manifest; changed prompt;
bad/omitted coverage; event ordering; missing challenge; malformed inputs; unsafe
paths/modes; concurrent issue/check; unchanged bytes after validation refusal;
key read by same-UID subject (counterexample to candidate-a independence).
Use literal expected statuses, not runtime-derived oracles. Record initial red
execution honestly; new commands absent are a missing-capability baseline, not
a historical bug. Syntax and integration help checks, fixture green, and a
same-UID forgery probe are required. No Clojure suite/JVM is relevant here.

## Integration and release boundary

Do not edit or run `seat-receipt`. Report exact current source anchors and a
future consumption snippet that checks verifier JSON/exit and gates `observed`
on ALL SEAT-RECEIPT predicates. Its current `on_loop` means configuration health,
not independent participation. Preserve v0 labeling; adding this local artifact
does not create a complete v1 daily receipt. Unnamed recorder/acceptor and
unratified epoch remain pending; Astra building is not independent qualification.

Build and fixtures are authorized now. Independent producer/acceptor operation,
external key custody, prospective roster/assignment ownership, actual observed
prompt uptake, complete child events/attempt denominator, independent clock and
acceptance, fresh Claude/Codex consumer cells and seat-receipt integration remain
unproved. Condition 2 stays NO-GO. This is an executable fail-closed foundation,
not a relaxation of Sol's condition.

Only commit/push `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-recorder-v1.md`:
pull --ff-only, check no unrelated commits ride, Gene co-author trailer, push
HEAD:MCP/main, verify remote object. All temporary files under /var/tmp/forge.
