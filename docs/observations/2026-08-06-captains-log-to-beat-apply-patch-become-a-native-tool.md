# Captain's Log: To Beat `apply_patch`, Become a Native Tool

<!-- agent-usage-window-end: 2026-08-07T05:25:35.668904Z -->

**Window:** 2026-08-07 04:19:32Z through 05:25:35Z
(2026-08-06 21:19:32 through 22:25:35 Pacific)

**Question:** How did the intent compiler perform against its stated goals,
and what is the shortest credible path from matching native patching to beating
it by a material margin?

## Sampling and exclusions

The bounded agent-usage receipt found two Codex sessions and no Claude
sessions. Both Codex sessions involved Clojure. The activation trigger was
visible in both; one loaded the skill. This is not a field-adoption sample. The
long session was the current clj-surgeon development turn: it built benchmark
fixtures, documentation, shell harness changes, and Babashka components. Most
native patches were therefore appropriate unsupported edits rather than
Surgeon fallbacks.

The receipt is the counting authority. It emitted hashed session keys,
privacy-safe route phases, operation counts, payload sizes, and direct tool
wall. It emitted no transcript prose or workspace paths. No private task
context was inspected. Claude has no observations in this window, so this log
does not claim a cross-caller result.

The controlled evidence comes from the corrected two-lane representative
benchmark recorded in the preceding Captain's Log. The first run is excluded
from product comparison because its copied task directory was not a Git
repository; ordinary verification commands failed and created artificial
recovery work.

## Naturalistic scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 2 | 0 |
| Clojure-relevant sessions | 2 | 0 |
| Skill visible in relevant sessions | 2 | — |
| Sessions that loaded the skill | 1 | — |
| Surgeon calls | 17 | 0 |
| Surgeon tool actions | 11 | 0 |
| Surgeon reads | 10 | 0 |
| Surgeon applies | 1 | 0 |
| Native patch actions | 24 | 0 |
| Native shell reads | 17 | 0 |
| Median Surgeon action wall | 569 ms | — |
| Median native patch action wall | 594 ms | — |
| Total Surgeon action wall | 7.353 s | — |
| Total native patch wall | 16.466 s | — |
| Surgeon output | 120,337 characters | 0 |

The operation mix was 11 `:cat`, five `:ls`, and one `:mv-with-deps`. Six
actions carried refusal-shaped output, but the types include the deliberately
exhaustive invalid-capsule matrix. They are test evidence, not a field refusal
rate.

The useful signal is not adoption. It is direct tool cost. Median Surgeon and
native patch actions were within 25 ms. Optimizing parser startup cannot create
a five-to-ten-second product win. Complete task time lives in model turns,
context acquisition, recovery, and verification ceremony.

## Score against the goals

The product goals came from the vision, intent-transaction plan, structural
change language, and earlier Captain's Logs.

| Goal | Evidence | Status |
|---|---|---|
| Preserve exact bytes and parse every result | Exact accepted bytes in both representative lanes; 533 tests and 4,579 assertions pass. | **Passed** |
| Refuse the whole transaction before partial writes | Pure, boundary, rollback, stale-hash, and read-back tests pass. | **Passed** |
| Produce a durable hash-fenced inverse | Implemented, installed, dogfooded, and restored exact original hashes. | **Passed** |
| Keep one coherent model decision in one source transaction | Six heterogeneous edits across two files committed in one `:change!`. | **Passed** |
| Avoid temporary manifest patching | The corrected caller used stdin and created no manifest file. | **Passed once** |
| Succeed on the first mutation attempt | The corrected Surgeon and native lanes both did. | **Passed once** |
| Be no slower than native | 46.154 s versus 45.777 s. Surgeon was 0.377 s slower. | **Effectively tied; strict gate not passed** |
| Beat native by at least five seconds | No. | **Failed** |
| Use at least 50% fewer source-bearing actions | Both corrected lanes used two source-bearing shell commands around their mutation route. | **Failed** |
| Emit at least 50% less source than native | 2,194 versus 3,477 bytes: 36.9% less. | **Improved; gate failed** |
| Use at least 50% fewer input tokens | 65,501 versus 74,710: 12.3% less. | **Improved; gate failed** |
| Beat the repeated-Surgeon microscope by at least 2× | The representative portfolio has not yet run a matched pre lane. | **Open** |
| Clean Codex and Claude callers independently choose the same one-shot scoped transaction | Codex did in the corrected run; current scoped-schema Claude evidence is incomplete. | **Open** |
| Prefer native patching for a unique prose edit | The frozen portfolio contains this control, but the full matrix has not run. | **Open** |

This is excellent mechanism progress and incomplete product progress. We built
the safe transaction engine we intended. We have not yet made it faster than
the strongest editor already native to the agent.

## Why `apply_patch` remains fearsome

`apply_patch` has four advantages that have nothing to do with textual editing
being intrinsically superior:

1. **It is already a typed tool.** The model does not spend an action reading a
   skill or remembering shell syntax.
2. **Its input is the model's native edit representation.** A patch can mix
   source, comments, insertions, deletion, and several files without encoding
   nested source strings inside EDN.
3. **It is one action.** There is no CLI launch decision, quoting concern,
   manifest choice, or plan/apply vocabulary.
4. **It accepts arbitrary context.** When one bounded read provides stable
   surrounding lines, the model can materialize the entire decision at once.

The current Surgeon lane was:

```text
read 7.6 KB skill
  -> emit a large EDN heredoc
  -> invoke :change!
  -> inspect an aggregate Git diff
  -> answer
```

The source mutation itself was already one action and completed quickly. The
remaining disadvantage is the entrance and exit ceremony around it.

## Where Surgeon has genuine comparative advantage

Do not attack native patching on its strongest ground. For one exact unique
text or prose edit, use native patching. Surgeon should win when structural
addresses eliminate work that a patch must reconstruct:

- the caller knows owners and exact forms but not stable surrounding lines;
- duplicated syntax requires per-owner or per-file cardinality proof;
- several heterogeneous edits must commit or refuse together;
- dependency closure determines a safe move;
- source spelling such as `#()`, metadata, comments, or reader conditionals is
  part of correctness;
- a durable inverse or stale-state refusal replaces manual recovery.

The earlier 3.48× win occurred precisely at this boundary: native patching had
to rediscover context after a context-free patch refused. The corrected
six-edit run tied because one bounded native read supplied enough context for a
successful multi-file patch. The product boundary is now empirical rather than
aspirational.

## The shortest route to a five-to-ten-second win

Make the transaction compiler as native to the model as `apply_patch`.

Expose one typed MCP operation shared by Codex and Claude:

```text
clj_change(scope, changes, expectations, apply=true)
  -> one compact verified receipt
```

The tool schema should carry the recognition example and exact field
descriptions. A caller should not read a skill, construct a temporary file,
escape EDN through a shell, or choose between preview and apply when the prompt
already supplies the complete decision. The same kernel remains underneath;
MCP changes transport, not judgment or safety.

On success, the response must be terminal evidence:

```clojure
{:ok true
 :committed true
 :changes 6
 :files 2
 :verified {:declared-results true
            :whole-files-parsed true
            :read-back-hashes {...}}
 :undo-receipt "..."}
```

The tool description should say explicitly: a successful verified receipt
completes mutation verification; do not reread source or run a duplicate diff
unless the user asked to review the aggregate patch. This removes the skill
read and redundant verification turn without weakening evidence.

The target route becomes:

```text
one typed clj_change call
  -> final answer
```

Native still needs:

```text
bounded context read
  -> apply_patch
  -> verification
  -> final answer
```

Removing two model boundaries is the plausible source of a five-to-ten-second
gain. Shaving 300 ms from Babashka startup is not.

## Do not expand the algebra yet

A typed entrance is the smallest falsifiable improvement because the current
kernel already expressed the six-edit decision correctly. Adding capture,
insert, delete, wrap, move, or require operators before testing the transport
would confound two hypotheses.

First compare three lanes on the frozen `decision-batch-edit` capsule:

| Lane | Entrance |
|---|---|
| Native | bounded read plus `apply_patch` |
| CLI transaction | installed skill plus stdin `:change!` |
| Typed transaction | one MCP call, no skill read |

Run four counterbalanced correct replicates. Keep the MCP entrance only if it:

- beats native median wall by at least five seconds;
- uses one source-bearing action;
- performs zero source reads and zero failed mutations;
- creates no temporary manifest;
- emits no more source than the CLI transaction;
- preserves every current parse, refusal, atomicity, and inverse gate.

If the typed entrance wins, run the complete five-task portfolio. Only then use
the losing structural strata and naturalistic fallbacks to choose the next
mechanical operators. This preserves the Bitter-Lesson boundary: observed
repeated mechanics shape the kernel; imagined refactoring intelligence does
not.

## Harness architecture

The experiment must not add a third benchmark application. The new canonical
repository rule treats Codex and Claude shell harnesses as thin adapters and
moves shared policy behind tested Babashka seams. The first seam is already
live: both callers use one Babashka component to initialize an isolated clean
Git workspace. Task catalogs, scoring, retention, summaries, and MCP receipts
should follow the same strangler pattern.

## Bottom line

We achieved the profound part: one model decision can now become one exact,
failure-atomic, reversible structural transaction. The corrected benchmark
shows that this stronger route can match `apply_patch` while using less context.

We did not achieve the performance goal. Surgeon is still presented as a CLI
that must be learned; `apply_patch` is presented as thought made executable.
To beat it, do not make the parser marginally faster and do not grow a catalog
of clever rewrites. Give the existing transaction compiler a first-class typed
entrance and make its verified receipt the end of the edit.
