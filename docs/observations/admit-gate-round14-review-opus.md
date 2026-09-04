## GO-WITH-FIX

*(final — round fourteen, clj-surgeon `bridge/admit-gate-r3` at `1bb136d0`. Written
incrementally; verdict below is final. Sol's content filter refused this
review after its provenance item; this is the Opus fallback review.)*

Review clone `/home/forge/tmp/sol/gate4-wt` at `1bb136d0b21ee60d7cc21262ba5408e1c751c570`,
`git status --porcelain` empty at start. Every suite figure below comes from a real
`git clone --no-local` of that worktree under `/var/tmp/forge/gate14-opus-fx` (never `/tmp`;
`git archive` exports fail `clj-surgeon.repository-hygiene-test` closed). No ports opened;
7888/7890/7894/7895 never contacted. Nothing committed, staged, stashed or pushed.

One deviation from the letter of the brief, disclosed: I ran `git fetch origin` **in the review
clone** so that `git merge-tree` would run against the *current* trunk rather than a stale
remote-tracking ref. A fetch updates refs only; `git status --porcelain` was empty before and
after, and no checkout, commit, stage, stash or push happened there. This mattered — see
finding 8, where a clone's inherited `origin/MCP/main` was **stale by two commits** and would
have produced a merge receipt about the wrong trunk.

---

## 1. PASS — the read-eval hazard is real, and the `clojure.edn` fix is load-bearing.

`test/clj_surgeon/admit_patch_test.clj:248`. The builder's mechanism claim is exactly right and I
reproduced the kill. Driving the **production function** `check-battery-precondition!` at the RED
sha `98c2eb55` with a receipt whose bytes are `#=(java.lang.System/exit 3)`:

```text
cd /var/tmp/forge/gate14-opus-fx/red && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate14-opus-fx/readeval.clj'; echo "RED_EXIT_CODE=$?"
```

```text
RECEIPT-BYTES "#=(java.lang.System/exit 3)"
ABOUT-TO-CLASSIFY
RED_EXIT_CODE=3
```

`STATE` and `JVM-STILL-ALIVE` never print: the reader **evaluated the form and killed the JVM
mid-classification**. A gate that a receipt file can terminate is not a gate.

At the tip, the same bytes classify `:failed` with the JVM alive (finding 2, case
`:read-eval-exit`). And the witness is proved load-bearing by sabotage — reverting **only** the
one word `edn/read-string` → `read-string` at `:248` re-opens it through the real suite:

```text
cd /var/tmp/forge/gate14-opus-fx/sab && perl -0777 -pi -e 's/\(try \(edn\/read-string \(slurp receipt\)\)/(try (read-string (slurp receipt))/' test/clj_surgeon/admit_patch_test.clj && <run the ns>
```

```text
-                 (try (edn/read-string (slurp receipt))
+                 (try (read-string (slurp receipt))
EXIT=3
```

No `Ran N tests` line is printed at all — the suite JVM is killed, exit 3. That is the loudest
possible sabotage receipt and it is the correct one.

## 2. NOTED (correct the record) — the RED commit `98c2eb55` exits **1, not 3**. The builder's narrative attributes the kill to the wrong commit.

The brief says to expect exit 3 when running the RED sha. It does not happen, and the reason is
worth recording because it is a witness-ordering fact, not a defect in the tip.

```text
cd /var/tmp/forge/gate14-opus-fx/red && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate14-opus-fx/run-admit.clj'
```

```text
FAIL in (a-receipt-from-a-failed-battery-is-a-failed-precondition-not-a-green) (admit_patch_test.clj:5849)
round eleven's five escape sites ... site :174 -- a number as :kinds-published must not classify :satisfied and then throw
expected: (= :failed state)
FAIL in (a-receipt-from-a-failed-battery-is-a-failed-precondition-not-a-green) (admit_patch_test.clj:5851)
expected: (= 1 (count failures))
ERROR in (a-receipt-from-a-failed-battery-is-a-failed-precondition-not-a-green) (LispReader.java:1392)
Ran 166 tests containing 4288 assertions.
2 failures, 1 errors.
SUMMARY {:test 166, :pass 4285, :fail 2, :error 1, :precondition-skipped 1, :type :summary}
EXIT_CODE=1
```

