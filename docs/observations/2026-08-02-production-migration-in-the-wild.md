# Ethnographic Study: clj-surgeon in a Production Storage Migration

**Date:** 2026-08-02<br>
**Observed interval:** 2026-08-01 12:36:54 PDT–2026-08-02 12:36:54 PDT<br>
**Primary setting:** feature work, adversarial review, and a live MySQL-to-Postgres
migration in a production application

## Method

This study reconstructs use from canonical local Codex rollout JSONL, then
checks claimed outcomes against the host repository's commits, working tree,
tests, plans, and operational evidence. It does not infer tool use from an
agent saying that it loaded a skill. The unit of analysis is:

> operator intent → exact CLI operation → result → changed understanding →
> edit, refusal, test, or production gate

The search covered every matching rollout in the fixed 24-hour interval. It
found five relevant sessions. Counts below expand shell loops and command
arrays. Skill reads, documentation examples, transcript quotations, and
commands after the cutoff are excluded.

| Operation attempt | Count |
|---|---:|
| `:ls` | 59 |
| `:find-subform` | 11 |
| `:replace-subform` | 14 |
| `:replace-subform!` | 15 |
| invalid `:get` | 1 |
| **Total** | **100** |

One `:replace-subform!` command was present after an empty `apply_patch` call in
the same orchestration program and was never reached when that patch failed.
The agent retried it directly five seconds later. Thus the corpus contains 100
operation attempts but 99 actual CLI process launches.

## Session Census

| Local start | Task | clj-surgeon role | Durable outcome |
|---|---|---|---|
| Aug 1 14:42 | iPhone controls and build identity | outline, nested target proof, parse checks | pushed to `main`; 159 tests and 610 assertions green |
| Aug 1 15:58 | adversarial OOM/SQL-cutover review | outline as navigation index | ranked seven defects; implementation later landed |
| Aug 1 17:41 | adversarial Mongo-cutover review | outline as navigation index | metadata race and residual require later fixed |
| Aug 1 20:44 | adversarial storage-plan review | announced, but not actually invoked | NO-GO findings recorded in the plan |
| Aug 1 21:55 | live storage migration | orientation plus guarded leaf replacement | `topuserposts` shadow proven; `userposts` partial; work remained uncommitted at cutoff |

The last row matters. At the cutoff, the agent had not “finished the migration.”
It had proved a complete 10,000-row `topuserposts` shadow, migrated 17 of 780
`userposts` users containing 6,401 memberships, and then recognized and stopped
scope drift into `top_posts`. The working tree remained large and
dirty. This is useful progress, not a completed deliverable.

## Executive Finding

clj-surgeon was excellent in two roles and almost absent in a third:

1. **Structural map:** `:ls` compressed large namespaces into trustworthy form
   boundaries before narrow reading.
2. **Mechanical scalpel:** a reviewed `:replace-subform` plan replaced one
   exact backend-call leaf, then a live JVM and both durable stores tested the
   semantic claim.
3. **Architectural intelligence:** the tool did not decide migration order,
   find all resolved callers, design schemas, diagnose collation, or judge
   rollback safety. `rg`, clj-kondo, live queries, tests, and the agent did that
   work.

That is the right boundary. The best episode followed this loop:

```text
human/agent judgment
  → exact structural target
  → hash-bound plan
  → reviewed diff
  → guarded apply
  → namespace reload
  → exact old/new durable-store parity
```

This does not make the Bitter Lesson mistake. The tool supplies reliable,
general bookkeeping and lets the model use evidence to decide what matters.
It does not encode a growing catalog of domain-specific refactoring wisdom or
silently infer which dependencies, callers, or data contracts should move.

## Episode 1: `:ls` Became the Default Cognitive Map

Fifty-nine of 100 attempts were `:ls`. In all four sessions that actually used
the CLI, it preceded narrow source reads of large namespaces such as
the legacy storage namespace, the public storage interface, `postgres.db`,
`server2.db`, and the main views namespace.

