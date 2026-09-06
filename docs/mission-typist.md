# Owner-forms typist executor (development prototype)

`bin/mission` can plan an `owner_forms` mission and later apply its frozen plan.
The request is EDN on stdin, avoiding shell quoting. This route is experimental;
it has not earned a production or end-to-end performance claim.

```sh
bin/mission propose --spec-file - < owner-forms.edn
bin/mission apply M-1 --workspace /absolute/project --receipt-dir /absolute/project/.clj-surgeon/typist
bin/mission undo M-1 --workspace /absolute/project
```

Owner-forms execution requires an explicit `--receipt-dir`: artifacts include
frozen source, candidates and receipts. Choose an appropriate local destination;
there is no implicit home-directory fallback. A missing/blank destination returns
`typist-receipt-dir-required` before source/artifact/provider work, with a retry
command bound to the saved id and workspace. CLI refusal leaves a ready mission
ready; the suggested directory is not created until an explicit execution.
`run` also retains its newly saved ready id when this preflight refuses. Explicit
destinations keep their existing behavior; helper extraction is unaffected.

For a fully decided mission, `bin/mission run --spec-file owner-forms.edn --receipt-dir /absolute/project/.clj-surgeon/typist`
saves and immediately applies the plan in one JVM. This is an explicit write
command, restricted to `owner_forms` and no existing mission id. Use `propose`
then `apply` when an intervening authority review matters. Proposal is an
**authority preview**, not a generated candidate diff: it freezes intent, owners,
source and proof authority and saves the mission. It calls no provider.

Successful CLI proposals print the bounded saved `mission-show` view, with the
mission id, state, next action and a workspace/state-home-bound `show --full`
command. `propose --full` preserves the complete proposal stdout; both modes
retain complete authority in the ledger. This output choice does not change
`run` or internal planning results. Candidate
generation happens during apply, followed by proof and the guarded live write;
there is currently no public candidate-diff approval pause between them. A blocked run retains its
mission id and returns `:error_type "mission-not-ready"` with the decision,
without applying; both that refusal and a failed apply exit nonzero. Application
reads the persisted plan and proof authority rather than replanning. This avoids
one cold process start; a comparative wall gain still requires measurement.

The saved spec has `:verb "owner_forms"`, `:request`, and optionally a trusted
`:profiles` map. The request contains:

- `:workspace_root`: canonical project directory.
- `:intent`: the complete bounded change, with discovery already finished.
- `:owners`: `{:file "src/app.clj" :owner "old-name" :new-owner "new-name"}`.
  Omit `:new-owner` when the definition keeps its name. Names refer to original
  definitions; the planner derives exact spans and refuses ambiguous owners.
- `:proof-files`: explicit additional relative files needed by proof, including
  test namespaces and dependency files. These are frozen too. Proof runs against
  this closed file set in a temporary tree.
- `:verification {:profile "gate"}` and `:acceptance_profile "witness"`.
  Each trusted profile supplies `:commands` (argv vectors), `:measured-ms` below
  5000, and a retained `:evidence` identifier. The commands and evidence must
  differ. Distinct strings alone do not establish independence: the caller must
  supply a separately authored behavioral witness.
- `:typist`: `:mission-class`, per-target `:source-policy`, `:budget`, `:provider`
  and `:rate`, with the complete contract below and a
  [parseable template](examples/owner-forms-template.edn).
  Source policy explicitly records `:generated?`, `:reader-conditionals?` and
  `:format-sensitive?`. Missing facts refuse. Rate counts are measured successes
  and attempts for this class/provider, with an evidence identifier; never invent
  counts to pass admission. The fake hand-drive uses synthetic facts only while
  transport is replaced and cannot support a live routing decision.

## Complete request contract

Start from [owner-forms-template.edn](examples/owner-forms-template.edn). It is a
complete **shape**, deliberately inadmissible until its nil/empty facts are
replaced. The local-rename intent is illustrative; it asserts no facts about
your source. Do not change unknown flags to false or invent successful runs to
make admission pass. No automatic calibration command is supplied here.

