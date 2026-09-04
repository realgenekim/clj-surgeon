# MEM-003 second landing — round eight build record

*Written 2026-09-04T18:35:39Z by forge-anvil on `bridge/integration-2026-09-03-mem003`.
Subject: the round-eight Opus review (`docs/observations/mem003-second-landing-round8-review-opus.md`),
its one blocking finding, its two non-blocking ones, and the trunk merge that followed.*

## Headline

All three review findings are closed. The merge with `origin/MCP/main` is NOT clean in
substance even though it is clean in `git`: **the MEM-003 landing and the relation-census
landing are incompatible as they stand, in four distinct ways, and the incompatibility is
attributed rather than suspected** — each side's suite is green alone and their composition
is red.

| tree | JVM suite | verdict |
|---|---|---|
| `origin/MCP/main` alone (`a8c800a0`) | `Ran 865 tests containing 13023 assertions. 0 failures, 0 errors.` | green |
| this branch pre-merge (`753b6b2c`) | `Ran 770 tests containing 10383 assertions. 0 failures, 0 errors.` | green |
| the composition (`276931c1`) | `Ran 870 tests containing 13082 assertions. 11 failures, 3 errors.` | RED |

Not one of the fourteen is in the measured lane's own witnesses. Every one is a place where
a requirement this branch introduces meets a receipt the census lane already ships.

## The three review findings

### 1 — BLOCKING, closed. The dot special form's parenthesised member

Clojure's `.` special form has TWO member spellings and the derivation emitted only the first:

```
(. instance-expr member-symbol)
(. instance-expr (method-symbol args*))     ;; <- no alternative matched this
```

`..` and `memfn` are macros in `clojure.core` whose entire job is to emit the second, so this
is the sugar a developer reaches for, not an exotic spelling a reviewer invented.

**RED (`1c9685d8`)** — five plants added as data, and reproduced IN THE PRODUCTION PATH as one
line at `src/clj_surgeon/mcp_hot_verify.clj:114` on `git archive` copies of `a2a15cc0`, each
publishing into an undeclared receipt field inside the hashed parity subject:

```
base  {:test 27, :pass 156, :fail 0, :error 0, :type :summary}
N1    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}   (. System (nanoTime))
N2    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}   (.. System (nanoTime))
N3    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}   (. rr (_launder))
N4    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}   ((memfn _launder) rr)
N7    {:test 27, :pass 156, :fail 0, :error 0, :type :summary}   (.getMethod java.util.Calendar "getInstance" ...)
```

Five clock-derived numbers reaching a hashed parity subject with the gate at its exact baseline.
The plant data alone took the gate to `{:test 28, :pass 157, :fail 5}`.

**GREEN (`aff80f15`)** — the member position admits `\(?\s*`; `(.. recv member` and
`(memfn member` are two further faces per derived name, both derived, nothing listed. Floor
entries added include the three the floor witness NAMED on its first run after the grammar
widened:

```
the JVM derives clock spellings this runtime cannot, and the floor does not carry them, so
the scan is STRICTLY WEAKER here than on the JVM for:
["(.. Calendar getInstance" "(memfn getInstance" "(memfn getTimeInMillis"]
```

Re-planted at the same production site on copies of the FIXED tree, N1–N4 go red, two failures
apiece over the baseline, naming `["src/clj_surgeon/mcp_hot_verify.clj" "verify!"]`.

**N5 is recorded as OUTSIDE the route set, with a witness rather than a comment.**
`(let [c System] (. c nanoTime))` does not compile — `System` evaluates to a `java.lang.Class`
object and the form is then an instance member access on it. A runtime that started accepting
it would reopen the route silently, so `the-round-eight-N5-route-does-not-compile` asserts it.

### 2 — non-blocking, closed. A clock class as a BARE SOURCE SYMBOL (`b4186940`)

The morpheme narrowing assumed `Class/forName` is the only route to a class. On the JVM a class
is an ordinary source symbol. The emission is stated as a RULE, not a list: a class gets the bare
form exactly when it carries a clock member the morpheme narrowing withholds a string form from
(a static returning a time value whose name has no clock morpheme).