The read-only reviews show the cleanest division of labor:

- `rg` discovered table names, routes, and possible callers across the tree;
- clj-kondo resolved actual var usages;
- `:ls` mapped a large file into named form boundaries;
- bounded `sed` reads supplied prose-scale context inside those boundaries.

This was especially effective in the SQL-cutover review. Thirteen outlines
supported a verdict that the globally navigable top-posts pages were not an
orphan, and exposed failure suppression, unbounded backfill reads, incomplete
retry selection, a username-only tripwire, stale liveness, recurring migration
inside a weekly job, and legacy CLI wiring. A later commit addressed the OOM
path and cut over the CLI argument.

The Mongo review used five outlines and found a concurrent JSONB metadata
clobber plus a needless transitive Mongo load. Later commits perform server-side
metadata patch merging and remove the legacy require.

This is real leverage even though no structural write occurred. An outline can
be valuable infrastructure for a high-quality code review.

## Episode 2: Inspection Did Not Automatically Become Structural Editing

The iPhone UI session used eight outlines and two `:find-subform` searches to
prove the exact hamburger and right-menu nodes inside `menu-bar`. The actual
edits were ordinary patches, not lens plans. That was reasonable: the change
spanned Clojure, CSS, and two new JavaScript files, and most edits were not
small subtree substitutions.

The resulting feature landed on `main`. Validation included:

- live Clojure rendering probes;
- Node behavior harnesses for iPhone and desktop paths;
- JavaScript syntax checks;
- structural reparsing of changed Clojure files;
- a full `159 tests, 610 assertions, 0 failures` run.

The honest conclusion is not “clj-surgeon made the UI feature.” It reduced the
cost and ambiguity of orienting inside a 2,000-plus-line view namespace and
helped prove that later patches still parsed.

## Episode 3: The Migration Found the Ideal Structural-Mutation Loop

The long migration session used saved plans for thirteen successful, exact
subtree replacements. Examples included:

- changing a swallowed MySQL pagination exception into a logged rethrow;
- preserving the complete wrapper with `(assoc data :posts sliced-posts)`;
- correcting a Guardrails count contract;
- moving individual public-interface readers behind MySQL/shadow/Postgres
  mode selection;
- replacing a top-level-only JSON number normalizer with recursive
  normalization inside `decode-json`.

Each interface move targeted one backend call inside one named form. The agent
planned it, inspected the single-edit diff and hashes, applied the saved plan,
reloaded the namespace, and exercised the relevant contract. Every successful
apply invalidated the previous file snapshot, so the next edit required a new
plan.

The recursive normalization episode is the strongest evidence. A 257-record
canary matched counts, order, and keys but still found 37 differing posts and
41 numeric-shape leaves. MySQL decoded scientific-notation timestamps as
`Double`; Postgres JSONB canonicalized them as `Long`; the compatibility
normalizer only visited the top level. The agent replaced the exact nested
`reduce` in `decode-json`, reloaded it, and obtained exact full/slim equality
and an identical corpus digest.

clj-surgeon did not discover the numeric contract. It made the final risky
mechanical change precise after live evidence identified it.

## Episode 4: Plans Became Evidence, Not Just Mutation Instructions

The operator explicitly praised the mechanical replace-form sequence and asked
for it to become the migration playbook. The session then recorded this rhythm
in the Captain's Log:

```text
:find-subform
  → reviewed hash-bound :replace-subform plan
  → :replace-subform!
  → reload
  → contract matrix
```

This is an important product effect. The plan file served simultaneously as:

- an exact selector;
- a review artifact;
- a temporary capability bound to one source hash;
- an execution record that could be named in the operational narrative.

The ceremony did not appear to slow the agent down. It made thirteen sequential
seam edits acceptable in a highly active, dirty working tree.

## Episode 5: Two Guessed Commands Exposed a Missing Read Primitive

At 23:05 PDT, the migration agent knew the desired form name and tried:

```bash
clj-surgeon :op :get :file ../postgres/src/postgres/db.clj \
  :form upsert-starred-post!
```

The CLI correctly returned `:unknown-operation`, but only listed every valid
operation. Seven seconds later the agent tried:

```bash
clj-surgeon :op :find-subform \
  :file ../postgres/src/postgres/db.clj :line 1134
```

That correctly returned `:missing-arguments` for `:match`. Both guesses express
the same unmet job: “show me the named form, or the form containing this line.”
The agent then fell back to a bounded source read.

This is stronger evidence than a feature wishlist. A clean-context agent
invented both plausible interfaces while doing real work.

## Episode 6: One Orchestration Failure Was Not a clj-surgeon Failure

After creating `/tmp/top-user-get-all-plan.edn`, one tool program first
attempted an empty `apply_patch` update to that plan and then intended to run
`:replace-subform!`. The patch failed as an invalid empty hunk, so JavaScript
control flow never reached clj-surgeon. Five seconds later the direct apply
succeeded with result hash `9cbc2595…7066e`.

The product lesson is about agent instructions: a saved structural plan is
already the authorized application artifact. Agents should not edit it or
route it through a generic patch tool. The CLI behaved correctly.

## Episode 7: Skill Invocation Can Be Ceremonial

The storage-plan reviewer announced use of the clj-surgeon skill, then made
zero CLI calls. This was not disobedience: the user supplied a narrower
read-only `rg`/`sed`/`git` allowlist, and the agent explicitly followed it. The
review still found seven serious design flaws and produced the NO-GO amendments
later committed to the plan.

The telemetry lesson is that “I am using the skill” is not evidence of tool
use. Reports should distinguish:

- skill guidance loaded;
- CLI actually invoked;
- CLI result changed the decision;
- structural mutation applied.

## Test Assessment: Exceptional Live Proof, Uneven Durability

### What was awesome

The migration validation was unusually strong:

- exact wrapper, order, duplicate-position, key-set, and decoded-value checks;
- complete semantic SHA-256 comparisons, not row-count theater;
- boundary pages and missing slices;
- a 288-case pagination matrix across sort fields, directions, authors, and
  positions;
- live namespace reloads after individual edits;
- independent rereads from both durable stores after a deployed job;
- stop-on-first-difference behavior that found real collation and numeric-shape
  defects.

The iPhone work also ran the complete fast suite and used direct JavaScript
behavior harnesses rather than syntax checks alone.

### What was not awesome enough

The best migration checks mostly live in a transient REPL transcript and
prose evidence. They are world-class migration gates but not yet world-class
regression tests.

The two new boundary test files in the working tree are a good instinct: they
make backend bypasses a permanent failure. But their implementation is textual.
They infer namespace aliases with regular expressions and search source strings
for `alias/var`. They can miss fully qualified calls, `:refer` imports, CLJC or
CLJS callers, macro-generated references, and alternate SQL spelling. Their
allowlists are hand-maintained copies of production knowledge. Neither test was
run in the migration session.

The `top_posts` artifact test is pure and preserves a crucial fact:
the same post ID may occupy multiple ordinals with distinct payloads. It is
still only one example. It does not exhaust malformed wrappers, missing IDs,
period handling, duplicate ordinals, ordering, or round-trip reconstruction.

The UI's checked-in Hiccup test verifies that both build identities and two
timestamps exist. Its JavaScript behavior harnesses were ad hoc commands in the
transcript, so CI cannot prevent a later regression in first-tap behavior or
the one-minute age refresh.

### The repository instruction caused part of the gap

`AGENTS.md` says all Codex agents lack outbound network and must never run test
commands. That assumption was false in this observed environment: the UI
session on the same checkout ran the full suite successfully, and the migration
session had a working attached JVM and dependency cache. `server2/CLAUDE.md`
already contains capability probes for Clojure, Maven, and local dependencies.

An identity-based prohibition (“Codex cannot test”) made the migration agent
obey a stale model of the environment instead of checking actual capability.
This is the most important repository-level one-shot failure found by the
study.

