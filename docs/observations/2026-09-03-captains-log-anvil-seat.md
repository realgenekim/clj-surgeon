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

## 02:30Z — rf2 NO-GO (caller writes escape the root via a directory symlink, witnessed; shell-string `:command` in receipts executes); q5z GO-WITH-FIX (confinement correct; 14 .cpcache files committed; no caps; symlink cycle). Verdicts filed (docs/observations/2026-09-03-rf2-q5z-redteam.md); fix rounds launched. Of the three 'ready' branches red-teamed tonight, none was mergeable as it stood.

## 02:38Z — the three receipt ratchets landed on `bridge/receipt-ratchets` (0434aae, 29090e9, ece8c1c)

From the friction ledger, each with a red-first witness and the field case reproduced verbatim:
1. The outline emits `dispatch` with the exact source spelling for every `defmethod`; the owner refusal on
   a multimethod says "117 defmethod arms share the name fold-event", shows the exact owner form to send
   (`{kind: "defmethod", name: "fold-event", dispatch: "\"event.3\""}`) and a bounded dispatch
   vocabulary (40/117, truncated). A caller who reads the outline first never pays the refusal.
2. `apply_clojure_changes` accepts an OPTIONAL `expect_matched {file match file_hash count}` copied
   from a match receipt and returns `matched_count`, `addressed_matches`, `unaddressed_matches`
   [{line hash}] (bounded 20), with a visible "⚠ 3 of 19 matched sites not addressed (pre-image lines
   118, 125, 132)"; stale basis → `expect-matched-stale` with a `mismatch` discriminator, bytes unchanged.
   No server-side state (the branch's `basis` lease was rejected for exactly that reason); "addressed" is
   the intersection of pre-image spans against the transaction's own frozen pre-image. The builder also
   found and fixed a double basis computation (~0.6 s on the 1235-line field file).
3. `missing-fields` names the field and prints the minimal valid shape; `invalid-require-policy` names the
   field and its two values (deliberately NOT defaulted: the field is `:required` in the published schema,
   and a silent `minimal` would hand a `copy-all` caller a smaller require list without saying so); a
   `match` miss with `_` carries "each `_` matches exactly one subtree; a longer form needs a longer pattern".
EARS MCP-OP-DISPATCH-001..003, MATCHED-001..003, FIELD-001..003. Suites: test-fast 712/5970 (baseline
five), mcp JVM half 387/4020 (baseline one); intent audit ok. Tempted-not-done, reported: letting
`inspect_clojure`'s `forms` accept the defmethod owner map (an arm can be written by an address it cannot
be read by — a contract change, filed as the next ratchet candidate); the same minimal-shape refusal on
apply; near-miss-by-prefix on a match miss; owner names in `unaddressed_matches` (receipt size).
**Host gap, now stated three times:** `make mcp-test` cannot run on Anvil — its first prerequisite shells
`swipl`, absent; the `MCP-OP-ORACLE-001` Prolog gate is unverified on this box. Morning item for the mayor.
My own suites running under the lock; push after.


## 02:51Z — fold-diff GO-WITH-FIX (read-only receipt digests the wrong file; frontier gap unmeasured; baseline vs the whole stack; 7 fixes); settings-lens GO (measured); template-upsert GO-WITH-FIX (id canonicalisation collapses distinct ids — silent loss); lens-followups NO-GO as written (slug-only payload dropped). Verdict filed (docs/observations/2026-09-03-folddiff-lens-redteam.md); three fix rounds launched.

## 02:54Z — `bridge/receipt-ratchets` pushed at ece8c1c (my suites: test-fast 712/5970 baseline five; mcp JVM 387/4020 baseline one). Per the fence-review doctrine it does not enter the queue until an executed review says GO — Opus review a62ad08979fc4afbc running (expect_matched pattern cost and confinement, refuse-before-write on every route, vocabulary size, dispatch spelling edge cases, schema drift, contract). Eight lanes in flight.

## 03:06Z — ratchets review GO-WITH-FIX: line-granular 'addressed' over-claims (wrong direction), vocabulary unbounded in bytes and leaks comments, one refusal unreachable on its route, schema advertises what it refuses. Verdict filed; fix round launched on the ratchets worktree.

## 03:08Z — rf2 fix round landed (f8613ba…5ccb4f0, EXTRACT-024..031); `.cpcache/` ignored on main (ef5a538)

