# B07 frozen arm instructions and operator protocol

No cohort has run. This protocol is the preregistered 24-run allocation. Do not
launch until b07-freeze.edn and astra-B07-report.md record a passing feasibility gate.
Use the exact pinned Cell C model/version/settings and runner attestation; if those
are unavailable, stop setup rather than substitute settings. Run sequentially with
fixed cores and the same ~/bin/clj-kondo serialization entrance.

## Common caller brief (N, F and D)

Perform the Cell B extraction below in your assigned fresh worktree at
92a7ca14fd904e4d962df38614b9af3a7ef41a11. The supplied 25-owner manifest is
b07-manifest.edn. The source remains. Explicitly authorized in EVERY arm: promote
communicated?, content-publishable? and speaker-names to public; remove only the
calendar invocation in the source comment block, including its sole print wrapper.
Preserve the surrounding block. These fixed decisions supersede the original task's
open choice of promotion/comment strategy and are a disclosed preregistration change.
The form-count deviation (25 > the original 16-form ceiling) remains disclosed.
Destination alias preference is calendar, then cal; retained-source preference is
exports, then projection. No caller-site table or generated requires are supplied.

Run the same frozen acceptance oracle from your assigned worktree root:

```bash
bash /var/tmp/forge/plan2/cellC/b07/frozen/cell_b_oracle.sh
```

A8 repairs the case-sensitive defect and compares against a fresh archive of the
exact base. All five existing unresolved diagnostics are printed; every new or
increased file/severity/message identity fails, ignoring coordinates only. A failed
analyzer or missing output fails. The fixture, all other acceptance checks, full
make runtests-once gate and protected-file rules remain fixed. The earlier absolute
A8 failure and complete rollback are retained in the feasibility report.

N: use unrestricted native scripts and patches, kondo, and ordinary existing tools.
Author your own changes. Do not acquire the B07 facts service or emitter.

F: author changes natively, with the optional read-only B07 facts projection. Inside
your task clock, use the supplied request rebound ONLY to your assigned worktree:

```bash
bb -cp /var/tmp/forge/plan2/cellC/b07/frozen/runtime/src \
  -m clj-surgeon.core :op :split-ns! :request-file RUN-request.edn :facts-only true
```

The equivalent MCP namespace_split request uses `:plan_only "facts"`. The EDN
response's `:facts` contains snapshot-bound owners, exact file/line/col references,
retained dependencies, promotions and graph facts. It has no candidate bytes or
emitter. Check `:ok`, `:state`, `:snapshot_hash`, `:blockers` and coverage. The
projection includes graph consequences of the supplied manifest, not generated
require forms. Discovery/schema learning, acquisition and any refresh stay timed.
Do not use plan_only=true (the broader emitted-candidate plan) or the D adapter.

D: submit the compact supplied request once through the frozen adapter:

```bash
bb -cp /var/tmp/forge/plan2/cellC/b07/frozen/runtime/src \
  /var/tmp/forge/plan2/cellC/b07/frozen/b07_split_adapter.clj \
  RUN-request.edn RUN-receipts
```

The adapter only injects the trusted `b07-cell-b` verification profile and receipt
directory; the shared compiler discovers references and emits every changed file.
Inspect mutation/commit status before any recovery. Success requires committed,
verification_complete=true, proof_pending=[], and all required checks successful.
A refusal, interface repair or native fallback remains part of the timed result.
Never blindly reapply a completed transaction. N/F may use the identical frozen
proof runner; a successful complete proof is never repeated just for arm parity.

## Operator allocation and measurement

Prepare fresh isolated worktrees with ~/bin/worktree-add. Rebind workspace_root
in a copy of b07-D-request.edn to the assigned worktree; no other request edits in
setup. The supplied snapshot_hash binds the exact frozen base and matches the
hand-drive; preserve it. Keep per-run request hashes. Supply the same manifest and common brief to
all arms, with only that arm's instructions. Never expose implementation source,
other callers' solutions, previous diffs, receipts or build hand-drive artifacts.
Place runner/receipt artifacts outside the fixture. Keep readiness equivalent to
the intended installed entrance and report setup separately. No application prewarm
is needed because final cold proof is mandatory for every arm.

| Run numbers | Block/order |
|---|---|
| 1–6 | Six fresh N floor runs |
| 7–9 | N, F, D |
| 10–12 | N, D, F |
| 13–15 | F, N, D |
| 16–18 | F, D, N |
| 19–21 | D, N, F |
| 22–24 | D, F, N |

