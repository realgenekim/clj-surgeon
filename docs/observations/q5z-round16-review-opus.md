## GO-WITH-FIX

Round-sixteen independent review of clj-surgeon `bridge/q5z-alias-migration` at `51da9446`.
Reviewer: Opus (Sol's content filter refused this brief on the hostile-`toString` material;
run with substituted paths per the launch note).

```text
$ cd /home/forge/tmp/sol/q5z14-wt && git rev-parse HEAD && git status --porcelain
51da9446bbc5a28bffc26a86804a3b21b9660874
(no output — clean)
```

Fixtures under `/var/tmp/forge/q5z16-review-fx/opus`, removed at the end. Nothing in the
clone was committed, pushed, stashed, or edited; every sabotage was applied to a
`git archive` export beneath the fixture directory. Surgeon server ran on an explicit
`:port 8129` and was stopped.

**Headline.** All three round-fifteen findings were answered, and two of the three are
genuinely closed: §1's renderer now bounds every attack in the brief, and §3's RESTORE is
correct and bounded. §2 is *narrowed but not closed* — the marker is now checked, and the
check is still defeatable by the brief's own named attack, which I reproduced end to end
with all four advertised witnesses green. Two further defects are named below: the §4
line-regex (latent, proven with planted counterexamples both ways) and the fact that the
§1 fix **has no witness of its own** — reverting the allowlist alone leaves the witness
22/22 green while a hostile `toString` demonstrably reaches the output again.

---

### 1. GO-WITH-FIX — the `forwarded-refusal-kind` marker is checked, but the check reads only LIST HEADS, so a threading form mints a kind under an exempt marker

`test/clj_surgeon/mcp_alias_migration_test.clj:1259-1314` (`forwarded-kind-expression?`).
The predicate collects `lists` and tests only each list's *first* child against
`kind-forwarding-heads`. In a threading form the minting function is a **bare symbol in an
argument position**, never a list head — so `keyword`, `symbol` and `str` walk straight
through the allowlist that exists to stop them. The brief's own named attack,
`(some-> kind name keyword)`, is exempt. A `get`/`get-in` against a **literal table** is
exempt for the same reason: `get` selects, but the value it selects is a fresh keyword
literal that no `(refusal :kw` scan can see.

Driven directly against the tip's own scanner:

```text
$ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z16-review-fx/opus/marker_probe.clj'
=== marked sites: does the shape check catch a MINT? ===
A threading-mint             expr=(some-> kind name keyword)         reported=NO  (EXEMPT)
B literal-table-get          expr=(get {:a :brand-new-kind} kind)    reported=NO  (EXEMPT)
C get-in-table               expr=(get-in {:a {:b :table-minted}} [kind :b]) reported=NO  (EXEMPT)
D nth-table                  expr=(nth [:k1 :k2] i)                  reported=YES ["plant:6"]
E r15-keyword-mint           expr=(keyword (:review_dynamic_kind params)) reported=YES ["plant:6"]
F str-mint                   expr=(str "heldout-" kind)              reported=YES ["plant:6"]
G legit-forward              expr=(some :error-type (remove :ok checks)) reported=NO  (EXEMPT)
H bare-symbol                expr=kind                               reported=NO  (EXEMPT)

=== forwarded-kind-expression? on a let-bound literal source ===
C2 let-bound-literal reported=NO  (EXEMPT)
```

D, E and F are the round-fifteen attack and its obvious neighbours, and they are now
correctly named — that part of the fix works. A, B, C and C2 are not.

End to end on a planted, reachable tree (`git archive 51da9446` export; a marked
`(refusal (some-> (:review_kind params) name keyword) …)` branch added at the head of
`validate-request`'s `cond` in `src/clj_surgeon/mcp_alias_migration.clj:115`, routed from
`handle-alias-migration`):

```text
$ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /var/tmp/forge/q5z16-review-fx/opus/plantA_probe.clj'
enumerated= 139
unscannable-sites= []
dynamic-sites= []
live-kind= "planted-runtime-kind"
live-kind-enumerated= false
```

and all four advertised witnesses stay green on that same planted tree — 21 assertions,
zero failures:

```text
$ ~/bin/suite-run bash -lc 'cd .../plantA && … clojure.main /var/tmp/forge/q5z16-review-fx/opus/witness.clj'
Ran 1 tests containing 1 assertions.   0 failures, 0 errors.   (no-reachable-namespace-spells-a-refusal-kind-dynamically)
Ran 1 tests containing 3 assertions.   0 failures, 0 errors.   (the-source-guard-exempts-only-the-forwarded-refusal-kind-marker)
Ran 1 tests containing 8 assertions.   0 failures, 0 errors.   (the-refusal-enumeration-contains-every-kind-the-entrance-constructs)
Ran 1 tests containing 9 assertions.   0 failures, 0 errors.   (the-forwarded-refusal-kind-marker-is-checked-and-not-merely-believed)
```

This is a future-regression hole and **not** a missing kind in the unmodified tip: on the
clean tree the enumeration is 139, `unscannable-sites` is `[]` and `dynamic-sites` is `[]`.
Fix: in a threading head (`->`, `->>`, `some->`, `some->>`) every subsequent bare symbol is
a call head and must satisfy the allowlist; and feed keyword literals appearing inside a
marked forwarding expression to `minted-kinds-in`, so a literal table cannot mint invisibly.

### 2. GO-WITH-FIX — the builder's reported open defect is real in both directions, and latent only because the two namespaces holding the shape are out of the reachable set

`test/clj_surgeon/mcp_alias_migration_test.clj:1384-1430`. `dynamic-refusal-kind-sites-in`
decides by `#"\(refusal\s"` **per line**. Planted counterexamples, both ways:

```text
$ ~/bin/suite-run bash -lc '… clojure.main /var/tmp/forge/q5z16-review-fx/opus/regex_probe.clj'
=== §4 line-regex counterexamples ===
1 same-line mint (control)         reported=YES ["plant:5"]
2 mint w/ arg on NEXT line         reported=NO  (INVISIBLE)
3 'refusal' inside a STRING        reported=YES ["plant:5"]
4 'refusal' inside a COMMENT       reported=YES ["plant:5"]
```

So the guard both **misses** a minting site whose kind starts on the next line and **fires
falsely** on the word inside a string or a comment. The same newline defeats the
enumeration's own `#"\(refusal :([a-z][a-z0-9-]*)"` (`:1508-1510`), which is anchored on a
single space — a literal kind written on the next line would be missed by the enumeration
*and* by the guard that exists to catch what the enumeration cannot see.

I ruled whether this is live. Seven such sites exist in `src/` today
(`mcp_source_anchor.clj` ×6, `mcp_extraction.clj:272`), all with literal kinds — but
neither namespace is in the subject:

```text
$ ~/bin/suite-run bash -lc '… clojure.main /var/tmp/forge/q5z16-review-fx/opus/reach.clj'
reachable-namespace-count= 20
mcp-source-anchor reachable= false
mcp-extraction    reachable= false
enumerated-count= 139
  enumerated? extraction-decisions-required          false
  enumerated? semantic-candidate-range-invalid       false
  enumerated? invalid-change-subject                 true
  enumerated? ok                                     false
```

**Ruling: GO-WITH-FIX, not BLOCKING.** No reachable site has the shape today, so nothing is
missing from the tip's enumeration. It is not "acceptable-declared" either, because the fix
is cheap and the machinery already exists: the same namespace already reads sites with the
reader (`error-type-value-sites`), so scanning `(refusal <non-literal>` as a *form* rather
than a *line* closes the miss and the false positive together. Deciding a call site by text
in a file you are already parsing is the defect class this branch has now paid for twice.

`ok` is absent from the enumeration, so the spurious 140th kind the builder reported is gone
at the tip. But the *class* is still unwitnessed: nothing asserts the enumeration's count or
that no spurious kind appears (`grep -n "= 139\|contains? kinds \"ok\"" test/…` returns
nothing), so a head allowlist that mis-classifies a forward would again change the
enumeration silently. Pin the count, or assert the enumeration's set difference against a
frozen list.