The top-level map is `{:verb "owner_forms" :request {...} :profiles {...}}`.
The request fields above supply source ownership and proof-file closure. Each
owner needs relative `:file` and original `:owner` strings; `:new-owner` is only
for renaming the definition itself, not a local binding. Both proof profile ids
must exist in the trusted profiles map (or configured verification profiles).
The user-supplied `:typist` map is:

| Field | Contract and evidence to supply |
|---|---|
| `:mission-class` | One of `:rename`, `:thread-parameter`, `:move-helpers`, `:fanout`, `:witness`. Discovery and the exact intended transformation are already decided. |
| `:source-policy` | Map keyed by every target's relative file. Each value explicitly has `:generated? false`, `:reader-conditionals? false`, `:format-sensitive? false`, based on inspection. Nil/missing/true refuses; do not guess. |
| `:budget` | `{:max-files N :max-changed-chars N}` with positive integer limits you authorize. File limit must cover all target files. The character limit is a change budget, not a performance measurement. |
| `:provider` | Exactly the supported identity triple: `{:id :openrouter :model "openai/gpt-oss-120b" :upstream "Cerebras"}` or `{:id :groq :model "openai/gpt-oss-120b" :upstream "Groq"}`. Spark execution refuses. Credentials belong to the configured transport, never this request. |
| `:rate` | `{:verified V :attempted A :evidence "retained-receipt-id" :mission-class CLASS :provider ID :model MODEL :upstream UPSTREAM}`. V/A are integers, A>0, 0<=V<=A; identity fields must match the selected class/provider exactly. Use retained measured verified outcomes, not estimates. The validator checks shape/binding, not whether your claimed measurements are true or transferable to a different workload. |
| `:candidate-format` | Optional `:owner-forms` (default JSON owner/form objects) or `:clojure-forms` (plain complete definitions, exactly one target file). |
| `:max-tokens` | Optional positive integer 1..8192, default 8192, per candidate. Reasoning and answer consume the allocation. |
| `:fallback` | Optional, only with OpenRouter primary: exactly `{:provider :groq :max-tokens N}`. Primary plus fallback allocations must total <=8192 per candidate. Only the documented typed primary responses trigger this provider fallback; it is separate from `mission fallback`. |

Each of the two proof profiles contains `{:commands [["executable" "arg" ...]]
:measured-ms N :evidence "retained-proof-receipt-id"}`. Supply nonempty argv
vectors, an actual nonnegative measured duration <=4999 ms, and retained evidence.
The gate and witness must have different ids, command vectors and evidence ids.
They must also be independently meaningful: different strings or two no-op
commands are not behavioral proof. Include required test/dependency/data files
in `:proof-files`; execution uses the frozen file set in a scratch tree. Proof
commands are trusted code, not an OS sandbox.

Measured verified rates select candidate count: >=85% selects one, <=70%
selects five, and the intervening range selects three. Do not edit counts to
obtain a cheaper route. If you lack the required prior/proof evidence for a
single known local rename, a native edit plus your real behavioral tests is the
usable alternative; the typist setup is not free and this surface promises no
speed advantage for that case.

After supplying genuine facts and reviewing the template:

```sh
bin/mission propose --spec-file - < owner-forms.edn
bin/mission show M-1 --workspace /absolute/project
# M-1 is illustrative: use the id actually returned, and inspect refusals.
bin/mission apply M-1 --workspace /absolute/project --receipt-dir /absolute/project/.clj-surgeon/typist
```

A candidate is a JSON array of objects with exactly `file`, `owner`, and `form`
string fields. The model emits a new definition, never old context, offsets or a
patch. The kernel validates it, formats only owned fragments and splices those
fragments into frozen source. Comments must retain their text and attached
expression identity. Whitespace-only reindentation is allowed; string bytes,
nested comments, attachment side and containing path remain significant.
Dropped or moved comments refuse with diagnostics; there is no broad rewrite
opt-in. Leading owner comments remain attached to the declared owner, whose
name, head and docstring are checked separately. This is not proof that prose
remains true after a behavior change. Metadata and unsupported reader syntax
still refuse; docstrings must remain byte-identical. Untouched owners and gaps
retain exact bytes. This does not prove that changed definitions preserve behavior.