Each arm uses one fresh caller. Primary runner clock: orient-start through all
frozen acceptance checks complete, before prose reporting. Include discovery,
schema learning, refusals, repairs, fallback, analyzer queueing and proof. Cap at
1,800 seconds; timeouts or missing completion remain failures, never exclusions.
Save candidate time, suite time, model returns, first-attempt success, refusal and
fallback counts, caller-authored graph/emitter artifacts, exact final diff and
oracle output. No mid-cohort fixes or selective reruns.

Two reviewers independently assess anonymized diffs against this frozen rubric:
all 25 moved definitions retain metadata, visibility, arity, strings/docstrings and
body semantics; all 87 retained definitions preserve behavior apart from exactly
three promotions; all mixed callers preserve retained references; only authorized
comment invocation is removed; requires/imports are coherent and acyclic; protected
bytes are unchanged. Record pass/fail and exact counterexample for each category;
a discrepancy must be resolved against frozen intent, without changing the oracle.
Any semantic/preservation failure fails acceptance regardless of wall time.

D wins only if all six D runs pass without semantic/preservation regression,
at least five finish without interface repair/native fallback, median paired D/N
is <= .75, and median paired seconds saved is >= 90 and greater than twice the
sample SD of the six N floor runs. A compiler correctness failure suspends this
candidate immediately. A miss retains native (unknown or lose per actual evidence).
Compare F/N and D/F without additional win fishing. F within 10% of D and the noise
floor favors a facts service; D must materially beat F to credit emission. If both
lose, record failure to transfer. No larger-N retry is authorized by this protocol.

## Original Cell B task (unchanged reference text)

# Cell B — frozen extraction task (Curtain Call CFP)

Frozen by the independent acceptance owner. Nothing below is discovered by the arms;
everything below is supplied identically to N, M and C. **Caller-site tables are NOT
supplied** (see "What is not supplied").

## Base

| field | value |
|---|---|
| repo | `/home/forge/src/curtaincall-cfp` (arms get their own worktree of it) |
| base sha | `92a7ca14fd904e4d962df38614b9af3a7ef41a11` (`origin/main`, 2026-09-07) |
| source file | `src/cfp_scheduler_killer/exports.clj` (1879 lines, 112 top-level vars) |
| source ns | `cfp-scheduler-killer.exports` |
| destination ns | `cfp-scheduler-killer.exports.calendar` |
| destination file | `src/cfp_scheduler_killer/exports/calendar.clj` |
| oracle | `/var/tmp/forge/plan2/cellB/oracle.sh` (run from the worktree root) |

## The closure — 25 forms, move all of them, exactly once each

Two carried constants (they live outside the calendar block but are used only by it):

```
 41  (def ^:private calendar-prodid …)      ; private
 78  (def ical-domain "cfp-scheduler-killer.local")
```

The calendar block (`;; --- calendar.ics ---`, lines 704–1053), in file order:

```
 706  ics-escape                 defn   [1]
 718  fold-line                  defn   [1]
 729  ics-date                   defn-  [1]     private
 732  ics-stamp                  defn-  [0]     private
 736  ics-uid                    defn   [1]
 743  ics-sequence               defn   [1]
 752  ics-local-datetime         defn-  [2]     private
 768  absolute-link              defn-  [2]     private
 774  vevent                     defn   [2]
 815  snapshot-published?        defn-  [2]     private
 821  last-publication-snapshot  defn-  [1]     private
 831  ics-sequence-at            defn-  [3]     private
 838  cancellation-vevent        defn-  [1]     private
 872  cancellation-for           defn   [2]
 890  hold-the-date-uid          defn   [1]
 897  hold-the-date-sequence     defn   [1]
 905  hold-the-date-vevent       defn   [1 2]
 942  cfp-deadline-uid           defn   [1]
 950  cfp-deadline-sequence      defn   [1]
 957  cfp-deadline-ics           defn   [1 2]
 998  submission-ics             defn   [2]
1017  calendar-ics-for           defn   [2]
1030  calendar-ics               defn   [1 2]
```

Arities, `defn-`/`^:private` visibility, docstrings, type hints and metadata are
preserved as-is. `private` above is the visibility **in the destination namespace**:
every one of these 25 is used only inside the closure or by external callers of the
public ones, so none of them needs a visibility change.

## Rule scorecard (Astra's selection rule, measured on the base sha with `~/bin/clj-kondo` analysis)

