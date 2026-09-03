# Captain's log — 2026-09-03, the seat on Anvil

## 2026-09-03T01:42Z — moved; oriented; cleared to build

The mayor moved this seat overnight (transcript 24 MB + 60 memory files; project keys rewritten) to
forge@anvil per Gene's order ("pick up session over there and have it just be like talking with you";
checklist inb-20c591). First action: the program resume note, then the Anvil seat note written and
pushed (f93cbbf), Memento pointers in both CLAUDE.md files (fcbb4a0, d170f3d), a seat-move memory.
Facts verified on the box: identity forge-anvil, gh as marvin-openclaw777 (`gh auth setup-git` was
needed once for https pushes), 16 cores, load ~1.1, ports 7888–7895 all held by other seats, `~/acid`
not readable by forge (good: the boundary is enforced by the filesystem, not by discipline). Missing:
the channel connector (no reply tool), maven creds, clj-nrepl-eval, `~/bin`, `/opt/claude-skills`, crons.
Reported as instructed; Gene: "Super!!! Let's go!!"

**First build here:** inb-a0f37e (names-only `ls-tree` rendering + `ns_grep`) and inb-d8a635 (`make
mcp-serve` honours MCP_PORT), re-run from origin `bridge/study-ops-mcp` b3c17bb in
`~/src/clj-surgeon-study` because the Buster builder's work never reached origin. Builder identity
forge-anvil; free port ≥ 7900 for the wire witness; one suite at a time under `~/tmp/suite.lock`.

## 02:01Z — parallelism up (Gene: "You have at least 8 cores. I think you can crank up parallelism. Your on anvil")

Four lanes now, disjoint worktrees, suites serialised behind `~/tmp/suite.lock`:
1. inb-a0f37e + inb-d8a635 (names-only ls-tree, MCP_PORT) — Sonnet on `~/src/clj-surgeon-study`.
2. inb-11a6ae + inb-a97614 + inb-3cb0f4 (the three receipt ratchets) — Opus on
   `~/src/clj-surgeon-ratchets` (`bridge/receipt-ratchets` from main a63171e).
3. inb-ace545 (LENS-005 program-speaker-updated onto the lens; LENS-006 reminder-schedule guard) —
   Sonnet on `~/src/curtaincall-cfp-lens2` (`bridge/lens-followups` from settings-lens aebb7b9a;
   curtain-call cloned here fresh).
4. Opus red-team of the study-ops MCP surface (confinement, subprocess argv, bounds, receipt honesty,
   contract, dep widening, the `format` shadow class, parity) — read-only.
Codex on this seat is unauthenticated (401 on `codex exec`), so Sol is unavailable until the mayor
provisions the login — a morning item; Opus stands in for the red-team tonight. Load at launch 3.9/16.

## 02:02Z — lane 5: inb-06d376 (comms.template-saved characterization + shadow rename + upsert decision) — Sonnet on `~/src/curtaincall-cfp-tmpl` (`bridge/template-upsert` from settings-lens). Five lanes; suites serialised.

## 02:12Z — RED-TEAM NO-GO on the study-ops branch: two code-execution holes reachable from one inspect call

