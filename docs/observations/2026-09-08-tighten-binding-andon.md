# tighten — the seat binding and the one-command Andon cord (forge@anvil, 2026-09-08)

Closes two of the seven NO-GO conditions Sol set on the `tighten-the-loop` skill
(`/var/tmp/forge/plan2/cellC/sol-tighten-review.md`, §4–5 and edit 7):

* **(1) a concrete retrievable binding** — `tighten binding` / `tighten verify-binding`
* **(4) the one-command Andon path with delivery and fail-closed resume** — `andon-pull` / `andon-lift`

Both were `contract-only, not available on this seat` in SKILL.md's inventory. They are now
installed, exercised by an executable fixture, and — for Andon — proved end to end against the real
durable channel with a real item id.

Companion: `2026-09-08-tighten-one-shots.md` (the five earlier one-shots). Scope discipline is the
same: what is written here is what this seat actually runs, and every gap is named rather than
implied.

## What is installed

| entrance | receipt line | refusal lines |
|---|---|---|
| `/home/forge/bin/tighten binding` | `BINDING-WRITTEN epoch=… file=… body_sha256=… commands_installed=10 commands_contract_only=2 current=…` | `BINDING-EXISTS epoch=… ` (exit 2), `BINDING-REFUSED reason=…` |
| `/home/forge/bin/tighten verify-binding` | `BINDING-OK epoch=… commands=10 file=…` (exit 0) | `BINDING-DRIFT <name> bound=<sha> installed=<sha> path=…` + `BINDING-DRIFTED …` (exit 2); `BINDING-EPOCH-MISMATCH …`; `BINDING-BODY-MODIFIED …`; `BINDING-ABSENT next: tighten binding` (exit 3) |
| `/home/forge/bin/andon-pull "<what stopped>" "<affected surface>" "<evidence>"` | `ANDON-STOP-RECORDED …` → `ANDON-FREEZE lane=…` → `ANDON-DELIVERY delivery=… inbox=…` → `ANDON-PULLED id=… inbox=… freeze=…` (always exit 0 once a pull is accepted) | `ANDON-REFUSED reason=empty-field field=what-stopped\|affected-surface\|evidence` and `reason=missing-arguments got=N need=3` (exit 2) |
| `/home/forge/bin/andon-lift <id> "<owner>" "<evidence>"` | `ANDON-LIFTED id=… owner=… lane=… freeze-removed=… record=…` | `ANDON-LIFT-REFUSED reason=empty-field\|missing-arguments` (2), `reason=unknown-stop` (3), `reason=not-frozen` (4), `reason=cannot-remove-freeze` (5) |
| `/home/forge/bin/tighten status` | now also prints a `-- binding` block (verify-binding's own lines plus an `on_loop reason:` line on drift/absence) and an `-- andon` block listing every OPEN scoped freeze | — |

Installed bytes at the time of writing:

```
6b5c7ead7cd6464a641868fed3385bb7770f56e2dc0d8428e19b1e50b90c5183  /home/forge/bin/tighten
55d64074df8fecb412ac7628eab8393268ca7c92931d1e4376d26f230a0dfb7d  /home/forge/bin/andon-pull
6d7d7a354892fedb720d758ab8239d2076b864ebf927539aea52538ace608dc5  /home/forge/bin/andon-lift
26f760806de5f5406eaeec1c7a78d1186e14ef24c6bb854ecd357fe11955bff2  /var/tmp/forge/tighten/fixtures/run-binding-andon.sh
```

## The binding (Sol condition 1)

`tighten binding` writes ONE immutable EDN file, `~/.local/state/tighten/binding-<epoch>.edn`, plus a
`current` symlink. The epoch id is the **sha256 prefix of the file's own canonical body**, so the
file's name is a claim the file can be checked against. The body is deliberately
**timestamp-free**: two calls on an unchanged seat hash to the same epoch and the second is refused
as a duplicate, instead of silently minting a new epoch on every call. `written-at` therefore lives
in the header comment, outside the hashed region, and the file says so.

Content: seat id/host/account/runner/workspace; package epoch (skills branch + tip, the three skill
file hashes, the MANIFEST file hash and every MANIFEST row, including the commented reference rows);
absolute argv + sha256 + status + scope for **12** commands (10 installed, 2 `contract-only` with
`:path nil` — the independent recorder and the external duty supervisor); the three cron lines read
from `crontab -l`; owners; the alert command; the records destination; the round profile; all 19
`TIGHTEN_*` names with their observed values (all `unset` in this process, recorded as `unset`
rather than omitted); and the doctrine it was derived from.

**Owners are `"unnamed pending inb-9c04cc(b)"` for steward, independent recorder/acceptor, external
duty supervisor and rung 2.** No name was invented. That is the honest state and it is why every
qualification claim under CHANGE-RULE.md is still pending.

`tighten verify-binding` re-hashes every bound command at its absolute path and refuses on any
drift. Three epochs were written today, and the mechanism proved itself in the ordinary way — each
edit to `tighten` or `andon-pull` made the previous epoch go red:

```
BINDING-DRIFT tighten bound=b767f7bb… installed=1da0e874… path=/home/forge/bin/tighten
BINDING-DRIFT andon-pull bound=8a90880b… installed=55d64074… path=/home/forge/bin/andon-pull
BINDING-DRIFTED epoch=6156eaa8e5c08bb5 commands=10 file=… next: repair the named command, or open a NEW epoch with 'tighten binding'
```

Current epoch: **`f52666ac556fbac1`**, `BINDING-OK epoch=f52666ac556fbac1 commands=10`. The two
superseded epochs (`6156eaa8e5c08bb5`, `357b8e49ed0afe9e`) are retained beside it, read-only (0444).

**Honest limit, stated in the code:** a hash stored in the file it covers detects accident and
careless editing, not a determined forger — anyone who can rewrite the body can rewrite both header
lines. Immutability here rests on the epoch file never being rewritten (0444 + `BINDING-EXISTS`) and
on publication; it is not a signature. A signed, externally issued binding is Sol condition 2, which
this work does not touch.

**`seat-receipt` was not edited.** Its `on_loop` reasons are computed from its own four checks, and
adding a fifth would have meant editing a file outside this file-set. Instead `tighten status` reads
`verify-binding`'s exit code and prints the missing reason where a reader will see it:

```
-- binding (immutable seat binding — Sol condition 1)
   BINDING-DRIFT …
   BINDING-DRIFTED …
   *** on_loop reason: binding-drift  (tighten verify-binding exit 2)
   NOTE: seat-receipt does NOT read the binding — this line is the reader.
```

That is a smaller ratchet than a receipt field: nothing files an inbox item on binding drift. Folding
`binding-drift` and `binding-absent` into `seat-receipt`'s `REASONS` (and therefore into its existing
tripwire) is the next owner's change, and it is two lines.

