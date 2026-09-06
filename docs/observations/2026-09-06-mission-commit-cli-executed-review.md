# GO — exercised public mission commit CLI

Reviewed 2026-09-06 03:56:13 UTC at `b3dbd9e4`, in fresh worktree
`/var/tmp/forge/astra-mission-cli-review-fx`. Reviewer: Astra subordinate
`typist_boundary_audit`. **Non-independent for the Git kernel and ledger adapter,
which I authored; the public CLI/launcher implementation had a separate author.**
This is executed integration evidence, not a replacement independent kernel fence.

One new synthetic owner_forms mission used actual saved planning, actual separate
gate/acceptance subprocesses, and kernel source publication. Only provider transport
was replaced with a frozen candidate. Every row below invoked the real command:
`bin/mission commit M-ID --workspace SCRATCH --state-home SCRATCH/state`.

| Case | Exit | Observed result | Source / ledger / HEAD preserved? |
|---|---:|---|---|
| Mission change unstaged | 1 | `git-staged-scope` | yes / yes / yes |
| Empty configured name/email | 1 | `git-identity-unavailable`, explicit setup advice | yes / yes / yes |
| Malformed configured name `<>` | 1 | `git-identity-unavailable` | yes / yes / yes |
| Unrelated staged file | 1 | `git-staged-scope` | yes / yes / yes |
| Saved mission state changed to failed | 1 | `git-ledger-not-verified` | yes / yes / yes |
| Live source changed after verification | 1 | `git-stale-or-unsupported-files` | yes / yes / yes |
| Real Git ref lock held during publication | 1 | `git-ref-update-uncertain`, possible oid, inspect-before-retry advice | yes / yes / yes |
| Exact staged verified change | 0 | `git-ref-updated true`, exact commit oid | yes / yes / advanced |
| Repeat after success | 1 | `git-staged-scope` | yes / yes / yes |

Success additionally checked exact committed source bytes, the sole changed path
`src/fixture/core.clj`, and the mission provenance trailer. Output disclosed false
hooks/signing/staging/push/source-write flags. The forced ref-lock error honestly
reported unknown rather than claiming the ref definitely stayed unchanged; the
reviewer independently verified that it stayed unchanged in this particular run.
No correctness defect was observed in these nine command executions.

Nonblocking UX findings: staging and staleness refusals lack affected paths and a
next action. Repeating a successful publication is indistinguishable in its error
from an unstaged first attempt; retained Git outcome linkage would clarify this.
Identity preflight checks what Git accepts, not whether the configured human is
the intended author. External writers ignoring the repository lock remain a
documented concurrency limit. No adversarial concurrent-writer stress was run.

Evidence: local `review_cli.clj`, `results.edn`, and `review-events.jsonl` in the
review worktree. Fixture Git repositories were removed in finally. Invocation:
`SLOT_OWNER=astra CLJ_SURGEON_EVENTS_FILE=<review-worktree>/review-events.jsonl
JDK_JAVA_OPTIONS='-Xmx512m -XX:ActiveProcessorCount=2' ~/bin/suite-run clojure
-J-Xmx512m -J-XX:ActiveProcessorCount=2 -M:clj-surgeon/test-deps review_cli.clj`.
Suite exited 0 with `REVIEW-COMPLETE`. No implementation edits, user mission
commits, providers, shared services or shared telemetry writes occurred. Individual
CLI walls were 5.20–6.96 seconds; these are untimed-review observations, not a
comparative performance claim.