### 3. GO-WITH-FIX — the §1 fix has NO witness of its own: reverting the allowlist alone leaves the advertised witness 22/22 green

The brief asked me to rule whether the allowlist's own witness is adequate. It is not, and
the measured result is worse than the reported `sab1 2/4 red`. I sabotaged **only**
`print-safe-leaf?` on a `git archive` export, restoring the pre-fix `instance?`-based
predicate and leaving `write-safe-leaf`'s guard and the time budget untouched:

```text
$ ~/bin/suite-run bash -lc 'cd .../sab-allowlist && … clojure.main .../run_var.clj the-fact-renderer-admits-only-the-exact-numeric-classes-it-prints'
VAR= the-fact-renderer-admits-only-the-exact-numeric-classes-it-prints
Ran 1 tests containing 22 assertions.
0 failures, 0 errors.
{:test 1, :pass 22, :fail 0, :error 0, :type :summary}
```

The sabotage is causally bound to a behaviour change — the same probe on both trees, a
benign `Number` proxy whose `toString` returns a marker string:

```text
$ … clojure.main /var/tmp/forge/q5z16-review-fx/opus/sab_binding.clj      # TIP
tree= /home/forge/tmp/sol/q5z14-wt
number?= true
rendered= "#object[Number$ff19274a 2b3ffbbf]"

$ … clojure.main /var/tmp/forge/q5z16-review-fx/opus/sab_binding.clj      # SABOTAGE
tree= /var/tmp/forge/q5z16-review-fx/opus/sab-allowlist
number?= true
rendered= "SABOTAGE-VISIBLE-42"
```

So a caller-controlled `toString` provably reaches the receipt again, and **every assertion
still passes**. The witness asserts the *outcome* (bounded, identity marker rendered), which
three independent layers each guarantee; it never asserts the allowlist's own behaviour. The
allowlist is real defence in depth and I would keep it — but as shipped it is an unratcheted
fix, exactly the `marker-presence-audit-is-not-a-ratchet` class. Add one assertion that a
`Number` subclass with a *benign, observable* `toString` renders as an identity marker; it
goes red the moment the allowlist is weakened, and it needs no hostile object at all.

### 4. §1's renderer attacks all close, and the daemon thread is genuinely abandoned

Every attack the brief named is bounded, and the caller sees a typed identity marker rather
than a hang:

```text
$ ~/bin/suite-run bash -lc '… clojure.main /var/tmp/forge/q5z16-review-fx/opus/render_probe.clj'
=== B. hostile objects ===                                  [ms, value]
looping-IPersistentMap-seq  [2002 "#object[LoopingMap 3c4570f4]"]
looping-hashCode            [1 "#object[LoopingHash 79419de0]"]
looping-toString            [0 "#object[LoopingStr 35de5cc2]"]
throwing-toString           [0 "#object[ThrowingStr 1ce7b36c]"]
looping-value-INSIDE-a-map  [0 "{:a #object[LoopingStr 19d2bca2]}"]

=== C. budget composition ===
one-looping-lazy-seq        [2000 "#object[LazySeq 2b2bc625]"]
map-of-100-looping-values   [2001 "#object[PersistentHashMap 482d8add]"]
refusal-fact-line 16 hostile facts [32008 "facts · f0=#object[LazySeq 57b414f0] · f1=…"]

=== D. ordinary JVM values ===
java-array           [0 "#object[int[] 1a260a38]"]
java.util.Date       [0 "#object[Date 40fda885]"]
clojure.lang.Var     [0 "#object[Var 10bea4]"]
Character            [0 "#object[Character 2e291b62]"]
java.util.HashMap    [0 "#object[HashMap 32e09ce]"]

=== E. thread leak: 50 timed-out renders ===
named-print-threads before=0
named-print-threads after 50 timed-out renders=0 (all daemon=true)
```

Four things worth naming. (a) The `hashCode` attack the brief flagged **cannot fire**:
`opaque-object-marker` uses `System/identityHashCode` (`src/clj_surgeon/mcp_tool.clj:1372`),
which never calls the value's own `hashCode` — measured at 1 ms. (b) On the subclassing
question: `Keyword` and `Symbol` are *not* final, but every declared constructor is
`private`, so no subclass can be written; `String`, `Boolean` and `Long` are final. The
exact-class rule is therefore belt-and-braces for those two and load-bearing for `Number`.

