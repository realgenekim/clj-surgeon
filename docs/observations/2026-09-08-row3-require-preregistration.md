# Row 3 preregistration: natural `require_change` across namespaces

**Status:** design-only, frozen before execution. No arm, fixture build,
baseline, tool call, load, test, lint, grader, or timing run was performed while
producing this document. Survey freeze: 2026-09-08T07:39:41Z, taken from
`date -u`.

**Admission status:** **DO NOT RUN.** The natural task selected below is outside
the schema at `/home/forge/src/clj-surgeon` HEAD
`1f962abc1447c594c9fc76636273f7d81fc59e72`: `require_change` is coupled to a
nonempty `symbol_migration`, accepts one exact global alias instead of an alias
policy, and refuses every comment-bearing require clause. The first product gain
required before this cohort is frozen under “Schema admission before the
cohort.” A future run must freeze the admitted schema/tool hashes in an addendum
before `F1`; it may not weaken this task, fixture, schedule, endpoint, oracle, or
gate.

## Decision

Use the real Marvin Voice Remote JSON-read policy migration recorded by commit
`9f9cf61441a7f2a984b55226d5d1ca6cbeb6706f`. Its parent is
`d170f3d5edea6faa39396ea8b3418e29b2e2b4b1`. That commit introduced
`marvin-voice-remote.json/parse`, migrated nine call sites, and added
`[marvin-voice-remote.json :as mjson]` to exactly eight existing namespaces.

The measured task is the require-only last step of that historical change. The
derived seed contains every non-require byte from the historical result,
including the new policy namespace and the nine already-migrated call sites,
but omits the one new target libspec from each of the eight callers. Both arms
must add that require, and nothing else.

This is a natural-source fixture, not a generated fan-out:

- every source byte is from Marvin history;
- all eight namespaces and their code, docstrings, aliases, whitespace, and
  comments are real;
- no caller, collision, comment, libspec, blank line, or decoy is injected;
- all eight parents already bind `json` to `clojure.data.json`, so the first
  alias preference is naturally occupied and `mjson` is the first free choice;
- `channel.clj` naturally contains four multi-line comment runs inside its
  `:require` block. In particular, the durable-capture comment trails
  `[marvin-voice-remote.blob :as blob]` and precedes the
  `capture-archive` libspec. Those bytes and that attachment must survive;
- the historical commit retained `clojure.data.json :as json` because live JSON
  write sites still existed. This task adds one require; it removes none.

The historical commit is the semantic golden. Row 3 additionally requires a
deterministic minimal sorted insertion, so the golden comparator canonicalizes
only the position and indentation of the one added target libspec before
comparison. It does not normalize comments, aliases, existing libspec order,
blank lines, or any non-`ns` byte. This qualification is fixed before results:
the historical commit's raw insertion positions are provenance, while the
layout oracle below is the presentation authority.

## Read-only survey and selection

The survey used only `git log`, `git show`, `git diff`, `git grep`, `git
rev-parse`, `git cat-file`, and `rg` over the three named repositories and all
locally visible refs. It did not fetch, pull, check out, reset, commit, or modify
any surveyed repository.

| Repository | Survey result | Disposition |
|---|---|---|
| `/home/forge/src/curtaincall-cfp` | Present; surveyed HEAD `00e8f0fae2c19258f0eab008e6b02caa8545591d`. Commit `a76f5ac…` adds `exports.calendar` to nine callers but has neither a naturally occupied target alias nor an in-block comment canary. Commit `225b4b7…` adds `web.links` to twenty namespaces but likewise lacks the required collision/comment combination. The large split/checkpoint commits (`c429961…`, `0fb346a…`, `b4b2103…`, `65ad613…`) create or repartition many namespaces and would confound a require-only calibration with a source split. | Rejected for row 3. |
| `/home/forge/src/marvin-voice-remote` | Present; surveyed HEAD `d170f3d5edea6faa39396ea8b3418e29b2e2b4b1`. Commit `9f9cf614…` changes exactly eight existing require blocks, has the natural occupied `json` alias in all eight, and includes the comment-bearing `channel.clj` require block. Commit `d3df192…` also adds one require to eight namespaces, but one is a newly created self-test and the selected alias is unopposed. | **Selected.** |
| `/home/forge/src/clj-surgeon` | Present; surveyed HEAD `1f962abc1447c594c9fc76636273f7d81fc59e72`. Large repeated require additions are predominantly implementation/test-fixture construction, benchmark corpora, or whole-feature landings rather than one production require intent over eight existing natural callers. Its current schema is, however, the authoritative evidence for the admission gap. | Rejected as a natural fixture; used only to audit expressibility. |

