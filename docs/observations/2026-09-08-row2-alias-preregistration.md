# Row 2 preregistration: natural alias migration with collisions

**Status:** design-only, frozen before execution. No arm, fixture build, baseline,
tool call, test, lint, grader, or timing run was performed while producing this
document. Survey freeze: 2026-09-08T07:17:28Z, taken from `date -u`.

## Decision

Use the real Marvin Voice Remote JSON-write migration recorded by commit
`5a4b4bcff5606769629147aeb9a346ae047db762`. Its parent is
`9f9cf61441a7f2a984b55226d5d1ca6cbeb6706f`. The measured task is the caller
migration that the historical commit says was performed by one
`alias_migration` call: 21 live `clojure.data.json/write-str` sites in nine
namespaces become `marvin-voice-remote.json/write` sites.

This is a natural-source fixture, not a generated corpus:

- all application and test bytes come from Gene's repository history;
- the destination `write` helper is seeded with the exact five-line hunk from
  the historical commit, matching the state that existed immediately before
  the historical migration call;
- no collision, comment, docstring, call site, namespace, or decoy is injected;
- the collision treatment is induced only by the preregistered alias preference
  `json` before `mjson`. The base already binds `json` to the old library.

The current schema can express this selected-Var task and its collision rule.
No multi-Var or new collision field is a prerequisite. It cannot express the
larger parent-to-commit task that also authors the helper body; that boundary is
made explicit below rather than hidden in the clock.

## Read-only survey and selection

The survey used only `git log`, `git show`, `git diff`, `git grep`, and `rg` over
the named paths and all locally visible refs. It did not check out or modify a
surveyed repository.

| Repository | Survey result | Disposition |
|---|---|---|
| `/home/forge/src/curtaincall-cfp` | Present; origin `https://github.com/realgenekim/curtaincall-cfp.git`. History contains large namespace splits and form moves. The strongest nearby cases (`a76f5ac…`, `65ad613…`) are multi-Var/whole-partition tasks rather than one selected-Var alias migration. | Rejected for row 2 because using them would test a different verb contract. |
| `/home/forge/src/marvin-voice-remote` | Present; origin `https://github.com/realgenekim/marvin-voice-remote.git`. Commit `5a4b4bc…` is an explicit real `alias_migration`: 21 sites, nine files, target alias `mjson`, full suite green. | **Selected.** It also contains the required protected prose and naturally occupied aliases. |
| `/home/forge/src/maven` | Absent at the specified path. | No evidence credited. |
| `/home/forge/src/kiloclaw` | Absent at the specified path. | No evidence credited. |

Two Marvin history facts make this a better collision fixture than the earlier
synthetic corpus:

1. The immediately preceding parse migration, `9f9cf614…`, had already added
   `[marvin-voice-remote.json :as mjson]` in six of the nine caller namespaces.
   The verb must reuse that binding rather than duplicate it or call it a
   collision.
2. All nine caller namespaces bind `[clojure.data.json :as json]`. In the three
   namespaces without an existing `mjson` binding—`app_route.clj`, `server.clj`,
   and `tts.clj`—the first policy candidate `json` is occupied by the old lib,
   so `mjson` must be selected as the first free candidate. This yields exactly
   three resolved collisions. In the other six files, existing-target reuse
   wins and contributes no collision.

The base also has a natural protected occurrence. The `cmd-attrs` docstring in
`src/marvin_voice_remote/bridge3_new.clj` says
`` `json/write-str` ``. It is not a live symbol and must remain byte-identical.
The historical tool commit `5a4b4bc…` preserved it. The parallel historical
native commit `73a02c1809da9e5c3ee0e2d9ce71ecae36e2203d` changed that prose to
`` `mjson/write` ``; that four-file difference is useful counterevidence and is
not the golden.

## Frozen fixture

### Provenance

