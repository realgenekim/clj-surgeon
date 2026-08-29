# Action-emission v6 still crosses its privacy boundary with structural arguments

Date: 2026-08-29

Verdict: **NO-GO for history recollection or treating the v6 receipt as a
closed privacy contract at exact head `6c271d1`.** The canonical byte, logical
digest, result-absence, reasoning-clock, and interval-union mechanisms work on
their intended cases. Three fail-closed gaps remain: a malformed inspect
operation reaches public JSON verbatim, raw structural target values cross
`completed_item_clock_sample` into `_clock_samples`, and valid `git`,
`live-probe`, and `skill-load` actions are absent from the endpoint/background
taxonomies.

This is a collector-boundary result. It does not claim that a normal valid
`inspect_clojure` call leaked source or result bodies.

## Scope and immutable identity

The audit used an isolated worktree and branch:

- base/head: `6c271d1759090cb42da0d5902d0caa91d572da01`
- tree: `00eb40cba19a00c7204cc545c8ae4476858b8b6f`
- relevant commits: `2c254b6`, `a4ccf6f`, `886d261`, `6c271d1`
- collector SHA-256:
  `6c985f6ce4b86eaba59b86737ed012c4ad23034478ea5a950333258446566a78`
- skill SHA-256:
  `a3a3e9434601f787a4ce46c85817460696eebe0c5c7b531091deba56cda6f795`
- design-receipt SHA-256:
  `deb098cb15bf51f537bc14021b1de86f65a5d8401677c0e9de79b5aa98cf5afd`
- integrated collector/skill diff SHA-256 from `045f4b7..6c271d1`:
  `5f3c8d006e882aeaf64b599f96d019824cc290b3c65590fd35fe54e3d4fd9610`

Only these files were read:

- `skills/study-agent-usage/scripts/collect_agent_usage.py`
- `skills/study-agent-usage/SKILL.md`
- `docs/observations/2026-08-29-action-emission-clock-design-audit.md`

The audit did not read or recollect Codex or Claude history, inspect a
transcript, advance a marker, launch a model, call MCP, touch a runtime, mutate
a process, install, or reload. Every executable probe used synthetic values.

## What passed

`make study-agent-usage-self-test` passed.

An independent synthetic matrix also passed these laws:

| Law | Result |
|---|---|
| Canonical sizes count compact UTF-8 bytes, not Python characters | PASS |
| Only the top-level `workspace_root` is normalized | PASS |
| Root spelling changes raw byte count but not logical digest | PASS |
| A non-root decision change changes the logical digest | PASS |
| Logical digest includes `clj-surgeon.logical-tool-arguments.v1\0` and differs from an unprefixed SHA-256 | PASS |
| Missing result omits `result_canonical_bytes`; explicit JSON null is four bytes | PASS |
| Later completed reasoning wins; reasoning overlapping the endpoint does not | PASS |
| Turn-end omits `last_reasoning_end_to_next_action_start_ms` | PASS |
| Included background intervals are clipped and unioned, not summed | PASS |
| Agent message, file change, CLI command, and non-map MCP arguments carry no action evidence | PASS |
| A v5-shaped boundary remains readable and does not invent argument/result/emission values | PASS |
| The timeline does not render argument hashes, sizes, or canary content | PASS |
| The skill and design call the logical digest equality evidence, not secrecy | PASS |

The independent positive probe produced:

```json
{
  "argument_bytes": 202,
  "result_bytes": 135,
  "last_reasoning_end_to_next_action_start_ms": 200,
  "overlapping_background_wall_ms": 400,
  "timeline_sha256": "7089fb356169d6e18506c9054ebc80a6df0ea66338ea430c32b4b7248922a879"
}
```

No argument, result, root, source, reasoning, path, account, URL, request ID,
or original source-hash canary appeared in that public clock or timeline.

## Falsifier 1: malformed inspect operation reaches the public receipt

`compile_inspect_clock_evidence` counts `request.operation` strings without a
closed vocabulary. A refused or malformed recorded call can therefore place
arbitrary argument text into both the public clock item and its boundary.

This synthetic probe reproduced the failure:

```python
payload = {
    "started_at_ms": 1000,
    "completed_at_ms": 1010,
    "item": {
        "type": "McpToolCall",
        "server": "clj-surgeon",
        "tool": "inspect_clojure",
        "status": "failed",
        "arguments": {
            "workspace_root": "/CANARY/ROOT",
            "requests": [{
                "id": "CANARY_ID",
                "operation": "CANARY_OPERATION_EXFIL",
                "file": "src/CANARY_PATH.clj",
                "forms": ["CANARY_OWNER"],
            }],
        },
        "result": {"isError": True, "content": [{
            "type": "text", "text": "CANARY_RESULT"
        }]},
    },
}
sample = completed_item_clock_sample(payload)
clock = compile_event_clock(
    "1970-01-01T00:00:01Z", 100,
    [sample, {
        "kind": "model-message",
        "started_at_ms": 1020,
        "completed_at_ms": 1030,
    }],
)
assert clock["items"][0]["request_operations"] == {
    "CANARY_OPERATION_EXFIL": 1
}
assert clock["post_surgeon_boundaries"][0]["request_operations"] == {
    "CANARY_OPERATION_EXFIL": 1
}
```