| criterion | required | measured on this closure | verdict |
|---|---|---|---|
| forms into one new namespace | 10–16 | **25** (23 `defn` + 2 `def`) | **FAIL — deviation, justified below** |
| external caller namespaces | 6–30 | 6 production (test callers counted separately) | PASS |
| production callers | ≥ 4 | 6: `agent.commands`, `handlers.exports`, `handlers.public-cfp`, `handlers.public-widgets`, `inform`, `session-invites` | PASS |
| callers that ALSO use retained source vars | ≥ 4 | 7 total (4 production: `agent.commands`, `handlers.exports`, `handlers.public-widgets`, `session-invites`; 3 test: `exports-test`, `schedule-test`, `comms-test`) | PASS |
| dependency edges among selected forms | ≥ 6 | 47 | PASS |
| dependency chain | ≥ 3 | 4 (`calendar-ics → cancellation-vevent → ics-uid → ical-domain`) | PASS |
| naturally occurring reference shapes | ≥ 2 | 2 — (a) alias-qualified `exports/<var>` in all 9 external caller files; (b) bare same-namespace `(calendar-ics e)` inside the `(comment …)` reader block in `exports.clj` | PASS, with the caveat below |
| test callers (counted separately) | — | 3: `exports-test` (21 sites), `schedule-test` (12), `comms-test` (3) | — |
| external call sites | — | 43 across 9 files, plus 1 in-file site in the `(comment …)` block | — |

**Caveat on reference shapes:** the *external* caller surface is homogeneous — every one
of the 9 files binds the alias `exports` and every one of the 43 sites is
`exports/<var>`. The second shape is the unqualified in-namespace reference inside the
`(comment …)` block at `exports.clj:1241`. This is a genuine second shape and it is the
one that makes the task hard (see the trap), but an arm that only handles
alias-qualified references will still get 43/44 sites right. Recorded, not padded.

### Why the 14-form sub-variant is rejected

The discovery report's 14-form core (`ics-escape` … `cancellation-for`) fails the rule
**and is architecturally invalid**:

- production callers = **1** (`session-invites`) — the rule needs ≥ 4. Production code
  only calls the five top-level assemblers (`calendar-ics`, `calendar-ics-for`,
  `submission-ics`, `cfp-deadline-ics`, `cancellation-for`); everything above
  `cancellation-for` in the tree has no production caller at all.
- it creates a **circular require**. The 14-form core needs 7 retained `exports` vars
  (`communicated?`, `content-publishable?`, `ical-domain`, `public-answers`,
  `published?`, `room-name`, `speaker-names`), so `exports.calendar → exports`. Five
  forms left behind in `exports` (`hold-the-date-vevent`, `cfp-deadline-ics`,
  `submission-ics`, `calendar-ics-for`, `calendar-ics`) call into the moved core, so
  `exports → exports.calendar`. Clojure refuses to load that.

The same objection kills every intermediate superset up to 22 forms: any cut that leaves
an `exports` form calling into the new namespace is cyclic, because the new namespace
must require `exports` for the 8 retained vars below. **The smallest upward-closed,
acyclic cut with ≥ 4 production callers is the full 25.** Measured supersets:

| candidate | forms | production callers | `exports` forms left calling in | acyclic? |
|---|---|---|---|---|
| 14-form core | 14 | 1 | 5 | no |
| + `submission-ics` | 15 | 2 | 4 | no |
| + `calendar-ics-for` | 16 | 3 | 3 | no |
| + the `cfp-deadline-*` trio | 19 | 4 | 2 | no |
| **full closure** | **25** | **6** | **0** (only the `(comment …)` block) | **yes** |

This is a recorded rule deviation on ONE criterion (form count 25 > 16), taken because
the alternative is "no eligible task found" for this repository. The deviation is
disclosed in every arm's brief and must be repeated in the final report; it does not
loosen any oracle check.

## The dependency graph (47 intra-closure edges)

```
vevent              -> ics-uid ics-stamp ics-sequence ics-local-datetime ics-date ics-escape fold-line
cancellation-vevent -> ics-uid ics-stamp ics-local-datetime ics-date ics-escape fold-line calendar-prodid(*)
cancellation-for    -> last-publication-snapshot snapshot-published? ics-sequence ics-sequence-at
hold-the-date-vevent-> hold-the-date-uid hold-the-date-sequence ics-stamp ics-date ics-escape absolute-link fold-line
cfp-deadline-ics    -> cfp-deadline-uid cfp-deadline-sequence ics-stamp ics-date ics-escape absolute-link fold-line calendar-prodid
submission-ics      -> cancellation-for cancellation-vevent vevent calendar-prodid
calendar-ics-for    -> vevent ics-escape calendar-prodid
calendar-ics        -> cancellation-for cancellation-vevent hold-the-date-vevent vevent ics-escape calendar-prodid
ics-uid             -> ical-domain
cfp-deadline-uid    -> ical-domain
```
(*) exact per-form constant use is the arm's to verify; the graph above is advisory
context, not an oracle input.

## Architectural rationale