No absent repository and no unobserved remote ref is credited. The survey is a
selection procedure, not evidence that no other qualifying commit exists.

## Frozen fixture

### Provenance

| Field | Frozen value |
|---|---|
| Repository | `realgenekim/marvin-voice-remote` |
| Historical result / semantic golden commit | `9f9cf61441a7f2a984b55226d5d1ca6cbeb6706f` |
| Golden commit tree | `430875629dbdce501eb94218d60600d2ecee5d16` |
| Parent / upstream base SHA | `d170f3d5edea6faa39396ea8b3418e29b2e2b4b1` |
| Parent tree | `c015c86338385491307a2b4bc4c34f5b188a91cf` |
| Historical full/source patch SHA-256 | `8461ce4d03e4de166c95f879e976b1a4b0c047698a64e1177c1b279ba39343fc` |
| Expected changed require files | 8 |
| Expected added target libspecs | 8 |
| Expected target alias | `mjson` in all 8 files |
| Expected occupied first preference | `json` in all 8 files, bound to `clojure.data.json` |
| Expected removals | 0 |
| Expected non-`ns` changes | 0 |
| Named comment canary | complete four comment runs inside `channel.clj`'s `:require` block |

The historical patch hash is the output of the following read-only survey
command; it is recorded as provenance, not rerun authorization:

```sh
git -C /home/forge/src/marvin-voice-remote diff --binary \
  d170f3d5edea6faa39396ea8b3418e29b2e2b4b1 \
  9f9cf61441a7f2a984b55226d5d1ca6cbeb6706f | sha256sum
```

### The eight caller files

```text
src/marvin_voice_remote/bridge3_new.clj
src/marvin_voice_remote/capture_archive.clj
src/marvin_voice_remote/channel.clj
src/marvin_voice_remote/codex_app_server.clj
src/marvin_voice_remote/director_control.clj
src/marvin_voice_remote/director_outbox.clj
src/marvin_voice_remote/groq.clj
src/marvin_voice_remote/reducer_session.clj
```

For fixture-construction verification, the historical parent/golden Git blob
IDs are frozen:

| File | Parent blob | Golden blob |
|---|---|---|
| `bridge3_new.clj` | `cfde91852f093b589611e58e7adac37bf60c05bd` | `75e536304464307be93ee05ab2e5fd409c6e7a94` |
| `capture_archive.clj` | `27b4781f089147738b1b3c56888881799be736ac` | `fbdfbbae575d3273d6d26a55ee68eea515aca72e` |
| `channel.clj` | `254cc3c3d4be8bcc3c7b52f9742a19c202dc70c6` | `4b5384222d2cd62785068edd65e31c7dbbd489af` |
| `codex_app_server.clj` | `5fc9ab8f92cd66097d9152bbf349ef759d489f1b` | `f267c98db7a0f33df83d28bea7ae3a0d1f7c38db` |
| `director_control.clj` | `20d44fc519628291ee17f6f6b4f1ba7820366565` | `091dc37c7e8e3b1d3a4ec5789ca7f8eaa5302d13` |
| `director_outbox.clj` | `e08856ae041ed7c48e9bc34da850cdd7ee9915a0` | `d4089fef1053331841cd7a3e3bd57e866ddd7c0a` |
| `groq.clj` | `f1cabe0d15e3ed5f2a3a44df1cca12d960948f19` | `3046a01df3a21842cdfd36685e16ad7a66300392` |
| `reducer_session.clj` | `abb277dfa123d1877c547db8cdedc3f04ddbc513` | `6fb5b27c39d0d3b169d484e5951c589b397e6264` |

