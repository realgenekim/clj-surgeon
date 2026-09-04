## NO-GO

Round-six review of clj-surgeon `bridge/integration-2026-09-03-mem003` at `432268cf`
(MEM-003 measured clock, second landing, trunk-merged). Taken with Sol's round-six brief
and its ROUND-SIX ADDENDUM after Sol's content filter refused the brief after 9 KB; paths
substituted per the integrator's instruction. Clone `/home/forge/tmp/sol/mem003r4-wt`,
proven clean at the tip; nothing in it committed, pushed, stashed, added or edited; no
server started on any port; sabotage only on `git archive 432268cf` exports under
`/var/tmp/forge/mem003r6-review-fx/opus`, removed at the end.

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git rev-parse HEAD && git status --porcelain
```

```text
432268cf40997ef7694ad44d8a6f7ff06e18ce35
```

(`git status --porcelain` printed nothing.)

**Housekeeping, as instructed.** A previous reviewer's fixtures existed under
`/var/tmp/forge/mem003r6-review-fx` (42 MB: `plant_reflective_clock/`,
`plant_reflective_method/`, `plant_reflector_method/`, `prune-root/`, `round3-tree/` and
five `*_probe.clj` files, all timestamped 12:03–12:12 today). **I removed them before
starting** and worked in a fresh `/var/tmp/forge/mem003r6-review-fx/opus`. Nothing else
under `/var/tmp/forge` was touched or deleted.

---
## Headline

Round six is the strongest round of the six. **Both round-five blockers are genuinely
closed**, and closed at the right rung — the escape-hatch pattern is no longer a list but a
derivation over the namespace's interns (public *and* private), the protocol's `:sigs`, and
the opaque types' declared fields; the naming rule gained a `:reflective` clause that shuts
`ns-resolve`/`resolve`/`find-var`/`intern`/`requiring-resolve` in one stroke; the clock class
list became a closure from ten roots and grew 46 → 159 spellings including constructors and
the dot special form. I reproduced the builder's own before/after table and every number in
it matched. Every gate I ran is green at the claimed figures.

**But the branch is still NO-GO, and it fails in the same shape for the fourth round
running.** The derivation now enumerates every *name*; it still does not enumerate every
*spelling of a call*, and the JVM offers several that contain no name at all. **Five ordinary
routes plant a clock-derived number in an undeclared receipt field inside the hashed parity
subject with twenty-four tests and one hundred twenty-six assertions green.** Four of them
were named explicitly in the round-six addendum I was asked to attack:

1. `(.getMethod (class r) "_launder" …)` + `.invoke` — the method name as a **string**;
2. `(clojure.lang.Reflector/invokeInstanceMethod r "_launder" …)` — the same, via Clojure's
   own reflector;
3. `(. r _launder)` — the **dot special form** of the interop route the round closed;
4. `(.getMethod (Class/forName "java.lang.System") "nanoTime" …)` — a raw clock read that
   the 159-spelling clock derivation cannot see, because the class and the method are both
   strings;
5. `(first (.getDeclaredFields (class r)))` + `setAccessible` — **positional**, so no field
   name is ever spelled.

The root cause is one sentence, and it is the same one the round-five review wrote about
`measured/`: **every alternative in both derived patterns is a literal source token, and a
reflective call site spells the token as a string argument instead.** A derivation over names
cannot see a call that names nothing.

There are also three non-blocking findings, one of which is a regression against a closure the
build record declares GREEN (the Java-collection walker does not walk a Java **array** or an
**Iterator**).

---

## 0. Verified first: both round-five blockers ARE closed, and the tip is green

Baseline, the scanning gate in the runtime it actually runs in:

```sh
cd /var/tmp/forge/mem003r6-review-fx/opus/base && ~/bin/suite-run bb -cp src:test -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

```text
Ran 24 tests containing 126 assertions.
0 failures, 0 errors.
{:test 24, :pass 126, :fail 0, :error 0, :type :summary}
```

The two round-five plants, re-exported at this tip, both go red at exactly the counts the
build record claims:

| plant, at `432268cf` | result |
|---|---|
| `(._launder (measured/elapsed-ms started))` | **2 failures** — `every-untagged-clock-verb-call-site-is-named`, naming `["src/clj_surgeon/mcp_hot_verify.clj" "verify!"]` as an untagged-clock site with no allow-list entry |
| `((ns-resolve 'clj-surgeon.measured 'unwrap-readings) …)` | **1 failure** — `the-measured-namespace-is-named-only-by-the-sanctioned-require`: `[["src/clj_surgeon/mcp_hot_verify.clj" 115 :reflective ":verification_wall_ms ((ns-resolve 'clj-surgeon.measured 'unwrap-readings)"]]` |

That is honest, and it is credit the builder has earned. The findings below are new ground,
not the old ones restated.

---

## 1. BLOCKING — the protocol method reached by its NAME AS A STRING: `.getMethod` and `Reflector/invokeInstanceMethod`

`escape-hatch-pattern` is derived and it produces exactly eleven alternatives on the JVM:

```sh
cd /var/tmp/forge/mem003r6-review-fx/opus/base && ~/bin/suite-run bb -cp src:test -e '…(count (str/split (str escape-hatch-pattern) #"\|"))…'
```

```text
escape-hatch alternatives: 11
clock alternatives: 161
```

Every one of the eleven is `(java.util.regex.Pattern/quote spelling)` of a token like
`._launder`, `.-launderable`, `measured/value` — i.e. the method **spelled at the call site**.
Java reflection does not spell it at the call site; it passes it as a string:

```clojure
(.invoke (.getMethod (class rr) "_launder" (into-array Class [])) rr (into-array Object []))
```

The text `"_launder"` is preceded by a quote, not a dot, so `\Q._launder\E(?![-\w])` does not
match. Nothing else in the file looks at `.getMethod`, `.invoke`, or `Reflector`.

### 1a. Plant A — `git archive 432268cf` at `/var/tmp/forge/mem003r6-review-fx/opus/plantA`, one form added at `src/clj_surgeon/mcp_hot_verify.clj:114`

```diff
-               :elapsed_ms (measured/elapsed-ms started)})))))))
+               :elapsed_ms (measured/elapsed-ms started)
+               :verification_wall_ms (let [rr (measured/elapsed-ms started)]
+                                       (.invoke (.getMethod (class rr) "_launder" (into-array Class []))
+                                                rr (into-array Object [])))})))))))
```

Scanning gate, under babashka:

```sh
cd /var/tmp/forge/mem003r6-review-fx/opus/plantA && ~/bin/suite-run bb -cp src:test -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

```text
Ran 24 tests containing 126 assertions.
0 failures, 0 errors.
{:test 24, :pass 126, :fail 0, :error 0, :type :summary}
```

Runtime, through the real `mcp-operation/invoke!` finalizer on the JVM:

```text
{:undeclared-field 1.626097, :hashed-field 1.626097, :unpartitioned []}
```

An undeclared clock-derived field, present in the **hashed parity subject**,
`unpartitioned []`, all twenty-four tests green.

### 1b. Plant B — Clojure's own reflector, same site

```diff
+               :verification_wall_ms (clojure.lang.Reflector/invokeInstanceMethod
+                                      (measured/elapsed-ms started) "_launder" (into-array Object []))
```

```text
Ran 24 tests containing 126 assertions.
0 failures, 0 errors.
{:test 24, :pass 126, :fail 0, :error 0, :type :summary}

{:undeclared-field 1.710624, :hashed-field 1.710624, :unpartitioned []}
```

This one deserves a second look because it is not exotic: `Reflector/invokeInstanceMethod` is
what the Clojure compiler *itself* emits for an un-hinted interop call. A developer who writes
`(.someMethod obj)` with a computed name reaches for exactly this.

### 1c. Plant H — the DOT SPECIAL FORM, which is round-five finding 1 with two characters moved

```diff
+               :verification_wall_ms (. (measured/elapsed-ms started) _launder)
```

```text
Ran 24 tests containing 126 assertions.
0 failures, 0 errors.

