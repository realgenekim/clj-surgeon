# Curtain Call speaker-identity LID ratchet report

Date: 2026-09-07
Branch: `astra/speaker-identity-lid`
Base refactor: `ce49083e`
Commit: `2cd2dd4e` (`test: ratchet canonical speaker identity ownership`)

## Outcome

The shared name/email overlay now has a linked-intent chain from the high-level
design through a dedicated `SPK-IDENT` design node and enforced EARS specification,
the append-only registry, source/test markers, an exact source-owner scanner, and an
independent behavioral witness. The scanner permits exactly the canonical helper and
the documented `exports.clj` presence-aware divergence.

This is rung b, the highest useful rung for this regression class. The failure is a
specific duplicate-definition and two-branch behavior risk, not a family of ordering,
retry, concurrency, or relational states that would benefit from a generative or
Prolog oracle.

## Files changed

- `docs/high-level-design.md` — adds canonical overlay ownership to the target-user promise.
- `docs/intent/speaker-identity/canonical-speaker-identity-design.md` — new LLD, projection boundary, edge audit, decisions, and explicit export divergence.
- `docs/intent/speaker-identity/canonical-speaker-identity-specs.md` — enforced EARS node.
- `docs/intent/registry.edn` — appends active `SPK-IDENT-001` with misreadings, boundaries, rationale, pins, and named tests.
- `src/cfp_scheduler_killer/speaker_identity.clj` — adds `INTENT` and `@spec` implementation links.
- `test/cfp_scheduler_killer/speaker_identity_test.clj` — adds the source-owner and behavioral witnesses with `INTENT-TEST` and `@spec` links.

Pre-existing `.codex/config.toml` deletion and
`.codex/config.toml.disabled-by-sol-yolo` untracked file were not touched or staged.

## Intent registry row

```clojure
{:id :SPK-IDENT-001
 :status :active
 :ears "While a submitted speaker may have a current person record, when a non-export source projects that speaker's current name and email, the application shall use the sole cfp-scheduler-killer.speaker-identity/current-speaker-identity implementation; exports.clj alone may retain its documented presence-aware profile projection."
 :misreadings ["A byte-identical private helper is harmless in a new consumer because its behavior matches today."
               "The export projection is redundant and may be replaced without preserving the difference between an absent profile key and a deliberately blank one."
               "A missing or blank current-person value should erase the submitted name or email instead of preserving the snapshot."]
 :boundaries ["The shared helper overlays only nonblank current person name and email."
              "When no current person resolves, the entire submitted speaker map is unchanged."
              "exports.clj is the only permitted second definition because it additionally projects maintained profile fields by key presence."]
 :tests [:current-speaker-identity-has-one-canonical-owner-test
         :current-speaker-identity-overlays-current-person-or-preserves-snapshot-test]
 :rationale "The 2026-09-07 consolidation removed byte-identical helpers from portal, inform, and handlers/files. Exact source ownership plus an independent behavior witness prevents the copies from silently returning while preserving exports' intentional presence-aware semantics."
 :pins "ce49083e speaker-identity consolidation — one canonical non-export implementation with exports.clj as the sole documented divergence"}
```

## Enforced EARS specification

```markdown
- [x] **SPK-IDENT-001**: While a submitted speaker may have a current person record, when a non-export source projects that speaker's current name and email, the application shall use the sole `cfp-scheduler-killer.speaker-identity/current-speaker-identity` implementation; `exports.clj` alone may retain its documented presence-aware profile projection.
```

## Planted-duplicate RED

I temporarily planted a private `current-speaker-identity` definition in
`src/cfp_scheduler_killer/portal.clj`, ran only the source-owner witness, and then
removed the plant. The plant is absent from the final diff.

Command:

```text
bin/kaocha unit --focus cfp-scheduler-killer.speaker-identity-test/current-speaker-identity-has-one-canonical-owner-test
```

Output tail:

```text
[(F)]
Randomized with --seed 1274575796

FAIL in cfp-scheduler-killer.speaker-identity-test/current-speaker-identity-has-one-canonical-owner-test (speaker_identity_test.clj:25)
speaker-identity is the sole canonical name/email overlay; exports.clj is the only permitted second definition because its profile projection distinguishes absent keys from deliberate blanks
Expected:
  #{"src/cfp_scheduler_killer/exports.clj" "src/cfp_scheduler_killer/speaker_identity.clj"}
Actual:
  #{"src/cfp_scheduler_killer/exports.clj"
    "src/cfp_scheduler_killer/speaker_identity.clj"
    +"src/cfp_scheduler_killer/portal.clj"}
1 tests, 1 assertions, 1 failures.
```

## Clean focused GREEN

Command:

```text
bin/kaocha unit --focus cfp-scheduler-killer.speaker-identity-test
```

Output tail:

```text
[(...)]
2 tests, 3 assertions, 0 failures.
```

The three assertions prove exact source ownership, current name/email overlay with
unrelated snapshot keys preserved, and byte-for-byte map preservation when the person
is absent.

## Requested gates

`bin/kaocha unit` tail:

```text
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  Async? false  Throw? false
WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
1023 tests, 12415 assertions, 0 failures.
```

The expected baseline was approximately 1,021 tests; the two new witnesses account
for the observed 1,023.

`~/bin/clj-kondo --lint src/cfp_scheduler_killer/speaker_identity.clj test/cfp_scheduler_killer/speaker_identity_test.clj` tail:

```text
linting took 15ms, errors: 0, warnings: 0
```

`git diff --check` also exited 0 with no output.

Raw captured output:

- `/var/tmp/forge/lid-fx/scanner-red.txt`
- `/var/tmp/forge/lid-fx/scanner-green.txt`
- `/var/tmp/forge/lid-fx/kaocha-unit.txt`
- `/var/tmp/forge/lid-fx/clj-kondo.txt`

## Doubts and follow-up judgment

- `exports.clj` could probably adopt the shared helper for its canonical name/email
  binding and then apply only its presence-aware profile-field reducer. That would be
  a stronger end state: one name/email implementation overall, with a differently
  named export-specific projection for deliberate blank-vs-absent semantics. I did
  not make that change because this order explicitly preserves exports as the sole
  permitted second implementation. If pursued, it should be a separate refactor with
  the export clearing tests held green and an intentional update to this exact-owner
  ratchet and its design decision.
- The EARS outcome intentionally pins an architectural ownership property rather than
  only an external UI outcome. That is justified here because byte-identical copies
  can preserve every behavioral test today while recreating the precise future-drift
  hazard Gene asked to ratchet.
- LID proves this promise is linked and witnessed; it does not prove the chosen export
  exception is permanently the best design.
- Beads task creation was attempted before edits, but this checkout's Beads database
  refused writes because its `issue_prefix` configuration is missing. No Beads state
  was mutated or initialized.
