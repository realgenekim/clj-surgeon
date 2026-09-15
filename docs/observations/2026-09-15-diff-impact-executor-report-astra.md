# Diff-impact executor parity — Round 1

Recorded: 2026-09-15T05:45:09.019800+00:00

Branch: `fable/impact-executor`, starting at `866df39bbfcae712d35f37497aa0c7d5927212e2`.
Issues: inb-b142af, inb-fbd110. Standing approval covers every phase.

## Contract and implementation

The executor now accepts `--suite LANE --selected '[namespace ...]'
--selection-sha SHA256`. It admits a nonempty vector of distinct members before
creating output, then uses the gate's existing preparation, cost partitioning,
runtime assignments, worker pool, slot admission, prerequisites and child launcher.
Partial receipts record `:partial true`, `:selected`, and `:selection-sha`.
They read scheduling estimates without replacing full-suite wall tables.
Serial groups are intersected with selection; raw child census is checked before
merged shard observations can conceal missing or outside namespaces.

`census-problems` accepts a partial receipt and checks its selected subset.
Full-suite validation rejects a partial receipt even when its subset equals the
entire suite. Battery freshness rejects it too, and ledger serialization preserves
the partial marker. The gate entry itself rejects `--selected` before effects.
The named refusal is `partial-receipt-not-a-gate-receipt`.

Fixed-point runs separately scoped `:fast` / `:all` observations by default;
its optional fourth argument runs only `fast` or `all`. Each observation records
selection, execution and combined walls, results, lane receipts and the SHA-256
of the exact selection EDN bytes. Before/after/merged retain a single all-scope
execution. HOLD and nothing-selected retain their original precedence and exits.

All scope includes the five registered dedicated witnesses. They use the existing
observed-test runner serially. The analyzer retains its mission and scope hash;
memory work resolves and acquires the same lock as `make memory-red-kernel` via
`bin/with-lock`. The dedicated inventory is explicitly bounded; future exclusions
need admission rather than silently receiving this policy. Unknown membership
is a typed refusal with no selected namespace silently omitted.

The real dedicated runner exposed nine retained recovery fixture directories.
The recovery tests now track and remove their own roots in `finally`; assertions
and the leak gate remain intact.

## Committed red and subsequent findings

- `fdfebf78`: first red commit, before executor implementation. Five failing
  assertions expose missing subset/census/scope functions and accepted partial
  gate/freshness receipts.
- `7a8d79ae`: dedicated selection committed red; registered analyzer, memory and
  lifecycle witnesses were refused by the initial lane-only projection.
- The original serial executor on a one-file fixture selected exactly
  `clj-surgeon.fast-lane-isolation-test`: 4 tests, 19 assertions, **8 failures**.
  Invocation wall **7,833 ms**, child wall **3,417 ms**, exit 1. Its inherited home
  causes the reported failure. The fixture adds only a comment to that test file.
- A serial-group negative reproduced an unselected member being scheduled;
  intersection repairs it without removing any selected member.
- Initial CLI verification caught a delimiter error. The first full-fast fixture
  also caught a malformed requirement heading and missing implementation link.
  Both failures are retained; the requirement and its actual pure admission
  implementation now pass the intent audit.

## Environment and comparison bounds

All scratch and fixtures live under `/var/tmp/forge/impact-fx/exec`.
The before/after fixtures are local shared clones. Their only measured diff is
`;; executor parity fixture: same assertion body` appended to the isolation test.
The original and repaired selector discover that diff themselves.

The fixture driver overrides only the launcher's root-location predicate so the
private root can be nested inside the user-mandated scratch directory. It retains
matching TMPDIR/java.io.tmpdir, the real envelope, real child execution, HOME and
user.home assertions. This is not evidence that an outside root passed the public
launcher. No product path contains the fixture root.

The serial child argv has been removed. The selected path is now the same
`run-suite! -> execute-plan! -> run-lane!` path used by the gate, with the same
`lane-command` / `bb-lane-command` and `tmp-leak-support` re-exec. Thus the child
heap policy, private temp root, startup `-Duser.home`, HOME and compile-cache
policy come from the same implementation. The selected isolation namespace is
assigned BB by this snapshot; a separate JVM invocation also exercises its 19
assertions at `-J-Xmx1024m`.

This host exposes **16 CPUs**, so the unchanged gate policy admits **8 workers**,
not the 16 workers of yesterday's 32-CPU measurement. A singleton uses one worker.
The runner still derives width from the same CPU/memory policy; it does not invent
capacity to reproduce yesterday's number. These are fixture correctness walls,
not the preregistered five-diff repaired replay or a routing/speed claim.