### Seed construction, before every arm and outside its clock

Build one history-isolated fixture repository from the complete tree at
`9f9cf614…`; do not give it the source repository's refs or remote. In exactly
the eight files above, remove exactly one direct libspec whose parsed value is
`[marvin-voice-remote.json :as mjson]`. Remove its owned indentation and line
ending only. Do not parse/reprint the enclosing `ns` form and do not touch any
other byte. Commit that derived tree once as the cohort seed.

Before every arm, create a fresh worktree from that one seed commit and verify:

- all paths outside the eight callers are byte-identical to tree `430875…`;
- every caller is byte-identical to its golden blob after deleting exactly the
  one target libspec line;
- `src/marvin_voice_remote/json.clj` is byte-identical to the historical golden
  and defines `parse`;
- there are exactly nine live `mjson/parse` sites in the eight callers, exactly
  zero target libspecs, and exactly eight `clojure.data.json :as json` bindings;
- `channel.clj`'s complete require-comment byte slices equal the historical
  golden;
- the worktree is clean, has only its seed ref, and has no remote;
- task, arm-envelope, seed-manifest, and oracle hashes equal the frozen cohort
  manifest.

The intentionally unresolved `mjson` qualifier makes the seed a faithful
pre-require state, not a baseline that is expected to load. Both arms receive
the same state. Fixture construction and attestation are reported separately
and excluded symmetrically from primary wall. No cache or artifact is copied
from a completed arm.

### Historical require-only golden

The golden delta is derived from the seed by restoring exactly one target
libspec in each of the eight files and changing no other byte. The semantic
golden comparison parses only the direct `:require` clause and asserts:

```clojure
{:add {:lib marvin-voice-remote.json :as mjson}
 :files 8
 :remove []}
```

For raw-byte comparison, candidate and historical require blocks are each
projected to the O5 canonical insertion position and indentation. The projected
diffs must then be byte-identical. This normalization is deliberately limited
to the single added libspec's leading whitespace and position. It cannot hide a
different alias, a changed existing libspec, moved comment, reordered existing
entry, blank-line change, or any non-`ns` churn.

## Exact intent and post-admission tool request

Frozen intent:

```text
add lib              marvin-voice-remote.json
alias preference     json, mjson, m-json, json-policy
scope                 the 8 named files only
expect                8 changed files, 8 added libspecs, 0 removals
existing requires     preserve all, including clojure.data.json :as json
comments              preserve byte-for-byte and keep attached to their entries
layout                stable sorted insertion, inherited indent, no blank lines
```

There is **no valid request under the surveyed current schema**. Supplying only
`require_change` fails the published pair constraint; adding a fabricated
`symbol_migration` changes the treatment and is forbidden; using exact
`"as":"mjson"` removes the collision decision; and `channel.clj` reaches the
typed refusal `Comment-bearing require clauses are unsupported`.

After the prerequisite is admitted, D sends exactly one request of this frozen
logical shape. The runner substitutes only the fresh absolute worktree for
`$WORKTREE`. The admitted public schema may choose JSON field spelling, but its
meaning, file order, counts, and one-call boundary may not change; the final
byte-for-byte request and schema hash must be frozen in the pre-`F1` addendum.

```json
{"op":"require_change","workspace_root":"$WORKTREE","add":{"lib":"marvin-voice-remote.json","alias_policy":["json","mjson","m-json","json-policy"]},"files":[{"file":"src/marvin_voice_remote/bridge3_new.clj"},{"file":"src/marvin_voice_remote/capture_archive.clj"},{"file":"src/marvin_voice_remote/channel.clj"},{"file":"src/marvin_voice_remote/codex_app_server.clj"},{"file":"src/marvin_voice_remote/director_control.clj"},{"file":"src/marvin_voice_remote/director_outbox.clj"},{"file":"src/marvin_voice_remote/groq.clj"},{"file":"src/marvin_voice_remote/reducer_session.clj"}],"layout":{"order":"stable-lib-symbol","comments":"attach-following","blank_lines":0},"expect":{"files":8,"adds":8,"removes":0}}
```

