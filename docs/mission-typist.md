# Owner-forms typist executor (development prototype)

`bin/mission` can plan an `owner_forms` mission and later apply its frozen plan.
The request is EDN on stdin, avoiding shell quoting. This route is experimental;
it has not earned a production or end-to-end performance claim.

```sh
bin/mission propose --spec-file - < owner-forms.edn
bin/mission apply M-1 --workspace /absolute/project
bin/mission undo M-1 --workspace /absolute/project
```

For a fully decided mission, `bin/mission run --spec-file owner-forms.edn`
saves and immediately applies the plan in one JVM. This is an explicit write
command, restricted to `owner_forms` and no existing mission id. Use `propose`
then `apply` when an intervening review matters. A blocked run retains its
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
  and `:rate`, as described in [the plan](plans/mission-typist-executor.md).
  Source policy explicitly records `:generated?`, `:reader-conditionals?` and
  `:format-sensitive?`. Missing facts refuse. Rate counts are measured successes
  and attempts for this class/provider, with an evidence identifier; never invent
  counts to pass admission. The fake hand-drive uses synthetic facts only while
  transport is replaced and cannot support a live routing decision.

A candidate is a JSON array of objects with exactly `file`, `owner`, and `form`
string fields. The model emits a new definition, never old context, offsets or a
patch. The kernel validates it, formats only owned fragments and splices those
fragments into frozen source. Comments, metadata and unsupported reader syntax
refuse; docstrings must remain byte-identical. Untouched owners and gaps retain
exact bytes. This does not prove that changed definitions preserve behavior.

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
or incomplete output refuse. Automatic Groq fallback and Spark execution are
not implemented yet. The executor starts at most five separately bounded requests and consumes them in
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
argv and safely quoted shell command. Mission write refusals also supply an
inspection/help example, never a blind replay of a failed mutation. These
examples concern the mission entrance, not every Surgeon core operation.

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
