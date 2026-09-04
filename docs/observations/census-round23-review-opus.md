## GO-WITH-FIX

# Round-23 independent review (Opus) — clj-surgeon bridge/census-verb @ dbc90692

*(Sol's content filter refused this brief for the seventh consecutive census round; taken by Opus
with substituted paths: verdict here, fixtures under /var/tmp/forge/census23-review-fx/opus.)*

Verdict line is the LAST line of this file. Findings appended incrementally as reproduced.

## Preconditions

```
$ cd /home/forge/tmp/sol/census21-wt && git rev-parse HEAD
dbc9069226463f61ed7a793e393d2a63331865ca
$ git status --porcelain
(empty)
$ git log -1 --format='%H %s'
dbc9069226463f61ed7a793e393d2a63331865ca fix: the fence namespace never COMPILED, and four intents had no src witness
```

Launchers used throughout (both REAL, as subprocesses, from the clone):

```
jvm: java -cp "$(clojure -A:clj-surgeon/mcp-test -Spath)" clojure.main -m clj-surgeon.core …
bb : bb -cp /home/forge/tmp/sol/census21-wt/src -m clj-surgeon.core …
bb v1.13.219 · openjdk 21.0.12 · trunk origin/MCP/main = add7aba8ef07ebe894cb43f54becbd26c937077d
```

---

## 1. Round-21 BLOCKING (reader-eval at `extract-source-paths`) — CLOSED. Reproduced at both launchers, all THREE build files, plus `.clj-surgeon.edn`.

`src/clj_surgeon/core.clj:327` is now `edn/read-string`. I planted the round-21 payload —
`{:paths #=(clojure.core/spit "<marker>" "READER EVAL EXECUTED")}` — in `deps.edn`, `bb.edn`,
`project.clj` (the brief's ask: the builder's plant covered `deps.edn` only) and `.clj-surgeon.edn`,
and drove `:op :ls-tree :dir <plant>` at both launchers. Same marker listing before and after:

```
$ ls $FX/PWNED-*                       # BEFORE
ls: cannot access '…/PWNED-*': No such file or directory
… 8 drives (jvm|bb × deps.edn|bb.edn|project.clj|.clj-surgeon.edn) …
$ ls $FX/PWNED-*                       # AFTER
ls: cannot access '…/PWNED-*': No such file or directory
```

The three build files: exit 0, tree listed, reader inert (`extract-source-paths` catches the
edn failure and falls back to `["src"]`, which is the documented answer for an unreadable build
file). `.clj-surgeon.edn` goes one better and refuses typed at BOTH launchers:

```
{:file ".../evil-.clj-surgeon.edn/.clj-surgeon.edn",
 :error ".../.clj-surgeon.edn: invalid EDN — No dispatch macro for: =",
 :error-type :invalid-arguments}
EXIT=1
```

**Tagged literals.** `#inst`, `#uuid` and a custom `#foo/bar` in `deps.edn`: all six drives
exit 0 with the tree listed and nothing evaluated. `clojure.edn/read-string` consults
`RT/DEFAULT_DATA_READERS` (inst, uuid) and the `:readers` opt only — **not** `*data-readers*` —
so a repo-local `data_readers.clj` cannot re-arm this site. Confirmed by the custom tag simply
throwing to the `["src"]` fallback.

**The refusal bound holds on config-file bytes too**, which the brief did not ask for and is
worth recording. A 20,023-byte `.clj-surgeon.edn` carrying a 20,000-character tag name — a
caller value arriving through a FILE rather than argv — is truncated by the leaf bound at both
launchers:

```
… "No reader function for tag ev/AAAA…AAAA… [truncated: 20108 characters]",
 :error-type :invalid-arguments}
$ wc -c bigtag-jvm.out → 1186     $ wc -c bigtag-bb.out → 1186
```

20,108 caller characters in, 1,186 bytes out, byte-identical across launchers.

---