Expected D receipt facts are `state=committed`, `files=8`, `adds=8`,
`removes=0`, `alias_histogram={"mjson":8}`, `collisions_resolved=8`, no
refusal, written-byte hashes for all eight files, and durable undo data. The
receipt must say that the comment-bearing clause was handled without protected
byte loss. Receipt claims never replace the arm-independent oracles.

## Arms

### N — native fastest-safe, unrestricted

N receives the exact task file at the end of this document and a native arm
envelope. Inside its fresh worktree it may use unrestricted native `rg`, shell,
scripts, and one or more native patches in whatever order it considers fastest
and safe. It may inspect files and diffs, repair its work, and run checks. It may
not call clj-surgeon, cclsp, or clojure-lsp, and it may not read another checkout
or any historical ref. Native helper scripts count in primary wall and may not
remain in the submitted diff.

“Unrestricted” means the control is not forced into eight manual writes. A
guarded native script or one batched patch is allowed; otherwise the experiment
would compare D with an intentionally weakened control.

### D — one whole-intent `require_change` call

D receives the same task file and a D envelope containing the frozen request.
After `orient-start`, its only source mutation authority is exactly one
`require_change` MCP call. It gets no preparatory `inspect_clojure`, no second
mutation call, no request repair, no native source edit, and no fallback. After
the call, it runs the repository load/tests and all arm-independent oracles.
Runner-owned read-only staging, hashing, linting, and grading are oracle
apparatus, not caller mutation work.

A refusal, rollback, partial commit, false success receipt, second-call need,
or native repair is a rejected D observation. Its full elapsed wall is retained
and is never recoded as a fast success. The cohort cannot start while the
admission status at the top of this document remains DO NOT RUN.

## Cohort, schedule, and meter

Freeze model/version, runner commit, task hash, tool build/commit, admitted
schema/request hash, MCP server identity, CPU set, memory limit, JVM flags,
environment, and cache policy before the first floor arm. Use a fresh caller
session and fresh worktree per arm. Run all 18 attempts sequentially; no cohort
arms overlap.

1. Run six fresh N controls first: `F1` through `F6`. These establish the native
   proof-inclusive complete-wall floor and its sample standard deviation `s_N`.
2. Then run six matched N/D pairs. Counterbalance order deterministically:
   `P1 N→D`, `P2 D→N`, `P3 N→D`, `P4 D→N`, `P5 N→D`, `P6 D→N`.
3. A pair shares frozen inputs, caller build, CPU set, cache policy, and adjacent
   time block, but its two arms use independent sessions and worktrees. There is
   no transcript, diff, cache, or worktree learning carry-over.

For every arm, set a unique `DOGFOOD_LOG` under that arm's retained artifact
directory. Invoke `/var/tmp/forge/dogfood-stamp.sh` for at least:

```text
orient-start
discovery-start
discovery-end
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
zero-churn-start
zero-churn-end
layout-start
layout-end
blind-review-start
blind-review-end
oracle-complete
```

Every stamp is emitted by the process at the boundary, never typed into a
receipt afterward. The runner also records monotonic start/end clocks, child
PIDs, process start ticks, exit status, and peak RSS for each phase. A phase
without matching start/end stamps and a terminal child status is incomplete.

The primary wall is `orient-start → oracle-complete`. It includes orientation,
all reads and discovery, request composition or native scripting, mutation and
tool latency, staging, repository load, both test commands, kondo, historical
golden comparison, byte-level zero-churn grading, layout grading, blind-review
packet creation and decisions, all allowed N repairs, and all failed attempts.
Direct tool time, mutation time, test time, oracle time, model returns, and
caller actions are secondary decompositions only. No proof time is subtracted
from either arm.

