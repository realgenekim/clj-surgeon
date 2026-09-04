## GO-WITH-FIX

*Round-eleven admit gate, clj-surgeon `bridge/admit-gate-r3` at `612ea68c`. Sol's review stopped
after item 2 (content filter); this completes it. Findings continue from Sol's numbering. Sol's
item 1 (provenance) stands. Sol's item 2 is re-verified as finding 3 below, together with **four
sibling throw sites and one classification hole Sol did not reach** — the class is "the checker can
exit through a path that is not one of its three states."*

Review clone `/home/forge/tmp/sol/gate4-wt` at `612ea68c0702292984835d251f714fc9ee713cf1`,
`git status --porcelain` empty at start and end. Fixtures under `/var/tmp/forge/gate11-opus-fx`
(never `/tmp`), removed at the end. No ports opened; 7888/7890/7894/7895 never contacted. Nothing
committed, staged, stashed or pushed.

**Apparatus note, so the numbers are readable.** My first "fresh clone" was a `git archive` export,
and `clj-surgeon.repository-hygiene-test:64` correctly fails closed without a `.git` directory
(`git is unusable in …, so repository hygiene cannot be observed`), giving 763/10579/**1**. Every
figure below is from a real `git clone --no-local` of the review worktree checked out at
`612ea68c`. This is my apparatus, not a finding against the tip; I record it because round nine's
export-based receipts would not reproduce here.

---

### 3. FIX REQUIRED — the classifier has FIVE ways to exit that are none of its three states, not one. Sol's item 2 is one of them, and the fix Sol proposes does not reach the other four.

`test/clj_surgeon/admit_patch_test.clj:85-156` (`classify-battery-receipt`) and `:158-183`
(`check-battery-precondition!`). MCP-OP-ADMIT-152's claim is that a receipt that EXISTS is either
SATISFIED or FAILED, and that every FAILED one is counted in `precondition-failures` and printed
with its clearing command. Five present-receipt shapes falsify that. I drove the exact production
function (not a re-implementation) with reports captured and buckets restored.

Exact command:

```text
~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate11-opus-fx/shapes.clj'
```

Verbatim output (the five escapes; the ten well-behaved shapes are in finding 7):

```text
{:case :string-key-8, :result {:THREW "java.lang.ClassCastException", :message "class java.lang.String cannot be cast to class java.lang.Number (java.lang.String and java.lang.Number are in module java.base of loader 'bootstrap')"}, :report-types {}, :new-failures [], :new-skips []}
{:case :S1-mixed-failing-arms, :result {:THREW "java.lang.ClassCastException", :message "class java.lang.String cannot be cast to class java.lang.Number (java.lang.String and java.lang.Number are in module java.base of loader 'bootstrap')"}, :report-types {}, :new-failures [], :new-skips []}
{:case :S2-arms-not-seqable, :result {:THREW "java.lang.RuntimeException", :message "Unable to convert: class java.lang.Long to Object[]"}, :report-types {}, :new-failures [], :new-skips []}
{:case :S4-kinds-not-seqable, :result {:state :satisfied}, :report-types {:error 1, :pass 2}, :new-failures [], :new-skips []}
READ-EVAL-EXECUTED-ARBITRARY-CODE
{:case :S5-read-eval, :result {:state :absent}, :report-types {:pass 3}, :new-failures [], :new-skips ["no battery receipt at /var/tmp/forge/gate11-opus-fx/shape-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone"]}
{:case :S6-deep-nesting, :result {:THREW "java.lang.StackOverflowError", :message nil}, :report-types {}, :new-failures [], :new-skips []}
```

The five, with their sites:

- **`:136` — Sol's case, re-verified.** `{"8" true, 32 true, 64 true}` reaches
  `(pr-str (vec (sort (keys verdicts))))` in the key-set branch and throws.
- **`:106` — a SECOND, EARLIER sort, which Sol's proposed fix at 136 does not reach.**
  `{"8" false, 32 false, 64 true}` throws in the `failing` binding *inside the `let`*, which is
  eager: it runs before the `cond` is entered at all, so no branch can catch it and no reordering
  of the `cond` helps. Proved distinct by stack frame, same command with the frames filtered:

  ```text
  {:case :string-key-8, ..., :top-frames ["clj_surgeon.admit_patch_test$classify_battery_receipt.invokeStatic(admit_patch_test.clj:136)" ...]}
  {:case :S1-mixed-failing, ..., :top-frames ["clj_surgeon.admit_patch_test$classify_battery_receipt.invokeStatic(admit_patch_test.clj:106)" ...]}
  ```

- **`:122` — `(vec (:arms record))` on a non-seqable `:arms`.** `{:arms 3, …}` throws
  `RuntimeException: Unable to convert: class java.lang.Long to Object[]`. (A `:arms` that is a
  *set* is handled correctly and lands in FAILED — see finding 7 — so this is specifically the
  non-seqable case.)
- **`:174` — `(set (:kinds-published record))` in the SATISFIED branch.** `:kinds-published 7`
  returns **`:state :satisfied`** and then throws inside the `is`. `clojure.test` records it as an
  `:error`, so the lane is non-green — but the receipt was classified **satisfied**, nothing enters
  `precondition-failures`, and the summary line prints `0 preconditions failed` with no clearing
  command. This is the same shape as Sol's item 2 with a worse verdict attached.
- **`:166` — an `Error` escapes `(catch Exception e)`.** A 60,000-deep nested receipt throws
  `StackOverflowError` out of the reader; the `::unreadable` path never fires.

Severity, stated plainly: **none of these buys a green.** `clojure.test` catches `Throwable` around
each `deftest`, so every throw becomes a counted `:error` and the lane exits nonzero. What they
falsify is MCP-OP-ADMIT-152's *stronger* claim — that every present, incomplete receipt is counted
in its own bucket and printed with the command that clears it. Fix all five in the round-twelve
worktree already open on `:136`: give the two sorts a total comparator (or drop the sort), make
`:arms`/`:kinds-published` coercions total, and catch `Throwable` rather than `Exception` at
`:166`. Add each of the five to the witness — the witness at `:5624` is the right place and already
has the harness for it.

### 4. FIX REQUIRED — a receipt that is PRESENT but reads as `nil` is classified ABSENT: a counted SKIP, exit 0, under a message that states the file does not exist.

`test/clj_surgeon/admit_patch_test.clj:165-167,111-112`. `check-battery-precondition!` computes
`record` as `(when (.exists receipt) (try (read-string (slurp receipt)) …))` and
`classify-battery-receipt` decides ABSENT on `(nil? record)`. **The absent test is by VALUE, not by
EXISTENCE**, so a present file whose content reads as `nil` takes the fresh-clone skip. This
contradicts the function's own docstring — *"Everything else that EXISTS is FAILED … never fall back
to the absent state's skip"* — and it is the only shape in this review that ends **exit 0**.

The neighbouring shapes are all handled correctly, which is what makes this a hole rather than a
policy: an empty file, whitespace, and a comment-only file are each FAILED.

Exact command:

```text
~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate11-opus-fx/shapes2.clj'
```

Verbatim output:

```text
{:case :present-file-reading-as-nil, :file-exists true, :result {:state :absent}, :report-types {:pass 3}, :new-skips 1, :new-failures 0}
{:case :present-file-empty-string, :file-exists true, :result {:state :failed}, :report-types {:fail 1, :pass 2}, :new-skips 0, :new-failures 1}
{:case :present-file-whitespace, :file-exists true, :result {:state :failed}, :report-types {:fail 1, :pass 2}, :new-skips 0, :new-failures 1}
{:case :present-file-comment-only, :file-exists true, :result {:state :failed}, :report-types {:fail 1, :pass 2}, :new-skips 0, :new-failures 1}
```

Confirmed through the real lane, not only the isolated driver — in the clone whose battery genuinely
failed 2/3, with the receipt replaced by the three bytes `nil`:

```text
cd /var/tmp/forge/gate11-opus-fx/redbattery && echo -n 'nil' > target/admit-transaction-recovery-battery-receipt.edn && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate11-opus-fx/run-admit.clj'; echo "EXIT_CODE=$?"
```

```text
Ran 166 tests containing 4268 assertions.
0 failures, 0 errors.
1 preconditions skipped.
  SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
0 preconditions failed.
{:test 166, :pass 4268, :fail 0, :error 0, :precondition-skipped 1, :type :summary}
EXIT_CODE=0
```

**Why I am NOT calling this blocking.** The green it produces is the *same* green the honest absent
state produces — a counted, printed skip that `make test` drives to zero. An attacker who can write
`nil` into `target/` can equally well `rm` the file and get the identical exit 0 with a *true*
message. So this does not let a red battery buy anything it could not buy more easily by deletion,
and round nine's hole (a red battery's archive *suppressing* the skip) stays closed. What it costs
is honesty: the summary line asserts `no battery receipt at target/…` about a file that exists, and
that is precisely the class of claim this lane was built to make impossible. One-line fix, same
function the round-twelve builder is in: decide ABSENT on `(.exists receipt)`, and let a present
file that reads as `nil` fall through to FAILED with the "not a map" reason that is already written.

### 5. FIX REQUIRED (hardening) — the receipt is read with `clojure.core/read-string`, so `*read-eval*` is ON and a receipt file executes arbitrary code inside the gate.

`test/clj_surgeon/admit_patch_test.clj:166`. A receipt beginning `#=(…)` is evaluated by the reader
during classification. In the run above, a receipt containing
`#=(clojure.core/println "READ-EVAL-EXECUTED-ARBITRARY-CODE")` printed that string from inside the
checker — and, because the `#=` form returned `nil` and `read-string` reads only the first form, the
same receipt then took the ABSENT skip of finding 4. Verbatim, from the finding-3 output:

```text
READ-EVAL-EXECUTED-ARBITRARY-CODE
{:case :S5-read-eval, :result {:state :absent}, :report-types {:pass 3}, ...}
```

Blast radius is a developer's own gitignored `target/`, so I rank this hardening rather than a
vulnerability. It is still a one-word fix — `clojure.edn/read-string` — and it closes finding 3's
StackOverflow case at the same time by refusing the construct instead of evaluating it.

### 6. PASS — the three-state table reproduces on a fresh clone exactly as claimed, with identical assertion counts in every state.

Clone: `git clone --no-local /home/forge/tmp/sol/gate4-wt`, `git checkout 612ea68c`,
`git status --porcelain` empty, no `target/`.

**A · absent.** `~/bin/suite-run clojure -M:clj-surgeon/mcp-test; echo EXIT_CODE=$?`

```text
Ran 763 tests containing 10582 assertions.
0 failures, 0 errors.
1 preconditions skipped.
  SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
0 preconditions failed.
EXIT_CODE=0
```

**B · genuine battery, then the same lane.** `~/bin/suite-run make admit-transaction-recovery-battery`

```text
PASS n=8 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=146
PASS n=32 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=170
PASS n=64 attempts=1 kind=:transaction-recovery-required source-unchanged=false enumerated=true wall-ms=209
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
BATTERY_EXIT_CODE=0
```

```text
Ran 763 tests containing 10582 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
EXIT_CODE=0
```

**C · round nine's exact attack, the n=8 arm forced red** (`hit? (and (not= n 8) …)` at
`test/admit_transaction_recovery_battery.clj:149`, in its own clone):

```text
admit-transaction-recovery-battery: 2/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :failed · 2/3 arms passed · failed arms [8] · kinds #{:transaction-recovery-required}
make: *** [Makefile:215: admit-transaction-recovery-battery] Error 1
BATTERY_EXIT_CODE=2
```

```text
Ran 763 tests containing 10582 assertions.
1 failures, 0 errors.
0 preconditions skipped.
1 preconditions failed.
  FAILED · battery receipt at target/admit-transaction-recovery-battery-receipt.edn is PRESENT but does NOT record a complete run · the battery did NOT pass every arm: 1 of 3 failed · failing arms [8] · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane
EXIT_CODE=1
```

Round nine's finding 1 is closed at the tip: the red battery's archive can no longer buy `0
preconditions skipped, exit 0`. 10,582 assertions in all three states, so the count still does not
reveal which machine ran the battery (MCP-OP-ADMIT-147).

### 7. PASS — every equality attack the addendum lists fails CLOSED, each with a distinct, accurate reason.

Same command as finding 3. Verbatim:

```text
{:case :permutation, :result {:state :failed}, :report-types {:fail 1, :pass 2}, :new-failures ["battery receipt at … is PRESENT but does NOT record a complete run · the receipt declares arms [64 32 8] but the battery script declares [8 32 64] · a receipt may not shrink its own subject · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane"], :new-skips []}
{:case :extra-arm, :result {:state :failed}, ... "the receipt declares arms [8 32 64 128] but the battery script declares [8 32 64] · a receipt may not shrink its own subject ..."}
{:case :extra-arm-verdicts-only, :result {:state :failed}, ... "the receipt records verdicts for [8 32 64 128] but the battery declares [8 32 64] ..."}
{:case :missing-arm-verdicts-count-ok, :result {:state :failed}, ... "the receipt records no per-arm verdict (`:arm-verdicts`), so it cannot show that every arm passed · it reports :arms-passed 3 of 3 ..."}
{:case :older-shape, :result {:state :failed}, ... "the receipt records no per-arm verdict (`:arm-verdicts`) ... it reports :arms-passed 3 of 3 ..."}
{:case :round-nine-red, :result {:state :failed}, ... "... it reports :arms-passed 2 of 3 ..."}
{:case :unreadable, :result {:state :failed}, ... "the receipt could not be read: \"EOF while reading\" ..."}
{:case :S3-arms-a-set, :result {:state :failed}, ... "the receipt declares arms #{32 64 8} but the battery script declares [8 32 64] ..."}
{:case :complete, :result {:state :satisfied}, :report-types {:pass 3}, :new-failures [], :new-skips []}
{:case :absent, :result {:state :absent}, :report-types {:pass 3}, :new-failures [], :new-skips ["no battery receipt at …"]}
```

Note the **permutation** ruling in particular: `:arms [64 32 8]` is rejected because the comparison
is `(not= (vec declared-arms) (vec (:arms record)))` — order-sensitive on `:arms` while the verdict
key set is compared as a set. That asymmetry is stricter than the specification's word "equal" and
is therefore fail-closed in the right direction; I raise no objection, but it is a deliberate
choice worth one line of comment, because the battery writes `arms` from a literal vector and a
future reordering of that literal would red the lane until the battery is re-run.

### 8. PASS — the subject genuinely comes from the battery SCRIPT, not from a list hardcoded beside the checker. A receipt cannot shrink its subject because it does not name it.

`test/clj_surgeon/admit_patch_test.clj:5570-5582` parses `(def arms [...])` out of the script at run
time. I falsified the alternative directly: in a clone at `612ea68c` I widened only the script's
declaration to `(def arms [8 32 64 128])` and left the genuine 3-arm passing receipt in place.

Exact commands:

```text
sed -i 's/^(def arms \[8 32 64\])/(def arms [8 32 64 128])/' test/admit_transaction_recovery_battery.clj
~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate11-opus-fx/run-admit.clj'; echo "EXIT_CODE=$?"
```

Verbatim output:

```text
0 preconditions skipped.
1 preconditions failed.
  FAILED · battery receipt at target/admit-transaction-recovery-battery-receipt.edn is PRESENT but does NOT record a complete run · the receipt declares arms [8 32 64] but the battery script declares [8 32 64 128] · a receipt may not shrink its own subject · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane
{:test 166, :pass 4267, :fail 1, :error 0, :precondition-failed 1, :type :summary}
EXIT_CODE=1
```

### 9. PASS — no re-implementation. The witness drives the exact function the lane runs.

```text
grep -n "check-battery-precondition!\|classify-battery-receipt" test/clj_surgeon/admit_patch_test.clj
```

```text
85:(defn classify-battery-receipt
158:(defn check-battery-precondition!
170:        (classify-battery-receipt record declared-arms)]
5579:        (check-battery-precondition!
5604:  "Run `check-battery-precondition!` on `content` in complete isolation.
5624:                      (check-battery-precondition!
```

Two call sites only: the lane's exemption witness at `:5579` and the new witness at `:5624`; there
is exactly one classifier and one caller of it. Nothing else in the repository reads the receipt
file except the battery that writes it:

```text
rg -n "admit-transaction-recovery-battery-receipt" --include=*.clj --include=Makefile
test/admit_transaction_recovery_battery.clj:116
test/clj_surgeon/admit_patch_test.clj:5580
test/clj_surgeon/admit_patch_test.clj:5614
```

One non-blocking note: the witness at `:5624` passes `[8 32 64]` as its declared arms rather than
parsing the script the way the lane does. That is defensible — the witness tests classification, and
finding 8 covers the parse independently — but the two would drift silently if the script's arms
ever change.

### 10. PASS — the sabotage reproduces, at exactly the claimed 18 failures, with BOTH receipts.

Round nine's hole put back in a clone (`classify-battery-receipt` reduced to
`(if (nil? record) {:state :absent} {:state :satisfied})`), then the focused lane run twice.

With the **passing** receipt:

```text
Ran 166 tests containing 4268 assertions.
18 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
{:test 166, :pass 4250, :fail 18, :error 0, :type :summary}
EXIT_CODE=1
```

With the **failed (2/3, `:verdict :failed`, `:failed-arms [8]`)** receipt — the exact shape round
nine's forced-red battery writes:

```text
Ran 166 tests containing 4268 assertions.
18 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
{:test 166, :pass 4250, :fail 18, :error 0, :type :summary}
EXIT_CODE=1
```

All 18 in one test, and it is the new one:

```text
grep "^FAIL in" … | sed 's/ (admit.*//' | sort | uniq -c
     18 FAIL in (a-receipt-from-a-failed-battery-is-a-failed-precondition-not-a-green)
