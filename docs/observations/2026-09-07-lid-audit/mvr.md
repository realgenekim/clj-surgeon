# LID / HLD audit — marvin-voice-remote @ origin/main d170f3d

Read-only audit, 2026-09-07. Scope: every commit on `origin/main` since 2026-08-15
touching `src`, `resources`, or `test`. No edits, no commits, no checkouts.

## Raw commands

```bash
cd /home/forge/src/marvin-voice-remote && git fetch origin
git rev-parse origin/main                       # d170f3d5edea6faa39396ea8b3418e29b2e2b4b1
ls docs/high-level-design.md                    # ABSENT
ls docs/intent/                                 # registry.edn, SUMMARY.md, 00-FABLE-SPEC-lid-bootstrap.md
git log origin/main --since=2026-08-15 --format='%h %ad %an %s' --date=short -- src resources test
git log origin/main --format='%h %ad %an %s' --date=short -- docs/intent/registry.edn
for c in <each sha>; do git log -1 --format=... $c; git show --numstat --format='' $c -- src resources test; done
grep -rnE '(;;|//|#)\s*INTENT:' src resources bin
grep -rnE '(;;|//|#)\s*INTENT-TEST:' test
grep -nE '^\s*\{:id ' docs/intent/registry.edn ; grep -oE ':status :[a-z-]+' docs/intent/registry.edn | sort | uniq -c
sed -n '1,149p' test/marvin_voice_remote/intent_contract_test.clj
git show 6d4cecf -- src/marvin_voice_remote/channel.clj src/marvin_voice_remote/friction_ui.clj
git show 6d4cecf -- test/marvin_voice_remote/channel_test.clj test/marvin_voice_remote/friction_ui_test.clj
git show c0b8cfe -- test | grep -E '^\+\(deftest'
cat test/director_control_invariants.pl
grep -nE '^[a-z0-9_.-]+:|swipl|director-control-contract' Makefile
```

## State of the LID machinery (healthy)