## 2. The DISCLOSED GAP (no depth bound at the build-file read) — REAL, reproduced, and it takes the new `Throwable` exit. NON-BLOCKING.

The builder disclosed that `refuse-over-nested!` guards `parse-val`'s two collection branches
only, so `extract-source-paths` reads a build file with no depth bound. Reproduced:

```
$ python3 -c "n=10001; open('deep-deps/deps.edn','w').write('{:paths '+'['*n+']'*n+'}')"
$ jvm  :op :ls-tree :dir deep-deps        (20,011 B build file)
{:error "the launcher failed with java.lang.StackOverflowError and no message",
 :error-type :invalid-arguments}
EXIT=1
$ jvm  :op :ls-tree :dir deep-deps100     (200,009 B)      → same typed refusal, EXIT=1
$ jvm  :op :ls-tree :dir deep-cfg         (.clj-surgeon.edn, 10,001 deep) → same, EXIT=1
```

**Which exit it takes: `core/launcher-throwable-refusal` via `-main`'s `catch Throwable`** —
exactly the last-resort exit round twenty-two added for this class. No stack trace, no
evaluation, no caller bytes published, bounded output, exit 1. That is the whole promise the
`Throwable` widening was made for, and the gap the builder disclosed is the case that proves it.
**Not blocking:** it is neither a reader-eval nor an unbounded caller value nor a read outside
the workspace.

Two divergences worth the record (neither blocking, both new information):

```
$ bb  :op :ls-tree :dir deep-deps    (10,001 deep)
{:error "No implementation of method: :as-file of protocol: #'clojure.java.io/Coercions
         found for class: clojure.lang.PersistentVector", :error-type :invalid-arguments}
EXIT=1
$ bb  :op :ls-tree :dir deep-cfg     (10,001 deep .clj-surgeon.edn)
src/a.clj  1 lines, 0 forms … ── total: 1 files, 0 forms
EXIT=0
```

babashka does **not** overflow at 10,001 (it does at 100,000): it reads the nested vector
successfully and then fails one frame later, in `io/file`, because nothing validates that
`:paths` is a sequence of strings. Same input, two launchers, two different exits — a real
parity divergence, and the tell for finding 3 below. Both refusals are still typed and bounded.

---

## 3. NEW FINDING — a build file under a caller-named `:dir` still steers `:op :ls-tree` to READ OUTSIDE THE WORKSPACE. The round closed the reader half of this vector and left the config half open.

The bb divergence above says `:paths` from the discovered build file is used **unvalidated**.
It is also used **unfenced**. `core.clj:366` does `(mapcat #(find-clj-files (fs/path root %)) src-paths)`,
and `fs/path` resolves an absolute or `..`-prefixed component straight out of the named tree.

```
$ mkdir -p $FX/outside && cat > $FX/outside/secret.clj
(ns secret-outside)
(def token :leaked)

$ cat $FX/esc-rel/deps.edn
{:paths ["../outside"]}
$ jvm :op :ls-tree :dir $FX/esc-rel
../outside/secret.clj  2 lines, 1 forms
  ns: secret-outside
  2: def token
── total: 1 files, 1 forms
EXIT=0
```

Absolute paths work identically, and the reach is arbitrary:

```
$ cat $FX/esc-far/deps.edn
{:paths ["/home/forge/tmp/sol/census21-wt/src/clj_surgeon"]}
$ jvm :op :ls-tree :dir $FX/esc-far
../../../../../../home/forge/tmp/sol/census21-wt/src/clj_surgeon/agent_routing.clj  160 lines, 14 forms
  ns: clj-surgeon.agent-routing
  requires: [clj-surgeon.file-ops :as file-ops] [clojure.java.io :as io] [clojure.string :as str]
  12: def managed-begin
  …
── total: 80 files, 2459 forms
EXIT=0
```

