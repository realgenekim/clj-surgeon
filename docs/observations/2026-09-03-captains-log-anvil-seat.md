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


## 03:52Z — `bridge/fold-diff-tool` pushed at a765d859 (my run 1076/13210/0, compile-check green); inb-3a9818 carries the new command marked DO NOT RUN YET; re-review a94e2e37c80843f35 hunts the Postgres path (loaders that could reach ensure-schema!, guard coverage incl. store-pg/append-line!, the secrets symlink, the new MAX(seq) query, DEPLOYED_REVISION semantics, gap wording, heap, hooks, stdout leakage) before the mayor runs it.

## 03:58Z — q5z re-review GO-WITH-FIX: seven closed on their probes; three new blockers in the silent-drop class (aggregate bytes unbounded → OOM under both ceilings; depth bound truncates; unreadable subtree vanishes). Verdict filed; round three launched. The homebrew clj-kondo path test (the standing 'one baseline failure') gets fixed on main separately.

## 03:59Z — q5z round three launched (a4f713cb01afc03cb, items 1–7 of the re-review); the standing Linux baseline failure (hardcoded /opt/homebrew/bin/clj-kondo in mcp_change_buffer_test.clj:686) gets its own fix on `bridge/kondo-path-test` from main (af5a3aed64de01ebb). Eight lanes live.

## 04:02Z — study re-review: NO-GO lifted to GO-WITH-FIX (10× on the motivating workload; twelve items dead); four blockers (outline regression from the budget semantics, quadratic discovery via `:paths [".."]`, ns_grep unguarded, limit-spelling defeats item 6); round three launched.

## 04:03Z — Gene: memory as files grow ("so afraid of huge heaps — but dont want to OOM… have sol figure out a clever way"). Question filed for Sol (docs/observations/2026-09-03-memory-design-question-for-sol.md); codex is unauthenticated on this seat, so an Opus design consult runs now and the mayor is asked to put the same question to Sol on the skiff; the two get reconciled.

## 04:05Z — Sol is live on this seat (mayor's /tmp/sol.txt; re-verified READY from my shell); provisioning closed