| Field | Frozen value |
|---|---|
| Repository | `realgenekim/marvin-voice-remote` |
| Historical result / golden commit | `5a4b4bcff5606769629147aeb9a346ae047db762` |
| Golden commit tree | `f94b6b52a5063ceba7c40f51e6b5e227c9fd6759` |
| Parent / upstream base SHA | `9f9cf61441a7f2a984b55226d5d1ca6cbeb6706f` |
| Parent tree | `430875629dbdce501eb94218d60600d2ecee5d16` |
| Parent of upstream base | `d170f3d5edea6faa39396ea8b3418e29b2e2b4b1` |
| Base `json.clj` SHA-256 | `a5c270597b159c47ca4aa082b0c26ff08aeb0348c8b15bc0671108de2d1ca720` |
| Seeded `json.clj` SHA-256 | `5e2b4151ff2823586bdb55d0b795d4f71d744b95571ce966eb685d4b89fec410` |
| Exact helper patch SHA-256 | `fc3a7948b0bd2b7fc14cb76d94d9f41f3c626a93534e6a16d7170ad48788e9fa` |
| Caller-only golden patch SHA-256 | `0649dd0bad24821981c73355fb6299a8b959922830dd016d332bf78faedfec1b` |
| Expected caller files | 9 |
| Expected live sites | 21 |
| Expected protected textual old-name occurrences after migration in the nine caller files | 1, the `cmd-attrs` docstring |

The helper patch hash is the output of:

```sh
git -C /home/forge/src/marvin-voice-remote diff \
  9f9cf61441a7f2a984b55226d5d1ca6cbeb6706f \
  5a4b4bcff5606769629147aeb9a346ae047db762 \
  -- src/marvin_voice_remote/json.clj | sha256sum
```

The caller-only golden patch hash uses the same endpoints and excludes
`src/marvin_voice_remote/json.clj`.

### Seed construction, before every arm and outside its clock

Build one history-isolated fixture repository from the complete tree at
`9f9cf614…`; do not give it the source repository's refs or remote. Replace only
`src/marvin_voice_remote/json.clj` with the bytes at that path from `5a4b4bc…`.
Equivalently, apply only this historical hunk:

```clojure
(defn write
  "Serialize x as JSON. Options pass through to clojure.data.json/write-str."
  [x & opts]
  (apply json/write-str x opts))
```

Commit that derived seed once in the fixture repository. Before every arm,
create a fresh worktree from that single seed commit and verify:

- every path except `src/marvin_voice_remote/json.clj` is byte-identical to the
  upstream parent tree;
- `json.clj` has the seeded SHA-256 above;
- the worktree is clean;
- the fixture repository has only its seed ref and no remote, so an arm cannot
  discover or cherry-pick the historical answer;
- the task and arm-envelope hashes match the frozen cohort manifest.

The seed helper is fixture setup, not free work granted to one arm. Both arms
start with the same already-defined destination Var. Setup time is reported
separately and excluded symmetrically. No dependency or build cache is copied
from a completed arm.

### The nine caller files

```text
src/marvin_voice_remote/app_route.clj
src/marvin_voice_remote/bridge3_new.clj
src/marvin_voice_remote/channel.clj
src/marvin_voice_remote/codex_app_server.clj
src/marvin_voice_remote/director_control.clj
src/marvin_voice_remote/director_outbox.clj
src/marvin_voice_remote/reducer_session.clj
src/marvin_voice_remote/server.clj
src/marvin_voice_remote/tts.clj
```

The task's admitted path is `src`; the helper implementation is excluded so
its intentional delegation to `clojure.data.json/write-str` never becomes a
self-call.

## Exact intent and tool request

Frozen intent:

```text
from  clojure.data.json/write-str
to    marvin-voice-remote.json/write
alias preference  json, mjson, m-json, json-policy
scope src, excluding src/marvin_voice_remote/json.clj
expect 9 requiring caller files and 21 live symbol sites
```

The D runner substitutes only the fresh absolute worktree for `$WORKTREE` and
sends this single request. No `verify` field is added.

