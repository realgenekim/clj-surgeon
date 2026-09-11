# Block 2 red-team, second pass — ship v3.10 staged set after Fix round 1

**VERDICT: GO-WITH-FIX for install on this seat.**

Every finding in the first report (`opus-redteam-report.md`, NO-GO) is repaired. I reintroduced each
defect into my own copy of the set, ran the fix round's *named* witness, and required it to go RED;
then ran it against the staged bytes and required GREEN; then retried the original bypass. **All 20
checked rows (P0-1…P3-5 plus the three summary items) are HELD. Nothing is NOT HELD.** Four are
HELD WITH A RESIDUAL — `P2-1` (a third, closely-related Andon-override escape remains open), `P3-5`
(the version-pinned staging literal was removed from one file of three), `S-2` (the prewarm receipt
now binds to a self-issued ledger event) and `S-3` (the publication-coverage *audit* is still a
source-text scan, though the behaviour it was meant to cover is now witnessed behaviourally).

The set now passes its own installer. Against my fixture destination
(`/var/tmp/forge/ship-v3.10-fx/redteam2/dest`), all **11 inherited suites returned `mismatches: 0`
(216 rows)**, the v3.9 witness suite passed against the installed bytes, and the installer printed its
`INSTALL OK v3.9` compatibility literal. The two headline P0s reproduce exactly as the first report
measured them and are gone from the fixed bytes:

| | original v3.10 defect, reintroduced | staged/installed v3.10 bytes |
|---|---|---|
| P0-1 `fence-run stop` (3 original call sites restored verbatim) | `run-ship-v2: 28 rows, mismatches: 1` — row `item2-cold-lease-is-taken-over: review-not-launched-after-takeover`; `run-ship-v3: 22 rows, mismatches: 2` | v2 **0/28**, v3 **0/22** |
| P0-4 absolute-path negative pathspec | `run-ship-v3: 22 rows, mismatches: 17`, every GO-WITH-FIX row, `reason=fix-patch-empty` | v3 **0/22**, row `1-GO-WITH-FIX-auto-closes-then-delta-GO-LANDS` green |

I spent **both** authorised subscription launches on the two scoped circuits against the current
staged bytes; both exited 0 and `test/circuits.py` now passes on them (receipts below). The stale
fix-round receipts were moved aside, not deleted
(`/var/tmp/forge/ship-v3.10-fx/redteam2/circuit-backup/`).

**Eleven new findings — three at P2, the rest P3 or informational — none of which makes v3.10 worse
than the v3.9 bytes installed today.** The three that should be fixed before the set is used for
real product packets are **N1** (the version-pinned staging literal survives into two *installed*
files, one of which is the default root for every production packet's evidence), **N2** (the Andon
override confinement is escapable by a caller that also declares `LEDGER_FIXTURE_ROOT`) and **N3**
(`INSTALL.PROVEN` has no consumer). None of them blocks the install itself.

**Operational carry-over, unchanged and still owed:** `/home/forge/src/clj-surgeon-receipt` is still
detached at `a15531ee` (`origin/MCP/main`), not on its owner's branch
`fable/cli-splice-classpath-r1` (`c74116ae`). That is the fix round's own isolation incident; I did
not touch it. `/home/forge/bin` is v3.9 and self-consistent
(`sha256sum --quiet -c MANIFEST.installed.sha256` → rc 0, 21 entries); `run` is absent; there is no
`INSTALL.PROVEN` file. The rollback the first report described was carried out.

Reviewer: Opus, forge@anvil, 2026-09-11. Everything below ran against a fixture install
(`SRC=/var/tmp/forge/ship-v3.10  DEST=/var/tmp/forge/ship-v3.10-fx/redteam2/dest`) or against
per-finding copies under `/var/tmp/forge/ship-v3.10-fx/redteam2/red/`. **The staged set was never
written to** — `sha256sum --quiet -c MANIFEST.staged.sha256` is clean before and after. Isolated
ledgers, `RECEIPT_WORKTREE`, `RECEIPT_LOG_ROOT`, `RECORDS_REPO`, `ANDON_ROOT` and `JVM_LEASE` for
every launch. Never `DEST=/home/forge/bin`; never the real receipt worktree; no servers; no forbidden
port; every process killed by exact pid.

---

## Finding table — first report's rows, re-checked

Method for each row: **(a)** reintroduce the defect in *my* copy and run the fix round's named
witness — require RED; **(b)** run the same witness on the staged bytes — require GREEN; **(c)** retry
the original bypass against the staged/installed bytes.

