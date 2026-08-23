# Captain's Log: The scalpel was fast; the refactor was not compiled

<!-- agent-usage-window-end: 2026-08-10T17:16:01Z -->

**Window:** 2026-08-10 15:16:01–17:16:01 UTC; 08:16:01–10:16:01 PDT

## Question

A live coding team was dismantling a large Clojure server namespace into
coherent handler families. The work was architecturally successful and heavily
dogfooded clj-surgeon. Did Surgeon let one model decision remain one edit
transaction, or did the caller still carry mechanical refactoring state?

## Method

`make study-agent-usage` produced one privacy-safe version 3 receipt for the
exact two-hour window. It joined Codex and Claude histories with clj-surgeon
MCP, cclsp, and clojure-lsp telemetry. The complete receipt—not transcript
prose—is the counting authority.

The focused refactor cohort contains one primary Codex session and three child
sessions whose session metadata named the same target checkout. I used only the
receipt's hashed session records and `route_phases`. A narrow local lookup read
the `cwd` field from five receipt-named session metadata records to separate
the target cohort from this tool-development session. No source, command text,
URLs, or private project prose entered this study.

Claude had two sessions in the complete window but no comparable task turns or
Surgeon calls. Its row is retained as a coverage fact, not a performance
control.

## Window scoreboard

| Provider | Sessions | Clojure-relevant sessions | Surgeon calls | Surgeon-using turns | Native patch actions |
|---|---:|---:|---:|---:|---:|
| Codex, all repositories | 7 | 6 | 196 | 8 | 73 |
| Claude, all repositories | 2 | 1 | 0 | 0 | 0 |
| Codex, focused refactor cohort | 4 | 4 | 118 | not separately emitted | 31 |

The provider rows are not a contest: the tasks differed and Claude supplied no
matched refactor lane.

## The focused refactor's keystroke sequence

The four focused sessions made 118 Surgeon calls:

| Operation | Calls |
|---|---:|
| `inspect_clojure` | 65 |
| `:extract` preview | 16 |
| `:ls-deps` | 16 |
| `:ls` | 11 |
| `:ls-extract` | 8 |
| `apply_clojure_changes` | 1 |
| `:help` | 1 |

The dominant collapsed phases were:

| Phase | Occurrences |
|---|---:|
| native patch | 36 |
| Surgeon read plus native read | 31 |
| native read | 28 |
| native read plus live probe and verification | 28 |
| Surgeon read | 17 |
| Surgeon plan | 7 |
| Surgeon read plus plan | 5 |

This is not the desired `inspect -> decide -> one transaction` route. The tool
was frequently the microscope and safety proof, while native patches remained
the materializer. The single MCP apply count is especially revealing: the
caller knew many architectural moves, but the public transaction language did
not compile deletion and extraction mechanics completely enough to keep those
decisions in one transaction.

## Direct tool wall versus interaction wall

Across the complete window, the hot Surgeon MCP served 95 calls with a
**217 ms median** and 117.1 seconds of total direct tool wall. It read 192
files and returned 261,311 source characters. Eighty calls succeeded and 15
refused safely.

The focused cohort's Surgeon-bearing outer actions consumed 269.8 seconds.
That number includes caller/tool action boundaries and is not directly
comparable to the service's internal wall. The route phases show why the outer
cost was larger: reads were followed by more reads, plans, native materializing
patches, and verification rounds.

The semantic side was the dominant pathological delay. One workspace issued
13 `resolve_var_surface` calls. Four clojure-lsp `initialize` requests timed
out at about 120 seconds each: **480.1 seconds aggregate**. The cclsp MCP calls
usually returned after about 10 seconds, but they returned warming/refusal
evidence rather than the caller proof needed to authorize deletion.

```text
desired
  decide family extraction
    -> compile owners + callers + future namespaces
    -> one verified commit

observed
  decide family extraction
    -> outline/deps/extract preview
    -> semantic warm-up/refusal
    -> more exact reads
    -> native materialization
    -> verification
    -> repeat for next family
```

The scalpel was fast. The workflow repeatedly stopped to redraw the map and
then asked the model to carry the move.

## What was excellent

- Every observed MCP refusal was failure-closed. Two attempted 17–19 edit
  transactions rolled back without corrupting source.
- Batched structural reads were exact and usually subsecond.
- Dependency and extraction previews prevented guessed architectural moves.
- The refactor still reduced the target monolith by roughly 65% while its
  repository suite remained green.
- A later direct multi-owner edit proved that one transaction can replace many
  owner bodies safely when the public operation exists.