```json
{"op":"alias_migration","workspace_root":"$WORKTREE","from":{"lib":"clojure.data.json","var":"write-str"},"to":{"lib":"marvin-voice-remote.json","var":"write","alias_policy":["json","mjson","m-json","json-policy"]},"scope":{"paths":["src"],"exclude":["src/marvin_voice_remote/json.clj"]},"expect":{"files":9}}
```

Expected D receipt facts are `committed=true`, `files=9`, `sites=21`,
`alias_histogram={"mjson":9}`, `collisions_resolved=3`, no refusal, and a
durable undo receipt. Receipt claims do not replace the independent oracles.

## Arms

### N — native fastest-safe

N receives the exact task file at the end of this document and a native arm
envelope. Within its fresh worktree it may use unrestricted native `rg`, shell
scripts, and a native patch, in any order it considers fastest and safe. It may
inspect diffs and run checks. It may not call any clj-surgeon, cclsp, or
clojure-lsp read or write operation, and may not read another checkout or the
historical refs. Native helper scripts count in the primary wall and may not be
left in the submitted diff.

### D — one whole-repository migration call

D receives the same task file and a D envelope containing the exact request
above. After `orient-start`, its only source mutation authority is exactly one
`alias_migration` MCP call. It gets no preparatory `inspect_clojure`, no second
request, no request repair, no native source edit, and no fallback. After the
call it runs the repository load/tests and all arm-independent oracles. Native
read-only commands used by the runner to stage, hash, diff, lint, and grade the
finished candidate are oracle apparatus, not caller work.

A refusal, rollback, partial commit, wrong receipt, or need for a second call is
a rejected D observation. Its elapsed time is retained; it is never recoded as
a fast success.

## Cohort, schedule, and meter

Freeze model/version, runner commit, task hash, tool build/commit, MCP server
identity, CPU set, memory limit, JVM flags, environment, and cache policy before
the first floor arm. Use a fresh caller session and fresh worktree per arm. Run
all 18 attempts sequentially; no cohort arms overlap.

1. Run six fresh N controls first: `F1` through `F6`. These establish the native
   complete-wall floor and its sample standard deviation `s_N`.
2. Then run six matched N/D pairs. Counterbalance order deterministically:
   `P1 N→D`, `P2 D→N`, `P3 N→D`, `P4 D→N`, `P5 N→D`, `P6 D→N`.
3. A pair shares the same frozen inputs, caller model build, CPU set, cache
   policy, and immediately adjacent time block, but its two arms use independent
   fresh sessions and worktrees. There is no learning carry-over.

For every arm, set a unique `DOGFOOD_LOG` beneath that arm's retained artifact
directory and invoke `/var/tmp/forge/dogfood-stamp.sh` for at least:

```text
orient-start
mutation-start
mutation-end
load-start
load-end
suite-start
suite-end
kondo-start
kondo-end
golden-start
golden-end
prose-collision-start
prose-collision-end
papercut-start
papercut-end
oracle-complete
```

The primary wall is `orient-start → oracle-complete`. It includes orientation,
all reads, request composition or native scripts, the mutation, tool latency,
staging, repository load and suite, kondo, golden comparison, protected-prose
and collision checks, papercut grading, any allowed native rework in N, and all
failed attempts. The runner also records a monotonic clock around the same
boundary; UTC process stamps are the audit trail. Direct tool time and phase
times are secondary decompositions only.

Before `orient-start`, the runner writes an attestation containing seed commit
and content hashes, clean status, task and envelope hashes, model, runner,
server health identity and tool contract hash for D, CPU/memory/JVM settings,
process table, and cache state. A mismatch refuses the arm before it enters the
sample. Pre-arm fixture setup and attestation time are reported but not charged.

## Arm-independent acceptance oracle

An arm is accepted only when every gate below passes. The same scripts and
versions grade N and D. The grader sees only base bytes, candidate bytes, the
task manifest, and anonymized arm ID; it does not see route or timing.

### O1 — repository load and suite

