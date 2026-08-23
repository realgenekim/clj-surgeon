# Captain's Log: Trust Became the Release Gate

**Date:** 2026-08-10

## Bottom line

The vision is converging. The remaining work is no longer “invent a better
editor.” It is to make every public layer tell the same truth: advertised,
accepted, executed, verified, and reversible must be one contract.

## The field failures

Two production reports exposed that boundary.

First, `:extract!` moved several adjacent forms from a valid 4,541-line
namespace and corrupted the source. The old executor reread the file for every
removal, reused original line numbers after each mutation, and always deleted
one extra line as if it were blank. When the next unrelated `defn` followed
immediately, Surgeon deleted its opening line. The result contained an orphaned
docstring, argument vector, and body. The target had already been created, the
source no longer parsed, and no inverse receipt existed.

Second, `apply_clojure_changes` advertised a top-level `verify` field while its
direct-change validator rejected that field. Four clean benchmark callers all
made the same reasonable first request, paid a refusal and about 20 seconds of
model recovery, then rebuilt the call. That was not user error. The generated
schema and executable contract disagreed.

## The enduring corrections

Extraction now compiles every removal against one immutable source snapshot.
It removes a trailing line only when that line is blank, adds the require
through rewrite-clj, parses both complete candidates before writing, refuses an
existing target or stale source, writes atomically, and verifies both read-back
files. A handled partial failure restores the original source and removes only
the exact target created by the transaction.

An optional `:receipt-out` is published last. `:undo-extract!` restores the
source and removes the target only while both still match the recorded result
hashes. A second undo or a drifted result refuses before mutation.

The MCP direct route now accepts the same `verify: fast|full` field that its
schema advertises. It runs the closed workspace profile after commit. Failed
verification applies the durable inverse and reports whether rollback
completed. Successful terminal evidence includes the verification result.

## Tests that drew blood

The minimized adjacent-form test failed on the old implementation for the
same reason as the field incident: `event-resume-path` lost its opener and the
complete namespace failed to parse.

The stronger fixture contains fifteen adjacent early forms followed by a
documented unrelated `defn` with no sacrificial blank line. It proves both
future namespaces parse, all fifteen forms arrive in the target, the complete
neighbor remains byte-for-byte, and the guarded inverse restores the exact
original source.

Boundary tests also cover an existing target, a stale source after planning, a
simulated second-write failure, receipt round-trip, repeated undo, and target
drift. The MCP tests cover the closed verification enum, successful profile
execution, and verification-failure rollback across the real multi-file
fixture.

The final gates were concrete:

- clj-kondo: 0 errors, 0 warnings on every changed Clojure namespace;
- main suite: 616 tests, 5,312 assertions, 0 failures;
- MCP suite: 131 tests, 1,079 assertions, 0 failures;
- full `make test`, including stdio smoke and benchmark harness self-tests:
  passed;
- copied-fixture CLI dogfood: fifteen forms extracted, both files parsed and
  read back, then `:undo-extract!` restored source SHA-256
  `f6e048cb16f5f452c89a116ad9f9e9a4a51b044b1d15e490725bf190a1fa4e89`
  and proved the target absent.

The live hot-server proof exercised both branches of direct `verify`. An
unformatted candidate refused, named the exact `npx` check and exit code,
returned bounded formatter output, and confirmed rollback. After formatting,
the same request committed and ran clj-kondo plus Standard Clojure Style in
1.44 seconds. The published terminal receipt included the profile and both
successful checks. The probe was then undone from its receipt.

## Caller ethnography: safe, but leaving capability unused

A field agent summarized its route as MCP for atomic writes, CLI for `cat`,
`ls`, `match-form`, and extraction evidence, then native patching for new files
and whole-form deletion. That model was safely conservative for the broken
installed extraction build, but it understated the current product:

- `inspect_clojure` is the hot entrance for batched forms, outlines, matches,
  and X-rays. CLI is the fallback or the home of operations not yet exposed by
  MCP, such as dependency/extraction analysis and `fix-declares`.
- prepared bases already support whole-owner `delete=true`; deletion does not
  inherently require native patching;
- native patching remains the control for prose, JavaScript, new arbitrary
  files, one unsupported text edit, or a structural operation outside the
  bounded MCP contract;
- avoiding the installed `:extract!` was correct until this repaired build
  passes its release and installation gates.

This is a product finding, not merely documentation cleanup. If a careful
caller systematically chooses the slower route, the tool has failed to make
its comparative advantage obvious.

## A private Var exposed one real proof gap

The first diagnosis was wrong. The generic structural matcher already gives
`#'views/log-summary` and `(var views/log-summary)` one identity and preserves
the reader spelling as exact evidence. The field caller searched the alias-
qualified symbol, but the fixture deliberately used the fully qualified Var.
Those are different symbols without namespace resolution. `clj-surgeon-9vp`
was closed after reproducing the correct match and its string/comment
exclusions.