{:undeclared-field 1.331295, :hashed-field 1.331295, :unpartitioned []}
```

This is the one I would put first in the build record's own terms. The round-six derivation
went to real trouble to emit the *dot special form* for every **clock** static
(`clock-expression-alternative` builds `\(\.\s+System\s+nanoTime\b`, and
`clock-expressions-the-ratchet-must-carry` carries `"(. System nanoTime"` as a named floor
entry with the comment *"the DOT SPECIAL FORM: the text is not `System/nanoTime` at all"*).
The identical transformation on the **escape-hatch** side was not made:
`escape-hatch-alternative` emits only `\Q._launder\E`, and `(. r _launder)` is the same call
with the dot detached. The insight was available inside the same commit range and was applied
to one of the two patterns.

### What would close finding 1

The escape-hatch side needs the two transformations the clock side already has, plus one the
clock side also needs (finding 2):

- for every derived method spelling `m`, also emit `\(\.\s+\S+\s+m\b` (the dot special form) —
  this is a three-line change to `escape-hatch-alternative`, mirroring
  `clock-expression-alternative` exactly;
- treat a **string literal equal to a derived name** as an alternative: emit
  `"m"` (quoted, with the quotes) alongside `.m`. That is one more `mapcat` in
  `escape-hatch-spellings` and it closes plants A and B together, because both must spell
  `"_launder"` to reach it;
- and add each new spelling to `escape-hatch-spellings-the-ratchet-must-carry` so the
  fail-first witness carries it, which is the pattern the file already uses correctly.

---

## 2. BLOCKING — a raw clock read the 159-spelling clock derivation cannot see: `Class/forName` + reflection

The clock derivation is the better of the two and it is genuinely a closure. It still shares
finding 1's defect, and here it costs a **raw `System/nanoTime`** — the exact subject of
`MCP-OP-TIME-005`:

```clojure
(.invoke (.getMethod (Class/forName "java.lang.System") "nanoTime" (into-array Class []))
         nil (into-array Object []))
```

Neither `System` nor `nanoTime` appears as a source token: both are strings. I confirmed
against the live pattern that no alternative matches:

```text
D forName System         escape=false  clock=false
```

### 2a. Plant D, same site, same export shape

```text
Ran 24 tests containing 126 assertions.
0 failures, 0 errors.
{:test 24, :pass 126, :fail 0, :error 0, :type :summary}

{:undeclared-field 1963217994616671, :hashed-field 1963217994616671, :unpartitioned []}
```

A raw monotonic clock value, sixteen digits of it, published in an undeclared receipt field
inside the parity hash, with the clock ratchet green. This is `MCP-OP-TIME-005`'s stated
subject reaching a receipt, which the brief names as blocking on its own.

**Why I rate this above finding 1 in severity even though it is the same class:** the
laundering routes require a `Reading` in hand, which only `measured` produces. This one
requires nothing at all — any function anywhere under `src/` can read the clock this way, and
it is the ordinary spelling for code that resolves a class by name.

**What would close it:** the same string-literal rule. Emit `"nanoTime"`, `"currentTimeMillis"`
and every derived *method* name as a quoted-string alternative, and emit each
`clock-source-class`'s **fully-qualified name as a string** (`"java.lang.System"`,
`"java.util.Calendar"`) — `Class/forName` cannot be reached without one. Both fall out of data
the derivation already holds; neither needs a new list.

---
## 3. BLOCKING — the dot special form with a FULLY-QUALIFIED class: `(. java.lang.System nanoTime)`

This is the finding I would fix first, because it needs no reflection, no `Reading`, and no
cleverness. It is four tokens.

Round five's finding 3 forced the dot special form into the derivation, and the builder did
the work: `clock-expression-alternative` emits `\(\.\s+\QSystem\E\s+\QnanoTime\E\b`, and
`clock-expressions-the-ratchet-must-carry` carries `"(. System nanoTime"` as a named floor
entry whose `:why` reads *"the DOT SPECIAL FORM: the text is not `System/nanoTime` at all."*

The alternative is built from `(.getSimpleName c)`. The **static slash** alternative is
deliberately built so that a fully-qualified call still matches — the docstring says so:
*"A STATIC spelling is quoted literally, and it deliberately matches a fully-qualified call
too (`java.time.Instant/now` contains `Instant/now`)."* The **dot-form** alternative loses
that property, because it anchors `\s+` immediately before the simple name, and
`java.lang.System` has a `.` there, not whitespace. Verified against the live pattern:

```sh
cd /var/tmp/forge/mem003r6-review-fx/opus/base && ~/bin/suite-run bb -cp src:test -e '…(re-find clock-pattern t)…'
```

```text
(. System nanoTime) simple         clock=true   escape=false
(. java.lang.System nanoTime)      clock=false  escape=false
(. Calendar getInstance)           clock=false  escape=false
(. java.util.Calendar getInst)     clock=false  escape=false
System/nanoTime fq                 clock=true   escape=false
(. java.time.Instant now)          clock=false  escape=false
(. r _launder) dot form            clock=false  escape=false
```

### 3a. Plant K — `/var/tmp/forge/mem003r6-review-fx/opus/plantK`, one line

```diff
-               :elapsed_ms (measured/elapsed-ms started)})))))))
+               :elapsed_ms (measured/elapsed-ms started)
+               :verification_wall_ms (. java.lang.System nanoTime)})))))))
```

```text
Ran 24 tests containing 126 assertions.
0 failures, 0 errors.
{:test 24, :pass 126, :fail 0, :error 0, :type :summary}