Run sequentially in each arm:

```sh
clojure -M -e "(require 'marvin-voice-remote.core)"
make server-test-run
make runtests-once
```

All three must exit zero. The direct `require` prevents the Make target's
output pipeline from being the sole load authority. The historical receipt reports 579 tests / 7,833
assertions / zero failures; record current counts but gate on the frozen command
and zero exit, not on prose in an old receipt. Any baseline environmental
failure invalidates the affected block; it is not waived for either arm.

### O2 — structural closure and historical golden

Stage all candidate changes and compare them with the derived seed and the
caller-only portion of `5a4b4bc…`.

- The changed path set is exactly the nine caller files above. The seeded
  helper, tests, configuration, docs, comments, and other paths are unchanged.
- Exactly 21 live symbol tokens formerly resolving to
  `clojure.data.json/write-str` now resolve to
  `marvin-voice-remote.json/write`; zero selected live old tokens remain.
- Extract every top-level form touched by the historical caller patch. Compare
  candidate and golden bytes after structural alpha-normalization of only the
  target namespace alias token in the target libspec and its live qualified
  symbols. Strings, docstrings, comments, reader discards, metadata, commas,
  whitespace, and all other tokens are not normalized. Each rewritten form
  must then be byte-identical to the historical golden.
- Forms not owned by the historical caller patch are byte-identical to the
  derived seed. The only allowed `ns`-form changes are retirement of the old
  libspec and addition or reuse of one target libspec under the valid chosen
  alias.

Alias spelling is therefore not smuggled into the byte oracle. O4 independently
checks that the spelling actually obeys the frozen policy; alpha-normalization
cannot turn a wrong alias decision into an acceptance.

### O3 — clj-kondo delta

Run lint only through the serialized entrance required by repository doctrine:

```sh
~/bin/clj-kondo --lint src test --config '{:output {:format :json}}'
```

Capture the same command on the clean derived seed before the cohort. Normalize
away locations and compare the multisets for exactly `:unresolved-namespace`
and `:unused-namespace`. Candidate minus seed and seed minus candidate must both
be empty: delta 0 for both categories. Any lint error is a rejection even when
its category is outside those two deltas. Do not call an absolute Homebrew
binary.

### O4 — protected prose and collision policy

- The complete `cmd-attrs` docstring byte slice, including its
  `` `json/write-str` `` occurrence, must equal the derived seed. The raw tree
  across the nine caller files has exactly one protected old-name occurrence
  after migration, and it is that docstring; comments and other string nodes
  containing either old alias or old Var are byte-identical. The excluded
  helper's intentional `json/write-str` delegation is counted separately and
  must also remain byte-identical to the seed.
- Each caller namespace has exactly one effective alias for
  `marvin-voice-remote.json`, and no live require of `clojure.data.json` remains
  in those nine files.
- Six namespaces reuse their existing `mjson` binding. In `app_route.clj`,
  `server.clj`, and `tts.clj`, `json` is observed occupied at planning time and
  `mjson` is chosen as the first free policy entry. The collision count is
  exactly three, whether established from D details/receipt or independently
  reconstructed for N.
- No alias outside the four-entry policy is accepted. No duplicate target
  libspec, duplicate alias, or qualifier resolving to the wrong namespace is
  accepted.

### O5 — papercut grader and blind review

Run a deterministic papercut-style grader over the staged patch, then give the
same anonymized packet to two independent blind reviewers. The packet contains
the task, derived seed diff, structural/golden report, lint delta, and test
receipt; it omits arm identity, transcript, tool receipt wording, and timing.
Reviewers rule independently before seeing each other. Disagreement is retained
and adjudicated against this frozen rubric, never averaged.