All eight NO-GO items, one commit each, each red against the reviewer's own probe: the directory-symlink
escape now refuses `caller-path-outside-root` with the outside file byte-identical (the builder reproduced
the HIGH finding verbatim first); `:command` is the two argv vectors actually executed, `:command_shell`
a quoted form round-tripped through bash with 11 hostile tokens, the old interpolation shown to create
PWNED; the compile check states `:runs-workspace-code true` and names the opt-out; the config walk is
bounded at the root with `:config-file` in the receipt; attribution by project-relative path (and a
silent pre-existing bug: the old regex excluded `/`, so the common `(clj_surgeon/extract.clj:12:3)` shape
never matched); discovery walks without following links, skips build trees, refuses above a raisable file
cap, skips and NAMES oversized files; `:verified` flags derived; the alias must be one simple symbol — and
hand-driving the other mode found a ninth hole the review did not name: with `:rewire-callers false` the
bad alias went straight into the header; closed and witnessed in both modes. Suites: test-fast 744/6301
(baseline five); mcp JVM 385/4071 (baseline one; the builder's own baseline measurement was contaminated by
an in-flight edit and is confirmed by the final, stated as such). Builder's process defect, self-caught:
`git add -A` swept 142 `.cpcache/` files into the commits; it rewrote its own eight commits with
filter-branch (base 5e6cdd2 intact, merge-base verified, 8 files / 0 cpcache tracked). Two rounds hit
this tonight, so `.cpcache/` is now ignored on main. Tempted-not-done, reported: `:file`/`:to` are not
confined (only caller plans, as specified); the undo receipt's `:command` is still a shell-ish string;
`find-config-file`'s one-arity walk still serves `load-project-aliases`/`outline`; an out-of-root directory
symlink anywhere under the root now refuses EVERY extraction in that repo (fail visibly, as mandated —
worth a second look before a monorepo). My suites running; push after; then an independent re-review.
Ratchets fix round (a90f9ebd74d4100af) launched on the ratchets worktree.

## 03:12Z — `bridge/rf2-extract-rewire` pushed at 5ccb4f0 (my suites: test-fast 744/6301 baseline five; mcp JVM 385/4071 baseline one; 0 errors). Not in the queue yet: Opus re-review a9f6694ffe16d0d1e re-runs every original probe and hunts holes the fixes introduced (the new discovered-path resolver, shell quoting, the raisable cap, the parent-config walk still serving outline/load-project-aliases, the tempted-not-done items).

## 03:17Z — LENS-006 fixed (934716dc on `bridge/lens-followups`): the reminder-schedule arm is back on slug addressing with `:event-id` as a cross-check only; the reviewer's two dropped shapes now write again, and a THIRD latent bug the review did not name is closed — an `:event-id` resolving to a different real event than the payload's `:slug` would have written into the wrong event; now it refuses, the one deliberate change, registered. Three witnesses red-first (5 failures on the buggy arm), a new replay history blessed additively, the arm kept out of the 19-arm table on purpose (its guard is a cross-check, not a key lookup). Builder unit 1055/13171/0; my run in progress; push after.

## 03:18Z — `bridge/lens-followups` pushed at 934716dc (my run 1055 tests, 13171 assertions, 0 failures).

## 03:25Z — Anvil environment: swipl 10 + plunit installed user-locally (micromamba), claude-skills + global CLAUDE.md + the-gene-maven in place, connector deps installed, .mcp.json repointed off 7888; wishlist for the mayor filed (docs/observations/2026-09-03-anvil-seat-wishlist-for-mayor.md) with Gene's restart command.

## 03:26Z — rf2 re-review GO-WITH-FIX: probes closed, but skips can lie about completeness and the cap remedy is broken from the CLI; round three launched (items 1–7). Verdict filed.

## 03:28Z — q5z fix round landed (cc4e38c…0f01440); one bound reverted by ruling

Seven items, each red first against a real probe: 14 tracked `.cpcache/*` removed and a repository-hygiene
gate added to `make test`; the walk no longer follows links (a cycle had recursed forty `loop/` segments
deep and pulled a file from a directory linked out of the root); ceilings `max-scope-files` 2000 and
`max-source-bytes` 2 MiB (the red run slurped a 2 MiB file, COMMITTED, and mutated twelve namespaces);
a symlinked defining file is now a typed refusal (the red run retired the link and left `vendor/store.clj`
as a second live definition); `:ok`/`:committed`/`next_action` and the ✓ summary derive from the kernel's
computed commit (a stubbed non-commit had published a green receipt over an unwritten tree); the
retire-failure rollback deletes the undo receipt; `:retired_to` is project-relative and detail files
prune to 20. Suites: test-fast 734/6257 (baseline five); mcp JVM 410/4598 (baseline one).
**Ruling:** the round's third bound — a pre-read refusal when `expect.files` exceeds the files in scope —
removed the verb's self-correcting field behaviour (the anchor: declared 176, found 171, refusal carried
`found_files` + an executable `next_call`, the agent re-sent it verbatim, one return). Dropped; the two
ceilings stay; the over-declared tests are restored as witnesses of the idiom. Builder resumed for that
one commit; my suites and a re-review after.