{:undeclared-field 1963393806256241, :hashed-field 1963393806256241, :unpartitioned []}
```

**A raw monotonic clock value in an undeclared receipt field inside the hashed parity subject,
`MCP-OP-TIME-005`'s exact prohibition, with the clock ratchet green.** The floor entry that
names this very read as a route was added in this round.

**What would close it:** in `clock-expression-alternative`, emit the dot form as
`\(\.\s+(?:[\w.]*\.)?\QSystem\E\s+\QnanoTime\E\b` — one optional package-prefix group — which
gives the dot form the same fully-qualified tolerance the slash form already has. Then add
`"(. java.lang.System nanoTime"` to `clock-expressions-the-ratchet-must-carry` so the
fail-first witness holds it. The identical fix belongs on `escape-hatch-alternative` for
finding 1c.

---

## 4. BLOCKING (compounding 2 and 3) — the floor union DOES let a JVM-only route through on babashka, and the scanning gate is babashka-only

The addendum asked me to check exactly this. The answer is yes, with one concrete alternative.

First, the gate really is babashka-only. `measured-invariant-test` appears in exactly one
runner:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && grep -rn "measured-invariant" test/ --include=*.clj -l
```

```text
test/run_all.clj
test/clj_surgeon/measured_invariant_test.clj
```

`test/run_all.clj` is the babashka suite. The JVM `mcp-test` suite does not load it. So
whatever babashka cannot derive, **nothing** enforces.

Second, I derived both patterns in both runtimes from the same tree and diffed them:

```sh
# same cmp.clj under bb and under clojure -M, then set/difference
```

```text
bb  escape 11  clock 161
jvm escape 11  clock 162

escape: in JVM not bb: ()
derived-escape JVM: [".-launder" ".-launderable" "._launder" ".launderable" "launderable"
                     "measured/-launder" "measured/raw-ms" "measured/raw-nanos"
                     "measured/start-nanos" "measured/unwrap-readings" "measured/value"]
derived-escape bb:  [".-launder" "._launder" "measured/-launder" "measured/raw-ms"
                     "measured/raw-nanos" "measured/start-nanos" "measured/unwrap-readings"
                     "measured/value"]

derived-clock JVM count 162   bb count 159
derived-clock in JVM not bb: ("(. Calendar getInstance" ".getTimeInMillis" "Calendar/getInstance")
clock: in JVM not bb: ("\\(\\.\\s+\\QCalendar\\E\\s+\\QgetInstance\\E\\b")
clock: in bb not JVM: ()
```

**The escape-hatch floor is correct and complete** — babashka derives eight, the JVM eleven,
and the three field spellings the floor carries are exactly the three babashka cannot reflect,
so the final pattern is eleven alternatives in both runtimes. That is the mechanism working as
designed, and the builder's reasoning about it is sound.

**The clock floor is one entry short.** Babashka underives three spellings; the floor carries
two of them (`Calendar/getInstance`, `.getTimeInMillis`) and **not** the third,
`(. Calendar getInstance` — the dot special form. So the gate's clock pattern is strictly
weaker under babashka than under the JVM, for a spelling the builder had already classified as
a clock read.

I planted it (`plantJ`, `(. java.util.Calendar getInstance)` with `.get` accessors, no
morpheme anywhere) and it is green under babashka **and** under the JVM — the latter only
because of finding 3's fully-qualified gap:

```text
=== bb scanning gate (the gate that actually runs) ===
Ran 24 tests containing 126 assertions.
0 failures, 0 errors.
=== same suite under the JVM ===
Ran 24 tests containing 126 assertions.
0 failures, 0 errors.
```

**Why I fold this into the blocking set rather than listing it as a residual:** the file's own
argument for accepting a floor is *"the witnesses assert the derivation produces every floor
entry on a runtime whose reflection is complete, so the floor is derived evidence rather than
a list."* That argument only holds if the floor is the **complete** JVM-minus-babashka
difference. It is computed by hand and it is short by one, and nothing checks it. The ratchet
that closes this is small and exact: assert
`(set/difference jvm-derived bb-derived) ⊆ (keys …-must-carry)`. There is no way to run that
in one process, but the cheap honest version is a witness that fails when
`clock-spellings` derived under the current runtime is a **strict subset** of a checked-in
JVM-derived manifest, with the manifest regenerated by a JVM step. That is the same shape as
the existing `the-derived-clock-pattern-carries-every-jdk-time-shape` witness, one level up.

---
## 5. NON-BLOCKING — the Java-collection walker does not walk a Java ARRAY or an ITERATOR

Item 5 of the round is declared closed: *"`unpartitioned-measured-paths` now walks
`java.util.Map` and `java.util.Collection`."* It does, and the `ArrayList` case the round-five
review raised is genuinely fixed. But an `Object[]` is neither a `Map` nor a `Collection`, and
neither is an `Iterator` (`measured.clj:574-590`), so two of the three shapes the round-six
addendum named are still invisible:

```sh
cd /var/tmp/forge/mem003r6-review-fx/opus/base && ~/bin/suite-run clojure … -M probe6.clj
```

```text
attach: reading in Java array                        => ()
attach: reading in Iterator                          => ()
attach: reading in lazy seq                          => ([:xs 0])
attach: reading in ArrayList                         => ([:xs 0])
```

The lazy seq (`seq?`) and the `ArrayList` are diagnosed. The array and the iterator are not:
`unpartitioned-measured-paths` reports `[]`, so `finalize-result`'s typed refusal never fires
and the `Reading` reaches the encoder, where cheshire refuses it. **Loud, not silent** — the
number does not reach the wire — which is why I rate it the same tier the builder rated the
`ArrayList` case at in round five, not blocking.

It is worth naming precisely because the round declared the class closed. The fix is two more
clauses in the same `cond`, before `:else`: `(.isArray (class node))` → walk `(seq node)`, and
`(instance? java.util.Iterator node)` → refuse outright (an iterator cannot be walked without
consuming it, so a typed refusal is the honest answer, not a diagnosis). Both belong in
`a-reading-inside-a-java-collection-is-diagnosed` as fail-first cases.

---

## 6. NON-BLOCKING (structural) — both allow-lists count matching LINES, not calls

`sites` (`measured_invariant_test.clj:85-97`) folds over `str/split-lines` and conjes **one**
hit per line on which `(re-find pattern code)` is truthy. So a form's declared count is a count
of *matching lines*, and a line that already matches absorbs an unlimited number of additional
clock reads or laundering calls without moving it.

This is visible in the tree today, benignly: `worktree_lifecycle/valid-future-expiry?` is
declared `:reads 1` and contains two:

```clojure
(.isAfter (Instant/parse expiry) (Instant/parse now))
```

Two `Instant/parse` reads, one line, one hit, and `(= declared scanned)` is green.

I tested whether this is exploitable at the strongest possible site — the publication boundary
itself, `mcp_operation/finalize-result`, whose escape-hatch entry is `:calls 1`
(`/var/tmp/forge/mem003r6-review-fx/opus/plantL`):

```diff
-  (let [elapsed-ms (measured/value elapsed)]
+  (let [elapsed-ms (measured/value elapsed) leaked-wall (measured/value elapsed)]
...
-      result)))
+      (assoc result :verification_wall_ms leaked-wall))))
```

The scan count is unchanged, exactly as predicted — **and the plant is caught anyway, by two
BEHAVIOURAL witnesses**:

```text
FAIL in (the-request-clock-does-not-survive-the-hashed-channel) (…:1321)
  actual: (not (= {:ok true, :receipt {:stable :fact}} {:ok true, :receipt {:stable :fact}, :verification_wall_ms 2.5}))
FAIL in (the-parity-hash-is-stable-across-two-runs-with-different-clock-ticks) (…:1459)
  actual: (not (true? false))
```

**That is the right answer and it deserves explicit credit:** the boundary function is
defended by witnesses that *drive it and inspect the hashed channel*, not merely by a text
scan, and those witnesses are the reason this attack fails. It is also the precise reason
findings 1–4 are blocking: those behavioural witnesses drive `finalize-result` with a
`(constantly …)` execute, so they see the boundary and nothing upstream of it. **Every
producer under `src/` — `mcp_hot_verify/verify!`, where all six of my successful plants sit —
is defended by the textual scan alone.** The scan is the only line of defence for the code the
findings walk through, and the counting rule inside it is per-line.

The cheap ratchet: change `sites` to add `(count (re-seq pattern code))` rather than 1, and
re-bless the two counts that move (`valid-future-expiry?` 1→2 is the only one I found). The
docstring already argues the right principle — *"a form's raw clock-read count changed; re-read
it and re-justify"* — it is simply not what the code measures.

---

## 7. RULINGS on the items the addendum named

**`(java.time.Instant/parse …)` classified `:control` — CORRECT, on all three sites.** I read
each. `worktree_lifecycle/instant-string?` (`:101-107`) parses a caller string inside a
`try`, discards the value, and returns `true`/`false`. `valid-future-expiry?` (`:495-498`)
compares two caller-supplied strings and returns the boolean from `.isAfter`.
`worktree_lifecycle_io/issue-current?` (`:964-968`) compares a caller's expiry string against
`Instant/now` inside an `and`, and the surrounding form yields a boolean. In all three the
parsed value is unreachable outside the predicate. `:control` is right, and the `:why` strings
say exactly this rather than gesturing at it. (The count on `valid-future-expiry?` is wrong for
the reason in finding 6, not for a classification reason.)

**`mcp_alias_migration/prune-details!` — `:control` holds; it publishes no timestamp.** The
single `.lastModified` read sits in the `sort-by` key that orders deletion candidates; the
function's value is the detail manifest, which carries file names. I re-read it at this tip and
the round-five ruling is unchanged.

**No `:receipt` entry exists in `clock-allow-list`, and the witness still refuses one**
(`measured_invariant_test.clj:722-725`). Every one of the 37 entries is `:control`. That
assertion is the right shape and it is live.

**`measured/attach` fed a bare number** — re-verified: the number lands *inside* the
`:measured` block, which is where MEM-005 requires a bare number to be. It cannot place one in
the hashed channel. The attack fails, correctly.

**`hashCode` is constant and equality is by value** — re-verified at this tip:

```text
hash equal diff numbers                              => true
value equality                                       => true
```

Two readings holding different numbers hash equally, and `(= (reading 987654.321)
(reading 987654.321))` is true. The `hashCode`/`equals` contract holds; the bisection-oracle
residual is now declared in `measured.clj`, as round five asked. **Accept as declared.**

**The `setAccessible` residual.** Still open, still declared. I note one sharpening: my plant F
reached the field **positionally** — `(first (.getDeclaredFields (class rr)))` — so it never
spells a field name at all, and the declared residual's wording ("with a computed field name")
does not quite cover it. The scanning gate is green on plant F. Since this is the same tier the
builder and two previous reviewers accepted, I do not block on it, but the declaration should
say *"reflection over the type's fields by any route, named or positional."*

---
## 8. Fail-first discipline: every one of the five sabotage figures reproduces EXACTLY

I rebuilt each sabotage on a `git archive 432268cf` export, confirmed the patch applied by
diff, and ran the witness suite under babashka. **All five figures match the build record to
the assertion, and each fails in the witness the build record names.**

```sh
cd /var/tmp/forge/mem003r6-review-fx/opus/<S> && ~/bin/suite-run bb -cp src:test -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (t/run-tests (quote clj-surgeon.measured-invariant-test))'
```

| sabotage | patch applied | witnesses that went red | failures | claimed |
|---|---|---|---|---|
| S1 `escape-hatch-pattern` back to the round-five literal alternation | yes | `the-escape-hatch-pattern-carries-every-route-to-a-readings-number`, `the-escape-hatch-scanner-catches-every-route-planted-in-a-receipt` (×4) | **5** | 5 |
| S2 `derived-escape-hatch-spellings` → `[]` (floor only) | yes | `the-escape-hatch-pattern-is-derived-from-the-namespace-not-from-a-list` (×2) | **2** | 2 |
| S3 the `:reflective` cond clause deleted | yes | `the-require-witness-catches-a-planted-reflective-resolution` (×10) | **10** | 10 |
| S4 constructors and the dot special form dropped from the clock derivation | yes | `the-derived-clock-pattern-carries-every-jdk-time-shape` (×3) | **3** | 3 |
| S5 the two Java-collection clauses deleted from the diagnostic | yes | `a-reading-inside-a-java-collection-is-diagnosed` (×3), `a-clock-reading-never-becomes-a-raw-number-in-any-placement` (×2) | **5** | 5 |

Baseline for all five: `Ran 24 tests containing 126 assertions. 0 failures, 0 errors.`

**These witnesses are not vacuous, and the fail-first claims are honest.** That is the part of
this round I would keep verbatim. My findings are not that the witnesses are fake; they are
that the *derivation the witnesses guard* enumerates names, and four of the six routes I found
spell no name at all.

---

## 9. Rulings on the declared-open items

- **`bench/event_timing.clj` `fail!` file-fatal, and `bench/` as a third unscanned root.**
  Correctly left open, unchanged from rounds four and five. `bench/` grew substantially on the
  trunk side of this merge (`bench/fanout/*`, ~2,400 lines), and nothing in it constructs an
  MCP result. **Not a merge condition**, but the round-five note now has more weight behind it:
  `scanned-roots` is `["src" "dev/experiments"]` and `bench/` is the largest unscanned tree in
  the repo.
- **§5b, a reading in METADATA.** Accepted as declared, unchanged: the reading rides on the
  hashed value's metadata, is inert on the wire, and metadata does not participate in map
  equality, so the parity subject is unaffected.
- **The four UNMEASURED reserved-peak lines and the two `held-scales-with-n` FAILs.**
  MEM-001's lane, pre-existing at the base, unchanged in kind by this branch. **Not this
  branch's to close and not a reason to hold it.** The reason to hold it is findings 1–4.
- **The `setAccessible` residual.** Accepted, with the wording sharpening in §7 above
  (positional field access is the same class and the current wording does not name it).
- **Value equality as a bisection oracle.** Now declared in `measured.clj`. Accepted; the
  builder's reason for rejecting identity-only `=` (the type's own witnesses depend on
  `(= (reading 1.5) (reading 1.5))`) is correct.