These are real product wins. The tool made a dangerous refactor safer even
when it did not make the whole route short.

## What amplified turns

### Exact deletion was missing from the direct transaction

The caller had 17 proven obsolete owners. Direct MCP could replace them but
could not delete them, so the proposed workaround was to replace each with a
marker and remove the markers natively. That is mechanical state created only
to cross an API gap.

This field case became `clj-surgeon-4uc`. During this study the direct contract
gained `forms + delete=true`, backed by the existing comment-aware deletion
kernel. A live call deleted two adjacent real fixture owners atomically and
published an inverse receipt. The first undo exposed equal-offset restoration
ordering; a permanent regression and original-offset tie-breaker then made the
undo byte-identical.

### Exact multi-owner preparation was missing

Known `{file, form}` owners should not require a language-server graph. The
single exact-source route existed but crashed in the MCP summary adapter. That
became `clj-surgeon-ox7`; the live route now succeeds with zero semantic calls.

The new ordered `owners` preparation route reads each distinct file once,
retains one basis, and assigns stable site IDs. It is the addressing substrate
for future `move-owners`, `delete-owners`, and extraction instructions.

### Verification confused baseline warnings with regressions

A correct contraction was rolled back because the workspace already contained
lint warnings. That is useful failure-closed behavior, but the fast profile
should prove diagnostic delta rather than require an unrelated clean baseline.
`clj-surgeon-ws5` now specifies baseline capture, refusal before write when the
baseline is unavailable, and rollback only for newly introduced diagnostics.

### Schema and lifecycle truth were duplicated

The direct MCP schema and hand-maintained validator sets could drift. cclsp
health, warming, and recovery independently interpreted the same process facts.
`clj-surgeon-kbb` removes both duplication points: Surgeon validators derive
their closed field sets from the published schema; cclsp projects one workspace
lifecycle value for every consumer.

The refactor itself caught two defects: `verify` had one validator meaning but
two schema descriptions, and the MCP reload manifest omitted the newly
extracted schema namespace. Both now have regression tests.

## Counterfactual limits

This is not a controlled native-versus-Surgeon benchmark. The primary task was
a long architectural refactor, the child sessions had different assignments,
and the receipt does not emit a matched no-Surgeon replay. The 480-second LSP
total is aggregate service time and can overlap outer work. The study therefore
supports a route diagnosis, not a claim that the refactor would have finished
faster without Surgeon.

The credible counterfactual is narrower: once the model already had 17 exact
owners, a direct `delete=true` transaction removes the marker plan, native
cleanup, and one verification/recovery round. That mechanism is now live and
falsifiable in a clean-caller test.

## The clean caller compiled the deletion

The clean-caller gate was deliberately exact: one formatted Clojure file, 17
supplied owner names, and no Surgeon-specific language in the task. The
installed skill was the only routing instruction.

The first synthetic attempt exposed formatting debt in the fixture rather than
a deletion defect. `verify=fast` rolled back twice because Standard Clojure
Style rejected pre-existing bytes; the same transaction then succeeded without
the optional repository profile. The fixture was restored from the inverse
receipt and formatted before rolling the clean die again.

The final clean session made exactly one source tool call:

```text
17 supplied owners
        -> apply_clojure_changes once
        -> 17 deletions / 1 file
        -> fast checks + read-back
        -> terminal inverse receipt
```

It used zero `inspect_clojure` calls, zero semantic calls, zero shell source
reads, zero markers, and zero native patches. The first attempt succeeded in
1.216 seconds of direct tool time. This is the first complete evidence that a
large, already-made mechanical decision can remain one edit transaction.

A separate frozen semantic-surface benchmark was not a valid substitute. Its
isolated Surgeon server used a temporary project root while cclsp remained
rooted elsewhere, so correct absolute and wrong relative paths disagreed and
the caller fell back. That harness defect is preserved as `clj-surgeon-78d`;
it does not weaken the exact-owner result.

The following experiment should lift the same boundary from deletion to
extraction: one explicit transaction program creates the target namespace,
moves named owners, rewrites declared callers, compiles the future require
graph, deletes source owners, verifies, and emits one undo receipt. The model
chooses the architecture once; the compiler owns the bookkeeping.

## Bottom line

The two-hour refactor validates both halves of the product thesis. Structural
perception and failure-atomic execution were excellent. Interaction compression
failed whenever a coherent architectural decision crossed an operation missing
from the transaction language.

Do not optimize 118 individual calls. Delete the calls. The target remains:

```text
think -> compile -> bang -> verified and reversible
```

## The compiler path crossed the boundary