Before `orient-start`, the runner writes an attestation containing seed commit
and file hashes, clean status, task/envelope/oracle hashes, model, runner,
server health identity and admitted tool/schema hash for D, CPU/memory/JVM
settings, process table, temp root, and cache state. Any mismatch refuses the
arm before it enters the sample. Pre-arm construction and attestation time are
reported but not charged.

## Arm-independent acceptance oracle

An arm is accepted only when O1 through O5 all pass. The same frozen scripts and
versions grade N and D. The grader sees only seed bytes, candidate bytes,
historical golden bytes, task manifest, and anonymized arm ID; it does not see
route, transcript, tool receipt language, or timing.

### O1 — repository load and suite

Run sequentially in every arm:

```sh
clojure -M -e "(require 'marvin-voice-remote.core)"
make server-test-run
make runtests-once
```

All three must exit zero. Record test/assertion counts and stderr, but gate on
the frozen commands and zero exits rather than a count remembered from history.
A baseline environmental failure invalidates the affected block for both arms;
it is not waived or scored as a D refusal.

### O2 — historical golden diff

Stage every candidate change and compare seed → candidate with the derived
require-only projection of `9f9cf614…`:

- the changed path set is exactly the eight caller files;
- each changed file adds exactly one effective require of
  `marvin-voice-remote.json` under `mjson`;
- no effective require is removed or changed;
- all eight pre-existing `clojure.data.json :as json` libspecs remain;
- after canonicalizing only the target line's position/indent under O5, the
  staged patch is byte-identical to the derived historical require-only golden;
- no live symbol, call site, string, docstring, comment, reader discard,
  metadata, import, or non-`ns` form differs from the seed.

The golden comparison is a gate, not a similarity score. A different but
compiling require strategy fails.

### O3 — clj-kondo delta 0

Run lint only through the serialized entrance required by repository doctrine:

```sh
~/bin/clj-kondo --lint src test --config '{:output {:format :json}}'
```

Before the cohort, run that command once on a history-isolated exact checkout
of golden tree `430875…` under the frozen environment and retain its normalized
JSON as the reference. Candidate and golden diagnostic multisets must be equal
after removing paths and line/column locations: candidate-minus-golden and
golden-minus-candidate are both empty. In particular, deltas for
`:unresolved-namespace`, `:unused-namespace`, `:redefined-var`, and all error
levels are zero. Any candidate lint error absent from the golden is rejection.
The intentionally broken derived seed is not the lint baseline.

### O4 — zero churn on untouched lines, byte-level

For every changed file, map the candidate's one added libspec line to the
seed's `:require` block. Delete only that line and its owned indentation/newline
from the candidate; the complete remaining file must be byte-identical to the
seed. Additionally:

- every existing libspec line and separator is byte-identical and in the same
  relative order;
- every comment byte in `channel.clj` is identical and each contiguous comment
  run remains attached to the same following libspec;
- line-ending style and final newline are unchanged;
- the destination policy source file and every path outside the eight callers are
  byte-identical to the seed;
- the candidate contains exactly eight new lines attributable to the task and
  zero other added, deleted, or modified lines.

This oracle is raw bytes, not an AST or formatter equivalence. It is the direct
test of the historical “nine namespaces, zero churn” claim against native.

### O5 — require-block layout and blind review

The deterministic layout grader checks each `:require` clause without
formatting it:

1. Parse existing direct libspecs as immutable groups. A contiguous comment run
   immediately before a libspec belongs to that following libspec.
2. Insert the target group immediately before the first existing group whose
   lib symbol sorts after `marvin-voice-remote.json` by ordinary Unicode code
   point order; if none does, insert last. Preserve the relative order of all
   inherited groups, even if the inherited block is not globally sorted.
3. Never split a comment from its following libspec. The new comment-free group
   may appear before an attached comment run, never between that run and its
   libspec.
4. Copy the indentation and line-ending convention of the nearest direct
   libspec in that clause. Do not convert hanging `(:require [x ...]` style to
   block style or vice versa.
5. Introduce no blank line before, after, or inside the require block.