## 03:31Z — Surgeon server for this seat: 127.0.0.1:7906, `:telemetry :full`, started from clj-surgeon main (Gene: "Do you have watcher loop running to time your every surgeon operation" — answer: no; the watcher is per hand-drive session; zero Surgeon calls from this seat since the move because the session MCP config was bound to 7888 at start and must never be used here). Standing meter from now: server-side telemetry on 7906 for every call (builders included when pointed at it), `make study-agent-usage` for the readout; the watcher stays reserved for hand-drives.

## 03:32Z — the Prolog gate runs on Anvil: `make mcp-operation-oracle` on main → "mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]", EXIT 0 (MCP-OP-ORACLE-001), on the user-local swipl 10. The swipl gap is closed; `make mcp-test` is whole on this box from here.

## 03:35Z — q5z round complete (23ea871, nine commits on 2753f23): the ruling landed as an inversion of MCP-OP-ALIAS-040 (the over-declared expect.files MUST reach discovery and return found_files + an executable next_call; 8 red assertions on the anchor shape, green), ceilings untouched, `plan!` docstring says expect.files is deliberately not a read bound. My suites running incl. the full `make mcp-test` (oracle on). Port decision (Gene: kill 7888 or use another port?): use another port — 7888 on Anvil is the fleet's, named in the curtain-call seats' repo configs and the acid arms; forge runs 7906+ (7906 up with telemetry); one session restart binds this seat's MCP tools to it. Asked the mayor, via the wishlist, for what 7888 runs and who depends on it, for the port table.

## 03:38Z — the mayor's port table and provisioning status (/tmp/ports.txt), verified on the box

7888 on Anvil: java, user surgeon, checkout `/srv/fleet/shared-tools/clj-surgeon-e7f72e2` (sha e7f72e2,
2026-08-25 — eight days and many commits behind main), bound 127.0.0.1, telemetry under surgeon's state;
dev-a/b/c each carry two configs naming it and all three have live agents (3, 6, 6). Not an orphan; the
no-kill call was right and is now measured. Listener map: 7889 cohort, 7890 cclsp, 7891/7892 bisect,
7893 shipped-build, 7894/7895 cohort, **7906 mine (up, telemetry), 7907–7910 reserved for my branch
servers and hand-drives**. Ruling: bind only inside that range; ask before anything outside it. Written
into kiloclaw docs/provisioning-a-constellation-seat.md. Retiring 7888 later flips only when dev-a/b/c are
retired or repointed — Gene's fleet decision.
Provisioning: maven creds DONE and proven from here (`maven-r inbox list` → 307 open items; the mayor
shimmed `clj` → `clojure` in `~/bin` because rlwrap is absent); `~/bin` scripts DONE (9 files) with PATH and
XDG_RUNTIME_DIR pinned in .bashrc; check-prompt-plate cron hourly at :41; repo-watchstander HELD (a fresh
bridge-cos on Buster would double the alerts — leave it there); beads: the mayor's call — inbox is the
one store, mirror as beads only on merge; phone channel CLOSED (seat=bridge is special: only that name
reaches Gene's page; Buster keeps it); codex auth still needs Gene interactively (401 confirmed again).
Inbox brought current from this seat: inb-a0f37e, d8a635, 06d376, ace545, 11a6ae, a97614, 3cb0f4,
3a9818 (fold-diff not ready for the production run until its fix round + re-review), c973d2; S9 filed as
inb-0817fe. The session's MCP binding to 7888 gets fixed by one restart from the connector directory when
the lanes are at a safe point; the mayor will not restart me from outside while six agents are live.

## 03:39Z — study-ops security round complete (232af3e…212b045, 14 commits on 8a52931), every item red-first

Item by item, witnessed on the pre-fix bytes: `#=(spit …)` had been written for ALL THREE build-file kinds
(deps.edn, bb.edn, project.clj) — now none; the symlink-out-of-root and the `:paths` decoy both left the
tree (5 → 1 files); the 3000-file tree went from "ok, 3000 outlined, 47 returned, 386 ms" to "refused
`study-tree-too-large`, 0 outlined, 86 ms" (discovery split from outlining; claypoole bounded map on the
JVM entrance only — babashka cannot load claypoole's classes and `make test-fast` runs on bb, so the kernel
takes the mapping strategy as an argument; same shape the census branch reached independently);
`study-oversized` and `ls-tree-refusal` no longer hand back the call just made; `source_character_count`
real (23,811 where it said 0), cycles and the envelope budgeted, single-form deps bounded; the text total
line agrees with the receipt under truncation; `bound-rows` charges the array's own characters (red at
limit 36); `format "EDN"` and `formatt` refuse typed server-side; `run-ls-tree` renamed and golden'd with
an error path; `rg --version` once; `mcp-serve-benchmark` honours MCP_PORT (a no-op assertion in the
heap self-test corrected on the way); a parity witness compares the CLI text against a max-limit MCP
`text` receipt. Specs STUDY-013..019. Suites: test-fast 723/5990 (baseline five), mcp JVM 404/5224
(baseline one), oracle PASS on the branch. Flagged for the re-review: refusal messages still carry a
host-absolute dir (inherited from `no-clojure-files`; STUDY-006 says none); `invalid-grep-pattern`'s
continuation still carries the rejected pattern; the real character count now refuses study requests on
files over 65,536 chars (honest, and reachable here); `forms/find-config-file` still walks to `/` and
SCI-evaluates (fence, untouched). Lock note from the builder: `~/tmp/suite.lock` is contended by ~4 agents;
one lock-wait timeout killed a command mid-swap and left `study.clj` at HEAD for ten minutes before the
builder noticed and restored it — a hazard of file-swap witnesses under contention; later swaps used a
bounded inner timeout with guaranteed restore. My full suites queued; push → re-review → queue.


