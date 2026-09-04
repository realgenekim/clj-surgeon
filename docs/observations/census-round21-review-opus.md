## NO-GO

# Round-twenty-one review — clj-surgeon `bridge/census-verb` at 0a91e720

Independent reviewer: Opus, taking this brief after Sol's content filter refused it — the
fifth consecutive census refusal. Same brief, substituted paths. Reviewed read-only in
`/home/forge/tmp/sol/census21-wt`. No commit, push, stash, index write, or source edit was
made anywhere in the clone. Fixtures were confined to
`/var/tmp/forge/census21-review-fx/opus` and removed at the end. No listed port was
contacted. Sabotage was applied only to `git archive` exports under my fixture directory,
and every export was verified byte-identical to its git object afterwards. The only remote
operation was one `git fetch origin` in the clone, required for the `origin/MCP/main`
dry-run; it updates remote-tracking refs only and left the worktree clean.

**Procedural note, since round nineteen recorded the opposite.** The fixture directory this
brief assigned me did not exist when I arrived:

```text
$ ls /var/tmp/forge/census21-review-fx
ls: cannot access '/var/tmp/forge/census21-review-fx': No such file or directory
```

I created it fresh. Every receipt below is from my own drives.

Entry proof:

```text
$ git rev-parse HEAD && git status --porcelain
0a91e72034e2359c552b155612ad2987ac43bc78

$ git log --oneline -3
0a91e720 merge-fix(2): actually untrack .cpcache — `git commit -- <paths>` ignored the index
b7f1df1a docs(census): CENSUS-014/018 state round twenty's two rules
8f9ee881 green: the tool's empty-tree remedy names the root by its ONE name
```

The blank line after the hash is the verbatim empty `git status --porcelain`.

**One BLOCKING finding.** Round twenty closed both of round nineteen's blocking findings —
I reproduced both closures and they are real. The blocking finding below is the round-19
item-2 *class* — a reader that evaluates caller-influenced text — surviving one frame over,
in the entrance the round-twenty enumeration did not walk. This is the third consecutive
round in which the repair is correct and the enumeration around it is a subset.

---

## 1. BLOCKING — a reader-eval IS reachable from an argument: `clojure.core/read-string` on a build file under caller-supplied `:dir`

`src/clj_surgeon/core.clj:285-290` (`extract-source-paths`), reached from
`core.clj:317` (`discover-projects`) and `core.clj:624-638` (`discover-projects-grep`),
reached from `core.clj:1736-1738` inside `run-ls-tree` (`core.clj:1715`), whose `:dir` is
an ordinary CLI argument.

Item 2 of this brief declares the reader-eval closed, and directs the attack at "every
other read-string / load-string / eval reachable from argv or config in src/". The
`parse-val` repair is genuine — I verify it in §5 below. But the sweep the brief prescribes
finds a second one, and it is not a latent path: it is the `:ls-tree` op's ordinary
invocation.

```clojure
;; src/clj_surgeon/core.clj:285-290
(defn- extract-source-paths
  "I/O wrapper: read a build file and return its source paths."
  [build-file]
  (try
    (source-paths-from-config (str (fs/file-name build-file))
                              (read-string (slurp (str build-file))))
    (catch Exception _e ["src"])))
```

`read-string`, not `edn/read-string` — bare `clojure.core/read-string`, which honours
`*read-eval*`, exactly the class `parse-val`'s new docstring (core.clj:2544-2545) names as
"a class rather than a nicety". The file it reads is a `deps.edn` / `project.clj` /
`bb.edn` **discovered under the directory the caller named**, so the caller does not even
need to control argv text — controlling a directory is enough.

The `(catch Exception _e ["src"])` does not help: the evaluation happens *inside* the
reader, before any value is returned, so the catch swallows the evidence rather than the
effect. This is the same tell round twenty wrote into `parse-val`'s docstring — "the
evaluation happened while the op was REFUSING the argument, ... the reader ran before any
validation could" — except here the op does not even refuse; it succeeds.

### Reproduced at BOTH real launchers, as subprocesses

Fixture — a directory containing nothing but a hostile build file and one source:

```text
$ cat /var/tmp/forge/census21-review-fx/opus/evil-tree/deps.edn
{:paths #=(clojure.core/spit "/var/tmp/forge/census21-review-fx/opus/PWNED-LSTREE.txt" "READER EVAL EXECUTED via :op :ls-tree :dir")}
```

JVM launcher:

```text
$ ls $FX/PWNED-LSTREE.txt
ls: cannot access '/var/tmp/forge/census21-review-fx/opus/PWNED-LSTREE.txt': No such file or directory

$ java -cp "$CP" clojure.main -m clj-surgeon.core :op :ls-tree :dir "$FX/evil-tree"
EXIT=0
src/a.clj  1 lines, 0 forms
  ns: a

── total: 1 files, 0 forms

$ ls -la $FX/PWNED-LSTREE.txt && cat $FX/PWNED-LSTREE.txt
-rw-r--r-- 1 forge forge 42 Sep  4 07:24 /var/tmp/forge/census21-review-fx/opus/PWNED-LSTREE.txt
READER EVAL EXECUTED via :op :ls-tree :dir
```

bb launcher, same fixture, payload text changed so the receipt cannot be the previous one:

```text
$ ls $FX/PWNED-LSTREE.txt
ls: cannot access '...PWNED-LSTREE.txt': No such file or directory

$ bb -cp "$PWD/src" -m clj-surgeon.core :op :ls-tree :dir "$FX/evil-tree"
EXIT=0
src/a.clj  1 lines, 0 forms
  ns: a

── total: 1 files, 0 forms

$ cat $FX/PWNED-LSTREE.txt
READER EVAL EXECUTED at the bb launcher
```

**Exit 0. A green receipt. Nothing printed about the evaluation.** The `:relation-census`
`#=` reproduction that round twenty fixed at least printed a refusal while it executed;
this one reports success. That is strictly worse as a signal, and it is the same defect.

### Why the round-twenty enumeration is green over it

The item-2 repair is scoped to `parse-val` — one function, on the argv-*text* path. The
brief's own sweep command is `rg -n 'read-string|load-string|\beval\b|\*read-eval\*' src`,
and `core.clj:290` is the second hit that command returns. The sweep was prescribed and the
repair was not extended to what it found. Every other `read-string` in `src/` is
`edn/read-string`, with the exception of six in `txn_journal.clj` (`:477`, `:511`, `:652`,
`:881`, `:2578`, `:2976`) which read the tool's own journal and lock files — those are not
caller-named and I do not rule them blocking, but they are the same primitive and should
move to `edn/read-string` in the same pass, because "the caller cannot name this file
today" is a statement about today's call graph.

### The fix

`edn/read-string` at `core.clj:290`, and the same witness shape round twenty already built
for `parse-val`: plant `#=` in a `deps.edn`, drive `:op :ls-tree :dir` through both real
launchers, assert the side-effect file was **not** created. `source-paths-from-config` is
already pure and already has unit tests (`ls_tree_test.clj:75-100`); only the I/O wrapper
changes, and `project.clj` still reads as data under EDN for the `:source-paths` lookup it
performs. If a `project.clj` genuinely needs code reading, that is an argument for refusing
it, not for `*read-eval*`.

---

## 2. NOT BLOCKING, but the evidence is wrong — the leaf bound is not load-bearing, and the claimed sabotage receipt does not reproduce