The `#=` case is written **fourth** in that `deftest`, after the 60,000-deep nesting case. At
`98c2eb55` the deep-nesting case throws `StackOverflowError` past `(catch Exception e)`, which
`clojure.test` records as one `:error` and which **aborts the rest of the `deftest`** — so the
read-eval case never executes. The RED commit is genuinely red (2 failures, 1 error, exit 1) for
the two sites it does reach, which is all a RED commit must be. **Non-blocking**, but the
build log should not claim the suite exits 3 at that sha; it exits 3 only when the read step is
reverted in isolation (finding 1) or when the read-eval case is driven directly.

## 3. PASS — 32 receipt shapes, including every sibling escape I could invent, all fail closed with the JVM alive.

Driven against the production `check-battery-precondition!` at the tip, reports captured:

```text
cd /var/tmp/forge/gate14-opus-fx/tip && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /var/tmp/forge/gate14-opus-fx/escapes.clj'; echo EXIT_CODE=$?
```

| shape | state | reports | verdict |
|---|---|---|---|
| `#=(java.lang.System/exit 3)` | `:failed` | fail 1 / pass 2 | "could not be read" |
| `#=(clojure.core/println "READ-EVAL-EXECUTED")` | `:failed` | fail 2 / pass 1 | **string never printed** |
| `#=` nested as a map VALUE `{:a #=(…exit 3)}` | `:failed` | fail 2 / pass 1 | not evaluated |
| `#=` inside a STRING, receipt otherwise complete | `:satisfied` | pass 3 | correct — it is data |
| receipt is the quoted string `"#=(…)"` | `:failed` | fail 1 / pass 2 | "is not a map" |
| `#inst "2026-09-04T…"` | `:failed` | fail 1 / pass 2 | "is not a map" |
| `#uuid "550e8400-…"` | `:failed` | fail 1 / pass 2 | "is not a map" |
| custom tag `#foo/bar {:x 1}` | `:failed` | fail 1 / pass 2 | "No reader function" |
| custom tag inside an otherwise complete map | `:failed` | fail 2 / pass 1 | "No reader function" |
| **list** not map `(1 2 3)` | `:failed` | fail 1 / pass 2 | "is not a map: (1 2 3)" |
| vector / set / keyword / number receipts | `:failed` | fail 1 / pass 2 | "is not a map" |
| **`:arms-passed` a string `"3"`** | `:failed` | fail 1 / pass 2 | ":arms-passed \"3\" but declares 3" |
| `:arms-passed` nil / `3.0` | `:failed` | fail 1 / pass 2 | same branch |
| a planted `::unreadable` key | `:failed` | fail 1 / pass 2 | fails closed |
| `:kinds-published` a number | `:failed` | fail 1 / pass 2 | "cannot be read as a set" |
| `:kinds-published` a string / a map | `:satisfied` | **fail 1** / pass 2 | red via the kind assertion |
| `:verdict "passed"` (string) | `:failed` | fail 1 / pass 2 | "verdict is \"passed\", not :passed" |
| `:arms :bogus` (non-seqable) | `:failed` | fail 1 / pass 2 | caught by the total wrapper |
| mixed arm keys `{"8" true 32 true 64 true}` | `:failed` | fail 1 / pass 2 | `sort-by pr-str`, no CCE |
| 60,000-deep nesting | `:failed` | fail 1 / pass 2 | "could not be read" |
| empty / whitespace / `nil` file | `:failed` | fail 1–2 / pass 1–2 | "is not a map: nil" |
| file genuinely absent | `:absent` | pass 3 | the honest skip |

`JVM-STILL-ALIVE` printed, `EXIT_CODE=0`. **Nothing threw out of the checker; nothing reached
`:satisfied` without either being a genuinely complete receipt or producing a failing assertion;
the read-eval marker string never appeared.** Round eleven's five escapes and round thirteen's
two holes are all closed, and I could not invent a sixth.

## 4. PASS (with one bound noted) — the reader is not bounded *by design*, but it fails closed on both resource attacks.

`:248` reads with a plain `slurp`, so there is no size ceiling. Both attacks nonetheless terminate
in under half a second because the `catch Throwable` converts the resulting `OutOfMemoryError`
into the ordinary `::unreadable` → `:failed` path.

```text
java -Xmx512m -Dreceipt.path=…/big/receipt.edn -cp "$CP" clojure.main …/bounded.clj      # 100 MB file
EXISTS true LENGTH 104857600
RESULT {:state :failed} MS 234 REPORTS {:fail 1, :pass 2}
JVM-STILL-ALIVE
A_EXIT=0
```