| Severity | Reviewer wart | Ruling |
|---|---|---|
| Critical | A live old call remains; a target call resolves to the wrong lib/Var; tests/load fail; a collision is shadowed rather than resolved; or the helper calls itself. | Reject. Hard correctness kill if D. |
| Critical | Any string, docstring, comment, reader-discard, metadata, unrelated form, or non-caller path is semantically rewritten. | Reject. The `cmd-attrs` prose is the named canary. |
| Major | Missing, unused, duplicated, or off-policy require/alias; target alias differs between files without a policy reason; old require remains after its last live use. | Reject. |
| Major | Rewritten owner differs from normalized historical golden, or unrelated formatting/require reordering enlarges the patch. | Reject unless the frozen oracle proves the difference is solely a permitted alias spelling. |
| Minor | Awkward but valid require placement, noisy whitespace, detached comment spacing, avoidable line wrapping, or receipt/detail wording that would make a reviewer re-open the files. | Count and report. Any D minor count above the historical golden's count rejects D; N is scored by the same rule. |
| None | Minimal token splices, one valid target binding per caller, protected prose intact, no unrelated churn, and all evidence locates its subject. | Accept. |

Every D must be accepted by O1–O5. Papercut score is not averaged with
correctness and cannot rescue a failed oracle.

## Registered statistics and win gates

Let `F_1…F_6` be the six initial N complete walls and `s_N` their sample standard
deviation. Let `N_i,D_i` be the complete walls in matched pair `i`.

The **primary Astra gate** is conjunctive. D wins only if:

1. all six D runs are accepted by every oracle, with no request repair, second
   call, operator intervention, or native fallback;
2. `median_i(D_i / N_i) ≤ 0.75`;
3. `median_i(N_i - D_i) ≥ 90 seconds`; and
4. `median_i(N_i - D_i) > 2 × s_N`.

Report individual walls, pair ratios, pair differences, medians, `s_N`, first
attempt/refusal/fallback rates, and acceptance before stating a verdict. Do not
pool the six floor controls into the paired effect.

The fixed **stricter secondary** is `median_i(D_i / N_i) ≤ 0.70`, i.e. at least
30% paired complete-wall reduction. It is reported pass/fail after the primary
gate and never substituted for it. No post-result threshold change, outlier
deletion, suite subtraction, direct-tool-only clock, or alternate endpoint is
allowed.

## Falsifiers, stopping, and kill condition

The design is falsified, and row 2 is not widened, by any of:

- any D refusal, rollback, partial write, second-call need, native repair, or
  operator intervention;
- any D failure of the historical golden, protected-prose, collision, kondo,
  load, suite, or papercut oracle;
- a D receipt claiming closure while an independent oracle finds a missed or
  wrongly resolved site;
- fewer or more than nine caller files or 21 live sites on the frozen seed;
- the collision behavior not being expressible as first reusable, otherwise
  first unbound policy entry;
- failure of any one of the four primary timing/acceptance clauses.

**Hard kill:** on the first D correctness failure—including a false successful
receipt—stop launching further arms, preserve the worktree, request/response,
undo data, process stamps, and graders, and suspend the entire automatic
`alias_migration` route until the defect has a fail-first natural witness, a
fix, independent review, and a fresh preregistered cohort. Do not retry the
committed migration blindly.

**Safe refusal/futility stop:** a source-unchanged typed refusal makes 6/6 D
acceptance impossible. Preserve it and stop the remaining cohort rather than
spend the battery. This blocks collision/natural-task widening but does not by
itself erase the old no-collision synthetic result.

**Performance loss:** if correctness is perfect but the complete-wall gate
fails, retain the existing narrow no-collision route only within its old
evidence boundary; do not widen to natural collision tasks. A systematic D wall
loss triggers a routing review against this class's controls, not a claim that
all possible alias migrations lose.

## Schema admission before the cohort

For this selected task, the closed schema already supplies the necessary
capabilities:

- selected-Var migration (`from.var` and `to.var`);
- deterministic per-file collision handling through ordered `alias_policy`;
- reuse of an alias already bound to `to.lib`;
- `scope.paths`, `scope.exclude`, and an expected file count;
- strings/docstrings/comments excluded from symbol rewriting.

