# MVR LID batch 4 — HLD

Worktree: `/home/forge/src/mvr-lid5`; branch: `astra/lid-batch-4-hld`.
Base: `e0680b6c5e49785c62d9dd3a11f7ff2170cb1935` (batch 3).
Commit: `d75120d981aa192bba71db67c6244eed50fb525a` (local only; not pushed).
Author: environment identity `forge-anvil <forge-anvil@anvil>`.
Both requested co-author trailers verified. Working tree is clean.
Tracking: `marvin-voice-remote-3wa` (closed).
Evidence: `/var/tmp/forge/lid-batch/mvr-b4-evidence/`.

## Outcome

Appended `## 13 — Surface flags, trial gates, and the command path` with all ten
ledger subsections, each 3–4 sentences in the existing architecture voice.
Only `ARCHITECTURE.md` and `docs/intent/registry.edn` changed. Section 13 follows
Appendix B to satisfy the literal append-only instruction: all 39,309 original
architecture bytes, including Appendix A and Appendix B, remain identical.
No competing `docs/high-level-design.md` was created.

The registry uses string `:doc` pointers and keyword-vector `:see` links. Added
15 `:doc` pointers to active rows and extended four existing documented-row
pointers while preserving their original provenance. All other row fields,
IDs, order, statuses, EARS promises, witnesses, and `:see` links are unchanged.

## Section text (verbatim)

```markdown
## 13 — Surface flags, trial gates, and the command path

### 13.1 One send path per surface

Spoken end words and tapped OVER converge on the surface's existing send
authority: dedup, archive, enqueue, and broadcast cannot acquire a second
implementation for a button. On legacy `/bridge4`, `force_end=1` travels on
the same keyword-check request and bypasses only the last-word test; blank
transcripts still refuse, end-word sentinels still strip, and replay of the
same dedup ID returns the cached result. Registry
`BRIDGE4-TAPPED-OVER-ONE-PATH-G1` enforces this boundary; the reducer's command
path is separately witnessed by `TAPPED-OVER-JOINS-ONE-TURN-C01`, preserving
D11's migration order.

### 13.2 Transport commands

OVER joins the turn: a live capture commits as a partial and posts once when
its completing transcript lands; a stash-only OVER posts immediately, and
an empty OVER posts nothing. SKIP advances playback without posting or
touching the stash or pending tapped-OVER request; CANCEL discards capture
and stash, clears that request, and stays in HF. Precedence is **CANCEL >
SKIP > OVER**; the OVER request is consumed once, never inherited by the next
turn. Registry `TAPPED-OVER-JOINS-ONE-TURN-C01`,
`TAPPED-CANCEL-DISCARDS-STAYS-HF-C02`, and `SPOKEN-SKIP-F2` carry the command
contracts and their witnesses.

### 13.3 The frozen artifact rule

The flagless `/bridge4` render is the rollback artifact: trial buttons,
bootstrap keys, force-end wiring, and trial words in the friction note are
absent from its bytes. `?controls=1` and `?micgate=1` are independent opt-ins;
neither authorizes the other or changes the flagless page. Registry
`BRIDGE4-FLAGLESS-IS-FROZEN-G3` requires both golden equality and independent
trial-byte absence checks, so re-blessing a contaminated golden cannot
authorize a trial leak. The golden moves only for an approved, named page
delta under D11's incident path.

### 13.4 Acoustic surface policy

Barge-in defaults OFF globally; headphones and room speakers have opposite
self-echo risks, so enablement is a physical per-surface field decision. On
`/bridge4`, the server resolves `?barge=1` or `?barge=0` into the page's
effective policy; only an explicit `?micgate=1` emits the mic gate, whose
value is the negation of that effective barge-in flag. Thus the gate prevents
capture onset during playback when barge-in is OFF and stays inert when barge-in
is deliberately ON. Registry `BARGE-IN-FIELD-GATE-F2`,
`BARGE-IN-SURFACE-DEFAULT-F2`, and `BRIDGE4-MICGATE-INVERTS-BARGE-G4` enforce
the default, the page override, and the inversion respectively.

### 13.5 Enablement vs mechanism

A production default is its own promise: a mechanism's passing tests do not
authorize turning it on. Registry `LONG-TTS-PROD-DEFAULT-F4` enables complete
prebuffering for automatic replies on bridge4/Code Director with
`?longttsbuffer=0` rollback, while `LONG-TTS-PREBUFFER-F4` preserves synchronous
gesture playback, observable streaming fallback, and stale-generation
rejection. `ECHO-OBSERVE-PROD-DEFAULT-F5` selects OBSERVE for live reducer
surfaces with the post gate open; `SELF-ECHO-QUARANTINE-F5` keeps the standalone
classifier OFF by default and quarantine a later field decision. Registry
`FRICTION-POLICY-NOTE-UI` requires the served note to describe that surface's
effective state and disable path, including echo guard **unavailable** on
legacy bridge4.

### 13.6 Echo evidence authority

Only an acknowledged `playback-finished` admits a reply to the echo evidence
ring: offering, starting, or downloading a reply does not prove Marvin's
words reached the acoustic surface. SKIP must not fabricate that receipt;
suspected echo retains the transcript, matched reply ID, and score evidence
instead of disappearing silently. Registry `SELF-ECHO-QUARANTINE-F5` enforces
this evidence boundary, extending `POL-P10`'s complete-artifact requirement
without mistaking artifact completion for client playback. OBSERVE records
evidence and permits posting; quarantine withholds a strong recent match
only under its explicit policy gate.

### 13.7 One router constructor

Every caller builds the application's route table through
`server/make-router`, production and tests alike. Router options and the
deliberate Director control-route overlap have one home; a test-local option
or constructor would let a green suite describe a server that cannot ship.
Registry `ONE-ROUTER-CONSTRUCTOR-M01` binds the route witnesses to that shared
constructor, so route construction has the same outcome in both contexts.

### 13.8 Director control lease

While hosted HF capture is active, a Director command stream at CLOSED must
be discarded and reconnected under the **same client identity**. A non-nil
EventSource or server heartbeat is not evidence that the browser receives
commands; reconnecting must not mint another capture identity or command
slot. Registry `MVR-DIRECTOR-CONTROL-LEASE-001` witnesses the lifecycle table:
active/open keeps the stream, active/closed reconnects, and inactive/closed
closes it.

### 13.9 Where the ledger lives

[`docs/intent/registry.edn`](docs/intent/registry.edn) is the durable, enforced
index of promises; this Architecture of Record remains their design
authority. Its D/S/P invariants have stable registry IDs, including `ARCH-D2`,
`SYS-S13`, and `POL-P6`, whose `:documented` rows point back to the binding
prose and do not claim active LID enforcement. Active rows must link in both
directions to implementation and witness tags, carry misreadings, and name
existing witness tests; `intent_contract_test` checks those obligations in
the normal suite. `:doc` carries a document pointer and `:see` carries related
registry IDs, keeping design provenance distinct from executable evidence.

### 13.10 Enrich-on-touch

The next change inside a `:documented` promise upgrades its row to `:active`
with misreadings, boundaries, implementation and witness tags, and named
tests. This applies to thin rows such as `ARCH-D2`, `SYS-S13`, and `POL-P6`;
their documented status records the traceability gap, never permission to
ignore the binding rule or §2.1's applicability. The intent contract enforces
the active obligations as soon as the status changes; adding a design
cross-link alone supplies no new behavioral evidence. IDs remain permanent:
retirement or supersession changes status, never deletes or repurposes the
promise.
```