`src/clj_surgeon/relation_census.clj:1207-1233` (`bound-refusal-leaf`, commit 8c93b878),
`:1251-1266` (`bound-refusal`'s whole-field bound, commit 2f500e89).

Item 6 claims the sabotage receipt `bound → string?-only 118/28`. I reproduced that exact
sabotage on a `git archive` export of 0a91e720 — reverting `bound-refusal-leaf` to round
nineteen's `(if (string? x) (bound-refusal-text x) x)` — and it is **not detected at all**:

```text
=== census battery under SABOTAGE 1 (leaf bound = string?-only) ===
Ran 97 tests containing 2775 assertions.
0 failures, 0 errors.
CENSUS-BATTERY 97 2775 0 0
```

and the real launchers publish byte-identical output sabotaged and unsabotaged:

```text
                    UNSABOTAGED                        SABOTAGE 1
dup-keyword   BYTES=1276 MAX_A_RUN=1021 MARKERS=1   BYTES=1276 MAX_A_RUN=1022 MARKERS=1
kw-vector     BYTES=2692 MAX_A_RUN=1021 MARKERS=2   BYTES=2692 MAX_A_RUN=1022 MARKERS=2
```

The reason is visible in `bound-refusal`: there are two bounds, and the **whole-field**
bound alone closes blocking-1, because any field holding an over-long keyword also *prints*
over the ceiling. The leaf bound is redundant for every input I could reach. The identity
keys are exempt from the whole-field bound — that is the one place the leaf bound could
matter — but `:dir`, `:anchor/:given` and `:anchor/:absolute` are built with `(str …)` and
are therefore always strings, which round nineteen's bound already caught.

This does not reopen blocking-1. **The bound is genuinely closed** — I verified that
independently in §5. It means the receipt offered as proof of *which* commit closes it is
wrong, and 8c93b878's witness does not pin 8c93b878's code. Two consequences worth acting on:

- Either state that the leaf bound is defence-in-depth with no reachable witness, or find
  the reachable input that needs it and drive it. A bound nothing can make red is not a
  ratchet.
- Re-derive the `118/28` figure or withdraw it. My census battery is 97 tests / 2775
  assertions; the brief's is `23/1281`, so the scales differ — but a sabotage that produces
  57 failures under one definition (§4 below) and 0 under the same definition here is a
  difference in kind, not in scale.

**Separately unreproducible:** the brief's "census battery twice (23/1281/0, byte-identical
sha 8d1bcb68…)". There is no `census-battery` target in the `Makefile` and no definition of
it in `docs/` or `logs/` (`grep -rn "census battery\|8d1bcb68" --include=*.md .` returns
nothing). I defined my own over the five census namespaces and report its numbers
throughout; the claimed figures are not checkable as stated. A battery quoted in a receipt
should be a target someone else can run.

---

## 3. NOT BLOCKING — a parity divergence inside item 3's declared attack list: `:dir` that is a FILE

`src/clj_surgeon/core.clj:728-783` (`census-workspace`) vs
`src/clj_surgeon/mcp_relation_census.clj:1390-1399`.

Item 3 names "`:dir` that is a FILE" as an attack to run. Run at both entrances, they
disagree:

```text
SHAPE                 TOOL_ERROR_TYPE            CLI_ERROR_TYPE            CLI_OK  AGREE
dir-is-a-file         "invalid-workspace-root"   "no-fold-arms-found"       false   false
dotdot                "no-fold-arms-found"       "no-fold-arms-found"       false   true
dot                   "no-fold-arms-found"       "no-fold-arms-found"       false   true
double-slash          "no-fold-arms-found"       "no-fold-arms-found"       false   true
trailing-slash        "no-fold-arms-found"       "no-fold-arms-found"       false   true
plain                 "no-fold-arms-found"       "no-fold-arms-found"       false   true
```

The three forms reported-not-fixed in item 4 (`..`, `.`, `//`) all **agree** — that concern
is empirically closed even though it was never enumerated. The one that diverges is the one
item 3 asked for.

The CLI's answer is not merely a different name, it is a false description. Verbatim:

```text
$ java -cp "$CP" clojure.main -m clj-surgeon.core :op :relation-census :dir "$FX/ws/deps.edn"
{:ok false,
 :error-type :no-fold-arms-found,
 :error "No file defines defmethod fold-event arms",
 :dir ".../ws/deps.edn",
 :remedy "Nothing under <workspace_root> defines defmethod fold-event arms (0 file(s) scanned), so no narrower command can be computed: point :dir at a directory whose sources define fold arms, or name one with :file.",
 :files-scanned 0}
```

The caller named a file; the receipt says the *tree* defines no fold arms and reports
`:files-scanned 0`, which is the shape of a completeness claim over a tree that was never a
tree. `census-workspace` reaches `.toRealPath`, which succeeds on a regular file, so the
"is there a tree here at all" question is never asked at the CLI.

**The CLI is also inconsistent with itself.** The same launcher, same argument, different
op:

```text
$ java -cp "$CP" clojure.main -m clj-surgeon.core :op :ls-tree :dir "$FX/ws/deps.edn"
{:error ":ls-tree :dir must be an existing directory: \".../ws/deps.edn\"",
 :error-type :workspace-root-not-a-directory, ...}
```

