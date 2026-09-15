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