```text
clojure.lang.Keyword       final=false  ctors=["private"]
clojure.lang.Symbol        final=false  ctors=["private" "private"]
java.lang.String           final=true   ctors=[…]
java.lang.Boolean          final=true   ctors=["public" "public"]
java.lang.Number           final=false  ctors=["public"]
java.lang.Long             final=true   ctors=["public" "public"]
```

(c) The budget is **per `bounded-pr-str` call**, not per leaf: a map of 100 looping values
costs 2001 ms, not 100 × 2 s. But `refusal-fact-line` (`src/clj_surgeon/mcp_tool.clj:1641-1647`)
calls it once per fact and `max-refusal-facts` is 16 (`:1260`), so a refusal carrying 16
hostile values costs a measured **32,008 ms**. That is bounded, not unbounded, and it is not
reachable from caller data — every entrance normalises through
`(json/parse-string (json/generate-string params) true)` (`:1769`), so a request can only
carry JSON scalars and collections, never a JVM object with a hostile `toString`. I record it
as a composition fact, not a finding; if a refusal ever carries a value from outside that
round-trip, the budget should be a deadline shared across the fact line rather than per fact.
(d) Zero named render threads survive 50 timed-out renders and all are daemons — the thread
is genuinely abandoned and cannot hold an MCP server's JVM open.

### 5. §3 RESTORE is correct, bounded, and the ordinary corpus is unaffected

```text
$ ~/bin/suite-run bash -lc '… clojure.main /var/tmp/forge/q5z16-review-fx/opus/record_probe.clj'
record-plain                 [3 "#user.Rec{:a 1, :b 2}"]
record-with-LOOPING-field    [2 "#user.Rec{:a #object[LoopStr 36013bda], :b 2}"]
record-10000-extra-keys      [14 "#user.Rec{:a 1, :b 2, :k2868 2868, …len=161"]
meta-with-LOOPING-value      [0 "^{:m #object[LoopStr 59ebd62f]} {:a 1}"]
meta-boolean-private         [0 "^{:private true} {:a 1}"]
record-with-meta             [0 "^Foo #user.Rec{:a 1, :b 2}"]
self-meta-cycle              [0 "^^^{:d 4} {:c 3} {:b 2} {:a 1}"]
```

The record tag is back and read with `.getName`; a looping field, looping metadata and a
10,000-key record are all bounded (161 characters, 14 ms); the lone-`:tag` form renders
`^Foo`; nested metadata chains terminate. Ordinary receipts render as callers already saw
them, including nested collections and a 500-character string elided at the 160-character
per-fact bound:

```text
FACTS: facts · found_files=0 · scanned_files=0
NEXT : next_call · {"op":"plan","workspace_root":"/x"}
FACTS: facts · big="zzz…zzz… · nested={:a [1 2 {:b "c"}]}
```

### 6. The three RED commits are red at their shas for the stated reasons

Each RED witness run at its own sha (`git archive` export, single test var):

```text
########## RED bc77e5f2 :: the-fact-renderer-admits-only-the-exact-numeric-classes-it-prints
  bounded-pr-str propagated a Number's own toString exception: <threw ExceptionInfo>
  a Number the renderer cannot print safely rendered no identity marker at all
  bounded-pr-str hung inside a Number's toString that never returns
  bounded-pr-str hung realising a lazy sequence whose body never returns
  Ran 1 tests containing 21 assertions.  4 failures, 0 errors.

########## RED 03507799 :: the-forwarded-refusal-kind-marker-is-checked-and-not-merely-believed
  a site that MINTS a kind was exempted by an unchecked forwarded-refusal-kind marker: []
  · (keyword (name kind)) mints a new kind from a parameter
  · (str "heldout-" (name kind)) composes a kind from a literal
  · a marked MINTING (refusal …) call site is still named
  Ran 1 tests containing 9 assertions.   4 failures, 0 errors.

########## RED 4d3682df :: the-fact-renderer-keeps-every-rendering-callers-already-saw
  metadata on clojure.lang.PersistentVector renders differently from pr-str
    actual: (not (= "^{:receipt true} [1 2]" "[1 2]"))
  … PersistentList, PersistentHashSet, and the lone-tag form
  Ran 1 tests containing 10 assertions.  6 failures, 0 errors.
```