```

Round nine's hole is visibly back under the sabotage and is caught **only** by the new witness —
which is the honest reading: nothing else in 166 tests notices.

### 11. PASS — the RED→GREEN pair is genuine and red for exactly the stated reason.

`4e5950b4` ("the classifier it consults is a deliberate stub: present ⇒ satisfied, which is today's
behaviour"), with a genuine passing receipt present:

```text
Ran 166 tests containing 4268 assertions.
18 failures, 0 errors.
{:test 166, :pass 4250, :fail 18, :error 0, :type :summary}
EXIT_CODE=1
     18 FAIL in (a-receipt-from-a-failed-battery-is-a-failed-precondition-not-a-green)
```

`612ea68c`, same command, same clone shape: `{:test 166, :pass 4268, :fail 0, :error 0}`,
`EXIT_CODE=0`. The RED sha's 18 failures are byte-identical in count and location to the sabotage of
finding 10, which independently confirms the sabotage restores the pre-152 behaviour rather than
some third state.

### 12. PASS — every claimed gate reproduces at the tip, on the fresh clone.

| gate | claimed | measured | exit |
|---|---|---|---|
| `clojure -M:clj-surgeon/mcp-test` (absent) | 763 / 10582 / 0, 1 skipped | 763 / 10582 / 0, 1 skipped, 0 failed | 0 |
| `clojure -M:clj-surgeon/mcp-test` (after battery) | 763 / 10582 / 0, 0 skipped | 763 / 10582 / 0, 0 skipped, 0 failed | 0 |
| `clj-surgeon.admit-patch-test` alone | 166 / 4268 / 0 | 166 / 4268 / 0 | 0 |
| `bb test/run_all.clj` | 814 / 6724 / 0 | 814 / 6724 / 0 | 0 |
| `make mcp-operation-oracle` | pass | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | 0 |
| intent audit | 372 / 0 | `{:ok true, :specs 372, :violations 0}` | 0 |
| `make admit-transaction-recovery-battery` | 3/3, `:verdict :passed` | 3/3, `verdict :passed · 3/3 arms passed` | 0 |
| `make admit-analyzer-memory-self-test` | 3/3 at `-Xmx512m` | `3/3 arms passed at -Xmx512m` (peak 35/78/83 MiB) | 0 |

Audit command, verbatim:

```text
~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (count (:violations r))}))"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:ok true, :specs 372, :violations 0}
EXIT_CODE=0
```

### 13. RULING — failing closed on a stale receipt in a developer's `target/` is ACCEPTABLE, because the remedy is one line, it is printed, and it is executable unchanged.

I ran it as a reader would. A pre-152 receipt planted in a good clone reds the lane with a named
command; running that command verbatim clears it; the same lane is then green.

```text
Ran 166 tests containing 4268 assertions.
1 failures, 0 errors.
0 preconditions skipped.
1 preconditions failed.
  FAILED · battery receipt at target/admit-transaction-recovery-battery-receipt.edn is PRESENT but does NOT record a complete run · the receipt records no per-arm verdict (`:arm-verdicts`), so it cannot show that every arm passed · it reports :arms-passed 3 of 3 · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane
EXIT_CODE=1
```

```text
~/bin/suite-run make admit-transaction-recovery-battery
admit-transaction-recovery-battery: 3/3 arms passed
battery receipt · target/admit-transaction-recovery-battery-receipt.edn · verdict :passed · 3/3 arms passed · kinds #{:transaction-recovery-required}
BATTERY_EXIT_CODE=0
```

```text
Ran 166 tests containing 4268 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
EXIT_CODE=0
```

The alternative — silently accepting a receipt written by a shape whose honesty rules did not exist
— is the round-nine defect with a longer fuse. `Makefile:987` runs the battery before `mcp-test` on
`make test`, so the canonical lane self-heals and only a raw `clojure -M:clj-surgeon/mcp-test` on a
stale `target/` sees the red. That is the correct trade: the cost is one command a developer is
told to run; the benefit is that no receipt can vouch for a run under rules it never faced.

### 14. RULING — the merge into `MCP/main` is one conflict, and it is the `.PHONY` line only. Keep all three words.

```text
git rev-parse origin/MCP/main
7e46b3088d780bbc115b00ac30cceb9b71bf2c0b
git merge-tree --write-tree HEAD 7e46b308; echo MERGE_TREE_EXIT=$?
a6a882eb3a56dba817bcb431c47bfaace1f996ab
100644 38494a98e70b30f7634b3be3ba94878fb2dda94c 1	Makefile
100644 c1845228f4435bd02aca70879dd9815efdee9c2d 2	Makefile
100644 f12d2750e9953e47479d34ed93ab7aeae8e5b5ae 3	Makefile