The typed extraction transaction now exists. One `apply_clojure_changes`
request can create an absent namespace, move several named owners, redirect
exact callers, verify the complete future file set, and return one hash-fenced
undo receipt. Every reported caller candidate must be changed or explicitly
ignored.

Live dogfood against the shared hot MCP moved one fixture owner in one call:
one edit, two files, both read back and hashed. Its receipt then restored the
source byte for byte and removed the created target. The production-shaped
boundary test is harder: one owner move plus one caller rewrite across three
files, followed by exact three-file undo.

This directly actions the log's central finding. The caller no longer has to
assemble a destination, leave marker forms, delete them later, or remember
partial progress. One architectural decision is now one mechanical
transaction.

## Release accounting

The finished `apply_clojure_changes` extraction entrance accepts the complete
decision as data:

```text
existing namespace + named owners + absent destination
+ exact caller rewrites + explicit caller exclusions + expected counts
  -> compile every future file
  -> parse and hash-fence the complete file set
  -> commit failure-atomically
  -> verify every written byte
  -> publish one guarded undo receipt
```

The model still owns architecture: which owners move, where they belong, how
callers change, and which candidates deliberately remain. The compiler now owns
the bookkeeping: namespace construction, dependency policy, file capture,
write order, cardinality, rollback, read-back hashes, and undo.

The expected default for a behavior-preserving first move is
`require_policy: copy-all`. It retains the complete source namespace header,
including comments, imports, reader conditionals, and side-effect-only
requires. Dependency minimization becomes a later, independently reviewable
change. `minimal` remains available when the dependency proof is sufficient.

Every discovered caller candidate must appear in `caller_changes` or
`ignored_caller_files`. The destination parent must resolve inside the
workspace and the destination must be absent. A path escape, existing target,
stale source, missing caller decision, count mismatch, parse failure, write
failure, or new verification failure refuses or rolls back the entire request.
Rollback never overwrites bytes that the transaction cannot identify as its
own.

The intended agent route is now:

```text
inspect enough to decide architecture
  -> submit one extraction transaction
  -> stop on verification_complete=true
```

Agents should not hand-assemble the target, create marker forms, delete moved
owners in a later patch, rewrite callers one at a time, or reread successfully
verified files. Native patching remains the control for prose, JavaScript, one
arbitrary text edit, or a structural operation that Surgeon does not expose.

The new receipt carries its own hash and works with the existing guarded
command:

```bash
clj-surgeon :op :undo-extract! :receipt RECEIPT
```

The release proof has three layers:

1. Pure and injected-I/O tests cover compilation, path confinement, source
   drift, target races, partial-write rollback, caller accounting, receipt
   inversion, and exact undo.
2. A production-shaped boundary moves one owner, creates one namespace,
   rewrites one caller across three files, and restores all three exactly.
3. Live shared-MCP dogfood moved one fixture owner in one call, read back and
   hashed both files, then used the receipt to restore the source byte for byte
   and remove the target.

Final meter:

| Gate | Result |
|---|---:|
| Main suite | 644 tests, 5,553 assertions, 0 failures |
| MCP suite | 148 tests, 1,216 assertions, 0 failures |
| Changed-file clj-kondo | 0 errors, 0 warnings |
| Skill entrance | exactly 70 lines |
| MCP publication | hot-reloaded; no server restart |
| Installed consumers | CLI, Codex skill, Claude skill |

The interaction-compression hypothesis is now executable rather than
aspirational:

```text
think -> compile -> bang -> verified and reversible
```

## Verification was not caller completeness

Fresh field use supplied an important correction. cclsp remained warm or
timeout-prone, so the caller used exact structural reads and verified
transactions without claiming a complete semantic caller surface. That was
the correct epistemic boundary.

The product now names the boundary in every extraction result and receipt:

- `semantic-complete` means resolved callers completed in one hash-bound
  semantic session. Within that declared authority, zero callers is evidence.
- `structural-candidates-only` means exact syntax and quoted-Var scans
  completed. Aliases, macro-generated uses, and generated code can remain;
  zero candidates is not deletion authority. This is what direct extraction
  reports today.
- `caller-proof-unavailable` means no trustworthy inventory completed. The
  extraction route refuses instead of returning successful deletion evidence
  at this level.

The compact MCP success message now displays the caller-proof level beside the
atomic commit and read-back proof. `verification_complete=true` continues to
mean that the requested bytes were changed correctly. It does not mean that
the tool proved every possible caller in the program.

## The edit loop became hot while the proof became asynchronous