## Verification and final fixture walls

Completed: 2026-09-15T05:53:23.859586+00:00

| Check | Result |
|---|---|
| JVM: diff-impact, battery-parallel, lane-manifest, battery-ledger | 111 tests / 2,611 assertions, 0 failures/errors |
| BB: diff-impact | 16 tests / 262 assertions, 0 failures/errors |
| JVM isolation, explicit 1024 MiB invocation | 4 tests / 19 assertions, green |
| Original analyzer alias after its require-safe refactor | 6 tests / 25 assertions, green |
| Dedicated analyzer + recovery + prune through selected entry | 40 tests / 147 assertions, green, 7,976 ms |
| Both dedicated memory namespaces through selected entry | 4 tests / 25 assertions, green, 505,909 ms |
| Final full-fast fixture control | 95 namespaces; 1,257 tests / 13,428 assertions; 0 isolation/leak failures |
| Formatter and ~/bin/clj-kondo | clean; 0 errors / 0 warnings |
| make census-regenerate | exit 0; net +5/-0; final regeneration +0/-0 |
| intent audit | :ok true, no violations |
| Protected ledger hashes | both byte-identical |

No new test namespace was introduced: the five new deftests are in existing
registered namespaces. `test/diff_impact_executor_parity.clj` is a standalone
artifact checker, not an additional test suite. No `make test` or `test-battery`
was run. The full fast control uses the direct gate runner, not those targets.
Required verification JVMs use `-J-Xmx1024m`, BB uses `-Xmx1024m`; gate children
retain the gate's own heap policy, and memory fixtures retain their specified
256 MiB subject / 2 GiB reference heaps.

### One-file fixture, measured before and after

| Arm | Selection ms | Executor ms | Whole invocation ms | Result |
|---|---:|---:|---:|---|
| Original serial fixed-point | included in invocation | 3,417 | 7,833 | 8 isolation assertion failures |
| Full fast control, final fixture | — | 26,593 | 26,924 | passed |
| Repaired fixed-point, fast only | 4399 | 554 | 5,049 | passed; same namespace verdict as full fast |
| Repaired default, fast observation | 4,224 | 573 | shared invocation below | passed |
| Repaired default, all observation | 4,224 | 467 | shared invocation below | passed |

The default invocation runs both observations and took **5,379 ms** in total.
Its scoped selection-plus-execution totals are 4,797 ms (fast) and 4,691 ms (all).
Those totals charge selection to each comparison; they are not added together.
External whole-invocation walls include process startup and output overhead.
These are single samples, with warmed classpaths; no variance-floor or speed
qualification is claimed.

The permanent checker compares selected per-namespace test/isolation verdicts
against the corresponding full-fast results, requires matching source digests,
checks partial provenance and census, and prints walls without asserting them.
Its final result is `:parity :passed`, `:delta []`; the selected isolation test is
true in both arms. A changed-subject negative is rejected with exit 1.

A second real diff adds a comment to the recovery test as well. Fixed-point all
selects exactly recovery + isolation, executes dedicated + fast receipts, and
passes with a 3,972 ms executor wall / 8,285 ms selection-plus-executor wall. This
witnesses actual per-lane delegation beyond a singleton fast invocation.

### Refusal and admission probes

The CLI rejected outside-suite names, duplicates, EDN reader-eval input and a
selected landing-gate request with exit 1, without creating an output directory.
The gate probe emitted `:partial-receipt-not-a-gate-receipt`. Partial full-suite
and freshness witnesses also include a subset equal to the full suite and a
ledger serialization round trip.

The memory lock probe opened the resolved lock again in its child and verified
that nonblocking exclusive acquisition fails while the inherited lock is held.
It reports `exclusive-memory-lock-held-through-exec`, exit 0. The resolver uses
Make's `info` expansion, not a generated shell command interpolating the path.

### Durable locations and reproduction

Report data and logs are under `/var/tmp/forge/impact-fx/exec/`:

- `red.log`, `dedicated-red.log`, `serial-group-red.log`
- `before.log`, `before-wall.txt`, `before-out/`
- `complete-full-fast/receipt.edn`, `complete-full-fast.log`, `complete-full-fast-wall.txt`
- `complete-after-out/impact-fixed-point.edn`, `complete-after-out/results-fixed-point.edn`
- `fast-only-out/`, `fast-only-wall.txt`, `multi-lane-out/`, `multi-lane-summary.edn`
- `parity-final.edn`, `parity-negative.log`, `fixture.diff`, `fixture-base.txt`
- `dedicated-verified/receipt.edn`, `memory-dedicated/receipt.edn`, `lock-probe-final.edn`
- `jvm-final-verified.log`, `bb-final-verified.log`, `isolation-jvm.edn`
- `analyzer-original-entry.log`, `cli-refusals.json`, `census-complete.log`, `intent-complete.log`
- `lint-complete.log`, `protected.sha256`

The fixture driver is `complete-parity-run.sh`. Check the retained pair from the
repository root with:

```sh
bb test/diff_impact_executor_parity.clj \
  /var/tmp/forge/impact-fx/exec/complete-full-fast/receipt.edn \
  /var/tmp/forge/impact-fx/exec/complete-after-out/results-fixed-point.edn
```

The selection SHA-256 is
`307d7fb67ac44e76e6f7d37f7698b8932eb71691efeb62fb78244f7935095420`.
The protected hashes, captured before edits and checked afterward, are:

```text
08a1d6ee9c7f319c4a146ec23798e535273a47b7633697354014b318bff8ad39  docs/observations/battery-ledger.edn
154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264  docs/observations/battery-namespace-walls.edn
```

Read first: the records repository's
`docs/observations/2026-09-14-diff-impact-measure/RESULT.md` and
`preregistration.md` Addendum. Their original broken bets remain broken. This
branch supplies executor parity and its class oracle; it does not execute or
score that repaired five-diff experiment, merge to trunk, or change routing.

## Round 2

IMPACT-EXEC-01 repair against `cf237f96`, under standing phase approval.
The red commit is `5e3367ef`. **That same commit replaces the contradictory
unknown-fast `{}` expectation with a typed-refusal expectation.** Seven assertions
failed before implementation. Sol's exact renamed-away call produced:

```edn
{:selected [] :receipts [] :runs [] :exit 0 :scope :fast :partial true}
```

The committed `fast-scope-total-membership` oracle covers the five-class fixture,
all unaccounted names in both scopes, stale manifest membership absent on disk,
explicit matching selection exclusions, mismatched/missing exclusion reasons,
BB-only admission, JVM admission despite BB incompatibility, and the exact
renamed-away public executor call with zero child launches. The repaired call
throws `:error-type :selected-namespace-unclassified` and names
`selected-namespace-unclassified clj-surgeon.renamed-away-test`; the CLI turns
that into a refused observation with exit 1.

Classification precedes projection. Successful and refused observations retain
`:classification` for the complete selection. Fast runs fast members and reports
other-lane members with their lane and suite; all routes those members normally.
An on-disk, admitted JVM/dedicated member remains a member when BB is incompatible.
An absent namespace cannot inherit admission from a stale manifest. Non-members
require an explicit matching `:selection-exclusion {:reason <classification>}`
on their selection-receipt entry; dependency-edge reasons cannot authorize omission.

| Fixture namespace | Classification | Lane / suite |
|---|---|---|
| fast-test | fast-member | fast / fast |
| other-test | other-lane-member | integration / mcp |
| renamed-test | unregistered/renamed | refusal |
| excluded-test | load-excluded | refusal |
| ineligible-test | bb-ineligible | refusal |

Both scopes report all three refused names together, each with
`selected-namespace-unclassified <ns>`. No run begins until the entire table
has passed admission.

### Re-run of Sol's probes

Sol's script was present. The retained copy changes only its scratch prefix to
this round's allowed directory. Selected output (full output in `sol-probes.log`):

```text
:battery-fresh {:ok false, :reason :partial-receipt-not-a-gate-receipt, :message A selected subset cannot certify battery freshness}
:census-gate [{:kind :partial-receipt-not-a-gate-receipt, :suite fast}]
:partial-execution-census []
:prewarm-entry {:error-type :partial-receipt-not-a-gate-receipt, :message A selected subset cannot certify the landing gate}
:landing-entry {:error-type :partial-receipt-not-a-gate-receipt, :message A selected subset cannot certify the landing gate}
:landing-receipt-chain {:eligible false, :problems [{:kind :partial-receipt-not-a-gate-receipt, :suite fast}]}
:selected-silently-omitted {:state :failed, :namespace-census {:expected [a], :observed [], :expected-count 1, :observed-count 0}}
:unselected-ran {:state :failed}
```