An experimental one-file response format is selected with
`:typist {:candidate-format :clojure-forms}`. The model returns plain complete
Clojure definitions. The kernel resolves emitted names against frozen owner/new-owner
names, requires exact coverage, then uses the same formatting, proof and commit.
There is no JSON-encoded source, guessed unescaping or markdown stripping. The
format is saved with the plan; apply-time request changes cannot switch it.
Multi-file raw requests refuse. Anonymous functions, sets and regex literals are
supported; character literals and other reader dispatch remain outside this first
prototype. Missing, extra or duplicate owners and changed docstrings refuse.
This representation has passed boundary tests and one live mission; all three
retained candidates also passed non-writing proof replay. It has no comparative
performance or calibrated reliability claim yet. Omission retains the existing JSON owner-form format.

The executor runs the gate and independent witness before writing live source,
checks proof inputs for byte/mode changes, rechecks the live frozen snapshot,
and uses the existing guarded transaction and undo kernel. The ledger records
the inverse receipt path before the first live write. Atomicity is per file with
rollback on caught failures, not simultaneous multi-file visibility or immunity
to process death. Scratch proof is file staging, not an operating-system sandbox
for arbitrary program behavior.

Candidate responses and proof outcomes remain under the receipt directory.
Runtime credentials are read by the fixed transport client, never copied into
the dossier. OpenRouter pins `openai/gpt-oss-120b` to Cerebras without upstream
fallback; direct Groq is an explicit selection. Wrong model/upstream and malformed
or incomplete output refuse. An explicitly frozen Groq fallback can follow a
started OpenRouter request's typed 429 or 503 response. Both attempts and their
observed usage/cost are retained; a fallback event requires actual dispatch,
not merely a configured route. Spark execution is not implemented; executor planning refuses it with
`:typist-executor-provider-unavailable` before marking the mission ready. The executor starts at most five separately bounded requests and consumes them in
completion order. Before commit it cancels remaining work and checks both worker
termination and tracked transport-process liveness. Completed replies are retained;
cancelled requests have unknown billed usage. The three-process fake-client path passes real proof, guarded commit and undo;
two live pinned-provider hand-drives now verify (one retained repository change, one fresh fixture). Independent combined fence review and replicated complete-wall performance validation remain pending.

Fake-provider real-program hand-drive:

```sh
clojure -M:clj-surgeon/test-deps -m astra-typist-real1 /path/to/proof.json
```

The fixture helper is a development experiment, not a generic project command.
It exercises persisted planning, owned-form formatting, real-1's actual gate and
independent acceptance, commit and exact undo. It makes no provider call and
prints no speed claim. Timing comparisons must separately charge orientation,
JVM startup, candidate generation, formatting, proof, commit and receipt writing.

## Read the saved result without parsing receipt files

```sh
bin/mission show M-1 --workspace /absolute/project
bin/mission show M-1 --workspace /absolute/project --full
```

The default is a readable EDN projection capped at 4096 UTF-8 bytes: actual
saved state and receipt, the planned route, and candidate refusals including
lost-content diagnostics. `:authority :saved-mission` means this reads recorded
proof; it does not rerun verification. Truncation and omitted candidate counts
are explicit. `:details` supplies the full command; `--full` retains the previous
complete ledger view, which can contain frozen source and large proof details.

A readable failed mission exits zero because the read succeeded. Missing or
corrupt mission rows exit nonzero with an executable `:example` containing both
argv and safely quoted shell command. Mission write refusals normally supply an
inspection/help example. A missing artifact destination instead supplies an
`apply` retry with an explicit `--receipt-dir`: this refusal occurs before
execution and preserves the ready mission. Inspect the suggested destination
before retrying. These examples concern the mission entrance, not every Surgeon
core operation.

`--workspace R` is required for ledger reads: `--state-home H` selects storage,
not the workspace identity. Omitting the workspace returns
`:mission-workspace-required` with `bin/mission help show` as an executable
recovery example; it does not guess a workspace from the launcher's directory.

Record an explicit decision to use native tools:

```sh
bin/mission fallback M-1 --workspace /absolute/workspace --reason refusal
```