## 03:42Z — `bridge/q5z-alias-migration` pushed at 23ea871 (my suites: test-fast 734/6257 baseline five; full `make mcp-test` 409/4593 baseline one, oracle PASS). Re-review acb7e66e0b6224894 running (the seven probes re-run, the walk depth bound, the ceilings' refusal shape, retire-before-write, derived :committed, retention race, the hygiene gate on bb, the restored over-declare idiom on both entrances).

## 03:43Z — `bridge/study-ops-mcp` pushed at 212b045 (my suites: test-fast 723/5990 baseline five; full `make mcp-test` 404/5224 baseline one, oracle PASS). Still NOT in the queue: re-review a4bf90d6d86f75204 re-runs all twelve probes plus the four flagged items (host-absolute dir in refusals, the grep continuation, the 65,536-char study limit, the claypoole/bb split) before the NO-GO is lifted.

## 03:50Z — fold-diff fix round landed (27b4adec…a765d859, nine commits on f2d8f6eb); a process defect to carry forward

Seven fixes, each red-first with the reviewer's probe: the write guard asserts every var stubbed and names
them (14 red assertions against the silently-dropping guard); a log past its checkpoint is refused with the
gap named, `FOLD_DIFF_ALLOW_GAP=1` compares-but-labels UNEXAMINED with a truthful exit; both sides are refs
in throwaway worktrees (`BASELINE_REF` required, `CANDIDATE_REF` default HEAD, `DEPLOYED_REVISION` printed
beside the baseline with a WARNING on mismatch); `--expect-baseline-sha` refuses a stale or mislabelled
baseline; heap (`FOLD_DIFF_XMX` 4g, parse-once, prune-first, streamed writer) with a byte-identical
characterization pin that bit on a single trailing newline; exit codes 0/1/2/3 measured as a subprocess
(`FOLD_DIFF_MAX_PATHS=many` was a JVM exit 1 reading as "differences"; now `FAILED :bad-env` exit 3);
userinfo JDBC URLs redacted (`cfpuser:hunter2@` had survived). Usage: `BASELINE_REF=origin/main
STORE_BACKEND=postgres make fold-diff-checkpoint`. Unit 1052/13017/0 → 1076/13210/0. Unverified without
Postgres, stated: the new `SELECT COALESCE(MAX(seq),0)` has never executed; the pool opener is read from
code; the write guard was exercised only under JSONL; the secrets symlink into the candidate worktree never
met a real `secrets/`; 4g is a judgement.
**Process defect (the builder's own report):** it killed a `flock` waiter (`kill 3724641`) before confirming
ownership; the signature matched its own orphan and other seats' jobs kept running, but ownership was
checked only for the second kill. Rule for every builder prompt from now on: never signal a process you did
not start without proving ownership by pid, ppid and cmdline first; on this shared box a wrong kill is
another seat's outage. My unit run + compile-check queued; push → a short re-review of the Postgres-path
delta → then the mayor's production run (inb-3a9818 updated with the new command at push time).