**Exit 0, a green receipt, and the namespace, requires, every `def`/`defn` name and its line
range for 80 files outside the directory the caller named.** Identical at both launchers.
`{:paths ["/etc"]}` walks `/etc` and reports `No Clojure files found under <the named dir>` —
so the walk leaves the workspace and the message still names the workspace.

This is the round-21 finding with the payload changed from code to configuration. Same op,
same frame, same premise — *controlling a directory is enough* — and it survives round
twenty-three's class ratchet because that ratchet asks "does a reader evaluate?" and this asks
"does a fence exist?". The answer at `discover-projects` is no: `census-workspace`,
`escaping-source` and the `:dir` fence all measure the path the CALLER named; nothing measures
the paths the BUILD FILE named.

**Scope, established rather than assumed:**

- `:op :relation-census` — the verb this branch ships — is **NOT** affected. Driven against
  `esc-far`: `{:ok false, :error-type :no-fold-arms-found, …, :files-scanned 1}`. It scanned the
  one real file under the named tree; its own fence held.
- The defect is **byte-identical on trunk**:
  `diff <(git show origin/MCP/main:…core.clj | sed -n '/^(defn- discover-projects$/,/^(defn- rg-available/p') <(same on HEAD)` → no output, `IDENTICAL`.
  This branch neither introduced nor widened it; it removed the *code-execution* half of exactly
  this vector.

**Ruling: required fix, not a merge blocker.** The brief's bar makes any read outside the
workspace blocking, and read literally this qualifies. I decline to block on it, and the reason
is not leniency: merging changes trunk's exposure here by zero bytes, while refusing the merge
leaves `read-string` on trunk — the strictly worse state on the identical vector. Blocking would
buy nothing and cost the fix already in hand. It must be filed with a named intent and a witness
before the next round: `:paths` entries are validated as strings and resolved-then-fenced against
the project root, with an escaping entry a typed, counted refusal rather than a silent read.

---

## 4. The CLASS ratchet — real, and narrower than the brief's description of it.

`test/clj_surgeon/reader_eval_fence_test.clj:180` parses every `src/` source with rewrite-clj,
walks token nodes only (so the three docstrings naming the rule are not hits), resolves
`clojure.core` aliases from each `ns` form, and fails on any hit outside
`allowed-evaluating-reader-sites`, which is `#{}` and separately asserted empty. That design is
right — parsed, not grepped — and it passes at the tip (finding 9).

But the brief says the witness catches "any `clojure.core/read-string` (bare or aliased),
`load-string`, **eval**". It does not — and to be fair to the builder, the INTENT it implements
(`MCP-OP-SHELL-ARGV-005`) scopes itself to "`clojure.core/read-string` or
`clojure.core/load-string`", so the witness matches its own declared requirement exactly; it is
the brief that overstated. The gap is in the requirement, not in the implementation of it. `reader_eval_fence_test.clj:128`:

```clojure
(def evaluating-reader-names
  #{"read-string" "load-string"})
```

`eval` is absent, and so are `clojure.core/read` (which honours `*read-eval*` exactly as
`read-string` does, given a `PushbackReader`), `read+string`, `load-reader`, `load-file` and
`load`. I swept `src/` for all of them myself:

```
$ rg -n '\(read\s|\(load-file|\(load-reader|read\+string|\(load\s|clojure\.core/read\b|\(eval\s' src/
(seven hits, every one inside a docstring or comment describing this rule — no call sites)
```

So the allow-list is honestly empty **today** on the wider set too; the ratchet just would not
notice tomorrow. Correcting `evaluating-reader-names` is a one-line change and is the "if an
oracle existed and missed it, correct the oracle in the same fix" rung. Non-blocking: no
present violation. The witness also scans `src/` only — `test/` and `bin/` are out of scope by
construction, which is defensible and should be written into the docstring rather than inferred.

**The other evaluators, enumerated as the brief asked.** Two SCI sites are reachable from a
caller-named directory or argument, and neither is a *reader*-eval:

- `forms.clj:143` `(sci/eval-form (sci/init (sci-opts)) form)` compiles a `(fn …)` extractor out
  of `.clj-surgeon.edn`, found by walking UP from the caller's `:file`/`:dir`. The EDN reader is
  inert (proved in finding 1); the interpreter is an explicit allow-list — `:namespaces` is
  ~40 named fns under `z`/`n`/`str`, `:bindings` is `fields/public`, and SCI grants no Java
  interop without `:classes`, so there is no `spit`/`slurp`/`System` in reach. A declared,
  documented feature with a real sandbox. It is **not depth- or time-bounded**, so a hostile
  `.clj-surgeon.edn` can still supply a non-terminating extractor; worth a bound, not worth a
  block.
- `edit_dsl.clj:418` `(sci/eval-string+ context expression …)` evaluates an expression the
  caller supplied explicitly — the feature is the evaluation.

---

## 5. The depth ceiling at argv — correct AT the boundary, and its `:value` leaf IS bounded.

`core.clj:2597 max-argument-nesting-depth = 256`; `refuse-over-nested!` throws
`:argument-nesting-too-deep` carrying `:value s`, the caller's whole argument. That merge into
the refusal is the shape the round-19 blocking finding was about, so I measured it:

```
$ jvm/bb  :op :relation-census :dir <clean> :doors "$(python3 -c "n=10001;print('['*n+']'*n)")"
                                                   # 20,002-character argument
{:error-type :argument-nesting-too-deep, :ceiling 256, :measured 257,
 :value "[[[[…]]]]… [truncated: 20002 characters]",
 :error "an argument nests at least 257 deep, past the 256-level ceiling; it is refused
         unread, because a reader deep enough to measure it is a reader deep enough to overflow"}
EXIT=1
refusal bytes: 1327 (jvm)   1327 (bb)   — byte-identical
```

At the boundary, exactly:

```
depth 256 → accepted by the ceiling, refused one frame later as :doors-not-a-string
depth 257 → :argument-nesting-too-deep, refused UNREAD
```

**I could not evade `scanned-nesting-depth`.** Its string and character-literal handling is
consistent with the reader (`\"` outside a string is a char literal to both; `\\` consumes two
to both), and every case where it *diverges* — `;` comments and `#_` discards, which it does not
model — makes it OVER-count, i.e. refuse something the reader would have accepted. Every
divergence is in the safe direction, which is the right way for a pre-reader scanner to be wrong.

**And the whole-field bound holds on the identity-key exemption.** The sharpest input I could
build is `:dir` — an exempt key — carrying a 10,000-element vector, every leaf two characters,
so the leaf bound has nothing to catch and the whole-field bound is exempted:

```
$ jvm/bb :op :relation-census :dir "[:a :a … 10000 …]"    # 30,001-character argument
{:ok false, :error-type :dir-not-a-string,
 :error ":dir must be a non-empty path (got [:a :a …… [truncated: 30200 characters]"}
refusal bytes: 5488 (jvm)   5488 (bb)   — byte-identical
```

The exemption is not reachable: the non-string `:dir` never survives to a `:dir` KEY, it is
folded into the `:error` STRING by the type check, and the string leaf bound truncates it. The
builder's own docstring says the non-string branch is defence in depth at this tip; I drove it
and that is exactly what it is.

---

## 6. Item 4 (`:dir` is a FILE) and the path forms — CLOSED, both launchers, all four forms agree.

`core.clj:815` now guards the resolved path with `(when (.isDirectory (.toFile real)) real)`.
Every drive below is `:op :relation-census :dir <x>` at both launchers; the two launchers
produced identical `:error-type` on all sixteen:

| `:dir` | jvm | bb |
|---|---|---|
| a regular FILE | `:invalid-workspace-root` | `:invalid-workspace-root` |
| symlink LOOP (`loopA→loopB→loopA`) | `:invalid-workspace-root` | `:invalid-workspace-root` |
| nonexistent | `:invalid-workspace-root` | `:invalid-workspace-root` |
| symlink to an OUTSIDE tree | reaches census (`:no-fold-arms-found`) | same |
| trailing `/` | reaches census | same |
| `…/clean/../clean` | reaches census | same |
| `…/clean/.` | reaches census | same |
| `…//clean` | reaches census | same |

`:invalid-workspace-root` is the MCP entrance's name, which is item 4's whole point. The symlink
row is the declared round-19 rule (the workspace is the RESOLVED tree, so a link to a real tree
is that tree) and it behaves as declared.

**One nit, non-blocking, same class as the one just fixed.** A directory the process cannot
traverse (`chmod 400`) still yields a COMPLETENESS-shaped receipt:

```
$ chmod 400 $FX/noexec && jvm :op :relation-census :dir $FX/noexec
{:ok false, :error-type :no-fold-arms-found,
 :remedy "Nothing under <workspace_root> defines defmethod fold-event arms (0 file(s) scanned)…",
 :files-scanned 0, :skipped-outside-root 1}
$ chmod 755 $FX/noexec     # RESTORED — verified 755, contents intact
```

"Nothing under the workspace defines fold arms" is asserted over a tree that was never read.
It is milder than the `:dir <file>` case round twenty-two fixed, because `:files-scanned 0` is
published and disambiguates it — but `:skipped-outside-root 1` is also published, and nothing
was outside the root; that counter is being credited for a traversal failure. Worth a look; not
a blocker. Every chmod in this review was restored and the restoration verified.

---

## 7. Item (5) — the RESUME FINDING is closed. Both halves verified.

The killed session shipped a witness namespace that never compiled, so the round's blocking
witnesses ran in no lane, and four intents carried `@spec` markers only in commit messages.

**The namespace loads and runs, in a named lane with a named count.** `mcp_test_runner.clj`
adds `clj-surgeon.reader-eval-fence-test` to BOTH the `:require` vector and the run list (diff
`0a91e720..dbc90692`), and the lane executed it:

```
$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
…
Testing clj-surgeon.mcp-relation-census-launcher-test
Testing clj-surgeon.reader-eval-fence-test
…
Ran 806 tests containing 11203 assertions.
0 failures, 0 errors.
```

**Lane: `mcp-test`. 806 tests / 11,203 assertions / 0 failures / 0 errors** — exactly the
claimed 806/11203/0.

**The intent audit is clean and the markers are in the SOURCES, not the commit messages:**

```
$ bb -cp src:test -e "(require '[clj-surgeon.mcp-intent-contract :as ic]) …"
{:ok true, …}          :violation-count 0
```

All four new intents — `MCP-OP-CENSUS-034`, `MCP-OP-CENSUS-035`, `MCP-OP-SHELL-ARGV-004`,
`MCP-OP-SHELL-ARGV-005` — appear as `:implemented` in `:specs` and are members of BOTH
`:implementation-witnesses` and `:test-witnesses`. Since the audit reads `src/`, `test/` and the
`Makefile` off disk and knows nothing about git history, membership there is proof the markers
are in the files.

---

## 8. Item (2) — the corrected sabotage receipts. ALL THREE EXECUTED AND REPRODUCED.

Round twenty withdrew "118/28" because neither bound was pinned (each masked the other).
Round twenty-two says each is now pinned separately. Every sabotage below was applied to a
`git archive dbc90692` export under my fixture directory — never to the clone — and each patch
was confirmed applied by `diff -r` against an unmodified export of the same commit before it ran.
Export integrity was checked first: `git hash-object` on the three files I patch equals
`git rev-parse dbc90692:<path>` for all three (0 mismatches, 2,574 files exported).

**Sabotage A — `bound-refusal-leaf` reverted to `(if (string? x) (bound-refusal-text x) x)`:**