The refactor study also exposed a second transaction cost. Even after the
mechanical edit compiled into one request, the caller still had to format the
file, reload a running application, run focused laws, and wait for the complete
repository suite. TypeScript and Rust normally make those phases feel like one
compiler action. Clojure had better live introspection, but the caller was
manually assembling the feedback loop.

The new transaction joins four existing strengths instead of inventing another
editor:

```text
typed owner or retained site
  -> compile exact future source
  -> format staged bytes
  -> commit + read back + inverse receipt
  -> reload the configured application nREPL
  -> run exact focused law Vars
  -> return while one bounded cold job continues
```

Typed `{kind: defmethod, name, dispatch}` owners remove the first concrete
TypeScript advantage found in the field: selecting one implementation among 61
same-named multimethod forms. The address is structural. It contains no line
number and refuses a missing or duplicate dispatch.

Formatting now happens before the first live-file write. The formatter receives
a confined temporary mirror of the complete future file set; its output becomes
the parsed, hash-fenced candidate stored in the transaction receipt. A formatter
failure cannot leave a half-formatted worktree. If formatting changes bytes
outside the original structural spans, the inverse retains the complete original
file so undo remains exact.

The hot verifier accepts only closed project data: one relative nREPL port file,
an ordered namespace reload vector, exact test Vars, and a deadline. It connects
to the repository's real application JVM, proves the canonical working directory,
reloads callees before callers in the declared order, and runs only the named
laws. It accepts no arbitrary MCP evaluation string. A failed hot law triggers
the guarded source inverse and reloads the original namespaces.

Cold verification is deliberately a different state machine. It starts only
after formatting, commit, read-back, commands, and hot laws pass. The mutation
returns immediately with `verification_complete=false`, an opaque `verify/...`
job, and one executable `inspect_clojure` call. The bounded worker records PID,
deadline, output, exit, wall time, and a durable local status receipt. A later
cold failure does not silently roll back bytes after the caller has continued;
it preserves the failure and points at the existing undo receipt.

### Dogfood drew blood twice

The first live `verify=full` edit safely rolled back instead of launching a job.
`make mcp-reload` had refreshed the transaction namespaces but omitted
`mcp-http-server`, which owned the built-in verification profile. The live
process therefore retained the old blocking `make test` vector while tests saw
the new asynchronous profile. The reload manifest now includes every runtime
and configuration namespace, and its existing regression enumerates the new
hot and cold verifiers.

That obsolete cold run then found a test-topology coupling. `make test` launched
the Babashka suite, but its runner also required JVM-only MCP namespaces. Adding
the real nREPL client made the duplication fail before the dedicated JVM MCP
suite could run. No test was removed from the complete gate: the Babashka runner
now owns the pure structural/core suite, `make mcp-test` owns the formatter,
nREPL, HTTP, process, and MCP boundaries, and `make test` still runs both.

The first real cold worker also lacked `~/bin` in its process PATH. A shell
regression that intentionally invokes `clj-surgeon` therefore failed with
command-not-found even though the same command worked in the agent shell. The
synchronous verifier and asynchronous worker had duplicated PATH policy. One
small `mcp-process` namespace now owns the effective PATH for both; pure tests
prove that `~/bin`, `~/.local/bin`, system tools, and the caller's existing PATH
survive without duplicate entries.

Finally, the complete MCP runner was not actually complete. The new hot, cold,
and process tests passed in the development nREPL but were absent from
`mcp-test`'s explicit namespace list. The runner now names all three. This raised
the mandatory MCP gate from 158 tests and 1,296 assertions to 168 tests and
1,356 assertions. The tests were not merely written; the release command must
execute them.

The resulting evidence before the final release gate was:

| Proof | Result |
|---|---:|
| Cold job state-machine tests | 4 tests, 29 assertions |
| Joined focused MCP tests | 80 tests, 1,019 assertions |
| Complete MCP suite | 168 tests, 1,356 assertions |
| Babashka structural/core suite | 600 tests, 5,189 assertions |
| First live async edit return | 3.0 seconds; cold suite still running |

The experiential difference is the intended one. The caller spends judgment on
the dispatch, replacement, reload order, and laws once. Formatting and process
bookkeeping leave working context. The hot edit feels like a compiler action;
the cold suite becomes background evidence rather than a conversational pause.

### The asynchronous edge earned its own trust contract

Adversarial review found three failure modes that the happy-path design did not
make impossible. Two callers could observe free worker capacity at the same
time, a timed-out shell could leave descendants alive, and a terminal cold
failure could tell the caller to use an undo receipt without carrying that
receipt itself.

