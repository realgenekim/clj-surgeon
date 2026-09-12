# Independent verification — clj-surgeon `fable/data-not-code` @ `7182573b`

**VERDICT: NO-GO** — the F3 hard-link fix is over-broad and regresses the transaction
journal: `clj-surgeon.txn-journal-test` goes from **80 tests / 545 assertions / 0 errors**
(parent `admit-target!`) to **80 tests / 498 assertions / 6 errors** at this tip, every one
an uncaught `:write-outside-envelope :reason :hard-link` on an **in-envelope**
`transactions/LOCK`, in the interrupted-break recovery path. F1 and F2 are genuinely fixed.

Subject: tip `7182573b67f7f02e821b3eb6376f93530b5a2882`, base `3b6d53577be3173cce209934e37bac904b3d38d7`,
prior red-team tip `4658dc124ee243300d19861402b2696ae1046321`.
Worktree `/var/tmp/forge/datacode-fx/verify`, detached, removed at the end; `git status --porcelain`
empty before and after every plant. All JVMs `-Xmx1g`, `TMPDIR=/var/tmp/forge/datacode-fx`,
one at a time, focused namespaces only. Plants made with native form edits and reverted with
`git checkout`. The builder's logs were not used as evidence.

---

## Verdicts

| # | Check | Verdict | Deciding command | Excerpt |
|---|---|---|---|---|
| 1 | **F1** — evidence rows bound to opened receipts | **FIXED** (one residual, N2) | `clojure -M:clj-surgeon/test-deps -e '(load-file ".../vprobe/p1.clj")'` | `A2 clj-surgeon.mcp-recovery-test bb walls [6 6 6 6 7 6] -> 9000s, :runtime :bb->:jvm -> REFUSED {:error-type :invalid-runtime-evidence, :namespace clj-surgeon.mcp-recovery-test, :field [:bb :walls-ms]}` |
| 2 | **F2** — `policy-envelope-roots` rename goes RED by name; count pin present | **FIXED**, pin = **12** | native rename plant of both `defn` + call site, then `run-tests` on `clj-surgeon.receipt-artifacts-boundary-test` | `FAIL … DATACODE-ENV-002: the policy-root function exists` **and** `FAIL … must execute all 12 assertions {:test 1, :pass 9, :fail 1, :error 0}` / `actual: (not (= 12 10))` |
| 3 | **F3** — hard-linked final file refused; symlink still refused; normal publication works | **FIXED for the attack, NOT SAFE** | `.../vprobe/p3f.clj` | `F3a … {:error-type :write-outside-envelope, :reason :hard-link, …}`; `F3c` symlink still refused; `F3d/F3e/F3f` admitted; **but `F3g in-envelope HARD LINK pair, both inside envelope -> refused`** |
| 4 | Preregistration amendments (units, success rule, refusal bet, frozen bytes, T0/T1 runner) | **MOSTLY FIXED** — 2 points still reinterpretable | `sha256sum -c`, `git apply --check` ×6, read of `measure.py` | `T1..T6: before_ok=True apply_check=True after_ok=True`; all six `SHA256SUMS` OK; `measure.py` committed with `init`/`cell`, monotonic T0/T1 |
| 5 | Nothing regressed | **REFUTED** | `run-tests 'clj-surgeon.txn-journal-test` with `CLJ_SURGEON_MEMORY_TMP` inside the envelope, tip vs parent `receipt_artifacts.clj` | tip `Ran 80 tests containing 498 assertions. 0 failures, 6 errors.` · parent `Ran 80 tests containing 545 assertions. 0 failures, 0 errors.` |

### Check 1 detail — every attack, one probe run

```
S1 shipped 38 rows as committed -> rows=38 receipts=456 :ACCEPTED
A1 nonexistent receipt path -> REFUSED {:namespace clj-surgeon.mcp-recovery-test, :runtime :bb,
     :field [:bb :logs], :error-type :missing-evidence-receipts, :path "does/not/exist.edn"}
A2 mcp-recovery-test  walls->9000, stats recomputed, bb->jvm, receipts untouched
     -> REFUSED {:error-type :invalid-runtime-evidence, :namespace …, :field [:bb :walls-ms]}
A2 battery-ledger-test same forgery -> REFUSED  {… :field [:bb :walls-ms]}
A5 mean-only altered          -> REFUSED {… :field [:bb :mean-ms]}
A6 one :logs element dropped  -> REFUSED {… :field :n}
```