```text
java -Xmx512m -Dreceipt.path=…/big/zero-receipt.edn -cp "$CP" clojure.main …/bounded.clj  # symlink -> /dev/zero
EXISTS true LENGTH 0
RESULT {:state :failed} MS 471 REPORTS {:fail 1, :pass 2}
JVM-STILL-ALIVE
B_EXIT=0
```

The `/dev/zero` symlink is the sharper case: `.length` reports 0, so no length pre-check could
have caught it, and the read is infinite — it terminates only by exhausting the heap. **Answer to
the brief's question: the reader is NOT bounded; it is fail-closed by heap exhaustion.** At
`-Xmx512m` that costs 471 ms. At a large heap it would cost proportionally more wall time and
would leave the JVM in a post-OOM state, which is a fragile place to keep running a suite from.
**Non-blocking** — every observable outcome is the correct `:failed`, and the write access needed
to plant such a receipt in a gitignored `target/` is strictly more powerful than the deletion that
buys the honest skip anyway. Cheap hardening if the builder wants it: read at most N bytes
(`java.io.Reader` + a bounded `char[]`) and treat overflow as `::unreadable` explicitly, so the
refusal is a decision rather than a side effect of running out of memory.

## 5. NON-BLOCKING — `edn/read-string` reads only the FIRST form; trailing content is silently ignored.

Two cases from finding 3's run, called out because they are the only `:satisfied` results that are
not obviously correct:

```text
{:case :two-forms, :result {:state :satisfied}, :report-types {:pass 3}, :first-msg nil}
{:case :trailing-garbage, :result {:state :satisfied}, :report-types {:pass 3}, :first-msg nil}
```

`:trailing-garbage` is a complete passing receipt followed by `#=(java.lang.System/exit 3)`. The
trailing form is neither read nor evaluated — that part is correct and is the whole point of the
`edn` fix — but the receipt is accepted with unexamined bytes after it. I rank this **non-blocking**
and barely worth fixing: to exploit it an attacker must already be able to write a complete,
honest, passing receipt as the first form, at which point the trailing bytes add nothing. Noting it
only so the record says it was tested.

## 6. PASS — the three-state table reproduces exactly, on real `git clone`s, with identical assertion counts in all three states.

Clones: `git clone --no-local /home/forge/tmp/sol/gate4-wt <dir>`, `git checkout 1bb136d0`,
`git status --porcelain` empty, no `target/`. (Confirmed the brief's apparatus note: a `git archive`
export is NOT usable — `clj-surgeon.repository-hygiene-test` fails closed without `.git`.)

**A · absent** — `~/bin/suite-run clojure -M:clj-surgeon/mcp-test`

```text
Ran 763 tests containing 10611 assertions.
0 failures, 0 errors.
1 preconditions skipped.
  SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
0 preconditions failed.
STATE_A_EXIT=0
```

**B · passing** — after `~/bin/suite-run make admit-transaction-recovery-battery`
(`PASS n=8/32/64`, `3/3 arms passed`, `verdict :passed`, `BATTERY_EXIT=0`):

```text
Ran 763 tests containing 10611 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.
STATE_B_EXIT=0
```

**C · forced-red** — the n=8 arm forced to fail (`hit?` guarded with `(not= n 8)`), the battery run
(`FAIL n=8 attempts=3`, `2/3 arms passed`, `verdict :failed`, exit 1), then the **battery script
reverted** so only the poisoned receipt in gitignored `target/` remains:

```text
Ran 763 tests containing 10611 assertions.
1 failures, 0 errors.
0 preconditions skipped.
1 preconditions failed.
  FAILED · battery receipt at target/admit-transaction-recovery-battery-receipt.edn is PRESENT but does NOT record a complete run · the battery did NOT pass every arm: 1 of 3 failed · failing arms [8] · re-run `make admit-transaction-recovery-battery` and make it pass before trusting this lane
STATE_C_EXIT=1
```

763 tests / **10611 assertions in every state** — the claim that the check spends the same
assertions whichever state it takes is true by execution. Round nine's hole (a red battery's
receipt buying a green) is closed and stays closed: the red receipt costs a failure *and* a named
counted precondition failure *and* exit 1, and it names the command that clears it.

## 7. PASS — the gates reproduce, and the sabotages bite.

All on real clones of `1bb136d0`. Commands exactly as run; `~/bin/suite-run` throughout.

