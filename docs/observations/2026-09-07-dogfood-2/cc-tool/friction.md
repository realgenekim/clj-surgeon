# Friction ledger — dogfood3 (clj-surgeon on cc-dogfood3, 2026-09-07)

Task: extract a byte-identical private helper from 3 namespaces into a new ns; one
inspect_clojure batch, one apply_clojure_changes fan-out; everything else native.
Result: first-attempt success, 0 refusals, kaocha 1021/0.

## F1 — `expect` is accepted and then discarded (apply_clojure_changes)

Exact receipt text (structuredContent):
```
"input_normalization": {"ignored": ["expect"], "reason": "editor counts are derived"}
```
Expected: `{"expect":{"changes":3,"edits":3,"files":3}}` to act as a GUARD — the call
refuses if the derived counts disagree with what I claimed. The doctrine's own
alias_migration schema example carries `expect`, and inspect_clojure honours it, so the
field reads as a gate across the verb family.
What I did: nothing — the receipt reported the ignore honestly, and the derived counts
happened to match. Seconds lost: 0 (no repair needed); the cost is latent, not paid here.
Why it still matters: a caller who mis-states the fan-out size gets a silent success. The
one field the caller uses to bind intent to effect is the one field the editor drops.
Ratchet: either (a) honour `expect` in apply_clojure_changes as a refusal-on-mismatch
guard, or (b) refuse the call outright with "expect is not supported by this verb" so the
caller cannot believe it has a guard it does not have. Silently ignoring a guard is the
worst of the three. Witness: a fan-out with `expect.files` = 2 against a 3-file edit set
must not return ok=true.

## F2 — `verification_complete=true` next to "verification: none requested"

Exact receipt text:
```
✓ atomic commit complete
✓ written bytes read back
✓ verification: none requested — bytes read back only
✓ terminal evidence · verification_complete=true · next action none
```
Expected: a field named `verification_complete` to mean semantic verification ran.
It means the WRITE was verified (bytes read back), not the change. Doctrine already warns
about this ("test values, not field names"), which is the tell that the naming misleads.
Seconds lost: 0 — I knew the rule and ran kaocha anyway (85.5 s, correctly mine to pay).
Ratchet: rename to `write_verification_complete`, or emit
`semantic_verification: "none requested"` as a sibling top-level field in
structuredContent (today it exists only in the human text line, so a machine reading
structuredContent alone sees only the true-looking flag). Text ⊇ structured is a known
class ratchet; here structured ⊉ text, the same defect mirrored.

## F3 — checkmark glyph on a non-event

`✓ verification: none requested` renders a NOT-DONE state with the same green check as
`✓ atomic commit complete`. Expected: absent-work to look different from done-work.
Seconds lost: ~0, but this is the exact shape of a false green.
Ratchet: reserve `✓` for completed checks; use `·` or `—` for "not requested".

## Wins — what the receipt got RIGHT and what it saved

W1. `inspect_clojure` returned per-owner match breakdown unasked:
```
r1 …portal.clj: 2 matches · owners [{"inside":"current-speaker-identity","matches":1},
                                    {"inside":"current-submission-speakers","matches":1}]
```
This is the whole planning step for a fan-out in one line: it separated the defn owner
(to be deleted) from the caller owner (to be rewritten) and gave me the exact
`within.form` + `matches:1` for all three edits. One 143 ms call replaced three file reads
and any doubt about counts. Directly responsible for the first-attempt success.

W2. `read_complete=true · next action none` — an explicit terminal signal, so I did not
re-read the files "to be sure". Saves the classic layered native loop the 2x-tax
measurement blames.

W3. `read_back_hashes` per file plus `canonical_effect_identity` and an `undo_receipt`
path: the write is auditable and reversible without a git operation, which matters under
a no-commit/no-stash rule like this run's.

W4. Zero refusals on a schema I typed from doctrine's example. The fan-out contract
(file / within.form / from / to / matches) transcribed cleanly against a real repo — the
schema example is doing its job.