Auto-merging Makefile
CONFLICT (content): Merge conflict in Makefile
MERGE_TREE_EXIT=1
```

One conflicted path, one conflicted hunk (`grep -c '^<<<<<<<'` of the three-way merge = 1), at
`Makefile` line 63, the `.PHONY` list. This side adds `admit-transaction-recovery-battery`; the
trunk adds `fanout-selftests` and `tmp-leak-ratchet-self-test`. The resolution is the union — 94
words — and no other file conflicts.

---

## Verdict

## GO-WITH-FIX

**Round eleven's substance holds and round nine's hole is genuinely closed.** The three-state table
reproduces on a real fresh clone with the exact claimed figures and identical assertion counts in
all three states; the receipt's subject is read from the battery script, so a receipt cannot shrink
what it vouches for (proved by widening the script and watching a good receipt go red); every
equality attack in the addendum — permutation, extra arm, verdicts-only extra arm, missing
`:arm-verdicts` with a matching `:arms-passed`, a contradictory count, a contradictory verdict, an
older-shape receipt, an unreadable receipt, and a set-valued `:arms` — fails closed with a distinct
and accurate reason; the sabotage reproduces at exactly 18 failures with both the passing and the
failed receipt, caught only by the new witness; the RED sha is red for precisely the stated reason
and the GREEN sha is green; and all eight claimed gates reproduce, with the merge a single `.PHONY`
union. Failing closed on a stale receipt is the right trade and I rule it acceptable.

**Nothing is BLOCKING under the brief's stated bar** — no refusal kind escapes the enumeration, no
text receipt loses a leaf the structured receipt carries, no `next_call` is inexecutable, and no
red battery can buy a green. **Non-blocking but FIX REQUIRED before this lands, all inside the one
function the round-twelve builder is already editing:** finding 3, five ways the classifier exits
outside its three states (two distinct `sort` sites at `:106` and `:136`, a non-seqable `:arms` at
`:122`, a `:kinds-published` coercion at `:174` that returns **`:satisfied`** and then errors, and
an `Error` slipping past `catch Exception` at `:166`) — each non-green but none counted in
`precondition-failures` or printed with its clearing command, which falsifies MCP-OP-ADMIT-152's
stronger claim; finding 4, a present receipt reading as `nil` classified ABSENT, the only exit-0
result in this review, under a summary line that states the file does not exist; and finding 5,
`clojure.core/read-string` leaving `*read-eval*` on so a receipt executes code inside the gate. Fix
those three, add all seven shapes to the witness at `:5624`, and this lane is GO for `MCP/main`.