Then send the same anonymized packet to two independent blind reviewers. The
packet contains the task, seed/candidate diff, normalized historical-golden
report, kondo delta, load/test receipt, zero-churn report, and layout report. It
omits arm identity, transcript, tool receipt wording, and timing. Reviewers rule
independently; disagreement is retained and adjudicated against this frozen
rubric, never averaged.

| Severity | Reviewer wart | Ruling |
|---|---|---|
| Critical | Missing target require; target resolves under a wrong alias; occupied `json` is shadowed/rebound; load/tests fail; or a success receipt disagrees with bytes. | Reject. Hard correctness kill if D. |
| Critical | Any existing require, comment, string, docstring, reader discard, metadata, import, non-`ns` form, or other path is semantically changed. | Reject. The `channel.clj` comment runs are named canaries. |
| Major | Duplicate target libspec, alias outside policy, removal of `clojure.data.json`, changed file set/count, or unresolved/unused namespace delta. | Reject. |
| Major | Target is not at the O5 stable sorted insertion point, inherited indentation changes, a comment detaches, an inherited entry reorders, or a blank line appears. | Reject. |
| Minor | Receipt evidence is hard to locate, output contains avoidable noise, or a reviewer must reopen source despite all deterministic gates passing. | Count and report. Any D minor count above the accepted N median rejects D for widening, though correctness remains reported separately. |
| None | Eight minimal target-line insertions, `mjson` chosen because `json` is occupied, comments untouched, layout exact, proof complete. | Accept. |

Every D must be accepted by O1–O5. Quality is never averaged with timing and
cannot rescue a failed acceptance gate.

## Registered statistics and win gates

Let `F_1…F_6` be the six initial N proof-inclusive complete walls and `s_N`
their sample standard deviation. Let `N_i,D_i` be the complete walls in matched
pair `i`.

The **primary Astra gate** is conjunctive. D wins only if:

1. all six D runs are accepted by every oracle, with no request repair, second
   call, operator intervention, or native fallback;
2. `median_i(D_i / N_i) ≤ 0.75`;
3. `median_i(N_i - D_i) ≥ 90 seconds`; and
4. `median_i(N_i - D_i) > 2 × s_N`.

Report all floor walls, paired walls, ratios, differences, medians, `s_N`,
first-attempt/refusal/fallback/intervention rates, acceptance, caller actions,
model returns, and apparatus share before stating a verdict. Do not pool the
six floor controls into the paired effect and do not exclude rejected D walls.

The fixed **30% secondary gate** is
`median_i(D_i / N_i) ≤ 0.70`. Report it pass/fail after the primary gate. It is
stricter on proportional wall only and never replaces the four-part primary
gate. No post-result threshold change, outlier deletion, proof subtraction,
suite subtraction, direct-tool-only clock, or alternate endpoint is allowed.

Passing both gates moves row 3 from **uncalibrated capability** to a witnessed
win only for this exact eight-file CLJ, direct-require, collision-policy,
comment-bearing contract. It does not establish a generic require editor,
reader-conditionals, `:refer`, prefix lists, CLJC/CLJS, unknown-file discovery,
or arbitrary require reordering.

## Falsifiers, stopping, and kill conditions

The design is falsified, and row 3 remains uncalibrated, by any of:

- the frozen seed does not contain exactly eight target-free callers, nine live
  `mjson/parse` sites, eight occupied `json` aliases, and the named comment
  bytes;
- the admitted one-call request cannot represent the exact file set, ordered
  alias policy, no-removal intent, comment attachment, and layout rule;
- any D refusal, rollback, partial write, second-call need, request repair,
  native mutation, fallback, or operator intervention;
- any D failure of load, suite, historical golden, kondo delta, byte-level
  zero-churn, layout, or blind review;
- a D receipt claims closure while an independent oracle finds a missed file,
  wrong alias, detached comment, wrong position, or extra byte;
- any one of Astra's four primary acceptance/timing clauses fails.

**Pre-cohort schema stop:** the known current-schema refusal is not an
observation and must not be spent as `D1`. If the prerequisite contract cannot
pass its faithful natural fixture and negative canaries before cohort freeze,
do not launch `F1`.

