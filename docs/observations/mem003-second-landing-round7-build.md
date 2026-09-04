# MEM-003 second landing — round seven build record

Branch `bridge/integration-2026-09-03-mem003`, built on `432268cf` after the
round-six independent review returned **NO-GO** with four blocking findings and
two non-blocking ones, committed alongside as
`mem003-second-landing-round6-review-opus.md`.

The round-six diagnosis is one sentence and it is the reviewer's:

> Every alternative in both derived patterns is a literal source token, and a
> reflective call site spells the token as a string argument instead. **A
> derivation over names cannot see a call that names nothing.**

Five ordinary routes published a clock-derived number in an undeclared receipt
field inside the hashed parity subject with twenty-four tests and one hundred
twenty-six assertions green.

## The requirement first

Amended before any witness was written, because a witness implementing the old
requirement faithfully still passes all five attacks. The old text enumerated
ROUTES — "a public var, a private var reached by var-quote, `ns-resolve` … the
protocol method reached as Java interop" — and every route in that list is a
call that SPELLS its target as a source token.

- **MCP-OP-TIME-005** now says a clock read is an offence whatever spelling
  names it, INCLUDING a call naming its target as a STRING or by POSITION, and
  names the three faces the scan must carry.
- **MCP-OP-TIME-006** makes the string/positional call an offence of the same
  class as the interop call, with the reviewer's three plants written into the
  requirement, and requires each derived name in three forms.
- **MCP-OP-TIME-007** requires the clock derivation to carry the three
  spellings plus each source class's fully-qualified name as a string, and
  requires the babashka floor to be the COMPLETE difference against a manifest
  derived where reflection is complete.

Commit `6bd223e3`. Intent audit after: 371 specs, 0 violations.

## Blocking 1–3 — one fix

Both alternative builders now emit three forms per derived name.

**Escape hatch.** A new `reflective-member-names` derives the measured
namespace's JAVA surface — each protocol method as written and as the JVM
munges it, plus each declared field of the opaque types — which is exactly the
set `.getMethod`, `.getDeclaredField` and `Reflector/invokeInstanceMethod`
accept as a string. Each is emitted as `"name"` and as `(. name` alongside the
token spellings. `escape-hatch-alternative` dispatches on shape: a token keeps
its literal quote plus the longer-identifier lookahead; a quoted string is
matched literally WITH its quotes; a dot special form carries the member only,
admitting a bare receiver or a single parenthesised one.

**Clock.** `derived-clock-expressions` emits the string form of every clock
method name and the fully-qualified name of every clock source class —
`Class/forName` cannot be reached without one. `clock-expression-alternative`
gains the string shape and an OPTIONAL FULLY-QUALIFIED PREFIX on the dot form:
the slash form's docstring already promised that a fully-qualified call matches
and the dot form had lost the property.

RED `99082fd6` (5 failures) → GREEN `16573f64`.

### Two narrowings, argued rather than convenient

`measured/<var>` spellings get NO string form. A string cannot name a Clojure
var by itself; it needs `resolve` / `ns-resolve` / `find-var` /
`requiring-resolve` / `intern`, every one already an offence under the naming
rule's `:reflective` clause, which
`the-require-witness-catches-a-planted-reflective-resolution` proves by planting
one. Giving `"value"` an alternative would instead have cost the two JSON schema
keys literally named `value` an allow-list entry apiece, and an allow-list
padded with entries that are not clock routes is one nobody reads carefully.

A clock METHOD name gets a string form only when it carries a clock morpheme.
`Instant/from` as the four characters `"from"` is a JSON schema key in six
files. A morpheme-free name stays closed at the TIGHTER end of the reflective
route: `Class/forName` cannot reach `Calendar/getInstance` without
`"java.util.Calendar"`, which the class alternative carries and which collides
with nothing. Measured over both scanned roots, the 68 surviving string
spellings collide with exactly one site in the tree — `txn_journal/evidence-stat`
line 904, `(get attrs "lastModifiedTime")`, a real file-mtime read named by
string and already `:control`.

### The sabotage found the fix half unwitnessed

Sabotaging on `git archive 16573f64` copies:

| sabotage | result |
|---|---|
| S1 `escape-hatch-alternative` back to ONE shape | 4 failures |
| S2 clock dot form loses the fully-qualified prefix | **0 failures — GREEN** |
| S3 clock derivation stops emitting the string forms | 1 failure (derivation witness only) |

S2 is the finding. Plants D and K both name `java.lang.System`, which the FLOOR
carries by name, so each was caught by a literal floor entry rather than by the
property the fix claims. Two new plants close it — **D2**
(`Class/forName "java.util.Calendar"` + `"getInstance"`) and **K2**
(`(. java.time.Instant now)`) — naming classes the floor does not. Re-run:
S2b 1 failure naming K2; S3b 2 failures naming D2. Commit `1fda0091`.

