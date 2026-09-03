# curtain-call: fold-diff tool GO-WITH-FIX; settings-lens GO; template-upsert GO-WITH-FIX; lens-followups NO-GO as written (2026-09-03T02:51Z)

Opus, executed (differential probe: three trees exported with `git archive`, 16 hand-built histories folded
through each tree's real `folds/fold-event`; results measured).

## fold-diff (f2d8f6eb) — GO-WITH-FIX before the mayor runs it against production

Clean: no write path (`db/start-pool!` = SELECT 1, never `migrate!`; one parameterised SELECT against the
ds value, never `store-pg/ds` whose lazy `start!` would CREATE the unique index on production); no defonce
I/O; no log file on this load graph; the baseline worktree runs with `STORE_BACKEND` unset and never opens
a pool; deps.edn has no root `:main-opts`. Re-check git hooks on the skiff (`post-checkout` fires on
`git worktree add`).

| # | finding | fix |
|---|---|---|
| 1 | the "read-only verified" line digests `checkpoint-path` and `@store/store-path` = `data/store/events.jsonl` — NOT the Postgres log; both absent on the skiff → equal for free. `call-with-writes-forbidden` silently drops vars that fail `requiring-resolve`, so if `store-pg` fails to load, `store-pg/append-line!` (the only var that reaches production) is never stubbed and the receipt still says verified | assert every write var installed (count), print the list, reword the receipt to name the files digested |
| 2 | only `seq <= frontier` is folded; events after `make download-cache` are never compared, yet IDENTICAL is read as "changes code, not data" | `SELECT COALESCE(MAX(seq),0)`; print `checkpoint N · live max M · gap`; refuse or loud-warn on a nonzero gap |
| 3 | `BASELINE_REF=origin/main` "the code production is running" is asserted, not verified; and the tool sits on the stacked branch (96387535 is 14 commits ahead of main), so the diff is dominated by the store stack's `:idempotency-keys` / `:announced-speaker-removals` deltas, burying the fold change | make both sides REFS (baseline and candidate each in a throwaway worktree), no default; print the deployed revision the mayor pastes (Cloud Run) beside the baseline; then it can compare origin/main vs bridge/fold-idempotence directly |
| 4 | a stale `cache/fold-diff-baseline.edn` from another ref over the same checkpoint compares silently (only `:checkpoint-sha256` is checked) | `--expect-baseline-sha` from bin; refuse on `:fold-source-digest`/`:label` mismatch |
| 5 | heap: no `-J-Xmx`; checkpoint parsed twice; `(prune-empty (normalize folded))` deep-copies `:log` then discards it; `pr-str` of the whole baseline as one String | set `-J-Xmx`; swap to `(normalize (prune-empty …))`; parse once |
| 6 | `-main` has no try/catch: missing db.edn, connection failure, OOM, the read-only violation all exit 1 = "differences found" | catch at `-main`, exit 3 on failure |
| 7 | `db.clj:206-207` redacts only `password=`/`user=` query params; a userinfo JDBC URL is logged verbatim on stdout, in the receipt the mayor will paste | strip `//[^@/]*@`; check `secrets/db.edn`'s URL shape before running |

## settings-lens aebb7b9a — GO
Arm by arm identical to baseline including the ragged `{:webhooks nil}`/`{:api-keys nil}` cases and the
two conditional arms. The `identical?` early-out fires in cases the old code lacked (e.g. `replay.marked`
twice) — a state-object identity difference only, unobservable (fold-one conjes `:log` after; no
`identical?` consumer, no watch). Coverage caveat: only 4 of 18 arms occur in the judge-sandbox log
(17 facts, 0.52%); the rest are pinned by the synthetic 19-arm table — adequate for a guard collapse,
not for a semantics change. Long-cache nondeterminism note for the docstring.

## template-upsert 619d3192 — GO-WITH-FIX
Disclosed: nil-id collide → append. UNDISCLOSED and the dangerous direction: `comms-template-identity`
uses `blank->nil` = `(some-> x str str/trim)`, so `:id "x "` then `:id "x"` COLLAPSE to one row, and
`:id 5` then `:id "5"` collapse — silent data loss on replay. Fix: compare the raw id when non-blank;
add trailing-space and numeric-id histories to the fixture; grep the production log for
`comms.template-saved` facts with nil/blank/whitespace/numeric ids before merging (the fact type does not
occur in the judge-sandbox log; nothing empirical stands behind it).

## lens-followups 7359b8f9 — NO-GO as written (one-line fix)
LENS-005 clean (three cases identical). LENS-006 changed the guard key from `:slug` to `:event-id`: a
payload with `:slug` only, or a wrong `:event-id` with the right `:slug`, was written before and is now
SILENTLY DROPPED. The only writer has carried `:event-id` since its one commit, so the practical risk is
~0, but the arm is in no golden and neither testing block covers the missing-event-id payload. Fix: keep
the slug guard as a cross-check (write only when `:event-id` resolves to the same slug), add the witness,
add the arm to `settings-arms` so it enters the replay golden — or run the fold-diff tool against
production for exactly this branch.

## Merge order
settings-lens → template-upsert (after fixes) → lens-followups (after fix); the two siblings both edit
folds.clj and have never been folded together — re-run the goldens at each step.