`ls-tree-root-refusal` (core.clj:1702) uses `existing-directory?`; the census does not. The
fix is one predicate in `census-workspace` — a workspace root must be an existing
*directory*, not merely an existing path — publishing `:invalid-workspace-root`, the name
the other entrance already uses. And the round-20 parity table (two rows, both with an
*unresolvable* root) should gain this row, because it is precisely the shape the table's
fixed drive shape cannot see.

---

## 4. NOT BLOCKING — a `StackOverflowError` escapes both real launchers untyped

Item 2 asks for "a 10,001-deep nested vector (reader stack)". Both launchers crash out of
their declared refusal set:

```text
$ java -cp "$CP" clojure.main -m clj-surgeon.core :op :relation-census :dir "$FX/cwt" :doors "$(python3 -c "print('['*10001 + ']'*10001)")"
EXIT=1  BYTES=224
Execution error (StackOverflowError) at java.io.PushbackReader/read (PushbackReader.java:87).
null

$ bb -cp "$PWD/src" -m clj-surgeon.core ... same argument
EXIT=1  BYTES=1402
Type:     java.lang.StackOverflowError
Location: /home/forge/tmp/sol/census21-wt/src/clj_surgeon/core.clj:2626:5
```

I rule this non-blocking on the brief's three criteria: no caller value is published
unbounded (224 and 1,402 bytes), no read leaves the workspace, and nothing is evaluated.
But it is a genuine gap in the claim that "every refusal the launcher prints leaves through
one bounded exit" — `-main` catches `Exception`, and a `StackOverflowError` is an `Error`.
A caller-controlled argument reaching an untyped stack trace is not a refusal the
enumeration can drive, which is the same argument round nineteen made about undeclared
names. Cheapest ratchet: catch `Throwable` at `-main`'s boundary and publish
`:invalid-arguments` through `bound-refusal`, plus a witness driving this exact argument.

---

## 5. What I reproduced as genuinely closed

Recorded because a NO-GO on one finding should not obscure that round twenty did the work
it claimed on the other four.

**Blocking 1 (the bound) — closed.** Round nineteen's own receipts, re-driven at this tip
through the real JVM launcher, with the same 10,001-character argument:

```text
                r19 (defect)                        0a91e720 (this tip)
dup-keyword   BYTES=20287 MAX_A_RUN=10001 MARKERS=0   BYTES=1276 MAX_A_RUN=1021 MARKERS=1
dup-symbol    BYTES=20228 MAX_A_RUN=10001 MARKERS=0   BYTES=1276 MAX_A_RUN=1022 MARKERS=1
kw-vector     BYTES=11683 MAX_A_RUN=10001 MARKERS=1   BYTES=2692 MAX_A_RUN=1021 MARKERS=2
sym-vector    BYTES=11682 MAX_A_RUN=10001 MARKERS=1   BYTES=2692 MAX_A_RUN=1022 MARKERS=2
map-value     —                                       BYTES=2692 MAX_A_RUN=1018 MARKERS=2
```

New attacks, both bounded:

```text
many-leaves (10,000 tiny keywords)   BYTES=2692 MAX_A_RUN=1 MARKERS=2 :doors-not-a-string
nested-kw   (10,001-char keyword
             in a map in a vector)   BYTES=2692 MAX_A_RUN=1 MARKERS=2 :doors-not-a-string
```

The **identity-key exemption cannot be abused**: driving a 10,001-character run into
`:dir` and into `:file` leaves `:anchor` bounded, because the exemption is from the
whole-field bound only and the leaf bound still applies:

```text
anchor-dir  BYTES=2577 MAX_A_RUN=1011 MARKERS=2 :invalid-workspace-root
anchor-file BYTES=5444 MAX_A_RUN=1011 MARKERS=5 :file-not-found
```

**The `parse-val` repair is real, at both launchers**, and the side-effect file is not
created:

```text
$ java … :op :relation-census :dir "$FX/cwt" :doors '[#=(clojure.core/spit "…/PWNED-PARSEVAL.txt" "x")]'
{:error "No dispatch macro for: =", :error-type :invalid-arguments}
$ bb   … same
{:error "No dispatch macro for: =", :error-type :invalid-arguments}
$ ls $FX/PWNED-PARSEVAL.txt
ls: cannot access '…/PWNED-PARSEVAL.txt': No such file or directory
```