```
$ cd $FX/sab-A && make census-battery
MISSING: []
  :r22   the-leaf-bound-is-what-saves-an-exempt-fields-non-string-leaf  pass 4  fail 3  error 0
:BATTERY-RESULT {:test 27, :pass 1331, :fail 3, :error 0}
make: *** [Makefile:929: census-battery] Error 1        SAB_A_EXIT=2
```

**3 failures**, exactly as claimed, and all three land in the one witness the builder named as
the pin. The leaf bound is now load-bearing on its own.

**Sabotage B — the whole-field bound removed from `bound-refusal`** (`(walk/postwalk bound-refusal-leaf v)` with no field measurement), driven with `:many-small-leaves-at-the-ops-exit`:

| | jvm | bb |
|---|---|---|
| sabotaged | **51,730 B** | **51,669 B** |
| tip (control, same drive) | 2,745 B | 2,684 B |

~19x, unbounded at BOTH real launchers, against the claimed 51,657 B — the few-byte difference
is the absolute workspace path, which differs between my export and the builder's. The
whole-field bound is load-bearing on its own. Each bound now goes red without the other:
A and B are independent.

**Sabotage C — `extract-source-paths` reverted to `clojure.core/read-string`:**

```
$ cd $FX/sab-C && java -cp … clojure.main -e "(clojure.test/run-tests 'clj-surgeon.reader-eval-fence-test)"
FAIL … the jvm launcher EVALUATED deps.edn while listing the tree the caller named — exit 0 …
FAIL … the jvm launcher EVALUATED bb.edn      …
FAIL … the jvm launcher EVALUATED project.clj …
FAIL … the bb  launcher EVALUATED deps.edn    …
FAIL … the bb  launcher EVALUATED bb.edn      …
FAIL … the bb  launcher EVALUATED project.clj …
FAIL … src/ calls an EVALUATING reader at 1 site(s) …
Ran 2 tests containing 14 assertions.
7 failures, 0 errors.
:RESULT {:test 2, :pass 7, :fail 7, :error 0, :type :summary}
```

**Exactly 7**, and the decomposition is the right one: 6 instance failures (2 launchers × 3
build files) plus 1 class-ratchet failure. The fix is witnessed at both launchers for all three
build files, not just the `deps.edn` the plant used.

---

## 9. Gates — every claimed figure reproduced.

Lanes note, as the brief requires: `~/bin/suite-run`'s three lanes were all held by other
fleet work for most of this review. `mcp-test`, `test-fast` and battery run 1 went through
`suite-run` (queued); **the three sabotage runs and battery run 2 were run one JVM at a time
directly**, at load 8.4 on 16 cores, never more than one of mine at once.

| gate | claimed | measured | exit |
|---|---|---|---|
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | 806/11203/0 | **Ran 806 tests containing 11203 assertions. 0 failures, 0 errors.** | 0 |
| `~/bin/suite-run make test-fast` | 829/6873/0 | **Ran 829 tests containing 6873 assertions. 0 failures, 0 errors.** | 0 |
| `swipl -q -f test/mcp_operation_contract_oracle.pl` | pass | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | 0 |
| `make repository-hygiene` | green | `repository hygiene: no machine-local build cache is tracked at any depth` | 0 |
| `make census-battery` ×2 | 27/1334/0, byte-identical | `{:test 27, :pass 1334, :fail 0, :error 0}` both runs | 0 |
| intent audit | `{:ok true :n 0}` | `{:ok true}`, `:violation-count 0` | — |

Battery determinism, my own capture:

```
$ diff <(run1) <(run2) → IDENTICAL
$ md5sum: c034c85e960a0b7495881efeb823385b (both runs)
```

My md5 is over my own capture with the JVM's `Picked up JAVA_TOOL_OPTIONS` line stripped, so it
is **not** comparable to the builder's `3ecc556d…` and I do not claim it corroborates that
digest. What I verified is what the digest is for: the two runs are byte-identical to each
other, and the composition line is the claimed `27/1334/0`.