The public synthetic receipt SHA-256 was
`65f13d403528c48dd940800d03be85096dc39ba0eb060a79e4e7ce6c2fc6f5cc`.
The root and result canaries were absent, but `CANARY_OPERATION_EXFIL` was
present twice. The timeline SHA-256 was
`71fc3e1131e646ad96ac0913034e155161d134c56c8cee937a8c60f6ef5fc5f2`
and contained no canary.

This is a public receipt leak from malformed structured arguments. A schema
refusal does not make the recorded argument safe to copy.

## Falsifier 2: raw structural targets survive the intended private boundary

The same sample retained this private field:

```json
{
  "_structural_target": {
    "requests": [{
      "file": "src/CANARY_PATH.clj",
      "forms": ["CANARY_OWNER"],
      "operation": "CANARY_OPERATION_EXFIL"
    }]
  }
}
```

`analyze_codex_file` appends that object directly to `_clock_samples`.
`compile_event_clock` later removes underscore-prefixed fields from public
items, so this object does not reach final JSON. However, the design states
that private values are reduced to scalars/hashes before the sample crosses
into `_clock_samples`, and the audit requirement explicitly asked that raw
arguments not survive `completed_item_clock_sample`. The current object
contains exact path and owner arguments and fails that boundary.

This gap is not necessary to preserve adjacent-read relations. Exact target
equality can use the existing structural digest. Same/overlapping file
relations can use domain-separated per-file identities compiled while the raw
request is in scope.

## Falsifier 3: valid action kinds are invisible as endpoints and background

`completed_item_clock_sample` emits `git`, `live-probe`, and `skill-load`, but
none is in `compile_post_surgeon_boundaries.endpoint_kinds` or
`BACKGROUND_ACTION_KINDS`.

With a Surgeon call completing at 1100 ms, `git status` starting at 1200 ms,
an nREPL probe starting at 1350 ms, and a model message starting at 1500 ms,
the compiler reported:

```json
{
  "boundary_ms": 400,
  "next_kind": "model-message",
  "overlapping_background_wall_ms": 0
}
```

The first externally visible action actually started 100 ms after the Surgeon
result. In a second probe, `git diff` was already active from 1050 through
1300 ms. Its clipped overlap from 1100 through 1300 ms was 200 ms, but the
background covariate remained zero.

The interval merge itself is correct. The allowlists feed it an incomplete
population. If no later recognized endpoint exists, the same omission becomes
a false `turn-end`, and the new emission wall is suppressed by `6c271d1` even
though a real next action exists.

## Falsifier 4: malformed MCP identity still receives structured evidence

The existing self-test treats non-map arguments as malformed. A map with a
missing or empty server/tool is also malformed, but currently receives
`argument_canonical_bytes` and `logical_argument_sha256` and becomes an
`other-tool` or `surgeon-plan` sample. The content remains hashed, so this is
not a prose leak, but it is a false-positive against the documented malformed
endpoint omission law.

## Smallest repair

Keep the working v6 size/digest mechanism. Repair only the fail-closed
boundaries:

1. Normalize inspect operations through a closed public vocabulary. Count any
   missing or unknown value as the literal `unknown`; never publish the raw
   string.
2. Replace `_structural_target` with privacy-safe internal relation evidence:
   the existing whole-target digest plus domain-separated file identities.
   Compute exact/same/overlap/disjoint from those identities. Do not put file,
   owner, subject, request ID, expectation, or source text in `_clock_samples`.
3. Define one closed action-kind set used consistently by endpoint selection
   and background coverage. Add at least `git`, `live-probe`, and `skill-load`.
   Keep source, endpoint, model reasoning, model messages, human input, and
   unattributed gaps excluded from the background union. Keep context
   compaction as background rather than an agent endpoint.
4. Require nonempty MCP server and tool strings plus map arguments before
   compiling action argument evidence. Preserve empty argument maps for valid
   no-argument tools.
5. Add the four falsifiers above to the permanent synthetic self-test and
   repeat the global receipt/timeline canary scan.

Then rerun only:

```text
make study-agent-usage-self-test
```

Do not recollect history to repair or prove this seam.

## Decision

The action-size and post-reasoning clocks are worth preserving. Their intended
positive matrix passed, their logical digest is honestly described, and no raw
canonical argument or result bytes are stored. The current implementation is
still **NO-GO** because malformed structured input reaches public JSON and
because the private reduction boundary retains target values it says it
discards. Endpoint/background omissions also make the new timing field
causally incomplete for valid clock kinds.

A small collector-only ratchet can close all three classes without changing
the receipt's useful public shape or touching product runtime.