`cfp-scheduler-killer.exports` is the projection layer: it turns the event log into
every machine-readable surface (JSON, CSV, llms.txt, webhooks, the ICS calendar). The
ICS family is the only surface in that file with its own *wire format* — RFC 5545 line
folding, escaping, UID stability, `SEQUENCE` amendment semantics, cancellation
`METHOD:CANCEL` events — and its own correctness contract (a UID must survive a title
amendment; a `SEQUENCE` must never go backwards; an unpublished session must not leak
into the feed). That contract is 350 lines of one file that otherwise knows nothing
about calendars. Moving it gives the calendar wire format a namespace whose tests,
docstrings and future changes are about iCalendar and nothing else, and leaves
`exports` as the projection/access-control layer it claims to be.

The direction of the dependency is the point: **`exports.calendar` requires `exports`,
never the reverse.** The calendar formatter asks the projection layer what is publishable
(`published?`, `publishable-sessions`, `content-publishable?`, `communicated?`) and how
to render a session (`room-name`, `speaker-names`, `public-answers`,
`date-range-string`); the projection layer must not know that iCalendar exists.

### The trap (stated, because it is architecture, not a caller site)

`exports.clj` contains a `(comment …)` REPL block that calls `calendar-ics` with an
unqualified symbol. Left in place it forces `exports` to require `exports.calendar`,
which requires `exports` back — a circular require and a hard load failure that a
compile-only check will catch but a text search will not. Resolving it is part of the
task; how (delete the line, delete the block, move the block, or qualify it in a way
that does not create the cycle) is the arm's decision.

### Retained vars the moved namespace will need

Three of them are currently private in `exports` and must be promoted to public (or the
arm must find another behaviour-preserving route):

| var | line | visibility |
|---|---|---|
| `communicated?` | 209 | `defn-` private |
| `content-publishable?` | 224 | `defn-` private |
| `speaker-names` | 366 | `defn-` private |
| `published?` | 235 | public |
| `publishable-sessions` | 250 | public |
| `public-answers` | 278 | public |
| `date-range-string` | 312 | public |
| `room-name` | 402 | public |

Promoting a private var is a visibility change, not a behaviour change, and is
permitted. Duplicating any of these into the new namespace is **not** permitted (the
oracle's "defined exactly once repo-wide" check will fail).

## Scope and rules for every arm

1. Move all 25 forms into `src/cfp_scheduler_killer/exports/calendar.clj`, namespace
   `cfp-scheduler-killer.exports.calendar`. Each form is defined exactly once,
   repo-wide, in that file.
2. `exports.clj` retains **no** definition of any of the 25 and **no** forwarding /
   facade / `def`-alias / `intern` / `potemkin` re-export of them.
3. Every reference in the declared project scope (`src/`, `test/`, `bin/`, `dev/`)
   is rewritten to the new namespace. No compatibility shims.
4. Callers that also use retained `exports` vars keep those references pointing at
   `exports` — a caller may end up requiring both namespaces.
5. No behaviour change: same arities, same visibility, same docstrings, same output
   bytes. Formatting-only churn in untouched forms is discouraged but not failed.
6. The full local gate must pass: `make runtests-once` (Prolog oracles + JS tests +
   the new-mission worktree test + `bin/kaocha unit --fail-fast`).
7. Only these files may change (the oracle hashes everything else against the base):
   `src/cfp_scheduler_killer/exports.clj`, `src/cfp_scheduler_killer/exports/calendar.clj`,
   `src/cfp_scheduler_killer/agent/commands.clj`,
   `src/cfp_scheduler_killer/handlers/exports.clj`,
   `src/cfp_scheduler_killer/handlers/public_cfp.clj`,
   `src/cfp_scheduler_killer/handlers/public_widgets.clj`,
   `src/cfp_scheduler_killer/inform.clj`,
   `src/cfp_scheduler_killer/session_invites.clj`,
   `test/cfp_scheduler_killer/comms_test.clj`,
   `test/cfp_scheduler_killer/exports_test.clj`,
   `test/cfp_scheduler_killer/schedule_test.clj`.
   That list is the *authorised footprint*, not a discovery answer: it names the files
   an arm is allowed to touch, and an arm that touches fewer still fails the reference
   checks. Nothing else — no `deps.edn`, no `tests.edn`, no Makefile, no other source
   or test file — may differ from the base.

## What is NOT supplied to the arms

- **No caller-site table.** No file list of the 43 sites, no line numbers, no
  per-file counts, no alias inventory. Finding and rewriting the caller surface is
  the work being measured. (The authorised-footprint list in rule 7 is a
  protected-content boundary; it does not say which var is referenced where, how many
  times, or under which alias.)
- No `:require` block for the new namespace, no import list, no ordering.
- No answer to the `(comment …)` cycle beyond "it exists and it is architecture".
- No promotion patch for the three private vars.
- No script, transcript, or plan from any other arm.