## Rows touched

| Registry ID | :doc |
|---|---|
| `ARCH-D2` | ARCHITECTURE.md §2 D2, §13.9, §13.10 |
| `SYS-S13` | ARCHITECTURE.md §3.1 S13, §13.9, §13.10 |
| `POL-P6` | ARCHITECTURE.md §3.2 P6 + reconcile note, §13.9, §13.10 |
| `POL-P10` | ARCHITECTURE.md §3.2 P10 (i37), §13.6 |
| `BARGE-IN-FIELD-GATE-F2` | ARCHITECTURE.md §13.4 |
| `SPOKEN-SKIP-F2` | ARCHITECTURE.md §13.2 |
| `LONG-TTS-PREBUFFER-F4` | ARCHITECTURE.md §13.5 |
| `SELF-ECHO-QUARANTINE-F5` | ARCHITECTURE.md §13.5, §13.6 |
| `MVR-DIRECTOR-CONTROL-LEASE-001` | ARCHITECTURE.md §13.8 |
| `ONE-ROUTER-CONSTRUCTOR-M01` | ARCHITECTURE.md §13.7 |
| `BARGE-IN-SURFACE-DEFAULT-F2` | ARCHITECTURE.md §13.4 |
| `LONG-TTS-PROD-DEFAULT-F4` | ARCHITECTURE.md §13.5 |
| `ECHO-OBSERVE-PROD-DEFAULT-F5` | ARCHITECTURE.md §13.5 |
| `FRICTION-POLICY-NOTE-UI` | ARCHITECTURE.md §13.5 |
| `BRIDGE4-TAPPED-OVER-ONE-PATH-G1` | ARCHITECTURE.md §13.1 |
| `TAPPED-OVER-JOINS-ONE-TURN-C01` | ARCHITECTURE.md §13.1, §13.2 |
| `TAPPED-CANCEL-DISCARDS-STAYS-HF-C02` | ARCHITECTURE.md §13.2 |
| `BRIDGE4-FLAGLESS-IS-FROZEN-G3` | ARCHITECTURE.md §13.3 |
| `BRIDGE4-MICGATE-INVERTS-BARGE-G4` | ARCHITECTURE.md §13.4 |

## Gates and tails

All requested gates exited 0. Both full test runs include the intent contract:
**581 tests, 7,981 assertions, zero failures** each. `make runtests-once` also
passed the Beads export check, Node Director reconnect contract, and all four
Prolog checks. Registry lint: **zero errors, zero warnings**.