- **The floor union.** *Not* accepted as declared, for the clock pattern specifically —
  see finding 4. The escape-hatch floor is complete and I accept that one.
- **Round three's two defects (recovery/`recover!` writing tagged readings into an on-disk EDN
  receipt; the pre-publication byte estimate measuring the raw map).** Both remain closed. The
  recovery witness is live at this tip (`test/clj_surgeon/recovery_test.clj:76-85`) and asserts
  a *bare number* in `[:measured :elapsed-ms :total]` of the persisted receipt plus
  `unpartitioned-measured-paths = []`; it runs inside the JVM `mcp-test` suite, which is green
  at 717/8497/0 below. I accept round five's reproduction of the fail-first behaviour on the
  round-three tree rather than re-exporting it, and I re-verified the witnesses are live and
  green here.

---
## 10. Gates — every one I ran, verbatim, at the tip

All run from the clone at `432268cf` through `~/bin/suite-run`.

| gate | command | result | claimed |
|---|---|---|---|
| babashka suite (the scanning gate) | `~/bin/suite-run bb test/run_all.clj` | `Ran 915 tests containing 7202 assertions. 0 failures, 0 errors.` | 915/7202/0 ✅ |
| JVM suite | `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 717 tests containing 8497 assertions. 0 failures, 0 errors.` | 717/8497/0 ✅ |
| operation oracle | `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | pass ✅ |
| txn kernel warnings | `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` | 2/0 ✅ |
| battery self-test | `make memory-battery-self-test` | `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` | 32/171/0 ✅ |
| parser-admission red witness, GREEN mode | `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` | 6/6 ✅ |
| transaction-kernel memory witness | `make memory-red-kernel` | `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.` — `heap-used-peak-mb 253.73` at `xmx-mb 256.0` | 4/25/0 ✅ |
| temp-dir hygiene ratchet | `make tmp-leak-ratchet-self-test` | `tmp-leak ratchet witness passed` (all seven `SELF_TEST_TMP` redirections correct) | PASS ✅ |
| admit analyzer memory | `make admit-analyzer-memory-self-test` | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` | 3/3 ✅ |