The omitted case includes `:missing [a]`; the unselected case includes
`{:kind :namespace-census-mismatch :missing [] :unexpected [b]}` and
`:partial-child-census-mismatch`. The existing parity checker accepts its retained
positive pair (exit 0, `:delta []`) and rejects Sol's retained user.home-omission
result (exit 1, `{:namespace clj-surgeon.fast-lane-isolation-test :control true
:candidate false}`). These are rechecks of retained artifacts, not new full-fast
measurements. JVM and BB child command comparisons report
`{:argv-prefix-delta []}`; only the namespace tail differs.

### Validation

- BB diff-impact: 17 tests / 287 assertions, zero failures/errors.
- JVM affected run: 97 tests / 2,531 assertions; only two stale-census assertions
  failed before regeneration. Diff-impact and battery-parallel passed.
- `make census-regenerate`: exit 0, +1/-0 (the new oracle).
- JVM lane-manifest after regeneration: 35 tests / 1,935 assertions, zero
  failures/errors. All requested affected namespaces therefore pass.
- Warm JVM exact oracle passed after checking this worktree's `user.dir`.
- Standard Clojure Style v0.29.0; `~/bin/clj-kondo`: 0 errors / 0 warnings.
- Intent audit: `:ok true`; `git diff --check` clean.
- Required JVM/BB invocations use 1024 MiB. Replay children keep the original
  gate's own runtime/heap policy. No `make test` or `test-battery` was run.
- Both protected ledgers remain byte-identical to the pre-round SHA-256 capture.
  The selector source and HOLD/nothing-selected block are byte-identical to
  `cf237f96`; both intent documents retain their complete base prefixes.

### D2 / D3 replay

Fresh shared clones of Sol's clean D2 (`8aedb65e`) and D3 (`ad05e27c`) subjects
live entirely under `/var/tmp/forge/impact-fx/exec/round2`. D2 compares `00566756`;
D3 compares `ad05e27c^`. The overlay supplies the repaired selector/coordinator
and its probe-state dependency without changing the subject Git diff or manifest.
The driver replaces only the environment root-location predicate for the nested
scratch root, as in round 1. This is not public-launcher root admission evidence.
No product path derives from scratch. Runs are serial by replay cell; affected
checks overlapped some cells. These single-sample walls carry no speed claim.

| Replay | Complete selection | Executed | Selection ms | Execution ms | Selection + execution ms | Invocation ms | Exit |
|---|---:|---:|---:|---:|---:|---:|---:|
| D2 list | 9 | 0 | — | — | — | 4,274 | 0 |
| D2 fast | 9 (7 fast / 2 other-lane) | 7 | 4,104 | 9,047 | 13,151 | 13,245 | 0 |
| D3 list | 101 | 0 | — | — | — | 4,607 | 0 |
| D3 fast | 101 (51 fast / 50 other-lane) | 51 | 4,331 | 33,357 | 37,688 | 37,781 | 1 |
| D3 all | 101 (51 fast / 50 other-lane) | 101 | 4,480 | 773,462 | 777,942 | 778,022 | 1 |

D3 fast fails only `clj-surgeon.lane-manifest-test`, as expected on that historic
red snapshot. D3 all reports those same 101 namespaces as executed, with failing
namespaces `clj-surgeon.txn-journal-test`,
`clj-surgeon.receipt-artifacts-boundary-test`, and
`clj-surgeon.lane-manifest-test`. Its battery receipt additionally reports two
nonzero child exits and `:partial-child-census-mismatch`; dedicated and mcp
receipts pass. These fixture failures are retained, not excused as repair-suite
passes. Battery wall is 160,219 ms; dedicated wall is 513,078 ms.

All round-2 scripts, logs, cloned subjects, overlay files and receipts are retained
under `/var/tmp/forge/impact-fx/exec/round2/`. Reproduction driver: `replay.py`.
Evidence: `red.log`, `green-focused.log`, `renamed-green.edn`, `sol-probes.clj`,
`sol-probes.log`, `argv.log`, `parity.log`, `parity-negative.log`, `jvm.log`,
`bb.log`, `lane-final.log`, `census.log`, `format.log`, `lint.log`, `intent.log`,
`summary.edn`, `replay-walls.json`, `protected.sha256`, `protected-code.json`,
and `d2-{list,fast}` / `d3-{list,fast,all}` receipt directories.
The project-local nREPL started for the warm check was stopped afterward.

Completed: 2026-09-15T06:59:38.442206+00:00

## Round 3