`docs/observations/2026-09-03-study-ops-redteam-NO-GO.md`. Critical: `grep` reaches ripgrep unescaped
(`--pre=/bin/sh` executed files in the red-team's own experiment, receipt said no-clojure-files);
`extract-source-paths` uses `clojure.core/read-string` with read-eval on scanned deps.edn (`#=(spit …)`
executed). High: symlinks out of the root are outlined; the whole tree is parsed before any bound
(618 MB for 1072 files). Eight more items. **The branch is out of the mayor's queue until the security
round lands and an independent re-review says GO** — exactly the class the 2026-09-02 andon memory
covers, caught before merge this time. The names-only builder was told to finish and stop, and to put
`--` before the pattern if it touches `grep-tree`. Mayor: item 2 is a pre-existing CLI exposure on main.

## 02:19Z — lane 5 landed: FOLD-IDEM-004 (bd483802 + 619d3192 on `bridge/template-upsert`)

Fact first, from a blessed history: two nil-id `comms.template-saved` facts COLLIDED under the
hand-rolled loop (`(= nil nil)`), so the second silently replaced the first — "Draft A" lost. The only
writer (`comms_templates.clj:21`) mints an id when none is supplied, so nil-id is unreachable from src.
Decision: migrate to `(upsert-by comms-template-identity % template)` with a TOTAL identity (falls back
to the value, the submission-speaker pattern); the reachable-shapes golden stayed byte-identical before
re-blessing anything; only the nil-id open-case history was re-blessed (both drafts now survive — the
new behaviour, pinned by its own witness). The param shadow the census flagged (`:unresolved-target`)
disappears with the loop. Builder's unit 1054/13155/0; my own run in progress under the lock; push after.
For Gene at merge time: on a historical log with nil-id templates (none known), the projection changes
from collide to append.


## 02:20Z — two more executed red-teams on queued branches; the fence-review rule widened

The study-ops NO-GO proved a read verb can be a write path without touching any fence file. Launched
Opus red-teams on the other "ready" branches with the same classes in scope: census 7244141 (discovery,
subprocess, bounds, `doors` as user input, pool_size 64 on a shared box) and rf2 5e6cdd2 + q5z 2753f23
(the write verbs: the compile subprocess built from the repo's own `.clj-surgeon.edn` aliases, reader
eval, write confinement for rename/retire and target files, atomicity, discovery bounds). CLAUDE.md now
carries "Fence review scope": subprocess argv, reader eval, walk paths, pre-parse bounds, next_call
termination — executed review before any surface enters the queue.

## 02:21Z — `bridge/template-upsert` pushed at 619d3192 (my run 1054/13155/0)

For Gene's merge list, stacked on settings-lens: FOLD-IDEM-004. Inbox item inb-06d376 cannot be updated
from this seat (no maven creds) — the mayor or the morning provisioning closes that loop.


## 02:24Z — lanes landing: names-only ls-tree (8557161) + MCP_PORT (8a52931); lens follow-ups (6da4150f, 7359b8f9); security round launched

**Study branch, S1/S3:** `format: "names"` = exactly {file, ns, form_count, line_count}, default when
`grep` is absent; `ns_grep` filters the scan-relative PATH (the builder's first version matched the
absolute path and `ns_grep "study"` matched every file in a checkout named clj-surgeon-study — caught by
hand-driving the wire, pinned by a regression test); `grep` documented as file CONTENTS via ripgrep.
NO-GO item 1 closed in the same commit: `"--"` before the pattern in both argv branches and a typed
refusal for `^-` patterns before any subprocess (`grep "--pre=/bin/sh"` → `invalid-grep-pattern`, verified
over the wire). Wire receipt on this repo's src: default names call 66 files / 37 returned at 4096 with a
raising next_call; at 16384 66/66, ~7.6 KB, `read_complete true` (the old rendering could not complete
116 files at the ceiling). `make mcp-serve` honours MCP_PORT (self-test extended); `mcp-serve-benchmark`
has the same bug, folded into the security round. Suites: test-fast 718/5967 (baseline five);
`clojure -M:clj-surgeon/mcp-test` 394/4347 (baseline one) — `make mcp-test`'s Prolog oracle cannot run
here (no swipl on Anvil; environment gap, noted for the mayor). Security round (items 2–12, Opus,
aab6a773af84935ee) now running on the same worktree.
**Lens follow-ups (`bridge/lens-followups`):** LENS-005 moved `event.program-speaker-updated` onto the
lens with the person check inside `f` (absent person returns `settings` itself → LENS-003 leaves state
untouched; the `:people` writes stay hand-written under the same predicate; evaluation order preserved);
LENS-006 corrected yesterday's premise — the reminder-schedule writer has carried `:event-id` since its
first commit, slugs are immutable, so the slug-keyed arm migrated too, writer untouched. Tripwire:
settings-path literals 4 → 3, slug-keyed reminder sites 1 → 0, extra update-settings calls 0 → 2, all
red-first. Builder's unit 1055/13163/0; my run in progress; push after. Neither arm is in the
judge-sandbox log, so the replay golden is unchanged by construction — the arms are pinned by their own
histories.

## 02:26Z — `bridge/lens-followups` pushed at 7359b8f9 (my run 1055 tests, 13163 assertions, 0 failures). Stacked on settings-lens; for Gene's merge list after fold → store → lens (inb-ace545 closes when the mayor can update the inbox).

## 02:28Z — census red-team: GO-WITH-FIX, four blocking (schema-only bounds, read-before-filter + uncapped requested files, symlink abort + unpruned walk, CLI crash on its own example). Verdict filed; fix round launched.

## 02:30Z — lane: executed red-team of the fold-diff tool (before the mayor runs it against production) and the three lens/template branches (aceddb474ac916ded). Five background items: census fix round, ratchets, study security round, rf2+q5z red-team, this one.