## The Andon cord (Sol condition 4)

House rules require a cord that is **adjacent** (one command, no permission, no mayor in the path),
that **summons help** rather than logging an event, and where **the line actually stops**. The order
of operations is the contract:

1. create the stop id and write `pull.edn` — **before any delivery attempt** (fail open to stop);
2. write the scoped `FREEZE` marker naming the affected release lane;
3. attempt durable delivery on the maven inbox and record the returned `inb-` id in `pull.edn`;
4. print the exact direct session message the seat's main loop must send, and say plainly that this
   program cannot send it.

`pull.edn` is **append-only**: form 1 is the stop and is never rewritten, form 2 is the delivery
receipt, form 3 is the lift. A stop record a later delivery attempt could rewrite is not a stop
record.

Delivery failure prints `ANDON-DELIVERY delivery=FAILED`, records `:status "FAILED"` durably, prints
`ANDON-ESCALATE reason=delivery-FAILED — the stop STANDS`, and still exits 0. **A failed delivery
escalates; it never cancels the stop.**

`andon-lift` is the other half: **fail open to stop, fail closed to resume.** It refuses without a
named owner AND evidence, refuses an unknown id, and refuses a second lift of an already-discharged
stop. It removes `FREEZE` and appends the lift form; it never deletes the stop record.

### Live delivery proof — a real pull, honestly labelled

One real pull was run so the delivery path is proved with a real item, and lifted immediately. It
does not claim a defect: the `what-stopped` field says so in capitals, and the surface is
`andon-self-test`, a lane nothing releases from.