`.cpcache` is untracked at the tip (`.gitignore:38`), hygiene is green, and the clone's
`git status --porcelain` was empty before, during and after every drive.

**No test was weakened.** `census_witness_battery.clj` only ADDS (23 → 27 witnesses, four `:r22`
entries); the launcher test's diff removes no drive — the two `-` lines are vector-close lines
re-emitted with the new entries appended.

---

## 10. Rulings on the carried-over items.

- **Round-19 item 7 (`:unresolvable-source-path` has no parity row).** Unchanged at the tip
  (`mcp_paths.clj:68,271`; exclusion at `mcp_relation_census_test.clj:6713-6720`). **I rule as
  rounds nineteen and twenty-one did: recommended, not required, not a merge blocker.** The
  exclusion is DERIVED — the assertion is over `(disj vocabulary …)` — so a cause added without
  a row still fails. That structure is what makes the hole safe, and it has not changed.
- **`mcp_contract.clj:72`, third copy of the extension set.** Still there
  (`supported-source-extensions #{"clj" "cljs" "cljc" "edn"}`), and round twenty-one's wider
  point stands: at least nine independent extension literals in `src/` that disagree. One bead
  over the whole set, not a fourth round of "another copy". Not a merge blocker.
- **Three path-form parity divergences (`..`, `.`, `//`).** Empirically closed again this round
  at both launchers (§6 table, all four forms agreeing). **Still not in the enumeration.** The
  ruling is unchanged: agreeing today without a witness is exactly the state round nineteen
  described, and this is the third round it has been observed rather than pinned. It is small —
  four rows in the existing parity table — and it should stop being deferred.
- **`run-ls-tree` calls `System/exit`** (`core.clj:1783, 1802`). Still deferred, still carrying
  `inb-eca3b1` inline, which is the right shape for a deferral. **It is now load-bearing for
  finding 3 as well**: the op whose build file steers a read outside the workspace is the same
  op that exits from inside a library call, so an embedder cannot catch it and cannot fence it.
  That raises its priority; it does not block this merge.

---

## 11. Mergeability — the merge-tree dry run is NOT clean any more, and that is a stale base, not a defect.

```
$ git rev-parse origin/MCP/main
add7aba8ef07ebe894cb43f54becbd26c937077d          ← trunk sha
$ git merge-tree --write-tree HEAD origin/MCP/main
MERGE_EXIT=1
14150ce01ee08161c3eae22bc2afbc745308cd83
… CONFLICT (content): Merge conflict in Makefile
… CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_intent_contract_test.clj
```

The builder's "zero conflicts" claim was TRUE when made, and I verified that rather than
assuming it:

```
$ git merge-tree --write-tree HEAD 3cff7733   (trunk at "census r22 built dbc90692")  → exit 0, 0 conflicts
$ git merge-tree --write-tree HEAD 2c980fc3   (trunk one fetch ago)                   → exit 0, 0 conflicts
$ git merge-tree --write-tree HEAD b1eb6767                                           → exit 0, 0 conflicts
```

`git reflog show origin/MCP/main` records `add7aba8 … fetch origin --quiet: fast-forward` as my
own fetch during this review. What landed in between is the temp-dir hygiene ratchet
(`d0b4e1ca`, `816d12cd`, `5a6e7c81`), which edits the same `Makefile` header and `.PHONY` line
and the same expected-spec-doc vectors in `mcp_intent_contract_test.clj` that this branch also
edits. **Both conflicts are union resolutions of the kind this branch's own merge commit already
did twelve times: keep both sides.** No semantic disagreement — trunk adds `SELF_TEST_TMP` and
`tmp-leak-ratchet-self-test`, the branch adds `census-battery`; trunk adds the
`temp-dir-hygiene` spec doc, the branch adds `relation-census`.

**Ruling: this tip is not merge-ready AS IT STANDS**, and the fix is one merge of current trunk
plus a union resolution of two files, followed by a re-run of `mcp-test` and the intent audit
(both files are gate-bearing). Nothing about the branch's content is at fault.

