# Lock-break interleaving

The project LOCK is a file two actors may touch at once, and every question
asked about it is asked as two syscalls: one that decides and one that acts.
The interval between them is a real window, and the three promises below are
what a caller is owed when another actor runs inside it.

Earned by inb-c74f05, 2026-09-13: `clj-surgeon.txn-journal-test/two-breakers-sharing-a-txid-cannot-destroy-each-others-evidence`
aborted with an uncaught `java.nio.file.NoSuchFileException` naming the LOCK,
three times in one day - two packet auto-batteries on unrelated branches and
the SHIP battery of the darwin landing, where it blocked a landing that had
already been given its GO. The throw came from `receipt-artifacts/resolved-target`,
called by `txn-journal/lock-file` to name the LOCK before the break had touched
anything: `Files/exists` said the file was there, the other breaker removed it,
and `.toRealPath` threw. Admission does not claim to prevent adversarial
filesystem races (DATACODE-ENV-001); it does have to answer the question it was
asked without throwing a race at its caller.

- [x] **TXN-RACE-001**: While a destination is being resolved for admission, when the path is removed between the existence check and the reading of its real path, or between the symbolic-link check and the reading of its link target, clj-surgeon shall resolve it as an absent tail under its resolved parent - the same answer it gives for a path that was already gone - rather than propagating the filesystem's exception to the caller.
- [x] **TXN-RACE-002**: While a stale claim is being broken, when the LOCK is removed or replaced at any enumerated step boundary of the break protocol, clj-surgeon shall return a typed break outcome carrying a cause the caller can act on rather than throwing out of the breaker.
- [x] **TXN-RACE-003**: While two breakers share one `:txid`, when their steps are interleaved at any enumerated boundary of the break protocol, clj-surgeon shall leave the judged claim readable on disk under exactly one name, with the tombstone name owned by at most one of them.

## Misreadings a maintainer could implement instead

- **TXN-RACE-001**: "Wrap the whole resolution in a `try` and return nil." A nil
  resolution is not an absent path; it turns a name the caller must still admit
  into a null the envelope check cannot evaluate, and admission would then pass
  whatever the caller did with it.
- **TXN-RACE-001**: "Retry the resolution until it succeeds." The path is gone;
  retrying asks the same question of the same absence, and under a hammer it
  never terminates.
- **TXN-RACE-002**: "Catch `Exception` in `break-lock!` and report
  `:lock-vanished`." That swallows the envelope refusal the admission boundary
  exists to raise, and a break outside the envelope would then report an
  ordinary missing lock.
- **TXN-RACE-003**: "Two breakers that both report `:broken true` are fine as
  long as no claim is lost." They were both handed one tombstone name, so one
  of them is telling its caller to look for evidence under a name it does not
  own.

## Boundaries

- The removal lands in the window belonging to an ANCESTOR rather than the
  final component (TXN-RACE-001).
- The final component is a symbolic link that is removed before its target can
  be read (TXN-RACE-001).
- The LOCK is not removed but REPLACED by a live holder's own claim inside the
  window (TXN-RACE-002, TXN-RACE-003).
- The tombstone name is taken by a third actor while the breaker holds its
  marker (TXN-RACE-002).
- A break that is refused must still leave every other receipt boolean able to
  be false: a typed outcome is not a success (TXN-RACE-002).