```
ANDON-STOP-RECORDED id=andon-20260908T140150Z-501edd file=/var/tmp/forge/andon/andon-20260908T140150Z-501edd/pull.edn (written BEFORE any delivery attempt — fail open to stop)
ANDON-FREEZE lane="andon-self-test" file=/var/tmp/forge/andon/andon-20260908T140150Z-501edd/FREEZE
ANDON-DELIVERY delivery=DELIVERED inbox=inb-a235c5 channel=maven-inbox log=…/delivery.log
ANDON-PULLED id=andon-20260908T140150Z-501edd inbox=inb-a235c5 freeze=andon-self-test
ANDON-LIFTED id=andon-20260908T140150Z-501edd owner=forge-anvil lane="andon-self-test" freeze-removed=…/FREEZE record=…/pull.edn
```

maven's own line: `✓ inb-a235c5 queued · task ANDON andon-20260908T140150Z-501edd: NOTHING IS BROKEN
— self-test … · priority normal`. Between the pull and the lift, `tighten status` showed the freeze
open:

```
   OPEN andon-20260908T140150Z-501edd lane="andon-self-test" pulled=2026-09-08T14:01:50Z inbox=inb-a235c5 age=0m
        publish/install/deploy/land are FROZEN for that lane; diagnosis and repair are not.