Reasons are `refusal`, `unsupported`, `slower-than-native`, and `user-choice`.
This appends a `mission-fallback` event to the shared events ledger (or
`CLJ_SURGEON_EVENTS_FILE` override). The event is explicitly **user-reported**:
it does not perform or verify a native edit, prove adoption, or record provider
fallback. Saved mission state, proof, and source remain unchanged. The receipt
contains the actual appended event and `:recorded true`; a failed append
returns `:recorded false` and exits 1. Missing missions and unsupported reasons
append no fallback event. For auditing, inspect the returned `:event` rather
than inferring native activity from a failed mission.

Historical `show` rows are interpreted conservatively: when a saved decision
has no direct error code, its nested evidence code is displayed with
`:error_source :saved-decision-evidence`. A saved example targeting another
mission verb is omitted and labeled `:incompatible-mission-verb`; runnable help
replaces it. The original row remains available through `--full`, and neither
reading mode rewrites the ledger.

Publish an already verified `owner_forms` mission as a Git commit:

```sh
git -C /absolute/workspace add -- src/the/verified_file.clj
bin/mission commit M-1 --workspace /absolute/workspace
```

Stage **exactly** the mission's verified changed files yourself. This command
stages nothing. It reads the saved mission, inverse receipt, and independent
proof; no spec, proof, profile, or receipt overrides are accepted. The staged
and live files must match the verified result, and HEAD must match its preimage.
The first version supports only regular-file modifications on a nonfrozen local
branch; `main` and `MCP/main` refuse.

This is Git ref publication, separate from the source kernel's commit event.
It uses `commit-tree` and deliberately **skips Git hooks and signing**. It does
not change source or push. Configure your repository's Git identity beforehand.
The generated commit body contains bounded mission/proof provenance. A receipt
with `:git-ref-updated :unknown` requires inspecting the branch and
`:possible-commit` before retrying; it is not evidence that the ref stayed put.

The event-only `fallback` entrance runs under Babashka, using the same handler
and JSONL schema as the JVM API. It loads no source planner or executor. Failed
appends still return `:recorded false` and exit 1; moving the runtime does not
turn user reports into evidence of performed or verified edits.

Successful Git publication records its oid/tree on the mission. `undo` and
`resume` then refuse with `mission-undo-after-git-publication`; they do not
silently revert source beneath the published Git ref or undo Git automatically.
A forced sidecar intent blocks undo during pending or uncertain publication,
including failure to finish recording the ledger metadata. When Git succeeded
but metadata recording failed, the response retains `:git-ref-updated true` and
the known oid/tree with `:metadata-recorded false`. Inspect Git and the
publication records before recovery; an uncertain result is not an unchanged ref.

Default `show` reads saved publication/sidecar metadata without querying Git and
suppresses resume/undo recommendations while recovery is required. `--full`
retains the saved ledger view, which can predate a failed metadata write. This
protection applies to publications made through the corrected command; older
publications made before it had no marker and require manual Git inspection.

## Inspect uncertain Git publication safely

Use the actual workspace and copy the full returned `:commit` or
`:possible-commit` hex oid; do not invent an oid if neither was returned.
The following commands only inspect Git. They disable optional index writes,
fsmonitor, external diff and text conversion where applicable:

```sh
WS=/absolute/project
OID=replace_with_full_returned_hex_oid
git --no-optional-locks -C "$WS" -c core.fsmonitor=false symbolic-ref -q HEAD
git --no-optional-locks -C "$WS" -c core.fsmonitor=false rev-parse --verify HEAD
git --no-optional-locks -C "$WS" -c core.fsmonitor=false show --no-patch --format=fuller "$OID" --
git --no-optional-locks -C "$WS" -c core.fsmonitor=false diff --no-ext-diff --no-textconv --ignore-submodules=none --name-status "$OID" --
bin/mission show M-1 --workspace "$WS"
```

The first two commands identify the current branch/ref. With a known oid, the
next two inspect its commit and differences from the current files; a missing
object or changed branch is not evidence that publication never happened. If
no oid is known, skip the oid-dependent commands and inspect the returned
publication status. Keep the saved receipt and sidecar. Do not delete a marker,
reset a ref or retry publication merely because these reads succeeded. This
prototype supplies no automatic reconciliation command; recovery requires a
review of Git and the saved records. `mission undo` deliberately cannot perform
that Git recovery for you.