## Product Recommendations

### P0 — Add a first-class exact-form read operation

Support the two jobs the agent guessed, under one stable operation such as
`:show-form`:

```bash
clj-surgeon :op :show-form :file state.clj :form transition!
clj-surgeon :op :show-form :file state.clj :line 1134
```

Require `:file` plus exactly one of `:form` or `:line`; accept optional
`:platform` only for reader-conditional disambiguation. Return exact form
source, type, name when present, platforms, line range, attached-comment start,
selector, and complete-file source hash. Refuse ambiguity. This removes the
recurring `:ls` → filter → bounded `sed` bridge without pretending to replace
broad search.

### P0 — Make errors recommend executable remedies

Unknown `:get` with explicit `:file` plus `:form` or `:line` should return an
executable remedy for `:show-form`. A line-only `:find-subform` error should
explain that the operation searches syntax patterns and point to the exact
containing-form command. Base remedies on supplied arguments, not fuzzy intent
inference. Keep the EDN machine-readable and the exit status nonzero.

### P0 — Teach the skill that plan application is the write step

State explicitly:

> Review the saved EDN plan, then run `:replace-subform!`. Do not edit the plan
> with `apply_patch`; regenerate it if the intended edit changes.

Add a clean-context test where an LLM is handed a reviewed plan and asked to
apply it. Passing means one direct apply, no generic file edit, and a reported
result hash.

### P1 — Preserve the dumb-kernel boundary

Do not add automatic migration design, caller semantics, schema selection, or
silent dependency pulling. The observed success came from deterministic syntax
and hash bookkeeping combined with model judgment and external semantic gates.

If batching is added, make it a manifest of explicit, independently reviewed
exact edits with per-file hashes. Do not let a batch operation infer additional
edits.

### P2 — Research an opt-in usage receipt only if process logs prove insufficient

Canonical process logs were sufficient for this study, including the apply
that never launched. Do not add default telemetry. If repeated studies expose
a concrete evidence gap, consider an opt-in receipt containing tool version,
operation, file, selector, source/result hashes, match count, outcome, and
elapsed time, with explicit privacy and retention rules.

## Host Repository Recommendations

### P0 — Replace the blanket Codex test ban with capability gates

Make `AGENTS.md` point to the existing Clojure/Maven/local-lib probes. The rule
should be: run tests when prerequisites are present; skip only after recording
the failed capability check. This uploads the standard once and lets every new
agent act on the real environment.

### P0 — Turn resolved-reference inventory into the permanent guard

The migration already used clj-kondo resolved `:var-usages` to find callers.
Use that same semantic source in the boundary test instead of regex alias
parsing. Include all production dialects and add negative-control fixtures that
prove the guard catches:

- aliased, fully qualified, and `:refer` calls;
- an unauthorized backend namespace;
- a new direct SQL reference;
- a new file outside the allowlist.

A guard test needs tests of the guard itself; otherwise weakening an allowlist
or parser can silently make the build greener.

### P0 — Promote migration parity into pure, durable contract tests

Extract the comparator functions and a sanitized fixture corpus that preserves
the discovered hazards: equal sort ties, ICU collation cases, nested timestamp
numbers, duplicate IDs at distinct ordinals, nil/missing values, first/middle/
last/past-last pages, and case-variant authors. Keep the live full-corpus digest
as an operational gate, but make its semantics reproducible without databases.

### P1 — Check in the JavaScript behavior harnesses

The iPhone control and build-age probes were good tests. Put them under the
repository's JavaScript test command and run them in CI so the behavior does
not survive only as rollout history.

### P1 — Make the verification ladder executable

Provide one documented command that runs, in order:

```text
formatter → structural parse → clj-kondo → pure boundary/contract tests
          → module tests → optional live parity/deployed canaries
```

Each stage should report what it could not run and why. New features then
inherit the repository's standard in one shot instead of relying on an agent to
reconstruct it from multiple instruction files.