Standing approval covered the complete TDD repair. Installed consumer source:
`/home/forge/bin/gate-envelope.clj`, SHA-256
`034b5afed6023b29da55ee8354b2c9448ca8d507dba5de17c057ba9ffb2765c3`.
The installed consumer and suite wrapper were read-only and their hashes were
checked afterward. Scratch evidence lives under
`/var/tmp/forge/impact-fx/exec/round3/`.

### Reproduction and corrected attribution

Both requested commits were checked out as detached scratch worktrees and run
through `/home/forge/bin/suite-run make landing-gate-prewarm`, with identical
explicit environment recorded in `environment.edn`. This is the ship's
prewarm-target entrance, with no replacement Make recipe or staged receipt.
The consumer was run as `bb /home/forge/bin/gate-envelope.clj <out>` with
`GATE_WORKTREE`, `GATE_RECEIPT_PATH`, and `GATE_OBLIGATIONS_PATH` pointing at each
worktree and its retained evidence. No missing inventory was manufactured.

**The requested positive control does not reproduce.** At `866df39b`, prewarm
passes but the installed consumer refuses both missing fields, just as the
candidate does. No code path in the requested diff dropped them:
`test/clj_surgeon/battery_parallel_runner.clj:862` at `866df39b` and `:962` at
`88b8603d` construct the same receipt map. The complete `run-gate!` diff adds
only the `--selected` refusal (`:900` at the candidate); it does not change the
receipt writer. Neither snapshot includes `gate_obligations.clj` or
`toolchain_identity.clj`. The historical producer implementation exists at
`7582c0d9` with refinements at `2f924bee`, outside the requested baseline's
ancestry. Evidence: `run-gate-only.diff`, `round3-runner.diff` in the parent
scratch directory, and the two retained raw producer receipts.

The supplied incident receipt names commit `2dce4893` but tree
`3291767f14eb0d9ac855a9e48ac567a726a32d9e`, the same tree as `88b8603d`.
Its top-level keys also match the reproduced baseline, including the absence
of `:toolchain`. The observed control result is RED/RED, not a fabricated
GREEN/RED or a claim that executor census changes removed these fields.

### Producer contract and TDD

`landing-envelope-consumer-contract` drives the real `run-gate!` writer with
bounded stage and identity facts, for both full landing and prewarm.
Identity subprocesses are isolated behind `gate-provenance`, captured once by
the real runner and substituted by the fast-lane boundary test. The original RED on
`88b8603d` records four failures: missing `:toolchain`, missing `:executions`,
a non-map toolchain, and absent inventory. `oracle-red.log` retains the actual
failures. `full-and-partial-receipts-share-envelope-evidence` has a separate
recorded RED (`partial-red.log`) before the common evidence helper was added.

The consumer's producer reads are explicitly mirrored:

- Receipt: `:toolchain`, `:git-head`, `:git-tree`, `:source-digest`, `:run-id`,
  `:prewarm?`, `:landing?`, `:stages`, `:suites`, `:executions`, `:state`,
  `:started-at`, `:completed-at`.
- Toolchain refusal check: `[:toolchain :unknown]` must be empty.
- Stage: `:target`, `:exit`, `:wall-ms`.
- Legacy suite adapter: `:suite`, `:state`, `:result`, `:runs`; run
  `:namespace`; result `:fail`, `:error`, `:test`, `:pass`, and optional
  `:precondition-skipped`. The native execution producer explicitly records
  zero skipped preconditions when the runner observed zero.
- Inventory: `:policy-sha256`, `:inventory-version`, `:obligations`;
  obligations' `:id`, `:stage`, `[:recipe :sha256]`, `:runtime`, `:scope`,
  suite `:suite`, and `:selected-test-identities`.
- Native execution adapter reads optional `:expected-vars`, `:executed-vars`,
  and `:target` to externalize large identity lists; the installed consumer's
  65,536-byte bound remains unchanged.

Restore the historical typed identity and inventory implementation with its
policy-hash, rule-input, namespace identity, and banner witnesses inside the
already registered `battery-parallel-test` namespace. Keep current hybrid MCP
runtime assignments and add diagnostic BB as R11. Inventory R1–R10 retain their
historical meaning. Prewarm discharges only executed stages and therefore
never claims R2/battery-fresh. `receipt-evidence` enriches both gate and suite
writers without projecting away existing fields; partial suites retain
`:partial`, `:selected`, `:selection-sha` and discharge no whole-gate obligation.
Each writer emits its inventory with a hash and byte count. Original receipt
values and `pr-str` EDN serialization are preserved; byte identity for missing
historical fields cannot be asserted.