**One note on `make memory-red`.** Its default is `PARSER_RED_EXPECT=red`, and at this tip the
default invocation **fails**, correctly: `memory-red: 0/3 assertions held (expect=red) — FAIL`,
every arm reporting `:outcome :parser-admission-refused`. That is the red witness no longer
reproducing red because the admission fix landed — which is why the claimed figure is
green-mode 6/6. I flag it only so nobody runs the bare target later and reads the failure as a
regression; the target's own default is now the wrong mode for this tree, and a line in the
build record or a flipped default would save the next reader the detour.

**One gate I could not isolate.** `admit-patch-test alone (114/2122/0)`: the
`:clj-surgeon/mcp-test` alias pins `:main-opts ["-m" "clj-surgeon.mcp-test-runner"]`, so
`-e`/`-n` overrides are ignored and every attempt ran the whole suite. I did not hand-build a
classpath to get around it. The containing suite is green at 717/8497/0, which covers the
namespace; I simply cannot quote the isolated subtotal, and I do not.

**The intent audit (claimed 369/0).** There is no `make` target and no script under this repo
for it — `grep -rn "intent-audit\|intent_audit"` over `Makefile`, `*.edn`, `*.sh` and `*.clj`
returns nothing, and the target list has no audit entry but `cclsp-client-audit` and
`worktree-audit`. The auditor is external to the repo (the linked-intent tooling). **I did not
run it and I do not confirm 369/0.**