---

## 12. Cleanup

Every fixture lived under `/var/tmp/forge/census23-review-fx/opus`, which did not exist when I
arrived (`ls: cannot access …: No such file or directory`) and does not exist now:

```
$ stat -c '%a %n' $FX/noexec           # chmod restoration, before removal
755 /var/tmp/forge/census23-review-fx/opus/noexec

$ rm -rf /var/tmp/forge/census23-review-fx
$ ls -la /var/tmp/forge/census23-review-fx
ls: cannot access '/var/tmp/forge/census23-review-fx': No such file or directory
$ ls -d /var/tmp/forge/census23-review-fx*
ls: cannot access '/var/tmp/forge/census23-review-fx*': No such file or directory
```

The only chmod I made (500, then 400, on a fixture directory I created) was restored to 755 and
verified before removal. Sabotage touched only `git archive` exports under that directory; the
clone was never edited, committed, stashed, or `git add`ed:

```
$ git rev-parse HEAD && git status --porcelain && git stash list | wc -l
dbc9069226463f61ed7a793e393d2a63331865ca
                     ← porcelain empty
0                    ← no stashes
```

The only remote operation was one `git fetch origin`, required for the dry run; it advanced
remote-tracking refs only and is the cause of §11. No port in the forbidden list was contacted;
no server was started. `/tmp` was never used.

---

## Verdict

**## GO-WITH-FIX**

Round twenty-one's BLOCKING finding is genuinely closed, and closed the right way — not just at
the instance the reviewer found, but at all three build files, at both real launchers, with a
parsed class ratchet whose allow-list is empty and separately asserted empty, riding a named
lane that I watched execute it. All three corrected sabotage receipts reproduce to the figure
(3 failures, ~51.7 KB at both launchers, exactly 7 failures), which is the round's real
achievement: round twenty's withdrawn receipt is replaced by three that a stranger can run. The
disclosed depth gap is real and takes precisely the typed, bounded `Throwable` exit the round
built for it. Every gate matches its claimed count.

The fix required before this can be called done, in priority order:

1. **Finding 3 — fence the build file's `:paths`.** A `deps.edn` under a caller-named `:dir`
   still directs `:op :ls-tree` to enumerate and print an arbitrary absolute tree (80 files,
   2,459 forms, every `def` name), exit 0, at both launchers. Validate the entries as strings,
   resolve-then-fence them against the project root, and refuse an escaping entry as a typed,
   counted refusal. This is the round-21 finding with the payload changed from code to
   configuration, and the round closed the reader half only. I do not block on it because it is
   **byte-identical on trunk** and confined to `:ls-tree`, not the census verb — refusing the
   merge would leave `read-string` on trunk on the identical vector, which is strictly worse.
2. **Finding 4 — correct the oracle in the same fix.** `evaluating-reader-names` is
   `#{"read-string" "load-string"}`; `clojure.core/read`, `read+string`, `load-reader`,
   `load-file` and `load` are not in it. No present violation, so the allow-list is honestly
   empty either way — but the ratchet would not notice tomorrow.
3. **Finding 11 — merge current trunk and re-resolve two files**, then re-run `mcp-test` and the
   intent audit, both of which are gate-bearing on the conflicted files.

**## GO-WITH-FIX**

**Mergeability, one sentence:** against trunk `add7aba8ef07ebe894cb43f54becbd26c937077d` this tip
is NOT mergeable as it stands — `git merge-tree --write-tree HEAD origin/MCP/main` exits 1 with
content conflicts in `Makefile` and `test/clj_surgeon/mcp_intent_contract_test.clj`, both purely
mechanical union resolutions created by the temp-dir hygiene ratchet landing on trunk during this
review (the same dry run is clean against `3cff7733`, the trunk the builder built on), so the
branch is GO on its own content and needs only a trunk merge and a re-run of the two gates those
files bear.
