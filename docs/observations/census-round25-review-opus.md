## GO

# Round-25 independent review (Opus) — clj-surgeon bridge/census-verb @ 2d82a677

*(Verdict line is revised as findings land; the final verdict is repeated at the end.)*

## Preconditions

```
$ cd /home/forge/tmp/sol/census21-wt && git rev-parse HEAD
2d82a677321bf7e25f241c1ef55051401e4dedd5
$ git status --porcelain
(empty)
```

Launchers, both REAL, as subprocesses, from the clone:

```
jvm: java -cp "$(clojure -A:clj-surgeon/mcp-test -Spath)" clojure.main -m clj-surgeon.core …
bb : bb -cp /home/forge/tmp/sol/census21-wt/src -m clj-surgeon.core …
bb v1.13.219 · openjdk 21 · trunk origin/MCP/main = a1083b94
```

Fixtures under `/var/tmp/forge/census25-review-fx` only; removed at the end. Nothing committed,
staged, stashed or pushed; the clone's `git status --porcelain` was empty before and after.

---

## 1. Item 1 — the `:paths` fence HOLDS. Every planted escape refused, at both launchers, byte-identical.

`src/clj_surgeon/core.clj:406` `fenced-source-paths`, called at `core.clj:539` (`discover-projects`)
and `core.clj:911` (`discover-projects-grep`). Every drive below is
`:op :ls-tree :dir <fixture>` at both launchers; `diff` of the two captures was empty in all seven.

| fixture | `deps.edn` `:paths` | output |
|---|---|---|
| `esc-abs` | `["/…/census21-wt/src/clj_surgeon"]` | `── total: 0 files, 0 forms` + `source_paths_outside_project: 1 entry` |
| `esc-rel` | `["../outside"]` | same shape, entry `"../outside"` |
| `esc-link` | `["src"]`, `src -> …/outside` | same shape, entry `"src"` |
| `mixed` | `["src" "../outside"]` | `src/x.clj` listed, **plus** the refusal block |
| `linkdir` (symlinked `:dir`) | `["src"]` | `src/a.clj` listed, no refusal — **the regression the builder caught is fixed** |
| `linkdir2` (symlinked `:dir`, `src -> outside`) | `["src"]` | refused |
| `linkdir3` (symlinked `:dir`, absolute escape) | `["src" "/…/clj_surgeon"]` | `src/a.clj` listed **and** the absolute entry refused |

```
$ FX/drive.sh jvm :op :ls-tree :dir $FX/esc-abs
── total: 0 files, 0 forms
── source_paths_outside_project: 1 entry
   esc-abs  "/home/forge/tmp/sol/census21-wt/src/clj_surgeon"  refused: it resolves outside the project root

EXIT=0
$ diff out-esc-abs.jvm out-esc-abs.bb   → (empty)
```

The round-23 finding-3 reproduction (`{:paths ["/…/src/clj_surgeon"]}` printing 80 files, 2,459
forms, every ns/require/def, at exit 0) is closed. The refusal names the entry AS SPELLED and
never the resolved target: `esc-rel` prints `"../outside"` and not `/var/tmp/forge/…/outside`.
The builder's stated requirement — an all-refused project must not read as "No Clojure files
found" — holds on the full-scan path.

The two-frames fix (`core.clj:427-440`, lexical entry vs UNRESOLVED root; real entry vs RESOLVED
root) is the right shape and I could not evade it: an absolute entry, a `..` entry, and a
symlinked entry are each caught, including all three *inside* a symlinked `:dir`, which is the
frame mix that produced the regression.

## 2. NON-BLOCKING FINDING — the fence is at both call sites; the *reporting* fix is at one. `:grep` still reports an all-refused project as "No Clojure files found".

`core.clj:552-554` (`discover-projects`) drops a project only when it has no files AND refused
nothing — the builder's item 2. `core.clj:916` (`discover-projects-grep`) still has the
unconditional `(remove #(empty? (:files %)))`:

```
$ sed -n '916p' src/clj_surgeon/core.clj
             (remove #(empty? (:files %))))
```

Driven, at BOTH launchers:

```
$ FX/drive.sh jvm :op :ls-tree :dir $FX/esc-abs :grep paths
No Clojure files found under /var/tmp/forge/census25-review-fx/esc-abs matching 'paths'
EXIT=1
$ FX/drive.sh bb  :op :ls-tree :dir $FX/esc-abs :grep paths
No Clojure files found under /var/tmp/forge/census25-review-fx/esc-abs matching 'paths'
EXIT=1
$ FX/drive.sh jvm :op :ls-tree :dir $FX/esc-rel :grep paths
No Clojure files found under /var/tmp/forge/census25-review-fx/esc-rel matching 'paths'
EXIT=1
```