The safety defect was downstream. After the first caller was migrated, a
literal sweep found a second fully qualified quoted Var that fresh clj-kondo
resolved usages had omitted. The new proof layer now unions cclsp references
with lossless `#'name` and `(var name)` evidence. It resolves fully qualified,
namespace-aliased, and same-namespace names; excludes strings, comments,
quoted data, syntax-quoted data, and `#_` discards; deduplicates at the exact
owner; and labels structural evidence `:structural-var-quote` rather than
pretending it came from LSP.

The same proof now appears in two entrances:

- `inspect_clojure mode=prepare-change` includes authority on every surface
  site plus a bounded `quoted_var_proof` summary;
- CLI `:extract` includes exact `quoted-var-references` before `:extract!` can
  write.

The field-shaped integration deliberately removes both quoted callers from the
semantic result. One prepare call still returns the ordinary caller and both
private-Var owners. Text, comments, and quoted data stay absent. On this
repository the hot supplement scans 167 Clojure files in about 320–380 ms,
roughly one percent of the measured 28-second one-shot agent turn.

## Dogfood: delimiter repair moved a binding boundary

While wiring the proof union, a native patch plus the automatic parenthesis
repair hook made the file parse by moving several intended `let` bindings into
expression position. The first namespace compile rejected an out-of-scope
symbol. No tests or release operation ran on that state.

The recovery was revealing: stop stacking punctuation repairs, restore the
complete named `prepare-change!` form from its pre-edit snapshot, and reapply
the smaller decision at a narrower seam. The dedicated pure recognizer was
pleasant to build and test. The large nested Clojure control form was not. This
is the exact experiential case for compiled structural decisions: the model
should decide the new binding structure once; the editor should own balanced
materialization and prove the resulting namespace compiles.

## Assessment

| Dimension | Assessment |
|---|---:|
| Vision | 9 / 10 |
| Structural transaction design | 8.5 / 10 |
| Current end-to-end reliability before these fixes | 7.5 / 10 |
| Probability of becoming genuinely beloved | high |

The 2.46× clean one-shot MCP win proves that compiled intent can beat native
patching. The failures show the condition: the first request must work, every
refusal must teach, and every successful compound write must be boringly
trustworthy.

The release gate is therefore coherence, not feature count. A layer may expose
less capability, but it must never advertise a field it rejects, report ready
when the next real call cannot run, or claim success before every owned byte is
parseable, hash-verified, and recoverable.

## Extraction integrity had a semantic hole

The failure-atomic extraction kernel preserved the filesystem while producing
the wrong program. It copied all 17 source requires into the destination,
appended a destination alias that collided with an existing `schedule` alias,
and added that require even though no source form called a moved Var. Parsing,
atomic write, and read-back verification all passed. Those checks proved that
the bytes were intact. They did not prove that the namespace header was right.

The red production fixture made the missing contract exact: destination
dependencies must come from the moved forms, and the source must require the
destination only for remaining callers. The first change was therefore not a
patch to the mutation shell. It was a pure namespace-header compiler. That
Kent Beck move made the hard decision cheap to test before reconnecting it to
effects.

The compiler uses free-symbol evidence instead of raw text. It retains aliases,
qualified namespaces, referred symbols, rename targets, and namespaced keyword
aliases. It refuses reader conditionals, prefix libspecs, comments in require
clauses, and dependencies whose side effects cannot be proved safe to remove.
When callers remain, it emits a deterministic collision-free alias plus a
sorted `:refer` list. When none remain, it leaves the source header alone.

An installed-CLI proof moved 15 adjacent forms. The target retained only
`clojure.string`; the source gained no destination require; both candidates
parsed and read back; and `undo-extract!` restored the exact source and removed
the target. The proof also found a path-identity papercut: `/var` and
`/private/var` made the source look like its own external caller. Canonical
paths now remove that false positive.

Final gates passed 630 main tests with 5,396 assertions and 131 MCP tests with
1,079 assertions. clj-kondo reported zero errors and warnings. The shared MCP
reloaded without restart, and its first functional request resolved the new
compiler in 46 ms.

The Rich Hickey lesson is the same as the release lesson: separate stable facts
and decisions from time and effects. The extraction shell now discovers,
hashes, commits, rolls back, and publishes receipts. The header compiler only
turns structural evidence into an immutable decision. Transaction integrity is
necessary; semantic candidate integrity is the actual release gate.

## The next coherence refactor is a pipeline, not a file split

The overnight review of another large Clojure system showed the value of
characterization tests, architecture guards, and modules with one owner. The
transferable lesson is not to split this repository's largest files by line
count. It is to expose the stable information flow already hidden inside them:

```text
schema and facts -> pure decision compiler -> candidate verifier
                 -> atomic executor -> inverse receipt
```

`extract-header` is the first proved seam. The next candidate is the large
intent-transaction namespace. Keep its public facade stable, extract one pure
stage at a time behind characterization tests, and add dependency guards that
prevent the compiler from importing filesystem, MCP, subprocess, or receipt
publication code. Make the structure easier to change before changing more
behavior.
