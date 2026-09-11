# Block 2 red-team — ship v3.10 staged set (`/var/tmp/forge/ship-v3.10`)

**VERDICT: NO-GO for use on this seat as staged.** The set cannot pass its own installer. Fable's real
install into `/home/forge/bin` finished while I was reviewing and ended:

```
   run-ship-v2:   28 rows, mismatches: 1        run-ship-v3:   22 rows, mismatches: 17
   run-ship-v3.1: 13 rows, mismatches: 2        run-ship-v3.4: 14 rows, mismatches: 13
   run-ship-v3.6:  9 rows, mismatches: 1        (v3.2, v3.3, v3.5, v3.7, land-auto, land-publication: 0)
INSTALL NOT PROVEN — a fixture mismatched against the installed files. The backups above are …
```

**34 mismatches across five suites**, exit 3 — *after* every target file had already been replaced, so
`/home/forge/bin` now carries v3.10 bytes that the installer refused to certify (`~/bin/ship` = the staged
`ship` sha `e7e50367…`; `~/bin/MANIFEST.installed.sha256` still describes v3.9 and now fails its own check).
My independent fixture install reproduced the identical per-suite counts, so this is deterministic, not load
or contention. Two distinct causes, both readable in the v3.9→v3.10 diff and both visible in one failing
fixture row: a new unrecorded inter-verb contract (`fence-run stop <pid>` → `fence_calls=3(want 2)`), and a
pathspec bug that silently empties every fix patch (`reason=fix-patch-empty`), which disables the entire
GO-WITH-FIX auto-close path in production, not only in fixtures. Independently: a builder inside the
`:standard-anvil` Landlock envelope **escapes it and executes code as the seat**, and the packet still
terminates green.

**Immediate operational note for Fable:** the seat's `~/bin` is in the unproven state described above. The
rollback the installer names is `/var/tmp/forge/ship-v3.10-fx/backups/20260911T190704Z/` (v3.9 bytes,
verified present).

Reviewer: Opus, forge@anvil, 2026-09-11. Everything below was executed against a **fixture install**
(`SRC=/var/tmp/forge/ship-v3.10 DEST=/var/tmp/forge/ship-v3.10-fx/redteam-dest`), with isolated ledgers
under `/var/tmp/forge/ship-v3.10-fx/redteam/`. The staged set was never edited. **Production-ledger
isolation verified by content**: 421 of my events recorded in my witness journals, `0` present in
`/var/tmp/forge/ledger/events.jsonl` (baseline 2,579 events → 2,592; the 13 new ones are one unrelated
live `run-bg/v3.9` run, `8f83b5ae`). Two subscription launches were budgeted; **zero were spent** — every
builder probe used a stub.

---

## P0 — the set does not install

### P0-1 `ship` invokes a `fence-run` subcommand that does not exist in the contract it inherited

`ship` v3.10 adds three new calls (`ship:464`, `ship:545`, `ship:561`):

```
if [ -n "${RPID:-}" ]; then "$FENCE_RUN_BIN" stop "$RPID" || event "REVIEW stop archive requires reconciliation pid=$RPID"; fi
```

`FENCE_RUN_BIN` is an **env-overridable dependency with an established calling convention**
(`fence-run <sha> <brief> <verdict-name>`); v3.9's `fence-run` has no `stop` argument anywhere
(`grep -n stop /var/tmp/forge/ship-v3.9/fence-run` → no output) and v3.9's `ship` calls it exactly once
(v3.9 `ship:1301`; v3.10 keeps that launch at `ship:1333`). Every existing wrapper/stub therefore receives `stop <pid>` as `<sha> <brief>` — including
the installer's own shipped fixtures. In `/var/tmp/forge/tighten/fixtures/run-ship-v3.sh` the stub logs a
call, `rm -f "$FX/verdicts/$3.md"` with an empty `$3`, resets and re-checks-out the shared fence worktree,
and launches a *second* detached reviewer. Hence:

```
   run-ship-v2:   ship-v2 fixtures: 28 rows, mismatches: 1     MISMATCH item2-cold-lease-is-taken-over: review-not-launched-after-takeover
   run-ship-v3:   ship-v3 fixtures: 22 rows, mismatches: 17
   run-ship-v3.1: ship-v3.1 fixtures: 13 rows, mismatches: 2
```

(`/var/tmp/forge/ship-v3.10-fx/install-final.log` = the real install; `/var/tmp/forge/ship-v3.10-fx/redteam/install.log`
= mine; `/var/tmp/forge/ship-v3.10-fx/redteam/shipv2-rerun.log:12` = the named row from a third, standalone run.)
The v2 row is exact and diagnostic: the fixture asserts `calls fence = 1` after a cold-lease takeover; a
NO-GO run now calls `fence-run` twice (launch + stop), and the fixture's label
("review-not-launched") is misleading, not wrong.

**This is the brief's own STOP CONDITION case** ("a line existing waiters parse") and it is not recorded
in REPORT.md's compatibility section. REPORT.md says "RUN, JSON handle, START/END RECEIPT, INDEPENDENT and
SHIP status-line shapes were preserved" — true as far as it goes, but it does not mention that the
`ship → fence-run` argv contract changed, nor that `SHIP-REVIEW-LATE` was **removed** from the SHIP status
vocabulary (`diff <(grep -o "SHIP-[A-Z-]*" v3.9/ship|sort -u) <(… v3.10/ship …)` → `-SHIP-REVIEW-LATE
+SHIP-QUEUED`).

**Repair (authority required, not a code fix I can bless):** either (a) teach the fixtures/stubs the new
subcommand and re-bless them as part of this set, with Fable/Gene's waiter-contract authority; or (b) guard
the new calls (`"$FENCE_RUN_BIN" stop … 2>/dev/null || true` is *not* enough — the stub's side effects are
the damage) by giving the stop path its own binary/env (`FENCE_STOP_BIN`, default `$FENCE_RUN_BIN stop`);
or (c) drop the stop calls. In all cases the set must pass its own installer before it is installed.

### P0-2 the deleted policy: a reviewer that was deliberately never killed is now killed

