# Anvil builder seat — onboarding brief (pre-staged 2026-09-02T23:37Z; Gene floated "get a claude code session going on anvil")

**Role.** A second seat, not a move: `forge-anvil` builds; `forge-bridge` (this seat) plans, verifies,
routes, and holds the phone channel, the memory, the inbox creds, the tweezer server (7888 on Buster),
and the resume-note pointer. Work flows bridge → anvil as inbox items with exact specs and verify
commands; anvil pushes branches; bridge re-runs the verify command independently before anything is
called done. Nothing on anvil merges.

**Seat (Gene, 2026-09-02: "Use anvil seats that exist already.").** The builder seat is `tester@anvil` — no new user. It already holds the acid clones, the bridge's scripts and a Claude config. Consequence: the battery hazard is ours to manage, so builders work in worktrees under `~/build/`, never `~/acid/`, and never start while a `~/acid/GO-*` file or the cohort lock exists. The fleet seats dev-a/b/c, foreman, merger, kentbeck stay untouched; `surgeon` runs the production Surgeon.

**Identity.** Export in the session, never `git config`:
`GIT_AUTHOR_NAME=forge-anvil GIT_AUTHOR_EMAIL=forge-anvil@anvil GIT_COMMITTER_NAME=forge-anvil
GIT_COMMITTER_EMAIL=forge-anvil@anvil`; every commit carries
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`. Verify after the first commit with
`git log -1 --format='%an <%ae>'`.

**Access.** A scoped GitHub token (repo scope, branches only; the same shape as the bridge's
`marvin-openclaw777`) delivered by the skiff into `~/secrets/` (700/600), never an env var; `gh`
via `hosts.yml` as on the bridge. Anvil already fetches origin read-only for clj-surgeon and
curtain-call under `~/acid/`.

**Boundaries carried from day one.** Never contact port 7888 on Anvil (another seat's production
Surgeon) or 7894/7895 (the cohort servers) — start your own on a free port. Never touch
`~/acid/GO-*`, `~/acid/.cohort-lock`, `chain-*.sh`, or the curtain-call fleet directories. Nothing
touches the SCI allowlist, the evaluation fence, or path confinement without adversarial review
before merge. Never git stash in a shared repo. One agent per disjoint file set.

**First action after compaction.** Read `docs/observations/2026-09-02-resume-here-bridge-program.md`
(this repo) and the seat's own `docs/observations/<date>-resume-here-anvil-seat.md`; the pointer
line goes at the top of the repo's CLAUDE.md the day the seat exists.

**Worktrees.** `~/src/clj-surgeon-<branch>` per branch from a fetched `origin/main` sha (record it);
curtain-call under `~/src/curtaincall-cfp-<branch>`; suites via `make test-fast` / `make mcp-test`
(clj-surgeon) and `flock ~/tmp/suite.lock bin/kaocha unit` (curtain-call); 16 cores means two
suites may run concurrently, but the cohort lock still serialises against batteries.

**First two builds to hand over (specs live in the bridge captain's log of 2026-09-02):**
1. The Surgeon receipt ratchets from the friction ledger: outline emits `dispatch` for defmethods and
   the owner refusal names the defmethod owner form; `unaddressed_matches` in the transaction receipt;
   `missing-fields` and `invalid-require-policy` refusals name the field. Branch `anvil/receipt-ratchets`.
2. The curtain-call follow-ups from LENS-004: migrate `event.program-speaker-updated` onto the lens
   under its own predicate; bring `speaker.reminder-schedule-configured` under an event-id-keyed
   guard or document why not. Branch `anvil/lens-followups` stacked on `bridge/settings-lens`.

**Reporting.** Each build ends with: sha, the verify command's exact output, fails-first evidence,
anything it was tempted to widen. The bridge logs it; the mayor queues the merge.