**Hard correctness kill:** on the first D correctness failure—including a false
successful receipt—stop launching further D or N arms, preserve the worktree,
request/response, undo data, process stamps, diffs, and graders, and suspend any
proposed automatic `require_change` route. The row remains uncalibrated until a
fail-first natural witness, linked fix, independent review, and fresh
preregistration exist. Never reapply a committed mutation blindly.

**Safe refusal/futility stop:** a source-unchanged typed D refusal makes 6/6 D
acceptance impossible. Preserve it and stop the remaining cohort. It does not
turn the historical zero-churn observation into a loss or win; it proves only
that the admitted contract failed this natural task.

**Performance loss:** if all D runs are correct but the complete-wall gate
fails, row 3 stays uncalibrated as a performance route. A systematic D loss is
recorded for this exact contract and blocks automatic routing here. It is not
generalized to every require task or file count.

**Apparatus falsifier:** if fixture setup leaks history, arm artifacts overlap,
process identity is unverified, grading is route-aware, or the primary endpoint
omits any O1–O5 work, invalidate the affected block. Apparatus defects are not
repaired by relabeling failed observations.

## Schema admission before the cohort: what the verb must gain first

The survey shows that today's schema cannot express the natural task. The first
gain must be a **standalone, whole-intent `require_change` operation** rather
than another prompt example or a dummy `symbol_migration`. Its smallest admitted
contract for this cohort is:

1. `require_change` is independently callable with one target lib, an ordered
   alias policy, an explicit nonempty file vector, and exact expected
   file/add/remove counts. It does not require or synthesize symbol edits.
2. Per file, reuse an existing policy alias already bound to the target;
   otherwise choose the first policy alias not bound to another lib. Here that
   must observe `json` occupied and choose `mjson` eight times. No per-file alias
   answer may be supplied by the runner.
3. Comment-bearing direct require clauses are supported by token splicing.
   Comments remain byte-identical and attached to the same following libspec;
   the operation never parses/reprints the whole namespace or file.
4. The insertion point and indentation are the deterministic O5 rules. Existing
   libspecs and separators are immutable, and blank-line count cannot change.
5. The transaction is all-or-nothing over eight frozen source hashes and emits
   a bounded receipt with per-file chosen alias, collision evidence, byte
   hashes, commit state, and durable undo authority.

All five are one product seam, not optional follow-up polish: standalone
admission without comment preservation still refuses the selected natural
file; comment preservation without collision policy makes the runner answer the
task; and either without atomic multi-file commit is not the row-3 verb.

Before the cohort, this gain needs linked intent, faithful fail-first witnesses
copied from the eight historical namespace forms, and at least these canaries:

- positive: all eight additions in one transaction, including `channel.clj`;
- positive: `json` occupied in every file yields `mjson` in every file;
- positive: compact and block-style require indentation both match O5;
- negative: all policy aliases occupied refuses with source unchanged;
- negative: stale source hash, duplicate target lib, ambiguous/multiple direct
  require clauses, and unsupported reader conditional each refuse atomically;
- negative: any lost/moved comment or non-target byte blocks commit;
- receipt/undo: independent byte replay restores the exact seed.

The current authoritative evidence is:

```text
/home/forge/src/clj-surgeon HEAD
  1f962abc1447c594c9fc76636273f7d81fc59e72
mcp_schema.clj SHA-256
  3813372fdd85ab89433cb9203c82a9c0673f3e0e7bda31a0bf16f91a8b1cf58f
mcp_compact_relations.clj SHA-256
  505bf95df971625dd5218896b07de8c0b067c9925ea83d29b714e4ca65b7be50
```

That version says “paired with symbol_migration,” enforces equal file vectors
and target aliases across the pair, and throws `Comment-bearing require clauses
are unsupported`. No run may conceal those facts with a bespoke native pre-edit
inside D. A broader schema—removals inferred from use, unknown-file discovery,
`:refer`, CLJC, or arbitrary ns normalization—is explicitly out of scope and
would require a new design.