| gate | claimed | observed | Δ |
|---|---|---|---|
| `clojure -M:clj-surgeon/mcp-test` (fresh clone, absent) | 763 / 10611 / 0 + 1 skipped | **763 / 10611 / 0, 1 skipped, 0 failed, exit 0** | 0 |
| `clojure -M:clj-surgeon/mcp-test` (receipt passing) | 0 skipped / 0 failed | **763 / 10611 / 0, 0 skipped, 0 failed, exit 0** | 0 |
| `clojure -M:clj-surgeon/mcp-test` (receipt forced-red) | 1 failure + 1 failed | **1 failure, 0 errors, 1 precondition failed, exit 1** | 0 |
| `bb test/run_all.clj` | 814 / 6724 / 0 | **814 / 6724 / 0, exit 0** | 0 |
| intent audit | 372 / 0 | **`{:ok true, :specs 372, :violations 0}`, exit 0** | 0 |
| `make mcp-operation-oracle` | pass | **`mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]`, exit 0** | 0 |
| `clj-surgeon.admit-patch-test` alone | 166 / 4297 / 0 | **166 / 4297 / 0, 1 skipped, exit 0** | 0 |
| `make admit-analyzer-memory-self-test` | 3/3 at `-Xmx512m` | **3/3, peak 35 / 34 / 146 MiB vs budget 409, exit 0** | 0 |
| `make admit-transaction-recovery-battery` | 3/3 `:verdict :passed` | **3/3 four consecutive runs, `verdict :passed`, exit 0** | 0 |

Audit command verbatim:

```text
~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (count (:violations r))}))"
{:ok true, :specs 372, :violations 0}
```

**Battery determinism.** The brief's earlier rounds flagged `transaction-recovery-required` as
timing-dependent. Four consecutive clean runs plus one deliberately-forced-red run, all decisive,
no flake:

```text
admit-transaction-recovery-battery: 3/3 arms passed   run1 exit=0
admit-transaction-recovery-battery: 3/3 arms passed   run2 exit=0
admit-transaction-recovery-battery: 3/3 arms passed   run3 exit=0
```

It remains correctly outside the fast lane (a battery, not a fast-suite witness), and the fast
lane's counted skip / counted failure is the right shape for that separation.

**Sabotage.** Three reverts, each applied to a clone, verified in `git diff`, run, then reverted:

| sabotage | site | result |
|---|---|---|
| `catch Throwable` → `catch Exception` at the read step | `:249` | **0 failures, 1 error**, exit 1 (`ERROR in … (EdnReader.java:100)`) — matches the brief's claim exactly |
| `edn/read-string` → `read-string` at the read step | `:248` | **suite JVM killed, exit 3, no summary printed at all** |
| `(not exists?)` → `(nil? record)` | `:129` | **4 failures**, exit 1 (`admit_patch_test.clj:5917,5919,5921,5922`) |

Every sabotage patch is confirmed applied by its diff before the run, and every clone is confirmed
back to `dirty=0` after. This closes the builder's own earlier honest negative (a sabotage patch
that silently no-op'd and ran green): none of these three no-op'd.

## 8. BLOCKING (for the merge, not for the lane's own work) — the tip is green, current trunk is green, and the MERGE OF THE TWO IS RED with 4 failures. `git merge-tree`'s "one `.PHONY` conflict" is a textual receipt that does not see it.

This is the brief's closing question — *is this tip GO on its own for MCP/main* — and the honest
answer is **no, not as-is**.

**First, the trap in the apparatus.** A `git clone --no-local` of the review worktree does **not**
inherit that worktree's remote-tracking refs; the clone's `origin/MCP/main` resolved to
`4116438b`, **two commits stale**. A merge receipt built in such a clone is a claim about the
wrong trunk. Everything below uses the real trunk sha, fetched explicitly:

```text
cd /home/forge/tmp/sol/gate4-wt && git fetch origin && git rev-parse origin/MCP/main
e8090624cd126600a8e0efba98386116c74ce1ed
```

**The textual merge is exactly as the builder claims.**

```text
cd /home/forge/tmp/sol/gate4-wt && git merge-tree --write-tree HEAD origin/MCP/main
1bcad3a9a257cd5b3b8ff7dc4e00c4eb07998c08
100644 38494a98e70b30f7634b3be3ba94878fb2dda94c 1	Makefile
100644 c1845228f4435bd02aca70879dd9815efdee9c2d 2	Makefile
100644 2795bab419bc045e605b8f33d23464603b77d457 3	Makefile

Auto-merging Makefile
CONFLICT (content): Merge conflict in Makefile
Auto-merging test/clj_surgeon/admit_patch_test.clj
```

