# Action-emission v6 closed its four blockers but still accepts two malformed identities

Date: 2026-08-29

Verdict: **NO-GO for history recollection at exact repaired candidate
`22e5fcc`.** The candidate closes every blocker in the prior independent
receipt. A new adversarial pass found two remaining fail-closed gaps: an
arbitrary malformed CLI `:op` value reaches the session aggregate, clock, and
timeline verbatim, and empty or whitespace-only structural file values produce
authoritative adjacent-read relations instead of `unknown`.

The first defect violates the collector's global privacy law. The second can
create false repeated-read candidates. Both repairs are small and synthetic;
neither requires changing the working v6 action-size or timing mechanism.

## Scope and immutable identity

The audit used an isolated worktree and branch at:

- repaired candidate: `22e5fcc9bc31e278f47fd2fc3e6cc968362fd282`
- candidate tree: `9caa5a0c913b3e955149358956032134d538bc0e`
- prior docs-only NO-GO base: `e7c447a9d0f6f7df42374f2e796b7f4efea143c2`
- red-witness commit: `d450d8be0a52f6a669fe9544bbb6b9aaab3afb17`
- collector SHA-256:
  `0cfc53e0bc83bdae4cb0b9581725c54d17f783727ed3791914b4c2eac233c3b7`
- skill SHA-256:
  `53b2478fc675128ab66bd80ebe7246975040f36cf5b50e1c4bc27af4db764e7a`
- prior receipt SHA-256:
  `61cf1fe608bc3d9c02baee2f161422e53e7a6ee726da71c2cf065e966b42474c`
- repair diff SHA-256 over the collector and skill from `e7c447a..22e5fcc`:
  `a54ed197f863c7f34d93012b20f9b01a472cab8156d332e4cd964331cae9968b`

The audit read only the collector, its synthetic self-test, the study skill,
and the prior design/audit receipts. It did not read or recollect provider
history, inspect a transcript, advance a marker, launch a model, call MCP,
touch a runtime, mutate a process, install, or reload. Every executable probe
used synthetic values.

## The prior blockers are closed

`make study-agent-usage-self-test` passed. Independent replays also proved:

| Prior blocker | Repaired result |
|---|---|
| Arbitrary inspect operation in public JSON | Closed: `forms`, `match`, `outline`, and `xray` remain; every other value becomes literal `unknown` |
| Raw `_structural_target` in `_clock_samples` | Closed: the sample carries only the whole-target digest and domain-separated per-file identities |
| `git`, `live-probe`, and `skill-load` absent as endpoints/background | Closed by one `ACTION_KIND_POLICY`; each is now an endpoint and a background kind |
| Missing/empty MCP server or tool receives action evidence | Closed for missing, empty, whitespace-only, non-string, and non-map cases; valid `{}` arguments remain measurable as two canonical bytes |

The repaired malformed-inspect sample contained no root, ID, operation,
path, owner, or result canary. Its SHA-256 was
`544505c5f37898605417aca13e22b2284722344f9ba721f9432701e221d1e464`.
The corresponding public clock SHA-256 was
`bc55e9c295b94d8e1db5533313c4483b7922f07e497409d26c17ddc073af7a5b`.

The original endpoint falsifiers now report a 100 ms boundary to each of
`git`, `live-probe`, and `skill-load`. A `git` interval already active across a
Surgeon completion contributes its exact clipped 200 ms to the background
union.

## Additional mechanisms that passed

| Law | Result |
|---|---|
| Canonical compact UTF-8 argument/result bytes | PASS |
| Top-level root normalization only | PASS |
| Logical-argument domain separation | PASS |
| Structural-file domain separation | PASS |
| Missing result omitted; explicit JSON null counted as four bytes | PASS |
| Last completed reasoning selected; endpoint-overlapping reasoning excluded | PASS |
| Endpoint excluded from its own background union | PASS |
| Context compaction is background-only | PASS |
| Model reasoning is neither endpoint nor background | PASS |
| Turn-end omits the emission-wall field but retains measured background | PASS |
| CLI endpoint carries transport/operation but no structured argument size/hash | PASS for valid closed operations |
| v5-shaped event clock remains renderable; absent evidence stays absent | PASS |
| Same target, same-files, overlap, disjoint, and no-file relations | PASS on nonblank file identities |
| Two different unknown inspect operations on one file | PASS: both publish `unknown`, while the whole-target digests preserve `same-files`, not false `exact` |
| Logical digests are described as equality evidence, not secrecy | PASS |

One complete policy probe produced:

```json
{
  "next_kind": "git",
  "boundary_ms": 300,
  "model_reasoning_ms": 100,
  "last_reasoning_end_to_next_action_start_ms": 150,
  "overlapping_background_wall_ms": 150
}
```

