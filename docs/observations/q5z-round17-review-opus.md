## NO-GO

Round-seventeen independent review of clj-surgeon `bridge/q5z-alias-migration` at `15fdf59c`.
Reviewer: Opus (Sol's content filter refused this brief; run with substituted paths per the
launch note — verdict at `/home/forge/tmp/sol/q5z17-opus-review.md`, fixtures under
`/var/tmp/forge/q5z17-review-fx/opus`, Surgeon server on explicit `:port 8162`/`:port 8163`).

```text
$ cd /home/forge/tmp/sol/q5z14-wt && git rev-parse HEAD && git status --porcelain
15fdf59c5e9fc17fa37a31caf118780f8b631020
(no output — clean; no .nrepl-port present in the clone)
```

Nothing in the clone was committed, pushed, stashed or edited. Every sabotage and every plant
was applied to a `git archive` export beneath the fixture directory.

**Headline.** Three of the four round-seventeen items are genuinely closed, and closed well:
the form-deep mint walk catches every KEYWORD attack the brief named (§1), the allowlist now
has a witness bound to itself alone (§3, sabotage = exactly the claimed 6 failures), and the
per-receipt budget is real (§4, 32,011 ms → 2,022 ms). All four RED commits are red at their
shas and green at the following fix; all four gates are green at the claimed numbers; the
enumeration is pinned at 139 in count and membership both directions.

But §2 is **not** closed, and the reason is the same one the brief flagged as the builder's own
declared residual — and it is worse than declared. **Two BLOCKING defects, each reproduced
end to end against a matched control, each producing a live refusal kind the entrance really
emits and the enumeration does not contain:**

1. `own-refusal-constructor?` is still a TEXT regex, and one DOCSTRING on the verb's own
   `refusal` constructor silently switches the entire guard off for all 15 of its sites;
2. the string-literal residual is reachable at a `(refusal …)` site under a marker, exactly
   as the brief asked — `mint-evidence` reads keyword literals and is blind to string literals,
   while `(name error-type)` in every constructor accepts a string kind happily.

Baseline first, so both findings are read against a clean tip:

```text
$ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z17-review-fx/opus/probe3.clj'
enumerated= 139
unscannable-sites= []
dynamic-sites= []
reachable-ns-count= 20
contains ok?= false
```

The tip is clean today. Both findings below are holes in the GUARD, not missing kinds in the
tip — but the brief's own rule makes them blocking: *"A marked site that mints a refusal kind
and stays unenumerated, a `(refusal …)` site the scan cannot see … is BLOCKING."* Each one is
both.

---

### 1. BLOCKING — one docstring on the verb's own `refusal` constructor turns the whole guard off for that file, and `mcp-workspace` already trips the same regex today

`test/clj_surgeon/mcp_alias_migration_test.clj:1622` (`own-refusal-constructor?`), gating
`:1626` (`(for [site (when own-refusal-constructor? (refusal-call-sites-in text))`).

Round sixteen's finding was that call sites were found by a per-LINE regex. That is fixed —
sites are now read as forms. What was NOT fixed is that whether the scan runs **at all** for a
file is still decided by a text regex over the constructor's *argument name*:

```clojure
#"\(defn-?\s+refusal\s*\n?\s*\[\s*(error-type|kind)\b"
```

`\s*\[` cannot cross a docstring, a metadata map, or a `(def refusal (fn …))`. Driven directly
against the tip's own scanner, with an identical dynamic-minting body in every row and only the
constructor's shape varying:

```text
$ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z17-review-fx/opus/probe2.clj'
=== ITEM 2b: own-refusal-constructor? TEXT regex — does the whole file get skipped? ===
ctor: canonical                    sites=1  literal-kinds=[]  dynamic-reported=["plant:5"]
ctor: WITH DOCSTRING               sites=1  literal-kinds=[]  dynamic-reported=[]
ctor: MULTI-ARITY                  sites=2  literal-kinds=[]  dynamic-reported=[]
ctor: (def refusal (fn             sites=1  literal-kinds=[]  dynamic-reported=[]
ctor: metadata map                 sites=1  literal-kinds=[]  dynamic-reported=[]
ctor: arg named 'k'                sites=1  literal-kinds=[]  dynamic-reported=[]
```

