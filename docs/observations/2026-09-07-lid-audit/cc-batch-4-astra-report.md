# Curtain Call LID batch 4 — G2 email delivery truth

Worktree: `/home/forge/src/cc-lid5`; branch `astra/lid-batch-4`.
Base: `ae6ea265` (batch 3). Scope: ledger G2 delivery truth only.
Commit: `f474df961aeaea66eaecaa1f884a4cf7827b24fb`. No push.

## Outcome and stopped behavior question

Current behavior is pinned, not changed. `:mode :sent`, `email.sent`, outbox
`:state :sent`, and history `:sent? true` do **not** alone prove a provider
handoff. Dev logging returns all of those without contacting a provider.
The distinctive fact payload is `:via "dev-log"` with message ID
`dev-log/<email-id>`. SES acceptance records `:via "ses"` and its message ID;
SMTP acceptance records the provider message ID without a `:via` field.
Acceptance means provider handoff, never inbox arrival.

**Behavior question left open per instruction:** should dev logging return
`:mode :logged` and append `email.logged`, and should missing providers on
Cloud Run return a named failure? No implementation of either change landed.
Callers depend on the existing vocabulary:

- `handlers.communications/outbox-command` recognizes sent/failed for redirects.
- Compose requires every delivery result to be sent; recipient counts use sent state.
- `mail/email-types`, `outbox`, and `history` depend on the current event vocabulary.
- Communications renders an “Email sent” toast from the sent result.
- `folds/email.sent` advances speaker-chase cadence without checking `:via`.

The existing deployment gate is `mail/provider-delivery-enabled?`, based on
`K_SERVICE` presence. There is **no production missing-provider refusal**.
With that gate true, SMTP absent, and SES disabled, dispatch still logs sent.
A named compatibility witness exercises this boundary, and a separate diagnostic
expecting failed went red against the unmodified dispatcher. No new ENV/config
mode was invented; no false passing production-refusal claim was registered.

## Arrow, rows, and rung

HLD delivery evidence section → new delivery design with decision table and edge
boundaries → `delivery-specs.md` plus eight append-only registry rows → provider
witnesses and fault plants → source linkage/docstring corrections.

| Active ID | One observable outcome |
| --- | --- |
| MAIL-DELIVERY-001 | Accepted SMTP/SES handoff returns sent mode |
| MAIL-DELIVERY-002 | Accepted handoff appends email.sent with provider message ID |
| MAIL-DELIVERY-003 | Provider-reported rejection returns failed mode |
| MAIL-DELIVERY-004 | Provider-reported rejection appends email.failed with its error |
| MAIL-DELIVERY-005 | Development log preserves current sent mode compatibility |
| MAIL-DELIVERY-006 | Development log identifies the fact transport as dev-log |
| MAIL-DELIVERY-007 | SES request preserves original calendar attachment bytes |
| MAIL-DELIVERY-008 | Delivery evidence helper refuses absent recording-fixture receipts |

Every row has misreadings, boundaries, named test witnesses, and source/test
markers. Rows 005/006 characterize compatibility debt, not a desired truth fix.
Row 008 governs the test evidence helper; its source tag identifies the dispatch
boundary under test. It is not a runtime delivery refusal.

Highest applicable product witness: **rung c**, exhaustive SMTP/SES ×
accepted/rejected × plain/calendar (8 cases), plus rung-b caller and compatibility
witnesses. Rung-e product refusal/vocabulary separation is deferred by the behavior
stop. The test helper itself uses named refusals. No new Prolog model was retained:
there was no additional relational counterexample beyond the native matrix.

## Three converted delivery witnesses

1. `mail-projection-test/send-now-records-a-synchronous-success-test`: previously
   forced development mode; now requires the SMTP sink to receive the exact letter
   before asserting synchronous sent mode and provider-correlated event.
2. `comms-test/magic-link-email-test`: previously forced development mode; now
   captures the addressed sign-in letter at the SMTP adapter and requires its
   receipt in the event chain.
3. `comms-test/outbox-approve-and-discard-route-test`: previously expected dev-log
   on approval; now the actual approval route must hand exactly the selected
   queued letter to the SMTP sink before its sent redirect/toast/state passes.

The shared fixture replaces `smtp/send!`, retaining the real email port dispatch,
and replaces `aws/invoke` plus the SES client delay, retaining real SES request
assembly. Neither uses sockets or credentials. The matrix checks recipient,
subject, body, reply-to, attachment bytes/filename, per-email event chain,
mode, failure error, and message-ID correlation.

`delivery-assertion-requires-provider-fixture-test` proves a handoff assertion
fails with `:provider-fixture-required` outside a fixture and
`:provider-handoff-required` when the fixture received nothing. This guard is used
by the three converted tests and matrix; it is not a source-wide ban on remaining
legacy mail tests.

## Red/green evidence

Initial focused preflight: 27 tests, 434 assertions, zero failures.
Then each plant changed the real source (or the diagnostic expectation), ran the
named tests, and restored original bytes in `finally`. All plants preceded final
source linkage and full green acceptance; no dispatcher behavior fix was needed.

| Plant / diagnostic | Observed red |
| --- | --- |
| Force all mail through dev-log | All three converted tests plus matrix rejected missing provider handoff: 4 tests, 26 assertions, 4 errors and 1 failure, exit 5 |
| Strip SES calendar bytes | 1 matrix test, 94 assertions, 2 failures, exit 2 |
| Change dev-log mode to logged | 2 compatibility tests, 20 assertions, 2 failures, exit 2 |
| Expect production missing-provider refusal | 1 boundary test, 10 assertions, 1 failure, exit 1; expected failed, actual sent |