## Retained artifacts and freeze rule

Retain the seed manifest, attestation, task/envelope/schema/tool hashes,
transcript, request/response, refusal or receipt, per-file collision decisions,
undo data, staged patch, stdout/stderr and exit for every load/test/lint command,
kondo JSON/reference, historical-golden projection, zero-churn report, layout
report, blind reviews, process stamps, monotonic wall, action/return census, and
final status through adjudication. Arm IDs and paths are unique and never reused.

Immediately before future execution, hash this design and the exact task block
below into the cohort manifest. The required admission addendum may fill only
the implementation/tool/schema/request hashes and exact mechanically equivalent
JSON field spelling. Any change to task meaning, fixture, arm authority,
schedule, endpoint, oracle, or gate creates a new design version and forbids
mixing attempts. This document authorizes no run by itself.

## One-screen cohort table

| Stage | IDs | Count | Fixed route/order | Freshness | Primary endpoint |
|---|---:|---:|---|---|---|
| Native floor | F1–F6 | 6 N | N, N, N, N, N, N | fresh session + isolated worktree each | `orient-start → oracle-complete` |
| Matched pair 1 | P1-N, P1-D | 2 | N→D | fresh/fresh, sequential | O1–O5 and both blind rulings complete |
| Matched pair 2 | P2-D, P2-N | 2 | D→N | fresh/fresh, sequential | O1–O5 and both blind rulings complete |
| Matched pair 3 | P3-N, P3-D | 2 | N→D | fresh/fresh, sequential | O1–O5 and both blind rulings complete |
| Matched pair 4 | P4-D, P4-N | 2 | D→N | fresh/fresh, sequential | O1–O5 and both blind rulings complete |
| Matched pair 5 | P5-N, P5-D | 2 | N→D | fresh/fresh, sequential | O1–O5 and both blind rulings complete |
| Matched pair 6 | P6-D, P6-N | 2 | D→N | fresh/fresh, sequential | O1–O5 and both blind rulings complete |
| **Total** |  | **18 attempts: 12 N, 6 D** | no overlap; D exactly one mutation call | one seed, no history leakage | proof-inclusive complete wall |

## Exact `task.md` received byte-for-byte by every arm

```markdown
# Task: finish the JSON read-policy requires

`marvin-voice-remote.json/parse` already exists. Exactly nine live call sites
in exactly eight source files already use it, but those eight namespaces do not
yet require `marvin-voice-remote.json`.

Add that require to exactly these files:

- `src/marvin_voice_remote/bridge3_new.clj`
- `src/marvin_voice_remote/capture_archive.clj`
- `src/marvin_voice_remote/channel.clj`
- `src/marvin_voice_remote/codex_app_server.clj`
- `src/marvin_voice_remote/director_control.clj`
- `src/marvin_voice_remote/director_outbox.clj`
- `src/marvin_voice_remote/groq.clj`
- `src/marvin_voice_remote/reducer_session.clj`

Use this ordered alias preference:

1. `json`
2. `mjson`
3. `m-json`
4. `json-policy`

In each namespace, reuse a preferred alias already bound to the target;
otherwise choose the first preferred alias not already bound there. Do not
shadow, rename, or replace an alias bound to another namespace. The existing
`json` alias and its `clojure.data.json` require are still live and must remain.
Do not change any call site or remove any existing require.

Make a minimal source-preserving insertion. Treat a contiguous comment block
immediately before a require entry as attached to that following entry; never
split, move, rewrite, or reindent it. Insert the new libspec immediately before
the first existing attached entry whose library symbol sorts after
`marvin-voice-remote.json`, or last if none does. Preserve the inherited order
of all existing entries, copy the local indentation and line-ending style, and
introduce no blank lines.

Do not rewrite strings, docstrings, comments, reader-discarded forms, metadata,
imports, unrelated code, or any file outside the eight named callers. Finish
only when the project loads, both full test commands pass, lint has no delta
from the historical golden, and the submitted diff consists of exactly eight
new require lines and no other changed byte.
```