`refusal-call-sites-in` finds the site in every row — the FORM scan is correct. The per-file
enable throws it away in five rows out of six.

**This is not hypothetical: one reachable namespace already fails the regex today.** Per
reachable namespace, gate versus sites actually present:

```text
=== per reachable ns: own-refusal-constructor? gate vs (refusal ...) sites present ===
clj-surgeon.alias-migration      defn-refusal=true  CTOR-REGEX-PASSES=true  (refusal ..)-sites=13 reported=[]
clj-surgeon.mcp-alias-migration  defn-refusal=true  CTOR-REGEX-PASSES=true  (refusal ..)-sites=15 reported=[]
clj-surgeon.mcp-workspace        defn-refusal=true  CTOR-REGEX-PASSES=false (refusal ..)-sites=4  reported=[]
```

`src/clj_surgeon/mcp_workspace.clj:8` has a docstring, so its 4 `(refusal …)` sites are skipped
wholesale right now. Today that is harmless *by luck* — its `refusal` takes `[message value]`
and spells `:error_type "invalid-workspace-root"` as a constant inside the constructor, so the
`:error_type "…"` scan already has it. The gate reaches the right answer for the wrong reason,
which is precisely why nobody has noticed.

**End to end, with a matched control.** Both trees are `git archive 15fdf59c` exports carrying
the SAME planted branch at the head of `validate-request`'s `cond`
(`src/clj_surgeon/mcp_alias_migration.clj:117`), routed from `handle-alias-migration`:

```clojure
(:review_kind params)
(refusal (keyword (:review_kind params)) "planted dynamic kind" {})
```

`plantN` is the control. `plantD` differs by **one docstring line** on that file's `refusal`:

```text
$ for T in plantN plantD; do (cd /var/tmp/forge/q5z17-review-fx/opus/$T && ~/bin/suite-run bash -lc '... clojure.main .../probe5.clj'); done
############ plantN  (control — no docstring) ############
enumerated= 139
unscannable-sites= ["src/clj_surgeon/mcp_alias_migration.clj:117"]
dynamic-sites= ["src/clj_surgeon/mcp_alias_migration.clj:117"]
live-kind= "planted-runtime-kind"
live-kind-enumerated= false
############ plantD  (one docstring on the ctor) ############
enumerated= 139
unscannable-sites= []
dynamic-sites= []
live-kind= "planted-runtime-kind"
live-kind-enumerated= false
```

The control names the site correctly — the round-sixteen fix works. Adding a docstring makes
`unscannable-sites` and `dynamic-sites` both **empty** while the entrance demonstrably emits
`"planted-runtime-kind"`, which is not among the 139. A `(refusal …)` site the scan cannot see,
by the brief's own rule.

The severity is not the docstring itself; it is *which* edit trips it. Every other function in
`mcp_alias_migration.clj` has a docstring. Adding one to `refusal` is the single most ordinary,
most house-style-encouraged edit anyone could make to that namespace, and it silently disables
the guard for all 15 of its sites with no test going red anywhere. That is the
`a-gate-a-caller-can-turn-off` class: a control that a routine, well-intentioned edit switches
off without saying so.

**Fix.** The namespace already reads this text with the reader twice over. Decide the
constructor the same way: find the top-level form whose head is `defn`/`defn-`/`def` and whose
name is `refusal`, take its first argument vector by walking children (skipping docstring and
metadata nodes, and handling multi-arity), and ask whether its first parameter is the kind.
Deciding by text in a file you are already parsing is the defect class this requirement has now
paid for three times — the round-sixteen commit message says so itself, one level down.
Cheaper and stronger still: drop the per-file enable entirely and let a marked, mechanically
forwarding site be the only exemption, which is the rule every other site already lives under.

**Adjacent, NON-blocking (same function, latent).** `refusal-call-sites-in:1546` matches only a
list whose head is the literal symbol `refusal`, so an aliased or applied constructor is
invisible even when the per-file gate passes:

```text
=== ITEM 2a: shapes that hide a (refusal ...) call from the FORM scan ===
control: plain dynamic       sites=1  dynamic-reported=["plant:5"]
reader conditional           sites=1  dynamic-reported=["plant:5"]
inside a defmacro body       sites=1  dynamic-reported=["plant:5"]
inside #_ discard            sites=1  dynamic-reported=["plant:5"]
aliased via def              sites=0  dynamic-reported=[]
aliased via let              sites=0  dynamic-reported=[]
(apply refusal ...)          sites=0  dynamic-reported=[]
kind on the NEXT line (lit)  sites=1  literal-kinds=["next-line-kind"]
'refusal' in a string        sites=0  dynamic-reported=[]
```

The last two rows are round sixteen's finding, and both directions are now correct — the
next-line kind is enumerated, the string false positive is gone. Reader conditionals and macro
bodies are seen (right). `#_` discard is a conservative false positive: it names a site that
cannot execute, which fails closed and is noise, not a hole. `rg -n "\(apply refusal|\(def refusal"
src/` returns nothing, so no reachable source has the aliased shape today. Rule: **acceptable-declared,
with a note** — worth one line in the docstring saying the scan assumes `refusal` is never aliased
or applied, so the assumption is written down rather than implied.

### 2. BLOCKING — `mint-evidence` reads KEYWORD literals and is blind to STRING literals, so the declared residual is reachable at a marked `(refusal …)` site

`test/clj_surgeon/mcp_alias_migration_test.clj:1291` (`mint-evidence`), line 1317:

```clojure
(when (and (keyword? value) (not head?) (not selector?) ...)
```

and `binding-mints?:1372`, which relies on the same evidence. A keyword literal in a value
position is a mint; the identical string literal is not. Every `refusal` constructor in the
reachable set forwards its kind through `(name error-type)`, and `(name "brand-new-kind")` is
`"brand-new-kind"` — a string kind is a perfectly good kind.

Driven against the tip's own predicate (`FORWARDED=true` means EXEMPT, i.e. the attack wins):

```text
$ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z17-review-fx/opus/probe1.clj'
A cond->        (cond-> kind true name true keyword)         FORWARDED=false  mint-evidence=["mints with `keyword`"]
B as-> threaded (-> kind (as-> k (keyword (name k))))        FORWARDED=false  mint-evidence=["mints with `keyword`"]
C apply keyword (apply keyword [(name kind)])                FORWARDED=false  mint-evidence=["mints with `keyword`"]
D let f=keyword (let [f keyword] (f (name kind)))            FORWARDED=false  mint-evidence=["mints with `keyword`"]
E case          (case x :a :new-kind :b kind)                FORWARDED=false  minted-kinds=#{"a" "b" "new-kind"}
F if literal    (if p :new-kind kind)                        FORWARDED=false  minted-kinds=#{"new-kind"}
G or literal    (or (:kind m) :new-kind)                     FORWARDED=false  minted-kinds=#{"new-kind"}
H helper call   (mk kind)                                    FORWARDED=false  mint-evidence=[]
I str-table get (get {"a" "brand-new-kind"} k)               FORWARDED=true   mint-evidence=[]
J kw-table get  (get {:a :brand-new-kind} k)                 FORWARDED=false  minted-kinds=#{"a" "brand-new-kind"}
K some->name-kw (some-> (:review_kind params) name keyword)  FORWARDED=false  mint-evidence=["mints with `keyword`"]
L legit forward (some :error-type (remove :ok checks))       FORWARDED=true   mint-evidence=[]
M bare symbol   kind                                         FORWARDED=true   mint-evidence=[]
N let-bound lit k   (let [k :planted-kind] ...)              FORWARDED=false  mint-evidence=[]
O let-bound str k   (let [k "planted-str-kind"] ...)         FORWARDED=true   mint-evidence=[]
P get-in table  (get-in {:a {:b :tbl-minted}} [kind :b])     FORWARDED=false  minted-kinds=#{"a" "b" "tbl-minted"}
```