| # | Finding | (a) RED with defect | (b) GREEN as fixed | (c) original bypass retried | Verdict |
|---|---|---|---|---|---|
| P0-1 | `ship` calls a `fence-run stop` subcommand that does not exist | **yes**, two ways: call-site invariant (`red-P0-1.log`) and, with the **three original call sites restored verbatim from the retained original bytes**, `run-ship-v2: 1/28` on the exact row the first report named + `run-ship-v3: 2/22` (`red-P0-1c-*.log`) | yes — `fix1.py P0-1`; v2 0/28, v3 0/22 on installed bytes | `fence-run stop 12345` → `fence-run: REFUSED usage: fence-run <sha> <brief> <verdict>` (rc 2). `ship` calls `"$FENCE_RUN_BIN" "$CAND" "$CUR_BRIEF" "$CUR_NAME"` exactly once (ship:1379); zero `stop` calls remain | **HELD** |
| P0-2 | a reviewer that was deliberately never killed is now killed | **yes** ×2: replacing the never-kill line (`red-P0-2a.log`), and disabling the live-reviewer queue refusal (`red-P0-2b.log`) | yes — `fix1.py P0-2` | v3.9's 1,405-byte fast-lane-red block is present **byte-for-byte** in v3.10 (`late-verdict.sh`, `SHIP-REVIEW-LATE … informational=true`, "The reviewer … is NOT killed"). Token vocabulary vs v3.9 is now purely additive: `13a14 > SHIP-QUEUED`; `SHIP-REVIEW-LATE` restored. `ship:676` calls `admission.py reviewer` and stops `reason=reviewer-queued`, with admission printing `queued resource=reviewer holder_pid=<pid> run=<run>` | **HELD** |
| P0-3 | a failed install leaves the destination replaced, unproven, with a lying manifest | **yes** ×5: no `inventory-missing` refusal; no `closure-incomplete`; `PROVEN=false`+manifest moved after the renames; rollback not removing added files; rollback not removing `.incoming.*` (`red-P0-3a..e.log`) | yes — `fix1.py P0-3` | see the installer section below: a refused install now prints *"destination was already replaced; PROVEN=false; backups=…"*, rolls itself back, restores the prior bytes, removes added files and `INSTALL.PROVEN`; `--rollback <stamp>` works from the bare stamp and is idempotent; a missing fixture suite cannot be skipped; mid-copy staged drift refuses and rolls back | **HELD** (residual N3, N9) |
| P0-4 | the verdict-exclusion pathspec silently emptied every fix patch | **yes** — reverting to `":(exclude)${VERDICT#"$fw"/}"` with `2>/dev/null` reproduces `run-ship-v3: 17/22` and the exact line `reason=fix-patch-empty` (`red-P0-4.log`) | yes — `fix1.py P0-4` runs the real v3 suite: 22 rows, 0 mismatches | GO-WITH-FIX auto-close works on a real fix block: row `1-GO-WITH-FIX-auto-closes-then-delta-GO-LANDS` green on installed bytes; git stderr retained to `git-diff.stderr`; `fix-patch-git-error` is a typed HOLD | **HELD** (residual N7) |
| P1-1 | builder plants `.git/config core.fsmonitor`; the verb's own git executes it | **yes** — removing *both* layers (`GIT_FLAGS` and the metadata-hash refusal) reproduces the escape exactly: `AssertionError: fsmonitor executed outside Landlock` (`red-P1-1c.log`). Either layer alone blocks it (`red-P1-1a/b.log`) — defence in depth, each masking the other | yes — `fix1.py P1-1` | four tamper variants, all refused `rc=3` with `git-metadata-changed` and payload **not executed**: `.git/config core.fsmonitor`; `.git/hooks/post-commit`; `.git/config.worktree core.fsmonitor`; deleting `.git/hooks` entirely. Control (ordinary file write) still green rc 0 | **HELD** (residual N6) |
| P1-2 | profile text overclaims "private" client state | **yes** (`red-P1-2.log`) | yes | rendered profile now reads *"writes are confined by Landlock; reads and network are NOT restricted"*; the word `private` is gone from the rules | **HELD** |
| P2-1 | the Andon freeze is bypassable by two environment variables | **yes** — `overrides()` made a no-op (`red-P2-1.log`) | yes — `fix1.py P2-1` | `ANDON_ROOT=/var/tmp/forge/not-fixture` → `REFUSED reason=andon-override-outside-fixtures key=ANDON_ROOT` (rc 2). `ANDON_ADMISSION=/bin/true` → same, key=`ANDON_ADMISSION` (rc 2). **But** a caller that also sets `LEDGER_FIXTURE_ROOT` under the boundary escapes — see **N2** | **HELD WITH RESIDUAL** |
| P2-2 | `andon-lift` accepts any non-blank evidence | **yes** — but only with a *faithful* defect (`red-P2-2b.log`); removing the validation alone leaves the witness green because the code then crashes (see **N4**) | yes — `fix1.py P2-2` | `andon-lift s1 Gene x` → `ANDON-LIFT-REFUSED reason=evidence-invalid`, rc 2, **FREEZE retained**. An existing non-empty evidence file lifts and records a `check` event carrying the evidence digest | **HELD** (residual N4) |
| P2-3 | `receipt-chain` publishes to a remote and is not Andon-guarded | **yes** — removing the guards from both publishers (`red-P2-3.log`) | yes — `fix1.py P2-3` | under a freeze, fully confined: `receipt-chain` rc 2 and `records-push` rc 2, both printing `REFUSED reason=andon-freeze stop=rt2-stop lane=ship`. **All five** `git push` sites in the payload (`ship:1241`, `ship:1770`, `land:157`, `receipt-chain:149`, `records-push:33`) carry `andon_guard` on the preceding line. The real receipt worktree HEAD was `a15531ee` before and after | **HELD** (audit residual below) |
| P2-4 | prewarm classifies path names only; its receipt is unauthenticated | **yes** ×2: reverting `catalog_change` to path names (`red-P2-4a.log`) and dropping the ledger-event binding (`red-P2-4b.log`) | yes — `fix1.py P2-4` | catalog entry hidden inside `src/mcp_server.clj` (no catalog-ish path name) → `catalog_change() = True`, prewarm **refused**. Hand-forged `SHIP_PREWARM_RECEIPT` with correct `git-head`/`git-tree`/`stages` and no ledger event → **refused**. Editing the receipt bytes after a matching event → **refused**. Residual **N10** | **HELD** (residual N10) |
| P2-5 | unknown brief keys silently accepted | **yes** (`red-P2-5.log`) | yes | `dif-plan` → `BUILD REFUSED unknown-brief-key: dif-plan; repair: use the closed schemas/brief.edn keys`, rc 2, before any launch | **HELD** |
| P2-6 | numbered `order` deliverables never verified | **yes** — with the loop removed the run terminates `{'exit_code': 0, 'status': 'completed'}` with step 2 missing, reproducing bypass #26 exactly (`red-P2-6.log`) | yes | a valid report plus a missing step-2 deliverable terminates `deliverable-missing` naming the step | **HELD** |
| P3-1 | JVM queue has no deadline and no liveness check on the holder | **yes** ×2: removing the deadline (`queue exceeded packet budget deadline`, `red-P3-1a.log`); removing **both** dead-holder reconcile paths (`red-P3-1c.log`) | yes — `fix1.py P3-1` | a live holder now yields `owed` and `Refused: jvm-queue-deadline holder=…` inside the packet's budget; a dead recorded owner holding an inherited descriptor is reconciled `reason=dead-holder state=lease-broken`; packet children no longer inherit the lease fd | **HELD** |
| P3-2 | a descendant in its own session survives the stop | **yes** — removing subreaper custody + descendant reaping (`red-P3-2.log`) | yes — `fix1.py P3-2` | there is **no public stop verb** any more; the supervisor is a Linux subreaper, waits for reviewer exit, then reaps `setsid` descendants and archives the verdict. `fence-run stop <pid>` refuses on arity | **HELD** (N5, N11) |
| P3-3 | `board`'s COVERAGE BY PRODUCER is a hardcoded literal | **yes** — re-hardcoding the line (`red-P3-3.log`) | yes — `fix1.py P3-3` | my own independent probe: on an empty ledger all 16 inventory verbs print `unobserved`; after appending two producers (`receipt-chain/v9.9`, `measure/v9.9`) **exactly those two rows change** and `OBSERVED PRODUCERS` lists them. The section also names its inventory source | **HELD** |
| P3-4 | delta rounds: superseded IDs enforced, stale *content* is not | **yes** — removing the `stale-content` refusal (`red-P3-4.log`) | yes — `fix1.py P3-4` | stale claim text under a **new** claim id **with changed evidence** → `BUILD REFUSED stale-content evidence=…` (rc 2). Stale evidence inside an `inputs` file → `BUILD REFUSED stale-content evidence=<inputfile>` (bypass #24 closed). Re-asserting a claim under a new id whose evidence is **unchanged** is still admitted — correct under the restated rule (staleness is now keyed on evidence bytes, not on the claim id) | **HELD** |
| P3-5 | installed ledger library hardcodes this version's staging fixture root | **yes** (`red-P3-5.log`) | yes | `lib/ledger.py` takes the root from `LEDGER_FIXTURE_ROOT` or the packet manifest's `fixture-root`, and refuses `fixture-root-missing` when neither is present. **But the same literal survives in two other installed files** — see **N1** | **HELD WITH RESIDUAL** |
| S-1 | receipt-chain push without an andon guard | covered by P2-3 | covered by P2-3 | closed — both publishers refuse under a freeze | **HELD** |
| S-2 | prewarm gate reading path names with an unsigned receipt | covered by P2-4 | covered by P2-4 | closed for the file; residual **N10** for the event | **HELD WITH RESIDUAL** |
| S-3 | coverage witness as a source-text scan | n/a | n/a | the audit (`test/refusals.py:92-97`) is **still** a source-text, spelling-based scan, now over a hand-list of four verbs. Demonstrated blind: respelling `git push -q origin` to `git push --quiet origin` in `receipt-chain` and deleting both its guards leaves the audit **GREEN**. In substance the class is closed by the *behavioural* `fix1 P2-3` witness (both publishers run under a real freeze) and by the fact that all five push sites are guarded — but a **new** publisher added later would be caught by neither | **HELD WITH RESIDUAL** |

---

## The installer against my fixture destination

```
env -u FIXTURE_ENV_READY -u INSTALL_PROC_ROOT \
  SRC=/var/tmp/forge/ship-v3.10 DEST=/var/tmp/forge/ship-v3.10-fx/redteam2/dest \
  FIXTURES=/var/tmp/forge/ship-v3.10-fx/redteam2/fixtures \
  INSTALL_BACKUP_ROOT=/var/tmp/forge/ship-v3.10-fx/backups/rt2-final-… \
  bash /var/tmp/forge/ship-v3.10/install.sh
```

**Result: `INSTALL OK v3.9 stamp=20260911T202817Z` — the set passes its own installer.**

All 11 inherited suites against the **installed** bytes, **216 rows, 0 mismatches**:

```
   run-land-auto:               19 rows, mismatches: 0
   run-land-publication-truth:  12 rows, mismatches: 0
   run-ship-v2:                 28 rows, mismatches: 0
   run-ship-v3:                 22 rows, mismatches: 0
   run-ship-v3.1:               13 rows, mismatches: 0
   run-ship-v3.2:                8 rows, mismatches: 0
   run-ship-v3.3:               13 rows, mismatches: 0
   run-ship-v3.4:               14 rows, mismatches: 0
   run-ship-v3.5:                5 rows, mismatches: 0
   run-ship-v3.6:                9 rows, mismatches: 0
   run-ship-v3.7:               73 rows, mismatches: 0
```

Then the v3.9 witness suite against the installed bytes: **79 `PASS` lines**, ending
`PASS all witnesses (artifacts: /var/tmp/forge/ship-v3.10-fx/witness.CILsh4uh)` — including all 16
`fix1` finding checks and `PASS scoped-circuit retained proof` on my two fresh receipts. The four
`FAIL production ledger content: fixture event in production: …` lines in the transcript are the
set's **deliberate red controls** over a scratch copy of the ledger; their enclosing witness passes
(`PASS content oracle: misdirected launcher RED, restored GREEN …`).

Destination state afterwards:

```
$ cat dest/INSTALL.PROVEN
PROVEN=true stamp=20260911T202817Z backup=/var/tmp/forge/ship-v3.10-fx/backups/rt2-final-20260911T202817Z
$ wc -l < dest/MANIFEST.installed.sha256          -> 34
$ (cd dest && sha256sum --quiet -c MANIFEST.installed.sha256)                  -> rc 0
$ (cd dest && sha256sum --quiet -c /var/tmp/forge/ship-v3.10/MANIFEST.staged.sha256) -> rc 0
```

Worth recording for the real install: **the v3.10 code will not seal a packet on today's `~/bin`.**
Pointing `packet.ROOT` at `/home/forge/bin` (read only) and calling `seal_manifest` gives
`closure-incomplete omitted=andon-lift,andon-pull,independent-review,lib/admission.py,lib/andon.sh,lib/edn.clj,lib/fence.py,lib/packet.py,measure,round,run,schemas/brief.edn,schemas/report.edn`
— the v3.9 manifest there describes 21 files while `~/bin` actually holds more. It fails closed, which
is right; it also means `run build` is unusable until v3.10 is installed, and that the install replaces
a manifest that under-describes its own destination with one that does not.

Separately exercised, each in its own fixture destination:

| probe | command | result |
|---|---|---|
| refused install → automatic rollback | a fixture suite reporting `mismatches: 1` | `INSTALL NOT PROVEN` (rc 3) → `INSTALL REFUSED destination was already replaced; PROVEN=false; backups=…; rolling back including added files` → `ROLLBACK OK … added-files-removed=true`; prior `ship` bytes and prior `MANIFEST.installed.sha256` restored; `INSTALL.PROVEN` gone |
| `--rollback <stamp>` | `bash install.sh --rollback rt2-20260911T202724Z` (bare stamp → `/var/tmp/forge/ship-v3.10-fx/backups/`) | `ROLLBACK OK … added-files-removed=true` (rc 0), idempotent on replay |
| missing fixture suite | `run-ship-v3.7.sh` absent | `INSTALL NOT PROVEN reason=fixture-inventory-missing suite=run-ship-v3.7` → rollback. Not silently skipped |
| staged-set drift mid-copy | `mv` shim mutates `$SRC/measure` after the 10th rename | `INSTALL REFUSED reason=staged-set-drift repair=reseal-and-restart-install` (rc 2), **then** "destination was already replaced … backups=…", then `ROLLBACK OK`; prior bytes restored |
| `INSTALL_PROC_ROOT` confinement | `DEST=/home/forge/bin-nope` with a fixture proc root | `INSTALL REFUSED reason=fixture-proc-requires-fixture-destination`; `/home/forge/bin-nope` never created |
| `INSTALL_PROC_ROOT` confinement | fixture `DEST`, `INSTALL_PROC_ROOT=/var/tmp/forge/elsewhere` | `INSTALL REFUSED reason=fixture-proc-outside-fixtures` |
| `seal_manifest` inventory | no `MANIFEST.*` present | `Refused: inventory-missing; repair: install a sealed one-shot inventory` |
| `seal_manifest` closure | complete inventory minus `measure` | `Refused: closure-incomplete omitted=measure` |
| `seal_manifest` closure | `measure` in the inventory but absent on disk | `Refused: closure-incomplete missing=<path>/measure` |

---

## The two scoped circuits (both authorised launches spent)

Run against the **current** staged bytes with `RUN_SCOPED_CIRCUITS=1`; the fix round's stale receipts
were moved to `/var/tmp/forge/ship-v3.10-fx/redteam2/circuit-backup/` first. Both exited 0 and
`test/circuits.py` now prints
`PASS scoped-circuit retained proof: fresh Claude and Codex, exact prompt/pack/manifest/report hashes, successful terminal`.

### claude-sonnet-5
- packet `ed5c55bb-2927-45b2-a0b1-35ae9d7e71df`, exit 0, launcher wall **21.855 s**
- report `…/fix1-scoped-circuits/claude-a14ce5d4-9da6-46f6-b3cc-93327a5206fa/owner/report.edn`
- **report SHA256** `e06cd0d59659d2efce461b56329bb71c2f0237f689c4492830ac52e2eccac7d6`
- **manifest_id / manifest.edn SHA256** `92a7649b58368d5baacf780fdb1c8eff588306c7d11360b1dfc347ebb68302e3`
- prompt.md SHA256 `886aabbe90e853741e0b730dcde1d0c466304e1c5c7b0be12e0c801ae84a5614`
- context-pack.md SHA256 `1d7f72be24ee9724b6253e5fb51dea2701b89565fa6bc7247b181da7d07edef3`

### gpt-6-astra
- packet `49df4e17-c8f7-4d74-8f0f-cf57bcf7bafc`, exit 0, launcher wall **29.234 s**
- report `…/fix1-scoped-circuits/codex-b2864fed-6ded-4cac-9af4-ef3694ca502e/owner/report.edn`
- **report SHA256** `9428dd3c473ac5d5a2fa5e98221f2667727c1b353014db05edf274fced136c72`
- **manifest_id / manifest.edn SHA256** `d2484879e7638f97cfccb35c4f36efea62ab1a5a252853fa480aedd88c0d5c57`
- prompt.md SHA256 `a31b6aa4c6a2557e1b7f2934e3b6ab30763aa3386822752f9cff8cd1741012f4`
- context-pack.md SHA256 `666db9210f50c59fb1ca7eb5a18f69166f3d5ccdeb6a3c2f45f73c1a00e3dc45`

`index.json` SHA256 `0bfa791645f1ae0177a8cf9ba9f3efa8039c484941d23cb0eb8fcf8af645df0b`
(`/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/index.json`).

**Caveat worth stating:** these packets were sealed with `ROOT=/var/tmp/forge/ship-v3.10`, so their
one-shot closure binds the **staged** paths. They verify from any destination, which is convenient —
but it means the circuit receipt proves the staged bytes, not the installed ones. The installer
closes that gap separately by re-checking `sha256sum -c MANIFEST.staged.sha256` inside `$DEST`.

---

## New findings

### N1 — P2. The version-pinned staging literal that P3-5 removed survives in two *installed* files, one of them the default root for every production packet

`lib/ledger.py` was cleaned. Two other payload files still carry `/var/tmp/forge/ship-v3.10-fx`, and
both are in `FILES`, so both land in `~/bin`:

```
$ grep -Hn 'ship-v3\.10-fx' $(cat FILES)
lib/admission.py:34: boundary=pathlib.Path('/var/tmp/forge/ship-v3.10-fx').resolve()
lib/packet.py:8:PROFILE={'temp-root':'/var/tmp/forge/ship-v3.10-fx','xmx-mb':1024,
```

`packet.py:8` is the consequential one. `packet_root()` is
`os.environ.get('PACKET_ROOT', PROFILE['temp-root']+'/packets')`, and `temp-root` is also the parent
of every packet's `tmp/`, `codex-home/` and `claude-home/` (all Landlock write roots). **After
install, every real `run build` writes its context pack, prompt, `manifest.edn`, content-addressed
manifests and builder output into `/var/tmp/forge/ship-v3.10-fx/packets/` — a directory named after a
*staged set*, which REPORT.md itself describes as the fixture area and which an operator will
reasonably delete once the install is done.** Deleting it destroys live packet evidence; and
`verify_manifest` then refuses `stale-session` for every packet that referenced it.

`admission.py:34` is the second: the Andon override confinement boundary is pinned to the same
version-named directory, so v3.11's fixture root will not be trusted and v3.10's will be trusted
forever.

**Repair:** derive both from a version-independent root (`/var/tmp/forge/ship/packets` for
`temp-root`; `/var/tmp/forge/ship-*-fx` or an installed config value for the boundary), and add a
witness that greps the installed payload for any `ship-v3.*-fx` literal — the same shape as the
existing `fix1.py P3-5` assertion, widened from one file to the whole `FILES` list.

### N2 — P2. The Andon override confinement is escapable by a caller that also declares `LEDGER_FIXTURE_ROOT`

`admission.py:overrides()` permits a non-default `ANDON_ROOT`/`ANDON_ADMISSION` whenever
`LEDGER_FIXTURE_ROOT` **and** the override value both resolve under the boundary. `LEDGER_FIXTURE_ROOT`
is an ordinary environment variable, and — crucially — **`LEDGER_FIXTURE=true` is not required**, so
the run's own ledger events are still written with `fixture: false`. A caller declares itself a
fixture for the purpose of the cord while remaining a production run for the purpose of the record.

Reproduced end to end through a real publisher (freeze present in the root that holds it):

```
$ env ANDON_ROOT=$D/andon LEDGER_FIXTURE_ROOT=/var/tmp/forge/ship-v3.10-fx records-push
REFUSED reason=andon-freeze stop=rt2-stop lane=ship; repair: …            rc=2

$ env ANDON_ROOT=$D/empty-andon LEDGER_FIXTURE_ROOT=/var/tmp/forge/ship-v3.10-fx records-push
RECORDS-PUSH REFUSED reason=rebase-failed                                  rc=3   <-- past the cord

$ env ANDON_ROOT=$D/andon ANDON_ADMISSION=$RT/noop-admission.py \
      LEDGER_FIXTURE_ROOT=/var/tmp/forge/ship-v3.10-fx records-push
RECORDS-PUSH REFUSED reason=rebase-failed                                  rc=3   <-- past the cord
```

(the `rebase-failed` is my empty fixture records repo; the point is that the freeze was never seen.)

**This is still strictly better than what is installed today** — `/home/forge/bin/lib/andon.sh` is
`ANDON_ADMISSION=${ANDON_ADMISSION:-…}` with no check at all — so it does not block the install. But
per the standing rule that a gate a caller can turn off is not a gate, it should be closed before the
set is relied on: require `LEDGER_FIXTURE=true` **and** a fixture-root declaration that the process
did not write itself, or take the override only from the packet manifest.

### N3 — P2. `INSTALL.PROVEN` has no consumer

The fix round added the marker; nothing in the payload reads it. `grep -rn 'INSTALL.PROVEN'` over the
set hits only `install.sh` (which writes it), `BRIEF-fix1.md` and `REPORT.md`. `seal_manifest`, `run`,
`ship`, `land` and `board` all ignore it. In the normal failure path the automatic rollback removes the
marker, so this only bites on the second-order path `install.sh` itself names —
`INSTALL REFUSED reason=rollback-failed` (exit 4) — which leaves a destination that is replaced,
marked `PROVEN=false`, and fully usable. **Repair:** one line in `seal_manifest` (and in `ship`'s
admission) refusing typed when `ROOT/INSTALL.PROVEN` exists and does not say `PROVEN=true`.

### N4 — P3. `fix1.py P2-2`'s witness asserts only a non-zero exit, so a crash satisfies it

```python
r=sh(str(ROOT/'andon-lift'),stop,'Gene','x'); assert r.returncode!=0 and (sd/'FREEZE').exists()
```

Measured: deleting only the evidence-validation `if` leaves the witness **green**, because the code
then raises `IndexError: list index out of range` on `es[0]['checksum']` and the `|| exit 2` still
fires. I had to also repair the crash before the witness would go RED (`red-P2-2b.log`). A refusal
witness should assert its typed reason (`reason=evidence-invalid`), not merely that something failed.
Same shape in `fix1.py P2-3` (asserts the stop id appears — that one is fine) and `P2-1` (asserts
`andon-override` — fine).

### N5 — P3. `fence-run` emits a blank refusal on a four-argument call

`fence-run stop 12345 extra` → `fence-run: REFUSED ` (rc 2) with no reason and no repair: `argv`
length is 4, so the usage check passes and a bare `assert pathlib.Path(brief).is_file()` raises with
an empty message. Give the assertion a message.

### N6 — P3. `git-metadata-changed` does not name what changed

`emit(c,'refusal',{'reason':'git-metadata-changed','repair':'restore trusted Git config/hooks before reuse'})`
— no path, no before/after digest. A builder that legitimately runs `git config --local user.email …`
is refused with nothing to diagnose. Per the receipt rule, the refusal should name its subject:
include the changed paths from the `git_metadata` diff.

### N7 — P3. The fix-patch exclusion is by **basename**, so an unrelated proposed file with the same name is silently dropped

`verdict_base=$(basename "${VERDICT_SRC:-$VERDICT}")` then `":(exclude,glob)**/$verdict_base"`.
Verified semantics:

```
$ git diff HEAD --name-only -- . ':(exclude,glob)**/v1.md'
f.txt                      # docs/observations/v1.md excluded (correct)
                           # root-level v1.md excluded too (correct)
                           # src/v1.md ALSO DROPPED  <-- collision
```

The verdict name is caller-supplied (`$NAME`, `$NAME-delta-$n`), so a fix block proposing a file whose
basename matches loses it from the patch — and because the patch is otherwise non-empty, the new
`[ -s "$patch" ]` guard does not fire. **Repair:** exclude the verdict's repo-relative path
(`VERDICT_SRC` is in scope and is inside `$fw`), not its basename.

### N8 — P3, inherited from v3.9, not a v3.10 regression. A lane-scoped freeze is dodged by `SHIP_DEST`

`andon_guard` builds its scope as `clj-surgeon ${SHIP_DEST:-${LAND_DEST:-MCP/main}}`, and
`freeze()` matches a `lane=` that is not `ship`/`tighten one-shots` only by prefix against that scope.
`andon-pull` writes `lane=$SURFACE`, free text.

```
freeze lane="clj-surgeon MCP/main", default SHIP_DEST     -> REFUSED rc=2
freeze lane="clj-surgeon MCP/main", SHIP_DEST=some/other  -> rc=0
```

The `freeze()` body is byte-identical to `/home/forge/bin/lib/admission.py`, so this is pre-existing.
Recorded so it is not mistaken for something v3.10 introduced.

### N9 — P3, cosmetic. Rollback leaves empty `lib/` and `schemas/` directories

`rollback()` unlinks added files but not the directories it created, so a destination that had no
`lib/` is left with an empty one. Harmless; mentioned because "restores the prior state" is the claim.

### N10 — P3, conceded in REPORT.md. Prewarm provenance binds to a self-issued ledger event

The forged-receipt bypass is closed: a receipt file alone no longer admits, and editing the receipt
after a matching event refuses. But the binding is to a `check` event in the same ledger the caller
writes. Forging the event admits:

```
--- forge a check EVENT by hand in the fixture ledger ---
   RESULT: ADMITTED
```

REPORT.md's Limits section says exactly this ("local execution evidence, not cryptographic
independent acceptance"). It raises the cost of the forgery from one artefact to two; it does not
create a causal binding to an execution.

### N11 — informational. `fence-run supervise <config.json> <fd>` is reachable from the CLI

`if sys.argv[1]=='supervise'` is checked before the arity guard, so any caller can invoke the
supervisor directly with an arbitrary config (choosing `worktree`, `verdict`, `log`, `archive`) and
`os.close(int(sys.argv[3]))` on an arbitrary descriptor. Same trust domain as the caller, so no
privilege boundary is crossed — but it is an undocumented verb on a public one-shot.

---

## Bypass table (second pass)

| # | Attempt | First pass | This pass |
|---|---|---|---|
| 1 | `fence-run stop <pid>` | (new contract, was the P0-1 cause) | **Refused** — `usage: fence-run <sha> <brief> <verdict>` |
| 2 | Catalog change hidden in `src/mcp_server.clj` | Bypass | **Refused** — `catalog_change()` reads changed content |
| 3 | Misspelled `dif-plan` key | Bypass | **Refused** — `unknown-brief-key` |
| 4 | Hand-forged `SHIP_PREWARM_RECEIPT` | Bypass | **Refused** — `prewarm-required` |
| 5 | Matching ledger event, then edit the receipt bytes | (n/a) | **Refused** |
| 6 | Forge the prewarm `check` **event** in the ledger | (n/a) | **Admitted** — N10, conceded |
| 7 | `ANDON_ROOT` → unconfined directory | Bypass | **Refused** — `andon-override-outside-fixtures` |
| 8 | `ANDON_ADMISSION=/bin/true` | Bypass | **Refused** — same |
| 9 | `ANDON_ROOT`=confined empty dir **+** `LEDGER_FIXTURE_ROOT` | (n/a) | **Bypass** — N2 |
| 10 | `ANDON_ADMISSION`=confined no-op **+** `LEDGER_FIXTURE_ROOT` | (n/a) | **Bypass** — N2 |
| 11 | `ANDON_ADMISSION` → confined path that does not exist | Fails closed | **Fails closed** (rc 2) |
| 12 | `SHIP_DEST` dodges a lane-scoped freeze | not tested | **Bypass** — N8, inherited from v3.9 |
| 13 | `andon-lift <id> Gene x` | Accepted | **Refused**, FREEZE retained |
| 14 | `receipt-chain` under the freeze | Not guarded | **Refused**, names the stop |
| 15 | `records-push` under the freeze | Not guarded | **Refused**, names the stop |
| 16 | Respelled unguarded push (`push --quiet origin`) vs the source audit | (the audit was the finding) | **Audit blind** — S-3 residual; behavioural witness would catch it |
| 17 | Builder plants `.git/config core.fsmonitor` | Escape, packet green | **Refused** `git-metadata-changed`, payload not executed |
| 18 | Builder plants `.git/hooks/post-commit` | Allowed | **Refused**, payload not executed |
| 19 | Builder plants `.git/config.worktree core.fsmonitor` | not tested | **Refused** |
| 20 | Builder deletes `.git/hooks` | not tested | **Refused** |
| 21 | Builder writes an ordinary file (control) | — | **Green** rc 0 |
| 22 | Builder skips numbered steps 2 and 3 | Green | **Refused** `deliverable-missing` naming the step |
| 23 | Packet sealed with no `MANIFEST.*` present | Empty closure, no refusal | **Refused** `inventory-missing` |
| 24 | Packet sealed with a one-shot missing from the inventory | (n/a) | **Refused** `closure-incomplete omitted=…` |
| 25 | Stale claim text, new claim id, **changed** evidence | Rendered | **Refused** `stale-content` |
| 26 | Stale claim text, new claim id, **unchanged** evidence | Rendered | Admitted — correct under the restated rule |
| 27 | Stale evidence inside an `inputs` file | Carried into the pack | **Refused** `stale-content` |
| 28 | Live non-JVM lease holder | Unbounded wait | **`owed` + `jvm-queue-deadline`** inside the budget |
| 29 | Dead recorded owner holding an inherited locked descriptor | (n/a) | **Reconciled** `reason=dead-holder state=lease-broken` |
| 30 | Reviewer's `setsid` descendant | Orphan survives | **Reaped** after reviewer exit; verdict archived |
| 31 | Drift between START hash and a mid-loop file copy | Refused, destination silently replaced | **Refused, says so, rolls back** |
| 32 | Fixture proc root with a non-fixture destination | (installer confinement) | **Refused** `fixture-proc-requires-fixture-destination` |
| 33 | Missing fixture suite in the inventory | (n/a) | **Refused** `fixture-inventory-missing` |
| 34 | Builder reads `~/.claude/.credentials.json` | Allowed | Allowed — now stated honestly in the profile |
| 35 | Branch occupied in a worktree of a different clone | Not detected (by design) | Unchanged |
| 36 | Tag naming an occupied branch tip | Admitted (by design) | Unchanged |

---

## Isolation

Production-ledger isolation verified **by content**, not by producer name:

```
witness journals written during this pass : 75
witness event IDs recorded                : 7280
of those present in the production ledger : 0
production ledger: baseline 2688 -> 2715 (+27)
new production producers                  : {'run-bg/v3.9': 27}
new production run_ids                    : ['8f83b5ae-6535-4733-aee6-aa3edca40d30']
new production events marked fixture      : False
```

The 27 new production events are one unrelated live `run-bg/v3.9` run that was already in flight. The
only file under `/var/tmp/forge/ship-v3.10-fx` containing production event IDs is
`witness.CILsh4uh/production-content-controls/production.jsonl` — the harness's own read-only copy of
the production ledger, made by the installer's deliberate red control; it is excluded above and
contains 2,713 IDs, all of them reads.

The staged set was never written to: `sha256sum --quiet -c MANIFEST.staged.sha256` is clean before and
after this pass. `/home/forge/bin` was never a destination and is byte-identical to
`/var/tmp/forge/ship-v3.9` for `ship`. `/home/forge/src/clj-surgeon-receipt` HEAD was `a15531ee`
before my `receipt-chain` probes and `a15531ee` after; every `receipt-chain`/`records-push` launch
supplied `RECEIPT_WORKTREE`, `RECEIPT_LOG_ROOT`, `RECORDS_REPO` and `RECORDS_PUSH_LEASE` under my
fixture root. No server was started; no port in 7888/7890/7894/7895/8300–8339 was contacted. Every
process I created exited on its own or was killed by exact pid.

## What I could not test

- **Real publication.** No remote push, no real `land`, no real `records-push` to a real remote;
  `receipt-chain` was exercised only to its admission guard.
- **A real prewarm / battery / landing-gate JVM run.** My prewarm probes exercise `admission.py`
  only — the same limit the fix round states for itself.
- **A faithful behavioural RED for P2-1's escape at `ship`'s own admission.** I refused to plant a
  FREEZE in the real `/var/tmp/forge/andon`, so N2 is demonstrated through `andon_guard` and
  `records-push` with a freeze in a confined root. The code path is identical (`freeze()` globs
  `$ANDON_ROOT/*/FREEZE`), so the conclusion carries.
- **Whether the packet escape is reachable from the *real* clients' sandboxes.** Both are launched
  with `--dangerously-bypass-approvals-and-sandbox` / `bypassPermissions`, so I expect yes; I spent my
  two launches on the circuits, not on an escape probe.
- **`round` and `land` beyond their freeze refusals** and the legacy fixtures.
- **Independent acceptance of the model reports themselves.** The circuits prove the packet round trip
  and the hash chain, not the quality of what the two models wrote.

## Retained evidence

`/var/tmp/forge/ship-v3.10-fx/redteam2/`:
`logs/` (every RED and GREEN transcript, the installer log, the circuits log), `probes/`
(`redrun.sh`, `p-*.sh` defect patches, `bypass-a.py`, `bypass-b.py`, `bypass-c.py`, `isolation.py`),
`circuit-receipts.txt`, `circuit-backup/` (the fix round's superseded receipts),
`prod-baseline.json`. The per-finding mutated copies under `red/` and my fixture `dest/` are removed
(see Cleanup).

## Cleanup

Removed: my fixture destination `/var/tmp/forge/ship-v3.10-fx/redteam2/dest`, the 25 per-finding
mutated copies under `redteam2/red/`, `redteam2/src-pristine`, the secondary install probes
(`rollback-probe`, `missing-fx`, `drift`, `p21-e2e`, `lane`, `p23`) and their backup stamps under
`/var/tmp/forge/ship-v3.10-fx/backups/rt2-*`, and the `redteam2-bypass{A,B,C}-*` probe directories.
Retained: `redteam2/logs/`, `redteam2/probes/`, `redteam2/circuit-receipts.txt`,
`redteam2/circuit-backup/`, `redteam2/isolation-final.txt`, `redteam2/prod-baseline.json`, and the
`fix1-*` / `witness.*` artefact directories the shipped harness creates for itself.

Every process I started exited or was killed by exact pid, including five `pgrep -f`-matching waiter
loops of my own that were self-matching through the harness's `bash -c` argv (the
waiters-bind-to-pid-not-argv failure, reproduced on myself). No server, no forbidden port, no
`pkill`, no outer `timeout` left running on a child.

Final state: the staged set is byte-identical to `MANIFEST.staged.sha256`; `/home/forge/bin/ship` is
still `d3926a31…` (= `/var/tmp/forge/ship-v3.9/ship`) and `/home/forge/bin/run` does not exist;
`/home/forge/src/clj-surgeon-receipt` is at `a15531ee` as I found it; no process of mine remains.