Two exclusions, each MEASURED before it was written:

| exclusion | why | cost if omitted |
|---|---|---|
| `(?![/.$])` — not `Class/member` call position | `(java.nio.file.Files/exists p)` is an ordinary qualified call the slash spelling already classifies | **55 new sites** |
| `^` in the lookbehind — not a type hint | `^java.io.File` is a compiler instruction and reads no clock | **40 more lines** |

Whole remaining cost on this tree: **two sites**, neither a read — both carry the literal text
`"(:import java.time.Instant)"` in a replay fixture's edit payload. They are NAMED in the
allow-list rather than excluded, and `clock-allow-list`'s docstring now names this THIRD kind
of entry explicitly (`NOT A READ` in its `:why`), because a source-text scan cannot tell a
string literal from source and the honest form of that limit is an entry a reader can see.

### 3 — wording, closed. Bound what a text scan can promise (`753b6b2c`)

`MCP-OP-TIME-005`'s final clause promised to catch "any other route that spells no class name
and no method name the scan can read". Nothing scanning source text can do that, and a
requirement a scan provably cannot satisfy reads as green forever. The requirement now states
its own bound and defers to the residual; the residual in `src/clj_surgeon/measured.clj` is
widened from *fields* to *any member of the type reached by a computed name*, and says why.

**AND THE RESIDUAL IS NOW CHECKED**, which is the ratchet rather than the prose:
`the-computed-member-name-residual-is-declared-at-its-real-width` plants N6 and the
computed-namespace shape and asserts the scan does NOT see them. If a future round closes one
of these routes the test goes red and names the correction, instead of leaving an overclaim
nobody re-read. A declared hole that nothing checks is indistinguishable from a hole nobody
noticed.

## Sabotage — fail-first on `git archive` copies of the tip

Baseline for all three is the tip's own `{:test 29, :pass 174, :fail 2}` (the two census
failures below).

| sabotage | result | what went red |
|---|---|---|
| **S1** clock parenthesised member collapsed to a bare token | `{:test 29, :pass 172, :fail 4}` | plants **N1** and **N2**, each naming its source verbatim |
| **S2** escape-hatch parenthesised member collapsed | `{:test 29, :pass 170, :fail 6}` | plant **N3**; `the-escape-hatch-pattern-carries-every-route-to-a-readings-number` naming `(.. -launder` and `(.. _launder`; `the-escape-hatch-scanner-catches-every-route-planted-in-a-receipt` ×2 |
| **S3** both collapsed | `{:test 29, :pass 168, :fail 8}` | the union |

`memfn` is a separate face from the dot branch, which is why S1 and S2 do not reach N4 — the
faces are independent by construction and the sabotage proves it.

## The merge, and the numbers re-derived ON IT

Two conflicts, both resolved by intent, neither a design collision: a require-list conflict in
`src/clj_surgeon/txn_journal.clj` (both requires kept) and the `frozen-refusal-kinds` docstring
sentence in `test/clj_surgeon/mcp_alias_migration_test.clj` (the SET body merged cleanly).

**Nothing was inherited.** Enumerated with `refusal-kinds-in-source` on each tree:

| | refusal kinds | pin set | extra in source | missing from source |
|---|---|---|---|---|
| branch tip `753b6b2c` | 145 | 145 | () | () |
| **the merge `276931c1`** | **147** | **147** | **()** | **()** |

Both parents pinned 145 and neither was right about this tree: the trunk's 145 was 143 + two
CENSUS kinds; this branch's 145 was 143 + two MEASURED-PARTITION kinds. Re-pinned at 147 with
the reason written at the pin.

Clock allow-list, re-derived the same way and **unchanged** across the merge: 43 entries,
19 `txn_journal.clj` entries, 24 declared reads there, 11 escape-hatch entries. The trunk's txn
round-eight sweep moved no clock read.