## Follow-up: the transcript became an executable product test

The proposed read primitive was implemented as `:show-form`, with strict
`:form` and `:line` selectors and `:cat` as a structural-shell alias. The
operation returns the exact parsed top-level source plus its location,
platforms, and complete-file hash. It fails closed on invalid, absent, or
ambiguous selectors. Bare `:cat :file ...` refuses rather than reverting to a
whole-file dump.

The first clean-context retest exposed an instruction defect rather than an
implementation defect. Fresh agents correctly chose `:show-form` and avoided
textual ranges, but both ran `:ls` first because the installed skill still said
to outline every large namespace before reading it. The skill, README, command
help, legacy skill, and repository instructions now distinguish discovery from
retrieval: use `:ls` when the relevant form is unknown; when a form name or
containing line is already known, make `:show-form` the first source inspection
and do not run `:ls` solely as a preflight.

Two new ephemeral Codex sessions then completed the named-form and line-form
tasks with one Clojure-source command each. They used neither `:ls` nor a text
range reader. Both selectors returned the same complete `format-op-help` form
and file hash. A recovery session guessed `:get`, received a nonzero structured
error, and executed the supplied `:show-form` remedy successfully without
consulting help. A final clean session applied an already reviewed replacement
plan with one direct `:replace-subform! :plan` command; it did not inspect,
edit, or regenerate the plan.

The important result is not merely that the command works. The product test
found and removed a contradictory instruction that deterministic unit tests
could not see. The transcript is now summarized in the
[Captain's Log](2026-08-02-captains-log-the-file-became-a-structural-shell.md),
and its decisive wording is protected by anti-drift assertions.

A final adversarial pass then challenged the contracts rather than the happy
path. It forced option-specific parsing, validated-only remedies, bounded
candidate evidence, explicit immutable alias input to the pure core, canonical
missing-file errors, legal `/` selection, and stronger boundary, `.cljs`, and
CLI-refusal tests. The green suite grew because critiques became regressions;
no assertion was removed or relaxed.

## Acceptance Tests for the Next clj-surgeon Improvement

The transcript itself supplies the clean-context evaluation set:

1. “Show `upsert-starred-post!` in this 1,000-line namespace.” The agent should
   use the named-form read in one command.
2. “Show the form containing line 1134.” The agent should use the line mode in
   one command.
3. “Apply this reviewed plan.” The agent should call only
   `:replace-subform!`, never `apply_patch`.
4. Unknown `:get` should fail nonzero and return a named remedy the agent can
   execute without opening global help.
5. Duplicate names must refuse. Comment forms and reader conditionals must
   return exact structural results when uniquely selected. A line between
   forms must refuse with specific EDN.
6. The README, `--help`, repository skill, and tests should all use the same
   operation name and fields, with anti-drift tests enforcing that contract.
7. Stale-hash refusal remains a separate `:replace-subform!` plan-application
   contract; a read-only `:show-form` call does not accept a stale plan.

This is how a new capability becomes one-shot: the production failure becomes
a fixture, the desired recovery becomes an executable clean-context scenario,
and docs/help/skill output become tested parts of the interface rather than
parallel prose.

## Conclusion

The production sessions validate clj-surgeon's core thesis more strongly than a
synthetic bake-off could. The tool was most valuable when it stayed modest:
map a huge namespace, prove one nested target, bind one reviewed edit to one
snapshot, then get out of the way while tests and live data judged correctness.

Its next highest-leverage feature is not more autonomous refactoring. It is the
small read primitive one agent tried to invoke through two sequential command
guesses, plus remedy-rich errors
and plan-application guidance. The larger quality gap belongs in the host
repository: preserve excellent live migration evidence as durable pure tests,
and replace stale environment assumptions with executable capability checks.

## Evidence Sources

- Five canonical local Codex rollout sessions in the fixed observation interval
- Host-repository commits that implemented the reviewed fixes
- Host-repository migration plans, Captain's Log, baseline records, and
  boundary tests at the observation cutoff
