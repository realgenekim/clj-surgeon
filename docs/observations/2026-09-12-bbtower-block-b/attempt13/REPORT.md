# Attempt 13: PASS-WITH-TMP-UNKNOWN

The requested temp/state-root repairs are implemented on
`bb-rewrite-tower-local`, starting at `b45d3eb113b5d258ba3601023a8c64fc9fce8339`.
Final source tip: `86d98e4afd8ab3d12cb5a3e6bd2e2710d655def6`. The observation
directory is committed last; no gate can attest a future documentation commit.

Product-owned direct bb commands and generated stable/checkout CLI launchers
now pass an explicit startup temp property. The selection preserves nonblank
TMPDIR except `/tmp` and `/dev/shm` trees, otherwise using `/var/tmp`.
Formatter/process staging uses that same explicit root. Typist formatter
refusals retain native message, path and process evidence. The alias tmpfs
fixture is beneath the configured product artifact root, and Git fixtures
use the same TMPDIR policy. Detached proof retains its stricter existing
root-admission check and now passes its admitted selected root at startup.

## Verification and the one repair

* CLI startup red: the real bb child returned `/tmp`, not TMPDIR. Typist red:
  two assertions showed its native formatter error/receipt were discarded
  (`red.log`). The original formatter also failed the explicit staging-root
  assertion and threw an uncaught native exception (`formatter-red.log`).
* Green focused checks: 25 tests / 110 assertions plus two boundary Vars /
  five assertions (`green.log`). After the remaining subprocess edits,
  Git/detached-proof/process/formatter boundaries passed 32 tests / 198
  assertions (`boundaries.log`).
* The real formatter succeeded with `java.io.tmpdir=/sys` and an allowed
  TMPDIR (`readonly-jvm-tmp.log`). This is the requested read-only stand-in;
  no Landlock confinement claim is made.
* The three previously red namespaces ran once under a fresh empty TMPDIR:
  202 tests / 3,827 assertions, zero failures/errors and zero isolation
  violations (`three-namespaces.log`). The root was empty afterward and
  removed (`focused-root-after.txt`, `focused.sh`).
* Both generated launchers passed eight root/argv cases and real CLI
  `--version` invocations in an owned scratch installation
  (`launcher-check.log`). Python spawn policy passed five real bb-child
  cases; mission help and shell syntax passed (`entrances.log`).
* `make test-fast` ran exactly once and passed: coordinator 1,242 tests /
  11,765 assertions; clj-splice 5 / 59. Fast cadence was 40,285 ms against
  the unchanged 60,000 ms ceiling; bb cadence 14,434 ms against 343,102 ms
  (`test-fast.log`, `test-fast.exit`).
* First prewarm failed one existing CLI compact-receipt assertion: console
  length 3,970 versus receipt length 3,660 (`gate-first.md`). The CLI ran in
  the live checkout, so its required workspace-status evidence grew with
  this attempt's untracked observations. All lane budgets passed. The one
  repair moves that test's CLI children into the owned fixture directory,
  anchors the classpath, and uses the shared bb temp adapter. Every original
  assertion remains; an added assertion checks the reported workspace root.
  The repaired real apply/undo witness passed 30 assertions (`repair.log`).
* All changed Clojure files were formatted and linted through `~/bin/clj-kondo`;
  final lint reports have zero errors/warnings. Four new tests were added to
  the census, with no removals. No membership, runtime or budget changed.

Final prewarm passed on the second and last invocation: exit 0,
`:state :passed`, `:problems []`, `:landing? false`, wall 386,707 ms.
Run: `5e655dfe-aa31-4ad5-9fc7-9f7836ba1870`. Exact machine evidence is in
`gate-receipt.edn`; all stdout/stderr is retained verbatim in `gate.md`.

| Final prewarm measurement | Result | Unchanged ceiling | Evidence |
| --- | ---: | ---: | --- |
| Alias battery cadence | 83,241 ms | 1,800,000 ms | gate.md:79 |
| MCP fast cadence | 41,882 ms | 60,000 ms | gate.md:479 |
| MCP integration cadence | 66,790 ms | 240,000 ms | gate.md:480 |
| bb runtime sum | 235,571 ms | 343,102 ms | gate.md:757 |
| bb runtime makespan | 85,965 ms | — | gate.md:757 |
| Serial bb diagnostic wall | 217,854 ms | — | gate.md:1094 |
| Complete prewarm wall | 386,707 ms | — | gate-receipt.edn |

Alias: 182 tests / 3,639 assertions. MCP: 1,410 / 15,563. bb: 899 / 7,984.
Serial diagnostic: 830 / 7,283. All passed. Recovery, shell checks,
repository hygiene and intent audit also exited zero. The source is accepted
by these checks after one repair; the overall verdict remains qualified by
the temp-audit uncertainty below.

## Temp audit: qualified evidence

`tmp-before.txt.gz` and `tmp-after.txt.gz` are names-only `/tmp` listings around the
single three-namespace run; full `ls -la` captures are retained too. The
immediate difference contains **one** name: `tmp.Ijj8taxfAV` (`tmp-new.txt`).
It had disappeared before stat/inspection. A later read-only listing contains
zero new names relative to the same baseline (`tmp-new-followup.txt`).
No existing shared `/tmp` entry was removed by this agent.
The five large endpoint listings are losslessly gzip-compressed; their
names-only differences remain plain text. Use `gzip -dc` to read a snapshot.

Therefore **the requested unconditional claim that /tmp gained no new entries
during the run is not established**. The transient name is unattributed,
not assumed to belong to another process and not counted as a proven product
leak. Endpoint listings cannot rule out create/delete activity between them.
The direct startup, explicit staging, and namespace-isolation witnesses prove
their named boundaries independently of this ambiguous shared-directory name.

## Commits, dogfood and limits

* `f097d30ff8f1147581376078a55c9491b190c20a`: product launches, formatter
  refusal/root handling, linked intent and regression witnesses.
* `c49e74252b8fddc09adba474a358c4b3301ff524`: alias and Git fixture roots.
* `86d98e4afd8ab3d12cb5a3e6bd2e2710d655def6`: the one prewarm repair,
  confined CLI compactness fixture.

[DOGFOOD.md](DOGFOOD.md) records every source edit, mechanism, refusal and
repair sufficiency. [commands.md](commands.md) and the executable observation
scripts document the checks. `gate.md` is verbatim final prewarm output;
`gate-first.md` retains the failed first attempt without alteration.

Least sure: attribution of the transient `/tmp` name. Literal TMPDIR selection
mirrors attempt 12; it does not add general mount/symlink validation. The
existing product admission checks retain their own stronger policies.

Disagreement/deviation: the immediate /tmp snapshot is not clean, so this
report does not manufacture the requested zero-new-entry claim. The first
prewarm needed one fixture repair. There were exactly two prewarms and one
test-fast, with no concurrent agent-launched suites.

Owed to Fable: review and packet-envelope recheck, including attribution or
confinement evidence for the transient /tmp name. A prewarm is not a landing
receipt or battery-fresh certification. No push, tag, shared install, shared
service operation or landing occurred. Existing untracked prewarm-checkonly
evidence is preserved.