Account genekkanban@gmail.com — a different pool from the skiff surgeon seats (genek@itrevolution.net hit
0% spark / 8% general weekly tonight and wedged a seat mid-fix); Gene chose the split so this seat's lanes
cannot starve the seats he talks to. Operational rules, learned the hard way tonight by the mayor:
`</dev/null` always (codex hangs reading stdin); `codex exec` auto-cancels MCP write tools without
`--dangerously-bypass-approvals-and-sandbox` (invalidated a measurement run); OpenAI's content filter
refused twice to review our own path-confinement/symlink fixtures — route that class to Opus and say so.
Memory saved (`sol-live-on-anvil-seat`). First Sol job on this seat: the memory-vs-files design question
(running, high reasoning, output → docs/observations/2026-09-03-memory-design-sol-answer.md); the Opus
consult continues in parallel for reconciliation. Provisioning final: 1 phone channel CLOSED by ruling;
2 codex DONE; 3 maven DONE; 4 bin/nrepl DONE; 5 crons partial (check-prompt-plate here; repo-watchstander
stays on Buster by Gene's ruling — Buster is mission critical, migration is a later decision); 6 ports
DONE; 7 GCP deferred; 8 beads = inbox. Reviews from here route to Sol (high) first, Opus for the
filter-refused class and as the second voice.

## 04:09Z — Gene: "When fixing, add LID (see skill) to add new requirements in"

Rule for the memory work (and every fix round from here): new requirements enter as linked intents per
`~/opt/claude-skills/linked-intent-testing/SKILL.md` (Jess Szmajda's LID as practiced here): a registry
entry with a stable id, an EARS-form intent, `:boundaries` naming what it does NOT promise, witness tests
that FAIL FIRST and are linked by id, and the contract test that makes an evaporated promise loud. For
memory: a new prefix `MCP-OP-MEM-001..` — the aggregate byte ceiling, the walk bound, the depth refusal,
the per-op `heap_used_mb` receipt, retention (hashes + spans resident, source re-read under a hash check),
pool size as a function of a memory budget, and the memory-vs-N battery pass line. The deciding test in the
skill applies: could a maintainer silently narrow it with every test green? A ceiling can be raised by one
constant with no test noticing — so the witness asserts the behaviour AT the ceiling, not the constant.
Builder prompts for the memory rounds carry this clause verbatim.

## 04:09Z — load 12/16 (three forge suite JVMs at 150–400% CPU); memory 18 GB available of 30, heaviest forge JVM 1.6 GB; no action — CPU-bound by design, memory safe.

## 04:10Z — Sol's memory design landed (docs/observations/2026-09-03-memory-design-sol-answer.md; gpt-5.6-sol, high reasoning, 16.5 KB; its sandbox could not run shell — bwrap loopback failure — so it reasoned from the measured facts in the question). Headline: an optimistic, disk-journaled transaction with a byte-budgeted heap — stream discovery → bounded projection/index → stage writes → validate the read set → commit/rollback; nothing realises the repository into one collection; pre-image = a sorted on-disk manifest of {path, bytes, sha256} plus rollback bytes pinned into a transaction CAS before any live write (hashes alone are NOT enough; revalidate the whole semantic read set, not just the write set; the honest contract is optimistic serializability with conflict detection and exact rollback, not snapshot isolation against arbitrary writers); node trees without zippers for read-only ops, zippers only for files being edited and never retained; ceilings as admission contracts (a table of request-visible vs server-hard limits, refusals structured with next_call that narrows scope); pool size derived from measured per-worker cost against a heap work budget with a weighted semaphore by byte bucket; an outline cache keyed by source hash with a second-hit policy; fold-diff as a streaming merge over sorted canonical relation records; a per-op :memory/:work receipt and a 100/1,000/10,000 battery at -Xmx512m with numeric pass lines (peak flat within 32 MiB from 1k to 10k). What it would NOT do: raise -Xmx to 4 GiB, retain sources for rollback, trust hashes without pinned bytes, size parallelism from CPU count, let explicit files bypass limits. Opus consult (with measurements) still running; reconcile → ordered builds, each with MCP-OP-MEM LID intents.

## 04:16Z — sol-yolo bundled with the codex-cli skill: PR https://github.com/realgenekim/claude-skills/pull/1 (via a fork under marvin-openclaw777; the seat has no direct write). Gene: "Sol not being able to shell out seems unacceptable" — cause: `codex exec --sandbox read-only` cannot run a shell on Anvil (bwrap loopback failure); fix per the skill's YOLO recipe: a fresh scratch clone is the fence, `--dangerously-bypass-approvals-and-sandbox`, stdin closed. Sol re-launched that way on a scratch clone of main with orders to MEASURE per-file heap (slurp / node tree / zipper / outline) before designing (`~/tmp/sol/memory-sol-answer-2.md`). Decision framed for Gene via help-gene-decide (below in the terminal): the one director-level item is the write-verb transaction contract; lazy/eager for the read verbs is authorised ("go") and starts when study round three and the census round land, because they touch the same receipt code.

## 04:17Z — Gene: "Make sure to write a test (not a unit test) that confirms we don't OOM; don't want to slow the make run tests too much, tho!"

Rule for the memory work: the no-OOM proof is a BATTERY, `make memory-battery`, outside `make test`/`test-fast`/`mcp-test`: each tree-scale op (ls-tree, alias migration plan, extract discovery, census, fold-diff) at 100 / 1,000 / 10,000 synthetic files (a cheap generator; sparse or templated sources), `-Xmx512m`, five reps fresh+warm, pass lines: no OOM; `heap_used_peak_mb ≤ min(start+224, 0.8×Xmx)`; the 10k peak ≤ 1k peak + 32 MiB; after-GC retained at 10k ≤ 1k + 8 MiB; over-budget cases refuse structured BEFORE any write; outputs equal the unbounded reference. Runs on Anvil on demand and as the merge gate for any MEM change. The fast suites keep only millisecond witnesses: admission arithmetic refuses before allocation; ceiling behaviour AT the ceiling on tiny fixtures; the receipt carries `heap_used_peak_mb`. The MEM intent "retained heap does not grow with repository size" links the battery by id (LID), so a refactor cannot drop it from the gate silently.

## 04:18Z — memory battery build launched (a0e2b73e06754ebf3, `~/src/clj-surgeon-membat`, `bridge/memory-battery` from main): generator for 100/1k/10k synthetic trees, one-JVM-at-a-time runner at -Xmx512m with a heap sampler, pass lines as one verdict fn, MCP-OP-MEM-001 with the battery as its linked witness, a ms-scale verdict witness in the fast suite, `make memory-battery` asserted OUT of `make test`; first run on main is the RED baseline (docs/observations/2026-09-03-memory-battery-baseline.md). Nine lanes live.

## 04:19Z — DECISION (Gene): "B. Go. Love the new make target for testing memory; go"

The write verbs' transaction contract becomes **optimistic serializability with conflict detection and
exact rollback** (Sol's design §1): hashes + spans resident; rollback bytes pinned to a per-transaction
directory under `.clj-surgeon/transactions/<txid>/` BEFORE any live write; the whole semantic read set
revalidated by hash before commit; per-path atomic rename with durable progress; crash recovery restores
pinned bytes and verifies. Not snapshot isolation against writers that ignore the lock — stated in the
contract. Build order: B1 the kernel as a new module on `bridge/txn-journal` (from main, no collision with
the open rf2/q5z/ratchets rounds), LID MCP-OP-MEM-002.., crash-injection witnesses, fence-scope review
before merge; B2 adoption in alias_migration and extract after their rounds land, measured by the memory
battery (peak-vs-N flat). The heap work budget: Sol's 192 MiB inside 512 MiB stands until the battery
baseline says otherwise.

## 04:20Z — B1 launched (a2b6bbcbdc96b2925, `~/src/clj-surgeon-txn`, `bridge/txn-journal` from main). Gene: "Use TDD style; replicate OOM first." Order fixed in the brief: commit 1 = a subprocess test at -Xmx256m that reproduces the OOM of the frozen-read pattern on a synthetic scope (the test PROVES the defect; `make memory-red`); commits 2–3 = the journal (manifest streamed to disk, pin-before-write CAS, read-set revalidation, per-path atomic rename with durable progress, rollback, crash recovery, advisory lock; MCP-OP-MEM-002..006 with crash-injection and concurrent-writer witnesses) and the streaming scope reader (no link following, depth/walk/per-file/AGGREGATE caps, typed refusals with narrowing next_call); commit 4 = the same scenario GREEN through the journal at -Xmx256m with sampled peak under the line and a hash-equal result. Adoption by alias_migration/extract is B2, after their open rounds. Ten lanes live.

## 04:22Z — ratchets fix round landed (70c2c60…49f6e12 on ece8c1c), eight items red-first: addressed-ness by preorder span (two sites on one line → 1 unaddressed, the containment direction pinned); dispatch vocabulary under a 2048-char budget with truncation and no comments/newlines (note: the owner form the refusal teaches now carries the NORMALISED dispatch spelling — safe because the selector compares parsed values, but a real change to that field); invalid-require-policy surfaced on the apply route by resurrecting the dead branch (the contract dropped its duplicate enum check); `#_` discards and `^meta` handled in dispatch extraction (three compiles that threw while scanning now succeed); the schema refuses `expect_matched` on the extraction/edits branches, and — the one contract tightening to flag — `edit_clojure` now refuses undeclared `changes` it used to accept and commit (`unexpected_fields`, no mutation); wildcard note from the parsed pattern; minimal shapes pinned to the live validators with a set-equality guard; cosmetics. Suites: test-fast 718/6005 (baseline five), mcp JVM 400/4138 (baseline one), mcp-smoke green. My full suites queued (`~/tmp/ratchets2-*.log`, waiter b1q8ixsp0); push → Sol re-check (no symlink fixtures here, so no filter issue) with the entrance tightening named explicitly.

## 04:23Z — kondo-path test fixed on `bridge/kondo-path-test` (f8a9ef9, test-only: assert the resolver's rule, not a macOS path; mcp-test 0 failures on this box, the resolver untouched); my full mcp-test queued, push after. The other standing baseline noise — five `terminal-response-routing…` failures (MCP-OP-RELAY-004; the routing doc drifted from the promise at 01f0739) — has its own lane on `bridge/routing-doc-test` (ab9e22b95f515a848). When both merge, every clj-surgeon suite on Linux reads zero failures and "baseline six" disappears from every report.

## 04:25Z — Sol, with a shell (YOLO in a scratch clone of main 37592c7): measured, then the plan re-issued (docs/observations/2026-09-03-memory-design-sol-answer-2.md)

Measured (one JVM, -Xmx1g, SerialGC, 5×System/gc, medians of 5): rewrite-clj node tree = **44.7 heap bytes per source byte** on a 100-file corpus (46.3× on intent_transaction.clj: 126,596 B → 5.59 MiB tree, 21,996 nodes, ~178 nodes/KiB); a root zipper adds ~348 B/file — below noise; final outlines ≈ 14.9 KiB/file (58× smaller than trees, still ~149 MiB at 10k files if all retained); slurped strings 26 KB/file. So "zippers cost 0.55 MB/file" is rejected as an attribution: the tree dominates; the win is tree LIFETIME and concurrency, not a lighter reader (parser replacement moved behind streaming + peak gates). Source audit on main with file:line: ls-tree realises every outline (`group-by`) and both formatters traverse the full set (core.clj:202-250, 321-339, 369-402, 463-481); outline parses twice and holds `:source` per form transiently (outline.clj:225-315; walk.clj:86-96); extract slurps the workspace into a sorted-map (extract.clj:447-479); the MCP workspace reader does the same and both extraction plan and execute call it (mcp_workspace_sources.clj:11-19; mcp_extraction_plan.clj:118-149; mcp_tool.clj:310-423); the generic transaction retains full `:original-sources` AND `:future-sources` plus the concatenated diff through commit because rollback restores from memory (intent_transaction.clj:1394-1473, 1880-1890, 2003-2026, 2175-2274). Correction that matters for B1: durable per-workspace state already lives under `~/.local/state/clj-surgeon/workspaces/<digest>/` (mcp_workspace.clj:40-52) — journal/CAS/cache go there, not in a project-local `.clj-surgeon`. Design unchanged in substance; ordered plan MEM-001..011 with exactly-at-the-ceiling witnesses (001 meter receipt; 002 unified admission; 003 stream ls-tree; 004 streaming fold/spill replacing `read-all`; 005 lexical/parser admission BEFORE building a full tree; 006 disk journal for originals/futures; 007 read-set revalidation under the lock; 008 byte-bucketed reservations; 009 hash-keyed projection cache; 010 streaming fold-diff; 011 battery as release gate) and the numeric pass line (reserved_peak ≤ 192 MiB; peak ≤ min(start+224, 0.8×Xmx); 10k ≤ 1k + 32 MiB peak, + 8 MiB retained). Parallelism intuition from the coefficient: a 2 MiB file's tree is ~89 MiB, so a 192 MiB budget admits ONE such worker until p99 data says otherwise. Both running memory builders (B1 kernel, battery) re-briefed with the state root and the id numbering; Opus consult still measuring for the second voice.

## 04:26Z — census fix round complete (55516d3…ef545c5, eleven commits on 7244141), CENSUS-016..026

Reds worth recording: `pool_size 0` HUNG the tool forever (a zero-thread claypoole pool never completes; killed at 600 s) — worse than the review's "untyped throw"; `pool_size 4096` really started 4096 platform threads; a 2 MB requested file was slurped and censused; one out-of-root symlink refused the whole census; the registry's own CLI example crashed and `:doors conj` made every conj a door; 14 long paths published 4,226 bytes over the 4 KB bound; two files cost four parses; a helper-hidden write read as clean. Greens: server-side validation (`pool-size-out-of-range` with next_call; effective pool capped at the box, `pool_size_requested` echoed); stream-filter before retaining, byte cap on requested paths, Throwable → typed; `walkFileTree` without link following, escapes skipped AND counted (`skipped_outside_root`), the `.git` arm never visited; CLI on the same kernels; a second arm fixture through the pool + a pool unit test; the receipt names the pool that actually ran (under babashka: serial, `pool-size 1` + requested); `by_file` trimmed last; one parse per file with truthful phases; `unrecognised_calls` with examples and a next_action that names them; doors confirmed against every scanned file; docs say five tools. Suites: test-fast 714/6035 (baseline five), full mcp-test 395/4146 (baseline one), oracle pass. Deviations reported, not hidden: claypoole via `requiring-resolve` with a serial fallback (bb cannot load it); a pure `validate-census-params` in the adapter rather than widening the editor validator; discovery SKIPS escapes (a one-directional relaxation) while every retained path still re-enters `resolve-source-path` and a caller-named escape still refuses; `unrecognised_calls` scoped to heads outside the modelled vocabulary (noisy on real bytes — the count is the signal). Process notes: an amend mid-run produced a spurious sixth failure (re-run on a frozen tree: five); builder logs in `~/tmp` were overwritten by a sibling lane — builders now log under `~/tmp/<lane>/`. My full suites queued (`~/tmp/census/my-*.log`); push → Sol re-review (its symlink fixture may trip the content filter; Opus for that item if so).

## 04:27Z — `bridge/kondo-path-test` pushed at f8a9ef9 (test-only; my full mcp-test 377/3949/0, oracle pass). Mayor: merge first — it removes the standing "one baseline failure" from every Linux run; no review round needed for a test-expectation change that asserts the resolver's own rule.

## 04:30Z — fold-diff re-review: NO-GO as specified — the baseline worktree cannot compile the tool (store_checkpoint.clj not copied; exit 1 reads as 'differences'); everything else closed or measured (5,000-event run: 952 MB RSS, 17.9 s; guard count asserted before the pool; no path to ensure-schema!). Round three (ac621a440d5557307): copy the validator, exit 3 on emit failure, two-refs self-test, greedy redaction, DEPLOYED_REVISION in the report, guard hardening, value redaction for sessions/api-keys, run bin/ not make. Verdict filed.

## 04:31Z — `bridge/receipt-ratchets` pushed at 49f6e12 (my suites: test-fast 718/6005 baseline five; full mcp-test 400/4138 baseline one; oracle pass). Not in the queue yet: Sol re-review (YOLO in a scratch clone, high reasoning) re-runs the eight probes and hunts the entrance tightening, the normalised dispatch spelling, preorder-span direction, and the resurrected apply-route refusal.

## 04:37Z — Opus memory consult landed and reconciled with Sol-2 (`memory-design-opus-answer.md`, `memory-design-reconciled.md`): node tree 48× source, zipper 1.2%, outline double-parse 21%/31%, aggregate-bytes ceiling missing = the alias_migration OOM. rf2 round 3 pushed at 465c956 (ruling lands fails-first; full gates baseline-only); Sol re-check running. Night orders filed (`night-orders-anvil.md`, inbox inb-1165ce to the mayor); 10-min heartbeat writes /tmp/anvil-pulse.txt and reads /tmp/mayor-*.txt.

## 04:38Z — census pushed ef545c5 (suites baseline-only, oracle pass), Sol re-review running. Answered the mayor's /tmp/ask.txt (policy-10 shape; `answer-to-mayor-ask.md`, copy at /tmp/anvil-answer-to-mayor.txt): only kondo f8a9ef9 is at GO; asked for a 30-min pulse watch, PR #1, three items carried to Gene.

## 04:42Z — routing-doc fix pushed a9d8701 (GO): the five terminal-response-routing baseline failures were doc drift — 01f0739 rewrote the routing doc for the Surgeon-default ruling and dropped the MCP-OP-RELAY-004 paragraph; restored verbatim-shape, test-fast 702/5912/0. With kondo f8a9ef9 the baseline failure set on main goes to zero once both merge.

## 04:44Z — Sol content filter refused the rf2 round-3 re-check (two "flagged for possible cybersecurity risk" lines on the confinement fixtures, 110k tokens, no report). Fallback per the standing rule: Opus re-review launched on a scratch clone, report to ~/tmp/sol/rf2-opus-review.md; receipt will say so.

## 04:45Z — Gene, going to sleep: "Fantastic; god speed" + fan-out multi-arm testing runs LOCALLY on Anvil (no ssh) + "capture the magic moments of tweezering, watcher doing timings, and parallel tests going on anvil … constantly watching how surgeon is or isn't used, and steering towards decisively winning in the squares we choose … that's what your work should feel like." Actions: chronicle agent mining the session history into `magic-moments-tweezer-watcher-anvil.md`; hourly usage-watch cron (make study-agent-usage → log line); E3/E6 prestage re-briefed to run arms here under ~/tmp/arms; memory saved.

## 04:46Z — vision.md: added "How we work, so the goal is surely reached" (Gene: "It states the goal, but let's add how we work so that we will surely achieve it") — the six-step loop: tweezer with a watcher, fan out locally on Anvil, usage watch on a clock, steer per square, receipts/LID/TDD/battery/red-team before merge, chronicle as you go.

## 04:47Z — ratchets Sol re-review: GO-WITH-FIX — 7 of 8 prior items CLOSED (preorder spans, MINIMAL policy, discards/meta, schema, wildcard note, pinned examples, cosmetics); PARTIAL: a multiline dispatch string still presents on two lines; NEW: the candidate catalog advertises `changes` on its projected edit_clojure while the handler now refuses it. Tightening audit clean (no production caller sends `changes` to edit_clojure). Round 3 launched (Sonnet). Verdict filed `ratchets-rereview.md`.

## 04:48Z — vision.md: added "When the work is delegated" under How we work (Gene: "we inspect the results, we analyze its telemetry, caring just as much about its methods and timings as if we did the work ourselves") — a delegated run is a measured arm: brief = pre-registration, report = claim, its telemetry = a free adoption arm, its friction = our ledger, executed re-derivation before the queue, same squares.

## 04:49Z — kondo f8a9ef9 MERGED into main by surgeon1 (acda1b3, "merge main"); my push was rejected non-fast-forward, rebased, pushed. Gene asked for the HOW of delegated-work inspection from the actual history; an Opus history-mining agent is writing `delegation-techniques-the-how.md`.

## 04:50Z — memory battery BUILT and pushed (bridge/memory-battery 2bae68b): `make memory-battery` at -Xmx512m, N=100/1k/10k, fresh+warm. RED on main as predicted — verbatim: `FAIL peak-over-budget {:op :cli-ls-tree, :n 1000, :phase :fresh, :observed 274.8, :limit 247.8}`, `{:n 10000 … :observed 418.3, :limit 247.9}`, `FAIL peak-scales-with-n {:op :cli-ls-tree, :observed 433.2, :limit 333.3}`, `{:op :workspace-sources-read-all, :observed 203.8, :limit 105.2}`; `UNMEASURED reserved-peak-over-budget (no admission accountant exists)`; no OOM; every bounded result hashed identical to the -Xmx4g reference. Retention: ls-tree 9.4 KB/file, read-all 4.1 KB/file (1.01 heap bytes per source byte), rename full-match 1.0 KB/file — linear but passes Sol's lines (a gap, sent to the kernel builder to rule). Generator: 10k files in 973 ms. Self-test proves the battery is absent from the `test` closure. B2 (MEM-015) and B1 briefed with the lines they must move; Sol reviewing the instrument.

## 04:51Z — q5z round 3 pushed ca677bc (23ea871..ca677bc, 7 commits, +10 tests/+119 assertions, baseline-only failures, oracle pass). The OOM shape is now a typed refusal: `alias-migration-scope-too-large-bytes`, `scope_bytes 855000000`, ceiling 256 MiB, before any slurp at -Xmx512m (sparse fixtures, zero bytes allocated); depth counted per entry (walkFileTree truncates silently); chmod-000 subtree → `scope-unreadable` (was laundered as expect-mismatch); walk bound 50,000 with no strings materialised; next_call replays proven by byte-compare; retention best-effort, documented; .cpcache hygiene gate as one script with a five-case self-test (old gate printed PASS on a forced add). Builder left next_call nil on the two aggregate ceilings with a written reason — sent to Sol to rule. Sol re-check running.

## 04:52Z — routing-doc a9d8701 MERGED by surgeon1 (merge commits 3ebeafd, d1c5330). With kondo, main's baseline failure set should now be ZERO — every branch's "baseline-only" claim from here on means 0 failures. Lesson paid: my own `git add docs` swept two agents' in-progress docs into a commit (caught before push, un-swept); I add by name from now on (memory: main-checkout-is-shared-add-by-name).

## 04:55Z — E3/E6 pre-staged (712a828, 1,234 lines): matched pair at N=21 (E3-L real textual fan-out where alias_migration provably does not apply; E3-P synthetic alias-shaped, one call); pass lines as numbers (1 committing verb call; non-test actions ≤ 10.5; churn [47,71]/[27,41]; wall claimable only above 172 s = the 2 sd floor); E6 ≥ 2 of 3 free-choice ls-tree calls, 0 of 3 closes the row; six arm prompts verbatim, ritual-strip block, no "available and expected"; attestation read from the server; watcher receipt per arm. Go tree: E3 needs q5z merged + battery green; E6 needs study-ops merged. Apparatus (attest.sh, watch.py, score.py, PF-5 smoke) launched as a builder on bridge/anvil-arms-apparatus so the morning "go" is one word.

## 04:56Z — census Sol re-review: NO-GO. Prior 11: 8 CLOSED, 3 PARTIAL. New: 4,002 sources → ok:true read_complete:true claiming 4,000 (silent truncation); a 2,097,153-byte file silently omitted while its 2,097,152-byte sibling publishes completion; CLI still fs/glob then take 4000; pool_size "8" accepted against an integer schema; 512 duplicate paths → files 512, arms 4608 (512× inflation); LLD promises a parse phase that does not exist. Ceilings inclusive and typed at 64/512/32/2 MiB. Classifier parity byte-for-byte (folds.clj 2,240 = 2,240; inventory_folds.clj 1,527 = 1,527). Round 3 launched (Opus) with fail-first witnesses AT each ceiling.

## 04:58Z — main re-baselined after kondo + routing-doc merges: `make test-fast` → ; 0 FAIL/ERROR lines, EXIT 0 (log test-fast-ca04f82.log). Every "baseline-only" claim from here means zero.
## 04:59Z — CORRECTION to the line above: the log filename took a different sha mid-run (I committed between the redirect and the exit stamp), so the figures were read from an empty file. Real log ~/tmp/main/test-fast-981a9f1.log: `Ran 702 tests containing 5912 assertions.` 0 FAIL/ERROR lines, exit 0. Main fast-suite baseline = ZERO failures at 981a9f1. Lesson: compute the log path once into a variable; never re-evaluate HEAD inside a long command.

## 05:07Z — q5z round-3 Sol re-check: NO-GO. Headline ceilings hold (855,000,000-byte scope refused with slurp_calls 0 at 512m; depth 65; chmod-000 → scope-unreadable; suffix ordering correct; link loops terminate). Blocker: with receipt-dir co-located with the detail dir, best-effort pruning deleted a SUCCESSFUL run's own undo receipt while ok=true. Also: OOM marker set before the kernel (pre-kernel OOM reports source_unchanged false); visitFileFailed never TERMINATEs (50,013 past the 50,000 bound); expect.files decremented for unread exclusions (2 → 0); hygiene gate swallows a failed `git ls-files` into green. And Sol DISPROVED the builder's impossibility claim: a 296-char prefix-narrowing next_call selected 141 files / 267,900,000 bytes under the 268,435,456 ceiling; the count analogue 288 chars → exactly 2,000 files. Round 4 launched (Opus). Verdict filed.

## 05:08Z — rf2 Opus re-check (filter fallback): round-2 items all CLOSED; the ruling CLOSED on the CLI/in-process entrance but OPEN on the MCP entrance — mcp_tool.clj:323 calls read-all + compile-plan directly, bypassing the walk/prune/canonicalise; read-all excludes .git lexically then keys by canonical path, so a symlink under src/ defeats it: the reviewer WROTE `.git/hooks/caller.clj` through the MCP entrance. Also: canonicalised prune set forbids trees the walk read (`out -> src` refuses every extraction); unreadable dir vanishes silently; confine-workspace-paths has no prune half. I treat it as NO-GO; round 4 launched (one discovery kernel for both entrances + parity test). Side note from the reviewer worth keeping: a gate red on every Linux box teaches seats to read "1 failures" as normal — kondo fixed exactly that today.
## 05:08Z — battery Sol instrument review: GO-WITH-FIX as tooling. Re-run reproduced held_mb almost exactly (94.0/93.6, 40.8, 9.8) while one identical peak cell swung 28.3 MB across the verdict line — peak is a TREND, held is the gate. Fixes: PASS (INCOMPLETE) exit 0 is possible (must be INCOMPLETE, nonzero); manifest no-op trusts claims; shared reference cache unattested; no held_mb gate — Sol's exact MEM-011 line: max(held at 10k) ≤ max(held at 1k) + 2.0 MiB; doc's "about 410 MiB" is wrong (enforced ≈ 248). Round 2 launched (Opus).

## 05:16Z — ratchets round 3 pushed fe7a1a1 (905e4a3 multiline dispatch → one physical line, read-string round-trip; fe7a1a1 candidate-catalog schema narrowed rather than rebinding a handler the production entrance refuses). Gates baseline-only on the OLD baseline (branch predates the kondo/routing merges). Collateral: a pre-existing handler-identity assertion in the candidate-catalog test fails in red and green alike — filed to the inbox (no bd db in worktrees). Sol re-check running.

## 05:18Z — arms apparatus BUILT (bridge/anvil-arms-apparatus 598139c): attest.sh (server identity read from the server: /proc/<port-pid>/cwd HEAD + ready-root HEAD, "unverified" → refuse), watch.py (event-driven, zero-return window kills a hung driver in 4 s, typed abort), score.py (computed counts only; missing or empty rollout → exit 3, NO receipt), run-arm/run-cohort (mirrored N-1 T-1 T-2 N-2 N-3 T-3), prompts built from the doc's fenced blocks with a drift check (case 9). `make anvil-arms-self-test`: "anvil-arms self-test: 55 passed, 0 failed" in 11.2 s. `make test-fast` measured twice: "Ran 702 tests containing 5912 assertions. 0 failures, 0 errors." Six documented choices where the spec was self-contradictory (the load-bearing one: A.4's server-sha field vs A.5's project-dir binding would refuse every tool arm; resolved by capturing both identities). Owed before a cohort: PF-1..4 and PF-6, the G1 hand-drive first. Sol instrument review running.

## 05:30Z — ratchets round-3 Sol re-check: GO-WITH-FIX. Both round-2 defects CLOSED (multiline string → one physical line, reader value equal; split-catalog `changes+expect` → :public-schema-denied, mutation-attempted=false); string values with backslashes/quotes/\u03bb round-trip; teaching-text audit clean (no live prompt or skill teaches `changes` on edit_clojure). Open: a `:regex` leaf with a raw newline still presents on two lines. Also: the branch must absorb origin/main (baseline now zero) before the mayor's gates. Round 4 launched (Sonnet): regex escaping witness + merge main + three gates at zero.

## 05:31Z — arms apparatus Sol review: NO-GO for a real cohort (self-test 55/0 reproduced; the happy path is fine). P0s: score.py skips malformed lines and emits receipts from truncated/duplicated/reversed streams with sources.agree=true; rc 3 leaves a stale receipt; empty watch.jsonl → rc 0; a tool call with no result is a citeable receipt; the driver is not in its own process group (orphaned `sleep 60` under PPID 1); health JSON accepted with ok:false and a wrong project_root; newest-mtime rollout latch bound a foreign session (blocker — per-arm CODEX_HOME + exact path); prompt check passed after the doc's governing prose changed; run-cohort continued past ATTEST-MISMATCH and n=0 exits success; cleanup may signal a pid it did not start; write paths outside the runner root. Exactly the instrument-lies class the night orders name. Round 2 launched (Opus) with fail-first self-test cases per item.

## 05:40Z — study round 3 pushed 3a237ab (212b045..3a237ab, 11 commits, +15 tests/+234 assertions, baseline-only on the old baseline, oracle pass). Headlines: the source budget charges source RETURNED (outline of intent_transaction.clj, 126,596 chars, was refused at 65,536 — now ok with 0 source returned); discovery counted each file once under a cap that stops the walk (500 files + 500 sibling deps.edn: file_count 250,000 → 500, 8,919 ms → 97 ms); ns_grep compiles once under a guard with typed field refusals; continuations carry their limit and are refused when they cannot succeed; the rendered rg remedy ends option parsing before the pattern. Builder corrected two of the reviewer's figures with real bytes (largest atomic result 22,141 not 28,168; floor 38 not 40). Sol re-check running; E6 cohort waits on this merge.

## 05:50Z — 
## 05:50Z — Sol content filter refused the study round-3 re-check (2 flags, 129,970 tokens, no report) → Opus fallback launched (report to ~/tmp/sol/study-opus-review.md).
## 05:50Z — CORRECTION (the previous usage line was empty: a quoting error in my extractor, not a collector zero) — usage watch: mcp_tool_calls 96, outcomes {"ok": 49, "refused": 47}, tools {"inspect_clojure": 96}, providers claude 96 / codex 128, error_types 47 (top: no-clojure-files 11, invalid-grep-pattern 7, study-tree-too-large 6, study-output-limit 4); window since 2026-08-30T15:00:00Z

## 06:03Z — Lock contention ruling: 10 waiters on ~/tmp/suite.lock at load 3.6 on 16 cores — the one-JVM-suite rule was Buster's (4 cores). New rule for Anvil: unit suites (test-fast, mcp-test, oracle, kaocha) run under `~/bin/suite-run <cmd>` = one of three lanes (suite-1..3.lock); the memory battery and memory-red keep the exclusive suite.lock. Wall numbers inside the battery are trend lines, heap is per-JVM, so concurrent unit suites do not corrupt its gate. Every live builder messaged.

## 06:05Z — curtain-call template-upsert fix pushed fdccfc8: `comms-template-identity` compared ids through blank->nil (str + trim), so `"x "` = `"x"` and `5` = `"5"` — two templates silently collapsed on replay; now raw ids, nil/blank → whole-template identity; witness red→green (2 rows → 1 before); two fixtures blessed; unrelated digests byte-identical; unit 1055/13168/0. Sol re-check running.

## 06:06Z — BOUNDARY INCIDENT (reported, not hidden): my first Sol launch on the curtain-call template-upsert clone died at codex session start — "required MCP servers failed to initialize: cclsp (127.0.0.1:7890)". The same repo-level `.codex/config.toml` declares clj-surgeon at 127.0.0.1:7888 as REQUIRED, so codex may have completed an MCP handshake with the other seat's production Surgeon before failing on cclsp. A handshake is a read (initialize/list-tools), no write path; the log was overwritten by the relaunch so I cannot prove either way — `:unverified`. Ratchet installed: ~/bin/sol-yolo now disables every repo-declared MCP server unless an explicit url is passed (then only clj-surgeon is re-pointed). Relaunched the review with both servers disabled. Mayor notified on inb-1165ce.

## 06:07Z — q5z round 4 pushed 562b214 (12 red/green commits; main absorbed; `Ran 734 tests containing 6257 assertions. 0 failures, 0 errors.` / `Ran 426 tests containing 4848 assertions. 0 failures, 0 errors.` / oracle pass). Detail docs own a name pattern (`detail-<uuid>.edn`) and a typed collision guard keeps receipt/detail namespaces disjoint; heap marker moved to the kernel entrance; visitFileFailed + postVisitDirectory terminate over-bound; both aggregate ceilings carry a ≤ 512-char narrowing next_call that replays to a commit (builder chose largest-fitting-subtree, ties deepest, because Sol's literal tie-break picks a leaf and cannot reproduce Sol's own figures — sent to Sol to rule); expect.files moves only for read sources, and base-call now carries scope.exclude (an omission Sol had not named — the chain could not converge without it). ALIAS-054/055 registered. Sol round-4 re-check running. Also: verified no clj-surgeon Sol run had MCP servers configured (no repo-level .codex/config.toml there; user config declares none) — the 7888 exposure was the curtain-call clone only.

## 06:09Z — ratchets GO at c5ef7ca: round 4 (ea05e5e) re-escapes a `:regex` leaf's raw newline as `\n` (regex-native; presented `#"a\nb"` on one line; read-back .pattern is `a\\nb`, behaviourally identical — stated in the docstring); main absorbed (c5ef7ca). I verified the one delta myself instead of a fifth Sol round: read the 19-line diff, ran the focused namespace under suite-run (`Ran 11 tests containing 74 assertions.` 0/0), and the builder's three gates at zero. Census round 3 pushed f43ac03 (six red/green pairs, CENSUS-027..031; both entrances refuse at 4,001 with files_read 0; 2,097,153-byte source named in oversized_skipped; pool_size parity; 512 duplicates → files 1, duplicates_collapsed 511; LLD matches) — Sol re-check running.

## 06:13Z — arms apparatus round 2 pushed 6c1cf0a (598139c → 22 commits): "anvil-arms self-test: 162 passed, 0 failed"; test-fast 702/5912/0. Every one of Sol's 12 items red-first: malformed/duplicate/reversed/truncated streams → typed abort, no receipt, stale receipts deleted; no-result tool call → incomplete-run; `make verify` metered via a `make -n` map hashed into the attestation; driver in its own session, group reaped (orphans 0); health JSON bound field-by-field to independent witnesses; per-arm CODEX_HOME bound by the session id codex announces (glob latch deleted); governing prose hashed (the "three→four edits" drift now fails --check); cohort aborts on first ATTEST-MISMATCH, n<1 refused; stop-server signals only the recorded pid with matching /proc start time; roots outside ~/tmp/arms refused. Discrimination check: Sol's good arms still score byte-identical. Note for a real cohort: COHORT_PORTS must name only ports the cohort holds (7908 was held by a reviewer server → native arms correctly refused). Sol round-2 review running.

## 06:15Z — rf2 round 4 pushed 965d49e: ONE discovery kernel (`extract/discover-workspace-sources`) feeds :extract, :extract!, the MCP plan route and the MCP apply route; `read-all` no longer walks; the w9 MCP witness went from `{:ok true}` + a rewritten `.git/hooks/caller.clj` (RED, verbatim in the commit) to `caller-path-in-skipped-tree`, byte-identical, link intact, no target — with a parity test asserting identical source sets and refusal types on both entrances. Prune rule: refuse only under a canonical pruned prefix that is NOT an ancestor of a path the walk read (kills the `out -> src` false refusal); `:unreadable` is a typed, counted, non-harmless skip; pre-write confinement applies the prune check; dry-run reports instead of refusing. EXTRACT-037 now says an entrance that builds its own universe is a defect even when its tests pass. Merge repair: main's kondo fix added a call that the requalify reference test now correctly rewrites (10 → 11). Gates at zero. Opus round-4 re-check running.

## 06:18Z — template-upsert Sol re-check: GO-WITH-FIX. Fix confirmed red→green (1/1/1 → 1/4/0); raw-equality matrix as the old loop ("x"/"X" 2; 5/5N 1; 5/5.0 2; :x/"x" 2); UI posts string ids and mints UUIDs for blank, so malformed ids are replay-only shapes; all pre-existing digests byte-identical incl. the 3,246-fact judge-sandbox digest. Open: a PRESENT blank-string id falls back to whole-template identity (2 rows vs the old loop's 1) — uncharacterized, and contradicts FOLD-IDEM-004's EARS; four references cite a doc that lives in clj-surgeon, not this repo; `:person-id` still goes through blank->nil (FOLD-IDEM-002/003) — separate audit filed. Round 2 launched (Sonnet): nil-only fallback, fixture pinned, EARS amended, qualified provenance.

## 06:18Z — study round-3 Opus re-check (Sol refused twice): GO-WITH-FIX. All 11 prior items re-run: 9 CLOSED, 2 PARTIAL. BLOCKER found by the hunt: ns_grep is guarded at compile time only — `(.*.*.*.*.*.*)*x` over dir "src" (67 files, 36-char paths): 294 → 1,242 → 7,091 → 41,804 ms as groups grow; at max_files 2000 ≈ 21 min, at the 20,000 ceiling ≈ 3.5 h; paid inside ls-tree before any byte budget; a future cannot cancel a matcher. Also: the empty receipt drops project headers (STUDY-024 vs -030 contradict); the file cap is tested after a whole project materialises (3000 realpaths at cap 10); `:args` returned verbatim and uncharged (2,696 chars); a :paths entry through a symlink silently yields no-clojure-files. Disputed figures ruled: largest atomic result 22,141 (builder right; true repo max 51,379 on a different subject); floor is 35 + digits (both sides half right). Round 4 launched (Opus): step-counting matcher first.

## 06:21Z — B2 MEM-015 landed (bridge/read-path-memory 61cb9b5, TDD: RED a845215 → registry b6aebbc → GREEN 46a61d2). Unit fixture 48,097 B: allocation 62,686,992 → 37,583,552 B (1303× → 781× source; the irreducible node tree alone is 749×, so my brief's "≤ 30×" was physically wrong — the builder measured the floor and set the ceiling at 980× = green × 1.25, stated in the docstring). 1,000 files: wall 11,784 → 6,621 ms (−43.8%), allocated 25.28 → 14.95 GB (−40.9%), identical SHA-256 over all outlines. Battery (cherry-picked 2bae68b): cli-ls-tree wall −19 to −38.5%, peak −7.6 to −24.9 MB, held_mb unchanged; NO pass line flipped (nearest 255.7 vs 247.7 MB) — G1 peak is set by what is live, and ls-tree holds every outline: the streaming leaf (MEM-003) owns that. Gates at zero after absorbing main. Sol review running.

## 06:21Z — q5z round-4 Sol re-check: NO-GO with 3 left (4 CLOSED). Sol conceded the aggregate next_call tie-break: "My earlier depth-first rule would prefer a tiny deep subtree and was wrong; largest first, then deepest, then lexicographic is the useful deterministic rule" — the builder's 316/315-char calls replayed to commits. Remaining: prefix-matching is not ownership (a caller's own `detail-*.edn` was pruned); the collision refusal fires after mkdirs and misses a symlinked twin; `attempted` still precedes the real write (entrance OOM reports mutation attempted with sources byte-identical and no receipt). Gates at zero. Round 5 launched (Opus).

## 06:25Z — battery round 2 pushed c6a2264 (11 commits, main absorbed; self-test 24/138/0; test-fast 726/6050/0). Round-2 table (verbatim in the baseline doc): FAIL (INCOMPLETE) exit 1 — three HARD failures on the NEW held_mb line: cli-ls-tree 9.5 → 94.0 MB (limit 11.5), workspace-sources-read-all 4.4 → 41.0 (limit 6.4), rename-ns-plan-full-match 1.0 → 9.9 (limit 3.0) — all three PASSED round 1 on the same numbers because nothing looked at them; narrow arm flat at 0.1 (control); grow_mb ≤ 0.4 everywhere (no leaks); parity on all six corpora; reserved peak UNMEASURED. Adversarial arms paid off first run: cli-ls-tree peaks 386.4 MB on ONE 1.9 MiB file and 285.7 MB on ONE 300-deep 111 KB file (1,322 MB at 4g) vs a 248 MB budget — heap sized by a file's SHAPE, invisible to any tree-scale arm; Opus's "2 MiB per-file ceiling exceeds one thread's budget" now has a measured witness. Attestation fired twice unprompted (unattested-reference, then stale-reference on a src edit). Held values reproduce to 0.3 MB across three runs while peak moves by tens. Builder's own catch: `(#{:default nil} nil)` is nil — a filter that silently disabled all cross-N lines. Sol round-2 review running.

## 06:25Z — B3 launched: MCP-OP-MEM-005 parser admission (bridge/parser-admission from the MEM-015 tip), TDD from the battery's shape finding (one 1.9 MiB flat file → 386 MB; one 300-deep 111 KB file → 286 MB, 1,322 MB at 4g): single-pass lexical pre-scan (node estimate + nesting depth, no tree), ceilings derived from measurement so every file under src/ and test/ admits with margin and the two adversarial shapes refuse before the tree constructor; typed, counted skip in tree-scale receipts. Battery cells before/after as the receipt.

## 06:27Z — census round-3 Sol re-check: NO-GO. MCP entrance is right at every ceiling (4,000/4,001; 2 MiB ±1; pool_size fuzz refused in 0.3 ms before discovery; 512 duplicates → 1). The NEW CLI walk (fs/walk-file-tree) is the defect: follows an escaping symlink (CLI files 2 vs MCP 1 + skipped_outside_root 1), reads a two-link chain three times, returns no-fold-arms-found on a symlinked root where MCP reads; the ceiling next_call carries a placeholder string and replays to unreadable-source-path; CLI success receipt drops files_scanned; oversized list hard-codes 12 with no omitted count. Round 4 launched (Opus): one discovery kernel for MCP and both CLIs (the same rule rf2 round 4 applied), real subtree narrowing per the q5z ruling. Template-upsert round 2 pushed 25b98a83 → GO: blank-string ids compared raw (one-condition change, read by me), fixture 59b8b4c2 pinned, digests byte-identical, five provenance refs qualified, unit 1056/13176/0.

## 06:27Z — rf2 GO at 965d49e (Opus round-4 re-check): the w9 MCP-entrance probe refuses with .git byte-identical; `out -> src` and `target -> build_out` extract normally; unreadable → typed skip; dry-run reports with :discovery; read-all callers all branch on :ok; the 10 → 11 repair is correct (main added one expand-command call). Residuals, pre-existing and identical on main, filed as follow-ups: the prune is root-only so a NESTED .git is a writable source tree; the pre-write prune half is inert for plan-canonicalised paths (bounded by the caller hash fence that runs first); unreadable out-of-root link is fatal while in-root is soft; MCP apply refusal drops :tree/:resolves-to/:discovery. Five GOs tonight: kondo, routing-doc (merged), ratchets, template-upsert, rf2.

## 06:31Z — arms apparatus round-2 Sol review: NO-GO (6 CLOSED, 6 PARTIAL). What closed: JSONL validation, stale receipts deleted, unanswered calls refused, health bound to witnesses, cohort aborts on first failure, root symlink refused. Still fail-open: attest-time `make -n` EXECUTED `$(shell …)` and `+$(MAKE)` (repo-controlled code ran); rollout rotation mixed inodes → rc-0 receipt with sources.agree=true; watcher rc 5 (idle-stop) scored rc 0; deleting the final `end` record still scored; a driver calling setsid escaped the PGID reap while reporting zero orphans; unknown/conditional make targets fail open as non-test actions; the B.4 parent paragraph ("byte-identical outside §5") is not hashed; boot id absent from pid binding; `--exp ../x` escaped the runner root; the self-test overrides COHORT_PORTS. Round 3 launched (Opus): static Makefile parse (never execute), inode-bound rollout, any watcher abort refuses scoring, `end` required, process-tree reaping with a computed orphan count, prose hashed, boot id, component validation, honour COHORT_PORTS.

## 06:33Z — MEM-015 Sol review: GO-WITH-FIX, implementation correct. Confirmed by Sol: the reconstruction matches origin/main's old outline-source exactly (independent overlay: 160 files, 0 mismatches); reverting to the exact old implementation raises allocation to 1363× and the checked-in witness FAILS at 1337.8× with parse-count failing at exactly two — the witness is real; all eight production callers keep exact :source; the receipt's interpretation is right and the residual owner is MEM-003 streaming ls-tree (should flatten held_mb and flip the cli-ls-tree scaling line), with MEM-001/002 first. Fixes: the gate takes 3 samples not the documented 5; 980× is tied to an unpinned JDK (explicit rebaseline, never a bump); the differential shares the refactored builder (freeze the old one; add malformed-reader parity); EARS overpromises for include_string_symbols. Round 2 launched (Sonnet). One filter flag in Sol's log but the review completed.

## 06:37Z — B1 kernel BUILT (bridge/txn-journal 1cece9a): TDD from the OOM as Gene ordered — RED cc04af6: 600 × 512 KiB (¼ the per-file ceiling, < ⅓ the file ceiling, every ceiling admits) → `Terminating due to java.lang.OutOfMemoryError` at -Xmx256m, 8-file positive control exit 0; GREEN cb67bd9: the SAME scope commits 600 files at the SAME -Xmx256m, retained 14.19 MB, flat 13.95 → 14.15 MB from 60 → 600 files, reserved peak 29.4 MB inside the 192 MiB budget; frozen reference at 2g used 2,046.8 MB; three-way digest parity 55423110…. 34 witnesses, 18 fail-first mutation probes all red; three probes were FINDINGS (vacuous `every?` on an empty restore set; a retention witness the JIT could hide; journal had no confinement — closed in fc1f0da). Builder gated on forced-GC retention + flatness, not sampled peak (matches Sol's instrument ruling). `make memory-red` 4/25/0. Battery on this branch identical to main's RED — correct, no verb adopts the kernel yet (B3 adoption next). MEM-015 id collided with the read-path lane → bounded walk took MEM-020; MEM-001 registered twice (fold on merge). Sol kernel review running.
