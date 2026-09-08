GO-WITH-FIX

# Sol fence review: TEST-ISO-013 parallel battery

Reviewed `fable/battery-parallel` at `119e0c61` as contained in the review
merge `13dcc378`. The retained runs and the coordinator support landing after
the fixes below. I did not run `make test-battery`.

## Fixes applied

1. **Alternate namespace fixture registration slipped the shard guard.** The
   guard read only `:clojure.test/once-fixtures` and
   `:clojure.test/each-fixtures` from namespace metadata. I constructed a
   namespace with `test-ns-hook`: `fixture-refusal` returned nil, the serial
   `clojure.test/test-ns` path ran only the hook, and the shard's `test-vars`
   path bypassed the hook and ran the deftest. `shard-refusal` now rejects a
   namespace that resolves `test-ns-hook`; the focused witness executes both
   paths and pins the difference.
2. **A coarse shard observation could overwrite fine per-var costs.** A
   multi-var result is no per-var measurement. `write-walls!` now records a
   var wall only when `(= 1 (count (:vars r)))`, and preserves the preceding
   fine measurement for all other vars. The preservation is necessary: merely
   filtering the current run and rewriting an empty map would make every var
   unknown and flatten the next schedule again. The witness starts with two
   unequal fine costs, submits one aggregate shard plus one singleton, and
   proves the unequal costs survive while the singleton is refreshed.
3. **The review switches the qualified path on.** `BATTERY_LANES` is now 8.
   There is no useful arbitrary "N green landings" hold after three retained
   green wide runs, a one-lane per-namespace control, and a red injected-failure
   run. `BATTERY_PREREQS` is now 1: a normal battery receipt used by `make test`
   should cover the prerequisite that `make test` owns. Use
   `BATTERY_PREREQS=0` only for the explicit serial comparison.

Changed files:

- `Makefile`
- `test/clj_surgeon/battery_parallel_runner.clj`
- `test/clj_surgeon/battery_parallel_test.clj`

## Adversarial findings

### Lane isolation and failed children

Each lane's test runner re-execs into a separately minted
`clj-surgeon-suite-<pid>-<random>` `java.io.tmpdir`; its descendants receive
that root through `TMPDIR`, `TMP`, and `TEMP`. I found no fixed listening port
in the battery namespace set; the real HTTP server witness requests port 0.

The paths intentionally shared between lanes are:

- the repository working directory and its `target/` tree;
- `target/admit-transaction-recovery-battery-receipt.edn`, read by
  `admit-patch-test` after the prerequisite stage completes;
- `target/battery-parallel/`, coordinator-owned, with distinct
  `lane-<index>.edn`, `.out`, and `.err` names inside one run;
- the real battery `$HOME`, notably `~/.m2`, `~/.gitlibs`, and `~/bin`, kept
  shared because the cold launcher tests need the installed caches/tools;
- `/var/tmp/forge` as the explicit parent used by
  `mission-git-boundary-test`; its actual directory is randomly named.

The repository and `target/` snapshots can attribute another lane's concurrent
write as a violation, but the retained wide runs report zero such violations
and the current battery tests use unique temp workspaces. A second coordinator
in the same checkout would collide on `target/battery-parallel/lane-<index>.*`;
the declared battery cadence relies on the external
`flock /home/forge/tmp/suite.lock` for that serialization. This is not a
within-run lane collision and is not a landing blocker, but the lock remains a
required operating condition.

A child that dies without a receipt is RED. `run-lane!` leaves `:emitted` nil;
`lane-failures` emits the named `WITHOUT writing its result` failure, the
namespace is also absent from the union, and both counts feed the coordinator's
nonzero exit. The fast witness covers exit 137 without a receipt. The retained
injected assertion failure is named in `parallel-inject.log`; `make` returned
RC 2 and the ledger recorded `:verdict :fail`.

### Residual serial difference

Landing does **not** need the additional approximately 14-minute
`--emit-edn` manifest-order serial run. The retained evidence already has:

- the original serial target at 743 tests / 13,756 assertions;
- the one-lane LPT coordinator and the 8-lane coordinator equal in every one
  of the 35 namespace rows, including failures, errors, and skips;
- the sharded namespace equal at 7 tests / 74 assertions;
- three green wide runs and one deliberate red run.

The extra serial receipt would improve attribution for manifest order and the
single `test-ns`/`test-vars` comparison, but it would not test the principal
parallel hazard—concurrent shared state—better than the retained wide runs.
The alternate `test-ns-hook` semantic gap found in this review is now refused.
The original `test-battery-serial` target remains available as a control.

### Ledger compatibility

The `:lanes` and `:skipped` additions do not change `battery-fresh`. Ledger
lines are read as EDN maps; `freshness` destructures only its established keys
and ignores the additions. The other in-repository reader extracts only
`:sha`. Old entries are emitted byte-identically because the new keys are
conditional. No in-repository exact-schema reader was found.

## Verification

- Focused JVM witness: 24 tests, 146 assertions, 0 failures, 0 errors, and 0
  isolation violations.
- Battery-ledger compatibility witness: 14 tests, 73 assertions, 0 failures,
  0 errors, and 0 isolation violations.
- `~/bin/clj-kondo` on both changed Clojure files: 0 errors, 0 warnings.
- `make -n test-battery` resolves the reviewed defaults to 8 lanes and
  `BATTERY_PREREQS=1` without executing the battery.
- `git diff --check`: clean.
- Retained evidence inspected: serial baseline 839 s; parallel receipts 267,
  217, and 218 s; per-namespace comparison all zero; injected failure named
  and ledger-red.


> END RECEIPT (fence-run): worktree HEAD at review exit = 13dcc378364ef00a57688c5280a77a9f0e7c63f1 = fenced sha.
