# Captain's Log: The Intent Compiler Won Its First Matched Race

<!-- agent-usage-window-end: 2026-08-07T04:19:32.422896Z -->

**Window:** 2026-08-06 22:43:19Z through 2026-08-07 04:19:32Z
(2026-08-06 15:43:19 through 21:19:32 Pacific)

**Question:** Are we meeting the goals stated in the recent Captain's Logs, or
have we built a safer interface that still loses to native editing?

## Sampling and exclusions

The bounded usage receipt found 12 Codex sessions and one Claude session. Four
Codex sessions contained relevant Clojure work. The Claude session did not, so
this window cannot support a caller comparison.

The collector emitted hashed session keys, privacy-safe route phases, operation
counts, tool payload sizes, and direct tool wall. It emitted no transcript
prose or workspace paths. This study used `make study-agent-usage`, one bounded
`jq` projection of its receipt, heading and conclusion searches over the
Captain's Logs, and narrow reads of the stated acceptance gates.

A separate fresh, ephemeral Codex pair ran immediately after the collected
window. It is reported as a matched product probe, not mixed into the
naturalistic usage counts. Both lanes used the same model, two copied fixture
files, exact owner names, exact target and replacement syntax, and the same
declared counts. One lane used the installed skill and Surgeon. The control
explicitly used native patching. Raw benchmark transcripts were not retained.

## Naturalistic scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 12 | 1 |
| Clojure-relevant sessions | 4 | 0 |
| Relevant sessions with skill visible | 4 | 0 |
| Relevant sessions that loaded the skill | 3 | 0 |
| Skill loads | 11 | 0 |
| Surgeon calls | 140 | 0 |
| Surgeon tool actions | 110 | 0 |
| Surgeon reads | 102 | 0 |
| Surgeon plans | 1 | 0 |
| Surgeon applies | 8 | 0 |
| Native patches | 78 | 0 |
| Surgeon refusals | 40 | 0 |
| Median Surgeon action wall | 692 ms | — |
| Total Surgeon action wall | 119.80 s | — |
| Surgeon output | 632,591 chars | 0 |

The operation mix was 81 `:cat`, 33 `:ls`, 12 `:xray`, eight `:change!`, one
`:change`, one `:undo-change!`, two `:match-form`, one `:help`, and one mistaken
`:get`.

This is a development-heavy sample. Many refusals came from adversarial tests,
malformed-spec probes, and self-hosting, so 40 refusals must not be interpreted
as a field failure rate. The durable signal is the route mix: transaction use
became real, but it did not replace the read loop or native patching. The
window still contains 102 Surgeon read actions and 78 native patches.

## The matched write probe

The task supplied the exact files, unique named owners, exact target,
replacement, and cardinalities. Neither lane needed discovery to understand
the requested change.

The Surgeon lane read the installed skill, wrote a temporary transaction
manifest, and called `:change!` once. It did not inspect source. The transaction
compiled two owner-scoped changes, committed two files atomically, parsed and
read back both results, and returned one durable inverse receipt.

The native lane tried one context-free two-file patch first. That patch refused
without changing bytes because the patcher needed containing-line context for
an indented token. The caller then listed the already-correct file names twice,
read bounded context around the two owners, applied one successful two-file
patch, and verified the diff.

| Measure | Scoped Surgeon | Native control | Change |
|---|---:|---:|---:|
| Correct final source | yes | yes | tied |
| First mutation attempt | succeeded | refused | Surgeon won |
| Complete task wall | **38.60 s** | 134.26 s | **−95.66 s; 3.48× faster** |
| Model tokens | **13,095** | 21,888 | **−8,793; 40.2% fewer** |
| Source reads before mutation | **0** | 1 bounded read | Surgeon won |
| Successful source mutation actions | 1 | 1 | tied |
| Failed mutation actions | **0** | 1 | Surgeon won |
| Source emitted to the model | **none** | bounded context plus diffs | Surgeon won |
| Structural owner/count proof | yes | no | Surgeon won |
| Durable hash-fenced inverse | yes | no | Surgeon won |

This is the first matched result in the intended 2–4× range. The gain did not
come from a faster parser. The installed binary itself completed the guarded
transaction in 103 ms. The gain came from giving the editor the address the
model actually had: file plus named owner plus exact syntax. Native patching
needed to rediscover line context before it could express the same decision.

