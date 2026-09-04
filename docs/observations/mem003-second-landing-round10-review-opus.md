## NO-GO

*(Round-ten independent LANDING review of clj-surgeon `bridge/integration-2026-09-03-mem003`
at `a9963bd3`, MEM-003 measured clock, second landing. Sol's content filter has refused this
lane every round; this is the Opus review, done directly.)*

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git rev-parse HEAD && git status --porcelain
```

```text
a9963bd3b2c9efe41d709e2b29f44b575083a027
```

(`git status --porcelain` printed nothing.) Nothing in the review clone was committed,
staged, stashed, pushed or edited; no branch was checked out in it. No server was started on
any port; none of 7888 / 7890 / 7894 / 7895 / 8171 was contacted. Fixtures live only under
`/var/tmp/forge/mem003r10-review-fx`, removed at the end. Current trunk at review time:
`origin/MCP/main` = `d5a5a3fd`.

---

## Headline

**Round nine closes round eight's blocker for real, and I reproduced it closing.** N1–N4 —
the parenthesised-member dot form, `..` and `memfn` — each go RED at the production receipt
site now, two failures apiece, from a base that is 29 tests / 176 assertions / **0 failures**.
The composition work (option (i), the census adapting to the measured contract) is sound: the
census reads elapsed through the partition-aware reader, no golden fixture moved, and the
refusal enumeration is 147 by derivation on the tip.

**It is NO-GO, and for the sixth round running it is the same shape one level down.** Round
eight's lesson was *"a derivation over names must also enumerate the GRAMMAR the names can
appear in."* The grammar includes **whitespace**, and Clojure's whitespace includes the
**newline**. Both alternative builders correctly write `\s+` between receiver and member — but
the scanner never hands the pattern a string that can contain a newline, because `sites`
matches **one line at a time**. Every parenthesised-member face round nine just added is
defeated by pressing Enter.

---

## 1. BLOCKING — the scan matches ONE LINE AT A TIME, so every multi-line spelling is invisible

`sites` (`test/clj_surgeon/measured_invariant_test.clj:96-109`) reduces over
`(str/split-lines (slurp file))` and matches the pattern against `code`, which is a single
line with its `;;` tail cut:

```clojure
   (reduce (fn [{:keys [form hits]} line]
             (let [code (or (first (str/split line #";;")) "")
                   ...
                :hits (into hits
                            (repeat (count (re-seq pattern code))
                                    [(site-path file root) form']))}))
           {:form nil :hits []}
           (str/split-lines (slurp file))))))
```

Both alternative builders write `\s+` between the receiver and the member
(`clock-expression-alternative` `:326-380`, `escape-hatch-alternative` `:790-835`). `\s`
matches `\n` — so the authors' own intent is arbitrary whitespace. **That tolerance is dead
on the only axis where it matters**: no string presented to the pattern ever contains a
newline. The `\s+` is load-bearing for spaces and tabs and inert for the line break.

### 1a. The plants, at the real production receipt site, on `git archive a9963bd3` copies

Same site the last three rounds used — `src/clj_surgeon/mcp_hot_verify.clj`, the SUCCESS
receipt, one line inserted after `:elapsed_ms (measured/elapsed-ms started)` (line 98), so the
value lands in an undeclared receipt field **inside the hashed parity subject**:

```clojure
:verification_wall_ms (let [rr (measured/elapsed-ms started)] <FORM>)
```

Scanning gate, under babashka — the runtime the gate actually runs in:

```sh
for d in base N1 N2 N3 N4 P7 P8 P9 P10 P11 P12; do (cd /var/tmp/forge/mem003r10-review-fx/$d && \
  bb -cp src:test -e '(require (quote [clojure.test :as t]) (quote clj-surgeon.measured-invariant-test)) (println (pr-str (t/run-tests (quote clj-surgeon.measured-invariant-test))))'); done
```

```text
base {:test 29, :pass 176, :fail 0, :error 0, :type :summary}
N1   {:test 29, :pass 174, :fail 2, :error 0, :type :summary}   (. System (nanoTime))
N2   {:test 29, :pass 174, :fail 2, :error 0, :type :summary}   (.. System (nanoTime))
N3   {:test 29, :pass 174, :fail 2, :error 0, :type :summary}   (. rr (_launder))
N4   {:test 29, :pass 174, :fail 2, :error 0, :type :summary}   ((memfn _launder) rr)
P7   {:test 29, :pass 176, :fail 0, :error 0, :type :summary}   (. rr \n (_launder))
P8   {:test 29, :pass 176, :fail 0, :error 0, :type :summary}   (. System \n (nanoTime))
P9   {:test 29, :pass 176, :fail 0, :error 0, :type :summary}   (. System \n nanoTime)
P10  {:test 29, :pass 176, :fail 0, :error 0, :type :summary}   (.. System \n (nanoTime))
P11  {:test 29, :pass 176, :fail 0, :error 0, :type :summary}   ((memfn \n _launder) rr)
P12  {:test 29, :pass 176, :fail 0, :error 0, :type :summary}   (.. rr \n (_launder))
```

**N1–N4 are closed — round eight's blocker is genuinely fixed, and the fix is non-vacuous.**
**Six newline-split forms sit at the exact baseline.** P9 is round-eight's N1 with one
newline in it, and P9 is not even a parenthesised member: it is `(. System nanoTime)`, the
plainest raw-clock dot form there is, which has been in the pattern since round six.

### 1b. The newline is whitespace to the reader, and the numbers are real

```sh
cd /var/tmp/forge/mem003r10-review-fx/base && java -cp "$(clojure -Sdeps '{:paths ["src" "test"]}' -Spath)" clojure.main .../reader.clj
```

```text
reader equivalence — the newline is whitespace to the Clojure reader:
  (. rr (_launder)) == (. rr (_launder)) -> true
  (. System nanoTime) == (. System nanoTime) -> true
  (.. System (nanoTime)) == (.. System (nanoTime)) -> true
  ((memfn _launder) rr) == ((memfn _launder) rr) -> true

and it really reads a clock / launders a reading:
  (. System\n nanoTime)  => 1994473217719813
  tagged reading printed  => #clj-surgeon.measured/reading
  (. rr\n (_launder))    => 1.42654
```

A raw sixteen-digit `nanoTime` and a laundered reading's number, both published into a
receipt field inside the hashed parity subject, with the gate at its exact baseline. By the
brief's own standard that is BLOCKING.

### 1c. Why this is not a contrivance

The plant line is long. **A formatter wraps it.** `(. instance-expr (method args))` with real
arguments, or a `memfn` in a threading form, routinely spans two lines in ordinary
hand-written Clojure and in anything cljfmt has touched. This is not an exotic spelling a
reviewer invented — it is what the code looks like after the editor is done with it. The
round-nine faces (`..`, `memfn`, parenthesised member) are precisely the *longest* spellings
and therefore the *most* likely to be wrapped: the fix that closed round eight created the
population most exposed to this hole.

### What would close finding 1

The pattern is already right; only the input is wrong. `sites` needs to match over the file
as one string while still attributing a hit to a form and cutting `;;` comments — e.g. cut
comments per line, `str/join "\n"` the remainder, and attribute by the `(def…`/`(defn…`
boundary offsets it already tracks. The hit count stays `(count (re-seq pattern code))`, so
round six's calls-not-lines property is preserved.

**And the ratchet on the ratchet is nearly free:** `round-eight-review-plants` already exists
as data at `:1229`. A newline-split twin of each entry, added there, makes the fail-first
witness go red without a new test being written — and would have caught this before I did.

---

## 2. VERIFIED — round eight's blocker is genuinely closed (brief claim 1)

N1–N4 planted at the same production receipt site each take the gate from
`{:test 29, :pass 176, :fail 0}` to `{:pass 174, :fail 2}` — table in §1a. The manifest and
alternative counts on the tip, read from the tree's own live vars:

```text
clock alternatives: 373
escape alternatives: 29
```

(The build record's "232 → 372" is the derived-spelling count; my 373 is the count of `|`-split
alternatives in the compiled pattern, a different denominator, not a discrepancy.)

### The NEW grammars I planted, and my honest ruling on each

Probed against the tip's own `clock-pattern` and `escape-hatch-pattern`:

```text
(. rr -launderable)                        clock=false escape=TRUE
(.-launderable rr)                         clock=false escape=TRUE
(doto (java.util.Date.) (.getTime))        clock=TRUE  escape=false
(doto cal (.getTimeInMillis))              clock=TRUE  escape=false
(-> (java.util.Calendar/getInstance) (.getTimeInMillis))  clock=TRUE  escape=false
(-> rr (._launder))                        clock=false escape=TRUE
(-> rr ._launder)                          clock=false escape=TRUE
(apply (memfn _launder) [rr])              clock=false escape=TRUE
(apply (memfn getTimeInMillis) [cal])      clock=TRUE  escape=false
(java.util.Date.)                          clock=TRUE     (new java.util.Date)  clock=TRUE
(Date.)                                    clock=TRUE     (new Date)            clock=false
(bean (java.util.Date.))                   clock=TRUE  (via the constructor)
(. x -nanoTime) / (.-nanoTime x)           clock=false escape=false
(definterface IClock (^long nanoTime []))  clock=false escape=false
(reify IClock (nanoTime [_] 42))           clock=false escape=false
```

**CAUGHT and in scope:** the `doto` chain, both `->` threads, `apply` + `memfn` on both sides,
the field-access faces of the escape hatch (`(.-launderable`, `(. rr -launderable`), the
constructor in all its FQ spellings. These are real closures and they were not luck: the inner
form still spells a derived member, and the derivation now carries the field face on the escape
side because a `Reading` genuinely has a mutable field.

**`(. x -nanoTime)` / `(.-nanoTime x)` — field access to a clock-holding field: NOT A ROUTE,
and I can show it rather than assert it.** The derivation walks `.getMethods` only, never
`.getFields`, so no clock alternative has a field face. I enumerated every public field of all
20 derived clock source classes:

```text
derived clock source classes: 20
PUBLIC FIELDS whose type is numeric or a time type:
   java.time.Instant / EPOCH : java.time.Instant STATIC final
   java.time.LocalTime / NOON : java.time.LocalTime STATIC final
   java.util.Calendar / MILLISECOND : int STATIC final
   … (every one STATIC final) …
```

**Every public field on every derived clock class is `static final` — a constant, not a
reading.** `Calendar/MILLISECOND` is the ordinal `14`; `Instant/EPOCH` is a fixed instant. All
the mutable state on these classes is private. So the missing field face costs nothing today.
**Non-blocking, but it is luck the derivation cannot see, and one line makes it evidence:** a
witness asserting `(.getFields c)` yields no non-static time-or-numeric field on any derived
class would turn "there is no such field" from my finding into the ratchet's own.

**`definterface` / `reify` with a clock-named method — NOT A ROUTE, honestly outside scope.**
These DEFINE a member; they read nothing. Calling one would spell `.nanoTime`, which the
derivation carries. No plant is possible.

**`(new Date)` (the `new` special form with a SIMPLE class name) — a real miss, and inert.**
`(java.util.Date.)`, `(new java.util.Date)` and `(Date.)` are all caught; only `(new Date)` is
not. But a `Date` yields a number only through an accessor, and every accessor is derived —
planted as `(let [d (new Date)] (.getTime d))` it goes **RED, 2 failures**. To publish from it
you would have to reach the number by a route that is itself already an offence.
**Non-blocking**; worth one alternative for completeness, not worth a round.

---

## 3. VERIFIED — the census composition (brief claim 2)

**(a) The summary reads elapsed through the partition-aware reader.** Planted a top-level
`elapsed_ms` alongside a real partition:

```text
A. partition present AND a planted top-level elapsed_ms:
   receipt = {:measured {:elapsed_ms 5}, :elapsed_ms 999999}
   op/elapsed-ms => 5    <- partition wins, planted top-level NOT read
```

**Exact about the tolerance**, because it is not what the claim says on its face:
`mcp-operation/measured-field` (`src/clj_surgeon/mcp_operation.clj:109`) is
`(or (get-in result [measured/measured-key k]) (get result k))`, so a receipt with **no**
partition still falls back to the top level (probe case B returns `999999`). That fallback is
documented at `:99-103` and is defensible — the invariant test forbids *producing* an
unpartitioned measured field, so no published receipt can carry one — but the honest statement
is "the partition WINS", not "a top-level field is never read".

**(b) CENSUS-013's amended text matches the wire.** Driven for real through
`handle-relation-census` on the tree itself:

```text
{:measured {:phases_elapsed_ms {:read 192.858956, :classify 376.084066,
                                :merge 1.348363, :discover 93.767659},
            :elapsed_ms 691.085659}, …}
"relation_census\n  12 file(s) · 15 arm(s) · 12 site(s) · … · 691.09 ms\n…"
```

`phases_elapsed_ms` is inside `measured`; there is no top-level `elapsed_ms` or
`phases_elapsed_ms`; the summary renders `691.09 ms`, which is the exact defect round nine
diagnosed, working. `census-output-schema` declares `"measured"` and `:required ["ok"
"operation" "measured"]` (`mcp_relation_census.clj:97-98`).

**(c) No census golden fixture changed.**

```sh
git diff --stat cb14686c..a9963bd3 -- test-fixtures/
```

printed nothing. The whole range touches 9 files, none under `test-fixtures/`.

---

## 4. VERIFIED — the streaming encoders carry every refusal block (brief claim 3)

`source-path-refusals` (`src/clj_surgeon/core.clj:855-869`) produces exactly **one** kind —
`source_paths_outside_project`, one entry per fenced `:paths` entry — so I enumerated instead
**every** block the batch renderer emits (`source_paths_outside_project`,
`parser_admission_refused`, `resources` + the measured line) and planted all of them at once:
a `deps.edn` with `{:paths ["src" "../root-outside"]}` plus a 201-level-deep file against the
150 ceiling.

**babashka launcher, streaming text:**

```text
── total: 2 files, 1 forms
── parser_admission_refused: 1 file(s)
   src/deep.clj  max_parse_depth limit 150, observed 201
── resources: bytes_scanned 442
── measured (not hashed): scan_ms 13.282
── source_paths_outside_project: 1 entry
   proj  "../root-outside"  refused: it resolves outside the project root
```

**JVM launcher, streaming text:** identical block set (`scan_ms 3.998`).
**Streaming EDN:** `:source_paths_outside_project {:count 1, :entries [{:project "proj",
:entry "\"../root-outside\""}]}` present in the trailer alongside the admission rows.

**Both encoders, both launchers, both refusal kinds. Claim confirmed.**

### 4a. NON-BLOCKING — the block ORDER differs from the batch renderer

The code comment claims the escaping block is *"byte-for-byte the batch encoder's"*
(`core.clj:1187`). Each block's own text is; the **output** is not. Batch emits
escaping → admission → resources; streaming emits admission → resources → escaping:

```text
batch (format-ls-tree-text, driven directly):
── total / ── source_paths_outside_project / ── parser_admission_refused / ── resources
streaming (the real CLI path):
── total / ── parser_admission_refused / ── resources / ── source_paths_outside_project
```

Harmless today — `format-ls-tree-text` and `format-ls-tree-edn` now have **no production
caller** (`rg` finds only `core.clj:2464-2465` using the encoders, and the batch pair only in
tests) — but **no witness compares the two on a tree carrying both refusals**, so the
byte-identity claim in the comment is unchecked prose. Either narrow the comment to "the same
block text" or add the differential; do not leave a byte-identity claim nothing reads.

---

## 5. VERIFIED by ENUMERATION — 147 refusal kinds (brief claim 4)

Not read from the pin. `refusal-kinds-in-source` run on both trees:

```sh
cd <tree> && java -cp "$(clojure -A:clj-surgeon/mcp-test -Spath)" clojure.main enum.clj
```

```text
--- TIP a9963bd3 ---              --- TRUNK d5a5a3fd ---
count= 147                        count= 145
invalid-measured-start? true      invalid-measured-start? false
unpartitioned-measured-field? true  unpartitioned-measured-field? false
invalid-mcp-elapsed-time? true    invalid-mcp-elapsed-time? true
```

Set difference both ways:

```text
tip 147  trunk 145
in tip, not trunk: ("invalid-measured-start" "unpartitioned-measured-field")
in trunk, not tip: ()
```

**147 = the current trunk's 145 plus exactly the two measured-partition kinds, and nothing is
lost.** The pin (`mcp_alias_migration_test.clj:5949-5958`) is a count plus a two-way
`difference` against the derived set, so it is a pin on a derivation, not a literal.

---

## 6. NON-BLOCKING, and inherited — `discover-projects-grep` drops an all-refused project (brief claim 5)

Reproduced. `discover-projects` (`src/clj_surgeon/core.clj:620-623`) keeps a project with no
files when it refused something, with a comment explaining that dropping it *"sends the op to
'No Clojure files found …' — a completeness claim over a walk that was fenced"*.
`discover-projects-grep` (`:1313`) is `(remove #(empty? (:files %)))`, unconditional.

One tree, `{:paths ["../root-outside"]}` and nothing else, both paths on the tip:

```text
=== NORMAL path ===                        === GREP fast path (:grep paths) ===
── total: 0 files, 0 forms                 {:error "No Clojure files found under …proj
── source_paths_outside_project: 1 entry             matching 'paths'",
   proj  "../root-outside"  refused: …      :error-type :no-clojure-files,
                                            :next-action "widen_the_scan_root_or_relax_the_grep"}
```

The fence held — nothing outside the tree was read — but the skip is **silent**, and the
`next_action` actively misdirects: no widening or relaxing will ever help, because the entry
was refused, not missed. It is the exact failure MCP-OP-SHELL-ARGV-006 exists to prevent, one
call site over from the comment arguing that *"a fence at one of two call sites is not a fence,
it is that call site's habit."*

**Attributed to the trunk, not to this branch.** `d5a5a3fd` behaves identically:

```text
=== TRUNK d5a5a3fd, grep fast path ===
No Clojure files found under …/fence2/proj matching 'paths'
=== TRUNK, normal path ===
── source_paths_outside_project: 1 entry
```

**It does not block this landing.** It carries no clock reading and no laundered number, it is
another lane's requirement, it is pre-existing on the trunk, and the round-25 census review
already listed it as next-round. The builder was right to report and not fix it inside a merge.
I record only that the symmetrical one-line fix is available and this branch is already editing
this file for this requirement.

---

## 1b. BLOCKING (same mechanism, worse consequence) — the newline reopens ROUND THREE's `:refer` bypass

Finding 1 is not confined to the two interop scans. `measured-naming-offenders`
(`measured_invariant_test.clj:1441-1453`) is line-based too, and `measured-naming-offence`
(`:1424-1439`) returns **nil** for any line that does not itself spell `clj-surgeon.measured`.
So splitting the require across lines defeats it in both directions at once:

```text
-- measured-naming-offence, line by line --
  "[clj-surgeon.measured :as measured]"                       offence=nil   (sanctioned)
  "[clj-surgeon.measured :refer [raw-nanos] :as measured]"    offence=:refer
  "[clj-surgeon.measured"                                     offence=nil   <- names it, no :refer on THIS line
  " :refer [raw-nanos]"                                       offence=nil   <- :refer, but names no namespace
  " :as measured]"                                            offence=nil
  "[clj-surgeon.measured :as m]"                              offence=:alias
```

And the escape-hatch pattern is written against the `measured/` prefix, so the bare name the
`:refer` introduces is invisible to it:

```text
-- escape-hatch-pattern on BARE (unprefixed) calls --
  (raw-nanos)              escape=false        (measured/raw-nanos)     escape=true
  (value r)                escape=false        (measured/value r)       escape=true
  (raw-ms)                 escape=false        (measured/raw-ms)        escape=true
```

**This is round three's §1b bypass, verbatim, reopened by pressing Enter** — the exact attack
`sanctioned-measured-require`'s docstring (`:115-133`) says it exists to close: *"a brand-new
namespace requiring `[clj-surgeon.measured :as measured :refer [raw-nanos]]` … called
`(raw-nanos)` under a bare name the clock scan does not match, published the result under an
undeclared key, and left all nine tests green with the field in the parity hash."*

Planted end to end on a `git archive a9963bd3` copy — the require split, and the raw verb
called under its bare name at the same production receipt site:

```clojure
   [clj-surgeon.measured
    :refer [raw-nanos]
    :as measured]
...
                   :elapsed_ms (measured/elapsed-ms started)
                   :verification_wall_ms (raw-nanos)
```

```text
base {:test 29, :pass 176, :fail 0, :error 0, :type :summary}
R1   {:test 29, :pass 176, :fail 0, :error 0, :type :summary}
```

```text
raw-nanos returns: 1995228515352838   class: java.lang.Long   number? true
```

A bare `System/nanoTime` (`src/clj_surgeon/measured.clj:237-240`) published into an undeclared
receipt field inside the hashed parity subject, through the *sanctioned* namespace, with the
gate at its **exact baseline**. This is the worst form of the class: it needs no interop
spelling, no reflection, and no `Reading` at all.

**One fix closes 1 and 1b together** — give every scan in this file a whole-file subject
instead of a per-line one. That is the finding, stated once: **the file's patterns are
correct; its INPUT is a line, and Clojure forms are not lines.**

---

## 1c. The recommended fix is FREE on this tree — I measured it before recommending it

The obvious objection to "join the lines" is that a whole-file subject will light up the
allow-lists and force a re-blessing. **It does not.** I cut `;;` comments per line exactly as
`sites` does, joined the remainder with `\n`, and counted `re-seq` hits both ways over every
file under `scanned-roots` with the tip's own live patterns:

```sh
cd /var/tmp/forge/mem003r10-review-fx/base && bb -cp src:test .../fixcheck.clj
```

```text
clock  TOTAL per-line 61   whole-file 61
escape TOTAL per-line 19   whole-file 19
```

**No file differs.** Not one allow-list entry moves, not one declared count changes, and
round six's calls-not-lines property is preserved because the counter is still
`(count (re-seq pattern code))`. The change is to `sites`' subject and nothing else.

The `[path form]` attribution `sites` already tracks by `(def…` prefix survives the change:
attribute by the offset of the last `(def` boundary at or before each match rather than by the
current line. And the same one-line change to `measured-naming-offenders` (`:1449`) and
`unknown-measured-verbs` (`:1934`) closes 1b with it.

**This is runtime-independent.** `sites` splits lines before the pattern is ever consulted, so
babashka and the JVM behave identically here; the JVM's richer derivation cannot help, because
no string containing a newline is ever offered to either pattern.

## 1d. Both findings are INSIDE the requirements' own stated scope — checked against the text, not my taste

`docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:33` (MCP-OP-TIME-005):

> *"a clock read shall be an offence **whatever spelling names it** … This requirement is
> bounded by what a SOURCE-TEXT scan can decide: a route that spells neither its class nor its
> member anywhere in the text, **because one or both is computed at runtime** … is a DECLARED
> RESIDUAL … and is outside this requirement"*

`(. System\n nanoTime)` spells both the class and the member as ordinary source tokens, in the
text, computed at nothing. The residual is explicitly about **runtime-computed names**; a
newline computes nothing. **Inside scope.**

`:34` (MCP-OP-TIME-006):

> *"each derived name shall be matched in **every form its GRAMMAR admits**"* … and
> *"**every reference** under those roots that names `clj-surgeon.measured` in **any form**
> other than the one sanctioned require **shall be an offence**"*

Whitespace is part of the grammar — the builders' own `\s+` says so. And the last clause is
finding 1b stated as a requirement: a split `[clj-surgeon.measured\n :refer [raw-nanos]\n :as
measured]` **is** a reference in a form other than the sanctioned require, and it is not an
offence today. **1b is a direct, textual violation of MCP-OP-TIME-006's final clause**, not an
interpretation of it.

Neither finding needs the requirement widened. Both need the scan's subject fixed.

---

## 7. VERIFIED — merge-tree clean against the CURRENT trunk (brief claim 7)

The trunk moved during the round; I re-checked against what `origin/MCP/main` is now.

```sh
cd /home/forge/tmp/sol/mem003r4-wt && git rev-parse origin/MCP/main
git merge-tree --write-tree HEAD origin/MCP/main
```

```text
trunk: d5a5a3fd43f59a7dfc329d6e849249ffa394c719
ac82c3b29c6e38d4aa213706d783b2e49b073fd1
EXIT=0
```

One tree oid, no conflict section, exit 0. `git status --porcelain` still empty afterwards.

---

## 8. What ELSE I confirmed, briefly

- **`raw-nanos` / `raw-ms` have exactly one prefixed call site in `src/`**
  (`mcp_operation.clj:131`, allow-listed). The bare-name route is closed **only** by the
  require rule — which is exactly what finding 1b defeats. Nothing else stands behind it.
- **`census-output-schema` is right**: `"measured"` declared, `:required ["ok" "operation"
  "measured"]`, and the old top-level `elapsed_ms` / `phases_elapsed_ms` properties are gone
  (`mcp_relation_census.clj:67-98`).
- **The batch formatters have no production caller any more** — `core.clj:2464-2465` uses the
  streaming encoders; `format-ls-tree-text` / `-edn` appear only in tests.
- **The build records are honest.** Round eight ATTRIBUTED the fourteen composition failures by
  running both parents rather than guessing, declined to resolve another lane's contract inside
  a merge commit, and said so. Round nine names the single line that caused four symptoms
  (`(:elapsed_ms result)` read top-level) and fixed it at the reader in product code rather than
  editing the three fixtures to fit. That is the right instinct twice in a row, and it is worth
  saying plainly in a review that ends NO-GO.

---

## 9. Apparatus disclosure

My first JVM suite run was SIGTERM'd (`EXIT=143`) partway through
`clj-surgeon.reader-eval-fence-test` — collateral from my own concurrent foreground JVM probes,
not a defect in the tree. I discarded it and re-ran. `reader_eval_fence_test` drives the real
launchers as subprocesses, so on a box at load 25-31 (other seats' work, not mine) it is
minutes-scale; that is slowness, not a stall — the process was at ~10% CPU and spawning
launcher JVMs throughout.

---

## 10. GATES — reproduced on a FRESH `git clone` at the tip `a9963bd3` (brief claim 6)

Cloned, `git remote set-url` to the real remote, `git fetch` inside, `git checkout a9963bd3`;
`git status --porcelain` empty, HEAD verified. `uptime` checked before each; every JVM suite
under `~/bin/suite-run`, the battery alone under `flock /home/forge/tmp/suite.lock` with a
fresh `MEMBAT_ROOT=/home/forge/tmp/membat-r10`, reference built explicitly first, never
`MEMBAT_ALLOW_ANY_ROOT`.

| gate | claimed | observed | load at start |
|---|---|---|---|
| JVM suite, run 1 | 870/13232/0 | **VOID — `EXIT=143`**, SIGTERM'd by my own concurrent probes (see §9) | 8.57 |
| JVM suite, run 2 | 870/13232/0 | `Ran 870 tests containing 13232 assertions.` `0 failures, 0 errors.` `EXIT=0` | 11.25 |
| JVM suite, run 3 | 870/13232/0 | `Ran 870 tests containing 13232 assertions.` `0 failures, 0 errors.` `EXIT=0` | 11.41 |
| babashka suite | 935/7403/0 | `Ran 935 tests containing 7403 assertions.` `0 failures, 0 errors.` `EXIT=0` | 10.79 |
| operation oracle | pass | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | — |
| intent audit | 410 / 0 | `:ok true`, **410** distinct specs, `:violations []` | — |
| txn kernel warnings | 0 | `kernel warning check: 2 namespace(s), 0 warning(s)` | — |
| `make memory-red PARSER_RED_EXPECT=green` | 6/6 | `memory-red: 6/6 assertions held (expect=green)` | 11.13 |
| tmp-leak ratchet | pass | `tmp-leak ratchet witness passed` | — |
| admit-analyzer memory self-test | 3/3 | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` | — |
| battery self-test | 32/171/0 | `Ran 32 tests containing 171 assertions.` `0 failures, 0 errors.` | — |
| `make census-battery` | 27/1336/0 | `:BATTERY-RESULT {:test 27, :pass 1336, :fail 0, :error 0}` | — |
| `git merge-tree --write-tree HEAD origin/MCP/main` | clean | exit 0, tree `ac82c3b2`, vs trunk **`d5a5a3fd`** | — |

**Every claimed figure reproduces exactly.** The JVM suite ran twice green (three attempts, one
void for a reason that is mine, not the tree's).

### The full memory battery, ONCE, under the exclusive lock

```text
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 9.8, :limit 3.0, :small-n-observed 1.0, :slack-mb 2.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 40.9, :limit 6.5, :small-n-observed 4.5, :slack-mb 2.0}
  UNMEASURED reserved-peak-over-budget {:op :cli-ls-tree, …}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-full-match, …}
  UNMEASURED reserved-peak-over-budget {:op :rename-ns-plan-narrow, …}
  UNMEASURED reserved-peak-over-budget {:op :workspace-sources-read-all, …}
receipt: /home/forge/tmp/membat-r10/receipts/20260904T221337.895702309Z-battery.edn
```

Parity read from the RECEIPT, not the console:

```text
cells: 48
distinct :reference-mismatch: (nil)
reference-mismatch cells: 0
tool-errors: []
attestation: {:jvm "21.0.12", :head-sha "a9963bd3b2c9efe41d709e2b29f44b575083a027"}
```

**Exactly what the brief expected and exactly what round nine recorded:** `FAIL (INCOMPLETE)`
on MEM-001's lane only, **48 cells, 0 reference mismatches**, the same two
`held-scales-with-n`, four `UNMEASURED`. The attestation names the tip itself, `a9963bd3`, so
it attests this code with no docs-commit caveat at all.

**Housekeeping:** 10 leaked `clj-surgeon-suite-*` temp dirs under `/var/tmp/forge` — other
seats', not this branch's; counted, not deleted. `/var/tmp` at 28% free space, 86% free inodes.


---

## VERDICT

## NO-GO

**This tip does not land.** Round nine's own work is good and I verified all of it: round
eight's parenthesised-member blocker is genuinely closed (N1–N4 each go red at the production
receipt site, from a clean 29/176/0 base); the census composition is right on the wire, with
`phases_elapsed_ms` inside `measured`, the partition-aware reader winning over a planted
top-level `elapsed_ms`, and not one byte of `test-fixtures/` moved; both streaming encoders
carry both refusal blocks on both launchers in both formats; the refusal enumeration really is
147 by derivation, exactly the current trunk's 145 plus the two measured-partition kinds and
nothing lost; and `git merge-tree --write-tree HEAD origin/MCP/main` is clean against the
current trunk `d5a5a3fd`. But the scan that carries this whole landing matches **one line at a
time** (`measured_invariant_test.clj:96-109`), while the patterns it applies are written with
`\s+` between receiver and member — so the tolerance the authors intended is dead on the one
axis that matters, and every parenthesised-member face round nine just added is defeated by
pressing Enter. Six newline-split plants at the same production receipt site
(`mcp_hot_verify.clj`, inside the hashed parity subject) leave the gate at its **exact
baseline** while publishing a raw sixteen-digit `nanoTime` and a laundered reading's number;
worse, the same mechanism reopens round three's `:refer` bypass verbatim, because a require
split across three lines is an offence on none of them and the bare `(raw-nanos)` it
introduces is invisible to a pattern written against the `measured/` prefix — a `System/nanoTime`
in a receipt field through the *sanctioned* namespace, with all 176 assertions green. Both are
inside the requirements' own stated scope (MCP-OP-TIME-005's residual is explicitly for
runtime-computed names, and MCP-OP-TIME-006's final clause forbids naming the namespace in
"any form" other than the sanctioned require), and this is not a formatter's-edge-case
argument: receipt lines and long `:refer` vectors are exactly what wrapping produces. **The
fix is one line and I measured that it is free** — joining the comment-stripped lines yields
identical hit counts on the live tree (clock 61/61, escape 19/19), so no allow-list entry moves
and round six's calls-not-lines property survives; apply it to `sites`,
`measured-naming-offenders` and `unknown-measured-verbs`, and add newline-split twins to
`round-eight-review-plants` so the ratchet catches this class itself next time.
**Blocking: findings 1 and 1b.** **Non-blocking: 2 (the missing clock field face — provably
inert today, every public field on all 20 derived clock classes is `static final`; `(new Date)`;
both worth one alternative each), 4a (the streaming/batch block ORDER differs from the
byte-identity the comment claims, unchecked by any witness), and 6 (`discover-projects-grep`
drops an all-refused project — reproduced, but inherited from the trunk, another lane's
requirement, and correctly reported-not-fixed).** One more round.