Plant definitions and full receipts:
`cc-b4-plants.py`, `cc-b4-red-results.json`, and `cc-b4-red-*.log` beside this report.
The last diagnostic is evidence of missing behavior, not a mutation of production
logic. Its failed expectation was not retained as an always-red test.

The first Makefile pass found our two helper-only deftests violated the repository's
“every deftest contains an assertion” rule (618 tests, 8,579 assertions, one failure).
Their meaningful mode assertions were moved into the named deftests; the gate was
not relaxed. Receipt: `cc-b4-make-initial-architecture-red.log`.

Final verification:

| Required entry | Result | Receipt |
| --- | --- | --- |
| `make runtests-once` | Exit 0; all 5 Prolog files, 18/18 JS tests, local worktree fixture, 1,036 tests / 13,320 assertions / 0 failures | `cc-b4-make-runtests-once.log` |
| `bin/kaocha unit` | Exit 0; 1,036 tests / 13,320 assertions / 0 failures | `cc-b4-kaocha-unit.log` |
| `~/bin/clj-kondo --lint src test --config '{:output {:format :edn}}'` | Exit 3 before and after; identical baseline diagnostics | `cc-b4-kondo-before.edn`, `cc-b4-kondo-after.edn`, `cc-b4-kondo-comparison.edn` |
| `git diff --check` | Clean | Verified before staging |

Kondo scanned 385 files before and 387 after. Both have 3 errors, 102 warnings,
and 29 informational findings. Comparing filename/type/level/message frequencies,
ignoring shifted source positions, produced zero additions and zero removals.
Existing errors: `handlers/bp.clj:661` invalid `push-everything!` arity;
`reviews.clj:328` static field called as a function; `rubric_predicates.clj:70`
unresolved `=>`. None belongs to this batch.

Full entries inherited `TMPDIR`, `TMP`, `TEMP`, `-Djava.io.tmpdir`, and
`-Dcfp.upload-root` under `/var/tmp/forge/lid-batch/`. PATH resolves kondo through
`/home/forge/bin/clj-kondo`. All command output was captured under the same root.

## Files

- `docs/high-level-design.md`: delivery evidence and production boundary.
- `docs/intent/delivery/delivery-design.md`: current contract, decisions, edge audit.
- `docs/intent/delivery/delivery-specs.md`: eight atomic EARS specifications.
- `docs/intent/registry.edn`: eight appended active rows.
- `src/cfp_scheduler_killer/mail.clj`: intent markers and accurate outcome docstring.
- `src/cfp_scheduler_killer/ses.clj`: attachment marker and accurate transport docs.
- `test/cfp_scheduler_killer/mail_fixture.clj`: recording providers and receipt guard.
- `test/cfp_scheduler_killer/mail_delivery_test.clj`: matrix, compatibility, fixture guard.
- `test/cfp_scheduler_killer/mail_projection_test.clj`: converted synchronous success.
- `test/cfp_scheduler_killer/comms_test.clj`: converted magic-link and approval-route tests.

`cc-b4-source-equivalence.clj` reads old/current forms, removes comments/docstrings,
normalizes reader gensyms, and compares printed forms (regex reader objects do not
have value equality). Its passing receipt proves the two source files retain their
executable forms. No event reader, writer, result mode, or provider branch changed.

## Doubts and limits

- The false sent result and absent Cloud Run refusal remain. Fixing these requires
  an explicit caller/event migration decision; legacy readers must remain.
- These are recording provider fixtures, not SMTP wire, live SES, or inbox tests.
- Existing mail tests outside the selected three can still pass against dev-log;
  this batch does not relabel them as delivery evidence.
- SDK throws, ambiguous timeout/retry outcomes, duplicate delivery, malformed
  success responses without message IDs, and inbox receipt are not covered by the
  matrix. SES invocation exceptions can propagate despite its former docstring;
  only the documentation was corrected.
- Identity/idempotence/unresolved-template rules and the broader replay firewall
  were not widened into this delivery-truth batch.
- The named `linked-intent-dev` skill is absent. As in the prior batches, the
  installed `/home/forge/.claude/skills/linked-intent-testing/SKILL.md` was read and
  used with the user's continuation instruction. No product migration was inferred.
- `bd prime` ran, but `bd create` failed because this worktree database lacks
  `issue_prefix`. No task could be created/claimed/closed; no database reinitialization
  or external tracker mutation was attempted.
- `.codex/config.toml` was already deleted and
  `.codex/config.toml.disabled-by-sol-yolo` already untracked at entry. Both remain
  untouched and excluded from staging. No project push or deployment occurred.
- Work stayed in this source worktree and permitted temp root. No subagents, live
  server, forbidden ports, acid, chain scripts, cohort locks, GO files, or fleet/runtime
  directories were used. The Makefile worktree test uses disposable local Git
  fixtures under the allowed temp root; its fixture-only pushes are not project pushes.

## Commit receipt

Committed the ten listed files with environment author/committer
`forge-anvil <forge-anvil@anvil>`. Both requested trailers are present:

```text
Co-Authored-By: Gene Kim <genek@itrevolution.com>
Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
```

Git status was checked before staging and immediately before commit; the staged
set was verified against an explicit ten-file allowlist. `.codex/config.toml` was
never staged. Post-commit status contains only the pre-existing deleted config
and untracked disabled-config file; the delivery work is committed. No push.