The red-team's exact forged row is refused and the refusal names **both** the namespace and the
field. A row citing a nonexistent path is refused. All 38 shipped rows / 456 receipts are opened
and admitted.

**The NEW attack (forge the receipt file too) succeeds, as the builder predicts:**

```
A3 battery-ledger-test row AND its 6 bb receipts both say 9000ms, runtime bb->jvm -> :ACCEPTED
    resulting runtime for clj-surgeon.battery-ledger-test = :jvm
```

**The claim is NOT stated in `DATACODE-ROWS-001`** — see N3.

### Check 4 detail — what the amendment fixed, verified independently

| F7 point | Amended? | Evidence |
|---|---|---|
| 1. sd-in-ms vs dimensionless ratio | **Yes** | "Primary P/N … is the ratio of medians of complete request-to-verdict wall in ms"; noise gate restated in ms: `median(NATIVE ms) − median(PROBE ms) > 2 × sd(native controls ms)`; "never compare an sd in ms directly with a ratio" |
| 2. no decision rule for "equal first-attempt success" | **Yes** | "equal first-attempt success means both arms are 6/6 on the six valid targets"; both bettors miss if either arm is below 6/6 or they differ in any repetition |
| 3. Fable's refusal bet unfalsifiable | **Half** | two planted non-test targets N1=`clj-surgeon.forms`, N2=`clj-surgeon.analyze`; PROBE half is falsifiable, **NATIVE half is not** — see N4 |
| 4. subject SHA + patch bytes not frozen | **Patches yes, SHA no** | six patches + `SHA256SUMS` committed; I verified all six hashes, `git apply --check`, and `after_sha256` after a real apply+revert. Declared assertion deltas 1,0,1,1,0,2 are correct (T6's `doseq` body carries two `is` forms, so one new row = 2 executed assertions). Subject SHA is pinned at runtime — see N6 |
| 5. T0/"verdict understood" uninstrumented | **Yes** | `round5/measure.py` committed; one runner for both arms, `time.monotonic_ns()`, T0 immediately before the frozen edit, T1 after `verdict.json` is written+fsynced, verdict parsed from actual EDN rather than exit status |
| 6. only the JVM stratum discriminates | **Disclosed** | "Both bettors predict ≤ 0.50 for bb-portable; that stratum alone cannot distinguish their bets" |

### Check 5 detail — the regression, and what did not regress

| Check | Command | Result |
|---|---|---|
| **txn journal, tip** | `run-tests 'clj-surgeon.txn-journal-test`, `CLJ_SURGEON_MEMORY_TMP=/var/tmp/forge/datacode-fx/vprobe/txn` | **`Ran 80 tests containing 498 assertions. 0 failures, 6 errors.`** |
| **txn journal, control** | same, after `git checkout 4658dc12 -- src/clj_surgeon/receipt_artifacts.clj` | **`Ran 80 tests containing 545 assertions. 0 failures, 0 errors.`** |
| envelope witnesses | `run-tests 'clj-surgeon.receipt-artifacts-boundary-test` | `Ran 31 tests containing 268 assertions. 0 failures, 0 errors.` |
| sandbox-classification + evidence-rows + sleep-identity | `run-tests 'clj-surgeon.xray-test 'clj-surgeon.lane-manifest-test` | `Ran 59 tests containing 1898 assertions. 0 failures, 0 errors.` |
| deftest census, source | `git grep '^(deftest ' base\|tip`, `comm` under `LC_ALL=C` | `base=2620 tip=2631 REMOVED=0 ADDED=11` |
| deftest census, declared file | `git diff … -- test/clj_surgeon/deftest_census.edn` | `+11 −0` base→tip, `+3 −0` for the round-5 commit; names agree with source exactly |
| `ns_isolation.clj` budget | `git diff 3b6d5357..7182573b -- test/clj_surgeon/ns_isolation.clj \| wc -l` | `0` |
| prewarm receipt binds to this tip | recomputed `source-digest` over `Makefile deps.edn bb.edn src test resources docs/intent` | `44e8998d7eca6d1379aaff333e336c82d06d178ab01b46359592afcb3ffb87ce` = the receipt's value. Content-bound to this tip (its `:git-head`/`:git-tree` name the parent, see N10) |

`clj-surgeon.xray-test` is a `:bb` namespace; like the prior review I exercised it on the JVM only.

---

## New findings, by severity

### N1 — HIGH. The F3 hard-link check refuses the transaction journal's own in-envelope LOCK, breaking interrupted-break recovery.
`src/clj_surgeon/receipt_artifacts.clj:126-134` refuses **any** final regular file whose
`unix:nlink > 1`, regardless of where the other link points. The txn journal's lock protocol is
built on `Files/createLink` precisely so acquisition and break are create-if-absent in the kernel
(`src/clj_surgeon/txn_journal.clj:1177`, `:1361`, and its own docstring at `:1165-1174`), and an
**interrupted break legitimately leaves `LOCK` and `LOCK.broken.*` as two names for one inode** —
the state `interrupted-break-files` and `recover!` exist to resolve (`txn_journal.clj:1011-1019`).
The journal then admits that same path through `artifacts/admitted-file` at
`txn_journal.clj:339` (`lock-file`) and `:1632`, so admission now throws on the recovery path.

Tip, with the temp root inside the envelope so only the link check can fire:

```
Ran 80 tests containing 498 assertions.
0 failures, 6 errors.
```

All six are uncaught `:write-outside-envelope … :reason :hard-link` on
`…/transactions/LOCK`, whose `:path` and `:resolved-path` are **identical and inside
`:roots`**:

- `test/clj_surgeon/txn_journal_test.clj:1779` `a-receipt-never-names-a-file-that-is-not-there`
- `:2111` `the-sweep-counts-corroborated-and-uncorroborated-interrupted-breaks-apart`
- `:2165` `a-break-interrupted-between-the-link-and-the-unlink-is-not-a-break`
- `:2218` `an-interrupted-break-of-a-LIVE-holders-lock-is-reverted`
- `:3143` `an-interrupted-break-stays-typed-after-its-holder-releases-the-lock`
- `:3237` `recovery-resolves-every-interrupted-break-in-one-call`

Reverting **only** `src/clj_surgeon/receipt_artifacts.clj` to the parent commit restores
`80 tests / 545 assertions / 0 failures / 0 errors` on the same tree, same environment. The
regression is this commit's, isolated to that one file.

Why every round-5 receipt stayed green: `clj-surgeon.txn-journal-test` is a **`:battery`-lane**
namespace (`test/clj_surgeon/lane_manifest.clj`), so it is not in `make test-fast`; it is
`BB-LOAD-EXCLUDED` from `test-bb`; and `make landing-gate-prewarm` is documented as the
"complete coordinator gate **except battery-fresh**" (`Makefile:109`) — its seven stages run
`test/admit_transaction_recovery_battery.clj`, a different artifact. The namespace that
exercises the lock protocol was run by neither the builder nor the prior red team.

Reproduce: `CLJ_SURGEON_MEMORY_TMP` must point inside the envelope roots, or the 74-error
root-check noise (the test defaults to a hardcoded `/home/forge/tmp`,
`txn_journal_test.clj:23-25`) masks the six.

**Fix direction:** the link check must compare where the *other* links point, not the count —
e.g. refuse only when the inode is reachable from outside the envelope, or exempt paths the
journal owns; a bare `nlink > 1` predicate cannot distinguish an escape from the tool's own
create-if-absent primitive.

### N2 — MEDIUM. F1's residual: `:portability-state` and `:contract-failure` are unbacked row fields that still change which runtime a namespace runs on, with receipts untouched.
`test/clj_surgeon/lane_manifest.clj:307-310` derives the expected runtime from
`(:portability-state row)`, `(:contract-failure row)` and `(:failed-samples row)`. Only the
statistics are bound to receipts; these three are row assertions nothing opens. The dangerous
direction works:

```
    shipped: ratio=1.0722727922425561 portability-state=:contract-failed runtime=:jvm
A4a clj-surgeon.intent-transaction-test: drop :contract-failure, :contract-failed->:portable,
    :runtime :jvm->:bb (receipts UNTOUCHED) -> runtime now :bb  :ACCEPTED
A4c clj-surgeon.battery-ledger-test: :portability-state :unverified + :runtime :bb->:jvm
    (receipts UNTOUCHED) -> runtime now :jvm  :ACCEPTED
```

`clj-surgeon.intent-transaction-test`'s conservative ratio is 1.07 (≤ 2.0), so the **only**
thing keeping it off Babashka is a prose `:contract-failure` string in the row. Deleting two
keys rehabilitates a namespace that was declared contract-failed onto bb, and the validator
that "refuses any statistic or selected-runtime disagreement" accepts it — the internal
consistency check `A4b` (leaving `:runtime :jvm`) correctly refuses, which is what makes the
forgery cheap: the attacker just flips the runtime too. This is the same class as F1 through a
field the fix did not cover.

### N3 — MEDIUM. The "receipts are evidence, not attestation" claim is nowhere in `DATACODE-ROWS-001`; and `:logs` may cite receipts outside version control.
The canonical intent file `docs/intent/test-isolation/test-isolation-specs.md:11` was **not
amended at all** in round 5 — it still reads "Missing receipts or inconsistent
statistics/assignments refuse by namespace," with no mention of opening receipts, binding
elapsed walls, or the limits of that binding. `docs/observations/2026-09-12-data-not-code/round3/requirements.md:37-45`
was amended and states the new bindings, but says nothing about tampering. The sentence
"Receipt contents remain evidence inputs, not cryptographic provenance: coordinated receipt
tampering is not attestation" exists **only** in `round5/REPORT.md` prose. The requested
confirmation fails: the limit is not stated in the intent it qualifies.

Sharper than "coordinated tampering": nothing constrains `:logs` to the committed measurements
tree. My A3 receipts were written to `/var/tmp/forge/datacode-fx/vprobe/forged-receipts/` and
were accepted — so the forgery does not even require editing a committed file, only adding
untracked ones and repointing `:logs`. A cheap ratchet: require every `:logs` path to be under
`docs/observations/…/measurements/` and tracked by git.

### N4 — MEDIUM. Fable's amended refusal bet is still half unfalsifiable, by construction of the instrument.
The bet is now "Exactly 2/2 typed non-test refusals; **NATIVE 0/2**"
(`round3/probe-measure-preregistration.md`, Bets table). In `round5/measure.py:118-129`, a cell
is scored `refused` only when `kind = receipt['error-type']` is non-nil. The NATIVE arm's
receipt is the `clojure.test` summary map (`{:test :pass :fail :error :type}`), which never
carries an `error-type`; and for N1/N2 the `row` is `None`, so the `test > 0` requirement is
waived and a zero-test run scores `accepted`. **NATIVE can never be recorded as refusing**, so
"NATIVE 0/2" holds for every possible execution. This is the same defect F7#3 named
(`0 ≥ 0` always holds), relocated. The PROBE half (2/2 typed refusals) is genuinely falsifiable.

### N5 — MEDIUM. The primary measure changed meaning in round 5; the bet numbers did not.
Before: *"The observer records T0 (task delivered)… Primary wall is T0 through the accepted
verdict; it **includes model returns, editing**, client startup, reload, execution,
interpretation and any refusal/recovery."*
After: *"T0 is the runner timestamp immediately before it applies the exact frozen edit…
Human/model preparation happens before this mechanical request and is reported separately."*

The edit is now `git apply` of a frozen patch, so the shared human/model/editing cost that both
arms used to carry has been removed from the primary interval. The bets (`≤ 0.50`, `≤ 0.30`,
`≤ 0.45`) were carried over unchanged. Removing a shared constant from both numerator and
denominator moves the ratio **toward the faster arm** — i.e. the amendment makes the tool's own
bets easier to win, and nothing in the document says the bounds were re-derived for the new
definition. This is the reinterpretable point a later scorer is most likely to be argued with.

### N6 — LOW. The subject SHA is still not frozen in the preregistration.
`measure.py:57,64-69` reads `git rev-parse HEAD` from whatever `--checkout` is passed at `init`
and writes it to `subject.json` under `/var/tmp/forge/probe-measure-fx` — scratch, uncommitted.
The self-reference problem is real and the resolution is defensible, but the consequence is that
the preregistration does not by itself identify its subject, and the artifact that does is not
under version control. Cheap fix: after the landing commit exists, append its SHA to the
preregistration in a follow-up commit, and have `measure.py` compare against that literal.

### N7 — LOW. The hard-link refusal reports the wrong kind, and leaks onto unrelated refusals.
`receipt_artifacts.clj:129-134` folds the link check into the envelope predicate, so a path that
is demonstrably **inside** `:roots` is refused as `:write-outside-envelope` with `:path` equal to
`:resolved-path` equal to an in-envelope path (`F3g`, and every N1 error). A reader of that
receipt cannot tell an escape from an over-refusal. Conversely `F3h` shows `:reason :hard-link`
attached to a genuinely-outside refusal whose target merely happened to have `nlink > 1`. If the
check survives in any form it needs its own error kind.

### N8 — LOW. `unix:nlink` is a POSIX-view-only attribute.
`Files/getAttribute resolved "unix:nlink"` throws `UnsupportedOperationException` on a
`FileSystem` provider without the unix view. In `admit-target!` that escapes as an untyped
exception rather than a typed refusal, so a provider difference turns the admission boundary
from "refuse" into "crash." Darwin remains unproved, as the round-5 report concedes.

### N9 — INFORMATIONAL. Unintended comment re-indentation in the tip commit.
`test/clj_surgeon/lane_manifest.clj:313, 349, 373, 410, 427` — five top-level `;; @spec`
comments moved from column 0 to column 2 in this commit. Harmless: the marker regex
(`src/clj_surgeon/mcp_intent_contract.clj:13`) is not line-anchored, and `intent-audit` passed
in the prewarm. It is formatting drift in a commit that otherwise claims a narrow scope.

### N10 — INFORMATIONAL. The prewarm receipt's commit fields name the parent; its content digest names this tip.
`round5/prewarm-receipt.edn` carries `:git-head "4658dc12…"` and
`:git-tree "249dd3d3…"` — and `249dd3d3` is exactly `4658dc12^{tree}`, because the runner takes
both from `git rev-parse HEAD` / `HEAD^{tree}` (`test/clj_surgeon/battery_parallel_runner.clj:840-841`)
and the commit was made afterwards. The binding evidence is `:source-digest`, which I
recomputed independently over the runner's exact path set
(`battery_parallel_runner.clj:685-694`) and reproduced byte-exactly. So the gate did run on this
tip's content. It nevertheless does not cover the battery lane, which is where N1 lives.

---

## What I could not verify, and why

- **Darwin / non-ext4 behaviour** of `unix:nlink`, `toRealPath` and `passwd-home`. No Mac reachable.
- **The `:bb` arm of the sandbox classifier** — `xray-test` exercised on the JVM only.
- **Whether the six preregistered patches are of comparable difficulty.** Bytes and hashes are
  frozen and verified; difficulty is not a property I can measure from the tree.
- **Any probe-vs-native performance number.** None run, none claimed; no arm executed.
- **TOCTOU between admission and publication** — explicitly out of scope per the design doc,
  and unchanged by this commit (the link check is as check-then-act as the root check).
- **The full battery lane.** I ran the one namespace the change implicates. Other `:battery`
  namespaces that admit artifacts were not run and should be before this lands.

## Recommended before landing

1. **N1 (blocking).** Replace the `nlink > 1` predicate with one that asks whether the inode is
   reachable from outside the envelope, or exempt the paths the journal publishes through its
   own create-if-absent primitive. Then run `make test-battery` (or at minimum
   `clj-surgeon.txn-journal-test`) and pin the interrupted-break recovery cases as the witness.
2. **N1 corollary (blocking).** A change to `admit-target!` must run the battery lane. The
   round-5 gate evidence does not reach it, and both reviews missed it for the same reason.
3. **N2.** Bind `:portability-state` / `:contract-failure` to something, or state in
   `DATACODE-ROWS-001` that they are declared policy that a reviewer must read — and add a
   witness that flips them and asserts the refusal, so the gap is at least named.
4. **N3.** Move the attestation limit and the `:logs` path constraint into the intent text, and
   amend `docs/intent/test-isolation/test-isolation-specs.md:11`, which round 5 left untouched.
5. **N4/N5.** Either re-derive the bet bounds for the narrowed interval or restore the broad one,
   and give the NATIVE arm a way to be scored as refusing.