Control: a project with one good and one escaping entry survives the `remove` and DOES report
on the grep path —

```
$ FX/drive.sh jvm :op :ls-tree :dir $FX/mixed :grep paths
src/x.clj  1 lines, 0 forms
  ns: inmixed

── total: 1 files, 0 forms
── source_paths_outside_project: 1 entry
   mixed  "../outside"  refused: it resolves outside the project root
EXIT=0
```

So the loss is exactly the all-refused case, and only on `:grep`. **The fence itself holds on
the grep path** — nothing escapes; what is lost is the receipt. That is the same defect the
builder's own commit message names as the second of the two he found ("The refusal has to
survive discovery to be reported by it"), and the same argument he makes one paragraph earlier
("A fence at one of two call sites is not a fence, it is that call site's habit, and `:grep` is
one argument away for the caller who just planted the file") applied to the drop rather than to
the fence.

**Not blocking** under this brief's stated bar: no read outside the workspace, no unbounded
caller value, no reader-eval. It is a one-line change (`(remove #(and (empty? (:files %))
(zero? (or (:refused-source-paths %) 0))))`, identical to `core.clj:553`) and it should ship
with the witness extended to the `:grep` drive.

## 3. NEW FINDING, non-blocking — the new refusal publishes an UNBOUNDED caller value, on both the text and the EDN surfaces, at both launchers.

`core.clj:794-798` prints `entry` with `format` and `core.clj:862` puts the same string into the
EDN receipt. Neither passes through `bound-refusal-text`, which is the control round twenty-two
built and round twenty-three verified applied even to bytes arriving through a config FILE
(a 20,023-byte `.clj-surgeon.edn` truncated to 1,186 bytes out). A build file's `:paths` entry
is the same shape of caller value and is not bounded.

One 40 KB entry, in ≈ out:

```
$ wc -c $FX/bigentry/deps.edn                  # {:paths ["../AAAA…40000…"]}
40017
$ FX/drive.sh jvm :op :ls-tree :dir $FX/bigentry | wc -c   → 40150
$ FX/drive.sh bb  … | wc -c                                → 40150   [byte-identical]
$ FX/drive.sh jvm :op :ls-tree :dir $FX/bigentry :format :edn | wc -c → 40188
   {:receipt {:source_paths_outside_project {:count 1, :entries [{:project "bigentry",
     :entry "\"../AAAAAAAA…"
```

Many small entries amplify ≈10x, because each entry costs ~65 bytes of fixed prose:

```
$ python3 …  {:paths ["../q" ×20000]}
$ wc -c deps.edn                        140011
$ FX/drive.sh jvm :op :ls-tree :dir $FX/manyentry | wc -c   1360090   (9.7x)
$ FX/drive.sh bb  …                     | wc -c             1360090   [byte-identical]
── source_paths_outside_project: 20000 entries
```

**Ruling: required fix, NOT a merge blocker**, and the reasoning is the round-23 reviewer's on
its finding 3 rather than leniency. What is published is the caller's OWN bytes handed back to
the caller; there is no fact about the box in it (the builder deliberately prints the spelling
and not the target, and I confirmed the target is absent in the relative case). It is reachable
only from `:op :ls-tree` on the CLI — `rg -n 'ls-tree|discover-projects' src/clj_surgeon/mcp_*.clj`
returns nothing, so no MCP tool surface carries it. And it is an op whose output is already
caller-proportional by design. Refusing the merge over it would leave the *arbitrary-tree read
at exit 0* live on trunk, which is strictly worse on the identical vector.

The fix is small and belongs in the next round with a named intent: bound each published entry
with `bound-refusal-text` and cap the entry LIST with a `+N more` tail, exactly as the leaf and
whole-field bounds already do — the round-19/20 finding was precisely that a caller controls the
leaf COUNT as well as the leaf.

## 4. Item 2 — the oracle widening is real and its two-tier rule is honest about what it gives up. Two UNDECLARED spelling gaps found.

I drove the detector itself (`test/clj_surgeon/reader_eval_fence_test.clj:465`
`evaluating-reader-calls-in`) on 23 synthetic sources through the tip's own classpath.

```
$ java -cp "$CP" clojure.main probe.clj
A (let [f eval] (f form))                  => []                          MISS  (declared)
B (apply load-string [s])                  => [… "load-string"]           CAUGHT
C (-> s read-string)                       => [… "read-string"]           CAUGHT
D (. clojure.lang.RT readString s)         => []                          MISS  (UNDECLARED)
E (Compiler/load rdr)                      => [… "Compiler/load"]         CAUGHT
F (requiring-resolve 'clojure.core/eval)   => [… "clojure.core/eval"]     CAUGHT
G (eval form) operator                     => [… "eval"]                  CAUGHT
H (clojure.lang.RT/readString s)           => [… "clojure.lang.RT/readString"] CAUGHT
I aliased (c/read-string s)                => [… "c/read-string"]         CAUGHT
J (map eval forms) bare arg                => []                          MISS  (declared)
K (load-file p)  L (load "res")  M (read r) P (resolve 'read-string)
Q ((var clojure.core/load-string) s)                                      CAUGHT
N local (let [read x] read)                => []                          correct non-hit
S (clojure.edn/read-string s)              => []                          correct non-hit
R (.invoke (clojure.lang.RT/var "clojure.core" "eval") f) => []           MISS  (reflection)
```

Six of the brief's named probes: **B, C, E, F caught; A and D missed; J missed.**
A and J are DECLARED out of scope, in the requirement itself — `shell-argv-safety-specs.md:22`
says "*the residual gap — a bare higher-order reference such as `(map eval forms)` — shall be
stated rather than hidden*". That is the honest way to ship a gap and I do not count it against
the round.

**D is not declared, and it is the sharper of the two.** The `.` special form reaches the
identical capability and the interop set cannot see it, because the set matches a whole symbol:

```
D2 (. clojure.lang.Compiler load rdr)      => []
```

The docstring's own argument — "*a fence the caller can step around by writing `RT/readString`
instead of `read-string` is a fence around a spelling*" — applies verbatim to `(. RT readString …)`.

**A second undeclared gap, in the ALIAS resolver, and it defeats the STRONG tier.**
`reader_eval_fence_test.clj:~455` `core-aliases` accepts only `vector?` libspecs, so a
prefix-list `:require` hides an alias. I proved the spelling is legal Clojure before claiming it:

```
$ cat nsprobe2.clj
(ns zz2 (:require (clojure [core :as k])))
(println "LOADED, prefix-list alias works:" (k/read-string "42"))
$ java -cp "$CP" clojure.main nsprobe2.clj
LOADED, prefix-list alias works: 42

$ probe:  "(ns z (:require (clojure [core :as k])))\n(defn g [s] (k/read-string s))"  => []
```

`k/read-string` — tier one, the exact fn the round-21 BLOCKING finding was about — is invisible.
(The other list spelling, `(:require (clojure.core :as k))`, I checked and it is NOT legal
Clojure: `IllegalArgumentException: Don't know how to create ISeq from: clojure.lang.Keyword`.
So the gap is the prefix-list form only.)

**No present violation.** I swept `src/` for all of it:

```
$ rg -n ':require\s*\(' src/           → 2 hits, both inside docstrings/predicates
$ rg -n '\(\.\s+(clojure\.lang\.)?(Compiler|RT)\b' src/   → 0
$ rg -n '\b(clojure\.lang\.)?(RT|Compiler)/' src/         → 0
```

**Non-blocking**, and it is the same rung as round twenty-three's finding 4: correct the oracle
in the same fix as the next round. Two lines — add the `.` head-symbol form to the interop match,
and accept a list libspec in `core-aliases` — plus two rows in
`the-oracle-names-every-evaluator-it-claims-to-fence`, which is where the round's 13 spellings
already live.

## 5. Item 3 — the build-file nesting ceiling is exact AT the boundary, typed, and byte-identical at both launchers.

`core.clj:290` `refuse-over-nested-build-file!`, called at `core.clj:378` OUTSIDE the `["src"]`
fallback. I asserted behaviour at the ceiling rather than at 10,001. `scanned-nesting-depth`
measures the whole file, so `{:paths [ ×N … ] }` measures N+1:

```
$ python3 …  {:paths [×255 "src" ]×255}          # measured depth 256
$ FX/drive.sh jvm/bb :op :ls-tree :dir $FX/deep255
── total: 0 files, 0 forms
── source_paths_outside_project: 1 entry
   deep255  [[[[…]]]]   refused: it resolves outside the project root
EXIT=0                                        ADMITTED by the ceiling  [byte-identical]

$ python3 …  {:paths [×256 "src" ]×256}          # measured depth 257
$ FX/drive.sh jvm/bb :op :ls-tree :dir $FX/deep256
{:error-type :build-file-nesting-too-deep,
 :build-file "deps.edn",
 :ceiling 256,
 :measured 257,
 :error
 "the build file deps.edn nests at least 257 deep, past the 256-level ceiling; it is refused unread, because a reader deep enough to measure it is a reader deep enough to overflow"}
EXIT=1                                        REFUSED                 [byte-identical]
```

Also drove 254 (admitted) and 257 nests (refused, `:measured 257` — the scanner short-circuits
at ceiling+1, so the published measure is bounded and does not grow with the input). The
`deep255` row is worth naming: at depth 256 the entry is a nested VECTOR, so it is admitted by
the ceiling and then refused by the string check one frame later — the two controls compose in
the right order, and the round-23 §2 parity divergence (bb reading it fine and dying inside
`io/file`) is gone.

`StackOverflowError` does not appear at either launcher on any of the four, and the 10,001-deep
case now takes the typed refusal rather than `-main`'s last-resort `catch Throwable`.

## 6. The merge (4f6cc775) — no trunk witness was weakened; both gate-found resolutions are sound.

Parents: `dbc90692` (branch) and `d8663852` (trunk). 81 files changed on the trunk side.

**`relation_census.clj:1431`** — the resolution is a COMMENT ONLY. `git diff dbc90692 4f6cc775 --
src/clj_surgeon/relation_census.clj` is six added lines, all prose, marking the `cli` destructure
as a forwarded-refusal-kind site for the ALIAS-059 scan. The claim it makes ("this site forwards
that literal verbatim and mints nothing of its own") is true: `cli` is destructured from a row of
`request-shape-rules`, every rule spells its kind as a keyword literal, and the source scan reads
those literals.

**`mcp_workspace.clj:42`** — the trunk had `(pr-str value)` interpolated into the `:remedy` PROSE;
the merged tree removes it and points at the `workspace_root_given` field instead.

*Confirmed as the brief asked: no trunk witness asserted the interpolation.*

```
$ git grep -n 'is not one' d8663852 -- test/
d8663852:test/clj_surgeon/tmp_leak_support_test.clj:88   (unrelated prose)
d8663852:test/clj_surgeon/tmp_leak_support_test.clj:177  (unrelated prose)
```

Nothing in the trunk's `test/` asserted that remedy string, so the resolution removed no
assertion. **And CENSUS-018 holds**: `mcp_relation_census_test.clj:7139` `root-carrying-fields`
enumerates `workspace_root_given` as exempt with the reason written down, and
`no-refusal-names-the-workspace-root-in-its-prose` (`:7175`) drives both the CLI and the MCP
enumerations plus a chmod-000 root. It is green in the `mcp-test` lane (§9).

## 7. The refusal enumeration 143 → 145 — verified BY ENUMERATION, not by reading the pin.

```
$ java -cp "$CP" clojure.main enum.clj      # calls refusal-kinds-in-source itself
:DERIVED-COUNT 145
:PINNED-COUNT 145
:DERIVED-MINUS-PINNED ()
:PINNED-MINUS-DERIVED ()
:CENSUS-TWO (census-worker-failure unparseable-file)

$ git show d8663852:test/clj_surgeon/mcp_alias_migration_test.clj | grep '(= 14'
5922:    (is (= 143 (count kinds))
```

The set derived from source at the tip is 145 and equals the pin in both directions; trunk's pin
was 143; the delta is exactly the two census kinds, and both are keyword literals in
`relation_census.clj:2073` and `:2203`, so the derivation reads them rather than the pin
asserting them into existence.

## 8. Gates — every claimed figure reproduced except ONE, and that one is a pre-existing trunk flake, proved.

All gates below ran on a **fresh clone** (`git clone --no-hardlinks` of the review clone, checked
out at `2d82a677`, `git status --porcelain` empty), never on the review clone itself.

| gate | claimed | measured | exit |
|---|---|---|---|
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | 865/13023/0 | **865 tests / 13009 assertions / 0 failures / 1 ERROR** | **1** |
| `~/bin/suite-run make test-fast` | 840/6919/0 | **Ran 840 tests containing 6919 assertions. 0 failures, 0 errors.** | 0 |
| `swipl -q -f test/mcp_operation_contract_oracle.pl` | pass | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | 0 |
| `make repository-hygiene` | green | `repository hygiene: no machine-local build cache is tracked at any depth` | 0 |
| `make census-battery` ×2 | byte-identical, md5 `a3057ecb…` | **both runs md5 `a3057ecbecb8a5959950a2ae31af7b63`**, `{:test 27, :pass 1336, :fail 0, :error 0}` | 0 |
| `make tmp-leak-ratchet-self-test` | green | `tmp-leak ratchet witness passed` | 0 |
| intent audit | `{:ok true :n 0}` | `:OK true :VIOLATIONS []` | — |
| `git merge-tree --write-tree HEAD a1083b94` | 0 conflicts | `MERGE_EXIT=0`, tree `40d8d449…`, `grep -c CONFLICT` = **0** | 0 |

**The battery digest is corroborated directly, not merely self-consistently.** My two runs on my
own fresh clone hash to `a3057ecbecb8a5959950a2ae31af7b63` — the builder's claimed
`a3057ecb…` — with only the JVM's `Picked up JAVA_TOOL_OPTIONS` line stripped.

### The one red: `mcp-test` exit 1, and why it is not this branch's

```
$ sed 's/^[0-9.]* //' mcp-test.out | grep -E 'ERROR|Ran '
ERROR in (prepared-confirm-preview-commit-and-replay-cross-the-real-http-wire) (URI.java:3186)
Uncaught exception, not in assertion.
  actual: java.lang.NullPointerException: Cannot invoke "String.length()" because "this.input" is null
 at java.net.URI.create (URI.java:930)
    clj_surgeon.mcp_prepared_wire_test$post_json.invokeStatic (mcp_prepared_wire_test.clj:181)
Ran 865 tests containing 13009 assertions.
0 failures, 1 errors.
```

Three independent facts put it outside this branch:

1. **The file is untouched by the branch.**
   `git log --oneline d8663852..2d82a677 -- test/clj_surgeon/mcp_prepared_wire_test.clj` → empty.
2. **The mechanism is a write/read race, visible in the source.** `mcp_prepared_wire_test.clj:222`
   does `(await! #(.isFile ready-file) …)` and then `(:url (edn/read-string (slurp ready-file)))`.
   `await!` (`:50`) polls `.isFile` only — it never checks the file has CONTENT — so a child that
   has created but not yet flushed `http-ready.edn` yields `edn/read-string ""` → `nil` →
   `(:url nil)` → `nil` → `URI/create nil` → this exact NPE. The window widens under load, and
   the suite ran at load 9-12.
3. **It passes on its own, repeatedly, at the same tip.** Three consecutive standalone runs of
   that namespace from the same clone:
   ```
   === run 1 ===  Ran 3 tests containing 27 assertions.  0 failures, 0 errors.
   === run 2 ===  Ran 3 tests containing 27 assertions.  0 failures, 0 errors.
   === run 3 ===  Ran 3 tests containing 27 assertions.  0 failures, 0 errors.
   ```

The 13009-vs-13023 assertion gap is the 14 assertions the aborted test did not reach, which is
consistent with the claimed 13023 being the same suite with that test completing. **The builder's
865/13023/0 is credible and I could not reproduce it in one shot; the test count 865 matched
exactly.** I do not treat this as a branch defect, and I do name it as a fleet-level ratchet owed
to somebody: `await!` should wait on a PARSEABLE ready-file, not on an inode. A readiness probe
that cannot see its own subject is the `:unverified` shape, and here it fails loudly, which is
the good version of that bug.

### The cost flag the brief asked for — the slowest namespace

Timestamped per-namespace, from the same run (`awk` over `Testing <ns>` lines):

```
   466 s  clj-surgeon.reader-eval-fence-test
    64 s  clj-surgeon.mcp-alias-migration-test
    62 s  clj-surgeon.mcp-relation-census-launcher-test
    39 s  clj-surgeon.mcp-relation-census-test
    20 s  clj-surgeon.mcp-prepared-wire-test
Elapsed (wall clock) time: 12:22.29     (load average 8.8-12.5 on 16 cores)
```

**`reader-eval-fence-test` is 466 s of a 742 s lane — 63% of `mcp-test` in one namespace**, from
six cold-JVM launcher subprocesses. The builder's own comment at `:156` already argues the
asymmetric matrix on exactly this ground, and the argument is right; but the trend is one-way and
the lane is now dominated by one witness. Worth a bead: the six `:jvm` drives share one subject
(the launcher's behaviour on a planted tree) and could be one drive over a table of fixtures.
Not blocking.

## 9. Further attacks on the fence that did NOT break it (recorded so the next round need not re-run them)

Both launchers, byte-identical output in every row.

- **A symlink INSIDE an admitted entry.** `{:paths ["src"]}` with `src/deep -> …/outside`:
  `── total: 1 files, 0 forms`, `secret-outside` absent. The walk does not follow links out of an
  admitted tree, so the fence and the walker compose.
- **A mid-path symlink in the entry.** `{:paths ["src" "a/sub"]}` with `a -> …/outside`: the
  lexical check passes (`root/a/sub` is under `root`), the REAL check catches it —
  `midlink  "a/sub"  refused: it resolves outside the project root`. This is the row that
  justifies keeping BOTH checks.
- **The shipped verb is unaffected.** `:op :relation-census` against `esc-abs`, `esc-rel` and
  `linkdir2` returns `:no-fold-arms-found` with `1 file(s) scanned` (its own tree) — the census
  fence held there before this round and still does. The whole exposure was CLI `:op :ls-tree`.

## 10. Sabotage — each fence pinned independently, on `git archive` exports, never on the clone.

Export integrity checked first: `git hash-object` on each patched file equals
`git rev-parse 2d82a677:<path>` before patching, and each patch was confirmed applied by `diff`
against the tip's blob.

**Sabotage A — `fenced-source-paths` neutered** (returns `{:paths (vec src-paths) :refused 0
:refused-entries []}`, the pre-fence behaviour):

```
$ cd $FX/sab-A && java -cp "$CP" clojure.main -e "(run-tests 'clj-surgeon.reader-eval-fence-test)"
Ran 7 tests containing 74 assertions.
:RESULT {:test 7, :pass 52, :fail 22, :error 0, :type :summary}

  8 × FAIL … no-real-launcher-follows-a-build-file-path-out-of-the-tree @:188  (printed `secret-outside`)
  8 × FAIL … same @:194                                                        (printed `def token`)
  4 × FAIL … same @:200                                                        (refusal did not name the spelling)
  2 × FAIL … a-non-string-paths-entry-never-reaches-io-file @:240              (jvm + bb)
```

**22 failures**, against the builder's claimed "20+". The decomposition is the right one: 8
launcher×build-file×spelling drives leaking on two assertions each, the four relative drives
losing the named-as-spelled refusal, and both launchers' non-string check. Note that neither
`the-fence-does-not-refuse-an-ordinary-path-under-a-symlinked-root` nor
`a-deeply-nested-build-file-is-refused-typed-not-overflowed` went red under A — so the false-positive
witness and the depth ceiling are pinned INDEPENDENTLY of the fence, which is the property round
twenty-two had to correct the round-20 receipt for.

### 6b. The other three of the merge's five resolutions, checked

- **`Makefile` `.PHONY`** — claimed 93 + 93 → 94. Measured, by tokenising the directive on all
  three commits: `head(dbc90692)=93 trunk(d8663852)=93 merged=94 tip=94`, `union expected: 94`,
  and `comm -23 <(union of both sides) <(tip)` is **empty** — no name on either side was dropped.
- **`test/clj_surgeon/mcp_intent_contract_test.clj` `expected-spec-docs`** — claimed union.
  `specs.md` entries: branch 38, trunk 38, tip **40**, with both
  `docs/intent/relation-census/relation-census-specs.md` and
  `docs/intent/temp-dir-hygiene/temp-dir-hygiene-specs.md` present (`:205`, `:208`).
- **`src/clj_surgeon/mcp_paths.clj`** — resolved by intent, not by union, as claimed: the branch's
  3-and-4 arity `path-refusal` body is kept and trunk's `@spec MCP-OP-ALIAS-059`
  `forwarded-refusal-kind` marker is carried onto it and extended to cover `cause`
  (`mcp_paths.clj:79-88`). The 3-arity delegates to the 4-arity, so every trunk caller is
  unchanged. This file also now takes `supported-source-extensions` from
  `census/named-source-extensions` rather than a literal — partial progress on the round-21/23
  carried item.

## 11. Rulings on the carried-over items (all unchanged, all still non-blocking)

- **`mcp_contract.clj:72` — the third copy of the extension set.** Still a literal
  `#{"clj" "cljs" "cljc" "edn"}`. `mcp_paths.clj` was converted to the shared def this merge, so
  the set is now two literals rather than three. Round twenty-one's wider point (nine independent
  extension literals in `src/`) stands; one bead over the whole set, not a fourth round of
  "another copy". **Not a merge blocker.**
- **`:unresolvable-source-path` has no parity row.** Unchanged. I rule as rounds nineteen,
  twenty-one and twenty-three did: recommended, not required. The exclusion is DERIVED
  (`(disj vocabulary …)`), so a cause added without a row still fails.
- **The three path-form parity divergences (`..`, `.`, `//`).** Still not in the enumeration; the
  battery's own table shows them agreeing today. Fourth round observed rather than pinned. It is
  four rows. **It should stop being deferred**, and it does not block.
- **`run-ls-tree` calls `System/exit`** (`core.clj:2032`, `:2038`). Still deferred, still carrying
  `inb-eca3b1` inline. Round twenty-three raised its priority because the escaping-`:paths` op was
  the same op that exits from inside a library call; that argument is *weaker* now that the escape
  is fenced, but the deferral is unchanged and correctly shaped. Not a blocker.

**Sabotage B — the build-file depth bound removed** (`core.clj:378` `(when text (refuse-over-nested-build-file! build-file text))` → `(when text nil)`):

```
Ran 7 tests containing 74 assertions.
:RESULT {:test 7, :pass 68, :fail 6, :error 0, :type :summary}

  2 × FAIL … a-deeply-nested-build-file-is-refused-typed-not-overflowed @:318  (StackOverflowError back)
  2 × FAIL … same @:323                                                        (not the TYPED refusal)
  2 × FAIL … same @:329                                                        (refusal does not name deps.edn)
```

**Exactly 6**, the builder's claim, and 2 × 3 is the right decomposition — both launchers on all
three assertions. Note the `(= 1 exit)` assertion did NOT fail: the last-resort `Throwable` exit
is also 1, which is precisely why the typed-refusal assertion has to exist separately.

**Sabotage C — the oracle narrowed back** (`evaluating-reader-names` → `#{"read-string" "load-string"}`, `evaluating-interop-names` → `#{}`):

```
Ran 7 tests containing 74 assertions.
:RESULT {:test 7, :pass 64, :fail 10, :error 0, :type :summary}

  9 × FAIL … the-oracle-names-every-evaluator-it-claims-to-fence @:545
      does not see "read"  "read+string"  "load-reader"  "load-file"  "load"  "eval"
                   "Compiler/load"  "RT/readString"  "clojure.lang.RT/readString"
  1 × FAIL … same @:562   (a real (read r) call must still be a hit)
```

The widened set is load-bearing on its own, and the failure NAMES each spelling it stopped
seeing. The three sabotages are independent: A touches only the fence witnesses, B only the
ceiling witness, C only the detector witness.

## 12. The RED commits are red at their own shas, for their stated reasons

```
$ cd $FX/red-da8   (git archive da8feb3c)
Ran 4 tests containing 42 assertions.
:RESULT {:test 4, :pass 24, :fail 18, :error 0, :type :summary}          ← claimed 18

$ cd $FX/red-dc1   (git archive dc148f1e)
Ran 6 tests containing 70 assertions.
:RESULT {:test 6, :pass 64, :fail 6, :error 0, :type :summary}
  2 × @:269  2 × @:274  2 × @:280   — all a-deeply-nested-build-file-is-refused-typed-not-overflowed
```

Both are green at the fix that follows them (§10 controls, and the tip's own `mcp-test` run).

## 13. NEW FINDING, non-blocking — the merge's `mcp_workspace` resolution is CORRECT but UNWITNESSED. Reverting it is fully green.

The merge commit says the trunk's `:remedy` interpolation was caught by "*the branch's
`no-refusal-names-the-workspace-root-in-its-prose` (MCP-OP-CENSUS-018)*". I tested that causal
claim by restoring the trunk's exact line on a `git archive 2d82a677` export
(`mcp_workspace.clj:42`, `"…already exists; " (pr-str value) " is not one…"`) and running
everything that could plausibly see it:

```
$ cd $FX/sab-D && java -cp "$CP" clojure.main -e "(run-tests 'clj-surgeon.mcp-relation-census-test)"
Ran 82 tests containing 2533 assertions.
:RESULT {:test 82, :pass 2533, :fail 0, :error 0, :type :summary}      EXIT=0

$ ~/bin/suite-run make census-battery
:BATTERY-RESULT {:test 27, :pass 1336, :fail 0, :error 0}              EXIT=0

$ run-tests over mcp-relation-census-launcher-test, mcp-paths-test, workspace-onboarding-test
Ran 23 tests containing 291 assertions.  0 failures, 0 errors.
```

**Nothing goes red.** The mechanism is visible in both halves of the CENSUS-018 pair:

- the DRIVE half (`mcp_relation_census_test.clj:7175`) checks each refusal against
  `(or root (.getCanonicalPath arms))` — a root the drive *has*. An `invalid-workspace-root`
  refusal is ABOUT a root that is not one of those, so interpolating it puts a string in the
  prose that the check is not looking for.
- the SOURCE-SCAN half (`:7267` `no-refusal-SITE-renders-a-raw-workspace-root-into-prose`) scans
  exactly three files — `core.clj`, `mcp_relation_census.clj`, `mcp_paths.clj` — and
  **`mcp_workspace.clj` is not among them**; its pattern also keys on a binding literally named
  `root`, and this site's binding is named `value`.

And no test anywhere asserts the remedy text: `rg -n 'Resend with workspace_root' test/` → 0 hits.

So the resolution is right by intent and the merge commit's *evidence* for it is wrong. This is
the "if an oracle existed and missed the bug, correct the oracle in the same fix" rung, and it is
also the shape my own standing note calls a marker-presence audit: the change carries an
`@spec MCP-OP-CENSUS-018` comment at `mcp_workspace.clj:33` and no executing witness behind it.
**Non-blocking** — reverting the change would make the receipt slightly worse, not unsafe, and
the merged state is the better one either way. **Required next round:** add `mcp_workspace.clj`
to the source-scan's file list and widen the pattern past the name `root`, then have the reviewer
re-plant this exact line and watch it go red.

---

## Verdict

**GO.** Every claim this round makes about its own three items is true at the tip, and I could
not break any of the three controls. The `:paths` fence refuses an absolute entry, a `..` entry
and a symlinked entry — each of them also *inside* a symlinked `:dir`, which is the frame mix
that produced the regression the builder caught — and the round-23 arbitrary-tree read at exit 0
is gone, byte-identically at both real launchers; a symlink inside an admitted path and a
mid-path symlink in the entry both fail to escape, and the shipped census verb was never exposed.
The build-file nesting ceiling is exact AT the boundary rather than at 10,001 (depth 256
admitted, 257 refused typed, `:measured` bounded, both launchers byte-identical), and it composes
in the right order with the string check. The widened reader/eval oracle catches ten of the
thirteen spellings I planted and is HONEST in its own requirement about the bare higher-order
reference it gives up; the two spellings it misses without saying so — the `.` special form and a
prefix-list `:require` alias — have no present violation in `src/` and are a two-line correction.
The three sabotages reproduce the claimed counts on `git archive` exports (22 against "20+", **6**
exactly, and 10 for the oracle) and go red *independently* of each other, so no bound is masking
another; both RED commits are red at their own shas for their stated reasons, 18 and 6. The
merge is sound in all five resolutions — `.PHONY` 93+93→94 with nothing dropped, `expected-spec-docs`
38+38→40, `mcp_paths` resolved by intent with trunk's marker carried, no trunk witness weakened —
and the refusal enumeration is **145 derived from source**, equal to the pin in both directions,
with the 143→145 delta exactly the two census kinds. Gates reproduce on a fresh clone:
test-fast 840/6919/0, oracle, hygiene, tmp-leak, intent audit `:ok true`, `merge-tree` 0
conflicts against `a1083b94`, and both census-battery runs hashing to the builder's own
`a3057ecbecb8a5959950a2ae31af7b63`. The single red I saw — `mcp-test` exit 1 — is a pre-existing
trunk flake in `mcp_prepared_wire_test`, a file this branch never touched, whose `await!` waits on
an inode rather than on parseable content and which passed three consecutive standalone runs at
the same tip. Nothing I found is a read outside the workspace, a reader-eval reachable from an
argument, or an unbounded refusal on a surface that carries a fact about the box; the four defects
I did find are reporting and ratchet defects that make the next round's list, not this merge's.

**BLOCKING: none.**

**NON-BLOCKING, required before the next round (in priority order):**

1. **§2 — `discover-projects-grep` still drops an all-refused project**, so `:op :ls-tree :grep`
   reports "No Clojure files found under \<dir\>" over a fenced walk, at both launchers. The fence
   is at both call sites; the reporting fix is at one. One line, plus a `:grep` row in the witness.
2. **§3 — the new refusal publishes an unbounded caller value** on both the text and the EDN
   receipt (40,017 B in → 40,150 B out; 140,011 B in → 1,360,090 B out, 9.7x). Bound each entry
   with `bound-refusal-text` and cap the list with a `+N more` tail.
3. **§13 — the merge's `mcp_workspace` CENSUS-018 resolution is unwitnessed**: restoring the
   trunk's interpolated remedy leaves `mcp_relation_census_test` 82/2533/0, the battery
   27/1336/0 and three more namespaces green. Add `mcp_workspace.clj` to the source-scan's file
   list, widen the pattern past the binding name `root`, then re-plant and watch it go red.
4. **§4 — two undeclared spellings escape the corrected oracle**: the `.` special form
   (`(. clojure.lang.RT readString s)`, `(. clojure.lang.Compiler load r)`) and a prefix-list
   alias (`(ns z (:require (clojure [core :as k])))` — proved legal Clojure — hiding
   `k/read-string`, a TIER-ONE name). No present violation; correct the oracle in the next fix.
5. **§8 (fleet, not this lane)** — `mcp_prepared_wire_test/await!` should wait on a parseable
   ready-file, not on `.isFile`; and `reader-eval-fence-test` is now **466 s of a 742 s
   `mcp-test` lane** (63%) from six cold-JVM launcher drives that share one subject.
6. Carried, unchanged, all previously ruled non-blocking: the `mcp_contract.clj:72` extension
   literal (now two copies, not three); `:unresolvable-source-path` with no parity row; the three
   path-form parity divergences observed for a fourth round without being pinned; `run-ls-tree`
   calling `System/exit` (`inb-eca3b1`).

**This tip is GO on its own for MCP/main**: `git merge-tree --write-tree HEAD a1083b94` exits 0
with zero conflicts against the current trunk. This lane LANDS.

## GO