Commands sourced `mvr-b4-evidence/env.sh`:

```sh
export TMPDIR=/var/tmp/forge/lid-batch/mvr-b4-evidence/tmp
export _JAVA_OPTIONS='-Djava.io.tmpdir=/var/tmp/forge/lid-batch/mvr-b4-evidence/tmp -Dmvr.data.dir=/var/tmp/forge/lid-batch/mvr-b4-evidence/data'
```

The inherited `JAVA_TOOL_OPTIONS` announces `/var/tmp/forge`; `_JAVA_OPTIONS`
takes precedence and keeps JVM temporary files and test persistence under the
requested batch root. The existing full-turn wire test uses its own `:port 0`
server; the host ephemeral range is 32768–60999, outside the forbidden ports.
No prohibited live-service ports were contacted, and no deploy or push ran.
`.mcp.json` and `.codex/` were untouched. Beads auto-import generated an untracked
scratch export; it was moved to this evidence directory and is not committed.

### bin/kaocha — exit 0

Tail with the progress-dot line omitted and ANSI color removed; raw log retained:

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up _JAVA_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch/mvr-b4-evidence/tmp -Dmvr.data.dir=/var/tmp/forge/lid-batch/mvr-b4-evidence/data
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up _JAVA_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch/mvr-b4-evidence/tmp -Dmvr.data.dir=/var/tmp/forge/lid-batch/mvr-b4-evidence/data
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  config: {:throw? true}
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
581 tests, 7981 assertions, 0 failures.
```

### make runtests-once — exit 0

Compact gate output with the progress-dot line omitted and ANSI color removed:

```text
🔎 Checking committed Beads JSONL export...
✓ Beads export is parseable and contains unique issue IDs
director capture-control reconnect contract: PASS
% [1/4] director_control_..d_stream_reconnects ...... passed (0.003 sec)
% [2/4] director_control_..es_not_claim_a_slot ...... passed (0.000 sec)
% [3/4] director_control_..ield_counterexample ...... passed (0.000 sec)
% [4/4] director_control_..tes_have_one_action ...... passed (0.000 sec)
Running tests with fail-fast...
bin/kaocha --fail-fast
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up _JAVA_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch/mvr-b4-evidence/tmp -Dmvr.data.dir=/var/tmp/forge/lid-batch/mvr-b4-evidence/data
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  config: {:throw? true}
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
2026-09-07T17:46:15.474Z anvil-server INFO [marvin-voice-remote.channel:187] - :dictations-restored :count 0
2026-09-07T17:46:15.533Z anvil-server INFO [marvin-voice-remote.channel:1009] - :bridge3-sessions-restored :count 0
2026-09-07T17:46:15.537Z anvil-server INFO [marvin-voice-remote.channel:1038] - :bridge3-convos-restored :seats 2 :turns 5
581 tests, 7981 assertions, 0 failures.
```

### ~/bin/clj-kondo --lint docs/intent/registry.edn — exit 0

```text
linting took 8ms, errors: 0, warnings: 0
```

### Supplemental review checks

```text
PASS: all 39309 original ARCHITECTURE.md bytes preserved, including Appendices A and B.
PASS: exactly section 13 with ten subsections appended.
PASS: no docs/high-level-design.md created.
PASS: only :doc metadata changed across 19 rows; statuses, promises, witnesses and :see links unchanged.
PASS: all :doc values are nonblank strings; all :see values are keyword vectors naming known IDs.
PASS: all changed §13 pointers and all named registry IDs link in both directions across ten subsections.
```

`git diff --check` passed. `check-links.clj` is a one-off review script retained
only in the evidence directory; it parses EDN, compares the full registry with
the base after removing `:doc`, checks pointer shapes and known `:see` targets,
and checks all section 13 links in both directions.

## Doubts and limits

- The prompt says the intent contract checks `:doc`/`:see` shapes. The current
  `test/marvin_voice_remote/intent_contract_test.clj` does not: it checks row
  basics, active obligations, bidirectional tags, and claimed witness names.
  The supplemental EDN check covers shapes and reciprocal links for this change;
  no persistent test changes were added to this documentation-only batch.
- Sections 13.9–13.10 document ledger governance. There is no dedicated active
  meta-intent for enrich-on-touch. They name `ARCH-D2`, `SYS-S13`, and `POL-P6`
  as representative documented rows and honestly distinguish their unenforced
  traceability tier from active obligations enforced by the intent contract.
  Those three rows and `POL-P10` retain `:documented`: adding cross-link metadata
  supplies no behavioral witness and does not justify promoting them.
- Section 13 records existing contracts, defaults, and evidence boundaries;
  it neither authorizes trial enablement nor claims new simulator/physical
  acceptance. The mic-gate prose specifically says capture onset, matching the
  rendered `onsetReady` guard rather than implying microphone hardware shutdown.
- The HLD audit's G1/G2/G3/G4 labels describe audit gaps and must not be confused
  with section 8's existing architectural decision gates; the new text names
  the full registry IDs to avoid that ambiguity.