## THE LANDING BLOCKER: MEM-003 x relation-census, four incompatibilities

**Attribution, not suspicion — both parents were run, on fresh clones, and both are green.**
`origin/MCP/main` at `a8c800a0`: `Ran 865 tests containing 13023 assertions. 0 failures, 0
errors.` This branch pre-merge at `753b6b2c`: `Ran 770 tests containing 10383 assertions. 0
failures, 0 errors.` The composition at `276931c1`: `Ran 870 tests containing 13082
assertions. 11 failures, 3 errors.` Fourteen failures that exist in neither parent are a
property of the composition, and saying so required running both parents rather than assuming
which side owned them.

**(a) A published clock-derived field outside the measured block — 2 failures**

```
FAIL in (no-raw-clock-read-lives-outside-the-measured-namespace)
raw clock reads with no allow-list entry:
  (["src/clj_surgeon/mcp_relation_census.clj" "execute-in-context!"]
   ["src/clj_surgeon/relation_census.clj" "plan"])
```

`relation_census/plan` reads `System/nanoTime` three times and returns `:phases`;
`mcp_relation_census/execute-in-context!` reads it three more and publishes the result as the
top-level receipt field `:phases_elapsed_ms` through the shared `mcp-operation/invoke!`.
`MCP-OP-CENSUS-013` REQUIRES that field; `MCP-OP-TIME-005` FORBIDS a clock-derived field
outside a `measured` block. The clock allow-list cannot absorb it — it is `:control` only by
construction and its own witness refuses a `:receipt` entry, correctly.

**(b) The census tool's output schema has no measured partition — 2 failures**

```
FAIL in (exposes-exactly-seven-typed-tools) (mcp_server_test.clj:52)
the request clock is published inside the measured partition
expected: (= {:type "number", :minimum 0} (get-in output-schema [:properties "measured" :properties "elapsed_ms"]))
  actual: (not (= {:type "number", :minimum 0} nil))

FAIL in (exposes-exactly-seven-typed-tools) (mcp_server_test.clj:56)
expected: (some #{"measured"} (:required output-schema))
  actual: (not (some #{"measured"} ["ok" "operation" "elapsed_ms"]))
```

`MCP-OP-SCHEMA-001` requires a `measured` object carrying `elapsed_ms` in EVERY canonical
tool's output schema. The census tool declares `elapsed_ms` at top level — the exact shape
MEM-003 replaces. It landed before the requirement existed.

**(c) Census tests construct results the measured finalizer refuses — 3 errors**

```
ERROR in (every-continuation-either-entrance-emits-fits-the-byte-ceiling) (mcp_operation.clj:19)
ERROR in (no-refusal-names-the-workspace-root-in-its-prose)              (mcp_operation.clj:19)
ERROR in (the-constructors-are-the-only-continuation-construction-sites) (mcp_operation.clj:19)
  actual: clojure.lang.ExceptionInfo: MCP elapsed time must be finite and non-negative
```

The measured landing's typed refusal firing on census test fixtures that do not supply a valid
measured start. This is the ratchet working; the fixtures need the new constructor.

**(d) Two further shapes**

```
FAIL in (every-declared-refusal-shape-carries-no-field-over-the-ceiling) (mcp_relation_census_test.clj:6416)
  the drives still cover every refusal the tool declares
FAIL in (no-real-launcher-follows-a-build-file-path-out-of-the-tree) (reader_eval_fence_test.clj:200)  x8
  the refusal did not name the entry the caller spelled — stdout "── total: 0 files, 0 forms\n\n"
```

The first is the census refusal-type set meeting the measured lane's typed refusals. The
eight in `reader_eval_fence_test` are a trunk test driving the real launcher as a subprocess
and finding its refusal prose changed; that one is NOT obviously measured-lane territory and
wants a look before it is assumed to be.

## What I did NOT do, and why

**I did not resolve (a)–(d).** Every resolution changes a published wire shape or a shipped
requirement:

- **(i)** the census reads its phases through `measured/start`/`elapsed-ms` and publishes them
  INSIDE the measured block. This is what `MCP-OP-TIME-004` already prescribes for a narrower
  internal phase, so it is the intent-preserving answer — but it moves `phases_elapsed_ms`
  under `measured`, touching the census output schema, ~10 assertions in
  `mcp_relation_census_test.clj`, and the text of `MCP-OP-CENSUS-011` and `MCP-OP-CENSUS-013`,
  and it must be paired with the schema fix in (b) and the fixture fix in (c).
- **(ii)** `MCP-OP-TIME-005` carves out an exception for phase timings. This weakens the
  invariant the whole landing exists to establish, and a gate relaxed to admit the first thing
  it caught is not a gate.
- **(iii)** the census keeps the field and launders a reading to fill it. `MCP-OP-TIME-006`
  forbids exactly that. Listed only so the option set is complete.

**(i) is the right answer and it is the census lane's call**, on files that lane is actively
landing. Deciding it inside a merge commit would be one seat rewriting another lane's published
contract. Routed rather than decided, per the standing rule that a design call is not the
integrator's to make alone.

Neither lane is at fault. The census landed in good faith before this ratchet existed, and the
ratchet did precisely its job: it found a published clock-derived field, a schema without a
measured partition, and three fixtures that skip the finalizer's precondition, the moment the
two lanes met — and it named every site.

## Gates, on a FRESH `git clone` at `276931c1`

`git status --porcelain` empty, HEAD verified.

| gate | result |
|---|---|
| JVM suite, run 1 | `Ran 870 tests containing 13082 assertions. 11 failures, 3 errors.` — **RED, all composition** |
| JVM suite, run 2 | `Ran 870 tests containing 13082 assertions. 11 failures, 3 errors.` — identical, same fourteen, deterministic |
| babashka suite | `Ran 935 tests containing 7403 assertions. 2 failures, 0 errors.` — the two are (a) |
| operation oracle | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` |
| intent audit | `:ok true :specs 410 :violations 0` |
| txn kernel warnings | `kernel warning check: 2 namespace(s), 0 warning(s)` |
| `git merge-tree --write-tree HEAD origin/MCP/main` | **exit 0, clean** (checked against `cc9544c4`) |

| `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` |
| tmp-leak ratchet | `tmp-leak ratchet witness passed` |
| admit-analyzer memory self-test | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` |
| battery self-test | `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` |

**The full memory battery, ONCE, under `flock /home/forge/tmp/suite.lock`, a fresh
`MEMBAT_ROOT=/home/forge/tmp/membat-r8`, reference built explicitly first, never
`MEMBAT_ALLOW_ANY_ROOT`:**

```text
verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :profile :default, :observed 9.8, :limit 3.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :profile :default, :observed 40.8, :limit 6.5}
  … 9 TREND lines …
  UNMEASURED reserved-peak-over-budget ×4
receipt: /home/forge/tmp/membat-r8/receipts/20260904T190931.688652498Z-battery.edn
```

Parity read from the receipt rather than from the console:

```text
cells: 48
distinct :reference-mismatch: (nil)
reference-mismatch cells: 0
tool-errors: []
:attestation :jvm "21.0.12" :head-sha "276931c19a2c06dae45c991260a6de53beb83a70"
```

Exactly the state the round-eight review recorded at `a2a15cc0`: `FAIL (INCOMPLETE)` exit 1,
48 cells, ZERO reference mismatches, the two known `held-scales-with-n`, four `UNMEASURED`.
All four are MEM-001's lane, pre-existing at the base and unchanged in kind by this branch or
by the merge. The attestation names `276931c1`, the merge commit; `276931c1..294d8a46` is a
single docs-only commit (this file), so the battery attests the merged tree's code exactly.

**Fixtures** lived only under `/var/tmp/forge/mem003r8-fx` and are removed. No server was
started on any port; none of 7888 / 7890 / 7894 / 7895 was contacted. Nothing was pushed to
`main`; the trunk was merged INTO this branch and never the reverse.
