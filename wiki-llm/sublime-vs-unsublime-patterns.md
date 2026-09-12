# Sublime vs unsublime patterns — first page (2026-09-12, block B of bb-rewrite-tower)

Format per entry: **name** · symptom · counterexample (receipt) · why it is unsublime · the sublime
pattern · the ratchet. The meters these entries move are the three in the captain's log (trunk,
window, skills'-terms index); an entry that moves none is still listed if it cost an hour.

## Anti-patterns we paid for

### 1. Instance-at-a-time discovery through the most expensive entrance
Symptom: the same class of defect found one member per full run. Counterexample: seven check-only
prewarm packets at ~7 min each plus an Astra round apiece found 21 out-of-envelope writers one or
two at a time (`docs/observations/2026-09-12-bbtower-block-b-attempt12..18-report.md`); the class
oracle, a Landlock wrapper over the whole gate, found the rest in a 127 s diagnostic
(`attempt18/envelope-census.md`). Why: the first refusal already named the class. Sublime: when a
refusal names a class, the next round's first deliverable is the class oracle over the whole
surface. Ratchet: inb-f346bc — install the wrapper as a bound entrance so any gate can be run under
the envelope before a packet is spent.

### 2. Hand-written boilerplate briefs
Symptom: N briefs that differ by one denied path. Counterexample: `/var/tmp/forge/bbtower-fx/brief-astra-blockB-12..18.md`,
twenty briefs in one block; the tighten skill names boilerplate briefs a loop defect. Sublime: a red
packet's `report.edn` (check row, log path, subject, envelope roots) seeds the next brief; the human
adds only the decision line. Ratchet: inb-bc3098 (`run brief-from <packet-id>`).

### 3. Cause stated from a summary number
Symptom: a brief names a cause that the rows do not support. Counterexamples: "the bb union caused
the 68 s fast lane" (attempt 1: the refusal charged the same 61 members at base and subject);
"contention" (attempt 7: coordinator arms a ≈ b); "arm N died at 9/26" (the ledger file was
rewritten at the end; all 26 had run). Each cost a round. Sublime: fold the rows before naming a
cause; a brief cites the fold that supports its premise. Ratchet: the builder's first act is the
fold; a brief premise without a fold path is refused at read time.

### 4. Policy from single observations
Symptom: a runtime or routing assignment decided by one wall each. Counterexample: TEST-ISO-016 v1
assigned rename-alias-test to bb at ratio 1.984, 52 ms from the line, on one sample
(`2026-09-12-bbtower-block-b-sol-fence-verdict-1.md` F1); six runs per runtime moved it to the JVM
at a conservative ratio of 2.21 (`attempt20/runtime-table.md`). Why: the repository's own doctrine
demands six identical control runs and two standard deviations. Sublime: the rule clears its
threshold at two sd in the conservative direction; n, mean, sd and receipt path sit beside every
assignment; the witness fails by name on an undersampled row. Ratchet: the manifest witness.

### 5. A constant typed by hand
Symptom: a number in a spec that fails to reproduce from its cited evidence. Counterexample: the bb
lane ceiling: 399,155 (frozen map omitted 33 namespaces), 374,149 (denominator from a patched
classification), 343,102 (computed) — three derivations, the first two wrong
(`2026-09-12-bbtower-block-b-opus-verify.md`). Sublime: the constant is the output of a script over
an archived receipt, and the witness calls the script and compares (`test/clj_surgeon/bb_ceiling.clj`).
Ratchet: a number in a spec that a witness does not compute is wrong by the third reading.

### 6. Facts coupled to incidental representation
Symptom: a fact changes when its container changes. Counterexamples (Astra,
`2026-09-12-astra-data-not-code.md`): refusal meaning bound to an exception wrapper and English
message text (JVM SCI re-wrapped a typed xray refusal into :evaluation-failed; bb did not);
evidence bound to generated source (the six-run fold manufactured a source patch into the
manifest); authority bound to ambient paths (state-home as a home prefix vs an artifact root as a
final root); identity bound to line numbers (sleep exemptions keyed file→line; attempt 21 had to
move a pin after inserting unrelated lines). Sublime: values with provenance; refusal
classification as a value; evidence rows consumed, never patched in; declared places; identities
that survive a moved line. Ratchet: inb-162207 (four small red-first changes).

