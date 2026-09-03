# Session 4 comparison receipt — settings-lens migration, Surgeon transaction vs native exact patch (2026-09-02 23:05Z)

Both plans start from byte-identical trees (`bridge/settings-lens` 55d1fd3f, folds.clj sha256 5afe41ac…),
same spec (the 19 guards / 23 paths list from LENS-001, the four named traps), neither applied to the
lens branch. Surgeon plan validated on a detached scratch worktree; native plan validated by
`git apply --check` and a babashka identity check, not by the gate. Per Sol: this receipt NOMINATES,
it does not select; Gene merges.

| axis | Surgeon (`apply_clojure_changes`, one transaction) | native (one unified patch) |
|---|---|---|
| plan wall (stopwatch) | 299 s session, first call → marker (watcher); ~7.1 s inside the tool | 465 s planning (agent's own `date -u`: 22:56:04 → 23:03:49) |
| returns / decisions | 11 counted returns (4 Surgeon + 7 shell) | 11 decisions listed; returns unmeasured (agent transcript) |
| arms migrated | 16 of 19 | 18 of 19 |
| hunks | 7 | 7 |
| lines −/+ | 83 / 58 | 125 / 82 |
| lines touched outside guard+path | **0** (residue = the sixteen `state))` closers) | **149**: 86 whitespace-only (reindent), 20 paren-only, 32 replacement call lines, 11 comment/`#_` lines |
| comments inside migrated forms | preserved verbatim inside the replacement | 11 comment lines moved/reflowed |
| evaluation order changed | no (form-for-form) | yes in `event.speaker-unannounced`: `target` hoisted above the guard, so `announced-speaker-identity` now runs for unknown events (agent's own top risk) |
| conditional no-op arms | held (2) pending LENS-003: an absent `:settings` would otherwise materialise as nil | migrated by restructuring; adopted keeps the no-op by construction, unannounced by hoisting |
| `export.generated` | held: writes `:exports`, lens is settings-scoped | same decision, same reason |
| tripwire after | 19 → 3 guards, 24 → 6 paths (before LENS-003) | 19 → 1, 24 → 3 |
| gate | **green on the scratch**: whole-projection replay equality, 19-arm oracle, three edge cases, LENS-002; only the tripwire numbers fail (deliberate) | not run (dry plan); `git apply --check` clean; babashka `pr-str` identity over six rewrite shapes × three settings states |
| stale-tree detection | each `find` must match exactly once inside its named arm, else the whole transaction refuses | patch context lines |
| new finding | tool receipt does not say why 16 of 19 matched arms were addressed (watcher) | **a 24th settings write**, `speaker.reminder-schedule-configured`, slug-keyed guard, counted by neither tripwire regex |

## Reading

- On the mechanical 16, Surgeon is strictly cleaner: zero reindent churn, comments preserved, verified by
  the gate, and the receipt proves each site matched exactly once. Native's 86 whitespace lines are the
  reprint cost, on the native side this time.
- Native's two extra arms come from restructuring, which is judgment work Surgeon was not asked to do and
  which changes when code runs; the honest route for those two is LENS-003 (identity no-op in the lens),
  then a second form-for-form transaction. That keeps "guard passes ⇒ settings written" out of the lens.
- Native's real contribution is analytical: the 24th write, and the tripwire's `(= arm-count guards)`
  assertion that a number change cannot fix. Both go into the migration commit's test edit.
- Sol's unconvincing-if list: same base ✔; same spec ✔; prompts differ (driver vs agent) — noted; warm-up:
  Surgeon's server was warm, the native agent was cold — noted, favours Surgeon on wall; Surgeon is
  form-scoped, not owner-reprinted ✔; native was free to write one patch ✔; churn and tree equivalence
  measured ✔; no retries on either side; n=1 — this is a specimen, not a general claim.

## Nomination

Production route for the migration commit on `bridge/settings-lens`: the Surgeon 16-change transaction
(re-run on the real worktree after LENS-003 lands), then a second transaction for the two conditional
arms once LENS-003 makes the no-op safe, `export.generated` untouched, the tripwire edited by hand
(numbers + the arm-count assertion + a row for the 24th write), gate = whole-projection replay
equality + `bin/kaocha unit` 0 failures. Native's patch stays on `bridge/settings-lens-native` as the
comparison specimen. Gene decides.

Artifacts: `~/src/curtaincall-cfp-lens/.plan/surgeon-settings-lens.patch`,
`~/src/curtaincall-cfp-lens-native/.plan/native-settings-lens.patch`,
`docs/observations/2026-09-02-session-4-native-plan.md`,
`docs/observations/2026-09-02-tweezer-session-4-watch.md`.