The same fixture without an endpoint produced `next_kind="turn-end"`, omitted
the emission-wall field, and retained 150 ms of context-compaction background.
The v5 timeline SHA-256 was
`1e46f9c46d07fe3f918954c7e85bc5fa81ce87fcc15753e41822a5e1a9ff9843`.

## New falsifier 1: malformed CLI operation text reaches every public layer

The structured MCP operation was closed, but the CLI route still copies the
raw `SURGEON_RE` capture into `sample.operation` and
`session.clj_surgeon_ops`.

Executable synthetic probe:

```python
cli = completed_item_clock_sample({
    "started_at_ms": 1000,
    "completed_at_ms": 1010,
    "item": {
        "type": "CommandExecution",
        "command": (
            "clj-surgeon :op :CANARY_CLI_OPERATION "
            ":file /CANARY/PATH.clj"
        ),
    },
})
clock = compile_event_clock("1970-01-01T00:00:01Z", 100, [cli])
assert cli["operation"] == ":CANARY_CLI_OPERATION"
assert clock["items"][0]["operation"] == ":CANARY_CLI_OPERATION"

receipt = {
    "window": {"since": "synthetic", "until": "synthetic"},
    "providers": {"codex": {"sessions": [{
        "session_key": "synthetic",
        "task_turns": [{
            "turn_key": "synthetic",
            "completed": True,
            "duration_ms": 100,
            "clj_surgeon_calls": 1,
            "event_clock": clock,
        }],
    }]}},
}
timeline = render_event_clock_receipt(receipt, top=1, minimum_ms=0)
assert ":CANARY_CLI_OPERATION" in timeline

session = empty_session("codex", Path("synthetic.jsonl"))
record_tool_text(
    session,
    "clj-surgeon :op :CANARY_CLI_OPERATION :file /CANARY/PATH.clj",
    "shell",
)
finalized_session = finalize_session(session)
assert finalized_session["clj_surgeon_ops"] == {
    ":CANARY_CLI_OPERATION": 1
}
assert provider_summary(
    "codex", [finalized_session]
)["clj_surgeon_ops"] == {
    ":CANARY_CLI_OPERATION": 1
}
```

The path is not emitted, but the arbitrary operation canary appears in the
completed sample, full clock, rendered timeline, session aggregate, and
provider aggregate. The timeline renders it as:

```text
shell · cli :CANARY_CLI_OPERATION
```

Valid CLI operation names are intended public evidence. An invalid operation
is untrusted command content and must not become a receipt key or timeline
label merely because the regular expression matched it.

## New falsifier 2: blank structural identities create false relations

`target_files` accepts every string under `file` or `files`, including empty
and whitespace-only strings. `structural_file_identities` hashes those values,
so the relation compiler treats them as genuine targets.

Executable synthetic outcomes:

```json
{
  "empty-file versus empty-file": "exact",
  "whitespace-file versus whitespace-file": "exact",
  "empty-file versus whitespace-file": "disjoint-files"
}
```

All three should be `unknown`. There is no usable file identity to compare,
and malformed input must not enter the repeated-read shortlist as an exact or
disjoint structural decision.

This issue is independent of unknown inspect operation collisions. Two
different unknown operations on the same valid file correctly remain
`same-files`; two identical unknown operations on the same valid file remain
`exact` by whole-target equality without exposing the raw operation.

## Smallest repair

Preserve the repaired MCP evidence and action policy unchanged.

1. Add one closed CLI operation normalizer over the existing
   `SURGEON_READ_OPS`, `SURGEON_PLAN_OPS`, `SURGEON_APPLY_OPS`, and `:help`.
   Use it at every `SURGEON_RE` projection, especially
   `completed_item_clock_sample` and `record_tool_text`. Missing or unknown
   values become one literal `unknown` bucket; arbitrary strings never become
   receipt keys or timeline labels.
2. Exclude empty and whitespace-only file strings before computing file
   cardinality or structural file identities. A relation with no remaining
   identities returns `unknown` before whole-target equality is considered.
3. Add the two falsifiers above to the permanent synthetic self-test. Scan the
   completed sample, session aggregate, provider aggregate, clock, full
   receipt, and timeline for the CLI canary.
4. Preserve the current unknown-inspect collision witnesses and all four
   repaired-blocker witnesses.

Then rerun only:

```text
make study-agent-usage-self-test
```

No history recollection is required or authorized to close these gaps.

## Decision

The repaired candidate materially strengthens the collector. Its structured
MCP privacy boundary, action-size evidence, relation hashing, endpoint policy,
background union, reasoning clock, turn-end behavior, and v5 rendering all
survived independent replay.

It remains **NO-GO** because one malformed CLI token is publicly exfiltrated
through three receipt surfaces and because blank structural identities create
false adjacent-read relations. Both are small fail-closed repairs. After they
turn green, the candidate merits one more synthetic-only audit before any
history window is collected.