Therefore **no schema change is admitted before this cohort**. In particular,
do not add multi-Var, per-file alias tables, or a separate collision map; those
would change the treatment and require a new preregistration.

There is one explicit boundary. Starting at the literal parent commit, the full
historical commit also adds the implementation of `mjson/write`. The present
verb cannot author that definition. If common seeding of the already-decided
destination helper is rejected, **do not run this cohort**. Before a true
parent-to-whole-commit cohort the verb would need one atomic, guarded
`target_definition`/prelude contract (exact destination file, exact new
top-level form, absent-before expectation, collision/migration in the same
transaction, and repository proof/rollback). That is not a multi-Var problem
and must not be approximated with an unmetered native edit in D.

The earlier parse migration `9f9cf614…` is also not a substitute fixture:
`json/read-str … :key-fn keyword` became `mjson/parse …`, changing argument
shape as well as Var identity. Supporting that task would require a bounded
call-shape rewrite rule with exact arity/option predicates, not merely
multi-Var or collision policy.

## Retained artifacts and freeze rule

Retain every clean seed manifest, attestation, task/envelope hash, transcript,
request/response, tool detail and undo receipt, staged patch, complete stdout
and stderr for every oracle, kondo JSON, golden report, papercut reports,
process stamp log, monotonic wall, and final status through adjudication. Arm
IDs are unique and no path is reused.

Immediately before any future execution, hash this design and the exact task
file block below into the cohort manifest. Any textual change after the first
arm creates a new design version and invalidates mixing old and new attempts.
This document authorizes no run by itself.

## One-screen cohort table

| Stage | IDs | Count | Fixed route/order | Freshness | Primary endpoint |
|---|---:|---:|---|---|---|
| Native floor | F1–F6 | 6 N | N, N, N, N, N, N | fresh session + isolated worktree each | `orient-start → oracle-complete` |
| Matched pair 1 | P1-N, P1-D | 2 | N→D | fresh/fresh, sequential | all O1–O5 complete |
| Matched pair 2 | P2-D, P2-N | 2 | D→N | fresh/fresh, sequential | all O1–O5 complete |
| Matched pair 3 | P3-N, P3-D | 2 | N→D | fresh/fresh, sequential | all O1–O5 complete |
| Matched pair 4 | P4-D, P4-N | 2 | D→N | fresh/fresh, sequential | all O1–O5 complete |
| Matched pair 5 | P5-N, P5-D | 2 | N→D | fresh/fresh, sequential | all O1–O5 complete |
| Matched pair 6 | P6-D, P6-N | 2 | D→N | fresh/fresh, sequential | all O1–O5 complete |
| **Total** |  | **18 attempts: 12 N, 6 D** | no overlap | one seed, no history leakage | proof-inclusive complete wall |

## Exact `task.md` received byte-for-byte by every arm

```markdown
# Task: centralize JSON serialization

The destination function `marvin-voice-remote.json/write` already exists and
preserves the arguments and result of `clojure.data.json/write-str`.

Migrate every live Clojure reference under `src` from
`clojure.data.json/write-str` to `marvin-voice-remote.json/write`, excluding
`src/marvin_voice_remote/json.clj` itself. There are exactly 21 live sites in
exactly 9 caller files.

For the target namespace, use this ordered alias preference:

1. `json`
2. `mjson`
3. `m-json`
4. `json-policy`

In each namespace, reuse a preferred alias already bound to the target
namespace; otherwise choose the first preferred alias not already bound in that
namespace. Do not shadow or replace an alias bound to another namespace. Retire
the old require when its final live use is gone, and leave exactly one effective
target libspec.

Do not rewrite strings, docstrings, comments, reader-discarded forms, metadata,
or unrelated code. In particular, prose that mentions `json/write-str` is
historical explanation and must remain unchanged. Do not modify the destination
helper, tests, configuration, documentation, or files outside the nine callers.

Finish only when the repository load and full suite pass and the submitted diff
contains only the intended minimal migration.
```
