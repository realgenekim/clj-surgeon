# MVR LID batch 3 — G3 and G4

Worktree: `/home/forge/src/mvr-lid4`; branch: `astra/lid-batch-3`.
Base: `3a2898f2a9d7c187ff6d1f17cbb8c2b59b1bfb25` (batch 2).
Commit: `e0680b6c5e49785c62d9dd3a11f7ff2170cb1935` (local only; not pushed).
Author: environment identity `forge-anvil <forge-anvil@anvil>`.
Both requested co-author trailers are present. Working tree is clean.
Tracking: `marvin-voice-remote-3zf` (closed).
Evidence: `/var/tmp/forge/lid-batch/mvr-b3-evidence/`.

## Outcome and files

Both gaps are registered as active intents with the ledger's EARS wording, four
misreadings each, source tags, and linked witness tags. No production behavior fix
was needed; both red mutations were removed.

- `docs/intent/registry.edn`: appended `BRIDGE4-FLAGLESS-IS-FROZEN-G3` and
  `BRIDGE4-MICGATE-INVERTS-BARGE-G4`.
- `src/marvin_voice_remote/channel.clj`: only two `;; INTENT:` comments remain
  changed, at `bridge4-page-html` and its micGate bootstrap projection.
- `test/marvin_voice_remote/friction_ui_test.clj`: tagged the existing
  `bridge4-controls-flag-is-off-by-default-and-purely-additive` witness; added golden
  equality and an isolated rendering fixture; added the tagged three-cell
  `bridge4-micgate-inverts-effective-barge-in` witness.

`test/golden/bridge4.html` was never edited or re-blessed. SHA-256:
`f1fcf73234eb9d0fc71f4a0de9ccfcaa2b32735cef7da35bbadc86d19cd8cc30`.
No native implementation or release configuration changed. `.mcp.json` and `.codex/`
were untouched. No push or live-service port contact occurred. The existing full-turn
wire test starts its own server with `:port 0`; the host ephemeral range is
32768–60999, outside the forbidden ports. Temporary test data and evidence were
redirected under the requested root. Beads generated an untracked auto-import scratch
file; it was moved into this evidence directory and is not committed.

## Registered rows