Every attack the brief named is answered — A, B, C, D, K (the minting symbol in argument,
threaded, applied and let-bound-to-a-symbol positions), E, F, G, J, P (the keyword-literal
sources, including the literal table through `get` and `get-in`), N (the let-bound keyword
literal). L and M stay correctly exempt. **H is the helper-call question the brief asked me to
rule on:** `(mk kind)` where `mk` mints elsewhere reports `FORWARDED=false`, so an opaque helper
call is NAMED by the guard rather than trusted — a typed refusal, which is the right answer and
not merely acceptable-declared. Good.

**I and O are the hole, and they are the same hole:** replace the keyword literals with string
literals and the expression becomes a forward. Note `minted-kinds-in` DOES read the string
(`#{"a" "brand-new-kind"}`), which is why this is invisible in both directions — at an
`:error-type` value site the marked expression is exempt, so `structural-error-type-kinds`
contributes nothing, while `runtime-spelled-kind-sites:1626`'s `(empty? (minted-kinds-in …))`
test is false, so it is not reported either.

**End to end.** A `git archive 15fdf59c` export with the brief's own shape planted, MARKED, at
the head of `validate-request`'s `cond` and routed from `handle-alias-migration`:

```clojure
;; @spec MCP-OP-ALIAS-059
;; forwarded-refusal-kind: relays a kind another scanned source minted
(:review_kind params)
(refusal (get {"planted-runtime-kind" "brand-new-kind"} (:review_kind params))
         "planted string-table kind"
         {:remedy "planted"})
```

```text
############ plantI ############
enumerated= 139
unscannable-sites= []
dynamic-sites= []
live-kind= "brand-new-kind"
live-kind-enumerated= false
```

So: a marked site that mints a refusal kind and stays unenumerated. The brief asked "is it
reachable at any `(refusal …)` site?" — **yes, verified by execution**, and it is reachable at
an `:error-type` value site too, by the same blindness.

**Fix.** One clause: treat a string literal exactly as a keyword literal in `mint-evidence`'s
second shape (`(or (keyword? value) (string? value))`), keeping the same head/selector
exemptions. `minted-kinds-in` already reads strings, so nothing downstream changes; row L
(`(some :error-type (remove :ok checks))`) carries no string literals and stays exempt. Ratchet:
add row I and row O to `the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped` — the
existing witness has the keyword twins already, so the string twins are two lines beside them.

### 3. CLOSED — the allowlist's own witness is real, and the sabotage produces exactly the claimed 6 failures

Sabotaging **only** `print-safe-leaf?` on a `git archive 15fdf59c` export
(`src/clj_surgeon/mcp_tool.clj:1351`), restoring the pre-fix `instance?`-based predicate and
leaving `write-safe-leaf`'s guard and the time budget untouched:

```text
$ (cd /var/tmp/forge/q5z17-review-fx/opus/sab3 && ~/bin/suite-run bash -lc '... clojure.main .../run_var.clj ...')
VAR= the-scalar-allowlist-refuses-a-benign-subclass-of-an-admitted-class
FAIL ... a Number SUBCLASS reached print-method and its own toString was published in the receipt · on its own · SABOTAGE-VISIBLE-42
FAIL ... a Number the allowlist does not admit rendered no identity marker · on its own · SABOTAGE-VISIBLE-42
FAIL ... · inside a map value · {:a SABOTAGE-VISIBLE-42}
FAIL ... · inside a map value · {:a SABOTAGE-VISIBLE-42}
FAIL ... · inside a vector · [SABOTAGE-VISIBLE-42]
FAIL ... · inside a vector · [SABOTAGE-VISIBLE-42]
RESULT= {:test 1, :pass 14, :fail 6, :error 0}
VAR= the-fact-renderer-admits-only-the-exact-numeric-classes-it-prints
RESULT= {:test 1, :pass 22, :fail 0, :error 0}
```

Exactly the claimed 6 failures, on the three positions the brief named, and the old advertised
witness stays 22/22 green on the same sabotaged tree — which independently re-confirms that
round sixteen's finding 3 was real and that the new witness is bound to the allowlist ALONE.

The brief's attacks on the allowlist all fail, correctly:

```text
$ ~/bin/suite-run bash -lc '... clojure.main .../probe4.clj'
  BigInteger subclass  class=user.proxy$java.math.BigInteger$ff19274a  rendered="#object[BigInteger$ff19274a 671c597d]"
  BigDecimal subclass  class=user.proxy$java.math.BigDecimal$ff19274a  rendered="#object[BigDecimal$ff19274a 710cb1e6]"
  Ratio subclass       class=user.proxy$clojure.lang.Ratio$ff19274a    rendered="#object[Ratio$ff19274a 283b28be]"
  Number proxy         class=user.proxy$java.lang.Number$ff19274a      rendered="#object[Number$ff19274a 43cafb0c]"
  Character \a         class=java.lang.Character                       rendered="#object[Character 40903af6]"
  real BigInteger      class=java.math.BigInteger                      rendered="42"
  real Ratio 3/4       class=clojure.lang.Ratio                        rendered="3/4"
  real BigDecimal      class=java.math.BigDecimal                      rendered="1.25M"
```

**Can `(class x)` be spoofed?** No, and the design is stronger than it needs to be on two counts.
`Object.getClass()` is `final native` and cannot be overridden by a proxy, a `deftype` or a
`reify`. And membership is tested against a set of `Class` OBJECTS, not names — so even a
synthesized class whose `getName` returned `"java.lang.Long"` would not be `java.lang.Long` and
would not be in the set. A `deftype` cannot extend `Number` at all (deftype implements
interfaces only), so `proxy` is the strongest available form of that attack and it is bounded.
`Character` correctly stays out, as the docstring says it must to avoid altering a rendering.

### 4. CLOSED — the per-receipt budget is one budget for the whole receipt, and the 2-arity is unchanged

```text
$ ~/bin/suite-run bash -lc '... clojure.main .../probe6.clj'
budget-ms= 2000  ceiling= 160
16 unrenderable facts: wall_ms= 2022  <= one budget + slack?= true
  head= facts · f0=#object[LazySeq 14d780b3] · f1=#object[LazySeq a425126] · ...
16 ORDINARY facts: wall_ms= 25
2-arity unchanged for ordinary values:
   42 -> "42"        "s" -> "\"s\""     {:a [1 2 3]} -> "{:a [1 2 3]}"
   [1 2] -> "[1 2]"  nil -> "nil"       :kw -> ":kw"   3/4 -> "3/4"   1.25M -> "1.25M"
hostile @deadline 0    : ms= 3  out= "#object[LazySeq 2e291b62]"
hostile @deadline -5000 : ms= 3  out= "#object[LazySeq 6340fdbb]"
hostile @deadline 1    : ms= 3  out= "#object[LazySeq 91bd4dd]"
ORDINARY value @deadline 0: ms= 2  out= "{:a 1, :b \"x\"}"
hostile @2-arity (default budget): ms= 2002  out= "#object[LazySeq 7f97ebe9]"
```

- **Total wall ≤ the deadline: yes.** 2,022 ms for 16 hostile facts against the RED's 32,011 ms
  — one budget for the receipt, which is the unit the caller waits on.
- **A deadline of 0 (and a negative one) is safe.** `(max 1 (long budget-ms))` in
  `src/clj_surgeon/mcp_tool.clj:1588` floors it at 1 ms; the call returns in 3 ms with a typed
  identity marker rather than hanging or throwing on `deref`'s illegal-timeout path.
- **The 2-arity is unchanged for every existing caller**, drives above, including the hostile
  case at the default 2,000 ms.

One behavioural note that is a consequence of the design and not a defect: with the budget
shared, a fact late in a receipt whose predecessors were slow gets less time, so an *ordinary*
value can render as an identity marker where it previously rendered as itself. The degradation
is to a typed marker, never to wrong data or a hang, and it is the correct trade for a bounded
receipt — worth one sentence in `refusal-fact-line`'s docstring so a future reader does not
read it as a bug.

### 5. RULING — a server writing `.nrepl-port` into the project under test is a SERVER-LAUNCH defect, not a scorer defect

Verified by execution, both directions, on my own ports.

Default launch, on `:port 8162`:

```text
$ clojure -X:clj-surgeon/mcp :telemetry :full :port 8162
clj-surgeon MCP: embedded nREPL on 43589 ( /var/tmp/forge/q5z17-review-fx/opus/nrepltest/.nrepl-port )
clj-surgeon MCP: persistent server ready on http://127.0.0.1:8162/mcp
after 60s: .nrepl-port    (contents: 43589)
```

Same build with the option the project already ships, on `:port 8163`:

```text
$ clojure -X:clj-surgeon/mcp :telemetry :full :port 8163 :nrepl-port :none
clj-surgeon MCP: persistent server ready on http://127.0.0.1:8163/mcp
with :nrepl-port :none -> NO .nrepl-port written
```

The mechanism is `src/clj_surgeon/mcp_server.clj:344` — `port-file` defaults to
`(io/file project-dir ".nrepl-port")`, and `nrepl-port` defaults to `0` (ephemeral) unless
`:none`. `Makefile:206` (`mcp-serve`) passes neither; `Makefile:209` (`mcp-serve-benchmark`)
passes `:nrepl-port :none`, and the dev targets pass an explicit `:port-file` under
`MCP_DEV_STATE_DIR`. So the project already knows the answer in two of three places.

**The ruling, in three parts:**

1. **It is NOT a scorer defect, and CHECK 1 must not be taught to ignore it.** CHECK 1's entire
   job is "changed files == the manifest's target set exactly, **no extras**." A stray untracked
   file in the tree is precisely what that check exists to catch — an arm that leaves output
   behind is a failed arm. Adding an ignore rule for `.nrepl-port` would blind the check to the
   general class ("a process wrote something into the tree") in order to excuse one instance of
   it, and the arms would then pay for the *next* stray file with a false 6/6. `fan_check.clj`'s
   own header is explicit that a step whose evidence is missing is a FAIL naming the reason,
   never a silent zero; suppressing a true extra is the same sin with the sign flipped.
2. **It IS a server-launch defect, and the fix belongs there.** Any harness that stands a server
   over a tree under measurement passes `:nrepl-port :none` (or an explicit `:port-file` outside
   the tree), exactly as `mcp-serve-benchmark` does. One line, at the launch.
3. **The DEFAULT is a design wart worth its own bead, and `make mcp-serve` inherits it.**
   Defaulting a discovery file INTO the project directory means the ordinary way to start the
   server is also the way to contaminate any gate run over that directory. Defaulting
   `port-file` to the state dir, or defaulting `nrepl-port` to `:none` for non-dev profiles,
   removes the trap rather than documenting it.

**And the remedy the builder actually used is the wrong one.** Deleting the file so CHECK 1
passes makes the 6/6 receipt depend on an undocumented manual step performed between the run and
the score — the receipt then names a state nobody can reproduce from the command line, which is
the `text-block-must-carry-the-structured-receipt` / `source-text-is-not-execution` class. If a
run needed a hand-deletion to score, the score is `:unverified` until the launch is fixed and
the run repeated.

I did not re-run the full FAN acceptance: with two blocking findings the tip is not mergeable
regardless of the fan-out result, and the fixture set present on this box
(`/home/forge/tmp/arms/e3/fanout`) holds only `canonical-21` / `manifest-21.edn` / `repo-21`,
so a fresh 6/6 would have measured a different N than the builder's run-1 claim. **The 6/6
byte-identical claim is therefore `:unverified` by me, neither confirmed nor disputed** — it
should be re-run after §1 and §2 land, from a launch that passes `:nrepl-port :none`, so the
receipt needs no hand-deletion.

### 6. VERIFIED — the RED→GREEN ladder, all four gates, and the enumeration pin

Every RED commit is red at its sha for its stated reason, and green at the following fix:

```text
#### RED-1   aae2078e  the-forwarded-kind-check-is-form-deep-...   RESULT= {:test 1, :pass 2,  :fail 5, :error 0}
#### GREEN-1 be3f80ab  the-forwarded-kind-check-is-form-deep-...   RESULT= {:test 1, :pass 7,  :fail 0, :error 0}
#### RED-2   a011b67b  the-refusal-call-site-scan-reads-forms-...  RESULT= {:test 1, :pass 0,  :fail 4, :error 0}
#### GREEN-2 86894409  the-refusal-call-site-scan-reads-forms-...  RESULT= {:test 1, :pass 4,  :fail 0, :error 0}
#### RED-3   f0eda8a3  the-scalar-allowlist-refuses-a-benign-...   RESULT= {:test 1, :pass 14, :fail 6, :error 0}
#### GREEN-3 e9697010  the-scalar-allowlist-refuses-a-benign-...   RESULT= {:test 1, :pass 20, :fail 0, :error 0}
#### RED-4   9ccb2ec8  the-refusal-fact-line-spends-one-print-...  RESULT= {:test 1, :pass 2,  :fail 1, :error 0}
#### GREEN-4 15fdf59c  the-refusal-fact-line-spends-one-print-...  RESULT= {:test 1, :pass 3,  :fail 0, :error 0}
```

All four gates green at the claimed numbers:

```text
$ ~/bin/suite-run bb test/run_all.clj
Ran 737 tests containing 6275 assertions.   0 failures, 0 errors.    EXIT=0

$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 508 tests containing 7100 assertions.   0 failures, 0 errors.    EXIT=0

$ make mcp-operation-oracle
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
ORACLE_EXIT=0

$ make repository-hygiene
repository hygiene: no machine-local build cache is tracked at any depth
HYGIENE_EXIT=0
```

Round sixteen's finding 2b (the unwitnessed enumeration) is closed properly:
`the-refusal-enumeration-is-pinned-in-count-and-in-membership` asserts `(= 139 (count kinds))`
and the set difference **both** directions against `frozen-refusal-kinds`, a literal 139-member
set (verified by counting its members, not by trusting its docstring). A kind that appears or
vanishes is now a failing test rather than a silent change.

### 7. Out of scope, reported not fixed — `transform_clojure` NPEs on EVERY op instead of refusing typed

Found while making the live call for §5. Untouched by this branch's diff (which changes only
`bounded-pr-str` and `refusal-fact-line` in `mcp_tool.clj`), so it is **not** part of this
verdict — reporting it because a reviewer who finds it and says nothing is the problem.

```text
--- transform_clojure op=alias_migration ---
{"ok":false,"operation":"transform_clojure","error_type":"mcp-adapter-failure",
 "error":"Cannot invoke \"clojure.lang.Named.getName()\" because \"x\" is null",
 "cause":"java.lang.NullPointerException","next_call":null,...}
--- transform_clojure op=totally_bogus_op ---
{"ok":false,"operation":"transform_clojure","error_type":"mcp-adapter-failure",
 "error":"Cannot invoke \"clojure.lang.Named.getName()\" because \"x\" is null", ...}
```

Both ops NPE identically, including a nonsense one. The neighbouring tools are healthy on the
same session — `inspect_clojure` returns `refused · unknown-fields · 3.35 ms`, and the dedicated
`alias_migration` tool returns proper typed refusals on both branches I drove
(`alias-migration-alias-policy-exhausted` with facts and remedy; `invalid-mcp-request` naming
`review_kind` with a stated absent `next_call`). So this is `transform_clojure`'s own op
dispatch reaching `(name nil)` before any validation. Worth a bead: an unrecognised op is the
most ordinary caller error there is, and it should be the tool's cleanest typed refusal, not an
adapter NPE.

---

## NO-GO

**Mergeability.** `git merge-tree --write-tree HEAD origin/MCP/main` against trunk
**`bc7f8a9c96f4b854ae2b299d62e0fa8db0c98b5d`** produced tree `283e9fef` with exit 0 and no
conflict markers, so the tip merges cleanly in the mechanical sense — but it is **not GO on its
own for MCP/main**, because two reproduced defects each let a live refusal kind the entrance
really emits stay out of the enumeration the text-block contract is derived from. (Note: the
builder claims clean at `b1eb6767`; trunk was `99d604bf` when I started and `bc7f8a9c` after my
fetch, so neither of us tested the sha the other did — the report above is what I actually ran.)
Both fixes are small and local — one clause in `mint-evidence` for string literals, and reading
the `refusal` constructor with the reader instead of a regex — and both should ship with the
witness rows that would have caught them.