The real partial smoke selects `clj-surgeon.analyze-test`: passed, one namespace,
zero isolation violations/skips, `:receipt-version 2`, `:partial true`, known
toolchain, readable hashed sidecar, and `:discharged-by-this-receipt []`.
The mocked writer's consumer smoke is labeled `contract-fixture/`; it is only a
boundary test and is not the real fixed-tip prewarm proof below.

### Verification

- JVM, `-Xmx1024m`: diff-impact-test, battery-parallel-test (including restored
  receipt-writer witnesses), lane-manifest-test: **113 tests / 2,838 assertions,
  zero failures/errors** (`affected-jvm-verified.log`, exit 0).
- BB, `-Xmx1024m`: diff-impact-test and battery-parallel-test: **78 tests / 903
  assertions, zero failures/errors** (`affected-bb-verified.log`, exit 0).
- Standard Clojure Style on all four changed Clojure files; lint through
  `~/bin/clj-kondo`: zero errors/warnings (`format*.log`, `lint*.log`, exit 0).
- `make census-regenerate`: exit 0, **+16/-0** (`census.log`); no test namespace
  added, renamed, or removed.
- Protected SHA-256 values remain byte-identical:
  `battery-ledger.edn` =
  `08a1d6ee9c7f319c4a146ec23798e535273a47b7633697354014b318bff8ad39`;
  `battery-namespace-walls.edn` =
  `154faae0c803099d1bfe88e8563a8cf85cdf2376af4e4309b10bd850cd6ef264`.

- Final isolated BB receipt-writer namespace: **61 tests / 615 assertions**,
  `:leak-fail 0`, `:violations []`, 1,738 ms
  (`battery-isolated-verified.edn`, exit 0). An intermediate attempt to mock the
  process library directly was rejected by the fast-lane source census; the
  final `gate-provenance` boundary avoids process-library references in the test
  and passes both the source census and runtime isolation. The intermediate
  failure remains in `affected-jvm-final.log`.

### Real prewarm and installed consumer

| Revision | Prewarm exit | Producer wall (ms) | Consumer exit | Result |
|---|---:|---:|---:|---|
| `866df39b` | 0 | 420574 | 5 | missing inventory and toolchain |
| `88b8603d` | 0 | 420291 | 5 | missing inventory and toolchain |
| `1a780eab` | 0 | 421485 | 0 | complete; 11 obligations / 7 executions |

Actual installed-consumer output, without normalization:

```text
gate-envelope: /var/tmp/forge/impact-fx/exec/round3/fixed-evidence/envelope.edn 59044 bytes state=:complete obligations=11 executions=7 pending=[]
```

Exactly one fixed prewarm ran on code commit `1a780eab5579174fbc78e473471b9e426a2d7642`
(tree `1489034ba2ce3188812c407aec81382a8cc8dbfa`), in `fixed/`.
All seven stages exited 0; producer state is `:passed`, `:prewarm? true`,
`:landing? false`, `:problems []`, and toolchain `:unknown []`. This proves
prewarm and envelope consumability, not battery freshness or landing authority.

| Fixed stage | Exit | Wall (ms) |
|---|---:|---:|
| `admit-transaction-recovery-battery` | 0 | 13075 |
| `alias-migration-test` | 0 | 99746 |
| `mcp-test` | 0 | 106296 |
| `test-bb` | 0 | 96363 |
| `test-bb-diagnostic` | 0 | 232049 |
| `repository-hygiene` | 0 | 2077 |
| `intent-audit` | 0 | 2221 |

Actual full-suite receipts lose no existing keys; their only added keys are
`:obligations`, `:receipt-version`, and `:toolchain`
(`full-suite-key-comparison.edn`). The final producer and inventory copies,
consumer envelope, and externalized Var sidecars are in `fixed-evidence/`.
Both original receipts and their raw diff are retained in `base-evidence/`,
`candidate-evidence/`, and `producer-receipts.diff`. No historical receipt
was patched to manufacture the expected positive control.

The subsequent report-only commit changes no gate input: Makefile, deps.edn,
bb.edn, src, test, resources, and docs/intent remain identical to the tested
code commit. Both commits carry the three required trailers. No merge or push
to main; publication is confined to `fable/impact-executor`.

Report assembled 2026-09-15T08:28:19.483479+00:00 from retained machine receipts.