**The general form, worth keeping:** a FLOOR entry and a DERIVED property can
catch the same plant, and when they do the plant witnesses the floor, not the
property. A plant meant to witness a derivation has to name something the floor
does not.

## Blocking 4 — the floor is now checked, and it was short again

`test/fixtures/clock-spellings-jvm.edn` holds the derivation's output on the
JVM; `make clock-spellings-manifest` regenerates it;
`the-babashka-clock-floor-is-the-complete-jvm-difference` compares in BOTH
directions — every spelling the manifest holds and this runtime cannot derive
must be a floor entry, and on a complete runtime nothing may be derived that
the manifest lacks (a stale manifest makes every floor conclusion unfounded).

On its FIRST RUN the ratchet found a live gap: the difference is four, not
three, and `"getTimeInMillis"` — the string form this round added — was not in
the floor. The hand-maintained floor had gone stale again inside this round,
one commit after being corrected by hand.

RED `b58165c1` → GREEN `53b816f6`. Sabotage `S4` (the `(. Calendar getInstance`
entry removed) reproduces the reviewer's finding 4 exactly, naming that
spelling.

The make target uses a `-Sdeps` alias rather than `:clj-surgeon/mcp-test`,
because that alias pins `:main-opts` to the suite runner so `-e` and `-i` are
ignored — the same wall the round-six reviewer hit trying to isolate
`admit-patch-test`, written into the target rather than into somebody's memory.

## Non-blocking 5 — the walker

An ARRAY is WALKED (`.isArray`, walking `(seq node)`); an ITERATOR is REFUSED
outright, because it cannot be inspected without being CONSUMED and a walker
that diagnosed one would hand the boundary a verdict about a value it had just
destroyed. The witness asserts `.hasNext` still holds after the refusal.

The review rated this non-blocking and was right — a `Reading` cannot be
encoded, so the number never reached the wire. It failed as an ENCODER STACK
TRACE rather than as the typed refusal that names the path, which is the entire
value of the diagnostic.

RED `b5022537` (3 failures) → GREEN `3e9467d9`.

## Non-blocking 6 — counts count CALLS

`sites` conjes one hit per MATCH, `(count (re-seq pattern code))`. Two declared
counts move, both `:control`:

- `worktree_lifecycle/valid-future-expiry?` 1 → 2 — the reviewer's own site,
  two `Instant/parse` reads on one line;
- `txn_journal/evidence-stat` 2 → 3 — and NOT for the same reason: the third is
  the java.nio attribute name `"lastModifiedTime"`, which became a clock
  spelling only in this round's string-form derivation.

That second entry is the round's best evidence that the two fixes were not
independent: a new spelling and a per-line ceiling of one compound, and either
alone would have hidden the other.

RED `8b4db1ff` (3 failures) → GREEN `e36368ab`.

## Wording

The `setAccessible` residual now reads "reflection over the type's fields BY
ANY ROUTE, NAMED OR POSITIONAL" at both sites — the reviewer's plant F reached
the field positionally and the old wording did not cover it. The
`make memory-red` default mode is documented at the target: the bare target
correctly FAILS since the admission fix landed, and the gate is
`PARSER_RED_EXPECT=green`. The default is deliberately NOT flipped — a red
witness whose default stops asking its own question has quietly become an
assertion. Commit `d93736aa`.

## A correction to this round's own record

`git status --porcelain` at the end of the round was non-empty on a file I
believed committed nine commits earlier. `git commit` after a conflicted merge
commits the INDEX, and two fixes made after the `git add` — the
`refusal-fact-line` clock allow-list entry and the enumeration re-pin at 145 —
were never staged. So **the merge commit `638b4169` does not carry them**, its
message quotes suite figures whose subject is my working tree rather than the
commit, and every commit from `638b4169` to `d93736aa` has a red JVM suite.
Corrected at `ec143202`, which is the first tree since the merge that can carry
its own gate figures. The fixes themselves are unchanged and the reasoning at
the pin is the reasoning that was reviewed; what was wrong is which tree could
be said to prove it.

The ratchet is a habit rather than a test: `git status --porcelain` is part of
running a gate, not part of tidying up afterwards. House rule 20 — a receipt
must name its subject — broken in the most ordinary way available.

## Derivation counts (babashka, the runtime the scanning gate runs in)