Tagged literals behave correctly: `#inst` and `#uuid` read as inert data and are refused
`:doors-not-a-string` with the value bounded; `#foo/bar` and `#clj-surgeon/x` are refused
`{:error "No reader function for tag …", :error-type :invalid-arguments}`. No custom data
readers are registered anywhere in `src/` (`rg 'data-readers'` → no hits). The SCI path
(`forms.clj:143`, `edit_dsl.clj:418`) evaluates `.clj-surgeon.edn` field extractors inside
an allowlist with no `:classes` entry, so interop is unavailable by construction; my
attempted `(.exec (Runtime/getRuntime) …)` escape produced no effect. I do not rule it a
finding, but it is a config-driven eval surface and belongs in the declared enumeration
rather than being absent from it.

**Blocking 2 (the fence) — closed.** The exact round-19 blocker — an unresolvable `:dir`
plus a `:file` whose link leaves the tree, which previously READ the target and published
`{:ok true, :files-scanned 1, :read-complete true}` — now refuses before any read, with the
MCP name, and the prose uses the token rather than an absolute root:

```text
$ java … :op :relation-census :dir "$FX/NO-SUCH-DIR" :file "$FX/ws/src/link-secret.clj"
{:ok false,
 :anchor {:kind :file, :given ".../ws/src/link-secret.clj", :absolute ".../ws/src/link-secret.clj"},
 :error-type :invalid-workspace-root,
 :error "<workspace_root> is not an existing directory, so there is no tree for this census to be over and no fence a source could be inside of",
 :remedy "<workspace_root> is not an existing directory, so nothing about it can be narrowed and no source can be inside it: name a directory that exists with :dir, or name one source to census with :file."}
```

`:file`-only with an escaping link → `:file-outside-workspace`. A `:dir` symlink loop →
typed refusal. A `:dir` symlink to outside → the workspace *becomes* the resolved target,
which is the documented "the tree the request named" rule, and the fence then measures
against it; not an escape. `escaping-source` (core.clj:794-830) makes the nil workspace
unrepresentable by throwing, and narrows the catch to `IOException → ::unresolvable`, as
claimed. Containment is tested on the **real** path after resolution
(`.toRealPath` then `.startsWith` on `Path`, not on strings), so the TOCTOU shape the brief
asks about is answered on one resolved path rather than across two.