The result is a breakthrough, not a population estimate. It is one roll on a
small, deliberately structural task. Native patching can still win when the
caller already has stable surrounding lines or when the edit is arbitrary
text. The next claim must be about the boundary, not universal superiority.

### Verification evidence

The implementation gate grew rather than weakened:

| Gate | Result |
|---|---:|
| Focused transaction suite | 32 tests, 348 assertions, zero failures |
| Complete repository suite | 533 tests, 4,579 assertions, zero failures |
| Clojure lint on changed source and tests | zero errors, zero warnings |
| Installed two-file transaction | committed and read back both files |
| Installed inverse | restored both original SHA-256 hashes exactly |
| Wrong owner distribution | refused with source unchanged and no receipt |

The scoped schema also gained permanent documentation and installation
contract tests. README, help, changelog, vision, and both installed agent skills
must teach `:changes`, named owner scope, `:each-form`, the supported
`[:replace SOURCE]` operator, legacy compatibility, and the prohibition on
mixing schemas.

## Score against the stated goals

The original transaction keep gate required correctness first, then large
reductions in wall time and source-bearing actions.

| Stated goal | Current evidence | Status |
|---|---|---|
| At least 2× faster than the repeated-Surgeon route | The scoped transaction removed repeated reads and edits, but no matched legacy-Surgeon lane ran in this pair. | Open |
| No slower than native | 38.60 s versus 134.26 s on the exact paired task. | **Passed once** |
| At least 50% fewer source-bearing actions | One guarded mutation and no source read versus a failed patch, bounded read, successful patch, and source-bearing diff verification. | **Passed once** |
| At least 50% less source output than native | Surgeon returned receipt data and no source; native returned context and two diffs. Exact character counts were not captured. | Direction passed; magnitude open |
| Zero partial writes after refusal | Pure, CLI, rollback, and wrong-distribution tests prove refusal leaves source and receipt untouched. | **Passed** |
| Exact inverse succeeds and refuses when stale | Repository tests and installed-binary smoke cover exact undo and stale-hash refusal. | **Passed** |
| One model decision becomes one transaction | Both self-hosting and the clean caller compiled multiple edits into one source transaction. | **Passed** |
| One model decision becomes one tool action | The clean caller loaded the skill and created a temporary manifest with native `apply_patch` before calling Surgeon. | **Failed** |
| Universal 2–4× product win | One matched task reached 3.48×; naturalistic work still contains large read and patch loops. | Not established |

## The honest product verdict

The write-side thesis has crossed from attractive design to real evidence.
Explicit owner scope plus exact replacement was sufficient to beat native
patching decisively when the model knew structural addresses but not line
context. It also bought stronger proof and undo rather than trading safety for
speed.

The complete product has not reached the stated ideal:

```text
inspect -> decide -> change and verify
```

Reading still pays per question in naturalistic work. Skill activation is
three of four relevant Codex sessions, not universal. The collector still sees
hundreds of read and patch actions and more than 600,000 characters of Surgeon
output. The earlier semantic-read win remains useful, but this window does not
show that the read side is irresistible.

The clean write route also violated the stricter one-action thesis:

```text
read skill -> create manifest -> change!
```

The skill already prefers stdin. Guidance did not overcome the caller's
preference for a file-backed artifact because a native patch tool offered an
easy typed way to create one. This is product evidence, not caller error.

## Next falsifiable improvements

1. Give the agent a typed `change` entrance that accepts the transaction object
   directly. The caller should not load a long skill, escape EDN through a
   shell, or create a temporary manifest.
2. Repeat the exact paired task at least twice more. Preserve the result only
   if both lanes remain correct and scoped Surgeon retains at least a 2× median
   wall advantage.
3. Run one task where the caller already knows stable line context. Native
   patching should remain the expected control there; Surgeon must earn use
   through stronger proof without pretending every task is structural.
4. Dogfood a task with five or more heterogeneous edits. The scoped compiler
   should beat both repeated `:edit` and one native patch without a source
   reconstruction round.
5. Treat nested source-string escaping, temporary manifest creation, and large
   success payloads as tracked product defects. The fastest route must also be
   the safest route.

## Bottom line

The Captain's Logs were right to reject subsecond parser victories as the main
goal. The first scoped compiler race saved 95.66 seconds because it materialized
the model's structural decision directly. That is the big game.

We have not built the perfect tool. We have proved one important part of it:
when the model knows *what structural object* must change, requiring it to
reconstruct line-oriented patch context can cost more than a minute. A guarded
intent compiler can remove that entire loop.