### 7. A test that passes on one runtime hides the defect on the other
Symptom: the nightly is green and the reference runtime is broken. Counterexample: xray-test ran
on bb in trunk's manifest and passed; on the JVM three typed refusals came back as
:evaluation-failed, pre-existing on trunk a15531ee, found only when the branch's cadence rule moved
the test to the JVM lane (`2026-09-12-bbtower-block-b-sol-fence-verdict-3-GO.md`, attempt 22).
Sublime: the JVM is the reference; a JVM control failing is always a refusal; a bb control failing
for a registered capability reason is an explicit ineligibility with the reason printed, never a
silent pin (`attempt23/portability-census.md`: 99 portable, 10 ineligible, 0 refused).

### 8. An over-broad rule, executed correctly
Symptom: the gate goes red on things that are not defects. Counterexample: attempt 22's rule
("refused by name until fixed, never pinned") refused 12 namespaces of which 9 were bb capability
limits. Why: "never silently pinned" was written as "never pinned". Sublime: three states, reasons
recorded; the ruling corrected in one round (attempt 23). Ratchet: a rule that can refuse must
enumerate the states it distinguishes before it runs.

### 9. Apparatus share at 100% while calling the day productive
Symptom: a night of tooling with no vs-native table. Counterexample: 2026-09-11/12, apparatus
~100%, window score 9 → 7, the canary itself at 69% apparatus (`tighten status`). The tighten
skill's tripwire: withdraw "sublime", name the dominant obstruction, select ONE bounded ratchet.
Sublime: the 50% floor; every apparatus block paired with a preregistered measure (the probe
measure, `docs/observations/2026-09-12-data-not-code/probe-measure-preregistration.md`).

### 10. Running the loop by hand instead of through its entrances
Symptom: the skill's meter goes stale while its work is done manually. Counterexample: the seat
receipt was 99 h old; the moment it ran it found routing-block drift (the forms rule had been
inserted inside the managed block on 2026-09-10) (`inb-9ce809`). Sublime: run `tighten day` /
`seat-receipt` / `ledger-to-findings`; friction filed with counterexample, ratchet, trigger.

### 11. A refusal that names nothing
Symptom: `{:error-type :typist-formatter-failed}` and nothing else. Counterexample: attempt 12's
prewarm; the same failure with attempt 13's repair read "npm error EACCES … /home/forge/.npm/_cacache",
which located the writer in one read (attempt 14). Sublime: every refusal names the native failure
it prevents and carries path, message and process evidence. Ratchet: refusals.edn completeness
witness per verb; receipt-text ⊇ structured.

### 12. A boundary that authenticates but does not authorize
Symptom: valid identity, wrong target, executed anyway. Counterexample: the probe accepted
`clj-surgeon.core` with a valid image identity and reloaded 36 production namespaces
(`2026-09-12-bbtower-block-b-sol-fence-verdict-2.md` F3). Sublime: authorize the requested target
against the declared roots before traversal; refuse with the symbol, resolved source, roots and an
EMPTY reload vector (attempt 21). Ratchet: BB-PROBE-003 and the refusal registry.

### 13. A waiter the harness can kill
Symptom: "low memory" reaps the process watching the process. Counterexample: three background
`while kill -0` waiters killed while builders lived (2026-09-12 11:4x–17:4xZ). Sublime: a monitor
bound to the exact pid that emits the terminal line; never a waiter that matches its own argv.

## Sublime patterns observed (keep doing these)

- **Stop and ask on an envelope fact rather than widen it.** Attempt 17 refused to assume a
  /dev/null exception because the brief said "exactly three roots"; the answer was one line and
  the wrapper gained parity with packet.py by line number.
- **GO-WITH-FIX is not GO; re-verify at the tip.** Opus red-team → five fixes → Opus verifier
  re-ran the same attacks at the new tip and found the ceiling still wrong (`opus-verify-blockB.md`).
- **A reviewer who re-attacks every prior repair.** Sol's three rounds: NO-GO, NO-GO, GO, each
  round re-running the previous findings' commands and reporting "held".
- **Red first, on both runtimes, for a runtime-dependent defect.** Attempt 22 recorded the xray
  failure on JVM and bb at trunk and tip before touching code.
- **Nothing waived, relabeled or patched into a sealed candidate.** A candidate with a red battery
  landed nothing; a conflict repair became a new candidate; a docs-only commit after a prewarm
  invalidated the receipt and a new prewarm was spent.
- **The seat's own tooling satisfies its own gate.** Ship admission's prewarm-required was met for
  the first time by a check-only packet's receipt and ledger event (packet 0836a451), after ship
  v3.12 removed the one environment override that had made it impossible.