**Item 4 — closed.** `mcp_relation_census.clj:1270-1278` uses
`census/workspace-root-token`; `b7f1df1a` states both rules in CENSUS-014 ("THE BOUND IS
OVER THE VALUE AS PRINTED … the identity fields exempt from the whole-field bound and never
from the leaf bound").

**The RED commits are red at their shas, for their stated reasons, and green at the tip.**
Each export run under my census battery:

```text
9e832529 (bound over one TYPE)        95 tests 2707 pass  44 fail  0 err
   FAIL :jvm :keyword-at-the-launchers-exit published 20226 bytes
        expected: (< … 8192)  actual: (not (< 20226 8192))
   FAIL :jvm :keyword-vector-at-the-ops-exit published 11704 bytes
d44baec6 (fence FAILS OPEN)           97 tests 2733 pass  27 fail  0 err
40e05f82 (MCP names root absolutely)  97 tests 2773 pass   2 fail  0 err
0a91e720 (the tip)                    97 tests 2775 pass   0 fail  0 err
```

**Sabotage 2 and 3 reproduce.** On the `git archive` export: opening the containment fence
(`escaping-source` always answering "contained") → **57 failures**, and the parity table
prints `agree false` once. Making the MCP empty-tree remedy name the canonical absolute
root → **2 failures**. Magnitudes differ from the brief's `125/21` and `47/2` because the
battery definition differs, but both classes are pinned and go loudly red. Only sabotage 1
fails to reproduce (§2).

**Gates, verbatim, with exit codes.**

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 800 tests containing 11136 assertions.
0 failures, 0 errors.
EXIT=0                                            [claim 800/11136/0 — matches]

$ ~/bin/suite-run make test-fast
Ran 829 tests containing 6873 assertions.
0 failures, 0 errors.
EXIT=0                                            [claim 829/6873/0 — matches]

$ swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
EXIT=0

$ make repository-hygiene
repository hygiene: no machine-local build cache is tracked at any depth
EXIT=0

$ git ls-files | grep -c cpcache
0                                                 [.cpcache untracked at the tip — confirmed]
```

**The merge did not weaken a trunk witness.** Every `test/` change in `b55c67da` measured
against the trunk parent `2af798b9` is net-additive, and each removal is a tool-count
recomputation from the merged catalog, not a relaxed assertion:

```text
mcp_http_server_test.clj      -6 +7    :tool-count 7→8, 6→7, "relation_census" added
mcp_server_test.clj           -4 +6    exposes-exactly-six→seven, (= 6 …)→(= 7 …)
mcp_stdio_smoke.clj           -3 +5    "exactly six"→"exactly seven"
help_test.clj                 -1 +2    :relation-census added to the op set
workspace_onboarding_test.clj -1 +1    "relation_census" added to enabled_tools
install_test.clj              -3 +75
```

No witness deleted, no ceiling raised, no assertion loosened. The parse-node admission
budget was resolved by splitting the launcher witnesses into their own namespace rather
than by raising `max-parse-nodes`, as claimed.

---

## 6. Rulings on the reported-not-fixed items

- **`mcp_contract.clj:72` third copy of the extension set** — correctly deferred, but the
  drift is wider than "a third copy". `src/` holds at least nine independent extension
  literals and they **disagree**: `#{"clj" "cljs" "cljc" "edn"}` (mcp_contract),
  `[".clj" ".cljs" ".cljc" ".edn"]` (intent_transaction:23),
  `[".clj" ".cljs" ".cljc"]` (structural_lens:24, mcp_alias_migration:229),
  `[".clj" ".cljc" ".cljs" ".pl"]` (mcp_intent_contract:174). Not a census defect and not a
  merge blocker; file it as one bead over the whole set rather than three rounds of
  "another copy", because the next reviewer will find a tenth.
- **Three path-form parity divergences (`..`, `.`, `//`)** — empirically closed (§3 table,
  all `agree true`). They should still enter the enumeration, since agreeing today without
  a witness is the state round nineteen described. The divergence that actually needs the
  row is `:dir`-is-a-file (§3).
- **`run-ls-tree` calls `System/exit`** (core.clj:1726, 1745) — correctly deferred; the
  code carries the bead reference `inb-eca3b1` inline at the site, which is the right shape
  for a deferral. Note it is now load-bearing for finding §1's fix as well: the op that
  evaluates the hostile build file is the same one that exits from inside a library call,
  so a caller embedding this cannot even catch the failure.
- **Item 7, left open on the round-19 reviewer's "recommended, not required"** — I rule the
  same way: recommended, not required, and not a merge blocker.

---

## Cleanup

Every mode I changed was restored (I changed none in the end — the chmod 500/400 attacks
were run against fixture directories I created and then removed wholesale). Both sabotaged
exports were restored and verified against their git objects before removal:

```text
src/clj_surgeon/relation_census.clj      export=31d787fcd26592f4  git=31d787fcd26592f4
src/clj_surgeon/core.clj                 export=4956e799c392930c  git=4956e799c392930c
src/clj_surgeon/mcp_relation_census.clj  export=df057ee454f9fb3a  git=df057ee454f9fb3a
```

Clone unchanged at the end:

```text
$ git rev-parse HEAD && git status --porcelain
0a91e72034e2359c552b155612ad2987ac43bc78

```

Fixtures removed — proof at the end of this review.

---

## NO-GO

**Mergeability:** `git merge-tree --write-tree HEAD origin/MCP/main` against trunk
`b1eb6767f7928d7339f317c8dd1f4329ce89cbad` exits 0 and emits the single tree oid
`869df9310d52785d02ce9e28abf0cb46037afcab` with zero conflicts, so the builder's
zero-conflict claim holds and this tip is textually mergeable — but it is **not GO on its
own for MCP/main**, because `core.clj:290` evaluates caller-influenced text through
`clojure.core/read-string` at both real launchers under the ordinary `:op :ls-tree :dir`
invocation, which is the exact class this branch's item 2 declares closed.