v3.10 replaces v3.9's fast-lane-red block — which explicitly refused to kill a working reviewer, wrote
`late-verdict.sh`, and appended `SHIP-REVIEW-LATE … informational=true` — with `"$FENCE_RUN_BIN" stop "$RPID"`.
The deleted comment states the invariant in the first person ("The reviewer (pid=$RPID) is NOT killed. Its
verdict is evidence someone is already paying for"). Reversing a written invariant is a direction change,
not an obviously-better change; it needs the owner's ruling and a witness, and it is the mechanical cause of
part of P0-1.

### P0-3 a failed install leaves the destination replaced, unproven, and with a lying digest manifest

`install.sh` installs all 34 files by atomic rename **first**, then runs the fixtures, and only writes
`MANIFEST.installed.sha256` if they pass. Today's real install therefore left `/home/forge/bin` carrying
v3.10 bytes while `/home/forge/bin/MANIFEST.installed.sha256` still describes v3.9:

```
$ (cd /home/forge/bin && sha256sum --quiet -c MANIFEST.installed.sha256)
ship: FAILED   land: FAILED   fence-run: FAILED   receipt-chain: FAILED   records-push: FAILED …
```

The installed-digest receipt is the artifact a later reader trusts; after a refused install it is worse than
absent. Consequence chain, executed: `lib/packet.py:seal_manifest` builds the "shipped one-shot closure"
from `MANIFEST.staged.sha256` *or* `MANIFEST.installed.sha256`, and **when neither is present it seals an
empty closure with no refusal** — a packet built on my install-refused DEST sealed
`one-shots: ['/usr/bin/python3','/usr/local/bin/bb','/usr/bin/bash','/usr/bin/git']` and nothing else, so
`verify_manifest`'s `stale-session` refusal protects none of the verbs the packet actually runs. On `~/bin`
the failure is quieter and worse: the v3.9 manifest names 21 files, so packets sealed there silently omit
`run`, `measure`, `lib/packet.py`, `lib/admission.py`, `lib/fence.py` — the new code — from the closure.

The rollback is also incomplete for a set that *adds* files: `/var/tmp/forge/ship-v3.10-fx/backups/20260911T190704Z/`
holds 25 files (everything that already existed in `~/bin`), while v3.10 installs 34 — so `cp $BACKUP_ROOT/<name>
$DEST/<name>`, the instruction the installer prints, restores v3.9's verbs and leaves nine v3.10-only files
(`run`, `measure`, `lib/packet.py`, `lib/admission.py`, `lib/fence.py`, `lib/andon.sh`, `schemas/*`) standing
beside them. Say so in the message, or make rollback a verb.

**Ratchet:** `seal_manifest` must refuse (typed) when no inventory is found; `install.sh` should write the
installed manifest and a `PROVEN=false` marker at the moment it mutates the destination, and its
`staged-set-drift` refusal must say the destination was already replaced and where the backups are.

### P0-4 the self-referential-manifest fix silently killed the whole GO-WITH-FIX auto-close path

v3.10 changed ship's fix capture to exclude the verdict document from the patch (`ship:891-892`):

```sh
git -C "$fw" diff HEAD -- . ":(exclude)${VERDICT#"$fw"/}" ":(exclude)${VERDICT#"$fw"/}.*" > "$patch" 2>/dev/null
```

`${VERDICT#"$fw"/}` only strips a prefix when `$VERDICT` is inside the fence worktree. It never is at that
point: 400 lines earlier, `ship:1487-1490` unconditionally copies the verdict out of the worktree and
**repoints `VERDICT` at the copy**:

```sh
VERDICT_ARCHIVE="$RUNDIR/verdict-$ITER.md"
cp -p "$VERDICT" "$VERDICT_ARCHIVE" 2>/dev/null || echo "SHIP-VERDICT-ARCHIVE-FAILED …"
[ -s "$VERDICT_ARCHIVE" ] && VERDICT="$VERDICT_ARCHIVE"
```

So the pathspec is an absolute path outside the repository, and git refuses the whole command:

```
$ git diff HEAD -- . ":(exclude)/var/tmp/forge/elsewhere.md"
fatal: :(exclude)/var/tmp/forge/elsewhere.md: '/var/tmp/forge/elsewhere.md' is outside repository at '…'
rc=128
```

`2>/dev/null` swallows it, `$patch` is empty, and the run ends:

```
SHIP status=HOLD … attempts=1 fix_attempts=1 auto_closes=0 … reason=fix-patch-empty …
```

That is the observed line from the fixture run against the installed v3.10 bytes
(`/var/tmp/forge/ship-v3.10-fx/redteam/v310-shipv3-names.log`, row 1), together with
`fence_calls=3(want 2)` — both v3.10 changes visible in one row. **Every GO-WITH-FIX becomes a HOLD with an
empty patch.** The set's own witness for this class (`test/packet.py:69-71`) exercises
`lib/packet.py:patch_manifest` — the Python packet path — and never touches ship's bash capture, which is
why a green suite did not see it. This is the negative-pathspec-on-an-unmatched-path class exactly: a filter
that silently yields nothing, and nothing downstream asserts the diff is non-empty for a verdict that
proposed a patch.

**Ratchet:** compute the exclusion from a path known to be inside `$fw` (`VERDICT_SRC` is still in scope), or
exclude by basename with `:(exclude,glob)`; drop the `2>/dev/null` on a command whose failure is
indistinguishable from "no changes"; and assert `[ -s "$patch" ]` with a typed refusal that names the git
error when a fix block listed files.

---

## P1 — the write envelope is escapable, and the packet stays green

### P1-1 a builder plants `.git/config` `core.fsmonitor`; the verb's own post-builder `git` runs it outside Landlock

`:standard-anvil` promises "Write only the owner worktree, its Git metadata/object store, and private
client/temp directories; `~/bin` and packet evidence are read-only (Landlock)". The Landlock envelope itself
holds — measured from inside a stub builder:

```
 "~/bin/rt-pwned":                 PermissionError [Errno 13]
 "context-pack":                   PermissionError [Errno 13]
 "packet-dir/new-file":            PermissionError [Errno 13]
 "production ledger append":       PermissionError [Errno 13]
 "/var/tmp/forge/rt-outside-probe": PermissionError [Errno 13]
 "worktree/.git/hooks/post-commit": WROTE
 "worktree/.git/config append":     WROTE
```

The git-metadata write root is required for commits — but `.git/config` is executable surface. A builder that
appends

```
[core]
	fsmonitor = /path/to/payload
```

gets that payload executed by the **next ordinary git command in that worktree**, and `lib/packet.py`
runs several immediately after the builder exits (`patch_manifest`: `git diff --name-only`,
`git ls-files --others`). Executed end to end via `run build` (probe `b2_escape.py`):

```
run build rc= 0   {"run_id": "ea2cffe4-…"}
ESCAPED as forge pid=925147 ppid=925146 at 2026-09-11T19:19:12Z
  /var/tmp/forge/... WRITE SUCCEEDED
outside-landlock write: tampered
```

Four executions as the seat, arbitrary writes anywhere the seat can write (`~/bin` included), **and the
packet terminated `exit 0`**. `.git/hooks/*` is the same class for any later `commit`/`checkout`/`merge` a
human runs in that worktree.

**Ratchet:** run every seat-side git in a packet worktree with the execution surface disabled —
`git -c core.fsmonitor= -c core.hooksPath=/var/empty -c core.sshCommand= -c core.pager=cat …` (and
`GIT_CONFIG_NOSYSTEM=1`) — and hash `.git/config` + `.git/hooks` before and after the builder, refusing
typed on any change. A witness should plant the config and assert the payload never runs.

### P1-2 Landlock allows all reads: the builder can read the seat's subscription credentials

`landlock()` handles write-class access only (`access=sum(1<<i for i in [1,4..12])`), by design. From inside
the builder, `~/.claude/.credentials.json` and `/home/forge/bin/ship` both read successfully, and the network
is unrestricted. That is consistent with the code comment ("restrict filesystem mutation, allow all reads"),
but "private client state" in the profile text reads stronger than what is enforced. Name it in the profile,
or drop the word private.

---

## P2 — gates that a caller can turn off, and gates that verify nothing

### P2-1 the Andon freeze is bypassable by two environment variables

Both reproduced against the fixture-installed bytes with a real FREEZE in place
(`probes/d_andon.py`, stop `andon-20260911T000000Z-redteam`):

| attempt | result |
|---|---|
| `ship` / `land` / `round` under the freeze | **refused, naming the stop id** (rc 2/2/3) ✔ |
| freeze created *after* admission, before mutation | **refused** (rc 3, no mutation file) ✔ |
| `ANDON_ROOT=<empty dir> ship …` | **freeze not seen; ship proceeds** ✘ |
| `ANDON_ADMISSION=/bin/true ship …` | **freeze not seen; ship proceeds** ✘ |

`lib/andon.sh` takes `ANDON_ADMISSION` from the environment and `admission.py` takes `ANDON_ROOT`
(`/var/tmp/forge/andon` default). Compare `install.sh`, which *confines* its equivalent override:
`INSTALL_PROC_ROOT` is refused unless both it and `DEST` are under the fixture root. The Andon overrides
deserve the same confinement (allow a non-default `ANDON_ROOT` only when it is under
`/var/tmp/forge/ship-v3.10-fx`), otherwise the cord is advisory for exactly the caller it is meant to bind.
Two honest mitigations: the guard **fails closed** when `ANDON_ADMISSION` resolves to a missing file (rc≠0,
observed), and the freeze lane scoping is correct.

### P2-2 `andon-lift` accepts any non-blank evidence

`andon-lift <id> Gene x` → `ANDON-LIFTED … freeze-removed=true` (rc 0). Empty owner, empty evidence,
whitespace-only evidence and an unknown stop id are all correctly refused (rc 2/2/2/3, freeze retained).
"Backed by independent repair evidence" is therefore a free-text field, not a checked one. A cheap ratchet:
require the evidence to name an existing path or a ledger event id, and record the lift as a ledger event.

### P2-3 `receipt-chain` publishes to a remote and is not Andon-guarded

`receipt-chain:144` runs `git push -q origin "HEAD:$BR"`; `grep -n andon receipt-chain` → nothing. Executed
under the same freeze that stopped ship/land/round, `receipt-chain` does **not** refuse at admission
(rc=5 from its own later check; the stop id appears nowhere in its output). `records-push` (also a
publisher) likewise has no guard.

The set's own coverage witness is a **source-text scan** of two file names:

```python
for verb in ['ship','land']:
  … if ' push -q origin ' in line: assert 'andon_guard' in lines[i-1]
```

That is a marker-presence audit over a hand-listed scope: it cannot see `receipt-chain`, and it cannot see a
push spelled any other way. Either guard the third publisher or record, in writing, that receipt/records
publication is deliberately outside the freeze — and make the audit enumerate publishers by behaviour, not
by a two-name list.

### P2-4 the prewarm gate classifies **path names only**, and its receipt is unauthenticated

`lib/admission.py:prewarm` inputs are exactly: `git diff --name-only <base> <tip>` **path strings** plus the
brief's `diff-plan` list, matched against `catalog|registry|ops[_.-]|mcp.*(?:intent|tool)`. Never file
content, never the tree. Executed:

| attempt | result |
|---|---|
| commit adds `tool-catalog.edn` | **refused** with the exact repair command ✔ |
| the same catalog entry added *inside* `src/mcp_server.clj` | **admitted** (rc 0) ✘ |
| hand-written `SHIP_PREWARM_RECEIPT` with the right `git-head`/`git-tree`, `:stages [{:exit 0}]` | **admitted** (rc 0) ✘ |

The second is acknowledged in REPORT.md's least-sure #1. The third is not: the receipt is an unsigned EDN
file at a caller-chosen path with no binding to any execution record (no argv, no producer identity, no
ledger event), so the gate is satisfiable by `write_edn` — which is exactly how the set's own positive
control admits (`test/refusals.py:26-29`, and REPORT.md concedes "no real repository JVM prewarm was run
here"). **Ratchet:** require the receipt to carry, and `admission.py` to verify, a ledger `check` event id
whose payload hash matches, or content-address the receipt to the producer's argv+exit.

### P2-5 unknown brief keys are silently accepted — a misspelled `diff-plan` disables the gate

`resolve(…,'schema', all(k in b for k in [...]))` checks presence, never closure. A brief carrying
`this-key-does-not-exist` **and** `dif-plan: ["src/tool_catalog.clj"]` built green (rc 0) with no warning;
the misspelling silently removes the only caller-supplied input to the catalog classifier. Same class for a
typo'd `checks`, `skills`, `claims` or `runner`. Reject unknown top-level keys, or resolve them explicitly as
`ignored` rows in the rendered prompt.

### P2-6 numbered `order` deliverables are never verified

A brief with three numbered steps whose deliverables are `report.edn`, `NEVER-WRITTEN.md`, `ALSO-NEVER.clj`,
run by a builder that writes only the report:

```
rc= 0  terminal= {'exit_code': 0, 'status': 'completed'}
step-2 deliverable exists: False   step-3: False
```

Only `deliverable.report` is enforced. "`order` (numbered steps each with a deliverable path)" is rendered
prompt text, not a contract. One `os.path.exists` loop before `emit(terminal)` converts it into one — with
`report-invalid`'s sibling terminal (`deliverables-missing`), not a green.

---

## P3 — scheduling, staleness, and the board

### P3-1 the JVM queue has no deadline and no liveness check on the holder

`ship`'s queue and `lib/packet.py:acquire` both block on `flock` forever. Measured: with a live non-JVM
process holding the lease, `ship` printed `SHIP-QUEUED resource=jvm holder=rt-not-a-jvm-holder`, emitted the
`queued` event (correct, and the holder is named), and was **still waiting after 20s**; it proceeded only
when I killed the holder. A *dead* holder is not a problem — `flock` is released by the kernel — confirmed:
after `SIGKILL`ing the holder, `ship` ran with `queued=False` in 0.5s. But the lease fd is passed to the
builder (`pass_fds`), so an orphaned descendant can hold it with nobody alive to name; and nothing ever
turns a long wait into the `owed` obligation the resource contract was built for. Compare `records-push`,
which does check `kill -0` on the holder pids. Add a `budget.wait` deadline that converts to `owed`.

### P3-2 a descendant in its own session survives `fence-run stop`

Reproduced (`probes/a_fence.py`):

| attempt | result |
|---|---|
| reviewer installs `SIG_IGN` for TERM/INT/HUP | **killed in 0.3s** (SIGKILL), partial verdict archived ✔ |
| `fence-run stop` on a pid never owned | **refused**: `unknown owned reviewer pid; repair: use the START RECEIPT pid` ✔ |
| reviewer spawns a child with `start_new_session=True` | **child survives the stop** ✘ |

`supervise`/`stop` kill by process *group*; a descendant that calls `setsid` leaves that group. "never leaves
an orphan" is therefore true for the ordinary client and false in general. Either reap by the retained
descendant set (`descendants()` already computes it) or soften the claim.

### P3-3 `board`'s COVERAGE BY PRODUCER is a hardcoded literal

`lib/board.clj` renders coverage from an inline map, not from the events. On my ledger (producers actually
observed: `await/v3.9`, `run-bg/v3.9`) it printed `build executable-packet/v3.10`, `measure
packet-command/v3.10`, `ship orchestrated-path/v3.9`; on an **empty ledger** it printed the byte-identical
block. Under the plan of record ("coverage by producer is a field", "the ledger observes"), a present-tense
coverage claim that no event supports is the wrong kind of row. `OBSERVED PRODUCERS` (a real fold) is right
there and honest — derive coverage from it.

Everything else the board owes, it delivered. My four runs (`rt-fast`, `rt-dashdash`, `rt-missing`,
`rt-live`), with `rt-live`'s supervisor and child `SIGKILL`ed:

```
fast     -> TERMINAL     dashdash -> TERMINAL     missing -> TERMINAL     live -> UNKNOWN
await:   rc=3  reconcile {"state":"UNKNOWN","reason":"missing-terminal", …}
```

Default `board` hides them (`fixture rows hidden: 4`) because they are fixture events — correct, and stated
in the output; `board --all` shows all four under the right headings.

### P3-4 delta rounds: superseded IDs are enforced, stale *content* is not

| attempt | result |
|---|---|
| descendant subject, new claim id | old claim text absent from the prompt, `superseded-claim-ids` rendered ✔ |
| `--delta` onto an ancestor subject | refused: `subject lineage diverged; repair: supply a descendant sha` ✔ |
| same stale claim **text** under a **new claim id** | **rendered into the prompt** ✘ |
| stale evidence inside an `inputs` file | **carried verbatim into the context pack** the prompt orders the builder to read ✘ |

Staleness is keyed on the claim ID. "A delta round carries no stale claims" holds only for claims the author
chooses to keep the ID of; it is not a property of the packet the builder reads. Either diff the pack across
the lineage and render what changed, or state the claim as "no stale claim *ids*".

### P3-5 the installed ledger library hardcodes this version's staging fixture root

`lib/ledger.py:append` refuses a fixture ledger that is not under the literal
`/var/tmp/forge/ship-v3.10-fx` — a staging path, version-pinned, installed into `~/bin/lib`. v3.9's copy
carries `/var/tmp/forge/ship-v3.9-fx`. It fails closed, which is right, but it is a string the next staged
set must remember to bump, and it silently invalidates any attempt to run one version's bytes under another
version's fixture harness: my v3.9 control arm (below) died exactly there, with every `ship_emit` raising
and ship emitting no status line at all. A prefix check (`/var/tmp/forge/ship-*-fx`) or an env-declared
fixture root removes the trap.

---

## What held up (the strong rungs)

- **brief.edn pre-launch resolution** refuses cheaply and with a correct repair, before any model spend:
  branch-name subject (`repair: pass the full sha, never the branch name`), skill path that is a directory
  (`repair: use …/SKILL.md`), unknown `rules-profile`, zero-minute budget, `minutes` as a string, report path
  outside the owned roots, `SERVICE_PORT=7888`, `JAVA_TOOL_OPTIONS=-Xmx2048m`.
- **report.edn validation** is real: missing fields, a vector instead of a map, an empty file, a report
  written to the wrong path, a builder that exits 0 having written nothing, **and a measurement whose
  `receipt` path does not exist** all terminate `report-invalid` with `exit_code 3` — never green.
- **The context pack** is byte-identical across two packets of the same brief
  (`8256557ddae27aaf…` twice) and is kernel-protected from the builder.
- **Occupied branch** refuses before fetch/review with `repair=pass-the-sha`. A branch checked out in a
  *different clone* is not detected (scope is `git worktree list` of `GIT_REPO`) — correct scoping, since the
  sealed sha is checked out detached in this repo's own worktree; a tag naming the occupied tip is likewise
  admitted. Both are recorded below as attempted, not as defects.
- **Staged-set drift at install** is caught even when it happens during the copy of the **last** file:
  `INSTALL REFUSED reason=staged-set-drift repair=reseal-and-restart-install` (exit 2), before witnesses and
  before `INSTALL OK`. Caveat in P0-3: by then the destination is already fully replaced and the message
  does not say so.
- **Ledger isolation**: zero fixture events reached the production ledger, verified by event-id content
  before and after.
- **Compatibility**: the `RUN` line and JSON handle are byte-identical to staged v3.9 *and* to the installed
  v3.9 backup; `run-bg NAME CMD…` and `run-bg NAME -- CMD…` produce identical line shapes; `INSTALL OK v3.9 …`
  is unchanged (deliberate, documented); `andon-lift` and `records-push` grammars are unchanged or additive.
  The one regression is the removed `SHIP-REVIEW-LATE` token (P0-1).

---

## Bypass table

| # | Attempt | Result |
|---|---|---|
| 1 | Branch occupied in a worktree of a **different clone** | Not detected — out of scope by design (sealed sha is detached in this repo) |
| 2 | Tag naming an occupied branch tip | Admitted (symbolic-full-name is not `refs/heads/…`) |
| 3 | Catalog change hidden in `src/mcp_server.clj` | **Bypass** — classifier reads changed path names + `diff-plan` only |
| 4 | Misspelled `dif-plan` key in the brief | **Bypass** — unknown keys silently ignored |
| 5 | Hand-forged `SHIP_PREWARM_RECEIPT` | **Bypass** — receipt is unsigned, unbound to execution |
| 6 | Dead JVM-lease holder | No bypass and no hang — kernel releases `flock` (0.5s) |
| 7 | Live non-JVM lease holder | Unbounded wait, holder named, no `owed` |
| 8 | Drift between START hash and the **last** file copied | Refused (exit 2) — but destination already replaced |
| 9 | Reviewer ignoring SIGTERM/SIGINT/SIGHUP | Killed (SIGKILL) in 0.3s, verdict archived |
| 10 | Reviewer's descendant in its own session (`setsid`) | **Orphan survives the stop** |
| 11 | `fence-run stop <pid never owned>` | Refused with the repair line |
| 12 | `ANDON_ROOT` → empty directory | **Freeze bypassed** |
| 13 | `ANDON_ADMISSION=/bin/true` | **Freeze bypassed** |
| 14 | `andon_guard` with an unresolvable admission path | Fails closed (refuses) |
| 15 | Freeze created between admission and mutation | Refused before the mutation |
| 16 | `andon-lift` with empty / whitespace evidence, empty owner, unknown id | Refused (freeze retained) |
| 17 | `andon-lift <id> Gene x` | **Accepted** — evidence unvalidated |
| 18 | `receipt-chain` under the freeze | **Not guarded** (no `andon_guard` in the file) |
| 19 | Builder writes `~/bin`, the context pack, the packet dir, the production ledger, `/var/tmp/forge/…` | All denied by Landlock |
| 20 | Builder writes `.git/config` `core.fsmonitor` | **Escape** — executed as the seat by the verb's own `git`; packet still green |
| 21 | Builder writes `.git/hooks/post-commit` | Allowed (same class, fires on the next commit) |
| 22 | Builder reads `~/.claude/.credentials.json` | Allowed (Landlock handles writes only) |
| 23 | Stale claim text under a new claim id | **Rendered into the delta prompt** |
| 24 | Stale evidence in an `inputs` file | **Carried into the delta context pack** |
| 25 | `--delta` onto an ancestor subject | Refused (`lineage diverged`) |
| 26 | Builder skips numbered steps 2 and 3, writes a valid report | **Green** (`status: completed`) |
| 27 | Packet sealed with no `MANIFEST.*.sha256` present | **Empty one-shot closure, no refusal** |

## What I could not test

- **The real model round trip.** Every builder was a stub; I spent none of my two subscription launches. The
  set's own scoped-circuit transcripts (`claude-sonnet-5`, `gpt-6-astra`) are retained evidence I read but
  did not reproduce.
- **A real prewarm / battery / landing gate.** No repository JVM gate was run — the same limit REPORT.md
  states for itself. My prewarm probes exercise `admission.py` only.
- **Any real publication.** No remote push, no `land`, no `records-push` to a real remote; `receipt-chain`
  was exercised only to its first check.
- **A clean v3.9 baseline.** I ran `run-ship-v3` against `/var/tmp/forge/ship-v3.9` as a control; it is
  **invalid** and I am not counting it. v3.9's `lib/ledger.py` refuses any fixture ledger outside
  `/var/tmp/forge/ship-v3.9-fx`, while the v3.10 harness puts `CASE` under `…/ship-v3.10-fx` (P3-5), so every
  `ship_emit` raised and ship produced no SHIP line — 15 empty-status rows that say nothing about v3.9
  (`/var/tmp/forge/ship-v3.10-fx/redteam/v39-control-shipv3.log`). The evidence for P0-1 and P0-4 does not
  depend on a baseline: two independent installs produced identical counts, v3.9's `fence-run` provably has
  no `stop` subcommand, the failing rows name `fence_calls=3(want 2)` and `reason=fix-patch-empty`, and both
  causes are readable in the v3.9→v3.10 diff.
- **`round`** beyond its freeze refusal; **`land`** beyond its freeze refusal and the legacy fixtures.
- Whether the `.git/config` escape is reachable from the *real* clients' sandboxes (they are given
  `--dangerously-bypass-approvals-and-sandbox` / `bypassPermissions`, so I expect yes, but I did not spend a
  launch to prove it).

## Cleanup

Fixture destination `/var/tmp/forge/ship-v3.10-fx/redteam-dest` removed; every process I started was
waited on or killed by exact pid (including the one escaped `setsid` descendant I created in probe 10); no
`pgrep -f`, no `pkill`, no outer `timeout` on a child. Probe sources, logs and ledgers are retained under
`/var/tmp/forge/ship-v3.10-fx/redteam/` (`probes/`, `case-*/`, `install.log`, `shipv2-rerun.log`,
`ab-fixture.log`, `prod-baseline.json`). The staged set was not modified: it is byte-identical to its own
`MANIFEST.staged.sha256`.