| | round six | round seven |
|---|---|---|
| escape-hatch derived | 8 | 12 |
| escape-hatch union with the floor | 11 | 17 |
| clock derived | 159 | 227 |
| clock union with the floor | 161 | 231 |
| clock derived on the JVM (manifest) | — | 231 |
| JVM-minus-babashka difference | 3 carried, 1 uncarried | 4, all carried, checked |

## Gates, at `ec143202`, `git status --porcelain` empty

| gate | command | result |
|---|---|---|
| babashka suite (the scanning gate) | `~/bin/suite-run bb test/run_all.clj` | `Ran 918 tests containing 7234 assertions. 0 failures, 0 errors.` |
| JVM suite | `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 770 tests containing 10383 assertions. 0 failures, 0 errors.` |
| operation oracle | `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` exit 0 |
| intent audit | `clojure -M -e "(clj-surgeon.mcp-intent-contract/audit-current-repository)"` | `:specs 371 :violations 0 :ok true` |
| txn kernel warnings | `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` |
| battery self-test | `make memory-battery-self-test` | `Ran 32 tests containing 171 assertions. 0 failures, 0 errors.` |
| parser-admission red witness, GREEN mode | `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` |
| transaction-kernel memory witness | `make memory-red-kernel` | `Ran 4 tests containing 25 assertions. 0 failures, 0 errors.` — `heap-used-peak-mb 253.58` at `xmx-mb 256.0` |
| temp-dir hygiene ratchet | `make tmp-leak-ratchet-self-test` | `tmp-leak ratchet witness passed` |
| admit analyzer memory | `make admit-analyzer-memory-self-test` | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` |

### The memory battery, ONCE, at the tip

Under `flock /home/forge/tmp/suite.lock`, fresh `MEMBAT_ROOT=/home/forge/tmp/membat-r7`,
reference built explicitly first, never `MEMBAT_ALLOW_ANY_ROOT`.

```
:attestation {:ops [:cli-ls-tree :workspace-sources-read-all
                    :rename-ns-plan-narrow :rename-ns-plan-full-match],
              :jvm "21.0.12",
              :head-sha "ec1432022bccc86074e1f19ded0070478da8f2e5"}

verdict: FAIL (INCOMPLETE)   exit 1
  FAIL held-scales-with-n {:op :rename-ns-plan-full-match, :observed 9.8, :limit 3.0}
  FAIL held-scales-with-n {:op :workspace-sources-read-all, :observed 40.8, :limit 6.6}
  … 9 TREND lines …
  4 × UNMEASURED reserved-peak-over-budget

cells 48 · reference-mismatch cells 0 · distinct :reference-mismatch (nil) · tool-errors []
receipt: /home/forge/tmp/membat-r7/receipts/20260904T145632.292915564Z-battery.edn
```

Identical in kind to rounds four, five and six: the two `held-scales-with-n`
lines and the four `UNMEASURED` reserved peaks are MEM-001's lane, pre-existing
at the base and not this branch's to close. Zero reference mismatches.

## Trunk

Merged at `638b4169` — see the CORRECTION section above for what that commit
does and does not carry. Resolutions, per file:

- `src/clj_surgeon/mcp_server.clj` — the adapter's catch. KEPT BOTH: this
  branch's clock plus `mcp-operation/finalize-failure` (so the last-resort
  receipt carries a `measured` block) and the trunk's `adapter-failure` plus
  `summarize` (so its text face is rendered, not raw JSON). One boundary, and
  one face.
- `src/clj_surgeon/mcp_tool.clj` — `alias-migration-refusal-envelope-keys`. The
  trunk's narrowed set (only the keys the renderer really renders) with
  `measured` in place of `elapsed_ms`.
- `test/clj_surgeon/mcp_alias_migration_test.clj` — the same key set plus the
  trunk's ~960 lines of file-scoped refusal enumeration; the trunk side whole,
  same one-key substitution.

**The refusal enumeration, re-verified on the merged tree rather than
inherited:** the trunk pinned 143 and the branch pinned 143, and neither is
right for the composition — the merged entrance enumerates **145**. The two
additions are this branch's own typed refusals, reachable from the
alias_migration entrance because every entrance now finalizes through
`mcp-operation/finalize-result`: `invalid-measured-start` (measured.clj:259) and
`unpartitioned-measured-field` (mcp_operation.clj:60). Re-pinned in both the
count and the frozen set, with the reason at the pin.

One further merge consequence the scanning gate caught rather than a human: the
trunk's new `mcp-tool/refusal-fact-line` takes two raw `System/currentTimeMillis`
reads for its one print deadline, so the trunk's form arrived undeclared. Added
`:control` with its reason.

`git merge-tree --write-tree HEAD origin/MCP/main` against trunk `c2c19691`:
**clean, exit 0.**