- `docs/intent/registry.edn` — 60 rows: **21 `:active`** (contract-enforced), 39 `:documented`
  (ARCHITECTURE.md's D1–D12 / S1–S14 / P1–P13 prose invariants, enrich-on-touch).
- `test/marvin_voice_remote/intent_contract_test.clj` — bidirectional contract, in the
  ordinary suite (`make runtests-once`). Harvests `;; INTENT:` from `src/`,
  `// INTENT:` from `resources/public/js/`, `# INTENT:` from `bin/`, `;; INTENT-TEST:` from
  `test/`. Enforces: unique forever ids · known status · EARS on every row · **misreadings and
  `:tests` required on every `:active` row** · both traceability directions · every claimed
  `:tests` deftest actually exists. Regexes are disjoint (colon after INTENT).
- Rung d exists and **is wired**: `test/director_control_invariants.pl` runs from
  `make director-control-contract`, which is a prerequisite of BOTH `runtests-once` and
  `check-pages`. It contains a real counterexample case
  (`old_non_null_handle_policy_has_the_field_counterexample`) — an oracle that can reject.
- All 21 active ids resolve to a code tag and a test tag (verified by grep, incl. the
  `bin/startup-friction-repro` shell tag for `STARTUP-FRICTION-REPRO-F3`).

**HLD: `docs/high-level-design.md` does NOT exist in this repo.** The de-facto HLD is
`/ARCHITECTURE.md` (Architecture of Record; §2 D1–D12 decisions, §3.1 S1–S14 system
invariants, §3.2 P1–P13 product policy). Its last commit is `7a56546 2026-07-26` — it has
**not been touched by any of the eleven source commits in this audit window.**

## Ledger

| sha | date | author | files (short) | promise? | intent row | witness | rung | HLD | verdict |
|---|---|---|---|---|---|---|---|---|---|
| `34a6d96` | 08-16 | code-director | director-capture-control.js; +mjs test, +`.pl` oracle, director_capture_contract_test | **Y** — recovery: a CLOSED command stream reconnects under the SAME client identity (no second capture slot) | `MVR-DIRECTOR-CONTROL-LEASE-001` (added same commit in a branch-local shape; normalized into the ledger schema at the `6f6e9d1` merge) | `capture-command-client-acks-exact-results` + `director_capture_control_test.mjs` + `director_control_invariants.pl` | **d** (oracle wired into `runtests-once` + `check-pages`, has a `fail`-style counterexample) | D7 (leases) generically; no Director-stream section | **INTEGRATED** |
| `87a3cb7` | 08-31 | forge-bridge | 8 src/js tag sites; `intent_contract_test.clj` (+146); registry (49 rows); +zz3-ios witness | N/A — this IS the machinery | 10 rich + 39 thin rows seeded | contract test itself | a→c | seeds `:doc` pointers into ARCHITECTURE.md | **INTEGRATED** |
| `c0b8cfe` | 08-31 | forge-bridge | protocol edn; bridge3_new.clj; **reducer/core.clj +213**; reducer_session.clj; core_test +135, protocol_test +28, full_turn_test +13 | **Y** — `:cmd/over` posts the JOINED turn exactly once via a one-shot `:over-requested?`; `:cmd/cancel` discards and stays in HF; button arming is the reducer's ruling (P4 disabled-honesty) | **NONE** | tests exist and are strong (`manual-over-flushes-a-live-capture-and-the-next-transcript-posts-the-turn`, `manual-over-with-only-a-stash-posts-it-immediately`, `manual-over-on-an-empty-session-posts-nothing`, `manual-cancel-drops-the-stash-discards-capture-and-stays-in-hf`, `manual-over-and-cancel-are-registered-and-rename-to-cmd`) but carry **no `INTENT-TEST:` tag**, so the contract does not enforce them | b (unlinked) | none (D6 covers "one versioned protocol" only) | **GAP — G2** |
| `e359bb5` | 08-31 | forge-bridge | merge of `c0b8cfe` | N/A merge | — | — | — | — | **N/A** |
| `f2db25e` | 08-31 | forge-bridge | `test/golden/asset-manifest.edn`, `bridge3-new.html` | N — golden re-bless for the tag bytes + capture row | — | goldens | — | — | **N/A (cosmetic)** |
| `783e5a2` | 09-01 | forge-bridge | reducer/policy.clj, reducer/core.clj, reducer_session.clj; core_test +118 | **Y** — barge-in ships default-OFF as a drift-checked literal, flipped only at runtime via `policy/effective`; spoken SKIP skips and posts nothing | `BARGE-IN-FIELD-GATE-F2`, `SPOKEN-SKIP-F2` (same commit, 5 misreadings each) | `barge-in-defaults-off-and-flips-only-through-effective` (asserts `effective` ≡ `current` when env unset, and that arming of `:barge-stop` follows the flag); `a-spoken-skip-skips-the-current-reply-and-posts-nothing`, `…-leaves-the-capture-stash-untouched` | b/c (+`:rung-d-candidate true`) | `:see [:POL-P1 :ARCH-D3]`, `[:POL-P6 :POL-P13]` | **INTEGRATED** |
| `f3ef69a` | 08-31 | surgeon1 | bridge-player.js; **reducer/echo_guard.clj (new, 102)**; tts.clj; +fixtures; bridge_player_long_tts_test.js, echo_guard_test, friction_harness_test | **Y** — long-TTS prebuffer for automatic replies only; pure dark echo classifier | rows land **two commits later** at `a40e2e4` (`LONG-TTS-PREBUFFER-F4`, `SELF-ECHO-QUARANTINE-F5`, `STARTUP-FRICTION-REPRO-F3`) — inside the 5-commit window | `controlled-long-reply-*`, `strong-recent-self-echo-is-quarantined-and-retained`, `startup-repro-separates-readiness-termination-and-reply-playback` | b/c | S13/P10/P12 via `:see` | **INTEGRATED (row lag: 2 commits)** |
| `21c72db` | 08-31 | surgeon1 | merge of the F2 lane | N/A merge | — | — | — | — | **N/A** |
| `a40e2e4` | 08-31 | surgeon1 | reducer/core.clj, echo_guard.clj, policy.clj, bridge-player.js; core_test +61, echo_guard_test, intent_contract_test tweak | **Y** — quarantine only on *acknowledged* (playback-finished) replies, retained with evidence, never silently dropped | `SELF-ECHO-QUARANTINE-F5` (+F3/F4) — same commit | `f5-an-acknowledged-reply-echo-is-quarantined-with-evidence`, `f5-spoken-skip-composes-with-echo-evidence-without-fabricating-playback` — assert status, `posted?` false, verbatim text retention, diagnostic kind | c | P1/P10/S13 via `:see` | **INTEGRATED** (nit: `echo_guard.clj` is named a site in the row but carries no `;; INTENT:` tag; the only F5 code tag is in `core.clj`) |
| `6f6e9d1` | 09-01 | mayor | merge origin/main → Director lane (3 weeks stranded) | N/A merge — but it resolved an **add/add conflict on `registry.edn`** by carrying the Director row into this ledger's schema (`:requirement`→`:ears`) so the contract could see it | — | — | — | — | **N/A (merge done correctly)** |
| `29a25d1` | 09-01 | mayor | server.clj (+19); shadow_test, reducer_lab_test, capture_archive_test | **Y** — one router constructor for tests and production (a test router that could never ship had passed for weeks) | `ONE-ROUTER-CONSTRUCTOR-M01` — same commit, 4 misreadings | `routes-are-wired`, `routes-are-wired-test`, `shadow-routes-are-wired`, all tagged, all built through `server/make-router` | b, reaching toward e (single constructor) | none | **INTEGRATED** |
| `ab267f9` | 09-01 | codex-sol | channel.clj, bridge3_new.clj, **friction_ui.clj (new, 99)**, policy.clj, css, js; goldens; friction_ui_test +94, constant_drift_test, shadow_test | **Y** — production enablement verdicts: barge-in stays OFF globally, F4 prebuffer ON with `?longttsbuffer=0` rollback, F5 in `:observe` (post gate stays open), UI note is a projection of the same effective state | `BARGE-IN-SURFACE-DEFAULT-F2`, `LONG-TTS-PROD-DEFAULT-F4`, `ECHO-OBSERVE-PROD-DEFAULT-F5`, `FRICTION-POLICY-NOTE-UI` — same commit, recorded **separately from the mechanism rows** (exemplary: enablement is its own promise) | `production-friction-policy-defaults-are-safe-and-reversible` (asserts `policy/version` 3, literal defaults, and page-level override precedence), `friction-ui-note-reflects-effective-state-including-off`, `served-friction-notes-match-the-bootstrapped-state`, `observe-mode-flags-without-requesting-quarantine` | b/c | none | **INTEGRATED** |
| `6d4cecf` | 09-02 | forge-bridge | **channel.clj +100** (`force-end-param`, `?controls`/`?micgate` render, variant links), friction_ui.clj +16; bridge4 golden; channel_test +48, friction_ui_test +69 | **Y ×3** — (a) a tapped OVER posts `force_end=1` on the SAME `/api/bridge4/keyword-check` request so tap and end-word share one dedup/archive/enqueue/broadcast path, only the last-word test bypassed; (b) `?controls`/`?micgate` default OFF and the flagless page stays byte-identical (the frozen incident artifact); (c) `micGate` is the INVERSE of the surface barge-in flag | **NONE** — `registry.edn` untouched by this commit and by every commit after it | tests exist and are strong (`bridge4-keyword-check-honors-a-tapped-over-force-end` — asserts exactly one message + one broadcast turn, sentinel stripping, blank-still-not-a-send, cancel-word-still-not-a-send; `bridge4-controls-flag-is-off-by-default-and-purely-additive` — asserts absence of `id=over`/`micGate`/`force_end` in the default page and `micGate:false` when `?barge=1`) but carry **no `INTENT-TEST:` tag** | b (unlinked) | none | **GAP — G1, G3, G4** |

Author distribution: forge-bridge (Buster seat) 5, surgeon1 3, mayor 2, codex-sol 1,
code-director 1. Nothing in this window was authored by `forge-anvil` (this seat).

## GAPs ranked by blast radius

### G1 — tapped OVER (`force_end=1`) on the bridge4 send path — `6d4cecf`, channel.clj
**What silently breaks:** this is the production send path for Gene's *driving* surface, the
one the whole incident (`marvin-voice-remote-3ug`) was about. `ended` became
`(or word-ended force-end?)`. A later maintainer who "tidies" that branch — moving the
force-end test above dedup, or short-circuiting to an early enqueue — gets double posts (the
dedup cache is consulted *before* this point) or silently drops the archive/broadcast half of
the turn. Nothing in the registry records that the ONLY difference between a tap and a spoken
"over" is the last-word test. The witness exists but is unlinked, so a refactor that rewrites
`channel_test.clj` alongside `channel.clj` leaves the contract green.

**Draft row (one outcome):**
```clojure
{:id :TAPPED-OVER-ONE-SEND-PATH-B4C1
 :status :active
 :see [:SPOKEN-SKIP-F2 :POL-P6]
 :ears "While a bridge4 hands-free utterance is open, when the client finalizes it with force_end=1 instead of a trailing end word, the server shall finalize it through the same dedup, archive, enqueue and broadcast path a spoken end word takes, bypassing only the last-word test."
 :misreadings
 ["Give a tapped OVER its own endpoint or its own enqueue call — a second send path is a second authority and will drift from the spoken one (dedup id reuse, archive verdict, bridge3 turn append)."
  "Let force_end skip the blank-transcript refusal — a tap on silence must still not post."
  "Let force_end make a trailing CANCEL word post — force_end ends the utterance, it does not overrule a discard."
  "Strip the end-word sentinel only on the spoken path — a user who taps AND says 'over' must not see the sentinel in the posted text."
  "Treat force_end as client state — it is a request parameter on the SAME keyword-check request, so a lost response replays into the same dedup entry."]
 :boundaries ["force_end=1 with no end word -> ended true, raw transcript posted once"
              "force_end=1 with a trailing end word -> sentinel stripped"
              "force_end=1 on a blank/whitespace transcript -> ended false, nothing enqueued"
              "no force_end, trailing cancel word -> ended false, nothing enqueued"
              "repeat of the same dedup_id -> the cached terminal result, never a second message"]
 :tests [:bridge4-keyword-check-honors-a-tapped-over-force-end]
 :rationale "Incident marvin-voice-remote-3ug, 2026-09-02 (commit 6d4cecf): car-speaker self-echo drove an OVER/CANCEL button trial on the live driving surface; the tap was deliberately routed onto the existing keyword-check request so tap and end word cannot drift. Site: channel.clj force-end-param / handle-bridge4-keyword-check."}
```
**Witness shape:** the existing `bridge4-keyword-check-honors-a-tapped-over-force-end`
already asserts the observable outcome the caller sees — HTTP 200, `ended` true, exactly one
entry in `channel-state[:messages seat]`, exactly one `"you"` turn in `bridge3-convos`, the
sentinel stripped, and both refusals (blank, cancel-word). It needs a `;; INTENT-TEST:` tag
and one added case: the same `dedup_id` replayed with `force_end=1` must return the cached
terminal result and leave the message count at one — that is the idempotency half of the
promise, which no current assertion covers.

### G2 — `:cmd/over` / `:cmd/cancel` on the reducer command path — `c0b8cfe`, reducer/core.clj (+213)
**What silently breaks:** OVER commits the live capture as a PARTIAL and arms a *one-shot*
`:over-requested?`; the completing transcript then posts the JOINED turn as ONE message.
If a refactor makes that flag sticky, the next transcript posts twice; if the join is dropped,
one utterance splits into two messages. CANCEL's "discard and stay in HF" is the destructive
sibling — losing "stay in HF" drops the user out of hands-free mid-drive. Five good deftests
exist, all untagged and unregistered, and the `SPOKEN-SKIP-F2` row *references*
`:over-requested?` in its boundaries without ever registering it.

**Draft rows (two outcomes → two rows):**
```clojure
{:id :TAPPED-OVER-JOINS-ONE-TURN-C01
 :status :active :see [:SPOKEN-SKIP-F2 :POL-P4]
 :ears "While a hands-free capture is live, when :cmd/over arrives, the reducer shall commit the live capture as a partial and post the joined turn exactly once when the completing transcript lands."
 :misreadings ["Post the partial immediately and the completing transcript as a second message — one utterance must reach the ledger as ONE turn."
               "Leave :over-requested? set after the join — it is one-shot; a sticky flag posts the NEXT turn early too."
               "Post nothing when there is only a stash and no live capture — a stash-only OVER posts immediately."
               "Post an empty turn on an empty session — an OVER with nothing to send is a named no-op, not a blank message."
               "Arm the button from client state — arming is the reducer's ruling (P4 disabled honesty)."]
 :tests [:manual-over-flushes-a-live-capture-and-the-next-transcript-posts-the-turn
         :manual-over-with-only-a-stash-posts-it-immediately
         :manual-over-on-an-empty-session-posts-nothing]}

{:id :TAPPED-CANCEL-DISCARDS-STAYS-HF-C02
 :status :active :see [:SPOKEN-SKIP-F2]
 :ears "While a hands-free capture or stash exists, when :cmd/cancel arrives, the reducer shall discard both and remain in hands-free mode."
 :misreadings ["Return the session to idle — CANCEL discards an utterance, it does not end the conversation."
               "Leave the stash behind because 'only the live capture was cancelled' — CANCEL is the destructive one; SKIP is the one that preserves the stash."
               "Post the discarded text anywhere (ledger, broadcast, archive as accepted)."
               "Let CANCEL and OVER race — CANCEL wins; precedence is CANCEL > SKIP > OVER."]
 :tests [:manual-cancel-drops-the-stash-discards-capture-and-stays-in-hf]}
```
**Witness shape:** the five existing deftests already assert observables (ledger contents,
message count, `:mode` after cancel, registered protocol kinds); they need tags plus one
state-space case (rung c) enumerating {live capture?} × {stash?} × {OVER, CANCEL} — eight
cells, each naming the expected posted-message count and resulting `:mode`, so the failure
message names the exact cell.

### G3 — flags default OFF; the flagless page is the frozen artifact — `6d4cecf`
**What silently breaks:** Gene's documented rollback for the incident is literally "reload
/bridge4 without flags; frozen bytes are the pinned artifact." If a later change makes
`?controls`/`?micgate` default ON (or leaks a button/`micGate` key into the default render),
the rollback path stops existing and the golden re-bless hides the move. `friction_ui_test`
asserts absence today, untagged.

**Draft row:**
```clojure
{:id :B4-TRIAL-FLAGS-DEFAULT-OFF-B4C2
 :status :active :see [:BARGE-IN-SURFACE-DEFAULT-F2 :FRICTION-POLICY-NOTE-UI]
 :ears "While /bridge4 is rendered without ?controls or ?micgate, when the page bytes are produced, the server shall emit the frozen artifact — no OVER/CANCEL buttons, no controls/micGate bootstrap keys, no force_end wiring, and no trial words in the friction note."
 :misreadings ["Default the trial ON because the field gate went well — the flagless page IS the rollback."
               "Ship the buttons but disable them in CSS — absence of the bytes is the contract, not visual absence."
               "Re-bless the bridge4 golden as part of a behavioral change — the golden may move only for an approved, named page delta."
               "Treat ?controls=1 as also enabling the mic gate — the two flags are independent."]
 :tests [:bridge4-controls-flag-is-off-by-default-and-purely-additive]}
```
**Witness shape:** already written as a byte-absence assertion over
`handle-bridge4-page` output (`id=over`, `id=cancel`, `controls:true`, `micGate`,
`hfControlsTick`, `force_end`) plus note-text absence; tag it, and pin it to the golden by
asserting the flagless body equals `test/golden/bridge4.html` so a re-bless cannot quietly
carry a trial byte into the frozen page.

### G4 — `micGate` is the inverse of barge-in — `6d4cecf`
**What silently breaks:** the exact incident. `micGate:true` means "gate the mic while a reply
plays"; it is emitted as `(if barge-in? false true)`. Invert that by accident and the driving
surface has an always-open mic during playback again — car-speaker self-echo, the thing the
whole commit exists to stop. One assertion covers it (`?micgate=1&barge=1` → `micGate:false`),
untagged, and its inverse relationship is recorded nowhere.

**Draft row:**
```clojure
{:id :MIC-GATE-INVERTS-BARGE-IN-B4C3
 :status :active :see [:BARGE-IN-SURFACE-DEFAULT-F2 :SELF-ECHO-QUARANTINE-F5 :POL-P1]
 :ears "While ?micgate=1 is set on bridge4, when the page bootstrap is rendered, the server shall emit micGate as the negation of the page's effective barge-in flag."
 :misreadings ["Emit micGate:true whenever ?micgate=1 — with barge-in ON the mic is deliberately hot, and gating it would silently disable barge-in."
               "Read the process default instead of the page's effective value — ?barge=1 must reach this negation."
               "Bundle the mic gate into ?controls — they are independent flags with independent field evidence."
               "Implement the gate in the player instead of the bootstrap — the server owns the policy projection (Datastar doctrine)."]
 :tests [:bridge4-controls-flag-is-off-by-default-and-purely-additive]}
```
**Witness shape:** a three-cell table over `{no flags, ?micgate=1, ?micgate=1&barge=1}`
asserting the emitted `micGate` token and the presence/absence of the
`if(BP.micGate&&playing)` guard — hand-written literals, not derived from `friction-ui`.

### G5 (minor) — `echo_guard.clj` carries no `INTENT:` tag
`SELF-ECHO-QUARANTINE-F5`'s `:rationale` names `reducer/echo_guard.clj classify/remember-playback`
as a site, but the only F5 code tag is in `reducer/core.clj:2443`. The contract is satisfied
(one site is enough) while the classifier itself — the file most likely to be rewritten —
is untraceable. One-line fix.

## HLD

**`docs/high-level-design.md` is absent.** Do NOT create a competing document: `/ARCHITECTURE.md`
is the binding Architecture of Record and already carries the D/S/P structure an HLD would
duplicate, and 39 registry rows point into it by section. The real HLD-shaped gap is that
ARCHITECTURE.md has not moved since **2026-07-26** while eleven source commits landed —
every promise below is HLD-uncovered.

Recommended: one new section, `## 13 — Surface flags, trial gates, and the command path`,
covering the load-bearing promises this audit found:

```
13.1 One send path per surface   — spoken end word and tapped OVER share dedup/archive/enqueue/broadcast (G1)
13.2 Transport commands          — OVER joins, SKIP preserves the stash, CANCEL discards; precedence CANCEL > SKIP > OVER (G2, SPOKEN-SKIP-F2)
13.3 The frozen artifact rule    — a flagless page render is the rollback; trial bytes appear only under an explicit flag (G3)
13.4 Acoustic surface policy     — barge-in OFF globally; mic gate is its inverse; per-page ?barge/?micgate are the honest gate (G4, BARGE-IN-SURFACE-DEFAULT-F2)
13.5 Enablement vs mechanism     — a production default is its own promise, registered separately from the feature (F4/F5 prod rows)
13.6 Echo evidence authority     — only playback-finished proves Marvin's words reached the air (SELF-ECHO-QUARANTINE-F5, P10)
13.7 One router constructor      — tests and production build the route table the same way (ONE-ROUTER-CONSTRUCTOR-M01)
13.8 Director control lease      — a CLOSED stream reconnects under the same client identity (MVR-DIRECTOR-CONTROL-LEASE-001)
13.9 Where the ledger lives      — docs/intent/registry.edn is the enforced index; ARCHITECTURE.md D/S/P are its :documented tier
13.10 Enrich-on-touch            — the next change inside a :documented row upgrades it to :active
```

## Verdict

11 source-bearing commits. **8 INTEGRATED** (rows in the same commit or within two),
**2 N/A** (merges), **1 GAP commit carrying three unregistered promises** (`6d4cecf`), plus
**1 GAP commit** from the reducer command path (`c0b8cfe`). The practice is working well —
`ab267f9`'s separation of enablement rows from mechanism rows and `29a25d1`'s misreadings are
better than the skill's own examples. The failure mode is the same in both gaps and worth
naming: **the tests were written, and written well; only the two-line link was skipped**, so
the contract stays green while the promise is undefended.