The worker now reserves capacity while holding one lock, kills and waits for
the complete process tree, reports whether termination was confirmed, and
attaches the exact inverse receipt and receipt hash to the durable job before
the initial mutation response returns. Focused proof is 24 tests and 174
assertions; changed-file lint is zero errors and zero warnings. The complete
MCP runner now executes 169 tests and 1,367 assertions.

Dogfood also found two honest remaining product gaps. Top-level sibling
insertion is still unsupported and is already tracked by `clj-surgeon-95u`.
A transaction that combined one namespace replacement with two named-form
replacements in the same file refused with `invalid-transaction-receipt`; exact
hash inspection proved the source stayed unchanged, but the response did not
say so clearly. That receipt-coherence gap is tracked by `clj-surgeon-dij`.
Neither gap weakens the shipped hot/cold loop, but both are now durable instead
of becoming caller folklore.

### The release proof completed the loop

The final dogfood transaction addressed exactly `render :card`, staged and
formatted its candidate, committed and read it back, returned in 2.7 seconds,
and launched the complete repository gate. Terminal status arrived after
189.8 seconds with `passed=true`, `verification_complete=true`, and
`next_action=none`. It still carried the same inverse receipt and receipt hash.
Applying that receipt restored the fixture's exact original SHA-256.

The final release evidence was:

| Proof | Result |
|---|---:|
| Structural/core suite | 600 tests, 5,199 assertions, 0 failures |
| JVM/MCP suite | 169 tests, 1,368 assertions, 0 failures |
| Stdio MCP smoke | 3 responses, 10.3 seconds |
| Changed Clojure formatting | 54 files stable |
| Changed Clojure lint | 0 errors, 0 warnings |
| Hot edit return | 2.7 seconds |
| Complete cold gate | 189.8 seconds, passed |
| Exact undo | original source hash restored |

Two failed release rehearsals made the final contract better. One caught a
skill sentence whose exact regression had not yet been updated. Another proved
that the old ten-minute cold deadline and 45-second stdio startup deadline were
too narrow under machine contention. The suite assertions stayed intact. The
bounds became 20 minutes for the complete gate and 120 seconds for cold stdio
startup; the successful run finished far below both. Wall-time variance remains
an optimization target, but it no longer compromises truth, cleanup, or the
interactive edit loop.

### Same-file transactions separated decisions from undo mechanics

The next self-hosting failure initially looked like a compiler defect. One MCP
request changed a namespace owner and two named forms in the same file. The
compiler had already done the hard parts correctly: all selectors used one
original snapshot, the edits were disjoint, the file was canonicalized once,
and request-order permutations produced identical future bytes.

The actual defect was downstream. Formatting may convert several logical edits
into one raw original-to-formatted edit so an undo restores exact pre-format
bytes. The receipt validator assumed that caller-visible match count and
physical inverse-record count were the same number. A valid transaction with
three logical edits and one physical inverse therefore failed only when its
receipt was validated.

The repaired receipt states both facts:

| Evidence | Meaning |
|---|---|
| `:match-count` | Logical selections proved against the original snapshot |
| `:inverse-edit-count` | Physical edit records required for byte-exact undo |

The validator proves the logical count from the receipt's intent evidence and
the physical count from its concrete inverse records. New receipts always carry
both counts. Ordinary version-1 receipts that omit `:inverse-edit-count` remain
valid and derive it from `:match-count`; existing binding-rename receipts remain
valid too. A rehashed receipt that lies about either fact now refuses.

The adversarial review prevented a false refactor. The first plan proposed a
new same-file grouping layer. Reading the pure compiler showed that grouping,
canonical paths, overlap refusal, and permutation invariance were already
present. The revised implementation changed only receipt semantics and public
diagnostics. Overlap refusals now name both change IDs and give the executable
remedy to make them disjoint.

The final evidence was:

| Proof | Result |
|---|---:|
| Red regression before repair | 3 logical edits, 1 inverse record, invalid receipt |
| Controlled public MCP | namespace + two defns, formatter coalescing, one commit, exact undo |
| Live shared MCP | 3 edits, 1 file, terminal verified receipt, exact SHA restored |
| Focused transaction/contract/tool suites | 86 tests, 910 assertions |
| Structural/core suite | 601 tests, 5,211 assertions |
| JVM/MCP suite | 170 tests, 1,383 assertions |
| Changed-file lint | 0 errors, 0 warnings |

Dogfooding also confirmed the boundary of the next hill. A named top-level form
still cannot act as the sibling anchor for `insert_after`; the public tool
refused safely with `unsupported-insertion-parent`. That is not hidden inside
this receipt fix. It remains the separate `clj-surgeon-95u` contract.