ONE conflict, the `.PHONY` line only, resolved by keeping all four words (ours
`admit-transaction-recovery-battery`; theirs `census-battery`, `fanout-selftests`,
`tmp-leak-ratchet-self-test`). Confirmed by performing the merge on a clone: `git diff
--name-only --diff-filter=U` returns `Makefile` and nothing else.

**But note the second line of that output**: `Auto-merging test/clj_surgeon/admit_patch_test.clj`.
Trunk has moved this lane's own file (`40f23986 merge-fix: the five gate failures the merge itself
produced, by intent` — this class of failure has already bitten this lane once). Git resolved it
textually. It does not resolve semantically. Three runs, same command, same lane:

| tree | sha | result |
|---|---|---|
| the lane's tip alone | `1bb136d0` | **763 / 10611 / 0**, exit 0 |
| current trunk alone | `e8090624` | **865 / 13023 / 0**, exit 0 |
| **the merge of the two** | `614e6230` (local fixture) | **917 / 15195, 4 failures**, exit 4 |

```text
cd /var/tmp/forge/gate14-opus-fx/merged && ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 917 tests containing 15195 assertions.
4 failures, 0 errors.
MERGED_MCP_EXIT=4
```

The four, verbatim, all in this lane's own `admit_patch_test.clj`:

```text
FAIL in (the-source-scan-survives-only-as-a-complement) (admit_patch_test.clj:6404)
a kind is constructed in the files the admit gate calls and is neither enumerated nor justified as unreachable: #{"source-not-readable"}
  actual: (not (empty? #{"source-not-readable"}))

FAIL in (a-source-path-under-a-regular-file-is-an-invalid-source-path) (admit_patch_test.clj:6113)
  actual: (not (= :invalid-source-path :source-file-not-found))

FAIL in () (admit_patch_test.clj:5996)
MCP-OP-ADMIT-133: the enumeration is the set the entrance produces
the enumeration claims kinds no fixture drives and no battery target proves, so nothing shows they exist or that their text is a superset: #{:invalid-source-path}

FAIL in () (admit_patch_test.clj:6009)
MCP-OP-ADMIT-133: the enumeration is the set the entrance produces
enumerated 34, observed 32, battery-only 1
```

**Cause, traced to the commit.** Trunk introduced a refusal kind this lane's enumeration has never
heard of, in a file the admit gate calls:

```text
cd /home/forge/tmp/sol/gate4-wt && git grep -n 'source-not-readable' origin/MCP/main -- src
origin/MCP/main:src/clj_surgeon/mcp_paths.clj:220:              (path-refusal :source-not-readable
origin/MCP/main:src/clj_surgeon/mcp_paths.clj:262:            (path-refusal :source-not-readable
origin/MCP/main:src/clj_surgeon/mcp_relation_census.clj:632:   :error_type :source-not-readable

cd /home/forge/tmp/sol/gate4-wt && git grep -n 'source-not-readable' HEAD -- src test
(no output)
```

and trunk's path-fence work (`f463f5f7` / `3bcff363`, "fence a build file's `:paths`", "the
containment fence fails CLOSED") reclassifies a source path under a regular file from
`:invalid-source-path` to `:source-file-not-found` — which removes the only fixture driving
`:invalid-source-path`, so the completeness proof loses its driver too. Failures 2, 3 and 4 are one
cause; failure 1 is the other.

**How to read this.** It is not a defect in the lane's design — it is **this lane's own
MCP-OP-ADMIT-133 witness working exactly as intended**, detecting that the entrance's kind
vocabulary changed under it. Round three's central claim ("the enumeration is the set the entrance
produces, proved by execution both ways") is only true relative to a particular `src/`, and trunk
moved `src/`. That the witness goes red rather than silently accepting a 34-kind declaration
against a 33-kind entrance is the whole point of building it.

**Why I nevertheless call it BLOCKING.** The brief's terminal instruction is "if everything holds,
GO and this lane LANDS on MCP/main." Landing this tip on current `MCP/main` produces a **red
trunk**, and the merge receipt offered in support of landing it — one `.PHONY` conflict — is
precisely the kind of claim this whole fourteen-round exercise exists to refuse: a verifier blind
to its own subject. A textual merge check cannot see a semantic merge failure, and the lane must
not be landed on the strength of one.

**The fix is small, named, and belongs in this lane before it lands** (all in
`test/clj_surgeon/admit_patch_test.clj`, at the merge, re-verified there):

1. add `:source-not-readable` to `admit-refusal-kinds`, with a live fixture that drives it — or to
   the justified `admit-refusal-kinds-not-reachable-from-the-entrance` list with a written
   justification, which is the honest option only if the admit entrance genuinely cannot reach
   `mcp_paths.clj:220/262`; **drive it before deciding**, per the lane's own standard;
2. re-point `a-source-path-under-a-regular-file-is-an-invalid-source-path` at trunk's current
   behaviour, and give `:invalid-source-path` a fixture that still drives it (or move it to the
   battery-only / not-reachable set) so MCP-OP-ADMIT-133's set equality holds again;
3. re-run the merge suite and show `0 failures`, plus the intent audit — which on the merge tree is
   already green at a larger spec count: `{:ok true, :specs 426, :violations 0}`.

**Ratchet worth taking with it.** The lane's merge evidence should be the suite run **on the merge
result against the fetched trunk sha**, never `git merge-tree` alone, and never a clone's inherited
`origin/MCP/main`. Two independent ways to be wrong showed up in this one check: a stale ref, and a
clean textual merge over a red semantic one.

---

## Verdict

## GO-WITH-FIX

**Non-blocking / verified, in the lane's favour.** Every round-fourteen claim about the classifier
holds under execution. The read-eval hazard is real and I reproduced the kill at the RED sha
(exit 3, JVM dead mid-classification); the one-word `clojure.edn/read-string` fix closes it, and
sabotaging that single word re-opens it so loudly that the suite prints no summary at all. All five
round-eleven escapes and both round-thirteen holes are closed, and across **32 receipt shapes** —
including every sibling I could invent: a list not a map, `:arms-passed` as a string, a planted
`::unreadable` key, `#inst` / `#uuid` / a custom `#foo/bar` tag, `#=` nested as a map value, `#=`
inside a string, a 100 MB receipt, and a receipt symlinked to `/dev/zero` — **nothing threw out of
the checker, nothing reached `:satisfied` without either being a genuinely complete receipt or
producing a failing assertion, and no receipt ever executed code.** Present-but-nil, whitespace-only
and non-map are FAILED, never the absent skip. The three-state table reproduces on real clones with
**identical assertion counts (10611) in all three states**, exit 0 / 0 / 1. Every headline gate
matches its claim to the digit — mcp-test 763/10611/0, bb 814/6724/0, audit 372/0, oracle pass, the
namespace alone 166/4297/0, the memory self-test 3/3 at `-Xmx512m`, the recovery battery 3/3 on four
consecutive runs — and all three sabotages bite, none silently no-op'ing. Two things I record
without blocking: the RED commit `98c2eb55` exits **1, not 3** (an earlier `StackOverflowError`
aborts the `deftest` before the read-eval case runs, so the kill must be attributed to the read step
in isolation, not to that sha); and the receipt reader is **not bounded by design** — it survives a
100 MB file and an infinite `/dev/zero` only because `catch Throwable` converts the resulting
`OutOfMemoryError` into `:failed`, which is the right answer reached by an uncomfortable route.

**Blocking, one item, finding 8.** The tip is green alone (763/10611/0) and current trunk
`e8090624` is green alone (865/13023/0), but **their merge is RED: 917 tests, 4 failures, exit 4** —
and `git merge-tree`'s receipt of "one `.PHONY` conflict" cannot see it, because the conflict is
semantic, in the very file git auto-merged. Trunk added the refusal kind `:source-not-readable` to
`src/clj_surgeon/mcp_paths.clj` and reclassified a source path under a regular file from
`:invalid-source-path` to `:source-file-not-found`; this lane's declared enumeration and its
MCP-OP-ADMIT-133 completeness proof are stale against that `src/`. The failure is the lane's own
witness working correctly, and the fix is small and named — enumerate or justify
`:source-not-readable` with a driven fixture, re-point the `:invalid-source-path` fixture, re-run
the suite **on the merge result** — but until that is done and shown green, landing this tip puts
`MCP/main` in the red. **This lane does not land today; it lands after finding 8 is closed and the
merge suite is shown at 0 failures against the fetched trunk sha.** A separate apparatus warning
travels with it: a `git clone` does not inherit the source's remote-tracking refs, and mine resolved
`origin/MCP/main` two commits stale — merge evidence must name the fetched sha it was built against.