The following are the appended registry rows (including the registry's closing delimiters):

```clojure
  {:id :BRIDGE4-FLAGLESS-IS-FROZEN-G3
   :status :active
   :see [:BARGE-IN-SURFACE-DEFAULT-F2 :FRICTION-POLICY-NOTE-UI]
   :ears "While /bridge4 is rendered without ?controls or ?micgate, when the page bytes are produced, the server shall emit the frozen artifact — no OVER/CANCEL buttons, no controls/micGate bootstrap keys, no force_end wiring, and no trial words in the friction note."
   :misreadings
   ["Default the trial ON because the field gate went well — the flagless page IS the rollback."
    "Ship the buttons but disable them in CSS — absence of the bytes is the contract, not visual absence."
    "Re-bless the bridge4 golden as part of a behavioral change — the golden may move only for an approved, named page delta."
    "Treat ?controls=1 as also enabling the mic gate — the two flags are independent."]
   :boundaries
   ["no flags -> test/golden/bridge4.html equality with fresh seat state and only volatile build/client/clock values fixed"
    "no flags -> independent trial-byte absence assertions still reject a contaminated golden re-bless"
    "?controls=1 alone -> buttons and force_end wiring, no micGate key or player guard"
    "?micgate=1 alone -> mic gate, no OVER/CANCEL buttons"
    "variant navigation links remain in the frozen artifact; trial words are forbidden in its friction note, not its links"]
   :tests [:bridge4-controls-flag-is-off-by-default-and-purely-additive]
   :rationale "Incident marvin-voice-remote-3ug, commit 6d4cecf, audit G3: reloading /bridge4 without trial flags is the frozen rollback. Golden equality and independent byte-absence checks must both hold; a re-bless alone cannot authorize a trial leak. Site: channel.clj bridge4-page-html; ARCHITECTURE.md D11/S0."}

  {:id :BRIDGE4-MICGATE-INVERTS-BARGE-G4
   :status :active
   :see [:BARGE-IN-SURFACE-DEFAULT-F2 :SELF-ECHO-QUARANTINE-F5 :POL-P1]
   :ears "While ?micgate=1 is set on bridge4, when the page bootstrap is rendered, the server shall emit micGate as the negation of the page's effective barge-in flag."
   :misreadings
   ["Emit micGate:true whenever ?micgate=1 — with barge-in ON the mic is deliberately hot, and gating it would silently disable barge-in."
    "Read the process default instead of the page's effective value — ?barge=1 must reach this negation."
    "Bundle the mic gate into ?controls — they are independent flags with independent field evidence."
    "Implement the gate in the player instead of the bootstrap — the server owns the policy projection (Datastar doctrine)."]
   :boundaries
   ["no flags -> no micGate token and no if(BP.micGate&&playing) guard"
    "?micgate=1 -> micGate:true and the player guard present"
    "?micgate=1&barge=1 -> micGate:false and the player guard present but inert"]
   :tests [:bridge4-micgate-inverts-effective-barge-in]
   :rationale "Incident marvin-voice-remote-3ug, commit 6d4cecf, audit G4: the mic gate prevents playback from starting a self-echo capture, while explicit barge-in deliberately keeps the mic hot. The witness uses literal rendered tokens and guard expectations, independent of the policy projection implementation. Site: channel.clj bridge4-page-html BP bootstrap."}]}
```

## G3: red first, restored green

The existing byte-absence deftest now compares the flagless handler's full body with
`(slurp "test/golden/bridge4.html")` using equality. Its fixture isolates the conversation,
voice selection and text-size atoms without mutating persisted state. Build SHA,
SSE client ID and deployment age are bound to the existing golden's tokens. Only
numeric `?v=` cache-busters are normalized after rendering. No markup, whitespace,
trial key, control, or note is stripped. The policy fixture is the existing
`policy/effective = policy/current` binding.

The independent assertions for `id=over`, `id=cancel`, `controls:true`, `micGate`,
`hfControlsTick` and `force_end` remain, alongside the existing note/state and
controls/micgate independence checks. A re-bless containing the planted trial token
would still fail its independent absence assertion.

Before the first execution of the strengthened test, planted one unconditional
trial bootstrap token in the real renderer:

```diff
           (when (:controls? friction-state)
             ",controls:true")
+          ",controls:true"
```

Command:

```sh
bin/kaocha --focus marvin-voice-remote.friction-ui-test/bridge4-controls-flag-is-off-by-default-and-purely-additive --no-color
```

Red (`g3-red.log`, exit 2): **1 test, 36 assertions, 2 failures**. The failures
were golden equality at line 122 and absence of `controls:true` at line 128 at
execution time. The full HTML diff is preserved in the raw log. Mutation receipt:
`g3-planted.diff`.

Removed only the planted token and ran the same command: **1 test, 36 assertions,
zero failures**, exit 0 (`g3-green.log`). No expectation or golden changed between
red and green.

## G4: literal table, red first, restored green

| Request | Expected emitted token | `if(BP.micGate&&playing)` present? |
|---|---|---|
| no flags | absent (`nil`) | false |
| `?micgate=1` | `micGate:true` | true |
| `?micgate=1&barge=1` | `micGate:false` | true |

All expectations are hand-written literals. The third cell retains the emitted guard;
its condition is inert because the bootstrap sets micGate false. The witness does
not calculate expected values from `friction-ui/effective-state`.

Before the new witness's first execution, planted the inverted expression:

```diff
- (str ",micGate:" (if (:barge-in? friction-state) "false" "true"))
+ (str ",micGate:" (if (:barge-in? friction-state) "true" "false"))
```

Command:

```sh
bin/kaocha --focus marvin-voice-remote.friction-ui-test/bridge4-micgate-inverts-effective-barge-in --no-color
```

Red receipt (`g4-red.log`, exit 2; mutation in `g4-planted.diff`):

```text
FAIL in marvin-voice-remote.friction-ui-test/bridge4-micgate-inverts-effective-barge-in (friction_ui_test.clj:200)
?micgate=1
bootstrap projects the negation of effective barge-in
Expected:
  "micGate:true"
Actual:
  -"micGate:true" +"micGate:false"

FAIL in marvin-voice-remote.friction-ui-test/bridge4-micgate-inverts-effective-barge-in (friction_ui_test.clj:200)
?micgate=1&barge=1
bootstrap projects the negation of effective barge-in
Expected:
  "micGate:false"
Actual:
  -"micGate:false" +"micGate:true"
1 tests, 6 assertions, 2 failures.
```

Restored only the expression; the identical witness passed: **1 test, 6 assertions,
zero failures**, exit 0 (`g4-green.log`). Both flagged token cells had failed red;
the flagless cell and all guard-presence assertions passed throughout.

## Full entry, in requested order

All commands sourced `mvr-b3-evidence/env.sh`:

```sh
export TMPDIR=/var/tmp/forge/lid-batch/mvr-b3-evidence/tmp
export _JAVA_OPTIONS='-Djava.io.tmpdir=/var/tmp/forge/lid-batch/mvr-b3-evidence/tmp -Dmvr.data.dir=/var/tmp/forge/lid-batch/mvr-b3-evidence/data'
```

The inherited `JAVA_TOOL_OPTIONS` announces another temporary directory;
`_JAVA_OPTIONS` takes precedence and redirects both JVM temp and test persistence.

1. `make runtests-once`: exit 0; **581 tests, 7981 assertions, zero failures**.
   Beads export validation passed. Full receipt: `make-runtests-once.log`.
2. `bin/kaocha`: exit 0; **581 tests, 7981 assertions, zero failures**.
   Full receipt: `kaocha-full.log`. Both full entries include the bidirectional
   intent contract and witness-name checks.
3. `~/bin/clj-kondo --lint docs/intent/registry.edn src/marvin_voice_remote/channel.clj test/marvin_voice_remote/friction_ui_test.clj --cache false`:
   exit 2; **0 errors, 11 warnings, 7 informational findings**.
   Full receipt: `clj-kondo.log`.

Prolog output from the make entry:

```text
director capture-control reconnect contract: PASS
% [1/4] director_control_..d_stream_reconnects ...... passed (0.003 sec)
% [2/4] director_control_..es_not_claim_a_slot ...... passed (0.000 sec)
% [3/4] director_control_..ield_counterexample ...... passed (0.000 sec)
% [4/4] director_control_..tes_have_one_action ...... passed (0.000 sec)
```

These four existing Prolog cases are `active_closed_stream_reconnects`,
`inactive_capture_does_not_claim_a_slot`,
`old_non_null_handle_policy_has_the_field_counterexample`, and
`all_stream_states_have_one_action`. They prove the existing Director control lease
oracle still runs and passes; they are not a new Prolog model of G3/G4.

Because kondo was nonzero, linted copies of the same three files from base `3a2898f`
using the same executable, working directory and `--cache false`. Baseline also exits
2 with **0 errors, 11 warnings, 7 info** (`clj-kondo-baseline.log`). Comparison keeps
relative file, severity, message and multiplicity, normalizing only the baseline
prefix and source locations:

```text
Baseline: 18 findings; batch: 18 findings.
Same relative file, severity, message, multiplicity (only source locations normalized): True
New findings: {}
Removed findings: {}
```

No findings were suppressed or added. `--cache false` follows batch 2's workaround
for an analyzer cache exception and avoids writing a lint cache. `git diff --check`
passes. No full-suite failures needed repair.

## Doubts and limits

- Golden equality is for a fresh default seat with volatile metadata fixed to the
  existing artifact's tokens. It does not demand identical raw wall-clock/client-ID
  bytes across real page loads or freeze each user's persisted conversation.
- The tests bind the process policy to the checked-in production default. G4 proves
  the requested three page-flag cases, including the effective `barge=1` override;
  it does not enumerate every environment override or query spelling.
- The witnesses assert rendered tokens and guard presence, as requested. They do
  not execute browser microphone/playback behavior or prove physical car-speaker
  self-echo prevention. No simulator or physical-device claim is made.
- Equality plus the independent known-byte checks catches the planted leak and
  prevents its acceptance through a golden re-bless alone. It is not an immutable
  approval system: future named page deltas still require the documented review.
- Raw kondo is nonzero because of unchanged baseline warnings; the batch introduces
  no lint debt. The Prolog evidence is the existing Director oracle, not a new
  oracle for these two surface contracts.

G3/G4 are complete. Broader architecture-ledger expansion remains outside this
batch; no new follow-up defect was found.
