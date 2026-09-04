## NO-GO

*(Round-eight independent review of clj-surgeon
`bridge/integration-2026-09-03-mem003` at `a2a15cc0`, MEM-003 measured clock, second
landing. Sol's content filter refused the brief; this is the Opus fallback, paths
substituted per the integrator's instruction.)*

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git rev-parse HEAD && git status --porcelain
```

```text
a2a15cc0f3f1192dca4221bda24562ac251f08a1
```

(`git status --porcelain` printed nothing.) Nothing in the clone was committed, staged,
stashed, pushed or edited. No server was started on any port; none of the forbidden ports
was contacted. Fixtures live only under `/var/tmp/forge/mem003r8-review-fx`, removed at the
end.

---

## Headline

Round seven closes **every one of the round-six blockers**: the three-form derivation
(call spelling, quoted string, dot special form with an optional fully-qualified prefix)
is real, the clock floor is now checked against a JVM manifest in both directions, the
walker handles arrays and iterators, and `sites` counts calls. I reproduced the round-six
plants going red at the tip.

**It is still NO-GO, and for the fifth round running it is the same shape one level down.**
The dot special form has **two** spellings in Clojure, `(. obj member)` and
`(. obj (member args))`, and the derivation emits only the first. The second is the form
the reader produces for `(.. obj (member))`, for `(memfn member)`, and is the spelling in
Clojure's own reference for the `.` special form. Findings below.

## 1. BLOCKING — the dot special form's OTHER spelling: `(. obj (member args))`, and `(.. obj (member))`

`clock-expression-alternative` (`test/clj_surgeon/measured_invariant_test.clj:282-321`)
emits the dot special form as

```
\(\.\s+(?:[\w.]*\.)?\QSystem\E\s+\QnanoTime\E\b
```

— receiver, whitespace, member. `escape-hatch-alternative` (`:664-693`) does the same:

```
\(\.\s+(?:\([^()]*\)|\S+)\s+\Q_launder\E\b
```

Clojure's `.` special form has **two** legal member spellings, and the reader macro `..`
and the `memfn` macro both expand to the second:

```
(. instance-expr member-symbol)
(. instance-expr (method-symbol args*))     ;; <- no alternative matches this
```

Probed against the live patterns from the tip's own tree
(`/var/tmp/forge/mem003r8-review-fx/probe1.clj`):

```sh
cd /var/tmp/forge/mem003r8-review-fx/base && bb -cp src:test /var/tmp/forge/mem003r8-review-fx/probe1.clj
```

```text
escape alternatives: 20
clock alternatives: 232
(let [c System] (. c nanoTime))                                    clock=false  escape=false
(.getMethod System (str "nano" "Time") (into-array Class []))      clock=false  escape=false
(.. System (nanoTime))                                             clock=false  escape=false
(. System (nanoTime))                                              clock=false  escape=false
((memfn getTimeInMillis) cal)                                      clock=false  escape=false
(doto (java.util.Date.) (.getTime))                                clock=true   escape=false
(System/nanoTime)                                                  clock=true   escape=false
(. System nanoTime)                                                clock=true   escape=false
(. java.lang.System nanoTime)                                      clock=true   escape=false
(.getMethod (Class/forName "java.lang.System") "nanoTime" ...)     clock=true   escape=false
(. r _launder)                                                     clock=false  escape=true
(._launder r)                                                      clock=false  escape=true
(.getMethod (class r) "_launder" (into-array Class []))            clock=false  escape=true
(clojure.lang.Reflector/invokeInstanceMethod r "_launder" ...)     clock=false  escape=true
(.. r (_launder))                                                  clock=false  escape=false
(. r (_launder))                                                   clock=false  escape=false
((memfn _launder) r)                                               clock=false  escape=false
(.getDeclaredMethod (class r) (str "_lau" "nder") ...)             clock=false  escape=false
(.getMethod java.util.Calendar "getInstance" (into-array Class []))clock=false  escape=false
(clojure.lang.Reflector/invokeStaticMethod "java.lang.System" ...) clock=true   escape=false
(clojure.lang.Reflector/invokeNoArgInstanceMember r "_launder")    clock=false  escape=true
```

Every route the round-six review found is now `true`. Six new ones are `false`.

### 1a. The plants, on `git archive a2a15cc0` copies, one line at `src/clj_surgeon/mcp_hot_verify.clj:114`

The same receipt-building site rounds five and six used. Each adds:

```clojure
:verification_wall_ms (let [rr (measured/elapsed-ms started)] <FORM>)
```

Scanning gate, under babashka, the runtime the gate actually runs in:

```sh
for d in base N1 N2 N3 N4 N5 N6 N7; do (cd /var/tmp/forge/mem003r8-review-fx/$d && \
  bb -cp src:test -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (println (pr-str (t/run-tests (quote clj-surgeon.measured-invariant-test))))'); done
```

```text
base  {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
N1    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
N2    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
N3    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
N4    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
N5    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
N6    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
N7    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
```

**All seven plants leave the scanning gate at the baseline, 27 tests / 156 assertions /
0 failures.**

### 1b. And the numbers really are published, inside the hashed parity subject

Through the real shared finalizer `mcp-operation/invoke!`, on the JVM
(`/var/tmp/forge/mem003r8-review-fx/runtime_probe.clj`):

```sh
cd /var/tmp/forge/mem003r8-review-fx/base && ~/bin/suite-run clojure -Sdeps '{:paths ["src" "test"]}' -M /var/tmp/forge/mem003r8-review-fx/runtime_probe.clj
```

```text
N1   value=1979523217299996  hashed-field=1979523217299996  unpartitioned=()
N2   value=1979523226620750  hashed-field=1979523226620750  unpartitioned=()
N3   value=0.025328          hashed-field=0.025328          unpartitioned=()
N4   value=0.025328          hashed-field=0.025328          unpartitioned=()
N6   value=0.025328          hashed-field=0.025328          unpartitioned=()
N7   value=946               hashed-field=946               unpartitioned=()
```

| plant | form | what it publishes | in scope of the stated control? |
|---|---|---|---|
| **N1** | `(. System (nanoTime))` | **raw 16-digit monotonic clock** | **YES** — both the class and the method are ordinary source tokens; MCP-OP-TIME-005 forbids "whatever spelling names it" |
| **N2** | `(.. System (nanoTime))` | **raw 16-digit monotonic clock** | **YES** — `..` is the reader's own sugar for N1 |
| **N3** | `(. rr (_launder))` | the tagged reading's number | **YES** — MCP-OP-TIME-006 names "the dot special form" explicitly |
| **N4** | `((memfn _launder) rr)` | the tagged reading's number | **YES** — `memfn` expands to N3; the name is a source token |
| N5 | `(let [c System] (. c nanoTime))` | — | **does not compile**: `No matching field found: nanoTime for class java.lang.Class`. The route is closed, by Clojure rather than by the ratchet — and a static reached through a local *must* go back through a method-name string, which the derivation carries. **Credit to the builder: this attack fails.** |
| N6 | `(.getDeclaredMethod (class rr) (str "_lau" "nder") …)` | the tagged reading's number | **NO** — the name is computed at runtime and spelled nowhere. Honestly outside a source-text scan's reach; see §4. |
| N7 | `(.getMethod java.util.Calendar "getInstance" …)` then `(.get cal 14)` | `Calendar.MILLISECOND`, a clock-derived number | **partly** — see finding 3, the morpheme narrowing |

**N1 is the finding I would put first.** It is four tokens, it is the spelling in Clojure's
own reference page for the `.` special form, it needs no reflection and no `Reading`, it
publishes a raw sixteen-digit `nanoTime` into an undeclared receipt field inside the hashed
parity subject, and the gate is green. It is `MCP-OP-TIME-005`'s literal subject.

The class is the round-six review's sentence with one word changed: **a derivation over
names cannot see a call that names nothing — and it cannot see a call that spells the same
names in a shape the alternative did not anticipate either.** Round six caught the dot form
`(. r m)`; the reader also accepts `(. r (m))`, and `..` and `memfn` emit only the second.

### What would close finding 1

Both alternative builders already branch on `(str/starts-with? spelling "(. ")`. Each needs
the member position to admit a parenthesised member as well as a bare one:

- clock: `\(\.\s+(?:[\w.]*\.)?\QSystem\E\s+\(?\s*\QnanoTime\E\b`
- escape hatch: `\(\.\s+(?:\([^()]*\)|\S+)\s+\(?\s*\Q_launder\E\b`

and a `..`/`memfn` face — `\(\.\.\s+…` and `\(memfn\s+\Qm\E\b` — as a fourth and fifth
alternative per derived name, each with an entry in the corresponding
`…-the-ratchet-must-carry` map so the fail-first witness holds it. Every input is already
derived; nothing new has to be listed. **A ratchet on the ratchet is also available and is
cheaper than the next round:** the file already holds `round-six-review-plants` as data;
adding these forms there makes the witness fail first without a new test being written.

## 2. NON-BLOCKING — the second narrowing has a real gap: a class named as a bare SOURCE SYMBOL

Brief item (c): *the two narrowings argued not convenient — find a real site either misses.*

**Narrowing 1 (`measured/<var>` gets no string form) — HOLDS, and the argument is sound.**
The claim is that a string cannot name a Clojure var without a var-resolution API, and every
such API is already `:reflective`. I read the rule (`measured_invariant_test.clj:1109-1149`).
`reflective-namespace-spelling` catches the namespace as a quoted symbol, a string **or** a
keyword; `var-resolution-api` independently catches the *call* —
`ns-resolve|requiring-resolve|find-var|ns-interns|ns-publics|ns-map|intern` plus `(resolve `
and `(var `. I probed the routes the round-five review used and three the builder did not
name: `(clojure.lang.RT/var "clj-surgeon.measured" "value")`,
`(requiring-resolve (symbol "clj-surgeon.measured" "value"))` and
`(var-get (get (ns-publics 'clj-surgeon.measured) 'value))` are each an offence — the first
two on the string spelling, the third on both clauses:

```sh
cd /var/tmp/forge/mem003r8-review-fx/base && bb -cp src:test /var/tmp/forge/mem003r8-review-fx/probe_naming.clj
```

```text
(clojure.lang.RT/var "clj-surgeon.measured" "value")             offence=:reflective
(requiring-resolve (symbol "clj-surgeon.measured" "value"))      offence=:reflective
(var-get (get (ns-publics 'clj-surgeon.measured) 'value))        offence=:reflective
((ns-resolve 'clj-surgeon.measured 'unwrap-readings) x)          offence=:reflective
(resolve (symbol (str "clj-surgeon" ".measured") "value"))       offence=nil
[clj-surgeon.measured :as measured]                              offence=nil
(measured/elapsed-ms started)                                    offence=nil
```

The last three are the correct answers: the sanctioned require and an ordinary call are not
offences, and the fifth spells no namespace anywhere (§4). **I found no in-scope site this
narrowing misses.** Accept as argued.

**Narrowing 2 (a clock METHOD name gets a string form only when it carries a clock morpheme)
— does miss a real shape, and the gap is not the method, it is the CLASS.**
`derived-clock-expressions` (`:218-281`) emits each source class **only** as
`"java.lang.System"` — a quoted string. It never emits `java.lang.System` or `System` as a
bare source token. The builder's argument for the morpheme narrowing is that
*"`Class/forName` cannot reach `Calendar/getInstance` without `"java.util.Calendar"`"* — true,
but `Class/forName` is not the only way to obtain a class. On the JVM the class **is** an
ordinary source symbol:

```clojure
(.getMethod java.util.Calendar "getInstance" (into-array Class []))
```

Neither `java.util.Calendar` (bare) nor `"getInstance"` (morpheme-free) is a clock
alternative, and my probe confirms `clock=false escape=false`. Plant **N7** carries this to a
receipt (`(.get cal 14)` — `Calendar.MILLISECOND`, which `.get` reaches positionally so no
accessor name is spelled): `hashed-field=946`, gate green.

I rate this **non-blocking**, and I want to be exact about why, because it is the same class
as finding 1 and gets a different tier. Every *ordinary* way of turning that `Calendar` into a
number is closed — `.getTimeInMillis`, `.getTime`, `Calendar/getInstance` are all
alternatives — so N7 has to combine the symbol-named class with a positional field read. It is
a two-step contrivance, where finding 1's N1 is four tokens of idiomatic Clojure. **The fix is
one line and I would take it in the same commit as finding 1:** emit each source class's
fully-qualified name UNQUOTED as an alternative too. The `:control` cost is bounded and
checkable — the builder already measured the string forms' collision cost as exactly one site.

---

## 3. Verified — the round's own claims, checked by re-derivation rather than by reading

### (a) The refusal enumeration at 145 — verified by ENUMERATION, on all three trees

The pin (`test/clj_surgeon/mcp_alias_migration_test.clj:5834,5931`) is checked against the
derived `refusal-kinds-in-source`, so I ran the derivation itself on the tip and on **both**
parents of the trunk merge `638b4169` (`432268cf` the branch side, `38f0f95c` the trunk side):

```sh
cd <tree> && java -cp "$(clojure -A:clj-surgeon/mcp-test -Spath)" clojure.main enum.clj
```

```text
--- a2a15cc0 (tip)
count= 145
invalid-measured-start? true
unpartitioned-measured-field? true
--- 432268cf (branch parent)
count= 26
invalid-measured-start? false
unpartitioned-measured-field? false
--- 38f0f95c (trunk parent)
count= 143
invalid-measured-start? false
unpartitioned-measured-field? false
```

**Confirmed, and the claim is if anything understated:** neither parent produced 145 — the
branch side enumerated 26 (the file-scoped enumeration is q5z's, and arrived with the trunk),
the trunk side 143 — and both new kinds are present at the tip and absent from both parents.
145 is a property of the composition. The `is (= 145 (count kinds))` is bound to the derived
set with a two-way `difference`, so it is a pin on a derivation and not a literal.

### (b) The five plants A/B/H/D/K unseen at `638b4169`, named at the tip — CONFIRMED

Both patterns loaded from each tree's own source and the plant sources matched against them:

```text
### at 638b4169 (the trunk merge, pre-fix) ###
escape alts: 11  clock alts: 161
A escape seen=false   B escape seen=false   H escape seen=false
D clock  seen=false   K clock  seen=false   D2 clock seen=false   K2 clock seen=false
### at a2a15cc0 (the tip) ###
escape alts: 20  clock alts: 232
A escape seen=true    B escape seen=true    H escape seen=true
D clock  seen=true    K clock  seen=true    D2 clock seen=true    K2 clock seen=true
```

Seven for seven, false → true. The derivation counts match the build record's table exactly
(escape 11 → 20 in the union; clock 161 → 232).

### (d) The builder's own correction — CONFIRMED, and the record is durable

`git status --porcelain` at the tip is empty. `ec143202`'s message states plainly that
`638b4169` carries the merge resolutions and **not** the two post-`git add` fixes, that the
merge commit's quoted suite figures have the working tree as their subject rather than the
commit, and that every commit from `638b4169` to `d93736aa` has a red JVM suite. The same
correction is repeated in the committed build record
(`docs/observations/mem003-second-landing-round7-build.md`, "A correction to this round's own
record"), which is **durable and in the branch** — the brief's requirement that the working-tree
figures be marked as such somewhere durable is met in two places.

`ec143202..a2a15cc0` is a single docs-only commit (271 lines, the build record), so a green
suite at `a2a15cc0` attests `ec143202`'s code exactly. My independent RED/GREEN check is in §5.

This is the right way to handle it and it deserves saying: the builder found a receipt whose
subject was wrong, in its own favour, at the end of a round, and gave it its own commit rather
than a quiet fold-in.

---

## 4. The sixth form, and what is honestly OUTSIDE the control's stated scope

The brief asked me to plant a sixth form no plant covers, and to say which of my six are
caught and which are honestly outside the control's stated scope. I planted seven. My ruling,
against the amended requirement text rather than against my own taste:

`MCP-OP-TIME-005` says *"a clock read shall be an offence whatever spelling names it,
INCLUDING a call that names its target as a STRING or by POSITION rather than as a source
token … or any other route that spells no class name and no method name the scan can read"*,
and then states the implementation obligation as *"the call, the quoted-string, and the
dot-special-form face of every derived name."*

- **N1 `(. System (nanoTime))`, N2 `(.. System (nanoTime))`, N3 `(. rr (_launder))`,
  N4 `((memfn _launder) rr)` — INSIDE the stated scope, and missed.** All four spell both
  the class/receiver and the member as ordinary source tokens. Two of them (`..`, `memfn`)
  are *macros in `clojure.core` whose entire job* is to emit the parenthesised member form,
  so they are not exotic spellings a reviewer invented — they are the sugar a developer
  reaches for. The requirement's own words cover them and the pattern does not. **Blocking.**
- **N7 `(.getMethod java.util.Calendar "getInstance" …)` + `(.get cal 14)` — INSIDE the
  stated scope by the requirement's text** ("names its target … by POSITION"), missed, but a
  two-step contrivance. **Non-blocking**, finding 2.
- **N5 `(let [c System] (. c nanoTime))` — the route DOES NOT EXIST.** Clojure evaluates
  `System` to a `java.lang.Class` object and `(. c nanoTime)` is then an *instance* member
  access on it: `IllegalArgumentException: No matching field found: nanoTime for class
  java.lang.Class`. To read a static through a class held in a local you must go back through
  `.getMethod` with a **method-name string**, which the derivation now carries. **The attack
  fails; credit to the round-seven fix.**
- **N6 `(.getDeclaredMethod (class rr) (str "_lau" "nder") …)` — HONESTLY OUTSIDE the reach
  of any source-text scan**, and I do not hold it against the branch. The name exists only at
  runtime; no regex over source can see it. The same is true of
  `(resolve (symbol (str "clj-surgeon" ".measured") "value"))`, which my naming probe shows
  returns `offence=nil`. **But the requirement claims it anyway** — "any other route that
  spells no class name and no method name the scan can read" — and a requirement a text scan
  provably cannot satisfy is a requirement that will read as green forever. **This is a
  wording defect, not a code defect.** Either the residual paragraph in `measured.clj` should
  be widened from *fields* to *any member reached by a computed name* (it currently declares
  only `.getDeclaredField` with a computed name and the positional field route), or
  `MCP-OP-TIME-005`'s final clause should be narrowed to what a source scan can actually
  promise. I would take the first: the residual is already declared at the right tier and one
  sentence covers both.

**The general form, offered as the sentence the next round should keep:** round six's lesson
was *a derivation over names cannot see a call that names nothing.* Round seven fixed that and
inherited the sibling: **a derivation over names must also enumerate the GRAMMAR the names can
appear in, and Clojure's `.` special form has two member spellings, not one.** The three-form
rule (call / string / dot) is the right shape; it is one production short.

---

## 5. Fail-first discipline: the four sabotages, on `git archive a2a15cc0` copies

Each applied to its own export, diff confirmed, then the scanning gate run under babashka.
Baseline for all four: `Ran 27 tests containing 156 assertions. 0 failures, 0 errors.`

```sh
cd /var/tmp/forge/mem003r8-review-fx/<S> && bb -cp src:test -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (println (pr-str (t/run-tests (quote clj-surgeon.measured-invariant-test))))'
```

| sabotage | witnesses that went red | failures | build record |
|---|---|---|---|
| **S1** `escape-hatch-alternative` collapsed to the single token shape | `every-round-six-review-plant-is-seen-by-the-scan-that-owns-it`, `the-escape-hatch-pattern-carries-every-route-to-a-readings-number`, `the-escape-hatch-scanner-catches-every-route-planted-in-a-receipt` (×2) | **4** | 4 ✅ |
| **S2** the clock dot form loses `(?:[\w.]*\.)?` (the fully-qualified prefix) | `every-round-six-review-plant-is-seen-by-the-scan-that-owns-it`, naming **plant K2** `(. java.time.Instant now)` | **1** | S2b 1, naming K2 ✅ |
| **S3** the clock derivation stops emitting BOTH the method-string and the class-string forms | `every-round-six-review-plant-…` naming **plant D2**; `the-derived-clock-pattern-carries-every-jdk-time-shape`; `the-babashka-clock-floor-is-the-complete-jvm-difference`; **and `no-raw-clock-read-lives-outside-the-measured-namespace` on LIVE TREE CODE** | **4** | S3b 2, naming D2 (mine is a wider cut) ✅ |
| **S4** the floor entry `"(. Calendar getInstance"` removed | `the-babashka-clock-floor-is-the-complete-jvm-difference` | **1** | reproduces round-six finding 4 ✅ |

Verbatim, the two that matter most:

```text
S2: round-six review plant K2 — the fully-qualified dot form on a class the floor does not
    name, so only the PREFIX TOLERANCE sees it reaches a receipt field unseen by the clock
    scan: {} — source: (. java.time.Instant now)

S3: round-six review plant D2 — Class/forName on a class the FLOOR does not name, so only
    the DERIVED class string sees it … source: (.invoke (.getMethod (Class/forName
    "java.util.Calendar") "getInstance" (into-array Class [])) nil (into-array Object []))
```

**Two things here are better than the build record claims and should be recorded.**

**(i) S3 goes red on the LIVE TREE, not only on a plant.** `no-raw-clock-read-lives-outside-
the-measured-namespace` fails with `txn_journal/evidence-stat` at 3 declared and 2 scanned —
the third read is `(get attrs "lastModifiedTime")`, a real file-mtime read named by string that
became visible only because of this round's string forms. So the string-form derivation is
load-bearing on production source, not merely on planted fixtures. That is the strongest kind
of non-vacuity evidence available and neither the round nor I would have got it from a plant.

**(ii) The floor-manifest witness is genuinely a closure, and its failure message is
actionable.** S3's `the-babashka-clock-floor-is-the-complete-jvm-difference` names all 64
uncovered spellings and states the remedy in the assertion text — *"add each to
`clock-expressions-the-ratchet-must-carry` with the route it opens. (This runtime underives 72
spellings in total.)"* Round-six finding 4 asked for exactly this ratchet and it was built at
the right rung: it compares in **both** directions, so a stale manifest is itself a failure
rather than a silent weakening. **Round-six blocking finding 4 is closed.**

---

## 6. The two non-blocking round-six items: both genuinely closed

**Item 5 — the walker.** Round six found `unpartitioned-measured-paths` blind to a Java array
and to an `Iterator`. Re-probed at the tip through `measured/attach` on the JVM
(`/var/tmp/forge/mem003r8-review-fx/walker.clj`):

```text
java array   => ([:xs 0])
Iterator     => ([:xs])
lazy seq     => ([:xs 0])
ArrayList    => ([:xs 0])
java Map     => ([:xs "k"])
map KEY      => ([:clj-surgeon.measured/reading-as-key])
iterator still has next after the refusal? true
```

The array is **walked** and the reading located at index 0; the iterator is **refused at its
own path** without being walked, and `.hasNext` is still `true` afterwards — the refusal did
not consume it. That is the right call and the build record's reasoning for it is right too:
a walker that diagnosed an iterator would hand the boundary a verdict about a value it had
just destroyed. **Closed.**

**Item 6 — counts count CALLS.** `sites` (`:78-110`) now conjes
`(repeat (count (re-seq pattern code)) …)`. The two re-blessings are real and I saw the second
one confirmed from the opposite direction: sabotage **S3**, which removes the string forms,
fails `no-raw-clock-read-lives-outside-the-measured-namespace` with
`txn_journal/evidence-stat` declared **3** and scanned **2** — so the third declared read is
exactly the `"lastModifiedTime"` string the build record says it is, and the count is bound to
a real match rather than to a number somebody chose. `worktree_lifecycle/valid-future-expiry?`
is 2, the round-six reviewer's own live example. **Closed.**

---

## 7. Rulings on the declared-open items

- **`bench/event_timing.clj` `fail!` file-fatal, and `bench/` as an unscanned root.**
  Unchanged and correctly left open. `scanned-roots` is still `["src" "dev/experiments"]`.
  Not a merge condition; still the largest unscanned tree in the repo, and it grows.
- **§5b, a reading in METADATA.** Accepted as declared, unchanged: metadata does not
  participate in map equality, so the parity subject is unaffected and the reading is inert
  on the wire.
- **The `setAccessible` residual.** The round-six wording request landed verbatim
  (`src/clj_surgeon/measured.clj:141-151`): *"reflection over the type's fields BY ANY ROUTE,
  NAMED OR POSITIONAL"*, with both the computed-name and the positional plant-F spelling
  written out. **Accept.** One extension is owed and it is my §4 finding: the residual covers
  *fields* and should cover *any member reached by a computed name*, because
  `(.getDeclaredMethod (class rr) (str "_lau" "nder") …)` is the same class and is not
  mentioned.
- **Value equality as a bisection oracle.** Declared at `measured.clj:162-181` with the
  reviewer's own 67-step measurement quoted and the reason identity-only `=` was rejected.
  **Accept.**
- **`make memory-red`'s default mode.** Documented at the target
  (`Makefile:960-979`), with the reason the default is deliberately NOT flipped: *"a red
  witness whose default stops asking its own question is a witness that has quietly become an
  assertion."* That is the right call and it is now written where the next reader will hit it.
  **Accept.**
- **The floor union.** Round-six finding 4 is **closed** — see §5(ii). The floor is now a
  checked two-way difference against `test/fixtures/clock-spellings-jvm.edn`, regenerated by
  `make clock-spellings-manifest`, and a stale manifest is itself a failure.
- **The four `UNMEASURED` reserved peaks and the two `held-scales-with-n` FAILs.**
  MEM-001's lane, pre-existing at the base, unchanged in kind by this branch. **Not this
  branch's to close and not a reason to hold it.** The reason to hold it is finding 1.
- **Round three's two defects** (recovery `recover!` writing tagged readings to an on-disk EDN
  receipt; the pre-publication byte estimate measuring the raw map). Both remain closed; the
  live witnesses run inside the JVM suite, green at 770/10383/0 twice below.

---

## 8. How live is finding 1? — the honest context

`(. obj (member))`, `(.. obj (member))` and `memfn` appear **nowhere** in this repository:

```sh
cd /home/forge/tmp/sol/mem003r4-wt && grep -rnE '\(\.\s+\S+\s+\(' src dev/experiments --include=*.clj ; \
  grep -rnE '\(\.\.[[:space:]]' --include=*.clj . | wc -l ; grep -rn "memfn" src dev/experiments bench --include=*.clj
```

```text
(no matches for the paren-member dot form under src or dev/experiments)
0
(no matches for memfn)
```

**So finding 1 is a hole in the ratchet, not a live leak.** I state that plainly because it is
the fairest reading of the branch, and I still rate it blocking, for two reasons and not for
sentiment:

1. **The brief's own criterion is what I am applying**, unchanged from the round that wrote
   it: *a raw clock reading reaching a published receipt field or a hashed parity subject, or
   a tagged reading that can be laundered into a number by ordinary code, is BLOCKING.*
   `(. System (nanoTime))` is ordinary code and it reaches the hashed parity subject with the
   gate green.
2. **Rounds five and six were held on exactly this evidence shape** — plants, no live sites —
   and were right to be. Applying a softer standard in round seven would mean the standard
   tracks reviewer fatigue rather than the invariant. The whole value of this ratchet is that
   it catches the *next* commit, and the next commit is precisely where a `..` or a `memfn`
   would appear.

**It is one commit.** Two lines in `clock-expression-alternative` and
`escape-hatch-alternative` (admit a parenthesised member), two new faces per derived name
(`(.. recv (m` and `(memfn m`), and the corresponding `…-the-ratchet-must-carry` entries. The
plant data structure `round-six-review-plants` already exists to hold them, so the fail-first
witness comes free.

---

## 9. Gates — every one I ran, verbatim, on a FRESH CLONE at `a2a15cc0`

`git clone /home/forge/tmp/sol/mem003r4-wt /var/tmp/forge/mem003r8-review-fx/fresh`,
`git checkout a2a15cc0`, `git status --porcelain` empty. Ambient state owned rather than
inherited.

| gate | command | result | claimed |
|---|---|---|---|
| **JVM suite, run 1** | `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 770 tests containing 10383 assertions. 0 failures, 0 errors.` | 770/10383/0 ✅ |
| **JVM suite, run 2** | (same, immediately again) | `Ran 770 tests containing 10383 assertions. 0 failures, 0 errors.` | ✅ deterministic |
| babashka suite (the scanning gate) | `~/bin/suite-run bb test/run_all.clj` | `Ran 918 tests containing 7234 assertions. 0 failures, 0 errors.` | 918/7234/0 ✅ |
| operation oracle | `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | pass ✅ |
| intent audit | `clojure -M -e "(clj-surgeon.mcp-intent-contract/audit-current-repository)"` | `:ok true :specs 371 :violations 0` | 371/0 ✅ |
| txn kernel warnings | `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` | ✅ |
| battery self-test | `make memory-battery-self-test` | `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` | 32/171/0 ✅ |
| parser-admission red witness, GREEN mode | `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` | 6/6 ✅ |
| temp-dir hygiene ratchet | `make tmp-leak-ratchet-self-test` | `tmp-leak ratchet witness passed` | PASS ✅ |
| admit analyzer memory | `make admit-analyzer-memory-self-test` | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` | 3/3 ✅ |

**On the intent audit specifically:** round six could not run it and said so. It runs —
`clojure -M -e "(require 'clj-surgeon.mcp-intent-contract)(clj-surgeon.mcp-intent-contract/audit-current-repository)"`,
the invocation the build record names — and it returns `371` specs and `0` violations, exactly
as claimed. I withdraw round six's "there is no runner in this repo" on the record.

**On isolating `admit-patch-test` (114/2122/0):** I did not isolate it either, for the same
reason (`:main-opts` is pinned in the alias). It is green inside the 770/10383/0 suite, twice.
I do not quote the subtotal.

---

## 10. Verified first, and it deserves the credit: the round-six blockers are closed IN THE PRODUCTION PATH

Not merely at the pattern level. I re-planted three of the round-six routes as one line of
ordinary Clojure at the same `src/clj_surgeon/mcp_hot_verify.clj:114` on `git archive`
exports of the tip, and ran the scanning gate under babashka:

```text
R6H  (. rr _launder)                                              {:test 27, :pass 154, :fail 2}
R6K  (. java.lang.System nanoTime)                                {:test 27, :pass 154, :fail 2}
R6A  (.invoke (.getMethod (class rr) "_launder" ...) rr ...)      {:test 27, :pass 154, :fail 2}
```

Baseline is `27/156/0`. **All three go red, two failures apiece, at the exact site where they
were green one round ago.** Round-six blocking findings 1, 2 and 3 are closed at the rung the
requirement names, and finding 4 is closed by the manifest ratchet (§5(ii)). Two of the four
derived-pattern counts I checked against the build record's table matched to the alternative:
escape 11 → 20 in the union, clock 161 → 232.

The clock allow-list holds **41 `:control` entries and zero `:receipt` entries**, and the
witness that refuses a `:receipt` entry is live. No allow-list entry admits publication.

### The heavy memory gates

| gate | result | claimed |
|---|---|---|
| transaction-kernel memory witness | `make memory-red-kernel` → `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.` — journal arm `heap-used-peak-mb 254.48` at `xmx-mb 256.0`, flatness-600 `254.74` | 4/25/0, peak 253.58 ✅ (mine 254.5–254.7; same arm, box under load 8–14) |

### The memory battery, ONCE, at the tip, under the exclusive lock

```sh
flock /home/forge/tmp/suite.lock bash -c '
  cd /var/tmp/forge/mem003r8-review-fx/fresh
  make memory-battery-reference MEMBAT_ROOT=/home/forge/tmp/membat-r8opus
  make memory-battery           MEMBAT_ROOT=/home/forge/tmp/membat-r8opus'
```

A fresh `MEMBAT_ROOT` under `/home/forge/tmp`, reference built explicitly first, never
`MEMBAT_ALLOW_ANY_ROOT`. The attestation names the tip:

```text
:attestation {:ops [:cli-ls-tree :workspace-sources-read-all :rename-ns-plan-narrow :rename-ns-plan-full-match],
              :jvm "21.0.12",
              :head-sha "a2a15cc0f3f1192dca4221bda24562ac251f08a1"}

verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 9.9, :limit 3.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 40.9, :limit 6.5}
  … 9 TREND lines …
  UNMEASURED reserved-peak-over-budget ×4  (cli-ls-tree, rename-ns-plan-full-match,
                                            rename-ns-plan-narrow, workspace-sources-read-all)

receipt: /home/forge/tmp/membat-r8opus/receipts/20260904T173033.346984407Z-battery.edn
```

Parity read from the receipt rather than from the console:

```text
cells: 48
distinct :reference-mismatch: (nil)
reference-mismatch cells: 0
tool-errors: []
```

**Exactly the claimed state: `FAIL (INCOMPLETE)` exit 1, 48 cells, zero reference mismatches,
the two known `held-scales-with-n`, four `UNMEASURED`.** All four are MEM-001's lane,
pre-existing at the base and unchanged in kind by this branch.

---

## 11. Mergeability — clean against the shas the builder tested, conflicting against the trunk NOW

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git merge-tree --write-tree HEAD <sha> >/dev/null; echo $?
```

```text
vs 0daeb19c   EXIT=0
vs c2c19691   EXIT=0
```

Both claimed shas reproduce clean. But `MCP/main` has moved again since — it is now at
`b916feb5` (`log: vision amended 16:55Z`), and the txn lane's round eight landed
`src/clj_surgeon/txn_journal.clj` in between:

```text
vs origin/MCP/main (b916feb5)   EXIT=1
CONFLICT (content): Merge conflict in src/clj_surgeon/txn_journal.clj
CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_alias_migration_test.clj
```

This is ordinary landing work, not a design collision — the trunk side is the txn round-eight
sweep and the q5z enumeration, neither of which is measured-clock territory. **Two things
follow and both are load-bearing:**

- `txn_journal.clj` carries **eleven** of this branch's `:control` clock allow-list entries.
  After the merge those counts are a property of the composition and must be re-derived, not
  inherited — exactly as the refusal enumeration was (§3a). The scan does catch it: sabotage
  S3 went red on `txn_journal/evidence-stat` for a one-count drift, so the ratchet will name
  any site the merge moves.
- The refusal enumeration must be re-pinned on the merged result again. 145 was a property of
  the last composition; `mcp_alias_migration_test.clj` is one of the two conflicted files.

---

## 12. Housekeeping and what I did not do

- **`git status --porcelain` in the review clone is empty at the end**, HEAD still
  `a2a15cc0`. Nothing committed, staged, stashed, pushed or edited there. All mutation
  happened on `git archive` exports and one `git clone`, under
  `/var/tmp/forge/mem003r8-review-fx`, which I remove.
- **No server was started on any port**, and none of `7888 / 7890 / 7894 / 7895 /
  7906–7910 / 7941–8146` was contacted. Nothing needed one.
- **Temp-dir leakage, counted, not deleted.** `/var/tmp/forge` held **1817** entries (1741
  directories) at 17:08Z and **1257** at 17:13Z — it churns because several seats run JVM
  suites concurrently. Inodes: `4911636 / 39438480 used`, **87% remaining**. Nothing is at
  risk; the tree is multi-tenant and none of it is this branch's, so **I deleted none of it**.
- I did **not** isolate `admit-patch-test` (the alias pins `:main-opts`); it is green inside
  the JVM suite, twice.
- I did **not** re-export the round-three tree to re-prove the `recover!` and byte-estimate
  fail-firsts; I verified their witnesses live and green in the JVM suite and carried round
  five's reproduction forward.
- I did **not** drive `prune-details!` again — `git diff dc6ee93f..a2a15cc0 --
  src/clj_surgeon/mcp_alias_migration.clj` leaves its clock read where round five drove it.

---

## 13. Summary of findings

| # | finding | tier |
|---|---|---|
| **1** | the dot special form's parenthesised-member spelling — `(. System (nanoTime))`, `(.. System (nanoTime))`, `(. rr (_launder))`, `((memfn _launder) rr)` — matches no alternative in either derived pattern; all four publish a clock-derived number into an undeclared receipt field inside the hashed parity subject with the gate at 27/156/0 | **BLOCKING** |
| 2 | a clock source class named as a bare SOURCE SYMBOL is not a spelling at all, so `(.getMethod java.util.Calendar "getInstance" …)` + a positional field read escapes (the morpheme narrowing's argument assumes `Class/forName` is the only route to a class) | non-blocking |
| 3 | `MCP-OP-TIME-005`'s final clause promises to catch "any other route that spells no class name and no method name" — which no source-text scan can do; the declared residual covers computed FIELD names only and should cover any member reached by a computed name | wording |
| — | `bench/` is still an unscanned root and still the largest tree in the repo | declared open, accepted |
| — | merge-tree conflicts against the current trunk `b916feb5` in `txn_journal.clj` and `mcp_alias_migration_test.clj`; the clock allow-list counts and the 145 enumeration must be re-derived on the merged result | landing work |

**Closed this round, verified independently:** round-six blocking 1, 2, 3 (three plants go red
in the production path, two failures each), round-six blocking 4 (the floor is a checked
two-way difference against a JVM manifest, and its failure names all 64 uncovered spellings
with the remedy), round-six non-blocking 5 (array walked, iterator refused without being
consumed) and 6 (counts count calls, confirmed from the opposite direction by a sabotage
failing on live tree code).

**Claims verified rather than read:** the 145 enumeration by re-derivation on the tip and on
BOTH parents (145 / 26 / 143); the five plants unseen at `638b4169` and seen at the tip, seven
for seven; the RED window `638b4169..d93736aa` (the JVM suite at `d93736aa` is 2 failures, both
`the-refusal-enumeration-is-pinned-in-count-and-in-membership`) and `ec143202` as the first
green tree; all four sabotages red at 4 / 1 / 4 / 1; every gate at its claimed figure; the JVM
suite green twice on a fresh clone; the battery attested at `a2a15cc0` with zero reference
mismatches.

---

## NO-GO

Round seven is the best round of the seven and it closes every finding it was given: the
three-form derivation is real and non-vacuous on live tree code, the floor stopped being a
hand-maintained list and became a checked two-way difference, the walker and the counting rule
are fixed at the right rung, and the builder found and corrected a receipt of its own whose
subject was the working tree rather than the commit — the exact failure this branch has spent
seven rounds writing witnesses against, caught by its author and given its own commit instead
of a quiet fold-in. Every gate reproduces at the claimed figure, the JVM suite is green twice
on a fresh clone, the battery attests at the tip with zero reference mismatches, and the three
round-six blocking plants now go red at the production site where they were green a round ago.
**It is still NO-GO on one finding and one only:** Clojure's `.` special form has two member
spellings and the derivation emits one, so `(. System (nanoTime))` — four tokens, the spelling
in Clojure's own reference, the shape `..` and `memfn` expand to — publishes a raw sixteen-digit
`nanoTime` into an undeclared receipt field inside the hashed parity subject with all
twenty-seven tests and one hundred fifty-six assertions green. No live site in the repository
uses that form today, so this is a hole in the ratchet rather than a leak in the product; I
hold it anyway because it is the brief's own blocking criterion, because rounds five and six
were held on identical evidence, and because a ratchet's whole job is the commit that has not
been written yet. The fix is one commit and every input it needs is already derived: admit a
parenthesised member in both `…-alternative` builders, add the `..` and `memfn` faces, and put
each new form in its `…-the-ratchet-must-carry` map — the `round-six-review-plants` data
structure will carry the fail-first witness without a new test being written.

**Blocking:** finding 1 (the parenthesised-member dot form, `..`, and `memfn` in both derived
patterns).

**Non-blocking:** finding 2 (a clock class named as a bare source symbol); finding 3 (the
requirement and the declared residual disagree about what a text scan can promise — widen the
residual from "fields" to "any member reached by a computed name"); `bench/` unscanned;
`admit-patch-test` not isolable because the alias pins `:main-opts`.

**Landing work, independent of the above:** this tip is **not** GO on its own for `MCP/main` —
`git merge-tree --write-tree HEAD origin/MCP/main` is clean against `0daeb19c` and `c2c19691`
as claimed but conflicts against the current trunk `b916feb5` in `src/clj_surgeon/txn_journal.clj`
and `test/clj_surgeon/mcp_alias_migration_test.clj`, so round eight must merge the trunk again
and re-derive both the clock allow-list counts in `txn_journal.clj` and the 145 refusal
enumeration on the merged result rather than inheriting either.