**Temp-dir leakage, counted as round five was asked.** The box is shared and the number churns:

```sh
ls -1 /var/tmp/forge | wc -l          # 1649
find /var/tmp/forge -maxdepth 1 -type d | wc -l   # 1591
df -i /var/tmp | tail -1              # 4789471 / 39438480 inodes, 13% used
```

1591 directories at 12:40Z, up from the 621 round five measured this morning — several seats
are running JVM suites concurrently (load average 6–8 throughout my run). Inode pressure is
unchanged at **13% used, 87% remaining**, so nothing is at risk. **I deleted none of them**;
they are not this branch's and the tree is multi-tenant. The temp-dir hygiene ratchet that
landed on the trunk in this merge is the right long-term answer and its witness passes.

---
## 11. Mergeability — the builder's claim held when made, and the trunk has moved since

The builder claims `merge-tree` clean against `3a8183f7`. **That reproduces exactly:**

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git merge-tree --write-tree HEAD 3a8183f7 >/dev/null 2>&1; echo "EXIT=$?"
```

```text
EXIT=0
```

But `MCP/main` has advanced past it — the q5z alias_migration verb landed, plus a merge-fix —
and against the **current** trunk the merge no longer applies cleanly:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git log --oneline -3 origin/MCP/main && git merge-tree --write-tree HEAD origin/MCP/main
```

```text
44e70af5 merge-fix: re-pin the refusal enumeration at 143 on the MCP/main landing
25ce3fa9 land: alias_migration verb (bridge/q5z-alias-migration @ ab34c93c) onto MCP/main
3a8183f7 queue+log: MEM-003 r6 built 69e58b41; trunk merge step before review

EXIT=1

Auto-merging src/clj_surgeon/mcp_change_buffer.clj
Auto-merging src/clj_surgeon/mcp_cold_verify.clj
Auto-merging src/clj_surgeon/mcp_server.clj
CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_server.clj
Auto-merging src/clj_surgeon/mcp_tool.clj
CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_tool.clj
Auto-merging test/clj_surgeon/mcp_alias_migration_test.clj
CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_alias_migration_test.clj
```

**The trunk sha I tested is `44e70af5`.** Three conflicted files, all on the q5z side
(`mcp_server.clj`, `mcp_tool.clj`, `mcp_alias_migration_test.clj`) — tool-registration and
refusal-enumeration territory, not measured-clock territory, so this is ordinary landing work
rather than a design collision. It does mean the branch needs one more trunk merge before it
can land, and — given `44e70af5` is itself a "re-pin the refusal enumeration at 143" merge-fix
— **the merged result's own enumeration counts must be re-verified after that merge, not
inherited from either side.**

---
## 12. The memory battery, run ONCE at the MERGED tip

Run under the exclusive suite lock with a fresh `MEMBAT_ROOT` under `/home/forge/tmp`, never
`MEMBAT_ALLOW_ANY_ROOT`, reference built explicitly first:

```sh
flock /home/forge/tmp/suite.lock bash -c '
  cd /home/forge/tmp/sol/mem003r4-wt
  make memory-battery-reference MEMBAT_ROOT=/home/forge/tmp/membat-r6opus
  make memory-battery           MEMBAT_ROOT=/home/forge/tmp/membat-r6opus'
```

**This run replaces the builder's stated caveat.** The build record attests the battery at
`69e58b41` — *before* the trunk merge. Mine attests the merged tip:

```text
:attestation {:ops [:cli-ls-tree :workspace-sources-read-all :rename-ns-plan-narrow :rename-ns-plan-full-match],
              :jvm "21.0.12",
              :head-sha "432268cf40997ef7694ad44d8a6f7ff06e18ce35"}
```

Verdict and lines, verbatim:

```text
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 9.9, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 40.9, :limit 6.5, :small-n-observed 4.5, :slack-mb 2.0}
  … 9 TREND lines …
  UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, …}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, …}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, …}
  UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, …}

receipt: /home/forge/tmp/membat-r6opus/receipts/20260904T130514.706752820Z-battery.edn
```

Reference parity, read from the receipt rather than from the console:

```text
cells: 48   reference-mismatch cells: 0
distinct :reference-mismatch values: (nil)
tool-errors: []
```

**Everything matches the claim: `FAIL (INCOMPLETE)` exit 1, reference-mismatch-count 0, the two
known `held-scales-with-n`, four `UNMEASURED`.** All four MEM-001-lane items, unchanged in kind
by this branch.

**One incidental confirmation worth recording.** My first attempt pre-created the root with
`mkdir -p`, and the battery refused rather than proceeding:

```text
REFUSED: MEMBAT_ROOT exists without its marker: /home/forge/tmp/membat-r6opus
{:reason :membat-root-unmarked, :root "…", :remedy "point MEMBAT_ROOT at a fresh directory, or create …/.membat-root yourself if you are certain this root was built by the battery"}
```

A typed refusal, named reason, remedy in the payload, non-zero exit. That is the house shape
and it worked on a reviewer's mistake, which is the only honest way to find out.

---

## 13. What I did not do

- I did not run the **intent audit** (claimed 369/0) — there is no runner in this repo, see §10.
- I did not isolate **`admit-patch-test`** (claimed 114/2122/0) — the alias pins `:main-opts`,
  see §10. It is green inside the 717/8497/0 suite.
- I did not re-export the **round-three tree** to re-prove the recovery and byte-estimate
  fail-firsts; I accept round five's reproduction and verified the witnesses live and green
  here (§9).
- I did not drive **`prune-details!`** again. `git diff --stat dc6ee93f..432268cf --
  src/clj_surgeon/mcp_alias_migration.clj src/clj_surgeon/txn_journal.clj` is **empty** — the
  `:control` sites are byte-identical to the tree round five drove — so I re-read them and
  carried that ruling forward rather than re-running it.
- I started **no server** on any port, and contacted none.
- I **deleted nothing** under `/var/tmp/forge` except the previous reviewer's
  `mem003r6-review-fx` fixtures (as instructed) and my own.

---

## 14. Summary of findings

| # | finding | tier |
|---|---|---|
| 1 | `.getMethod`/`Reflector` reach `_launder` by **string name**; `(. r _launder)` reaches it by the **dot special form** — all three plant a clock number in the parity hash, gate green | **BLOCKING** |
| 2 | `(Class/forName "java.lang.System")` + reflection publishes a raw `nanoTime` in the parity hash, invisible to the 159-spelling clock derivation | **BLOCKING** |
| 3 | `(. java.lang.System nanoTime)` — the dot special form with a fully-qualified class — publishes raw `nanoTime`; the simple-name form IS caught, the qualified one is not | **BLOCKING** |
| 4 | the clock floor union is one entry short of the JVM-minus-babashka difference, and the scanning gate is babashka-only | **BLOCKING** (compounding 2–3) |
| 5 | the diagnostic walker still misses a Java **array** and an **Iterator** (declared closed this round) | non-blocking |
| 6 | both allow-lists count matching **lines**, not calls; `valid-future-expiry?` is live proof (2 reads declared 1) | non-blocking |
| — | the `setAccessible` residual should say "named or positional" | wording |
| — | `make memory-red`'s default mode now fails at this tip; green-mode is the gate | wording |
| — | merge-tree conflicts against the current trunk `44e70af5` (clean against `3a8183f7`) | landing work |

**Every one of the five sabotage figures reproduced exactly (5/2/10/3/5), both round-five
blockers are genuinely closed, all nine gates I ran are green at the claimed numbers, and the
battery attests at the merged tip with zero reference mismatches.** The round did real work.
It is still NO-GO, because a clock number reaching a published receipt field inside the hashed
parity subject is the brief's blocking criterion, and six different ordinary spellings do it
with the ratchet green.

**The one-sentence fix that closes 1, 2 and 3 together:** both `escape-hatch-alternative` and
`clock-expression-alternative` should emit each derived name in **three** forms, not one — the
call spelling, the **quoted-string** spelling, and the **dot special form with an optional
fully-qualified prefix** — and each new form should get an entry in its
`…-the-ratchet-must-carry` map so the fail-first witness holds it. Every input needed is
already derived; nothing new has to be listed.

---

## NO-GO

**Mergeability:** this tip is **not** GO on its own for `MCP/main` — quite apart from the four
blocking findings, `git merge-tree --write-tree HEAD origin/MCP/main` against the current trunk
`44e70af5` conflicts in three files (`src/clj_surgeon/mcp_server.clj`,
`src/clj_surgeon/mcp_tool.clj`, `test/clj_surgeon/mcp_alias_migration_test.clj`) because q5z
landed since the builder's clean check at `3a8183f7`, so a round seven must merge the trunk
again, re-verify the refusal enumeration on the merged result, and close findings 1–4 before
this branch can land.