All three are green at the tip, inside the full suites below.

### 7. Gates — every claimed figure reproduced exactly

```text
$ ~/bin/suite-run bb test/run_all.clj
Ran 737 tests containing 6275 assertions.
0 failures, 0 errors.
EXIT=0                                          (claim: 737/6275/0 — exact)

$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 503 tests containing 7063 assertions.
0 failures, 0 errors.
EXIT=0                                          (claim: 503/7063/0 — exact)

$ make mcp-operation-oracle
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
EXIT=0

$ make repository-hygiene
repository hygiene: no machine-local build cache is tracked at any depth
EXIT=0
```

### 8. FAN gate — 6/6 on the fetched scorer, byte-identical after exactly one live call

Scorer taken from `origin/MCP/main` at `4b0e0294c0440d5432e1cc350f2a20c31ea38c3b`
(`git archive origin/MCP/main bench/fanout`). Server started from the branch tip on explicit
`:port 8129`, bound to a fresh copy of `repo-21`; exactly one `tools/call`:

```text
{"content":[{"type":"text","text":"alias_migration\n  21 files · 63 sites · aliases {\"es\" 5, \"st2\" 5, \"store-2\" 5, \"store2\" 6} · 30 collisions resolved · 740.23 ms\n\n✓ atomic commit complete\n✓ written bytes read back and verified…"}],"isError":false,…"files":21,"sites":63,"collisions_resolved":30,"ok":true}
```

Server stopped, then:

```text
$ FAN_BASE=4d8142e3… ~/bin/suite-run .../rescore-FAN.sh .../fan/run1 21 .../fan/generated
rescore-FAN: worktree=… n=21 base=4d8142e320ee13a7dfe442293c659a411eec5446 base-from=FAN_BASE
fan_check: git=/usr/bin/git … resolution=absolute-candidate
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
fan_check: 4/4 structural checks passed
CHECK 4 load: PASS namespaces=100 rc=0
CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
rescore-FAN: 6/6 checks passed
SCORE_EXIT=0
BYTE_DIFF_EXIT=0
```

The identical file/site/collision/alias figures to round fifteen, on a scorer fetched from
`MCP/main` rather than from the branch, against an independently generated base.

### 9. Ports and fixtures

```text
$ cd /home/forge/tmp/sol/q5z14-wt && git rev-parse HEAD && git status --porcelain
51da9446bbc5a28bffc26a86804a3b21b9660874
(no output — clone unchanged by this review)

$ rm -rf /var/tmp/forge/q5z16-review-fx && ls -d /var/tmp/forge/q5z16-review-fx
ls: cannot access '/var/tmp/forge/q5z16-review-fx': No such file or directory

$ ss -ltn | grep -E ':(8129|8130|8131)\b'
(no output)
no listener on 8129-8131

$ ps -eo pid,args | grep "[c]lj-surgeon/mcp :project-dir" | grep -c q5z16
0
```

Fixtures removed and the removal proved; no listener remains and no server from this review
survives. Ports 7888/7890/7894/7895/7906–7910/7941–8128/8132–8143 were never contacted.

---

## GO-WITH-FIX

**Mergeability:** `git merge-tree --write-tree HEAD origin/MCP/main` exits 0 and prints the
single tree `a464474e4bdb7d0c1568e11cff8c82a4254285c8` with no conflict block against
`origin/MCP/main` at `4b0e0294c0440d5432e1cc350f2a20c31ea38c3b`, so this tip merges cleanly
on its own — but it should not land until finding 1 (the threading-form mint that defeats the
now-checked marker) and finding 3 (the allowlist fix having no witness that would go red if it
were removed) are closed, with finding 2's form-level scan folded into the same round.