```

The pull/lift pair and its inbox item are the receipt and are retained.

### Freeze enforcement — the one line `land` and `round` should add

`tighten status` reads open freezes. **Nothing enforces them yet**: `land` and `round` were not
edited (outside this file-set), so today an open freeze on the clj-surgeon lane stops nothing by
itself. The exact line to add near the top of `/home/forge/bin/land`, after `tip` and `title` are
bound:

```bash
for f in /var/tmp/forge/andon/*/FREEZE; do [ -e "$f" ] && grep -qx "lane=clj-surgeon MCP/main" "$f" && { echo "REFUSED: andon freeze $(basename "$(dirname "$f")") holds the lane 'clj-surgeon MCP/main'; lift it first: /home/forge/bin/andon-lift <id> \"<owner>\" \"<evidence>\""; exit 9; }; done
```

and in `/home/forge/bin/round`, the same line with `lane=clj-surgeon namespace_split` (or whatever
lane string that round's profile registers). Two notes for whoever adds it:

* **Match the lane, never the presence of any freeze.** A freeze on an unrelated lane must not block
  this one; a total freeze rewards silence and is not what the house rule says.
* **The puller must keep publishing the fix.** If the lane being frozen is the lane the repair
  itself lands on, the check needs a documented, receipted override — freezing the one seat that can
  close the defect is how a cord turns into a deadlock.

## The witness

`/var/tmp/forge/tighten/fixtures/run-binding-andon.sh` — **35 rows, 0 mismatches**, exit 0. It never
writes under `/home/forge/bin`: every row runs against an isolated `TIGHTEN_STATE_DIR` and
`ANDON_ROOT` under its own `mktemp -d` tree, the drift row alters a **copy** of `canary-cell` and a
**copy** of the binding, and one row asserts afterwards that the installed `canary-cell` still hashes
to its bound sha.

Rows: binding absent → created → epoch file exists and is 0444 → second call refused → verify OK →
DRIFT on an altered command → installed command untouched → BODY-MODIFIED on an unrepaired edit →
ABSENT on a missing file; then andon-pull refuses each empty field and a wrong arg count, a refused
pull creates no stop dir, a pull with `MAVEN_W=/bin/false` still records the stop, writes the freeze,
records `delivery=FAILED`, escalates and prints the message it cannot send, `tighten status` lists it
with the real delivery id, andon-lift refuses empty owner / empty evidence / wrong arg count /
unknown id with the freeze surviving every refusal, then succeeds, removes FREEZE, retains the stop
record, refuses a second lift, and disappears from `tighten status`.

Last run: `/var/tmp/forge/tighten/fixtures/last-run.txt`.

## Two defects the fixture found in the code it was written for

1. **A receipt that named the wrong subject.** `tighten status` extracted the delivery id with
   `grep -oE 'inb-[0-9a-f]+' pull.edn` and printed the *owners note's* `inb-9c04cc` as the pull's
   durable item — a delivery receipt pointing at an unrelated inbox item, exactly house rule 20's
   failure ("a receipt must name its subject, its evidence source, and the causal binding"). Fixed to
   read the `:inbox-id` field of the delivery receipt form, and pinned by two fixture rows, one of
   which asserts `inb-9c04cc` never appears on that line. A second bug hid behind the first: the
   repaired pattern required exactly one space and the EDN is aligned, so it silently returned empty
   — found only because a row asserted the *value*, not the absence of the wrong one.
2. **`tighten help` printed shell code.** The help arm was a hard-coded `sed -n '2,26p'`; the first
   edit that grew the header made it print `set -uo pipefail` and variable assignments as
   documentation. Replaced with an awk that prints the header comment block until the first
   non-comment line, so it cannot overshoot again.

## Defects found elsewhere (reported, not fixed — outside this file-set)

* **The package MANIFEST is already stale for `land`.** `tighten-the-loop/bin/MANIFEST.txt` was
  written at 13:36:23Z and records `land` as `c6131fabd851…`; `/home/forge/bin/land` was modified at
  13:46:37Z and is `9eead0e40c6b…`. `tighten` is likewise `25f48a35…` in the manifest against the
  installed bytes of every epoch since. The manifest's own instruction is "verify a copy against this
  seat", so a destination seat following it would conclude a delivery copy is corrupt when the
  *manifest* is the stale artifact. `verify-binding` deliberately checks **bound commands only** and
  does not fail on manifest staleness — that is a package-epoch fact, and folding it in would make
  `BINDING-OK` unreachable today for a reason that has nothing to do with command resolution. A
  manifest regenerated on each package change, or a dated `as-of` line per row, is the fix.
* **`land --help` still crashes on an unbound `$2`** (already recorded in SKILL.md's inventory;
  unchanged).
* **Nothing files an inbox item on binding drift.** `seat-receipt`'s tripwire is the only thing on
  this seat that reaches a named owner automatically, and it cannot see the binding. Until
  `binding-drift` is one of its `REASONS`, a drifted binding is visible only to whoever runs
  `tighten status`.

## Where conditions 1 and 4 actually stand

**Condition 1 — a concrete retrievable binding: MET on this seat, with one named limit.** A binding
exists, is retrievable at a stable path, is immutable per epoch, is refused on rewrite, carries every
field Sol's edit 7 and SEAT-RECEIPT.md's `commands` row name, and a fresh `verify-binding` exercises
command resolution at the absolute launcher paths without performing any daily mutation. It is
**self-issued**: the subject seat wrote it. Sol's condition 1 asks for retrievability, which this
gives; **condition 2** asks for the independent issuer, and that is untouched — the binding says so
in its own `:owners` and `:commands` rows. Not yet done: `assignment_manifest` (there is no
prospectively assigned task set to bind), and `seat-receipt` does not consume the binding.

**Condition 4 — a one-command Andon path with delivery and fail-closed resume: MET for pull,
delivery and resume; NOT met for enforcement.** One command, no permission, no mayor in the path;
the stop id and the freeze precede delivery; delivery is proved against the real durable channel with
a real item id (`inb-a235c5`); a delivery failure escalates and never cancels; resume requires an
explicit owner and evidence. What remains: (a) **`land` and `round` do not consult the freeze**, so
the freeze is currently a marker plus a status line rather than an enforced stop — the exact one-line
check is above; (b) the **direct session message is printed, not sent** — this program has no session
channel, and the seat's main loop must paste it, which means the 5-minute acknowledgment contract
still depends on a human or main loop acting; (c) **rung 2 is unnamed** (`inb-9c04cc(b)`), and the
house rule that an unstaffed escalation path keeps the scope closed is written into the pull record
but has no automated enforcement; (d) there is **no acknowledgment tracker** — nothing measures the
5-minute deadline or escalates on silence.

The remaining five Sol conditions (2 independent receipt and signed participation, 3 fresh-cell
execution of the full command set, 5 the `round` builder-commit contradiction, 6 Gene's ratification
and named owners, 7 consumer-side uptake) are untouched by this work. `tighten-the-loop` remains
NO-GO as installable seat authority.

Sources: `/var/tmp/forge/plan2/cellC/sol-tighten-review.md` §4–5 and edit 7;
`/home/forge/opt/claude-skills/_doctrine/house-rules.md`, "The Andon cord" and delivery invariants
17–20; `tighten-the-loop/{SKILL.md,SEAT-RECEIPT.md,CHANGE-RULE.md}`;
`docs/observations/2026-09-08-tighten-one-shots.md`.
